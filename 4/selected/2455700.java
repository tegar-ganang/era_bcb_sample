package org.pixory.pxfoundation;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class PXFileUtility extends Object {

    private static final Log LOG = LogFactory.getLog(PXFileUtility.class);

    public static final String WIN_ATTRIBUTE_READONLY = "R";

    public static final String WIN_ATTRIBUTE_ARCHIVE = "A";

    public static final String WIN_ATTRIBUTE_SYSTEM = "S";

    public static final String WIN_ATTRIBUTE_HIDDEN = "H";

    public static final String WIN_SET_ATTRIBUTE = "+";

    public static final String WIN_UNSET_ATTRIBUT = "-";

    private static final String WIN_EXECUTABLE_FOR_ATTRIBUTE = "attrib";

    private static final String CURRENT_DIR_KEY = "user.dir";

    private static final String HOME_DIRECTORY_KEY = "user.home";

    private static final String TEMP_DIR_KEY = "java.io.tmpdir";

    private static final char FILE_NAME_EXTENSION_SEPARATOR = '.';

    /**
	 * Set of WIN_ATTRIBUTE_* constants Strings
	 */
    private static Set _windozeFileAttributes;

    private static String _applicationLibraryDirectoryPath;

    private static File _tempDirectory;

    private PXFileUtility() {
    }

    public static String getCurrentDirectoryPath() {
        return System.getProperty(CURRENT_DIR_KEY);
    }

    public static File getCurrentDirectory() {
        File getCurrentDirectory = null;
        String aCurrentPath = getCurrentDirectoryPath();
        if (aCurrentPath != null) {
            getCurrentDirectory = new File(aCurrentPath);
        }
        return getCurrentDirectory;
    }

    public static String getHomeDirectoryPath() {
        return System.getProperty(HOME_DIRECTORY_KEY);
    }

    /**
	 * OS X only-- ~/Library/ <application-name>
	 */
    public static String getApplicationLibraryDirectoryPath() {
        if (SystemUtils.IS_OS_MAC_OSX) {
            if (_applicationLibraryDirectoryPath == null) {
                String aHomeDirPath = getHomeDirectoryPath();
                if (aHomeDirPath != null) {
                    String aLibraryDirPath = PXPathUtility.pathByAppendingPathComponent(aHomeDirPath, "Library");
                    _applicationLibraryDirectoryPath = PXPathUtility.pathByAppendingPathComponent(aLibraryDirPath, PXApplicationName.NAME);
                }
            }
        }
        return _applicationLibraryDirectoryPath;
    }

    public static File getApplicationLibraryDirectory() {
        File getApplicationLibraryDirectory = null;
        String aPath = getApplicationLibraryDirectoryPath();
        if (aPath != null) {
            getApplicationLibraryDirectory = new File(aPath);
        }
        return getApplicationLibraryDirectory;
    }

    public static File getTempDirectory() {
        if (_tempDirectory == null) {
            String aTempPath = System.getProperty(TEMP_DIR_KEY);
            if (aTempPath != null) {
                _tempDirectory = new File(aTempPath);
            } else {
                LOG.warn("Could not find property for key: " + TEMP_DIR_KEY);
            }
        }
        return _tempDirectory;
    }

    public static byte[] readContents(File file) throws IOException {
        byte[] readContents = null;
        if (file != null) {
            if (file.canRead()) {
                FileInputStream aFileStream = new FileInputStream(file);
                readContents = PXStreamUtility.readFully(aFileStream);
            } else {
                throw new IOException("Can't read file '" + file + "'");
            }
        }
        return readContents;
    }

    /** 
	 * uses the default charset 
	 */
    public static String readContentsAsString(File file) throws IOException {
        String readContentsAsString = null;
        byte[] someBytes = readContents(file);
        if (someBytes != null) {
            readContentsAsString = new String(someBytes);
        }
        return readContentsAsString;
    }

    /**
	 * never clobbers, throws exception if file exists 
	 */
    public static void writeContents(File file, byte[] contents) throws IOException {
        if ((file != null) && (contents != null)) {
            if (file.createNewFile()) {
                FileOutputStream aFileStream = new FileOutputStream(file);
                BufferedOutputStream aBufferedStream = new BufferedOutputStream(aFileStream);
                for (int i = 0; i < contents.length; i++) {
                    aBufferedStream.write(contents[i]);
                }
                aBufferedStream.flush();
                aBufferedStream.close();
            } else {
                throw new IOException("Can't writeContents '" + file + "' a file already exists at that path");
            }
        }
    }

    public static void writeContentsFromString(File file, String string) throws IOException {
        if ((file != null) && (string != null)) {
            writeContents(file, string.getBytes());
        }
    }

    /**
	 * @return returns true if target is a directory, exists, and has no children
	 *         at all
	 */
    public static boolean isDirectoryEmpty(File target) {
        boolean isDirectoryEmpty = false;
        if (target != null) {
            if (target.isDirectory()) {
                String[] someFileNames = target.list();
                if ((someFileNames == null) || (someFileNames.length == 0)) {
                    isDirectoryEmpty = true;
                }
            }
        }
        return isDirectoryEmpty;
    }

    public static void copyFile(File sourceFile, File destinationFile) throws IOException {
        if ((sourceFile != null) && (destinationFile != null)) {
            byte[] someContentBytes = readContents(sourceFile);
            writeContents(destinationFile, someContentBytes);
        }
    }

    /** 
	 * a 'smart' move that can work on individual files or directories
	 * 
	 * files are never clobbered
	 * 
	 * @return true if the operation suceeded entirely, false otherwise
	 */
    public static boolean move(File source_, File destination_, FileFilter filter_) throws IOException {
        boolean move = false;
        Validate.notNull(source_);
        Validate.notNull(destination_);
        move = copy(source_, destination_, filter_);
        LOG.debug("move: " + move);
        if (move) {
            move = delete(source_);
        }
        return move;
    }

    /** 
	 * a 'smart' copy that can work on individual files or directories
	 * 
	 * files are never clobbered
	 * 
	 * @return true if the operation suceeded entirely, false otherwise
	 */
    public static boolean copy(File source_, File destination_, FileFilter filter_) throws IOException {
        boolean copy = false;
        Validate.notNull(source_);
        Validate.notNull(destination_);
        Validate.isTrue(source_.exists());
        if (source_.isFile()) {
            if (!destination_.exists()) {
                copyFile(source_, destination_);
                copy = true;
            }
        } else if (source_.isDirectory()) {
            if (!destination_.exists()) {
                copy = destination_.mkdirs();
                if (copy) {
                    File[] contents = source_.listFiles(filter_);
                    if ((contents != null) && (contents.length > 0)) {
                        for (int i = 0; i < contents.length; i++) {
                            File newSourceFile = contents[i];
                            String sourceName = newSourceFile.getName();
                            File newDestinationFile = new File(destination_, sourceName);
                            copy = copy(newSourceFile, newDestinationFile, filter_);
                            if (!copy) {
                                break;
                            }
                        }
                    }
                }
            }
        }
        return copy;
    }

    public static void copyFilesFromDirectory(File fromDirectory, File toDirectory, FileFilter filter) throws IllegalArgumentException, IOException {
        if ((fromDirectory != null) && (toDirectory != null)) {
            if (fromDirectory.isDirectory()) {
                File[] someSourceFiles = fromDirectory.listFiles(filter);
                if ((someSourceFiles != null) && (someSourceFiles.length > 0)) {
                    if (!toDirectory.exists()) {
                        toDirectory.mkdir();
                    }
                    for (int i = 0; i < someSourceFiles.length; i++) {
                        File aFile = someSourceFiles[i];
                        if (aFile.isFile()) {
                            File aToFile = new File(toDirectory, aFile.getName());
                            copyFile(aFile, aToFile);
                        }
                    }
                }
            } else {
                throw new IllegalArgumentException("'" + fromDirectory + "' is not a directory");
            }
        }
    }

    /**
	 * recurses from the target directory
	 * 
	 * @return returns the *first* matching file
	 */
    public static File findFileNamed(File targetDirectory, String filename) {
        File findFileNamed = null;
        if ((targetDirectory != null) && (filename != null)) {
            if (targetDirectory.isDirectory()) {
                FilenameFilter aFilenameFilter = new PXEqualsFilenameFilter(filename);
                File[] someFiles = targetDirectory.listFiles(aFilenameFilter);
                if ((someFiles != null) && (someFiles.length > 0)) {
                    findFileNamed = someFiles[0];
                } else {
                    File[] someSubdirectories = getDirectories(targetDirectory);
                    if (someSubdirectories != null) {
                        for (int i = 0; i < someSubdirectories.length; i++) {
                            findFileNamed = findFileNamed(someSubdirectories[i], filename);
                            if (findFileNamed != null) {
                                break;
                            }
                        }
                    }
                }
            }
        }
        return findFileNamed;
    }

    public static String getBaseName(File file) {
        String getBaseName = null;
        if (file != null) {
            String aFileName = file.getName();
            if (aFileName != null) {
                getBaseName = aFileName;
                int aSeparatorIndex = aFileName.lastIndexOf(FILE_NAME_EXTENSION_SEPARATOR);
                if (aSeparatorIndex > 0) {
                    getBaseName = getBaseName.substring(0, aSeparatorIndex);
                }
            }
        }
        return getBaseName;
    }

    public static String getExtension(File file) {
        String getExtension = null;
        if (file != null) {
            String aFileName = file.getName();
            if (aFileName != null) {
                int aSeparatorIndex = aFileName.lastIndexOf(FILE_NAME_EXTENSION_SEPARATOR);
                if ((aSeparatorIndex >= 0) && (aSeparatorIndex < (aFileName.length() - 1))) {
                    getExtension = aFileName.substring(aSeparatorIndex + 1, aFileName.length());
                }
            }
        }
        return getExtension;
    }

    /**
	 * @return a new File formed by adding the provided suffix to the target File
	 *         baseName
	 */
    public static File addSuffix(File target, String suffix) {
        File addSuffix = null;
        if ((target != null) && (suffix != null)) {
            String aTargetPath = target.getPath();
            String anOldBase = PXPathUtility.filePathBaseName(aTargetPath);
            String anExtension = PXPathUtility.filePathExtension(aTargetPath);
            String aNewBase = anOldBase + suffix;
            String aNewComponent = aNewBase;
            if (anExtension != null) {
                aNewComponent += anExtension;
            }
            String aNewPath = PXPathUtility.pathByReplacingLastPathComponent(aTargetPath, aNewComponent);
            addSuffix = new File(aNewPath);
        }
        return addSuffix;
    }

    /**
	 * @return path of child minus path of parent. If child arg is not really a
	 *         child of parent then returns null. Returned path is always
	 *         'relative', meaning that leading path separator is always stripped
	 */
    public static String getPathDifference(File parent, File child) {
        String getPathDifference = null;
        if ((parent != null) && (child != null)) {
            try {
                String aParentPath = parent.getCanonicalPath();
                String aChildPath = child.getCanonicalPath();
                if (aChildPath.startsWith(aParentPath)) {
                    getPathDifference = PXStringUtility.removePrefix(aChildPath, aParentPath);
                    if ((getPathDifference != null) && (getPathDifference.startsWith(File.separator))) {
                        getPathDifference = PXStringUtility.removePrefix(getPathDifference, File.separator);
                    }
                }
            } catch (Exception anException) {
                LOG.warn(null, anException);
            }
        }
        return getPathDifference;
    }

    /**
	 * @return the cumulative size of all of the regular files in the paramter
	 *         array
	 */
    public static long getSize(File[] someFiles) {
        long getSize = 0;
        if ((someFiles != null) && (someFiles.length > 0)) {
            for (int i = 0; i < someFiles.length; i++) {
                if (someFiles[i].isFile()) {
                    getSize += someFiles[i].length();
                }
            }
        }
        return getSize;
    }

    /**
	 * recurses, deleting all contained files
	 */
    public static boolean delete(File file) {
        boolean delete = false;
        if ((file != null) && (file.exists())) {
            delete = true;
            if (file.isDirectory()) {
                File[] someFiles = file.listFiles();
                if ((someFiles != null) && (someFiles.length > 0)) {
                    for (int i = 0; i < someFiles.length; i++) {
                        if (!(delete = delete(someFiles[i]))) {
                            break;
                        }
                    }
                }
            }
            if (delete) {
                delete = file.delete();
            }
        }
        return delete;
    }

    /**
	 * does not recurse
	 * 
	 * @return true if and only if every file matching the regex was successfully
	 *         deleted
	 */
    public static boolean deleteMatching(File dir, String regex) {
        boolean delete = false;
        File[] someFiles = filesMatching(dir, regex);
        if ((someFiles != null) && (someFiles.length > 0)) {
            delete = true;
            for (int i = 0; i < someFiles.length; i++) {
                if (!someFiles[i].delete()) {
                    delete = false;
                }
            }
        }
        return delete;
    }

    public static File[] filesMatching(File dir, String regex) {
        File[] filesMatching = null;
        if ((dir != null) && (dir.isDirectory()) && (regex != null)) {
            PXRegexFilenameFilter aFilter = new PXRegexFilenameFilter(regex);
            filesMatching = dir.listFiles((FilenameFilter) aFilter);
        }
        return filesMatching;
    }

    /**
	 * @return returns matching file if there is one and only one match prints a
	 *         warning if there is more than one match. The regex matches against
	 *         the filename.
	 */
    public static File fileMatching(File dir, String regex) {
        File fileMatching = null;
        File[] someFiles = filesMatching(dir, regex);
        if (someFiles != null) {
            if (someFiles.length == 1) {
                fileMatching = someFiles[0];
            } else if (someFiles.length > 1) {
                LOG.warn("more than one file matching regex: " + regex + " in directory: " + dir);
            }
        }
        return fileMatching;
    }

    /**
	 * @return returns all the immediate subdirectories of the targetDirectory
	 */
    public static File[] getDirectories(File targetDirectory) {
        File[] getDirectories = null;
        if ((targetDirectory != null) && (targetDirectory.isDirectory())) {
            FileFilter aDirectoryFilter = PXDirectoryFileFilter.getInstance();
            getDirectories = targetDirectory.listFiles(aDirectoryFilter);
        }
        return getDirectories;
    }

    /** a singleton that filters for directories */
    private static class PXDirectoryFileFilter implements FileFilter {

        private static final PXDirectoryFileFilter _instance = new PXDirectoryFileFilter();

        public static PXDirectoryFileFilter getInstance() {
            return _instance;
        }

        private PXDirectoryFileFilter() {
        }

        /**
		 * @param pathname
		 *           The abstract pathname to be tested
		 * @return <code>true</code> if pathname is a directory
		 *  
		 */
        public boolean accept(File file) {
            boolean accept = false;
            if (file.isDirectory()) {
                accept = true;
            }
            return accept;
        }
    }

    /**
	 * filters for an exact filename match; ostensibly this is faster than using
	 * the RegexFilenameFilter
	 */
    public static class PXEqualsFilenameFilter implements FilenameFilter {

        private String _filename;

        public PXEqualsFilenameFilter(String filename) {
            if (filename != null) {
                _filename = filename;
            } else {
                String aMessage = "PXEqualsFilenameFilter does not accept null args";
                throw new IllegalArgumentException(aMessage);
            }
        }

        public boolean accept(File directory, String filename) {
            boolean accept = false;
            if ((filename != null) && (_filename.equals(filename))) {
                accept = true;
            }
            return accept;
        }
    }

    /**
	 * @return true if the target is a parent path of the source
	 */
    public static boolean isParent(File source_, File target_) {
        boolean isParent = false;
        if (source_ != null) {
            if (target_ != null) {
                try {
                    String sourcePath = source_.getAbsolutePath();
                    String targetPath = target_.getAbsolutePath();
                    if (sourcePath.startsWith(targetPath)) {
                        isParent = true;
                    }
                } catch (Exception e) {
                    LOG.warn(null, e);
                    isParent = false;
                }
            }
        } else {
            if (target_ == null) {
                isParent = true;
            }
        }
        return isParent;
    }

    /**
	 * @param file
	 * @return true if the parentDirectory existed or was created, false if it
	 *         still doesn't exist after this method invocation
	 */
    public static boolean ensureParentDirectory(File file) {
        boolean ensureParentDirectory = false;
        if (file != null) {
            File aParentDirectory = file.getParentFile();
            if (aParentDirectory != null) {
                if (aParentDirectory.isDirectory()) {
                    ensureParentDirectory = true;
                } else {
                    ensureParentDirectory = aParentDirectory.mkdirs();
                }
            }
        }
        return ensureParentDirectory;
    }

    /**
	 * @return Set of possible Windoze file attributes
	 */
    private static Set getWinFileAttributes() {
        if (SystemUtils.IS_OS_WINDOWS) {
            if (_windozeFileAttributes == null) {
                _windozeFileAttributes = new HashSet();
                _windozeFileAttributes.add(WIN_ATTRIBUTE_ARCHIVE);
                _windozeFileAttributes.add(WIN_ATTRIBUTE_HIDDEN);
                _windozeFileAttributes.add(WIN_ATTRIBUTE_READONLY);
                _windozeFileAttributes.add(WIN_ATTRIBUTE_SYSTEM);
            }
        }
        return _windozeFileAttributes;
    }

    /**
	 * this applies on Win32 only; on other platforms, does nothing and returns
	 * false
	 */
    public static boolean modifyFileAttribute(File file, String attribute, String modification) {
        boolean modifyFileAttribute = false;
        if (SystemUtils.IS_OS_WINDOWS && (file != null) && (file.exists()) && (attribute != null) && (modification != null)) {
            if (!(modification.equals(WIN_SET_ATTRIBUTE) || modification.equals(WIN_UNSET_ATTRIBUT))) {
                String aMessage = "Invalid modification argument";
                throw new IllegalArgumentException(aMessage);
            }
            Set somePossibleAttributes = getWinFileAttributes();
            if (!(somePossibleAttributes != null) && (somePossibleAttributes.contains(attribute))) {
                String aMessage = "Invalid attribute argument";
                throw new IllegalArgumentException(aMessage);
            }
            String[] aCommandArray = new String[3];
            aCommandArray[0] = WIN_EXECUTABLE_FOR_ATTRIBUTE;
            aCommandArray[1] = modification + attribute;
            aCommandArray[2] = file.getAbsolutePath();
            try {
                Process aProcess = Runtime.getRuntime().exec(aCommandArray);
                aProcess.waitFor();
                if (aProcess.exitValue() == 0) {
                    modifyFileAttribute = true;
                } else {
                    InputStream anErrorStream = aProcess.getErrorStream();
                    if (anErrorStream != null) {
                        anErrorStream = new BufferedInputStream(anErrorStream);
                        byte[] someBytes = PXStreamUtility.readFully(anErrorStream);
                        if (someBytes != null) {
                            String anErrorString = new String(someBytes);
                            LOG.warn(anErrorString);
                        }
                    }
                }
            } catch (Exception e) {
                LOG.warn(null, e);
            }
        }
        return modifyFileAttribute;
    }

    /**
	 * @return true if we believe the file is hidden (whatever that means on
	 *         platform specific basis) after the call returns
	 */
    public static boolean ensureHidden(File file) {
        boolean ensureHidden = false;
        if (file != null) {
            if (file.isHidden()) {
                ensureHidden = true;
            } else if (SystemUtils.IS_OS_WINDOWS) {
                ensureHidden = PXFileUtility.modifyFileAttribute(file, WIN_ATTRIBUTE_HIDDEN, WIN_SET_ATTRIBUTE);
            }
        }
        return ensureHidden;
    }
}
