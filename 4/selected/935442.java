package gov.sns.apps.mpx;

import gov.sns.application.Commander;
import gov.sns.tools.apputils.SimpleProbeEditor;
import gov.sns.tools.data.DataAdaptor;
import gov.sns.tools.data.DataListener;
import gov.sns.tools.xml.XmlDataAdaptor;
import gov.sns.tools.pvlogger.*;
import gov.sns.tools.database.*;
import gov.sns.tools.apputils.pvlogbrowser.PVLogSnapshotChooser;
import gov.sns.tools.apputils.wirescan.*;
import gov.sns.xal.model.ModelException;
import gov.sns.xal.model.mpx.ModelProxy;
import gov.sns.xal.model.mpx.ModelProxyListenerAdaptor;
import gov.sns.xal.model.probe.*;
import gov.sns.xal.model.alg.*;
import gov.sns.xal.slg.LatticeError;
import gov.sns.xal.smf.*;
import gov.sns.xal.smf.application.AcceleratorDocument;
import gov.sns.xal.smf.data.XMLDataManager;
import gov.sns.xal.smf.impl.ProfileMonitor;
import gov.sns.xal.smf.impl.Electromagnet;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.*;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.JToggleButton.ToggleButtonModel;
import javax.swing.JFrame;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import org.w3c.dom.Document;
import gov.sns.tools.apputils.files.*;

/**
 * The custom document object for the MPXMain application.
 * 
 * @author wdklotz
 * @version $Id: MPXDocument.java 2 2006-08-17 12:20:30 +0000 (Thursday, 17 8 2006) t6p $
 */
public class MPXDocument extends AcceleratorDocument implements DataListener {

    private MPXProxy mxProxy;

    private DataAdaptor windowAdaptor;

    private Action selectProbeAction;

    private Action probeEditorAction;

    private Action runModelAction;

    private Action syncModelAction;

    private Action pvloggerAction;

    private Action latticeTreeAction;

    private ToggleButtonModel useCaModel;

    private ToggleButtonModel useDesignModel;

    private ToggleButtonModel useRfDesignModel;

    private ToggleButtonModel usePvlogModel;

    protected ToggleButtonModel useWsModel;

    ToggleButtonModel set = new ToggleButtonModel();

    ToggleButtonModel readback = new ToggleButtonModel();

    protected boolean isOnline = true;

    protected boolean isReadback = false;

    LoggerSession loggerSession;

    MachineSnapshot snapshot;

    private PVLogSnapshotChooser plsc;

    private boolean usePVLog = false;

    private long pvlogId = 0;

    private JDialog pvLogSelector;

    private RecentFileTracker _wireFileTracker;

    double[][] posData;

    double[][] xData;

    double[][] yData;

    /** Create a new empty document */
    public MPXDocument() {
        this(null);
    }

    /**
	 * Create a new document loaded from the URL file
	 * 
	 * @param url
	 *            The URL of the file to load into the new document.
	 */
    MPXDocument(java.net.URL url) {
        _wireFileTracker = new RecentFileTracker(1, this.getClass(), "recent_wires");
        useCaModel = new ToggleButtonModel();
        useCaModel.setEnabled(false);
        useRfDesignModel = new ToggleButtonModel();
        useRfDesignModel.setEnabled(false);
        usePvlogModel = new ToggleButtonModel();
        usePvlogModel.setEnabled(false);
        useWsModel = new ToggleButtonModel();
        useWsModel.setEnabled(false);
        mxProxy = new MPXProxy(MPXMain.PARAM_SRC);
        mxProxy.addModelProxyListener(new ModelProxyListenerAdaptor() {

            @Override
            public void accelMasterChanged(ModelProxy mp) {
                MPXProxy mpx = (MPXProxy) mp;
                if (mpx.getAcceleratorAsDocument() != null) myWindow().setAcceleratorTree(mpx.getAcceleratorAsDocument());
            }

            @Override
            public void accelSequenceChanged(ModelProxy mp) {
                MPXProxy mpx = (MPXProxy) mp;
                try {
                    myWindow().setSequenceTree(mpx.getSequenceAsDocument());
                } catch (LatticeError e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void probeMasterChanged(ModelProxy mp) {
                myWindow().setProbeTree(mp.getProbeAsDocument());
            }

            @Override
            public void modelResultsChanged(ModelProxy mp) {
                MPXProxy mpx = (MPXProxy) mp;
                if (mpx.getTrajectoryAsDocument() != null) myWindow().setTrajectoryTree(mpx.getTrajectoryAsDocument());
                MPXPhaseVectorTable table1 = mpx.getPhaseVectorTable();
                if (table1 != null) myWindow().setPhaseVectorTable(table1);
                MPXTwissFunctionTable table2 = mpx.getTwissFunctionTable();
                if (table2 != null) myWindow().setTwissFunctionTable(table2);
                myWindow().setTwissPlot(mpx);
            }

            @Override
            public void missingInputToRunModel(ModelProxy mp) {
                MPXProxy mpx = (MPXProxy) mp;
                if (!mpx.hasAccelerator()) {
                    popNoAcclMessage();
                } else if (!mpx.hasLattice()) {
                    popNoSequenceMessage();
                } else if (!mpx.hasProbe()) {
                    popNoProbeMessage();
                }
            }
        });
        if (url != null) {
            System.out.println("Opening document: " + url.toString());
            setSource(url);
        }
    }

    @Override
    public void makeMainWindow() {
        mainWindow = new MPXMainWindow(this);
        plsc = new PVLogSnapshotChooser(mainWindow);
        if (getSource() != null) {
            java.net.URL url = getSource();
            DataAdaptor documentAdaptor = XmlDataAdaptor.adaptorForUrl(url, false);
            update(documentAdaptor.childAdaptor("MpxDocument"));
            setHasChanges(false);
        }
        if (useCaModel.isSelected()) {
            MPXMain.PARAM_SRC = ModelProxy.PARAMSRC_LIVE;
            initPVLogger();
            syncModelAction.setEnabled(true);
        } else if (useRfDesignModel.isSelected()) {
            MPXMain.PARAM_SRC = ModelProxy.PARAMSRC_RF_DESIGN;
            syncModelAction.setEnabled(true);
        } else if (useDesignModel.isSelected()) {
            MPXMain.PARAM_SRC = ModelProxy.PARAMSRC_DESIGN;
            syncModelAction.setEnabled(true);
        } else if (usePvlogModel.isSelected()) MPXMain.PARAM_SRC = ModelProxy.PARAMSRC_DESIGN; else if (useWsModel.isSelected()) MPXMain.PARAM_SRC = ModelProxy.PARAMSRC_DESIGN;
        setHasChanges(false);
    }

    /**
	 * Save the document to the specified URL.
	 * 
	 * @param url
	 *            The URL to which the document should be saved.
	 */
    @Override
    public void saveDocumentAs(java.net.URL url) {
        try {
            XmlDataAdaptor documentAdaptor = XmlDataAdaptor.newEmptyDocumentAdaptor();
            documentAdaptor.writeNode(this);
            documentAdaptor.writeToUrl(url);
            setHasChanges(false);
        } catch (XmlDataAdaptor.WriteException exception) {
            exception.printStackTrace();
            displayError("Save Failed!", "Save failed due to an internal write exception!", exception);
        } catch (Exception exception) {
            exception.printStackTrace();
            displayError("Save Failed!", "Save failed due to an internal exception!", exception);
        }
    }

    /**
	 * Convenience method for getting the main window cast to the proper
	 * subclass of XalWindow. This allows me to avoid casting the window every
	 * time I reference it.
	 * 
	 * @return The main window cast to its dynamic runtime class
	 */
    private MPXMainWindow myWindow() {
        return (MPXMainWindow) mainWindow;
    }

    /**
	 * Register custom actions for the commands of this application
	 * 
	 * @param commander
	 *            The commander with which to register the custom commands.
	 */
    @Override
    protected void customizeCommands(Commander commander) {
        selectProbeAction = new AbstractAction("select-probe") {

            public void actionPerformed(ActionEvent event) {
                File probeMasterFile = mxProxy.getProbeMasterFile();
                probeMasterFile = myWindow().openMasterFileChooser(probeMasterFile);
                if (probeMasterFile == null) return;
                try {
                    mxProxy.setNewProbe(probeMasterFile);
                } catch (Throwable ex) {
                    popThrowableMessage(ex);
                }
            }
        };
        commander.registerAction(selectProbeAction);
        probeEditorAction = new AbstractAction("probe-editor") {

            public void actionPerformed(ActionEvent event) {
                SimpleProbeEditor spe = new SimpleProbeEditor();
                if (mxProxy.hasProbe()) {
                    mxProxy.resetProbe();
                    spe.createSimpleProbeEditor(mxProxy.getProbe());
                } else {
                    if (mxProxy.getProbeMasterFile() != null) {
                        spe.createSimpleProbeEditor(mxProxy.getProbeMasterFile());
                    } else {
                        spe.createSimpleProbeEditor();
                    }
                }
                if (spe.probeHasChanged()) {
                    if (spe.getProbe() instanceof EnvelopeProbe) mxProxy.setNewProbe(spe.getProbe()); else mxProxy.setNewProbe(spe.getProbe());
                }
            }
        };
        commander.registerAction(probeEditorAction);
        runModelAction = new AbstractAction("run-model") {

            public void actionPerformed(ActionEvent event) {
                try {
                    mxProxy.checkAccelerator();
                    mxProxy.checkLattice();
                    mxProxy.checkProbe();
                    mxProxy.synchronizeAcceleratorSeq();
                } catch (LatticeError e) {
                    e.printStackTrace();
                    return;
                }
                if (mxProxy.hasProbe()) {
                    if (usePVLog) {
                        if (pvlogId == 0 && plsc != null) pvlogId = plsc.getPVLogId();
                        if (pvlogId > 0) mxProxy.setPVlogger(pvlogId); else System.out.println("invalid PV Logger ID");
                    }
                    try {
                        mxProxy.runModel();
                    } catch (ModelException e) {
                        e.printStackTrace();
                        return;
                    }
                }
                if (useCaModel.isSelected() || useRfDesignModel.isSelected()) {
                }
                if (useWsModel.isSelected()) myWindow().plotPane.setWSData(posData, xData, yData);
            }
        };
        commander.registerAction(runModelAction);
        syncModelAction = new AbstractAction("synchronize-model") {

            public void actionPerformed(ActionEvent event) {
                try {
                    mxProxy.checkAccelerator();
                    mxProxy.checkLattice();
                    mxProxy.synchronizeAcceleratorSeq();
                } catch (LatticeError e) {
                    e.printStackTrace();
                    return;
                }
            }
        };
        commander.registerAction(syncModelAction);
        pvloggerAction = new AbstractAction("save-pvlogger") {

            public void actionPerformed(ActionEvent event) {
                loggerSession.publishSnapshot(snapshot);
            }
        };
        commander.registerAction(pvloggerAction);
        pvloggerAction.setEnabled(false);
        latticeTreeAction = new AbstractAction("lattice-tree") {

            public void actionPerformed(ActionEvent event) {
                System.out.println("start document prep...");
                Document latticeDoc = mxProxy.getOnLineModelLatticeAsDocument();
                System.out.println("end document prep...");
                myWindow().setLatticeTree(latticeDoc);
                myWindow().getTabbedPane().setEnabledAt(2, true);
            }
        };
        commander.registerAction(latticeTreeAction);
        latticeTreeAction.setEnabled(false);
        usePvlogModel.setSelected(MPXMain.PARAM_SRC == ModelProxy.PARAMSRC_DESIGN);
        usePvlogModel.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                if (pvLogSelector == null) pvLogSelector = plsc.choosePVLogId(); else pvLogSelector.setVisible(true);
                mxProxy.setChannelSource(ModelProxy.PARAMSRC_DESIGN);
                MPXMain.PARAM_SRC = mxProxy.getChannelSource();
                syncModelAction.setEnabled(true);
                usePVLog = true;
            }
        });
        commander.registerModel("use-pvlogger", usePvlogModel);
        useWsModel.setSelected(MPXMain.PARAM_SRC == ModelProxy.PARAMSRC_DESIGN);
        useWsModel.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                String currentDirectory = _wireFileTracker.getRecentFolderPath();
                JFrame frame = new JFrame();
                JFileChooser fileChooser = new JFileChooser(currentDirectory);
                fileChooser.addChoosableFileFilter(new WireFileFilter());
                int status = fileChooser.showOpenDialog(frame);
                if (status == JFileChooser.APPROVE_OPTION) {
                    _wireFileTracker.cacheURL(fileChooser.getSelectedFile());
                    File file = fileChooser.getSelectedFile();
                    setWSFile(file);
                }
                mxProxy.setChannelSource(ModelProxy.PARAMSRC_DESIGN);
                MPXMain.PARAM_SRC = mxProxy.getChannelSource();
                syncModelAction.setEnabled(true);
                usePVLog = true;
            }
        });
        commander.registerModel("use-ws", useWsModel);
        useCaModel.setSelected(MPXMain.PARAM_SRC == ModelProxy.PARAMSRC_LIVE);
        useCaModel.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                useCa(event);
                initPVLogger();
                pvloggerAction.setEnabled(true);
            }
        });
        commander.registerModel("use-ca", useCaModel);
        useDesignModel = new ToggleButtonModel();
        useDesignModel.setSelected(MPXMain.PARAM_SRC == ModelProxy.PARAMSRC_DESIGN);
        useDesignModel.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                useDesign(event);
                pvloggerAction.setEnabled(false);
            }
        });
        commander.registerModel("use-design", useDesignModel);
        useRfDesignModel.setSelected(MPXMain.PARAM_SRC == ModelProxy.PARAMSRC_RF_DESIGN);
        useRfDesignModel.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                useRfDesign(event);
            }
        });
        commander.registerModel("use-rf_design", useRfDesignModel);
        set.setSelected(false);
        set.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                isReadback = false;
                if (getSelectedSequence() != null) {
                    List allEMMag = selectedSequence.getAllNodesOfType("emag");
                    for (int i = 0; i < allEMMag.size(); i++) {
                        ((Electromagnet) allEMMag.get(i)).setUseFieldReadback(false);
                    }
                } else {
                    myWindow().getTextField().setText("Please select Accelerator/Sequence first!");
                }
            }
        });
        commander.registerModel("set", set);
        readback.setSelected(true);
        readback.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                isReadback = true;
                if (selectedSequence != null) {
                    List allEMMag = selectedSequence.getAllNodesOfType("emag");
                    for (int i = 0; i < allEMMag.size(); i++) {
                        ((Electromagnet) allEMMag.get(i)).setUseFieldReadback(true);
                    }
                } else {
                    myWindow().getTextField().setText("Please select Accelerator/Sequence first!");
                }
            }
        });
        commander.registerModel("readback", readback);
    }

    @Override
    public void acceleratorChanged() {
        if (accelerator != null) {
            try {
                mxProxy.setAccelerator(getAccelerator());
                setHasChanges(true);
            } catch (Exception e) {
                System.out.println("Accelerator file path is incorrect");
                e.printStackTrace();
            }
        } else {
            try {
                mxProxy.setAccelerator(new File(XMLDataManager.defaultPath()));
            } catch (Exception e) {
                System.out.println("Default accelerator file path is incorrect");
                e.printStackTrace();
            }
        }
    }

    @Override
    public void selectedSequenceChanged() {
        if (selectedSequence != null) {
            try {
                if (getSelectedSequence() instanceof Ring) {
                    mxProxy.setAcceleratorSeq(getSelectedSequence());
                } else {
                    mxProxy.setAcceleratorSeq(getSelectedSequence());
                }
                setHasChanges(true);
                try {
                    if (selectedSequence instanceof Ring) {
                        TransferMapProbe myProbe = ProbeFactory.getTransferMapProbe(selectedSequence, new TransferMapTracker());
                        mxProxy.setNewProbe(myProbe);
                    } else {
                        EnvelopeProbe myProbe = ProbeFactory.getEnvelopeProbe(selectedSequence, new EnvTrackerAdapt(selectedSequence));
                        mxProxy.setNewProbe(myProbe);
                    }
                } catch (NullPointerException e) {
                    myWindow().getTextField().setText("There is no default probe for this sequence.");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            useRfDesignModel.setEnabled(true);
            useCaModel.setEnabled(true);
            usePvlogModel.setEnabled(true);
            useWsModel.setEnabled(true);
            mxProxy.setChannelSource(MPXMain.PARAM_SRC);
            latticeTreeAction.setEnabled(true);
            myWindow().getTabbedPane().setEnabledAt(2, false);
        }
    }

    /**
	 * initialize PV logger
	 */
    private void initPVLogger() {
        ConnectionDictionary dict = ConnectionDictionary.defaultDictionary();
        SqlStateStore store;
        if (dict != null) {
            store = new SqlStateStore(dict);
        } else {
            ConnectionPreferenceController.displayPathPreferenceSelector();
            dict = ConnectionDictionary.defaultDictionary();
            store = new SqlStateStore(dict);
        }
        ChannelGroup group = store.fetchGroup("default");
        loggerSession = new LoggerSession(group, store);
    }

    private void useCa(ActionEvent ev) {
        mxProxy.setChannelSource(ModelProxy.PARAMSRC_LIVE);
        MPXMain.PARAM_SRC = mxProxy.getChannelSource();
        syncModelAction.setEnabled(true);
    }

    private void useDesign(ActionEvent ev) {
        mxProxy.setChannelSource(ModelProxy.PARAMSRC_DESIGN);
        MPXMain.PARAM_SRC = mxProxy.getChannelSource();
        syncModelAction.setEnabled(true);
    }

    private void useRfDesign(ActionEvent ev) {
        mxProxy.setChannelSource(ModelProxy.PARAMSRC_RF_DESIGN);
        MPXMain.PARAM_SRC = mxProxy.getChannelSource();
        syncModelAction.setEnabled(true);
    }

    MPXProxy getModelProxy() {
        return mxProxy;
    }

    public String dataLabel() {
        return "MpxDocument";
    }

    private void setWSFile(File file) {
        WireDataFileParser wdfp = new WireDataFileParser();
        ArrayList wires = wdfp.readFile(file);
        HashMap wireMap = wdfp.getWireMap();
        ArrayList allWSs = (ArrayList) getSelectedSequence().getAllNodesOfType("WS");
        posData = new double[1][allWSs.size()];
        xData = new double[1][allWSs.size()];
        yData = new double[1][allWSs.size()];
        String[] wsNames = new String[allWSs.size()];
        for (int i = 0; i < allWSs.size(); i++) {
            wsNames[i] = ((ProfileMonitor) allWSs.get(i)).getId();
            posData[0][i] = getSelectedSequence().getPosition(((ProfileMonitor) allWSs.get(i)));
            WireData wd = (WireData) wireMap.get(wsNames[i]);
            try {
                xData[0][i] = wd.getZFitSigma();
                yData[0][i] = wd.getXFitSigma();
            } catch (NullPointerException ne) {
                System.out.println("No data for " + wsNames[i]);
                xData[0][i] = 0.;
                yData[0][i] = 0.;
            }
        }
        pvlogId = wdfp.getPVLoggerId();
    }

    public void update(DataAdaptor adaptor) {
        if (getSource() != null) {
            this.setAcceleratorFilePath(adaptor.stringValue("accelerator"));
            File probeMasterFile = new File(adaptor.stringValue("probeFile"));
            try {
                String accelUrl = "file://" + this.getAcceleratorFilePath();
                XMLDataManager dMgr = new XMLDataManager(accelUrl);
                this.setAccelerator(dMgr.getAccelerator(), getAcceleratorFilePath());
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Hey - I had trouble parsing the accelerator input xml file you fed me", "MPX error", JOptionPane.ERROR_MESSAGE);
            }
            List temp = adaptor.childAdaptors("sequences");
            if (temp.isEmpty()) return;
            ArrayList seqs = new ArrayList();
            DataAdaptor da2a = adaptor.childAdaptor("sequences");
            String seqName = da2a.stringValue("name");
            temp = da2a.childAdaptors("seq");
            Iterator itr = temp.iterator();
            while (itr.hasNext()) {
                DataAdaptor da = (DataAdaptor) itr.next();
                seqs.add(getAccelerator().getSequence(da.stringValue("name")));
            }
            setSelectedSequence(new AcceleratorSeqCombo(seqName, seqs));
            try {
                mxProxy.setNewProbe(probeMasterFile);
            } catch (Throwable ex) {
                popThrowableMessage(ex);
            }
        }
    }

    public void write(DataAdaptor adaptor) {
        adaptor.setValue("date", new Date().toString());
        adaptor.setValue("accelerator", getAcceleratorFilePath());
        adaptor.setValue("probeFile", mxProxy.getProbeMasterFile());
        ArrayList seqs;
        if (getSelectedSequence() != null) {
            DataAdaptor daSeq = adaptor.createChild("sequences");
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
    }

    public void WillClose() {
    }

    private void popNoAcclMessage() {
        String message = "Operation needs an accelerator!\n Select an input file first!";
        String title = "Missing Input!";
        displayError(title, message);
    }

    private void popNoSequenceMessage() {
        String message = "Operation needs a lattice!\n Select an accelerator sequence first!";
        String title = "Missing Input!";
        displayError(title, message);
    }

    private void popNoProbeMessage() {
        String message = "Operation needs a probe!\n Select a probe input file first!";
        String title = "Missing Input!";
        displayError(title, message);
    }

    void popInvalidProbeMessage(Throwable ex) {
        String message = "Operation needs a valid probe!\n Run the probe first!";
        String title = "Invalid Probe!";
        System.out.println(ex.getMessage());
        displayError(title, message);
    }

    private void popThrowableMessage(Throwable ex) {
        String message = "Read the console for details!";
        String title = "An Error or Exception ocurred!";
        System.out.println(ex.getMessage());
        ex.printStackTrace();
        displayError(title, message);
    }
}

class Utils {

    public static final String xml = "inp";

    public static final String wire = "txt";

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

class WireFileFilter extends javax.swing.filechooser.FileFilter {

    @Override
    public boolean accept(File f) {
        if (f.isDirectory()) {
            return true;
        }
        String extension = Utils.getExtension(f);
        if (extension != null) {
            if (extension.equals(Utils.wire)) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    @Override
    public String getDescription() {
        return "Wire Scan Data File";
    }
}
