package opennlp.tools.cmdline.doccat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import opennlp.tools.cmdline.BasicTrainingParameters;
import opennlp.tools.cmdline.CLI;
import opennlp.tools.cmdline.CmdLineTool;
import opennlp.tools.cmdline.CmdLineUtil;
import opennlp.tools.cmdline.TerminateToolException;
import opennlp.tools.doccat.DoccatModel;
import opennlp.tools.doccat.DocumentCategorizerME;
import opennlp.tools.doccat.DocumentSample;
import opennlp.tools.doccat.DocumentSampleStream;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;

public class DoccatTrainerTool implements CmdLineTool {

    public String getName() {
        return "DocumentCategorizerTrainer";
    }

    public String getShortDescription() {
        return "trainer for the learnable document categorizer";
    }

    public String getHelp() {
        return "Usage: " + CLI.CMD + " " + getName() + " " + BasicTrainingParameters.getParameterUsage() + " -data trainingData -model model\n" + BasicTrainingParameters.getDescription();
    }

    static ObjectStream<DocumentSample> openSampleData(String sampleDataName, File sampleDataFile, Charset encoding) {
        CmdLineUtil.checkInputFile(sampleDataName + " Data", sampleDataFile);
        FileInputStream sampleDataIn = CmdLineUtil.openInFile(sampleDataFile);
        ObjectStream<String> lineStream = new PlainTextByLineStream(sampleDataIn.getChannel(), encoding);
        return new DocumentSampleStream(lineStream);
    }

    public void run(String[] args) {
        if (args.length < 8) {
            System.out.println(getHelp());
            throw new TerminateToolException(1);
        }
        BasicTrainingParameters parameters = new BasicTrainingParameters(args);
        if (!parameters.isValid()) {
            System.out.println(getHelp());
            throw new TerminateToolException(1);
        }
        File trainingDataInFile = new File(CmdLineUtil.getParameter("-data", args));
        File modelOutFile = new File(CmdLineUtil.getParameter("-model", args));
        CmdLineUtil.checkOutputFile("document categorizer model", modelOutFile);
        ObjectStream<DocumentSample> sampleStream = openSampleData("Training", trainingDataInFile, parameters.getEncoding());
        DoccatModel model;
        try {
            model = DocumentCategorizerME.train(parameters.getLanguage(), sampleStream, parameters.getCutoff(), parameters.getNumberOfIterations());
        } catch (IOException e) {
            CmdLineUtil.printTrainingIoError(e);
            throw new TerminateToolException(-1);
        } finally {
            try {
                sampleStream.close();
            } catch (IOException e) {
            }
        }
        CmdLineUtil.writeModel("document categorizer", modelOutFile, model);
    }
}
