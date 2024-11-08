package gov.sns.apps.orbitdifference;

import java.util.*;
import java.net.*;
import java.io.*;
import javax.swing.*;
import javax.swing.text.*;
import java.awt.event.*;
import javax.swing.event.*;
import gov.sns.xal.smf.application.*;
import gov.sns.application.*;
import gov.sns.ca.*;
import gov.sns.xal.smf.*;
import gov.sns.xal.smf.data.*;
import gov.sns.xal.slg.*;
import gov.sns.xal.tools.extlatgen.*;
import gov.sns.tools.data.EditContext;
import gov.sns.xal.model.*;
import gov.sns.xal.model.probe.*;
import gov.sns.xal.model.probe.traj.EnvelopeProbeState;
import gov.sns.xal.model.probe.traj.ProbeState;
import gov.sns.xal.model.xml.*;
import gov.sns.xal.model.mpx.ModelProxy;
import gov.sns.xal.model.scenario.Scenario;
import gov.sns.xal.model.alg.*;
import gov.sns.xal.smf.impl.*;
import gov.sns.xal.smf.impl.qualify.OrTypeQualifier;
import gov.sns.tools.apputils.SimpleProbeEditor;
import gov.sns.tools.scan.SecondEdition.BeamTrigger;
import gov.sns.tools.xml.*;
import gov.sns.tools.data.*;
import gov.sns.tools.beam.TraceXalUnitConverter;

/**
 * OrbitDiffDocument is a custom AcceleratorDocument for orbit difference 
 * application.  
 *
 * @version   1.5  27 Feb 2004
 * @author  Paul Chu
 */
public class OrbitDiffDocument extends AcceleratorDocument {

    /**
     * The document for the text pane in the main window.
     */
    protected PlainDocument textDocument;

    /** For on-line model */
    protected ModelProxy model;

    protected gov.sns.xal.slg.Lattice lattice;

    private EnvelopeProbe myProbe;

    String dataSource = Scenario.SYNC_MODE_RF_DESIGN;

    protected String theProbeFile;

    protected boolean runable = false;

    int runT3d_OK = 0;

    double tmpData1[] = new double[1000];

    double tmpData2[] = new double[1000];

    double tmpData3[] = new double[1000];

    double tmpData4[] = new double[1000];

    double tmpData5[] = new double[1000];

    double tmpData6[] = new double[1000];

    /** Create a new empty document */
    public OrbitDiffDocument() {
        this(null);
    }

    /** 
     * Create a new document loaded from the URL file 
     * @param url The URL of the file to load into the new document.
     */
    public OrbitDiffDocument(java.net.URL url) {
        setSource(url);
        makeTextDocument();
        model = new ModelProxy();
        if (url == null) return;
    }

    /**
     * Make a main window by instantiating the my custom window.  Set the text 
     * pane to use the textDocument variable as its document.
     */
    @Override
    public void makeMainWindow() {
        mainWindow = new OrbitDiffWindow(this);
        if (getSource() != null) {
            XmlDataAdaptor xda = XmlDataAdaptor.adaptorForUrl(getSource(), false);
            DataAdaptor da1 = xda.childAdaptor("orbitdifference");
            this.setAcceleratorFilePath(da1.childAdaptor("accelerator").stringValue("xmlFile"));
            String accelUrl = "file://" + this.getAcceleratorFilePath();
            try {
                XMLDataManager dMgr = new XMLDataManager(accelUrl);
                this.setAccelerator(dMgr.getAccelerator(), this.getAcceleratorFilePath());
            } catch (Exception exception) {
                JOptionPane.showMessageDialog(null, "Hey - I had trouble parsing the accelerator input xml file you fed me", "OrbitDiff error", JOptionPane.ERROR_MESSAGE);
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
            setSelectedSequence(new AcceleratorSeqCombo(seqName, seqs));
            setSelectedSequenceList(seqs.subList(0, seqs.size()));
            DataAdaptor probeFile = da1.childAdaptor("env_probe");
            theProbeFile = probeFile.stringValue("probeXmlFile");
            try {
                myProbe = (EnvelopeProbe) ProbeXmlParser.parse(theProbeFile);
            } catch (ParsingException e) {
            }
            model = new ModelProxy();
            model.setNewProbe(myProbe);
            lattice = new gov.sns.xal.slg.Lattice(seqName);
            try {
                model.setAcceleratorSeq(getSelectedSequence());
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
        setHasChanges(false);
    }

    /**
     * Save the document to the specified URL.
     * @url The URL to which the document should be saved.
     */
    @Override
    public void saveDocumentAs(URL url) {
        XmlDataAdaptor xda = XmlDataAdaptor.newEmptyDocumentAdaptor();
        DataAdaptor daLevel1 = xda.createChild("orbitdifference");
        DataAdaptor daXMLFile = daLevel1.createChild("accelerator");
        daXMLFile.setValue("xmlFile", this.getAcceleratorFilePath());
        DataAdaptor envProbeXMLFile = daLevel1.createChild("env_probe");
        envProbeXMLFile.setValue("probeXmlFile", theProbeFile);
        ArrayList seqs;
        if (getSelectedSequence() != null) {
            DataAdaptor daSeq = daLevel1.createChild("sequences");
            daSeq.setValue("name", getSelectedSequence().getId());
            if (getSelectedSequence().getClass() == AcceleratorSeqCombo.class) {
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
    }

    /**
     * Convenience method for getting the main window cast to the proper subclass of XalWindow.
     * This allows me to avoid casting the window every time I reference it.
     * @return The main window cast to its dynamic runtime class
     */
    private OrbitDiffWindow myWindow() {
        return (OrbitDiffWindow) mainWindow;
    }

    /** 
     * Instantiate a new PlainDocument that servers as the document for the text pane.
     * Create a handler of text actions so we can determine if the document has 
     * changes that should be saved.
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

    @Override
    protected void customizeCommands(Commander commander) {
        Action openprobeAction = new AbstractAction() {

            public void actionPerformed(ActionEvent event) {
                JFrame frame = new JFrame();
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.addChoosableFileFilter(new ProbeFileFilter());
                int status = fileChooser.showOpenDialog(frame);
                if (status == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile();
                    theProbeFile = "file://" + file.getPath();
                    try {
                        myProbe = (EnvelopeProbe) ProbeXmlParser.parse(theProbeFile);
                        model.setNewProbe(myProbe);
                    } catch (ParsingException e) {
                    }
                }
            }
        };
        openprobeAction.putValue(Action.NAME, "openprobe");
        commander.registerAction(openprobeAction);
        Action probeEditorAction = new AbstractAction("probe-editor") {

            public void actionPerformed(ActionEvent event) {
                SimpleProbeEditor spe = new SimpleProbeEditor();
                if (model.hasProbe()) {
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
                    myProbe = (EnvelopeProbe) spe.getProbe();
                    model.setNewProbe(myProbe);
                }
            }
        };
        commander.registerAction(probeEditorAction);
        Action export1Action = new AbstractAction() {

            public void actionPerformed(ActionEvent event) {
                if (getSelectedSequence() == null) {
                    JFrame frame = new JFrame();
                    JOptionPane.showMessageDialog(frame, "You need to select sequence(s) first.", "Warning!", JOptionPane.PLAIN_MESSAGE);
                    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                } else if (theProbeFile == null && myProbe == null) {
                    JFrame frame = new JFrame();
                    JOptionPane.showMessageDialog(frame, "You need to select probe file first.", "Warning!", JOptionPane.PLAIN_MESSAGE);
                    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                } else {
                    runable = false;
                    runModel();
                }
            }
        };
        export1Action.putValue(Action.NAME, "run-model");
        commander.registerAction(export1Action);
    }

    @Override
    public void acceleratorChanged() {
        if (accelerator != null) {
            StringBuffer description = new StringBuffer("Selected Accelerator: " + accelerator.getId() + '\n');
            description.append("Sequences:\n");
            Iterator sequenceIter = accelerator.getSequences().iterator();
            while (sequenceIter.hasNext()) {
                AcceleratorSeq sequence = (AcceleratorSeq) sequenceIter.next();
                description.append('\t' + sequence.getId() + '\n');
            }
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
            try {
                model.setAcceleratorSeq(getSelectedSequence());
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
            lattice = new gov.sns.xal.slg.Lattice("newSeq");
            myProbe = ProbeFactory.getEnvelopeProbe(getSelectedSequence(), new EnvTrackerAdapt(selectedSequence));
            model.setNewProbe(myProbe);
            setHasChanges(true);
        }
    }

    public void runModel() {
        model.resetProbe();
        myProbe = (EnvelopeProbe) model.getProbe();
        try {
            buildT3d();
        } catch (IOException e) {
            System.out.println(e);
        }
        buildOnlineModel();
        runTrace();
        updatePlot();
    }

    /** 
   * Call T3dBuilder method to build Trace 3D input file.
   * Also create and run on-line model here.
   */
    public void buildT3d() throws IOException {
        createT3dLattice();
        T3dGenerator t3dGenerator = new T3dGenerator(lattice, myProbe);
        try {
            t3dGenerator.createT3dInput(dataSource);
        } catch (IOException e) {
        }
    }

    public void createT3dLattice() {
        LatticeFactory factory = new LatticeFactory();
        factory.setDebug(false);
        factory.setVerbose(false);
        factory.setHalfMag(true);
        try {
            lattice = factory.getLattice(getSelectedSequence());
            lattice.clearMarkers();
            lattice.joinDrifts();
        } catch (LatticeError lerr) {
            System.out.println(lerr.getMessage());
        }
    }

    public void buildOnlineModel() {
        try {
            model.setChannelSource(dataSource);
            model.synchronizeAcceleratorSeq();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        runable = true;
    }

    /**
 * run Trace 3D through here
 */
    public synchronized void runTrace() {
        while (runable == false) {
            try {
                wait();
            } catch (InterruptedException e) {
                System.out.println(e);
            }
        }
        initialize();
        runT3d_OK = trace3d(getSelectedSequence().getId());
        try {
            model.runModel();
        } catch (ModelException me) {
            System.out.println(me);
        }
        notifyAll();
    }

    /**
 * native language (Fortran) trace3d defined here.  
 * The arguement "seqName" is the sequence name, e.g. "MEBT" 
 */
    public native synchronized int trace3d(String seqName);

    /**
 * call the library "libt3d.so" here 
 */
    public static boolean initialize() {
        System.loadLibrary("t3d");
        return true;
    }

    /** 
 * update data and plots 
 */
    public void updatePlot() {
        String strUrl1 = "./twiss.xml";
        EditContext editContext = new EditContext();
        try {
            editContext = getAccelerator().editContext();
            XMLDataManager.readTableGroupFromUrl(editContext, "twiss", strUrl1);
        } catch (XmlDataAdaptor.ParseException e) {
            System.out.println("Exception - " + e.getMessage());
        }
        java.util.List collect = getSelectedSequence().getAllNodes();
        int devCount = collect.size();
        OrTypeQualifier typeQualifier = new OrTypeQualifier();
        typeQualifier.or("QH");
        typeQualifier.or("QV");
        typeQualifier.or("PMQH");
        typeQualifier.or("PMQV");
        typeQualifier.or("DCH");
        typeQualifier.or("DCV");
        java.util.List devices = getSelectedSequence().getNodesWithQualifier(typeQualifier);
        int devCountNoDiag = devices.size();
        double xDatatl[][] = new double[3][devCountNoDiag];
        double xDatatr[][] = new double[3][devCountNoDiag];
        double xDatabl[][] = new double[3][devCountNoDiag];
        double xDatabr[][] = new double[3][devCountNoDiag];
        double yDatatl[][] = new double[3][devCountNoDiag];
        double yDatatr[][] = new double[3][devCountNoDiag];
        double yDatabl[][] = new double[3][devCountNoDiag];
        double yDatabr[][] = new double[3][devCountNoDiag];
        java.util.List collect1 = getSelectedSequence().getAllNodesOfType("BPM");
        ArrayList badBPMs = new ArrayList();
        for (int ii = 0; ii < collect1.size(); ii++) {
            BPM bpm = (BPM) collect1.get(ii);
            if (bpm.getStatus() == false) badBPMs.add(bpm);
        }
        for (int ii = 0; ii < badBPMs.size(); ii++) {
            collect1.remove(badBPMs.get(ii));
        }
        int devBPMCount = collect1.size();
        double xDatabl1[][] = new double[2][devBPMCount];
        double xDatabr1[][] = new double[2][devBPMCount];
        double yDatabl1[][] = new double[2][devBPMCount];
        double yDatabr1[][] = new double[2][devBPMCount];
        double delX[] = new double[devBPMCount];
        double delY[] = new double[devBPMCount];
        int i, j;
        j = 0;
        for (i = 0; i < devCount; i++) {
            AcceleratorNode obj = (AcceleratorNode) collect.get(i);
            if (obj.getType().equals("QH") || obj.getType().equals("QV") || obj.getType().equals("PMQH") || obj.getType().equals("PMQV") || obj.getType().equals("DCH") || obj.getType().equals("DCV")) {
                xDatatl[1][j] = getSelectedSequence().getPosition(obj);
                yDatatl[1][j] = (editContext.getTable("twiss").record("id", obj.getId()).doubleValueForKey("x") - tmpData1[j]);
                tmpData1[j] = editContext.getTable("twiss").record("id", obj.getId()).doubleValueForKey("x");
                xDatabl[1][j] = getSelectedSequence().getPosition(obj);
                yDatabl[1][j] = tmpData1[j];
                xDatatr[1][j] = getSelectedSequence().getPosition(obj);
                yDatatr[1][j] = editContext.getTable("twiss").record("id", obj.getId()).doubleValueForKey("y") - tmpData2[j];
                tmpData2[j] = editContext.getTable("twiss").record("id", obj.getId()).doubleValueForKey("y");
                xDatabr[1][j] = getSelectedSequence().getPosition(obj);
                yDatabr[1][j] = tmpData2[j];
                try {
                    ProbeState probeState = model.stateForElement(obj.getId());
                    TraceXalUnitConverter uc = TraceXalUnitConverter.newConverter(402500000., probeState.getSpeciesRestEnergy(), probeState.getKineticEnergy());
                    xDatatl[2][j] = getSelectedSequence().getPosition(obj);
                    yDatatl[2][j] = uc.xalToTraceCoordinates(((EnvelopeProbeState) probeState).phaseMean()).getx() - tmpData5[j];
                    tmpData5[j] = uc.xalToTraceCoordinates(((EnvelopeProbeState) probeState).phaseMean()).getx();
                    xDatabl[2][j] = getSelectedSequence().getPosition(obj);
                    yDatabl[2][j] = tmpData5[j];
                    xDatatr[2][j] = getSelectedSequence().getPosition(obj);
                    yDatatr[2][j] = uc.xalToTraceCoordinates(((EnvelopeProbeState) probeState).phaseMean()).gety() - tmpData6[j];
                    tmpData6[j] = uc.xalToTraceCoordinates(((EnvelopeProbeState) probeState).phaseMean()).gety();
                    xDatabr[2][j] = getSelectedSequence().getPosition(obj);
                    yDatabr[2][j] = tmpData6[j];
                } catch (ModelException me) {
                    System.out.println(me.getMessage());
                }
                j = j + 1;
            }
        }
        BPM bpmObj[] = new BPM[devBPMCount];
        Channel bpmXCh[] = new Channel[devBPMCount];
        Channel bpmYCh[] = new Channel[devBPMCount];
        double XtempBPMData[] = new double[devBPMCount];
        double YtempBPMData[] = new double[devBPMCount];
        for (int ijk = 0; ijk < devBPMCount; ijk++) {
            XtempBPMData[ijk] = 0.0;
            YtempBPMData[ijk] = 0.0;
            bpmObj[ijk] = (BPM) ((AcceleratorNode) collect1.get(ijk));
            bpmXCh[ijk] = bpmObj[ijk].getChannel(BPM.X_AVG_HANDLE);
            bpmYCh[ijk] = bpmObj[ijk].getChannel(BPM.Y_AVG_HANDLE);
            bpmXCh[ijk].connectAndWait();
            bpmYCh[ijk].connectAndWait();
            Channel.pendIO(5);
        }
        BeamTrigger bt = new BeamTrigger();
        bt.setDelay(1.);
        double Xtemp[][] = new double[devBPMCount][10];
        double Ytemp[][] = new double[devBPMCount][10];
        int k, kk;
        kk = 0;
        for (k = 0; k < 3; k++) {
            if (bt.isOn()) bt.makePulse();
            double beamCurrent = 0.0;
            double bpmSum = 5.0;
            bpmSum = 5.0;
            if (bpmSum > 0.0) {
                for (i = 0; i < devBPMCount; i++) {
                    try {
                        Xtemp[i][kk] = bpmObj[i].getXAvg();
                        Ytemp[i][kk] = bpmObj[i].getYAvg();
                        XtempBPMData[i] = XtempBPMData[i] + Xtemp[i][kk];
                        YtempBPMData[i] = YtempBPMData[i] + Ytemp[i][kk];
                    } catch (ConnectionException e) {
                        System.out.println(e);
                    } catch (GetException e) {
                        System.out.println(e);
                    }
                }
                kk = kk + 1;
            } else {
                System.out.println("Beam Current too low");
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }
        System.out.println("successfully got " + kk + " pulses");
        for (i = 0; i < devBPMCount; i++) {
            XtempBPMData[i] = XtempBPMData[i] / kk;
            YtempBPMData[i] = YtempBPMData[i] / kk;
            delX[i] = 0.0;
            delY[i] = 0.0;
            for (k = 0; k < kk; k++) {
                delX[i] = delX[i] + (XtempBPMData[i] - Xtemp[i][k]) * (XtempBPMData[i] - Xtemp[i][k]);
                delY[i] = delY[i] + (YtempBPMData[i] - Ytemp[i][k]) * (YtempBPMData[i] - Ytemp[i][k]);
            }
            delX[i] = Math.sqrt(delX[i] / kk);
            delY[i] = Math.sqrt(delY[i] / kk);
            System.out.println(bpmObj[i].getId() + "   " + XtempBPMData[i] + "+/-" + delX[i] + "   " + YtempBPMData[i] + "+/-" + delY[i]);
        }
        for (i = 0; i < devBPMCount; i++) {
            AcceleratorNode obJ = (AcceleratorNode) collect1.get(i);
            BPM obj = (BPM) obJ;
            xDatatl[0][i] = getSelectedSequence().getPosition(obj);
            yDatatl[0][i] = XtempBPMData[i] - tmpData3[i];
            tmpData3[i] = XtempBPMData[i];
            xDatabl[0][i] = getSelectedSequence().getPosition(obj);
            xDatabl1[0][i] = xDatabl[0][i];
            xDatabl1[1][i] = xDatabl[0][i];
            yDatabl[0][i] = tmpData3[i];
            yDatabl1[0][i] = delX[i];
            xDatatr[0][i] = getSelectedSequence().getPosition(obj);
            yDatatr[0][i] = YtempBPMData[i] - tmpData4[i];
            tmpData4[i] = YtempBPMData[i];
            xDatabr[0][i] = getSelectedSequence().getPosition(obj);
            xDatabr1[0][i] = xDatabr[0][i];
            xDatabr1[1][i] = xDatabr[0][i];
            yDatabr[0][i] = tmpData4[i];
            yDatabr1[0][i] = delY[i];
        }
        for (i = devBPMCount; i < devCountNoDiag; i++) {
            xDatatl[0][i] = xDatatl[0][devBPMCount - 1];
            yDatatl[0][i] = yDatatl[0][devBPMCount - 1];
            xDatabl[0][i] = xDatabl[0][devBPMCount - 1];
            yDatabl[0][i] = yDatabl[0][devBPMCount - 1];
            xDatatr[0][i] = xDatatr[0][devBPMCount - 1];
            yDatatr[0][i] = yDatatr[0][devBPMCount - 1];
            xDatabr[0][i] = xDatabr[0][devBPMCount - 1];
            yDatabr[0][i] = yDatabr[0][devBPMCount - 1];
        }
        myWindow().plot(xDatatl, yDatatl, xDatatr, yDatatr, xDatabl, yDatabl, xDatabr, yDatabr, delX, delY, devBPMCount);
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
            if (extension.equals(Utils.xml)) {
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

class Utils {

    public static final String xml = "probe";

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
