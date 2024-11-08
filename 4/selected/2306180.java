package org.openmobster.core.mobileCloud.android_native.framework;

import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.Vector;
import org.openmobster.android.api.sync.MobileBean;
import org.openmobster.core.mobileCloud.android.errors.ErrorHandler;
import org.openmobster.core.mobileCloud.android.errors.SystemException;
import org.openmobster.core.mobileCloud.android.module.bus.Bus;
import org.openmobster.core.mobileCloud.android.module.bus.Invocation;
import org.openmobster.core.mobileCloud.android.module.bus.SyncInvocation;
import org.openmobster.core.mobileCloud.api.ui.framework.AppConfig;
import system.CometUtil;

/**
 *
 * @author openmobster@gmail.com
 */
public final class BackgroundSync extends TimerTask {

    public void run() {
        try {
            CometUtil.subscribeChannels();
            Invocation invocation = new Invocation("org.openmobster.core.mobileCloud.android.invocation.ChannelBootupHandler");
            invocation.setValue("push-restart-cancel", "" + Boolean.FALSE);
            Bus.getInstance().invokeService(invocation);
            List<String> channelsToSync = this.findTwoWaySyncChannels();
            if (channelsToSync != null && !channelsToSync.isEmpty()) {
                for (String channel : channelsToSync) {
                    SyncInvocation syncInvocation = new SyncInvocation("org.openmobster.core.mobileCloud.android.invocation.SyncInvocationHandler", SyncInvocation.twoWay, channel);
                    syncInvocation.deactivateBackgroundSync();
                    Bus.getInstance().invokeService(syncInvocation);
                }
            }
            SyncInvocation syncInvocation = new SyncInvocation("org.openmobster.core.mobileCloud.android.invocation.SyncInvocationHandler", SyncInvocation.proxySync);
            syncInvocation.deactivateBackgroundSync();
            Bus.getInstance().invokeService(syncInvocation);
        } catch (Throwable t) {
            SystemException syse = new SystemException(this.getClass().getName(), "run", new Object[] { "Exception: " + t.toString(), "Message: " + t.getMessage() });
            ErrorHandler.getInstance().handle(syse);
        } finally {
            this.cancel();
        }
    }

    private List<String> findTwoWaySyncChannels() {
        List<String> channelsToSync = new ArrayList<String>();
        AppConfig appConfig = AppConfig.getInstance();
        Vector appChannels = appConfig.getChannels();
        if (appChannels != null) {
            int size = appChannels.size();
            for (int i = 0; i < size; i++) {
                String channel = (String) appChannels.get(i);
                if (MobileBean.isBooted(channel)) {
                    channelsToSync.add(channel);
                }
            }
        }
        return channelsToSync;
    }
}
