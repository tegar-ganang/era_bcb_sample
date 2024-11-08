package ch.comtools.ssh;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import ch.comtools.ssh.packet.SSHPacketChannelOpen;

/**
 * @author Roger Dudler <roger.dudler@gmail.com>
 * @since 1.0
 * @version $Id$
 */
public class SSHChannel {

    /**
	 * The 'channel type' is a name, as described in [SSH-ARCH] and
     * [SSH-NUMBERS], with similar extension mechanisms.
	 */
    private String type;

    private SSHClient client;

    public SSHChannel(SSHClient client) {
        this.client = client;
    }

    public SSHChannel(SSHClient client, String type) {
        this.client = client;
        this.type = type;
    }

    /**
	 * @throws IOException 
	 * 
	 */
    public void open() throws IOException {
        SSHPacketChannelOpen packet = new SSHPacketChannelOpen();
        packet.type = this.getType();
        this.getClient().getTransportManager().send(packet);
        ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
        int size = this.getChannel().read(buffer);
        buffer.flip();
        System.out.println(size);
    }

    public SSHClient getClient() {
        return client;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    private SocketChannel getChannel() {
        return this.getClient().getConnection().getChannel();
    }
}
