package org.myrobotlab.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.Vector;
import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ToolTip;
import org.myrobotlab.service.data.IOData;
import org.myrobotlab.service.data.PinData;
import org.myrobotlab.service.data.PinState;
import org.myrobotlab.service.interfaces.AnalogIO;
import org.myrobotlab.service.interfaces.DigitalIO;
import org.myrobotlab.service.interfaces.MotorController;
import org.myrobotlab.service.interfaces.SensorData;
import org.myrobotlab.service.interfaces.ServoController;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 *  Implementation of a Arduino Service connected to MRL through a serial port.  
 *  The protocol is basically a pass through of system calls to the Arduino board.  Data 
 *  can be passed back from the digital or analog ports by request to start polling. The
 *  serial port can be wireless (bluetooth), rf, or wired. The communication protocol
 *  supported is in arduinoSerial.pde - located here :
 *  
 *	Should support nearly all Arduino board types  
 *   
 *   References:
 *    <a href="http://www.arduino.cc/playground/Main/RotaryEncoders">Rotary Encoders</a> 
 *   @author GroG
 */
@Root
public class ArduinoBT extends Service implements SensorData, DigitalIO, AnalogIO, ServoController, MotorController {

    public static final Logger LOG = Logger.getLogger(ArduinoBT.class.getCanonicalName());

    private static final long serialVersionUID = 1L;

    private static final String TAG = "ArduinoBT";

    private static final boolean D = true;

    private final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public static final int MESSAGE_STATE_CHANGE = 1;

    public static final int MESSAGE_READ = 2;

    public static final int MESSAGE_WRITE = 3;

    public static final int MESSAGE_DEVICE_NAME = 4;

    public static final int MESSAGE_TOAST = 5;

    public static final String DEVICE_NAME = "device_name";

    public static final String TOAST = "toast";

    public BluetoothAdapter btAdapter;

    private volatile Handler mHandler;

    private int state;

    public static final int STATE_NONE = 0;

    public static final int STATE_LISTEN = 1;

    public static final int STATE_CONNECTING = 2;

    public static final int STATE_CONNECTED = 3;

    boolean rawReadMsg = false;

    int rawReadMsgLength = 4;

    public static final String digitalWrite = "digitalWrite";

    public static final String pinMode = "pinMode";

    public static final String analogWrite = "analogWrite";

    public static final String analogReadPollingStart = "analogReadPollingStart";

    public static final String analogReadPollingStop = "analogReadPollingStop";

    BluetoothAdapter adapter = null;

    @Element
    String deviceName = null;

    private ConnectThread connectThread = null;

    private ConnectedThread connectedThread = null;

    @Element
    int baudRate = 115200;

    @Element
    int dataBits = 8;

    @Element
    int parity = 0;

    @Element
    int stopBits = 1;

    public static final int HIGH = 0x1;

    public static final int LOW = 0x0;

    public static final int TCCR0B = 0x25;

    public static final int TCCR1B = 0x2E;

    public static final int TCCR2B = 0xA1;

    public static final int DIGITAL_WRITE = 0;

    public static final int ANALOG_WRITE = 2;

    public static final int ANALOG_VALUE = 3;

    public static final int PINMODE = 4;

    public static final int PULSE_IN = 5;

    public static final int SERVO_ATTACH = 6;

    public static final int SERVO_WRITE = 7;

    public static final int SERVO_SET_MAX_PULSE = 8;

    public static final int SERVO_DETACH = 9;

    public static final int SET_PWM_FREQUENCY = 11;

    public static final int SERVO_READ = 12;

    public static final int ANALOG_READ_POLLING_START = 13;

    public static final int ANALOG_READ_POLLING_STOP = 14;

    public static final int DIGITAL_READ_POLLING_START = 15;

    public static final int DIGITAL_READ_POLLING_STOP = 16;

    public static final int SET_ANALOG_PIN_SENSITIVITY = 17;

    public static final int SET_ANALOG_PIN_GAIN = 18;

    public static final int SERVO_ANGLE_MIN = 0;

    public static final int SERVO_ANGLE_MAX = 180;

    public static final int SERVO_SWEEP = 10;

    public static final int MAX_SERVOS = 8;

    boolean[] servosInUse = new boolean[MAX_SERVOS - 1];

    HashMap<Integer, Integer> pinToServo = new HashMap<Integer, Integer>();

    HashMap<Integer, Integer> servoToPin = new HashMap<Integer, Integer>();

    public HashMap<Integer, PinState> pins = new HashMap<Integer, PinState>();

    /**
	 *  list of serial port names from the system which the Arduino service is 
	 *  running
	 */
    public ArrayList<String> portNames = new ArrayList<String>();

    public ArduinoBT(String n) {
        super(n, ArduinoBT.class.getCanonicalName());
        load();
        for (int i = 0; i < 20; ++i) {
            PinState p = new PinState();
            p.value = 0;
            p.address = i;
            if (i == 3 || i == 5 || i == 6 || i == 9 || i == 10 || i == 11) {
                p.type = PinState.ANALOGDIGITAL;
                pins.put(i, p);
            } else if (i > 13) {
                p.type = PinState.ANALOG;
                pins.put(i, p);
            } else {
                p.type = PinState.DIGITAL;
                pins.put(i, p);
            }
        }
        adapter = BluetoothAdapter.getDefaultAdapter();
        state = STATE_NONE;
        for (int i = 0; i < servosInUse.length; ++i) {
            servosInUse[i] = false;
        }
    }

    public PinState getPinState(int pinNum) {
        return pins.get(pinNum);
    }

    /**
	 * @return the current serials port name or null if not opened
	 */
    public String getDeviceName() {
        return deviceName;
    }

    @Override
    public void loadDefaultConfiguration() {
    }

    /**
	 * serialSend communicate to the arduino using our simple language 3 bytes 3
	 * byte functions - |function name| d0 | d1
	 * 
	 * if outputStream is null: Important note to Fedora 13 make sure
	 * /var/lock/uucp /var/spool/uucp /var/spool/uucppublic and all are chown'd
	 * by uucp:uucp
	 */
    public synchronized void serialSend(int function, int param1, int param2) {
        LOG.info("serialSend fn " + function + " p1 " + param1 + " p2 " + param2);
        byte data[] = new byte[3];
        data[0] = (byte) function;
        data[1] = (byte) param1;
        data[2] = (byte) param2;
        if (connectedThread != null) {
            connectedThread.write(data);
        } else {
            LOG.error("currently not connected");
        }
    }

    @ToolTip("sends an array of data to the serial port which an Arduino is attached to")
    public void serialSend(String data) {
        LOG.error("serialSend [" + data + "]");
        serialSend(data.getBytes());
    }

    public synchronized void serialSend(byte[] data) {
        connectedThread.write(data);
    }

    public void setPWMFrequency(IOData io) {
        int freq = io.value;
        int prescalarValue = 0;
        switch(freq) {
            case 31:
            case 62:
                prescalarValue = 0x05;
                break;
            case 125:
            case 250:
                prescalarValue = 0x04;
                break;
            case 500:
            case 1000:
                prescalarValue = 0x03;
                break;
            case 4000:
            case 8000:
                prescalarValue = 0x02;
                break;
            case 32000:
            case 64000:
                prescalarValue = 0x01;
                break;
            default:
                prescalarValue = 0x03;
        }
        serialSend(SET_PWM_FREQUENCY, io.address, prescalarValue);
    }

    public boolean servoAttach(Integer pin) {
        if (deviceName == null) {
            LOG.error("could not attach servo to pin " + pin + " serial port in null - not initialized?");
            return false;
        }
        LOG.info("servoAttach (" + pin + ") to " + deviceName + " function number " + SERVO_ATTACH);
        for (int i = 0; i < servosInUse.length; ++i) {
            if (!servosInUse[i]) {
                servosInUse[i] = true;
                pinToServo.put(pin, i);
                servoToPin.put(i, pin);
                serialSend(SERVO_ATTACH, pinToServo.get(pin), pin);
                return true;
            }
        }
        LOG.error("servo " + pin + " attach failed - no idle servos");
        return false;
    }

    public boolean servoDetach(Integer pin) {
        LOG.info("servoDetach (" + pin + ") to " + deviceName + " function number " + SERVO_DETACH);
        if (pinToServo.containsKey(pin)) {
            int removeIdx = pinToServo.get(pin);
            serialSend(SERVO_DETACH, pinToServo.get(pin), 0);
            servosInUse[removeIdx] = false;
            return true;
        }
        LOG.error("servo " + pin + " detach failed - not found");
        return false;
    }

    public void servoWrite(IOData io) {
        servoWrite(io.address, io.value);
    }

    public void servoWrite(Integer pin, Integer angle) {
        if (deviceName == null) {
            return;
        }
        LOG.info("servoWrite (" + pin + "," + angle + ") to " + deviceName + " function number " + SERVO_WRITE);
        if (angle < SERVO_ANGLE_MIN || angle > SERVO_ANGLE_MAX) {
            return;
        }
        serialSend(SERVO_WRITE, pinToServo.get(pin), angle);
    }

    public void releaseSerialPort() {
        LOG.debug("releaseSerialPort");
        stop();
        LOG.info("released port");
    }

    public String getDeviceString() {
        return adapter.getName();
    }

    public boolean setBaud(int baudRate) {
        if (deviceName == null) {
            LOG.error("setBaudBase - deviceName is null");
            return false;
        }
        try {
            boolean ret = false;
            this.baudRate = baudRate;
            save();
            broadcastState();
            return ret;
        } catch (Exception e) {
            Service.logException(e);
        }
        return false;
    }

    public int getBaudRate() {
        return baudRate;
    }

    public boolean setSerialPortParams(int baudRate, int dataBits, int stopBits, int parity) {
        if (deviceName == null) {
            LOG.error("setSerialPortParams - deviceName is null");
            return false;
        }
        return true;
    }

    public void digitalReadPollStart(Integer address) {
        LOG.info("digitalRead (" + address + ") to " + deviceName);
        serialSend(DIGITAL_READ_POLLING_START, address, 0);
    }

    public void digitalReadPollStop(Integer address) {
        LOG.info("digitalRead (" + address + ") to " + deviceName);
        serialSend(DIGITAL_READ_POLLING_STOP, address, 0);
    }

    public IOData digitalWrite(IOData io) {
        serialSend(DIGITAL_WRITE, io.address, io.value);
        return io;
    }

    public void pinMode(Integer address, Integer value) {
        pins.get(address).mode = value;
        serialSend(PINMODE, address, value);
    }

    public IOData analogWrite(IOData io) {
        serialSend(ANALOG_WRITE, io.address, io.value);
        return io;
    }

    public PinData publishPin(PinData p) {
        LOG.info(p);
        return p;
    }

    public String readSerialMessage(String s) {
        return s;
    }

    public void setRawReadMsg(Boolean b) {
        rawReadMsg = b;
    }

    public void setReadMsgLength(Integer length) {
        rawReadMsgLength = length;
    }

    public void digitalReadPollingStart(Integer pin) {
        serialSend(DIGITAL_READ_POLLING_START, pin, 0);
    }

    public void digitalReadPollingStop(Integer pin) {
        serialSend(DIGITAL_READ_POLLING_STOP, pin, 0);
    }

    public void analogReadPollingStart(Integer pin) {
        serialSend(ANALOG_READ_POLLING_START, pin, 0);
    }

    public void analogReadPollingStop(Integer pin) {
        serialSend(ANALOG_READ_POLLING_STOP, pin, 0);
    }

    class MotorData {

        boolean isAttached = false;
    }

    HashMap<String, MotorData> motorMap = new HashMap<String, MotorData>();

    public void motorAttach(String name, Integer PWMPin, Integer DIRPin) {
        if (deviceName != null) {
            pinMode(PWMPin, PinState.OUTPUT);
            pinMode(DIRPin, PinState.OUTPUT);
        } else {
            LOG.error("attempting to attach motor before serial connection to " + name + " Arduino is ready");
        }
    }

    public void motorDetach(String name) {
    }

    public void motorMove(String name, Integer amount) {
    }

    public void motorMoveTo(String name, Integer position) {
    }

    @Override
    public String getToolTip() {
        return "<html>Arduino is a service which interfaces with an Arduino micro-controller.<br>" + "This interface can operate over radio, IR, or other communications,<br>" + "but and appropriate .PDE file must be loaded into the micro-controller.<br>" + "See http://myrobotlab.org/communication for details";
    }

    public void stopService() {
        super.stopService();
        releaseSerialPort();
    }

    public Vector<Integer> getOutputPins() {
        Vector<Integer> ret = new Vector<Integer>();
        for (int i = 2; i < 13; ++i) {
            ret.add(i);
        }
        return ret;
    }

    /**
     * Return the current connection state. */
    public synchronized int getBTState() {
        return state;
    }

    /**
     * Gleaned from Google's API Bluetooth Demo
     * Start the ConnectThread to initiate a connection to a remote device.
     * @param device  The BluetoothDevice to connect
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    public synchronized void connect(BluetoothDevice device) {
        if (D) Log.d(TAG, "connect to: " + device);
        if (state == STATE_CONNECTING) {
            if (connectThread != null) {
                connectThread.cancel();
                connectThread = null;
            }
        }
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }
        connectThread = new ConnectThread(device);
        connectThread.start();
        setState(STATE_CONNECTING);
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * @param socket  The BluetoothSocket on which the connection was made
     * @param device  The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device, final String socketType) {
        if (D) Log.d(TAG, "connected, Socket Type:" + socketType);
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }
        connectedThread = new ConnectedThread(socket, socketType, this);
        connectedThread.start();
        Message msg = mHandler.obtainMessage(MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        deviceName = device.getName();
        setState(STATE_CONNECTED);
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        if (D) Log.d(TAG, "stop");
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }
        setState(STATE_NONE);
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
        ConnectedThread r;
        synchronized (this) {
            if (state != STATE_CONNECTED) return;
            r = connectedThread;
        }
        r.write(out);
    }

    public void write(int function, int param1, int param2) {
        ConnectedThread r;
        synchronized (this) {
            if (state != STATE_CONNECTED) return;
            r = connectedThread;
        }
        r.write(new byte[] { (byte) function, (byte) param1, (byte) param2 });
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        Message msg = mHandler.obtainMessage(MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(TOAST, "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        deviceName = null;
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        Message msg = mHandler.obtainMessage(MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        deviceName = null;
    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {

        private final BluetoothSocket mmSocket;

        private final BluetoothDevice mmDevice;

        private String mSocketType;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            try {
                tmp = device.createInsecureRfcommSocketToServiceRecord(SPP_UUID);
                Log.e(TAG, "tmp = " + tmp);
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e);
                deviceName = null;
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN connectThread SocketType:" + mSocketType);
            setName("ConnectThread" + mSocketType);
            adapter.cancelDiscovery();
            try {
                mmSocket.connect();
            } catch (IOException e) {
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() " + mSocketType + " socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }
            synchronized (ArduinoBT.this) {
                connectThread = null;
            }
            connected(mmSocket, mmDevice, mSocketType);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect " + mSocketType + " socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {

        private final BluetoothSocket mmSocket;

        private final InputStream inputStream;

        private final OutputStream mmOutStream;

        private final Service myService;

        public ConnectedThread(BluetoothSocket socket, String socketType, Service myService) {
            Log.d(TAG, "create ConnectedThread: " + socketType);
            this.myService = myService;
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }
            inputStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "BEGIN connectedThread");
            byte[] buffer = new byte[1024];
            while (true) {
                try {
                    byte[] msg = new byte[rawReadMsgLength];
                    int newByte;
                    int numBytes = 0;
                    while ((newByte = inputStream.read()) >= 0) {
                        msg[numBytes] = (byte) newByte;
                        ++numBytes;
                        if (numBytes == rawReadMsgLength) {
                            if (rawReadMsg) {
                                String s = new String(msg);
                                LOG.info(s);
                                invoke("readSerialMessage", s);
                            } else {
                                PinData p = new PinData();
                                p.method = msg[0];
                                p.pin = msg[1];
                                p.value = (msg[2] & 0xFF) << 8;
                                p.value += (msg[3] & 0xFF);
                                p.source = myService.getName();
                                invoke(SensorData.publishPin, p);
                            }
                            numBytes = 0;
                            for (int i = 0; i < rawReadMsgLength; ++i) {
                                msg[i] = -1;
                            }
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         * @param buffer  The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
                mHandler.obtainMessage(MESSAGE_WRITE, -1, -1, buffer).sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    /**
     * Set the current state of the chat connection
     * @param state  An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        if (D) Log.d(TAG, "setState() " + state + " -> " + state);
        this.state = state;
        mHandler.obtainMessage(MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    public Handler getmHandler() {
        return mHandler;
    }

    public void setmHandler(Handler mHandler) {
        this.mHandler = mHandler;
    }
}
