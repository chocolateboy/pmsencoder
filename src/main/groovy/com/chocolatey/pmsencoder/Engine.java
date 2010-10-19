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

    @Override
    // temporarily log calls to this method so we can figure out its scope
    public String mimeType() {
        // log.info("mimeType called");
        return "video/mpeg";
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

    private String getPipePath(String name) {
        try {
            return PMS.get().isWindows() ? "\\\\.\\pipe\\" + name : PMS.getConfiguration().getTempFolder() + "/" + name;
        } catch (IOException e) {
            PMS.error("Pipe may not be in temporary directory", e);
            return name;
        }
    }

    @Override
    public ProcessWrapper launchTranscode(String uri, DLNAMediaInfo media, OutputParams params) throws IOException {
        log.info("media: " + media); // XXX
        log.info("params: " + params); // XXX
        long now = System.currentTimeMillis();
        PipeProcess pipe = new PipeProcess("pmsencoder_transcoder_" + now);
        String outfile = pipe.getInputPipe();
        String downloaderPipeBasename = "pmsencoder_downloader_" + now;
        String downloaderPipePath = getPipePath(downloaderPipeBasename);
        Command command = new Command();
        command.setParams(params);
        Stash oldStash = command.getStash();
        List<String> oldArgs = command.getArgs();

        oldStash.put("$URI", uri);
        oldStash.put("$EXECUTABLE", executable());
        oldStash.put("$OUTPUT", outfile);
        oldStash.put("$DOWNLOADER_OUTPUT", downloaderPipePath);

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

        if (nMatches == 0) {
            log.info("0 matches for: " + uri);
        } else if(nMatches == 1) {
            log.info("1 match (" + matches + ") for: " + uri);
        } else {
            log.info(nMatches + " matches (" + matches + ") for: " + uri);
        }

        String downloaderString = stash.get("$DOWNLOADER");
        String[] downloaderArray = null;

        if (downloaderString != null) {
            downloaderArray = StringUtils.split(downloaderString, null); // split on whitespace
        }

        String executable = stash.get("$EXECUTABLE");
        String cmdArray[];

        // cmdArray[ shellCmdArray.length ] = StringUtils.join(args, " ");

        args.add(0, executable);
        if (executable == executable()) {
            args.add(stash.get("$URI"));
        }

        cmdArray = new String[ args.size() ];
        args.toArray(cmdArray);

        if (downloaderArray != null) {
            log.info("downloader: " + downloaderString);
        }

        log.info("command: " + Arrays.toString(cmdArray));

        params.input_pipes[0] = pipe;
        params.minBufferSize = params.minFileSize;
        params.secondread_minsize = 100000;
        params.log = true; // send the command's stdout/stderr to debug.log

        ProcessWrapper mkfifo_process = pipe.getPipeProcess();
        ProcessWrapperImpl pw = new ProcessWrapperImpl(cmdArray, params);

        PipeProcess downloaderPipe = null;

        if (downloaderArray != null) {
            // create the downloader's mkfifo process
            downloaderPipe = new PipeProcess(downloaderPipeBasename);
            ProcessWrapper downloaderMkfifoProcess = downloaderPipe.getPipeProcess();

            pw.attachProcess(downloaderMkfifoProcess);
            pw.attachProcess(mkfifo_process);

            downloaderMkfifoProcess.runInNewThread();
            mkfifo_process.runInNewThread();

            try {
                Thread.sleep(350);
            } catch (InterruptedException e) { }

            downloaderPipe.deleteLater();
            pipe.deleteLater();

            // create the downloader process
            OutputParams downloaderParams = new OutputParams(configuration);
            downloaderParams.log = true;
            ProcessWrapperImpl downloaderProcess = new ProcessWrapperImpl(downloaderArray, downloaderParams);
            pw.attachProcess(downloaderProcess);
            downloaderProcess.runInNewThread();

            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                PMS.error("thread interrupted", e);
            }
        } else {
            pw.attachProcess(mkfifo_process);
            mkfifo_process.runInNewThread();

            try {
                Thread.sleep(300);
            } catch (InterruptedException e) { }

            pipe.deleteLater();
        }

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
