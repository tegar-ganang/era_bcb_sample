package edu.utah.seq.analysis.multi;

import java.io.*;
import java.util.regex.*;
import java.util.*;
import net.sf.samtools.*;
import util.bio.annotation.Bed;
import util.bio.annotation.ExonIntron;
import util.bio.parsers.*;
import util.gen.*;
import edu.utah.seq.analysis.OverdispersedRegionScanSeqs;
import edu.utah.seq.data.*;
import util.bio.cluster.*;
import org.apache.commons.math.stat.inference.ChiSquareTestImpl;
import com.sun.tools.javac.code.Attribute.Array;

/**
 * @author Nix
 * */
public class MultipleConditionRNASeq {

    private File[] conditionDirectories;

    private File saveDirectory;

    private File fullPathToR = new File("/usr/bin/R");

    private File bedFile;

    private File refSeqFile;

    private String genomeVersion;

    private float minFDR = 10;

    private float minLog2Ratio = 1f;

    private boolean removeOverlappingRegions = true;

    private int minimumCounts = 20;

    private boolean deleteTempFiles = true;

    private boolean filterOutliers = false;

    private UCSCGeneLine[] genes;

    private Condition[] conditions;

    private HashSet<String> geneNamesPassingThresholds = new HashSet<String>();

    private HashSet<String> geneNamesWithMinimumCounts = new HashSet<String>();

    private HashMap<String, SAMFileReader> samReaders = new HashMap<String, SAMFileReader>();

    private File serializedConditons = null;

    private String url;

    private UCSCGeneLine[] workingGenesToTest = null;

    private ArrayList<String> workingTNs = null;

    private ArrayList<String> workingConditionNames = null;

    private int workingNumberScores = 3;

    private String workingName = null;

    private File workingCountTable = null;

    private File[] workingDESeqFiles = null;

    private int workingGenesPassingFilters = 0;

    private StringBuilder spreadSheetHeader = null;

    private int workingNumberFirstReplicas;

    private int workingNumberSecondReplicas;

    /**Stand alone.*/
    public MultipleConditionRNASeq(String[] args) {
        long startTime = System.currentTimeMillis();
        processArgs(args);
        run();
        double diffTime = ((double) (System.currentTimeMillis() - startTime)) / 60000;
        System.out.println("\nDone! " + Math.round(diffTime) + " minutes\n");
    }

    public void run() {
        System.out.println("Loading regions/ gene models...");
        loadGeneModels();
        loadConditions();
        if (serializedConditons.exists() == false) {
            System.out.print("Collecting counts for each gene exon/ region (very slow using Picard query lookup)");
            scanGenes();
            System.out.println(" saving counts\n");
            IO.saveObject(serializedConditons, conditions);
        }
        for (SAMFileReader s : samReaders.values()) s.close();
        System.out.println("Running pairwise DESeq analysis to identify differentially expressed genes under any conditions...");
        analyzeForDifferentialExpression();
        System.out.println("\n\t" + geneNamesPassingThresholds.size() + "\tgenes differentially expressed (FDR " + Num.formatNumber(minFDR, 2) + ", log2Ratio " + Num.formatNumber(minLog2Ratio, 2) + ")\n");
        if (geneNamesPassingThresholds.size() > 5000 && minFDR <= 13) System.out.println("WARNING: too many differentially expressed genes?! This may freeze the R clustering. Consider more stringent thresholds (e.g. FDR of 20 or 30)\n");
        Arrays.sort(genes, new UCSCGeneLineComparatorLog2Ratio());
        Arrays.sort(genes, geneNamesPassingThresholds.size(), genes.length, new UCSCGeneLineComparatorPValue());
        System.out.println("Clustering genes and samples...");
        clusterDifferentiallyExpressedGenes();
        printStatSpreadSheet();
    }

    private void loadConditions() {
        serializedConditons = new File(saveDirectory, "conditions.ser");
        if (serializedConditons.exists()) {
            conditions = (Condition[]) IO.fetchObject(serializedConditons);
            System.out.println("\nWARNING: Loading " + conditions.length + " conditions from file, delete " + serializedConditons + " if you'd like to recount gene exon/ regions....");
            loadMinimumCountsHash();
        } else {
            conditions = new Condition[conditionDirectories.length];
            for (int i = 0; i < conditionDirectories.length; i++) {
                conditions[i] = new Condition(conditionDirectories[i]);
                for (Replica r : conditions[i].getReplicas()) {
                    SAMFileReader sfr = new SAMFileReader(r.getBamFile());
                    samReaders.put(r.getNameNumber(), sfr);
                }
            }
        }
        System.out.println("\nConditions and replicas:");
        for (Condition c : conditions) {
            System.out.println(c);
        }
        if (conditions.length < 2) Misc.printErrAndExit("\nError: must provide at least two Conditions for analysis.\n");
    }

    private void clusterDifferentiallyExpressedGenes() {
        File countTable = new File(saveDirectory, "geneCountTable.txt");
        String[] allGenes = new String[genes.length];
        for (int i = 0; i < genes.length; i++) {
            allGenes[i] = genes[i].getName();
        }
        ArrayList<String> conditionNames = writeCountTable(allGenes, countTable);
        File varCorrData = executeDESeqCluster(countTable, conditionNames);
        appendCountVarCorrData(countTable, varCorrData, conditionNames);
    }

    public void appendCountVarCorrData(File countTable, File varCorrData, ArrayList<String> conditionNames) {
        spreadSheetHeader.append("\t\t");
        spreadSheetHeader.append("Counts_" + Misc.stringArrayListToString(conditionNames, "\tCounts_"));
        spreadSheetHeader.append("\t\t");
        spreadSheetHeader.append("VarCorCounts_" + Misc.stringArrayListToString(conditionNames, "\tVarCorCounts_"));
        spreadSheetHeader.append("\t\t");
        spreadSheetHeader.append("FPKM_" + Misc.stringArrayListToString(conditionNames, "\tFPKM_"));
        try {
            BufferedReader counts = new BufferedReader(new FileReader(countTable));
            counts.readLine();
            BufferedReader varCounts = new BufferedReader(new FileReader(varCorrData));
            for (int i = 0; i < genes.length; i++) {
                StringBuilder sb = genes[i].getText();
                sb.append("\t");
                String line = counts.readLine();
                int index = line.indexOf("\t");
                sb.append(line.substring(index));
                sb.append("\t\t");
                sb.append(varCounts.readLine());
                sb.append("\t\t");
                for (Condition c : conditions) {
                    for (Replica r : c.getReplicas()) {
                        double num = 0;
                        if (r.getGeneCounts().get(genes[i].getName()) != null) num = r.getGeneCounts().get(genes[i].getName()).calculateFPKM(r.getTotalCounts(), genes[i].getTotalExonicBasePairs());
                        sb.append(num);
                        sb.append("\t");
                    }
                }
            }
            counts.close();
            varCounts.close();
        } catch (IOException e) {
            e.printStackTrace();
            Misc.printErrAndExit("\nError parsing counts and varCorr counts.\n");
        }
    }

    public File executeDESeqCluster(File countTable, ArrayList<String> conditionNames) {
        int numDiffExp = geneNamesPassingThresholds.size();
        File varCorrData = new File(saveDirectory, "allGene_DESeqVarCorrData.txt");
        File clusteredDiffExpressGenes = new File(saveDirectory, "clusterPlot" + numDiffExp + "DiffExpGenes.pdf");
        File sampleClustering = new File(saveDirectory, "clusterPlotSamples.pdf");
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("numDiffExpGenes = " + numDiffExp + "\n");
            sb.append("library(DESeq)\n");
            sb.append("countsTable = read.delim('" + countTable.getCanonicalPath() + "', header=TRUE)\n");
            sb.append("rownames(countsTable) = countsTable[,1]\n");
            sb.append("countsTable = countsTable[,-1]\n");
            sb.append("conds = c('" + Misc.stringArrayListToString(conditionNames, "','") + "')\n");
            sb.append("cds = newCountDataSet( countsTable, conds)\n");
            sb.append("cds = estimateSizeFactors( cds )\n");
            sb.append("cds = estimateDispersions( cds, method='blind', sharingMode='fit-only' )\n");
            sb.append("vsd = getVarianceStabilizedData( cds )\n");
            sb.append("write.table(vsd, file = '" + varCorrData.getCanonicalPath() + "', quote=FALSE, sep ='\t', row.names = FALSE, col.names = FALSE)\n");
            if (numDiffExp != 0) {
                sb.append("colors = colorRampPalette(c('white','darkblue'))(100)\n");
                sb.append("pdf('" + clusteredDiffExpressGenes.getCanonicalPath() + "', height=10, width=10)\n");
                sb.append("heatmap( vsd[1:numDiffExpGenes,], col=colors, scale='none')\n");
                sb.append("dev.off()\n");
            }
            sb.append("dists = dist( t( vsd ) )\n");
            sb.append("pdf('" + sampleClustering.getCanonicalPath() + "', height=10, width=10)\n");
            sb.append("heatmap( as.matrix( dists ), symm=TRUE, scale='none', margins=c(10,10), col = colorRampPalette(c('darkblue','white'))(100), labRow = paste( pData(cds)$condition, pData(cds)$type ) )\n");
            sb.append("dev.off()\n");
            File scriptFile = new File(saveDirectory, "allGene_RScript.txt");
            File rOut = new File(saveDirectory, "allGene_RScript.txt.Rout");
            IO.writeString(sb.toString(), scriptFile);
            String[] command = new String[] { fullPathToR.getCanonicalPath(), "CMD", "BATCH", "--no-save", "--no-restore", scriptFile.getCanonicalPath(), rOut.getCanonicalPath() };
            IO.executeCommandLine(command);
            if (varCorrData.exists() == false || varCorrData.exists() == false) throw new IOException("\nR varCorr results file doesn't exist. Check temp files in save directory for error.\n");
            if (deleteTempFiles) {
                rOut.deleteOnExit();
                countTable.deleteOnExit();
                scriptFile.deleteOnExit();
                varCorrData.deleteOnExit();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Misc.printErrAndExit("\nError executing var corr bulk DESeq.\n");
        }
        return varCorrData;
    }

    public void analyzeForDifferentialExpression() {
        for (int i = 0; i < conditions.length; i++) {
            Condition first = conditions[i];
            for (int j = i + 1; j < conditions.length; j++) {
                Condition second = conditions[j];
                differentialExpress(first, second);
            }
        }
    }

    public void differentialExpress(Condition first, Condition second) {
        System.out.print("\tComparing " + first.getName() + " vs " + second.getName());
        workingName = first.getName() + "_" + second.getName();
        workingNumberFirstReplicas = first.getReplicas().length;
        workingNumberSecondReplicas = second.getReplicas().length;
        if (writePairedCountTable(first, second) == false) return;
        executeDESeqDiffExp(false);
        if (workingDESeqFiles == null) return;
        if (parseDESeqStatResults() == false) return;
        System.out.println(" : " + workingGenesPassingFilters);
        if (spreadSheetHeader == null) saveFirstWorkingGeneModels(); else saveWorkingGeneModels();
    }

    /**Prints a spread sheet of counts from all of the genes*/
    public void printCountSpreadSheet() {
        File f = new File(saveDirectory, "geneCounts.xls");
        String[] allGenes = new String[genes.length];
        for (int i = 0; i < genes.length; i++) {
            allGenes[i] = genes[i].getName();
        }
        writeCountTable(allGenes, f);
    }

    /**Prints a spread sheet from all of the genes sorted by max log2 ratio for those found differentially expressed.*/
    public void printStatSpreadSheet() {
        try {
            File f = new File(saveDirectory, "geneStats.xls");
            PrintWriter spreadSheetOut = new PrintWriter(new FileWriter(f));
            spreadSheetOut.println(spreadSheetHeader.toString() + "\t\tGenomeVersion=" + genomeVersion);
            boolean printBlank = true;
            for (UCSCGeneLine gene : genes) {
                if (printBlank && gene.getLog2Ratio() == 0f) {
                    printBlank = false;
                    spreadSheetOut.println();
                }
                spreadSheetOut.println(gene.getText());
            }
            spreadSheetOut.close();
        } catch (Exception e) {
            System.err.println("\nProblem printing gene models.");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void saveFirstWorkingGeneModels() {
        spreadSheetHeader = new StringBuilder();
        if (workingGenesToTest[0].getDisplayName() != null) spreadSheetHeader.append("#DisplayName\tName\t"); else spreadSheetHeader.append("#Name\t");
        spreadSheetHeader.append("Chr\tStrand\tStart\tStop\tTotalBPs\t\t" + workingName + "_PVal\t" + workingName + "_FDR\t" + workingName + "_Lg2Rto");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < workingNumberScores; i++) sb.append("\t");
        String tabs = sb.toString();
        for (int i = 0; i < genes.length; i++) {
            StringBuilder text = new StringBuilder();
            String name;
            if (genes[i].getDisplayName() != null) name = genes[i].getDisplayName(); else name = genes[i].getName();
            int start = genes[i].getTxStart() - 10000;
            if (start < 0) start = 0;
            int end = genes[i].getTxEnd() + 10000;
            text.append(url);
            text.append(genes[i].getChrom());
            text.append("&start=");
            text.append(start);
            text.append("&end=");
            text.append(end);
            text.append("\",\"");
            text.append(name);
            text.append("\")\t");
            if (genes[i].getDisplayName() != null) {
                text.append(genes[i].getName());
                text.append("\t");
            }
            text.append(genes[i].coordinates());
            text.append("\t");
            text.append(genes[i].getTotalExonicBasePairs());
            text.append("\t");
            float[] s = genes[i].getScores();
            if (s == null) text.append(tabs); else {
                text.append("\t");
                text.append(Misc.floatArrayToString(s, "\t"));
            }
            genes[i].setText(text);
        }
    }

    public void saveWorkingGeneModels() {
        spreadSheetHeader.append("\t\t" + workingName + "_PVal\t" + workingName + "_FDR\t" + workingName + "_Lg2Rto");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < workingNumberScores; i++) sb.append("\t");
        String tabs = sb.toString();
        for (int i = 0; i < genes.length; i++) {
            StringBuilder text = genes[i].getText();
            text.append("\t");
            float[] s = genes[i].getScores();
            if (s == null) text.append(tabs); else {
                text.append("\t");
                text.append(Misc.floatArrayToString(s, "\t"));
            }
        }
    }

    private boolean parseDESeqStatResults() {
        try {
            BufferedReader inStats = new BufferedReader(new FileReader(workingDESeqFiles[0]));
            BufferedReader inVarCorr = new BufferedReader(new FileReader(workingDESeqFiles[1]));
            String line;
            String[] stats;
            String[] varCorr;
            Pattern tab = Pattern.compile("\t");
            float maxPVal = 0;
            float maxAdjPVal = 0;
            workingNumberScores = 3;
            int totalReps = workingNumberFirstReplicas + workingNumberSecondReplicas;
            for (int x = 0; x < workingGenesToTest.length; x++) {
                line = inStats.readLine();
                stats = tab.split(line);
                if (stats.length != 3) Misc.printErrAndExit("One of the DESeq stats R results rows is malformed -> " + line);
                float[] scores = new float[stats.length];
                for (int i = 0; i < stats.length; i++) {
                    if (stats[i].equals("Inf") || stats[i].equals("NA")) scores[i] = Float.MIN_VALUE; else scores[i] = Float.parseFloat(stats[i]);
                }
                if (scores[0] > maxPVal) maxPVal = scores[0];
                if (scores[1] > maxAdjPVal) maxAdjPVal = scores[1];
                if (totalReps > 2) {
                    line = inVarCorr.readLine();
                    varCorr = tab.split(line);
                    if (varCorr.length != totalReps) Misc.printErrAndExit("One of the DESeq varCorr R results rows is malformed -> " + line);
                    scores[2] = calculateDifference(varCorr);
                }
                workingGenesToTest[x].setScores(scores);
            }
            inStats.close();
            inVarCorr.close();
            maxPVal = maxPVal * 1.01f;
            maxAdjPVal = maxAdjPVal * 1.01f;
            workingGenesPassingFilters = 0;
            for (int i = 0; i < workingGenesToTest.length; i++) {
                float[] scores = workingGenesToTest[i].getScores();
                if (scores[0] == Float.MIN_VALUE) scores[0] = maxPVal;
                if (workingGenesToTest[i].getpValue() < scores[0]) workingGenesToTest[i].setpValue(scores[0]);
                if (scores[1] == Float.MIN_VALUE) scores[1] = maxAdjPVal;
                float absLog2Ratio = Math.abs(scores[2]);
                if (scores[1] >= minFDR && absLog2Ratio >= minLog2Ratio) {
                    geneNamesPassingThresholds.add(workingGenesToTest[i].getName());
                    if (workingGenesToTest[i].getLog2Ratio() < absLog2Ratio) workingGenesToTest[i].setLog2Ratio(absLog2Ratio);
                    workingGenesPassingFilters++;
                }
            }
            if (deleteTempFiles) workingDESeqFiles[0].delete();
            return true;
        } catch (Exception e) {
            System.err.println("\nProblem parsing DESeq stats results from R.");
            e.printStackTrace();
            System.exit(1);
        }
        return false;
    }

    public float calculateDifference(String[] varCorr) {
        float[] t = new float[workingNumberFirstReplicas];
        float[] c = new float[workingNumberSecondReplicas];
        for (int i = 0; i < workingNumberFirstReplicas; i++) {
            t[i] = Float.parseFloat(varCorr[i]);
        }
        int index = 0;
        for (int i = workingNumberFirstReplicas; i < varCorr.length; i++) {
            c[index++] = Float.parseFloat(varCorr[i]);
        }
        if (varCorr.length < 7) {
            float diff = 0f;
            float absDiff = 100000000f;
            for (int i = 0; i < workingNumberFirstReplicas; i++) {
                for (int j = 0; j < workingNumberSecondReplicas; j++) {
                    float testDiff = t[i] - c[j];
                    float absTestDiff = Math.abs(testDiff);
                    if (absTestDiff < absDiff) {
                        absDiff = absTestDiff;
                        diff = testDiff;
                    }
                }
            }
            return diff;
        } else {
            return (float) (Num.pseudoMedian(t) - Num.pseudoMedian(c));
        }
    }

    public float parseFloat(String f) {
        if (f.equals("Inf") || f.equals("NA")) return Float.MIN_VALUE; else return Float.parseFloat(f);
    }

    public void executeDESeqDiffExp(boolean useLocalFitType) {
        File rResultsStats = new File(saveDirectory, workingName + "_DESeqResults.txt");
        File rResultsData = new File(saveDirectory, workingName + "_DESeqResultsData.txt");
        workingDESeqFiles = null;
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("library(DESeq)\n");
            sb.append("countsTable = read.delim('" + workingCountTable.getCanonicalPath() + "', header=TRUE)\n");
            sb.append("rownames(countsTable) = countsTable[,1]\n");
            sb.append("countsTable = countsTable[,-1]\n");
            sb.append("conds = c('" + Misc.stringArrayListToString(workingTNs, "','") + "')\n");
            sb.append("cds = newCountDataSet( countsTable, conds)\n");
            sb.append("cds = estimateSizeFactors( cds )\n");
            if (workingTNs.size() == 2) sb.append("cds = estimateDispersions( cds, method='blind', sharingMode='fit-only' )\n"); else {
                String outlier = "";
                String local = "";
                if (filterOutliers == false) outlier = ", sharingMode='fit-only'";
                if (useLocalFitType) local = ", fitType='local'";
                sb.append("cds = estimateDispersions( cds" + outlier + local + ")\n");
            }
            sb.append("res = nbinomTest( cds, 'N', 'T', pvals_only = FALSE)\n");
            sb.append("vsd = getVarianceStabilizedData( cds )\n");
            sb.append("res[,6] = (rowMeans( vsd[, conditions(cds)=='T', drop=FALSE] ) - rowMeans( vsd[, conditions(cds)=='N', drop=FALSE] ))\n");
            sb.append("res[,7] = -10 * log10(res[,7])\n");
            sb.append("res[,8] = -10 * log10(res[,8])\n");
            sb.append("res = res[,c(7,8,6)]\n");
            sb.append("write.table(res, file = '" + rResultsStats.getCanonicalPath() + "', quote=FALSE, sep ='\t', row.names = FALSE, col.names = FALSE)\n");
            sb.append("write.table(vsd, file = '" + rResultsData.getCanonicalPath() + "', quote=FALSE, sep ='\t', row.names = FALSE, col.names = FALSE)\n");
            File scriptFile = new File(saveDirectory, workingName + "_RScript.txt");
            File rOut = new File(saveDirectory, workingName + "_RScript.txt.Rout");
            IO.writeString(sb.toString(), scriptFile);
            String[] command = new String[] { fullPathToR.getCanonicalPath(), "CMD", "BATCH", "--no-save", "--no-restore", scriptFile.getCanonicalPath(), rOut.getCanonicalPath() };
            IO.executeCommandLine(command);
            if (rResultsStats.exists() == false) throw new IOException("\n\nR results file doesn't exist. Check temp files in save directory for error.\n");
            String[] res = IO.loadFile(rOut);
            for (String s : res) {
                if (s.contains("Dispersion fit did not converge")) {
                    System.err.println("\n\tWarning, DESeq's GLM dispersion fit failed to converge. Relaunching using fitType='local'");
                    executeDESeqDiffExp(true);
                    return;
                }
            }
            if (deleteTempFiles) {
                rOut.deleteOnExit();
                scriptFile.deleteOnExit();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Misc.printErrAndExit("Error: failed to execute DESeq.\n");
        }
        workingDESeqFiles = new File[] { rResultsStats, rResultsData };
    }

    public void setTNs(Condition first, Condition second) {
        workingTNs = new ArrayList<String>();
        workingConditionNames = new ArrayList<String>();
        for (Replica r : first.getReplicas()) {
            workingTNs.add("T");
            workingConditionNames.add(r.getNameNumber());
        }
        for (Replica r : second.getReplicas()) {
            workingTNs.add("N");
            workingConditionNames.add(r.getNameNumber());
        }
    }

    public boolean writePairedCountTable(Condition first, Condition second) {
        try {
            HashSet<String> genesToTest = new HashSet<String>();
            Condition[] twoConditions = new Condition[] { first, second };
            workingCountTable = new File(saveDirectory, "countTable_" + workingName + ".txt");
            if (deleteTempFiles) workingCountTable.deleteOnExit();
            PrintWriter out = new PrintWriter(new FileWriter(workingCountTable));
            out.print("GeneName");
            for (Condition c : twoConditions) {
                for (Replica r : c.getReplicas()) {
                    out.print("\t");
                    out.print(r.getNameNumber());
                    HashMap<String, GeneCount> geneCounts = r.getGeneCounts();
                    Iterator<String> it = geneCounts.keySet().iterator();
                    while (it.hasNext()) {
                        String geneName = it.next();
                        GeneCount gc = geneCounts.get(geneName);
                        if (gc != null && gc.getCount() > minimumCounts) {
                            genesToTest.add(geneName);
                        }
                    }
                }
            }
            out.println();
            if (genesToTest.size() == 0) {
                out.close();
                System.err.println("\nWARNING: no genes were found with minimum counts.  Skipping " + workingName);
                return false;
            }
            setTNs(first, second);
            workingGenesToTest = new UCSCGeneLine[genesToTest.size()];
            int index = 0;
            for (UCSCGeneLine gene : genes) {
                gene.setScores(null);
                if (genesToTest.contains(gene.getName())) {
                    workingGenesToTest[index] = gene;
                    index++;
                }
            }
            for (UCSCGeneLine gene : workingGenesToTest) {
                String geneName = gene.getName();
                out.print(geneName);
                for (Condition c : twoConditions) {
                    for (Replica r : c.getReplicas()) {
                        out.print("\t");
                        int count = 0;
                        if (r.getGeneCounts().get(geneName) != null) count = r.getGeneCounts().get(geneName).getCount();
                        out.print(count);
                    }
                }
                out.println();
            }
            out.close();
            return true;
        } catch (Exception e) {
            System.err.println("Problem writing out count table.");
            e.printStackTrace();
        }
        return false;
    }

    public ArrayList<String> writeCountTable(String[] genesNamesToWrite, File countTable) {
        ArrayList<String> conditionNames = new ArrayList<String>();
        try {
            PrintWriter out = new PrintWriter(new FileWriter(countTable));
            out.print("GeneName");
            for (Condition c : conditions) {
                for (Replica r : c.getReplicas()) {
                    out.print("\t");
                    out.print(r.getNameNumber());
                    conditionNames.add(r.getNameNumber());
                }
            }
            out.println();
            for (String geneName : genesNamesToWrite) {
                out.print(geneName);
                for (Condition c : conditions) {
                    for (Replica r : c.getReplicas()) {
                        out.print("\t");
                        int num = 0;
                        if (r.getGeneCounts().get(geneName) != null) num = r.getGeneCounts().get(geneName).getCount();
                        out.print(num);
                    }
                }
                out.println();
            }
            out.close();
        } catch (Exception e) {
            System.err.println("Problem writing out count table.");
            e.printStackTrace();
        }
        return conditionNames;
    }

    public void loadGeneModels() {
        UCSCGeneModelTableReader reader = null;
        if (refSeqFile != null) {
            reader = new UCSCGeneModelTableReader(refSeqFile, 0);
            if (removeOverlappingRegions) {
                System.out.print("\tRemoving overlapping regions from gene models");
                String deletedGenes = reader.removeOverlappingExons();
                int numDelGenes = deletedGenes.split(",").length;
                if (deletedGenes.length() != 0) {
                    File deleted = new File(saveDirectory, Misc.removeExtension(refSeqFile.getName()) + "_TrimmedDeletedGenes.txt");
                    System.out.println("\tWARNING: " + numDelGenes + " genes had more than 1/2 of their exonic bps removed. See " + deleted);
                    IO.writeString(deletedGenes, deleted);
                }
                File f = new File(saveDirectory, Misc.removeExtension(refSeqFile.getName()) + "_NoOverlappingExons.txt");
                System.out.println("\tWrote the trimmed gene table to the save directory. Use this table and the -o option to speed up subsequent processing.");
                reader.writeGeneTableToFile(f);
            }
            genes = reader.getGeneLines();
        } else if (bedFile != null) {
            Bed[] bed = Bed.parseFile(bedFile, 0, 0);
            genes = new UCSCGeneLine[bed.length];
            boolean addName = bed[0].getName().trim().equals("");
            for (int i = 0; i < bed.length; i++) {
                if (addName) bed[i].setName((i + 1) + "");
                genes[i] = new UCSCGeneLine(bed[i]);
            }
            reader = new UCSCGeneModelTableReader();
            reader.setGeneLines(genes);
        }
        if (genes == null || genes.length == 0) Misc.printExit("\nProblem loading your USCS gene model table or bed file? No genes/ regions?\n");
        if (reader.checkStartStopOrder() == false) Misc.printExit("\nOne of your regions's coordinates are reversed. Check that each start is less than the stop.\n");
        reader.replaceNameWithDisplayName();
    }

    public ArrayList<String> fetchAlignmentNames(String chromosome, ExonIntron ei, SAMFileReader reader) {
        ArrayList<String> al = new ArrayList<String>();
        SAMRecordIterator i = reader.queryOverlapping(chromosome, ei.getStart() + 1, ei.getEnd());
        while (i.hasNext()) al.add(i.next().getReadName());
        i.close();
        i = null;
        return al;
    }

    public ArrayList<String>[] fetchOverlappingAlignmentNames(String chromosome, ExonIntron[] ei, SAMFileReader reader) {
        ArrayList<String>[] als = new ArrayList[ei.length];
        for (int i = 0; i < ei.length; i++) als[i] = fetchAlignmentNames(chromosome, ei[i], reader);
        return als;
    }

    public int countUniqueNames(ArrayList<String>[] alignments) {
        HashSet<String> uniqueReadNames = new HashSet<String>();
        for (ArrayList<String> al : alignments) {
            for (String sam : al) uniqueReadNames.add(sam);
        }
        return uniqueReadNames.size();
    }

    /**Loades the geneNamesWithMinimumCounts hash with gene names that pass the minimumCounts array.
	 * No need to call if scanGenes called.*/
    private void loadMinimumCountsHash() {
        geneNamesWithMinimumCounts.clear();
        for (Condition c : conditions) {
            for (Replica r : c.getReplicas()) {
                HashMap<String, GeneCount> counts = r.getGeneCounts();
                for (String geneName : counts.keySet()) {
                    if (counts.get(geneName).getCount() >= minimumCounts) geneNamesWithMinimumCounts.add(geneName);
                }
            }
        }
    }

    /**Collects number of observations under each gene's exons.*/
    private void scanGenes() {
        geneNamesWithMinimumCounts.clear();
        int dotCounter = 0;
        HashSet<String> geneNames = new HashSet<String>();
        for (int i = 0; i < genes.length; i++) {
            if (++dotCounter > 500) {
                System.out.print(".");
                dotCounter = 0;
            }
            String geneName = genes[i].getName();
            if (geneNames.contains(geneName)) Misc.printErrAndExit("\nError: duplicate gene name found, aborting. See -> " + geneName);
            geneNames.add(geneName);
            String chromosome = genes[i].getChrom();
            ExonIntron[] exons = genes[i].getExons();
            for (Condition c : conditions) {
                for (Replica r : c.getReplicas()) {
                    ArrayList<String>[] names = fetchOverlappingAlignmentNames(chromosome, exons, samReaders.get(r.getNameNumber()));
                    int[] exonCounts = new int[exons.length];
                    boolean countsFound = false;
                    for (int x = 0; x < exons.length; x++) {
                        exonCounts[x] = names[x].size();
                        if (exonCounts[x] != 0) countsFound = true;
                    }
                    if (countsFound) {
                        int totalCount = countUniqueNames(names);
                        GeneCount tcg = new GeneCount(totalCount, exonCounts);
                        r.getGeneCounts().put(geneName, tcg);
                        if (totalCount >= minimumCounts) geneNamesWithMinimumCounts.add(geneName);
                        r.setTotalCounts(r.getTotalCounts() + totalCount);
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            printDocs();
            System.exit(0);
        }
        new MultipleConditionRNASeq(args);
    }

    /**This method will process each argument and assign new variables*/
    public void processArgs(String[] args) {
        Pattern pat = Pattern.compile("-[a-z]");
        System.out.println("\nArguments: " + Misc.stringArrayToString(args, " ") + "\n");
        for (int i = 0; i < args.length; i++) {
            String lcArg = args[i].toLowerCase();
            Matcher mat = pat.matcher(lcArg);
            if (mat.matches()) {
                char test = args[i].charAt(1);
                try {
                    switch(test) {
                        case 'c':
                            conditionDirectories = IO.extractOnlyDirectories(new File(args[++i]));
                            break;
                        case 's':
                            saveDirectory = new File(args[++i]);
                            break;
                        case 'r':
                            fullPathToR = new File(args[++i]);
                            break;
                        case 'u':
                            refSeqFile = new File(args[++i]);
                            break;
                        case 'b':
                            bedFile = new File(args[++i]);
                            break;
                        case 'f':
                            minFDR = Float.parseFloat(args[++i]);
                            break;
                        case 'l':
                            minLog2Ratio = Float.parseFloat(args[++i]);
                            break;
                        case 'e':
                            minimumCounts = Integer.parseInt(args[++i]);
                            break;
                        case 'o':
                            removeOverlappingRegions = false;
                            break;
                        case 't':
                            deleteTempFiles = false;
                            break;
                        case 'v':
                            filterOutliers = true;
                            break;
                        case 'g':
                            genomeVersion = args[++i];
                            break;
                        case 'h':
                            printDocs();
                            System.exit(0);
                        default:
                            Misc.printErrAndExit("\nProblem, unknown option! " + mat.group());
                    }
                } catch (Exception e) {
                    Misc.printErrAndExit("\nSorry, something doesn't look right with this parameter: -" + test + "\n");
                }
            }
        }
        if (genomeVersion == null) Misc.printErrAndExit("\nPlease provide a versioned genome (e.g. H_sapiens_Mar_2006).\n");
        url = "=HYPERLINK(\"http://localhost:7085/UnibrowControl?version=" + genomeVersion + "&seqid=";
        if (conditionDirectories == null || conditionDirectories.length == 0) Misc.printErrAndExit("\nError: cannot find any condition directories?\n");
        for (File dir : conditionDirectories) {
            File[] bamFiles = IO.extractFiles(dir, ".bam");
            OverdispersedRegionScanSeqs.lookForBaiIndexes(bamFiles);
        }
        if (saveDirectory == null) Misc.printErrAndExit("\nError: enter a directory text to save results.\n");
        saveDirectory.mkdir();
        if (fullPathToR == null || fullPathToR.canExecute() == false) {
            Misc.printErrAndExit("\nError: Cannot find or execute the R application -> " + fullPathToR + "\n");
        } else {
            String errors = IO.runRCommandLookForError("library(DESeq)", fullPathToR, saveDirectory);
            if (errors == null || errors.length() != 0) {
                Misc.printErrAndExit("\nError: Cannot find the required R library.  Did you install DESeq " + "(http://www-huber.embl.de/users/anders/DESeq/)?  See the author's websites for installation instructions. Once installed, " + "launch an R terminal and type 'library(DESeq)' to see if it is present. R error message:\n\t\t" + errors + "\n\n");
            }
        }
        if (estimateDispersions(fullPathToR, saveDirectory) == false) {
            Misc.printErrAndExit("\nError: Please upgrade DESeq to the latest version, see http://www-huber.embl.de/users/anders/DESeq/ \n");
        }
        if (refSeqFile == null && bedFile == null) {
            Misc.printErrAndExit("\nPlease enter a regions or gene file to use in scoring regions.\n");
        }
    }

    /**Returns true if DESeq uses the estimateDispersions function otherwise false.*/
    public static boolean estimateDispersions(File fullPathToR, File tempDir) {
        String error = IO.runRCommandLookForError("library(DESeq); estimateDispersions(5);", fullPathToR, tempDir);
        if (error.contains("could not find function")) {
            System.err.println("\nWARNING: You have installed an obsolete version of DESeq. Update R, Bioconductor, and DESeq to latest versions. Key changes have been implemented to control for variance outliers. Don't use this old version!\n");
            return false;
        }
        return true;
    }

    public static void printDocs() {
        System.out.println("\n" + "**************************************************************************************\n" + "**                         Multiple Condition RNASeq: Mar 2012                      **\n" + "**************************************************************************************\n" + "MCRS takes bam files, one per replica, minimum one per condition, and performs a time\n" + "series/ multiple condition type analysis using the DESeq R package.  The first step\n" + "identifies differentially expressed genes under any pairwise comparison. The second\n" + "step estimates dispersion with all of the data and heirachically clusters the \n" + "differentially expressed genes as well as the samples.  See the DESeq manual for\n" + "details.  In addition to the cluster plots, a spread sheet is created with the pValue,\n" + "FDR, and variance corrected log2Ratios for each of the pairwise comparisons as well as\n" + "the raw and log2 variance corrected alignment counts.  Use the later for subsequent\n" + "clustering and distance estimations.\n" + "\nOptions:\n" + "-s Save directory.\n" + "-c Conditions directory containing one directory for each condition with one xxx.bam\n" + "       file per biological replica and their xxx.bai indexs. 3-4 reps recommended per\n" + "       condition. The BAM files should be sorted by coordinate using Picard's SortSam.\n" + "       All spice junction coordinates should be converted to genomic coordinates using\n" + "       USeq's SamTranscriptomeParser.\n" + "-r Full path to R loaded with DESeq library, defaults to '/usr/bin/R' file, see\n" + "       http://www-huber.embl.de/users/anders/DESeq/ . Type 'library(DESeq)' in\n" + "       an R terminal to see if it is installed. \n" + "-u UCSC RefFlat or RefSeq GENE table file, full path. See,\n" + "       http://genome.ucsc.edu/cgi-bin/hgTables, (name1 name2(optional) chrom strand\n" + "       txStart txEnd cdsStart cdsEnd exonCount exonStarts exonEnds). WARNING!!!!!!\n" + "       This table should contain only one composite transcript per gene. Use the\n" + "       MergeUCSCGeneTable app to collapse Ensembl transcripts downloaded from UCSC in\n" + "       RefFlat format.\n" + "-b (Or) a bed file (chr, start, stop,...), full path, See,\n" + "       http://genome.ucsc.edu/FAQ/FAQformat#format1\n" + "-g Genome Version  (ie H_sapiens_Mar_2006), see UCSC Browser,\n" + "      http://genome.ucsc.edu/FAQ/FAQreleases.\n" + "\nAdvanced Options:\n" + "-v Filter for variance outliers in DESeq, defaults to not filtering.\n" + "-o Don't remove overlapping exons, defaults to filtering gene annotation for overlaps.\n" + "-f Minimum FDR threshold, defaults to 10 (-10Log10(FDR=0.1))\n" + "-l Minimum absolute log2 ratio threshold, defaults to 1 (2x)\n" + "-e Minimum number alignments per gene/ region, defaults to 20\n" + "-t Don't delete temp (R script, results, Rout, etc..) files.\n" + "\n" + "Example: java -Xmx4G -jar pathTo/USeq/Apps/MultipleConditionRNASeq -c\n" + "      /Data/TimeCourse/ESCells/ -s /Data/TimeCourse/MCRS -g H_sapiens_Feb_2009\n" + "     -u /Anno/mergedHg19EnsemblGenes.ucsc.gz\n\n" + "**************************************************************************************\n");
    }
}
