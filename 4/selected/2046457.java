package net.sf.dz3.device.sensor.impl.xbee;

import java.io.IOException;
import net.sf.dz3.device.sensor.DzSwitchContainer;
import net.sf.dz3.device.sensor.impl.AbstractDeviceContainer;
import org.apache.log4j.Logger;
import org.apache.log4j.NDC;
import com.rapplogic.xbee.api.AtCommandResponse;
import com.rapplogic.xbee.api.XBeeAddress64;
import com.rapplogic.xbee.api.zigbee.ZNetRemoteAtRequest;

/**
 * A platform independent switch container.
 */
public class XBeeSwitchContainer extends AbstractDeviceContainer implements DzSwitchContainer {

    private final Logger logger = Logger.getLogger(getClass());

    private final XBeeDeviceFactory factory;

    private final XBeeAddress64 xbeeAddress;

    /**
     * Create an instance.
     * 
     * @param container
     *            1-Wire API container to base this container on.
     */
    public XBeeSwitchContainer(final XBeeDeviceFactory factory, final XBeeAddress64 address) {
        this.factory = factory;
        this.xbeeAddress = address;
    }

    public final String getType() {
        return "S";
    }

    /**
     * @return Number of channels the device has.
     */
    public final synchronized int getChannelCount() {
        return 4;
    }

    /**
     * Read channel.
     * 
     * @param channel Channel to read.
     * @exception IOException if there was a problem reading the device.
     * @return Channel value.
     */
    public final boolean read(final int channel) throws IOException {
        String address = Parser.render4x4(xbeeAddress);
        NDC.push("read(" + address + ":" + channel + ")");
        long start = System.currentTimeMillis();
        String target = "D" + channel;
        try {
            ZNetRemoteAtRequest request = new ZNetRemoteAtRequest(xbeeAddress, target);
            AtCommandResponse rsp = (AtCommandResponse) factory.sendSynchronous(request, 5000);
            logger.info(target + " response: " + rsp);
            if (rsp.isError()) {
                throw new IOException(target + " + query failed, status: " + rsp.getStatus());
            }
            int buffer[] = rsp.getValue();
            if (buffer.length != 1) {
                throw new IOException("Unexpected buffer size " + buffer.length);
            }
            switch(buffer[0]) {
                case 4:
                    return false;
                case 5:
                    return true;
                default:
                    throw new IOException(target + " is not configured as switch, state is " + buffer[0]);
            }
        } catch (Throwable t) {
            IOException secondary = new IOException("Unable to read " + address + ":" + channel);
            secondary.initCause(t);
            throw secondary;
        } finally {
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
        String address = Parser.render4x4(xbeeAddress);
        NDC.push("write(" + address + ":" + channel + ", " + value + ")");
        long start = System.currentTimeMillis();
        String target = "D" + channel;
        try {
            ZNetRemoteAtRequest request = new ZNetRemoteAtRequest(xbeeAddress, target, new int[] { value ? 5 : 4 });
            AtCommandResponse rsp = (AtCommandResponse) factory.sendSynchronous(request, 5000);
            logger.info(target + " response: " + rsp);
            if (rsp.isError()) {
                throw new IOException(target + " + query failed, status: " + rsp.getStatus());
            }
        } catch (Throwable t) {
            IOException secondary = new IOException("Unable to write " + address);
            secondary.initCause(t);
            throw secondary;
        } finally {
            logger.debug("complete in " + (System.currentTimeMillis() - start) + "ms");
            NDC.pop();
        }
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

    @Override
    public String getAddress() {
        return Parser.render4x4(xbeeAddress);
    }

    @Override
    public String getName() {
        return "XBee Switch";
    }
}
