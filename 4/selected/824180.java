package opennlp.tools.cmdline.namefind;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import opennlp.tools.cmdline.CLI;
import opennlp.tools.cmdline.CmdLineTool;
import opennlp.tools.cmdline.CmdLineUtil;
import opennlp.tools.cmdline.TerminateToolException;
import opennlp.tools.namefind.NameSample;
import opennlp.tools.namefind.NameSampleDataStream;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;

public final class TokenNameFinderTrainerTool implements CmdLineTool {

    public String getName() {
        return "TokenNameFinderTrainer";
    }

    public String getShortDescription() {
        return "trainer for the learnable name finder";
    }

    public String getHelp() {
        return "Usage: " + CLI.CMD + " " + getName() + " " + TrainingParameters.getParameterUsage() + " -data trainingData -model model\n" + TrainingParameters.getDescription();
    }

    static ObjectStream<NameSample> openSampleData(String sampleDataName, File sampleDataFile, Charset encoding) {
        CmdLineUtil.checkInputFile(sampleDataName + " Data", sampleDataFile);
        FileInputStream sampleDataIn = CmdLineUtil.openInFile(sampleDataFile);
        ObjectStream<String> lineStream = new PlainTextByLineStream(sampleDataIn.getChannel(), encoding);
        return new NameSampleDataStream(lineStream);
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
        CmdLineUtil.checkOutputFile("name finder model", modelOutFile);
        ObjectStream<NameSample> sampleStream = openSampleData("Training", trainingDataInFile, parameters.getEncoding());
        TokenNameFinderModel model;
        try {
            model = opennlp.tools.namefind.NameFinderME.train(parameters.getLanguage(), parameters.getType(), sampleStream, Collections.<String, Object>emptyMap(), parameters.getNumberOfIterations(), parameters.getCutoff());
        } catch (IOException e) {
            CmdLineUtil.printTrainingIoError(e);
            throw new TerminateToolException(-1);
        } finally {
            try {
                sampleStream.close();
            } catch (IOException e) {
            }
        }
        CmdLineUtil.writeModel("name finder", modelOutFile, model);
    }
}
