package org.jdna.bmt.web.client.media;

import java.io.Serializable;

public class GWTAiringDetails implements Serializable {

    private static final long serialVersionUID = 1L;

    public long startTime;

    public boolean firtRun;

    public long duration;

    public int year;

    public int season;

    public int episode;

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public boolean isFirtRun() {
        return firtRun;
    }

    public void setFirtRun(boolean firtRun) {
        this.firtRun = firtRun;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getNetwork() {
        return network;
    }

    public void setNetwork(String network) {
        this.network = network;
    }

    public String channel;

    public String network;

    private boolean manual;

    public GWTAiringDetails() {
    }

    public boolean isManualRecord() {
        return manual;
    }

    public void setManualRecord(boolean record) {
        this.manual = record;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getSeason() {
        return season;
    }

    public void setSeason(int season) {
        this.season = season;
    }

    public int getEpisode() {
        return episode;
    }

    public void setEpisode(int episode) {
        this.episode = episode;
    }
}
