package org.ludo.plugins.azureus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;
import org.gudy.azureus2.plugins.Plugin;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ipfilter.IPBlocked;
import org.gudy.azureus2.plugins.ipfilter.IPRange;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.ludo.config.ConfigEntry;
import org.ludo.logging.DefaultMiniLogger;
import org.ludo.logging.MiniLogger;
import org.ludo.net.IpRange;
import org.ludo.safepeer.SafePeer;
import org.ludo.safepeer.SafepeerConfigConstants;
import org.ludo.safepeer.cache.DefaultEvilIPCache;

/** Plugin for Azureus allowing to export the evil IPs defined in SafePeer to the IpFilter
 * class of Azureus.
 * @author  <a href="mailto:masterludo@gmx.net">Ludovic Kim-Xuan Galibert</a>
 * @revision $Id: AzureusIpFilterExporter.java,v 1.4 2004/12/12 10:06:42 masterludo Exp $
 * @created 10. November 2003, 12:35
 */
public class AzureusIpFilterExporter implements Plugin {

    /** Holds the SafePeer */
    private SafePeer safePeer;

    /** The version number */
    public static final String INTERNAL_REVISION = "2.5.1";

    private long start;

    /** Creates a new AzureusIpFilterExporter
   */
    public AzureusIpFilterExporter() {
        start = System.currentTimeMillis();
    }

    /** Export the IPs to the Azureus IpFilter
   * @param aPlugin  the PluginInterface
   */
    protected synchronized void exportToIpFilter(PluginInterface aPlugin) {
        List evilEntries = getSafePeer().getCache().getEvilPeerEntries();
        Iterator it = evilEntries.iterator();
        List newRanges = new ArrayList(evilEntries.size());
        List oldRanges = new ArrayList(Arrays.asList(aPlugin.getIPFilter().getRanges()));
        Collections.sort(oldRanges);
        int newRangesCount = 0;
        while (it.hasNext()) {
            IpRange entry = (IpRange) it.next();
            IPRange range = aPlugin.getIPFilter().createRange(true);
            range.setStartIP(entry.getStartIp());
            range.setEndIP(entry.getEndIp());
            range.setDescription(entry.getDescription());
            range.checkValid();
            if (!containsRange(oldRanges, range)) {
                newRangesCount++;
                aPlugin.getIPFilter().addRange(range);
            }
        }
        if (0 == newRangesCount) aPlugin.getIPFilter().markAsUpToDate();
        LoggerChannel logger = aPlugin.getLogger().getChannel("SafePeer");
        String entry = "entry";
        if (newRangesCount > 1) entry = "entries";
        String logMsg = "SafePeer exported " + newRangesCount + " " + entry + " to the IpFilter (currently " + aPlugin.getIPFilter().getRanges().length + " " + entry + ")";
        logger.log(LoggerChannel.LT_INFORMATION, logMsg);
        long end = System.currentTimeMillis();
        DefaultEvilIPCache.getInstance().clear();
        MiniLogger log = DefaultMiniLogger.getInstance(SafePeer.LOG_IDENTIFIER);
        log.info(logMsg);
    }

    protected synchronized boolean containsRange(List someRanges, IPRange aRange) {
        boolean result = false;
        result = Collections.binarySearch(someRanges, aRange) >= 0;
        return result;
    }

    /** Gets the SafePeer
   * @return  a SafePeer, cannot be null
   */
    public SafePeer getSafePeer() {
        return safePeer;
    }

    /** Sets the SafePeer
   * @param aSafePeer  a SafePeer, must not be null
   */
    protected void setSafePeer(SafePeer aSafePeer) {
        safePeer = aSafePeer;
    }

    /** Initialize this plugin by exporting the evil IPs to the IpFilter class.<br>
   * There is currently no view to add to the plugin interface.
   * @param aPlugin  the PluginInterface calling the initialize method
   */
    public void initialize(PluginInterface aPlugin) {
        ConfigEntry entry = new ConfigEntry(aPlugin.getPluginDirectoryName(), SafepeerConfigConstants.CONFIG_FILE_NAME);
        boolean isGuiEnabled = Boolean.valueOf(entry.getProperty("enable.gui")).booleanValue();
        if (isGuiEnabled) {
            aPlugin.getUIManager().getSWTManager().addView(new AzureusSafePeerView(entry));
        }
        boolean isEnabled = Boolean.valueOf(entry.getProperty(SafepeerConfigConstants.ENABLE_SAFEPEER)).booleanValue();
        if (isEnabled) {
            export(entry, aPlugin);
        } else {
            LoggerChannel logger = aPlugin.getLogger().getChannel("SafePeer");
            logger.log(LoggerChannel.LT_INFORMATION, "SafePeer is currently disabled. To enable SafePeer, change the safepeer.properties file to: enable.safepeer=true");
        }
    }

    /**
   * Exports the IP ranges to IpFilter synchronously or asynchronously corresponding to the configuration.
   * @param aConfigEntry  the ConfigEntry, must not be null.
   * @param aPlugin  the PluginInterface, must not be null.
   */
    protected void export(ConfigEntry aConfigEntry, PluginInterface aPlugin) {
        final boolean doPeriodicUpdate = Boolean.valueOf(aConfigEntry.getProperty(AzureusConfigConstants.ENABLE_PERIODIC_UPDATE)).booleanValue();
        boolean isAsync = Boolean.valueOf(aConfigEntry.getProperty(AzureusConfigConstants.ASYNC_LOADING)).booleanValue();
        LoggerChannel logger = aPlugin.getLogger().getChannel("SafePeer");
        DefaultMiniLogger.createLoggerInstance(SafePeer.LOG_IDENTIFIER, aConfigEntry);
        MiniLogger log = DefaultMiniLogger.getInstance(SafePeer.LOG_IDENTIFIER);
        if (isAsync) {
            String logMsg = "Starting SafePeer Plugin asynchronously";
            logger.log(LoggerChannel.LT_INFORMATION, logMsg);
            log.info(logMsg);
        } else {
            String logMsg = "Starting SafePeer Plugin synchronously";
            logger.log(LoggerChannel.LT_INFORMATION, logMsg);
            log.info(logMsg);
        }
        long period = 0;
        if (doPeriodicUpdate) {
            try {
                String timer = aConfigEntry.getProperty(AzureusConfigConstants.UPDATE_TIMER);
                StringTokenizer tokenizer = new StringTokenizer(timer, ":");
                long hours = Long.valueOf(tokenizer.nextToken()).longValue();
                long minutes = Long.valueOf(tokenizer.nextToken()).longValue();
                long seconds = Long.valueOf(tokenizer.nextToken()).longValue();
                period = hours * 60 * 60 * 1000 + minutes * 60 * 1000 + seconds * 1000;
                String logMsg = "Periodic update of the IpFilter will occur every " + hours + "h" + minutes + "m" + seconds + "s";
                logger.log(LoggerChannel.LT_INFORMATION, logMsg);
                log.info(logMsg);
            } catch (Exception ex) {
                period = 2 * 60 * 60 * 1000 + 0 * 60 * 1000 + 0 * 1000;
                String logMsg = "Periodic update of the IpFilter will occur every 2h";
                logger.log(LoggerChannel.LT_INFORMATION, logMsg);
                log.info(logMsg);
            }
        }
        if (isAsync) {
            final ConfigEntry entry = aConfigEntry;
            final long updatePeriod = period;
            final PluginInterface plugin = aPlugin;
            Thread t = new Thread() {

                public void run() {
                    setSafePeer(new SafePeer(entry));
                    exportToIpFilter(plugin);
                    if (doPeriodicUpdate) {
                        startExportTask(updatePeriod, plugin);
                    }
                }
            };
            t.setName("SafePeerAsyncLoader");
            t.setDaemon(true);
            t.start();
        } else {
            setSafePeer(new SafePeer(aConfigEntry));
            exportToIpFilter(aPlugin);
            if (doPeriodicUpdate) {
                startExportTask(period, aPlugin);
            }
        }
        startBlockLogTask(5 * 60 * 1000, aPlugin);
    }

    /**
   * Starts the export in a periodic task.
   * @param aPeriod  the period of the task if it's periodic.
   * @param aPlugin  the PluginInterface.
   */
    protected void startExportTask(long aPeriod, PluginInterface aPlugin) {
        Timer timer = new Timer(true);
        final PluginInterface plugin = aPlugin;
        TimerTask task = new TimerTask() {

            public void run() {
                MiniLogger log = DefaultMiniLogger.getInstance(SafePeer.LOG_IDENTIFIER);
                LoggerChannel logger = plugin.getLogger().getChannel("SafePeer");
                String logMsg = "Performing periodic IpFilter update...";
                logger.log(LoggerChannel.LT_INFORMATION, logMsg);
                log.info(logMsg);
                boolean cacheUpdated = getSafePeer().updateCache();
                if (cacheUpdated) {
                    logMsg = "Ip ranges retrieved, performing update...";
                    logger.log(LoggerChannel.LT_INFORMATION, logMsg);
                    log.info(logMsg);
                    exportToIpFilter(plugin);
                    logMsg = "Periodic update performed";
                    logger.log(LoggerChannel.LT_INFORMATION, logMsg);
                    log.info(logMsg);
                } else {
                    logMsg = "Ip ranges of the database could not be updated, old ranges will be used";
                    logger.log(LoggerChannel.LT_WARNING, logMsg);
                    log.warn(logMsg);
                }
            }
        };
        timer.scheduleAtFixedRate(task, aPeriod, aPeriod);
    }

    private static int blockCount = 0;

    /**
   * Starts the task for logging blocked IPs.  
   * @param aPeriod
   * @param aPlugin
   */
    protected void startBlockLogTask(long aPeriod, PluginInterface aPlugin) {
        Timer timer = new Timer(true);
        final PluginInterface plugin = aPlugin;
        TimerTask task = new TimerTask() {

            public void run() {
                MiniLogger log = DefaultMiniLogger.getInstance(SafePeer.LOG_IDENTIFIER);
                IPBlocked[] blocked = plugin.getIPFilter().getBlockedIPs();
                int len = blocked.length;
                for (int i = blockCount; i < len; i++) {
                    IPBlocked ip = blocked[i];
                    log.info("IP '" + ip.getBlockedIP() + "' in range '" + ip.getBlockingRange().getDescription() + "' has been blocked on '" + new Date(ip.getBlockedTime()) + "' for torrent' " + ip.getBlockedTorrentName() + "'");
                }
                blockCount = len;
            }
        };
        timer.scheduleAtFixedRate(task, aPeriod, aPeriod);
    }
}
