package freelands;

import freelands.actor.ActorState;
import freelands.actor.Player;
import freelands.chat.Channel;
import freelands.protocol.ChatChannel;
import freelands.protocol.message.toclient.RawMessage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 * @author michael
 */
public class NetworkClient {

    public Player player;

    public final short id;

    public SocketChannel hisSocket;

    public boolean isLoggedIn = false;

    public boolean isToBeKilled = false;

    public long lastPingRequest;

    public int lastPingValue;

    private Queue<ByteBuffer> incomingmessage;

    private Queue<ByteBuffer> outcomingmessage;

    public long lastHeartbeat = -1;

    public byte[] msgbuffer = null;

    public NetworkClient(short id) {
        this.id = id;
        incomingmessage = new ConcurrentLinkedQueue<ByteBuffer>();
        outcomingmessage = new ConcurrentLinkedQueue<ByteBuffer>();
    }

    public SocketChannel getChannel() {
        return hisSocket;
    }

    private void sendPacket(ByteBuffer msg) {
        if (isToBeKilled) {
            return;
        }
        msg.flip();
        try {
            msg.rewind();
            hisSocket.write(msg);
        } catch (Exception e) {
            Main.preferences.LOGGER.fine("In Client::SendPacket -> killing client!");
            e.printStackTrace();
            if (player != null) {
                if (CharDatabase.charExists(player.getContent().name)) {
                    CharDatabase.saveChar(player.getContent(), player.getInventory());
                }
                player.setState(ActorState.dead);
                player.clean();
                player.needToRemove = true;
            }
            isToBeKilled = true;
            try {
                hisSocket.close();
            } catch (IOException e2) {
                Main.preferences.LOGGER.fine("In Client::SendPacket -> can't close socket!");
            }
        }
    }

    public void addPacketTosend(ByteBuffer msg) {
        incomingmessage.add(msg);
    }

    public void sendCurrentsMessages() {
        ByteBuffer msg = incomingmessage.poll();
        while (msg != null) {
            sendPacket(msg);
            msg = incomingmessage.poll();
        }
    }

    public void loginOk() {
        this.isLoggedIn = true;
        ByteBuffer msg = RawMessage.loginOkMessage();
        sendPacket(msg);
    }

    public void loginNotOk(String str) {
        this.isLoggedIn = false;
        ByteBuffer msg = RawMessage.loginNotOkMessage(str);
        sendPacket(msg);
    }

    void createPlayer() {
        this.player = new Player(id, outcomingmessage, incomingmessage);
    }

    void delegate(ByteBuffer msg) {
        this.outcomingmessage.add(msg);
    }

    public void addMessageToPlayer(ByteBuffer bb) {
        this.outcomingmessage.add(bb);
    }

    public ChatChannel getChatChannel(int number) {
        int[] channels = this.player.getContent().channels;
        if (number == channels[0]) return ChatChannel.CHANNEL1; else if (number == channels[1]) return ChatChannel.CHANNEL2; else if (number == channels[2]) return ChatChannel.CHANNEL3; else if (number == channels[3]) return ChatChannel.CHANNEL4; else if (number == channels[4]) return ChatChannel.CHANNEL5;
        return ChatChannel.LOCAL;
    }

    public int getActiveChatChannel() {
        return this.player.getContent().channels[this.player.getContent().activecc];
    }

    public boolean isInChannel(int channel) {
        for (int i : this.player.getContent().channels) {
            if (i == channel) return true;
        }
        return false;
    }

    public void setActiveChannel(byte n) {
        ChatChannel cc = ChatChannel.get(n);
        int[] channels = this.player.getContent().channels;
        if (cc == null) return;
        switch(cc) {
            case CHANNEL1:
                if (channels[0] > 0) this.player.getContent().activecc = 0;
                break;
            case CHANNEL2:
                if (channels[1] > 0) this.player.getContent().activecc = 1;
                break;
            case CHANNEL3:
                if (channels[2] > 0) this.player.getContent().activecc = 2;
                break;
            case CHANNEL4:
                if (channels[3] > 0) this.player.getContent().activecc = 3;
                break;
            case CHANNEL5:
                if (channels[4] > 0) this.player.getContent().activecc = 4;
                break;
        }
    }

    void connectToChannels() {
        int[] channels = this.player.getContent().channels;
        Channel chan;
        for (int c : channels) {
            if (c > 0) {
                chan = Channel.getChannel(c);
                chan.addClientInSilent(this);
            }
        }
    }

    public void removeChannel(int number) {
        for (int i = 0; i < this.player.getContent().channels.length; i++) {
            if (this.player.getContent().channels[i] == number) {
                this.player.getContent().channels[i] = 0;
                return;
            }
        }
    }
}
