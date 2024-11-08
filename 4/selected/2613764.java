package org.activision.net.codec;

import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.activision.io.OutStream;
import org.activision.model.player.Player;

public class ConnectionHandler {

    public ConnectionHandler(Channel channel) {
        this.channel = channel;
    }

    private transient Channel channel;

    private transient byte ConnectionStage;

    private transient byte NameHash;

    private transient long SessionKey;

    private transient Player player;

    private transient byte displayMode;

    public Channel getChannel() {
        return channel;
    }

    public ChannelFuture write(OutStream outStream) {
        if (channel != null && outStream.offset() > 0 && channel.isConnected()) {
            return channel.write(ChannelBuffers.copiedBuffer(outStream.buffer(), 0, outStream.offset()));
        }
        return null;
    }

    public void setConnectionStage(byte connectionStage) {
        ConnectionStage = connectionStage;
    }

    public byte getConnectionStage() {
        return ConnectionStage;
    }

    public void setNameHash(byte nameHash) {
        NameHash = nameHash;
    }

    public byte getNameHash() {
        return NameHash;
    }

    public void setSessionKey(long sessionKey) {
        SessionKey = sessionKey;
    }

    public long getSessionKey() {
        return SessionKey;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    public boolean isDisconnected() {
        return !getChannel().isConnected();
    }

    public int getDisplayMode() {
        return displayMode;
    }

    public void setDisplayMode(int mode) {
        this.displayMode = (byte) mode;
    }
}
