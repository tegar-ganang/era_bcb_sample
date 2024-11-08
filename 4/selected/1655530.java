package com.realtime.crossfire.jxclient.sound;

import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import org.jetbrains.annotations.NotNull;

/**
 * A thread that plays a music file over and over until terminated.
 * @author Andreas Kirschbaum
 */
public class Processor implements Runnable {

    /**
     * The minimum factor for fading in/out effects.
     */
    private static final float MIN_VALUE = 1E-3F;

    /**
     * The step for the fading in/out factor. It is multiplied to the current
     * value for each sample.
     */
    private static final float VOLUME_STEP_PER_SAMPLE = 1.00005F;

    /**
     * The name of the music to play.
     */
    @NotNull
    private final String name;

    /**
     * The {@link AudioFileLoader} for loading audio files.
     */
    @NotNull
    private final AudioFileLoader audioFileLoader;

    /**
     * The current state: 0=fade in, 1=playing, 2=fade out, 3=terminate,
     * 4=terminate immediately.
     */
    private int state = 0;

    /**
     * The current volume. It is a factor that is multiplied to all sample
     * values while fading in/out.
     */
    private float volume = MIN_VALUE;

    /**
     * Creates a new instance.
     * @param name the music name to play
     * @param audioFileLoader the audio file loader for loading audio files
     */
    public Processor(@NotNull final String name, @NotNull final AudioFileLoader audioFileLoader) {
        this.name = name;
        this.audioFileLoader = audioFileLoader;
    }

    /**
     * Stops playing music. The music is faded out rather than cut off.
     * @param fadeOut Whether tp fade out the music (<code>true</code>) or to
     * cut it off (<code>false</code>).
     */
    public void terminate(final boolean fadeOut) {
        state = fadeOut ? 2 : 4;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        try {
            AudioInputStream audioInputStream = openAudioInputStream();
            try {
                final SourceDataLine sourceDataLine = AudioSystem.getSourceDataLine(audioInputStream.getFormat());
                final AudioFormat audioFormat = sourceDataLine.getFormat();
                if (audioFormat.getChannels() > 2) {
                    System.err.println("music " + name + ": cannot handle more than two channels");
                    return;
                }
                if (audioFormat.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
                    System.err.println("music " + name + ": encoding must be PCM_SIGNED");
                    return;
                }
                if (audioFormat.getSampleSizeInBits() != 16) {
                    System.err.println("music " + name + ": sample size must be 16 bits");
                    return;
                }
                if (audioFormat.isBigEndian()) {
                    System.err.println("music " + name + ": cannot handle little endian encoding");
                    return;
                }
                sourceDataLine.open(audioInputStream.getFormat());
                try {
                    sourceDataLine.start();
                    try {
                        final byte[] buf = new byte[8192];
                        while (state < 3 && !Thread.currentThread().isInterrupted()) {
                            int len = audioInputStream.read(buf, 0, buf.length);
                            if (len == -1) {
                                final AudioInputStream newAudioInputStream = openAudioInputStream();
                                if (!newAudioInputStream.getFormat().matches(audioInputStream.getFormat())) {
                                    newAudioInputStream.close();
                                    System.err.println("music " + name + ": file format has changed");
                                    break;
                                }
                                final AudioInputStream oldAudioInputStream = audioInputStream;
                                audioInputStream = newAudioInputStream;
                                oldAudioInputStream.close();
                                len = audioInputStream.read(buf, 0, buf.length);
                                if (len == -1) {
                                    System.err.println("music " + name + ": cannot re-read file");
                                    break;
                                }
                            }
                            switch(state) {
                                case 0:
                                    for (int i = 0; i + 3 < len; i += 4) {
                                        volume *= VOLUME_STEP_PER_SAMPLE;
                                        if (volume >= 1F) {
                                            state = 1;
                                            volume = 1F;
                                            break;
                                        }
                                        convertSample(buf, i);
                                        convertSample(buf, i + 2);
                                    }
                                    break;
                                case 1:
                                    break;
                                case 2:
                                    for (int i = 0; i + 3 < len; i += 4) {
                                        volume /= VOLUME_STEP_PER_SAMPLE;
                                        if (volume <= MIN_VALUE) {
                                            state = 3;
                                            len = i;
                                            break;
                                        }
                                        convertSample(buf, i);
                                        convertSample(buf, i + 2);
                                    }
                                    break;
                                default:
                                    throw new AssertionError();
                            }
                            sourceDataLine.write(buf, 0, len);
                        }
                        if (state != 4) {
                            sourceDataLine.drain();
                        }
                    } finally {
                        sourceDataLine.stop();
                    }
                } finally {
                    sourceDataLine.close();
                }
            } finally {
                audioInputStream.close();
            }
        } catch (final IOException ex) {
            System.err.println("music " + name + ": " + ex.getMessage());
        } catch (final LineUnavailableException ex) {
            System.err.println("music " + name + ": " + ex.getMessage());
        } catch (final UnsupportedAudioFileException ex) {
            System.err.println("music " + name + ": " + ex.getMessage());
        }
    }

    /**
     * Converts one audio sample according to the current {@link #volume}.
     * @param buf the buffer holding the sample
     * @param i the sample offset
     */
    private void convertSample(@NotNull final byte[] buf, final int i) {
        final float value = (short) ((buf[i] & 0xFF) + (buf[i + 1] & 0xFF) * 0x100) * volume;
        final short s = (short) value;
        if (s >= 0) {
            buf[i] = (byte) s;
            buf[i + 1] = (byte) (s / 0x100);
        } else {
            buf[i] = (byte) s;
            buf[i + 1] = (byte) ((s + 0x10000) / 0x100);
        }
    }

    /**
     * Opens and returns an audio stream for {@link #name}.
     * @return the audio stream
     * @throws IOException if the file cannot be opened
     * @throws UnsupportedAudioFileException if the file cannot be decoded
     */
    @NotNull
    private AudioInputStream openAudioInputStream() throws IOException, UnsupportedAudioFileException {
        return AudioSystem.getAudioInputStream(audioFileLoader.getInputStream(null, name));
    }
}
