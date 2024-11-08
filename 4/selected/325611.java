package telkku.channellist;

import java.util.Vector;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.Application;
import org.jdesktop.application.ApplicationContext;
import telkku.TelkkuApp;
import syndication.rss.Rss;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Omistaja
 */
public class ChannelList {

    private Vector<Channel> channels = null;

    private Vector<IntPair> startTimePairs = null;

    private ResourceMap resourceMap = null;

    public ChannelList(Rss[] channelFeeds) {
        ApplicationContext appContext = Application.getInstance(TelkkuApp.class).getContext();
        resourceMap = appContext.getResourceMap(ChannelList.class);
        int channelCount = resourceMap.getInteger("ChannelList.channelCount");
        if (channelFeeds != null) {
            channels = new Vector<Channel>();
            startTimePairs = new Vector<IntPair>();
            int index;
            int feedCount = channelFeeds.length;
            for (index = 0; index < channelFeeds.length; index++) {
                if (channelFeeds[index] != null) {
                    channels.add(new Channel(channelFeeds[index]));
                }
            }
            int maxChannelItems = 0;
            for (index = 0; index < channels.size(); index++) {
                Channel chan = channels.get(index);
                int showCount = chan.getShows().size();
                if (showCount > maxChannelItems) {
                    maxChannelItems = showCount;
                }
            }
            int chanCount = channels.size();
            int chanIndex = 0;
            for (index = 0; index < maxChannelItems; index++) {
                for (chanIndex = 0; chanIndex < chanCount; chanIndex++) {
                    Channel chan = channels.get(chanIndex);
                    if (chan.getShows().size() > index) {
                        Show s = chan.getShows().get(index);
                        IntPair p = new IntPair(s.getStartHour(), s.getStartMinutes());
                        if (startTimePairs.contains(p) == false) {
                            int hInd = getHourIndex(startTimePairs, s.getStartHour());
                            if (hInd == startTimePairs.size()) {
                                startTimePairs.add(p);
                            } else {
                                startTimePairs.insertElementAt(p, hInd);
                            }
                        }
                    }
                }
            }
            sortHours(startTimePairs);
        }
    }

    public Vector<IntPair> getStartTimes() {
        if (startTimePairs == null) {
            return null;
        }
        return startTimePairs;
    }

    public int getChannelCount() {
        if (channels == null) {
            return 0;
        }
        return channels.size();
    }

    public Channel getChannelAt(int index) {
        if (channels == null) {
            return null;
        }
        return channels.elementAt(index);
    }

    private int getHourIndex(Vector<IntPair> v, int h) {
        for (int i = 0; i < v.size(); i++) {
            if (v.get(i).first() == h) {
                return i;
            }
        }
        return v.size();
    }

    private void sortHours(Vector<IntPair> v) {
        int index;
        for (index = 0; index < v.size(); index++) {
            if (v.get(index).first() == 23) {
                break;
            }
        }
        for (; index < v.size(); index++) {
            if (v.get(index).first() != 23) {
                break;
            }
        }
        List<IntPair> beforeMidnight = v.subList(0, index);
        List<IntPair> afterMidnight = v.subList(index, v.size());
        Comparator<IntPair> comparator = new StartTimeComparer();
        Collections.sort(beforeMidnight, comparator);
        Collections.sort(afterMidnight, comparator);
    }
}
