package house.neko.media.common;

import java.util.Vector;
import java.net.URL;
import java.io.*;
import javax.sound.sampled.*;
import javax.sound.midi.*;
import org.apache.commons.logging.Log;

/**
 *
 * @author andy
 */
public final class MediaPlayer {

    private Log log;

    private Player player;

    private Thread playerThread;

    private Vector<MediaPlayerCallback.Stop> stopCallbacks;

    private Vector<MediaPlayerCallback.Play> playCallbacks;

    private Vector<MediaPlayerCallback.Pause> pauseCallbacks;

    /**
	 *
	 */
    public MediaPlayer() {
        this.log = ConfigurationManager.getLog(getClass());
        player = new Player();
        stopCallbacks = new Vector<MediaPlayerCallback.Stop>();
        playCallbacks = new Vector<MediaPlayerCallback.Play>();
        pauseCallbacks = new Vector<MediaPlayerCallback.Pause>();
        playerThread = new Thread(player, "MediaPlayer");
        playerThread.start();
    }

    /**
	 *
	 */
    public void play() {
        start();
    }

    /**
	 *
	 */
    public void start() {
        player.stop = false;
        player.pause = false;
        player.play();
        playerThread.interrupt();
    }

    /**
	 *
	 */
    public void pause() {
        player.pause();
    }

    /**
	 *
	 */
    public void stop() {
        player.stop();
    }

    /**
	 *
	 */
    public void skipBackward() {
    }

    /**
	 *
	 */
    public void skipForward() {
    }

    /**
	 *
	 * @return
	 */
    public long getCurrentPlayTime() {
        if (player.line != null) {
            return player.line.getMicrosecondPosition();
        } else {
            return 0L;
        }
    }

    /**
	 *
	 * @param i
	 */
    public void setInputStream(InputStream i) {
        player.inputStream = i;
    }

    public void setURL(URL url) {
        player.url = url;
    }

    public void registerPlayCallback(MediaPlayerCallback.Play c) {
        playCallbacks.add(c);
    }

    public void registerStopCallback(MediaPlayerCallback.Stop c) {
        stopCallbacks.add(c);
    }

    public void registerPauseCallback(MediaPlayerCallback.Pause c) {
        pauseCallbacks.add(c);
    }

    /**
	 *
	 */
    protected class Player implements Runnable {

        private AudioInputStream ain = null;

        private URL url = null;

        /**
		 *
		 */
        public SourceDataLine line = null;

        private boolean isLineOpen = false;

        private AudioFormat format = null;

        private DataLine.Info info = null;

        /**
		 *
		 */
        protected InputStream inputStream;

        protected InputStream lastInputStream;

        /**
		 *
		 */
        public boolean stop = false;

        /**
		 *
		 */
        public boolean pause = false;

        /**
		 *
		 */
        public boolean stopped = true;

        /**
		 *
		 */
        public boolean quit = false;

        /**
		 *
		 */
        public void play() {
            if (line != null && !line.isRunning()) {
                line.start();
                for (int i = 0; i < playCallbacks.size(); i++) {
                    playCallbacks.elementAt(i).notifyPlayerStarted();
                }
            }
        }

        /**
		 *
		 */
        public void pause() {
            player.pause = true;
            if (line != null && line.isRunning()) {
                line.stop();
                doPauseCallbacks();
            }
        }

        private void doPauseCallbacks() {
            for (int i = 0; i < pauseCallbacks.size(); i++) {
                pauseCallbacks.elementAt(i).notifyPlayerPaused();
            }
        }

        /**
		 *
		 */
        public void stop() {
            if (log.isTraceEnabled()) {
                log.trace("Stopping player");
            }
            player.stop = true;
            if (line != null && line.isRunning()) {
                line.stop();
                line.flush();
                doStopCallbacks();
            }
        }

        private void doStopCallbacks() {
            for (int i = 0; i < stopCallbacks.size(); i++) {
                stopCallbacks.elementAt(i).notifyPlayerStopped();
            }
        }

        /**
		 *
		 */
        public synchronized void run() {
            if (log.isTraceEnabled()) {
                log.trace("Starting to play!");
            }
            InputStream currentlyPlaying;
            URL currentURL = url;
            QUIT: while (!quit) {
                try {
                    if (log.isTraceEnabled()) {
                        log.trace("Parking player");
                    }
                    try {
                        wait();
                    } catch (InterruptedException ie) {
                        if (log.isTraceEnabled()) {
                            log.trace("Player was unparked from interruption");
                        }
                    }
                    currentlyPlaying = inputStream;
                    lastInputStream = inputStream;
                    if (currentlyPlaying == null) {
                        if (log.isWarnEnabled()) {
                            log.warn("Input stream is null, yeilding");
                        }
                        Thread.yield();
                        continue QUIT;
                    }
                    if (inputStream != currentlyPlaying) {
                        if (ain != null) {
                            if (log.isTraceEnabled()) {
                                log.trace("Closing existing audio stream ");
                            }
                            ain.close();
                        }
                    }
                    if (ain == null) {
                        if (log.isTraceEnabled()) {
                            log.trace("Creating audio stream from input stream: " + currentlyPlaying.available());
                        }
                        ain = AudioSystem.getAudioInputStream(currentlyPlaying);
                    }
                    format = ain.getFormat();
                    info = new DataLine.Info(SourceDataLine.class, format);
                    if (log.isTraceEnabled()) {
                        log.trace("Got format: " + format);
                    }
                    if (!AudioSystem.isLineSupported(info)) {
                        if (log.isTraceEnabled()) {
                            log.trace("Creating transcoder to PCM");
                        }
                        AudioFormat pcm = new AudioFormat(format.getSampleRate(), 16, format.getChannels(), true, false);
                        ain = AudioSystem.getAudioInputStream(pcm, ain);
                        format = ain.getFormat();
                        info = new DataLine.Info(SourceDataLine.class, format);
                    }
                    log.trace("Getting line");
                    line = (SourceDataLine) AudioSystem.getLine(info);
                    if (log.isTraceEnabled()) {
                        log.trace("Setting format to " + format);
                    }
                    line.open(format);
                    log.trace("Starting line out");
                    line.start();
                    int framesize = format.getFrameSize();
                    if (log.isTraceEnabled()) {
                        log.trace("Got frame size of " + framesize);
                    }
                    byte[] buffer = new byte[4096 * framesize];
                    int numbytes = 0;
                    long played = 0;
                    int bytesread = 0;
                    int bytestowrite = 0;
                    int remaining = 0;
                    if (!line.isRunning()) {
                        line.flush();
                        line.start();
                    }
                    stopped = false;
                    log.trace("About to start playing");
                    PLAY: while (!stop) {
                        if (inputStream != currentlyPlaying) {
                            log.trace("Player -- inputStream changed, stopping");
                            stop = true;
                            break PLAY;
                        }
                        if (pause) {
                            if (log.isTraceEnabled()) {
                                log.trace("pause flag set, pausing player");
                            }
                            try {
                                doPauseCallbacks();
                                wait();
                            } catch (InterruptedException ie) {
                                if (log.isTraceEnabled()) {
                                    log.trace("player was interrupted from pause");
                                }
                            }
                        }
                        try {
                            bytesread = ain.read(buffer, numbytes, buffer.length - numbytes);
                            if (bytesread == -1) {
                                if (log.isTraceEnabled()) {
                                    log.trace("Player -- End of stream reached, stopping");
                                }
                                inputStream.close();
                                inputStream = null;
                                currentlyPlaying = null;
                                break PLAY;
                            }
                            numbytes += bytesread;
                            played += (long) bytesread;
                            bytestowrite = (numbytes / framesize) * framesize;
                            line.write(buffer, 0, bytestowrite);
                            remaining = numbytes - bytestowrite;
                            if (remaining > 0) {
                                System.arraycopy(buffer, bytestowrite, buffer, 0, remaining);
                            }
                            numbytes = remaining;
                        } catch (Throwable t) {
                            log.error("Failure playing", t);
                            inputStream.close();
                            inputStream = null;
                            currentlyPlaying = null;
                            line.stop();
                            doStopCallbacks();
                        }
                    }
                    if (line.isRunning()) {
                        line.drain();
                    }
                    if (log.isTraceEnabled()) {
                        log.trace("Player stopping line out (done playing?)");
                    }
                    line.stop();
                    stopped = true;
                } catch (UnsupportedAudioFileException e) {
                    log.error("Error playing stream", e);
                } catch (LineUnavailableException e) {
                    log.error("Error playing stream", e);
                } catch (IOException e) {
                    log.error("Error playing stream", e);
                }
                doStopCallbacks();
                if (log.isTraceEnabled()) {
                    log.trace("Player reached end of control loop, starting over");
                }
            }
            try {
                if (line != null) {
                    line.close();
                }
            } catch (Exception e) {
                log.error(e);
            }
            try {
                if (ain != null) {
                    ain.close();
                }
            } catch (Exception e) {
                log.error(e);
            }
            if (log.isTraceEnabled()) {
                log.trace("Player exiting!");
            }
        }

        /**
		 *
		 */
        @Override
        protected void finalize() {
            try {
                if (line != null) {
                    line.close();
                }
            } catch (Exception e) {
                log.error(e);
            }
            try {
                if (ain != null) {
                    ain.close();
                }
            } catch (Exception e) {
                log.error(e);
            }
            try {
                super.finalize();
            } catch (Throwable t) {
                log.error(t);
            }
        }
    }
}
