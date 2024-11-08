package sun.jkernel;

import java.io.*;

/**
 * Invoked by DownloadManager to begin (in a new JRE) the process of downloading
 * all remaining JRE components in the background.  A mutex is used to ensure
 * that only one BackgroundDownloader can be active at a time.
 *
 */
public class BackgroundDownloader {

    public static final String BACKGROUND_DOWNLOAD_PROPERTY = "kernel.background.download";

    public static final String PID_PATH = "tmp" + File.separator + "background.pid";

    private static final int WAIT_TIME = 10000;

    private static Mutex backgroundMutex;

    static synchronized Mutex getBackgroundMutex() {
        if (backgroundMutex == null) backgroundMutex = Mutex.create(DownloadManager.MUTEX_PREFIX + "background");
        return backgroundMutex;
    }

    private static void doBackgroundDownloads() {
        if (DownloadManager.isJREComplete()) return;
        if (getBackgroundMutex().acquire(0)) {
            try {
                writePid();
                Thread.sleep(WAIT_TIME);
                DownloadManager.doBackgroundDownloads(false);
                DownloadManager.performCompletionIfNeeded();
            } catch (InterruptedException e) {
            } finally {
                getBackgroundMutex().release();
            }
        } else {
            System.err.println("Unable to acquire background download mutex.");
            System.exit(1);
        }
    }

    /**
     * Writes the current process ID to a file, so that the uninstaller can
     * find and kill this process if needed.
     */
    private static void writePid() {
        try {
            File pid = new File(DownloadManager.getBundlePath(), PID_PATH);
            pid.getParentFile().mkdirs();
            PrintStream out = new PrintStream(new FileOutputStream(pid));
            pid.deleteOnExit();
            out.println(DownloadManager.getCurrentProcessId());
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Reads from an InputStream until exhausted, writing all data to the
     * specified OutputStream.
     */
    private static void send(InputStream in, OutputStream out) throws IOException {
        int c;
        byte[] buffer = new byte[2048];
        while ((c = in.read(buffer)) > 0) out.write(buffer, 0, c);
    }

    public static boolean getBackgroundDownloadProperty() {
        boolean bgDownloadEnabled = getBackgroundDownloadKey();
        if (System.getProperty(BACKGROUND_DOWNLOAD_PROPERTY) != null) {
            bgDownloadEnabled = Boolean.valueOf(System.getProperty(BACKGROUND_DOWNLOAD_PROPERTY));
        }
        return bgDownloadEnabled;
    }

    static native boolean getBackgroundDownloadKey();

    static void startBackgroundDownloads() {
        if (!getBackgroundDownloadProperty()) {
            return;
        }
        while (System.err == null) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                return;
            }
        }
        try {
            String args = "-D" + BACKGROUND_DOWNLOAD_PROPERTY + "=false -Xmx256m";
            String backgroundDownloadURL = DownloadManager.getBaseDownloadURL();
            if (backgroundDownloadURL != null && backgroundDownloadURL.equals(DownloadManager.DEFAULT_DOWNLOAD_URL) == false) {
                args += " -D" + DownloadManager.KERNEL_DOWNLOAD_URL_PROPERTY + "=" + backgroundDownloadURL;
            }
            ;
            args += " sun.jkernel.BackgroundDownloader";
            final Process jvm = Runtime.getRuntime().exec("\"" + new File(System.getProperty("java.home"), "bin" + File.separator + "java.exe") + "\" " + args);
            Thread outputReader = new Thread("kernelOutputReader") {

                public void run() {
                    try {
                        InputStream in = jvm.getInputStream();
                        send(in, new PrintStream(new ByteArrayOutputStream()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };
            outputReader.setDaemon(true);
            outputReader.start();
            Thread errorReader = new Thread("kernelErrorReader") {

                public void run() {
                    try {
                        InputStream in = jvm.getErrorStream();
                        send(in, new PrintStream(new ByteArrayOutputStream()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };
            errorReader.setDaemon(true);
            errorReader.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] arg) {
        doBackgroundDownloads();
    }
}
