package net.sf.viwow.nio.net.tcp;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.logging.Logger;
import net.sf.viwow.seda.Stage;

public class Socket {

    protected Logger logger = Logger.getLogger(Socket.class.getName());

    private SelectionKey readKey;

    private SelectionKey writeKey;

    private Stage readStage;

    private Stage writeStage;

    private SelectableChannel channel;

    private Selector selector;

    private int port;

    private String host;

    public void setReadStage(Stage stage) {
        readStage = stage;
    }

    public Stage getReadStage() {
        return readStage;
    }

    public void setWriteStage(Stage stage) {
        writeStage = stage;
    }

    public Stage getWriteStage() {
        return writeStage;
    }

    public Stage getAcceptStage() {
        return null;
    }

    public Stage getConnectStage() {
        return null;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getHost() {
        return host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    public void setSelector(Selector selector) {
        if (this.selector != null) {
            throw new IllegalStateException("selector already set");
        }
        this.selector = selector;
    }

    public Selector getSelector() {
        return selector;
    }

    public void setChannel(SelectableChannel channel) {
        try {
            channel.configureBlocking(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (this.channel != null) {
            throw new IllegalStateException("channel already set");
        }
        this.channel = channel;
    }

    public SelectableChannel getChannel() {
        return channel;
    }

    public void registerKeys() {
        try {
            readKey = getChannel().register(getSelector(), SelectionKey.OP_READ);
            readKey.attach(this);
            writeKey = getChannel().register(getSelector(), SelectionKey.OP_WRITE);
            writeKey.attach(this);
        } catch (ClosedChannelException e) {
            logger.warning(e.toString());
        }
    }

    public void unregisterKeys() {
        readKey.cancel();
        writeKey.cancel();
    }
}
