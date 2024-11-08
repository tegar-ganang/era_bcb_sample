package org.openremote.controller.protocol.lutron;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.SocketException;
import java.util.HashMap;
import org.apache.commons.net.telnet.TelnetClient;
import org.apache.log4j.Logger;
import org.openremote.controller.LutronHomeWorksConfig;

/**
 * 
 * @author <a href="mailto:eric@openremote.org">Eric Bariaux</a>
 *
 */
public class LutronHomeWorksGateway {

    /**
   * Lutron HomeWorks logger. Uses a common category for all Lutron related
   * logging.
   */
    private static final Logger log = Logger.getLogger(LutronHomeWorksCommandBuilder.LUTRON_LOG_CATEGORY);

    private static HashMap<LutronHomeWorksAddress, HomeWorksDevice> deviceCache = new HashMap<LutronHomeWorksAddress, HomeWorksDevice>();

    private LutronHomeWorksConfig lutronConfig;

    private MessageQueueWithPriorityAndTTL<LutronCommand> queue = new MessageQueueWithPriorityAndTTL<LutronCommand>();

    private LoginState loginState = new LoginState();

    private LutronHomeWorksConnectionThread connectionThread;

    public synchronized void startGateway() {
        if (lutronConfig == null) {
            lutronConfig = LutronHomeWorksConfig.readXML();
            log.info("Got Lutron config");
            log.info("Address >" + lutronConfig.getAddress() + "<");
            log.info("Port >" + lutronConfig.getPort() + "<");
            log.info("UserName >" + lutronConfig.getUserName() + "<");
            log.info("Password >" + lutronConfig.getPassword() + "<");
        }
        if (connectionThread == null) {
            connectionThread = new LutronHomeWorksConnectionThread();
            connectionThread.start();
        }
    }

    public void sendCommand(String command, LutronHomeWorksAddress address, String parameter) {
        log.info("Asked to send command " + command);
        startGateway();
        queue.add(new LutronCommand(command, address, parameter));
    }

    /**
   * Gets the HomeWorks device from the cache, creating it if not already
   * present.
   * 
   * @param address
   * @return
   * @return
   * @throws LutronHomeWorksDeviceException 
   */
    public HomeWorksDevice getHomeWorksDevice(LutronHomeWorksAddress address, Class<? extends HomeWorksDevice> deviceClass) throws LutronHomeWorksDeviceException {
        HomeWorksDevice device = deviceCache.get(address);
        if (device == null) {
            try {
                Constructor<? extends HomeWorksDevice> constructor = deviceClass.getConstructor(LutronHomeWorksGateway.class, LutronHomeWorksAddress.class);
                device = constructor.newInstance(this, address);
            } catch (SecurityException e) {
                throw new LutronHomeWorksDeviceException("Impossible to create device instance", address, deviceClass, e);
            } catch (NoSuchMethodException e) {
                throw new LutronHomeWorksDeviceException("Impossible to create device instance", address, deviceClass, e);
            } catch (IllegalArgumentException e) {
                throw new LutronHomeWorksDeviceException("Impossible to create device instance", address, deviceClass, e);
            } catch (InstantiationException e) {
                throw new LutronHomeWorksDeviceException("Impossible to create device instance", address, deviceClass, e);
            } catch (IllegalAccessException e) {
                throw new LutronHomeWorksDeviceException("Impossible to create device instance", address, deviceClass, e);
            } catch (InvocationTargetException e) {
                throw new LutronHomeWorksDeviceException("Impossible to create device instance", address, deviceClass, e);
            }
        }
        if (!(deviceClass.isInstance(device))) {
            throw new LutronHomeWorksDeviceException("Invalid device type found at given address", address, deviceClass, null);
        }
        deviceCache.put(address, device);
        return device;
    }

    private class LutronHomeWorksConnectionThread extends Thread {

        TelnetClient tc;

        private LutronHomeWorksReaderThread readerThread;

        private LutronHomeWorksWriterThread writerThread;

        @Override
        public void run() {
            if (tc == null) {
                tc = new TelnetClient();
                tc.setConnectTimeout(10000);
                while (!isInterrupted()) {
                    try {
                        log.info("Trying to connect to " + lutronConfig.getAddress() + " on port " + lutronConfig.getPort());
                        tc.connect(lutronConfig.getAddress(), lutronConfig.getPort());
                        log.info("Telnet client connected");
                        readerThread = new LutronHomeWorksReaderThread(tc.getInputStream());
                        readerThread.start();
                        log.info("Reader thread started");
                        writerThread = new LutronHomeWorksWriterThread(tc.getOutputStream());
                        writerThread.start();
                        log.info("Writer thread started");
                        while (readerThread != null) {
                            readerThread.join(1000);
                            if (!readerThread.isAlive()) {
                                log.info("Reader thread is dead, clean and re-try to connect");
                                tc.disconnect();
                                readerThread = null;
                                writerThread.interrupt();
                                writerThread = null;
                            }
                        }
                    } catch (SocketException e) {
                        e.printStackTrace();
                        try {
                            Thread.sleep(15000);
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        try {
                            Thread.sleep(15000);
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private class LutronHomeWorksWriterThread extends Thread {

        private OutputStream os;

        public LutronHomeWorksWriterThread(OutputStream os) {
            super();
            this.os = os;
        }

        @Override
        public void run() {
            log.info("Writer thread starting");
            PrintWriter pr = new PrintWriter(new OutputStreamWriter(os));
            pr.println("");
            pr.flush();
            while (!isInterrupted()) {
                synchronized (loginState) {
                    while (!loginState.loggedIn) {
                        try {
                            while (!loginState.needsLogin && !loginState.loggedIn) {
                                log.info("Not logged in, waiting to be woken up");
                                loginState.wait();
                                log.info("Woken up on loggedIn, loggedIn: " + loginState.loggedIn + "- needsLogin: " + loginState.needsLogin);
                            }
                            if (!loginState.loggedIn) {
                                pr.println(lutronConfig.getUserName() + "," + lutronConfig.getPassword());
                                pr.flush();
                                loginState.needsLogin = false;
                                log.info("Sent log in info");
                            }
                        } catch (InterruptedException e) {
                        }
                    }
                }
                LutronCommand cmd = queue.blockingPoll();
                if (cmd != null) {
                    log.info("Sending >" + cmd.toString() + "< on print writer " + pr);
                    pr.println(cmd.toString() + "\n");
                    pr.flush();
                }
            }
        }
    }

    private class LutronHomeWorksReaderThread extends Thread {

        private InputStream is;

        public LutronHomeWorksReaderThread(InputStream is) {
            super();
            this.is = is;
        }

        @Override
        public void run() {
            log.info("Reader thread starting");
            log.info("TC input stream " + is);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            log.info("Buffered reader " + br);
            String line = null;
            try {
                line = br.readLine();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            do {
                try {
                    log.info("Reader thread got line >" + line + "<");
                    if (line.startsWith("LOGIN: login successful")) {
                        synchronized (loginState) {
                            loginState.loggedIn = true;
                            loginState.invalidLogin = false;
                            loginState.notify();
                        }
                        queue.priorityAdd(new LutronCommand("PROMPTOFF", null, null));
                        queue.priorityAdd(new LutronCommand("DLMON", null, null));
                        queue.priorityAdd(new LutronCommand("GSMON", null, null));
                        queue.priorityAdd(new LutronCommand("KLMON", null, null));
                    } else if (line.startsWith("LOGIN:")) {
                        log.info("Asked to login, wakening writer thread");
                        synchronized (loginState) {
                            loginState.loggedIn = false;
                            loginState.needsLogin = true;
                            loginState.notify();
                        }
                    } else if (line.startsWith("login incorrect")) {
                        synchronized (loginState) {
                            loginState.loggedIn = false;
                            loginState.invalidLogin = true;
                        }
                    } else if (line.startsWith("closing connection")) {
                        synchronized (loginState) {
                            loginState.loggedIn = false;
                        }
                        break;
                    } else {
                        LutronResponse response = parseResponse(line);
                        if (response != null) {
                            if ("GSS".equals(response.response)) {
                                try {
                                    GrafikEye ge = (GrafikEye) getHomeWorksDevice(response.address, GrafikEye.class);
                                    if (ge != null) {
                                        ge.processUpdate(response.parameter);
                                    }
                                } catch (LutronHomeWorksDeviceException e) {
                                    log.error("Impossible to get device", e);
                                }
                            } else if ("KLS".equals(response.response)) {
                                try {
                                    Keypad keypad = (Keypad) getHomeWorksDevice(response.address, Keypad.class);
                                    if (keypad != null) {
                                        keypad.processUpdate(response.parameter);
                                    }
                                } catch (LutronHomeWorksDeviceException e) {
                                    log.error("Impossible to get device", e);
                                }
                            } else if ("DL".equals(response.response)) {
                                try {
                                    Dimmer dim = (Dimmer) getHomeWorksDevice(response.address, Dimmer.class);
                                    if (dim != null) {
                                        dim.processUpdate(response.parameter);
                                    }
                                } catch (LutronHomeWorksDeviceException e) {
                                    log.error("Impossible to get device", e);
                                }
                            }
                        } else {
                        }
                    }
                    line = br.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } while (line != null && !isInterrupted());
        }
    }

    private LutronResponse parseResponse(String responseText) {
        LutronResponse response = null;
        String[] parts = responseText.split(",");
        if (parts.length == 3) {
            try {
                response = new LutronResponse();
                response.response = parts[0].trim();
                response.address = new LutronHomeWorksAddress(parts[1].trim());
                response.parameter = parts[2].trim();
                log.info("Response is (" + response.response + "," + response.address + "," + response.parameter + ")");
            } catch (InvalidLutronHomeWorksAddressException e) {
                response = null;
            }
        }
        return response;
    }

    public class LutronCommand {

        private String command;

        private LutronHomeWorksAddress address;

        private String parameter;

        public LutronCommand(String command, LutronHomeWorksAddress address, String parameter) {
            this.command = command;
            this.address = address;
            this.parameter = parameter;
        }

        public String toString() {
            StringBuffer buf = new StringBuffer(command);
            if (address != null) {
                buf.append(", ");
                buf.append(address);
            }
            if (parameter != null) {
                buf.append(", ");
                buf.append(parameter);
            }
            return buf.toString();
        }
    }

    private class LutronResponse {

        public String response;

        public LutronHomeWorksAddress address;

        public String parameter;
    }

    private class LoginState {

        /**
     * Indicates that we must send the login information.
     */
        public boolean needsLogin;

        /**
     * Indicates if we're logged into the system, if not commands must be queued.
     */
        public boolean loggedIn;

        /**
     * Indicates if we tried logging in and been refused the login, if so do not try again.
     * TODO: there must be a way to reset this.
     */
        public boolean invalidLogin;
    }
}
