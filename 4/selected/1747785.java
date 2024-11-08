package ontology;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import util.DataCache;
import util.IOUtil;
import datastructures.Concept;
import datastructures.ConceptInstance;
import datastructures.ConceptMap;
import datastructures.DocumentCollection;
import datastructures.NotTaggedException;
import datastructures.Relation;
import datastructures.RelationInstance;
import datastructures.Ontology;
import extraction.Stopwords;
import extraction.conceptextraction.POSPatterns;
import extraction.relationextraction.RelationDiscovery;
import extraction.relationextraction.RelationInstanceDiscovery;

public class Main {

    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

    DataCache cache = null;

    private void run() {
        final boolean debug = false;
        System.out.println("============================== Sport Ontology ==============================");
        newline();
        String path = debug ? Constants.articlePath : (readLine("- Enter path to articles (default: " + Constants.articlePath + "):", Constants.articlePath));
        boolean useCache = debug ? true : readBoolean("- Try to use cache?", true);
        String cacheName = debug ? Constants.cacheName : (useCache ? readLine("     Cache name (default: " + Constants.cacheName + "):", Constants.cacheName) : Constants.cacheName);
        if (useCache) {
            cache = new DataCache("cache" + File.separator, cacheName);
        }
        boolean displayOntology = debug ? true : readBoolean("- Display created ontology graphically?", true);
        boolean displayRelationInstances = debug ? false : (displayOntology ? readBoolean("     Display relation instances?", false) : false);
        boolean writeXMLOntology = debug ? true : readBoolean("- Write created ontology into XML file?", true);
        String xmlOutputPath = debug ? Constants.outputPath : (writeXMLOntology ? readLine("     Enter output path (default: " + Constants.outputPath + "):", Constants.outputPath) : "");
        boolean printResults = debug ? true : readBoolean("- Print results to console?", false);
        newline();
        DocumentCollection docColl = loadDocumentCollection(path);
        extraction.Stopwords sw = stopwordsDetection(docColl);
        if (printResults) {
            output("Stopwords:", true);
            output(util.CollectionUtil.collectionToString(sw.getStopwords()));
            newline();
        }
        List<Concept> concepts = conceptDetection(docColl, sw);
        List<String> conceptStringList = new ArrayList<String>();
        List<String> conceptinstanceStringList = new ArrayList<String>();
        List<ConceptInstance> conceptinstanceList = new ArrayList<ConceptInstance>();
        if (printResults) {
            output("Found the following concepts:", true);
            int index = 1;
            for (Concept c : concepts) {
                output("Concept #" + (index++) + ": \"" + c.getName() + "\" with instances:");
                output(util.CollectionUtil.collectionToString(c.getInstances()));
                newline();
                conceptStringList.add(c.getName());
                for (ConceptInstance ci : c.getInstances()) {
                    conceptinstanceList.add(ci);
                    conceptinstanceStringList.add(ci.getName());
                }
            }
            newline();
        }
        List<RelationInstance> relationInstances = relationInstanceDetection(docColl, concepts);
        if (printResults) {
            output("Found the following relation instances:", true);
            for (RelationInstance relInst : relationInstances) {
                output(relInst.toString());
                newline();
            }
            newline();
        }
        List<Relation> relations = relationDetection(docColl, concepts, relationInstances);
        if (printResults) {
            output("Found the following relations:", true);
            for (Relation rel : relations) {
                output(rel.toString());
                newline();
            }
            newline();
        }
        if (displayOntology) {
            new Visualization(concepts, relations, relationInstances, displayRelationInstances);
        }
        if (writeXMLOntology) {
            Ontology ontology = new Ontology(concepts, relations);
            try {
                IOUtil.writeOntology(ontology, xmlOutputPath, IOUtil.FileType.UTF8);
            } catch (FileNotFoundException e) {
                System.err.println("Could not write the ontology to the XML file, because the file was not found. " + e.getMessage());
            }
        }
        System.out.println("---------- finished ----------");
        System.out.println("(depending on your choices, an output window and/or an xml file shall be generated)");
    }

    /**
	 * 
	 * @param filename can be null, then the user is interactively asked
	 */
    private void visualize(String filename) {
        if (filename == null) {
            filename = readLine("Where is the ontology file located (default: " + Constants.outputPath + ")?", Constants.outputPath);
        }
        boolean displayRelationInstances = readBoolean("Display relation instances?", false);
        Ontology ontology;
        try {
            ontology = IOUtil.readOntology(filename, IOUtil.FileType.UTF8);
        } catch (FileNotFoundException e) {
            System.out.println("File not found: " + e.getMessage());
            return;
        }
        List<RelationInstance> relationInstances = new ArrayList<RelationInstance>(ontology.relations.size() * 5);
        for (Relation rel : ontology.relations) relationInstances.addAll(rel.getInstances());
        new Visualization(ontology.concepts, ontology.relations, relationInstances, displayRelationInstances);
    }

    public static void main(String[] args) throws Exception {
        boolean buildOntology = true;
        Main main = new Main();
        if (args.length > 0) {
            if (args[0].equals("-v") || args[0].equals("--visualize")) {
                buildOntology = false;
                String filename = null;
                if (args.length == 2) filename = args[1];
                main.visualize(filename);
            }
        }
        if (buildOntology) main.run();
    }

    private DocumentCollection loadDocumentCollection(String path) {
        action("Loading document collection");
        if (cache != null) {
            DocumentCollection docColl = cache.loadDocumentCollection(path, true);
            ok();
            newline();
            return docColl;
        } else {
            DocumentCollection docColl = IOUtil.loadDocumentCollection(path, IOUtil.FileType.UTF8);
            ok();
            output(docColl.getDocumentCount() + " documents loaded");
            newline();
            prepareDocColl(docColl);
            return docColl;
        }
    }

    private void prepareDocColl(DocumentCollection docColl) {
        action("Removing spurious capitals");
        int count = docColl.removeSpuriousCapitals();
        ok();
        output(count + " capital words removed");
        newline();
        action("POS-tagging");
        docColl.tagPhrases();
        ok();
        newline();
    }

    private extraction.Stopwords stopwordsDetection(DocumentCollection docColl) {
        extraction.Stopwords sw;
        action("Collecting stop words");
        if (cache != null) {
            sw = cache.detectStopwords(docColl, Stopwords.DetectionStrategy.DocumentOccurence);
            ok();
            newline();
        } else {
            sw = new extraction.Stopwords(docColl, Stopwords.DetectionStrategy.DocumentOccurence);
            ok();
            newline();
        }
        return sw;
    }

    private List<Concept> conceptDetection(DocumentCollection docColl, Stopwords sw) throws NotTaggedException {
        List<Concept> concepts = new ArrayList<Concept>();
        action("Matching LexicoSyntacticPatterns");
        if (cache != null) {
            ConceptMap conceptMap = cache.extractLexicoSyntactically(docColl, sw.getStopwords(), false);
            concepts.addAll(conceptMap.getSortedList());
            ok();
            newline();
        } else {
            ConceptMap conceptMap = extraction.conceptextraction.LexicoSyntacticPatterns.extractLexicoSyntactically(docColl, sw.getStopwords(), false);
            concepts.addAll(conceptMap.getSortedList());
            ok();
            newline();
        }
        action("Matching POS patterns");
        concepts.addAll(POSPatterns.detectConcepts(docColl));
        ok();
        newline();
        action("Concept pruning");
        concepts = util.ConceptUtil.pruneSize(util.ConceptUtil.pruning(concepts), 2);
        ok();
        newline();
        return concepts;
    }

    private List<RelationInstance> relationInstanceDetection(DocumentCollection docColl, List<Concept> concepts) {
        List<RelationInstance> relationInstances = new ArrayList<RelationInstance>();
        action("Detecting relation instances");
        if (cache != null) {
            relationInstances = cache.findRelationInstances(docColl, concepts);
        } else {
            relationInstances = RelationInstanceDiscovery.findRelationInstances(docColl, concepts);
        }
        ok();
        newline();
        return relationInstances;
    }

    private List<Relation> relationDetection(DocumentCollection docColl, List<Concept> concepts, List<RelationInstance> relationInstances) {
        List<Relation> relations = new ArrayList<Relation>();
        action("Detecting relations");
        if (cache != null && false) {
            relations = cache.findRelations(docColl, concepts, relationInstances, 5);
        } else {
            relations = RelationDiscovery.findRelations(docColl, concepts, relationInstances, 5);
        }
        ok();
        newline();
        return relations;
    }

    private boolean inprocess = false;

    private void newline() {
        System.out.println("");
    }

    private void output(String msg) {
        output(msg, false);
    }

    private boolean readBoolean(String msg, boolean def) {
        System.out.print(msg + " [y/n] (default: " + (def ? "y" : "n") + ")");
        int ch = -1;
        boolean input = false;
        try {
            String inp = br.readLine();
            ch = (inp.length() > 0 ? inp.charAt(0) : -1);
        } catch (IOException ioe) {
            ch = -1;
        }
        if (def) {
            if (((char) ch) == 'n') {
                input = false;
            } else {
                input = true;
            }
        }
        if (!def) {
            if (((char) ch) == 'y') {
                input = true;
            } else {
                input = false;
            }
        }
        return input;
    }

    @SuppressWarnings("unused")
    private String readLine(String msg) {
        return readLine(msg, "");
    }

    private String readLine(String msg, String def) {
        try {
            System.out.print(msg + " ");
            String input = br.readLine();
            return (input.length() == 0 ? def : input);
        } catch (IOException ioe) {
            return def;
        }
    }

    private void output(String msg, boolean underline) {
        if (inprocess) System.out.println("");
        int linestart = 17;
        int charsperline = 80 - linestart;
        int lastOutputLength = 0;
        for (int i = 0; i < msg.length(); i += lastOutputLength) {
            int outputEndIndex;
            int maxLen = Math.min(i + charsperline, msg.length());
            if (msg.indexOf("\n", i) < 0 || maxLen < msg.indexOf("\n", i)) {
                outputEndIndex = maxLen;
                lastOutputLength = outputEndIndex - i;
            } else {
                outputEndIndex = msg.indexOf("\n", i);
                lastOutputLength = outputEndIndex - i + 1;
            }
            System.out.println("                 " + msg.substring(i, outputEndIndex));
            if (underline) {
                System.out.print("                 ");
                for (int j = 0; j < msg.length(); j++) {
                    System.out.print('-');
                }
                newline();
            }
        }
    }

    private void action(String msg) {
        if (inprocess) System.out.println("");
        System.out.print("[" + now() + "] " + msg + " ... ");
        inprocess = true;
    }

    private void ok() {
        System.out.println("ok");
        inprocess = false;
    }

    private String now() {
        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        return df.format(new Date());
    }
}
