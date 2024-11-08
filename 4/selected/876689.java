package org.charvolant.tmsnet.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;
import org.charvolant.properties.annotations.Property;
import org.charvolant.tmsnet.AbstractModel;
import org.charvolant.tmsnet.resources.ResourceLocatable;
import org.charvolant.tmsnet.resources.ResourceLocator;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * A snapshot of the state of the PVR.
 *
 * @author Doug Palmer &lt;doug@charvolant.org&gt;
 *
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class PVRState extends AbstractModel implements ResourceLocatable {

    /** The name of the PVR; this is usually the host name */
    @XmlID
    @Property
    private String name;

    /** The time of the last update */
    @XmlAttribute
    @Property
    private Date lastUpdate;

    /** The system information */
    @XmlElementRef
    @Property
    private SystemInformation sysInfo;

    /** The station list */
    @XmlElementRef
    @Property
    private StationList[] stations;

    /** A list of events, partitioned by channel number and then sorted by time */
    @XmlElementRef
    @Property
    private List<Channel> epg;

    /** The timer list */
    @XmlElementRef
    @Property
    private List<Timer> timers;

    /** The list of active recordings */
    @XmlElementRef
    @Property
    private List<Timer> recordings;

    /** The current channel */
    @XmlElement
    @Property
    private ChannelSelection currentChannel;

    /** The file directory root */
    @XmlElement
    @Property
    private DirectoryRoot root;

    /** The currently playing item */
    private Playback playback;

    /** The resources model */
    private ResourceLocator locator;

    /**
   * Construct an empty PVRState.
   */
    public PVRState() {
        super();
        this.name = "unknown";
        this.lastUpdate = new Date(0);
        this.stations = new StationList[ServiceType.values().length];
        this.epg = new ArrayList<Channel>(32);
        this.timers = new ArrayList<Timer>(64);
        this.recordings = new ArrayList<Timer>(4);
        this.currentChannel = new ChannelSelection();
        this.root = new DirectoryRoot(this);
        this.locator = null;
    }

    /**
   * Get the name.
   *
   * @return the name
   */
    public String getName() {
        return this.name;
    }

    /**
   * Set the name.
   *
   * @param name the name to set
   */
    public void setName(String name) {
        String old = this.name;
        this.name = name;
        this.firePropertyChange("name", old, this.name);
    }

    /**
   * Get the last update time.
   *
   * @return the last update time
   */
    public Date getLastUpdate() {
        return this.lastUpdate;
    }

    /**
   * Get the system information.
   *
   * @return the system information
   */
    public SystemInformation getSysInfo() {
        return this.sysInfo;
    }

    /**
   * Set the system information.
   *
   * @param sysDesc the system description
   */
    public void setSysInfo(SystemDescription sysDesc) {
        SystemInformation old = this.sysInfo;
        this.sysInfo = new SystemInformation(this, sysDesc);
        this.firePropertyChange("sysInfo", old, this.sysInfo);
    }

    /**
   * Get the station list for a particular type.
   *
   * @return the stations
   */
    public StationList getStations(ServiceType type) {
        return type == null ? null : this.stations[type.ordinal()];
    }

    /**
   * Set the station list with some new channels.
   * <p>
   * The station is assumed to be complete.
   * 
   * @param type The channel type
   * @param channels The new channels.
   */
    public void setStations(ServiceType type, List<ChannelDescription> channels) {
        List<Station> sl = new ArrayList<Station>(channels.size());
        StationList list = this.stations[type.ordinal()];
        Resource media = this.locator.findEnum(type);
        int index = 0;
        if (list == null) {
            list = new StationList(this, type);
            this.stations[type.ordinal()] = list;
        }
        for (ChannelDescription channel : channels) if (channel != null) sl.add(new Station(list, channel, media, index++));
        list.setStations(sl);
        this.firePropertyChange("stations", null, this.stations);
    }

    /**
   * Get a station by type and index.
   * 
   * @param type The channel type
   * @param channel The channel index
   *
   * @return the channel or null for not found
   */
    public Station getStation(ServiceType type, int channel) {
        StationList channels = this.getStations(type);
        if (channels == null) return null;
        return channels.getStation(channel);
    }

    /**
   * Find a station that matches a resource.
   * 
   * @param resource The resource
   * 
   * @return The matching station or null for not found
   */
    public Station getStation(Resource resource) {
        String uri = resource.getURI();
        for (StationList sl : this.stations) {
            if (sl == null) continue;
            for (Station station : sl.getStations()) if (station.getResource().getURI().equals(uri)) return station;
        }
        return null;
    }

    /**
   * Find a channel by service id
   * <p>
   * This doesn't match the onid/sid is unique model.
   * But it should work for most areas.
   * 
   * @param sid The service identifier
   * 
   * @return The channel information or null for not found
   */
    public Station findStation(int sid) {
        Station station;
        for (StationList sl : this.stations) {
            if (sl == null) continue;
            if ((station = sl.findStation(sid)) != null) return station;
        }
        return null;
    }

    /**
   * Get the epg list.
   *
   * @return the events
   */
    public List<Channel> getEPG() {
        return this.epg;
    }

    /**
   * Get the EPG for a particular channel.
   * 
   * @param type The channel type
   * @param channel The channel index
   * 
   * @return The EPG or null for not present
   */
    public Channel getEPG(ServiceType type, int channel) {
        if (type != ServiceType.TV) return null;
        if (this.epg.size() <= channel) return null;
        return this.epg.get(channel);
    }

    /**
   * Find a channel that matches a channel resource
   * 
   * @param resource The resource
   * @return The channel or null for not found
   */
    public Channel getEPG(Resource resource) {
        String uri = resource.getURI();
        for (Channel channel : this.epg) if (channel.getResource().getURI().equals(uri)) return channel;
        return null;
    }

    /**
   * Update the events list with a list of new events.
   * <p>
   * This is likely to be a partial update and all events need to be
   * either replaced or updated. The rules for replace/update is that
   * the events are partitioned into channels. The events in each channel
   * are sorted and the sorted segment replaces the segment with the same
   * times in the existing list.
   * 
   * @param station The station index
   * @param update The events to update.
   */
    public void updateEPG(int channel, List<EventDescription> update) {
        Station station = this.getStation(ServiceType.TV, channel);
        Resource ch = station.getChannel();
        Collections.sort(update, new EventDescription.DateComparator());
        while (this.epg.size() <= channel) this.epg.add(null);
        Channel cepg = this.epg.get(channel);
        if (cepg == null) {
            cepg = new Channel(this, ch, channel);
            this.epg.set(channel, cepg);
        }
        cepg.updateEvents(update);
        this.firePropertyChange("epg", null, this.epg);
    }

    /**
   * Partition a list of events by channel. 
   * <p>
   * The events are first placed into lists partitioned by
   * {@link Event#getServiceId()}, which is the channel index. 
   * The position of the specific channel in the final list corresponds to
   * the channel index.
   * <p>
   * The events are then sorted by {@link Event.DateComparator}.
   * 
   * @param events The events to partition
   * 
   * @return The partitioned list.
   */
    protected List<List<EventDescription>> partition(Collection<EventDescription> events) {
        List<List<EventDescription>> partition = new ArrayList<List<EventDescription>>(this.epg.size());
        EventDescription.DateComparator eventOrder = new EventDescription.DateComparator();
        for (EventDescription evt : events) {
            List<EventDescription> channel = null;
            int chno = evt.getServiceId();
            if (chno < partition.size()) channel = partition.get(chno);
            if (channel == null) {
                channel = new ArrayList<EventDescription>(128);
                while (partition.size() <= chno) partition.add(null);
                partition.set(chno, channel);
            }
            channel.add(evt);
        }
        for (List<EventDescription> channel : partition) if (channel != null) Collections.sort(channel, eventOrder);
        return partition;
    }

    /**
   * Clean the EPG of any events that end before a given date.
   * 
   * @param expiry The expiry date
   */
    public void cleanEPG(Date expiry) {
        for (Channel cepg : this.epg) if (cepg != null) cepg.cleanEPG(expiry);
        this.firePropertyChange("epg", null, this.epg);
    }

    /**
   * Get the timer list.
   *
   * @return the timers
   */
    public List<Timer> getTimers() {
        return this.timers;
    }

    /**
   * Set the timer list.
   * <p>
   * A complete set of timers is assumed for each time.
   * The previous set of timers is removed from the semantic model,
   * as well as the state.
   *
   * @param timers the timers to set
   */
    public void setTimers(List<TimerDescription> timers) {
        List<Timer> old = this.timers;
        this.getLocator().deletePendingRecordings();
        this.timers = new ArrayList<Timer>(timers.size());
        for (TimerDescription timer : timers) this.timers.add(new Timer(this, timer));
        this.firePropertyChange("timers", old, this.timers);
    }

    /**
   * Add a timer to the timer list.
   * <p>
   * The timer's slot is set, based on the position in the list.
   * 
   * @param timer The timer description
   */
    public void addTimer(TimerDescription timer) {
        timer.setSlot(this.timers.size());
        this.timers.add(new Timer(this, timer));
    }

    /**
   * Find a timer that matches the specified event.
   * 
   * @param event The event
   * 
   * @return The matching timer, or null for none
   */
    public Timer findTimer(Event event) {
        String uri = event.getResource().getURI();
        for (Timer timer : this.timers) if (timer.getEvent() != null && timer.getEvent().getURI().equals(uri)) return timer;
        return null;
    }

    /**
   * Get the recording list.
   *
   * @return the recordings
   */
    public List<Timer> getRecordings() {
        return this.recordings;
    }

    /**
   * Set the recording list.
   *
   * @param recordings the recordings to set
   */
    public void setRecordings(List<RecordingDescription> recordings) {
        List<Timer> old = this.recordings;
        this.recordings = new ArrayList<Timer>(4);
        for (RecordingDescription recording : recordings) this.recordings.add(new Timer(this, recording, this.recordings.size()));
        this.firePropertyChange("recordings", old, this.recordings);
    }

    /**
   * Get a recording by slot.
   *
   * @param slot The slot
   * 
   * @return the recording or null for not found
   */
    public Timer getRecording(int slot) {
        if (this.recordings == null || slot < 0 || slot >= this.recordings.size()) return null;
        return this.recordings.get(slot);
    }

    /**
   * Get the current channel.
   *
   * @return the current channel
   */
    public ChannelSelection getCurrentChannel() {
        return this.currentChannel;
    }

    /**
   * Set the current channel.
   *
   * @param currentChannel the current channel to set
   */
    public void setCurrentChannel(ChannelSelection currentChannel) {
        ChannelSelection old = this.currentChannel;
        this.currentChannel = currentChannel;
        this.currentChannel.setContext(this);
        this.firePropertyChange("currentChannel", old, this.currentChannel);
    }

    /**
   * Get the directory root
   * 
   * @return The directory root
   */
    public DirectoryRoot getRoot() {
        return this.root;
    }

    /**
   * Add some files for a requested directory.
   * 
   * @param directory The directory
   * @param files The files
   */
    public void addFiles(Directory directory, Collection<FileInfo> files) {
        directory.addFiles(files);
        this.firePropertyChange("files", null, this.root);
    }

    /**
   * Add some recordings for a requested directory.
   * 
   * @param directory The directory
   * @param recordings The recordings
   */
    public void addRecordings(Directory directory, Collection<RecordingInfo> recordings) {
        for (RecordingInfo recording : recordings) recording.setContext(this);
        directory.addRecordings(recordings);
        this.firePropertyChange("files", null, this.root);
    }

    /**
   * Get the play info.
   *
   * @return Returns the play info.
   */
    public Playback getPlayInfo() {
        return this.playback;
    }

    /**
   * Set the play info.
   *
   * @param play info The play info to set.
   */
    public void setPlayInfo(Playback playback) {
        Playback old = this.playback;
        this.playback = playback;
        if (this.playback != null) this.playback.setContext(this);
        this.firePropertyChange("playinfo", old, this.playback);
    }

    /**
   * Set the play info along with additional information.
   *
   * @param play info The play info to set.
   * @param event The associated event
   * @param file The associated file
   */
    public void setPlayInfo(PlaybackDescription play, Event event, FileInfoShort file) {
        Playback old = this.playback;
        this.playback = play == null ? null : new Playback(this, play, event, file);
        this.firePropertyChange("playinfo", old, this.playback);
    }

    /**
   * {@inheritDoc}
   *
   * @return The resource locator.
   * 
   * @see org.charvolant.tmsnet.resources.ResourceLocatable#getLocator()
   */
    @Override
    public ResourceLocator getLocator() {
        return this.locator;
    }

    /**
   * Set the resource locator.
   * 
   * @param locator The locator
   */
    public void setLocator(ResourceLocator locator) {
        this.locator = locator;
    }

    /**
   * See if there is a recording that covers this event.
   * 
   * @param event The event
   * 
   * @return The covering recording or null for none
   */
    public Timer findRecording(Event event) {
        String uri = event.getResource().getURI();
        for (Timer recording : this.recordings) if (recording.getEvent() != null && recording.getEvent().getURI().equals(uri)) return recording;
        return null;
    }

    /**
   * See if there is an event that occurs on this time.
   * 
   * @param type The source type
   * @param channel The channel index
   * @param time The time of the event
   * 
   * @return The covering event or null for none
   */
    public Event findEvent(ServiceType type, int channel, Date time) {
        Channel epg = this.getEPG(type, channel);
        if (epg == null) return null;
        return epg.findEvent(time);
    }

    /**
   * Load the resources associated with a locale.
   * 
   * @param locale The locale
   * 
   * @see ResourceLocator#loadLocale(Locale)
   */
    public void loadResources(Locale locale) {
        this.locator.loadLocale(locale);
    }
}
