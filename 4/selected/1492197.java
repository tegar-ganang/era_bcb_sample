package moea.moga.examples;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import moea.commons.ObjectiveVector;
import moea.commons.Population;
import moea.moga.algorithms.Moea;
import moea.moga.algorithms.Nsga2;
import moea.moga.algorithms.Spea2;
import moea.moga.algorithms.Vega;
import moea.moga.genome.Chromosome;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import ext.number.ValModPosInt;
import ext.number.Value;

public class Profile extends Chromosome {

    private static final long serialVersionUID = 1L;

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

    public static double cacheAccessTime;

    public static double dramAccessTime;

    public static double cacheLineSize;

    public static double dramBandwith;

    public static double cpuPower;

    public static double cacheAccessEnergy;

    public static double dramAccessPower;

    public static long[] Ne;

    public static double[] NeSd;

    public static double[] Nve;

    public static int Na;

    public static double[] Nn;

    public static int[] Te;

    public static int Tref;

    public static long[][] readsPerDdt;

    public static long[][] writesPerDdt;

    public static long[] readsPlusWritesPerDdt;

    public static double[][] cacheMissesL1;

    public static void initializeProblem(String proPath) {
        cacheAccessTime = 0.0000001;
        dramAccessTime = 0.000001;
        cacheLineSize = 32;
        dramBandwith = 50000000;
        cpuPower = 0.019;
        cacheAccessEnergy = 0.0001;
        dramAccessPower = 0.001;
        Element xmlProfileAnalyzer = null;
        try {
            File file = new File(proPath);
            if (!file.exists()) throw new Exception("The file path does not exist.");
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document docApplication = builder.parse(file.toURI().toString());
            xmlProfileAnalyzer = (Element) docApplication.getElementsByTagName("ProfileAnalyzer").item(0);
            N = 3;
            M = loadScalarAsInteger(xmlProfileAnalyzer, "VarCount");
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
        try {
            ValModPosInt.setModulus(DLLARO + 1);
        } catch (Exception ee) {
            ee.printStackTrace();
        }
        xL = new Value[M];
        xU = new Value[M];
        for (int j = 0; j < M; j++) {
            xL[j] = new ValModPosInt(AR);
            xU[j] = new ValModPosInt(DLLARO);
        }
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
        for (int i = 0; i < Profile.ddts.length; ++i) for (int j = 0; j < M; ++j) readsPlusWritesPerDdt[i] += (readsPerDdt[j][i] + writesPerDdt[j][i]);
        cacheMissesL1 = new double[M][Profile.ddts.length];
        for (int i = 0; i < M; ++i) for (int j = 0; j < Profile.ddts.length; ++j) cacheMissesL1[i][j] = (1.0 * NeSd[i] * (readsPlusWritesPerDdt[j])) / (readsPlusWritesPerDdt[j] + readsPerDdt[i][j] + writesPerDdt[i][j]);
    }

    public Profile() {
        super();
    }

    public Profile(Value[] x) {
        super();
        super.x = x;
    }

    public Profile(Profile src) {
        super(src);
    }

    public Profile clone() {
        Profile clone = new Profile(this);
        return clone;
    }

    public void evaluate() {
        for (int i = 0; i < 3; i++) objectiveVector.set(i, 0.0);
        double performance = 0;
        double memory = 0;
        double energy = 0;
        double randomCount = 0;
        double secuentialCount = 0;
        double averageSize = 0;
        double numCreations = 0;
        double reads = 0;
        double writes = 0;
        double execTime = 0;
        for (int i = 0; i < M; i++) {
            int ddt = x[i].intValue();
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
        objectiveVector.set(0, performance);
        objectiveVector.set(1, memory);
        objectiveVector.set(2, energy);
    }

    public static void main(String[] args) throws Exception {
        String outDir = ".";
        if (args.length < 5) {
            System.out.println("Usage:");
            System.out.println("java -jar Profile.jar <XML-PROFILE> <VEGA|SPEA2|NSGA2> <NumTrials:1..Inf> <NumOfIndividuals:1..Inf> <MaxGenerations:1..Inf> <UniformAnalysis:true|false>");
            args = new String[6];
            args[0] = "Physics.pro";
            args[1] = "VEGA";
            args[2] = "0";
            args[3] = "200";
            args[4] = "4000";
            args[5] = "true";
            outDir = "D:/jlrisco/Trabajo/MisPapers/IEEE T EVOLUT COMPUT/2007/Results";
            return;
        }
        String proPath = outDir + File.separator + args[0];
        String algorithmName = args[1];
        Integer numberOfTrials = Integer.valueOf(args[2]);
        Integer numberOfIndividuals = Integer.valueOf(args[3]);
        Integer maxGenerations = Integer.valueOf(args[4]);
        Boolean uniformAnalysis = false;
        if (args.length > 5) uniformAnalysis = Boolean.valueOf(args[5]);
        Profile.initializeProblem(proPath);
        BufferedWriter loggerMetrics = new BufferedWriter(new FileWriter(new File(proPath + "." + algorithmName + ".MetricsAvg")));
        ArrayList<Double> spreads = new ArrayList<Double>();
        ArrayList<Double> spacings = new ArrayList<Double>();
        ArrayList<Double> times = new ArrayList<Double>();
        Moea algorithm = null;
        for (int i = 0; i < numberOfTrials; i++) {
            BufferedWriter loggerPop = new BufferedWriter(new FileWriter(new File(proPath + "." + algorithmName + "." + i)));
            System.out.println("Iteration number: " + i);
            Population<Chromosome> popIni = new Population<Chromosome>();
            for (int k = 0; k < numberOfIndividuals; ++k) {
                Profile ind = new Profile();
                popIni.add(ind);
            }
            if (algorithmName.equals("VEGA")) algorithm = new Vega("Vega", popIni, maxGenerations, 0.80, 0.01); else if (algorithmName.equals("SPEA2")) algorithm = new Spea2("Spea2", popIni, maxGenerations, 0.80, 0.01); else if (algorithmName.equals("NSGA2")) algorithm = new Nsga2("Nsga2", popIni, maxGenerations, 0.80, 0.01);
            double start = System.currentTimeMillis();
            while (!algorithm.done()) {
                if (algorithm.getCurrentGeneration() % 100 == 0) {
                    System.out.println("Current generation: " + algorithm.getCurrentGeneration());
                }
                algorithm.step();
            }
            double end = System.currentTimeMillis();
            spreads.add(algorithm.getPopulation().calculateSpread());
            spacings.add(algorithm.getPopulation().calculateSpacing());
            times.add((end - start) / 1000.0);
            System.out.println("Spread: " + spreads.get(spreads.size() - 1));
            System.out.println("Spacing: " + spacings.get(spacings.size() - 1));
            System.out.println("Time: " + (end - start) / 1000);
            System.out.println("done.");
            loggerPop.write(ParetoFrontToString(algorithm.getPopulation()));
            loggerPop.flush();
            loggerPop.close();
        }
        if (uniformAnalysis) saveUniformObjectives(proPath);
        double spreadMean = calculateMean(spreads);
        double spacingMean = calculateMean(spacings);
        double timeMean = calculateMean(times);
        double spreadStd = calculateStd(spreads, spreadMean);
        double spacingStd = calculateStd(spacings, spacingMean);
        double timeStd = calculateStd(times, timeMean);
        loggerMetrics.write("Spread(Mean,Std):\t" + spreadMean + "\t" + spreadStd + "\n");
        loggerMetrics.write("Spacing(Mean,Std):\t" + spacingMean + "\t" + spacingStd + "\n");
        loggerMetrics.write("Time(Mean,Std):\t" + timeMean + "\t" + timeStd + "\n");
        loggerMetrics.flush();
        loggerMetrics.close();
    }

    public static double calculateMean(ArrayList<Double> list) {
        double res = 0;
        for (Double d : list) res += d;
        res = res / list.size();
        return res;
    }

    public static double calculateStd(ArrayList<Double> list, double mean) {
        double res = 0;
        for (Double d : list) res += Math.pow(d - mean, 2);
        res = Math.sqrt(res / (list.size() - 1));
        return res;
    }

    public static String ParetoFrontToString(Population<Chromosome> pop) {
        StringBuffer buffer = new StringBuffer();
        Population<Chromosome> popTemp = new Population<Chromosome>();
        popTemp.add(pop);
        popTemp.keepNonDominated();
        for (Chromosome chrom : popTemp) {
            ObjectiveVector objs = chrom.getObjectiveVector();
            for (int i = 0; i < Profile.N; ++i) buffer.append(objs.get(i) + "\t");
            buffer.append("\n");
        }
        return buffer.toString();
    }

    public static String PopulationToString(Population<Chromosome> pop) {
        StringBuffer buffer = new StringBuffer();
        for (Chromosome chrom : pop) {
            ObjectiveVector objs = chrom.getObjectiveVector();
            for (int i = 0; i < Profile.N; ++i) buffer.append(objs.get(i) + "\t");
            buffer.append("\n");
        }
        return buffer.toString();
    }

    public static void saveUniformObjectives(String proPath) throws Exception {
        StringBuffer buffer = new StringBuffer();
        BufferedWriter out = new BufferedWriter(new FileWriter(new File(proPath + ".UniformAnalysis")));
        Value[] xTemp = new Value[M];
        for (int d = 0; d < Profile.ddts.length; d++) {
            for (int i = 0; i < M; i++) {
                xTemp[i] = new ValModPosInt(d);
            }
            Profile temp = new Profile(xTemp);
            temp.evaluate();
            ObjectiveVector objs = temp.getObjectiveVector();
            for (int i = 0; i < Profile.N; ++i) {
                buffer.append(objs.get(i) + "\t");
            }
            buffer.append("\n");
        }
        for (int i = 0; i < M; i++) {
            xTemp[i] = new ValModPosInt((int) (ddts.length * Math.random()));
        }
        Profile temp = new Profile(xTemp);
        temp.evaluate();
        ObjectiveVector objs = temp.getObjectiveVector();
        for (int i = 0; i < Profile.N; ++i) {
            buffer.append(objs.get(i) + "\t");
        }
        buffer.append("\n");
        out.write(buffer.toString());
        out.flush();
        out.close();
    }

    public static int getSubPopSize(int popSize, int numThreads) {
        if (numThreads == 1) return popSize;
        double ratio = (1.0 * popSize) / numThreads;
        return (int) Math.round(ratio + (2.0 / numThreads) * (popSize - ratio));
    }

    public static int getMigrationRate(int popSize) {
        if (popSize < 100) return 1;
        return (int) Math.round((1.0 * popSize) / 100.0);
    }
}
