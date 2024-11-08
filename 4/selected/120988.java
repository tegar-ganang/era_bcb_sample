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
 * A description of a currently active recording.
 * <p>
 * This object is derived from the TAP_RecInfo structure in the
 * Topfield TAP SDK.
 * 
 * @author Doug Palmer &lt;doug@charvolant.org&gt;
 *
 */
@XmlRootElement()
@XmlAccessorType(XmlAccessType.NONE)
@TMSNetElement(type = TMSNetType.RECORD)
public class Recording extends AbstractCoverable<PVRState> implements Coverable {

    /** The recording type */
    @XmlAttribute
    @TMSNetField(order = 0)
    @Property(label = "label.recordingType", detail = "label.recordingType.detail", group = "label.recording")
    private RecordingType recordingType;

    /** Tuner number. */
    @XmlAttribute
    @TMSNetField(type = TMSNetFieldType.BYTE, order = 1)
    @Property(label = "label.tuner", detail = "label.tuner.detail", group = "label.recording")
    private int tuner;

    /** The service type */
    @XmlAttribute
    @TMSNetField(order = 2)
    @Property(label = "label.serviceType", detail = "label.serviceType.detail", group = "label.source")
    private ChannelType serviceType;

    /** Service identifier */
    @XmlElement(name = "sid")
    @TMSNetField(order = 3)
    @Property(label = "label.serviceId", detail = "label.serviceId.detail.recording", group = "label.source")
    private int serviceId;

    /** The duration of the recording (minutes) */
    @XmlElement
    @TMSNetField(order = 4)
    @Property(label = "label.duration", detail = "label.duration.detail.recording", group = "label.recording")
    private int duration;

    /** Start time for the timer */
    @XmlElement(name = "start-time")
    @TMSNetField(order = 5)
    @Property(label = "label.start", detail = "label.start.detail.recording", group = "label.recording")
    private Date start;

    /** End time for the timer */
    @XmlElement(name = "end-time")
    @TMSNetField(order = 6)
    @Property(label = "label.finish", detail = "label.finish.detail.recording", group = "label.recording")
    private Date finish;

    /** The time spent recording (seconds)*/
    @XmlElement
    @TMSNetField(order = 7)
    @Property(label = "label.elapsedTime", detail = "label.elapsedTime.detail.recording", group = "label.recording")
    private int recorded;

    /** The file name of the recording */
    @XmlElement(name = "file-name")
    @TMSNetField(order = 8)
    @Property(label = "label.fileName", detail = "label.fileName.detail.recording", group = "label.recording")
    private String fileName;

    /**
   * Construct an empty recording information.
   *
   */
    public Recording() {
        super();
    }

    /**
   * Get the recording type.
   *
   * @return the recording type
   */
    public RecordingType getRecordingType() {
        return this.recordingType;
    }

    /**
   * Set the recording type.
   *
   * @param recordingType the recording type to set
   */
    public void setRecordingType(RecordingType recordingType) {
        this.recordingType = recordingType;
    }

    /**
   * Get the tuner number.
   *
   * @return the tuner number
   */
    public int getTuner() {
        return this.tuner;
    }

    /**
   * Set the tuner number.
   *
   * @param tuner the tuner number to set
   */
    public void setTuner(int tuner) {
        this.tuner = tuner;
    }

    /**
   * Get the service type.
   *
   * @return the service type
   */
    public ChannelType getServiceType() {
        return this.serviceType;
    }

    /**
   * Set the service type.
   *
   * @param serviceType the service type to set
   */
    public void setServiceType(ChannelType serviceType) {
        this.serviceType = serviceType;
    }

    /**
   * Get the service identifier.
   * <p>
   * This is the channel identifier to record from.
   *
   * @return the service identifier
   * 
   * @see ChannelInformation#getServiceId()
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
   * Get the recording duration.
   *
   * @return the duration in minutes
   */
    public int getDuration() {
        return this.duration;
    }

    /**
   * Set the recording duration.
   *
   * @param duration the duration in minutes to set
   */
    public void setDuration(int duration) {
        this.duration = duration;
    }

    /**
   * Get the start time.
   * <p>
   * Start times are only accurate to the nearest minute.
   *
   * @return the start time
   */
    public Date getStart() {
        return this.start;
    }

    /**
   * Set the start time.
   *
   * @param start the start time to set
   */
    public void setStart(Date start) {
        this.start = start;
    }

    /**
   * Get the finish time.
   * <p>
   * End times are only accurate to the nearest minute.
   *
   * @return the end time
   */
    public Date getFinish() {
        return this.finish;
    }

    /**
   * Set the finish time.
   *
   * @param finish the end time to set
   */
    public void setFinish(Date finish) {
        this.finish = finish;
    }

    /**
   * Get the recorded time.
   * <p>
   * This is the amount of time the recorder has been running.
   *
   * @return the time in seconds
   */
    public int getRecorded() {
        return this.recorded;
    }

    /**
   * Set the recorded time.
   *
   * @param duration the duration in minutes to set
   */
    public void setRecorded(int recorded) {
        this.recorded = recorded;
    }

    /**
   * Get the file name.
   *
   * @return the file name
   */
    public String getFileName() {
        return this.fileName;
    }

    /**
   * Set the file name.
   *
   * @param fileName the file name to set
   */
    public void setFileName(String fileName) {
        this.fileName = fileName;
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
    @Property(label = "label.channel", detail = "label.channel.detail.recording", group = "label.source")
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
   * Get the progress when recording.
   * 
   * @return The progress as a percentage.
   */
    @Property(label = "label.position", detail = "label.position.detail.recording", group = "label.recording", display = DisplayHint.BAR)
    public int getProgress() {
        return (int) ((this.recorded * 100) / (this.duration * 60));
    }

    /**
   * Get the event that this recording is recording.
   * 
   * @return The event or null if not found
   */
    public Event getEvent() {
        PVRState state = this.getContext();
        if (state == null) return null;
        return state.findEvent(this);
    }

    /**
   * Get the title of what's playing
   * 
   * @return The title
   * 
   * @see org.charvolant.tmsnet.model.Event#getTitle()
   */
    @Property(label = "label.title", detail = "label.title.detail.recording", group = "label.source", display = DisplayHint.TITLE)
    public String getTitle() {
        Event event = this.getEvent();
        return event == null ? null : event.getTitle();
    }

    /**
   * Get the description of what's playing
   * 
   * @return The description
   * 
   * @see org.charvolant.tmsnet.model.Event#getDescription()
   */
    @Property(label = "label.description", detail = "label.description.detail.recording", group = "label.source", display = DisplayHint.TEXT)
    public String getDescription() {
        Event event = this.getEvent();
        return event == null ? null : event.getDescription();
    }

    /**
   * Get the slot for this recording
   * 
   * @return The slot position, 0 if not in a specific slot
   */
    public int getSlot() {
        PVRState state = this.getContext();
        int slot = 0;
        if (state == null) return 0;
        for (Recording recording : state.getRecordings()) {
            if (recording == this) return slot;
            slot++;
        }
        return 0;
    }
}
