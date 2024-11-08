package org.openmobster.core.synchronizer.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.List;
import org.apache.log4j.Logger;
import org.openmobster.core.common.event.Event;
import org.openmobster.core.common.event.EventListener;
import org.openmobster.core.push.notification.Notification;
import org.openmobster.core.push.notification.Notifier;
import org.openmobster.core.synchronizer.server.engine.ChangeLogEntry;
import org.openmobster.core.synchronizer.server.engine.ConflictEngine;
import org.openmobster.core.synchronizer.server.engine.ConflictEntry;
import org.openmobster.core.synchronizer.server.engine.Tools;
import org.openmobster.cloud.api.sync.MobileBean;
import org.openmobster.cloud.api.ExecutionContext;
import org.openmobster.core.synchronizer.server.SyncContext;
import org.openmobster.core.synchronizer.server.Session;
import org.openmobster.core.synchronizer.server.engine.ServerSyncEngine;

/**
 *
 * @author openmobster@gmail.com
 */
public class DeleteBeanEventListener implements EventListener {

    private static Logger log = Logger.getLogger(DeleteBeanEventListener.class);

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
        if (!action.equalsIgnoreCase("delete")) {
            return;
        }
        SyncContext context = (SyncContext) ExecutionContext.getInstance().getSyncContext();
        Session session = context.getSession();
        String deviceId = session.getDeviceId();
        String channel = session.getChannel();
        String operation = ServerSyncEngine.OPERATION_DELETE;
        String oid = Tools.getOid(mobileBean);
        String app = session.getApp();
        log.debug("*************************************");
        log.debug("Bean Deleted: " + oid);
        log.debug("DeviceId : " + deviceId);
        log.debug("Channel: " + channel);
        log.debug("Operation: " + operation);
        log.debug("App: " + app);
        List<ConflictEntry> liveEntries = this.conflictEngine.findLiveEntries(channel, oid);
        if (liveEntries == null || liveEntries.isEmpty()) {
            return;
        }
        Map<String, Notification> pushNotifications = new HashMap<String, Notification>();
        for (ConflictEntry entry : liveEntries) {
            if (entry.getDeviceId().equals(deviceId) && entry.getApp().equals(app) && entry.getChannel().equals(channel) && entry.getOid().equals(oid)) {
                continue;
            }
            ChangeLogEntry changelogEntry = new ChangeLogEntry();
            changelogEntry.setTarget(entry.getDeviceId());
            changelogEntry.setNodeId(entry.getChannel());
            changelogEntry.setApp(entry.getApp());
            changelogEntry.setOperation(operation);
            changelogEntry.setRecordId(entry.getOid());
            boolean exists = this.syncEngine.changeLogEntryExists(changelogEntry);
            if (exists) {
                continue;
            }
            List entries = new ArrayList();
            entries.add(changelogEntry);
            this.syncEngine.addChangeLogEntries(entry.getDeviceId(), entry.getApp(), entries);
            Notification notification = Notification.createSilentSyncNotification(entry.getDeviceId(), channel);
            pushNotifications.put(entry.getDeviceId(), notification);
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
