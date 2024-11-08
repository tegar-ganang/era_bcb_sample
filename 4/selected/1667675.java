package net.sf.smartcrib.dmx;

import org.hibernate.validator.Length;
import javax.persistence.*;
import java.util.*;
import java.io.IOException;

/**
 * A DMX device such as switch or dimmer and controlled through a {@link DMXController}.
 * @author Adrian BER
 */
@Entity
public class DMXDevice {

    /** The possible existing types for this device. */
    public static enum Type {

        SWITCH(false, 0, 255), DIMMER(false, 0, 255), SENSOR(true, 0, 255);

        private int minValue = 0;

        private int maxValue = 255;

        private boolean readOnly = false;

        Type(boolean readOnly, int minValue, int maxValue) {
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.readOnly = readOnly;
        }

        public int getMinValue() {
            return minValue;
        }

        public void setMinValue(int minValue) {
            this.minValue = minValue;
        }

        public int getMaxValue() {
            return maxValue;
        }

        public void setMaxValue(int maxValue) {
            this.maxValue = maxValue;
        }

        public boolean isReadOnly() {
            return readOnly;
        }

        public void setReadOnly(boolean readOnly) {
            this.readOnly = readOnly;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(length = 100)
    @Length(max = 100)
    private String controllerAddress = "usbdmx:0";

    @Transient
    private DMXController controller;

    private short channel = -1;

    @Column(length = 100)
    @Length(max = 100)
    private String name;

    @Column(length = 255)
    @Length(max = 255)
    private String description;

    @Enumerated(EnumType.ORDINAL)
    private Type type = Type.SWITCH;

    private int minValue = -1;

    private int maxValue = -1;

    @Transient
    private transient int value = -1;

    @Transient
    private List<DMXDeviceListener> listeners = new ArrayList<DMXDeviceListener>();

    public DMXDevice() {
    }

    public DMXDevice(short channel) {
        setChannel(channel);
    }

    public DMXDevice(String controllerAddress, short channel) {
        setControllerAddress(controllerAddress);
        setChannel(channel);
    }

    public Long getId() {
        return id;
    }

    /** The associated controlling device.
     * @return the DMX controller device.
     */
    private DMXController getController() {
        if (controller == null) {
            try {
                DMXControllerFactory.getInstance().open(controllerAddress);
            } catch (IOException e) {
            }
        }
        return controller;
    }

    public String getControllerAddress() {
        return controllerAddress;
    }

    public void setControllerAddress(String controllerAddress) {
        this.controllerAddress = controllerAddress;
        this.controller = null;
    }

    /** @return the DMX channel address for this device. */
    public short getChannel() {
        return channel;
    }

    public void setChannel(short channel) {
        this.channel = channel;
    }

    /** @return the name for this device. */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /** @return the description for this device. */
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns the DMX value for this device by interrogating the DMX daisy chain.
     * This is to be used ONLY when recovering from a failure or when starting the system.
     * @return the DMX value
     * @throws IOException if an error occured
     */
    private int getHardwareValue() throws IOException {
        byte[] data = new byte[2];
        if (getChannel() <= 256) {
            data[0] = 0x52;
            data[1] = (byte) (getChannel() - 1);
        } else if (getChannel() <= 512) {
            data[0] = 0x53;
            data[1] = (byte) (getChannel() - 1);
        }
        getController().getOutputStream().write(data);
        byte[] response = new byte[3];
        if (getController().getInputStream().read(response) == 3) return response[2] & 0xff; else return value;
    }

    public int getValue() {
        if (value < 0) {
            try {
                value = getHardwareValue();
            } catch (IOException e) {
            }
        }
        return value;
    }

    public void setValue(int value) throws IOException {
        if (type.isReadOnly()) {
            return;
        }
        if (value < getMinValue()) value = getMinValue();
        if (value > getMaxValue()) value = getMaxValue();
        int oldValue = this.value;
        byte[] data = new byte[3];
        if (getChannel() <= 256) {
            data[0] = 0x48;
            data[1] = (byte) (getChannel() - 1);
            data[2] = (byte) value;
        } else if (getChannel() <= 512) {
            data[0] = 0x49;
            data[1] = (byte) (getChannel() - 1);
            data[2] = (byte) value;
        }
        getController().getOutputStream().write(data);
        this.value = value;
        fireValueChanged(oldValue);
    }

    public void switchValue() throws IOException {
        if (getValue() > getMinValue()) setValue(getMinValue()); else setValue(getMaxValue());
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public int getMinValue() {
        return minValue < 0 ? type.getMinValue() : minValue;
    }

    public void setMinValue(int minValue) {
        int v = getValue();
        if (v < minValue) throw new IllegalArgumentException("Minimmum value cannot be higher than current value of " + v);
        this.minValue = minValue;
    }

    public int getMaxValue() {
        return maxValue < 0 ? type.getMaxValue() : maxValue;
    }

    public void setMaxValue(int maxValue) {
        int v = getValue();
        if (v > maxValue) throw new IllegalArgumentException("Maximmum value cannot be lower than current value of " + v);
        this.maxValue = maxValue;
    }

    /**
     * Adds a listener to this device.
     * @param listener the listener to be added
     */
    public void addListener(DMXDeviceListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes a listener from this device.
     * @param listener the listener to be removed
     */
    public void removeListener(DMXDeviceListener listener) {
        listeners.remove(listener);
    }

    /**
     * Fires the valuChanged event by calling all the listeners.
     * @param oldValue the DMX value for this device before the event.
     */
    protected void fireValueChanged(int oldValue) {
        DMXDeviceEvent event = new DMXDeviceEvent(this, oldValue);
        for (DMXDeviceListener listener : listeners) {
            listener.valueChanged(event);
        }
    }
}
