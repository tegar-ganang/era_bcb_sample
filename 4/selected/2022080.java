package net.sourceforge.picdev.util;

import org.apache.commons.io.IOUtils;
import java.io.*;

/**
 * @author Klaus Friedel
 *         Date: 18.12.2007
 *         Time: 20:28:41
 */
public class CmdLine {

    private static class StreamCopy extends Thread {

        InputStream in;

        OutputStream out;

        private StreamCopy(InputStream in, OutputStream out) {
            this.in = in;
            this.out = out;
        }

        public void run() {
            try {
                IOUtils.copy(in, out);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static int execute(String[] cmd, OutputStream out, OutputStream err) throws IOException {
        Process process = Runtime.getRuntime().exec(cmd);
        return waitFor(process, out, err);
    }

    public static int execute(String[] cmd, OutputStream out, OutputStream err, String currentDirectory) throws IOException {
        Process process = Runtime.getRuntime().exec(cmd, null, new File(currentDirectory));
        return waitFor(process, out, err);
    }

    public static int execute(String cmd, OutputStream out, OutputStream err) throws IOException {
        Process process = Runtime.getRuntime().exec(cmd);
        return waitFor(process, out, err);
    }

    private static int waitFor(Process process, OutputStream out, OutputStream err) throws InterruptedIOException {
        StreamCopy errCopy = new StreamCopy(process.getErrorStream(), err);
        StreamCopy outCopy = new StreamCopy(process.getInputStream(), out);
        errCopy.start();
        outCopy.start();
        try {
            return process.waitFor();
        } catch (InterruptedException e) {
            throw new InterruptedIOException(e.getMessage());
        }
    }
}
