package edu.mapi.ir.gui;

import java.applet.AudioClip;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import sun.applet.AppletAudioClip;

/**
 * @author ZP
 */
public class AudioClips {

    public static AudioClip r2d2a = createAudioClip("sounds/r2d2a.wav");

    public static AudioClip r2d2b = createAudioClip("sounds/r2d2b.wav");

    public static AudioClip r2d2c = createAudioClip("sounds/r2d2c.wav");

    public static AudioClip r2d2d = createAudioClip("sounds/r2d2d.wav");

    public static AudioClip r2d2e = createAudioClip("sounds/r2d2e.wav");

    public static AudioClip r2d2f = createAudioClip("sounds/r2d2f.wav");

    public static AudioClip radio = createAudioClip("sounds/radio.wav");

    public static AudioClip hit = createAudioClip("sounds/hit.wav");

    public static byte[] readStream(InputStream stream) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int numBytes = 0;
        byte[] buf = new byte[4096];
        try {
            while ((numBytes = stream.read(buf)) > 0) baos.write(buf, 0, numBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0];
        }
        return baos.toByteArray();
    }

    public static AudioClip createAudioClip(String filename) {
        return new AppletAudioClip(readStream(ClassLoader.getSystemResourceAsStream(filename)));
    }
}
