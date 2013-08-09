package com.chocolatey.pmsencoder

import static com.chocolatey.pmsencoder.Util.shellQuote

import com.sun.jna.Platform

import java.util.Collections

import net.pms.PMS
import net.pms.configuration.PmsConfiguration
import net.pms.dlna.DLNAMediaInfo
import net.pms.dlna.DLNAResource
import net.pms.encoders.FFmpegWebVideo
import net.pms.encoders.Player
import net.pms.util.PlayerUtil
import net.pms.io.OutputParams
import net.pms.io.ProcessWrapper
import net.pms.io.ProcessWrapperImpl
import net.pms.network.HTTPResource

// import net.pms.encoders.FFmpegVideo // TODO add support for video transcode profiles

@groovy.transform.CompileStatic
@groovy.util.logging.Log4j(value="logger")
public class PMSEncoder extends FFmpegWebVideo {
    public static final boolean isWindows = Platform.isWindows()
    private Plugin plugin
    private final PmsConfiguration configuration
    private final int nbCores

    // FIXME make this private when PMS makes it private
    public static final String ID = 'pmsencoder'

    private long currentThreadId() {
        Thread.currentThread().getId()
    }

    @Override
    public String mimeType() {
        return HTTPResource.VIDEO_TRANSCODE // i.e. the mime-type that matches the renderer's VideoTranscode profile
    }

    @Override
    public String name() {
        'PMSEncoder'
    }

    private String normalizePath(String path) {
        return isWindows ? path.replaceAll(~'/', '\\\\') : path
    }

    @Override
    public String id() {
        ID
    }

    public PMSEncoder(PmsConfiguration configuration, Plugin plugin) {
        // FIXME temporary hack for 1.90.0 compatibility.
        // the null FFmpegProtocols instance is required to work around
        // the fact that the FFMpegWebVideo constructor requires this parameter.
        // null is OK (for the time being) since we don't need to call any
        // FFmpegWebVideo methods that use the protocols object
        super(configuration, null)
        this.configuration = configuration
        this.plugin = plugin
        this.nbCores = configuration.getNumberOfCpuCores()
    }

    @Override
    public boolean isCompatible(DLNAResource dlna) {
        // support every protocol i.e. allow scripts
        // to handle custom protocols
        return PlayerUtil.isWebVideo(dlna)
    }

    @Override
    @groovy.transform.CompileStatic(groovy.transform.TypeCheckingMode.SKIP)
    public ProcessWrapper launchTranscode(
        DLNAResource dlna,
        DLNAMediaInfo media,
        OutputParams params
    ) throws IOException {
        def oldURI = dlna.getSystemName()
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
        def ffmpegPath = normalizePath(configuration.getFfmpegPath())
        def command = new Command()
        def oldStash = command.getStash()

        command.setDlna(dlna)
        command.setMedia(media)
        command.setParams(params)
        command.setFfmpegPath(ffmpegPath)

        oldStash.put('uri', oldURI)

        plugin.match(command)

        // the whole point of the command abstraction is that the stash Map/transcoder command List
        // can be changed by the matcher, so make sure we refresh
        def newStash = command.getStash()

        // FIXME: groovy++ type inference fail: the subscript and/or concatenation operations
        // on downloaderArgs and transcoderArgs are causing groovy++ to define them as
        // Collection<String> rather than List<String>
        List<String> hookArgs = command.getHook()
        List<String> downloaderArgs = command.getDownloader()
        List<String> transcoderArgs = command.getTranscoder()
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

        // automagically add extra command-line options for the PMS-native downloaders/transcoders
        // and substitute the configured path for 'FFMPEG'
        if (transcoderArgs) {
            Collections.replaceAll(transcoderArgs, 'DOWNLOADER_OUT', downloaderOutputPath)
            Collections.replaceAll(transcoderArgs, 'TRANSCODER_OUT', transcoderOutputPath)

            def transcoderName = transcoderArgs[0]

            if (transcoderName == 'FFMPEG') {
                def transcoderInput = downloaderArgs ? downloaderOutputPath : newURI

                /*
                    before:

                         ffmpeg -loglevel warning -y -threads nbcores

                    after (with downloader):

                         /path/to/ffmpeg -loglevel warning -y -threads nbcores -i DOWNLOADER_OUT \
                            -threads nbcores -output-args TRANSCODER_OUT

                    after (without downloader):

                         /path/to/ffmpeg -loglevel warning -y -threads nbcores -i URI -threads nbcores \
                             -output-args TRANSCODER_OUT
                */

                // handle TranscodeVideo=WMV|MPEGTSAC3|MPEGPSAC3
                // and audio/video bitrates
                def renderer = params.mediaRenderer

                transcoderArgs[0] = ffmpegPath

                // List<String>: more Groovy++ type-inference lamery
                List<String> args = [
                    // end input args
                    '-i', transcoderInput,
                    // output args
                    '-threads', "" + nbCores
                ]

                transcoderArgs.addAll(args)

                List<String> audioBitrateOptions = command.getAudioBitrateOptions()
                List<String> videoBitrateOptions = command.getVideoBitrateOptions()
                List<String> videoTranscodeOptions = command.getVideoTranscodeOptions()

                if (audioBitrateOptions == null) {
                    transcoderArgs.addAll(getAudioBitrateOptions(dlna, media, params))
                } else {
                    transcoderArgs.addAll(audioBitrateOptions)
                }

                if (videoBitrateOptions == null) {
                    transcoderArgs.addAll(getVideoBitrateOptions(dlna, media, params))
                } else {
                    transcoderArgs.addAll(videoBitrateOptions)
                }

                if (videoTranscodeOptions == null) {
                    transcoderArgs.addAll(getVideoTranscodeOptions(dlna, media, params))
                } else {
                    transcoderArgs.addAll(videoTranscodeOptions)
                }

                transcoderArgs.add(transcoderOutputPath)
            }
        }

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
