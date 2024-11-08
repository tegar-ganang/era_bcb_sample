package hu.sztaki.lpds.storage.service.carmen.commons;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Makes stream and file changes. Creates zip streams 
 * from files or files from zip stream. (helper class)
 *
 * @author lpds
 */
public class ZipUtils {

    private static ZipUtils instance = null;

    private String sep;

    private static final int bufferSize = 4 * 1024;

    /**
 * Constructor
 */
    public ZipUtils() {
        if (instance == null) {
            instance = this;
        }
        sep = FileUtils.getInstance().getSeparator();
    }

    /**
     * Returns a ZipUtils instance.
     *
     * @return packing singleton instance
     */
    public static ZipUtils getInstance() {
        if (instance == null) {
            instance = new ZipUtils();
        }
        return instance;
    }

    /**
     * Returns the zip version number.
     * This data is only to inform that
     * the zip file was made in version 3.2.
     *
     * @return zipVersionString
     */
    public String getZipVersionString() {
        return "gUSE zip 3.2 rc4++";
    }

    /**
     * Returns the zip closing string
     *
     * @return zipEndString
     */
    public String getZipEndString() {
        return "#guse#ok";
    }

    /**
     * Returns the zip closing string in case of an error
     *
     * @return zipEndErrorString
     */
    public String getZipEndErrorString() {
        return "#guse#error";
    }

    /**
     * Writes the zipped files based on the baseDir (received in the parameters) 
     * and the hash value with the name stored in the hash key value to the stream.
     *
     * @param baseDirStr -
     *            counts the files in the oldPathName from this file
     * @param httpout -
     *            will be written here
     * @param newNamesTable -
     *            renaming and relative path descriptor hash (key: newName,
     *            value: oldPathName), if empty or null it throws
     *            exception!
     * @throws Exception
     */
    public void sendHashAllFilesToStream(String baseDirStr, OutputStream httpout, Hashtable newNamesTable) throws Exception {
        if (baseDirStr.endsWith(sep)) {
            baseDirStr = baseDirStr.substring(0, baseDirStr.length() - 1);
        }
        FileUtils.getInstance().createDirectory(baseDirStr);
        if (null == newNamesTable) {
            newNamesTable = new Hashtable();
        }
        BufferedInputStream in = null;
        byte dataBuff[] = new byte[bufferSize];
        File baseDir = new File(baseDirStr);
        if ((baseDir.exists()) && (baseDir.isDirectory())) {
            if (!newNamesTable.isEmpty()) {
                ZipOutputStream outStream = new ZipOutputStream(httpout);
                Enumeration enumFiles = newNamesTable.keys();
                while (enumFiles.hasMoreElements()) {
                    String newName = (String) enumFiles.nextElement();
                    String oldPathName = (String) newNamesTable.get(newName);
                    if ((newName != null) && (!"".equals(newName)) && (oldPathName != null) && (!"".equals(oldPathName))) {
                        String oldPathFileName = baseDirStr + sep + oldPathName;
                        if (oldPathName.startsWith(sep)) {
                            oldPathFileName = baseDirStr + oldPathName;
                        }
                        File f = new File(oldPathFileName);
                        if ((f.exists()) && (f.isFile())) {
                            System.out.println("download:" + f.getAbsolutePath());
                            in = new BufferedInputStream(new FileInputStream(oldPathFileName), bufferSize);
                            ZipEntry entry = new ZipEntry(newName);
                            outStream.putNextEntry(entry);
                            int writeLen;
                            while ((writeLen = in.read(dataBuff)) > 0) {
                                outStream.write(dataBuff, 0, writeLen);
                            }
                            outStream.flush();
                            outStream.closeEntry();
                            in.close();
                        }
                    } else {
                        throw new Exception("newNamesTable parameters wrong (null or empty) !");
                    }
                }
                outStream.finish();
                httpout.flush();
            } else {
                throw new Exception("newNamesTable is empty !");
            }
        } else {
            throw new Exception("Base (baseDirStr) dir not exist !");
        }
    }

    /**
     * Writes all of the files of the given directory zipped 
     * to the given output stream, does not use the renaming hash.
     *
     * @param FilesDirStr -
     *            all of the files from this directory (dirname/.)
     * @param httpout -
     *            output stream
     * @throws Exception
     */
    public void sendDirAllFilesToStream(String FilesDirStr, OutputStream httpout) throws Exception {
        sendDirAllFilesToStream(FilesDirStr, httpout, null);
    }

    /**
     * Writes all of the files of the given directory zipped 
     * to the given output stream.
     *
     * @param FilesDirStr -
     *            all of the files from this directory (dirname/.)
     * @param httpout -
     *            output stream
     * @param newNamesTable -
     *            renaming descriptor hash (key: oldPathName, value: newName)
     *            if null then there is no renaming given
     * @throws Exception
     */
    public void sendDirAllFilesToStream(String FilesDirStr, OutputStream httpout, Hashtable newNamesTable) throws Exception {
        if (!FilesDirStr.endsWith(sep)) {
            FilesDirStr += sep;
        }
        if (null == newNamesTable) {
            newNamesTable = new Hashtable();
        }
        FileUtils.getInstance().createDirectory(FilesDirStr);
        BufferedInputStream in = null;
        byte dataBuff[] = new byte[bufferSize];
        File filesDir = new File(FilesDirStr + ".");
        if ((filesDir.exists()) && (filesDir.isDirectory())) {
            String fileList[] = filesDir.list();
            if (fileList.length > 0) {
                ZipOutputStream outStream = new ZipOutputStream(httpout);
                for (int pos = 0; pos < fileList.length; pos++) {
                    if (!new File(FilesDirStr + fileList[pos]).isDirectory()) {
                        String oldName = fileList[pos];
                        String oldFileName = FilesDirStr + oldName;
                        String newName = oldName;
                        if (newNamesTable.containsKey(oldName)) {
                            newName = (String) newNamesTable.get(oldName);
                            if ((newName == null) && ("".equals(newName))) {
                                newName = oldName;
                            }
                        }
                        in = new BufferedInputStream(new FileInputStream(oldFileName), bufferSize);
                        ZipEntry entry = new ZipEntry(newName);
                        outStream.putNextEntry(entry);
                        int writeLen;
                        while ((writeLen = in.read(dataBuff)) > 0) {
                            outStream.write(dataBuff, 0, writeLen);
                        }
                        outStream.flush();
                        outStream.closeEntry();
                        in.close();
                    }
                }
                outStream.finish();
                httpout.flush();
            } else {
                throw new Exception("FilesDirStr is empty !");
            }
        }
    }

    /**
     * Reads the zipped file from the given input stream 
     * and unpacks it to the given directory. Does not use renaming hash.
     *
     * @param in -
     *            the zipped content comes from this stream
     * @param FilesDirStr -
     *            target directory, where the unzipped files go
     * @return - plussQuotaSize
     * @throws Exception
     */
    public long getFilesFromStream(InputStream in, String FilesDirStr) throws Exception {
        return getFilesFromStream(in, FilesDirStr, null);
    }

    /**
     * Reads the zipped file from the given input stream 
     * and unpacks it to the given directory, during the extraction 
     * checks the content of the hash given in the parameters 
     * and regarding to that content does the renaming necessary. 
     * (The return value is the total size of unpacked files in bytes)
     *
     * @param in -
     *            the zipped content comes from this stream
     * @param FilesDirStr -
     *            target directory, where the unzipped files go
     * @param newNamesTable -
     *            renaming descriptor hash (key: oldName, value: newName)
     *            if null then there is no renaming given
     * @return - plussQuotaSize
     * @throws Exception
     */
    public long getFilesFromStream(InputStream in, String FilesDirStr, Hashtable newNamesTable) throws Exception {
        long plussQuotaSize = 0;
        if (!FilesDirStr.endsWith(sep)) {
            FilesDirStr += sep;
        }
        if (null == newNamesTable) {
            newNamesTable = new Hashtable();
        }
        FileUtils.getInstance().createDirectory(FilesDirStr);
        ZipInputStream zin = null;
        zin = new ZipInputStream(new BufferedInputStream(in));
        BufferedOutputStream out = null;
        ZipEntry zipEntry = null;
        while ((zipEntry = zin.getNextEntry()) != null) {
            if (!zipEntry.isDirectory()) {
                String oldName = zipEntry.getName();
                String newName = oldName;
                String newFileName = FilesDirStr + newName;
                if (newNamesTable.containsKey(oldName)) {
                    newName = (String) newNamesTable.get(oldName);
                    if ((newName != null) && (!"".equals(newName))) {
                        newFileName = FilesDirStr + newName;
                    } else {
                        newName = oldName;
                    }
                }
                File newFile = new File(newFileName);
                FileUtils.getInstance().createDirectory(newFile.getParent());
                if (newFile.exists()) {
                    plussQuotaSize -= newFile.length();
                    newFile.delete();
                }
                out = new BufferedOutputStream(new FileOutputStream(newFile), bufferSize);
                int readLen;
                byte dataBuff[] = new byte[bufferSize];
                while ((readLen = zin.read(dataBuff)) > 0) {
                    out.write(dataBuff, 0, readLen);
                    plussQuotaSize += readLen;
                }
                out.flush();
                out.close();
            }
        }
        zin.close();
        return plussQuotaSize;
    }

    /**
     * During upload, a new runtimeID will be created from 
     * the old runtimeID and from the uploadID.
     *
     * (This method can be also found in the storage and in the wfs)
     *
     * @param runtimeID - old instance ID
     * @param uploadID - upload ID
     * @return the made new runtimeID
     */
    private String createNewRuntimeID(String runtimeID, String uploadID) {
        if ((runtimeID == null) || ("".equals(uploadID))) {
            return runtimeID;
        }
        int index = runtimeID.lastIndexOf(")upload");
        if (index >= 0) {
            return new String(runtimeID.substring(0, index).concat(")" + uploadID));
        } else {
            return new String(runtimeID).concat(uploadID);
        }
    }

    /**
     * Extracts the given file (zip file) to the given directory.
     *
     * Extraction process:
     *
     * Stores only the content (workflow) of the directory given in the EnabledDirName.
     *
     * Even if the zip is in the root directory, files (E.g.: workflow.xml)
     * will not be stored.
     *
     * (The return value is the total size of unpacked files in bytes)
     * @param EnabledDirName only this directory's content will be stored
     * @param FilesDirStr  target file path in a string
     * @param NewEnabledDirName  the content of the EnabledDirName directory will be stored with this name
     * @param in source stream
     * @param newNamesTable [no function]
     * @param uploadID will be tailed to the end of runtimeID
     * @return qutahash: the amounts and targets to increase
     * @throws Exception
     */
    public Hashtable getDirFilesOnlyFromZipStream(InputStream in, String FilesDirStr, String EnabledDirName, String NewEnabledDirName, Hashtable newNamesTable, String uploadID) throws Exception {
        Hashtable plussQuotaSizeHash = new Hashtable();
        String otherKey = "others";
        plussQuotaSizeHash.put(otherKey, new Long(0));
        if (!FilesDirStr.endsWith(sep)) {
            FilesDirStr += sep;
        }
        if (null == newNamesTable) {
            newNamesTable = new Hashtable();
        }
        ZipInputStream zin = null;
        zin = new ZipInputStream(new BufferedInputStream(in));
        BufferedOutputStream out = null;
        ZipEntry zipEntry = null;
        while ((zipEntry = zin.getNextEntry()) != null) {
            if (!zipEntry.isDirectory()) {
                String oldEnabledDirPath = new String(FilesDirStr + EnabledDirName + sep);
                String newEnabledDirPath = new String(FilesDirStr + NewEnabledDirName + sep);
                String oldName = zipEntry.getName();
                String newName = oldName;
                String newFileName = FilesDirStr + newName;
                if (newNamesTable.containsKey(oldName)) {
                    newName = (String) newNamesTable.get(oldName);
                    if ((newName != null) && (!"".equals(newName))) {
                        newFileName = FilesDirStr + newName;
                    } else {
                        newName = oldName;
                    }
                }
                newFileName = replaceFirst(newFileName, oldEnabledDirPath, newEnabledDirPath);
                File newFile = new File(newFileName);
                boolean storeThisFile = true;
                String runtimeID = new String("");
                if (newFile.getParentFile().getParent().endsWith("outputs")) {
                    runtimeID = newFile.getParentFile().getName();
                    String newRuntimeID = createNewRuntimeID(runtimeID, uploadID);
                    newFileName = replaceFirst(newFileName, runtimeID, newRuntimeID);
                    runtimeID = newRuntimeID;
                    newFile = new File(newFileName);
                    if (!plussQuotaSizeHash.containsKey(runtimeID)) {
                        plussQuotaSizeHash.put(runtimeID, new Long(0));
                    }
                }
                if (!newFileName.startsWith(newEnabledDirPath)) {
                    storeThisFile = false;
                }
                if (storeThisFile) {
                    String hashKey = otherKey;
                    if (!"".equals(runtimeID)) {
                        hashKey = runtimeID;
                    }
                    long plussQuotaSize = ((Long) plussQuotaSizeHash.get(hashKey)).longValue();
                    FileUtils.getInstance().createDirectory(newFile.getParent());
                    if (newFile.exists()) {
                        plussQuotaSize -= newFile.length();
                        newFile.delete();
                    }
                    out = new BufferedOutputStream(new FileOutputStream(newFile), bufferSize);
                    int readLen;
                    byte dataBuff[] = new byte[bufferSize];
                    while ((readLen = zin.read(dataBuff)) > 0) {
                        out.write(dataBuff, 0, readLen);
                        plussQuotaSize += readLen;
                    }
                    out.flush();
                    out.close();
                    plussQuotaSizeHash.put(hashKey, plussQuotaSize);
                } else {
                    int readLen;
                    byte dataBuff[] = new byte[bufferSize];
                    while ((readLen = zin.read(dataBuff)) > 0) {
                    }
                }
            }
        }
        zin.close();
        return plussQuotaSizeHash;
    }

    /**
     * Reads the content of the searched file from the given zip input stream
     * (does not save it) and returns it in a string.
     *
     * @param in -
     *            the zipped content comes from this stream
     * @param EntryFileName -
     *            the searched file
     * @return string - the content of the searched file
     * @throws Exception
     */
    public String getFilesStringFromZipStream(InputStream in, String EntryFileName) throws Exception {
        String fileValueString = new String("");
        ZipInputStream zin = null;
        zin = new ZipInputStream(new BufferedInputStream(in));
        ByteArrayOutputStream out = null;
        ZipEntry zipEntry = null;
        while ((zipEntry = zin.getNextEntry()) != null) {
            if (!zipEntry.isDirectory()) {
                String EntryName = zipEntry.getName();
                if (EntryFileName.equals(EntryName)) {
                    out = new ByteArrayOutputStream();
                    int readLen;
                    byte dataBuff[] = new byte[bufferSize];
                    while ((readLen = zin.read(dataBuff)) > 0) {
                        out.write(dataBuff, 0, readLen);
                    }
                    out.flush();
                    out.close();
                    zin.close();
                    if ("".equals(fileValueString)) {
                        fileValueString = out.toString();
                        return fileValueString;
                    }
                } else {
                    int readLen;
                    byte dataBuff[] = new byte[bufferSize];
                    while ((readLen = zin.read(dataBuff)) > 0) {
                    }
                }
            }
        }
        zin.close();
        return fileValueString;
    }

    /**
     * Extracts the given file (zip file) to the given directory.
     * (Return value is the total size of the unzipped files in bytes.)
     *
     * @param zFile -
     *            file to be extracted
     * @param FilesDirStr -
     *            target directory
     * @return - plussQuotaSize
     * @throws Exception
     */
    public long unZipFile(File zFile, String FilesDirStr) throws Exception {
        return getFilesFromStream(new FileInputStream(zFile), FilesDirStr, null);
    }

    /**
     * Extracts the given file (zip file) to the given directory.
     *
     * Extraction process:
     *
     * Stores only the content (workflow) of the directory given in the EnabledDirName.
     *
     * Even if the zip is in the root directory, files (E.g.: workflow.xml)
     * will not be stored.
     *
     * (The return value is the total size of unpacked files in bytes)
     *
     * @param zFile -
     *            file to be extracted
     * @param FilesDirStr -
     *            where the files will be stored
     * @param EnabledDirName -
     *            only the content of this directory will be stored
     * @param NewEnabledDirName -
     *            this will be the name of the content of the EnabledDirName directory
     * @param uploadID -
     *            it will be tailed to the end of the runtimeID
     * @return - plussQuotaSizeHash
     * @throws Exception
     */
    public Hashtable unZipWorkflowZipFileNotStoreRoot(File zFile, String FilesDirStr, String EnabledDirName, String NewEnabledDirName, String uploadID) throws Exception {
        return getDirFilesOnlyFromZipStream(new FileInputStream(zFile), FilesDirStr, EnabledDirName, NewEnabledDirName, null, uploadID);
    }

    /**
     * Finds and returns the content of the entry file with 
     * the given name from the zipped file, in a string.
     *
     * (does not create a file)
     *
     * The content of the zip file is a real and an abstract workflow.
     *
     * (The return value is the content of the file in a string)
     *
     * @param zFile -
     *            zipped file
     * @param EntryFileName -
     *            the searched file (E.g.: workflow.xml)
     * @return the content of the requested file -
     *            in a string.
     * @throws Exception
     */
    public String getStringFromZipFile(File zFile, String EntryFileName) throws Exception {
        return getFilesStringFromZipStream(new FileInputStream(zFile), EntryFileName);
    }

    /**
     * Extracts the given zip file to the given directory.
     * During the extraction there are some renaming regarding 
     * to the content of the hash received in the parameters.
     * The return value is the total size of the extracted files.
     *
     * @param zFile -
     *            file to be extracted
     * @param FilesDirStr -
     *            files will be extracted to here
     * @param newNamesTable -
     *            (key: newName, value: oldName)
     * @return - plussQuotaSize
     * @throws Exception
     */
    public long unZipFile(File zFile, String FilesDirStr, Hashtable newNamesTable) throws Exception {
        return getFilesFromStream(new FileInputStream(zFile), FilesDirStr, newNamesTable);
    }

    /**
     * Writes the given file to the given stream (zipped).
     *
     * @param userbaseDir user directory
     * @param fileStr file access
     * @param zipout zipStream
     * @throws Exception compressing error
     */
    public void sendFileToZipStream(String userbaseDir, String fileStr, ZipOutputStream zipout) throws Exception {
        if (!userbaseDir.endsWith(sep)) {
            userbaseDir += sep;
        }
        BufferedInputStream in = null;
        byte dataBuff[] = new byte[bufferSize];
        String entryPath = fileStr;
        String entryFullPath = userbaseDir + entryPath;
        File sendFile = new File(entryFullPath);
        if ((sendFile.exists()) && (!sendFile.isDirectory())) {
            in = new BufferedInputStream(new FileInputStream(entryFullPath), bufferSize);
            ZipEntry zipEntry = new ZipEntry(entryPath);
            zipout.putNextEntry(zipEntry);
            int writeLen;
            while ((writeLen = in.read(dataBuff)) > 0) {
                zipout.write(dataBuff, 0, writeLen);
            }
            zipout.flush();
            zipout.closeEntry();
            in.close();
        } else {
            throw new Exception("file is not exist ! entryFullPath = (" + entryFullPath + ")");
        }
    }

    /**
     * Writes the received string with the given entry name to the given stream. (zipped)
     *
     * @param userbaseDir
     * @param fileStr
     * @param fileValueString
     * @param zipout
     * @throws Exception
     */
    public void sendStringToZipStream(String userbaseDir, String fileStr, String fileValueString, ZipOutputStream zipout) throws Exception {
        if (!userbaseDir.endsWith(sep)) {
            userbaseDir += sep;
        }
        BufferedInputStream in = null;
        byte dataBuff[] = new byte[bufferSize];
        String entryPath = fileStr;
        in = new BufferedInputStream(new ByteArrayInputStream(fileValueString.getBytes()), bufferSize);
        ZipEntry zipEntry = new ZipEntry(entryPath);
        zipout.putNextEntry(zipEntry);
        int writeLen;
        while ((writeLen = in.read(dataBuff)) > 0) {
            zipout.write(dataBuff, 0, writeLen);
        }
        zipout.flush();
        zipout.closeEntry();
        in.close();
    }

    /**
     * Writes every files of the given input directory zipped 
     * to the given zip output stream. The stream stays open.
     *
     * @param userbaseDir -
     *            from this directory
     * @param FilesDirStr -
     *            every files and sub-directories from this directory
     * @param zipout -
     *            files will be written to here
     * @throws Exception
     */
    public void sendInputDirAllFilesToZipStream(String userbaseDir, String FilesDirStr, ZipOutputStream zipout) throws Exception {
        sendDirAllFilesToZipStream(userbaseDir, FilesDirStr, zipout);
    }

    /**
     * Writes every files of the given output directory zipped 
     * to the given zip output stream. The stream stays open.
     *
     * @param userbaseDir -
     *            from this directory
     * @param FilesDirStr -
     *            every files and sub-directories from this directory
     * @param zipout -
     *            files will be written to here
     * @param outputLogType -
     *            (all - every log file, or none - non of the log files)
     *            go to the zip output stream
     * @throws Exception
     */
    public void sendOutputDirAllFilesToZipStream(String userbaseDir, String FilesDirStr, ZipOutputStream zipout, String outputLogType) throws Exception {
        if (!userbaseDir.endsWith(sep)) {
            userbaseDir += sep;
        }
        if (!FilesDirStr.endsWith(sep)) {
            FilesDirStr += sep;
        }
        BufferedInputStream in = null;
        byte dataBuff[] = new byte[bufferSize];
        File filesDir = new File(userbaseDir + FilesDirStr + ".");
        if ((filesDir.exists()) && (filesDir.isDirectory())) {
            String fileList[] = filesDir.list();
            if (fileList.length > 0) {
                for (int pos = 0; pos < fileList.length; pos++) {
                    String entry = fileList[pos];
                    String entryPath = FilesDirStr + entry;
                    String entryFullPath = userbaseDir + entryPath;
                    if (!new File(entryFullPath).isDirectory()) {
                        boolean sendFileToZip = true;
                        if (FileUtils.getInstance().isLogFileName(entry)) {
                            if ("none".equals(outputLogType)) {
                                sendFileToZip = false;
                            }
                        }
                        if (FileUtils.getInstance().isLinkFile(entryFullPath)) {
                            sendFileToZip = false;
                        }
                        if (sendFileToZip) {
                            String entry2 = FileUtils.getInstance().getNormalFromPidName(entry);
                            String entryPath2 = FilesDirStr + entry2;
                            in = new BufferedInputStream(new FileInputStream(entryFullPath), bufferSize);
                            ZipEntry zipEntry = new ZipEntry(entryPath2);
                            zipout.putNextEntry(zipEntry);
                            int writeLen;
                            while ((writeLen = in.read(dataBuff)) > 0) {
                                zipout.write(dataBuff, 0, writeLen);
                            }
                            zipout.flush();
                            zipout.closeEntry();
                            in.close();
                        }
                    } else {
                        sendOutputDirAllFilesToZipStream(userbaseDir, entryPath, zipout, outputLogType);
                    }
                }
            } else {
            }
        } else {
        }
    }

    /**
     * Writes every output files of the given job output directory zipped 
     * to the given zip output stream. The stream stays open.
     * 
     * (considers the value of the outputLogType)
     *
     * @param userbaseDir -
     *            from this directory
     * @param FilesDirStr -
     *            every files and sub-directories from this directory
     * @param zipout -
     *            files will be written to here
     * @param outputLogType -
     *            (all - every log file, or none - non of the log files)
     *            go to the zip output stream
     * @param pidID -
     *            pid ID (pl: rtID/12/output, this case the 12 is the pid)
     * @throws Exception
     */
    public void sendJobOutputDirPidFilesToZipStream(String userbaseDir, String FilesDirStr, ZipOutputStream zipout, String outputLogType, String pidID) throws Exception {
        System.out.println(userbaseDir + FilesDirStr);
        if (!userbaseDir.endsWith(sep)) {
            userbaseDir += sep;
        }
        if (!FilesDirStr.endsWith(sep)) {
            FilesDirStr += sep;
        }
        BufferedInputStream in = null;
        byte dataBuff[] = new byte[bufferSize];
        File filesDir = new File(userbaseDir + FilesDirStr + ".");
        if ((filesDir.exists()) && (filesDir.isDirectory())) {
            String fileList[] = filesDir.list();
            if (fileList.length > 0) {
                for (int pos = 0; pos < fileList.length; pos++) {
                    String entry = fileList[pos];
                    String entryPath = FilesDirStr + entry;
                    String entryFullPath = userbaseDir + entryPath;
                    if (new File(entryFullPath).isFile()) {
                        boolean sendFileToZip = true;
                        if (FileUtils.getInstance().isLogFileName(entry)) {
                            if ("none".equals(outputLogType)) {
                                sendFileToZip = false;
                            }
                        }
                        if (FileUtils.getInstance().isLinkFile(entryFullPath)) {
                            sendFileToZip = false;
                        }
                        if (sendFileToZip) {
                            String entry2 = FileUtils.getInstance().getNormalFromPidName(entry);
                            String entryPath2 = FilesDirStr + entry2;
                            in = new BufferedInputStream(new FileInputStream(entryFullPath), bufferSize);
                            ZipEntry zipEntry = new ZipEntry(entryPath2);
                            zipout.putNextEntry(zipEntry);
                            int writeLen;
                            while ((writeLen = in.read(dataBuff)) > 0) {
                                zipout.write(dataBuff, 0, writeLen);
                            }
                            zipout.flush();
                            zipout.closeEntry();
                            in.close();
                        }
                    }
                }
            } else {
            }
        } else {
        }
    }

    /**
     * Writes every files of the given directory zipped 
     * to the given zip output stream. The stream stays open.
     *
     * @param userbaseDir -
     *            from this directory
     * @param FilesDirStr -
     *            every files and sub-directories from this directory
     * @param zipout -
     *            files will be written to here
     * @throws Exception
     */
    private void sendDirAllFilesToZipStream(String userbaseDir, String FilesDirStr, ZipOutputStream zipout) throws Exception {
        if (!userbaseDir.endsWith(sep)) {
            userbaseDir += sep;
        }
        if (!FilesDirStr.endsWith(sep)) {
            FilesDirStr += sep;
        }
        BufferedInputStream in = null;
        byte dataBuff[] = new byte[bufferSize];
        File filesDir = new File(userbaseDir + FilesDirStr + ".");
        if ((filesDir.exists()) && (filesDir.isDirectory())) {
            String fileList[] = filesDir.list();
            if (fileList.length > 0) {
                for (int pos = 0; pos < fileList.length; pos++) {
                    String entry = fileList[pos];
                    String entryPath = FilesDirStr + entry;
                    String entryFullPath = userbaseDir + entryPath;
                    if (!new File(entryFullPath).isDirectory()) {
                        in = new BufferedInputStream(new FileInputStream(entryFullPath), bufferSize);
                        ZipEntry zipEntry = new ZipEntry(entryPath);
                        zipout.putNextEntry(zipEntry);
                        int writeLen;
                        while ((writeLen = in.read(dataBuff)) > 0) {
                            zipout.write(dataBuff, 0, writeLen);
                        }
                        zipout.flush();
                        zipout.closeEntry();
                        in.close();
                    } else {
                        sendDirAllFilesToZipStream(userbaseDir, entryPath, zipout);
                    }
                }
            } else {
            }
        } else {
        }
    }

    /**
     * Writes the name of the given workflow directory 
     * to the given zip stream. The stream stays open.
     *
     * @param userbaseDir -
     *            from this directory
     * @param DirStr -
     *            name of the directory
     * @param zipout -
     *            files will be written to here
     * @throws Exception
     */
    public void sendDirNameToZipStream(String userbaseDir, String DirStr, ZipOutputStream zipout) throws Exception {
        if (!userbaseDir.endsWith(sep)) {
            userbaseDir += sep;
        }
        if (!DirStr.endsWith(sep)) {
            DirStr += sep;
        }
        String entryPath = userbaseDir + DirStr;
        File dir = new File(entryPath);
        dir.mkdirs();
        if ((dir.exists()) && (dir.isDirectory())) {
            ZipEntry zipEntry = new ZipEntry(DirStr);
            zipEntry.setMethod(ZipEntry.STORED);
            zipEntry.setSize(0);
            zipEntry.setCrc(0);
            zipout.putNextEntry(zipEntry);
            zipout.flush();
            zipout.closeEntry();
        } else {
        }
    }

    /**
     * If the file received in the parameters is a zip file and it contains a workflow
     * then the return value is the name (the directory) of the workflow.
     * The first directory name will be returned from the zip.
     *
     * (This will be the name of the real workflow directory)
     *
     * @param zipFile -
     *            workflow zip file
     * @throws Exception
     * @return workflow name
     */
    public String getWorkflowNameFromZipFile(File zipFile) throws Exception {
        String workflowID = new String("");
        ZipInputStream zin = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)));
        ZipEntry zipEntry = null;
        while ((zipEntry = zin.getNextEntry()) != null) {
            if (zipEntry.isDirectory()) {
                String zipEntryName = zipEntry.getName();
                if ("".equals(workflowID)) {
                    workflowID = zipEntryName;
                }
            }
        }
        zin.close();
        return workflowID;
    }

    /**
     * Extracts the given file (zip file) to the given directory.
     * (Return value is the total size of the extracted files)
     *
     * (Only the files from zip base, root directory will be extracted)
     * (Sub-directories and their content will not be extracted)
     * (It is advised to be used to extract the parametric inputs after upload)
     *
     * @param zFile -
     *            file to be extracted
     * @param FilesDirStr -
     *            files will be extracted to here
     * @return - plussQuotaSize
     * @throws Exception
     */
    public long unZipFileOnlyRootFiles(File zFile, String FilesDirStr) throws Exception {
        return getFilesFromStreamOnlyRootFiles(new FileInputStream(zFile), FilesDirStr, null);
    }

    /**
     * Reads the zipped file from the given input stream and extracts it to the
     * given directory. During the extraction there are some renaming regarding 
     * to the content of the hash received in the parameters.
     * The return value is the total size of the extracted files.
     *
     * (Only the files from zip base, root directory will be extracted)
     * (Sub-directories and their content will not be extracted)
     * (It is advised to be used to extract the parametric inputs after upload)
     *
     * @param in -
     *            zipped content comes from this stream
     * @param FilesDirStr -
     *            extracted files are here
     * @param newNamesTable -
     *            renaming descriptor hash (key: oldName, value: newName) 
     *            if null then there is no renaming
     * @return - plussQuotaSize
     * @throws Exception
     */
    public long getFilesFromStreamOnlyRootFiles(InputStream in, String FilesDirStr, Hashtable newNamesTable) throws Exception {
        long plussQuotaSize = 0;
        if (!FilesDirStr.endsWith(sep)) {
            FilesDirStr += sep;
        }
        if (null == newNamesTable) {
            newNamesTable = new Hashtable();
        }
        FileUtils.getInstance().createDirectory(FilesDirStr);
        ZipInputStream zin = null;
        zin = new ZipInputStream(new BufferedInputStream(in));
        BufferedOutputStream out = null;
        ZipEntry zipEntry = null;
        while ((zipEntry = zin.getNextEntry()) != null) {
            if (!zipEntry.isDirectory()) {
                String zipEntryName = zipEntry.getName();
                String oldName = zipEntryName;
                String newName = oldName;
                String newFileName = FilesDirStr + newName;
                if (newNamesTable.containsKey(oldName)) {
                    newName = (String) newNamesTable.get(oldName);
                    if ((newName != null) && (!"".equals(newName))) {
                        newFileName = FilesDirStr + newName;
                    } else {
                        newName = oldName;
                    }
                }
                boolean rootFile = true;
                if (new File(zipEntryName).getParentFile() != null) {
                    rootFile = false;
                }
                File newFile = new File(newFileName);
                if (rootFile) {
                    FileUtils.getInstance().createDirectory(newFile.getParent());
                    if (newFile.exists()) {
                        plussQuotaSize -= newFile.length();
                        newFile.delete();
                    }
                    out = new BufferedOutputStream(new FileOutputStream(newFile), bufferSize);
                    int readLen;
                    byte dataBuff[] = new byte[bufferSize];
                    while ((readLen = zin.read(dataBuff)) > 0) {
                        out.write(dataBuff, 0, readLen);
                        plussQuotaSize += readLen;
                    }
                    out.flush();
                    out.close();
                } else {
                    byte dataBuff[] = new byte[bufferSize];
                    while ((zin.read(dataBuff)) > 0) {
                    }
                }
            }
        }
        zin.close();
        return plussQuotaSize;
    }

    /**
     * Returns a unique zip file name.
     *
     * @return file name
     */
    public String getUniqueZipFileName() {
        Long randomNum = new Long(Math.round(Math.random() * 900000) + 100000);
        return new String("ZipFile-4815162342-AbB03x-" + randomNum + ".zip");
    }

    /**
     * Checks the given parameter input file (zip file).
     *
     * If the content of the zip file is eligible then it returns an empty string,
     * else the return value is an error message.
     *
     * (Only the files from zip base, root directory will be checked)
     * (Sub-directories and their content will not be checked)
     * (It is advised to be used to check the parametric inputs after upload)
     *
     * @param zFile the parameter zip file to be checked
     * @param fileName  zip file name
     * @param jobID job name
     * @param sfile  source file name
     * @return - error string
     * @throws Exception
     */
    public String checkParamInputZipFile(File zFile, String fileName, String jobID, String sfile) throws Exception {
        String zipName = fileName;
        String jobName = jobID;
        String portName = "";
        if (sfile.contains("_")) {
            String fileType = sfile.split("_")[0];
            portName = sfile.split("_")[1];
        }
        String retErrStr = new String("OK");
        ArrayList fileNameList = getFileNamesFromStreamOnlyRootFiles(new FileInputStream(zFile));
        if (fileNameList.isEmpty()) {
            retErrStr = new String("Error: not valid param inputs zip file, job name = " + jobName + ", port name = " + portName + ".");
        } else {
            boolean errorInList = checkParamInputNamesArrayList(fileNameList);
            if (errorInList) {
                retErrStr = new String("Error in " + zipName + " file, job name = " + jobName + ", port name = " + portName + ".");
            }
        }
        return retErrStr;
    }

    /**
     * Reads the zipped file names from the given input stream.
     *
     * (The zip file will not be extracted)
     * (Only the names of the files from zip base, root directory will be returned)
     * (Sub-directories and their content will not be checked)
     * (It is advised to be used to check the parametric inputs before extraction)
     *
     * @param in -
     *            the zipped content comes from this stream
     * @return - file names
     * @throws Exception
     */
    private ArrayList getFileNamesFromStreamOnlyRootFiles(InputStream in) throws Exception {
        ZipInputStream zin = null;
        ArrayList fileNameList = new ArrayList();
        zin = new ZipInputStream(new BufferedInputStream(in));
        ZipEntry zipEntry = null;
        while ((zipEntry = zin.getNextEntry()) != null) {
            if (!zipEntry.isDirectory()) {
                String zipEntryName = zipEntry.getName();
                boolean rootFile = true;
                if (new File(zipEntryName).getParentFile() != null) {
                    rootFile = false;
                }
                if (rootFile) {
                    fileNameList.add(zipEntryName);
                    byte dataBuff[] = new byte[bufferSize];
                    while ((zin.read(dataBuff)) > 0) {
                    }
                } else {
                    byte dataBuff[] = new byte[bufferSize];
                    while ((zin.read(dataBuff)) > 0) {
                    }
                }
            }
        }
        zin.close();
        return fileNameList;
    }

    /**
     * Checks the given list of files names
     * if they are eligible to the following conditions.
     *
     * Conditions: It starts from 0 and the numbers of the list elements ends with -1.
     *
     * @param list - file names (number list)
     * @return - not correct list,
     *           false if the names are eligible to the conditions
     *           else true
     * @throws Exception
     */
    private boolean checkParamInputNamesArrayList(ArrayList list) throws Exception {
        ArrayList listDel = new ArrayList();
        listDel.addAll(list);
        for (int iPos = 0; iPos < list.size(); iPos++) {
            String entry = String.valueOf(iPos);
            if (list.indexOf(entry) != -1) {
                listDel.remove(entry);
            } else {
                return true;
            }
        }
        if (listDel.size() > 0) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Sub string change
     *
     * @param str - original string
     * @param oldStr - string to be changed
     * @param newStr - new string
     * @return original string with the changed content
     */
    private String replaceFirst(String str, String oldStr, String newStr) {
        int index = str.indexOf(oldStr);
        if (index > -1) {
            String partBefore = str.substring(0, index);
            String partAfter = str.substring(index + oldStr.length());
            return partBefore + newStr + partAfter;
        } else {
            return str;
        }
    }

    /**
     * Gets the name of the workflow from the content of the zip.
     *
     * (The return value is the name of the 
     * first level (root dir) directory in the zip.)
     *
     * @param zipFile - zip file to be examined
     * @return String workflow name
     * @throws Exception
     */
    public String getBaseDirNameFromZipFile(File zipFile) throws Exception {
        String wfNameString = new String("");
        ZipInputStream zin = null;
        zin = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)));
        ZipEntry zipEntry = null;
        while (((zipEntry = zin.getNextEntry()) != null) && ("".equals(wfNameString))) {
            if (zipEntry != null) {
                String entryName = zipEntry.getName();
                String str = entryName.split(sep)[0];
                if (str.trim().length() > 0) {
                    wfNameString = str;
                }
            }
        }
        zin.close();
        if ("".equals(wfNameString)) {
            throw new Exception("not valid zip file !!!");
        }
        return wfNameString;
    }
}
