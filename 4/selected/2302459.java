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
import ext.number.ValInt;
import ext.number.ValLong;
import ext.number.Value;

public class ProfileChristosV2 extends Chromosome {

    private static final long serialVersionUID = 1L;

    public static final int NOTHING = 0;

    public static final int AR = 1;

    public static final int ARP = 2;

    public static final int SLL = 3;

    public static final int DLL = 4;

    public static final int SLLO = 5;

    public static final int DLLO = 6;

    public static final String[] ddts = { "NOTHING", "AR", "ARP", "SLL", "DLL", "SLLO", "DLLO" };

    public static long[] Ne;

    public static double[] NeSd;

    public static double[] Nve;

    public static int[] Te;

    public static int Tref;

    public static long[] reads;

    public static long[] writes;

    public static void initializeProblem(String proPath) {
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
            M = 5 * loadScalarAsInteger(xmlProfileAnalyzer, "VarCount");
            Tref = loadScalarAsInteger(xmlProfileAnalyzer, "Tref");
            Ne = loadArrayAsLong(xmlProfileAnalyzer, "Ne");
            NeSd = loadArrayAsDouble(xmlProfileAnalyzer, "NeSd");
            Nve = loadArrayAsDouble(xmlProfileAnalyzer, "Nve");
            Te = loadArrayAsInteger(xmlProfileAnalyzer, "Te");
            reads = loadArrayAsLong(xmlProfileAnalyzer, "Reads");
            writes = loadArrayAsLong(xmlProfileAnalyzer, "Writes");
        } catch (Exception ee) {
            ee.printStackTrace();
        }
        xL = new Value[M];
        xU = new Value[M];
        for (int j = 0; j < M / 5; j++) {
            xL[5 * j + 0] = new ValInt(AR);
            xU[5 * j + 0] = new ValInt(DLLO);
            xL[5 * j + 1] = new ValInt(NOTHING);
            xU[5 * j + 1] = new ValInt(DLLO);
            xL[5 * j + 2] = new ValLong(0);
            xU[5 * j + 2] = new ValLong(Ne[j]);
            xL[5 * j + 3] = new ValInt(NOTHING);
            xU[5 * j + 3] = new ValInt(DLLO);
            xL[5 * j + 4] = new ValLong(0);
            xU[5 * j + 4] = new ValLong(Ne[j]);
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

    public ProfileChristosV2() {
        super();
    }

    public ProfileChristosV2(Value[] x) {
        super();
        super.x = x;
    }

    public ProfileChristosV2(ProfileChristosV2 src) {
        super(src);
    }

    public ProfileChristosV2 clone() {
        ProfileChristosV2 clone = new ProfileChristosV2(this);
        return clone;
    }

    public void evaluate() {
        for (int i = 0; i < 3; i++) objectiveVector.set(i, 0.0);
        double memoryAccesses = 0;
        double memoryUsage = 0;
        double energyConsumption = 0;
        double sequentialAccesses = 0;
        double randomAccesses = 0;
        double averageSize = 0;
        double readsCounter = 0;
        double writesCounter = 0;
        int Te0 = 0;
        int Te1 = 0;
        int Te2 = 0;
        for (int i = 0; i < M / 5; i++) {
            int ddt0 = x[5 * i + 0].intValue();
            int ddt1 = x[5 * i + 1].intValue();
            int ddt2 = x[5 * i + 3].intValue();
            long na1 = x[5 * i + 2].longValue();
            long na2 = x[5 * i + 4].longValue();
            Te0 = Tref;
            Te1 = Tref;
            Te2 = Te[i];
            if (ddt1 == NOTHING || na1 == 0) {
                Te0 = Te[i];
                x[5 * i + 1].setValue(0);
                ddt1 = NOTHING;
                x[5 * i + 2].setValue(0);
                na1 = 1;
                x[5 * i + 3].setValue(NOTHING);
                ddt2 = NOTHING;
            }
            if (ddt2 == NOTHING || na2 == 0) {
                Te1 = Te[i];
                x[5 * i + 3].setValue(NOTHING);
                ddt2 = NOTHING;
                x[5 * i + 4].setValue(0);
                na2 = 1;
            }
            double complexity = calculateComplexity(ddt0, ddt1, ddt2, i);
            double na0 = (1.0 * Ne[i] / (1.0 * na1 * na2));
            if (na0 == 0) na0 = Math.round(Long.MAX_VALUE / (na1 * na2));
            if (ddt1 == NOTHING) na1 = 0;
            if (ddt2 == NOTHING) na2 = 0;
            sequentialAccesses = 0;
            randomAccesses = 0;
            averageSize = 0;
            readsCounter = 0;
            writesCounter = 0;
            sequentialAccesses += calculateSequentialAccesses(ddt0, na0);
            randomAccesses += calculateRandomAccesses(ddt0, na0);
            averageSize += calculateAverageSize(ddt0, na0, Te0);
            sequentialAccesses += calculateSequentialAccesses(ddt1, na0 * na1);
            randomAccesses += calculateRandomAccesses(ddt1, na0 * na1);
            averageSize += calculateAverageSize(ddt1, na0 * na1, Te1);
            sequentialAccesses += calculateSequentialAccesses(ddt2, na0 * na1 * na2);
            randomAccesses += calculateRandomAccesses(ddt2, na0 * na1 * na2);
            averageSize += calculateAverageSize(ddt2, na0 * na1 * na2, Te2);
            writesCounter = 1.0 * writes[i];
            readsCounter = 1.0 * reads[i];
            memoryAccesses += (sequentialAccesses + randomAccesses);
            memoryUsage += averageSize;
            energyConsumption += (0.96 - complexity) * (writesCounter + readsCounter) * 1e-4 + (0.04 + complexity) * (writesCounter + readsCounter) * 1e-2;
        }
        objectiveVector.set(0, memoryAccesses);
        objectiveVector.set(1, memoryUsage);
        objectiveVector.set(2, energyConsumption);
    }

    public static void main(String[] args) throws Exception {
        String outDir = ".";
        if (args.length < 5) {
            System.out.println("Usage:");
            System.out.println("java -jar ProfileChristosV2.jar <XML-PROFILE> <VEGA|SPEA2|NSGA2> <NumTrials:1..Inf> <NumOfIndividuals:1..Inf> <MaxGenerations:1..Inf> <UniformAnalysis:true|false>");
            args = new String[6];
            args[0] = "VDrift.1.pro";
            args[1] = "VEGA";
            args[2] = "1";
            args[3] = "200";
            args[4] = "5000";
            args[5] = "true";
            outDir = "D:/jlrisco/Trabajo/MisPapers/WorkingPapers/J SYST SOFTWARE/ResultsV2.0";
            return;
        }
        String proPath = outDir + File.separator + args[0];
        String algorithmName = args[1];
        Integer numberOfTrials = Integer.valueOf(args[2]);
        Integer numberOfIndividuals = Integer.valueOf(args[3]);
        Integer maxGenerations = Integer.valueOf(args[4]);
        Boolean uniformAnalysis = false;
        if (args.length > 5) uniformAnalysis = Boolean.valueOf(args[5]);
        ProfileChristosV2.initializeProblem(proPath);
        Moea algorithm = null;
        for (int i = 0; i < numberOfTrials; i++) {
            BufferedWriter loggerPop = new BufferedWriter(new FileWriter(new File(proPath + "." + algorithmName + "." + i)));
            System.out.println("Iteration number: " + i);
            Population<Chromosome> popIni = new Population<Chromosome>();
            for (int k = 0; k < numberOfIndividuals; ++k) {
                ProfileChristosV2 ind = new ProfileChristosV2();
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
            System.out.println("Time: " + (end - start) / 1000);
            System.out.println("done.");
            loggerPop.write(PopulationToString(algorithm.getPopulation()));
            loggerPop.flush();
            loggerPop.close();
        }
        if (uniformAnalysis) saveUniformObjectives(proPath);
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
        res = Math.sqrt(res / list.size());
        return res;
    }

    public static String ParetoFrontToString(Population<Chromosome> pop) {
        StringBuffer buffer = new StringBuffer();
        Population<Chromosome> popTemp = new Population<Chromosome>();
        popTemp.add(pop);
        popTemp.keepNonDominated();
        for (Chromosome chrom : popTemp) {
            buffer.append(chrom.toString() + "\t");
            buffer.append("\n");
        }
        return buffer.toString();
    }

    public static String PopulationToString(Population<Chromosome> pop) {
        StringBuffer buffer = new StringBuffer();
        Population<Chromosome> popTemp = new Population<Chromosome>();
        popTemp.add(pop);
        for (Chromosome chrom : popTemp) {
            buffer.append(chrom.toString() + "\t");
            buffer.append("\n");
        }
        return buffer.toString();
    }

    public static void saveUniformObjectives(String proPath) throws Exception {
        StringBuffer buffer = new StringBuffer();
        BufferedWriter out = new BufferedWriter(new FileWriter(new File(proPath + ".out")));
        Value[] xTemp = new Value[M];
        ProfileChristosV2 temp = null;
        for (int d = AR; d < ProfileChristosV2.ddts.length; d++) {
            for (int i = 0; i < M / 5; i++) {
                xTemp[5 * i + 0] = new ValInt(d);
                xTemp[5 * i + 1] = new ValInt(NOTHING);
                xTemp[5 * i + 2] = new ValInt(0);
                xTemp[5 * i + 3] = new ValInt(NOTHING);
                xTemp[5 * i + 4] = new ValInt(0);
            }
            temp = new ProfileChristosV2(xTemp);
            temp.evaluate();
            buffer.append(temp.toString() + "\n");
        }
        if (proPath.indexOf("SimBlob") > -1 || proPath.indexOf("Physics") > -1) {
            for (int i = 0; i < M / 5; i++) {
                xTemp[5 * i + 0].setValue(AR);
                xTemp[5 * i + 1].setValue(NOTHING);
                xTemp[5 * i + 2].setValue(0);
                xTemp[5 * i + 3].setValue(NOTHING);
                xTemp[5 * i + 4].setValue(0);
            }
            temp = new ProfileChristosV2(xTemp);
            temp.evaluate();
            buffer.append(temp.toString() + "\n");
        } else if (proPath.indexOf("VDrift") > -1) {
            for (int i = 0; i < M / 5; i++) {
                if (i != 2) xTemp[5 * i + 0].setValue(AR); else xTemp[5 * i + 0].setValue(DLL);
                xTemp[5 * i + 1].setValue(NOTHING);
                xTemp[5 * i + 2].setValue(0);
                xTemp[5 * i + 3].setValue(NOTHING);
                xTemp[5 * i + 4].setValue(0);
            }
            temp = new ProfileChristosV2(xTemp);
            temp.evaluate();
            buffer.append(temp.toString() + "\n");
        }
        out.write(buffer.toString());
        out.flush();
        out.close();
    }

    private double calculateSequentialAccesses(int ddt, double ne) {
        if (ddt == AR) return 9.0 * ne; else if (ddt == ARP) return 10 * ne; else if (ddt == SLL) return 7 * ne; else if (ddt == DLL) return 7 * ne; else if (ddt == SLLO) return 10 * ne; else if (ddt == DLLO) return 10 * ne;
        return 0.0;
    }

    private double calculateRandomAccesses(int ddt, double ne) {
        if (ddt == AR) return 2.0; else if (ddt == ARP) return 3.0; else if (ddt == SLL) return (1.0 * ne) / 2.0 + 1.0; else if (ddt == DLL) return (1.0 * ne) / 4.0 + 1.0; else if (ddt == SLLO) return (1.0 * ne) / 3.0 + 1.0; else if (ddt == DLLO) return (1.0 * ne) / 6.0 + 1.0;
        return 0.0;
    }

    private double calculateAverageSize(int ddt, double ne, int te) {
        if (ddt == AR) return 19.0 * Tref + 1.0 * ne * te; else if (ddt == ARP) return 19.0 * Tref + 1.0 * ne * (Tref + te); else if (ddt == SLL) return 19.0 * Tref + 1.0 * ne * (2.0 * Tref + te); else if (ddt == DLL) return 19.0 * Tref + 1.0 * ne * (3.0 * Tref + te); else if (ddt == SLLO) return 20.0 * Tref + 1.0 * ne * (2.0 * Tref + te); else if (ddt == DLLO) return 20.0 * Tref + 1.0 * ne * (3.0 * Tref + te);
        return 0.0;
    }

    private double calculateComplexity(int ddt0, int ddt1, int ddt2, int varIndex) {
        double complexity = 0.0;
        if (ddt0 == NOTHING) complexity += 0.0; else if (ddt0 == AR || ddt0 == ARP) complexity += NeSd[varIndex]; else complexity += (1.0) / NeSd[varIndex];
        if (ddt1 == NOTHING) complexity += 0.0; else if (ddt1 == AR || ddt1 == ARP) complexity += NeSd[varIndex]; else complexity += (1.0) / NeSd[varIndex];
        if (ddt2 == NOTHING) complexity += 0.0; else if (ddt2 == AR || ddt2 == ARP) complexity += NeSd[varIndex]; else complexity += (1.0) / NeSd[varIndex];
        complexity = complexity / (1000.0 + NeSd[varIndex]);
        return complexity;
    }

    public String toString() {
        StringBuffer result = new StringBuffer();
        ObjectiveVector objs = this.getObjectiveVector();
        for (Double value : objs) result.append(value + "\t");
        for (int j = 0; j < M / 5; j++) {
            int ddt0 = x[5 * j + 0].intValue();
            int ddt1 = x[5 * j + 1].intValue();
            int ddt2 = x[5 * j + 3].intValue();
            result.append(ddt0 + "" + ddt1 + "" + ddt2 + "\t");
        }
        return result.toString();
    }

    public String toPrettyFormat(int j) {
        StringBuffer result = new StringBuffer();
        int ddt0, ddt1, ddt2;
        ddt0 = x[5 * j + 0].intValue();
        ddt1 = x[5 * j + 1].intValue();
        ddt2 = x[5 * j + 3].intValue();
        result.append("\'" + ddts[ddt0]);
        if (ddt1 != NOTHING) result.append("(" + ddts[ddt1] + "[" + x[5 * j + 2].toString() + "]");
        if (ddt2 != NOTHING) result.append("(" + ddts[ddt2] + "[" + x[5 * j + 4].toString() + "]");
        if (ddt1 != NOTHING) result.append(")");
        if (ddt2 != NOTHING) result.append(")");
        result.append(" - " + Ne[j] + "\'");
        result.append("\t");
        return result.toString();
    }
}
