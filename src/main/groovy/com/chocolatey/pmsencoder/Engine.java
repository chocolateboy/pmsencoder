package com.chocolatey.pmsencoder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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

import org.apache.commons.lang.StringUtils;
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
          shellCmdArray = new String[]{ "/bin/sh", "-c" };
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
        String cmdArray[];

        if (executable == "SHELL") {
            cmdArray = new String[ shellCmdArray.length + 1 ];
            for (int i = 0; i < shellCmdArray.length; ++i) {
                cmdArray[i] = shellCmdArray[i];
            }
            cmdArray[ shellCmdArray.length ] = StringUtils.join(args, " ");
        } else {
            args.add(0, executable);
            if (executable == executable()) {
                args.add(stash.get("$URI"));
            }
            cmdArray = new String[ args.size() ];
            args.toArray(cmdArray);
        }

        log.info("command: " + Arrays.toString(cmdArray));

        params.input_pipes[0] = pipe;
        params.minBufferSize = params.minFileSize;
        params.secondread_minsize = 100000;
        params.log = true; // send the command's stdout/stderr to debug.log


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
