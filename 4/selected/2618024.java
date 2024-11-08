package org.sam.util.sound;

import java.io.*;
import javax.sound.midi.*;
import javax.sound.sampled.*;

/**
 * 
 *
 */
public class Sounds {

    private static final long serialVersionUID = 1L;

    public static void playMidi(String fileName) {
        try {
            Sequencer sequencer = MidiSystem.getSequencer();
            sequencer.open();
            sequencer.setSequence(MidiSystem.getSequence(new File(fileName)));
            sequencer.setLoopCount(Clip.LOOP_CONTINUOUSLY);
            sequencer.start();
        } catch (MidiUnavailableException e) {
            e.printStackTrace();
        } catch (InvalidMidiDataException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void play(final InputStream data_input_stream, AudioFormat output_format) throws LineUnavailableException {
        final SourceDataLine output_line = (SourceDataLine) AudioSystem.getLine(new DataLine.Info(SourceDataLine.class, output_format));
        output_line.open();
        output_line.start();
        int tBuff = 4 << 10;
        byte[] output_buffer = new byte[tBuff];
        try {
            while (data_input_stream.available() > 0) {
                int read = data_input_stream.read(output_buffer, 0, tBuff);
                output_line.write(output_buffer, 0, read);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        output_line.drain();
        output_line.close();
    }

    public static void playWav(String fileName) {
        try {
            File sf = new File(fileName);
            play(AudioSystem.getAudioInputStream(sf), AudioSystem.getAudioFileFormat(sf).getFormat());
        } catch (UnsupportedAudioFileException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    public static void playTracker(String filename) {
        try {
            TrackerInputStream data_input_stream = new TrackerInputStream(new FileInputStream(filename));
            play(data_input_stream, data_input_stream.getAudioFormat());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    public static void playVorbisOgg(String filename) {
        try {
            OggInputStream data_input_stream = new OggInputStream(new FileInputStream(filename));
            play(data_input_stream, data_input_stream.getAudioFormat());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    public static void play(String filename) {
        String filenameUpperCase = filename.toUpperCase();
        if (filenameUpperCase.endsWith(".MID") || filenameUpperCase.endsWith(".MIDI")) playMidi(filename); else if (filenameUpperCase.endsWith(".WAV")) playWav(filename); else if (filenameUpperCase.endsWith(".OGG")) playVorbisOgg(filename); else if (filenameUpperCase.endsWith(".MOD") || filenameUpperCase.endsWith(".S3M") || filenameUpperCase.endsWith(".XM")) playTracker(filename);
    }

    public static void main(String arrgs[]) {
        play("resources/sounds/nstalgia.s3m");
        play("resources/sounds/snow.xm");
        play("resources/sounds/side_effects.mod");
    }
}
