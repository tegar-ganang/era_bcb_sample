package org.triviatime.client;

import java.io.*;
import javax.sound.sampled.*;
import java.net.*;

public class WaveFile {

    private Clip m_clip;

    /** constructor */
    public WaveFile(String path) {
        try {
            AudioInputStream stream = AudioSystem.getAudioInputStream(new File(path));
            AudioFormat format = stream.getFormat();
            if ((format.getEncoding() == AudioFormat.Encoding.ULAW) || (format.getEncoding() == AudioFormat.Encoding.ALAW)) {
                AudioFormat fmt = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, format.getSampleRate(), format.getSampleSizeInBits() * 2, format.getChannels(), format.getFrameSize() * 2, format.getFrameRate(), true);
                stream = AudioSystem.getAudioInputStream(fmt, stream);
                format = fmt;
            }
            DataLine.Info info = new DataLine.Info(Clip.class, stream.getFormat(), ((int) stream.getFrameLength() * format.getFrameSize()));
            m_clip = (Clip) AudioSystem.getLine(info);
            m_clip.open(stream);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public WaveFile(URL path) {
        try {
            AudioInputStream stream = AudioSystem.getAudioInputStream(path);
            AudioFormat format = stream.getFormat();
            if ((format.getEncoding() == AudioFormat.Encoding.ULAW) || (format.getEncoding() == AudioFormat.Encoding.ALAW)) {
                AudioFormat fmt = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, format.getSampleRate(), format.getSampleSizeInBits() * 2, format.getChannels(), format.getFrameSize() * 2, format.getFrameRate(), true);
                stream = AudioSystem.getAudioInputStream(fmt, stream);
                format = fmt;
            }
            DataLine.Info info = new DataLine.Info(Clip.class, stream.getFormat(), ((int) stream.getFrameLength() * format.getFrameSize()));
            m_clip = (Clip) AudioSystem.getLine(info);
            m_clip.open(stream);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** */
    void play() {
        m_clip.setFramePosition(0);
        m_clip.start();
    }
}
