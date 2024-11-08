package aionjp.network.gameserver;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import aionjp.network.IOServer;
import aionjp_commons.network.IAcceptor;
import aionjp_commons.network.nio.Dispatcher;

/**
 * @author -Nemesiss-
 */
public class GsAcceptor implements IAcceptor {

    @Override
    public void accept(SelectionKey key) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);
        GsConnection con = new GsConnection(socketChannel);
        Dispatcher readDispatcher = IOServer.getInstance().getReadDispatcher();
        SelectionKey readKey = readDispatcher.register(socketChannel, SelectionKey.OP_READ, con);
        Dispatcher writeDispatcher = IOServer.getInstance().getWriteDispatcher();
        if (writeDispatcher != readDispatcher) con.setWriteKey(writeDispatcher.register(socketChannel, 0, con)); else con.setWriteKey(readKey);
    }

    @Override
    public String getName() {
        return "GameServer connections";
    }
}
