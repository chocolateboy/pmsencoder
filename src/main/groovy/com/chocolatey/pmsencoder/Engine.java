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
    private static final String[] shellCmdArray;

    // via http://ostermiller.org/utils/ExecHelper.java.html
    static {
        String os = System.getProperty("os.name");

        if (os.equals("Windows 95") || os.equals("Windows 98") || os.equals("Windows ME")) {
          shellCmdArray = new String[]{ "command.exe", "/C" };
        } else if (os.startsWith("Windows")){
          shellCmdArray = new String[]{ "cmd.exe", "/C" };
        } else {
          shellCmdArray = new String[]{ "/bin/sh" };
        }
    }

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
        log.info("mediainfo: " + media);
        PipeProcess pipe = new PipeProcess("pmsencoder" + System.currentTimeMillis());
        String outfile = pipe.getInputPipe();
        Command command = new Command();
        Stash oldStash = command.getStash();
        List<String> oldArgs = command.getArgs();

        oldStash.put("$URI", uri);
        oldStash.put("$EXECUTABLE", executable());
        oldStash.put("$OUTPUT", outfile);

        oldArgs.add("-o");
        oldArgs.add(outfile);

        log.info("invoking matcher for: " + uri);

        try {
            matcher.match(command);
        } catch (Throwable e) {
            log.error("match error: " + e);
            PMS.error("match error", e);
        }

        // the whole point of the command abstraction is that the stash-Map/args-List
        // can be changed by the matcher; so make sure to refresh
        Stash stash = command.getStash();
        List<String> args = command.getArgs();
        List<String> matches = command.getMatches();
        int nMatches = matches.size();

        if (nMatches == 1) {
            log.info("1 match for: " + uri);
        } else {
            log.info(nMatches + " matches for: " + uri);
        }

        String executable = stash.get("$EXECUTABLE");

        int postExecutable;

        if (executable == "SHELL") {
            int i;
            for (i = 0; i < shellCmdArray.length; ++i) {
                args.add(i, shellCmdArray[i]);
            }
            postExecutable = i + 1;
        } else {
            args.add(0, executable);
            postExecutable = 1;
        }

        /*
         * if it's still an MEncoder command, add "$URI -o /tmp/javaps3media/psmesencoder1234";
         * otherwise assume the matching action has defined the URI and any output option(s)
         */
        String addURI = stash.get("$ADD_URI");

        if (addURI != null) {
            if (addURI == "prepend") {
                args.add(postExecutable, stash.get("$URI"));
            } else if (addURI == "append") {
                args.add(stash.get("$URI"));
            } else {
                log.info("skipping URI append/prepend: $ADD_URI: " + addURI);
            }
        }

        log.info("command: " + args);

        params.input_pipes[0] = pipe;
        params.minBufferSize = params.minFileSize;
        params.secondread_minsize = 100000;
        params.log = true; // send the command's stdout/stderr to debug.log

        String cmdArray[] = new String[ args.size() ];
        args.toArray(cmdArray);

        ProcessWrapper mkfifo_process = pipe.getPipeProcess();
        ProcessWrapperImpl pw = new ProcessWrapperImpl(cmdArray, params);
        pw.attachProcess(mkfifo_process);
        mkfifo_process.runInNewThread();

        try {
            Thread.sleep(300);
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
