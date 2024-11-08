package de.frewert.vboxj.message;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.frewert.vboxj.AudioException;
import de.frewert.vboxj.vbox.MessageInfo;
import de.frewert.vboxj.vbox.ProxyInterface;
import de.frewert.vboxj.vbox.VBoxException;

/**
 * <pre>
 * Copyright (C) 2001, 2003, 2005, 2010 Carsten Frewert. All Rights Reserved.
 * 
 * The VBox/J package (de.frewert.vboxj.*) is distributed under
 * the terms of the Artistic license.
 * </pre>
 * @author Carsten Frewert
 * &lt;<a href="mailto:frewert@users.sourceforge.net">
 * frewert@users.sourceforge.net</a>&gt;
 */
public class PlayableMessage {

    private static final int SAMPLE_RATE = 8000;

    private static final Logger LOG = LoggerFactory.getLogger(PlayableMessage.class);

    public static final int HEADER_LENGTH = 372;

    private static float defaultGain;

    private final MessageInfo message;

    private final ProxyInterface proxy;

    private Clip clip;

    private volatile boolean paused;

    private volatile boolean playing;

    private FloatControl gainControl;

    /**
     * Create a PlayableMessage.
     *
     * @param msg the description of the message
     * @param proxy the proxy delivering the actual message data
     */
    public PlayableMessage(final MessageInfo msg, final ProxyInterface proxy) {
        if ((null == msg) || (null == proxy)) {
            throw new IllegalArgumentException("null not allowed.");
        }
        this.message = msg;
        this.proxy = proxy;
    }

    /**
     * Play this message. If it is already playing, do nothing.
     * @exception AudioException in case of sound related errors
     * @see #pause
     * @see #stop
     * @see #resume
     */
    public void play() throws AudioException, IOException {
        if (isPlaying()) {
            return;
        }
        try {
            getClip().start();
            setPlaying(true);
            setPaused(false);
        } catch (LineUnavailableException lue) {
            throw new AudioException(lue);
        } catch (UnsupportedAudioFileException uafe) {
            throw new AudioException(uafe);
        }
    }

    /**
     * Cancel playing this message. 
     * @see #play
     * @see #pause
     * @see #resume
     */
    public void stop() {
        setPaused(false);
        setPlaying(false);
        if (clip != null) {
            clip.stop();
            clip.setFramePosition(0);
        }
    }

    /**
     * Interrupt playing this message.
     * @see #play
     * @see #stop
     * @see #resume
     */
    public void pause() {
        if (isPaused()) {
            return;
        }
        setPaused(true);
        if (clip != null) {
            clip.stop();
        }
    }

    /**
     * Continue playing this message. 
     * @see #play
     * @see #pause
     * @see #stop
     */
    public void resume() {
        if (!isPaused()) {
            return;
        }
        setPaused(false);
        if (clip != null) {
            clip.start();
        }
    }

    public static void setDefaultGain(final double gain) {
        PlayableMessage.defaultGain = (float) gain;
    }

    public void setGain(final double dB) {
        if ((gainControl == null) && (clip != null)) {
            this.gainControl = getGainControl(clip);
        }
        if (null != gainControl) {
            final float minGain = gainControl.getMinimum();
            final float maxGain = gainControl.getMaximum();
            double dbValue = (dB == 0) ? 0.1 : dB;
            double logGain = Math.log(dbValue) / Math.log(10) * 50;
            if (logGain < 0d) {
                logGain = 0d;
            }
            dbValue = (logGain * (maxGain - minGain) / 100) + minGain;
            gainControl.setValue((float) dbValue);
        } else {
            LOG.info("Cannot set gain, no gain control found.");
        }
    }

    /**
     * Add a LineListener
     * @param listener the listener to add
     * @see #removeLineListener
     */
    public void addLineListener(final LineListener listener) throws IOException {
        try {
            getClip().addLineListener(listener);
        } catch (LineUnavailableException lue) {
        } catch (UnsupportedAudioFileException aufe) {
        }
    }

    /**
     * Remove a LineListener
     * @param listener the listener to remove
     * @see #addLineListener
     */
    public void removeLineListener(final LineListener listener) {
        try {
            getClip().removeLineListener(listener);
        } catch (IOException e) {
            LOG.warn("Remvoing line listener failed", e);
        } catch (LineUnavailableException e) {
            LOG.warn("Remvoing line listener failed", e);
        } catch (UnsupportedAudioFileException e) {
            LOG.warn("Remvoing line listener failed", e);
        }
    }

    public synchronized boolean isPaused() {
        return this.paused;
    }

    public synchronized boolean isPlaying() {
        return this.playing;
    }

    private synchronized void setPaused(final boolean paused) {
        this.paused = paused;
    }

    private synchronized void setPlaying(final boolean playing) {
        this.playing = playing;
    }

    private synchronized Clip getClip() throws IOException, LineUnavailableException, UnsupportedAudioFileException {
        if (clip == null) {
            clip = createClip();
            gainControl = getGainControl(clip);
            setGain(PlayableMessage.defaultGain);
        }
        return clip;
    }

    /**
     * @return the length of the message in microseconds
     * @throws AudioException
     */
    public long getMicrosecondLength() throws AudioException {
        try {
            return getClip().getMicrosecondLength();
        } catch (Exception e) {
            throw new AudioException(e);
        }
    }

    /**
     * @return the already played time of this message in microseconds
     * @throws AudioException
     */
    public long getMicrosecondPosition() throws AudioException {
        try {
            return getClip().getMicrosecondPosition();
        } catch (Exception e) {
            throw new AudioException(e);
        }
    }

    public InputStream getAudioData() throws IOException {
        final String name = message.getFilename();
        final InputStream rawAudio = proxy.getMessageData(name);
        final long skipped = rawAudio.skip(HEADER_LENGTH);
        if (skipped != HEADER_LENGTH) {
            LOG.warn("skipped " + skipped + " bytes instead of " + HEADER_LENGTH);
        }
        final int size = rawAudio.available();
        LOG.debug("raw audio data: " + size + " bytes");
        final byte[] snd = ".snd".getBytes();
        final byte[] comment = "Created by VBox/J".getBytes();
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final DataOutputStream dos = new DataOutputStream(bos);
        dos.write(snd, 0, snd.length);
        dos.writeInt(24 + comment.length);
        dos.writeInt(size);
        dos.writeInt(1);
        dos.writeInt(SAMPLE_RATE);
        dos.writeInt(1);
        dos.write(comment, 0, comment.length);
        dos.flush();
        byte[] auHeader = bos.toByteArray();
        dos.close();
        BufferedInputStream audio = new BufferedInputStream(new SequenceInputStream(new ByteArrayInputStream(auHeader), rawAudio));
        return audio;
    }

    public void writeAudioData(final OutputStream out, final AudioFileFormat.Type type) throws IOException, AudioException {
        if (out == null) {
            return;
        }
        try {
            AudioInputStream ais = AudioSystem.getAudioInputStream(getAudioData());
            AudioSystem.write(ais, type, out);
        } catch (UnsupportedAudioFileException uafe) {
            throw new AudioException(uafe);
        }
    }

    private Clip createClip() throws LineUnavailableException, UnsupportedAudioFileException, IOException {
        Clip newClip = null;
        try {
            AudioInputStream ais = AudioSystem.getAudioInputStream(getAudioData());
            AudioFormat format = ais.getFormat();
            AudioFormat.Encoding encoding = format.getEncoding();
            if ((encoding == AudioFormat.Encoding.ULAW) || (encoding == AudioFormat.Encoding.ALAW)) {
                AudioFormat tmp = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, format.getSampleRate(), format.getSampleSizeInBits() * 2, format.getChannels(), format.getFrameSize() * 2, format.getFrameRate(), true);
                ais = AudioSystem.getAudioInputStream(tmp, ais);
                format = tmp;
            }
            LOG.debug("Using AudioFormat " + format);
            DataLine.Info info = new DataLine.Info(Clip.class, format);
            newClip = (Clip) AudioSystem.getLine(info);
            newClip.open(ais);
            ais.close();
        } catch (IllegalArgumentException iae) {
            throw new VBoxException(iae.getMessage());
        }
        return newClip;
    }

    private FloatControl getGainControl(final Clip clip) {
        if (null == clip) {
            throw new IllegalArgumentException("null not allowed");
        }
        FloatControl result = null;
        if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            result = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
        }
        return result;
    }

    @Override
    protected final void finalize() throws Throwable {
        if ((null != clip) && (clip.isOpen())) {
            clip.close();
        }
        super.finalize();
    }
}
