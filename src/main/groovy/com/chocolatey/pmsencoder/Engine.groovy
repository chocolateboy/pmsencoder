@Typed
package com.chocolatey.pmsencoder

import javax.swing.JComponent

import net.pms.configuration.PmsConfiguration
import net.pms.dlna.DLNAMediaInfo
import net.pms.encoders.MEncoderWebVideo
import net.pms.formats.Format
import net.pms.io.OutputParams
import net.pms.io.PipeIPCProcess
import net.pms.io.PipeProcess
import net.pms.io.ProcessWrapper
import net.pms.io.ProcessWrapperImpl
import net.pms.PMS

import org.apache.log4j.Logger

public class Engine extends MEncoderWebVideo {
    public static final String ID = 'pmsencoder'
    private static final boolean isWindows = PMS.get().isWindows()
    private Matcher matcher
    private final PmsConfiguration configuration
    private Logger log

    @Override
    public String mimeType() {
        'video/mpeg'
    }

    @Override
    public String name() {
        'PMSEncoder'
    }

    @Override
    public String id() {
        ID
    }

    public Engine(PmsConfiguration configuration, Matcher matcher) {
        super(configuration)
        this.configuration = configuration
        this.matcher = matcher
        this.log = Logger.getLogger(this.getClass().getName())
    }

    private String getFifoPath(String basename) {
        try {
            return isWindows ?
                '\\\\.\\pipe\\' + basename :
                (new File(PMS.getConfiguration().getTempFolder(), basename)).getCanonicalPath()
        } catch (IOException e) {
            PMS.error('Pipe may not be in temporary directory', e)
            return basename
        }
    }

    // XXX unused
    private boolean fifoCreated(PipeProcess pp) {
        return (new File(pp.getInputPipe())).exists()
    }

    private void sleepFor(long milliseconds) {
        try {
            Thread.sleep(milliseconds)
        } catch (InterruptedException e) {
            PMS.error('thread interrupted', e)
        }
    }

    // XXX: workaround Groovy's nonexistant support for generic methods
    private PipeIPCProcess mkfifo(ProcessWrapperImpl pw, PipeIPCProcess pipe) {
        mkfifo(pw, pipe.getPipeProcess())
        pipe.deleteLater()
        return pipe
    }

    // XXX: workaround Groovy's nonexistant support for generic methods
    private PipeProcess mkfifo(ProcessWrapperImpl pw, PipeProcess pipe) {
        mkfifo(pw, pipe.getPipeProcess())
        pipe.deleteLater()
        return pipe
    }

    private void mkfifo(ProcessWrapperImpl pw, ProcessWrapper process) {
        process.runInNewThread()
        pw.attachProcess(process)
        sleepFor(1000)
    }

    @Override
    public ProcessWrapper launchTranscode(String uri, DLNAMediaInfo media, OutputParams transcoderParams)
    throws IOException {
        def now = System.currentTimeMillis()

        /*
         * On Windows, there are potentially 3 files (named pipes):
         *
         * 1) the downloader's output (invisible to the user)
         * 2) the transcoder's input
         * 3) the transcoder's output (read by PMS)
         *
         * e.g.
         *
         * downloader --output downloader.output
         * transcoder --input transcoder.input --output transcoder.output
         *
         * net.pms.io.PipeIPCProcess pipes bytes from downloader.output into transcoder.input
         *
         * On other platforms, there are still 3 names, but two of the files are the same:
         *
         * 1) the downloader's output/transcoder's input (same file)
         * 2) the transcoder's output
         *
         * downloader --output fifo
         * transcoder --input fifo --output transcoder.output
         *
         */

        def downloaderOutputBasename = 'pmsencoder_downloader_out_' + now // downloader-only
        def transcoderInputBasename = 'pmsencoder_transcoder_in_' + now   // Windows/downloader-only
        def transcoderOutputBasename = 'pmsencoder_transcoder_out_' + now // always used (read by PMS)
        def transcoderOutputPath = getFifoPath(transcoderOutputBasename)

        def command = new Command()
        def oldStash = command.getStash()
        def oldArgs = command.getArgs()

        command.setParams(transcoderParams)

        oldStash.put('$URI', uri)
        oldStash.put('$MENCODER', configuration.getMencoderPath())
        oldStash.put('$MENCODER_MT', configuration.getMencoderMTPath())
        oldStash.put('$TRANSCODER_OUT', transcoderOutputPath)

        def downloaderOutputPath = getFifoPath(downloaderOutputBasename)
        def transcoderInputPath = getFifoPath(transcoderInputBasename)

        if (isWindows) {
            oldStash.put('$DOWNLOADER_OUT', downloaderOutputPath)
            oldStash.put('$TRANSCODER_IN', transcoderInputPath)
        } else {
            oldStash.put('$DOWNLOADER_OUT', downloaderOutputPath)
            oldStash.put('$TRANSCODER_IN', downloaderOutputPath)
        }

        oldArgs.add('-o')
        oldArgs.add(transcoderOutputPath)

        log.info('invoking matcher for: ' + uri)

        try {
            matcher.match(command)
        } catch (Throwable e) {
            log.error('match error: ' + e)
            PMS.error('match error', e)
        }

        // the whole point of the command abstraction is that the stash-Map/args-List
        // can be changed by the matcher so make sure we refresh
        def newStash = command.getStash()
        def newArgs = command.getArgs()
        def matches = command.getMatches()
        def nMatches = matches.size()

        if (nMatches == 0) {
            log.info('0 matches for: ' + uri)
        } else if(nMatches == 1) {
            log.info('1 match (' + matches + ') for: ' + uri)
        } else {
            log.info(nMatches + ' matches (' + matches + ') for: ' + uri)
        }

        def downloaderArgs = command.getDownloader()
        def transcoderArgs = command.getTranscoder()

        if (transcoderArgs == null) { // using MEncoder: prepends the executable and append the URI
            transcoderArgs = newArgs
            transcoderArgs.add(0, executable())
            transcoderArgs.add(downloaderArgs == null ? newStash.get('$URI') : transcoderInputPath)
        }

        def transcoderCmdArray = new String[ transcoderArgs.size() ]
        transcoderArgs.toArray(transcoderCmdArray)

        transcoderParams.minBufferSize = transcoderParams.minFileSize
        transcoderParams.secondread_minsize = 100000
        transcoderParams.log = true // send the command's stdout/stderr to debug.log
        def pw = new ProcessWrapperImpl(transcoderCmdArray, transcoderParams)

        // create the transcoder's mkfifo process
        // the names used here are (pipe/mkfifo) are an imperfect attempt to make the current names less 
        // incomprehensible (a PipeProcess has a getPipeProcess method which returns a... ProcessWrapper?!)
        // note: we could set this thread running before calling the match() method
        def pipe = mkfifo(pw, new PipeProcess(transcoderOutputBasename))

        // XXX it's safe to set this lazily because the input_pipes array is not used by the ProcessWrapperImpl
        // constructor above the alternative is to call pw.attachProcess() ourselves every time we
        // create a mkfifo process
        transcoderParams.input_pipes[0] = pipe

        if (downloaderArgs != null) {
            handleDownload(pw, downloaderArgs, downloaderOutputBasename, transcoderInputBasename)
        }

        log.info('transcoder command: ' + Arrays.toString(transcoderCmdArray))
        pw.runInNewThread()
        sleepFor(1000)
        return pw
    }

    void handleDownload(
        ProcessWrapperImpl pw,
        List<String> args,
        String downloaderOutputBasename,
        String transcoderInputBasename
    ) {
        if (isWindows) {
            def pipe = new PipeIPCProcess(downloaderOutputBasename, transcoderInputBasename, true, true)
            mkfifo(pw, pipe)
        } else {
            mkfifo(pw, new PipeProcess(downloaderOutputBasename))
        }

        def cmdArray = new String[ args.size() ]
        args.toArray(cmdArray)

        def params = new OutputParams(configuration)
        params.log = true

        def downloader = new ProcessWrapperImpl(cmdArray, params)
        pw.attachProcess(downloader)
        log.info('downloader command: ' + Arrays.toString(cmdArray))
        downloader.runInNewThread()
    }
}
