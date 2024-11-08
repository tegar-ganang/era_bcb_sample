package org.charvolant.tmsnet.model;

import java.net.URI;
import java.util.Date;
import javax.xml.datatype.Duration;
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
 * A description of a timer.
 * <p>
 * This object is derived from the TAP_TimerInfo structure in the
 * Topfield TAP SDK.
 * 
 * @author Doug Palmer &lt;doug@charvolant.org&gt;
 *
 */
public class Timer extends CoverableResourceHolder<PVRState> {

    /**
   * Construct an empty channel information.
   */
    public Timer() {
        super();
    }

    /**
   * Construct a timer from a description.
   *
   * @param context The timer context
   * @param desc The timer description
   */
    public Timer(PVRState context, TimerDescription desc) {
        super(context);
        SystemInformation sysInfo = context.getSysInfo();
        Station station = context.getStation(desc.getServiceType(), desc.getServiceId());
        ResourceLocator locator = this.getLocator();
        Resource resource = locator.createPendingRecording(sysInfo.getResource(), desc.getSlot());
        Resource interval = locator.createInterval(desc.getStart(), desc.getFinish());
        Resource event = locator.findEvent(interval, station.getChannel());
        locator.create(resource, TMSNet.station, station.getResource());
        locator.create(resource, TMSNet.recorder, sysInfo.getResource());
        locator.create(resource, NetEvent.time, interval);
        locator.create(resource, TMSNet.timerRecording, desc.isRecording());
        locator.create(resource, TMSNet.fixFileName, desc.isFixFileName());
        locator.create(resource, TMSNet.fileName, desc.getFileName());
        locator.create(resource, TMSNet.timerSlot, desc.getSlot());
        locator.create(resource, TMSNet.timerTuner, desc.getTuner());
        locator.create(resource, TMSNet.repeat, desc.getReservationType());
        if (event != null) locator.create(resource, TMSNet.records, event);
        this.setResource(resource);
    }

    /**
   * Construct a timer from a recording description.
   *
   * @param context The timer context
   * @param desc The recordidng description
   * @param slot The recording slot
   */
    public Timer(PVRState context, RecordingDescription desc, int slot) {
        super(context);
        SystemInformation sysInfo = context.getSysInfo();
        Station station = context.getStation(desc.getServiceType(), desc.getServiceId());
        ResourceLocator locator = this.getLocator();
        Resource resource = locator.createActiveRecording(sysInfo.getResource(), slot);
        Resource interval = locator.createInterval(desc.getStart(), desc.getFinish());
        Resource event = locator.findEvent(interval, station.getChannel());
        locator.create(resource, TMSNet.station, station.getResource());
        locator.create(resource, TMSNet.recorder, sysInfo.getResource());
        locator.create(resource, NetEvent.time, interval);
        locator.create(resource, TMSNet.fileName, desc.getFileName());
        locator.create(resource, TMSNet.timerSlot, slot);
        locator.create(resource, TMSNet.timerTuner, desc.getTuner());
        locator.create(resource, TMSNet.recordingType, desc.getRecordingType());
        locator.createDuration(resource, TMSNet.timerRecorded, 0, desc.getRecorded());
        if (event != null) locator.create(resource, TMSNet.records, event);
        this.setResource(resource);
    }

    /**
   * Get the recording flag.
   *
   * @return True if currently recording
   */
    @Property(label = "label.recording", detail = "label.recording.detail.timer", group = "label.recording")
    public boolean isRecording() {
        return this.locateBoolean(false, TMSNet.timerRecording);
    }

    /**
   * Get the tuner number.
   *
   * @return the tuner number
   */
    @Property(label = "label.tuner", detail = "label.tuner.detail", group = "label.recording")
    public int getTuner() {
        return this.locateInt(0, TMSNet.timerTuner);
    }

    /**
   * Get the reservation type.
   *
   * @return the reservation type
   */
    @Property(label = "label.reservationType", detail = "label.reservationType.detail", group = "label.time")
    public ReservationType getReservationType() {
        return this.locateEnum(ReservationType.class, TMSNet.repeat);
    }

    /**
   * Is the filename fixed?
   *
   * @return True if the filename is fixed
   */
    @Property(label = "label.fixFileName", detail = "label.fixFileName.detail", group = "label.storage")
    public boolean isFixFileName() {
        return this.locateBoolean(false, TMSNet.fixFileName);
    }

    /**
   * Get the recording duration.
   *
   * @return the duration in minutes
   */
    @Property(label = "label.duration", detail = "label.duration.detail.timer", group = "label.time")
    public int getDuration() {
        return (int) ((this.getFinish().getTime() - this.getStart().getTime() + 30000) / 60000);
    }

    /**
   * Get the recorded time.
   * <p>
   * Active recordings have an amount recorded in seconds.
   *
   * @return the recording type
   */
    @Property(label = "label.elapsedTime", detail = "label.elapsedTime.detail.recording", group = "label.recording")
    public int getRecorded() {
        Duration d = this.locateDuration(TMSNet.timerRecorded);
        if (d == null) return 0;
        return (int) (d.getTimeInMillis(new Date()) / 1000);
    }

    /**
   * Get the progress when recording.
   * 
   * @return The progress as a percentage.
   */
    @Property(label = "label.position", detail = "label.position.detail.recording", group = "label.recording", display = DisplayHint.BAR)
    public int getProgress() {
        return (int) ((this.getRecorded() * 100) / (this.getDuration() * 60));
    }

    /**
   * Get the recording type.
   * <p>
   * Active recordings have a type
   *
   * @return the recording type
   */
    @Property(label = "label.recordingType", detail = "label.recordingType.detail", group = "label.recording")
    public RecordingType getRecordingType() {
        return this.locateEnum(RecordingType.class, TMSNet.recordingType);
    }

    /**
   * Get the start time.
   * <p>
   * Start times are only accurate to the nearest minute.
   *
   * @return the start time
   */
    @Property(label = "label.start", detail = "label.start.detail.timer", group = "label.time")
    public Date getStart() {
        return super.getStart();
    }

    /**
   * {@inheritDoc}
   *
   * @return The interval start
   * 
   * @see org.charvolant.tmsnet.model.CoverableResourceHolder#locateStart()
   */
    @Override
    protected Date locateStart() {
        return this.locateDate(NetEvent.time, Timeline.start);
    }

    /**
   * {@inheritDoc}
   *
   * @return The interval end
   * 
   * @see org.charvolant.tmsnet.model.CoverableResourceHolder#locateFinish()
   */
    @Override
    protected Date locateFinish() {
        return this.locateDate(NetEvent.time, Timeline.end);
    }

    /**
   * Get the finish time.
   * 
   * @return The finish time
   */
    @Property(label = "label.finish", detail = "label.finish.detail.timer", group = "label.time")
    public Date getFinish() {
        return super.getFinish();
    }

    /**
   * Get the file name.
   *
   * @return the file name
   */
    @Property(label = "label.fileName", detail = "label.fileName.detail", group = "label.storage")
    public String getFileName() {
        return this.locateString(TMSNet.fileName);
    }

    /**
   * Get the slot.
   * <p>
   * This is the index number of the timer in the timer table.
   * Annoyingly, slot numbers start at 1.
   *
   * @return the slot
   */
    @Property(label = "label.slot", detail = "label.slot.detail", group = "label.recording")
    public int getSlot() {
        return this.locateInt(0, TMSNet.timerSlot);
    }

    /**
   * Set the timer slot.
   * <p>
   * Used for testing.
   * 
   * @param slot The slot number
   */
    public void setSlot(int slot) {
        ResourceLocator locator = this.getLocator();
        Resource resource = this.getResource();
        locator.create(resource, TMSNet.timerSlot, slot);
    }

    /**
   * Get the station name for the timer
   * 
   * @return The title
   */
    @Property(label = "label.channel", detail = "label.channel.detail.timer", group = "label.source")
    public String getStationName() {
        return this.locateString(TMSNet.station, TMSNet.label);
    }

    /**
   * Get the station index for the timer
   * 
   * @return The title
   */
    @Property(label = "label.channel", detail = "label.channel.detail.timer", group = "label.source")
    public int getStationIndex() {
        return this.locateInt(0, TMSNet.station, TMSNet.stationIndex);
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
        return super.getLargeIcon();
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
        return super.getSmallIcon();
    }

    /**
   * {@inheritDoc}
   * <p>
   * Get the station's icon.
   *
   * @param size The icon size
   * 
   * @return The icon associated with the station
   * 
   * @see org.charvolant.tmsnet.model.ResourceHolder#locateIcon(int)
   */
    @Override
    protected URI locateIcon(int size) {
        return super.locateIcon(this.locateResource(TMSNet.station), size);
    }

    /**
   * Get any matching event for the timer.
   * 
   * @return The event or null for no matching event
   */
    public Resource getEvent() {
        return this.locateResource(TMSNet.records);
    }

    /**
   * Get a title for the timer.
   * <p>
   * If possible, this is deduced from the context.
   * Otherwise, it'll just have to be the file name.
   * 
   * @return The title
   */
    @Property(label = "label.title", detail = "label.title.detail.event", group = "label.name", display = DisplayHint.TITLE)
    public String getTitle() {
        String title = this.locateString(TMSNet.records, PO.broadcast_of, TMSNet.version_of, TMSNet.episode_of, DCTerms.title);
        if (title == null) title = this.getFileName();
        return title;
    }

    /**
   * Get a description for the timer.
   * <p>
   * If possible, this is deduced from the context.
   * Otherwise, it'll just have to be the file name.
   * 
   * @return The title
   */
    @Property(label = "label.description", detail = "label.description.detail.event", group = "label.name", display = DisplayHint.TEXT)
    public String getDescription() {
        return this.locateString(TMSNet.records, PO.broadcast_of, TMSNet.version_of, PO.short_synopsis);
    }

    /**
   * Convert this timer into a description for transfer
   * 
   * @return The equivalent descrption
   */
    public TimerDescription toDescription() {
        TimerDescription desc = new TimerDescription();
        desc.setDuration(this.getDuration());
        desc.setFileName(this.getFileName());
        desc.setFixFileName(this.isFixFileName());
        desc.setRecording(this.isRecording());
        desc.setReservationType(this.getReservationType());
        desc.setServiceId(this.locateInt(0, TMSNet.station, TMSNet.stationIndex));
        desc.setServiceType(this.locateEnum(ServiceType.class, TMSNet.station, TMSNet.serviceType));
        desc.setSlot(this.getSlot());
        desc.setStart(this.getStart());
        desc.setTuner(this.getTuner());
        return desc;
    }

    /**
   * Convert this timer into a recording description for transfer
   * 
   * @return The equivalent descrption
   */
    public RecordingDescription toRecordingDescription() {
        RecordingDescription desc = new RecordingDescription();
        desc.setDuration(this.getDuration());
        desc.setFileName(this.getFileName());
        desc.setFinish(this.getFinish());
        desc.setRecorded(this.getRecorded());
        desc.setRecordingType(this.getRecordingType());
        desc.setServiceId(this.locateInt(0, TMSNet.station, TMSNet.stationIndex));
        desc.setServiceType(this.locateEnum(ServiceType.class, TMSNet.station, TMSNet.serviceType));
        desc.setStart(this.getStart());
        desc.setTuner(this.getTuner());
        return desc;
    }
}
