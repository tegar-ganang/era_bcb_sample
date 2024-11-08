package gov.sns.apps.tripviewer;

import gov.sns.services.tripmonitor.*;
import java.util.*;

/** Handle service requests for a single service */
public class ServiceHandler {

    /** service proxy to handle */
    protected final TripMonitorPortal SERVICE;

    /** service ID */
    protected final String ID;

    /** host */
    protected final String HOST_NAME;

    /** launch time */
    protected final Date LAUNCH_TIME;

    /** list of trip monitors */
    protected final List<String> TRIP_MONITOR_NAMES;

    /** channel refs keyed by trip monitor ID */
    protected final Map<String, List<ChannelRef>> CHANNEL_REF_MAP;

    /** trip records keyed by trip monitor ID */
    protected final Map<String, List<TripRecord>> TRIP_RECORD_MAP;

    /** last refresh time */
    protected Date _lastRefresh;

    /** Constructor */
    public ServiceHandler(final TripMonitorPortal service, final String identifier) {
        SERVICE = service;
        ID = identifier;
        HOST_NAME = service.getHostName();
        LAUNCH_TIME = service.getLaunchTime();
        final Vector monitorNames = service.getTripMonitorNames();
        TRIP_MONITOR_NAMES = new ArrayList<String>(monitorNames.size());
        for (final Object monitorName : monitorNames) {
            TRIP_MONITOR_NAMES.add(monitorName.toString());
        }
        CHANNEL_REF_MAP = new HashMap<String, List<ChannelRef>>();
        TRIP_RECORD_MAP = new HashMap<String, List<TripRecord>>();
        _lastRefresh = new Date(0);
    }

    /** get the ID */
    public String getID() {
        return ID;
    }

    /** get the host */
    public String getHostName() {
        return HOST_NAME;
    }

    /** get the launch time */
    public Date getLaunchTime() {
        return LAUNCH_TIME;
    }

    /** get the trip monitor names */
    public List getTripMonitorNames() {
        return TRIP_MONITOR_NAMES;
    }

    /** get the trip monitor names */
    public Vector getTripMonitorNameVector() {
        return new Vector(TRIP_MONITOR_NAMES);
    }

    /** get the channel refs for the specified monitor */
    public List<ChannelRef> getChannelRefs(final String monitorID) {
        synchronized (CHANNEL_REF_MAP) {
            return CHANNEL_REF_MAP.get(monitorID);
        }
    }

    /** update the channel refs */
    protected void updateChannelRefs() {
        final Map<String, List<ChannelRef>> channelRefMap = new HashMap<String, List<ChannelRef>>();
        for (final String monitorName : TRIP_MONITOR_NAMES) {
            final List<ChannelRef> channelRefs = fetchChannelRefs(monitorName);
            channelRefMap.put(monitorName, channelRefs);
        }
        synchronized (CHANNEL_REF_MAP) {
            CHANNEL_REF_MAP.clear();
            CHANNEL_REF_MAP.putAll(channelRefMap);
        }
    }

    /** fetch the channel references */
    protected List<ChannelRef> fetchChannelRefs(final String monitorName) {
        final Vector channelInfo = SERVICE.getChannelInfo(monitorName);
        final List<ChannelRef> channelRefs = new ArrayList<ChannelRef>(channelInfo.size());
        for (final Object record : channelInfo) {
            final Map channelRecord = (Map) record;
            channelRefs.add(ChannelRef.getInstanceFromInfoRecord(channelRecord));
        }
        return channelRefs;
    }

    /** get the trip records for the specified monitor */
    public List<TripRecord> getTripRecords(final String monitorID) {
        synchronized (TRIP_RECORD_MAP) {
            return TRIP_RECORD_MAP.get(monitorID);
        }
    }

    /** update the trip records */
    protected void updateTripRecords() {
        final Map<String, List<TripRecord>> tripRecordMap = new HashMap<String, List<TripRecord>>();
        for (final String monitorName : TRIP_MONITOR_NAMES) {
            final List<TripRecord> tripRecords = fetchTripRecords(monitorName);
            tripRecordMap.put(monitorName, tripRecords);
        }
        synchronized (TRIP_RECORD_MAP) {
            TRIP_RECORD_MAP.clear();
            TRIP_RECORD_MAP.putAll(tripRecordMap);
        }
    }

    /** fetch the trip records */
    protected List<TripRecord> fetchTripRecords(final String monitorName) {
        final Vector tripRecordVector = SERVICE.getTripRecords(monitorName);
        final List<TripRecord> tripRecords = new ArrayList<TripRecord>(tripRecordVector.size());
        for (final Object record : tripRecordVector) {
            final Map tripRecordInfo = (Map) record;
            tripRecords.add(TripRecord.getInstanceFromRecordMap(tripRecordInfo));
        }
        return tripRecords;
    }

    /** get the last refresh time */
    public Date getLastRefresh() {
        return _lastRefresh;
    }

    /**
	 * Determine if this handler has refreshed within the specified period.
	 * @param period refresh period in seconds
	 * @return true if this handler has refreshed within the specified period and false if not
	 */
    public boolean hasRefreshedInPeriod(final double period) {
        return new Date().getTime() - _lastRefresh.getTime() < 1000 * period;
    }

    /** publish buffer records to the database */
    public void publishBuffer() {
        SERVICE.publishTrips();
        refresh();
    }

    /** refresh cached data from the service */
    public void refresh() {
        updateChannelRefs();
        updateTripRecords();
        _lastRefresh = new Date();
    }
}
