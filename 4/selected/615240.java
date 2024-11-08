package uk.ac.ebi.interpro.scan.io.model;

import uk.ac.ebi.interpro.scan.io.ParseException;
import java.io.*;
import java.nio.channels.FileLock;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class creates a map of model names to GA values, for a particular model
 * file.  If this object is being created for the first time, it parses the model
 * file to create this mapping.  This can be a little time consuming, so it then
 * saves the mapping to a Properties file with an extension MAP_FILE_EXTENSION, located
 * in the same place as the model file.  On subsequent instantiations of this object,
 * the properties file will be loaded instead to give better performance.
 * Note that this method uses java.nio.channels to ensure correct file locking, so is
 * both thread safe and multiple-process / multiple JVM safe.
 * <p/>
 * The values are stored initially as a String, comprising the sequence GA value
 * followed by white space and then the domain GA value.  They are kept in this format
 * to make placing into a properties file more simple.
 * <p/>
 * Finally this class parses this simple String to return the sequence and
 * domain GA values as double values.
 *
 * @author Phil Jones
 * @version $Id: GaValuesRetriever.java 1521 2011-11-11 17:28:49Z philip.j.r.jones@gmail.com $
 * @since 1.0
 */
public class GaValuesRetriever implements Serializable {

    /**
     * Extracts the GA values in the forms "24.0 24.0" as group 1.
     */
    private static final Pattern GA_LINE_PATTERN = Pattern.compile("^GA\\s+(.+);?\\s*$");

    private static final Pattern ACCESSION_PATTERN = Pattern.compile("^ACC\\s+([A-Z0-9]+)\\.?.*$");

    private static final String END_OF_RECORD = "//";

    private Properties accessionToGAProps = null;

    private static final String MAP_FILE_EXTENSION = ".accession_to_ga_map";

    private static final Map<String, Double> ACC_TO_SEQUENCE_GA = new HashMap<String, Double>();

    private static final Map<String, Double> ACC_TO_DOMAIN_GA = new HashMap<String, Double>();

    private boolean initialised = false;

    private final String modelFileAbsolutePath;

    /**
     * Constructs a new Map of names to accessions based upon the model file passed in
     * as an argument.
     *
     * @param modelFileAbsolutePath the model file to parse.
     * @throws java.io.IOException in the event of a problem reading the file or cleaning up afterwards.
     * @throws uk.ac.ebi.interpro.scan.io.ParseException
     *                             if an Exception is thrown during parsing.
     */
    public GaValuesRetriever(String modelFileAbsolutePath) throws IOException {
        this.modelFileAbsolutePath = modelFileAbsolutePath;
    }

    private File createMapFileObject(String modelFileAbsolutePath) throws IOException {
        return new File(modelFileAbsolutePath + MAP_FILE_EXTENSION);
    }

    /**
     * Save the parsed mapping to a Properties file so they can be accessed more quickly on
     * subsequent runs.
     * <p/>
     * This method uses java.nio.channels to lock the mapping file / check for locks
     * on partially written mapping files.
     * <p/>
     * These locks only work cross-process, not cross-thread, so the method is also
     * synchronized.
     *
     * @param modelFileAbsolutePath the absolute path to the Model file.
     * @throws java.io.IOException thrown when writing / locking / closing streams.
     */
    private synchronized void storeMappingToPropertiesFile(String modelFileAbsolutePath) throws IOException {
        File mapFile = createMapFileObject(modelFileAbsolutePath);
        if (mapFile.exists()) {
            return;
        }
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mapFile);
            FileLock lock = fos.getChannel().lock();
            accessionToGAProps.store(fos, "Mapping of model accessions to GA values.");
            lock.release();
        } catch (Exception ioe) {
            mapFile.delete();
        } finally {
            if (fos != null) {
                fos.close();
            }
        }
    }

    /**
     * Load the mappings from a Properties file so they can be accessed more quickly.
     * <p/>
     * This method uses java.nio.channels to lock the mapping file / check for locks
     * on partially written mapping files.
     * <p/>
     * These locks only work cross-process, not cross-thread, so the method is also
     * synchronized.
     *
     * @param mapFile being a handle on the Properties file.
     * @throws java.io.IOException thrown when reading / locking / closing streams.
     */
    private synchronized void loadMappingFromPropertiesFile(File mapFile) throws IOException {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(mapFile);
            FileLock lock = fis.getChannel().lock(0, Long.MAX_VALUE, true);
            accessionToGAProps = new Properties();
            accessionToGAProps.load(fis);
            lock.release();
        } finally {
            if (fis != null) {
                fis.close();
            }
        }
    }

    public void parseModelFile(String modelFileAbsolutePath) throws IOException {
        BufferedReader reader = null;
        accessionToGAProps = new Properties();
        try {
            reader = new BufferedReader(new FileReader(modelFileAbsolutePath));
            String accession = null, gaValues = null;
            int lineNumber = 0;
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                char firstChar = line.charAt(0);
                if (firstChar == '/' || firstChar == 'G' || firstChar == 'A') {
                    if (line.startsWith(END_OF_RECORD)) {
                        if (accession == null || gaValues == null) {
                            throw new IllegalStateException("The HMMER3 model file '" + modelFileAbsolutePath + "' contains a record that does not contain a accession and GA row.  PfamModel ending on line number " + lineNumber);
                        }
                        if (accessionToGAProps.containsKey(accession)) {
                            throw new IllegalStateException("The HMMER3 model file '" + modelFileAbsolutePath + "' contains a duplicated model accession " + accession);
                        }
                        accessionToGAProps.put(accession, gaValues);
                        accession = null;
                        gaValues = null;
                    } else if (line.startsWith("ACC")) {
                        Matcher nameMatcher = ACCESSION_PATTERN.matcher(line);
                        if (nameMatcher.matches()) {
                            if (accession != null) {
                                throw new IllegalStateException("The HMMER3 model file '" + modelFileAbsolutePath + "' contains a record that appears to contain more than one accession row.  PfamModel ending on line number " + lineNumber);
                            }
                            accession = nameMatcher.group(1);
                        }
                    } else if (line.startsWith("GA")) {
                        Matcher accessionMatcher = GA_LINE_PATTERN.matcher(line);
                        if (accessionMatcher.matches()) {
                            if (gaValues != null) {
                                throw new IllegalStateException("The HMMER3 model file '" + modelFileAbsolutePath + "' contains a record that appears to contain more than one GA row.  PfamModel ending on line number " + lineNumber);
                            }
                            gaValues = accessionMatcher.group(1);
                        }
                    }
                }
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    public double getSequenceGAForAccession(String modelAccession) throws IOException {
        lazyInitialise();
        return ACC_TO_SEQUENCE_GA.get(modelAccession);
    }

    public double getDomainGAForAccession(String modelAccession) throws IOException {
        lazyInitialise();
        return ACC_TO_DOMAIN_GA.get(modelAccession);
    }

    private void lazyInitialise() throws IOException {
        if (!initialised) {
            File mapFile = createMapFileObject(modelFileAbsolutePath);
            if (mapFile.exists()) {
                loadMappingFromPropertiesFile(mapFile);
            } else {
                parseModelFile(modelFileAbsolutePath);
                storeMappingToPropertiesFile(modelFileAbsolutePath);
            }
            for (Object accessionObject : accessionToGAProps.keySet()) {
                String accession = (String) accessionObject;
                String gaString = (String) accessionToGAProps.get(accessionObject);
                gaString = gaString.trim();
                if (gaString.endsWith(";")) {
                    gaString = gaString.substring(0, gaString.length() - 1);
                }
                String values[] = gaString.split("\\s");
                if (values.length < 2) {
                    throw new ParseException("The GA line format is not as expected (was expecting at least two floating point numbers separated by a space).", "NOT_A_FILE", gaString, 1);
                }
                ACC_TO_SEQUENCE_GA.put(accession, new Double(values[0]));
                ACC_TO_DOMAIN_GA.put(accession, new Double(values[1]));
            }
            initialised = true;
        }
    }
}
