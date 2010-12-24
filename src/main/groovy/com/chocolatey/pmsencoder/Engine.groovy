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
    private Plugin plugin
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

    public Engine(PmsConfiguration configuration, Plugin plugin) {
        super(configuration)
        this.configuration = configuration
        this.plugin = plugin
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
        sleepFor(200)
    }

    @Override
    public ProcessWrapper launchTranscode(String uri, DLNAMediaInfo media, OutputParams params)
    throws IOException {
        // FIXME: should really use a synchronized counter to ensure
        // concurrent requests don't use the same filenames
        def now = System.currentTimeMillis()
        def transcoderOutputBasename = 'pmsencoder_transcoder_out_' + now // always used (read by PMS)
        def transcoderOutputPath = getFifoPath(transcoderOutputBasename)

        def command = new Command()
        def oldStash = command.getStash()
        def oldArgs = command.getArgs()

        command.setParams(params)

        oldStash.put('$URI', uri)
        oldStash.put('$MENCODER', configuration.getMencoderPath())
        oldStash.put('$MENCODER_MT', configuration.getMencoderMTPath())
        oldStash.put('$TRANSCODER_OUT', transcoderOutputPath)

        // these are only used if a (non-Windows) downloader is assigned
        def downloaderOutputBasename = 'pmsencoder_downloader_out_' + now
        def downloaderOutputPath = getFifoPath(downloaderOutputBasename)

        if (isWindows) {
            oldStash.put('$DOWNLOADER_OUT', '-')
        } else {
            oldStash.put('$DOWNLOADER_OUT', downloaderOutputPath)
        }

        oldArgs.add('-o')
        oldArgs.add(transcoderOutputPath)

        log.info('invoking matcher for: ' + uri)

        try {
            plugin.match(command)
        } catch (Throwable e) {
            log.error('match error: ' + e)
            PMS.error('match error', e)
        }

        // the whole point of the command abstraction is that the stash Map/args List
        // can be changed by the matcher, so make sure we refresh
        def newStash = command.getStash()
        def newArgs = command.getArgs()
        def matches = command.getMatches()
        def nMatches = matches.size()

        if (nMatches == 0) {
            log.info('0 matches for: ' + uri)
        } else if (nMatches == 1) {
            log.info('1 match (' + matches + ') for: ' + uri)
        } else {
            log.info(nMatches + ' matches (' + matches + ') for: ' + uri)
        }

        def downloaderArgs = command.getDownloader()
        def transcoderArgs = command.getTranscoder()

        if (transcoderArgs == null) { // using MEncoder: prepend the executable and append the URI
            transcoderArgs = newArgs

            if (downloaderArgs == null) {
                transcoderArgs.add(0, executable())
                transcoderArgs.add(newStash.get('$URI'))
            } else {
                // XXX ffs: http://jira.codehaus.org/browse/GROOVY-2225
                transcoderArgs.add(0, (isWindows ? executable().replaceAll(~'/', '\\\\') : executable()))
                transcoderArgs.add(isWindows ? '-' : downloaderOutputPath)
            }
        }

        params.minBufferSize = params.minFileSize
        params.secondread_minsize = 100000
        params.log = true // send the command's stdout/stderr to debug.log

        def pw

        if ((downloaderArgs != null) && isWindows) {
            pw = handleDownloadWindows(downloaderArgs, transcoderArgs, params)
        } else {
            def cmdArray = new String[ transcoderArgs.size() ]
            transcoderArgs.toArray(cmdArray)
            log.info('transcoder command: ' + Arrays.toString(cmdArray))
            pw = new ProcessWrapperImpl(cmdArray, params)
        }

        if ((downloaderArgs != null) && !isWindows) {
            handleDownloadUnix(pw, downloaderArgs, downloaderOutputBasename)
        }

        // create the transcoder's mkfifo process
        // note: we could set this thread running before calling the match() method
        def pipe = mkfifo(pw, new PipeProcess(transcoderOutputBasename))

        // XXX it's safe to set this lazily because the input_pipes array is not used by the ProcessWrapperImpl
        // constructor above. the alternative is to call pw.attachProcess() ourselves every time we
        // create a mkfifo process
        params.input_pipes[0] = pipe

        pw.runInNewThread()
        sleepFor(200)
        return pw
    }

    private ProcessWrapperImpl handleDownloadWindows(
        List<String> downloaderArgs,
        List<String> transcoderArgs,
        OutputParams params
    ) {
        def cmdList = [ "cmd.exe", "/C" ] + downloaderArgs + "|" + transcoderArgs
        def cmdArray = new String[ cmdList.size() ]

        cmdList.toArray(cmdArray)

        def pw = new ProcessWrapperImpl(cmdArray, params)

        log.info('command: ' + Arrays.toString(cmdArray))
        return pw
    }

    private void handleDownloadUnix(ProcessWrapperImpl pw, List<String> downloaderArgs, String downloaderOutputBasename) {
        mkfifo(pw, new PipeProcess(downloaderOutputBasename))

        def cmdArray = new String[ downloaderArgs.size() ]
        downloaderArgs.toArray(cmdArray)

        def params = new OutputParams(configuration)
        params.log = true

        def downloader = new ProcessWrapperImpl(cmdArray, params)
        pw.attachProcess(downloader)
        log.info('downloader command: ' + Arrays.toString(cmdArray))
        downloader.runInNewThread()
    }
}
