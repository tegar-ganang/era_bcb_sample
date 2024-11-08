package gov.sns.apps.scldriftbeam;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.io.*;
import java.util.*;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JToggleButton.ToggleButtonModel;
import gov.sns.application.Commander;
import gov.sns.tools.data.DataAdaptor;
import gov.sns.tools.xml.XmlDataAdaptor;
import gov.sns.xal.smf.AcceleratorSeq;
import gov.sns.xal.smf.AcceleratorSeqCombo;
import gov.sns.xal.smf.application.AcceleratorDocument;
import gov.sns.xal.model.scenario.*;
import gov.sns.xal.model.probe.*;
import gov.sns.xal.model.alg.*;
import gov.sns.ca.*;
import gov.sns.ca.correlator.*;
import gov.sns.tools.correlator.*;

public class DriftBeamDocument extends AcceleratorDocument implements CorrelationNotice, Runnable {

    CavitySelector cavSelector = null;

    Action selectCavAction;

    ToggleButtonModel online = new ToggleButtonModel();

    ToggleButtonModel offline = new ToggleButtonModel();

    protected boolean isOnline = true;

    protected boolean isReadback = false;

    protected String pv1;

    protected String pv2;

    DriftBeamDocument myDoc = this;

    protected Scenario scenario;

    protected ChannelCorrelator correlator;

    protected double[][] signalphase = new double[40][512];

    protected double[][] signalamp = new double[40][512];

    protected double[] beamShape;

    protected double[] bcmx;

    protected double[] llrfx;

    private PeriodicPoster poster;

    protected boolean correlatorRunning = false;

    /** time to wait between get attempts (sec) */
    private Double dwellTime;

    /** max timeStamp difference to consitute a correlated set (sec) */
    private Double deltaT;

    private int nCorrelations;

    private int unused;

    protected boolean fullRecord;

    public DriftBeamDocument() {
        this(null);
        deltaT = new Double(0.01);
        dwellTime = new Double(0.502);
        nCorrelations = 0;
        unused = 0;
        fullRecord = false;
        correlator = new ChannelCorrelator(deltaT.doubleValue());
        poster = new PeriodicPoster(correlator, dwellTime.doubleValue());
        poster.addCorrelationNoticeListener(this);
    }

    /**
	 * Create a new document loaded from the URL file
	 * 
	 * @param url
	 *            The URL of the file to load into the new document.
	 */
    public DriftBeamDocument(java.net.URL url) {
        if (url == null) return;
        setSource(url);
    }

    protected int getLatest() {
        if (nCorrelations == 0) return 39; else return nCorrelations - 1;
    }

    protected boolean setPV1(String name) {
        if (correlatorRunning) stopCorrelator();
        if (pv1 == name) {
            if (!checkConnection(name)) return false;
            return true;
        }
        if (pv1 != null) correlator.removeSource(pv1);
        pv1 = name;
        if (!checkConnection(name)) return false;
        correlator.addChannel(name);
        return true;
    }

    protected boolean setPV1(Channel name) {
        if (correlatorRunning) stopCorrelator();
        if (pv1 == name.getId()) {
            if (!checkConnection(pv1)) return false;
            return true;
        }
        if (pv1 != null) correlator.removeSource(pv1);
        pv1 = name.getId();
        if (!checkConnection(pv1)) return false;
        correlator.addChannel(pv1);
        return true;
    }

    protected boolean setPV2(String name) {
        if (correlatorRunning) stopCorrelator();
        if (pv2 == name) {
            if (!checkConnection(name)) return false;
            return true;
        }
        if (pv2 != null) correlator.removeSource(pv2);
        if (!checkConnection(name)) return false;
        pv2 = name;
        correlator.addChannel(name);
        return true;
    }

    protected boolean setPV2(Channel name) {
        if (correlatorRunning) stopCorrelator();
        if (pv2 == name.getId()) {
            if (!checkConnection(pv2)) return false;
            return true;
        }
        if (pv2 != null) correlator.removeSource(pv2);
        pv2 = name.getId();
        if (!checkConnection(pv2)) return false;
        correlator.addChannel(pv2);
        return true;
    }

    /** check to see if we can connect to this PV: */
    private boolean checkConnection(String name) {
        Channel tempChannel = ChannelFactory.defaultFactory().getChannel(name);
        try {
            tempChannel.checkConnection();
        } catch (ConnectionException e) {
            return false;
        }
        return true;
    }

    public synchronized void newCorrelation(Object Sender, Correlation correlation) {
        ChannelTimeRecord pvValue1, pvValue2;
        pvValue1 = (ChannelTimeRecord) (correlation.getRecord(pv1));
        pvValue2 = (ChannelTimeRecord) (correlation.getRecord(pv2));
        signalphase[nCorrelations] = pvValue1.doubleArray();
        signalamp[nCorrelations] = pvValue2.doubleArray();
        nCorrelations++;
        if (nCorrelations >= 40) {
            nCorrelations = 0;
            unused++;
            fullRecord = true;
            getSCLPhase().phaseAvgBtn.setEnabled(true);
            if (unused > 200) {
                stopCorrelator();
            }
        }
    }

    public synchronized void noCorrelationCaught(Object sender) {
        unused++;
        if (unused > 1000) {
            stopCorrelator();
        }
    }

    /** This method controls the action when the correlator is
     * either stopped or started */
    protected void startCorrelator(java.awt.event.ActionEvent event) {
        if (correlatorRunning) {
            return;
        }
        correlator.startMonitoring();
        poster.start();
        correlatorRunning = true;
        unused = 0;
    }

    /** the  method to handle stop button clicks */
    protected void stopCorrelator() {
        if (!correlatorRunning) return;
        poster.stop();
        correlatorRunning = false;
        nCorrelations = 0;
        fullRecord = false;
    }

    public void run() {
    }

    /**
	 * Make a main window by instantiating the my custom window. Set the text
	 * pane to use the textDocument variable as its document.
	 */
    @Override
    public void makeMainWindow() {
        mainWindow = new DriftBeamWindow(this);
        if (getSource() != null) {
            XmlDataAdaptor xda = XmlDataAdaptor.adaptorForUrl(getSource(), false);
            DataAdaptor da1 = xda.childAdaptor("scldriftbeam");
            List pvs = da1.childAdaptor("pvList").childAdaptors("pv");
            if (pvs.isEmpty()) return;
        }
        setHasChanges(false);
    }

    /**
	 * Convenience method for getting the main window cast to the proper
	 * subclass of XalWindow. This allows me to avoid casting the window every
	 * time I reference it.
	 * 
	 * @return The main window cast to its dynamic runtime class
	 */
    protected DriftBeamWindow myWindow() {
        return (DriftBeamWindow) mainWindow;
    }

    @Override
    public void acceleratorChanged() {
        ArrayList sclSeqs = new ArrayList();
        List scls;
        if (accelerator != null) {
            try {
                sclSeqs.add(accelerator.getSequence("SCLMed"));
                sclSeqs.add(accelerator.getSequence("SCLHigh"));
                scls = AcceleratorSeq.orderSequences(sclSeqs);
                setSelectedSequence(AcceleratorSeqCombo.getInstance("SCL", scls));
                scenario = Scenario.newScenarioFor(getSelectedSequence());
                EnvelopeProbe probe = ProbeFactory.getEnvelopeProbe(getSelectedSequence(), new EnvTrackerAdapt(selectedSequence));
                scenario.setProbe(probe);
                scenario.resync();
                scenario.run();
                if (selectCavAction != null) {
                    selectCavAction.setEnabled(true);
                }
            } catch (Exception e) {
                System.out.println("Missing SCL sequence(s)!");
            }
        }
    }

    @Override
    protected void customizeCommands(Commander commander) {
        selectCavAction = new AbstractAction() {

            private static final long serialVersionUID = 1;

            public void actionPerformed(java.awt.event.ActionEvent event) {
                cavSelector = new CavitySelector(getSelectedSequence(), myDoc);
                cavSelector.setLocationRelativeTo(myWindow());
                cavSelector.popupCavSelector();
            }
        };
        selectCavAction.putValue(Action.NAME, "selectCav");
        commander.registerAction(selectCavAction);
        if (accelerator == null) selectCavAction.setEnabled(false);
        online.setSelected(true);
        online.addActionListener(new ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent event) {
                isOnline = true;
                getSCLPhase().lbPhaseBm.setEnabled(true);
                if (fullRecord) getSCLPhase().phaseAvgBtn.setEnabled(true); else getSCLPhase().phaseAvgBtn.setEnabled(false);
                getSCLPhase().phaseAvgBtn1.setEnabled(true);
                getSCLPhase().btBegin.setEnabled(true);
                getSCLPhase().btPulse.setEnabled(true);
            }
        });
        commander.registerModel("online", online);
        offline.setSelected(false);
        offline.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                stopCorrelator();
                isOnline = false;
                getSCLPhase().lbPhaseBm.setEnabled(false);
                getSCLPhase().phaseAvgBtn.setEnabled(false);
                getSCLPhase().phaseAvgBtn1.setEnabled(false);
                getSCLPhase().btBegin.setEnabled(false);
                getSCLPhase().btPulse.setEnabled(false);
                getSCLPhase().reset(event);
            }
        });
        commander.registerModel("offline", offline);
    }

    protected SCLPhase getSCLPhase() {
        return myWindow().getSCLPhase();
    }

    protected CavitySelector getCavSelector() {
        return cavSelector;
    }

    protected Scenario getScenario() {
        return scenario;
    }

    /**
	 * Save the document to the specified URL.
	 * 
	 * @param url
	 *            The URL to which the document should be saved.
	 */
    @Override
    public void saveDocumentAs(URL url) {
        try {
            Formatter sv = new Formatter(url.getFile());
            sv.format("DeltaT-us: LLRF %5.2f  BCM %5.2f\n", myDoc.getCavSelector().pulseWidthdt, 1.E6 * myDoc.getSCLPhase().deltaT);
            sv.format("Shape-used %3d\n", beamShape.length);
            for (int i = 0; i < beamShape.length; i++) {
                sv.format("%5.2f ", beamShape[i] * 1.E3);
            }
            sv.format("\nBCM-mA %3d\n", myDoc.getSCLPhase().bArry.length);
            for (int i = 0; i < myDoc.getSCLPhase().bArry.length; i++) {
                sv.format("%5.2f ", myDoc.getSCLPhase().bArry[i] * 1.E3);
            }
            sv.format("\n%s  %s\n", pv1, pv2);
            for (int i = 0; i < 40; i++) {
                for (int j = 0; j < 512; j++) sv.format("%8.3f %8.3f\n", signalphase[i][j], signalamp[i][j]);
            }
            sv.close();
        } catch (IOException ie) {
            System.out.println(ie + " in saving data to " + url.getFile());
        }
    }

    @Override
    protected void willClose() {
        if (correlatorRunning) stopCorrelator();
    }

    protected void setpv(Channel ca1, Channel ca2) {
        if (isOnline) {
            boolean t1 = setPV1(ca1);
            boolean t2 = setPV2(ca2);
        }
    }
}
