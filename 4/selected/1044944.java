package gov.sns.apps.mtv;

import gov.sns.ca.*;
import java.text.*;

/**
 * This is an intermediate object to facilitate the table display 
 * from the PVData objects.
 * A table cell that listens to CA. This class bundles together some
 * functionality that will be used by the java TableModel and
 * will hide some details for getting info about live channels
 *
 */
public class PVTableCell {

    /** The channel this cell is monitoring */
    protected ChannelWrapper theChannel;

    /** indicator that this is a placeholder 
     * (needed  for general xml parsing of the DataTable Structure */
    protected boolean amADummy;

    /** format for displaying in scientific notation */
    private DecimalFormat scientificFormat;

    /** format for displaying in float notation */
    private DecimalFormat floatFormat;

    /** constructor used by the dummy table cells */
    public PVTableCell() {
        amADummy = true;
    }

    /** constructor used by table cells with a real PV in it */
    public PVTableCell(ChannelWrapper channel) {
        amADummy = false;
        theChannel = channel;
        scientificFormat = new DecimalFormat("0.00000E0");
        floatFormat = new DecimalFormat("0.000000");
    }

    /** constructor used by table cells with a real PV in it */
    public PVTableCell(String channelName) {
        theChannel = new ChannelWrapper(channelName);
        amADummy = false;
        scientificFormat = new DecimalFormat("0.00000E0");
        floatFormat = new DecimalFormat("0.000000");
    }

    /** constructor used by table cells with a real PV in it */
    public PVTableCell(Channel channel) {
        theChannel = new ChannelWrapper(channel);
        amADummy = false;
        scientificFormat = new DecimalFormat("0.00000E0");
        floatFormat = new DecimalFormat("0.000000");
    }

    /** return the name of the channel */
    public String PVName() {
        return theChannel.getId();
    }

    /** return true if there is a connected channel in this cell */
    public boolean isConnected() {
        if (amADummy) return false;
        return theChannel.isConnected();
    }

    /** Output the PV value to a String 
     * needed by the tablemodel */
    @Override
    public String toString() {
        if (amADummy) return "  ";
        NumberFormat fieldFormat = null;
        double val = theChannel.getValDbl();
        if (Math.abs(val) > 1000000. || Math.abs(val) < 0.000001) fieldFormat = scientificFormat; else fieldFormat = floatFormat;
        return fieldFormat.format(val);
    }

    public double getValDbl() {
        return theChannel.getValDbl();
    }

    protected Channel getChannel() {
        return theChannel.getChannel();
    }
}
