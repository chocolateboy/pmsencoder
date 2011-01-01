@Typed
package com.chocolatey.pmsencoder

import net.pms.io.OutputParams
import net.pms.io.PipeProcess
import net.pms.io.ProcessWrapper
import net.pms.io.ProcessWrapperImpl
import net.pms.PMS

private class ProcessManager implements LoggerMixin {
    static final long LAUNCH_TRANSCODE_SLEEP = 200
    static final long MKFIFO_SLEEP = 200
    List<ProcessWrapper> attachedProcesses
    OutputParams outputParams
    private PMSEncoder pmsencoder

    ProcessManager(PMSEncoder pmsencoder, OutputParams params) {
        this.pmsencoder = pmsencoder
        this.outputParams = params
        attachedProcesses = new ArrayList<ProcessWrapper>()
        // modify the output params object *before* the match so it can optionally be customized
        outputParams.minBufferSize = params.minFileSize
        outputParams.secondread_minsize = 100000
        outputParams.log = true // for documentation only as it's done automatically for pipe-writing processes
    }

    private String[] listToArray(List<String> list) {
        def array = new String[ list.size() ]
        list.toArray(array)
        return array
    }

    private void sleepFor(long milliseconds) {
        try {
            Thread.sleep(milliseconds)
        } catch (InterruptedException e) {
            PMS.error('PMSEncoder: thread interrupted', e)
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
            return pmsencoder.isWindows ?
                '\\\\.\\pipe\\' + basename :
                (new File(PMS.getConfiguration().getTempFolder(), basename)).getCanonicalPath()
        } catch (IOException e) {
            PMS.error('PMSEncoder: Pipe may not be in temporary directory', e)
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
        def params = new OutputParams(pmsencoder.getConfiguration())

        params.log = true

        def hookProcess = new ProcessWrapperImpl(cmdArray, params)

        log.info('hook command: ' + Arrays.toString(cmdArray))
        hookProcess.runInNewThread()
        attachedProcesses << hookProcess
    }

    public ProcessWrapperImpl handleDownloadWindows(List<String> downloaderArgs, List<String> transcoderArgs) {
        def cmdList = ([ "cmd.exe", "/C" ] + downloaderArgs + "|" + transcoderArgs) as List<String>
        def cmdArray = listToArray(cmdList)
        def pw = new ProcessWrapperImpl(cmdArray, outputParams) // may modify cmdArray[0]

        log.info('command: ' + Arrays.toString(cmdArray))
        return pw
    }

    public void handleDownloadUnix(List<String> downloaderArgs, String downloaderOutputBasename) {
        def downloaderOutputPipe = mkfifo(new PipeProcess(downloaderOutputBasename))
        attachedProcesses << downloaderOutputPipe.getPipeProcess()
        def cmdArray = listToArray(downloaderArgs)

        // PMS doesn't require input from this process - so use new OutputParams
        def params = new OutputParams(pmsencoder.getConfiguration())
        params.log = true

        def downloaderProcess = new ProcessWrapperImpl(cmdArray, params) // may modify cmdArray[0]
        attachedProcesses << downloaderProcess
        log.info('downloader command: ' + Arrays.toString(cmdArray))
        downloaderProcess.runInNewThread()
    }

    public ProcessWrapperImpl handleTranscode(List<String> transcoderArgs) {
        def cmdArray = listToArray(transcoderArgs)
        def transcoderProcess = new ProcessWrapperImpl(cmdArray, outputParams) // may modify cmdArray[0]
        log.info('transcoder command: ' + Arrays.toString(cmdArray))
        return transcoderProcess
    }

    public ProcessWrapper launchTranscode(ProcessWrapperImpl transcoderProcess) {
        attachedProcesses.each { transcoderProcess.attachProcess(it) }
        transcoderProcess.runInNewThread()
        sleepFor(LAUNCH_TRANSCODE_SLEEP)
        return transcoderProcess
    }
}
