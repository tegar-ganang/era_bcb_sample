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
import eu.fbk.hlt.common.module.Command;
import eu.fbk.hlt.edits.EDITS;
import eu.fbk.hlt.edits.engines.EntailmentEngine.Output;
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
public class ExperimentHandler extends CommandHandler {

    public static final String EXPERIMENT_COMMAND = "experiment";

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

    @Override
    public Command command() {
        Command o = new Command();
        o.setName(EXPERIMENT_COMMAND);
        o.setAbbreviation("p");
        o.setDescription(EDITS.system().description(EXPERIMENT_COMMAND));
        return o;
    }

    @Override
    public void execute(String command) throws EDITSException {
        if (EDITS.system().script().input().size() == 0) throw new EDITSException("Input file expected!");
        Experiment ex = loadExperiment(EDITS.system().script().input().get(0));
        expeirment(ex);
    }
}
