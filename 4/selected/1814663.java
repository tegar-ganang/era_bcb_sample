package net.sf.appia.jgcs.protocols.services;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Event;
import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;
import net.sf.appia.core.events.SendableEvent;
import net.sf.appia.jgcs.protocols.top.JGCSGroupEvent;
import net.sf.appia.jgcs.protocols.top.JGCSSendEvent;
import net.sf.appia.jgcs.protocols.top.JGCSSendableEvent;
import net.sf.appia.protocols.common.ServiceEvent;

/**
 * This class defines a DefaultServiceSession
 * 
 * @author <a href="mailto:nunomrc@di.fc.ul.pt">Nuno Carvalho</a>
 * @version 1.0
 */
public class DefaultServiceSession extends Session {

    /**
     * Creates a new DefaultServiceSession.
     * @param layer
     */
    public DefaultServiceSession(Layer layer) {
        super(layer);
    }

    public void handle(Event event) {
        try {
            event.go();
            if (isAccepted(event)) {
                new ServiceEvent(event.getChannel(), Direction.UP, this, ((SendableEvent) event).getMessage()).go();
            }
        } catch (AppiaEventException e) {
            e.printStackTrace();
        }
    }

    private boolean isAccepted(Event e) {
        return (e instanceof JGCSSendableEvent || e instanceof JGCSSendEvent || e instanceof JGCSGroupEvent) && (e.getDir() == Direction.UP);
    }
}
