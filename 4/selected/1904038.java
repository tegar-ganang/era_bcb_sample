package net.sf.dz2.logger.impl.udp;

import net.sf.dz.util.HostHelper;
import net.sf.dz2.logger.impl.AbstractLogger;
import net.sf.dz2.logger.impl.udp.xap.XapLogger;
import net.sf.dz2.logger.impl.udp.xpl.XplLogger;
import net.sf.dz2.meta.model.MetaMeta;
import net.sf.dz2.signal.model.DataSample;
import net.sf.jukebox.conf.Configuration;
import net.sf.jukebox.jmx.JmxAttribute;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * An abstract logger that provides logging by broadcasting UDP packets.
 * Provides a base for {@link XapLogger xAP logger} and {@link XplLogger xPL logger}.
 *
 * @param <E> Data type to log.
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2005-2007
 * @version $Id: UdpLogger.java,v 1.8 2007-03-26 09:31:29 vtt Exp $
 */
public abstract class UdpLogger<E extends Number> extends AbstractLogger<E> {

    /**
   * Date format to use.
   */
    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    /**
   * Socket port to broadcast on.
   */
    private int port;

    /**
   * Socket to broadcast to.
   */
    private DatagramSocket socket;

    /**
   * Host name. It is resolved at {@link #startup startup}, to avoid
   * repetitive invocations.
   */
    private String hostname;

    /**
   * Mapping of a host address to a network address. Used in
   * {@link #resolveNetworkAddress resolveNetworkAddress()}.
   */
    private final Map<InetAddress, InetAddress> host2network = new HashMap<InetAddress, InetAddress>();

    /**
   * Set of local addresses we can't support.
   */
    private final Set<InetAddress> unsupported = new HashSet<InetAddress>();

    /**
   * Get the default port to broadcast on.
   *
   * @return The port.
   */
    @JmxAttribute(description = "Default port")
    public abstract int getDefaultPort();

    /**
   * Get the port to broadcast on.
   *
   * @return The port.
   */
    @JmxAttribute(description = "Current port")
    public final int getPort() {
        return port;
    }

    /**
   * Get a string that will be used as a source signature.
   *
   * @return {@link #hostname host name}.
   */
    @JmxAttribute(description = "Source signature")
    public final String getSource() {
        return hostname;
    }

    /**
   * {@inheritDoc}
   */
    @Override
    protected final void configure() throws Throwable {
        Configuration cf = getConfiguration();
        String cfroot = getConfigurationRoot();
        port = cf.getInteger(cfroot + ".port", getDefaultPort());
    }

    /**
   * {@inheritDoc}
   */
    @Override
    protected final void startup() throws Throwable {
        getConfiguration();
        socket = new DatagramSocket();
        socket.setBroadcast(true);
        hostname = InetAddress.getLocalHost().getHostName();
        int dotOffset = hostname.indexOf(".");
        if (dotOffset != -1) {
            hostname = hostname.substring(0, dotOffset);
        }
        logger.info("Using host name: " + hostname);
    }

    /**
   * {@inheritDoc}
   */
    @Override
    protected final synchronized void shutdown() throws Throwable {
        socket.close();
        socket = null;
    }

    /**
   * {@inheritDoc}
   */
    @Override
    protected final void consume(String signature, DataSample<E> value) {
        if (!isEnabled()) {
            logger.warn("update() ignored - not enabled");
            return;
        }
        StringBuffer sb = new StringBuffer();
        writeHeader(sb);
        writeData(sb, signature, value);
        String packet = sb.toString();
        logger.debug("Packet:\n" + packet);
        logger.debug("Packet size: " + packet.length());
        try {
            send(packet);
        } catch (IOException ex) {
            logger.warn("send() failed:", ex);
        }
    }

    /**
   * Write a protocol header.
   *
   * @param sb String buffer to write the header to.
   */
    protected abstract void writeHeader(StringBuffer sb);

    /**
   * Write a protocol data.
   *
   * @param sb String buffer to write the header to.
   * @param signature Channel signature to use.
   * @param value Data value.
   */
    protected abstract void writeData(StringBuffer sb, String signature, DataSample<E> value);

    /**
   * Resolve a host address into a network address.
   *
   * @param address Address to resolve.
   * @return Class C network address corresponding to the given network
   * address.
   * @throws UnknownHostException if the address cannot be resolved.
   */
    private synchronized InetAddress resolveNetworkAddress(final InetAddress address) throws UnknownHostException {
        if (address == null) {
            throw new IllegalArgumentException("address can't be null");
        }
        if (!(address instanceof Inet4Address)) {
            if (unsupported.contains(address)) {
                return null;
            }
            unsupported.add(address);
            logger.warn("Don't know how to handle address " + address + " - it's a " + address.getClass().getName() + ", skipped");
            return null;
        }
        InetAddress result = host2network.get(address);
        if (result == null) {
            try {
                StringTokenizer st = new StringTokenizer(address.getHostAddress(), ".");
                String targetAddress = st.nextToken() + "." + st.nextToken() + "." + st.nextToken() + ".0";
                result = InetAddress.getByName(targetAddress);
                logger.info("Host address " + address + " translated into network address " + result);
            } catch (NoSuchElementException ex) {
                logger.warn("Unable to parse address into components: " + address, ex);
                logger.warn("Packets will be sent to interface (not network) address");
                result = address;
            }
            host2network.put(address, result);
        }
        return result;
    }

    /**
   * Send the message out.
   *
   * @param message Message to broadcast.
   * @throws IOException If there was an I/O error.
   * @throws SocketException If there was a network problem.
   */
    private void send(String message) throws IOException {
        byte[] data = message.getBytes();
        int sent = 0;
        for (Iterator<InetAddress> i = HostHelper.getLocalAddresses().iterator(); i.hasNext(); ) {
            InetAddress address = resolveNetworkAddress(i.next());
            if (address == null) {
                continue;
            }
            DatagramPacket packet = new DatagramPacket(data, message.length(), address, port);
            try {
                socket.send(packet);
                sent++;
                logger.debug("Sent packet to " + address + ":" + port);
            } catch (IOException ioex) {
                logger.warn("socket.send(" + address + ":" + port + ") failed", ioex);
            }
        }
        if (sent == 0) {
            logger.error("Couldn't send a packet to any of the addresses, check your network setup: " + HostHelper.getLocalAddresses());
        }
    }

    /**
   * {@inheritDoc}
   */
    @Override
    @SuppressWarnings(value = "unused")
    protected final void createChannel(String name, String signature, long timestamp) {
        signature2name.put(signature, name);
    }

    /**
   * Resolve channel name by signature.
   *
   * @param signature Signature to resolve.
   * @return Channel name, or <code>null</code> if unknown.
   */
    protected final String getChannelName(String signature) {
        return signature2name.get(signature);
    }

    /**
   * Replace newlines with spaces so the message can be stuffed into a single
   * line.
   *
   * @param message Original message.
   * @return Message converted to a single line.
   */
    protected final String normalize(String message) {
        if (message == null) {
            return null;
        }
        if (!message.contains("\n")) {
            return message;
        }
        StringBuffer sb = new StringBuffer();
        for (int offset = 0; offset < message.length(); offset++) {
            char c = message.charAt(offset);
            if (c == '\n') {
                sb.append(" ");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
   * Get the value string.
   *
   * @param sample Data sample.
   * @return The value as as a string representation of a number, or empty
   * string if the sample is an error sample.
   */
    protected final String getValueString(DataSample<E> sample) {
        if (sample.isError()) {
            return "";
        }
        return Double.toString(sample.sample.doubleValue());
    }

    /**
   * Get the error string.
   *
   * @param sample Data sample.
   * @return Normalized exception message, or literal
   * <code>&lt;null&gt;</code> if there's no message.
   * @exception IllegalStateException if the sample is not an error sample.
   * This is done to prevent wasting valuable UDP packet space - I don't want
   * to fragment the packets...
   */
    protected final String getErrorString(DataSample<E> sample) {
        if (!sample.isError()) {
            throw new IllegalStateException("You're not supposed to get the error string if the sample is not an error sample, call isError() first");
        }
        return normalize(sample.error.getMessage());
    }

    /**
   * Get a timestamp for the given format.
   *
   * @param time Time to get the timestamp for.
   * @return Timestamp as a string, according to
   * {@link #dateFormat date format used}.
   */
    protected final String getTimestamp(long time) {
        String timestamp;
        Date date = new Date(time);
        try {
            timestamp = dateFormat.format(date);
        } catch (Throwable t) {
            timestamp = date.toString();
        }
        return timestamp;
    }

    /**
   * Get a human readable description of the class functionality.
   *
   * @return The description.
   */
    protected abstract String getDescription();

    /**
   * Get the base metadata for this class and its subclasses.
   *
   * @return Base metadata.
   */
    protected MetaMeta getBaseMeta() {
        List<String> args = new LinkedList<String>();
        Map<String, String> defaults = new TreeMap<String, String>();
        Set<String> requires = new TreeSet<String>();
        Map<String, String> tips = new TreeMap<String, String>();
        args.add("port");
        args.add("broadcast_address");
        defaults.put("port", Integer.toString(getDefaultPort()));
        defaults.put("broadcast_address", "*");
        requires.add(".*");
        tips.put("port", "UDP port to broadcast on");
        tips.put("broadcast_address", "List of *network* addresses to broadcast on.");
        return new MetaMeta(getDescription(), null, args, defaults, requires, tips);
    }
}
