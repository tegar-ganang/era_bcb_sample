package org.ethontos.owlwatcher.view.data.xugglerViewLib;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import com.xuggle.xuggler.Global;
import com.xuggle.xuggler.IAudioSamples;
import com.xuggle.xuggler.IStreamCoder;

public class AudioProcessor {

    private static final int ABUFFERSIZE = 2000000;

    private static final int ABUFFERSHIFT = ABUFFERSIZE / 2;

    private byte[] audioBuffer = new byte[ABUFFERSIZE];

    private final AudioFormat audioFormat;

    private final IStreamCoder coder;

    private SourceDataLine mLine;

    private int savedSampleRate = -1;

    private long savedSampleSize = -1;

    private long audioBufferStartPTS;

    private long audioBufferOffset = 0;

    static Logger logger = Logger.getLogger(AudioProcessor.class.getName());

    public AudioProcessor(IStreamCoder p_coder) {
        logger.setLevel(Level.WARN);
        coder = p_coder;
        audioFormat = new AudioFormat(coder.getSampleRate(), (int) IAudioSamples.findSampleBitDepth(coder.getSampleFormat()), coder.getChannels(), true, false);
        final DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        try {
            mLine = (SourceDataLine) AudioSystem.getLine(info);
            mLine.open(audioFormat);
            mLine.start();
        } catch (LineUnavailableException e) {
            errorAndThrowException("could not open audio line: " + e.toString());
        }
    }

    public void addPacket(ProcessedPacket pPacket) {
        final IAudioSamples samples = pPacket.getSamples();
        byte[] bytes = samples.getData().getByteArray(0, samples.getSize());
        long timeStamp = samples.getPts();
        if (savedSampleRate == -1) {
            savedSampleRate = samples.getSampleRate();
        } else if (savedSampleRate != samples.getSampleRate()) logger.warn("Sample rate is inconsistent with rate set earlier in the stream");
        if (savedSampleSize == -1) {
            savedSampleSize = samples.getSampleSize();
        } else if (savedSampleSize != samples.getSampleSize()) logger.warn("Sample size is inconsistent with size set earlier in the stream");
        long nextTimeStamp = samples.getNextPts();
        long bufferStart = IAudioSamples.defaultPtsToSamples(timeStamp, savedSampleRate) * savedSampleSize;
        long bufferEnd = IAudioSamples.defaultPtsToSamples(nextTimeStamp, savedSampleRate) * savedSampleSize;
        if (bufferStart - audioBufferOffset > bufferEnd) {
            System.arraycopy(audioBuffer, ABUFFERSHIFT, audioBuffer, 0, ABUFFERSHIFT);
            audioBufferOffset += ABUFFERSHIFT;
            logger.warn("Buffer size exceeded");
        }
        try {
            System.arraycopy(bytes, 0, audioBuffer, (int) (bufferStart - audioBufferOffset), bytes.length);
        } catch (Exception e) {
            logger.error("An exception occurred while copying a packet into the audio buffer" + e);
        }
        logger.warn("timeStamp = " + timeStamp + " ;bufferStart = " + bufferStart + " ;bufferEnd =  " + bufferEnd + ";offset = " + audioBufferOffset);
    }

    public void playAtFrame(long timeStamp, long frameLength) {
        int frameWidth = (int) (IAudioSamples.defaultPtsToSamples(frameLength, savedSampleRate) * savedSampleSize);
        mLine.write(audioBuffer, (int) (IAudioSamples.defaultPtsToSamples(timeStamp, savedSampleRate) * savedSampleSize - audioBufferOffset), frameWidth);
    }

    public void playPacket(ProcessedPacket pPacket) {
        final IAudioSamples samples = pPacket.getSamples();
        byte[] bytes = samples.getData().getByteArray(0, samples.getSize());
        mLine.write(bytes, 0, samples.getSize());
        if (audioBufferStartPTS == Global.NO_PTS) audioBufferStartPTS = pPacket.getTimeStamp();
        long deltaTime = pPacket.getTimeStamp() - audioBufferStartPTS;
        long bufferPos = IAudioSamples.defaultPtsToSamples(deltaTime, coder.getSampleRate());
        samples.getData().get(0, audioBuffer, (int) bufferPos, samples.getSize());
    }

    /**
     * Method for retiring audio packets after their contents have been dumped to the audio line
     * @param pPacket processed audio packet to retire to the backward queue
     */
    private void retireAudioPacket(ProcessedPacket pPacket) {
        if (!pPacket.isAudio()) {
            errorAndThrowIllegalArgumentException("Expected to retire audio packet, found " + pPacket.toString() + " instead");
        }
        pPacket.clearSamples();
    }

    private void errorAndThrowIllegalArgumentException(String msg) {
        logger.error(msg);
        throw new IllegalArgumentException(msg);
    }

    private void errorAndThrowException(String msg) {
        logger.error(msg);
        throw new RuntimeException(msg);
    }
}
