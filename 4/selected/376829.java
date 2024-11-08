package ch.unizh.ini.jaer.projects.gesture.virtualdrummer;

import javax.sound.midi.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * This program the MIDI percussion channel with a Swing window.  It monitors
 * keystrokes and mouse motion in the window and uses them to create music.
 * Keycodes between 35 and 81, inclusive, generate different percussive sounds.
 * See the VK_ constants in java.awt.event.KeyEvent, or just experiment.
 * Mouse position controls volume: move the mouse to the right of the window
 * to increase the volume.
 *
 * <p>
 * From http://onjava.com/pub/a/onjava/excerpt/jenut3_ch17/index1.html?page=2
 */
public class KeyboardDrumsDemo extends JFrame {

    MidiChannel channel;

    int velocity = 64;

    public static void main(String[] args) throws MidiUnavailableException {
        Synthesizer synthesizer = MidiSystem.getSynthesizer();
        synthesizer.open();
        JFrame frame = new KeyboardDrumsDemo(synthesizer);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(50, 128);
        frame.setVisible(true);
    }

    public KeyboardDrumsDemo(Synthesizer synth) {
        super("Drums");
        channel = synth.getChannels()[9];
        addKeyListener(new KeyAdapter() {

            public void keyPressed(KeyEvent e) {
                int key = e.getKeyCode();
                if (key >= 35 && key <= 81) {
                    channel.noteOn(key, velocity);
                }
            }

            public void keyReleased(KeyEvent e) {
                int key = e.getKeyCode();
                if (key >= 35 && key <= 81) channel.noteOff(key);
            }
        });
        addMouseMotionListener(new MouseMotionAdapter() {

            public void mouseMoved(MouseEvent e) {
                velocity = e.getX();
            }
        });
    }
}
