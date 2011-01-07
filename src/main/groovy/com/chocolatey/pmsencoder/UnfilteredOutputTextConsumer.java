package com.chocolatey.pmsencoder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import net.pms.PMS;
import net.pms.io.BufferedOutputFile;
import net.pms.io.OutputConsumer;

public class UnfilteredOutputTextConsumer extends OutputConsumer {
    public UnfilteredOutputTextConsumer(InputStream inputStream) {
        super(inputStream);
    }

    @Override
    public void run() {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            String line = null;
            while ((line = br.readLine()) != null) {
                PMS.debug(line);
            }
        } catch (IOException ioe) {
            PMS.info("Error consuming stream of spawned process: " +  ioe.getMessage());
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (Exception ignore) {}
            }
        }
    }

    @Override
    public BufferedOutputFile getBuffer() {
        return null;
    }

    @Override
    public List<String> getResults() {
        return null;
    }
}
