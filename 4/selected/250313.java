package tjacobs.mp3;

import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.sampled.*;
import javax.swing.JFileChooser;
import java.io.*;

/**
 * Call play() with the file name of an MP3 and it will play that MP3
 */
public class MP3 {

    private MP3() {
        super();
    }

    private static class MP3Runnable implements Runnable {

        String file;

        MP3Runnable(String filename) {
            file = filename;
        }

        public void run() {
            play(file);
        }
    }

    public static void play(String filename, boolean createThread) {
        if (!createThread) {
            play(filename);
        } else {
            Runnable r = new MP3Runnable(filename);
            Thread t = new Thread(r);
            t.start();
        }
    }

    public static void play(InputStream stream) {
        try {
            AudioInputStream in = AudioSystem.getAudioInputStream(stream);
            AudioInputStream din = null;
            AudioFormat baseFormat = in.getFormat();
            AudioFormat decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, baseFormat.getSampleRate(), 16, baseFormat.getChannels(), baseFormat.getChannels() * 2, baseFormat.getSampleRate(), false);
            din = AudioSystem.getAudioInputStream(decodedFormat, in);
            rawplay(decodedFormat, din);
            in.close();
        } catch (UnsupportedAudioFileException ex) {
            ex.printStackTrace();
        } catch (IOException iox) {
            iox.printStackTrace();
        } catch (LineUnavailableException lue) {
            lue.printStackTrace();
        }
    }

    public static void play(String filename) {
        File file = new File(filename);
        play(file);
    }

    public static void play(File audiofile) {
        try {
            if (audiofile.getName().endsWith(".mid") || audiofile.getName().endsWith(".midi")) {
                Sequence sequence = MidiSystem.getSequence(audiofile);
                if (sequence != null) {
                    try {
                        final Sequencer sequencer = MidiSystem.getSequencer();
                        sequencer.open();
                        sequencer.setSequence(sequence);
                        sequencer.start();
                        new MetaEventListener() {

                            public void meta(MetaMessage ev) {
                                if (ev.getType() == 47) {
                                }
                            }
                        };
                    } catch (MidiUnavailableException ex) {
                        return;
                    }
                }
                return;
            }
            AudioInputStream in = AudioSystem.getAudioInputStream(audiofile);
            AudioInputStream din = null;
            AudioFormat baseFormat = in.getFormat();
            AudioFormat decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, baseFormat.getSampleRate(), 16, baseFormat.getChannels(), baseFormat.getChannels() * 2, baseFormat.getSampleRate(), false);
            din = AudioSystem.getAudioInputStream(decodedFormat, in);
            rawplay(decodedFormat, din);
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void rawplay(AudioFormat targetFormat, AudioInputStream din) throws IOException, LineUnavailableException {
        byte[] data = new byte[4096];
        SourceDataLine line = getLine(targetFormat);
        if (line != null) {
            line.start();
            @SuppressWarnings("unused") int nBytesRead = 0, nBytesWritten = 0;
            while (nBytesRead != -1) {
                nBytesRead = din.read(data, 0, data.length);
                if (nBytesRead != -1) nBytesWritten = line.write(data, 0, nBytesRead);
            }
            line.drain();
            line.stop();
            line.close();
            din.close();
        }
    }

    private static SourceDataLine getLine(AudioFormat audioFormat) throws LineUnavailableException {
        SourceDataLine res = null;
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        res = (SourceDataLine) AudioSystem.getLine(info);
        res.open(audioFormat);
        return res;
    }

    public static void main(String args[]) {
        JFileChooser chooser = new JFileChooser();
        File f = new File("C:/program files/englishotto/media/music");
        chooser.setCurrentDirectory(f);
        int result = chooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            File mp3 = chooser.getSelectedFile();
            play(mp3);
        }
    }
}
