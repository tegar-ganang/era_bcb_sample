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
 * @author Genome Sciences Centre
 * @version $Revision: 320 $
 */
public class ParseMultipleElandsLossless {

    public static final String SVNID = "$Id: ParseMultipleElandsLossless.java 320 2008-09-12 19:02:26Z apfejes $";

    /** dummy Constructor */
    private ParseMultipleElandsLossless() {
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
        for (String s : files) {
            System.out.println(s);
        }
        int[] lengths = getLengths(files);
        BufferedReader[] elands = new BufferedReader[files.length];
        String[] lines = new String[files.length];
        String all_f = outName + ".all.eland";
        String sum_f = outName + ".summary";
        System.out.println("Writing to:\n" + all_f + "\n" + sum_f);
        FileWriter fw_all = null;
        FileWriter sum = null;
        try {
            fw_all = new FileWriter(all_f);
            sum = new FileWriter(sum_f);
        } catch (IOException io) {
            System.out.println("Can't initialize files!");
            System.out.println("Message thrown by Java environment (may be null):" + io.getMessage());
            System.exit(0);
        }
        assert (fw_all != null);
        assert (sum != null);
        for (int i = 0; i < elands.length; i++) {
            try {
                elands[i] = new BufferedReader(new FileReader(files[i]));
            } catch (FileNotFoundException e) {
                System.out.println("Can't find file: " + files[i]);
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
                    fw_all.write(alnToLine(lines[lines.length - 1], lengths[lines.length - 1]) + "\tQC\n");
                    continue;
                }
                if (hasNoMatch(lines[0])) {
                    try {
                        fw_all.write(alnToLine(lines[lines.length - 1], lengths[lines.length - 1]) + "\tNM\n");
                    } catch (IOException io) {
                        System.out.println("Warning sequence length not as long as line length: " + lines[lines.length - 1]);
                        System.out.println("Message thrown by Java environment (may be null):" + io.getMessage());
                        continue;
                    }
                    nmcnt++;
                    continue;
                }
                boolean umatch = false;
                for (int i = lines.length - 1; i >= 0; i--) {
                    if (hasUniqueMatch(lines[i])) {
                        fw_all.write(lines[i].substring(0, lines[i].indexOf('\t') + 1));
                        String seq = null;
                        seq = getSeq(lines[i], lengths[i]);
                        lenHisto[seq.length()]++;
                        fw_all.write(seq);
                        fw_all.write(lines[i].substring(lines[i].indexOf('\t', lines[i].indexOf(seq) + 1)));
                        fw_all.write("\n");
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
                        alnToLine(lines[i], lengths[i]);
                        fw_all.write(alnToLine(lines[i], lengths[i]) + "\tMM\n");
                        mmatch = true;
                        mmcnt++;
                        break;
                    }
                }
                if (!mmatch) {
                    System.out.println("Error! No matches?!?!");
                    for (String l : lines) System.out.println(l);
                }
            }
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
            System.out.printf("%n||Total reads |%d|%n||Unique Matches |%d|%n||Multimatches |%d|%n||No Match |%d|%n||Poor quality |%d|%n", total, umcnt, mmcnt, nmcnt, qccnt);
            fw_all.close();
            sum.close();
        } catch (IOException io) {
            System.out.println("Error Iterating on file");
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
    public static String alnToLine(String align, int len) {
        int dex = align.indexOf('\t');
        if (dex == -1) {
            System.out.println(align);
        }
        String seqName = align.substring(0, dex);
        String seq = getSeq(align, len);
        if (seqName.charAt(0) != '>') {
            seqName = ">" + seqName;
        }
        return seqName + "\t" + seq;
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
