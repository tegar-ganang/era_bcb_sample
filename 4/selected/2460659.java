package algo.examples;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Vector;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import moea.moga.examples.Profile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import algo.api.AbstractSolver;
import algo.api.Solution;
import algo.solvers.BreadthFirstSolver;

public class ProfileSolution implements Solution {

    public static final int AR = 0;

    public static final int ARP = 1;

    public static final int SLL = 2;

    public static final int DLL = 3;

    public static final int SLLO = 4;

    public static final int DLLO = 5;

    public static final int SLLAR = 6;

    public static final int DLLAR = 7;

    public static final int SLLARO = 8;

    public static final int DLLARO = 9;

    public static final String[] ddts = { "AR", "AR(P)", "SLL", "DLL", "SLL(O)", "DLL(O)", "SLL(AR)", "DLL(AR)", "SLL(ARO)", "DLL(ARO)" };

    protected static int varCount = 0;

    protected static String baseName = null;

    protected static String outDir = ".";

    protected static double cacheAccessTime;

    protected static double dramAccessTime;

    protected static double cacheLineSize;

    protected static double dramBandwith;

    protected static double cpuPower;

    protected static double cacheAccessEnergy;

    protected static double dramAccessPower;

    protected static long[] Ne;

    protected static double[] NeSd;

    protected static double[] Nve;

    protected static int Na;

    protected static double[] Nn;

    protected static int[] Te;

    protected static int Tref;

    protected static long[][] readsPerDdt;

    protected static long[][] writesPerDdt;

    protected static long[] readsPlusWritesPerDdt;

    protected static double[][] cacheMissesL1;

    public static void initializeProblem() {
        cacheAccessTime = 0.0000001;
        dramAccessTime = 0.000001;
        cacheLineSize = 32;
        dramBandwith = 50000000;
        cpuPower = 0.019;
        cacheAccessEnergy = 0.0001;
        dramAccessPower = 0.001;
        Element xmlProfileAnalyzer = null;
        try {
            File file = new File(outDir + File.separator + baseName + ".pro");
            if (!file.exists()) throw new Exception("The file path does not exist.");
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document docApplication = builder.parse(file.toURI().toString());
            xmlProfileAnalyzer = (Element) docApplication.getElementsByTagName("ProfileAnalyzer").item(0);
            varCount = loadScalarAsInteger(xmlProfileAnalyzer, "VarCount");
            Tref = loadScalarAsInteger(xmlProfileAnalyzer, "Tref");
            Na = loadScalarAsInteger(xmlProfileAnalyzer, "Na");
            Ne = loadArrayAsLong(xmlProfileAnalyzer, "Ne");
            NeSd = loadArrayAsDouble(xmlProfileAnalyzer, "NeSd");
            Nve = loadArrayAsDouble(xmlProfileAnalyzer, "Nve");
            Nn = loadArrayAsDouble(xmlProfileAnalyzer, "Nn");
            Te = loadArrayAsInteger(xmlProfileAnalyzer, "Te");
            readsPerDdt = loadMatrixAsLong(xmlProfileAnalyzer, "ReadsPerDdt");
            writesPerDdt = loadMatrixAsLong(xmlProfileAnalyzer, "WritesPerDdt");
        } catch (Exception ee) {
            ee.printStackTrace();
        }
        updateCacheMisses();
    }

    public static int loadScalarAsInteger(Element xmlProfileAnalyzer, String scalarName) {
        int scalar = 0;
        Element xmlNode = (Element) xmlProfileAnalyzer.getElementsByTagName(scalarName).item(0);
        scalar = Integer.valueOf(xmlNode.getAttribute("Value"));
        return scalar;
    }

    public static int[] loadArrayAsInteger(Element xmlProfileAnalyzer, String arrayName) {
        Element xmlNode = (Element) xmlProfileAnalyzer.getElementsByTagName(arrayName).item(0);
        int size = Integer.valueOf(xmlNode.getAttribute("Size"));
        int[] array = new int[size];
        NodeList xmlItemList = xmlNode.getElementsByTagName("Item");
        for (int i = 0; i < size; ++i) {
            Element xmlItem = (Element) xmlItemList.item(i);
            int index = Integer.valueOf(xmlItem.getAttribute("Index"));
            int value = Integer.valueOf(xmlItem.getAttribute("Value"));
            array[index] = value;
        }
        return array;
    }

    public static long[] loadArrayAsLong(Element xmlProfileAnalyzer, String arrayName) {
        Element xmlNode = (Element) xmlProfileAnalyzer.getElementsByTagName(arrayName).item(0);
        int size = Integer.valueOf(xmlNode.getAttribute("Size"));
        long[] array = new long[size];
        NodeList xmlItemList = xmlNode.getElementsByTagName("Item");
        for (int i = 0; i < size; ++i) {
            Element xmlItem = (Element) xmlItemList.item(i);
            int index = Integer.valueOf(xmlItem.getAttribute("Index"));
            long value = Long.valueOf(xmlItem.getAttribute("Value"));
            array[index] = value;
        }
        return array;
    }

    public static double[] loadArrayAsDouble(Element xmlProfileAnalyzer, String arrayName) {
        Element xmlNode = (Element) xmlProfileAnalyzer.getElementsByTagName(arrayName).item(0);
        int size = Integer.valueOf(xmlNode.getAttribute("Size"));
        double[] array = new double[size];
        NodeList xmlItemList = xmlNode.getElementsByTagName("Item");
        for (int i = 0; i < size; ++i) {
            Element xmlItem = (Element) xmlItemList.item(i);
            int index = Integer.valueOf(xmlItem.getAttribute("Index"));
            double value = Double.valueOf(xmlItem.getAttribute("Value"));
            array[index] = value;
        }
        return array;
    }

    public static long[][] loadMatrixAsLong(Element xmlProfileAnalyzer, String matrixName) {
        Element xmlNode = (Element) xmlProfileAnalyzer.getElementsByTagName(matrixName).item(0);
        int rows = Integer.valueOf(xmlNode.getAttribute("Rows"));
        int columns = Integer.valueOf(xmlNode.getAttribute("Columns"));
        long[][] matrix = new long[rows][columns];
        NodeList xmlItemList = xmlNode.getElementsByTagName("Item");
        for (int i = 0; i < xmlItemList.getLength(); ++i) {
            Element xmlItem = (Element) xmlItemList.item(i);
            int row = Integer.valueOf(xmlItem.getAttribute("Row"));
            int column = Integer.valueOf(xmlItem.getAttribute("Column"));
            long value = Long.valueOf(xmlItem.getAttribute("Value"));
            matrix[row][column] = value;
        }
        return matrix;
    }

    public static void updateCacheMisses() {
        readsPlusWritesPerDdt = new long[Profile.ddts.length];
        for (int i = 0; i < Profile.ddts.length; ++i) readsPlusWritesPerDdt[i] = 0;
        for (int i = 0; i < Profile.ddts.length; ++i) for (int j = 0; j < varCount; ++j) readsPlusWritesPerDdt[i] += (readsPerDdt[j][i] + writesPerDdt[j][i]);
        cacheMissesL1 = new double[varCount][Profile.ddts.length];
        for (int i = 0; i < varCount; ++i) for (int j = 0; j < Profile.ddts.length; ++j) cacheMissesL1[i][j] = (1.0 * NeSd[i] * (readsPlusWritesPerDdt[j])) / (readsPlusWritesPerDdt[j] + readsPerDdt[i][j] + writesPerDdt[i][j]);
    }

    protected ArrayList<Integer> ddtsSelected;

    protected boolean objectiveValid;

    protected double objectiveValue;

    public ProfileSolution() {
        ddtsSelected = new ArrayList<Integer>();
        objectiveValid = false;
    }

    public ProfileSolution(ProfileSolution src, Integer newDdt) {
        ddtsSelected = new ArrayList<Integer>();
        for (Integer i : src.ddtsSelected) {
            ddtsSelected.add(i);
        }
        ddtsSelected.add(newDdt);
        objectiveValid = false;
    }

    public ProfileSolution clone() {
        ProfileSolution clone = new ProfileSolution();
        clone.ddtsSelected = new ArrayList<Integer>();
        for (Integer i : ddtsSelected) {
            clone.ddtsSelected.add(i);
        }
        clone.objectiveValid = objectiveValid;
        clone.objectiveValue = objectiveValue;
        return clone;
    }

    public boolean isFeasible() {
        return true;
    }

    public boolean isComplete() {
        return ddtsSelected.size() == varCount;
    }

    public double getObjective() {
        if (this.objectiveValid) return this.objectiveValue;
        double randomCount = 0;
        double secuentialCount = 0;
        double averageSize = 0;
        double numCreations = 0;
        double reads = 0;
        double writes = 0;
        double execTime = 0;
        double performance = 0;
        double memory = 0;
        double energy = 0;
        for (int i = 0; i < ddtsSelected.size(); i++) {
            Integer ddt = ddtsSelected.get(i);
            if (ddt == null) continue;
            randomCount = 0;
            secuentialCount = 0;
            averageSize = 0;
            numCreations = 0;
            reads = readsPerDdt[i][ddt];
            writes = writesPerDdt[i][ddt];
            execTime = 0;
            if (ddt == Profile.AR) {
                secuentialCount = 9 * Ne[i];
                randomCount = 2;
                averageSize = 19 * Tref + Ne[i] * Te[i];
                numCreations = 1;
            } else if (ddt == Profile.ARP) {
                secuentialCount = 10 * Ne[i];
                randomCount = 3;
                averageSize = 19 * Tref + Ne[i] * (Tref + Te[i]);
                numCreations = 1 + Nve[i];
            } else if (ddt == Profile.SLL) {
                secuentialCount = 7 * Ne[i];
                randomCount = Ne[i] / 2 + 1;
                averageSize = 19 * Tref + Ne[i] * (2 * Tref + Te[i]);
                numCreations = 1 + Nve[i];
            } else if (ddt == Profile.DLL) {
                secuentialCount = 7 * Ne[i];
                randomCount = Ne[i] / 4 + 1;
                averageSize = 19 * Tref + Ne[i] * (3 * Tref + Te[i]);
                numCreations = 1 + Nve[i];
            } else if (ddt == Profile.SLLO) {
                secuentialCount = 10 * Ne[i];
                randomCount = Ne[i] / 3 + 1;
                averageSize = 20 * Tref + Ne[i] * (2 * Tref + Te[i]);
                numCreations = 1 + Nve[i];
            } else if (ddt == Profile.DLLO) {
                secuentialCount = 10 * Ne[i];
                randomCount = Ne[i] / 6 + 1;
                averageSize = 20 * Tref + Ne[i] * (3 * Tref + Te[i]);
                numCreations = 1 + Nve[i];
            } else if (ddt == Profile.SLLAR) {
                secuentialCount = 18 * Ne[i] + 8 * Na;
                randomCount = Nn[i] / 2 + 1;
                averageSize = 21 * Tref + Nn[i] * (21 * Tref + Ne[i] * (Te[i] + Tref));
                numCreations = 2 * Ne[i] + Nve[i];
            } else if (ddt == Profile.DLLAR) {
                secuentialCount = 18 * Ne[i] + 8 * Na;
                randomCount = Nn[i] / 4 + 1;
                averageSize = 21 * Tref + Nn[i] * (22 * Tref + Ne[i] * (Te[i] + Tref));
                numCreations = 2 * Ne[i] + Nve[i];
            } else if (ddt == Profile.SLLARO) {
                secuentialCount = 18 * Ne[i] + 10 * Na;
                randomCount = Nn[i] / 3 + 1;
                averageSize = 22 * Tref + Nn[i] * (21 * Tref + Ne[i] * (Te[i] + Tref));
                numCreations = 2 * Ne[i] + Nve[i];
            } else if (ddt == Profile.DLLARO) {
                secuentialCount = 18 * Ne[i] + 10 * Na;
                randomCount = Nn[i] / 6 + 1;
                averageSize = 22 * Tref + Nn[i] * (22 * Tref + Ne[i] * (Te[i] + Tref));
                numCreations = 2 * Ne[i] + Nve[i];
            }
            performance += 0.00001 * ((randomCount * (3 * (reads + writes - 2) / 4)) + (secuentialCount * ((reads + writes - 2) / 4)) + (2 * numCreations));
            memory += averageSize;
            execTime = (reads + writes) * (1 - cacheMissesL1[i][ddt]) * cacheAccessTime + (reads + writes) * cacheMissesL1[i][ddt] * dramAccessTime + (reads + writes) * cacheMissesL1[i][ddt] * cacheLineSize * (1 / dramBandwith);
            energy += (execTime * cpuPower + (reads + writes) * (1 - cacheMissesL1[i][ddt]) * cacheAccessEnergy + (reads + writes) * cacheMissesL1[i][ddt] * cacheAccessEnergy * cacheLineSize + (reads + writes) * cacheMissesL1[i][ddt] * dramAccessPower * (dramAccessTime + cacheLineSize / dramBandwith));
        }
        this.objectiveValue = performance + memory + energy;
        this.objectiveValid = true;
        return this.objectiveValue;
    }

    public double getBound() {
        if (AbstractSolver.bestSolution == null) return this.getObjective();
        ProfileSolution bestSolution = ((ProfileSolution) AbstractSolver.bestSolution).clone();
        for (int i = 0; i < ddtsSelected.size(); ++i) {
            bestSolution.ddtsSelected.set(i, null);
            bestSolution.objectiveValid = false;
        }
        return this.getObjective() + bestSolution.getObjective();
    }

    public Enumeration<Solution> getSuccessors() {
        Vector<Solution> succesors = new Vector<Solution>();
        for (int i = 0; i < ddts.length; i++) {
            Solution solution = new ProfileSolution(this, i);
            if (solution.getObjective() < AbstractSolver.bestObjective) succesors.add(solution);
        }
        return succesors.elements();
    }

    public int compareTo(Solution rhs) {
        double objLhs = this.getObjective();
        double objRhs = rhs.getObjective();
        if (objLhs < objRhs) return -1;
        if (objLhs == objRhs) return 0;
        return 1;
    }

    public String toString() {
        String result = "";
        result += this.getObjective();
        for (int i = 0; i < varCount; i++) result += "\t" + ddtsSelected.get(i);
        return result;
    }

    public static void main(String[] args) throws Exception {
        System.out.print("XML basename (BASENAME.pro): ");
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        baseName = br.readLine();
        ProfileSolution.initializeProblem();
        for (int i = 0; i < 1; i++) {
            System.out.println("Iteration number: " + i);
            Solution initialSolution = new ProfileSolution();
            AbstractSolver solver = new BreadthFirstSolver();
            double start = System.currentTimeMillis();
            Solution solution = solver.solve(initialSolution);
            System.out.println("Final solution: " + solution.toString());
            double end = System.currentTimeMillis();
            System.out.println("Time: " + (end - start) / 1000);
            System.out.println("done.");
        }
    }
}
