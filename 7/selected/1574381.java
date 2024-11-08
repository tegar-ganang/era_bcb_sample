package maui.filters;

import gnu.trove.TIntHashSet;
import java.io.File;
import java.io.IOException;
import java.lang.Math;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.wikipedia.miner.model.Anchor;
import org.wikipedia.miner.model.Article;
import org.wikipedia.miner.model.Wikipedia;
import org.wikipedia.miner.model.Anchor.Sense;
import org.wikipedia.miner.util.ProgressNotifier;
import org.wikipedia.miner.util.SortedVector;
import org.wikipedia.miner.util.text.CaseFolder;
import org.wikipedia.miner.util.text.TextProcessor;
import maui.stemmers.PorterStemmer;
import maui.stemmers.Stemmer;
import maui.stopwords.Stopwords;
import maui.stopwords.StopwordsEnglish;
import maui.util.Candidate;
import maui.util.Counter;
import maui.vocab.Vocabulary;
import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import weka.core.Capabilities.Capability;
import weka.filters.Filter;
import weka.filters.supervised.attribute.Discretize;
import weka.classifiers.Classifier;
import weka.classifiers.bayes.NaiveBayesSimple;
import weka.classifiers.meta.Bagging;
import weka.classifiers.meta.FilteredClassifier;
import weka.classifiers.meta.RegressionByDiscretization;

/**
 * This filter converts the incoming data into data appropriate for keyphrase
 * classification. It assumes that the dataset contains three string attributes.
 * The first attribute should contain the name of the file. The second attribute
 * should contain the text of a document from that file. The second attribute
 * should contain the keyphrases associated with that document (if present).
 * 
 * The filter converts every instance (i.e. document) into a set of instances,
 * one for each word-based n-gram in the document. The string attribute
 * representing the document is replaced by some numeric features, the estimated
 * probability of each n-gram being a keyphrase, and the rank of this phrase in
 * the document according to the probability. Each new instances also has a
 * class value associated with it. The class is "true" if the n-gram is a true
 * keyphrase, and "false" otherwise. It is also possible to use numeric
 * attributes, if more then one manually selected keyphrase sets per document
 * were available. If the input document doesn't come with author-assigned
 * keyphrases, the class values for that document will be missing.
 * 
 * @author Eibe Frank (eibe@cs.waikato.ac.nz), Olena Medelyan
 *         (olena@cs.waikato.ac.nz)
 * @version 2.0
 */
public class MauiFilter extends Filter {

    private static final long serialVersionUID = 1L;

    /** Index of attribute containing the name of the file */
    private int fileNameAtt = 0;

    /** Index of attribute containing the documents */
    private int documentAtt = 1;

    /** Index of attribute containing the keyphrases */
    private int keyphrasesAtt = 2;

    /** Maximum length of phrases */
    private int maxPhraseLength = 5;

    /** Minimum length of phrases */
    private int minPhraseLength = 1;

    private double minKeyphraseness = 0.01;

    private double minSenseProbability = 0.05;

    private int maxContextSize = 10;

    transient TextProcessor textProcessor = new CaseFolder();

    /** Number of human indexers (times a keyphrase appears in the keyphrase set) */
    private int numIndexers = 1;

    /** Is class value nominal or numeric? * */
    private boolean nominalClassValue = true;

    /** Flag for debugging mode */
    private boolean debugMode = false;

    /** The minimum number of occurences of a phrase */
    private int minOccurFrequency = 1;

    /** The number of features describing a phrase */
    private int numFeatures = 14;

    /** Number of manually specified keyphrases */
    private int totalCorrect = 0;

    private int tfIndex = 0;

    private int idfIndex = 1;

    private int tfidfIndex = 2;

    private int firstOccurIndex = 3;

    private int lastOccurIndex = 4;

    private int spreadOccurIndex = 5;

    private int domainKeyphIndex = 6;

    private int lengthIndex = 7;

    private int generalityIndex = 8;

    private int nodeDegreeIndex = 9;

    private int semRelIndex = 10;

    private int wikipKeyphrIndex = 11;

    private int invWikipFreqIndex = 12;

    private int totalWikipKeyphrIndex = 13;

    /**
	 * Use basic features TFxIDF & First Occurrence
	 */
    boolean useBasicFeatures = true;

    /** Use keyphraseness feature */
    boolean useKeyphrasenessFeature = true;

    /**
	 * Use frequency features TF & IDF additionally
	 */
    boolean useFrequencyFeatures = true;

    /**
	 * Use occurrence position features LastOccurrence & Spread
	 */
    boolean usePositionsFeatures = true;

    /**
	 * Use thesaurus features Node degree
	 */
    boolean useNodeDegreeFeature = true;

    /** Use length feature */
    boolean useLengthFeature = true;

    /**
	 * Use basic Wikipedia features Wikipedia keyphraseness & Total Wikipedia
	 * keyphraseness
	 */
    boolean useBasicWikipediaFeatures = true;

    /**
	 * Use all Wikipedia features Inverse Wikipedia frequency & Semantic
	 * relatedness
	 */
    boolean useAllWikipediaFeatures = true;

    /** The punctuation filter used by this filter */
    private MauiPhraseFilter phraseFilter = null;

    /** The numbers filter used by this filter */
    private NumbersFilter numbersFilter = null;

    /** The actual classifier used to compute probabilities */
    private Classifier classifier = null;

    /** The dictionary containing the document frequencies */
    public HashMap<String, Counter> globalDictionary = null;

    /** The dictionary containing the keyphrases */
    private HashMap<String, Counter> keyphraseDictionary = null;

    transient HashMap<Instance, HashMap<String, Candidate>> allCandidates = null;

    /** The number of documents in the global frequencies corpus */
    private int numDocs = 0;

    /** Template for the classifier data */
    private Instances classifierData = null;

    /** Default stemmer to be used */
    private Stemmer stemmer = new PorterStemmer();

    /** List of stop words to be used */
    private Stopwords stopwords = new StopwordsEnglish();

    /** Default language to be used */
    private String documentLanguage = "en";

    /** Vocabulary object */
    public Vocabulary vocabulary;

    /** Vocabulary name */
    private String vocabularyName = "agrovoc";

    /** Vocabulary format */
    private String vocabularyFormat = "skos";

    transient Wikipedia wikipedia = null;

    public void setWikipedia(Wikipedia wikipedia) {
        this.wikipedia = wikipedia;
    }

    public void setWikipedia(String wikipediaServer, String wikipediaDatabase, boolean cacheData, String wikipediaDataDirectory) {
        try {
            if (debugMode) {
                System.err.println("--- Initializing Wikipedia database on server " + wikipediaServer + " with database " + wikipediaDatabase);
            }
            this.wikipedia = new Wikipedia(wikipediaServer, wikipediaDatabase, "root", null);
        } catch (Exception e) {
            System.err.println("Error initializing Wikipedia database!");
            e.printStackTrace();
        }
        if (cacheData && wikipediaDataDirectory != null) {
            cacheWikipediaData(wikipediaDataDirectory);
        } else if (cacheData && wikipediaDataDirectory == null) {
            System.err.println("In order to cache Wikipedia data, specify Wikipedia data directory");
        }
    }

    public void cacheWikipediaData(String wikipediaDataDirectory) {
        ProgressNotifier progress = new ProgressNotifier(5);
        File dataDirectory = new File(wikipediaDataDirectory);
        TIntHashSet validPageIds;
        try {
            validPageIds = wikipedia.getDatabase().getValidPageIds(dataDirectory, 2, progress);
            wikipedia.getDatabase().cachePages(dataDirectory, validPageIds, progress);
            wikipedia.getDatabase().cacheAnchors(dataDirectory, textProcessor, validPageIds, 2, progress);
            wikipedia.getDatabase().cacheInLinks(dataDirectory, validPageIds, progress);
        } catch (IOException e) {
            System.err.println("Error caching Wikipedia data...");
            e.printStackTrace();
        }
    }

    /**
	 * Returns the total number of manually assigned topics in a given document
	 * 
	 * @return number of manually assigned topics (int)
	 */
    public int getTotalCorrect() {
        return totalCorrect;
    }

    public void setBasicFeatures(boolean useBasicFeatures) {
        this.useBasicFeatures = useBasicFeatures;
    }

    public void setKeyphrasenessFeature(boolean useKeyphrasenessFeature) {
        this.useKeyphrasenessFeature = useKeyphrasenessFeature;
    }

    public void setFrequencyFeatures(boolean useFrequencyFeatures) {
        this.useFrequencyFeatures = useFrequencyFeatures;
    }

    public void setPositionsFeatures(boolean usePositionsFeatures) {
        this.usePositionsFeatures = usePositionsFeatures;
    }

    public void setThesaurusFeatures(boolean useThesaurusFeatures) {
        this.useNodeDegreeFeature = useThesaurusFeatures;
    }

    public void setLengthFeature(boolean useLengthFeature) {
        this.useLengthFeature = useLengthFeature;
    }

    public void setBasicWikipediaFeatures(boolean useBasicWikipediaFeatures) {
        this.useBasicWikipediaFeatures = useBasicWikipediaFeatures;
        if (useBasicWikipediaFeatures && wikipedia == null) {
            System.err.println("The Wikipedia-based features will not be computed, because the connection to the wikipedia data is not specified.");
            System.err.println("Use MauiModelBuilder.setWikipedia(\"server\", \"wikipedia database\") to set the connection!");
        }
    }

    public void setAllWikipediaFeatures(boolean useAllWikipediaFeatures) {
        this.useAllWikipediaFeatures = useAllWikipediaFeatures;
        if (useAllWikipediaFeatures && wikipedia == null) {
            System.err.println("The Wikipedia-based features will not be computed, because the connection to the wikipedia data is not specified.");
            System.err.println("Use MauiModelBuilder.setWikipedia(\"server\", \"wikipedia database\") to set the connection!");
        }
    }

    public void setStopwords(Stopwords stopwords) {
        this.stopwords = stopwords;
    }

    public void setStemmer(Stemmer stemmer) {
        this.stemmer = stemmer;
    }

    public void setNumIndexers(int numIndexers) {
        this.numIndexers = numIndexers;
    }

    public void setMinNumOccur(int minNumOccur) {
        this.minOccurFrequency = minNumOccur;
    }

    public void setMaxPhraseLength(int maxPhraseLength) {
        this.maxPhraseLength = maxPhraseLength;
    }

    public void setMinPhraseLength(int minPhraseLength) {
        this.minPhraseLength = minPhraseLength;
    }

    public void setDocumentLanguage(String documentLanguage) {
        this.documentLanguage = documentLanguage;
    }

    public void setDebug(boolean debugMode) {
        this.debugMode = debugMode;
    }

    public void setVocabularyName(String vocabularyName) {
        if (vocabularyName.equals("none")) {
            setThesaurusFeatures(false);
        }
        this.vocabularyName = vocabularyName;
    }

    public void setVocabularyFormat(String vocabularyFormat) {
        this.vocabularyFormat = vocabularyFormat;
    }

    /**
	 * Returns the index of the normalized candidate form in the output ARFF
	 * file.
	 */
    public int getNormalizedFormIndex() {
        return documentAtt;
    }

    /**
	 * Returns the index of the most frequent form for the candidate topic or
	 * the original form of it in the vocabulary in the output ARFF file.
	 */
    public int getOutputFormIndex() {
        return documentAtt;
    }

    /**
	 * Returns the index of the candidates' probabilities in the output ARFF
	 * file.
	 */
    public int getProbabilityIndex() {
        return documentAtt + numFeatures + 1;
    }

    /**
	 * Returns the index of the candidates' ranks in the output ARFF file.
	 */
    public int getRankIndex() {
        return getProbabilityIndex() + 1;
    }

    public int getDocumentAtt() {
        return documentAtt;
    }

    public void setDocumentAtt(int documentAtt) {
        this.documentAtt = documentAtt;
    }

    public int getKeyphrasesAtt() {
        return keyphrasesAtt;
    }

    public void setKeyphrasesAtt(int keyphrasesAtt) {
        this.keyphrasesAtt = keyphrasesAtt;
    }

    public void loadThesaurus(Stemmer st, Stopwords sw) {
        if (vocabulary != null) return;
        try {
            if (debugMode) {
                System.err.println("--- Loading the vocabulary...");
            }
            vocabulary = new Vocabulary(vocabularyName, vocabularyFormat);
            vocabulary.setStemmer(stemmer);
            vocabulary.setStopwords(stopwords);
            vocabulary.setDebug(debugMode);
            vocabulary.initialize();
        } catch (Exception e) {
            System.err.println("Failed to load thesaurus!");
            e.printStackTrace();
        }
    }

    /**
	 * Returns a string describing this filter
	 * 
	 * @return a description of the filter suitable for displaying in the
	 *         explorer/experimenter gui
	 */
    public String globalInfo() {
        return "Converts incoming data into data appropriate for " + "keyphrase classification.";
    }

    /**
	 * Sets the format of the input instances.
	 * 
	 * @param instanceInfo
	 *            an Instances object containing the input instance structure
	 *            (any instances contained in the object are ignored - only the
	 *            structure is required).
	 * @return true if the outputFormat may be collected immediately
	 */
    public boolean setInputFormat(Instances instanceInfo) throws Exception {
        if (instanceInfo.classIndex() >= 0) {
            throw new Exception("Don't know what do to if class index set!");
        }
        if (!instanceInfo.attribute(keyphrasesAtt).isString() || !instanceInfo.attribute(documentAtt).isString()) {
            throw new Exception("Keyphrase attribute and document attribute " + "need to be string attributes.");
        }
        phraseFilter = new MauiPhraseFilter();
        int[] arr = new int[1];
        arr[0] = documentAtt;
        phraseFilter.setAttributeIndicesArray(arr);
        phraseFilter.setInputFormat(instanceInfo);
        if (vocabularyName.equals("none")) {
            numbersFilter = new NumbersFilter();
            numbersFilter.setInputFormat(phraseFilter.getOutputFormat());
            super.setInputFormat(numbersFilter.getOutputFormat());
        } else {
            super.setInputFormat(phraseFilter.getOutputFormat());
        }
        return false;
    }

    /**
	 * Returns the Capabilities of this filter.
	 * 
	 * @return the capabilities of this object
	 * @see Capabilities
	 */
    public Capabilities getCapabilities() {
        Capabilities result = super.getCapabilities();
        result.enableAllAttributes();
        result.enable(Capability.MISSING_VALUES);
        result.enable(Capability.NOMINAL_CLASS);
        result.enable(Capability.NO_CLASS);
        result.enableAllClasses();
        return result;
    }

    /**
	 * Input an instance for filtering. Ordinarily the instance is processed and
	 * made available for output immediately. Some filters require all instances
	 * be read before producing output.
	 * 
	 * @param instance
	 *            the input instance
	 * @return true if the filtered instance may now be collected with output().
	 * @exception Exception
	 *                if the input instance was not of the correct format or if
	 *                there was a problem with the filtering.
	 */
    @SuppressWarnings("unchecked")
    public boolean input(Instance instance) throws Exception {
        if (getInputFormat() == null) {
            throw new Exception("No input instance format defined");
        }
        if (m_NewBatch) {
            resetQueue();
            m_NewBatch = false;
        }
        if (debugMode) {
            System.err.println("-- Reading instance");
        }
        phraseFilter.input(instance);
        phraseFilter.batchFinished();
        instance = phraseFilter.output();
        if (vocabularyName.equals("none")) {
            numbersFilter.input(instance);
            numbersFilter.batchFinished();
            instance = numbersFilter.output();
        }
        if (globalDictionary == null) {
            bufferInput(instance);
            return false;
        } else {
            FastVector vector = convertInstance(instance, false);
            Enumeration<Instance> en = vector.elements();
            while (en.hasMoreElements()) {
                Instance inst = en.nextElement();
                push(inst);
            }
            return true;
        }
    }

    /**
	 * Signify that this batch of input to the filter is finished. If the filter
	 * requires all instances prior to filtering, output() may now be called to
	 * retrieve the filtered instances.
	 * 
	 * @return true if there are instances pending output
	 * @exception Exception
	 *                if no input structure has been defined
	 */
    public boolean batchFinished() throws Exception {
        if (getInputFormat() == null) {
            throw new Exception("No input instance format defined");
        }
        if (globalDictionary == null) {
            selectCandidates();
            buildGlobalDictionaries();
            buildClassifier();
            convertPendingInstances();
        }
        flushInput();
        m_NewBatch = true;
        return (numPendingOutput() != 0);
    }

    private void selectCandidates() throws Exception {
        if (debugMode) {
            System.err.println("--- Computing candidates...");
        }
        allCandidates = new HashMap<Instance, HashMap<String, Candidate>>();
        for (int i = 0; i < getInputFormat().numInstances(); i++) {
            Instance current = getInputFormat().instance(i);
            String fileName = current.stringValue(fileNameAtt);
            if (debugMode) {
                System.err.println("---- Processing document " + fileName + "...");
            }
            String documentText = current.stringValue(documentAtt);
            HashMap<String, Candidate> candidateList = getCandidates(documentText);
            if (debugMode) {
                System.err.println("---- Candidates extracted: " + candidateList.size());
            }
            allCandidates.put(current, candidateList);
        }
    }

    /**
	 * Builds the global dictionaries.
	 */
    public void buildGlobalDictionaries() throws Exception {
        if (debugMode) {
            System.err.println("--- Building global frequency dictionary");
        }
        globalDictionary = new HashMap<String, Counter>();
        for (HashMap<String, Candidate> candidates : allCandidates.values()) {
            for (String candidateName : candidates.keySet()) {
                Counter counter = globalDictionary.get(candidateName);
                if (counter == null) {
                    globalDictionary.put(candidateName, new Counter());
                } else {
                    counter.increment();
                }
            }
        }
        if (debugMode) {
            System.err.println("--- Building keyphraseness dictionary");
        }
        keyphraseDictionary = new HashMap<String, Counter>();
        for (int i = 0; i < getInputFormat().numInstances(); i++) {
            String str = getInputFormat().instance(i).stringValue(keyphrasesAtt);
            HashMap<String, Counter> hash = getGivenKeyphrases(str);
            if (hash != null) {
                for (String term : hash.keySet()) {
                    Counter documentCount = hash.get(term);
                    Counter counter = keyphraseDictionary.get(term);
                    if (counter == null) {
                        keyphraseDictionary.put(term, new Counter(documentCount.value()));
                    } else {
                        counter.increment(documentCount.value());
                    }
                }
            }
        }
        if (debugMode) {
            System.err.println("--- Statistics about global dictionaries: ");
            System.err.println("\t" + globalDictionary.size() + " terms in the global dictionary");
            System.err.println("\t" + keyphraseDictionary.size() + " terms in the keyphrase dictionary");
        }
        numDocs = getInputFormat().numInstances();
    }

    /**
	 * Builds the classifier.
	 */
    private void buildClassifier() throws Exception {
        FastVector atts = new FastVector();
        for (int i = 0; i < getInputFormat().numAttributes(); i++) {
            if (i == documentAtt) {
                atts.addElement(new Attribute("Term_frequency"));
                atts.addElement(new Attribute("IDF"));
                atts.addElement(new Attribute("TFxIDF"));
                atts.addElement(new Attribute("First_occurrence"));
                atts.addElement(new Attribute("Last_occurrence"));
                atts.addElement(new Attribute("Spread"));
                atts.addElement(new Attribute("Domain_keyphraseness"));
                atts.addElement(new Attribute("Length"));
                atts.addElement(new Attribute("Generality"));
                atts.addElement(new Attribute("Node_degree"));
                atts.addElement(new Attribute("Semantic_relatedness"));
                atts.addElement(new Attribute("Wikipedia_keyphraseness"));
                atts.addElement(new Attribute("Inverse_Wikip_frequency"));
                atts.addElement(new Attribute("Total_Wikip_keyphraseness"));
            } else if (i == keyphrasesAtt) {
                if (nominalClassValue) {
                    FastVector vals = new FastVector(2);
                    vals.addElement("False");
                    vals.addElement("True");
                    atts.addElement(new Attribute("Keyphrase?", vals));
                } else {
                    atts.addElement(new Attribute("Keyphrase?"));
                }
            }
        }
        classifierData = new Instances("ClassifierData", atts, 0);
        classifierData.setClassIndex(numFeatures);
        if (debugMode) {
            System.err.println("--- Converting instances for classifier");
        }
        for (int i = 0; i < getInputFormat().numInstances(); i++) {
            Instance current = getInputFormat().instance(i);
            String keyphrases = current.stringValue(keyphrasesAtt);
            HashMap<String, Counter> hashKeyphrases = getGivenKeyphrases(keyphrases);
            HashMap<String, Candidate> candidateList = allCandidates.get(current);
            int countPos = 0;
            int countNeg = 0;
            for (Candidate candidate : candidateList.values()) {
                if (candidate.getFrequency() < minOccurFrequency) {
                    continue;
                }
                double[] vals = computeFeatureValues(candidate, true, hashKeyphrases, candidateList);
                if (vals[vals.length - 1] == 0) {
                    countNeg++;
                } else {
                    countPos++;
                }
                Instance inst = new Instance(current.weight(), vals);
                classifierData.add(inst);
            }
            System.err.println(countPos + " positive; " + countNeg + " negative instances");
        }
        if (debugMode) {
            System.err.println("--- Building classifier");
        }
        if (nominalClassValue) {
            FilteredClassifier fclass = new FilteredClassifier();
            fclass.setClassifier(new NaiveBayesSimple());
            fclass.setFilter(new Discretize());
            classifier = fclass;
        } else {
            classifier = new Bagging();
            String optionsString = "-P 100 -S 1 -I 10 -W weka.classifiers.trees.M5P -- -U -M 7.0";
            String[] options = Utils.splitOptions(optionsString);
            classifier.setOptions(options);
        }
        classifier.buildClassifier(classifierData);
        if (debugMode) {
            System.err.println(classifier);
        }
        classifierData = new Instances(classifierData, 0);
    }

    /**
	 * Conmputes the feature values for a given phrase.
	 */
    private double[] computeFeatureValues(Candidate candidate, boolean training, HashMap<String, Counter> hashKeyphrases, HashMap<String, Candidate> candidates) {
        Article candidateArticle = candidate.getArticle();
        double[] newInst = new double[numFeatures + 1];
        String name = candidate.getName();
        String original = candidate.getBestFullForm();
        String title = candidate.getTitle();
        Counter counterGlobal = (Counter) globalDictionary.get(name);
        double globalVal = 0;
        if (counterGlobal != null) {
            globalVal = counterGlobal.value();
            if (training) {
                globalVal = globalVal - 1;
            }
        }
        double tf = candidate.getTermFrequency();
        double idf = -Math.log((globalVal + 1) / ((double) numDocs + 1));
        if (useBasicFeatures) {
            newInst[tfidfIndex] = tf * idf;
            newInst[firstOccurIndex] = candidate.getFirstOccurrence();
        }
        if (useFrequencyFeatures) {
            newInst[tfIndex] = tf;
            newInst[idfIndex] = idf;
        }
        if (usePositionsFeatures) {
            newInst[lastOccurIndex] = candidate.getLastOccurrence();
            newInst[spreadOccurIndex] = candidate.getSpread();
        }
        if (useKeyphrasenessFeature) {
            if (vocabularyName.equals("wikipedia")) {
                name = title;
            }
            Counter domainKeyphr = keyphraseDictionary.get(name);
            if ((training) && (hashKeyphrases != null) && (hashKeyphrases.containsKey(name))) {
                newInst[domainKeyphIndex] = domainKeyphr.value() - 1;
            } else {
                if (domainKeyphr != null) {
                    newInst[domainKeyphIndex] = domainKeyphr.value();
                } else {
                    newInst[domainKeyphIndex] = 0;
                }
            }
        }
        if (useLengthFeature) {
            if (original == null) {
                System.err.println("Warning! Problem with candidate " + name);
                newInst[lengthIndex] = 1.0;
            } else {
                String[] words = original.split(" ");
                newInst[lengthIndex] = (double) words.length;
            }
        }
        if (useNodeDegreeFeature) {
            int nodeDegree = 0;
            if (vocabularyName.equals("wikipedia")) {
                try {
                    for (int relatedID : candidateArticle.getLinksInIds()) {
                        if (candidates.containsKey(relatedID + "")) {
                            nodeDegree++;
                        }
                    }
                    for (int relatedID : candidateArticle.getLinksOutIds()) {
                        if (candidates.containsKey(relatedID + "")) {
                            nodeDegree++;
                        }
                    }
                } catch (SQLException e) {
                    System.err.println("Error retrieving ids for candidate " + candidate);
                }
            } else if (vocabulary != null) {
                Vector<String> relatedTerms = vocabulary.getRelated(name);
                if (relatedTerms != null) {
                    for (String relatedTerm : relatedTerms) {
                        if (candidates.get(relatedTerm) != null) nodeDegree++;
                    }
                }
            }
            newInst[nodeDegreeIndex] = (double) nodeDegree;
        }
        if (useBasicWikipediaFeatures && wikipedia != null) {
            double wikipKeyphraseness = 0;
            if (vocabularyName.equals("wikipedia")) {
                wikipKeyphraseness = candidate.getWikipKeyphraseness();
            } else {
                Anchor anchor = null;
                try {
                    anchor = new Anchor(wikipedia.getDatabase().addEscapes(original), null, wikipedia.getDatabase());
                    if (anchor != null) {
                        if (anchor.getLinkProbability() != 0) {
                            wikipKeyphraseness = anchor.getLinkProbability();
                        }
                    }
                } catch (SQLException e) {
                    System.err.println("Error retrieving the anchor for " + candidate);
                }
            }
            newInst[wikipKeyphrIndex] = wikipKeyphraseness;
            newInst[totalWikipKeyphrIndex] = candidate.getTotalWikipKeyphraseness();
        }
        if (useAllWikipediaFeatures) {
            if (candidateArticle == null) {
                try {
                    candidateArticle = wikipedia.getMostLikelyArticle(original, new CaseFolder());
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            double wikipFrequency = 0;
            double generality = 0;
            double semRelatedness = 0;
            if (candidateArticle != null) {
                try {
                    double pageCount = candidateArticle.getLinksInCount();
                    wikipFrequency = -Math.log(pageCount / 2000000);
                    generality = candidateArticle.getGenerality();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (vocabularyName.equals("wikipedia") && candidateArticle != null) {
                for (Candidate c : candidates.values()) {
                    if (!c.equals(candidate)) {
                        double relatedness = 0;
                        Article article = c.getArticle();
                        try {
                            relatedness = candidateArticle.getRelatednessTo(article);
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                        if (relatedness > 0) {
                            semRelatedness += relatedness;
                        }
                    }
                }
                semRelatedness = semRelatedness / (candidates.size() - 1);
            }
            newInst[semRelIndex] = semRelatedness;
            newInst[invWikipFreqIndex] = wikipFrequency;
            newInst[generalityIndex] = generality;
        }
        String checkManual = name;
        if (!vocabularyName.equals("none")) {
            checkManual = candidate.getTitle();
        }
        if (hashKeyphrases == null) {
            newInst[numFeatures] = 0;
        } else if (!hashKeyphrases.containsKey(checkManual)) {
            newInst[numFeatures] = 0;
        } else {
            if (nominalClassValue) {
                newInst[numFeatures] = 1;
            } else {
                double c = (double) ((Counter) hashKeyphrases.get(checkManual)).value() / numIndexers;
                newInst[numFeatures] = c;
            }
        }
        return newInst;
    }

    /**
	 * Sets output format and converts pending input instances.
	 */
    @SuppressWarnings("unchecked")
    private void convertPendingInstances() throws Exception {
        if (debugMode) {
            System.err.println("--- Converting pending instances");
        }
        FastVector atts = new FastVector();
        for (int i = 1; i < getInputFormat().numAttributes(); i++) {
            if (i == documentAtt) {
                atts.addElement(new Attribute("Candidate_name", (FastVector) null));
                atts.addElement(new Attribute("Candidate_original", (FastVector) null));
                atts.addElement(new Attribute("Term_frequency"));
                atts.addElement(new Attribute("IDF"));
                atts.addElement(new Attribute("TFxIDF"));
                atts.addElement(new Attribute("First_occurrence"));
                atts.addElement(new Attribute("Last_occurrence"));
                atts.addElement(new Attribute("Spread"));
                atts.addElement(new Attribute("Domain_keyphraseness"));
                atts.addElement(new Attribute("Length"));
                atts.addElement(new Attribute("Generality"));
                atts.addElement(new Attribute("Node_degree"));
                atts.addElement(new Attribute("Semantic_relatedness"));
                atts.addElement(new Attribute("Wikipedia_keyphraseness"));
                atts.addElement(new Attribute("Inverse_Wikip_frequency"));
                atts.addElement(new Attribute("Total_Wikip_keyphraseness"));
                atts.addElement(new Attribute("Probability"));
                atts.addElement(new Attribute("Rank"));
            } else if (i == keyphrasesAtt) {
                if (nominalClassValue) {
                    FastVector vals = new FastVector(2);
                    vals.addElement("False");
                    vals.addElement("True");
                    atts.addElement(new Attribute("Keyphrase?", vals));
                } else {
                    atts.addElement(new Attribute("Keyphrase?"));
                }
            } else {
                atts.addElement(getInputFormat().attribute(i));
            }
        }
        Instances outFormat = new Instances("mauidata", atts, 0);
        setOutputFormat(outFormat);
        for (int i = 0; i < getInputFormat().numInstances(); i++) {
            Instance current = getInputFormat().instance(i);
            FastVector vector = convertInstance(current, true);
            Enumeration en = vector.elements();
            while (en.hasMoreElements()) {
                Instance inst = (Instance) en.nextElement();
                push(inst);
            }
        }
    }

    /**
	 * Converts an instance.
	 */
    private FastVector convertInstance(Instance instance, boolean training) throws Exception {
        FastVector vector = new FastVector();
        String fileName = instance.stringValue(fileNameAtt);
        if (debugMode) {
            System.err.println("-- Converting instance for document " + fileName);
        }
        HashMap<String, Counter> hashKeyphrases = null;
        if (!instance.isMissing(keyphrasesAtt)) {
            String keyphrases = instance.stringValue(keyphrasesAtt);
            hashKeyphrases = getGivenKeyphrases(keyphrases);
        }
        String documentText = instance.stringValue(documentAtt);
        HashMap<String, Candidate> candidateList;
        if (allCandidates != null && allCandidates.containsKey(instance)) {
            candidateList = allCandidates.get(instance);
        } else {
            candidateList = getCandidates(documentText);
        }
        System.err.println(candidateList.size() + " candidates ");
        int tfidfAttIndex = documentAtt + 2;
        int distAttIndex = documentAtt + 3;
        int probsAttIndex = documentAtt + numFeatures;
        int countPos = 0;
        int countNeg = 0;
        for (Candidate candidate : candidateList.values()) {
            if (candidate.getFrequency() < minOccurFrequency) {
                continue;
            }
            String name = candidate.getName();
            String orig = candidate.getBestFullForm();
            if (!vocabularyName.equals("none")) {
                orig = candidate.getTitle();
            }
            double[] vals = computeFeatureValues(candidate, training, hashKeyphrases, candidateList);
            Instance inst = new Instance(instance.weight(), vals);
            inst.setDataset(classifierData);
            double[] probs = classifier.distributionForInstance(inst);
            double prob = probs[0];
            if (nominalClassValue) {
                prob = probs[1];
            }
            double[] newInst = new double[instance.numAttributes() + numFeatures + 2];
            int pos = 0;
            for (int i = 1; i < instance.numAttributes(); i++) {
                if (i == documentAtt) {
                    int index = outputFormatPeek().attribute(pos).addStringValue(name);
                    newInst[pos++] = index;
                    if (orig != null) {
                        index = outputFormatPeek().attribute(pos).addStringValue(orig);
                    } else {
                        index = outputFormatPeek().attribute(pos).addStringValue(name);
                    }
                    newInst[pos++] = index;
                    newInst[pos++] = inst.value(tfIndex);
                    newInst[pos++] = inst.value(idfIndex);
                    newInst[pos++] = inst.value(tfidfIndex);
                    newInst[pos++] = inst.value(firstOccurIndex);
                    newInst[pos++] = inst.value(lastOccurIndex);
                    newInst[pos++] = inst.value(spreadOccurIndex);
                    newInst[pos++] = inst.value(domainKeyphIndex);
                    newInst[pos++] = inst.value(lengthIndex);
                    newInst[pos++] = inst.value(generalityIndex);
                    newInst[pos++] = inst.value(nodeDegreeIndex);
                    newInst[pos++] = inst.value(semRelIndex);
                    newInst[pos++] = inst.value(wikipKeyphrIndex);
                    newInst[pos++] = inst.value(invWikipFreqIndex);
                    newInst[pos++] = inst.value(totalWikipKeyphrIndex);
                    probsAttIndex = pos;
                    newInst[pos++] = prob;
                    newInst[pos++] = Instance.missingValue();
                } else if (i == keyphrasesAtt) {
                    newInst[pos++] = inst.classValue();
                } else {
                    newInst[pos++] = instance.value(i);
                }
            }
            Instance ins = new Instance(instance.weight(), newInst);
            ins.setDataset(outputFormatPeek());
            vector.addElement(ins);
            if (inst.classValue() == 0) {
                countNeg++;
            } else {
                countPos++;
            }
        }
        System.err.println(countPos + " positive; " + countNeg + " negative instances");
        double[] vals = new double[vector.size()];
        for (int i = 0; i < vals.length; i++) {
            vals[i] = ((Instance) vector.elementAt(i)).value(distAttIndex);
        }
        FastVector newVector = new FastVector(vector.size());
        int[] sortedIndices = Utils.stableSort(vals);
        for (int i = 0; i < vals.length; i++) {
            newVector.addElement(vector.elementAt(sortedIndices[i]));
        }
        vector = newVector;
        for (int i = 0; i < vals.length; i++) {
            vals[i] = -((Instance) vector.elementAt(i)).value(tfidfAttIndex);
        }
        newVector = new FastVector(vector.size());
        sortedIndices = Utils.stableSort(vals);
        for (int i = 0; i < vals.length; i++) {
            newVector.addElement(vector.elementAt(sortedIndices[i]));
        }
        vector = newVector;
        for (int i = 0; i < vals.length; i++) {
            vals[i] = 1 - ((Instance) vector.elementAt(i)).value(probsAttIndex);
        }
        newVector = new FastVector(vector.size());
        sortedIndices = Utils.stableSort(vals);
        for (int i = 0; i < vals.length; i++) {
            newVector.addElement(vector.elementAt(sortedIndices[i]));
        }
        vector = newVector;
        int rank = 1;
        for (int i = 0; i < vals.length; i++) {
            Instance currentInstance = (Instance) vector.elementAt(i);
            if (Utils.grOrEq(vals[i], 1.0)) {
                currentInstance.setValue(probsAttIndex + 1, Integer.MAX_VALUE);
                continue;
            }
            int startInd = i;
            while (startInd < vals.length) {
                Instance inst = (Instance) vector.elementAt(startInd);
                if ((inst.value(tfidfAttIndex) != currentInstance.value(tfidfAttIndex)) || (inst.value(probsAttIndex) != currentInstance.value(probsAttIndex)) || (inst.value(distAttIndex) != currentInstance.value(distAttIndex))) {
                    break;
                }
                startInd++;
            }
            currentInstance.setValue(probsAttIndex + 1, rank++);
        }
        return vector;
    }

    /**
	 * Expects an empty hashtable. Fills the hashtable with the candidate
	 * keyphrases Stores the position, the number of occurences, and the most
	 * commonly occurring orgininal version of each candidate in the Candidate
	 * object.
	 * 
	 * Returns the total number of words in the document.
	 * 
	 * @throws Exception
	 */
    public HashMap<String, Candidate> getCandidates(String text) {
        if (debugMode) {
            System.err.println("---- Extracting candidates...");
        }
        HashMap<String, Candidate> candidatesTable = new HashMap<String, Candidate>();
        int countCandidates = 0;
        String[] buffer = new String[maxPhraseLength];
        StringTokenizer tok = new StringTokenizer(text, "\n");
        int pos = 0;
        int totalFrequency = 0;
        int firstWord = 0;
        while (tok.hasMoreTokens()) {
            String token = tok.nextToken();
            int numSeen = 0;
            StringTokenizer wordTok = new StringTokenizer(token, " ");
            while (wordTok.hasMoreTokens()) {
                pos++;
                String word = wordTok.nextToken();
                for (int i = 0; i < maxPhraseLength - 1; i++) {
                    buffer[i] = buffer[i + 1];
                }
                buffer[maxPhraseLength - 1] = word;
                numSeen++;
                if (numSeen > maxPhraseLength) {
                    numSeen = maxPhraseLength;
                }
                if (vocabularyName.equals("none")) {
                    if (stopwords.isStopword(buffer[maxPhraseLength - 1])) {
                        continue;
                    }
                }
                StringBuffer phraseBuffer = new StringBuffer();
                for (int i = 1; i <= numSeen; i++) {
                    if (i > 1) {
                        phraseBuffer.insert(0, ' ');
                    }
                    phraseBuffer.insert(0, buffer[maxPhraseLength - i]);
                    if (vocabularyName.equals("none")) {
                        if ((i > 1) && (stopwords.isStopword(buffer[maxPhraseLength - i]))) {
                            continue;
                        }
                    }
                    if (i >= minPhraseLength) {
                        String form = phraseBuffer.toString();
                        Vector<String> candidateNames = new Vector<String>();
                        if (vocabularyName.equals("none")) {
                            String phrase = pseudoPhrase(form);
                            if (phrase != null) candidateNames.add(phrase);
                            totalFrequency++;
                        } else if (vocabularyName.equals("wikipedia")) {
                            String patternStr = "[0-9\\s]+";
                            Pattern pattern = Pattern.compile(patternStr);
                            Matcher matcher = pattern.matcher(form);
                            boolean matchFound = matcher.matches();
                            if (matchFound == false) {
                                candidateNames.add(form);
                            }
                        } else {
                            for (String sense : vocabulary.getSenses(form)) {
                                candidateNames.add(sense);
                            }
                        }
                        if (!candidateNames.isEmpty()) {
                            for (String name : candidateNames) {
                                Candidate candidate = candidatesTable.get(name);
                                if (candidate == null) {
                                    if (vocabularyName.equals("wikipedia")) {
                                        Anchor anchor;
                                        try {
                                            anchor = new Anchor(form, textProcessor, wikipedia.getDatabase());
                                            double probability = anchor.getLinkProbability();
                                            if (probability >= minKeyphraseness) {
                                                countCandidates++;
                                                totalFrequency++;
                                                firstWord = pos - i;
                                                candidate = new Candidate(name, form, firstWord, anchor, probability);
                                            }
                                        } catch (SQLException e) {
                                            System.err.println("Error adding ngram " + form);
                                            e.printStackTrace();
                                        }
                                    } else {
                                        firstWord = pos - i;
                                        candidate = new Candidate(name, form, firstWord);
                                        totalFrequency++;
                                        if (!vocabularyName.equals("none")) {
                                            candidate.setTitle(vocabulary.getTerm(name));
                                        }
                                    }
                                } else {
                                    firstWord = pos - i;
                                    candidate.recordOccurrence(form, firstWord);
                                    countCandidates++;
                                    totalFrequency++;
                                }
                                if (candidate != null) {
                                    candidatesTable.put(name, candidate);
                                }
                            }
                        }
                    }
                }
            }
        }
        for (Candidate candidate : candidatesTable.values()) {
            candidate.normalize(totalFrequency, pos);
        }
        if (vocabularyName.equals("wikipedia")) {
            candidatesTable = disambiguateCandidates(candidatesTable.values());
        }
        return candidatesTable;
    }

    /**
	 * Given a set of candidate terms extracted from the text computes, which
	 * one of these are the least ambiguous ones Creates a vector of Wikipedia
	 * articles representing their senses.
	 * 
	 * @param candidates
	 * @return vector of context articles
	 */
    private Vector<Article> collectContextTerms(Collection<Candidate> candidates) {
        Vector<Article> context = new Vector<Article>();
        SortedVector<Article> bestCandidateSenses = new SortedVector<Article>();
        for (Candidate candidate : candidates) {
            Anchor anchor = candidate.getAnchor();
            try {
                if (context.size() >= maxContextSize) {
                    break;
                }
                if (anchor.getSenses().isEmpty()) {
                    continue;
                }
                Sense bestSense = anchor.getSenses().first();
                if (anchor.getSenses().size() == 1 && anchor.getLinkProbability() > 0.5) {
                    context.add(bestSense);
                    continue;
                }
                double senseProbability = bestSense.getProbability();
                double linkProbability = anchor.getLinkProbability();
                if (senseProbability >= 0.9 && linkProbability > 0.5) {
                    bestSense.setWeight(senseProbability);
                    bestCandidateSenses.add(bestSense, false);
                }
            } catch (SQLException e) {
                System.err.println("Error computing senses for " + anchor);
                e.printStackTrace();
            }
        }
        if (context.size() < maxContextSize) {
            for (int i = 0; i < bestCandidateSenses.size() && context.size() < maxContextSize; i++) {
                Article sense = bestCandidateSenses.elementAt(i);
                context.add(sense);
            }
        }
        return context;
    }

    /**
	 * Given a collection of candidate terms, each term is disambiguated to its
	 * most likely meaning, given the mappings for other terms (context)
	 * 
	 * @param candidates
	 * @return hashmap of Wikipedia articles candidates
	 */
    private HashMap<String, Candidate> disambiguateCandidates(Collection<Candidate> candidates) {
        if (debugMode) {
            System.err.println("---- Disambiguating candidates...");
        }
        HashMap<String, Candidate> disambiguatedTopics = new HashMap<String, Candidate>();
        Vector<Article> context = collectContextTerms(candidates);
        String id;
        for (Candidate candidate : candidates) {
            try {
                for (Anchor.Sense sense : candidate.getAnchor().getSenses()) {
                    Candidate candidateCopy = candidate.getCopy();
                    double senseProbablity = sense.getProbability();
                    if (senseProbablity < minSenseProbability) break;
                    if (senseProbablity == 1.0) {
                        id = sense.getId() + "";
                        candidateCopy.setName(id);
                        candidateCopy.setTitle(sense.getTitle());
                        candidateCopy.setArticle(sense);
                        if (disambiguatedTopics.containsKey(id)) {
                            Candidate previousCandidate = disambiguatedTopics.get(id);
                            candidateCopy.mergeWith(previousCandidate);
                        }
                        disambiguatedTopics.put(id, candidateCopy);
                    } else {
                        if (senseProbablity == 0) {
                            senseProbablity = minSenseProbability;
                        }
                        double semanticRelatedness = 0;
                        try {
                            semanticRelatedness = getRelatednessTo(sense, context);
                        } catch (Exception e) {
                            System.err.println("Error computing semantic relatedness for the sense " + sense);
                            e.printStackTrace();
                        }
                        double disambiguationScore = senseProbablity * semanticRelatedness;
                        if (disambiguationScore > 0.01) {
                            id = sense.getId() + "";
                            candidateCopy.setName(id);
                            candidateCopy.setTitle(sense.getTitle());
                            candidateCopy.setArticle(sense);
                            if (disambiguatedTopics.containsKey(id)) {
                                Candidate previousCandidate = disambiguatedTopics.get(id);
                                candidateCopy.mergeWith(previousCandidate);
                            }
                            disambiguatedTopics.put(id, candidateCopy);
                        }
                    }
                }
            } catch (SQLException e) {
                System.err.println("Error disambiguating candidate " + candidate);
                e.printStackTrace();
            }
        }
        return disambiguatedTopics;
    }

    /**
	 * Given a Wikipedia article and a set of context article collected from the
	 * same text, this method computes the article's average semantic
	 * relatedness to the context
	 * 
	 * @param article
	 * @param contextArticles
	 * @return double -- semantic relatedness
	 */
    private double getRelatednessTo(Article article, Vector<Article> contextArticles) {
        double totalRelatedness = 0;
        double currentRelatedness = 0;
        double totalComparisons = 0;
        for (Article contextArticle : contextArticles) {
            if (article != contextArticle) {
                try {
                    currentRelatedness = article.getRelatednessTo(contextArticle);
                } catch (Exception e) {
                    System.err.println("Error computing semantic relatedness for " + article + " and " + contextArticle);
                    e.printStackTrace();
                }
                totalRelatedness += currentRelatedness;
                totalComparisons++;
            }
        }
        return totalRelatedness / totalComparisons;
    }

    /**
	 * Collects all the topics assigned manually and puts them into the
	 * hashtable. Also stores the counts for each topic, if they are available
	 */
    private HashMap<String, Counter> getGivenKeyphrases(String keyphraseListings) {
        HashMap<String, Counter> keyphrases = new HashMap<String, Counter>();
        String keyphrase, listing;
        int tab, frequency;
        StringTokenizer tok = new StringTokenizer(keyphraseListings, "\n");
        while (tok.hasMoreTokens()) {
            listing = tok.nextToken();
            listing = listing.trim();
            tab = listing.indexOf("\t");
            if (tab != -1) {
                keyphrase = listing.substring(0, tab);
                frequency = Integer.parseInt(listing.substring(tab + 1));
            } else {
                keyphrase = listing;
                frequency = 1;
            }
            if (vocabularyName.equals("none")) {
                keyphrase = pseudoPhrase(keyphrase);
                Counter counter = keyphrases.get(keyphrase);
                if (counter == null) {
                    keyphrases.put(keyphrase, new Counter(frequency));
                } else {
                    counter.increment(frequency);
                }
            } else if (vocabularyName.equals("wikipedia")) {
                int colonIndex = keyphrase.indexOf(":");
                if (colonIndex != -1) {
                    keyphrase = keyphrase.substring(colonIndex + 2);
                }
                Counter counter = keyphrases.get(keyphrase);
                if (counter == null) {
                    keyphrases.put(keyphrase, new Counter(frequency));
                } else {
                    counter.increment(frequency);
                }
            } else {
                for (String id : vocabulary.getSenses(keyphrase)) {
                    keyphrase = vocabulary.getTerm(id);
                    Counter counter = keyphrases.get(keyphrase);
                    if (counter == null) {
                        keyphrases.put(keyphrase, new Counter(frequency));
                    } else {
                        counter.increment(frequency);
                    }
                }
            }
        }
        if (keyphrases.size() == 0) {
            System.err.println("Warning! This documents does not contain valid keyphrases");
            return null;
        } else {
            totalCorrect = keyphrases.size();
            if (debugMode) {
                System.err.println("---- Manually assigned keyphrases: " + totalCorrect);
            }
            return keyphrases;
        }
    }

    /**
	 * Generates a normalized preudo phrase from a string. A pseudo phrase is a
	 * version of a phrase that only contains non-stopwords, which are stemmed
	 * and sorted into alphabetical order.
	 */
    public String pseudoPhrase(String str) {
        String result = "";
        str = str.toLowerCase();
        str = str.replace('-', ' ');
        str = str.replace('&', ' ');
        str = str.replaceAll("\\*", "");
        str = str.replaceAll("\\, ", " ");
        str = str.replaceAll("\\. ", " ");
        str = str.replaceAll("\\:", "");
        str = str.trim();
        String[] words = str.split(" ");
        Arrays.sort(words);
        for (String word : words) {
            if (stopwords.isStopword(word)) continue;
            int apostr = word.indexOf('\'');
            if (apostr != -1) word = word.substring(0, apostr);
            word = stemmer.stem(word);
            result += word + " ";
        }
        result = result.trim();
        if (result.equals("")) {
            return null;
        } else {
            return result;
        }
    }

    /**
	 * Main method.
	 */
    public static void main(String[] argv) {
        System.err.println("Use MauiModelBuilder or MauiTopicExtractor!");
    }
}
