package rvision;

import gnu.io.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.*;

/**
 *
 * @author robert.harder
 */
public class SerialStream implements SerialPortEventListener {

    private static final Logger LOGGER = Logger.getLogger(SerialStream.class.getName());

    static {
        try {
            loadLibrary();
        } catch (Throwable t) {
            LOGGER.warning("Error while copying native code to working directory (not necessarily bad): " + t.getMessage());
        }
    }

    private static boolean loadLibrary() throws Throwable {
        String[] libs = new String[] { "rxtxSerial.dll", "librxtxSerial.so", "librxtxSerial.jnilib" };
        for (String lib : libs) {
            try {
                String[] ll = lib.split("\\.");
                String prefix = ll[0];
                String suffix = ll[1];
                File outFile = new File(lib);
                if (!outFile.exists()) {
                    System.out.println("Copying " + lib + " to the working directory...");
                    InputStream inputStream = SerialStream.class.getResource("/" + lib).openStream();
                    File rxtxDll = new File(lib);
                    FileOutputStream outputStream = new FileOutputStream(outFile);
                    byte[] array = new byte[8192];
                    int read = 0;
                    while ((read = inputStream.read(array)) >= 0) outputStream.write(array, 0, read);
                    outputStream.close();
                }
                System.load(outFile.getAbsolutePath());
            } catch (Throwable t) {
                System.err.println("SerialStream.loadLibrary: " + t.getMessage());
            }
        }
        return true;
    }

    private static final String DEFAULT_PORT = "COM1";

    private static final int DEFAULT_BAUD = 9600;

    private static final int DEFAULT_DATA_BITS = SerialPort.DATABITS_8;

    private static final int DEFAULT_PARITY = SerialPort.PARITY_NONE;

    private static final int DEFAULT_STOP_BITS = SerialPort.STOPBITS_1;

    private static final int DEFAULT_READ_TIMEOUT = 3000;

    private CommPortIdentifier portId;

    private SerialPort serialPort;

    private String port;

    private int baud;

    private int dataBits;

    private int parity;

    private int stopBits;

    private int readTimeout;

    private InputStream in;

    private OutputStream out;

    private BlockingQueue<Integer> inputQueue;

    private static Integer[] BYTES;

    static {
        BYTES = new Integer[256];
        for (int i = 0; i < 255; i++) {
            BYTES[i] = new Integer(i);
        }
    }

    public SerialStream() throws java.io.IOException {
        this(DEFAULT_PORT, DEFAULT_BAUD, DEFAULT_DATA_BITS, DEFAULT_PARITY, DEFAULT_STOP_BITS, DEFAULT_READ_TIMEOUT);
    }

    public SerialStream(String port, int baud) throws java.io.IOException {
        this(port, baud, DEFAULT_DATA_BITS, DEFAULT_PARITY, DEFAULT_STOP_BITS, DEFAULT_READ_TIMEOUT);
    }

    public SerialStream(String port, int baud, int dataBits, int parity, int stopBits, int readTimeout) throws java.io.IOException {
        this.port = port;
        this.baud = baud;
        this.dataBits = dataBits;
        this.parity = parity;
        this.stopBits = stopBits;
        this.readTimeout = readTimeout;
        initComponents();
    }

    private void initComponents() throws IOException {
        this.portId = getPortId(this.port);
        this.inputQueue = new LinkedBlockingQueue<Integer>();
        try {
            this.serialPort = (SerialPort) this.portId.open("Java Serial Stream", 2000);
        } catch (PortInUseException e) {
            close();
            throw new java.io.IOException("The port " + portId.getName() + " was in use.", e);
        }
        final InputStream serialIn = this.serialPort.getInputStream();
        this.in = new InputStream() {

            @Override
            public int read() throws IOException {
                int b = -1;
                while ((b = serialIn.read()) < 0) {
                    Thread.yield();
                    synchronized (serialIn) {
                        Thread.yield();
                        try {
                            Thread.sleep(100);
                            serialIn.wait();
                        } catch (InterruptedException exc) {
                            throw new IOException("Interrupted while waiting on serial input.", exc);
                        }
                    }
                }
                return b;
            }
        };
        try {
            this.out = serialPort.getOutputStream();
        } catch (java.io.IOException e) {
            close();
            throw new java.io.IOException("Couldn't connect to the input stream on " + portId.getName() + ".", e);
        }
        try {
            serialPort.addEventListener(this);
        } catch (java.util.TooManyListenersException e) {
            close();
            throw new java.io.IOException("Too many listeners on " + portId.getName() + ".", e);
        }
        serialPort.notifyOnDataAvailable(true);
        try {
            this.serialPort.setSerialPortParams(this.baud, this.dataBits, this.stopBits, this.parity);
        } catch (UnsupportedCommOperationException e) {
            close();
            throw new java.io.IOException("Could not set parameters on serial port " + portId.getName() + ".", e);
        }
    }

    private CommPortIdentifier getPortId(String commPort) {
        CommPortIdentifier portId = null;
        java.util.Enumeration portList = CommPortIdentifier.getPortIdentifiers();
        while (portList.hasMoreElements()) {
            CommPortIdentifier someId = (CommPortIdentifier) portList.nextElement();
            if (someId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                if (commPort.equals(someId.getName())) {
                    portId = someId;
                    break;
                }
            }
        }
        return portId;
    }

    public static String[] getPortNames() {
        LinkedList<String> names = new LinkedList<String>();
        java.util.Enumeration portList = CommPortIdentifier.getPortIdentifiers();
        while (portList.hasMoreElements()) {
            CommPortIdentifier someId = (CommPortIdentifier) portList.nextElement();
            if (someId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                names.add(someId.getName());
            }
        }
        return names.toArray(new String[names.size()]);
    }

    public String getPortName() {
        return this.portId == null ? null : this.portId.getName();
    }

    public InputStream getInputStream() {
        return this.in;
    }

    public OutputStream getOutputStream() {
        return this.out;
    }

    public void close() {
        try {
            this.serialPort.close();
        } catch (Exception exc) {
            LOGGER.warning("Error while closing serial port: " + exc.getMessage());
        }
    }

    public void serialEvent(SerialPortEvent event) {
        switch(event.getEventType()) {
            case SerialPortEvent.BI:
                System.out.println("Break interrupt");
                break;
            case SerialPortEvent.OE:
                System.out.println("Overrun error");
                break;
            case SerialPortEvent.FE:
                System.out.println("Framing error");
                break;
            case SerialPortEvent.PE:
                System.out.println("Parity error");
                break;
            case SerialPortEvent.CD:
                System.out.println("Carrier detect");
                break;
            case SerialPortEvent.CTS:
                System.out.println("Clear to send");
                break;
            case SerialPortEvent.DSR:
                System.out.println("Data set ready");
                break;
            case SerialPortEvent.RI:
                System.out.println("Ring indicator");
                break;
            case SerialPortEvent.OUTPUT_BUFFER_EMPTY:
                System.out.println("Output buffer is empty");
                break;
            case SerialPortEvent.DATA_AVAILABLE:
                Thread.yield();
                InputStream serialIn = ((RXTXPort) event.getSource()).getInputStream();
                synchronized (serialIn) {
                    serialIn.notify();
                }
                break;
        }
    }
}
