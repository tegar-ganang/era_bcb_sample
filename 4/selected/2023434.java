package com.dynamide.util;

import java.io.*;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

public class FileTools implements IFileDiverListener {

    private static final boolean debug = false;

    /** A class that contains information about a file and its contents so that the class
     *  can be read fully, while preserving the File handle to pass back to the caller.
     */
    public static class FileInfo {

        /** This is for empty files or file not found.
         *  @param binary means the file was <i>supposed</i> to be binary.
         */
        public FileInfo(boolean binary, boolean notFound, File file) {
            m_file = file;
            m_isBinary = binary;
            m_empty = true;
            m_fileNotFound = notFound;
        }

        public FileInfo(File file, String string) {
            m_file = file;
            m_asString = string;
            m_bytes = new byte[0];
            m_isBinary = false;
        }

        public FileInfo(File file, byte[] bytes) {
            m_file = file;
            m_bytes = bytes;
            m_isBinary = true;
        }

        public String toString() {
            return "FileInfo:" + m_notModified + ";" + m_fileNotFound + ";" + m_isBinary + ";" + m_empty + ";" + m_file;
        }

        private boolean m_notModified = false;

        public boolean getNotModified() {
            return m_notModified;
        }

        public void setNotModified(boolean new_value) {
            m_notModified = new_value;
        }

        private boolean m_fileNotFound = false;

        public boolean getFileNotFound() {
            return m_fileNotFound;
        }

        public void setFileNotFound(boolean new_value) {
            m_fileNotFound = new_value;
        }

        private boolean m_isBinary = false;

        public boolean getIsBinary() {
            return m_isBinary;
        }

        protected void setIsBinary(boolean new_value) {
            m_isBinary = new_value;
        }

        private File m_file = null;

        public File getFile() {
            return m_file;
        }

        protected void setFile(File new_value) {
            m_file = new_value;
        }

        private String m_asString = "";

        public String getAsString() {
            return m_asString;
        }

        protected void setAsString(String new_value) {
            m_asString = new_value;
        }

        private byte[] m_bytes = new byte[0];

        public byte[] getBytes() {
            return m_bytes;
        }

        protected void setBytes(byte[] new_value) {
            m_bytes = new_value;
        }

        private boolean m_empty = false;

        public boolean isEmpty() {
            return m_empty;
        }

        protected void setEmpty(boolean new_value) {
            m_empty = new_value;
        }
    }

    public static void directoryDiver(String diveID, String startingDir, IFileDiverListener listener) {
        directoryDiver(diveID, startingDir, listener, File.separator);
    }

    public static void directoryDiver(String diveID, String startingDir, IFileDiverListener listener, String separator) {
        if (debug) System.out.println("Starting directoryDiver");
        File file = new File(Tools.fixFilename(startingDir));
        directoryDiver(diveID, file, "", listener, separator);
    }

    public static void directoryDiver(String diveID, File directory, String relativePath, IFileDiverListener listener, String separator) {
        if (debug) System.out.println("in directoryDiver");
        if (listener == null) {
            Log.error(FileTools.class, "null listener");
            return;
        }
        String[] files = directory.list();
        if (files == null || files.length == 0) {
            if (debug) Log.error(FileTools.class, "null or zero length name");
            return;
        }
        String newname;
        for (int i = 0; i < files.length; i++) {
            if (files[i].equals(".") || files[i].equals("..")) {
                continue;
            }
            File file = new File(directory, files[i]);
            if (file != null && !file.isDirectory()) {
                newname = relativePath.length() > 0 ? relativePath + separator + file.getName() : file.getName();
                listener.onFile(diveID, directory, file, files[i], newname);
            }
        }
        boolean diveThisDir;
        for (int i = 0; i < files.length; i++) {
            if (files[i].equals(".") || files[i].equals("..")) {
                continue;
            }
            File file = new File(directory, files[i]);
            if (file != null && file.isDirectory()) {
                newname = relativePath.length() > 0 ? relativePath + separator + file.getName() : file.getName();
                diveThisDir = listener.onDirectory(diveID, file, files[i], newname);
                if (diveThisDir) {
                    directoryDiver(diveID, file, newname, listener, separator);
                }
            }
        }
        return;
    }

    /** filename is the short name of the file, without directory info, relativePath is the relative
     * path, including intermediate directories, starting from the point where the dive was initiated.
     *  So if you called directoryDiver("/dynamide", this), you could expect to see a file
     *  whose full path was /dynamide/src/conf/log.conf be reported as "src/conf/log.conf".
     */
    public void onFile(String diveID, File directory, File file, String filename, String relativePath) {
        System.out.println("onFile: " + filename);
    }

    /** @see #onFile for a description of relativePath.
     */
    public boolean onDirectory(String diveID, File directory, String dirname, String relativePath) {
        System.out.println("onDirectory: " + dirname);
        return true;
    }

    public static final int LT_FILES = 0;

    public static final int LT_DIRS = 1;

    public static final int LT_ALL = 2;

    public static final int LN_FULL = 0;

    public static final int LN_FILENAME = 1;

    public static final int LN_FILENAME_NO_EXT = 2;

    /** @param type should be one of LT_FILES, LT_DIRS or LT_ALL.
      * @param nameStyle should be one of LN_FULL, LN_FILENAME or LN_FILENAME_NO_EXT. */
    public static Map list(String dirname, int type, int nameStyle) throws Exception {
        Hashtable hashtable = new Hashtable();
        File directory = new File(dirname);
        if (directory.exists()) {
            String[] files = directory.list();
            if (files != null && files.length > 0) {
                for (int i = 0; i < files.length; i++) {
                    boolean doit = false;
                    if (files[i].equals(".") || files[i].equals("..")) {
                        continue;
                    }
                    File file = new File(directory, files[i]);
                    if (file != null) {
                        if (file.isDirectory()) {
                            if (type == LT_ALL || type == LT_DIRS) {
                                doit = true;
                            }
                        } else {
                            doit = (type != LT_DIRS);
                        }
                        if (doit) {
                            String nm = "UNDEFINED";
                            switch(nameStyle) {
                                case LN_FULL:
                                    nm = file.getCanonicalPath();
                                    break;
                                case LN_FILENAME:
                                    nm = file.getCanonicalPath();
                                    break;
                                case LN_FILENAME_NO_EXT:
                                    nm = file.getName();
                                    int idot = nm.lastIndexOf(".");
                                    if (idot > -1) {
                                        nm = nm.substring(0, idot);
                                    }
                                    break;
                            }
                            hashtable.put(nm, file);
                        }
                    }
                }
            }
        }
        return hashtable;
    }

    static boolean m_fileSystemIsDOS = "\\".equals(File.separator);

    public static boolean fileSystemIsDOS() {
        return m_fileSystemIsDOS;
    }

    public static String fixFilename(String filename) {
        if (m_fileSystemIsDOS) {
            return filename.replace('/', '\\');
        }
        return filename.replace('\\', '/');
    }

    public static String join(String dir, String file) {
        if (dir.length() == 0) {
            return file;
        }
        dir = Tools.fixFilename(dir);
        file = Tools.fixFilename(file);
        if (!dir.endsWith(File.separator)) {
            dir += File.separator;
        }
        if (file.startsWith(File.separator)) {
            file = file.substring(1);
        }
        return dir + file;
    }

    public static String joinURI(String dir, String file) {
        if (dir.length() == 0) {
            return file;
        }
        dir = toURI(dir);
        file = toURI(file);
        if (!dir.endsWith("/")) {
            dir += "/";
        }
        if (file.startsWith("/")) {
            file = file.substring(1);
        }
        return dir + file;
    }

    public static String toURI(String filename) {
        return filename.replace('\\', '/');
    }

    public static String joinExt(String filename, String ext) {
        return joinExt(filename, ext, false);
    }

    public static String joinURIExt(String filename, String ext) {
        return joinExt(filename, ext, true);
    }

    public static String joinURIExt(String dir, String filename, String ext) {
        return joinExt(joinURI(dir, filename), ext, true);
    }

    public static String joinExt(String filename, String ext, boolean isURI) {
        String filenameU = filename.toUpperCase();
        if (!ext.startsWith(".")) {
            ext = "." + ext;
        }
        String extU = ext.toUpperCase();
        if (filenameU.endsWith(extU)) {
            return isURI ? toURI(filenameU) : fixFilename(filename);
        }
        if (extU.equals(".HTML") || extU.equals(".HTM")) {
            if (filenameU.endsWith(".HTM") || filenameU.endsWith(".HTML")) {
                return isURI ? toURI(filename) : fixFilename(filename);
            }
        }
        filename = filename + ext;
        return isURI ? toURI(filename) : fixFilename(filename);
    }

    public static File createDirectory(String fullPath) {
        return createDirectory(fullPath, "");
    }

    /** Creates a directory, returned as a File object -- call File.getCanonicalPath() to see the full path as a String().
     *  @param relativePath can be empty.
     *  @param baseDir can be be a full path or partial path.
     *  @return a File object pointing at the new directory if successful, else null.
     */
    public static File createDirectory(String baseDir, String relativePath) {
        return createDirectory(baseDir, relativePath, false);
    }

    public static File createDirectory(String baseDir, String relativePath, boolean nullReturnIfDirExists) {
        File f = new File(baseDir, relativePath);
        try {
            if (f.exists()) {
                if (nullReturnIfDirExists) {
                    return null;
                }
                return f;
            } else {
                f.mkdirs();
                if (f.exists()) {
                    return f;
                } else {
                    Log.error(FileTools.class, "FileTools.createDirectory could not mkdirs for \"" + baseDir + "\",\"" + relativePath + "\" path: \"" + f.getCanonicalPath() + "\" exists: " + f.exists());
                }
            }
        } catch (Exception e) {
            Log.error(FileTools.class, "FileTools.createDirectory failed", e);
        }
        return null;
    }

    public static File createTempSubdirectory(String leader) throws ConcurrentModificationException {
        String tmpdir = System.getProperty("java.io.tmpdir");
        String subdir = leader + '-' + Tools.now();
        File fSubdir = FileTools.createDirectory(tmpdir, subdir, true);
        if (fSubdir == null) {
            throw new ConcurrentModificationException("Subdirectory (" + subdir + ") of tempdir (" + tmpdir + ") exists.  Possible concurrent modification error.");
        }
        return fSubdir;
    }

    public static boolean delete(String filename) {
        return delete(new File(filename));
    }

    public static boolean delete(String dirname, String filename) {
        return delete(new File(dirname, filename));
    }

    private static boolean delete(File file) {
        if (file != null) {
            file.delete();
            return true;
        } else {
            return false;
        }
    }

    /** @param sanity is a safety check, or you can pass "". */
    public static void cleanOneDir(String dir, String name, String sanity) {
        File directory = new File(dir, name);
        if (directory == null) {
            return;
        }
        String dirname = join(dir, name);
        String[] files = directory.list();
        if (files == null || files.length == 0) {
            return;
        }
        for (int i = 0; i < files.length; i++) {
            if (files[i].startsWith(sanity)) {
                File file = (new File(dirname, files[i]));
                if (file != null) {
                    file.delete();
                }
            }
        }
    }

    public static boolean isWebSafeFileName(String path) {
        if (path.indexOf(';') > -1 || path.indexOf("..") > -1 || path.indexOf(' ') > -1 || path.indexOf('') > 0 || path.indexOf('\n') > -1 || path.indexOf('\r') > -1 || path.indexOf('') > -1 || path.indexOf('\t') > -1) {
            return false;
        }
        return true;
    }

    /** Copy a binary source file to a destination file.  Filenames are platform dependent.
      * Returns true for copying a zero byte file.
      * Returns true if the file copy was succesful.
      */
    public static boolean copyFile(String sourceFileName, String destFileName) {
        if (sourceFileName == null || destFileName == null) return false;
        if (sourceFileName.equals(destFileName)) return false;
        try {
            java.io.FileInputStream in = new java.io.FileInputStream(sourceFileName);
            java.io.FileOutputStream out = new java.io.FileOutputStream(destFileName);
            try {
                byte[] buf = new byte[31000];
                int read = in.read(buf);
                while (read > -1) {
                    out.write(buf, 0, read);
                    read = in.read(buf);
                }
            } finally {
                in.close();
                out.close();
            }
        } catch (Exception e) {
            System.out.println(e.toString());
            return false;
        }
        return true;
    }

    /** Copy srcDir to destDir, overwriting all files. No files or dirs in destDir are removed if
     * not present in srcDir.  destDir may exist.  "CVS" directories are skipped.
     */
    public static List copyDir(String srcDir, String destDir) throws IOException {
        srcDir = fixFilename(srcDir);
        destDir = fixFilename(destDir);
        List outputList = new ArrayList();
        return copyDir(new File(srcDir), destDir, File.separator, outputList);
    }

    private static List copyDir(File directory, String relativePath, String separator, List outputList) throws IOException {
        Log.debug(FileTools.class, "in copyDir: " + directory + " path: " + relativePath + " sep: " + separator);
        String[] files = directory.list();
        if (files == null || files.length == 0) {
            Log.error(FileTools.class, "null or zero length name");
            return outputList;
        }
        Log.debug(FileTools.class, "mkdirs: " + relativePath);
        File dirFile = new File(relativePath);
        dirFile.mkdirs();
        outputList.add(dirFile.getCanonicalPath() + separator);
        String newname;
        for (int i = 0; i < files.length; i++) {
            if (files[i].equals(".") || files[i].equals("..") || files[i].equals("CVS")) {
                continue;
            }
            File file = new File(directory, files[i]);
            if (file != null && !file.isDirectory()) {
                newname = fixFilename(relativePath + separator + file.getName());
                Log.debug(FileTools.class, "copyFile from: \r\n\t" + file + " to: \r\n\t" + newname);
                outputList.add(newname);
                copyFile(file.getCanonicalPath(), newname);
            }
        }
        for (int i = 0; i < files.length; i++) {
            if (files[i].equals(".") || files[i].equals("..") || files[i].equals("CVS")) {
                continue;
            }
            File file = new File(directory, files[i]);
            if (file != null && file.isDirectory()) {
                newname = fixFilename(relativePath + separator + file.getName());
                copyDir(file, newname, separator, outputList);
            }
        }
        return outputList;
    }

    public static File openFile(String dir, String relPath) {
        return openFile(dir, relPath, false);
    }

    public static File openFile(String dir, String relPath, boolean forceParentDirectories) {
        File theFile;
        if (dir.length() == 0) {
            theFile = new File(relPath);
            if (!theFile.exists()) {
                try {
                    theFile = new File(resolve(relPath));
                } catch (Exception e) {
                    if (!forceParentDirectories) Log.warn(FileTools.class, "FileTools.openFile could not open relPath: " + relPath);
                }
            }
        } else {
            theFile = new File(dir, relPath);
            if (!theFile.exists()) {
                try {
                    theFile = new File(resolve(dir), resolve(relPath));
                } catch (Exception e) {
                    if (!forceParentDirectories) Log.warn(FileTools.class, "FileTools.openFile could not open dir: '" + dir + "' and relPath: '" + relPath + '\'');
                }
            }
        }
        if (forceParentDirectories) {
            String parent = theFile.getParent();
            if (parent != null) {
                (new File(parent)).mkdirs();
            }
        }
        if (theFile != null) {
            try {
            } catch (Exception e) {
                System.out.println("FileTools.openFile opened: " + relPath + " in " + dir + " " + theFile.toString());
            }
        } else {
        }
        return theFile;
    }

    public static String resolve(String path) throws IOException {
        boolean trimend = false, trimbegin = false;
        if (path.startsWith("\"")) {
            trimbegin = true;
        }
        if (path.endsWith("\"")) {
            trimend = true;
        }
        if (path.startsWith("\'")) {
            trimbegin = true;
        }
        if (path.endsWith("\'")) {
            trimend = true;
        }
        String result = path;
        if (trimend && trimbegin) {
            result = path.substring(1, path.length() - 1);
        } else {
            if (trimend) {
                result = path.substring(0, path.length() - 1);
            }
            if (trimbegin) {
                result = path.substring(1, path.length());
            }
        }
        return result;
    }

    public static String readFile(String fullPath) {
        return readFile("", fullPath);
    }

    public static String readFile(String dir, String relPath) {
        FileInfo info = readFileInfo(dir, relPath);
        if (info.isEmpty()) {
            return null;
        } else {
            return info.getAsString();
        }
    }

    public static FileInfo readFileInfo(String dir, String relPath) {
        return readFileInfo(dir, relPath, 0);
    }

    /** @param lastModified Pass zero to indicate that you want the file read regardless of
     *  the last modified timestamp, otherwise this method compares the value with
     *  the File's lastModified() result, and returns an empty FileInfo with getNotModified() == true,
     *  or a filled out FileInfo if the file has been modified since the timestamp.
     */
    public static FileInfo readFileInfo(String dir, String relPath, long lastModified) {
        try {
            if (relPath.length() == 0) {
                relPath = dir;
                dir = "";
            }
            File theFile = openFile(dir, relPath);
            if (theFile == null) {
                return new FileInfo(false, true, null);
            }
            if (!theFile.isDirectory()) {
                if (lastModified > 0) {
                    if ((theFile.lastModified() - lastModified) < 0) {
                        FileInfo result = new FileInfo(false, false, theFile);
                        result.setNotModified(true);
                        return result;
                    }
                }
                FileInputStream fis = new FileInputStream(theFile);
                byte[] theData = new byte[(int) theFile.length()];
                int howmany = fis.read(theData);
                if (howmany != theData.length) {
                    Log.error(FileTools.class, "#################################### ERROR: #################################" + "\r\n############# Couldn't read all of stream!  filesize: " + theData.length + "  read: " + howmany);
                }
                fis.close();
                return new FileInfo(theFile, new String(theData));
            }
            return new FileInfo(false, true, theFile);
        } catch (Exception e) {
            return new FileInfo(false, true, null);
        }
    }

    public static byte[] readBinaryFile(String dir, String relPath) {
        return readBinaryFileInfo(dir, relPath).getBytes();
    }

    public static FileInfo readBinaryFileInfo(String dir, String relPath) {
        return readBinaryFileInfo(dir, relPath, 0);
    }

    public static FileInfo readBinaryFileInfo(String dir, String relPath, long lastModified) {
        try {
            File theFile = openFile(dir, relPath);
            if (theFile == null) {
                return new FileInfo(true, true, null);
            }
            if (!theFile.isDirectory()) {
                if (lastModified > 0) {
                    if (theFile.lastModified() - lastModified > 0) {
                        FileInfo result = new FileInfo(true, false, theFile);
                        result.setNotModified(true);
                        return result;
                    }
                }
                FileInputStream fis = new FileInputStream(theFile);
                byte[] theData = new byte[(int) theFile.length()];
                int howmany = fis.read(theData);
                if (howmany != theData.length) {
                    Log.error(FileTools.class, "#################################### ERROR: #################################" + "\r\n############# Couldn't read all of BINARY stream!  filesize: " + theData.length + "  read: " + howmany);
                }
                fis.close();
                return new FileInfo(theFile, theData);
            }
            return new FileInfo(true, true, theFile);
        } catch (Exception e) {
            return new FileInfo(false, true, null);
        }
    }

    public static File saveToTempFile(String directory, String content) {
        int i = 0;
        while (fileExists(join(directory, "" + i + ".tmp"))) {
            i++;
        }
        return saveFile(directory, "" + i + ".tmp", content);
    }

    /** Finds a new name for the file named in the directory named, and creates that file, without deleting the original file.
     * 
     * @param directory  The canonical path to the file, without filename or extension
     * @param shortFilename the basename of the file without extension, e.g. for /tmp/MyFile.txt shortFilename is "MyFile"
     * @param extension Just the extension, without the dot, eg. for /tmp/MyFile.txt extension is "txt"  
     * @return the handle to the newly copied file.
     */
    public static String copyToTempFile(String directory, String shortFilename, String extension) throws Exception {
        int i = 0;
        while (fileExists(join(directory, shortFilename + i + '.' + extension))) {
            i++;
        }
        String src = directory + File.separator + shortFilename + '.' + extension;
        String dest = directory + File.separator + shortFilename + '.' + i + '.' + extension;
        if (copyFile(src, dest)) {
            return dest;
        } else {
            throw new Exception("ERROR in copyToTempFile because copyFile(\"" + src + "\",\"" + dest + "\") failed");
        }
    }

    /** @return The File object if successful, otherwise null.
     */
    public static File saveFile(String dir, String relativeName, String content) {
        File result = null;
        PrintWriter writer;
        try {
            result = openFile(dir, relativeName, true);
            writer = new PrintWriter(new FileOutputStream(result));
        } catch (Exception e) {
            System.out.println("Can't write to file in FileTools.saveFile: " + relativeName + "  \r\n" + Tools.errorToString(e, true));
            return null;
        }
        writer.write(content);
        writer.close();
        return result;
    }

    public static File saveFile(String dir, String relativeName, byte[] content) {
        File result = null;
        try {
            result = openFile(dir, relativeName, true);
            java.io.FileOutputStream out = new java.io.FileOutputStream(result);
            out.write(content);
            out.close();
            return result;
        } catch (Exception e) {
            System.out.println("Can't write to file in FileTools.saveFile: " + dir + " rel: " + relativeName + "  \r\n" + Tools.errorToString(e, true));
            return null;
        }
    }

    public static String generateBackupName(String basename) throws Exception {
        String BAK = ".bak";
        File file;
        int i = 0;
        do {
            i++;
            file = new File(basename + BAK + i);
        } while (file.exists());
        return file.getCanonicalPath();
    }

    public static boolean backup(String destNewName) {
        try {
            if ((new File(destNewName)).exists()) {
                String tempName = FileTools.generateBackupName(destNewName);
                FileTools.copyFile(destNewName, tempName);
            }
            return true;
        } catch (Exception e) {
            Log.error(FileTools.class, "ERROR in backup of file: " + destNewName, e);
            return false;
        }
    }

    /** @return false if file is a directory or file not found.
     */
    public static boolean fileExists(String filename) {
        if (filename == null) {
            return false;
        }
        File f = new File(Tools.fixFilename(filename));
        if (f.exists() && !f.isDirectory()) {
            return true;
        }
        return false;
    }

    /** @return false if file is a not a directory or is not found.
     */
    public static boolean directoryExists(String filename) {
        File f = new File(Tools.fixFilename(filename));
        if (f.exists() && f.isDirectory()) {
            return true;
        }
        return false;
    }

    /** @return null if file not found, etc.
     */
    public static Properties loadPropertiesFromFile(String dir, String relativeName) {
        try {
            Properties props = new Properties();
            File theFile = openFile(dir, relativeName);
            if (theFile == null) {
                return null;
            }
            if (!theFile.isDirectory()) {
                FileInputStream fis = new FileInputStream(theFile);
                props.load(fis);
                fis.close();
                return props;
            }
        } catch (Exception e) {
        }
        return null;
    }

    /** @param headerComment can be null, if no # comment is desired at the top of the file.
     */
    public static boolean savePropertiesToFile(Properties props, String headerComment, String dir, String relativeName) {
        try {
            FileOutputStream fos = new FileOutputStream(openFile(dir, relativeName, true));
            props.store(fos, headerComment);
            fos.close();
            return true;
        } catch (Exception e) {
            Log.error(FileTools.class, "savePropertiesToFile: ", e);
        }
        return false;
    }

    public static List<String> fileToLines(File file) throws FileNotFoundException {
        List<String> list = new ArrayList<String>();
        Scanner scanner = new Scanner(new FileReader(file));
        try {
            while (scanner.hasNextLine()) {
                list.add(scanner.nextLine());
            }
        } finally {
            scanner.close();
        }
        return list;
    }

    public static void usage() {
        System.out.println("Usage: java com.dynamide.util.FileTools -testDiver | -testSaveFile <filename>");
    }

    public static void main(String args[]) {
        if (args.length == 0) {
            usage();
            System.exit(0);
        }
        Opts opts = new Opts(args);
        if (opts.getOptionBool("-testDiver")) {
            FileTools tools = new FileTools();
            directoryDiver("FileTools.main", "C:\\temp", tools);
        }
        String filename = opts.getOption("-testSaveFile");
        if (filename.length() > 0) {
            saveFile("", filename, "This is a test\r\nThis is line 2\r\n");
        }
        filename = opts.getOption("-testBinaryFile");
        if (filename.length() > 0) {
            String content = readFile("", filename);
            System.out.println("content: " + content);
            saveFile("", filename + ".bin", content);
        }
    }
}
