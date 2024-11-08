package net.videgro.oma.managers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import net.videgro.oma.domain.Channel;
import net.videgro.oma.domain.Member;
import net.videgro.oma.domain.MemberPermissions;
import net.videgro.oma.persistence.IChannelManagerDao;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@SuppressWarnings("serial")
public class ChannelManager implements Serializable {

    protected final Log logger = LogFactory.getLog(getClass());

    private IChannelManagerDao cmd;

    private AuthenticationManager authenticationManager;

    public void setChannelManagerDao(IChannelManagerDao mmd) {
        this.cmd = mmd;
    }

    public void setAuthenticationManager(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    public Channel getChannel(int id) {
        Channel channel = cmd.getChannel(id);
        return channel;
    }

    public Channel getChannel(String name) {
        Channel channel = cmd.getChannel(name);
        if (channel == null) {
            channel = new Channel(name);
            cmd.setChannel(channel);
        }
        return channel;
    }

    public ArrayList<Channel> getChannelList(boolean replaceSpaces) {
        ArrayList<Channel> channels = cmd.getChannelList();
        if (replaceSpaces) {
            ArrayList<Channel> result = new ArrayList<Channel>();
            Iterator<Channel> iter = channels.iterator();
            while (iter.hasNext()) {
                Channel channel = iter.next();
                channel.setName(channel.getName().replace(" ", "_"));
                result.add(channel);
            }
            return result;
        } else {
            return channels;
        }
    }

    public ArrayList<Channel> getChannelList(Member member) {
        ArrayList<Channel> channels = cmd.getChannelList(member);
        return channels;
    }

    public int setChannel(Channel m) {
        int id = -1;
        id = cmd.setChannel(m);
        return id;
    }

    public void deleteChannel(int id) {
        cmd.deleteChannel(id);
    }

    public void deleteChannel(Channel channel) {
        cmd.deleteChannel(channel);
    }

    public void setChannelList(Channel[] l, int who) {
        logger.debug("setChannelList");
        if (authenticationManager.isInGroup(who, MemberPermissions.GROUP_ADMINISTRATOR)) {
            for (Channel channel : l) {
                cmd.setChannel(channel);
            }
        }
    }
}
