package common;

import common.log.Log;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
   This class encapsulates a number of static methods related to input/output.
*/
public final class IO {

    public static final FileType DIRECTORY_ONLY = new FileType();

    public static final FileType FILE_ONLY = new FileType();

    public static final FileType ALL = new FileType();

    public static final boolean IS_WINDOWS = System.getProperty("file.separator").equals("\\");

    public static final String FILE_SEPARATOR = System.getProperty("file.separator");

    public static final String LINE_SEPARATOR = System.getProperty("line.separator");

    public static final File TEMP_DIR = new File(System.getProperty("java.io.tmpdir"));

    /** Character replaced by an index in backup/temporary file names. */
    public static final String BACKUP_INDEX_CHAR = "$";

    public static final String DEFAULT_BACKUP_PREFIX = "backup" + BACKUP_INDEX_CHAR + ".";

    /** compareContent() status: files compare. */
    public static final FileContent C_SAME = new FileContent("files content is the same");

    /** compareContent() status: file 1 not comparable. */
    public static final FileContent C_FILE1_NF = new FileContent("file 1 not found, not a file, or caused i/o error");

    /** compareContent() status: file 2 not comparable. */
    public static final FileContent C_FILE2_NF = new FileContent("file 2 not found, not a file, or caused i/o error");

    /** compareContent() status: files differ. */
    public static final FileContent C_DIFFER = new FileContent("files content is different");

    /** Character that can be part of a base name on Windows or *nix. */
    private static final String LEGAL_FILENAME_CHARS = " _%^&()-=+,.";

    private static final Random ourRandom = new Random();

    /** Base buffer size, 16K bytes. */
    private static final int BASE_BUFFER_SIZE = 1024 * 16;

    /** Big buffer size, 128K bytes. */
    private static final long BIG_BUFFER_SIZE = 1024L * 128L;

    /**
     Compares the content of two files.

     @param theFile1 the first file to compare.

     @param theFile2 the second file to compare.

     @return {@link #C_FILE1_NF} if the first file is not found, is not a
     file, is not readable, or reading causes an i/o error, {@link
     #C_FILE2_NF} if the first file is not found, is not a file, is not
     readable, or reading causes an i/o error, {@link #C_DIFFER} if the
     file's content differ or {@link #C_SAME} if the files have the same
     content.
  */
    public static FileContent compareContent(File theFile1, File theFile2) {
        FileContent content = null;
        if ((theFile1 == null) || !theFile1.exists() || !theFile1.isFile()) {
            content = C_FILE1_NF;
        } else if ((theFile2 == null) || !theFile2.exists() || !theFile2.isFile()) {
            content = C_FILE2_NF;
        } else if (theFile1.equals(theFile2)) {
            content = C_SAME;
        } else if (theFile1.length() != theFile2.length()) {
            content = C_DIFFER;
        } else {
            content = C_SAME;
            int bufferSize = (int) Math.min(BIG_BUFFER_SIZE, theFile1.length());
            byte[] buffer1 = new byte[bufferSize];
            byte[] buffer2 = new byte[bufferSize];
            FileInputStream in1 = null;
            FileInputStream in2 = null;
            try {
                in1 = new FileInputStream(theFile1);
            } catch (Exception e) {
                Log.main.println("IO.compareContent(), file1=" + theFile1, e);
                content = C_FILE1_NF;
            }
            if (content == C_SAME) {
                try {
                    in2 = new FileInputStream(theFile2);
                } catch (Exception e) {
                    Log.main.println("IO.compareContent(), file2=" + theFile2, e);
                    content = C_FILE2_NF;
                }
                while (content == C_SAME) {
                    int read1 = 0;
                    int read2 = 0;
                    try {
                        read1 = in1.read(buffer1, 0, bufferSize);
                    } catch (Exception e) {
                        content = C_FILE1_NF;
                        Log.main.println("IO.compareContent(), file1=" + theFile1, e);
                        break;
                    }
                    try {
                        read2 = in2.read(buffer2, 0, bufferSize);
                    } catch (Exception e) {
                        Log.main.println("IO.compareContent(), file2=" + theFile2, e);
                        content = C_FILE2_NF;
                        break;
                    }
                    if (read1 != read2) {
                        content = C_DIFFER;
                    } else if (read1 < 1) {
                        break;
                    } else {
                        for (int index = 0; index < read1; index++) {
                            if (buffer1[index] != buffer2[index]) {
                                content = C_DIFFER;
                                break;
                            }
                        }
                    }
                }
                if (in2 != null) {
                    try {
                        in2.close();
                    } catch (Exception e) {
                        Log.main.println("IO.compareContent(), file2=" + theFile2, e);
                    }
                }
            }
            if (in1 != null) {
                try {
                    in1.close();
                } catch (Exception e) {
                    Log.main.println("IO.compareContent(), file1=" + theFile1, e);
                }
            }
        }
        return content;
    }

    /**
     Returns a valid (on Windows or unix) file name from the specified string.
     Note the string should only specify the base name, i.e., both '/' and '\\'
     will be converted to an underscore.

     @param theString potential base name string, possibly including invalid
     characters.

     @return the file base name.
  */
    public static String getFileName(String theString) {
        String name = "_";
        if (theString != null) {
            StringBuffer buffer = new StringBuffer();
            for (char chr : theString.toCharArray()) {
                if (!Character.isLetterOrDigit(chr) && (LEGAL_FILENAME_CHARS.indexOf(chr) < 0)) {
                    chr = '_';
                }
                buffer.append(chr);
            }
            name = buffer.toString();
        }
        return name;
    }

    /**
     Returns the absolute path name for the specified file.

     @param theFileName the file name.

     @return the absolute path name for the file.
  */
    public static String getAbsolutePath(String theFileName) {
        File file = new File(theFileName);
        String name = file.getAbsolutePath();
        if ((name.indexOf(".." + FILE_SEPARATOR) >= 0) || (name.indexOf(".." + FILE_SEPARATOR) >= 0)) {
            try {
                name = file.getCanonicalPath();
            } catch (Exception e) {
            }
        }
        return name;
    }

    /**
     Returns the full path name to the current directory.

     @return the full path name to the current directory.
  */
    public static String getCurrentDirectory() {
        String cd = System.getProperty("user.dir");
        File current_dir = new File(cd);
        cd = current_dir.getAbsolutePath();
        int l = cd.length();
        if (cd.substring(l - 1).equals(System.getProperty("file.separator"))) {
            cd = cd.substring(0, l - 1);
        }
        return cd.replace('\\', '/');
    }

    /**
     Checks if the specified file exists.  If it exists, ensures it can be
     read.  If it does not exist, optionally displays an error dialog.

     @param theFileName the name of the file.

     @param theShowWarning show a warning dialog.

     @return true if the file exists and can be read, else false.

     @see #canRead(String)
  */
    public static boolean canRead(String theFileName, boolean theShowWarning) {
        boolean read = true;
        if ((theFileName == null) || theFileName.equals("")) {
            read = false;
        } else {
            File inputFile = new File(theFileName);
            if (!inputFile.exists() || !inputFile.canRead()) {
                if (theShowWarning) {
                }
                read = false;
            }
        }
        return read;
    }

    /**
     Checks if the specified file exists.  If it exists, ensures it can be
     read.  If it does not exist, this method displays an error dialog.

     @param theFileName the name of the file.

     @return true if the file can be read; okay to overwrite it and permissions
     allow it, else return false.

     @see #canRead(String, boolean)
  */
    public static boolean canRead(String theFileName) {
        return canRead(theFileName, true);
    }

    /**
     Returns the contents of a file as a String.

     @param theSource the source file.

     @param theMaxSize the maximum size file, or zero for unlimited size.

     @return the file contents as a String or null if the file is not found or
     is too large.
  */
    public static String getString(File theSource, int theMaxSize) {
        String value = null;
        if (theSource.exists() && theSource.canRead()) {
            long lSize = theSource.length();
            if ((theMaxSize > 0) && (lSize > theMaxSize)) {
                Log.main.println(Log.FAULT, "IO.getString(), file " + theSource.getAbsolutePath() + " too large (>" + theMaxSize);
            } else {
                int size = (int) lSize;
                try {
                    byte[] buffer = new byte[size];
                    FileInputStream in = new FileInputStream(theSource);
                    int numRead = in.read(buffer, 0, size);
                    in.close();
                    value = new String(buffer, 0, numRead);
                } catch (Exception e) {
                    Log.main.println("IO.getString()", e);
                }
            }
        }
        return value;
    }

    /**
     Returns some number of bytes from the contents of a file.

     @param theSource the source file.

     @param theMaxSize the maximum size to read, or zero for unlimited size.

     @return the contents read from the file, or null null if the file is
     not found or an i/o exception occurs.
  */
    public static byte[] getBytes(File theSource, int theMaxSize) {
        byte[] buffer = null;
        if (theSource.exists() && theSource.canRead()) {
            long lSize = theSource.length();
            int size = (theMaxSize < 1) ? (int) lSize : (int) Math.min(theMaxSize, lSize);
            try {
                buffer = new byte[size];
                FileInputStream in = new FileInputStream(theSource);
                in.read(buffer, 0, size);
                in.close();
            } catch (Exception e) {
                Log.main.println("IO.getBytes()", e);
                buffer = null;
            }
        }
        return buffer;
    }

    /**
     Scans the specified file to determine if it is binary.

     @param theSource the source file.

     @return true if any binary data was found in the file, else return false.
  */
    public static boolean isBinary(File theSource) {
        boolean binary = false;
        if (theSource.exists() && theSource.canRead()) {
            int size = (int) Math.min(BIG_BUFFER_SIZE, theSource.length());
            byte[] bytes = new byte[size];
            try {
                FileInputStream in = new FileInputStream(theSource);
                while (in.read(bytes, 0, size) > 0) {
                    for (byte data : bytes) {
                        if ((data <= 0) || (data > 127)) {
                            binary = true;
                            break;
                        }
                    }
                }
                in.close();
            } catch (Exception e) {
                Log.main.println("IO.isBinary()", e);
            }
        }
        return binary;
    }

    /**
     Writes the specified array of bytes to the specified file.

     @param theFile the write to be written.

     @param theBytes the array of bytes to be written.

     @return true if the file was written.
  */
    public static boolean writeBytes(File theFile, byte[] theBytes) {
        boolean ok = false;
        try {
            if (theFile.exists()) {
                IO.delete(theFile);
            }
            int size = theBytes.length;
            FileOutputStream out = new FileOutputStream(theFile);
            out.write(theBytes, 0, size);
            out.close();
            ok = true;
        } catch (Exception e) {
            Log.main.println(Log.FAULT, "IO.writeBytes: " + e.getMessage());
        }
        return ok;
    }

    /**
     Makes a backup of the specified file using a rolling naming scheme, e.g.,
     backupN.FILE where 0 <= N < theMaxNumber.  When all backup files are in
     use, the oldest of the set will be reused.

     @param theSource the source file to be backed up.

     @param theBackupDir the backup directory, or null to use the same
     directory as the source file.

     @param thePrefix the backup file prefix, where the character '$'
     is substituted with the file index number.  If the prefix is null,
     or contains no '$', a prefix of {@link #DEFAULT_BACKUP_PREFIX} is used.

     @param theMaxNumber the maximum number of backups to be maintained.

     @return the File used for the backup or null if the source file does
     no exist.
  */
    public static File backupFile(File theSource, File theBackupDir, String thePrefix, int theMaxNumber) {
        int maxNumber = Math.max(theMaxNumber, 1);
        String prefix = (thePrefix == null) ? DEFAULT_BACKUP_PREFIX : thePrefix;
        prefix = (prefix.indexOf(BACKUP_INDEX_CHAR) < 0) ? DEFAULT_BACKUP_PREFIX : prefix;
        File backup = null;
        if (theSource.exists() && theSource.canRead()) {
            File backupDir = (theBackupDir != null) ? theBackupDir : theSource.getParentFile();
            if (!backupDir.exists()) {
                backupDir.mkdirs();
            }
            String baseName = theSource.getName();
            for (int index = 0; index < maxNumber; index++) {
                File file = IO.getFile(backupDir, prefix.replace(BACKUP_INDEX_CHAR, String.valueOf(index)) + baseName);
                if (!file.exists()) {
                    backup = file;
                    break;
                } else if (backup == null) {
                    backup = file;
                } else if (file.lastModified() < backup.lastModified()) {
                    backup = file;
                }
            }
            IO.copyFile(theSource, backup);
            backup.setLastModified(theSource.lastModified());
        }
        return backup;
    }

    /**
     Returns a sorted list of backup files for the specified file using a
     rolling naming scheme, e.g., backupN.FILE where 0 <= N < theMaxNumber.
     The list will be returned with the files sorted from newest to oldest.

     @param theSource the source file to be backed up.

     @param theBackupDir the backup directory, or null to use the same
     directory as the source file.

     @param thePrefix the backup file prefix, where the character '$'
     is substituted with the file index number.  If the prefix is null,
     or contains no '$', a prefix of {@link #DEFAULT_BACKUP_PREFIX} is used.

     @param theMaxNumber the maximum number of backups to be maintained.

     @return the list of backup files.
  */
    public static ArrayList<File> getBackupFiles(File theSource, File theBackupDir, String thePrefix, int theMaxNumber) {
        ArrayList<File> list = new ArrayList<File>();
        if (theSource != null) {
            int maxNumber = Math.max(theMaxNumber, 1);
            String prefix = (thePrefix == null) ? DEFAULT_BACKUP_PREFIX : thePrefix;
            prefix = (prefix.indexOf(BACKUP_INDEX_CHAR) < 0) ? DEFAULT_BACKUP_PREFIX : prefix;
            File backupDir = (theBackupDir != null) ? theBackupDir : theSource.getParentFile();
            if (backupDir.exists()) {
                String baseName = theSource.getName();
                for (int index = 0; index < maxNumber; index++) {
                    File file = IO.getFile(backupDir, prefix.replace(BACKUP_INDEX_CHAR, String.valueOf(index)) + baseName);
                    if (file.exists()) {
                        int size = list.size();
                        for (int inner = 0; inner < size; inner++) {
                            File old = list.get(inner);
                            if (file.lastModified() > old.lastModified()) {
                                list.add(inner, file);
                                file = null;
                                break;
                            }
                        }
                        if (file != null) {
                            list.add(file);
                        }
                    }
                }
            }
        }
        return list;
    }

    /**
     Read bytes from a file.

     @param theSource the source file.

     @param theMaxBytes the maximum number of bytes to read, or < 0 for reading
     all of the bytes.

     @return a byte array, no larger than the size specified, or null if the
     file could not be read.
  */
    public static byte[] readBytes(File theSource, int theMaxBytes) {
        byte[] buffer = null;
        if (theSource.exists() && theSource.canRead()) {
            long longLen = theSource.length();
            int readLen = (int) Math.min(Integer.MAX_VALUE, longLen);
            if (theMaxBytes >= 0) {
                readLen = Math.min(theMaxBytes, readLen);
            }
            buffer = new byte[readLen];
            try {
                FileInputStream in = new FileInputStream(theSource);
                int numRead = in.read(buffer, 0, readLen);
                if (numRead != readLen) {
                    buffer = null;
                    Log.main.println(Log.FAULT, "IO.readBytes(), for " + theSource + ", read error, requested " + readLen + " bytes, got " + numRead + " bytes");
                }
                in.close();
            } catch (Exception e) {
                Log.main.println(Log.FAULT, "IO.readBytes, for " + theSource + ", read error: " + e.getMessage());
                buffer = null;
            }
        }
        return buffer;
    }

    /**
     Copies the specified source file to the specified target.

     @param theSource the source file.

     @param theTarget the target file.

     @return true if the copy was successful, else returns false.
  */
    public static boolean copyFile(File theSource, File theTarget) {
        boolean ok = false;
        if (theSource.exists() && theSource.canRead()) {
            theTarget.delete();
            int size = (int) Math.min(BIG_BUFFER_SIZE, theSource.length());
            byte[] bytes = new byte[size];
            try {
                FileInputStream in = new FileInputStream(theSource);
                FileOutputStream out = new FileOutputStream(theTarget);
                int numRead;
                while ((numRead = in.read(bytes, 0, size)) > 0) {
                    out.write(bytes, 0, numRead);
                }
                out.close();
                in.close();
                Log.main.println(Log.VERBOSE, theSource.getAbsolutePath() + " copied to " + theTarget.getAbsolutePath());
                ok = true;
            } catch (Exception e) {
                Log.main.println("IO.copyFile()", e);
            }
        }
        return ok;
    }

    /**
     Make the specified directory, if needed.

     @param theDirectory the directory to be created if it does not already
     exist.

     @return true if the directory exists or was created, else false if the
     directory could not be create.
  */
    public static boolean mkdirs(File theDirectory) {
        boolean ok = false;
        try {
            if (theDirectory.exists()) {
                if (theDirectory.isDirectory()) {
                    ok = true;
                } else {
                    Log.main.println(Log.FAULT, "Attempt to create a directory that is a plain file: " + theDirectory);
                }
            } else {
                if (theDirectory.mkdirs()) {
                    ok = true;
                } else {
                    Log.main.println(Log.FAULT, "Failed to create directory: " + theDirectory);
                }
            }
        } catch (Exception e) {
            Log.main.stackTrace("Can't create directory: " + theDirectory, e);
        }
        return ok;
    }

    /**
     Copies all the files from specified source directory to the specified
     target directory.

     @param theSource the source directory.

     @param theTarget the target directory.

     @param theDescendFlag descend into subdirectories of the source, creating
     associated subdirectories in the target.

     @return true if the copy was successful, else returns false.
  */
    public static boolean copyFiles(File theSource, File theTarget, boolean theDescendFlag) {
        boolean ok = true;
        if (theSource.exists() && theSource.isDirectory() && theSource.canRead()) {
            if (!theTarget.exists()) {
                theTarget.mkdir();
            }
            File[] files = theSource.listFiles();
            for (File source : files) {
                String name = source.getName();
                File target = new File(theTarget.getAbsolutePath() + File.separator + name);
                if (source.isDirectory()) {
                    if (theDescendFlag) {
                        if (!copyFiles(source, target, theDescendFlag)) {
                            ok = false;
                        }
                    }
                } else {
                    if (!copyFile(source, target)) {
                        ok = false;
                    }
                }
            }
        }
        return ok;
    }

    /**
     Deletes a file or directory.  The specified path is a directory, all
     files and sub-directories in the directory are deleted.

     @param thePath the File object to be deleted.

     @return true if the path was deleted or does not exist, else return false.
  */
    public static boolean delete(File thePath) {
        boolean success = false;
        if (!thePath.exists()) {
            success = true;
        } else {
            Runtime.getRuntime().runFinalization();
            if (!IS_WINDOWS) {
                unsetReadOnly(thePath);
            }
            if (thePath.isDirectory()) {
                File[] files = thePath.listFiles();
                int numFiles = (files == null) ? 0 : files.length;
                for (int f = 0; f < numFiles; f++) {
                    delete(files[f]);
                }
            }
            success = thePath.delete();
            if (!success) {
                Log.main.println(Log.DEBUG, "not deleted: " + thePath.getAbsolutePath());
            }
        }
        return success;
    }

    /**
     Determines if a file or directory is writable.  If the specified File
     is a directory, a test is made to whether a new file can be written
     to the directory.  If the file is a file and it exists, File.canWrite()
     is used to test the file.  If the file does not exist, an attempt
     if made to create the file as a test.

     @param theFile the file to be tested.

     @return true if the file or directory can be written to, else returns
     false.
  */
    public static boolean canWrite(File theFile) {
        boolean canWrite = false;
        if (theFile.isDirectory()) {
            int count = 0;
            synchronized (ourRandom) {
                count = Math.abs((ourRandom.nextInt() * 1013) % 1000000);
                ourRandom.notifyAll();
            }
            while (true) {
                String name = "canWrite." + String.valueOf(count++) + ".tmp";
                File test = IO.getFile(theFile, name);
                if (!test.exists()) {
                    try {
                        test.createNewFile();
                        test.delete();
                        canWrite = true;
                    } catch (Exception e) {
                    }
                    break;
                }
            }
        } else if (theFile.isFile()) {
            canWrite = theFile.canWrite();
        } else if (!theFile.exists()) {
            File parentDir = theFile.getParentFile();
            if (parentDir != null) {
                canWrite = canWrite(parentDir);
            }
        }
        return canWrite;
    }

    /**
     Resets (removes) the read-only mode on a file or directory.

     @param theFile the file/directory to be made writable.

     @return true if the path is now writable, else return false.
  */
    public static boolean unsetReadOnly(File theFile) {
        boolean success = false;
        if (theFile.exists()) {
            if (theFile.canWrite()) {
                success = true;
            } else {
                String[] command = null;
                String fs = System.getProperty("file.separator");
                if (fs.equals("/")) {
                    command = new String[] { "chmod", "a+w", theFile.getAbsolutePath() };
                } else if (fs.equals("\\")) {
                    command = new String[] { "attrib", "-r", theFile.getAbsolutePath() };
                }
                if (command != null) {
                    Runtime rt = Runtime.getRuntime();
                    try {
                        rt.exec(command);
                        success = true;
                    } catch (Exception e) {
                    }
                }
            }
        }
        return success;
    }

    /**
     Gets a URL from a File object.

     @param theFile the existing File object.

     @return a URL object or null for a malformed URL.
  */
    public static URL getURL(File theFile) {
        URL url = null;
        if (theFile != null) {
            try {
                url = theFile.toURI().toURL();
            } catch (Exception e) {
                Log.main.println(Log.FAULT, "malformed url, file=" + theFile.getAbsolutePath());
            }
        }
        return url;
    }

    /**
     Gets a line from the specified URL.

     @param theURL the existing URL object.

     @return an array of String[] object or null for error.
  */
    public static String[] readLines(URL theURL) {
        String[] lines = null;
        if (theURL != null) {
            try {
                ArrayList<String> list = new ArrayList<String>();
                BufferedReader in = new BufferedReader(new InputStreamReader(theURL.openStream()), BASE_BUFFER_SIZE);
                String line = null;
                while ((line = in.readLine()) != null) {
                    list.add(line);
                }
                in.close();
                int numLines = list.size();
                lines = new String[numLines];
                for (int lineIndex = 0; lineIndex < numLines; numLines++) {
                    lines[lineIndex] = list.get(lineIndex);
                }
            } catch (Exception e) {
                Log.main.println("IO.readLines() url=" + theURL, e);
            }
        }
        return lines;
    }

    /**
     Gets lines from a zip file entry.

     @param theFile the existing ZipFile object.

     @return an array of String[] object or null for error.
  */
    public static String[] readLines(ZipFile theFile, String theEntry) {
        String[] lines = null;
        if (theFile != null) {
            ArrayList<String> list = null;
            try {
                if (theEntry == null) {
                    list = new ArrayList<String>();
                    for (Enumeration<? extends ZipEntry> entries = theFile.entries(); entries.hasMoreElements(); ) {
                        list.add(((ZipEntry) entries.nextElement()).getName());
                    }
                } else {
                    ZipEntry entry = theFile.getEntry(theEntry);
                    if (entry != null) {
                        list = new ArrayList<String>();
                        BufferedReader in = new BufferedReader(new InputStreamReader(theFile.getInputStream(entry)), BASE_BUFFER_SIZE);
                        String line = null;
                        while ((line = in.readLine()) != null) {
                            list.add(line);
                        }
                        in.close();
                    }
                }
            } catch (Exception e) {
                Log.main.println("IO.readLines() zip=" + theFile.getName(), e);
                list = null;
            }
            if (list != null) {
                int numLines = list.size();
                lines = new String[numLines];
                for (int lineIndex = 0; lineIndex < numLines; lineIndex++) {
                    lines[lineIndex] = list.get(lineIndex);
                }
            }
        }
        return lines;
    }

    /**
     Gets bytes from a zip file entry.

     @param theFile the existing ZipFile object.

     @return an array of String[] object or null for error.
  */
    public static byte[] readBytes(ZipFile theFile, String theEntry) {
        byte[] bytes = null;
        if (theFile != null) {
            try {
                ZipEntry entry = theFile.getEntry(theEntry);
                if (entry != null) {
                    byte[] buffer = new byte[BASE_BUFFER_SIZE];
                    ByteArrayOutputStream out = new ByteArrayOutputStream(BASE_BUFFER_SIZE);
                    InputStream in = theFile.getInputStream(entry);
                    int read = 0;
                    while ((read = in.read(buffer)) > 0) {
                        out.write(buffer, 0, read);
                    }
                    in.close();
                    bytes = out.toByteArray();
                }
            } catch (Exception e) {
                Log.main.println("IO.readBytes() zip=" + theFile.getName(), e);
                bytes = null;
            }
        }
        return bytes;
    }

    /**
     Returns a list of files, either all files, only directories, or only
     non-directories.

     @param theDirectory the starting directory.

     @param theType the type of file to return, e.g., {@link #DIRECTORY_ONLY},
     {@link #FILE_ONLY} or {@link #ALL}.

     @param theDescendFlag a flag to specify to descend into sub-directories.

     @return a list of files with the specified prefix and/or suffix.
  */
    public static ArrayList<File> getFiles(File theDirectory, FileType theType, boolean theDescendFlag) {
        ArrayList<File> files = new ArrayList<File>();
        File[] list = theDirectory.listFiles();
        if (list != null) {
            for (File file : list) {
                if (file.isDirectory()) {
                    if (theType != FILE_ONLY) {
                        files.add(file);
                    }
                    if (theDescendFlag) {
                        files.addAll(getFiles(file, theType, true));
                    }
                } else if (theType != DIRECTORY_ONLY) {
                    files.add(file);
                }
            }
        }
        return files;
    }

    /**
     Sorts a list of existing files by the time last modified.

     @param theList the list of existing File objects.

     @param theNewTopFlag when true, sort the list from newest to oldest,
     else sort the list from oldest to newest.

     @return the list of existing files, sorted as specified.
  */
    public static ArrayList<File> sortFiles(ArrayList<File> theList, boolean theNewTopFlag) {
        ArrayList<File> sorted = new ArrayList<File>();
        for (File file : theList) {
            if (!file.exists()) {
                continue;
            }
            long fileTime = file.lastModified();
            int size = sorted.size();
            for (int index = 0; index < size; index++) {
                long oldTime = sorted.get(index).lastModified();
                if ((theNewTopFlag && (fileTime > oldTime)) || ((!theNewTopFlag) && (fileTime < oldTime))) {
                    sorted.add(index, file);
                    file = null;
                    break;
                }
            }
            if (file != null) {
                sorted.add(file);
            }
        }
        return sorted;
    }

    /**
     Returns a list of files with the specified prefix and/or suffix.

     @param theDirectory the starting directory.

     @param thePrefix an optional file name prefix specification or null.

     @param theSuffix an optional file name suffix specification or null.

     @param theDescendFlag a flag to specify to descend into sub-directories.

     @return a list of files with the specified prefix and/or suffix.
  */
    public static ArrayList<File> getFiles(File theDirectory, String thePrefix, String theSuffix, boolean theDescendFlag) {
        ArrayList<File> list = new ArrayList<File>();
        if ((theDirectory != null) && theDirectory.exists() && theDirectory.isDirectory()) {
            File[] files = theDirectory.listFiles();
            for (File file : files) {
                if (file.isDirectory()) {
                    if (theDescendFlag) {
                        list.addAll(getFiles(file, thePrefix, theSuffix, true));
                    }
                } else if ((thePrefix != null) && !thePrefix.equals("") && !file.getName().startsWith(thePrefix)) {
                } else if ((theSuffix != null) && !theSuffix.equals("") && !file.getName().endsWith(theSuffix)) {
                } else {
                    list.add(file);
                }
            }
        }
        return list;
    }

    /**
     Builds a new File object from an existing File and a suffix.

     @param theFile the existing File (file or directory) object.

     @param theSuffix the file suffix or filename.

     @return a new File object.
  */
    public static File getFile(File theFile, String theSuffix) {
        File newFile = null;
        if (theFile.isDirectory()) {
            newFile = new File(theFile.getAbsolutePath() + FILE_SEPARATOR + theSuffix);
        } else {
            newFile = new File(theFile.getAbsolutePath() + theSuffix);
        }
        return newFile;
    }

    /**
     Returns a File for the specified base file name in the temporary file
     directory.

     @param theName the base file name.

     @param theUniqueFlag a flag to specify the name must be unique, e.g.,
     not already exist.  When true, a '$' character in theName will be replaced
     with a number to make the name unique.  If no '$' character is present in
     theName, the number is prefixed to the base name.

     @return a new File object.  The file will be deleted on normal VM exit.
  */
    public static File getTempFile(String theName, boolean theUniqueFlag) {
        File newFile = null;
        if (!theUniqueFlag) {
            newFile = getFile(TEMP_DIR, theName);
        } else {
            int count = 0;
            while (true) {
                String name = theName;
                String number = String.valueOf(count++);
                if (theName.indexOf('$') >= 0) {
                    name = StringUtils.replaceText(theName, "$", number);
                } else {
                    name = number + "." + theName;
                }
                newFile = getFile(TEMP_DIR, name);
                if (!newFile.exists()) {
                    break;
                }
            }
        }
        newFile.deleteOnExit();
        return newFile;
    }

    /**
     Returns the base name for the specified file.

     @param theFileName the file name.

     @return the base name for the file.
  */
    public static final String getBaseName(String theFileName) {
        File file = new File(theFileName);
        return file.getName();
    }

    /**
     Returns an array of File objects that are children of the specified parent.

     @param theParent the parent.
  */
    public static final File[] getSubTree(File theParent) {
        Vector<File> list = new Vector<File>();
        getSubTree(theParent, list);
        int size = list.size();
        File[] files = new File[size];
        for (int i = 0; i < size; i++) {
            files[i] = list.elementAt(i);
        }
        return files;
    }

    public static final void getSubTree(File theParent, Vector<File> theList) {
        if (theParent.exists()) {
            if (!theParent.isDirectory()) {
                theList.add(theParent);
            } else {
                File[] list = theParent.listFiles();
                int numList = (list == null) ? 0 : list.length;
                for (int l = 0; l < numList; l++) {
                    if (list[l].isDirectory()) {
                        getSubTree(list[l], theList);
                    } else {
                        theList.add(list[l]);
                    }
                }
            }
        }
    }

    public static class FileType {

        private FileType() {
        }
    }

    public static class FileContent {

        private String myTitle;

        private FileContent(String theTitle) {
            myTitle = theTitle;
        }

        @Override
        public String toString() {
            return myTitle;
        }
    }
}
