package com.peterhi.net.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import com.peterhi.io.SerialInputStream;
import com.peterhi.io.SerialObject;
import com.peterhi.io.SerialOutputStream;
import com.peterhi.net.Channel;
import com.peterhi.net.Configuration;
import com.peterhi.net.Endpoint;
import com.peterhi.net.impl.StdEndpointImpl;

/**
 * A multiplexed helper to handle multiple
 * incoming and outgoing data transmission to
 * different remote hosts.
 * @author hytparadisee
 */
public class Multiplexer implements Runnable {

    private DatagramSocket socket;

    private Endpoint endpoint;

    private Set listeners = new HashSet();

    private Set clientHandles = new HashSet();

    private byte[] temp = new byte[1000];

    /**
	 * Creates a new instance of {@link Multiplexer}, passing
	 * in the port which the {@link DatagramSocket} binds
	 * itself to.
	 * @param port The port number to bind to.
	 * @throws IOException IO error.
	 */
    public Multiplexer(int port) throws IOException {
        socket = new DatagramSocket(port);
        Configuration configuration = new Configuration();
        configuration.setMtu(550);
        endpoint = new StdEndpointImpl(socket, configuration);
    }

    /**
	 * Adds a {@link MultiplexerListener} to the {@link Multiplexer}
	 * to be notified of events.
	 * @param l The {@link MultiplexerListener} to receive events.
	 */
    public void addMultiplexerListener(MultiplexerListener l) {
        if (l == null) throw new NullPointerException();
        synchronized (listeners) {
            listeners.add(l);
        }
    }

    /**
	 * Removes a {@link MultiplexerListener} from the {@link Multiplexer}
	 * to stop being notified of events.
	 * @param l The {@link MultiplexerListener} to stop receiving events.
	 */
    public void removeMultiplexerListener(MultiplexerListener l) {
        if (l == null) throw new NullPointerException();
        synchronized (listeners) {
            listeners.remove(l);
        }
    }

    /**
	 * Post a {@link SerialObject} onto the sending queue so that it
	 * will be sent. Note that this method is non-blocking, so delivery
	 * cannot be guaranteed.
	 * @param handle The remote {@link MultiplexedClientHandle} to send the data.
	 * @param o The {@link SerialObject} to send.
	 * @throws IOException IO error.
	 */
    public void send(MultiplexedClientHandle handle, SerialObject o) throws IOException {
        synchronized (clientHandles) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            SerialOutputStream sos = new SerialOutputStream(baos);
            sos.writeSerialObject(o);
            byte[] bytes = baos.toByteArray();
            baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            if (handle.writeBuffer != null && handle.writeBuffer.length > 0) dos.write(handle.writeBuffer);
            dos.writeInt(bytes.length);
            dos.write(bytes);
            handle.writeBuffer = baos.toByteArray();
        }
    }

    /**
	 * <p>Gets the {@link MultiplexedClientHandle} at the specified
	 * {@link SocketAddress}.</p>
	 * <p>If the {@link MultiplexedClientHandle} doesn't exist in
	 * this {@link Multiplexer}, please use {@link #newClientHandle(SocketAddress)}
	 * to create one first.</p>
	 * @param address The {@link SocketAddress} of the
	 * {@link MultiplexedClientHandle}.
	 * @return The {@link MultiplexedClientHandle}, or null
	 * if none found.
	 */
    public MultiplexedClientHandle getClientHandle(SocketAddress address) {
        synchronized (clientHandles) {
            for (Iterator itor = clientHandles.iterator(); itor.hasNext(); ) {
                MultiplexedClientHandle cur = (MultiplexedClientHandle) itor.next();
                if (cur.channel.getSocketAddress().equals(address)) return cur;
            }
        }
        return null;
    }

    /**
	 * Creates a new {@link MultiplexedClientHandle}, specified by
	 * the remote {@link SocketAddress}. If the {@link MultiplexedClientHandle}
	 * already exists, return the existing one.
	 * @param address The {@link SocketAddress} of the remote host.
	 * @return The newly created {@link MultiplexedClientHandle}.
	 * @throws IOException IO error.
	 */
    public MultiplexedClientHandle newClientHandle(SocketAddress address) throws IOException {
        synchronized (clientHandles) {
            for (Iterator itor = clientHandles.iterator(); itor.hasNext(); ) {
                MultiplexedClientHandle cur = (MultiplexedClientHandle) itor.next();
                if (cur.channel.getSocketAddress().equals(address)) return cur;
            }
        }
        Channel channel = endpoint.connect(address);
        MultiplexedClientHandle handle = new MultiplexedClientHandle(channel);
        synchronized (clientHandles) {
            clientHandles.add(handle);
        }
        fireNewHandleEvent(handle);
        return handle;
    }

    public void run() {
        while (!socket.isClosed()) {
            try {
                endpoint.run();
                Channel channel = endpoint.accept();
                if (channel != null) addChannel(channel);
                synchronized (clientHandles) {
                    for (Iterator itor = clientHandles.iterator(); itor.hasNext(); ) {
                        try {
                            MultiplexedClientHandle handle = (MultiplexedClientHandle) itor.next();
                            if (handle.writeBuffer != null && handle.writeBuffer.length > 0) {
                                int written = handle.channel.write(handle.writeBuffer, 0, handle.writeBuffer.length);
                                if (written > 0) {
                                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                    baos.write(handle.writeBuffer, written, handle.writeBuffer.length - written);
                                    handle.writeBuffer = baos.toByteArray();
                                }
                            }
                            int read = handle.channel.read(temp, 0, temp.length);
                            if (read > 0) {
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                if (handle.readBuffer != null && handle.readBuffer.length > 0) baos.write(handle.readBuffer);
                                baos.write(temp, 0, read);
                                handle.readBuffer = baos.toByteArray();
                                if (handle.currentLength == -1) {
                                    if (handle.readBuffer.length >= 4) {
                                        ByteArrayInputStream bais = new ByteArrayInputStream(handle.readBuffer);
                                        DataInputStream dis = new DataInputStream(bais);
                                        handle.currentLength = dis.readInt();
                                        if (handle.readBuffer.length == 4) {
                                            handle.readBuffer = null;
                                        } else {
                                            baos = new ByteArrayOutputStream();
                                            baos.write(handle.readBuffer, 4, handle.readBuffer.length - 4);
                                            handle.readBuffer = baos.toByteArray();
                                        }
                                    }
                                }
                                if (handle.currentLength != -1) {
                                    if (handle.readBuffer.length >= handle.currentLength) {
                                        ByteArrayInputStream bais = new ByteArrayInputStream(handle.readBuffer, 0, handle.currentLength);
                                        SerialInputStream sis = new SerialInputStream(bais);
                                        Object o = sis.readSerialObject();
                                        this.fireReceivedEvent(handle, (SerialObject) o);
                                        if (handle.readBuffer.length == handle.currentLength) {
                                            handle.readBuffer = null;
                                        } else {
                                            baos = new ByteArrayOutputStream();
                                            baos.write(handle.readBuffer, handle.currentLength, handle.readBuffer.length - handle.currentLength);
                                            handle.readBuffer = baos.toByteArray();
                                        }
                                        handle.currentLength = -1;
                                    }
                                }
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    protected void fireNewHandleEvent(MultiplexedClientHandle handle) {
        synchronized (listeners) {
            for (Iterator itor = listeners.iterator(); itor.hasNext(); ) {
                MultiplexerEvent e = new MultiplexerEvent(this, handle, null);
                ((MultiplexerListener) itor.next()).accepted(e);
                if (e.isRemove()) itor.remove();
            }
        }
    }

    protected void fireAcceptedEvent(MultiplexedClientHandle handle) {
        synchronized (listeners) {
            for (Iterator itor = listeners.iterator(); itor.hasNext(); ) {
                MultiplexerEvent e = new MultiplexerEvent(this, handle, null);
                ((MultiplexerListener) itor.next()).accepted(e);
                if (e.isRemove()) itor.remove();
            }
        }
    }

    protected void fireSentEvent(MultiplexedClientHandle handle, SerialObject object) {
        synchronized (listeners) {
            for (Iterator itor = listeners.iterator(); itor.hasNext(); ) {
                MultiplexerEvent e = new MultiplexerEvent(this, handle, object);
                ((MultiplexerListener) itor.next()).sent(e);
                if (e.isRemove()) itor.remove();
            }
        }
    }

    protected void fireReceivedEvent(MultiplexedClientHandle handle, SerialObject object) {
        synchronized (listeners) {
            for (Iterator itor = listeners.iterator(); itor.hasNext(); ) {
                MultiplexerEvent e = new MultiplexerEvent(this, handle, object);
                ((MultiplexerListener) itor.next()).received(e);
                if (e.isRemove()) itor.remove();
            }
        }
    }

    private void addChannel(Channel channel) {
        MultiplexedClientHandle handle = new MultiplexedClientHandle(channel);
        synchronized (clientHandles) {
            if (clientHandles.add(handle)) fireAcceptedEvent(handle);
        }
    }
}
