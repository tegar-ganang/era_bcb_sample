package org.charvolant.tmsnet.model;

import java.net.URI;
import java.util.Date;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.charvolant.properties.annotations.DisplayHint;
import org.charvolant.properties.annotations.Property;
import org.charvolant.tmsnet.protocol.TMSNetField;
import org.charvolant.tmsnet.protocol.TMSNetType;
import org.charvolant.tmsnet.protocol.TMSNetElement;
import org.charvolant.tmsnet.resources.networks.ChannelIdentification;

/**
 * A description of a TV or radio channel.
 * <p>
 * This object is derived from the TAP_TapChInfo structure in the
 * Topfield TAP SDK.
 * <p>
 * Some explanation of the teminology
 * <ul>
 * <li><em>Transport Stream</em> Essentially a single tunable channel, possibly
 * multiplexing multiple programs.
 * 
 * <li><em>Service</em> A single program (representing a bundle of video, audio, etc.
 * as required) within a transport stream.
 * 
 * <li><em>Packet Identifier</em> (PID) Each type of information has an attached
 * packet identifier, indicating what sort of stream or data it carries.
 * 
 * <li><em>Program Map Table</em> (PMT) Information embedded in the transport stream.
 * A transport stream may contain several programs. The PMT maps an individual program
 * onto the video, audio and data streams that make up the program.
 * 
 * <li><em>Program Clock Reference</em> (PCR) An embedded clock signal that can be
 * used to align the various streams.
 * 
 * <li><em>Original Network ID</em> A unique identifier for the transmitting network.
 * A network consists of multiple channels.
 * 
 * <li><em>Transport Stream ID</em> A unique identifier for a transport stream
 * within a single network.
 * 
 * <li><em>Service ID</em> A unique identifer for a service within a transport stream. 
 * A service can be uniquely referenced through original network ID/transport stream ID/service ID
 * 
 * <li>Logical Channel Number</em> A consistent channel number assigned to a service
 * that remains constant when a network uses different service IDs to broadcast the same
 * program.
 * 
 * <li><em>Teletext<em> (TTX) A teletext stream of supporting text.
 * </ul>
 * 
 *
 * @author Doug Palmer &lt;doug@charvolant.org&gt;
 *
 */
@XmlRootElement()
@XmlAccessorType(XmlAccessType.FIELD)
@TMSNetElement(type = TMSNetType.RECORD)
public class ChannelInformation extends ContextHolder<ChannelList> {

    /** The satellite name. Set to DVBT for DVB-T broadcasts */
    @XmlElement
    @TMSNetField(order = 0)
    @Property(label = "label.satellite", detail = "label.satellite.detail", group = "label.source")
    private String satellite;

    /** The channel name */
    @XmlElement
    @TMSNetField(order = 1)
    @Property(label = "label.name", detail = "label.name.detail.channel", group = "label.name")
    private String name;

    /** Flags. Function currently unknown */
    @XmlAttribute
    @TMSNetField(order = 2)
    @Property
    private int flags;

    /** Tuner number. Function currently unknown */
    @XmlAttribute
    @TMSNetField(order = 3)
    @Property
    private int tunerNumber;

    /** Polar flag. Function currently unknown */
    @XmlAttribute
    @TMSNetField(order = 4)
    @Property(label = "label.polar", detail = "label.polar.detail", group = "label.source")
    private boolean polar;

    /** TTX flag. Indicates whether Teletext is available */
    @XmlAttribute(name = "ttx")
    @TMSNetField(order = 5)
    @Property(label = "label.ttxAvailable", detail = "label.ttxAvailable.detail", group = "label.streams")
    private boolean ttxAvailable;

    /** Frequency in 0.1MHz increments */
    @XmlElement
    @TMSNetField(order = 6)
    @Property(label = "label.frequency", detail = "label.frequency.detail", group = "label.source")
    private int frequency;

    /** Sr flag. Function currently unknown */
    @XmlAttribute
    @TMSNetField(order = 7)
    @Property(label = "label.srId", detail = "label.srId", group = "label.streams")
    private int sr;

    /** Service identifier */
    @XmlElement(name = "sid")
    @TMSNetField(order = 8)
    @Property(label = "label.serviceId", detail = "label.serviceId.detail", group = "label.source")
    private int serviceId;

    /** Program map table */
    @XmlAttribute
    @TMSNetField(order = 9)
    @Property(label = "label.pmtId", detail = "label.pmtId.detail", group = "label.streams")
    private int pmtId;

    /** Program clock reference */
    @XmlAttribute
    @TMSNetField(order = 10)
    @Property(label = "label.pcrId", detail = "label.pcrId.detail", group = "label.streams")
    private int pcrId;

    /** The video stream */
    @XmlAttribute
    @TMSNetField(order = 11)
    @Property(label = "label.videoPid", detail = "label.videoPid.detail", group = "label.streams")
    private int videoPid;

    /** The audio stream */
    @XmlAttribute
    @TMSNetField(order = 12)
    @Property(label = "label.audioPid", detail = "label.audioPid.detail", group = "label.streams")
    private int audioPid;

    /** The dolby track (255 for none) */
    @XmlAttribute
    @TMSNetField(order = 13)
    @Property(label = "label.dolby", detail = "label.dolby.detail", group = "label.streams")
    private int dolby;

    /** Multifeed. Function currently unknown */
    @XmlAttribute
    @TMSNetField(order = 14)
    @Property
    private int multifeed;

    /** The satellite index. Function currently unknown; potentially the index of the stream from a multi-stream satellite */
    @XmlAttribute
    @TMSNetField(order = 15)
    @Property
    private int satelliteIndex;

    /** The originating network identifier */
    @XmlElement(name = "onid")
    @TMSNetField(order = 16)
    @Property(label = "label.originatingNetworkId", detail = "label.originatingNetworkId.detail", group = "label.source")
    private int originatingNetworkId;

    /** The transport stream identifier */
    @XmlElement(name = "tsid")
    @TMSNetField(order = 17)
    @Property(label = "label.transportStreamId", detail = "label.transportStreamId.detail", group = "label.source")
    private int transportStreamId;

    /** The logical channel number */
    @XmlElement(name = "logical-channel")
    @TMSNetField(order = 18)
    @Property(label = "label.logicalChannel", detail = "label.logicalChannel.detail", group = "label.source")
    private int logicalChannel;

    /**
   * Construct an empty channel information.
   *
   */
    public ChannelInformation() {
        super();
    }

    /**
   * Get the satellite name.
   * <p>
   * DVB-T (terrestrial broadcasts) have a satellite name of <code>DVBT</code>.
   *
   * @return the satellite name
   */
    public String getSatellite() {
        return this.satellite;
    }

    /**
   * Set the satellite name.
   *
   * @param satellite the satellite name to set
   */
    public void setSatellite(String satellite) {
        this.satellite = satellite;
    }

    /**
   * Get the channel name.
   *
   * @return the channel name
   */
    public String getName() {
        return this.name;
    }

    /**
   * Set the channel name.
   *
   * @param name the channel name to set
   */
    public void setName(String name) {
        this.name = name;
    }

    /**
   * Get the flags.
   * <p>
   * Current function unkown.
   *
   * @return the flags
   */
    public int getFlags() {
        return this.flags;
    }

    /**
   * Set the flags.
   *
   * @param flags the flags to set
   */
    public void setFlags(int flags) {
        this.flags = flags;
    }

    /**
   * Get the tunerNumber.
   * <p>
   * Current function unknown.
   *
   * @return the tunerNumber
   */
    public int getTunerNumber() {
        return this.tunerNumber;
    }

    /**
   * Set the tunerNumber.
   *
   * @param tunerNumber the tunerNumber to set
   */
    public void setTunerNumber(int tunerNumber) {
        this.tunerNumber = tunerNumber;
    }

    /**
   * Get the polar flag.
   * <p>
   * Current function unknown.
   *
   * @return the polar flag
   */
    public boolean isPolar() {
        return this.polar;
    }

    /**
   * Set the polar flag.
   *
   * @param polar the polar flag to set
   */
    public void setPolar(boolean polar) {
        this.polar = polar;
    }

    /**
   * Is TTX available?
   *
   * @return the ttx available flag
   */
    public boolean isTtxAvailable() {
        return this.ttxAvailable;
    }

    /**
   * Set the TTX available flag.
   *
   * @param ttxAvailable the TTX available flag to set
   */
    public void setTtxAvailable(boolean ttxAvailable) {
        this.ttxAvailable = ttxAvailable;
    }

    /**
   * Get the channel frequency.
   *
   * @return the frequency in units of 0.1MHz
   */
    public int getFrequency() {
        return this.frequency;
    }

    /**
   * Set the channel frequency.
   *
   * @param frequency the frequency to set
   */
    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }

    /**
   * Get the sr.
   * <p>
   * Function currently unknown.
   *
   * @return the sr
   */
    public int getSr() {
        return this.sr;
    }

    /**
   * Set the sr.
   *
   * @param sr the sr to set
   */
    public void setSr(int sr) {
        this.sr = sr;
    }

    /**
   * Get the service identifier.
   *
   * @return the service identifier
   */
    public int getServiceId() {
        return this.serviceId;
    }

    /**
   * Set the service identifier.
   *
   * @param serviceId the service identifier to set
   */
    public void setServiceId(int serviceId) {
        this.serviceId = serviceId;
    }

    /**
   * Get the PMT identifier.
   *
   * @return the PMT identifier
   */
    public int getPmtId() {
        return this.pmtId;
    }

    /**
   * Set the PMT identifier.
   *
   * @param pmtId the pmt identifier to set
   */
    public void setPmtId(int pmtId) {
        this.pmtId = pmtId;
    }

    /**
   * Get the PCR identifier.
   *
   * @return the PCR identifier
   */
    public int getPcrId() {
        return this.pcrId;
    }

    /**
   * Set the PCR identifier
   *
   * @param pcrId the PCR identifier to set
   */
    public void setPcrId(int pcrId) {
        this.pcrId = pcrId;
    }

    /**
   * Get the video PID.
   *
   * @return the video PID
   */
    public int getVideoPid() {
        return this.videoPid;
    }

    /**
   * Set the video PID.
   *
   * @param videoPid the video PID to set
   */
    public void setVideoPid(int videoPid) {
        this.videoPid = videoPid;
    }

    /**
   * Get the audio PID.
   *
   * @return the audio PID
   */
    public int getAudioPid() {
        return this.audioPid;
    }

    /**
   * Set the audio PID.
   *
   * @param audioPid the audio PID to set
   */
    public void setAudioPid(int audioPid) {
        this.audioPid = audioPid;
    }

    /**
   * Get the dolby track number.
   * <p>
   * A value of 255 means no dolby audio.
   *
   * @return the dolby track number
   */
    public int getDolby() {
        return this.dolby;
    }

    /**
   * Set the dolby track number.
   *
   * @param dolby the dolby track number to set
   */
    public void setDolby(int dolby) {
        this.dolby = dolby;
    }

    /**
   * Get the multifeed.
   * <p>
   * Function currently unknown
   *
   * @return the multifeed
   */
    public int getMultifeed() {
        return this.multifeed;
    }

    /**
   * Set the multifeed.
   *
   * @param multifeed the multifeed to set
   */
    public void setMultifeed(int multifeed) {
        this.multifeed = multifeed;
    }

    /**
   * Get the satellite index.
   *
   * @return the satellite index
   */
    public int getSatelliteIndex() {
        return this.satelliteIndex;
    }

    /**
   * Set the satellite index.
   *
   * @param satelliteIndex the satellite index to set
   */
    public void setSatelliteIndex(int satelliteIndex) {
        this.satelliteIndex = satelliteIndex;
    }

    /**
   * Get the originating network id.
   *
   * @return the originating network id
   */
    public int getOriginatingNetworkId() {
        return this.originatingNetworkId;
    }

    /**
   * Set the originating network id.
   *
   * @param originatingNetworkId the originating network id to set
   */
    public void setOriginatingNetworkId(int originatingNetworkId) {
        this.originatingNetworkId = originatingNetworkId;
    }

    /**
   * Get the transport stream id.
   *
   * @return the transport stream id
   */
    public int getTransportStreamId() {
        return this.transportStreamId;
    }

    /**
   * Set the transport stream id.
   *
   * @param transportStreamId the transport stream id to set
   */
    public void setTransportStreamId(int transportStreamId) {
        this.transportStreamId = transportStreamId;
    }

    /**
   * Get the logical channel number.
   *
   * @return the logical channel number
   */
    public int getLogicalChannel() {
        return this.logicalChannel;
    }

    /**
   * Set the logical channel number.
   *
   * @param logicalChannel the logical channel to set
   */
    public void setLogicalChannel(int logicalChannel) {
        this.logicalChannel = logicalChannel;
    }

    /**
   * Get the channel identification information.
   *
   * @return the identification
   */
    public ChannelIdentification getIdentification() {
        ChannelList list = this.getContext();
        PVRState state;
        if (list == null) return null;
        state = list.getContext();
        if (state == null) return null;
        return state.getNetworkMap().getChannelIdentification(this);
    }

    /**
   * Get the URI of the large icon associated with channel
   * 
   * @return The large icon
   */
    @Property(display = DisplayHint.ICON, group = "label.name")
    public URI getLargeIcon() {
        ChannelIdentification identification = this.getIdentification();
        return identification == null ? null : identification.getLargeIcon();
    }

    /**
   * Get the URI of the small icon associated with channel
   * 
   * @return The small icon
   */
    @Property(display = DisplayHint.ICON, group = "label.name")
    public URI getSmallIcon() {
        ChannelIdentification identification = this.getIdentification();
        return identification == null ? null : identification.getSmallIcon();
    }

    /**
   * Get the service type of this channel.
   * <p>
   * Derived from the context.
   * 
   * @return The service type or null for not known.
   */
    public ChannelType getServiceType() {
        ChannelList list = this.getContext();
        return list == null ? null : list.getType();
    }

    /**
   * Get index number of this channel
   * <p>
   * Derived from the context.
   * 
   * @return The index number or -1 for not known.
   */
    public int getIndex() {
        ChannelList list = this.getContext();
        return list == null ? -1 : list.getChannels().indexOf(this);
    }

    /**
   * Get the event that corresponds to this time on this channel.
   * 
   * @param time The event time
   * 
   * @return The corresponding event, or null for not found
   */
    public Event getEvent(Date time) {
        ChannelList list = this.getContext();
        int index;
        PVRState state;
        if (list == null) return null;
        index = list.getChannels().indexOf(this);
        if (index < 0) return null;
        state = list.getContext();
        if (state == null) return null;
        return state.findEvent(list.getType(), index, time);
    }
}
