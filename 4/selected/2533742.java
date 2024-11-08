package pl.edu.icm.pnpca.rss;

import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple rss manager which allows to store/export feed.
 * @author Aleksander Nowinski <axnow@icm.edu.pl>
 */
public class RssManagerImpl implements RssManager {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(RssManagerImpl.class);

    File file = null;

    List<SyndEntryImpl> entries = new ArrayList<SyndEntryImpl>();

    XStream xstream;

    SyndFeed feed = null;

    private String feedLink = "http://localhost/";

    private String feedTitle = "anonymousFeed";

    private String feedType = "rss_1.0";

    private String feedDescription;

    private int maxEntries = 30;

    public RssManagerImpl() {
        xstream = new XStream(new DomDriver());
    }

    public RssManagerImpl(File file) {
        this();
        this.file = file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public String getFeedDescription() {
        return feedDescription;
    }

    public void setFeedDescription(String feedDescription) {
        this.feedDescription = feedDescription;
    }

    public String getFeedLink() {
        return feedLink;
    }

    public void setFeedLink(String feedLink) {
        this.feedLink = feedLink;
    }

    public String getFeedTitle() {
        return feedTitle;
    }

    public void setFeedTitle(String feedTitle) {
        this.feedTitle = feedTitle;
    }

    public String getFeedType() {
        return feedType;
    }

    public void setFeedType(String feedType) {
        this.feedType = feedType;
    }

    public int getMaxEntries() {
        return maxEntries;
    }

    public void setMaxEntries(int maxEntries) {
        this.maxEntries = maxEntries;
    }

    public void init() {
        if (file != null && file.exists()) {
            FileInputStream is = null;
            try {
                is = new FileInputStream(file);
                entries = (List<SyndEntryImpl>) xstream.fromXML(is);
            } catch (FileNotFoundException ex) {
                log.error("Failed to initialize RSS entries from file: " + file);
            } finally {
                try {
                    is.close();
                } catch (IOException ex) {
                    log.warn("Failed to close stream. Wazzup?", ex);
                }
            }
        } else {
            entries = new ArrayList<SyndEntryImpl>();
        }
    }

    private void store() {
        if (file == null) {
            return;
        }
        try {
            FileOutputStream os = new FileOutputStream(file);
            xstream.toXML(entries, os);
            os.flush();
            os.close();
            log.debug("Stored " + entries.size() + " entries into " + file);
        } catch (IOException ioe) {
            log.error("Unexpected exception while exporting rss entries to file " + file, ioe);
        }
        return;
    }

    public void registerEntry(SyndEntryImpl entry) {
        synchronized (this) {
            entries.add(entry);
            while (entries.size() > maxEntries) {
                entries.remove(0);
            }
            feed = null;
        }
        store();
    }

    public List<SyndEntryImpl> getChannel(int numEntries) {
        List<SyndEntryImpl> res = new ArrayList<SyndEntryImpl>();
        int sx = entries.size() - numEntries;
        if (sx < 0) {
            sx = 0;
        }
        synchronized (this) {
            res.addAll(entries.subList(sx, entries.size()));
        }
        return res;
    }

    private SyndFeed buildFeed() {
        SyndFeed nfeed = new SyndFeedImpl();
        nfeed.setFeedType(feedType);
        nfeed.setTitle(feedTitle);
        nfeed.setLink(feedLink);
        nfeed.setDescription(feedDescription);
        nfeed.setEntries(entries);
        return nfeed;
    }

    public synchronized SyndFeed getFeed() {
        if (feed == null) {
            feed = buildFeed();
        }
        return feed;
    }
}
