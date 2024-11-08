package org.ttalbott.mytelly;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
*
* @author  Tom Talbott
* @version
*/
public class Config extends java.lang.Object {

    private static final String COLOR = "color";

    private static final String CATEGORY = "category";

    private static final String CATEGORYCOLORS = "categorycolors";

    /**
	 * @author Tom Talbott
	 *
	 * Store the color and the string value that represents it.
	 */
    public static class ColorValue {

        public Color color;

        public String strColor;

        public ColorValue(String strColor) {
            this.color = ColorUtil.stringToColor(strColor);
            this.strColor = strColor;
        }

        public ColorValue(Color color) {
            this.strColor = ColorUtil.colorToHex(color);
            this.color = color;
        }

        public String toString() {
            return strColor;
        }
    }

    public static final String WEBSERVICE = "webservice";

    public static final String PASSWORD = "password";

    public static final String USERID = "userid";

    public static final String ZAPWS = "ZapWebService";

    public static final String SELECTED = "selected";

    public static final String CONFIG = "config";

    public static final String ZIPCODE = "zipcode";

    public static final String POSTALCODE = "postalcode";

    public static final String PROVIDER = "provider";

    public static final String CHANNELS = "channels";

    public static final String CHANNEL = "channel";

    public static final String SEARCHES = "searches";

    public static final String SEARCH = "search";

    public static final String DESC = "desc";

    public static final String IN = "in";

    public static final String NAME = "name";

    public static final String TYPE = "type";

    public static final String KEYWORDS = "keywords";

    public static final String CATEGORYTEXT = "searchcat";

    public static final String FIRSTONLY = "firstonly";

    public static final String TITLESONLY = "titlesonly";

    public static final String CHANNELTEXT = "searchch";

    public static final String DEBUG = "debug";

    public static final String EXPERIMENTAL = "experimental";

    public static final String FIRSTRUNBOLD = "firstrunbold";

    public static final String FIRSTRUNDATE = "firstrundate";

    public static final String PRUNEPROGRAMS = "pruneprograms";

    public static final String CACHEWARN = "cachewarn";

    public static final String KEY = "key";

    public static final String PROVIDERS = "providers";

    public static final String PROVID = "provID";

    public static final String RESULTS = "results";

    public static final String PROGRAM = "programme";

    public static final String START = "start";

    public static final String SCRLOC = "screen";

    public static final String XLOC = "xloc";

    public static final String YLOC = "yloc";

    public static final String WIDTH = "width";

    public static final String HEIGHT = "height";

    public static final String YES = "yes";

    public static final String NO = "no";

    public static final String DAYS = "days";

    public static final String TIMEOUT = "timeout";

    public static final String RETRY = "retry";

    public static final String HTTP = "scraper";

    public static final String GRABBERS = "grabbers";

    public static final String GRABBER = "grabber";

    public static final String ZAPSCRAPER = "ZapScraper";

    public static final String LOCALGRABBER = "localgrabber";

    public static final String TVPI = "tvpi";

    public static final String GMT = "GMT";

    public static final String DEFAULTFILE = "config.xml";

    private static final int DEFAULT_CACHED_DAYS_WARNING = 2;

    private int m_cachedDaysWarn = DEFAULT_CACHED_DAYS_WARNING;

    private static final int DEFAULT_OMIT_PROGRAMS_PRIOR = 4;

    private int omitProgramsPrior = DEFAULT_OMIT_PROGRAMS_PRIOR;

    private static final boolean DEFAULT_TVPI_GMT = true;

    private static final boolean DEFAULT_EXPERIMENTAL = false;

    private static final boolean DEFAULT_FIRSTRUNBOLD = true;

    private static final boolean DEFAULT_FIRSTRUNDATE = true;

    private Map m_grabbers = null;

    private boolean m_debug;

    private boolean m_experimental = DEFAULT_EXPERIMENTAL;

    private boolean m_firstrunbold = DEFAULT_FIRSTRUNBOLD;

    private boolean m_firstrundate = DEFAULT_FIRSTRUNDATE;

    private TreeMap m_channels = null;

    /** Search map:
     *      name => [ TYPE => type,
     *                KEYWORDS => [keywords,...] ]
     */
    private TreeMap m_searches = null;

    private Map m_providers = null;

    private Dimension m_appSize = null;

    private Point m_appLocation = null;

    private String m_filename;

    private boolean m_tvpi_gmt = DEFAULT_TVPI_GMT;

    private String m_selectedGrabber = ZAPWS;

    private HashMap m_catColors = new HashMap();

    private static final String[][] m_defaultCatColors = { { "Hidden Programs", "Silver" }, { "Movie", "Blue" }, { "Sports", "Red" }, { "Short Film", "Blue" }, { "Series", "Black" }, { "Special", "Fuchsia" }, { "Limited Series", "Teal" }, { "Miniseries", "Teal" }, { "Paid Programming", "Silver" } };

    /** Creates new Config */
    private Config(String file) {
        m_filename = file;
        clear();
        setupInternalGrabbers();
        initCategoryColors();
    }

    /**
	 * 
	 */
    private void initCategoryColors() {
        for (int i = 0; i < m_defaultCatColors.length; i++) {
            setCategoryColor(m_defaultCatColors[i][0].toUpperCase(), m_defaultCatColors[i][1]);
        }
    }

    public HashMap getDefaultCategoryColors() {
        HashMap retVal = new HashMap();
        for (int i = 0; i < m_defaultCatColors.length; i++) {
            retVal.put(m_defaultCatColors[i][0].toUpperCase(), new ColorValue(m_defaultCatColors[i][1]));
        }
        return retVal;
    }

    public static String[] getCategoryNames() {
        String[] retVal = new String[m_defaultCatColors.length];
        for (int i = 0; i < m_defaultCatColors.length; i++) {
            if (i == 0) retVal[0] = ProgramData.ALL_CATS; else retVal[i] = m_defaultCatColors[i][0];
        }
        return retVal;
    }

    public static Config newConfig() {
        return newConfig(DEFAULTFILE);
    }

    public static Config newConfig(java.lang.String filename) {
        return new Config(filename);
    }

    public static Config readConfig() throws IOException {
        return readConfig(DEFAULTFILE);
    }

    public static Config readConfig(String file) throws IOException {
        boolean validation = false;
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setValidating(validation);
        XMLReader xmlReader = null;
        try {
            SAXParser saxParser = spf.newSAXParser();
            xmlReader = saxParser.getXMLReader();
        } catch (Exception ex) {
            System.err.println("SAXParser Exception #1:" + ex);
            ex.printStackTrace();
            return null;
        }
        xmlReader.setErrorHandler(new MyErrorHandler(System.err));
        Config config = new Config(file);
        xmlReader.setContentHandler(new ConfigHandler(config));
        try {
            xmlReader.parse(convertToFileURL(file));
        } catch (SAXException se) {
            System.err.println("SAXParser SAXException #2:" + se.getMessage());
            se.printStackTrace();
        } catch (IOException ioe) {
            System.err.println("SAXParser IOException #3:" + ioe);
            ioe.printStackTrace();
        }
        return config;
    }

    /**
     * Convert from a filename to a file URL.
     */
    private static String convertToFileURL(String filename) {
        String path = null;
        try {
            path = new File(filename).toURL().toString();
        } catch (java.net.MalformedURLException mue) {
            System.err.println("File malformed exception:" + mue.getMessage());
            mue.printStackTrace();
        }
        ;
        return path;
    }

    private static class MyErrorHandler implements ErrorHandler {

        /** Error handler output goes here */
        private PrintStream out;

        MyErrorHandler(PrintStream out) {
            this.out = out;
        }

        /**
         * Returns a string describing parse exception details
         */
        private String getParseExceptionInfo(SAXParseException spe) {
            String systemId = spe.getSystemId();
            if (systemId == null) {
                systemId = "null";
            }
            String info = "URI=" + systemId + " Line=" + spe.getLineNumber() + ": " + spe.getMessage();
            return info;
        }

        public void warning(SAXParseException spe) throws SAXException {
            out.println("Warning: " + getParseExceptionInfo(spe));
        }

        public void error(SAXParseException spe) throws SAXException {
            String message = "Error: " + getParseExceptionInfo(spe);
            throw new SAXException(message);
        }

        public void fatalError(SAXParseException spe) throws SAXException {
            String message = "Fatal Error: " + getParseExceptionInfo(spe);
            throw new SAXException(message);
        }
    }

    private static class ConfigHandler extends DefaultHandler {

        private Config m_config;

        private StringBuffer m_buf = new StringBuffer();

        private TreeMap m_searchmap;

        private Vector m_keywords;

        private String m_categorytext;

        private Boolean m_firstonly;

        private Boolean m_titlesonly;

        private String m_channeltext;

        private String m_searchname;

        private TreeMap m_providers;

        private String m_providerID;

        private Vector m_results;

        private Grabber m_grabber;

        int m_field = 0x00000000;

        private static final int ZIPCODE = 0x00000001;

        private static final int POSTALCODE = 0x00000002;

        private static final int PROVIDERID = 0x00000004;

        private static final int DEBUG = 0x00000006;

        private static final int EXPERIMENTAL = 0x00000007;

        private static final int PRUNEPROGRAMS = 0x00000008;

        private static final int CACHEWARN = 0x00000009;

        private static final int SEARCH = 0x00000010;

        private static final int FIRSTRUNBOLD = 0x00000011;

        private static final int FIRSTRUNDATE = 0x00000012;

        private static final int KEYWORDS = 0x00000020;

        private static final int KEY = 0x00000022;

        private static final int CATEGORYTEXT = 0x00000024;

        private static final int FIRSTONLY = 0x00000026;

        private static final int TITLESONLY = 0x00000028;

        private static final int CHANNELTEXT = 0x00000040;

        private static final int PROVIDERS = 0x00000080;

        private static final int PROVIDER = 0x00000100;

        private static final int RESULTS = 0x00000200;

        private static final int GRABBERS = 0x00000400;

        private static final int ZAPSCRAPPERGRABBER = 0x00000800;

        private static final int TVPI = 0x00001000;

        private static final int ZAPWSGRABBER = 0x00002000;

        private static final int USERID = 0x00004000;

        private static final int PASSWORD = 0x00008000;

        private static final int CATEGORIES = 0x00010000;

        private static final int CATEGORY = 0x00020000;

        public ConfigHandler(Config config) {
            m_config = config;
        }

        public void startDocument() throws SAXException {
            System.out.println("Parsing config file.");
        }

        public void startElement(String namespaceURI, String localName, String rawName, Attributes atts) throws SAXException {
            if (m_buf.length() > 0) m_buf.delete(0, m_buf.length());
            if (rawName.equals(Config.GRABBERS) || localName.equals(Config.GRABBERS)) {
                m_field |= GRABBERS;
                String value = atts.getValue(SELECTED);
                if (value != null) {
                    m_config.setSelectedGrabber(value);
                }
            } else if (rawName.equals(Config.LOCALGRABBER) || localName.equals(Config.LOCALGRABBER)) {
                m_grabber = new ZapScraperGrabber();
                m_field |= ZAPSCRAPPERGRABBER;
            } else if (rawName.equals(Config.GRABBER) || localName.equals(Config.GRABBER)) {
                String value = atts.getValue(Config.NAME);
                if (value.equals(ZAPSCRAPER)) {
                    m_grabber = new ZapScraperGrabber();
                    m_field |= ZAPSCRAPPERGRABBER;
                } else if (value.equals(ZAPWS)) {
                    m_grabber = new ZapWsGrabber();
                    m_field |= ZAPWSGRABBER;
                }
            }
            if (rawName.equals(Config.ZIPCODE) || localName.equals(Config.ZIPCODE)) {
                m_field |= ZIPCODE;
            } else if (rawName.equals(Config.POSTALCODE) || localName.equals(Config.POSTALCODE)) {
                m_field |= POSTALCODE;
            } else if (rawName.equals(Config.PROVID) || localName.equals(Config.PROVID)) {
                m_field |= PROVIDERID;
            } else if (rawName.equals(Config.DEBUG) || localName.equals(Config.DEBUG)) {
                m_field |= DEBUG;
            } else if (rawName.equals(Config.EXPERIMENTAL) || localName.equals(Config.EXPERIMENTAL)) {
                m_field |= EXPERIMENTAL;
            } else if (rawName.equals(Config.FIRSTRUNBOLD) || localName.equals(Config.FIRSTRUNBOLD)) {
                m_field |= FIRSTRUNBOLD;
            } else if (rawName.equals(Config.FIRSTRUNDATE) || localName.equals(Config.FIRSTRUNDATE)) {
                m_field |= FIRSTRUNDATE;
            } else if (rawName.equals(Config.PRUNEPROGRAMS) || localName.equals(Config.PRUNEPROGRAMS)) {
                m_field |= PRUNEPROGRAMS;
            } else if (rawName.equals(Config.CACHEWARN) || localName.equals(Config.CACHEWARN)) {
                m_field |= CACHEWARN;
            } else if (rawName.equals(Config.TVPI) || localName.equals(Config.TVPI)) {
                m_field |= TVPI;
            } else if (rawName.equals(Config.HTTP) || localName.equals(Config.HTTP)) {
                if ((m_field & ZAPSCRAPPERGRABBER) != 0) {
                    String value = atts.getValue(Config.DAYS);
                    if (value != null) ((ZapScraperGrabber) m_grabber).setDaysToDownload(Integer.valueOf(value).intValue());
                    value = atts.getValue(Config.TIMEOUT);
                    if (value != null) ((ZapScraperGrabber) m_grabber).setTimeout(Integer.valueOf(value).intValue());
                    value = atts.getValue(Config.RETRY);
                    if (value != null) ((ZapScraperGrabber) m_grabber).setRetrypause(Integer.valueOf(value).intValue());
                } else {
                    String value = atts.getValue(Config.DAYS);
                    if (value != null) m_config.setDaysToDownload(Integer.valueOf(value).intValue());
                    value = atts.getValue(Config.TIMEOUT);
                    if (value != null) m_config.setTimeout(Integer.valueOf(value).intValue());
                    value = atts.getValue(Config.RETRY);
                    if (value != null) m_config.setRetrypause(Integer.valueOf(value).intValue());
                }
            } else if (rawName.equals(Config.USERID) || localName.equals(Config.USERID)) {
                m_field |= USERID;
            } else if (rawName.equals(Config.PASSWORD) || localName.equals(Config.PASSWORD)) {
                m_field |= PASSWORD;
            } else if (rawName.equals(Config.WEBSERVICE) || localName.equals(Config.WEBSERVICE)) {
                if ((m_field & ZAPWSGRABBER) != 0) {
                    String value = atts.getValue(Config.DAYS);
                    if (value != null) {
                        ((ZapWsGrabber) m_grabber).setDaysToDownload(Integer.valueOf(value).intValue());
                    }
                }
            } else if (rawName.equals(Config.CHANNEL) || localName.equals(Config.CHANNEL)) {
                String desc = atts.getValue(Config.DESC);
                String in = atts.getValue(Config.IN);
                m_config.setChannel(desc, in);
            } else if (rawName.equals(Config.SCRLOC) || localName.equals(Config.SCRLOC)) {
                m_config.setAppSize(new Dimension(Integer.valueOf(atts.getValue(Config.WIDTH)).intValue(), Integer.valueOf(atts.getValue(Config.HEIGHT)).intValue()));
                m_config.setAppLocation(new Point(Integer.valueOf(atts.getValue(Config.XLOC)).intValue(), Integer.valueOf(atts.getValue(Config.YLOC)).intValue()));
            } else if (rawName.equals(Config.CATEGORY) || localName.equals(Config.CATEGORY)) {
                m_config.setCategoryColor(atts.getValue(Config.NAME), atts.getValue(Config.COLOR));
            } else if (rawName.equals(Config.SEARCH) || localName.equals(Config.SEARCH)) {
                m_field |= SEARCH;
                m_searchname = atts.getValue(Config.NAME);
                m_searchmap = new TreeMap();
                m_keywords = new Vector();
            } else if ((m_field & SEARCH) != 0 && (rawName.equals(Config.KEYWORDS) || localName.equals(Config.KEYWORDS))) {
                m_field |= KEYWORDS;
                m_searchmap.put(Config.TYPE, atts.getValue(Config.TYPE));
            } else if ((m_field & KEYWORDS) != 0 && (rawName.equals(Config.KEY) || localName.equals(Config.KEY))) {
                m_field |= KEY;
            } else if ((m_field & SEARCH) != 0 && (rawName.equals(Config.CATEGORYTEXT) || localName.equals(Config.CATEGORYTEXT))) {
                m_field |= CATEGORYTEXT;
            } else if ((m_field & SEARCH) != 0 && (rawName.equals(Config.FIRSTONLY) || localName.equals(Config.FIRSTONLY))) {
                m_field |= FIRSTONLY;
            } else if ((m_field & SEARCH) != 0 && (rawName.equals(Config.TITLESONLY) || localName.equals(Config.TITLESONLY))) {
                m_field |= TITLESONLY;
            } else if ((m_field & SEARCH) != 0 && (rawName.equals(Config.CHANNELTEXT) || localName.equals(Config.CHANNELTEXT))) {
                m_field |= CHANNELTEXT;
            } else if ((m_field & SEARCH) != 0 && (rawName.equals(Config.RESULTS) || localName.equals(Config.RESULTS))) {
                m_field |= RESULTS;
                m_results = new Vector();
            } else if ((m_field & RESULTS) != 0 && (rawName.equals(Config.PROGRAM) || localName.equals(Config.PROGRAM))) {
                String[] program = new String[2];
                program[0] = atts.getValue(Config.CHANNEL);
                program[1] = atts.getValue(Config.START);
                m_results.add(program);
            } else if (rawName.equals(Config.PROVIDERS) || localName.equals(Config.PROVIDERS)) {
                m_field |= PROVIDERS;
                m_providers = new TreeMap();
            } else if (rawName.equals(Config.PROVIDER) || localName.equals(Config.PROVIDER)) {
                m_field |= PROVIDER;
                m_providerID = atts.getValue(Config.PROVID);
            }
        }

        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (qName.equals(Config.GRABBERS) || localName.equals(Config.GRABBERS)) {
                m_field &= ~GRABBERS;
            } else if (qName.equals(Config.LOCALGRABBER) || localName.equals(Config.LOCALGRABBER)) {
                m_field &= ~ZAPSCRAPPERGRABBER;
                m_config.addGrabber(ZAPSCRAPER, m_grabber);
            } else if (qName.equals(GRABBER) || localName.equals(GRABBER)) {
                if ((m_field & ZAPSCRAPPERGRABBER) != 0) {
                    m_field &= ~ZAPSCRAPPERGRABBER;
                    m_config.addGrabber(ZAPSCRAPER, m_grabber);
                } else if ((m_field & ZAPWSGRABBER) != 0) {
                    m_field &= ~ZAPWSGRABBER;
                    m_config.addGrabber(ZAPWS, m_grabber);
                }
            } else if (qName.equals(Config.ZIPCODE) || localName.equals(Config.ZIPCODE)) {
                m_field &= ~ZIPCODE;
                if ((m_field & ZAPSCRAPPERGRABBER) != 0) {
                    if (m_buf.length() > 0) ((ZapScraperGrabber) m_grabber).setZipcode(m_buf.toString());
                } else {
                    if (m_buf.length() > 0) m_config.setZipcode(m_buf.toString());
                }
            } else if (qName.equals(Config.POSTALCODE) || localName.equals(Config.POSTALCODE)) {
                m_field &= ~POSTALCODE;
                if ((m_field & ZAPSCRAPPERGRABBER) != 0) {
                    if (m_buf.length() > 0) ((ZapScraperGrabber) m_grabber).setPostalcode(m_buf.toString());
                } else {
                    if (m_buf.length() > 0) m_config.setPostalcode(m_buf.toString());
                }
            } else if (qName.equals(Config.PROVID) || localName.equals(Config.PROVID)) {
                m_field &= ~PROVIDERID;
                if ((m_field & ZAPSCRAPPERGRABBER) != 0) {
                    if (m_buf.length() > 0) ((ZapScraperGrabber) m_grabber).setProvider(m_buf.toString());
                } else {
                    if (m_buf.length() > 0) m_config.setProvider(m_buf.toString());
                }
            } else if (qName.equals(Config.DEBUG) || localName.equals(Config.DEBUG)) {
                m_field &= ~DEBUG;
                if (m_buf.length() > 0) m_config.setDebug(m_buf.toString().compareToIgnoreCase("true") == 0);
            } else if (qName.equals(Config.EXPERIMENTAL) || localName.equals(Config.EXPERIMENTAL)) {
                m_field &= ~EXPERIMENTAL;
                if (m_buf.length() > 0) m_config.setExperimental(m_buf.toString().compareToIgnoreCase("true") == 0);
            } else if (qName.equals(Config.FIRSTRUNBOLD) || localName.equals(Config.FIRSTRUNBOLD)) {
                m_field &= ~FIRSTRUNBOLD;
                if (m_buf.length() > 0) m_config.setFirstRunBold(m_buf.toString().compareToIgnoreCase("true") == 0);
            } else if (qName.equals(Config.FIRSTRUNDATE) || localName.equals(Config.FIRSTRUNDATE)) {
                m_field &= ~FIRSTRUNDATE;
                if (m_buf.length() > 0) m_config.setFirstRunDate(m_buf.toString().compareToIgnoreCase("true") == 0);
            } else if (qName.equals(Config.PRUNEPROGRAMS) || localName.equals(Config.PRUNEPROGRAMS)) {
                m_field &= ~PRUNEPROGRAMS;
                if (m_buf.length() > 0) m_config.setPruneProgramsPrior(new Integer(m_buf.toString()).intValue());
            } else if (qName.equals(Config.CACHEWARN) || localName.equals(Config.CACHEWARN)) {
                m_field &= ~CACHEWARN;
                if (m_buf.length() > 0) m_config.setCachedDaysWarn(new Integer(m_buf.toString()).intValue());
            } else if (qName.equals(Config.SEARCH) || localName.equals(Config.SEARCH)) {
                m_field &= ~SEARCH;
                m_searchmap.put(Config.KEYWORDS, m_keywords);
                m_config.setSearch(m_searchname, m_searchmap);
            } else if (qName.equals(Config.KEYWORDS) || localName.equals(Config.KEYWORDS)) {
                m_field &= ~KEYWORDS;
            } else if (qName.equals(Config.KEY) || localName.equals(Config.KEY)) {
                m_field &= ~KEY;
                if (m_buf.length() > 0) m_keywords.add(m_buf.toString());
            } else if (qName.equals(Config.CATEGORYTEXT) || localName.equals(Config.CATEGORYTEXT)) {
                m_field &= ~CATEGORYTEXT;
                if (m_buf.length() > 0) {
                    m_categorytext = m_buf.toString();
                    m_searchmap.put(Config.CATEGORYTEXT, m_categorytext);
                    m_config.setSearch(m_searchname, m_searchmap);
                }
            } else if (qName.equals(Config.FIRSTONLY) || localName.equals(Config.FIRSTONLY)) {
                m_field &= ~FIRSTONLY;
                if (m_buf.length() > 0) {
                    m_firstonly = new Boolean(m_buf.toString());
                    m_searchmap.put(Config.FIRSTONLY, m_firstonly);
                    m_config.setSearch(m_searchname, m_searchmap);
                }
            } else if (qName.equals(Config.TITLESONLY) || localName.equals(Config.TITLESONLY)) {
                m_field &= ~TITLESONLY;
                if (m_buf.length() > 0) {
                    m_titlesonly = new Boolean(m_buf.toString());
                    m_searchmap.put(Config.TITLESONLY, m_titlesonly);
                    m_config.setSearch(m_searchname, m_searchmap);
                }
            } else if (qName.equals(Config.CHANNELTEXT) || localName.equals(Config.CHANNELTEXT)) {
                m_field &= ~CHANNELTEXT;
                if (m_buf.length() > 0) {
                    m_channeltext = m_buf.toString();
                    m_searchmap.put(Config.CHANNELTEXT, m_channeltext);
                    m_config.setSearch(m_searchname, m_searchmap);
                }
            } else if (qName.equals(Config.TVPI) || localName.equals(Config.TVPI)) {
                m_field &= ~TVPI;
            } else if (qName.equals(Config.GMT) || localName.equals(Config.GMT)) {
                if (m_buf.length() > 0) m_config.setTvpiGMT(m_buf.toString().compareToIgnoreCase("true") == 0);
            } else if (qName.equals(Config.RESULTS) || localName.equals(Config.RESULTS)) {
                m_field &= ~RESULTS;
                m_searchmap.put(Config.RESULTS, m_results);
                m_results = null;
            } else if (qName.equals(Config.PROVIDERS) || localName.equals(Config.PROVIDERS)) {
                m_field &= ~PROVIDERS;
                if (m_providers.size() > 0) {
                    m_config.setProviders(m_providers);
                }
            } else if (qName.equals(Config.PROVIDER) || localName.equals(Config.PROVIDER)) {
                m_field &= ~PROVIDER;
                if (m_buf.length() > 0) if (m_providers != null) {
                    m_providers.put(m_buf.toString(), m_providerID);
                }
            } else if (qName.equals(Config.USERID) || localName.equals(Config.USERID)) {
                m_field &= ~USERID;
                if ((m_field & ZAPWSGRABBER) != 0) {
                    if (m_buf.length() > 0) {
                        ((ZapWsGrabber) m_grabber).setUserID(m_buf.toString());
                    }
                }
            } else if (qName.equals(Config.PASSWORD) || localName.equals(Config.PASSWORD)) {
                m_field &= ~PASSWORD;
                if ((m_field & ZAPWSGRABBER) != 0) {
                    if (m_buf.length() > 0) {
                        ((ZapWsGrabber) m_grabber).setPassword(m_buf.toString());
                    }
                }
            }
        }

        public void characters(char[] values, int start, int length) throws org.xml.sax.SAXException {
            m_buf.append(values, start, length);
        }
    }

    public void writeConfig() throws IOException {
        writeConfig(m_filename);
    }

    /**
	 * @param value
	 */
    public void setSelectedGrabber(String value) {
        m_selectedGrabber = value;
        System.out.println("Grabber set to:" + m_selectedGrabber);
    }

    public void writeConfig(String file) throws IOException {
        MyXMLWriter writer = new MyXMLWriter(new FileWriter(file));
        writer.setIndent(2);
        writer.startDocument();
        writer.xmlDecl("ISO-8859-1", "1.0");
        writer.startTag(CONFIG);
        writer.dataElement(DEBUG, "" + m_debug);
        writer.dataElement(EXPERIMENTAL, "" + m_experimental);
        writer.dataElement(FIRSTRUNBOLD, "" + m_firstrunbold);
        writer.dataElement(FIRSTRUNDATE, "" + m_firstrundate);
        writer.dataElement(PRUNEPROGRAMS, "" + omitProgramsPrior);
        writer.dataElement(CACHEWARN, "" + m_cachedDaysWarn);
        writer.startTag(TVPI);
        writer.dataElement(GMT, "" + m_tvpi_gmt);
        writer.endTag(TVPI);
        Map grbAtts = new HashMap();
        grbAtts.put(SELECTED, m_selectedGrabber);
        writer.startTag(GRABBERS, grbAtts);
        if (m_grabbers != null) {
            Collection grabbers = m_grabbers.values();
            for (Iterator iter = grabbers.iterator(); iter.hasNext(); ) {
                Grabber grabber = (Grabber) iter.next();
                grabber.writeXML(writer);
            }
        }
        writer.endTag(GRABBERS);
        if (m_appSize != null && m_appLocation != null) {
            TreeMap scrAtts = new TreeMap();
            scrAtts.put(XLOC, new Integer(m_appLocation.x));
            scrAtts.put(YLOC, new Integer(m_appLocation.y));
            scrAtts.put(WIDTH, new Integer(m_appSize.width));
            scrAtts.put(HEIGHT, new Integer(m_appSize.height));
            writer.dataElement(SCRLOC, "", scrAtts);
        }
        if (m_catColors != null) {
            writer.startTag(CATEGORYCOLORS);
            m_catColors.keySet().iterator();
            for (Iterator iter = m_catColors.keySet().iterator(); iter.hasNext(); ) {
                String category = (String) iter.next();
                TreeMap catAtts = new TreeMap();
                catAtts.put(NAME, category);
                catAtts.put(COLOR, ((ColorValue) m_catColors.get(category)).strColor);
                writer.dataElement(CATEGORY, "", catAtts);
            }
            writer.endTag(CATEGORYCOLORS);
        }
        if (m_channels != null) {
            writer.startTag(CHANNELS);
            Set keys = m_channels.keySet();
            Iterator it = keys.iterator();
            while (it.hasNext()) {
                TreeMap atts = new TreeMap();
                String desc = (String) it.next();
                atts.put(DESC, desc);
                atts.put(IN, (String) m_channels.get(desc));
                writer.dataElement(CHANNEL, "", atts);
            }
            writer.endTag(CHANNELS);
        }
        if (m_searches != null) {
            writer.startTag(SEARCHES);
            Vector searchnames = new Vector();
            getSearchNames(searchnames);
            int namesCount = searchnames.size();
            for (int i = 0; i < namesCount; i++) {
                TreeMap atts = new TreeMap();
                atts.put(NAME, (String) searchnames.get(i));
                writer.startTag(SEARCH, atts);
                Map search = getSearch((String) searchnames.get(i));
                atts = new TreeMap();
                String type = (String) search.get(TYPE);
                if (type == null) {
                    atts.put(TYPE, "any");
                } else {
                    atts.put(TYPE, (String) search.get(TYPE));
                }
                Vector keywords = (Vector) search.get(KEYWORDS);
                String key;
                if (keywords != null) {
                    writer.startTag(KEYWORDS, atts);
                    int keyCount = keywords.size();
                    for (int j = 0; j < keyCount; j++) {
                        key = (String) keywords.get(j);
                        key = key.trim();
                        if (key.length() > 0) writer.dataElement(KEY, (String) keywords.get(j));
                    }
                    writer.endTag(KEYWORDS);
                }
                String m_categorytext = (String) search.get(CATEGORYTEXT);
                if (m_categorytext != null && m_categorytext.length() > 0 && !(m_categorytext.equals(ProgramData.ALL_CATS))) {
                    writer.dataElement(CATEGORYTEXT, "" + m_categorytext);
                }
                Boolean m_firstonly = (Boolean) search.get(FIRSTONLY);
                if (m_firstonly != null && m_firstonly.booleanValue()) {
                    writer.dataElement(FIRSTONLY, "" + m_firstonly.toString());
                }
                Boolean m_titlesonly = (Boolean) search.get(TITLESONLY);
                if (m_titlesonly != null && m_titlesonly.booleanValue()) {
                    writer.dataElement(TITLESONLY, "" + m_titlesonly.toString());
                }
                String m_channeltext = (String) search.get(CHANNELTEXT);
                if (m_channeltext != null && m_channeltext.length() > 0) {
                    writer.dataElement(CHANNELTEXT, "" + m_channeltext);
                }
                Vector results = (Vector) search.get(RESULTS);
                if (results != null) {
                    writer.startTag(RESULTS);
                    int resCount = results.size();
                    for (int j = 0; j < resCount; j++) {
                        atts.clear();
                        atts.put(CHANNEL, ((String[]) results.get(j))[0]);
                        atts.put(START, ((String[]) results.get(j))[1]);
                        writer.dataElement(PROGRAM, "", atts);
                    }
                    writer.endTag(RESULTS);
                }
                writer.endTag(SEARCH);
            }
            writer.endTag(SEARCHES);
        }
        if (m_providers != null) {
            writer.startTag(PROVIDERS);
            Set keys = m_providers.keySet();
            Iterator it = keys.iterator();
            while (it.hasNext()) {
                TreeMap atts = new TreeMap();
                String provDesc = (String) it.next();
                atts.put(PROVID, (String) m_providers.get(provDesc));
                writer.dataElement(PROVIDER, provDesc, atts);
            }
            writer.endTag(PROVIDERS);
        }
        writer.endTag(CONFIG);
        writer.endDocument();
    }

    public void clear() {
        m_providers = null;
        m_channels = new TreeMap();
        m_searches = new TreeMap();
        m_grabbers = new HashMap();
        m_debug = false;
        m_experimental = DEFAULT_EXPERIMENTAL;
        m_firstrunbold = DEFAULT_FIRSTRUNBOLD;
        m_firstrundate = DEFAULT_FIRSTRUNDATE;
        m_cachedDaysWarn = DEFAULT_CACHED_DAYS_WARNING;
        omitProgramsPrior = DEFAULT_OMIT_PROGRAMS_PRIOR;
    }

    /**
     * @return number of actual grabbers
     */
    private int setupInternalGrabbers() {
        Map grabbers = getGrabbersMap();
        Grabber grabber = (Grabber) grabbers.get(ZAPWS);
        if (grabber == null) {
            grabbers.put(ZAPWS, new ZapWsGrabber());
        }
        return grabbers.size();
    }

    public Map getGrabbersMap() {
        if (m_grabbers == null) m_grabbers = new HashMap();
        return m_grabbers;
    }

    public void addGrabber(String grabberName, Grabber gbr) {
        Map grabbers = getGrabbersMap();
        if (gbr != null) grabbers.put(grabberName, gbr);
    }

    public int getNumGrabbers() {
        if (m_grabbers != null) return m_grabbers.size();
        return 0;
    }

    public Grabber getGrabber(String grabberName) {
        if (m_grabbers != null) return (Grabber) m_grabbers.get(grabberName);
        return null;
    }

    /**
     * @deprecated use <CODE>addGrabber(int pos, Grabber grabber)</CODE>
     */
    public void setZipcode(String zipcode) {
        int numGrabbers = setupInternalGrabbers();
        if (numGrabbers > 0) {
            Grabber grabber = (Grabber) m_grabbers.get(ZAPSCRAPER);
            if (grabber instanceof ZapScraperGrabber) {
                ((ZapScraperGrabber) grabber).setZipcode(zipcode);
            }
        }
    }

    /**
     * @deprecated use <CODE>getGrabber(int pos)</CODE>
     */
    public String getZipcode() {
        String ret = null;
        int numGrabbers = setupInternalGrabbers();
        if (numGrabbers > 0) {
            Grabber grabber = (Grabber) m_grabbers.get(ZAPSCRAPER);
            if (grabber instanceof ZapScraperGrabber) {
                ret = ((ZapScraperGrabber) grabber).getZipcode();
            }
        }
        return ret;
    }

    /**
     * @deprecated use <CODE>addGrabber(int pos, Grabber grabber)</CODE>
     */
    public void setPostalcode(String postalcode) {
        int numGrabbers = setupInternalGrabbers();
        if (numGrabbers > 0) {
            Grabber grabber = (Grabber) m_grabbers.get(ZAPSCRAPER);
            if (grabber instanceof ZapScraperGrabber) {
                ((ZapScraperGrabber) grabber).setPostalcode(postalcode);
            }
        }
    }

    /**
     * @deprecated use <CODE>getGrabber(int pos)</CODE>
     */
    public String getPostalcode() {
        String ret = null;
        int numGrabbers = setupInternalGrabbers();
        if (numGrabbers > 0) {
            Grabber grabber = (Grabber) m_grabbers.get(ZAPSCRAPER);
            if (grabber instanceof ZapScraperGrabber) {
                ret = ((ZapScraperGrabber) grabber).getPostalcode();
            }
        }
        return ret;
    }

    /**
     * @deprecated use <CODE>addGrabber(int pos, Grabber grabber)</CODE>
     */
    public void setProvider(String provider) {
        int numGrabbers = setupInternalGrabbers();
        if (numGrabbers > 0) {
            Grabber grabber = (Grabber) m_grabbers.get(ZAPSCRAPER);
            if (grabber instanceof ZapScraperGrabber) {
                ((ZapScraperGrabber) grabber).setProvider(provider);
            }
        }
    }

    /**
     * @deprecated use <CODE>getGrabber(int pos)</CODE>
     */
    public String getProvider() {
        String ret = null;
        int numGrabbers = setupInternalGrabbers();
        if (numGrabbers > 0) {
            Grabber grabber = (Grabber) m_grabbers.get(ZAPSCRAPER);
            if (grabber instanceof ZapScraperGrabber) {
                ret = ((ZapScraperGrabber) grabber).getProvider();
            }
        }
        return ret;
    }

    /** Getter for property m_daysToDownload.
     * @return Value of property m_daysToDownload.
     * @deprecated use <CODE>getGrabber(int grabber)</CODE>
     */
    public int getDaysToDownload() {
        int ret = 0;
        int numGrabbers = setupInternalGrabbers();
        if (numGrabbers > 0) {
            Grabber grabber = (Grabber) m_grabbers.get(ZAPSCRAPER);
            if (grabber instanceof ZapScraperGrabber) {
                ret = ((ZapScraperGrabber) grabber).getDaysToDownload();
            }
        }
        return ret;
    }

    /** Setter for property m_daysToDownload.
     * @param daysToDownload New value of property m_daysToDownload.
     * @deprecated use <CODE>addGrabber(int grabber)</CODE>
     */
    public void setDaysToDownload(int daysToDownload) {
        int numGrabbers = setupInternalGrabbers();
        if (numGrabbers > 0) {
            Grabber grabber = (Grabber) m_grabbers.get(ZAPSCRAPER);
            if (grabber instanceof ZapScraperGrabber) {
                ((ZapScraperGrabber) grabber).setDaysToDownload(daysToDownload);
            }
        }
    }

    /** Getter for property timeout.
     * @return Value of property timeout.
     * @deprecated use <CODE>getGrabber(int grabber)</CODE>
     */
    public int getTimeout() {
        int ret = 0;
        int numGrabbers = setupInternalGrabbers();
        if (numGrabbers > 0) {
            Grabber grabber = (Grabber) m_grabbers.get(ZAPSCRAPER);
            if (grabber instanceof ZapScraperGrabber) {
                ret = ((ZapScraperGrabber) grabber).getTimeout();
            }
        }
        return ret;
    }

    /** Setter for property timeout.
     * @param timeout New value of property timeout.
     * @deprecated use <CODE>getGrabber(int grabber)</CODE>
     */
    public void setTimeout(int timeout) {
        int numGrabbers = setupInternalGrabbers();
        if (numGrabbers > 0) {
            Grabber grabber = (Grabber) m_grabbers.get(ZAPSCRAPER);
            if (grabber instanceof ZapScraperGrabber) {
                ((ZapScraperGrabber) grabber).setTimeout(timeout);
            }
        }
    }

    /** Getter for property retrypause.
     * @return Value of property retrypause.
     * @deprecated use <CODE>getGrabber(int grabber)</CODE>
     */
    public int getRetrypause() {
        int ret = 0;
        int numGrabbers = setupInternalGrabbers();
        if (numGrabbers > 0) {
            Grabber grabber = (Grabber) m_grabbers.get(ZAPSCRAPER);
            if (grabber instanceof ZapScraperGrabber) {
                ret = ((ZapScraperGrabber) grabber).getRetrypause();
            }
        }
        return ret;
    }

    /** Setter for property retrypause.
     * @param retrypause New value of property retrypause.
     * @deprecated use <CODE>getGrabber(int grabber)</CODE>
     */
    public void setRetrypause(int retrypause) {
        int numGrabbers = setupInternalGrabbers();
        if (numGrabbers > 0) {
            Grabber grabber = (Grabber) m_grabbers.get(ZAPSCRAPER);
            if (grabber instanceof ZapScraperGrabber) {
                ((ZapScraperGrabber) grabber).setRetrypause(retrypause);
            }
        }
    }

    public void setDebug(boolean debug) {
        m_debug = debug;
    }

    public boolean getDebug() {
        return m_debug;
    }

    public void setExperimental(boolean b) {
        m_experimental = b;
    }

    public boolean getDefaultExperimental() {
        return DEFAULT_EXPERIMENTAL;
    }

    public boolean getExperimental() {
        return m_experimental;
    }

    public void setFirstRunBold(boolean b) {
        m_firstrunbold = b;
    }

    public boolean getDefaultFirstRunBold() {
        return DEFAULT_FIRSTRUNBOLD;
    }

    public boolean getFirstRunBold() {
        return m_firstrunbold;
    }

    public void setFirstRunDate(boolean b) {
        m_firstrundate = b;
    }

    public boolean getDefaultFirstRunDate() {
        return DEFAULT_FIRSTRUNDATE;
    }

    public boolean getFirstRunDate() {
        return m_firstrundate;
    }

    public void setCachedDaysWarn(int i) {
        m_cachedDaysWarn = i;
    }

    public int getCachedDaysWarn() {
        return m_cachedDaysWarn;
    }

    public int getDefaultDaysWarn() {
        return DEFAULT_CACHED_DAYS_WARNING;
    }

    public void setPruneProgramsPrior(int i) {
        omitProgramsPrior = i;
    }

    public int getPruneProgramsPrior() {
        return omitProgramsPrior;
    }

    public int getDefaultPrunePrograms() {
        return DEFAULT_OMIT_PROGRAMS_PRIOR;
    }

    public void setChannel(String desc, String in) {
        m_channels.put(desc, in);
    }

    public String getChannelMark(String desc) {
        return (String) m_channels.get(desc);
    }

    public boolean isChannelMarked(String desc) {
        String in = (String) m_channels.get(desc);
        return in == null || ((String) m_channels.get(desc)).equals(YES);
    }

    public void markChannel(String desc, boolean mark) {
        setChannel(desc, (mark ? YES : NO));
    }

    public void setSearch(String name, Map search) {
        m_searches.put(name, search);
    }

    public Set getChannels() {
        TreeSet channels = new TreeSet(new ChannelDescComparitor());
        channels.addAll(m_channels.keySet());
        return channels;
    }

    public void getSearchNames(Collection names) {
        names.clear();
        names.addAll(m_searches.keySet());
    }

    public Map getSearch(String searchName) {
        return (Map) m_searches.get(searchName);
    }

    public boolean searchExists(String searchName) {
        return m_searches.containsKey(searchName);
    }

    public void storeSearchResults(String searchName, ProgramList results) {
        Map search = (Map) m_searches.get(searchName);
        int count = results.getLength();
        Vector storedResults = new Vector(count);
        Programs programs = Programs.getInstance();
        for (int i = 0; i < count; i++) {
            ProgItem result = (ProgItem) results.item(i);
            String[] program = new String[2];
            program[0] = programs.getChannel(result);
            program[1] = programs.getStartTime(result);
            storedResults.add(program);
        }
        search.put(RESULTS, storedResults);
    }

    public ProgramList recallStoredSearchResults(String searchName, ProgramList inputNodes) {
        Map search = (Map) m_searches.get(searchName);
        Programs programs = Programs.getInstance();
        ProgramList fullResults = programs.getEmptyProgramList();
        Vector storedResults = (Vector) search.get(RESULTS);
        Date now = new Date();
        if (storedResults != null) {
            TreeSet sortedResults = new TreeSet(new ResultComparitor());
            sortedResults.addAll(storedResults);
            int count = inputNodes.getLength();
            for (int i = 0; i < count; i++) {
                ProgItem prog = (ProgItem) inputNodes.item(i);
                String[] program = new String[2];
                program[0] = programs.getChannel(prog);
                program[1] = programs.getStartTime(prog);
                if (getSearchFromNow()) {
                    Calendar startTime = Utilities.makeCal(program[1]);
                    if (startTime.getTime().compareTo(now) < 0) continue;
                }
                if (sortedResults.contains(program)) {
                    fullResults.add(prog);
                }
            }
        }
        return fullResults;
    }

    public String getSearchExpression(String searchName) {
        Map search = getSearch(searchName);
        Vector keywords = (Vector) search.get(KEYWORDS);
        int keysSize = keywords.size();
        StringBuffer regexp = new StringBuffer();
        regexp.append("m/\\b(");
        for (int i = 0; i < keysSize; i++) {
            if (i != 0) regexp.append('|');
            regexp.append((String) keywords.get(i));
        }
        regexp.append(")/i");
        return regexp.toString();
    }

    public String getSearchText(String searchName) {
        Map search = getSearch(searchName);
        Vector keywords = (Vector) search.get(KEYWORDS);
        StringBuffer regexp = new StringBuffer();
        try {
            int keysSize = keywords.size();
            for (int i = 0; i < keysSize; i++) {
                if (i != 0) regexp.append('|');
                regexp.append((String) keywords.get(i));
            }
        } catch (Exception e) {
            if (getDebug()) System.out.println("Note: No keywords in search");
        }
        return regexp.toString();
    }

    public String getSearchCategory(String searchName) {
        String retVal = ProgramData.ALL_CATS;
        Map search = getSearch(searchName);
        String temp = (String) search.get(CATEGORYTEXT);
        if (temp != null && temp.length() > 0) {
            retVal = temp;
        }
        return retVal;
    }

    public String getSearchChannel(String searchName) {
        String retVal = "";
        Map search = getSearch(searchName);
        String temp = (String) search.get(CHANNELTEXT);
        if (temp != null && temp.length() > 0) {
            retVal = temp;
        }
        return retVal;
    }

    public boolean getSearchFirstOnly(String searchName) {
        Map search = getSearch(searchName);
        boolean retVal = false;
        Boolean temp = (Boolean) search.get(FIRSTONLY);
        if (temp != null && temp.booleanValue()) {
            retVal = temp.booleanValue();
        }
        return retVal;
    }

    public boolean getSearchTitlesOnly(String searchName) {
        Map search = getSearch(searchName);
        boolean retVal = false;
        Boolean temp = (Boolean) search.get(TITLESONLY);
        if (temp != null && temp.booleanValue()) {
            retVal = temp.booleanValue();
        }
        return retVal;
    }

    public void removeSearch(String searchName) {
        m_searches.remove(searchName);
    }

    public java.lang.String toString() {
        java.lang.String retValue;
        retValue = "[";
        if (m_grabbers != null) {
            Collection grabbers = m_grabbers.values();
            for (Iterator iter = grabbers.iterator(); iter.hasNext(); ) {
                Grabber grabber = (Grabber) iter.next();
                if (grabber instanceof ZapScraperGrabber) {
                    ZapScraperGrabber lGrabber = (ZapScraperGrabber) grabber;
                    retValue += "Local grabber: zipcode=" + lGrabber.getZipcode() + ", postalcode=" + lGrabber.getPostalcode() + ", provider=" + lGrabber.getProvider() + ",\n";
                }
            }
        }
        retValue += "\tdebug=" + m_debug + ",\n";
        retValue += "\texperimental=" + m_experimental + ",\n";
        retValue += "\tfirstrunbold=" + m_firstrunbold + ",\n";
        retValue += "\tfirstrundate=" + m_firstrundate + ",\n";
        retValue += "\tchannels=" + m_channels.toString() + ",\n";
        retValue += "\tsearches=" + m_searches.toString() + ",\n";
        retValue += "\tproviders=" + m_providers.toString() + "]";
        return retValue;
    }

    public boolean getSearchFromNow() {
        return false;
    }

    public Map getProviders() {
        return m_providers;
    }

    public void setProviders(Map providers) {
        m_providers = providers;
    }

    public void setAppSize(Dimension size) {
        m_appSize = size;
    }

    public Dimension getAppSize() {
        return m_appSize;
    }

    public void setAppLocation(Point pt) {
        m_appLocation = pt;
    }

    public Point getAppLocation() {
        return m_appLocation;
    }

    public boolean getDefaultTvpiGMT() {
        return DEFAULT_TVPI_GMT;
    }

    public boolean getTvpiGMT() {
        return m_tvpi_gmt;
    }

    public void setTvpiGMT(boolean gmt) {
        m_tvpi_gmt = gmt;
    }

    /**
	 * @return
	 */
    public Grabber getSelectedGrabber() {
        return getGrabber(m_selectedGrabber);
    }

    public String getSelectedGrabberName() {
        return m_selectedGrabber;
    }

    public ColorValue getCategoryColor(String category) {
        return (ColorValue) m_catColors.get(category.toUpperCase());
    }

    public HashMap getCategoryColors() {
        return m_catColors;
    }

    public void setCategoryColors(HashMap hm) {
        hm = m_catColors;
    }

    public void setCategoryColor(String category, Color color) {
        m_catColors.put(category.toUpperCase(), new ColorValue(color));
    }

    public void setCategoryColor(String category, String color) {
        m_catColors.put(category.toUpperCase(), new ColorValue(color));
    }
}
