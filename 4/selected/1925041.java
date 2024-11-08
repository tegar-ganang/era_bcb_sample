package com.cirnoworks.cas.impl.jsa;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import com.cirnoworks.cas.BGMStatus;
import com.cirnoworks.cas.SoundManager;
import com.cirnoworks.common.ResourceFetcher;

/**
 * @author cloudee
 * 
 */
public class JSASoundManager implements SoundManager {

    private final List<String> extNames = Arrays.asList("mid", "ogg", "wav", "MID", "OGG", "WAV");

    private final Set<String> midiExtNames = new TreeSet<String>(Arrays.asList("mid", "MID"));

    private final List<String> seExts = Arrays.asList("ogg", "wav", "OGG", "WAV");

    private String bgPath = "/res/bgm/";

    private String sePath = "/res/se/";

    private ResourceFetcher resourceFetcher;

    public static final AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2, 4, 44100, false);

    private JSAPlayer[] bgPlayers;

    private JSASoftMixer sePlayer;

    private int nowSe;

    public JSASoundManager(ResourceFetcher resourceFetcher, int bgChannels, int seChannels) throws LineUnavailableException {
        this.resourceFetcher = resourceFetcher;
        bgPlayers = new JSAPlayer[bgChannels];
        sePlayer = new JSASoftMixer();
        for (int i = 0; i < bgChannels; i++) {
            bgPlayers[i] = new JSAPlayer();
        }
    }

    public void playbg(int channel, String name, int loop, long pos) {
        if (name == null) {
            bgPlayers[channel].stop();
        } else {
            InputStream is = null;
            boolean midi = false;
            for (String ext : extNames) {
                String fn = bgPath + name + "." + ext;
                try {
                    URL u = resourceFetcher.getResource(fn);
                    if (u != null) is = u.openStream();
                } catch (IOException e) {
                }
                if (is != null) {
                    midi = midiExtNames.contains(ext);
                    break;
                }
            }
            bgPlayers[channel].play(name, is, midi, loop, pos);
        }
    }

    public void playse(byte[] content) {
        sePlayer.play(content);
    }

    public void setBGVolume(int channel, double volume) {
        bgPlayers[channel].setVolume((float) volume);
    }

    public void setSEVolume(double volume) {
        sePlayer.setVolume((float) volume);
    }

    public int getBgChannels() {
        return bgPlayers.length;
    }

    public int getSeChannels() {
        return sePlayer.getChannels();
    }

    public BGMStatus getBgStatus(int channel) {
        return bgPlayers[channel].getStatus();
    }

    public void stopSE() {
    }

    @Override
    public void stopBG() {
        for (JSAPlayer player : bgPlayers) {
            player.stop();
        }
    }

    @Override
    public byte[] loadSE(String seName) {
        InputStream is = null;
        for (String ext : seExts) {
            try {
                URL u = resourceFetcher.getResource("/res/se/" + seName + "." + ext);
                if (u != null) is = u.openStream();
            } catch (IOException e) {
            }
            if (is != null) {
                break;
            }
        }
        if (is == null) {
            return null;
        }
        byte[] buf = new byte[65536];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int read;
        try {
            while ((read = is.read(buf)) >= 0) {
                baos.write(buf, 0, read);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                is.close();
            } catch (Exception e) {
            }
        }
        is = new ByteArrayInputStream(baos.toByteArray());
        baos.reset();
        AudioInputStream ais = null;
        try {
            ais = AudioSystem.getAudioInputStream(format, AudioSystem.getAudioInputStream(is));
            while ((read = ais.read(buf)) >= 0) {
                baos.write(buf, 0, read);
            }
            return baos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                ais.close();
            } catch (Exception e) {
            }
        }
        return null;
    }

    public void destroy() {
    }
}
