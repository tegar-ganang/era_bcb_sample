package net.videgro.oma.services;

import java.util.List;
import net.videgro.oma.domain.Channel;
import net.videgro.oma.domain.Member;
import net.videgro.oma.managers.ChannelManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@SuppressWarnings("unchecked")
public class ChannelService implements IChannelService {

    protected final Log logger = LogFactory.getLog(getClass());

    private ChannelManager channelManager = null;

    public ChannelService() {
        super();
    }

    public ChannelManager getChannelManager() {
        return channelManager;
    }

    public void setChannelManager(ChannelManager channelManager) {
        this.channelManager = channelManager;
    }

    public List getChannelList() {
        List list = null;
        try {
            list = channelManager.getChannelList(false);
        } catch (RuntimeException e) {
            logger.error(e.getMessage());
        }
        return list;
    }

    public List getChannelListByMember(Member member) {
        List list = null;
        try {
            list = channelManager.getChannelList(member);
        } catch (RuntimeException e) {
            logger.error(e.getMessage());
        }
        return list;
    }

    public Channel getChannel(int id) {
        Channel result = null;
        try {
            result = channelManager.getChannel(id);
        } catch (RuntimeException e) {
            logger.error(e.getMessage());
        }
        return result;
    }

    public Channel getChannelByName(String name) {
        Channel result = null;
        try {
            result = channelManager.getChannel(name);
        } catch (RuntimeException e) {
            logger.error(e.getMessage());
        }
        return result;
    }

    public int setChannel(Channel m) {
        int result = -1;
        try {
            result = channelManager.setChannel(m);
        } catch (RuntimeException e) {
            logger.error(e.getMessage());
        }
        return result;
    }

    public void deleteChannel(int id) {
        try {
            channelManager.deleteChannel(id);
        } catch (RuntimeException e) {
            logger.error(e.getMessage());
        }
    }

    public void setChannelList(Channel[] l, int who) {
        try {
            channelManager.setChannelList(l, who);
        } catch (RuntimeException e) {
            logger.error(e.getMessage());
        }
    }
}
