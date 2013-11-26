package com.chocolatey.pmsencoder

import com.sun.jna.Platform
import net.pms.PMS
import net.pms.configuration.PmsConfiguration
import net.pms.io.OutputParams
import net.pms.io.PipeProcess
import net.pms.io.ProcessWrapper
import net.pms.io.ProcessWrapperImpl

import static Util.cmdListToArray
import static Util.shellQuoteList
import static Util.shellQuoteString

@groovy.transform.CompileStatic
@groovy.util.logging.Log4j(value='logger')
class ProcessManager {
    static final long LAUNCH_TRANSCODE_SLEEP = 200
    static final long MKFIFO_SLEEP = 200
    List<ProcessWrapper> attachedProcesses
    OutputParams outputParams
    private PMSEncoder pmsencoder
    private static final PmsConfiguration configuration = PMS.getConfiguration()
    private static final String cmdExe = Platform.isWindows() ? getCmdExe() : null

    // find the cmd.exe path
    private static String getCmdExe() {
        def cmd = String.format('%s\\System32\\cmd.exe', System.getenv('SystemRoot') ?: 'C:\\Windows')
        def comSpec = System.getenv('ComSpec')?.trim()

        if (comSpec) {
            // XXX watch out for multiple entries: http://superuser.com/questions/446595/is-it-valid-for-comspec-to-have-multiple-entries
            def status = 'OK'

            if (!FileUtil.fileExists(comSpec)) {
                status = 'not found'
            } else if (!FileUtil.isExecutable(comSpec)) {
                status = 'not executable'
            } else {
                cmd = comSpec
            }

            Plugin.debug("ComSpec ($status): ${comSpec.inspect()}")
        }

        Plugin.debug("cmd.exe: ${cmd.inspect()}")
        return cmd
    }

    /*
        if a downloader is used on Windows, we use cmd.exe (rather than named pipes) to pipe its output to the transcoder e.g.:

            cmd.exe /C "downloader.exe --input http://example.com --output - | transcoder.exe --input -"

        cmd.exe's command parsing is so badly documented that it may as well be undocumented.
        From the scattered and inconsistent notes online, it appears that we need to double-quote arguments that contain
        spaces or special (to cmd.exe) characters and escape embedded double quotes with a backslash.

        A heroic attempt to decipher the lunacy of cmd.exe's parser can be found here:

            http://windowsinspired.com/the-correct-way-to-quote-command-line-arguments/
            http://windowsinspired.com/understanding-the-command-line-string-and-arguments-received-by-a-windows-program/
            http://windowsinspired.com/how-a-windows-programs-splits-its-command-line-into-individual-arguments/

        The release notes for JDK 7u25 also include some hints about working with (i.e. around) cmd.exe:

            http://www.oracle.com/technetwork/java/javase/7u25-relnotes-1955741.html#jruntime
    */
    private static String shellQuoteString(Object obj) {
        String str = obj?.toString()

        assert str != null

        // XXX there's a missing case here: "Escape any literal backslashes that occur immediately prior to either
        // a double quote or an e-quote character so they're not interpreted as an escape character."
        // http://windowsinspired.com/how-a-windows-programs-splits-its-command-line-into-individual-arguments/
        if (Platform.isWindows()) {
            if (str.contains('"')) { // escape embedded double quotes
                str = str.replaceAll('"', '\\"')
            }

            if (str =~ /\s|[&<>()@^|]/) { // quote options that include spaces or special characters
                str = '"' + str + '"'
            }
        }

        return str
    }

    private static List<String> shellQuoteList(List<?> list) {
        return list.collect { shellQuoteString(it) }
    }

    ProcessManager(PMSEncoder pmsencoder, OutputParams params) {
        this.pmsencoder = pmsencoder
        this.outputParams = params

        attachedProcesses = new ArrayList<ProcessWrapper>()

        // modify the output params object *before* the match so it can optionally be customized

        // minFileSize (MB) corresponds to the PMS.conf option minimum_web_buffer_size (default: 1)
        // i.e. buffer (only) 1 MB before sending the file
        outputParams.minBufferSize = params.minFileSize

        // XXX cargo-culted from legacy web engines
        // the default is 1,000,000
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
        def cmdArray = cmdListToArray(hookArgs)

        // PMS doesn't require input from this process - so use new OutputParams
        def params = new OutputParams(configuration)
        params.log = true

        def hookProcess = new ProcessWrapperImpl(cmdArray, params)

        logger.info("hook command: ${hookArgs.join(' ')}")
        hookProcess.runInNewThread()
        attachedProcesses << hookProcess
    }

    public ProcessWrapperImpl handleDownloadWindows(List<String> downloaderArgs, List<String> transcoderArgs) {
        def cmd = (shellQuoteList(downloaderArgs) + '|' + shellQuoteList(transcoderArgs)).join(' ')
        def cmdList = [ shellQuoteString(cmdExe), '/C', cmd ]
        def cmdArray = cmdListToArray(cmdList)
        def pw = new ProcessWrapperImpl(cmdArray, outputParams) // may modify cmdArray[0]

        logger.info("command: ${cmdList.join(' ')}")
        return pw
    }

    public void handleDownloadUnix(List<String> downloaderArgs, String downloaderOutputBasename) {
        def downloaderOutputPipe = mkfifo(new PipeProcess(downloaderOutputBasename))
        attachedProcesses << downloaderOutputPipe.getPipeProcess()
        def cmdArray = cmdListToArray(downloaderArgs)

        // PMS doesn't require input from this process - so use new OutputParams
        def params = new OutputParams(configuration)
        params.log = true

        def downloaderProcess = new ProcessWrapperImpl(cmdArray, params) // may modify cmdArray[0]
        attachedProcesses << downloaderProcess
        logger.info("downloader command: ${downloaderArgs.join(' ')}")
        downloaderProcess.runInNewThread()
    }

    public ProcessWrapperImpl handleTranscode(List<String> transcoderArgs) {
        def cmdArray = cmdListToArray(shellQuoteList(transcoderArgs))
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
