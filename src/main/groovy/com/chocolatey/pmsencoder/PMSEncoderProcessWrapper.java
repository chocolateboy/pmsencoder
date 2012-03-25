/*
    chocolateboy 2011-01-05

    this is a cut-down version of shagrath's net/pms/io/ProcessWrapperImpl.java
    the main bugfix is: the stderr consumer is started *before* the process has
    finished rather than after (!). This finally means one can see the error
    in the debug.log, rather than having to copy, paste, tweak and re-run the
    command on the command-line

    the buggy BufferedOutputFile implementation will likely be replaced at some
    stage as well
*/
package com.chocolatey.pmsencoder;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.pms.io.BufferedOutputFile;
import net.pms.io.OutputBufferConsumer;
import net.pms.io.OutputConsumer;
import net.pms.io.OutputParams;
import net.pms.io.ProcessWrapper;
import net.pms.PMS;
import net.pms.util.ProcessUtil;

public class PMSEncoderProcessWrapper extends Thread implements ProcessWrapper {
    private String cmdLine;
    private Process process;
    private OutputConsumer stdoutConsumer;
    private UnfilteredOutputTextConsumer stderrConsumer;
    private OutputParams params;
    private boolean destroyed;
    private String[] cmdArray;
    private boolean nullable;
    private ArrayList<ProcessWrapper> attachedProcesses;
    private BufferedOutputFile bufferedOutputFile = null;
    private boolean success;

    @Override
    public String toString() {
        return super.getName();
    }

    public boolean isSuccess() {
        return success;
    }

    public PMSEncoderProcessWrapper(String cmdArray [], OutputParams params) {
        super(cmdArray[0]);
        File exec = new File(cmdArray[0]);

        if (exec.exists() && exec.isFile()) {
            cmdArray[0] = exec.getAbsolutePath();
        }

        this.cmdArray = cmdArray;
        StringBuffer sb = new StringBuffer("");

        for (int i = 0; i <cmdArray.length; ++i) {
            if (i > 0) {
                sb.append(" ");
            }
            sb.append(cmdArray[i]);
        }

        cmdLine = sb.toString();
        this.params = params;
        attachedProcesses = new ArrayList<ProcessWrapper>();
    }

    public void attachProcess(ProcessWrapper process) {
        attachedProcesses.add(process);
    }

    public void run() {
        ProcessBuilder pb = new ProcessBuilder(cmdArray);

        try {
            PMS.info("Starting " + cmdLine);

            // not used/exposed yet, but could be
            if (params.workDir != null && params.workDir.isDirectory()) {
                pb.directory(params.workDir);
            }

            process = pb.start();
            PMS.get().currentProcesses.add(process);
            stderrConsumer = new UnfilteredOutputTextConsumer(process.getErrorStream());
            stderrConsumer.start();
            stdoutConsumer = null;

            if (params.input_pipes[0] != null) {
                PMS.info("Reading pipe: " + params.input_pipes[0].getInputPipe());
                bufferedOutputFile = params.input_pipes[0].getDirectBuffer();
                if (bufferedOutputFile == null) { // null on non-Windows
                    InputStream is = params.input_pipes[0].getInputStream();
                    stdoutConsumer = new OutputBufferConsumer(is, params);
                    bufferedOutputFile = (BufferedOutputFile) stdoutConsumer.getBuffer();
                }
                bufferedOutputFile.attachThread(this);
                new UnfilteredOutputTextConsumer(process.getInputStream()).start();
            } else {
                stdoutConsumer = new UnfilteredOutputTextConsumer(process.getInputStream());
            }

            if (stdoutConsumer != null) {
                stdoutConsumer.start();
            }

            process.waitFor();

            try {
                if (stdoutConsumer != null) {
                    stdoutConsumer.join(1000);
                }
            } catch (InterruptedException e) {}

            if (bufferedOutputFile != null) {
                bufferedOutputFile.close();
            }
        } catch (Exception e) {
            PMS.error("Fatal error in process starting: ", e);
            stopProcess();
        } finally {
            if (!destroyed && !params.noexitcheck) {
                try {
                    success = true;
                    if (process != null && process.exitValue() != 0) {
                        PMS.minimal("Process " + cmdArray[0] + " has a return code of " + process.exitValue() + "! Maybe an error occured... check the log file");
                        success = false;
                    }
                } catch (IllegalThreadStateException itse) {
                    PMS.error("An error occured", itse);
                }
            }

            if (attachedProcesses != null) {
                for (ProcessWrapper pw: attachedProcesses) {
                    if (pw != null) {
                        pw.stopProcess();
                    }
                }
            }

            PMS.get().currentProcesses.remove(process);
        }
    }

    public void runInNewThread() {
        this.start();
    }

    public InputStream getInputStream(long seek) throws IOException {
        if (bufferedOutputFile != null) {
            return bufferedOutputFile.getInputStream(seek);
        } else {
            return null;
        }
    }

    public List<String> getResults() {
        return null;
    }

    public void stopProcess() {
        PMS.info("Stopping process: " + this);
        destroyed = true;

        if (process != null) {
            ProcessUtil.destroy(process);
        }

        if (attachedProcesses != null) {
            for (ProcessWrapper pw: attachedProcesses) {
                if (pw != null)
                    pw.stopProcess();
            }
        }

        if (stdoutConsumer != null && stdoutConsumer.getBuffer() != null) {
            stdoutConsumer.getBuffer().reset();
        }
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    public boolean isReadyToStop() {
        return nullable;
    }

    public void setReadyToStop(boolean nullable) {
        if (nullable != this.nullable) {
            PMS.debug("Ready to Stop: " + nullable);
        }
        this.nullable = nullable;
    }
}
