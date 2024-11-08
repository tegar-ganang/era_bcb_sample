package termfrequency;

import fileutils.SparseFileUtils;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * This class gathers term frequencies from ascii documents and dumps them
 * into an ascii format.
 * @author Chris De Vries (chris@de-vries.ws)
 */
public class TermFrequency {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("Usage: termfrequency [directory to .txt files]" + " [output term frequency ascii file]");
            System.out.println("Recursively scans directory.");
            return;
        }
        String outFile = args[1];
        File dir = new File(args[0]);
        if (!dir.isDirectory()) {
            System.out.println(args[0] + " is not a directory.");
            return;
        }
        process(outFile, scan(dir));
    }

    /**
     * Processes files and writes ascii term frequency file in the following
     * format.
     *   <integer : document id> <string : term name 1>:<integer : term count 1> ... <string : term name n> <integer : term count n>
     * @param outFile  The file to write to.
     * @param files  The files to process.
     */
    private static void process(String outFile, List<File> files) throws IOException {
        BufferedWriter os = null;
        try {
            os = new BufferedWriter(new FileWriter(outFile));
            for (File file : files) {
                System.out.println("Processing " + file + " ...");
                int id = Integer.parseInt(getBase(file.toString()));
                SparseFileUtils.writeLine(os, id, readTerms(file));
            }
        } finally {
            if (os != null) {
                os.close();
            }
        }
    }

    /**
     * Get a filename without file extension.
     */
    private static String getBase(String filename) {
        String choppedFilename;
        String ext;
        int dotPlace = filename.lastIndexOf('.');
        if (dotPlace >= 0) {
            choppedFilename = filename.substring(0, dotPlace);
            ext = filename.substring(dotPlace + 1);
        } else {
            choppedFilename = filename;
            ext = "";
        }
        dotPlace = filename.lastIndexOf(File.separatorChar);
        if (dotPlace >= 0) {
            choppedFilename = choppedFilename.substring(dotPlace + 1);
        }
        return choppedFilename;
    }

    /**
     * Scan a directory recursively for all .txt files.
     * @param dir  The directory to scan.
     * @return  List of files found.
     */
    private static List<File> scan(File dir) throws IOException {
        FilenameFilter txtFilter = new FilenameFilter() {

            public boolean accept(File dir, String file) {
                return file.toLowerCase().endsWith(".txt");
            }
        };
        List<File> fileList = new ArrayList<File>();
        File[] files = dir.listFiles(txtFilter);
        for (File f : files) {
            fileList.add(f);
        }
        FileFilter dirs = new FileFilter() {

            public boolean accept(File file) {
                return file.isDirectory();
            }
        };
        File[] subDirs = dir.listFiles(dirs);
        for (File d : subDirs) {
            fileList.addAll(scan(d));
        }
        return fileList;
    }

    /**
     * Read a text file prepared by the XMLToText program.
     * It is assumed the input is terms separated by whitespace.
     * @param filename The file to read.
     * @return  Map of term -> count.
     */
    private static Map<String, Integer> readTerms(File file) throws IOException {
        Map<String, Integer> terms = new TreeMap<String, Integer>();
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(file));
            String line = null;
            while ((line = in.readLine()) != null) {
                String[] split = line.split(" ");
                for (String s : split) {
                    String term = s.trim();
                    if (term.length() != 0) {
                        term = term.toLowerCase();
                        Integer count = terms.get(term);
                        if (count == null) {
                            count = 1;
                        } else {
                            ++count;
                        }
                        terms.put(term, count);
                    }
                }
            }
        } finally {
            if (in != null) {
                in.close();
            }
        }
        return terms;
    }
}
