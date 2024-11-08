package sf2.io.test.bench;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class FileBenchServer {

    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.err.println("usage: FileBenchServer [bindIP] [port] [file]");
            return;
        }
        InetAddress local = InetAddress.getByName(args[0]);
        int port = Integer.parseInt(args[1]);
        ServerSocketChannel channel = ServerSocketChannel.open();
        channel.socket().bind(new InetSocketAddress(local, port));
        ByteBuffer buf = ByteBuffer.allocateDirect(512);
        while (true) {
            SocketChannel cchannel = channel.accept();
            buf.clear();
            buf.limit(8);
            cchannel.read(buf);
            buf.flip();
            long size = buf.getLong();
            System.out.println("size=" + size);
            FileOutputStream out = new FileOutputStream(args[2]);
            FileChannel fchannel = out.getChannel();
            fchannel.transferFrom(cchannel, 0, size);
            fchannel.close();
            cchannel.close();
        }
    }
}
