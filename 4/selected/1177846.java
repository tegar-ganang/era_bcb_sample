package statechum.analysis.learning.observers;

import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
import java.util.Stack;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import statechum.StatechumXML;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Element;
import statechum.Configuration;
import statechum.JUConstants;
import statechum.Label;
import statechum.Pair;
import statechum.analysis.learning.Learner;
import statechum.analysis.learning.PairScore;
import statechum.analysis.learning.StatePair;
import statechum.analysis.learning.rpnicore.AbstractPersistence;
import statechum.analysis.learning.rpnicore.LearnerGraph;
import statechum.model.testset.PTASequenceEngine;

/** Stores some arguments and results of calls to learner's methods 
 * so that one could later test if a particular learner's behaviour 
 * exactly matches that of a known learner. 
 *
 * @author kirill
 */
public class RecordProgressDecorator extends ProgressDecorator {

    protected OutputStream outputStream = null;

    /** The top-most element of the trace log file. */
    protected Element topElement = null;

    /** Whether to write pure XML files or zip files where each entry is the compressed XML entry.
	 * We cannot keep half-gig of XML in memory - have to split and compress. 
	 */
    protected boolean writeZip = true;

    /** Graph compressor. */
    protected GraphSeries series = null;

    public RecordProgressDecorator(Learner learner, OutputStream outStream, int threadNumber, Configuration conf, boolean writeInZipFormat) {
        super(learner);
        config = conf;
        writeZip = writeInZipFormat;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            factory.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            factory.setValidating(false);
            doc = factory.newDocumentBuilder().newDocument();
            if (writeZip) {
                outputStream = new ZipOutputStream(new java.io.BufferedOutputStream(outStream));
            } else {
                outputStream = outStream;
                topElement = doc.createElement(StatechumXML.ELEM_STATECHUM_TESTTRACE.name());
                doc.appendChild(topElement);
                topElement.appendChild(AbstractPersistence.endl(doc));
            }
            Configuration seriesConfiguration = config.copy();
            seriesConfiguration.setGdMaxNumberOfStatesInCrossProduct(0);
            series = new GraphSeries(doc, threadNumber, seriesConfiguration);
            initIO(doc, config);
        } catch (ParserConfigurationException e) {
            statechum.Helper.throwUnchecked("failed to construct DOM document", e);
        }
    }

    /** Writes the outcome of learning into the log and closes the log.
	 * 
	 * @param graph the outcome of learning.
	 */
    protected void writeResult(LearnerGraph graph) {
        Element finalGraphXMLNode = series.writeGraph(graph);
        finalGraphXMLNode.setAttribute(StatechumXML.ATTR_GRAPHKIND.name(), StatechumXML.ATTR_LEARNINGOUTCOME.name());
        writeElement(finalGraphXMLNode);
    }

    /** Closes the trace log, writing the constructed XML out. */
    public void close() {
        try {
            if (!writeZip) {
                Transformer trans = TransformerFactory.newInstance().newTransformer();
                trans.transform(new DOMSource(doc), new StreamResult(outputStream));
            }
            outputStream.close();
        } catch (Exception e) {
            statechum.Helper.throwUnchecked("failed to write out XML ", e);
        } finally {
            doc = null;
            topElement = null;
        }
    }

    /** Converts an integer to a string, padded with zeroes. 
	 * Does not use printf - it's too simple for that. 
	 * 
	 * <p>
	 * @param number integer number to convert. 
	 * @param digits the minimal number of digits, used for alignment. 
	 * @return result of conversion.
	 */
    public static String intToString(int number, int digits) {
        assert number >= 0;
        String value = Integer.toString(number);
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < digits - value.length(); ++i) result.append('0');
        result.append(value);
        return result.toString();
    }

    /** Determines whether compression is used or not. */
    protected int compressionMethod = ZipEntry.DEFLATED;

    public void setCompressionMethod(int newValue) {
        compressionMethod = newValue;
    }

    /** Used to give all entries in a zip file unique names. */
    protected int entryNumber = 1;

    /** Writes the supplied XML element out, either to XML file or to Zip stream. 
	 * The destination of data depends on the <em>writeZip</em> attribute.
	 * 
	 * @param elem what to write out.
	 */
    protected void writeElement(Element elem) {
        if (writeZip) {
            try {
                doc.appendChild(elem);
                ZipEntry entry = new ZipEntry(intToString(entryNumber++, 8) + "_" + elem.getNodeName());
                entry.setMethod(compressionMethod);
                ((ZipOutputStream) outputStream).putNextEntry(entry);
                Transformer trans = TransformerFactory.newInstance().newTransformer();
                trans.transform(new DOMSource(doc), new StreamResult(outputStream));
                doc.removeChild(elem);
            } catch (Exception e) {
                statechum.Helper.throwUnchecked("failed to write out XML ", e);
            }
        } else {
            topElement.appendChild(elem);
            topElement.appendChild(AbstractPersistence.endl(doc));
        }
    }

    @Override
    public LearnerGraph learnMachine(final PTASequenceEngine engine, int plusSize, int minusSize) {
        LearnerGraph graph = decoratedLearner.learnMachine(engine, plusSize, minusSize);
        writeResult(graph);
        close();
        return graph;
    }

    @Override
    public LearnerGraph learnMachine(Collection<List<Label>> plus, Collection<List<Label>> minus) {
        LearnerGraph graph = decoratedLearner.learnMachine(plus, minus);
        writeResult(graph);
        close();
        return graph;
    }

    @Override
    public Pair<Integer, String> CheckWithEndUser(LearnerGraph graph, List<Label> question, int responseForNoRestart, List<Boolean> acceptedElements, PairScore pairBeingMerged, Object[] options) {
        Element questionElement = doc.createElement(StatechumXML.ELEM_ANSWER.name());
        Pair<Integer, String> result = decoratedLearner.CheckWithEndUser(graph, question, responseForNoRestart, acceptedElements, pairBeingMerged, options);
        StringBuffer strBuffer = new StringBuffer();
        labelio.writeInputSequence(strBuffer, question);
        questionElement.setAttribute(StatechumXML.ATTR_QUESTION.name(), strBuffer.toString());
        questionElement.setAttribute(StatechumXML.ATTR_FAILEDPOS.name(), result.firstElem.toString());
        if (result.secondElem != null) questionElement.setAttribute(StatechumXML.ATTR_LTL.name(), result.secondElem);
        writeElement(questionElement);
        return result;
    }

    @Override
    public Stack<PairScore> ChooseStatePairs(LearnerGraph graph) {
        Stack<PairScore> result = decoratedLearner.ChooseStatePairs(graph);
        Element pairsElement = doc.createElement(StatechumXML.ELEM_PAIRS.name());
        for (PairScore p : result) {
            pairsElement.appendChild(writePair(p, doc));
            pairsElement.appendChild(AbstractPersistence.endl(doc));
        }
        writeElement(pairsElement);
        return result;
    }

    @Override
    public List<List<Label>> ComputeQuestions(PairScore pair, LearnerGraph original, LearnerGraph temp) {
        List<List<Label>> result = decoratedLearner.ComputeQuestions(pair, original, temp);
        Element questions = doc.createElement(StatechumXML.ELEM_QUESTIONS.name());
        Element questionList = labelio.writeSequenceList(StatechumXML.ATTR_QUESTIONS.name(), result);
        questions.appendChild(questionList);
        questions.appendChild(writePair(pair, doc));
        writeElement(questions);
        return result;
    }

    @Override
    public List<List<Label>> RecomputeQuestions(PairScore pair, LearnerGraph original, LearnerGraph temp) {
        List<List<Label>> result = decoratedLearner.RecomputeQuestions(pair, original, temp);
        Element questions = doc.createElement(StatechumXML.ELEM_QUESTIONS.name());
        Element questionList = labelio.writeSequenceList(StatechumXML.ATTR_MOREQUESTIONS.name(), result);
        questions.appendChild(questionList);
        questions.appendChild(writePair(pair, doc));
        writeElement(questions);
        return result;
    }

    /** Stores the current learner input parameters. */
    public void writeLearnerEvaluationData(LearnerEvaluationConfiguration cnf) {
        writeElement(writeLearnerEvaluationConfiguration(cnf));
    }

    @Override
    public LearnerGraph MergeAndDeterminize(LearnerGraph original, StatePair pair) {
        LearnerGraph result = decoratedLearner.MergeAndDeterminize(original, pair);
        Element mergedGraph = series.writeGraph(result);
        Element mergeNode = doc.createElement(StatechumXML.ELEM_MERGEANDDETERMINIZE.name());
        mergeNode.appendChild(mergedGraph);
        mergeNode.appendChild(writePair(new PairScore(pair.getQ(), pair.getR(), 0, 0), doc));
        writeElement(mergeNode);
        return result;
    }

    /** We deliberately avoid storing this so as to be able to change 
	 * the format of diagnostics without having to regenerate test data. 
	 */
    @Override
    public String getResult() {
        String result = decoratedLearner.getResult();
        return result;
    }

    @Override
    public LearnerGraph init(final PTASequenceEngine engine, int plusSize, int minusSize) {
        LearnerGraph result = decoratedLearner.init(engine, plusSize, minusSize);
        final PTASequenceEngine.FilterPredicate positiveFilter = engine.getFSM_filterPredicate(), negativeFilter = new PTASequenceEngine.FilterPredicate() {

            @Override
            public boolean shouldBeReturned(Object name) {
                return !positiveFilter.shouldBeReturned(name);
            }
        };
        writeElement(writeInitialData(new InitialData(engine.getData(positiveFilter), plusSize, engine.getData(negativeFilter), minusSize, result)));
        return result;
    }

    @Override
    public LearnerGraph init(Collection<List<Label>> plus, Collection<List<Label>> minus) {
        LearnerGraph result = decoratedLearner.init(plus, minus);
        writeElement(writeInitialData(new InitialData(plus, plus.size(), minus, minus.size(), result)));
        return result;
    }

    @Override
    public void Restart(RestartLearningEnum mode) {
        decoratedLearner.Restart(mode);
        Element restartElement = doc.createElement(StatechumXML.ELEM_RESTART.name());
        restartElement.setAttribute(StatechumXML.ATTR_KIND.name(), mode.toString());
        writeElement(restartElement);
        if (mode != RestartLearningEnum.restartNONE) series.reset();
    }

    @Override
    public void AugmentPTA(LearnerGraph pta, RestartLearningEnum ptaKind, List<Label> sequence, boolean accepted, JUConstants newColour) {
        decoratedLearner.AugmentPTA(pta, ptaKind, sequence, accepted, newColour);
        writeElement(writeAugmentPTA(new AugmentPTAData(ptaKind, sequence, accepted, newColour)));
    }

    @Override
    public boolean AddConstraints(LearnerGraph graph, LearnerGraph outcome, StringBuffer counterExampleHolder) {
        boolean result = decoratedLearner.AddConstraints(graph, outcome, counterExampleHolder);
        Element ptaWithConstraintsGraphXMLNode = series.writeGraph(outcome);
        ptaWithConstraintsGraphXMLNode.setAttribute(StatechumXML.ATTR_GRAPHKIND.name(), StatechumXML.ATTR_WITHCONSTRAINTS.name());
        ptaWithConstraintsGraphXMLNode.setAttribute(StatechumXML.ATTR_CONSTRAINTSADDED.name(), Boolean.toString(result));
        writeElement(ptaWithConstraintsGraphXMLNode);
        return result;
    }
}
