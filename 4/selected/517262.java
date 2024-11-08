package gr.thesis.www;

import gr.model.thesis.EventPublisher;
import gr.model.thesis.Location;
import gr.model.thesis.NewEventForm;
import gr.model.thesis.PMF;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.jdo.PersistenceManager;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextAttributeEvent;
import javax.servlet.ServletContextAttributeListener;
import org.cometd.Bayeux;
import org.cometd.Channel;
import org.cometd.Client;
import dk.itu.infobus.http.IBListener;
import dk.itu.infobus.http.InformationBus;
import dk.itu.infobus.http.ListenerCallback;

public class EventBusEventsListener implements ServletContextAttributeListener {

    Bayeux bayeux = null;

    IBListener ibBTListener;

    IBListener ibGPSListener;

    ServletContext servletContext;

    InformationBus infobus = null;

    boolean configured() {
        return bayeux != null && infobus != null;
    }

    void initialize() {
        if (!configured()) return;
        final Client local = bayeux.newClient("myapp.server.client");
        ListenerCallback callback = new ListenerCallback() {

            public void handle(Map<String, Object> m, IBListener l) throws Exception {
                HashMap<String, Object> n = (HashMap<String, Object>) m;
                if (m.containsKey("blipsystem.id")) {
                    EventPublisher ep = new EventPublisher();
                    ep.publish(n);
                    String currentZone = (String) m.get("zone.current");
                    System.out.println(currentZone);
                    Channel channel = bayeux.getChannel("/blip/zones/" + currentZone, true);
                    channel.publish(local, m, null);
                } else if (m.containsKey("mobileOS_id")) {
                    EventPublisher ep = new EventPublisher();
                    ep.publish(n);
                } else {
                    System.out.println("unknown event type");
                }
            }
        };
        try {
            ibBTListener = infobus.createListener(callback, "listenerBlip", infobus.createToken("blipsystem.id", "blipsystem.itu"));
            ibGPSListener = infobus.createListener(callback, "listenerGps", infobus.createToken("type", "device.GPSpos"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void attributeAdded(ServletContextAttributeEvent evt) {
        String name = evt.getName();
        if (Bayeux.ATTRIBUTE.equals(name)) {
            bayeux = (Bayeux) evt.getValue();
            initialize();
        } else if (InformationBus.ATTRIBUTE.equals(name)) {
            infobus = (InformationBus) evt.getValue();
            initialize();
        }
    }

    @Override
    public void attributeRemoved(ServletContextAttributeEvent evt) {
    }

    @Override
    public void attributeReplaced(ServletContextAttributeEvent evt) {
    }
}
