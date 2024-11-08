package demo.examples.channels;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class Server implements Runnable {

    private int port;

    private final ByteBuffer buffer = ByteBuffer.allocate(16384);

    public Server(int port) {
        this.port = port;
        new Thread(this).start();
    }

    public void run() {
        try {
            ServerSocketChannel ssc = ServerSocketChannel.open();
            ssc.configureBlocking(false);
            ServerSocket ss = ssc.socket();
            InetSocketAddress isa = new InetSocketAddress(port);
            ss.bind(isa);
            Selector selector = Selector.open();
            ssc.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("Listening on port " + port);
            while (true) {
                int num = selector.select();
                if (num == 0) {
                    continue;
                }
                Set keys = selector.selectedKeys();
                Iterator it = keys.iterator();
                while (it.hasNext()) {
                    SelectionKey key = (SelectionKey) it.next();
                    if ((key.readyOps() & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT) {
                        System.out.println("acc");
                        Socket s = ss.accept();
                        System.out.println("Got connection from " + s);
                        SocketChannel sc = s.getChannel();
                        sc.configureBlocking(false);
                        sc.register(selector, SelectionKey.OP_READ);
                    } else if ((key.readyOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ) {
                        SocketChannel sc = null;
                        try {
                            sc = (SocketChannel) key.channel();
                            boolean ok = processInput(sc);
                            if (!ok) {
                                key.cancel();
                                Socket s = null;
                                try {
                                    s = sc.socket();
                                    s.close();
                                } catch (IOException ie) {
                                    System.err.println("Error closing socket " + s + ": " + ie);
                                }
                            }
                        } catch (IOException ie) {
                            key.cancel();
                            try {
                                sc.close();
                            } catch (IOException ie2) {
                                System.out.println(ie2);
                            }
                            System.out.println("Closed " + sc);
                        }
                    }
                }
                keys.clear();
            }
        } catch (IOException ie) {
            System.err.println(ie);
        }
    }

    private boolean processInput(SocketChannel sc) throws IOException {
        buffer.clear();
        sc.read(buffer);
        buffer.flip();
        if (buffer.limit() == 0) {
            return false;
        }
        for (int i = 0; i < buffer.limit(); ++i) {
            byte b = buffer.get(i);
            if ((b >= 'a' && b <= 'm') || (b >= 'A' && b <= 'M')) {
                b += 13;
            } else if ((b >= 'n' && b <= 'z') || (b >= 'N' && b <= 'Z')) {
                b -= 13;
            }
            buffer.put(i, b);
        }
        sc.write(buffer);
        System.out.println("Processed " + buffer.limit() + " from " + sc);
        return true;
    }

    public static void main(String args[]) throws Exception {
        int port = Integer.parseInt(args[0]);
        new Server(port);
    }
}
