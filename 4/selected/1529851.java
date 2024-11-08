package gov.sns.xal.tools.simulationmanager;

import gov.sns.tools.data.EditContext;
import gov.sns.xal.model.ModelException;
import gov.sns.xal.model.elem.Element;
import gov.sns.xal.model.probe.EnvelopeProbe;
import gov.sns.xal.model.probe.Probe;
import gov.sns.xal.model.probe.traj.ProbeState;
import gov.sns.xal.model.scenario.Scenario;
import gov.sns.xal.model.sync.SynchronizationException;
import gov.sns.xal.model.xml.ProbeXmlWriter;
import gov.sns.xal.slg.Lattice;
import gov.sns.xal.slg.LatticeError;
import gov.sns.xal.slg.LatticeFactory;
import gov.sns.xal.smf.AcceleratorNode;
import gov.sns.xal.smf.AcceleratorSeq;
import gov.sns.xal.smf.AcceleratorSeqCombo;
import gov.sns.xal.smf.data.XMLDataManager;
import gov.sns.xal.smf.impl.Magnet;
import gov.sns.xal.smf.impl.RfGap;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

public abstract class SimulationManager {

    private static final boolean BENCH_MARK = false;

    public static final int XAL_ID = 0;

    public static final int T3D_ID = 1;

    public static final int SAD_ID = 2;

    public static final int MAD_ID = 3;

    protected boolean halfMag;

    Probe probe;

    public Probe getProbe() {
        return probe;
    }

    public void writeProbe(String outFile, String elemId) {
        try {
            ProbeState state = this.stateForElement(elemId);
            Probe tempProbe = Probe.newProbeInitializedFrom(probe);
            tempProbe.applyState(state);
            tempProbe.setPosition(0);
            tempProbe.setTime(0);
            tempProbe.setCurrentElement(" ");
            if (tempProbe instanceof EnvelopeProbe) {
                EnvelopeProbe eprobe = (EnvelopeProbe) tempProbe;
                ProbeXmlWriter.writeXmlAsTwiss(eprobe, outFile);
            } else {
                ProbeXmlWriter.writeXml(tempProbe, outFile);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<AcceleratorNode> getNodesOfType(String type) {
        return sequence.getNodesOfType(type);
    }

    public void writeProbesAtMarker(String outFileBase) {
        writeProbesAtMarker(outFileBase, "");
    }

    public void writeProbesAtMarker(String outFileBase, String tag) {
        List<AcceleratorNode> list = sequence.getNodesOfType("Marker");
        Iterator<AcceleratorNode> iter = list.iterator();
        while (iter.hasNext()) {
            AcceleratorNode marker = iter.next();
            String markerId = marker.getId();
            String outFile = null;
            if (tag == null || tag.equals("")) {
                outFile = outFileBase + "_" + markerId + ".probe";
            } else {
                outFile = outFileBase + "_" + markerId + "_" + tag + ".probe";
            }
            StringTokenizer st = new StringTokenizer(outFile, ":");
            int nt = st.countTokens();
            String trimOutFile = "";
            System.out.println("nt = " + nt);
            for (int it = 0; it < nt; it++) {
                String word = st.nextToken();
                if (it < nt - 1) {
                    trimOutFile += (word + "_");
                } else {
                    trimOutFile += word;
                }
            }
            System.out.println("trimOutFile = " + trimOutFile);
            writeProbe(trimOutFile, markerId);
        }
    }

    AcceleratorSeq sequence;

    Lattice lattice;

    String dataSource;

    protected Scenario scenario;

    /**
     * Set field path flag for Brho scaling for magnets
     *
     */
    public void setFieldPathFlag(double d) {
        List<AcceleratorNode> nodes = sequence.getAllNodesOfType("magnet");
        Iterator<AcceleratorNode> iter = nodes.iterator();
        while (iter.hasNext()) {
            AcceleratorNode node = iter.next();
            if (node instanceof Magnet) {
                Magnet mag = (Magnet) node;
                System.out.println("set fieldPathFlag of " + mag.getId() + " to " + d);
                mag.getMagBucket().setFieldPathFlag(d);
            }
        }
    }

    public void inverseResyncQuad() throws SynchronizationException {
        scenario.inverseResyncQuad();
    }

    /**
     * Switching on/off of RfGap node by using TTF
     * assumption is that TTF is 1 (when on).
     * @param seqId
     * @param onoff
     */
    public void switchRfGap(String seqId, boolean onoff) {
        if (sequence instanceof AcceleratorSeqCombo) {
            AcceleratorSeqCombo comboseq = (AcceleratorSeqCombo) sequence;
            System.out.println("*switchRfGap, seqId = " + seqId);
            List<AcceleratorSeq> seqList = comboseq.getBaseConstituents();
            Iterator<AcceleratorSeq> iterseq = seqList.iterator();
            AcceleratorSeq childseq = null;
            while (iterseq.hasNext()) {
                AcceleratorSeq seq = iterseq.next();
                if (seqId.equals(seq.getId())) {
                    childseq = seq;
                    break;
                }
            }
            if (childseq == null) {
                System.out.println("childseq = null");
                return;
            }
            List<AcceleratorNode> nodes = childseq.getAllNodesOfType("RG");
            if (nodes == null) {
                return;
            }
            Iterator<AcceleratorNode> iter = nodes.iterator();
            while (iter.hasNext()) {
                AcceleratorNode node = iter.next();
                if (node instanceof RfGap) {
                    RfGap rfgap = (RfGap) node;
                    System.out.println("Switching " + rfgap.getId() + " to " + onoff);
                    if (onoff) {
                    } else {
                        rfgap.getRfGap().setAmpFactor(0);
                    }
                }
            }
        }
    }

    public SimulationManager(AcceleratorSeq tmpSequence, Probe tmpProbe, String tmpDataSource) {
        this(tmpSequence, tmpProbe, tmpDataSource, false);
    }

    public SimulationManager(AcceleratorSeq tmpSequence, Probe tmpProbe, String tmpDataSource, boolean hMag) {
        probe = tmpProbe;
        halfMag = hMag;
        try {
            setSequence(tmpSequence);
        } catch (Exception e) {
        }
        dataSource = tmpDataSource;
    }

    public void setStartElementId(String id) {
        if (scenario != null) {
            scenario.setStartElementId(id);
        }
    }

    public void setStopElementId(String id) {
        if (scenario != null) {
            scenario.setStopElementId(id);
        }
    }

    protected void checkProbe() throws SimulationManagerException {
        if (probe == null) {
            throw new SimulationManagerException();
        }
    }

    protected void checkSequence() throws SimulationManagerException {
        if (sequence == null) {
            throw new SimulationManagerException();
        }
    }

    public abstract int getType();

    public void writeAccelerator(String outfile) {
        XMLDataManager datamanager = XMLDataManager.managerWithFilePath(outfile);
        datamanager.setMainUrlSpec("file://" + outfile + ".xal");
        datamanager.setOpticsUrlSpec("file://" + outfile + ".xdxf");
        datamanager.writeAccelerator(sequence.getAccelerator());
    }

    public ProbeState stateForElement(String id) throws ModelException {
        ProbeState state = null;
        AcceleratorNode node = scenario.nodeWithId(id);
        Element elem = null;
        if (node.getType().equals("DH")) {
            elem = (Element) scenario.elementsMappedTo(node).get(1);
        } else {
            elem = (Element) scenario.elementsMappedTo(node).get(0);
        }
        String latticeElementId = elem.getId();
        state = scenario.trajectoryStatesForElement(latticeElementId)[0];
        return state;
    }

    public EditContext pass() throws Exception {
        Date date1 = new Date();
        generateInput();
        Date date2 = new Date();
        run();
        Date date3 = new Date();
        EditContext data = readData();
        Date date4 = new Date();
        if (BENCH_MARK) {
            System.out.println("dt (generateInput) = " + (date2.getTime() - date1.getTime()) / 1000.);
            System.out.println("dt (run) = " + (date3.getTime() - date2.getTime()) / 1000.);
            System.out.println("dt (readFastData) = " + (date4.getTime() - date3.getTime()) / 1000.);
        }
        return data;
    }

    public List<TwissFastData> fastpass() throws Exception {
        Date date1 = new Date();
        generateInput();
        Date date2 = new Date();
        run();
        Date date3 = new Date();
        List<TwissFastData> list = readFastData();
        Date date4 = new Date();
        if (BENCH_MARK) {
            System.out.println("dt (generateInput) = " + (date2.getTime() - date1.getTime()) / 1000.);
            System.out.println("dt (run) = " + (date3.getTime() - date2.getTime()) / 1000.);
            System.out.println("dt (readFastData) = " + (date4.getTime() - date3.getTime()) / 1000.);
        }
        return list;
    }

    public TwissFastLists fastlistspass() throws Exception {
        Date date1 = new Date();
        generateInput();
        Date date2 = new Date();
        run();
        Date date3 = new Date();
        TwissFastLists lists = readFastLists();
        Date date4 = new Date();
        if (BENCH_MARK) {
            System.out.println("dt (generateInput) = " + (date2.getTime() - date1.getTime()) / 1000.);
            System.out.println("dt (run) = " + (date3.getTime() - date2.getTime()) / 1000.);
            System.out.println("dt (readFastData) = " + (date4.getTime() - date3.getTime()) / 1000.);
        }
        return lists;
    }

    public void generateInput() throws Exception {
        checkSequence();
        if (sequence instanceof gov.sns.xal.smf.Ring) {
            scenario = Scenario.newScenarioFor((gov.sns.xal.smf.Ring) sequence);
        } else {
            scenario = Scenario.newScenarioFor(sequence);
        }
        scenario.setSynchronizationMode(dataSource);
    }

    public void resync(String elemid) throws SynchronizationException {
        AcceleratorNode node = sequence.getNodeWithId(elemid);
        scenario.resync(node);
    }

    public void run() throws Exception {
        checkProbe();
        checkSequence();
        probe.reset();
        scenario.setProbe(probe);
        scenario.resync();
        Date t1 = new Date();
        scenario.run();
        Date t2 = new Date();
        if (BENCH_MARK) {
            System.out.println("SimulationManager.run dt(scenario.run) = " + (t2.getTime() - t1.getTime()) / 1000.);
        }
    }

    public abstract EditContext readData() throws Exception;

    public abstract List<TwissFastData> readFastData() throws Exception;

    public abstract TwissFastLists readFastLists() throws Exception;

    public static EditContext readData(String outputFile) throws Exception {
        EditContext editContext = new EditContext();
        XMLDataManager.readTableGroupFromUrl(editContext, "twiss", outputFile);
        return editContext;
    }

    private Lattice createLattice(AcceleratorSeq seq) {
        LatticeFactory factory = new LatticeFactory();
        factory.setDebug(false);
        factory.setVerbose(false);
        factory.setHalfMag(halfMag);
        Lattice lattice = null;
        try {
            lattice = factory.getLattice(seq);
            lattice.clearMarkers();
            lattice.joinDrifts();
        } catch (LatticeError lerr) {
            System.out.println(lerr.getMessage());
        }
        return lattice;
    }

    public Lattice getLattice() {
        return lattice;
    }

    public void setProbe(Probe newProbe) {
        probe = newProbe;
    }

    public void setSequence(AcceleratorSeq newSeq) throws Exception {
        sequence = newSeq;
        if (sequence != null) {
            lattice = createLattice(sequence);
        } else {
            throw new SimulationManagerException();
        }
    }

    public AcceleratorSeq getSequence() {
        return sequence;
    }

    public String getChannelSource() {
        return dataSource;
    }

    public void setChannelSource(String src) {
        dataSource = src;
    }
}
