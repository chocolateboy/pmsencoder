package com.chocolatey.pmsencoder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;

import com.chocolatey.pmsencoder.Matcher;
import com.chocolatey.pmsencoder.Command;

import net.pms.configuration.PmsConfiguration;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.encoders.MEncoderWebVideo;
import net.pms.formats.Format;
import net.pms.io.OutputParams;
import net.pms.io.PipeProcess;
import net.pms.io.ProcessWrapper;
import net.pms.io.ProcessWrapperImpl;
import net.pms.PMS;

import org.apache.log4j.Logger;

public class Engine extends MEncoderWebVideo {
    public static final String ID = "pmsencoder";
    private Matcher matcher;
    private final PmsConfiguration configuration;
    private Logger log;

    @Override
    public String id() {
        return ID;
    }

    public Engine(PmsConfiguration configuration, Matcher matcher) {
        super(configuration);
        this.configuration = configuration;
        this.matcher = matcher;
        this.log = Logger.getLogger(this.getClass().getName());
    }

    @Override
    public ProcessWrapper launchTranscode(String uri, DLNAMediaInfo media, OutputParams params) throws IOException {
        PipeProcess pipe = new PipeProcess("pmsencoder" + System.currentTimeMillis());
        String outfile = pipe.getInputPipe();
        Command command = new Command();
        Stash stash = command.getStash();

        stash.put("URI", uri);
        stash.put("EXECUTABLE", executable());
        stash.put("OUTPUT", outfile);

        List<String> matches;

        log.info("invoking matcher for: " + uri);

        try {
            matches = matcher.match(command);
            int nMatches = matches.size();

            if (nMatches == 1) {
                log.info("1 match for: " + uri);
            } else {
                log.info(nMatches + " matches for: " + uri);
            }
        } catch (Throwable e) {
            log.error("match error: " + e);
            PMS.error("match error", e);
        }

        List<String> args = command.getArgs();
        args.add(0, stash.get("EXECUTABLE"));

        /*
         * if it's still an MEncoder command, add "-o /tmp/javaps3media/psmesencoder1234";
         * otherwise assume the matching action has defined the whole command,
         * including the output file option
         */
        if (args.get(0).equals(executable()) && !(args.contains("-o"))) {
            args.add("-o");
            args.add(outfile);
        }

        params.input_pipes[0] = pipe;
        params.minBufferSize = params.minFileSize;
        params.secondread_minsize = 100000;
        params.log = true;

        String cmdArray[] = new String[ args.size() ];
        args.toArray(cmdArray);

        ProcessWrapper mkfifo_process = pipe.getPipeProcess();
        ProcessWrapperImpl pw = new ProcessWrapperImpl(cmdArray, params);
        pw.attachProcess(mkfifo_process);
        mkfifo_process.runInNewThread();

        try {
            // Thread.sleep(50);
            Thread.sleep(200);
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
