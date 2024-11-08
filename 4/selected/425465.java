package ch.comtools.ssh;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import ch.comtools.ssh.datatype.NameList;
import ch.comtools.ssh.packet.SSHPacketKeyExchangeInitialization;

/**
 * Key Exchange.
 * <p>
 * See RFC4253#Section-7
 * </p>
 * @author Roger Dudler <roger.dudler@gmail.com>
 * @since 1.0
 * @version $Id$
 */
public class SSHKeyExchange {

    private SSHClient client;

    private SSHPacketKeyExchangeInitialization serverInitializationPacket;

    private SSHPacketKeyExchangeInitialization clientInitializationPacket;

    private SSHKeyExchangeConfiguration configuration;

    /**
	 * @param channel
	 */
    public SSHKeyExchange(SSHClient client) {
        this.client = client;
        this.clientInitializationPacket = new SSHPacketKeyExchangeInitialization();
    }

    /**
	 * Perform key exchange between client and server as specified in
	 * RFC4253#Section-7.
	 * @throws IOException 
	 */
    public void perform() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
        this.getChannel().read(buffer);
        buffer.flip();
        this.serverInitializationPacket = new SSHPacketKeyExchangeInitialization(buffer);
        System.out.println(this.getServerInitializationPacket());
        this.guessConfiguration();
        this.createClientInitializationPacket();
        this.getClient().getTransportManager().send(this.getClientInitializationPacket());
        buffer = ByteBuffer.allocateDirect(1024);
        int size = this.getChannel().read(buffer);
        buffer.flip();
        System.out.println(size);
    }

    /**
	 * Guess algoritms and other initialization configuration based on 
	 * the server initialization packet.
	 */
    private void guessConfiguration() {
        this.configuration = new SSHKeyExchangeConfiguration();
        for (String s : this.serverInitializationPacket.compressionAlgorithmsClientToServer.getEntries()) {
            if (s.equals(SSHClientConfiguration.getInstance().get("compression.algorithm.c2s"))) {
                this.configuration.setCompressionAlgorithmClientToServer(s);
                break;
            }
        }
        for (String s : this.serverInitializationPacket.compressionAlgorithmsServerToClient.getEntries()) {
            if (s.equals(SSHClientConfiguration.getInstance().get("compression.algorithm.s2c"))) {
                this.configuration.setCompressionAlgorithmServerToClient(s);
                break;
            }
        }
        for (String s : this.serverInitializationPacket.kexAlgorithms.getEntries()) {
            if (s.equals(SSHClientConfiguration.getInstance().get("key.exchange.algorithm"))) {
                this.configuration.setKexAlgorithm(s);
                break;
            }
        }
        for (String s : this.serverInitializationPacket.serverHostKeyAlgorithms.getEntries()) {
            if (s.equals(SSHClientConfiguration.getInstance().get("server.hostkey.algorithm"))) {
                this.configuration.setServerHostKeyAlgorithm(s);
                break;
            }
        }
        for (String s : this.serverInitializationPacket.encryptionAlgorithmsClientToServer.getEntries()) {
            if (s.equals(SSHClientConfiguration.getInstance().get("encryption.algorithm.c2s"))) {
                this.configuration.setEncryptionAlgorithmClientToServer(s);
                break;
            }
        }
        for (String s : this.serverInitializationPacket.encryptionAlgorithmsServerToClient.getEntries()) {
            if (s.equals(SSHClientConfiguration.getInstance().get("encryption.algorithm.s2c"))) {
                this.configuration.setEncryptionAlgorithmServerToClient(s);
                break;
            }
        }
        for (String s : this.serverInitializationPacket.macAlgorithmsClientToServer.getEntries()) {
            if (s.equals(SSHClientConfiguration.getInstance().get("mac.algorithm.c2s"))) {
                this.configuration.setMacAlgorithmClientToServer(s);
                break;
            }
        }
        for (String s : this.serverInitializationPacket.macAlgorithmsServerToClient.getEntries()) {
            if (s.equals(SSHClientConfiguration.getInstance().get("mac.algorithm.s2c"))) {
                this.configuration.setMacAlgorithmServerToClient(s);
                break;
            }
        }
        for (String s : this.serverInitializationPacket.languagesClientToServer.getEntries()) {
            if (s.equals(SSHClientConfiguration.getInstance().get("language.c2s"))) {
                this.configuration.setLanguageClientToServer(s);
                break;
            }
        }
        for (String s : this.serverInitializationPacket.languagesServerToClient.getEntries()) {
            if (s.equals(SSHClientConfiguration.getInstance().get("language.s2c"))) {
                this.configuration.setLanguageServerToClient(s);
                break;
            }
        }
    }

    /**
	 * Create client initialization packet based on previously guessed
	 * configuration.
	 * @return
	 */
    private void createClientInitializationPacket() {
        this.clientInitializationPacket.cookie = this.configuration.getCookie();
        this.clientInitializationPacket.firstKexPacketFollows = this.configuration.isFirstKexPacketFollows();
        this.clientInitializationPacket.compressionAlgorithmsClientToServer = new NameList(this.configuration.getCompressionAlgorithmClientToServer());
        this.clientInitializationPacket.compressionAlgorithmsServerToClient = new NameList(this.configuration.getCompressionAlgorithmServerToClient());
        this.clientInitializationPacket.encryptionAlgorithmsClientToServer = new NameList(this.configuration.getEncryptionAlgorithmClientToServer());
        this.clientInitializationPacket.encryptionAlgorithmsServerToClient = new NameList(this.configuration.getEncryptionAlgorithmServerToClient());
        this.clientInitializationPacket.kexAlgorithms = new NameList(this.configuration.getKexAlgorithm());
        this.clientInitializationPacket.languagesClientToServer = new NameList(this.configuration.getLanguageClientToServer());
        this.clientInitializationPacket.languagesServerToClient = new NameList(this.configuration.getLanguageServerToClient());
        this.clientInitializationPacket.macAlgorithmsClientToServer = new NameList(this.configuration.getMacAlgorithmClientToServer());
        this.clientInitializationPacket.macAlgorithmsServerToClient = new NameList(this.configuration.getMacAlgorithmServerToClient());
        this.clientInitializationPacket.serverHostKeyAlgorithms = new NameList(this.configuration.getServerHostKeyAlgorithm());
    }

    /**
	 * @return
	 */
    public SSHPacketKeyExchangeInitialization getClientInitializationPacket() {
        return clientInitializationPacket;
    }

    /**
	 * @return
	 */
    public SSHPacketKeyExchangeInitialization getServerInitializationPacket() {
        return serverInitializationPacket;
    }

    /**
	 * @return
	 */
    public SSHKeyExchangeConfiguration getConfiguration() {
        return configuration;
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
    public SocketChannel getChannel() {
        return this.getClient().getConnection().getChannel();
    }
}
