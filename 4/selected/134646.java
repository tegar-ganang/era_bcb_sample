package com.jme3.network.kernel.udp;

import com.jme3.network.Filter;
import com.jme3.network.kernel.*;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *  A Kernel implementation using UDP packets.
 *
 *  @version   $Revision: 8944 $
 *  @author    Paul Speed
 */
public class UdpKernel extends AbstractKernel {

    static Logger log = Logger.getLogger(UdpKernel.class.getName());

    private InetSocketAddress address;

    private HostThread thread;

    private ExecutorService writer;

    private Map<SocketAddress, UdpEndpoint> socketEndpoints = new ConcurrentHashMap<SocketAddress, UdpEndpoint>();

    public UdpKernel(InetAddress host, int port) {
        this(new InetSocketAddress(host, port));
    }

    public UdpKernel(int port) throws IOException {
        this(new InetSocketAddress(port));
    }

    public UdpKernel(InetSocketAddress address) {
        this.address = address;
    }

    protected HostThread createHostThread() {
        return new HostThread();
    }

    public void initialize() {
        if (thread != null) throw new IllegalStateException("Kernel already initialized.");
        writer = Executors.newFixedThreadPool(2, new NamedThreadFactory(toString() + "-writer"));
        thread = createHostThread();
        try {
            thread.connect();
            thread.start();
        } catch (IOException e) {
            throw new KernelException("Error hosting:" + address, e);
        }
    }

    public void terminate() throws InterruptedException {
        if (thread == null) throw new IllegalStateException("Kernel not initialized.");
        try {
            thread.close();
            writer.shutdown();
            thread = null;
        } catch (IOException e) {
            throw new KernelException("Error closing host connection:" + address, e);
        }
    }

    /**
     *  Dispatches the data to all endpoints managed by the
     *  kernel.  'routing' is currently ignored.
     */
    public void broadcast(Filter<? super Endpoint> filter, ByteBuffer data, boolean reliable, boolean copy) {
        if (reliable) throw new UnsupportedOperationException("Reliable send not supported by this kernel.");
        if (copy) {
            byte[] temp = new byte[data.remaining()];
            System.arraycopy(data.array(), data.position(), temp, 0, data.remaining());
            data = ByteBuffer.wrap(temp);
        }
        for (UdpEndpoint p : socketEndpoints.values()) {
            if (filter != null && !filter.apply(p)) continue;
            p.send(data);
        }
    }

    protected Endpoint getEndpoint(SocketAddress address, boolean create) {
        UdpEndpoint p = socketEndpoints.get(address);
        if (p == null && create) {
            p = new UdpEndpoint(this, nextEndpointId(), address, thread.getSocket());
            socketEndpoints.put(address, p);
            addEvent(EndpointEvent.createAdd(this, p));
        }
        return p;
    }

    /**
     *  Called by the endpoints when they need to be closed.
     */
    protected void closeEndpoint(UdpEndpoint p) throws IOException {
        if (socketEndpoints.remove(p.getRemoteAddress()) == null) return;
        log.log(Level.INFO, "Closing endpoint:{0}.", p);
        log.log(Level.FINE, "Socket endpoints size:{0}", socketEndpoints.size());
        addEvent(EndpointEvent.createRemove(this, p));
        if (!hasEnvelopes()) {
            addEnvelope(EVENTS_PENDING);
        }
    }

    protected void newData(DatagramPacket packet) {
        Endpoint p = getEndpoint(packet.getSocketAddress(), true);
        byte[] data = new byte[packet.getLength()];
        System.arraycopy(packet.getData(), 0, data, 0, data.length);
        Envelope env = new Envelope(p, data, false);
        addEnvelope(env);
    }

    protected void enqueueWrite(Endpoint endpoint, DatagramPacket packet) {
        writer.execute(new MessageWriter(endpoint, packet));
    }

    protected class MessageWriter implements Runnable {

        private Endpoint endpoint;

        private DatagramPacket packet;

        public MessageWriter(Endpoint endpoint, DatagramPacket packet) {
            this.endpoint = endpoint;
            this.packet = packet;
        }

        public void run() {
            if (!endpoint.isConnected()) {
                return;
            }
            try {
                thread.getSocket().send(packet);
            } catch (Exception e) {
                KernelException exc = new KernelException("Error sending datagram to:" + address, e);
                exc.fillInStackTrace();
                reportError(exc);
            }
        }
    }

    protected class HostThread extends Thread {

        private DatagramSocket socket;

        private AtomicBoolean go = new AtomicBoolean(true);

        private byte[] buffer = new byte[65535];

        public HostThread() {
            setName("UDP Host@" + address);
            setDaemon(true);
        }

        protected DatagramSocket getSocket() {
            return socket;
        }

        public void connect() throws IOException {
            socket = new DatagramSocket(address);
            log.log(Level.INFO, "Hosting UDP connection:{0}.", address);
        }

        public void close() throws IOException, InterruptedException {
            go.set(false);
            socket.close();
            join();
        }

        public void run() {
            log.log(Level.INFO, "Kernel started for connection:{0}.", address);
            while (go.get()) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    newData(packet);
                } catch (IOException e) {
                    if (!go.get()) return;
                    reportError(e);
                }
            }
        }
    }
}
