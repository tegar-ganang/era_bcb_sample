package jeco.lib.problems.ddts;

import jeco.kernel.problem.Problem;
import jeco.kernel.problem.Solution;
import jeco.kernel.problem.Solutions;
import jeco.kernel.problem.Variable;
import jeco.kernel.util.RandomGenerator;
import jeco.kernel.util.Xml;
import org.w3c.dom.Element;

public class DynamicDataTypes extends Problem<Variable<Integer>> {

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

    private double cacheAccessTime;

    private double dramAccessTime;

    private double cacheLineSize;

    private double dramBandwith;

    private double cpuPower;

    private double cacheAccessEnergy;

    private double dramAccessPower;

    private long[] Ne;

    private double[] NeSd;

    private double[] Nve;

    private int Na;

    private double[] Nn;

    private int[] Te;

    private int Tref;

    private long[][] readsPerDdt;

    private long[][] writesPerDdt;

    private long[] readsPlusWritesPerDdt;

    private double[][] cacheMissesL1;

    public DynamicDataTypes(String name, String profilePath, double cacheAccessTime, double dramAccessTime, double cacheLineSize, double dramBandwith, double cpuPower, double cacheAccessEnergy, double dramAccessPower) {
        super(name, 0, 3);
        this.cacheAccessTime = cacheAccessTime;
        this.dramAccessTime = dramAccessTime;
        this.cacheLineSize = cacheLineSize;
        this.dramBandwith = dramBandwith;
        this.cpuPower = cpuPower;
        this.cacheAccessEnergy = cacheAccessEnergy;
        this.dramAccessPower = dramAccessPower;
        this.initializeProblem(profilePath);
        lowerBound = new double[numberOfVariables];
        upperBound = new double[numberOfVariables];
        for (int i = 0; i < numberOfVariables; i++) {
            lowerBound[i] = AR;
            upperBound[i] = DLLARO + 1;
        }
    }

    public DynamicDataTypes(String name, String profilePath) {
        this(name, profilePath, 0.0000001, 0.000001, 32, 50000000, 0.019, 0.0001, 0.001);
    }

    public void newRandomSetOfSolutions(Solutions<Variable<Integer>> solutions) {
        Solution<Variable<Integer>> solution = null;
        for (int i = 0; i < solutions.size(); ++i) {
            solution = solutions.get(i);
            for (int j = 0; j < numberOfVariables; ++j) {
                solution.getVariables().get(j).setValue(RandomGenerator.nextInteger((int) lowerBound[j], (int) upperBound[j]));
            }
        }
    }

    public void evaluate(Solution<Variable<Integer>> solution) {
        for (int i = 0; i < 3; i++) {
            solution.getObjectives().set(i, 0.0);
        }
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
        for (int i = 0; i < numberOfVariables; i++) {
            Variable<Integer> variable = solution.getVariables().get(i);
            int ddt = variable.getValue();
            randomCount = 0;
            secuentialCount = 0;
            averageSize = 0;
            numCreations = 0;
            reads = readsPerDdt[i][ddt];
            writes = writesPerDdt[i][ddt];
            execTime = 0;
            if (ddt == DynamicDataTypes.AR) {
                secuentialCount = 9 * Ne[i];
                randomCount = 2;
                averageSize = 19 * Tref + Ne[i] * Te[i];
                numCreations = 1;
            } else if (ddt == DynamicDataTypes.ARP) {
                secuentialCount = 10 * Ne[i];
                randomCount = 3;
                averageSize = 19 * Tref + Ne[i] * (Tref + Te[i]);
                numCreations = 1 + Nve[i];
            } else if (ddt == DynamicDataTypes.SLL) {
                secuentialCount = 7 * Ne[i];
                randomCount = Ne[i] / 2 + 1;
                averageSize = 19 * Tref + Ne[i] * (2 * Tref + Te[i]);
                numCreations = 1 + Nve[i];
            } else if (ddt == DynamicDataTypes.DLL) {
                secuentialCount = 7 * Ne[i];
                randomCount = Ne[i] / 4 + 1;
                averageSize = 19 * Tref + Ne[i] * (3 * Tref + Te[i]);
                numCreations = 1 + Nve[i];
            } else if (ddt == DynamicDataTypes.SLLO) {
                secuentialCount = 10 * Ne[i];
                randomCount = Ne[i] / 3 + 1;
                averageSize = 20 * Tref + Ne[i] * (2 * Tref + Te[i]);
                numCreations = 1 + Nve[i];
            } else if (ddt == DynamicDataTypes.DLLO) {
                secuentialCount = 10 * Ne[i];
                randomCount = Ne[i] / 6 + 1;
                averageSize = 20 * Tref + Ne[i] * (3 * Tref + Te[i]);
                numCreations = 1 + Nve[i];
            } else if (ddt == DynamicDataTypes.SLLAR) {
                secuentialCount = 18 * Ne[i] + 8 * Na;
                randomCount = Nn[i] / 2 + 1;
                averageSize = 21 * Tref + Nn[i] * (21 * Tref + Ne[i] * (Te[i] + Tref));
                numCreations = 2 * Ne[i] + Nve[i];
            } else if (ddt == DynamicDataTypes.DLLAR) {
                secuentialCount = 18 * Ne[i] + 8 * Na;
                randomCount = Nn[i] / 4 + 1;
                averageSize = 21 * Tref + Nn[i] * (22 * Tref + Ne[i] * (Te[i] + Tref));
                numCreations = 2 * Ne[i] + Nve[i];
            } else if (ddt == DynamicDataTypes.SLLARO) {
                secuentialCount = 18 * Ne[i] + 10 * Na;
                randomCount = Nn[i] / 3 + 1;
                averageSize = 22 * Tref + Nn[i] * (21 * Tref + Ne[i] * (Te[i] + Tref));
                numCreations = 2 * Ne[i] + Nve[i];
            } else if (ddt == DynamicDataTypes.DLLARO) {
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
        solution.getObjectives().set(0, performance);
        solution.getObjectives().set(1, memory);
        solution.getObjectives().set(2, energy);
    }

    private void initializeProblem(String proPath) {
        try {
            Element xmlProfileAnalyzer = Xml.getTree("ProfileAnalyzer", proPath);
            super.numberOfVariables = Xml.loadScalarAsInteger(xmlProfileAnalyzer, "VarCount");
            Tref = Xml.loadScalarAsInteger(xmlProfileAnalyzer, "Tref");
            Na = Xml.loadScalarAsInteger(xmlProfileAnalyzer, "Na");
            Ne = Xml.loadArrayAsLong(xmlProfileAnalyzer, "Ne");
            NeSd = Xml.loadArrayAsDouble(xmlProfileAnalyzer, "NeSd");
            Nve = Xml.loadArrayAsDouble(xmlProfileAnalyzer, "Nve");
            Nn = Xml.loadArrayAsDouble(xmlProfileAnalyzer, "Nn");
            Te = Xml.loadArrayAsInteger(xmlProfileAnalyzer, "Te");
            readsPerDdt = Xml.loadMatrixAsLong(xmlProfileAnalyzer, "ReadsPerDdt");
            writesPerDdt = Xml.loadMatrixAsLong(xmlProfileAnalyzer, "WritesPerDdt");
            updateCacheMisses();
        } catch (Exception ee) {
            ee.printStackTrace();
        }
    }

    private void updateCacheMisses() {
        readsPlusWritesPerDdt = new long[DynamicDataTypes.ddts.length];
        for (int i = 0; i < DynamicDataTypes.ddts.length; ++i) {
            readsPlusWritesPerDdt[i] = 0;
        }
        for (int i = 0; i < DynamicDataTypes.ddts.length; ++i) {
            for (int j = 0; j < numberOfVariables; ++j) {
                readsPlusWritesPerDdt[i] += (readsPerDdt[j][i] + writesPerDdt[j][i]);
            }
        }
        cacheMissesL1 = new double[numberOfVariables][DynamicDataTypes.ddts.length];
        for (int i = 0; i < numberOfVariables; ++i) {
            for (int j = 0; j < DynamicDataTypes.ddts.length; ++j) {
                cacheMissesL1[i][j] = (1.0 * NeSd[i] * (readsPlusWritesPerDdt[j])) / (readsPlusWritesPerDdt[j] + readsPerDdt[i][j] + writesPerDdt[i][j]);
            }
        }
    }

    @Override
    public Variable<Integer> newVariable() {
        return new Variable<Integer>(0);
    }

    @Override
    public Problem<Variable<Integer>> clone() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
