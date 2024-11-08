package gov.sns.xal.tools.virtualaccelerator;

import gov.sns.ca.Channel;
import gov.sns.xal.smf.*;
import gov.sns.xal.smf.impl.qualify.*;
import gov.sns.xal.smf.impl.*;
import java.io.*;
import java.util.*;

/**
 * PCASGenerator is used to generate a Portable Channel Access Server input file
 * from an accelerator sequence.
 *
 * @author  tap
 */
public class PCASGenerator {

    /** The sequence for which to generate a PCAS file */
    protected AcceleratorSeq _sequence;

    /**
	 * Constructor
	 * @param sequence The sequence for which to generate a PCAS file
	 */
    public PCASGenerator(AcceleratorSeq sequence) {
        _sequence = sequence;
    }

    /**
	 * Write the PCAS input file for the list of PVs in the usual nodes
	 * @param writer The writer to which to output the PCAS file
	 */
    public void processNodes(Writer writer) throws IOException {
        processNodes(writer, Quadrupole.s_strType, AndTypeQualifier.qualifierWithQualifiers(new KindQualifier(Quadrupole.s_strType), new NotTypeQualifier(TrimmedQuadrupole.s_strType)));
        processNodes(writer, TrimmedQuadrupole.s_strType);
        processNodes(writer, Bend.s_strType);
        processNodes(writer, HDipoleCorr.s_strType);
        processNodes(writer, VDipoleCorr.s_strType);
        processNodes(writer, RfCavity.s_strType);
        processNodes(writer, BPM.s_strType);
        processNodes(writer, BLM.s_strType);
        processNodes(writer, ProfileMonitor.s_strType);
        processTimingSignals(writer);
    }

    /**
	 * Write to the PCAS input file the list of PVs for the specified node type.
	 * @param writer The writer to which to output the PCAS file
	 */
    protected void processNodes(final Writer writer, final String type) throws IOException {
        processNodes(writer, type, new KindQualifier(type));
    }

    /**
	 * Write to the PCAS input file the list of PVs for the specified node type.
	 * @param writer The writer to which to output the PCAS file
	 * @param nodeFilter a qualifier to filter which nodes to process
	 */
    protected void processNodes(final Writer writer, final String type, final TypeQualifier nodeFilter) throws IOException {
        final NodeSignalProcessor processor = NodeSignalProcessor.getInstance(type);
        final List<SignalEntry> signals = new ArrayList<SignalEntry>();
        final TypeQualifier qualifier = QualifierFactory.qualifierForQualifiers(true, nodeFilter);
        final List<AcceleratorNode> nodes = _sequence.getAllInclusiveNodesWithQualifier(qualifier);
        for (AcceleratorNode node : nodes) {
            final Collection<String> handles = processor.getHandlesToProcess(node);
            for (Iterator<String> handleIter = handles.iterator(); handleIter.hasNext(); ) {
                final String handle = handleIter.next();
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
            final String line = processor.process(type, entry);
            writer.write(line);
            writer.write('\n');
        }
    }

    /**
	 * Write to the PCAS input file the list of timing signals.
	 * @param writer The writer to which to output the PCAS file
	 */
    protected void processTimingSignals(final Writer writer) throws IOException {
        final TimingCenterProcessor processor = new TimingCenterProcessor();
        final List<SignalEntry> signals = new ArrayList<SignalEntry>();
        final TimingCenter timingCenter = _sequence.getAccelerator().getTimingCenter();
        if (timingCenter == null) return;
        final Collection<String> handles = processor.getHandlesToProcess(timingCenter);
        for (Iterator<String> handleIter = handles.iterator(); handleIter.hasNext(); ) {
            final String handle = handleIter.next();
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
            final String line = processor.process(entry);
            writer.write(line);
            writer.write('\n');
        }
    }
}

/**
 * Default processor for a signal
 */
class SignalProcessor {

    /**
	 * Generate the line for the specified type and signal entry
	 * @param entry The handle/signal pair
	 * @return the line for the PV to appear in the PCAS input file
	 */
    public String process(final SignalEntry entry) {
        String line = appendSize("name = " + entry.getSignal(), entry);
        return appendLimits(line, entry);
    }

    /**
	 * Generate a label for the lower limit portion of the line
	 * @param limit The lower limit
	 * @return lower limit portion of the line
	 */
    public static String getLowLimitLabel(final double limit) {
        return " lowCtrlLim = " + limit;
    }

    /**
	 * Generate a label for the upper limit portion of the line
	 * @param limit The upper limit
	 * @return upper limit portion of the line
	 */
    public static String getUpperLimitLabel(final double limit) {
        return " uppCtrlLim = " + limit;
    }

    /**
	 * Append the limits portion to the line.  The handle is used to filter which
	 * lines should have limits.  The default version simply returns the line
	 * with no limits appended.
	 * @param line to which to append the limits
	 * @param entry The signal/handle entry
	 */
    protected String appendLimits(final String line, final SignalEntry entry) {
        return line;
    }

    /**
	 * Append the size portion to the line.  The size specifies the length of the array data for a PV.
	 * Array PVs are identified by whether they end in "TBT" or "A".
	 * @param line to which to append the limits
	 * @param entry The signal/handle entry
	 */
    protected String appendSize(final String line, final SignalEntry entry) {
        boolean isArray = entry.getSignal().matches(".*(TBT|A)");
        return isArray ? line + " size = 256" : line;
    }
}

/**
 * Signal processor class for nodes.
 */
class NodeSignalProcessor extends SignalProcessor {

    /**
	 * Generate the line for the specified type and signal entry
	 * @param type The type of node for which to generate the line
	 * @param entry The handle/signal pair
	 * @return the line for the PV to appear in the PCAS input file
	 */
    public String process(final String type, final SignalEntry entry) {
        String line = appendSize("name = " + entry.getSignal(), entry);
        return appendLimits(line, entry);
    }

    /**
	 * Get the appropriate processor instance for the specified node type
	 * @param type The type of node for which to process the signals
	 * @return An instance of SignalProcessor or a subclass appropriate for the node type
	 */
    public static NodeSignalProcessor getInstance(final String type) {
        if (type == Quadrupole.s_strType || type == Bend.s_strType) return new UnipolarEMProcessor(); else if (type == TrimmedQuadrupole.s_strType) return new TrimmedQuadrupoleProcessor(); else if (type == BPM.s_strType) return new BPMProcessor(); else if (type == VDipoleCorr.s_strType || type == HDipoleCorr.s_strType) return new DipoleCorrectorProcessor(); else if (type == ProfileMonitor.s_strType) return new ProfileMonitorProcessor(); else return new NodeSignalProcessor();
    }

    /**
	 * Get the handles we wish to process for a node.  By default we process all
	 * of a node's handles.  Subclasses may wish to override this method to 
	 * return only a subset of handles.
	 * @param node The node whose handles we wish to get
	 * @return the collection of the node's handles we wish to process
	 */
    public Collection<String> getHandlesToProcess(final AcceleratorNode node) {
        return node.getHandles();
    }
}

/**
 * Implement the processor for the ProfileMonitor.  This class returns only the X and Y sigma M handles.
 */
class ProfileMonitorProcessor extends NodeSignalProcessor {

    static final Collection<String> HANDLES;

    /**
	 * static initializer
	 */
    static {
        HANDLES = new ArrayList<String>();
        HANDLES.add(ProfileMonitor.H_SIGMA_M_HANDLE);
        HANDLES.add(ProfileMonitor.V_SIGMA_M_HANDLE);
    }

    /**
	 * Get the handles we wish to process for a node.  This processor overrides this method
	 * to return only the handles of interest for the node.
	 * @return the collection of the node's handles we wish to process
	 */
    @Override
    public Collection<String> getHandlesToProcess(final AcceleratorNode node) {
        return HANDLES;
    }
}

/**
 * Signal processor appropriate for processing unipolar electro magnets
 */
class UnipolarEMProcessor extends NodeSignalProcessor {

    static final Set<String> LIMIT_HANDLES;

    static final String LOW_LIMIT_LABEL;

    static final String UPPER_LIMIT_LABEL;

    static {
        LOW_LIMIT_LABEL = getLowLimitLabel(0.0);
        UPPER_LIMIT_LABEL = getUpperLimitLabel(50.0);
        LIMIT_HANDLES = new HashSet<String>();
        LIMIT_HANDLES.add(Electromagnet.FIELD_RB_HANDLE);
        LIMIT_HANDLES.add(MagnetMainSupply.FIELD_SET_HANDLE);
        LIMIT_HANDLES.add(MagnetMainSupply.FIELD_RB_HANDLE);
    }

    /**
	 * Append the limits portion to the line.  The handle is used to filter which
	 * lines should have limits.
	 * @param line to which to append the limits
	 * @param entry The signal/handle entry
	 */
    @Override
    protected String appendLimits(final String line, final SignalEntry entry) {
        return (LIMIT_HANDLES.contains(entry.getHandle())) ? line + LOW_LIMIT_LABEL + UPPER_LIMIT_LABEL : line;
    }
}

/** Signal processor appropriate for processing trimmed quadrupoles */
class TrimmedQuadrupoleProcessor extends NodeSignalProcessor {

    static final Set<String> MAIN_LIMIT_HANDLES;

    static final String LOW_MAIN_LIMIT_LABEL;

    static final String UPPER_MAIN_LIMIT_LABEL;

    static final Set<String> TRIM_LIMIT_HANDLES;

    static final String LOW_TRIM_LIMIT_LABEL;

    static final String UPPER_TRIM_LIMIT_LABEL;

    static {
        LOW_MAIN_LIMIT_LABEL = getLowLimitLabel(0.0);
        UPPER_MAIN_LIMIT_LABEL = getUpperLimitLabel(50.0);
        LOW_TRIM_LIMIT_LABEL = getLowLimitLabel(-1.0);
        UPPER_TRIM_LIMIT_LABEL = getUpperLimitLabel(1.0);
        MAIN_LIMIT_HANDLES = new HashSet<String>();
        MAIN_LIMIT_HANDLES.add(Electromagnet.FIELD_RB_HANDLE);
        MAIN_LIMIT_HANDLES.add(MagnetMainSupply.FIELD_SET_HANDLE);
        MAIN_LIMIT_HANDLES.add(MagnetMainSupply.FIELD_RB_HANDLE);
        TRIM_LIMIT_HANDLES = new HashSet<String>();
        TRIM_LIMIT_HANDLES.add(MagnetTrimSupply.FIELD_RB_HANDLE);
        TRIM_LIMIT_HANDLES.add(MagnetTrimSupply.FIELD_SET_HANDLE);
    }

    /**
	 * Append the limits portion to the line.  The handle is used to filter which lines should have limits.
	 * @param line to which to append the limits
	 * @param entry The signal/handle entry
	 */
    @Override
    protected String appendLimits(final String line, final SignalEntry entry) {
        if (MAIN_LIMIT_HANDLES.contains(entry.getHandle())) {
            return line + LOW_MAIN_LIMIT_LABEL + UPPER_MAIN_LIMIT_LABEL;
        } else if (TRIM_LIMIT_HANDLES.contains(entry.getHandle())) {
            return line + LOW_TRIM_LIMIT_LABEL + UPPER_TRIM_LIMIT_LABEL;
        } else {
            return line;
        }
    }
}

/**
 * Signal processor appropriate for processing BPMs
 */
class BPMProcessor extends NodeSignalProcessor {

    static final Set<String> LIMIT_HANDLES;

    static final String LOW_LIMIT_LABEL;

    static final String UPPER_LIMIT_LABEL;

    /**
	 * Static initializer for setting constant values
	 */
    static {
        LOW_LIMIT_LABEL = getLowLimitLabel(0.0);
        UPPER_LIMIT_LABEL = getUpperLimitLabel(50.0);
        LIMIT_HANDLES = new HashSet<String>();
        LIMIT_HANDLES.add(BPM.AMP_AVG_HANDLE);
    }

    /**
	 * Append the limits portion to the line.  The handle is used to filter which
	 * lines should have limits.
	 * @param line to which to append the limits
	 * @param entry The signal/handle entry
	 */
    @Override
    protected String appendLimits(final String line, final SignalEntry entry) {
        return (LIMIT_HANDLES.contains(entry.getHandle())) ? line + LOW_LIMIT_LABEL + UPPER_LIMIT_LABEL : line;
    }
}

/**
 * Signal processor appropriate for processing bends
 */
class BendProcessor extends NodeSignalProcessor {

    static final Set<String> LIMIT_HANDLES;

    static final String LOW_LIMIT_LABEL;

    static final String UPPER_LIMIT_LABEL;

    /**
		* Static initializer for setting constant values
	 */
    static {
        LOW_LIMIT_LABEL = getLowLimitLabel(-1.5);
        UPPER_LIMIT_LABEL = getUpperLimitLabel(1.5);
        LIMIT_HANDLES = new HashSet<String>();
        LIMIT_HANDLES.add(Electromagnet.FIELD_RB_HANDLE);
        LIMIT_HANDLES.add(MagnetMainSupply.FIELD_SET_HANDLE);
        LIMIT_HANDLES.add(MagnetMainSupply.FIELD_RB_HANDLE);
    }

    /**
		* Append the limits portion to the line.  The handle is used to filter which
	 * lines should have limits.
	 * @param line to which to append the limits
	 * @param entry The signal/handle entry
	 */
    @Override
    protected String appendLimits(final String line, final SignalEntry entry) {
        return (LIMIT_HANDLES.contains(entry.getHandle())) ? line + LOW_LIMIT_LABEL + UPPER_LIMIT_LABEL : line;
    }
}

/**
 * Signal processor appropriate for processing dipole correctors
 */
class DipoleCorrectorProcessor extends NodeSignalProcessor {

    static final Set<String> LIMIT_HANDLES;

    static final String LOW_LIMIT_LABEL;

    static final String UPPER_LIMIT_LABEL;

    /**
	 * Static initializer for setting constant values
	 */
    static {
        LOW_LIMIT_LABEL = getLowLimitLabel(-1);
        UPPER_LIMIT_LABEL = getUpperLimitLabel(1);
        LIMIT_HANDLES = new HashSet<String>();
        LIMIT_HANDLES.add(Electromagnet.FIELD_RB_HANDLE);
        LIMIT_HANDLES.add(MagnetMainSupply.FIELD_SET_HANDLE);
        LIMIT_HANDLES.add(MagnetMainSupply.FIELD_RB_HANDLE);
    }

    /**
	 * Append the limits portion to the line.  The handle is used to filter which
	 * lines should have limits.
	 * @param line to which to append the limits
	 * @param entry The signal/handle entry
	 */
    @Override
    protected String appendLimits(final String line, final SignalEntry entry) {
        return (LIMIT_HANDLES.contains(entry.getHandle())) ? line + LOW_LIMIT_LABEL + UPPER_LIMIT_LABEL : line;
    }
}

/**
 * Implement the processor for the TimingCenter.
 */
class TimingCenterProcessor extends SignalProcessor {

    /**
	 * Get the handles from the TimingCenter.
	 * @param timingCenter The timing center whose handles we wish to get
	 * @return the collection of the node's handles we wish to process
	 */
    public Collection<String> getHandlesToProcess(final TimingCenter timingCenter) {
        return timingCenter.getHandles();
    }
}

/**
 * Signal/handle pair
 */
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
    @Override
    public final boolean equals(final Object anObject) {
        return _signal.equals(((SignalEntry) anObject).getSignal());
    }
}
