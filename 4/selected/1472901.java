package vqwiki.file;

import org.apache.log4j.Logger;
import vqwiki.*;
import vqwiki.db.DBDate;
import vqwiki.utils.TextFileFilter;
import vqwiki.utils.Utilities;
import java.io.*;
import java.util.*;

public class FileHandler implements Handler {

    public static final String VERSION_DIR = "versions";

    public static final String EXT = ".txt";

    private static final Logger logger = Logger.getLogger(FileHandler.class);

    protected Map readOnlyTopics;

    private static final String READ_ONLY_FILE = "ReadOnlyTopics";

    public static final String VIRTUAL_WIKI_LIST = "virtualwikis.lst";

    public static final String TEMPLATES_DIR = "templates";

    private File file;

    private final String LOCK_EXTENSION = ".lock";

    /**
     *
     */
    public FileHandler() {
        this.readOnlyTopics = new HashMap();
        createDefaults(Locale.ENGLISH);
    }

    /**
     * Set up the file system and default topics if necessary
     */
    public void createDefaults(Locale locale) {
        File dirCheck = new File(fileBase(""));
        dirCheck.mkdir();
        File versionDirCheck = new File(fileBase("") + VERSION_DIR);
        versionDirCheck.mkdir();
        File virtualList = new File(fileBase("") + VIRTUAL_WIKI_LIST);
        ResourceBundle messages = ResourceBundle.getBundle("ApplicationResources", locale);
        try {
            if (!virtualList.exists()) {
                createVirtualWikiList(virtualList);
            }
            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(virtualList), Environment.getInstance().getFileEncoding()));
            boolean lastOne = false;
            while (true) {
                String vWiki = in.readLine();
                if (vWiki == null) {
                    if (lastOne) {
                        break;
                    } else {
                        vWiki = "";
                        lastOne = true;
                    }
                }
                logger.debug("Creating defaults for " + vWiki);
                File dummy;
                dummy = getPathFor(vWiki, "");
                dummy.mkdir();
                dummy = getPathFor(vWiki, VERSION_DIR);
                dummy.mkdir();
                setupSpecialPage(vWiki, messages.getString("specialpages.startingpoints"));
                setupSpecialPage(vWiki, messages.getString("specialpages.textformattingrules"));
                setupSpecialPage(vWiki, messages.getString("specialpages.leftMenu"));
                setupSpecialPage(vWiki, messages.getString("specialpages.topArea"));
                setupSpecialPage(vWiki, messages.getString("specialpages.bottomArea"));
                setupSpecialPage(vWiki, messages.getString("specialpages.stylesheet"));
                setupSpecialPage(vWiki, messages.getString("specialpages.adminonlytopics"));
                setupSpecialPage(vWiki, messages.getString("specialpages.quickhelp"));
                setupSpecialPage(vWiki, messages.getString("specialpages.wikihelp"));
                setupSpecialPage(vWiki, messages.getString("specialpages.wikihelpbasicformatting"));
                setupSpecialPage(vWiki, messages.getString("specialpages.wikihelpadvancedformatting"));
                setupSpecialPage(vWiki, messages.getString("specialpages.wikihelpmakinglinks"));
                loadReadOnlyTopics(vWiki);
            }
            in.close();
        } catch (Exception ex) {
            logger.error(ex);
            ex.printStackTrace();
        }
    }

    /**
     *
     */
    private void setupSpecialPage(String vWiki, String specialPage) throws Exception {
        File dummy = getPathFor(vWiki, specialPage + ".txt");
        if (!dummy.exists()) {
            Writer writer = new OutputStreamWriter(new FileOutputStream(dummy), Environment.getInstance().getFileEncoding());
            writer.write(WikiBase.readDefaultTopic(specialPage));
            writer.close();
        }
    }

    /**
     *
     */
    private void createVirtualWikiList(File virtualList) throws IOException {
        PrintWriter writer = getNewPrintWriter(virtualList, true);
        writer.println(WikiBase.DEFAULT_VWIKI);
        writer.close();
    }

    /**
     *
     */
    public static File getPathFor(String virtualWiki, String dir, String fileName) {
        StringBuffer buffer = new StringBuffer();
        if (virtualWiki == null || virtualWiki.equals(WikiBase.DEFAULT_VWIKI)) {
            virtualWiki = "";
        }
        buffer.append(fileBase(virtualWiki));
        buffer.append(File.separator);
        if (dir != null) {
            buffer.append(dir);
            buffer.append(File.separator);
        }
        if (fileName != null) {
            buffer.append(Utilities.encodeSafeFileName(fileName));
        }
        return new File(buffer.toString());
    }

    /**
     *
     */
    public static File getPathFor(String virtualWiki, String fileName) {
        return getPathFor(virtualWiki, null, fileName);
    }

    /**
     * Reads a file from disk
     */
    public String read(String virtualWiki, String topicName) throws Exception {
        if (topicName.indexOf(System.getProperty("file.separator")) >= 0) {
            throw new WikiException("WikiNames may not contain special characters:" + topicName);
        }
        StringBuffer buffer = new StringBuffer();
        buffer.append(topicName);
        buffer.append(EXT);
        File file = getPathFor(virtualWiki, buffer.toString());
        StringBuffer contents = read(file);
        return contents.toString();
    }

    /**
     *
     */
    public StringBuffer read(File file) throws IOException {
        StringBuffer contents = new StringBuffer();
        if (file.exists()) {
            Reader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), Environment.getInstance().getFileEncoding()));
            boolean cr = false;
            while (true) {
                int c = in.read();
                if (c == -1) break;
                if (c == 13) cr = true;
                if (cr && c == 10) {
                    cr = false;
                    contents.append((char) 10);
                } else {
                    if (c == 13) {
                    } else if (cr) {
                        contents.append((char) 13);
                        contents.append((char) c);
                        cr = false;
                    } else {
                        contents.append((char) c);
                    }
                }
            }
            in.close();
        } else {
            logger.debug("File does not exist, returning default contents: " + file);
            contents.append("This is a new topic");
        }
        return contents;
    }

    /**
     * Checks if lock exists
     * @throws Exception 
     */
    public synchronized boolean holdsLock(String virtualWiki, String topicName, String key) throws Exception {
        File lockFile = makeLockFile(virtualWiki, topicName);
        if (!lockFile.exists()) {
            return lockTopic(virtualWiki, topicName, key);
        }
        java.util.Date lockedAt = new Date(lockFile.lastModified());
        VersionManager versionManager = WikiBase.getInstance().getVersionManagerInstance();
        java.util.Date lastRevision = versionManager.lastRevisionDate(virtualWiki, topicName);
        logger.debug("Checking for lock possession: locked at " + lockedAt + " last changed at " + lastRevision);
        if (lastRevision != null) {
            if (lastRevision.after(lockedAt)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Locks a file for editing
     */
    public synchronized boolean lockTopic(String virtualWiki, String topicName, String key) throws IOException {
        File lockFile = makeLockFile(virtualWiki, topicName);
        logger.debug("Locking " + topicName);
        Date currentDate = new Date();
        logger.debug("Edit timeout in minutes is " + Environment.getInstance().getEditTimeOut());
        long fiveMinutesAgo = currentDate.getTime() - 60000 * Environment.getInstance().getEditTimeOut();
        if (lockFile.exists()) {
            long mDate = lockFile.lastModified();
            logger.debug("Lock exists for " + topicName + " modified " + mDate);
            if (mDate < fiveMinutesAgo) {
                logger.debug("Lock has expired (timeout " + fiveMinutesAgo + ")");
                if (!lockFile.delete()) {
                    logger.info("Unable to delete lockfile [" + lockFile + "]");
                }
            } else {
                String lockKey = readLockFileKey(lockFile);
                if (key.equals(lockKey)) {
                    if (!lockFile.delete()) logger.info("Unable to delete lockfile [" + lockFile + "]");
                }
            }
        }
        if (!lockFile.createNewFile()) return false;
        Writer writer = new OutputStreamWriter(new FileOutputStream(lockFile), Environment.getInstance().getFileEncoding());
        writer.write(key);
        writer.close();
        return true;
    }

    /**
     *
     */
    public boolean exists(String virtualWiki, String topicName) throws Exception {
        File checkFile = getPathFor(virtualWiki, topicName + ".txt");
        return checkFile.exists();
    }

    /**
     * Create a lock file of the format topicName.lock
     */
    private File makeLockFile(String virtualWiki, String topicName) {
        StringBuffer buffer = new StringBuffer();
        if (virtualWiki.equals(WikiBase.DEFAULT_VWIKI)) virtualWiki = "";
        buffer.append(fileBase(virtualWiki));
        buffer.append(File.separator);
        buffer.append(Utilities.encodeSafeFileName(topicName));
        buffer.append(LOCK_EXTENSION);
        return new File(buffer.toString());
    }

    /**
     * Reads the key from a lockFile
     */
    private synchronized String readLockFileKey(File lockFile) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(lockFile), Environment.getInstance().getFileEncoding()));
        String lockKey = reader.readLine();
        reader.close();
        return lockKey;
    }

    /**
     * Unlocks a locked file
     */
    public synchronized void unlockTopic(String virtualWiki, String topicName) throws IOException {
        File lockFile = getPathFor(virtualWiki, topicName + LOCK_EXTENSION);
        if (!lockFile.exists()) {
            logger.warn("attempt to unlock topic by deleting lock file failed (file does not exist): " + lockFile);
        }
        if (!lockFile.delete()) logger.info("Unable to delete lockfile [" + lockFile + "]");
    }

    /**
     * Write contents to file
     * Write to version file if versioning is on
     */
    public synchronized void write(String virtualWiki, String contents, boolean convertTabs, String topicName) throws Exception {
        if (topicName.indexOf(System.getProperty("file.separator")) >= 0) {
            throw new WikiException("WikiNames may not contain special characters:" + topicName);
        }
        File versionFile = getPathFor(virtualWiki, VERSION_DIR, topicName + EXT + "." + Utilities.fileFriendlyDate(new Date()));
        File file = getPathFor(virtualWiki, topicName + EXT);
        PrintWriter writer = getNewPrintWriter(file, true);
        PrintWriter versionWriter = null;
        if (Environment.getInstance().isVersioningOn()) {
            versionWriter = getNewPrintWriter(versionFile, true);
        }
        if (convertTabs) {
            contents = Utilities.convertTabs(contents);
        }
        if (Environment.getInstance().isVersioningOn()) {
            logger.debug("Writing version: " + versionFile);
            versionWriter.print(contents);
            versionWriter.close();
        }
        logger.debug("Writing topic: " + file);
        writer.print(contents);
        writer.close();
    }

    /**
     *  returns a printwriter using utf-8 encoding
     *
     */
    private PrintWriter getNewPrintWriter(File file, boolean autoflush) throws IOException {
        return new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), Environment.getInstance().getFileEncoding()), autoflush);
    }

    /**
     * Write the read-only list out to disk
     */
    protected synchronized void saveReadOnlyTopics(String virtualWiki) throws IOException {
        File roFile = getPathFor(virtualWiki, READ_ONLY_FILE);
        logger.debug("Saving read-only topics to " + roFile);
        Writer out = new OutputStreamWriter(new FileOutputStream(roFile), Environment.getInstance().getFileEncoding());
        Iterator it = ((Collection) this.readOnlyTopics.get(virtualWiki)).iterator();
        while (it.hasNext()) {
            out.write((String) it.next() + System.getProperty("line.separator"));
        }
        out.close();
        logger.debug("Saved read-only topics: " + this.readOnlyTopics);
    }

    /**
     * Makes check to see if the specified topic is read-only. The check is case-insensitive.
     * @param virtualWiki the virtual wiki it appears in
     * @param topicName the name of the topic
     * @return
     * @throws Exception
     */
    public boolean isTopicReadOnly(String virtualWiki, String topicName) throws Exception {
        logger.debug("isTopicReadonly: " + virtualWiki + "/" + topicName);
        if (readOnlyTopics == null) {
            return false;
        } else {
            if (readOnlyTopics.get(virtualWiki) == null) {
                return false;
            }
            Collection readOnlyTopicsForVWiki = ((Collection) readOnlyTopics.get(virtualWiki));
            for (Iterator iterator = readOnlyTopicsForVWiki.iterator(); iterator.hasNext(); ) {
                String readOnlyTopicName = (String) iterator.next();
                if (topicName.equalsIgnoreCase(readOnlyTopicName)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Return a list of all read-only topics
     */
    public Collection getReadOnlyTopics(String virtualWiki) throws Exception {
        logger.debug("Returning read only topics for " + virtualWiki);
        return (Collection) this.readOnlyTopics.get(virtualWiki);
    }

    /**
     * Read the read-only topics from disk
     */
    protected synchronized void loadReadOnlyTopics(String virtualWiki) {
        logger.debug("Loading read only topics for " + virtualWiki);
        Collection roTopics = new ArrayList();
        File roFile = getPathFor(virtualWiki, READ_ONLY_FILE);
        if (!roFile.exists()) {
            logger.debug("Empty read only topics for " + virtualWiki);
            if (virtualWiki == null || virtualWiki.equals("")) {
                virtualWiki = WikiBase.DEFAULT_VWIKI;
            }
            this.readOnlyTopics.put(virtualWiki, roTopics);
            return;
        }
        logger.debug("Loading read-only topics from " + roFile);
        BufferedReader in = null;
        try {
            roFile.createNewFile();
            in = new BufferedReader(new InputStreamReader(new FileInputStream(roFile), Environment.getInstance().getFileEncoding()));
        } catch (IOException e) {
            logger.error(e);
        }
        while (true) {
            String line = null;
            try {
                line = in.readLine();
            } catch (IOException e) {
                logger.error(e);
            }
            if (line == null) break;
            roTopics.add(line);
        }
        try {
            in.close();
        } catch (IOException e) {
            logger.error(e);
        }
        if (virtualWiki.equals("")) {
            virtualWiki = WikiBase.DEFAULT_VWIKI;
        }
        this.readOnlyTopics.put(virtualWiki, roTopics);
    }

    /**
     *
     */
    public static String fileBase(String virtualWiki) {
        return Environment.getInstance().getHomeDir() + Utilities.sep() + virtualWiki;
    }

    /**
     *
     */
    public void addReadOnlyTopic(String virtualWiki, String topicName) throws Exception {
        logger.debug("Adding read-only topic: " + topicName);
        Collection roTopics = (Collection) this.readOnlyTopics.get(virtualWiki);
        roTopics.add(topicName);
        this.saveReadOnlyTopics(virtualWiki);
    }

    /**
     *
     */
    public void removeReadOnlyTopic(String virtualWiki, String topicName) throws Exception {
        logger.debug("Removing read-only topic: " + topicName);
        ((Collection) this.readOnlyTopics.get(virtualWiki)).remove(topicName);
        this.saveReadOnlyTopics(virtualWiki);
    }

    /**
     *
     */
    public void initialise(Locale locale) throws Exception {
        this.createDefaults(locale);
    }

    /**
     *
     */
    public Collection getVirtualWikiList() throws Exception {
        Collection all = new ArrayList();
        File file = getPathFor("", VIRTUAL_WIKI_LIST);
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), Environment.getInstance().getFileEncoding()));
        while (true) {
            String line = in.readLine();
            if (line == null) break;
            all.add(line);
        }
        in.close();
        if (!all.contains(WikiBase.DEFAULT_VWIKI)) {
            all.add(WikiBase.DEFAULT_VWIKI);
        }
        return all;
    }

    /**
     *
     */
    public Collection getTemplateNames(String virtualWiki) throws Exception {
        File file = getPathFor(virtualWiki, TEMPLATES_DIR);
        file.mkdir();
        Collection all = new ArrayList();
        String[] filenames = file.list(new TextFileFilter());
        if (filenames != null) {
            for (int i = 0; i < filenames.length; i++) {
                String filename = filenames[i];
                all.add(filename.substring(0, filename.length() - 4));
            }
        }
        return all;
    }

    /**
     *
     */
    public String getTemplate(String virtualWiki, String templateName) throws Exception {
        File dir = getPathFor(virtualWiki, TEMPLATES_DIR);
        File file = new File(dir, templateName + EXT);
        StringBuffer buffer = new StringBuffer();
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
        int nextByte;
        while (-1 != (nextByte = in.read())) {
            char nextChar = (char) nextByte;
            buffer.append(nextChar);
        }
        in.close();
        return buffer.toString();
    }

    /**
     *
     */
    public void addVirtualWiki(String virtualWiki) throws Exception {
        Collection all = new ArrayList();
        File file = getPathFor("", VIRTUAL_WIKI_LIST);
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), Environment.getInstance().getFileEncoding()));
        while (true) {
            String line = in.readLine();
            if (line == null) break;
            all.add(line);
        }
        in.close();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), Environment.getInstance().getFileEncoding()));
        for (Iterator iterator = all.iterator(); iterator.hasNext(); ) {
            String s = (String) iterator.next();
            writer.println(s);
        }
        writer.println(virtualWiki);
        writer.close();
    }

    /**
     *
     */
    public Collection purgeDeletes(String virtualWiki) throws Exception {
        Collection all = new ArrayList();
        file = getPathFor(virtualWiki, "");
        File[] files = file.listFiles(new TextFileFilter());
        for (int i = 0; i < files.length; i++) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(files[i]), Environment.getInstance().getFileEncoding()));
            String line = reader.readLine();
            reader.close();
            if (line != null) {
                if (line.trim().equals("delete")) {
                    files[i].delete();
                    String name = files[i].getName();
                    all.add(Utilities.decodeSafeFileName(name.substring(0, name.length() - 4)));
                }
            }
        }
        return all;
    }

    /**
     *
     */
    public void purgeVersionsOlderThan(String virtualWiki, DBDate date) throws Exception {
        throw new UnsupportedOperationException("New version purging available for file handler yet");
    }

    /**
     *
     */
    public void saveAsTemplate(String virtualWiki, String templateName, String contents) throws Exception {
        File dir = getPathFor(virtualWiki, TEMPLATES_DIR);
        logger.debug("saving template: " + templateName + " to " + dir);
        dir.mkdir();
        File file = new File(dir, templateName + EXT);
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), Environment.getInstance().getFileEncoding()));
        writer.print(contents);
        writer.close();
    }

    /**
     *
     */
    public List getLockList(String virtualWiki) throws Exception {
        if (virtualWiki == null) virtualWiki = "";
        List all = new ArrayList();
        File path = getPathFor(virtualWiki, "");
        File[] files = path.listFiles(new FileExtensionFilter(LOCK_EXTENSION));
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            String fileName = file.getName();
            logger.debug("filename: " + fileName);
            String topicName = fileName.substring(0, fileName.indexOf("."));
            DBDate lockedAt = new DBDate(new Date(file.lastModified()));
            all.add(new TopicLock(virtualWiki, topicName, lockedAt, readLockFileKey(file)));
        }
        return all;
    }
}
