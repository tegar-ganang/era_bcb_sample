package edu.utah.seq.analysis;

import java.io.*;
import edu.utah.seq.data.FilterPointData;
import edu.utah.seq.data.PointData;
import edu.utah.seq.data.ReadCoverage;
import edu.utah.seq.data.SmoothingWindowInfo;
import edu.utah.seq.data.SubSamplePointData;
import edu.utah.seq.parsers.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.*;
import edu.utah.seq.useq.*;
import edu.utah.seq.useq.apps.Bar2USeq;
import util.bio.annotation.Coordinate;
import util.bio.parsers.UCSCGeneModelTableReader;
import util.gen.*;

/**Application for chaining together the many steps in an RNA-Seq analysis.*/
public class RNASeq2 {

    private File rApplication = new File("/usr/bin/R");

    private File resultsDirectory;

    private boolean stranded = false;

    private File geneTableFile;

    private boolean filterGeneTable = true;

    private boolean convert2USeq = false;

    private float maximumAlignmentScore = 60;

    private float minimumMappingQualityScore = 13;

    private String genomeVersion;

    private String alignmentType;

    private String alignmentTypeSam = "sam";

    private String alignmentTypeEland = "eland";

    private String alignmentTypeNovoalign = "novoalign";

    private String alignmentTypeBed = "bed";

    private File[] treatmentReplicaDirectories;

    private File[] controlReplicaDirectories;

    private float minimumFDR = 0.5f;

    private boolean useDESeq = true;

    private File filteredGeneTableFile;

    private File geneRegionFile;

    private File geneExonFile;

    private File geneIntronFile;

    private File readCoverageTracks;

    private boolean skipParsingPointData = false;

    private File[] treatmentPointData;

    private File[] controlPointData;

    private int scanSeqsWindowSize = 150;

    private SmoothingWindowInfo[] smoothingWindowInfo;

    private File swiFile;

    private File scanSeqs;

    private int[] bpBuffers = new int[] { 0, 500, 5000 };

    public RNASeq2(String[] args) {
        long startTime = System.currentTimeMillis();
        processArgs(args);
        parseGeneTable();
        parsePointData();
        calculateFractions();
        readCoverage();
        scanSeqs();
        enrichedRegionMaker();
        definedRegionScanSeqs();
        cleanUp();
        double diffTime = ((double) (System.currentTimeMillis() - startTime)) / (1000 * 60);
        System.out.println("\nDone! " + Math.round(diffTime) + " minutes\n");
    }

    public void calculateFractions() {
        System.out.println("\n*******************************************************************************");
        System.out.println("Calculating fraction of exonic, intronic, and intergenic reads...");
        FilterPointData fp = new FilterPointData(geneExonFile, treatmentPointData);
        double totalObsTreatment = Num.sumArray(fp.getStartingNumObs());
        double nonExonicTreatment = Num.sumArray(fp.getEndingNumObs());
        fp = new FilterPointData(geneRegionFile, treatmentPointData);
        double nonGenicTreatment = Num.sumArray(fp.getEndingNumObs());
        double exonic = totalObsTreatment - nonExonicTreatment;
        double genic = totalObsTreatment - nonGenicTreatment;
        double intronic = genic - exonic;
        System.out.println("\tTreatment\t" + fetchFractionExonicIntronicIntergenic(exonic, intronic, totalObsTreatment));
        fp = new FilterPointData(geneExonFile, controlPointData);
        double totalObsControl = Num.sumArray(fp.getStartingNumObs());
        double nonExonicControl = Num.sumArray(fp.getEndingNumObs());
        fp = new FilterPointData(geneRegionFile, controlPointData);
        double nonGenicControl = Num.sumArray(fp.getEndingNumObs());
        exonic = totalObsControl - nonExonicControl;
        genic = totalObsControl - nonGenicControl;
        intronic = genic - exonic;
        System.out.println("\tControl\t" + fetchFractionExonicIntronicIntergenic(exonic, intronic, totalObsControl));
    }

    public void cleanUp() {
        System.out.println("\n*******************************************************************************");
        System.out.println("Compressing and possibly converting data...");
        if (swiFile != null && swiFile.exists()) IO.zipAndDelete(swiFile);
        if (convert2USeq) {
            if (readCoverageTracks != null) new Bar2USeq(readCoverageTracks, true);
            if (scanSeqs != null) new Bar2USeq(scanSeqs, true);
        }
    }

    public void definedRegionScanSeqs() {
        System.out.println("\n*******************************************************************************");
        String name;
        if (useDESeq) {
            name = "MultipleReplicaDefinedRegionScanSeqs";
        } else {
            name = "DefinedRegionScanSeqs";
        }
        System.out.println("Scanning genes for differential transcription with " + name + "...");
        File mrdrss = new File(resultsDirectory, name);
        if (mrdrss.exists() && mrdrss.isDirectory()) {
            System.out.println("\tWARNING: " + name + " folder exists, skipping analysis.  Delete " + mrdrss + " if you would like to reprocess and restart.");
            return;
        }
        mrdrss.mkdir();
        File exons = new File(mrdrss, "Exons");
        exons.mkdir();
        File introns = new File(mrdrss, "Introns");
        introns.mkdir();
        if (useDESeq) {
            MultipleReplicaDefinedRegionScanSeqs m = new MultipleReplicaDefinedRegionScanSeqs(treatmentPointData, controlPointData, exons, rApplication, filteredGeneTableFile, stranded, false);
            System.out.println("\n");
            m = new MultipleReplicaDefinedRegionScanSeqs(treatmentPointData, controlPointData, introns, rApplication, filteredGeneTableFile, stranded, true);
        } else {
            DefinedRegionScanSeqs m = new DefinedRegionScanSeqs(treatmentPointData, controlPointData, exons, rApplication, filteredGeneTableFile, false);
            System.out.println("\n");
            m = new DefinedRegionScanSeqs(treatmentPointData, controlPointData, introns, rApplication, filteredGeneTableFile, true);
        }
    }

    public String fetchFractionExonicIntronicIntergenic(double exonic, double intronic, double total) {
        double exonicFraction = exonic / total;
        double intronicFraction = intronic / total;
        double intergenic = total - (exonic + intronic);
        double intergenicFraction = intergenic / total;
        StringBuilder sb = new StringBuilder();
        sb.append(Num.formatNumber(exonicFraction, 3) + " (" + (int) exonic + "/" + (int) total + ")\t");
        sb.append(Num.formatNumber(intronicFraction, 3) + " (" + (int) intronic + "/" + (int) total + ")\t");
        sb.append(Num.formatNumber(intergenicFraction, 3) + " (" + (int) intergenic + "/" + (int) total + ")");
        return sb.toString();
    }

    public void enrichedRegionMaker() {
        System.out.println("\n*******************************************************************************");
        System.out.println("Building lists of differentially expressed transfrags from the window data using the EnrichedRegionMaker...");
        int[] scoreIndexes;
        float[] scoreThresholds;
        String name;
        if (useDESeq) {
            name = "MultipleReplicaScanSeqs";
            scoreIndexes = new int[] { 0, 1 };
            scoreThresholds = new float[] { 20, 1 };
        } else {
            name = "ScanSeqs";
            scoreIndexes = new int[] { 1, 4 };
            scoreThresholds = new float[] { 20, 1 };
        }
        if (swiFile == null) {
            System.out.println("\tWARNING: " + name + " was not run, skipping EnrichedRegionMaker.  Delete " + scanSeqs + " if you would like to reprocess and restart.");
            return;
        }
        for (int i = 0; i < bpBuffers.length; i++) {
            File dummy = new File(swiFile.getParentFile(), bpBuffers[i] + "BPBuffer.swi");
            new EnrichedRegionMaker(smoothingWindowInfo, dummy, scanSeqsWindowSize, treatmentPointData, controlPointData, false, geneExonFile, bpBuffers[i], rApplication, scoreIndexes, scoreThresholds);
            new EnrichedRegionMaker(smoothingWindowInfo, dummy, scanSeqsWindowSize, treatmentPointData, controlPointData, true, geneExonFile, bpBuffers[i], rApplication, scoreIndexes, scoreThresholds);
        }
    }

    public void scanSeqs() {
        System.out.println("\n*******************************************************************************");
        String name;
        if (useDESeq) name = "MultipleReplicaScanSeqs"; else name = "ScanSeqs";
        System.out.println("Scanning chromosomes for differential transcription with " + name + "...");
        scanSeqs = new File(resultsDirectory, name);
        if (scanSeqs.exists() && scanSeqs.isDirectory()) {
            System.out.println("\tWARNING: " + name + " folder exists, skipping analysis.  Delete " + scanSeqs + " if you would like to reprocess and restart.");
            return;
        }
        scanSeqs.mkdir();
        if (useDESeq) {
            MultipleReplicaScanSeqs ss = new MultipleReplicaScanSeqs(treatmentPointData, controlPointData, scanSeqs, rApplication, scanSeqsWindowSize, 0, minimumFDR);
            swiFile = ss.getSwiFile();
        } else {
            ScanSeqs ss = new ScanSeqs(treatmentPointData, controlPointData, scanSeqs, rApplication, scanSeqsWindowSize, 0, true);
            swiFile = ss.getSwiFile();
        }
        smoothingWindowInfo = (SmoothingWindowInfo[]) IO.fetchObject(swiFile);
    }

    public void readCoverage() {
        System.out.println("\n*******************************************************************************");
        System.out.println("Making relative ReadCoverage tracks (the number of reads per million mapped that intersect each bp) from the PointData...");
        readCoverageTracks = new File(resultsDirectory, "ReadCoverageTracks");
        if (readCoverageTracks.exists() && readCoverageTracks.isDirectory()) {
            System.out.println("\tWARNING: ReadCoverage folder exists, skipping track generation.  Delete " + readCoverageTracks + " if you would like to reprocess and restart.");
            return;
        }
        readCoverageTracks.mkdir();
        for (int i = 0; i < treatmentPointData.length; i++) {
            File rc = new File(readCoverageTracks, "T" + treatmentPointData[i].getParentFile().getName());
            rc.mkdir();
            new ReadCoverage(rc, new File[] { treatmentPointData[i] }, stranded);
        }
        if (treatmentPointData.length != 1) {
            File all = new File(readCoverageTracks, "CombineTreatment");
            all.mkdir();
            new ReadCoverage(all, treatmentPointData, stranded);
        }
        for (int i = 0; i < controlPointData.length; i++) {
            File rc = new File(readCoverageTracks, "C" + controlPointData[i].getParentFile().getName());
            rc.mkdir();
            new ReadCoverage(rc, new File[] { controlPointData[i] }, stranded);
        }
        if (controlPointData.length != 1) {
            File all = new File(readCoverageTracks, "CombineControl");
            all.mkdir();
            new ReadCoverage(all, controlPointData, stranded);
        }
    }

    public void parseGeneTable() {
        System.out.println("\n*******************************************************************************");
        System.out.println("Parsing gene table for gene regions and exons...");
        File annotation = new File(resultsDirectory, "Annotation");
        annotation.mkdir();
        filteredGeneTableFile = new File(annotation, "geneModels.ucsc");
        geneRegionFile = new File(annotation, "geneRegions.bed");
        geneExonFile = new File(annotation, "geneExons.bed");
        geneIntronFile = new File(annotation, "geneIntrons.bed");
        if (filteredGeneTableFile.exists() && geneRegionFile.exists() && geneExonFile.exists()) {
            System.out.println("\tWARNING: Annotation folder exists, skipping parsing.  Delete " + annotation + " if you would like to reprocess and restart, otherwise using files within.");
            return;
        }
        UCSCGeneModelTableReader reader = new UCSCGeneModelTableReader(geneTableFile, 0);
        if (filterGeneTable) {
            System.out.print("\tRemoving overlapping exons");
            reader.removeOverlappingExons();
        }
        System.out.println("\tWriting gene regions, exons, introns, and unique gene models...");
        reader.writeGeneTableToFile(filteredGeneTableFile);
        Coordinate[] geneRegions = reader.fetchGeneRegions();
        Coordinate.writeToFile(geneRegions, geneRegionFile);
        Coordinate[] exons = reader.fetchExons();
        Coordinate.writeToFile(exons, geneExonFile);
        reader.swapIntronsForExons();
        Coordinate[] introns = reader.fetchExons();
        Coordinate.writeToFile(introns, geneIntronFile);
    }

    public void parsePointData() {
        System.out.println("\n*******************************************************************************");
        System.out.println("Parsing PointData from raw alignments...");
        File pointDataDirectory = new File(resultsDirectory, "PointData");
        if (pointDataDirectory.exists()) {
            skipParsingPointData = true;
            System.out.println("\tWARNING: PointData directory exists, skipping parsing and using files within.  Delete " + pointDataDirectory + " and restart if you would like to reparse the alignments.");
        }
        pointDataDirectory.mkdir();
        File t = new File(pointDataDirectory, "TreatmentPointData");
        if (skipParsingPointData == false) System.out.println("\tParsing treatment PointData...");
        treatmentPointData = parsePointData(t, treatmentReplicaDirectories);
        File c = new File(pointDataDirectory, "ControlPointData");
        if (skipParsingPointData == false) System.out.println("\tParsing control PointData...");
        controlPointData = parsePointData(c, controlReplicaDirectories);
    }

    public File[] parsePointData(File saveDir, File[] replicaDirectories) {
        saveDir.mkdir();
        File[] pd = new File[replicaDirectories.length];
        for (int i = 0; i < replicaDirectories.length; i++) {
            File repDir = new File(saveDir, "Rep" + i);
            repDir.mkdir();
            pd[i] = new File(repDir, "PointData");
            if (skipParsingPointData) continue;
            System.out.print("\t" + repDir + "\t");
            if (alignmentType.equals(alignmentTypeEland)) {
                pd[i].mkdir();
                ElandParser p = new ElandParser(pd[i], IO.extractFiles(replicaDirectories[i]), maximumAlignmentScore, genomeVersion);
                System.out.println(p.getTotalNumMatch());
            } else if (alignmentType.equals(alignmentTypeSam)) {
                SamParser p = new SamParser(repDir, IO.extractFiles(replicaDirectories[i]), minimumMappingQualityScore, maximumAlignmentScore, genomeVersion);
                System.out.println(p.getNumberPassingAlignments());
            } else if (alignmentType.equals(alignmentTypeNovoalign)) {
                NovoalignParser p = new NovoalignParser(repDir, IO.extractFiles(replicaDirectories[i]), minimumMappingQualityScore, maximumAlignmentScore, genomeVersion, geneRegionFile);
                System.out.println(p.getNumberPassingAlignments());
            } else if (alignmentType.equals(alignmentTypeBed)) {
                Tag2Point p = new Tag2Point(repDir, IO.extractFiles(replicaDirectories[i]), genomeVersion);
                System.out.println(p.getNumberParsedRegions());
            }
        }
        return pd;
    }

    public void checkArgs() {
        System.out.println("\nChecking parameters...");
        boolean passed = true;
        StringBuilder notes = new StringBuilder();
        if (IO.checkJava() == false) {
            notes.append("\tYour java application is not >= 1.6 (type 'java -version' on the cmd line). Install the most recent java from http://www.java.com/en/download/ .\n");
            passed = false;
        }
        if (resultsDirectory != null) {
            if (resultsDirectory.exists()) notes.append("\tYour save results directory exits, may overwrite files within.\n"); else if (resultsDirectory.mkdirs() == false) {
                notes.append("\tCannot create your results directory? Does the parent directory exist?\n");
                passed = false;
            }
        } else {
            notes.append("\tPlease enter a directory in which to save your results.\n");
            passed = false;
        }
        if (rApplication == null || rApplication.canExecute() == false) {
            notes.append("\tCannot find or execute the R application -> " + rApplication + "\n");
            passed = false;
        } else {
            String errors = IO.runRCommandLookForError("library(DESeq)", rApplication, resultsDirectory);
            if (errors == null || errors.length() != 0) {
                passed = false;
                notes.append("\nError: Cannot find the required R library.  Did you install DESeq " + "(http://www-huber.embl.de/users/anders/DESeq/)?  See the author's websites for installation instructions. Once installed, " + "launch an R terminal and type 'library(DESeq)' to see if it is present. Error message:\n\t\t" + errors + "\n\n");
            }
            if (useDESeq == false) {
                errors = IO.runRCommandLookForError("library(qvalue)", rApplication, resultsDirectory);
                if (errors == null || errors.length() != 0) {
                    passed = false;
                    notes.append("\nError: Cannot find the required R library.  Did you install qvalue " + "(http://genomics.princeton.edu/storeylab/qvalue/)?  See the author's websites for installation instructions. Once installed, " + "launch an R terminal and type 'library(qvalue)' to see if it is present. R error message:\n\t\t" + errors + "\n\n");
                }
            }
        }
        if (geneTableFile == null || geneTableFile.canRead() == false) {
            notes.append("\tCannot find your geneTable -> " + geneTableFile + "\n");
            passed = false;
        }
        if (alignmentType != null) {
            alignmentType = alignmentType.toLowerCase();
            if (alignmentType.startsWith("s")) alignmentType = alignmentTypeSam; else if (alignmentType.startsWith("e")) alignmentType = alignmentTypeEland; else if (alignmentType.startsWith("n")) alignmentType = alignmentTypeNovoalign; else if (alignmentType.startsWith("b")) alignmentType = alignmentTypeBed; else {
                notes.append("\tYour alignment type does not appear to match any of the recognized formats? \n");
                passed = false;
            }
        } else {
            notes.append("\tPlease indicate what type of alignments you are providing. Either " + alignmentTypeSam + ", " + alignmentTypeEland + ", or " + alignmentTypeNovoalign + " .\n");
            passed = false;
        }
        if (genomeVersion != null) {
            if (ArchiveInfo.DAS2_VERSIONED_GENOME_FORM.matcher(genomeVersion).matches() == false) {
                notes.append("\tWARNING! Your versioned genome does not follow recommended form (e.g. H_sapiens_Mar_2006) -> " + genomeVersion + "\n");
            }
        } else {
            notes.append("\tPlease provide a versioned genome.\n");
            passed = false;
        }
        if (treatmentReplicaDirectories == null || controlReplicaDirectories == null) {
            notes.append("\tPlease enter, at minimum, one or more treatment and control alignment file directories.\n");
            passed = false;
        } else {
            if (treatmentReplicaDirectories.length == 1) {
                File[] otherDirs = IO.extractOnlyDirectories(treatmentReplicaDirectories[0]);
                if (otherDirs != null && otherDirs.length > 0) treatmentReplicaDirectories = otherDirs;
            }
            if (controlReplicaDirectories.length == 1) {
                File[] otherDirs = IO.extractOnlyDirectories(controlReplicaDirectories[0]);
                if (otherDirs != null && otherDirs.length > 0) controlReplicaDirectories = otherDirs;
            }
            for (int i = 0; i < treatmentReplicaDirectories.length; i++) {
                if (treatmentReplicaDirectories[i].isDirectory() == false) {
                    notes.append("\tThis treatment directory can't be found or isn't a directory? -> " + treatmentReplicaDirectories[i] + "\n");
                    passed = false;
                }
            }
            for (int i = 0; i < controlReplicaDirectories.length; i++) {
                if (controlReplicaDirectories[i].isDirectory() == false) {
                    notes.append("\tThis control directory can't be found or isn't a directory? -> " + controlReplicaDirectories[i] + "\n");
                    passed = false;
                }
            }
        }
        if (passed) {
            printParameters();
            if (notes.length() != 0) System.out.print("\nNotes: " + notes);
        } else {
            Misc.printErrAndExit("\nThe following problems were encountered when processing your parameter file.  Correct and restart. -> \n" + notes);
        }
    }

    public void printParameters() {
        System.out.println("\tResults directory = " + resultsDirectory);
        System.out.println("\tStranded = " + stranded);
        System.out.println("\tGene table = " + geneTableFile);
        System.out.println("\tRemove overlapping exons = " + filterGeneTable);
        System.out.println("\tAlignment type = " + alignmentType);
        System.out.println("\tMaximum alignment score = " + maximumAlignmentScore);
        System.out.println("\tMinimum mapping quality score = " + minimumMappingQualityScore);
        System.out.println("\tMinimum window FDR = " + minimumFDR);
        System.out.println("\tGenome version = " + genomeVersion);
        System.out.println("\tTreatment replica directories:\n\t\t" + IO.concatinateFileFullPathNames(treatmentReplicaDirectories, "\n\t\t"));
        System.out.println("\tControl replica directories:\n\t\t" + IO.concatinateFileFullPathNames(controlReplicaDirectories, "\n\t\t"));
        System.out.println("\tConvert bar graph files to useq format = " + convert2USeq);
        System.out.println("\tRun multiple replica DESeq analysis = " + useDESeq);
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            printDocs();
            System.exit(0);
        }
        new RNASeq2(args);
    }

    /**This method will process each argument and assign new variables*/
    public void processArgs(String[] args) {
        Pattern pat = Pattern.compile("-[a-z]");
        for (int i = 0; i < args.length; i++) {
            String lcArg = args[i].toLowerCase();
            Matcher mat = pat.matcher(lcArg);
            if (mat.matches()) {
                char test = args[i].charAt(1);
                try {
                    switch(test) {
                        case 's':
                            resultsDirectory = new File(args[++i]);
                            break;
                        case 't':
                            treatmentReplicaDirectories = IO.extractFiles(args[++i]);
                            break;
                        case 'c':
                            controlReplicaDirectories = IO.extractFiles(args[++i]);
                            break;
                        case 'y':
                            alignmentType = args[++i];
                            break;
                        case 'v':
                            genomeVersion = args[++i];
                            break;
                        case 'g':
                            geneTableFile = new File(args[++i]);
                            break;
                        case 'r':
                            rApplication = new File(args[++i]);
                            break;
                        case 'a':
                            maximumAlignmentScore = Float.parseFloat(args[++i]);
                            break;
                        case 'q':
                            minimumMappingQualityScore = Float.parseFloat(args[++i]);
                            break;
                        case 'o':
                            filterGeneTable = false;
                            break;
                        case 'd':
                            minimumFDR = Float.parseFloat(args[++i]);
                            break;
                        case 'n':
                            stranded = true;
                            break;
                        case 'u':
                            convert2USeq = true;
                            break;
                        case 'm':
                            useDESeq = false;
                            break;
                        case 'h':
                            printDocs();
                            System.exit(0);
                        default:
                            Misc.printExit("\nProblem, unknown option! " + mat.group());
                    }
                } catch (Exception e) {
                    Misc.printExit("\nSorry, something doesn't look right with this parameter request: -" + test);
                }
            }
        }
        checkArgs();
    }

    public static void printDocs() {
        System.out.println("\n" + "**************************************************************************************\n" + "**                                   RNASeq: Jan 2011                               **\n" + "**************************************************************************************\n" + "The RNASeq application is a wrapper for processing RNA-Seq data through a variety of\n" + "USeq applications. It:\n" + "   1) Parses raw alignments (sam, eland, bed, or novoalign) into binary PointData\n" + "   2) Makes relative ReadCoverage tracks from the PointData (reads per million mapped)\n" + "   3) Calculates fraction of reads that are exonic, intronic, and intergenic\n" + "   3) Runs MultipleReplicaScanSeqs and the EnrichedRegionMaker to identify novel RNAs\n" + "   4) Runs the MultipleReplicaDefinedRegionScanSeqs to score known genes for\n" + "        differential exonic and intronic expression as well as alternative splicing.\n" + "Use this application as a starting point in your transcriptome analysis.\n" + "\nOptions:\n" + "-s Save directory, full path.\n" + "-t Treatment alignment file directories, full path, comma delimited, no spaces, one\n" + "       for each biological replica. These should each contain one or more text\n" + "       alignment files (gz/zip OK) for a particular replica. Alternatively, provide\n" + "       one directory that contains multiple alignment file directories.\n" + "-c Control alignment file directories, ditto. \n" + "-y Type of alignments, either novoalign, sam, bed, or eland (sorted or export).\n" + "-n Data is stranded. Only analyze reads from the same strand as the annotation.\n" + "-v Genome version (e.g. H_sapiens_Feb_2009, M_musculus_Jul_2007), see UCSC FAQ,\n" + "      http://genome.ucsc.edu/FAQ/FAQreleases.\n" + "-g UCSC RefFlat or RefSeq gene table file, full path. See,\n" + "       http://genome.ucsc.edu/cgi-bin/hgTables, (name1 name2(optional) chrom strand\n" + "       txStart txEnd cdsStart cdsEnd exonCount (commaDelimited)exonStarts\n" + "       (commaDelimited)exonEnds). Example: ENSG00000183888 C1orf64 chr1 + 16203317\n" + "       16207889 16203385 16205428 2 16203317,16205000 16203467,16207889 . NOTE:\n" + "       this table should contain only one composite transcript per gene (e.g. use\n" + "       Ensembl genes NOT transcripts). Otherwise set the -o option.\n" + "-r Full path to R, defaults to '/usr/bin/R'. Be sure to install Ander's DESeq\n" + "       (http://www-huber.embl.de/users/anders/DESeq/) R library.\n" + "\nAdvanced Options:\n" + "-m Combine any replicas and run single replica analysis (ScanSeqs &\n" + "       DefinedRegionScanSeqs), defaults to using DESeq.\n" + "-a Maximum alignment score. Defaults to 60, smaller numbers are more stringent.\n" + "-q Minimum mapping quality score. Defaults to 13, bigger numbers are more stringent.\n" + "       This is a phred-scaled posterior probability that the mapping position of read\n" + "       is incorrect.\n" + "-d Minimum FDR threshold for filtering windows, defaults to 0.5\n" + "-o Don't delete overlapping exons from the gene table.\n" + "-u Convert bar graph folders to xxx.useq format.\n" + "\n" + "Example: java -Xmx2G -jar pathTo/USeq/Apps/RNASeq -y eland -v D_rerio_Dec_2008 -t \n" + "      /Data/PolIIRep1/,/Data/PolIIRep2/ -c /Data/PolIINRep1/,/Data/PolIINRep2/ -s\n" + "      /Data/Results/WtVsNull -g /Anno/zv8Genes.ucsc \n\n" + "**************************************************************************************\n");
    }

    public static LinkedHashMap<String, String> loadKeyValueFile(File file) {
        LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
        String line = null;
        try {
            BufferedReader in = new BufferedReader(new FileReader(file));
            Pattern equal = Pattern.compile("(.+)\\s*=\\s*(.+)");
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("#") || line.length() == 0) continue;
                Matcher mat = equal.matcher(line);
                if (mat.matches()) map.put(mat.group(1), mat.group(2)); else Misc.printErrAndExit("\tProblem parsing parameter file line -> " + line);
            }
        } catch (Exception e) {
            System.out.println("Prob with loadin parameter file.\n\t" + line);
            e.printStackTrace();
        }
        return map;
    }
}
