package com.chocolatey.pmsencoder

import com.sun.jna.Platform
import groovy.transform.CompileStatic
import groovy.util.logging.Log4j
import net.pms.PMS
import net.pms.configuration.PmsConfiguration
import net.pms.dlna.DLNAMediaInfo
import net.pms.dlna.DLNAResource
import net.pms.encoders.FFmpegWebVideo
import net.pms.io.OutputParams
import net.pms.io.ProcessWrapper
import net.pms.io.ProcessWrapperImpl
import net.pms.network.HTTPResource
import net.pms.util.PlayerUtil

import static Util.cmdListToArray
import static Util.shellQuote

@CompileStatic
@Log4j(value='logger')
public class PMSEncoder extends FFmpegWebVideo {
    private final int nbCores
    private Plugin plugin
    public static final boolean isWindows = Platform.isWindows()
    private static final PmsConfiguration configuration = PMS.getConfiguration()
    private static final String FFMPEG_PATH = normalizePath(configuration.getFfmpegPath())
    private static final String DEFAULT_QSCALE = '3'

    // FIXME make this private when PMS makes it private
    public static final String ID = Plugin.name.toLowerCase()

    private static long currentThreadId() {
        Thread.currentThread().getId()
    }

    @Override
    public String mimeType() {
        return HTTPResource.VIDEO_TRANSCODE // i.e. the mime-type that matches the renderer's VideoTranscode profile
    }

    @Override
    public String name() { Plugin.name }

    private static String normalizePath(String path) {
        return isWindows ? path.replaceAll(~'/', '\\\\') : path
    }

    @Override
    public String id() { ID }

    public PMSEncoder(Plugin plugin) {
        // FIXME temporary hack for 1.90.0 compatibility.
        // the null FFmpegProtocols instance is required to work around
        // the fact that the FFMpegWebVideo constructor requires this parameter.
        // null is OK (for the time being) since we don't need to call any
        // FFmpegWebVideo methods that use the protocols object
        super(configuration, null)
        this.plugin = plugin
        this.nbCores = configuration.getNumberOfCpuCores()
    }

    // by default, we support every protocol i.e. allow scripts
    // to handle custom protocols
    @Override
    public boolean isCompatible(DLNAResource dlna) {
        if (PlayerUtil.isWebVideo(dlna)) {
            // allow PMSEncoder to be disabled for resources
            // that are better handled by another engine (e.g. VLC)
            // http://www.ps3mediaserver.org/forum/viewtopic.php?f=6&t=16828&p=79334#p79334
            def command = new Command()
            command.setEvent(Event.INCOMPATIBLE)
            command.setDlna(dlna)

            def stash = command.getStash()
            stash.put('uri', dlna.getSystemName())

            // disable PMSEncoder for this resource if any
            // profiles match this event/resource.
            // false: log/dump the command at trace
            // level (verbose = false) rather than the default
            // debug level (verbose = true) to avoid logspam
            return !plugin.match(command, false)
        } else {
            return false
        }
    }

    @SuppressWarnings('GroovyUnsynchronizedMethodOverridesSynchronizedMethod')
    @Override
    public ProcessWrapper launchTranscode(
        DLNAResource dlna,
        DLNAMediaInfo media,
        OutputParams params
    ) throws IOException {
        def oldURI = dlna.getSystemName()
        def processManager = new ProcessManager(this, params)
        def threadId = currentThreadId() // make sure concurrent threads don't use the same filename
        def uniqueId = threadId + '_' + System.currentTimeMillis()
        def transcoderOutputBasename = "pmsencoder_transcoder_out_${uniqueId}" // always used (read by PMS)
        def transcoderOutputPath = processManager.getFifoPath(transcoderOutputBasename)
        def downloaderOutputBasename = "pmsencoder_downloader_out_${uniqueId}"
        def downloaderOutputPath = isWindows ? '-' : processManager.getFifoPath(downloaderOutputBasename)

        // whatever happens, we need a transcoder output FIFO (even if there's a match error, we carry
        // on with the unmodified URI), so we can create that upfront
        processManager.createTranscoderFifo(transcoderOutputBasename)

        def command = new Command()
        command.setEvent(Event.TRANSCODE)
        command.setDlna(dlna)
        command.setMedia(media)
        command.setParams(params)

        def oldStash = command.getStash()
        oldStash.put('uri', oldURI)

        plugin.match(command)

        // the command abstraction allows the stash and command lists
        // to be changed by the matcher, so make sure we refresh
        def newStash = command.getStash()
        def hookArgs = command.getHook()
        def downloaderArgs = command.getDownloader()
        def transcoderArgs = command.getTranscoder()
        def newURI = newStash.get('uri')?.toString()

        // work around an ffmpeg bug:
        // http://ffmpeg.org/trac/ffmpeg/ticket/998
        // FIXME: use protocols.getFilename(newURI) when
        // FFmpegProtocols is fixed (PMS > 1.90.0)
        if (newURI.startsWith('mms:')) {
            newURI = 'mmsh:' + newURI.substring(4);
        }

        newURI = shellQuote(newURI)

        if (hookArgs) {
            processManager.handleHook(hookArgs)
        }

        // if the executable is 'FFMPEG', automagically add ffmpeg input and output options
        // and replace 'FFMPEG' with the configured ffmpeg path
        if (transcoderArgs) {
            Collections.replaceAll(transcoderArgs, 'DOWNLOADER_OUT', downloaderOutputPath)
            Collections.replaceAll(transcoderArgs, 'TRANSCODER_OUT', transcoderOutputPath)

            // XXX we could have two templates for the ffmpeg path: one (e.g. FFMPEG) with
            // the current behaviour and another (e.g. FFMPEG_PATH) which just substitutes
            // the path (i.e. for cases where the user wants full control over the command).
            // In practice, more control than is provided here shouldn't be needed (if it is,
            // the underlying issue should be fixed in PMS/PMSEncoder).
            if (transcoderArgs[0] == 'FFMPEG') {
                def transcoderInput = downloaderArgs ? downloaderOutputPath : newURI

                /*
                    before:

                         FFMPEG -global-args

                    after (without downloader):

                         /path/to/ffmpeg -global-args -user-args -i URI -output-args

                    after (with downloader):

                         /path/to/ffmpeg -global-args -user-args -i DOWNLOADER_OUT -output-args

                    i.e. user args can be global args (e.g. -loglevel) and/or input args (e.g. -r).
                    output options can be revoked/overridden via the bitrate and transcode options (see below)
                */

                // handle TranscodeVideo=WMV|MPEGTSAC3|MPEGPSAC3
                // and audio/video bitrates

                transcoderArgs[0] = FFMPEG_PATH

                // List<String>: more Groovy++ type-inference lamery
                List<String> args = [
                    // end input args
                    '-i', transcoderInput,
                    // output args
                    '-threads', '' + nbCores
                ]

                transcoderArgs.addAll(args)

                // try to preserve the video and audio quality
                //
                // XXX note: we ignore the renderer/global bitrate limits (for now)
                // since a) the PMS FFmpeg options are incomplete[1] and b) web video bitrates/bandwidth
                // shouldn't usually hit those limits. if they do, there are other options e.g.:
                //
                // 1) select lower-quality videos e.g. 640w Apple Trailers, YOUTUBE_DL_MAX_QUALITY &c.
                // 2) custom bitrate options can be set via a finalizeTranscoderArgs script
                //
                // [1] http://www.ps3mediaserver.org/forum/viewtopic.php?f=12&p=80836#p80836

                // set video bitrate options
                transcoderArgs.add('-q:v')
                transcoderArgs.add(DEFAULT_QSCALE)

                // set audio bitrate options
                transcoderArgs.add('-q:a')
                transcoderArgs.add(DEFAULT_QSCALE)

                transcoderArgs.addAll(getVideoTranscodeOptions(dlna, media, params))
                transcoderArgs.add(transcoderOutputPath)
            }
        }

        def cmdArray = finalizeTranscoderArgs(
            newURI,
            dlna,
            media,
            params,
            cmdListToArray(transcoderArgs)
        )

        transcoderArgs = cmdArray.toList()

        // Groovy's "documentation" doesn't make it clear whether local variables are null-initialized
        // http://stackoverflow.com/questions/4025222
        ProcessWrapperImpl transcoderProcess = null

        if (downloaderArgs) {
            Collections.replaceAll(downloaderArgs, 'DOWNLOADER_OUT', downloaderOutputPath)

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
