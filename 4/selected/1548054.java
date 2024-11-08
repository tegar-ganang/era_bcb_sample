package org.gvsig.graph.core.loaders;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import org.gvsig.graph.core.EdgePair;
import org.gvsig.graph.core.GvEdge;
import org.gvsig.graph.core.GvGraph;
import org.gvsig.graph.core.GvNode;
import org.gvsig.graph.core.IGraph;
import org.gvsig.graph.core.INetworkLoader;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.decorators.Indexer;
import edu.uci.ics.jung.graph.impl.DirectedSparseEdge;
import edu.uci.ics.jung.graph.impl.DirectedSparseVertex;
import edu.uci.ics.jung.graph.impl.SparseGraph;

/**
 * @author fjp
 * 
 * Primero vienen los arcos, y luego los nodos. En la cabecera, 3 enteros
 * con el numero de tramos, el de arcos y el de nodos.
 *
 */
public class NetworkJungLoader implements INetworkLoader {

    private File netFile = new File("c:/ejes.red");

    public Graph loadJungNetwork() {
        SparseGraph g = new SparseGraph();
        long t1 = System.currentTimeMillis();
        RandomAccessFile file;
        try {
            file = new RandomAccessFile(netFile.getPath(), "r");
            FileChannel channel = file.getChannel();
            MappedByteBuffer buf = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
            buf.order(ByteOrder.LITTLE_ENDIAN);
            int numArcs = buf.getInt();
            int numEdges = buf.getInt();
            int numNodes = buf.getInt();
            buf.position(24 * numEdges + 12);
            for (int i = 0; i < numNodes; i++) {
                GvNode node = readNode(buf);
                Vertex v = new DirectedSparseVertex();
                g.addVertex(v);
            }
            Indexer indexer = Indexer.getIndexer(g);
            buf.position(12);
            for (int i = 0; i < numEdges; i++) {
                GvEdge edge = readEdge(buf);
                int nodeOrig = edge.getIdNodeOrig();
                int nodeEnd = edge.getIdNodeEnd();
                Vertex vFrom = (Vertex) indexer.getVertex(nodeOrig);
                Vertex vTo = (Vertex) indexer.getVertex(nodeEnd);
                DirectedSparseEdge edgeJ = new DirectedSparseEdge(vFrom, vTo);
                g.addEdge(edgeJ);
            }
            long t2 = System.currentTimeMillis();
            System.out.println("Tiempo de carga: " + (t2 - t1) + " msecs");
            return g;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private GvNode readNode(MappedByteBuffer buf) {
        GvNode node = new GvNode();
        node.setIdNode(buf.getInt());
        node.setX(buf.getDouble());
        node.setY(buf.getDouble());
        return node;
    }

    private GvEdge readEdge(MappedByteBuffer buf) {
        GvEdge edge = new GvEdge();
        edge.setIdArc(buf.getInt());
        edge.setDirec(buf.getInt());
        edge.setIdNodeOrig(buf.getInt());
        edge.setIdNodeEnd(buf.getInt());
        edge.setType(buf.getInt());
        edge.setDistance(buf.getDouble());
        edge.setWeight(buf.getDouble());
        return edge;
    }

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        NetworkJungLoader redLoader = new NetworkJungLoader();
        redLoader.loadNetwork();
        redLoader.loadJungNetwork();
    }

    public File getNetFile() {
        return netFile;
    }

    public void setNetFile(File netFile) {
        this.netFile = netFile;
    }

    public IGraph loadNetwork() {
        return null;
    }
}
