package jmri.jmrit;

import java.applet.AudioClip;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.*;
import javax.sound.sampled.*;

/**
 * Provide simple way to load and play sounds in JMRI.
 * <P>
 * This is placed in the jmri.jmrit package by process of
 * elimination.  It doesn't belong in the base jmri package, as
 * it's not a basic interface.  Nor is it a specific implementation
 * of a basic interface, which would put it in jmri.jmrix.  It seems
 * most like a "tool using JMRI", or perhaps a tool for use with JMRI,
 * so it was placed in jmri.jmrit.
 * <P>
 * S@see jmri.jmrit.sound
 *
 * @author	Bob Jacobsen  Copyright (C) 2004, 2006
 * @author  Dave Duchamp  Copyright (C) 2011 - add streaming play of large files
 * @version	$Revision: 1.12 $
 */
public class Sound {

    public Sound(String filename) {
        if (needStreaming(filename)) {
            streaming = true;
            _fileName = filename;
        } else loadingSound(filename);
    }

    public static final long LARGE_SIZE = 100000;

    private String _fileName = "";

    private boolean streaming = false;

    private boolean streamingStop = false;

    /**
     * Play the sound once
     */
    public void play() {
        if (streaming) {
            Runnable streamSound = new StreamingSound(_fileName);
            Thread tStream = new Thread(streamSound);
            tStream.start();
        } else audioClip.play();
    }

    /**
     * Play the sound as a loop
     */
    public void loop() {
        if (streaming) {
            log.warn("Streaming this audio file, loop() not allowed");
        } else audioClip.loop();
    }

    /**
     * Stop playing as a loop
     */
    public void stop() {
        if (streaming) {
            streamingStop = true;
        } else audioClip.stop();
    }

    private boolean needStreaming(String fileName) {
        return (new File(fileName).length() > LARGE_SIZE);
    }

    /**
     * Load the requested sound resource, not streaming
     */
    void loadingSound(String filename) {
        try {
            URL url = (new java.io.File(filename)).toURI().toURL();
            audioClip = new sun.applet.AppletAudioClip(url);
        } catch (MalformedURLException e) {
            log.error("Error creating sound address: " + e.getMessage());
        }
    }

    /**
     * Play a sound from a buffer
     *
     */
    public static void playSoundBuffer(byte[] wavData) {
        float sampleRate = 11200.0f;
        int sampleSizeInBits = 8;
        int channels = 1;
        boolean signed = (sampleSizeInBits > 8 ? true : false);
        boolean bigEndian = true;
        AudioFormat format = new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
        SourceDataLine line;
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        if (!AudioSystem.isLineSupported(info)) {
            log.error("line not supported: " + info);
            return;
        }
        try {
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format);
        } catch (LineUnavailableException ex) {
            log.error("error opening line: " + ex);
            return;
        }
        line.start();
        line.write(wavData, 0, wavData.length);
    }

    /**
     * The actual sound, stored as an AudioClip
     */
    public AudioClip audioClip = null;

    public static class WavBuffer {

        @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "EI_EXPOSE_REP2")
        public WavBuffer(byte[] content) {
            buffer = content;
            int index = 12;
            while (index < buffer.length) {
                if (buffer[index] == 0x66 && buffer[index + 1] == 0x6D && buffer[index + 2] == 0x74 && buffer[index + 3] == 0x20) {
                    fmtOffset = index;
                    return;
                } else {
                    index = index + 8 + buffer[index + 4] + buffer[index + 5] * 256 + buffer[index + 6] * 256 * 256 + buffer[index + 7] * 256 * 256 * 256;
                    System.out.println("index now " + index);
                }
            }
            log.error("Didn't find fmt chunk");
        }

        @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "URF_UNREAD_FIELD")
        int fmtOffset;

        byte[] buffer;

        float getSampleRate() {
            return 11200.0f;
        }

        int getSampleSizeInBits() {
            return 8;
        }

        int getChannels() {
            return 1;
        }

        boolean getBigEndian() {
            return false;
        }

        boolean getSigned() {
            return (getSampleSizeInBits() > 8);
        }
    }

    public class StreamingSound implements Runnable {

        /**
		 * A runnable to stream in sound and play it
		 *  This method does not read in an entire large sound file at one time,
		 *        but instead reads in smaller chunks as needed.
		*/
        public StreamingSound(String fileName) {
            _file = fileName;
        }

        private String _file;

        private AudioInputStream stream = null;

        private AudioFormat format = null;

        private SourceDataLine line = null;

        public void run() {
            try {
                stream = AudioSystem.getAudioInputStream(new File(_file));
                format = stream.getFormat();
                log.debug("Audio format: " + format);
                if ((format.getEncoding() == AudioFormat.Encoding.ULAW) || (format.getEncoding() == AudioFormat.Encoding.ALAW)) {
                    AudioFormat newFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, format.getSampleRate(), format.getSampleSizeInBits() * 2, format.getChannels(), format.getFrameSize() * 2, format.getFrameRate(), true);
                    stream = AudioSystem.getAudioInputStream(newFormat, stream);
                    System.out.println("Converted Audio format: " + newFormat);
                    format = newFormat;
                    log.debug("new converted Audio format: " + format);
                }
            } catch (UnsupportedAudioFileException e) {
                log.error("AudioFileException " + e.getMessage());
                return;
            } catch (IOException e) {
                log.error("IOException " + e.getMessage());
                return;
            }
            if (!streamingStop) {
                try {
                    DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                    if (!AudioSystem.isLineSupported(info)) {
                        log.error("Audio play() does not support: " + format);
                        return;
                    }
                    line = (SourceDataLine) AudioSystem.getLine(info);
                    line.open(format);
                } catch (Exception e) {
                    log.error("Exception while creating Audio out " + e.getMessage());
                    return;
                }
            }
            if (streamingStop) {
                line.close();
                return;
            }
            int numRead = 0;
            byte[] buffer = new byte[line.getBufferSize()];
            log.debug("streaming sound buffer size = " + line.getBufferSize());
            line.start();
            try {
                int offset;
                while ((numRead = stream.read(buffer, 0, buffer.length)) >= 0) {
                    offset = 0;
                    while (offset < numRead) offset += line.write(buffer, offset, numRead - offset);
                }
            } catch (IOException e) {
                log.error("IOException while reading sound file " + e.getMessage());
            }
            line.drain();
            line.stop();
            line.close();
        }
    }

    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(Sound.class.getName());
}
