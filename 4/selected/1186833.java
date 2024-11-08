package org.openmobster.core.synchronizer.server.engine;

import java.util.Set;

/**
 *
 * @author openmobster@gmail.com
 */
public class AppToChannelAssociation {

    private long oid;

    private String deviceId;

    private String app;

    private String channel;

    public AppToChannelAssociation() {
    }

    public AppToChannelAssociation(String deviceId, String app, String channel) {
        this.deviceId = deviceId;
        this.app = app;
        this.channel = channel;
    }

    public long getOid() {
        return oid;
    }

    public void setOid(long oid) {
        this.oid = oid;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public static void associate(AppToChannelAssociation association) {
        AppToChannelPersistence.getInstance().storeAssociation(association);
    }

    public static Set<String> getApps(String deviceId, String channel) {
        return AppToChannelPersistence.getInstance().readApps(deviceId, channel);
    }
}
