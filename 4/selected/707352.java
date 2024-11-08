package net.sourceforge.jaulp.file;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;
import net.sourceforge.jaulp.date.DateUtils;

public class RenameFileUtils {

    /**
     * Returns the filename from the given file with the systemtime.
     * 
     * @param fileToRename
     *            The file.
     * @return Returns the filename from the given file with the systemtime.
     */
    public static String appendSystemtimeToFilename(File fileToRename) {
        return appendSystemtimeToFilename(fileToRename, null);
    }

    /**
     * Returns the filename from the given file with the systemtime.
     * 
     * @param fileToRename
     *            The file.
     * @param add2Name
     *            Adds the Date to the Filename.
     * @return Returns the filename from the given file with the systemtime.
     */
    public static String appendSystemtimeToFilename(File fileToRename, Date add2Name) {
        String format = "HHmmssSSS";
        String sysTime = null;
        if (null != add2Name) {
            sysTime = DateUtils.parseToString(add2Name, format);
        } else {
            sysTime = DateUtils.parseToString(new Date(), format);
        }
        String fileName = fileToRename.getName();
        int ext_index = fileName.lastIndexOf(".");
        String ext = fileName.substring(ext_index, fileName.length());
        String newName = fileName.substring(0, ext_index);
        newName += "_" + sysTime + ext;
        return newName;
    }

    /**
     * Changes all the Filenames with the new Suffix recursively.
     * 
     * @param file
     *            The file where to change the Filename with the new Suffix.
     * @param oldSuffix
     *            All files that have the old suffix will be renamed with the
     *            new Suffix.
     * @param newSuffix
     *            The new suffix.
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     * @throws FileDoesNotExistException
     *             If the file does not exist.
     */
    public static List changeAllFilenameSuffix(File file, String oldSuffix, String newSuffix) throws IOException, FileDoesNotExistException {
        return changeAllFilenameSuffix(file, oldSuffix, newSuffix, false);
    }

    /**
     * Changes all the Filenames with the new Suffix recursively. If delete is
     * true its deletes the existing file with the same name.
     * 
     * @param file
     *            The file where to change the Filename with the new Suffix.
     * @param oldSuffix
     *            All files that have the old suffix will be renamed with the
     *            new Suffix.
     * @param newSuffix
     *            The new suffix.
     * @param delete
     *            If its true than its deletes the existing file with the same
     *            name. But before it copys the contents into the new File.
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     * @throws FileDoesNotExistException
     *             If the file does not exist.
     */
    public static List changeAllFilenameSuffix(File file, String oldSuffix, String newSuffix, boolean delete) throws IOException, FileDoesNotExistException {
        boolean success;
        List notDeletedFiles = null;
        String filePath = file.getAbsolutePath();
        String suffix[] = { oldSuffix };
        Vector files = FileUtils.findFiles(filePath, suffix);
        int fileCount = files.size();
        for (int i = 0; i < fileCount; i++) {
            File currentFile = (File) files.elementAt(i);
            try {
                success = FileUtils.changeFilenameSuffix(currentFile, newSuffix, delete);
            } catch (FileDoesNotExistException e) {
                success = false;
            }
            if (!success) {
                if (null != notDeletedFiles) {
                    notDeletedFiles.add(currentFile);
                } else {
                    notDeletedFiles = new ArrayList();
                    notDeletedFiles.add(currentFile);
                }
            }
        }
        return notDeletedFiles;
    }

    /**
     * Changes the suffix from the Filename. Example: test.dat to test.xxx
     * 
     * @param file
     *            The file to change.
     * @param newSuffix
     *            The new suffix. You must start with a dot. For instance: .xxx
     * @return true if the file was renamed.
     * @throws FileNotRenamedException
     *             If the file could not renamed.
     * @throws FileDoesNotExistException
     *             If the file does not exist.
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static boolean changeFilenameSuffix(File file, String newSuffix) throws FileNotRenamedException, FileDoesNotExistException, IOException {
        return changeFilenameSuffix(file, newSuffix, false);
    }

    /**
     * Changes the suffix from the Filename. Example: test.dat to test.xxx
     * 
     * @param file
     *            The file to change.
     * @param newSuffix
     *            The new suffix. You must start with a dot. For instance: .xxx
     * @param delete
     *            If its true than its deletes the existing file with the same
     *            name. But before it copys the contents into the new File.
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     * @throws FileDoesNotExistException
     *             If the file does not exist.
     */
    public static boolean changeFilenameSuffix(File file, String newSuffix, boolean delete) throws IOException, FileDoesNotExistException {
        if (!file.exists()) {
            String error = "The " + file + " does not exists.";
            throw new FileDoesNotExistException(error);
        }
        String fileNamePrefix = FileUtils.getFilenamePrefix(file);
        String newFilename = fileNamePrefix + newSuffix;
        File file2 = new File(newFilename);
        boolean success = FileUtils.renameFile(file, file2, delete);
        return success;
    }

    /**
     * This method renames a given file. For instance if we have a file which we
     * want to rename with the path "/tmp/test.dat" to "/tmp/renamed.dat" then
     * you call the method as follow: renameFile(new File("C://tmp//test.dat"),
     * new File("C://tmp//renamed.dat"));
     * 
     * @param fileToRename
     *            The file to rename.
     * @param newFileName
     *            The new name from the file.
     * @return 's true if the file was renamed otherwise false.
     */
    public static boolean renameFile(File fileToRename, File newFileName) {
        return renameFile(fileToRename, newFileName, false);
    }

    /**
     * This method renames a given file. For instance if we have a file which we
     * want to rename with the path "/tmp/test.dat" to "/tmp/renamed.dat" then
     * you call the method as follow: renameFile(new File("C://tmp//test.dat"),
     * new File("C://tmp//renamed.dat"));
     * 
     * @param fileToRename
     *            The file to rename.
     * @param newFileName
     *            The new name from the file.
     * @param delete
     *            If true an attempt to copy the content from the file to rename
     *            to the new file and then delete the file to rename otherwise
     *            not.
     * @return 's true if the file was renamed otherwise false.
     */
    public static boolean renameFile(File fileToRename, File newFileName, boolean delete) {
        boolean success = fileToRename.renameTo(newFileName);
        if (!success) {
            System.err.println("The file " + fileToRename.getName() + " was not renamed.");
            if (delete) {
                System.err.println("Try to delete the old file and " + "copy the content into the new file with the new name.");
                try {
                    FileUtils.copyFile(fileToRename, newFileName);
                    FileUtils.delete(fileToRename);
                    success = true;
                } catch (IOException e) {
                    System.err.println("The file " + fileToRename.getName() + " was not renamed." + " Failed to copy the old file into the new File.");
                }
            }
        }
        return success;
    }

    /**
     * This method renames a given file. For instance if we have a file which we
     * want to rename with the path "/tmp/test.dat" to "/tmp/renamed.dat" then
     * you call the method as follow: renameFile(new File("C://tmp//test.dat"),
     * new File("C://tmp//renamed.dat"));
     * 
     * @param fileToRename
     *            The file to rename.
     * @param newFileNameWithoutAbsolutPath
     *            The new name from the file.
     * @return 's true if the file was renamed otherwise false.
     */
    public static boolean renameFile(File fileToRename, String newFileNameWithoutAbsolutPath) {
        if (!fileToRename.exists()) {
            try {
                throw new FileDoesNotExistException("File" + fileToRename.getName() + " does not exists!");
            } catch (FileDoesNotExistException e) {
                e.printStackTrace();
            }
            return false;
        }
        String fileNameAbsolutPathPrefix = FileUtils.getAbsolutPathWithoutFilename(fileToRename);
        StringBuffer sb = new StringBuffer();
        sb.append(fileNameAbsolutPathPrefix);
        sb.append(newFileNameWithoutAbsolutPath);
        File newNameForFile = new File(sb.toString());
        boolean sucess = renameFile(fileToRename, newNameForFile);
        return sucess;
    }

    /**
     * Renames the given file and add to the filename the systemtime.
     * 
     * @param fileToRename
     *            The file to rename.
     * @return Returns the renamed file from the given file with the systemtime.
     */
    public static File renameFileWithSystemtime(File fileToRename) {
        String newFilenameWithSystemtime = appendSystemtimeToFilename(fileToRename);
        File fileWithNewName = new File(fileToRename.getParent(), newFilenameWithSystemtime);
        renameFile(fileToRename, fileWithNewName, true);
        return fileWithNewName;
    }
}
