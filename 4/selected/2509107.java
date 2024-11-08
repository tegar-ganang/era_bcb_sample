package org.dreamspeak.lib.data;

import java.nio.ByteBuffer;
import java.util.HashMap;

public class PlayerList extends HashMap<Integer, Player> {

    private static final long serialVersionUID = 4254365107627101671L;

    int waitingForFrame = -1;

    ByteBuffer reassembler;

    protected final ChannelList channelList;

    /**
	 * The ChannelList this PlayerList belongs to
	 * 
	 * @return
	 */
    public ChannelList getChannelList() {
        return channelList;
    }

    /**
	 * Creates a new PlayerList
	 * 
	 * @param channelList
	 */
    public PlayerList(ChannelList channelList) {
        super(INITIAL_PLAYERLIST_SIZE);
        if (channelList == null) throw new NullPointerException("Argument channelList has to be not null;");
        this.channelList = channelList;
    }

    /**
	 * The initial size of the backing ArrayList. Maybe resized by experience.
	 */
    public static final int INITIAL_PLAYERLIST_SIZE = 64;

    public void addPlayer(Player player) throws IllegalArgumentException, IndexOutOfBoundsException {
        if (get(player.getId()) != null) {
            throw new IllegalArgumentException("Player with id " + player.getId() + " is already listed.");
        }
        super.put(player.getId(), player);
    }
}
