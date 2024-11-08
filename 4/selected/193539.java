package org.matsim.vis.otfvis.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.TreeMap;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.queuesim.QueueLink;
import org.matsim.core.mobsim.queuesim.QueueNetwork;
import org.matsim.core.mobsim.queuesim.QueueNode;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.collections.QuadTree.Rect;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.data.OTFDefaultNetWriterFactoryImpl;
import org.matsim.vis.otfvis.data.OTFNetWriterFactory;
import org.matsim.vis.otfvis.data.OTFServerQuad;
import org.matsim.vis.otfvis.handler.OTFAgentsListHandler;
import org.matsim.vis.otfvis.handler.OTFDefaultNodeHandler;
import org.matsim.vis.otfvis.handler.OTFLinkLanesAgentsNoParkingHandler;
import org.matsim.vis.otfvis.interfaces.OTFServerRemote;
import org.matsim.vis.snapshots.writers.PositionInfo;
import org.matsim.world.World;

public class OTFTVehServer implements OTFServerRemote {

    private String vehFileName = "";

    private static final int BUFFERSIZE = 100000000;

    private BufferedReader reader = null;

    private double nextTime = -1;

    private TreeMap<Integer, byte[]> timesteps = new TreeMap<Integer, byte[]>();

    private final OTFAgentsListHandler.Writer writer = new OTFAgentsListHandler.Writer();

    private OTFServerQuad quad;

    private ByteBuffer buf = ByteBuffer.allocate(BUFFERSIZE);

    private OTFAgentsListHandler.ExtendedPositionInfo readVehicle = null;

    private double time;

    public OTFTVehServer(String netFileName, String vehFileName) {
        this.vehFileName = vehFileName;
        if (Gbl.getConfig() == null) Gbl.createConfig(null);
        Gbl.startMeasurement();
        World world = Gbl.createWorld();
        NetworkLayer net = new NetworkLayer();
        new MatsimNetworkReader(net).readFile(netFileName);
        world.setNetworkLayer(net);
        world.complete();
        QueueNetwork qnet = new QueueNetwork(net);
        OTFConnectionManager connect = new OTFConnectionManager();
        connect.add(QueueLink.class, OTFLinkLanesAgentsNoParkingHandler.Writer.class);
        connect.add(QueueNode.class, OTFDefaultNodeHandler.Writer.class);
        this.quad = new OTFServerQuad(qnet);
        this.quad.fillQuadTree(connect);
        this.quad.addAdditionalElement(this.writer);
        open();
        readOneStep();
    }

    public boolean readOneLine() {
        String line = null;
        boolean lineFound = false;
        try {
            line = this.reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        while (!lineFound && (line != null)) {
            String[] result = line.split("\t");
            if (result.length == 16) {
                double easting = Double.parseDouble(result[11]);
                double northing = Double.parseDouble(result[12]);
                if ((easting >= this.quad.getMinEasting()) && (easting <= this.quad.getMaxEasting()) && (northing >= this.quad.getMinNorthing()) && (northing <= this.quad.getMaxNorthing())) {
                    String agent = result[0];
                    String time = result[1];
                    String speed = result[6];
                    String elevation = result[13];
                    String azimuth = result[14];
                    lineFound = true;
                    this.time = Double.parseDouble(time);
                    this.readVehicle = new OTFAgentsListHandler.ExtendedPositionInfo(new IdImpl(agent), easting, northing, Double.parseDouble(elevation), Double.parseDouble(azimuth), Double.parseDouble(speed), PositionInfo.VehicleState.Driving, Integer.parseInt(result[7]), Integer.parseInt(result[15]));
                    return true;
                }
            }
            try {
                line = this.reader.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private boolean finishedReading = false;

    private int newTime;

    private synchronized void preCacheTime() {
        while ((this.time <= this.newTime) && !this.finishedReading) readOneStep();
    }

    private synchronized void readOneStep() {
        if (this.finishedReading) return;
        double actTime = this.time;
        if (this.readVehicle == null) {
            readOneLine();
            this.writer.positions.add(this.readVehicle);
            actTime = this.time;
        } else {
            this.writer.positions.clear();
            this.writer.positions.add(this.readVehicle);
        }
        while (readOneLine() && (this.time == actTime)) this.writer.positions.add(this.readVehicle);
        if (this.time == actTime) this.finishedReading = true;
        synchronized (this.buf) {
            this.buf.position(0);
            this.quad.writeDynData(null, this.buf);
            byte[] buffer = new byte[this.buf.position() + 1];
            System.arraycopy(this.buf.array(), 0, buffer, 0, buffer.length);
            this.nextTime = actTime;
            this.timesteps.put((int) this.nextTime, buffer);
        }
    }

    private void open() {
        Gbl.startMeasurement();
        try {
            this.reader = IOUtils.getBufferedReader(this.vehFileName);
            this.reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
            this.finishedReading = true;
            return;
        }
    }

    public int getLocalTime() throws RemoteException {
        return (int) this.nextTime;
    }

    public OTFServerQuad getQuad(String id, OTFConnectionManager connect) throws RemoteException {
        return this.quad;
    }

    public byte[] getQuadConstStateBuffer(String id) throws RemoteException {
        this.buf.position(0);
        this.quad.writeConstData(this.buf);
        byte[] result;
        synchronized (this.buf) {
            result = this.buf.array();
        }
        return result;
    }

    public byte[] getQuadDynStateBuffer(String id, Rect bounds) throws RemoteException {
        if (this.nextTime == -1) {
            throw new RemoteException("nextTime == -1 in OTFTVehServer");
        }
        return this.timesteps.get((int) this.nextTime);
    }

    public byte[] getStateBuffer() throws RemoteException {
        throw new RemoteException("getStateBuffer not implemented for OTFTVehServer");
    }

    public boolean isLive() throws RemoteException {
        return false;
    }

    public boolean requestNewTime(int time, TimePreference searchDirection) throws RemoteException {
        int lastTime = -1;
        int foundTime = -1;
        this.newTime = time;
        if ((this.timesteps.lastKey() < time) && (searchDirection == TimePreference.LATER)) {
            if (this.finishedReading) return false; else this.newTime = (int) this.time;
        }
        preCacheTime();
        for (Integer timestep : this.timesteps.keySet()) {
            if (searchDirection == TimePreference.EARLIER) {
                if (timestep >= this.newTime) {
                    foundTime = lastTime;
                    break;
                }
            } else {
                if (timestep >= this.newTime) {
                    foundTime = timestep;
                    break;
                }
            }
            lastTime = timestep;
        }
        if (foundTime == -1) return false;
        this.nextTime = foundTime;
        return true;
    }

    public Collection<Double> getTimeSteps() throws RemoteException {
        return null;
    }
}
