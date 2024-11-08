package org.openexi.fujitsu.proc.io.compression;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import org.openexi.fujitsu.proc.common.EXIOptions;

final class ChannelKeeper {

    private int m_totalValueCount;

    private final ArrayList<Channel> m_smallChannelList;

    private final ArrayList<Channel> m_largeChannelList;

    private final HashMap<String, Channel[]> m_channelMap;

    private final ChannelFactory m_channelFactory;

    private int m_blockSize;

    ChannelKeeper(ChannelFactory channelFactory) {
        m_totalValueCount = 0;
        m_smallChannelList = new ArrayList();
        m_largeChannelList = new ArrayList();
        m_channelMap = new HashMap();
        m_channelFactory = channelFactory;
        m_blockSize = EXIOptions.BLOCKSIZE_DEFAULT;
    }

    public void reset() {
        m_totalValueCount = 0;
        m_smallChannelList.clear();
        m_largeChannelList.clear();
        m_channelMap.clear();
    }

    public void finish() {
        Collections.sort(m_smallChannelList);
        Collections.sort(m_largeChannelList);
    }

    void setBlockSize(int blockSize) {
        m_blockSize = blockSize;
    }

    List<Channel> getSmallChannels() {
        return m_smallChannelList;
    }

    List<Channel> getLargeChannels() {
        return m_largeChannelList;
    }

    int getTotalValueCount() {
        return m_totalValueCount;
    }

    Channel getChannel(String name, String uri) {
        Channel[] candidates;
        Channel channel;
        final int n;
        if ((candidates = m_channelMap.get(name)) != null) {
            int i;
            final int len;
            for (i = 0, len = candidates.length; i < len; i++) {
                channel = candidates[i];
                assert name.equals(channel.name);
                if (uri.equals(channel.uri)) {
                    return channel;
                }
            }
            final Channel[] _candidates;
            _candidates = new Channel[candidates.length + 1];
            System.arraycopy(candidates, 0, _candidates, 0, candidates.length);
            candidates = _candidates;
            n = len;
        } else {
            candidates = new Channel[1];
            n = 0;
        }
        channel = m_channelFactory.createChannel(name, uri, m_totalValueCount, this);
        m_smallChannelList.add(channel);
        candidates[n] = channel;
        m_channelMap.put(name, candidates);
        return channel;
    }

    boolean incrementValueCount(Channel channel) {
        if (++channel.valueCount == 101) {
            m_smallChannelList.remove(channel);
            m_largeChannelList.add(channel);
        }
        return ++m_totalValueCount == m_blockSize;
    }
}
