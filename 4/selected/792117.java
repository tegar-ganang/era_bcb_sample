package net.sf.smartcrib.media;

import net.sf.smartcrib.media.ProcessListener;
import java.io.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * A player which is actually an interface to the famous MPlayer.
 */
public class JMPlayer extends AbstractPlayer {

    private static Logger logger = Logger.getLogger(JMPlayer.class.getName());

    /** A thread that reads from an input stream and outputs to another line by line. */
    private static class LineRedirecter extends Thread {

        /** The input stream to read from. */
        private InputStream in;

        /** The output stream to write to. */
        private OutputStream out;

        /** The prefix used to prefix the lines when outputting to the logger. */
        private String prefix;

        /**
         * @param in the input stream to read from.
         * @param out the output stream to write to.
         * @param prefix the prefix used to prefix the lines when outputting to the logger.
         */
        LineRedirecter(InputStream in, OutputStream out, String prefix) {
            this.in = in;
            this.out = out;
            this.prefix = prefix;
        }

        public void run() {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                PrintStream printStream = new PrintStream(out);
                String line;
                while ((line = reader.readLine()) != null) {
                    if (logger != null) logger.finest((prefix != null ? prefix : "") + line);
                    printStream.println(line);
                }
            } catch (IOException exc) {
                if (logger != null) logger.log(Level.WARNING, "An error has occured while grabbing lines", exc);
            }
        }
    }

    /** The path to the MPlayer executable. */
    private String mplayerPath = "c:\\Program Files\\SMPlayer\\mplayer\\mplayer.exe";

    /** Options passed to MPlayer. */
    private String mplayerOptions = "-slave";

    /** The process corresponding to MPlayer. */
    private Process mplayerProcess;

    private ProcessExitDetector mplayerDetector;

    /** The standard input for MPlayer where you can send commands. */
    private PrintStream mplayerIn;

    /** A combined reader for the the standard output and error of MPlayer. Used to read MPlayer responses. */
    private BufferedReader mplayerOutErr;

    private transient boolean playing = false;

    public JMPlayer() {
        Runtime.getRuntime().addShutdownHook(new Thread() {

            public void run() {
                close(true);
            }
        });
    }

    /** @return the path to the MPlayer executable. */
    public String getMPlayerPath() {
        return mplayerPath;
    }

    /** Sets the path to the MPlayer executable.
     * @param mplayerPath the new MPlayer path; this will be actually effective
     * after {@link #close() closing} the currently running player.
     */
    public void setMPlayerPath(String mplayerPath) {
        this.mplayerPath = mplayerPath;
    }

    public void open(String path) throws IOException {
        super.open(path);
        path = path.replaceAll("\\\\", "\\\\\\\\");
        if (mplayerProcess != null) close();
        if (mplayerProcess == null) {
            String command = "\"" + mplayerPath + "\" " + mplayerOptions + " \"" + path + "\"";
            logger.info("Starting MPlayer process: " + command);
            mplayerProcess = Runtime.getRuntime().exec(command);
            mplayerDetector = new net.sf.smartcrib.media.ProcessExitDetector(mplayerProcess);
            mplayerDetector.addProcessListener(new ProcessListener() {

                public void processFinished(Process process) {
                    mplayerProcess = null;
                    playing = false;
                    firePlayerItemEnded();
                }
            });
            mplayerDetector.start();
            PipedInputStream readFrom = new PipedInputStream(1024 * 1024);
            PipedOutputStream writeTo = new PipedOutputStream(readFrom);
            mplayerOutErr = new BufferedReader(new InputStreamReader(readFrom));
            new LineRedirecter(mplayerProcess.getInputStream(), writeTo, "MPlayer says: ").start();
            new LineRedirecter(mplayerProcess.getErrorStream(), writeTo, "MPlayer encountered an error: ").start();
            mplayerIn = new PrintStream(mplayerProcess.getOutputStream());
        }
        waitForAnswer("Starting playback...");
        playing = true;
        firePlayerStarted();
        logger.info("Started playing file " + path);
    }

    public void close() {
        close(false);
    }

    private void close(boolean force) {
        if (mplayerProcess != null) {
            if (mplayerDetector != null) {
                mplayerDetector.cancel();
                mplayerDetector = null;
            }
            if (force) {
                mplayerProcess.destroy();
            } else {
                execute("quit");
                try {
                    mplayerProcess.waitFor();
                } catch (InterruptedException e) {
                }
            }
            mplayerProcess = null;
            firePlayerStopped();
        }
    }

    public File getPlayingFile() {
        if (playing) {
            String path = getProperty("path");
            if (path != null) {
                path = path.replaceAll("\\\\\\\\", "\\\\");
                return new File(path);
            }
        }
        return null;
    }

    public boolean togglePlay() {
        execute("pause");
        playing = !playing;
        firePlayerPauseToggled();
        return isPlaying();
    }

    public void stop() {
        if (playing) {
            execute("pausing set_property time_pos 0");
            playing = false;
            firePlayerStopped();
        }
    }

    public boolean isPlaying() {
        return mplayerProcess != null && playing;
    }

    public long getTimePosition() {
        return getPropertyAsLong("time_pos");
    }

    public void setTimePosition(long seconds) {
        setProperty("time_pos", seconds);
    }

    public long getTotalTime() {
        return getPropertyAsLong("length");
    }

    public int getVolume() {
        return getPropertyAsInt("volume");
    }

    public void setVolume(int volume) {
        setProperty("volume", volume);
    }

    protected String getProperty(String name) {
        if (name == null || mplayerProcess == null) {
            return null;
        }
        String s = "ANS_" + name + "=";
        String x = execute("get_property " + name, s);
        if (x == null) return null;
        if (!x.startsWith(s)) return null;
        return x.substring(s.length());
    }

    protected int getPropertyAsInt(String name) {
        try {
            return Integer.parseInt(getProperty(name));
        } catch (NumberFormatException exc) {
        } catch (NullPointerException exc) {
        }
        return 0;
    }

    protected long getPropertyAsLong(String name) {
        try {
            return Long.parseLong(getProperty(name));
        } catch (NumberFormatException exc) {
        } catch (NullPointerException exc) {
        }
        return 0;
    }

    protected float getPropertyAsFloat(String name) {
        try {
            return Float.parseFloat(getProperty(name));
        } catch (NumberFormatException exc) {
        } catch (NullPointerException exc) {
        }
        return 0f;
    }

    protected void setProperty(String name, String value) {
        execute("set_property " + name + " " + value);
    }

    protected void setProperty(String name, int value) {
        execute("set_property " + name + " " + value);
    }

    protected void setProperty(String name, long value) {
        execute("set_property " + name + " " + value);
    }

    protected void setProperty(String name, float value) {
        execute("set_property " + name + " " + value);
    }

    /** Sends a command to MPlayer..
     * @param command the command to be sent
     */
    private void execute(String command) {
        execute(command, null);
    }

    /** Sends a command to MPlayer and waits for an answer.
     * @param command the command to be sent
     * @param expected the string with which has to start the line; if null don't wait for an answer
     * @return the MPlayer answer
     */
    private String execute(String command, String expected) {
        if (mplayerProcess != null) {
            if (!(command.startsWith("pausing") || command.startsWith("pause"))) {
                command = "pausing_keep " + command;
            }
            if (logger != null) logger.info("Send to MPlayer the command \"" + command + "\" and expecting " + (expected != null ? "\"" + expected + "\"" : "no answer"));
            mplayerIn.print(command);
            mplayerIn.print("\n");
            mplayerIn.flush();
            if (logger != null) logger.info("Command sent");
            if (command.startsWith("pausing_toggle")) {
                playing = !playing;
                firePlayerPauseToggled();
            }
            if (expected != null) {
                String response = waitForAnswer(expected);
                if (logger != null) logger.info("MPlayer command response: " + response);
                return response;
            }
        }
        return null;
    }

    /** Read from the MPlayer standard output and error a line that starts with the given parameter and return it.
     * @param expected the expected starting string for the line
     * @return the entire line from the standard output or error of MPlayer
     */
    private String waitForAnswer(String expected) {
        String line = null;
        if (expected != null) {
            try {
                while ((line = mplayerOutErr.readLine()) != null) {
                    logger.finest("Reading line: " + line);
                    if (line.startsWith(expected)) {
                        return line;
                    }
                }
            } catch (IOException e) {
            }
        }
        return line;
    }
}
