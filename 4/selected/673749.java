package eu.fbk.hlt.edits.engines;

import java.io.File;
import eu.fbk.hlt.common.CommandHandler;
import eu.fbk.hlt.common.ConfigurationLoader;
import eu.fbk.hlt.common.EDITSException;
import eu.fbk.hlt.common.ModuleLoader;
import eu.fbk.hlt.common.SerializationManager;
import eu.fbk.hlt.common.conffile.Configuration;
import eu.fbk.hlt.common.conffile.Type;
import eu.fbk.hlt.common.module.Command;
import eu.fbk.hlt.common.module.ModuleInfo;
import eu.fbk.hlt.common.module.OptionInfo;
import eu.fbk.hlt.common.module.SubModule;
import eu.fbk.hlt.edits.EDITS;
import eu.fbk.hlt.edits.etaf.EntailmentPair;
import eu.fbk.hlt.edits.processor.FilesEPSource;
import eu.fbk.hlt.edits.processor.Source;

/**
 * @author Milen Kouylekov
 */
public class TrainingHandler extends CommandHandler {

    public static final String TRAIN_COMMAND = "train";

    private static final String ADD_DATE_OPTION = "add-date";

    private static final String SAVE_MODEL_OPTION = "save-model";

    @Override
    public Command command() {
        Command o = null;
        o = new Command();
        o.setName(TRAIN_COMMAND);
        o.setAbbreviation("r");
        o.setDescription(EDITS.system().description(TRAIN_COMMAND));
        return o;
    }

    @Override
    public void execute(String command) throws EDITSException {
        if (EDITS.system().script().input().size() == 0) throw new EDITSException("Input file expected!");
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

    @Override
    public ModuleInfo info() {
        ModuleInfo def = super.info();
        OptionInfo o = null;
        o = new OptionInfo();
        o.setName(SAVE_MODEL_OPTION);
        o.setAbbreviation("sm");
        o.setType(Type.OUTPUT);
        o.setMultiple(false);
        o.setRequired(false);
        o.setId(false);
        o.setDescription(EDITS.system().description(SAVE_MODEL_OPTION));
        def.options().add(o);
        o = new OptionInfo();
        o.setName(ADD_DATE_OPTION);
        o.setAbbreviation("ad");
        o.setType(Type.BOOLEAN);
        o.setMultiple(false);
        o.setRequired(false);
        o.setDefault("false");
        o.setDescription(EDITS.system().description(ADD_DATE_OPTION));
        def.options().add(o);
        SubModule module = new SubModule();
        module.setType(EDITS.ENTAILMENT_ENGINE);
        module.setMultiple(false);
        module.setRequired(true);
        def.subModules().add(module);
        return def;
    }
}
