package opennlp.tools.sentdetect;

import java.io.FileInputStream;
import java.io.IOException;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;
import opennlp.tools.util.eval.CrossValidationPartitioner;
import opennlp.tools.util.eval.FMeasure;

/**
 * 
 */
public class SDCrossValidator {

    private final String languageCode;

    private final int cutoff;

    private final int iterations;

    private FMeasure fmeasure = new FMeasure();

    public SDCrossValidator(String languageCode, int cutoff, int iterations) {
        this.languageCode = languageCode;
        this.cutoff = cutoff;
        this.iterations = iterations;
    }

    public SDCrossValidator(String languageCode) {
        this(languageCode, 5, 100);
    }

    public void evaluate(ObjectStream<SentenceSample> samples, int nFolds) throws IOException {
        CrossValidationPartitioner<SentenceSample> partitioner = new CrossValidationPartitioner<SentenceSample>(samples, nFolds);
        while (partitioner.hasNext()) {
            CrossValidationPartitioner.TrainingSampleStream<SentenceSample> trainingSampleStream = partitioner.next();
            SentenceModel model = SentenceDetectorME.train(languageCode, trainingSampleStream, true, null, cutoff, iterations);
            SentenceDetectorEvaluator evaluator = new SentenceDetectorEvaluator(new SentenceDetectorME(model));
            evaluator.evaluate(trainingSampleStream.getTestSampleStream());
            fmeasure.mergeInto(evaluator.getFMeasure());
        }
    }

    public FMeasure getFMeasure() {
        return fmeasure;
    }

    @Deprecated
    public static void main(String[] args) throws Exception {
        SDCrossValidator cv = new SDCrossValidator("en");
        cv.evaluate(new SentenceSampleStream(new PlainTextByLineStream(new FileInputStream("/home/joern/Infopaq/opennlp.data/en/eos/eos.all").getChannel(), "ISO-8859-1")), 10);
        System.out.println(cv.getFMeasure().toString());
    }
}
