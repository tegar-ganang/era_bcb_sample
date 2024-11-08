package sf2.vm.impl.xen;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.StringTokenizer;
import sf2.core.Config;
import sf2.core.ConfigException;
import sf2.core.ProcessExecutor;
import sf2.core.ProcessExecutorException;
import sf2.vm.AbstractVMNetwork;
import sf2.vm.PortsMapping;
import sf2.vm.VMException;
import sf2.vm.VMNetwork;
import sf2.vm.VirtualMachine;

public class XenNetwork extends AbstractVMNetwork {

    private static final long serialVersionUID = 1L;

    protected static final String PROP_NAT_CMD = "sf2.vm.impl.xen.natCmd";

    protected static final String DEFAULT_NAT_CMD = "scripts/xennat.py";

    protected static final String PROP_BRIDGE_CMD = "sf2.vm.impl.xen.bridgeCmd";

    protected static final String DEFAULT_BRIDGE_CMD = "scripts/xenbridge.py";

    protected static final String PROP_DHCP_MAPPER = "sf2.vm.impl.xen.dhcpMapper";

    protected static final String DEFAULT_DHCP_MAPPER = "dhcp.mapper";

    protected static String natCmd, bridgeCmd;

    protected static String dhcpMapper;

    protected static final String[] dhcpList = { "192.168.122.238", "192.168.122.83" };

    protected static int dhcpIndex = 0;

    protected static Map<PortsMapping, String> dstMap = new HashMap<PortsMapping, String>();

    static {
        try {
            Config config = Config.search();
            natCmd = config.get(PROP_NAT_CMD, DEFAULT_NAT_CMD);
            bridgeCmd = config.get(PROP_BRIDGE_CMD, DEFAULT_BRIDGE_CMD);
            dhcpMapper = config.get(PROP_DHCP_MAPPER, DEFAULT_DHCP_MAPPER);
        } catch (ConfigException e) {
            e.printStackTrace();
        }
    }

    protected transient boolean running = false;

    protected transient RandomAccessFile mapperFile;

    public void configure(Config config, InetAddress local) throws VMException {
        super.configure(config, local);
        switch(type) {
            case NAT:
                if (ports.size() == 0) throw new VMException("port must be set.");
                break;
            case BRIDGE:
                break;
            case STATIC:
                if (mac == null || addr == null) throw new VMException("mac or addr must be set.");
                break;
        }
    }

    public void reference() throws VMException {
        switch(type) {
            case BRIDGE:
                referenceBridge();
                break;
        }
    }

    public void startBefore(VirtualMachine vm) throws VMException {
        switch(type) {
            case BRIDGE:
                startBeforeBridge(vm);
                break;
            case STATIC:
                startBeforeStatic(vm);
                break;
        }
    }

    public void start(VirtualMachine vm) throws VMException {
        switch(type) {
            case BRIDGE:
                startBridge(vm);
                break;
            case NAT:
                startNAT(vm);
                break;
        }
    }

    public void restore(VirtualMachine vm) throws VMException {
        switch(type) {
            case NAT:
                startNAT(vm);
                break;
        }
    }

    public void stop() {
        try {
            switch(type) {
                case BRIDGE:
                    stopBridge();
                    break;
                case NAT:
                    stopNAT();
                    break;
            }
        } catch (VMException e) {
        }
    }

    protected void startBeforeStatic(VirtualMachine vm) throws VMException {
        try {
            logging.debug(LOG_NAME, "starting static...");
            if (!running) {
                running = true;
                ProcessExecutor.exec(true, bridgeCmd, ((XenVirtualMachine) vm).image.getPath(), mac);
            }
        } catch (ProcessExecutorException e) {
            throw new VMException(e);
        }
    }

    class DHCPEntry {

        public String mac, addr, status;

        public DHCPEntry(String mac, String addr, String status) {
            this.mac = mac;
            this.addr = addr;
            this.status = status;
        }
    }

    protected void referenceBridge() throws VMException {
        if (running) {
            logging.debug(LOG_NAME, "prepare bridge...");
            try {
                mapperFile = new RandomAccessFile(dhcpMapper, "rw");
                mapperFile.getChannel().lock();
                LinkedList<DHCPEntry> entries = new LinkedList<DHCPEntry>();
                String line = null, mac = null, addr = null, status = null;
                while ((line = mapperFile.readLine()) != null) {
                    StringTokenizer tokenizer = new StringTokenizer(line);
                    mac = tokenizer.nextToken();
                    addr = tokenizer.nextToken();
                    status = tokenizer.nextToken();
                    entries.add(new DHCPEntry(mac, addr, status));
                }
                mapperFile.seek(0);
                mapperFile.setLength(0);
                boolean found = false;
                String current = this.addr.getHostAddress();
                for (DHCPEntry e : entries) {
                    if (!found && current.equals(e.addr)) {
                        int ref = Integer.parseInt(e.status);
                        e.status = Integer.toString(++ref);
                        found = true;
                    }
                    mapperFile.writeBytes(e.mac + "\t" + e.addr + "\t\t" + e.status + "\n");
                }
                mapperFile.close();
            } catch (FileNotFoundException e) {
                throw new VMException(e);
            } catch (IOException e) {
                throw new VMException(e);
            }
        }
    }

    protected void startBeforeBridge(VirtualMachine vm) throws VMException {
        if (!running) {
            logging.debug(LOG_NAME, "starting bridge....");
            try {
                mapperFile = new RandomAccessFile(dhcpMapper, "rw");
                mapperFile.getChannel().lock();
                LinkedList<DHCPEntry> entries = new LinkedList<DHCPEntry>();
                String line = null, mac = null, addr = null, status = null;
                while ((line = mapperFile.readLine()) != null) {
                    StringTokenizer tokenizer = new StringTokenizer(line);
                    mac = tokenizer.nextToken();
                    addr = tokenizer.nextToken();
                    status = tokenizer.nextToken();
                    entries.add(new DHCPEntry(mac, addr, status));
                }
                mapperFile.seek(0);
                mapperFile.setLength(0);
                boolean found = false;
                for (DHCPEntry e : entries) {
                    if (!found && e.status.equals("unused")) {
                        e.status = "1";
                        this.addr = InetAddress.getByName(e.addr);
                        mac = e.mac;
                        ProcessExecutor.exec(true, bridgeCmd, ((XenVirtualMachine) vm).image.getPath(), mac);
                        found = true;
                    }
                    mapperFile.writeBytes(e.mac + "\t" + e.addr + "\t\t" + e.status + "\n");
                }
                running = true;
            } catch (FileNotFoundException e) {
                throw new VMException(e);
            } catch (IOException e) {
                throw new VMException(e);
            } catch (ProcessExecutorException e) {
                throw new VMException(e);
            }
        }
    }

    protected void startBridge(VirtualMachine vm) throws VMException {
        try {
            if (running) {
                mapperFile.close();
            }
        } catch (IOException e) {
            throw new VMException(e);
        }
    }

    protected void stopBridge() throws VMException {
        if (running) {
            running = false;
            logging.debug(LOG_NAME, "stopping bridge....");
            try {
                RandomAccessFile file = new RandomAccessFile(dhcpMapper, "rw");
                file.getChannel().lock();
                LinkedList<DHCPEntry> entries = new LinkedList<DHCPEntry>();
                String line = null, mac = null, addr = null, status = null;
                while ((line = file.readLine()) != null) {
                    StringTokenizer tokenizer = new StringTokenizer(line);
                    mac = tokenizer.nextToken();
                    addr = tokenizer.nextToken();
                    status = tokenizer.nextToken();
                    entries.add(new DHCPEntry(mac, addr, status));
                }
                file.seek(0);
                file.setLength(0);
                boolean found = false;
                String current = this.addr.getHostAddress();
                for (DHCPEntry e : entries) {
                    if (!found && current.equals(e.addr)) {
                        int ref = Integer.parseInt(e.status);
                        ref--;
                        if (ref <= 0) e.status = "unused"; else e.status = Integer.toString(ref);
                        found = true;
                    }
                    file.writeBytes(e.mac + "\t" + e.addr + "\t\t" + e.status + "\n");
                }
                file.close();
            } catch (FileNotFoundException e) {
                throw new VMException(e);
            } catch (IOException e) {
                throw new VMException(e);
            }
        }
    }

    protected void startNAT(VirtualMachine vm) throws VMException {
        if (!running) {
            logging.config(LOG_NAME, "starting network...");
            running = true;
            for (PortsMapping port : ports) enableNetwork(port, true);
        }
    }

    protected void stopNAT() throws VMException {
        if (running) {
            logging.config(LOG_NAME, "stopping network...");
            running = false;
            for (PortsMapping port : ports) enableNetwork(port, false);
        }
    }

    protected void enableNetwork(PortsMapping port, boolean enable) throws VMException {
        try {
            String srcPort = (port.srcPort == port.srcPortEnd) ? Integer.toString(port.srcPort) : port.srcPort + ":" + port.srcPortEnd;
            String dstPort = (port.dstPort == port.dstPortEnd) ? Integer.toString(port.dstPort) : port.dstPort + "-" + port.dstPortEnd;
            String src = hostAddr.getHostAddress();
            String dst = null;
            synchronized (XenNetwork.class) {
                if (enable) {
                    dst = dhcpList[dhcpIndex++];
                    dstMap.put(port, dst);
                } else {
                    dst = dstMap.get(port);
                }
            }
            logging.config(LOG_NAME, "start/stop network via " + natCmd + " src=" + src + ", sport=" + srcPort + ", dst=" + dst + ", dport=" + dstPort + " enable=" + enable);
            ProcessExecutor.exec(true, natCmd, src, srcPort, dst, dstPort, enable ? "start" : "stop");
        } catch (ProcessExecutorException e) {
            throw new VMException(e);
        }
    }

    public InetAddress getGuestAddress() {
        switch(type) {
            case BRIDGE:
                return addr;
            case STATIC:
                return addr;
            case NAT:
                return hostAddr;
        }
        return null;
    }

    public String toString() {
        return "{running=" + running + ", mac=" + mac + ", addr=" + addr + "}";
    }
}
