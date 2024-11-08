package playground.david.vis.deprecated;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.plans.Plan;
import org.matsim.utils.collections.QuadTree.Rect;
import org.matsim.utils.vis.netvis.streaming.SimStateWriterI;
import playground.david.vis.OTFVisNet;
import playground.david.vis.data.OTFDefaultNetWriterFactoryImpl;
import playground.david.vis.data.OTFNetWriterFactory;
import playground.david.vis.data.OTFServerQuad;
import playground.david.vis.interfaces.OTFNetHandler;
import playground.david.vis.interfaces.OTFServerRemote;

public class OTFQuadFileHandlerOLD implements SimStateWriterI, OTFServerRemote {

    private static final int BUFFERSIZE = 100000000;

    private OutputStream outStream = null;

    private DataOutputStream outFile;

    private final String fileName;

    private QueueNetworkLayer net = null;

    private OTFServerQuad quad = null;

    private final String id = null;

    private byte[] actBuffer = null;

    double nextTime = -1;

    double intervall_s = 1;

    SortedMap<Double, Long> timeSteps = new TreeMap<Double, Long>();

    public OTFQuadFileHandlerOLD(double intervall_s, QueueNetworkLayer network, String fileName) {
        if (network != null) net = network;
        this.intervall_s = intervall_s;
        this.fileName = fileName;
    }

    public void close() throws IOException {
        try {
            outFile.close();
            outStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    ByteBuffer buf = ByteBuffer.allocate(BUFFERSIZE);

    public void dumpConstData() throws IOException {
        buf.position(0);
        outFile.writeDouble(-1.);
        quad.writeConstData(buf);
        outFile.writeInt(buf.position());
        outFile.write(buf.array(), 0, buf.position());
    }

    public boolean dump(int time_s) throws IOException {
        if (time_s >= nextTime) {
            buf.position(0);
            outFile.writeDouble(time_s);
            quad.writeDynData(null, buf);
            outFile.writeInt(buf.position());
            outFile.write(buf.array(), 0, buf.position());
            nextTime = time_s + intervall_s;
            return true;
        }
        return false;
    }

    public void open() {
        try {
            if (fileName.endsWith(".gz")) {
                outStream = new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(fileName), BUFFERSIZE), BUFFERSIZE);
            } else {
                outStream = new BufferedOutputStream(new FileOutputStream(fileName), BUFFERSIZE);
            }
            outFile = new DataOutputStream(outStream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            outFile.writeDouble(intervall_s);
            Gbl.startMeasurement();
            quad = new OTFServerQuad(net);
            System.out.print("build Quad on Server: ");
            Gbl.printElapsedTime();
            Gbl.startMeasurement();
            quad.fillQuadTree(new OTFDefaultNetWriterFactoryImpl());
            System.out.print("fill writer Quad on Server: ");
            Gbl.printElapsedTime();
            Gbl.startMeasurement();
            new ObjectOutputStream(outStream).writeObject(quad);
            dumpConstData();
            System.out.print("write to file  Quad on Server: ");
            Gbl.printElapsedTime();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private InputStream inStream = null;

    private DataInputStream inFile;

    public void readQuad() {
        try {
            if (fileName.endsWith(".gz")) {
                InputStream gzInStream = new BufferedInputStream(new GZIPInputStream(new FileInputStream(fileName), BUFFERSIZE), BUFFERSIZE);
                inStream = gzInStream;
            } else {
                inStream = new BufferedInputStream(new FileInputStream(fileName), BUFFERSIZE);
            }
            inFile = new DataInputStream(inStream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            intervall_s = inFile.readDouble();
            this.quad = (OTFServerQuad) new ObjectInputStream(inStream).readObject();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void hasNextTimeStep() {
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

    long filepos = 0;

    byte[] result = result = new byte[BUFFERSIZE];

    public byte[] getStateBuffer() throws RemoteException {
        int size = 0;
        Gbl.startMeasurement();
        try {
            nextTime = inFile.readDouble();
            size = inFile.readInt();
            int offset = 0;
            int remain = size;
            int read = 0;
            while ((remain > 0) && (read != -1)) {
                read = inFile.read(result, offset, remain);
                remain -= read;
                offset += read;
                System.out.print(" " + read);
            }
            if (offset != size) throw new IOException("READ SIZE did not fit! File corrupted!");
            timeSteps.put(nextTime, filepos);
            filepos += read;
        } catch (IOException e) {
            System.out.println(e.toString());
        }
        System.out.print("getStateBuffer: ");
        Gbl.printElapsedTime();
        return result;
    }

    public void pause() throws RemoteException {
    }

    public void play() throws RemoteException {
    }

    public void setStatus(int status) throws RemoteException {
    }

    public void step() throws RemoteException {
        actBuffer = getStateBuffer();
    }

    public boolean isLive() {
        return false;
    }

    private final List<Double> timeStepIndex = new ArrayList<Double>();

    private boolean readNextTimeStep() {
        double time = 0;
        try {
            time = inFile.readDouble();
            int size = inFile.readInt();
            timeStepIndex.add(time);
            inFile.skip(size);
            timeSteps.put(time, filepos);
            filepos += size;
        } catch (IOException e) {
            System.out.println(e.toString());
            return false;
        }
        return true;
    }

    private void buildIndex() {
        inFile.mark(-1);
        while (readNextTimeStep()) ;
        try {
            inFile.reset();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public OTFServerQuad getQuad(String id, OTFNetWriterFactory writers) throws RemoteException {
        if (writers != null) throw new RemoteException("writers need to be NULL, when reading from file");
        if (this.id == null) readQuad();
        if (id != null && !id.equals(this.id)) throw new RemoteException("id does not match, set id to NULL will match ALL!");
        return quad;
    }

    public byte[] getQuadConstStateBuffer(String id) throws RemoteException {
        byte[] buffer = getStateBuffer();
        if (nextTime != -1) throw new RemoteException("CONST data needs to be read FIRST");
        return buffer;
    }

    public byte[] getQuadDynStateBuffer(String id, Rect bounds) throws RemoteException {
        if (actBuffer == null) step();
        return actBuffer;
    }

    public boolean requestNewTime(int time, TimePreference searchDirection) throws RemoteException {
        return false;
    }
}
