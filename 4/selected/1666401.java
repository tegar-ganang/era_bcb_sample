package org.charvolant.tmsnet.model;

import java.util.Date;
import org.charvolant.properties.annotations.Property;
import org.charvolant.tmsnet.resources.ResourceLocator;
import org.charvolant.tmsnet.resources.TMSNet;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * A station description.
 * <p>
 * Stations represent individual transmissions of channels.
 *
 * @author Doug Palmer &lt;doug@charvolant.org&gt;
 *
 */
public class Station extends ResourceHolder<StationList> {

    /**
   * Construct an empty station.
   *
   */
    public Station() {
        super();
    }

    /**
   * Construct a station with a context and a resource.
   *
   * @param context The context
   * @param resource The station resource
   */
    public Station(StationList context, Resource resource) {
        super(context, resource);
    }

    /**
   * Create a station out of the channel description.
   * <p>
   * This resource
   *
   * @param context The context
   * @param desc The channel description
   * @param media The media type (TV or radio)
   * @param index The station index (from the POVof the PVR)
   * 
   * @return The created resource
   * 
   * @see org.charvolant.tmsnet.model.Description#createResource(org.charvolant.tmsnet.resources.ResourceLocator, com.hp.hpl.jena.rdf.model.Resource)
   */
    public Station(StationList context, ChannelDescription desc, Resource media, int index) {
        super(context);
        ResourceLocator locator = this.getLocator();
        Resource resource = locator.createStation(desc.getOriginatingNetworkId(), desc.getTransportStreamId(), desc.getServiceId());
        Resource channel = locator.findChannel(desc.getOriginatingNetworkId(), desc.getLogicalChannel());
        Resource transmitter = locator.findTransmitter(desc.getSatellite(), desc.isPolar());
        locator.create(resource, TMSNet.audioPid, desc.getAudioPid());
        if (desc.getDolby() == 255) resource.removeAll(TMSNet.dolby); else locator.create(resource, TMSNet.dolby, desc.getDolby());
        locator.create(resource, TMSNet.transmissionFlags, desc.getFlags());
        locator.create(resource, TMSNet.frequency, desc.getFrequency() * 100000L);
        locator.create(resource, TMSNet.parentChannel, channel);
        locator.create(resource, TMSNet.multifeed, desc.getMultifeed());
        locator.create(resource, TMSNet.label, desc.getName());
        locator.create(resource, TMSNet.originatingNetworkId, desc.getOriginatingNetworkId());
        locator.create(resource, TMSNet.pcrId, desc.getPcrId());
        locator.create(resource, TMSNet.pmtId, desc.getPmtId());
        locator.create(resource, TMSNet.transmitter, transmitter);
        locator.create(resource, TMSNet.satelliteIndex, desc.getSatelliteIndex());
        locator.create(resource, TMSNet.serviceId, desc.getServiceId());
        locator.create(resource, TMSNet.srFlag, desc.getSr());
        locator.create(resource, TMSNet.transportStreamId, desc.getTransportStreamId());
        locator.create(resource, TMSNet.teletextAvailable, desc.isTtxAvailable());
        locator.create(resource, TMSNet.tunerNumber, desc.getTunerNumber());
        locator.create(resource, TMSNet.videoPid, desc.getVideoPid());
        locator.create(resource, TMSNet.stationIndex, index);
        locator.create(resource, TMSNet.serviceType, media);
        this.setResource(resource);
    }

    /**
   * Get the channel that this station delivers.
   * 
   * @return The channel, or null for not present
   */
    public Resource getChannel() {
        return this.locateResource(TMSNet.parentChannel);
    }

    /**
   * Get the station name.
   * <p>
   * This is either the local label, or the label from the parents
   *
   * @return the station name
   */
    @Property(label = "label.name", detail = "label.name.detail.channel", group = "label.name")
    public String getName() {
        String name = this.getLabel();
        if (name != null) return name;
        name = this.locateString(TMSNet.parentChannel, TMSNet.label);
        if (name != null) return name;
        name = this.locateString(TMSNet.parentChannel, TMSNet.network, TMSNet.label);
        if (name != null) return name;
        return "";
    }

    /**
   * Get the satellite name.
   * <p>
   * DVB-T (terrestrial broadcasts) have a satellite name of <code>DVBT</code>.
   *
   * @return the satellite name
   */
    @Property(label = "label.transmitter", detail = "label.transmitter.detail", group = "label.source")
    public String getSatellite() {
        return this.locateString(TMSNet.transmitter, TMSNet.label);
    }

    /**
   * Get the flags.
   * <p>
   * Current function unkown.
   *
   * @return the flags
   */
    @Property
    public int getFlags() {
        return this.locateInt(0, TMSNet.transmissionFlags);
    }

    /**
   * Get the tunerNumber.
   * <p>
   * Current function unknown.
   *
   * @return the tunerNumber
   */
    @Property
    public int getTunerNumber() {
        return this.locateInt(0, TMSNet.tunerNumber);
    }

    /**
   * Get the polar flag.
   * <p>
   * Current function unknown.
   *
   * @return the polar flag
   */
    @Property(label = "label.polar", detail = "label.polar.detail", group = "label.source")
    public boolean isPolar() {
        return this.locateBoolean(false, TMSNet.transmitter, TMSNet.polar);
    }

    /**
   * Is TTX available?
   *
   * @return the ttx available flag
   */
    @Property(label = "label.ttxAvailable", detail = "label.ttxAvailable.detail", group = "label.streams")
    public boolean isTtxAvailable() {
        return this.locateBoolean(false, TMSNet.teletextAvailable);
    }

    /**
   * Get the channel frequency.
   *
   * @return the frequency in units of Hz
   */
    @Property(label = "label.frequency", detail = "label.frequency.detail", group = "label.source")
    public int getFrequency() {
        return this.locateInt(0, TMSNet.frequency);
    }

    /**
   * Get the sr.
   * <p>
   * Function currently unknown.
   *
   * @return the sr
   */
    @Property(label = "label.srId", detail = "label.srId", group = "label.streams")
    public int getSr() {
        return this.locateInt(0, TMSNet.srFlag);
    }

    /**
   * Get the service identifier.
   *
   * @return the service identifier
   */
    @Property(label = "label.serviceId", detail = "label.serviceId.detail", group = "label.source")
    public int getServiceId() {
        return this.locateInt(0, TMSNet.serviceId);
    }

    /**
   * Get the PMT identifier.
   *
   * @return the PMT identifier
   */
    @Property(label = "label.pmtId", detail = "label.pmtId.detail", group = "label.streams")
    public int getPmtId() {
        return this.locateInt(0, TMSNet.pmtId);
    }

    /**
   * Get the PCR identifier.
   *
   * @return the PCR identifier
   */
    @Property(label = "label.pcrId", detail = "label.pcrId.detail", group = "label.streams")
    public int getPcrId() {
        return this.locateInt(0, TMSNet.pcrId);
    }

    /**
   * Get the video PID.
   *
   * @return the video PID
   */
    @Property(label = "label.videoPid", detail = "label.videoPid.detail", group = "label.streams")
    public int getVideoPid() {
        return this.locateInt(0, TMSNet.videoPid);
    }

    /**
   * Get the audio PID.
   *
   * @return the audio PID
   */
    @Property(label = "label.audioPid", detail = "label.audioPid.detail", group = "label.streams")
    public int getAudioPid() {
        return this.locateInt(0, TMSNet.audioPid);
    }

    /**
   * Get the dolby track number.
   * <p>
   * A value of 255 means no dolby audio.
   *
   * @return the dolby track number
   */
    @Property(label = "label.dolby", detail = "label.dolby.detail", group = "label.streams")
    public int getDolby() {
        return this.locateInt(255, TMSNet.dolby);
    }

    /**
   * Get the multifeed.
   * <p>
   * Function currently unknown
   *
   * @return the multifeed
   */
    @Property
    public int getMultifeed() {
        return this.locateInt(0, TMSNet.multifeed);
    }

    /**
   * Get the satellite index.
   *
   * @return the satellite index
   */
    @Property
    public int getSatelliteIndex() {
        return this.locateInt(0, TMSNet.satelliteIndex);
    }

    /**
   * Get the originating network id.
   *
   * @return the originating network id
   */
    @Property(label = "label.originatingNetworkId", detail = "label.originatingNetworkId.detail", group = "label.source")
    public int getOriginatingNetworkId() {
        return this.locateInt(0, TMSNet.originatingNetworkId);
    }

    /**
   * Get the transport stream id.
   *
   * @return the transport stream id
   */
    @Property(label = "label.transportStreamId", detail = "label.transportStreamId.detail", group = "label.source")
    public int getTransportStreamId() {
        return this.locateInt(0, TMSNet.transportStreamId);
    }

    /**
   * Get the logical channel number.
   *
   * @return the logical channel number
   */
    @Property(label = "label.logicalChannel", detail = "label.logicalChannel.detail", group = "label.source")
    public int getLogicalChannel() {
        return this.locateInt(0, TMSNet.parentChannel, TMSNet.logicalChannelNumber);
    }

    /**
   * Get index number of this channel
   * <p>
   * Derived from the context.
   * 
   * @return The index number or -1 for not known.
   */
    public int getIndex() {
        StationList list = this.getContext();
        return list == null ? -1 : list.getStations().indexOf(this);
    }

    /**
   * Get the network resource.
   * 
   * @return The network resource, located through the channel and network links
   */
    public Resource getNetwork() {
        return this.locateResource(TMSNet.parentChannel, TMSNet.network);
    }

    /**
   * Get the event that corresponds to this time on this station.
   * 
   * @param time The event time
   * 
   * @return The corresponding event, or null for not found
   */
    public Event getEvent(Date time) {
        StationList list = this.getContext();
        int index;
        PVRState state;
        if (list == null) return null;
        index = list.getStations().indexOf(this);
        if (index < 0) return null;
        state = list.getContext();
        if (state == null) return null;
        return state.findEvent(list.getType(), index, time);
    }

    /**
   * Get the service type that this station uses.
   * 
   * @return The service type
   */
    public ServiceType getServiceType() {
        StationList context = this.getContext();
        return context == null ? null : context.getType();
    }
}
