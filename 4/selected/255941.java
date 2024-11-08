package org.aubg.comparisong.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.AudioFormat.Encoding;

/*******************************************************************************
 * @author Joro Simple player to work with .mp3 and .wav files. Uses java sound.
 */
public class AudioPlayer {

    /**
	 * Play_Audio Class to play a selected song. Invoked by play button
	 */
    private class Play_Audio extends Thread {

        byte buffer[] = new byte[10000];

        /**
		 * Thread event which handles audio playback
		 */
        public void run() {
            try {
                sourceDataLine.open(format);
                sourceDataLine.start();
                int continue_play;
                while ((continue_play = ais.read(buffer, 0, buffer.length)) != -1 && stopPlayback == false) {
                    if (continue_play > 0) {
                        sourceDataLine.write(buffer, 0, continue_play);
                    }
                }
                sourceDataLine.drain();
                sourceDataLine.close();
            } catch (Exception e) {
            }
        }
    }

    ByteArrayOutputStream out = null;

    public boolean running = false;

    AudioInputStream ais = null;

    AudioFormat format = null;

    SourceDataLine sourceDataLine;

    public boolean stopPlayback = false;

    /**
	 * Plays the recorded in out field ByteArrayOutputStream.
	 * Not  implemented in current version
	 */
    public void playRecorded() {
        if (this.out == null) {
            System.out.println("error in saved recorded audio in buffer ");
        }
        byte audio[] = out.toByteArray();
        InputStream input = new ByteArrayInputStream(audio);
        final AudioFormat format = getFormat();
        final AudioInputStream ais = new AudioInputStream(input, format, audio.length / format.getFrameSize());
        try {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            final SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();
            int bufferSize = (int) format.getSampleRate() * format.getFrameSize();
            byte buff[] = new byte[bufferSize];
            try {
                int bytesRead = 0;
                while (bytesRead > -1) {
                    bytesRead = ais.read(buff, 0, buff.length);
                    if (bytesRead > -1) line.write(buff, 0, bytesRead);
                }
                line.drain();
                line.close();
            } catch (IOException e) {
                System.err.println("I/O problems: " + e);
            }
        } catch (LineUnavailableException e) {
            System.err.println("Line unavailable: " + e);
            System.exit(-4);
        }
    }

    /***************************************************************************
	 * Decodes (possibly) encoded format into playable decoded
	 * 
	 * @param inputStream
	 * @return
	 */
    private static AudioFormat decodeFormat(AudioInputStream inputStream) {
        AudioFormat songFormat = inputStream.getFormat();
        Encoding enc = AudioFormat.Encoding.PCM_SIGNED;
        float samplingRate = songFormat.getSampleRate();
        int samplingSize = 16;
        int channels = songFormat.getChannels();
        int frameSize = channels * samplingSize / 8;
        float framesRate = samplingRate;
        AudioFormat decodedFormat = new AudioFormat(enc, samplingRate, samplingSize, channels, frameSize, framesRate, false);
        return decodedFormat;
    }

    /***************************************************************************
	 * Logic to record a user humming a query. Not implemented in current
	 * version
	 */
    public void captureAudio() {
        try {
            final AudioFormat format = getFormat();
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            final TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();
            Runnable runner = new Runnable() {

                int bufferSize = (int) format.getSampleRate() * format.getFrameSize();

                byte buffer[] = new byte[bufferSize];

                public void run() {
                    out = new ByteArrayOutputStream();
                    running = true;
                    try {
                        while (running) {
                            int count = line.read(buffer, 0, buffer.length);
                            if (count > 0) {
                                out.write(buffer, 0, count);
                            }
                        }
                        out.close();
                    } catch (IOException e) {
                        System.err.println("I/O problems: " + e);
                        System.exit(-1);
                    }
                }
            };
            Thread captureThread = new Thread(runner);
            captureThread.start();
        } catch (LineUnavailableException e) {
            System.err.println("Line unavailable: " + e);
            System.exit(-2);
        }
    }

    private AudioFormat getFormat() {
        float sampleRate = 44100;
        int sampleSizeInBits = 16;
        int channels = 2;
        boolean signed = true;
        boolean bigEndian = true;
        return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
    }

    /**
	 * Takes the Byte array output stream and saves it into a file. Not
	 * implemented in current version
	 */
    public void saveBufferedInFile() {
        if (this.out == null) {
            System.err.println("saving recorded query");
        }
        byte[] bufferedAudioData = out.toByteArray();
        InputStream byteArrayInputStream = new ByteArrayInputStream(bufferedAudioData);
        AudioFormat format = getFormat();
        AudioInputStream mOutput = new AudioInputStream(byteArrayInputStream, format, bufferedAudioData.length / format.getFrameSize());
        writeToFile(mOutput);
    }

    /**
	 * saves an input stream to a wav file. Not implemented in current version
	 */
    private void writeToFile(AudioInputStream outputStream) {
        File outputFile = new File(AudioConstants.RECORDEDQUERYPATH);
        try {
            AudioSystem.write(outputStream, AudioFileFormat.Type.WAVE, outputFile);
        } catch (IOException e) {
            System.err.println("Creating file for recorded Query");
        }
    }

    /**
	 * * PLays an audio input stream that is in ais field using format that is
	 * in format field
	 * 
	 * @param soundFile
	 */
    public void play(File soundFile) {
        try {
            AudioInputStream auis = AudioSystem.getAudioInputStream(soundFile);
            AudioFormat decodedFormat = decodeFormat(auis);
            AudioInputStream decodedStream = AudioSystem.getAudioInputStream(decodedFormat, auis);
            ais = decodedStream;
            format = decodedFormat;
            DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, format);
            sourceDataLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
            stopPlayback = false;
            new Play_Audio().start();
        } catch (Exception error) {
        }
    }
}
