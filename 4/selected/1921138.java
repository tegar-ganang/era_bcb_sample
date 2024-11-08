package telkku;

import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.Application;
import org.jdesktop.application.ApplicationContext;

/**
 *
 * @author Omistaja
 */
public class SettingsData {

    private static ApplicationContext appContext = null;

    private static ResourceMap resourceMap = null;

    public static int getChannelCount() {
        if (appContext == null) {
            appContext = Application.getInstance(TelkkuApp.class).getContext();
        }
        if (resourceMap == null) {
            resourceMap = appContext.getResourceMap(TelkkuView.class);
        }
        return resourceMap.getInteger("ChannelList.channelCount");
    }

    public static String getChannelUrl(int index) {
        if (appContext == null) {
            appContext = Application.getInstance(TelkkuApp.class).getContext();
        }
        if (resourceMap == null) {
            resourceMap = appContext.getResourceMap(TelkkuView.class);
        }
        return resourceMap.getString("ChannelList.channel" + index + ".url");
    }
}
