package clubmixer.commons.plugins.communication;

import com.slychief.clubmixer.commons.IClubmixerProperties;
import com.slychief.clubmixer.logging.ClubmixerLogger;
import java.lang.reflect.Method;
import javax.xml.ws.Endpoint;

/**
 *
 * @author Alexander Schindler
 */
public class PluginCommunicationRuntime {

    private final Registry reg;

    private final Endpoint wsendpoint;

    /**
     * Constructs ...
     *
     *
     *
     * @param host
     * @param port
     */
    public PluginCommunicationRuntime(final String host, final String port) {
        reg = Registry.getInstance();
        CommChannelService service = new CommChannelService();
        String serverURL = "http://" + host + ":" + port + "/" + IClubmixerProperties.PLUGIN_COMMUNICATION_WEBSERVICE_PATH;
        wsendpoint = Endpoint.publish(serverURL, service);
        ClubmixerLogger.info(this, "PluginCommunication channel started on " + serverURL);
    }

    /**
     * Method description
     *
     *
     * @param pluginName
     * @param obj
     */
    public void register(final String pluginName, final RemoteMethodEntry obj) {
        reg.put(pluginName, obj);
    }

    /**
     * Method description
     *
     */
    public void close() {
        wsendpoint.stop();
    }

    /**
     * Method description
     *
     *
     * @return
     */
    public ICommunicationChannel getChannel() {
        return null;
    }
}
