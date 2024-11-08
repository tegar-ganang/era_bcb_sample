package org.openmobster.core.synchronizer.event;

import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import org.apache.log4j.Logger;
import org.openmobster.core.common.event.Event;
import org.openmobster.core.common.event.EventListener;
import org.openmobster.core.security.device.Device;
import org.openmobster.core.security.device.DeviceController;
import org.openmobster.core.security.identity.Identity;
import org.openmobster.core.synchronizer.server.Session;
import org.openmobster.core.synchronizer.server.SyncContext;
import org.openmobster.core.synchronizer.server.engine.Tools;
import org.openmobster.cloud.api.ExecutionContext;
import org.openmobster.cloud.api.sync.MobileBean;
import org.openmobster.core.synchronizer.server.engine.ServerSyncEngine;
import org.openmobster.core.synchronizer.server.engine.ConflictEngine;
import org.openmobster.core.synchronizer.server.engine.ChangeLogEntry;
import org.openmobster.core.push.notification.Notifier;
import org.openmobster.core.push.notification.Notification;

/**
 *
 * @author openmobster@gmail.com
 */
public class CreateBeanEventListener implements EventListener {

    private static Logger log = Logger.getLogger(CreateBeanEventListener.class);

    private ServerSyncEngine syncEngine = null;

    private ConflictEngine conflictEngine = null;

    private Notifier notifier = null;

    public ServerSyncEngine getSyncEngine() {
        return syncEngine;
    }

    public void setSyncEngine(ServerSyncEngine syncEngine) {
        this.syncEngine = syncEngine;
    }

    public ConflictEngine getConflictEngine() {
        return conflictEngine;
    }

    public void setConflictEngine(ConflictEngine conflictEngine) {
        this.conflictEngine = conflictEngine;
    }

    public Notifier getNotifier() {
        return notifier;
    }

    public void setNotifier(Notifier notifier) {
        this.notifier = notifier;
    }

    public void start() {
    }

    public void stop() {
    }

    public void onEvent(Event event) {
        MobileBean mobileBean = (MobileBean) event.getAttribute("mobile-bean");
        if (mobileBean == null) {
            return;
        }
        String action = (String) event.getAttribute("action");
        if (!action.equalsIgnoreCase("create")) {
            return;
        }
        SyncContext context = (SyncContext) ExecutionContext.getInstance().getSyncContext();
        Session session = context.getSession();
        DeviceController deviceController = DeviceController.getInstance();
        String deviceId = session.getDeviceId();
        String channel = session.getChannel();
        String operation = ServerSyncEngine.OPERATION_ADD;
        String oid = Tools.getOid(mobileBean);
        String app = session.getApp();
        log.debug("*************************************");
        log.debug("Bean Added: " + oid);
        log.debug("DeviceId : " + deviceId);
        log.debug("Channel: " + channel);
        log.debug("Operation: " + operation);
        log.debug("App: " + app);
        Device device = deviceController.read(deviceId);
        if (device == null) {
            return;
        }
        Identity registeredUser = device.getIdentity();
        log.debug("User: " + registeredUser.getPrincipal());
        Set<Device> allDevices = deviceController.readByIdentity(registeredUser.getPrincipal());
        if (allDevices == null || allDevices.isEmpty()) {
            return;
        }
        Map<String, Notification> pushNotifications = new HashMap<String, Notification>();
        for (Device local : allDevices) {
            String myDeviceId = local.getIdentifier();
            log.debug("DeviceId: " + myDeviceId);
            if (myDeviceId.equals(deviceId)) {
                continue;
            }
            Set<String> apps = this.conflictEngine.findLiveApps(myDeviceId, channel);
            if (apps == null || apps.isEmpty()) {
                continue;
            }
            for (String subscribedApp : apps) {
                ChangeLogEntry changelogEntry = new ChangeLogEntry();
                changelogEntry.setTarget(myDeviceId);
                changelogEntry.setNodeId(channel);
                changelogEntry.setApp(subscribedApp);
                changelogEntry.setOperation(operation);
                changelogEntry.setRecordId(oid);
                boolean exists = this.syncEngine.changeLogEntryExists(changelogEntry);
                if (exists) {
                    continue;
                }
                List entries = new ArrayList();
                entries.add(changelogEntry);
                this.syncEngine.addChangeLogEntries(myDeviceId, subscribedApp, entries);
                Notification notification = Notification.createSilentSyncNotification(myDeviceId, channel);
                pushNotifications.put(myDeviceId, notification);
            }
        }
        if (pushNotifications.isEmpty()) {
            return;
        }
        Set<String> deviceIds = pushNotifications.keySet();
        for (String id : deviceIds) {
            Notification notification = pushNotifications.get(id);
            log.debug("Notification----------------------------------------------");
            log.debug("Device: " + notification.getMetaDataAsString("device") + ", Channel: " + notification.getMetaDataAsString("service"));
            log.debug("----------------------------------------------");
            this.notifier.process(notification);
        }
        log.debug("*************************************");
    }
}
