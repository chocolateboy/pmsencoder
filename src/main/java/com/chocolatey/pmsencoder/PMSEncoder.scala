package com.chocolatey.pmsencoder

// import com.chocolatey.pmsencoder.Matcher

import java.io.IOException

import javax.swing.JComponent

import net.pms.configuration.PmsConfiguration
import net.pms.dlna.DLNAMediaInfo
import net.pms.encoders.MEncoderWebVideo
import net.pms.formats.Format
import net.pms.io.OutputParams
import net.pms.io.PipeProcess
import net.pms.io.ProcessWrapper
import net.pms.io.ProcessWrapperImpl

object PMSEncoder {
    val ID = "pmsencoder"

    def defaultArgs: Array[String] = Array[String](
        "-prefer-ipv4",
        "-oac", "lavc",
        "-of", "lavf",
        "-lavfopts", "format=dvd",
        "-ovc", "lavc",
        "-lavcopts", "vcodec=mpeg2video:vbitrate=4096:threads=2:acodec=ac3:abitrate=128",
        "-ofps", "25",
        "-cache", "16384",
        "-vf", "harddup"
    )
}

class PMSEncoder(configuration: PmsConfiguration, matcher: Matcher) extends MEncoderWebVideo(configuration) {
    override def id = PMSEncoder.ID
    override def name = "PMSEncoder"

    override def launchTranscode(_filename: String, media: DLNAMediaInfo, params: OutputParams): ProcessWrapper = {
        val (filename, mencoder_args) = matcher.match_uri(_filename, PMSEncoder.defaultArgs)
        return new PMSEncoderWithArgs(configuration, mencoder_args).launchTranscode(filename, media, params)
    }
}

class PMSEncoderWithArgs(cfg: PmsConfiguration, mencoder_args: Array[String]) extends MEncoderWebVideo(cfg) {
    override def args = mencoder_args
}
