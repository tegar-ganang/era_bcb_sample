package src.lib.analysisTools;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.ensembl.datamodel.Exon;
import org.ensembl.datamodel.Location;
import org.ensembl.datamodel.Sequence;
import org.ensembl.datamodel.Transcript;
import org.ensembl.datamodel.Translation;
import org.ensembl.util.SequenceUtil;
import src.lib.Chromosome;
import src.lib.Constants;
import src.lib.Ensembl;
import src.lib.SNPDB;
import src.lib.Utilities;
import src.lib.ioInterfaces.Log_Buffer;
import src.lib.ioInterfaces.PSNPwriter;
import src.lib.objects.SNP;

/**
 * @version $Revision: 2608 $
 * @author 
 */
public class Process_Exons {

    private static final int PRINT_SEQ_SIZE = 26;

    private Process_Exons() {
    }

    /**
	 * 
	 * This process provides the main functionality of the algorithm. It
	 * downloads all transcripts and exons, as well as the mapping (from the
	 * ensembl mysql database), and then performs the mapping automagically.
	 * 
	 * the version from PSNPAnalysis
	 * 
	 * @param LB
	 * @param map_transcript_exon
	 * @param Const
	 * @param Chr
	 * @param current_chromosome
	 * @param input_species
	 * @param aligner
	 * @param output_path
	 * @param min_percent
	 * @param min_observed
	 */
    public static void process_exons(Log_Buffer LB, HashMap<String, Integer> map_transcript_exon, Ensembl Const, Chromosome Chr, int current_chromosome, String input_species, String aligner, String output_path, float min_percent, int min_observed) {
        LB.notice("Fetching Transcript/Exon Locations for chromosome...                  ");
        Location loc = new Location("chromosome", Const.get_chromosome(current_chromosome));
        List<Exon> exon_list = Ensembl.get_exons(loc);
        List<Transcript> transcript_list = Ensembl.get_transcripts(loc);
        LB.notice("Retrieving all known SNPs from Ensembl variation db for chromosome... ");
        SNPDB[] SNP_list = null;
        if (aligner.equals(Constants.FILE_TYPE_ELAND) || aligner.equals(Constants.FILE_TYPE_MAQ) || aligner.equals(Constants.FILE_TYPE_SAM) || aligner.equals(Constants.FILE_TYPE_BAM)) {
            SNP_list = SNPDB.get_variation_SNPs(Const, current_chromosome);
        } else if (aligner.equals("slider")) {
            LB.error("aligner type 'slider' no longer supported");
            LB.die();
        } else {
            LB.error("Unrecognized aligner in Process exons");
            LB.die();
        }
        PSNPwriter psnpfile = new PSNPwriter(output_path + Const.get_chromosome(current_chromosome) + ".allsnps", LB);
        LB.notice("Processing Transcripts/Exons...");
        for (Transcript t : transcript_list) {
            Location tl = t.getLocation();
            if (!tl.overlaps(loc)) {
                continue;
            }
            long t_id = t.getInternalID();
            String Accession = t.getAccessionID();
            String Display = t.getDisplayName();
            StringBuffer NewTranscript = new StringBuffer();
            Translation t_translation = t.getTranslation();
            if (t.getCDNALocation().getEnd() > Chr.get_canonical_sequence_length()) {
                continue;
            }
            tl.getStart();
            int strand = tl.getStrand();
            Exon[] exon_sublist = get_exon_sublist(map_transcript_exon, t_id, exon_list);
            ArrayList<SNP> trans_snps = new ArrayList<SNP>(100);
            for (Exon exon : exon_sublist) {
                Location el = exon.getLocation();
                int exon_start = el.getStart();
                int exon_end = el.getEnd();
                if (exon_start > exon_end) {
                    LB.warning("Start greater than end!");
                    LB.warning("Strand: " + strand);
                }
                Sequence exon_seq = exon.getSequence();
                String exon_baseSeq = exon_seq.getString();
                if (t_translation != null) {
                    trans_snps.addAll(build_snp_list(Chr, exon_start, exon_end, strand, NewTranscript.length(), min_percent, min_observed));
                }
                NewTranscript.append(exon_baseSeq);
            }
            if (t_translation != null) {
                String codingDNA = t_translation.getSequence().getString();
                codingDNA = Utilities.remove_spurious_Ns(codingDNA, input_species);
                codingDNA = Utilities.remove_all_Xs(codingDNA);
                int index_start = NewTranscript.indexOf(codingDNA);
                if (index_start == -1) {
                    LB.warning("Can't find sequence in transcript: " + Accession + " Error!");
                    LB.warning("NewTranscript: " + NewTranscript);
                    LB.warning("codingDNA:     " + codingDNA);
                } else {
                    String chromosome = Const.get_chromosome(current_chromosome);
                    String peptide = t_translation.getPeptide();
                    CoreFunctions.compare_and_print_transcripts(LB, codingDNA, index_start, peptide, trans_snps, psnpfile, strand, NewTranscript, SNP_list, Accession, Display, chromosome, Chr);
                }
            }
            trans_snps.clear();
        }
        exon_list.clear();
        psnpfile.close();
    }

    /**
	 * @param map_transcript_exon
	 * @param t_id
	 * @param exon_list
	 * @return
	 */
    private static Exon[] get_exon_sublist(HashMap<String, Integer> map_transcript_exon, long t_id, List<Exon> exon_list) {
        Exon[] exon_sublist = null;
        int rank = 1;
        String key = null;
        ArrayList<Integer> exon_ids = new ArrayList<Integer>();
        while (true) {
            key = t_id + "-" + rank;
            Integer ex = map_transcript_exon.get(key);
            if (ex == null) {
                break;
            }
            exon_ids.add(ex);
            rank++;
        }
        exon_sublist = new Exon[exon_ids.size()];
        for (Exon E : exon_list) {
            for (int y = 0; y < exon_ids.size(); y++) {
                if (E.getInternalID() == exon_ids.get(y)) {
                    exon_sublist[y] = E;
                }
            }
        }
        return exon_sublist;
    }

    /**
	 * 
	 * @param Chr
	 * @param exon_start
	 * @param exon_end
	 * @param strand
	 * @param length
	 * @param min_percent
	 * @param min_observed
	 * @return
	 */
    private static ArrayList<SNP> build_snp_list(Chromosome Chr, int exon_start, int exon_end, int strand, int length, float min_percent, int min_observed) {
        ArrayList<SNP> VSNP = new ArrayList<SNP>();
        VSNP = Chr.get_local_SNPs(exon_start, exon_end, min_percent, min_observed);
        int y = 0;
        while (y < VSNP.size()) {
            SNP sp = VSNP.get(y);
            float ratio = (float) sp.get_coverage_snp() / (float) sp.get_total_coverage();
            if ((ratio >= min_percent) && (sp.get_total_coverage() >= min_observed)) {
                if (strand == -1) {
                    sp.set_misc_value(exon_end - sp.get_position() + length);
                } else {
                    sp.set_misc_value(sp.get_position() + length - exon_start);
                }
                VSNP.set(y, sp);
            } else {
                VSNP.remove(y);
                y--;
            }
            y++;
        }
        return VSNP;
    }

    /**
	 * Version used by GenomicAlignmentAnalysis and PSNPAnalysis when "-transcript_profile" flag is used.
	 * Also does exon coverage.
	 * @param LB
	 * @param Const
	 * @param Chr
	 * @param list
	 * @param current_chromosome
	 * @param input_species
	 * @param output_path
	 * @param min_percent
	 * @param min_observed
	 */
    public static void process_exons(Log_Buffer LB, Ensembl Const, Chromosome Chr, List<Transcript> list, int current_chromosome, String input_species, String output_path, float min_percent, int min_observed) {
        BufferedWriter exonfile = null;
        BufferedWriter psnpfile = null;
        String psnpfile_name = output_path + Const.get_chromosome(current_chromosome) + ".psnps";
        String exonfile_name = output_path + Const.get_chromosome(current_chromosome) + ".exons";
        String chr_name = Const.get_chromosome(current_chromosome);
        try {
            exonfile = new BufferedWriter(new FileWriter(exonfile_name));
            psnpfile = new BufferedWriter(new FileWriter(psnpfile_name));
        } catch (IOException io) {
            LB.error("Can't create files : ");
            LB.error("\t" + psnpfile_name);
            LB.error("\t" + exonfile_name);
            LB.error("Message thrown by Java environment (may be null):" + io.getMessage());
            LB.die();
        }
        assert (psnpfile != null);
        assert (exonfile != null);
        for (Transcript t : list) {
            Location tl = t.getLocation();
            String Accession = t.getAccessionID();
            String Display = t.getDisplayName();
            StringBuffer NewTranscript = new StringBuffer();
            Translation t_translation = t.getTranslation();
            if (t.getCDNALocation().getEnd() > Chr.get_canonical_sequence_length()) {
                continue;
            }
            tl.getStart();
            int strand = tl.getStrand();
            int read_transcript = Chr.get_starts_and_ends(tl.getStart(), tl.getEnd());
            int read_trans_exons = 0;
            ArrayList<SNP> trans_snps = new ArrayList<SNP>();
            @SuppressWarnings("unchecked") List<Exon> exon_list = t.getExons();
            for (Exon exon : exon_list) {
                Location el = exon.getLocation();
                int exon_start = el.getStart();
                int exon_end = el.getEnd();
                if (exon_start > exon_end) {
                    LB.warning("Start greater than end!");
                    LB.warning("Strand: " + strand);
                }
                Sequence exon_seq = exon.getSequence();
                String exon_baseSeq = exon_seq.getString();
                float exon_avg_coverage = Chr.get_coverage_sum(exon_start, exon_end);
                float exon_coverage = Chr.get_coverage(exon_start, exon_end);
                int starts = Chr.get_starts_and_ends(exon_start, exon_end);
                try {
                    exonfile.write(Accession + "\t " + Display + "-" + exon_start + "-" + exon_end + "\t" + starts + "\t" + Utilities.DecimalPoints(exon_avg_coverage, 2) + "\t" + Utilities.DecimalPoints(exon_coverage * Constants.PERCENT_100, 2));
                    exonfile.newLine();
                } catch (IOException io) {
                    LB.error("Can't create file : " + output_path + Const.get_chromosome(current_chromosome) + ".exons");
                    LB.error("Message thrown by Java environment (may be null):" + io.getMessage());
                    LB.die();
                }
                read_trans_exons += starts;
                if (t_translation != null) {
                    ArrayList<SNP> VSNP = new ArrayList<SNP>();
                    VSNP = Chr.get_local_SNPs(exon_start, exon_end, min_percent, min_observed);
                    for (int y = 0; y < VSNP.size(); y++) {
                        SNP sp = VSNP.get(y);
                        if (strand == -1) {
                            sp.set_misc_value(exon_end - sp.get_position() + NewTranscript.length());
                        } else {
                            sp.set_misc_value(sp.get_position() - exon_start + NewTranscript.length());
                        }
                        VSNP.set(y, sp);
                    }
                    trans_snps.addAll(VSNP);
                    VSNP.clear();
                }
                NewTranscript.append(exon_baseSeq);
            }
            process_coding_exon(LB, psnpfile, t_translation, input_species, NewTranscript, trans_snps, chr_name, psnpfile_name, Accession, Display, strand);
            exon_list.clear();
            trans_snps.clear();
            try {
                exonfile.write(Accession + "\t " + Display + "-exons\t" + read_trans_exons);
                exonfile.newLine();
                exonfile.write(Accession + "\t " + Display + "-trans\t" + read_transcript);
                exonfile.newLine();
            } catch (IOException io) {
                LB.warning("Can't write to : " + output_path + Const.get_chromosome(current_chromosome) + ".exons");
                LB.warning("Message thrown by Java environment (may be null):" + io.getMessage());
            }
        }
        try {
            exonfile.close();
            psnpfile.close();
        } catch (IOException io) {
            LB.error("Can't close file : " + output_path + Const.get_chromosome(current_chromosome) + ".exons");
            LB.error("Message thrown by Java environment (may be null):" + io.getMessage());
            LB.die();
        }
    }

    private static void process_coding_exon(Log_Buffer LB, BufferedWriter psnpfile, Translation t_translation, String input_species, StringBuffer NewTranscript, ArrayList<SNP> trans_snps, String chr_name, String psnpfile_name, String Accession, String Display, int strand) {
        if (t_translation != null) {
            String codingDNA = t_translation.getSequence().getString();
            codingDNA = Utilities.remove_spurious_Ns(codingDNA, input_species);
            int index_start = NewTranscript.indexOf(codingDNA);
            if (index_start == -1) {
                LB.warning("Can't find sequence in transcript.");
                LB.warning("NewTranscript: " + NewTranscript);
                LB.warning("codingDNA:     " + codingDNA);
            } else {
                int index_end = index_start + (codingDNA.length() - 1);
                StringBuffer Mod_Exon = null;
                String Mod_peptide = null;
                String peptide = t_translation.getPeptide();
                for (int q = 0; q < trans_snps.size(); q++) {
                    Mod_Exon = new StringBuffer(NewTranscript);
                    SNP snp = trans_snps.get(q);
                    if (strand == -1) {
                        Mod_Exon.setCharAt(snp.get_misc_value(), Utilities.Flip_Base(snp.get_new_base()));
                    } else {
                        Mod_Exon.setCharAt(snp.get_misc_value(), snp.get_new_base());
                    }
                    Mod_peptide = SequenceUtil.dna2protein(Mod_Exon.substring(index_start, index_end - 1), true);
                    int dex = peptide.indexOf("U");
                    int startat = 0;
                    while (dex != -1 && dex < Mod_peptide.length()) {
                        String tmp = Mod_peptide.substring(0, dex);
                        tmp = tmp.concat("U");
                        if (dex < Mod_peptide.length() - 1) {
                            tmp = tmp.concat(Mod_peptide.substring(dex + 1));
                        }
                        Mod_peptide = tmp;
                        startat = dex + 1;
                        dex = peptide.indexOf("U", startat);
                    }
                    if (!Mod_peptide.equals(peptide) && (peptide.length() == Mod_peptide.length())) {
                        for (int p = 0; p < Mod_peptide.length(); p++) {
                            if (Mod_peptide.charAt(p) != peptide.charAt(p)) {
                                try {
                                    psnpfile.write(chr_name + "\t");
                                    psnpfile.write(snp.get_position() + "\t");
                                    psnpfile.write(Accession + "\t");
                                    psnpfile.write(Display + "\t");
                                    psnpfile.write((p + 1) + "\t");
                                    psnpfile.write(peptide.charAt(p) + ">>>");
                                    psnpfile.write(Mod_peptide.charAt(p) + "\t");
                                    psnpfile.write(snp.get_coverage_snp() + "\t");
                                    psnpfile.write(snp.get_total_coverage());
                                    psnpfile.newLine();
                                    int r = 0;
                                    for (r = ((p < Constants.PRINT_FLANK_SIZE) ? 0 : p - Constants.PRINT_FLANK_SIZE); r < p; r++) {
                                        psnpfile.write(peptide.charAt(r));
                                    }
                                    psnpfile.write("*" + peptide.charAt(p) + "*");
                                    for (r = p + 1; (r <= (p + PRINT_SEQ_SIZE) && (r < Mod_peptide.length())); r++) {
                                        psnpfile.write(peptide.charAt(r));
                                    }
                                    psnpfile.newLine();
                                } catch (IOException io) {
                                    LB.error("Can't write to SNP file : " + psnpfile_name);
                                    LB.error("Message thrown by Java environment (may be null):" + io.getMessage());
                                    LB.die();
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
