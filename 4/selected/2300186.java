package com.frinika.sequencer.model.audio;

import java.io.IOException;
import uk.org.toot.audio.core.AudioBuffer;
import uk.org.toot.audio.core.AudioProcess;

public class AudioReader extends AudioWavReader implements BlockableAudioProcess, AudioProcess {

    RandomAccessFileIF bfis;

    long startByte;

    long endByte;

    byte byteBuff[];

    long fPtrBytes;

    double sampleRate;

    private boolean closed;

    public AudioReader(RandomAccessFileIF fisIF) throws IOException {
        super(fisIF.getRandomAccessFile());
        sampleRate = format.getSampleRate();
        startByte = 0;
        endByte = audioDataByteLength;
        closed = endByte != 0;
        bfis = fisIF;
        fisIF.seek(audioDataStartBytePtr, false);
    }

    final long milliToByte(double milli) {
        return nChannels * 2 * (long) (milli * sampleRate / 1000000.0);
    }

    public void seekTimeInMicros(double micros, boolean realTime) throws IOException {
        long framePos = (long) (micros * sampleRate / 1000000.0);
        seekFrame(framePos, realTime);
    }

    /**
	 * 
	 * @param framePos
	 *            frame postition reltive to start of audio. e.g. zero is start
	 *            of audio.
	 * 
	 * @throws IOException
	 */
    public void seekFrame(long framePos, boolean realTime) throws IOException {
        fPtrBytes = framePos * 2 * nChannels;
        if (fPtrBytes >= startByte) {
            if (fPtrBytes < audioDataByteLength && fPtrBytes < endByte) bfis.seek(fPtrBytes + audioDataStartBytePtr, realTime);
        } else {
            bfis.seek(audioDataStartBytePtr + startByte, realTime);
        }
    }

    public boolean eof() {
        try {
            return fPtrBytes - audioDataStartBytePtr >= bfis.length();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    public void setBoundsInMicros(double start, double end) {
        assert (start <= end);
        startByte = Math.max(0, milliToByte(start));
        endByte = Math.min(audioDataByteLength, milliToByte(end));
    }

    public void close() {
    }

    public void open() {
    }

    /**
	 * 
	 * this version will block if the file is being written to and there is not enough
	 * data to fill the buffer
	 * 
	 * @param buffer
	 * @return
	 * @throws IOException 
	 */
    public void processAudioBlock(AudioBuffer buffer) throws Exception {
        if (closed) {
            processAudio(buffer);
            return;
        }
        int n = buffer.getSampleCount();
        while (n + fPtrBytes - audioDataStartBytePtr >= bfis.length()) {
            if (getLengthInFrames() > 0) {
                closed = true;
                break;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        int nBytes = nChannels * 2 * buffer.getSampleCount();
        boolean realTime = buffer.isRealTime();
        if (byteBuff == null || byteBuff.length != nBytes) {
            byteBuff = new byte[nBytes];
        }
        int nread = bfis.read(byteBuff, 0, nBytes, false);
        fill(buffer, 0, n);
    }

    /**
	 * 
	 * 
	 * Read from file into byte buffer and advance the fPtrBytes pointer it is
	 * OK to read before/after start/end of the file you'll just get zeros.
	 * fPtrBytes is advanced by appropriate byte count.
	 * 
	 * @param byteBuffer
	 *            buffer to fill
	 * @param offSet
	 *            offset into byteBuffer
	 * @param n
	 *            number of bytes to be read
	 * @throws IOException
	 */
    public int processAudio(AudioBuffer buffer) {
        int nBytes = nChannels * 2 * buffer.getSampleCount();
        boolean realTime = buffer.isRealTime();
        if (byteBuff == null || byteBuff.length != nBytes) {
            byteBuff = new byte[nBytes];
        }
        int startChunk = 0;
        int endChunk = nBytes;
        long minEndByte = Math.min(endByte, audioDataByteLength);
        try {
            if (fPtrBytes < startByte) {
                int nRead = (int) (nBytes + fPtrBytes - startByte);
                if (nRead > 0) {
                    startChunk = nBytes - nRead;
                    bfis.read(byteBuff, startChunk, nRead, realTime);
                } else {
                    fPtrBytes += nBytes;
                    return AUDIO_OK;
                }
            } else if (fPtrBytes <= minEndByte) {
                int nExtra = (int) (fPtrBytes + nBytes - minEndByte);
                if (nExtra > 0) {
                    endChunk = nBytes - nExtra;
                    bfis.read(byteBuff, 0, endChunk, realTime);
                } else {
                    int nread = bfis.read(byteBuff, 0, nBytes, realTime);
                    if (nread != nBytes) try {
                        throw new Exception(" Ooops only read " + nread + " out of " + nBytes);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else {
                fPtrBytes += nBytes;
                return AUDIO_OK;
            }
            processAudioImp(buffer, startChunk, endChunk);
        } catch (IOException e) {
            e.printStackTrace();
        }
        fPtrBytes += nBytes;
        return AUDIO_OK;
    }

    protected void processAudioImp(AudioBuffer buffer, int startChunk, int endChunk) {
        fill(buffer, startChunk, endChunk);
    }

    /**
	 * 
	 * 
	 * @param buffer
	 * @param startChunk
	 * @param endChunk
	 * @param gain1
	 * @param gain2
	 */
    protected void fillLinearInterpolate(AudioBuffer buffer, int startChunk, int endChunk, double gain1, double gain2) {
        double dG = (gain2 - gain1) / (endChunk - startChunk) / nChannels / 2.0;
        if (nChannels == 2) {
            float[] left = buffer.getChannel(0);
            float[] right = buffer.getChannel(1);
            for (int n = startChunk / 2; n < endChunk / 2; n++) {
                float sample = ((short) ((0xff & byteBuff[(n * 2) + 0]) + ((0xff & byteBuff[(n * 2) + 1]) * 256)) / 32768f);
                sample *= gain1;
                if (n % 2 == 0) left[n / 2] += sample; else right[n / 2] += sample;
                gain1 += dG;
            }
        } else {
            float[] left = buffer.getChannel(0);
            for (int n = startChunk; n < endChunk; n += 2) {
                float val = ((short) ((0xff & byteBuff[n]) + ((0xff & byteBuff[n + 1]) * 256)) / 32768f);
                left[n / 2] += val * gain1;
                gain1 += dG;
            }
        }
    }

    protected void fillConstantGain(AudioBuffer buffer, int startChunk, int endChunk, double gain) {
        if (nChannels == 2) {
            float[] left = buffer.getChannel(0);
            float[] right = buffer.getChannel(1);
            for (int n = startChunk / 2; n < endChunk / 2; n++) {
                float sample = ((short) ((0xff & byteBuff[(n * 2) + 0]) + ((0xff & byteBuff[(n * 2) + 1]) * 256)) / 32768f);
                sample *= gain;
                if (n % 2 == 0) left[n / 2] += sample; else right[n / 2] += sample;
            }
        } else {
            float[] left = buffer.getChannel(0);
            for (int n = startChunk; n < endChunk; n += 2) {
                float val = ((short) ((0xff & byteBuff[n]) + ((0xff & byteBuff[n + 1]) * 256)) / 32768f);
                left[n / 2] += val * gain;
            }
        }
    }

    protected void fill(AudioBuffer buffer, int startChunk, int endChunk) {
        if (nChannels == 2) {
            float[] left = buffer.getChannel(0);
            float[] right = buffer.getChannel(1);
            for (int n = startChunk / 2; n < endChunk / 2; n++) {
                float sample = ((short) ((0xff & byteBuff[(n * 2) + 0]) + ((0xff & byteBuff[(n * 2) + 1]) * 256)) / 32768f);
                if (n % 2 == 0) left[n / 2] += sample; else right[n / 2] += sample;
            }
        } else {
            float[] left = buffer.getChannel(0);
            for (int n = startChunk; n < endChunk; n += 2) {
                float val = ((short) ((0xff & byteBuff[n]) + ((0xff & byteBuff[n + 1]) * 256)) / 32768f);
                left[n / 2] += val;
            }
        }
    }
}
