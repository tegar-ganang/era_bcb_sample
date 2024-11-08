package brut.util;

import brut.common.BrutException;
import java.io.*;
import java.util.Arrays;
import org.apache.commons.io.IOUtils;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class OS {

    public static void rmdir(File dir) throws BrutException {
        if (!dir.exists()) {
            return;
        }
        File[] files = dir.listFiles();
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            if (file.isDirectory()) {
                rmdir(file);
            } else {
                file.delete();
            }
        }
        dir.delete();
    }

    public static void rmdir(String dir) throws BrutException {
        rmdir(new File(dir));
    }

    public static void cpdir(File src, File dest) throws BrutException {
        dest.mkdirs();
        File[] files = src.listFiles();
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            File destFile = new File(dest.getPath() + File.separatorChar + file.getName());
            if (file.isDirectory()) {
                cpdir(file, destFile);
                continue;
            }
            try {
                InputStream in = new FileInputStream(file);
                OutputStream out = new FileOutputStream(destFile);
                IOUtils.copy(in, out);
                in.close();
                out.close();
            } catch (IOException ex) {
                throw new BrutException("Could not copy file: " + file, ex);
            }
        }
    }

    public static void cpdir(String src, String dest) throws BrutException {
        cpdir(new File(src), new File(dest));
    }

    public static void exec(String[] cmd) throws BrutException {
        if (cmd[0] != null && cmd[0].startsWith("aapt") || cmd[0].startsWith("zipalign")) {
            String osName = System.getProperty("os.name");
            if (osName.toUpperCase().indexOf("WIN") >= 0) {
                cmd[0] = "tools\\" + cmd[0] + "-win.exe";
            } else if (osName.toUpperCase().indexOf("NIX") >= 0 || osName.toUpperCase().indexOf("NUX") >= 0) {
                cmd[0] = "./tools/" + cmd[0] + "-nix";
            }
        }
        Process ps = null;
        try {
            ps = Runtime.getRuntime().exec(cmd);
            new StreamForwarder(ps.getInputStream(), System.err).start();
            new StreamForwarder(ps.getErrorStream(), System.err).start();
            if (ps.waitFor() != 0) {
                throw new BrutException("could not exec command: " + Arrays.toString(cmd));
            }
        } catch (IOException ex) {
            throw new BrutException("could not exec command: " + Arrays.toString(cmd), ex);
        } catch (InterruptedException ex) {
            throw new BrutException("could not exec command: " + Arrays.toString(cmd), ex);
        }
    }

    public static File createTempDirectory() throws BrutException {
        try {
            File tmp = File.createTempFile("BRUT", null);
            if (!tmp.delete()) {
                throw new BrutException("Could not delete tmp file: " + tmp.getAbsolutePath());
            }
            if (!tmp.mkdir()) {
                throw new BrutException("Could not create tmp dir: " + tmp.getAbsolutePath());
            }
            return tmp;
        } catch (IOException ex) {
            throw new BrutException("Could not create tmp dir", ex);
        }
    }

    static class StreamForwarder extends Thread {

        public StreamForwarder(InputStream in, OutputStream out) {
            mIn = in;
            mOut = out;
        }

        @Override
        public void run() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(mIn));
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(mOut));
                String line;
                while ((line = in.readLine()) != null) {
                    out.write(line);
                    out.newLine();
                }
                out.flush();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        private final InputStream mIn;

        private final OutputStream mOut;
    }
}
