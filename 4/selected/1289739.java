package net.sf.jgcs.jgroups;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import net.sf.jgcs.AbstractDataSession;
import net.sf.jgcs.AbstractProtocol;
import net.sf.jgcs.Annotation;
import net.sf.jgcs.ClosedSessionException;
import net.sf.jgcs.GroupConfiguration;
import net.sf.jgcs.JGCSException;
import net.sf.jgcs.Message;
import net.sf.jgcs.Service;
import net.sf.jgcs.UnsupportedServiceException;
import org.jgroups.ChannelClosedException;
import org.jgroups.ChannelNotConnectedException;
import org.jgroups.JChannel;
import org.jgroups.stack.IpAddress;

public class JGroupsDataSession extends AbstractDataSession {

    private HashMap<JGroupsService, JChannel> channelsMap;

    public JGroupsDataSession(AbstractProtocol proto, JChannel channel, GroupConfiguration group) throws JGCSException {
        super(proto, group);
        channelsMap = new HashMap<JGroupsService, JChannel>();
        channelsMap.put(new JGroupsService("vsc+total"), channel);
    }

    public void close() {
    }

    public Message createMessage() throws ClosedSessionException {
        return new JGroupsMessage();
    }

    public void multicast(Message msg, Service service, Object cookie, Annotation... annotation) throws IOException, UnsupportedServiceException {
        if (!(service instanceof JGroupsService)) throw new UnsupportedServiceException("Service " + service + " is not supported.");
        JChannel channel = channelsMap.get(service);
        if (channel == null) throw new UnsupportedServiceException("There is no JGroups channel for the service " + service);
        try {
            channel.send((org.jgroups.Message) msg);
        } catch (ChannelNotConnectedException e) {
            throw new ClosedSessionException("Channel is closed.", e);
        } catch (ChannelClosedException e) {
            throw new ClosedSessionException("Channel is closed.", e);
        }
    }

    public void send(Message msg, Service service, Object cookie, SocketAddress destination, Annotation... annotation) throws IOException, UnsupportedServiceException {
        if (!(service instanceof JGroupsService)) throw new UnsupportedServiceException("Service " + service + " is not supported.");
        JChannel channel = channelsMap.get(service);
        if (channel == null) throw new UnsupportedServiceException("There is no JGroups channel for the service " + service);
        IpAddress jgroupsAddr = AddressUtils.getJGroupsAddress((InetSocketAddress) destination);
        ((org.jgroups.Message) msg).setDest(jgroupsAddr);
        try {
            channel.send((org.jgroups.Message) msg);
        } catch (ChannelNotConnectedException e) {
            throw new ClosedSessionException("Channel is closed.", e);
        } catch (ChannelClosedException e) {
            throw new ClosedSessionException("Channel is closed.", e);
        }
    }

    public void notifyExceptionListeners(JGCSException exception) {
        super.notifyExceptionListeners(exception);
    }

    public Object notifyMessageListeners(Message msg) {
        return super.notifyMessageListeners(msg);
    }

    public void notifyServiceListeners(Object context, Service service) {
        super.notifyServiceListeners(context, service);
    }

    public JChannel getChannel(Service service) {
        return channelsMap.get(service);
    }
}
