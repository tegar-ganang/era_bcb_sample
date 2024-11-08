package org.anuta.xmltv;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import org.anuta.xmltv.beans.Channel;
import org.anuta.xmltv.xmlbeans.Image;
import org.anuta.xmltv.xmlbeans.Programme;
import org.anuta.xmltv.xmlbeans.TvDocument;
import org.anuta.xmltv.xmlbeans.TvDocument.Tv;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.Oustermiller.util.StringHelper;
import edu.emory.mathcs.backport.java.util.concurrent.BlockingQueue;
import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentLinkedQueue;
import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;
import edu.emory.mathcs.backport.java.util.concurrent.ThreadPoolExecutor;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

public class XMLTVGrabberThreadPool extends ThreadPoolExecutor {

    private static final Log log = LogFactory.getLog(XMLTVGrabberThreadPool.class);

    private XMLTVGrabberTaskFactory factory;

    private List<Channel> channels = new ArrayList<Channel>();

    private int daysToGrab = 4;

    private CountDownLatch doneSignal = null;

    private ConcurrentLinkedQueue queue = new ConcurrentLinkedQueue();

    public XMLTVGrabberThreadPool(final int corePoolSize, final int maximumPoolSize, final long keepAliveTime, final TimeUnit unit, final BlockingQueue workQueue, final int daysToGrab, final List<Channel> channels, final XMLTVGrabberTaskFactory factory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
        setDaysToGrab(daysToGrab);
        setChannels(channels);
        setFactory(factory);
    }

    public int getDaysToGrab() {
        return daysToGrab;
    }

    public void setDaysToGrab(int daysToGrab) {
        this.daysToGrab = daysToGrab;
    }

    public List<Channel> getChannels() {
        return channels;
    }

    public void setChannels(List<Channel> channels) {
        this.channels = channels;
    }

    public TvDocument grab() {
        if (log.isInfoEnabled()) {
            log.info("Starting the grabber");
        }
        if (getFactory().getCache() != null) {
            doneSignal = new CountDownLatch(channels.size() * daysToGrab + 1);
            execute(getFactory().getCache().createCleaner());
        } else {
            doneSignal = new CountDownLatch(channels.size() * daysToGrab);
        }
        SimpleDateFormat sdfDateTime = new SimpleDateFormat("yyyyMMddHHmmss ZZZZ");
        int days = getDaysToGrab();
        TvDocument doc = TvDocument.Factory.newInstance();
        Tv tv = doc.addNewTv();
        tv.setGeneratorInfoName("anuta xmltv generator");
        tv.setGeneratorInfoUrl("http://www.anuta.org/xmltv");
        tv.setDate(sdfDateTime.format(new Date()));
        tv.setSourceDataUrl("http://www.tvgids.nl");
        int day = 0;
        while (day < days) {
            if (log.isDebugEnabled()) {
                log.debug("We are here");
            }
            for (Channel channel : getChannels()) {
                XMLTVGrabberTask task = getFactory().getGrabberTask(day, channel);
                execute(task);
            }
            day++;
        }
        log.debug("Start waiting");
        try {
            doneSignal.await();
        } catch (InterruptedException e) {
            log.debug(e);
            Thread.currentThread().interrupt();
        }
        log.debug("End waiting");
        shutdown();
        try {
            awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            log.debug(e);
            Thread.currentThread().interrupt();
        }
        for (Channel channel : getChannels()) {
            if (log.isDebugEnabled()) {
                log.debug("Add channel " + channel + " to xml");
            }
            org.anuta.xmltv.xmlbeans.Channel channelXml = tv.addNewChannel();
            channelXml.setId(channel.getXmltvChannelId());
            channelXml.setDisplayName(StringHelper.unescapeHTML(channel.getChannelName()));
            if (channel.getChannelLogo() != null) {
                Image icon = channelXml.addNewIcon();
                icon.setSrc(channel.getChannelLogo());
            }
        }
        Iterator it = queue.iterator();
        while (it.hasNext()) {
            TvDocument chandoc = (TvDocument) it.next();
            Programme[] progs = chandoc.getTv().getProgrammeArray();
            if ((progs != null) && (progs.length > 0)) {
                for (int i = 0; i < progs.length; i++) {
                    tv.addNewProgramme().set(progs[i]);
                }
            }
        }
        log.debug("ENDED");
        return doc;
    }

    @Override
    protected void afterExecute(Runnable task, Throwable t) {
        if (log.isDebugEnabled()) {
            log.debug("After execute thread " + task);
        }
        if (task instanceof XMLTVGrabberTask) {
            XMLTVGrabberTask gtask = (XMLTVGrabberTask) task;
            if (log.isDebugEnabled()) {
                log.debug(gtask.getResult());
            }
            queue.add(gtask.getResult());
        }
        super.afterExecute(task, t);
        doneSignal.countDown();
        if (log.isDebugEnabled()) {
            log.debug("There are " + doneSignal + " tasks left...");
        }
    }

    @Override
    protected void beforeExecute(Thread t, Runnable task) {
        if (log.isDebugEnabled()) {
            log.debug("Before execute thread " + task);
        }
        super.beforeExecute(t, task);
    }

    @Override
    public void execute(Runnable task) {
        if (log.isDebugEnabled()) {
            log.debug("Execute thread " + task);
        }
        super.execute(task);
    }

    public XMLTVGrabberTaskFactory getFactory() {
        return factory;
    }

    public void setFactory(XMLTVGrabberTaskFactory factory) {
        this.factory = factory;
    }
}
