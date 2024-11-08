package edu.lcmi.grouppac.scg;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import org.javagroups.*;
import org.javagroups.blocks.GroupRequest;
import org.javagroups.blocks.MessageDispatcher;
import org.javagroups.blocks.RequestHandler;
import org.javagroups.util.RspList;
import org.omg.CORBA.FT.Property;
import org.omg.CORBA.FT.RequestInfo;
import org.omg.CORBA.FT.ResponseInfo;
import org.omg.CORBA.FT.scg.FIRST_RESPONSE;
import org.omg.CORBA.FT.scg.MAJORITY_RESPONSE;
import edu.lcmi.grouppac.util.Debug;

/**
 * Implementation of JavaGroups adaptor for SCG.
 * 
 * @version $Revision: 1.19 $
 * @author <a href="mailto:padilha@das.ufsc.br">Ricardo Sangoi Padilha</a>, <a
 *         href="http://www.das.ufsc.br/">UFSC, Florian�polis, SC, Brazil</a>
 */
public class JavaGroupsAdaptor extends AbstractSCGAdaptor implements MembershipListener, RequestHandler {

    protected String props = "UDP(mcast_addr=228.1.2.3;mcast_port=45566;ip_ttl=16):" + "PING:" + "MERGE2:" + "pbcast.STABLE:" + "pbcast.NAKACK:" + "UNICAST:" + "FRAG:" + "pbcast.GMS(shun=true;print_local_addr=false):" + "VIEW_ENFORCER:" + "QUEUE:";

    private Map channels;

    private Map dispatchers;

    private Map results;

    private int group_size;

    /**
	 * Creates a new JavaGroupsAdaptor object.
	 */
    public JavaGroupsAdaptor() {
        super();
        init();
    }

    /**
	 * Creates a new JavaGroupsAdaptor object.
	 * 
	 * @param the_criteria
	 */
    public JavaGroupsAdaptor(Property[] the_criteria) {
        super(the_criteria);
        init();
    }

    /**
	 * @see SCGAdaptor#getResponse(RequestInfo msg)
	 */
    public ResponseInfo getResponse(RequestInfo msg) throws IOException {
        ResponseInfo ret = null;
        if (msg.reply_needed && isPendingRequest(msg)) {
            synchronized (this) {
                Vector rsplist = ((RspList) results.get(msg.message_id)).getResults();
                if (getResponsePolicy() == FIRST_RESPONSE.value) {
                    for (Iterator e = rsplist.iterator(); e.hasNext(); ) {
                        ret = (ResponseInfo) e.next();
                        if (ret != null) break;
                    }
                } else if (getResponsePolicy() == MAJORITY_RESPONSE.value) {
                    ret = chooseByAverage((ResponseInfo[]) rsplist.toArray());
                }
                removePendingRequest(msg);
                results.remove(msg.message_id);
            }
        }
        return ret;
    }

    /**
	 * @see SCGAdaptor#close()
	 */
    public void close() {
        for (Iterator e = dispatchers.keySet().iterator(); e.hasNext(); ) {
            Object key = e.next();
            close((String) key);
        }
    }

    /**
	 * @see RequestHandler#handle(Message)
	 */
    public Object handle(Message msg) {
        return null;
    }

    /**
	 * @see SCGAdaptor#sendRequest(RequestInfo msg)
	 */
    public void sendRequest(RequestInfo msg) throws IOException {
        if (isPendingRequest(msg)) return;
        try {
            MessageDispatcher dispatcher = openChannel(msg.type_id);
            Debug.output(2, "JavaGroupsAdaptor: Number of adaptors in group " + msg.type_id + " : " + group_size);
            Message m = new Message(null, null, msg);
            synchronized (this) {
                Debug.output(2, "JavaGroupsAdaptor: Send request: " + msg.operation + " to " + msg.type_id);
                RspList rsp = dispatcher.castMessage(null, m, GroupRequest.GET_ALL, 0);
                if (msg.reply_needed) {
                    addPendingRequest(msg);
                    results.put(msg.message_id, rsp);
                }
            }
        } catch (ChannelException e) {
            throw new IOException(e.getMessage());
        } catch (ChannelClosedException e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
	 * @see SCGAdaptor#startup(String)
	 */
    public void startup(String group_id) {
        try {
            openChannel(group_id);
        } catch (ChannelException e) {
        } catch (ChannelClosedException e) {
        }
    }

    /**
	 * @see MembershipListener#block()
	 */
    public void block() {
    }

    /**
	 * @see MembershipListener#suspect(Address)
	 */
    public void suspect(Address suspected_mbr) {
    }

    /**
	 * @see MembershipListener#viewAccepted(View)
	 */
    public void viewAccepted(View new_view) {
        group_size = new_view.getMembers().size();
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
        channel.setOpt(Channel.LOCAL, new Boolean(false));
        MessageDispatcher dispatcher = new MessageDispatcher(channel, null, this, this);
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
        results = new Hashtable();
    }
}
