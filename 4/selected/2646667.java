package ursus.client;

import ursus.io.IReactor;
import ursus.io.IHandler;
import ursus.io.crypto.Encryption;
import ursus.io.crypto.CipherSet;
import ursus.io.crypto.ICipherSetSpec;
import ursus.io.crypto.CryptoSpec;
import ursus.io.crypto.KeyExchanger;
import java.security.PublicKey;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ByteArrayInputStream;
import java.net.InetSocketAddress;
import java.util.Set;
import java.util.Iterator;

/**
 * Implements IClientReactor.
 * @author Anthony
 */
public class ClientReactor implements IClientReactor {

    protected Selector selector;

    protected IHandler handler;

    protected String url;

    protected int port;

    protected boolean alive;

    /** Creates a new instance of ClientReactor 
     * 
     * @param url The URL to connect this ClientReactor to
     * @param port The port of the URL.
     */
    public ClientReactor(String url, int port) throws IOException {
        selector = Selector.open();
        this.url = url;
        this.port = port;
    }

    public void run() {
        try {
            while (!Thread.interrupted() && alive) {
                selector.select();
                Set selected = selector.selectedKeys();
                Iterator it = selected.iterator();
                while (it.hasNext()) dispatch((SelectionKey) (it.next()));
                selected.clear();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Selector getSelector() {
        return selector;
    }

    public Channel getChannel() {
        return handler.getSocketChannel();
    }

    public void dispatch(SelectionKey key) {
        Runnable r = (Runnable) (key.attachment());
        if (r != null) {
            r.run();
        }
    }

    public IHandler getHandler() {
        return handler;
    }

    public void init() {
        try {
            System.out.println("Establishing a connection...");
            SocketChannel socket = SocketChannel.open();
            System.out.println(url + port);
            socket.connect(new InetSocketAddress(url, port));
            ByteBuffer buffer = ByteBuffer.allocate(IReactor.HAND_SHAKE_BUFFER);
            socket.read(buffer);
            buffer.flip();
            byte[] bytes = new byte[buffer.getInt()];
            buffer.get(bytes);
            ByteArrayInputStream bin = new ByteArrayInputStream(bytes);
            ObjectInputStream in = new ObjectInputStream(bin);
            int bufferSize = in.readInt();
            CryptoSpec dhSpec = (CryptoSpec) in.readObject();
            ICipherSetSpec cipherSpec = (ICipherSetSpec) in.readObject();
            in.close();
            System.out.println("Generating a public key, this may take a while...");
            KeyExchanger dh = (KeyExchanger) new ursus.io.crypto.DHAgreement(dhSpec);
            bytes = dh.generatePublicKey();
            System.out.println("Sending public key to server...");
            buffer.clear();
            buffer.putInt(bytes.length);
            buffer.put(bytes);
            buffer.flip();
            socket.write(buffer);
            buffer.clear();
            socket.read(buffer);
            buffer.flip();
            bytes = new byte[buffer.getInt()];
            buffer.get(bytes);
            System.out.println("Generating a secret key...");
            PublicKey publicKey = dh.getPublicKeyFromEncoded(bytes);
            bytes = dh.generatePrivateKey(publicKey);
            System.out.println("Finishing up handshake...");
            Encryption encryption = (Encryption) new CipherSet(cipherSpec, bytes);
            handler = new ClientHandler(selector, socket, bufferSize, encryption);
            Thread th = new Thread(this);
            alive = true;
            th.start();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void destroy() {
        alive = false;
    }
}
