package system;

import java.util.List;
import java.util.Vector;
import android.content.Context;
import org.openmobster.core.mobileCloud.api.ui.framework.AppConfig;
import org.openmobster.core.mobileCloud.android.configuration.Configuration;
import org.openmobster.core.mobileCloud.android.module.bus.Bus;
import org.openmobster.core.mobileCloud.android.module.bus.BusException;
import org.openmobster.core.mobileCloud.android.module.bus.Invocation;
import org.openmobster.core.mobileCloud.android.service.Registry;

/**
 * @author openmobster@gmail
 *
 */
public final class CometUtil {

    public static boolean subscribeChannels() throws BusException {
        boolean wasChannelBootupStarted = false;
        Context context = Registry.getActiveInstance().getContext();
        Configuration configuration = Configuration.getInstance(context);
        if (!configuration.isActive()) {
            return false;
        }
        Vector channels = AppConfig.getInstance().getChannels();
        boolean newAdded = false;
        if (channels != null && !channels.isEmpty()) {
            int size = channels.size();
            for (int i = 0; i < size; i++) {
                String channel = (String) channels.get(i);
                boolean cour = configuration.addMyChannel(channel);
                if (!newAdded && cour) {
                    newAdded = true;
                }
            }
            configuration.save(context);
            if (newAdded) {
                CometUtil.performChannelBootup(configuration);
                wasChannelBootupStarted = true;
            }
        }
        return wasChannelBootupStarted;
    }

    private static synchronized void performChannelBootup(Configuration configuration) {
        Thread thread = new Thread(new Runnable() {

            public void run() {
                try {
                    Invocation invocation = new Invocation("org.openmobster.core.mobileCloud.android.invocation.ChannelBootupHandler");
                    Bus.getInstance().invokeService(invocation);
                } catch (Exception be) {
                }
            }
        });
        thread.start();
    }
}
