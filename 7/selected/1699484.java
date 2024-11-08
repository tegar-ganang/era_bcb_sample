package src.fileUtilities;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Alright... lets say you can eland with the exact same sequences multiple
 * times but using different sequence lengths. Then you need to get all
 * alignmnets where there is a "Unique" alignment. Also, we'll stick a all the
 * NM reads into a file and maybe you can run more eland on them.
 * 
 * My suggestion is to run eland using the whole read length, then readlen -2,
 * -4, -6 down to 20 or so.
 * 
 * The first argument to this program should be the output file name.
 * 
 * This program assumes arg[1] is the shortest read length, arg[2] the next
 * shortest etc.
 * 
 * The filename format must be <name>.<length>.<anything>
 * 
 * @author Genome Sciences Centre
 * @version $Revision: 320 $
 */
public class ParseMultipleElands {

    /** SVNID identifier for program */
    public static final String SVNID = "$Id: ParseMultipleElands.java 320 2008-09-12 19:02:26Z apfejes $";

    private ParseMultipleElands() {
    }

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        int nmcnt = 0;
        int mmcnt = 0;
        int umcnt = 0;
        int qccnt = 0;
        int total = 0;
        int[] lenHisto = new int[100];
        if (args.length < 2) {
            System.out.println("usage: <output file name> <eland files>");
            System.exit(0);
        }
        String outName = args[0];
        String[] files = new String[args.length - 1];
        for (int i = 0; i < files.length; i++) {
            files[i] = args[i + 1];
        }
        System.out.println("Working with:");
        for (String s : files) System.out.println(s);
        int[] lengths = null;
        lengths = getLengths(files);
        BufferedReader[] elands = new BufferedReader[files.length];
        String[] lines = new String[files.length];
        String um_f = outName + ".um.eland";
        String nm_f = outName + ".nm.eland";
        String mm_f = outName + ".mm.eland";
        String qc_f = outName + ".qc.eland";
        String sum_f = outName + ".summary";
        System.out.println("Writing to:\n" + um_f + "\n" + nm_f + "\n" + mm_f + "\n" + qc_f + "\n" + sum_f);
        FileWriter um = null;
        FileWriter nm = null;
        FileWriter mm = null;
        FileWriter qc = null;
        FileWriter sum = null;
        try {
            um = new FileWriter(um_f);
            nm = new FileWriter(nm_f);
            mm = new FileWriter(mm_f);
            qc = new FileWriter(qc_f);
            sum = new FileWriter(sum_f);
        } catch (IOException io) {
            System.out.println("Can't create files.  Exiting");
            System.out.println("Message thrown by Java environment (may be null):" + io.getMessage());
            System.exit(0);
        }
        assert (um != null);
        assert (nm != null);
        assert (mm != null);
        assert (qc != null);
        assert (sum != null);
        for (int i = 0; i < elands.length; i++) {
            try {
                elands[i] = new BufferedReader(new FileReader(files[i]));
            } catch (FileNotFoundException FNF) {
                System.out.println("Could not open a file provided: " + files[i]);
            }
        }
        try {
            while (elands[0].ready()) {
                total++;
                if (total % 10000 == 0) {
                    System.out.print(".");
                }
                for (int i = 0; i < elands.length; i++) {
                    lines[i] = elands[i].readLine();
                }
                if (!sameRead(lines)) {
                    System.out.println("reads are not the same!");
                    write(lines);
                    System.exit(0);
                }
                if (hasQCerror(lines[0])) {
                    qccnt++;
                    qc.write(alnToFasta(lines[lines.length - 1], lengths[lines.length - 1]) + "\n");
                    continue;
                }
                if (hasNoMatch(lines[0])) {
                    nm.write(alnToFasta(lines[lines.length - 1], lengths[lines.length - 1]) + "\n");
                    nmcnt++;
                    continue;
                }
                boolean umatch = false;
                for (int i = lines.length - 1; i >= 0; i--) {
                    if (hasUniqueMatch(lines[i])) {
                        um.write(lines[i].substring(0, lines[i].indexOf('\t') + 1));
                        String seq = null;
                        seq = getSeq(lines[i], lengths[i]);
                        lenHisto[seq.length()]++;
                        um.write(seq);
                        um.write(lines[i].substring(lines[i].indexOf('\t', lines[i].indexOf(seq) + 1)));
                        um.write("\n");
                        umatch = true;
                        umcnt++;
                        break;
                    }
                }
                if (umatch) {
                    continue;
                }
                boolean mmatch = false;
                for (int i = lines.length - 1; i >= 0; i--) {
                    if (hasMultiMatch(lines[i])) {
                        alnToFasta(lines[i], lengths[i]);
                        mm.write(alnToFasta(lines[i], lengths[i]) + "\n");
                        mmatch = true;
                        mmcnt++;
                        break;
                    }
                }
                if (!mmatch) {
                    System.out.println("Error! No matches?!?!");
                    write(lines);
                }
            }
        } catch (IOException io) {
            System.out.println("Failure parsing file.");
            System.out.println("Message thrown by Java environment (may be null):" + io.getMessage());
        }
        try {
            sum.write("total\t" + total + "\n");
            sum.write("unique\t" + umcnt + "\n");
            sum.write("multi\t" + mmcnt + "\n");
            sum.write("none\t" + nmcnt + "\n");
            sum.write("QC\t" + qccnt + "\n");
            for (int i = 0; i < lenHisto.length; i++) {
                if (lenHisto[i] != 0) {
                    sum.write(i + "\t" + lenHisto[i] + "\n");
                }
            }
        } catch (IOException io) {
            System.out.println("Failure writing to files.");
            System.out.println("Message thrown by Java environment (may be null):" + io.getMessage());
        }
        System.out.printf("%n||Total reads |%d|%n||Unique Matches |%d|%n||Multimatches |%d|%n||No Match |%d|%n||Poor quality |%d|%n", total, umcnt, mmcnt, nmcnt, qccnt);
        try {
            um.close();
            nm.close();
            mm.close();
            sum.close();
            qc.close();
        } catch (IOException io) {
            System.out.println("Failure closing files.");
            System.out.println("Message thrown by Java environment (may be null):" + io.getMessage());
        }
    }

    /**
	 * 
	 * @param args
	 * @return
	 */
    public static int[] getLengths(String[] args) {
        int[] lengths = new int[args.length];
        for (int i = 0; i < args.length; i++) {
            int dex1 = args[i].indexOf('.');
            int dex2 = args[i].indexOf('.', dex1 + 1);
            lengths[i] = Integer.parseInt(args[i].substring(dex1 + 1, dex2));
            if (i > 0) {
                if (lengths[i] < lengths[i - 1]) {
                    System.out.println("Files are in wrong order, must be from smallest to largest.");
                    System.exit(0);
                }
            }
        }
        return lengths;
    }

    /**
	 * 
	 * @param lines
	 */
    public static void write(String[] lines) {
        for (String s : lines) System.out.println(s);
    }

    /**
	 * 
	 * @param align
	 * @return
	 */
    public static boolean hasQCerror(String align) {
        if (align.indexOf("\tQC") != -1) {
            return true;
        }
        return false;
    }

    /**
	 * 
	 * @param align
	 * @return
	 */
    public static boolean hasNoMatch(String align) {
        if (align.indexOf("\tNM\t") != -1) {
            return true;
        }
        return false;
    }

    /**
	 * 
	 * @param align
	 * @return
	 */
    public static boolean hasMultiMatch(String align) {
        if (align.indexOf("\tR0\t") != -1) {
            return true;
        }
        if (align.indexOf("\tR1\t") != -1) {
            return true;
        }
        if (align.indexOf("\tR2\t") != -1) {
            return true;
        }
        return false;
    }

    /**
	 * 
	 * @param align
	 * @return
	 */
    public static boolean hasUniqueMatch(String align) {
        if (align.indexOf("\tU0\t") != -1) {
            return true;
        }
        if (align.indexOf("\tU1\t") != -1) {
            return true;
        }
        if (align.indexOf("\tU2\t") != -1) {
            return true;
        }
        return false;
    }

    /**
	 * 
	 * @param align
	 * @param len
	 * @return
	 */
    public static String getSeq(String align, int len) {
        int dex = align.indexOf('\t');
        if (dex == -1) {
            System.out.println(align);
        }
        int dex2 = align.indexOf("\t", dex + 1);
        String seq = align.substring(dex + 1, dex2);
        return seq.substring(0, len);
    }

    /**
	 * 
	 * @param align
	 * @param len
	 * @return
	 */
    public static String alnToFasta(String align, int len) {
        int dex = align.indexOf('\t');
        if (dex == -1) {
            System.out.println(align);
        }
        String seqName = align.substring(0, dex);
        String seq = getSeq(align, len);
        if (seqName.charAt(0) != '>') {
            seqName = ">" + seqName;
        }
        return seqName + "\n" + seq;
    }

    /**
	 * 
	 * @param reads
	 * @return
	 */
    public static boolean sameRead(String[] reads) {
        if (reads.length < 2) {
            return true;
        }
        String r = reads[0].substring(0, reads[0].indexOf("\t"));
        for (int i = 1; i < reads.length; i++) {
            if (!reads[i].substring(0, reads[i].indexOf("\t")).equals(r)) {
                return false;
            }
        }
        return true;
    }
}
