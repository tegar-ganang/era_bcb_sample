package org.jtv.common;

import java.beans.ConstructorProperties;

public class RecordingData implements Comparable<RecordingData> {

    private int id;

    private int channel;

    private long start;

    private long end;

    private String name;

    public RecordingData() {
        super();
    }

    @ConstructorProperties({ "channel", "start", "end", "name" })
    public RecordingData(int channel, long start, long end, String name) {
        super();
        this.id = -1;
        this.channel = channel;
        this.start = start;
        this.end = end;
        this.name = name;
    }

    public int getChannel() {
        return channel;
    }

    public long getEnd() {
        return end;
    }

    public String getName() {
        return name;
    }

    public long getStart() {
        return start;
    }

    public int hashCode() {
        final int PRIME = 31;
        int result = super.hashCode();
        result = PRIME * result + channel;
        result = PRIME * result + (int) (end ^ (end >>> 32));
        result = PRIME * result + ((name == null) ? 0 : name.hashCode());
        result = PRIME * result + (int) (start ^ (start >>> 32));
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (getClass() != obj.getClass()) return false;
        final RecordingData other = (RecordingData) obj;
        if (channel != other.channel) return false;
        if (end != other.end) return false;
        if (name == null) {
            if (other.name != null) return false;
        } else if (!name.equals(other.name)) return false;
        if (start != other.start) return false;
        return true;
    }

    public int compareTo(RecordingData o) {
        int startCompare = (int) (getStart() - o.getStart());
        if (startCompare == 0) {
            if (equals(o)) {
                return 0;
            } else {
                return 1;
            }
        } else {
            return startCompare;
        }
    }

    public String toString() {
        return "recording data: start " + getStart() + " end " + getEnd() + " channel " + getChannel() + " name " + getName();
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
