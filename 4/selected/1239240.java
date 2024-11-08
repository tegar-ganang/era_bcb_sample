package org.anuta.xmltv;

import java.util.ArrayList;
import java.util.List;
import org.anuta.xmltv.beans.Channel;
import org.anuta.xmltv.exceptions.ExportException;
import org.anuta.xmltv.export.Export;
import org.anuta.xmltv.xmlbeans.TvDocument;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.LogFactoryImpl;
import edu.emory.mathcs.backport.java.util.concurrent.ArrayBlockingQueue;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

/**
 * XMLTVGrabber.
 *
 * @author fedor
 */
public class XMLTVGrabber {

    private static final long TIMEOUT = 10000L;

    private static final int MAX_DAYS_TO_GRAB = 4;

    private static final Log log = LogFactoryImpl.getLog(XMLTVGrabber.class);

    private static final int MAX_THREADS = 5;

    private List<Channel> channels = new ArrayList<Channel>();

    private Export export;

    private int daysToGrab = MAX_DAYS_TO_GRAB;

    private ArrayBlockingQueue queue;

    private XMLTVGrabberTaskFactory factory;

    public final XMLTVGrabberTaskFactory getFactory() {
        return factory;
    }

    public final void setFactory(final XMLTVGrabberTaskFactory factory) {
        this.factory = factory;
    }

    /**
     * Start grabbing.
     */
    public final void grab() {
        queue = new ArrayBlockingQueue(getChannels().size() * daysToGrab);
        XMLTVGrabberThreadPool threadPool = new XMLTVGrabberThreadPool(MAX_THREADS, MAX_THREADS, TIMEOUT, TimeUnit.SECONDS, queue, getDaysToGrab(), getChannels(), getFactory());
        TvDocument doc = threadPool.grab();
        try {
            if (log.isDebugEnabled()) {
                log.debug(doc.toString());
            }
            if (getExport() != null) {
                getExport().export(doc);
            }
        } catch (ExportException e) {
            if (log.isErrorEnabled()) {
                log.error("Unable to export document", e);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("ALL DONE");
        }
    }

    public final List<Channel> getChannels() {
        return channels;
    }

    public final void setChannels(final List<Channel> channels) {
        this.channels = channels;
    }

    public final Export getExport() {
        return export;
    }

    public final void setExport(final Export export) {
        this.export = export;
    }

    public final int getDaysToGrab() {
        return daysToGrab;
    }

    public final void setDaysToGrab(final int daysToGrab) {
        this.daysToGrab = daysToGrab;
    }
}
