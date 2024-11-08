package com.anotherbigidea.flash.sound;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import com.anotherbigidea.flash.SWFConstants;
import com.anotherbigidea.flash.interfaces.SWFTagTypes;
import com.anotherbigidea.flash.movie.Sound;
import com.anotherbigidea.flash.structs.Color;
import com.anotherbigidea.flash.writers.SWFWriter;
import com.anotherbigidea.flash.writers.TagWriter;

/**
 * Raw Audio Utilities
 */
public class RawHelper {

    /**
     * Read an audio input file that is supported by the Java Sound API
     * @param soundBlocks used to return the sound blocks (byte[]) - can be null
     */
    public static SoundStreamHead streamingBlocks(InputStream audioFile, int framesPerSecond, ArrayList soundBlocks) throws IOException, UnsupportedAudioFileException {
        AudioInputStream audioIn = AudioSystem.getAudioInputStream(audioFile);
        AudioFormat format = audioIn.getFormat();
        int frameSize = format.getFrameSize();
        boolean isStereo = format.getChannels() == 2;
        boolean is16Bit = format.getSampleSizeInBits() > 8;
        int sampleRate = (int) format.getSampleRate();
        int rate = SWFConstants.SOUND_FREQ_5_5KHZ;
        if (sampleRate >= 44000) sampleRate = 44000; else if (sampleRate >= 22000) sampleRate = 22000; else if (sampleRate >= 11000) sampleRate = 11000; else sampleRate = 5500;
        if (sampleRate == 44000) rate = SWFConstants.SOUND_FREQ_44KHZ; else if (sampleRate == 22000) rate = SWFConstants.SOUND_FREQ_22KHZ; else if (sampleRate == 11000) rate = SWFConstants.SOUND_FREQ_11KHZ;
        int samplesPerFrame = sampleRate / framesPerSecond;
        int blockSize = samplesPerFrame * (is16Bit ? 2 : 1) * (isStereo ? 2 : 1);
        SoundStreamHead soundHead = new SoundStreamHead(rate, is16Bit, isStereo, SWFConstants.SOUND_FORMAT_RAW, rate, is16Bit, isStereo, samplesPerFrame);
        if (soundBlocks == null) return soundHead;
        ByteArrayOutputStream bout = new ByteArrayOutputStream(blockSize + 1000);
        while (true) {
            bout.reset();
            byte[] b = new byte[frameSize];
            int num = 0;
            int read = 0;
            while (num < blockSize) {
                read = 0;
                while ((read = audioIn.read(b, read, frameSize - read)) < frameSize && read != -1) ;
                if (read == -1) break;
                bout.write(b);
                num += read;
            }
            while (num < blockSize) {
                bout.write(0x80);
                num++;
            }
            soundBlocks.add(bout.toByteArray());
            if (read == -1) break;
        }
        return soundHead;
    }

    public static Sound getSoundDefinition(InputStream audioFile, int framesPerSecond) throws IOException, UnsupportedAudioFileException {
        AudioInputStream audioIn = AudioSystem.getAudioInputStream(new BufferedInputStream(audioFile));
        AudioFormat format = audioIn.getFormat();
        int frameSize = format.getFrameSize();
        boolean isStereo = format.getChannels() == 2;
        boolean is16bit = format.getSampleSizeInBits() > 8;
        int sampleRate = (int) format.getSampleRate();
        int rate = SWFConstants.SOUND_FREQ_5_5KHZ;
        if (sampleRate >= 44000) sampleRate = 44000; else if (sampleRate >= 22000) sampleRate = 22000; else if (sampleRate >= 11000) sampleRate = 11000; else sampleRate = 5500;
        if (sampleRate == 44000) rate = SWFConstants.SOUND_FREQ_44KHZ; else if (sampleRate == 22000) rate = SWFConstants.SOUND_FREQ_22KHZ; else if (sampleRate == 11000) rate = SWFConstants.SOUND_FREQ_11KHZ;
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ADPCMHelper.FramedInputStream in = new ADPCMHelper.FramedInputStream(audioIn, frameSize);
        int b = 0;
        int count = 0;
        while ((b = in.read()) >= 0) {
            count++;
            bout.write(b);
        }
        int sampleCount = count;
        if (is16bit) sampleCount /= 2;
        if (isStereo) sampleCount /= 2;
        byte[] soundData = bout.toByteArray();
        return new Sound(SWFConstants.SOUND_FORMAT_RAW, rate, is16bit, isStereo, sampleCount, soundData);
    }

    /**
     * Makes a streaming SWF from a Java Sound compatible audio file.
     * args[0] = audio in filename
     * args[1] = SWF out filename
     */
    public static void main(String[] args) throws Exception {
        InputStream audioFile = new BufferedInputStream(new FileInputStream(args[0]));
        SWFWriter swfwriter = new SWFWriter(args[1]);
        SWFTagTypes tags = new TagWriter(swfwriter);
        tags.header(5, -1, 200, 200, 12, -1);
        tags.tagSetBackgroundColor(new Color(255, 255, 255));
        ArrayList blocks = new ArrayList();
        SoundStreamHead header = streamingBlocks(audioFile, 12, blocks);
        audioFile.close();
        header.write(tags);
        for (Iterator it = blocks.iterator(); it.hasNext(); ) {
            byte[] data = (byte[]) it.next();
            tags.tagSoundStreamBlock(data);
            tags.tagShowFrame();
        }
        tags.tagEnd();
    }
}
