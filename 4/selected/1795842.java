package org.phramer.v1.decoder;

import info.olteanu.utils.*;
import info.olteanu.utils.io.*;
import java.io.*;
import java.util.*;
import org.phramer.*;
import org.phramer.v1.decoder.extensionifs.*;
import org.phramer.v1.decoder.instrumentation.*;
import org.phramer.v1.decoder.instrumentation.analysis.*;
import org.phramer.v1.decoder.lm.*;
import org.phramer.v1.decoder.lm.context.*;
import org.phramer.v1.decoder.lm.processor.*;
import org.phramer.v1.decoder.loader.*;
import org.phramer.v1.decoder.math.*;
import org.phramer.v1.decoder.table.*;
import org.phramer.v1.decoder.token.*;

public class PhramerConfig {

    private static class ClassInstantiation {

        public static final String[] _NAMES = { "-x-unk-phrases", "-x-translation-variants-inspector", "-x-constraint-processor", "-x-custom-probability-calculator" };

        public static final int OOTT_PHRASE_GENERATOR = 0;

        public static final int INSPECTOR = 1;

        public static final int CONSTRAINT = 2;

        public static final int CUSTOM_PROBABILITY_CALCULATOR = 3;
    }

    private boolean isRescoring;

    public Instrument instrument;

    public TokenBuilder tokenBuilder;

    public PhramerHelperIf helper;

    public PrintStream verboseDump;

    public TranslationTable translationTable;

    public LMProcessor lmProcessor;

    public ContextComparator contextComparator;

    private boolean ttStoreDetails = false;

    public ConstraintChecker constraintChecker;

    public ConstraintCreator constraintCreator;

    public OutOfTranslationTablePhraseGenerator unkWordsPhraseVariants;

    public PhraseTranslationVariantInspector translationVariantsInspector;

    public CustomProbability customProbability;

    public boolean calculateLM;

    public FutureCostInstrument futureCostInstrument = null;

    public String encodingLM;

    public String encodingTT;

    public String encodingInput;

    public String encodingOutput;

    public int onlyNonPositiveLogProbabilitiesLevel;

    public int compatibilityLevel;

    public String futureCostCalculatorClass;

    public String unkWordsTransliteratorClass;

    public String[] _class;

    public String[][] _param;

    /** How long can be a phrase?
	 * It is designed to accelerate and reduce memory consumption.
	 * It is supposed to help non-memory TTs
	 * 0 = infinite
	 * Default value (10) */
    public int maxPhraseLength;

    /** How many words are needed to compute the probability of adding a new word into the sentence. <br>
	 * If a n-gram model is used, lmContextLength = n-1.
	 */
    public int lmContextLength;

    /** Translate chunks, not sentences. Don't add &lt;s> and &lt;/s> in language model
	 */
    public boolean chunk;

    public Boolean simpleScoreFiles;

    public String[] scoreMask;

    public String scorePath;

    public int scoreN;

    public String nBestPath;

    public int nBestN;

    public boolean nBestIncludeP;

    /** If a phrase <w1 .. wk> is in the translation table, then also <w1 .. wk-1> is in the translation table */
    public boolean prefixTT;

    public double[] weightX;

    public boolean trace;

    public int verboseLevel;

    public String lattice;

    private double[] weightT;

    public double[] weightT() {
        return weightT;
    }

    public double[] weightL() {
        return _lmWeights;
    }

    public double weightD;

    public double weightW;

    public int tTableLimit;

    public double tTableThreshold;

    public int stack;

    private double beamThreshold;

    public double logBeamThreshold;

    public int distortionLimit;

    public boolean monotone;

    public boolean bypassMarked;

    public double weightMarked;

    public void reparseCommandLine(String commandLine) throws PhramerException, IOException {
        String[] cmdLine = CommandLineTools.tokenize(commandLine);
        String configFile = CommandLineTools.getParameter("-f", cmdLine);
        if (configFile == null) configFile = CommandLineTools.getParameter("-config", cmdLine);
        if (configFile != null) throw new PhramerException("cannot re-read config file");
        HashMap<String, Object> params = CommandLineTools.getParameters(cmdLine, "-");
        Set<String> keys = params.keySet();
        for (String key : keys) if (params.get(key) == null) parameterNull(key, true, true); else if (params.get(key) instanceof String) parameterSingle(key, (String) params.get(key), true, true); else parameterMulti(key, (String[]) params.get(key), true, true);
        if (!translationTable.readjustWeights(isRescoring ? Integer.MAX_VALUE : tTableLimit, tTableThreshold, weightT)) translationTable = helper.loadTranslationTable(tokenBuilder, _ttFile, encodingTT, isRescoring ? Integer.MAX_VALUE : tTableLimit, maxPhraseLength == 0 ? Integer.MAX_VALUE : maxPhraseLength, tTableThreshold, weightT, ttStoreDetails);
        for (int i = 0; i < _languageModelFiles.length; i++) {
            double logProbabilityUnk = -10;
            if (_lmUnkLogProbability != null) logProbabilityUnk = _lmUnkLogProbability[i];
            double minLogProbability = -10;
            if (_lmMinLogProbability != null) minLogProbability = _lmMinLogProbability[i];
            lmProcessor.reconfigure(i, _lmWeights[i], logProbabilityUnk, minLogProbability);
        }
        contextComparator = helper.getContextComparator(lmContextLength);
    }

    public PhramerConfig(String commandLine, PhramerHelperIf helper, Instrument instrument) throws IOException, PhramerException {
        this();
        String[] cmdLine = CommandLineTools.tokenize(commandLine);
        initialize(cmdLine, helper, instrument);
    }

    public PhramerConfig(String[] cmdLine, PhramerHelperIf helper, Instrument instrument) throws IOException, PhramerException {
        this();
        initialize(cmdLine, helper, instrument);
    }

    private void initialize(String[] cmdLine, PhramerHelperIf helper, Instrument instrument) throws PhramerException, IOException {
        String configFile = CommandLineTools.getParameter("-f", cmdLine);
        if (configFile == null) configFile = CommandLineTools.getParameter("-config", cmdLine);
        if (configFile != null) {
            String activeParameter = null;
            Vector<String> values = new Vector<String>();
            BufferedReader inputFile = new BufferedReader(new InputStreamReader(IOTools.getInputStream(configFile), PhramerTools.getDefaultEncodingDataFiles()));
            String lineFile;
            while ((lineFile = inputFile.readLine()) != null) {
                lineFile = lineFile.trim();
                if (lineFile.startsWith("#") || lineFile.length() == 0) continue;
                if (lineFile.startsWith("[") && lineFile.endsWith("]")) {
                    if (activeParameter != null) commitParams(values, activeParameter);
                    activeParameter = "-" + lineFile.substring(1, lineFile.length() - 1);
                } else values.add(lineFile);
            }
            inputFile.close();
            if (activeParameter != null) commitParams(values, activeParameter);
            assert values.size() == 0;
        }
        HashMap<String, Object> params = CommandLineTools.getParameters(cmdLine, "-");
        Set<String> keys = params.keySet();
        for (String key : keys) if (params.get(key) == null) parameterNull(key, true, false); else if (params.get(key) instanceof String) parameterSingle(key, (String) params.get(key), true, false); else parameterMulti(key, (String[]) params.get(key), true, false);
        if (_ttFile == null) throw new PhramerException("no phrase translation table");
        if (_languageModelFiles == null) throw new PhramerException("no language model");
        if (!_weightD) throw new PhramerException("no weight-d");
        if (!_weightL) throw new PhramerException("no weight-l");
        if (!_weightT) throw new PhramerException("no weight-t");
        if (!_weightW) throw new PhramerException("no weight-w");
        if (_languageModelFiles.length != _lmWeights.length) throw new PhramerException("no match between # of LMs and # of LM weights");
        this.instrument = getInstrument(instrument);
        if (helper == null) helper = PhramerHelperSimpleImpl.get(unkWordsTransliteratorClass);
        this.helper = helper;
        tokenBuilder = helper.getTokenBuilder();
        if (_languageModelFiles.length != _lmWeights.length) throw new PhramerException("weight-l doesn't match lmodel-file");
        if (_lmUnkLogProbability != null) if (_languageModelFiles.length != _lmUnkLogProbability.length) throw new PhramerException("x-oov-probability doesn't match lmodel-file");
        if (_lmMinLogProbability != null) if (_languageModelFiles.length != _lmMinLogProbability.length) throw new PhramerException("x-lm-lbound doesn't match lmodel-file");
        contextComparator = helper.getContextComparator(lmContextLength);
        if (_class[ClassInstantiation.OOTT_PHRASE_GENERATOR] != null) unkWordsPhraseVariants = (OutOfTranslationTablePhraseGenerator) PhramerTools.instantiateClass(_class[ClassInstantiation.OOTT_PHRASE_GENERATOR], this, _param[ClassInstantiation.OOTT_PHRASE_GENERATOR]);
        if (_class[ClassInstantiation.INSPECTOR] != null) translationVariantsInspector = (PhraseTranslationVariantInspector) PhramerTools.instantiateClass(_class[ClassInstantiation.INSPECTOR], this, _param[ClassInstantiation.INSPECTOR]);
        if (_class[ClassInstantiation.CONSTRAINT] != null) {
            ConstraintProcessor cp = (ConstraintProcessor) PhramerTools.instantiateClass(_class[ClassInstantiation.CONSTRAINT], this, _param[ClassInstantiation.CONSTRAINT]);
            constraintChecker = cp;
            constraintCreator = cp;
        }
        if (_class[ClassInstantiation.CUSTOM_PROBABILITY_CALCULATOR] != null) {
            customProbability = (CustomProbability) PhramerTools.instantiateClass(_class[ClassInstantiation.CUSTOM_PROBABILITY_CALCULATOR], this, _param[ClassInstantiation.CUSTOM_PROBABILITY_CALCULATOR]);
            ttStoreDetails |= customProbability.requiresDetailedProbabilitiesInTranslationTable();
        }
        if (weightX != null && customProbability == null || weightX == null && customProbability != null) throw new PhramerException("-weight-x need to pair -x-custom-probability-calculator");
        if (weightX != null) if (weightX.length != customProbability.getNoProbabilities()) throw new PhramerException("-weight-x doesn't match -x-custom-probability-calculator (weightX:" + weightX.length + " expected:" + customProbability.getNoProbabilities() + ")");
        translationTable = helper.loadTranslationTable(tokenBuilder, _ttFile, encodingTT, isRescoring ? Integer.MAX_VALUE : tTableLimit, maxPhraseLength == 0 ? Integer.MAX_VALUE : maxPhraseLength, tTableThreshold, weightT, ttStoreDetails);
        LanguageModelIf lm[] = new LanguageModelIf[_languageModelFiles.length];
        for (int i = 0; i < _languageModelFiles.length; i++) lm[i] = helper.loadLanguageModel(_languageModelFiles[i], encodingLM, i);
        if (lm.length == 1 && lm[0].isVocabularyBasedLM() && lm[0].allowFastCall()) lmProcessor = new Vocabulary1LMProcessor(0); else lmProcessor = new GenericLMProcessor(_languageModelFiles.length, 0, helper.getNullContext(lmContextLength));
        for (int i = 0; i < _languageModelFiles.length; i++) {
            double logProbabilityUnk = -10;
            if (_lmUnkLogProbability != null) logProbabilityUnk = _lmUnkLogProbability[i];
            double minLogProbability = -10;
            if (_lmMinLogProbability != null) minLogProbability = _lmMinLogProbability[i];
            lmProcessor.registerLM(lm[i], helper.loadPreprocessor(_languageModelFiles[i], i), _lmWeights[i], logProbabilityUnk, minLogProbability);
        }
        this.instrument.config(this);
    }

    private Instrument getInstrument(Instrument instrument) {
        if (instrument == null) instrument = VeryVoidInstrument.VVI;
        if (verboseLevel >= 3) instrument = new VerboseLevel3Instrument(instrument, verboseDump, this); else if (verboseLevel == 2) instrument = new VerboseLevel2Instrument(instrument, verboseDump, this);
        return instrument;
    }

    private void commitParams(Vector<String> values, String activeParameter) throws PhramerException {
        if (values.size() == 0) parameterNull(activeParameter, false, false); else if (values.size() == 1) parameterSingle(activeParameter, values.firstElement(), false, false); else parameterMulti(activeParameter, values.toArray(new String[values.size()]), false, false);
        values.clear();
    }

    private void parameterNull(String key, boolean commandLine, boolean reapply) throws PhramerException {
        if (key.equals("-simple-sc") || key.equals("-x-score-simple")) {
            simpleScoreFiles = true;
            return;
        }
        if (key.equals("-x-chunk")) {
            chunk = true;
            return;
        }
        if (key.equals("-x-nbest-includeprob")) {
            nBestIncludeP = true;
            return;
        }
        if (key.equals("-x-prefix-tt")) {
            prefixTT = true;
            return;
        }
        if (commandLine) {
            if (key.equals("-profile")) return;
            if (key.equals("-concurrent")) return;
            if (key.equals("-rescore") || key.equals("-r")) {
                if (reapply && !ttStoreDetails) throw new PhramerException("Reparse command line: cannot store TT details from now on");
                ttStoreDetails = true;
                isRescoring = true;
                return;
            }
            if (key.equals("-monotone")) {
                monotone = true;
                return;
            }
            if (key.equals("-trace") || key.equals("-t")) {
                trace = true;
                return;
            }
            if (key.equals("-bypass-marked")) {
                bypassMarked = true;
                return;
            }
        }
        throw new PhramerException("Invalid parameter: " + key);
    }

    private void parameterSingle(String key, String value, boolean commandLine, boolean reapply) throws PhramerException {
        if (key.equals("-x-level-good-probabilities")) {
            onlyNonPositiveLogProbabilitiesLevel = Integer.parseInt(value);
            return;
        }
        if (key.equals("-x-score-mask") || key.equals("-sc-mask")) {
            scoreMask = StringTools.tokenize(value, ",");
            return;
        }
        if (key.equals("-compatibility")) {
            compatibilityLevel = PhramerTools.getCompatibilityLevel(value);
            return;
        }
        if (key.equals("-x-future-cost-class")) {
            futureCostCalculatorClass = value;
            return;
        }
        if (key.equals("-ttable-file")) {
            _ttFile = value;
            return;
        }
        if (key.equals("-lmodel-file")) {
            _languageModelFiles = new String[1];
            _languageModelFiles[0] = value;
            return;
        }
        if (key.equals("-weight-x") || key.equals("-px")) {
            if (reapply && (weightX.length != 1)) throw new PhramerException("Reparse command line: different number of weight-x");
            weightX = new double[1];
            weightX[0] = Double.parseDouble(value);
            return;
        }
        if (key.equals("-weight-t") || key.equals("-tm")) {
            if (reapply && (weightT.length != 1)) throw new PhramerException("Reparse command line: different number of weight-t");
            weightT = new double[1];
            weightT[0] = Double.parseDouble(value);
            _weightT = true;
            return;
        }
        if (key.equals("-weight-l") || key.equals("-lm")) {
            if (reapply && (_lmWeights.length != 1)) throw new PhramerException("Reparse command line: different number of weight-l");
            _lmWeights = new double[1];
            _lmWeights[0] = Double.parseDouble(value);
            _weightL = true;
            return;
        }
        if (key.equals("-weight-d") || key.equals("-d")) {
            weightD = Double.parseDouble(value);
            _weightD = true;
            return;
        }
        if (key.equals("-weight-w") || key.equals("-w")) {
            weightW = Double.parseDouble(value);
            _weightW = true;
            return;
        }
        if (key.equals("-x-oov-probability") || key.equals("-x-oov")) {
            if (reapply && (_lmUnkLogProbability.length != 1)) throw new PhramerException("Reparse command line: different number of x-oov-probability");
            _lmUnkLogProbability = new double[1];
            _lmUnkLogProbability[0] = Double.parseDouble(value);
            return;
        }
        if (key.equals("-x-lm-lbound") || key.equals("-x-lb")) {
            if (reapply && (_lmMinLogProbability.length != 1)) throw new PhramerException("Reparse command line: different number of x-lm-lbound");
            _lmMinLogProbability = new double[1];
            _lmMinLogProbability[0] = Double.parseDouble(value);
            return;
        }
        if (key.equals("-x-context-length")) {
            lmContextLength = Integer.parseInt(value);
            return;
        }
        if (key.equals("-x-max-phrase-length")) {
            maxPhraseLength = Integer.parseInt(value);
            return;
        }
        if (key.equals("-ttable-limit")) {
            tTableLimit = Integer.parseInt(value);
            return;
        }
        if (key.equals("-ttable-threshold")) {
            tTableThreshold = Double.parseDouble(value);
            return;
        }
        if (key.equals("-stack") || key.equals("-s")) {
            stack = Integer.parseInt(value);
            return;
        }
        if (key.equals("-beam-threshold") || key.equals("-b")) {
            beamThreshold = Double.parseDouble(value);
            logBeamThreshold = MathTools.numberToLog(beamThreshold);
            return;
        }
        if (key.equals("-distortion-limit") || key.equals("-dl")) {
            distortionLimit = Integer.parseInt(value);
            return;
        }
        if (key.equals("-weight-marked")) {
            weightMarked = Double.parseDouble(value);
            return;
        }
        if (key.equals("-lmenc")) {
            encodingLM = value;
            return;
        }
        if (key.equals("-ttenc")) {
            encodingTT = value;
            return;
        }
        for (int i = 0; i < _class.length; i++) {
            if (key.equals(ClassInstantiation._NAMES[i])) {
                _class[i] = value;
                return;
            }
            if (key.equals(ClassInstantiation._NAMES[i] + "-param")) {
                _param[i] = new String[1];
                _param[i][0] = value;
                return;
            }
        }
        if (commandLine) {
            if (key.equals("-x-unk-words-transliterator")) {
                unkWordsTransliteratorClass = value;
                return;
            }
            if (key.equals("-inputtype")) return;
            if (key.equals("-config") || key.equals("-f")) return;
            if (key.equals("-server")) return;
            if (key.equals("-threads")) return;
            if (key.equals("-cache")) return;
            if (key.equals("-renc")) {
                encodingInput = value;
                return;
            }
            if (key.equals("-wenc")) {
                encodingOutput = value;
                return;
            }
            if (key.equals("-rescore") || key.equals("-r")) {
                ttStoreDetails = true;
                return;
            }
            if (key.equals("-verbose") || key.equals("-v")) {
                verboseLevel = Integer.parseInt(value);
                return;
            }
            if (key.equals("-lattice") || key.equals("-l")) {
                lattice = value;
                return;
            }
            if (key.equals("-read") || key.equals("-write") || key.equals("-start-id")) return;
        }
        throw new PhramerException("Invalid parameter: " + key);
    }

    private void parameterMulti(String key, String[] value, boolean commandLine, boolean reapply) throws PhramerException {
        if (key.equals("-lmodel-file")) {
            _languageModelFiles = value;
            return;
        }
        if (key.equals("-weight-t") || key.equals("-tm")) {
            if (reapply && (weightT.length != value.length)) throw new PhramerException("Reparse command line: different number of weight-t");
            weightT = new double[value.length];
            for (int i = 0; i < value.length; i++) weightT[i] = Double.parseDouble(value[i]);
            _weightT = true;
            return;
        }
        if (key.equals("-weight-l") || key.equals("-lm")) {
            if (reapply && (_lmWeights.length != value.length)) throw new PhramerException("Reparse command line: different number of weight-l");
            _lmWeights = new double[value.length];
            for (int i = 0; i < value.length; i++) _lmWeights[i] = Double.parseDouble(value[i]);
            _weightL = true;
            return;
        }
        if (key.equals("-weight-x") || key.equals("-px")) {
            if (reapply && (weightX.length != value.length)) throw new PhramerException("Reparse command line: different number of weight-x");
            weightX = new double[value.length];
            for (int i = 0; i < value.length; i++) weightX[i] = Double.parseDouble(value[i]);
            return;
        }
        if (key.equals("-x-oov-probability") || key.equals("-x-oov")) {
            if (reapply && (_lmUnkLogProbability.length != value.length)) throw new PhramerException("Reparse command line: different number of x-oov-probability");
            _lmUnkLogProbability = new double[value.length];
            for (int i = 0; i < value.length; i++) _lmUnkLogProbability[i] = Double.parseDouble(value[i]);
            return;
        }
        if (key.equals("-x-lm-lbound") || key.equals("-x-lb")) {
            if (reapply && (_lmMinLogProbability.length != value.length)) throw new PhramerException("Reparse command line: different number of x-lm-lbound");
            _lmMinLogProbability = new double[value.length];
            for (int i = 0; i < value.length; i++) _lmMinLogProbability[i] = Double.parseDouble(value[i]);
            return;
        }
        for (int i = 0; i < _class.length; i++) if (key.equals(ClassInstantiation._NAMES[i] + "-param")) {
            _param[i] = value;
            return;
        }
        if (commandLine) {
            if (key.equals("-rescoredir") || key.equals("-rd")) {
                if (value.length != 2) throw new PhramerException("Expecting 2 values for " + key + ". Got " + value.length);
                if (reapply && !ttStoreDetails) throw new PhramerException("Reparse command line: cannot store TT details from now on");
                ttStoreDetails = true;
                isRescoring = true;
                return;
            }
            if (key.equals("-x-score") || key.equals("-sc")) {
                if (value.length != 2) throw new PhramerException("Expecting 2 values for " + key + ". Got " + value.length);
                if (reapply && !ttStoreDetails) throw new PhramerException("Reparse command line: cannot store TT details from now on");
                ttStoreDetails = true;
                scorePath = value[0];
                scoreN = Integer.parseInt(value[1]);
                return;
            }
            if (key.equals("-x-nbest") || key.equals("-nb")) {
                if (value.length != 2) throw new PhramerException("Expecting 2 values for " + key + ". Got " + value.length);
                if (reapply && !ttStoreDetails) throw new PhramerException("Reparse command line: cannot store TT details from now on");
                ttStoreDetails = true;
                nBestPath = value[0];
                nBestN = Integer.parseInt(value[1]);
                return;
            }
        }
        throw new PhramerException("Invalid parameter: " + key);
    }

    private String[] _languageModelFiles;

    private double[] _lmWeights;

    private double[] _lmUnkLogProbability;

    private double[] _lmMinLogProbability;

    private String _ttFile;

    private boolean _weightT, _weightL, _weightD, _weightW;

    private PhramerConfig() {
        compatibilityLevel = Integer.MAX_VALUE;
        tTableLimit = 20;
        tTableThreshold = 0;
        stack = 100;
        beamThreshold = 0.00001;
        logBeamThreshold = MathTools.numberToLog(beamThreshold);
        distortionLimit = 0;
        monotone = false;
        onlyNonPositiveLogProbabilitiesLevel = 0;
        bypassMarked = false;
        weightMarked = 1;
        maxPhraseLength = 10;
        lmContextLength = 2;
        trace = false;
        lattice = null;
        simpleScoreFiles = false;
        scoreMask = null;
        scorePath = null;
        scoreN = 0;
        nBestPath = null;
        nBestN = 0;
        nBestIncludeP = false;
        prefixTT = false;
        calculateLM = true;
        verboseLevel = 0;
        verboseDump = System.out;
        unkWordsTransliteratorClass = null;
        unkWordsPhraseVariants = null;
        translationVariantsInspector = null;
        constraintChecker = ConstraintChecker.VOID;
        constraintCreator = null;
        customProbability = null;
        weightX = null;
        futureCostCalculatorClass = null;
        encodingInput = PhramerTools.getDefaultEncodingInput();
        encodingOutput = PhramerTools.getDefaultEncodingOutput();
        encodingTT = PhramerTools.getDefaultEncodingDataFiles();
        encodingLM = PhramerTools.getDefaultEncodingDataFiles();
        _class = new String[ClassInstantiation._NAMES.length];
        _param = new String[ClassInstantiation._NAMES.length][];
    }

    public boolean latticeUse() {
        return lattice != null || scorePath != null || nBestPath != null;
    }
}
