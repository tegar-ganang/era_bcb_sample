package ch.comtools.ssh;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import ch.comtools.ssh.packet.Packet;

/**
 * Protocol Version Exchange.
 * 
 * @author Roger Dudler <roger.dudler@gmail.com>
 * @since 1.0
 * @version $Id$
 */
public class SSHProtocolVersionExchange {

    private SSHClient client;

    /**
	 * 
	 */
    public SSHProtocolVersionExchange() {
    }

    /**
	 * @param client
	 */
    public SSHProtocolVersionExchange(SSHClient client) {
        this.client = client;
    }

    /**
	 * Perform protocol version exchange as specified in RFC4253#Section-4.2.
	 * <code>SSH-protoversion-softwareversion SP comments CR LF</code>
	 * @throws IOException 
	 */
    public void perform() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocateDirect(512);
        this.getChannel().read(buffer);
        buffer.flip();
        String serverVersion = this.getClient().getDecoder().decode(buffer).toString();
        System.out.println(serverVersion);
        Packet packet = new Packet(1024);
        packet.put((SSHClient.ID + "\r\n").getBytes());
        this.getClient().getTransportManager().send(packet);
    }

    /**
	 * @return
	 */
    public SSHClient getClient() {
        return client;
    }

    /**
	 * @return
	 */
    private SocketChannel getChannel() {
        return this.getClient().getConnection().getChannel();
    }
}
