package com.chocolatey.pmsencoder

import static Util.listToArray

import com.sun.jna.Platform

import net.pms.configuration.PmsConfiguration
import net.pms.io.OutputParams
import net.pms.io.PipeProcess
import net.pms.io.ProcessWrapper
import net.pms.io.ProcessWrapperImpl
import net.pms.PMS

@groovy.transform.CompileStatic
@groovy.util.logging.Log4j(value="logger")
class ProcessManager {
    static final long LAUNCH_TRANSCODE_SLEEP = 200
    static final long MKFIFO_SLEEP = 200
    List<ProcessWrapper> attachedProcesses
    OutputParams outputParams
    private PMSEncoder pmsencoder
    private static final PmsConfiguration configuration = PMS.getConfiguration()

    ProcessManager(PMSEncoder pmsencoder, OutputParams params) {
        this.pmsencoder = pmsencoder
        this.outputParams = params
        attachedProcesses = new ArrayList<ProcessWrapper>()
        // modify the output params object *before* the match so it can optionally be customized
        outputParams.minBufferSize = params.minFileSize
        outputParams.secondread_minsize = 100000
        outputParams.log = true // for documentation only as it's done automatically for pipe-writing processes
    }

    private void sleepFor(long milliseconds) {
        try {
            Thread.sleep(milliseconds)
        } catch (InterruptedException e) {
            Plugin.error('thread interrupted', e)
        }
    }

    private PipeProcess mkfifo(PipeProcess pipe) {
        def process = pipe.getPipeProcess()
        attachedProcesses << process
        process.runInNewThread()
        sleepFor(MKFIFO_SLEEP)
        pipe.deleteLater()
        return pipe
    }

    public String getFifoPath(String basename) {
        try {
            return Platform.isWindows() ?
                '\\\\.\\pipe\\' + basename :
                (new File(configuration.getTempFolder(), basename)).getCanonicalPath()
        } catch (IOException e) {
            Plugin.error('Pipe may not be in temporary directory', e)
            return basename
        }
    }

    public void createTranscoderFifo(String transcoderOutputBasename) {
        def transcoderOutputPipe = mkfifo(new PipeProcess(transcoderOutputBasename))

        // this file is the one that PMS reads the transcoded video from via params.input_pipes[0],
        // so we can assign that upfront as well
        outputParams.input_pipes[0] = transcoderOutputPipe
    }

    public void handleHook(List<String> hookArgs) {
        def cmdArray = listToArray(hookArgs)

        // PMS doesn't require input from this process - so use new OutputParams
        def params = new OutputParams(configuration)
        params.log = true

        def hookProcess = new ProcessWrapperImpl(cmdArray, params)

        logger.info("hook command: ${hookArgs.join(' ')}")
        hookProcess.runInNewThread()
        attachedProcesses << hookProcess
    }

    public ProcessWrapperImpl handleDownloadWindows(List<String> downloaderArgs, List<String> transcoderArgs) {
        def cmdList = ([ "cmd.exe", "/C" ] + downloaderArgs + "|" + transcoderArgs) as List<String>
        def cmdArray = listToArray(cmdList)
        def pw = new ProcessWrapperImpl(cmdArray, outputParams) // may modify cmdArray[0]

        logger.info("command: ${cmdList.join(' ')}")
        return pw
    }

    public void handleDownloadUnix(List<String> downloaderArgs, String downloaderOutputBasename) {
        def downloaderOutputPipe = mkfifo(new PipeProcess(downloaderOutputBasename))
        attachedProcesses << downloaderOutputPipe.getPipeProcess()
        def cmdArray = listToArray(downloaderArgs)

        // PMS doesn't require input from this process - so use new OutputParams
        def params = new OutputParams(configuration)
        params.log = true

        def downloaderProcess = new ProcessWrapperImpl(cmdArray, params) // may modify cmdArray[0]
        attachedProcesses << downloaderProcess
        logger.info("downloader command: ${downloaderArgs.join(' ')}")
        downloaderProcess.runInNewThread()
    }

    public ProcessWrapperImpl handleTranscode(List<String> transcoderArgs) {
        def cmdArray = listToArray(transcoderArgs)
        def transcoderProcess = new ProcessWrapperImpl(cmdArray, outputParams) // may modify cmdArray[0]
        logger.info("transcoder command: ${transcoderArgs.join(' ')}")
        return transcoderProcess
    }

    public ProcessWrapper launchTranscode(ProcessWrapperImpl transcoderProcess) {
        attachedProcesses.each { ProcessWrapper pw -> transcoderProcess.attachProcess(pw) }
        transcoderProcess.runInNewThread()
        sleepFor(LAUNCH_TRANSCODE_SLEEP)
        return transcoderProcess
    }
}
