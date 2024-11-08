package gov.sns.apps.energymanager;

import gov.sns.xal.smf.*;
import gov.sns.xal.smf.impl.*;
import gov.sns.ca.Channel;
import gov.sns.tools.data.*;
import java.util.*;

/** abstract electromagnet agent */
public abstract class ElectromagnetAgent extends NodeAgent {

    protected final int FIELD_INDEX = 0;

    /** Constructor */
    public ElectromagnetAgent(final AcceleratorSeq sequence, final Electromagnet node, final ParameterStore parameterStore) {
        super(sequence, node, parameterStore);
        requestBookConnection();
    }

    /**
	 * Get the electromagnet field adaptor.
	 * @return the electromagnet field adaptor
	 */
    protected abstract ElectromagnetFieldAdaptor getFieldAdaptor();

    /** Request a connection to the book channel. */
    protected void requestBookConnection() {
        try {
            final Channel bookChannel = getNode().getChannel(MagnetMainSupply.FIELD_BOOK_HANDLE);
            bookChannel.requestConnection();
        } catch (NoSuchChannelException exception) {
        }
    }

    /** populate live parameters */
    @Override
    protected void populateLiveParameters(final ParameterStore parameterStore) {
        _liveParameters = new ArrayList<LiveParameter>(1);
        _liveParameters.add(parameterStore.addLiveParameter(this, getFieldAdaptor()));
    }

    /** 
	* Scale the field by beta-gamma to preserve the magnet's influence on the beam.
	* @param kineticEnergy
	* @param designKineticEnergy
	* @param restEnergy
	*/
    public void preserveDesignInfluence(final double kineticEnergy, final double designKineticEnergy, final double restEnergy) {
        final double energy = kineticEnergy + restEnergy;
        final double designEnergy = designKineticEnergy + restEnergy;
        final double betaGamma = Math.sqrt(Math.pow(energy / restEnergy, 2) - 1.0);
        final double designBetaGamma = Math.sqrt(Math.pow(designEnergy / restEnergy, 2) - 1.0);
        final LiveParameter fieldParameter = getLiveParameter(FIELD_INDEX);
        final double designField = fieldParameter.getDesignValue();
        fieldParameter.setCustomValue(designField * betaGamma / designBetaGamma);
    }

    /**
	 * Export optics changes using the exporter.
	 * @param exporter the optics exporter to use for exporting this node's optics changes
	 */
    @Override
    public void exportOpticsChanges(final OpticsExporter exporter) {
        final LiveParameter parameter = getLiveParameter(FIELD_INDEX);
        if (parameter.getDesignValue() != parameter.getInitialValue()) {
            final DataAdaptor adaptor = exporter.getChildAdaptor(getNode().getParent(), getNode().dataLabel());
            adaptor.setValue("id", getNode().getId());
            final DataAdaptor attributesAdaptor = adaptor.createChild("attributes");
            final DataAdaptor magnetAdaptor = attributesAdaptor.createChild("magnet");
            magnetAdaptor.setValue("dfltMagFld", parameter.getInitialValue());
        }
    }
}
