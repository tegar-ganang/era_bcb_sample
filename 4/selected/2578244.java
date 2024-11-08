package net.sourceforge.speedcontrol.util;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.logging.LoggerChannel;

/**
 * Adapter to access Azureus Logger 
 */
public final class LogAdapter {

    private static final String LOG_CHANNEL = "SpeedControl";

    private LoggerChannel loggerChannel;

    public LogAdapter(PluginInterface pluginInterface) {
        this.loggerChannel = pluginInterface.getLogger().getChannel(LOG_CHANNEL);
    }

    public void info(String msg) {
        loggerChannel.log(LoggerChannel.LT_INFORMATION, msg);
    }

    public void warning(String msg) {
        loggerChannel.log(LoggerChannel.LT_WARNING, msg);
    }

    public void error(String msg) {
        loggerChannel.log(LoggerChannel.LT_ERROR, msg);
    }

    public void error(String msg, Throwable throwable) {
        loggerChannel.log(msg, throwable);
    }
}
