package org.nubo.encapsulation;

import java.io.File;
import java.io.IOException;
import java.lang.Thread;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.Mixer;
import org.nubo.util.De;

public class SPIPlayer implements PlayerInterface {

    private static final int EXTERNAL_BUFFER_SIZE = 128000;

    boolean pause = false;

    File f;

    boolean shouldRun;

    SourceDataLine line;

    PlayerController playerController;

    boolean mute;

    AudioInputStream audioInputStream = null;

    SPIPlayer(PlayerController p) {
        this.playerController = p;
    }

    public void pause(boolean pause) {
        De.bug(3, "Setting !!!PAUSE!!!" + pause);
        this.pause = pause;
        line.flush();
    }

    public void play(File f) {
        this.f = f;
        shouldRun = true;
        System.out.println("=========================> PLAY START");
        boolean bInterpretFilenameAsUrl = false;
        boolean bForceConversion = false;
        boolean bBigEndian = false;
        int nSampleSizeInBits = 16;
        String strMixerName = null;
        int nExternalBufferSize = EXTERNAL_BUFFER_SIZE;
        int nInternalBufferSize = AudioSystem.NOT_SPECIFIED;
        AudioFormat audioFormat = null;
        for (int i = 0; i < AudioSystem.getAudioFileTypes().length; i++) System.out.println(AudioSystem.getAudioFileTypes()[i].getExtension());
        try {
            audioInputStream = AudioSystem.getAudioInputStream(f);
            audioFormat = audioInputStream.getFormat();
        } catch (Exception e) {
            e.toString();
        }
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat, nInternalBufferSize);
        boolean bIsSupportedDirectly = AudioSystem.isLineSupported(info);
        if (!bIsSupportedDirectly || bForceConversion) {
            AudioFormat sourceFormat = audioFormat;
            AudioFormat targetFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sourceFormat.getSampleRate(), nSampleSizeInBits, sourceFormat.getChannels(), sourceFormat.getChannels() * (nSampleSizeInBits / 8), sourceFormat.getSampleRate(), bBigEndian);
            audioInputStream = AudioSystem.getAudioInputStream(targetFormat, audioInputStream);
            audioFormat = audioInputStream.getFormat();
        }
        line = getSourceDataLine(strMixerName, audioFormat, nInternalBufferSize);
        if (line == null) {
            System.exit(1);
        }
        line.start();
        int nBytesRead = 0;
        byte[] abData = new byte[nExternalBufferSize];
        while (nBytesRead != -1 && shouldRun) {
            if (pause) try {
                setMute(true);
                Thread.sleep(1000);
                setMute(false);
                continue;
            } catch (Exception e) {
            }
            ;
            try {
                nBytesRead = audioInputStream.read(abData, 0, abData.length);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (nBytesRead >= 0) {
                int nBytesWritten = line.write(abData, 0, nBytesRead);
            }
        }
        setMute(true);
        line.drain();
        line.close();
        System.out.println("=========================> PLAY STOP");
        return;
    }

    public void rewindBackward(int i) {
    }

    public void rewindFoward(int i) {
        try {
            long secondIsNBytes = (long) audioInputStream.getFormat().getFrameSize() * (long) audioInputStream.getFormat().getFrameRate();
            if (audioInputStream != null) {
                audioInputStream.skip(i * secondIsNBytes);
                line.flush();
            }
        } catch (Exception e) {
            De.bug(2, "Unable to rewind, not supported");
        }
    }

    public void stop() {
        shouldRun = false;
        setMute(true);
        line.flush();
    }

    public static Mixer.Info getMixerInfo(String strMixerName) {
        Mixer.Info[] aInfos = AudioSystem.getMixerInfo();
        for (int i = 0; i < aInfos.length; i++) {
            if (aInfos[i].getName().equals(strMixerName)) {
                return aInfos[i];
            }
        }
        return null;
    }

    private static SourceDataLine getSourceDataLine(String strMixerName, AudioFormat audioFormat, int nBufferSize) {
        SourceDataLine line = null;
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat, nBufferSize);
        try {
            if (strMixerName != null) {
                Mixer.Info mixerInfo = getMixerInfo(strMixerName);
                if (mixerInfo == null) {
                    System.exit(1);
                }
                Mixer mixer = AudioSystem.getMixer(mixerInfo);
                line = (SourceDataLine) mixer.getLine(info);
            } else {
                line = (SourceDataLine) AudioSystem.getLine(info);
            }
            line.open(audioFormat, nBufferSize);
        } catch (LineUnavailableException e) {
        } catch (Exception e) {
        }
        return line;
    }

    public boolean isMute() {
        return mute;
    }

    public void setMute(boolean mute) {
        if (line == null) return;
        this.mute = mute;
        BooleanControl muteControl = (BooleanControl) line.getControl(BooleanControl.Type.MUTE);
        muteControl.setValue(mute);
    }
}
