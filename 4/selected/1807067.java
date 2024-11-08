package src.lib.ioInterfaces;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Vector;
import src.lib.objects.AlignedRead;

/**
 * A class that writes ace files from other formats
 * 
 * @author tceszard
 * @version $Revision: 2777 $
 */
public class AceWriter {

    private static boolean display_version = true;

    private static Log_Buffer LB;

    private String file_name;

    /**
	 * Killer information:  Unlike Wig files, Bed files are Zero based!!!!!
	 */
    BufferedWriter bw;

    public AceWriter(Log_Buffer logbuffer, String file) {
        LB = logbuffer;
        file_name = file;
        if (display_version) {
            LB.Version("AceWriter", "$Revision: 2777 $");
            display_version = false;
        }
        try {
            bw = new BufferedWriter(new FileWriter(file));
        } catch (FileNotFoundException E) {
            LB.error("Error: Could not create new Ace file : " + file);
            LB.die();
        } catch (IOException io) {
            LB.error("Error: Coundn't create bed file : " + file);
            LB.error("Message thrown by Java environment (may be null):" + io.getMessage());
            LB.die();
        }
    }

    /**
	 * Split the string into as many lines as necessary with the given line length.
	 * @param st the string to split.
	 * @param length the length of each line.
	 * @return
	 */
    private static String split_in_lines(String st, int length) {
        StringBuffer buff = new StringBuffer();
        int i = 0;
        for (i = 0; i < st.length() - length; i += length) {
            buff.append(st.substring(i, i + length) + "\n");
        }
        buff.append(st.substring(i, st.length()));
        return buff.toString();
    }

    /**
	 * Create lines of dummy quality values
	 * @param value
	 * @param line_length
	 * @param total_length
	 * @return
	 */
    private static String dummy_quality_value(int value, int line_length, int total_length) {
        StringBuffer buff = new StringBuffer();
        for (int i = 0; i < total_length; i += line_length) {
            for (int j = 0; j < line_length && i + j < total_length; j++) {
                buff.append(value + " ");
            }
            buff.append("\n");
        }
        return buff.toString();
    }

    /**
	 * Create an ace file for the given contig and list of read
	 * @param aligned_reads
	 * @param fasta_sequence
	 * @param contig_name
	 * @param start_contig
	 */
    public void process_contig_and_reads(Vector<AlignedRead> aligned_reads, String fasta_sequence, String contig_name, int start_contig) {
        try {
            bw.write("AS 1 " + (aligned_reads.size() + 1));
            bw.newLine();
            bw.write("CO " + contig_name + " " + fasta_sequence.length() + " " + (aligned_reads.size() + 1) + " 1 U");
            bw.newLine();
            bw.write(split_in_lines(fasta_sequence, 9));
            bw.newLine();
            bw.write("BQ");
            bw.newLine();
            bw.write(dummy_quality_value(30, 20, fasta_sequence.length()));
            bw.newLine();
            StringBuffer rd_buff = new StringBuffer();
            for (AlignedRead read : aligned_reads) {
                char sense = 'U';
                String read_sequence = read.get_sequence();
                if (read.get_direction() == '-') {
                    sense = 'C';
                }
                bw.write("AF " + read.get_name() + " " + sense + " " + (read.get_alignStart() - start_contig + 1));
                bw.newLine();
                rd_buff.append("RD " + read.get_name() + " " + read_sequence.length() + " 0 0\n" + read_sequence + "\n\n");
                rd_buff.append("QA 1 " + read_sequence.length() + " 1 " + read_sequence.length() + "\n");
                rd_buff.append("DS PHD_FILE: x\n");
            }
            bw.write("AF CONTIG_" + contig_name + " U 1");
            bw.newLine();
            rd_buff.append("RD CONTIG_" + contig_name + " " + fasta_sequence.length() + " 0 0\n" + split_in_lines(fasta_sequence, 60) + "\n\n");
            rd_buff.append("QA 1 " + fasta_sequence.length() + " 1 " + fasta_sequence.length() + "\n");
            rd_buff.append("DS PHD_FILE: x\n");
            bw.write("BS 1 " + fasta_sequence.length() + " CONTIG_" + contig_name);
            bw.newLine();
            bw.newLine();
            bw.write(rd_buff.toString());
            bw.flush();
            bw.close();
        } catch (FileNotFoundException fnf) {
            LB.error(fnf.getMessage());
            LB.die();
        } catch (IOException ioe) {
            LB.error(ioe.getMessage());
            LB.die();
        }
        LB.notice("Output to: " + file_name);
    }
}
