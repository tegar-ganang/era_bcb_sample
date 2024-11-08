package playground.johannes.snowball;

import hep.aida.ref.Histogram1D;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import org.matsim.config.Config;
import org.matsim.gbl.Gbl;
import org.matsim.utils.io.IOUtils;
import playground.johannes.socialnets.GraphStatistics;
import playground.johannes.socialnets.PersonGraphMLFileHandler;
import playground.johannes.socialnets.UserDataKeys;
import cern.colt.list.IntArrayList;
import edu.uci.ics.jung.algorithms.cluster.WeakComponentClusterer;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.io.GraphMLFile;
import edu.uci.ics.jung.random.generators.BarabasiAlbertGenerator;
import edu.uci.ics.jung.random.generators.ErdosRenyiGenerator;
import edu.uci.ics.jung.random.generators.WattsBetaSmallWorldGenerator;

/**
 * @author illenberger
 *
 */
public class SnowballAnalyser {

    private static Logger logger = Logger.getLogger(SnowballAnalyser.class);

    private static final String DEGREE_DIR = "degree/";

    private static final String CLUSTERING_DIR = "clustering/";

    private static final String BETWEENESS_DIR = "betweeness/";

    private static final String APL_DIR = "apl/";

    private static final String PAJEK_DIR = "pajek/";

    private static final String NETWORK_TYPE_FILE = "file";

    private static final String NETWORK_TYPE_RANDOM = "random";

    private static final String NETWORK_TYPE_SMALLWORLD = "smallworld";

    private static final String NETWORK_TYPE_BARABSI_ALBERT = "barabsialbert";

    public static void main(String args[]) throws FileNotFoundException, IOException {
        Config config = Gbl.createConfig(args);
        final String MODULE_NAME = "snowballsampling";
        String graphFile = config.getParam(MODULE_NAME, "inputGraph");
        String networkType = config.getParam(MODULE_NAME, "networktype");
        int numEdges = Integer.parseInt(config.getParam(MODULE_NAME, "numEdges"));
        int numVertices = Integer.parseInt(config.getParam(MODULE_NAME, "numVertices"));
        double param1 = Double.parseDouble(config.getParam(MODULE_NAME, "param1"));
        int degree = Integer.parseInt(config.getParam(MODULE_NAME, "degree"));
        double pParticipate = Double.parseDouble(config.getParam(MODULE_NAME, "pParticipate"));
        double pTieNamed = Double.parseDouble(config.getParam(MODULE_NAME, "pTieNamed"));
        long randomseed = Gbl.getConfig().global().getRandomSeed();
        String outDir = config.getParam(MODULE_NAME, "outputDir");
        int seeds = Integer.parseInt(config.getParam(MODULE_NAME, "seeds"));
        int waves = Integer.parseInt(config.getParam(MODULE_NAME, "waves"));
        boolean extendedAnalysis = Boolean.parseBoolean(config.getParam(MODULE_NAME, "extendedAnalysis"));
        new File(outDir + DEGREE_DIR).mkdirs();
        new File(outDir + CLUSTERING_DIR).mkdirs();
        new File(outDir + BETWEENESS_DIR).mkdirs();
        new File(outDir + APL_DIR).mkdirs();
        new File(outDir + PAJEK_DIR).mkdirs();
        Graph g = null;
        if (NETWORK_TYPE_FILE.equals(networkType)) {
            logger.info("Loading social network from file...");
            PersonGraphMLFileHandler fileHandler = new PersonGraphMLFileHandler();
            GraphMLFile gmlFile = new GraphMLFile(fileHandler);
            g = gmlFile.load(graphFile);
        } else if (NETWORK_TYPE_RANDOM.equals(networkType)) {
            logger.info("Generating random network...");
            ErdosRenyiGenerator generator = new ErdosRenyiGenerator(numVertices, param1);
            generator.setSeed(randomseed);
            g = (Graph) generator.generateGraph();
            initUserDatums(g);
        } else if (NETWORK_TYPE_SMALLWORLD.equals(networkType)) {
            WattsBetaSmallWorldGenerator generator = new WattsBetaSmallWorldGenerator(numVertices, param1, degree);
            g = (Graph) generator.generateGraph();
            initUserDatums(g);
        } else if (NETWORK_TYPE_BARABSI_ALBERT.equals(networkType)) {
            BarabasiAlbertGenerator generator = new BarabasiAlbertGenerator(numVertices, numEdges, false, false, (int) randomseed);
            generator.evolveGraph((int) param1);
            g = (Graph) generator.generateGraph();
            initUserDatums(g);
        }
        BufferedWriter meanWriter = IOUtils.getBufferedWriter(outDir + "meanValues.txt");
        meanWriter.write("wave\tnumVertices\tnumEdges\tfracVertices\tfracEdges\tnumVNotTail\tfracVNotTail\tnumIsolated\tdegree\tclustering\tassortativity\tcomponents\tbetweeness\tapl");
        meanWriter.newLine();
        logger.info("Computing graph statistics...");
        GraphStatsContainer dummy = new GraphStatsContainer();
        dummy.g = g;
        GraphStatsContainer obsGraphStats = dumpGraphStatistics(g, dummy, meanWriter, outDir, 0, extendedAnalysis);
        Sampler sampler = new Sampler(randomseed);
        sampler.setPParticipate(pParticipate);
        sampler.setPTieNamed(pTieNamed);
        for (int wave = 1; wave <= waves; wave++) {
            logger.info("Sampling with " + wave + " waves...");
            sampler.run(g, wave, seeds);
            logger.info("Extracting sampled graph...");
            Graph sample = sampler.extractSampledGraph(g, false);
            logger.info("Computing sampled graph statistics...");
            dumpGraphStatistics(sample, obsGraphStats, meanWriter, outDir, wave, extendedAnalysis);
        }
        logger.info("Done.");
    }

    private static void initUserDatums(Graph g) {
        int counter = 0;
        for (Object v : g.getVertices()) {
            ((Vertex) v).addUserDatum(UserDataKeys.ID, String.valueOf(counter), UserDataKeys.COPY_ACT);
            ((Vertex) v).addUserDatum(UserDataKeys.X_COORD, 0.0, UserDataKeys.COPY_ACT);
            ((Vertex) v).addUserDatum(UserDataKeys.Y_COORD, 0.0, UserDataKeys.COPY_ACT);
        }
    }

    private static GraphStatsContainer dumpGraphStatistics(Graph g, GraphStatsContainer stats, BufferedWriter meanWriter, String outDir, int wave, boolean extendedAnalysis) throws IOException {
        DegreeHistogramThread degreeThread = null;
        ClusteringThread clusteringThread = null;
        CoverageThread coverageThread = new CoverageThread(g, wave);
        AssortativityThread assortativityThread = new AssortativityThread(g);
        ComponentsThread componentsThread = new ComponentsThread(g);
        BetweenessThread betweenessThread = null;
        APLThread aplThread = null;
        if (wave == 0) {
            degreeThread = new DegreeHistogramThread(g, wave, -1, -1);
            clusteringThread = new ClusteringThread(g, wave, -1, -1);
            betweenessThread = new BetweenessThread(g, wave, -1, -1);
            aplThread = new APLThread(g, wave, -1, -1);
        } else {
            degreeThread = new DegreeHistogramThread(g, wave, (int) stats.degreeHistogram.xAxis().lowerEdge(), (int) stats.degreeHistogram.xAxis().upperEdge());
            clusteringThread = new ClusteringThread(g, wave, stats.clusteringHistogram.xAxis().lowerEdge(), stats.clusteringHistogram.xAxis().upperEdge());
            betweenessThread = new BetweenessThread(g, wave, stats.betweenessHistogram.xAxis().lowerEdge(), stats.betweenessHistogram.xAxis().upperEdge());
            aplThread = new APLThread(g, wave, stats.aplHistogram.xAxis().lowerEdge(), stats.aplHistogram.xAxis().upperEdge());
        }
        List<Thread> threads = new ArrayList<Thread>();
        threads.add(coverageThread);
        threads.add(degreeThread);
        threads.add(clusteringThread);
        threads.add(assortativityThread);
        threads.add(componentsThread);
        if (extendedAnalysis) {
            threads.add(betweenessThread);
            threads.add(aplThread);
        }
        for (Thread t : threads) t.start();
        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        meanWriter.write(String.valueOf(wave));
        meanWriter.write("\t");
        meanWriter.write(String.valueOf(coverageThread.numVertices));
        meanWriter.write("\t");
        meanWriter.write(String.valueOf(coverageThread.numEdges));
        meanWriter.write("\t");
        meanWriter.write(String.valueOf(coverageThread.numVertices / (float) stats.g.numVertices()));
        meanWriter.write("\t");
        meanWriter.write(String.valueOf(coverageThread.numEdges / (float) stats.g.numEdges()));
        meanWriter.write("\t");
        meanWriter.write(String.valueOf(coverageThread.numVerticesNotTail));
        meanWriter.write("\t");
        meanWriter.write(String.valueOf(coverageThread.numVerticesNotTail / (float) stats.g.numVertices()));
        meanWriter.write("\t");
        meanWriter.write(String.valueOf(coverageThread.numIsolatedVertices));
        meanWriter.write("\t");
        meanWriter.write(String.valueOf((float) degreeThread.getHistogram().mean()));
        meanWriter.write("\t");
        meanWriter.write(String.valueOf((float) clusteringThread.getHistogram().mean()));
        meanWriter.write("\t");
        meanWriter.write(String.valueOf((float) assortativityThread.getAssortativity()));
        meanWriter.write("\t");
        meanWriter.write(String.valueOf(componentsThread.getNumComponents()));
        if (extendedAnalysis) {
            meanWriter.write("\t");
            meanWriter.write(String.valueOf((float) betweenessThread.getHistogram().mean()));
            meanWriter.write("\t");
            meanWriter.write(String.valueOf((float) aplThread.getHistogram().mean()));
        } else {
            meanWriter.write("\tn.a.\tn.a.");
        }
        meanWriter.newLine();
        meanWriter.flush();
        GraphStatistics.writeHistogram(degreeThread.getHistogram(), outDir + DEGREE_DIR + wave + ".degree.png");
        GraphStatistics.writeHistogram(clusteringThread.getHistogram(), outDir + CLUSTERING_DIR + wave + ".clustering.png");
        if (extendedAnalysis) {
            GraphStatistics.writeHistogram(betweenessThread.getHistogram(), outDir + BETWEENESS_DIR + wave + ".betweeness.png");
            GraphStatistics.writeHistogram(aplThread.getHistogram(), outDir + APL_DIR + wave + ".apl.png");
        }
        PajekVisWriter pajekWriter = new PajekVisWriter();
        pajekWriter.write(g, outDir + PAJEK_DIR + wave + ".sampled.net");
        GraphStatsContainer container = new GraphStatsContainer();
        container.g = g;
        container.degreeHistogram = degreeThread.getHistogram();
        container.clusteringHistogram = clusteringThread.getHistogram();
        container.betweenessHistogram = betweenessThread.getHistogram();
        container.aplHistogram = aplThread.getHistogram();
        return container;
    }

    private static class CoverageThread extends Thread {

        private Graph g;

        private int wave;

        private int numVertices;

        private int numEdges;

        private int numVerticesNotTail;

        private int numIsolatedVertices;

        public CoverageThread(Graph g, int wave) {
            this.g = g;
            this.wave = wave;
        }

        public void run() {
            numVertices = g.numVertices();
            numEdges = g.numEdges();
            numVerticesNotTail = 0;
            for (Object o : g.getVertices()) {
                IntArrayList waves = (IntArrayList) ((Vertex) o).getUserDatum(UserDataKeys.WAVE_KEY);
                if (waves != null) {
                    if (waves.get(0) != wave) numVerticesNotTail++;
                }
            }
            numIsolatedVertices = GraphStatistics.countIsolates(g);
        }

        public int getNumVertices() {
            return numVertices;
        }

        public int getNumEdges() {
            return numEdges;
        }

        public int getNumVerticesNotTail() {
            return numVerticesNotTail;
        }

        public int getNumIsolatedVertices() {
            return numIsolatedVertices;
        }
    }

    private static class HistogramThread extends Thread {

        protected Graph g;

        protected int wave;

        protected double min;

        protected double max;

        protected Histogram1D histogram;

        public HistogramThread(Graph g, int wave, double min, double max) {
            this.g = g;
            this.wave = wave;
            this.min = min;
            this.max = max;
        }

        public Histogram1D getHistogram() {
            return histogram;
        }
    }

    private static class DegreeHistogramThread extends HistogramThread {

        public DegreeHistogramThread(Graph g, int wave, double min, double max) {
            super(g, wave, min, max);
        }

        public void run() {
            if (wave == 0) histogram = GraphStatistics.createDegreeHistogram(g, -1, -1, wave); else histogram = GraphStatistics.createDegreeHistogram(g, (int) min, (int) max, wave);
        }
    }

    private static class ClusteringThread extends HistogramThread {

        public ClusteringThread(Graph g, int wave, double min, double max) {
            super(g, wave, min, max);
        }

        public void run() {
            if (wave == 0) histogram = GraphStatistics.createClusteringCoefficientsHistogram(g, -1, -1, wave); else histogram = GraphStatistics.createClusteringCoefficientsHistogram(g, min, max, wave);
        }
    }

    private static class AssortativityThread extends Thread {

        private Graph g;

        private double assortativity;

        public AssortativityThread(Graph g) {
            this.g = g;
        }

        public void run() {
            assortativity = GraphStatistics.pearsonCorrelationCoefficient(g);
        }

        public double getAssortativity() {
            return assortativity;
        }
    }

    private static class ComponentsThread extends Thread {

        private Graph g;

        private int numComponents;

        public ComponentsThread(Graph g) {
            this.g = g;
        }

        public void run() {
            numComponents = new WeakComponentClusterer().extract(g).size();
        }

        public int getNumComponents() {
            return numComponents;
        }
    }

    private static class BetweenessThread extends HistogramThread {

        public BetweenessThread(Graph g, int wave, double min, double max) {
            super(g, wave, min, max);
        }

        public void run() {
            histogram = GraphStatistics.createBetweenessHistogram(g, min, max);
        }
    }

    private static class APLThread extends HistogramThread {

        public APLThread(Graph g, int wave, double min, double max) {
            super(g, wave, min, max);
        }

        public void run() {
            histogram = GraphStatistics.createAPLHistogram(g, min, max);
        }
    }

    private static class GraphStatsContainer {

        public Graph g;

        public Histogram1D degreeHistogram;

        public Histogram1D clusteringHistogram;

        public Histogram1D betweenessHistogram;

        public Histogram1D aplHistogram;
    }
}
