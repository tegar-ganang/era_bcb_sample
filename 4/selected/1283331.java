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
import org.charvolant.tmsnet.protocol.TMSNetElement;
import org.charvolant.tmsnet.protocol.TMSNetField;
import org.charvolant.tmsnet.protocol.TMSNetFieldType;
import org.charvolant.tmsnet.protocol.TMSNetType;

/**
 * Information about a currently playing recording.
 * <p>
 * This object is derived from the TAP_PlayInfo structure in the
 * Topfield TAP SDK.
 * The file entry and event information has been split out.
 *
 * @author Doug Palmer &lt;doug@charvolant.org&gt;
 *
 */
@XmlRootElement()
@XmlAccessorType(XmlAccessType.NONE)
@TMSNetElement(type = TMSNetType.RECORD)
public class PlayInfo extends ContextHolder<PVRState> {

    /** The play mode */
    @XmlElement
    @TMSNetField(order = 0)
    @Property(label = "label.playMode", detail = "label.playMode.detail", group = "label.playback")
    private PlayMode playMode;

    /** The start time */
    @XmlElement
    @TMSNetField(order = 1)
    @Property(label = "label.trickMode", detail = "label.trickMode.detail", group = "label.playback")
    private TrickMode trickMode;

    /** The ff/rewind speed */
    @XmlElement
    @TMSNetField(type = TMSNetFieldType.BYTE, order = 2)
    @Property(label = "label.speed", detail = "label.speed.detail", group = "label.playback")
    private int speed;

    /** The service type */
    @XmlAttribute
    @TMSNetField(order = 3)
    @Property(label = "label.serviceType", detail = "label.serviceType.detail", group = "label.source")
    private ChannelType serviceType;

    /** Service identifier */
    @XmlElement(name = "sid")
    @TMSNetField(order = 4)
    @Property(label = "label.serviceId", detail = "label.serviceId.detail.event", group = "label.source")
    private int serviceId;

    /** The duration, encoded as hour << 8 | minutes  */
    @XmlElement
    @TMSNetField(type = TMSNetFieldType.DURATION, order = 5)
    @Property(label = "label.duration", detail = "label.duration.detail.event", group = "label.playback")
    private int duration;

    /** The current block */
    @TMSNetField(order = 6)
    @Property(label = "label.currentBlock", detail = "label.currentBlock.detail", group = "label.playback")
    private long currentBlock;

    /** The current block */
    @TMSNetField(order = 7)
    @Property(label = "label.totalBlocks", detail = "label.totalBlocks.detail", group = "label.playback")
    private long totalBlocks;

    /** The associated event */
    private Event event;

    /** The associated file */
    private FileInfoShort file;

    /**
   * Construct an empty event.
   *
   */
    public PlayInfo() {
        super();
    }

    /**
   * Get the playMode.
   *
   * @return Returns the playMode.
   */
    public PlayMode getPlayMode() {
        return this.playMode;
    }

    /**
   * Set the playMode.
   *
   * @param playMode The playMode to set.
   */
    public void setPlayMode(PlayMode playMode) {
        this.playMode = playMode;
    }

    /**
   * Get the trickMode.
   *
   * @return Returns the trickMode.
   */
    public TrickMode getTrickMode() {
        return this.trickMode;
    }

    /**
   * Set the trickMode.
   *
   * @param trickMode The trickMode to set.
   */
    public void setTrickMode(TrickMode trickMode) {
        this.trickMode = trickMode;
    }

    /**
   * Get the speed.
   *
   * @return Returns the speed.
   */
    public int getSpeed() {
        return this.speed;
    }

    /**
   * Set the speed.
   *
   * @param speed The speed to set.
   */
    public void setSpeed(int speed) {
        this.speed = speed;
    }

    /**
   * Get the serviceType.
   *
   * @return Returns the serviceType.
   */
    public ChannelType getServiceType() {
        return this.serviceType;
    }

    /**
   * Set the serviceType.
   *
   * @param serviceType The serviceType to set.
   */
    public void setServiceType(ChannelType serviceType) {
        this.serviceType = serviceType;
    }

    /**
   * Get the serviceId.
   *
   * @return Returns the serviceId.
   */
    public int getServiceId() {
        return this.serviceId;
    }

    /**
   * Set the serviceId.
   *
   * @param serviceId The serviceId to set.
   */
    public void setServiceId(int serviceId) {
        this.serviceId = serviceId;
    }

    /**
   * Get the duration.
   *
   * @return Returns the duration.
   */
    public int getDuration() {
        return this.duration;
    }

    /**
   * Set the duration.
   *
   * @param duration The duration to set.
   */
    public void setDuration(int duration) {
        this.duration = duration;
    }

    /**
   * Get the currentBlock.
   *
   * @return Returns the currentBlock.
   */
    public long getCurrentBlock() {
        return this.currentBlock;
    }

    /**
   * Set the currentBlock.
   *
   * @param currentBlock The currentBlock to set.
   */
    public void setCurrentBlock(long currentBlock) {
        this.currentBlock = currentBlock;
    }

    /**
   * Get the totalBlocks.
   *
   * @return Returns the totalBlocks.
   */
    public long getTotalBlocks() {
        return this.totalBlocks;
    }

    /**
   * Set the totalBlocks.
   *
   * @param totalBlocks The totalBlocks to set.
   */
    public void setTotalBlocks(long totalBlocks) {
        this.totalBlocks = totalBlocks;
    }

    /**
   * Get the event.
   *
   * @return Returns the event.
   */
    public Event getEvent() {
        return this.event;
    }

    /**
   * Set the event.
   *
   * @param event The event to set.
   */
    public void setEvent(Event event) {
        this.event = event;
    }

    /**
   * Get the file.
   *
   * @return Returns the file.
   */
    public FileInfoShort getFile() {
        return this.file;
    }

    /**
   * Set the file.
   *
   * @param file The file to set.
   */
    public void setFile(FileInfoShort file) {
        this.file = file;
    }

    /**
   * Get the channel associated with this timer.
   * 
   * @return The channel information
   */
    public ChannelInformation getChannel() {
        PVRState state = this.getContext();
        if (state == null) return null;
        return state.getChannel(this.getServiceType(), this.getServiceId());
    }

    /**
   * Get the channel name for the timer
   * <p>
   * If possible, this is deduced from the context.
   * Otherwise, it'll just have to be the file name.
   * 
   * @return The title
   */
    @Property(label = "label.channel", detail = "label.channel.detail.event", group = "label.source")
    public String getChannelName() {
        ChannelInformation channel = this.getChannel();
        return channel == null ? "" : channel.getName();
    }

    /**
   * Get the large channel icon for the timer
   * <p>
   * This is derived from the channel information.
   * 
   * @return The icon URI
   */
    @Property(group = "label.source", display = DisplayHint.ICON)
    public URI getLargeIcon() {
        ChannelInformation channel = this.getChannel();
        return channel == null ? null : channel.getLargeIcon();
    }

    /**
   * Get the small channel icon for the timer
   * <p>
   * This is derived from the channel information.
   * 
   * @return The icon URI
   */
    @Property(group = "label.source", display = DisplayHint.ICON)
    public URI getSmallIcon() {
        ChannelInformation channel = this.getChannel();
        return channel == null ? null : channel.getSmallIcon();
    }

    /**
   * Get the title of what's playing
   * 
   * @return The title
   * 
   * @see org.charvolant.tmsnet.model.Event#getTitle()
   */
    @Property(label = "label.title", detail = "label.title.detail.event", group = "label.source", display = DisplayHint.TITLE)
    public String getTitle() {
        return this.event == null ? null : this.event.getTitle();
    }

    /**
   * Get the description of what's playing
   * 
   * @return The description
   * 
   * @see org.charvolant.tmsnet.model.Event#getDescription()
   */
    @Property(label = "label.description", detail = "label.description.detail.event", group = "label.source", display = DisplayHint.TEXT)
    public String getDescription() {
        return this.event == null ? null : this.event.getDescription();
    }

    /**
   * Get the start time of what's playing
   * 
   * @return The start time
   * 
   * @see org.charvolant.tmsnet.model.Event#getStart()
   */
    @Property(label = "label.start", detail = "label.start.detail.event", group = "label.source")
    public Date getStart() {
        return this.event == null ? null : this.event.getStart();
    }

    /**
   * Get the progress when playing.
   * 
   * @return The progress as a percentage.
   */
    @Property(label = "label.position", detail = "label.position.detail.playInfo", group = "label.playback", display = DisplayHint.BAR)
    public int getProgress() {
        return (int) (this.currentBlock * 100L / this.totalBlocks);
    }
}
