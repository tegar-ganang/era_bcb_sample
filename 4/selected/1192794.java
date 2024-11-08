package org.charvolant.tmsnet.client;

import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.charvolant.tmsnet.TMSNetError;
import org.charvolant.tmsnet.command.GetChannelList;
import org.charvolant.tmsnet.command.GetChannelListResponse;
import org.charvolant.tmsnet.command.GetCurrentChannel;
import org.charvolant.tmsnet.command.GetCurrentChannelResponse;
import org.charvolant.tmsnet.command.GetFileInfos;
import org.charvolant.tmsnet.command.GetFileInfosResponse;
import org.charvolant.tmsnet.command.GetOneChannelEvents;
import org.charvolant.tmsnet.command.GetOneChannelEventsResponse;
import org.charvolant.tmsnet.command.GetPlayInfo;
import org.charvolant.tmsnet.command.GetPlayInfoResponse;
import org.charvolant.tmsnet.command.GetRecInfo;
import org.charvolant.tmsnet.command.GetRecInfoResponse;
import org.charvolant.tmsnet.command.GetSysInfo;
import org.charvolant.tmsnet.command.GetSysInfoResponse;
import org.charvolant.tmsnet.command.GetTimersList;
import org.charvolant.tmsnet.command.GetTimersListResponse;
import org.charvolant.tmsnet.command.ListFiles;
import org.charvolant.tmsnet.command.ListFilesResponse;
import org.charvolant.tmsnet.model.Channel;
import org.charvolant.tmsnet.model.Directory;
import org.charvolant.tmsnet.model.Event;
import org.charvolant.tmsnet.model.EventDescription;
import org.charvolant.tmsnet.model.ServiceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Get the state of the PVR.
 * <p>
 * This is a fairly long-running transaction where we suck all the information that we can across from
 * the PVR.
 *
 * @author Doug Palmer &lt;doug@charvolant.org&gt;
 *
 */
@XmlRootElement()
@XmlAccessorType(XmlAccessType.NONE)
public class GetStateTransaction extends Transaction<Transactable> {

    /** The logger for this transaction */
    private static final Logger logger = LoggerFactory.getLogger(GetStateTransaction.class);

    /** Trigger that we have cleaned the EPG */
    public static final String TRIGGER_CLEANED_EPG = "cleaned-epg";

    /** Trigger that we have filled the EPG */
    public static final String TRIGGER_FILLED_EPG = "filled-epg";

    /** The number of days of EPG to keep before */
    @XmlElement
    private int epgDaysBefore;

    /** The number of days of EPG to keep after */
    @XmlElement
    private int epgDaysAfter;

    /** The start epg day */
    private Calendar startEpgDay;

    /** The current epg day */
    private Calendar currentEpgDay;

    /** The limit epg day */
    private Calendar limitEpgDay;

    /** The channel index */
    private int channel;

    /** The directory */
    private Directory directory;

    /**
   * Construct an empty get state transaction
   *
   */
    public GetStateTransaction() {
        super();
    }

    /**
   * Construct a get state transaction for a specific range of epg days.
   *
   * @param epgDaysBefore The number of days before now to retain
   * @param epgDaysAfter The number of days after now to get
   */
    public GetStateTransaction(int epgDaysBefore, int epgDaysAfter) {
        super();
        this.epgDaysBefore = epgDaysBefore;
        this.epgDaysAfter = epgDaysAfter;
    }

    /**
   * Start the ball rolling by requesting to play a file.
   *
   * @see org.charvolant.tmsnet.client.Transaction#onExecute()
   */
    @Override
    protected void onExecute() {
        super.onExecute();
        this.queue(new GetSysInfo());
    }

    /**
   * Process system information coming in
   * 
   * @param response The system information response
   */
    public void onGetSysInfoResponse(GetSysInfoResponse response) {
        this.state.setSysInfo(response.getSysInfo());
    }

    /**
   * Request a channel list from the server
   */
    public void onGetChannelListTV() {
        this.queue(new GetChannelList(ServiceType.TV));
    }

    /**
   * Process channel list coming in
   * 
   * @param response The channel list response
   */
    public void onGetChannelListResponseTV(GetChannelListResponse response) {
        this.state.setStations(ServiceType.TV, response.getChannels());
    }

    /**
   * Request a channel list from the server
   */
    public void onGetChannelListRadio() {
        this.queue(new GetChannelList(ServiceType.RADIO));
    }

    /**
   * Process channel list coming in
   * 
   * @param response The channel list response
   */
    public void onGetChannelListResponseRadio(GetChannelListResponse response) {
        this.state.setStations(ServiceType.RADIO, response.getChannels());
    }

    /**
   * Request the current channel from the server
   */
    public void onGetCurrentChannel() {
        this.queue(new GetCurrentChannel());
    }

    /**
   * Process system information coming in
   * 
   * @param response The system information response
   */
    public void onGetCurrentChannelResponse(GetCurrentChannelResponse response) {
        this.state.setCurrentChannel(response.getChannel());
    }

    /**
   * Clean the EPG of expired entries and set things up for collecting the channel data.
   */
    public void onCleanEPG() {
        Calendar cleanEpgDay;
        this.startEpgDay = Calendar.getInstance();
        this.startEpgDay.set(Calendar.HOUR_OF_DAY, 0);
        this.startEpgDay.set(Calendar.MINUTE, 0);
        this.startEpgDay.set(Calendar.SECOND, 0);
        this.startEpgDay.set(Calendar.MILLISECOND, 0);
        this.currentEpgDay = Calendar.getInstance();
        this.currentEpgDay.setTime(this.startEpgDay.getTime());
        this.limitEpgDay = Calendar.getInstance();
        this.limitEpgDay.setTime(this.currentEpgDay.getTime());
        this.limitEpgDay.add(Calendar.DAY_OF_MONTH, this.epgDaysAfter);
        cleanEpgDay = Calendar.getInstance();
        cleanEpgDay.setTime(this.currentEpgDay.getTime());
        cleanEpgDay.add(Calendar.DAY_OF_MONTH, -this.epgDaysBefore);
        this.state.cleanEPG(cleanEpgDay.getTime());
        this.context.addProgress(this.state.getStations(ServiceType.TV).getStations().size());
        this.channel = 0;
        this.context.event(this.TRIGGER_CLEANED_EPG);
    }

    /**
   * Get one days worth of events.
   * <p>
   * We get events for a week, starting from today.
   * Too bad if there isn't anything there.
   */
    public void onGetOneChannelEvents() {
        Date start, end;
        if (this.channel >= this.state.getStations(ServiceType.TV).getStations().size()) {
            this.context.event(this.TRIGGER_FILLED_EPG);
            return;
        }
        start = this.currentEpgDay.getTime();
        this.currentEpgDay.add(Calendar.DAY_OF_MONTH, 7);
        end = this.currentEpgDay.getTime();
        this.logger.debug("Getting for channel " + this.channel + " " + this.state.getStations(ServiceType.TV).getStation(this.channel).getName() + " from " + start + " to " + end);
        this.queue(new GetOneChannelEvents(this.channel, start, end));
    }

    /**
   * Get a response of a days events.
   * 
   * @param response The response
   */
    public void onGetOneChannelEventsResponse(GetOneChannelEventsResponse response) {
        if (response.getEvents() != null) {
            Iterator<EventDescription> ri = response.getEvents().iterator();
            while (ri.hasNext()) {
                EventDescription event = ri.next();
                if (event.getFinish().before(this.startEpgDay.getTime()) || event.getStart().after(this.limitEpgDay.getTime())) {
                    this.logger.debug("Discarding event " + event.getEventId() + " " + event.getTitle() + " " + event.getStart());
                    ri.remove();
                }
            }
            this.logger.debug("Got " + response.getEvents().size() + " events");
            this.state.updateEPG(this.channel, response.getEvents());
        }
        if (!this.currentEpgDay.before(this.limitEpgDay)) {
            this.currentEpgDay.setTime(this.startEpgDay.getTime());
            this.channel++;
        }
    }

    /**
   * Get a response of a days events.
   * 
   * @param response The response
   */
    public void onGetOneChannelEventsError(TMSNetError error) {
        this.logger.warn("Unable to get events " + error);
        this.currentEpgDay.setTime(this.startEpgDay.getTime());
        this.channel++;
    }

    /**
   * Request the recording information from the server
   */
    public void onGetRecInfo() {
        this.queue(new GetRecInfo());
    }

    /**
   * Process recording information coming in
   * 
   * @param response The system information response
   */
    public void onGetRecInfoResponse(GetRecInfoResponse response) {
        this.state.setRecordings(response.getRecordings());
    }

    /**
   * Request the timers from the server
   */
    public void onGetTimersList() {
        this.queue(new GetTimersList());
    }

    /**
   * Process timers coming in
   * 
   * @param response The system information response
   */
    public void onGetTimersListResponse(GetTimersListResponse response) {
        this.state.setTimers(response.getTimers());
    }

    /**
   * Request the root file list from the server
   */
    public void onListRootFiles() {
        this.directory = this.state.getRoot().getRoot();
        this.queue(new ListFiles(this.directory.getPath()));
    }

    /**
   * Respond to a list of files
   * 
   * @param response The response
   */
    public void onListFilesResponse(ListFilesResponse response) {
        this.state.addFiles(this.directory, response.getFiles());
    }

    /**
   * Get the file information for the currently requested directory.
   */
    public void onGetFileInfos() {
        this.queue(new GetFileInfos(this.directory.getPath()));
    }

    /**
   * Respond to a list of recording information
   * 
   * @param response The response
   */
    public void onGetFileInfosResponse(GetFileInfosResponse response) {
        this.state.addRecordings(this.directory, response.getFiles());
    }

    /**
   * Respond to current play information
   * 
   * @param response The response
   */
    public void onGetPlayInfoResponse(GetPlayInfoResponse response) {
        EventDescription ed = response.getEvent();
        Channel channel = ed == null ? null : this.state.getEPG(ServiceType.TV, ed.getServiceId());
        Event event = channel == null ? null : new Event(channel, ed, channel.getResource());
        this.state.setPlayInfo(response.getPlayInfo(), event, response.getFileInfo());
    }

    /**
   * Get the currently playing state from the server
   */
    public void onGetPlayInfo() {
        this.queue(new GetPlayInfo());
    }
}
