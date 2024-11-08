package com.myjavalab.nio;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;

public class NBTest {

    /** Creates new NBTest */
    public NBTest() {
    }

    public void startServer() throws Exception {
        int channels = 0;
        int nKeys = 0;
        int currentSelector = 0;
        Selector selector = Selector.open();
        ServerSocketChannel ssc = ServerSocketChannel.open();
        InetSocketAddress address = new InetSocketAddress(InetAddress.getLocalHost(), 9000);
        ssc.socket().bind(address);
        ssc.configureBlocking(false);
        SelectionKey s = ssc.register(selector, SelectionKey.OP_ACCEPT);
        printKeyInfo(s);
        while (true) {
            debug("NBTest: Starting select");
            nKeys = selector.select();
            if (nKeys > 0) {
                debug("NBTest: Number of keys after select operation: " + nKeys);
                Set selectedKeys = selector.selectedKeys();
                Iterator i = selectedKeys.iterator();
                while (i.hasNext()) {
                    s = (SelectionKey) i.next();
                    printKeyInfo(s);
                    debug("NBTest: Nr Keys in selector: " + selector.keys().size());
                    i.remove();
                    if (s.isAcceptable()) {
                        Socket socket = ((ServerSocketChannel) s.channel()).accept().socket();
                        SocketChannel sc = socket.getChannel();
                        sc.configureBlocking(false);
                        sc.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                        System.out.println(++channels);
                    } else {
                        debug("NBTest: Channel not acceptable");
                    }
                }
            } else {
                debug("NBTest: Select finished without any keys.");
            }
        }
    }

    private static void debug(String s) {
        System.out.println(s);
    }

    private static void printKeyInfo(SelectionKey sk) {
        String s = new String();
        s = "Att: " + (sk.attachment() == null ? "no" : "yes");
        s += ", Read: " + sk.isReadable();
        s += ", Acpt: " + sk.isAcceptable();
        s += ", Cnct: " + sk.isConnectable();
        s += ", Wrt: " + sk.isWritable();
        s += ", Valid: " + sk.isValid();
        s += ", Ops: " + sk.interestOps();
        debug(s);
    }

    /**
	 * @param args
	 *            the command line arguments
	 */
    public static void main(String args[]) {
        NBTest nbTest = new NBTest();
        try {
            nbTest.startServer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
