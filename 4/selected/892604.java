package uk.ac.ebi.pride.tools.converter.dao.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import matrix_science.msparser.ms_inputquery;
import matrix_science.msparser.ms_mascotresfile;
import matrix_science.msparser.ms_mascotresults;
import matrix_science.msparser.ms_peptide;
import matrix_science.msparser.ms_peptidesummary;
import matrix_science.msparser.ms_protein;
import matrix_science.msparser.ms_proteinsummary;
import matrix_science.msparser.ms_searchparams;
import matrix_science.msparser.ms_taxonomychoice;
import matrix_science.msparser.ms_taxonomyfile;
import matrix_science.msparser.vectord;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import uk.ac.ebi.pride.jaxb.model.Data;
import uk.ac.ebi.pride.jaxb.model.IntenArrayBinary;
import uk.ac.ebi.pride.jaxb.model.MzArrayBinary;
import uk.ac.ebi.pride.jaxb.model.Precursor;
import uk.ac.ebi.pride.jaxb.model.PrecursorList;
import uk.ac.ebi.pride.jaxb.model.Spectrum;
import uk.ac.ebi.pride.jaxb.model.SpectrumDesc;
import uk.ac.ebi.pride.jaxb.model.SpectrumInstrument;
import uk.ac.ebi.pride.jaxb.model.SpectrumSettings;
import uk.ac.ebi.pride.tools.converter.dao.DAO;
import uk.ac.ebi.pride.tools.converter.dao.DAOCvParams;
import uk.ac.ebi.pride.tools.converter.dao.DAOProperty;
import uk.ac.ebi.pride.tools.converter.report.model.CV;
import uk.ac.ebi.pride.tools.converter.report.model.Contact;
import uk.ac.ebi.pride.tools.converter.report.model.CvParam;
import uk.ac.ebi.pride.tools.converter.report.model.DatabaseMapping;
import uk.ac.ebi.pride.tools.converter.report.model.FragmentIon;
import uk.ac.ebi.pride.tools.converter.report.model.Identification;
import uk.ac.ebi.pride.tools.converter.report.model.InstrumentDescription;
import uk.ac.ebi.pride.tools.converter.report.model.PTM;
import uk.ac.ebi.pride.tools.converter.report.model.Param;
import uk.ac.ebi.pride.tools.converter.report.model.Peptide;
import uk.ac.ebi.pride.tools.converter.report.model.PeptidePTM;
import uk.ac.ebi.pride.tools.converter.report.model.Protocol;
import uk.ac.ebi.pride.tools.converter.report.model.Reference;
import uk.ac.ebi.pride.tools.converter.report.model.SearchResultIdentifier;
import uk.ac.ebi.pride.tools.converter.report.model.Software;
import uk.ac.ebi.pride.tools.converter.report.model.SourceFile;
import uk.ac.ebi.pride.tools.converter.utils.ConverterException;
import uk.ac.ebi.pride.tools.converter.utils.FileUtils;
import uk.ac.ebi.pride.tools.converter.utils.InvalidFormatException;
import uk.ac.ebi.pride.tools.converter.utils.config.Configurator;

/**
 * MascotDAO using the Matrix Science ms_parser
 * library to convert Mascot DAT files into PRIDE
 * XML files<br>
 * To convert Mascot results into PRIDE XML files several
 * compromises had to be taken which are as follows:<br>
 * <b>Different Ion Series:</b> Mascot supports the possibility
 * to query spectra where the ion series (b, y, rest) are
 * separated beforehand. These cases are currently not supported
 * by MascotDAO and only ionSeries 1 (as recommended in the ms_parser
 * documentation) is taken into consideration. This should work fine
 * for 99% of the cases.<br>
 * <b>Precursor Charge States:</b> There's currently only one
 * precursor supported per spectrum. Furthermore, as Mascot can
 * report multiple peptides per spectrum the charge state is only
 * reported at the petpide level and NOT at the precursor level.<br>
 * <b>Precursor retention time:</b> Mascot can return multiple retention
 * times for one precursor. The MascotDAO currently only uses the first
 * retention time. <br>
 * <b>Unsupported PRIDE XML objects:</b> The following objects are
 * currently not supported (and thus not returned) by the MascotDAO:
 * Activation parameter, spectrum acquisition parameters. <br>
 * <b>Error Tolerant Searches: </b> Only integrated error tolerant
 * searches are supported by the Mascot DAO as separate error tolerant
 * searches are not recommended by Matrix Science. <br>
 * <b>Quantitation Methods: </b>Quantitation methods are not supported
 * by the MascotDAO. These should generally not be supported by DAOs but by
 * specific QuantitationHandlers. <br>
 * <b>Protein families (Mascot >= V2.3):</b>Protein families cannot be reported
 * in PRIDE XML files. Therefore, the here presented results correspond to the
 * results seen in the older "peptide summary" view. <br>
 * <b>Protein scores in MudPIT experiments:</b> For several reasons when using
 * MudPIT scoring, proteins with only one peptide can have a lower score than
 * the threshold while still being deemed significant identifications. This is caused
 * by the fact that protein thesholds have to be determined by using the
 * average peptide threshold in the file (as recommended in the msparser
 * documentation).
 *
 * @author jg
 */
public class MascotDAO extends AbstractDAOImpl implements DAO {

    /**
     * The mascot result file object
     */
    private ms_mascotresfile mascotFile;

    /**
     * File representing the actual source file on the filesystem.
     */
    private File sourcefile;

    /**
     * File pointing to the temporary copy of the mascot library
     * in case one was created.
     */
    private File tmpMascotLibraryFile;

    /**
     * log4j logger object
     */
    private Logger logger = Logger.getLogger(MascotDAO.class);

    /**
     * String identifying the mascot search engine
     */
    private final String searchEngineString = "Matrix Science Mascot";

    /**
     * Properties object. Initially an empty object. Can be overwritten
     * by setProperties.
     */
    private Properties properties = new Properties();

    /**
     * String to map the mascot varModString to numbers. 0-9 indicate the
     * numbers 0-9, A-W the numbers 10-32
     */
    public final String mascotVarPtmString = "0123456789ABCDEFGHIJKLMNOPQRSTUVW";

    /**
     * Decimal format to use for all doubles
     */
    private final DecimalFormat decimalFormat;

    /**
     * Creating a (peptide) result summary is a time consuming
     * process. To make sure it's only done once, the function
     * getPeptideResults is used by all functions requiring such
     * an object. Thus, the object only is created once.
     */
    private ms_mascotresults results;

    private ms_mascotresults decoyResults;

    /**
     * A set holding the ids (= numbers) of all empty spectra.
     * These spectra are loaded in the constructer and will be
     * ignored in any iterator.
     */
    private Set<Integer> emptySpectraIds;

    /**
     * Holds the ids (= numbers) of all identified queries. These
     * might still include spectra not identified in the generated
     * result as the significance checks are not performed there.
     */
    private Set<Integer> identifiedSpectra;

    /**
     * Collection to hold all supported properties by this DAO.
     */
    @SuppressWarnings("rawtypes")
    private static Collection<DAOProperty> supportedProperties;

    /**
     * Indicates whether the mascot library was already loaded.
     */
    private static boolean isMascotLibraryLoaded = false;

    /**
     * Just a list of supported properties to keep thing's a little cleaner.
     *
     * @author jg
     */
    private enum SupportedProperties {

        MIN_PROPABILITY("min_probability", 0.05, MascotDAO.SupportedProperties.TYPE.DOUBLE), USE_MUDPIT_SCORING("use_mudpit_scoring", true, MascotDAO.SupportedProperties.TYPE.BOOLEAN), ONLY_SIGNIFICANT("only_significant", true, MascotDAO.SupportedProperties.TYPE.BOOLEAN), DUPE_SAME_QUERY("remove_duplicates_same_query", true, MascotDAO.SupportedProperties.TYPE.BOOLEAN), DUPE_DIFF_QUERY("remove_duplicates_different_query", false, MascotDAO.SupportedProperties.TYPE.BOOLEAN), INCLUDE_ERR_TOL("include_error_tolerant", false, MascotDAO.SupportedProperties.TYPE.BOOLEAN), DECOY_PREFIX("decoy_accession_prefix", "DECOY_", MascotDAO.SupportedProperties.TYPE.STRING), ENABLE_GROUPING("enable_protein_grouping", true, MascotDAO.SupportedProperties.TYPE.BOOLEAN), IGNORE_BELOW_SCORE("ignore_below_ions_score", 0.0, MascotDAO.SupportedProperties.TYPE.DOUBLE), COMPATIBILITY_MODE("compatibility_mode", true, MascotDAO.SupportedProperties.TYPE.BOOLEAN), REMOVE_EMPTY_SPECTRA("remove_empty_spectra", true, MascotDAO.SupportedProperties.TYPE.BOOLEAN), USE_HOMOLOGY_THREHOLD("homology_threshold", false, MascotDAO.SupportedProperties.TYPE.BOOLEAN);

        private String name;

        private Object defaultValue;

        private TYPE type;

        public enum TYPE {

            STRING, BOOLEAN, DOUBLE
        }

        private SupportedProperties(String name, Object defaultValue, TYPE type) {
            this.name = name;
            this.defaultValue = defaultValue;
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public Object getDefaultValue() {
            return defaultValue;
        }

        public TYPE getType() {
            return type;
        }

        @Override
        public String toString() {
            return name + " (" + defaultValue.toString() + ")";
        }
    }

    /**
     * Generates the collection of supported properties if
     * it wasn't created before.
     */
    @SuppressWarnings("rawtypes")
    private static void generateSupportedProperties() {
        if (supportedProperties != null) return;
        supportedProperties = new ArrayList<DAOProperty>(7);
        DAOProperty<Double> minProbability = new DAOProperty<Double>(SupportedProperties.MIN_PROPABILITY.getName(), (Double) SupportedProperties.MIN_PROPABILITY.getDefaultValue(), 0.0, 1.0);
        minProbability.setDescription("Specifies a cut-off point for protein scores, a cut-off for an Integrated error tolerant search and a threshold for calculating MudPIT scores. This value represents a probability threshold.");
        minProbability.setShortDescription("Probability cut-off for protein or peptide scores depending on the search (PMF or MS2).");
        supportedProperties.add(minProbability);
        DAOProperty<Boolean> useMudpit = new DAOProperty<Boolean>(SupportedProperties.USE_MUDPIT_SCORING.getName(), (Boolean) SupportedProperties.USE_MUDPIT_SCORING.getDefaultValue());
        useMudpit.setDescription("Indicates whether MudPIT or normal scoring should be used.");
        useMudpit.setShortDescription("Indicates whether MudPIT or normal scoring should be used.");
        supportedProperties.add(useMudpit);
        DAOProperty<Boolean> onlySignificant = new DAOProperty<Boolean>(SupportedProperties.ONLY_SIGNIFICANT.getName(), (Boolean) SupportedProperties.ONLY_SIGNIFICANT.getDefaultValue());
        onlySignificant.setDescription("Indicates whether only significant peptides / (in PMF searches) proteins should be included in the generated PRIDE file.");
        onlySignificant.setShortDescription("Only report significant identifications (peptides in MS2 and proteins in PMF searches).");
        supportedProperties.add(onlySignificant);
        DAOProperty<Boolean> dupeSameQuery = new DAOProperty<Boolean>(SupportedProperties.DUPE_SAME_QUERY.getName(), (Boolean) SupportedProperties.DUPE_SAME_QUERY.getDefaultValue());
        dupeSameQuery.setDescription("Indicates whether duplicate peptides having the same sequence and coming from the same query (= spectrum) should be removed. These peptides may have different modifications reported.");
        dupeSameQuery.setShortDescription("Remove duplicate identifications with the same sequence coming from the same spectrum.");
        dupeSameQuery.setAdvanced(true);
        supportedProperties.add(dupeSameQuery);
        DAOProperty<Boolean> dupeDiffQuery = new DAOProperty<Boolean>(SupportedProperties.DUPE_DIFF_QUERY.getName(), (Boolean) SupportedProperties.DUPE_DIFF_QUERY.getDefaultValue());
        dupeDiffQuery.setDescription("Indicates whether duplicate peptides having the same sequence (but maybe different modifications) coming from different queries (= spectra) should be removed.");
        dupeDiffQuery.setShortDescription("Remove duplicate peptides with the same sequence but coming from different spectra.");
        dupeDiffQuery.setAdvanced(true);
        supportedProperties.add(dupeDiffQuery);
        DAOProperty<Boolean> errTolSearch = new DAOProperty<Boolean>(SupportedProperties.INCLUDE_ERR_TOL.getName(), (Boolean) SupportedProperties.INCLUDE_ERR_TOL.getDefaultValue());
        errTolSearch.setDescription("Indicates whether integrated error tolerant search results should be included in the PRIDE XML support. These results are not included in the protein scores by Mascot.");
        errTolSearch.setShortDescription("Include error tolerant search results in PRIDE XML file (if present).");
        supportedProperties.add(errTolSearch);
        DAOProperty<String> decoyAccPrec = new DAOProperty<String>(SupportedProperties.DECOY_PREFIX.getName(), (String) SupportedProperties.DECOY_PREFIX.getDefaultValue());
        decoyAccPrec.setDescription("An accession prefix that identifies decoy hits. Every protein with an accession starting with this precursor will be flagged as decoy hit. Furthermore, any decoy hit who's accession does not start with this prefix will be altered accordingly.");
        decoyAccPrec.setShortDescription("Protein accession prefix to identify decoy hits.");
        supportedProperties.add(decoyAccPrec);
        DAOProperty<Boolean> proteinGrouping = new DAOProperty<Boolean>(SupportedProperties.ENABLE_GROUPING.getName(), (Boolean) SupportedProperties.ENABLE_GROUPING.getDefaultValue());
        proteinGrouping.setDescription("Indicates whether the grouping mode (Occam's Razor, see Mascot documentation) should be enabled. This is the default behaviour for Mascot. This mode is not equivalent to the protein clustering introduced in Mascot 2.3.");
        proteinGrouping.setShortDescription("Enable Mascot protein grouping mode (Occam's Razor).");
        proteinGrouping.setAdvanced(true);
        supportedProperties.add(proteinGrouping);
        DAOProperty<Double> ignoreIonsScore = new DAOProperty<Double>(SupportedProperties.IGNORE_BELOW_SCORE.getName(), (Double) SupportedProperties.IGNORE_BELOW_SCORE.getDefaultValue(), 0.0, 1.0);
        ignoreIonsScore.setDescription("Peptides with a lower expect ratio (of being false positives) will be ignored completely. Set to 1 to deactivate. Default value is " + SupportedProperties.IGNORE_BELOW_SCORE.getDefaultValue());
        ignoreIonsScore.setShortDescription("Ignore peptides with a lower expect ratio from any further analysis.");
        supportedProperties.add(ignoreIonsScore);
        DAOProperty<Boolean> compMode = new DAOProperty<Boolean>(SupportedProperties.COMPATIBILITY_MODE.getName(), (Boolean) SupportedProperties.COMPATIBILITY_MODE.getDefaultValue());
        compMode.setDescription("If set to true (default) the precuror charge will also be reported at the spectrum level using the best ranked peptide's charge state. This might lead to wrong precursor charges being reported. The correct charge state is always additionally reported at the peptide level.");
        compMode.setShortDescription("Report precursor charges at the spectrum level for compatibility with older applications (can lead to wrong results).");
        compMode.setAdvanced(true);
        supportedProperties.add(compMode);
        DAOProperty<Boolean> removeEmptySpec = new DAOProperty<Boolean>(SupportedProperties.REMOVE_EMPTY_SPECTRA.getName(), (Boolean) SupportedProperties.REMOVE_EMPTY_SPECTRA.getDefaultValue());
        removeEmptySpec.setDescription("If set to true (default) spectra without any peaks are ignored and not reported in the PRIDE XML file.");
        removeEmptySpec.setShortDescription("Do not report empty spectra in the PRIDE XML file.");
        removeEmptySpec.setAdvanced(true);
        supportedProperties.add(removeEmptySpec);
        DAOProperty<Boolean> homologyThreshold = new DAOProperty<Boolean>(SupportedProperties.USE_HOMOLOGY_THREHOLD.getName(), (Boolean) SupportedProperties.USE_HOMOLOGY_THREHOLD.getDefaultValue());
        homologyThreshold.setDescription("If set to true (default is \"false\" the homology instead of the identity threshold will be used to identify significant identifications.");
        homologyThreshold.setShortDescription("Use the homology threshold instead of the identity threshold.");
        supportedProperties.add(homologyThreshold);
    }

    /**
     * Returns the current value for the given property.
     *
     * @param property The property to get the current value for.
     * @return An Object representing the property's current value.
     */
    private Object getCurrentProperty(SupportedProperties property) {
        if (properties.containsKey(property.getName())) {
            String value = properties.getProperty(property.getName());
            switch(property.getType()) {
                case BOOLEAN:
                    return Boolean.parseBoolean(value);
                case DOUBLE:
                    return Double.parseDouble(value);
                case STRING:
                default:
                    return value;
            }
        } else {
            return property.getDefaultValue();
        }
    }

    /**
     * Used to retrieve the list of supported properties. Properties should nevertheless
     * be set using the setConfiguration method.
     *
     * @return A collection of supported properties.
     */
    @SuppressWarnings("rawtypes")
    public static Collection<DAOProperty> getSupportedProperties() {
        generateSupportedProperties();
        return supportedProperties;
    }

    @Override
    public void setExternalSpectrumFile(String filename) {
    }

    /**
     * Detault constructor. Expects the result file as parameter.
     *
     * @param resultFile
     * @throws InvalidFormatException
     * @throws IllegalArgumentException Thrown if the argument isn't pointing to a valid mascot file. (File is checked for validity)
     * @throws UnsatisfiedLinkError     Thrown if the mascot library could not be found at the expected location.
     */
    public MascotDAO(File resultFile) throws InvalidFormatException {
        try {
            DecimalFormatSymbols decimalSymbols = new DecimalFormatSymbols(Locale.US);
            decimalFormat = new DecimalFormat("#.##", decimalSymbols);
            this.loadMascotLibrary();
            if (!resultFile.isFile()) throw new FileNotFoundException();
            logger.debug("Parsing .dat file " + resultFile.getAbsolutePath());
            mascotFile = new ms_mascotresfile(resultFile.getAbsolutePath());
            if (!mascotFile.isValid()) throw new InvalidFormatException("Invalid mascot file passed");
            if (mascotFile.isPMF() && (mascotFile.isMSMS() || mascotFile.isSQ())) throw new InvalidFormatException("Cannot handle PMF result files combined with results form other query methods.");
            sourcefile = resultFile;
            prescanSpectra();
        } catch (UnsatisfiedLinkError ex) {
            logger.error(ex.getMessage());
            throw new ConverterException("Mascot library not found", ex);
        } catch (FileNotFoundException ex) {
            logger.error(ex.getMessage());
            throw new InvalidFormatException("Mascot result file not found", ex);
        }
    }

    /**
     * Scans all queries in the loaded file and
     * checks for queries without any peaks as well
     * as the ones that lead to a peptide identification.
     */
    private void prescanSpectra() {
        emptySpectraIds = new HashSet<Integer>();
        identifiedSpectra = new HashSet<Integer>();
        ms_mascotresults results = getResults();
        for (int i = 1; i <= mascotFile.getNumQueries(); i++) {
            ms_peptide pep = results.getPeptide(i, 1);
            if (pep.getAnyMatch()) identifiedSpectra.add(i);
            ms_inputquery query = new ms_inputquery(mascotFile, i);
            if (query.getNumberOfPeaks(1) < 1) {
                emptySpectraIds.add(i);
            }
        }
        results = getDecoyResults();
        if (results == null) return;
        for (int i = 1; i <= mascotFile.getNumQueries(); i++) {
            ms_peptide pep = results.getPeptide(i, 1);
            if (pep.getAnyMatch()) identifiedSpectra.add(i);
            ms_inputquery query = new ms_inputquery(mascotFile, i);
            if (query.getNumberOfPeaks(1) < 1) {
                emptySpectraIds.add(i);
            }
        }
    }

    @Override
    protected void finalize() throws Throwable {
        if (tmpMascotLibraryFile != null && tmpMascotLibraryFile.exists()) tmpMascotLibraryFile.delete();
        super.finalize();
    }

    /**
     * Tries to load the mascot library.
     */
    private void loadMascotLibrary() {
        if (isMascotLibraryLoaded) return;
        try {
            boolean isLinux = false;
            boolean isAMD64 = false;
            String mascotLibraryFile;
            if (Configurator.getOSName().toLowerCase().contains("linux")) {
                isLinux = true;
            }
            if (Configurator.getOSArch().toLowerCase().contains("amd64")) {
                isAMD64 = true;
            }
            if (isLinux) {
                if (isAMD64) {
                    mascotLibraryFile = "libmsparserj-64.so";
                } else {
                    mascotLibraryFile = "libmsparserj-32.so";
                }
            } else {
                if (isAMD64) {
                    mascotLibraryFile = "msparserj-64.dll";
                } else {
                    mascotLibraryFile = "msparserj-32.dll";
                }
            }
            logger.warn("Using: " + mascotLibraryFile);
            URL mascot_lib = MascotDAO.class.getClassLoader().getResource(mascotLibraryFile);
            if (mascot_lib != null) {
                logger.debug("Mascot library URL: " + mascot_lib);
                tmpMascotLibraryFile = File.createTempFile("libmascot.so.", ".tmp", new File(System.getProperty("java.io.tmpdir")));
                InputStream in = mascot_lib.openStream();
                OutputStream out = new FileOutputStream(tmpMascotLibraryFile);
                IOUtils.copy(in, out);
                in.close();
                out.close();
                System.load(tmpMascotLibraryFile.getAbsolutePath());
                isMascotLibraryLoaded = true;
            } else {
                throw new ConverterException("Could not load Mascot Library for system: " + Configurator.getOSName() + Configurator.getOSArch());
            }
        } catch (IOException e) {
            throw new ConverterException("Error loading Mascot library: " + e.getMessage(), e);
        }
    }

    @Override
    public Collection<CV> getCvLookup() {
        ArrayList<CV> cvs = new ArrayList<CV>();
        cvs.add(new CV("MS", "PSI Mass Spectrometry Ontology", "1.2", "http://psidev.cvs.sourceforge.net/viewvc/*checkout*/psidev/psi/psi-ms/mzML/controlledVocabulary/psi-ms.obo"));
        cvs.add(new CV("PRIDE", "PRIDE Controlled Vocabulary", "1.101", "http://ebi-pride.googlecode.com/svn/trunk/pride-core/schema/pride_cv.obo"));
        return cvs;
    }

    @Override
    public String getExperimentTitle() throws InvalidFormatException {
        return "";
    }

    @Override
    public String getExperimentShortLabel() {
        return null;
    }

    @Override
    public Param getExperimentParams() {
        Param params = new Param();
        int seconds = mascotFile.getDate();
        long msec = (long) seconds * (long) 1000;
        Date searchDate = new Date(msec);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        params.getCvParam().add(DAOCvParams.DATE_OF_SEARCH.getParam(formatter.format(searchDate)));
        params.getCvParam().add(DAOCvParams.ORIGINAL_MS_FORMAT.getParam("Mascot dat file"));
        if (mascotFile.isPMF()) params.getCvParam().add(DAOCvParams.PMF_SEARCH.getParam());
        if (mascotFile.isSQ()) params.getCvParam().add(DAOCvParams.TAG_SEARCH.getParam());
        if (mascotFile.isMSMS()) params.getCvParam().add(DAOCvParams.MS_MS_SEARCH.getParam());
        Double fdr = getFDR();
        if (fdr != null) params.getCvParam().add(DAOCvParams.PEPTIDE_FDR.getParam(fdr.toString()));
        return params;
    }

    @Override
    public String getSampleName() {
        return null;
    }

    @Override
    public String getSampleComment() {
        return null;
    }

    @Override
    public Param getSampleParams() {
        Param sampleParams = new Param();
        HashMap<Integer, String> taxids = getTaxids();
        if (taxids != null) {
            for (Integer taxid : taxids.keySet()) sampleParams.getCvParam().add(new CvParam("NEWT", taxid.toString(), taxids.get(taxid), ""));
        }
        return sampleParams;
    }

    /**
     * Extracts the included taxonomies from the taxonomy file
     * and returns them in a HashMap with the taxid as key and the
     * name as value.
     *
     * @return A HashMap of taxonomies found in the file. NULL if the taxonomy file could not be parsed.
     */
    private HashMap<Integer, String> getTaxids() {
        ms_taxonomyfile taxFile = new ms_taxonomyfile();
        if (mascotFile.getTaxonomy(taxFile)) {
            if (taxFile.isValid()) {
                HashMap<Integer, String> taxids = new HashMap<Integer, String>();
                int entries = taxFile.getNumberOfEntries();
                for (int i = 0; i < entries; i++) {
                    ms_taxonomychoice entry = taxFile.getEntryByNumber(i);
                    for (int j = 0; j < entry.getNumberOfIncludeTaxonomies(); j++) {
                        taxids.put(entry.getIncludeTaxonomy(j), entry.getTitle().replace(".", "").trim());
                    }
                }
                return taxids;
            }
        }
        return null;
    }

    @Override
    public SourceFile getSourceFile() {
        SourceFile file = new SourceFile();
        file.setPathToFile(sourcefile.getAbsolutePath());
        file.setNameOfFile(sourcefile.getName());
        file.setFileType("Mascot dat file");
        return file;
    }

    @Override
    public Collection<Contact> getContacts() {
        Collection<Contact> contacts = new ArrayList<Contact>(1);
        if (mascotFile.params().getUSERNAME() != null && mascotFile.params().getUSERNAME().length() > 0) {
            Contact contact = new Contact();
            contact.setName(mascotFile.params().getUSERNAME());
            if (mascotFile.params().getUSEREMAIL() != null && mascotFile.params().getUSEREMAIL().length() > 0) contact.setContactInfo(mascotFile.params().getUSEREMAIL());
            contact.setInstitution("");
            contacts.add(contact);
        }
        return contacts;
    }

    @Override
    public InstrumentDescription getInstrument() {
        return null;
    }

    @Override
    public Software getSoftware() {
        Software software = new Software();
        software.setName(searchEngineString);
        software.setVersion(mascotFile.getMascotVer());
        return software;
    }

    @Override
    public Param getProcessingMethod() {
        Param params = new Param();
        ms_searchparams searchParams = mascotFile.params();
        if (searchParams == null) return null;
        Double fragmentTolerance = searchParams.getITOL();
        params.getCvParam().add(DAOCvParams.SEARCH_SETTING_FRAGMENT_MASS_TOLERANCE.getParam(fragmentTolerance + " " + searchParams.getITOLU()));
        Double parentTolerance = searchParams.getTOL();
        params.getCvParam().add(DAOCvParams.SEARCH_SETTING_PARENT_MASS_TOLERANCE.getParam(parentTolerance + " " + searchParams.getTOLU()));
        params.getCvParam().add(DAOCvParams.SEARCH_SETTING_MISSED_CLEAVAGES.getParam(searchParams.getPFA()));
        params.getCvParam().add(DAOCvParams.MASCOT_SIGNIFICANCE_THRESHOLD.getParam(getCurrentProperty(SupportedProperties.MIN_PROPABILITY)));
        params.getCvParam().add(DAOCvParams.MASCOT_SIGNIFICANCE_THRESHOLD_TYPE.getParam((Boolean) getCurrentProperty(SupportedProperties.USE_HOMOLOGY_THREHOLD) ? "homology" : "identity"));
        return params;
    }

    /**
     * Returns comma-delimited string containing the names of the used search
     * databases.
     */
    @Override
    public String getSearchDatabaseName() {
        int nDbs = mascotFile.params().getNumberOfDatabases();
        if (nDbs == 1) return mascotFile.params().getDB(1);
        String dbs = "";
        for (int i = 1; i <= nDbs; i++) {
            if (i > 1) dbs += ", ";
            dbs += mascotFile.params().getDB(i);
        }
        return dbs;
    }

    /**
     * Returns a comma-delimited string containing the (fasta) versions of the used
     * search database.
     */
    @Override
    public String getSearchDatabaseVersion() {
        int nDbs = mascotFile.params().getNumberOfDatabases();
        if (nDbs == 1) return mascotFile.getFastaVer(1);
        String versions = "";
        for (int i = 1; i <= nDbs; i++) {
            if (i > 1) versions += ", ";
            versions += mascotFile.getFastaVer(i);
        }
        return versions;
    }

    @Override
    public Collection<DatabaseMapping> getDatabaseMappings() {
        int nDbs = mascotFile.params().getNumberOfDatabases();
        ArrayList<DatabaseMapping> mappings = new ArrayList<DatabaseMapping>();
        for (int i = 1; i <= nDbs; i++) {
            DatabaseMapping mapping = new DatabaseMapping();
            mapping.setSearchEngineDatabaseName(mascotFile.params().getDB(i));
            mapping.setSearchEngineDatabaseVersion(mascotFile.getFastaVer(i));
            mappings.add(mapping);
        }
        return mappings;
    }

    /**
     * Returns a collection of PTMs. The PTMs only contain the
     * searchEnginePTMLabel as well as if they are fixed modifications.
     */
    @Override
    public Collection<PTM> getPTMs() {
        boolean average = mascotFile.params().getMASS().equals("Average");
        ArrayList<PTM> ptms = new ArrayList<PTM>();
        String modName = "";
        int modNumber = 1;
        do {
            modName = mascotFile.params().getFixedModsName(modNumber);
            if (modName.length() > 0) {
                PTM ptm = new PTM();
                ptm.setFixedModification(true);
                ptm.setSearchEnginePTMLabel(modName);
                ptm.setResidues(mascotFile.params().getFixedModsResidues(modNumber).replace("C_term", "1").replace("N_term", "0"));
                Double modDelta = mascotFile.params().getFixedModsDelta(modNumber);
                if (average) {
                    if (ptm.getModAvgDelta().size() == 0) {
                        ptm.getModAvgDelta().add(modDelta.toString());
                    }
                } else {
                    if (ptm.getModMonoDelta().size() == 0) {
                        ptm.getModMonoDelta().add(modDelta.toString());
                    }
                }
                ptms.add(ptm);
            }
            modNumber++;
        } while (modName.length() > 0);
        modName = "";
        modNumber = 1;
        Map<String, String> varModResidues = getVarModResidues();
        do {
            modName = mascotFile.params().getVarModsName(modNumber);
            if (modName.length() > 0) {
                PTM ptm = new PTM();
                ptm.setFixedModification(false);
                ptm.setSearchEnginePTMLabel(modName);
                if (varModResidues.containsKey(modName)) ptm.setResidues(varModResidues.get(modName)); else logger.warn("No residue information available for modification '" + modName + "'");
                Double modDelta = mascotFile.params().getVarModsDelta(modNumber);
                if (average) {
                    if (ptm.getModAvgDelta().size() == 0) {
                        ptm.getModAvgDelta().add(modDelta.toString());
                    }
                } else {
                    if (ptm.getModMonoDelta().size() == 0) {
                        ptm.getModMonoDelta().add(modDelta.toString());
                    }
                }
                ptms.add(ptm);
            }
            modNumber++;
        } while (modName.length() > 0);
        return ptms;
    }

    /**
     * Adds the amino acid specificity information
     * to the found variable modifications.
     *
     * @param ptms
     * @return
     */
    private Map<String, String> getVarModResidues() {
        HashMap<String, HashSet<Character>> varModResidues = new HashMap<String, HashSet<Character>>();
        ms_mascotresults results = getResults();
        for (int queryNum = 1; queryNum <= mascotFile.getNumQueries(); queryNum++) {
            ms_peptide peptide = results.getPeptide(queryNum, 1);
            if (!peptide.getAnyMatch()) continue;
            String modString = peptide.getVarModsStr();
            for (Integer position = 0; position < modString.length(); position++) {
                char modChar = modString.charAt(position);
                int modNumber = mascotVarPtmString.indexOf(modChar);
                if (modNumber == -1) continue;
                if (modNumber == 0 && modChar != 'X') continue;
                String name;
                if (modChar != 'X') name = mascotFile.params().getVarModsName(modNumber); else name = results.getErrTolModName(peptide.getQuery(), peptide.getRank());
                if (name.length() < 1) continue;
                if (!varModResidues.containsKey(name)) varModResidues.put(name, new HashSet<Character>());
                if (position == 0) varModResidues.get(name).add('0'); else if (position > peptide.getPeptideStr().length()) varModResidues.get(name).add('1'); else varModResidues.get(name).add(peptide.getPeptideStr().charAt(position - 1));
            }
        }
        HashMap<String, String> varModResidueStrings = new HashMap<String, String>();
        for (String modName : varModResidues.keySet()) {
            ArrayList<Character> chars = new ArrayList<Character>(varModResidues.get(modName));
            Collections.sort(chars);
            String residueString = "";
            for (Character c : chars) residueString += c;
            varModResidueStrings.put(modName, residueString);
        }
        return varModResidueStrings;
    }

    @Override
    public SearchResultIdentifier getSearchResultIdentifier() {
        SearchResultIdentifier identifier = new SearchResultIdentifier();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        identifier.setSourceFilePath(sourcefile.getAbsolutePath());
        identifier.setTimeCreated(formatter.format(new Date(System.currentTimeMillis())));
        identifier.setHash(FileUtils.MD5Hash(sourcefile.getAbsolutePath()));
        return identifier;
    }

    @Override
    public int getSpectrumCount(boolean onlyIdentified) {
        int count = 0;
        if (mascotFile.isPMF()) return 1;
        if (onlyIdentified) {
            count = identifiedSpectra.size();
        } else {
            count = mascotFile.getNumQueries() - emptySpectraIds.size();
        }
        return count;
    }

    @Override
    public Iterator<Spectrum> getSpectrumIterator(boolean onlyIdentified) {
        return new MascotSpectrumIterator(onlyIdentified);
    }

    private class MascotSpectrumIterator implements Iterator<Spectrum> {

        /**
         * A list of spectrum ids, used if only spectra with identifications should be returned
         */
        private ArrayList<Integer> queryIds = new ArrayList<Integer>();

        /**
         * Indicates whether only spectra with identifications or all of them should be returned
         */
        private boolean onlyIdentified = false;

        /**
         * current index, either in the queryIds array or in the mascot file
         */
        private int currentIndex = 0;

        /**
         * number of spectra in the mascot file
         */
        private final int nSpectra;

        /**
         * The difference between the actual query id
         * and the currentIndex. As Mascot queries are
         * 1-based and the currentIndex is 0-based the
         * factor is always 1. It's incremented as soon
         * as empty spectra are encountered and need to be
         * ignored.
         */
        private int correctionFactor = 1;

        /**
         * The default constructor.
         *
         * @param onlyIdentified Indicates whether all available spectra or only spectra with a peptide identification should be returned.
         */
        public MascotSpectrumIterator(boolean onlyIdentified) {
            this.onlyIdentified = onlyIdentified;
            if (mascotFile.isPMF()) nSpectra = 1; else nSpectra = mascotFile.getNumQueries() - ((Boolean) getCurrentProperty(SupportedProperties.REMOVE_EMPTY_SPECTRA) ? emptySpectraIds.size() : 0);
            if (onlyIdentified) {
                queryIds.addAll(identifiedSpectra);
                Collections.sort(queryIds);
            }
        }

        @Override
        public boolean hasNext() {
            if (mascotFile.isPMF()) return currentIndex < 1;
            if (onlyIdentified) {
                return currentIndex < queryIds.size();
            } else {
                return currentIndex < nSpectra;
            }
        }

        @Override
        public Spectrum next() {
            if (mascotFile.isPMF()) {
                currentIndex++;
                return createPMFSpectrum();
            }
            int spectrumIndex = -1;
            if (onlyIdentified) {
                spectrumIndex = queryIds.get(currentIndex);
            } else {
                spectrumIndex = currentIndex + correctionFactor;
                if ((Boolean) getCurrentProperty(SupportedProperties.REMOVE_EMPTY_SPECTRA)) {
                    while (emptySpectraIds.contains(spectrumIndex)) {
                        correctionFactor++;
                        spectrumIndex = currentIndex + correctionFactor;
                    }
                }
            }
            currentIndex++;
            if (spectrumIndex < 1 || currentIndex > nSpectra) return null;
            ms_inputquery query = new ms_inputquery(mascotFile, spectrumIndex);
            try {
                return createSpectrum(query, spectrumIndex);
            } catch (InvalidFormatException e) {
                throw new ConverterException(e);
            }
        }

        /**
         * Creates a spectrum object from the passed PMF queries
         * entered by the user.
         * @return
         */
        private Spectrum createPMFSpectrum() {
            Spectrum spectrum = new Spectrum();
            spectrum.setId(1);
            ArrayList<Double> masses = new ArrayList<Double>();
            ArrayList<Double> intensities = new ArrayList<Double>();
            for (int i = 1; i <= mascotFile.getNumQueries(); i++) {
                Double mz = mascotFile.getObservedMass(i);
                Double intensity = mascotFile.getObservedIntensity(i);
                if (intensity == 0) intensity = 1.0;
                masses.add(mz);
                intensities.add(intensity);
            }
            byte[] massesBytes = doubleCollectionToByteArray(masses);
            byte[] intenBytes = doubleCollectionToByteArray(intensities);
            Data intenData = new Data();
            intenData.setEndian("little");
            intenData.setLength(intenBytes.length);
            intenData.setPrecision("64");
            intenData.setValue(intenBytes);
            IntenArrayBinary intenArrayBin = new IntenArrayBinary();
            intenArrayBin.setData(intenData);
            Data massData = new Data();
            massData.setEndian("little");
            massData.setLength(massesBytes.length);
            massData.setPrecision("64");
            massData.setValue(massesBytes);
            MzArrayBinary massArrayBinary = new MzArrayBinary();
            massArrayBinary.setData(massData);
            spectrum.setIntenArrayBinary(intenArrayBin);
            spectrum.setMzArrayBinary(massArrayBinary);
            SpectrumDesc description = new SpectrumDesc();
            SpectrumSettings settings = new SpectrumSettings();
            SpectrumInstrument instrument = new SpectrumInstrument();
            instrument.setMsLevel(1);
            Float rangeStart = Collections.min(masses).floatValue();
            Float rangeStop = Collections.max(masses).floatValue();
            instrument.setMzRangeStart(rangeStart);
            instrument.setMzRangeStop(rangeStop);
            settings.setSpectrumInstrument(instrument);
            description.setSpectrumSettings(settings);
            spectrum.setSpectrumDesc(description);
            return spectrum;
        }

        @Override
        public void remove() {
        }

        private Spectrum createSpectrum(ms_inputquery query, int spectrumId) throws InvalidFormatException {
            Spectrum spectrum = new Spectrum();
            spectrum.setId(spectrumId);
            ArrayList<Double> masses = new ArrayList<Double>();
            ArrayList<Double> intensities = new ArrayList<Double>();
            for (int ions = 1; ions <= 1; ions++) {
                int numPeaks = query.getNumberOfPeaks(ions);
                for (int peakNo = 1; peakNo <= numPeaks; peakNo++) {
                    masses.add(query.getPeakMass(ions, peakNo));
                    intensities.add(query.getPeakIntensity(ions, peakNo));
                }
            }
            byte[] massesBytes = doubleCollectionToByteArray(masses);
            byte[] intenBytes = doubleCollectionToByteArray(intensities);
            Data intenData = new Data();
            intenData.setEndian("little");
            intenData.setLength(intenBytes.length);
            intenData.setPrecision("64");
            intenData.setValue(intenBytes);
            IntenArrayBinary intenArrayBin = new IntenArrayBinary();
            intenArrayBin.setData(intenData);
            Data massData = new Data();
            massData.setEndian("little");
            massData.setLength(massesBytes.length);
            massData.setPrecision("64");
            massData.setValue(massesBytes);
            MzArrayBinary massArrayBinary = new MzArrayBinary();
            massArrayBinary.setData(massData);
            spectrum.setIntenArrayBinary(intenArrayBin);
            spectrum.setMzArrayBinary(massArrayBinary);
            spectrum.setSpectrumDesc(generateSpectrumDescription(query, spectrumId));
            return spectrum;
        }

        /**
         * Generates the SpectrumDesc object for the passed spectrum. <br>
         * A charge state is only reported at the peptide level
         *
         * @param query      The ms_inputquery object representing the given spectrum.
         * @param spectrumId The spectrum's id in the mascot file (1-based)
         * @return The SpectrumDesc object for the given spectrum
         */
        private SpectrumDesc generateSpectrumDescription(ms_inputquery query, int spectrumId) {
            SpectrumDesc description = new SpectrumDesc();
            int msLevel = 0;
            if (mascotFile.isMSMS()) msLevel = 2; else logger.error("Spectrum msLevel cannot be determined for non-MS/MS experiments.");
            SpectrumSettings settings = new SpectrumSettings();
            SpectrumInstrument instrument = new SpectrumInstrument();
            instrument.setMsLevel(msLevel);
            Float rangeStart = new Float((query.getMinInternalMass() != -1) ? query.getMinInternalMass() : query.getMassMin());
            Float rangeStop = new Float((query.getMaxInternalMass() != -1) ? query.getMaxInternalMass() : query.getMassMax());
            instrument.setMzRangeStart(rangeStart);
            instrument.setMzRangeStop(rangeStop);
            settings.setSpectrumInstrument(instrument);
            description.setSpectrumSettings(settings);
            PrecursorList precList = new PrecursorList();
            precList.setCount(1);
            Precursor prec = new Precursor();
            prec.setMsLevel(msLevel - 1);
            Spectrum spec = new Spectrum();
            spec.setId(0);
            prec.setSpectrum(spec);
            uk.ac.ebi.pride.jaxb.model.Param ionSelection = new uk.ac.ebi.pride.jaxb.model.Param();
            if (mascotFile.getObservedIntensity(spectrumId) != 0) ionSelection.getCvParam().add(DAOCvParams.PRECURSOR_INTENSITY.getJaxbParam(new Double(mascotFile.getObservedIntensity(spectrumId)).toString()));
            if (mascotFile.getObservedMass(spectrumId) != 0) ionSelection.getCvParam().add(DAOCvParams.PRECURSOR_MZ.getJaxbParam(new Double(mascotFile.getObservedMass(spectrumId)).toString()));
            if (query.getRetentionTimes().length() > 0) ionSelection.getCvParam().add(DAOCvParams.RETENTION_TIME.getJaxbParam(query.getRetentionTimes()));
            if ((Boolean) getCurrentProperty(SupportedProperties.COMPATIBILITY_MODE)) {
                ms_mascotresults res = getResults();
                ms_peptide p = res.getPeptide(spectrumId, 1);
                if (p.getAnyMatch()) {
                    ionSelection.getCvParam().add(DAOCvParams.CHARGE_STATE.getJaxbParam(new Integer(p.getCharge()).toString()));
                }
            }
            prec.setIonSelection(ionSelection);
            prec.setActivation(new uk.ac.ebi.pride.jaxb.model.Param());
            precList.getPrecursor().add(prec);
            description.setPrecursorList(precList);
            return description;
        }
    }

    @Override
    public int getSpectrumReferenceForPeptideUID(String peptideUID) {
        if (mascotFile.isPMF()) return 1;
        int index = peptideUID.indexOf("_");
        if (index == -1) return -1;
        String strQueryId = peptideUID.substring(0, index);
        return Integer.parseInt(strQueryId);
    }

    @Override
    public Identification getIdentificationByUID(String proteinUID) throws InvalidFormatException {
        boolean isDecoy = false;
        if (proteinUID.startsWith("d_")) {
            proteinUID = proteinUID.substring(2);
            isDecoy = true;
        }
        Integer index = Integer.parseInt(proteinUID);
        ms_mascotresults res = (isDecoy) ? getDecoyResults() : getResults();
        ms_protein p = res.getHit(index);
        if (p == null) throw new InvalidFormatException("Protein with uid = " + proteinUID + " could not be found (decoy = " + isDecoy + ")");
        return createIdentification(p, index, false, isDecoy);
    }

    @Override
    public Iterator<Identification> getIdentificationIterator(boolean prescanMode) {
        return new MascotIdentificationIterator(prescanMode);
    }

    private class MascotIdentificationIterator implements Iterable<Identification>, Iterator<Identification> {

        /**
         * Current position in the identifications
         */
        private int index = 1;

        /**
         * Total number of identifications available
         */
        private int size;

        /**
         * Indicates whether iterator should return pre-scan or scan (= complete) objects.
         */
        private final boolean prescanMode;

        /**
         * A local representation of the ms_mascotresults. This is
         * to make sure that the results were created.
         */
        private ms_mascotresults localResults;

        /**
         * Decoy results are processed after the true results.
         */
        private ms_mascotresults localDecoyResults;

        /**
         * Indicates whether the current protein is a decoy result
         */
        private boolean isDecoyHit;

        /**
         * In PMF based experiments the proteins need to be checked
         * whether they are significant hits
         */
        private List<Integer> significantPMFIdentifications;

        /**
         * Default constructor
         *
         * @param prescanMode Boolean to indicate whether complete or pre-scan objects should be returned.
         */
        public MascotIdentificationIterator(boolean prescanMode) {
            this.prescanMode = prescanMode;
            if (!mascotFile.anyPeptideSummaryMatches() && !mascotFile.anyPMF()) {
                size = 0;
                return;
            }
            if (mascotFile.isPMF() && (Boolean) getCurrentProperty(SupportedProperties.ONLY_SIGNIFICANT)) {
                loadSignificantPMFHits();
            }
            localResults = getResults();
            localDecoyResults = getDecoyResults();
            size = localResults.getNumberOfHits();
            if (localDecoyResults != null) size += localDecoyResults.getNumberOfHits();
        }

        /**
         * Loads the significant PMF (protein)
         * hits into the significantPMFIdentifications
         * list.
         */
        private void loadSignificantPMFHits() {
            if (!mascotFile.isPMF()) return;
            significantPMFIdentifications = new ArrayList<Integer>();
            ms_mascotresults results = getResults();
            Double threshold = new Double(results.getProteinThreshold(1 / (Double) getCurrentProperty(SupportedProperties.MIN_PROPABILITY)));
            for (int i = 1; i <= results.getNumberOfHits(); i++) {
                ms_protein protein = results.getHit(i);
                if (protein.getScore() > threshold) significantPMFIdentifications.add(i);
            }
        }

        @Override
        public boolean hasNext() {
            if (significantPMFIdentifications != null) return index <= significantPMFIdentifications.size();
            return index >= 1 && index <= size;
        }

        @Override
        public Identification next() {
            ms_protein protein;
            int resultIndex = 0;
            if (significantPMFIdentifications != null) {
                resultIndex = significantPMFIdentifications.get(index - 1);
                protein = localResults.getHit(resultIndex);
                isDecoyHit = false;
            } else if (index <= localResults.getNumberOfHits()) {
                protein = localResults.getHit(index);
                resultIndex = index;
                isDecoyHit = false;
            } else {
                isDecoyHit = true;
                if (localDecoyResults != null) protein = localDecoyResults.getHit(index - localResults.getNumberOfHits()); else protein = null;
                resultIndex = index - localResults.getNumberOfHits();
            }
            index++;
            if (protein == null) return null;
            Identification ident = createIdentification(protein, resultIndex, prescanMode, isDecoyHit);
            if (ident.getPeptide().size() < 1) {
                if (hasNext()) ident = next(); else ident = null;
            }
            return ident;
        }

        @Override
        public void remove() {
        }

        @Override
        public Iterator<Identification> iterator() {
            return this;
        }
    }

    /**
     * Creates an Identification object based on a ms_protein
     * object. In pre-scan mode the peptide's fragment ion
     * annotations are omitted. Furthermore, the different handlers
     * (QuantitationHandler, etc.) are only called in pre-scan mode
     * as their information should be included in the report file.
     * Additional parameters (for both peptide and protein) as well as
     * peptide PTMs are only reported in pre-scan mode.
     *
     * @param protein     The ms_protein object to create the Identification object from.
     * @param preScanMode
     * @param decoyHit
     * @return The Identification object representing the ms_protein object.
     */
    private Identification createIdentification(ms_protein protein, Integer index, boolean preScanMode, boolean decoyHit) {
        Identification ident = new Identification();
        String decoyPrefix = (String) getCurrentProperty(SupportedProperties.DECOY_PREFIX);
        if (decoyHit && decoyPrefix != null && !protein.getAccession().startsWith(decoyPrefix)) ident.setAccession(decoyPrefix + protein.getAccession()); else ident.setAccession(protein.getAccession());
        ident.setUniqueIdentifier(((decoyHit) ? "d_" : "") + index.toString());
        ident.setDatabase(mascotFile.params().getDB(protein.getDB()));
        ident.setDatabaseVersion(mascotFile.getFastaVer(protein.getDB()));
        ident.setScore(roundDouble(protein.getScore()));
        ms_mascotresults res = (decoyHit) ? getDecoyResults() : getResults();
        if (mascotFile.isPMF()) {
            Double threshold = new Double(res.getProteinThreshold(1 / (Double) getCurrentProperty(SupportedProperties.MIN_PROPABILITY)));
            ident.setThreshold(threshold);
        } else {
            Double threshold = new Double(res.getAvePeptideIdentityThreshold(1 / (Double) getCurrentProperty(SupportedProperties.MIN_PROPABILITY)));
            ident.setThreshold(threshold);
        }
        ident.setSearchEngine(searchEngineString);
        ident.getPeptide().addAll(getProteinPeptides(protein, preScanMode, decoyHit));
        if (preScanMode) {
            Param additional = new Param();
            String description = results.getProteinDescription(protein.getAccession());
            if (description.length() > 0) additional.getCvParam().add(DAOCvParams.PROTEIN_NAME.getParam(description));
            int similarHitIndex = 1;
            ms_protein simProtein = res.getNextSimilarProtein(protein.getHitNumber(), similarHitIndex);
            while (simProtein != null) {
                if (simProtein.getGrouping() == ms_protein.GROUP_COMPLETE) additional.getCvParam().add(DAOCvParams.INDISTINGUISHABLE_ACCESSION.getParam(simProtein.getAccession()));
                simProtein = res.getNextSimilarProtein(protein.getHitNumber(), ++similarHitIndex);
            }
            if (decoyHit || (decoyPrefix != null && ident.getAccession().startsWith(decoyPrefix))) additional.getCvParam().add(DAOCvParams.DECOY_HIT.getParam());
            boolean signifcantPeptides = false;
            for (Peptide peptide : ident.getPeptide()) {
                boolean isSignificant = true;
                for (CvParam param : peptide.getAdditional().getCvParam()) {
                    if (param.getAccession().equals(DAOCvParams.NON_SIGNIFICANT_PEPTIDE.getAccession())) {
                        isSignificant = false;
                        break;
                    }
                }
                if (isSignificant) {
                    signifcantPeptides = true;
                    break;
                }
            }
            if (!signifcantPeptides) additional.getCvParam().add(DAOCvParams.NON_SIGNIFICANT_PROTEIN.getParam());
            if (mascotFile.isPMF()) additional.getCvParam().add(DAOCvParams.PMF_IDENTIFICATION.getParam());
            if (mascotFile.isMSMS()) additional.getCvParam().add(DAOCvParams.MS_MS_IDENTIFICATION.getParam());
            ident.setAdditional(additional);
        }
        return ident;
    }

    /**
     * Round the double using the decimalFormat.
     *
     * @param d
     * @return
     */
    private Double roundDouble(Double d) {
        Double value = Double.valueOf(decimalFormat.format(d));
        return value;
    }

    /**
     * Checks whether a peptide is "valid" which means, checks whether the peptide
     * should be reported as part of an identification.
     * Duplicate peptides as well as insignificant peptides are being removed.
     *
     * @param protein       The ms_protein in which the peptide was identified.
     * @param peptideNumber The peptide's number within the protein.
     * @param isDecoyHit
     * @return
     */
    private boolean isPeptideValid(ms_protein protein, int peptideNumber, ms_peptide peptide, boolean isDecoyHit) {
        if (mascotFile.isPMF()) return true;
        if ((Boolean) getCurrentProperty(SupportedProperties.DUPE_SAME_QUERY) && protein.getPeptideDuplicate(peptideNumber) == ms_protein.DUPE_DuplicateSameQuery) {
            logger.debug(protein.getAccession() + " - " + peptide.getPeptideStr() + " (" + peptide.getVarModsStr() + "): Duplicate Same query");
            return false;
        }
        if ((Boolean) getCurrentProperty(SupportedProperties.DUPE_DIFF_QUERY) && protein.getPeptideDuplicate(peptideNumber) == ms_protein.DUPE_Duplicate) {
            logger.debug(protein.getAccession() + " - " + peptide.getPeptideStr() + " (" + peptide.getVarModsStr() + "): Duplicate");
            return false;
        }
        ms_mascotresults res = (isDecoyHit) ? getDecoyResults() : getResults();
        int th = 0;
        if ((Boolean) getCurrentProperty(SupportedProperties.USE_HOMOLOGY_THREHOLD)) th = res.getHomologyThreshold(peptide.getQuery(), 1 / (Double) getCurrentProperty(SupportedProperties.MIN_PROPABILITY), peptide.getRank()); else th = res.getPeptideIdentityThreshold(peptide.getQuery(), 1 / (Double) getCurrentProperty(SupportedProperties.MIN_PROPABILITY));
        if ((Boolean) getCurrentProperty(SupportedProperties.ONLY_SIGNIFICANT) && peptide.getIonsScore() < th) {
            logger.debug(protein.getAccession() + " - " + peptide.getPeptideStr() + "(" + peptide.getQuery() + " - " + peptide.getVarModsStr() + "): Not significant score: " + peptide.getIonsScore() + " < " + th);
            return false;
        }
        logger.debug(protein.getAccession() + " - " + peptide.getPeptideStr() + "(" + peptide.getQuery() + " - " + peptide.getVarModsStr() + "):--OK-- score: " + peptide.getIonsScore() + " < " + th);
        return true;
    }

    /**
     * Creates a list of peptides that are assigned to this specific
     * protein. PTMs as well as additional parameters are only
     * returned in prescan mode.
     *
     * @param protein     The ms_protein to create the peptides for.
     * @param prescanMode
     * @param decoyHit    Indicates whether the passed protein is a decoy hit.
     * @return A List of Peptides for the specific protein.
     */
    private List<Peptide> getProteinPeptides(ms_protein protein, boolean prescanMode, boolean decoyHit) {
        ArrayList<Peptide> peptides = new ArrayList<Peptide>();
        for (int i = 1; i <= protein.getNumPeptides(); i++) {
            int queryId = protein.getPeptideQuery(i);
            int rank = protein.getPeptideP(i);
            if (queryId == -1) continue;
            ms_peptide msPep = results.getPeptide(queryId, rank);
            if (!msPep.getAnyMatch()) continue;
            logger.debug("----------------------------------");
            if (!isPeptideValid(protein, i, msPep, decoyHit)) continue;
            logger.debug("Processing peptide: " + msPep.getPeptideStr());
            logger.debug("Protein Accession:  " + protein.getAccession());
            logger.debug("Query id:           " + queryId);
            logger.debug("Peaks used:         " + msPep.getPeaksUsedFromIons1());
            Peptide peptide = new Peptide();
            peptide.setSequence(msPep.getPeptideStr());
            peptide.setUniqueIdentifier(queryId + "_" + rank);
            if (mascotFile.isPMF()) peptide.setSpectrumReference(1); else peptide.setSpectrumReference(queryId);
            peptide.setIsSpecific(msPep.getNumProteins() == 1);
            peptide.setStart(protein.getPeptideStart(i));
            peptide.setEnd(protein.getPeptideEnd(i));
            if (prescanMode) {
                peptide.getPTM().addAll(createFixedPeptidePTMs(msPep));
                peptide.getPTM().addAll(createVarPeptidePTMs(msPep, prescanMode));
            }
            if (!prescanMode) {
                HashMap<String, Double> theoreticalFragments = createTheoreticalFragments(msPep.getPeptideStr(), getPeptideMassChanges(msPep, prescanMode), msPep.getSeriesUsedStr());
                Double tolerance = mascotFile.params().getITOL();
                if (mascotFile.params().getITOLU().equals("mmu")) tolerance = tolerance / 1000;
                List<FragmentIon> fragmentIons = getMatchedFragments(theoreticalFragments, new ms_inputquery(mascotFile, queryId), 1, msPep.getPeaksUsedFromIons1(), tolerance);
                peptide.getFragmentIon().addAll(fragmentIons);
            }
            if (prescanMode) {
                Param additional = new Param();
                if (mascotFile.isMSMS()) additional.getCvParam().add(DAOCvParams.MS_MS_IDENTIFICATION.getParam());
                if (mascotFile.isPMF()) {
                    additional.getCvParam().add(DAOCvParams.PMF_IDENTIFICATION.getParam());
                    additional.getCvParam().add(DAOCvParams.UNIT_MZ.getParam(mascotFile.getObservedMass(queryId)));
                }
                if (!mascotFile.isPMF()) additional.getCvParam().add(DAOCvParams.MASCOT_SCORE.getParam(new Double(msPep.getIonsScore()).toString()));
                additional.getCvParam().add(DAOCvParams.PEPTIDE_RANK.getParam(String.format("%d", rank)));
                String upStream = String.format("%c", protein.getPeptideResidueBefore(i));
                if (!"@".equals(upStream) && !"-".equals(upStream) && !"?".equals(upStream)) additional.getCvParam().add(DAOCvParams.UPSTREAM_FLANKING_SEQUENCE.getParam(upStream));
                String downStream = String.format("%c", protein.getPeptideResidueAfter(i));
                if (!"@".equals(downStream) && !"-".equals(downStream) && !"?".equals(downStream)) additional.getCvParam().add(DAOCvParams.DOWNSTREAM_FLANKING_SEQUENCE.getParam(downStream));
                additional.getCvParam().add(DAOCvParams.CHARGE_STATE.getParam(String.format("%d", msPep.getCharge())));
                if (!mascotFile.isPMF()) {
                    ms_mascotresults rs = (decoyHit) ? getDecoyResults() : getResults();
                    int th = 0;
                    if ((Boolean) getCurrentProperty(SupportedProperties.USE_HOMOLOGY_THREHOLD)) th = rs.getHomologyThreshold(msPep.getQuery(), 1 / (Double) getCurrentProperty(SupportedProperties.MIN_PROPABILITY), msPep.getRank()); else th = rs.getPeptideIdentityThreshold(msPep.getQuery(), 1 / (Double) getCurrentProperty(SupportedProperties.MIN_PROPABILITY));
                    if (msPep.getIonsScore() < th) additional.getCvParam().add(DAOCvParams.NON_SIGNIFICANT_PEPTIDE.getParam());
                }
                peptide.setAdditional(additional);
            }
            peptides.add(peptide);
        }
        return peptides;
    }

    /**
     * Returns a list of fixed PeptidePTMs.
     *
     * @param peptide The ms_peptide to create the ptms for.
     * @return List of PeptidePTMs
     */
    private List<PeptidePTM> createFixedPeptidePTMs(ms_peptide peptide) {
        List<PeptidePTM> ptms = new ArrayList<PeptidePTM>();
        ms_searchparams params = mascotFile.params();
        boolean average = params.getMASS().equals("Average");
        int modNum = 1;
        String modName;
        do {
            modName = params.getFixedModsName(modNum);
            if (modName.length() == 0) break;
            Double delta = params.getFixedModsDelta(modNum);
            Double neutralLoss = params.getFixedModsNeutralLoss(modNum);
            String residues = params.getFixedModsResidues(modNum);
            Integer index = -1;
            if (residues.equals("N_term")) index = 0; else if (residues.equals("C_term")) index = peptide.getPeptideStr().length() + 1; else index = peptide.getPeptideStr().indexOf(residues);
            while (index != -1) {
                if (!residues.equals("N_term") && !residues.equals("C_term")) index++;
                PeptidePTM ptm = new PeptidePTM();
                ptm.setFixedModification(true);
                ptm.setModLocation(index);
                ptm.setSearchEnginePTMLabel(modName);
                if (average) {
                    ptm.getModAvgDelta().add(delta.toString());
                } else {
                    ptm.getModMonoDelta().add(delta.toString());
                }
                if (neutralLoss != 0) {
                    ptm.getAdditional().getCvParam().add(DAOCvParams.NEUTRAL_LOSS.getParam(neutralLoss.toString()));
                }
                ptms.add(ptm);
                if (!residues.equals("N_term") && !residues.equals("C_term")) index = peptide.getPeptideStr().indexOf(residues, index); else break;
            }
            modNum++;
        } while (modName.length() > 0);
        return ptms;
    }

    /**
     * List of variable PTMs for the given peptide.
     *
     * @param peptide The ms_peptide to create the list for.
     * @return A list of PeptidePTMs.
     */
    private List<PeptidePTM> createVarPeptidePTMs(ms_peptide peptide, boolean isDecoyHit) {
        List<PeptidePTM> ptms = new ArrayList<PeptidePTM>();
        ms_searchparams params = mascotFile.params();
        boolean average = params.getMASS().equals("Average");
        String modString = peptide.getVarModsStr();
        String neutralLossString = peptide.getPrimaryNlStr();
        for (Integer position = 0; position < modString.length(); position++) {
            char modChar = modString.charAt(position);
            int modNumber = mascotVarPtmString.indexOf(modChar);
            if (modNumber == -1 && modChar != 'X') {
                logger.error("Invalid variable modification char '" + modChar + "' found at " + position + " in " + peptide.getPeptideStr());
                continue;
            }
            if (modNumber == 0 && modChar != 'X') continue;
            String name;
            if (modChar != 'X') name = params.getVarModsName(modNumber); else name = (isDecoyHit) ? getDecoyResults().getErrTolModName(peptide.getQuery(), peptide.getRank()) : getResults().getErrTolModName(peptide.getQuery(), peptide.getRank());
            if (name.length() < 1) {
                logger.error("Found variable modification number " + modNumber + " is not defined in the parameters");
                continue;
            }
            PeptidePTM ptm = new PeptidePTM();
            ptm.setFixedModification(false);
            ptm.setModLocation(position);
            ptm.setSearchEnginePTMLabel(name);
            ptm.setAdditional(new Param());
            Double delta;
            if (modChar != 'X') delta = params.getVarModsDelta(modNumber); else delta = (isDecoyHit) ? getDecoyResults().getErrTolModDelta(peptide.getQuery(), peptide.getRank()) : getResults().getErrTolModDelta(peptide.getQuery(), peptide.getRank());
            if (average) ptm.getModAvgDelta().add(delta.toString()); else ptm.getModMonoDelta().add(delta.toString());
            if (neutralLossString.length() > 0 && modChar != 'X') {
                vectord neutralLosses = params.getVarModsNeutralLosses(modNumber);
                char neutralLossChar = neutralLossString.charAt(position);
                Integer neutralLossInt = mascotVarPtmString.indexOf(neutralLossChar);
                Double neutralLoss = 0.0;
                if (neutralLossInt > 0) neutralLoss = neutralLosses.get(neutralLossInt - 1);
                if (neutralLoss > 0) {
                    ptm.getAdditional().getCvParam().add(DAOCvParams.NEUTRAL_LOSS.getParam(neutralLoss.toString()));
                    logger.debug("Neutral loss found: " + neutralLoss);
                }
            } else if (modChar == 'X') {
                Double neutralLoss = (isDecoyHit) ? getDecoyResults().getErrTolModNeutralLoss(peptide.getQuery(), peptide.getRank()) : getResults().getErrTolModNeutralLoss(peptide.getQuery(), peptide.getRank());
                if (neutralLoss > 0) {
                    ptm.getAdditional().getCvParam().add(DAOCvParams.NEUTRAL_LOSS.getParam(neutralLoss.toString()));
                    logger.debug("Neutral loss found: " + neutralLoss);
                }
            }
            ptms.add(ptm);
        }
        return ptms;
    }

    /**
     * Returns an array of doubles the size of the peptide's sequence +2
     * (length) indicating the mass change for the given AA + termini caused by
     * variable modifications.
     *
     * @param peptide The peptide to create the massChangeArray for.
     * @return Double array the length of the peptide sequence.
     */
    private double[] getPeptideMassChanges(ms_peptide peptide, boolean isDecoyHit) {
        double[] massChanges = new double[peptide.getPeptideStr().length() + 2];
        for (int i = 0; i < massChanges.length; i++) massChanges[i] = 0.0;
        ms_searchparams params = mascotFile.params();
        String modString = peptide.getVarModsStr();
        String neutralLossString = peptide.getPrimaryNlStr();
        for (Integer position = 0; position < modString.length(); position++) {
            char modChar = modString.charAt(position);
            int modNumber = mascotVarPtmString.indexOf(modChar);
            if (modChar == 'X') {
                Double delta = (isDecoyHit) ? getDecoyResults().getErrTolModDelta(peptide.getQuery(), peptide.getRank()) : getResults().getErrTolModDelta(peptide.getQuery(), peptide.getRank());
                if (delta > 0) massChanges[position] = delta;
                Double neutralLoss = (isDecoyHit) ? getDecoyResults().getErrTolModNeutralLoss(peptide.getQuery(), peptide.getRank()) : getResults().getErrTolModNeutralLoss(peptide.getQuery(), peptide.getRank());
                ;
                if (neutralLoss > 0) massChanges[position] -= neutralLoss;
                continue;
            }
            if (modNumber == -1) {
                logger.error("Invalid variable modification char '" + modChar + "' found at " + position + " in " + peptide.getPeptideStr());
                continue;
            }
            if (modNumber == 0) continue;
            String name = params.getVarModsName(modNumber);
            if (name.length() < 1) {
                logger.error("Found variable modification number " + modNumber + " is not defined in the parameters");
                continue;
            }
            massChanges[position] = params.getVarModsDelta(modNumber);
            if (neutralLossString.length() > 0) {
                vectord neutralLosses = params.getVarModsNeutralLosses(modNumber);
                char neutralLossChar = neutralLossString.charAt(position);
                Integer neutralLossInt = mascotVarPtmString.indexOf(neutralLossChar);
                Double neutralLoss = 0.0;
                if (neutralLossInt > 0) neutralLoss = neutralLosses.get(neutralLossInt - 1);
                if (neutralLoss > 0) {
                    massChanges[position] -= neutralLoss;
                }
            }
        }
        return massChanges;
    }

    @Override
    public void setConfiguration(Properties props) {
        properties = props;
        results = null;
        decoyResults = null;
    }

    @Override
    public Properties getConfiguration() {
        return properties;
    }

    @Override
    public Protocol getProtocol() {
        return null;
    }

    @Override
    public Collection<Reference> getReferences() {
        return null;
    }

    /**
     * This functions returns a ms_mascotresults object that provides
     * access to the protein / peptide identifications. As creating
     * this object is very time-consuming it's only created once. This
     * is checked by this function.
     *
     * @return A ms_mascotresults object providing access to the peptide / protein identifications of the result file.
     */
    private ms_mascotresults getResults() {
        if (results != null) return results;
        if (mascotFile.isPMF()) {
            results = new ms_proteinsummary(mascotFile, getProteinSummaryFlrags(), (Double) getCurrentProperty(SupportedProperties.MIN_PROPABILITY), 10000000, null, null);
        } else {
            results = new ms_peptidesummary(mascotFile, getPeptideSummaryFlags(), (Double) getCurrentProperty(SupportedProperties.MIN_PROPABILITY), 10000000, null, (Double) getCurrentProperty(SupportedProperties.IGNORE_BELOW_SCORE), 0, null);
        }
        return results;
    }

    /**
     * Returns the decoy peptide summary - if a decoy database was used.
     * If no decoy database was used, null is returned.
     * For PMF queries null is always returned.
     *
     * @return The ms_mascotresults for the decoy database results. Null if no decoy database was searched
     */
    private ms_mascotresults getDecoyResults() {
        if (decoyResults != null) return decoyResults;
        if (mascotFile.params().getDECOY() != 1) return null;
        if (mascotFile.isPMF()) return null;
        decoyResults = new ms_peptidesummary(mascotFile, getPeptideSummaryFlags() | ms_mascotresults.MSRES_DECOY, (Double) getCurrentProperty(SupportedProperties.MIN_PROPABILITY), 10000000, null, (Double) getCurrentProperty(SupportedProperties.IGNORE_BELOW_SCORE), 0, null);
        return decoyResults;
    }

    /**
     * Returns the peptide summary flags to use
     *
     * @return
     */
    private int getPeptideSummaryFlags() {
        int flags = 0;
        flags = flags | ms_mascotresults.MSRES_DUPE_DEFAULT;
        if ((Boolean) getCurrentProperty(SupportedProperties.USE_MUDPIT_SCORING)) flags = flags | ms_mascotresults.MSRES_MUDPIT_PROTEIN_SCORE;
        if ((Boolean) getCurrentProperty(SupportedProperties.INCLUDE_ERR_TOL)) flags = flags | ms_mascotresults.MSRES_INTEGRATED_ERR_TOL;
        if ((Boolean) getCurrentProperty(SupportedProperties.ENABLE_GROUPING)) flags = flags | ms_mascotresults.MSRES_GROUP_PROTEINS;
        return flags;
    }

    /**
     * Returns the flags for a protein summary object.
     * @return
     */
    private int getProteinSummaryFlrags() {
        int flags = 0;
        if ((Boolean) getCurrentProperty(SupportedProperties.ENABLE_GROUPING)) flags = flags | ms_mascotresults.MSRES_GROUP_PROTEINS;
        return flags;
    }

    /**
     * Returns the matched fragments for a given spectrum (= ms_inputquery).
     *
     * @param theoreticalFragments A HashMap holding the theoretical fragments for the given spectrum.
     * @param query                The spectrum (=ms_inputquery) used to retrieve the result.
     * @param ionSeries            The used ionSeries (generally 1)
     * @param numPeaksUsed         The number of peaks used by mascot.
     * @param tolerance            The tolerance to use in Dalton.
     * @return A List<FragmentIon> holding all the FragmentIons identified.
     */
    private List<FragmentIon> getMatchedFragments(HashMap<String, Double> theoreticalFragments, ms_inputquery query, int ionSeries, int numPeaksUsed, double tolerance) {
        HashMap<String, FragmentIon> fragmentIons = new HashMap<String, FragmentIon>();
        HashMap<String, Double> fragmentIntensities = new HashMap<String, Double>();
        logger.debug("\t----- Fragment Ions for Query " + query.getIndex() + " ----------");
        for (int nPeak = 0; nPeak < numPeaksUsed; nPeak++) {
            Double mz = query.getPeakMass(ionSeries, nPeak);
            Double intens = query.getPeakIntensity(ionSeries, nPeak);
            for (String ion : theoreticalFragments.keySet()) {
                double theoreticalMz = theoreticalFragments.get(ion);
                if (theoreticalMz >= mz - tolerance && theoreticalMz <= mz + tolerance) {
                    if (fragmentIntensities.containsKey(ion) && fragmentIntensities.get(ion) > intens) {
                        continue;
                    }
                    FragmentIon fragmentIon = new FragmentIon();
                    String productIonCharge = (ion.contains("++")) ? "2" : "1";
                    fragmentIon.getCvParam().add(DAOCvParams.PRODUCT_ION_CHARGE.getParam(productIonCharge));
                    fragmentIon.getCvParam().add(DAOCvParams.PRODUCT_ION_INTENSITY.getParam(intens));
                    fragmentIon.getCvParam().add(DAOCvParams.PRODUCT_ION_MZ.getParam(mz));
                    Double massError = theoreticalMz - mz;
                    fragmentIon.getCvParam().add(DAOCvParams.PRODUCT_ION_MASS_ERROR.getParam(massError));
                    CvParam name = getIonName(ion);
                    if (name != null) fragmentIon.getCvParam().add(name);
                    fragmentIons.put(ion, fragmentIon);
                    fragmentIntensities.put(ion, intens);
                    logger.debug("\t" + ion + ": " + mz);
                }
            }
        }
        return new ArrayList<FragmentIon>(fragmentIons.values());
    }

    /**
     * Returns the given ion name CvParam. If the ion isn't known null
     * is returned.
     *
     * @param ion The ion as a string (f.e. y++, y°, b*++)
     * @return The name CvParam for the given ion. Null if the ion name isn't known.
     */
    private CvParam getIonName(String ion) {
        String position = ion.replaceAll("[^0-9]", "");
        if (ion.startsWith("y°")) return new CvParam("PRIDE", "PRIDE:0000197", "y ion -H2O", position);
        if (ion.startsWith("y*")) return new CvParam("PRIDE", "PRIDE:0000198", "y ion -NH3", position);
        if (ion.startsWith("y")) return new CvParam("PRIDE", "PRIDE:0000193", "y ion", position);
        if (ion.startsWith("b°")) return new CvParam("PRIDE", "PRIDE:0000196", "b ion -H2O", position);
        if (ion.startsWith("b*")) return new CvParam("PRIDE", "PRIDE:0000195", "b ion -NH3", position);
        if (ion.startsWith("b")) return new CvParam("PRIDE", "PRIDE:0000194", "b ion", position);
        if (ion.startsWith("c°")) return new CvParam("PRIDE", "PRIDE:0000237", "c ion -H2O", position);
        if (ion.startsWith("c*")) return new CvParam("PRIDE", "PRIDE:0000238", "c ion -NH3", position);
        if (ion.startsWith("c")) return new CvParam("PRIDE", "PRIDE:0000236", "c ion", position);
        if (ion.startsWith("a°")) return new CvParam("PRIDE", "PRIDE:0000234", "a ion -H2O", position);
        if (ion.startsWith("a*")) return new CvParam("PRIDE", "PRIDE:0000235", "a ion -NH3", position);
        if (ion.startsWith("a")) return new CvParam("PRIDE", "PRIDE:0000233", "a ion", position);
        if (ion.startsWith("x°")) return new CvParam("PRIDE", "PRIDE:0000228", "x ion -H2O", position);
        if (ion.startsWith("x*")) return new CvParam("PRIDE", "PRIDE:0000229", "x ion -NH3", position);
        if (ion.startsWith("x")) return new CvParam("PRIDE", "PRIDE:0000227", "x ion", position);
        if (ion.startsWith("z°")) return new CvParam("PRIDE", "PRIDE:0000231", "z ion -H2O", position);
        if (ion.startsWith("z*")) return new CvParam("PRIDE", "PRIDE:0000232", "z ion -NH3", position);
        if (ion.startsWith("zh++")) return new CvParam("PRIDE", "PRIDE:0000281", "zHH ion", position);
        if (ion.startsWith("zh")) return new CvParam("PRIDE", "PRIDE:0000280", "zH ion", position);
        if (ion.startsWith("z")) return new CvParam("PRIDE", "PRIDE:0000230", "z ion", position);
        return null;
    }

    /**
     * Calculates the theoretical fragment masses for the given series and
     * massChanges. The massChanges should only represent variable modifications
     * as fixed modifications are automatically taken into consideration.
     *
     * @param sequence    The peptide's sequence.
     * @param massChanges An array of mass changes. This array has to have the same size as the peptide length + 2 as it has to have one value for every AA in the peptide + the termini.
     * @param seriesUsed  The string representing which ion series was used. Only scoring series are taken into consideration.
     * @return A HashMap with the fragments name (y, y++, y* - ammonia loss, y*++, y° - water loss, y°++, b++ ...) as key and the mass as value
     */
    private HashMap<String, Double> createTheoreticalFragments(String sequence, double[] massChanges, String seriesUsed) {
        HashMap<String, Double> fragments = new HashMap<String, Double>();
        logger.debug("--CreateTheoreticalFragments");
        logger.debug("\tseriesUsed: " + seriesUsed);
        if (seriesUsed.charAt(3) == '2') fragments.putAll(createTheoreticalBSeries(sequence, massChanges, seriesUsed.charAt(5) == '2'));
        if (seriesUsed.charAt(6) == '2') fragments.putAll(createTheoreticalYSeries(sequence, massChanges, seriesUsed.charAt(8) == '2'));
        if (seriesUsed.charAt(0) == '2') fragments.putAll(createTheoreticalASeries(sequence, massChanges, seriesUsed.charAt(2) == '2'));
        if (seriesUsed.charAt(9) == '2') fragments.putAll(createTheoreticalCSeries(sequence, massChanges, seriesUsed.charAt(10) == '2'));
        if (seriesUsed.charAt(11) == '2') fragments.putAll(createTheoreticalXSeries(sequence, massChanges, seriesUsed.charAt(12) == '2'));
        if (seriesUsed.charAt(13) == '2') fragments.putAll(createTheoreticalZSeries(sequence, massChanges, seriesUsed.charAt(14) == '2'));
        if (seriesUsed.charAt(15) == '2') fragments.putAll(createTheoreticalZHSeries(sequence, massChanges, seriesUsed.charAt(16) == '2'));
        return fragments;
    }

    /**
     * Calculates the theoretical y series masses for the given series and
     * massChanges. The massChanges should only represent variable modifications
     * as fixed modifications are automatically taken into consideration.
     *
     * @param sequence            The peptide's sequence.
     * @param massChanges         An array of mass changes. This array has to have the same size as the peptide length +2 as it has to have one value for every AA + termini in the peptide.
     * @param includeDoubleCharge Boolean indicating whether the double charged ions should be included
     * @return A HashMap with the fragments name (y, y++, y* - ammonia loss, y*++, y° - water loss, y°++) as key and the mass as value
     */
    private HashMap<String, Double> createTheoreticalYSeries(String sequence, double[] massChanges, boolean includeDoubleCharge) {
        HashMap<String, Double> fragments = new HashMap<String, Double>();
        ms_searchparams params = mascotFile.params();
        int nY = 1;
        for (int position = sequence.length() - 1; position > 0; position--, nY++) {
            String residues = sequence.substring(position);
            double mass = 0.0;
            for (int i = 0; i < residues.length(); i++) mass += params.getResidueMass(residues.charAt(i));
            for (int i = 0; i < residues.length(); i++) mass += massChanges[sequence.length() - i];
            mass += params.getCTermMass();
            mass += massChanges[sequence.length() + 1];
            mass += params.getHydrogenMass();
            fragments.put("y" + nY, mass + 1);
            if (includeDoubleCharge) fragments.put("y++" + nY, mass / 2 + 1);
        }
        return fragments;
    }

    private HashMap<String, Double> createTheoreticalBSeries(String sequence, double[] massChanges, boolean includeDoubleCharge) {
        HashMap<String, Double> fragments = new HashMap<String, Double>();
        ms_searchparams params = mascotFile.params();
        for (int position = 0; position < sequence.length() - 1; position++) {
            String residues = sequence.substring(0, position + 1);
            double mass = 0.0;
            for (int i = 0; i < residues.length(); i++) {
                mass += params.getResidueMass(residues.charAt(i));
                mass += massChanges[i + 1];
            }
            mass += params.getNTermMass();
            mass += massChanges[0];
            mass -= params.getHydrogenMass();
            fragments.put("b" + (position + 1), mass + 1);
            if (includeDoubleCharge) fragments.put("b++" + (position + 1), mass / 2 + 1);
        }
        return fragments;
    }

    private HashMap<String, Double> createTheoreticalASeries(String sequence, double[] massChanges, boolean includeDoubleCharge) {
        HashMap<String, Double> fragments = new HashMap<String, Double>();
        ms_searchparams params = mascotFile.params();
        for (int position = 0; position < sequence.length() - 1; position++) {
            String residues = sequence.substring(0, position + 1);
            double mass = 0.0;
            for (int i = 0; i < residues.length(); i++) {
                mass += params.getResidueMass(residues.charAt(i));
                mass += massChanges[i + 1];
            }
            mass += params.getNTermMass();
            mass += massChanges[0];
            mass -= params.getCarbonMass() - params.getHydrogenMass() - params.getOxygenMass();
            fragments.put("a" + (position + 1), mass + 1);
            if (includeDoubleCharge) fragments.put("a++" + (position + 1), mass / 2 + 1);
        }
        return fragments;
    }

    private HashMap<String, Double> createTheoreticalCSeries(String sequence, double[] massChanges, boolean includeDoubleCharge) {
        HashMap<String, Double> fragments = new HashMap<String, Double>();
        ms_searchparams params = mascotFile.params();
        for (int position = 0; position < sequence.length() - 1; position++) {
            String residues = sequence.substring(0, position + 1);
            double mass = 0.0;
            for (int i = 0; i < residues.length(); i++) {
                mass += params.getResidueMass(residues.charAt(i));
                mass += massChanges[i + 1];
            }
            mass += params.getNTermMass();
            mass += massChanges[0];
            mass += params.getNitrogenMass() + params.getHydrogenMass() * 2;
            fragments.put("c" + (position + 1), mass + 1);
            if (includeDoubleCharge) fragments.put("c++" + (position + 1), mass / 2 + 1);
        }
        return fragments;
    }

    private HashMap<String, Double> createTheoreticalXSeries(String sequence, double[] massChanges, boolean includeDoubleCharge) {
        HashMap<String, Double> fragments = new HashMap<String, Double>();
        ms_searchparams params = mascotFile.params();
        int nX = 1;
        for (int position = sequence.length() - 1; position > 0; position--, nX++) {
            String residues = sequence.substring(position);
            double mass = 0.0;
            for (int i = 0; i < residues.length(); i++) mass += params.getResidueMass(residues.charAt(i));
            for (int i = 0; i < residues.length(); i++) mass += massChanges[sequence.length() - i];
            mass += params.getCTermMass();
            mass += massChanges[sequence.length() + 1];
            mass += params.getCarbonMass() + params.getOxygenMass() - params.getHydrogenMass();
            fragments.put("x" + nX, mass + 1);
            if (includeDoubleCharge) fragments.put("x++" + nX, mass / 2 + 1);
        }
        return fragments;
    }

    private HashMap<String, Double> createTheoreticalZSeries(String sequence, double[] massChanges, boolean includeDoubleCharge) {
        HashMap<String, Double> fragments = new HashMap<String, Double>();
        ms_searchparams params = mascotFile.params();
        int nZ = 1;
        for (int position = sequence.length() - 1; position > 0; position--, nZ++) {
            String residues = sequence.substring(position);
            double mass = 0.0;
            for (int i = 0; i < residues.length(); i++) mass += params.getResidueMass(residues.charAt(i));
            for (int i = 0; i < residues.length(); i++) mass += massChanges[sequence.length() - i];
            mass += params.getCTermMass();
            mass += massChanges[sequence.length() + 1];
            mass -= params.getNitrogenMass() - params.getHydrogenMass() * 2;
            fragments.put("z" + nZ, mass + 1);
            if (includeDoubleCharge) fragments.put("z++" + nZ, mass / 2 + 1);
        }
        return fragments;
    }

    private HashMap<String, Double> createTheoreticalZHSeries(String sequence, double[] massChanges, boolean includeDoubleCharge) {
        HashMap<String, Double> fragments = new HashMap<String, Double>();
        ms_searchparams params = mascotFile.params();
        int nZ = 1;
        for (int position = sequence.length() - 1; position > 0; position--, nZ++) {
            String residues = sequence.substring(position);
            double mass = 0.0;
            for (int i = 0; i < residues.length(); i++) mass += params.getResidueMass(residues.charAt(i));
            for (int i = 0; i < residues.length(); i++) mass += massChanges[sequence.length() - i];
            mass += params.getCTermMass();
            mass += massChanges[sequence.length() + 1];
            mass -= params.getNitrogenMass() - params.getHydrogenMass() * 2 + params.getHydrogenMass();
            fragments.put("zh" + nZ, mass + 1);
            if (includeDoubleCharge) fragments.put("zh++" + nZ, mass / 2 + 1);
        }
        return fragments;
    }

    /**
     * Calculates the FDR for the used result file under the
     * set thresholds. This function only uses the identity
     * or homolgy threshold for its calculation.
     *
     * @return The FDR for the set probability. Null in case there was no decoy search performed.
     */
    private Double getFDR() {
        if (mascotFile.isPMF()) return null;
        ms_mascotresults results = getResults();
        if (mascotFile.params().getDECOY() != 1) return null;
        Double probability = (Double) getCurrentProperty(SupportedProperties.MIN_PROPABILITY);
        Double oneInXProb = (probability <= 1) ? 1 / probability : probability;
        Double hits = 0.0;
        Double decoyHits = 0.0;
        if ((Boolean) getCurrentProperty(SupportedProperties.USE_HOMOLOGY_THREHOLD)) {
            hits = (double) results.getNumHitsAboveHomology(oneInXProb);
            decoyHits = (double) results.getNumDecoyHitsAboveHomology(oneInXProb);
        } else {
            hits = (double) results.getNumHitsAboveIdentity(oneInXProb);
            decoyHits = (double) results.getNumDecoyHitsAboveIdentity(oneInXProb);
        }
        return decoyHits / hits;
    }
}
