package net.sourceforge.ondex.parser.kegg56;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;
import net.sourceforge.ondex.InvalidPluginArgumentException;
import net.sourceforge.ondex.ONDEXPluginArguments;
import net.sourceforge.ondex.annotations.Authors;
import net.sourceforge.ondex.annotations.Custodians;
import net.sourceforge.ondex.annotations.DataURL;
import net.sourceforge.ondex.annotations.DatabaseTarget;
import net.sourceforge.ondex.annotations.Status;
import net.sourceforge.ondex.annotations.StatusType;
import net.sourceforge.ondex.args.ArgumentDefinition;
import net.sourceforge.ondex.args.BooleanArgumentDefinition;
import net.sourceforge.ondex.args.FileArgumentDefinition;
import net.sourceforge.ondex.parser.ONDEXParser;
import net.sourceforge.ondex.parser.kegg56.args.ArgumentNames;
import net.sourceforge.ondex.parser.kegg56.args.SpeciesArgumentDefinition;
import net.sourceforge.ondex.parser.kegg56.comp.CompParser;
import net.sourceforge.ondex.parser.kegg56.comp.CompPathwayMerger;
import net.sourceforge.ondex.parser.kegg56.data.Entry;
import net.sourceforge.ondex.parser.kegg56.data.Pathway;
import net.sourceforge.ondex.parser.kegg56.data.Reaction;
import net.sourceforge.ondex.parser.kegg56.data.Relation;
import net.sourceforge.ondex.parser.kegg56.data.Subtype;
import net.sourceforge.ondex.parser.kegg56.enzyme.EnzymePathwayParser;
import net.sourceforge.ondex.parser.kegg56.gene.GeneFilesParser;
import net.sourceforge.ondex.parser.kegg56.gene.GenePathwayParser;
import net.sourceforge.ondex.parser.kegg56.gene.GenesPathwayParser;
import net.sourceforge.ondex.parser.kegg56.ko.KoParser;
import net.sourceforge.ondex.parser.kegg56.ko.KoPathwayMerger;
import net.sourceforge.ondex.parser.kegg56.ko.KoRelationMerger;
import net.sourceforge.ondex.parser.kegg56.path.PathwayMerger;
import net.sourceforge.ondex.parser.kegg56.reaction.ReactionLigandDBParser;
import net.sourceforge.ondex.parser.kegg56.reaction.ReactionPathwayParser;
import net.sourceforge.ondex.parser.kegg56.relation.RelationPathwayParser;
import net.sourceforge.ondex.parser.kegg56.sink.Concept;
import net.sourceforge.ondex.parser.kegg56.sink.ConceptWriter;
import net.sourceforge.ondex.parser.kegg56.sink.RelationWriter;
import net.sourceforge.ondex.parser.kegg56.sink.SequenceWriter;
import net.sourceforge.ondex.parser.kegg56.util.BerkleyLocalEnvironment;
import net.sourceforge.ondex.parser.kegg56.util.DPLPersistantSet;
import net.sourceforge.ondex.parser.kegg56.util.Util;
import net.sourceforge.ondex.parser.kegg56.xml.XMLParser;
import org.xml.sax.SAXException;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.persist.EntityCursor;

/**
 * @author taubertj, hindlem
 */
@Status(description = "Tested November 2010 (taubertj et al.)", status = StatusType.STABLE)
@DatabaseTarget(name = "KEGG", description = "The KEGG database", version = "Release 56.0, October 1, 2010", url = "http://www.genome.jp/kegg")
@DataURL(name = "KEGG databases", description = "KEGG genes, ligand, pathway, and brite databases. keggHierarchy file is optional and adds a shallow hierarchy of super pathways.", urls = { "ftp://ftp.genome.jp/pub/kegg/release/current/brite.tar.gz", "ftp://ftp.genome.jp/pub/kegg/release/current/kgml.tar.gz", "ftp://ftp.genome.jp/pub/kegg/release/current/pathway.tar.gz", "ftp://ftp.genome.jp/pub/kegg/release/current/ligand.tar.gz", "ftp://ftp.genome.jp/pub/kegg/release/current/genes.tar.gz", "ftp://ftp.genome.jp/pub/kegg/release/current/medicus.tar.gz" })
@Authors(authors = { "Matthew Hindle", "Jan Taubert" }, emails = { "matthew_hindle at users.sourceforge.net", "jantaubert at users.sourceforge.net" })
@Custodians(custodians = { "Shaochih Kuo" }, emails = { "sckuo at users.sourceforge.net" })
public class Parser extends ONDEXParser implements ArgumentNames {

    public static final boolean DEBUG = true;

    protected static Util writerUtils;

    public static ConceptWriter getConceptWriter() {
        return writerUtils.getCw();
    }

    public static RelationWriter getRelationWriter() {
        return writerUtils.getRw();
    }

    public static SequenceWriter getSequenceWriter() {
        return writerUtils.getSw();
    }

    public static Util getUtil() {
        return writerUtils;
    }

    private static boolean importAllSequences4Species = false;

    public static boolean isImportAllSequences4Species() {
        return importAllSequences4Species;
    }

    public static Map<String, Set<Entry>> mergeGenes(Map<String, Set<Entry>> t1, Map<String, Set<Entry>> t2) {
        Map<String, Set<Entry>> parentHash;
        Map<String, Set<Entry>> mergerHash;
        if (t1.size() >= t2.size()) {
            parentHash = t1;
            mergerHash = t2;
        } else {
            parentHash = t2;
            mergerHash = t1;
        }
        for (String mergerGene : mergerHash.keySet()) {
            Set<Entry> mergerSet = mergerHash.get(mergerGene);
            if (parentHash.containsKey(mergerGene)) parentHash.get(mergerGene).addAll(mergerSet); else parentHash.put(mergerGene, mergerSet);
        }
        return parentHash;
    }

    /**
	 * @param speciesNames
	 *            can be unique common name, NCBI taxid, or kegg species code
	 * @param genomeParser
	 *            index of genome file
	 * @return list of GenomeParser.Taxonomony
	 */
    private static Set<GenomeParser.Taxonomony> parseTaxids(List<String> speciesNames, GenomeParser genomeParser) {
        Set<GenomeParser.Taxonomony> orgs = new HashSet<GenomeParser.Taxonomony>(15);
        for (String speciesName : speciesNames) {
            try {
                GenomeParser.Taxonomony taxid = genomeParser.getTaxonomony(Integer.parseInt(speciesName));
                if (taxid == null) System.err.println("Unknown ncbi taxid " + speciesName + " in parseTaxids() of Parser "); else orgs.add(taxid);
            } catch (NumberFormatException e) {
                GenomeParser.Taxonomony taxid = genomeParser.getTaxonomony(speciesName);
                if (taxid == null) {
                    taxid = genomeParser.getTaxonomonyByUniqueName(speciesName);
                    if (taxid == null) System.err.println("Can not identify species name in kegg: " + speciesName); else orgs.add(taxid);
                } else {
                    orgs.add(taxid);
                }
            }
        }
        return orgs;
    }

    private boolean cleanup = true;

    /**
	 * Clean all entries in a pathway which are not referenced in relation or
	 * reaction.
	 * 
	 * @param pathwayCache
	 *            all pathways
	 * @throws DatabaseException
	 */
    private void cleanPathways(DPLPersistantSet<Pathway> pathwayCache) throws DatabaseException {
        EntityCursor<Pathway> cursor = pathwayCache.getCursor();
        for (Pathway pathway : cursor) {
            Set<String> referenced = extractReferences(pathway);
            Set<String> remove = new HashSet<String>();
            for (String key : pathway.getEntries().keySet()) {
                Entry entry = pathway.getEntries().get(key);
                if (!referenced.contains(entry.getName())) remove.add(key); else if (entry.getComponents().size() > 0) {
                    for (String id : entry.getComponents().keySet()) remove.remove(id);
                }
            }
            for (String key : remove) {
                pathway.getEntries().remove(key);
            }
            cursor.update(pathway);
        }
        cursor.close();
    }

    /**
	 * Returns the set of all IDs which is really referenced in a relation.
	 * 
	 * @param pathway
	 *            current Pathway
	 * @return set of all IDs
	 */
    private Set<String> extractReferences(Pathway pathway) {
        Set<String> referenced = new HashSet<String>();
        for (Relation r : pathway.getRelations()) {
            referenced.add(pathway.getEntries().get(r.getEntry1().getId()).getName());
            referenced.add(pathway.getEntries().get(r.getEntry2().getId()).getName());
            for (Subtype sub : r.getSubtype()) {
                String id = sub.getValue();
                Entry entry = pathway.getEntries().get(id);
                if (entry != null) referenced.add(entry.getName());
            }
        }
        Map<String, Entry> map = new HashMap<String, Entry>();
        for (String key : pathway.getEntries().keySet()) {
            Entry entry = pathway.getEntries().get(key);
            if (entry.getReaction() != null) map.put(entry.getReaction(), entry);
        }
        for (String key : pathway.getReactions().keySet()) {
            Reaction r = pathway.getReactions().get(key);
            if (map.containsKey(r.getName())) {
                referenced.add(map.get(r.getName()).getName());
            }
            for (Entry entry : r.getProducts()) {
                referenced.add(entry.getName());
            }
            for (Entry entry : r.getSubstrates()) {
                referenced.add(entry.getName());
            }
        }
        return referenced;
    }

    public ArgumentDefinition<?>[] getArgumentDefinitions() {
        ArrayList<ArgumentDefinition<?>> args = new ArrayList<ArgumentDefinition<?>>();
        args.add(new FileArgumentDefinition(FileArgumentDefinition.INPUT_DIR, FileArgumentDefinition.INPUT_DIR_DESC, true, true, true, false));
        args.add(new SpeciesArgumentDefinition(SPECIES_ARG, SPECIES_ARG_DESC, true));
        args.add(new BooleanArgumentDefinition(IMPORT_SEQS_4_SPECIES_ARG, IMPORT_SEQS_4_SPECIES_ARG_DESC, true, false));
        args.add(new SpeciesArgumentDefinition(SPECIES_OTHO_ARG, SPECIES_OTHO_ARG_DESC, false));
        args.add(new BooleanArgumentDefinition(CLEANUP_ARG, CLEANUP_ARG_DESC, false, true));
        return args.toArray(new ArgumentDefinition[args.size()]);
    }

    public ONDEXPluginArguments getArguments() {
        return args;
    }

    public String getName() {
        return "KEGG parser, latest";
    }

    public String getVersion() {
        return "25.11.2010";
    }

    @Override
    public String getId() {
        return "kegg56";
    }

    @Override
    public String[] requiresValidators() {
        return new String[0];
    }

    /**
	 * constructs file locations and constructs species
	 */
    private void setUpParameters() throws IOException, InvalidPluginArgumentException {
        Boolean seq = (Boolean) args.getUniqueValue(ArgumentNames.IMPORT_SEQS_4_SPECIES_ARG);
        if (seq != null && seq == true) {
            importAllSequences4Species = true;
        }
        if (args.getOptions().containsKey(ArgumentNames.CLEANUP_ARG)) {
            cleanup = ((Boolean) args.getUniqueValue(ArgumentNames.CLEANUP_ARG));
        }
    }

    public void start() throws Exception {
        setUpParameters();
        File inputDir = new File((String) args.getUniqueValue(FileArgumentDefinition.INPUT_DIR));
        FileRegistry fr = FileRegistry.getInstance(inputDir.getAbsolutePath());
        FileIndex genesResource = fr.getIndex(FileRegistry.DataResource.GENES);
        FileIndex kgmlResource = fr.getIndex(FileRegistry.DataResource.KGML);
        FileIndex pathwayResource = fr.getIndex(FileRegistry.DataResource.PATHWAY);
        FileIndex ligandResource = fr.getIndex(FileRegistry.DataResource.LIGAND);
        FileIndex briteResource = fr.getIndex(FileRegistry.DataResource.BRITE);
        FileIndex medicusResource = fr.getIndex(FileRegistry.DataResource.MEDICUS);
        GenomeParser genomeParser = new GenomeParser(genesResource.getFile("genome"));
        Set<GenomeParser.Taxonomony> orgs = parseTaxids((List<String>) args.getObjectValueList(ArgumentNames.SPECIES_ARG), genomeParser);
        Set<GenomeParser.Taxonomony> orthologOrgs = parseTaxids((List<String>) args.getObjectValueList(ArgumentNames.SPECIES_OTHO_ARG), genomeParser);
        writerUtils = new Util(graph, genomeParser);
        if (DEBUG) System.out.println("KoParser");
        KoParser koParser = new KoParser(genesResource.getFile("ko"));
        BerkleyLocalEnvironment env = new BerkleyLocalEnvironment(graph);
        final DPLPersistantSet<Pathway> pathwayCache = new DPLPersistantSet<Pathway>(env, Pathway.class);
        DPLPersistantSet<net.sourceforge.ondex.parser.kegg56.sink.Relation> relationsCache = new DPLPersistantSet<net.sourceforge.ondex.parser.kegg56.sink.Relation>(env, net.sourceforge.ondex.parser.kegg56.sink.Relation.class);
        DPLPersistantSet<net.sourceforge.ondex.parser.kegg56.sink.Sequence> sequenceCache = new DPLPersistantSet<net.sourceforge.ondex.parser.kegg56.sink.Sequence>(env, net.sourceforge.ondex.parser.kegg56.sink.Sequence.class);
        Set<String> species = getSpeciesToParse(orgs, genomeParser);
        System.out.println(species);
        Pattern kgmlRegex = getKGMLRegex(species);
        List<String> kgmlFiles = kgmlResource.getFileNames(kgmlRegex, true);
        ExecutorService EXECUTOR = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 5);
        XMLParser xmlParser;
        URL url = new URL("http://www.genome.jp/kegg/xml/KGML_v0.7.1_.dtd");
        try {
            String file = File.createTempFile("KGML_v0.7.1_", ".dtd").getAbsolutePath();
            saveURL(url, file);
            xmlParser = new XMLParser(new File(file).toURI().toURL());
        } catch (IOException e) {
            xmlParser = new XMLParser();
            System.err.println("Unable to access " + url.toString() + " disabling validation: " + e.getMessage());
        }
        Set<Future<?>> futures = new HashSet<Future<?>>();
        for (String kgmlFile : kgmlFiles) futures.add(EXECUTOR.submit(new KGMLJob(xmlParser, kgmlResource.getFile(kgmlFile))));
        int percentProgress = 0;
        int kgmlFilesCompleted = 0;
        for (Future<?> future : futures) {
            try {
                Pathway pathway = (Pathway) future.get();
                pathwayCache.add(pathway);
                kgmlFilesCompleted++;
                int percentComplete = Math.round(((float) kgmlFilesCompleted / (float) futures.size()) * 100f);
                if (percentComplete % 10 == 0 && percentComplete > percentProgress) {
                    percentProgress = percentComplete;
                    System.out.println("KGML pathways parsed " + percentComplete + " % (" + kgmlFilesCompleted + ")");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
                throw new Error(e);
            }
        }
        EXECUTOR.shutdown();
        System.out.println(pathwayCache.size() + " organism pathways parsed");
        if (cleanup) try {
            cleanPathways(pathwayCache);
        } catch (DatabaseException e) {
            e.printStackTrace();
        }
        if (DEBUG) System.out.println("ReactionLigandDBParser");
        ReactionLigandDBParser reactionParser = new ReactionLigandDBParser();
        reactionParser.parse(ligandResource.getFile("reaction"));
        reactionParser.addReactionInfoToPathways(pathwayCache);
        if (DEBUG) System.out.println("Write and merge");
        PathwayMerger pm = new PathwayMerger();
        pm.mergeAndWrite(pathwayCache, genomeParser);
        if (DEBUG) System.out.println("CompParser");
        CompParser compParser = new CompParser(ligandResource.getFile("compound"), ligandResource.getFile("glycan"), medicusResource.getFile("drug"));
        Map<String, Concept> compounds = compParser.parse();
        if (DEBUG) System.out.println("Processing ChemicalStructure");
        Pattern molRegex = Pattern.compile("^C[\\d]{5}\\.mol$");
        List<String> molFiles = ligandResource.getFileNames(molRegex, true);
        for (String filename : molFiles) compParser.parseMol(compounds, filename, ligandResource.getFile(filename));
        if (DEBUG) System.out.println("CompPathwayMerger");
        new CompPathwayMerger().mergeAndWrite(pathwayCache, compounds);
        if (DEBUG) System.out.println("KoPathwayMerger");
        KoPathwayMerger koPathwayMerger = new KoPathwayMerger(pathwayCache, koParser.getKoConceptToGenes(), koParser.getKoNamesToKoConcept(), koParser.getKoAccessionToKoConcept(), relationsCache);
        koPathwayMerger.merge(species);
        if (DEBUG) System.out.println("GenePathwayParser");
        GenePathwayParser parseGene = new GenePathwayParser(pathwayCache);
        Map<String, Set<Entry>> gene2GeneEntries = parseGene.parse();
        if (DEBUG) System.out.println("mergeGenes");
        Map<String, Set<Entry>> merge = Parser.mergeGenes(gene2GeneEntries, koPathwayMerger.getGene2KoEntries());
        if (DEBUG) System.out.println("GeneFileParser");
        GeneFilesParser geneParser = new GeneFilesParser(genesResource, merge, sequenceCache, relationsCache, pathwayCache);
        geneParser.parseAndWrite(orgs, orthologOrgs, genomeParser);
        if (DEBUG) System.out.println("KoRelationMerger");
        new KoRelationMerger().mergeAndWrite(koPathwayMerger.getKo2Genes(), koParser.getKoAccessionToKoConcept());
        if (DEBUG) System.out.println("EnzymePathwayParser");
        new EnzymePathwayParser().parseAndWrite(pathwayCache, relationsCache);
        if (DEBUG) System.out.println("GenesPathwayParser");
        new GenesPathwayParser().parseAndWrite(pathwayCache, relationsCache);
        if (DEBUG) System.out.println("ReactionPathwayParser");
        new ReactionPathwayParser().parseAndWrite(pathwayCache, relationsCache);
        if (DEBUG) System.out.println("RelationPathwayParser");
        new RelationPathwayParser().parseAndWrite(pathwayCache, relationsCache);
        if (DEBUG) System.out.println("clean up Writers");
    }

    private void saveURL(URL url, String filename) throws IOException {
        URLConnection connection = url.openConnection();
        connection.connect();
        InputStreamReader ReadIn = new InputStreamReader(connection.getInputStream());
        BufferedReader BufData = new BufferedReader(ReadIn);
        FileWriter FWriter = new FileWriter(filename);
        BufferedWriter BWriter = new BufferedWriter(FWriter);
        String urlData = null;
        while ((urlData = BufData.readLine()) != null) {
            BWriter.write(urlData);
            BWriter.newLine();
        }
        BWriter.close();
    }

    /**
	 * Constructs a regex that matches kgml files
	 * 
	 * @param keggOrganisms
	 *            list of kegg organisms to creat regex for (pre-processed will
	 *            not accept "all" as a species)
	 * @return
	 */
    private Pattern getKGMLRegex(Set<String> keggOrganisms) {
        Set<String> regexs = new HashSet<String>();
        for (String keggOrganism : keggOrganisms) {
            regexs.add("^" + keggOrganism.toLowerCase() + "[\\d]{5}\\.xml$");
        }
        StringBuilder builder = new StringBuilder();
        for (String regex : regexs) {
            if (builder.length() == 0) builder.append("(" + regex + ")"); else builder.append("|(" + regex + ")");
        }
        return Pattern.compile(builder.toString());
    }

    private Set<String> getSpeciesToParse(Set<GenomeParser.Taxonomony> orgs, GenomeParser genomeParser) {
        Set<String> organisms = new HashSet<String>();
        for (GenomeParser.Taxonomony org : orgs) {
            String keggId = org.getKeggId();
            if (keggId.equals("all")) {
                Set<GenomeParser.Taxonomony> allSpecies = new HashSet<GenomeParser.Taxonomony>();
                for (String species : genomeParser.getAllKeggSpecies()) {
                    if (species.equals("all")) continue;
                    GenomeParser.Taxonomony taxon = genomeParser.getTaxonomony(species);
                    allSpecies.add(taxon);
                }
                return organisms;
            } else {
                organisms.add(org.getKeggId());
            }
        }
        return organisms;
    }

    /**
	 * @author hindlem
	 */
    class KGMLJob implements Callable<Pathway> {

        private XMLParser xmlParser;

        private InputStream fileStream;

        public KGMLJob(XMLParser xmlParser, InputStream fileStream) {
            this.xmlParser = xmlParser;
            this.fileStream = fileStream;
        }

        public Pathway call() throws IOException, SAXException {
            return xmlParser.parse(fileStream);
        }
    }
}
