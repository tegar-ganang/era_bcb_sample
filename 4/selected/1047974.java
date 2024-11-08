package edu.utah.seq.useq.apps;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import util.gen.IO;
import util.gen.Misc;
import edu.utah.seq.useq.*;
import edu.utah.seq.useq.data.*;
import edu.utah.seq.parsers.*;

/**Converts directories of bar files to useq. */
public class Bar2USeq {

    private int rowChunkSize = 10000;

    private File[] barDirectories;

    private String versionedGenome = null;

    private boolean resetGenomeVersion = false;

    private int graphStyle = 0;

    private String color = null;

    private String description = null;

    private boolean deleteOriginals = false;

    private boolean replaceOriginals = false;

    private boolean verbose = true;

    public static String[] GRAPH_STYLES = { ArchiveInfo.GRAPH_STYLE_VALUE_BAR, ArchiveInfo.GRAPH_STYLE_VALUE_STAIRSTEP, ArchiveInfo.GRAPH_STYLE_VALUE_HEATMAP, ArchiveInfo.GRAPH_STYLE_VALUE_LINE };

    private File workingBinarySaveDirectory;

    private ArrayList<File> files2Zip = new ArrayList<File>();

    public static final Pattern PATTERN_STRAND = Pattern.compile(".*[+-\\.]$");

    public Bar2USeq(File rootDirectory, boolean deleteOriginals) {
        verbose = false;
        barDirectories = fetchBarDirectories(rootDirectory);
        this.deleteOriginals = deleteOriginals;
        convert();
    }

    public Bar2USeq(File rootDirectory, boolean deleteOriginals, String versionedGenome) {
        verbose = false;
        barDirectories = fetchBarDirectories(rootDirectory);
        this.deleteOriginals = deleteOriginals;
        this.versionedGenome = versionedGenome;
        convert();
    }

    public Bar2USeq(String[] args) {
        long startTime = System.currentTimeMillis();
        processArgs(args);
        convert();
        double diffTime = ((double) (System.currentTimeMillis() - startTime)) / 1000;
        if (verbose) System.out.println("\nDone! " + Math.round(diffTime) + " seconds\n");
    }

    public void convert() {
        for (int i = 0; i < barDirectories.length; i++) {
            if (verbose) System.out.println("Processing " + barDirectories[i]);
            workingBinarySaveDirectory = USeqUtilities.makeDirectory(barDirectories[i], ".TempDelMe");
            files2Zip.clear();
            if (resetGenomeVersion == false) versionedGenome = null;
            if (verbose) System.out.println("\tParsing, slicing, and writing binary data...");
            if (sliceWriteBarData(barDirectories[i]) == false) {
                USeqUtilities.deleteDirectory(workingBinarySaveDirectory);
                USeqUtilities.printErrAndExit("\nFailed to convert bar data to binary, aborting!\n");
            }
            writeReadMeTxt(barDirectories[i]);
            if (verbose) System.out.println("\tZipping...");
            String zipName = USeqUtilities.removeExtension(workingBinarySaveDirectory.getName()) + USeqUtilities.USEQ_EXTENSION_WITH_PERIOD;
            File zipFile = new File(barDirectories[i].getParentFile(), zipName);
            File[] files = new File[files2Zip.size()];
            files2Zip.toArray(files);
            if (USeqUtilities.zip(files, zipFile) == false) {
                USeqUtilities.deleteDirectory(workingBinarySaveDirectory);
                zipFile.delete();
                USeqUtilities.printErrAndExit("\nProblem zipping data for " + barDirectories[i]);
            }
            USeqUtilities.deleteDirectory(workingBinarySaveDirectory);
            if (replaceOriginals) {
                File moved = new File(barDirectories[i], zipName);
                if (zipFile.renameTo(moved)) {
                    IO.deleteFilesNotEndingInExtension(barDirectories[i], USeqUtilities.USEQ_EXTENSION_WITH_PERIOD);
                } else {
                    USeqUtilities.deleteDirectory(workingBinarySaveDirectory);
                    zipFile.delete();
                    USeqUtilities.printErrAndExit("\nAborting: problem moving useq file into " + barDirectories[i]);
                }
            } else if (deleteOriginals) USeqUtilities.deleteDirectory(barDirectories[i]);
        }
    }

    public static File[] extractBarFiles(File directory) {
        File[][] tot = new File[3][];
        tot[0] = USeqUtilities.extractFiles(directory, ".bar");
        tot[1] = USeqUtilities.extractFiles(directory, ".bar.zip");
        tot[2] = USeqUtilities.extractFiles(directory, ".bar.gz");
        return USeqUtilities.collapseFileArray(tot);
    }

    private void writeReadMeTxt(File sourceFile) {
        try {
            ArchiveInfo ai = new ArchiveInfo(versionedGenome, null);
            ai.setDataType(ArchiveInfo.DATA_TYPE_VALUE_GRAPH);
            ai.setInitialGraphStyle(GRAPH_STYLES[graphStyle]);
            ai.setOriginatingDataSource(sourceFile.toString());
            if (color != null) ai.setInitialColor(color);
            if (description != null) ai.setDescription(description);
            File readme = ai.writeReadMeFile(workingBinarySaveDirectory);
            files2Zip.add(0, readme);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**Calls the appropriate slice writer */
    private boolean sliceWriteBarData(File barDir) {
        File[] barFiles = extractBarFiles(barDir);
        if (barFiles == null || barFiles.length == 0) return false;
        Pattern STRANDED_BAR = Pattern.compile("(chr.+)_([\\.\\+-])_\\.bar.*");
        Pattern BAR = Pattern.compile("(chr.+)\\.bar.*");
        Pattern STRANDED_ODD = Pattern.compile("(.+)_([\\.\\+-])_\\.bar.*");
        Pattern ODD = Pattern.compile("(.+)\\.bar.*");
        for (int i = 0; i < barFiles.length; i++) {
            if (verbose) System.out.print(barFiles[i].getName() + " ");
            String strand = ".";
            String chrom = null;
            Matcher mat = STRANDED_BAR.matcher(barFiles[i].getName());
            if (mat.matches()) {
                chrom = mat.group(1);
                strand = mat.group(2);
            } else {
                mat = BAR.matcher(barFiles[i].getName());
                if (mat.matches()) {
                    chrom = mat.group(1);
                } else {
                    mat = STRANDED_ODD.matcher(barFiles[i].getName());
                    if (mat.matches()) {
                        chrom = mat.group(1);
                        strand = mat.group(2);
                    } else {
                        mat = ODD.matcher(barFiles[i].getName());
                        if (mat.matches()) {
                            chrom = mat.group(1);
                        } else {
                            System.err.println("Failed to parse chromosome information from " + barFiles[i].getName());
                            return false;
                        }
                    }
                }
            }
            BarParser bp = new BarParser();
            bp.readBarFile(barFiles[i], true);
            if (bp.getBasePositions() == null || bp.getBasePositions().length == 0) {
                if (verbose) System.out.print("<-No obs, skipping! ");
                continue;
            }
            if (bp.getStrand().equals(".") == false) strand = bp.getStrand();
            if (versionedGenome == null) versionedGenome = bp.getVersionedGenome();
            String style = bp.getTagValues().get(bp.GRAPH_TYPE_TAG);
            if (style != null) setGraphStyle(style);
            String graphColor = bp.getTagValues().get(bp.GRAPH_TYPE_COLOR_TAG);
            if (graphColor != null) color = graphColor;
            SliceInfo sliceInfo = new SliceInfo(chrom, strand, 0, 0, 0, null);
            PositionScore[] positions = new PositionScore[bp.getNumberPositionValues()];
            int[] pos = bp.getBasePositions();
            float[] scores = bp.getValues();
            int lastPosition = 0;
            int numBadPos = 0;
            for (int j = 0; j < pos.length; j++) {
                if (pos[j] < lastPosition) {
                    System.err.println("\n\nWarning: your bar file isn't sorted by position! See new position -> " + pos[j] + ", prior position " + lastPosition + ", line # " + j + ", replacing new with prior.");
                    pos[j] = lastPosition;
                    numBadPos++;
                    if (numBadPos > 10) Misc.printErrAndExit("\nError: too many mis sorted positions, aborting, see -> " + barFiles[i] + "\n");
                }
                lastPosition = pos[j];
                positions[j] = new PositionScore(pos[j], scores[j]);
            }
            PositionScoreData psd = new PositionScoreData(positions, sliceInfo);
            psd.sliceWritePositionScoreData(rowChunkSize, workingBinarySaveDirectory, files2Zip);
        }
        if (verbose) System.out.println();
        return true;
    }

    public void setGraphStyle(String style) {
        if (style.equals(BarParser.GRAPH_TYPE_BAR)) graphStyle = 0; else if (style.equals(BarParser.GRAPH_TYPE_STAIRSTEP)) graphStyle = 1; else if (style.equals(BarParser.GRAPH_TYPE_HEATMAP)) graphStyle = 2; else if (style.equals(BarParser.GRAPH_TYPE_LINE)) graphStyle = 3;
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            printDocs();
            System.exit(0);
        }
        new Bar2USeq(args);
    }

    public File[] fetchBarDirectories(File f) {
        if (f.isDirectory() == false) return null;
        File[] testFiles = IO.fetchFilesRecursively(f);
        HashSet<File> directories = new HashSet<File>();
        for (int i = 0; i < testFiles.length; i++) {
            if (testFiles[i].getName().contains(".bar")) directories.add(testFiles[i].getParentFile());
        }
        testFiles = new File[directories.size()];
        Iterator<File> it = directories.iterator();
        int index = 0;
        while (it.hasNext()) {
            testFiles[index++] = it.next();
        }
        return testFiles;
    }

    /**This method will process each argument and assign new variables*/
    public void processArgs(String[] args) {
        Pattern pat = Pattern.compile("-[a-z]");
        System.out.println("\nArguments: " + USeqUtilities.stringArrayToString(args, " ") + "\n");
        for (int i = 0; i < args.length; i++) {
            String lcArg = args[i].toLowerCase();
            Matcher mat = pat.matcher(lcArg);
            if (mat.matches()) {
                char test = args[i].charAt(1);
                try {
                    switch(test) {
                        case 'f':
                            barDirectories = fetchBarDirectories(new File(args[++i]));
                            break;
                        case 'i':
                            rowChunkSize = Integer.parseInt(args[++i]);
                            break;
                        case 'd':
                            description = args[++i];
                            break;
                        case 'e':
                            deleteOriginals = true;
                            break;
                        case 'm':
                            replaceOriginals = true;
                            break;
                        case 'h':
                            color = args[++i];
                            break;
                        case 'g':
                            versionedGenome = args[++i];
                            resetGenomeVersion = true;
                            break;
                        case 'r':
                            graphStyle = Integer.parseInt(args[++i]);
                            break;
                        default:
                            USeqUtilities.printExit("\nProblem, unknown option! " + mat.group() + " . Aborting.\n");
                    }
                } catch (Exception e) {
                    USeqUtilities.printExit("\nSorry, something doesn't look right with this parameter: -" + test + "\n");
                }
            }
        }
        if (barDirectories == null || barDirectories.length == 0) USeqUtilities.printErrAndExit("\nCannot find your bar directories?\n");
        if (color != null) {
            if (ArchiveInfo.COLOR_HEX_FORM.matcher(color).matches() == false) {
                USeqUtilities.printErrAndExit("\nCannot parse a hexidecimal color code (e.g. #CCFF33) from your color choice?! -> " + color);
            }
        }
    }

    public static void printDocs() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < GRAPH_STYLES.length; i++) {
            sb.append("      " + i + "\t" + GRAPH_STYLES[i] + "\n");
        }
        System.out.println("\n" + "**************************************************************************************\n" + "**                                 Bar 2 USeq: Mar 2011                             **\n" + "**************************************************************************************\n" + "Recurses through directories and sub directories of xxx.bar(.zip/.gz OK) files\n" + "converting them to xxx.useq files (http://useq.sourceforge.net/useqArchiveFormat.html).  \n\n" + "Required Options:\n" + "-f Full path directory containing bar files or directories of bar files.\n" + "\n" + "Default Options:\n" + "-i Index size for slicing split chromosome data (e.g. # rows per file),\n" + "      defaults to 10000.\n" + "-r For graphs, select a style, defaults to 0\n" + sb + "-h Color, hexadecimal (e.g. #6633FF), enclose in quotations\n" + "-d Description, enclose in quotations \n" + "-g Reset genome version, defaults to that indicated by the bar files.\n" + "-e Delete original folders, use with caution.\n" + "-m Replace bar files with new xxx.useq file in bar file directory, use with caution.\n" + "\nExample: java -Xmx4G -jar pathTo/USeq/Apps/Bar2USeq -f\n" + "      /AnalysisResults/ -i 5000 -h '#6633FF' -g D_rerio_Jul_2010 \n" + "      -d 'Final processed chIP-Seq results for Bcd and Hunchback, 30M reads' \n\n" + "**************************************************************************************\n");
    }
}
