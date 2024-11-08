package it.polimi.MA.service;

import org.slasoi.common.messaging.Settings;

/**
 * Data structure describe where to find the data of which sensor...
 *
 * TODO: Is the sensorID okay?
 *
 */
public class SensorSubscriptionData {

    protected String sensorID;

    protected Settings busConfiguration;

    protected String[] channels;

    public String getSensorID() {
        return sensorID;
    }

    public void setSensorID(String sensorID) {
        this.sensorID = sensorID;
    }

    public Settings getBusConfiguration() {
        return busConfiguration;
    }

    public void setBusConfiguration(Settings busConfiguration) {
        this.busConfiguration = busConfiguration;
    }

    public String[] getChannels() {
        return channels;
    }

    public void setChannels(String[] channels) {
        this.channels = channels;
    }
}
