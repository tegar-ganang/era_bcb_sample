package gov.lanl.arc.dkImpl;

import java.io.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.*;

/**
 * Created by IntelliJ IDEA. User: lc Date: Jan 5, 2004 Time: 10:08:40 AM To
 * change this template use Options | File Templates.
 */
public class ExtractCDX {

    private static Map legalFields = new TreeMap();

    static {
        legalFields.put("A", "canonized url");
        legalFields.put("B", "news group");
        legalFields.put("C", "rulespace category");
        legalFields.put("D", "compressed dat file offset");
        legalFields.put("F", "canonized frame");
        legalFields.put("G", "multi-columm language description");
        legalFields.put("H", "canonized host");
        legalFields.put("I", "canonized image");
        legalFields.put("J", "canonized jump point");
        legalFields.put("K", "Some weird FBIS what's changed kinda thing");
        legalFields.put("L", "canonized link");
        legalFields.put("M", "meta tags (AIF)");
        legalFields.put("N", "massaged url");
        legalFields.put("P", "canonized path");
        legalFields.put("Q", "language string");
        legalFields.put("R", "canonized redirect");
        legalFields.put("U", "uniqness");
        legalFields.put("V", "compressed arc file offset");
        legalFields.put("X", "canonized url in other href tages");
        legalFields.put("Y", "canonized url in other src tags");
        legalFields.put("Z", "canonized url found in script");
        legalFields.put("a", "original url");
        legalFields.put("b", "date");
        legalFields.put("c", "old style checksum");
        legalFields.put("d", "uncompressed dat file offset");
        legalFields.put("e", "IP");
        legalFields.put("f", "frame");
        legalFields.put("g", "file name");
        legalFields.put("h", "original host");
        legalFields.put("i", "image");
        legalFields.put("j", "original jump point");
        legalFields.put("k", "new style checksum");
        legalFields.put("l", "link");
        legalFields.put("m", "mime type of original document");
        legalFields.put("n", "arc document length");
        legalFields.put("o", "port");
        legalFields.put("p", "original path");
        legalFields.put("r", "redirect");
        legalFields.put("s", "response code");
        legalFields.put("t", "title");
        legalFields.put("v", "uncompressed arc file offset");
        legalFields.put("x", "url in other href tages");
        legalFields.put("y", "url in other src tags");
        legalFields.put("z", "url found in script");
        legalFields.put("#", "comment");
    }

    public static void main(String[] argv) {
        if (argv.length == 0) {
            printUsage(null);
            System.exit(1);
        }
        String fields = null;
        int argindex = 0;
        if (argv[argindex].charAt(0) == '-' && !argv[argindex].equals("--")) {
            if (argv[argindex].equals("-f")) {
                if (argindex == argv.length - 1) {
                    printUsage("Option -f takes a list of field specifiers as argument");
                    System.exit(1);
                }
                argindex++;
                fields = argv[argindex++];
            } else if (argv[argindex].equals("-h")) {
                printHelp();
                System.exit(1);
            } else {
                printUsage("Illegal option " + argv[argindex]);
            }
        }
        for (int i = 0; i < argv.length; i++) {
            if (argv[i].endsWith(".dat") || argv[i].endsWith(".DAT")) {
                extractFromDat(argv[i], fields);
            } else if (argv[i].endsWith(".arc") || argv[i].endsWith(".ARC") || argv[i].endsWith(".arc.gz") || argv[i].endsWith(".ARC.gz")) {
                extractFromArc(argv[i], fields);
            } else {
                System.err.println("Not an arc or dat file: " + argv[i]);
                continue;
            }
        }
    }

    private static void printHelp() {
        System.err.println("Usage: java dk.netarkivet.ArcUtil.ExtractCDX [-f fields] <files>");
        System.err.println("Legal field keys are:");
        for (Iterator it = legalFields.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            System.out.println("\t" + entry.getKey() + "\t" + entry.getValue());
        }
    }

    private static void printUsage(String s) {
        if (s != null) System.err.println(s);
        System.err.println("Usage: java dk.netarkivet.ArcUtil.ExtractCDX [-f fields] <files>");
        System.out.print("Legal field indicators are: ");
        for (Iterator it = legalFields.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            System.out.print(entry.getKey());
        }
        System.out.println();
    }

    /**
     * this is new function serves as indexing from registry program
     */
    public static void extractFromArc(String s) throws Exception {
        String fields = null;
        long offset = 0;
        String idxname = s;
        int jj = idxname.lastIndexOf(".");
        idxname = idxname.substring(0, jj);
        OutputStream out = new FileOutputStream(idxname + ".idxtmp");
        PrintWriter pw = new PrintWriter(out);
        boolean is_compressed = s.endsWith(".gz");
        if (fields == null) fields = (is_compressed ? "AebmngV" : "Aebmngv");
        String[] fieldarray = parseFields(fields);
        File f = new File(s);
        Hashtable fieldsread = new Hashtable();
        try {
            RandomAccessFile file = new RandomAccessFile(f, "r");
            try {
                do {
                    ARCInputStream in = new ARCInputStream(file, is_compressed, offset);
                    fieldsread.put("A", in.getUrl());
                    fieldsread.put("e", in.getIp());
                    fieldsread.put("b", in.getDate());
                    fieldsread.put("m", in.getMime());
                    fieldsread.put("n", in.getLength());
                    fieldsread.put(is_compressed ? "V" : "v", Long.toString(offset));
                    fieldsread.put("g", f.getName());
                    for (int i = 0; i < fieldarray.length; i++) {
                        pw.write((i > 0 ? "\t" : "") + fieldsread.get(fieldarray[i]));
                    }
                    pw.println();
                    in.readAll();
                    offset = file.getFilePointer();
                } while (offset < f.length());
            } finally {
                pw.flush();
                if (pw.checkError()) {
                    throw new Exception("problem with indexing");
                }
                pw.close();
                file.close();
            }
        } catch (IOException e) {
            System.out.println("Error reading from " + s + ": " + e);
        }
    }

    /**
     * Extract CDX information from an ARC file. The resulting information will
     * be printed to stdout.
     * 
     * @param s
     *            The name for an ARC file, compressed or uncompressed.
     * @param fields
     *            An array of fields to write out for CDX. If null, the standard
     *            set Aemgv will be used for uncompressed arc files and AemgV
     *            for compressed arc files. See field definitions at
     *            http://www.archive.org/web/researcher/cdx_legend.php
     */
    public static void extractFromArc(String s, String fields) {
        long offset = 0;
        boolean is_compressed = s.endsWith(".gz");
        if (fields == null) fields = (is_compressed ? "AebmngV" : "Aebmngv");
        String[] fieldarray = parseFields(fields);
        File f = new File(s);
        Hashtable fieldsread = new Hashtable();
        try {
            RandomAccessFile file = new RandomAccessFile(f, "r");
            try {
                do {
                    ARCInputStream in = new ARCInputStream(file, is_compressed, offset);
                    fieldsread.put("A", in.getUrl());
                    fieldsread.put("e", in.getIp());
                    fieldsread.put("b", in.getDate());
                    fieldsread.put("m", in.getMime());
                    fieldsread.put("n", in.getLength());
                    fieldsread.put(is_compressed ? "V" : "v", Long.toString(offset));
                    fieldsread.put("g", f.getName());
                    printFields(fieldsread, fieldarray);
                    in.readAll();
                    offset = file.getFilePointer();
                } while (offset < f.length());
            } finally {
                file.close();
            }
        } catch (IOException e) {
            System.out.println("Error reading from " + s + ": " + e);
        }
    }

    /**
     * Parse a fieldspec in string form, checking validity.
     * 
     * @param fieldspec
     *            A string containing characters from the letters specified in
     *            http://www.archive.org/web/researcher/cdx_legend.php
     * @return The same characters as elements of an array.
     */
    public static String[] parseFields(String fieldspec) {
        char[] chars = fieldspec.toCharArray();
        String[] strings = new String[chars.length];
        for (int i = 0; i < chars.length; i++) {
            strings[i] = Character.toString(chars[i]);
            if (!legalFields.containsKey(strings[i])) {
                System.err.println("Illegal field key " + strings[i] + ". Legal keys are:");
                for (Iterator it = legalFields.entrySet().iterator(); it.hasNext(); ) {
                    Map.Entry entry = (Map.Entry) it.next();
                    System.out.println("\t" + entry.getKey() + "\t" + entry.getValue());
                }
                System.exit(1);
            }
        }
        return strings;
    }

    /**
     * Print the values found for a set of fields.
     * 
     * @param fieldsread
     *            A hashtable of values indexed by field letters
     * @param fields
     *            An array indicating which fields to write, and in what order.
     */
    private static void printFields(Hashtable fieldsread, String[] fields) {
        for (int i = 0; i < fields.length; i++) {
            System.out.print((i > 0 ? "\t" : "") + fieldsread.get(fields[i]));
        }
        System.out.println();
    }

    private static void writeFields(PrintWriter pw, Hashtable fieldsread, String[] fields) {
        for (int i = 0; i < fields.length; i++) {
            pw.write((i > 0 ? "\t" : "") + fieldsread.get(fields[i]));
            System.out.print((i > 0 ? "\t" : "") + fieldsread.get(fields[i]));
        }
        pw.println();
    }

    static final Matcher headmatch = Pattern.compile("(\\S+)\\s+([0-9.]+)\\s+(\\d+)\\s+(\\S+)\\s+(\\d+)").matcher("");

    private static final int READ_HEADER = 0;

    private static final int READ_FIELDS = 1;

    /**
     * Extract CDX information from a DAT file. The resulting information will
     * be printed to stdout. Any entries not found are printed as 'null'
     * 
     * @param s
     *            The name of a DAT file
     * @param fields
     *            An array of fields to write out for CDX. If null, the standard
     *            set AbeamsckrVvDdgMn will be used. See field definitions at
     *            http://www.archive.org/web/researcher/cdx_legend.php
     */
    public static void extractFromDat(String s, String fields) {
        if (fields == null) fields = "AbeamsckrVvDdgMn";
        String[] fieldsarray = parseFields(fields);
        int state = READ_HEADER;
        try {
            String line = null;
            BufferedReader in = new BufferedReader(new FileReader(new File(s)));
            Hashtable fieldsread = new Hashtable();
            while ((line = in.readLine()) != null) {
                if (state == READ_HEADER) {
                    headmatch.reset(line);
                    if (headmatch.matches()) {
                        fieldsread.put("A", headmatch.group(1));
                        fieldsread.put("e", headmatch.group(2));
                        fieldsread.put("b", headmatch.group(3));
                        state = READ_FIELDS;
                    }
                } else if (state == READ_FIELDS) {
                    if (line.length() == 0) {
                        printFields(fieldsread, fieldsarray);
                        state = READ_HEADER;
                    } else {
                        fieldsread.put(line.substring(0, 1), line.substring(2));
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading from " + s + ": " + e);
        }
    }
}
