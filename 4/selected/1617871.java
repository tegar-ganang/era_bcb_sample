package javab.ootil.config.ConfigEditor;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Map.Entry;
import javab.ootil.Ootil;

/**
 * A class to write ConfigSections. After you are done using 
 * the ConfigWriter you should release all resources with 
 * releaseResources();
 * @author Babyface
 *
 */
public class ConfigWriter extends ConfigEditor {

    protected FileLock lock;

    protected PrintWriter printWriter;

    private static final String STARTING_COMMENT = "/*";

    private static final String ENDING_COMMENT = "*/";

    private static final String EQUALS = " = ";

    private static final String END_LINE = ";";

    /***
     * Creates a ConfigWriter for writing ConfigSections
     */
    public ConfigWriter(String configFile, ConfigErrorHandler errorHandler) {
        super(configFile, errorHandler);
        FileOutputStream out = openFileOutputStream();
        this.lock = acquireLock(out);
        this.printWriter = openPrintWriter(out);
    }

    /**
     * Tries to open a FileOutputStream.
     * Returns null if there is an error
     */
    private FileOutputStream openFileOutputStream() {
        FileOutputStream stream = null;
        try {
            stream = new FileOutputStream(getFile());
        } catch (Exception e) {
            e.printStackTrace();
            errorHandler.showErrorWritingToConfigFileMessage();
            return null;
        }
        return stream;
    }

    /**
     * Tries to acquire a lock on the FileOutputStream
     * Returns null if there is an error
     */
    private FileLock acquireLock(FileOutputStream out) {
        FileLock lock = null;
        try {
            lock = out.getChannel().lock();
        } catch (Exception e) {
            e.printStackTrace();
            errorHandler.showConfigFileLockErrorMessage();
            return null;
        }
        return lock;
    }

    /**
     * Tries to open a PrintWriter
     * Returns null if there is an error
     */
    private PrintWriter openPrintWriter(FileOutputStream out) {
        if (out == null) {
            return null;
        } else {
            return new PrintWriter(out);
        }
    }

    /***
     * Saves all configSections before the CONFIG_FILE_SECTION_NUMBER section. 
     * <p>
     * This method writes the CONFIG_FILE_SECTION_NUMBER's comment for you 
     * (since it will not change).
     * <p>
     * @param fileSections - The array of ConfigSections
     * @param CONFIG_FILE_SECTION_NUMBER The ConfigSection number you are going to change
     */
    public void saveConfigSections(ArrayList<ConfigSection> fileSections) {
        if (printWriter == null) {
            System.err.println("ConfigWriter PrintWriter is null");
            return;
        }
        for (ConfigSection s : fileSections) {
            printWriter.print(STARTING_COMMENT);
            printWriter.print(s.getComment());
            printWriter.println(ENDING_COMMENT);
            for (Entry<String, String> e : s.getMap().entrySet()) {
                printWriter.print(e.getKey());
                printWriter.print(EQUALS);
                printWriter.print(e.getValue());
                printWriter.println(END_LINE);
            }
            printWriter.println();
        }
    }

    /**
     * Releases all resources used by this ConfigWriter
     */
    public void releaseResources() {
        Ootil.close(printWriter);
        try {
            if (lock != null) lock.release();
        } catch (IOException e) {
        }
    }
}
