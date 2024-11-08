package org.matsim.utils.vis.otfivs.server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
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
import org.matsim.utils.vis.otfivs.data.OTFDefaultNetWriterFactoryImpl;
import org.matsim.utils.vis.otfivs.data.OTFNetWriterFactory;
import org.matsim.utils.vis.otfivs.data.OTFServerQuad;
import org.matsim.utils.vis.otfivs.gui.OTFVisConfig;
import org.matsim.utils.vis.otfivs.handler.OTFAgentsListHandler;
import org.matsim.utils.vis.otfivs.handler.OTFDefaultLinkHandler;
import org.matsim.utils.vis.otfivs.handler.OTFDefaultNodeHandler;
import org.matsim.utils.vis.otfivs.handler.OTFLinkAgentsHandler;
import org.matsim.utils.vis.otfivs.handler.OTFLinkAgentsNoParkingHandler;
import org.matsim.utils.vis.otfivs.handler.OTFLinkTravelTimesHandler;
import org.matsim.utils.vis.otfivs.handler.OTFNoDynLinkHandler;
import org.matsim.utils.vis.otfivs.interfaces.OTFServerRemote;
import org.matsim.utils.vis.snapshots.writers.PositionInfo;
import org.matsim.utils.vis.snapshots.writers.SnapshotWriterI;

public class OTFQuadFileHandler {

    private static final int BUFFERSIZE = 100000000;

    public static final int VERSION = 1;

    public static final int MINORVERSION = 3;

    public static class Writer implements SimStateWriterI, SnapshotWriterI {

        protected QueueNetworkLayer net = null;

        protected OTFServerQuad quad = null;

        private final String fileName;

        protected double intervall_s = 1, nextTime = -1;

        ;

        private ZipOutputStream zos = null;

        private DataOutputStream outFile;

        private boolean isOpen = false;

        private final ByteBuffer buf = ByteBuffer.allocate(BUFFERSIZE);

        public Writer(double intervall_s, QueueNetworkLayer network, String fileName) {
            this.net = network;
            this.intervall_s = intervall_s;
            this.fileName = fileName;
        }

        public boolean dump(int time_s) throws IOException {
            if (time_s >= this.nextTime) {
                writeDynData(time_s);
                this.nextTime = time_s + this.intervall_s;
                return true;
            }
            return false;
        }

        private void writeInfos() throws IOException {
            this.zos.putNextEntry(new ZipEntry("info.bin"));
            this.outFile = new DataOutputStream(this.zos);
            this.outFile.writeInt(VERSION);
            this.outFile.writeInt(MINORVERSION);
            this.outFile.writeDouble(this.intervall_s);
            this.zos.closeEntry();
        }

        protected void onAdditionalQuadData() {
        }

        private void writeQuad() throws IOException {
            this.zos.putNextEntry(new ZipEntry("quad.bin"));
            Gbl.startMeasurement();
            this.quad = new OTFServerQuad(this.net);
            System.out.print("build Quad on Server: ");
            Gbl.printElapsedTime();
            onAdditionalQuadData();
            Gbl.startMeasurement();
            this.quad.fillQuadTree(new OTFDefaultNetWriterFactoryImpl());
            System.out.print("fill writer Quad on Server: ");
            Gbl.printElapsedTime();
            Gbl.startMeasurement();
            new ObjectOutputStream(this.zos).writeObject(this.quad);
            this.zos.closeEntry();
        }

        private void writeConstData() throws IOException {
            this.zos.putNextEntry(new ZipEntry("const.bin"));
            this.outFile = new DataOutputStream(this.zos);
            this.buf.position(0);
            this.outFile.writeDouble(-1.);
            this.quad.writeConstData(this.buf);
            this.outFile.writeInt(this.buf.position());
            this.outFile.write(this.buf.array(), 0, this.buf.position());
            this.zos.closeEntry();
        }

        private void writeDynData(int time_s) throws IOException {
            this.zos.putNextEntry(new ZipEntry("step." + time_s + ".bin"));
            this.outFile = new DataOutputStream(this.zos);
            this.buf.position(0);
            this.outFile.writeDouble(time_s);
            this.quad.writeDynData(null, this.buf);
            this.outFile.writeInt(this.buf.position());
            this.outFile.write(this.buf.array(), 0, this.buf.position());
            this.zos.closeEntry();
        }

        public void open() {
            isOpen = true;
            try {
                this.zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(this.fileName), BUFFERSIZE));
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

        public void close() {
            try {
                this.zos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void addAgent(PositionInfo position) {
        }

        public void beginSnapshot(double time) {
            if (!isOpen) open();
            try {
                dump((int) time);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void endSnapshot() {
        }

        public void finish() {
            close();
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
            this.nextTime = -1;
            Enumeration zipFileEntries = this.zipFile.entries();
            Gbl.startMeasurement();
            while (zipFileEntries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) zipFileEntries.nextElement();
                String currentEntry = entry.getName();
                System.out.println("Found: " + entry);
                if (currentEntry.contains("step")) {
                    String regex = "";
                    String[] spliti = StringUtils.explode(currentEntry, '.', 10);
                    int time_s = Integer.parseInt(spliti[1]);
                    if (this.nextTime == -1) this.nextTime = time_s;
                    this.timesteps.put(time_s, entry.getSize());
                }
            }
            Gbl.printElapsedTime();
            Gbl.printMemoryUsage();
        }

        public byte[] readTimeStep(int time_s) throws IOException {
            ZipEntry entry = this.zipFile.getEntry("step." + time_s + ".bin");
            byte[] buffer = new byte[(int) this.timesteps.get(time_s).longValue()];
            this.inFile = new DataInputStream(new BufferedInputStream(this.zipFile.getInputStream(entry)));
            readStateBuffer(buffer);
            return buffer;
        }

        public void readZIPFile2() throws IOException {
            Enumeration zipFileEntries = this.zipFile.entries();
            while (zipFileEntries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) zipFileEntries.nextElement();
                String currentEntry = entry.getName();
                System.out.println("Extracting: " + entry);
                if (currentEntry.contains("step")) {
                }
            }
        }

        private void openAndReadInfo() {
            try {
                File sourceZipFile = new File(this.fileName);
                this.zipFile = new ZipFile(sourceZipFile, ZipFile.OPEN_READ);
                ZipEntry infoEntry = this.zipFile.getEntry("info.bin");
                this.inFile = new DataInputStream(this.zipFile.getInputStream(infoEntry));
                int version = this.inFile.readInt();
                int minorversion = this.inFile.readInt();
                this.intervall_s = this.inFile.readDouble();
                OTFVisConfig config = (OTFVisConfig) Gbl.getConfig().getModule(OTFVisConfig.GROUP_NAME);
                config.setFileVersion(version);
                config.setFileMinorVersion(minorversion);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        public static class OTFObjectInputStream extends ObjectInputStream {

            public OTFObjectInputStream(InputStream in) throws IOException {
                super(in);
            }

            @Override
            protected Class resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
                String name = desc.getName();
                if (name.equals("playground.david.vis.data.OTFServerQuad")) return OTFServerQuad.class; else if (name.equals("playground.david.vis.handler.OTFDefaultNodeHandler$Writer")) return OTFDefaultNodeHandler.Writer.class; else if (name.equals("playground.david.vis.handler.OTFDefaultLinkHandler$Writer")) return OTFDefaultLinkHandler.Writer.class; else if (name.equals("playground.david.vis.handler.OTFLinkAgentsHandler$Writer")) return OTFLinkAgentsHandler.Writer.class; else if (name.equals("playground.david.vis.handler.OTFLinkAgentsNoParkingHandler$Writer")) return OTFLinkAgentsNoParkingHandler.Writer.class; else if (name.equals("playground.david.vis.handler.OTFLinkTravelTimesHandler$Writer")) return OTFLinkTravelTimesHandler.Writer.class; else if (name.equals("playground.david.vis.handler.OTFNoDynLinkHandler$Writer")) return OTFNoDynLinkHandler.Writer.class; else if (name.equals("playground.david.vis.handler.OTFAgentsListHandler$Writer")) return OTFAgentsListHandler.Writer.class; else if (name.startsWith("playground.david.vis")) {
                    name = name.replaceFirst("playground.david.vis", "org.matsim.utils.vis.otfvis");
                    return Class.forName(name);
                }
                return super.resolveClass(desc);
            }
        }

        public void readQuad() {
            try {
                scanZIPFile();
                ZipEntry quadEntry = this.zipFile.getEntry("quad.bin");
                BufferedInputStream is = new BufferedInputStream(this.zipFile.getInputStream(quadEntry));
                try {
                    this.quad = (OTFServerQuad) new OTFObjectInputStream(is).readObject();
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
            return this.timesteps.get((int) this.nextTime) != null;
        }

        public void getNextTimeStep() {
        }

        public Plan getAgentPlan(String id) throws RemoteException {
            return null;
        }

        public int getLocalTime() throws RemoteException {
            return (int) this.nextTime;
        }

        public void readStateBuffer(byte[] result) throws RemoteException {
            int size = 0;
            try {
                double timenextTime = this.inFile.readDouble();
                size = this.inFile.readInt();
                int offset = 0;
                int remain = size;
                int read = 0;
                while ((remain > 0) && (read != -1)) {
                    read = this.inFile.read(result, offset, remain);
                    remain -= read;
                    offset += read;
                }
                if (offset != size) {
                    throw new IOException("READ SIZE did not fit! File corrupted! in second " + timenextTime);
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
            this.actBuffer = null;
        }

        public boolean isLive() {
            return false;
        }

        public OTFServerQuad getQuad(String id, OTFNetWriterFactory writers) throws RemoteException {
            if (writers != null) throw new RemoteException("writers need to be NULL, when reading from file");
            if (this.id == null) readQuad();
            if ((id != null) && !id.equals(this.id)) throw new RemoteException("id does not match, set id to NULL will match ALL!");
            return this.quad;
        }

        public byte[] getQuadConstStateBuffer(String id) throws RemoteException {
            ZipEntry entry = this.zipFile.getEntry("const.bin");
            byte[] buffer = new byte[(int) entry.getSize()];
            try {
                this.inFile = new DataInputStream(this.zipFile.getInputStream(entry));
                readStateBuffer(buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return buffer;
        }

        public byte[] getQuadDynStateBuffer(String id, Rect bounds) throws RemoteException {
            if (this.actBuffer == null) this.actBuffer = getStateBuffer();
            return this.actBuffer;
        }

        public byte[] getStateBuffer() throws RemoteException {
            byte[] buffer = null;
            try {
                buffer = readTimeStep((int) this.nextTime);
            } catch (IOException e) {
                e.printStackTrace();
            }
            int time = 0;
            Iterator<Integer> it = this.timesteps.keySet().iterator();
            while (it.hasNext() && (time <= this.nextTime)) time = it.next();
            if (time == this.nextTime) {
                time = this.timesteps.firstKey();
            }
            this.nextTime = time;
            return buffer;
        }

        public boolean requestNewTime(int time, TimePreference searchDirection) throws RemoteException {
            int lastTime = -1;
            int foundTime = -1;
            for (Integer timestep : this.timesteps.keySet()) {
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
            this.nextTime = foundTime;
            this.actBuffer = null;
            return true;
        }
    }
}
