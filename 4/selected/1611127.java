package org.oclc.da.ndiipp.extensions.heritrix;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import org.apache.commons.httpclient.Header;
import org.archive.io.arc.ARCReader;
import org.archive.io.arc.ARCReaderFactory;
import org.archive.io.arc.ARCRecord;
import org.archive.io.arc.ARCRecordMetaData;

/**
  * HeritrixARCExtractor
  *
  * This class will analyze ARC files created by the heritrix spider.
  * It will extract them onto a file system and create a metadata
  * document that contains information on each file.
  * 
  * NOTE: This class is intended to be installed in another application,
  * so it does not follow all of the exception and logging standards used
  * by typical application code.
  * 
  * An extract.txt file will be created in the same directory as the ARC 
  * location specified. Each entry in extract.txt will contain the
  * following lines:
  * 
  * Line 1: Location of file extracted from ARC
  * Line 2+: Header Information about the file
  * Last Line: Blank line separator
  * 
  * @author JCG
  * @version 1.0, 
  * @created 07/20/2005
  */
public class HeritrixARCExtractor {

    /** Name of the file that stores extracted file info. */
    public static final String EXTRACT_FILE = "extract.txt";

    /** Name of the file that stores extracted file info. */
    public static final String ALIASES_FILE = "aliases.txt";

    /** The common prefix of all files extracted from the arc. */
    public static final String EXT_PREFIX = "FILE";

    /** Extension of uncompressed ARC file. */
    private static final String ARC_EXT = ".arc";

    /** Extension of compressed ARC file. */
    private static final String GZIP_EXT = ".gz";

    /** Location header field for redirects. */
    private static final String HDR_LOCATION = "Location";

    /** Root directory where Heritrix ARC files are stored. */
    private File arcLoc = null;

    /** Extraction file for the current harvest. */
    private File extractFile = null;

    /** Alias file for the current harvest. */
    private File aliasFile = null;

    /** Current file counter. */
    private long fileCount = 0;

    /** Alias table. */
    private Hashtable<String, String> aliases = new Hashtable<String, String>();

    /**
     * Construct a Heritrix ARC extractor.
     * <p>
     * @param arcLoc    Location of the ARC directory where the
     *                  downloaded content is stored.
     */
    public HeritrixARCExtractor(File arcLoc) {
        this.arcLoc = arcLoc;
    }

    /**
     * Extract file information from the current harvest.
     * It will use the ARC file location specified in the constructor.
     * It will create an extraction file in the same ARC directory and
     * also extract the content from the ARC files.
     * @throws Exception 
     */
    public void extract() throws Exception {
        File[] files = arcLoc.listFiles();
        Arrays.sort(files);
        if (files == null) {
            throw new Exception("Invalid path :" + arcLoc);
        }
        File extractFile = findExtractFile(files);
        if (extractFile == null) {
            createExtractFile(files);
        }
        writeAliasFile();
    }

    /**
     * Find extraction file in list of files.
     * Sets the extract file member variable if found.
     * <p>
     * @param files list of files
     * @return The extract file found or <code>null</code> if not found.
     */
    private File findExtractFile(File[] files) {
        for (int index = 0; index < files.length; index++) {
            String fileName = files[index].getName();
            if (EXTRACT_FILE.equals(fileName)) {
                extractFile = files[index];
                return extractFile;
            }
        }
        return null;
    }

    /**
     * Parse the heritrix ARC files and create an extraction file.
     * Also, files will be extracted from the ARC files and loaded onto
     * disk relative to the ARC directory specified in the constructor.
     * <p>
     * @param files List of files in the ARC directory. These will
     *              typically be ARC files.
     * @throws Exception 
     */
    private void createExtractFile(File[] files) throws Exception {
        extractFile = new File(arcLoc, EXTRACT_FILE);
        extractFile.createNewFile();
        for (int index = 0; index < files.length; index++) {
            String fileName = files[index].getName().toLowerCase();
            if ((fileName.endsWith(ARC_EXT)) || (fileName.endsWith(GZIP_EXT))) {
                extractARC(files[index]);
            }
        }
    }

    /**
     * Extract files from the current ARC file specified.
     * Also, update the extract file with file info.
     * <p>
     * @param arcFile   A Heritrix ARC file to extract from.
     * @throws Exception 
     */
    private void extractARC(File arcFile) throws Exception {
        FileWriter writer = null;
        BufferedWriter fileOut = null;
        try {
            writer = new FileWriter(extractFile.toString(), true);
            fileOut = new BufferedWriter(writer);
            ARCReader reader = ARCReaderFactory.get(arcFile);
            Iterator recs = reader.iterator();
            while (recs.hasNext()) {
                ARCRecord current = null;
                try {
                    current = (ARCRecord) recs.next();
                    extractRecord(current, fileOut);
                    updateAliases(current);
                } finally {
                    if (current != null) {
                        current.close();
                    }
                }
            }
        } finally {
            if (fileOut != null) {
                fileOut.close();
            }
            if (writer != null) {
                writer.close();
            }
        }
    }

    /**
     * Extract the next file from the ARC.
     * <p>
     * @param arcRec    An ARC record for a file to extract.
     * @param fileOut   The output stream to the extract file.
     * @throws Exception 
     */
    private void extractRecord(ARCRecord arcRec, BufferedWriter fileOut) throws Exception {
        arcRec.skipHttpHeader();
        if (!rejectRecord(arcRec)) {
            File file = writeContentFile(arcRec);
            fileOut.write(file.toString());
            fileOut.newLine();
            writeHeaderFields(arcRec, fileOut);
            fileOut.newLine();
            fileOut.flush();
        }
    }

    /**
     * Determine if the ARC record specified should be rejected for extraction.
     * <p>
     * @param arcRec    An ARC record to check.
     * @return <code>true</code> if record should be rejected.
     *         <code>false</code> otherwise.
     */
    private boolean rejectRecord(ARCRecord arcRec) {
        int statusCode = arcRec.getStatusCode();
        return ((statusCode < 200) || (statusCode > 299));
    }

    /**
     * Determine if the ARC record contains a redirect (alias).
     * <p>
     * @param arcRec    An ARC record to check.
     * @return <code>true</code> if record shows a redirect.
     *         <code>false</code> otherwise.
     */
    private boolean isRedirect(ARCRecord arcRec) {
        int statusCode = arcRec.getStatusCode();
        return ((statusCode >= 300) && (statusCode <= 399));
    }

    /**
     * Write the header fields to the extract file.
     * <p>
     * @param arcRec    An ARC record for a file to extract.
     * @param fileOut   The output stream to the extract file.
     * @throws Exception 
     */
    private void writeHeaderFields(ARCRecord arcRec, BufferedWriter fileOut) throws Exception {
        ARCRecordMetaData metadata = arcRec.getMetaData();
        Iterator keys = metadata.getHeaderFieldKeys().iterator();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            Object value = metadata.getHeaderValue(key);
            fileOut.write(key + "=" + value);
            fileOut.newLine();
        }
    }

    /**
     * Write the content file from the ARC to disk.
     * <p>
     * @param arcRec    An ARC record for a file to extract.
     * @return The file that was written.
     * @throws Exception 
     */
    private File writeContentFile(ARCRecord arcRec) throws Exception {
        byte[] buffer = new byte[4000];
        File filePath = new File(arcLoc, EXT_PREFIX + (++fileCount));
        FileOutputStream fileOut = null;
        try {
            fileOut = new FileOutputStream(filePath);
            int read = 0;
            while ((read = arcRec.read(buffer)) != -1) {
                fileOut.write(buffer, 0, read);
            }
        } finally {
            if (fileOut != null) {
                fileOut.close();
            }
        }
        return filePath;
    }

    /**
     * Update the alias table, if an alias is found.
     * Typically, an HTTP 3xx response code indicates an alias.
     * <p>
     * @param arcRec    An ARC record for a file to extract.
     */
    private void updateAliases(ARCRecord arcRec) {
        if (isRedirect(arcRec)) {
            ARCRecordMetaData metadata = arcRec.getMetaData();
            String url = metadata.getUrl();
            String location = null;
            Header[] headers = arcRec.getHttpHeaders();
            for (int i = 0; i < headers.length; i++) {
                String field = headers[i].getName();
                String value = headers[i].getValue();
                if (HDR_LOCATION.equalsIgnoreCase(field)) {
                    location = headers[i].getValue();
                }
            }
            if ((url != null) && (location != null)) {
                aliases.put(url, location);
            }
        }
    }

    /**
     * Write the aliases file.
     * @throws Exception 
     */
    private void writeAliasFile() throws Exception {
        aliasFile = new File(arcLoc, ALIASES_FILE);
        aliasFile.createNewFile();
        FileWriter writer = null;
        BufferedWriter fileOut = null;
        try {
            writer = new FileWriter(aliasFile);
            fileOut = new BufferedWriter(writer);
            Iterator keys = aliases.keySet().iterator();
            while (keys.hasNext()) {
                String key = (String) keys.next();
                String alias = (String) aliases.get(key);
                fileOut.write(key);
                fileOut.newLine();
                fileOut.write(alias);
                fileOut.newLine();
                fileOut.newLine();
            }
        } finally {
            if (fileOut != null) {
                fileOut.close();
            }
            if (writer != null) {
                writer.close();
            }
        }
    }

    /**
     * Run Heritrix extraction.
     * <p>
     * @param args  Command line arguments. First argument should be the
     *              location of the ARC directory to extract from.
     * @throws Exception 
     */
    public static void main(String[] args) throws Exception {
        if (args.length >= 1) {
            HeritrixARCExtractor extractor = new HeritrixARCExtractor(new File(args[0]));
            extractor.extract();
        }
    }
}
