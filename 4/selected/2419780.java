package com.limegroup.gnutella;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.vendor.UDPCrawlerPing;
import com.limegroup.gnutella.messages.vendor.UDPCrawlerPong;
import com.limegroup.gnutella.util.ArrayHashSet;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.gnutella.util.IpPortImpl;
import com.limegroup.gnutella.util.RoundRobinQueue;

/**
 * Most of the logic for the host cache goes here.
 */
public class PongPoolManager {

    private static final Log LOG = LogFactory.getLog(PongPoolManager.class);

    public static PongPoolManager instance() {
        return INSTANCE;
    }

    private static final int NUM_BUCKETS = 4;

    private static final int BUCKET_EXPIRATION_TIME = 150 * 1024;

    private static final int PONGS_PER_BUCKET = 500;

    private static final int PONGS_TO_SEND = 25;

    private static final int PINGER_DELAY = 20 * 1000;

    private static final int PINGER_REUSE = 5;

    private static final int STATS_LOGGING_TIME = 60 * 1000;

    private static final UDPCrawlerPing PING = new UDPCrawlerPing(new GUID(GUID.makeGuid()), -1, 0, UDPCrawlerPing.NEW_ONLY);

    /** 
     * All the hosts we currently have in memory.  Manipulated only
     * from the timer thread
     */
    static ArrayHashSet _everybody = new ArrayHashSet(NUM_BUCKETS * BUCKET_EXPIRATION_TIME * 2);

    private static final PongPoolManager INSTANCE = new PongPoolManager();

    private long _nextCrawlTime, _nextLoggingTime, _nextPingerAgingTime;

    private final RoundRobinQueue pingers = new RoundRobinQueue();

    private final Set agingPingers = new HashSet();

    /** the crawler runnable */
    private final Crawler _crawler = new Crawler();

    /** the runnable that saves the gnutella.net */
    private final GnutellaNetWriter _saver = new GnutellaNetWriter();

    /** the runnable which logs things */
    private final Logger _logger = new Logger("uhc.log");

    /** any messages pending to be logged */
    private final List logMessages = new ArrayList(10);

    /** 
     * stat for the pings we have received this period
     * will eventually be moved somewhere proper
     */
    private int pingsThisPeriod, upPingsThisPeriod;

    /** whether we are crawling at the moment */
    private boolean crawling;

    /** 
     * List of immutable Set objects representing the various results from pings
     */
    LinkedList buckets = new LinkedList();

    private PongPoolManager() {
        LOG.debug("creating PongPoolManager", new Exception());
        try {
            read();
        } catch (IOException bad) {
            ErrorService.error(bad);
        }
        LOG.debug("loaded " + _everybody.size() + " hosts");
        for (int i = 0; i < NUM_BUCKETS; i++) buckets.add(new ArrayHashSet(PONGS_PER_BUCKET * 2, 0.6f));
        int i = 0;
        for (Iterator iter = _everybody.iterator(); iter.hasNext(); ) {
            ((Set) buckets.get(i)).add(iter.next());
            i++;
            if (i >= buckets.size()) i = 0;
        }
        for (i = 0; i < PINGER_REUSE; i++) pingers.enqueue(new ArrayList(10));
        long now = System.currentTimeMillis();
        _nextCrawlTime = now + 10 * 1000;
        _nextLoggingTime = now + 10 * 1000;
        _nextPingerAgingTime = now + 10 * 1000;
    }

    /**
     * reads from a standard format gnutella.net, ignoring gwebcaches
     * and udp hostcaches.
     */
    private static void read() throws FileNotFoundException, IOException {
        LOG.debug("trying to read gnutella.net");
        File hostFile = new File("gnutella.net");
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(hostFile));
            while (true) {
                String line = in.readLine();
                if (line == null) break;
                try {
                    ExtendedEndpoint ee = ExtendedEndpoint.read(line);
                    if (ee.isUDPHostCache()) continue;
                    _everybody.add(new IpPortImpl(ee.getAddress(), ee.getPort()));
                } catch (ParseException pe) {
                    continue;
                }
            }
        } finally {
            try {
                if (in != null) in.close();
            } catch (IOException e) {
            }
        }
        LOG.debug("finished reading gnutella.net");
    }

    /**
     * writes the content of the _everybody set to the gnutella.net file
     * in its format.
     */
    private void write() {
        final ArrayHashSet copy = new ArrayHashSet(_everybody.size());
        copy.addAll(_everybody);
        _saver.setData(copy);
        Thread writer = new Thread(_saver);
        writer.setDaemon(false);
        writer.setName("gnutella.net saver");
        writer.setPriority(Thread.MIN_PRIORITY);
        writer.start();
    }

    private void log(String str) {
        _logger.setMsg(str);
        Thread writer = new Thread(_logger);
        writer.setName("logger");
        writer.setPriority(Thread.MIN_PRIORITY);
        writer.start();
    }

    public void runPendingTasks() {
        long now = System.currentTimeMillis();
        logStatsIfNeeded(now);
        addAgingPingersIfNeeded(now);
        if (_nextCrawlTime > now) return;
        _crawler.run();
    }

    private void addAgingPingersIfNeeded(long now) {
        if (_nextPingerAgingTime > now || agingPingers.isEmpty()) return;
        logMessages.add("preparing " + agingPingers.size() + " aged pingers for distribution");
        for (Iterator iter = agingPingers.iterator(); iter.hasNext(); ) {
            IpPort pinger = (IpPort) iter.next();
            for (int i = 0; i < PINGER_REUSE; i++) {
                List l = (List) pingers.next();
                l.add(pinger);
            }
        }
        agingPingers.clear();
        _nextPingerAgingTime = now + PINGER_DELAY;
    }

    /** 
     * Replies to a ping request.
     */
    public void replyToPing(PingRequest ping, IpPort sender) {
        if (LOG.isDebugEnabled()) LOG.debug("replying to ping " + ping + " from " + sender);
        if (!ping.supportsCachedPongs()) {
            LOG.debug("not an UHC ping");
            return;
        }
        int pongsPerBucket = PONGS_PER_BUCKET / buckets.size();
        boolean up = ping.getSupportsCachedPongData() != null && (ping.getSupportsCachedPongData()[0] & PingRequest.SCP_ULTRAPEER) == PingRequest.SCP_ULTRAPEER;
        Set results = new HashSet(PONGS_TO_SEND);
        int size = buckets.size();
        if (up) {
            upPingsThisPeriod++;
            LOG.debug("is an ultrapeer");
            agingPingers.add(sender);
        } else {
            LOG.debug("is leaf");
            List l = (List) pingers.next();
            if (!l.isEmpty()) results.add(l.remove(0));
        }
        pongsPerBucket = PONGS_TO_SEND / (size);
        if (LOG.isDebugEnabled()) LOG.debug("will try to get from each bucket " + pongsPerBucket);
        for (Iterator iter = buckets.iterator(); iter.hasNext() && results.size() < PONGS_TO_SEND; ) {
            addSomeRandom((ArrayHashSet) iter.next(), results, pongsPerBucket);
        }
        if (LOG.isDebugEnabled()) LOG.debug("sending back pongs " + results.size());
        PingReply reply;
        reply = PingReply.create(ping.getGUID(), (byte) 0, sender, results);
        UDPService.instance().send(reply, sender);
        pingsThisPeriod++;
    }

    /**
     * logs some usage statistics
     * TODO: move this in its own class eventually
     */
    private void logStatsIfNeeded(long now) {
        if (now < _nextLoggingTime) return;
        _nextLoggingTime = now + STATS_LOGGING_TIME;
        StringBuffer message = new StringBuffer();
        message.append("LOG as of ");
        message.append(new Date(now)).append(":\n");
        message.append("hosts in pool ").append(_everybody.size()).append("\n");
        message.append("ultrapeer buckets: ");
        for (int i = 0; i < PINGER_REUSE; i++) {
            List l = (List) pingers.next();
            message.append(l.size()).append(" ");
        }
        message.append("\n");
        message.append("pings received since last period ").append(upPingsThisPeriod).append("/").append(pingsThisPeriod);
        if (logMessages.size() > 0) {
            message.append("\nother events:\n");
            for (int i = 0; i < logMessages.size(); i++) message.append("  * ").append(logMessages.remove(0)).append("\n");
        }
        pingsThisPeriod = 0;
        upPingsThisPeriod = 0;
        log(message.toString());
    }

    /**
     * Puts number elements at random from the source set to the dest set.
     * Relies on the fact that the natural hashCode and equals methods
     * are used in both the source and dest sets.  
     * (i.e. do not use with custom hashers)
     */
    private static void addSomeRandom(ArrayHashSet src, Set dest, int num) {
        if (num <= 0) return;
        int originalSize = dest.size();
        if (src.size() <= num) {
            dest.addAll(src);
            return;
        }
        while (dest.size() < originalSize + num) dest.add(src.getRandom());
    }

    /**
     * The event that crawls the network, trying to find at least certain
     * number of hosts for the new bucket.
     */
    private class Crawler implements Runnable {

        long lastCrawlStartTime;

        /** the hosts we have discovered in this crawl */
        Set hosts;

        /** the hosts we discovered in the last iteration */
        Set hostsIter;

        /** the set of hosts we are about to replace */
        ArrayHashSet discarded;

        /** 
         * the set of entry points we have pinged this run.
         */
        Set pinged;

        public void run() {
            LOG.debug("running crawler");
            if (!crawling) startCrawl(); else processPongs();
            int nextRun = crawling ? 300 : BUCKET_EXPIRATION_TIME;
            _nextCrawlTime = System.currentTimeMillis() + nextRun;
        }

        /** starts a new crawl */
        private void startCrawl() {
            logMessages.add("starting a crawl");
            crawling = true;
            hosts = new ArrayHashSet(PONGS_PER_BUCKET * 2);
            pinged = new HashSet();
            lastCrawlStartTime = System.currentTimeMillis();
            discarded = (ArrayHashSet) buckets.getLast();
            sendPings();
        }

        /** processes the received pongs */
        private void processPongs() {
            if (LOG.isDebugEnabled()) LOG.debug("processing pongs, hosts is " + hosts.size());
            List pongs = UDPService.instance().getReceivedPongs();
            for (Iterator iter = pongs.iterator(); iter.hasNext(); ) {
                UDPCrawlerPong pong = (UDPCrawlerPong) iter.next();
                hostsIter.addAll(pong.getUltrapeers());
            }
            if (!pongs.isEmpty()) {
                pongs.clear();
                if (LOG.isDebugEnabled()) LOG.debug("received " + hostsIter.size() + " hosts this iteration");
                Set copy = new HashSet(hostsIter);
                copy.removeAll(_everybody);
                if (LOG.isDebugEnabled()) LOG.debug("out of which we didn't know about " + copy.size());
                hosts.addAll(copy);
            }
            if (hosts.size() >= PONGS_PER_BUCKET) {
                logMessages.add("crawl finished");
                crawling = false;
                buckets.addFirst(hosts);
                buckets.removeLast();
                _everybody.removeAll(discarded);
                _everybody.addAll(hosts);
                write();
                return;
            }
            long now = System.currentTimeMillis();
            if (now - lastCrawlStartTime > 5 * 1000) {
                LOG.debug("getting more entry points");
                lastCrawlStartTime = now;
                sendPings();
            }
        }

        /** sends some pings */
        private void sendPings() {
            LOG.debug("sending pings");
            int hostsToPing = Math.max(((PONGS_PER_BUCKET - hosts.size()) / 60), 5);
            if (LOG.isDebugEnabled()) LOG.debug("will ping " + hostsToPing);
            Set batch = new HashSet();
            int selectionAttempts = 0;
            while (batch.size() < hostsToPing) {
                int entrySize = batch.size();
                if (hostsIter != null) addSomeRandom((ArrayHashSet) hostsIter, batch, hostsToPing - batch.size());
                addSomeRandom((ArrayHashSet) hosts, batch, hostsToPing - batch.size());
                addSomeRandom(discarded, batch, hostsToPing - batch.size());
                addSomeRandom(_everybody, batch, hostsToPing - batch.size());
                batch.removeAll(pinged);
                if (batch.size() == entrySize) {
                    selectionAttempts++;
                    if (selectionAttempts > 100) {
                        LOG.debug("we need a larger gnutella.net.");
                        batch.addAll(hosts);
                        batch.addAll(discarded);
                        batch.addAll(_everybody);
                        batch.removeAll(pinged);
                        if (batch.isEmpty()) {
                            LOG.warn("hopeless");
                            _everybody.addAll(hosts);
                            write();
                        }
                        break;
                    }
                }
            }
            hostsIter = new ArrayHashSet(batch.size() * 20);
            for (Iterator iter = batch.iterator(); iter.hasNext(); ) UDPService.instance().send(PING, (IpPort) iter.next());
            pinged.addAll(batch);
        }
    }

    private class GnutellaNetWriter implements Runnable {

        private Set data;

        private volatile boolean running = false;

        public void setData(Set s) {
            if (!running) data = s;
        }

        public synchronized void run() {
            running = true;
            Writer out = null;
            try {
                LOG.debug("about to write +" + data.size() + " hosts to gnutella.net");
                File hostFile = new File("gnutella.net");
                out = new BufferedWriter(new FileWriter(hostFile));
                for (Iterator iter = data.iterator(); iter.hasNext(); ) {
                    IpPort current = (IpPort) iter.next();
                    ExtendedEndpoint tmp = new ExtendedEndpoint(current.getAddress(), current.getPort());
                    tmp.write(out);
                }
                out.flush();
            } catch (IOException bad) {
                ErrorService.error(bad);
            } finally {
                data = null;
                try {
                    if (out != null) out.close();
                } catch (IOException ignored) {
                }
                running = false;
                System.gc();
                System.runFinalization();
            }
            LOG.debug("finished writing gnutella.net");
        }
    }

    private class Logger implements Runnable {

        private PrintStream logger;

        public Logger(String fileName) {
            try {
                logger = new PrintStream(new BufferedOutputStream(new FileOutputStream(new File(fileName))));
            } catch (IOException bad) {
                ErrorService.error(bad);
            }
        }

        /** the message to log */
        private String msg;

        public void setMsg(String msg) {
            if (!running) this.msg = msg;
        }

        private volatile boolean running = false;

        public synchronized void run() {
            if (logger == null) return;
            running = true;
            try {
                logger.println(msg);
            } finally {
                logger.flush();
                running = false;
            }
        }
    }
}
