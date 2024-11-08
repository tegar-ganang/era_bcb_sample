package rotorsim.controller;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JToggleButton;
import org.lwjgl.LWJGLException;
import rotorsim.controller.RSController.HeliControl;

public class TXCalibrationDialog extends JDialog implements ActionListener {

    private static final Color[] CHANNEL_COLOURS = new Color[] { new Color(212, 0, 0), new Color(255, 102, 0), new Color(255, 204, 0), new Color(21, 128, 0), new Color(0, 85, 212), new Color(102, 0, 255) };

    private List<RSController> controllers;

    private int curController;

    private TXView txView;

    private JPanel settingsPanel;

    private JPanel channels;

    private JPanel heliControls;

    private JComboBox joystickSelect;

    private JComboBox modeSelect;

    private JToggleButton calibrate;

    private JoystickChannelPanel[] joystickPanels = new JoystickChannelPanel[6];

    private HeliControlPanel[] heliPanels = new HeliControlPanel[4];

    private JPanel joysticks = new JPanel();

    public TXCalibrationDialog(List<RSController> controllers) {
        this.controllers = controllers;
        joystickSelect = new JComboBox();
        for (RSController controller : controllers) {
            joystickSelect.addItem(controller.getName());
        }
        curController = 0;
        this.setLayout(new BorderLayout());
        try {
            txView = new TXView();
            txView.setSize(320, 240);
            txView.setController(controllers.get(curController));
            add(txView, BorderLayout.CENTER);
        } catch (LWJGLException e) {
            e.printStackTrace();
        }
        settingsPanel = new JPanel();
        settingsPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 32, 16));
        heliControls = new JPanel();
        joysticks = new JPanel();
        joysticks.setLayout(new BorderLayout());
        channels = new JPanel();
        channels.setLayout(new GridLayout(6, 1));
        calibrate = new JToggleButton("Calibrate");
        calibrate.addActionListener(this);
        for (int i = 0; i < 6; i++) {
            joystickPanels[i] = new JoystickChannelPanel("Channel: " + (i + 1), i);
            channels.add(joystickPanels[i]);
        }
        joysticks.add(joystickSelect, BorderLayout.NORTH);
        joysticks.add(channels, BorderLayout.CENTER);
        joysticks.add(calibrate, BorderLayout.SOUTH);
        JPanel txControls = new JPanel();
        txControls.setLayout(new BorderLayout());
        heliControls.setLayout(new GridLayout(4, 1));
        modeSelect = new JComboBox();
        for (TXView.TXMode mode : TXView.TXMode.values()) {
            modeSelect.addItem(mode);
        }
        int panel = 0;
        txControls.add(modeSelect, BorderLayout.NORTH);
        for (HeliControl control : HeliControl.values()) {
            heliPanels[panel] = new HeliControlPanel(control, panel);
            heliControls.add(heliPanels[panel++]);
        }
        txControls.add(heliControls, BorderLayout.CENTER);
        settingsPanel.add(joysticks);
        settingsPanel.add(txControls);
        add(settingsPanel, BorderLayout.SOUTH);
        settingsPanel.doLayout();
        controllerSelected(0);
    }

    public void channelSelected(int selectedChannel) {
        RSController controller = controllers.get(curController);
        for (int i = 0; i < joystickPanels.length; i++) {
            if (i != selectedChannel) {
                joystickPanels[i].setSelected(false);
            }
        }
        mapControlPair();
    }

    public void controlSelected(int control) {
        for (int i = 0; i < heliPanels.length; i++) {
            if (control != i) {
                heliPanels[i].setSelected(false);
            }
        }
        mapControlPair();
    }

    public void mapControlPair() {
        int channel = getSelectedChannel();
        int control = getSelectedControl();
        if (channel != -1 && control != -1) {
            controllers.get(curController).mapControl(channel, heliPanels[control].getControl());
            heliPanels[control].setSelected(false);
            heliPanels[control].setMappedChannel(channel);
            joystickPanels[channel].setSelected(false);
        }
    }

    public int getSelectedControl() {
        for (int i = 0; i < heliPanels.length; i++) {
            if (heliPanels[i].isSelected()) {
                return i;
            }
        }
        return -1;
    }

    public int getSelectedChannel() {
        for (int i = 0; i < joystickPanels.length; i++) {
            if (joystickPanels[i].isSelected()) {
                return i;
            }
        }
        return -1;
    }

    public RSController getCurrentController() {
        return controllers.get(curController);
    }

    public void updateController() {
        RSController controller = getCurrentController();
        for (int i = 0; i < controller.getChannelCount(); i++) {
            joystickPanels[i].setValue(controller.getChannelValue(i, calibrate.isSelected()));
        }
        for (HeliControlPanel heliPanel : heliPanels) {
            heliPanel.setValue(controller.getControlValue(heliPanel.getControl()));
        }
        txView.updateControls();
        txView.repaint();
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == calibrate && calibrate.isSelected()) {
            controllers.get(curController).clearCalibration();
        }
    }

    public void controllerSelected(int index) {
        curController = index;
        RSController controller = getCurrentController();
        for (int i = 0; i < joystickPanels.length; i++) {
            if (i >= controller.getChannelCount()) {
                joystickPanels[i].setVisible(false);
            } else {
                joystickPanels[i].setVisible(true);
                joystickPanels[i].setReversed(controller.isReversed(i));
            }
        }
        for (int i = 0; i < heliPanels.length; i++) {
            int channel = controller.getAxisForControl(heliPanels[i].getControl());
            if (channel >= 0) {
                heliPanels[i].setMappedChannel(channel);
            }
        }
    }

    private class JoystickChannelPanel extends JPanel implements ActionListener {

        private JProgressBar channelValue;

        private JLabel channelName;

        private JToggleButton mapButton;

        private JCheckBox reversed;

        private int index;

        public JoystickChannelPanel(String name, int index) {
            mapButton = new JToggleButton("Map");
            mapButton.addActionListener(this);
            setLayout(new FlowLayout());
            channelValue = new JProgressBar();
            channelValue.setStringPainted(false);
            channelValue.setForeground(CHANNEL_COLOURS[index]);
            channelName = new JLabel(name);
            reversed = new JCheckBox("Reverse");
            reversed.addActionListener(this);
            add(channelName);
            add(channelValue);
            add(reversed);
            add(mapButton);
            this.index = index;
        }

        public void setReversed(boolean reversed) {
            this.reversed.setSelected(reversed);
        }

        public boolean isReversed() {
            return reversed.isSelected();
        }

        public boolean isSelected() {
            return mapButton.isSelected();
        }

        public void setSelected(boolean selected) {
            mapButton.setSelected(selected);
        }

        public void setValue(float value) {
            channelValue.setValue((int) (value * 100));
        }

        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == mapButton) {
                channelSelected(mapButton.isSelected() ? index : -1);
            } else if (e.getSource() == reversed) {
                controllers.get(curController).setReversed(index, isReversed());
            }
        }
    }

    private class HeliControlPanel extends JPanel implements ActionListener {

        private JLabel img;

        private JProgressBar value;

        private JToggleButton mapButton;

        private HeliControl control;

        private int index;

        private int channel;

        public HeliControlPanel(HeliControl control, int index) {
            this.index = index;
            setLayout(new FlowLayout());
            img = new JLabel();
            mapButton = new JToggleButton("Map");
            mapButton.addActionListener(this);
            this.control = control;
            channel = -1;
            value = new JProgressBar();
            value.setStringPainted(false);
            value.setForeground(Color.LIGHT_GRAY);
            try {
                switch(control) {
                    case CYCLIC_PITCH:
                        img.setIcon(new ImageIcon(ImageIO.read(getClass().getClassLoader().getResourceAsStream("images/control_pitch.png"))));
                        break;
                    case CYCLIC_ROLL:
                        img.setIcon(new ImageIcon(ImageIO.read(getClass().getClassLoader().getResourceAsStream("images/control_roll.png"))));
                        break;
                    case COLLECTIVE:
                        img.setIcon(new ImageIcon(ImageIO.read(getClass().getClassLoader().getResourceAsStream("images/control_collective.png"))));
                        break;
                    case TAIL:
                        img.setIcon(new ImageIcon(ImageIO.read(getClass().getClassLoader().getResourceAsStream("images/control_yaw.png"))));
                        break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            add(mapButton);
            add(img);
            add(value);
        }

        public HeliControl getControl() {
            return control;
        }

        public void setMappedChannel(int channel) {
            this.channel = channel;
            value.setForeground(CHANNEL_COLOURS[channel]);
        }

        public void setValue(float value) {
            this.value.setValue((int) (value * 100));
        }

        public void setSelected(boolean selected) {
            mapButton.setSelected(selected);
        }

        public boolean isSelected() {
            return mapButton.isSelected();
        }

        public void actionPerformed(ActionEvent e) {
            controlSelected(index);
        }
    }
}
