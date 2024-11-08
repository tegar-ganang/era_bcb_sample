package Network;

import java.util.Enumeration;
import java.util.Vector;
import java.util.Timer;
import javax.bluetooth.LocalDevice;
import java.util.Hashtable;
import Datatypes.*;
import Interfaces.IntNetwork;
import java.util.Random;

/**
 * Framework for whole Bluetooth stuff
 * 
 * Does formation, routing, reaction to external interfaces, connecting,
 * keeping track of all connected devices, ...
 *
 * @author maxx
 */
public class NetworkControl implements Globals, SocketHandler, ApplicationControl {

    private Debug debug;

    private Server server;

    private Scanner scanner;

    private DeviceConnector connector;

    private Vector interfaces = new Vector();

    private static final NetworkControl INSTANCE = new NetworkControl();

    private Timer keepalive_timer;

    private Timer scan_timer;

    private KeepaliveTask keepalive_task;

    private ScanTask scan_task;

    private NameResolution name_resolution;

    private int totalIncomingBytes = 0;

    private int totalOutgoingBytes = 0;

    private Random random = new Random(System.currentTimeMillis());

    public int conn_max;

    public String my_address;

    public String my_name = "not set";

    /**
     * Constructor
     *
     * Creates server and scanner threads.
     */
    private NetworkControl() {
        debug = Debug.getInstance();
    }

    /**
     * sends statistics to Networkinterface
     * @return int array with statistics
     */
    public int[] getStatistics() {
        int statistics[] = new int[2];
        statistics[0] = totalIncomingBytes;
        statistics[1] = totalOutgoingBytes;
        return statistics;
    }

    /**
     * sends message to all devices
     * @param text messagetext
     * @return error code
     */
    public ErrCode sendOutgoingMessage(String text) {
        return sendOutgoingMessage(BROADCAST_ADDRESS, text);
    }

    /**
     * sends message to one device
     * @param dest destinationaddress
     * @param text destinationtext
     * @return error code
     */
    public ErrCode sendOutgoingMessage(String dest, String text) {
        sendMessage(text, dest);
        return new ErrCode(Constants.ERR_SUCCESS);
    }

    /**
     * sends incoming message to networkinterface
     * @param incoming message to forward
     * @return error code
     */
    public ErrCode forwardIncomingMessage(Message incoming) {
        return IntNetwork.getInstance().forwardIncomingMessage(incoming);
    }

    /**
     * start all network services
     * @return error code
     */
    public ErrCode startApp() {
        LocalDevice dev;
        try {
            dev = LocalDevice.getLocalDevice();
            my_address = dev.getBluetoothAddress();
            if (my_name.equals("not set")) my_name = dev.getFriendlyName();
            conn_max = Integer.parseInt(LocalDevice.getProperty("bluetooth.connected.devices.max"));
            conn_max = Math.min(conn_max, 7);
            debug.writeLine("I'm " + my_address);
            server = new Server(this);
            server.start();
            scanner = new Scanner();
            scanner.start();
        } catch (Exception ex) {
            debug.exception(ex);
            return new ErrCode(Constants.ERR_FAILURE);
        }
        keepalive_timer = new Timer();
        keepalive_task = new KeepaliveTask(this);
        keepalive_timer.schedule(keepalive_task, 5000, 5000);
        scan_timer = new Timer();
        scan_task = new ScanTask(this);
        int time = 80000 - Math.abs(random.nextInt() % 40000);
        scan_timer.schedule(scan_task, 5000, time);
        debug.writeLine("scan period: " + time);
        name_resolution = NameResolution.getInstance();
        connector = new DeviceConnector(this);
        connector.start();
        IntNetwork.getInstance().setFriendlyName(my_name);
        return new ErrCode(Constants.ERR_SUCCESS);
    }

    /**
     * not used
     * @return always error code success
     */
    public ErrCode pauseApp() {
        return new ErrCode(Constants.ERR_SUCCESS);
    }

    /**
     * destroys all network services
     * @return always error code success
     */
    public ErrCode endApp() {
        destroy();
        return new ErrCode(Constants.ERR_SUCCESS);
    }

    /**
     * used to ensure singleton
     * @return instance of networkControl
     */
    public static synchronized NetworkControl getInstance() {
        return INSTANCE;
    }

    /**
     * Invoked by Client if some packet came in.
     * @param packet the received packet
     */
    public synchronized void packetReceived(Packet packet) {
        byte type = PacketFactory.packetType(packet);
        if (type == PacketFactory.PACKET_ROUTE_ADD) {
            debug.writeLine("route add");
            String[] routes = PacketFactory.parseRouteInfo(packet);
            for (int i = 0; i < routes.length; i++) debug.writeLine(routes[i]);
            Interface[] if_list = new Interface[interfaces.size()];
            interfaces.copyInto(if_list);
            for (int i = 0; i < if_list.length; i++) for (int j = 0; j < routes.length; j++) if (routes[j].equals(if_list[i].remote_addr)) {
                if_list[i].destroy();
                return;
            }
            Interface iface = getInterfaceByAddress(packet.remote_addr);
            if (iface == null) return;
            iface.reachable = addRoutes(iface.reachable, routes);
            if_list = new Interface[interfaces.size()];
            interfaces.copyInto(if_list);
            for (int i = 0; i < if_list.length; i++) if (!if_list[i].equals(iface)) if_list[i].send(packet);
            for (int i = 0; i < routes.length; i++) {
                if (name_resolution.isResolved(routes[i])) {
                    IntNetwork.getInstance().addBuddy(routes[i], name_resolution.getName(routes[i]));
                }
            }
        } else if (type == PacketFactory.PACKET_ROUTE_DEL) {
            debug.writeLine("route del");
            String[] routes = PacketFactory.parseRouteInfo(packet);
            for (int i = 0; i < routes.length; i++) debug.writeLine(routes[i]);
            Interface[] if_list = new Interface[interfaces.size()];
            interfaces.copyInto(if_list);
            for (int i = 0; i < if_list.length; i++) for (int j = 0; j < routes.length; j++) if (routes[j].equals(if_list[i].remote_addr)) {
                if_list[i].destroy();
                return;
            }
            Interface iface = getInterfaceByAddress(packet.remote_addr);
            if (iface == null) return;
            iface.reachable = delRoutes(iface.reachable, routes);
            if_list = new Interface[interfaces.size()];
            interfaces.copyInto(if_list);
            for (int i = 0; i < if_list.length; i++) if (!if_list[i].equals(iface)) if_list[i].send(packet);
            for (int i = 0; i < routes.length; i++) {
                IntNetwork.getInstance().removeDevice(routes[i]);
            }
        } else if (type == PacketFactory.PACKET_MESSAGE) {
            Message message = PacketFactory.parseMessagePacket(packet);
            if (isPacketForMe(packet)) {
                forwardIncomingMessage(message);
                debug.writeLine("msg " + getDeviceName(message.src) + ": " + message.text);
            }
            forward(packet);
        } else if (type == PacketFactory.PACKET_QUERY_NAME) {
            Message message = PacketFactory.parseMessagePacket(packet);
            if (isPacketForMe(packet)) {
                if (interfaces.size() > 0) name_resolution.sendName(message.src);
            }
            forward(packet);
        } else if (type == PacketFactory.PACKET_NAME) {
            Message message = PacketFactory.parseMessagePacket(packet);
            if (isPacketForMe(packet)) {
                name_resolution.setName(message.src, message.text);
            }
            forward(packet);
        } else if (type == PacketFactory.PACKET_KEEPALIVE) {
        } else {
            debug.writeLine("unknown packet received: " + type);
        }
    }

    /**
     * forwarding function to forward broadcast and unicast packets
     * and recompute multicast packets and send them
     * @param packet packet to forward
     */
    public void forward(Packet packet) {
        Message message = PacketFactory.parseMessagePacket(packet);
        if (message.dest[0].equals(BROADCAST_ADDRESS)) {
            Interface[] if_list = new Interface[interfaces.size()];
            interfaces.copyInto(if_list);
            for (int i = 0; i < if_list.length; i++) {
                if (packet.remote_addr == null || !packet.remote_addr.equals(if_list[i].remote_addr)) if_list[i].send(packet);
            }
        } else {
            try {
                if (message.dest.length == 1 && message.dest[0].toLowerCase().equals(my_address.toLowerCase())) return;
                Hashtable hashtable = new Hashtable();
                for (int i = 0; i < message.dest.length; i++) {
                    String[] addresses;
                    if (getUnicastDestination(message.dest[i]) != null) {
                        String nextHop = getUnicastDestination(message.dest[i]).remote_addr;
                        if (!hashtable.containsKey(nextHop)) {
                            addresses = new String[0];
                        } else {
                            addresses = (String[]) hashtable.get(nextHop);
                        }
                        if (!message.dest[i].toLowerCase().equals(my_address.toLowerCase())) {
                            String[] new_addresses = new String[addresses.length + 1];
                            for (int j = 0; j < addresses.length; j++) {
                                new_addresses[j] = addresses[j];
                            }
                            new_addresses[addresses.length] = message.dest[i];
                            hashtable.put(nextHop, new_addresses);
                        }
                    }
                }
                for (Enumeration e = hashtable.keys(); e.hasMoreElements(); ) {
                    String key = (String) e.nextElement();
                    Interface iface = getUnicastDestination(key);
                    Packet new_packet = PacketFactory.createMessagePacket(new Message(message.src, (String[]) hashtable.get(key), message.text));
                    new_packet.byte_data[0] = packet.byte_data[0];
                    if (iface != null) iface.send(new_packet);
                }
            } catch (Exception e) {
                debug.writeLine("exception");
                e.printStackTrace();
            }
        }
    }

    /**
     * check if a packet is for own device
     * @param packet packet to check
     * @return true if packet ist broadcast or own address is in destinations, false otherwise
     */
    private boolean isPacketForMe(Packet packet) {
        Message message = PacketFactory.parseMessagePacket(packet);
        if (message.dest[0].equals(BROADCAST_ADDRESS)) return true;
        for (int i = 0; i < message.dest.length; i++) {
            if (message.dest[i].toLowerCase().equals(my_address.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * only used for debug
     * to be removed
     */
    public synchronized void sendAdd() {
        String[] routes = new String[1];
        routes[0] = new String("001122334455");
        Packet packet = PacketFactory.createRoutePacket(PacketFactory.PACKET_ROUTE_ADD, routes);
        Interface[] if_list = new Interface[interfaces.size()];
        interfaces.copyInto(if_list);
        for (int i = 0; i < if_list.length; i++) if_list[i].send(packet);
    }

    /**
     * only used for debug
     * to be removed
     */
    public synchronized void sendDel() {
        String[] routes = new String[1];
        routes[0] = new String("001122334455");
        Packet packet = PacketFactory.createRoutePacket(PacketFactory.PACKET_ROUTE_DEL, routes);
        Interface[] if_list = new Interface[interfaces.size()];
        interfaces.copyInto(if_list);
        for (int i = 0; i < if_list.length; i++) if_list[i].send(packet);
    }

    /**
     * Send message via unicast
     * @param message message to send
     * @param address destination address
     */
    public synchronized void sendMessage(String message, String address) {
        String[] addresses = new String[1];
        addresses[0] = address;
        sendMessage(message, addresses);
    }

    /**
     * send message via multicast
     * @param message message to send
     * @param addresses destination addresses
     */
    public synchronized void sendMessage(String message, String[] addresses) {
        Packet packet = PacketFactory.createMessagePacket(message, my_address, addresses);
        forward(packet);
    }

    /**
     * TESTING: make scan invokable by UI
     *
     * TODO: to be removed
     */
    public synchronized void startScan() {
        if (scanner == null) return;
        if (!scanner.inProgress()) {
            scanner.startScan(connector);
        }
    }

    /**
     * unused
     */
    public synchronized void updateView() {
        if (scanner == null) return;
        if (!scanner.inProgress()) {
            scanner.updateView();
        }
    }

    /**
     * Invoked when connection takes place
     * @param iface interface of the new connection
     */
    public synchronized void newConnection(Interface iface) {
        debug.writeLine("connect " + iface.remote_addr);
        if (isKnown(iface.remote_addr)) {
            iface.destroy();
            return;
        }
        if (!interfaces.contains(iface)) {
            interfaces.addElement(iface);
            server.updateNumConnections(interfaces.size());
            if (name_resolution.isResolved(iface.remote_addr)) {
                IntNetwork.getInstance().addBuddy(iface.remote_addr, name_resolution.getName(iface.remote_addr));
            }
            Packet update_packet = PacketFactory.createRoutePacket(PacketFactory.PACKET_ROUTE_ADD, iface.reachable);
            Interface[] if_list = new Interface[interfaces.size()];
            interfaces.copyInto(if_list);
            for (int i = 0; i < if_list.length; i++) if (!if_list[i].equals(iface)) if_list[i].send(update_packet);
        }
    }

    /**
     * Invoked when some connection got lost by whatever reason
     * @param thread interface of the closed connection
     */
    public synchronized void connectionClosed(Interface thread) {
        IntNetwork.getInstance().removeDevice(thread.remote_addr);
        if (interfaces.contains(thread)) {
            debug.writeLine("disconnect " + thread.remote_addr);
            int importance = thread.reachable.length;
            for (int i = 0; i < thread.reachable.length; i++) {
                IntNetwork.getInstance().removeDevice(thread.reachable[i]);
            }
            interfaces.removeElement(thread);
            server.updateNumConnections(interfaces.size());
            if ((importance > 1) || (interfaces.size() == 0)) if (connector != null) connector.fastReconnect();
            Interface[] if_list = new Interface[interfaces.size()];
            interfaces.copyInto(if_list);
            for (int i = 0; i < if_list.length; i++) if_list[i].send(PacketFactory.createRoutePacket(PacketFactory.PACKET_ROUTE_DEL, thread.reachable));
        }
    }

    /**
     * list interfaces
     * @return list of interface addresses
     */
    public String[] listConnections() {
        String ret[] = new String[interfaces.size()];
        int i = 0;
        Enumeration con_enum = interfaces.elements();
        while (con_enum.hasMoreElements()) {
            Interface t = (Interface) con_enum.nextElement();
            ret[i++] = t.remote_addr;
        }
        return ret;
    }

    /**
     * sends a keepalive packet to all interfaces
     */
    public void sendKeepalive() {
        Packet keepalive = new Packet();
        keepalive.byte_data[0] = PacketFactory.PACKET_KEEPALIVE;
        keepalive.len = 1;
        Interface[] if_list = new Interface[interfaces.size()];
        interfaces.copyInto(if_list);
        for (int i = 0; i < if_list.length; i++) if_list[i].send(keepalive);
    }

    /**
     * computes next hop
     * @param address address of destination
     * @return interface of next hop
     */
    private Interface getUnicastDestination(String address) {
        Interface[] if_list = new Interface[interfaces.size()];
        Interface result = null;
        interfaces.copyInto(if_list);
        for (int i = 0; i < if_list.length; i++) for (int j = 0; j < if_list[i].reachable.length; j++) if (address.toLowerCase().equals(if_list[i].reachable[j].toLowerCase())) {
            result = if_list[i];
            break;
        }
        return result;
    }

    /**
     * concatenates two String arrays
     * @param current first String array
     * @param extra second String array
     * @return result array
     */
    private String[] addRoutes(String[] current, String[] extra) {
        String[] result = new String[current.length + extra.length];
        for (int i = 0; i < current.length; i++) result[i] = current[i];
        for (int i = 0; i < extra.length; i++) result[i + current.length] = extra[i];
        return result;
    }

    /**
     * deletes some String from an String array
     * @param current String array
     * @param todelete String array with Strings to delete
     * @return result array
     */
    private String[] delRoutes(String[] current, String[] todelete) {
        Vector temp = new Vector();
        for (int i = 0; i < current.length; i++) {
            boolean equal = false;
            for (int j = 0; j < todelete.length; j++) if (current[i].toLowerCase().equals(todelete[j].toLowerCase())) {
                equal = true;
                break;
            }
            if (!equal) temp.addElement(current[i]);
        }
        String[] result = new String[temp.size()];
        temp.copyInto(result);
        return result;
    }

    /**
     * checks if address is reachable
     * @param address address to check
     * @return true if address is reachable
     */
    public boolean isKnown(String address) {
        String[] nodes = getAllKnownNodes();
        for (int i = 0; i < nodes.length; i++) if (address.equals(nodes[i])) return true;
        return false;
    }

    /**
     * gets all reachable nodes
     * @return String array with all reachable nodes
     */
    public String[] getAllKnownNodes() {
        Vector node_list = new Vector();
        Interface[] if_list = new Interface[interfaces.size()];
        interfaces.copyInto(if_list);
        for (int i = 0; i < if_list.length; i++) for (int j = 0; j < if_list[i].reachable.length; j++) node_list.addElement(if_list[i].reachable[j]);
        String[] result = new String[node_list.size()];
        node_list.copyInto(result);
        return result;
    }

    /**
     * only used for debug
     */
    public void printReachability() {
        String[] nodes = getAllKnownNodes();
        for (int i = 0; i < nodes.length; i++) debug.writeLine(nodes[i] + " " + name_resolution.getName(nodes[i]));
    }

    /**
     * Shutdown
     */
    public void destroy() {
        keepalive_timer.cancel();
        scan_timer.cancel();
        name_resolution.destroy();
        server.destroy();
        connector.destroy();
        scanner.destroy();
        Interface[] t_list = new Interface[interfaces.size()];
        interfaces.copyInto(t_list);
        for (int i = 0; i < t_list.length; i++) t_list[i].destroy();
    }

    /**
     * get interface of a neighbor
     * @param address address of neighbor device
     * @return interface of searched device
     */
    public Interface getInterfaceByAddress(String address) {
        Interface[] if_list = new Interface[interfaces.size()];
        interfaces.copyInto(if_list);
        if (address == null) return null;
        for (int i = 0; i < if_list.length; i++) if (address.equals(if_list[i].remote_addr)) return if_list[i];
        return null;
    }

    /**
     * get name of a device
     * @param addr address of device
     * @return name of device
     */
    public String getDeviceName(String addr) {
        return name_resolution.getName(addr);
    }

    /**
     * set own device name
     * @param name own device name
     */
    public void setOwnDeviceName(String name) {
        my_name = name;
        if (name_resolution != null) name_resolution.changeName(name);
    }

    /**
     * increases total incoming bytes
     * used for statistics
     * @param bytes amount of bytes
     */
    public void addIncomingBytes(int bytes) {
        totalIncomingBytes += bytes;
    }

    /**
     * increases total outgoing bytes
     * used for statistics
     * @param bytes amount of bytes
     */
    public void addOutgoingBytes(int bytes) {
        totalOutgoingBytes += bytes;
    }

    /**
     * used for debug
     * @param text debugtext
     */
    public void debugOutput(String text) {
        IntNetwork.getInstance().debugOut(text);
    }

    /**
     * get the number of neighbors
     * @return number of interfaces
     */
    public int getConnectionCount() {
        return interfaces.size();
    }
}
