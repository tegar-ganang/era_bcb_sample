package com.cube42.util.audio;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import com.cube42.util.datamodel.DataModel;
import com.cube42.util.logging.Logger;

/**
 * Allows audio streams to be played over the users speakers.
 * <p>
 * The audio player will attempt to reconnect if an exception is encountered while playing.
 * If the audio player does not reconnect in the first short attempts, it will then attempt to 
 * reconnect over a larger interval.
 * 
 * 
 * @author Matt Paulin
 */
public class AudioPlayer extends DataModel {

    /**
     * The time between reconnection attempts in milliseconds, this time is used after the
     * stream has been determine to have died.
     */
    private static final long BIG_RECONNECT_TIME = 3000;

    /**
     * The short amount of time between reconnect attemps, for when the stream is initialy interrupted
     */
    private static final long SMALL_RECONNECT_TIME = 100;

    /**
     * The number of reconnect attempts to make
     */
    private static final int NUM_SHORT_ATTEMPTS = 20;

    /**
     * The url currently streaming from
     */
    private String urlLoc;

    /**
     * Thread actually processing the data
     */
    private PlayThread playThread;

    /**
     * Set to true if the player is to auto reconnect
     */
    private boolean autoReconnect;

    /**
     * Used to hold the gain value as a float from 1 to 0, 1 being the maximum
     */
    private float gain;

    /**
     * Controls the panning for the audio player
     */
    private float pan;

    /**
     * Flag to mark if the audio player has been muted
     */
    private boolean muted;

    /**
     * Constructs the AudioPlayer
     * 
     * @param   autoReconnect   The player will automatically try to reconnect if the stream dies
     */
    public AudioPlayer(boolean autoReconnect) {
        super();
        this.autoReconnect = autoReconnect;
    }

    /**
     * Sets the gain value as a float from 1 to 0, 1 being the maximum
     * 
     * @param gain	The new gain value
     */
    public synchronized void setGain(float gain) {
        if (gain < 0) gain = 0;
        if (gain > 1) gain = 1;
        this.gain = gain;
        if (this.playThread != null && !this.muted) {
            this.playThread.setGain(gain);
        }
        this.updatedDataModel();
    }

    /**
     * Returns the gain
     * 
     * @return	The gain
     */
    public float getGain() {
        return this.gain;
    }

    /**
     * Sets the pan value
     * 
     * @param pan	The new pan value
     */
    public synchronized void setPan(float pan) {
        this.pan = pan;
        if (this.playThread != null) {
            this.playThread.setPan(pan);
        }
        this.updatedDataModel();
    }

    /**
     * Returns the pan
     * 
     * @return	The pan
     */
    public float getPan() {
        return this.pan;
    }

    /**
     * Sets the muted value for the audio player
     */
    public synchronized void setMuted(boolean muted) {
        this.muted = muted;
        if (this.playThread != null) {
            this.playThread.setMuted(muted);
        }
        this.updatedDataModel();
    }

    /**
     * Returns true of the audio player has been muted
     * 
     * @return	true if the audio player has been muted
     */
    public boolean isMuted() {
        return this.muted;
    }

    /**
     * Kills the player
     */
    public void kill() {
        stopPlayer();
    }

    /**
     * Sets the audio player to the specified URL
     *
     *
     *  @param  urlLoc  The location of the url to stream from
     */
    public void setStreamURL(String urlLoc) {
        if (urlLoc != null) {
            if ((this.urlLoc == null) || (!this.urlLoc.equals(urlLoc))) {
                stopPlayer();
                this.urlLoc = urlLoc;
                startPlayer();
            }
        }
    }

    /**
     * Starts the player using the url currently saved
     * 
     * @throws  IOException if the stream cannot be opened
     */
    private void startPlayer() {
        playThread = new PlayThread(this.urlLoc, this, gain, muted, pan);
        playThread.start();
    }

    /**
     * Stops the currently playing music
     */
    private synchronized void stopPlayer() {
        if (playThread != null) {
            this.playThread.stop();
            this.playThread = null;
        }
    }

    /**
     * Converts a volume value from 0 -> 1 into a decible value
     * 
     * @param	value	The value of the volume from 0 to 1.  0 being mute
     * @param	maxGainDB	The maximum gain in decibles
     * @param	minGainDB	The minimum gain in decibles
     * @return	The value as a decible
     */
    public static double convertToDB(double value, double maxGainDB, double minGainDB) {
        if (value < 0) value = 0;
        if (value > 1) value = 1;
        double fGain = value;
        double ampGainDB = ((10.0f / 20.0f) * maxGainDB) - minGainDB;
        double cste = Math.log(10.0) / 20;
        double valueDB = minGainDB + (1 / cste) * Math.log(1 + (Math.exp(cste * ampGainDB) - 1) * fGain);
        return valueDB;
    }

    /**
     * Main method used to test the audio player
     */
    public static void main(String[] args) {
        String station;
        if (args != null && args.length > 0) {
            station = args[0];
        } else {
            station = "http://kexp-mp3-1.cac.washington.edu:8000";
        }
        AudioPlayer ap = new AudioPlayer(true);
        ap.setMuted(false);
        ap.setGain(1.0f);
        ap.setStreamURL(station);
    }

    /**
     * Thread used to run the music
     * @author Matt Paulin
     */
    private class PlayThread implements Runnable {

        /**
         * Audio input stream to read from
         */
        private AudioInputStream inStream;

        /**
         * Data Line to write out to
         */
        private SourceDataLine dataLine;

        /**
         * Thread actually running 
         */
        private Thread thread;

        /**
         * Set to true when the stream is running
         */
        private boolean running;

        /**
         * Size of the buffer
         */
        private static final int EXTERNAL_BUFFER_SIZE = 16384;

        /**
         * The undecoded input stream
         */
        private AudioInputStream undecodedInput;

        /**
         * The mute control to use to mute the player
         */
        private BooleanControl muteControl;

        /**
         * The float control to use to change the gain
         */
        private FloatControl gainControl;

        /**
         * The float control to use to change the pan
         */
        private FloatControl panControl;

        /**
         * The URL to connect to 
         */
        private String url;

        /**
         * Reference to the Audio Player that is containing this Thread
         */
        private AudioPlayer audioPlayer;

        /**
         * The source format
         */
        private AudioFormat sourceFormat;

        /**
         * The target format
         */
        private AudioFormat targetFormat;

        /**
         * The mute value
         */
        private boolean muted;

        /**
         * The pan value
         */
        private float pan;

        /**
         * The gain value
         */
        private float gain;

        /**
         * Constructs the play thread
         * 
         * @param   inStream    The URL to connect to
         * @param	audioPlayer	The audioPlayer using this thread
         * @param	gain		The gain to start with
         * @param	muted		The mute value to start with
         * @param	pan			The current pan
         */
        public PlayThread(String url, AudioPlayer audioPlayer, float gain, boolean muted, float pan) {
            this.url = url;
            this.audioPlayer = audioPlayer;
            this.setGain(gain);
            this.setMuted(muted);
            this.setPan(pan);
            thread = new Thread(this);
        }

        /**
         * Gets the input stream and sets everything up
         *
         */
        private synchronized void setupInputStream() throws LineUnavailableException, IOException {
            try {
                tearDownInputStream();
                URL urlCon = new URL(this.url);
                this.undecodedInput = AudioSystem.getAudioInputStream(urlCon);
                AudioFileFormat sourceFileFormat = AudioSystem.getAudioFileFormat(undecodedInput);
                sourceFormat = undecodedInput.getFormat();
                targetFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sourceFormat.getSampleRate(), 16, sourceFormat.getChannels(), sourceFormat.getChannels() * 2, sourceFormat.getSampleRate(), false);
                this.inStream = AudioSystem.getAudioInputStream(targetFormat, undecodedInput);
                AudioFormat audioFormat = inStream.getFormat();
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat, AudioSystem.NOT_SPECIFIED);
                this.dataLine = (SourceDataLine) AudioSystem.getLine(info);
                this.dataLine.open(audioFormat, dataLine.getBufferSize());
                this.dataLine.start();
                if (dataLine.isControlSupported(FloatControl.Type.PAN)) {
                    FloatControl tempPanControl = (FloatControl) dataLine.getControl(FloatControl.Type.PAN);
                    if (this.panControl != null) {
                        tempPanControl.setValue(this.panControl.getValue());
                    }
                    this.panControl = tempPanControl;
                    this.setPan(this.pan);
                }
                if (dataLine.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    FloatControl tempGainControl = (FloatControl) dataLine.getControl(FloatControl.Type.MASTER_GAIN);
                    if (this.gainControl != null) {
                        tempGainControl.setValue(this.gainControl.getValue());
                    }
                    this.gainControl = tempGainControl;
                    this.setGain(this.gain);
                }
                if (dataLine.isControlSupported(BooleanControl.Type.MUTE)) {
                    BooleanControl newControl = (BooleanControl) dataLine.getControl(BooleanControl.Type.MUTE);
                    if (this.muteControl != null) {
                        newControl.setValue(this.muteControl.getValue());
                    }
                    this.muteControl = newControl;
                    this.setMuted(muted);
                }
            } catch (MalformedURLException e) {
                Logger.error(AudioSystemCodes.MALFORMED_STREAM_URL, new Object[] { url, e.getMessage() });
            } catch (UnsupportedAudioFileException e) {
                Logger.error(AudioSystemCodes.UNSUPPORTED_AUDIO_FORMAT, new Object[] { url, e.getMessage(), e.getStackTrace() });
            }
        }

        /**
         * Tears down the input streams
         */
        private void tearDownInputStream() {
            try {
                if (this.undecodedInput != null) {
                    this.undecodedInput.close();
                }
                if (this.inStream != null) {
                    this.inStream.close();
                }
                if (this.dataLine != null) {
                    this.dataLine.drain();
                    this.dataLine.close();
                }
            } catch (IOException e) {
                Logger.error(AudioSystemCodes.TEARDOWN_IOEXCEPTION, new Object[] { e.getMessage() });
            }
        }

        /**
         * Returns the URL this player is connected to
         * 
         * @return	The URL this player is connected to
         */
        public String getURL() {
            return this.url;
        }

        /**
         * Sets the gain value as a float from 1 to 0, 1 being the maximum
         * 
         * @param gain	The new gain value
         */
        public synchronized void setGain(float gain) {
            this.gain = gain;
            if (this.gainControl != null) {
                this.gainControl.setValue((float) AudioPlayer.convertToDB(gain, this.getMaxGain(), this.getMinGain()));
            }
        }

        /**
         * Returns the maximum gain in decibles
         * 
         * @return	The maximum gain in decibles
         */
        public double getMaxGain() {
            if (this.gainControl == null) return 0;
            return this.gainControl.getMaximum();
        }

        /**
         * Returns the minimum gain in decibles
         * 
         * @return	The minimum gain in decibles, zero if there is no control
         */
        public double getMinGain() {
            if (this.gainControl == null) return 0;
            return this.gainControl.getMinimum();
        }

        /**
         * Sets the pan value
         * 
         * @param pan	The new pan value
         */
        public void setPan(float pan) {
            this.pan = pan;
            if (this.panControl != null) {
                this.panControl.setValue(pan);
            }
        }

        /**
         * Sets the muted value for the audio player
         */
        public synchronized void setMuted(boolean muted) {
            this.muted = muted;
            if (this.muteControl != null) {
                this.muteControl.setValue(muted);
            }
        }

        /**
         * Starts the thread
         */
        public void start() {
            this.running = true;
            this.thread.start();
        }

        /**
         * Shuts down the thread
         */
        public void stop() {
            this.running = false;
        }

        /**
         * Runs the thread 
         */
        public void run() {
            int nBytesRead = -1;
            int nBytesWritten = 0;
            int numAttempts = NUM_SHORT_ATTEMPTS;
            byte[] abData = new byte[EXTERNAL_BUFFER_SIZE];
            try {
                int numZeros = 0;
                while (this.running) {
                    try {
                        if (numZeros > 10 || nBytesRead == -1) {
                            numZeros = 0;
                            if (numAttempts > 0) {
                                numAttempts--;
                                Thread.sleep(SMALL_RECONNECT_TIME);
                            } else {
                                Thread.sleep(BIG_RECONNECT_TIME);
                            }
                            this.setupInputStream();
                        }
                        if (inStream != null) {
                            nBytesRead = inStream.read(abData, 0, abData.length);
                        }
                        if (nBytesRead == 0) {
                            numZeros++;
                        } else {
                            numZeros = 0;
                        }
                        if (nBytesRead > 0) {
                            numAttempts = NUM_SHORT_ATTEMPTS;
                            nBytesWritten = dataLine.write(abData, 0, nBytesRead);
                        }
                    } catch (IOException e) {
                        Logger.error(AudioSystemCodes.IOEXCEPTION_IN_PLAYTHREAD, new Object[] { e.getMessage() });
                    }
                }
            } catch (Throwable e) {
                e.printStackTrace();
                Logger.error(AudioSystemCodes.THROWABLE_THROWN, new Object[] { e.getMessage() });
            }
            this.tearDownInputStream();
        }
    }
}
