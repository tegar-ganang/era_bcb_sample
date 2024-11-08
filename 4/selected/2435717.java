package org.opennms.rxtx.test.internal;

import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.RXTXVersion;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.TooManyListenersException;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.ops4j.io.*;

/**
 * Internal implementation of our example OSGi service
 */
public final class RxtxCommands implements CommandProvider {

    private final class EventLogger implements SerialPortEventListener {

        private final CommandInterpreter intp;

        private EventLogger(CommandInterpreter intp) {
            this.intp = intp;
        }

        public void serialEvent(SerialPortEvent ev) {
            switch(ev.getEventType()) {
                case SerialPortEvent.BI:
                    intp.println(String.format("EVENT: Received BI event: changed %s to %s", ev.getOldValue(), ev.getNewValue()));
                    break;
                case SerialPortEvent.CD:
                    intp.println(String.format("EVENT: Received CD event: changed %s to %s", ev.getOldValue(), ev.getNewValue()));
                    break;
                case SerialPortEvent.CTS:
                    intp.println(String.format("EVENT: Received CTS event: changed %s to %s", ev.getOldValue(), ev.getNewValue()));
                    break;
                case SerialPortEvent.DATA_AVAILABLE:
                    intp.println(String.format("EVENT: Received DATA_AVAIL event: changed %s to %s", ev.getOldValue(), ev.getNewValue()));
                    break;
                case SerialPortEvent.DSR:
                    intp.println(String.format("EVENT: Received DSR event: changed %s to %s", ev.getOldValue(), ev.getNewValue()));
                    break;
                case SerialPortEvent.FE:
                    intp.println(String.format("EVENT: Received FE event: changed %s to %s", ev.getOldValue(), ev.getNewValue()));
                    break;
                case SerialPortEvent.OE:
                    intp.println(String.format("EVENT: Received OE event: changed %s to %s", ev.getOldValue(), ev.getNewValue()));
                    break;
                case SerialPortEvent.OUTPUT_BUFFER_EMPTY:
                    intp.println(String.format("EVENT: Received OUT_BUF_EMPTY event: changed %s to %s", ev.getOldValue(), ev.getNewValue()));
                    break;
                case SerialPortEvent.PE:
                    intp.println(String.format("EVENT: Received PE event: changed %s to %s", ev.getOldValue(), ev.getNewValue()));
                    break;
                case SerialPortEvent.RI:
                    intp.println(String.format("EVENT: Received RI event: changed %s to %s", ev.getOldValue(), ev.getNewValue()));
                    break;
            }
        }
    }

    private Map<String, SerialPort> m_openPorts = new HashMap<String, SerialPort>();

    private Map<String, Pipe> m_loggingPorts = new HashMap<String, Pipe>();

    public Object _rxtxVersion(CommandInterpreter intp) {
        intp.println("RXTX Version: " + RXTXVersion.getVersion());
        intp.println("RXTX NativeVersion: " + RXTXVersion.nativeGetVersion());
        return null;
    }

    public Object _rxtxListPorts(CommandInterpreter intp) {
        Enumeration<?> en = CommPortIdentifier.getPortIdentifiers();
        while (en.hasMoreElements()) {
            CommPortIdentifier commPortId = (CommPortIdentifier) en.nextElement();
            intp.println("Port: " + commPortId.getName() + " Type: " + commPortId.getPortType());
        }
        return null;
    }

    public Object _rxtxOpen(CommandInterpreter intp) {
        String id = intp.nextArgument();
        String port = intp.nextArgument();
        if (id == null || port == null) {
            intp.println("usage: rxtxOpen <id> <port>");
            return null;
        }
        if (m_openPorts.containsKey(id)) {
            intp.println("there is already a port with id " + id);
            return null;
        }
        CommPortIdentifier portId;
        try {
            portId = CommPortIdentifier.getPortIdentifier(port);
            if (portId.getPortType() != CommPortIdentifier.PORT_SERIAL) {
                intp.println("Only Serial Ports are currently supported.");
                return null;
            }
        } catch (NoSuchPortException e) {
            intp.println("port not found: " + e.getMessage());
            return null;
        }
        try {
            SerialPort commPort = (SerialPort) portId.open("rxtx-test", 4000);
            m_openPorts.put(id, commPort);
            intp.println("Port " + commPort.getName() + " assigned to id " + id);
            return null;
        } catch (PortInUseException e) {
            intp.println("exception opening port: " + e.getMessage());
            return null;
        }
    }

    public Object _rxtxClose(CommandInterpreter intp) {
        try {
            String id = intp.nextArgument();
            assertNotNull(id, "usage: rxtxClose <id>");
            assertOpenPort(id);
            Pipe pipe = m_loggingPorts.remove(id);
            if (pipe != null) {
                pipe.stop();
            }
            SerialPort port = m_openPorts.remove(id);
            intp.print("Closing port " + port.getName() + " with id " + id + "...");
            port.close();
            intp.println("done.");
        } catch (IllegalArgumentException e) {
            intp.println(e.getMessage());
        }
        return null;
    }

    public Object _rxtxWrite(CommandInterpreter intp) {
        String id = intp.nextArgument();
        String text = intp.nextArgument();
        try {
            assertNotNull(id, "usage: rxtxWrite <id> <text>");
            assertNotNull(text, "usage: rxtxWrite <id> <text>");
            assertOpenPort(id);
            SerialPort port = m_openPorts.get(id);
            Writer out = new OutputStreamWriter(port.getOutputStream(), "US-ASCII");
            if (text.startsWith("<<")) {
                String eof = text.substring(2);
                BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
                String line = in.readLine();
                while (!eof.equals(line)) {
                    out.write(line + "\r\n");
                    out.flush();
                    line = in.readLine();
                }
            } else {
                text = text.replace("\\r", "\r");
                text = text.replace("\\n", "\n");
                text = text.replace("\\\\", "\\");
                out.write(text + "\r\n");
                out.flush();
            }
        } catch (IllegalArgumentException e) {
            intp.println(e.getMessage());
        } catch (UnsupportedEncodingException e) {
            intp.println("Unsupported Encoded " + e.getMessage());
        } catch (IOException e) {
            intp.println("Exception writing " + text + " to port with id " + id);
            intp.printStackTrace(e);
        }
        return null;
    }

    public Object _rxtxRead(CommandInterpreter intp) {
        try {
            String id = intp.nextArgument();
            assertNotNull(id, "usage: rxtxRead <id> <timeout>");
            assertOpenPort(id);
            SerialPort port = m_openPorts.get(id);
            Reader r = new InputStreamReader(port.getInputStream(), "US-ASCII");
            while (r.ready()) {
                intp.print((char) r.read());
            }
        } catch (IllegalArgumentException e) {
            intp.println(e.getMessage());
        } catch (Exception e) {
            intp.println("Exception will reading.");
            intp.printStackTrace(e);
        }
        return null;
    }

    public Object _rxtxLog(CommandInterpreter intp) {
        try {
            String id = intp.nextArgument();
            assertNotNull(id, "usage: rxtxLog <id>");
            assertOpenPort(id);
            SerialPort port = m_openPorts.get(id);
            Pipe pipe = new Pipe(port.getInputStream(), System.out).start("Modem DataStream");
            m_loggingPorts.put(id, pipe);
        } catch (IllegalArgumentException e) {
            intp.println(e.getMessage());
        } catch (IOException e) {
            intp.println("Error reading from port");
            intp.printStackTrace(e);
        }
        return null;
    }

    public Object _rxtxUnlog(CommandInterpreter intp) {
        try {
            String id = intp.nextArgument();
            assertNotNull(id, "usage: rxtxUnlog <id>");
            assertOpenPort(id);
            assertTrue(m_loggingPorts.containsKey(id), "port with id " + id + " is not currently logging");
            Pipe pipe = m_loggingPorts.remove(id);
            pipe.stop();
        } catch (IllegalArgumentException e) {
            intp.println(e.getMessage());
        }
        return null;
    }

    public Object _rxtxInfo(CommandInterpreter intp) {
        try {
            String id = intp.nextArgument();
            assertNotNull(id, "usage: rxtxInfo <id>");
            assertOpenPort(id);
            SerialPort port = m_openPorts.get(id);
            intp.println("===== Info for port " + port.getName() + " =====");
            intp.println("\tbaudRate: " + port.getBaudRate());
            intp.println("\tisCD: " + port.isCD());
            intp.println("\tisCTS: " + port.isCTS());
            intp.println("\tisDSR: " + port.isDSR());
            intp.println("\tisDTR: " + port.isDTR());
            intp.println("\tisRI: " + port.isRI());
            intp.println("\tisRTS: " + port.isRTS());
            intp.println("\tdataBits: " + port.getDataBits());
            intp.println("\tendOfInputChar: " + port.getEndOfInputChar());
            intp.println("\tflowControlMode: " + port.getFlowControlMode());
            intp.println("\tinputBufferSize: " + port.getInputBufferSize());
            intp.println("\toutputBufferSize: " + port.getOutputBufferSize());
            intp.println("\tparity: " + port.getParity());
            intp.println("\tparityErrorChar: " + port.getParityErrorChar());
            intp.println("\treceiveFramingEnabled: " + port.isReceiveFramingEnabled());
            intp.println("\treceiveFramingByte: " + port.getReceiveFramingByte());
            intp.println("\treceiveThresholdEnabled: " + port.isReceiveThresholdEnabled());
            intp.println("\treceiveThreshold: " + port.getReceiveThreshold());
            intp.println("\treceiveTimeoutEnabled: " + port.isReceiveTimeoutEnabled());
            intp.println("\treceiveTimeout: " + port.getReceiveTimeout());
            intp.println("\tstopBits: " + port.getStopBits());
            intp.println("===================================================");
        } catch (IllegalArgumentException e) {
            intp.println(e.getMessage());
        } catch (Exception e) {
            intp.printStackTrace(e);
        }
        return null;
    }

    public void _rxtxEnableEvents(final CommandInterpreter intp) {
        try {
            String id = intp.nextArgument();
            assertNotNull(id, "usage: rxtxEnableEvents <id>");
            assertOpenPort(id);
            SerialPort port = m_openPorts.get(id);
            SerialPortEventListener listener = new EventLogger(intp);
            port.addEventListener(listener);
            port.notifyOnBreakInterrupt(true);
            port.notifyOnCarrierDetect(true);
            port.notifyOnCTS(true);
            port.notifyOnDataAvailable(true);
            port.notifyOnDSR(true);
            port.notifyOnFramingError(true);
            port.notifyOnOutputEmpty(true);
            port.notifyOnOverrunError(true);
            port.notifyOnParityError(true);
            port.notifyOnRingIndicator(true);
        } catch (TooManyListenersException e) {
            intp.printStackTrace(e);
        } catch (IllegalArgumentException e) {
            intp.println(e.getMessage());
        }
    }

    public void _rxtxDisableEvents(CommandInterpreter intp) {
        try {
            String id = intp.nextArgument();
            assertNotNull(id, "usage: rxtxDisableEvents <id>");
            assertOpenPort(id);
            SerialPort port = m_openPorts.get(id);
            removeListener(port);
        } catch (IllegalArgumentException e) {
            intp.print(e.getMessage());
        }
    }

    private void removeListener(SerialPort port) {
        port.notifyOnRingIndicator(false);
        port.notifyOnParityError(false);
        port.notifyOnOverrunError(false);
        port.notifyOnOutputEmpty(false);
        port.notifyOnFramingError(false);
        port.notifyOnDSR(false);
        port.notifyOnDataAvailable(false);
        port.notifyOnCTS(false);
        port.notifyOnCarrierDetect(false);
        port.notifyOnBreakInterrupt(false);
        port.removeEventListener();
    }

    public void _rxtxEventTest(CommandInterpreter intp) {
        try {
            String port = intp.nextArgument();
            String testString = intp.nextArgument();
            assertNotNull(port, "usage: rxtxEventTest <port>");
            String[] args = testString == null ? new String[] { port } : new String[] { port, testString };
            LoopbackEventTest.SerialEventHandler.main(args);
        } catch (IllegalArgumentException e) {
            intp.println(e.getMessage());
        }
    }

    public String getHelp() {
        StringBuilder buf = new StringBuilder();
        buf.append("--- RXTX Commands ---").append("\n\t").append("rxtxVersion            -- display the rxtx version").append("\n\t").append("rxtxListPorts          -- list the available ports").append("\n\t").append("rxtxOpen <id> <port>   -- open <port> and assign to <id>").append("\n\t").append("rxtxClose <id>         -- close port <id>").append("\n\t").append("rxtxInfo <id>          -- print info about port <id>").append("\n\t").append("rxtxRead <id>          -- read all available bytes from port <id>").append("\n\t").append("rxtxWrite <id> <text>  -- write <text> to port <id> followed by \\r\\n").append("\n\t").append("rxtxLog <id>           -- redirect all bytes from port <id> to stdin").append("\n\t").append("rxtxUnlog <id>         -- stop redirectory of data from port <id>").append("\n\t").append("rxtxEnableEvents <id>  -- enable logging of events for port <id>").append("\n\t").append("rxtxDisableEvents <id> -- disable logging of events for port <id>").append("\n\t").append("rxtxEventTest <port>   -- test serial events on device").append("\n");
        return buf.toString();
    }

    public void stop() {
        for (Pipe pipe : m_loggingPorts.values()) {
            pipe.stop();
        }
        m_loggingPorts.clear();
        System.out.print("Closing " + m_openPorts.size() + " open comm ports... ");
        for (SerialPort port : m_openPorts.values()) {
            removeListener(port);
            port.close();
        }
        m_openPorts.clear();
        System.out.println("done.");
    }

    private void assertNotNull(Object arg, String msg) {
        assertFalse(arg == null, msg);
    }

    private void assertOpenPort(String id) {
        assertTrue(m_openPorts.containsKey(id), "No open port with id " + id + " found.");
    }

    private void assertTrue(boolean test, String msg) {
        if (!test) {
            throw new IllegalArgumentException(msg);
        }
    }

    private void assertFalse(boolean test, String msg) {
        if (test) {
            throw new IllegalArgumentException(msg);
        }
    }
}
