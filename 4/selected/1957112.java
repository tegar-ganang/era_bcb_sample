package de.genodeftest.k8055_old;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Thread.State;
import java.util.Vector;
import javax.swing.JOptionPane;
import de.genodeftest.k8055_old.IOEvent.AnalogAllEvent;
import de.genodeftest.k8055_old.IOEvent.AnalogEvent;
import de.genodeftest.k8055_old.IOEvent.CounterAllEvent;
import de.genodeftest.k8055_old.IOEvent.CounterEvent;
import de.genodeftest.k8055_old.IOEvent.DigitalAllEvent;
import de.genodeftest.k8055_old.IOEvent.DigitalEvent;
import de.genodeftest.k8055_old.JWrapperK8055.AnalogInput;
import de.genodeftest.k8055_old.JWrapperK8055.AnalogOutput;
import de.genodeftest.k8055_old.JWrapperK8055.Counter;
import de.genodeftest.k8055_old.JWrapperK8055.DigitalInput;
import de.genodeftest.k8055_old.driver.ByMikePeter;
import de.genodeftest.k8055_old.driver.JNA_DirectMapped;

/**
 * @author genodeftest (Christian Stadelmann)
 * @since 05.11.2010
 */
public class InputOutputAdapter {

    /**
	 * used for implementing ChangeListener<br>
	 * accessed by both IOThread and AWTEventThread(Event-Dispatching-Thread)<br>
	 * <b>access only via synchronized blocks</b>
	 */
    protected volatile Vector<IOListener> listenerList = new Vector<IOListener>();

    /**
	 * used to notify the IOThread that a value changed<br>
	 * accessed by both IOThread and AWTEventThread(Event-Dispatching-Thread)<br>
	 * <b>access only via synchronized blocks</b>
	 */
    private volatile Boolean outputChanged = true, resetCounter1 = true, resetCounter2 = true;

    /**
	 * used to submit new values of Analog Outputs to IOThread<br>
	 * accessed by both IOThread and AWTEventThread(Event-Dispatching-Thread)<br>
	 * <b>access only via synchronized blocks</b>
	 */
    private volatile Short[] analogOut = new Short[] { 0, 0 };

    /**
	 * used to transmit new values of Counter Debounce Time to IOThread<br>
	 * accessed by both IOThread and AWTEventThread(Event-Dispatching-Thread)<br>
	 * <b>access only via synchronized blocks</b>
	 */
    private volatile Short[] counterDebounce = new Short[] { 0, 0 };

    /**
	 * used to transmit new values of Digital Outputs to IOThread<br>
	 * accessed by both IOThread and AWTEventThread(Event-Dispatching-Thread)<br>
	 * <b>access only via synchronized blocks</b>
	 */
    private volatile Boolean[] digitalOut = new Boolean[] { false, false, false, false, false, false, false, false };

    private volatile IOThread ioThread;

    private volatile Long debounceTime = 10L;

    private volatile Boolean pauseIOThread = Boolean.TRUE;

    public enum Drivers {

        JNA_With_DirectMapping_Default, K8055_JWrapper, Other
    }

    /**
	 * Adds an order to set the specified boolean value at the Digital Output. This may take
	 * some time, depending on settings made to K8055Connection.java. It can last up to 10
	 * seconds to change the value.
	 * 
	 * @param channelNo channel number(1 - 8)
	 * @param value new value for Analog Output on channel <code>channelNo</code>.<br>
	 *        <code>true</code> for ON<br>
	 *        <code>false</code>for OFF
	 * @throws ArithmeticException if parameter is out of range
	 */
    public void setValue_Digital(int channelNo, boolean value) {
        if (channelNo < 1 | channelNo > 8) throw new ArithmeticException("channelNo out of range");
        synchronized (digitalOut) {
            digitalOut[channelNo - 1] = value;
        }
        synchronized (outputChanged) {
            outputChanged = true;
        }
        Thread.yield();
    }

    /**
	 * Adds an order to set the specified value at the Analog Output. This may take some time,
	 * depending on settings made to K8055Connection.java. It can last up to 10 seconds to
	 * change the value.
	 * 
	 * @param channel AnalogInput
	 * @param value a positive integer from 0 to 255 as new Analog Output on channel
	 *        <code>channelNo</code>.
	 * @throws ArithmeticException if parameter is out of range
	 */
    public void setValue_Analog(AnalogOutput channel, short value) {
        if (value < 0 | value > 255) throw new ArithmeticException("parameter out of range");
        synchronized (analogOut) {
            analogOut[channel.channelNo - 1] = value;
        }
        synchronized (outputChanged) {
            outputChanged = true;
        }
        Thread.yield();
    }

    /**
	 * Adds an order to set the specified values at the Digital Output. This may take some time,
	 * depending on settings made to K8055Connection.java. It can last up to 10 seconds to
	 * change the value.
	 * 
	 * @param value1 new value for channel 1
	 * @param value2 new value for channel 2
	 * @param value3 new value for channel 3
	 * @param value4 new value for channel 4
	 * @param value5 new value for channel 5
	 * @param value6 new value for channel 6
	 * @param value7 new value for channel 7
	 * @param value8 new value for channel 8
	 */
    public void setAll_Digital(boolean value1, boolean value2, boolean value3, boolean value4, boolean value5, boolean value6, boolean value7, boolean value8) {
        synchronized (digitalOut) {
            digitalOut[0] = value1;
            digitalOut[1] = value2;
            digitalOut[2] = value3;
            digitalOut[3] = value4;
            digitalOut[4] = value5;
            digitalOut[5] = value6;
            digitalOut[6] = value7;
            digitalOut[7] = value8;
        }
        synchronized (outputChanged) {
            outputChanged = true;
        }
        Thread.yield();
    }

    /**
	 * Adds an order to set the specified values at the Digital Output. This may take some time,
	 * depending on settings made to K8055Connection.java. It can last up to 10 seconds to
	 * change the value.
	 * 
	 * @param values boolean Array with the size 8
	 * @throws IllegalArgumentException if <code>values</code> does not have size '8'
	 */
    public void setAll_Digital(boolean[] values) {
        if (values.length != 8) throw new IllegalArgumentException("values does not have size 8");
        synchronized (digitalOut) {
            for (int i = 0; i < 8; i++) {
                digitalOut[i] = values[i];
            }
        }
        synchronized (outputChanged) {
            outputChanged = true;
        }
        Thread.yield();
    }

    /**
	 * Adds an order to set the specified values at the Analog Output. This may take some time,
	 * depending on settings made to K8055Connection.java. It can last up to 10 seconds to
	 * change the value.
	 * 
	 * @param value1 a positive integer from 0 to 255 as new Analog Output on channel 1.
	 * @param value2 a positive integer from 0 to 255 as new Analog Output on channel 2.
	 * @throws ArithmeticException if parameter is out of range
	 */
    public void setAll_Analog(short value1, short value2) {
        if (value1 < 0 | value1 > 255 | value2 < 0 | value2 > 255) throw new ArithmeticException("parameter out of range");
        synchronized (analogOut) {
            analogOut[0] = value1;
            analogOut[1] = value2;
        }
        synchronized (outputChanged) {
            outputChanged = true;
        }
        Thread.yield();
    }

    /**
	 * Adds an order to set the specified value for Counter Debounce Time. This may take some
	 * time, depending on settings made to K8055Connection.java. It can last up to 10 seconds to
	 * change the value.
	 * 
	 * @param channelNo channel number(1 or 2)
	 * @param value a positive long as new Counter Debounce Time on channel
	 *        <code>channelNo</code>.
	 * @throws ArithmeticException if parameter is out of range
	 */
    public void setValue_CounterDebounce(int channelNo, short value) {
        if (value < 0) throw new ArithmeticException("parameter out of range");
        if (channelNo == 1) {
            synchronized (counterDebounce) {
                counterDebounce[0] = value;
            }
        } else if (channelNo == 2) {
            synchronized (counterDebounce) {
                counterDebounce[1] = value;
            }
        } else {
            throw new ArithmeticException("parameter out of range");
        }
        synchronized (outputChanged) {
            outputChanged = true;
        }
        Thread.yield();
    }

    /**
	 * Adds an order to set the specified values for Counter Debounce Time. This may take some
	 * time, depending on settings made to K8055Connection.java. It can last up to 10 seconds to
	 * change the value.
	 * 
	 * @param value1 a positive long as new Counter Debounce Time on channel 1.
	 * @param value2 a positive long as new Counter Debounce Time on channel 2.
	 * @throws ArithmeticException if parameter is out of range
	 */
    public void setValue_CounterDebounceAll(short value1, short value2) {
        if (value1 < 0 | value2 < 0) throw new ArithmeticException("parameter out of range");
        synchronized (counterDebounce) {
            counterDebounce[0] = value1;
        }
        synchronized (counterDebounce) {
            counterDebounce[1] = value2;
        }
        synchronized (outputChanged) {
            outputChanged = true;
        }
        Thread.yield();
    }

    /**
	 * Adds an order to reset the specified channel of the Counter. This may take some time,
	 * depending on settings made to K8055Connection.java. It can last up to 10 seconds to
	 * change the value.
	 * 
	 * @param channelNo number(1 or 2) of the channel to reset
	 * @throws ArithmeticException if parameter is out of range
	 */
    public void resetCounter(int channelNo) {
        if (channelNo != 1 & channelNo != 2) throw new ArithmeticException("channelNo out of range");
        if (channelNo == 1) {
            synchronized (resetCounter1) {
                resetCounter1 = true;
            }
        } else {
            synchronized (resetCounter2) {
                resetCounter2 = true;
            }
        }
        synchronized (outputChanged) {
            outputChanged = true;
        }
    }

    /**
	 * Adds a dataListener to the ListenersList of this class. Adding more Listeners does
	 * <b>not</b> reduce performance
	 * 
	 * @param l a {@link IOListener}
	 */
    public void addDataListener(IOListener l) {
        if (l == null) throw new NullPointerException();
        listenerList.add(l);
    }

    /**
	 * Removes a IOListener from internal ListenerList if it was added before.
	 * 
	 * @param l the IOListener to remove
	 * @throws NullPointerException if argument is <code>null<code>
	 */
    public void removeDataListener(IOListener l) {
        if (l == null) {
            throw new NullPointerException();
        }
        try {
            listenerList.remove(l);
        } catch (Exception e) {
            System.err.println("Listener was not added or already removed");
        }
    }

    public InputOutputAdapter(long debounceTime) {
        JWrapperK8055 driver = prepareDefaultJNADriver();
        if (driver == null) {
            JOptionPane.showMessageDialog(null, "Ein schwerwiegender Fehler ist aufgetreten:\n" + "Die ben�tigte Programmbibliothek K8055D konnte nicht gefunden\n" + "und in keinem der verf�gbaren Programmverzeichnisse erstellt werden\n" + "Wenn sie auf UNIX/Linux-Betriebssystemen arbeiten,\n" + "m�ssen sie das Programm mit Administratorrechten starten", "Schwerwiegender Fehler", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
        ioThread = new IOThread(driver);
        this.debounceTime = debounceTime;
        try {
            ioThread.closeConnection();
        } catch (Exception e) {
        }
        ioThread.start();
    }

    public void changeDriver(Drivers driver) throws Exception {
        securePauseIOThread();
        JWrapperK8055 jwrap = null;
        switch(driver) {
            case JNA_With_DirectMapping_Default:
                jwrap = prepareDefaultJNADriver();
                break;
            case K8055_JWrapper:
                jwrap = prepareJWrapperByMikePeter();
                break;
            case Other:
            default:
                throw new Exception("This Driver is not supported yet");
        }
        if (jwrap == null) throw new Exception("Failed to load Driver");
        ioThread = new IOThread(jwrap);
        ioThread.closeConnection();
        ioThread.start();
    }

    /**
	 * @author genodeftest (Christian Stadelmann)
	 * @since 04.01.2010
	 * @return true if the ioThread is running
	 */
    public boolean isIOThreadRunning() {
        return !pauseIOThread;
    }

    public void pauseIOThread(boolean pause) {
        synchronized (pauseIOThread) {
            pauseIOThread = pause;
        }
        if (!pause) synchronized (ioThread) {
            ioThread.notify();
        }
    }

    /**
	 * @author genodeftest (Christian Stadelmann)
	 * @since 05.01.2010
	 * @return previous state of isIOThreadRunning()
	 */
    public boolean securePauseIOThread() {
        boolean wasRunning = isIOThreadRunning();
        pauseIOThread(true);
        if (ioThread == null) return wasRunning;
        synchronized (ioThread) {
            try {
                ioThread.notify();
            } catch (Exception e) {
            }
        }
        while (ioThread.getState() == State.BLOCKED) Thread.yield();
        return wasRunning;
    }

    /**
	 * sets the minimum time interval between 2 circles of checking if data input(of
	 * hardware)changed.
	 * 
	 * @param value the time interval in milliseconds
	 */
    public void setDebounceTimeMillis(long value) {
        if (value < 0) throw new IllegalArgumentException("debounceTime can't be negative");
        synchronized (debounceTime) {
            debounceTime = value;
        }
        if (value == 0) ioThread.setPriority(Thread.MIN_PRIORITY); else ioThread.setPriority(Thread.MAX_PRIORITY);
    }

    /**
	 * @author genodeftest (Christian Stadelmann)
	 * @since 25.11.2009
	 * @return the current debounce time in milliseconds
	 * @see #setDebounceTimeMillis(long)
	 */
    public long getDebounceTimeMillis() {
        return debounceTime;
    }

    /**
	 * Tries to automatically connect with an available K8055 Board.
	 * 
	 * @return a {@link K8055AddressState} representing the actual state of the Connection
	 */
    public K8055AddressState autoConnect() {
        return ioThread.autoConnect();
    }

    /**
	 * Closes the connection between IOThread and Velleman K8055 Board
	 * 
	 * @return true if closing succeeded
	 */
    public boolean closeConnection() {
        return ioThread.closeConnection();
    }

    /**
	 * This method builds up a connection with Velleman K8055 Board
	 * 
	 * @param connectWith the address to connect with
	 * @return the new K8055AddressState
	 */
    public K8055AddressState connectWithBoard(K8055AddressState connectWith) {
        return ioThread.connectWithBoard(connectWith);
    }

    /**
	 * @author genodeftest (Christian Stadelmann)
	 * @since 25.11.2009
	 * @return The address of the current connected K8055 Board
	 */
    public K8055AddressState getAdressState() {
        return ioThread.getAdressState();
    }

    /**
	 * This method will list up all available addresses and address states.
	 * 
	 * @author genodeftest (Christian Stadelmann)
	 * @since 25.11.2009
	 * @return all available This method will list up all available
	 */
    public AddressState[] getAdressStates() {
        return new K8055AddressState[] { K8055AddressState.NOT_CONNECTED, K8055AddressState.CONNECT_WITH_SK0, K8055AddressState.CONNECT_WITH_SK1, K8055AddressState.CONNECT_WITH_SK2, K8055AddressState.CONNECT_WITH_SK3 };
    }

    public boolean isDeviceConnected() {
        return ioThread.isDeviceConnected();
    }

    private class IOThread extends Thread {

        private volatile EventQueue systemEventQueue = Toolkit.getDefaultToolkit().getSystemEventQueue();

        private volatile JWrapperK8055 jWrapper;

        private short[] counterDebounce_OLD = new short[] { -1, -1 };

        private short[] analogOut_OLD = new short[] { -1, -1 };

        private boolean[] digitalOut_OLD = new boolean[] { true, true, true, true, true, true, true, true };

        private long[] counterIn = new long[] { 0l, 0l };

        private boolean[] digitalIn = new boolean[5];

        IOThread(JWrapperK8055 jwrapperInstance) {
            super("I/O-Thread");
            if (debounceTime == 0) this.setPriority(Thread.MIN_PRIORITY); else this.setPriority(Thread.MAX_PRIORITY);
            setDaemon(true);
            jWrapper = jwrapperInstance;
        }

        boolean isDeviceConnected() {
            synchronized (jWrapper) {
                return jWrapper.isConnected();
            }
        }

        /**
		 * This method builds up a connection with Velleman K8055 Board
		 * 
		 * @param connectWith the address to connect with
		 * @return the new K8055AddressState
		 */
        K8055AddressState connectWithBoard(K8055AddressState connectWith) {
            try {
                switch(connectWith) {
                    case NOT_CONNECTED:
                        synchronized (jWrapper) {
                            jWrapper.closeDevice();
                        }
                        pauseIOThread(true);
                        break;
                    case CONNECT_WITH_SK0:
                    case CONNECT_WITH_SK1:
                    case CONNECT_WITH_SK2:
                    case CONNECT_WITH_SK3:
                        synchronized (jWrapper) {
                            jWrapper.openDevice(connectWith);
                        }
                        break;
                    default:
                        throw new IllegalArgumentException();
                }
            } catch (Exception e) {
            }
            K8055AddressState k = getAdressState();
            if (k == K8055AddressState.NOT_CONNECTED) pauseIOThread(true); else pauseIOThread(false);
            return k;
        }

        K8055AddressState getAdressState() {
            synchronized (jWrapper) {
                if (jWrapper.isConnected()) {
                    return jWrapper.getCurrentDevice();
                }
            }
            return K8055AddressState.NOT_CONNECTED;
        }

        /**
		 * closes the connection between IOThread and Velleman K8055 Board
		 * 
		 * @return true if closing succeeded
		 */
        boolean closeConnection() {
            pauseIOThread(true);
            synchronized (jWrapper) {
                return jWrapper.closeDevice();
            }
        }

        /**
		 * <b>!Invoke only if IOThread was paused at least 100ms before!</b>
		 * 
		 * @return a {@link K8055AddressState} representing the actual state of the IOThread
		 */
        K8055AddressState autoConnect() {
            synchronized (jWrapper) {
                if (!jWrapper.autoConnect()) {
                    return K8055AddressState.NOT_CONNECTED;
                }
                pauseIOThread(false);
                return jWrapper.getCurrentDevice();
            }
        }

        @Override
        public void run() {
            while (true) {
                try {
                    boolean pause;
                    synchronized (pauseIOThread) {
                        pause = pauseIOThread;
                    }
                    if (pause) {
                        try {
                            synchronized (this) {
                                this.wait();
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else toBeWorkedBy_I_O_Thread();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        private void toBeWorkedBy_I_O_Thread() {
            long startTime = System.currentTimeMillis();
            try {
                handle();
            } catch (Exception e) {
                e.printStackTrace();
            }
            long timeToWait = debounceTime - (System.currentTimeMillis() - startTime);
            if (timeToWait > 1) {
                try {
                    synchronized (this) {
                        this.wait(timeToWait);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                Thread.yield();
            }
        }

        /**
		 * this method has to be run by the I/O-Thread
		 * 
		 * @see IOThreadControl
		 * @param wrapper
		 * @throws ConnectionException
		 * @throws NullPointerException if listener changes its state while this method is run
		 *         by ioThread
		 */
        private void handle() throws IOException, NullPointerException {
            Vector<IOListener> analog = new Vector<IOListener>(), digital = new Vector<IOListener>(), counter = new Vector<IOListener>();
            for (final IOListener l : listenerList) {
                switch(l.getDataType()) {
                    case ANALOG:
                        analog.add(copy(l));
                        break;
                    case COUNTER:
                        counter.add(copy(l));
                        break;
                    case DIGITAL:
                        digital.add(copy(l));
                        break;
                }
            }
            {
                if (analog.size() != 0) {
                    short[] a = new short[2];
                    if (analog.size() == 1 & !analog.get(0).listenToAllChannels()) {
                        short channelIndex = (short) (((AnalogInput) (analog.get(0).getChannel())).channelNo - 1);
                        a[channelIndex] = jWrapper.readAnalogInput(channelIndex);
                        postEvent(new AnalogEvent(analog.get(0).getTargetComponent(), a[channelIndex], System.currentTimeMillis()));
                    } else {
                        a = jWrapper.readAllAnalogInput();
                        {
                            for (IOListener listener : analog) {
                                if (listener.listenToAllChannels()) {
                                    postEvent(new AnalogAllEvent(listener.getTargetComponent(), a, System.currentTimeMillis()));
                                } else {
                                    postEvent(new AnalogEvent(listener.getTargetComponent(), a[1], System.currentTimeMillis()));
                                }
                            }
                        }
                    }
                }
            }
            {
                if (digital.size() != 0) {
                    boolean[] newDigitalIn = new boolean[5];
                    boolean singleListenersOnly = true;
                    for (IOListener listener : digital) {
                        if (listener.listenToAllChannels()) {
                            singleListenersOnly = false;
                            break;
                        }
                    }
                    if (digital.size() < 3 & singleListenersOnly) {
                        for (IOListener listener : digital) {
                            DigitalInput channel = ((DigitalInput) listener.getChannel());
                            newDigitalIn[channel.channelNo - 1] = jWrapper.readDigitalInput(channel);
                        }
                    } else {
                        newDigitalIn = jWrapper.readAllDigitalInput();
                    }
                    boolean oneOrMoreChanged = false;
                    for (int i = 0; i < 5; i++) {
                        if (newDigitalIn[i] == digitalIn[i]) {
                            oneOrMoreChanged = true;
                            break;
                        }
                    }
                    if (oneOrMoreChanged) {
                        for (IOListener listener : digital) {
                            if (listener.listenToAllChannels()) {
                                postEvent(new DigitalAllEvent(listener.getTargetComponent(), newDigitalIn, System.currentTimeMillis()));
                            } else {
                                int channelNo = ((DigitalInput) listener.getChannel()).channelNo;
                                if (newDigitalIn[channelNo - 1] != digitalIn[channelNo - 1]) {
                                    postEvent(new DigitalEvent(listener.getTargetComponent(), newDigitalIn[channelNo - 1], System.currentTimeMillis()));
                                }
                            }
                        }
                    }
                    digitalIn = newDigitalIn;
                }
            }
            {
                if (counter.size() != 0) {
                    long[] c = new long[2];
                    if (counter.size() == 1 & !counter.get(0).listenToAllChannels()) {
                        int channelIndex = ((Counter) counter.get(0).getChannel()).channelNo;
                        c[channelIndex - 1] = jWrapper.readCounter(Counter.getChannelForIndex(channelIndex));
                        if (c[channelIndex - 1] != counterIn[channelIndex - 1]) {
                            counterIn[channelIndex - 1] = c[channelIndex - 1];
                            postEvent(new CounterEvent(counter.get(0).getTargetComponent(), c[channelIndex - 1], System.currentTimeMillis()));
                        }
                    } else {
                        c[0] = jWrapper.readCounter(Counter.CHANNEL1);
                        c[1] = jWrapper.readCounter(Counter.CHANNEL2);
                        if (c[0] == counterIn[0]) {
                            if (c[1] == counterIn[1]) {
                            } else {
                                for (IOListener listener : counter) {
                                    if (listener.listenToAllChannels()) {
                                        postEvent(new CounterAllEvent(listener.getTargetComponent(), c, System.currentTimeMillis()));
                                    } else {
                                        if (((Counter) listener.getChannel()).channelNo == 2) {
                                            postEvent(new CounterEvent(listener.getTargetComponent(), c[1], System.currentTimeMillis()));
                                        }
                                    }
                                }
                            }
                        } else {
                            if (c[1] == counterIn[1]) {
                                for (IOListener listener : counter) {
                                    if (listener.listenToAllChannels()) {
                                        postEvent(new CounterAllEvent(listener.getTargetComponent(), c, System.currentTimeMillis()));
                                    } else {
                                        if (((Counter) listener.getChannel()).channelNo == 1) {
                                            postEvent(new CounterEvent(listener.getTargetComponent(), c[0], System.currentTimeMillis()));
                                        }
                                    }
                                }
                            } else {
                                for (IOListener listener : counter) {
                                    if (listener.listenToAllChannels()) {
                                        postEvent(new CounterAllEvent(listener.getTargetComponent(), c, System.currentTimeMillis()));
                                    } else {
                                        postEvent(new CounterEvent(listener.getTargetComponent(), c[((Counter) listener.getChannel()).channelNo - 1], System.currentTimeMillis()));
                                    }
                                }
                            }
                        }
                        counterIn = c;
                    }
                }
            }
            synchronized (outputChanged) {
                if (!outputChanged) return;
                outputChanged = false;
            }
            {
                boolean value1Changed = (analogOut[0] != analogOut_OLD[0]);
                boolean value2Changed = (analogOut[1] != analogOut_OLD[1]);
                if (value1Changed) {
                    if (value2Changed) {
                        analogOut_OLD[0] = analogOut[0];
                        analogOut_OLD[1] = analogOut[1];
                        jWrapper.setAllAnalogOutput(analogOut[0], analogOut[1]);
                    } else {
                        analogOut_OLD[0] = analogOut[0];
                        jWrapper.setAnalogOutput((short) 1, analogOut[0]);
                    }
                } else if (value2Changed) {
                    analogOut_OLD[1] = analogOut[1];
                    jWrapper.setAnalogOutput((short) 2, analogOut[1]);
                }
            }
            {
                int channelsChangedCount = 0;
                for (int i = 0; i < 8; i++) {
                    if (digitalOut[i] != digitalOut_OLD[i]) channelsChangedCount++;
                }
                if (channelsChangedCount == 1) {
                    for (short i = 0; i < 8; i++) {
                        if (digitalOut[i] != digitalOut_OLD[i]) {
                            jWrapper.setDigitalOutput(i + 1, digitalOut[i]);
                            digitalOut_OLD[i] = digitalOut[i];
                        }
                    }
                } else if (channelsChangedCount > 1) {
                    jWrapper.setAllDigitalOutput(digitalOut);
                    for (int i = 0; i < 8; i++) {
                        digitalOut_OLD[i] = digitalOut[i];
                    }
                }
            }
            {
                if (counterDebounce[0] != counterDebounce_OLD[0]) {
                    counterDebounce_OLD[0] = counterDebounce[0];
                    jWrapper.setCounterDebounceTime(Counter.CHANNEL1, counterDebounce[0]);
                }
                if (counterDebounce[1] != counterDebounce_OLD[1]) {
                    counterDebounce_OLD[1] = counterDebounce[1];
                    jWrapper.setCounterDebounceTime(Counter.CHANNEL2, counterDebounce[1]);
                }
                boolean reset;
                reset = resetCounter1;
                synchronized (resetCounter1) {
                    resetCounter1 = false;
                }
                if (reset) {
                    jWrapper.resetCounter(Counter.CHANNEL1);
                }
                reset = resetCounter2;
                synchronized (resetCounter2) {
                    resetCounter2 = false;
                }
                if (reset) {
                    jWrapper.resetCounter(Counter.CHANNEL2);
                }
            }
        }

        private void postEvent(AWTEvent e) {
            synchronized (systemEventQueue) {
                systemEventQueue.postEvent(e);
            }
            Thread.yield();
        }
    }

    /**
	 * This method copies the IOListener l to a new IOListener. This method may cause errors
	 * because {@link IOListener#getTargetComponent()} may return a non-existing Component.
	 * 
	 * @param l the IOListener to copy
	 * @return copy of l
	 */
    private static IOListener copy(final IOListener l) {
        final boolean listenToAllChannels = l.listenToAllChannels();
        final Component targetComponent = l.getTargetComponent();
        final IOChannels iOChannels = l.getDataType();
        final K8055Channel k8055Channel = l.getChannel();
        IOListener ret = new IOListener() {

            @Override
            public boolean listenToAllChannels() {
                return listenToAllChannels;
            }

            @Override
            public Component getTargetComponent() {
                return targetComponent;
            }

            @Override
            public IOChannels getDataType() {
                return iOChannels;
            }

            @Override
            public K8055Channel getChannel() {
                return k8055Channel;
            }
        };
        return ret;
    }

    private JWrapperK8055 prepareDefaultJNADriver() {
        File lib_K8055D = copyRequiredLibraryIfNeeded("/K8055D.dll", "K8055D.dll");
        if (lib_K8055D == null) {
            return null;
        }
        JWrapperK8055 newDriver;
        try {
            newDriver = new JNA_DirectMapped(lib_K8055D);
        } catch (Exception e) {
            return null;
        }
        return newDriver;
    }

    private JWrapperK8055 prepareJWrapperByMikePeter() {
        copyRequiredLibraryIfNeeded("/K8055D_C.DLL", "K8055D_C.dll");
        copyRequiredLibraryIfNeeded("/K8055-JWrapper.dll", "K8055-JWrapper.dll");
        JWrapperK8055 newDriver;
        try {
            newDriver = new ByMikePeter();
        } catch (Exception e) {
            return null;
        }
        return newDriver;
    }

    /**
	 * @author genodeftest (Christian Stadelmann)
	 * @since 05.01.2010
	 * @param absoluteInternalPath
	 * @param libnameWithSuffix the new name the File will get outside
	 * @param classPath
	 * @return null if any error occurred <br>
	 *         the File which is the copy of the original, internal File(from absolutePath)
	 */
    private static File copyRequiredLibrary0(String absoluteInternalPath, String libnameWithSuffix, String classPath) {
        try {
            File dest = new File(classPath + File.separator + libnameWithSuffix);
            if (!dest.createNewFile()) return null;
            dest.deleteOnExit();
            if (!dest.canWrite()) return null;
            InputStream is = InputOutputAdapter.class.getResourceAsStream(absoluteInternalPath);
            if (is == null) return null;
            System.out.println("Copy library " + libnameWithSuffix + " to " + dest.getCanonicalPath());
            FileOutputStream fos = new FileOutputStream(dest);
            int count;
            while (true) {
                count = is.read();
                if (count == -1) break;
                fos.write(count);
            }
            fos.flush();
            fos.close();
            is.close();
            return dest;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
	 * Searches the requested library in all directories provided by {@code
	 * System.getProperty("java.library.path")}. If it is found, it will be returned. Otherwise
	 * the library will be copied from the given internal path
	 * 
	 * @author genodeftest (Christian Stadelmann)
	 * @since 05.01.2010
	 * @param absoluteInternalPath representing the File, internal in the current ClassLoader
	 *        directory, e.g. {@code /de/genodeftest/k8055/driver/k8055.dll}
	 * @param libnameWithSuffix
	 * @return the File object representing the requested library
	 */
    private static File copyRequiredLibraryIfNeeded(String absoluteInternalPath, String libnameWithSuffix) {
        for (String classpath : System.getProperty("java.library.path").split(File.pathSeparator)) {
            File possiblyLib = new File(classpath + File.separator + libnameWithSuffix);
            if (possiblyLib.exists() && possiblyLib.canExecute() && possiblyLib.isFile()) {
                System.out.println(possiblyLib.length());
                System.out.println("Required library " + libnameWithSuffix + " exists and can be accessed:\n" + possiblyLib.getAbsolutePath());
                return possiblyLib;
            }
        }
        System.out.println("Required library " + libnameWithSuffix + " not found, trying to create it");
        for (String classpath : System.getProperty("java.library.path").split(File.pathSeparator)) {
            File f = copyRequiredLibrary0(absoluteInternalPath, libnameWithSuffix, classpath);
            if (f == null) return null;
            return f;
        }
        return null;
    }
}
