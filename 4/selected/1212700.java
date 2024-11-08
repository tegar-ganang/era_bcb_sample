package org.ttalbott.mytelly;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author  Tom Talbott
 * @version
 */
public class Favorites extends java.lang.Object {

    private static Favorites m_instance = null;

    TreeSet m_favorites;

    String m_filename;

    Vector m_results;

    public static final String TITLE = "title";

    public static final String FAVORITES = "favorites";

    public static final String RESULTS = "results";

    public static final String CHANNEL = "channel";

    public static final String START = "start";

    public static final String PROGRAM = "programme";

    public static final String DEFAULTFILE = "favorites.xml";

    /** Creates new Favorites */
    private Favorites() {
        m_favorites = new TreeSet(new FavoritesComparitor());
        m_results = null;
    }

    private void setFilename(String filename) {
        m_filename = filename;
    }

    public static Favorites getInstance() {
        if (m_instance == null) m_instance = new Favorites();
        return m_instance;
    }

    public static void release() {
        m_instance = null;
    }

    public static Favorites newFavorites() {
        return newFavorites(DEFAULTFILE);
    }

    public static Favorites newFavorites(java.lang.String filename) {
        Favorites favs = Favorites.getInstance();
        favs.setFilename(filename);
        return favs;
    }

    public static Favorites readFavorites() throws IOException {
        return readFavorites(DEFAULTFILE);
    }

    public static Favorites readFavorites(String file) throws IOException {
        boolean validation = false;
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setValidating(validation);
        XMLReader xmlReader = null;
        try {
            SAXParser saxParser = spf.newSAXParser();
            xmlReader = saxParser.getXMLReader();
        } catch (Exception ex) {
            System.err.println(ex);
            return null;
        }
        xmlReader.setErrorHandler(new MyErrorHandler(System.err));
        Favorites fav = Favorites.getInstance();
        fav.setFilename(file);
        xmlReader.setContentHandler(new FavoritesHandler(fav));
        try {
            xmlReader.parse(convertToFileURL(file));
        } catch (SAXException se) {
            System.err.println(se.getMessage());
        } catch (IOException ioe) {
            System.err.println(ioe);
        }
        return fav;
    }

    /**
     * Convert from a filename to a file URL.
     */
    private static String convertToFileURL(String filename) {
        String path = null;
        try {
            path = new File(filename).toURL().toString();
        } catch (MalformedURLException mue) {
            System.err.println(mue.getMessage());
        }
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

    private static class FavoritesHandler extends DefaultHandler {

        Favorites m_favs;

        Vector m_results = null;

        StringBuffer m_buf = new StringBuffer();

        boolean m_inTitle = false;

        public FavoritesHandler(Favorites fav) {
            m_favs = fav;
        }

        public void startDocument() throws SAXException {
            System.out.println("Parsing favorites file.");
        }

        public void startElement(String namespaceURI, String localName, String rawName, Attributes atts) throws SAXException {
            if (m_buf.length() > 0) m_buf.delete(0, m_buf.length());
            if (rawName.equals(Favorites.TITLE) || localName.equals(Favorites.TITLE)) {
                m_inTitle = true;
            }
            if (rawName.equals(Favorites.RESULTS) || localName.equals(Favorites.RESULTS)) {
                m_results = new Vector();
            }
            if (rawName.equals(Favorites.PROGRAM) || localName.equals(Favorites.PROGRAM)) {
                String[] program = new String[2];
                program[0] = atts.getValue(Favorites.CHANNEL);
                program[1] = atts.getValue(Favorites.START);
                m_results.add(program);
            }
        }

        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (qName.equals(Favorites.TITLE) || localName.equals(Favorites.TITLE)) {
                m_inTitle = false;
                if (m_buf.length() > 0) m_favs.add(m_buf.toString());
            }
            if (qName.equals(Favorites.RESULTS) || localName.equals(Favorites.RESULTS)) {
                if (m_results != null) {
                    m_favs.setResults(m_results);
                }
            }
        }

        public void characters(char[] values, int start, int length) throws SAXException {
            m_buf.append(values, start, length);
        }
    }

    private void setResults(Vector results) {
        m_results = results;
    }

    public void add(String fav) {
        m_favorites.add(fav.toUpperCase());
    }

    public void remove(String title) {
        m_favorites.remove(title.toUpperCase());
    }

    public void toggleFavorite(String title) {
        if (m_favorites.contains(title)) {
            remove(title);
        } else {
            add(title);
        }
    }

    public void setFavorites(Vector favs) {
        m_favorites.clear();
        m_favorites.addAll(favs);
    }

    public Vector getItems() {
        return new Vector(m_favorites);
    }

    public String getSearchExpression() {
        Iterator it = m_favorites.iterator();
        StringBuffer regexp = new StringBuffer();
        StringBuffer searchItems = new StringBuffer();
        regexp.append("m/^(");
        int i = 0;
        while (it.hasNext()) {
            if (i++ != 0) searchItems.append('|');
            searchItems.append((String) it.next());
        }
        if (searchItems.length() == 0) {
            return null;
        }
        regexp.append(searchItems.toString());
        regexp.append(")\\b/i");
        return regexp.toString();
    }

    public boolean isFavorite(String title) {
        return m_favorites.contains(title.toUpperCase());
    }

    public void writeFavorites() throws IOException {
        writeFavorites(m_filename);
    }

    public void writeFavorites(String file) throws IOException {
        System.out.println("Writing favorites file: " + file);
        MyXMLWriter writer = new MyXMLWriter(new FileWriter(file));
        TreeMap atts = new TreeMap();
        writer.setIndent(2);
        writer.startDocument();
        writer.xmlDecl("ISO-8859-1", "1.0");
        writer.startTag(FAVORITES);
        Iterator it = m_favorites.iterator();
        while (it.hasNext()) {
            writer.dataElement(TITLE, (String) it.next());
        }
        if (m_results != null) {
            writer.startTag(RESULTS);
            int resCount = m_results.size();
            for (int j = 0; j < resCount; j++) {
                atts.clear();
                atts.put(CHANNEL, ((String[]) m_results.get(j))[0]);
                atts.put(START, ((String[]) m_results.get(j))[1]);
                writer.dataElement(PROGRAM, "", atts);
            }
            writer.endTag(RESULTS);
        } else {
            System.out.println("No results in favorites");
        }
        writer.endTag(FAVORITES);
        writer.endDocument();
    }

    public void storeFavoritesResults(ProgramList results) {
        int count = (results != null ? results.getLength() : 0);
        m_results = new Vector(count);
        Programs programs = Programs.getInstance();
        for (int i = 0; i < count; i++) {
            ProgItem result = (ProgItem) results.item(i);
            String[] program = new String[2];
            program[0] = Programs.getInstance().getChannel(result);
            program[1] = Programs.getInstance().getStartTime(result);
            m_results.add(program);
        }
    }

    public ProgramList recallStoredFavoritesResults(ProgramList inputNodes) {
        Programs programs = Programs.getInstance();
        ProgramList fullResults = programs.getEmptyProgramList();
        if (m_results != null && inputNodes != null && fullResults != null) {
            TreeSet sortedResults = new TreeSet(new ResultComparitor());
            sortedResults.addAll(m_results);
            int count = inputNodes.getLength();
            for (int i = 0; i < count; i++) {
                ProgItem prog = (ProgItem) inputNodes.item(i);
                String[] program = new String[2];
                program[0] = Programs.getInstance().getChannel(prog);
                program[1] = Programs.getInstance().getStartTime(prog);
                if (sortedResults.contains(program)) {
                    fullResults.add(prog);
                }
            }
        }
        return fullResults;
    }
}
