package org.light.portal.core.event;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.light.portal.logger.Logger;
import org.light.portal.logger.LoggerFactory;
import org.light.portal.user.model.User;

/**
 * 
 * @author Jianmin Liu
 **/
public class EventHandler {

    private static Logger logger = LoggerFactory.getLogger(EventHandler.class);

    public static void invoke(Object... args) {
        String eventName = (String) args[0];
        int len = args.length - 1;
        Object[] eventArgs = new Object[len];
        for (int i = 0; i < len; i++) {
            eventArgs[i] = args[i + 1];
        }
        List<Event> events = EventFactory.getInstance().getEvents(eventName);
        if (events != null) {
            for (Event event : events) {
                try {
                    event.execute(eventArgs);
                } catch (Exception ex) {
                    logger.error(ex);
                }
            }
        }
    }

    public static void invoke(String eventName, HttpServletRequest request, HttpServletResponse response) {
        List<Event> events = EventFactory.getInstance().getEvents(eventName);
        if (events != null) {
            for (Event event : events) {
                try {
                    event.execute(request, response);
                } catch (Exception ex) {
                    logger.error(ex);
                }
            }
        }
    }
}
