import java.io.*;
import java.util.*;
import javax.comm.*;

interface BlueSentryListener {

    void callback();
}

;

public class BlueSentry implements Runnable, SerialPortEventListener, CommPortOwnershipListener {

    static CommPortIdentifier portId;

    static Enumeration portList;

    boolean DEBUG = false;

    private final int BUF_SIZE = 1024;

    private final int MAX_CHANNELS = 8;

    private final int RECONNECT_TIMEOUT = 5000;

    private final String TOGGLE_AD = "$";

    private final String SPEED_UP = "+";

    private final String SLOW_DOWN = "-";

    private final String DISP_DELAY = "d";

    private final String ASCII_MODE = "a";

    private final String BINARY_MODE = "b";

    private final String ECHO_CHAR = "e";

    private final String NO_PROMPT = "q";

    private final String SLEEP = "s";

    private final String VERSION = "v";

    private final String INIT = TOGGLE_AD + SPEED_UP + SPEED_UP + SPEED_UP;

    InputStream inputStream;

    OutputStream outputStream;

    SerialPort serialPort;

    Thread readThread;

    String portName;

    private Vector clients = new Vector();

    int numChannels;

    int channel[] = new int[MAX_CHANNELS];

    Timer reconnectTimer;

    boolean connected;

    BufferedReader bluesentryIn;

    public static void main(String[] args) {
        BlueSentry bs;
        if (args.length > 0) bs = new BlueSentry(args[0]); else bs = new BlueSentry();
        bs.DEBUG = true;
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            try {
                bs.getOutputStream().write(in.readLine().getBytes());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public BlueSentry() {
        this("/dev/tty.BlueSentry-out");
    }

    public BlueSentry(String portName) {
        this.portName = portName;
        openPort(portName);
        connected = false;
    }

    public void openPort(String portName) {
        portList = CommPortIdentifier.getPortIdentifiers();
        System.out.println("BlueSentry: Establishing Connection to BlueSentry");
        reconnectTimer = new Timer();
        while (portList.hasMoreElements()) {
            portId = (CommPortIdentifier) portList.nextElement();
            if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                System.out.println(portId.getName());
                if (portId.getName().equals(portName)) {
                    System.out.println("BlueSentry: Found serial port");
                    portId.addPortOwnershipListener(this);
                    connect();
                    break;
                }
            }
        }
    }

    public class ReconnectTask extends TimerTask {

        public void run() {
            System.out.println("BlueSentry: Connection failed");
            reconnect();
        }
    }

    public void reconnect() {
        System.out.println("BlueSentry: Attempting to reconnect");
        connected = false;
        if (serialPort != null) serialPort.close();
        try {
            Thread.sleep(25000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        openPort(portName);
    }

    public void connect() {
        try {
            serialPort = (SerialPort) portId.open("BlueSentry", RECONNECT_TIMEOUT);
            try {
                inputStream = serialPort.getInputStream();
                outputStream = serialPort.getOutputStream();
                bluesentryIn = new BufferedReader(new InputStreamReader(inputStream));
                try {
                    serialPort.addEventListener(this);
                } catch (TooManyListenersException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("BlueSentry: Registered for SerialEvents");
            serialPort.notifyOnDataAvailable(true);
            System.out.println("BlueSentry: Waiting on SerialEvent");
            try {
                serialPort.setSerialPortParams(115200, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
            } catch (UnsupportedCommOperationException e) {
                e.printStackTrace();
            }
            readThread = new Thread(this);
            readThread.start();
            reconnectTimer.schedule(new ReconnectTask(), RECONNECT_TIMEOUT);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("BlueSentry: Could not establish connection to BlueSentry");
            serialPort = null;
            reconnect();
        }
    }

    public void ownershipChange(int type) {
        switch(type) {
            case PORT_OWNED:
                System.out.println("BlueSentry: PORT_OWNED");
                break;
            case PORT_OWNERSHIP_REQUESTED:
                System.out.println("BlueSentry: PORT_OWNERSHIP_REQUESTED");
                break;
            case PORT_UNOWNED:
                System.out.println("BlueSentry: PORT_UNOWNED");
                break;
        }
        if (type == PORT_OWNERSHIP_REQUESTED) {
            if (connected == false && serialPort != null) {
                System.out.println("BlueSentry: Received ownership change, closing serial port");
                serialPort.close();
            }
        }
    }

    public void run() {
        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {
        }
    }

    public void serialEvent(SerialPortEvent event) {
        switch(event.getEventType()) {
            case SerialPortEvent.BI:
                System.out.println("BlueSentry: Break interrupt");
                break;
            case SerialPortEvent.OE:
                System.out.println("BlueSentry: Overrun error");
                break;
            case SerialPortEvent.FE:
                System.out.println("BlueSentry: Framing error");
                break;
            case SerialPortEvent.PE:
                System.out.println("BlueSentry: Parity error");
                break;
            case SerialPortEvent.CD:
                System.out.println("BlueSentry: Carrier detect");
                break;
            case SerialPortEvent.CTS:
                System.out.println("BlueSentry: Clear to send");
                break;
            case SerialPortEvent.DSR:
                System.out.println("BlueSentry: Data set ready");
                break;
            case SerialPortEvent.RI:
                System.out.println("BlueSentry: Ring indicator");
                break;
            case SerialPortEvent.OUTPUT_BUFFER_EMPTY:
                System.out.println("BlueSentry: Output Buffer Empty");
                break;
            case SerialPortEvent.DATA_AVAILABLE:
                try {
                    try {
                        reconnectTimer.cancel();
                    } catch (IllegalStateException e) {
                    }
                    connected = true;
                    String line = bluesentryIn.readLine();
                    if (DEBUG) System.out.print(line + ":");
                    StringTokenizer st = new StringTokenizer(line);
                    int numTokens = st.countTokens();
                    numChannels = numTokens - 1;
                    if (st.hasMoreTokens()) {
                        if (st.nextToken().equals("?")) {
                            outputStream.write(INIT.getBytes());
                            return;
                        }
                        try {
                            for (int i = 0; i < numChannels; i++) {
                                channel[i] = Integer.parseInt(st.nextToken(), 16);
                            }
                            notifyClients();
                            if (DEBUG) System.out.println(getChannel(0));
                        } catch (NumberFormatException ex) {
                        }
                    }
                    reconnectTimer = new Timer();
                    reconnectTimer.schedule(new ReconnectTask(), RECONNECT_TIMEOUT);
                } catch (IOException e) {
                }
                break;
        }
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }

    public void slowDown() throws IOException {
        outputStream.write(SLOW_DOWN.getBytes());
    }

    public void speedUp() throws IOException {
        outputStream.write(SPEED_UP.getBytes());
    }

    public int getChannel(int channelNum) throws IOException {
        if (channelNum >= numChannels || channelNum < 0) {
            throw new IOException();
        }
        return channel[channelNum];
    }

    public void register(BlueSentryListener lt) {
        clients.add(lt);
    }

    public void notifyClients() {
        Iterator it = clients.iterator();
        while (it.hasNext()) {
            BlueSentryListener lt = (BlueSentryListener) it.next();
            lt.callback();
        }
    }

    public void sleep() {
        try {
            outputStream.write(SLEEP.getBytes());
        } catch (IOException e) {
        }
    }
}
