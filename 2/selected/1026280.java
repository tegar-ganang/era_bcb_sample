package apollo.dataadapter.analysis;

import java.net.URL;
import java.net.MalformedURLException;
import java.util.*;
import java.io.*;
import apollo.datamodel.*;
import apollo.config.Config;
import apollo.config.FeatureProperty;
import apollo.config.PropertyScheme;
import apollo.config.TierProperty;
import apollo.dataadapter.AbstractApolloAdapter;
import apollo.dataadapter.ApolloDataAdapterI;
import apollo.dataadapter.ApolloAdapterException;
import apollo.dataadapter.DataInputType;
import apollo.dataadapter.DataInputType.UnknownTypeException;
import apollo.analysis.filter.AnalysisFilterI;
import apollo.seq.io.FastaFile;
import apollo.analysis.filter.AnalysisInput;
import org.apache.log4j.*;
import org.bdgp.io.*;
import org.bdgp.util.*;

public class AnalysisAdapter extends AbstractApolloAdapter {

    protected static final Logger logger = LogManager.getLogger(AnalysisAdapter.class);

    private AnalysisParserI parser;

    private AnalysisFilterI filter;

    private AnalysisInput analysis_input;

    SequenceI curated_seq = null;

    public AnalysisAdapter() {
        setName("Computational analysis results");
    }

    IOOperation[] supportedOperations = { ApolloDataAdapterI.OP_READ_DATA, ApolloDataAdapterI.OP_APPEND_DATA };

    public void init() {
    }

    /**
   * org.bdgp.io.DataAdapter method
   */
    public String getType() {
        return "Computational analysis results (filename or URL)";
    }

    public IOOperation[] getSupportedOperations() {
        return supportedOperations;
    }

    public DataAdapterUI getUI(IOOperation op) {
        if (!super.operationIsSupported(op)) return null; else {
            DataAdapterUI ui = super.getCachedUI(op);
            if (ui == null) {
                ui = new AnalysisAdapterGUI(op, curation_set);
                super.cacheUI(op, ui);
            }
            return ui;
        }
    }

    public Properties getStateInformation() {
        Properties props = new Properties();
        props.put("inputType", getInputType().toString());
        props.put("input", getInput());
        return props;
    }

    /** DataLoader calls this */
    public void setStateInformation(Properties props) {
        try {
            setInputType(DataInputType.stringToType(props.getProperty("inputType")));
        } catch (UnknownTypeException e) {
            logger.error(e.getMessage() + " Can not set analysis state info", e);
        }
        setInput(props.getProperty("input"));
    }

    public void setAnalysisInput(AnalysisInput input) {
        this.analysis_input = input;
    }

    public void setParser(AnalysisParserI parser) {
        this.parser = parser;
    }

    public void setFilter(AnalysisFilterI filter) {
        this.filter = filter;
    }

    public void setRegion(String seq_file) throws ApolloAdapterException {
        if (seq_file != null) {
            try {
                FastaFile ff = new FastaFile(seq_file, "File");
                setRegion((SequenceI) ff.getSeqs().elementAt(0));
            } catch (IOException e) {
                logger.error("IOException caught reading sequence file " + seq_file + " " + e, e);
            }
        }
    }

    public void setRegion(SequenceI seq) throws ApolloAdapterException {
        if (seq != null) {
            this.curated_seq = seq;
            this.region = seq.getName();
        }
    }

    /** from ApolloDataAdapterI interface. input type and input should be set
   * previous to this.
   * This is called when loading analysis results as a fresh curation set.
   */
    public CurationSet getCurationSet() throws ApolloAdapterException {
        logger.debug("AnalysisAdapter.getCurationSet: reading analysis results into fresh curation set");
        CurationSet curation = new CurationSet();
        if (curated_seq != null) {
            curation.setRefSequence(curated_seq);
            logger.info("Current curated_seq is " + curated_seq.getName() + " (" + curated_seq.getLength() + " bases)");
        }
        curation.setAnnots(new StrandedFeatureSet(new FeatureSet(), new FeatureSet()));
        try {
            InputStream analysis_stream = getInputStream(getInputType(), getInput());
            String analysis_type = parser.load(curation, true, analysis_stream, analysis_input);
            logger.debug("Loaded analysis results--type = " + analysis_type);
            if (analysis_type != null) {
                super.clearOldData();
                findOrCreateTier(analysis_type);
                if (filter != null) filter.cleanUp(curation, analysis_type, analysis_input);
            }
            System.gc();
        } catch (ApolloAdapterException dae) {
            throw new ApolloAdapterException(dae.getMessage());
        } catch (Exception ex2) {
            throw new ApolloAdapterException(ex2.getMessage());
        }
        return curation;
    }

    /** from ApolloDataAdapterI interface. input type and input should be set
   * previous to this.
   * This is used when adding analysis results to already-loaded curation set.
   */
    public Boolean addToCurationSet() throws ApolloAdapterException {
        boolean okay = false;
        try {
            InputStream analysis_stream = getInputStream(getInputType(), getInput());
            String analysis_type = parser.load(curation_set, false, analysis_stream, analysis_input);
            logger.debug("addToCurationSet: loaded analysis results--type = " + analysis_type);
            if (analysis_type != null) {
                findOrCreateTier(analysis_type);
                if (filter != null) filter.cleanUp(curation_set, analysis_type, analysis_input);
                okay = true;
            }
        } catch (ApolloAdapterException dae) {
            throw new ApolloAdapterException(dae.getMessage());
        } catch (Exception ex2) {
            throw new ApolloAdapterException(ex2.getMessage());
        }
        return (new Boolean(okay));
    }

    private void findOrCreateTier(String analysis_type) {
        PropertyScheme scheme = Config.getPropertyScheme();
        String tier = analysis_input.getTier();
        String type = analysis_input.getType();
        logger.debug("Looking for tier " + tier + " for type " + type);
        TierProperty tp = scheme.getTierProperty(tier, false);
        if (tp == null) {
            logger.info("Creating new tier " + tier + " and type " + type);
            int max_rows = analysis_input.getMaxCover();
            if (max_rows < 0) max_rows = 10;
            tp = new TierProperty(tier, true, true, true, max_rows, false);
            scheme.addTierType(tp);
            Vector types = new Vector();
            types.addElement(analysis_type);
            FeatureProperty fp = new FeatureProperty(tp, type, types);
            fp = scheme.getFeatureProperty(analysis_type);
            tp = scheme.getTierProperty(tier);
            logger.debug("Wanted tier=" + tier + " and type=" + type + " got tier=" + tp.getLabel() + " and type=" + fp.getDisplayType());
        } else {
            FeatureProperty fp = tp.getFeatureProperty(type);
            if (fp == null) {
                Vector types = new Vector();
                types.addElement(analysis_type);
                fp = new FeatureProperty(tp, type, types);
            } else if (tp.featureForAnalysisType(analysis_type) == null) {
                fp.addAnalysisType(analysis_type);
            }
        }
        tp.setVisible(true);
    }

    private InputStream getInputStream(DataInputType type, String input) throws ApolloAdapterException {
        InputStream stream = null;
        if (type == DataInputType.FILE) {
            stream = getStreamFromFile(input);
        } else if (type == DataInputType.URL) {
            stream = getStreamFromUrl(getUrlFromString(input), "URL " + input + " not found");
        }
        return stream;
    }

    /** Makes InputStream from URL - throws DataAdapterException with
   * notFoundMessage if URL is not found */
    private InputStream getStreamFromUrl(URL url, String notFoundMessage) throws ApolloAdapterException {
        InputStream stream = null;
        if (url == null) {
            String message = "Couldn't find url for " + getInput();
            logger.error(message);
            throw new ApolloAdapterException(message);
        }
        if (url != null) {
            try {
                logger.info("Trying to open url " + url);
                stream = url.openStream();
                logger.debug("Succesfully opened url " + url);
            } catch (IOException e) {
                logger.error(notFoundMessage, e);
                stream = null;
                throw new ApolloAdapterException(notFoundMessage);
            }
        }
        return stream;
    }

    private InputStream getStreamFromFile(String filename) throws ApolloAdapterException {
        InputStream stream = null;
        try {
            stream = new FileInputStream(filename);
        } catch (Exception e) {
            stream = null;
            String rootdir = System.getProperty("APOLLO_ROOT");
            if (!(filename.startsWith("/")) && !(filename.startsWith("\\")) && !filename.startsWith(rootdir)) {
                String absolute = rootdir + "/" + filename;
                try {
                    InputStream newstream = getStreamFromFile(absolute);
                    return (newstream);
                } catch (ApolloAdapterException e2) {
                    absolute = rootdir + "/data/" + filename;
                    try {
                        InputStream newstream = getStreamFromFile(absolute);
                        return (newstream);
                    } catch (ApolloAdapterException e3) {
                        throw new ApolloAdapterException("Error: could not open file " + filename + " for reading.");
                    }
                }
            }
            throw new ApolloAdapterException("Error: could not open " + getInput() + " for reading.");
        }
        return stream;
    }

    private URL getUrlFromString(String urlString) {
        URL url;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException ex) {
            logger.error("caught exception creating URL " + urlString, ex);
            return (null);
        }
        return (url);
    }
}
