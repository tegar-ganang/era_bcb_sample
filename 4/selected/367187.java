package playground.david.vis;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.rmi.RemoteException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.plans.Plan;
import org.matsim.utils.StringUtils;
import org.matsim.utils.collections.QuadTree.Rect;
import org.matsim.utils.vis.netvis.streaming.SimStateWriterI;
import playground.david.vis.data.OTFDefaultNetWriterFactoryImpl;
import playground.david.vis.data.OTFNetWriterFactory;
import playground.david.vis.data.OTFServerQuad;
import playground.david.vis.gui.OTFVisConfig;
import playground.david.vis.interfaces.OTFNetHandler;
import playground.david.vis.interfaces.OTFServerRemote;

public class OTFQuadFileHandler {

    private static final int BUFFERSIZE = 100000000;

    public static final int VERSION = 1;

    public static final int MINORVERSION = 3;

    public static class Writer implements SimStateWriterI {

        protected QueueNetworkLayer net = null;

        protected OTFServerQuad quad = null;

        private final String fileName;

        protected double intervall_s = 1, nextTime = -1;

        ;

        private ZipOutputStream zos = null;

        private DataOutputStream outFile;

        private final ByteBuffer buf = ByteBuffer.allocate(BUFFERSIZE);

        public Writer(double intervall_s, QueueNetworkLayer network, String fileName) {
            net = network;
            this.intervall_s = intervall_s;
            this.fileName = fileName;
        }

        public boolean dump(int time_s) throws IOException {
            if (time_s >= nextTime) {
                writeDynData(time_s);
                nextTime = time_s + intervall_s;
                return true;
            }
            return false;
        }

        private void writeInfos() throws IOException {
            zos.putNextEntry(new ZipEntry("info.bin"));
            outFile = new DataOutputStream(zos);
            outFile.writeInt(VERSION);
            outFile.writeInt(MINORVERSION);
            outFile.writeDouble(intervall_s);
            zos.closeEntry();
        }

        protected void onAdditionalQuadData() {
        }

        private void writeQuad() throws IOException {
            zos.putNextEntry(new ZipEntry("quad.bin"));
            Gbl.startMeasurement();
            quad = new OTFServerQuad(net);
            System.out.print("build Quad on Server: ");
            Gbl.printElapsedTime();
            onAdditionalQuadData();
            Gbl.startMeasurement();
            quad.fillQuadTree(new OTFDefaultNetWriterFactoryImpl());
            System.out.print("fill writer Quad on Server: ");
            Gbl.printElapsedTime();
            Gbl.startMeasurement();
            new ObjectOutputStream(zos).writeObject(quad);
            zos.closeEntry();
        }

        private void writeConstData() throws IOException {
            zos.putNextEntry(new ZipEntry("const.bin"));
            outFile = new DataOutputStream(zos);
            buf.position(0);
            outFile.writeDouble(-1.);
            quad.writeConstData(buf);
            outFile.writeInt(buf.position());
            outFile.write(buf.array(), 0, buf.position());
            zos.closeEntry();
        }

        private void writeDynData(int time_s) throws IOException {
            zos.putNextEntry(new ZipEntry("step." + time_s + ".bin"));
            outFile = new DataOutputStream(zos);
            buf.position(0);
            outFile.writeDouble(time_s);
            quad.writeDynData(null, buf);
            outFile.writeInt(buf.position());
            outFile.write(buf.array(), 0, buf.position());
            zos.closeEntry();
        }

        public void open() {
            try {
                zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(fileName), BUFFERSIZE));
            } catch (FileNotFoundException e1) {
                e1.printStackTrace();
            }
            try {
                writeInfos();
                writeQuad();
                writeConstData();
                System.out.print("write to file  Quad on Server: ");
                Gbl.printElapsedTime();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void close() throws IOException {
            try {
                zos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static class Reader implements OTFServerRemote {

        private final String fileName;

        public Reader(String fname) {
            this.fileName = fname;
            openAndReadInfo();
        }

        private ZipFile zipFile = null;

        private final ZipInputStream inStream = null;

        private DataInputStream inFile;

        protected OTFServerQuad quad = null;

        private final String id = null;

        private byte[] actBuffer = null;

        protected double intervall_s = -1, nextTime = -1;

        TreeMap<Integer, Long> timesteps = new TreeMap<Integer, Long>();

        public void scanZIPFile() throws IOException {
            nextTime = -1;
            Enumeration zipFileEntries = zipFile.entries();
            Gbl.startMeasurement();
            while (zipFileEntries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) zipFileEntries.nextElement();
                String currentEntry = entry.getName();
                System.out.println("Found: " + entry);
                if (currentEntry.contains("step")) {
                    String regex = "";
                    String[] spliti = StringUtils.explode(currentEntry, '.', 10);
                    int time_s = Integer.parseInt(spliti[1]);
                    if (nextTime == -1) nextTime = time_s;
                    timesteps.put(time_s, entry.getSize());
                }
            }
            Gbl.printElapsedTime();
            Gbl.printMemoryUsage();
        }

        public byte[] readTimeStep(int time_s) throws IOException {
            ZipEntry entry = zipFile.getEntry("step." + time_s + ".bin");
            byte[] buffer = new byte[(int) timesteps.get(time_s).longValue()];
            inFile = new DataInputStream(new BufferedInputStream(zipFile.getInputStream(entry)));
            readStateBuffer(buffer);
            return buffer;
        }

        public void readZIPFile2() throws IOException {
            Enumeration zipFileEntries = zipFile.entries();
            while (zipFileEntries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) zipFileEntries.nextElement();
                String currentEntry = entry.getName();
                System.out.println("Extracting: " + entry);
                if (currentEntry.contains("step")) {
                    String regex = "";
                    String[] spliti = StringUtils.explode(currentEntry, '.', 10);
                    int time_s = Integer.parseInt(spliti[1]);
                }
            }
        }

        private void openAndReadInfo() {
            try {
                File sourceZipFile = new File(fileName);
                zipFile = new ZipFile(sourceZipFile, ZipFile.OPEN_READ);
                ZipEntry infoEntry = zipFile.getEntry("info.bin");
                inFile = new DataInputStream(zipFile.getInputStream(infoEntry));
                int version = inFile.readInt();
                int minorversion = inFile.readInt();
                intervall_s = inFile.readDouble();
                OTFVisConfig config = (OTFVisConfig) Gbl.getConfig().getModule(OTFVisConfig.GROUP_NAME);
                config.setFileVersion(version);
                config.setFileMinorVersion(minorversion);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        public void readQuad() {
            try {
                scanZIPFile();
                ZipEntry quadEntry = zipFile.getEntry("quad.bin");
                BufferedInputStream is = new BufferedInputStream(zipFile.getInputStream(quadEntry));
                try {
                    this.quad = (OTFServerQuad) new ObjectInputStream(is).readObject();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public boolean hasNextTimeStep() {
            return timesteps.get((int) nextTime) != null;
        }

        public void getNextTimeStep() {
        }

        public Plan getAgentPlan(String id) throws RemoteException {
            return null;
        }

        public int getLocalTime() throws RemoteException {
            return (int) nextTime;
        }

        public OTFVisNet getNet(OTFNetHandler handler) throws RemoteException {
            throw new RemoteException("getNet not implemented for QuadFileHandler");
        }

        public void readStateBuffer(byte[] result) throws RemoteException {
            int size = 0;
            try {
                double timenextTime = inFile.readDouble();
                size = inFile.readInt();
                int offset = 0;
                int remain = size;
                int read = 0;
                while ((remain > 0) && (read != -1)) {
                    read = inFile.read(result, offset, remain);
                    remain -= read;
                    offset += read;
                }
                if (offset != size) {
                    throw new IOException("READ SIZE did not fit! File corrupted!");
                }
            } catch (IOException e) {
                System.out.println(e.toString());
            }
        }

        public void pause() throws RemoteException {
        }

        public void play() throws RemoteException {
        }

        public void setStatus(int status) throws RemoteException {
        }

        public void step() throws RemoteException {
            actBuffer = null;
        }

        public boolean isLive() {
            return false;
        }

        public OTFServerQuad getQuad(String id, OTFNetWriterFactory writers) throws RemoteException {
            if (writers != null) throw new RemoteException("writers need to be NULL, when reading from file");
            if (this.id == null) readQuad();
            if (id != null && !id.equals(this.id)) throw new RemoteException("id does not match, set id to NULL will match ALL!");
            return quad;
        }

        public byte[] getQuadConstStateBuffer(String id) throws RemoteException {
            ZipEntry entry = zipFile.getEntry("const.bin");
            byte[] buffer = new byte[(int) entry.getSize()];
            try {
                inFile = new DataInputStream(zipFile.getInputStream(entry));
                readStateBuffer(buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return buffer;
        }

        public byte[] getQuadDynStateBuffer(String id, Rect bounds) throws RemoteException {
            if (actBuffer == null) actBuffer = getStateBuffer();
            return actBuffer;
        }

        public byte[] getStateBuffer() throws RemoteException {
            byte[] buffer = null;
            try {
                buffer = readTimeStep((int) nextTime);
            } catch (IOException e) {
                e.printStackTrace();
            }
            int time = 0;
            Iterator<Integer> it = timesteps.keySet().iterator();
            while (it.hasNext() && time <= nextTime) time = it.next();
            if (time == nextTime) {
                time = timesteps.firstKey();
            }
            nextTime = time;
            return buffer;
        }

        public boolean requestNewTime(int time, TimePreference searchDirection) throws RemoteException {
            int lastTime = -1;
            int foundTime = -1;
            for (Integer timestep : timesteps.keySet()) {
                if (searchDirection == TimePreference.EARLIER) {
                    if (timestep >= time) {
                        foundTime = lastTime;
                        break;
                    }
                } else {
                    if (timestep >= time) {
                        foundTime = timestep;
                        break;
                    }
                }
                lastTime = timestep;
            }
            if (foundTime == -1) return false;
            nextTime = foundTime;
            actBuffer = null;
            return true;
        }
    }
}
