package com.continuent.tungsten.router.jdbc;

import java.io.Serializable;
import java.util.UUID;
import com.continuent.tungsten.commons.cluster.resource.DataSource;
import com.continuent.tungsten.commons.config.TungstenProperties;
import com.continuent.tungsten.commons.patterns.order.HighWaterResource;

public class TSRSession implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private TSRSessionManager manager = null;

    private DataSource source = null;

    private long referenceCount = 0;

    private long timeCreated = System.currentTimeMillis();

    private long lastTimeUsed = System.currentTimeMillis();

    private long switchToMaster = 0;

    private long switchToSlave = 0;

    /**
     * The number of times we need to go to the master instead of a slave
     */
    private long slaveMissCount = 0;

    /**
     * The number of times we kept the slave.
     */
    private long slaveHitCount = 0;

    /**
     * The number of times this session did a transaction which may or may not
     * have had writes.
     */
    private long writeXactCount = 0;

    /**
     * The number of times this session has been accessed
     */
    private long sessionAccessCount = 0;

    /**
     * Session ID for this session. This can be any arbitrary string, as long as
     * it does not contain whitespace. Applications that use this facility must
     * guarantee that this sessionId is sufficiently unique that there will
     * never be two semantically separate sessions that reference the same
     * sessionId.
     */
    private String sessionId = null;

    /**
     * This property represents the eventId associated with the last commit
     * performed by this session. Sinde it's impractical and unnecessary to get
     * the exact 'eventId' associated with a specific commit, it's sufficient to
     * simply get an eventId that is assured to be at or after the commit. This
     * is why we call it a 'highWater' and not, explicitly, lastCommitEventId or
     * something like this.
     */
    private HighWaterResource highWater = new HighWaterResource(-1, "");

    private boolean autoSession = false;

    /**
     * Creates a new <code>TSRSession</code> object The sessionId is set from
     * the one passed in.
     * 
     * @param sessionId
     */
    public TSRSession(String sessionId, TSRSessionManager manager) {
        this.sessionId = sessionId;
        this.manager = manager;
    }

    /**
     * Creates a new <code>TSRSession</code> object The sessionId is generated
     * internally and initialized.
     */
    public TSRSession() {
        this.sessionId = newSessionID();
    }

    /**
     * Creates a unique identifier that is suitable for a sessionId
     * 
     * @return new unique sessionId
     */
    public static String newSessionID() {
        return UUID.randomUUID().toString();
    }

    /**
     * Returns the sessionId value.
     * 
     * @return Returns the sessionId.
     */
    public String getSessionID() {
        return sessionId;
    }

    /**
     * Sets the sessionId value.
     * 
     * @param sessionId The sessionId to set.
     */
    public void setSessionID(String sessionId) {
        this.sessionId = sessionId;
    }

    public HighWaterResource getHighWater() {
        return highWater;
    }

    public void setHighWater(DataSource source, HighWaterResource highWater) {
        this.highWater = highWater;
    }

    public void release() {
        manager.release(this);
    }

    public void close() {
        manager.close(this);
    }

    /**
     * Increments the reference count
     */
    public void incrementReferenceCount() {
        referenceCount++;
        sessionAccessCount++;
    }

    /**
     * Decrements the reference count
     */
    public void decrementReferenceCount() {
        referenceCount--;
    }

    public long getReferenceCount() {
        return referenceCount;
    }

    public void setReferenceCount(long referenceCount) {
        this.referenceCount = referenceCount;
    }

    public long getLastTimeUsed() {
        return lastTimeUsed;
    }

    public void setLastTimeUsed(long lastTimeUsed) {
        this.lastTimeUsed = lastTimeUsed;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public long getTimeCreated() {
        return timeCreated;
    }

    public void setTimeCreated(long timeCreated) {
        this.timeCreated = timeCreated;
    }

    public long getSlaveMissCount() {
        return slaveMissCount;
    }

    public void setSlaveMissCount(long slaveMissCount) {
        this.slaveMissCount = slaveMissCount;
    }

    public void incrementSlaveMissCount() {
        this.slaveMissCount += 1;
    }

    public long getSlaveHitCount() {
        return slaveHitCount;
    }

    public void setSlaveHitCount(long slaveHitCount) {
        this.slaveHitCount = slaveHitCount;
    }

    public void incrementSlaveHitCount() {
        this.slaveHitCount += 1;
    }

    public void decrementSlaveHitCount() {
        this.slaveHitCount -= 1;
    }

    public long getWriteXactCount() {
        return writeXactCount;
    }

    public void incrementWriteXactCount() {
        this.writeXactCount += 1;
    }

    public void setWriteXactCount(long writeXactCount) {
        this.writeXactCount = writeXactCount;
    }

    public long getTotalConnectCount() {
        return slaveHitCount + slaveMissCount + writeXactCount;
    }

    public String toString() {
        return String.format("sessionId=%s, referenceCount=%d, slaveHitCount=%d, slaveMissCount=%d, writeXactCount=%d", sessionId, referenceCount, slaveHitCount, slaveMissCount, writeXactCount);
    }

    public long getSessionAccessCount() {
        return sessionAccessCount;
    }

    public void setSessionAccessCount(long sessionAccessCount) {
        this.sessionAccessCount = sessionAccessCount;
    }

    public long getSwitchToMaster() {
        return switchToMaster;
    }

    public void setSwitchToMaster(long switchToMaster) {
        this.switchToMaster = switchToMaster;
    }

    public void incrementSwitchToMaster() {
        this.switchToMaster++;
    }

    public long getSwitchToSlave() {
        return switchToSlave;
    }

    public void incrementSwitchToSlave() {
        this.switchToSlave++;
    }

    public void setSwitchToSlave(long switchToSlave) {
        this.switchToSlave = switchToSlave;
    }

    public void touch() {
        this.lastTimeUsed = System.currentTimeMillis();
    }

    public DataSource getSource() {
        return source;
    }

    public void setSource(DataSource source) {
        this.source = source;
    }

    public TungstenProperties getStatistics() {
        TungstenProperties sessionProps = new TungstenProperties();
        long readXactCount = slaveHitCount + slaveMissCount;
        double slaveHitPercentage = 0;
        if (readXactCount > 0) {
            slaveHitPercentage = (((double) slaveHitCount / (double) readXactCount)) * 100;
        }
        double readWriteRatio = 0;
        if (writeXactCount > 0) {
            readWriteRatio = ((double) readXactCount / ((double) (writeXactCount + readXactCount))) * 100;
        }
        sessionProps.setString("sessionId", sessionId);
        sessionProps.setString("highWater", highWater.toString());
        sessionProps.setLong("timeCreated", timeCreated);
        sessionProps.setLong("lastTimeUsed", lastTimeUsed);
        sessionProps.setLong("sessionAccessCount", sessionAccessCount);
        sessionProps.setLong("writeXactCount", writeXactCount);
        sessionProps.setLong("readXactCount", readXactCount);
        sessionProps.setLong("slaveHitCount", slaveHitCount);
        sessionProps.setLong("slaveMissCount", slaveMissCount);
        sessionProps.setDouble("slaveHitPercentage", slaveHitPercentage);
        sessionProps.setDouble("readWriteRatio", readWriteRatio);
        sessionProps.setLong("switchToMaster", switchToMaster);
        sessionProps.setLong("switchToSlave", switchToSlave);
        return sessionProps;
    }

    public boolean isAutoSession() {
        return autoSession;
    }

    public void setAutoSession(boolean autoSession) {
        this.autoSession = autoSession;
    }
}
