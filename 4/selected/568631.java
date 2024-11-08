package src.transcriptome_analysis.Genome_Alignment;

import java.util.HashMap;
import java.util.Iterator;
import src.lib.Chromosome;
import src.lib.CommandLine;
import src.lib.Ensembl;
import src.lib.IterableIterator;
import src.lib.ReducedAlignedReads;
import src.lib.Utilities;
import src.lib.ioInterfaces.ElandIterator;
import src.lib.ioInterfaces.FastaIterator;
import src.lib.ioInterfaces.Log_Buffer;
import src.lib.ioInterfaces.Wigwriter;
import src.lib.objects.AlignedRead;

/**
 * @version $Revision: 367 $
 * @author 
 */
public class WigGeneration {

    public static final String SVNID = "$Id: WigGeneration.java 367 2008-09-23 18:12:29Z tcezard $";

    private static Log_Buffer LB = null;

    private static Ensembl Const = null;

    private static Chromosome Chr;

    private static int current_chromosome;

    private static String elandfile_path;

    private static String output_path;

    private static String input_species;

    private static String input_chr;

    private static String name;

    private static String prepend;

    private static float min_percent;

    private static int min_observed;

    private static String conf_file;

    private WigGeneration() {
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
        if (Variables.containsKey("prepend")) {
            CommandLine.test_parameter_count("prepend", Variables.get("prepend"), 1);
            prepend = Variables.get("prepend");
            LB.notice(" * Chr name prepend  : " + Variables.get("prepend"));
        } else {
            prepend = "";
            LB.notice(" * Chr name prepend  : none");
        }
        if (Variables.containsKey("force32")) {
            CommandLine.test_parameter_count("force32", Variables.get("force32"), 0);
            Chromosome.set_force32(true);
            LB.notice(" * Filter Duplicates : On");
        } else {
            Chromosome.set_force32(false);
            LB.notice(" * Filter Duplicates : Off");
        }
        if (Variables.containsKey("min_alt")) {
            CommandLine.test_parameter_count("min_alt", Variables.get("min_alt"), 1);
            min_percent = Float.parseFloat(Variables.get("min_alt"));
            if (min_percent > 1 || min_percent < 0) {
                LB.error("Min_alt value must be in the range of zero to one.");
                LB.close();
                System.exit(0);
            }
        } else {
            LB.error("Must specify minimum alternative base percent for SNP positions with the -min_alt flag");
            usage();
        }
        LB.notice(" * Min. change fract : " + min_percent);
        if (Variables.containsKey("min_obs")) {
            CommandLine.test_parameter_count("min_obs", Variables.get("min_obs"), 1);
            min_observed = Integer.parseInt(Variables.get("min_obs"));
        } else {
            LB.error("Must specify minimum observed base count for SNP positions with the -min_obs flag");
            usage();
        }
        LB.notice(" * Minimum coverage  : " + min_observed);
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
        if (Variables.containsKey("name")) {
            CommandLine.test_parameter_count("name", Variables.get("name"), 1);
            name = Variables.get("name");
            LB.notice(" * Naming            : " + name);
        } else {
            LB.error("File naming scheme (String) must be supplied with -name flag");
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
        Variables.remove("min_alt");
        Variables.remove("min_obs");
        Variables.remove("force32");
        Variables.remove("conf");
        Variables.remove("prepend");
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
        LB.notice(" -min_alt | <Float>  | Indicate the minimum fraction for calling a snp (eg 0.5");
        LB.notice(" -min_obs | <Int>    | Indicate the minimum coverage that must be observed to call. (eg 4)");
        LB.notice(" -force32 |          | use to force the maximum read length to be 32 bases.");
        LB.notice(" -conf    | <String> | The location of the configuration file to use.");
        LB.notice(" -prepend | <String>  | allows a string to be prepended to the chromosome name");
        LB.die();
    }

    private static void generate_wig_file() {
        Wigwriter wgw = new Wigwriter(LB, output_path + Const.get_chromosome(current_chromosome) + ".Trans.wig.gz", prepend);
        wgw.header("Transcriptome", "Genomic_alignment");
        int limit = Chr.get_canonical_sequence_length();
        boolean writing = false;
        int occ = 0;
        for (int g = 0; g < limit; g++) {
            occ = Chr.occupancy(g);
            if (writing) {
                if (occ == 0) {
                    writing = false;
                } else {
                    wgw.writeln(occ);
                }
            } else {
                if (occ > 0) {
                    wgw.section_header(Const.get_chromosome(current_chromosome), g + 1);
                    writing = true;
                    wgw.writeln(Chr.occupancy(g));
                }
            }
        }
        wgw.close();
    }

    private static void version_headers() {
        LB.notice("Libraries in Use.  Please provide these IDs for debugging:");
        LB.notice("    Core Libraries:");
        LB.notice("\t" + Chromosome.SVNID);
        LB.notice("    Libraries:");
        LB.notice("\t" + AlignedRead.SVNID);
        LB.notice("\t" + Ensembl.SVNID);
        LB.notice("\t" + Utilities.SVNID);
        LB.notice("    File writer/reader Libraries:");
        LB.notice("\t" + ElandIterator.SVNID);
        LB.notice("\t" + FastaIterator.SVNID);
        LB.notice("\t" + Wigwriter.SVNID);
        LB.notice("");
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
        Const = Ensembl.init(LB, input_species, conf_file, input_chr);
        int read_count = 0;
        int fail_count = 0;
        int coverage = 0;
        int total_coverage = 0;
        for (current_chromosome = 0; current_chromosome < Const.get_number_of_chromosomes(); current_chromosome++) {
            read_count = 0;
            fail_count = 0;
            LB.notice("*** Begin Processing Chromosome " + Const.get_chromosome(current_chromosome));
            LB.notice("Creating Chromosome...                                                ");
            String ffile = Const.getFastaFilename(current_chromosome);
            FastaIterator fi = new FastaIterator(LB, ffile);
            String[] rrr = null;
            while (rrr == null && fi.hasNext()) {
                rrr = fi.next();
            }
            fi.close();
            Chr = new Chromosome(LB, rrr[1], "chr" + current_chromosome);
            LB.notice("Done");
            LB.notice("Loading Reads...                                                      ");
            String elandfile = elandfile_path + Const.get_chr_filename(current_chromosome) + ".part.eland.gz";
            ReducedAlignedReads.LoadElandReads(LB, elandfile, Chr, Const, current_chromosome);
            LB.notice("Done");
            LB.notice("Chromosome " + Const.get_chromosome(current_chromosome) + " Reads passed: " + read_count + " Reads Failed: " + fail_count);
            total_coverage += coverage;
            LB.notice("Writing Wig File...                                                   ");
            generate_wig_file();
            LB.notice("Done");
            Chr.destroy();
        }
        LB.close();
    }
}
