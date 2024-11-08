package edu.utah.seq.parsers;

import java.io.*;
import java.util.regex.*;
import util.bio.parsers.MultiFastaParser;
import util.bio.seq.Seq;
import util.gen.*;
import java.util.*;
import net.sf.samtools.SAMRecord;
import edu.utah.seq.data.*;
import edu.utah.seq.data.sam.MalformedSamAlignmentException;
import edu.utah.seq.data.sam.SamAlignment;
import edu.utah.seq.data.sam.SamAlignmentFlags;

/**Parses a Novoalign bisulfite alignment txt file. PointData scores are set to 1.
 * @author david.nix@hci.utah.edu 
 **/
public class NovoalignBisulfiteParser {

    private File[] dataFiles;

    private String versionedGenome;

    private HashMap<String, File> chromosomeFastaFiles = new HashMap<String, File>();

    private File workingFile;

    private File saveDirectory;

    private File nonConvertedCPointDataDirectory;

    private File convertedCPointDataDirectory;

    private float minimumPosteriorProbability = 13;

    private float maximumAlignmentScore = 240;

    private int minimumBaseScore = 20;

    private int minimumStandAloneBaseScore = 30;

    private boolean printBed = false;

    private boolean uniquesOnly = false;

    private boolean reverseSecondPairsStrand = true;

    private static Pattern TAB = Pattern.compile("\\t");

    public static final Pattern COMMENT = Pattern.compile("^#.*");

    public static final Pattern CTStart = Pattern.compile("CT.*");

    public static final Pattern MD_BIS_SUB_MATCHER = Pattern.compile("(\\d+)([^0-9])");

    private boolean samFormat = true;

    private boolean useParsedFiles = false;

    private String adapter = "chrAdapt";

    private String phiX = "chrPhiX";

    private PrintWriter nonConvertedCs = null;

    private PrintWriter convertedCs = null;

    private HashMap<String, Long> nonConvertedCTypes = new HashMap<String, Long>();

    private HashMap<String, Long> convertedCTypes = new HashMap<String, Long>();

    private HashMap<String, DataOutputStream> chromOut = new HashMap<String, DataOutputStream>();

    private ArrayList<File> parsedBinaryDataFiles = new ArrayList<File>();

    private ArrayList<Point> nonConvertedCPoints = new ArrayList<Point>();

    private ArrayList<Point> convertedCPoints = new ArrayList<Point>();

    private String strand;

    private String chromosome = "";

    private String genomicSequence = null;

    private int genomicSequenceLengthMinus3 = 0;

    private long totalConvertedCsSequenced = 0;

    private long totalNonConvertedCsSequenced = 0;

    private long bpPairedSequence = 0;

    private long bpPairedOverlappingSequence = 0;

    private ArrayList<BaseObservation> boAL = new ArrayList<BaseObservation>();

    private HashMap<Integer, BaseObservation> firstHM = new HashMap<Integer, BaseObservation>();

    private int readIDIndex = 0;

    private int readTypeIndex = 1;

    private int sequenceIndex = 2;

    private int qualityIndex = 3;

    private int alignmentStatusIndex = 4;

    private int alignmentScoreIndex = 5;

    private int posteriorProbabilityIndex = 6;

    private int chromosomeIndex = 7;

    private int positionIndex = 8;

    private int strandIndex = 9;

    private int chromosomeIndexMate = 10;

    private int positionIndexMate = 11;

    private int strandIndexMate = 12;

    private int baseCallCommentIndex = 13;

    private long numberAlignmentsFailingDuplicateCheck = 0;

    private long numberAlignmentsFailingQualityScore = 0;

    private long numberAlignmentsFailingAlignmentScore = 0;

    private long numberControlAlignments = 0;

    private long numberAlignmentsFailingQC = 0;

    private long numberAlignmentsUnmapped = 0;

    private long numberPassingAlignments = 0;

    public NovoalignBisulfiteParser(String[] args) {
        long startTime = System.currentTimeMillis();
        processArgs(args);
        System.out.println("\nSplitting text alignment data by chromosome and filtering...");
        if (useParsedFiles && fetchParsedFiles()) {
            System.err.println("\nWARNING: Parsed files found! Using these instead.  Be sure prior txt parsing completed. Delete the chrXXXX+/- files from " + saveDirectory + " if you would like to reparse the txt alignment files.");
        } else {
            for (int i = 0; i < dataFiles.length; i++) {
                workingFile = dataFiles[i];
                System.out.print("\t" + workingFile);
                boolean parsed;
                if (workingFile.getName().endsWith(".sam.gz") || workingFile.getName().endsWith(".sam")) {
                    samFormat = true;
                    System.out.print(" (SAM format) ");
                    parsed = parseWorkingSAMFile();
                } else {
                    System.out.print(" (NATIVE format) ");
                    parsed = parseWorkingFile();
                    samFormat = false;
                }
                if (parsed == false) Misc.printExit("\n\tError: failed to parse, aborting.\n");
            }
            closeWriters();
            double total = numberAlignmentsFailingDuplicateCheck + numberAlignmentsFailingQualityScore + numberAlignmentsFailingAlignmentScore + numberControlAlignments + numberAlignmentsFailingQC + numberAlignmentsUnmapped + numberPassingAlignments;
            System.out.println("\nFiltering statistics for " + (int) total + " alignments:");
            System.out.println(numberAlignmentsFailingDuplicateCheck + "\tFailed duplicate flag");
            System.out.println(numberAlignmentsFailingQualityScore + "\tFailed mapping quality score (" + minimumPosteriorProbability + ")");
            System.out.println(numberAlignmentsFailingAlignmentScore + "\tFailed alignment score (" + maximumAlignmentScore + ")");
            System.out.println(numberControlAlignments + "\tAligned to phiX or adapters");
            System.out.println(numberAlignmentsFailingQC + "\tFailed vendor QC");
            System.out.println(numberAlignmentsUnmapped + "\tAre unmapped\n");
            System.out.println(numberPassingAlignments + "\tPassed filters (" + Num.formatPercentOneFraction(((double) numberPassingAlignments) / total) + ")");
        }
        System.out.print("\nParsing binary data...\n\t");
        parseBinaryData();
        System.out.println();
        System.out.println("\nCounts for non-converted C contexts:\n" + nonConvertedCTypes + "\n");
        System.out.println("\nCounts for converted C contexts:\n" + convertedCTypes + "\n");
        System.out.println(totalNonConvertedCsSequenced + "\tTotal non-converted Cs sequenced");
        System.out.println(totalConvertedCsSequenced + "\tTotal converted Cs sequenced");
        double fractionNonConverted = (double) (totalNonConvertedCsSequenced) / (double) (totalNonConvertedCsSequenced + totalConvertedCsSequenced);
        System.out.println(Num.formatNumber(fractionNonConverted, 3) + "\tFraction non converted C's.");
        if (bpPairedOverlappingSequence != 0) {
            System.out.println();
            System.out.println(bpPairedOverlappingSequence + "\tBPs overlapping paired sequence");
            System.out.println(bpPairedSequence + "\tBPs paired sequence");
            double fractionOverlap = (double) (bpPairedOverlappingSequence) / (double) bpPairedSequence;
            System.out.println(Num.formatNumber(fractionOverlap, 3) + "\tFraction overlapping bps from paired reads.");
        }
        for (int i = 0; i < parsedBinaryDataFiles.size(); i++) parsedBinaryDataFiles.get(i).delete();
        double diffTime = ((double) (System.currentTimeMillis() - startTime)) / 1000;
        System.out.println("\nDone! " + Math.round(diffTime) + " seconds\n");
    }

    /**For processing blocks of SAMRecords*/
    public NovoalignBisulfiteParser(String chromosome, String genomicSequence) {
        this.chromosome = chromosome;
        this.genomicSequence = genomicSequence;
        genomicSequenceLengthMinus3 = genomicSequence.length() - 3;
    }

    public boolean fetchParsedFiles() {
        Pattern p = Pattern.compile(".+[-+]$");
        File[] files = IO.extractFiles(saveDirectory);
        for (int i = 0; i < files.length; i++) {
            if (p.matcher(files[i].getName()).matches()) parsedBinaryDataFiles.add(files[i]);
        }
        if (parsedBinaryDataFiles.size() != 0) return true;
        return false;
    }

    public void parseBinaryData() {
        try {
            if (printBed) {
                nonConvertedCs = new PrintWriter(new FileWriter(new File(saveDirectory, "nonConvertedCs.bed")));
                convertedCs = new PrintWriter(new FileWriter(new File(saveDirectory, "convertedCs.bed")));
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        File[] files = new File[parsedBinaryDataFiles.size()];
        parsedBinaryDataFiles.toArray(files);
        Arrays.sort(files);
        String oldChrom = "";
        for (int i = 0; i < files.length; i++) {
            chromosome = files[i].getName();
            strand = chromosome.substring(chromosome.length() - 1);
            chromosome = chromosome.substring(0, chromosome.length() - 1);
            int readLength = 0;
            if (oldChrom.equals(chromosome) == false) {
                File seqFile = chromosomeFastaFiles.get(chromosome);
                if (seqFile == null) {
                    System.err.println("\n\tWarning, could not find a fasta file for " + chromosome + ", skipping!");
                    continue;
                } else System.out.print(chromosome + " ");
                MultiFastaParser mfp = new MultiFastaParser(seqFile);
                genomicSequence = mfp.getSeqs()[0];
                genomicSequenceLengthMinus3 = genomicSequence.length() - 3;
                oldChrom = chromosome;
            }
            DataInputStream dis = null;
            ParsedAlignment oldPA = null;
            try {
                dis = new DataInputStream(new BufferedInputStream(new FileInputStream(files[i])));
                while (true) {
                    ParsedAlignment pa = new ParsedAlignment(dis, samFormat, this);
                    if (pa.isPassesParsing() == false) continue; else if (oldPA == null) oldPA = pa; else if (oldPA.getReadID().equals(pa.getReadID())) {
                        int leftPos = oldPA.getPosition();
                        int rightPos = pa.getPosition();
                        int l;
                        int r;
                        BaseObservation[] first;
                        BaseObservation[] second;
                        if (leftPos > rightPos) {
                            l = rightPos;
                            r = leftPos;
                            first = pa.getBo();
                            second = oldPA.getBo();
                        } else {
                            l = leftPos;
                            r = rightPos;
                            second = pa.getBo();
                            first = oldPA.getBo();
                        }
                        int stopL = l + oldPA.getBases().length;
                        long diff = stopL - r;
                        if (diff > 0) {
                            addBaseObservations(processPairs(r, stopL, first, second));
                        } else {
                            addBaseObservations(first);
                            addBaseObservations(second);
                        }
                        oldPA = null;
                    } else {
                        addBaseObservations(oldPA.getBo());
                        oldPA = pa;
                    }
                }
            } catch (EOFException eof) {
                if (oldPA != null) addBaseObservations(oldPA.getBo());
                Point[] p = null;
                PointData pd = null;
                Info info = null;
                if (nonConvertedCPoints.size() != 0) {
                    p = new Point[nonConvertedCPoints.size()];
                    nonConvertedCPoints.toArray(p);
                    nonConvertedCPoints.clear();
                    Arrays.sort(p, new ComparatorPointPosition());
                    p = Point.sumIdenticalPositionScores(p);
                    pd = Point.extractPositionScores(p);
                    info = new Info("MergedHitCountNonConvertedC", versionedGenome, chromosome, strand, readLength, null);
                    pd.setInfo(info);
                    pd.writePointData(nonConvertedCPointDataDirectory);
                }
                if (convertedCPoints.size() != 0) {
                    p = new Point[convertedCPoints.size()];
                    convertedCPoints.toArray(p);
                    convertedCPoints.clear();
                    Arrays.sort(p, new ComparatorPointPosition());
                    p = Point.sumIdenticalPositionScores(p);
                    pd = Point.extractPositionScores(p);
                    info = new Info("MergedHitCountConvertedC", versionedGenome, chromosome, strand, readLength, null);
                    pd.setInfo(info);
                    pd.writePointData(convertedCPointDataDirectory);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Misc.printErrAndExit("\nError encountered in parsing this binary file? " + files[i]);
            } finally {
                if (dis != null) {
                    try {
                        dis.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        }
        if (printBed) {
            nonConvertedCs.close();
            convertedCs.close();
        }
    }

    public ArrayList<AlignmentPair> parseSamAlignments(ArrayList<SAMRecord> samAL) {
        ParsedAlignment[] pas = new ParsedAlignment[samAL.size()];
        for (int i = 0; i < pas.length; i++) pas[i] = new ParsedAlignment(samAL.get(i), this);
        Arrays.sort(pas);
        ArrayList<AlignmentPair> pairs = new ArrayList<AlignmentPair>();
        ParsedAlignment oldPA = null;
        for (ParsedAlignment pa : pas) {
            if (pa.isPassesParsing() == false) continue; else if (oldPA == null) oldPA = pa; else if (oldPA.getReadID().equals(pa.getReadID())) {
                int leftPos = oldPA.getPosition();
                int rightPos = pa.getPosition();
                int l;
                int r;
                BaseObservation[] first;
                BaseObservation[] second;
                ParsedAlignment firstPA;
                ParsedAlignment secondPA;
                if (leftPos > rightPos) {
                    l = rightPos;
                    r = leftPos;
                    first = pa.getBo();
                    second = oldPA.getBo();
                    firstPA = pa;
                    secondPA = oldPA;
                } else {
                    l = leftPos;
                    r = rightPos;
                    second = pa.getBo();
                    first = oldPA.getBo();
                    secondPA = pa;
                    firstPA = oldPA;
                }
                int stopL = l + oldPA.getBases().length;
                long diff = stopL - r;
                if (diff > 0) {
                    pairs.add(new AlignmentPair(firstPA, secondPA, processPairs(r, stopL, first, second)));
                } else {
                    pairs.add(new AlignmentPair(firstPA, secondPA));
                }
                oldPA = null;
            } else {
                oldPA = pa;
                pairs.add(new AlignmentPair(oldPA));
            }
        }
        if (oldPA != null) pairs.add(new AlignmentPair(oldPA));
        return pairs;
    }

    /**Closes writers.*/
    public void closeWriters() {
        try {
            Iterator<DataOutputStream> it = chromOut.values().iterator();
            while (it.hasNext()) it.next().close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean parseWorkingFile() {
        try {
            int badLines = 0;
            BufferedReader in = IO.fetchBufferedReader(workingFile);
            String line;
            String[] tokens = null;
            int counter = 0;
            String currentChromStrand = "";
            DataOutputStream dos = null;
            while ((line = in.readLine()) != null) {
                if (++counter == 25000) {
                    System.out.print(".");
                    counter = 0;
                }
                if (COMMENT.matcher(line).matches()) continue;
                tokens = TAB.split(line);
                if (tokens.length < 14) continue;
                if (tokens[baseCallCommentIndex].equals("QC")) {
                    numberAlignmentsFailingQC++;
                    continue;
                }
                if (tokens[chromosomeIndex].contains(adapter) || tokens[chromosomeIndex].contains(phiX)) {
                    numberControlAlignments++;
                    continue;
                }
                int sequenceLength = tokens[sequenceIndex].length();
                if (sequenceLength != tokens[qualityIndex].length()) {
                    System.err.println("\nSeq length != Qual length, skipping -> " + line);
                    if (badLines++ > 1000) Misc.printErrAndExit("\nToo many malformed lines. Aborting \n");
                    continue;
                }
                if (uniquesOnly && tokens[alignmentStatusIndex].equals("U") == false) {
                    numberAlignmentsFailingDuplicateCheck++;
                    continue;
                }
                float probScore = Float.parseFloat(tokens[posteriorProbabilityIndex]);
                if (probScore < minimumPosteriorProbability) {
                    numberAlignmentsFailingQualityScore++;
                    continue;
                }
                float alignmentScore = Float.parseFloat(tokens[alignmentScoreIndex]);
                if (alignmentScore > maximumAlignmentScore) {
                    numberAlignmentsFailingAlignmentScore++;
                    continue;
                }
                numberPassingAlignments++;
                String chromosome = tokens[chromosomeIndex].substring(1);
                String strand = "-";
                Matcher mat = CTStart.matcher(tokens[baseCallCommentIndex]);
                if (mat.matches()) strand = "+";
                String chromosomeStrand = chromosome + strand;
                if (tokens[strandIndex].equals("R")) {
                    tokens[sequenceIndex] = Seq.reverseComplementDNA(tokens[sequenceIndex]);
                    tokens[qualityIndex] = Misc.reverse(tokens[qualityIndex]);
                }
                if (currentChromStrand.equals(chromosomeStrand) == false) {
                    currentChromStrand = chromosomeStrand;
                    if (chromOut.containsKey(currentChromStrand)) dos = chromOut.get(currentChromStrand); else {
                        File f = new File(saveDirectory, currentChromStrand);
                        parsedBinaryDataFiles.add(f);
                        dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(f)));
                        chromOut.put(currentChromStrand, dos);
                    }
                }
                dos.writeUTF(tokens[readIDIndex]);
                int position = Integer.parseInt(tokens[positionIndex]) - 1;
                dos.writeInt(position);
                dos.writeUTF(tokens[sequenceIndex]);
                dos.writeUTF(tokens[qualityIndex]);
                dos.writeUTF(tokens[baseCallCommentIndex]);
                if (tokens[readTypeIndex].equals("L")) {
                    if (tokens[strandIndexMate].equals(".")) continue;
                    if (tokens[chromosomeIndexMate].equals(".") == false) continue;
                    int leftPos = position;
                    int rightPos = Integer.parseInt(tokens[positionIndexMate]) - 1;
                    int l;
                    int r;
                    if (leftPos > rightPos) {
                        l = rightPos;
                        r = leftPos;
                    } else {
                        l = leftPos;
                        r = rightPos;
                    }
                    int stopL = l + sequenceLength;
                    long diff = stopL - r;
                    if (diff >= 0) bpPairedOverlappingSequence += diff;
                    bpPairedSequence += (2 * sequenceLength);
                }
            }
            System.out.println();
        } catch (Exception e) {
            System.err.println("\nError parsing Novoalign file or writing split binary chromosome files.\nToo many open files? Too many chromosomes? " + "If so then login as root and set the default higher using the ulimit command (e.g. ulimit -n 10000)\n");
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean parseWorkingSAMFile() {
        try {
            BufferedReader in = IO.fetchBufferedReader(workingFile);
            String line;
            int counter = 0;
            String currentChromStrand = "";
            DataOutputStream dos = null;
            int numBadLines = 0;
            while ((line = in.readLine()) != null) {
                if (++counter == 25000) {
                    System.out.print(".");
                    counter = 0;
                }
                line = line.trim();
                if (line.length() == 0 || line.startsWith("@")) continue;
                SamAlignment sa;
                try {
                    sa = new SamAlignment(line, false);
                } catch (Exception e) {
                    System.out.println("\nSkipping malformed sam alignment ->\n" + line + "\n" + e.getMessage());
                    if (numBadLines++ > 1000) Misc.printErrAndExit("\nAboring: too many malformed SAM alignments.\n");
                    continue;
                }
                if (sa.isUnmapped()) {
                    numberAlignmentsUnmapped++;
                    continue;
                }
                if (sa.failedQC()) {
                    numberAlignmentsFailingQC++;
                    continue;
                }
                if (sa.getReferenceSequence().startsWith(phiX) || sa.getReferenceSequence().startsWith(adapter)) {
                    numberControlAlignments++;
                    continue;
                }
                if (sa.getAlignmentScore() > maximumAlignmentScore) {
                    numberAlignmentsFailingAlignmentScore++;
                    continue;
                }
                if (sa.getMappingQuality() < minimumPosteriorProbability) {
                    numberAlignmentsFailingQualityScore++;
                    continue;
                }
                if (uniquesOnly && sa.isADuplicate()) {
                    numberAlignmentsFailingDuplicateCheck++;
                    continue;
                }
                numberPassingAlignments++;
                String firstSecond = "";
                if (sa.isFirstPair()) firstSecond = "/1";
                if (sa.isSecondPair()) firstSecond = "/2";
                String readID = sa.getName() + firstSecond;
                String chromosomeStrand = null;
                if (reverseSecondPairsStrand && sa.isSecondPair()) {
                    if (sa.isReverseStrand()) chromosomeStrand = sa.getReferenceSequence() + "+"; else chromosomeStrand = sa.getReferenceSequence() + "-";
                } else {
                    if (sa.isReverseStrand()) chromosomeStrand = sa.getReferenceSequence() + "-"; else chromosomeStrand = sa.getReferenceSequence() + "+";
                }
                int position = sa.getPosition();
                sa.trimMaskingOfReadToFitAlignment();
                String isGA;
                if (sa.isPartOfAPairedAlignment()) {
                    if (sa.isFirstPair() && sa.isReverseStrand() == false) isGA = "f"; else if (sa.isSecondPair() && sa.isReverseStrand()) isGA = "f"; else isGA = "t";
                } else if (sa.isReverseStrand()) isGA = "t"; else isGA = "f";
                if (currentChromStrand.equals(chromosomeStrand) == false) {
                    currentChromStrand = chromosomeStrand;
                    if (chromOut.containsKey(currentChromStrand)) dos = chromOut.get(currentChromStrand); else {
                        File f = new File(saveDirectory, currentChromStrand);
                        parsedBinaryDataFiles.add(f);
                        dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(f)));
                        chromOut.put(currentChromStrand, dos);
                    }
                }
                dos.writeUTF(readID);
                dos.writeInt(position);
                dos.writeUTF(sa.getSequence());
                dos.writeUTF(sa.getQualities());
                dos.writeUTF(isGA + sa.getCigar());
                if (sa.isFirstPair() && sa.isPartOfAPairedAlignment()) {
                    if (sa.getMateReferenceSequence().equals("=") || sa.getMateReferenceSequence().equals(sa.getReferenceSequence())) {
                        int leftPos = position;
                        int rightPos = sa.getMatePosition();
                        int l;
                        int r;
                        if (leftPos > rightPos) {
                            l = rightPos;
                            r = leftPos;
                        } else {
                            l = leftPos;
                            r = rightPos;
                        }
                        int sequenceLength = sa.getSequence().length();
                        int stopL = l + sequenceLength;
                        long diff = stopL - r;
                        if (diff >= 0) bpPairedOverlappingSequence += diff;
                        bpPairedSequence += (2 * sequenceLength);
                    }
                }
            }
            System.out.println();
        } catch (Exception e) {
            System.err.println("\nError parsing Novoalign file or writing split binary chromosome files.\nToo many open files? Too many chromosomes? " + "If so then login as root and set the default higher using the ulimit command (e.g. ulimit -n 10000)\n");
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void addBaseObservations(BaseObservation[] bos) {
        for (int i = 0; i < bos.length; i++) {
            if (bos[i].isConverted()) {
                if (printBed) convertedCs.println(bos[i].getBedLine());
                convertedCPoints.add(bos[i].getPoint());
                totalConvertedCsSequenced++;
                long count = 1;
                if (convertedCTypes.containsKey(bos[i].getGenSeq())) {
                    count = convertedCTypes.get(bos[i].getGenSeq()).longValue() + 1;
                }
                convertedCTypes.put(bos[i].getGenSeq(), new Long(count));
            } else {
                if (printBed) nonConvertedCs.println(bos[i].getBedLine());
                nonConvertedCPoints.add(bos[i].getPoint());
                totalNonConvertedCsSequenced++;
                long count = 1;
                if (nonConvertedCTypes.containsKey(bos[i].getGenSeq())) {
                    count = nonConvertedCTypes.get(bos[i].getGenSeq()).longValue() + 1;
                }
                nonConvertedCTypes.put(bos[i].getGenSeq(), new Long(count));
            }
        }
    }

    /**Flattens two overlapping BaseObservations.  Only saves consensus BOs in the range defined by start and stop(excluded) or where one score is much better than the other.*/
    private BaseObservation[] processPairs(int start, int stop, BaseObservation[] first, BaseObservation[] second) {
        boAL.clear();
        firstHM.clear();
        for (int i = 0; i < first.length; i++) {
            if (first[i].getPosition().intValue() < start) boAL.add(first[i]); else firstHM.put(first[i].getPosition(), first[i]);
        }
        for (int i = 0; i < second.length; i++) {
            if (second[i].getPosition().intValue() >= stop) boAL.add(second[i]); else {
                BaseObservation left = firstHM.get(second[i].getPosition());
                if (left != null) {
                    if (left.isConverted() == second[i].isConverted()) boAL.add(left); else {
                        double fraction = left.getScore() / second[i].getScore();
                        if (fraction >= 1.5) {
                            boAL.add(left);
                        } else if (fraction <= 0.667) boAL.add(second[i]);
                    }
                    firstHM.remove(second[i].getPosition());
                } else if (second[i].getScore() >= minimumStandAloneBaseScore) boAL.add(second[i]);
            }
        }
        if (firstHM.size() != 0) {
            Iterator<BaseObservation> c = firstHM.values().iterator();
            while (c.hasNext()) {
                BaseObservation bo = c.next();
                if (bo.getScore() >= minimumStandAloneBaseScore) boAL.add(bo);
            }
        }
        first = new BaseObservation[boAL.size()];
        boAL.toArray(first);
        return first;
    }

    /**Returns the score if it's better than the minimum otherwise returns 'x'*/
    public char checkBase(int score, char base) {
        if (score >= minimumBaseScore) return base; else return 'x';
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            printDocs();
            System.exit(0);
        }
        new NovoalignBisulfiteParser(args);
    }

    /**This method will process each argument and assign new varibles*/
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
                        case 'a':
                            dataFiles = IO.extractFiles(new File(args[++i]));
                            break;
                        case 'f':
                            fastas = IO.extractFiles(new File(args[++i]));
                            break;
                        case 's':
                            saveDirectory = new File(args[++i]);
                            saveDirectory.mkdir();
                            break;
                        case 'v':
                            versionedGenome = args[i + 1];
                            i++;
                            break;
                        case 'p':
                            printBed = true;
                            break;
                        case 'u':
                            uniquesOnly = true;
                            break;
                        case 'z':
                            useParsedFiles = true;
                            break;
                        case 'b':
                            minimumBaseScore = Integer.parseInt(args[++i]);
                            break;
                        case 'c':
                            minimumStandAloneBaseScore = Integer.parseInt(args[++i]);
                            break;
                        case 'x':
                            maximumAlignmentScore = Float.parseFloat(args[++i]);
                            break;
                        case 'q':
                            minimumPosteriorProbability = Float.parseFloat(args[++i]);
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
        if (versionedGenome == null) Misc.printErrAndExit("\nPlease provide a versioned genome (e.g. H_sapiens_Mar_2006).\n");
        if (fastas == null || fastas.length == 0) Misc.printErrAndExit("\nError: cannot find any fasta sequence files?\n");
        if (dataFiles == null || dataFiles.length == 0 || dataFiles[0].canRead() == false) Misc.printErrAndExit("\nError: cannot find your alignment file(s)!\n");
        if (saveDirectory == null || saveDirectory.isDirectory() == false) Misc.printErrAndExit("\nPlease enter a directory to use in saving your results.\n");
        File pointDataDirectory = new File(saveDirectory, "MergedHitCountPointData");
        pointDataDirectory.mkdirs();
        nonConvertedCPointDataDirectory = new File(pointDataDirectory, "NonConvertedC");
        nonConvertedCPointDataDirectory.mkdir();
        convertedCPointDataDirectory = new File(pointDataDirectory, "ConvertedC");
        convertedCPointDataDirectory.mkdir();
        chromosomeFastaFiles = new HashMap<String, File>();
        Pattern chrom = Pattern.compile("(.+)\\.fa.*");
        for (int i = 0; i < fastas.length; i++) {
            Matcher mat = chrom.matcher(fastas[i].getName());
            if (mat.matches()) chromosomeFastaFiles.put(mat.group(1), fastas[i]);
        }
    }

    public static void printDocs() {
        System.out.println("\n" + "**************************************************************************************\n" + "**                        Novoalign Bisulfite Parser: Feb 2012                      **\n" + "**************************************************************************************\n" + "Parses Novoalign single and paired bisulfite sequence alignment files into xxx.bed\n" + "and PointData file formats. Generates several summary statistics on converted and non-\n" + "converted C contexts. Flattens overlapping reads in a pair to call consensus bps.\n" + "Note: for paired read RNA-Seq data run through the SamTranscriptomeParser, be sure to\n" + "disable the reversing of the 2nd read's strand (e.g. use the -o option).\n" + "\nOptions:\n" + "-a Alignment file or directory containing novoalignments in native xxx.txt(.zip/.gz)\n" + "      or SAM (xxx.sam(.zip/.gz OK)) format. Multiple files will be merged.\n" + "-f Fasta file directory, chromosome specific xxx.fa/.fasta(.zip/.gz OK) files.\n" + "-s Save directory.\n" + "-v Versioned Genome (ie H_sapiens_Mar_2006), see UCSC Browser,\n" + "      http://genome.ucsc.edu/FAQ/FAQreleases.\n" + "\nDefault Options:\n" + "-p Print bed file parsed data.\n" + "-x Maximum alignment score. Defaults to 240, smaller numbers are more stringent.\n" + "-q Minimum mapping quality score. Defaults to 13, bigger numbers are more stringent.\n" + "      This is a phred-scaled posterior probability that the mapping position of read\n" + "      is incorrect.\n" + "-b Minimum base quality score for reporting a non/converted C, defaults to 20.\n" + "-c Minimum base quality score for reporting a overlapping non/converted C not found\n" + "      in the other pair, defaults to 30.\n" + "-u Unique alignments only, defaults to all. For SAM format, you need to run the\n" + "      Picard MarkDuplicates app first.\n" + "\nExample: java -Xmx25G -jar pathToUSeq/Apps/NovoalignBisulfiteParser -u -a\n" + "      /Novo/Run7/ -f /Genomes/Hg19/Fastas/ -v H_sapiens_Feb_2009 -s /Novo/Run7/NBP \n\n" + "**************************************************************************************\n");
    }

    public String getGenomicSequence() {
        return genomicSequence;
    }

    public int getGenomicSequenceLengthMinus3() {
        return genomicSequenceLengthMinus3;
    }

    public int getMinimumBaseScore() {
        return minimumBaseScore;
    }

    public String getChromosome() {
        return chromosome;
    }

    public String getStrand() {
        return strand;
    }
}
