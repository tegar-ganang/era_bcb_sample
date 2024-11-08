package com.cyberoblivion.comm.plc.modbus;

import com.cyberoblivion.modsynch.events.ValueChangeEventListener;
import com.cyberoblivion.modsynch.events.ValueChangeEvent;
import com.cyberoblivion.modsynch.common.ApplicationError;
import java.io.IOException;
import javax.swing.event.EventListenerList;
import org.apache.log4j.Logger;

/**
 * Describe class ModbusSynchronizer here.
 *
 *
 * Created: Tue Feb  7 08:18:51 2006
 *
 * @author <a href="mailto:bene@velvet.cyberoblivion.com">Ben Erridge</a>
 * @version 1.0
 */
public class ModbusSynchronizer implements Runnable {

    /**
     * Describe variable <code>logger</code> here.
     * logger for this class
     */
    public static Logger logger = Logger.getLogger(ModbusSynchronizer.class);

    /**
     * Describe variable <code>listenerList</code> here.
     *  add ability to subscribe to events
     */
    protected EventListenerList listenerList = new EventListenerList();

    /**
     * Describe variable <code>stop</code> here.
     * Stop this class from updating
     */
    private boolean stop = false;

    /**
     * Describe variable <code>sleepTime</code> here.
     * This is the time between checking for value changes
     */
    private long sleepTime = DEFAULT_SLEEP_TIME;

    /**
     * Describe variable <code>sleepTime</code> here.
     * This is the time between checking for value changes
     */
    private static long DEFAULT_SLEEP_TIME = 1000;

    /**
     * Describe constant <code>MAX_REGISTER_SETS</code> here.
     * this is the max number of register sets we will allowed per device
     */
    public static final int MAX_REGISTER_SETS = 10;

    /**
     * Describe variable <code>registers</code> here.
     * these are the values from the plc
     */
    private byte[][] registers = new byte[MAX_REGISTER_SETS][];

    /**
     * Describe variable <code>readOffset</code> here.
     * this is an array of offsets for reading in the plc
     */
    private int[] readOffset = new int[MAX_REGISTER_SETS];

    /**
     * Describe variable <code>device</code> here.
     * this is the modbus device we are synchronizing to
     */
    private ModbusTCPDevice device;

    /**
     * Describe variable <code>registerSetID</code> here.
     * this is the next register set we will add
     */
    private int registerSetID = 0;

    private int synchDirection[] = new int[MAX_REGISTER_SETS];

    /**
     *  constant <code>SYNCH_PC_TO_PLC</code> .
     *  id to synch PC registers to PLC registers
     */
    public static final int SYNCH_PC_TO_PLC = 0;

    /**
     *  constant <code>SYNCH_PlC_TO_PC</code> .
     *  ID to synch PLC registers to pc registers
     */
    public static final int SYNCH_PlC_TO_PC = 1;

    /**
     *  constant <code>DEFAULT_SYNCH_DIRECTION</code> .
     *
     */
    public static final int DEFAULT_SYNCH_DIRECTION = 0;

    private int inbetweenDatasetSleepTime = DEFAULT_IN_BETWEEN_DC_SLEEP;

    public static int DEFAULT_IN_BETWEEN_DC_SLEEP = 150;

    /**
     * Creates a new <code>ModbusSynchronizer</code> instance.
     * Using the default sleeptime, and synch direction
     * @param device a <code>ModbusTCPDevice</code> value
     * @param readOffset an <code>int</code> value
     * @param registers a <code>byte[]</code> value
     */
    public ModbusSynchronizer(ModbusTCPDevice device, int readOffset, byte[] registers) {
        this(device, readOffset, registers, DEFAULT_SLEEP_TIME, DEFAULT_SYNCH_DIRECTION);
    }

    /**
     * Creates a new <code>ModbusSynchronizer</code> instance.
     * uses default synchtime
     * @param device a <code>ModbusTCPDevice</code> value
     * @param readOffset an <code>int</code> value
     * @param registers a <code>byte[]</code> value
     * @param synchDirection an <code>int</code> value
     */
    public ModbusSynchronizer(ModbusTCPDevice device, int readOffset, byte[] registers, int synchDirection) {
        this(device, readOffset, registers, DEFAULT_SLEEP_TIME, synchDirection);
    }

    /**
     * Describe <code>addSynchronousRegisterSet</code> method here.
     * adds a register set to synchronize to plc registers
     * @param readOffset an <code>int</code> value
     * @param registers a <code>byte[]</code> value
     * @param mysynchDirection 
     * @return an <code>int</code> value the register set index into
     * the arrays
     */
    public int addSynchronousRegisterSet(int readOffset, byte[] registers, int mysynchDirection) {
        if (registerSetID < MAX_REGISTER_SETS) {
            this.registerSetID++;
            this.readOffset[registerSetID] = readOffset;
            this.registers[registerSetID] = registers;
            this.synchDirection[registerSetID] = mysynchDirection;
            return registerSetID;
        }
        return -1;
    }

    /**
     *  <code>addSynchronousRegisterSet</code> method .
     *
     * @param readOffset an <code>int</code> value
     * @param registers a <code>byte[]</code> value
     * @return an <code>int</code> value
     */
    public int addSynchronousRegisterSet(int readOffset, byte[] registers) {
        return this.addSynchronousRegisterSet(readOffset, registers, DEFAULT_SYNCH_DIRECTION);
    }

    /**
     * Creates a new <code>ModbusSynchronizer</code> instance.
     * Using the given sleeptime.
     * @param device a <code>ModbusTCPDevice</code> value
     * @param readOffset an <code>int</code> value
     * @param registers a <code>byte[]</code> value Must be divisible by 2
     * in order to only words from plc
     * @param sleepTime a <code>long</code> value
     * @param mysynchDirection 
     */
    public ModbusSynchronizer(ModbusTCPDevice device, int readOffset, byte[] registers, long sleepTime, int mysynchDirection) {
        this.device = device;
        this.readOffset[0] = readOffset;
        this.registers[0] = registers;
        this.sleepTime = sleepTime;
        this.synchDirection[0] = mysynchDirection;
    }

    /**
     * Describe <code>run</code> method here.  function continually
     * updates arrays to register values
     */
    public final void run() {
        boolean res = false;
        byte regData[];
        int i = 0;
        try {
            res = device.connect();
            logger.warn("device could not connect in ModbusSynchronizer " + res);
        } catch (Exception e) {
            logger.error("unable to connect to device" + e.toString());
            throw new ApplicationError(ApplicationError.CAT_PLC, e);
        }
        try {
            while (!stop) {
                for (i = 0; i <= registerSetID; i++) {
                    regData = device.readMultiReg(readOffset[i], registers[i].length);
                    if (updateChanges(registers[i], regData, this.synchDirection[i], i)) {
                        logger.debug("found change in regset ID " + i + " SynchDirection " + this.synchDirection[i]);
                        fireValueChangeEvent(new ValueChangeEvent(this, i));
                    }
                    Thread.sleep(inbetweenDatasetSleepTime);
                }
                Thread.sleep(sleepTime);
            }
        } catch (Exception e) {
            throw new ApplicationError(ApplicationError.CAT_PLC, e);
        }
        device.disconnect();
    }

    /**
     * This function
     * checks for differences in byte arrays if there is a difference
     * it returns true and sets last = current in the case of synching
     * pc to plc and writes the registers to the plc if we are synching
     * plc to pc
     *
     * @param last a <code>byte[]</code> value the last
     * bytes we received from the PLC
     * @param current a <code>byte[]</code> value
     * the latest bytes from the plc
     * @param mysynchDirection 
     * @param regsetID an <code>int</code> value
     * @return a <code>boolean</code> value true if a
     * current value is different from a last value
     * @exception IOException if an error occurs
     */
    public boolean updateChanges(byte[] last, byte[] current, int mysynchDirection, int regsetID) throws IOException {
        int i = 0;
        boolean res = false;
        for (i = 0; i < last.length; i++) {
            if (last[i] != current[i]) {
                res = true;
                if (mysynchDirection == SYNCH_PlC_TO_PC) {
                    last[i] = current[i];
                } else if (mysynchDirection == SYNCH_PC_TO_PLC) {
                    logger.info("--writing because of bit " + i + "  " + last[i] + " " + current[i] + " off " + readOffset[regsetID] + " reglen " + registers[regsetID].length);
                    device.writeMultiReg(readOffset[regsetID], registers[regsetID], registers[regsetID].length);
                    break;
                }
            }
        }
        return res;
    }

    /**
     * Get the <code>SleepTime</code> value.
     *
     * @return a <code>long</code> value
     */
    public final long getSleepTime() {
        return sleepTime;
    }

    /**
     * Set the <code>SleepTime</code> value.
     *
     * @param newSleepTime The new SleepTime value.
     */
    public final void setSleepTime(final long newSleepTime) {
        this.sleepTime = newSleepTime;
    }

    /**
     * Get the <code>Device</code> value.
     *
     * @return a <code>ModbusTCPDevice</code> value
     */
    public final ModbusTCPDevice getDevice() {
        return device;
    }

    /**
     * Set the <code>Device</code> value.
     *
     * @param newDevice The new Device value.
     */
    public final void setDevice(final ModbusTCPDevice newDevice) {
        this.device = newDevice;
    }

    /**
     * Describe <code>stop</code> method here.  will stop the thread
     * from executing and checking status of the registers
     */
    public void stop() {
        this.stop = true;
    }

    /**
     * methods to send progress events
     *
     * @param listener a <code>ValueChangeEventListener</code> value
     */
    public void addValueChangeEventListener(ValueChangeEventListener listener) {
        listenerList.add(ValueChangeEventListener.class, listener);
    }

    /**
     * Describe <code>removeValueChangeEventListener</code> method here.
     * This methods allows classes to unregister events
     * @param listener a <code>ValueChangeEventListener</code> value
     */
    public void removeValueChangeEventListener(ValueChangeEventListener listener) {
        listenerList.remove(ValueChangeEventListener.class, listener);
    }

    /**
     * Describe <code>fireValueChangeEvent</code> method here.
     * function fires events to the listeners
     * @param evt a <code>ValueChangeEvent</code> value
     */
    private void fireValueChangeEvent(ValueChangeEvent evt) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = 0; i < listeners.length; i += 2) {
            if (listeners[i] == ValueChangeEventListener.class) {
                ((ValueChangeEventListener) listeners[i + 1]).ValueChangeEventOccurred(evt);
            }
        }
    }

    public int getInbetweenDatasetSleepTime() {
        return inbetweenDatasetSleepTime;
    }

    public void setInbetweenDatasetSleepTime(int inbetweenDatasetSleepTime) {
        this.inbetweenDatasetSleepTime = inbetweenDatasetSleepTime;
    }
}
