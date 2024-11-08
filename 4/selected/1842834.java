package vqwiki.persistence.file;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.PrefixFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import vqwiki.Constants;
import vqwiki.Environment;
import vqwiki.TopicLock;
import vqwiki.WikiBase;
import vqwiki.WikiException;
import vqwiki.persistence.PersistenceHandler;
import vqwiki.plugin.PluginManager;
import vqwiki.svc.VersionManager;
import vqwiki.utils.SystemTime;
import vqwiki.utils.Utilities;

public class FileHandler implements PersistenceHandler {

    public static final String VERSION_DIR = "versions";

    public static final String TEXT_EXTENSION = ".txt";

    public static final String LOCK_EXTENSION = ".lock";

    public static final String READ_ONLY_FILE = "ReadOnlyTopics";

    public static final String TEMPLATES_DIR = "templates";

    public static final FilenameFilter textFilenameFilter = new SuffixFileFilter(TEXT_EXTENSION);

    public static final FilenameFilter lockFilenameFilter = new SuffixFileFilter(LOCK_EXTENSION);

    private static final Logger logger = Logger.getLogger(FileHandler.class.getName());

    protected Map readOnlyTopics;

    /**
     *
     */
    public FileHandler() {
        this.readOnlyTopics = new HashMap();
    }

    /**
     *
     */
    private void setupSpecialPage(String vWiki, String specialPage) throws Exception {
        File dummy = getPathFor(vWiki, specialPage + TEXT_EXTENSION);
        if (!dummy.exists()) {
            Writer writer = new OutputStreamWriter(new FileOutputStream(dummy), Environment.getInstance().getFileEncoding());
            writer.write(WikiBase.readDefaultTopic(specialPage));
            writer.close();
        }
    }

    /**
     *
     */
    public static File getPathFor(String virtualWiki, String dir, String fileName) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(getVirtualWikiFileBase(virtualWiki));
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
        File file = getPathForTopic(virtualWiki, topicName);
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
            logger.fine("File does not exist, returning default contents: " + file);
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
        Date lockedAt = new Date(lockFile.lastModified());
        VersionManager versionManager = WikiBase.getInstance().getVersionManager();
        Date lastRevision = versionManager.lastRevisionDate(virtualWiki, topicName);
        logger.fine("Checking for lock possession: locked at " + lockedAt + " last changed at " + lastRevision);
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
        logger.fine("Locking " + topicName + ", edit timeout in minutes is " + Environment.getInstance().getEditTimeout());
        long fiveMinutesAgo = SystemTime.asMillis() - 60000 * Environment.getInstance().getEditTimeout();
        if (lockFile.exists()) {
            long mDate = lockFile.lastModified();
            logger.fine("Lock exists for " + topicName + " modified " + mDate);
            if (mDate < fiveMinutesAgo) {
                logger.fine("Lock has expired (timeout " + fiveMinutesAgo + ")");
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
        return getPathFor(virtualWiki, topicName + TEXT_EXTENSION).exists();
    }

    /**
     * Create a lock file of the format topicName.lock
     */
    private File makeLockFile(String virtualWiki, String topicName) {
        StringBuffer buffer = new StringBuffer(getVirtualWikiFileBase(virtualWiki)).append(Utilities.encodeSafeFileName(topicName)).append(LOCK_EXTENSION);
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
            logger.log(Level.WARNING, "attempt to unlock topic by deleting lock file failed (file does not exist): " + lockFile);
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
        File versionFile = getPathFor(virtualWiki, VERSION_DIR, topicName + TEXT_EXTENSION + "." + FileUtilities.fileFriendlyDate(SystemTime.asDate()));
        File file = getPathForTopic(virtualWiki, topicName);
        PrintWriter writer = getNewPrintWriter(file, true);
        PrintWriter versionWriter = null;
        if (Environment.getInstance().isVersioningOn()) {
            versionWriter = getNewPrintWriter(versionFile, true);
        }
        if (convertTabs) {
            contents = Utilities.convertTabs(contents);
        }
        if (Environment.getInstance().isVersioningOn()) {
            logger.fine("Writing version: " + versionFile);
            versionWriter.print(contents);
            versionWriter.close();
        }
        logger.fine("Writing topic: " + file);
        writer.print(contents);
        writer.close();
    }

    /**
     *  returns a printwriter using utf-8 encoding
     *
     * @return
     * @param file
     * @param autoflush
     * @throws java.io.IOException
     */
    private PrintWriter getNewPrintWriter(File file, boolean autoflush) throws IOException {
        return new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), Environment.getInstance().getFileEncoding()), autoflush);
    }

    /**
     * Write the read-only list out to disk
     *
     * @param virtualWiki the virtualWiki name
     * @throws java.io.IOException
     */
    protected synchronized void saveReadOnlyTopics(String virtualWiki) throws IOException {
        File roFile = getPathFor(virtualWiki, READ_ONLY_FILE);
        logger.fine("Saving read-only topics to " + roFile);
        Writer out = new OutputStreamWriter(new FileOutputStream(roFile), Environment.getInstance().getFileEncoding());
        Iterator it = ((Collection) this.readOnlyTopics.get(virtualWiki)).iterator();
        while (it.hasNext()) {
            out.write((String) it.next() + IOUtils.LINE_SEPARATOR);
        }
        out.close();
        logger.fine("Saved read-only topics: " + this.readOnlyTopics);
    }

    /**
     * Makes check to see if the specified topic is read-only. The check is case-insensitive.
     *
     * @param virtualWiki the virtual wiki it appears in
     * @param topicName the name of the topic
     * @return true if the topic is read only
     * @throws Exception
     */
    public boolean isTopicReadOnly(String virtualWiki, String topicName) throws Exception {
        logger.fine("isTopicReadonly: " + virtualWiki + "/" + topicName);
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
        logger.fine("Returning read only topics for " + virtualWiki);
        return (Collection) this.readOnlyTopics.get(virtualWiki);
    }

    /**
     * Read the read-only topics from disk
     *
     * @param virtualWiki name of the virtual wiki
     */
    protected synchronized void loadReadOnlyTopics(String virtualWiki) {
        logger.fine("Loading read only topics for " + virtualWiki);
        Collection roTopics = new ArrayList();
        File roFile = getPathFor(virtualWiki, READ_ONLY_FILE);
        if (!roFile.exists()) {
            logger.fine("Empty read only topics for " + virtualWiki);
            if (virtualWiki == null || virtualWiki.equals("")) {
                virtualWiki = Constants.DEFAULT_VWIKI;
            }
            this.readOnlyTopics.put(virtualWiki, roTopics);
            return;
        }
        logger.fine("Loading read-only topics from " + roFile);
        BufferedReader in = null;
        try {
            roFile.createNewFile();
            in = new BufferedReader(new InputStreamReader(new FileInputStream(roFile), Environment.getInstance().getFileEncoding()));
        } catch (IOException e) {
            logger.log(Level.SEVERE, "", e);
        }
        while (true) {
            String line = null;
            try {
                line = in.readLine();
            } catch (IOException e) {
                logger.log(Level.SEVERE, "", e);
            }
            if (line == null) break;
            roTopics.add(line);
        }
        try {
            in.close();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "", e);
        }
        if (virtualWiki.equals("")) {
            virtualWiki = Constants.DEFAULT_VWIKI;
        }
        this.readOnlyTopics.put(virtualWiki, roTopics);
    }

    public static String getVirtualWikiFileBase(String virtualWiki) {
        StringBuffer sb = new StringBuffer(Environment.getInstance().getHomeDir());
        if (virtualWiki != null && !"".equals(virtualWiki) && !Constants.DEFAULT_VWIKI.equals(virtualWiki)) {
            sb.append(File.separator);
            sb.append(virtualWiki);
        }
        sb.append(File.separator);
        return sb.toString();
    }

    public void addReadOnlyTopic(String virtualWiki, String topicName) throws Exception {
        logger.fine("Adding read-only topic: " + topicName);
        Collection roTopics = (Collection) this.readOnlyTopics.get(virtualWiki);
        roTopics.add(topicName);
        this.saveReadOnlyTopics(virtualWiki);
    }

    public void removeReadOnlyTopic(String virtualWiki, String topicName) throws Exception {
        logger.fine("Removing read-only topic: " + topicName);
        ((Collection) this.readOnlyTopics.get(virtualWiki)).remove(topicName);
        this.saveReadOnlyTopics(virtualWiki);
    }

    /**
     * Set up the file system and default topics if necessary.
     *
     * @see vqwiki.persistence.PersistenceHandler#initialise(java.util.Locale)
     */
    public void initialise(Locale locale) throws Exception {
        String wikiHome = Environment.getInstance().getHomeDir();
        File dirCheck = new File(wikiHome);
        logger.fine("Using filebase: " + dirCheck);
        dirCheck.mkdir();
        try {
            addVirtualWiki(Constants.DEFAULT_VWIKI);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Collection getVirtualWikis() throws Exception {
        String wikiHome = Environment.getInstance().getHomeDir();
        List virtualWikiNames = new ArrayList();
        virtualWikiNames.add(Constants.DEFAULT_VWIKI);
        File[] files = new File(wikiHome).listFiles();
        final List protectedDirs = Arrays.asList(new String[] { VERSION_DIR, TEMPLATES_DIR, PluginManager.PLUGINS_DIR, Environment.getInstance().getStringSetting(Environment.PROPERTY_TEMP_DIR), Environment.getInstance().getStringSetting(Environment.PROPERTY_UPLOAD_DIR) });
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            if (file.isDirectory() && !protectedDirs.contains(file.getName())) {
                virtualWikiNames.add(file.getName());
            }
        }
        return virtualWikiNames;
    }

    public Collection getTemplateNames(String virtualWiki) throws Exception {
        File file = getPathFor(virtualWiki, TEMPLATES_DIR);
        file.mkdir();
        Collection all = new ArrayList();
        String[] filenames = file.list(textFilenameFilter);
        if (filenames != null) {
            for (int i = 0; i < filenames.length; i++) {
                String filename = filenames[i];
                all.add(filename.substring(0, filename.length() - 4));
            }
        }
        return all;
    }

    /**
     * Get the content of a template from the tamplate name and the virtual wiki instance
     * NOTE: The String returning the content has the file encoding specified
     *
     * @param virtualWiki Virtual wiki name referring to the template
     * @param templateName template name
     * @return the String containig the template code
     * @throws Exception possible exceptions are IOExceptions and UnupportedEncodingExceptions
     */
    public String getTemplate(String virtualWiki, String templateName) throws Exception {
        File file = new File(getPathFor(virtualWiki, TEMPLATES_DIR), templateName + TEXT_EXTENSION);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
        for (int fileByte = in.read(); fileByte > -1; fileByte = in.read()) {
            outputStream.write(fileByte);
        }
        return outputStream.toString(Environment.getInstance().getFileEncoding());
    }

    public void addVirtualWiki(String virtualWiki) throws Exception {
        logger.fine("Creating defaults for " + virtualWiki);
        getPathFor(virtualWiki, "").mkdir();
        getPathFor(virtualWiki, VERSION_DIR).mkdir();
        ResourceBundle messages = ResourceBundle.getBundle("ApplicationResources", Locale.getDefault());
        setupSpecialPage(virtualWiki, messages.getString("specialpages.startingpoints"));
        setupSpecialPage(virtualWiki, messages.getString("specialpages.textformattingrules"));
        setupSpecialPage(virtualWiki, messages.getString("specialpages.leftMenu"));
        setupSpecialPage(virtualWiki, messages.getString("specialpages.topArea"));
        setupSpecialPage(virtualWiki, messages.getString("specialpages.bottomArea"));
        setupSpecialPage(virtualWiki, messages.getString("specialpages.stylesheet"));
        setupSpecialPage(virtualWiki, messages.getString("specialpages.adminonlytopics"));
        setupSpecialPage(virtualWiki, messages.getString("specialpages.quickhelp"));
        setupSpecialPage(virtualWiki, messages.getString("specialpages.wikihelp"));
        setupSpecialPage(virtualWiki, messages.getString("specialpages.wikihelpbasicformatting"));
        setupSpecialPage(virtualWiki, messages.getString("specialpages.wikihelpadvancedformatting"));
        setupSpecialPage(virtualWiki, messages.getString("specialpages.wikihelpmakinglinks"));
        loadReadOnlyTopics(virtualWiki);
    }

    public Collection purgeDeletes(String virtualWiki) throws Exception {
        Collection all = new ArrayList();
        File file = getPathFor(virtualWiki, "");
        File[] files = file.listFiles(textFilenameFilter);
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
        file = getPathFor(virtualWiki, TEMPLATES_DIR);
        file.mkdir();
        files = file.listFiles(textFilenameFilter);
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

    public void purgeVersionsOlderThan(String virtualWiki, Date date) throws Exception {
        throw new UnsupportedOperationException("New version purging not available for file persistenceHandler yet");
    }

    public void saveAsTemplate(String virtualWiki, String templateName, String contents) throws Exception {
        File dir = getPathFor(virtualWiki, TEMPLATES_DIR);
        logger.fine("saving template: " + templateName + " to " + dir);
        dir.mkdir();
        File file = new File(dir, templateName + TEXT_EXTENSION);
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), Environment.getInstance().getFileEncoding()));
        writer.print(contents);
        writer.close();
    }

    public List getLockList(String virtualWiki) throws Exception {
        if (virtualWiki == null) virtualWiki = "";
        List all = new ArrayList();
        File path = getPathFor(virtualWiki, "");
        File[] files = path.listFiles(lockFilenameFilter);
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            String fileName = file.getName();
            logger.fine("filename: " + fileName);
            String topicName = fileName.substring(0, fileName.indexOf("."));
            Date lockedAt = new Date(file.lastModified());
            all.add(new TopicLock(virtualWiki, topicName, lockedAt, readLockFileKey(file)));
        }
        return all;
    }

    public void rename(String virtualWiki, String topicName, String newTopicName) throws Exception {
        File topicFile = getPathForTopic(virtualWiki, topicName);
        if (topicFile.renameTo(getPathForTopic(virtualWiki, newTopicName)) == false) throw new IOException("Unable to rename topic " + topicName + " on wiki " + virtualWiki);
        renameHistoryFiles(virtualWiki, topicName, newTopicName);
        makeLockFile(virtualWiki, topicName).delete();
    }

    private void renameHistoryFiles(String virtualWiki, String oldTopicName, String newTopicName) {
        final File versionDir = getPathFor(virtualWiki, VERSION_DIR);
        final String oldFileNamePrefix = oldTopicName + TEXT_EXTENSION;
        final FilenameFilter filter = new PrefixFileFilter(oldFileNamePrefix);
        File versionfiles[] = versionDir.listFiles(filter);
        final String newFileNamePrefix = newTopicName + TEXT_EXTENSION;
        for (int i = 0; i < versionfiles.length; i++) {
            final File oldFileName = versionfiles[i];
            String suffix = oldFileName.getName().substring(oldFileNamePrefix.length());
            File newFileName = new File(versionDir, newFileNamePrefix + suffix);
            oldFileName.renameTo(newFileName);
        }
    }

    private File getPathForTopic(String virtualWiki, String topicName) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(topicName);
        buffer.append(TEXT_EXTENSION);
        return getPathFor(virtualWiki, buffer.toString());
    }

    public void destroy() {
    }

    public void cleanup(String virtualWikiName) {
        try {
            getPathFor(virtualWikiName, "recent.xml").delete();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error removing recent.xml", e);
        }
    }
}
