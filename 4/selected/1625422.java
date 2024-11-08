package net.sf.viwow.nio.net.tcp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.logging.Logger;
import net.sf.viwow.seda.Stage;

public class ServerSocket extends Socket {

    private Stage acceptStage;

    protected Logger logger = Logger.getLogger(ServerSocket.class.getName());

    public static final String DEFAULT_HOST = "localhost";

    private SelectionKey acceptKey;

    private enum Status {

        STOPPED, PAUSED, STARTED
    }

    ;

    private Status status = Status.STOPPED;

    public Stage getAcceptStage() {
        return acceptStage;
    }

    public void setAcceptStage(Stage stage) {
        acceptStage = stage;
    }

    public String getHost() {
        String host = super.getHost();
        if (host == null) {
            host = DEFAULT_HOST;
        }
        return host;
    }

    public void start() throws IOException {
        if (status != Status.STOPPED) {
            throw new IllegalStateException("ServerSocket is not in stopped state");
        }
        ServerSocketChannel channel = ServerSocketChannel.open();
        setChannel(channel);
        channel.configureBlocking(false);
        channel.socket().bind(new InetSocketAddress(getHost(), getPort()));
        registerKeys();
        status = Status.STARTED;
    }

    public void pause() {
        if (status != Status.STARTED) {
            throw new IllegalStateException("ServerSocket is not in started state");
        }
        unregisterKeys();
        status = Status.PAUSED;
    }

    public void resume() {
        if (status != Status.PAUSED) {
            throw new IllegalStateException("ServerSocket is not in paused state");
        }
        registerKeys();
        status = Status.STARTED;
    }

    public void stop() throws IOException {
        if (status == Status.STARTED) {
            pause();
        }
        if (status != Status.PAUSED) {
            throw new IllegalStateException("ServerSocket is not in paused state");
        }
        getChannel().close();
        setChannel(null);
        status = Status.STOPPED;
    }

    public void registerKeys() {
        try {
            acceptKey = getChannel().register(getSelector(), SelectionKey.OP_ACCEPT);
            acceptKey.attach(this);
        } catch (ClosedChannelException e) {
            logger.warning(e.toString());
        }
    }

    public void unregisterKeys() {
        acceptKey.cancel();
    }

    public ServerSocketChannel getChannel() {
        return (ServerSocketChannel) super.getChannel();
    }

    public void accept(SelectionKey key) {
        Socket socket = new Socket();
        try {
            SocketChannel channel = getChannel().accept();
            if (channel != null) {
                logger.fine("accept a not null channel");
                socket.setChannel(channel);
                socket.setSelector(getSelector());
                registerKeys();
            }
        } catch (IOException e) {
            logger.severe(e.toString());
        }
    }
}
