package com;

import java.util.List;
import java.util.logging.Logger;
import javax.jdo.PersistenceManager;
import com.google.appengine.api.channel.ChannelFailureException;
import com.google.appengine.api.channel.ChannelMessage;
import com.google.appengine.api.channel.ChannelService;
import com.google.appengine.api.channel.ChannelServiceFactory;
import com.google.appengine.api.taskqueue.DeferredTask;
import com.google.appengine.repackaged.org.json.JSONException;
import com.google.appengine.repackaged.org.json.JSONObject;
import com.model.ChannelClient;
import com.model.PMF;

public class BroadcastToClientsTask implements DeferredTask {

    private static final Logger log = Logger.getLogger(BroadcastToClientsTask.class.getName());

    private String title;

    private String link;

    private String description;

    /**
   * @return the title
   */
    public String getTitle() {
        return title;
    }

    /**
   * @param title the title to set
   */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
   * @return the link
   */
    public String getLink() {
        return link;
    }

    /**
   * @param link the link to set
   */
    public void setLink(String link) {
        this.link = link;
    }

    /**
   * @return the description
   */
    public String getDescription() {
        return description;
    }

    /**
   * @param description the description to set
   */
    public void setDescription(String description) {
        this.description = description;
    }

    BroadcastToClientsTask(String title, String link, String description) {
        this.title = title;
        this.link = link;
        if (description != null) {
            this.description = description;
        }
    }

    public void run() {
        if (this.getTitle() != null && this.getLink() != null) {
            broadcastToClients(this.getTitle(), this.getLink(), this.getDescription());
            log.info("Broadcasted news message to clients");
        }
    }

    private void broadcastToClients(String title, String link, String description) {
        JSONObject jsonMessage = null;
        String query = "select from " + ChannelClient.class.getName();
        PersistenceManager pm = PMF.get().getPersistenceManager();
        List<ChannelClient> ids = (List<ChannelClient>) pm.newQuery(query).execute();
        ChannelService channelService = ChannelServiceFactory.getChannelService();
        for (ChannelClient m : ids) {
            String client = m.getClientId();
            try {
                jsonMessage = new JSONObject();
                jsonMessage.put("title", title);
                jsonMessage.put("link", link);
                jsonMessage.put("description", description);
                System.out.println("sending json stream: " + jsonMessage.toString());
                System.out.println("to client: " + client);
                channelService.sendMessage(new ChannelMessage(client, jsonMessage.toString()));
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (ChannelFailureException e) {
                log.warning("msg: " + e.getMessage());
            }
        }
        pm.close();
    }
}
