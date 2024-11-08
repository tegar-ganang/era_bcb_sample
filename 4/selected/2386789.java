package backend.parser.coryne3;

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import backend.core.AbstractONDEXGraph;
import backend.core.security.Session;
import backend.event.type.EventType;
import backend.param.args.ArgumentDefinition;
import backend.parser.AbstractONDEXParser;
import backend.parser.ParserArguments;
import backend.parser.coryne3.cGlutamicum_parsers.HomologousReader;
import backend.parser.coryne3.cGlutamicum_parsers.Reader;
import backend.parser.coryne3.cGlutamicum_parsers.Writer2Ondex;

/**
 * 
 * @author elsner
 */
public class Parser extends AbstractONDEXParser {

    ParserArguments pa;

    AbstractONDEXGraph aog;

    private static Parser instance;

    String ondexDir = System.getProperty("ondex.dir");

    String inputDir = ondexDir + File.separator + "importdata" + File.separator + "coryne" + File.separator + "sourceFiles" + File.separator;

    String homologousDir = ondexDir + File.separator + "importdata" + File.separator + "coryne" + File.separator + "blast" + File.separator;

    String defaultFilename = ondexDir + File.separator + "importdata" + File.separator + "coryne" + File.separator + "defaults.cor";

    String possumDir = ondexDir + File.separator + "importdata" + File.separator + "coryne" + File.separator + "PoSSuMsearch" + File.separator;

    public Parser(final Session s) {
        super(s);
    }

    @Override
    public String getName() {
        return new String("Coryne 3.0");
    }

    @Override
    public String getVersion() {
        return new String("22.04.2007");
    }

    @Override
    public ArgumentDefinition[] getArgumentDefinitions() {
        return new ArgumentDefinition[0];
    }

    @Override
    public ParserArguments getParserArguments() {
        return this.pa;
    }

    /**
	 * Set up the OndexGraph
	 * 
	 * @param graph - graph which will be filled 
	 */
    @Override
    public void setONDEXGraph(final AbstractONDEXGraph graph) {
        instance = this;
        this.aog = graph;
        try {
            start();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Removes char c from the beginning and end of string s
	 * 
	 * @param s - complete string
	 * @param c - char to remove
	 * @return String s without char c at the beginning and end 
	 */
    public static String trimChar(final String s, final char c) {
        int start = 0;
        int end = s.length();
        if (s.charAt(start) == c) {
            start++;
        }
        if (s.charAt(end - 1) == c) {
            end--;
        }
        return s.substring(start, end);
    }

    /**
	 * set up the parser arguments
	 */
    @Override
    public void setParserArguments(final ParserArguments pa) {
        this.pa = pa;
    }

    public static void propagateEventOccurred(EventType et) {
        if (instance != null) instance.fireEventOccurred(et);
    }

    /**
	 * starts the parser action
	 * 
	 * @throws IOException
	 */
    private void start() throws IOException {
        final HomologousReader hr = new HomologousReader();
        final Hashtable homologous = hr.start(homologousDir);
        final String entries[] = new File(inputDir).list();
        Reader reader = new Reader();
        Reader read_tmp = null;
        for (final String entry : entries) {
            System.out.println("Read now " + entry);
            final String dirName = inputDir + entry + File.separator;
            read_tmp = read(dirName);
            reader.getNucleotides().putAll(read_tmp.getNucleotides());
            reader.getBindingMotifsForGenes().putAll(read_tmp.getBindingMotifsForGenes());
            reader.getGenes().putAll(read_tmp.getGenes());
            reader.getOperons().putAll(read_tmp.getOperons());
            reader.getOrganisms().putAll(read_tmp.getOrganisms());
            reader.getProteins().putAll(read_tmp.getProteins());
            reader.getRegulations().putAll(read_tmp.getRegulations());
            reader.getRegulators().putAll(read_tmp.getRegulators());
            reader.getRegulatorTypes().putAll(read_tmp.getRegulatorTypes());
        }
        write(reader, possumDir, homologous, s);
    }

    /**
	 * reads the database files
	 * 
	 * @param inputDir - flder path to the files
	 * @param tables - tables to fill
	 * @return the filled tables
	 * @throws IOException
	 */
    @SuppressWarnings("unchecked")
    public Reader read(final String inputDir) throws IOException {
        final String file1 = inputDir + "gene_annotation.gb";
        final String file2 = inputDir + "coding_regions.gb";
        final String file3 = inputDir + "gene_regulations.gb";
        final String file4 = inputDir + "mapping_table.gb";
        final String file5 = inputDir + "regulator_types.gb";
        final String file6 = inputDir + "operons.gb";
        final String file7 = inputDir + "databaseID.gb";
        final Reader r = new Reader(file1, file2, file3, file4, file5, file6, file7);
        r.start();
        return r;
    }

    /**
	 * creates the ondexgraph
	 * 
	 * @param defaultCorPath - 
	 * @param tables - the filled tables
	 * @param possumDir - not used at the moment
	 * @param homologous - not used at the moment
	 * @throws IOException
	 */
    public void write(final Reader reader, final String possumDir, final Hashtable homologous, final Session s) throws IOException {
        final Writer2Ondex w = new Writer2Ondex(aog, s, reader, possumDir, homologous);
        w.start();
    }
}
