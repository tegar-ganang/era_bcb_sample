package edu.sdsc.cleos;

public final class NeonSingleStringData {

    protected static final String[] CHANNEL_LIST = new String[] { "PTB210", "LI190QS", "HMP45T", "HMP45H", "BF3TR", "BF3DR", "BF3SS", "WXT510T", "WXT510H", "WXT510P", "WXT510RA", "WXT510RD", "WXT510RI", "WXT510HA", "WXT510HD", "WXT510HI", "WXT510RPI", "WXT510HPI", "WXT510WDMIN", "WXT510WDAVG", "WXT510WDMAX", "WXT510WSMIN", "WXT510WSAVG", "WXT510WSMAX", "NWPLOG", "GPSLOG" };

    protected static final String[] UNIT_LIST = new String[] { "hPa", "umol/(s m^2)", "degrees Celsius", "%RH", "umol/(s m^2)", "umol/(s m^2)", "Sunshine presence (1|0)", "degrees Celsius", "%RH", "hPa", "mm", "s", "mm/h", "hits/cm^2", "s", "hits/cm^2h", "mm/h", "hits/cm^2h", "degrees", "degrees", "degrees", "m/s", "m/s", "m/s", "string", "string" };

    protected static final String[] UB_LIST = null;

    protected static final String[] LB_LIST = null;

    public static String[] getChannels() {
        return CHANNEL_LIST;
    }

    public static String[] getUnits() {
        return UNIT_LIST;
    }
}
