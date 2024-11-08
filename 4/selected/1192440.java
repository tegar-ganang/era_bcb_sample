package com;

import java.io.IOException;
import java.util.Date;
import java.util.Random;
import javax.jdo.PersistenceManager;
import javax.servlet.http.*;
import com.google.appengine.api.channel.ChannelService;
import com.google.appengine.api.channel.ChannelServiceFactory;
import com.model.ChannelClient;
import com.model.PMF;

@SuppressWarnings("serial")
public class TokenServlet extends HttpServlet {

    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/plain");
        ChannelService channelService = ChannelServiceFactory.getChannelService();
        Random randomGenerator = new Random();
        String clientid = Integer.toString(randomGenerator.nextInt(100000));
        String token = channelService.createChannel(clientid);
        persistId(clientid);
        resp.getWriter().println(token);
    }

    private void persistId(String clientid) {
        ChannelClient client = new ChannelClient();
        client.setClientId(clientid);
        client.setTimestamp(new Date());
        PersistenceManager pm = PMF.get().getPersistenceManager();
        try {
            pm.makePersistent(client);
        } finally {
            pm.close();
        }
    }
}
