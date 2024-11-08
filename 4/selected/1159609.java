package org.matsim.vis.otfvis.data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.vis.otfvis.interfaces.OTFDataReader;
import org.matsim.vis.otfvis.interfaces.OTFServerRemote;

/**
 * OTFServerQuad is the quad representation of all elements of the network on the server
 * side. This QuadTree is mirrored on the client side by OTFClientQuad.
 *
 * @author dstrippgen
 *
 */
public abstract class OTFServerQuad2 extends QuadTree<OTFDataWriter> implements OTFServerQuadI {

    private static final Logger log = Logger.getLogger(OTFServerQuad2.class);

    private final List<OTFDataWriter> additionalElements = new LinkedList<OTFDataWriter>();

    private static final long serialVersionUID = 1L;

    protected double minEasting;

    protected double maxEasting;

    protected double minNorthing;

    protected double maxNorthing;

    protected double easting;

    protected double northing;

    public static double offsetEast;

    public static double offsetNorth;

    public OTFServerQuad2(Network network) {
        super(0, 0, 0, 0);
        this.setBoundingBoxFromNetwork(network);
    }

    /**
	 * This method should be abstract as it has to be overwritten in subclasses.
	 * Due to deserialization backwards compatibility this is not possible. dg dez 09
	 */
    public abstract void initQuadTree(final OTFConnectionManager connect);

    protected void setBoundingBoxFromNetwork(Network n) {
        this.minEasting = Double.POSITIVE_INFINITY;
        this.maxEasting = Double.NEGATIVE_INFINITY;
        this.minNorthing = Double.POSITIVE_INFINITY;
        this.maxNorthing = Double.NEGATIVE_INFINITY;
        for (org.matsim.api.core.v01.network.Node node : n.getNodes().values()) {
            this.minEasting = Math.min(this.minEasting, node.getCoord().getX());
            this.maxEasting = Math.max(this.maxEasting, node.getCoord().getX());
            this.minNorthing = Math.min(this.minNorthing, node.getCoord().getY());
            this.maxNorthing = Math.max(this.maxNorthing, node.getCoord().getY());
        }
        this.maxEasting += 1;
        this.maxNorthing += 1;
        offsetEast = this.minEasting;
        offsetNorth = this.minNorthing;
        this.easting = this.maxEasting - this.minEasting;
        this.northing = this.maxNorthing - this.minNorthing;
        setTopNode(0, 0, easting, northing);
    }

    /**
	 * Sets a new top node in case the extremities from the c'tor are not
	 * good anymore, it also clear the QuadTree
	 * @param minX The smallest x coordinate expected
	 * @param minY The smallest y coordinate expected
	 * @param maxX The largest x coordinate expected
	 * @param maxY The largest y coordinate expected
	 */
    protected void setTopNode(final double minX, final double minY, final double maxX, final double maxY) {
        this.top = new Node<OTFDataWriter>(minX, minY, maxX, maxY);
    }

    public void addAdditionalElement(OTFDataWriter element) {
        this.additionalElements.add(element);
    }

    public OTFClientQuad convertToClient(String id, final OTFServerRemote host, final OTFConnectionManager connect) {
        final OTFClientQuad client = new OTFClientQuad(id, host, 0., 0., this.easting, this.northing);
        client.offsetEast = this.minEasting;
        client.offsetNorth = this.minNorthing;
        this.execute(0., 0., this.easting, this.northing, new ConvertToClientExecutor(connect, client));
        for (OTFDataWriter<?> element : this.additionalElements) {
            Collection<Class<OTFDataReader>> readerClasses = connect.getReadersForWriter(element.getClass());
            for (Class<? extends OTFDataReader> readerClass : readerClasses) {
                try {
                    Object reader = readerClass.newInstance();
                    client.addAdditionalElement((OTFDataReader) reader);
                    log.info("Connected additional element writer " + element.getClass().getName() + "(" + element + ")  to " + reader.getClass().getName() + " (" + reader + ")");
                } catch (InstantiationException e) {
                    throw new RuntimeException(e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return client;
    }

    public void writeConstData(ByteBuffer out) {
        for (OTFDataWriter element : this.values()) {
            try {
                element.writeConstData(out);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        for (OTFDataWriter element : this.additionalElements) {
            try {
                element.writeConstData(out);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void writeDynData(QuadTree.Rect bounds, ByteBuffer out) {
        this.execute(bounds, new WriteDataExecutor(out, false));
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

    private static class ConvertToClientExecutor implements Executor<OTFDataWriter> {

        final OTFConnectionManager connect;

        final OTFClientQuad client;

        public ConvertToClientExecutor(OTFConnectionManager connect2, OTFClientQuad client) {
            this.connect = connect2;
            this.client = client;
        }

        @SuppressWarnings("unchecked")
        public void execute(double x, double y, OTFDataWriter writer) {
            Collection<Class<?>> readerClasses = this.connect.getToEntries(writer.getClass());
            for (Class readerClass : readerClasses) {
                try {
                    OTFDataReader reader = (OTFDataReader) readerClass.newInstance();
                    reader.setSrc(writer);
                    this.client.put(x, y, reader);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException();
                }
            }
        }
    }

    private static class WriteDataExecutor implements Executor<OTFDataWriter> {

        final ByteBuffer out;

        boolean writeConst;

        public WriteDataExecutor(ByteBuffer out, boolean writeConst) {
            this.out = out;
            this.writeConst = writeConst;
        }

        public void execute(double x, double y, OTFDataWriter writer) {
            try {
                if (this.writeConst) writer.writeConstData(this.out); else writer.writeDynData(this.out);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
