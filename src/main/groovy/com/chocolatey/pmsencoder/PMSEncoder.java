package net.pms.encoders;

import java.io.IOException;

import javax.swing.JComponent;

import com.chocolatey.pmsencoder.Matcher;
import com.chocolatey.pmsencoder.Stash;

import net.pms.configuration.PmsConfiguration;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.formats.Format;
import net.pms.io.OutputParams;
import net.pms.io.PipeProcess;
import net.pms.io.ProcessWrapper;
import net.pms.io.ProcessWrapperImpl;

import org.apache.log4j.Logger;

public class PMSEncoder extends MEncoderWebVideo {
    public static final String ID = "pmsencoder";
    private Matcher matcher;
    private final PmsConfiguration configuration;
    private Logger log;

    @Override
    public String id() {
	return ID;
    }

    public PMSEncoder(PmsConfiguration configuration, Matcher matcher) {
        super(configuration);
        this.configuration = configuration;
        this.matcher = matcher;
	this.log = Logger.getLogger(this.getClass().getName());
    }

    @Override
    public ProcessWrapper launchTranscode(String uri, DLNAMediaInfo media, OutputParams params) throws IOException {
	Stash stash = new Stash();
	stash.put("uri", uri);
	List<String> args = new List<String>();

	log.info("looking for match for " + uri);

	try {
	    if (matcher.match(stash, args)) {
		log.info("matched " + uri);
	    } else {
		log.info("didn't match " + uri);
	    }
	} catch (Throwable e) {
	    log.error(e);
	}

	params.minBufferSize = params.minFileSize;
	params.secondread_minsize = 100000;

	PipeProcess pipe = new PipeProcess("pmsencoder" + System.currentTimeMillis());
	params.input_pipes[0] = pipe;

	String cmdArray[] = new String[ args.size() + 4 ];
	cmdArray[0] = executable();
	cmdArray[1] = stash.get("uri");

	for (int i = 0; i < args.size(); ++i) {
	    cmdArray[ i + 2 ] = args.get(i);
	}

	cmdArray[ cmdArray.length - 2 ] = "-o";
	cmdArray[ cmdArray.length - 1 ] = pipe.getInputPipe();

	ProcessWrapper mkfifo_process = pipe.getPipeProcess();

	ProcessWrapperImpl pw = new ProcessWrapperImpl(cmdArray, params);
	pw.attachProcess(mkfifo_process);
	mkfifo_process.runInNewThread();

	try {
	    Thread.sleep(50);
	} catch (InterruptedException e) { }

	pipe.deleteLater();
	pw.runInNewThread();

	try {
	    Thread.sleep(50);
	} catch (InterruptedException e) { }

	return pw;
    }

    @Override
    public String name() {
	return "PMSEncoder";
    }
}
