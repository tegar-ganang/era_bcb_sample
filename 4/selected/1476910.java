package co.edu.unal.ungrid.image.dicom.display.wave;

/**
 * <p>
 * An abstract class that encapsulates the features and values from an ECG
 * source, usually for the purpose of displaying it.
 * </p>
 * 
 * 
 */
public abstract class SourceECG {

    /**
	 * @uml.property name="samples"
	 */
    protected short[][] samples;

    /**
	 * @uml.property name="numberOfChannels"
	 */
    protected int numberOfChannels;

    /**
	 * @uml.property name="nSamplesPerChannel"
	 */
    protected int nSamplesPerChannel;

    /**
	 * @uml.property name="samplingIntervalInMilliSeconds"
	 */
    protected float samplingIntervalInMilliSeconds;

    /**
	 * @uml.property name="amplitudeScalingFactorInMilliVolts"
	 */
    protected float[] amplitudeScalingFactorInMilliVolts;

    /**
	 * @uml.property name="channelNames"
	 */
    protected String[] channelNames;

    /**
	 * @uml.property name="displaySequence"
	 */
    protected int displaySequence[];

    /**
	 * <p>
	 * Use the default encoded order.
	 * </p>
	 */
    protected void buildPreferredDisplaySequence() {
        displaySequence = new int[numberOfChannels];
        for (int i = 0; i < numberOfChannels; ++i) {
            displaySequence[i] = i;
        }
    }

    /**
	 * <p>
	 * Find the named lead in an array of lead names.
	 * </p>
	 * 
	 * @param leadNames
	 *            an array of String names to designate leads (may be null, or
	 *            contain null strings, in which case won't be found)
	 * @param leadName
	 *            the string name of the lead wanted (may be null, in which case
	 *            won't be found)
	 * @return the index in leadNames of the requested lead if present, else -1
	 */
    protected static int findLead(String[] leadNames, String leadName) {
        if (leadNames != null && leadName != null) {
            String upperCaseLeadName = leadName.toUpperCase();
            for (int i = 0; i < leadNames.length; ++i) {
                if (leadNames[i] != null && leadNames[i].toUpperCase().equals(upperCaseLeadName)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static final String[] preferred12LeadOrder = { "I", "II", "III", "aVR", "aVL", "aVF", "V1", "V2", "V3", "V4", "V5", "V6" };

    /**
	 * <p>
	 * Using the lead descriptions, look for patterns and determine the desired
	 * sequential display order, defaulting to the encoded order if no
	 * recognized pattern.
	 * </p>
	 * 
	 * @param labels
	 *            the labels to use to match the preferred order (may or may not
	 *            be <code>this.channelNames</code>)
	 */
    protected void buildPreferredDisplaySequence(String[] labels) {
        displaySequence = null;
        if (numberOfChannels == preferred12LeadOrder.length) {
            displaySequence = new int[numberOfChannels];
            for (int i = 0; i < numberOfChannels; ++i) {
                int leadIndex = findLead(labels, preferred12LeadOrder[i]);
                if (leadIndex == -1) {
                    displaySequence = null;
                    break;
                } else {
                    displaySequence[i] = leadIndex;
                }
            }
        }
        if (displaySequence == null) {
            buildPreferredDisplaySequence();
        }
        for (int i = 0; i < numberOfChannels; ++i) {
        }
    }

    /**
	 * @uml.property name="title"
	 */
    protected String title;

    /**
	 */
    protected static String buildInstanceTitle() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("");
        return buffer.toString();
    }

    /**
	 * @uml.property name="samples"
	 */
    public short[][] getSamples() {
        return samples;
    }

    /**
	 * @uml.property name="numberOfChannels"
	 */
    public int getNumberOfChannels() {
        return numberOfChannels;
    }

    /***/
    public int getNumberOfSamplesPerChannel() {
        return nSamplesPerChannel;
    }

    /**
	 * @uml.property name="samplingIntervalInMilliSeconds"
	 */
    public float getSamplingIntervalInMilliSeconds() {
        return samplingIntervalInMilliSeconds;
    }

    /**
	 * @uml.property name="amplitudeScalingFactorInMilliVolts"
	 */
    public float[] getAmplitudeScalingFactorInMilliVolts() {
        return amplitudeScalingFactorInMilliVolts;
    }

    /**
	 * @uml.property name="channelNames"
	 */
    public String[] getChannelNames() {
        return channelNames;
    }

    /**
	 * @uml.property name="title"
	 */
    public String getTitle() {
        return title;
    }

    /**
	 * @uml.property name="displaySequence"
	 */
    public int[] getDisplaySequence() {
        return displaySequence;
    }
}
