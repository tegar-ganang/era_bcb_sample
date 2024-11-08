package moea.moga.examples;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import moea.commons.ObjectiveVector;
import moea.commons.Population;
import moea.moga.genome.Chromosome;

public class Analysis extends Chromosome {

    private static final long serialVersionUID = 1L;

    public static void initialize(int nObjs) {
        N = nObjs;
        M = 0;
    }

    public Analysis() {
        super();
    }

    public Analysis(Analysis src) {
        super(src);
    }

    public Analysis clone() {
        Analysis clone = new Analysis(this);
        return clone;
    }

    public void evaluate() {
    }

    public static void main(String[] args) throws Exception {
        String outDir = ".";
        if (args.length < 5) {
            System.out.println("Usage:");
            System.out.println("java -jar Analysis.jar <BASENAME> <NumObjectives:1..Inf> <NumTrials:1..Inf>");
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
        BufferedWriter loggerMetrics = new BufferedWriter(new FileWriter(new File(proPath + "." + algorithmName + ".MetricsAvg")));
        ArrayList<Double> spreads = new ArrayList<Double>();
        ArrayList<Double> spacings = new ArrayList<Double>();
        for (int i = 0; i < numberOfTrials; i++) {
            System.out.println("Checking iteration number: " + i);
            Population<Chromosome> popIni = new Population<Chromosome>();
            for (int k = 0; k < numberOfIndividuals; ++k) {
                Analysis ind = new Analysis();
                popIni.add(ind);
            }
            spreads.add(popIni.calculateSpread());
            spacings.add(popIni.calculateSpacing());
            System.out.println("Spread: " + spreads.get(spreads.size() - 1));
            System.out.println("Spacing: " + spacings.get(spacings.size() - 1));
            System.out.println("done.");
        }
        double spreadMean = calculateMean(spreads);
        double spacingMean = calculateMean(spacings);
        double spreadStd = calculateStd(spreads, spreadMean);
        double spacingStd = calculateStd(spacings, spacingMean);
        loggerMetrics.write("Spread(Mean,Std):\t" + spreadMean + "\t" + spreadStd + "\n");
        loggerMetrics.write("Spacing(Mean,Std):\t" + spacingMean + "\t" + spacingStd + "\n");
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
        res = Math.sqrt(res / list.size());
        return res;
    }

    public static String ParetoFrontToString(Population<Chromosome> pop) {
        StringBuffer buffer = new StringBuffer();
        Population<Chromosome> popTemp = new Population<Chromosome>();
        popTemp.add(pop);
        popTemp.keepNonDominated();
        for (Chromosome chrom : popTemp) {
            ObjectiveVector objs = chrom.getObjectiveVector();
            for (int i = 0; i < Analysis.N; ++i) buffer.append(objs.get(i) + "\t");
            buffer.append("\n");
        }
        return buffer.toString();
    }
}
