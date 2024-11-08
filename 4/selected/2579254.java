package opennlp.tools.tokenize;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectStreamException;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;
import opennlp.tools.util.eval.CrossValidationPartitioner;
import opennlp.tools.util.eval.FMeasure;

public class TokenizerCrossValidator {

    private final String language;

    private final boolean alphaNumericOptimization;

    private final int cutoff;

    private final int iterations;

    private FMeasure fmeasure = new FMeasure();

    public TokenizerCrossValidator(String language, boolean alphaNumericOptimization, int cutoff, int iterations) {
        this.language = language;
        this.alphaNumericOptimization = alphaNumericOptimization;
        this.cutoff = cutoff;
        this.iterations = iterations;
    }

    public TokenizerCrossValidator(String language, boolean alphaNumericOptimization) {
        this(language, alphaNumericOptimization, 5, 100);
    }

    public void evaluate(ObjectStream<TokenSample> samples, int nFolds) throws IOException {
        CrossValidationPartitioner<TokenSample> partitioner = new CrossValidationPartitioner<TokenSample>(samples, nFolds);
        while (partitioner.hasNext()) {
            CrossValidationPartitioner.TrainingSampleStream<TokenSample> trainingSampleStream = partitioner.next();
            TokenizerModel model = TokenizerME.train(language, trainingSampleStream, alphaNumericOptimization, cutoff, iterations);
            TokenizerEvaluator evaluator = new TokenizerEvaluator(new TokenizerME(model));
            evaluator.evaluate(trainingSampleStream.getTestSampleStream());
            fmeasure.mergeInto(evaluator.getFMeasure());
        }
    }

    public FMeasure getFMeasure() {
        return fmeasure;
    }

    private static void usage() {
        System.err.println("Usage: TokenizerCrossValidator -encoding charset -lang language trainData");
        System.err.println("-encoding charset specifies the encoding which should be used ");
        System.err.println("                  for reading and writing text.");
        System.err.println("-lang language    specifies the language which ");
        System.err.println("                  is being processed.");
        System.exit(1);
    }

    @Deprecated
    public static void main(String[] args) throws IOException, ObjectStreamException {
        int ai = 0;
        String encoding = null;
        String lang = null;
        if (args.length != 5) {
            usage();
        }
        while (args[ai].startsWith("-")) {
            if (args[ai].equals("-encoding")) {
                ai++;
                if (ai < args.length) {
                    encoding = args[ai];
                    ai++;
                } else {
                    usage();
                }
            } else if (args[ai].equals("-lang")) {
                ai++;
                if (ai < args.length) {
                    lang = args[ai];
                    ai++;
                } else {
                    usage();
                }
            } else {
                usage();
            }
        }
        File trainingDataFile = new File(args[ai++]);
        FileInputStream trainingDataIn = new FileInputStream(trainingDataFile);
        ObjectStream<String> lineStream = new PlainTextByLineStream(trainingDataIn.getChannel(), encoding);
        ObjectStream<TokenSample> sampleStream = new TokenSampleStream(lineStream);
        TokenizerCrossValidator validator = new TokenizerCrossValidator(lang, false);
        validator.evaluate(sampleStream, 10);
        FMeasure result = validator.getFMeasure();
        System.out.println(result.toString());
    }
}
