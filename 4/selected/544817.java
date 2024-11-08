package com.continuent.tungsten.router.jdbc;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import org.apache.log4j.Logger;
import com.continuent.tungsten.commons.config.TungstenProperties;
import com.continuent.tungsten.router.resource.DataService;

public class TSRSessionManager extends HashMap<String, TSRSession> implements Runnable {

    private static Logger logger = Logger.getLogger(TSRSessionManager.class);

    Thread salvageThread = null;

    /**
     * Indicates how long, in seconds, a session will be kept around before it
     * is removed
     */
    static final int SESSION_EXPIRATION_INTERVAL_MILLIS = 300000;

    static final int MAX_SESSION_COUNT = 5000;

    private double accumulatedReadWriteRatio = 0.0;

    private double accumulatedSlaveHitPercentage = 0.0;

    private long accumulatedSessionAccessCount = 0;

    private long accumulatedSessionCount = 0;

    private long accumulatedWriteXactCount = 0;

    private long accumulatedReadXactCount = 0;

    private long accumulatedSlaveHitCount = 0;

    private long accumulatedSlaveMissCount = 0;

    private long accumulatedSwitchToMasterCount = 0;

    private long accumulatedSwitchToSlaveCount = 0;

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public TSRSessionManager() {
        salvageThread = new Thread(this, "Expired TSRSession Cleanup");
        salvageThread.start();
    }

    public synchronized void release(TSRSession session) {
        if (session == null) return;
        if (session.isAutoSession()) {
            close(session);
        } else {
            session.decrementReferenceCount();
        }
    }

    public synchronized void close(TSRSession session) {
        accumulatedSessionCount++;
        accumulatedSlaveHitCount += session.getSlaveHitCount();
        accumulatedSlaveMissCount += session.getSlaveMissCount();
        accumulatedSessionAccessCount += session.getSessionAccessCount();
        accumulatedWriteXactCount += session.getWriteXactCount();
        accumulatedSwitchToSlaveCount += session.getSwitchToSlave();
        accumulatedSwitchToMasterCount += session.getSwitchToMaster();
        accumulatedReadXactCount = (accumulatedSlaveHitCount + accumulatedSlaveMissCount);
        long readXactCount = session.getSlaveHitCount() + session.getSlaveMissCount();
        if (accumulatedReadXactCount + readXactCount > 0) {
            accumulatedSlaveHitPercentage = ((((double) accumulatedSlaveHitCount + session.getSlaveHitCount()) / (double) (accumulatedReadXactCount + readXactCount))) * 100;
        }
        if (accumulatedWriteXactCount + session.getWriteXactCount() > 0) {
            accumulatedReadWriteRatio = (((double) accumulatedReadXactCount + readXactCount) / ((double) (accumulatedWriteXactCount + session.getWriteXactCount() + accumulatedReadXactCount + readXactCount))) * 100;
        }
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Closing session %s", session));
        }
        remove(session.getSessionId());
    }

    public synchronized TSRSession getSession(String sessionId) {
        TSRSession session = super.get(sessionId);
        if (session == null) {
            session = new TSRSession(sessionId, this);
            put(sessionId, session);
        }
        session.incrementReferenceCount();
        session.setLastTimeUsed(System.currentTimeMillis());
        return session;
    }

    public Map<String, TSRSession> getSessions() {
        TreeMap<String, TSRSession> sessions = new TreeMap<String, TSRSession>();
        synchronized (this) {
            sessions.putAll(this);
        }
        return sessions;
    }

    public void run() {
        logger.info("Expired TSRSession cleanup thread has been started");
        while (true) {
            try {
                long nextExpiration = System.currentTimeMillis() + SESSION_EXPIRATION_INTERVAL_MILLIS;
                boolean isExpired = false;
                boolean isMaxSessionCount = false;
                do {
                    Thread.sleep(SESSION_EXPIRATION_INTERVAL_MILLIS / 10);
                    if (System.currentTimeMillis() >= nextExpiration) {
                        isExpired = true;
                    } else if (size() >= MAX_SESSION_COUNT) {
                        isMaxSessionCount = true;
                    }
                } while (!isExpired && !isMaxSessionCount);
                long checkTime = System.currentTimeMillis();
                synchronized (this) {
                    int releasedSessionCount = 0;
                    Vector<TSRSession> sessionsToRelease = new Vector<TSRSession>();
                    for (TSRSession session : values()) {
                        if (checkTime - session.getLastTimeUsed() >= SESSION_EXPIRATION_INTERVAL_MILLIS) {
                            sessionsToRelease.add(session);
                            releasedSessionCount++;
                        } else {
                            if (isMaxSessionCount) {
                                if (releasedSessionCount < MAX_SESSION_COUNT / 2) {
                                    sessionsToRelease.add(session);
                                    releasedSessionCount++;
                                } else {
                                    break;
                                }
                            }
                        }
                    }
                    for (TSRSession session : sessionsToRelease) {
                        close(session);
                    }
                    if (logger.isDebugEnabled()) {
                        if (sessionsToRelease.size() > 0) logger.debug(String.format("Removed %d expired sessions.", sessionsToRelease.size()));
                    }
                    sessionsToRelease.clear();
                }
            } catch (Exception e) {
                logger.error("Caught exception while processing expired sessions:" + e);
            }
        }
    }

    public synchronized TungstenProperties getSessionStatistics() {
        int currentSessionCount = 0;
        long slaveHitCount = 0;
        long slaveMissCount = 0;
        long writeXactCount = 0;
        long readXactCount = 0;
        long sessionAccessCount = 0;
        long switchToSlaveCount = 0;
        long switchToMasterCount = 0;
        for (TSRSession session : values()) {
            currentSessionCount++;
            sessionAccessCount += session.getSessionAccessCount();
            slaveHitCount += session.getSlaveHitCount();
            slaveMissCount += session.getSlaveMissCount();
            writeXactCount += session.getWriteXactCount();
            switchToMasterCount += session.getSwitchToMaster();
            switchToSlaveCount += session.getSwitchToSlave();
        }
        readXactCount = slaveHitCount + slaveMissCount;
        TungstenProperties sessionProps = new TungstenProperties();
        if (accumulatedReadXactCount + readXactCount > 0) {
            accumulatedSlaveHitPercentage = ((((double) accumulatedSlaveHitCount + slaveHitCount) / (double) (accumulatedReadXactCount + readXactCount))) * 100;
        }
        double slaveHitPercentage = 0;
        if (readXactCount > 0) {
            slaveHitPercentage = (((double) slaveHitCount / (double) readXactCount)) * 100;
        }
        double readWriteRatio = 0;
        if (writeXactCount > 0) {
            readWriteRatio = ((double) readXactCount / ((double) (writeXactCount + readXactCount))) * 100;
        }
        if (accumulatedWriteXactCount + writeXactCount > 0) {
            accumulatedReadWriteRatio = (((double) accumulatedReadXactCount + readXactCount) / ((double) (accumulatedWriteXactCount + writeXactCount + accumulatedReadXactCount + readXactCount))) * 100;
        }
        sessionProps.setLong("accumulatedSessionAccessCount", accumulatedSessionAccessCount + sessionAccessCount);
        sessionProps.setLong("accumulatedSessionCount", accumulatedSessionCount + currentSessionCount);
        sessionProps.setLong("accumulatedSlaveHitCount", accumulatedSlaveHitCount + slaveHitCount);
        sessionProps.setLong("accumulatedSlaveMissCount", accumulatedSlaveMissCount + slaveMissCount);
        sessionProps.setDouble("accumulatedSlaveHitPercentage", accumulatedSlaveHitPercentage);
        sessionProps.setDouble("accumulatedReadWriteRatio", accumulatedReadWriteRatio);
        sessionProps.setLong("accumulatedSwitchToMasterCount", accumulatedSwitchToMasterCount + switchToMasterCount);
        sessionProps.setLong("accumulatedSwitchToSlaveCount", accumulatedSwitchToMasterCount + switchToSlaveCount);
        sessionProps.setInt("currentSessionCount", currentSessionCount);
        sessionProps.setLong("sessionAccessCount", sessionAccessCount);
        sessionProps.setLong("writeXactCount", writeXactCount);
        sessionProps.setLong("readXactCount", readXactCount);
        sessionProps.setLong("slaveHitCount", slaveHitCount);
        sessionProps.setLong("slaveMissCount", slaveMissCount);
        sessionProps.setDouble("slaveHitPercentage", slaveHitPercentage);
        sessionProps.setDouble("readWriteRatio", readWriteRatio);
        sessionProps.setLong("switchToMasterCount", switchToMasterCount);
        sessionProps.setLong("switchToSlaveCount", switchToSlaveCount);
        return sessionProps;
    }
}
