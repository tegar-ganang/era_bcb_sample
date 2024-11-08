package edu.lcmi.grouppac.scgi;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;
import org.javagroups.*;
import org.javagroups.blocks.MessageDispatcher;
import org.javagroups.blocks.RequestHandler;
import org.javagroups.util.RspList;
import org.omg.CORBA.FT.Property;
import org.omg.CORBA.FT.RequestInfo;
import edu.lcmi.grouppac.util.Debug;

/**
 * Implementation of JavaGroups adaptor for SCG.
 * 
 * @version $Revision: 1.17 $
 * @author <a href="mailto:luciana@das.ufsc.br">Luciana Moreira S� de Souza</a>, <a
 *         href="http://www.das.ufsc.br/">UFSC, Florian�polis, SC, Brazil</a>
 */
public class JavaGroupsAdaptor extends AbstractSCGIAdaptor implements RequestHandler {

    protected String props = "UDP(mcast_addr=228.1.2.3;mcast_port=45566;ip_ttl=16):" + "PING:" + "MERGE2:" + "pbcast.STABLE:" + "pbcast.NAKACK:" + "UNICAST:" + "FRAG:" + "pbcast.GMS(shun=true;print_local_addr=false):" + "VIEW_ENFORCER:" + "QUEUE:";

    protected Map channels;

    protected Map dispatchers;

    protected RspList rsp_list;

    /**
	 * Creates a new JavaGroupsAdaptor object.
	 * 
	 * @param handler
	 * @param criteria
	 */
    public JavaGroupsAdaptor(RequestInfoHandler handler, Property[] criteria) {
        super(handler, criteria);
        init();
    }

    /**
	 * @see RequestHandler#handle(Message)
	 */
    public Object handle(Message msg) {
        RequestInfo info = (RequestInfo) msg.getObject();
        Debug.output(2, "JavaGroupsAdaptor: Received request: " + info.operation + " to " + info.type_id);
        return getRequestInfoHandler().receivedMessage(info);
    }

    /**
	 * @see SCGIAdaptor#joinGroup(String)
	 */
    public void joinGroup(String group_id) throws IOException {
        try {
            openChannel(group_id);
        } catch (ChannelException cc) {
            throw new IOException(cc.toString());
        } catch (ChannelClosedException cc) {
            throw new IOException(cc.toString());
        }
    }

    /**
	 * @see SCGIAdaptor#leaveGroup(String)
	 */
    public void leaveGroup(String group_id) throws IOException {
        close(group_id);
    }

    /**
	 * Closes one JavaGroups channel.
	 * 
	 * @param group_id the group channel name to be closed
	 */
    protected synchronized void close(String group_id) {
        MessageDispatcher dispatcher = (MessageDispatcher) dispatchers.remove(group_id);
        Channel channel = (Channel) channels.remove(group_id);
        if (channel != null) {
            channel.disconnect();
            channel.close();
        }
        if (dispatcher != null) dispatcher.stop();
    }

    /**
	 * Opens, if necessary, a JavaGroups channel. A channel is only opened if the group_id is new.
	 * 
	 * @param type_id
	 * @return the message dispatcher to be used with the group
	 * @throws ChannelException
	 * @throws ChannelClosedException
	 * @see MessageDispatcher#castMessage(java.util.Vector, org.javagroups.Message, int, long)
	 */
    protected synchronized MessageDispatcher openChannel(String type_id) throws ChannelException, ChannelClosedException {
        String group_id = type_id;
        if (dispatchers.get(group_id) != null) return (MessageDispatcher) dispatchers.get(group_id);
        Channel channel = new JChannel(props);
        MessageDispatcher dispatcher = new MessageDispatcher(channel, null, null, this);
        channel.connect(group_id);
        Debug.output(1, "JavaGroupsAdaptor: Channel: " + channel.getChannelName() + " Address: " + channel.getLocalAddress());
        channels.put(group_id, channel);
        dispatchers.put(group_id, dispatcher);
        return dispatcher;
    }

    /**
	 * Description
	 */
    private void init() {
        channels = new Hashtable();
        dispatchers = new Hashtable();
    }
}
