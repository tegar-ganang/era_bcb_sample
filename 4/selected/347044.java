package org.ttalbott.mytelly;

import java.util.*;
import org.ttalbott.mytelly.ChannelData;

/**
 *
 * @author  Tom Talbott
 * @version 
 */
public class Channels extends java.lang.Object {

    private static Channels m_instance = null;

    ChannelData m_channelData = null;

    /** Creates new Favorites */
    private Channels() {
    }

    public static Channels getInstance() {
        if (m_instance == null) m_instance = new Channels();
        return m_instance;
    }

    public static void release() {
        m_instance = null;
    }

    public void setChannelData(ChannelData channelData) {
        m_channelData = channelData;
    }

    public ChannelData getChannelData() {
        return m_channelData;
    }

    public void add(String id, Map data) {
        if (m_channelData != null) {
            m_channelData.add(id, data);
        }
    }

    public Map getItems() {
        if (m_channelData != null) {
            return m_channelData.getItems();
        } else return null;
    }

    public void getSortedChannelDescriptions(Collection outChannelDescs) {
        if (m_channelData != null) {
            m_channelData.getSortedChannelDescriptions(outChannelDescs);
        }
    }
}
