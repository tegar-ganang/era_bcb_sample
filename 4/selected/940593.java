package org.coos.messaging;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import org.coos.messaging.impl.DefaultChannel;
import org.coos.messaging.impl.DefaultProcessor;
import org.coos.messaging.util.Log;
import org.coos.messaging.util.LogFactory;
import org.coos.messaging.util.UuidGenerator;

/**
 * @author Knut Eilif Husa, Tellu AS A link defines the processing used on a
 *         message
 */
public class Link extends DefaultProcessor implements ChannelProcessor {

    public static final String DEFAULT_QOS_CLASS = "defaultQos";

    public static final String ALIASES = "aliases";

    private static UuidGenerator linkUuid = new UuidGenerator("link");

    private String destinationUuid;

    private Vector aliases = new Vector();

    private Vector filterProcessors = new Vector();

    private Processor linkProcessor;

    private Hashtable costMap = new Hashtable();

    private Hashtable origCostMap;

    private Channel channel;

    private String linkId;

    private boolean inLink = true;

    protected Log log = LogFactory.getLog(this.getClass().getName());

    public Link() {
        linkId = String.valueOf(linkUuid.generateId());
        costMap.put(DEFAULT_QOS_CLASS, new Integer(0));
    }

    public Link(Channel channel) {
        this();
        costMap.put(DEFAULT_QOS_CLASS, new Integer(0));
        this.channel = channel;
    }

    public Link(int cost) {
        this();
        costMap.put(DEFAULT_QOS_CLASS, new Integer(cost));
    }

    public Link(String uuid, int cost) {
        this();
        this.destinationUuid = uuid;
        costMap.put(DEFAULT_QOS_CLASS, new Integer(cost));
    }

    public void setInLink(boolean inLink) {
        this.inLink = inLink;
    }

    public boolean isInLink() {
        return inLink;
    }

    public void setOutLink(boolean outLink) {
        this.inLink = !outLink;
    }

    public boolean isOutLink() {
        return !inLink;
    }

    /**
     * Add linkProcessor that act as filter, i.e. a linkProcessor that can
     * inspect the message and pass it on to the next linkProcessor Pattern is
     * pipes and filters
     *
     * @param processor
     *            the filter linkProcessor to be added
     */
    public void addFilterProcessor(Processor processor) {
        if (processor != null) {
            filterProcessors.addElement(processor);
        }
    }

    /**
     * remove a filter linkProcessor
     *
     * @param processor
     *            the filter linkProcessor to be removed
     */
    public void removeFilterProcessor(Processor processor) {
        filterProcessors.removeElement(processor);
    }

    /**
     * remove a filter linkProcessor
     *
     * @param processor
     *            the filter linkProcessor to be removed
     */
    public Vector getFilterProcessors() {
        return filterProcessors;
    }

    /**
     * Get the linkProcessor that the link ends into,
     *
     * @return the link end linkProcessor
     */
    public Processor getLinkedProcessor() {
        return linkProcessor;
    }

    /**
     * Set the linkProcessor that the link ends into,
     *
     * @param processor
     *            the link end linkProcessor
     */
    public void setChainedProcessor(Processor processor) {
        this.linkProcessor = processor;
    }

    public void addAlias(String alias) {
        synchronized (aliases) {
            if (!aliases.contains(alias)) {
                aliases.addElement(alias);
            }
        }
    }

    /**
     * Remove alias
     *
     * @param alias
     */
    public void removeAlias(String alias) {
        synchronized (aliases) {
            aliases.removeElement(alias);
        }
    }

    /**
     * Remove all aliases
     */
    public void removeAllAliases() {
        synchronized (aliases) {
            aliases.removeAllElements();
        }
    }

    /**
     * Get the aliases
     *
     * @return Vector containing all aliases
     */
    public Vector getAlises() {
        return aliases;
    }

    /**
     * Returns the unique link id
     *
     * @return the link id
     */
    public String getLinkId() {
        return linkId;
    }

    /**
     * Returns the Channel this Link belongs to
     *
     * @return the Channel
     */
    public Channel getChannel() {
        return channel;
    }

    /**
     * Sets the owning channel of this Link
     */
    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    /**
     * Processing of the message
     *
     * @param msg
     */
    public void processMessage(Message msg) throws ProcessorException {
        if (channel != null) {
            msg.getMessageContext().setCurrentChannel(channel);
        }
        msg.getMessageContext().setCurrentLink(this);
        if (inLink) {
            if (channel != null) {
                msg.getMessageContext().setInBoundChannel(channel);
                if (!channel.isConnected()) {
                    log.debug("Cannot process message in inLink since channel is not connected, Msg:" + msg);
                    throw new ProcessorException("Cannot process message in InLink since Channel :" + getName() + " is not connected");
                }
            }
            msg.getMessageContext().setInBoundLink(this);
        } else {
            if (channel != null) {
                msg.getMessageContext().setOutBoundChannel(channel);
                if (!channel.isConnected()) {
                    log.debug("Cannot process message in OutLink since channel is not connected, Msg:" + msg);
                    throw new ProcessorException("Cannot process message in OutLink since Channel :" + getName() + " is not connected");
                }
            }
            msg.getMessageContext().setOutBoundLink(this);
        }
        for (int i = 0; i < filterProcessors.size(); i++) {
            Processor processor = (Processor) filterProcessors.elementAt(i);
            try {
                processor.processMessage(msg);
            } catch (ProcessorInterruptException e) {
                log.debug("Interrupted processing of message:" + e.getMessage());
                return;
            }
        }
        linkProcessor.processMessage(msg);
    }

    /**
     * Starting any linkProcessor that is a service
     *
     * @throws Exception
     */
    public void start() throws Exception {
        for (int i = 0; i < filterProcessors.size(); i++) {
            Processor processor = (Processor) filterProcessors.elementAt(i);
            if (processor instanceof Service) {
                ((Service) processor).start();
            }
        }
        if (origCostMap == null) {
            origCostMap = new Hashtable();
            Enumeration enumer = costMap.keys();
            while (enumer.hasMoreElements()) {
                String key = (String) enumer.nextElement();
                origCostMap.put(key, costMap.get(key));
            }
        } else {
            costMap = new Hashtable();
            Enumeration enumer = origCostMap.keys();
            while (enumer.hasMoreElements()) {
                String key = (String) enumer.nextElement();
                costMap.put(key, origCostMap.get(key));
            }
        }
    }

    /**
     * stopping any linkProcessor that is a service
     *
     * @throws Exception
     */
    public void stop() throws Exception {
        for (int i = 0; i < filterProcessors.size(); i++) {
            Processor processor = (Processor) filterProcessors.elementAt(i);
            if (processor instanceof Service) {
                ((Service) processor).stop();
            }
        }
    }

    /**
     * Gets the default (if present) cost of a link
     *
     * @return the cost
     */
    public int getCost() {
        Integer integer = (Integer) costMap.get(DEFAULT_QOS_CLASS);
        if (integer != null) {
            return integer.intValue();
        } else {
            return 0;
        }
    }

    /**
     * Gets the link cost of a link based on a qos class
     *
     * @param qosClass
     *            the qosClass
     * @return the cost
     */
    public int getCost(String qosClass) {
        Integer integer = (Integer) costMap.get(qosClass);
        if (integer != null) {
            return integer.intValue();
        } else {
            return 0;
        }
    }

    /**
     * Sets the default cost
     *
     * @param cost
     *            the cost
     */
    public void setCost(int cost) {
        costMap.put(DEFAULT_QOS_CLASS, new Integer(cost));
    }

    /**
     * Gets the cost based on qosClass
     *
     * @param qosClass
     *            the qosClass
     * @param cost
     *            the cost
     */
    public void setCost(String qosClass, int cost) {
        costMap.put(qosClass, new Integer(cost));
    }

    /**
     * Returns the CostMap of this link
     *
     * @return the costMap
     */
    public Hashtable getCostMap() {
        return costMap;
    }

    /**
     * Returns the destination UUID, i.e. the destination of this Link
     *
     * @return the destination UUID
     */
    public String getDestinationUuid() {
        return destinationUuid;
    }

    /**
     * Sets the destination UUID, i.e. the destination of this Link
     *
     * @param destinationUuid
     */
    public void setDestinationUuid(String destinationUuid) {
        this.destinationUuid = destinationUuid;
    }

    public String toString() {
        if (linkProcessor != null) {
            return "Link to " + linkProcessor.toString() + ", destUUID: " + destinationUuid + ", aliases:" + aliases + ",linkId:" + linkId;
        }
        return "No link processor";
    }

    /**
     * Returns a copy of the Link
     *
     * @return the Link
     */
    public Processor copy() {
        Link copy = new Link();
        for (int i = 0; i < filterProcessors.size(); i++) {
            Processor processor = (Processor) filterProcessors.elementAt(i);
            copy.addFilterProcessor(processor.copy());
        }
        Enumeration enumer = costMap.keys();
        while (enumer.hasMoreElements()) {
            String key = (String) enumer.nextElement();
            copy.setCost(new String(key), ((Integer) costMap.get(key)).intValue());
        }
        return copy;
    }

    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((linkId == null) ? 0 : linkId.hashCode());
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Link other = (Link) obj;
        if (linkId == null) {
            if (other.linkId != null) return false;
        } else if (!linkId.equals(other.linkId)) return false;
        return true;
    }
}
