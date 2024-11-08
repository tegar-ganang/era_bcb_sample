package opennlp.tools.cmdline.tokenizer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import opennlp.tools.cmdline.CLI;
import opennlp.tools.cmdline.CmdLineTool;
import opennlp.tools.cmdline.CmdLineUtil;
import opennlp.tools.cmdline.TerminateToolException;
import opennlp.tools.tokenize.TokenSample;
import opennlp.tools.tokenize.TokenSampleStream;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;

public final class TokenizerTrainerTool implements CmdLineTool {

    public String getName() {
        return "TokenizerTrainer";
    }

    public String getShortDescription() {
        return "trainer for the learnable tokenizer";
    }

    public String getHelp() {
        return "Usage: " + CLI.CMD + " " + getName() + TrainingParameters.getParameterUsage() + " -data trainingData -model model\n" + TrainingParameters.getDescription();
    }

    static ObjectStream<TokenSample> openSampleData(String sampleDataName, File sampleDataFile, Charset encoding) {
        CmdLineUtil.checkInputFile(sampleDataName + " Data", sampleDataFile);
        FileInputStream sampleDataIn = CmdLineUtil.openInFile(sampleDataFile);
        ObjectStream<String> lineStream = new PlainTextByLineStream(sampleDataIn.getChannel(), encoding);
        return new TokenSampleStream(lineStream);
    }

    public void run(String[] args) {
        if (args.length < 6) {
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
        CmdLineUtil.checkOutputFile("tokenizer model", modelOutFile);
        ObjectStream<TokenSample> sampleStream = openSampleData("Training", trainingDataInFile, parameters.getEncoding());
        TokenizerModel model;
        try {
            model = opennlp.tools.tokenize.TokenizerME.train(parameters.getLanguage(), sampleStream, parameters.isAlphaNumericOptimizationEnabled(), parameters.getCutoff(), parameters.getNumberOfIterations());
        } catch (IOException e) {
            CmdLineUtil.printTrainingIoError(e);
            throw new TerminateToolException(-1);
        } finally {
            try {
                sampleStream.close();
            } catch (IOException e) {
            }
        }
        CmdLineUtil.writeModel("tokenizer", modelOutFile, model);
    }
}
