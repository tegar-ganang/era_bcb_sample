package demo;

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

    ControlPanelFactory factory;

    private static boolean isLinux = System.getProperty("os.name").equals("Linux");

    GridBagConstraints c = new GridBagConstraints();

    private int counter;

    JComponent controlPanel;

    JComponent midiMonitPanel;

    private boolean debug;

    Vector<ControlPanel> panels = new Vector<ControlPanel>();

    private boolean velSense = false;

    private boolean mono = true;

    private ShortMessage noteOffMessage;

    private ShortMessage noteOffMessageX = new ShortMessage();

    ControllerHub(int chan, ControllerHandle oo[]) {
        controlPanel = new JPanel();
        controlPanel.setLayout(new GridBagLayout());
        factory = new ControlPanelFactory();
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
    }

    void setDebug(boolean yes) {
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
                System.out.println(String.format(" cmd: %3d chn: %3d  data: %3d  %3d \n", cmd, chn, dat1, dat2));
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

    ControlPanel createKnob(String target, String src, int i) {
        ControlPanel pan = new ControlPanel(target, src, i);
        panels.add(pan);
        return pan;
    }

    void applyProperties(Properties properties) {
        if (properties == null) return;
        for (ControlPanel pan : panels) {
            Object val = properties.get(pan.handle.name);
            if (val != null) {
                pan.control.setValue(Float.valueOf(val.toString()));
            }
        }
    }

    void mergeInto(Properties properties) {
        for (ControlPanel pan : panels) {
            float val = pan.control.getValue();
            properties.put(pan.handle.name, Float.toString(val));
        }
    }

    void addVelsense() {
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

    public class ControlPanel {

        JComponent knob;

        ControllerHandle handle;

        JComboBox effectCombo;

        JComboBox cntrlCombo;

        FloatControl control;

        int id;

        ControlPanel(String target, String src, int i) {
            this.id = i;
            ControlLaw law = new LinearLaw(0.0f, 1.0f, "");
            control = new FloatControl(0, target, law, 1.0f, 0);
            knob = factory.createComponent(control, 1, false);
            cntrlCombo = new JComboBox(o);
            cntrlCombo.addItemListener(new ItemListener() {

                public void itemStateChanged(ItemEvent e) {
                    ControllerHandle h = (ControllerHandle) cntrlCombo.getSelectedItem();
                    Config.setProperty("control" + id, h.toString());
                    try {
                        ShortMessage mess = h.createMessage(chan, 0.0f);
                        long midiHash = MidiHashUtil.hashValue(mess);
                        router.assignMapping(midiHash, control);
                    } catch (InvalidMidiDataException ex) {
                        Logger.getLogger(ControllerHub.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            });
            effectCombo = new JComboBox(o);
            effectCombo.addItemListener(new ItemListener() {

                public void itemStateChanged(ItemEvent e) {
                    handle = (ControllerHandle) effectCombo.getSelectedItem();
                    Config.setProperty("effect" + id, handle.toString());
                    control.setName(handle.getName());
                }
            });
            for (ControllerHandle x : o) {
                if (x.toString().equals(target)) {
                    cntrlCombo.setSelectedItem(x);
                }
                if (x.toString().equals(src)) {
                    effectCombo.setSelectedItem(x);
                }
            }
            counter++;
            c.gridy = counter;
            c.gridx = 0;
            controlPanel.add(cntrlCombo, c);
            c.gridx++;
            controlPanel.add(knob, c);
            c.gridx++;
            controlPanel.add(effectCombo, c);
            control.addObserver(new Observer() {

                public void update(Observable o, Object arg) {
                    float val = (float) control.getValue();
                    System.out.println(handle + ":" + val);
                    if (handle == null) {
                        return;
                    }
                    try {
                        MidiMessage mess = handle.createMessage(chan, val);
                        recv.send(mess, -1);
                    } catch (InvalidMidiDataException ex) {
                        Logger.getLogger(ControlPanel.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            });
        }
    }
}
