package edu.utah.seq.analysis;

import java.io.*;
import java.util.regex.*;
import java.util.*;
import edu.utah.seq.useq.data.RegionScoreText;
import net.sf.samtools.*;
import util.bio.annotation.Bed;
import util.bio.annotation.ExonIntron;
import util.bio.parsers.*;
import util.bio.seq.Seq;
import util.gen.*;
import edu.utah.seq.data.*;
import edu.utah.seq.parsers.*;
import util.bio.cluster.*;
import org.apache.commons.math.stat.inference.ChiSquareTestImpl;

/** Application for examining select regions for a bimodal distribution of methylation in reads.
 * @author Nix
 * */
public class AllelicMethylationDetector {

    private File[] convertedPointDirs;

    private File[] nonConvertedPointDirs;

    private File[] bamFiles;

    private HashMap<String, File> chromosomeFastaFiles = new HashMap<String, File>();

    private File saveDirectory;

    private File fullPathToR = new File("/usr/bin/R");

    private File bedFile;

    private File refSeqFile;

    private int minimumCsInAlignment = 6;

    private double minimumReadsInRegion = 15;

    private boolean removeOverlappingRegions = false;

    private float minimumFractionMethylation = 0.4f;

    private float maximumFractionMethylation = 0.6f;

    private int maximumNumberReads = 10000;

    private HashMap<String, UCSCGeneLine[]> geneModels;

    private UCSCGeneLine[] allGeneLines;

    private UCSCGeneLine[] geneLinesWithReads;

    private AllelicRegionMaker irm;

    private RegionScoreText[] rst;

    private String chromosome;

    private String[] chromosomes;

    private SAMFileReader[] samReaders;

    private String genomicSequence = null;

    private NovoalignBisulfiteParser novoalignBisulfiteParser;

    private ArrayList<PutativeImprintRegion> pirAL = new ArrayList<PutativeImprintRegion>();

    private PutativeImprintRegion[] pirs;

    /**Stand alone.*/
    public AllelicMethylationDetector(String[] args) {
        long startTime = System.currentTimeMillis();
        processArgs(args);
        findRegions();
        if (pirAL.size() == 0) System.out.println("\nNo regions found passing filters?!\n"); else {
            pirs = new PutativeImprintRegion[pirAL.size()];
            pirAL.toArray(pirs);
            scoreRegions();
            Arrays.sort(pirs);
            printRegions();
        }
        double diffTime = ((double) (System.currentTimeMillis() - startTime)) / 60000;
        System.out.println("\nDone! " + Math.round(diffTime) + " minutes\n");
    }

    public void scoreRegions() {
        System.out.println("\nScoring " + pirs.length + " regions for log likelihood of U distribution (it's R so patience required)...");
        ArrayList<int[][]> counts = new ArrayList<int[][]>();
        for (PutativeImprintRegion p : pirs) {
            counts.add(p.nonConReadCounts);
        }
        double[][] stats = logLikeRatio(counts, saveDirectory, fullPathToR);
        for (int i = 0; i < pirs.length; i++) {
            stats[i][3] = Num.minus10log10(stats[i][3]);
            pirs[i].kenStats = stats[i];
            pirs[i].sortBy = pirs[i].histogram.getTotalBinCounts();
        }
    }

    public void printRegions() {
        try {
            File bedFile = new File(saveDirectory, "putativeImprintedRegions.bed");
            PrintWriter bedOut = new PrintWriter(new FileWriter(bedFile));
            File regionFile = new File(saveDirectory, "putativeImprintedRegions.txt");
            PrintWriter regionsOut = new PrintWriter(new FileWriter(regionFile));
            for (PutativeImprintRegion i : pirs) {
                bedOut.println(i.bedLine);
                regionsOut.println(i);
                if (i.histogram.getTotalBinCounts() > 300) i.histogram.printScaledHistogram(regionsOut); else i.histogram.printHistogram(regionsOut);
                regionsOut.println();
            }
            bedOut.close();
            regionsOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void makeSamReaders() {
        samReaders = new SAMFileReader[bamFiles.length];
        for (int i = 0; i < samReaders.length; i++) samReaders[i] = new SAMFileReader(bamFiles[i]);
    }

    public void closeSamReaders() {
        for (int i = 0; i < samReaders.length; i++) samReaders[i].close();
    }

    public void findRegions() {
        if (convertedPointDirs != null) {
            irm = new AllelicRegionMaker(convertedPointDirs, nonConvertedPointDirs);
            chromosomes = irm.getChromosomes();
        } else {
            loadGeneModels();
            chromosomes = Misc.setToStringArray(geneModels.keySet());
        }
        makeSamReaders();
        System.out.println("\nScanning regions by chromosome");
        for (String c : chromosomes) {
            chromosome = c;
            File seqFile = chromosomeFastaFiles.get(chromosome);
            if (seqFile == null) {
                System.err.println("\n\tWarning, could not find a fasta file for " + chromosome + ", skipping!");
                continue;
            }
            MultiFastaParser mfp = new MultiFastaParser(seqFile);
            genomicSequence = mfp.getSeqs()[0];
            if (irm != null) {
                rst = irm.scan(chromosome);
                if (rst == null) {
                    System.err.println("\tNo regions for " + chromosome);
                    continue;
                }
            }
            novoalignBisulfiteParser = new NovoalignBisulfiteParser(chromosome, genomicSequence);
            if (geneModels != null) scanGenes(); else scanChromosome();
        }
        closeSamReaders();
    }

    public File writeOutObservations(String randomWord) {
        File matrixFile = null;
        try {
            matrixFile = new File(saveDirectory, randomWord + "_CountMatrix.txt");
            PrintWriter out = new PrintWriter(new FileWriter(matrixFile));
            for (int i = 0; i < geneLinesWithReads.length; i++) {
                float[] scores = geneLinesWithReads[i].getScores();
                out.print("G" + i);
                for (int j = 0; j < scores.length; j++) {
                    out.print("\t" + (int) scores[j]);
                }
                out.println();
            }
            out.close();
        } catch (Exception e) {
            System.err.println("Problem writing out observations for R.");
            e.printStackTrace();
        }
        return matrixFile;
    }

    public float parseFloat(String f) {
        if (f.equals("Inf")) return Float.MIN_VALUE; else return Float.parseFloat(f);
    }

    public void loadGeneModels() {
        UCSCGeneModelTableReader reader = null;
        if (refSeqFile != null) {
            reader = new UCSCGeneModelTableReader(refSeqFile, 0);
            if (removeOverlappingRegions) {
                System.out.print("\tRemoving overlapping regions from gene models");
                String deletedGenes = reader.removeOverlappingExons();
                if (deletedGenes.length() != 0) System.out.println("\t\tWARNING: the following genes had more than 1/2 of their exonic bps removed -> " + deletedGenes);
                File f = new File(saveDirectory, Misc.removeExtension(refSeqFile.getName()) + "_NoOverlappingExons.txt");
                System.out.println("\t\tWrote modified gene table to the save directory.");
                reader.writeGeneTableToFile(f);
            }
            reader.splitByChromosome();
            geneModels = reader.getChromSpecificGeneLines();
            allGeneLines = reader.getGeneLines();
        } else if (bedFile != null) {
            Bed[] bed = Bed.parseFile(bedFile, 0, 0);
            allGeneLines = new UCSCGeneLine[bed.length];
            boolean addName = bed[0].getName().trim().equals("");
            for (int i = 0; i < bed.length; i++) {
                if (addName) bed[i].setName((i + 1) + "");
                allGeneLines[i] = new UCSCGeneLine(bed[i]);
            }
            reader = new UCSCGeneModelTableReader();
            reader.setGeneLines(allGeneLines);
            reader.splitByChromosome();
            geneModels = reader.getChromSpecificGeneLines();
        }
        if (geneModels == null || allGeneLines == null || allGeneLines.length == 0) Misc.printExit("\nProblem loading your USCS gene model table or bed file? No genes/ regions?\n");
        if (reader.checkStartStopOrder() == false) Misc.printExit("\nOne of your regions's coordinates are reversed. Check that each start is less than the stop.\n");
        for (int i = 0; i < allGeneLines.length; i++) allGeneLines[i].setScores(new float[] { 0, 0 });
    }

    /**Interbase coordinates!*/
    public ArrayList<SAMRecord> fetchAlignments(ExonIntron ei, SAMFileReader reader) {
        ArrayList<SAMRecord> al = new ArrayList<SAMRecord>();
        SAMRecordIterator i = reader.queryOverlapping(chromosome, ei.getStart() + 1, ei.getEnd());
        while (i.hasNext()) al.add(i.next());
        i.close();
        return al;
    }

    /**Interbase coordinates!*/
    public ArrayList<SAMRecord>[] fetchOverlappingAlignments(ExonIntron[] ei, SAMFileReader reader) {
        ArrayList<SAMRecord>[] als = new ArrayList[ei.length];
        for (int i = 0; i < ei.length; i++) als[i] = fetchAlignments(ei[i], reader);
        return als;
    }

    /**Interbase coordinates!*/
    public ArrayList<SAMRecord>[][] fetchOverlappingAlignments(ExonIntron[] ei, SAMFileReader[] readers) {
        ArrayList<SAMRecord>[][] ohMy = new ArrayList[readers.length][];
        for (int i = 0; i < readers.length; i++) ohMy[i] = fetchOverlappingAlignments(ei, readers[i]);
        return ohMy;
    }

    /**Interbase coordinates!*/
    public ArrayList<SAMRecord> fetchOverlappingAlignmentsCombine(ExonIntron[] ei, SAMFileReader[] readers) {
        ArrayList<SAMRecord> ohMy = new ArrayList<SAMRecord>();
        for (int i = 0; i < readers.length; i++) {
            ArrayList<SAMRecord>[] al = fetchOverlappingAlignments(ei, readers[i]);
            for (ArrayList<SAMRecord> a : al) ohMy.addAll(a);
        }
        return ohMy;
    }

    /**Interbase coordinates!*/
    public void fetchAlignments(RegionScoreText region, SAMFileReader reader, ArrayList<SAMRecord> samRecordsAL) {
        SAMRecordIterator i = reader.queryOverlapping(chromosome, region.getStart() + 1, region.getStop());
        int readCount = 0;
        while (i.hasNext()) {
            if (readCount++ > maximumNumberReads) {
                System.err.println("WARNING: max read count reached for -> '" + region.toString() + "', skipping remaining reads.");
                i.close();
                return;
            }
            SAMRecord sr = i.next();
            if (sr.getReadUnmappedFlag() == false) samRecordsAL.add(sr);
        }
        i.close();
    }

    /**Interbase coordinates!*/
    public ArrayList<SAMRecord> fetchOverlappingAlignments(RegionScoreText region, SAMFileReader[] readers) {
        ArrayList<SAMRecord> ohMy = new ArrayList<SAMRecord>();
        for (int i = 0; i < readers.length; i++) fetchAlignments(region, readers[i], ohMy);
        return ohMy;
    }

    /**Interbase coordinates!*/
    public float countUniqueAlignments(ArrayList<SAMRecord>[] alignments) {
        HashSet<String> uniqueReadNames = new HashSet<String>();
        for (ArrayList<SAMRecord> al : alignments) {
            for (SAMRecord sam : al) uniqueReadNames.add(sam.getReadName());
        }
        return uniqueReadNames.size();
    }

    /**Collects number of observations under each gene's exons.*/
    private void scanGenes() {
        UCSCGeneLine[] genes = geneModels.get(chromosome);
        for (int i = 0; i < genes.length; i++) {
            System.out.println("\nDefined Region " + genes[i]);
            ExonIntron[] exons = genes[i].getExons();
            Histogram histogram = new Histogram(0, 1, 20);
            histogram.setSkipZeroBins(false);
            int start = genes[i].getTxStart();
            int stop = genes[i].getTxEnd();
            ArrayList<SAMRecord> samAL = fetchOverlappingAlignmentsCombine(exons, samReaders);
            ArrayList<AlignmentPair> pairs = novoalignBisulfiteParser.parseSamAlignments(samAL);
            float fractionSum = 0;
            for (AlignmentPair pa : pairs) {
                int[] nonCon = pa.getmCGCounts(start, stop);
                if ((nonCon[0] + nonCon[1]) < minimumCsInAlignment) continue;
                float fraction = calculateFractionNon(nonCon);
                fractionSum += fraction;
                System.out.println("\t" + nonCon[0] + "\t" + nonCon[1] + "\t" + fraction);
                histogram.count(fraction);
            }
            float meanMethyl = fractionSum / (float) histogram.getTotalBinCounts();
            System.out.println("\tMean methy " + meanMethyl);
            histogram.printScaledHistogram();
        }
    }

    /**Collects number of observations under each gene's exons.*/
    private void scanChromosome() {
        System.out.println("\t" + chromosome + "\t" + rst.length + " regions");
        ArrayList<int[]> pairCounts = new ArrayList<int[]>();
        for (RegionScoreText region : rst) {
            Histogram histogram = new Histogram(0, 1, 20);
            histogram.setSkipZeroBins(false);
            int start = region.getStart();
            int stop = region.getStop();
            ArrayList<SAMRecord> samAL = fetchOverlappingAlignments(region, samReaders);
            ArrayList<AlignmentPair> pairs = novoalignBisulfiteParser.parseSamAlignments(samAL);
            float fractionSum = 0;
            pairCounts.clear();
            for (AlignmentPair pa : pairs) {
                int[] nonCon = pa.getmCGCounts(start, stop);
                if ((nonCon[0] + nonCon[1]) < minimumCsInAlignment) continue;
                float fraction = calculateFractionNon(nonCon);
                pairCounts.add(new int[] { nonCon[0], nonCon[1] });
                fractionSum += fraction;
                histogram.count(fraction);
            }
            float meanMethyl = fractionSum / (float) histogram.getTotalBinCounts();
            if (histogram.getTotalBinCounts() > minimumReadsInRegion && meanMethyl >= minimumFractionMethylation && meanMethyl <= maximumFractionMethylation) {
                float[] binFractions = fetchBinHistogramFractions(histogram);
                if (binFractions != null) {
                    PutativeImprintRegion pir = new PutativeImprintRegion(binFractions, meanMethyl, region.getBedLine(chromosome), histogram, pairCounts);
                    pirAL.add(pir);
                }
            }
        }
    }

    private class PutativeImprintRegion implements Comparable<PutativeImprintRegion> {

        float[] binFractions;

        float meanMethylation;

        int numberReads;

        String bedLine;

        Histogram histogram;

        int[][] nonConReadCounts;

        double[] kenStats;

        double sortBy = 0;

        private PutativeImprintRegion(float[] binFractions, float meanMethylation, String bedLine, Histogram histogram, ArrayList<int[]> counts) {
            this.binFractions = binFractions;
            this.meanMethylation = meanMethylation;
            this.bedLine = bedLine;
            this.histogram = histogram;
            numberReads = (int) histogram.getTotalBinCounts();
            nonConReadCounts = new int[counts.size()][2];
            counts.toArray(nonConReadCounts);
        }

        public int compareTo(PutativeImprintRegion pir) {
            if (pir.sortBy > this.sortBy) return 1;
            if (pir.sortBy < this.sortBy) return -1;
            return 0;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(bedLine);
            sb.append("\n");
            sb.append(meanMethylation);
            sb.append("\tMean Alignment Methylation\n");
            sb.append(kenStats[0] + "\t" + kenStats[1] + "\t" + kenStats[2] + "\t" + kenStats[3] + "\t");
            sb.append("LogLR p1 p2 -10Log10(p-val) (Ken's test stats)\n");
            sb.append(histogram.getTotalBinCounts());
            sb.append("\tNumber Reads\n");
            sb.append(binFractions[0]);
            sb.append(" ");
            sb.append(binFractions[1]);
            sb.append(" ");
            sb.append(binFractions[2]);
            sb.append("\tBin Fractions");
            return sb.toString();
        }
    }

    /**Returns null if fraction bin failed or the first middle and last.*/
    public float[] fetchBinHistogramFractions(Histogram h) {
        float[] fractions = calculateSplits(h);
        if (fractions[0] < .25 || fractions[2] < .25 || fractions[1] > .15) return null;
        return fractions;
    }

    /**Returns the fraction found*/
    public float[] calculateSplits(Histogram h) {
        int[] counts = h.getBinCounts();
        float first = 0;
        for (int i = 0; i < 7; i++) first += counts[i];
        float middle = 0;
        for (int i = 7; i < 12; i++) middle += counts[i];
        float last = 0;
        for (int i = 12; i < counts.length; i++) last += counts[i];
        float total = first + middle + last;
        return new float[] { first / total, middle / total, last / total };
    }

    public static float calculateFractionNon(int[] nonCon) {
        float non = nonCon[0] + 1;
        float con = nonCon[1] + 1;
        return non / (non + con);
    }

    private static String kensRFunction1 = "CalcBin <- function(y,p){\n" + "# y is a pair of integers\n" + "# p is a proportion strictly between 0 and 1\n" + "# Note: A factor that cancels out in the likelihood ratio is ignored.\n" + "  return(p^y[1]*(1-p)^y[2])\n" + "}\n" + "\n" + "CalcBinLogLike <- function(x){\n" + "# x is a matrix of integer pairs. The maximum of the binomial likelihood \n" + "# occurs at the sample proportion of the first column divided by the total.\n" + "# Note: A factor that cancels out of the likelihood ratio is ignored.\n" + "  a <- apply(x,2,sum)\n" + "  p0 <- a[1]/sum(a)\n" + "  bin <- log(apply(x,1,CalcBin,p0))\n" + "  return(sum(bin))\n" + "}\n" + "\n" + "CalcMixLogLike <- function(x,del){\n" + "# x is a matrix of integer pairs. \n" + "# del is a difference from the overall proportion\n" + "# we are assuming a 50% mixture\n" + "  a <- apply(x,2,sum)\n" + "  p0 <- a[1]/sum(a)\n" + "  p1 <- p0 - del\n" + "  p2 <- p0 + del\n" + "  mix <- log(0.5) + log(apply(x,1,CalcBin,p1) + apply(x,1,CalcBin,p2))\n" + "  return(sum(mix))\n" + "}\n" + "\n" + "\n" + "CalcMaxLogLike <- function(x,inc =0.001){\n" + "# Calculates the final likelihood ratio by searching for a 2-component \n" + "# mixture centered at the mean proportion.\n" + "# inc is the increments for the search (0 < inc < min(p0,1-p0))\n" + "  CalcMixLogLike1 <- function(s){CalcMixLogLike(x,s)}\n" + "  a <- apply(x,2,sum)\n" + "  p0 <- a[1]/sum(a)\n" + "  p <- min(p0,1-p0)\n" + "  inc1 <- min(inc,p)\n" + "  n <- floor(p/inc)-1\n" + "  val <- as.array(inc*(0:n))\n" + "  y <- apply(val,1,CalcMixLogLike1)\n" + "  m <- max(y)\n" + "  v <- val[y == m]\n" + "  r <- c(m,p0-v,p0+v)\n" + "  return(r)\n" + "}\n" + "\n" + "CalculatePvalue <- function(loglikratio){\n" + "# Calculates a p-value using the assumption that twice the log likelihood rato \n" + "# has a central chi-square distribution with one df\n" + "return(pchisq(2*loglikratio,df = 1,lower.tail = FALSE))\n" + "}\n" + "\n" + "CalcLogLikeRatioStat <- function(x,c){\n" + "# Evaluates the difference between the a two-component mixture model and \n" + "# a single component mode.\n" + "# Returns loglikelihood ratio and the two proportions\n" + "a <- CalcBinLogLike(x)\n" + "b <- CalcMaxLogLike(x,c)\n" + "d <- CalculatePvalue(b[1]-a)\n" + "likrat <- b[1]-a\n" + "r <- cbind(likrat,b[2],b[3],d)\n" + "colnames(r) <- c('logLR','p1','p2','p-value')\n" + "return(r)\n" + "}\n\n";

    /**Runs Ken Boucher's logLikelihood ratio test for the U distribution in R. counts[regions][#NonCon, #Con].
	 * @return double[regions]['logLR','p1','p2','p-value'] best region is one with small p1 and large p2*/
    public static double[][] logLikeRatio(ArrayList<int[][]> counts, File tempDirectory, File fullPathToR) {
        double[][] values = null;
        try {
            String randomWord = Passwords.createRandowWord(6) + ".txt";
            File rScriptFile = new File(tempDirectory, "rScript_" + randomWord);
            File rOutFile = new File(tempDirectory, "rOut_" + randomWord);
            File rResultsFile = new File(tempDirectory, "rResults_" + randomWord);
            File matrixFile = new File(tempDirectory, "countMatrix_" + randomWord);
            PrintWriter dataOut = new PrintWriter(new FileWriter(matrixFile));
            int numberRegions = counts.size();
            int runningCount = 0;
            StringBuilder stopsSB = new StringBuilder("stops = c(");
            for (int[][] regionCounts : counts) {
                for (int[] obs : regionCounts) {
                    dataOut.print(obs[0]);
                    dataOut.print("\t");
                    dataOut.println(obs[1]);
                    runningCount++;
                }
                stopsSB.append(runningCount);
                stopsSB.append(",");
            }
            dataOut.close();
            String stops = stopsSB.toString();
            stops = stops.substring(0, stops.length() - 1) + ")\n";
            StringBuilder script = new StringBuilder(kensRFunction1);
            script.append("numberStops = " + numberRegions + "\n");
            script.append(stops);
            script.append("bigMatrix = read.table('" + matrixFile + "') \n");
            script.append("results = matrix(nrow=" + numberRegions + ", ncol=4) \n");
            script.append("start = 1 \n");
            script.append("for (i in 1:numberStops){ \n");
            script.append("stop = stops[i] \n");
            script.append("subMatrix = bigMatrix[start : stops[i],] \n");
            script.append("results[i,] = CalcLogLikeRatioStat(subMatrix, 0.001) \n");
            script.append("start = stop +1 \n");
            script.append("} \n");
            script.append("write.table(results, file='" + rResultsFile.getCanonicalPath() + "',row.names = FALSE, col.names = FALSE, sep = \"\t\") ");
            IO.writeString(script.toString(), rScriptFile);
            String[] command = new String[] { fullPathToR.getCanonicalPath(), "CMD", "BATCH", "--no-save", "--no-restore", rScriptFile.getCanonicalPath(), rOutFile.getCanonicalPath() };
            IO.executeCommandLine(command);
            values = Num.loadDoubleMatrix(rResultsFile);
            if (values == null || values.length != numberRegions) throw new Exception("Number of results from R does not match the number of regions?! See tempFiles xxx+" + randomWord + " and try executing in R command shell.");
            matrixFile.deleteOnExit();
            rResultsFile.deleteOnExit();
            rOutFile.deleteOnExit();
            rScriptFile.deleteOnExit();
            rOutFile.deleteOnExit();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return values;
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            printDocs();
            System.exit(0);
        }
        new AllelicMethylationDetector(args);
    }

    /**This method will process each argument and assign new variables*/
    public void processArgs(String[] args) {
        Pattern pat = Pattern.compile("-[a-z]");
        File[] fastas = null;
        System.out.println("\nArguments: " + Misc.stringArrayToString(args, " ") + "\n");
        for (int i = 0; i < args.length; i++) {
            String lcArg = args[i].toLowerCase();
            Matcher mat = pat.matcher(lcArg);
            if (mat.matches()) {
                char test = args[i].charAt(1);
                try {
                    switch(test) {
                        case 't':
                            bamFiles = IO.extractFiles(args[++i], ".bam");
                            break;
                        case 'f':
                            fastas = IO.extractFiles(new File(args[++i]));
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
                        case 'c':
                            convertedPointDirs = IO.extractFiles(args[++i]);
                            break;
                        case 'n':
                            nonConvertedPointDirs = IO.extractFiles(args[++i]);
                            break;
                        case 'e':
                            minimumCsInAlignment = Integer.parseInt(args[++i]);
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
        if (convertedPointDirs != null || nonConvertedPointDirs != null) {
            if (refSeqFile != null || bedFile != null) {
                Misc.printErrAndExit("\nPlease enter a regions file to use in scoring regions OR provide converted and non converted PointData to scan the genome, not both!\n");
            }
            if (convertedPointDirs == null || convertedPointDirs[0].isDirectory() == false) Misc.printExit("\nError: cannot find your converted PointData directories(s)!\n");
            if (convertedPointDirs.length == 1) {
                File[] otherDirs = IO.extractOnlyDirectories(convertedPointDirs[0]);
                if (otherDirs != null && otherDirs.length > 0) convertedPointDirs = otherDirs;
            }
            if (nonConvertedPointDirs == null || nonConvertedPointDirs[0].isDirectory() == false) Misc.printExit("\nError: cannot find your non converted PointData directories(s)!\n");
            if (nonConvertedPointDirs.length == 1) {
                File[] otherDirs = IO.extractOnlyDirectories(nonConvertedPointDirs[0]);
                if (otherDirs != null && otherDirs.length > 0) nonConvertedPointDirs = otherDirs;
            }
        }
        if (bamFiles == null || bamFiles.length == 0) Misc.printErrAndExit("\nError: cannot find any treatment xxx.bam files?\n");
        lookForBaiIndexes(bamFiles);
        if (fastas == null || fastas.length == 0) Misc.printErrAndExit("\nError: cannot find any fasta sequence files?\n");
        chromosomeFastaFiles = new HashMap<String, File>();
        Pattern chrom = Pattern.compile("(.+)\\.fa.*");
        for (int i = 0; i < fastas.length; i++) {
            Matcher mat = chrom.matcher(fastas[i].getName());
            if (mat.matches()) chromosomeFastaFiles.put(mat.group(1), fastas[i]);
        }
        if (saveDirectory == null) Misc.printErrAndExit("\nError: enter a directory text to save results.\n");
        saveDirectory.mkdir();
        if (fullPathToR == null || fullPathToR.canExecute() == false) {
            Misc.printErrAndExit("\nError: Cannot find or execute the R application -> " + fullPathToR + "\n");
        }
        if (refSeqFile == null && bedFile == null && convertedPointDirs == null) {
            Misc.printErrAndExit("\nPlease enter a regions file to use in scoring regions OR provide converted and non converted PointData to scan the genome.\n");
        }
    }

    /**Looks for xxx.bam.bai and xxx.bai for each bamFile, prints error and exits if missing.*/
    public static void lookForBaiIndexes(File[] bamFiles) {
        for (File f : bamFiles) {
            File index = new File(f + ".bai");
            if (index.exists() == false) {
                index = new File(f.toString().replace(".bam", ".bai"));
                if (index.exists() == false) Misc.printErrAndExit("\nError: failed to find a xxx.bai index file for -> " + f);
            }
        }
    }

    public static void printDocs() {
        System.out.println("\n" + "**************************************************************************************\n" + "**                        Allelic Methylation Detector:  Feb 2012                   **\n" + "**************************************************************************************\n" + "AMD identifies regions displaying allelic methylation, e.g. ~50% average mCG\n" + "methylation yet individual read pairs show a bimodal fraction distribution of either\n" + "fully methylated or unmethylated.  Beta....... \n\n" + "Options:\n" + "-s Save directory.\n" + "-f Fasta file directory.\n" + "-t BAM file directory containing one or more xxx.bam file with their associated xxx.bai\n" + "       index. The BAM files should be sorted by coordinate and have passed Picard\n" + "       validation.\n" + "-r Full path to R, defaults to /usr/bin/R\n" + "-e Minimum number mapping reads per region, defaults to 20\n" + "-c Converted CG context PointData directories, full path, comma delimited. These \n" + "       should contain stranded chromosome specific xxx_-/+_.bar.zip files. One\n" + "       can also provide a single directory that contains multiple PointData\n" + "       directories. Use the ParsePointDataContexts on the output of the\n" + "       NovoalignBisulfiteParser to select CG contexts. \n" + "-n Non-converted PointData directories, ditto. \n" + "\n" + "Example: java -Xmx4G -jar pathTo/USeq/Apps/ \n\n" + "**************************************************************************************\n");
    }
}
