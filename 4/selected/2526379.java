package uk.co.drpj.midi.router;

import uk.co.drpj.midi.router.MidiEventRouter;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;
import java.util.Properties;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Transmitter;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import uk.org.toot.control.ControlLaw;
import uk.org.toot.control.FloatControl;
import uk.org.toot.control.LinearLaw;

/**
 *
 * @author pjl
 */
public class ControllerHub implements Receiver, Transmitter {

    Receiver recv;

    int chan;

    MidiEventRouter router;

    ControllerHandle o[];

    HashMap<Long, ControllerHandle> map = new HashMap<Long, ControllerHandle>();

    private static boolean isLinux = System.getProperty("os.name").equals("Linux");

    GridBagConstraints c = new GridBagConstraints();

    private int counter;

    JComponent controlPanel;

    JComponent midiMonitPanel;

    private boolean debug;

    JTextArea ta;

    private boolean velSense = false;

    private boolean mono = true;

    private ShortMessage noteOffMessage;

    private ShortMessage noteOffMessageX = new ShortMessage();

    public ControllerHub(int chan, ControllerHandle oo[]) {
        controlPanel = new JPanel();
        controlPanel.setLayout(new GridBagLayout());
        router = new MidiEventRouter();
        this.chan = chan;
        o = oo;
        c.gridx = 0;
        c.gridy = 0;
        controlPanel.add(new JLabel("Controller"), c);
        c.gridx = 2;
        controlPanel.add(new JLabel("Effect"), c);
        counter = 0;
        midiMonitPanel = new JPanel();
        midiMonitPanel.setLayout(new BoxLayout(midiMonitPanel, BoxLayout.Y_AXIS));
        ta = new JTextArea("Midi From Input Device: \n", 25, 30);
        ta.setLineWrap(true);
        JScrollPane sbrText = new JScrollPane(ta);
        sbrText.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        midiMonitPanel.add(sbrText);
    }

    public void setDebug(boolean yes) {
        debug = yes;
    }

    @Override
    public void send(MidiMessage mess, long timeStamp) {
        if (mess.getStatus() >= ShortMessage.MIDI_TIME_CODE) {
            return;
        }
        if (mess instanceof ShortMessage) {
            ShortMessage shm = (ShortMessage) mess;
            if (isLinux) {
                if (shm.getStatus() == ShortMessage.PITCH_BEND) {
                    short low = (byte) shm.getData1();
                    short high = (byte) shm.getData2();
                    int channel = shm.getChannel();
                    low = (byte) shm.getData1();
                    high = (byte) shm.getData2();
                    high = (short) ((high + 64) & 0x007f);
                    try {
                        shm.setMessage(ShortMessage.PITCH_BEND, channel, low, high);
                    } catch (InvalidMidiDataException e) {
                        e.printStackTrace();
                    }
                }
            }
            int cmd = shm.getCommand();
            int dat2 = shm.getData2();
            if (cmd == ShortMessage.NOTE_ON && dat2 == 0) {
                cmd = ShortMessage.NOTE_OFF;
            } else if (cmd == ShortMessage.NOTE_OFF) {
                dat2 = 0;
            }
            int chn = shm.getChannel();
            int dat1 = shm.getData1();
            if (!velSense && cmd == ShortMessage.NOTE_ON) {
                dat2 = 120;
                try {
                    shm.setMessage(cmd, chn, dat1, dat2);
                } catch (InvalidMidiDataException ex) {
                    Logger.getLogger(ControllerHub.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (debug) {
                ta.append(String.format(" cmd: %3d chn: %3d  data: %3d  %3d \n", cmd, chn, dat1, dat2));
                ta.setCaretPosition(ta.getDocument().getLength());
            }
            if (router.consume(mess, timeStamp)) {
                return;
            }
            if (mono) {
                if (cmd == ShortMessage.NOTE_ON) {
                    if (noteOffMessage != null) {
                        recv.send(noteOffMessage, -1);
                    }
                    noteOffMessage = noteOffMessageX;
                    try {
                        noteOffMessage.setMessage(ShortMessage.NOTE_OFF, chan, dat1, 0);
                    } catch (InvalidMidiDataException ex) {
                        Logger.getLogger(ControllerHub.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
            recv.send(mess, -1);
        }
    }

    public void close() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setReceiver(Receiver receiver) {
        this.recv = receiver;
    }

    public Receiver getReceiver() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void addVelsense() {
        counter++;
        c.gridy = counter++;
        c.gridx = 0;
        JPanel panel = new JPanel();
        final JCheckBox velNoteBut = new JCheckBox("Velocity Sensitive");
        panel.add(velNoteBut, c);
        velNoteBut.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                velSense = velNoteBut.isSelected();
            }
        });
        final JCheckBox monoBut = new JCheckBox("Polyphonic");
        panel.add(monoBut, c);
        monoBut.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                mono = !monoBut.isSelected();
            }
        });
        c.gridwidth = GridBagConstraints.REMAINDER;
        controlPanel.add(panel, c);
    }

    public JComponent getPanel() {
        return controlPanel;
    }

    public JComponent getMonitPanel() {
        return midiMonitPanel;
    }
}
