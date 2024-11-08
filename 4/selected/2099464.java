package chatserver.model;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javolution.util.FastMap;
import chatserver.model.channel.Channel;
import chatserver.network.netty.handler.ClientChannelHandler;

public class ChatClient {

    /**
	* Id of chat client (player id)
	*/
    private int clientId;

    /**
	* Identifier used when sending message
	*/
    private byte[] identifier;

    /**
	* Token used during auth with GS
	*/
    private byte[] token;

    /**
	* Channel handler of chat client
	*/
    private ClientChannelHandler channelHandler;

    /**
	* Map with all connected channels<br>
	* Only one channel of specific type can be added
	*/
    private Map<ChannelType, Channel> channelsList = new FastMap<ChannelType, Channel>();

    /**
	* Incremented during each new channel request
	*/
    private AtomicInteger channelIndex = new AtomicInteger(1);

    /**
	* 
	* @param clientId
	* @param token
	* @param identifier
	*/
    public ChatClient(int clientId, byte[] token) {
        this.clientId = clientId;
        this.token = token;
    }

    /**
	* 
	* @param channel
	*/
    public void addChannel(Channel channel) {
        channelsList.put(channel.getChannelType(), channel);
    }

    /**
	* 
	* @param channel
	*/
    public boolean isInChannel(Channel channel) {
        return channelsList.containsKey(channel.getChannelType());
    }

    /**
	* @return the clientId
	*/
    public int getClientId() {
        return clientId;
    }

    /**
	* @return the token
	*/
    public byte[] getToken() {
        return token;
    }

    /**
	* @return the identifier
	*/
    public byte[] getIdentifier() {
        return identifier;
    }

    /**
	* @return the channelHandler
	*/
    public ClientChannelHandler getChannelHandler() {
        return channelHandler;
    }

    /**
	* @param channelHandler
	*            the channelHandler to set
	*/
    public void setChannelHandler(ClientChannelHandler channelHandler) {
        this.channelHandler = channelHandler;
    }

    /**
	* @param identifier
	*            the identifier to set
	*/
    public void setIdentifier(byte[] identifier) {
        this.identifier = identifier;
    }

    /**
	* 
	* @return
	*/
    public int nextIndex() {
        return channelIndex.incrementAndGet();
    }
}
