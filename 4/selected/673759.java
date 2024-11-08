package de.frewert.vboxj;

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
import de.frewert.vboxj.vbox.MessageInfo;
import de.frewert.vboxj.vbox.Proxy;
import de.frewert.vboxj.vbox.VBoxException;

/**
 * <pre>
 * Copyright (C) 2001, 2003, 2005 Carsten Frewert. All Rights Reserved.
 * 
 * The VBox/J package (de.frewert.vboxj.*) is distributed under
 * the terms of the Artistic license.
 * </pre>
 * @author Carsten Frewert
 * &lt;<a href="mailto:frewert@users.sourceforge.net">
 * frewert@users.sourceforge.net</a>&gt;
 */
public class PlayableMessage {

    public static final int VBOX_HEADER_LENGTH = 372;

    private static float defaultGain;

    private MessageInfo _message;

    private Proxy _proxy;

    private Clip _clip;

    private volatile boolean paused;

    private volatile boolean playing;

    private FloatControl gainControl;

    /**
     * Create a PlayableMessage.
     *
     * @param msg the description of the message
     * @param proxy the proxy delivering the actual message data
     */
    public PlayableMessage(final MessageInfo msg, final Proxy proxy) {
        if ((null == msg) || (null == proxy)) {
            throw new IllegalArgumentException("null not allowed.");
        }
        this._message = msg;
        this._proxy = proxy;
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
        if (_clip != null) {
            _clip.stop();
            _clip.setFramePosition(0);
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
        if (_clip != null) {
            _clip.stop();
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
        if (_clip != null) {
            _clip.start();
        }
    }

    public static void setDefaultGain(final double gain) {
        PlayableMessage.defaultGain = (float) gain;
    }

    public void setGain(double dB) {
        if ((gainControl == null) && (_clip != null)) {
            this.gainControl = getGainControl(_clip);
        }
        if (null != gainControl) {
            float minGain = gainControl.getMinimum();
            float maxGain = gainControl.getMaximum();
            if (dB == 0) {
                dB = 0.1;
            }
            double logGain = Math.log(dB) / Math.log(10) * 50;
            if (logGain < 0) {
                logGain = 0;
            }
            dB = (logGain * (maxGain - minGain) / 100) + minGain;
            gainControl.setValue((float) dB);
        } else {
            System.out.println("No GainControl!");
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
        } catch (Exception e) {
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
        if (_clip == null) {
            _clip = createClip();
            gainControl = getGainControl(_clip);
            setGain(PlayableMessage.defaultGain);
        }
        return _clip;
    }

    public long getMicrosecondLength() throws AudioException {
        try {
            return getClip().getMicrosecondLength();
        } catch (Exception e) {
            throw new AudioException(e);
        }
    }

    public long getMicrosecondPosition() throws AudioException {
        try {
            return getClip().getMicrosecondPosition();
        } catch (Exception e) {
            throw new AudioException(e);
        }
    }

    public InputStream getAudioData() throws IOException {
        String name = _message.getFilename();
        InputStream rawAudio = _proxy.getMessageData(name);
        rawAudio.skip(VBOX_HEADER_LENGTH);
        int size = rawAudio.available();
        System.out.println("raw audio data: " + size + " bytes");
        byte[] snd = ".snd".getBytes();
        byte[] comment = "Created by VBox/J".getBytes();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        dos.write(snd, 0, snd.length);
        dos.writeInt(24 + comment.length);
        dos.writeInt(size);
        dos.writeInt(1);
        dos.writeInt(8000);
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
        Clip clip = null;
        try {
            AudioInputStream ais = AudioSystem.getAudioInputStream(getAudioData());
            AudioFormat format = ais.getFormat();
            AudioFormat.Encoding encoding = format.getEncoding();
            if ((encoding == AudioFormat.Encoding.ULAW) || (encoding == AudioFormat.Encoding.ALAW)) {
                AudioFormat tmp = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, format.getSampleRate(), format.getSampleSizeInBits() * 2, format.getChannels(), format.getFrameSize() * 2, format.getFrameRate(), true);
                ais = AudioSystem.getAudioInputStream(tmp, ais);
                format = tmp;
            }
            DataLine.Info info = new DataLine.Info(Clip.class, format);
            clip = (Clip) AudioSystem.getLine(info);
            clip.open(ais);
            ais.close();
        } catch (IllegalArgumentException iae) {
            throw new VBoxException(iae.getMessage());
        }
        return clip;
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

    protected final void finalize() throws Throwable {
        if ((null != _clip) && (_clip.isOpen())) {
            _clip.close();
        }
    }
}
