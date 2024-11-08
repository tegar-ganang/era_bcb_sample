package freestyleLearningGroup.independent.util;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.tree.*;

/**
 * This class provides some useful routines to copy and delete files and directories.
 * <p> Special Warning: Only one Copy Thread of this class can run at a special time.
 * @version 1.0
 */
public class FLGFileUtility {

    public static File createNewFile(String fileNamePrefix, String fileNameSuffix, File directory) {
        long fileNameNumber = 1;
        while (new File(directory, fileNamePrefix + fileNameNumber + fileNameSuffix).exists()) fileNameNumber++;
        File newFile = new File(directory, fileNamePrefix + fileNameNumber + fileNameSuffix);
        if (!directory.exists()) directory.mkdirs();
        return newFile;
    }

    public static File createNamedDirectory(String directoryPrefix, String directorySuffix, File directory) {
        if (new File(directory, directoryPrefix + directorySuffix).exists()) {
            return createNewDirectory(directoryPrefix, directorySuffix, directory);
        }
        File newFile = new File(directory, directoryPrefix + directorySuffix);
        if (!directory.exists()) directory.mkdirs();
        if (newFile.mkdirs()) return newFile;
        return null;
    }

    public static File createNewDirectory(String directoryPrefix, String directorySuffix, File directory) {
        long directoryNumber = 1;
        while (new File(directory, directoryPrefix + directoryNumber + directorySuffix).exists()) directoryNumber++;
        File newFile = new File(directory, directoryPrefix + directoryNumber + directorySuffix);
        if (!directory.exists()) directory.mkdirs();
        if (newFile.mkdirs()) return newFile;
        return null;
    }

    /** This method copies all files from one directory to another including all subdirectories */
    public static void copyDirectoryContent(File sourceDirectory, File destinationDirectory) {
        Vector directoryContent = readDirectoryContent(sourceDirectory);
        for (int i = 0; i < directoryContent.size(); i++) {
            String sourcePath = directoryContent.elementAt(i).toString();
            int sourceDirectoryPathLength = sourceDirectory.getPath().length();
            StringBuffer relativePathBuffer = new StringBuffer();
            for (int j = sourceDirectoryPathLength; j < sourcePath.length(); j++) {
                relativePathBuffer.append(sourcePath.charAt(j));
            }
            String destinationPath = destinationDirectory.getPath() + new String(relativePathBuffer);
            File sourceFile = new File(sourcePath);
            File destinationFile = new File(destinationPath);
            if (!sourceFile.isDirectory()) copyFile(sourceFile, destinationFile); else {
                if (!destinationFile.exists()) {
                    destinationFile.mkdirs();
                    destinationFile.mkdir();
                }
            }
        }
    }

    public static String createNewRelativeFileName(String fileNamePrefix, String fileNameSuffix, File directory) {
        long fileNameNumber = 1;
        while (new File(directory, fileNamePrefix + fileNameNumber + fileNameSuffix).exists()) fileNameNumber++;
        return fileNamePrefix + fileNameNumber + fileNameSuffix;
    }

    public static void copyFile(File source, File target) {
        try {
            target.getParentFile().mkdirs();
            byte[] buffer = new byte[4096];
            int len = 0;
            FileInputStream in = new FileInputStream(source);
            FileOutputStream out = new FileOutputStream(target);
            while ((len = in.read(buffer)) != -1) out.write(buffer, 0, len);
            in.close();
            out.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static String readFileInString(File file) {
        StringBuffer stringBuffer = new StringBuffer();
        try {
            FileReader fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String newLine = null;
            while ((newLine = bufferedReader.readLine()) != null) {
                stringBuffer.append(newLine + "\n");
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        return stringBuffer.toString();
    }

    public static void writeStringIntoFile(String text, File file) {
        try {
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(file)));
            out.println(text);
            out.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static boolean directoryContains(File directory, String fileName, boolean ignoreCase) {
        String[] fileNames = directory.list();
        for (int i = 0; i < fileNames.length; i++) {
            if (ignoreCase) {
                if (fileNames[i].equalsIgnoreCase(fileName)) return true;
            } else {
                if (fileNames[i].equals(fileName)) return true;
            }
        }
        return false;
    }

    public static void cleanDirectory(File directory, File[] fileList) {
        if (directory == null || directory.getAbsolutePath().length() < 5) return;
        Vector directoryContent = readDirectoryContent(directory);
        if (fileList != null) {
            for (int i = 0; i < fileList.length; i++) {
                File file = fileList[i];
                File parent = file.getParentFile();
                directoryContent.remove(file.getPath());
                if (file.isDirectory()) {
                    Vector subDirectoryContent = readDirectoryContent(file);
                    for (int j = 0; j < subDirectoryContent.size(); j++) {
                        String subDirectoryFileName = subDirectoryContent.elementAt(j).toString();
                        directoryContent.remove(subDirectoryFileName);
                    }
                }
                while (!parent.getPath().equals(directory.getPath())) {
                    directoryContent.remove(parent.getPath());
                    parent = parent.getParentFile();
                }
            }
        }
        for (int i = 0; i < directoryContent.size(); i++) {
            String fileName = (String) directoryContent.elementAt(i);
            File file = new File(fileName);
            file.delete();
        }
    }

    public static void deleteDirectory(File directory) {
        if ((directory != null) && (directory.isDirectory())) {
            cleanDirectory(directory, null);
            directory.delete();
        }
    }

    /** This Mehod returns a vector all subdirectories of a given directory */
    public static Vector getSubdirectories(File directory) {
        Vector contentVector = new Vector();
        File[] content = directory.listFiles();
        if (content != null) {
            for (int i = 0; i < content.length; i++) {
                if (content[i].isDirectory()) {
                    contentVector.addAll(getSubdirectories(content[i]));
                    contentVector.add(content[i].getPath());
                }
            }
        }
        return contentVector;
    }

    /** 
     * This method creates a vector containing all file names (Strings) of a given directory including subdirectories.
     * @param File directory
     * @return Vector contentVector
     */
    public static Vector readDirectoryContent(File directory) {
        Vector contentVector = new Vector();
        File[] content = directory.listFiles();
        if (content != null) {
            for (int i = 0; i < content.length; i++) {
                if (content[i].isDirectory()) {
                    contentVector.addAll(readDirectoryContent(content[i]));
                }
                contentVector.add(content[i].getPath());
            }
        }
        return contentVector;
    }

    /**
     * Carsten Fiedler, 07.09.2006
     * This method performs lexicographically sorting of a given <code>File</code> array
     * in ascending or descending order. Return value is a new <code>File</code> array
     * containing the original Strings in sorted manner.
     * @param <code>File[]</code> unsorted file array
     * @param <code>boolean</code> true for ascending order 
     * @return sorted <code>File[]<code> array
     */
    public static File[] bubbleSortFiles(File[] unsortedFiles, boolean ascending) {
        if (unsortedFiles.length < 2) return unsortedFiles;
        if (ascending) {
            for (int i = 0; i < unsortedFiles.length - 1; i++) {
                for (int j = 1; j < unsortedFiles.length - 1 - i; j++) if ((unsortedFiles[j + 1].getName()).compareToIgnoreCase(unsortedFiles[j].getName()) < 0) {
                    File swap = unsortedFiles[j];
                    unsortedFiles[j] = unsortedFiles[j + 1];
                    unsortedFiles[j + 1] = swap;
                }
            }
        } else {
            for (int i = unsortedFiles.length - 2; i >= 0; i--) {
                for (int j = unsortedFiles.length - 2 - i; j >= 0; j--) if ((unsortedFiles[j + 1].getName()).compareToIgnoreCase(unsortedFiles[j].getName()) > 0) {
                    File swap = unsortedFiles[j];
                    unsortedFiles[j] = unsortedFiles[j + 1];
                    unsortedFiles[j + 1] = swap;
                }
            }
        }
        return unsortedFiles;
    }

    /** 
     * Carsten Fiedler, 07.09.2006
     * This method creates a <code>DefaultTreeModel</code> containing all files of a given directory including subdirectories.
     * @param <code>File</code> directory
     * @return <code>DefaultTreeModel</code> directoryTreeModel
     */
    public static DefaultTreeModel loadDirectoryContent(File directory) {
        DefaultTreeModel directoryTreeModel;
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(directory);
        directoryTreeModel = new DefaultTreeModel(root);
        File[] content = directory.listFiles();
        content = FLGFileUtility.bubbleSortFiles(content, true);
        if (content != null) {
            for (int i = 0; i < content.length; i++) {
                if (content[i].isDirectory()) {
                    loadDirectoryContent(content[i], root, directoryTreeModel);
                } else {
                    directoryTreeModel.insertNodeInto(new DefaultMutableTreeNode(content[i]), root, root.getChildCount());
                }
            }
        }
        return directoryTreeModel;
    }

    private static DefaultTreeModel loadDirectoryContent(File directory, DefaultMutableTreeNode parent, DefaultTreeModel model) {
        if (directory != null) {
            DefaultMutableTreeNode currentRoot = new DefaultMutableTreeNode(directory);
            model.insertNodeInto(currentRoot, parent, parent.getChildCount());
            File[] content = directory.listFiles();
            if (content != null) {
                for (int i = 0; i < content.length; i++) {
                    if (content[i].isDirectory()) {
                        loadDirectoryContent(content[i], currentRoot, model);
                    } else {
                        model.insertNodeInto(new DefaultMutableTreeNode(content[i]), currentRoot, currentRoot.getChildCount());
                    }
                }
            }
        }
        return model;
    }

    /** This method returns all first-level subdirectories of a given parent directory */
    public static File[] listDirectories(File parentDirectory) {
        Vector subDirs = new Vector();
        File[] subFiles = parentDirectory.listFiles();
        File[] subDirectories = new File[0];
        if (subFiles != null) {
            for (int i = 0; i < subFiles.length; i++) {
                if (subFiles[i].isDirectory()) subDirs.add(subFiles[i]);
            }
            subDirectories = new File[subDirs.size()];
            for (int i = 0; i < subDirs.size(); i++) {
                subDirectories[i] = (File) subDirs.get(i);
            }
        }
        return subDirectories;
    }

    /**
     * Checks if the use can write to directory.
     * @param  directory a <code>File</code> object specifying the path to a directory
     * @return true, if the file could be created.
     */
    public static boolean isDirectoryWritable(File directory) {
        boolean result = true;
        File tmpFile = new File(directory, "test.tmp");
        try {
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(tmpFile));
            for (int i = 0; i < 10; i++) {
                bos.write(0);
            }
            bos.close();
            tmpFile.delete();
        } catch (Exception e) {
            result = false;
        }
        return result;
    }

    /**
     * Creates a new (not existing) name for a directory. A <code>string</code> prefix is
     * concatenated with an <code>integer</code> to generate a new name. If no directory
     * having this name exists in the path, a new <code>File</code> object is returned.
     * Otherwise the number is increased by one. <p> Example: prefix "user" might result in a new
     * directory called "user1". <p> There directory is NOT created! This is done by calling
     * <code>mkdir</code> on the new <code>File</code>.
     * @param parent a <code>File</code> object specifying the path to a directory in which the new directory will be created
     * @param prefix a <code>String</code> that is used as the prfix of the directory name.
     * @return a new <code>File</code> object specifying a new directory.
     */
    public static File getNextDirectoryName(File parent, String prefix) {
        File result;
        int counter = 0;
        result = new File(parent, prefix + counter);
        while (result.exists()) {
            counter++;
            result = new File(parent, prefix + counter);
        }
        return result;
    }

    /**
     * This method returns the file extension of a given name. This is done by seeking backwards
     * for the first occurance of a ".".
     * @param a <code>File</code> object
     * @return a <code>String</code> object which descripes the extension of this file object.
     */
    public static String getExtension(String filename) {
        String ext = "";
        int i = filename.lastIndexOf('.');
        if (i > 0 && i < filename.length() - 1) {
            ext = filename.substring(i + 1).toLowerCase();
        }
        return ext;
    }

    /**
     * Carsten Fiedler, 03.10.2006
     * This method removes the file extension of a given name. 
     * @param <code>String</code> name with file extension
     * @return <code>String</code> name without file extension
     */
    public static String removeExtension(String fileName) {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < fileName.length(); i++) {
            if (fileName.charAt(i) == '.') {
                break;
            } else {
                buffer.append(fileName.charAt(i));
            }
        }
        return buffer.toString();
    }

    /**
     * Return absolute URL for a given relative path
     * @param obj Object which wants to resolve the relative path
     * @param relativePath A given relative path to resolve
     * @return absolute URL
     */
    public URL getAbsolutePath(Object obj, String relativePath) {
        return obj.getClass().getClassLoader().getResource(relativePath);
    }

    /**
     * Copy a single file-object to another
     * @param source a <code>File</code> to copy
     * @param destination a new <code>File</code>
     * @return successful or not
     */
    public static boolean copy(File sourceFile, File destinationFile) {
        return hardCopy(sourceFile, destinationFile, null);
    }

    /**
     * Copy a single file-object to another
     * @param source a <code>File</code> to copy
     * @param destination a new <code>File</code>
     * @param errorLog A Stringbuffer to report errors to. Could be <code>null</code>. Then, all
     * reports are send to the standard-output.
     * @return successful or not
     */
    public static boolean copy(File sourceFile, File destinationFile, StringBuffer errorLog) {
        return hardCopy(sourceFile, destinationFile, errorLog);
    }

    /**
     * Copy a single file-object to another
     * @param source a <code>File</code> to copy
     * @param destination a new <code>File</code>
     * @return 	successful or not
     */
    public static boolean copy(String source, String destination) {
        return copy(source, destination, null);
    }

    /**
     * Copy a single file-object to another
     * @param source a <code>File</code> to copy
     * @param destination a new <code>File</code>
     * @param errorLog A Stringbuffer to report errors to. Could be <code>null</code>. Then, all
     * reports are send to the standard-output.
     * @return successful or not
     */
    public static boolean copy(String source, String destination, StringBuffer errorLog) {
        File sourceFile = new File(source);
        File destinationFile = new File(destination);
        return hardCopy(sourceFile, destinationFile, errorLog);
    }

    /**
     * Count the number of files (non-directories) within a directory
     * @param directory a <code>File</code> object specifying a directory or file
     * @param fileFilter extensions of files to count
     * @return number of files within directory (including subdriectories)
     */
    public static long fileCount(File directory, String[] fileFilter) {
        long result = 0;
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            int length = files.length;
            for (int i = 0; i < length; i++) {
                if (files[i].isDirectory()) result += fileCount(files[i], fileFilter); else {
                    for (int j = 0; j < fileFilter.length; j++) {
                        if (files[i].getName().endsWith(fileFilter[j])) {
                            result++;
                            break;
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * Count the number of files (non-directories) within a directory
     * @param directory a <code>File</code> object specifying a directory or file
     * @return number of files within directory (including subdriectories)
     */
    public static long fileCount(File directory) {
        long result = 0;
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            int length = files.length;
            for (int i = 0; i < length; i++) {
                if (files[i].isDirectory()) result += fileCount(files[i]); else {
                    result++;
                }
            }
        }
        return result;
    }

    /**
     * Calculate the occupied space in bytes of a <code>File</code>.
     * @param directory a <code>File</code> object specifying a directory or file
     * @param destination a new <code>File</code>
     * @return the size of the directory (including subdriectories) or file
     */
    public static long size(File directory) {
        long result = 0;
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            int length = files.length;
            for (int i = 0; i < length; i++) {
                if (files[i].isDirectory()) result += size(files[i]); else result += files[i].length();
            }
        } else {
            result = directory.length();
        }
        return result;
    }

    /**
     * This method copies a file to another directory.
     * @param targetFile	a <code>File</code> to copy
     * @param sourcedirectory The directory the file will be copied to.
     * @return					true, if successful.
     */
    public static boolean copy2Directory(File sourceFile, File targetDirectory) {
        return copy2Directory(sourceFile, targetDirectory, null);
    }

    /**
     * This method copies a file to another directory.
     * @param sourcFile	a <code>File</code> to copy
     * @param targetdirectory The directory the file will be copied to.
     * @param errorLog A Stringbuffer to report errors to. Could be <code>null</code>. Then, all
     * reports are send to the standard-output.
     * @return true, if successful.
     */
    public static boolean copy2Directory(File sourceFile, File targetDirectory, StringBuffer errorLog) {
        boolean result;
        result = hardCopy(sourceFile, new File(targetDirectory, sourceFile.getName()), errorLog);
        return result;
    }

    /**
     * This method copies a directory or a file to another directory. <p> Cause it�s possible to
     * copy the content of a whole directory including subdirectories to another directory, the
     * method has to distinguish some different calls and can not
     * be the main recursive copy routine. <p> That means, if <code>sourceFile</code> is a
     * directory and <code> withSubDirectories</code> is true, then the recursive routine
     * will create a new directory in the <code>targetDirectory</code> and
     * store the root files of <code>sourceFile</code> in <code>targetDirectory/sourceFile</code>.
     * <p> This method calls for all subdirectories and files of <code>sourceFile</code> the help
     * method <code>copyFileStructureRecursive</code>. <p> The runtime of this routine of course
     * depends of the files to copy. If the user should have the option to abort the copy process,
     * he can call <code>abort</code>. This will abort the current copy process.
     * @param sourcFile				The directory to copy from.
     * @param targetDirectory		The directory the directory/file will be copied to.
     * @param withSubDirectories	Indicates if subdirectories will be copied, too.
     * @return						true, if successful.
     */
    public static boolean copyFileStructure(File sourceFile, File targetDirectory, boolean withSubDirectories) {
        return copyFileStructure(sourceFile, targetDirectory, withSubDirectories, null);
    }

    /**
     * @param sourcFile				The directory to copy from.
     * @param targetDirectory		The directory the directory/file will be copied to.
     * @param withSubDirectories	Indicates if subdirectories will be copied, too.
     * @param errorLog 				A Stringbuffer to report errors to. Could be <code>null. If
     * so, all reports or send to the standard-output.
     * @return										true, if successful.
     */
    public static boolean copyFileStructure(File sourceDirectory, File targetDirectory, boolean withSubDirectories, StringBuffer errorLog) {
        if (!sourceDirectory.isDirectory()) return false;
        if (!targetDirectory.isDirectory()) return false;
        abortCopy = false;
        abortSuccessful = false;
        boolean result = true;
        File[] srcDirContent = sourceDirectory.listFiles();
        for (int i = 0; i < srcDirContent.length; i++) {
            if (abortCopy) return false;
            if (!result) return false;
            result = result & copyFileStructureRecursive(srcDirContent[i], targetDirectory, withSubDirectories, errorLog);
        }
        abortSuccessful = true;
        return result;
    }

    /**
     * This method is a help method for <code>copyFileStructure</code>
     * @param sourcFile				The directory to copy from.
     * @param targetDirectory		The directory the directory/file will be copied to.
     * @param withSubDirectories	Indicates if subdirectories will be copied, too.
     * @param errorLog 				A Stringbuffer to report errors to. Could be <code>null. If
     * so, all reports or send to the standard-output.
     * @return										true, if successful.
     */
    private static boolean copyFileStructureRecursive(File sourceFile, File targetDirectory, boolean withSubDirectories, StringBuffer errorLog) {
        boolean result = true;
        if (abortCopy) return false;
        if (sourceFile.isDirectory()) {
            if (withSubDirectories) {
                File newDir = new File(targetDirectory, sourceFile.getName());
                newDir.mkdirs();
                if (sourceFile.list() != null && sourceFile.list().length > 0) {
                    File[] srcDirContent = sourceFile.listFiles();
                    for (int i = 0; i < srcDirContent.length; i++) {
                        if (abortCopy) return false;
                        if (!result) return result;
                        result = result & copyFileStructureRecursive(srcDirContent[i], newDir, withSubDirectories, errorLog);
                    }
                }
            } else {
                if (sourceFile.list() != null && sourceFile.list().length > 0) {
                    File[] srcDirContent = sourceFile.listFiles();
                    for (int i = 0; i < srcDirContent.length; i++) {
                        if (abortCopy) return false;
                        if (!result) return result;
                        result = result & copyFileStructure(srcDirContent[i], targetDirectory, withSubDirectories, errorLog);
                    }
                }
            }
        } else {
            result = result & copy2Directory(sourceFile, targetDirectory, errorLog);
        }
        return result;
    }

    private static boolean hardCopy(File sourceFile, File destinationFile, StringBuffer errorLog) {
        boolean result = true;
        try {
            notifyCopyStart(destinationFile);
            destinationFile.getParentFile().mkdirs();
            byte[] buffer = new byte[4096];
            int len = 0;
            FileInputStream in = new FileInputStream(sourceFile);
            FileOutputStream out = new FileOutputStream(destinationFile);
            while ((len = in.read(buffer)) != -1) out.write(buffer, 0, len);
            in.close();
            out.close();
        } catch (Exception e) {
            result = false;
            handleException("\n Error in method: copy!\n", e, errorLog);
        } finally {
            notifyCopyEnd(destinationFile);
        }
        return result;
    }

    /** This method sets the <code>abortCopyFlag</code> to true, indicating that the copy process has to stop. */
    public static void abortCopy() {
        abortCopy = true;
    }

    /** This method calls <code>abortCopy</code> and wait until the abort was successfull */
    public static void waitForAbort() {
        abortCopy();
        while (!abortSuccessful) {
        }
    }

    /**
     * Delete a <code>File</code> object (file or directory).
     * @param a <code>File</code> to delete (including subdirectories)
     */
    public static boolean delete(File file) {
        return delete(file, null);
    }

    /**
     * This method deletes a file / directory. If the file to be deleted is a directory all subelemens will be deleted, too.
     * @param a <code>File</code> to delete (including subdirectories)
     * @param errorLog 				A Stringbuffer to report errors to. Could be <code>null. If
     * so, all reports or send to the standard-output.
     */
    public static boolean delete(File file, StringBuffer errorLog) {
        boolean result = true;
        if (file.isDirectory()) {
            File[] fileDirContent = file.listFiles();
            if (file.list() != null && file.list().length > 0) {
                for (int i = 0; i < fileDirContent.length; i++) result = result & delete(fileDirContent[i], errorLog);
            }
            result = file.delete();
        } else {
            result = file.delete();
        }
        return result;
    }

    private static void handleException(String errorMessage, Exception e, StringBuffer errorLog) {
        if (errorLog != null) {
            errorLog.append("\n Error in method: copy!\n");
            errorLog.append(e);
            errorLog.append("\n");
        } else {
            System.out.println("\n Error in method: copy!\n");
            System.out.println(e);
            System.out.println("\n");
        }
    }

    /** Adds a copy listener. */
    public static void addFileCopyListener(FLGFileCopyListener listener) {
        if (fileCopyListeners == null) fileCopyListeners = new Vector();
        fileCopyListeners.add(listener);
    }

    /** Remove a copy listener. */
    public static boolean removeFileCopyListener(FLGFileCopyListener listener) {
        return fileCopyListeners.remove(listener);
    }

    /** Notify all listeners about the start of a new file copy. */
    private static void notifyCopyStart(File file) {
        FLGFileCopyListener listener;
        Iterator iterator = fileCopyListeners.iterator();
        while (iterator.hasNext()) {
            listener = (FLGFileCopyListener) iterator.next();
            listener.copyStarted(new FLGFileCopyEvent(file));
        }
    }

    /** Notify all listeners about the end of a file copy. */
    private static void notifyCopyEnd(File file) {
        FLGFileCopyListener listener;
        Iterator iterator = fileCopyListeners.iterator();
        while (iterator.hasNext()) {
            listener = (FLGFileCopyListener) iterator.next();
            listener.copyEnded(new FLGFileCopyEvent(file));
        }
    }

    protected static Vector fileCopyListeners = new Vector();

    private static boolean abortCopy;

    private static boolean abortSuccessful;
}
