package com.frinika.sequencer.model.audio;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import javax.sound.sampled.AudioFormat;
import uk.org.toot.audio.core.AudioBuffer;
import uk.org.toot.audio.core.AudioProcess;

/**
 * @deprecated   TODO use AudioReader 
 */
public class AudioClipReader implements DoubleDataSource, AudioProcess {

    DAudioReader reader;

    byte byteBuffer[];

    float bPtr[][] = new float[2][];

    private int nch;

    private long startFrame;

    /**
	 * 
	 * @param clipFile
	 *            file with audio
	 * @param startFrame
	 *            position in frames relative to start of sequence.
	 * @throws IOException
	 */
    public AudioClipReader(File clipFile, long startFrame) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(clipFile, "r");
        this.reader = new DAudioReader(raf);
        reader.seekFrame(0);
        this.nch = reader.getChannels();
        System.out.println(" Channels = " + nch);
        this.startFrame = startFrame;
    }

    /**
	 * read from file into a double array.
	 * 
	 * @param buffer
	 *            double buffer
	 * @param offSet
	 *            start writing here
	 * @param nFrame
	 *            number of frames to read.
	 */
    public void readNextDouble(double buffer[], int offSet, int nFrame) {
        int nByte = nFrame * nch * 2;
        if (this.byteBuffer == null || this.byteBuffer.length != nByte) {
            this.byteBuffer = new byte[nByte];
        }
        try {
            this.reader.read(this.byteBuffer, 0, nByte);
            if (nch == 2) {
                for (int n = 0; n < 2 * nFrame; n++) {
                    buffer[offSet + n] = ((short) ((0xff & this.byteBuffer[(n * 2) + 0]) + ((0xff & this.byteBuffer[(n * 2) + 1]) * 256)) / 32768f);
                }
            } else {
                for (int n = 0; n < nFrame; n++) {
                    double val = ((short) ((0xff & this.byteBuffer[2 * n]) + ((0xff & this.byteBuffer[2 * n + 1]) * 256)) / 32768f);
                    buffer[offSet + n] = val;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * read from file into a double array.
	 * 
	 * @param buffer
	 *            double buffer
	 * @param offSet
	 *            start writing here
	 * @param nFrame
	 *            number of frames to read.
	 */
    public int processAudio(AudioBuffer buffer) {
        int nFrame = buffer.getSampleCount();
        int nByte = nFrame * nch * 2;
        if (this.byteBuffer == null || this.byteBuffer.length != nByte) {
            this.byteBuffer = new byte[nByte];
        }
        for (int i = 0; i < buffer.getChannelCount(); i++) {
            bPtr[i] = buffer.getChannel(i);
        }
        try {
            this.reader.read(this.byteBuffer, 0, nByte);
            for (int ch = 0; ch < nch; ch++) {
                for (int n = 0; n < nFrame; n++) {
                    int ptr = (n * nch + ch) * 2;
                    bPtr[ch][n] = ((short) ((0xff & this.byteBuffer[ptr + 0]) + ((0xff & this.byteBuffer[ptr + 1]) * 256)) / 32768f);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return AUDIO_OK;
    }

    /**
	 * 
	 * @param pos position relative to start of clip
	 */
    public void seekFrameInClip(long pos) {
        try {
            reader.seekFrame(pos);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Seek frame absolute frame postion pos-startFrame
	 */
    public void seekFrame(long pos) {
        try {
            reader.seekFrame(pos - startFrame);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * true if ptr is after the last data in the file.
	 */
    public boolean endOfFile() {
        return reader.eof();
    }

    public int getChannels() {
        return nch;
    }

    public AudioFormat getFormat() {
        return reader.getFormat();
    }

    public void open() {
    }

    public void close() {
    }

    public long getCurrentFrame() {
        return reader.getCurrentFrame();
    }

    public long getLengthInFrames() {
        return reader.getLengthInFrames();
    }
}
