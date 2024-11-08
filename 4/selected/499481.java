package net.sourceforge.ondex.workflow2_old;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import net.sourceforge.ondex.AbstractArguments;
import net.sourceforge.ondex.AbstractONDEXPlugin;
import net.sourceforge.ondex.args.ArgumentDefinition;
import net.sourceforge.ondex.config.BerkeleyRegistry;
import net.sourceforge.ondex.config.Config;
import net.sourceforge.ondex.config.LuceneRegistry;
import net.sourceforge.ondex.config.ValidatorRegistry;
import net.sourceforge.ondex.core.CV;
import net.sourceforge.ondex.core.ConceptClass;
import net.sourceforge.ondex.core.ONDEXConcept;
import net.sourceforge.ondex.core.ONDEXGraph;
import net.sourceforge.ondex.core.ONDEXIterator;
import net.sourceforge.ondex.core.ONDEXRelation;
import net.sourceforge.ondex.core.ONDEXView;
import net.sourceforge.ondex.core.RelationType;
import net.sourceforge.ondex.core.memory.MemoryONDEXGraph;
import net.sourceforge.ondex.core.persistent.AbstractONDEXPersistent;
import net.sourceforge.ondex.core.persistent.BerkeleyEnv;
import net.sourceforge.ondex.core.searchable.LuceneEnv;
import net.sourceforge.ondex.core.util.ONDEXViewFunctions;
import net.sourceforge.ondex.event.ONDEXEvent;
import net.sourceforge.ondex.event.ONDEXEventHandler;
import net.sourceforge.ondex.event.ONDEXListener;
import net.sourceforge.ondex.event.type.EnvironmentVariable;
import net.sourceforge.ondex.event.type.EventType;
import net.sourceforge.ondex.event.type.GeneralOutputEvent;
import net.sourceforge.ondex.export.AbstractONDEXExport;
import net.sourceforge.ondex.export.ExportArguments;
import net.sourceforge.ondex.filter.AbstractONDEXFilter;
import net.sourceforge.ondex.filter.FilterArguments;
import net.sourceforge.ondex.init.Initialisation;
import net.sourceforge.ondex.logging.ONDEXCoreLogger;
import net.sourceforge.ondex.logging.ONDEXPluginLogger;
import net.sourceforge.ondex.mapping.AbstractONDEXMapping;
import net.sourceforge.ondex.mapping.MappingArguments;
import net.sourceforge.ondex.parser.AbstractONDEXParser;
import net.sourceforge.ondex.parser.ParserArguments;
import net.sourceforge.ondex.statistics.AbstractONDEXStatistics;
import net.sourceforge.ondex.statistics.StatisticsArguments;
import net.sourceforge.ondex.tools.DirUtils;
import net.sourceforge.ondex.transformer.AbstractONDEXTransformer;
import net.sourceforge.ondex.transformer.TransformerArguments;
import net.sourceforge.ondex.validator.AbstractONDEXValidator;
import net.sourceforge.ondex.validator.ValidatorArguments;
import net.sourceforge.ondex.workflow.Parameters.AbstractONDEXPluginInit;
import net.sourceforge.ondex.workflow.Parameters.GraphInit;
import net.sourceforge.ondex.workflow.events.InvalidArgumentEvent;
import net.sourceforge.ondex.workflow2_old.tools.ValueTuple;
import org.apache.log4j.Level;
import org.codehaus.stax2.XMLInputFactory2;
import org.codehaus.stax2.XMLStreamReader2;

/**
 * ONDEX Work flow is the main entry point for running ONDEX work flows specified
 * in ONDEXParamiters.xml To use initialise this class and run runWorkflow()
 * 
 * @author hindlem, lysenkoa
 * @see it run!
 */
public class WrappedEngine {

    private static final Pattern delim = Pattern.compile(";");

    private static List<EventType> errors = new ArrayList<EventType>();

    private static String systemDataDirectory;

    private static Integer indexCounter = 0;

    private final List<ONDEXListener> listeners = new ArrayList<ONDEXListener>();

    private ONDEXCoreLogger logger;

    private static WrappedEngine engine;

    private AbstractONDEXPersistent penv;

    private ONDEXPluginLogger pluginLogger;

    private Map<ONDEXGraph, String> indexedGraphs = new HashMap<ONDEXGraph, String>();

    private Set<String> indeciesToRetain = new HashSet<String>();

    static {
        systemDataDirectory = net.sourceforge.ondex.config.Config.ondexDir;
        if (systemDataDirectory == null) {
            System.err.println("Warning ondex.dir not specified in System properties");
        } else if (systemDataDirectory.endsWith(File.separator)) {
            systemDataDirectory = systemDataDirectory.substring(0, systemDataDirectory.length() - 1);
        }
    }

    public static WrappedEngine getEngine() {
        if (engine == null) {
            engine = new WrappedEngine();
        }
        return engine;
    }

    public static void setOndexDir(String dir) {
        if (new File(dir).exists()) {
            Config.ondexDir = dir;
        } else {
            System.err.println("Warning " + dir + " does not exist!");
        }
    }

    /**
	 * A direct constructor that does not require a xml work flow file. methods in
	 * this class can be used to run a work flow directly Does not run a work flow
	 * as non is specified
	 * 
	 * @param ondexDir the ondex data dir sys variable ondex.dir
	 * @param replaceExisting
	 * @param storageDir the dir to store a persistent graph is applicable graph
	 * @param graphType e.g memory or berkley
	 */
    private WrappedEngine() {
        logger = new ONDEXCoreLogger();
        addONDEXListener(logger);
        pluginLogger = new ONDEXPluginLogger();
        EnvironmentVariable ev = new EnvironmentVariable("ONDEX VAR=" + Config.ondexDir);
        ev.setLog4jLevel(Level.INFO);
        fireEventOccurred(ev);
    }

    public static ONDEXGraph getNewGraph(String name, String type, String storageDir) throws Exception {
        return getEngine().getNewGraph_internal(name, type, storageDir);
    }

    private ONDEXGraph getNewGraph_internal(String type, String name, String storageDir) throws Exception {
        boolean no_metadata = false;
        System.out.println("ondex.dir = " + Config.ondexDir);
        if (Config.ondexDir != null) {
            File file = new File(Config.ondexDir);
            file.mkdirs();
            if (!file.exists() || !file.isDirectory()) {
                System.err.println("ondex.dir " + Config.ondexDir + " specified in System properties is not not valid, does not exist or is not a Dir");
                no_metadata = true;
            }
        } else {
            System.err.println("ondex.dir " + Config.ondexDir + " specified in System properties is not not valid, does not exist or is not a Dir");
            no_metadata = true;
        }
        if (name == null) name = "temp_graph";
        if (storageDir == null || storageDir.equals("")) {
            storageDir = System.getProperty("java.io.tmpdir") + File.separator + name;
        }
        try {
            DirUtils.deleteTree(storageDir);
        } catch (IOException e) {
        }
        ONDEXGraph result = null;
        if (type.equalsIgnoreCase(GraphInit.BERKELEY)) {
            penv = new BerkeleyEnv(storageDir, name, logger);
            result = penv.getAbstractONDEXGraph();
            BerkeleyRegistry.sid2berkeleyEnv.put(result.getSID(), (BerkeleyEnv) penv);
            ONDEXEventHandler.getEventHandlerForSID(result.getSID()).addONDEXGraphListener(logger);
        } else if (type.equalsIgnoreCase(GraphInit.MEMORY)) {
            penv = null;
            result = new MemoryONDEXGraph(name);
            ONDEXEventHandler.getEventHandlerForSID(result.getSID()).addONDEXGraphListener(logger);
        }
        try {
            if (!no_metadata) {
                Initialisation init = new Initialisation(Config.ondexDir + File.separator + "xml" + File.separator + "ondex_metadata.xml", Config.ondexDir + File.separator + "xml" + File.separator + "ondex.xsd");
                init.addONDEXListener(logger);
                init.initMetaData(result);
            }
        } catch (Exception e) {
            System.err.println("WARNING!!! The location currently set as 'ondex.dir' (" + Config.ondexDir + ") does not contain default ondex metadata.\nPlugins that need default metadata or validation may fail as a result.");
        }
        return result;
    }

    public void runStatistics(AbstractONDEXPluginInit pluginInit, ONDEXGraph inGraph) throws Exception {
        StatisticsArguments statsArgs = (StatisticsArguments) pluginInit.getArguments();
        if (statsArgs == null) statsArgs = new StatisticsArguments();
        LuceneEnv lenv = null;
        if (pluginInit.getPlugin().requiresIndexedGraph()) {
            lenv = getIndex(inGraph, pluginInit.getPlugin().getName());
        }
        String name = pluginInit.getPlugin().getName();
        AbstractONDEXStatistics stats = (AbstractONDEXStatistics) pluginInit.getPlugin();
        stats.setArguments(statsArgs);
        stats.addStatisticsListener(pluginLogger);
        long start = System.currentTimeMillis();
        stats.setONDEXGraph(inGraph);
        stats.start();
        fireEventOccurred(new GeneralOutputEvent("Statistics with " + name + " took " + (System.currentTimeMillis() - start) + " msec.", getCurrentMethodName()));
        if (lenv != null) lenv.cleanup();
        System.runFinalization();
        System.gc();
    }

    /**
	 * Runs an export plug-in on the specified graph
	 * 
	 * @param export
	 *            the export to run
	 * @param inputGraph
	 *            the graph to export
	 * @throws Exception 
	 */
    public void runExport(AbstractONDEXPluginInit export, ONDEXGraph inputGraph) throws Exception {
        ExportArguments exportArgs = (ExportArguments) export.getArguments();
        if (exportArgs == null) exportArgs = new ExportArguments();
        String exportFile = exportArgs.getExportFile();
        if (exportFile != null && (exportFile.contains(File.separator) && !new File(exportFile.substring(0, exportFile.lastIndexOf(File.separator))).exists())) {
            if (exportFile.startsWith(File.separator) || exportFile.startsWith("\\") || exportFile.startsWith("/")) {
                exportFile = systemDataDirectory + exportFile;
            } else {
                exportFile = systemDataDirectory + File.separator + exportFile;
            }
        }
        exportArgs.setExportFile(exportFile);
        String name = export.getPlugin().getName();
        AbstractONDEXExport exporter = (AbstractONDEXExport) export.getPlugin();
        exporter.setArguments(exportArgs);
        exporter.addExportListener(pluginLogger);
        long start = System.currentTimeMillis();
        exporter.setONDEXGraph(inputGraph);
        exporter.start();
        fireEventOccurred(new GeneralOutputEvent("Exporting with " + name + " took " + (System.currentTimeMillis() - start) + " msec.", getCurrentMethodName()));
        System.runFinalization();
        System.gc();
        if (penv != null) penv.commit();
    }

    /**
	 * Runs a Filter plugin on the input graph and applies results and there
	 * context dependencies to the output graph
	 * 
	 * @param pluginInit
	 *            the filter to run
	 * @param graphInput
	 *            the graph to apply the filter on
	 * @param graphOutput
	 *            the graph to write results to (cloned from input graph)
	 * @throws Exception 
	 */
    public ONDEXGraph runFilter(AbstractONDEXPluginInit pluginInit, ONDEXGraph graphInput, ONDEXGraph graphOutput) throws Exception {
        ONDEXGraph graph;
        FilterArguments filterArgs = (FilterArguments) pluginInit.getArguments();
        if (filterArgs == null) filterArgs = new FilterArguments();
        LuceneEnv lenv = null;
        if (pluginInit.getPlugin().requiresIndexedGraph()) {
            lenv = getIndex(graphInput, pluginInit.getPlugin().getName());
        }
        String name = pluginInit.getPlugin().getName();
        AbstractONDEXFilter filter = (AbstractONDEXFilter) pluginInit.getPlugin();
        filter.setArguments(filterArgs);
        long start = System.currentTimeMillis();
        filter.addFilterListener(pluginLogger);
        filter.setONDEXGraph(graphInput);
        filter.start();
        if (graphOutput != null) {
            System.err.println(">>>>>>>1");
            fireEventOccurred(new GeneralOutputEvent("Filter complete cloning returned concept from " + graphInput.getName() + " to " + graphOutput.getName(), getCurrentMethodName()));
            filter.copyResultsToNewGraph(graphOutput);
            graph = graphOutput;
        } else {
            System.err.println(">>>>>>>2");
            fireEventOccurred(new GeneralOutputEvent("Filter complete removing non matching concepts from original graph as GraphInput is the same as GraphOutput", getCurrentMethodName()));
            fireEventOccurred(new GeneralOutputEvent("Identifing Context dependencies on Relations ", getCurrentMethodName()));
            ONDEXView<ONDEXConcept> contexts = null;
            ONDEXView<ONDEXRelation> relationsVisible = filter.getVisibleRelations();
            while (relationsVisible.hasNext()) {
                ONDEXRelation relation = relationsVisible.next();
                if (contexts == null) {
                    contexts = relation.getContext();
                } else {
                    ONDEXView<ONDEXConcept> newContexts = ONDEXViewFunctions.or(contexts, relation.getContext());
                    contexts.close();
                    contexts = newContexts;
                }
            }
            relationsVisible.close();
            relationsVisible = filter.getVisibleRelations();
            fireEventOccurred(new GeneralOutputEvent("Removing Relations", getCurrentMethodName()));
            ONDEXIterator<ONDEXRelation> relations = graphInput.getRelations();
            if (relations != null) {
                while (relations.hasNext()) {
                    ONDEXRelation relation = relations.next();
                    if (!relationsVisible.contains(relation)) {
                        graphInput.deleteRelation(relation.getId());
                    }
                }
                relations.close();
            }
            relationsVisible.close();
            fireEventOccurred(new GeneralOutputEvent("Identifing Context dependencies on Concepts ", getCurrentMethodName()));
            ONDEXView<ONDEXConcept> conceptsVisible = filter.getVisibleConcepts();
            while (conceptsVisible.hasNext()) {
                ONDEXConcept concept = conceptsVisible.next();
                if (contexts == null || contexts.size() == 0) {
                    contexts = ONDEXViewFunctions.or(concept.getContext(), graphInput.getConceptsOfContext(concept));
                } else {
                    ONDEXView<ONDEXConcept> newContexts = ONDEXViewFunctions.or(contexts, ONDEXViewFunctions.or(concept.getContext(), graphInput.getConceptsOfContext(concept)));
                    contexts.close();
                    contexts = newContexts;
                }
            }
            conceptsVisible.close();
            conceptsVisible = filter.getVisibleConcepts();
            fireEventOccurred(new GeneralOutputEvent("Removing Concepts", getCurrentMethodName()));
            ONDEXIterator<ONDEXConcept> concepts = graphInput.getConcepts();
            while (concepts.hasNext()) {
                ONDEXConcept concept = concepts.next();
                if (!conceptsVisible.contains(concept) && (contexts == null || !contexts.contains(concept))) {
                    graphInput.deleteConcept(concept.getId());
                }
            }
            concepts.close();
            conceptsVisible.close();
            graph = graphInput;
        }
        fireEventOccurred(new GeneralOutputEvent("Filter with " + name + " took " + (System.currentTimeMillis() - start) + " msec.", getCurrentMethodName()));
        removeIndex(graph, lenv);
        if (penv != null) penv.commit();
        return graph;
    }

    /**
	 * Runs a Transformer plugin on the specified graph
	 * 
	 * @param inter
	 *            the Transformer to run
	 * @param graphInput
	 *            the graph to use as input (and by implication output)
	 * @throws Exception 
	 */
    public ONDEXGraph runTransformer(AbstractONDEXPluginInit inter, ONDEXGraph graphInput) throws Exception {
        TransformerArguments interA = (TransformerArguments) inter.getArguments();
        if (interA == null) interA = new TransformerArguments();
        LuceneEnv lenv = null;
        if (inter.getPlugin().requiresIndexedGraph()) {
            lenv = getIndex(graphInput, inter.getPlugin().getName());
        }
        String name = inter.getPlugin().getName();
        AbstractONDEXTransformer transformer = (AbstractONDEXTransformer) inter.getPlugin();
        transformer.addTransformerListener(pluginLogger);
        transformer.setArguments(interA);
        long start = System.currentTimeMillis();
        transformer.setONDEXGraph(graphInput);
        transformer.start();
        fireEventOccurred(new GeneralOutputEvent(name + " took " + (System.currentTimeMillis() - start) + " msec.", getCurrentMethodName()));
        removeIndex(graphInput, lenv);
        System.runFinalization();
        System.gc();
        if (penv != null) penv.commit();
        return graphInput;
    }

    /**
	 * Runs a Mapping plugin
	 * 
	 * @param map
	 *            the mapping to run
	 * @param graphInput
	 *            the graph to use as input (and by implication output)
	 */
    public ONDEXGraph runMapping(AbstractONDEXPluginInit map, ONDEXGraph graphInput) throws Exception {
        ONDEXIterator<ONDEXRelation> rit = graphInput.getRelations();
        if (rit == null) return graphInput;
        long relationsPre = rit.size();
        rit.close();
        MappingArguments ma = (MappingArguments) map.getArguments();
        if (ma == null) ma = new MappingArguments();
        LuceneEnv lenv = null;
        if (map.getPlugin().requiresIndexedGraph()) {
            lenv = getIndex(graphInput, map.getPlugin().getName());
        }
        String name = map.getPlugin().getName();
        AbstractONDEXMapping mapping = (AbstractONDEXMapping) map.getPlugin();
        mapping.addMappingListener(pluginLogger);
        mapping.setArguments(ma);
        long start = System.currentTimeMillis();
        mapping.setONDEXGraph(graphInput);
        mapping.start();
        fireEventOccurred(new GeneralOutputEvent(name + " took " + (System.currentTimeMillis() - start) + " msec.", getCurrentMethodName()));
        rit = graphInput.getRelations();
        long relationsPost = rit.size() - relationsPre;
        fireEventOccurred(new GeneralOutputEvent("New Relations: " + relationsPost, getCurrentMethodName()));
        rit.close();
        rit = null;
        removeIndex(graphInput, lenv);
        System.runFinalization();
        System.gc();
        if (penv != null) penv.commit();
        return graphInput;
    }

    /**
	 * Runs a parser plugin
	 * 
	 * @param parserInit
	 *            the parser to run
	 * @param graphInput
	 *            the graph to use as input (and by implication output)
	 * @throws Exception 
	 */
    public ONDEXGraph runParser(AbstractONDEXPluginInit parserInit, ONDEXGraph graphInput) throws Exception {
        ParserArguments pargs = ((ParserArguments) parserInit.getArguments());
        if (pargs == null) pargs = new ParserArguments();
        String inputDir = pargs.getInputDir();
        if (inputDir != null) {
            pargs.setInputDir(verifyPath(inputDir));
        }
        String inputFile = pargs.getInputFile();
        if (inputFile != null) {
            pargs.setInputFile(verifyPath(inputFile));
        }
        LuceneEnv lenv = null;
        if (parserInit.getPlugin().requiresIndexedGraph()) {
            lenv = getIndex(graphInput, parserInit.getPlugin().getName());
        }
        AbstractONDEXParser parser = (AbstractONDEXParser) parserInit.getPlugin();
        parser.addParserListener(pluginLogger);
        parser.setArguments(pargs);
        if (parser.requiresValidators() != null && parser.requiresValidators().length > 0) {
            initializeValidators(parser.requiresValidators(), graphInput);
        }
        long start = System.currentTimeMillis();
        parser.setONDEXGraph(graphInput);
        parser.start();
        long timeTaken = System.currentTimeMillis() - start;
        fireEventOccurred(new GeneralOutputEvent(parserInit.getPlugin().getName() + " took " + timeTaken + " msec.", getCurrentMethodName()));
        removeIndex(graphInput, lenv);
        System.runFinalization();
        System.gc();
        if (penv != null) {
            penv.commit();
        }
        return graphInput;
    }

    public LuceneEnv getIndex(ONDEXGraph graph, String name) {
        if (penv != null) penv.commit();
        System.runFinalization();
        System.gc();
        long start = System.currentTimeMillis();
        fireEventOccurred(new GeneralOutputEvent("Index required by " + name + " starting index", getCurrentMethodName()));
        String dir = indexedGraphs.get(graph);
        if (dir != null && new File(dir).exists()) {
            LuceneEnv lenv = new LuceneEnv(dir, false);
            lenv.addONDEXListener(logger);
            lenv.setONDEXGraph(graph);
            return lenv;
        }
        String graphdir = null;
        if (Config.ondexDir.endsWith(File.separator)) {
            graphdir = Config.ondexDir + "index" + File.separator + graph.getName() + File.separator + "index";
        } else {
            graphdir = Config.ondexDir + File.separator + "index" + File.separator + graph.getName() + File.separator + "index";
        }
        dir = graphdir + indexCounter;
        while (new File(dir).exists()) {
            dir = graphdir + indexCounter;
            indexCounter++;
        }
        LuceneEnv lenv = new LuceneEnv(dir, true);
        lenv.addONDEXListener(logger);
        lenv.setONDEXGraph(graph);
        LuceneRegistry.sid2luceneEnv.put(graph.getSID(), lenv);
        indexedGraphs.put(graph, dir);
        fireEventOccurred(new GeneralOutputEvent("Lucene took " + (System.currentTimeMillis() - start) + " msec.", getCurrentMethodName()));
        if (penv != null) penv.commit();
        System.runFinalization();
        System.gc();
        return lenv;
    }

    public void removeIndex(ONDEXGraph graph, LuceneEnv lenv) {
        if (lenv != null) lenv.cleanup();
        String dir = indexedGraphs.get(graph);
        indexedGraphs.remove(graph);
        if (dir == null || indeciesToRetain.contains(dir)) return;
        try {
            DirUtils.deleteTree(dir);
        } catch (Exception e) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e1) {
            }
            try {
                DirUtils.deleteTree(dir);
            } catch (IOException e1) {
                System.err.println("Warning: could not delete " + dir + " - directory is locked by Windows.");
            }
        }
    }

    /**
	 * Saves lucene index
	 * @param graph - graph
	 * @param dir - directory to save index to
	 */
    public static void saveIndex(ONDEXGraph graph, String newDir) {
        if (newDir != null) {
            LuceneEnv lenv = new LuceneEnv(newDir, true);
            lenv.addONDEXListener(engine.logger);
            lenv.setONDEXGraph(graph);
            engine.indexedGraphs.put(graph, newDir);
            engine.indeciesToRetain.add(newDir);
        }
    }

    /**
	 * Loads lucene index
	 * @param graph - graph
	 * @param dir - directory with index
	 */
    public static void loadIndex(ONDEXGraph graph, String dir) {
        engine.indeciesToRetain.add(dir);
        engine.indexedGraphs.put(graph, dir);
    }

    /**
	 * Cleanup default graph if persistant and closes all existing graphs
	 */
    public void cleanUp() {
        if (penv != null) {
            penv.cleanup();
        }
        List<ONDEXGraph> indexed = new LinkedList<ONDEXGraph>();
        Set<String> graphFolders = new HashSet<String>();
        for (Entry<ONDEXGraph, String> ent : indexedGraphs.entrySet()) {
            if (!indeciesToRetain.contains(ent.getValue())) graphFolders.add(new File(ent.getValue()).getParent());
            indexed.add(ent.getKey());
        }
        for (ONDEXGraph graph : indexed) removeIndex(graph, null);
        for (String graphDir : graphFolders) {
            try {
                DirUtils.deleteTree(graphDir);
            } catch (IOException e) {
            }
        }
    }

    private ONDEXPluginLogger validatorLogger;

    /**
	 * Initalize all validators in name list that have not already been
	 * initialized
	 * 
	 * @param validatorNames
	 *            the names of validators which should be their package names
	 *            within net.sourceforge.ondex.validator.*
	 */
    public void initializeValidators(String[] validatorNames, ONDEXGraph graph) {
        if (validatorLogger == null) {
            validatorLogger = new ONDEXPluginLogger();
        }
        for (String validator : validatorNames) {
            String className = "net.sourceforge.ondex.validator." + validator.toLowerCase() + ".Validator";
            if (ValidatorRegistry.validators.keySet().contains(className)) {
                continue;
            }
            try {
                Class<?> validatorClass = Class.forName(className);
                Class<?>[] args = new Class<?>[] {};
                Constructor<?> constructor = validatorClass.getClassLoader().loadClass(className).getConstructor(args);
                AbstractONDEXValidator validatorInstance = (AbstractONDEXValidator) constructor.newInstance(new Object[] {});
                ValidatorRegistry.validators.put(validator.toLowerCase(), validatorInstance);
                File vout = new File(Config.ondexDir + File.separator + "dbs" + File.separator + graph.getName() + File.separator + "validatorsout" + File.separator + validatorInstance.getName());
                vout.mkdirs();
                vout.deleteOnExit();
                ValidatorArguments va = new ValidatorArguments();
                va.setInputDir(Config.ondexDir + File.separator + "importdata" + File.separator + validator.toLowerCase());
                System.out.println("Validator input dir ==> " + va.getInputDir());
                va.setOutputDir(vout.getAbsolutePath());
                if (validatorInstance.requiresIndexedGraph()) {
                    LuceneEnv lenv = getIndex(graph, validatorInstance.getName());
                }
                validatorInstance.addValidatorListener(validatorLogger);
                validatorInstance.setArguments(va);
                validatorInstance.start();
            } catch (ClassNotFoundException e) {
                fireEventOccurred(new InvalidArgumentEvent(validator.toLowerCase() + " validator " + className + " does not exist"));
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    /**
	 * Prints to System.out the current status of concepts and relation ect. in
	 * the graph
	 * 
	 * @param graph
	 *            the graph to present statistics on
	 */
    public void outputCurrentGraphStatistics(ONDEXGraph graph) {
        fireEventOccurred(new GeneralOutputEvent("\nGraph Statistics for " + graph.getName(), getCurrentMethodName()));
        ONDEXIterator<ONDEXConcept> cit = graph.getConcepts();
        fireEventOccurred(new GeneralOutputEvent("\nConcepts: " + cit.size(), getCurrentMethodName()));
        cit.close();
        cit = null;
        ONDEXIterator<ONDEXRelation> rit = graph.getRelations();
        if (rit == null) {
            fireEventOccurred(new GeneralOutputEvent("\nRelations: 0", getCurrentMethodName()));
        } else {
            fireEventOccurred(new GeneralOutputEvent("\nRelations: " + rit.size(), getCurrentMethodName()));
            rit.close();
        }
        rit = null;
        ONDEXIterator<CV> cvit = graph.getMetaData().getCVs();
        fireEventOccurred(new GeneralOutputEvent("\nCVs: " + cvit.size(), getCurrentMethodName()));
        cvit.close();
        cvit = null;
        ONDEXIterator<ConceptClass> ccit = graph.getMetaData().getConceptClasses();
        fireEventOccurred(new GeneralOutputEvent("\nCCs: " + ccit.size(), getCurrentMethodName()));
        ccit.close();
        ccit = null;
        ONDEXIterator<RelationType> rtit = graph.getMetaData().getRelationTypes();
        fireEventOccurred(new GeneralOutputEvent("\nRTs: " + rtit.size(), getCurrentMethodName()));
        rtit.close();
        rtit = null;
        ONDEXIterator<RelationType> rtsetit = graph.getMetaData().getRelationTypes();
        fireEventOccurred(new GeneralOutputEvent("\nRTsets: " + rtsetit.size(), getCurrentMethodName()));
        rtsetit.close();
        rtsetit = null;
    }

    /**
	 * Adds a ONDEX listener to the list.
	 * 
	 * @param l -
	 *            ONDEXListener
	 */
    public void addONDEXListener(ONDEXListener l) {
        listeners.add(l);
    }

    /**
	 * Removes a ONDEX listener listener from the list.
	 * 
	 * @param l -
	 *            ONDEXListener
	 */
    public void removeONDEXListener(ONDEXListener l) {
        listeners.remove(l);
    }

    /**
	 * Returns the list of ONDEX listener listeners.
	 * 
	 * @return list of ONDEXListeners
	 */
    public ONDEXListener[] getONDEXListeners() {
        return listeners.toArray(new ONDEXListener[0]);
    }

    /**
	 * Notify all listeners that have registered with this class.
	 * 
	 * @param eventName -
	 *            name of event
	 */
    protected void fireEventOccurred(EventType e) {
        if (listeners.size() > 0) {
            ONDEXEvent oe = new ONDEXEvent(this, e);
            Iterator<ONDEXListener> it = listeners.iterator();
            while (it.hasNext()) {
                it.next().eventOccurred(oe);
            }
        }
    }

    /**
	 * Convenience method for outputing the current method name in a dynamic way
	 * @return the calling method name
	 */
    public static String getCurrentMethodName() {
        Exception e = new Exception();
        StackTraceElement trace = e.fillInStackTrace().getStackTrace()[1];
        String name = trace.getMethodName();
        String className = trace.getClassName();
        int line = trace.getLineNumber();
        return "[CLASS:" + className + " - METHOD:" + name + " LINE:" + line + "]";
    }

    @SuppressWarnings("unchecked")
    public static AbstractONDEXPluginInit process(List<ValueTuple<String, String>> toParse, String cls, String pluginCls) {
        AbstractONDEXPluginInit currentPluginInit = new AbstractONDEXPluginInit();
        try {
            currentPluginInit.setPlugin((AbstractONDEXPlugin) Class.forName(pluginCls).getConstructor(new Class<?>[] {}).newInstance(new Object[] {}));
            currentPluginInit.setArguments((AbstractArguments) Class.forName(cls).newInstance());
        } catch (Exception e) {
            System.err.println(pluginCls);
            System.err.println(cls);
            e.printStackTrace();
            return null;
        }
        for (ValueTuple<String, String> ent : toParse) {
            if (ent.getKey().equals("importfile") || ent.getKey().equals("exportfile")) {
                String file = ent.getValue();
                try {
                    Object obj = currentPluginInit.getArguments();
                    obj.getClass().getMethod("setInputFile", new Class<?>[] { String.class }).invoke(obj, new Object[] { file });
                } catch (NoSuchMethodException e) {
                } catch (Exception e1) {
                }
                try {
                    Object obj = currentPluginInit.getArguments();
                    obj.getClass().getMethod("setExportFile", new Class<?>[] { String.class }).invoke(obj, new Object[] { file });
                } catch (NoSuchMethodException e) {
                } catch (Exception e1) {
                }
            } else if (ent.getKey().equals("datadir")) {
                String dir = ent.getValue();
                try {
                    Object obj = currentPluginInit.getArguments();
                    obj.getClass().getMethod("setInputDir", new Class<?>[] { String.class }).invoke(obj, new Object[] { dir });
                } catch (IllegalArgumentException e) {
                } catch (Exception e1) {
                }
            } else {
                String pname = ent.getKey();
                String text = ent.getValue();
                Map<String, List<Object>> options = currentPluginInit.getArguments().getOptions();
                List<Object> list = options.get(pname);
                if (list == null) {
                    list = new ArrayList<Object>(1);
                    options.put(pname, list);
                }
                String[] values = delim.split(text);
                for (String value : values) {
                    list.add(castToPluginArgNativeObject(value, currentPluginInit, pname));
                }
            }
        }
        return currentPluginInit;
    }

    /**
	 * instanciates a plugin paramiter from its name-value pair
	 * @param chars the object as specified in the param
	 * @param pluginInit current plugin
	 * @param paramName the name for this paramiter
	 * @return the value in its native Java object form
	 */
    private static Object castToPluginArgNativeObject(String chars, AbstractONDEXPluginInit pluginInit, String paramName) {
        AbstractONDEXPlugin<?> plugin = pluginInit.getPlugin();
        if (plugin != null) {
            for (ArgumentDefinition<?> definition : plugin.getArgumentDefinitions()) {
                if (definition.getName().equalsIgnoreCase(paramName)) {
                    Object obj = definition.getDefaultValue();
                    try {
                        obj = definition.parseString(chars);
                    } catch (Exception e) {
                        errors.add(new InvalidArgumentEvent("The " + pluginInit + " Parameter " + paramName + " is invalid for value " + chars + " \n error:" + e.getMessage()));
                        return definition.getDefaultValue();
                    }
                    if (obj == null) {
                        errors.add(new InvalidArgumentEvent("The " + pluginInit + " Parameter " + paramName + " does not support instansiation from String"));
                        return definition.getDefaultValue();
                    }
                    return obj;
                }
            }
            errors.add(new InvalidArgumentEvent("The " + plugin.getClass() + " Parameter " + paramName + " is not a argument"));
        }
        return null;
    }

    public static String getVersion(String file) throws XMLStreamException, IOException {
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
        XMLInputFactory2 ifact = (XMLInputFactory2) XMLInputFactory.newInstance();
        ifact.configureForXmlConformance();
        XMLStreamReader2 staxXmlReader = (XMLStreamReader2) ifact.createXMLStreamReader(bis);
        String currentElement = null;
        while (staxXmlReader.hasNext()) {
            int event = staxXmlReader.next();
            switch(event) {
                case XMLStreamConstants.START_ELEMENT:
                    currentElement = staxXmlReader.getLocalName();
                    if (currentElement.equalsIgnoreCase("ONDEX")) {
                        for (int i = 0; i < staxXmlReader.getAttributeCount(); i++) {
                            if (staxXmlReader.getAttributeName(i).getLocalPart().equalsIgnoreCase("version")) {
                                String result = staxXmlReader.getAttributeValue(i);
                                bis.close();
                                return result;
                            }
                        }
                    }
            }
        }
        return "1.0";
    }

    /**
	 * Copies diretory to another direcotry
	 * @param srcDir
	 * @param dstDir
	 * @throws IOException
	 */
    public static void copyDirectory(File srcDir, File dstDir) throws IOException {
        if (srcDir.isDirectory()) {
            if (!dstDir.exists()) dstDir.mkdir();
            String[] children = srcDir.list();
            for (int i = 0; i < children.length; i++) copyDirectory(new File(srcDir, children[i]), new File(dstDir, children[i]));
        } else {
            copyFile(srcDir, dstDir);
        }
    }

    /**
     * Copies file to another file
     * @param src source file
     * @param dst - destination file
     * @throws IOException
     */
    public static void copyFile(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
        in.close();
        out.close();
    }

    /**
     * Converts a relative path to fully qualified path by prepending ondex dir
     * @param fileOrDir - path to check
     * @return - absolute path to file or folder
     */
    private static String verifyPath(String fileOrDir) {
        if (!new File(fileOrDir).exists()) {
            if (fileOrDir.startsWith(File.separator) || fileOrDir.startsWith("\\") || fileOrDir.startsWith("/")) {
                fileOrDir = systemDataDirectory + fileOrDir;
            } else {
                fileOrDir = systemDataDirectory + File.separator + fileOrDir;
            }
        }
        return fileOrDir;
    }
}
