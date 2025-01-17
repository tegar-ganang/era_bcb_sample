package org.solrmarc.marc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.solrmarc.marc.MarcImporter.MyShutdownThread;
import org.solrmarc.tools.SolrMarcIndexerException;
import org.solrmarc.tools.Utils;

/**
 * 
 * 
 * @author Robert Haschart 
 * @version $Id: BooklistReader.java 1378 2010-10-26 15:56:08Z rh9ec@virginia.edu $
 *
 */
public class BooklistReader extends SolrReIndexer {

    Map<String, Map<String, Object>> documentCache = null;

    static Logger logger = Logger.getLogger(BooklistReader.class.getName());

    String booklistFilename = null;

    /**
     * Constructor
     * @param properties Path to properties files
     * @throws IOException
     */
    public BooklistReader() {
    }

    static String[] addArg(String args[], String toAdd) {
        String result[] = new String[args.length + 1];
        System.arraycopy(args, 0, result, 1, args.length);
        result[0] = toAdd;
        return (result);
    }

    @Override
    protected void loadLocalProperties() {
        super.loadLocalProperties();
        if (solrFieldContainingEncodedMarcRecord == null) {
            solrFieldContainingEncodedMarcRecord = "marc_display";
        }
    }

    @Override
    protected void processAdditionalArgs() {
        super.processAdditionalArgs();
        booklistFilename = addnlArgs.length > 0 ? addnlArgs[0] : "booklists.txt";
        documentCache = new LinkedHashMap<String, Map<String, Object>>();
    }

    public int handleAll() {
        Runtime.getRuntime().addShutdownHook(new MyShutdownThread(this));
        Date start = new Date();
        try {
            readBooklist(booklistFilename);
        } catch (Exception e) {
            System.err.println("Exception: " + e.getMessage());
            e.printStackTrace();
        }
        finish();
        signalServer();
        return (0);
    }

    /**
     * Read a book list
     * @param filename Path to the book list file
     */
    public void readBooklist(String filename) {
        Reader input = null;
        try {
            if (filename.startsWith("http:")) {
                URL url = new URL(filename);
                URLConnection conn = url.openConnection();
                input = new InputStreamReader(conn.getInputStream());
            } else {
                String fileNameAll = filename;
                try {
                    fileNameAll = new File(filename).getCanonicalPath();
                } catch (IOException e) {
                    fileNameAll = new File(filename).getAbsolutePath();
                }
                input = new FileReader(new File(fileNameAll));
            }
            BufferedReader reader = new BufferedReader(input);
            String line;
            Date today = new Date();
            while ((line = reader.readLine()) != null) {
                if (shuttingDown) break;
                String fields[] = line.split("\\|");
                Map<String, String> valuesToAdd = new LinkedHashMap<String, String>();
                valuesToAdd.put("fund_code_facet", fields[11]);
                valuesToAdd.put("date_received_facet", fields[0]);
                DateFormat format = new SimpleDateFormat("yyyyMMdd");
                Date dateReceived = format.parse(fields[0], new ParsePosition(0));
                if (dateReceived.after(today)) continue;
                String docID = "u" + fields[9];
                try {
                    Map<String, Object> docMap = getDocumentMap(docID);
                    if (docMap != null) {
                        addNewDataToRecord(docMap, valuesToAdd);
                        documentCache.put(docID, docMap);
                        if (doUpdate && docMap != null && docMap.size() != 0) {
                            update(docMap);
                        }
                    }
                } catch (SolrMarcIndexerException e) {
                    if (e.getLevel() == SolrMarcIndexerException.IGNORE) {
                        logger.error("Indexing routine says record " + docID + " should be ignored");
                    } else if (e.getLevel() == SolrMarcIndexerException.DELETE) {
                        logger.error("Indexing routine says record " + docID + " should be deleted");
                    }
                    if (e.getLevel() == SolrMarcIndexerException.EXIT) {
                        logger.error("Indexing routine says processing should be terminated by record " + docID);
                        break;
                    }
                }
            }
        } catch (FileNotFoundException e) {
            logger.info(e.getMessage());
            logger.error(e.getCause());
        } catch (IOException e) {
            logger.info(e.getMessage());
            logger.error(e.getCause());
        }
    }

    /**
     * Get the documentMap for the the given marc id
     * @param docID 
     * @return The map of index fields
     */
    private Map<String, Object> getDocumentMap(String docID) {
        Map<String, Object> docMap = null;
        if (documentCache.containsKey(docID)) {
            docMap = documentCache.get(docID);
        } else {
            docMap = readAndIndexDoc("id", docID, false);
        }
        return docMap;
    }

    private void addNewDataToRecord(Map<String, Object> docMap, Map<String, String> valuesToAdd) {
        Iterator<String> keyIter = valuesToAdd.keySet().iterator();
        while (keyIter.hasNext()) {
            String keyVal = keyIter.next();
            String addnlFieldVal = valuesToAdd.get(keyVal);
            addToMap(docMap, keyVal, addnlFieldVal);
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        logger.info("Starting Booklist processing.");
        BooklistReader reader = null;
        try {
            reader = new BooklistReader();
            reader.init(addArg(args, "NONE"));
        } catch (IllegalArgumentException e) {
            logger.error(e.getMessage());
            System.err.println(e.getMessage());
            System.exit(1);
        }
        int exitCode = reader.handleAll();
        System.exit(exitCode);
    }
}
