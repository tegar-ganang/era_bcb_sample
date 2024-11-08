package navigators.smart.communication.client.netty;

import java.security.PublicKey;
import java.util.concurrent.locks.Lock;
import javax.crypto.Mac;
import org.jboss.netty.channel.Channel;

/**
 *
 * @author Paulo Sousa
 */
public class NettyClientServerSession {

    private Channel channel;

    private Mac macSend;

    private Mac macReceive;

    private int replicaId;

    private PublicKey pKey;

    private Lock lock;

    private int lastMsgReceived;

    public NettyClientServerSession(Channel channel, Mac macSend, Mac macReceive, int replicaId, PublicKey pKey, Lock lock) {
        this.channel = channel;
        this.macSend = macSend;
        this.macReceive = macReceive;
        this.replicaId = replicaId;
        this.pKey = pKey;
        this.lock = lock;
        this.lastMsgReceived = -1;
    }

    public Mac getMacReceive() {
        return macReceive;
    }

    public Mac getMacSend() {
        return macSend;
    }

    public Channel getChannel() {
        return channel;
    }

    public int getReplicaId() {
        return replicaId;
    }

    public PublicKey getPublicKey() {
        return pKey;
    }

    public Lock getLock() {
        return lock;
    }

    public int getLastMsgReceived() {
        return lastMsgReceived;
    }

    public void setLastMsgReceived(int lastMsgReceived_) {
        this.lastMsgReceived = lastMsgReceived_;
    }
}
