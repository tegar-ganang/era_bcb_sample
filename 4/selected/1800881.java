package gov.sns.apps.energymanager;

import gov.sns.xal.smf.*;
import gov.sns.xal.smf.impl.*;
import gov.sns.xal.smf.proxy.RfCavityPropertyAccessor;
import gov.sns.ca.Channel;
import gov.sns.tools.data.*;
import java.util.*;

/** Wrap an RF Cavity node. */
public class RFCavityAgent extends NodeAgent {

    static final double DEGREES_TO_RADIANS = Math.PI / 180.0;

    public static final ParameterTypeAdaptor AMPLITUDE_ADAPTOR;

    public static final ParameterTypeAdaptor PHASE_ADAPTOR;

    protected final int AMPLITUDE_INDEX = 0;

    protected final int PHASE_INDEX = 1;

    static {
        AMPLITUDE_ADAPTOR = new AmplitudeAdaptor();
        PHASE_ADAPTOR = new PhaseAdaptor();
    }

    /** Constructor */
    public RFCavityAgent(final AcceleratorSeq sequence, final RfCavity node, final ParameterStore parameterStore) {
        super(sequence, node, parameterStore);
    }

    /** populate live parameters */
    @Override
    protected void populateLiveParameters(final ParameterStore parameterStore) {
        _liveParameters = new ArrayList<LiveParameter>(2);
        _liveParameters.add(parameterStore.addLiveParameter(this, AMPLITUDE_ADAPTOR));
        _liveParameters.add(parameterStore.addLiveParameter(this, PHASE_ADAPTOR));
    }

    /** Pick a phase preserving the design focusing by preserving the product of amplitude and effective sine against that of the design product. */
    public void preserveDesignFocusingWithPhase() {
        final LiveParameter amplitudeParameter = getLiveParameter(AMPLITUDE_INDEX);
        final double amplitude = amplitudeParameter.getCustomValue();
        final double designAmplitude = amplitudeParameter.getDesignValue();
        if (amplitude == 0 || amplitude == designAmplitude) return;
        final RfCavity cavity = (RfCavity) getNode();
        final double designPhase = cavity.getDfltCavPhase();
        final double designEffectiveSinePhase = toEffectiveSinePhase(designAmplitude, designPhase, cavity);
        final double effectivePhase = Math.asin(designAmplitude * designEffectiveSinePhase / amplitude) / DEGREES_TO_RADIANS;
        double entrancePhase = effectivePhase + cavity.getDfltCavPhase() - Math.asin(designEffectiveSinePhase) / DEGREES_TO_RADIANS;
        for (int index = 0; index < 3; index++) {
            entrancePhase = effectivePhase + entrancePhase - toEffectivePhase(amplitude, entrancePhase, cavity);
        }
        if (!Double.isNaN(entrancePhase) && !Double.isInfinite(entrancePhase)) {
            getLiveParameter(PHASE_INDEX).setCustomValue(entrancePhase);
        }
    }

    /**
	 * Convert the entrance cavity phase to effective sine cavity phase (focussing component).
	 * @param cavityAmplitude the amplitude of the cavity
	 * @param cavityPhase the phase at the start of the cavity.
	 * @param cavity the RF cavity
	 * @return the effective sine phase for the cavity
	 */
    protected static double toEffectiveSinePhase(final double cavityAmplitude, final double cavityPhase, final RfCavity cavity) {
        double sum = 0.0;
        final Iterator gapIter = cavity.getGaps().iterator();
        while (gapIter.hasNext()) {
            final RfGap gap = (RfGap) gapIter.next();
            final double gapPhase = gap.toGapPhaseFromCavityPhase(cavityPhase) * DEGREES_TO_RADIANS;
            final double gapAmplitude = gap.toGapAmpFromCavityAmp(cavityAmplitude);
            sum += gapAmplitude * gap.getGapLength() * Math.sin(gapPhase);
        }
        return sum / (cavity.getLength() * cavityAmplitude);
    }

    /**
	 * Convert the entrance cavity phase to effective cavity phase based on sine phase averaging (focussing component).
	 * @param cavityAmplitude the amplitude of the cavity
	 * @param cavityPhase the phase at the start of the cavity.
	 * @param cavity the RF cavity
	 * @return the effective phase for the cavity
	 */
    protected static double toEffectivePhase(final double cavityAmplitude, final double cavityPhase, final RfCavity cavity) {
        return Math.asin(toEffectiveSinePhase(cavityAmplitude, cavityPhase, cavity)) / DEGREES_TO_RADIANS;
    }

    /**
	 * Estimate the starting (cavity) phase from the the average cavity phase.  Assumes phase slip same as design.
	 * @param averagePhase the average cavity phase in degrees
	 * @param cavityAmplitude the cavity amplitude
	 * @return the cavity start phase in degrees
	 */
    public double toCavityPhaseFromAverage(final double averagePhase, final double cavityAmplitude) {
        return toCavityPhaseFromAverage(averagePhase, cavityAmplitude, (RfCavity) getNode());
    }

    /**
	 * Estimate the starting (cavity) phase from the the average cavity phase.  Assumes phase slip same as design.
	 * @param averagePhase the average cavity phase
	 * @param cavityAmplitude the cavity amplitude
	 * @param cavity the RF cavity
	 * @return the cavity start phase in degrees
	 */
    protected static double toCavityPhaseFromAverage(final double averagePhase, final double cavityAmplitude, final RfCavity cavity) {
        double cavityPhase = averagePhase;
        int iter = 0;
        double phaseError = 0.0;
        do {
            phaseError = averagePhase - toAveragePhase(cavityPhase, cavityAmplitude, cavity);
            cavityPhase += phaseError;
            ++iter;
        } while (Math.abs(phaseError) > 0.1 && iter < 5);
        System.out.println(cavity.getId() + " Cavity phase error:  " + phaseError + ", iter:  " + iter + ", average phase:  " + averagePhase + ", cavity phase:  " + cavityPhase);
        return cavityPhase;
    }

    /**
	 * Get the average cavity phase from the entrance phase.  Assumes phase slip is same as design.
	 * @param cavityPhase the cavity entrance phase
	 * @param cavityAmplitude the cavity amplitude
	 * @param cavity the RF Cavity
	 * @return the average cavity phase in degrees
	 */
    protected static double toAveragePhase(final double cavityPhase, final double cavityAmplitude, final RfCavity cavity) {
        double sum = 0.0;
        final Iterator gapIter = cavity.getGaps().iterator();
        while (gapIter.hasNext()) {
            final RfGap gap = (RfGap) gapIter.next();
            final double gapPhase = gap.toGapPhaseFromCavityPhase(cavityPhase);
            final double gapAmplitude = gap.toGapAmpFromCavityAmp(cavityAmplitude);
            sum += gapAmplitude * gap.getGapLength() * gapPhase;
        }
        return sum / (cavity.getLength() * cavityAmplitude);
    }

    /**
	 * Get the amplitude parameter.
	 * @return the amplitude parameter
	 */
    public LiveParameter getAmplitudeParameter() {
        return getLiveParameter(AMPLITUDE_INDEX);
    }

    /**
	 * Get the phase parameter.
	 * @return the phase parameter
	 */
    public LiveParameter getPhaseParameter() {
        return getLiveParameter(PHASE_INDEX);
    }

    /**
	 * Export optics changes using the exporter.
	 * @param exporter the optics exporter to use for exporting this node's optics changes
	 */
    @Override
    public void exportOpticsChanges(final OpticsExporter exporter) {
        final List changedParameterIndices = new ArrayList();
        for (int index = 0; index < 2; index++) {
            final LiveParameter parameter = getLiveParameter(index);
            if (parameter.getDesignValue() != parameter.getInitialValue()) {
                changedParameterIndices.add(new Integer(index));
            }
        }
        if (!changedParameterIndices.isEmpty()) {
            final DataAdaptor adaptor = exporter.getChildAdaptor(getNode().getParent(), getNode().dataLabel());
            adaptor.setValue("id", getNode().getId());
            final DataAdaptor attributesAdaptor = adaptor.createChild("attributes");
            final DataAdaptor cavityAdaptor = attributesAdaptor.createChild("rfcavity");
            final Iterator paramIndexIter = changedParameterIndices.iterator();
            while (paramIndexIter.hasNext()) {
                final int paramIndex = ((Number) paramIndexIter.next()).intValue();
                switch(paramIndex) {
                    case AMPLITUDE_INDEX:
                        cavityAdaptor.setValue("amp", getLiveParameter(AMPLITUDE_INDEX).getInitialValue());
                        break;
                    case PHASE_INDEX:
                        cavityAdaptor.setValue("phase", getLiveParameter(PHASE_INDEX).getInitialValue());
                        break;
                    default:
                        break;
                }
            }
        }
    }

    /**
	 * Write the header for live parameters.
	 * @param writer the writer to which the header should be written
	 */
    public static void exportParameterHeader(final java.io.Writer writer) throws java.io.IOException {
        writer.write("\n########## \n");
        writer.write("# RF Cavities \n");
        writer.write("# Cavity  \tAmplitude(MV/m)  \tPhase(degrees)  \tAverage Phase(degrees) \n");
    }
}

/** AmplitudeAdaptor */
class AmplitudeAdaptor implements ParameterTypeAdaptor {

    /**
	 * Get an identifier of this parameter type.
	 * @return a unique string identifying this parameter type.
	 */
    public String getID() {
        return "RF Amplitude Adaptor";
    }

    /**
	 * Get the parameter name.
	 * @return the parameter name.
	 */
    public String getName() {
        return "Amplitude";
    }

    /**
	 * Get the design value for the parameter.
	 * @param nodeAgent the node agent from which to get the design value.
	 * @return the design value
	 */
    public double getDesignValue(final NodeAgent nodeAgent) {
        return ((RfCavity) nodeAgent.getNode()).getDfltCavAmp();
    }

    /**
	 * Get the lower and upper design limits for the parameter.  Assume 20% cushion from design value.
	 * @param nodeAgent the node agent from which to get the design limits.
	 * @return the design limits array [lower, upper] in that order
	 */
    public double[] getDesignLimits(NodeAgent nodeAgent, final double designValue) {
        return new double[] { 0.0, 1.2 * designValue };
    }

    /**
	 * Get the readback channel for the specified node.
	 * @param nodeAgent the node agent for which to get the readback channel.
	 * @return the readback channel.
	 */
    public Channel getReadbackChannel(final NodeAgent nodeAgent) {
        return nodeAgent.getNode().getChannel(RfCavity.CAV_AMP_AVG_HANDLE);
    }

    /**
	 * Get the control channel for the specified node.
	 * @param nodeAgent the node agent for which to get the control channel.
	 */
    public Channel getControlChannel(final NodeAgent nodeAgent) {
        return nodeAgent.getNode().getChannel(RfCavity.CAV_AMP_SET_HANDLE);
    }

    /**
	 * Upload a value to the control system.
	 * @param parameter the parameter whose initial value should be uploaded.
	 * @param value the value to upload
	 * @return true if the request is successful and false if not
	 */
    public boolean uploadValue(final LiveParameter parameter, final double value) {
        throw new UnsupportedOperationException("Uploading values to the control system is unsupported for RF Amplitude.");
    }

    /**
	 * Get the online model property accessor for the parameter.
	 * @return the parameter's online model property accessor
	 */
    public String getPropertyAccessor() {
        return RfCavityPropertyAccessor.PROPERTY_AMPLITUDE;
    }

    /**
	 * Convert the specified CA value to a physical value.
	 * @param nodeAgent the node agent to use for the conversion.
	 * @param caValue the CA value to convert
	 */
    public double toPhysical(final NodeAgent nodeAgent, final double caValue) {
        return ((RfCavity) nodeAgent.getNode()).toCavAmpAvgFromCA(caValue);
    }

    /**
	 * Convert the specified physical value to a CA value.
	 * @param nodeAgent the node agent to use for the conversion.
	 * @param value the physical value to convert to CA
	 */
    public double toCA(final NodeAgent nodeAgent, final double value) {
        return ((RfCavity) nodeAgent.getNode()).toCAFromCavAmpAvg(value);
    }

    /**
	 * The value as a string for purposes of exporting.
	 * @param value the parameter's value
	 * @return the string representation of the value appropriate for export
	 */
    public String toExportValueString(final NodeAgent nodeAgent, final double value) {
        return "  \t" + value;
    }
}

/** PhaseAdaptor */
class PhaseAdaptor implements ParameterTypeAdaptor {

    /**
	 * Get an identifier of this parameter type.
	 * @return a unique string identifying this parameter type.
	 */
    public String getID() {
        return "RF Phase Adaptor";
    }

    /**
	 * Get the parameter name.
	 * @return the parameter name.
	 */
    public String getName() {
        return "Phase";
    }

    /**
	 * Get the design value for the parameter.
	 * @param nodeAgent the node agent from which to get the design value.
	 * @return the design value
	 */
    public double getDesignValue(final NodeAgent nodeAgent) {
        return ((RfCavity) nodeAgent.getNode()).getDfltCavPhase();
    }

    /**
	 * Get the lower and upper design limits for the parameter.
	 * @param nodeAgent the node agent from which to get the design limits.
	 * @return the design limits array [lower, upper] in that order
	 */
    public double[] getDesignLimits(final NodeAgent nodeAgent, final double designValue) {
        final RfCavity cavity = (RfCavity) nodeAgent.getNode();
        final double highestPhase = cavity.getDfltCavPhase() - cavity.getDfltAvgCavPhase() - 10;
        final double upperLimit = Math.min(180.0, Math.min(highestPhase, designValue + 20));
        final double lowerLimit = Math.max(-180.0, Math.min(upperLimit, designValue - 20));
        return new double[] { lowerLimit, upperLimit };
    }

    /**
	 * Get the readback channel for the specified node.
	 * @param nodeAgent the node agent for which to get the readback channel.
	 * @return the readback channel.
	 */
    public Channel getReadbackChannel(final NodeAgent nodeAgent) {
        return nodeAgent.getNode().getChannel(RfCavity.CAV_PHASE_AVG_HANDLE);
    }

    /**
	 * Get the control channel for the specified node.
	 * @param nodeAgent the node agent for which to get the control channel.
	 */
    public Channel getControlChannel(final NodeAgent nodeAgent) {
        return nodeAgent.getNode().getChannel(RfCavity.CAV_PHASE_SET_HANDLE);
    }

    /**
	 * Upload a value to the control system.
	 * @param parameter the parameter whose initial value should be uploaded.
	 * @param value the value to upload
	 * @return true if the request is successful and false if not
	 */
    public boolean uploadValue(final LiveParameter parameter, final double value) {
        throw new UnsupportedOperationException("Uploading values to the control system is unsupported for RF Phase.");
    }

    /**
	 * Get the online model property accessor for the parameter.
	 * @return the parameter's online model property accessor
	 */
    public String getPropertyAccessor() {
        return RfCavityPropertyAccessor.PROPERTY_PHASE;
    }

    /**
	 * Convert the specified CA value to a physical value.
	 * @param nodeAgent the node agent to use for the conversion.
	 * @param caValue the CA value to convert
	 */
    public double toPhysical(final NodeAgent nodeAgent, final double caValue) {
        return ((RfCavity) nodeAgent.getNode()).toCavPhaseAvgFromCA(caValue);
    }

    /**
	 * Convert the specified physical value to a CA value.
	 * @param nodeAgent the node agent to use for the conversion.
	 * @param value the physical value to convert to CA
	 */
    public double toCA(final NodeAgent nodeAgent, final double value) {
        return ((RfCavity) nodeAgent.getNode()).toCAFromCavPhaseAvg(value);
    }

    /**
	 * The value as a string for purposes of exporting.
	 * @param value the parameter's value
	 * @return the string representation of the value appropriate for export
	 */
    public String toExportValueString(final NodeAgent nodeAgent, final double value) {
        final RfCavity cavity = (RfCavity) nodeAgent.getNode();
        return "  \t" + value + "  \t" + cavity.toAvgCavPhaseFromCavPhase(value);
    }
}
