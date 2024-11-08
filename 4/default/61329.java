import javax.sound.midi.*;
import java.io.File;
import java.io.IOException;
import javax.swing.*;
import javax.swing.filechooser.*;

class MidiWriter {

    final int PROGRAM = 192;

    final int NOTEON = 144;

    final int NOTEOFF = 128;

    public boolean recording = false;

    public boolean playing = false;

    private Sequencer sequencer;

    private Sequence sequence;

    private Track track;

    private boolean agentsAsTracks = false;

    public MidiWriter() {
        try {
            sequencer = MidiSystem.getSequencer();
            sequencer.setTempoInBPM((float) TempoAgent.tempoBPM);
            sequencer.open();
        } catch (Exception e) {
            System.out.println("Sequencer could not be opend.");
        }
    }

    public void close() {
        if (sequencer != null) {
            sequencer.close();
        }
        sequencer = null;
    }

    public void setReceiver() {
    }

    public void startPlaying() {
        try {
            sequencer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Sequencer started.");
        playing = true;
    }

    public void stopPlaying() {
        sequencer.stop();
        System.out.println("Sequencer stopped.");
        playing = false;
    }

    public void startRecording() {
        try {
            sequence = new Sequence(Sequence.PPQ, TempoAgent.resolution());
            track = sequence.createTrack();
        } catch (Exception e) {
            System.out.println("Couldn't create track.");
        }
        recording = true;
    }

    public void stopRecording() {
        recording = false;
        try {
            sequencer.setSequence(sequence);
        } catch (Exception e) {
            System.out.println("Couldn't set sequence.");
        }
    }

    public void writePatterns(BeatAgent[] baS, long offset) {
        int patternResolution = TempoAgent.resolution() * 4, ticksPrHit, position, gridDetail;
        BeatPattern bp;
        for (int i = 0; i < baS.length; i++) {
            bp = baS[i].getPattern();
            position = bp.getGridPosition();
            gridDetail = bp.getGridDetail();
            if (bp.getPatternLength() > 0 && !bp.mute) {
                ticksPrHit = patternResolution / bp.getGridDetail();
                for (int j = 0; j < bp.getPatternLength(); j++) {
                    try {
                        if ((bp.pattern[j].getUnBeat() == false) && (bp.pattern[j].getVelocity() > bp.getVelocityLimitLow()) && (bp.pattern[j].getVelocity() < bp.getVelocityLimitHigh())) {
                            createSequencerEvent(NOTEON, bp.pattern[j].getMidiChannel() - 1, bp.pattern[j].getBaseNote(), bp.pattern[j].getVelocity(), offset + (((j + (position - 1)) % gridDetail) * ticksPrHit));
                            createSequencerEvent(NOTEOFF, bp.pattern[j].getMidiChannel() - 1, bp.pattern[j].getBaseNote(), bp.pattern[j].getVelocity(), offset + (((j + (position - 1)) % gridDetail) * ticksPrHit) + bp.pattern[j].getDuration());
                        }
                    } catch (Exception e) {
                        System.out.println("Trouble writing midi data.");
                    }
                }
            }
        }
    }

    private void createSequencerEvent(int type, int chan, int num, int vel, long tick) {
        ShortMessage message = new ShortMessage();
        try {
            message.setMessage(type, chan, num, vel);
            MidiEvent event = new MidiEvent(message, tick);
            track.add(event);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void saveMidiFile(File file) {
        try {
            int[] fileTypes = MidiSystem.getMidiFileTypes(sequence);
            if (fileTypes.length == 0) {
                System.out.println("Can't save sequence");
            } else {
                if (MidiSystem.write(sequence, fileTypes[0], file) == -1) {
                    throw new IOException("Problems writing to file");
                }
            }
        } catch (SecurityException ex) {
            showInfoDialog();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void selectAndSaveMidiFile() {
        try {
            File file = new File(System.getProperty("user.dir"));
            JFileChooser chooser = new JFileChooser(file);
            chooser.setFileFilter(new javax.swing.filechooser.FileFilter() {

                public boolean accept(File f) {
                    if (f.isDirectory() || (f.isFile() && f.getName().endsWith(".mid"))) {
                        return true;
                    }
                    return false;
                }

                public String getDescription() {
                    return "Save as .mid file.";
                }
            });
            if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                saveMidiFile(chooser.getSelectedFile());
            }
        } catch (SecurityException e) {
            showInfoDialog();
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showInfoDialog() {
        final String msg = "When running this program as an applet these permissions\n" + "are necessary in order to load/save files and record audio :  \n\n" + "grant { \n" + "  permission java.io.FilePermission \"<<ALL FILES>>\", \"read, write\";\n" + "  permission javax.sound.sampled.AudioPermission \"record\"; \n" + "  permission java.util.PropertyPermission \"user.dir\", \"read\";\n" + "}; \n\n" + "The permissions need to be added to the .java.policy file.";
        new Thread(new Runnable() {

            public void run() {
                JOptionPane.showMessageDialog(null, msg, "Applet Info", JOptionPane.INFORMATION_MESSAGE);
            }
        }).start();
    }

    public boolean getAgentsAsTracks() {
        return agentsAsTracks;
    }

    public void setAgentsAsTracks(boolean b) {
        agentsAsTracks = b;
    }
}
