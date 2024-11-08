package opennlp.tools.cmdline.chunker;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import opennlp.tools.chunker.ChunkSample;
import opennlp.tools.chunker.ChunkSampleStream;
import opennlp.tools.chunker.ChunkerME;
import opennlp.tools.chunker.ChunkerModel;
import opennlp.tools.cmdline.BasicTrainingParameters;
import opennlp.tools.cmdline.CLI;
import opennlp.tools.cmdline.CmdLineTool;
import opennlp.tools.cmdline.CmdLineUtil;
import opennlp.tools.cmdline.TerminateToolException;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;

public class ChunkerTrainerTool implements CmdLineTool {

    public String getName() {
        return "ChunkerTrainerME";
    }

    public String getShortDescription() {
        return "trainer for the learnable chunker";
    }

    public String getHelp() {
        return "Usage: " + CLI.CMD + " " + getName() + BasicTrainingParameters.getParameterUsage() + " -data trainingData -model model\n" + BasicTrainingParameters.getDescription();
    }

    static ObjectStream<ChunkSample> openSampleData(String sampleDataName, File sampleDataFile, Charset encoding) {
        CmdLineUtil.checkInputFile(sampleDataName + " Data", sampleDataFile);
        FileInputStream sampleDataIn = CmdLineUtil.openInFile(sampleDataFile);
        ObjectStream<String> lineStream = new PlainTextByLineStream(sampleDataIn.getChannel(), encoding);
        return new ChunkSampleStream(lineStream);
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
        CmdLineUtil.checkOutputFile("sentence detector model", modelOutFile);
        ObjectStream<ChunkSample> sampleStream = openSampleData("Training", trainingDataInFile, parameters.getEncoding());
        ChunkerModel model;
        try {
            model = ChunkerME.train(parameters.getLanguage(), sampleStream, parameters.getCutoff(), parameters.getNumberOfIterations());
        } catch (IOException e) {
            CmdLineUtil.printTrainingIoError(e);
            throw new TerminateToolException(-1);
        } finally {
            try {
                sampleStream.close();
            } catch (IOException e) {
            }
        }
        CmdLineUtil.writeModel("chunker", modelOutFile, model);
    }
}
