@Typed
package com.chocolatey.pmsencoder

import static com.chocolatey.pmsencoder.Util.quoteURI

import java.util.Collections

import net.pms.configuration.PmsConfiguration
import net.pms.dlna.DLNAMediaInfo
import net.pms.dlna.DLNAResource
import net.pms.encoders.FFMpegWebVideo
import net.pms.encoders.Player
import net.pms.io.OutputParams
import net.pms.io.ProcessWrapper
import net.pms.network.HTTPResource
import net.pms.PMS

// import net.pms.encoders.FFMpegVideo // TODO add support for video transcode profiles

public class PMSEncoder extends FFMpegWebVideo implements LoggerMixin {
    public static final boolean isWindows = PMS.get().isWindows()
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
        super(configuration)
        this.configuration = configuration
        this.plugin = plugin
        this.nbCores = configuration.getNumberOfCpuCores()
    }

    @Override
    public ProcessWrapper launchTranscode(
        String oldURI,
        DLNAResource dlna,
        DLNAMediaInfo media,
        OutputParams params
    ) throws IOException {
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

        command.setDlna(dlna)
        command.setMedia(media)
        command.setParams(params)

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
        def newURI = quoteURI(newStash.get('uri')?.toString())

        if (hookArgs) {
            processManager.handleHook(hookArgs)
        }

        // automagically add extra command-line options for the PMS-native downloaders/transcoders
        // and substitute the configured path for 'FFMPEG'

        def ffmpegPath = normalizePath(configuration.getFfmpegPath())

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
                    '-threads', nbCores
                ]

                transcoderArgs.addAll(args)
                transcoderArgs.addAll(getVideoBitrateOptions(renderer, media))
                transcoderArgs.addAll(getAudioBitrateOptions(renderer, media))
                transcoderArgs.addAll(getTranscodeVideoOptions(renderer, media))
                transcoderArgs.add(transcoderOutputPath)
            }
        }

        // Groovy's "documentation" doesn't make it clear whether local variables are null-initialized
        // http://stackoverflow.com/questions/4025222
        def transcoderProcess = null

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
