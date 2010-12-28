@Typed
package com.chocolatey.pmsencoder

import net.pms.configuration.PmsConfiguration
import net.pms.dlna.DLNAMediaInfo
import net.pms.encoders.MEncoderWebVideo
import net.pms.io.OutputParams
import net.pms.io.ProcessWrapper
import net.pms.PMS

import org.apache.log4j.Logger

public class Engine extends MEncoderWebVideo {
    public static final boolean isWindows = PMS.get().isWindows()
    private Plugin plugin
    private Logger log

    final PmsConfiguration configuration
    public static final String ID = 'pmsencoder'

    @Override
    public String mimeType() {
        'video/mpeg'
        // 'video/mp4'
    }

    @Override
    public String name() {
        'PMSEncoder'
    }

    @Override
    public String executable() {
        def executable = super.executable()
        // XXX ffs: http://jira.codehaus.org/browse/GROOVY-2225
        return isWindows ? executable.replaceAll(~'/', '\\\\') : executable
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

    @Override
    public ProcessWrapper launchTranscode(String uri, DLNAMediaInfo media, OutputParams params)
    throws IOException {
        def processManager = new ProcessManager(this, params)
        // FIXME: should really use a synchronized counter to ensure
        // concurrent requests don't use the same filenames
        def now = System.currentTimeMillis()
        def transcoderOutputBasename = 'pmsencoder_transcoder_out_' + now // always used (read by PMS)
        def transcoderOutputPath = processManager.getFifoPath(transcoderOutputBasename)
        def downloaderOutputBasename = 'pmsencoder_downloader_out_' + now
        def downloaderOutputPath = isWindows ? '-' : processManager.getFifoPath(downloaderOutputBasename)

        // whatever happens, we need a transcoder output FIFO (even if there's a match error, we carry
        // on with the unmodified URI), so we can create that upfront
        processManager.createTranscoderFifo(transcoderOutputBasename)

        def command = new Command()
        def oldStash = command.getStash()
        def oldArgs = command.getArgs()

        command.setParams(params)

        oldStash.put('$DOWNLOADER_OUT', downloaderOutputPath)
        oldStash.put('$MENCODER', configuration.getMencoderPath())
        oldStash.put('$MENCODER_MT', configuration.getMencoderMTPath())
        oldStash.put('$TRANSCODER_OUT', transcoderOutputPath)
        oldStash.put('$URI', uri)

        oldArgs.add('-o')
        oldArgs.add(transcoderOutputPath)

        log.info('invoking matcher for: ' + uri)

        try {
            plugin.match(command)
        } catch (Throwable e) {
            log.error('match error: ' + e)
            PMS.error('PMSEncoder: match error', e)
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

        def hookArgs = command.getHook()
        def downloaderArgs = command.getDownloader()
        def transcoderArgs = command.getTranscoder()

        if (hookArgs != null) {
            processManager.handleHook(hookArgs)
        }

        if (transcoderArgs == null) { // using MEncoder: prepend the executable and append the URI/downloader FIFO
            def transcoderInput = (downloaderArgs == null) ? newStash.get('$URI') : downloaderOutputPath
            transcoderArgs = newArgs
            transcoderArgs.add(0, executable())
            transcoderArgs.add(transcoderInput)
        }

        // Groovy's "documentation" doesn't answer make it clear whether local variables are null-initialized
        // http://stackoverflow.com/questions/4025222
        def transcoderProcess = null

        if (downloaderArgs != null) {
            if (isWindows) {
                transcoderProcess = processManager.handleDownloadWindows(downloaderArgs, transcoderArgs)
            } else {
                processManager.handleDownloadUnix(downloaderArgs, downloaderOutputBasename)
            }
        }

        if (transcoderProcess == null) {
            transcoderProcess = processManager.handleTranscode(transcoderArgs)
        }

        return processManager.launchTranscode(transcoderProcess)
    }
}
