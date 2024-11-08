package jmms.processor.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import jmms.processor.MidiInputLocation;
import jmms.processor.MidiLocation;
import jmms.processor.MidiOutputLocation;
import jmms.processor.MidiProcessor;
import ren.util.Make;

/**
 * @author wooller
 * 
 * 19/01/2005
 * 
 * Copyright JEDI/Rene Wooller
 *  
 */
public class MidiLocationSetter extends JPanel implements Serializable {

    private MidiLocation midiLocation;

    private MidiProcessor mproc;

    private JTextField name = new JTextField();

    private JComboBox chan = new JComboBox();

    private JComboBox ctrl = new JComboBox();

    private JButton learn;

    /**
	 *  
	 */
    public MidiLocationSetter() {
        super();
        ActionListener cal = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == chan || e.getSource() == ctrl) {
                    if (midiLocation instanceof MidiInputLocation) {
                        mproc.setMidiInputLocation(midiLocation.getChannel(), midiLocation.getCtrlType(), ((Integer) chan.getSelectedItem()).intValue(), ((Integer) ctrl.getSelectedItem()).intValue());
                    } else if (midiLocation instanceof MidiOutputLocation) {
                        midiLocation.setMidiController(((Integer) chan.getSelectedItem()).intValue(), ((Integer) ctrl.getSelectedItem()).intValue());
                    }
                }
            }
        };
        for (int i = 1; i < 17; i++) {
            chan.addItem(new Integer(i));
        }
        for (int i = 0; i < 128; i++) {
            ctrl.addItem(new Integer(i));
        }
        chan.addActionListener(cal);
        ctrl.addActionListener(cal);
    }

    public void construct(MidiLocation ml, MidiProcessor mpro) {
        this.setMidiLocation(ml);
        this.mproc = mpro;
        this.add(new JLabel("channel"));
        this.add(chan);
        this.add(new JLabel("controller"));
        this.add(ctrl);
        ActionListener al = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (e.getActionCommand().equals("learn MIDI ")) {
                    mproc.setLearning(midiLocation);
                    learn.setText("stop learn");
                } else if (e.getActionCommand().equals("stop learn")) {
                    mproc.setLearning(null);
                    learn.setText("learn MIDI ");
                }
            }
        };
        learn = Make.button("learn MIDI ", "detect midi input and set it", al);
        this.add(learn);
        this.setSize(40, 40);
    }

    public MidiLocation getMidiLocation() {
        return midiLocation;
    }

    public void setMidiLocation(MidiLocation midiLocation) {
        this.midiLocation = midiLocation;
    }
}
