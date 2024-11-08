package org.personalsmartspace.sre.pem.test;

import java.io.File;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.personalsmartspace.sre.pem.tools.Tools;
import org.personalsmartspace.sre.pem.tools.ControllableAddress;
import org.personalsmartspace.sre.pem.tools.IControllablesDisplay;
import org.personalsmartspace.sre.pem.tools.SearcherForControllables;
import junit.framework.*;

public class PersistenceManagementTests extends TestSuite {

    public static Test suite() {
        return new PersistenceManagementTests();
    }

    PersistenceManagementTests() {
        addTest(new ControlTestPersistence());
    }

    private class ControlTestPersistence extends TestCase {

        private final int inetAddressLength = Tools.isIPv6 ? 16 : 4;

        private final short intLength = 4;

        private final int read_chunk = 512;

        private int num_nodes;

        private int wait;

        private final String test1Key = "/persist/persistence/record/slot/1";

        private final String test2Key = "/persist/persistence/record/slot/1";

        private final String test1Value = "123477474773772727";

        private final String test2Value = "797579757477747499";

        Process[] processes;

        private Map<ControllableAddress, Set<ControllableAddress>> controllables;

        private SearcherForControllables searcher;

        private Map<ControllableAddress, ControllableAddress> hazelcast2controllable;

        private Map<ControllableAddress, ControllableAddress> controllable2hazelcast;

        private Map<ControllableAddress, ControllableAddress> controllable2soap;

        private Map<ControllableAddress, ControllableAddress> soap2controllable;

        private Socket soc;

        boolean isClosed = false;

        private byte[] data, length;

        public void setUp() throws Exception {
            super.setUp();
            String sn = System.getProperty("persistence.test.nodes");
            if (sn == null || "".equals(sn)) num_nodes = 0; else num_nodes = (new Integer(sn)).intValue();
            String sw = System.getProperty("persistence.test.wait");
            if (sw == null || "".equals(sw)) wait = 500; else wait = (new Integer(sw)).intValue();
            controllables = new HashMap<ControllableAddress, Set<ControllableAddress>>();
            hazelcast2controllable = new HashMap<ControllableAddress, ControllableAddress>();
            controllable2hazelcast = new HashMap<ControllableAddress, ControllableAddress>();
            controllable2soap = new HashMap<ControllableAddress, ControllableAddress>();
            soap2controllable = new HashMap<ControllableAddress, ControllableAddress>();
            searcher = new SearcherForControllables(controllables, hazelcast2controllable, controllable2hazelcast, controllable2soap, soap2controllable, new IControllablesDisplay() {

                public void addNode(ControllableAddress ca) {
                }

                ;

                public void removeNode(ControllableAddress ca) {
                }

                ;

                public void setMaster(ControllableAddress ca) {
                }

                ;

                public void setExMaster(ControllableAddress ca) {
                }

                ;

                public void setOrdinary(ControllableAddress ca) {
                }

                ;

                public void drawConnection(ControllableAddress ca, ControllableAddress cb) {
                }

                ;
            });
            String currentDir = System.getProperty("user.dir");
            if (currentDir.endsWith("/")) currentDir = currentDir.substring(0, currentDir.length());
            String mode = "";
            if (!(new File(currentDir + "/controllable-init.xargs")).exists()) {
                byte[] buf = new byte[read_chunk];
                int numread = 0;
                InputStream is = getClass().getResourceAsStream("/controllable-init.xargs");
                FileOutputStream os = new FileOutputStream(currentDir + "/controllable-init.xargs");
                while (true) {
                    numread = is.read(buf, 0, buf.length);
                    if (numread < 0) break;
                    os.write(buf, 0, numread);
                }
                mode = " -init -xargs controllable-init.xargs";
            }
            processes = new Process[num_nodes];
            for (int i = 0; i < num_nodes; i++) {
                processes[i] = Runtime.getRuntime().exec("java -XX:+UseParallelGC -Xdebug -Xnoagent" + " -Xrunjdwp:transport=dt_socket,address=" + (8001 + i) + ",server=y,suspend=n -Djava.compiler=NONE -Dpersistence.control.port=" + (19001 + i) + " -Dorg.osgi.service.http.port=" + (8081 + num_nodes + i) + " -Dorg.osgi.framework.dir=fw_persistence_node" + i + " -jar framework.jar" + mode + " >> /tmp/persistence.demo.log", null, null);
            }
            Thread.sleep(num_nodes * wait);
        }

        public void tearDown() throws Exception {
            super.tearDown();
            for (int i = 0; i < num_nodes; i++) processes[i].destroy();
        }

        public void runTest() {
            try {
                search();
                assertEquals(num_nodes + 1, controllables.size());
                testRejoin();
                ControllableAddress[] cas = controllables.keySet().toArray(new ControllableAddress[0]);
                set(cas[0], test1Key, test1Value);
                assertEquals(test1Value, recall(cas[num_nodes - 1], test1Key));
                for (int i = 0; i < num_nodes / 2; i++) {
                    for (int j = num_nodes / 2; j < num_nodes; j++) {
                        disconnect(cas[i], cas[j]);
                    }
                }
                testRefresh();
                set(cas[0], test2Key, test1Value);
                set(cas[num_nodes - 1], test1Key, test2Value);
                testRejoin();
                assertEquals(recall(cas[0], test1Key), test2Value);
                assertEquals(recall(cas[num_nodes - 1], test2Key), test1Value);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void testRefresh() throws Exception {
            ControllableAddress[] cas = controllables.keySet().toArray(new ControllableAddress[0]);
            for (ControllableAddress ca : cas) {
                refresh(ca);
                rejoin(ca, controllables.get(ca));
            }
        }

        private void testRejoin() throws Exception {
            ControllableAddress[] cas = controllables.keySet().toArray(new ControllableAddress[0]);
            for (ControllableAddress ca : cas) {
                rejoin(ca, controllables.keySet());
            }
        }

        private void search() throws IOException, InterruptedException {
            controllables.clear();
            searcher.search();
        }

        private void rejoin(ControllableAddress ca, Set<ControllableAddress> cons) throws IOException {
            byte[] hdr = { 0x0 };
            ControllableAddress sa = controllable2soap.get(ca);
            for (ControllableAddress cb : cons) {
                OpenConnection(cb);
                WriteData(hdr);
                if (isClosed) {
                    CloseConnection();
                    return;
                }
                WriteData(sa.getAddress().getAddress());
                if (isClosed) {
                    CloseConnection();
                    return;
                }
                WriteData(Tools.encode(sa.getPort()));
                CloseConnection();
            }
        }

        private void disconnect(ControllableAddress ca, ControllableAddress cb) throws IOException {
            ControllableAddress atr1, atr2, sa, sb;
            atr1 = controllable2hazelcast.get(ca);
            atr2 = controllable2hazelcast.get(cb);
            sa = controllable2soap.get(ca);
            sb = controllable2soap.get(cb);
            byte[] hdr = { 0x1 };
            OpenConnection(ca);
            WriteData(hdr);
            if (isClosed) {
                CloseConnection();
                return;
            }
            WriteData(atr1.getAddress().getAddress());
            if (isClosed) {
                CloseConnection();
                return;
            }
            WriteData(Tools.encode(atr1.getPort()));
            if (isClosed) {
                CloseConnection();
                return;
            }
            WriteData(sa.getAddress().getAddress());
            if (isClosed) {
                CloseConnection();
                return;
            }
            WriteData(Tools.encode(sa.getPort()));
            if (isClosed) {
                CloseConnection();
                return;
            }
            WriteData(hdr);
            if (isClosed) {
                CloseConnection();
                return;
            }
            WriteData(atr2.getAddress().getAddress());
            if (isClosed) {
                CloseConnection();
                return;
            }
            WriteData(Tools.encode(atr2.getPort()));
            if (isClosed) {
                CloseConnection();
                return;
            }
            WriteData(sb.getAddress().getAddress());
            if (isClosed) {
                CloseConnection();
                return;
            }
            WriteData(Tools.encode(sb.getPort()));
            CloseConnection();
            OpenConnection(cb);
            WriteData(hdr);
            if (isClosed) {
                CloseConnection();
                return;
            }
            WriteData(atr1.getAddress().getAddress());
            if (isClosed) {
                CloseConnection();
                return;
            }
            WriteData(Tools.encode(atr1.getPort()));
            if (isClosed) {
                CloseConnection();
                return;
            }
            WriteData(sa.getAddress().getAddress());
            if (isClosed) {
                CloseConnection();
                return;
            }
            WriteData(Tools.encode(sa.getPort()));
            if (isClosed) {
                CloseConnection();
                return;
            }
            WriteData(hdr);
            if (isClosed) {
                CloseConnection();
                return;
            }
            WriteData(atr2.getAddress().getAddress());
            if (isClosed) {
                CloseConnection();
                return;
            }
            WriteData(Tools.encode(atr2.getPort()));
            if (isClosed) {
                CloseConnection();
                return;
            }
            WriteData(sb.getAddress().getAddress());
            if (isClosed) {
                CloseConnection();
                return;
            }
            WriteData(Tools.encode(sb.getPort()));
            CloseConnection();
            controllables.get(ca).remove(cb);
            controllables.get(cb).remove(ca);
        }

        private Object recall(ControllableAddress ca, String key) throws Exception {
            if (key == null) return "";
            byte[] hdr = { 0x2 };
            OpenConnection(ca);
            WriteData(hdr);
            if (isClosed) {
                CloseConnection();
                return "";
            }
            data = key.getBytes();
            WriteData(Tools.encode(data.length));
            if (isClosed) {
                CloseConnection();
                return "";
            }
            WriteData(data);
            if (isClosed) {
                CloseConnection();
                return "";
            }
            hdr = ReadData(1);
            if (isClosed) {
                CloseConnection();
                return "";
            }
            if (hdr[0] != 0x2) {
                CloseConnection();
                return "";
            }
            int len = Tools.decode(ReadData(intLength));
            if (isClosed) {
                CloseConnection();
                return "";
            }
            data = ReadData(len);
            if (isClosed) {
                CloseConnection();
                return "";
            }
            CloseConnection();
            ByteArrayInputStream in = new ByteArrayInputStream(data);
            ObjectInputStream is = new ObjectInputStream(in);
            return is.readObject();
        }

        private void set(ControllableAddress ca, String key, Object value) throws IOException {
            if (key == null || value == null) return;
            byte[] hdr = { 0x3 };
            OpenConnection(ca);
            WriteData(hdr);
            if (isClosed) {
                CloseConnection();
                return;
            }
            data = key.getBytes();
            WriteData(Tools.encode(data.length));
            if (isClosed) {
                CloseConnection();
                return;
            }
            WriteData(data);
            if (isClosed) {
                CloseConnection();
                return;
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ObjectOutputStream os = new ObjectOutputStream(out);
            os.writeObject(value);
            data = out.toByteArray();
            WriteData(Tools.encode(data.length));
            if (isClosed) {
                CloseConnection();
                return;
            }
            WriteData(data);
            CloseConnection();
        }

        private void refresh(ControllableAddress ca) throws IOException {
            byte[] hdr = { 0x4 };
            OpenConnection(ca);
            WriteData(hdr);
            if (isClosed) {
                CloseConnection();
                return;
            }
            hdr = ReadData(1);
            if (isClosed) {
                CloseConnection();
                return;
            }
            if (hdr[0] != 0x4) {
                CloseConnection();
                return;
            }
            length = ReadData(intLength);
            int len = Tools.decode(length);
            if (isClosed) {
                CloseConnection();
                return;
            }
            data = ReadData(len * (inetAddressLength + intLength));
            if (isClosed) {
                CloseConnection();
                return;
            }
            Set<ControllableAddress> cons = new HashSet<ControllableAddress>();
            byte[] address = new byte[inetAddressLength];
            byte[] port = new byte[intLength];
            for (int i = 0; i < len; i++) {
                System.arraycopy(data, (inetAddressLength + intLength) * i, address, 0, inetAddressLength);
                System.arraycopy(data, (inetAddressLength + intLength) * i + inetAddressLength, port, 0, intLength);
                ControllableAddress cb = new ControllableAddress(InetAddress.getByAddress(address), Tools.decode(port));
                if (hazelcast2controllable.containsKey(cb)) cb = hazelcast2controllable.get(cb);
                cons.add(cb);
            }
            controllables.remove(ca);
            controllables.put(ca, cons);
            CloseConnection();
        }

        private void OpenConnection(ControllableAddress ca) throws IOException {
            soc = new Socket(ca.getAddress(), ca.getPort());
            isClosed = false;
        }

        private byte[] ReadData(int len) throws IOException {
            if (soc == null || isClosed) return new byte[0];
            byte[] pbuf = new byte[len];
            int off = 0;
            while (off < len) {
                off += soc.getInputStream().read(pbuf, off, len - off);
                if (off < 0) {
                    isClosed = true;
                    return pbuf;
                }
            }
            return pbuf;
        }

        private void WriteData(byte[] data) throws IOException {
            if (soc == null || soc.isClosed()) {
                isClosed = true;
                return;
            }
            OutputStream os = soc.getOutputStream();
            os.write(data);
            os.flush();
        }

        private void CloseConnection() throws IOException {
            if (soc == null) return;
            soc.close();
            isClosed = true;
            soc = null;
        }
    }
}
