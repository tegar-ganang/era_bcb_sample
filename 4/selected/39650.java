package org.xbup.library.audio.wave;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * Simple panel audio wave.
 *
 * @version 0.1.0 2011/03/19
 * @author SoundComp Project (http://soundcomp.sf.net)
 */
public class XBWave {

    public static long[] xbBlockPath = { 0, 1000, 0, 0 };

    private AudioFormat audioFormat;

    private List<byte[]> data;

    public int chunkSize;

    public XBWave() {
        audioFormat = null;
        chunkSize = 65520;
        data = new ArrayList<byte[]>();
    }

    public void loadFromFile(File soundFile) {
        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(soundFile);
            audioFormat = audioInputStream.getFormat();
            System.out.println(getAudioFormat());
            if ((audioFormat.getChannels() != 2) || (audioFormat.getSampleRate() != 44100) || (audioFormat.getEncoding() != AudioFormat.Encoding.PCM_SIGNED)) {
                System.out.println("Unable to load! Currently only 44kHz SIGNED 16bit Stereo is supported.");
                return;
            }
            loadRawFromStream(audioInputStream);
        } catch (UnsupportedAudioFileException ex) {
            Logger.getLogger(XBWave.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(XBWave.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void loadRawFromStream(InputStream inputStream) {
        try {
            data = new ArrayList<byte[]>();
            byte[] buffer = new byte[chunkSize];
            int cnt;
            int offset = 0;
            while ((cnt = inputStream.read(buffer, offset, buffer.length - offset)) != -1) {
                if (cnt + offset < chunkSize) {
                    offset = offset + cnt;
                } else {
                    data.add(buffer);
                    buffer = new byte[chunkSize];
                    offset = 0;
                }
            }
            if (offset > 0) {
                byte[] tail = new byte[offset];
                System.arraycopy(buffer, 0, tail, 0, offset - 1);
                data.add(tail);
            }
        } catch (IOException ex) {
            Logger.getLogger(XBWave.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void saveToFile(File soundFile) {
        saveToFile(soundFile, Type.WAVE);
    }

    public void saveToFile(File soundFile, Type fileType) {
        try {
            AudioSystem.write(new AudioInputStream(new WaveInputStream(), audioFormat, getLengthInTicks()), fileType, soundFile);
        } catch (IOException ex) {
            Logger.getLogger(XBWave.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private class WaveInputStream extends InputStream {

        private int blockPosition;

        private int offsetPosition;

        public WaveInputStream() {
            blockPosition = 0;
            offsetPosition = 0;
        }

        @Override
        public int read() throws IOException {
            if (blockPosition >= data.size()) return -1;
            byte[] block = data.get(blockPosition);
            int result = (int) block[offsetPosition] & 0xFF;
            offsetPosition++;
            if (offsetPosition >= block.length) {
                blockPosition++;
                offsetPosition = 0;
            }
            return result;
        }

        @Override
        public int read(byte[] output, int off, int len) throws IOException {
            if (blockPosition >= data.size()) return -1;
            if (output.length == 0) return 0;
            int length = len;
            if (length > output.length - off) {
                length = output.length - off;
            }
            byte[] block = data.get(blockPosition);
            if (length + offsetPosition >= block.length) {
                length = block.length - offsetPosition;
            }
            System.arraycopy(block, offsetPosition, output, off, length);
            offsetPosition += length;
            if (offsetPosition >= block.length) {
                blockPosition++;
                offsetPosition = 0;
            }
            return length;
        }

        @Override
        public int available() throws IOException {
            if (blockPosition >= data.size()) return 0;
            return 1;
        }
    }

    public InputStream getInputStream() {
        return new WaveInputStream();
    }

    public AudioInputStream getAudioInputStream() {
        return new AudioInputStream(new WaveInputStream(), audioFormat, getLengthInTicks());
    }

    /**
     * @return the data
     */
    public List<byte[]> getData() {
        return data;
    }

    /**
     * @param data the data to set
     */
    public void setData(List<byte[]> data) {
        this.data = data;
    }

    public int getRatioValue(int pos, int channel, int height) {
        int chunk = (pos * 4 + (channel * 2)) / chunkSize;
        int offset = (pos * 4 + (channel * 2)) % chunkSize;
        int value = 127 + getBlock(chunk)[offset] + ((getBlock(chunk)[offset + 1] + 127) << 8);
        return (int) ((long) value * height) >> 16;
    }

    public void setRatioValue(int pos, int value, int channel, int height) {
        int chunk = (pos * 4 + (channel * 2)) / chunkSize;
        int offset = (pos * 4 + (channel * 2)) % chunkSize;
        byte[] block = getBlock(chunk);
    }

    public byte[] getBlock(int pos) {
        if (pos >= data.size()) return null;
        return data.get(pos);
    }

    /**
     * @return the audioFormat
     */
    public AudioFormat getAudioFormat() {
        return audioFormat;
    }

    public int getLengthInTicks() {
        return ((data.size() - 1) * chunkSize + data.get(data.size() - 1).length) / 4;
    }

    public void append(byte[] data) {
    }

    public void apendTo(XBWave wave) {
        apendTo(wave, 0, getLengthInTicks());
    }

    public void apendTo(XBWave wave, int start, int length) {
    }

    public byte[] readChunk(int start, int length) {
        int pos = 0;
        return null;
    }

    public XBWave cut(int start, int length) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
