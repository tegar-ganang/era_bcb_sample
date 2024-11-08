package ch.comtools.ssh.transport.impl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import ch.comtools.ssh.SSHClient;
import ch.comtools.ssh.authentication.HMACSHA1;
import ch.comtools.ssh.compression.Compressor;
import ch.comtools.ssh.packet.Packet;
import ch.comtools.ssh.packet.SSHPacket;
import ch.comtools.ssh.transport.TransportManager;

/**
 * @author Roger Dudler <roger.dudler@gmail.com>
 * @version $Id$
 */
public class DefaultTransportManager implements TransportManager {

    private SSHClient client;

    public DefaultTransportManager(SSHClient client) {
        this.client = client;
    }

    private int sequenceNumber = 0;

    /**
	 * 
	 */
    private Compressor compressor;

    /**
	 * @return
	 */
    public Compressor getCompressor() {
        return compressor;
    }

    /**
	 * @param compressor
	 */
    public void setCompressor(Compressor compressor) {
        this.compressor = compressor;
    }

    /**
	 * Receives a packet from server.
	 * @param packet
	 */
    public void receive(SSHPacket packet) {
    }

    /**
	 * Send a packet to server.
	 * @param packet
	 * @throws IOException 
	 */
    public void send(Packet packet) throws IOException {
        if (packet instanceof SSHPacket) {
            System.out.println("Sending SSH packet...");
            String macKey = "";
            HMACSHA1 mac = new HMACSHA1();
            byte[] code = mac.compute(macKey, this.getSequenceNumber(), (SSHPacket) packet);
            ((SSHPacket) packet).mac = code;
            if (this.getCompressor() != null) {
                packet = (SSHPacket) this.getCompressor().compress(packet);
            }
            ByteBuffer data = packet.getData();
            System.out.println(packet);
            this.getChannel().write(data);
            this.sequenceNumber++;
        } else {
            System.out.println("Sending plain packet...");
            ByteBuffer data = packet.getData();
            System.out.println(packet);
            this.getChannel().write(data);
        }
    }

    /**
	 * @return
	 */
    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public SSHClient getClient() {
        return client;
    }

    public SocketChannel getChannel() {
        return this.getClient().getConnection().getChannel();
    }
}
