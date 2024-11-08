package org.charvolant.tmsnet.model;

import java.net.URI;
import java.text.MessageFormat;
import java.util.Comparator;
import java.util.Date;
import org.charvolant.properties.annotations.DisplayHint;
import org.charvolant.properties.annotations.Property;
import org.charvolant.tmsnet.resources.NetEvent;
import org.charvolant.tmsnet.resources.PO;
import org.charvolant.tmsnet.resources.ResourceLocator;
import org.charvolant.tmsnet.resources.TMSNet;
import org.charvolant.tmsnet.resources.Timeline;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.DCTerms;

/**
 * A description of an EPG event.
 * <p>
 * This object is derived from the TAP_TapEvent structure in the
 * Topfield TAP SDK.
 * <p>
 * See {@link ChannelInformation} for a discussion of the terminology.
 *
 * @author Doug Palmer &lt;doug@charvolant.org&gt;
 *
 */
public class Event extends CoverableResourceHolder<Channel> {

    /** The pattern for formatting file names */
    private static final MessageFormat FILE_NAME_FORMAT = new MessageFormat("{0} {1,date,yyyy-MM-dd}.mpg");

    /** The maximum name length of the name part */
    private static final int MAX_NAME = 32;

    /**
   * Construct an empty event.
   *
   */
    public Event() {
        super();
    }

    /**
   * Construct an event with a context and a resource.
   *
   * @param context The context
   * @param resource The station resource
   */
    public Event(Channel context, Resource resource) {
        super(context, resource);
    }

    /**
   * Create a station out of the channel description.
   * <p>
   * This resource
   *
   * @param context The context
   * @param desc The channel description
   * @param channel The channel associated with this event
   * 
   * @return The created resource
   * 
   * @see org.charvolant.tmsnet.model.Description#createResource(org.charvolant.tmsnet.resources.ResourceLocator, com.hp.hpl.jena.rdf.model.Resource)
   */
    public Event(Channel context, EventDescription desc, Resource channel) {
        super(context);
        ResourceLocator locator = this.getLocator();
        Resource resource = locator.createBroadcast(channel, desc.getEventId());
        Resource episode = locator.findOrCreateEpisode(desc.getTitle(), desc.getDescription());
        Resource version = locator.createVersion(resource, episode);
        Resource interval = locator.createInterval(desc.getStart(), desc.getFinish());
        locator.create(resource, TMSNet.eventId, desc.getEventId());
        locator.createDate(resource, PO.schedule_date, desc.getStart());
        locator.create(resource, NetEvent.time, interval);
        locator.create(version, PO.duration, desc.getDuration());
        this.setResource(resource);
    }

    /**
   * Set the label for this program.
   *
   * @return The program title.
   * 
   * @see org.charvolant.tmsnet.model.ResourceHolder#locateLabel()
   */
    @Override
    protected String locateLabel() {
        return this.locateString(PO.broadcast_of, TMSNet.version_of, TMSNet.episode_of, DCTerms.title);
    }

    /**
   * Get the event id.
   *
   * @return the event id
   */
    @Property(label = "label.eventId", detail = "label.eventId.detail", group = "label.source")
    public int getEventId() {
        return this.locateInt(0, TMSNet.eventId);
    }

    /**
   * {@inheritDoc}
   * <p>
   * Implemented to provide a {@link Property} definition.
   *
   * @return The start date
   * 
   * @see org.charvolant.tmsnet.model.CoverableResourceHolder#getStart()
   */
    @Override
    @Property(label = "label.start", detail = "label.start.detail.event", group = "label.time")
    public Date getStart() {
        return super.getStart();
    }

    /**
   * {@inheritDoc}
   * <p>
   * Implemented to provide a {@link Property} definition.
   *
   * @return
   * @see org.charvolant.tmsnet.model.CoverableResourceHolder#getFinish()
   */
    @Override
    @Property(label = "label.finish", detail = "label.finish.detail.event", group = "label.time")
    public Date getFinish() {
        return super.getFinish();
    }

    /**
   * Locate the start time.
   *
   * @return the start time
   */
    @Override
    public Date locateStart() {
        return this.locateDate(NetEvent.time, Timeline.start);
    }

    /**
   * Locate the finish time.
   *
   * @return the finish time
   */
    @Override
    public Date locateFinish() {
        return this.locateDate(NetEvent.time, Timeline.end);
    }

    /**
   * Get the duration.
   * 
   * @return the duration
   */
    @Property(label = "label.duration", detail = "label.duration.detail.event", group = "label.time")
    public int getDuration() {
        return this.locateInt(0, PO.broadcast_of, PO.duration);
    }

    /**
   * Is this program currently running?
   *
   * @return the running flag
   */
    @Property(label = "label.running", detail = "label.running.detail.event", group = "label.time")
    public boolean isRunning() {
        return false;
    }

    /**
   * Get the title.
   * <p>
   * The same as the label.
   *
   * @return the title
   */
    @Property(label = "label.title", detail = "label.title.detail.event", group = "label.name", display = DisplayHint.TITLE)
    public String getTitle() {
        return this.getLabel();
    }

    /**
   * Get the description.
   * <p>
   * Try the various synopsis forms.
   *
   * @return the description
   */
    @Property(label = "label.description", detail = "label.description.detail.event", group = "label.name", display = DisplayHint.TEXT)
    public String getDescription() {
        Resource episode = this.locateResource(PO.broadcast_of, TMSNet.version_of);
        if (episode.hasProperty(PO.long_synopsis)) return episode.getProperty(PO.long_synopsis).getString();
        if (episode.hasProperty(PO.medium_synopsis)) return episode.getProperty(PO.medium_synopsis).getString();
        if (episode.hasProperty(PO.short_synopsis)) return episode.getProperty(PO.short_synopsis).getString();
        return null;
    }

    /**
   * Get a filename for this event.
   * <p>
   * We need to take care of dodgy punctuation and long file names.
   * We also add a date, so people can see when something was recorded.
   * 
   * @return The suggested filename for the event
   */
    public String getFilename() {
        StringBuilder name = new StringBuilder(32);
        String title = this.getTitle();
        int pos = 0, lastSp = -1;
        char ch;
        for (pos = 0; pos < title.length() && name.length() < this.MAX_NAME; pos++) {
            ch = title.charAt(pos);
            if (Character.isLetterOrDigit(ch) || ch == '.' || ch == '-' || ch == '_' || ch == '+' || ch == '=' || ch == ',' || ch == ':' || ch == ';' || ch == '?' || ch == '&') name.append(ch); else if (Character.isWhitespace(ch)) {
                lastSp = name.length();
                name.append(' ');
            }
        }
        if (name.length() == 0) name.append("Unknown");
        if (name.length() == this.MAX_NAME && lastSp > this.MAX_NAME - 8) name.setLength(lastSp);
        return this.FILE_NAME_FORMAT.format(new Object[] { name.toString(), this.getStart() });
    }

    /**
   * Get the channel name for the event
   * <p>
   * If possible, this is deduced from the context.
   * Otherwise, it'll just have to be the file name.
   * 
   * @return The title
   */
    @Property(label = "label.channel", detail = "label.channel.detail.event", group = "label.source")
    public String getChannelName() {
        return this.locateString(PO.broadcast_on, TMSNet.label);
    }

    /**
   * Locate an icon from the broadcasting channel.
   *
   * @param size The icon size
   * 
   * @return The icon
   * 
   * @see org.charvolant.tmsnet.model.ResourceHolder#locateIcon(int)
   */
    @Override
    protected URI locateIcon(int size) {
        return super.locateIcon(this.locateResource(PO.broadcast_on), size);
    }

    /**
   * Get the large channel icon for the event
   * <p>
   * This is derived from the channel information.
   * 
   * @return The icon URI
   */
    @Property(group = "label.source", display = DisplayHint.ICON)
    @Override
    public URI getLargeIcon() {
        return super.getLargeIcon();
    }

    /**
   * Get the small channel icon for the event
   * <p>
   * This is derived from the channel information.
   * 
   * @return The icon URI
   */
    @Property(group = "label.source", display = DisplayHint.ICON)
    @Override
    public URI getSmallIcon() {
        return super.getSmallIcon();
    }

    /**
   * See if there is a recording that matches this event
   * 
   * @return The corresponding timer or null for none
   */
    public Timer getRecording() {
        Channel epg = this.getContext();
        PVRState state = epg == null ? null : epg.getContext();
        return state == null ? null : state.findRecording(this);
    }

    /**
   * Get a timer that records this event.
   * 
   * @return The matching timer for this event
   */
    public Timer getTimer() {
        Channel epg = this.getContext();
        PVRState state = epg == null ? null : epg.getContext();
        return state == null ? null : state.findTimer(this);
    }

    /**
   * Convert this event into a description.
   * 
   * @return The event description
   */
    public EventDescription toDescription() {
        EventDescription desc = new EventDescription();
        Channel channel = this.getContext();
        PVRState state = channel == null ? null : channel.getContext();
        Station station = state == null ? null : state.getStation(ServiceType.TV, channel.getIndex());
        desc.setDescription(this.getDescription());
        desc.setDuration(this.getDuration());
        desc.setEventId(this.getEventId());
        desc.setFinish(this.getFinish());
        desc.setRunning(this.isRunning());
        desc.setServiceId(channel == null ? 0 : channel.getIndex());
        desc.setStart(this.getStart());
        desc.setTitle(this.getTitle());
        desc.setTransportStreamId(station == null ? 0 : station.getTransportStreamId());
        return desc;
    }

    /**
   * A comparator to sort events into date/time order.
   */
    public static class DateComparator implements Comparator<Event> {

        @Override
        public int compare(Event o1, Event o2) {
            return o1.getStart().compareTo(o2.getStart());
        }
    }
}
