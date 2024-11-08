package system.container;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import system.log.LogMessage;
import system.mq.msg;

/**
 *
 * @author Nuno Brito, 29th of June 2011 in Darmstadt, Germany.
 */
public class ContainerFlatFile implements ContainerInterface {

    private long maxRecords = 1000;

    private HashMap<String, KnowledgeFile> knowledge = new HashMap();

    private String[] readPriority = new String[] {}, writePriority = new String[] {};

    private File rootFolder = null;

    private String id;

    private String[] fields;

    private String who = "ContainerFlatFile";

    private LogMessage logger = new LogMessage();

    public ContainerFlatFile(final String title, final String[] fields, File rootFolder, LogMessage result) {
        if (utils.text.isEmpty(title)) {
            result.add(who, msg.ERROR, "Title is empty");
            return;
        }
        if (fields == null) {
            result.add(who, msg.ERROR, "Fields are null");
            return;
        }
        this.id = title;
        this.who = "db-" + title;
        String out = "";
        for (String field : fields) {
            out = out.concat(field + ";");
        }
        this.fields = out.split(";");
        this.rootFolder = rootFolder;
        if (initialization() == false) {
            result.add(who, msg.ERROR, "Failed to initialize");
            return;
        }
        long plus = 0;
        for (String reference : readPriority) {
            KnowledgeFile current = knowledge.get(reference);
            plus += current.getCount();
        }
        result.add(who, msg.COMPLETED, "Container has started, %1 records are " + "available", "" + plus);
    }

    /** Initialize the work folder and files */
    private boolean initialization() {
        if (checkFolder() == false) return false;
        if (findKnowledgeFiles() == false) return false;
        sortKnowledgeFiles();
        processKnowledgeFiles();
        return true;
    }

    /**
     * Sort these files according to their ranking level.
     * Higher number = higher ranking = first to be processed
     */
    private void sortKnowledgeFiles() {
        String sort = "";
        for (int i = 9; i > 0; i--) {
            for (KnowledgeFile current : knowledge.values()) {
                if (current.getSettings().getProperty(msg.RANK, "1").equalsIgnoreCase("" + i) == false) continue;
                String name = current.toString();
                sort = sort.concat(name + ";");
            }
        }
        if (utils.text.isEmpty(sort)) {
            readPriority = new String[] {};
            writePriority = new String[] {};
        } else {
            readPriority = sort.split(";");
            writePriority = readPriority;
        }
    }

    /** Test if our folders are ready and available */
    private boolean checkFolder() {
        if (rootFolder.exists() == false) {
            boolean mkdir = rootFolder.mkdir();
            if (mkdir == false) {
                log(msg.ERROR, "Unable to create the '%1' folder", rootFolder.getAbsolutePath());
                return false;
            }
        }
        if ((rootFolder.exists() == false) || (rootFolder.isDirectory() == false) || (rootFolder.canRead() == false) || (rootFolder.canWrite() == false)) {
            log(msg.ERROR, "Unable to create the '%1' folder", rootFolder.getAbsolutePath());
            return false;
        }
        return true;
    }

    /** Read the file name and decompose all the properties mentioned */
    private Properties readFileName(File file) {
        Properties result = new Properties();
        String filename = file.getName();
        for (String property : filename.split("_")) {
            String[] out = property.split("-");
            if (out.length != 2) {
                log(msg.REFUSED, "Invalid key/value pair size of '%1' from '%2'", property, file.getAbsolutePath());
                continue;
            }
            String key = out[0];
            String value = out[1];
            result.setProperty(key, value);
        }
        log(msg.COMPLETED, "readFilename operation completed");
        return result;
    }

    /** Find knowledge files inside the target folder, crawl subfolders */
    private boolean findKnowledgeFiles() {
        ArrayList<File> list = utils.files.findfiles(rootFolder, 25);
        for (File file : list) {
            if (file.getName().substring(0, 2).equals("db") == false) continue;
            Properties currentKnowledge = this.readFileName(file);
            if (logger.getResult() == msg.ERROR) return false;
            if (currentKnowledge.getProperty("db").equalsIgnoreCase(id) != true) continue;
            KnowledgeFile knowhow = new KnowledgeFile(currentKnowledge, file);
            knowledge.put(knowhow.toString(), knowhow);
        }
        return true;
    }

    /** Process each Knowledge file that was found */
    private void processKnowledgeFiles() {
        if (knowledge.isEmpty()) {
            this.createKnowledgeFile();
        }
        for (String out : readPriority) {
            boolean doFullCheck = true;
            KnowledgeFile current = knowledge.get(out);
            Properties settings = current.getSettings();
            File file = current.getFile();
            if (settings.containsKey(msg.CHECKSUM)) {
                String checksumProvided = settings.getProperty(msg.CHECKSUM);
                String checksumGenerated = app.sentinel.ScannerChecksum.generateStringSHA256(file.getAbsolutePath());
                doFullCheck = checksumProvided.equalsIgnoreCase("" + checksumGenerated) == false;
            }
            if (doFullCheck) {
                boolean result = check(current);
                if (result == false) System.out.println(logger.getRecent());
            }
        }
    }

    /** Will verify that a given knowledge file is correct*/
    private boolean check(KnowledgeFile check) {
        Properties settings = check.getSettings();
        File file = check.getFile();
        String lines = utils.files.readAsString(file);
        long i = 0;
        int size = fields.length;
        if (lines.length() > 0) for (String record : lines.split("\n")) {
            ++i;
            String[] data = record.split(";");
            if (size != data.length) {
                log(msg.REFUSED, "Knowledge file '%1' has a data field sized" + " in %2 while we are expecting %3. Error" + " occurred in line %4", "" + file.getAbsolutePath(), "" + data.length, "" + size, "" + i);
                removeKnowledge(check.toString());
                return false;
            }
        }
        long count = Long.parseLong(settings.getProperty(msg.COUNT, "" + 0));
        if (count > 0) if (count != i) {
            log(msg.REFUSED, "Knowledge file '%1' has %2 records but reported %3", "" + file.getAbsolutePath(), "" + i, "" + size);
            removeKnowledge(check.toString());
            return false;
        }
        check.setCount(i);
        return true;
    }

    /** Add a given knowledge file to our internal lists */
    private KnowledgeFile addKnowledge(File newKnowledge) {
        Properties settings = new Properties();
        settings.setProperty(msg.COUNT, "" + 0);
        KnowledgeFile result = new KnowledgeFile(settings, newKnowledge);
        String identifier = newKnowledge.toString();
        readPriority = utils.text.stringArrayAdd(readPriority, identifier);
        writePriority = utils.text.stringArrayAdd(writePriority, identifier);
        knowledge.put(identifier, result);
        return result;
    }

    /** Remove a given knowledge file from our internal lists */
    public void deleteKnowledgeFiles() {
        for (String identifier : this.readPriority) {
            KnowledgeFile current = this.knowledge.get(identifier);
            if (current == null) {
                log(msg.IGNORED, "deleteKnowledgeFile operation: Could not find" + " knowledge file '%1'", identifier);
                return;
            }
            try {
                boolean delete = current.getFile().delete();
                if (delete == false) {
                    log(msg.ERROR, "deleteKnowledgeFile operation failed: " + "Could not delete file '%1'", current.getFile().getPath());
                }
            } catch (Exception e) {
            }
            if (current.getFile().exists()) {
                log(msg.ERROR, "deleteKnowledgeFile operation failed: " + "Could not delete file '%1'", current.getFile().getPath());
            }
            this.removeKnowledge(identifier);
        }
        readPriority = new String[] {};
        writePriority = new String[] {};
    }

    /** Remove a given knowledge file from our internal lists */
    private void removeKnowledge(String identifier) {
        readPriority = utils.text.stringArrayRemove(fields, identifier);
        writePriority = utils.text.stringArrayRemove(fields, identifier);
        knowledge.remove(identifier);
    }

    /** Write this record onto our containers */
    public Boolean write(final String[] fields) {
        if (writePreCheck(fields) == false) return false;
        if (knowledge.isEmpty()) {
            this.createKnowledgeFile();
        } else if (writeOverwrite(fields)) return true;
        if (writeCreateNew(fields)) return true;
        log(msg.ERROR, "Write operation: Failed to write key '%1'", fields[0]);
        return false;
    }

    /** Do the preflight checks for the write operation*/
    private boolean writePreCheck(final String[] fields) {
        if (fields.length != this.fields.length) {
            log(msg.ERROR, "Write operation failed: Attempted to write a " + "record with %1 columns on a table with %2 columns", "" + fields.length, "" + this.fields.length);
            return false;
        }
        if (utils.text.isEmpty(fields[0])) {
            log(msg.ERROR, "Write operation failed: Cannot accept an empty " + "value on the first parameter");
            return false;
        }
        return true;
    }

    /** Try to overwrite a key if it already exists */
    private boolean writeOverwrite(final String[] fields) {
        String find = fields[0];
        boolean success = false;
        for (String reference : readPriority) {
            KnowledgeFile current = knowledge.get(reference);
            File file = current.getFile();
            String lines = utils.files.readAsString(file);
            int i = 0;
            for (String record : lines.split("\n")) {
                ++i;
                String[] data = record.split(";");
                if (data[0].equals(find)) {
                    String recordModified = convertRecordToString(fields);
                    lines = lines.replaceFirst(record + "\n", recordModified);
                    success = utils.files.SaveStringToFile(file, lines);
                    if (success == false) {
                        log(msg.ERROR, "Write operation: Failed to save record '%1' " + "at file '%2'");
                        return false;
                    }
                    log(msg.COMPLETED, "Write operation: Overwrote" + " key '%1' at file '%2'", data[0], file.getPath());
                    return true;
                }
            }
        }
        return success;
    }

    /** Create a new file */
    private KnowledgeFile createKnowledgeFile() {
        File file = new File(rootFolder, "db-" + id + "_created-" + System.currentTimeMillis() + ".txt");
        try {
            boolean createNewFile = file.createNewFile();
            if (createNewFile == false) {
                log(msg.ERROR, "Create New KnowledgeFile operation failed:" + " Unable to create file '%1'", file.getPath());
                return null;
            }
        } catch (IOException ex) {
            log(msg.ERROR, "Create New KnowledgeFile operation failed:" + " Unable to create file '%1', an exception was " + "reported. %2", file.getPath(), ex.toString());
            return null;
        }
        KnowledgeFile addKnowledge = this.addKnowledge(file);
        return addKnowledge;
    }

    /** returns the first available knowledge file. Creates a new one 
     *  if none availble */
    private KnowledgeFile getFreeKnowledgeFile() {
        if (writePriority.length > 0) for (String reference : writePriority) {
            KnowledgeFile current = knowledge.get(reference);
            if (current.getCount() >= this.maxRecords) {
                writePriority = utils.text.stringArrayRemove(writePriority, reference);
                continue;
            }
            return current;
        }
        return this.createKnowledgeFile();
    }

    /** Create a new key */
    private boolean writeCreateNew(final String[] fields) {
        KnowledgeFile current = this.getFreeKnowledgeFile();
        File file = current.getFile();
        String lines = utils.files.readAsString(file);
        String recordModified = convertRecordToString(fields);
        lines = lines.concat(recordModified);
        boolean success = utils.files.SaveStringToFile(file, lines);
        if (success == false) {
            log(msg.ERROR, "Write operation: Failed to save record '%1' " + "at file '%2'");
            return false;
        }
        current.incCount();
        log(msg.COMPLETED, "Write operation: Wrote" + " key '%1' at file '%2'", fields[0], file.getPath());
        return true;
    }

    /**
     * Find all records that match a given string.
     * @param field the column to look
     * @find the text string to find
     *
     * The parameter field is case insensitive, the parameter find is case
     * sensitive.
     */
    public ArrayList<Properties> read(String field, String find) {
        ArrayList<Properties> result = new ArrayList();
        int pos = utils.text.arrayIndex(field, fields);
        if (pos == -1) {
            log(msg.ERROR, "Read operation failed: Field '%1' was not found.", field);
            return null;
        }
        for (String reference : this.readPriority) {
            KnowledgeFile current = this.knowledge.get(reference);
            File file = current.getFile();
            String lines = utils.files.readAsString(file);
            int i = 0;
            for (String record : lines.split("\n")) {
                ++i;
                String[] data = record.split(";");
                if (data[pos].equalsIgnoreCase(find)) {
                    result.add(convertRecordToProperties(data));
                }
            }
        }
        return result;
    }

    /** This is a clean and mean version of read. It will only retrieve
     the first record that matches our index key, assumed as being
     the first column on the records. It is case-sensitive, IT IS FAST.*/
    public String[] read(String find) {
        if (knowledge.isEmpty()) return new String[] { "" };
        for (String reference : this.readPriority) {
            KnowledgeFile current = this.knowledge.get(reference);
            File file = current.getFile();
            String lines = utils.files.readAsString(file);
            int i = 0;
            for (String record : lines.split("\n")) {
                ++i;
                String[] data = record.split(";");
                if (data[0].equals(find)) {
                    return data;
                }
            }
        }
        return new String[] { "" };
    }

    /** Converts a given record onto a Properties object */
    private Properties convertRecordToProperties(String[] record) {
        Properties result = new Properties();
        int i = -1;
        for (String field : fields) {
            i++;
            result.setProperty(field, record[i]);
        }
        return result;
    }

    /** Converts a given record onto a string that can be written on a file */
    private String convertRecordToString(String[] record) {
        String result = "";
        for (String field : record) result = result.concat(field + ";");
        result = result.concat("\n");
        return result;
    }

    /** Delete a given record from our container **/
    public boolean delete(String field, String find) {
        if (knowledge.isEmpty()) {
            log(msg.INACTIVE, "Delete operation not accepted: No knowledge" + " files available to process.");
            return false;
        }
        int fieldIndex = utils.text.arrayIndex(field, fields);
        if (fieldIndex < 0) {
            log(msg.ERROR, "Delete operation failed: Field %1 was not found.", field);
            return false;
        }
        long deletedCounter = 0;
        for (String reference : this.readPriority) {
            KnowledgeFile current = this.knowledge.get(reference);
            File file = current.getFile();
            String lines = utils.files.readAsString(file);
            int i = 0;
            for (String record : lines.split("\n")) {
                ++i;
                String[] data = record.split(";");
                if (data[fieldIndex].equals(find)) {
                    lines = lines.replaceAll(record + "\n", "");
                    boolean result = utils.files.SaveStringToFile(file, lines);
                    System.out.println();
                    if (result == true) {
                        current.decCount();
                        deletedCounter++;
                    } else {
                        return result;
                    }
                }
            }
        }
        if (deletedCounter == 0) {
            log(msg.ERROR, "Delete operation failed: Record %1 was not found on " + "field %2", find, field);
            return false;
        } else {
            log(msg.COMPLETED, "Delete operation: %1 records with id '%2' where deleted" + " from field '%3'", "" + deletedCounter, find, field);
            System.out.println("--->" + this.logger.getRecent());
            return true;
        }
    }

    /** Return the number of records available in our container */
    public long count() {
        long result = 0;
        for (String reference : readPriority) {
            KnowledgeFile current = knowledge.get(reference);
            result += current.getCount();
        }
        return result;
    }

    public long countBetween(long since, long until) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getName() {
        return this.id;
    }

    public boolean isRunning() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean stop() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /** Drop all our records */
    public void drop() {
        String[] target = this.readPriority;
        for (String reference : target) {
            this.removeKnowledge(reference);
        }
    }

    public String webRequest(Request request, Response response) {
        String action = utils.internet.getHTMLparameter(request, "action");
        String result = "";
        if (action.equalsIgnoreCase("count")) {
            result = "" + this.count();
            log(msg.INFO, "DB webRequest. Action 'count', we have " + result + " records inside our container");
            return result;
        }
        return result;
    }

    /** central logger for this class */
    private void log(final int gender, final String message, final String... args) {
        logger.add(who, gender, message, args);
    }

    public LogMessage getLog() {
        return logger;
    }

    public long getMaxRecordsAllowed() {
        return maxRecords;
    }

    /** Get all field titlrs of this container */
    public String[] getFields() {
        String out = "";
        for (String field : fields) out = out.concat(field + ";");
        return out.split(";");
    }
}

class KnowledgeFile {

    private Properties settings;

    private File file;

    private long count, modified;

    /** public constructor */
    public KnowledgeFile(Properties assignedSettings, File assignedFile) {
        if ((assignedSettings == null) || (assignedFile == null)) return;
        settings = assignedSettings;
        file = assignedFile;
        modified = file.lastModified();
        count = Integer.parseInt(settings.getProperty(msg.COUNT, "0"));
    }

    public File getFile() {
        return file;
    }

    public long getModified() {
        return modified;
    }

    public Properties getSettings() {
        return settings;
    }

    public long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    public void incCount() {
        this.count++;
    }

    public void decCount() {
        this.count--;
    }
}
