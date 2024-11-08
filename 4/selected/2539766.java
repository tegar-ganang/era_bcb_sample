package org.openremote.controller.protocol.domintell;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.HashMap;
import org.apache.log4j.Logger;
import org.openremote.controller.DomintellConfig;
import org.openremote.controller.protocol.domintell.model.DimmerModule;
import org.openremote.controller.protocol.domintell.model.DomintellModule;
import org.openremote.controller.protocol.domintell.model.RelayModule;
import org.openremote.controller.protocol.domintell.model.TemperatureModule;
import org.openremote.controller.protocol.lutron.LutronHomeWorksDeviceException;
import org.openremote.controller.protocol.lutron.MessageQueueWithPriorityAndTTL;

public class DomintellGateway {

    /**
    * Domintell logger. Uses a common category for all Domintell related logging.
    */
    private static final Logger log = Logger.getLogger(DomintellCommandBuilder.DOMINTELL_LOG_CATEGORY);

    private static HashMap<String, DomintellModule> moduleCache = new HashMap<String, DomintellModule>();

    private static HashMap<String, Class<? extends DomintellModule>> moduleClasses = new HashMap<String, Class<? extends DomintellModule>>();

    private DomintellConfig domintellConfig;

    private MessageQueueWithPriorityAndTTL<DomintellCommandPacket> queue = new MessageQueueWithPriorityAndTTL<DomintellCommandPacket>();

    private LoginState loginState = new LoginState();

    private DomintellConnectionThread connectionThread;

    static {
        moduleClasses.put("BIR", RelayModule.class);
        moduleClasses.put("DMR", RelayModule.class);
        moduleClasses.put("TRP", RelayModule.class);
        moduleClasses.put("DIM", DimmerModule.class);
        moduleClasses.put("D10", DimmerModule.class);
        moduleClasses.put("TSB", TemperatureModule.class);
        moduleClasses.put("TE1", TemperatureModule.class);
        moduleClasses.put("TE2", TemperatureModule.class);
        moduleClasses.put("LC3", TemperatureModule.class);
        moduleClasses.put("PBL", TemperatureModule.class);
    }

    public synchronized void startGateway() {
        if (domintellConfig == null) {
            domintellConfig = DomintellConfig.readXML();
            log.info("Got Domintell config");
            log.info("Address >" + domintellConfig.getAddress() + "<");
            log.info("Port >" + domintellConfig.getPort() + "<");
        }
        if (connectionThread == null) {
            connectionThread = new DomintellConnectionThread();
            connectionThread.start();
        }
    }

    public void sendCommand(String command) {
        log.info("Asked to send command " + command);
        startGateway();
        queue.add(new DomintellCommandPacket(command));
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
    public DomintellModule getDomintellModule(String moduleType, DomintellAddress address, Class<? extends DomintellModule> moduleClass) throws DomintellModuleException {
        DomintellModule module = moduleCache.get(moduleType + address);
        if (module == null) {
            try {
                Constructor<? extends DomintellModule> constructor = moduleClass.getConstructor(DomintellGateway.class, String.class, DomintellAddress.class);
                module = constructor.newInstance(this, moduleType, address);
            } catch (SecurityException e) {
                throw new DomintellModuleException("Impossible to create device instance", moduleType, address, moduleClass, e);
            } catch (NoSuchMethodException e) {
                throw new DomintellModuleException("Impossible to create device instance", moduleType, address, moduleClass, e);
            } catch (IllegalArgumentException e) {
                throw new DomintellModuleException("Impossible to create device instance", moduleType, address, moduleClass, e);
            } catch (InstantiationException e) {
                throw new DomintellModuleException("Impossible to create device instance", moduleType, address, moduleClass, e);
            } catch (IllegalAccessException e) {
                throw new DomintellModuleException("Impossible to create device instance", moduleType, address, moduleClass, e);
            } catch (InvocationTargetException e) {
                throw new DomintellModuleException("Impossible to create device instance", moduleType, address, moduleClass, e);
            }
        }
        if (!(moduleClass.isInstance(module))) {
            throw new DomintellModuleException("Invalid device type found at given address", moduleType, address, moduleClass, null);
        }
        moduleCache.put(moduleType + address, module);
        return module;
    }

    private class DomintellConnectionThread extends Thread {

        DatagramSocket socket;

        private DomintellReaderThread readerThread;

        private DomintellWriterThread writerThread;

        @Override
        public void run() {
            if (socket == null) {
                while (!isInterrupted()) {
                    try {
                        socket = new DatagramSocket();
                        socket.setSoTimeout(60000);
                        log.info("Trying to connect to " + domintellConfig.getAddress() + " on port " + domintellConfig.getPort());
                        socket.connect(InetAddress.getByName(domintellConfig.getAddress()), domintellConfig.getPort());
                        log.info("Socket connected");
                        readerThread = new DomintellReaderThread(socket);
                        readerThread.start();
                        log.info("Reader thread started");
                        writerThread = new DomintellWriterThread(socket);
                        writerThread.start();
                        log.info("Writer thread started");
                        while (readerThread != null) {
                            readerThread.join(1000);
                            if (!readerThread.isAlive()) {
                                log.info("Reader thread is dead, clean and re-try to connect");
                                socket.disconnect();
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

    private class DomintellWriterThread extends Thread {

        private DatagramSocket socket;

        public DomintellWriterThread(DatagramSocket socket) {
            super();
            this.socket = socket;
        }

        @Override
        public void run() {
            log.info("Writer thread starting");
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
                                byte[] buf = "LOGIN".getBytes();
                                try {
                                    DatagramPacket p = new DatagramPacket(buf, buf.length, InetAddress.getByName(domintellConfig.getAddress()), domintellConfig.getPort());
                                    socket.send(p);
                                } catch (IOException e) {
                                    log.warn("Could not send LOGIN packet");
                                }
                                loginState.needsLogin = false;
                                log.info("Sent log in info");
                            }
                        } catch (InterruptedException e) {
                        }
                    }
                }
                DomintellCommandPacket cmd = queue.blockingPoll();
                if (cmd != null) {
                    log.info("Sending >" + cmd.toString() + "< on socket");
                    byte[] buf = cmd.toString().getBytes();
                    try {
                        DatagramPacket p = new DatagramPacket(buf, buf.length, InetAddress.getByName(domintellConfig.getAddress()), domintellConfig.getPort());
                        socket.send(p);
                    } catch (IOException e) {
                        log.warn("Could not send packet >" + cmd.toString() + "<");
                    }
                }
            }
        }
    }

    private class DomintellReaderThread extends Thread {

        private DatagramSocket socket;

        public DomintellReaderThread(DatagramSocket socket) {
            super();
            this.socket = socket;
        }

        @Override
        public void run() {
            log.info("Reader thread starting");
            synchronized (loginState) {
                loginState.loggedIn = false;
                loginState.needsLogin = true;
                loginState.notify();
            }
            do {
                try {
                    byte[] buffer = new byte[256];
                    DatagramPacket p = new DatagramPacket(buffer, buffer.length);
                    socket.receive(p);
                    String packetText = new String(p.getData(), 0, p.getLength(), "ISO-8859-1");
                    log.info("Reader thread got packet >" + packetText + "<");
                    if (packetText.startsWith("INFO:Session opened:INFO")) {
                        synchronized (loginState) {
                            loginState.loggedIn = true;
                            loginState.invalidLogin = false;
                            loginState.notify();
                        }
                    } else if (packetText.startsWith("INFO:Auth failed:INFO")) {
                        synchronized (loginState) {
                            loginState.loggedIn = false;
                            loginState.invalidLogin = true;
                        }
                    } else if (packetText.startsWith("INFO:Access denied. Close current session:INFO")) {
                        synchronized (loginState) {
                            loginState.loggedIn = false;
                        }
                        break;
                    } else {
                        String moduleType = packetText.substring(0, 3);
                        String address = packetText.substring(3, 9);
                        log.info("Module type " + moduleType + " address " + address);
                        log.info("Module classes " + moduleClasses);
                        try {
                            Class<? extends DomintellModule> moduleClass = moduleClasses.get(moduleType);
                            log.info("Module class " + moduleClass);
                            if (moduleClass != null) {
                                DomintellAddress domintellAddress;
                                domintellAddress = new DomintellAddress("0x" + address);
                                DomintellModule module = getDomintellModule(moduleType, domintellAddress, moduleClass);
                                if (module != null) {
                                    module.processUpdate(packetText.substring(9).trim());
                                }
                            }
                        } catch (DomintellModuleException e) {
                            log.error("Impossible to get module", e);
                        } catch (InvalidDomintellAddressException e) {
                            log.error("Impossible to get module", e);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            } while (!isInterrupted());
        }
    }

    private class DomintellCommandPacket {

        private String command;

        public DomintellCommandPacket(String command) {
            this.command = command;
        }

        public String toString() {
            return command;
        }
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
