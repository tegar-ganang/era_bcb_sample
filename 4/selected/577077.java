package org.chernovia.sims.wondrous;

import java.applet.Applet;
import java.awt.Button;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;
import org.chernovia.lib.music.midi.JMIDI;
import acme.MainFrame;

public class JWonderApp extends Applet implements ActionListener {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    private Button BUTT_NEW;

    private int machines = 0, MAX_MACHINES = 8;

    public static void main(String[] args) {
        new MainFrame(new JWonderApp(), 800, 600);
    }

    @Override
    public void init() {
        setBackground(Color.BLACK);
        JWondrousMachine.MACHINES = new Vector<JWondrousMachine>();
        JMIDI.load();
        JWondrousMachine.MAX_CHAN = JMIDI.getChannels().length;
        JWondrousMachine.MAX_INST = JMIDI.getInstruments().length;
        BUTT_NEW = new Button("Create New Machine");
        BUTT_NEW.addActionListener(this);
        add(BUTT_NEW);
    }

    @Override
    public void destroy() {
        JMIDI.unload();
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == BUTT_NEW && machines < MAX_MACHINES) {
            add(new JWonderPanel(new JWondrousMachine(machines, 0, 88, 250, 33, 99, 100, 500, 200, 3)));
            paintAll(getGraphics());
            machines++;
        }
    }
}
