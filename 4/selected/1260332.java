package opennlp.tools.cmdline.postag;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import opennlp.tools.cmdline.CLI;
import opennlp.tools.cmdline.CmdLineTool;
import opennlp.tools.cmdline.CmdLineUtil;
import opennlp.tools.cmdline.TerminateToolException;
import opennlp.tools.postag.POSDictionary;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSSample;
import opennlp.tools.postag.WordTagSampleStream;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;

public final class POSTaggerTrainerTool implements CmdLineTool {

    public String getName() {
        return "POSTaggerTrainer";
    }

    public String getShortDescription() {
        return "trains a model for the part-of-speech tagger";
    }

    public String getHelp() {
        return "Usage: " + CLI.CMD + " " + getName() + " " + TrainingParameters.getParameterUsage() + " -data trainingData -model model\n" + TrainingParameters.getDescription();
    }

    static ObjectStream<POSSample> openSampleData(String sampleDataName, File sampleDataFile, Charset encoding) {
        CmdLineUtil.checkInputFile(sampleDataName + " Data", sampleDataFile);
        FileInputStream sampleDataIn = CmdLineUtil.openInFile(sampleDataFile);
        ObjectStream<String> lineStream = new PlainTextByLineStream(sampleDataIn.getChannel(), encoding);
        return new WordTagSampleStream(lineStream);
    }

    public void run(String[] args) {
        if (args.length < 8) {
            System.out.println(getHelp());
            throw new TerminateToolException(1);
        }
        TrainingParameters parameters = new TrainingParameters(args);
        if (!parameters.isValid()) {
            System.out.println(getHelp());
            throw new TerminateToolException(1);
        }
        File trainingDataInFile = new File(CmdLineUtil.getParameter("-data", args));
        File modelOutFile = new File(CmdLineUtil.getParameter("-model", args));
        CmdLineUtil.checkOutputFile("pos tagger model", modelOutFile);
        ObjectStream<POSSample> sampleStream = openSampleData("Training", trainingDataInFile, parameters.getEncoding());
        POSModel model;
        try {
            POSDictionary tagdict = null;
            if (parameters.getDictionaryPath() != null) {
                tagdict = new POSDictionary(parameters.getDictionaryPath());
            }
            model = opennlp.tools.postag.POSTaggerME.train(parameters.getLanguage(), sampleStream, parameters.getModel(), tagdict, null, parameters.getCutoff(), parameters.getNumberOfIterations());
        } catch (IOException e) {
            CmdLineUtil.printTrainingIoError(e);
            throw new TerminateToolException(-1);
        } finally {
            try {
                sampleStream.close();
            } catch (IOException e) {
            }
        }
        CmdLineUtil.writeModel("pos tagger", modelOutFile, model);
    }
}
