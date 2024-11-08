package org.monome.pages;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Random;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.apache.commons.lang.StringEscapeUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import com.cloudgarden.layout.AnchorConstraint;
import com.cloudgarden.layout.AnchorLayout;

/**
 * The Machine Drum Interface page.  Usage information is available at:
 * 
 * http://code.google.com/p/monome-pages/wiki/MachineDrumInterfacePage
 *   
 * @author Tom Dinchak
 *
 */
public class MachineDrumInterfacePage implements Page, ActionListener {

    /**
	 * The MonomeConfiguration object this page belongs to
	 */
    MonomeConfiguration monome;

    /**
	 * The index of this page (the page number) 
	 */
    private int index;

    /**
	 * The GUI for this page's configuration
	 */
    private JPanel panel;

    private JLabel jLabel1;

    /**
	 * The Add MIDI Output button
	 */
    private JButton addMidiOutButton;

    /**
	 * The Speed label 
	 */
    private JLabel speedLabel;

    /**
	 * The Update Preferences button
	 */
    private JButton updatePrefsButton;

    /**
	 * The Speed text field
	 */
    private JTextField speedTF;

    /**
	 * The MIDI device to send to the MachineDrum on
	 */
    private Receiver recv;

    /**
	 * The name of the selected MIDI device
	 */
    private String midiDeviceName;

    /**
	 * morph_machines[machine_number] - 1 if machine_number machine should be sent random parameter changes
	 */
    private int[] morph_machines = new int[16];

    /**
	 * morph_params[param_number] - 1 if the param_number paramater should be sent random changes 
	 */
    private int[] morph_params = new int[24];

    /**
	 * fx_morph[fx_number] - 1 if the fx_number fx unit should be sent random parameter changes, [0] = echo, [1] = gate, [2] = eq, [3] = compressor
	 */
    private int[] fx_morph = new int[4];

    /**
	 * true randomly enables and disables morph_machines and morph_params
	 */
    private boolean auto_morph = false;

    /**
	 * Random number generator 
	 */
    private Random generator;

    /**
	 * Utility class for sending MIDI messages to the MachineDrum 
	 */
    private MachineDrum machine_drum;

    /**
	 * A counter for MIDI clock sync ticks 
	 */
    private int ticks;

    /**
	 * How often random param changes are sent. 
	 */
    private int speed = 100;

    /**
	 * The name of the page 
	 */
    private String pageName = "Machine Drum Interface";

    private JLabel pageNameLBL;

    /**
	 * @param monome The MonomeConfiguration this page belongs to
	 * @param index The index of this page (the page number)
	 */
    public MachineDrumInterfacePage(MonomeConfiguration monome, int index) {
        this.machine_drum = new MachineDrum();
        this.monome = monome;
        this.index = index;
        this.generator = new Random();
    }

    public String getName() {
        return pageName;
    }

    public void setName(String name) {
        this.pageName = name;
        this.pageNameLBL.setText("Page " + (this.index + 1) + ": " + pageName);
        this.monome.setJMenuBar(this.monome.createMenuBar());
    }

    public void handlePress(int x, int y, int value) {
        if (value == 1) {
            if (y < 2) {
                int machine_num = getMachineNum(x, y);
                if (morph_machines[machine_num] == 1) {
                    morph_machines[machine_num] = 0;
                    this.monome.led(x, y, 0, this.index);
                } else {
                    morph_machines[machine_num] = 1;
                    this.monome.led(x, y, 1, this.index);
                }
            } else if (y < 5) {
                int param_num = getMachineNum(x, y - 2);
                if (morph_params[param_num] == 1) {
                    morph_params[param_num] = 0;
                    this.monome.led(x, y, 0, this.index);
                } else {
                    morph_params[param_num] = 1;
                    this.monome.led(x, y, 1, this.index);
                }
            } else if (y == 5) {
                machine_drum.initKit(recv, x);
            } else if (y == 6) {
                System.out.println("kit function");
                if (x < 4) {
                    machine_drum.sendKitLoad(recv, x);
                } else {
                    machine_drum.sendKitSave(recv, x - 4);
                }
            } else if (y == 7) {
                if (x == 0) {
                    if (auto_morph == false) {
                        auto_morph = true;
                        this.monome.led(x, y, 1, this.index);
                    } else {
                        auto_morph = false;
                        this.monome.led(x, y, 0, this.index);
                    }
                } else if (x > 0 && x < 5) {
                    if (fx_morph[x - 1] == 0) {
                        fx_morph[x - 1] = 1;
                    } else {
                        fx_morph[x - 1] = 0;
                    }
                    this.monome.led(x, y, fx_morph[x - 1], this.index);
                }
            }
        }
    }

    /**
	 * Translate monome x/y to a MachineDrum machine number
	 * 
	 * @param x The x coordinate on the monome
	 * @param y The y coordinate on the monome
	 * @return The MachineDrum machine number
	 */
    public int getMachineNum(int x, int y) {
        return (y * 8) + x;
    }

    public void handleReset() {
        ticks = 0;
    }

    public void handleTick() {
        if (ticks == 6) {
            ticks = 0;
        }
        if (auto_morph == true && generator.nextInt(this.speed) == 1) {
            int machine_num = generator.nextInt(12) + 2;
            int param_num = generator.nextInt(24);
            int x_m = machine_num % 8;
            int y_m = machine_num / 8;
            int x_p = param_num % 8;
            int y_p = (param_num / 8) + 2;
            if (morph_machines[machine_num] == 1) {
                morph_machines[machine_num] = 0;
                this.monome.led(x_m, y_m, 0, this.index);
            } else {
                morph_machines[machine_num] = 1;
                this.monome.led(x_m, y_m, 1, this.index);
            }
            if (morph_params[param_num] == 1) {
                morph_params[param_num] = 0;
                this.monome.led(x_p, y_p, 0, this.index);
            } else {
                morph_params[param_num] = 1;
                this.monome.led(x_p, y_p, 1, this.index);
            }
        }
        if (fx_morph[0] == 1 && ticks == 0) {
            machine_drum.sendFxParam(recv, "echo", generator.nextInt(8), generator.nextInt(127));
        }
        if (fx_morph[1] == 1 && ticks == 1) {
            machine_drum.sendFxParam(recv, "gate", generator.nextInt(8), generator.nextInt(127));
        }
        if (fx_morph[2] == 1 && ticks == 2) {
            machine_drum.sendFxParam(recv, "eq", generator.nextInt(8), generator.nextInt(127));
        }
        if (fx_morph[3] == 1 && ticks == 3) {
            machine_drum.sendFxParam(recv, "compressor", generator.nextInt(8), generator.nextInt(127));
        }
        for (int x = 0; x < 16; x++) {
            if (ticks == 0 && (x > 2)) {
                continue;
            } else if (ticks == 1 && (x > 5 || x < 3)) {
                continue;
            } else if (ticks == 2 && (x > 8 || x < 6)) {
                continue;
            } else if (ticks == 3 && (x > 11 || x < 9)) {
                continue;
            } else if (ticks == 4 && (x > 14 || x < 12)) {
                continue;
            } else if (ticks == 5 && (x > 16 || x < 15)) {
                continue;
            }
            for (int y = 0; y < 24; y++) {
                if (morph_machines[x] == 1) {
                    if (morph_params[y] == 1) {
                        if (generator.nextInt(this.speed) == 1) {
                            machine_drum.sendRandomParamChange(recv, x, y);
                        }
                    }
                }
            }
        }
        ticks++;
    }

    public void redrawMonome() {
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                if (y < 2) {
                    int machine_num = getMachineNum(x, y);
                    if (morph_machines[machine_num] == 1) {
                        this.monome.led(x, y, 1, this.index);
                    } else {
                        this.monome.led(x, y, 0, this.index);
                    }
                } else if (y < 5) {
                    int param_num = getMachineNum(x, y - 2);
                    if (morph_params[param_num] == 1) {
                        this.monome.led(x, y, 1, this.index);
                    } else {
                        this.monome.led(x, y, 0, this.index);
                    }
                } else if (y == 7) {
                    if (x == 0) {
                        if (auto_morph == true) {
                            this.monome.led(x, y, 1, this.index);
                        } else {
                            this.monome.led(x, y, 0, this.index);
                        }
                    } else if (x > 0 && x < 5) {
                        this.monome.led(x, y, fx_morph[x - 1], this.index);
                    } else {
                        this.monome.led(x, y, 0, this.index);
                    }
                } else {
                    this.monome.led(x, y, 0, this.index);
                }
            }
        }
    }

    public void send(MidiMessage message, long timeStamp) {
        if (this.recv == null) {
            return;
        }
        ShortMessage shortMessage;
        if (message instanceof ShortMessage) {
            shortMessage = (ShortMessage) message;
            switch(shortMessage.getCommand()) {
                case 0xF0:
                    if (shortMessage.getChannel() == 0x08) {
                        this.recv.send(message, timeStamp);
                    }
                    if (shortMessage.getChannel() == 0x0A) {
                        this.recv.send(message, timeStamp);
                    }
                    if (shortMessage.getChannel() == 0x0C) {
                        this.recv.send(message, timeStamp);
                    }
                    break;
                default:
                    this.recv.send(message, timeStamp);
                    break;
            }
        }
    }

    public String toXml() {
        String xml = "";
        xml += "      <name>Machine Drum Interface</name>\n";
        xml += "      <pageName>" + this.pageName + "</pageName>\n";
        xml += "      <selectedmidioutport>" + StringEscapeUtils.escapeXml(this.midiDeviceName) + "</selectedmidioutport>\n";
        xml += "      <speed>" + this.speed + "</speed>\n";
        return xml;
    }

    public JPanel getPanel() {
        if (this.panel != null) {
            return this.panel;
        }
        JPanel panel = new JPanel();
        AnchorLayout panelLayout = new AnchorLayout();
        panel.setLayout(panelLayout);
        panel.setPreferredSize(new java.awt.Dimension(319, 127));
        panel.add(getAddMidiOutButton(), new AnchorConstraint(775, 963, 917, 521, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
        panel.add(getUpdatePrefsButton(), new AnchorConstraint(767, 487, 917, 20, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
        panel.add(getSpeedTF(), new AnchorConstraint(507, 340, 712, 236, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
        panel.add(getSpeedLabel(), new AnchorConstraint(531, 236, 673, 57, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
        this.getUpdatePrefsButton().addActionListener(this);
        this.getAddMidiOutButton().addActionListener(this);
        pageNameLBL = new JLabel("Page " + (this.index + 1) + ": Machine Drum Interface");
        panel.add(pageNameLBL, new AnchorConstraint(0, 600, 120, 0, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
        JLabel midiout = new JLabel("MIDI Out: " + this.midiDeviceName);
        panel.add(midiout, new AnchorConstraint(271, 948, 429, 20, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
        pageNameLBL.setPreferredSize(new java.awt.Dimension(296, 20));
        this.panel = panel;
        return panel;
    }

    /**
	 * @return The speed label
	 */
    private JLabel getSpeedLabel() {
        if (speedLabel == null) {
            speedLabel = new JLabel();
            speedLabel.setText("Speed");
            speedLabel.setPreferredSize(new java.awt.Dimension(57, 18));
        }
        return speedLabel;
    }

    /**
	 * @return The speed text field
	 */
    private JTextField getSpeedTF() {
        if (speedTF == null) {
            speedTF = new JTextField();
            speedTF.setText("100");
            speedTF.setPreferredSize(new java.awt.Dimension(33, 26));
        }
        return speedTF;
    }

    /**
	 * @return The Add MIDI Output button
	 */
    private JButton getAddMidiOutButton() {
        if (addMidiOutButton == null) {
            addMidiOutButton = new JButton();
            addMidiOutButton.setText("Set MIDI Output");
            addMidiOutButton.setPreferredSize(new java.awt.Dimension(141, 18));
        }
        return addMidiOutButton;
    }

    /**
	 * @return The Update Preferences button
	 */
    private JButton getUpdatePrefsButton() {
        if (updatePrefsButton == null) {
            updatePrefsButton = new JButton();
            updatePrefsButton.setText("Update Preferences");
            updatePrefsButton.setPreferredSize(new java.awt.Dimension(149, 19));
        }
        return updatePrefsButton;
    }

    /**
	 * @param speed Sets the speed to send random parameter changes or auto morph, lower is faster
	 */
    public void setSpeed(int speed) {
        this.speed = speed;
        this.getSpeedTF().setText(String.valueOf(speed));
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("Set MIDI Output")) {
            String[] midiOutOptions = this.monome.getMidiOutOptions();
            String deviceName = (String) JOptionPane.showInputDialog(this.monome, "Choose a MIDI Output to add", "Set MIDI Output", JOptionPane.PLAIN_MESSAGE, null, midiOutOptions, "");
            if (deviceName == null) {
                return;
            }
            this.addMidiOutDevice(deviceName);
        }
        if (e.getActionCommand().equals("Update Preferences")) {
            this.speed = Integer.parseInt(this.getSpeedTF().getText());
        }
    }

    public void addMidiOutDevice(String deviceName) {
        this.recv = this.monome.getMidiReceiver(deviceName);
        this.midiDeviceName = deviceName;
        this.getAddMidiOutButton().removeActionListener(this);
        this.getUpdatePrefsButton().removeActionListener(this);
        this.panel.removeAll();
        this.panel = null;
        this.monome.redrawPanel();
    }

    public boolean getCacheDisabled() {
        return false;
    }

    public void destroyPage() {
        return;
    }

    public void clearPanel() {
        this.panel = null;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public void handleADC(int adcNum, float value) {
    }

    public void handleADC(float x, float y) {
    }

    public boolean isTiltPage() {
        return false;
    }

    public ADCOptions getAdcOptions() {
        return null;
    }

    public void setAdcOptions(ADCOptions options) {
    }

    public void configure(Element pageElement) {
        NodeList nameNL = pageElement.getElementsByTagName("pageName");
        Element el = (Element) nameNL.item(0);
        if (el != null) {
            NodeList nl = el.getChildNodes();
            String name = ((Node) nl.item(0)).getNodeValue();
            this.setName(name);
        }
        NodeList rowNL = pageElement.getElementsByTagName("speed");
        el = (Element) rowNL.item(0);
        if (el != null) {
            NodeList nl = el.getChildNodes();
            String speed = ((Node) nl.item(0)).getNodeValue();
            this.setSpeed(Integer.parseInt(speed));
        }
    }
}
