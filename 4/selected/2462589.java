package hu.csq.dyneta.networkstatistics;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.graph.util.Pair;
import hu.csq.dyneta.lib.plugins.NetworkStatistics;
import hu.csq.dyneta.ParameterizablePlugin;
import hu.csq.dyneta.parameterhelper.ParameterHelper;
import hu.csq.dyneta.parameterhelper.constraints.PHCBoolean;
import hu.csq.dyneta.parameterhelper.constraints.PHCIntegerMinimum;
import hu.csq.dyneta.parameterhelper.constraints.PHCNoConstraint;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.TreeSet;
import org.apache.log4j.Logger;

class PajekNetworkSaver<V extends Comparable<V>, E> {

    /**
     * Saves graph g to a file called filename in pajek format.
     * Runs in O(n*logn + e) time where n is the number
     * of vertices, e is the number of edges.
     *
     * @param filename
     * @param g
     */
    public void savePajekNetwork(Graph<V, E> g, String filename) throws IOException {
        PrintWriter pw;
        pw = new PrintWriter(new File(filename));
        TreeSet<V> orderer = new TreeSet<V>(g.getVertices());
        pw.print("*Vertices ");
        pw.println(orderer.size());
        HashMap<V, Integer> vertexcodes = new HashMap<V, Integer>();
        int i = 1;
        for (V v : orderer) {
            vertexcodes.put(v, new Integer(i));
            pw.println(i);
            i++;
        }
        Collection<E> undiredges = g.getEdges(EdgeType.UNDIRECTED);
        if (undiredges.size() > 0) {
            pw.println("*Edges");
            for (E e : undiredges) {
                Pair<V> p = g.getEndpoints(e);
                pw.print(vertexcodes.get(p.getFirst()));
                pw.print(' ');
                pw.print(vertexcodes.get(p.getSecond()));
                pw.println(" 1.0");
            }
        }
        undiredges = null;
        Collection<E> diredges = g.getEdges(EdgeType.DIRECTED);
        if (diredges.size() > 0) {
            pw.println("*Arcs");
            for (E e : diredges) {
                V src = g.getSource(e);
                V dst = g.getDest(e);
                pw.print(vertexcodes.get(src));
                pw.print(' ');
                pw.print(vertexcodes.get(dst));
                pw.println(" 1.0");
            }
        }
        pw.close();
    }
}

/**
 * Saves the network after each step.
 *
 * @author Tomika
 */
public class NetworkWriterv2<V extends Comparable<V>, E> implements NetworkStatistics<V, E>, ParameterizablePlugin {

    ParameterHelper ph = new ParameterHelper();

    PajekNetworkSaver<V, E> pns = new PajekNetworkSaver<V, E>();

    public NetworkWriterv2() {
        ph.add("filenameprefix", "", new PHCNoConstraint(), "Prefix of file names. Sequence number and file extension will be added automatically.");
        ph.add("seqnumberdigits", "1", new PHCIntegerMinimum(1), "Sequence number will be AT LEAST seqnumberdigits digits long. If needed zeros will be appeded to the beginning of the number.");
        ph.add("seqnumbermax", "-1", new PHCIntegerMinimum(-1), "Maximum number of files to save. Use -1 to save everything.");
        ph.add("safemode", "true", new PHCBoolean(), "Stops experiment if first file exitst.");
    }

    private String generateFileName(String fnprefix, int fnindex, String fnext) {
        StringBuilder sb = new StringBuilder(fnprefix);
        String sfnindex = Integer.toString(fnindex);
        int nrequiredzeros = seqnumberdigits - sfnindex.length();
        while (nrequiredzeros > 0) {
            sb.append('0');
            nrequiredzeros--;
        }
        sb.append(sfnindex);
        sb.append(fnext);
        return sb.toString();
    }

    String format;

    String filenameprefix;

    String filenameext = ".net";

    int seqnumbermax;

    int seqnumberdigits;

    public void init() {
        filenameprefix = ph.get("filenameprefix");
        seqnumberdigits = ph.getInteger("seqnumberdigits");
        if (filenameprefix.length() == 0) {
            throw new RuntimeException("Please give a file name prefix.");
        }
        boolean safemode = Boolean.parseBoolean(ph.get("safemode"));
        if (safemode) {
            String currentFileName = generateFileName(filenameprefix, filenamesuffix + 1, filenameext);
            File file = new File(currentFileName);
            if (file.exists()) {
                throw new RuntimeException("Network writer plugin - First file already exists, and safe mode was on.");
            }
        }
        seqnumbermax = ph.getInteger("seqnumbermax");
    }

    public int getNumberOfStats() {
        return 0;
    }

    int filenamesuffix = -1;

    public void getStats(Graph<V, E> g, double[] statsArray, int arrayLowerBound) {
        if (seqnumbermax == -1 || filenamesuffix < seqnumbermax) {
            filenamesuffix++;
            String currentFileName = generateFileName(filenameprefix, filenamesuffix, filenameext);
            try {
                pns.savePajekNetwork(g, currentFileName);
            } catch (IOException ex) {
                Logger.getLogger(NetworkWriterv2.class.getName()).error("Error when saving network to " + currentFileName, ex);
            }
        }
    }

    public String[] getStatNames() {
        return null;
    }

    public ParameterHelper getParameterHelper() {
        return ph;
    }
}
