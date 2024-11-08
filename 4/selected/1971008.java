package com.nullfish.app.jfd2.ext_command;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

public class StreamReaderThread extends Thread {

    boolean working = false;

    BufferedReader reader;

    BufferedWriter writer;

    public StreamReaderThread(InputStream is, OutputStream os) {
        reader = new BufferedReader(new InputStreamReader(is));
        writer = new BufferedWriter(new OutputStreamWriter(os));
    }

    public StreamReaderThread(InputStream is, Writer writer) {
        reader = new BufferedReader(new InputStreamReader(is));
        this.writer = new BufferedWriter(writer);
    }

    public void run() {
        working = true;
        String line;
        try {
            while (working && (line = reader.readLine()) != null) {
                writer.write(line);
                writer.write('\n');
            }
        } catch (Exception e) {
        } finally {
            try {
                writer.flush();
            } catch (Exception e) {
            }
            try {
                reader.close();
            } catch (Exception e) {
            }
        }
    }
}
