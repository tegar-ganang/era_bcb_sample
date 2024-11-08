package hinton.control;

import cholsey.Net;
import hinton.ambassador.AmbassadorInterface;
import hinton.ambassador.RobCom;
import hinton.ambassador.RobComBroker;
import hinton.ambassador.RobotStruct;
import hinton.ambassador.SimComBroker;
import hinton.analyser.Analyser;
import hinton.broker.EvoComInterface;
import hinton.executive.FitnessFunctionBroker;
import hinton.executive.FitnessFunctionInterface;
import hinton.executive.Processor;
import hinton.executive.ProcessorThread;
import hinton.executive.ProcessParameter;
import hinton.gui.LastNetDialog;
import hinton.gui.LoadNetMode;
import hinton.gui.ParameterDialog;
import hinton.gui.ProcessParameterDialog;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import util.io.EvolutionSaxHandlerThread;
import util.io.EvolutionSaxInterface;
import util.io.ExFileFilter;
import util.io.XMLHandler;
import util.misc.NetEdit;

/**
 *  Implements the Graphical User Interface ... and much more
 *
 */
public class Butler extends JFrame implements ActionListener, EvolutionSaxInterface, Observer {

    private boolean keepOnRunning = true;

    private ProcessorThread processThread = null;

    private Document document = null;

    private EvolutionSaxHandlerThread eh = null;

    private final String RUN_LABEL = new String("Run Net");

    private final String STOP_LABEL = new String("Stop Net");

    private final String CONNECT = new String("Connect");

    private final String ANALYSER_LABEL = new String("Analyser");

    private final String LOAD_NET_LABEL = new String("Get Net");

    private final String LOAD_FILE_LABEL = new String("Load File");

    private final String NET_SELECTOR_LABEL = new String("...");

    private final String VIEW_NET_LABEL = new String("View Net");

    private final String SELECT_SIM_PARAM_LABEL = new String("Select Parameter");

    private final String EXIT_LABEL = new String("Exit");

    private NetEdit netEdit = new NetEdit();

    private JPanel evolutionComPanel = new JPanel();

    private JPanel parameterSelectionPanel = new JPanel();

    private JPanel simulatorComPanel = new JPanel();

    private JPanel rootPanel = new JPanel();

    private Net net = new Net();

    private JButton runButton = null;

    private JButton viewNetButton = null;

    private JButton analysisButton = null;

    private JButton exitButton = new JButton(EXIT_LABEL);

    private JButton loadConfigButton = new JButton("load cfg");

    private JButton saveConfigButton = new JButton("save cfg");

    private JButton processParameterButton = new JButton("parameter");

    private JButton reloadFFButton = new JButton("F-Function");

    private JButton simulatorReloadButton = new JButton("Simulators");

    private JButton showLastNetDataButton = new JButton("past");

    private JButton loadNetButton = new JButton(LOAD_NET_LABEL);

    private ProcessParameter processParameter = new ProcessParameter();

    private ProcessParameterDialog processParameterDialog = new ProcessParameterDialog(processParameter);

    private LastNetDialog showLastNetDataPanel = new LastNetDialog();

    private Processor processor = new Processor();

    private JLabel status = null;

    private JButton parameterSelectionButton = null;

    private EvoComInterface evoCom = null;

    private File loadNetFile = null;

    private LoadNetMode loadingNetMode = LoadNetMode.TCPIP;

    private Analyser analyser = Analyser.getInstance();

    private FitnessFunctionBroker fitnessBroker = new FitnessFunctionBroker();

    private SimComBroker simComBroker = new SimComBroker();

    private RobComBroker robComBroker = new RobComBroker();

    private JFileChooser fileDialog = new JFileChooser(".");

    private JTextField populationInput = new JTextField("0", 3);

    private JTextField generationInput = new JTextField("0", 3);

    private JTextField individualInput = new JTextField("0", 3);

    private JTextField internetAddrInput = new JTextField("localhost", 8);

    private JTextField portInput = new JTextField("7000", 4);

    private JTextField simPortInput = new JTextField("0", 4);

    private JTextField simInternetAddrInput = new JTextField("localhost", 8);

    private JComboBox simulators = null;

    private JComboBox evolators = null;

    private JComboBox robots = null;

    private JComboBox fitnessFunctions = null;

    JButton cenConnectButton = null;

    JButton simConnectButton = null;

    JButton simDisconnectButton = new JButton("Disconnect");

    private AmbassadorInterface ai = null;

    /**
   * Default constructor. Initialises the Panels and Events.
   */
    public Butler() {
        super("Hinton");
        this.addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                quit();
            }
        });
        evolutionComPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Neural Net Communication"), BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        parameterSelectionPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Parameter Selection"), BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        simulatorComPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Simulator/Roboter Communication"), BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        rootPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        rootPanel.setLayout(new GridLayout(1, 3, 10, 10));
        rootPanel.add(evolutionComPanel);
        rootPanel.add(parameterSelectionPanel);
        rootPanel.add(simulatorComPanel);
        getContentPane().add(rootPanel);
        String s[];
        Vector simulatorNames = new Vector();
        s = simComBroker.getSimComNames();
        for (int i = 0; i < s.length; i++) {
            simulatorNames.add(s[i]);
        }
        simulators = new JComboBox(simulatorNames);
        simulators.addActionListener(this);
        Vector evolatorNames = new Vector();
        evolatorNames.add("");
        for (int i = 0; i < EvoComInterface.LIST.length; i++) {
            evolatorNames.add(EvoComInterface.LIST[i].getName());
        }
        evolators = new JComboBox(evolatorNames);
        evolators.addActionListener(this);
        s = robComBroker.getRobotComNames();
        Vector robotNames = new Vector();
        for (int i = 0; i < s.length; i++) {
            robotNames.add(s[i]);
        }
        robots = new JComboBox(robotNames);
        s = fitnessBroker.getFitnessFunctionNames();
        Vector fitnessFunctionNames = new Vector();
        for (int i = 0; i < s.length; i++) {
            fitnessFunctionNames.add(s[i]);
        }
        fitnessFunctions = new JComboBox(fitnessFunctionNames);
        fillEvolutionComPanel();
        fillParameterPanel();
        fillSimulatorComPanel();
        reloadFFButton.setToolTipText("Click here to reload all fitness functions" + " from disk");
        simulatorReloadButton.setToolTipText("Click here to reload all simulator" + " classes from disk");
        fitnessFunctions.addActionListener(this);
        reloadFFButton.addActionListener(this);
        simulatorReloadButton.addActionListener(this);
        showLastNetDataButton.addActionListener(this);
        loadNetButton.setEnabled(false);
        showLastNetDataPanel.setProcessParameter(processParameter);
        pack();
    }

    private void updateParameterPanel() {
        if (processParameter != null) {
        }
    }

    private ProcessParameter getProcessParameter() {
        return processParameter;
    }

    private JPanel getFileComPanel() {
        JPanel fileComPanel = new JPanel();
        GridBagLayout fileComGL = new GridBagLayout();
        JLabel populationLabel = new JLabel("Population");
        JLabel generationLabel = new JLabel("Generation");
        JLabel individualLabel = new JLabel("Individual");
        JButton loadFileButton = new JButton(LOAD_FILE_LABEL);
        JButton selectNetButton = new JButton(NET_SELECTOR_LABEL);
        GridBagConstraints populationLabelConstraints = new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5);
        GridBagConstraints generationLabelConstraints = new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5);
        GridBagConstraints individualLabelConstraints = new GridBagConstraints(0, 2, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5);
        GridBagConstraints selectNetConstaints = new GridBagConstraints(2, 2, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5);
        GridBagConstraints populationInputConstraints = new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5);
        GridBagConstraints generationInputConstraints = new GridBagConstraints(1, 1, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5);
        GridBagConstraints individualInputConstraints = new GridBagConstraints(1, 2, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5);
        GridBagConstraints loadFileButtonConstraints = new GridBagConstraints(0, 3, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5);
        GridBagConstraints loadNetButtonConstraints = new GridBagConstraints(1, 3, 2, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5);
        fileComGL.setConstraints(populationLabel, populationLabelConstraints);
        fileComGL.setConstraints(generationLabel, generationLabelConstraints);
        fileComGL.setConstraints(individualLabel, individualLabelConstraints);
        fileComGL.setConstraints(populationInput, populationInputConstraints);
        fileComGL.setConstraints(generationInput, generationInputConstraints);
        fileComGL.setConstraints(individualInput, individualInputConstraints);
        fileComGL.setConstraints(loadNetButton, loadNetButtonConstraints);
        fileComGL.setConstraints(loadFileButton, loadFileButtonConstraints);
        fileComGL.setConstraints(selectNetButton, selectNetConstaints);
        fileComPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("File Load"), BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        loadNetButton.addActionListener(this);
        loadFileButton.addActionListener(this);
        selectNetButton.addActionListener(this);
        selectNetButton.setPreferredSize(new Dimension(15, 15));
        fileComPanel.setLayout(fileComGL);
        fileComPanel.add(populationLabel);
        fileComPanel.add(populationInput);
        fileComPanel.add(generationLabel);
        fileComPanel.add(generationInput);
        fileComPanel.add(individualLabel);
        fileComPanel.add(individualInput);
        fileComPanel.add(loadFileButton);
        fileComPanel.add(loadNetButton);
        fileComPanel.add(selectNetButton);
        return fileComPanel;
    }

    private JPanel getCenComPanel() {
        JPanel cenComPanel = new JPanel();
        GridBagLayout cenComGL = new GridBagLayout();
        JLabel internetAddrLabel = new JLabel("INetAddr");
        JLabel portLabel = new JLabel("Port");
        JLabel evoLabel = new JLabel("evo. prog.");
        cenConnectButton = new JButton(CONNECT);
        portInput.setHorizontalAlignment(JTextField.RIGHT);
        GridBagConstraints internetAddrLabelConstraints = new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5);
        GridBagConstraints portLabelConstraints = new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5);
        GridBagConstraints evoLabelConstraints = new GridBagConstraints(0, 2, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5);
        GridBagConstraints internetAddrInputConstraints = new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5);
        GridBagConstraints portInputConstraints = new GridBagConstraints(1, 1, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5);
        GridBagConstraints evoComboBoxConstraints = new GridBagConstraints(1, 2, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5);
        GridBagConstraints connectButtonConstraints = new GridBagConstraints(1, 3, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5);
        cenComGL.setConstraints(internetAddrLabel, internetAddrLabelConstraints);
        cenComGL.setConstraints(portLabel, portLabelConstraints);
        cenComGL.setConstraints(evoLabel, evoLabelConstraints);
        cenComGL.setConstraints(internetAddrInput, internetAddrInputConstraints);
        cenComGL.setConstraints(portInput, portInputConstraints);
        cenComGL.setConstraints(evolators, evoComboBoxConstraints);
        cenComGL.setConstraints(cenConnectButton, connectButtonConstraints);
        cenComPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("TCP/IP Connection"), BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        cenConnectButton.addActionListener(this);
        cenComPanel.setLayout(cenComGL);
        cenComPanel.add(internetAddrLabel);
        cenComPanel.add(internetAddrInput);
        cenComPanel.add(portLabel);
        cenComPanel.add(evoLabel);
        cenComPanel.add(evolators);
        cenComPanel.add(portInput);
        cenComPanel.add(cenConnectButton);
        return cenComPanel;
    }

    private void fillEvolutionComPanel() {
        GridLayout top_gl = new GridLayout(2, 1);
        evolutionComPanel.setLayout(top_gl);
        evolutionComPanel.add(getCenComPanel());
        evolutionComPanel.add(getFileComPanel());
    }

    private void fillSimulatorComPanel() {
        GridBagLayout gl = new GridBagLayout();
        JLabel ipLabel = new JLabel("IP");
        JLabel portLabel = new JLabel("Port");
        JLabel robotLabel = new JLabel("Robot");
        simConnectButton = new JButton(CONNECT);
        GridBagConstraints ipLabelConstraints = new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5);
        GridBagConstraints portLabelConstraints = new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5);
        GridBagConstraints ipInputConstraints = new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5);
        GridBagConstraints portInputConstraints = new GridBagConstraints(1, 1, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5);
        GridBagConstraints simulatorLabelConstraints = new GridBagConstraints(0, 2, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5);
        GridBagConstraints robotLabelConstraints = new GridBagConstraints(0, 3, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5);
        GridBagConstraints simulatorsConstraints = new GridBagConstraints(1, 2, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5);
        GridBagConstraints robotsConstraints = new GridBagConstraints(1, 3, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5);
        GridBagConstraints simConnectButtonConstraints = new GridBagConstraints(1, 4, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5);
        GridBagConstraints simDisconnectButtonConstraints = new GridBagConstraints(1, 5, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5);
        GridBagConstraints parameterSelectionButtonConstraints = new GridBagConstraints(1, 6, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5);
        simPortInput.setHorizontalAlignment(JTextField.RIGHT);
        gl.setConstraints(ipLabel, ipLabelConstraints);
        gl.setConstraints(portLabel, portLabelConstraints);
        gl.setConstraints(simulatorReloadButton, simulatorLabelConstraints);
        gl.setConstraints(robotLabel, robotLabelConstraints);
        gl.setConstraints(simulators, simulatorsConstraints);
        gl.setConstraints(robots, robotsConstraints);
        gl.setConstraints(simConnectButton, simConnectButtonConstraints);
        gl.setConstraints(simDisconnectButton, simDisconnectButtonConstraints);
        gl.setConstraints(simInternetAddrInput, ipInputConstraints);
        gl.setConstraints(simPortInput, portInputConstraints);
        simConnectButton.addActionListener(this);
        simDisconnectButton.addActionListener(this);
        simulatorComPanel.setLayout(gl);
        simulatorComPanel.add(ipLabel);
        simulatorComPanel.add(portLabel);
        simulatorComPanel.add(robotLabel);
        simulatorComPanel.add(simulatorReloadButton);
        simulatorComPanel.add(simConnectButton);
        simulatorComPanel.add(simDisconnectButton);
        simulatorComPanel.add(simulators);
        simulatorComPanel.add(simInternetAddrInput);
        simulatorComPanel.add(simPortInput);
        simulatorComPanel.add(robots);
    }

    private void fillParameterPanel() {
        GridBagLayout gbl = new GridBagLayout();
        JLabel iterationsLabel = new JLabel("Iterations");
        JLabel cyclesLabel = new JLabel("Cycles");
        JLabel triesLabel = new JLabel("Tries");
        JLabel statusLabel = new JLabel("Status");
        JLabel fitnessFunctionLabel = new JLabel("F-Function");
        JLabel speedFactorLabel = new JLabel("Speed factor");
        status = new JLabel("stopped");
        runButton = new JButton(RUN_LABEL);
        analysisButton = new JButton(ANALYSER_LABEL);
        viewNetButton = new JButton(VIEW_NET_LABEL);
        if (net == null) {
        }
        int row = -1;
        GridBagConstraints statusLabelConstraints = new GridBagConstraints(0, ++row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5);
        GridBagConstraints statusConstraints = new GridBagConstraints(1, row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5);
        GridBagConstraints reloadFFConstraints = new GridBagConstraints(0, ++row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 5, 5);
        GridBagConstraints fitnessFunctionConstraints = new GridBagConstraints(1, row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5);
        GridBagConstraints viewNetButtonConstraints = new GridBagConstraints(0, ++row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5);
        GridBagConstraints analysisButtonConstraints = new GridBagConstraints(1, row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5);
        GridBagConstraints runButtonConstraints = new GridBagConstraints(0, ++row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5);
        GridBagConstraints showLastNetDataButtonConstraints = new GridBagConstraints(1, row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5);
        GridBagConstraints loadConfigButtonConstraints = new GridBagConstraints(1, ++row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5);
        GridBagConstraints saveConfigButtonConstraints = new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5);
        GridBagConstraints processParameterConstraints = new GridBagConstraints(0, ++row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5);
        GridBagConstraints exitButtonConstraints = new GridBagConstraints(1, row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5);
        gbl.setConstraints(analysisButton, analysisButtonConstraints);
        gbl.setConstraints(statusLabel, statusLabelConstraints);
        gbl.setConstraints(status, statusConstraints);
        gbl.setConstraints(fitnessFunctions, fitnessFunctionConstraints);
        gbl.setConstraints(reloadFFButton, reloadFFConstraints);
        gbl.setConstraints(runButton, runButtonConstraints);
        gbl.setConstraints(viewNetButton, viewNetButtonConstraints);
        gbl.setConstraints(exitButton, exitButtonConstraints);
        gbl.setConstraints(showLastNetDataButton, showLastNetDataButtonConstraints);
        gbl.setConstraints(loadConfigButton, loadConfigButtonConstraints);
        gbl.setConstraints(saveConfigButton, saveConfigButtonConstraints);
        gbl.setConstraints(processParameterButton, processParameterConstraints);
        parameterSelectionPanel.setLayout(gbl);
        parameterSelectionPanel.add(runButton);
        parameterSelectionPanel.add(analysisButton);
        parameterSelectionPanel.add(statusLabel);
        parameterSelectionPanel.add(status);
        parameterSelectionPanel.add(fitnessFunctions);
        parameterSelectionPanel.add(reloadFFButton);
        parameterSelectionPanel.add(viewNetButton);
        parameterSelectionPanel.add(showLastNetDataButton);
        parameterSelectionPanel.add(exitButton);
        parameterSelectionPanel.add(loadConfigButton);
        parameterSelectionPanel.add(saveConfigButton);
        parameterSelectionPanel.add(processParameterButton);
        viewNetButton.addActionListener(this);
        runButton.addActionListener(this);
        analysisButton.addActionListener(this);
        exitButton.addActionListener(this);
        loadConfigButton.addActionListener(this);
        saveConfigButton.addActionListener(this);
        processParameterButton.addActionListener(this);
    }

    /**
   * Handles all GUI-Action. That is:
   * <ul>
   * <li> "Load Net"-Button </li>
   * <li> "Run Net"-Button </li>
   * </ul>
   * @param    e ActionEvent e
   * @return   none
   */
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals(NET_SELECTOR_LABEL)) {
            reading.gui.Butler readingButler = new reading.gui.Butler("cfg/log4j.cfg", this);
            readingButler.show();
        }
        if (e.getSource() == simDisconnectButton) {
            if (ai != null) {
                ai.disconnect();
            }
        }
        if (e.getSource() == evolators) {
            if (evolators.getSelectedIndex() > 0 && EvoComInterface.LIST[evolators.getSelectedIndex() - 1].autoRun()) {
            } else {
                runButton.setEnabled(true);
                simConnectButton.setEnabled(true);
            }
            return;
        }
        if (e.getSource() == processParameterButton) {
            processParameterDialog.setVisible(true);
            return;
        }
        if (e.getSource() == saveConfigButton) {
            saveConfig();
            return;
        }
        if (e.getSource() == loadConfigButton) {
            loadConfig();
            return;
        }
        if (e.getSource() == fitnessFunctions) {
            processor.setFitnessFunction(fitnessBroker.getFitnessFunction((String) fitnessFunctions.getSelectedItem()));
            return;
        }
        if (e.getSource() == showLastNetDataButton) {
            if (showLastNetDataPanel.isVisible()) {
                showLastNetDataPanel.setVisible(false);
            } else {
                showLastNetDataPanel.setVisible(true);
            }
        }
        if (e.getSource() == simulatorReloadButton) {
            int i;
            if (status.getText().equalsIgnoreCase("running")) {
                i = JOptionPane.showConfirmDialog(this, "Hinton says you are running a simulation! Reloading all communication classes\n" + "will crash your simulation if it is not interrupted. I propose\n" + "you click the stop button before relaoding all communication classes.\n" + "\nDo you anyway want to reload all communication classes?", "Attention", JOptionPane.YES_NO_OPTION);
                if (i != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            simulators.removeAllItems();
            Vector simulatorNames = new Vector();
            simComBroker.reloadSimComList();
            String s[] = simComBroker.getSimComNames();
            for (i = 0; i < s.length; i++) {
                simulators.addItem(s[i]);
            }
        }
        if (e.getSource() == reloadFFButton) {
            int i;
            if (status.getText().equalsIgnoreCase("running")) {
                i = JOptionPane.showConfirmDialog(this, "Hinton says you are running a simulation! Reloading all fitness functions\n" + "will crash your simulation if it is not interrupted. I propose\n" + "you click the stop button before relaoding all fitness functions.\n" + "\nDo you anyway want to reload all fitness functions?", "Attention", JOptionPane.YES_NO_OPTION);
                if (i != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            fitnessFunctions.removeAllItems();
            fitnessBroker.reloadFitnessFunctionsList();
            String s[] = fitnessBroker.getFitnessFunctionNames();
            for (i = 0; i < s.length; i++) {
                fitnessFunctions.addItem(s[i]);
            }
        }
        if (e.getSource() == simulators) {
            if (simulators.getSelectedIndex() == 0) {
                simPortInput.setText("0");
                return;
            }
            simPortInput.setText("" + simComBroker.getSimCom((String) simulators.getSelectedItem()).getPreferredPort());
            return;
        }
        if (e.getSource() == exitButton) {
            System.exit(0);
        }
        if (e.getActionCommand().equals(ANALYSER_LABEL)) {
            analyser.setVisible(true);
            analyser.setNet(net);
            return;
        }
        if (e.getActionCommand().equals(LOAD_NET_LABEL)) {
            loadNet();
            return;
        }
        if (e.getActionCommand().equals(LOAD_FILE_LABEL)) {
            loadFile();
            return;
        }
        if (e.getActionCommand().equals(RUN_LABEL)) {
            run();
            return;
        }
        if (e.getActionCommand().equals(STOP_LABEL)) {
            stop();
            return;
        }
        if (e.getActionCommand().equals(VIEW_NET_LABEL)) {
            if (netEdit.isVisible()) {
                netEdit.setVisible(false);
            } else {
                netEdit.setVisible(true);
                netEdit.setNet(net);
            }
            return;
        }
        if (e.getActionCommand().equals(SELECT_SIM_PARAM_LABEL)) {
            RobotStruct rs = null;
            if (simulators.getSelectedIndex() != 0) {
                rs = simComBroker.getSimCom((String) simulators.getSelectedItem()).robotStruct();
            } else if (robots.getSelectedIndex() != 0) {
                rs = robComBroker.getRobotCom((String) robots.getSelectedItem()).robotStruct();
            }
            ParameterDialog pd = new ParameterDialog(rs, net);
            pd.setVisible(true);
            return;
        }
        if (e.getActionCommand().equals(CONNECT)) {
            if (e.getSource() == cenConnectButton) {
                connectToEvolutionProgram();
            }
            if (e.getSource() == simConnectButton) {
                if (simulators.getSelectedIndex() != 0) {
                    AmbassadorInterface ai = simComBroker.getSimCom((String) simulators.getSelectedItem());
                    ai.setPortIP(Integer.parseInt(simPortInput.getText()), simInternetAddrInput.getText());
                    ai.connect();
                }
            }
            return;
        }
        System.out.println(e.getActionCommand());
    }

    public void stop() {
        System.out.println("STOPPED");
        processThread.stop();
        processor.getAmbassador().stop();
        if (processor.getAmbassador() instanceof RobCom) {
            processor.getAmbassador().close();
        }
        runButton.setText(RUN_LABEL);
    }

    private boolean loadFile() {
        fileDialog.addChoosableFileFilter(new ExFileFilter("xml", "Neural Net ( .xml )"));
        int returnVal = fileDialog.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            if (fileDialog.getSelectedFile().exists()) {
                System.out.println("You chose to open this file: " + fileDialog.getCurrentDirectory() + "/" + fileDialog.getSelectedFile().getName());
                loadNetFile = fileDialog.getSelectedFile();
                loadNet();
                loadNetButton.setEnabled(true);
                setTitle("Hinton -- " + fileDialog.getSelectedFile().getName());
            } else {
                System.out.println("File does not exist: " + fileDialog.getCurrentDirectory() + "/" + fileDialog.getSelectedFile().getName());
                loadNetFile = null;
                loadNetButton.setEnabled(false);
            }
            return true;
        }
        return false;
    }

    public void loadCommandLineNet(String commandLineFilename) {
        this.loadCommandLineNet(commandLineFilename, 0, 0, 0);
    }

    public void loadCommandLineNet(String commandLineFilename, int generationIndex, int populationIndex, int individualIndex) {
        System.out.println("Trying to load net (pop: " + populationIndex + ", gen: " + generationIndex + ", ind: " + individualIndex + ")");
        eh = new EvolutionSaxHandlerThread(commandLineFilename, generationIndex, populationIndex, individualIndex, this, this);
        Thread t = new Thread(eh);
        t.start();
    }

    private void loadNet() {
        if (loadNetFile == null) {
            System.out.println("You did not load a (valid) Net-File. " + "So, there is no net which can be loaded");
            return;
        }
        int populationIndex = Integer.parseInt(populationInput.getText());
        int generationIndex = Integer.parseInt(generationInput.getText());
        int individualIndex = Integer.parseInt(individualInput.getText());
        System.out.println("Trying to load net (pop: " + populationIndex + ", gen: " + generationIndex + ", ind: " + individualIndex + ")");
        eh = new EvolutionSaxHandlerThread(loadNetFile.getAbsolutePath(), generationIndex, populationIndex, individualIndex, this, this);
        Thread t = new Thread(eh);
        t.start();
    }

    public void netLoaded() {
        net = eh.getNet();
        updateNet(net);
        if (net != null) {
            runButton.setEnabled(true);
            analysisButton.setEnabled(true);
            viewNetButton.setEnabled(true);
            System.out.println("Net loaded.");
        } else {
            int populationIndex = Integer.parseInt(populationInput.getText());
            int generationIndex = Integer.parseInt(generationInput.getText());
            int individualIndex = Integer.parseInt(individualInput.getText());
            XMLHandler xmlh = new XMLHandler();
            xmlh.read(loadNetFile.getAbsolutePath());
            net = xmlh.getNet(xmlh.getNetNode(generationIndex, individualIndex));
            if (net == null) {
                System.out.println("Could not load net! Wrong segmentation or grammar...");
                System.out.println("Please Check Parameters");
                net = new Net();
                return;
            }
            System.out.println("Net loaded from OLD grammar. Everything is ok, but " + " you should as soon as possible convert it to the new grammar.");
            updateNet(net);
        }
        return;
    }

    private void run() {
        if (simulators.getSelectedIndex() == 0 && robots.getSelectedIndex() == 0 && evoCom == null) {
            return;
        }
        runButton.setText(STOP_LABEL);
        setSize(getMinimumSize());
        if (simulators.getSelectedIndex() > 0) {
            ai = simComBroker.getSimCom((String) simulators.getSelectedItem());
            ai.setProcessParameter(processParameter);
        }
        if (robots.getSelectedIndex() > 0) {
            ai = robComBroker.getRobotCom((String) robots.getSelectedItem());
            ai.setProcessParameter(processParameter);
            ai.connect();
        }
        FitnessFunctionInterface ff = fitnessBroker.getFitnessFunction((String) fitnessFunctions.getSelectedItem());
        ff.setProcessParameter(processParameter);
        processParameter.setFitnessFunction(ff);
        analyser.setNet(net);
        processor.setParameter(processParameter);
        processor.setAnalyser(analyser);
        processor.setAmbassador(ai);
        processor.setFitnessFunction(ff);
        analyser.setRobotStruct(ai.robotStruct());
        if (evoCom != null) {
            evoCom.setProcessParameter(processParameter);
        }
        if (loadingNetMode == LoadNetMode.FILE_LOAD) {
            processThread = new ProcessorThread(processor, null, status, showLastNetDataPanel);
            processThread.setProcessParameter(processParameter);
        } else {
            processThread = new ProcessorThread(processor, evoCom, status, showLastNetDataPanel);
        }
        new Thread(processThread).start();
    }

    private void saveConfig() {
        System.out.println("saving the config");
        String filename = null;
        fileDialog.addChoosableFileFilter(new ExFileFilter("xml", "Neural Net ( .xml )"));
        int returnVal = fileDialog.showSaveDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            filename = new String(fileDialog.getCurrentDirectory() + System.getProperty("file.separator") + fileDialog.getSelectedFile().getName());
            if (filename.indexOf(".") == -1) {
                filename = new String(filename + ".xml");
            }
            File xmlFile = new File(filename);
            try {
                PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(xmlFile)));
                out.println("<?xml version=\"1.0\" encoding=\"LATIN1\"?>");
                out.println("<!DOCTYPE HintonMainDefaultValues [");
                out.println("  <!ELEMENT HintonMainDefaultValues EMPTY>");
                out.println("  <!ATTLIST HintonMainDefaultValues");
                out.println("            TCPIPHost         CDATA #REQUIRED");
                out.println("            TCPIPPort         CDATA #REQUIRED");
                out.println("            EvolutionProgramm CDATA #REQUIRED");
                out.println("            Tries             CDATA #REQUIRED");
                out.println("            Iterations        CDATA #REQUIRED");
                out.println("            InitialIterations CDATA #IMPLIED");
                out.println("            Cycles            CDATA #REQUIRED");
                out.println("            WarmUpSteps       CDATA #REQUIRED");
                out.println("            Constants0        CDATA #REQUIRED");
                out.println("            Constants1        CDATA #REQUIRED");
                out.println("            Constants2        CDATA #REQUIRED");
                out.println("            Constants3        CDATA #REQUIRED");
                out.println("            Display           CDATA #REQUIRED");
                out.println("            SpeedFactor       CDATA #REQUIRED");
                out.println("            FitnessFunction   CDATA #REQUIRED");
                out.println("            SimulatorIP       CDATA #REQUIRED");
                out.println("            SimulatorPort     CDATA #REQUIRED");
                out.println("            Simulator         CDATA #REQUIRED>");
                out.println("]>");
                out.println("<HintonMainDefaultValues");
                out.println("            TCPIPHost         =\"" + internetAddrInput.getText().trim() + "\"");
                out.println("            TCPIPPort         =\"" + portInput.getText().trim() + "\"");
                out.println("            EvolutionProgramm =\"" + evolators.getSelectedIndex() + "\"");
                out.println("            Tries             =\"" + processParameter.tries() + "\"");
                out.println("            Iterations        =\"" + processParameter.iterations() + "\"");
                out.println("            InitialIterations =\"" + processParameter.getInitialIterations() + "\"");
                out.println("            Cycles            =\"" + processParameter.cycles() + "\"");
                out.println("            WarmUpSteps       = \"" + processParameter.warmUpSteps() + "\"");
                out.println("            SpeedFactor       = \"" + processParameter.getMaxSpeed() + "\"");
                out.println("            Constants0        = \"" + processParameter.getConstant(0) + "\"");
                out.println("            Constants1        = \"" + processParameter.getConstant(1) + "\"");
                out.println("            Constants2        = \"" + processParameter.getConstant(2) + "\"");
                out.println("            Constants3        = \"" + processParameter.getConstant(3) + "\"");
                out.println("            Display           = \"" + ((processParameter.getDisplay()) ? 1 : 0) + "\"");
                out.println("            FitnessFunction   = \"" + String.valueOf(fitnessFunctions.getSelectedItem()) + "\"");
                out.println("            SimulatorIP       = \"" + simInternetAddrInput.getText().trim() + "\"");
                out.println("            SimulatorPort     = \"" + simPortInput.getText().trim() + "\"");
                out.println("            Simulator         = \"" + String.valueOf(simulators.getSelectedItem()) + "\"");
                out.println("    />");
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void loadConfig() {
        System.out.println("loading the config");
        String filename = null;
        fileDialog.addChoosableFileFilter(new ExFileFilter("xml", "Config File ( .xml )"));
        int returnVal = fileDialog.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            filename = new String(fileDialog.getCurrentDirectory() + File.separator + fileDialog.getSelectedFile().getName());
            System.out.println("Loading " + filename);
            loadConfig(filename);
        }
    }

    public void loadConfig(String filename) {
        File file = new File(filename);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(true);
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.parse(file);
        } catch (FileNotFoundException e) {
            JOptionPane.showMessageDialog(this, "<html> Config File not found<br>", "Config File Error", JOptionPane.ERROR_MESSAGE);
            return;
        } catch (SAXException sxe) {
            Exception x = sxe;
            if (sxe.getException() != null) {
                x = sxe.getException();
            }
            x.printStackTrace();
        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        Element configData = document.getDocumentElement();
        if (configData == null) {
            System.out.println("ups!!");
            return;
        }
        internetAddrInput.setText(configData.getAttribute("TCPIPHost").trim());
        portInput.setText(configData.getAttribute("TCPIPPort").trim());
        evolators.setSelectedIndex(Integer.parseInt(configData.getAttribute("EvolutionProgramm").trim()));
        fitnessFunctions.setSelectedItem(configData.getAttribute("FitnessFunction").trim());
        try {
            simulators.setSelectedIndex(Integer.parseInt(configData.getAttribute("Simulator").trim()));
            System.out.println("This is an old version of config file types." + " No Problem, it can be read, but please rewrite it soon!");
        } catch (NumberFormatException ex) {
            simulators.setSelectedItem(configData.getAttribute("Simulator").trim());
        }
        simInternetAddrInput.setText(configData.getAttribute("SimulatorIP").trim());
        simPortInput.setText(configData.getAttribute("SimulatorPort").trim());
        processParameter.setMaxSpeed(Double.parseDouble(configData.getAttribute("SpeedFactor").trim()));
        processParameter.setCycles(Integer.parseInt(configData.getAttribute("Cycles").trim()));
        processParameter.setIterations(Integer.parseInt(configData.getAttribute("Iterations").trim()));
        if (configData.getAttribute("InitialIterations") != null) {
            processParameter.setInitialIterations(Integer.parseInt(configData.getAttribute("InitialIterations").trim()));
        }
        processParameter.setTries(Integer.parseInt(configData.getAttribute("Tries").trim()));
        processParameter.setWarmUpSteps(Integer.parseInt(configData.getAttribute("WarmUpSteps").trim()));
        processParameter.setConstants(Double.parseDouble(configData.getAttribute("Constants0").trim()), Double.parseDouble(configData.getAttribute("Constants1").trim()), Double.parseDouble(configData.getAttribute("Constants2").trim()), Double.parseDouble(configData.getAttribute("Constants3").trim()));
        processParameter.setDisplay((Integer.parseInt(configData.getAttribute("Display").trim()) == 0) ? false : true);
        processParameterDialog.updatePanel();
    }

    private void connectToEvolutionProgram() {
        evoCom = null;
        if (evolators.getSelectedIndex() == 0) {
            return;
        }
        evoCom = EvoComInterface.LIST[evolators.getSelectedIndex() - 1];
        evoCom.connect(internetAddrInput.getText(), Integer.parseInt(portInput.getText()));
        evoCom.setProcessParameter(processParameter);
        runButton.setEnabled(true);
        analysisButton.setEnabled(true);
        if (evoCom.autoRun()) {
            simConnectButton.setEnabled(true);
            runButton.setEnabled(true);
            simConnectButton.doClick();
            runButton.doClick();
        }
        loadingNetMode = LoadNetMode.TCPIP;
    }

    private void quit() {
        System.exit(0);
    }

    public void update(Observable o, Object arg) {
        updateNet((Net) arg);
    }

    private void updateNet(Net net) {
        loadingNetMode = LoadNetMode.FILE_LOAD;
        this.net = net;
        processParameter.setNet(net);
        analyser.setNet(net);
        if (netEdit.isVisible()) {
            netEdit.setVisible(false);
            netEdit.setNet(net);
            netEdit.setVisible(true);
        }
    }
}
