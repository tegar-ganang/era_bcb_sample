package eu.redseeds.owl.tests;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import de.uni_koblenz.jgralab.GraphIO;
import de.uni_koblenz.jgralab.GraphIOException;
import edu.stanford.smi.protegex.owl.jena.JenaOWLModel;
import eu.redseeds.owl.Properties;
import eu.redseeds.owl.SupportFunctions;
import eu.redseeds.owl.connector.Comparator;
import eu.redseeds.scl.SCLGraph;

/**
 * This class defines Test Cases. It does not provide functionality to be
 * used within the ReDSeeDS Engine but to test the DL-based similarity
 * measure.
 * 
 * @author preliminary version: Thorsten Krebs
 * @author final version: Arved, Lothar
 */
public class TestCase {

    public static final String commonCasesDir = Properties.WORKING_DIR + "SoftwareCases" + File.separator + "common" + File.separator;

    public static final String experiment16 = Properties.WORKING_DIR + "SoftwareCases" + File.separator + "experiment16cases" + File.separator;

    public static final String twoCases = Properties.WORKING_DIR + "SoftwareCases" + File.separator + "twoCases" + File.separator;

    public static final String taxDistExp = Properties.WORKING_DIR + "SoftwareCases" + File.separator + "taxDistExp" + File.separator;

    public static final String caseLargerThanQuery = Properties.WORKING_DIR + "SoftwareCases" + File.separator + "caseLargerThanQuery" + File.separator;

    public static final boolean testClassifiedAndNotClassified = false;

    @SuppressWarnings("unused")
    public static void main(String[] args) {
        Properties.test = true;
        SupportFunctions.startPelletDigServer();
        String arg = args[0];
        Vector<String> scFiles = new Vector<String>();
        List<SCLGraph> testGraphs = new ArrayList<SCLGraph>();
        if (arg.equals("testInterface")) {
            String queryFileName = "sclgraph1.tg";
            SCLGraph queryGraph = null;
            Integer queryNumber = 0;
            try {
                if (!(queryFileName.equals(""))) {
                    queryNumber = 1;
                    System.out.println("Loading graph from file " + twoCases + queryFileName + "...");
                    queryGraph = (SCLGraph) GraphIO.loadGraphFromFile(twoCases + queryFileName, null);
                }
            } catch (GraphIOException e) {
                System.out.println(" WARNING: unable to open graph file!");
                e.printStackTrace();
            }
            scFiles.add("sclgraph2.tg");
            String outfileName;
            JenaOWLModel ontology;
            Integer sizeOfTBox, sizeOfTBox2;
            HashMap<String, Integer> allSizes = new HashMap<String, Integer>();
            if (Properties.CREATE_SINGLE_FILES == true) {
            } else {
                testGraphs = new ArrayList<SCLGraph>();
                for (String n : scFiles) {
                    SCLGraph scGraph = null;
                    try {
                        System.out.println("Loading graph from file " + twoCases + n + "...");
                        scGraph = (SCLGraph) GraphIO.loadGraphFromFile(twoCases + n, null);
                        testGraphs.add((SCLGraph) scGraph);
                    } catch (GraphIOException e) {
                        e.printStackTrace();
                    }
                }
                System.out.println("Number of loaded Graphs: " + testGraphs.size());
                Comparator myComparator;
                if (Properties.COMPUTE_SIMILARITY) {
                    assert testGraphs.size() == scFiles.size();
                    Properties.CLASSIFY = false;
                    for (int i = 0; i < testGraphs.size(); i++) {
                        System.out.println(" Starting OWL similarity computation for case " + scFiles.elementAt(i) + " and query " + queryFileName);
                        myComparator = new Comparator(queryGraph, testGraphs.get(i));
                        System.out.println(" Similarity: " + myComparator.runCompare());
                    }
                    Properties.CLASSIFY = true;
                    for (int i = 0; i < testGraphs.size(); i++) {
                        System.out.println(" Starting OWL similarity experiment for case " + scFiles.elementAt(i) + " and query " + queryFileName);
                        myComparator = new Comparator(queryGraph, testGraphs.get(i));
                        System.out.println(" Similarity: " + myComparator.runCompare());
                    }
                }
            }
        } else if (arg.equals("owlExperiment")) {
            OWLSimilarityExperiment currentExperiment = null;
            currentExperiment = new OWLSimilarityExperiment(commonCasesDir);
        } else {
            System.out.println("Argument has to be \"testInterface\" or \"owlExperiment\"!");
        }
    }

    public static void copy(String file1, String file2) throws IOException {
        File inputFile = new File(file1);
        File outputFile = new File(file2);
        FileReader in = new FileReader(inputFile);
        FileWriter out = new FileWriter(outputFile);
        System.out.println("Copy file from: " + file1 + " to: " + file2);
        int c;
        while ((c = in.read()) != -1) out.write(c);
        in.close();
        out.close();
    }

    @SuppressWarnings("unused")
    private static void printSizes(HashMap<String, Integer> allSizes) {
        Set<Map.Entry<String, Integer>> set = allSizes.entrySet();
        Iterator<Map.Entry<String, Integer>> i = set.iterator();
        while (i.hasNext()) {
            Map.Entry<String, Integer> me = (Map.Entry<String, Integer>) i.next();
            System.out.println(me.getKey() + "  Size: " + me.getValue());
        }
    }

    @SuppressWarnings("unused")
    private static Integer computeTBoxSize(JenaOWLModel ontology) {
        int count = 0;
        Iterator<?> allConcepts = ontology.getRDFSClasses().iterator();
        while (allConcepts.hasNext()) {
            if (allConcepts.next().getClass().getName().equals("edu.stanford.smi.protegex.owl.model.impl.DefaultOWLNamedClass")) count = count + 1;
        }
        return count;
    }
}
