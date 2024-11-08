package org.anuta.xmltv;

import java.util.HashSet;
import java.util.Set;
import org.anuta.imdb.IMDBAccess;
import org.anuta.xmltv.beans.Channel;
import org.anuta.xmltv.cache.CacheManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class XMLTVGrabberTaskFactory {

    private static final Log log = LogFactory.getLog(XMLTVGrabberTaskFactory.class);

    private long maxOverlap = 10;

    private int overlapFixMode = 0;

    private Set starRatingGanres = new HashSet();

    private CacheManager cache;

    private IMDBAccess imdbAccess = null;

    public IMDBAccess getImdbAccess() {
        return imdbAccess;
    }

    public void setImdbAccess(IMDBAccess imdbAccess) {
        this.imdbAccess = imdbAccess;
    }

    public CacheManager getCache() {
        return cache;
    }

    public void setCache(CacheManager cache) {
        this.cache = cache;
    }

    public Set getStarRatingGanres() {
        return starRatingGanres;
    }

    public void setStarRatingGanres(Set starRatingGanres) {
        this.starRatingGanres = starRatingGanres;
    }

    public XMLTVGrabberTask getGrabberTask(int day, Channel channel) {
        if (log.isDebugEnabled()) {
            log.debug("Making task for day: " + day + " of channel " + channel.getChannelId() + "-" + channel.getChannelName());
        }
        XMLTVGrabberTask task = new XMLTVGrabberTask();
        task.setDay(day);
        task.setMaxOverlap(getMaxOverlap());
        task.setOverlapFixMode(getOverlapFixMode());
        task.setStarRatingGanres(getStarRatingGanres());
        task.setCache(getCache());
        task.setImdbAccess(getImdbAccess());
        task.setChannel(channel);
        return task;
    }

    public long getMaxOverlap() {
        return maxOverlap;
    }

    public void setMaxOverlap(long maxOverlap) {
        this.maxOverlap = maxOverlap;
    }

    public int getOverlapFixMode() {
        return overlapFixMode;
    }

    public void setOverlapFixMode(int overlapFixMode) {
        this.overlapFixMode = overlapFixMode;
    }
}
