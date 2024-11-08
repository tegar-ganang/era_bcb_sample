package bman.tools.net;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.logging.Logger;
import javax.xml.transform.Source;
import bman.filecopy.FileSizeTracker;

/**
 * This class works like a connection wrapper. A SocketSession will hold
 * a SocketChannel and an invocation to any of SocketSession's send or receive methods
 * will be passed through this SocketChannel. When a SocketSession is created, it will have
 * a working SocketChannel or it will immediately try to establish a connection. The general 
 * contract is, this SocketSession will try everything possible to provide 
 * a stable and resilient connection. You do not need to worry how it will connect.
 * <p>
 * What will happen when the SocketSession's channel is disconnected?
 * <p>
 * Untested. But there is not code yet for the SocketSession to be able to recover.
 * 
 * @author MrJacky
 *
 */
public class SocketSession {

    private Logger log = Logger.getLogger(this.getClass().getName());

    private ByteStation bt;

    private SocketChannel channel;

    private String address;

    /**
	 * This variable will tell if any byte has been sent
	 * through this  session
	 */
    private boolean hasSentSomething = false;

    /**
	 * The address format is <ip>:<port>
	 * The SocketSession will attempt to connect to the address that is
	 * provided. 
	 * A successful connection will result with SoecktSession having a channel.
	 * SocketSession will use ByteStation in order to obtain/establish a SocketChannel connection.
	 */
    protected SocketSession(ByteStation bt, String address) {
        this.bt = bt;
        this.address = address;
        initChannel();
    }

    DataInputStream dis;

    /**
	 * Initializes this SocketSession's channel. 
	 * The SocketChannel is obtained from the ByteStation.
	 */
    private void initChannel() {
        try {
            channel = bt.getChannel(address);
            dis = new DataInputStream(this.getChannel().socket().getInputStream());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
	 * This method sends the "magic" int value on this session's channel. The purpose of this
	 * is to notify the ByteListener that there is an incoming transmission. The ByteListener will
	 * then read the "magic" int value so that the socket position will be updated and then pass the
	 * SocketSession to the Receiver registered to the ByteListener.
	 * <p>
	 * It is very important to call notifyReceiver on every send operation of SocektListener. This method will notify the Receiver to get ready to read the bytes that
	 * will be send on any of SocketSession's send methods.
	 * <p>
	 * A better design is to make this mandatory - ie to guarantee that this method will be called before
	 * each and every attempt of SocketSession to send data through the channel.
	 */
    private void notifyReciever() {
        if (hasSentSomething == false) {
            try {
                DataOutputStream dis = new DataOutputStream(channel.socket().getOutputStream());
                dis.writeInt(917);
                log.info("Reciever has been notified.");
                hasSentSomething = true;
            } catch (Exception e) {
                bt.removeSocket(address);
                throw new RuntimeException(e);
            }
        }
    }

    /**
	 * Reads bytes from this session's channel and saves it into the specified
	 * file. The file will be created if it does not yet exists.
	 * The number of bytes to be read will be equal to the length parameter.
	 * <p>
	 * This method will block until bytes are sent to this session's channel.
	 * If the length is more that what is sent through this channel, that behavior
	 * is untested.
	 * 
	 * @param length the number of bytes to read
	 * @param saveHere file where the incoming bytes will be saved
	 */
    public long receiveFileAndSaveTo(long length, File saveHere) {
        try {
            FileOutputStream source = new FileOutputStream(saveHere);
            long b = source.getChannel().transferFrom(channel, 0, length);
            source.flush();
            source.close();
            return b;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
	 * Sends the contents of the file through this sessions channel. The files will be
	 * sent in raw bytes.
	 * 
	 * @param file to be sent
	 */
    public void send(File file) {
        notifyReciever();
        try {
            FileInputStream source = new FileInputStream(file);
            source.getChannel().transferTo(0, file.length(), channel);
            source.close();
        } catch (Exception e) {
            bt.removeSocket(address);
            throw new RuntimeException(e);
        }
    }

    /**
	 * Sends a string through this sessions SocketChannel.
	 */
    public void send(String value) {
        ByteBuffer bb = ByteBuffer.wrap(value.getBytes());
        send(bb);
    }

    /**
	 * Sends an int through this sessions SocketChannel.
	 */
    public void send(int value) {
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.asIntBuffer().put(value);
        send(bb);
    }

    /**
	 * Sends an long through this sessions SocketChannel.
	 */
    public void send(long value) {
        ByteBuffer bb = ByteBuffer.allocate(8);
        bb.asLongBuffer().put(value);
        send(bb);
    }

    private long send(ByteBuffer buffer) {
        notifyReciever();
        try {
            long b = channel.write(buffer);
            log.info("send: " + b + " bytes written");
            return b;
        } catch (Exception e) {
            bt.removeSocket(address);
            throw new RuntimeException(e);
        }
    }

    /**
	 * An unused method
	 */
    public boolean transfer(ByteChannel sourceChannel, SocketChannel destChannel) {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            int r = sourceChannel.read(buffer);
            long count = 0;
            while (r != -1) {
                count++;
                buffer.flip();
                destChannel.write(buffer);
                buffer.clear();
                r = sourceChannel.read(buffer);
            }
            log.info("Transfered " + count + " bytes to " + destChannel);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public SocketChannel getChannel() {
        return this.channel;
    }

    public int readInt() {
        try {
            return dis.readInt();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public long readLong() {
        try {
            return dis.readLong();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] read(int length) {
        try {
            ByteBuffer b = ByteBuffer.allocate(length);
            channel.read(b);
            return b.array();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
