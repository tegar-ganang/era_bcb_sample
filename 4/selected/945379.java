package org.charvolant.tmsnet.resources.networks;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.charvolant.tmsnet.resources.AbstractIdentifiable;
import org.charvolant.tmsnet.resources.ResourceFactory;

/**
 * Information about a particular network.
 *
 * @author Doug Palmer &lt;doug@charvolant.org&gt;
 *
 */
@XmlRootElement(name = "network")
@XmlAccessorType(XmlAccessType.FIELD)
public class NetworkIdentification extends AbstractIdentifiable<AbstractIdentifiable<?>> {

    /** The logger */
    private static final Logger logger = Logger.getLogger(NetworkMap.class.getName());

    /** The original network identifier */
    @XmlElement(name = "onid")
    private int originalNetworkId;

    /** The network identifiers */
    @XmlElement(name = "nid")
    private List<Integer> networkIds;

    /** The name of the network operator */
    @XmlElement
    private String operator;

    /** The channel map */
    @XmlElement
    @XmlJavaTypeAdapter(ChannelIdentificationAdapter.class)
    private HashMap<Integer, ChannelIdentification> channels;

    /** The sub-networks */
    @XmlElementRef
    private List<NetworkIdentification> networks;

    /**
   * Construct an empty network description.
   *
   */
    public NetworkIdentification() {
        super();
        this.channels = new HashMap<Integer, ChannelIdentification>();
        this.networks = new ArrayList<NetworkIdentification>();
        this.originalNetworkId = 0xFFFF;
        this.networkIds = new ArrayList<Integer>();
    }

    /**
   * Get the original network id.
   * <p>
   * For some reason, networks can have multiple onids.
   *
   * @return the original network id
   */
    public int getOriginalNetworkId() {
        return this.originalNetworkId;
    }

    /**
   * Set the original network id.
   *
   * @param originalNetworkId the original network id to set
   */
    public void setOriginalNetworkId(int originalNetworkId) {
        this.originalNetworkId = originalNetworkId;
    }

    /**
   * Get the networkIds.
   *
   * @return the networkIds
   */
    public List<Integer> getNetworkIds() {
        return this.networkIds;
    }

    /**
   * Set the networkIds.
   *
   * @param networkIds the networkIds to set
   */
    public void setNetworkIds(List<Integer> networkIds) {
        this.networkIds = networkIds;
    }

    /**
   * Get the operator.
   * <p>
   * The operator is the name of the company that operates the named network.
   *
   * @return the operator
   */
    public String getOperator() {
        return this.operator;
    }

    /**
   * Set the operator.
   *
   * @param operator the operator to set
   */
    public void setOperator(String operator) {
        this.operator = operator;
    }

    /**
   * Get the channels.
   *
   * @return the channels
   */
    public HashMap<Integer, ChannelIdentification> getChannels() {
        return this.channels;
    }

    /**
   * Set the channels.
   *
   * @param channels the channels to set
   */
    public void setChannels(HashMap<Integer, ChannelIdentification> channels) {
        this.channels = channels;
    }

    /**
   * Get the sub-networks.
   *
   * @return the networks
   */
    public List<NetworkIdentification> getNetworks() {
        return this.networks;
    }

    /**
   * Set the sub-networks.
   *
   * @param betworks the networks to set
   */
    public void setNetworks(List<NetworkIdentification> networks) {
        this.networks = networks;
    }

    /**
   * Get a channel corresponding to a logical channel number.
   * 
   * @param lcn The logical channel number
   * 
   * @return The channel identification or null for not found
   */
    public ChannelIdentification getChannel(int lcn) {
        ChannelIdentification channel = this.channels.get(lcn);
        if (channel != null) return channel;
        for (NetworkIdentification ni : this.networks) {
            channel = ni.getChannel(lcn);
            if (channel != null) return channel;
        }
        return null;
    }

    /**
   * Create a channel corresponding to a logical channel number.
   * 
   * @param lcn The logical channel number
   * 
   * @return The channel identification
   */
    public ChannelIdentification createChannel(int lcn) {
        ChannelIdentification channel = new ChannelIdentification();
        this.logger.fine("Creating channel " + lcn + " on network " + this.getName());
        channel.setName(Integer.toString(lcn));
        channel.setLogicalChannelNumber(lcn);
        channel.setParent(this);
        this.channels.put(lcn, channel);
        return channel;
    }

    /**
   * Copy any resources that we need into a save directory.
   *
   * @param factory The map factory
   * @param base The base URL for relative
   * @param copied The URIs already copied
   * @throws IOException
   * @throws URISyntaxException if unable to map URIs
   * @see org.charvolant.tmsnet.resources.AbstractIdentifiable#copyResources(org.charvolant.tmsnet.resources.networks.NetworkMapFactory, java.net.URL)
   */
    @Override
    public void copyResources(ResourceFactory<?> factory, URL base, Map<URI, URI> copied) throws IOException, URISyntaxException {
        super.copyResources(factory, base, copied);
        for (ChannelIdentification ci : this.channels.values()) ci.copyResources(factory, base, copied);
        for (NetworkIdentification ni : this.networks) ni.copyResources(factory, base, copied);
    }

    /**
   * After unmarshalling, make sure that the parent/child relationship is OK
   * 
   * @param unmarshaller
   * @param parent
   */
    public void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
        for (NetworkIdentification ni : this.networks) ni.setParent(this);
    }
}
