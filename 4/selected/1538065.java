package com.elibera.ccs.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import com.elibera.ccs.app.MLEConfig;
import com.elibera.ccs.panel.HelperPanel;
import com.elibera.ccs.res.Msg;

/**
 * @author meisi
 *
 */
public class MP3Player implements Runnable {

    private byte[] audio;

    public boolean running = true;

    public MP3Player(byte[] b) throws Exception {
        audio = b;
    }

    boolean bForceConversion = false;

    boolean bBigEndian = false;

    int nSampleSizeInBits = 16;

    String strMixerName = null;

    int nExternalBufferSize = 128000;

    int nInternalBufferSize = AudioSystem.NOT_SPECIFIED;

    SourceDataLine line = null;

    public void stop() {
        try {
            if (line != null) line.stop();
        } catch (Exception e) {
        }
        try {
            if (line != null) line.close();
        } catch (Exception e) {
        }
        running = false;
    }

    public void play() throws Exception {
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new ByteArrayInputStream(audio));
        AudioInputStream din = null;
        AudioFormat audioFormat = audioInputStream.getFormat();
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
            System.out.println("AudioPlayer: cannot get SourceDataLine for format " + audioFormat);
            return;
        }
        line.start();
        int nBytesRead = 0;
        byte[] abData = new byte[nExternalBufferSize];
        while (nBytesRead != -1) {
            if (!running) break;
            try {
                nBytesRead = audioInputStream.read(abData, 0, abData.length);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (nBytesRead >= 0) {
                int nBytesWritten = line.write(abData, 0, nBytesRead);
            }
        }
        line.drain();
        line.close();
    }

    private static SourceDataLine getSourceDataLine(String strMixerName, AudioFormat audioFormat, int nBufferSize) {
        SourceDataLine line = null;
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat, nBufferSize);
        try {
            if (strMixerName != null) {
                Mixer.Info mixerInfo = getMixerInfo(strMixerName);
                if (mixerInfo == null) {
                    System.out.println("AudioPlayer: mixer not found: " + strMixerName);
                    System.exit(1);
                }
                Mixer mixer = AudioSystem.getMixer(mixerInfo);
                line = (SourceDataLine) mixer.getLine(info);
            } else {
                line = (SourceDataLine) AudioSystem.getLine(info);
            }
            line.open(audioFormat, nBufferSize);
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return line;
    }

    /**	TODO:
		This method tries to return a Mixer.Info whose name
		matches the passed name. If no matching Mixer.Info is
		found, null is returned.
	*/
    public static Mixer.Info getMixerInfo(String strMixerName) {
        Mixer.Info[] aInfos = AudioSystem.getMixerInfo();
        for (int i = 0; i < aInfos.length; i++) {
            if (aInfos[i].getName().equals(strMixerName)) {
                return aInfos[i];
            }
        }
        return null;
    }

    public void run() {
        try {
            play();
        } catch (javax.sound.sampled.UnsupportedAudioFileException au) {
            HelperPanel.showErrorPopUp(MLEConfig.conf.ep, Msg.getMsg("AUDIO_FORMAT_PLAYBACK_NOT_SUPPORTED"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
