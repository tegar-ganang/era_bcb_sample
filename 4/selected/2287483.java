package freelands.chat;

import freelands.NetworkClient;
import freelands.Preferences;
import freelands.protocol.ChatChannel;
import freelands.protocol.TextColor;
import freelands.protocol.message.toclient.RawMessage;
import java.util.HashMap;

/**
 *
 * @author michael
 */
public class Channel {

    private final HashMap<Short, NetworkClient> clients;

    public final int number;

    public Channel(int number) {
        assert (number > 0);
        this.number = number;
        clients = new HashMap<Short, NetworkClient>();
    }

    public void sendAtAll(String message) {
        for (NetworkClient nc : clients.values()) {
            nc.addPacketTosend(RawMessage.rawTextMessage(nc.getChatChannel(number), message));
        }
    }

    public void sendAtAll(String message, TextColor tc) {
        for (NetworkClient nc : clients.values()) {
            nc.addPacketTosend(RawMessage.rawTextMessage(nc.getChatChannel(number), tc, message));
        }
    }

    public boolean addClient(NetworkClient nc) {
        if (addClientInSilent(nc)) {
            nc.addPacketTosend(RawMessage.rawTextMessage(ChatChannel.SERVER, TextColor.c_lbound, "Welcome on channel " + number));
            return true;
        } else {
            nc.addPacketTosend(RawMessage.rawTextMessage(ChatChannel.SERVER, TextColor.c_lbound, "you can't join this channel, close an other before"));
            return false;
        }
    }

    public boolean addClientInSilent(NetworkClient nc) {
        for (byte i = 0; i < Preferences.MAXACTIVECHANNELS; i++) {
            if (nc.player.getContent().channels[i] == number) {
                nc.player.getContent().activecc = i;
                this.clients.put(nc.id, nc);
                return true;
            }
        }
        for (byte i = 0; i < Preferences.MAXACTIVECHANNELS; i++) {
            if (nc.player.getContent().channels[i] < 0) {
                nc.player.getContent().channels[i] = number;
                this.clients.put(nc.id, nc);
                nc.player.getContent().activecc = i;
                return true;
            }
        }
        if (clients.isEmpty()) {
            channels.remove(number);
        }
        return false;
    }

    public boolean removeClient(NetworkClient nc) {
        boolean result = this.clients.remove(nc.id) != null;
        if (result) {
            nc.removeChannel(this.number);
            nc.addPacketTosend(RawMessage.rawTextMessage(ChatChannel.SERVER, TextColor.c_lbound, "You leave the channel " + number));
            if (clients.isEmpty()) {
                channels.remove(number);
            }
        }
        return result;
    }

    private static final HashMap<Integer, Channel> channels = new HashMap<Integer, Channel>();

    public static Channel getChannel(int number) {
        Channel c = channels.get(number);
        if (c == null) {
            c = new Channel(number);
            channels.put(number, c);
        }
        return c;
    }

    public int getConnected() {
        return this.clients.size();
    }
}
