package org.fjank.jcache.distribution;

import java.util.Enumeration;
import org.fjank.jcache.CacheImpl;
import org.jgroups.Address;
import org.jgroups.ChannelClosedException;
import org.jgroups.ChannelException;
import org.jgroups.ChannelNotConnectedException;
import org.jgroups.JChannel;
import org.jgroups.Membership;
import org.jgroups.MembershipListener;
import org.jgroups.Message;
import org.jgroups.MessageListener;
import org.jgroups.View;
import org.jgroups.blocks.PullPushAdapter;

public class JGroupsDistributionEngine extends DistributionEngine implements MessageListener, MembershipListener {

    private static JGroupsDistributionEngine _singleton;

    public static synchronized JGroupsDistributionEngine instanceOf(CacheImpl cache) {
        if (_singleton == null) {
            _singleton = new JGroupsDistributionEngine(cache);
        }
        return _singleton;
    }

    private JChannel channel;

    private Membership members = new Membership();

    private JGroupsDistributionEngine(CacheImpl cache) {
        this.cache = cache;
        if (cache.isDistributed()) {
            try {
                channel = new JChannel(null);
                channel.connect("FKacheOS");
                new PullPushAdapter(channel, this, this);
            } catch (ChannelException e) {
                throw new IllegalStateException(e.getMessage());
            }
        }
    }

    public void block() {
    }

    /**
     *
     */
    @Override
    public Enumeration getCacheAddr() {
        return members.getMembers().elements();
    }

    protected JChannel getChannel() {
        return channel;
    }

    /**
	 * state not currently in use.
	 */
    public byte[] getState() {
        return new byte[0];
    }

    public void receive(Message msg) {
        if ((msg.getObject() instanceof ClusterNotification) && !msg.getSrc().equals(channel.getLocalAddress())) {
            ClusterNotification clusterNotification = (ClusterNotification) msg.getObject();
            handleClusterNotification(clusterNotification);
        }
    }

    @Override
    public void sendNotification(ClusterNotification clusterNotification) {
        if (cache.isDistributed()) {
            Message message = new Message();
            message.setObject(clusterNotification);
            try {
                channel.send(message);
            } catch (ChannelNotConnectedException e) {
                throw new IllegalStateException(e.getMessage());
            } catch (ChannelClosedException e) {
                throw new IllegalStateException(e.getMessage());
            }
        }
    }

    /**
	 * state not currently in use.
	 */
    public void setState(byte[] state) {
    }

    public void suspect(Address suspected_mbr) {
    }

    /**
	 * Is called when new members arrives or leaves the group.
	 * 
	 * @see org.jgroups.MembershipListener#viewAccepted(org.jgroups.View)
	 */
    public void viewAccepted(View new_view) {
        members.add(new_view.getMembers());
    }
}
