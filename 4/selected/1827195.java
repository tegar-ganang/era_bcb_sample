package de.jochenbrissier.backyard;

import java.io.IOException;
import java.text.ParseException;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.catalina.CometEvent;
import org.apache.catalina.CometProcessor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.persistence.internal.oxm.schema.model.List;
import org.json.JSONObject;

/**
 * This servlet is the communications backend to the jquery plugin
 *<br>
 *set this servelt in your web.xml to use the plugin. 
 *<br>
 *This supports Tomcat 6.0.x
 * 
 * 
 * 
 * 
 * @author jochen brissier
 *
 */
@WebServlet(asyncSupported = true)
public class BackyardTomcatServlet extends HttpServlet implements CometProcessor {

    Log log = LogFactory.getLog(BackyardTomcatServlet.class);

    @Override
    public void init() throws ServletException {
        Backyard.setAlternativeImpl(new Tomcat6(), new JsonModule());
    }

    private void send(HttpServletRequest req, HttpServletResponse resp, JSONObject json) {
    }

    private void listenChannel(HttpServletRequest req, HttpServletResponse resp, JSONObject json, Backyard backyard) {
        String channelName = json.getJSONObject("channel").getString("channel_name");
        backyard.listenToChannel(channelName);
    }

    public void event(CometEvent ev) throws IOException, ServletException {
        log.debug("enter service backyard servlet");
        log.debug("enter BackyardServlet");
        HttpServletRequest req = ev.getHttpServletRequest();
        HttpServletResponse resp = ev.getHttpServletResponse();
        Backyard backyard = new Backyard(req, resp);
        backyard.setServlet(this);
        System.out.println("servlet invoked");
        String data = req.getParameter("data");
        try {
            log.debug("Parsing JSON: " + data);
            JSONObject json = new JSONObject(data);
            String function = json.getString("fn");
            if (function.matches("handshake")) {
                log.debug("Handshake");
                backyard.startAsync(ev);
            }
            if (function.matches("listen")) {
                log.debug("listen");
                listenChannel(req, resp, json, backyard);
                ev.close();
            }
            if (function.matches("send")) {
                log.debug("sendtochannel");
                JSONObject channeldata = json.getJSONObject("channel");
                try {
                    backyard.getChannel(channeldata.getString("channel_name")).sendMessage(json.getString("data"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                ev.close();
            }
        } catch (ParseException e) {
            log.warn(e);
        }
    }
}
