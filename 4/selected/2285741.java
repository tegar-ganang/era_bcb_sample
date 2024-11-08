package de.jochenbrissier.backyard.web;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.catalina.CometEvent;
import org.apache.catalina.CometProcessor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import de.jochenbrissier.backyard.core.Backyard;
import de.jochenbrissier.backyard.util.ServerDedec;

/**
 * This servlet is the communications backend to the jquery plugin <br>
 *set this servelt in your web.xml to use the plugin. <br>
 *This supports Tomcat 6.0.x
 * 
 * 
 * 
 * 
 * @author jochen brissier
 * 
 */
public class BackyardTomcatServlet extends HttpServlet implements CometProcessor {

    Log log = LogFactory.getLog(BackyardTomcatServlet.class);

    @Override
    public void init() throws ServletException {
        Backyard.autoDedectImpl(this);
    }

    private void send(HttpServletRequest req, HttpServletResponse resp, JSONObject json) {
    }

    private HttpServletRequest req;

    private HttpServletResponse res;

    @Override
    public void service(final HttpServletRequest req, final HttpServletResponse res) throws ServletException, IOException {
        if (ServerDedec.isServer(ServerDedec.TOMCAT6, this)) {
            return;
        }
        req.setAttribute("de.jochenbrissier.byw.comet", "noneTomcat");
        event(new CometEvent() {

            @Override
            public void setTimeout(int arg0) throws IOException, ServletException, UnsupportedOperationException {
            }

            @Override
            public HttpServletResponse getHttpServletResponse() {
                return res;
            }

            @Override
            public HttpServletRequest getHttpServletRequest() {
                return req;
            }

            @Override
            public EventType getEventType() {
                return EventType.BEGIN;
            }

            @Override
            public EventSubType getEventSubType() {
                return null;
            }

            @Override
            public void close() throws IOException {
            }
        });
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
        String data = req.getParameter("data");
        try {
            log.debug("Parsing JSON: " + data);
            JSONObject json = new JSONObject(data);
            String function = json.getString("fn");
            if (function.matches("handshake")) {
                log.debug("Handshake");
                PrintWriter pw = ev.getHttpServletResponse().getWriter();
                pw.print("{\"status\":\"OK\"}");
                pw.flush();
                pw.close();
                return;
            }
            if (function.matches("comet")) {
                log.debug("comet");
                if (req.getAttribute("de.jochenbrissier.byw.comet") != null && req.getAttribute("de.jochenbrissier.byw.comet").equals("nonTomcat")) {
                    backyard.startAsync();
                } else {
                    backyard.startAsync(ev);
                }
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
        } catch (Exception e) {
            log.warn(e);
        }
    }
}
