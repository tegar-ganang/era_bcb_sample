package org.encuestame.comet.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.commons.collections.ListUtils;
import org.apache.log4j.Logger;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.cometd.java.annotation.Listener;
import org.cometd.java.annotation.Service;
import org.encuestame.core.util.JSONUtils;
import org.encuestame.utils.web.notification.UtilNotification;

/**
 * Description.
 * @author Picado, Juan juanATencuestame.org
 * @since 12/08/2011
 */
@Named
@Singleton
@Service("streamService")
public class ActivityStreamService extends AbstractCometService {

    private Logger log = Logger.getLogger(this.getClass());

    /**
     *
     */
    @Listener("/service/stream/get")
    public void processStream(final ServerSession remote, final ServerMessage.Mutable message) {
        final Map<String, Object> output = new HashMap<String, Object>();
        try {
            log.debug("ActivityStreamService............");
            final List<UtilNotification> activities = getStreamOperations().retrieveLastNotifications(20, false, null);
            log.debug("not stream SIZE...." + activities.size());
            output.put("stream", JSONUtils.convertObjectToJsonString(activities));
        } catch (Exception e) {
            log.fatal("cometd error: " + e.getMessage());
            output.put("stream", ListUtils.EMPTY_LIST);
        }
        remote.deliver(getServerSession(), message.getChannel(), output, null);
    }
}
