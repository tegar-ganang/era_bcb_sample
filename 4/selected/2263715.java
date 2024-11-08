package org.matsim.utils.vis.otfivs.data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.matsim.mobsim.QueueLink;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.mobsim.QueueNode;
import org.matsim.utils.collections.QuadTree;
import org.matsim.utils.vis.otfivs.interfaces.OTFDataReader;
import org.matsim.utils.vis.otfivs.interfaces.OTFServerRemote;

public class OTFServerQuad extends QuadTree<OTFDataWriter> {

    private final List<OTFDataWriter> additionalElements = new LinkedList<OTFDataWriter>();

    class ConvertToClientExecutor extends Executor<OTFDataWriter> {

        final OTFConnectionManager connect;

        final OTFClientQuad client;

        public ConvertToClientExecutor(OTFConnectionManager connect2, OTFClientQuad client) {
            this.connect = connect2;
            this.client = client;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void execute(double x, double y, OTFDataWriter writer) {
            Collection<Class> readerClasses = this.connect.getEntries(writer.getClass());
            for (Class readerClass : readerClasses) {
                try {
                    Object reader = readerClass.newInstance();
                    this.client.put(x, y, (OTFDataReader) reader);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException();
                }
            }
        }
    }

    class WriteDataExecutor extends Executor<OTFDataWriter> {

        final ByteBuffer out;

        boolean writeConst;

        public WriteDataExecutor(ByteBuffer out, boolean writeConst) {
            this.out = out;
            this.writeConst = writeConst;
        }

        @Override
        public void execute(double x, double y, OTFDataWriter writer) {
            try {
                if (this.writeConst) writer.writeConstData(this.out); else writer.writeDynData(this.out);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
	 *
	 */
    private static final long serialVersionUID = 1L;

    private double minEasting;

    private double maxEasting;

    private double minNorthing;

    private double maxNorthing;

    private transient QueueNetworkLayer net;

    public static double offsetEast;

    public static double offsetNorth;

    public OTFServerQuad(QueueNetworkLayer net) {
        super(0, 0, 0, 0);
        updateBoundingBox(net);
    }

    public OTFServerQuad(double minX, double minY, double maxX, double maxY) {
        super(minX, minY, maxX, maxY);
        this.minEasting = minX;
        this.maxEasting = maxX + 1;
        this.minNorthing = minY;
        this.maxNorthing = maxY + 1;
    }

    public void updateBoundingBox(QueueNetworkLayer net) {
        this.minEasting = Double.POSITIVE_INFINITY;
        this.maxEasting = Double.NEGATIVE_INFINITY;
        this.minNorthing = Double.POSITIVE_INFINITY;
        this.maxNorthing = Double.NEGATIVE_INFINITY;
        for (Iterator<? extends QueueNode> it = net.getNodes().values().iterator(); it.hasNext(); ) {
            QueueNode node = it.next();
            this.minEasting = Math.min(this.minEasting, node.getNode().getCoord().getX());
            this.maxEasting = Math.max(this.maxEasting, node.getNode().getCoord().getX());
            this.minNorthing = Math.min(this.minNorthing, node.getNode().getCoord().getY());
            this.maxNorthing = Math.max(this.maxNorthing, node.getNode().getCoord().getY());
        }
        this.maxEasting += 1;
        this.maxNorthing += 1;
        this.net = net;
        offsetEast = this.minEasting;
        offsetNorth = this.minNorthing;
    }

    public void fillQuadTree(OTFNetWriterFactory writers) {
        final double easting = this.maxEasting - this.minEasting;
        final double northing = this.maxNorthing - this.minNorthing;
        setTopNode(0, 0, easting, northing);
        for (QueueNode node : this.net.getNodes().values()) {
            OTFDataWriter<QueueNode> writer = writers.getNodeWriter();
            if (writer != null) writer.setSrc(node);
            put(node.getNode().getCoord().getX() - this.minEasting, node.getNode().getCoord().getY() - this.minNorthing, writer);
        }
        for (QueueLink link : this.net.getLinks().values()) {
            double middleEast = (link.getLink().getToNode().getCoord().getX() + link.getLink().getFromNode().getCoord().getX()) * 0.5 - this.minEasting;
            double middleNorth = (link.getLink().getToNode().getCoord().getY() + link.getLink().getFromNode().getCoord().getY()) * 0.5 - this.minNorthing;
            OTFDataWriter<QueueLink> writer = writers.getLinkWriter();
            if (writer != null) writer.setSrc(link);
            put(middleEast, middleNorth, writer);
        }
    }

    public void addAdditionalElement(OTFDataWriter element) {
        this.additionalElements.add(element);
    }

    public OTFClientQuad convertToClient(String id, final OTFServerRemote host, final OTFConnectionManager connect) {
        final OTFClientQuad client = new OTFClientQuad(id, host, 0., 0., this.maxEasting - this.minEasting, this.maxNorthing - this.minNorthing);
        client.offsetEast = this.minEasting;
        client.offsetNorth = this.minNorthing;
        this.execute(0., 0., this.maxEasting - this.minEasting, this.maxNorthing - this.minNorthing, this.new ConvertToClientExecutor(connect, client));
        for (OTFDataWriter element : this.additionalElements) {
            Collection<Class> readerClasses = connect.getEntries(element.getClass());
            for (Class readerClass : readerClasses) {
                try {
                    Object reader = readerClass.newInstance();
                    client.addAdditionalElement((OTFDataReader) reader);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException();
                }
            }
        }
        return client;
    }

    public void writeConstData(ByteBuffer out) {
        this.execute(0., 0., this.maxEasting - this.minEasting, this.maxNorthing - this.minNorthing, this.new WriteDataExecutor(out, true));
        for (OTFDataWriter element : this.additionalElements) {
            try {
                element.writeConstData(out);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void writeDynData(QuadTree.Rect bounds, ByteBuffer out) {
        this.execute(bounds, this.new WriteDataExecutor(out, false));
        for (OTFDataWriter element : this.additionalElements) {
            try {
                element.writeDynData(out);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public double getMaxEasting() {
        return this.maxEasting;
    }

    @Override
    public double getMaxNorthing() {
        return this.maxNorthing;
    }

    @Override
    public double getMinEasting() {
        return this.minEasting;
    }

    @Override
    public double getMinNorthing() {
        return this.minNorthing;
    }
}
