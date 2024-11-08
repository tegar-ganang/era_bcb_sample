package gov.sns.xal.model.mpx;

import gov.sns.tools.beam.Twiss;
import gov.sns.xal.model.ModelException;
import gov.sns.xal.model.elem.Element;
import gov.sns.xal.model.probe.EnvelopeProbe;
import gov.sns.xal.model.probe.Probe;
import gov.sns.xal.model.probe.TransferMapProbe;
import gov.sns.xal.model.probe.traj.EnvelopeProbeState;
import gov.sns.xal.model.probe.traj.ProbeState;
import gov.sns.xal.model.pvlogger.PVLoggerDataSource;
import gov.sns.xal.model.scenario.Scenario;
import gov.sns.xal.model.sync.SynchronizationException;
import gov.sns.xal.model.xml.ParsingException;
import gov.sns.xal.model.xml.ProbeXmlParser;
import gov.sns.xal.model.xml.ProbeXmlWriter;
import gov.sns.xal.slg.LatticeError;
import gov.sns.xal.smf.AcceleratorNode;
import gov.sns.xal.smf.AcceleratorSeq;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import javax.swing.event.EventListenerList;
import org.w3c.dom.Document;

/**This class provides an API to the on-line model.
 * It features convenience methods to input data to the model, to generate the lattice from 
 * an {@link gov.sns.xal.smf.AcceleratorSeq accelerator sequence}, to run the model, to synchronize the model with the real accelerator,
 * and to extract results from a model run.
 * @author wdklotz
 * @version $Id: ModelProxy.java 2 2006-08-17 12:20:30 +0000 (Thursday, 17 8 2006) t6p $
 */
public class ModelProxy {

    protected AcceleratorSeq acceleratorSequence;

    private File probeMasterFile = null;

    private EventListenerList listeners;

    private boolean bLattice;

    private boolean bPropagated = false;

    protected Scenario scenario;

    protected Probe probe = null;

    private EnvelopeProbeState initProbeState;

    private Twiss[] initTwiss;

    public static final String PARAMSRC_DESIGN = Scenario.SYNC_MODE_DESIGN;

    public static final String PARAMSRC_RF_DESIGN = Scenario.SYNC_MODE_RF_DESIGN;

    public static final String PARAMSRC_LIVE = Scenario.SYNC_MODE_LIVE;

    public static final String PARAMSRC_MONITOR_ONLINE = Scenario.SYNC_MODE_MONITOR_ONLINE;

    private String paramSrc;

    /**Probe type indicating a diagnostic probe.*/
    public static final int DIAGNOSTIC_PROBE = 16;

    /**Probe type indicating an ensemble probe.*/
    public static final int ENSEMBLE_PROBE = 17;

    /**Probe type indicating an envelope probe.*/
    public static final int ENVELOPE_PROBE = 18;

    /**Probe type indicating a particle probe.*/
    public static final int PARTICLE_PROBE = 19;

    public static final int TRANSFERMAP_PROBE = 20;

    /**Create a new (empty) model proxy. Per default this proxy will be synchronized by
	 * channel access.
	 */
    public ModelProxy() {
        this(PARAMSRC_LIVE);
    }

    /**Create a new (empty) model proxy.
	 * @param paramSrc an integer to indicate where to synchronize from. If <code>true</code> the proxy
	 * will use channel access to read settings of lattice elements, if <code>false</code> the element
	 * settings will be taken from design values defined in the accelerator database.
	 */
    public ModelProxy(String paramSrc) {
        this.paramSrc = paramSrc;
        bLattice = false;
        listeners = new EventListenerList();
    }

    /**Add a ModelProxyListener to the internal list of event listeners.
	 * @param listener the listener.
	 */
    public void addModelProxyListener(ModelProxyListener listener) {
        listeners.add(ModelProxyListener.class, listener);
    }

    /**Remove a ModelProxyListener from the internal list of event listeners.
	 * @param listener the listener.
	 */
    public void removeModelProxyListener(ModelProxyListener listener) {
        listeners.remove(ModelProxyListener.class, listener);
    }

    /**Getter for the ModelProxyListener event list property.
	 */
    protected ModelProxyListener[] getModelProxyListeners() {
        return listeners.getListeners(ModelProxyListener.class);
    }

    /**Run the model. The probe will be propagated through the lattice. All registered ModelProxyListeners will be
	 * notified by calling 
	 * {@link gov.sns.xal.model.mpx.ModelProxyListener#modelResultsChanged modelResultsChanged}.
	 * @throws ModelException
	 */
    public void runModel() throws ModelException {
        try {
            checkLattice();
            scenario.setProbe(probe);
            if (bPropagated) resetProbe();
            scenario.run();
            bPropagated = true;
            notifyListeners(ModelProxyListener.RESULTS_CHANGED);
        } catch (LatticeError e) {
            throw new ModelException(e.getMessage());
        }
    }

    /**Notify registered ModelProxyListeners.
	 * @param cause what to notify. Cause can be either of:
	 * <code>ModelProxyListener.ACCEL_CHANGED</code>,
	 * <code>ModelProxyListener.PROBE_CHANGED</code>,
	 * <code>ModelProxyListener.SEQUENCE_CHANGED</code>,
	 * <code>ModelProxyListener.RESULTS_CHANGED</code>,
	 * <code>ModelProxyListener.MISSING_INPUT</code>. If neither
	 * of these values no notification is issued.
	 */
    protected void notifyListeners(int cause) {
        ModelProxyListener[] mpxLs = getModelProxyListeners();
        for (int i = 0; i < mpxLs.length; i++) {
            switch(cause) {
                case ModelProxyListener.ACCEL_CHANGED:
                    mpxLs[i].accelMasterChanged(this);
                    break;
                case ModelProxyListener.PROBE_CHANGED:
                    mpxLs[i].probeMasterChanged(this);
                    break;
                case ModelProxyListener.SEQUENCE_CHANGED:
                    mpxLs[i].accelSequenceChanged(this);
                    break;
                case ModelProxyListener.RESULTS_CHANGED:
                    mpxLs[i].modelResultsChanged(this);
                    break;
                case ModelProxyListener.MISSING_INPUT:
                    mpxLs[i].missingInputToRunModel(this);
                    break;
                default:
                    break;
            }
        }
    }

    /**Synchronize the model with according to current sync mode. This operation
	 * resets the probe to its initial values. All registered ModelProxyListeners will be
	 * notified by:
	 * {@link gov.sns.xal.model.mpx.ModelProxyListener#probeMasterChanged probeMasterChanged} and
	 * {@link gov.sns.xal.model.mpx.ModelProxyListener#accelSequenceChanged accelSequenceChanged}.
	 * @throws LatticeError
	 * @throws ParsingException
	 * @throws Exception
	 */
    public void synchronizeAcceleratorSeq() throws LatticeError {
        checkLattice();
        MPXStopWatch.timeElapsed("...xal.model.Scenario.resync():begin! ");
        try {
            scenario.resync();
        } catch (SynchronizationException e) {
            System.out.println(e);
            throw new LatticeError("SynchronizationException during resync");
        }
        MPXStopWatch.timeElapsed("...xal.model.Scenario.resync():end! ");
        notifyListeners(ModelProxyListener.SEQUENCE_CHANGED);
    }

    /**Reset the probe to its initial values.
	 * @throws ParsingException
	 */
    public void resetProbe() {
        try {
            checkProbe();
            probe.reset();
            if (probe instanceof EnvelopeProbe) {
                ((EnvelopeProbe) probe).initFromTwiss(initTwiss);
                probe.applyState(initProbeState);
            }
            if (!(probe instanceof TransferMapProbe)) {
                probe.initialize();
            }
            bPropagated = false;
        } catch (LatticeError e) {
            e.printStackTrace();
        }
    }

    /**Setter for the {@link gov.sns.xal.smf.AcceleratorSeq accelerator sequence} property.
	 * As a result of calling this member a new Scenario will be generated.  In Addition all registered ModelproxyListeners will be notified
	 * by {@link gov.sns.xal.model.mpx.ModelProxyListener#accelSequenceChanged 
	 * accelSequenceChanged}.
	 * @param seq the accelerator sequence.
	 * @throws LatticeError
	 * @throws ParsingException
	 * @throws Exception
	 */
    public void setAcceleratorSeq(AcceleratorSeq seq) throws LatticeError {
        acceleratorSequence = seq;
        bLattice = false;
        MPXStopWatch.timeElapsed("...xal.model.Scenario: begin! ");
        try {
            if (seq instanceof gov.sns.xal.smf.Ring) {
                scenario = Scenario.newScenarioFor((gov.sns.xal.smf.Ring) seq);
            } else {
                scenario = Scenario.newScenarioFor(seq);
            }
        } catch (ModelException e) {
            throw new LatticeError("ModelException building scenario for: " + seq);
        }
        MPXStopWatch.timeElapsed("...xal.model.Scenario: end! ");
        bLattice = true;
        notifyListeners(ModelProxyListener.SEQUENCE_CHANGED);
    }

    /**Setter for the {@link gov.sns.xal.model.probe.Probe probe} property. All
	 * registered ModleProxyListeners will be notified by
	 * {@link gov.sns.xal.model.mpx.ModelProxyListener#probeMasterChanged probeMasterChanged}.
	 * @param  probeFile the file for the probe definition in XML.
	 * @throws ParsingException
	 * @throws ClassNotFoundException
	 * @throws MalformedURLException
	 */
    public void setNewProbe(File probeFile) throws LatticeError {
        if (probeFile.equals(probeMasterFile)) {
            return;
        } else {
            probeMasterFile = probeFile;
            Probe p;
            try {
                p = ProbeXmlParser.parse(probeMasterFile.getPath());
            } catch (ParsingException e) {
                throw new LatticeError(e.getMessage());
            }
            setNewProbe(p);
        }
    }

    /**
	 * Setter for a new probe.
	 * @param aProbe the new probe object.
	 */
    public void setNewProbe(Probe aProbe) {
        probe = aProbe;
        bPropagated = false;
        if (probe instanceof EnvelopeProbe) {
            initProbeState = (EnvelopeProbeState) probe.createProbeState();
            initTwiss = ((EnvelopeProbe) probe).getCorrelation().computeTwiss();
        }
        notifyListeners(ModelProxyListener.PROBE_CHANGED);
    }

    /**Setter for the data source flag.
	 * 
	 * @param src a constant to indicate the channel source. May be one of
	 * <code>Synchronization.PARAMSRC_DESIGN</code>
	 * <code>Synchronization.PARAMSRC_LIVE</code>
	 * @return true on succes, false if failed.
	 */
    public boolean setChannelSource(String src) {
        paramSrc = src;
        if (src == Scenario.SYNC_MODE_DESIGN) {
            scenario.setSynchronizationMode(Scenario.SYNC_MODE_DESIGN);
            return true;
        } else if (src == Scenario.SYNC_MODE_LIVE) {
            scenario.setSynchronizationMode(Scenario.SYNC_MODE_LIVE);
            return true;
        } else if (src == Scenario.SYNC_MODE_RF_DESIGN) {
            scenario.setSynchronizationMode(Scenario.SYNC_MODE_RF_DESIGN);
            return true;
        } else if (src == Scenario.SYNC_MODE_MONITOR_ONLINE) {
            scenario.setSynchronizationMode(Scenario.SYNC_MODE_MONITOR_ONLINE);
            return true;
        }
        return false;
    }

    public void setPVlogger(long pvlogId) {
        scenario.setSynchronizationMode(Scenario.SYNC_MODE_DESIGN);
        PVLoggerDataSource plds = new PVLoggerDataSource(pvlogId);
        scenario = plds.setModelSource(acceleratorSequence, scenario);
    }

    /**
	 * Returns last selected sequence.
	 * @return selectet sequence. If not set <code>null</code> is returned.
	 */
    public AcceleratorSeq getAcceleratorSequence() {
        try {
            checkLattice();
        } catch (LatticeError e) {
            e.printStackTrace();
            return null;
        }
        return acceleratorSequence;
    }

    /**Getter for the channel source flag */
    public String getChannelSource() {
        return paramSrc;
    }

    /**Getter for the {@link gov.sns.xal.model.Lattice on-line-model lattice} property. 
	 * @return the on-line-model lattice.
	 */
    public gov.sns.xal.model.Lattice getOnLineModelLattice() {
        try {
            checkLattice();
        } catch (LatticeError e) {
            e.printStackTrace();
            return null;
        }
        return scenario.getLattice();
    }

    /**Getter for the probe type. The type is either of: 
	 * <code>DIAGNOSTIC_PROBE</code>,
	 * <code>ENSEMBLE_PROBE</code>,
	 * <code>ENVELOPE_PROBE</code>,
	 * <code>PARTICLE_PROBE</code>
	 * @return the probe type. If probe is not set yet a -1 is returned.
	 */
    public int getProbeType() {
        try {
            checkProbe();
        } catch (LatticeError e) {
            e.printStackTrace();
            return -1;
        }
        if (getProbe() instanceof gov.sns.xal.model.probe.EnvelopeProbe) {
            return ModelProxy.ENVELOPE_PROBE;
        } else if (getProbe() instanceof gov.sns.xal.model.probe.DiagnosticProbe) {
            return ModelProxy.DIAGNOSTIC_PROBE;
        } else if (getProbe() instanceof gov.sns.xal.model.probe.EnsembleProbe) {
            return ModelProxy.ENSEMBLE_PROBE;
        } else if (getProbe() instanceof gov.sns.xal.model.probe.ParticleProbe) {
            return ModelProxy.PARTICLE_PROBE;
        } else if (getProbe() instanceof gov.sns.xal.model.probe.TransferMapProbe) {
            return ModelProxy.TRANSFERMAP_PROBE;
        }
        return -1;
    }

    /**Getter for the on-line-model {@link gov.sns.xal.model.probe.Probe probe} property.
	 * @return the on-line-model probe. If probe is not set yet <code>null</code> is retuned.
	 */
    public Probe getProbe() {
        try {
            checkProbe();
        } catch (LatticeError e) {
            e.printStackTrace();
            return null;
        }
        return probe;
    }

    /**Getter for the xml-file defining the probe.
	 * @return the opened probe file. If file is not set yet <code>null</code> is returned.
	 */
    public File getProbeMasterFile() {
        return probeMasterFile;
    }

    /**Getter for the {@link gov.sns.xal.model.Lattice on-line-model lattice} property. 
	 * @return the on-line-model lattice. If lattice is not set yet <code>null</code> is returned.
	 */
    public Document getOnLineModelLatticeAsDocument() {
        try {
            checkLattice();
        } catch (LatticeError e) {
            return null;
        }
        try {
            return getOnLineModelLattice().asDocument();
        } catch (IOException e1) {
            return null;
        }
    }

    /**Getter for the probe DOM property.
	 * 
	 * @return the DOM of the probe. If not set yet <code>null</code> is returned.
	 */
    public Document getProbeAsDocument() {
        try {
            checkProbe();
        } catch (LatticeError e) {
            e.printStackTrace();
            return null;
        }
        try {
            return ProbeXmlWriter.documentForProbe(probe);
        } catch (IOException e1) {
            e1.printStackTrace();
            return null;
        }
    }

    /**Find the probe's state for a given {@link gov.sns.xal.smf.AcceleratorNode accelerator node}.
	 * @param id the identifier of the given accelerator node.
	 * @return the probe state for the center of that node.
	 * @throws ModelException
	 */
    public ProbeState stateForElement(String id) throws ModelException {
        ProbeState state;
        try {
            checkLattice();
            checkProbe();
            AcceleratorNode node = scenario.nodeWithId(id);
            Element elem = (Element) scenario.elementsMappedTo(node).get(0);
            String latticeElementId = elem.getId();
            state = scenario.trajectoryStatesForElement(latticeElementId)[0];
        } catch (LatticeError e) {
            throw new ModelException(e.getMessage());
        }
        return state;
    }

    /**Find all probe states for a given {@link gov.sns.xal.smf.AcceleratorNode accelerator node}.
	 * @param id the identifier of the given accelerator node.
	 * @return an array probe states for the given element.
	 * @throws ModelException
	 */
    public ProbeState[] statesForElement(String id) throws ModelException {
        ProbeState[] states;
        try {
            checkLattice();
            checkProbe();
        } catch (LatticeError e) {
            throw new ModelException(e.getMessage());
        }
        states = scenario.trajectoryStatesForElement(id);
        return states;
    }

    /**Check wether the model proxy has a valid lattice and take actions. If the lattice is not
	 * valid all ModelProxyListeners will be notified by calling 
	 * {@link gov.sns.xal.model.mpx.ModelProxyListener#missingInputToRunModel missingInputToRunModel}
	 * and and an {@link java.lang.Error} will be thrown.
	 * @throws Error
	 */
    public void checkLattice() throws LatticeError {
        if (!hasLattice()) {
            notifyListeners(ModelProxyListener.MISSING_INPUT);
            throw new LatticeError("Missing Lattice:");
        }
    }

    /**Check wether the model proxy has a valid probe and take actions. If the probe is not
	 * valid all ModelProxyListeners will be notified by calling 
	 * {@link gov.sns.xal.model.mpx.ModelProxyListener#missingInputToRunModel missingInputToRunModel}
	 * and and an {@link java.lang.Error} will be thrown.
	 * @throws Error
	 */
    public void checkProbe() throws LatticeError {
        if (!hasProbe()) {
            notifyListeners(ModelProxyListener.MISSING_INPUT);
            throw new LatticeError("Missing Probe:");
        }
    }

    /**Test the model proxy for a valid lattice.
	 * @return <code>true</code> if the model has a valid lattice , <code>false</code> if not.
	 */
    public boolean hasLattice() {
        return bLattice;
    }

    /**Test the model proxy for a valid probe.
	 * @return <code>true</code> if the model has a valid probe , <code>false</code> if not.
	 */
    public boolean hasProbe() {
        return probe != null;
    }

    /** Returns true if the probe has been propagated, false otherwise.
	 * @return ture if the probe has been propagated, false otherwise
	 */
    protected boolean isProbePropagated() {
        return bPropagated;
    }
}
