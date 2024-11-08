package com.google.code.guidatv.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ScheduleResume implements Serializable {

    private static final long serialVersionUID = -5340375584340342592L;

    private Date start;

    private Date end;

    private int minutesInterval;

    private int module;

    private Map<Date, IntervalEntry> intervals;

    private List<Channel> channels = new ArrayList<Channel>();

    public ScheduleResume() {
    }

    public ScheduleResume(Date start, Date end, int minutesInterval) {
        init(start, end, minutesInterval);
    }

    public void init(Date start, Date end, int minutesInterval) {
        module = minutesInterval * 60 * 1000;
        this.start = new Date(start.getTime() - (start.getTime() % module));
        this.end = end;
        this.minutesInterval = minutesInterval;
        intervals = new LinkedHashMap<Date, IntervalEntry>();
        Date currentDate = new Date(start.getTime());
        while (currentDate.compareTo(end) < 0) {
            intervals.put(currentDate, new IntervalEntry(currentDate));
            currentDate = new Date(currentDate.getTime() + module);
        }
    }

    public void add(Schedule schedule) {
        List<Transmission> transmissions = schedule.getTransmissions();
        if (!transmissions.isEmpty()) {
            Channel channel = schedule.getChannel();
            channels.add(channel);
            for (Transmission transmission : transmissions) {
                long time = transmission.getStart().getTime();
                Date pointer = new Date(time - time % module);
                IntervalEntry entry = intervals.get(pointer);
                if (entry != null) {
                    entry.add(channel, transmission);
                }
            }
        }
    }

    public Date getStart() {
        return start;
    }

    public Date getEnd() {
        return end;
    }

    public int getMinutesInterval() {
        return minutesInterval;
    }

    public Collection<IntervalEntry> getIntervals() {
        return intervals.values();
    }

    public List<Channel> getChannels() {
        return channels;
    }

    @Override
    public String toString() {
        return "DayScheduleResume [" + (start != null ? "start=" + start + ", " : "") + (end != null ? "end=" + end + ", " : "") + "minutesInterval=" + minutesInterval + ", module=" + module + ", " + (intervals != null ? "intervals=" + intervals : "") + "]";
    }
}
