import javax.sound.sampled.*;
import javax.swing.*;
import java.applet.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;

abstract class killThread extends Thread {

    public abstract void kill();
}

class AudioOutput extends killThread {

    private String file;

    private Boolean stop;

    public AudioOutput(String f) {
        file = f.intern();
        stop = false;
        this.start();
    }

    public void kill() {
        stop = true;
        try {
            this.interrupt();
        } catch (SecurityException SE) {
            System.out.println("SE: " + SE);
        }
    }

    public void run() {
        AudioInputStream stream;
        AudioFormat format;
        SourceDataLine.Info info;
        SourceDataLine line;
        byte[] buffer;
        try {
            stream = AudioSystem.getAudioInputStream(new File(file));
            format = stream.getFormat();
            if (format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
                format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, format.getSampleRate(), format.getSampleSizeInBits() * 2, format.getChannels(), format.getFrameSize() * 2, format.getFrameRate(), true);
                stream = AudioSystem.getAudioInputStream(format, stream);
            }
            info = new DataLine.Info(SourceDataLine.class, stream.getFormat(), ((int) stream.getFrameLength() * format.getFrameSize()));
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(stream.getFormat());
            line.start();
            int numRead = 0;
            buffer = new byte[line.getBufferSize()];
            while ((numRead = stream.read(buffer, 0, buffer.length)) >= 0 && !stop) {
                int offset = 0;
                while (offset < numRead && !stop) {
                    offset += line.write(buffer, offset, numRead - offset);
                }
            }
            line.drain();
            line.stop();
        } catch (IOException e) {
        } catch (LineUnavailableException e) {
        } catch (UnsupportedAudioFileException e) {
        } catch (NullPointerException e) {
        }
    }
}

class AudioWaiter extends killThread {

    private killThread oldAudio;

    private killThread newAudio;

    private String file;

    private Boolean stop;

    public AudioWaiter(String f, killThread a) {
        oldAudio = a;
        file = f.intern();
        newAudio = null;
        stop = false;
        this.start();
    }

    public void kill() {
        stop = true;
        if (newAudio != null) {
            newAudio.kill();
            try {
                newAudio.interrupt();
            } catch (SecurityException SE) {
                System.out.println("SE: " + SE);
            }
        }
    }

    public void run() {
        try {
            oldAudio.join();
        } catch (InterruptedException IE) {
        }
        if (!stop) {
            newAudio = new AudioOutput(file);
            try {
                newAudio.join();
            } catch (InterruptedException IE) {
            }
        }
    }
}

/**
 * AudioPlayer bruges til at afspille lyd. Klassen holder styr p� en r�kke tr�de som st�r for selve afspilningen.
 */
public class AudioPlayer {

    private killThread audio;

    /**
   * Constructoren initialiserer et nyt AudioPlayer objekt.
   */
    public AudioPlayer() {
        audio = null;
    }

    /**
   * playFile starter en tr�d som kan afspille den angivne lyd
   * @param file                Filnavn p� den lydfil der skal afspilles
   * @param kill                Angiver om den forrige lyd skal afbrydes
   * @param fast                Angiver om lyden skal vente p� at den forrige lyd er f�rdig
   * @param priority            Angiver om lyden SKAL afspilles. Hvis true afspilles lyden
   *                            uanset hvad, hvis false spilles den kun hvis ingen anden lyd er i gang.
   * @return Boolean            False hvis en ikke-prioriteret lyd ikke blev afspillet, ellers true.
   */
    public Boolean playFile(String file, Boolean kill, Boolean fast, Boolean priority) {
        if (audio != null) {
            if (audio.isAlive() && !priority) return false;
            if (kill) audio.kill();
            if (fast) audio = new AudioOutput(file); else audio = new AudioWaiter(file, audio);
        } else audio = new AudioOutput(file);
        return true;
    }
}
