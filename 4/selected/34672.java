package be.roam.drest.service.bloglines;

import java.util.ArrayList;
import java.util.List;
import be.roam.drest.xml.rss.Rss20Feed;

/**
 * A list of Bloglines entries.
 */
public class BloglinesEntryList extends Rss20Feed<BloglinesSite> {

    @Override
    public void addChannel(BloglinesSite channel) {
        if (getChannelList() == null) {
            ArrayList<BloglinesSite> list = new ArrayList<BloglinesSite>();
            setChannelList(list);
        }
        getChannelList().add(channel);
    }

    /**
     * Convenience method to increase readibility: simply returns the channel list.
     * 
     * @return the list of sites or channels
     */
    public List<BloglinesSite> getSiteList() {
        return getChannelList();
    }
}
