package net.sf.viwow.nio.net.tcp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.logging.Logger;
import net.sf.viwow.seda.Stage;

public class ClientSocket extends Socket {

    protected Logger logger = Logger.getLogger(ClientSocket.class.getName());

    private Stage connectStage;

    private SelectionKey connectKey;

    private boolean connected;

    public void setConnectStage(Stage stage) {
        connectStage = stage;
    }

    public Stage getConnectStage() {
        return connectStage;
    }

    public void connect() throws IOException {
        if (connected) {
            throw new IllegalStateException("ClientSocket already connected to the server");
        }
        SocketChannel channel = SocketChannel.open();
        channel.configureBlocking(false);
        channel.socket().connect(new InetSocketAddress(getHost(), getPort()));
        setChannel(channel);
        registerKeys();
        connected = true;
    }

    public void disconnect() throws IOException {
        if (!connected) {
            throw new IllegalStateException("ClientSocket has not connected to the server yet");
        }
        unregisterKeys();
        getChannel().close();
        setChannel(null);
        connected = false;
    }

    public void registerKeys() {
        super.registerKeys();
        try {
            connectKey = getChannel().register(getSelector(), SelectionKey.OP_CONNECT);
            connectKey.attach(this);
        } catch (ClosedChannelException e) {
            logger.warning(e.toString());
        }
    }

    public void unregisterKeys() {
        connectKey.cancel();
        super.unregisterKeys();
    }
}
