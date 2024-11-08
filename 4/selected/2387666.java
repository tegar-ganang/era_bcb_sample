package apollo.dataadapter.gamexml;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileReader;
import java.io.File;
import java.io.BufferedReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.Stack;
import java.util.Vector;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;
import org.apache.log4j.*;
import org.bdgp.io.DataAdapterUI;
import org.bdgp.io.IOOperation;
import org.bdgp.util.ProgressEvent;
import org.bdgp.xml.XMLElement;
import apollo.config.ApolloNameAdapterI;
import apollo.config.Config;
import apollo.config.Style;
import apollo.dataadapter.AbstractApolloAdapter;
import apollo.dataadapter.ApolloDataAdapterI;
import apollo.dataadapter.ApolloAdapterException;
import apollo.dataadapter.DataInput;
import apollo.dataadapter.DataInputType;
import apollo.dataadapter.DataInputType.UnknownTypeException;
import apollo.dataadapter.Region;
import apollo.dataadapter.StateInformation;
import apollo.dataadapter.TransactionOutputAdapter;
import apollo.dataadapter.chado.ChadoTransactionTransformer;
import apollo.dataadapter.chadoxml.ChadoTransactionXMLWriter;
import apollo.datamodel.AnnotatedFeature;
import apollo.datamodel.AnnotatedFeatureI;
import apollo.datamodel.Comment;
import apollo.datamodel.CurationSet;
import apollo.datamodel.DbXref;
import apollo.datamodel.Exon;
import apollo.datamodel.ExonI;
import apollo.datamodel.FeaturePair;
import apollo.datamodel.FeaturePairI;
import apollo.datamodel.FeatureSet;
import apollo.datamodel.FeatureSetI;
import apollo.datamodel.Protein;
import apollo.datamodel.RangeI;
import apollo.datamodel.SeqFeature;
import apollo.datamodel.SeqFeatureI;
import apollo.datamodel.Sequence;
import apollo.datamodel.SequenceI;
import apollo.datamodel.StrandedFeatureSet;
import apollo.datamodel.StrandedFeatureSetI;
import apollo.datamodel.Synonym;
import apollo.datamodel.Transcript;
import apollo.datamodel.seq.GAMESequence;
import apollo.editor.Transaction;
import apollo.editor.TransactionManager;
import apollo.main.Version;
import apollo.util.DateUtil;
import apollo.util.FastaHeader;
import apollo.util.HTMLUtil;
import apollo.util.IOUtil;
import apollo.main.*;

/**
 * Reader for GAME XML files.
 *
 * WARNING -- AElfred (and other SAX drivers) _may_ break large
 * stretches of unmarked content into smaller chunks and call
 * characters() for each smaller chunk
 * CURRENT IMPLEMENTATION DOES NOT DEAL WITH THIS
 * COULD CAUSE PROBLEM WHEN READING IN SEQUENCE RESIDUES
 * haven't seen a problem yet though -- GAH 6-15-98
 * GAMEAdapter is presently not a singleton. There is separate instances for synteny
 * and non-synteny/one-species. This may change in future.
 */
public class GAMEAdapter extends AbstractApolloAdapter {

    protected static final Logger logger = LogManager.getLogger(GAMEAdapter.class);

    static String originalFilename = null;

    protected boolean NO_GUI = false;

    String region;

    /** Root of the parse tree for the XML input */
    XMLElement game_element = null;

    int element_count = 0;

    SequenceI curated_seq = null;

    StrandedFeatureSetI analyses = null;

    Hashtable all_analyses = null;

    /** Amount to pad left */
    private int padLeft = -1;

    private int padRight = -1;

    private String NAME_LABEL_FOR_WRITING = "GAME XML format";

    private String NAME_LABEL_FOR_READING = "GAME XML format";

    IOOperation[] supportedOperations = { ApolloDataAdapterI.OP_READ_DATA, ApolloDataAdapterI.OP_WRITE_DATA, ApolloDataAdapterI.OP_READ_SEQUENCE, ApolloDataAdapterI.OP_APPEND_DATA };

    public GAMEAdapter() {
        setInputType(DataInputType.FILE);
        setName(NAME_LABEL_FOR_READING);
    }

    public GAMEAdapter(DataInputType inputType, String input) {
        setInputType(inputType);
        setInput(input);
    }

    public GAMEAdapter(DataInputType inputType, String input, boolean noGUI) {
        NO_GUI = noGUI;
        setInputType(inputType);
        setInput(input);
    }

    public void init() {
    }

    /**
   * org.bdgp.io.DataAdapter method
   */
    public String getType() {
        return "GAME XML source (filename, URL, gene=cact, band=34A, or location=3L:12345-67890)";
    }

    public IOOperation[] getSupportedOperations() {
        return supportedOperations;
    }

    public void setName(String nameForReading) {
        super.setName(nameForReading);
        NAME_LABEL_FOR_READING = nameForReading;
    }

    public DataAdapterUI getUI(IOOperation op) {
        if (!super.operationIsSupported(op)) return null;
        DataAdapterUI ui = super.getCachedUI(op);
        if (ui == null) {
            ui = new GAMEAdapterGUI(op);
            super.cacheUI(op, ui);
        }
        if (op.equals(OP_WRITE_DATA)) super.setName(NAME_LABEL_FOR_WRITING); else super.setName(NAME_LABEL_FOR_READING);
        return ui;
    }

    /** Request to "pad" the input padLeft basepairs to the left(5' forward strand) -
      Could do this in AbstractApolloAdapter but only game adapter needs this now */
    public void setPadLeft(int padLeft) {
        this.padLeft = padLeft;
    }

    /** Request to "pad" the input padRight basepairs to the right(3' forward strand) */
    public void setPadRight(int padRight) {
        this.padRight = padRight;
    }

    private int getPadLeft() {
        if (padLeft == -1) padLeft = getGAMEStyle().getDefaultPadding();
        return padLeft;
    }

    private int getPadRight() {
        if (padRight == -1) padRight = getGAMEStyle().getDefaultPadding();
        return padRight;
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

    public void setRegion(SequenceI seq) throws ApolloAdapterException {
        if (seq != null) {
            this.curated_seq = seq;
            setRegion(seq.getName());
        }
    }

    /** Returns true as game data contains link data that can be used by 
      synteny */
    public boolean hasLinkData() {
        return true;
    }

    /** Save original file name so that if we save the data we can access the
   *  header info from the original file; and also so we can save the file name
   *  as a comment. */
    public void setOriginalFilename(String file) {
        logger.debug("GAMEAdapter.setOriginalFilename: setting original filename = " + file);
        this.originalFilename = file;
    }

    /** Tests all the input types - can enter filename as arg */
    public static void main(String[] args) throws ApolloAdapterException {
        GAMEAdapter databoy;
        String url = "http://www.fruitfly.org/annot/gbunits/xml/AE003650.xml";
        databoy = new GAMEAdapter(DataInputType.URL, url);
        testAdapter(databoy);
        databoy = new GAMEAdapter(DataInputType.GENE, "cact");
        testAdapter(databoy);
        databoy = new GAMEAdapter(DataInputType.CYTOLOGY, "34A");
        testAdapter(databoy);
        databoy = new GAMEAdapter(DataInputType.SCAFFOLD, "AE003490");
        testAdapter(databoy);
        String file = "/users/mgibson/cvs/apollo/dev/sanger/data/josh";
        if (args.length > 0) file = args[0];
        databoy = new GAMEAdapter(DataInputType.FILE, file);
        testAdapter(databoy);
        String seq = "actggcgtgctgtgttattagtgatgatgtcgcaatcgtgaatcgatgcatgcacacatcgtgtgtgtggtctgcgaatatggcattccgtaaagtgccgcgcgtatgtcgcgcgattatgatgtatgctgctgatgtagctgtgatattctaatgagtgctgatcgtgatgtagtcgtagtctagctagctagtcgatcgtagctacgtagctagctagcttgtgtgcgcgcgctg";
        databoy = new GAMEAdapter(DataInputType.SEQUENCE, seq);
        testAdapter(databoy);
    }

    private static void testAdapter(GAMEAdapter databoy) {
        try {
            CurationSet curation = databoy.getCurationSet();
            apollo.dataadapter.debug.DisplayTool.showFeatureSet(curation.getResults());
        } catch (ApolloAdapterException ex) {
            logger.error("No data to read", ex);
        }
    }

    public void commitChanges(CurationSet curation) {
        commitChanges(curation, true, true);
    }

    /**
   * Writes XML
   * If the input type is not FILE, prompts user for a file to save to.
   */
    public void commitChanges(CurationSet curation, boolean saveAnnots, boolean saveResults) {
        if (getInputType() != DataInputType.FILE) {
            apollo.main.DataLoader loader = new apollo.main.DataLoader();
            loader.saveFileDialog(curation);
            return;
        }
        String filename = apollo.util.IOUtil.findFile(getInput(), true);
        if (filename == null) filename = getInput();
        if (filename == null) return;
        if (Config.getConfirmOverwrite()) {
            File handle = new File(filename);
            if (handle.exists()) {
                if (!LoadUtil.areYouSure(filename + " already exists--overwrite?")) {
                    apollo.main.DataLoader loader = new apollo.main.DataLoader();
                    loader.saveFileDialog(curation);
                    return;
                } else {
                    logger.info("GAMEAdapter overwriting existing file " + filename);
                }
            }
        }
        String msg = "Saving data to file " + filename;
        setInput(filename);
        fireProgressEvent(new ProgressEvent(this, new Double(10.0), msg));
        if (GAMESave.writeXML(curation, filename, saveAnnots, saveResults, "Apollo version: " + Version.getVersion(), false)) {
            saveTransactions(curation, filename);
        } else {
            String message = "Failed to save GAME XML to " + filename;
            logger.error(message);
            JOptionPane.showMessageDialog(null, message, "Warning", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void saveTransactions(CurationSet curation, String fileName) {
        if (curation.getTransactionManager() == null) return;
        try {
            curation.getTransactionManager().coalesce();
            if (Config.outputTransactionXML() && !getGAMEStyle().transactionsAreInGameFile()) {
                saveApolloTransactions(curation, fileName);
            }
        } catch (Exception e) {
            logger.error("GAMEAdapter encountered Exception in saveTransactions() (game)", e);
            JOptionPane.showMessageDialog(null, "Apollo Transactions cannot be saved.", "Error", JOptionPane.ERROR_MESSAGE);
        }
        try {
            if (Config.isChadoTnOutputNeeded()) {
                TransactionOutputAdapter output = new ChadoTransactionXMLWriter();
                if (curation.isChromosomeArmUsed()) {
                    output.setMapID(curation.getChromosome());
                    output.setMapType("chromosome_arm");
                } else {
                    output.setMapID(curation.getChromosome());
                    output.setMapType("chromosome");
                }
                output.setTransformer(new ChadoTransactionTransformer());
                output.setTarget(fileName);
                output.commitTransactions(curation.getTransactionManager());
            }
        } catch (Exception e) {
            logger.error("GAMEAdapter encountered Exception in saveTransactions() (chado)", e);
            JOptionPane.showMessageDialog(null, "Chado Transactions cannot be saved.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveApolloTransactions(CurationSet curationSet, String fileName) {
        try {
            TransactionXMLAdapter tnAdapter = new TransactionXMLAdapter();
            tnAdapter.setFileName(fileName);
            tnAdapter.save(curationSet.getTransactionManager().getTransactions());
        } catch (IOException e) {
            logger.error("GAMEAdapter encountered Exception in saveApolloTransactions()", e);
        }
    }

    /** if file starts with http: then its really an url so correct the type 
   bug fix for being able to load urls from file adapter and with -x */
    public DataInput getDataInput() {
        super.getDataInput().setTypeToFileOrUrl();
        return super.getDataInput();
    }

    /** from ApolloDataAdapterI interface. dataInput should be set
   * previous to this
   */
    public CurationSet getCurationSet() throws ApolloAdapterException {
        CurationSet curation = new CurationSet();
        try {
            fireProgressEvent(new ProgressEvent(this, new Double(5.0), "Finding data..."));
            InputStream xml_stream;
            logger.debug("opening GAME XML file " + getDataInput());
            xml_stream = xmlInputStream(getDataInput());
            BufferedInputStream bis = new BufferedInputStream(xml_stream);
            if (!NO_GUI) {
                super.clearOldData();
            }
            fireProgressEvent(new ProgressEvent(this, new Double(10.0), "Reading GAME XML..."));
            XMLParser parser = new XMLParser();
            game_element = parser.readXML(bis);
            if (game_element == null) {
                String msg = "GAME XML input stream was empty--nothing loaded.";
                logger.error(msg);
                throw new ApolloAdapterException(msg);
            }
            xml_stream.close();
            fireProgressEvent(new ProgressEvent(this, new Double(20.0), "Getting genomic sequence..."));
            getFocusSequence(curation);
            if (curated_seq == null) {
                String m = ("Failed to load genomic sequence for the entry.\n" + "One possible cause could be the absence of a 'seq' element with a\n" + "'focus=true' attribute, which is required (even if it has no 'residues').\n");
                logger.error(m);
                throw new ApolloAdapterException(m);
            }
            curation.setRefSequence(curated_seq);
            logger.debug("Set reference seq to " + curated_seq.getName() + ", length = " + curation.getRefSequence().getLength());
            fireProgressEvent(new ProgressEvent(this, new Double(25.0), "Setting genome position..."));
            getGenomePosition(curation);
            fireProgressEvent(new ProgressEvent(this, new Double(30.0), "Reading annotations and results..."));
            getResultsAndAnnots(curation);
            if (getDataInput().isFile()) {
                fireProgressEvent(new ProgressEvent(this, new Double(85.0), "Reading transaction file..."));
                TransactionXMLAdapter.loadTransactions(getInput(), curation, getCurationState());
            }
            fireProgressEvent(new ProgressEvent(this, new Double(90.0), "Cleaning up..."));
            logger.info("Completed XML parse of " + curation.getName());
            parser.clean();
            game_element = null;
            fireProgressEvent(new ProgressEvent(this, new Double(95.0), "Drawing..."));
        } catch (ApolloAdapterException dae) {
            throw dae;
        } catch (Exception ex2) {
            logger.error("GAMEAdapter encountered Exception in getCurationSet()", ex2);
            ;
            throw new ApolloAdapterException(ex2.getMessage());
        }
        curation.setInputFilename(originalFilename);
        return curation;
    }

    /** Called if user is layering additional GAME data on top of whatever's
      already been loaded */
    public Boolean addToCurationSet() throws ApolloAdapterException {
        boolean okay = false;
        try {
            fireProgressEvent(new ProgressEvent(this, new Double(5.0), "Finding data..."));
            InputStream xml_stream;
            xml_stream = xmlInputStream(getDataInput());
            BufferedInputStream bis = new BufferedInputStream(xml_stream);
            fireProgressEvent(new ProgressEvent(this, new Double(10.0), "Reading XML..."));
            XMLParser parser = new XMLParser();
            game_element = parser.readXML(bis);
            if (game_element == null) throw new ApolloAdapterException("GAME XML input stream was empty--nothing loaded.");
            xml_stream.close();
            fireProgressEvent(new ProgressEvent(this, new Double(20.0), "Getting genomic sequence..."));
            getFocusSequence(curation_set);
            curation_set.setRefSequence(curated_seq);
            fireProgressEvent(new ProgressEvent(this, new Double(25.0), "Setting genome position..."));
            getGenomePosition(curation_set);
            fireProgressEvent(new ProgressEvent(this, new Double(30.0), "Setting annotations and results..."));
            getResultsAndAnnots(curation_set);
            if (getDataInput().isFile()) {
                fireProgressEvent(new ProgressEvent(this, new Double(90.0), "Reading transaction file..."));
                TransactionXMLAdapter.loadTransactions(getInput(), curation_set, getCurationState());
            }
            fireProgressEvent(new ProgressEvent(this, new Double(90.0), "Cleaning up..."));
            logger.info("Completed XML parse of " + curation_set.getName());
            parser.clean();
            game_element = null;
            fireProgressEvent(new ProgressEvent(this, new Double(95.0), "Drawing..."));
            okay = true;
        } catch (ApolloAdapterException dae) {
            throw dae;
        } catch (Exception ex2) {
            logger.error("Exception in addToCurationSet", ex2);
            throw new ApolloAdapterException(ex2.getMessage());
        }
        return new Boolean(okay);
    }

    private InputStream xmlInputStream(DataInput dataInput) throws ApolloAdapterException {
        InputStream stream = null;
        DataInputType type = dataInput.getType();
        String input = dataInput.getInputString();
        if (type == DataInputType.FILE) {
            stream = getStreamFromFile(input);
            setOriginalFilename(input);
        } else if (type == DataInputType.URL) {
            URL url = makeUrlFromString(input);
            if (url == null) {
                String message = "Couldn't find URL for " + getInput();
                logger.error(message);
                throw new ApolloAdapterException(message);
            }
            logger.info("Trying to open URL " + input + " to read GAME XML...");
            stream = apollo.util.IOUtil.getStreamFromUrl(url, "URL " + input + " not found");
            setOriginalFilename(url.toString());
        } else if (type == DataInputType.GENE) {
            String err = "Can't connect to URL for request gene=" + input + "--server not responding.";
            String notfound = "Gene " + input + " not found (or server not responding)";
            logger.info("Looking up GAME XML data for gene " + input + "...");
            stream = apollo.util.IOUtil.getStreamFromUrl(getURLForGene(input), err, notfound);
            setOriginalFilename((getURLForGene(input)).toString());
        } else if (type == DataInputType.CYTOLOGY) {
            String err = "Can't connect to URL for band=" + input + "--server not responding.";
            String notfound = "Cytological band " + input + " not found (or server not responding)";
            logger.info("Looking up GAME XML data for band " + input + "...");
            stream = apollo.util.IOUtil.getStreamFromUrl(getURLForBand(input), err, notfound);
            setOriginalFilename((getURLForBand(input)).toString());
        } else if (type == DataInputType.SCAFFOLD) {
            String err = "Can't connect to URL for scaffold=" + input + "--server not responding.";
            String notfound = "Scaffold " + input + " not found (or server not responding)";
            logger.info("Looking up GAME XML data for scaffold " + input + "...");
            stream = apollo.util.IOUtil.getStreamFromUrl(getURLForScaffold(input), err, notfound);
            setOriginalFilename((getURLForScaffold(input)).toString());
        } else if (type == DataInputType.BASEPAIR_RANGE) {
            String err = "Can't connect to URL for requested region--server not responding.";
            String notfound = "Region " + dataInput.getRegion() + " not found (or server not responding)";
            logger.info("Looking up GAME XML data for range " + dataInput.getRegion() + "...");
            stream = apollo.util.IOUtil.getStreamFromUrl(getURLForRange(dataInput.getRegion()), err, notfound);
            setOriginalFilename((getURLForRange(dataInput.getRegion())).toString());
        }
        return stream;
    }

    /** Only files should be sent this way - if an URL is sent here it wont work
      and something is wrong */
    private InputStream getStreamFromFile(String filename) throws ApolloAdapterException {
        InputStream stream = null;
        String path = apollo.util.IOUtil.findFile(filename, false);
        try {
            logger.info("Trying to open GAME XML file " + filename);
            stream = new FileInputStream(path);
        } catch (Exception e) {
            stream = null;
            throw new ApolloAdapterException("Error: could not open GAME XML file " + filename + " for reading.");
        }
        BufferedReader in;
        try {
            in = new BufferedReader(new FileReader(path));
        } catch (Exception e) {
            stream = null;
            throw new ApolloAdapterException("Error: could not open GAME XML file " + path + " for reading.");
        }
        for (int i = 0; i < 12; i++) {
            String line;
            try {
                line = in.readLine();
            } catch (Exception e) {
                in = null;
                throw new ApolloAdapterException("Error: file " + filename + " is empty.");
            }
            if (line == null) {
                in = null;
                throw new ApolloAdapterException("Error: file " + filename + " does not appear to contain GAME XML.");
            }
            if (line.toLowerCase().indexOf("<game") >= 0) {
                in = null;
                return stream;
            }
        }
        in = null;
        throw new ApolloAdapterException("Error: file " + filename + " does not appear to contain GAME XML.");
    }

    /** Retrieve style from config
    *  (Need to do it this way because style hasn't been set yet when we're about
    *  to open a GAME file.) */
    private Style getGAMEStyle() {
        return Config.getStyle(getClass().getName());
    }

    /** make URL from urlString, replace %DATABASE% with selected database */
    public URL makeUrlFromString(String urlString) {
        URL url;
        urlString = fillInDatabase(urlString);
        urlString = fillInPadding(urlString);
        try {
            url = new URL(urlString);
        } catch (MalformedURLException ex) {
            logger.error("caught exception creating URL " + urlString, ex);
            return (null);
        }
        return (url);
    }

    /** Replace %DATABASE% field with selected database.
   *  Note: some of this is FlyBase-specific! */
    public String fillInDatabase(String urlString) {
        String dbField = getGAMEStyle().getDatabaseURLField();
        if (dbField == null) return urlString;
        int index = urlString.indexOf(dbField);
        if (index == -1) {
            return urlString;
        }
        StringBuffer sb = new StringBuffer(urlString);
        String dbname = getDatabase();
        if (dbname.indexOf("ot available") > 0) return "";
        if (dbname.indexOf(" ") > 0) dbname = dbname.substring(0, dbname.indexOf(" "));
        sb.replace(index, index + dbField.length(), dbname);
        return sb.toString();
    }

    public String getDatabase() {
        if (super.getDatabase() != null) {
            return super.getDatabase();
        } else {
            return getGAMEStyle().getDefaultDatabase();
        }
    }

    /** Replace %PadLeft/Right% with pad ints */
    public String fillInPadding(String urlString) {
        StringBuffer sb = new StringBuffer(urlString);
        pad(sb, getGAMEStyle().getPadLeftURLField(), getPadLeft());
        pad(sb, getGAMEStyle().getPadRightURLField(), getPadRight());
        return sb.toString();
    }

    private static void pad(StringBuffer urlBuff, String field, int pad) {
        if (field == null) return;
        int index = urlBuff.indexOf(field);
        if (index == -1) return;
        urlBuff.replace(index, index + field.length(), pad + "");
    }

    private URL getURLForScaffold(String scaffold) {
        String query = getGAMEStyle().getScaffoldUrl() + scaffold;
        URL url = makeUrlFromString(query);
        String msg = "Searching for location of scaffold " + scaffold + "...";
        fireProgressEvent(new ProgressEvent(this, new Double(2.0), msg));
        return (url);
    }

    private URL getURLForGene(String gene) {
        String query = getGAMEStyle().getGeneUrl() + gene;
        URL url = makeUrlFromString(query);
        String msg = "Searching for location of gene " + gene + "...";
        fireProgressEvent(new ProgressEvent(this, new Double(2.0), msg));
        return (url);
    }

    private URL getURLForBand(String band) {
        String query = getGAMEStyle().getBandUrl() + band;
        URL url = makeUrlFromString(query);
        fireProgressEvent(new ProgressEvent(this, new Double(2.0), "Searching for cytological location " + band + "--please be patient"));
        return (url);
    }

    /** parse range string "Chr 2L 10000 20000" -> "2L:10000:20000"   */
    private URL getURLForRange(Region region) {
        String rangeForUrl = region.getColonDashString();
        String query = getGAMEStyle().getRangeUrl() + rangeForUrl;
        URL url = makeUrlFromString(query);
        fireProgressEvent(new ProgressEvent(this, new Double(5), "Searching for region " + rangeForUrl));
        return url;
    }

    /** Creates a gene from annot_element. This can make real genes and
      also put non genes into Gene objects */
    private AnnotatedFeatureI getAnnot(XMLElement annot_element, CurationSet curation, StrandedFeatureSetI annots) {
        AnnotatedFeatureI annot = new AnnotatedFeature();
        annot.setId(annot_element.getID());
        if ((annot_element.getAttribute("problem") != null) && (annot_element.getAttribute("problem").equals("true"))) {
            annot.setIsProblematic(true);
        }
        String nickname = null;
        String desc = null;
        Vector elements = annot_element.getChildren();
        for (int i = 0; i < elements.size(); i++) {
            XMLElement element = (XMLElement) elements.elementAt(i);
            if (element.getType().equals("name")) {
                if (element.getCharData() != null) annot.setName(element.getCharData());
            } else if (element.getType().equals("type")) {
                annot.setTopLevelType(element.getCharData());
                annot.setFeatureType(element.getCharData());
            } else if (element.getType().equals("author")) {
                annot.setOwner(element.getCharData());
            } else if (element.getType().equals("synonym") || element.getType().equals("nickname")) {
                Synonym syn = getSynonym(element);
                annot.addSynonym(syn);
                nickname = element.getCharData();
            } else if (element.getType().equals("comment")) {
                setComment(element, annot);
            } else if (element.getType().equalsIgnoreCase("gene")) {
                setGeneInfo(element, annot);
            } else if (element.getType().equals("description")) {
                annot.setDescription(element.getCharData());
                desc = element.getCharData();
            } else if (element.getType().equals("dbxref")) {
                setAnnotXref(element, annot);
            } else if (element.getType().equals("aspect")) {
                XMLElement dbx = (XMLElement) element.getChildren().elementAt(0);
                setAnnotXref(dbx, annot);
            } else if (element.getType().equals("feature_set")) {
                Transcript transcript = getTranscript(element, annot, curation);
                annot.addFeature(transcript);
                String id = transcript.getId();
                if (id == null || id.length() == 0 || id.startsWith("feature_set:")) {
                    ApolloNameAdapterI nameAdapter = getCurationState().getNameAdapter(annot);
                    id = nameAdapter.generateId(annots, curation.getName(), transcript);
                }
                transcript.setId(id);
            } else if (element.getType().equals("property")) {
                String type_tag = getTypeTag(element);
                if (type_tag.equals("internal_synonym")) {
                    String value = getTypeValue(element);
                    Synonym syn = new Synonym(value);
                    syn.addProperty("is_internal", "1");
                    annot.addSynonym(syn);
                } else addOutput(annot, element);
            } else if (GAMESave.isOneLevelAnnot(annot) && element.getType().equals("seq_relationship")) {
                setRange(element, annot, curation.getRefSequence(), curation.getStart() - 1);
            }
        }
        if (annot.getName() == null) {
            if (nickname != null) annot.setName(nickname); else if (desc != null) annot.setName(desc); else annot.setName("");
        }
        repairGreek(annot);
        return annot;
    }

    private Synonym getSynonym(XMLElement synElement) {
        String name = synElement.getCharData();
        Synonym syn = new Synonym(name);
        if (synElement.hasAttribute("owner")) syn.setOwner(synElement.getAttribute("owner"));
        return syn;
    }

    private void setComment(XMLElement comment_element, AnnotatedFeatureI sf) {
        String text = null;
        String person = null;
        Date date = null;
        Vector elements = comment_element.getChildren();
        for (int i = 0; i < elements.size(); i++) {
            XMLElement element = (XMLElement) elements.elementAt(i);
            if (element.getType().equals("text")) {
                text = element.getCharData();
            } else if (element.getType().equals("person")) {
                person = element.getCharData();
            } else if (element.getType().equals("date")) {
                date = DateUtil.makeADate(element.getCharData());
            }
        }
        if (text != null) {
            Comment comment = new Comment();
            comment.setText(text);
            sf.addComment(comment);
            if (comment_element.getAttribute("id") != null) {
                comment.setId(comment_element.getAttribute("id"));
            }
            if ((comment_element.getAttribute("internal") != null && comment_element.getAttribute("internal").equals("true")) || (text.indexOf("nternal view only") >= 0)) {
                comment.setIsInternal(true);
            }
            String clue = "no atg translation start identified";
            if (text.toLowerCase().indexOf(clue) >= 0) {
                ((FeatureSetI) sf).setMissing5prime(true);
            }
            if (person != null) comment.setPerson(person);
            if (date != null) comment.setTimeStamp(date.getTime());
        }
    }

    private Transcript getTranscript(XMLElement transcript_element, AnnotatedFeatureI gene, CurationSet curation) {
        Transcript transcript = new Transcript();
        transcript.setId(transcript_element.getID());
        int minus1_frameshift = 0;
        int plus1_frameshift = 0;
        String readthrough_stop = null;
        if ((transcript_element.getAttribute("problem") != null) && (transcript_element.getAttribute("problem").equals("true"))) {
            transcript.setIsProblematic(true);
        }
        String name = null;
        String desc = null;
        SeqFeatureI translateStart = null;
        SeqFeatureI prot = null;
        Vector elements = transcript_element.getChildren();
        for (int i = 0; i < elements.size(); i++) {
            XMLElement element = (XMLElement) elements.elementAt(i);
            if (element.getType().equals("name")) {
                if (element.getCharData() != null) {
                    name = element.getCharData();
                    transcript.setName(name);
                }
            } else if (element.getType().equals("type")) {
            } else if (element.getType().equals("synonym")) {
                transcript.addSynonym(getSynonym(element));
            } else if (element.getType().equals("comment")) {
                setComment(element, transcript);
            } else if (element.getType().equals("author")) {
                transcript.setOwner(element.getCharData());
            } else if (element.getType().equals("date")) {
                Date date = DateUtil.makeADate(element.getCharData());
                if (date != null) transcript.addProperty("date", date.toString()); else transcript.addProperty("date", element.getCharData());
            } else if (element.getType().equals("description")) {
                desc = element.getCharData();
                transcript.setDescription(desc);
            } else if (element.getType().equals("seq_relationship")) {
                setRange(element, transcript, curation.getRefSequence(), curation.getStart() - 1);
                gene.setStrand(transcript.getStrand());
            } else if (element.getType().equals("evidence")) {
                addEvidence(transcript, element);
            } else if (element.getType().equals("seq")) {
                addTranscriptSequence(curation, transcript, element);
            } else if (element.getType().equals("output") || element.getType().equals("property")) {
                String type_tag = getTypeTag(element);
                if (type_tag.equals("plus_1_translational_frame_shift") || type_tag.equals("plus1_translational_frameshift")) {
                    String value = getTypeValue(element);
                    plus1_frameshift = Integer.parseInt(value);
                } else if (type_tag.equals("minus_1_translational_frame_shift") || type_tag.equals("minus1_translational_frameshift")) {
                    String value = getTypeValue(element);
                    minus1_frameshift = Integer.parseInt(value);
                } else if (type_tag.equalsIgnoreCase("stop_codon_redefinition_as_selenocysteine")) {
                    String value = getTypeValue(element);
                    if (value.toLowerCase().startsWith("t") || value.equalsIgnoreCase("U")) readthrough_stop = "U";
                } else if (type_tag.equals("readthrough_stop_codon")) {
                    readthrough_stop = getTypeValue(element);
                } else if (type_tag.equals("internal_synonym")) {
                    String value = getTypeValue(element);
                    Synonym syn = new Synonym(value);
                    syn.addProperty("is_internal", "1");
                    transcript.addSynonym(syn);
                } else addOutput(transcript, element);
            } else if (element.getType().equals("feature_span")) {
                SeqFeatureI featSpan = addTranscriptFeatSpan(element, gene, transcript, curation);
                if (isTranslationStart(featSpan)) translateStart = featSpan;
                if (featSpan.isProtein()) prot = featSpan;
            } else {
                if (element.getCharData() != null && !(element.getCharData().equals(""))) logger.warn(transcript_element.getType() + ": Either intentionally ignoring or " + "inadvertently forgetting to parse " + element.getType() + "=" + element.getCharData());
            }
        }
        if (translateStart != null && gene.isProteinCodingGene()) {
            if (transcript.getProperty("missing_start_codon").equals("true")) {
                transcript.calcTranslationStartForLongestPeptide();
            } else {
                boolean foundTranslationStart = transcript.setTranslationStart(translateStart.getStart(), true);
                if (!foundTranslationStart) {
                    transcript.calcTranslationStartForLongestPeptide();
                } else if (transcript.isTransSpliced()) {
                    transcript.sortTransSpliced();
                }
            }
            setProteinNameAndId(transcript);
        }
        transcript.setPlus1FrameShiftPosition(plus1_frameshift);
        transcript.setMinus1FrameShiftPosition(minus1_frameshift);
        if (readthrough_stop != null) transcript.setReadThroughStop(readthrough_stop);
        if (name == null) {
            if (desc != null) transcript.setName(desc); else transcript.setName("");
        }
        return transcript;
    }

    /** Return true if span is either a protein(1.1)
      or a translation start seq feat(1.0) */
    private boolean isTranslationStart(SeqFeatureI span) {
        if (span.isProtein()) return true;
        return span.getFeatureType().matches("start_codon|translate offset");
    }

    /** generate name & id if not set above from game prot 1.1 */
    private void setProteinNameAndId(Transcript transcript) {
        Protein prot = transcript.getProteinFeat();
        if (prot.hasName() && !prot.hasId()) prot.setId(prot.getName()); else if (prot.hasId() && !prot.hasName()) prot.setName(prot.getId());
        if (!prot.hasName() && !prot.hasId()) {
            ApolloNameAdapterI na = getNameAdapter(transcript);
            String name = na.generatePeptideNameFromTranscriptName(transcript.getName());
            String id = na.generatePeptideIdFromTranscriptId(transcript.getId());
            prot.setName(name);
            prot.setId(id);
        }
    }

    private String setRange(XMLElement range_element, SeqFeatureI feat, SequenceI refSeq, int offset) {
        String align_str = null;
        if (refSeq != null) feat.setRefSequence(refSeq);
        Vector elements = range_element.getChildren();
        XMLElement span_element = null;
        for (int i = 0; i < elements.size(); i++) {
            XMLElement element = (XMLElement) elements.elementAt(i);
            if (element.getType().equals("span")) {
                span_element = element;
            } else if (element.getType().equals("alignment")) {
                align_str = element.getCharData();
            }
        }
        if (span_element == null) {
            logger.error("XML is messed up, span element is missing");
            return align_str;
        }
        setSpan(span_element, feat, offset);
        return align_str;
    }

    /** GAME spans are relative to the GAME map. Apollo SeqFeature have absolute
      coordinates NOT cur set relative. The offset(cur set start) gets added to
      each feature to compensate for this. */
    private void setSpan(XMLElement span_element, RangeI feat, int offset) {
        int start = 0;
        int end = 0;
        Vector elements = span_element.getChildren();
        for (int i = 0; i < elements.size(); i++) {
            XMLElement element = (XMLElement) elements.elementAt(i);
            if (element.getType().equals("start")) {
                start = Integer.parseInt(element.getCharData());
            } else if (element.getType().equals("end")) {
                end = Integer.parseInt(element.getCharData());
            }
        }
        int strand = (start < end) ? 1 : -1;
        feat.setStrand(strand);
        feat.setStart(start + offset);
        feat.setEnd(end + offset);
    }

    private SequenceI getSpanSequence(XMLElement range_element, CurationSet curation) {
        SequenceI seq = null;
        String seq_id = range_element.getAttribute("seq");
        if (seq_id == null) seq_id = range_element.getAttribute("id");
        if (seq_id != null) {
            seq = createSequence(seq_id, curation, true);
        }
        return seq;
    }

    private void addEvidence(AnnotatedFeatureI ga, XMLElement ev_element) {
        String result_id = ev_element.getAttribute("id");
        if (result_id == null || result_id.equals("")) {
            result_id = ev_element.getAttribute("result");
        }
        if (result_id == null || result_id.equals("")) {
            result_id = ev_element.getAttribute("result_id");
        }
        if (result_id != null && !result_id.equals("")) {
            ga.addEvidence(result_id);
        }
    }

    /** Transcripts have feature_spans elements which can be exons, proteins, or
      translation_start - "add" isnt quit the right concept - gleen? */
    private SeqFeatureI addTranscriptFeatSpan(XMLElement spanElement, AnnotatedFeatureI gene, Transcript transcript, CurationSet curation) {
        SeqFeatureI span = new SeqFeature();
        ExonI exon = null;
        if (spanElement.getAttribute("type") != null) span.setFeatureType(spanElement.getAttribute("type"));
        span.setId(spanElement.getID());
        boolean typeIsNotInGame = true;
        while (spanElement.numChildren() > 0) {
            XMLElement element = spanElement.popChild();
            if (element.getType().equals("type")) {
                String type = element.getCharData();
                if (type.equals("exon") || type.equals("gene")) {
                    exon = new Exon(span);
                    span = exon;
                    if (type.equals("gene")) {
                        logger.error("Error in game file - exon with type 'gene'");
                    }
                } else if (type.equals("polypeptide")) span = new Protein(span, transcript);
                span.setFeatureType(type);
                typeIsNotInGame = false;
            } else if (element.getType().equals("seq_relationship")) {
                setRange(element, span, curation.getRefSequence(), curation.getStart() - 1);
                transcript.setStrand(span.getStrand());
                gene.setStrand(span.getStrand());
            } else if (element.getType().equals("evidence") && span.hasAnnotatedFeature()) {
                addEvidence(span.getAnnotatedFeature(), element);
            } else if (element.getType().equals("name")) {
                span.setName(element.getCharData());
            } else {
                String data = element.getCharData();
                if (data != null && !data.equals("")) {
                    String m = spanElement.getType() + ": Either intentionally ignoring or " + "inadvertently forgetting to parse span type " + element.getType() + "=" + data;
                    logger.warn(m);
                }
            }
        }
        if (typeIsNotInGame) {
            exon = new Exon(span);
            span = exon;
            logger.debug("GAME transcript feature_span has no type. setting to 'exon'");
        }
        if (span.isExon()) {
            addExon(exon, transcript, gene, curation);
        }
        return span;
    }

    private void addExon(ExonI exon, Transcript transcript, AnnotatedFeatureI gene, CurationSet curation) {
        transcript.addExon(exon);
        String id = gene.getId() + ":" + exon.getStart() + "-" + exon.getEnd();
        exon.setId(id);
        if (exon.getName() == null || exon.getName().equals("")) {
            ApolloNameAdapterI nameAdapter = getCurationState().getNameAdapter(gene);
            nameAdapter.generateName(curation.getAnnots(), curation.getName(), exon);
        }
    }

    /** Loop through children of root XML element game_element. For those of type
      "computational_analysis", create FeatureSets for each strand and add them
      to a newly created StrandedFeatureSet which is saved in the curation set.
      Children of type "annotation" are saved as annotations. */
    private void getResultsAndAnnots(CurationSet curation) throws ApolloAdapterException {
        StrandedFeatureSetI annotations = curation.getAnnots();
        if (annotations == null) {
            annotations = new StrandedFeatureSet(new FeatureSet(), new FeatureSet());
            annotations.setName("Annotations");
            annotations.setFeatureType("Annotation");
            curation.setAnnots(annotations);
        }
        analyses = curation.getResults();
        if (analyses == null) {
            analyses = new StrandedFeatureSet(new FeatureSet(), new FeatureSet());
            analyses.setName("Analyses");
            curation.setResults(analyses);
            all_analyses = new Hashtable();
        }
        int count = 0;
        int total = game_element.numChildren();
        while (game_element.numChildren() > 0) {
            try {
                ++count;
                fireProgressEvent(new ProgressEvent(this, new Double(30.0 + ((double) count / (double) total) * 55.0), "Parsing XML element #" + count));
                XMLElement element = game_element.popChild();
                if (element.getType().equals("computational_analysis")) {
                    addAnalysis(element, analyses, curation, all_analyses);
                } else if (element.getType().equalsIgnoreCase("annotation")) {
                    AnnotatedFeatureI annot = getAnnot(element, curation, annotations);
                    annotations.addFeature(annot);
                } else if (element.getType().equals("seq")) {
                    getSequence(element, curation);
                } else if (element.getType().equals("map_position")) {
                } else if (element.getType().equals("gameTransactions") || element.getType().equals("apolloTransactions")) {
                    getTransactions(element, curation);
                } else {
                    logger.warn("Warning--don't know how to handle xml element of type " + element.getType());
                }
            } catch (ApolloAdapterException dae) {
                throw dae;
            } catch (Exception ex2) {
                logger.error("Caught exception while parsing XML", ex2);
                throw new ApolloAdapterException(ex2.getMessage());
            }
        }
    }

    /** Create forward and reverse strand feature sets for analysis of analysis element,
      which are added to analyses. Add all sub features to the appropriate stranded
      FeatureSet. */
    private void addAnalysis(XMLElement analysis_element, StrandedFeatureSetI analyses, CurationSet curation, Hashtable all_analyses) {
        String analysis_id = analysis_element.getID();
        String prog = "";
        String db = "";
        String type = null;
        String date = null;
        String version = null;
        Vector elements = analysis_element.getChildren();
        for (int i = 0; i < elements.size(); i++) {
            XMLElement element = (XMLElement) elements.elementAt(i);
            if (element.getType().equals("program")) {
                prog = element.getCharData();
            } else if (element.getType().equals("database")) {
                db = element.getCharData();
            } else if (element.getType().equals("type")) {
                type = element.getCharData();
            } else if (element.getType().equals("date")) {
                date = element.getCharData();
            } else if (element.getType().equals("version")) {
                version = element.getCharData();
            }
        }
        FeatureSetI forward_analysis = initAnalysis(analyses, 1, prog, db, analysis_id, type, date, version, all_analyses);
        FeatureSetI reverse_analysis = initAnalysis(analyses, -1, prog, db, analysis_id, type, date, version, all_analyses);
        String analysis_type = getAnalysisType(prog, db);
        for (int i = 0; i < elements.size(); i++) {
            XMLElement element = (XMLElement) elements.elementAt(i);
            if (element.getType().equals("result_set")) {
                FeatureSetI result = getResult(element, analysis_type, curation);
                result.setProgramName(prog);
                result.setDatabase(db);
                if (result.getStrand() == 1) {
                    forward_analysis.addFeature(result);
                    if (!forward_analysis.hasFeatureType()) forward_analysis.setFeatureType(result.getFeatureType());
                } else {
                    reverse_analysis.addFeature(result);
                    if (!reverse_analysis.hasFeatureType()) {
                        logger.info("Setting analysis type to " + result.getFeatureType());
                        reverse_analysis.setFeatureType(result.getFeatureType());
                    }
                }
            } else if (element.getType().equals("property")) {
                addOutput(forward_analysis, element);
                addOutput(reverse_analysis, element);
            } else if (element.getType().equals("result_span")) {
                SeqFeatureI span = getSpan(element, analysis_type, curation);
                span.setProgramName(prog);
                span.setDatabase(db);
                if (span.getStrand() == 1) {
                    forward_analysis.addFeature(span);
                    if (!forward_analysis.hasFeatureType()) forward_analysis.setFeatureType(span.getFeatureType());
                } else {
                    reverse_analysis.addFeature(span);
                    if (!reverse_analysis.hasFeatureType()) reverse_analysis.setFeatureType(span.getFeatureType());
                }
            }
        }
        boolean fwd = addAnalysisIfHasFeatures(forward_analysis, all_analyses, analyses);
        boolean rev = addAnalysisIfHasFeatures(reverse_analysis, all_analyses, analyses);
        if (fwd && rev) {
            forward_analysis.setAnalogousOppositeStrandFeature(reverse_analysis);
            reverse_analysis.setAnalogousOppositeStrandFeature(forward_analysis);
        }
    }

    private String getAnalysisType(String prog, String db) {
        String analysis_type;
        if (!(prog == null) && !prog.equals("") && !(db == null) && !db.equals("")) analysis_type = prog + ":" + db; else if (!(prog == null) && !prog.equals("")) analysis_type = prog; else if (!(db == null) && !db.equals("")) analysis_type = db; else analysis_type = RangeI.NO_TYPE;
        return analysis_type;
    }

    /** Add in stranded analyses only if features were found on that strand
      otherwise its scrapped (not added to all_analyses and analyses) 
      and false is returned */
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

    /** Creates a new FeatureSet for the analysis on strand. If an analysis with prog,db, 
  and strand has already been created, the existing one is returned. If new a
  FeatureSet is created, added to all_analyses hash, added to analyses, and returned. */
    private FeatureSetI initAnalysis(StrandedFeatureSetI analyses, int strand, String prog, String db, String analysis_id, String type, String date, String version, Hashtable all_analyses) {
        String analysis_type = getAnalysisType(prog, db);
        String analysis_name;
        if (!analysis_type.equals(RangeI.NO_TYPE)) analysis_name = (analysis_type + (strand == 1 ? "-plus" : (strand == -1 ? "-minus" : ""))); else analysis_name = (RangeI.NO_NAME + (strand == 1 ? "-plus" : (strand == -1 ? "-minus" : "")));
        if (all_analyses == null) all_analyses = new Hashtable();
        FeatureSetI analysis = (FeatureSetI) all_analyses.get(analysis_name);
        if (analysis == null) {
            analysis = new FeatureSet();
            analysis.setId(analysis_id);
            analysis.setProgramName(prog);
            analysis.setDatabase(db);
            analysis.setFeatureType(analysis_type);
            if (!analysis_type.equals(RangeI.NO_TYPE)) analysis.setName(analysis_name);
            analysis.setStrand(strand);
            if (type != null) analysis.setTopLevelType(type);
            if (date != null) analysis.addProperty("date", date);
            if (version != null) analysis.addProperty("version", version);
        }
        return analysis;
    }

    private FeatureSetI getResult(XMLElement result_element, String analysis_type, CurationSet curation) {
        FeatureSetI result = new FeatureSet();
        result.setId(result_element.getID());
        result.setFeatureType(analysis_type);
        result.setName("");
        SeqFeatureI translate_start = null;
        Vector elements = result_element.getChildren();
        for (int i = 0; i < elements.size(); i++) {
            XMLElement element = (XMLElement) elements.elementAt(i);
            if (element.getType().equals("score")) {
                setScore(element, result);
            } else if (element.getType().equals("name")) {
                if (element.getCharData() == null) logger.warn("Hey, " + result.getFeatureType() + " id " + result.getId() + " has empty name element"); else result.setName(element.getCharData());
            } else if (element.getType().equals("type")) {
                result.setTopLevelType(element.getCharData());
            } else if (element.getType().equals("output")) {
                addOutput(result, element);
            } else if (element.getType().equals("seq_relationship")) {
                String rel_type = element.getAttribute("type");
                if (rel_type.equals("sbjct") || rel_type.equals("subject")) {
                    SequenceI seq = getSpanSequence(element, curation);
                    result.setHitSequence(seq);
                    if (seq.getName() == null || seq.getName().equals("")) {
                        try {
                            throw (new Exception("Missing id for seq " + seq.getAccessionNo()));
                        } catch (Exception e) {
                            logger.error("Exception getting accessionNo of SpanSequence", e);
                            return null;
                        }
                    }
                    if (result.getName().equals("")) result.setName(seq.getName());
                } else {
                    setRange(element, result, curation.getRefSequence(), curation.getStart() - 1);
                }
            } else if (element.getType().equals("result_set")) {
                FeatureSetI span = getResult(element, analysis_type, curation);
                addToResult(result, span);
            } else if (element.getType().equals("result_span")) {
                SeqFeatureI span = getSpan(element, analysis_type, curation);
                if (span.getTopLevelType().equals("") || (!span.getTopLevelType().equals("translate offset") && !span.getTopLevelType().equals("start codon"))) {
                    addToResult(result, span);
                } else {
                    translate_start = span;
                }
            } else {
                if (element.getCharData() != null && !(element.getCharData().equals(""))) logger.warn(result_element.getType() + ": Either intentionally ignoring or " + "inadvertently forgetting to parse " + element.getType() + (element.getCharData() == null ? "" : "=" + element.getCharData()));
            }
        }
        if (translate_start != null) {
            result.sort(result.getStrand());
            result.setProteinCodingGene(true);
            if (!result.setTranslationStart(translate_start.getStart(), true)) logger.error("unable to set translation start of " + result.toString());
        }
        sniffOutInsertionSite(result);
        return result;
    }

    private void addToResult(FeatureSetI result, SeqFeatureI span) {
        if (span.getStrand() != result.getStrand() && result.getStrand() == 0) {
            result.setStrand(span.getStrand());
        }
        if (!result.hasFeatureType()) {
            result.setFeatureType(span.getTopLevelType());
        }
        if (span.getStrand() == result.getStrand()) {
            if (result.getName().equals("") || result.getName().equals(RangeI.NO_NAME)) result.setName(span.getName()); else if (span.getName().equals("")) {
                span.setName((result.getName().equals("") ? result.getId() : result.getName()) + " span " + result.size());
            }
            if (result.getHitSequence() == null && (span instanceof FeaturePairI)) result.setHitSequence(((FeaturePairI) span).getHitSequence());
            result.addFeature(span);
        } else {
            logger.warn("Ignored result_span " + span.getName() + " of type " + span.getTopLevelType() + ": " + span.getStart() + "-" + span.getEnd() + " because span_strand " + span.getStrand() + " doesn't match result strand " + result.getStrand());
        }
    }

    /** Creates and returns a SeqFeature or a FeaturePair for the span XMLElement -
      if we have a seq_relationship then its a FeaturePair */
    private SeqFeatureI getSpan(XMLElement span_element, String analysis_type, CurationSet curation) {
        SeqFeatureI query = new SeqFeature();
        SeqFeatureI hit = new SeqFeature();
        SeqFeatureI span = null;
        query.setName("");
        hit.setName("");
        query.setFeatureType(analysis_type);
        hit.setFeatureType(analysis_type);
        query.setTopLevelType(analysis_type);
        hit.setTopLevelType(analysis_type);
        query.setId(span_element.getID());
        hit.setId(span_element.getID());
        String query_str = null;
        String sbjct_str = null;
        Vector elements = span_element.getChildren();
        for (int i = 0; i < elements.size(); i++) {
            XMLElement element = (XMLElement) elements.elementAt(i);
            if (element.getType().equals("type")) {
                if (!element.getCharData().equals(analysis_type)) query.setTopLevelType(analysis_type);
                if (element.getCharData().equals("start codon")) query.setTopLevelType("start codon");
                hit.setTopLevelType(analysis_type);
            } else if (element.getType().equals("name")) {
                query.setName(element.getCharData());
                hit.setName(element.getCharData());
            } else if (element.getType().equals("score")) {
                setScore(element, query);
                setScore(element, hit);
            } else if (element.getType().equals("output")) {
                String type_tag = getTypeTag(element);
                if (type_tag.equals("cigar")) {
                    span = new FeaturePair(query, hit);
                    setTagValue(span, element, type_tag);
                } else setTagValue(query, element, type_tag);
            } else if (element.getType().equals("seq_relationship")) {
                String rel_type = element.getAttribute("type");
                if (rel_type.equals("sbjct") || rel_type.equals("subject")) {
                    if (span == null) {
                        span = new FeaturePair(query, hit);
                    }
                    sbjct_str = setRange(element, hit, getSpanSequence(element, curation), 0);
                    hit.setName(hit.getRefSequence().getName());
                    query.addProperty("description", hit.getRefSequence().getDescription());
                    query.setName(hit.getName());
                    if (sbjct_str != null && !sbjct_str.equals("")) {
                        hit.setExplicitAlignment(sbjct_str);
                    }
                } else {
                    query_str = setRange(element, query, curation.getRefSequence(), curation.getStart() - 1);
                    if (query_str != null && !query_str.equals("")) {
                        if (span == null) span = new FeaturePair(query, hit);
                        span.setExplicitAlignment(query_str);
                    }
                }
            } else {
                if (element.getCharData() != null && !(element.getCharData().equals(""))) logger.warn(span_element.getType() + ": Either intentionally ignoring or " + "inadvertently forgetting to parse span type " + element.getType() + (element.getCharData() == null ? "" : "=" + element.getCharData()));
            }
        }
        if (span == null) span = query;
        return span;
    }

    /** Find the focus (genomic) sequence and use it to set the current region. */
    private void getFocusSequence(CurationSet curation) throws ApolloAdapterException {
        Vector elements = game_element.getChildren();
        boolean found = false;
        for (int i = 0; i < elements.size(); i++) {
            XMLElement seq_element = (XMLElement) elements.elementAt(i);
            if (seq_element.getType().equals("seq")) {
                SequenceI seq;
                String focus = seq_element.getAttribute("focus");
                if (focus != null && focus.equals("true")) {
                    if (found) logger.warn("Found duplicate focus sequence " + seq_element.getID());
                    seq = new GAMESequence(seq_element.getID(), Config.getController(), "");
                    seq.setAccessionNo(seq_element.getID());
                    seq.setResidueType(SequenceI.DNA);
                    curation.addSequence(seq);
                    curation.setName(seq.getName());
                    this.setRegion(seq);
                    found = true;
                    parseSequence(seq, seq_element);
                }
            }
        }
    }

    /** We've already handled the focus (genomic) sequence, so don't deal with it here. */
    private void getSequence(XMLElement seq_element, CurationSet curation) throws ApolloAdapterException {
        SequenceI seq;
        String focus = seq_element.getAttribute("focus");
        if (focus != null && focus.equals("true")) {
            return;
        }
        seq = createSequence(seq_element.getID(), curation, false);
        parseSequence(seq, seq_element);
    }

    /** Finish filling in the fields in seq */
    private void parseSequence(SequenceI seq, XMLElement seq_element) {
        String lengthString = seq_element.getAttribute("length");
        int length = 0;
        if (lengthString != null) {
            length = Integer.parseInt(lengthString);
            seq.setLength(Integer.parseInt(lengthString));
        }
        String checksum = seq_element.getAttribute("md5checksum");
        if (checksum != null && !checksum.equals("")) {
            seq.setChecksum(checksum);
        }
        Vector seq_elements = seq_element.getChildren();
        for (int j = 0; j < seq_elements.size(); j++) {
            XMLElement element = (XMLElement) seq_elements.elementAt(j);
            if (element.getType().equals("name")) {
                FastaHeader fasta = new FastaHeader(element.getCharData());
                seq.setName(fasta.getSeqId());
            } else if (element.getType().equals("description")) {
                setSeqDescription(seq, element.getCharData(), seq_element.getID());
            } else if (element.getType().equals("dbxref")) {
                addSeqXref(element, seq);
            } else if (element.getType().equals("residues")) {
                seq.setResidues(element.getCharData());
            } else if (element.getType().equals("organism")) {
                seq.setOrganism(element.getCharData());
            } else if (element.getType().equals("potential_sequencing_error")) {
                addSequenceEdit(element, seq);
            } else {
                logger.warn("Not dealing with seq element " + element.getType());
            }
        }
    }

    private void addSeqXref(XMLElement dbxref_element, SequenceI seq) {
        Vector elements = dbxref_element.getChildren();
        String db = null;
        String acc = null;
        for (int i = 0; i < elements.size(); i++) {
            XMLElement element = (XMLElement) elements.elementAt(i);
            if (element.getType().equals("xref_db")) {
                db = element.getCharData();
            }
            if (element.getType().equals("db_xref_id")) {
                acc = element.getCharData();
            }
            seq.addDbXref(db, acc);
        }
    }

    /** Given a FASTA-style header line, set the sequence's description and
   * extract the date.
   * For fly sequences, the expected format of the header line is
   * >gi||gb|AB003910|AB003910 Fruitfly DNA for 88F actin, complete cds. (08-JUN-1999)
   * Date is that thing at the end, obviously.
   * Another format we see sometimes is
   * RE70410.5prime BI486922 [similar by BLASTN (3.2e-110) to l(3)82Fd "FBan0010199 GO:[]  located on: 3R 82F8-82F10;" 05/17/2001]
   * Using the expected dateFormat (defined in DateUtil)
   * parse the date string and return it as a Date object.
   */
    public static void setSeqDescription(SequenceI seq, String description, String seq_id) {
        if (description != null && description.startsWith(seq_id)) description = (seq_id.length() < description.length() ? description.substring(seq_id.length()) : "");
        String current_desc = seq.getDescription();
        if (current_desc != null) description = current_desc + " " + description;
        seq.setDescription(description);
        Date date = null;
        if (description != null && description.length() > 5) {
            int index = (description.trim()).lastIndexOf(' ');
            if (index > 0) {
                String possibleDate = (description.substring(index)).trim();
                if (possibleDate.indexOf("]") > 0) possibleDate = possibleDate.substring(possibleDate.indexOf("]") + 1);
                if (possibleDate.indexOf("(") >= 0) possibleDate = possibleDate.substring(possibleDate.indexOf("(") + 1);
                if (possibleDate.indexOf(")") >= 0) possibleDate = possibleDate.substring(0, possibleDate.indexOf(")"));
                date = DateUtil.makeADate(possibleDate);
            }
        }
        seq.setDate(date);
    }

    private void addSequenceEdit(XMLElement edit_element, SequenceI seq) {
        Vector elements = edit_element.getChildren();
        String edit_type = null;
        int position = 0;
        String base = null;
        for (int i = 0; i < elements.size(); i++) {
            XMLElement element = (XMLElement) elements.elementAt(i);
            if (element.getType().equals("type")) {
                edit_type = element.getCharData();
            }
            if (element.getType().equals("position")) {
                position = Integer.parseInt(element.getCharData());
            }
            if (element.getType().equals("base")) {
                base = element.getCharData();
            }
        }
        if (edit_type != null && position > 0) seq.addSequencingErrorPosition(edit_type, position, base);
    }

    private void getGenomePosition(CurationSet curation) throws ApolloAdapterException {
        int curation_strand = 1;
        String seq_id = null;
        int start = 0;
        int end = 0;
        Vector elements = game_element.getChildren();
        for (int i = 0; i < elements.size(); i++) {
            XMLElement map_element = (XMLElement) elements.elementAt(i);
            if (map_element.getType().equals("map_position")) {
                if (map_element.getAttribute("type") != null) {
                    curation.setFeatureType(map_element.getAttribute("type"));
                }
                if (map_element.getAttribute("seq") != null) {
                    seq_id = map_element.getAttribute("seq");
                    if (!seq_id.equals(curation.getName())) logger.warn("getGenomePosition: map position is not for " + seq_id + ", but is for " + curation.getName() + "??");
                }
                Vector map_elements = map_element.getChildren();
                for (int j = 0; j < map_elements.size(); j++) {
                    XMLElement element = (XMLElement) map_elements.elementAt(j);
                    if (element.getType().equals("arm")) {
                        String arm = element.getCharData();
                        seq_id = arm;
                        String chromosome = arm;
                        int dot_index = chromosome.indexOf(".");
                        if (dot_index != -1) chromosome = chromosome.substring(0, dot_index);
                        curation.setChromosome(chromosome);
                    } else if (element.getType().equals("chromosome")) {
                        curation.setChromosome(element.getCharData());
                    } else if (element.getType().equals("organism")) {
                        String organism = element.getCharData();
                        curation.setOrganism(organism);
                        if (Config.hasStyleForSpecies(organism)) {
                            Config.setStyleForSpecies(organism);
                            setStyle(Config.getStyle());
                        }
                    } else if (element.getType().equals("span")) {
                        Vector map_pos_elements = element.getChildren();
                        start = 0;
                        end = 0;
                        XMLElement map_pos_element = null;
                        for (int k = 0; k < map_pos_elements.size(); k++) {
                            map_pos_element = (XMLElement) map_pos_elements.elementAt(k);
                            if (map_pos_element.getType().equals("start")) {
                                start = Integer.parseInt(map_pos_element.getCharData());
                            } else if (map_pos_element.getType().equals("end")) {
                                end = Integer.parseInt(map_pos_element.getCharData());
                            }
                        }
                        if (end == start) {
                            logger.warn("top level end == start = " + end + ".  Using seq length  " + curated_seq.getLength() + " to set end position.");
                            end = start + curated_seq.getLength() + 1;
                            map_pos_element.setCharData((new Integer(end)).toString());
                        } else if (start > end) {
                            String message = "Problem in XML: map_position start is bigger than end.\nI don't yet know how to handle reversed regions--sorry.";
                            logger.error(message);
                            throw new ApolloAdapterException(message);
                        }
                        setSpan(element, curation, 0);
                    }
                }
            }
        }
        if (seq_id == null) seq_id = curated_seq.getName();
        if (start <= 0 || end <= 0) {
            start = 1;
            end = curated_seq.getLength();
            curation.setStrand(curation_strand);
            curation.setStart(start);
            curation.setEnd(end);
        }
        curated_seq.setName(seq_id);
    }

    /** Parses out dbxref and name from "gene" xml element. This name is used
      as display name in apollo. Changed input from Gene to AnnotatedFeatureI
      for non gene types. rename setAnnotInfo or something more specific?
  */
    private void setGeneInfo(XMLElement gene_element, AnnotatedFeatureI gene) {
        Vector gene_elements = gene_element.getChildren();
        for (int i = 0; i < gene_elements.size(); i++) {
            XMLElement element = (XMLElement) gene_elements.elementAt(i);
            if (element.getType().equals("dbxref")) {
                setAnnotXref(element, gene);
            } else if (element.getType().equals("name")) {
                if (element.getCharData() != null && !element.getCharData().equals("")) gene.setName(element.getCharData());
            }
        }
    }

    private void setAnnotXref(XMLElement element, AnnotatedFeatureI gene) {
        String db = "";
        String id = "";
        Vector xref_elements = element.getChildren();
        for (int j = 0; j < xref_elements.size(); j++) {
            XMLElement xref_element = (XMLElement) xref_elements.elementAt(j);
            if (xref_element.getType().equals("xref_db")) {
                db = xref_element.getCharData();
            }
            if (xref_element.getType().equals("db_xref_id")) {
                id = xref_element.getCharData();
            }
        }
        if (!db.equals("") && !id.equals("")) {
            gene.addDbXref(new DbXref("id", id, db));
        }
    }

    private void setScore(XMLElement score_element, SeqFeatureI feat) {
        String scoreString = score_element.getCharData();
        double score = Double.valueOf(scoreString).doubleValue();
        feat.setScore(score);
    }

    private void addTranscriptSequence(CurationSet curation, Transcript transcript, XMLElement seq_element) {
        String seq_id = seq_element.getID();
        SequenceI seq = createSequence(seq_id, curation, false);
        String res_type = seq_element.getAttribute("type");
        String lengthString = seq_element.getAttribute("length");
        int length = 0;
        if (lengthString != null) {
            length = Integer.parseInt(lengthString);
            seq.setLength(Integer.parseInt(lengthString));
        }
        String checksum = seq_element.getAttribute("md5checksum");
        if (checksum != null && !checksum.equals("")) {
            seq.setChecksum(checksum);
        }
        Vector elements = seq_element.getChildren();
        for (int i = 0; i < elements.size(); i++) {
            XMLElement element = (XMLElement) elements.elementAt(i);
            if (element.getType().equals("name")) {
                FastaHeader fasta = new FastaHeader(element.getCharData());
                seq.setName(fasta.getSeqId());
            } else if (element.getType().equals("description")) {
                seq.setDescription(element.getCharData());
            } else if (element.getType().equals("dbxref")) {
                addSeqXref(element, (Sequence) seq);
            } else if (element.getType().equals("residues")) {
                seq.setResidues(element.getCharData());
            } else if (element.getType().equals("organism")) {
                seq.setOrganism(element.getCharData());
            } else {
                logger.warn("Not dealing with seq element " + element.getType());
            }
        }
        if (res_type != null && res_type.equalsIgnoreCase(SequenceI.AA)) {
            seq.setResidueType(SequenceI.AA);
            transcript.setPeptideSequence(seq);
            transcript.setPeptideValidity(!(Config.getRefreshPeptides()));
        } else {
            transcript.set_cDNASequence(seq);
        }
    }

    private SequenceI createSequence(String header, CurationSet curation, boolean alert) {
        FastaHeader fasta = new FastaHeader(header);
        String seq_id = fasta.getSeqId();
        SequenceI seq = (SequenceI) curation.getSequence(seq_id);
        if (seq == null) {
            seq = fasta.generateSequence();
            curation.addSequence(seq);
            if (alert) {
                logger.trace("Had to create sequence " + seq.getName());
            }
        }
        return seq;
    }

    private boolean addOutput(SeqFeatureI sf, XMLElement output_element) {
        String type = getTypeTag(output_element);
        return (setTagValue(sf, output_element, type));
    }

    private String getTypeTag(XMLElement output_element) {
        Vector elements = output_element.getChildren();
        String type = null;
        for (int i = 0; i < elements.size() && type == null; i++) {
            XMLElement element = (XMLElement) elements.elementAt(i);
            if (element.getType().equals("type")) {
                type = element.getCharData();
            }
        }
        return type;
    }

    private String getTypeValue(XMLElement output_element) {
        Vector elements = output_element.getChildren();
        String value = null;
        for (int i = 0; i < elements.size() && value == null; i++) {
            XMLElement element = (XMLElement) elements.elementAt(i);
            if (element.getType().equals("value")) {
                value = element.getCharData();
            }
        }
        return value;
    }

    /** Deals with a handful of tag-value pairs: total_score/score, amino-acid, 
   *  non-canonical_splice_site, cigar, symbol, problem and ResultTags (output type=tag)
   *  <output>
   *     <type>tag</type>
   *     <value>comment: incomplete CDS</value>
   *  </output>
   */
    private boolean setTagValue(SeqFeatureI sf, XMLElement output_element, String type) {
        String value = getTypeValue(output_element);
        boolean valid = true;
        if (type != null && value != null) {
            try {
                double score = Double.valueOf(value).doubleValue();
                if (type.equals("total_score")) {
                    type = "score";
                } else sf.addScore(type, score);
            } catch (Exception ex) {
                if (type.equals("amino-acid")) {
                    sf.setName(value);
                } else if (type.equals("non-canonical_splice_site") && (sf instanceof Transcript)) {
                    ((Transcript) sf).nonConsensusSplicingOkay(value.equals("approved"));
                } else if (type.equals("cigar")) {
                    if (sf instanceof FeaturePairI) ((FeaturePairI) sf).setCigar(value); else valid = false;
                } else if (type.equals("symbol")) {
                    String current_name = sf.getName();
                    if (current_name == null) sf.setName(value); else if (current_name.equals("") || current_name.equals(RangeI.NO_NAME)) sf.setName(value);
                } else {
                    sf.addProperty(type, value);
                    if (type.equals("problem") && value.equals("true")) {
                        if (sf instanceof AnnotatedFeatureI) ((AnnotatedFeatureI) sf).setIsProblematic(true);
                    }
                }
            }
        }
        return valid;
    }

    /** This is very FlyBase/BDGP specific, but since it will do
      nothing in other cases it is a relatively benign hack. 
      Better here, on loading, than in the more generic glyph
      drawing. Anyway, this snippet checks the description of
      the aligned sequence to see if there is any information
      there to indicate the actual point of insertion. Doing
      this so that it may be cleanly propagated to any new 
      annotations of P element insertions derived from this
      alignment */
    private static String sesame = "inserted at base ";

    private void sniffOutInsertionSite(FeatureSetI result) {
        SequenceI seq = result.getHitSequence();
        if (seq == null && result.size() > 0) {
            SeqFeatureI sf = result.getFeatureAt(0);
            if (sf instanceof FeaturePairI) {
                seq = ((FeaturePairI) sf).getHitSequence();
            }
        }
        if (seq != null && seq.getDescription() != null) {
            String desc = seq.getDescription();
            int index = desc.indexOf(sesame);
            if (index >= 0) {
                desc = desc.substring(index + sesame.length());
                index = desc.indexOf(" ");
                if (index > 0) desc = desc.substring(0, index);
                try {
                    int site = Integer.parseInt(desc);
                    if (site < result.length()) result.addProperty("insertion_site", desc);
                } catch (Exception e) {
                }
            }
        }
    }

    private void repairGreek(SeqFeatureI sf) {
        sf.setName(HTMLUtil.replaceSGMLWithGreekLetter(sf.getName()));
        AnnotatedFeature gene = (AnnotatedFeature) sf;
        Vector syns = gene.getSynonyms();
        for (int i = 0; i < syns.size(); i++) {
            Synonym syn = (Synonym) syns.elementAt(i);
            String fixed = HTMLUtil.replaceSGMLWithGreekLetter(syn.getName());
            if (!(syn.getName().equals(fixed))) {
                logger.debug("Replacing synonym " + syn + " for gene " + gene.getName() + " with " + fixed);
                gene.deleteSynonym(syn.getName());
                syn.setName(fixed);
                gene.addSynonym(syn);
            }
        }
    }

    private void getTransactions(XMLElement transactionsElement, CurationSet curation) {
        TransactionXMLAdapter adap = new TransactionXMLAdapter();
        adap.getTransFromTopElement(transactionsElement, curation, getCurationState());
    }
}
