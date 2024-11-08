package ru.adv.web.webadmin;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.Cookie;
import org.cometd.Client;
import org.cometd.Message;
import org.cometd.MessageListener;
import org.cometd.client.BayeuxClient;
import org.eclipse.jetty.client.HttpClient;
import ru.adv.logger.TLogger;
import ru.adv.repository.shelladmin.ShellAdmin;
import ru.adv.web.webadmin.cometd.CometdController;

public class WebAdmin extends ShellAdmin {

    private TLogger logger = new TLogger(CometdController.class);

    private BufferedWriter outputWriter = null;

    private HttpClient httpClient;

    private BayeuxClient client;

    private String sessionId;

    private Boolean hasClient = false;

    private StringBuffer msgHistory = new StringBuffer();

    private String userName = "webadmin_" + this.hashCode();

    public static String outputChannel = "/webadmin/output";

    public static String membersChannel = "/webadmin/members";

    public static String privateOutputChannel = "/service/privatechat";

    public static String cookieName = "webadmin";

    public WebAdmin(String hostName, String sessionId) {
        super();
        this.sessionId = sessionId;
        httpClient = new HttpClient();
        logger.info(hostName + "/cometd/");
        client = new BayeuxClient(httpClient, hostName + "/cometd/");
        Cookie cookie = new Cookie(cookieName, "true");
        client.setCookie(cookie);
        client.addListener(new ReceiveListener());
        try {
            httpClient.start();
            client.start();
            join();
        } catch (Exception e) {
            logger.warning("Can't execute cometd client");
            throw new RuntimeException("Can't execute cometd client", e);
        }
    }

    public void join() {
        client.startBatch();
        try {
            client.subscribe(outputChannel);
            client.subscribe(membersChannel);
            client.publish(outputChannel, new Msg().add("user", userName).add("join", Boolean.TRUE).add("chat", ""), String.valueOf(System.currentTimeMillis()));
        } finally {
            client.endBatch();
        }
    }

    public void leave() {
        client.startBatch();
        try {
            client.unsubscribe(outputChannel);
            client.unsubscribe(membersChannel);
            client.publish(outputChannel, new Msg().add("user", userName).add("leave", Boolean.TRUE).add("chat", ""), String.valueOf(System.currentTimeMillis()));
        } finally {
            client.endBatch();
        }
    }

    public void chat(String message) {
        client.publish(outputChannel, new Msg().add("user", userName).add("chat", message), String.valueOf(System.currentTimeMillis()));
    }

    public void chat(String message, String user) {
        client.publish(privateOutputChannel, new Msg().add("user", userName).add("room", outputChannel).add("chat", message).add("peer", user), null);
    }

    public void setOutputWriter(BufferedWriter outputWriter) {
        this.outputWriter = outputWriter;
    }

    private void saveMsg(String msg) {
        msgHistory.append(msg);
    }

    public void print(String msg) {
        if (outputWriter != null) {
            try {
                if (!hasClient) {
                    saveMsg(msg);
                } else {
                    chat(msg, sessionId);
                }
                outputWriter.write(msg);
                outputWriter.flush();
            } catch (IOException e) {
                throw new RuntimeException("It seems outputWriter was closed", e);
            }
        } else {
            throw new RuntimeException("outputWriter is null");
        }
    }

    public void onMessageReceived(Client from, Map<String, Object> message) {
        if ("private".equals(message.get("scope"))) onPrivateMessageReceived(from, message); else onPublicMessageReceived(from, message);
    }

    public void onPublicMessageReceived(Client from, Map<String, Object> message) {
    }

    public void onPrivateMessageReceived(Client from, Map<String, Object> message) {
    }

    public void onUserListRefreshed(Object[] users) {
        for (Object user : users) {
            if (((String) user).equals(this.sessionId) && !hasClient) {
                hasClient = true;
                chat(msgHistory.toString(), sessionId);
            }
        }
    }

    public void close() throws IOException {
        try {
            client.disconnect();
            httpClient.stop();
        } catch (Exception e) {
            throw new RuntimeException("Can't stop cometd client", e);
        }
        super.close();
    }

    class ReceiveListener implements MessageListener {

        public void deliver(Client from, Client to, Message message) {
            if (message.getChannel() != null && membersChannel.equals(message.getChannel())) {
                Object data = message.getData();
                if (data == null) return;
                if (data.getClass().isArray()) onUserListRefreshed((Object[]) data); else if (data instanceof Map) onMessageReceived(from, (Map<String, Object>) data);
            }
        }
    }

    public class Msg extends HashMap<String, Object> {

        Msg add(String name, Object value) {
            put(name, value);
            return this;
        }
    }
}
