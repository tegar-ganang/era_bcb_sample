package com.dukesoftware.utils.sig.fft.test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

public class URLAudio {

    private AudioInputStream audioInputStream;

    private boolean isBigEndian;

    private int bytesPerFrame;

    private long frameLength;

    private int channels;

    private float sampleRate;

    private int sampleSize;

    private long sampleLength;

    private int bytesPerSample;

    private int samplesPerFrame;

    private String audioStreamDescription;

    /** Creates a new instance of URLAudioReader */
    URLAudio(String urlstr) {
        try {
            URL url = new URL(urlstr);
            audioInputStream = AudioSystem.getAudioInputStream(url);
            frameLength = audioInputStream.getFrameLength();
            if (audioInputStream.markSupported()) audioInputStream.mark((int) frameLength);
            isBigEndian = audioInputStream.getFormat().isBigEndian();
            bytesPerFrame = audioInputStream.getFormat().getFrameSize();
            channels = audioInputStream.getFormat().getChannels();
            sampleRate = audioInputStream.getFormat().getSampleRate();
            sampleSize = audioInputStream.getFormat().getSampleSizeInBits();
            bytesPerSample = sampleSize / 8;
            samplesPerFrame = bytesPerFrame / bytesPerSample;
            sampleLength = (frameLength / channels) * samplesPerFrame;
            audioStreamDescription = urlstr + " :\n " + Integer.toString(sampleSize) + " bits " + Float.toString(sampleRate) + " Hz " + Integer.toString(channels) + " channels " + Long.toString(sampleLength) + " samples/channel " + Float.toString((float) sampleLength / (float) sampleRate) + " seconds.";
        } catch (MalformedURLException ex) {
            System.out.println(ex.getMessage());
        } catch (UnsupportedAudioFileException ex) {
            System.out.println(ex.getMessage());
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    public long getSampleLength() {
        return sampleLength;
    }

    public String getAudioStreamDescription() {
        return audioStreamDescription;
    }

    public double[] readAudioStream(long beginFrom, int nSamples) {
        double[] readSamples = new double[nSamples];
        int interval = channels * bytesPerSample;
        int numBytes = nSamples * bytesPerSample;
        byte[] audioBytes = new byte[numBytes];
        int numBytesRead = 0;
        int numSamplesRead = 0;
        try {
            audioInputStream.reset();
            audioInputStream.skip(beginFrom * bytesPerSample * channels);
        } catch (IOException ex) {
            System.out.println("Can't place the pointer to " + beginFrom + "th samples");
        }
        try {
            if ((numBytesRead = audioInputStream.read(audioBytes)) != -1) {
                numSamplesRead = numBytesRead / bytesPerSample;
                for (int i = 0; i < numBytesRead; i += interval) {
                    if (bytesPerSample == 2) {
                        int high, low;
                        if (isBigEndian) {
                            high = audioBytes[i] & 0xff;
                            low = audioBytes[i + 1] & 0xff;
                        } else {
                            low = audioBytes[i] & 0xff;
                            high = audioBytes[i + 1] & 0xff;
                        }
                        short value = (short) (high << 8 | low);
                        readSamples[i / interval] = (float) value / (float) (2 << 15);
                    } else {
                        readSamples[i / interval] = (float) audioBytes[i] / 255.0f;
                    }
                }
            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        return (readSamples);
    }
}
