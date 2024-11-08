package jbuzzer.audio;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Control;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * @author <a href='Achim.Westermann@gmx.de'>Achim Westermann</a>
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class Main {

    public static void main(String[] args) {
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        Mixer mixer = null;
        Line[] lines = null;
        for (int i = 0; i < mixers.length; i++) {
            System.out.println(mixers[i].toString());
            System.out.println(mixers[i].getDescription());
            mixer = AudioSystem.getMixer(mixers[i]);
            System.out.println(mixer.getClass().getName());
            linetest(mixer, 2);
            System.out.println("  Available source lines (input):");
            lines = mixer.getSourceLines();
            for (int j = 0; j < lines.length; j++) {
                linetest(lines[i], 4);
            }
            System.out.println("  Available target lines (output):");
            lines = mixer.getTargetLines();
            for (int j = 0; j < lines.length; j++) {
                linetest(lines[i], 4);
            }
        }
        try {
            URL baseurl = new File("C:/data/media/sound/own/").toURL();
            URL snd1 = new URL(baseurl, "burn.wav");
            AudioInputStream stream = AudioSystem.getAudioInputStream(snd1);
            AudioFormat format = stream.getFormat();
            if (format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
                format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, format.getSampleRate(), format.getSampleSizeInBits() * 2, format.getChannels(), format.getFrameSize() * 2, format.getFrameRate(), true);
                stream = AudioSystem.getAudioInputStream(format, stream);
            }
            SourceDataLine.Info info = new DataLine.Info(SourceDataLine.class, stream.getFormat(), ((int) stream.getFrameLength() * format.getFrameSize()));
            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(stream.getFormat());
            line.start();
            int numRead = 0;
            byte[] buf = new byte[line.getBufferSize()];
            while ((numRead = stream.read(buf, 0, buf.length)) >= 0) {
                int offset = 0;
                while (offset < numRead) {
                    offset += line.write(buf, offset, numRead - offset);
                }
            }
            line.drain();
            line.stop();
        } catch (MalformedURLException e) {
        } catch (IOException e) {
        } catch (LineUnavailableException e) {
        } catch (UnsupportedAudioFileException e) {
        }
    }

    private static void linetest(Line l, int indent) {
        System.out.println(l.getLineInfo().toString() + " :");
        Control[] controls = l.getControls();
        String spaces = spaces(indent);
        System.out.println(spaces + "Controls:");
        for (int i = 0; i < controls.length; i++) {
            System.out.println(spaces + "  " + controls[i].toString());
            System.out.println(spaces + "  type: " + controls[i].getType().toString());
        }
    }

    private static String spaces(int howmuch) {
        StringBuffer ret = new StringBuffer();
        for (int i = 0; i < howmuch; i++) ret.append(' ');
        return ret.toString();
    }
}
