package topology.graphParsers;

import javolution.util.FastMap;
import javolution.util.Index;
import java.io.*;
import java.util.regex.Matcher;
import server.common.DummyProgress;
import server.common.LoggingManager;
import server.execution.AbstractExecution;
import topology.EdgeInfo;
import topology.GraphInterface;
import topology.GraphRegularExpressions;
import topology.GraphAsHashMap;
import topology.VertexInfo;
import topology.WeightsLoader;
import topology.graphParsers.common.GraphAuxStructure;
import common.Pair;

/**
 * This class is responsible for loading graphs from *.net files.
 * It also loads communication weights from files, and parses Strings which contain the communication weights.
 * 
 * Created by IntelliJ IDEA.
 * User: Polina Zilberman
 * Date: Aug 23, 2007
 * Time: 1:38:38 PM
 * To change this template use File | Settings | File Templates.
 */
public class NetFileParser implements NetworkGraphParser {

    private static final String EXTENSION = "net";

    public static void main(String[] args) {
        GraphInterface<Index> graph = null;
        NetFileParser gl = new NetFileParser();
        graph = gl.parseGraph("res/smallnet.net", new DummyProgress(), 1);
        double[][] comm = WeightsLoader.loadWeightsFromString("0 1 2 3 4 5 " + "0 1 2 3 4 5 " + "0 1 2 3 4 5 " + "0 1 2 3 4 5 " + "0 1 2 3 4 5 " + "0 1 2 3 4 5", 6);
        System.out.println(graph);
        System.out.println(comm);
    }

    public GraphInterface<Index> parseGraph(BufferedReader in, AbstractExecution progress, double precentage) {
        GraphInterface<Index> graph = null;
        graph = analyzeFile(in, progress, precentage);
        try {
            if (in != null) in.close();
        } catch (IOException ex) {
            LoggingManager.getInstance().writeSystem("Couldn't close InputStream: " + in + "\n" + ex.getMessage() + "\n" + ex.getStackTrace(), "GraphLoader", "loadFile", ex);
        }
        return graph;
    }

    public GraphInterface<Index> parseGraph(String filename, AbstractExecution progress, double precentage) {
        File file = new File(filename);
        FileInputStream fin = null;
        BufferedReader reader = null;
        GraphInterface<Index> graph = null;
        try {
            fin = new FileInputStream(file);
            reader = new BufferedReader(new InputStreamReader(fin));
            graph = analyzeFile(reader, progress, precentage);
        } catch (FileNotFoundException ex) {
            LoggingManager.getInstance().writeSystem("Couldn't find the network file: " + filename + "\n" + ex.getMessage() + "\n" + ex.getStackTrace(), "GraphLoader", "loadFile", ex);
        } finally {
            try {
                if (fin != null) fin.close();
            } catch (IOException ex) {
                LoggingManager.getInstance().writeSystem("Couldn't close FileInputStream to: " + file.getAbsoluteFile() + "\n" + ex.getMessage() + "\n" + ex.getStackTrace(), "GraphLoader", "loadFile", ex);
            }
        }
        return graph;
    }

    private int readFirstLine(BufferedReader reader) {
        try {
            String firstLine = reader.readLine();
            Matcher verticesNumber = GraphRegularExpressions.VERTICES_BEGINING.matcher(firstLine);
            if (verticesNumber.find()) return Integer.parseInt(verticesNumber.group(1)); else {
                LoggingManager.getInstance().writeSystem("Couldn't read fisrt line of the network file.\nFirst line: " + firstLine, "GraphLoader", "readFirstLine", null);
                return -1;
            }
        } catch (IOException ex) {
            LoggingManager.getInstance().writeSystem(ex.getMessage() + "\n" + ex.getStackTrace(), "GraphLoader", "readFirstLine", ex);
            return -1;
        }
    }

    private int readVertices(int numOfVertices, BufferedReader reader, GraphInterface<Index> graph) {
        FastMap<String, Pair<String, String>> m_lastInfo = new FastMap<String, Pair<String, String>>();
        String line = null;
        try {
            for (int i = 0; i < numOfVertices && ((line = reader.readLine()) != null); i++) {
                Matcher verticesLine = GraphRegularExpressions.VERTICES_LINE.matcher(line);
                if (verticesLine.find()) {
                    int vertexNum = Integer.parseInt(verticesLine.group(1));
                    String label = verticesLine.group(2);
                    double x = Double.parseDouble(verticesLine.group(3));
                    double y = Double.parseDouble(verticesLine.group(4));
                    double z = Double.parseDouble(verticesLine.group(5));
                    FastMap<String, Pair<String, String>> info = new FastMap<String, Pair<String, String>>();
                    int infoIndex = line.indexOf(verticesLine.group(5)) + verticesLine.group(5).length() + 1;
                    if (line.length() > infoIndex) {
                        String infoStr = line.substring(infoIndex);
                        Matcher vertexInfo = GraphRegularExpressions.OPTIONAL_INFO.matcher(infoStr);
                        while (vertexInfo.find()) {
                            String labelName = vertexInfo.group(1);
                            String labelValue = vertexInfo.group(2);
                            if (labelName != null) info.put(labelName.trim().toLowerCase(), new Pair<String, String>(labelName.trim(), labelValue));
                        }
                    }
                    m_lastInfo.putAll(info);
                    info.putAll(m_lastInfo);
                    VertexInfo vInfo = new VertexInfo(vertexNum, label, x, y, z, info);
                    graph.addVertex(Index.valueOf(vertexNum - 1), vInfo);
                } else {
                    verticesLine = GraphRegularExpressions.VERTICES_LINE_VERSION_2.matcher(line);
                    if (verticesLine.find()) {
                        int vertexNum = Integer.parseInt(verticesLine.group(1));
                        VertexInfo vInfo = new VertexInfo(vertexNum, "", 0, 0, 0, new FastMap<String, Pair<String, String>>());
                        graph.addVertex(Index.valueOf(vertexNum - 1), vInfo);
                    }
                }
            }
            return 1;
        } catch (IOException ex) {
            LoggingManager.getInstance().writeSystem(ex.getMessage() + "\n" + ex.getStackTrace(), "GraphLoader", "readVertices", ex);
            return -1;
        }
    }

    private int readEdges(BufferedReader reader, GraphInterface<Index> graph) {
        FastMap<String, Pair<String, String>> m_lastInfo = new FastMap<String, Pair<String, String>>();
        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                Matcher edgesLine = GraphRegularExpressions.EDGE_LINE.matcher(line);
                if (edgesLine.find()) {
                    FastMap<String, Pair<String, String>> info = new FastMap<String, Pair<String, String>>();
                    int infoIndex = line.lastIndexOf(edgesLine.group(3)) + edgesLine.group(3).length() + 1;
                    if (line.length() > infoIndex) {
                        String infoStr = line.substring(infoIndex);
                        Matcher vertexInfo = GraphRegularExpressions.OPTIONAL_INFO.matcher(infoStr);
                        while (vertexInfo.find()) {
                            String labelName = vertexInfo.group(1);
                            String labelValue = vertexInfo.group(2);
                            if (labelName != null) info.put(labelName.trim().toLowerCase(), new Pair<String, String>(labelName.trim(), labelValue));
                        }
                    }
                    m_lastInfo.putAll(info);
                    info.putAll(m_lastInfo);
                    graph.addEdge(Index.valueOf(Integer.parseInt(edgesLine.group(1)) - 1), Index.valueOf(Integer.parseInt(edgesLine.group(2)) - 1), new EdgeInfo(Double.parseDouble(edgesLine.group(3)), info));
                }
            }
            return 1;
        } catch (IOException ex) {
            LoggingManager.getInstance().writeSystem(ex.getMessage() + "\n" + ex.getStackTrace(), "GraphLoader", "readEdges", ex);
            return -1;
        }
    }

    private void cleanClose(BufferedReader reader, String msg) {
        System.err.println(msg);
        try {
            if (reader != null) reader.close();
        } catch (IOException ex) {
            LoggingManager.getInstance().writeSystem("An exception has occured while closing BufferedReader.\n" + ex.getMessage() + "\n" + ex.getStackTrace(), "GraphLoader", "cleanClose", ex);
        }
    }

    public void updateLoadProgress(AbstractExecution progress, double percentage) {
        double p = progress.getProgress();
        p += 0.5 * percentage;
        progress.setProgress(p);
    }

    public GraphInterface<Index> analyzeFile(BufferedReader reader, AbstractExecution progress, double precentage) {
        GraphInterface<Index> graph = null;
        int numOfVertices = readFirstLine(reader);
        if (numOfVertices != -1) graph = new GraphAsHashMap<Index>(); else {
            cleanClose(reader, "Could not read the first line of the graph description properly, exiting the program.");
            graph = null;
        }
        int res = readVertices(numOfVertices, reader, graph);
        if (res == -1) {
            cleanClose(reader, "Could not read the vertices' description properly, exiting the program.");
            graph = null;
        }
        updateLoadProgress(progress, precentage);
        res = readEdges(reader, graph);
        if (res == -1) {
            cleanClose(reader, "Could not read the edges' description properly, exiting the program.");
            graph = null;
        }
        updateLoadProgress(progress, precentage);
        try {
            if (reader != null) reader.close();
        } catch (IOException ex) {
            LoggingManager.getInstance().writeSystem("An exception has occured while closing BufferedReader.\n" + ex.getMessage() + "\n" + ex.getStackTrace(), "GraphLoader", "analyzeFile", ex);
            graph = null;
        }
        return graph;
    }

    public static String getClassextension() {
        return EXTENSION;
    }

    public String getextension() {
        return EXTENSION;
    }

    @Override
    public GraphAuxStructure analyzeFile(InputStream in, GraphAuxStructure struct, AbstractExecution progress, double percentage, String filename) {
        return null;
    }
}
