package eu.fbk.hlt.edits.engines;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.Marshaller;
import eu.fbk.hlt.common.CommandHandler;
import eu.fbk.hlt.common.ConfigurationLoader;
import eu.fbk.hlt.common.EDITSException;
import eu.fbk.hlt.common.FileTools;
import eu.fbk.hlt.common.ModuleLoader;
import eu.fbk.hlt.common.SerializationManager;
import eu.fbk.hlt.common.conffile.Configuration;
import eu.fbk.hlt.common.conffile.Configurations;
import eu.fbk.hlt.common.conffile.Type;
import eu.fbk.hlt.common.module.Command;
import eu.fbk.hlt.common.module.ModuleInfo;
import eu.fbk.hlt.common.module.OptionInfo;
import eu.fbk.hlt.common.module.SubModule;
import eu.fbk.hlt.edits.EDITS;
import eu.fbk.hlt.edits.FileLoader;
import eu.fbk.hlt.edits.engines.info.ModelInformation;
import eu.fbk.hlt.edits.etaf.EDITSStatistics;
import eu.fbk.hlt.edits.etaf.EntailmentPair;
import eu.fbk.hlt.edits.etaf.EvaluationStatistics;
import eu.fbk.hlt.edits.experiment.Experiment;
import eu.fbk.hlt.edits.experiment.ObjectFactory;
import eu.fbk.hlt.edits.processor.FileInterlocutor;
import eu.fbk.hlt.edits.processor.FilesEPSource;
import eu.fbk.hlt.edits.processor.Interlocutor;
import eu.fbk.hlt.edits.processor.Source;

/**
 * @author Milen Kouylekov
 */
public class EntailmentEngineHandler extends CommandHandler {

    public enum Output {

        EXTENDED, FULL, NO, SIMPLE
    }

    public static final String EXPERIMENT_COMMAND = "experiment";

    public static final String INFO_COMMAND = "info";

    public static final String TEST_COMMAND = "test";

    public static final String TRAIN_COMMAND = "train";

    private static final String ADD_DATE_OPTION = "add-date";

    private static final String LOAD_MODEL_OPTION = "model";

    private static final String OUTPUT_OPTION = "output";

    private static final String OUTPUT_TYPE_OPTION = "output-type";

    private static final String SAVE_MODEL_OPTION = "save-model";

    @Override
    public void execute(String command) throws EDITSException {
        if (EDITS.system().script().option(CONFIGURATION_FILE_OPTION) != null && !command.equals(TRAIN_COMMAND)) throw new EDITSException("The configuration option is not compatible with the test command.");
        if (EDITS.system().script().input().size() == 0) throw new EDITSException("Input file expected!");
        if (command.equals(EXPERIMENT_COMMAND)) {
            expeirment();
            return;
        }
        if (command.equals(INFO_COMMAND)) {
            showInfo();
            return;
        }
        if (command.equals(TRAIN_COMMAND)) {
            train();
            return;
        }
        if (command.equals(TEST_COMMAND)) {
            test();
        }
    }

    @Override
    public ModuleInfo info() {
        ModuleInfo def = super.info();
        OptionInfo o = null;
        for (OptionInfo i : def.options()) {
            if (!i.name().equals(CommandHandler.CONFIGURATION_FILE_OPTION)) {
                i.context().add(TEST_COMMAND);
                i.context().add(TRAIN_COMMAND);
                continue;
            }
            i.context().add(TRAIN_COMMAND);
        }
        o = new OptionInfo();
        o.setName(SAVE_MODEL_OPTION);
        o.setAbbreviation("sm");
        o.setType(Type.OUTPUT);
        o.setMultiple(false);
        o.setRequired(false);
        o.setId(false);
        o.context().add(TRAIN_COMMAND);
        o.setDescription(EDITS.system().description(SAVE_MODEL_OPTION));
        def.options().add(o);
        o = new OptionInfo();
        o.setName(ADD_DATE_OPTION);
        o.setAbbreviation("ad");
        o.setType(Type.BOOLEAN);
        o.setMultiple(false);
        o.setRequired(false);
        o.setDefault("false");
        o.context().add(TRAIN_COMMAND);
        o.setDescription(EDITS.system().description(ADD_DATE_OPTION));
        def.options().add(o);
        o = new OptionInfo();
        o.setName(OUTPUT_OPTION);
        o.setAbbreviation("o");
        o.setType(Type.OUTPUT);
        o.setMultiple(false);
        o.setRequired(false);
        o.setId(false);
        o.context().add(TEST_COMMAND);
        o.setDescription(EDITS.system().description(OUTPUT_OPTION));
        def.options().add(o);
        o = new OptionInfo();
        o.setName(OUTPUT_TYPE_OPTION);
        o.setAbbreviation("ot");
        o.setType(Type.ENUMERATED);
        o.setMultiple(false);
        o.setRequired(false);
        o.setId(false);
        o.context().add(TEST_COMMAND);
        o.setDefault(Output.SIMPLE.toString());
        for (Output i : Output.values()) o.values().add(i.toString());
        o.setDescription(EDITS.system().description(OUTPUT_TYPE_OPTION));
        def.options().add(o);
        o = new OptionInfo();
        o.setName(LOAD_MODEL_OPTION);
        o.setAbbreviation("m");
        o.setType(Type.OUTPUT);
        o.setMultiple(false);
        o.setRequired(true);
        o.context().add(TEST_COMMAND);
        o.setDescription(EDITS.system().description(LOAD_MODEL_OPTION));
        def.options().add(o);
        SubModule module = new SubModule();
        module.setType(EDITS.ENTAILMENT_ENGINE);
        module.setMultiple(false);
        module.setRequired(true);
        module.context().add(TRAIN_COMMAND);
        def.subModules().add(module);
        return def;
    }

    @Override
    public List<Command> supportedCommands() {
        List<Command> ops = new ArrayList<Command>();
        Command o = null;
        o = new Command();
        o.setName(TRAIN_COMMAND);
        o.setAbbreviation("r");
        o.setDescription(EDITS.system().description(TRAIN_COMMAND));
        ops.add(o);
        o = new Command();
        o.setName(TEST_COMMAND);
        o.setAbbreviation("e");
        o.setDescription(EDITS.system().description(TEST_COMMAND));
        ops.add(o);
        o = new Command();
        o.setName(EXPERIMENT_COMMAND);
        o.setAbbreviation("p");
        o.setDescription(EDITS.system().description(EXPERIMENT_COMMAND));
        ops.add(o);
        o = new Command();
        o.setName(INFO_COMMAND);
        o.setAbbreviation("i");
        o.setDescription(EDITS.system().description(INFO_COMMAND));
        ops.add(o);
        return ops;
    }

    private void expeirment() throws EDITSException {
        Experiment ex = loadExperiment(EDITS.system().script().input().get(0));
        expeirment(ex);
    }

    private void showInfo() throws EDITSException {
        String filename = EDITS.system().script().input().get(0);
        File file = new File(filename);
        if (!file.exists()) throw new EDITSException("The file " + filename + " does not exist!");
        if (!file.canRead()) throw new EDITSException("The file " + filename + " can not be read.");
        String tempdir = EDITS.system().tempdir() + SerializationManager.getDate() + "/";
        new File(tempdir).mkdir();
        SerializationManager.unzipModel(filename, tempdir);
        String confFile = tempdir + "/conf.xml";
        Configurations conf = SerializationManager.loadConfigurations(confFile);
        String infofile = tempdir + "/" + SerializationManager.DEFAILT_ID + "-info.xml";
        if (new File(infofile).exists()) {
            ModelInformation info = EntailmentEngine.loadModelInformation(infofile);
            EDITS.system().outputStream().println("The model was created on: " + info.getCreated().toString() + "\n", 0);
            EDITS.system().outputStream().println("Training files:\n", 0);
            for (String f : info.getTraining()) {
                EDITS.system().outputStream().println(f, 0);
            }
            EDITS.system().outputStream().println("\n*******************************\n", 0);
        }
        String statsfile = tempdir + "/" + SerializationManager.DEFAILT_ID + "-statistics.xml";
        if (new File(statsfile).exists()) {
            EDITS.system().outputStream().println("The model obtained the following performance on the training set:\n", 0);
            EDITSStatistics stats = FileLoader.loadStatistics(statsfile);
            EDITS.system().outputStream().println(stats, 0);
        }
        EDITS.system().outputStream().println("Configuration:\n", 0);
        EDITS.system().outputStream().println(conf.getModule().get(0), 0);
        SerializationManager.delete(new File(tempdir));
    }

    private void test() throws EDITSException {
        String output = EDITS.system().script().option(OUTPUT_OPTION);
        List<Interlocutor<EntailmentPair, EntailmentPair>> prs = FileInterlocutor.make(EDITS.system().script().input(), output, overwrite(), true);
        Output outputType = Output.NO;
        String outputT = EDITS.system().script().option(OUTPUT_TYPE_OPTION);
        if (output != null && outputT != null && outputT.length() > 0) {
            if (outputT.equalsIgnoreCase("simple")) outputType = Output.SIMPLE;
            if (outputT.equalsIgnoreCase("full")) outputType = Output.FULL;
            if (outputT.equalsIgnoreCase("extended")) outputType = Output.EXTENDED;
        }
        String model = EDITS.system().script().option(LOAD_MODEL_OPTION);
        if (model != null) {
            String add = EDITS.system().script().option(ADD_DATE_OPTION);
            model += (add != null && add.equalsIgnoreCase("true") ? SerializationManager.getDate() : "");
        }
        EntailmentEngine engine = null;
        try {
            engine = (EntailmentEngine) SerializationManager.load(model, EDITS.ENTAILMENT_ENGINE);
        } catch (EDITSException e) {
            throw new EDITSException("Can not read model from " + model + " because:\n" + e.getMessage());
        }
        List<EvaluationStatistics> all = new ArrayList<EvaluationStatistics>(prs.size());
        for (Interlocutor<EntailmentPair, EntailmentPair> a : prs) {
            all.add(engine.evaluate(a, outputType));
        }
        EvaluationStatistics stats = all.size() == 1 ? all.get(0) : EntailmentEngineUtils.combine(all, engine.trainedAsThree());
        EDITS.system().outputStream().println("Test Result:", 0);
        if (stats != null) EDITS.system().outputStream().println(stats, 0);
    }

    private void train() throws EDITSException {
        String model = EDITS.system().script().option(SAVE_MODEL_OPTION);
        if (model != null) {
            model += (EDITS.system().script().option(ADD_DATE_OPTION) != null && EDITS.system().script().option(ADD_DATE_OPTION).equals("yes") ? SerializationManager.getDate() : "");
        } else model = EDITS.system().path() + "model" + SerializationManager.getDate();
        Source<EntailmentPair> all = null;
        if (useMemory()) all = FilesEPSource.loadFromShell(EDITS.system().script().input()); else all = FilesEPSource.initFromShell(EDITS.system().script().input());
        if (model != null) {
            File f = new File(model);
            if (f.exists()) {
                if (!overwrite()) throw new EDITSException("The model " + model + " already exists");
                if (f.isDirectory()) throw new EDITSException("The model is a directory!");
            }
        }
        ConfigurationLoader loader = new ConfigurationLoader();
        Configuration conf = loader.readModule(EDITS.ENTAILMENT_ENGINE);
        EntailmentEngine engine = (EntailmentEngine) ModuleLoader.initialize(conf.getClassName());
        engine.configure(conf);
        engine.train(all);
        if (model != null) SerializationManager.export(model, engine);
        EDITS.system().outputStream().println(engine.describe(), 0);
        EDITS.system().outputStream().println(engine.statistics(), 0);
    }

    public static void expeirment(Experiment ex) throws EDITSException {
        boolean overwrite = ex.isOverwrite() != null && ex.isOverwrite();
        Source<EntailmentPair> all = null;
        Boolean v = ex.isUseMemory();
        boolean loadInMemory = v == null || v;
        if (loadInMemory) all = FilesEPSource.loadFromShell(ex.getTraining()); else all = FilesEPSource.initFromShell(ex.getTraining());
        EDITS.system().outputStream().println("Loading configuration from file " + ex.getConfiguration(), 0);
        Configurations confs = SerializationManager.loadConfigurations(ex.getConfiguration());
        ConfigurationLoader loader = new ConfigurationLoader(confs);
        Configuration conf = loader.readModule(EDITS.ENTAILMENT_ENGINE);
        EntailmentEngine engine = (EntailmentEngine) ModuleLoader.initialize(conf.getClassName());
        engine.configure(conf);
        engine.train(all);
        EDITS.system().outputStream().print(engine.describe());
        EDITS.system().outputStream().print(engine.statistics());
        String model = ex.getModel();
        if (model != null) {
            model += (ex.isAddDate() != null && ex.isAddDate() ? SerializationManager.getDate() : "");
        }
        if (model != null) {
            File f = new File(model);
            if (f.exists()) {
                if (!overwrite) throw new EDITSException("The model " + model + " already exists");
                if (f.isDirectory()) throw new EDITSException("The model is a directory!");
            }
        }
        if (model != null) SerializationManager.export(model, engine);
        if (ex.getTest().size() == 0) return;
        String output = ex.getOutput();
        List<Interlocutor<EntailmentPair, EntailmentPair>> prs = FileInterlocutor.make(ex.getTest(), output, overwrite, loadInMemory);
        Output outputType = Output.NO;
        String outputT = ex.getOutputType();
        if (output != null && outputT != null && outputT.length() > 0) {
            if (outputT.equalsIgnoreCase("simple")) outputType = Output.SIMPLE;
            if (outputT.equalsIgnoreCase("full")) outputType = Output.FULL;
            if (outputT.equalsIgnoreCase("extended")) outputType = Output.EXTENDED;
        }
        List<EvaluationStatistics> alls = new ArrayList<EvaluationStatistics>(prs.size());
        for (Interlocutor<EntailmentPair, EntailmentPair> a : prs) {
            alls.add(engine.evaluate(a, outputType));
        }
        EvaluationStatistics stats = alls.size() == 1 ? alls.get(0) : EntailmentEngineUtils.combine(alls, engine.trainedAsThree());
        EDITS.system().outputStream().print(stats);
    }

    public static Experiment loadExperiment(String filename) throws EDITSException {
        Object o = FileTools.loadObject(filename, "eu.fbk.hlt.edits.experiment");
        if (o instanceof Experiment) return (Experiment) o;
        throw new EDITSException("The file " + filename + " is not in the correct format!");
    }

    public static void saveExperiment(String filename, Experiment s, boolean overwrite) throws EDITSException {
        try {
            Marshaller marshaller = FileTools.header(filename, "eu.fbk.hlt.edits.experiment", overwrite);
            FileOutputStream fos = new FileOutputStream(filename);
            marshaller.marshal(new ObjectFactory().createExperiment(s), fos);
            fos.close();
        } catch (Exception e) {
            throw new EDITSException("The file " + filename + " is not in the correct format!");
        }
    }
}
