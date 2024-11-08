package gov.sns.apps.virtualaccelerator;

import gov.sns.ca.Channel;
import gov.sns.xal.smf.*;
import gov.sns.xal.smf.impl.qualify.*;
import gov.sns.xal.smf.impl.*;
import java.io.*;
import java.util.*;
import com.cosylab.epics.caj.cas.util.DefaultServerImpl;
import com.cosylab.epics.caj.cas.util.MemoryProcessVariable;
import com.cosylab.epics.caj.cas.util.examples.CounterProcessVariable;
import gov.sns.ca.*;
import gov.aps.jca.CAException;
import gov.aps.jca.JCALibrary;
import gov.aps.jca.cas.ServerContext;
import gov.aps.jca.dbr.DBR_Double;

/** Server channel access */
public class VAServer {

    /** The sequence for which to server channels */
    private final AcceleratorSeq SEQUENCE;

    /** CA Server context */
    private final ServerContext CONTEXT;

    /** CA Server */
    private final DefaultServerImpl SERVER;

    /**
	 * Constructor
	 * @param sequence The sequence for which to serve channels
	 */
    public VAServer(final AcceleratorSeq sequence) throws Exception {
        SEQUENCE = sequence;
        JCALibrary jca = JCALibrary.getInstance();
        SERVER = new DefaultServerImpl();
        CONTEXT = jca.createServerContext(JCALibrary.CHANNEL_ACCESS_SERVER_JAVA, SERVER);
        System.out.println(CONTEXT.getVersion().getVersionString());
        CONTEXT.printInfo();
        registerNodeChannels();
    }

    /** dispose of the context */
    public void destroy() throws Exception {
        if (CONTEXT != null) {
            CONTEXT.destroy();
        }
    }

    /**
	 * Write the PCAS input file for the list of PVs in the usual nodes
	 * @param writer The writer to which to output the PCAS file
	 */
    private void registerNodeChannels() throws Exception {
        registerNodeChannels(Quadrupole.s_strType, AndTypeQualifier.qualifierWithQualifiers(new KindQualifier(Quadrupole.s_strType), new NotTypeQualifier(TrimmedQuadrupole.s_strType)));
        registerNodeChannels(TrimmedQuadrupole.s_strType);
        registerNodeChannels(Bend.s_strType);
        registerNodeChannels(Sextupole.s_strType);
        registerNodeChannels(HDipoleCorr.s_strType);
        registerNodeChannels(VDipoleCorr.s_strType);
        registerNodeChannels(ExtractionKicker.s_strType);
        registerNodeChannels(RfCavity.s_strType);
        registerNodeChannels(CurrentMonitor.s_strType);
        registerNodeChannels(BPM.s_strType);
        registerNodeChannels(BLM.s_strType);
        registerNodeChannels(ProfileMonitor.s_strType, AndTypeQualifier.qualifierWithQualifiers(new KindQualifier(ProfileMonitor.s_strType), QualifierFactory.getSoftTypeQualifier(ProfileMonitor.SOFTWARE_TYPE)));
        registerTimingSignals();
    }

    /**
	 * Write to the PCAS input file the list of PVs for the specified node type.
	 * @param type node type for which to register channels
	 */
    private void registerNodeChannels(final String type) throws IOException {
        registerNodeChannels(type, new KindQualifier(type));
    }

    /**
	 * Write to the PCAS input file the list of PVs for the specified node type.
	 * @param type node type for which to register channels
	 * @param nodeFilter a qualifier to filter which nodes to process
	 */
    private void registerNodeChannels(final String type, final TypeQualifier nodeFilter) throws IOException {
        final NodeSignalProcessor processor = NodeSignalProcessor.getInstance(type);
        final List<SignalEntry> signals = new ArrayList<SignalEntry>();
        final TypeQualifier qualifier = QualifierFactory.qualifierForQualifiers(true, nodeFilter);
        final List<AcceleratorNode> nodes = SEQUENCE.getAllInclusiveNodesWithQualifier(qualifier);
        for (AcceleratorNode node : nodes) {
            final Collection handles = processor.getHandlesToProcess(node);
            for (Iterator handleIter = handles.iterator(); handleIter.hasNext(); ) {
                final String handle = (String) handleIter.next();
                final Channel channel = node.getChannel(handle);
                if (channel != null) {
                    final String signal = channel.channelName();
                    final SignalEntry entry = new SignalEntry(signal, handle);
                    if (!signals.contains(entry)) {
                        signals.add(entry);
                    }
                }
            }
        }
        for (SignalEntry entry : signals) {
            final MemoryProcessVariable pv = processor.makePV(SERVER, entry);
        }
    }

    /**
	 * Write to the PCAS input file the list of timing signals.
	 */
    protected void registerTimingSignals() throws IOException {
        final TimingCenterProcessor processor = new TimingCenterProcessor();
        final List<SignalEntry> signals = new ArrayList<SignalEntry>();
        final TimingCenter timingCenter = SEQUENCE.getAccelerator().getTimingCenter();
        if (timingCenter == null) return;
        final Collection handles = processor.getHandlesToProcess(timingCenter);
        for (Iterator handleIter = handles.iterator(); handleIter.hasNext(); ) {
            final String handle = (String) handleIter.next();
            final Channel channel = timingCenter.getChannel(handle);
            if (channel != null) {
                final String signal = channel.channelName();
                final SignalEntry entry = new SignalEntry(signal, handle);
                if (!signals.contains(entry)) {
                    signals.add(entry);
                }
            }
        }
        for (SignalEntry entry : signals) {
            processor.makePV(SERVER, entry);
        }
    }
}

/** Default processor for a signal */
class SignalProcessor {

    /** size for array PVs */
    private static final int DEFAULT_ARRAY_SIZE = 1024;

    /** process the signal entry */
    public MemoryProcessVariable makePV(final DefaultServerImpl server, final SignalEntry entry) {
        final String signalName = entry.getSignal();
        final int size = entry.getSignal().matches(".*(TBT|A)") ? DEFAULT_ARRAY_SIZE : 1;
        final MemoryProcessVariable mpv = new MemoryProcessVariable(signalName, null, DBR_Double.TYPE, new double[size]);
        mpv.setUnits("units");
        appendLimits(entry, mpv);
        if (size == 1) {
            final String[] warningPVs = ChannelFactory.defaultFactory().getChannel(signalName).getWarningLimitPVs();
            server.registerProcessVaribale(new MemoryProcessVariable(warningPVs[0], null, DBR_Double.TYPE, new double[1]));
            server.registerProcessVaribale(new MemoryProcessVariable(warningPVs[1], null, DBR_Double.TYPE, new double[1]));
            final String[] alarmPVs = ChannelFactory.defaultFactory().getChannel(signalName).getAlarmLimitPVs();
            server.registerProcessVaribale(new MemoryProcessVariable(alarmPVs[0], null, DBR_Double.TYPE, new double[1]));
            server.registerProcessVaribale(new MemoryProcessVariable(alarmPVs[1], null, DBR_Double.TYPE, new double[1]));
            final String[] operationLimitPVs = ChannelFactory.defaultFactory().getChannel(signalName).getOperationLimitPVs();
            server.registerProcessVaribale(new MemoryProcessVariable(operationLimitPVs[0], null, DBR_Double.TYPE, new double[] { mpv.getLowerDispLimit().doubleValue() }));
            server.registerProcessVaribale(new MemoryProcessVariable(operationLimitPVs[1], null, DBR_Double.TYPE, new double[] { mpv.getUpperDispLimit().doubleValue() }));
            final String[] driveLimitPVs = ChannelFactory.defaultFactory().getChannel(signalName).getDriveLimitPVs();
            server.registerProcessVaribale(new MemoryProcessVariable(driveLimitPVs[0], null, DBR_Double.TYPE, new double[] { mpv.getLowerCtrlLimit().doubleValue() }));
            server.registerProcessVaribale(new MemoryProcessVariable(driveLimitPVs[1], null, DBR_Double.TYPE, new double[] { mpv.getUpperCtrlLimit().doubleValue() }));
        }
        server.registerProcessVaribale(mpv);
        return mpv;
    }

    protected void appendLimits(final SignalEntry entry, final MemoryProcessVariable mpv) {
    }

    protected void setLimits(final MemoryProcessVariable mpv, final double lowerLimit, final double upperLimit) {
        mpv.setLowerDispLimit(lowerLimit);
        mpv.setUpperDispLimit(upperLimit);
        mpv.setLowerAlarmLimit(lowerLimit);
        mpv.setUpperAlarmLimit(upperLimit);
        mpv.setLowerCtrlLimit(lowerLimit);
        mpv.setUpperCtrlLimit(upperLimit);
        mpv.setLowerWarningLimit(lowerLimit);
        mpv.setUpperWarningLimit(upperLimit);
    }
}

/**
 * Signal processor class for nodes.
 */
class NodeSignalProcessor extends SignalProcessor {

    /**
	 * Get the appropriate processor instance for the specified node type
	 * @param type The type of node for which to process the signals
	 * @return An instance of SignalProcessor or a subclass appropriate for the node type
	 */
    public static NodeSignalProcessor getInstance(final String type) {
        if (type == Quadrupole.s_strType || type == Bend.s_strType) return new UnipolarEMProcessor(); else if (type == TrimmedQuadrupole.s_strType) return new TrimmedQuadrupoleProcessor(); else if (type == BPM.s_strType) return new BPMProcessor(); else if (type == VDipoleCorr.s_strType || type == HDipoleCorr.s_strType) return new DipoleCorrectorProcessor(); else if (type == Sextupole.s_strType) return new SextupoleProcessor(); else if (type == ProfileMonitor.s_strType) return new ProfileMonitorProcessor(); else return new NodeSignalProcessor();
    }

    /**
	 * Get the handles we wish to process for a node.  By default we process all of a node's handles.  Subclasses may wish to override this method to return only a subset of handles.
	 * @param node The node whose handles we wish to get
	 * @return the collection of the node's handles we wish to process
	 */
    public Collection getHandlesToProcess(final AcceleratorNode node) {
        return node.getHandles();
    }
}

/** Implement the processor for the ProfileMonitor.  This class returns only the X and Y sigma M handles. */
class ProfileMonitorProcessor extends NodeSignalProcessor {

    private static final Collection HANDLES;

    static {
        HANDLES = new ArrayList();
        HANDLES.add(ProfileMonitor.H_SIGMA_M_HANDLE);
        HANDLES.add(ProfileMonitor.V_SIGMA_M_HANDLE);
    }

    /**
	 * Get the handles we wish to process for a node.  This processor overrides this method to return only the handles of interest for the node.
	 * @return the collection of the node's handles we wish to process
	 */
    public Collection getHandlesToProcess(final AcceleratorNode node) {
        return HANDLES;
    }
}

/** Signal processor appropriate for processing sextupole electro magnets */
class SextupoleProcessor extends NodeSignalProcessor {

    private static final Set LIMIT_HANDLES;

    static {
        LIMIT_HANDLES = new HashSet();
        LIMIT_HANDLES.add(Electromagnet.FIELD_RB_HANDLE);
        LIMIT_HANDLES.add(MagnetMainSupply.FIELD_SET_HANDLE);
        LIMIT_HANDLES.add(MagnetMainSupply.FIELD_RB_HANDLE);
    }

    protected void appendLimits(final SignalEntry entry, final MemoryProcessVariable mpv) {
        if (LIMIT_HANDLES.contains(entry.getHandle())) {
            setLimits(mpv, -10.0, 10.0);
        }
    }
}

/** Signal processor appropriate for processing unipolar electro magnets */
class UnipolarEMProcessor extends NodeSignalProcessor {

    private static final Set LIMIT_HANDLES;

    static {
        LIMIT_HANDLES = new HashSet();
        LIMIT_HANDLES.add(Electromagnet.FIELD_RB_HANDLE);
        LIMIT_HANDLES.add(MagnetMainSupply.FIELD_SET_HANDLE);
        LIMIT_HANDLES.add(MagnetMainSupply.FIELD_RB_HANDLE);
        LIMIT_HANDLES.add(MagnetMainSupply.FIELD_BOOK_HANDLE);
    }

    protected void appendLimits(final SignalEntry entry, final MemoryProcessVariable mpv) {
        if (LIMIT_HANDLES.contains(entry.getHandle())) {
            setLimits(mpv, 0.0, 50.0);
        }
    }
}

/** Signal processor appropriate for processing trimmed quadrupoles */
class TrimmedQuadrupoleProcessor extends NodeSignalProcessor {

    private static final Set MAIN_LIMIT_HANDLES;

    private static final Set TRIM_LIMIT_HANDLES;

    static {
        MAIN_LIMIT_HANDLES = new HashSet();
        MAIN_LIMIT_HANDLES.add(Electromagnet.FIELD_RB_HANDLE);
        MAIN_LIMIT_HANDLES.add(MagnetMainSupply.FIELD_SET_HANDLE);
        MAIN_LIMIT_HANDLES.add(MagnetMainSupply.FIELD_RB_HANDLE);
        MAIN_LIMIT_HANDLES.add(MagnetMainSupply.FIELD_BOOK_HANDLE);
        TRIM_LIMIT_HANDLES = new HashSet();
        TRIM_LIMIT_HANDLES.add(MagnetTrimSupply.FIELD_RB_HANDLE);
        TRIM_LIMIT_HANDLES.add(MagnetTrimSupply.FIELD_SET_HANDLE);
    }

    protected void appendLimits(final SignalEntry entry, final MemoryProcessVariable mpv) {
        if (MAIN_LIMIT_HANDLES.contains(entry.getHandle())) {
            setLimits(mpv, 0.0, 50.0);
        } else if (TRIM_LIMIT_HANDLES.contains(entry.getHandle())) {
            setLimits(mpv, -1.0, 1.0);
        }
    }
}

/** Signal processor appropriate for processing BPMs */
class BPMProcessor extends NodeSignalProcessor {

    protected void appendLimits(final SignalEntry entry, final MemoryProcessVariable mpv) {
        final String handle = entry.getHandle();
        if (BPM.AMP_AVG_HANDLE.equals(handle)) {
            setLimits(mpv, 0.0, 50.0);
        } else {
            setLimits(mpv, -1000.0, 1000.0);
        }
    }
}

/** Signal processor appropriate for processing bends */
class BendProcessor extends NodeSignalProcessor {

    private static final Set LIMIT_HANDLES;

    /**
	 * Static initializer for setting constant values
	 */
    static {
        LIMIT_HANDLES = new HashSet();
        LIMIT_HANDLES.add(Electromagnet.FIELD_RB_HANDLE);
        LIMIT_HANDLES.add(MagnetMainSupply.FIELD_SET_HANDLE);
        LIMIT_HANDLES.add(MagnetMainSupply.FIELD_RB_HANDLE);
    }

    protected void appendLimits(final SignalEntry entry, final MemoryProcessVariable mpv) {
        if (LIMIT_HANDLES.contains(entry.getHandle())) {
            setLimits(mpv, -1.5, 1.5);
        }
    }
}

/** Signal processor appropriate for processing dipole correctors */
class DipoleCorrectorProcessor extends NodeSignalProcessor {

    private static final Set LIMIT_HANDLES;

    /**
	 * Static initializer for setting constant values
	 */
    static {
        LIMIT_HANDLES = new HashSet();
        LIMIT_HANDLES.add(Electromagnet.FIELD_RB_HANDLE);
        LIMIT_HANDLES.add(MagnetMainSupply.FIELD_SET_HANDLE);
        LIMIT_HANDLES.add(MagnetMainSupply.FIELD_RB_HANDLE);
    }

    protected void appendLimits(final SignalEntry entry, final MemoryProcessVariable mpv) {
        if (LIMIT_HANDLES.contains(entry.getHandle())) {
            setLimits(mpv, -0.01, 0.01);
        }
    }
}

/** Implement the processor for the TimingCenter. */
class TimingCenterProcessor extends SignalProcessor {

    /**
	 * Get the handles from the TimingCenter.
	 * @param timingCenter The timing center whose handles we wish to get
	 * @return the collection of the node's handles we wish to process
	 */
    public Collection getHandlesToProcess(final TimingCenter timingCenter) {
        return timingCenter.getHandles();
    }
}

/** Signal/handle pair */
final class SignalEntry {

    protected final String _signal;

    protected final String _handle;

    /**
	 * Constructor
	 */
    public SignalEntry(final String signal, final String handle) {
        _signal = signal;
        _handle = handle;
    }

    /**
	 * Get the signal
	 * @return the signal
	 */
    public final String getSignal() {
        return _signal;
    }

    /**
	 * Get the handle
	 * @return the handle
	 */
    public final String getHandle() {
        return _handle;
    }

    /**
	 * Two signal entries are equal if they have the same signal
	 * @param anObject The signal entry against which to compare
	 * @return true if the two signal entries have the same signal
	 */
    public final boolean equals(final Object anObject) {
        return _signal.equals(((SignalEntry) anObject).getSignal());
    }
}
