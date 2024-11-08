package net.sourceforge.texture.audio;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import net.sourceforge.texture.audio.exceptions.LowSampleRateException;
import net.sourceforge.texture.audio.exceptions.StreamLengthOutOfBoundsException;
import net.sourceforge.texture.audio.exceptions.UnallowedSampleSizeException;
import net.sourceforge.texture.audio.exceptions.UnavailableStreamSizeException;
import net.sourceforge.texture.audio.exceptions.UnsupportedAudioLineException;
import net.sourceforge.texture.common.ConfigManager;
import com.imagero.uio.RandomAccessInput;
import com.imagero.uio.UIOStreamBuilder;

public class AudioAnalyzer implements AudioAnalyzerResults {

    private final PlayerListener playerListener;

    private AudioInputStream audioInputStream;

    private RandomAccessInput randomAccessAudioStreamController;

    private HashMap<Object, RandomAccessInput> randomAccessAudioStreamMap;

    private AudioFormat audioFormat;

    private SourceDataLine sourceDataLine;

    private long audioInputStreamLength;

    private long durationInMicroseconds;

    private AudioAnalyzerResults results;

    public AudioAnalyzer(File audioFile, PlayerListener playerListener) throws IOException, UnsupportedAudioFileException, UnavailableStreamSizeException, LowSampleRateException, UnallowedSampleSizeException, StreamLengthOutOfBoundsException, UnsupportedAudioLineException, LineUnavailableException {
        this.playerListener = playerListener;
        this.prepareAudioStream(audioFile);
        this.prepareAudioLine();
    }

    public AudioAnalyzerResults getResults() {
        return this.results;
    }

    public void freeResources() throws IOException {
        this.randomAccessAudioStreamController = null;
        this.sourceDataLine = null;
        this.audioFormat = null;
        this.audioInputStream.close();
        this.audioInputStream = null;
    }

    @Override
    public RandomAccessInput getRandomAccessStream(Object consumer) throws IOException {
        RandomAccessInput randomAccessAudioStream = this.randomAccessAudioStreamMap.get(consumer);
        if (randomAccessAudioStream == null) {
            randomAccessAudioStream = this.randomAccessAudioStreamController.createInputChild(0, this.audioInputStreamLength, this.randomAccessAudioStreamController.getByteOrder(), false);
            this.randomAccessAudioStreamMap.put(consumer, randomAccessAudioStream);
        }
        return randomAccessAudioStream;
    }

    @Override
    public long getByteLength() {
        return this.audioInputStreamLength;
    }

    @Override
    public long getFrameLength() {
        return this.audioInputStream.getFrameLength();
    }

    @Override
    public long getDurationInMicroseconds() {
        return this.durationInMicroseconds;
    }

    @Override
    public AudioFormat getFormat() {
        return this.audioFormat;
    }

    @Override
    public SourceDataLine getLine() {
        return this.sourceDataLine;
    }

    private void prepareAudioStream(File audioFile) throws IOException, UnsupportedAudioFileException, UnavailableStreamSizeException, LowSampleRateException, UnallowedSampleSizeException, StreamLengthOutOfBoundsException {
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioFile);
        if (audioInputStream.getFrameLength() == AudioSystem.NOT_SPECIFIED || audioInputStream.getFormat().getFrameSize() == AudioSystem.NOT_SPECIFIED) {
            throw new UnavailableStreamSizeException();
        }
        if (audioInputStream.getFormat().getSampleRate() < ConfigManager.getAudioMinSampleRate()) {
            throw new LowSampleRateException();
        }
        if (Arrays.binarySearch(ConfigManager.getAudioAvailableSampleSizes(), audioInputStream.getFormat().getSampleSizeInBits()) == -1) {
            throw new UnallowedSampleSizeException();
        }
        if (audioInputStream.getFrameLength() > ConfigManager.getAudioMaxFrameLength()) {
            throw new StreamLengthOutOfBoundsException();
        }
        this.audioFormat = audioInputStream.getFormat();
        if (this.audioFormat.getEncoding() != AudioFormat.Encoding.PCM_SIGNED && this.audioFormat.getEncoding() != AudioFormat.Encoding.PCM_UNSIGNED) {
            AudioFormat targetFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, this.audioFormat.getSampleRate(), this.audioFormat.getSampleSizeInBits(), this.audioFormat.getChannels(), this.audioFormat.getFrameSize(), this.audioFormat.getFrameRate(), this.audioFormat.isBigEndian());
            audioInputStream = AudioSystem.getAudioInputStream(targetFormat, audioInputStream);
            this.audioFormat = targetFormat;
        }
        this.audioInputStream = audioInputStream;
        this.randomAccessAudioStreamController = new UIOStreamBuilder(this.audioInputStream).setByteOrder(RandomAccessInput.LITTLE_ENDIAN).create();
        this.randomAccessAudioStreamMap = new HashMap<Object, RandomAccessInput>();
        this.audioInputStreamLength = this.randomAccessAudioStreamController.length();
        this.durationInMicroseconds = (long) (((double) this.audioInputStream.getFrameLength() / (double) this.audioFormat.getSampleRate()) * 1000000);
    }

    private void prepareAudioLine() throws UnsupportedAudioLineException, LineUnavailableException {
        DataLine.Info datalineInfo = new DataLine.Info(SourceDataLine.class, this.audioFormat);
        if (!AudioSystem.isLineSupported(datalineInfo)) {
            throw new UnsupportedAudioLineException();
        }
        SourceDataLine sourceDataLine = (SourceDataLine) AudioSystem.getLine(datalineInfo);
        sourceDataLine.addLineListener(new AudioLineListener());
        this.sourceDataLine = sourceDataLine;
    }

    private class AudioLineListener implements LineListener {

        @Override
        public void update(LineEvent e) {
            if (e.getType().equals(LineEvent.Type.START)) {
                AudioAnalyzer.this.playerListener.notifyPlayerStarted();
            } else if (e.getType().equals(LineEvent.Type.STOP)) {
                AudioAnalyzer.this.playerListener.notifyPlayerStopped();
            } else if (e.getType().equals(LineEvent.Type.CLOSE)) {
                AudioAnalyzer.this.playerListener.notifyPlayerClosed();
            }
        }
    }
}
