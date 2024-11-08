package org.openmobster.core.synchronizer.server.engine;

import java.io.Serializable;

/**
 * 
 * @author openmobster@gmail.com
 */
public class ConflictEntry implements Serializable {

    private long id;

    private String deviceId;

    private String app;

    private String channel;

    private String oid;

    private String state;

    public ConflictEntry() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getOid() {
        return oid;
    }

    public void setOid(String oid) {
        this.oid = oid;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }
}
