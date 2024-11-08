package telkku.channellist;

import java.util.Vector;
import syndication.rss.Rss;
import syndication.rss.Item;

/**
 *
 * @author Omistaja
 */
public class Channel {

    private String channelName = null;

    private Vector<Show> channelShows = null;

    public Channel(Rss feed) {
        channelName = feed.getChannel().getTitle();
        int channelNameEnd = channelName.indexOf(" - ");
        channelName = channelName.substring(0, channelNameEnd);
        channelShows = new Vector<Show>();
        Vector<Item> items = feed.getChannel().getItems();
        if (items != null && items.size() > 0) {
            int itemCount = items.size();
            for (int i = 0; i < itemCount; i++) {
                channelShows.add(new Show(items.elementAt(i)));
            }
        }
    }

    public String getChannelName() {
        return channelName;
    }

    public Vector<Show> getShows() {
        return channelShows;
    }

    public String getShowStartingAt(int hour, int min) {
        for (int i = 0; i < channelShows.size(); i++) {
            Show s = channelShows.elementAt(i);
            if (s.getStartHour() == hour && s.getStartMinutes() == min) {
                return s.getShowName();
            }
        }
        return null;
    }
}
