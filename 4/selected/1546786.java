package spindles.gwt.shared;

public class SleepPartDTO extends DTO {

    private String channel;

    private long sleepSessionID;

    private String startTime;

    private String endTime;

    public SleepPartDTO() {
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String end) {
        this.endTime = end;
    }

    public long getSleepSessionID() {
        return sleepSessionID;
    }

    public void setSleepSessionID(long sleepSessionID) {
        this.sleepSessionID = sleepSessionID;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String start) {
        this.startTime = start;
    }

    public String[] toRecord() {
        return new String[] { getId(), startTime, endTime, channel };
    }
}
