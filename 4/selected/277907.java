package net.sourceforge.recman.backend.manager.pojo;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 * Recording Pojo
 * 
 * @author Marcus Kessel
 * 
 */
@XmlRootElement(name = "recording")
public class Recording implements RecmanObject {

    private String id;

    private String channelName;

    private XMLGregorianCalendar startDate;

    private Duration duration;

    private Duration totalDuration;

    private String title;

    private String shortText;

    private String description;

    private List<StreamInfo> streamInfos = new ArrayList<StreamInfo>();

    private String path;

    private int parts;

    private boolean ts;

    private String streamUrl;

    /**
     * Constructor
     * 
     * @param channelName
     * @param startDate
     * @param duration
     * @param totalDuration
     * @param title
     * @param description
     * @param shortText
     * @param id
     * @param streamInfos
     * @param path
     * @param parts
     * @param ts
     */
    public Recording(String channelName, XMLGregorianCalendar startDate, Duration duration, Duration totalDuration, String title, String description, String shortText, String id, List<StreamInfo> streamInfos, String path, int parts, boolean ts, String streamUrl) {
        this.channelName = channelName;
        this.startDate = startDate;
        this.duration = duration;
        this.totalDuration = totalDuration;
        this.title = title;
        this.description = description;
        this.shortText = shortText;
        this.id = id;
        this.streamInfos = streamInfos;
        this.path = path;
        this.parts = parts;
        this.ts = ts;
        this.streamUrl = streamUrl;
    }

    /**
     * Constructor
     */
    public Recording() {
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getParts() {
        return parts;
    }

    public void setParts(int parts) {
        this.parts = parts;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public XMLGregorianCalendar getStartDate() {
        return startDate;
    }

    public void setStartDate(XMLGregorianCalendar startDate) {
        this.startDate = startDate;
    }

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    public Duration getTotalDuration() {
        return totalDuration;
    }

    public void setTotalDuration(Duration totalDuration) {
        this.totalDuration = totalDuration;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getShortText() {
        return shortText;
    }

    public void setShortText(String shortText) {
        this.shortText = shortText;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isTs() {
        return ts;
    }

    public void setTs(boolean ts) {
        this.ts = ts;
    }

    @XmlElement(name = "streamInfo")
    public List<StreamInfo> getStreamInfos() {
        return streamInfos;
    }

    public void setStreamInfos(List<StreamInfo> streamInfos) {
        this.streamInfos = streamInfos;
    }

    public void addStreamInfo(StreamInfo streamInfo) {
        if (streamInfos != null) {
            if (streamInfos.contains(streamInfo)) {
                return;
            }
            streamInfos.add(streamInfo);
        }
    }

    public void removeStreamInfo(StreamInfo streamInfo) {
        if (streamInfos != null) {
            if (streamInfos.contains(streamInfo)) {
                streamInfos.remove(streamInfo);
            }
        }
    }

    public String getStreamUrl() {
        return streamUrl;
    }

    public void setStreamUrl(String streamUrl) {
        this.streamUrl = streamUrl;
    }

    public boolean isActive() {
        return false;
    }

    /**
     * to String
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("\nTitle: " + this.getTitle());
        sb.append("\nStartdate: " + this.getStartDate());
        sb.append("\nDuration: " + this.getDuration());
        sb.append("\nTotalDuration: " + this.getTotalDuration());
        sb.append("\nChannelname: " + this.getChannelName());
        sb.append("\nDescription: " + this.getDescription());
        sb.append("\nShort text: " + this.getShortText());
        if (streamInfos != null && streamInfos.size() > 0) {
            for (StreamInfo streamInfo : streamInfos) {
                sb.append("\nStreaminfo: " + streamInfo.toString());
            }
        }
        sb.append("\nPath: " + this.getPath());
        sb.append("\nParts: " + this.getParts());
        sb.append("\nTS: " + this.isTs());
        sb.append("\nID: " + this.getId());
        return sb.toString();
    }
}
