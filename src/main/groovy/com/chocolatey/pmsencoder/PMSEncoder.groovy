@Typed
package com.chocolatey.pmsencoder

import net.pms.configuration.PmsConfiguration
import net.pms.dlna.DLNAMediaInfo
import net.pms.encoders.MEncoderWebVideo
import net.pms.io.OutputParams
import net.pms.io.ProcessWrapper
import net.pms.PMS

public class PMSEncoder extends MEncoderWebVideo implements LoggerMixin {
    public static final boolean isWindows = PMS.get().isWindows()
    private Plugin plugin
    private final static ThreadLocal threadLocal = new ThreadLocal<String>()
    private static final String DEFAULT_MIME_TYPE = 'video/mpeg'

    final PmsConfiguration configuration
    public static final String ID = 'pmsencoder'

    private long currentThreadId() {
        Thread.currentThread().getId()
    }

    @Override
    public String mimeType() {
        def mimeType = threadLocal.get()

        if (mimeType != null) { // transcode thread
            log.debug('thread id: ' + currentThreadId())
            log.info("getting custom mime type: $mimeType")
            threadLocal.remove() // remove it to prevent memory leaks
            return mimeType
        } else {
            return DEFAULT_MIME_TYPE
        }
    }

    @Override
    public String name() {
        'PMSEncoder'
    }

    private String normalizePath(String path) {
        return isWindows ? path.replaceAll(~'/', '\\\\') : path
    }

    @Override
    public String executable() {
        normalizePath(super.executable())
    }

    @Override
    public String id() {
        ID
    }

    public PMSEncoder(PmsConfiguration configuration, Plugin plugin) {
        super(configuration)
        this.configuration = configuration
        this.plugin = plugin
    }

    @Override
    public ProcessWrapper launchTranscode(String uri, DLNAMediaInfo media, OutputParams params)
    throws IOException {
        def processManager = new ProcessManager(this, params)
        def threadId = currentThreadId() // make sure concurrent threads don't use the same filename
        def uniqueId = System.currentTimeMillis() + '_' + threadId
        def transcoderOutputBasename = "pmsencoder_transcoder_out_${uniqueId}" // always used (read by PMS)
        def transcoderOutputPath = processManager.getFifoPath(transcoderOutputBasename)
        def downloaderOutputBasename = "pmsencoder_downloader_out_${uniqueId}"
        def downloaderOutputPath = isWindows ? '-' : processManager.getFifoPath(downloaderOutputBasename)

        // whatever happens, we need a transcoder output FIFO (even if there's a match error, we carry
        // on with the unmodified URI), so we can create that upfront
        processManager.createTranscoderFifo(transcoderOutputBasename)

        def command = new Command()
        def oldStash = command.getStash()
        def oldArgs = command.getArgs()

        command.setParams(params)

        oldStash.put('$DOWNLOADER_OUT', downloaderOutputPath)
        oldStash.put('$FFMPEG', normalizePath(configuration.getFfmpegPath()))
        oldStash.put('$MENCODER', normalizePath(configuration.getMencoderPath()))
        oldStash.put('$MENCODER_MT', normalizePath(configuration.getMencoderMTPath()))
        oldStash.put('$MPLAYER', normalizePath(configuration.getMplayerPath()))
        oldStash.put('$TRANSCODER_OUT', transcoderOutputPath)
        oldStash.put('$URI', uri)

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

        def mimeType = newStash.get('$MIME_TYPE')

        if (mimeType != null) {
            log.debug('thread id: ' + threadId)
            log.info("setting custom mime-type: $mimeType")
            threadLocal.set(mimeType)
        } else {
            threadLocal.remove() // remove it to prevent memory leaks
        }

        def hookArgs = command.getHook()
        def downloaderArgs = command.getDownloader()
        def transcoderArgs = command.getTranscoder()

        if (hookArgs != null) {
            processManager.handleHook(hookArgs)
        }

        // using MEncoder: prepend the executable, the output file, and the input file/URI
        if (transcoderArgs == null) {
            def transcoderInput = (downloaderArgs == null) ? newStash.get('$URI') : downloaderOutputPath
            transcoderArgs = newArgs
            transcoderArgs.add(0, executable())
            transcoderArgs.add(1, '-o')
            transcoderArgs.add(2, transcoderOutputPath)
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
