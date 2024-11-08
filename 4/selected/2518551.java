package com.virtela.poller;

import java.io.*;
import java.util.regex.*;
import java.nio.*;
import java.nio.charset.*;
import java.nio.channels.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * RTGTargetParser parses and holds device, interface and target information based on the target file.
 *
 * @author Jonathan Leech
 * @author Marc Brashear
 * @version CVS $Revision: 1.1.1.1 $
 * @since 1.0
 */
public class RTGTargetParser implements TargetParser {

    /**
    * Returns a read-only CharBuffer of the targets file.
    *
    * @param filename path to the targets file
    * @return cbuf the CharBuffer of the file
    * @throws IOException  If an input or output exception occurred
    */
    protected static CharSequence fromFile(String filename) throws IOException {
        FileInputStream fis = new FileInputStream(filename);
        FileChannel fc = fis.getChannel();
        ByteBuffer bbuf = fc.map(FileChannel.MapMode.READ_ONLY, 0, (int) fc.size());
        CharBuffer cbuf = Charset.forName("8859_1").newDecoder().decode(bbuf);
        return cbuf;
    }

    /**
    * Returns a CharSequence representing the next portion of the targets file.
    * Returns null if we are the end of the CharBuffer.
    * Combines portions of the targets file that are between double quotes.
    * Skips comment lines, which is any line starting with a '#'.
    *
    * @return token the next CharSequence from the CharBuffer
    */
    protected CharSequence nextToken() {
        CharSequence token = null;
        int previousStart = start;
        if (start >= cs.length()) {
            return null;
        } else if (m.find()) {
            token = cs.subSequence(start, m.start());
            start = m.end();
        } else {
            token = cs.subSequence(start, cs.length());
            start = cs.length();
        }
        if (token.toString().startsWith("\"")) {
            StringBuffer sb = new StringBuffer();
            sb.append(token.toString());
            while (!token.toString().endsWith("\"")) {
                token = nextToken();
                sb.append(token.toString());
            }
            return sb;
        } else if (token.toString().startsWith("#")) {
            Character previousChar = null;
            if (previousStart > 0) {
                previousChar = cs.charAt(previousStart - 1);
            }
            if (previousChar == null || previousChar == '\r' || previousChar == '\n') {
                while (!m.group().contains("\r") && !m.group().contains("\n")) {
                    token = nextToken();
                }
                token = nextToken();
            }
        }
        return token;
    }

    /**
    * Creates an RTGTargetParser with the path to the targets file.
    *
    * @param filename the path to the targets file
    */
    public RTGTargetParser(Configuration configuration) {
        this.filename = configuration.getTargetFileName();
    }

    String filename;

    Collection<Device> devices = null;

    /**
    * Returns the Devices read from the targets file.
    * Interfaces and Targets returned in the Device.
    * 
    * @return the devices read from the targets file.
    */
    public synchronized Collection<Device> getDevices() {
        return devices;
    }

    /**
    * Populates the collection of Devices which include the interface and targets from the file.
    * synchronized to ensure reparsing does not overrun an already parseTarget
    *
    * @throws  IOException  If an input or output exception occurred
    */
    public synchronized void parseTargets() throws IOException {
        cs = fromFile(filename);
        m = pattern.matcher(cs);
        start = 0;
        Long rid = null;
        String IPAddress = null;
        String SNMPString = null;
        String name = null;
        boolean counter = true;
        String oid = null;
        Integer SNMPVersion = null;
        Long speed = null;
        Long interfaceID = null;
        ArrayList<Interface> hostInterfaces = new ArrayList<Interface>();
        ArrayList<Target> interfaceTargets = new ArrayList<Target>();
        devices = new ArrayList<Device>();
        Set<String> interfacesHash = new HashSet<String>();
        Device device = null;
        Interface hostInterface = null;
        Target target = null;
        String token = nextToken().toString();
        while (token != null) {
            if ("host".equals(token)) {
                if (IPAddress != null && rid != null) {
                    if (interfaceID != null) {
                        hostInterface = new Interface(interfaceID, null, 0, interfaceTargets);
                        hostInterfaces.add(hostInterface);
                    }
                    device = new Device(rid, null, IPAddress, SNMPString, SNMPVersion, null, hostInterfaces);
                    for (Interface devint : hostInterfaces) {
                        devint.setDevice(device);
                    }
                    devices.add(device);
                    rid = null;
                    IPAddress = null;
                    SNMPString = null;
                    name = null;
                    oid = null;
                    SNMPVersion = null;
                    interfaceID = null;
                    hostInterfaces = new ArrayList<Interface>();
                    interfaceTargets = new ArrayList<Target>();
                }
                IPAddress = nextToken().toString().intern();
            } else if ("community".equals(token)) {
                SNMPString = nextToken().toString().intern();
            } else if ("target".equals(token)) {
                oid = nextToken().toString().substring(1).intern();
            } else if ("table".equals(token)) {
                token = nextToken().toString();
                String[] tableSplit = tablePattern.split(token.toString());
                name = tableSplit[0].intern();
                if (rid == null) {
                    rid = Long.parseLong(tableSplit[1]);
                }
            } else if ("id".equals(token)) {
                token = nextToken().toString().intern();
                if (!interfacesHash.contains(token)) {
                    if (interfaceID != null) {
                        hostInterface = new Interface(interfaceID, null, 0, interfaceTargets);
                        hostInterfaces.add(hostInterface);
                    }
                    interfaceID = Long.parseLong(token);
                    interfacesHash.add(token);
                    interfaceTargets = new ArrayList<Target>();
                    target = new Target(null, oid, counter, name);
                    interfaceTargets.add(target);
                } else if (interfaceID != null) {
                    target = new Target(null, oid, counter, name);
                    if (Long.parseLong(token) == interfaceID) {
                        interfaceTargets.add(target);
                    } else {
                        for (Interface ints : hostInterfaces) {
                            Interface devints = ints;
                            if (Long.parseLong(token) == devints.getIID()) {
                                ints.addTarget(target);
                            }
                        }
                    }
                } else {
                    target = new Target(null, oid, counter, name);
                    for (Interface ints : hostInterfaces) {
                        Interface devints = ints;
                        if (Long.parseLong(token) == devints.getIID()) {
                            ints.addTarget(target);
                        }
                    }
                    interfaceTargets = new ArrayList<Target>();
                    interfaceID = null;
                    hostInterface = null;
                }
            } else if ("speed".equals(token)) {
                if (hostInterface != null) {
                    hostInterface.setSpeed(Long.parseLong(nextToken().toString()));
                }
            } else if ("bits".equals(token)) {
                counter = (!"0".equals(nextToken().toString()));
            } else if ("snmpver".equals(token)) {
                SNMPVersion = Integer.parseInt(nextToken().toString());
            }
            CharSequence tmp = nextToken();
            if (tmp == null) {
                token = null;
            } else {
                token = tmp.toString();
            }
        }
        boolean unstoredDevice = true;
        for (Device dev : devices) {
            if (rid == dev.getRID()) {
                unstoredDevice = false;
            }
        }
        if (unstoredDevice) {
            hostInterface = new Interface(interfaceID, null, 0, interfaceTargets);
            hostInterfaces.add(hostInterface);
            device = new Device(rid, null, IPAddress, SNMPString, SNMPVersion, null, hostInterfaces);
            for (Interface devint : hostInterfaces) {
                devint.setDevice(device);
            }
            devices.add(device);
        }
        int interfaceCount = 0;
        int targetCount = 0;
        for (Device dev : devices) {
            for (Interface devint : dev.getInterfaces()) {
                Interface tempDeviceInterface = devint;
                interfaceCount++;
                for (Target devinttar : tempDeviceInterface.getTargets()) {
                    targetCount++;
                    devinttar.setTargetInterface(tempDeviceInterface);
                }
                ;
                devint.setDevice(dev);
            }
        }
        logger.log(Level.INFO, "Parsed " + devices.size() + " devices, " + interfaceCount + " interfaces, and " + targetCount + " targets.");
        logger.log(Level.FINEST, "Devices: " + devices);
    }

    static Pattern tablePattern = Pattern.compile("_");

    static Pattern pattern = Pattern.compile("[\\s;{}]+");

    CharSequence cs;

    Matcher m;

    int start = 0;

    /**
    * Logger object for logging messages
    */
    static Logger logger = Logger.getLogger("com.virtela.poller.RTGTargetParser");
}
