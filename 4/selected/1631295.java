package opennlp.tools.cmdline.sentdetect;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import opennlp.tools.cmdline.CLI;
import opennlp.tools.cmdline.CmdLineTool;
import opennlp.tools.cmdline.CmdLineUtil;
import opennlp.tools.cmdline.TerminateToolException;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.sentdetect.SentenceSample;
import opennlp.tools.sentdetect.SentenceSampleStream;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;

public final class SentenceDetectorTrainerTool implements CmdLineTool {

    public String getName() {
        return "SentenceDetectorTrainer";
    }

    public String getShortDescription() {
        return "trainer for the learnable sentence detector";
    }

    public String getHelp() {
        return "Usage: " + CLI.CMD + " " + getName() + " " + TrainingParameters.getParameterUsage() + " -data trainingData -model model\n" + TrainingParameters.getDescription();
    }

    static ObjectStream<SentenceSample> openSampleData(String sampleDataName, File sampleDataFile, Charset encoding) {
        CmdLineUtil.checkInputFile(sampleDataName + " Data", sampleDataFile);
        FileInputStream sampleDataIn = CmdLineUtil.openInFile(sampleDataFile);
        ObjectStream<String> lineStream = new PlainTextByLineStream(sampleDataIn.getChannel(), encoding);
        return new SentenceSampleStream(lineStream);
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
        CmdLineUtil.checkOutputFile("sentence detector model", modelOutFile);
        ObjectStream<SentenceSample> sampleStream = openSampleData("Training", trainingDataInFile, parameters.getEncoding());
        SentenceModel model;
        try {
            model = SentenceDetectorME.train(parameters.getLanguage(), sampleStream, true, null, parameters.getCutoff(), parameters.getNumberOfIterations());
        } catch (IOException e) {
            CmdLineUtil.printTrainingIoError(e);
            throw new TerminateToolException(-1);
        } finally {
            try {
                sampleStream.close();
            } catch (IOException e) {
            }
        }
        CmdLineUtil.writeModel("sentence detector", modelOutFile, model);
    }
}
