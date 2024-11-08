package org.edits;

import java.io.File;
import java.util.List;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.edits.definition.ModuleDefinition;
import org.edits.definition.ObjectFactory;
import org.edits.engines.EntailmentEngine;
import org.edits.engines.EvaluationResult;
import org.edits.engines.EvaluationStatistics;
import org.edits.engines.MultiThreadEngine;
import org.edits.engines.distance.DistanceEntailmentEngine;
import org.edits.etaf.EntailmentCorpus;
import org.edits.etaf.EntailmentPair;
import org.edits.models.EDITSModel;
import org.edits.models.EntailmentEngineModel;
import org.edits.processor.EDITSIterator;
import org.edits.processor.EDITSListIterator;
import org.edits.processor.EntailmentPairSource;
import org.edits.processor.FilesEPSource;
import org.edits.processor.StringFileTarget;
import org.edits.processor.Target;

/**
 * @author Milen Kouylekov
 */
public class CommandExecutor implements CommandExecutorHanlder {

    private String configurationFile;

    private String output;

    private boolean overwrite;

    private boolean useMemory;

    @Override
    public Options commands() {
        OptionGroup out = new OptionGroup();
        Option o = null;
        boolean hasArgs = false;
        String longName = null;
        String shortName = null;
        String description = null;
        longName = "train";
        shortName = "r";
        description = "Train Edits model";
        o = new Option(shortName, longName, hasArgs, description);
        out.addOption(o);
        longName = "test";
        shortName = "e";
        description = "Test Edits model";
        o = new Option(shortName, longName, hasArgs, description);
        out.addOption(o);
        longName = "info";
        shortName = "i";
        description = "Show model information";
        o = new Option(shortName, longName, hasArgs, description);
        out.addOption(o);
        longName = "help";
        shortName = "h";
        description = "Show help";
        o = new Option(shortName, longName, hasArgs, description);
        out.addOption(o);
        Options xx = new Options();
        xx.addOptionGroup(out);
        return xx;
    }

    public String configurationFile() {
        return configurationFile;
    }

    public Target<EvaluationResult> createTarget() throws Exception {
        if (output == null) return null;
        File f = new File(output);
        if (f.exists() && !overwrite) throw new Exception("Output already exists");
        return new StringFileTarget<EvaluationResult>(output);
    }

    @Override
    public Options defaultOptions() {
        Options out = new Options();
        Option o = null;
        boolean hasArgs = true;
        String longName = null;
        String shortName = null;
        String description = null;
        longName = "output";
        shortName = "o";
        description = "Output path. When used with the train command will save the obtained model in the specified path." + " When used with the test command will save an annotated version of the test file in the specified path";
        o = new Option(shortName, longName, hasArgs, description);
        o.setArgName("file");
        out.addOption(o);
        longName = "model";
        shortName = "m";
        description = "Edits model path";
        o = new Option(shortName, longName, hasArgs, description);
        o.setArgName("file");
        out.addOption(o);
        longName = "verbose";
        shortName = "v";
        description = "Enter verbose mode";
        o = new Option(shortName, longName, false, description);
        out.addOption(o);
        longName = "force";
        shortName = "f";
        description = "Overwrite the output file";
        o = new Option(shortName, longName, false, description);
        out.addOption(o);
        longName = "configuration";
        shortName = "c";
        description = "Configuration file path";
        o = new Option(shortName, longName, hasArgs, description);
        o.setArgName("file");
        out.addOption(o);
        longName = "pipe";
        shortName = "p";
        description = "Read data in a pipe mode";
        o = new Option(shortName, longName, false, description);
        out.addOption(o);
        longName = "help";
        shortName = "h";
        description = "Print help";
        o = new Option(shortName, longName, false, description);
        out.addOption(o);
        return out;
    }

    @Override
    public void execute(CommandLine line) throws Exception {
        if (line.hasOption("l")) Edits.registry().setLanguage(line.getOptionValue("l"));
        if (line.hasOption("r")) {
            loadDefaultOptions(line);
            train(line);
            return;
        }
        if (line.hasOption("e")) {
            loadDefaultOptions(line);
            test(line);
            return;
        }
        if (line.hasOption("i")) {
            modelInfo(line);
            return;
        }
        if (line.hasOption("h")) {
            Edits.printEditsInfo(true);
            return;
        }
        Edits.printEditsInfo(false);
    }

    public void modelInfo(CommandLine script) throws Exception {
        for (String input : script.getArgs()) {
            EntailmentEngineModel engine = (EntailmentEngineModel) EDITSModel.loadModel(input, Edits.tempdir());
            System.out.println("Training files:");
            for (String file : engine.trainingFiles()) System.out.println(file);
            System.out.println(EngineToString.toString(engine.statistics()));
        }
    }

    private ModuleDefinition createModelDefinition(CommandLine script) throws Exception {
        ModuleDefinition def = null;
        String conf = configurationFile();
        if (conf != null) {
            def = (ModuleDefinition) new ObjectFactory().load(conf);
            def.init(script, Edits.registry());
            return def;
        }
        def = new ModuleDefinition();
        String scriptEngine = script.getOptionValue("engine");
        if (scriptEngine == null) {
            for (String s : Edits.registry().implementations("engine")) if (script.hasOption(s)) {
                if (s.equals(DistanceEntailmentEngine.NAME) && scriptEngine != null) continue;
                scriptEngine = s;
            }
        }
        if (scriptEngine != null) {
            System.out.println("Using engine " + scriptEngine);
            if (!Edits.registry().containsAlias(scriptEngine)) {
                System.out.println("Unrecognized Entailment Engine " + scriptEngine);
                return null;
            }
            def.setName(scriptEngine);
        } else def.setName(Edits.registry().defaultModules().get(Edits.ENTAILMENT_ENGINE));
        def.init(script, Edits.registry());
        if (scriptEngine != null) def.globalModules().get("engine").remove(scriptEngine);
        return def;
    }

    private EDITSIterator<EntailmentPair> inputIterator(String file) throws Exception {
        if (useMemory) return new EDITSListIterator<EntailmentPair>(((EntailmentCorpus) new org.edits.etaf.ObjectFactory().load(file)).getPair());
        return new EntailmentPairSource(file);
    }

    private void loadDefaultOptions(CommandLine script) throws Exception {
        overwrite = script.hasOption("force");
        useMemory = !script.hasOption("pipe");
        output = script.getOptionValue("output");
        configurationFile = script.getOptionValue("configuration");
        boolean verbose = script.hasOption("verbose");
        Edits.setVerbose(verbose);
    }

    private void test(CommandLine script) throws Exception {
        List<String> files = FileTools.inputFiles(script.getArgs());
        String model = script.getOptionValue("model");
        EntailmentEngine engine = null;
        if (model == null) {
            System.out.println("Initializing engine with default values.");
            ModuleDefinition def = createModelDefinition(script);
            if (def == null) return;
            if (output != null) {
                File f = new File(output);
                if (f.exists() && !overwrite) throw new Exception("Output already exists");
            }
            engine = (EntailmentEngine) EditsModuleLoader.loadModule(def);
        } else {
            System.out.println("Loading Model " + model);
            EntailmentEngineModel engineModel = (EntailmentEngineModel) EDITSModel.loadModel(model, Edits.tempdir());
            engine = engineModel.engine();
        }
        MultiThreadEngine mte = new MultiThreadEngine(engine);
        Target<EvaluationResult> result = createTarget();
        EvaluationStatistics stats = new EvaluationStatistics();
        for (String file : files) {
            EDITSIterator<EntailmentPair> source = inputIterator(file);
            EvaluationStatistics s = mte.test(source, result);
            if (s != null) {
                System.out.println("=== Test Result ===\n");
                if (files.size() > 1) System.out.println("File: " + file + "\n");
                System.out.println(s.toString());
                stats.add(s);
            }
        }
        if (result != null) result.close();
        if (files.size() > 1) {
            stats.init();
            System.out.println("=== Test Result ===\n");
            System.out.println(stats);
        }
    }

    private void train(CommandLine script) throws Exception {
        ModuleDefinition def = createModelDefinition(script);
        if (def == null) return;
        if (output != null) {
            File f = new File(output);
            if (f.exists() && !overwrite) throw new Exception("Output already exists");
        }
        EntailmentEngine engine = (EntailmentEngine) EditsModuleLoader.loadModule(def);
        System.out.println(EditsToString.toString(engine.definition()));
        List<String> files = FileTools.inputFiles(script.getArgs());
        EDITSIterator<EntailmentPair> all = inputIterator(files, useMemory);
        EvaluationStatistics stats = engine.train(all);
        if (output != null) saveModel(output, engine, stats, files, overwrite);
        System.out.println("=== Performance On Training ===\n");
        System.out.println(EngineToString.toString(stats));
    }

    public static EDITSIterator<EntailmentPair> inputIterator(List<String> files, boolean useMemory) throws Exception {
        if (useMemory) return FilesEPSource.loadFromShell(files);
        return FilesEPSource.initFromShell(files);
    }

    public static void saveModel(String modelFile, EntailmentEngine engine, EvaluationStatistics stats, List<String> files, boolean overwrite) throws Exception {
        EntailmentEngineModel med = new EntailmentEngineModel();
        med.setEngine(engine);
        med.setStatistics(stats);
        med.setTrainingFiles(files);
        ModuleDefinition defx = new ModuleDefinition();
        defx.setName(EntailmentEngineModel.NAME);
        defx.getModule().add(engine.definition());
        med.setDefinition(defx);
        med.serialize(modelFile, Edits.tempdir(), overwrite);
    }
}
