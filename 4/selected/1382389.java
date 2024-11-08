package apollo.dataadapter.chadoxml;

import java.io.File;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Stack;
import java.util.Vector;
import javax.swing.JOptionPane;
import org.apache.log4j.*;
import org.bdgp.io.DataAdapterUI;
import org.bdgp.io.IOOperation;
import org.bdgp.util.ProgressEvent;
import org.bdgp.xml.XMLElement;
import apollo.config.Config;
import apollo.main.Version;
import apollo.config.PropertyScheme;
import apollo.config.FeatureProperty;
import apollo.config.ApolloNameAdapterI;
import apollo.config.GmodNameAdapter;
import apollo.dataadapter.AbstractApolloAdapter;
import apollo.dataadapter.ApolloDataAdapterI;
import apollo.dataadapter.ApolloAdapterException;
import apollo.dataadapter.DataInput;
import apollo.dataadapter.DataInputType;
import apollo.dataadapter.StateInformation;
import apollo.dataadapter.DataInputType.UnknownTypeException;
import apollo.dataadapter.chadoxml.ChadoXmlUtils;
import apollo.dataadapter.gamexml.GAMEAdapter;
import apollo.dataadapter.gamexml.XMLParser;
import apollo.dataadapter.gamexml.TransactionXMLAdapter;
import apollo.datamodel.*;
import apollo.datamodel.seq.GAMESequence;
import apollo.editor.TransactionManager;
import apollo.util.FastaHeader;
import apollo.util.HTMLUtil;
import apollo.util.IOUtil;
import apollo.main.LoadUtil;

/** 
 * A FlyBase-specific reader for Chado XML files. 
 * Currently handles only unmacroized Chado XML. */
public class ChadoXmlAdapter extends AbstractApolloAdapter {

    protected static final Logger logger = LogManager.getLogger(ChadoXmlAdapter.class);

    static String originalFilename = null;

    /** For labeling flat fields (which are stored as properties) so they
   *  can be written out properly.
   *  (Should this go somewhere global so that other adapters can see it?) */
    public static String FIELD_LABEL = "field:";

    /** Used by org.bdgp.io.DataAdapter */
    private static final IOOperation[] supportedOperations = { ApolloDataAdapterI.OP_READ_DATA, ApolloDataAdapterI.OP_WRITE_DATA, ApolloDataAdapterI.OP_APPEND_DATA };

    private ChadoXmlAdapterGUI gui;

    String filename = null;

    /** These need to be global to allow easy layering of new data */
    StrandedFeatureSetI analyses = null;

    Hashtable all_analyses = null;

    /** Every result has a block for the (genomic) query, including a dbxref--only
   *  save this the first time. */
    boolean savedGenomicDbxref = false;

    boolean warnedAboutFeatureCvterm = false;

    /** This indicates no GUI so certain non-apollo, non-gui apps can use this
   *  adapter. (But who sets this?) */
    boolean NO_GUI = false;

    boolean genomicRegionSet = false;

    /** Must have empty constructor to work with org.bdgp.io.DataAdapterRegistry
      instance creation from config string */
    public ChadoXmlAdapter() {
        setName("Chado XML file (FlyBase v1.0, no macros)");
    }

    /** From org.bdgp.io.VisualDataAdapter interface */
    public DataAdapterUI getUI(IOOperation op) {
        if (gui == null) gui = new ChadoXmlAdapterGUI();
        gui.setIOOperation(op);
        return gui;
    }

    /** From org.bdgp.io.DataAdapter interface */
    public IOOperation[] getSupportedOperations() {
        return supportedOperations;
    }

    /** Used by GenericFileAdapterGUI */
    public String getType() {
        return getName();
    }

    public void setInput(String inputfile) {
        this.filename = inputfile;
    }

    public String getInput() {
        return filename;
    }

    public void setDataInput(DataInput dataInput) {
        super.setDataInput(dataInput);
        setInput(dataInput.getInputString());
    }

    /** Save original file name so that if we save the data we can access the
   *  header info from the original file; and also so we can save the file name
   *  as a comment. */
    public void setOriginalFilename(String file) {
        logger.debug("setOriginalFilename: setting original filename = " + file);
        this.originalFilename = file;
    }

    /** Open the requested file as a stream; check that file really does contain
   *  ChadoXML before returning stream. */
    private InputStream chadoXmlInputStream(String filename) throws ApolloAdapterException {
        InputStream stream = null;
        logger.info("locating Chado XML datasource...");
        if (filename.startsWith("http")) {
            URL url;
            try {
                logger.debug("chadoXmlInputStream: type is URL");
                url = new URL(filename);
                stream = apollo.util.IOUtil.getStreamFromUrl(url, "URL " + url + " not found");
                setOriginalFilename(filename);
                return stream;
            } catch (Exception e) {
                stream = null;
                throw new ApolloAdapterException("Error: could not open ChadoXML URL " + filename + " for reading.");
            }
        }
        String path = apollo.util.IOUtil.findFile(filename, false);
        try {
            stream = new FileInputStream(path);
            setOriginalFilename(path);
        } catch (Exception e) {
            stream = null;
            throw new ApolloAdapterException("could not open ChadoXML file " + filename + " for reading.");
        }
        BufferedReader in;
        try {
            in = new BufferedReader(new FileReader(path));
        } catch (Exception e) {
            stream = null;
            throw new ApolloAdapterException("Error: could not open ChadoXML file " + path + " for reading.");
        }
        if (!appearsToBeChadoXML(filename, in)) {
            throw new ApolloAdapterException("File " + filename + "\ndoes not appear to contain chadoXML--couldn't find <chado> line.\n");
        }
        return stream;
    }

    /** This is the main method for reading the data.  The filename should already
   *  have been set. */
    public CurationSet getCurationSet() throws ApolloAdapterException {
        try {
            fireProgressEvent(new ProgressEvent(this, new Double(5.0), "Finding data..."));
            InputStream xml_stream;
            xml_stream = chadoXmlInputStream(getInput());
            return (getCurationSetFromInputStream(xml_stream));
        } catch (ApolloAdapterException dae) {
            logger.error("Error while parsing " + getInput(), dae);
            throw dae;
        } catch (Exception ex2) {
            logger.error("Error while parsing " + getInput(), ex2);
            throw new ApolloAdapterException(ex2.getMessage());
        }
    }

    public CurationSet getCurationSetFromInputStream(InputStream xml_stream) throws ApolloAdapterException {
        genomicRegionSet = false;
        CurationSet curation = null;
        try {
            BufferedInputStream bis = new BufferedInputStream(xml_stream);
            if (!NO_GUI) super.clearOldData();
            fireProgressEvent(new ProgressEvent(this, new Double(10.0), "Reading XML..."));
            XMLParser parser = new XMLParser();
            XMLElement rootElement = parser.readXML(bis);
            if (rootElement == null) {
                String msg = "XML input stream was empty--nothing loaded.";
                logger.error(msg);
                throw new ApolloAdapterException(msg);
            }
            xml_stream.close();
            fireProgressEvent(new ProgressEvent(this, new Double(40.0), "Populating data models..."));
            curation = new CurationSet();
            populateDataModels(rootElement, curation);
            logger.info("Completed XML parse of " + curation.getName());
            parser.clean();
            rootElement = null;
            fireProgressEvent(new ProgressEvent(this, new Double(90.0), "Reading transaction file..."));
            TransactionXMLAdapter.loadTransactions(getInput(), curation, getCurationState());
            fireProgressEvent(new ProgressEvent(this, new Double(95.0), "Drawing..."));
        } catch (ApolloAdapterException dae) {
            logger.error("Error while parsing " + getInput(), dae);
            throw dae;
        } catch (Exception ex2) {
            logger.error("Error while parsing " + getInput(), ex2);
            throw new ApolloAdapterException(ex2.getMessage());
        }
        curation.setInputFilename(originalFilename);
        return curation;
    }

    /** Like getCurationSet--used for layering new data. */
    public Boolean addToCurationSet() throws ApolloAdapterException {
        boolean okay = false;
        if (curation_set == null) {
            String message = "Can't layer ChadoXML data on top of non-ChadoXML data.";
            logger.error(message);
            JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.WARNING_MESSAGE);
            return new Boolean(false);
        }
        try {
            fireProgressEvent(new ProgressEvent(this, new Double(5.0), "Finding new data..."));
            InputStream xml_stream;
            xml_stream = chadoXmlInputStream(getInput());
            BufferedInputStream bis = new BufferedInputStream(xml_stream);
            fireProgressEvent(new ProgressEvent(this, new Double(10.0), "Reading XML..."));
            XMLParser parser = new XMLParser();
            XMLElement rootElement = parser.readXML(bis);
            if (rootElement == null) {
                String msg = "XML input stream was empty--nothing loaded.";
                logger.warn(msg);
                throw new ApolloAdapterException(msg);
            }
            xml_stream.close();
            fireProgressEvent(new ProgressEvent(this, new Double(40.0), "Populating data models..."));
            populateDataModels(rootElement, curation_set);
            logger.info("Completed XML parse of file " + getInput() + " for region " + curation_set.getName());
            parser.clean();
            rootElement = null;
            fireProgressEvent(new ProgressEvent(this, new Double(90.0), "Reading transaction file..."));
            TransactionXMLAdapter.loadTransactions(getInput(), curation_set, getCurationState());
            fireProgressEvent(new ProgressEvent(this, new Double(95.0), "Drawing..."));
            okay = true;
        } catch (ApolloAdapterException dae) {
            logger.error("Error while parsing " + getInput(), dae);
            throw dae;
        } catch (Exception ex2) {
            logger.error("Error while parsing " + getInput(), ex2);
            throw new ApolloAdapterException(ex2.getMessage());
        }
        return new Boolean(okay);
    }

    /** Starting with the root XML element, go through the parse tree and
   *  populate the Apollo datamodels for annotations and results. */
    private void populateDataModels(XMLElement rootElement, CurationSet curation) throws ApolloAdapterException {
        String seq_id = "";
        int start = -1;
        int end = -1;
        String dna = "";
        String arm = "";
        StrandedFeatureSetI annotations = curation.getAnnots();
        if (annotations == null) {
            annotations = new StrandedFeatureSet(new FeatureSet(), new FeatureSet());
            annotations.setName("Annotations");
            annotations.setFeatureType("Annotation");
            curation.setAnnots(annotations);
        }
        StrandedFeatureSetI results = curation.getResults();
        if (results == null) {
            results = new StrandedFeatureSet(new FeatureSet(), new FeatureSet());
            analyses = new StrandedFeatureSet(new FeatureSet(), new FeatureSet());
            all_analyses = new Hashtable();
        }
        int total = rootElement.numChildren();
        int count = 0;
        while (rootElement.numChildren() > 0) {
            try {
                ++count;
                fireProgressEvent(new ProgressEvent(this, new Double(40.0 + ((double) count / (double) total) * 55.0), "Parsing XML element #" + count));
                XMLElement element = rootElement.popChild();
                if (element.getAttribute("name") != null) {
                    try {
                        if (element.getAttribute("name").equalsIgnoreCase("title")) {
                            seq_id = element.getCharData();
                        } else if (element.getAttribute("name").equalsIgnoreCase("arm")) {
                            arm = element.getCharData();
                            if (seq_id.equals("")) seq_id = arm;
                            if (arm != null && !arm.equals("")) curation.setChromosome(arm);
                        } else if (element.getAttribute("name").equalsIgnoreCase("fmin")) {
                            try {
                                start = Integer.parseInt(element.getCharData());
                            } catch (Exception e) {
                                logger.error("Couldn't parse integer from fmin " + element.getCharData() + " in XML element " + count, e);
                            }
                        } else if (element.getAttribute("name").equalsIgnoreCase("fmax")) {
                            try {
                                end = Integer.parseInt(element.getCharData());
                            } catch (Exception e) {
                                logger.error("Couldn't parse integer from fmax " + element.getCharData() + " in XML element " + count, e);
                            }
                        } else if (element.getAttribute("name").equalsIgnoreCase("residues")) {
                            dna = element.getCharData();
                        }
                    } catch (Exception e) {
                        logger.warn("error parsing map element " + ChadoXmlUtils.printXMLElement(element), e);
                    }
                } else if (element.getType().equalsIgnoreCase("feature")) {
                    if (!genomicRegionSet) createGenomicRegion(start, end, seq_id, dna, curation, annotations, results);
                    FeatureSetI feature = processFeature(element, curation);
                    logger.debug("Processed feature " + feature.getName());
                    if (feature instanceof AnnotatedFeatureI) {
                        annotations.addFeature(feature);
                    }
                } else if (element.getType().equalsIgnoreCase("cv") || element.getType().equalsIgnoreCase("cvterm") || element.getAttribute("lookup") != null) {
                    String error = "This file includes macros!  I can't deal with macros!\nOffending element: " + ChadoXmlUtils.printXMLElement(element);
                    logger.error(error);
                    throw new ApolloAdapterException(error);
                } else logger.error("Unknown top-level element " + element.getType());
            } catch (ApolloAdapterException dae) {
                throw dae;
            } catch (Exception ex2) {
                logger.error("Caught exception while parsing XML", ex2);
                throw new ApolloAdapterException(ex2.getMessage());
            }
        }
        if (curation.getStart() == curation.getEnd()) {
            String error = "Error: input from " + originalFilename + "\nis not valid Chado XML--couldn't find _appdata fields.\nProbably the input was not actually Chado XML but was actually some other format.\n";
            logger.error(error);
            throw new ApolloAdapterException(error);
        }
        curation.setResults(analyses);
    }

    /** Sets up a refsequence for curation set (and also sets the genomic range).
   *  Right now, this relies on there being _appdata records at the beginning of
   *  the chadoXML file:
   * <chado  dumpspec="dumpspec_apollo.xml" date="Wes Sept 24 12:45:36 EDT 2003">
   *    <_appdata  name="title">FBgn0000826</_appdata>
   *    <_appdata  name="arm">X</_appdata>
   *    <_appdata  name="fmin">1212808</_appdata>
   *    <_appdata  name="fmax">1214934</_appdata>
   *    <_appdata  name="residues">GAGAAGCAACACTTCAGTCTGACCAAAATCCTCAAGA...</_appdata>
   *
   * This is an idiosyncratic FlyBase way of representing the region info.
   * It would be nice if there were a more standardized way of doing this, or
   * if Apollo could at least estimate the fmin/fmax by looking at the features
   * and finding the lowest/highest endpoints.
   * Unfortunately, the genomic region info currently needs to be set up BEFORE we read
   * the features. */
    private void createGenomicRegion(int start, int end, String seq_id, String dna, CurationSet curation, StrandedFeatureSetI annotations, StrandedFeatureSetI results) {
        GAMESequence curated_seq = new GAMESequence(seq_id, Config.getController(), dna);
        curated_seq.setLength(Math.abs(end - start) + 1);
        curated_seq.setName(seq_id);
        curated_seq.setAccessionNo(seq_id);
        curated_seq.setResidueType(SequenceI.DNA);
        curation.setRefSequence(curated_seq);
        curation.addSequence(curated_seq);
        annotations.setRefSequence(curated_seq);
        results.setRefSequence(curated_seq);
        ++start;
        curation.setLow(start);
        curation.setHigh(end);
        int strand = (start < end) ? 1 : -1;
        curation.setStrand(strand);
        curation.setName(seq_id);
        genomicRegionSet = true;
    }

    /** Returns a feature that is either an annotation or a result */
    private FeatureSetI processFeature(XMLElement xml, CurationSet curation) throws ApolloAdapterException {
        Vector elements = xml.getChildren();
        boolean isResult = true;
        String type = "";
        String name = "";
        String uniquename = "";
        for (int i = 0; i < elements.size(); i++) {
            XMLElement child = (XMLElement) elements.elementAt(i);
            if (child.getType().equalsIgnoreCase("is_analysis")) {
                if (child.getCharData().equals("0")) isResult = false; else isResult = true;
            } else if (child.getType().equalsIgnoreCase("type_id")) {
                type = getDataType(child);
            } else if (child.getType().equalsIgnoreCase("name")) name = child.getCharData(); else if (child.getType().equalsIgnoreCase("uniquename")) uniquename = child.getCharData();
        }
        if (isResult) {
            FeatureSetI result = new FeatureSet();
            result.addProperty("is_analysis", "1");
            result.setId(uniquename);
            result.setName(name);
            processResult(xml, result, curation);
            logger.debug("After processResult, id is " + result.getId() + " for result " + result.getName());
            logger.debug("processFeature: returning result " + result);
            return result;
        } else {
            AnnotatedFeature annot = new AnnotatedFeature();
            annot.setId(uniquename);
            annot.setName(name);
            logger.debug("Before processAnnot, id = " + uniquename + ", name = " + name);
            annot.setFeatureType(type);
            annot.setTopLevelType(type);
            processAnnot(xml, annot, curation);
            if (logger.isDebugEnabled()) {
                logger.debug("processFeature: returning annotation " + annot + " (type " + type + "): id = " + annot.getId() + ", name = " + annot.getName());
            }
            return annot;
        }
    }

    /** Read in and return a result.
   *  xml is <feature> node. */
    private FeatureSetI processResult(XMLElement xml, FeatureSetI result, CurationSet curation) {
        Vector elements = xml.getChildren();
        for (int i = 0; i < elements.size(); i++) {
            XMLElement child = (XMLElement) elements.elementAt(i);
            if (child.getType().equalsIgnoreCase("featureloc")) {
                logger.warn("How weird--a featureloc right under a result for " + ChadoXmlUtils.printXMLElement(xml));
                return null;
            } else if (child.getType().equalsIgnoreCase("name")) {
                result.setName(child.getCharData());
            } else if (child.getType().equalsIgnoreCase("uniquename")) {
                result.setId(child.getCharData());
            } else if (child.getType().equalsIgnoreCase("analysisfeature")) {
                handleAnalysisType(child, result);
            } else if (child.getType().equalsIgnoreCase("feature_relationship")) {
                SeqFeatureI seqFeat = getSeqFeature(child, result, curation);
                logger.debug("result name = " + result.getName() + ", seq feat name = " + seqFeat.getName());
                result.addFeature(seqFeat);
                result.setStrand(seqFeat.getStrand());
            } else {
                if (!child.getCharData().equals("")) {
                    addField(result, child);
                }
            }
        }
        addResultToAnalysis(result);
        return result;
    }

    /** Read in info for annotation (skeleton is already set up). */
    private AnnotatedFeatureI processAnnot(XMLElement xml, AnnotatedFeature annot, CurationSet curation) throws ApolloAdapterException {
        boolean gotFeatureloc = false;
        annot.addProperty("is_analysis", "0");
        Vector elements = xml.getChildren();
        for (int i = 0; i < elements.size(); i++) {
            XMLElement child = (XMLElement) elements.elementAt(i);
            if (child.getType().equalsIgnoreCase("dbxref_id")) {
                DbXref xref = getDbxref(child, annot, true);
                if (xref != null) {
                    String dbxref = xref.getIdValue();
                    if (annot.getId() != null && !(dbxref.equals(annot.getId()))) {
                        logger.warn("annot's dbxref_id " + dbxref + " doesn't match its uniquename " + annot.getId());
                    } else annot.setId(dbxref);
                }
            } else if (child.getType().equalsIgnoreCase("uniquename")) {
                if (annot.getId() != null && !(annot.getId().equals(child.getCharData()))) {
                    logger.warn("annot's primary xref id " + annot.getId() + " doesn't match its uniquename " + child.getCharData());
                    logger.warn("Using new uniquename " + child.getCharData() + " as the annotation id");
                }
                annot.setId(child.getCharData());
            } else if (child.getType().equalsIgnoreCase("featureloc")) {
                gotFeatureloc = true;
                handleFeatureLoc(child, annot, curation);
            } else if (child.getType().equalsIgnoreCase("feature_relationship")) {
                Transcript transcript = getTranscript(child, curation);
                String transcriptType = transcript.getTopLevelType();
                annot.addFeature(transcript);
                annot.setStrand(transcript.getStrand());
                annot.setFeatureType(transcriptType);
                annot.setTopLevelType(transcriptType);
            } else if (child.getType().equalsIgnoreCase("feature_synonym")) {
                Synonym syn = getSynonym(child);
                annot.addSynonym(syn);
            } else if (child.getType().equalsIgnoreCase("featureprop")) {
                getProperty(child, annot);
            } else if (child.getType().equalsIgnoreCase("feature_dbxref")) {
                getDbxref(child, annot, false);
            } else if (child.getType().equalsIgnoreCase("feature_cvterm")) {
                if (!warnedAboutFeatureCvterm) {
                    logger.warn("not handling feature_cvterm(s) for annot " + annot.getId());
                    warnedAboutFeatureCvterm = true;
                }
            } else if (child.getType().equalsIgnoreCase("feature_pub")) {
                logger.warn("not handling feature_pub for annot " + annot.getId());
            } else if (child.getType().equalsIgnoreCase("organism_id")) {
                getOrganism(child, annot);
                if (curation.getOrganism() == null || curation.getOrganism().equals("")) curation.setOrganism(annot.getProperty("organism"));
            } else {
                if (!child.getCharData().equals("")) {
                    addField(annot, child);
                }
            }
        }
        if (!gotFeatureloc) {
            logger.warn(annot.getFeatureType() + " annotation " + annot.getName() + " (" + annot.getId() + ") has no start/end positions!");
            if (annot.getStrand() == 0) {
                annot.addProperty("unstranded", "true");
                annot.setStrand(1);
            }
            annot.setStart(0);
            annot.setEnd(0);
            return annot;
        }
        warnIfOneLevelDiscrepancy(annot);
        forceStrandIfNeeded(annot);
        if (annot.getName().equals("no_name") && annot.getId() != null) {
            logger.warn("annot with uniquename " + annot.getId() + " has no name--using uniquename as name.");
            annot.setName(annot.getId());
        }
        return annot;
    }

    /** Now that we're treating one-level annots as one-levels (rather than
    * promoting them to three levels), we expect them to be identified as such
    * in the tiers file:
    * number_of_levels : 1
    * If the user has an out-of-date tiers file without this line for a
    * one-level type, the one-level annot will not show up.  Pop up a warning. */
    private void warnIfOneLevelDiscrepancy(SeqFeature annot) {
        if ((annot.getFeatures() == null || annot.getFeatures().size() == 0)) {
            FeatureProperty fp = Config.getPropertyScheme().getFeatureProperty(annot.getFeatureType());
            if (fp.getNumberOfLevels() != 1) {
                apollo.util.IOUtil.errorDialog("Annotation " + annot.getName() + " is one-level, but type " + annot.getFeatureType() + "\ndoes not have 'number_of_levels: 1' in tiers file " + Config.getStyle().getTiersFile() + ".\nEither the data is buggy or your tiers file is out of date.");
            }
        }
    }

    /** Wordy chadoXML way to store feature type is
   *    <type_id>       <cvterm>        <cv_id>          <cv>            <name>SO</name>          </cv>        </cv_id>
   *    <name>gene</name>      </cvterm>    </type_id>
   *   So the actual type name (in this case, gene) is in the "name" element. */
    private String getDataType(XMLElement xml) {
        if (!(xml.getType().equalsIgnoreCase("type_id"))) {
            xml = getChild(xml, "type_id");
            if (xml == null) return null;
        }
        XMLElement child = (XMLElement) xml.getChildren().firstElement();
        XMLElement grandchild2 = getChild(child, "name");
        return (grandchild2.getCharData());
    }

    /** <feature_dbxref> (optional layer--expect this if is_primary_dbxref is false)
   *   <dbxref_id>      
   *     <dbxref>        
   *       <accession>CG10833</accession>
   *       <db_id>
   *         <db>
   *           <contact_id>  <contact>  <description>dummy</description>  (optional)
   *           <name>FlyBase</name>
   *   </dbxref_id>
   *   <is_current>   (only for feature_dbxrefs)
   *   </feature_dbxref> (optional) 
   *  Adds a dbxref to feature (if feature is not null) and also 
   *  returns the dbxref. */
    private DbXref getDbxref(XMLElement xml, SeqFeature feature, boolean is_primary_dbxref) {
        Vector children = xml.getChildren();
        if (children == null) {
            logger.warn("getDbxref: no children of " + ChadoXmlUtils.printXMLElement(xml));
            return null;
        }
        XMLElement child = (XMLElement) children.firstElement();
        if (!is_primary_dbxref) child = getChild(child, "dbxref");
        Vector grandchildren = child.getChildren();
        if (grandchildren == null) {
            logger.warn("getDbxref: no grandchildren of " + ChadoXmlUtils.printXMLElement(xml));
            return null;
        }
        XMLElement grandchild = (XMLElement) grandchildren.firstElement();
        if (!grandchild.getType().equalsIgnoreCase("accession")) {
            logger.warn("Grandchild of dbxref_id is not accession: " + ChadoXmlUtils.printXMLElement(grandchild));
            return null;
        }
        String acc = grandchild.getCharData();
        XMLElement db_xml = getChild(child, "db_id");
        String db = "";
        if (db_xml != null) db = getDb(db_xml);
        int isCurrent = 1;
        if (!is_primary_dbxref) {
            XMLElement current = getChild(xml, "is_current");
            if (current != null) {
                try {
                    isCurrent = Integer.parseInt(current.getCharData());
                } catch (Exception e) {
                    logger.warn("Couldn't parse integer from is_current " + current + " for acc " + acc, e);
                }
            }
        }
        if (!(db == null) && !db.equals("") && !(acc == null) && !acc.equals("")) {
            DbXref xref = new DbXref("id", acc, db);
            if (is_primary_dbxref) {
                xref.setIsPrimary(true);
                xref.setIsSecondary(false);
            } else {
                DbXref primary = feature.getPrimaryDbXref();
                if (primary != null && primary.getIdValue().equals(acc)) {
                    primary.setIsSecondary(true);
                    xref = primary;
                }
            }
            xref.setCurrent(isCurrent);
            if (feature != null) feature.addDbXref(xref);
            return xref;
        }
        return null;
    }

    /** <db_id>
   *    <db>
   *       <contact_id>  <contact>  <description>dummy</description>  (optional)
   *       <name>FlyBase</name> */
    private String getDb(XMLElement xml) {
        XMLElement name = getGrandchild(xml, -1, "name");
        if (name == null) return ""; else return (name.getCharData());
    }

    /** Sets start, end, and strand for feature, based on featureloc record. */
    private void handleFeatureLoc(XMLElement xml, SeqFeatureI feature, CurationSet curation) {
        Vector children = xml.getChildren();
        int start = -1, end = -1, strand = 1;
        for (int i = 0; i < children.size(); i++) {
            XMLElement child = (XMLElement) children.elementAt(i);
            try {
                if (child.getType().equalsIgnoreCase("fmin")) {
                    try {
                        start = Integer.parseInt(child.getCharData()) + 1;
                        feature.setLow(start);
                    } catch (Exception e) {
                        logger.error("Couldn't parse integer from fmin " + child.getCharData(), e);
                    }
                } else if (child.getType().equalsIgnoreCase("fmax")) {
                    try {
                        end = Integer.parseInt(child.getCharData());
                        feature.setHigh(end);
                    } catch (Exception e) {
                        logger.error("Couldn't parse integer from fmax " + child.getCharData(), e);
                    }
                } else if (child.getType().equalsIgnoreCase("strand")) {
                    try {
                        strand = Integer.parseInt(child.getCharData());
                        feature.setStrand(strand);
                    } catch (Exception e) {
                        logger.error("Couldn't parse integer from strand " + child.getCharData(), e);
                    }
                } else if (isAnalysis(feature) && child.getType().equalsIgnoreCase("rank")) {
                    feature.addProperty(child.getType(), child.getCharData());
                } else if (child.getType().equalsIgnoreCase("residue_info")) {
                    String residues = child.getCharData();
                    feature.setExplicitAlignment(residues);
                } else if (child.getType().equalsIgnoreCase("srcfeature_id")) {
                    if (isAnalysis(feature)) handleSrcFeature(child, feature, curation);
                } else if (child.getType().equalsIgnoreCase("is_fmin_partial") || child.getType().equalsIgnoreCase("is_fmax_partial")) {
                    feature.addProperty(child.getType(), child.getCharData());
                }
            } catch (Exception e) {
                logger.error("Exception handling featureloc " + ChadoXmlUtils.printXMLElement(xml), e);
            }
        }
    }

    /** Handle the scrfeature_id record, which is for a query or subject.
   *  <srcfeature_id>
   *    <feature>
   *     <dbxref_id>
   *     <name>RH26018.3prime</name>
   *     <organism_id>
   *     <residues>
   *     <type_id>
   *     <featureprop>
   *      <name>description</name>
   *      <value>gb|CK135150|bdgp|RH26018.3prime DESCRIPTION:&quot;RH26018.3prime RH Drosophila melanogaster normalized Head pFlc-1 Drosophila melanogaster cDNA clone RH26018 3, mRNA sequence.&quot; organism:&quot;Drosophila melanogaster&quot; (02-DEC-2003)</value>
   *    </feature> */
    void handleSrcFeature(XMLElement xml, SeqFeatureI feat, CurationSet curation) {
        XMLElement xml_feature = getChild(xml, "feature");
        if (xml == null) {
            logger.error("handleSrcFeature: couldn't find feature child of " + ChadoXmlUtils.printXMLElement(xml));
            return;
        }
        Vector children = xml_feature.getChildren();
        for (int i = 0; i < children.size(); i++) {
            XMLElement child = (XMLElement) children.elementAt(i);
            try {
                if (child.getType().equalsIgnoreCase("is_analysis")) {
                } else if (child.getType().equalsIgnoreCase("name")) {
                    feat.replaceProperty("ref_name", child.getCharData());
                    SequenceI seq = feat.getRefSequence();
                    if (seq == null) {
                        seq = new Sequence(feat.getName(), "");
                        feat.setRefSequence(seq);
                    }
                    seq.setName(child.getCharData());
                } else if (child.getType().equalsIgnoreCase("uniquename")) {
                    feat.replaceProperty("ref_id", child.getCharData());
                } else if (child.getType().equalsIgnoreCase("dbxref_id")) {
                    if (!savedGenomicDbxref && feat.getProperty("rank").equals("0")) {
                        DbXref xref = getDbxref(child, null, true);
                        if (xref != null) {
                            curation.getRefSequence().addDbXref(xref);
                            savedGenomicDbxref = true;
                        }
                    }
                } else if (child.getType().equalsIgnoreCase("organism_id")) {
                    getOrganism(child, feat);
                } else if (child.getType().equalsIgnoreCase("type_id")) {
                    String type = getDataType(child);
                    feat.addProperty("ref_type", type);
                } else if (child.getType().equalsIgnoreCase("residues")) {
                    String residues = child.getCharData();
                    SequenceI seq = feat.getRefSequence();
                    if (seq == null) {
                        seq = new Sequence(feat.getName(), residues);
                        feat.setRefSequence(seq);
                    } else seq.setResidues(residues);
                } else if (child.getType().equalsIgnoreCase("featureprop")) {
                    getProperty(child, feat);
                }
            } catch (Exception e) {
                logger.error("Exception handling srcfeature_id " + ChadoXmlUtils.printXMLElement(child) + ": " + e, e);
            }
        }
    }

    private boolean isAnalysis(SeqFeatureI feat) {
        if (feat.getProperty("is_analysis").equals("") || feat.getProperty("is_analysis").equals("0") || feat.getProperty("is_analysis").equals(FIELD_LABEL + "0")) return false; else return true;
    }

    /** Add a flat field (e.g. <is_obsolete>1</is_obsolete>) as a property,
      marking it as a field if appropriate. */
    private void addField(SeqFeatureI feat, XMLElement xml) {
        String type = xml.getType();
        String value = xml.getCharData();
        if (!ChadoXmlWrite.isSpecialProperty(type)) value = FIELD_LABEL + value;
        feat.addProperty(type, value);
    }

    /** For parsing children (transcripts/exons) of an annotation.
   *   xml node passed in is feature_relationship.
   *   <feature_relationship>
   *     <subject_id>
   *       <feature>
   *         <uniquename>CG10833-RA</uniquename>          
   *         <feature_relationship>
   *           [exons] 
   *         <featureloc> */
    private Transcript getTranscript(XMLElement xml, CurationSet curation) throws ApolloAdapterException {
        Transcript transcript = new Transcript();
        transcript.setRefSequence(curation.getRefSequence());
        transcript.addProperty("is_analysis", "0");
        Vector children = xml.getChildren();
        Vector grandchildren = null;
        for (int i = 0; i < children.size(); i++) {
            XMLElement child = (XMLElement) children.elementAt(i);
            if (child.getType().equalsIgnoreCase("subject_id")) {
                grandchildren = child.getChildren();
                if (grandchildren == null) {
                    logger.error("getTranscript: no grandchildren for " + ChadoXmlUtils.printXMLElement(xml));
                    return transcript;
                }
                XMLElement xml_feature = (XMLElement) grandchildren.firstElement();
                if (!xml_feature.getType().equalsIgnoreCase("feature")) {
                    logger.error("getTranscript: found non-feature child " + ChadoXmlUtils.printXMLElement(xml_feature) + "\n of subject_id " + ChadoXmlUtils.printXMLElement(child));
                    return null;
                }
                Vector transcript_parts = xml_feature.getChildren();
                for (int j = 0; j < transcript_parts.size(); j++) {
                    XMLElement tp = (XMLElement) transcript_parts.elementAt(j);
                    if (tp.getType().equalsIgnoreCase("dbxref_id")) {
                        DbXref xref = getDbxref(tp, transcript, true);
                        if (xref != null) transcript.setId(xref.getIdValue());
                    } else if (tp.getType().equalsIgnoreCase("uniquename")) {
                        if (tp.getCharData() == null) {
                            String errMsg = "<uniquename> cannot be null";
                            logger.error(errMsg);
                            throw new ApolloAdapterException(errMsg);
                        }
                        if (!(tp.getCharData().equals(transcript.getId()))) {
                            logger.warn("uniquename " + tp.getCharData() + " doesn't match transcript's dbxref_id " + transcript.getId());
                            transcript.setId(tp.getCharData());
                        }
                    } else if (tp.getType().equalsIgnoreCase("name")) {
                        transcript.setName(tp.getCharData());
                    } else if (tp.getType().equalsIgnoreCase("type_id")) {
                        String type = getDataType(tp);
                        if (type.equalsIgnoreCase("mRNA")) transcript.setTopLevelType("gene"); else transcript.setTopLevelType(type);
                    } else if (tp.getType().equalsIgnoreCase("md5checksum")) {
                        String checksum = tp.getCharData();
                        if (checksum != null && !checksum.equals("")) {
                            SequenceI seq = transcript.get_cDNASequence();
                            if (seq == null) {
                                seq = new Sequence(transcript.getId(), "");
                                transcript.set_cDNASequence(seq);
                            }
                            seq.setChecksum(checksum);
                        }
                    } else if (tp.getType().equalsIgnoreCase("residues")) {
                        String dna = tp.getCharData();
                        SequenceI seq = transcript.get_cDNASequence();
                        if (seq == null) {
                            seq = new Sequence(transcript.getId(), dna);
                            transcript.set_cDNASequence(seq);
                        } else seq.setResidues(dna);
                        seq.setLength(dna.length());
                        if (transcript.getId() != null && !transcript.getId().equals("")) seq.setAccessionNo(transcript.getId());
                        seq.setResidueType(SequenceI.DNA);
                        curation.addSequence(seq);
                    } else if (tp.getType().equalsIgnoreCase("seqlen")) {
                    } else if (tp.getType().equalsIgnoreCase("feature_relationship")) {
                        XMLElement rank = getChild(tp, "rank");
                        if (rank != null) transcript.addProperty(tp.getType(), tp.getCharData());
                        XMLElement subject_id = getChild(tp, "subject_id");
                        String type = getDataType(getGrandchild(subject_id, "type_id"));
                        if (type.equalsIgnoreCase("exon")) {
                            ExonI exon = getExon(tp, curation.getRefSequence());
                            transcript.addExon(exon);
                            if (transcript.getStrand() != 0 && exon.getStrand() != transcript.getStrand()) logger.warn("strand for " + exon + " doesn't match strand " + transcript.getStrand() + " for transcript " + transcript);
                            transcript.setStrand(exon.getStrand());
                        } else if (type.equalsIgnoreCase("protein")) {
                            addPeptide(getChild(subject_id, 0, null), transcript, curation);
                        } else {
                            logger.error("Don't know how to handle child type " + type + " for transcript " + transcript.getId());
                        }
                    } else if (tp.getType().equals("featureloc")) {
                        handleFeatureLoc(tp, transcript, curation);
                        if (transcript.get_cDNASequence() == null) {
                            Sequence seq = new Sequence(transcript.getId(), "");
                            transcript.set_cDNASequence(seq);
                        } else {
                            transcript.get_cDNASequence().setRange(new Range(transcript.getLow(), transcript.getHigh()));
                        }
                    } else if (tp.getType().equalsIgnoreCase("feature_dbxref")) {
                        getDbxref(tp, transcript, false);
                    } else if (tp.getType().equalsIgnoreCase("feature_synonym")) {
                        Synonym syn = getSynonym(tp);
                        transcript.addSynonym(syn);
                    } else if (tp.getType().equalsIgnoreCase("featureprop")) {
                        getProperty(tp, transcript);
                    } else if (tp.getType().equalsIgnoreCase("feature_cvterm")) {
                        if (!warnedAboutFeatureCvterm) {
                            logger.warn("not handling feature_cvterm for transcript " + transcript.getId());
                            warnedAboutFeatureCvterm = true;
                        }
                    } else if (tp.getType().equalsIgnoreCase("feature_pub")) {
                        logger.warn("not handling feature_pub for transcript " + transcript.getId());
                    } else if (tp.getType().equalsIgnoreCase("organism_id")) {
                        getOrganism(tp, transcript);
                    } else {
                        if (!(tp.getCharData().equals(""))) {
                            addField(transcript, tp);
                        }
                    }
                }
            }
        }
        if (transcript.isMissing5prime()) {
            transcript.calcTranslationStartForLongestPeptide();
        }
        return transcript;
    }

    /** This does mostly the same thing as getTranscript (at the next level down).
   *   xml element is feature_relationship.
   *   <uniquename>CG10833-RA</uniquename>          
   *   <feature_relationship>
   *   <rank>6</rank>
   *   <subject_id>
   *   <feature>
   *   <name>Cyp28d1:6</name>
   *   ...
   *   <featureloc>
   *   <fmax>5212450</fmax>
   *   <fmin>5212212</fmin> */
    private ExonI getExon(XMLElement xml, SequenceI refSeq) {
        Exon exon = new Exon();
        exon.setRefSequence(refSeq);
        exon.addProperty("is_analysis", "0");
        Vector children = xml.getChildren();
        for (int i = 0; i < children.size(); i++) {
            XMLElement child = (XMLElement) children.elementAt(i);
            if (child.getType().equalsIgnoreCase("rank")) {
                exon.addProperty(child.getType(), child.getCharData());
            } else if (child.getType().equalsIgnoreCase("subject_id")) {
                Vector grandchildren = child.getChildren();
                if (grandchildren == null) {
                    logger.warn("getExon: no grandchildren for " + ChadoXmlUtils.printXMLElement(xml));
                    return exon;
                }
                XMLElement xml_feature = (XMLElement) grandchildren.firstElement();
                if (!xml_feature.getType().equalsIgnoreCase("feature")) {
                    logger.warn("Wrong child " + ChadoXmlUtils.printXMLElement(xml_feature) + " of subject_id " + ChadoXmlUtils.printXMLElement(child));
                    return exon;
                }
                Vector exon_parts = xml_feature.getChildren();
                for (int j = 0; j < exon_parts.size(); j++) {
                    XMLElement ep = (XMLElement) exon_parts.elementAt(j);
                    if (ep.getType().equalsIgnoreCase("name")) {
                        exon.setName(ep.getCharData());
                    } else if (ep.getType().equalsIgnoreCase("uniquename")) {
                        exon.setId(ep.getCharData());
                    } else if (ep.getType().equals("featureloc")) handleFeatureLoc(ep, exon, null); else if (ep.getType().equalsIgnoreCase("organism_id")) {
                        getOrganism(ep, exon);
                    } else {
                        if (!ep.getCharData().equals("")) {
                            addField(exon, ep);
                        }
                    }
                }
            }
        }
        return exon;
    }

    /** Use peptide's featureloc to set translation start and end
      under the xml element passed in:
      <uniquename>CG9397-PA</uniquename>
      <residues>
      <featureloc> */
    private void addPeptide(XMLElement xml, Transcript transcript, CurationSet curation) {
        String seq_id = "";
        Sequence seq = new Sequence(seq_id, "");
        AnnotatedFeatureI protFeat = transcript.getProteinFeat();
        Vector children = xml.getChildren();
        for (int i = 0; i < children.size(); i++) {
            XMLElement child = (XMLElement) children.elementAt(i);
            if (child.getType().equalsIgnoreCase("uniquename")) {
                seq_id = child.getCharData();
                if (seq_id != null && !seq_id.equals("")) seq.setAccessionNo(seq_id);
            } else if (child.getType().equalsIgnoreCase("dbxref_id")) {
                DbXref xref = getDbxref(child, (SeqFeature) protFeat, true);
                if (xref != null) {
                    seq_id = xref.getIdValue();
                    if (seq_id != null && !seq_id.equals("")) seq.setAccessionNo(seq_id);
                }
            } else if (child.getType().equalsIgnoreCase("feature_dbxref")) {
                getDbxref(child, (SeqFeature) protFeat, false);
            } else if (child.getType().equalsIgnoreCase("residues")) {
                seq.setResidues(child.getCharData());
                seq.setResidueType(SequenceI.AA);
            } else if (child.getType().equalsIgnoreCase("name")) {
                seq.setName(child.getCharData());
            } else if (child.getType().equalsIgnoreCase("md5checksum")) {
                String checksum = child.getCharData();
                if (checksum != null && !checksum.equals("")) seq.setChecksum(checksum);
            } else if (child.getType().equalsIgnoreCase("featureloc")) {
                Vector fchildren = child.getChildren();
                int start = -1, end = -1, strand = 1;
                for (int j = 0; j < fchildren.size(); j++) {
                    XMLElement fchild = (XMLElement) fchildren.elementAt(j);
                    try {
                        if (fchild.getType().equalsIgnoreCase("fmin")) {
                            try {
                                start = Integer.parseInt(fchild.getCharData()) + 1;
                            } catch (Exception e) {
                                logger.error("Couldn't parse integer from fmin " + child.getCharData(), e);
                            }
                        } else if (fchild.getType().equalsIgnoreCase("fmax")) {
                            try {
                                end = Integer.parseInt(fchild.getCharData());
                            } catch (Exception e) {
                                logger.error("Couldn't parse integer from fmax " + child.getCharData(), e);
                            }
                        } else if (fchild.getType().equalsIgnoreCase("strand")) {
                            try {
                                strand = Integer.parseInt(fchild.getCharData());
                            } catch (Exception e) {
                                logger.error("Couldn't parse integer from strand " + child.getCharData(), e);
                            }
                        } else if (fchild.getType().equalsIgnoreCase("is_fmin_partial") || fchild.getType().equalsIgnoreCase("is_fmax_partial")) {
                            protFeat.addProperty(fchild.getType(), fchild.getCharData());
                        }
                    } catch (Exception e) {
                        logger.error("Exception handling featureloc " + ChadoXmlUtils.printXMLElement(fchild), e);
                    }
                }
                seq.setRange(new Range(start, end));
                transcript.setPeptideSequence(seq);
                curation.addSequence(seq);
                boolean missing5prime = false;
                boolean missing3prime = false;
                if ((protFeat.getProperty("is_fmin_partial").equals("1") && strand == 1) || (protFeat.getProperty("is_fmax_partial").equals("1") && strand == -1)) missing5prime = true;
                if ((protFeat.getProperty("is_fmax_partial").equals("1") && strand == 1) || (protFeat.getProperty("is_fmin_partial").equals("1") && strand == -1)) missing3prime = true;
                if (transcript.getProperty("missing_start_codon").equals("true") || missing5prime) {
                    transcript.setMissing5prime(true);
                    logger.debug(seq.getName() + " has now been marked as missing start codon");
                } else if (!missing3prime) {
                    boolean foundTranslationStart = false;
                    if (strand == 1) foundTranslationStart = transcript.setTranslationStart(start, false); else foundTranslationStart = transcript.setTranslationStart(end, false);
                    if (!foundTranslationStart) {
                        logger.warn("couldn't set translation start to " + (strand == 1 ? start : end) + " for transcript " + transcript.getName());
                    } else if (transcript.isTransSpliced()) {
                        logger.debug("Dealing with trans-spliced transcript " + transcript.getName());
                        transcript.sortTransSpliced();
                    }
                }
                if (transcript.getProperty("missing_stop_codon").equals("true") || missing3prime) {
                    logger.debug(seq.getName() + " is marked as missing stop codon.");
                    transcript.setMissing3prime(true);
                } else {
                    if (strand == 1) {
                        transcript.setTranslationEnd(end + 1);
                    } else {
                        transcript.setTranslationEnd(start - 1);
                    }
                }
            } else if (child.getType().equalsIgnoreCase("feature_synonym")) {
                Synonym synonym = getSynonym(child);
                protFeat.addSynonym(synonym);
            } else if (child.getType().equalsIgnoreCase("organism_id")) {
                getOrganism(child, seq);
            } else {
                if (!child.getCharData().equals("")) {
                    addField(protFeat, child);
                }
            }
        }
    }

    /** Some features are unstranded--put on the best-guess strand, add
    * property indicating that. */
    private void forceStrandIfNeeded(AnnotatedFeatureI annot) {
        if (annot.getStrand() == 0) {
            annot.addProperty("unstranded", "true");
            int strand = 1;
            if (annot.getStart() > annot.getEnd()) {
                strand = -1;
                int temp = annot.getStart();
                annot.setStart(annot.getEnd());
                annot.setEnd(temp);
                logger.info("Had to swap start and end for unstranded feature " + annot.getName() + " because start>end");
            }
            annot.setStrand(strand);
            logger.info("Annot " + annot.getName() + " (" + annot.getId() + ") is unstranded--showing on " + strand + " strand.  start = " + annot.getStart() + ", end = " + annot.getEnd());
        }
    }

    private Synonym getSynonym(XMLElement xml) {
        Synonym syn = new Synonym();
        Vector children = xml.getChildren();
        for (int i = 0; i < children.size(); i++) {
            XMLElement child = (XMLElement) children.elementAt(i);
            if (child.getType().equalsIgnoreCase("synonym_id")) {
                XMLElement nameElement = getGrandchild(child, "name");
                String name = nameElement.getCharData();
                name = HTMLUtil.replaceSGMLWithGreekLetter(name);
                syn.setName(name);
                XMLElement sgml = getGrandchild(child, "synonym_sgml");
                if (sgml != null) syn.addProperty("synonym_sgml", sgml.getCharData()); else syn.addProperty("synonym_sgml", name);
            } else if (child.getType().equalsIgnoreCase("pub_id")) {
                syn.setOwner(getSynonymAuthor(child));
                String pubType = getPubType(child);
                if (pubType != null) syn.addProperty("pub_type", pubType);
            } else if (child.getCharData() != null && !(child.getCharData().equals(""))) syn.addProperty(child.getType(), child.getCharData());
        }
        return syn;
    }

    private String getProperty(XMLElement xml, SeqFeatureI feature) {
        XMLElement type_id = getChild(xml, "type_id");
        String val = null;
        if (type_id == null) {
            logger.debug("getProperty: couldn't find grandchild type_id of node " + ChadoXmlUtils.printXMLElement(xml));
            return null;
        }
        String prop = getDataType(type_id);
        XMLElement value = getChild(xml, "value");
        if (value == null) return null;
        val = value.getCharData();
        if (handleSpecialProp(xml, prop, val, feature)) return val;
        if (feature != null) {
            feature.addProperty(prop, val);
        }
        return val;
    }

    /** Certain special properties (e.g. "comment", "owner") are dealt with specially
   *  rather than being saved as generic properties. 
   *  Returns true if the property is indeed special (and thus is dealt with here). */
    private boolean handleSpecialProp(XMLElement xml, String prop, String val, SeqFeatureI feature) {
        if (val == null) {
            logger.info("empty <value> for <featureprop> - skipping this featureprop");
            return false;
        }
        if (prop.equalsIgnoreCase("comment")) {
            if (!(feature instanceof AnnotatedFeatureI)) {
                logger.error("Can't add comment " + val + " to non-annotation feature " + feature);
                feature.addProperty(prop, val);
                return true;
            }
            Comment comment = new Comment();
            comment.setText(val);
            if (val.indexOf("nternal view only") > 0) comment.setIsInternal(true);
            String curator = getCurator(getChild(xml, "featureprop_pub"));
            comment.setPerson(curator);
            if (val.indexOf("TS") > 0) {
                String timestring = val.substring(val.indexOf("TS") + 3);
                try {
                    long time = Long.parseLong(timestring);
                    comment.setTimeStamp(time);
                } catch (NumberFormatException e) {
                    logger.warn("error parsing timestamp " + timestring + " from comment " + val);
                }
            }
            ((AnnotatedFeatureI) feature).addComment(comment);
            return true;
        } else if (prop.equalsIgnoreCase("owner")) {
            if (feature instanceof AnnotatedFeatureI) {
                ((AnnotatedFeatureI) feature).setOwner(val);
                return true;
            }
        } else if (prop.equalsIgnoreCase("problem") && (feature instanceof AnnotatedFeatureI)) {
            if (val.equals("true") || val.equals("t")) ((AnnotatedFeatureI) feature).setIsProblematic(true); else logger.warn("non-boolean value for problem for annotation or transcript " + feature.getName() + "--can't save it.");
            return true;
        } else if (prop.equalsIgnoreCase("problem")) {
            ((SeqFeature) feature).addProperty("tag", val);
            return true;
        } else if (prop.equalsIgnoreCase("description")) {
            addDescription(feature, val);
        } else if (prop.equalsIgnoreCase("non_canonical_start_codon") && (feature instanceof Transcript)) {
            logger.warn("not yet doing anything with non_canonical_start_codon property--\nnormally this is derived by Apollo.");
            return true;
        } else if (prop.equalsIgnoreCase("non_canonical_splice_site") && (feature instanceof Transcript)) {
            logger.info("Marking non_canonical_splice_site " + (val.equalsIgnoreCase("approved") ? "approved" : "unapproved") + " for transcript " + feature.getName());
            ((Transcript) feature).nonConsensusSplicingOkay(val.equalsIgnoreCase("approved"));
            return true;
        } else if ((prop.equalsIgnoreCase("plus_1_translational_frame_shift") || prop.equalsIgnoreCase("plus1_translational_frame_shift") || prop.equalsIgnoreCase("plus_1_translational_frameshift")) && (feature instanceof Transcript)) {
            try {
                int plus1_frameshift = Integer.parseInt(val);
                logger.info("Marking plus_1_translational_frameshift = " + plus1_frameshift + " for transcript " + feature.getName());
                ((Transcript) feature).setPlus1FrameShiftPosition(plus1_frameshift);
            } catch (Error e) {
                logger.error("Couldn't parse plus_1_translational_frameshift value " + val + "--not an integer", e);
            }
            return true;
        } else if ((prop.equalsIgnoreCase("minus_1_translational_frame_shift") || prop.equalsIgnoreCase("minus1_translational_frame_shift") || prop.equalsIgnoreCase("minus_1_translational_frameshift")) && (feature instanceof Transcript)) {
            try {
                int minus1_frameshift = Integer.parseInt(val);
                logger.info("Marking minus_1_translational_frameshift = " + minus1_frameshift + " for transcript " + feature.getName());
                ((Transcript) feature).setMinus1FrameShiftPosition(minus1_frameshift);
            } catch (Exception e) {
                logger.error("Couldn't parse minus_1_translational_frameshift value " + val + "--not an integer", e);
            }
            return true;
        } else if (prop.equalsIgnoreCase("stop_codon_redefinition_as_selenocysteine") && (feature instanceof Transcript)) {
            boolean seleno = (val.toLowerCase().startsWith("t") || val.equalsIgnoreCase("U"));
            if (seleno) {
                logger.info("Got stop_codon_redefinition_as_selenocysteine for transcript " + feature.getName());
                ((Transcript) feature).setReadThroughStop("U");
                return true;
            }
        } else if ((feature instanceof Transcript) && ((prop.equalsIgnoreCase("stop_codon_readthrough") || prop.equalsIgnoreCase("readthrough_stop_codon")))) {
            logger.info("Got stop_codon_readthrough = " + val + " for transcript " + feature.getName());
            ((Transcript) feature).setReadThroughStop(val);
            return true;
        } else if ((feature instanceof Transcript) && (prop.equalsIgnoreCase("missing_start_codon") && val.equalsIgnoreCase("true"))) {
            logger.info("Marking missing_start_codon for transcript " + feature.getName());
            ((Transcript) feature).calcTranslationStartForLongestPeptide();
            return true;
        } else if ((feature instanceof Transcript) && (prop.equalsIgnoreCase("missing_stop_codon") && val.equalsIgnoreCase("true"))) {
            logger.info("Marking missing_stop_codon for transcript " + feature.getName());
            ((Transcript) feature).setMissing3prime(true);
            return true;
        }
        return false;
    }

    /** Descriptions are actually added to the ref sequence of the feature. */
    private void addDescription(SeqFeatureI feature, String description) {
        SequenceI seq = feature.getRefSequence();
        if (seq == null) {
            seq = new Sequence(feature.getName(), "");
            feature.setRefSequence(seq);
        }
        GAMEAdapter.setSeqDescription(seq, description, feature.getName());
    }

    /** <featureprop_pub>  <pub_id>  <pub>  <type_id> pub type = curator </type_id>
   *  <uniquename>curatorname</uniquename> */
    private String getCurator(XMLElement xml) {
        XMLElement pub = getGrandchild(xml, 0, null);
        if (pub == null) return null;
        XMLElement curator = getChild(pub, "uniquename");
        if (curator == null) return null;
        return (curator.getCharData());
    }

    /** <pub_id>  <pub>  <type_id> </type_id>
   *  <uniquename>author</uniquename> */
    private String getSynonymAuthor(XMLElement xml) {
        XMLElement pub = getChild(xml, 0, null);
        if (pub == null) return null;
        XMLElement curator = getChild(pub, "uniquename");
        if (curator == null) return null;
        return (curator.getCharData());
    }

    /**   <pub_id>
   *         <pub>
   *           <is_obsolete>0</is_obsolete>
   *           <type_id>
   *             <cvterm>
   *               <cv_id>
   *                 <cv>
   *                   <name>pub type</name>
   *                 </cv>
   *               </cv_id>
   *               <is_obsolete>0</is_obsolete>
   *               <is_relationshiptype>0</is_relationshiptype>
   *               <name>publication</name>
   *             </cvterm>
   *           </type_id>
   *           <uniquename>FBrf0132177</uniquename>
   *         </pub>
   *       </pub_id>
   * The thing we're looking for here is <name>publication</name> */
    private String getPubType(XMLElement xml) {
        XMLElement pub = getChild(xml, 0, null);
        if (pub == null) return null;
        XMLElement type = getChild(pub, "type_id");
        if (type == null) return null;
        return (getDataType(type));
    }

    /**    <organism_id>
   *         <organism>
   *         <genus>Drosophila</genus>
   *         <species>melanogaster</species>
   *  For now, save as a property (organism="Drosophila melanogaster"), since
   *  features don't have an Organism field. */
    private void getOrganism(XMLElement xml, SeqFeatureI feat) {
        XMLElement genus = getGrandchild(xml, "genus");
        XMLElement species = getGrandchild(xml, "species");
        if (genus != null && species != null) {
            feat.addProperty("organism", genus.getCharData() + " " + species.getCharData());
        }
    }

    /** Sequences DO have an organism field */
    private void getOrganism(XMLElement xml, SequenceI seq) {
        XMLElement genus = getGrandchild(xml, "genus");
        XMLElement species = getGrandchild(xml, "species");
        if (genus != null && species != null) {
            seq.setOrganism(genus.getCharData() + " " + species.getCharData());
        }
    }

    /** For parsing children (SeqFeatures) of a computational result.
   *  These SeqFeatures are analagous to exons.
   *
   *  xml node passed in is feature_relationship.
   *      <feature_relationship>
   *      <subject_id>
   *      <feature>
   *      <uniquename>:7323967</uniquename>
   *      <analysisfeature>
   *      <feature_relationship>
   *      <featureloc> */
    private SeqFeatureI getSeqFeature(XMLElement xml, SeqFeatureI result, CurationSet curation) {
        SeqFeature feature1 = new SeqFeature();
        feature1.addProperty("is_analysis", "1");
        boolean sawFeatureLoc = false;
        SeqFeature feature2 = null;
        SeqFeature current_feature = feature1;
        Vector children = xml.getChildren();
        for (int i = 0; i < children.size(); i++) {
            XMLElement child = (XMLElement) children.elementAt(i);
            if (child.getType().equalsIgnoreCase("subject_id")) {
                Vector grandchildren = child.getChildren();
                if (grandchildren == null) {
                    logger.error("getSeqFeature: no grandchildren for " + ChadoXmlUtils.printXMLElement(xml));
                    return feature1;
                }
                XMLElement xml_feat = (XMLElement) grandchildren.firstElement();
                if (!xml_feat.getType().equalsIgnoreCase("feature")) {
                    logger.error("Wrong child " + ChadoXmlUtils.printXMLElement(xml_feat) + " of subject_id " + ChadoXmlUtils.printXMLElement(child));
                    return feature1;
                }
                Vector feat_parts = xml_feat.getChildren();
                for (int j = 0; j < feat_parts.size(); j++) {
                    XMLElement tp = (XMLElement) feat_parts.elementAt(j);
                    if (tp.getType().equalsIgnoreCase("organism_id")) {
                        getOrganism(tp, current_feature);
                    }
                    if (tp.getType().equalsIgnoreCase("type_id")) {
                        String type_id = getDataType(child);
                        current_feature.addProperty("type_id", type_id);
                    } else if (tp.getType().equalsIgnoreCase("dbxref_id")) {
                        if (current_feature.getId() == null) {
                            DbXref xref = getDbxref(tp, current_feature, false);
                            if (xref != null) current_feature.setId(xref.getIdValue());
                        }
                    } else if (tp.getType().equalsIgnoreCase("uniquename")) {
                        current_feature.setId(tp.getCharData());
                    } else if (tp.getType().equalsIgnoreCase("residue_info")) {
                        String residues = tp.getCharData();
                        current_feature.setExplicitAlignment(residues);
                    } else if (tp.getType().equalsIgnoreCase("analysisfeature")) {
                        getScore(tp, current_feature);
                    } else if (tp.getType().equalsIgnoreCase("featureloc")) {
                        if (!sawFeatureLoc) {
                            sawFeatureLoc = true;
                        } else {
                            feature2 = new SeqFeature();
                            feature2.addProperty("is_analysis", "1");
                            current_feature = feature2;
                        }
                        handleFeatureLoc(tp, current_feature, curation);
                    } else {
                        current_feature.addProperty(child.getType(), child.getCharData());
                    }
                }
            }
        }
        feature1.setFeatureType(result.getFeatureType());
        if (feature2 == null) {
            feature1.setName(result.getName());
            return feature1;
        }
        SeqFeature query = feature2;
        SeqFeature subject = feature1;
        if (feature2.getProperty("rank").equals("1")) {
            query = feature2;
            subject = feature1;
        }
        if (subject.getRefSequence() != null) curation.addSequence(subject.getRefSequence());
        query.setFeatureType(subject.getFeatureType());
        query.setScore(subject.getScore());
        FeaturePair pair = new FeaturePair(query, subject);
        pair.setExplicitAlignment(query.getExplicitAlignment());
        pair.setName(subject.getName());
        pair.setId(subject.getId());
        copyProperties(subject, pair);
        if (!subject.getProperty("tag").equals("")) {
            result.addProperty("tag", subject.getProperty("tag"));
        }
        return pair;
    }

    /** Extract analysis type fields (program, etc.) and set in result.
   *    Note that because Apollo's datamodels don't explicitly support these fields
   *    at the result set level (some of them live at the Analysis level), we will
   *    just save them all as properties.
   *      <analysisfeature>
   *      <analysis_id>
   *      <analysis>
   *      <program>sim4</program>
   *      <programversion>1.0</programversion>
   *      <sourcename>na_dbEST.same.dmel</sourcename>
   *      <sourceversion>1.0</sourceversion>
   *      <timeexecuted>2004-07-15 20:17:40</timeexecuted> 
   *      </analysis>
   *      </analysis_id>
   *      Result spans also have a <rawscore> field here (result sets don't).
   *      <rawscore>-0.77</rawscore>
  */
    private void handleAnalysisType(XMLElement xml, FeatureSetI result) {
        XMLElement analysis = getGrandchild(xml, 0, null);
        if (analysis == null) {
            result.setFeatureType("unknown_type");
            return;
        }
        String program = "";
        String db = "";
        Vector fields = analysis.getChildren();
        for (int i = 0; i < fields.size(); i++) {
            XMLElement field = (XMLElement) fields.elementAt(i);
            String name = field.getType();
            String value = field.getCharData();
            result.addProperty(name, value);
            if (name.equalsIgnoreCase("program")) {
                program = value;
                result.setProgramName(program);
            } else if (name.equalsIgnoreCase("sourcename")) {
                db = value;
                result.setDatabase(db);
            }
        }
        result.setFeatureType(constructAnalysisType(program, db));
    }

    /** Build Apollo-style analysis type name from program and sourcename.
   *  (From GAMEAdapter (called getAnalysisType there).) */
    private String constructAnalysisType(String prog, String db) {
        String analysis_type;
        if (!(prog == null) && !prog.equals("") && !(db == null) && !db.equals("")) analysis_type = prog + ":" + db; else if (!(prog == null) && !prog.equals("")) analysis_type = prog; else if (!(db == null) && !db.equals("")) analysis_type = db; else analysis_type = RangeI.NO_TYPE;
        return analysis_type;
    }

    /** Result spans have a <rawscore> field under analysisfeature (result sets don't).
   *  We can ignore the rest of the stuff in analysisfeature because it's a duplicate
   *  of the parent result set's.
   *      <analysisfeature>
   *      <analysis_id>
   *      <analysis>
   *      <program>sim4</program>
   *      [etc]
   *      </analysis>
   *      </analysis_id>
   *      <rawscore>-0.77</rawscore>  */
    private void getScore(XMLElement xml, SeqFeatureI result) {
        XMLElement xml_score = getChild(xml, "rawscore");
        if (xml_score != null) {
            try {
                double score = Double.parseDouble(xml_score.getCharData());
                result.setScore(score);
            } catch (Exception e) {
                logger.error("Exception parsing score " + ChadoXmlUtils.printXMLElement(xml_score), e);
            }
        }
    }

    /** Create forward and reverse strand feature sets for analysis of
   *  analysis element, which are added to analyses. Add all sub
   *  features to the appropriate stranded FeatureSet.  (Adapted from
   *  GAMEAdapter.)
   *
   *  Note that some of the fields that appear at the Result Set level
   *  in Chado XML (e.g. program, sourcename (i.e. database)) live at
   *  the Analysis level in the Apollo datamodels, so we copy them to
   *  the Analysis, but this assumes that that all the results of the
   *  same analysis type (program+db) in this file will have the same
   *  other fields (e.g. programversion) as well--that we won't have
   *  (in the same file) some results representing blastx against
   *  aa_SPTR of dec 2003 and some representing blastx against aa_SPTR
   *  of june 2004, for example.  This should be a harmless assumption
   *  because it will only affect the output when we're using Apollo to
   *  convert from Chado XML to GAME XML. */
    private void addResultToAnalysis(FeatureSetI result) {
        String prog = result.getProperty("program");
        String db = result.getProperty("sourcename");
        String date = result.getProperty("timeexecuted");
        String programversion = result.getProperty("programversion");
        String sourceversion = result.getProperty("sourceversion");
        FeatureSetI forward_analysis = initAnalysis(analyses, 1, prog, db, date, programversion, sourceversion, all_analyses);
        FeatureSetI reverse_analysis = initAnalysis(analyses, -1, prog, db, date, programversion, sourceversion, all_analyses);
        if (result.getStrand() == 1) {
            forward_analysis.addFeature(result);
            if (!forward_analysis.hasFeatureType()) forward_analysis.setFeatureType(result.getFeatureType());
        } else {
            reverse_analysis.addFeature(result);
            if (!reverse_analysis.hasFeatureType()) {
                reverse_analysis.setFeatureType(result.getFeatureType());
            }
        }
        boolean fwd = addAnalysisIfHasFeatures(forward_analysis, all_analyses, analyses);
        boolean rev = addAnalysisIfHasFeatures(reverse_analysis, all_analyses, analyses);
        if (fwd && rev) {
            forward_analysis.setAnalogousOppositeStrandFeature(reverse_analysis);
            reverse_analysis.setAnalogousOppositeStrandFeature(forward_analysis);
        }
    }

    /** Creates a new FeatureSet for the analysis on strand. If an analysis with prog,db, 
   *  and strand has already been created, the existing one is returned. If new a
   *  FeatureSet is created, added to all_analyses hash, added to analyses, and returned. 
   *  (Adapted from GAMEAdapter.) 
   *  Note that many of the arguments are not actually used. */
    private FeatureSetI initAnalysis(StrandedFeatureSetI analyses, int strand, String prog, String db, String date, String programversion, String sourceversion, Hashtable all_analyses) {
        String analysis_type = constructAnalysisType(prog, db);
        String analysis_name;
        if (!analysis_type.equals(RangeI.NO_TYPE)) analysis_name = (analysis_type + (strand == 1 ? "-plus" : (strand == -1 ? "-minus" : ""))); else analysis_name = (RangeI.NO_NAME + (strand == 1 ? "-plus" : (strand == -1 ? "-minus" : "")));
        if (all_analyses == null) all_analyses = new Hashtable();
        FeatureSetI analysis = (FeatureSetI) all_analyses.get(analysis_name);
        if (analysis == null) {
            analysis = new FeatureSet();
            analysis.setProgramName(prog);
            analysis.setDatabase(db);
            analysis.setFeatureType(analysis_type);
            if (!analysis_type.equals(RangeI.NO_TYPE)) analysis.setName(analysis_name);
            analysis.setStrand(strand);
        }
        return analysis;
    }

    /** Add in stranded analyses only if features were found on that strand;
   *  otherwise it's scrapped (not added to all_analyses and analyses) 
   *  and false is returned */
    private boolean addAnalysisIfHasFeatures(FeatureSetI analysis, Hashtable all_analyses, StrandedFeatureSetI analyses) {
        if (all_analyses == null) {
            return false;
        }
        if (all_analyses.containsKey(analysis.getName())) {
            return true;
        }
        if (analysis.size() > 0) {
            all_analyses.put(analysis.getName(), analysis);
            analyses.addFeature(analysis);
            return true;
        }
        return false;
    }

    private static void copyProperties(SeqFeatureI from, SeqFeatureI to) {
        Hashtable props = from.getProperties();
        Enumeration e = props.keys();
        while (e.hasMoreElements()) {
            String type = (String) e.nextElement();
            if (dontCopyProperty(type)) continue;
            Vector values = ((SeqFeature) from).getPropertyMulti(type);
            if (values == null) continue;
            for (int i = 0; i < values.size(); i++) {
                String value = (String) values.elementAt(i);
                to.addProperty(type, value);
            }
        }
    }

    /** Some properties shouldn't be copied from subject to feature pair, e.g. rank.
   *  (otherwise it gets rank=1 from the subject) */
    private static boolean dontCopyProperty(String prop) {
        if (ChadoXmlWrite.isSpecialProperty(prop)) return true; else return false;
    }

    private XMLElement getChild(XMLElement xml, String elementType) {
        return getChild(xml, -1, elementType);
    }

    private XMLElement getChild(XMLElement xml, int num, String elementType) {
        if (xml == null) return null;
        Vector children = xml.getChildren();
        if (children == null || children.size() <= num) return null;
        if (num < 0 && elementType != null) {
            for (int i = 0; i < children.size(); i++) {
                XMLElement child = (XMLElement) children.elementAt(i);
                if (child.getType().equalsIgnoreCase(elementType)) return child;
            }
            return null;
        }
        XMLElement child = (XMLElement) children.elementAt(num);
        if (elementType != null && !(child.getType().equalsIgnoreCase(elementType))) return null;
        return child;
    }

    /** Return desired child of FIRST child of xml. */
    private XMLElement getGrandchild(XMLElement xml, String elementType) {
        return getGrandchild(xml, -1, elementType);
    }

    /** Return numth child of FIRST child of xml,
   *  or, if num is -1, look for appropriate child of FIRST child of xml. */
    private XMLElement getGrandchild(XMLElement xml, int num, String elementType) {
        XMLElement child = getChild(xml, 0, null);
        if (child == null) return null;
        Vector grandchildren = child.getChildren();
        if (grandchildren == null || grandchildren.size() <= num) {
            logger.error("getGrandchild: failed to find children of child " + ChadoXmlUtils.printXMLElement(child));
            return null;
        }
        if (num < 0 && elementType != null) {
            for (int i = 0; i < grandchildren.size(); i++) {
                XMLElement grandchild = (XMLElement) grandchildren.elementAt(i);
                if (grandchild.getType().equalsIgnoreCase(elementType)) return grandchild;
            }
            return null;
        }
        XMLElement grandchild = (XMLElement) grandchildren.elementAt(num);
        if (elementType != null && !(grandchild.getType().equalsIgnoreCase(elementType))) return null;
        return grandchild;
    }

    /** Main method for writing ChadoXML file */
    public void commitChanges(CurationSet curation) {
        commitChanges(curation, true, true);
    }

    /** Main method for writing ChadoXML file */
    public void commitChanges(CurationSet curation, boolean saveAnnots, boolean saveResults) {
        String filename = apollo.util.IOUtil.findFile(getInput(), true);
        if (filename == null) filename = getInput();
        if (filename == null) return;
        String msg = "Retrieving preamble from original file... ";
        fireProgressEvent(new ProgressEvent(this, new Double(5.0), msg));
        String preamble = getPreamble(curation.getInputFilename());
        if (Config.getConfirmOverwrite()) {
            File handle = new File(filename);
            if (handle.exists()) {
                if (!LoadUtil.areYouSure(filename + " already exists--overwrite?")) {
                    apollo.main.DataLoader loader = new apollo.main.DataLoader();
                    loader.saveFileDialog(curation);
                    return;
                }
            }
        }
        setInput(filename);
        msg = "Saving Chado XML to file " + filename + "... ";
        fireProgressEvent(new ProgressEvent(this, new Double(20.0), msg));
        if (ChadoXmlWrite.writeXML(curation, filename, preamble, saveAnnots, saveResults, getNameAdapter(curation), "Apollo version: " + Version.getVersion())) {
            logger.info("Saved Chado XML to " + filename);
            ChadoXmlWrite.saveTransactions(curation, filename);
        } else {
            String message = "Failed to save Chado XML to " + filename;
            logger.error(message);
            JOptionPane.showMessageDialog(null, message, "Warning", JOptionPane.WARNING_MESSAGE);
        }
    }

    private ApolloNameAdapterI getNameAdapter(CurationSet curation) {
        StrandedFeatureSetI annots = curation.getAnnots();
        if (annots == null || annots.size() == 0) return new GmodNameAdapter();
        AnnotatedFeatureI annot = (AnnotatedFeatureI) annots.getFeatureAt(0);
        return (getCurationState().getNameAdapter(annot));
    }

    /** Check whether the input appears to be chadoXML by looking for the <chado> line */
    public boolean appearsToBeChadoXML(String filename, BufferedReader in) throws ApolloAdapterException {
        for (int i = 0; i < 5000; i++) {
            String line = "";
            try {
                line = in.readLine();
            } catch (Exception e) {
                throw new ApolloAdapterException("Error: ChadoXML file " + filename + " is empty.");
            }
            if (line == null) break;
            if (line.toLowerCase().indexOf("<chado") >= 0) return true;
        }
        return false;
    }

    /** Retrieve from filename any lines preceding the <chado> line (except for
   *  the initial <?xml> line) and return as a string. */
    private String getPreamble(String filename) {
        if (filename == null || filename.equals("")) {
            logger.warn("original input filename not set--couldn't retrieve preamble");
            return "";
        }
        logger.debug("Retrieving preamble from " + filename + "...");
        StringBuffer input = new StringBuffer();
        BufferedReader in;
        try {
            if (filename.startsWith("http")) {
                URL url = new URL(filename);
                InputStream stream = apollo.util.IOUtil.getStreamFromUrl(url, "URL " + url + " not found");
                in = new BufferedReader(new InputStreamReader(stream));
            } else in = new BufferedReader(new FileReader(filename));
            input.append("<!-- Header lines that follow were preserved from the input source: " + filename + " -->\n");
            String line = "";
            while ((line = in.readLine()) != null) {
                if (line.toLowerCase().indexOf("<chado") >= 0 && line.indexOf("<!--") < 0) return input.toString();
                if (line.toLowerCase().indexOf("<game>") >= 0 && line.indexOf("<!--") < 0) return input.toString();
                if (line.indexOf("<?xml") >= 0) ; else input.append(line + "\n");
            }
        } catch (Exception exception) {
            logger.warn("failed to retrieve preamble from original input file " + filename);
            return "";
        }
        return "";
    }

    public Properties getStateInformation() {
        StateInformation props = new StateInformation();
        props.put(StateInformation.INPUT_TYPE, getInputType().toString());
        if (getInput() != null) props.put(StateInformation.INPUT_STRING, getInput());
        return props;
    }

    /** DataLoader calls this. input, inputType and optionally database set here.
  MovementPanel(nav bar) sets db to default db (it has no db chooser) */
    public void setStateInformation(Properties props) {
        String typeString = props.getProperty(StateInformation.INPUT_TYPE);
        try {
            DataInputType type = DataInputType.stringToType(typeString);
            String inputString = props.getProperty(StateInformation.INPUT_STRING);
            setDataInput(new DataInput(type, inputString));
        } catch (UnknownTypeException e) {
            logger.error("Cannot set game adapter state info", e);
        }
    }
}
