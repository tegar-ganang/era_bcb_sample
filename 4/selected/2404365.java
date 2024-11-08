package net.sourceforge.plantuml.cucadiagram.dot;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import net.sourceforge.plantuml.Log;

public class ProcessRunner {

    private final String cmd;

    private String error;

    private String out;

    public ProcessRunner(String cmd) {
        this.cmd = cmd;
    }

    private static int cpt = 0;

    public void run(byte in[], OutputStream redirection) throws IOException, InterruptedException {
        run(in, redirection, null);
    }

    public void run(byte in[], OutputStream redirection, File dir) throws IOException, InterruptedException {
        final Process process = Runtime.getRuntime().exec(cmd, null, dir);
        final ThreadStream errorStream = new ThreadStream(process.getErrorStream(), null);
        final ThreadStream outStream = new ThreadStream(process.getInputStream(), redirection);
        errorStream.start();
        outStream.start();
        if (in != null) {
            final OutputStream os = process.getOutputStream();
            os.write(in);
            os.close();
        }
        process.waitFor();
        errorStream.join(10000L);
        outStream.join(10000L);
        this.out = outStream.sb.toString();
        this.error = errorStream.sb.toString();
        Log.info("RUN nb = " + (++cpt));
    }

    static class ThreadStream extends Thread {

        private final InputStream streamToRead;

        private final OutputStream redirection;

        private final StringBuilder sb = new StringBuilder();

        ThreadStream(InputStream streamToRead, OutputStream redirection) {
            this.streamToRead = streamToRead;
            this.redirection = redirection;
        }

        @Override
        public void run() {
            Log.debug("STARTING " + this);
            int read = 0;
            try {
                while ((read = streamToRead.read()) != -1) {
                    if (redirection == null) {
                        sb.append((char) read);
                    } else {
                        redirection.write(read);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                sb.append('\n');
                sb.append(e.toString());
            }
            Log.debug("FINISHING " + this);
        }
    }

    public final String getError() {
        return error;
    }

    public final String getOut() {
        return out;
    }
}
