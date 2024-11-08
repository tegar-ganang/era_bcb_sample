package n2hell.xmpp;

import java.util.HashMap;
import n2hell.config.XmppConfig;
import n2hell.http.JSONRPCResult;
import n2hell.http.JsonRpcService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.FromContainsFilter;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.OrFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.json.JSONObject;

public class XmppRouter extends Thread {

    private final Log log = LogFactory.getLog(XmppRouter.class);

    private final PacketCollector collector;

    private final JsonRpcService rpcService;

    private final ChatManager chatManager;

    private static final String REQUEST = "jsonRequest";

    private static final String RESPONSE = "jsonResponse";

    private static final String REQUEST_KEY = "jsonRequestObjectKey";

    private static final String RESPONSE_ID = "responseId";

    private static final HashMap<String, String> responses = new HashMap<String, String>();

    private final XmppConfig config;

    private final String from;

    private XMPPConnection connection;

    public XmppRouter(XmppConfig config, JsonRpcService rpcService, boolean server) throws XMPPException {
        this.config = config;
        this.rpcService = rpcService;
        ConnectionConfiguration xmppConfig = new ConnectionConfiguration(config.getHost(), config.getPort());
        if (config.getProxy().getEnabled()) xmppConfig.setSocketFactory(new ProxySocketFactory(config.getProxy()));
        connection = new XMPPConnection(xmppConfig);
        PacketFilter filter = new AndFilter(new OrFilter(new MessageTypeFilter(Message.Type.chat), new MessageTypeFilter(Message.Type.normal)), new FromContainsFilter(config.getAcceptFrom()));
        connection.connect();
        collector = connection.createPacketCollector(filter);
        connection.login(config.getUser().getName(), config.getUser().getPassword(), config.getUser().getResource());
        from = connection.getUser();
        chatManager = connection.getChatManager();
        if (server) start();
    }

    public void run() {
        HashMap<String, Chat> chats = new HashMap<String, Chat>();
        while (!isInterrupted()) {
            Message request = (Message) collector.nextResult();
            String json = (String) request.getProperty(REQUEST);
            String jsonRequestObjectKey = (String) request.getProperty(REQUEST_KEY);
            Message response = new Message();
            response.setProperty(RESPONSE_ID, request.getPacketID());
            if (json != null) {
                try {
                    JSONObject jsonReq = new JSONObject(json);
                    JSONRPCResult res = rpcService.call(jsonReq, jsonRequestObjectKey);
                    response.setProperty(RESPONSE, res.toString());
                } catch (Throwable e) {
                    response.setProperty(RESPONSE, new JSONRPCResult(JSONRPCResult.CODE_REMOTE_EXCEPTION, 0, e).toString());
                } finally {
                    try {
                        Chat chat = null;
                        if (chats.containsKey(request.getFrom())) chat = chats.get(request.getFrom()); else {
                            chat = chatManager.createChat(request.getFrom(), null);
                            chats.put(request.getFrom(), chat);
                        }
                        chat.sendMessage(response);
                    } catch (XMPPException e) {
                        log.error(e.getMessage(), e);
                    }
                }
            }
        }
    }

    private String getResponse(String requestId) throws XMPPException {
        if (responses.containsKey(requestId)) {
            String result = responses.get(requestId);
            responses.remove(requestId);
            return result;
        }
        do {
            Message response = (Message) collector.nextResult(config.getTimeout());
            if (response == null) {
                throw new XMPPException("XMPP response timeout");
            }
            String id = (String) response.getProperty(RESPONSE_ID);
            if (requestId.equals(id)) return (String) response.getProperty(RESPONSE); else responses.put(requestId, (String) response.getProperty(RESPONSE));
        } while (true);
    }

    public synchronized String call(String json, String objectKey) throws XMPPException {
        String id = new Double(Math.random()).toString();
        Message request = new Message();
        request.setPacketID(id);
        request.setProperty(REQUEST, json);
        request.setProperty(REQUEST_KEY, objectKey);
        request.setFrom(from);
        Chat chat = chatManager.createChat(config.getProxyJid(), null);
        chat.sendMessage(request);
        return getResponse(id);
    }

    public void disconnect() {
        connection.disconnect();
    }
}
