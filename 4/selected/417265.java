package org.encuestame.comet.services;

import java.util.HashMap;
import java.util.Map;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.log4j.Logger;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.cometd.java.annotation.Listener;
import org.cometd.java.annotation.Service;
import org.encuestame.persistence.domain.security.UserAccount;
import org.encuestame.persistence.exception.EnMeNoResultsFoundException;

/**
 * Notification comet service.
 * @author Picado, Juan juanATencuestame.org
 * @since Mar 4, 2011
 */
@Named
@Singleton
@Service("notificationService")
public class NotificationCometService extends AbstractCometService {

    private Logger log = Logger.getLogger(this.getClass());

    /**
     * Notification services response.
     * @param remote
     * @param message
     */
    @Listener("/service/notification/status")
    public void processNotification(final ServerSession remote, final ServerMessage.Mutable message) {
        final Map<String, Object> input = message.getDataAsMap();
        final Map<String, Object> output = new HashMap<String, Object>();
        UserAccount userAccount;
        try {
            userAccount = getByUsername(getUserPrincipalUsername());
            if (userAccount != null) {
                final Long totalNot = getNotificationDao().retrieveTotalNotificationStatus(userAccount.getAccount());
                log.debug("totalNot " + totalNot);
                final Long totalNewNot = getNotificationDao().retrieveTotalNotReadedNotificationStatus(userAccount.getAccount());
                log.debug("totalNewNot " + totalNewNot);
                output.put("totalNot", totalNot);
                output.put("totalNewNot", totalNewNot);
                log.debug(totalNewNot + " NEW of " + totalNot + " total not");
            } else {
                output.put("totalNot", 0);
                output.put("totalNewNot", 0);
            }
        } catch (EnMeNoResultsFoundException e) {
            output.put("totalNot", 0);
            output.put("totalNewNot", 0);
            log.fatal("cometd: username invalid");
        }
        remote.deliver(getServerSession(), message.getChannel(), output, null);
    }
}
