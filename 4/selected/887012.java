package net.sf.dz3.device.sensor.impl.onewire;

import java.io.IOException;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.sf.dz3.device.sensor.DzSwitchContainer;
import net.sf.dz3.device.sensor.SensorType;
import org.apache.log4j.Logger;
import org.apache.log4j.NDC;
import com.dalsemi.onewire.container.OneWireContainer;
import com.dalsemi.onewire.container.SwitchContainer;

/**
 * A platform independent switch container.
 */
public class OneWireSwitchContainer extends OneWireDeviceContainer implements DzSwitchContainer {

    private final Logger logger = Logger.getLogger(getClass());

    private final OwapiDeviceFactory factory;

    /**
     * Number of channels the device has.
     */
    private int channelCount = 0;

    /**
     * Create an instance.
     * 
     * @param container
     *            1-Wire API container to base this container on.
     */
    public OneWireSwitchContainer(final OwapiDeviceFactory factory, final OneWireContainer container) {
        super(container);
        this.factory = factory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final SensorType getType() {
        return SensorType.SWITCH;
    }

    /**
     * @return Number of channels the device has.
     */
    public final synchronized int getChannelCount() {
        if (channelCount == 0) {
            ReentrantReadWriteLock lock = null;
            String address = container.getAddressAsString();
            SwitchContainer sc = (SwitchContainer) container;
            try {
                long start = System.currentTimeMillis();
                lock = factory.getLock();
                lock.writeLock().lock();
                long gotLock = System.currentTimeMillis();
                factory.getDevicePath(address).open();
                byte[] state = sc.readDevice();
                channelCount = sc.getNumberChannels(state);
                long now = System.currentTimeMillis();
                logger.info(address + " has " + channelCount + " channel[s], took us " + (now - start) + "ms to figure out (" + (gotLock - start) + " to get the lock, " + (now - gotLock) + " to retrieve)");
            } catch (Throwable t) {
                logger.warn(address + ": can't retrieve channel count (assuming 2):", t);
                channelCount = 2;
            } finally {
                if (lock != null) {
                    lock.writeLock().unlock();
                }
            }
        }
        return channelCount;
    }

    /**
     * Read channel.
     * 
     * @param channel Channel to read.
     * @exception IOException if there was a problem reading the device.
     * @return Channel value.
     */
    public final boolean read(final int channel) throws IOException {
        ReentrantReadWriteLock lock = null;
        SwitchContainer sc = (SwitchContainer) container;
        String address = container.getAddressAsString();
        NDC.push("read(" + address + ":" + channel + ")");
        long start = System.currentTimeMillis();
        try {
            lock = factory.getLock();
            lock.writeLock().lock();
            logger.debug("got lock in " + (System.currentTimeMillis() - start) + "ms");
            start = System.currentTimeMillis();
            factory.getDevicePath(address).open();
            byte[] state = sc.readDevice();
            logger.debug("readDevice: " + (System.currentTimeMillis() - start));
            boolean result = sc.getLatchState(channel, state);
            logger.debug("state=" + result);
            return result;
        } catch (Throwable t) {
            IOException secondary = new IOException("Unable to read " + container);
            secondary.initCause(t);
            throw secondary;
        } finally {
            if (lock != null) {
                lock.writeLock().unlock();
            }
            logger.debug("complete in " + (System.currentTimeMillis() - start) + "ms");
            NDC.pop();
        }
    }

    /**
     * Write channel.
     * 
     * @param channel
     *            Channel to write.
     * @param value
     *            Value to write.
     * @exception IOException
     *                if there was a problem writing to the device.
     */
    public final void write(final int channel, final boolean value) throws IOException {
        ReentrantReadWriteLock lock = null;
        SwitchContainer sc = (SwitchContainer) container;
        String address = container.getAddressAsString();
        NDC.push("write(" + address + ":" + channel + ", " + value + ")");
        long start = System.currentTimeMillis();
        try {
            lock = factory.getLock();
            lock.writeLock().lock();
            logger.debug("got lock in " + (System.currentTimeMillis() - start) + "ms");
            start = System.currentTimeMillis();
            factory.getDevicePath(address).open();
            byte[] state = sc.readDevice();
            if (logger.isDebugEnabled()) {
                logger.debug("readDevice/1: " + dumpState(state) + " " + (System.currentTimeMillis() - start) + "ms");
            }
            boolean smart = sc.hasSmartOn();
            sc.setLatchState(channel, value, smart, state);
            if (logger.isDebugEnabled()) {
                logger.debug("writeDevice:  " + dumpState(state));
            }
            sc.writeDevice(state);
            state = sc.readDevice();
            if (logger.isDebugEnabled()) {
                logger.debug("readDevice/2: " + dumpState(state) + " " + (System.currentTimeMillis() - start) + "ms");
            }
            if (value == sc.getLatchState(channel, state)) {
                return;
            }
            logger.error("Failed to write " + container);
        } catch (Throwable t) {
            IOException secondary = new IOException("Unable to write " + container);
            secondary.initCause(t);
            throw secondary;
        } finally {
            if (lock != null) {
                lock.writeLock().unlock();
            }
            logger.debug("complete in " + (System.currentTimeMillis() - start) + "ms");
            NDC.pop();
        }
    }

    private String dumpState(byte[] state) {
        StringBuilder sb = new StringBuilder();
        if (state == null) {
            sb.append("<null>");
        } else {
            sb.append("(");
            for (int offset = 0; offset < state.length; offset++) {
                if (offset > 0) {
                    sb.append(" ");
                }
                sb.append("0x").append(Integer.toHexString(state[offset] & 0xFF));
            }
            sb.append(")");
        }
        return sb.toString();
    }

    /**
     * Reset the device. In other words, set all channels to 0.
     * 
     * @exception IOException
     *                if there was an exception writing the device.
     */
    public final void reset() throws IOException {
        for (int channel = 0; channel < getChannelCount(); channel++) {
            write(channel, false);
        }
    }
}
