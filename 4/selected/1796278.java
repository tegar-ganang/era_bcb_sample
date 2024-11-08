package blue.utility;

import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import blue.BlueSystem;

public class SoundFileUtilities {

    public static int getNumberOfChannels(String soundFileName) throws IOException, UnsupportedAudioFileException {
        File soundFile = BlueSystem.findFile(soundFileName);
        AudioFileFormat aFormat = AudioSystem.getAudioFileFormat(soundFile);
        AudioFormat format = aFormat.getFormat();
        return format.getChannels();
    }

    public static float getDurationInSeconds(String soundFileName) throws IOException, UnsupportedAudioFileException {
        File soundFile = BlueSystem.findFile(soundFileName);
        AudioFileFormat aFormat = AudioSystem.getAudioFileFormat(soundFile);
        AudioFormat format = aFormat.getFormat();
        float duration = aFormat.getByteLength() / (format.getSampleRate() * (format.getSampleSizeInBits() / 8) * format.getChannels());
        return duration;
    }

    /**
     * @param fileName
     * @return
     */
    public static int getNumberOfFrames(String soundFileName) throws IOException, UnsupportedAudioFileException {
        File soundFile = BlueSystem.findFile(soundFileName);
        AudioFileFormat aFormat = AudioSystem.getAudioFileFormat(soundFile);
        return aFormat.getFrameLength();
    }

    /**
     * @param fileName
     * @return
     */
    public static float getSampleRate(String soundFileName) throws IOException, UnsupportedAudioFileException {
        File soundFile = BlueSystem.findFile(soundFileName);
        AudioFileFormat aFormat = AudioSystem.getAudioFileFormat(soundFile);
        AudioFormat format = aFormat.getFormat();
        return format.getSampleRate();
    }
}
