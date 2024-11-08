package com.rbnb.api;

import com.rbnb.utility.SortCompareInterface;
import com.rbnb.utility.SortException;

final class TimeRelativeChannel implements com.rbnb.compat.Cloneable, SortCompareInterface {

    /**
     * sort by channel name.
     * <p>
     *
     * @author Ian Brown
     *
     * @since V2.2
     * @version 10/10/2003
     */
    public static final String SORT_CHANNEL_NAME = "name";

    /**
     * the name of the channel.
     * <p>
     *
     * @author Ian Brown
     *
     * @since V2.2
     * @version 10/10/2003
     */
    private String channelName = null;

    public TimeRelativeChannel() {
        super();
    }

    public final Object clone() {
        TimeRelativeChannel clonedR = new TimeRelativeChannel();
        clonedR.channelName = channelName;
        return clonedR;
    }

    public final int compareTo(Object sidI, Object otherI) throws SortException {
        String mine = (String) sortField(sidI);
        String other = ((otherI instanceof String) ? (String) otherI : (String) ((TimeRelativeChannel) otherI).sortField(sidI));
        return (mine.compareTo(other));
    }

    public final double determineNextReference(Rmap matchI, TimeRelativeRequest requestI) throws com.rbnb.api.AddressException, com.rbnb.api.SerializeException, java.io.EOFException, java.io.IOException, java.lang.InterruptedException {
        double referenceR = Double.NaN;
        DataArray data = matchI.extract(getChannelName());
        if (data == null || data.getNumberOfPoints() == 0 || data.timeRanges == null) {
            referenceR = Double.NaN;
        } else {
            double[] times;
            TimeRange tr;
            switch(requestI.getRelationship()) {
                case TimeRelativeRequest.BEFORE:
                case TimeRelativeRequest.AT_OR_BEFORE:
                    referenceR = ((TimeRange) data.timeRanges.firstElement()).getTime();
                    break;
                case TimeRelativeRequest.AT_OR_AFTER:
                case TimeRelativeRequest.AFTER:
                    tr = (TimeRange) data.timeRanges.lastElement();
                    if (tr.getInclusive()) {
                        referenceR = tr.getPtimes()[tr.getNptimes() - 1] + tr.getDuration();
                    } else {
                        times = data.getTime();
                        referenceR = times[times.length - 1];
                    }
                    break;
            }
        }
        return (referenceR);
    }

    public final String getChannelName() {
        return (channelName);
    }

    public final String nextNameLevel(int nameOffsetI) {
        String nextLevelR = null;
        if (nameOffsetI < getChannelName().length()) {
            int endIndex = getChannelName().indexOf("/", nameOffsetI + 1);
            if (endIndex == -1) {
                endIndex = getChannelName().length();
            }
            nextLevelR = getChannelName().substring(nameOffsetI + 1, endIndex);
        }
        return (nextLevelR);
    }

    public final void setChannelName(String channelNameI) {
        channelName = channelNameI;
    }

    public final Object sortField(Object sidI) throws SortException {
        if (sidI != SORT_CHANNEL_NAME) {
            throw new com.rbnb.utility.SortException("The sort identifier is not valid.");
        }
        return (getChannelName());
    }

    public final String toString() {
        return ("TimeRelativeChannel: " + getChannelName());
    }
}
