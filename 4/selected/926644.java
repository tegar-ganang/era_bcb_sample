package com.totalchange.jizz.audio;

import java.io.IOException;
import java.io.InputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tritonus.share.sampled.Encodings;

public class JizzAudioSystem {

    private static final Logger logger = LoggerFactory.getLogger(JizzAudioSystem.class);

    public static JizzAudioInputStream convertToMp3(JizzAudioSource src, int bitRate) throws UnsupportedAudioFileException, IOException {
        if (logger.isTraceEnabled()) {
            logger.trace("Converting audio source " + src + " to mp3 with " + "destination bit rate " + bitRate);
        }
        JizzAudioInfo info = new JizzAudioInfo(src.getContributor(), src.getArtist(), src.getAlbum(), src.getTitle());
        AudioInputStream srcStream = src.getInputStream();
        AudioFormat srcFormat = srcStream.getFormat();
        if (logger.isTraceEnabled()) {
            logger.trace("Audio stream input format: " + srcFormat);
        }
        AudioFormat.Encoding destEncoding = Encodings.getEncoding("MPEG1L3");
        if (!AudioSystem.isConversionSupported(destEncoding, srcFormat)) {
            if (logger.isTraceEnabled()) {
                logger.trace("Direct conversion from " + srcFormat + " to " + destEncoding + " not possible - trying intermediate PCM format");
            }
            AudioFormat pcmFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, srcFormat.getSampleRate(), srcFormat.getSampleSizeInBits(), srcFormat.getChannels(), srcFormat.getFrameSize(), srcFormat.getFrameRate(), false);
            if (AudioSystem.isConversionSupported(pcmFormat, srcFormat)) {
                srcStream = AudioSystem.getAudioInputStream(pcmFormat, srcStream);
            } else {
                throw new UnsupportedAudioFileException("Cannot convert " + "source format " + srcFormat + " to destiantion format " + destEncoding);
            }
        }
        AudioInputStream destStream = AudioSystem.getAudioInputStream(destEncoding, srcStream);
        if (destStream == null) {
            throw new UnsupportedAudioFileException("Cannot convert " + "source format " + srcFormat + " to destination format " + destEncoding);
        }
        return new JizzAudioInputStream(info, destStream);
    }

    public static JizzAudioInputStream getJizzAudioInputStream(InputStream src) throws IOException {
        return new JizzAudioInputStream(src);
    }

    public static void main(String[] args) throws Exception {
        JizzAudioSource src = new JizzAudioSource() {

            public String getContributor() {
                return "Contributor";
            }

            public String getArtist() {
                return "Artist";
            }

            public String getAlbum() {
                return "Album";
            }

            public String getTitle() {
                return "Title";
            }

            public AudioInputStream getInputStream() throws IOException, UnsupportedAudioFileException {
                return AudioSystem.getAudioInputStream(new java.io.File("input.mp3"));
            }
        };
        JizzAudioInputStream in = JizzAudioSystem.convertToMp3(src, 64);
        java.io.OutputStream out = new java.io.FileOutputStream("output.jizz");
        byte[] buf = new byte[4 * 1024];
        int read = -1;
        while ((read = in.read(buf)) != -1) {
            out.write(buf, 0, read);
        }
        out.close();
        in.close();
    }
}
