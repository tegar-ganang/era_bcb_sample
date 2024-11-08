package vehikel.protocol.serial;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import vehikel.IConsistencyCheck;
import vehikel.IUserInteraction;
import vehikel.testing.TestsRunner;

public class RxTxLoopConsitencyCheck implements IConsistencyCheck {

    private static final String prefixMessage = "RXTX loopback test for serial communication: ";

    private static String message = prefixMessage;

    private static byte[] pattern = new byte[] { (byte) '#', (byte) 'l', (byte) 'o', (byte) 'o', (byte) 'p', (byte) 't', (byte) 'e', (byte) 's', (byte) 't', (byte) '.' };

    private static int[] receiveBuffer = new int[256];

    private static int receiveCount = 0;

    private static boolean stop = false;

    private static int state = 0;

    private String recognisedDevice = null;

    private Thread reader;

    private Thread writer;

    InputStream in;

    OutputStream out;

    IUserInteraction userInteraction = null;

    public RxTxLoopConsitencyCheck() {
        this(null);
    }

    public RxTxLoopConsitencyCheck(IUserInteraction userInteraction) {
        this.userInteraction = userInteraction;
    }

    public boolean check() {
        if (TestsRunner.skipIoTests) return false;
        state = 0;
        message = prefixMessage;
        stop = false;
        CommPorts seriallCommPorts = new CommPorts();
        if (userInteraction != null) {
            SerialPort serialPort = null;
            int countDevices = seriallCommPorts.count();
            if (countDevices < 1) {
                state = 7;
                message += " No serieal device found: Skiping loop back test.";
            } else {
                for (int dx = 0; dx < countDevices && recognisedDevice == null; ++dx) {
                    receiveCount = 0;
                    userInteraction.informAndWait("reconnect");
                    try {
                        serialPort = connect(seriallCommPorts.device(dx));
                    } catch (Exception e) {
                        state = 2;
                        message += e.toString();
                        e.printStackTrace();
                    }
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        state = 3;
                        message += e.toString();
                        e.printStackTrace();
                    }
                    stop = true;
                    try {
                        if (in != null) {
                            in.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        if (out != null) {
                            out.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (writer != null) {
                        writer.interrupt();
                    }
                    if (reader != null) {
                        reader.interrupt();
                    }
                    if (serialPort != null) {
                        try {
                            userInteraction.informAndWait("disconnect");
                            serialPort.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    if (pattern.length == receiveCount) {
                        boolean diff = false;
                        for (int cx = 0; cx < pattern.length; cx++) {
                            if (pattern[cx] != receiveBuffer[cx]) {
                                diff = true;
                            }
                        }
                        if (diff) {
                            state = 4;
                            message += "unexpected characters received '" + new String(receiveBuffer, 0, receiveCount) + "'. ";
                        } else {
                            state = 1;
                            message += "ok";
                            recognisedDevice = seriallCommPorts.device(dx);
                        }
                    } else {
                        state = 5;
                        message += pattern.length + " bytes expexted, " + receiveCount + " bytes received. Is your serial communication in loop back mode?";
                    }
                }
            }
        } else {
            state = 6;
            message += " Skiping loop back test.";
        }
        System.out.println(message);
        return state == 1;
    }

    public String getMessage() {
        return message;
    }

    public String getRecognisedDevice() {
        return recognisedDevice;
    }

    private SerialPort connect(String portName) throws Exception {
        SerialPort serialPort = null;
        CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(portName);
        if (portIdentifier.isCurrentlyOwned()) {
            state = 9;
            message += "Error: '" + portName + "'Port is currently in use. May be an other application is using the serial communication.";
        } else {
            CommPort commPort = portIdentifier.open(this.getClass().getName(), 2000);
            if (commPort instanceof SerialPort) {
                serialPort = (SerialPort) commPort;
                serialPort.setSerialPortParams(9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
                in = serialPort.getInputStream();
                out = serialPort.getOutputStream();
                reader = new Thread(new SerialReader(in));
                writer = new Thread(new SerialWriter(out));
                reader.start();
                writer.start();
            } else {
                message += "No serial or USB port found.";
            }
        }
        return serialPort;
    }

    private static class SerialReader implements Runnable {

        InputStream in;

        public SerialReader(InputStream in) {
            this.in = in;
        }

        public void run() {
            int len = 0;
            byte buffer[] = new byte[64];
            try {
                while (!stop && (len = in.read(buffer)) > -1) {
                    for (int rx = 0; rx < len; ++rx) {
                        if (receiveCount + rx < receiveBuffer.length) {
                            receiveBuffer[receiveCount + rx] = buffer[rx];
                        }
                    }
                    receiveCount += len;
                }
                displayReceived();
            } catch (IOException e) {
                state = 6;
                message += e.toString();
                e.printStackTrace();
            }
            try {
                in.close();
            } catch (IOException e) {
                state = 7;
                message += e.toString();
                e.printStackTrace();
            }
        }

        /**
		 * 
		 */
        private void displayReceived() {
            System.out.println("Bytes received = " + receiveCount);
            for (int rx = 0; rx < receiveCount && rx < receiveBuffer.length; ++rx) {
                int rc = receiveBuffer[rx];
                if (rc < ' ' || rc == 127) {
                    System.out.print('\\');
                    System.out.print(rc);
                } else {
                    System.out.print((char) rc);
                }
                System.out.print(' ');
            }
            System.out.println();
        }
    }

    private static class SerialWriter implements Runnable {

        OutputStream out;

        public SerialWriter(OutputStream out) {
            this.out = out;
        }

        public void run() {
            try {
                for (int px = 0; !stop && px < pattern.length; ++px) {
                    this.out.write(pattern[px]);
                }
                out.close();
            } catch (IOException e) {
                state = 8;
                message += e.toString();
                e.printStackTrace();
            }
            try {
                out.close();
            } catch (IOException e) {
                state = 9;
                message += e.toString();
                e.printStackTrace();
            }
        }
    }
}
