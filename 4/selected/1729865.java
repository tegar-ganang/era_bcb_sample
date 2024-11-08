package midi;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ResourceBundle;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import com.jgoodies.forms.factories.DefaultComponentFactory;
import com.phidgets.PhidgetException;
import com.visutools.nav.bislider.BiSlider;
import de.humatic.mmj.MidiOutput;
import de.humatic.mmj.MidiSystem;
import de.humatic.mmj.MidiSystemListener;

public class MidiApp implements MidiSystemListener, KeyListener {

    final int N = 14;

    private JTextField TypetextField;

    private JTextField KeyPanel2textField;

    private JComboBox comboBox_2;

    private ButtonGroup buttonGroup_1 = new ButtonGroup();

    private JTextField TimePanel2textField_4;

    private JTabbedPane comboBox_1;

    private JTextField KeyPanel1textField_3;

    private JComboBox comboBox;

    private JTextField TimePanel1textField_2;

    private JTextField InputPorttextField_1;

    private JTextField SensorNametextField;

    private JFrame frame;

    private JCheckBox suspendCheckBox;

    private JCheckBox activeCheckBox_1;

    private JCheckBox releaseCheckBox;

    private JCheckBox latencyCheckBox;

    private Vector<JComponent> components;

    PhidgetController pc;

    private BiSlider sensitivity, sensitivity2, inputLevel;

    StringBuffer sb;

    String serverID = null, port = null, password = null;

    private LEDInfo li;

    /**
	 * Launch the application
	 * 
	 * @param args
	 */
    public static void main(String args[]) {
        EventQueue.invokeLater(new Runnable() {

            public void run() {
                try {
                    MidiApp window = new MidiApp();
                    window.frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    Piano p;

    ResourceBundle properties;

    JSlider slRate;

    LEDFrame lf;

    /**
	 * Create the application
	 * 
	 * @throws PhidgetException
	 */
    public MidiApp() throws PhidgetException {
        try {
            UIManager.setLookAndFeel("com.shfarr.ui.plaf.fh.FhLookAndFeel");
        } catch (Exception e) {
            e.printStackTrace();
        }
        properties = ResourceBundle.getBundle("blink");
        sb = new StringBuffer();
        components = new Vector<JComponent>();
        li = new LEDInfo();
        createConfig();
        createLatencySniffers();
        createPiano();
        createLogger();
        pc = new PhidgetController(this);
        pc.start();
        initializeModel();
        sensitivity = createSensitivity(30);
        sensitivity2 = createSensitivity(60);
        for (int i = 0; i < N; i++) {
            trigger[i] = false;
            release[i] = true;
            rangeMin[i] = 950;
            rangeMax[i] = 1000;
            for (int j = 0; j < 3; j++) {
                rangeModMin[i][j] = 950;
                rangeModMax[i][j] = 1000;
            }
        }
        createInputLevel();
        createContents();
        modulationRadioButton.doClick();
        loadSensorInfo(0);
        createMenu();
        if (properties.getString("default.file") != null && !properties.getString("default.file").trim().equals("")) {
            loadDefaultFile(properties.getString("default.file"));
        }
        font = activeCheckBox.getFont();
        sensitivity.setFont(font);
        sensitivity2.setFont(font);
        inputLevel.setFont(font);
        tc = new ThCallback(this);
        tc.start();
        AbstractAction[] tabChange;
        int i;
        tabChange = new AbstractAction[10];
        for (i = 0; i < 10; i++) {
            tabChange[i] = new KeyAction(this, i);
            frame.getRootPane().getActionMap().put("TabChange" + i, tabChange[i]);
        }
        InputMap im = frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_0, InputEvent.ALT_MASK), "TabChange0");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_1, InputEvent.ALT_MASK), "TabChange1");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_2, InputEvent.ALT_MASK), "TabChange2");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_3, InputEvent.ALT_MASK), "TabChange3");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_4, InputEvent.ALT_MASK), "TabChange4");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_5, InputEvent.ALT_MASK), "TabChange5");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_6, InputEvent.ALT_MASK), "TabChange6");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_7, InputEvent.ALT_MASK), "TabChange7");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_8, InputEvent.ALT_MASK), "TabChange8");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_9, InputEvent.ALT_MASK), "TabChange9");
        lf = new LEDFrame(this, li, pc);
        lf.setVisible(true);
    }

    private JComponent getRootPane() {
        return null;
    }

    Font font;

    ThCallback tc;

    JFrame config;

    GridLayout gbl;

    JLabel[] lbConfig;

    JTextField[] tfConfig;

    JButton btConfig;

    void createConfig() {
        String[] labels;
        int i;
        labels = new String[] { "ServerID", "Port", "Password", "Interface Kit Serial No", "RFID Serial No", "Linear Touch Serial No", "LED 64 Serial No", "Refresh Rate" };
        gbl = new GridLayout(labels.length + 1, 2);
        config = new JFrame();
        config.setResizable(true);
        config.getContentPane().setLayout(null);
        config.setBounds(850, 20, 200, 400);
        config.setLayout(gbl);
        config.setVisible(true);
        lbConfig = new JLabel[labels.length];
        tfConfig = new JTextField[labels.length];
        for (i = 0; i < labels.length; i++) {
            lbConfig[i] = new JLabel(labels[i]);
            config.add(lbConfig[i]);
            lbConfig[i].setVisible(true);
            if (i == labels.length - 1) {
                slRate = new JSlider();
                slRate.setMaximum(1000);
                slRate.setMinimum(10);
                slRate.setVisible(true);
                slRate.setValue(Integer.parseInt(properties.getString(labels[i].replace(' ', '.'))));
                slRate.setVisible(true);
                slRate.addChangeListener(new RefreshListener(this));
                config.add(slRate);
            } else {
                tfConfig[i] = new JTextField();
                tfConfig[i].setText(properties.getString(labels[i].replace(' ', '.')));
                config.add(tfConfig[i]);
                tfConfig[i].setVisible(true);
            }
        }
        btConfig = new JButton("Apply");
        btConfig.addMouseListener(new ApplyConfigListener(this));
        config.add(btConfig);
        config.pack();
    }

    ThLatencySniffer[][] tls;

    private void createLatencySniffers() {
        tls = new ThLatencySniffer[N][3];
    }

    JMenu jm;

    private void createMenu() {
        JMenuBar jmb;
        JMenu jmFile, jmView, jmHelp;
        String[] file, view, help;
        char[] shortFile, shortView, shortHelp;
        file = new String[] { "Load...", "Save", "Save As...", "Quit" };
        view = new String[] { "Keyboard", "Sensors", "LED", "Log", "Preferences..." };
        help = new String[] { "Interface", "FAQ", "Webpage", "Credits" };
        shortFile = new char[] { 'L', 'S', 'A', 'Q' };
        shortView = new char[] { 'K', 'R', 'D', 'G', 'P' };
        shortHelp = new char[] { 'I', 'F', 'W', 'C' };
        jmb = new JMenuBar();
        jm = new JMenu();
        jmFile = populateMenu("File", file, shortFile);
        jmView = populateMenu("View", view, shortView);
        jmHelp = populateMenu("Help", help, shortHelp);
        jmb.add(jmFile);
        jmb.add(jmView);
        jmb.add(jmHelp);
        frame.setJMenuBar(jmb);
    }

    private JMenu populateMenu(String name, String[] list, char[] shortList) {
        JMenu jm;
        int i;
        JMenuItem jmi;
        jm = new JMenu(name);
        for (i = 0; i < list.length; i++) {
            jmi = new JMenuItem(list[i], shortList[i]);
            jmi.setAccelerator(KeyStroke.getKeyStroke(shortList[i], Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(), false));
            jmi.addActionListener(new CommandListener(this, shortList[i]));
            jm.add(jmi);
        }
        return jm;
    }

    JFrame pianoFrame;

    Piano piano;

    private void createPiano() {
        pianoFrame = new JFrame();
        pianoFrame.setResizable(false);
        pianoFrame.getContentPane().setLayout(null);
        pianoFrame.setBounds(260, 620, 757, 151);
        final JScrollPane panel = new JScrollPane();
        panel.setBorder(new BevelBorder(BevelBorder.RAISED));
        panel.setLayout(null);
        panel.setBounds(0, 0, 800, 200);
        panel.setBackground(Color.WHITE);
        panel.setVisible(true);
        piano = new Piano(this);
        pianoFrame.getContentPane().add(panel);
        piano.setVisible(true);
        piano.setBounds(0, 0, 800, 200);
        panel.add(piano);
        pianoFrame.setVisible(true);
    }

    private JFrame logger;

    JTextArea logtext;

    JScrollPane panel;

    private void createLogger() {
        logger = new JFrame();
        logger.setResizable(true);
        logger.getContentPane().setLayout(null);
        logger.setBounds(0, 0, 400, 300);
        logtext = new JTextArea();
        logtext.setBounds(0, 0, 400, 300);
        logtext.setEditable(false);
        logtext.setText("Logger initialized");
        panel = new JScrollPane(logtext);
        panel.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        panel.setPreferredSize(new Dimension(250, 250));
        logger.getContentPane().setLayout(new BorderLayout());
        logger.getContentPane().add(panel, BorderLayout.CENTER);
        logtext.setVisible(true);
        panel.setVisible(true);
        logger.setVisible(true);
    }

    SensorInfo si[];

    private void initializeModel() {
        int i;
        si = new SensorInfo[14];
        for (i = 0; i < 14; i++) {
            si[i] = new SensorInfo(i);
        }
    }

    private BiSlider createSensitivity(int height) {
        BiSlider sensitivity;
        sensitivity = new BiSlider(BiSlider.HSB);
        sensitivity.setVisible(true);
        sensitivity.setMinimumValue(0);
        sensitivity.setMaximumValue(1000);
        sensitivity.setSegmentSize(50);
        sensitivity.setMinimumColor(new Color(0xE2, 0xE6, 0xD6));
        sensitivity.setMaximumColor(Color.RED);
        sensitivity.setPrecise(true);
        sensitivity.setBounds(10, height, 280, 40);
        sensitivity.setBackground(new Color(0xE2, 0xE6, 0xD6));
        sensitivity.setColoredValues(950, 1000);
        sensitivity.addMouseMotionListener(new RangeListener(this));
        sensitivity.setFont(font);
        return sensitivity;
    }

    private void createInputLevel() {
        inputLevel = new BiSlider(BiSlider.HSB);
        inputLevel.setVisible(true);
        inputLevel.setMinimumValue(0);
        inputLevel.setMaximumValue(1000);
        inputLevel.setSegmentSize(50);
        inputLevel.setMinimumColor(new Color(0xE2, 0xE6, 0xD6));
        inputLevel.setMaximumColor(Color.RED);
        inputLevel.setPrecise(true);
        inputLevel.setBounds(50, 110, 280, 40);
        inputLevel.setBackground(new Color(0xE2, 0xE6, 0xD6));
        inputLevel.setColoredValues(0, 0);
        inputLevel.setFont(font);
    }

    private JLabel sensorNameLabel;

    private JCheckBox triggerCheckBox;

    JLabel inoutValueLabel;

    JCheckBox invertCheckBox, invertModuleCheckBox;

    JCheckBox activeCheckBox;

    private JCheckBox radioCheckBox;

    private JSlider reactionSlider;

    /**
	 * Initialize the contents of the frame
	 */
    private void createContents() {
        frame = new JFrame();
        frame.setResizable(false);
        frame.getContentPane().setLayout(null);
        frame.setBounds(435, 20, 387, 585);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        InputPorttextField_1 = new JTextField();
        InputPorttextField_1.setBounds(338, 44, 32, 28);
        frame.getContentPane().add(InputPorttextField_1);
        InputPorttextField_1.setVisible(false);
        TypetextField = new JTextField();
        TypetextField.setBounds(335, 440, 40, 28);
        frame.getContentPane().add(TypetextField);
        TypetextField.setVisible(false);
        createInputPortComboBox();
        sensorNameLabel = DefaultComponentFactory.getInstance().createLabel("SENSOR NAME");
        sensorNameLabel.setBounds(40, 91, 90, 16);
        frame.getContentPane().add(sensorNameLabel);
        SensorNametextField = new JTextField();
        SensorNametextField.setBounds(136, 85, 111, 28);
        SensorNametextField.addKeyListener(new RFIDNameListener(this));
        frame.getContentPane().add(SensorNametextField);
        components.add(SensorNametextField);
        JLabel reactionLabel = DefaultComponentFactory.getInstance().createLabel("REACTION");
        reactionLabel.setBounds(40, 55, 90, 16);
        frame.getContentPane().add(reactionLabel);
        reactionSlider = new JSlider();
        reactionSlider.setMinimum(0);
        reactionSlider.setMaximum(1000);
        reactionSlider.setValue(10);
        reactionSlider.setBounds(100, 55, 150, 16);
        reactionSlider.setVisible(true);
        reactionSlider.addChangeListener(new ReactionListener(this));
        frame.getContentPane().add(reactionSlider);
        radioCheckBox = new JCheckBox();
        radioCheckBox.setHorizontalTextPosition(SwingConstants.LEFT);
        radioCheckBox.setHorizontalAlignment(SwingConstants.LEADING);
        radioCheckBox.setSelected(true);
        radioCheckBox.setText("RATIOMETRIC");
        radioCheckBox.setBounds(251, 55, 123, 16);
        radioCheckBox.addChangeListener(new RadiometricListener(this));
        frame.getContentPane().add(radioCheckBox);
        activeCheckBox = new JCheckBox();
        activeCheckBox.setHorizontalTextPosition(SwingConstants.LEFT);
        activeCheckBox.setHorizontalAlignment(SwingConstants.LEADING);
        activeCheckBox.setSelected(true);
        activeCheckBox.setText("ACTIVE");
        activeCheckBox.setBounds(251, 75, 123, 16);
        activeCheckBox.addChangeListener(new ActiveListener(this));
        frame.getContentPane().add(activeCheckBox);
        invertCheckBox = new JCheckBox();
        invertCheckBox.setHorizontalTextPosition(SwingConstants.LEFT);
        invertCheckBox.setHorizontalAlignment(SwingConstants.LEADING);
        invertCheckBox.setSelected(false);
        invertCheckBox.setText("INVERT");
        invertCheckBox.setBounds(251, 95, 123, 16);
        invertCheckBox.addChangeListener(new InvertListener(this));
        frame.getContentPane().add(invertCheckBox);
        components.add(invertCheckBox);
        releaseCheckBox = new JCheckBox();
        releaseCheckBox.setToolTipText("");
        releaseCheckBox.setSelected(true);
        releaseCheckBox.setHorizontalTextPosition(SwingConstants.LEFT);
        releaseCheckBox.setText("RELEASE");
        releaseCheckBox.setBounds(251, 155, 123, 23);
        releaseCheckBox.addChangeListener(new ReleaseListener(this));
        frame.getContentPane().add(releaseCheckBox);
        components.add(releaseCheckBox);
        inoutValueLabel = new JLabel();
        inoutValueLabel.setText("INPUT VALUE");
        inoutValueLabel.setBounds(150, 123, 90, 16);
        inoutValueLabel.setVisible(false);
        frame.getContentPane().add(inoutValueLabel);
        frame.getContentPane().add(inputLevel);
        components.add(inputLevel);
        final JPanel panel = createPanel1();
        final JPanel panel_1 = createPanel2();
        triggerCheckBox = new JCheckBox();
        triggerCheckBox.setSelected(true);
        triggerCheckBox.setHorizontalTextPosition(SwingConstants.LEFT);
        triggerCheckBox.setText("TRIGGER");
        triggerCheckBox.setBounds(35, 155, 129, 23);
        frame.getContentPane().add(triggerCheckBox);
        triggerCheckBox.addChangeListener(new ChangeListener() {

            public void stateChanged(final ChangeEvent e) {
                if (triggerCheckBox.isSelected()) {
                    panel.setEnabled(true);
                    panel_1.setEnabled(false);
                    panel.setVisible(true);
                    panel_1.setVisible(false);
                } else {
                    panel.setEnabled(false);
                    panel_1.setEnabled(true);
                    panel.setVisible(false);
                    panel_1.setVisible(true);
                }
            }
        });
        components.add(triggerCheckBox);
    }

    JLabel timemsLabel;

    private MidiOutput mo;

    private JPanel createPanel1() {
        final JPanel panel = new JPanel();
        panel.setBorder(new BevelBorder(BevelBorder.RAISED));
        panel.setLayout(null);
        panel.setBounds(40, 184, 296, 151);
        frame.getContentPane().add(panel);
        suspendCheckBox = new JCheckBox();
        suspendCheckBox.setSelected(true);
        suspendCheckBox.setHorizontalTextPosition(SwingConstants.LEFT);
        suspendCheckBox.setText("SUSPEND");
        suspendCheckBox.setBounds(0, 0, 129, 23);
        panel.add(suspendCheckBox);
        components.add(suspendCheckBox);
        timemsLabel = new JLabel();
        timemsLabel.setText("TIME (ms)");
        timemsLabel.setBounds(165, 5, 68, 16);
        panel.add(timemsLabel);
        TimePanel1textField_2 = new JTextField();
        TimePanel1textField_2.setBounds(238, -1, 58, 28);
        panel.add(TimePanel1textField_2);
        components.add(TimePanel1textField_2);
        panel.add(sensitivity);
        components.add(sensitivity);
        final JLabel channelLabel = new JLabel();
        channelLabel.setText("CHANNEL");
        channelLabel.setBounds(5, 75, 68, 16);
        panel.add(channelLabel);
        comboBox = new JComboBox();
        comboBox.setModel(new DefaultComboBoxModel(new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16" }));
        comboBox.setBounds(106, 75, 58, 27);
        panel.add(comboBox);
        components.add(comboBox);
        final JLabel keyLabel = new JLabel();
        keyLabel.setText("KEY");
        keyLabel.setBounds(5, 121, 68, 16);
        panel.add(keyLabel);
        KeyPanel1textField_3 = new JTextField();
        KeyPanel1textField_3.setBounds(106, 115, 100, 28);
        panel.add(KeyPanel1textField_3);
        components.add(KeyPanel1textField_3);
        MidiSystem.initMidiSystem("Phidget MIDI", "Phidget MIDI");
        for (int i = 0; i < MidiSystem.getInputs().length; i++) {
            log("input: " + MidiSystem.getInputs()[i]);
        }
        for (int i = 0; i < MidiSystem.getOutputs().length; i++) {
            log("output: " + MidiSystem.getOutputs()[i]);
        }
        MidiSystem.addSystemListener(this);
        if (mo != null) {
            mo.close();
            mo = null;
        }
        mo = openOutput(0);
        System.out.println(mo.getDeviceInfo());
        panel.setEnabled(true);
        return panel;
    }

    private MidiOutput openOutput(int index) {
        return de.humatic.mmj.MidiSystem.openMidiOutput(index);
    }

    int previousModuleIndex;

    JRadioButton pitchRadioButton;

    JRadioButton velocityRadioButton;

    JRadioButton modulationRadioButton;

    private JPanel createPanel2() {
        final JPanel panel_1 = new JPanel();
        panel_1.setBounds(40, 349, 296, 189);
        frame.getContentPane().add(panel_1);
        panel_1.setLayout(null);
        panel_1.setBorder(new BevelBorder(BevelBorder.RAISED));
        final JLabel moduleLabel = new JLabel();
        moduleLabel.setText("MODULE");
        moduleLabel.setBounds(10, 10, 68, 16);
        panel_1.add(moduleLabel);
        comboBox_1 = new JTabbedPane();
        String label[] = new String[] { "1", "2", "3" };
        for (int i = 0; i < label.length; i++) {
            JPanel panel = new JPanel();
            comboBox_1.addTab(label[i], panel);
        }
        previousModuleIndex = 0;
        comboBox_1.addChangeListener(new ModuleTabListener(this));
        comboBox_1.setBounds(60, 5, 150, 27);
        panel_1.add(comboBox_1);
        components.add(comboBox_1);
        activeCheckBox_1 = new JCheckBox();
        activeCheckBox_1.setToolTipText("");
        activeCheckBox_1.setSelected(true);
        activeCheckBox_1.setHorizontalTextPosition(SwingConstants.LEFT);
        activeCheckBox_1.setText("ACTIVE");
        activeCheckBox_1.setBounds(205, 5, 95, 10);
        panel_1.add(activeCheckBox_1);
        components.add(activeCheckBox_1);
        invertModuleCheckBox = new JCheckBox();
        invertModuleCheckBox.setHorizontalTextPosition(SwingConstants.LEFT);
        invertModuleCheckBox.setHorizontalAlignment(SwingConstants.LEADING);
        invertModuleCheckBox.setSelected(false);
        invertModuleCheckBox.setText("INVERT");
        invertModuleCheckBox.setBounds(205, 5, 95, 30);
        invertModuleCheckBox.addChangeListener(new InvertModuleListener(this));
        panel_1.add(invertModuleCheckBox);
        components.add(invertModuleCheckBox);
        latencyCheckBox = new JCheckBox();
        latencyCheckBox.setHorizontalTextPosition(SwingConstants.LEFT);
        latencyCheckBox.setSelected(false);
        latencyCheckBox.setText("LATENCY");
        latencyCheckBox.setBounds(5, 40, 129, 23);
        latencyCheckBox.addChangeListener(new LatencyListener(this));
        panel_1.add(latencyCheckBox);
        components.add(latencyCheckBox);
        final JLabel timemsLabel_1 = new JLabel();
        timemsLabel_1.setText("TIME (ms)");
        timemsLabel_1.setBounds(155, 45, 68, 16);
        panel_1.add(timemsLabel_1);
        TimePanel2textField_4 = new JTextField();
        TimePanel2textField_4.setBounds(229, 40, 59, 28);
        panel_1.add(TimePanel2textField_4);
        components.add(TimePanel2textField_4);
        panel_1.add(sensitivity2);
        final JLabel typeLabel = new JLabel();
        typeLabel.setText("TYPE");
        typeLabel.setBounds(10, 100, 68, 16);
        panel_1.add(typeLabel);
        pitchRadioButton = new JRadioButton();
        final JLabel keyLabel_1 = new JLabel();
        keyLabel_1.setText("KEY");
        keyLabel_1.setBounds(10, 163, 68, 16);
        pitchRadioButton.setSelected(true);
        buttonGroup_1.add(pitchRadioButton);
        pitchRadioButton.setText("PITCH");
        pitchRadioButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                TypetextField.setText(pitchRadioButton.getText());
                KeyPanel2textField.setVisible(false);
                keyLabel_1.setVisible(false);
            }
        });
        pitchRadioButton.setBounds(85, 95, 70, 23);
        panel_1.add(pitchRadioButton);
        components.add(pitchRadioButton);
        velocityRadioButton = new JRadioButton();
        buttonGroup_1.add(velocityRadioButton);
        velocityRadioButton.setText("VEL");
        velocityRadioButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                TypetextField.setText(velocityRadioButton.getText());
                KeyPanel2textField.setVisible(true);
                keyLabel_1.setVisible(true);
            }
        });
        velocityRadioButton.setBounds(155, 95, 70, 23);
        panel_1.add(velocityRadioButton);
        components.add(velocityRadioButton);
        modulationRadioButton = new JRadioButton();
        buttonGroup_1.add(modulationRadioButton);
        modulationRadioButton.setText("MOD");
        modulationRadioButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                TypetextField.setText(modulationRadioButton.getText());
                KeyPanel2textField.setVisible(false);
                keyLabel_1.setVisible(false);
            }
        });
        modulationRadioButton.setBounds(215, 95, 70, 23);
        panel_1.add(modulationRadioButton);
        components.add(modulationRadioButton);
        final JLabel channelLabel_1 = new JLabel();
        channelLabel_1.setText("CHANNEL");
        channelLabel_1.setBounds(10, 125, 68, 16);
        panel_1.add(channelLabel_1);
        comboBox_2 = new JComboBox();
        comboBox_2.setModel(new DefaultComboBoxModel(new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16" }));
        comboBox_2.setBounds(112, 124, 59, 27);
        panel_1.add(comboBox_2);
        components.add(comboBox_2);
        panel_1.add(keyLabel_1);
        KeyPanel2textField = new JTextField();
        KeyPanel2textField.setBounds(100, 157, 59, 28);
        KeyPanel2textField.setText("72");
        panel_1.add(KeyPanel2textField);
        components.add(KeyPanel2textField);
        panel_1.setVisible(false);
        return panel_1;
    }

    private JTabbedPane paneRFID;

    JTabbedPane pane;

    private void createInputPortComboBox() {
        pane = new JTabbedPane();
        pane.setBounds(10, 10, 350, 30);
        String label[] = new String[] { "0", "1", "2", "3", "4", "5", "6", "7", "R0", "L" };
        for (int i = 0; i < label.length; i++) {
            JPanel panel = new JPanel();
            pane.addTab(label[i], panel);
        }
        frame.getContentPane().add(pane);
        previousIndex = 0;
        pane.addChangeListener(new MainTabListener(this));
        paneRFID = new JTabbedPane();
        paneRFID.setBounds(10, 40, 350, 30);
        label = new String[] { "0", "1", "2", "3", "4", "5" };
        for (int i = 0; i < label.length; i++) {
            JPanel panel = new JPanel();
            paneRFID.addTab(label[i], panel);
        }
        paneRFID.setVisible(false);
        frame.getContentPane().add(paneRFID);
        paneRFID.addChangeListener(new RFIDTabListener(this));
    }

    public void activateSpecialTab(boolean bRFID, boolean bSlider) {
        paneRFID.setVisible(bRFID);
        if (bRFID) {
            if (!triggerCheckBox.isSelected()) {
                triggerCheckBox.setEnabled(true);
                triggerCheckBox.doClick();
            }
            sensorNameLabel.setText("RFID ID");
            invertCheckBox.setText("REDUNDANCY");
        } else {
            sensorNameLabel.setText("SENSOR NAME");
            invertCheckBox.setText("INVERT");
        }
        if (bSlider) {
            if (triggerCheckBox.isSelected()) {
                triggerCheckBox.setEnabled(true);
                triggerCheckBox.doClick();
            }
        }
        sensitivity.setVisible(!bRFID);
        inputLevel.setVisible(!bRFID);
        triggerCheckBox.setEnabled(!(bRFID || bSlider));
        radioCheckBox.setVisible(!bSlider && !bRFID);
        reactionSlider.setVisible(!bRFID);
    }

    int previousIndex;

    public synchronized void activateTab(int index) {
        if (index == 9) {
            index = 13;
        }
        saveSensorInfo(previousIndex);
        previousIndex = index;
        loadSensorInfo(index);
        changeLevel(index, values[index]);
    }

    private synchronized void loadSensorInfo(int index) {
        SensorInfo s;
        ContinousInfo ci;
        TriggerInfo ti;
        int i, min;
        System.out.println("Loading state of " + index + ", invert " + si[index].isInvert());
        s = si[index];
        SensorNametextField.setText(s.getName());
        if (radioCheckBox.isSelected() != s.isRadiometric()) {
            radioCheckBox.doClick();
        }
        reactionSlider.setValue(s.getReaction());
        if (activeCheckBox.isSelected() != s.isActive()) {
            activeCheckBox.doClick();
        }
        if (triggerCheckBox.isSelected() != s.isTrigger()) {
            triggerCheckBox.doClick();
        }
        if (invertCheckBox.isSelected() != s.isInvert()) {
            invertCheckBox.doClick();
        }
        ti = s.getTi();
        if (releaseCheckBox.isSelected() != ti.isRelease()) {
            releaseCheckBox.doClick();
        }
        comboBox.setSelectedIndex(ti.getChannel());
        sensitivity.setColoredValues(ti.getMin(), ti.getMax());
        KeyPanel1textField_3.setText(ti.getNote() + "");
        if (suspendCheckBox.isSelected() != ti.isSuspend()) {
            suspendCheckBox.doClick();
        }
        TimePanel1textField_2.setText(ti.getTime() + "");
        ci = s.getCi();
        comboBox_1.setSelectedIndex(ci.getModule());
        loadModuleInfo(ci.getModule());
        System.out.println("Check for active sensors in " + index + "... " + si[index].isActive());
        if (si[index].isActive() && index < 8) {
            min = 0;
            for (i = 0; i < 8 && min == 0; i++) {
                if (i != index && si[i].isActive()) {
                    min = min + 1;
                }
            }
            System.out.println("Sensors active: " + min);
            if (min == 0) {
                activeCheckBox.setEnabled(false);
            }
        }
    }

    private synchronized void saveSensorInfo(int index) {
        SensorInfo s;
        ContinousInfo ci;
        TriggerInfo ti;
        activeCheckBox.setEnabled(true);
        s = si[index];
        s.setName(SensorNametextField.getText());
        s.setActive(activeCheckBox.isSelected());
        s.setTrigger(triggerCheckBox.isSelected());
        s.setInvert(invertCheckBox.isSelected());
        s.setRadiometric(radioCheckBox.isSelected());
        s.setReaction(reactionSlider.getValue());
        ti = s.getTi();
        ti.setChannel(comboBox.getSelectedIndex());
        ti.setMax(sensitivity.getMaximumColoredValue());
        ti.setMin(sensitivity.getMinimumColoredValue());
        ti.setNote(Integer.parseInt(KeyPanel1textField_3.getText()));
        ti.setSuspend(suspendCheckBox.isSelected());
        ti.setRelease(releaseCheckBox.isSelected());
        try {
            ti.setTime(Integer.parseInt(TimePanel1textField_2.getText()));
        } catch (NumberFormatException nfe) {
            ti.setTime(0);
        }
        ci = s.getCi();
        ci.setModule(comboBox_1.getSelectedIndex());
        saveModuleInfo(comboBox_1.getSelectedIndex());
    }

    public void changeModule(int selectedIndex) {
        saveModuleInfo(previousModuleIndex);
        previousModuleIndex = selectedIndex;
        loadModuleInfo(selectedIndex);
    }

    private void saveModuleInfo(int index) {
        SensorInfo s;
        ModuleInfo mi;
        s = si[previousIndex];
        mi = s.getCi().getMi()[index];
        mi.setActive(activeCheckBox_1.isSelected());
        mi.setChannel(comboBox_2.getSelectedIndex());
        mi.setLatency(latencyCheckBox.isSelected());
        mi.setMax(sensitivity2.getMaximumColoredValue());
        mi.setMin(sensitivity2.getMinimumColoredValue());
        mi.setNote(Integer.parseInt(KeyPanel2textField.getText()));
        mi.setPitch(pitchRadioButton.isSelected());
        mi.setMod(modulationRadioButton.isSelected());
        mi.setVel(velocityRadioButton.isSelected());
        mi.setInvert(invertModuleCheckBox.isSelected());
        try {
            mi.setTime(Integer.parseInt(TimePanel2textField_4.getText()));
        } catch (Exception e) {
            mi.setTime(0);
        }
    }

    private void loadModuleInfo(int index) {
        SensorInfo s;
        ModuleInfo mi;
        s = si[previousIndex];
        mi = s.getCi().getMi()[index];
        if (invertModuleCheckBox.isSelected() != mi.isInvert()) {
            invertModuleCheckBox.doClick();
        }
        if (activeCheckBox_1.isSelected() != mi.isActive()) {
            activeCheckBox_1.doClick();
        }
        comboBox_2.setSelectedIndex(mi.getChannel());
        if (latencyCheckBox.isSelected() != mi.isLatency()) {
            latencyCheckBox.doClick();
        }
        sensitivity2.setColoredValues(mi.getMin(), mi.getMax());
        KeyPanel2textField.setText(mi.getNote() + "");
        if (mi.isPitch()) {
            pitchRadioButton.doClick();
        } else if (mi.isVel()) {
            velocityRadioButton.doClick();
        } else {
            modulationRadioButton.doClick();
        }
        TimePanel2textField_4.setText(mi.getTime() + "");
    }

    double rangeMin[] = new double[N];

    double rangeMax[] = new double[N];

    double rangeModMin[][] = new double[N][3];

    double rangeModMax[][] = new double[N][3];

    int values[] = new int[N];

    public void changeLevel(int index, int value) {
        if (index == previousIndex) {
            inputLevel.setColoredValues(0, value);
        }
        values[index] = value;
    }

    Set<String> tags = new TreeSet<String>();

    String[] names = new String[] { "0106935b97", "01068dc67f", "", "", "", "" };

    public void tagGained(String value) {
        tags.add(value);
        inoutValueLabel.setText(value);
        if (value.equals(SensorNametextField.getText())) {
            SensorNametextField.setBackground(Color.RED);
        } else {
            SensorNametextField.setBackground(Color.WHITE);
        }
    }

    public void tagLost(String value) {
        tags.remove(value);
        if (value.equals(SensorNametextField.getText())) {
            SensorNametextField.setBackground(Color.WHITE);
        }
    }

    int previousRFID = 0;

    public synchronized void updateRFID() {
        saveSensorInfo(8 + previousRFID);
        previousRFID = paneRFID.getSelectedIndex();
        loadSensorInfo(8 + previousRFID);
        SensorNametextField.setText(names[paneRFID.getSelectedIndex()]);
        pane.setTitleAt(8, "R" + previousRFID);
        if (tags.contains(names[paneRFID.getSelectedIndex()])) {
            SensorNametextField.setBackground(Color.RED);
        } else {
            SensorNametextField.setBackground(Color.WHITE);
        }
    }

    public void updateRFIDName() {
        if (previousIndex == 8) {
            names[paneRFID.getSelectedIndex()] = SensorNametextField.getText();
        }
    }

    public void systemChanged() {
    }

    boolean trigger[] = new boolean[N];

    boolean release[] = new boolean[N];

    private boolean[][] module = new boolean[N][3];

    public synchronized void checkSensor() {
        int note;
        saveSensorInfo(previousIndex);
        saveModuleInfo(previousModuleIndex);
        if (lf != null) {
            lf.checkSensor();
        }
        values[previousIndex] = (int) inputLevel.getMaximumColoredValue();
        for (int i = 0; i < 14; i++) {
            if ((i < 8 || i > 12) && si[i].isActive()) {
                if (rangeMin[i] <= values[i] && rangeMax[i] >= values[i]) {
                    if (!trigger[i]) {
                        System.out.println("Trigger! " + rangeMin[i] + " < " + values[i] + " < " + rangeMax[i]);
                        note = getActiveNote(i);
                        if (note > 0) {
                            mo.sendMidi(new byte[] { (byte) (144 + getChannel(i)), (byte) note, (byte) 127 }, de.humatic.mmj.MidiSystem.getHostTime());
                            delayMidi(new byte[] { (byte) (128 + getChannel(i)), (byte) note, (byte) 0 }, i);
                        }
                        trigger[i] = true;
                    }
                } else {
                    if (trigger[i]) {
                        if (release[i]) {
                            System.out.println("Release! " + rangeMin[i] + " < " + values[i] + " < " + rangeMax[i]);
                            note = getActiveNote(i);
                            if (note > 0) {
                                mo.sendMidi(new byte[] { (byte) (144 + getChannel(i)), (byte) note, (byte) 127 }, de.humatic.mmj.MidiSystem.getHostTime());
                                delayMidi(new byte[] { (byte) (128 + getChannel(i)), (byte) note, (byte) 0 }, i);
                            }
                        }
                        trigger[i] = false;
                    }
                }
                for (int j = 0; i != 8 && j < 3; j++) {
                    if (rangeModMin[i][j] <= values[i] && rangeModMax[i][j] >= values[i] && !si[i].isTrigger()) {
                        module[i][j] = true;
                        note = getActiveNote(i, j);
                        if (note > 0) {
                            if (si[i].getCi().getMi()[j].isPitch()) {
                                if (tls[i][j] != null) {
                                    note = (int) (16384 * tls[i][j].getEase());
                                }
                                if (si[i].getCi().getMi()[j].isInvert()) {
                                    note = 16384 - note;
                                }
                                System.out.println("Pitch = " + note);
                                mo.sendMidi(new byte[] { (byte) (224 + getChannel(i, j)), (byte) (note % 0x7F), (byte) (note / 128) }, de.humatic.mmj.MidiSystem.getHostTime());
                            } else if (si[i].getCi().getMi()[j].isVel()) {
                                int velocity;
                                velocity = getVelocity(i, j);
                                if (tls[i][j] != null) {
                                    velocity = (int) (127 * tls[i][j].getEase());
                                }
                                if (si[i].getCi().getMi()[j].isInvert()) {
                                    velocity = 127 - velocity;
                                }
                                System.out.println("Velocity = " + getVelocity(i, j));
                                mo.sendMidi(new byte[] { (byte) (144 + getChannel(i, j)), (byte) note, (byte) velocity }, de.humatic.mmj.MidiSystem.getHostTime());
                            } else if (si[i].getCi().getMi()[j].isMod()) {
                                if (tls[i][j] != null) {
                                    note = (int) (127 * tls[i][j].getEase());
                                }
                                if (si[i].getCi().getMi()[j].isInvert()) {
                                    note = 127 - note;
                                }
                                mo.sendMidi(new byte[] { (byte) (176 + getChannel(i, j)), (byte) 1, (byte) (note) }, de.humatic.mmj.MidiSystem.getHostTime());
                            }
                        }
                    } else if (module[i][j] == true && (rangeModMin[i][j] >= values[i] || rangeModMax[i][j] < values[i]) && !si[i].isTrigger()) {
                        int value;
                        if (values[i] < rangeModMin[i][j]) {
                            value = 0;
                        } else {
                            value = 1;
                        }
                        if (si[i].getCi().getMi()[j].isPitch()) {
                            note = 16384 * value;
                            System.out.println("Pitch = " + note);
                            if (si[i].getCi().getMi()[j].isInvert()) {
                                note = 16384 - note;
                            }
                            mo.sendMidi(new byte[] { (byte) (224 + getChannel(i, j)), (byte) (note % 0x7F), (byte) (note / 128) }, de.humatic.mmj.MidiSystem.getHostTime());
                        } else if (si[i].getCi().getMi()[j].isVel()) {
                            int velocity;
                            velocity = 127 * value;
                            note = getActiveNote(i, j);
                            if (si[i].getCi().getMi()[j].isInvert()) {
                                velocity = 127 - velocity;
                            }
                            mo.sendMidi(new byte[] { (byte) (144 + getChannel(i, j)), (byte) note, (byte) velocity }, de.humatic.mmj.MidiSystem.getHostTime());
                        } else if (si[i].getCi().getMi()[j].isMod()) {
                            note = 127 * value;
                            if (si[i].getCi().getMi()[j].isInvert()) {
                                note = 127 - note;
                            }
                            mo.sendMidi(new byte[] { (byte) (176 + getChannel(i, j)), (byte) 1, (byte) (note) }, de.humatic.mmj.MidiSystem.getHostTime());
                        }
                    }
                    module[i][j] = false;
                }
            }
        }
        for (int i = 0; i < 6; i++) {
            if (!names[i].equals("")) {
                if (tags.contains(names[i])) {
                    if (!trigger[8 + i]) {
                        if (!lastTag[0].equals(names[i])) {
                            note = getActiveNote(8 + i);
                            if (note > 0) {
                                mo.sendMidi(new byte[] { (byte) (144 + getChannel(8 + i)), (byte) note, (byte) 127 }, de.humatic.mmj.MidiSystem.getHostTime());
                                delayMidi(new byte[] { (byte) (128 + getChannel(8 + i)), (byte) note, (byte) 0 }, 8 + i);
                            }
                        } else {
                            if (si[8 + i].isInvert()) {
                                note = getActiveNote(8 + i);
                                if (note > 0) {
                                    mo.sendMidi(new byte[] { (byte) (144 + getChannel(8 + i)), (byte) note, (byte) 127 }, de.humatic.mmj.MidiSystem.getHostTime());
                                    delayMidi(new byte[] { (byte) (128 + getChannel(8 + i)), (byte) note, (byte) 0 }, 8 + i);
                                }
                            }
                        }
                        lastTag[0] = names[i];
                        trigger[8 + i] = true;
                    }
                } else {
                    if (trigger[8 + i]) {
                        if (release[8 + i]) {
                            if (!lastReleaseTag[0].equals(names[i])) {
                                note = getActiveNote(8 + i);
                                if (note > 0) {
                                    mo.sendMidi(new byte[] { (byte) (144 + getChannel(8 + i)), (byte) note, (byte) 127 }, de.humatic.mmj.MidiSystem.getHostTime());
                                    delayMidi(new byte[] { (byte) (128 + getChannel(8 + i)), (byte) note, (byte) 0 }, 8 + i);
                                }
                            } else {
                                if (si[8 + i].isInvert()) {
                                    note = getActiveNote(8 + i);
                                    if (note > 0) {
                                        mo.sendMidi(new byte[] { (byte) (144 + getChannel(8 + i)), (byte) note, (byte) 127 }, de.humatic.mmj.MidiSystem.getHostTime());
                                        delayMidi(new byte[] { (byte) (128 + getChannel(8 + i)), (byte) note, (byte) 0 }, 8 + i);
                                    }
                                }
                            }
                        }
                        lastReleaseTag[0] = names[i];
                        trigger[8 + i] = false;
                    }
                }
            }
        }
    }

    String lastTag[] = new String[] { "", "", "", "", "", "" };

    String lastReleaseTag[] = new String[] { "", "", "", "", "", "" };

    private int getChannel(int sensor) {
        int channel;
        channel = si[sensor].getTi().getChannel();
        return channel;
    }

    private int getChannel(int sensor, int mod) {
        int channel;
        channel = si[sensor].getCi().getMi()[mod].getChannel();
        return channel;
    }

    private int getVelocity(int sensor, int mod) {
        return (int) (((values[sensor] - rangeModMin[sensor][mod]) / (rangeModMax[sensor][mod] - rangeModMin[sensor][mod])) * 127);
    }

    private void delayMidi(byte[] bs, int i) {
        int time;
        ThMidiDelay md;
        if (previousIndex == i || i == (previousRFID + 8)) {
            if (suspendCheckBox.isSelected()) {
                time = Integer.parseInt(TimePanel1textField_2.getText());
            } else {
                time = 100;
            }
        } else {
            if (si[i].getTi().isSuspend()) {
                time = si[i].getTi().getTime();
            } else {
                time = 100;
            }
        }
        md = new ThMidiDelay(mo, time, bs);
        md.start();
    }

    private int getActiveNote(int sensor, int mod) {
        int note;
        note = 0;
        if (!si[sensor].isTrigger()) {
            if (si[sensor].getCi().getMi()[mod].isPitch()) {
                note = getPitch(sensor, mod);
            } else if (si[sensor].getCi().getMi()[mod].isVel()) {
                note = si[sensor].getCi().getMi()[mod].getNote();
            } else {
                note = getModulation(sensor, mod);
            }
        }
        return note;
    }

    private int getModulation(int sensor, int mod) {
        double value;
        int modulation;
        value = values[sensor];
        modulation = formulaModulation(si[sensor].getCi().getMi()[mod].getMax(), si[sensor].getCi().getMi()[mod].getMin(), value);
        return modulation;
    }

    private int getModulation() {
        return formulaModulation(sensitivity2.getMaximumColoredValue(), sensitivity2.getMinimumColoredValue(), inputLevel.getMaximumColoredValue());
    }

    private int formulaModulation(double max, double min, double value) {
        return (int) (((value - min) / (max - min)) * 127);
    }

    private int getPitch(int sensor, int mod) {
        double value;
        int pitch;
        value = values[sensor];
        pitch = formulaPitch(si[sensor].getCi().getMi()[mod].getMax(), si[sensor].getCi().getMi()[mod].getMin(), value);
        return pitch;
    }

    private int formulaPitch(double max, double min, double value) {
        return (int) (((value - min) / (max - min)) * 16384);
    }

    private int getPitch() {
        return formulaPitch(sensitivity2.getMaximumColoredValue(), sensitivity2.getMinimumColoredValue(), inputLevel.getMaximumColoredValue());
    }

    private int getActiveNote(int sensor) {
        int note;
        note = 0;
        if (si[sensor].isTrigger()) {
            note = si[sensor].getTi().getNote();
        }
        return note;
    }

    public void log(String msg) {
        updateLogger(msg);
    }

    private void updateLogger(String msg) {
    }

    public void raiseException(PhidgetException e) {
        log(e);
    }

    public void log(Exception e) {
        updateLogger(e.getMessage());
    }

    public void sendMidi(byte[] bs, long hostTime) {
        if (bs[1] > 0) {
            if (triggerCheckBox.isSelected()) {
                KeyPanel1textField_3.setText(bs[1] + "");
            } else {
                KeyPanel2textField.setText(bs[1] + "");
            }
        }
    }

    public void updateRanges() {
        if (triggerCheckBox.isSelected()) {
            System.out.println("Updating ranges for " + previousIndex);
            rangeMin[previousIndex] = sensitivity.getMinimumColoredValue();
            rangeMax[previousIndex] = sensitivity.getMaximumColoredValue();
        } else {
            System.out.println("Updating ranges for " + previousIndex + ", " + previousModuleIndex);
            rangeModMin[previousIndex][previousModuleIndex] = sensitivity2.getMinimumColoredValue();
            rangeModMax[previousIndex][previousModuleIndex] = sensitivity2.getMaximumColoredValue();
        }
    }

    public void updateRelease() {
        if (previousIndex < 8 || previousIndex == 13) {
            release[previousIndex] = releaseCheckBox.isSelected();
        } else {
            release[8 + previousRFID] = releaseCheckBox.isSelected();
        }
    }

    public void checkInvert() {
        System.out.println("Modifying " + previousIndex + " with value " + invertCheckBox.isSelected());
        if (previousIndex < 8 || previousIndex == 13) {
            si[previousIndex].setInvert(invertCheckBox.isSelected());
        } else {
            si[8 + previousRFID].setInvert(invertCheckBox.isSelected());
        }
    }

    public void checkModuleInvert() {
        if (previousIndex < 8 || previousIndex == 13) {
            si[previousIndex].getCi().getMi()[previousModuleIndex].setInvert(invertModuleCheckBox.isSelected());
        } else {
            si[8 + previousRFID].setInvert(invertCheckBox.isSelected());
        }
    }

    public void checkActive() {
        if (previousIndex < 8 || previousIndex == 13) {
            si[previousIndex].setActive(activeCheckBox.isSelected());
        } else {
            si[8 + previousRFID].setActive(activeCheckBox.isSelected());
        }
        activate(activeCheckBox.isSelected());
    }

    private void activate(boolean b) {
        Iterator<JComponent> it;
        it = components.iterator();
        while (it.hasNext()) {
            it.next().setEnabled(b);
        }
    }

    public void checkLatency() {
        System.out.println("Latency is " + latencyCheckBox.isSelected());
        if (latencyCheckBox.isSelected()) {
            if (tls[previousIndex][previousModuleIndex] == null) {
                tls[previousIndex][previousModuleIndex] = new ThLatencySniffer(previousIndex, previousModuleIndex, this);
                tls[previousIndex][previousModuleIndex].start();
            }
        } else {
            if (tls[previousIndex][previousModuleIndex] != null) {
                tls[previousIndex][previousModuleIndex].shutdown();
                tls[previousIndex][previousModuleIndex] = null;
            }
        }
    }

    public int getLatency(int sensor, int module) {
        int latency;
        latency = si[sensor].getCi().getMi()[module].getTime();
        if (latency == 0) {
            latency = 100;
        }
        return latency;
    }

    public double read(int sensor, int module) {
        double value, min, max;
        value = values[sensor];
        if (previousIndex == sensor && previousModuleIndex == module) {
            min = sensitivity2.getMinimumColoredValue();
            max = sensitivity2.getMaximumColoredValue();
        } else {
            min = si[sensor].getCi().getMi()[module].getMin();
            max = si[sensor].getCi().getMi()[module].getMax();
        }
        return (double) ((value - min) / (max - min));
    }

    public double getNormalizedPosition(double oldBase, double formula, double difference, int sensor, int module, double oldObs, double base) {
        double min, max, norm;
        if (previousIndex == sensor && previousModuleIndex == module) {
            min = sensitivity2.getMinimumColoredValue();
            max = sensitivity2.getMaximumColoredValue();
        } else {
            min = si[sensor].getCi().getMi()[module].getMin();
            max = si[sensor].getCi().getMi()[module].getMax();
        }
        difference = base - oldBase;
        oldObs = 1;
        norm = Math.abs(oldBase + formula * difference);
        System.out.println("old value: " + oldBase + ", new value: " + base + ", difference: " + difference + ", norm: " + norm);
        return norm;
    }

    public void changeLinear(int index, int value) {
        if (previousIndex == 13) {
            inputLevel.setColoredValues(0, value);
        }
        values[13] = value;
    }

    public void updateRefresh() {
        tc.setRate(slRate.getValue());
    }

    public void executeCommand(char c) {
        switch(c) {
            case 'A':
                saveFileAs();
                break;
            case 'S':
                saveFile();
                break;
            case 'L':
                loadFile();
                break;
        }
    }

    String loaded = null;

    private synchronized void saveFile() {
        File f;
        FileOutputStream fos;
        ObjectOutputStream oos;
        if (loaded == null) {
            saveFileAs();
        } else {
            saveSensorInfo(previousIndex);
            saveModuleInfo(previousModuleIndex);
            f = new File(loaded);
            try {
                fos = new FileOutputStream(f);
                oos = new ObjectOutputStream(fos);
                oos.writeObject(si);
                oos.writeObject(serverID);
                oos.writeObject(password);
                oos.close();
                fos.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                log(e);
            } catch (IOException e) {
                e.printStackTrace();
                log(e);
            }
        }
    }

    private synchronized void loadFile() {
        File f;
        FileInputStream fis;
        ObjectInputStream ois;
        FileDialog fd = new FileDialog(frame, "Load...", FileDialog.LOAD);
        fd.setDirectory(System.getProperty("user.home"));
        String[] filterExt = { "*.txt", "*.doc", ".rtf", "*.*" };
        fd.setVisible(true);
        String selected = fd.getDirectory() + fd.getFile();
        System.out.println(selected);
        f = new File(selected);
        try {
            fis = new FileInputStream(f);
            ois = new ObjectInputStream(fis);
            si = (SensorInfo[]) ois.readObject();
            serverID = (String) ois.readObject();
            password = (String) ois.readObject();
            ois.close();
            fis.close();
            tfConfig[0].setText(serverID);
            tfConfig[2].setText(password);
            for (int i = 0; i < N; i++) {
                trigger[i] = false;
                release[i] = true;
                rangeMin[i] = si[i].getTi().getMin();
                rangeMax[i] = si[i].getTi().getMax();
                for (int j = 0; j < 3; j++) {
                    rangeModMin[i][j] = si[i].getCi().getMi()[j].getMin();
                    rangeModMax[i][j] = si[i].getCi().getMi()[j].getMax();
                }
            }
            loadModuleInfo(previousModuleIndex);
            loadSensorInfo(previousIndex);
            loaded = selected;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            log(e);
        } catch (IOException e) {
            e.printStackTrace();
            log(e);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            log(e);
        }
    }

    private synchronized void saveFileAs() {
        File f;
        FileOutputStream fos;
        ObjectOutputStream oos;
        saveSensorInfo(previousIndex);
        saveModuleInfo(previousModuleIndex);
        FileDialog fd = new FileDialog(frame, "Save As...", FileDialog.SAVE);
        fd.setDirectory(System.getProperty("user.home"));
        String[] filterExt = { "*.txt", "*.doc", ".rtf", "*.*" };
        fd.setVisible(true);
        String selected = fd.getDirectory() + fd.getFile();
        System.out.println(selected);
        f = new File(selected);
        try {
            fos = new FileOutputStream(f);
            oos = new ObjectOutputStream(fos);
            oos.writeObject(si);
            oos.writeObject(serverID);
            oos.writeObject(password);
            oos.close();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            log(e);
        } catch (IOException e) {
            e.printStackTrace();
            log(e);
        }
    }

    private synchronized void loadDefaultFile(String name) {
        File f;
        FileInputStream fis;
        ObjectInputStream ois;
        String selected = name;
        System.out.println(selected);
        f = new File(selected);
        try {
            fis = new FileInputStream(f);
            ois = new ObjectInputStream(fis);
            si = (SensorInfo[]) ois.readObject();
            serverID = (String) ois.readObject();
            password = (String) ois.readObject();
            ois.close();
            fis.close();
            tfConfig[0].setText(serverID);
            tfConfig[2].setText(password);
            loadModuleInfo(previousModuleIndex);
            loadSensorInfo(previousIndex);
            loaded = selected;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            log(e);
        } catch (IOException e) {
            e.printStackTrace();
            log(e);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            log(e);
        }
    }

    public void updateRadiometric() {
        si[previousIndex].setRadiometric(radioCheckBox.isSelected());
        pc.updateRadiometric(previousIndex, radioCheckBox.isSelected());
    }

    public void updateReaction() {
        si[previousIndex].setReaction(reactionSlider.getValue());
        try {
            pc.updateReaction(previousIndex, reactionSlider.getValue());
        } catch (PhidgetException e) {
            e.printStackTrace();
        }
    }

    public void keyPressed(KeyEvent e) {
    }

    public void keyReleased(KeyEvent e) {
    }

    public void keyTyped(KeyEvent e) {
        System.out.println(e.getKeyChar());
    }

    public void tabChange(int counter) {
        pane.setSelectedIndex(counter);
        activateTab(counter);
    }

    public String getServerID() {
        if (serverID == null) {
            return properties.getString("ServerID");
        } else {
            return serverID;
        }
    }

    public String getPort() {
        if (port == null) {
            return properties.getString("Port");
        } else {
            return port;
        }
    }

    public String getPassword() {
        if (password == null) {
            return properties.getString("Password");
        } else {
            return password;
        }
    }

    public void updateConfig() {
        serverID = tfConfig[0].getText();
        port = tfConfig[1].getText();
        password = tfConfig[2].getText();
        pc.setSerialLP(tfConfig[6].getText());
    }

    public int getValue(int i) {
        return values[i];
    }

    public double getRefreshRate() {
        return slRate.getValue();
    }
}
