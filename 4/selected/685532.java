package org.chizar.mp3union.engine;

import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.AudioFileFormat.Type;
import org.chizar.mp3union.util.Utils;
import javazoom.jl.converter.Converter;
import javazoom.jl.decoder.JavaLayerException;

public class AudioConverter {

    AudioFormat targetFormat;

    public static boolean isPcm(AudioFormat.Encoding encoding) {
        return encoding.equals(AudioFormat.Encoding.PCM_SIGNED) || encoding.equals(AudioFormat.Encoding.PCM_UNSIGNED);
    }

    public AudioInputStream convert(AudioInputStream stream, File temporaryFile) throws JavaLayerException, UnsupportedAudioFileException, IOException {
        if (!isPcm(stream.getFormat().getEncoding())) {
            stream = convertToPcm(stream);
        }
        if (stream.getFormat().getSampleRate() != targetFormat.getSampleRate()) {
            stream = convertToEqualsSampleRate(stream, targetFormat.getSampleRate());
        }
        if (stream.getFormat().getChannels() != targetFormat.getChannels()) {
            stream = convertChannels(stream, targetFormat.getChannels());
        }
        stream = saveToTemporaryFile(stream, temporaryFile);
        return stream;
    }

    private AudioInputStream convertChannels(AudioInputStream sourceStream, int nChannels) {
        AudioFormat sourceFormat = sourceStream.getFormat();
        AudioFormat targetFormat = new AudioFormat(sourceFormat.getEncoding(), sourceFormat.getSampleRate(), sourceFormat.getSampleSizeInBits(), nChannels, calculateFrameSize(nChannels, sourceFormat.getSampleSizeInBits()), sourceFormat.getFrameRate(), sourceFormat.isBigEndian());
        return AudioSystem.getAudioInputStream(targetFormat, sourceStream);
    }

    private static int calculateFrameSize(int nChannels, int nSampleSizeInBits) {
        return ((nSampleSizeInBits + 7) / 8) * nChannels;
    }

    private AudioInputStream convertToEqualsSampleRate(AudioInputStream stream, float sampleRate) {
        AudioFormat formatWithNeedeedSampleRate = new AudioFormat(stream.getFormat().getEncoding(), sampleRate, stream.getFormat().getSampleSizeInBits(), stream.getFormat().getChannels(), stream.getFormat().getFrameSize(), sampleRate, stream.getFormat().isBigEndian());
        return AudioSystem.getAudioInputStream(formatWithNeedeedSampleRate, stream);
    }

    public AudioInputStream convertToPcm(AudioInputStream stream) throws JavaLayerException, UnsupportedAudioFileException, IOException {
        Converter conv = new Converter();
        File temporaryFile = Utils.createTemporaryFile();
        conv.convert(stream, temporaryFile.getAbsolutePath(), null, null);
        return AudioSystem.getAudioInputStream(temporaryFile);
    }

    public AudioFormat getTargetFormat() {
        return targetFormat;
    }

    public void setTargetFormat(AudioFormat targetFormat) {
        this.targetFormat = targetFormat;
    }

    private AudioInputStream saveToTemporaryFile(AudioInputStream stream, File temporaryFile) throws IOException, UnsupportedAudioFileException {
        AudioSystem.write(stream, Type.WAVE, temporaryFile);
        return AudioSystem.getAudioInputStream(temporaryFile);
    }
}
