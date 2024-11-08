package identifierMessageBus;

import identifierMessageBus.bus.Device;
import identifierMessageBus.bus.DirectMulticastMessage;
import identifierMessageBus.bus.DistributorMessage;
import identifierMessageBus.bus.IndirectMulticastMessage;
import identifierMessageBus.bus.UnicastMessage;
import identifierMessageBus.bus.Event;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

public class MessageBus {

    /**
	 * This method convers a given value to its binary representation and
	 * returns a new string consisting of 64 chracters, that are either 0 or 1.
	 * 
	 * @param id
	 *            The value to convert.
	 * @return the converted value.
	 */
    public static String IDtoString(long id) {
        char[] zeros = new char[64];
        for (byte i = 63; i >= 0; i--) zeros[63 - i] = ((id & (Long.rotateLeft(1, i))) == 0) ? '0' : '1';
        return new String(zeros);
    }

    /**
	 * Construct a new {@link MessageBus} object. The constructor is called with
	 * the bus ID and the number of bits reserved for the bus ID.
	 * 
	 * @param id
	 *            The designated ID of the bus.
	 * @param busmask
	 *            The number of bits reserved for the bus ID. The remaining bits
	 *            are used for local adressing.
	 */
    public MessageBus(int id, byte busmask) {
        if ((busmask & 0x3f) != busmask) throw new IllegalArgumentException("Invalid bus mask supplied: " + busmask + " max: 0x3f");
        this.busID = id;
        byte end = (byte) (0x3f - busmask);
        this.busBitMask = -1 << end + 1;
        fullID = Long.rotateLeft(id, end + 1);
        System.out.println("ID\t" + IDtoString(id));
        System.out.println("bitmask\t" + IDtoString(busBitMask) + " " + Long.bitCount(busBitMask) + " " + busBitMask);
        System.out.println("fullID\t" + IDtoString(fullID));
        assert 0 < busmask && busmask < 63;
        assert Long.bitCount(busBitMask) == busmask;
        assert (fullID | busBitMask) == busBitMask;
        assert 0 < id && id < Long.rotateLeft(1, busmask);
        if ((fullID | busBitMask) != busBitMask) throw new IllegalArgumentException("Invalid bus id supplied: " + id + " (max: " + ((1 << busmask) - 1) + " with " + busmask + " id bits.)");
    }

    private final int busID;

    private final long busBitMask;

    private final long fullID;

    private final AtomicInteger intSequence = new AtomicInteger();

    private Set<Device> devices = new HashSet<Device>();

    private LinkedList<Long> freedIDs = new LinkedList<Long>();

    private long getNextID() {
        if (freedIDs.size() == 0) return intSequence.incrementAndGet() & ~busBitMask | fullID; else return freedIDs.remove();
    }

    public void addDevice(Device d) {
        devices.add(d);
        d.setID(getNextID());
        d.setBus(this);
    }

    public void removeDevice(Device d) {
        if (devices.remove(d)) freedIDs.add(d.getID());
        for (Set<Device> s : distributor2devices.values()) for (Device sd : s) {
            if (sd.equals(d)) s.remove(d);
        }
        for (Entry<Class<? extends Event>, Entry<Long, Device>> e : distributors.entrySet()) if (e.getValue().getValue().equals(d)) {
            distributors.remove(e);
            freedIDs.add(e.getValue().getKey());
        }
    }

    /**
	 * Expose the list of registered devices.
	 * 
	 * @return The subscribers that are registered. Use the set to modify the
	 *         bus.
	 */
    public Set<Device> getDevices() {
        return devices;
    }

    /**
	 * Post an event on the bus. This method accepts an event descriptor and
	 * searches for a work unit with the given name.
	 * 
	 * @param <T>
	 *            The event type.
	 * @param event
	 *            The event to execute.
	 * @param message
	 *            The message type for sending the {@link Event}.
	 * @return The {@link Event} returned by the device or <code>null</code>
	 *         if the subscriber was not found.
	 */
    public <T extends Event> T postEvent(T event, UnicastMessage message) {
        Class<? extends Event> workUnitType = event.getClass();
        if (message.isDirected()) {
            for (Device d : devices) {
                if (d.getID() == message.getReceiver() && d.getEvents().contains(workUnitType)) {
                    return d.receiveEvent(event);
                }
            }
        } else for (Device d : devices) {
            if (d.getEvents().contains(workUnitType)) {
                message.setReceiver(d.getID());
                return d.receiveEvent(event);
            }
        }
        return null;
    }

    /**
	 * Get a list of all receivers that can receive a certain event type. This
	 * list is only required to be complete for local devices.
	 * 
	 * @param <T>
	 *            The Event's type.
	 * @param eventType
	 *            The event to be queried.
	 * @return An array of receiver IDs.
	 */
    public <T extends Event> long[] getAllReceivers(Class<T> eventType) {
        List<Long> IDs = new ArrayList<Long>();
        for (Device d : devices) if (d.getEvents().contains(eventType)) IDs.add(d.getID());
        long[] longs = new long[IDs.size()];
        int p = 0;
        for (Long id : IDs) longs[p++] = id.longValue();
        return longs;
    }

    public <T extends Event> T postEvent(T event, IndirectMulticastMessage message) {
        Class<? extends Event> workUnitType = event.getClass();
        for (Device d : devices) {
            if (d.getEvents().contains(workUnitType)) {
                message.addReceiver(d.getID(), d.receiveEvent(event));
            }
        }
        return null;
    }

    public <T extends Event> T postEvent(T event, DirectMulticastMessage message) {
        Class<? extends Event> workUnitType = event.getClass();
        for (long id : message.getResult().keySet()) for (Device d : devices) {
            if (d.getID() == id && d.getEvents().contains(workUnitType)) {
                message.addReceiver(d.getID(), d.receiveEvent(event));
            }
        }
        return null;
    }

    public Map<Long, Set<Device>> distributor2devices = new HashMap<Long, Set<Device>>();

    public <T extends Event> T postEvent(T event, DistributorMessage message) {
        for (Device d : distributor2devices.get(message.getChannel())) d.receiveEvent(event);
        return null;
    }

    private Map<Class<? extends Event>, Entry<Long, Device>> distributors = new HashMap<Class<? extends Event>, Entry<Long, Device>>();

    public long registerDistributor(Device device, Class<? extends Event> distributor) {
        long id = getNextID();
        Entry<Long, Device> e = new AbstractMap.SimpleEntry<Long, Device>(id, device);
        distributors.put(distributor, e);
        return id;
    }

    public void subscribeDistributor(Device d, Class<? extends Event> distributor) {
        long id = distributors.get(distributor).getKey();
        Set<Device> distDevices = distributor2devices.get(id);
        if (distDevices == null) {
            distDevices = new HashSet<Device>();
            distributor2devices.put(id, distDevices);
        }
        distDevices.add(d);
    }

    public void unregisterDistributor(Device device, Class<? extends Event> distributor) {
        Entry<Long, Device> e = distributors.get(distributor);
        if (e != null && e.getValue() == device) {
            distributors.remove(distributor);
            freedIDs.add(e.getKey());
        }
    }

    public void unsubscribeDistributor(Device d, Class<? extends Event> distributor) {
        long id = distributors.get(distributor).getKey();
        Set<Device> distDevices = distributor2devices.get(id);
        if (distDevices != null) {
            distDevices.remove(d);
        }
    }

    public long getDistributorID(Class<? extends Event> unit) {
        Entry<Long, Device> e = distributors.get(unit);
        if (e == null) return -1; else return e.getKey();
    }

    /**
	 * Check is an ID is local or remote.
	 * 
	 * @param id
	 *            The ID to test.
	 * @return <code>true</code> if the ID is local to the bus.
	 */
    public boolean isLocal(final long id) {
        return (id & busBitMask) == fullID;
    }

    protected int getBusID() {
        return busID;
    }
}
