package gov.sns.apps.virtualacceleratorold;

import java.util.*;
import java.util.prefs.*;
import java.net.*;
import java.io.*;
import java.awt.BorderLayout;
import java.awt.event.*;
import java.awt.GridLayout;
import java.text.NumberFormat;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.event.*;
import javax.swing.JToggleButton.ToggleButtonModel;
import gov.sns.xal.smf.application.*;
import gov.sns.application.*;
import gov.sns.ca.*;
import gov.sns.xal.smf.*;
import gov.sns.xal.smf.attr.*;
import gov.sns.xal.smf.data.*;
import gov.sns.xal.smf.impl.*;
import gov.sns.xal.smf.impl.qualify.OrTypeQualifier;
import gov.sns.xal.model.*;
import gov.sns.xal.model.probe.*;
import gov.sns.xal.model.alg.*;
import gov.sns.xal.model.probe.traj.TransferMapState;
import gov.sns.xal.model.probe.traj.ProbeState;
import gov.sns.xal.model.probe.traj.EnvelopeProbeState;
import gov.sns.xal.model.probe.traj.ICoordinateState;
import gov.sns.xal.model.xml.*;
import gov.sns.xal.model.xml.ParsingException;
import gov.sns.xal.model.scenario.Scenario;
import gov.sns.xal.tools.virtualaccelerator.PCASGenerator;
import gov.sns.tools.apputils.SimpleProbeEditor;
import gov.sns.tools.xml.*;
import gov.sns.tools.data.*;
import gov.sns.tools.beam.PhaseVector;
import gov.sns.tools.swing.DecimalField;
import gov.sns.tools.apputils.files.*;
import gov.sns.tools.apputils.pvlogbrowser.PVLogSnapshotChooser;
import gov.sns.xal.model.pvlogger.PVLoggerDataSource;

/**
 * VADocument is a custom AcceleratorDocument for virtual accelerator
 * application.
 * 
 * @version 1.5 15 Jul 2004
 * @author Paul Chu
 */
public class VADocument extends AcceleratorDocument implements ActionListener, PutListener {

    /**
	 * The document for the text pane in the main window.
	 */
    protected PlainDocument textDocument;

    /** For on-line model */
    protected Scenario model;

    protected gov.sns.xal.slg.Lattice lattice = null;

    private Probe myProbe;

    String dataSource = Scenario.SYNC_MODE_LIVE;

    protected String theProbeFile;

    int runT3d_OK = 0;

    private JDialog setNoise = new JDialog();

    private DecimalField df1, df2, df3, df4;

    private DecimalField df11, df21, df31, df41;

    private double quadNoise = 0.0;

    private double dipoleNoise = 0.0;

    private double correctorNoise = 0.0;

    private double bpmNoise = 0.0;

    private double rfAmpNoise = 0.0;

    private double rfPhaseNoise = 0.0;

    private double quadOffset = 0.0;

    private double dipoleOffset = 0.0;

    private double correctorOffset = 0.0;

    private double bpmOffset = 0.0;

    private double rfAmpOffset = 0.0;

    private double rfPhaseOffset = 0.0;

    private JButton done = new JButton("OK");

    private boolean vaRunning = false;

    private Collection magMainSupplies;

    private java.util.List rfCavities;

    private java.util.List mags;

    private java.util.List bpms;

    private java.util.List wss;

    private Channel beamOnEvent;

    private Channel beamOnEventCount;

    private long beamOnEventCounter = 0;

    private Map rb_setMap;

    private Map ch_noiseMap;

    private Map ch_offsetMap;

    protected PCAS pcas;

    String pcasPath = "";

    Preferences pcasPrefs = Preferences.userNodeForPackage(this.getClass());

    protected static String pcasFileName = "";

    private RecentFileTracker _probeFileTracker;

    ToggleButtonModel olmModel = new ToggleButtonModel();

    ToggleButtonModel pvlogModel = new ToggleButtonModel();

    ToggleButtonModel pvlogMovieModel = new ToggleButtonModel();

    private boolean isFromPVLogger = false;

    private PVLogSnapshotChooser plsc;

    private JDialog pvLogSelector;

    private PVLoggerDataSource plds;

    /** Create a new empty document */
    public VADocument() {
        this(null);
    }

    /**
	 * Create a new document loaded from the URL file
	 * 
	 * @param url
	 *            The URL of the file to load into the new document.
	 */
    public VADocument(java.net.URL url) {
        setSource(url);
        makeTextDocument();
        _probeFileTracker = new RecentFileTracker(1, this.getClass(), "recent_probes");
        if (url == null) return;
    }

    /**
	 * Make a main window by instantiating the my custom window. Set the text
	 * pane to use the textDocument variable as its document.
	 */
    @Override
    public void makeMainWindow() {
        mainWindow = new VAWindow(this);
        JPanel settingPanel = new JPanel();
        JPanel noiseLevelPanel = new JPanel();
        JPanel offsetPanel = new JPanel();
        noiseLevelPanel.setLayout(new GridLayout(7, 1));
        noiseLevelPanel.add(new JLabel("Noise Level for Device Type:"));
        JLabel percent = new JLabel("%");
        NumberFormat numberFormat;
        numberFormat = NumberFormat.getNumberInstance();
        numberFormat.setMaximumFractionDigits(3);
        JPanel noiseLevel1 = new JPanel();
        noiseLevel1.setLayout(new GridLayout(1, 3));
        JLabel label1 = new JLabel("Quad: ");
        df1 = new DecimalField(0., 5, numberFormat);
        noiseLevel1.add(label1);
        noiseLevel1.add(df1);
        noiseLevel1.add(percent);
        noiseLevelPanel.add(noiseLevel1);
        JPanel noiseLevel2 = new JPanel();
        noiseLevel2.setLayout(new GridLayout(1, 3));
        JLabel label2 = new JLabel("Bending Dipole: ");
        percent = new JLabel("%");
        df2 = new DecimalField(0., 5, numberFormat);
        noiseLevel2.add(label2);
        noiseLevel2.add(df2);
        noiseLevel2.add(percent);
        noiseLevelPanel.add(noiseLevel2);
        JPanel noiseLevel3 = new JPanel();
        noiseLevel3.setLayout(new GridLayout(1, 3));
        df3 = new DecimalField(0., 5, numberFormat);
        noiseLevel3.add(new JLabel("Dipole Corr.: "));
        noiseLevel3.add(df3);
        noiseLevel3.add(new JLabel("%"));
        noiseLevelPanel.add(noiseLevel3);
        JPanel noiseLevel4 = new JPanel();
        noiseLevel4.setLayout(new GridLayout(1, 3));
        df4 = new DecimalField(0., 5, numberFormat);
        noiseLevel4.add(new JLabel("BPM: "));
        noiseLevel4.add(df4);
        noiseLevel4.add(new JLabel("%"));
        noiseLevelPanel.add(noiseLevel4);
        offsetPanel.setLayout(new GridLayout(7, 1));
        offsetPanel.add(new JLabel("Offset for Device Type:"));
        JPanel offset1 = new JPanel();
        offset1.setLayout(new GridLayout(1, 2));
        df11 = new DecimalField(0., 5, numberFormat);
        offset1.add(new JLabel("Quad: "));
        offset1.add(df11);
        offsetPanel.add(offset1);
        JPanel offset2 = new JPanel();
        offset2.setLayout(new GridLayout(1, 2));
        df21 = new DecimalField(0., 5, numberFormat);
        offset2.add(new JLabel("Bending Dipole: "));
        offset2.add(df21);
        offsetPanel.add(offset2);
        JPanel offset3 = new JPanel();
        offset3.setLayout(new GridLayout(1, 2));
        df31 = new DecimalField(0., 5, numberFormat);
        offset3.add(new JLabel("Dipole Corr.: "));
        offset3.add(df31);
        offsetPanel.add(offset3);
        JPanel offset4 = new JPanel();
        offset4.setLayout(new GridLayout(1, 2));
        df41 = new DecimalField(0., 5, numberFormat);
        offset4.add(new JLabel("BPM: "));
        offset4.add(df41);
        offsetPanel.add(offset4);
        setNoise.setBounds(300, 300, 300, 300);
        setNoise.setTitle("Set Noise Level...");
        settingPanel.setLayout(new BoxLayout(settingPanel, BoxLayout.Y_AXIS));
        settingPanel.add(noiseLevelPanel);
        settingPanel.add(offsetPanel);
        setNoise.getContentPane().setLayout(new BorderLayout());
        setNoise.getContentPane().add(settingPanel, BorderLayout.CENTER);
        setNoise.getContentPane().add(done, BorderLayout.SOUTH);
        done.setActionCommand("noiseSet");
        done.addActionListener(this);
        if (getSource() != null) {
            java.net.URL url = getSource();
            DataAdaptor documentAdaptor = XmlDataAdaptor.adaptorForUrl(url, false);
            update(documentAdaptor.childAdaptor("MpxDocument"));
            setHasChanges(false);
        }
        pcasPath = pcasPrefs.get("PCAS_PATH", "");
        setHasChanges(false);
    }

    /**
	 * Save the document to the specified URL.
	 * 
	 * @param url
	 *            The URL to which the document should be saved.
	 */
    @Override
    public void saveDocumentAs(URL url) {
        XmlDataAdaptor xda = XmlDataAdaptor.newEmptyDocumentAdaptor();
        DataAdaptor daLevel1 = xda.createChild("VA");
        DataAdaptor daXMLFile = daLevel1.createChild("accelerator");
        daXMLFile.setValue("xmlFile", this.getAcceleratorFilePath());
        if (theProbeFile != null) {
            DataAdaptor envProbeXMLFile = daLevel1.createChild("env_probe");
            envProbeXMLFile.setValue("probeXmlFile", theProbeFile);
        }
        if (pcasPath.equals("")) {
            setPcasPath();
        }
        DataAdaptor pcasScrptPath = daLevel1.createChild("pcas_path");
        pcasScrptPath.setValue("pcasScrptPath", pcasPath);
        ArrayList seqs;
        if (getSelectedSequence() != null) {
            DataAdaptor daSeq = daLevel1.createChild("sequences");
            daSeq.setValue("name", getSelectedSequence().getId());
            if (getSelectedSequence() instanceof AcceleratorSeqCombo) {
                AcceleratorSeqCombo asc = (AcceleratorSeqCombo) getSelectedSequence();
                seqs = (ArrayList) asc.getConstituentNames();
            } else {
                seqs = new ArrayList();
                seqs.add(getSelectedSequence().getId());
            }
            Iterator itr = seqs.iterator();
            while (itr.hasNext()) {
                DataAdaptor daSeqComponents = daSeq.createChild("seq");
                daSeqComponents.setValue("name", itr.next());
            }
        }
        xda.writeToUrl(url);
        setHasChanges(false);
    }

    /**
	 * Convenience method for getting the main window cast to the proper
	 * subclass of XalWindow. This allows me to avoid casting the window every
	 * time I reference it.
	 * 
	 * @return The main window cast to its dynamic runtime class
	 */
    private VAWindow myWindow() {
        return (VAWindow) mainWindow;
    }

    /**
	 * Instantiate a new PlainDocument that servers as the document for the text
	 * pane. Create a handler of text actions so we can determine if the
	 * document has changes that should be saved.
	 */
    private void makeTextDocument() {
        textDocument = new PlainDocument();
        textDocument.addDocumentListener(new DocumentListener() {

            public void changedUpdate(javax.swing.event.DocumentEvent evt) {
                setHasChanges(true);
            }

            public void removeUpdate(DocumentEvent evt) {
                setHasChanges(true);
            }

            public void insertUpdate(DocumentEvent evt) {
                setHasChanges(true);
            }
        });
    }

    /**
	 * Create the default probe from the edit context.
	 */
    private void createDefaultProbe() {
        if (selectedSequence != null) {
            if (selectedSequence instanceof gov.sns.xal.smf.Ring) {
                myProbe = ProbeFactory.getTransferMapProbe(selectedSequence, new TransferMapTracker());
            } else {
                myProbe = ProbeFactory.getEnvelopeProbe(selectedSequence, new EnvTrackerAdapt(selectedSequence));
            }
            model.setProbe(myProbe);
        }
    }

    @Override
    protected void customizeCommands(Commander commander) {
        Action openprobeAction = new AbstractAction() {

            public void actionPerformed(ActionEvent event) {
                String currentDirectory = _probeFileTracker.getRecentFolderPath();
                JFrame frame = new JFrame();
                JFileChooser fileChooser = new JFileChooser(currentDirectory);
                fileChooser.addChoosableFileFilter(new ProbeFileFilter());
                int status = fileChooser.showOpenDialog(frame);
                if (status == JFileChooser.APPROVE_OPTION) {
                    _probeFileTracker.cacheURL(fileChooser.getSelectedFile());
                    File file = fileChooser.getSelectedFile();
                    theProbeFile = file.getPath();
                    try {
                        myProbe = ProbeXmlParser.parse(theProbeFile);
                        model.setProbe(myProbe);
                    } catch (ParsingException e) {
                        System.out.println(e);
                    }
                }
            }
        };
        openprobeAction.putValue(Action.NAME, "openprobe");
        commander.registerAction(openprobeAction);
        Action probeEditorAction = new AbstractAction("probe-editor") {

            public void actionPerformed(ActionEvent event) {
                SimpleProbeEditor spe = new SimpleProbeEditor();
                if (model.getProbe() != null) {
                    model.resetProbe();
                    spe.createSimpleProbeEditor(model.getProbe());
                } else {
                    if (theProbeFile != null) {
                        spe.createSimpleProbeEditor(new File(theProbeFile));
                    } else {
                        spe.createSimpleProbeEditor();
                    }
                }
                if (spe.probeHasChanged()) {
                    myProbe = spe.getProbe();
                    model.setProbe(myProbe);
                }
            }
        };
        probeEditorAction.putValue(Action.NAME, "probe-editor");
        commander.registerAction(probeEditorAction);
        olmModel.setSelected(true);
        olmModel.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                isFromPVLogger = false;
            }
        });
        commander.registerModel("olm", olmModel);
        pvlogModel.setSelected(false);
        pvlogModel.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                isFromPVLogger = true;
                if (pvLogSelector == null) {
                    plsc = new PVLogSnapshotChooser();
                    pvLogSelector = plsc.choosePVLogId();
                } else pvLogSelector.setVisible(true);
            }
        });
        commander.registerModel("pvlogger", pvlogModel);
        Action runAction = new AbstractAction() {

            public void actionPerformed(ActionEvent event) {
                if (vaRunning) {
                    JFrame frame = new JFrame();
                    JOptionPane.showMessageDialog(frame, "Virtual Accelerator has already started.", "Warning!", JOptionPane.PLAIN_MESSAGE);
                    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    return;
                }
                if (getSelectedSequence() == null) {
                    JFrame frame = new JFrame();
                    JOptionPane.showMessageDialog(frame, "You need to select sequence(s) first.", "Warning!", JOptionPane.PLAIN_MESSAGE);
                    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                } else {
                    if (isFromPVLogger) {
                        long pvLoggerId = plsc.getPVLogId();
                        if (pcasPath.equals("")) setPcasPath();
                        if (pcas == null) {
                            pcas = new PCAS(pcasPath);
                            startPCAS();
                        } else {
                            pcas.restartPCAS();
                        }
                        vaRunning = true;
                        plds = new PVLoggerDataSource(pvLoggerId);
                        RunVAWithPVLogger runIt = new VADocument.RunVAWithPVLogger();
                        runIt.start();
                    } else {
                        if (theProbeFile == null && myProbe == null) {
                            createDefaultProbe();
                            if (myProbe == null) {
                                displayWarning("Warning!", "You need to select probe file first.");
                                return;
                            }
                            actionPerformed(event);
                        } else {
                            if (pcasPath.equals("")) setPcasPath();
                            if (pcas == null) {
                                pcas = new PCAS(pcasPath);
                                startPCAS();
                            } else {
                                pcas.restartPCAS();
                            }
                            vaRunning = true;
                            putSetPVs();
                            RunModel runModel = new RunModel();
                            runModel.start();
                        }
                    }
                }
            }
        };
        runAction.putValue(Action.NAME, "run-va");
        commander.registerAction(runAction);
        Action stopAction = new AbstractAction() {

            public void actionPerformed(ActionEvent event) {
                stopPCAS();
            }
        };
        stopAction.putValue(Action.NAME, "stop-va");
        commander.registerAction(stopAction);
        Action setNoiseAction = new AbstractAction() {

            public void actionPerformed(ActionEvent event) {
                setNoise.setVisible(true);
            }
        };
        setNoiseAction.putValue(Action.NAME, "set-noise");
        commander.registerAction(setNoiseAction);
    }

    @Override
    protected void willClose() {
        System.out.println("Document will be closed");
        if (pcas != null && pcas.vaIsRunning()) pcas.stopVA();
    }

    public void update(DataAdaptor adaptor) {
        if (getSource() != null) {
            XmlDataAdaptor xda = XmlDataAdaptor.adaptorForUrl(getSource(), false);
            DataAdaptor da1 = xda.childAdaptor("VA");
            this.setAcceleratorFilePath(da1.childAdaptor("accelerator").stringValue("xmlFile"));
            String accelUrl = "file://" + this.getAcceleratorFilePath();
            try {
                XMLDataManager dMgr = new XMLDataManager(accelUrl);
                this.setAccelerator(dMgr.getAccelerator(), this.getAcceleratorFilePath());
            } catch (Exception exception) {
                JOptionPane.showMessageDialog(null, "Hey - I had trouble parsing the accelerator input xml file you fed me", "VA error", JOptionPane.ERROR_MESSAGE);
            }
            this.acceleratorChanged();
            List temp = da1.childAdaptors("sequences");
            if (temp.isEmpty()) return;
            ArrayList seqs = new ArrayList();
            List selectedSeqList = null;
            DataAdaptor da2a = da1.childAdaptor("sequences");
            String seqName = da2a.stringValue("name");
            temp = da2a.childAdaptors("seq");
            Iterator itr = temp.iterator();
            while (itr.hasNext()) {
                DataAdaptor da = (DataAdaptor) itr.next();
                seqs.add(getAccelerator().getSequence(da.stringValue("name")));
            }
            if (seqName.equals("Ring")) setSelectedSequence(new Ring(seqName, seqs)); else setSelectedSequence(new AcceleratorSeqCombo(seqName, seqs));
            setSelectedSequenceList(seqs.subList(0, seqs.size()));
            if (da1.hasAttribute("env_probe")) {
                DataAdaptor probeFile = da1.childAdaptor("env_probe");
                theProbeFile = probeFile.stringValue("probeXmlFile");
                if (theProbeFile.length() > 1) {
                    try {
                        myProbe = ProbeXmlParser.parse(theProbeFile);
                    } catch (ParsingException e) {
                        createDefaultProbe();
                    }
                }
            } else {
                createDefaultProbe();
            }
            model.setProbe(myProbe);
        }
    }

    public static String getPCASFileName() {
        return pcasFileName;
    }

    protected Scenario getScenario() {
        return model;
    }

    protected boolean isVARunning() {
        return vaRunning;
    }

    /**
	 * This method is for populating the readback PVs
	 */
    private void putReadbackPVs() {
        try {
            beamOnEvent.putVal(0);
            beamOnEventCounter++;
            beamOnEventCount.putVal(beamOnEventCounter);
        } catch (ConnectionException e) {
            System.out.println(e);
        } catch (PutException e) {
            System.out.println(e);
        }
        Iterator it = rb_setMap.keySet().iterator();
        while (it.hasNext()) {
            Channel rbPV = (Channel) it.next();
            Channel setPV = (Channel) rb_setMap.get(rbPV);
            try {
                double noise = ((Double) ch_noiseMap.get(rbPV)).doubleValue();
                double offset = ((Double) ch_offsetMap.get(rbPV)).doubleValue();
                System.out.println("Ready to put " + NoiseGenerator.setValForPV(setPV.getValDbl(), noise, offset) + " to " + rbPV.getId());
                rbPV.putVal(NoiseGenerator.setValForPV(setPV.getValDbl(), noise, offset));
            } catch (ConnectionException e) {
                System.out.println(e.getMessage());
            } catch (GetException e) {
                System.out.println(e.getMessage());
            } catch (PutException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private void putReadbackPVsFromPVLogger() {
        HashMap qPVMap = plds.getQuadMap();
        try {
            beamOnEvent.putVal(0);
            beamOnEventCounter++;
            beamOnEventCount.putVal(beamOnEventCounter);
        } catch (ConnectionException e) {
            System.out.println(e);
        } catch (PutException e) {
            System.out.println(e);
        }
        Iterator it = rb_setMap.keySet().iterator();
        while (it.hasNext()) {
            Channel rbPV = (Channel) it.next();
            try {
                double noise = ((Double) ch_noiseMap.get(rbPV)).doubleValue();
                double offset = ((Double) ch_offsetMap.get(rbPV)).doubleValue();
                if (qPVMap.containsKey(rbPV.getId())) rbPV.putVal(NoiseGenerator.setValForPV(((Double) qPVMap.get(rbPV.getId())).doubleValue(), noise, offset));
            } catch (ConnectionException e) {
                System.out.println(e.getMessage());
            } catch (PutException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    /**
	 * populate all the "set" PV values from design values
	 */
    private void putSetPVs() {
        Iterator iAllMag = mags.iterator();
        while (iAllMag.hasNext()) {
            Electromagnet em = (Electromagnet) iAllMag.next();
            try {
                Channel ch = em.getMainSupply().getAndConnectChannel(MagnetMainSupply.FIELD_SET_HANDLE);
                Channel.setSyncRequest(false);
                System.out.println("Ready to put " + Math.abs(em.getDfltField()) + " to " + ch.getId());
                ch.putValCallback(Math.abs(em.getDfltField()), this);
            } catch (NoSuchChannelException e) {
                System.out.println(e.getMessage());
            } catch (ConnectionException e) {
                System.out.println(e.getMessage());
            } catch (PutException e) {
                System.out.println(e.getMessage());
            }
        }
        Iterator irf = rfCavities.iterator();
        while (irf.hasNext()) {
            RfCavity rfCavity = (RfCavity) irf.next();
            try {
                Channel ampSetCh = rfCavity.getAndConnectChannel(RfCavity.CAV_AMP_SET_HANDLE);
                System.out.println("Ready to put " + rfCavity.getDfltCavAmp() + " to " + ampSetCh.getId());
                ampSetCh.putVal(rfCavity.getDfltCavAmp());
                Channel phaseSetCh = rfCavity.getAndConnectChannel(RfCavity.CAV_PHASE_SET_HANDLE);
                System.out.println("Ready to put " + rfCavity.getDfltCavPhase() + " to " + phaseSetCh.getId());
                phaseSetCh.putVal(rfCavity.getDfltCavPhase());
            } catch (NoSuchChannelException e) {
                System.out.println(e.getMessage());
            } catch (ConnectionException e) {
                System.out.println(e.getMessage());
            } catch (PutException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private void putSetPVsFromPVLogger() {
        Iterator iAllMag = mags.iterator();
        HashMap qPSPVMap = plds.getQuadPSMap();
        while (iAllMag.hasNext()) {
            Electromagnet em = (Electromagnet) iAllMag.next();
            try {
                Channel ch = em.getMainSupply().getAndConnectChannel(MagnetMainSupply.FIELD_SET_HANDLE);
                Channel.setSyncRequest(false);
                System.out.println("Ready to put " + Math.abs(em.getDfltField()) + " to " + ch.getId());
                if (qPSPVMap.containsKey(ch.getId())) ch.putValCallback(((Double) qPSPVMap.get(ch.getId())).doubleValue(), this);
            } catch (NoSuchChannelException e) {
                System.out.println(e.getMessage());
            } catch (ConnectionException e) {
                System.out.println(e.getMessage());
            } catch (PutException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    /**
	 * This method is for populating the diagnostic PVs (only BPMs + WSs for
	 * now)
	 */
    protected void putDiagPVs() {
        Iterator ibpm = bpms.iterator();
        while (ibpm.hasNext()) {
            BPM bpm = (BPM) ibpm.next();
            Channel bpmX = bpm.getChannel(BPM.X_AVG_HANDLE);
            Channel bpmY = bpm.getChannel(BPM.Y_AVG_HANDLE);
            Channel bpmAmp = bpm.getChannel(BPM.AMP_AVG_HANDLE);
            Channel bpmPhase = bpm.getChannel(BPM.PHASE_AVG_HANDLE);
            try {
                ProbeState probeState = model.getTrajectory().stateForElement(bpm.getId());
                System.out.println("Now updating " + bpm.getId());
                if (probeState instanceof ICoordinateState) {
                    final PhaseVector coordinates = ((ICoordinateState) probeState).getFixedOrbit();
                    bpmX.putVal(NoiseGenerator.setValForPV(coordinates.getx() * 1000., bpmNoise, bpmOffset));
                    bpmY.putVal(NoiseGenerator.setValForPV(coordinates.gety() * 1000., bpmNoise, bpmOffset));
                }
                bpmAmp.putVal(NoiseGenerator.setValForPV(20., 5., 0.1));
                bpmPhase.putVal(probeState.getTime() * 360. * (((BPMBucket) bpm.getBucket("bpm")).getFrequency() * 1.e6) % 360.0);
            } catch (ConnectionException e) {
                System.out.println(e.getMessage());
            } catch (PutException e) {
                System.out.println(e.getMessage());
            }
        }
        Iterator iws = wss.iterator();
        while (iws.hasNext()) {
            ProfileMonitor ws = (ProfileMonitor) iws.next();
            Channel wsX = ws.getChannel(ProfileMonitor.H_SIGMA_M_HANDLE);
            Channel wsY = ws.getChannel(ProfileMonitor.V_SIGMA_M_HANDLE);
            try {
                ProbeState probeState = model.getTrajectory().stateForElement(ws.getId());
                System.out.println("Now updating " + ws.getId());
                if (model.getProbe() instanceof EnvelopeProbe) {
                    wsX.putVal(((EnvelopeProbeState) probeState).getCorrelationMatrix().getSigmaX() * 1000.);
                    wsY.putVal(((EnvelopeProbeState) probeState).getCorrelationMatrix().getSigmaY() * 1000.);
                }
            } catch (ConnectionException e) {
                System.out.println(e.getMessage());
            } catch (PutException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private void putDiagPVsFromPVLogger() {
        HashMap bpmXMap = plds.getBPMXMap();
        HashMap bpmYMap = plds.getBPMYMap();
        HashMap bpmAmpMap = plds.getBPMAmpMap();
        HashMap bpmPhaseMap = plds.getBPMPhaseMap();
        Iterator ibpm = bpms.iterator();
        while (ibpm.hasNext()) {
            BPM bpm = (BPM) ibpm.next();
            Channel bpmX = bpm.getChannel(BPM.X_AVG_HANDLE);
            Channel bpmY = bpm.getChannel(BPM.Y_AVG_HANDLE);
            Channel bpmAmp = bpm.getChannel(BPM.AMP_AVG_HANDLE);
            Channel bpmPhase = bpm.getChannel(BPM.PHASE_AVG_HANDLE);
            try {
                System.out.println("Now updating " + bpm.getId());
                if (bpmXMap.containsKey(bpmX.getId())) bpmX.putVal(NoiseGenerator.setValForPV(((Double) bpmXMap.get(bpmX.getId())).doubleValue(), bpmNoise, bpmOffset));
                if (bpmYMap.containsKey(bpmY.getId())) bpmY.putVal(NoiseGenerator.setValForPV(((Double) bpmYMap.get(bpmY.getId())).doubleValue(), bpmNoise, bpmOffset));
                if (bpmAmpMap.containsKey(bpmAmp.getId())) bpmAmp.putVal(NoiseGenerator.setValForPV(((Double) bpmAmpMap.get(bpmAmp.getId())).doubleValue(), 5., 0.1));
                if (bpmPhaseMap.containsKey(bpmPhase.getId())) bpmPhase.putVal(((Double) bpmPhaseMap.get(bpmPhase.getId())).doubleValue());
            } catch (ConnectionException e) {
                System.out.println(e.getMessage());
            } catch (PutException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    public void putCompleted(Channel chan) {
    }

    /**
	 * create the map between the "readback" and "set" PVs
	 */
    private void rb_set() {
        rb_setMap = new LinkedHashMap();
        ch_noiseMap = new LinkedHashMap();
        ch_offsetMap = new LinkedHashMap();
        Iterator iAllMag = mags.iterator();
        while (iAllMag.hasNext()) {
            Electromagnet em = (Electromagnet) iAllMag.next();
            rb_setMap.put(em.getChannel(Electromagnet.FIELD_RB_HANDLE), em.getMainSupply().getChannel(MagnetMainSupply.FIELD_SET_HANDLE));
            if (em.getType().equals("QH") || em.getType().equals("QV") || em.getType().equals("QTH") || em.getType().equals("QTV") || em.getType().equals("PMQH") || em.getType().equals("PMQV")) {
                ch_noiseMap.put(em.getChannel(Electromagnet.FIELD_RB_HANDLE), new Double(quadNoise));
                ch_offsetMap.put(em.getChannel(Electromagnet.FIELD_RB_HANDLE), new Double(quadOffset));
            } else if (em.getType().equals("DH")) {
                ch_noiseMap.put(em.getChannel(Electromagnet.FIELD_RB_HANDLE), new Double(dipoleNoise));
                ch_offsetMap.put(em.getChannel(Electromagnet.FIELD_RB_HANDLE), new Double(dipoleOffset));
            } else if (em.getType().equals("DCH") || em.getType().equals("DCV")) {
                ch_noiseMap.put(em.getChannel(Electromagnet.FIELD_RB_HANDLE), new Double(correctorNoise));
                ch_offsetMap.put(em.getChannel(Electromagnet.FIELD_RB_HANDLE), new Double(correctorOffset));
            }
        }
        Iterator iRfCavity = rfCavities.iterator();
        while (iRfCavity.hasNext()) {
            RfCavity rfCav = (RfCavity) iRfCavity.next();
            rb_setMap.put(rfCav.getChannel(RfCavity.CAV_AMP_AVG_HANDLE), rfCav.getChannel(RfCavity.CAV_AMP_SET_HANDLE));
            rb_setMap.put(rfCav.getChannel(RfCavity.CAV_PHASE_AVG_HANDLE), rfCav.getChannel(RfCavity.CAV_PHASE_SET_HANDLE));
            ch_noiseMap.put(rfCav.getChannel(RfCavity.CAV_AMP_AVG_HANDLE), new Double(rfAmpNoise));
            ch_noiseMap.put(rfCav.getChannel(RfCavity.CAV_PHASE_AVG_HANDLE), new Double(rfPhaseNoise));
            ch_offsetMap.put(rfCav.getChannel(RfCavity.CAV_AMP_AVG_HANDLE), new Double(rfAmpOffset));
            ch_offsetMap.put(rfCav.getChannel(RfCavity.CAV_PHASE_AVG_HANDLE), new Double(rfPhaseOffset));
        }
    }

    public void setPcasPath() {
        JFrame frame = new JFrame();
        JPanel message = new JPanel();
        message.setLayout(new BorderLayout());
        JLabel messageText = new JLabel("Please enter the path to the PCAS launch script first.");
        final JTextField pcasPathField = new JTextField();
        JButton browse = new JButton("Browse");
        browse.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                JFrame frame1 = new JFrame();
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.addChoosableFileFilter(new ExeFileFilter());
                int status = fileChooser.showOpenDialog(frame1);
                if (status == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile();
                    pcasPath = file.getPath();
                    pcasPathField.setText(pcasPath);
                    pcasPrefs.put("PCAS_PATH", pcasPath);
                    try {
                        pcasPrefs.sync();
                    } catch (BackingStoreException e) {
                        System.out.println(e);
                    }
                }
            }
        });
        message.add(messageText, BorderLayout.NORTH);
        message.add(pcasPathField, BorderLayout.CENTER);
        message.add(browse, BorderLayout.EAST);
        JOptionPane.showMessageDialog(frame, message, "Warning!", JOptionPane.PLAIN_MESSAGE);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    /**
	 * start the Portable Channel Access Server
	 */
    private void startPCAS() {
        pcas.start();
    }

    /**
	 * stop the Portable Channel Access Server
	 */
    private void stopPCAS() {
        vaRunning = false;
    }

    public PCAS getPCAS() {
        return pcas;
    }

    @Override
    public void acceleratorChanged() {
        if (accelerator != null) {
            stopPCAS();
            StringBuffer description = new StringBuffer("Selected Accelerator: " + accelerator.getId() + '\n');
            description.append("Sequences:\n");
            Iterator sequenceIter = accelerator.getSequences().iterator();
            while (sequenceIter.hasNext()) {
                AcceleratorSeq sequence = (AcceleratorSeq) sequenceIter.next();
                description.append('\t' + sequence.getId() + '\n');
            }
            beamOnEvent = accelerator.getTimingCenter().getChannel(TimingCenter.BEAM_ON_EVENT_HANDLE);
            beamOnEventCount = accelerator.getTimingCenter().getChannel(TimingCenter.BEAM_ON_EVENT_COUNT_HANDLE);
            setHasChanges(true);
        }
    }

    @Override
    public void selectedSequenceChanged() {
        if (selectedSequence != null) {
            StringBuffer description = new StringBuffer("Selected Sequence: " + selectedSequence.getId() + '\n');
            description.append("Nodes:\n");
            Iterator nodeIter = selectedSequence.getNodes().iterator();
            while (nodeIter.hasNext()) {
                AcceleratorNode node = (AcceleratorNode) nodeIter.next();
                description.append('\t' + node.getId() + '\n');
            }
            PCASGenerator pcasFile = new PCASGenerator(getSelectedSequence());
            pcasFileName = "server_" + getSelectedSequence().getId() + "_pvs";
            try {
                FileWriter file = new FileWriter(new File(pcasFileName));
                pcasFile.processNodes(file);
                file.close();
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
            OrTypeQualifier typeQualifier = new OrTypeQualifier();
            typeQualifier.or("QH");
            typeQualifier.or("QV");
            typeQualifier.or("QTH");
            typeQualifier.or("QTV");
            typeQualifier.or("DCH");
            typeQualifier.or("DCV");
            typeQualifier.or("DH");
            typeQualifier.or("PMQH");
            typeQualifier.or("PMQV");
            mags = getSelectedSequence().getAllNodesWithQualifier(typeQualifier);
            typeQualifier = new OrTypeQualifier();
            typeQualifier.or("Bnch");
            typeQualifier.or("DTLTank");
            typeQualifier.or("CCL");
            typeQualifier.or("SCLCavity");
            rfCavities = getSelectedSequence().getAllInclusiveNodesWithQualifier(typeQualifier);
            bpms = getSelectedSequence().getAllNodesOfType("BPM");
            wss = getSelectedSequence().getAllNodesOfType("WS");
            rb_set();
            try {
                model = Scenario.newScenarioFor(getSelectedSequence());
            } catch (ModelException e) {
                System.out.println(e.getMessage());
            }
            try {
                if (selectedSequence instanceof Ring) {
                    myProbe = ProbeFactory.getTransferMapProbe(selectedSequence, new TransferMapTracker());
                    TransferMapState state = (TransferMapState) myProbe.createProbeState();
                    state.setPhaseCoordinates(new PhaseVector(0.01, 0., 0.01, 0., 0., 0.));
                    myProbe.applyState(state);
                } else myProbe = ProbeFactory.getEnvelopeProbe(selectedSequence, new EnvTrackerAdapt(selectedSequence));
            } catch (NullPointerException e) {
                System.out.println("There is no default probe for this sequence.");
            }
            if (myProbe != null) model.setProbe(myProbe);
            setHasChanges(true);
        }
    }

    public void buildOnlineModel() {
        try {
            model.setSynchronizationMode(Scenario.SYNC_MODE_LIVE);
            model.resync();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void actionPerformed(ActionEvent ev) {
        if (ev.getActionCommand().equals("noiseSet")) {
            quadNoise = df1.getValue();
            dipoleNoise = df2.getValue();
            correctorNoise = df3.getValue();
            bpmNoise = df4.getValue();
            quadOffset = df11.getValue();
            dipoleOffset = df21.getValue();
            correctorOffset = df31.getValue();
            bpmOffset = df41.getValue();
            setNoise.setVisible(false);
        }
    }

    class RunModel extends Thread implements Runnable {

        @Override
        public void run() {
            for (; ; ) {
                try {
                    awaitTurn();
                    putReadbackPVs();
                    buildOnlineModel();
                    try {
                        myProbe.reset();
                        model.run();
                        putDiagPVs();
                    } catch (ModelException e) {
                        System.out.println(e.getMessage());
                    }
                    System.gc();
                } catch (InterruptedException e) {
                    System.out.println(e);
                }
            }
        }

        synchronized void awaitTurn() throws InterruptedException {
            while (vaRunning == false) wait();
        }
    }

    class RunVAWithPVLogger extends Thread implements Runnable {

        @Override
        public void run() {
            Thread thisThread = Thread.currentThread();
            while (vaRunning) {
                try {
                    putSetPVsFromPVLogger();
                    putReadbackPVsFromPVLogger();
                    putDiagPVsFromPVLogger();
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                }
            }
        }
    }
}

class ProbeFileFilter extends javax.swing.filechooser.FileFilter {

    @Override
    public boolean accept(File f) {
        if (f.isDirectory()) {
            return true;
        }
        String extension = Utils.getExtension(f);
        if (extension != null) {
            if (extension.equals(Utils.probe)) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    @Override
    public String getDescription() {
        return "Probe File";
    }
}

class ExeFileFilter extends javax.swing.filechooser.FileFilter {

    @Override
    public boolean accept(File f) {
        if (f.isDirectory()) {
            return true;
        }
        String extension = Utils.getExtension(f);
        if (extension != null) {
            if (extension.equals(Utils.exe)) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    @Override
    public String getDescription() {
        return "PCAS Script File";
    }
}

class Utils {

    public static final String probe = "probe";

    public static final String exe = "exe";

    /**
	 * Get the extension of a file.
	 */
    public static String getExtension(File f) {
        String ext = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');
        if (i > 0 && i < s.length() - 1) {
            ext = s.substring(i + 1).toLowerCase();
        }
        return ext;
    }
}
