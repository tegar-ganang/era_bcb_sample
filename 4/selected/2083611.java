package de.nava.informa.utils;

import java.net.URL;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import de.nava.informa.core.ChannelBuilderIF;
import de.nava.informa.core.ChannelFormat;
import de.nava.informa.core.ChannelIF;
import de.nava.informa.core.ItemIF;
import de.nava.informa.impl.hibernate.Channel;
import de.nava.informa.impl.hibernate.ChannelBuilder;
import de.nava.informa.impl.hibernate.Item;
import de.nava.informa.parsers.FeedParser;

/**
 * PersistChanGrpMgrTask - description...
 *  
 */
public class PersistChanGrpMgrTask extends Thread {

    private static Log logger = LogFactory.getLog(PersistChanGrpMgrTask.class);

    private PersistChanGrpMgr mgr;

    private ChannelBuilder builder;

    private ChannelBuilderIF tempBuilder;

    private Map<URL, UpdateChannelInfo> channelInfos;

    private long minChannelUpdateDelay;

    private volatile boolean running = false;

    /**
   * Construct and setup context of the PersistChanGrpMgr
   * 
   * @param mgr
   * @param minChannelUpdateDelay minimum number of millis between channel updates.
   */
    public PersistChanGrpMgrTask(PersistChanGrpMgr mgr, long minChannelUpdateDelay) {
        super("PCGrp: " + mgr.getChannelGroup().getTitle());
        this.minChannelUpdateDelay = minChannelUpdateDelay;
        this.mgr = mgr;
        builder = mgr.getBuilder();
        channelInfos = new HashMap<URL, UpdateChannelInfo>();
        tempBuilder = new de.nava.informa.impl.basic.ChannelBuilder();
    }

    /**
   * Minimum number of milliseconds between updates of channel.
   * 
   * @param minChannelUpdateDelay minimum pause between updates in milliseconds.
   */
    public void setMinChannelUpdateDelay(long minChannelUpdateDelay) {
        this.minChannelUpdateDelay = minChannelUpdateDelay;
    }

    /**
   * run - Called each iteration to process all the Channels in this Group. This will skip inactive
   * channels. -
   */
    public void run() {
        running = true;
        try {
            while (!isInterrupted()) {
                long startedLoop = System.currentTimeMillis();
                performUpdates();
                long leftToSleep = minChannelUpdateDelay - (startedLoop - System.currentTimeMillis());
                logger.debug("Going to sleep for " + leftToSleep + " millis");
                if (leftToSleep > 0) Thread.sleep(leftToSleep);
            }
        } catch (InterruptedException e) {
            logger.warn("Interrupted exception within Run method");
        } catch (Exception ignoredException) {
            ignoredException.printStackTrace();
        } finally {
            running = false;
            synchronized (this) {
                notifyAll();
            }
        }
    }

    /**
   * Returns TRUE if current thread is running.
   * 
   * @return TRUE if running.
   */
    public boolean isRunning() {
        return running;
    }

    /**
   * Interrupt the thread and return.
   * 
   * @see java.lang.Thread#interrupt()
   */
    public void interrupt() {
        interrupt(false);
    }

    /**
   * Interrupts execution of task.
   * 
   * @param wait TRUE to wait for finish of task.
   */
    public void interrupt(boolean wait) {
        super.interrupt();
        if (wait && isRunning()) {
            while (isRunning()) {
                try {
                    synchronized (this) {
                        wait(1000);
                    }
                } catch (InterruptedException e) {
                }
            }
        }
    }

    /**
   * Perform single update cycle for current group.
   */
    public void performUpdates() {
        logger.debug("Starting channel updates loop for " + mgr.getChannelGroup().getTitle());
        mgr.notifyPolling(true);
        Iterator iter = mgr.channelIterator();
        Channel nextChan;
        while (iter.hasNext()) {
            nextChan = (Channel) iter.next();
            logger.info("processing: " + nextChan);
            try {
                handleChannel(nextChan, getUpdChanInfo(nextChan));
            } catch (RuntimeException e) {
                logger.error("Error during processing: " + nextChan, e);
            } catch (NoSuchMethodError ignoreNoSuchMethod) {
                logger.error("NoSuchMethodError exception within Run method. Ignoring." + nextChan, ignoreNoSuchMethod);
            }
        }
        mgr.notifyPolling(false);
        mgr.incrPollingCounter();
    }

    /**
   * Return (and create if necessary) an UpdateChannelInfo object, which is a parallel object which
   * we use here to keep track of information about a channel.
   * 
   * @param chan - Corresponding Channel.
   */
    private UpdateChannelInfo getUpdChanInfo(Channel chan) {
        UpdateChannelInfo info = channelInfos.get(chan.getLocation());
        if (info == null) {
            info = new UpdateChannelInfo(mgr.getAcceptNrErrors());
            channelInfos.put(chan.getLocation(), info);
        }
        return info;
    }

    /**
   * Process the Channel information.
   * 
   * @param chan - Channel to process
   * @param info - UpdateChannelInfo - additional Channel Info object
   */
    private void handleChannel(Channel chan, UpdateChannelInfo info) {
        if (!info.shouldDeactivate()) {
            if (shouldUpdate(info)) {
                synchronized (builder) {
                    if (!info.getFormatDetected()) handleChannelHeader(chan, info);
                    handleChannelItems(chan, info);
                }
                info.setLastUpdatedTimestamp(System.currentTimeMillis());
            }
        } else {
            logger.info("Not processing channel: " + chan + " because exceeded error threshold.");
            return;
        }
    }

    /**
   * Returns TRUE if the cannel represented by the <code>info</code> should be updated. Decision
   * is basing on the fact of last update. If there's not enough time passed since then we don't
   * need to update this channel.
   * 
   * @param info info object of the channel.
   * 
   * @return result of the check.
   */
    private boolean shouldUpdate(UpdateChannelInfo info) {
        return System.currentTimeMillis() - info.getLastUpdatedTimestamp() > minChannelUpdateDelay;
    }

    /**
   * handleChannelHeader -
   * 
   * @param chan
   * @param info -
   */
    private void handleChannelHeader(Channel chan, UpdateChannelInfo info) {
        if (!info.getFormatDetected()) {
            logger.debug("Handling Channel Header. Format not yet detected.");
            try {
                builder.beginTransaction();
                builder.reload(chan);
                ChannelFormat format = FormatDetector.getFormat(chan.getLocation());
                chan.setFormat(format);
                info.setFormatDetected(true);
                chan.setLastUpdated(new Date());
                builder.endTransaction();
            } catch (UnknownHostException e) {
                logger.debug("Host not found: " + e.getMessage());
            } catch (Exception e) {
                info.increaseProblemsOccurred(e);
                String msg = "Exception in handleChannelHeader for : " + chan;
                logger.fatal(msg + "\n     Continue....");
            } finally {
                if (builder.inTransaction()) builder.resetTransaction();
            }
        }
    }

    /**
   * Process items in the newly parsed Channel. If they are new (i.e. not yet persisted) then add
   * them to the Channel. Note the logXXX variables were put in to do better error reporting in the
   * event of an Exception.
   * 
   * @param chan
   * @param info -
   */
    private void handleChannelItems(Channel chan, UpdateChannelInfo info) {
        ChannelIF tempChannel = null;
        int logHowManySearched = 0;
        int logHowManyAdded = 0;
        try {
            builder.beginTransaction();
            builder.reload(chan);
            tempChannel = FeedParser.parse(tempBuilder, chan.getLocation());
            InformaUtils.copyChannelProperties(tempChannel, chan);
            chan.setLastUpdated(new Date());
            mgr.notifyChannelRetrieved(chan);
            if (!tempChannel.getItems().isEmpty()) {
                Iterator it = tempChannel.getItems().iterator();
                while (it.hasNext()) {
                    logHowManySearched++;
                    de.nava.informa.impl.basic.Item transientItem = (de.nava.informa.impl.basic.Item) it.next();
                    if (!chan.getItems().contains(transientItem)) {
                        logger.info("Found new item: " + transientItem);
                        logHowManyAdded++;
                        ItemIF newItem = builder.createItem(chan, transientItem);
                        mgr.notifyItemAdded((Item) newItem);
                    }
                }
            }
            builder.endTransaction();
        } catch (UnknownHostException e) {
            logger.debug("Host not found: " + e.getMessage());
        } catch (Exception e) {
            info.increaseProblemsOccurred(e);
            String msg = "Exception in handleChannelItems. # Potential new items = " + logHowManySearched + ", # Items actually added to channel: " + logHowManyAdded + "\n     Stored Chan=" + chan + "\n     ParsedChan=" + tempChannel;
            logger.fatal(msg + "\n     Continue....");
        } finally {
            if (builder.inTransaction()) builder.resetTransaction();
        }
    }
}
