package com.tirsen.hanoi.engine;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.util.*;

/**
 * 
 * 
 * <!-- $Id: AbstractConnector.java,v 1.4 2002/08/18 17:22:42 tirsen Exp $ -->
 * <!-- $Author: tirsen $ -->
 *
 * @author Jon Tirs&eacute;n (tirsen@users.sourceforge.net)
 * @version $Revision: 1.4 $
 *
 * @deprecated the connector abstraction has been replaced with a more general resource-registry.
 */
public abstract class AbstractConnector implements Connector {

    private static final Log logger = LogFactory.getLog(AbstractConnector.class);

    private Engine engine;

    private String name;

    private List channels = new LinkedList();

    private List instanceIDs = new LinkedList();

    private List channelNames = new LinkedList();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void init(Engine engine) {
        this.engine = engine;
    }

    public boolean maybeConnect(Channel channel) {
        if (canConnect(channel)) {
            connect(channel);
            return true;
        } else {
            return false;
        }
    }

    protected boolean canConnect(Channel channel) {
        return channel.getClass() == getChannelClass();
    }

    public void connect(Channel channel) {
        if (!canConnect(channel)) {
            throw new IllegalArgumentException("Can not connect this connector to channel: " + channel);
        }
        logger.info("connecting " + this + " to channel " + channel);
        instanceIDs.add(channel.getProcessInstance().getId());
        channelNames.add(channel.getName());
        channels.add(channel);
    }

    public void processRequests() {
        if (engine != null) {
            Iterator instanceIterator = instanceIDs.iterator();
            Iterator channelIterator = channelNames.iterator();
            while (instanceIterator.hasNext()) {
                String instanceID = (String) instanceIterator.next();
                String channelName = (String) channelIterator.next();
                Queue incoming = engine.getToConnectorsQueue();
                Queue outgoing = engine.getToChannelsQueue();
                Object request;
                while ((request = incoming.dequeue(instanceID, channelName)) != null) {
                    Object response = processRequest(request);
                    outgoing.enqueue(instanceID, channelName, response);
                }
            }
        } else {
            for (Iterator iterator = channels.iterator(); iterator.hasNext(); ) {
                Channel channel = (Channel) iterator.next();
                ProcessInstance instance = channel.getProcessInstance();
                Queue outgoing = instance.getOutgoingQueue();
                Object request = (Object) outgoing.dequeue(instance.getId(), channel.getName());
                Object response = processRequest(request);
                if (request != null) {
                    instance.getIncomingQueue().enqueue(instance.getId(), channel.getName(), response);
                }
            }
        }
    }

    protected abstract Class getChannelClass();

    protected abstract Object processRequest(Object request);
}
