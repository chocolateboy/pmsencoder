package com.chocolatey.pmsencoder;

import java.io.IOException;

import javax.swing.JComponent;

import net.pms.configuration.PmsConfiguration;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.encoders.MEncoderWebVideo;
import net.pms.formats.Format;
import net.pms.io.OutputParams;
import net.pms.io.PipeProcess;
import net.pms.io.ProcessWrapper;
import net.pms.io.ProcessWrapperImpl;

object PMSEncoder {
    val ID = "pmsencoder";
}

class PMSEncoder(configuration: PmsConfiguration) extends MEncoderWebVideo(configuration) {
    override def id(): String = PMSEncoder.ID;
    override def mimeType(): String = "video/mpeg";
    override def name = "PMSEncoder"

    override protected def getDefaultArgs(): Array[String] = Array[String](
        "-prefer-ipv4",
        "-oac", "lavc",
        "-of", "lavf",
        "-lavfopts", "format=dvd",
        "-ovc", "lavc",
        "-lavcopts", "vcodec=mpeg2video:vbitrate=4096:threads=2:acodec=ac3:abitrate=128",
        "-ofps", "25",
        "-cache", "16384",
        "-vf", "harddup"
    );
    
    /*
    @Override
    public ProcessWrapper launchTranscode(String fileName, DLNAMediaInfo media, OutputParams params) throws IOException {
	params.minBufferSize = params.minFileSize;
	params.secondread_minsize = 100000;

	PipeProcess pipe = new PipeProcess("pmsencoder" + System.currentTimeMillis());

	params.input_pipes[0] = pipe;

	String cmdArray [] = new String [args().length + 4];

	cmdArray[0] = executable();
	cmdArray[1] = fileName;

	for(int i = 0; i < args().length; ++i) {
	    cmdArray[ i + 2 ] = args()[i];
	}

	cmdArray[cmdArray.length-2] = "-o";
	cmdArray[cmdArray.length-1] = pipe.getInputPipe();
	ProcessWrapper mkfifo_process = pipe.getPipeProcess();

	ProcessWrapperImpl pw = new ProcessWrapperImpl(cmdArray, params);

	pw.attachProcess(mkfifo_process);
	mkfifo_process.runInNewThread();

	try {
	    Thread.sleep(50);
	} catch (InterruptedException e) {}

	pipe.deleteLater();

	pw.runInNewThread();

	try {
	    Thread.sleep(50);
	} catch (InterruptedException e) { }

	return pw;
    }
    */
}
