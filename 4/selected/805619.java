package uk.org.toot.swingui.midiui.sequenceui;

import java.awt.event.MouseEvent;
import java.awt.Point;
import java.awt.Graphics;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.InvalidMidiDataException;
import uk.org.toot.midi.sequence.MidiSequence;
import uk.org.toot.midi.sequence.MidiTrack;
import uk.org.toot.midi.sequence.edit.TrackSelection;
import uk.org.toot.midi.sequence.MidiNote;
import uk.org.toot.midi.sequence.edit.Paste;
import uk.org.toot.swingui.midiui.MidiColor;
import static uk.org.toot.midi.message.NoteMsg.*;

public class DrawTool extends PaletteTool {

    public DrawTool(Editor editor) {
        super("Draw", "Draw16", "Draw32", new Point(9, 28), editor);
    }

    protected int getNoteValue(SequenceView view, int x, int y) {
        return editor.getDefaultNote(view, x, y);
    }

    public void mouseDragged(MouseEvent e) {
        if (press == null) return;
        if (e.getSource() != press.getSource()) return;
        super.mouseDragged(e);
        SequenceView view = (SequenceView) e.getSource();
        Graphics g = view.getGraphics();
        int velocity = 100;
        MidiTrack t = view.getTopTrack();
        float hue = (Float) t.getClientProperty("Hue");
        g.setColor(MidiColor.asHSB(hue, view.saturation(velocity), 1.0f));
        view.paintNote(g, press.getValue(), velocity, view.snap(press.getTick()), view.tick(e.getX(), e.getY()), false);
        g.dispose();
    }

    public void mouseReleased(MouseEvent e) {
        if (press == null) return;
        SequenceView view = (SequenceView) e.getSource();
        if (e.getSource() == press.getSource()) {
            try {
                MidiSequence sequence = view.getSequence();
                long releaseTick = view.tick(e.getX(), e.getY());
                if (releaseTick <= press.getTick()) {
                    releaseTick = press.getTick() + (sequence.getResolution() / 32);
                }
                MidiTrack track = view.getTopTrack();
                int chan = track.getChannel();
                int velocity = editor.getDefaultVelocity(view);
                MidiMessage on = on(chan, press.getValue(), velocity);
                MidiMessage off = off(chan, press.getValue());
                MidiNote note = new MidiNote(new MidiEvent(on, view.snap(press.getTick())), new MidiEvent(off, releaseTick));
                TrackSelection notesel = new TrackSelection(track, note);
                edit(new Paste(notesel));
            } catch (InvalidMidiDataException ex) {
                ex.printStackTrace();
            } catch (CloneNotSupportedException cnse) {
            }
        }
        press = null;
    }
}
