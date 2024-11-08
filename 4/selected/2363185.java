package org.charvolant.tmsnet.resources.networks;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.charvolant.tmsnet.model.ChannelInformation;
import org.charvolant.tmsnet.resources.AbstractIdentifiable;
import org.charvolant.tmsnet.resources.ResourceFactory;
import org.charvolant.tmsnet.resources.VersionedResource;
import org.charvolant.tmsnet.util.Version;
import org.charvolant.tmsnet.util.VersionAdapter;

/**
 * A description of a set of netwotks.
 *
 * @author Doug Palmer &lt;doug@charvolant.org&gt;
 *
 */
@XmlRootElement(name = "network-map")
@XmlAccessorType(XmlAccessType.NONE)
public class NetworkMap extends AbstractIdentifiable<NetworkMap> implements VersionedResource {

    /** The logger */
    private static final Logger logger = Logger.getLogger(NetworkMap.class.getName());

    /** The version of this map */
    @XmlAttribute(required = true)
    @XmlJavaTypeAdapter(VersionAdapter.class)
    private Version version;

    /** The time this map was updated. */
    @XmlAttribute
    private Date updated;

    /** The locale that this map is used for */
    @SuppressWarnings("unused")
    @XmlAttribute
    private String locale;

    /** The network map */
    @XmlElement
    @XmlJavaTypeAdapter(NetworkIdentificationAdapter.class)
    private HashMap<Integer, NetworkIdentification> networks;

    /**
   * Construct an empty network map.
   *
   */
    public NetworkMap() {
        super();
        this.networks = new HashMap<Integer, NetworkIdentification>();
    }

    /**
   * Get the version.
   *
   * @return the version
   */
    @Override
    public Version getVersion() {
        return this.version;
    }

    /**
   * Set the version.
   *
   * @param version the version to set
   */
    public void setVersion(Version version) {
        this.version = version;
    }

    /**
   * Get the updated.
   *
   * @return the updated
   */
    @Override
    public Date getUpdated() {
        return this.updated;
    }

    /**
   * Set the updated.
   *
   * @param updated the updated to set
   */
    public void setUpdated(Date updated) {
        this.updated = updated;
    }

    /**
   * Get the networks.
   * <p>
   * A single network may be mapped multiple times by different network ids.
   *
   * @return the networks
   */
    public HashMap<Integer, NetworkIdentification> getNetworks() {
        return this.networks;
    }

    /**
   * Set the networks.
   *
   * @param networks the networks to set
   */
    public void setNetworks(HashMap<Integer, NetworkIdentification> networks) {
        this.networks = networks;
    }

    /**
   * Get network information from an original network identifier.
   * <p>
   * If the network information cannot be found, a place-holder
   * 
   * @param onid The originating network identifier
   * 
   * @return The network identification for this 
   */
    public NetworkIdentification getNetwork(int onid) {
        NetworkIdentification network = this.networks.get(onid);
        if (network != null) return network;
        this.logger.fine("Creating network for onid=" + onid);
        network = new NetworkIdentification();
        network.setName(Integer.toString(onid));
        network.getNetworkIds().add(onid);
        network.setParent(this);
        this.networks.put(onid, network);
        return network;
    }

    /**
   * Get channel identification for a channel.
   * <p>
   * This will always produce a channel identification,
   * even if it is a place-holder.
   * 
   * @param info The channel information
   * 
   * @return The channel identification
   */
    public ChannelIdentification getChannelIdentification(ChannelInformation info) {
        NetworkIdentification network = this.getNetwork(info.getOriginatingNetworkId());
        ChannelIdentification channel = network.getChannel(info.getLogicalChannel());
        if (channel != null) return channel;
        return network.createChannel(info.getLogicalChannel());
    }

    /**
   * Copy any resources that we need into a save directory.
   *
   * @param factory The map factory
   * @param base The base URL for relative
   * @param copied The URIs already copied
   * @throws IOException if unable to copy
   * @throws URISyntaxException if unable to map URIs
   * @see org.charvolant.tmsnet.resources.AbstractIdentifiable#copyResources(org.charvolant.tmsnet.resources.networks.NetworkMapFactory, java.net.URL)
   */
    @Override
    public void copyResources(ResourceFactory<?> factory, URL base, Map<URI, URI> copied) throws IOException, URISyntaxException {
        super.copyResources(factory, base, copied);
        for (NetworkIdentification ni : this.networks.values()) ni.copyResources(factory, base, copied);
    }
}
