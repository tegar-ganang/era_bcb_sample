package org.logicalcobwebs.proxool.admin;

import org.logicalcobwebs.concurrent.WriterPreferenceReadWriteLock;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.logicalcobwebs.proxool.ProxoolException;
import java.util.Calendar;

/**
 * Responsbile for a single set of statistics. It rolls over to a new set
 * whenever it should. It provides access to the latest complete set
 * when it is available.
 *
 * @version $Revision: 1.9 $, $Date: 2006/01/18 14:39:58 $
 * @author bill
 * @author $Author: billhorsman $ (current maintainer)
 * @since Proxool 0.7
 */
class StatsRoller {

    private static final Log LOG = LogFactory.getLog(StatsRoller.class);

    private WriterPreferenceReadWriteLock readWriteLock = new WriterPreferenceReadWriteLock();

    private Statistics completeStatistics;

    private Statistics currentStatistics;

    private Calendar nextRollDate;

    private int period;

    private int units;

    private boolean running = true;

    private CompositeStatisticsListener compositeStatisticsListener;

    private String alias;

    public StatsRoller(String alias, CompositeStatisticsListener compositeStatisticsListener, String token) throws ProxoolException {
        this.alias = alias;
        this.compositeStatisticsListener = compositeStatisticsListener;
        nextRollDate = Calendar.getInstance();
        if (token.endsWith("s")) {
            units = Calendar.SECOND;
            nextRollDate.clear(Calendar.SECOND);
            nextRollDate.clear(Calendar.MILLISECOND);
        } else if (token.endsWith("m")) {
            units = Calendar.MINUTE;
            nextRollDate.clear(Calendar.MINUTE);
            nextRollDate.clear(Calendar.SECOND);
            nextRollDate.clear(Calendar.MILLISECOND);
        } else if (token.endsWith("h")) {
            nextRollDate.clear(Calendar.HOUR_OF_DAY);
            nextRollDate.clear(Calendar.MINUTE);
            nextRollDate.clear(Calendar.SECOND);
            nextRollDate.clear(Calendar.MILLISECOND);
            units = Calendar.HOUR_OF_DAY;
        } else if (token.endsWith("d")) {
            units = Calendar.DATE;
            nextRollDate.clear(Calendar.HOUR_OF_DAY);
            nextRollDate.clear(Calendar.MINUTE);
            nextRollDate.clear(Calendar.SECOND);
            nextRollDate.clear(Calendar.MILLISECOND);
        } else {
            throw new ProxoolException("Unrecognised suffix in statistics: " + token);
        }
        period = Integer.parseInt(token.substring(0, token.length() - 1));
        Calendar now = Calendar.getInstance();
        while (nextRollDate.before(now)) {
            nextRollDate.add(units, period);
        }
        LOG.debug("Collecting first statistics for '" + token + "' at " + nextRollDate.getTime());
        currentStatistics = new Statistics(now.getTime());
        final Thread t = new Thread() {

            public void run() {
                while (running) {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        LOG.debug("Interruption", e);
                    }
                    roll();
                }
            }
        };
        t.setDaemon(true);
        t.start();
    }

    /**
     * Cancels the timer that outputs the stats
     */
    protected void cancel() {
        running = false;
    }

    private void roll() {
        if (!isCurrent()) {
            try {
                readWriteLock.writeLock().acquire();
                if (!isCurrent()) {
                    currentStatistics.setStopDate(nextRollDate.getTime());
                    completeStatistics = currentStatistics;
                    currentStatistics = new Statistics(nextRollDate.getTime());
                    nextRollDate.add(units, period);
                    compositeStatisticsListener.statistics(alias, completeStatistics);
                }
            } catch (Throwable e) {
                LOG.error("Unable to roll statistics log", e);
            } finally {
                readWriteLock.writeLock().release();
            }
        }
    }

    private boolean isCurrent() {
        return (System.currentTimeMillis() < nextRollDate.getTime().getTime());
    }

    /**
     * @see org.logicalcobwebs.proxool.admin.Admin#connectionReturned
     */
    public void connectionReturned(long activeTime) {
        roll();
        try {
            readWriteLock.readLock().acquire();
            currentStatistics.connectionReturned(activeTime);
        } catch (InterruptedException e) {
            LOG.error("Unable to log connectionReturned", e);
        } finally {
            readWriteLock.readLock().release();
        }
    }

    /**
     * @see org.logicalcobwebs.proxool.admin.Admin#connectionRefused
     */
    public void connectionRefused() {
        roll();
        try {
            readWriteLock.readLock().acquire();
            currentStatistics.connectionRefused();
        } catch (InterruptedException e) {
            LOG.error("Unable to log connectionRefused", e);
        } finally {
            readWriteLock.readLock().release();
        }
    }

    /**
     *
     * @return
     */
    public Statistics getCompleteStatistics() {
        try {
            readWriteLock.readLock().acquire();
            return completeStatistics;
        } catch (InterruptedException e) {
            LOG.error("Couldn't read statistics", e);
            return null;
        } finally {
            readWriteLock.readLock().release();
        }
    }
}
