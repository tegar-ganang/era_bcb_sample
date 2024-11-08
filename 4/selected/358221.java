package com.rbnb.api;

final class TimeRelativeResponse {

    /**
     * invert the resulting request?
     * <p>
     * For cases where we grab the end time of something, we want to invert the
     * request handling so that it is exclusive start, inclusive end, rather
     * than the standard inclusive start, exclusive end.
     * <p>
     *
     * @author Ian Brown
     *
     * @since V2.2
     * @version 11/18/2003
     */
    private boolean invert = false;

    /**
     * the status.
     * <p>
     * Status values are:
     * <p><ol start=-2>
     *    <li>channels in request have different time bases,</li>
     *    <li>request is before the data object checked,</li>
     *    <li>request was matched in the data object, or</li>
     *    <li>request is after the data object checked.</li>
     * </ol>
     * <p>
     *
     * @author Ian Brown
     *
     * @since V2.2
     * @version 11/04/2003
     */
    private int status = 0;

    /**
     * the time found.
     * <p>
     *
     * @author Ian Brown
     *
     * @since V2.2
     * @version 11/04/2003
     */
    private double time = 0.;

    public TimeRelativeResponse() {
        super();
    }

    final Rmap buildRequest(TimeRelativeRequest requestI, RequestOptions optionsI) throws com.rbnb.utility.SortException, com.rbnb.api.AddressException, com.rbnb.api.SerializeException, java.io.EOFException, java.io.IOException, java.lang.InterruptedException {
        Rmap requestR = new DataRequest();
        TimeRelativeChannel trc;
        Rmap trChild;
        Rmap child = null;
        for (int idx = 0; idx < requestI.getByChannel().size(); ++idx) {
            trc = (TimeRelativeChannel) requestI.getByChannel().elementAt(idx);
            trChild = Rmap.createFromName("/" + trc.getChannelName());
            trChild.moveToBottom().setDblock(Rmap.MarkerBlock);
            if (child == null) {
                child = trChild;
            } else {
                child = child.mergeWith(trChild);
            }
        }
        double ltime = getTime();
        double lduration = requestI.getTimeRange().getDuration();
        boolean ldirection = getInvert();
        if ((optionsI != null) && optionsI.getExtendStart()) {
            if (ltime != 0.) {
                long lltime = Double.doubleToLongBits(ltime);
                lltime--;
                ltime = Double.longBitsToDouble(lltime);
            }
            lduration += requestI.getTimeRange().getTime() - ltime;
            ldirection = false;
        } else {
            switch(requestI.getRelationship()) {
                case TimeRelativeRequest.BEFORE:
                case TimeRelativeRequest.AT_OR_BEFORE:
                    ltime -= lduration;
                    break;
                case TimeRelativeRequest.AFTER:
                case TimeRelativeRequest.AT_OR_AFTER:
                    break;
            }
        }
        child.setTrange(new TimeRange(ltime, lduration));
        child.getTrange().setDirection(ldirection);
        requestR.addChild(child);
        return (requestR);
    }

    public final boolean getInvert() {
        return (invert);
    }

    public final int getStatus() {
        return (status);
    }

    public final double getTime() {
        return (time);
    }

    public final void setInvert(boolean invertI) {
        invert = invertI;
    }

    public final void setStatus(int statusI) {
        status = statusI;
    }

    public final void setTime(double timeI) {
        time = timeI;
    }

    public final String toString() {
        return ("TimeRelativeResponse: " + getStatus() + " " + getTime());
    }
}
