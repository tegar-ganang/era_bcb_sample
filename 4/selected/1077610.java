package src.transcriptome_analysis.Genome_Alignment;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.ensembl.datamodel.Exon;
import org.ensembl.datamodel.Location;
import org.ensembl.datamodel.Transcript;
import src.lib.Chromosome;
import src.lib.CommandLine;
import src.lib.Constants;
import src.lib.Ensembl;
import src.lib.IterableIterator;
import src.lib.ReducedAlignedReads;
import src.lib.Utilities;
import src.lib.ioInterfaces.ElandIterator;
import src.lib.ioInterfaces.FastaIterator;
import src.lib.ioInterfaces.Log_Buffer;
import src.lib.objects.AlignedRead;
import src.lib.objects.SNP;

/**
 * @version $Revision: 367 $
 * @author 
 */
public class ExonAnalysis {

    public static final String SVNID = "$Id: ExonAnalysis.java 367 2008-09-23 18:12:29Z tcezard $";

    private static Log_Buffer LB = null;

    private static Ensembl ens = null;

    private static Chromosome Chr;

    private static int current_chromosome;

    static String elandfile_path = null;

    static String output_path;

    static String input_species;

    static String input_chr;

    static String conf_file;

    static String name;

    private ExonAnalysis() {
    }

    /**
	 * Processing command line arguments for program.
	 * 
	 * @param Variables
	 *            Command line arguments: input path, output path, Species,
	 *            Chromosome(s), min snp percent, min snp observed.
	 */
    private static void parse_input(HashMap<String, String> Variables) {
        if (Variables == null) {
            usage();
        }
        assert (Variables != null);
        if (Variables.containsKey("help")) {
            usage();
        }
        if (Variables.containsKey("force32")) {
            CommandLine.test_parameter_count("force32", Variables.get("force32"), 0);
            Chromosome.set_force32(true);
            LB.notice(" * Filter Duplicates : On");
        } else {
            Chromosome.set_force32(false);
            LB.notice(" * Filter Duplicates : Off");
        }
        if (Variables.containsKey("conf")) {
            CommandLine.test_parameter_count("conf", Variables.get("conf"), 1);
            conf_file = Variables.get("conf");
            LB.notice(" * Config file       : " + conf_file);
        } else {
            LB.error("Must specify config file with the -conf flag");
            usage();
        }
        if (Variables.containsKey("chr")) {
            CommandLine.test_parameter_count_min("chr", Variables.get("chr"), 1);
            input_chr = Variables.get("chr");
            LB.notice(" * Chromosome in use : " + input_chr);
        } else {
            LB.error("chomosome must be supplied with -chr flag");
            usage();
        }
        if (Variables.containsKey("species")) {
            CommandLine.test_parameter_count("species", Variables.get("species"), 1);
            input_species = Variables.get("species");
            LB.notice(" * Input Species     : " + input_species);
        } else {
            LB.error("input species must be supplied with -input flag");
            usage();
        }
        if (Variables.containsKey("output")) {
            CommandLine.test_parameter_count("output", Variables.get("output"), 1);
            output_path = Variables.get("output");
            if (output_path.charAt(output_path.length() - 1) != '/') {
                output_path = output_path.concat("/");
            }
            LB.notice(" * Output directory  : " + output_path);
        } else {
            LB.error("An output directory must be supplied with the -output flag");
            usage();
        }
        if (Variables.containsKey("name")) {
            CommandLine.test_parameter_count("name", Variables.get("name"), 1);
            name = Variables.get("name");
            LB.notice(" * Naming            : " + name);
        } else {
            LB.error("An output directory must be supplied with the -output flag");
            usage();
        }
        if (Variables.containsKey("input")) {
            CommandLine.test_parameter_count("input", Variables.get("input"), 1);
            elandfile_path = Variables.get("input");
            if (elandfile_path.charAt(elandfile_path.length() - 1) != '/') {
                elandfile_path = elandfile_path.concat("/");
            }
            LB.notice(" * Input directory   : " + elandfile_path);
        } else {
            LB.error("An input directory must be supplied with the -input flag");
            usage();
        }
        Variables.remove("input");
        Variables.remove("output");
        Variables.remove("species");
        Variables.remove("chr");
        Variables.remove("force32");
        Variables.remove("conf");
        Variables.remove("name");
        Iterator<String> keys = Variables.keySet().iterator();
        if (keys.hasNext()) {
            LB.error("Could not process the following flags:");
            for (String k : new IterableIterator<String>(keys)) {
                LB.error("  " + k);
            }
            LB.die();
        }
    }

    private static void usage() {
        LB.notice("This program requires six parameters:");
        LB.notice(" -input   | <String> | provide the full path to the eland files.");
        LB.notice(" -output  | <String> | provide a valid path for the output.");
        LB.notice(" -species | <String> | Provide a Species handled in the conf file");
        LB.notice(" -chr     | <String> | Indicate which chromosome to run, or \"A\" for all.");
        LB.notice(" -force32 |          | use to force the maximum read length to be 32 bases.");
        LB.notice(" -conf    | <String> | The location of the configuration file to use.");
        LB.die();
    }

    private static void process_exons(List<Transcript> list) {
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(output_path + ens.get_chromosome(current_chromosome) + ".exons"));
        } catch (IOException io) {
            io.printStackTrace();
            LB.error("Can't create file : " + output_path + ens.get_chromosome(current_chromosome) + ".exons/psnps");
            LB.error("Message thrown by Java environment (may be null):" + io.getMessage());
            LB.die();
        }
        assert (bw != null);
        for (Transcript t : list) {
            Location tl = t.getLocation();
            String Accession = t.getAccessionID();
            String Display = t.getDisplayName();
            if (t.getCDNALocation().getEnd() > Chr.get_canonical_sequence_length()) {
                continue;
            }
            tl.getStart();
            int strand = tl.getStrand();
            int read_transcript = Chr.get_starts_and_ends(tl.getStart(), tl.getEnd());
            int read_trans_exons = 0;
            ArrayList<SNP> trans_snps = new ArrayList<SNP>(100);
            @SuppressWarnings("unchecked") List<Exon> exon_list = t.getExons();
            for (Exon exon : exon_list) {
                Location el = exon.getLocation();
                int exon_start = el.getStart();
                int exon_end = el.getEnd();
                if (exon_start > exon_end) {
                    LB.warning("Start greater than end!");
                    LB.warning("Strand: " + strand);
                }
                float exon_avg_coverage = Chr.get_coverage_sum(exon_start, exon_end);
                float exon_coverage = Chr.get_coverage(exon_start, exon_end);
                int starts = Chr.get_starts_and_ends(exon_start, exon_end);
                try {
                    bw.write(Accession + "\t " + Display + "-" + exon_start + "-" + exon_end + "\t" + starts + "\t" + Utilities.DecimalPoints(exon_avg_coverage, 2) + "\t" + Utilities.DecimalPoints(exon_coverage * Constants.PERCENT_100, 2));
                    bw.newLine();
                } catch (IOException io) {
                    LB.error("Can't create file : " + output_path + ens.get_chromosome(current_chromosome) + ".exons");
                    LB.error("Message thrown by Java environment (may be null):" + io.getMessage());
                    LB.die();
                }
                read_trans_exons += starts;
            }
            exon_list.clear();
            trans_snps.clear();
            try {
                bw.write(Accession + "\t " + Display + "-exons\t" + read_trans_exons);
                bw.newLine();
                bw.write(Accession + "\t " + Display + "-trans\t" + read_transcript);
                bw.newLine();
            } catch (IOException io) {
                LB.error("Can't write to : " + output_path + ens.get_chromosome(current_chromosome) + ".exons");
                LB.error("Message thrown by Java environment (may be null):" + io.getMessage());
                LB.die();
            }
        }
        try {
            bw.close();
        } catch (IOException io) {
            LB.error("Can't close file : " + output_path + ens.get_chromosome(current_chromosome) + ".exons");
            LB.error("Message thrown by Java environment (may be null):" + io.getMessage());
            LB.die();
        }
    }

    private static void version_headers() {
        LB.notice("Libraries in Use.  Please provide these IDs for debugging:");
        LB.notice("    Core Libraries:");
        LB.notice("\t" + Chromosome.SVNID);
        LB.notice("\t" + SNP.SVNID);
        LB.notice("    Libraries:");
        LB.notice("\t" + AlignedRead.SVNID);
        LB.notice("\t" + Ensembl.SVNID);
        LB.notice("\t" + Utilities.SVNID);
        LB.notice("    File writer/reader Libraries:");
        LB.notice("\t" + ElandIterator.SVNID);
        LB.notice("\t" + FastaIterator.SVNID);
    }

    /**
	 * Main function for processing Transcriptome data.
	 * 
	 * @param args
	 */
    public static void main(String[] args) {
        LB = Log_Buffer.getLogBufferInstance();
        LB.addPrintStream(System.out);
        LB.addLogFile(CommandLine.get_output_path_bootstrap(args) + CommandLine.get_output_name_bootstrap(args) + ".log");
        Thread th = new Thread(LB);
        th.start();
        HashMap<String, String> Variables = CommandLine.process_CLI(args);
        version_headers();
        parse_input(Variables);
        ens = Ensembl.init(LB, input_species, conf_file, input_chr);
        int read_count = 0;
        int fail_count = 0;
        int coverage = 0;
        int total_coverage = 0;
        for (current_chromosome = 0; current_chromosome < ens.get_number_of_chromosomes(); current_chromosome++) {
            read_count = 0;
            fail_count = 0;
            LB.notice("*** Begin Processing Chromosome " + ens.get_chromosome(current_chromosome));
            System.out.print("Creating Chromosome...                                                ");
            String ffile = ens.getFastaFilename(current_chromosome);
            FastaIterator fi = new FastaIterator(LB, ffile);
            String[] rrr = null;
            while (rrr == null && fi.hasNext()) {
                rrr = fi.next();
            }
            fi.close();
            Chr = new Chromosome(LB, rrr[1], "chr" + current_chromosome);
            LB.notice("Done");
            LB.notice("Loading Reads...                                                      ");
            String elandfile = elandfile_path + ens.get_chr_filename(current_chromosome) + ".part.eland.gz";
            ReducedAlignedReads.LoadElandReads(LB, elandfile, Chr, ens, current_chromosome);
            LB.notice("Done");
            LB.notice("Chromosome " + ens.get_chromosome(current_chromosome) + " Reads passed: " + read_count + " Reads Failed: " + fail_count);
            total_coverage += coverage;
            LB.notice("Fetching Exon Locations...                                            ");
            Location loc = new Location("chromosome", ens.get_chromosome(current_chromosome));
            List<Transcript> list = Ensembl.get_ta(loc);
            LB.notice("Done");
            LB.notice("Processing Exons in Chromosome...                                     ");
            process_exons(list);
            LB.notice("Done");
            Chr.destroy();
        }
        LB.close();
    }
}
