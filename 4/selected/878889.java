package com.cyberoblivion.comm.plc.modbus;

import com.cyberoblivion.modsynch.events.ProgressEventListener;
import com.cyberoblivion.modsynch.events.ProgressEvent;
import com.cyberoblivion.modsynch.common.ApplicationError;
import javax.swing.event.EventListenerList;
import java.net.*;
import org.apache.log4j.Logger;

/**
 * Describe class ExecuteModbusCommand here.
 *
 * This class takes a modbustcpdevice and will write to a specified
 * address then wait for a particular value in another register to
 * complete the query.
 *
 * Created: Mon Jan 16 19:49:09 2006
 *
 * @author <a href="mailto:bene@velvet.cyberoblivion.com">Ben Erridge</a>
 * @version 1.0
 */
public class ExecuteModbusCommand implements Runnable {

    /**
    * It is intended that this
    * logger will be shut off in favor of the global logger during
    * production. */
    public static Logger logger = Logger.getLogger(ExecuteModbusCommand.class);

    /**
    * It is intended that
    * this logger will be shut off in favor of the global logger during
    * production.  The logger guarantees that loggers obtained with the
    * same name return the same logger instance.*/
    public static Logger executeModbusCommand;

    static {
        executeModbusCommand = Logger.getLogger(ExecuteModbusCommand.class);
    }

    /**
    * Describe variable <code>readOffset</code> here.
    * offser into plc sometimes refered to as Reference number
    */
    private int readOffset;

    /**
    * Describe variable <code>writeOffset</code> here.
    * where to write the command into the plc
    */
    private int writeOffset;

    /**
    * Describe variable <code>device</code> here.
    * device which speaks modbusTCP protocol
    */
    private ModbusTCPDevice device = null;

    /**
    * Describe variable <code>completionValue</code> here.  this is
    * the value in the readOffset register which means we have
    * completed the command
    */
    private byte completionValue = 100;

    /**
    * Describe variable <code>data</code> here.
    * The command to execute
    */
    private byte[] command;

    /**
    * add ability to subscribe to the progress events
    **/
    protected EventListenerList listenerList = new EventListenerList();

    /**
    * Describe variable <code>status</code> here.
    * what the status of the command is
    */
    private int status = 0;

    /**
    * Describe variable <code>finished</code> here.
    * this goes to true when we read the completionValue from the PLC
    */
    private boolean finished = false;

    /**
    * Describe variable <code>sleepTime</code> here.
    * This is the time between checking the progress of the command
    */
    private long sleepTime = 1000;

    private boolean stop = false;

    /**
    * Describe constant <code>PROGRESS_REGISTER_SIZE</code> here.  how
    * many registers to read when checking for the completion command
    */
    private static final int PROGRESS_REGISTER_SIZE = 2;

    /**
    * Creates a new <code>ExecuteModbusCommand</code> instance.
    *
    * @param device a <code>ModbusTCPDevice</code> value
    * @param readOffset an <code>int</code> value the register offset
    * to read the completion command from
    * @param writeOffset an <code>int</code> value the offset to write
    * the command into the plc
    * @param completionValue a <code>byte</code> value value of
    * copletion register which means we have finished executing
    * command
    * @param command a <code>byte[]</code> value this is the command
    * to write into the plc
    */
    public ExecuteModbusCommand(ModbusTCPDevice device, int readOffset, int writeOffset, byte completionValue, byte[] command) {
        this.device = device;
        this.writeOffset = writeOffset;
        this.readOffset = readOffset;
        this.completionValue = completionValue;
        this.command = command;
    }

    /**
    * <code>run</code> This will write to the device and wait for
    * the request to sending progress updates to listeners all the
    * while
    *
    */
    public void run() {
        boolean res = false;
        byte regData[];
        int i = 0;
        InetAddress a = null;
        try {
            device.connect();
            res = ModbusTCPDevice.writeMultiReg(writeOffset, device.connection, command, (char) command.length);
        } catch (Exception e) {
            throw new ApplicationError(ApplicationError.CAT_PLC, e);
        }
        if (res) {
            try {
                while (!finished & !stop) {
                    Thread.sleep(sleepTime);
                    logger.debug(device.getIPAddress() + " " + readOffset);
                    regData = ModbusTCPDevice.readMultiReg(readOffset, device.connection, PROGRESS_REGISTER_SIZE);
                    if (logger.isDebugEnabled()) {
                        String debugInfo = "data buffer = ";
                        for (i = 0; i < regData.length; i++) {
                            debugInfo += " " + (int) regData[i];
                        }
                        logger.debug(debugInfo);
                    }
                    if (regData[1] != status & regData[1] != completionValue) {
                        logger.info("Status Update!!");
                        status = (int) regData[1];
                        fireProgressEvent(new ProgressEvent(this, status, false));
                    }
                    if (false) {
                        finished = true;
                        status = (int) regData[1];
                        fireProgressEvent(new ProgressEvent(this, status, true));
                    }
                }
            } catch (Exception e) {
                throw new ApplicationError(ApplicationError.CAT_PLC, e);
            }
        }
        device.disconnect();
    }

    /**
    * Describe <code>stop</code> method here.  will stop the thread
    * from executing and checking status of the executed command
    */
    public void stop() {
        this.stop = true;
    }

    /**
    * Get the <code>WriteOffset</code> value.
    *
    * @return an <code>int</code> value
    */
    public final int getWriteOffset() {
        return writeOffset;
    }

    /**
    * Set the <code>WriteOffset</code> value.
    *
    * @param newWriteOffset The new WriteOffset value.
    */
    public final void setWriteOffset(final int newWriteOffset) {
        this.writeOffset = newWriteOffset;
    }

    /**
    * Get the <code>ReadOffset</code> value.
    *
    * @return an <code>int</code> value
    */
    public final int getReadOffset() {
        return readOffset;
    }

    /**
    * Set the <code>ReadOffset</code> value.
    *
    * @param newReadOffset The new ReadOffset value.
    */
    public final void setReadOffset(final int newReadOffset) {
        this.readOffset = newReadOffset;
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
    * methods to send progress events
    *
    * @param listener a <code>ProgressEventListener</code> value
    */
    public void addProgressEventListener(ProgressEventListener listener) {
        listenerList.add(ProgressEventListener.class, listener);
    }

    /**
    * This methods allows classes to unregister for ProgressEvents
    *
    * @param listener a <code>ProgressEventListener</code> value
    */
    public void removeProgressEventListener(ProgressEventListener listener) {
        listenerList.remove(ProgressEventListener.class, listener);
    }

    /**
    * private class to let subscribers know when Progress is made
    * @param evt a <code>ProgressEvent</code> value
    */
    private void fireProgressEvent(ProgressEvent evt) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = 0; i < listeners.length; i += 2) {
            if (listeners[i] == ProgressEventListener.class) {
                ((ProgressEventListener) listeners[i + 1]).ProgressEventOccurred(evt);
            }
        }
    }
}
