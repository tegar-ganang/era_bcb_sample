package eu.planets_project.ifr.core.services.comparison.comparator.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import eu.planets_project.ifr.core.techreg.formats.FormatRegistryFactory;
import eu.planets_project.services.utils.ProcessRunner;

/**
 * Wrapper for the comparator command-line tool. This functionality is exposed
 * in different ways via web services, implemented in the comparator service
 * implementation classes.
 * @author Fabian Steeg (fabian.steeg@uni-koeln.de)
 */
public final class ComparatorWrapper {

    /**
     * We enforce non-instantiability with a private constructor (this is a
     * static utility class).
     */
    private ComparatorWrapper() {
    }

    /** The filename of the result file. */
    private static final String RESULT_FILENAME = "copra.xml";

    /** A planets logger. */
    private static final Log LOG = LogFactory.getLog(ComparatorWrapper.class);

    /** The home of the comparator command-line tool. */
    private static final String XCLTOOLS_HOME = System.getenv("XCLTOOLS_HOME") + File.separator;

    static final String COMPARATOR_HOME = (XCLTOOLS_HOME + File.separator + "comparator" + File.separator + "v1.0" + File.separator).replace(File.separator + File.separator, File.separator);

    /** The default config file; is used when no config is specified. */
    private static final String DEFAULT_CONFIG = COMPARATOR_HOME + "defaultPCR.xml";

    /** The file names of the result and log files. */
    private static final String LOG_TXT = "log.txt";

    /** The comparator executable, has to be on the path on the server. */
    private static final String COMPARATOR = "comparator" + (System.getProperty("os.name").toLowerCase().contains("windows") ? ".exe" : "");

    /**
     * @param xcdl The base XCDL
     * @param xcdls The XCDLs to be compared with the base XCDL
     * @param pcr The comparison configuration
     * @return The comparison result of the XCDL compared to the other XCDLs
     */
    static String compare(final String xcdl, final List<String> xcdls, final String pcr) {
        File tempXcdl = createBaseXcdlFile(xcdl);
        List<File> tempXcdls = createXcdlFiles(xcdls);
        File tempFolder = createTempFolder(tempXcdl.getParent());
        File pcrFile = createConfigFile(pcr);
        File outputFolder = createOutputFolder(tempFolder.getAbsolutePath());
        List<String> commands = createCommands(tempXcdls, tempXcdl, outputFolder, pcrFile);
        callComparator(tempXcdl, tempXcdls, commands);
        String result = read(outputFolder.getAbsolutePath() + (outputFolder.getAbsolutePath().endsWith(File.separator) ? "" : File.separator) + RESULT_FILENAME);
        String logged = read(COMPARATOR_HOME + LOG_TXT);
        LOG.info("Comparator result: " + result);
        LOG.debug("Comparator log: " + logged);
        delete(tempXcdl);
        delete(tempXcdls.toArray(new File[] {}));
        return result;
    }

    /**
     * @return The file formats supported by the comparator
     */
    public static List<URI> getSupportedInputFormats() {
        List<URI> inputFormats = new ArrayList<URI>();
        inputFormats.add(FormatRegistryFactory.getFormatRegistry().createExtensionUri("XCDL"));
        return inputFormats;
    }

    /**
     * @param xcdl The XCDL
     * @return Returns a file containing the XCDL
     */
    private static File createBaseXcdlFile(final String xcdl) {
        File tempXcdl = tempFile("XCDL1");
        save(tempXcdl.getAbsolutePath(), xcdl);
        return tempXcdl;
    }

    /**
     * @param pcr The config
     * @return Returns a file containing the config
     */
    private static File createConfigFile(final String pcr) {
        File pcrFile = new File(COMPARATOR_HOME + "sentPCR.xml");
        if (pcr == null) {
            pcrFile = new File(DEFAULT_CONFIG);
        } else {
            save(pcrFile.getAbsolutePath(), pcr);
        }
        return pcrFile;
    }

    /**
     * @param tempFolder The location of the temp folder to use
     * @return Returns the temp folder
     */
    private static File createTempFolder(final String tempFolder) {
        File f = new File(tempFolder);
        if (!f.canRead() || !f.canWrite()) {
            throw new IllegalStateException("Can't read from or write to the temp folder: " + f.getAbsolutePath());
        }
        return f;
    }

    /**
     * @param xcdls The XCDLs
     * @return Returns a list of files for the XCDLs
     */
    private static List<File> createXcdlFiles(final List<String> xcdls) {
        List<File> tempXcdls = new ArrayList<File>();
        for (int i = 0; i < xcdls.size(); i++) {
            tempXcdls.add(tempFile("XCDL" + (i + 2)));
        }
        for (int i = 0; i < xcdls.size(); i++) {
            save(tempXcdls.get(i).getAbsolutePath(), xcdls.get(i));
        }
        return tempXcdls;
    }

    /**
     * @param parent The parent folder
     * @return Returns a file representing the output folder
     */
    private static File createOutputFolder(final String parent) {
        File outputFolder = new File(parent, "output");
        boolean mkdir = outputFolder.mkdir();
        if (!mkdir && !outputFolder.exists()) {
            throw new IllegalStateException("Could not create an output directory");
        }
        return outputFolder;
    }

    /**
     * @param tempXcdl The base XCDL file
     * @param tempXcdls The other XCDL files
     * @param commands The commands for the comparator tool
     */
    private static void callComparator(final File tempXcdl, final List<File> tempXcdls, final List<String> commands) {
        ProcessRunner pr = new ProcessRunner(commands);
        File home = new File(COMPARATOR_HOME);
        if (!home.exists()) {
            throw new IllegalStateException("COMPARATOR_HOME does not exist: " + COMPARATOR_HOME);
        }
        pr.setStartingDir(home);
        LOG.info("Executing: " + commands);
        if (!new File(tempXcdl.getAbsolutePath()).exists()) {
            throw new IllegalStateException("Temp files not accessible;");
        }
        for (File file : tempXcdls) {
            if (!new File(file.getAbsolutePath()).exists()) {
                throw new IllegalStateException("Temp files not accessible;");
            }
        }
        pr.run();
        LOG.info("Comparator call output: " + pr.getProcessOutputAsString());
        LOG.info("Comparator call error: " + pr.getProcessErrorAsString());
    }

    /**
     * @param tempXcdls The other XCDL files
     * @param tempXcdl The base XCDL file
     * @param outputFolder The output folder
     * @param pcrFile The config file
     * @return Returns a list of the commands to be passed for calling the
     *         comparator
     */
    private static List<String> createCommands(final List<File> tempXcdls, final File tempXcdl, final File outputFolder, final File pcrFile) {
        List<String> commands = new ArrayList<String>(Arrays.asList(COMPARATOR_HOME + COMPARATOR, tempXcdl.getAbsolutePath()));
        for (File file : tempXcdls) {
            commands.add(file.getAbsolutePath());
        }
        commands.addAll(Arrays.asList("-c", pcrFile.getAbsolutePath()));
        commands.addAll(Arrays.asList("-o", outputFolder.getAbsolutePath() + File.separator));
        commands.addAll(Arrays.asList("-recursive"));
        return commands;
    }

    /**
     * @param files The files to delete
     */
    private static void delete(final File... files) {
        for (File f : files) {
            if (!f.delete() && f.exists()) {
                throw new IllegalStateException("Could not delete temp file;");
            }
        }
    }

    /**
     * @param location The location of the text file to read
     * @return Return the content of the file at the specified location,
     *         replacing line breaks with blanks
     */
    static String read(final String location) {
        StringBuilder builder = new StringBuilder();
        Scanner s;
        try {
            s = new Scanner(new File(location));
            while (s.hasNextLine()) {
                builder.append(s.nextLine()).append(" \n");
            }
            String string = builder.toString();
            return string;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return "No result for: " + location;
    }

    /**
     * @param fileName The file name to write the specified content to
     * @param content The content to write to a file with the specified name
     */
    static void save(final String fileName, final String content) {
        try {
            FileWriter out = new FileWriter(fileName);
            out.write(content);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param name The name to use when generating the temp file
     * @return Returns a temp file created using File.createTempFile
     */
    private static File tempFile(final String name) {
        return tempFile(name, null);
    }

    /**
     * @param name The name to use when generating the temp file
     * @param suffix The suffix to use
     * @return Returns a temp file created using File.createTempFile
     */
    static File tempFile(final String name, final String suffix) {
        File input;
        try {
            input = File.createTempFile(name, suffix);
            input.deleteOnExit();
            return input;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
