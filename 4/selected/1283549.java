package gov.sns.apps.mpsinputtest;

/** Mode Mask Record */
public class MMRecord implements Comparable {

    protected String _jeriValue;

    protected String _jeriIOC;

    protected String _jeriSubSystem;

    protected String _jeriDevice;

    protected String _jeriChannelNo;

    protected String _isTested;

    protected String _TestRdy;

    protected String _PFstatus;

    protected int _isRdy;

    /** Constructor */
    public MMRecord(final String jeriValue, final String jeriSubSystem, final String jeriDevice, final String jeriIOC, final String jeriChannelNo, final String isTested, final String testRdy, final String pfStat, final int isRdy) {
        _jeriIOC = jeriIOC;
        _jeriValue = jeriValue;
        _jeriSubSystem = jeriSubSystem;
        _jeriDevice = jeriDevice;
        _jeriChannelNo = jeriChannelNo;
        _isTested = isTested;
        _TestRdy = testRdy;
        _PFstatus = pfStat;
        _isRdy = isRdy;
    }

    public String getTestedValue() {
        return _isTested;
    }

    public String getPFstatus() {
        return _PFstatus;
    }

    public String getTestRdyValue() {
        return _TestRdy;
    }

    public int getRdyStatus() {
        return _isRdy;
    }

    /** get the jeri value */
    public String getJeriValue() {
        return _jeriValue;
    }

    /** get the jeri value */
    public String getIOC() {
        return _jeriIOC;
    }

    /** get the jeri subsystem */
    public String getSubSystem() {
        return _jeriSubSystem;
    }

    /** get the jeri device */
    public String getDevice() {
        return _jeriDevice;
    }

    /** get the jeri channel number */
    public String getChannelNo() {
        return _jeriChannelNo;
    }

    /** get the PV */
    public String getPV() {
        return _jeriValue;
    }

    /** get the display color */
    public String getDisplayColor() {
        return "<font COLOR=#ff0000>";
    }

    /** compare records to facilitate sorting */
    public int compareTo(Object other) {
        final MMRecord otherRecord = (MMRecord) other;
        if (getRdyStatus() < otherRecord.getRdyStatus()) {
            return getRdyStatus();
        } else if (getRdyStatus() > otherRecord.getRdyStatus()) {
            return otherRecord.getRdyStatus();
        } else return getPV().compareTo(otherRecord.getPV());
    }
}
