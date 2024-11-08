package hu.sztaki.lpds.storage.service.carmen.commons;

import hu.sztaki.lpds.information.local.PropertyLoader;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Implements file and directory handling. (helper class)
 *
 * @author lpds
 */
public class FileUtils {

    private static FileUtils instance = null;

    private static final int bufferSize = 4 * 1024;

    private String sep;

    private String repositoryDir;

    private String storageURL;

    private String defaultBinaryName;

    private String defaultWrapperName;

    private ArrayList logNamePrefixList;

    private int delCntMax = 500;

    /**
 * Constructor
 * parameters settings
 */
    public FileUtils() {
        sep = System.getProperty("file.separator");
        if (sep == null) {
            sep = "/";
        }
        repositoryDir = PropertyLoader.getInstance().getProperty("prefix.dir");
        if ((repositoryDir == null) || (repositoryDir.trim().equals(""))) {
            repositoryDir = PropertyLoader.getInstance().getProperty("portal.prefix.dir");
        }
        if ((repositoryDir == null) || (repositoryDir.trim().equals(""))) {
            repositoryDir = System.getProperty("java.io.tmpdir");
        }
        if (!repositoryDir.endsWith(sep)) {
            repositoryDir += sep;
        }
        repositoryDir += "storage/";
        storageURL = PropertyLoader.getInstance().getProperty("service.url");
        defaultBinaryName = "execute.bin";
        defaultWrapperName = "wrapper.sh";
        this.setUpLogNamePrefixList();
    }

    /**
     * Returns a FileUtils instance.
     *
     * @return
     */
    public static FileUtils getInstance() {
        if (instance == null) {
            instance = new FileUtils();
        }
        return instance;
    }

    /**
     * Returns the file separator value.
     *
     * @return
     */
    public String getSeparator() {
        return sep;
    }

    /**
     * Returns the storage repository directory value.
     *
     * @return
     */
    public String getRepositoryDir() {
        return repositoryDir;
    }

    /**
     * Returns the default binary name.
     *
     * (execute.bin)
     *
     * @return
     */
    public String getDefaultBinaryName() {
        return defaultBinaryName;
    }

    /**
     * Returns the default wrapper name.
     *
     * (wrapper.sh)
     *
     * @return string
     */
    public String getDefaultWrapperName() {
        return defaultWrapperName;
    }

    /**
     * Returns the storage service URL value.
     *
     * (note: not used in local mode !)
     *
     * (pl: portal sends the zip to the storage in local mode)
     * (because in this case the 
     * return value is the portal serviceURl)
     *
     * @return
     */
    public String getStorageUrl() {
        return storageURL;
    }

    /**
     * Creates the repository directory.
     */
    public void createRepositoryDirectory() {
        try {
            this.createDirectory(this.repositoryDir);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates the given directory. (and all of the parent directories)
     *
     * @param dirNameStr -
     *            directory name
     * @throws Exception
     */
    public void createDirectory(String dirNameStr) throws Exception {
        File repositoryDirectory = new File(dirNameStr);
        repositoryDirectory.mkdirs();
        if (!repositoryDirectory.canWrite()) {
            throw new IllegalArgumentException("Not writable: " + dirNameStr);
        }
    }

    /**
     * Deletes the given directory, the subdirectories and all the files
     * in them. (every content and the empty directory will be deleted too)
     * (return value in bytes is the total size of the deleted files)
     *
     * @param dirNameStr
     * @return - plussQuotaSize
     * @throws Exception
     */
    public long deleteDirectory(String dirNameStr) throws Exception {
        return this.deleteDirectory(dirNameStr, true);
    }

    /**
     * Uploads the list of the log file names.
     *
     * A file is a log file if the beginning of the
     * file name is in the list.
     *
     */
    private void setUpLogNamePrefixList() {
        logNamePrefixList = new ArrayList();
        logNamePrefixList.add(new String("jobinput.log"));
        logNamePrefixList.add(new String("stderr.log"));
        logNamePrefixList.add(new String("stdout.log"));
        logNamePrefixList.add(new String("gridnfo.log"));
        logNamePrefixList.add(new String("sys.log"));
        logNamePrefixList.add(new String("job.rsl"));
        logNamePrefixList.add(new String("job.jdl"));
        logNamePrefixList.add(new String("job.url"));
        logNamePrefixList.add(new String("job.sid"));
    }

    /**
     * Returns whether the received file name is a log file or not.
     *
     * (return value is the result of the check)
     *
     * (If empty jobPID == "" returns true for all the log files
     * not depending on the pid)
     *
     * @param fileName - log file name
     * @return - true if the file is a log file, else false
     * @throws Exception
     */
    public boolean isLogFileName(String fileName) throws Exception {
        return isLogFileName(fileName, "");
    }

    /**
     * Returns whether the received file name is a log file or not.
     *
     * (return value is the result of the check)
     *
     * (If empty jobPID == "" returns true for all the log files
     * not depending on the pid)
     *
     * @param fileName - log file name
     * @param jobPID - job PID ID
     * @return - true if the file is a log file, else false
     * @throws Exception
     */
    private boolean isLogFileName(String fileName, String jobPID) throws Exception {
        if ("".equals(jobPID)) {
            for (int iPos = 0; iPos < logNamePrefixList.size(); iPos++) {
                if (fileName.startsWith((String) logNamePrefixList.get(iPos))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns whether the received file link is a file or not.
     *
     * (return value is the result of the check)
     *
     * @param fileName - file name
     * @return - true if the file is a log file, else false
     * @throws Exception
     */
    public boolean isLinkFile(String fileName) throws Exception {
        File entry = new File(fileName);
        if (!entry.getCanonicalPath().endsWith(entry.getName())) {
            return true;
        }
        return false;
    }

    /**
     * Deletes the given directory and the subdirectories and all the files in them
     * (return value in bytes, the total size of the deleted files)
     *
     * @param dirNameStr
     * @param deleteEmptyBaseDir -
     *            if true then the given directory will be deleted too - if false then
     *            the given directory stays empty (only the content will be deleted)
     * @return - plussQuotaSize
     * @throws Exception
     */
    public long deleteDirectory(String dirNameStr, boolean deleteEmptyBaseDir) throws Exception {
        long plussQuotaSize = 0;
        if (!dirNameStr.endsWith(sep)) {
            dirNameStr += sep;
        }
        File delDirectory = new File(dirNameStr);
        if (delDirectory.exists()) {
            if (!delDirectory.isDirectory()) {
                throw new IllegalArgumentException("Not a directory: " + dirNameStr);
            } else {
                String[] filesList = delDirectory.list();
                if (filesList.length > 0) {
                    for (int pos = 0; pos < filesList.length; pos++) {
                        String entryPath = dirNameStr + filesList[pos];
                        File fileEntry = new File(entryPath);
                        if (fileEntry.isDirectory()) {
                            plussQuotaSize += this.deleteDirectory(entryPath);
                            int dcnt = 0;
                            fileEntry.delete();
                            while ((fileEntry.exists() && (dcnt < delCntMax))) {
                                fileEntry.delete();
                                Thread.sleep(7);
                                dcnt++;
                            }
                        } else {
                            plussQuotaSize += fileEntry.length();
                            int dcnt = 0;
                            fileEntry.delete();
                            while ((fileEntry.exists() && (dcnt < delCntMax))) {
                                fileEntry.delete();
                                Thread.sleep(7);
                                dcnt++;
                            }
                        }
                    }
                }
                if (deleteEmptyBaseDir) {
                    delDirectory.delete();
                }
            }
        }
        return plussQuotaSize;
    }

    /**
     * Creates the given file with the content provided in the parameter.
     *
     * @param f - file
     * @param value - content
     * @throws Exception
     */
    public void createFile(File f, String value) throws Exception {
        this.createDirectory(f.getParent());
        f.createNewFile();
        FileWriter fileWriter = new FileWriter(f);
        fileWriter.append(value);
        fileWriter.flush();
        fileWriter.close();
    }

    /**
     * Returns the content of a file in string. (Only the first line!)
     *
     * @param filePath
     * @return
     * @throws Exception
     */
    public String getFileFirstLineValue(String filePath) throws Exception {
        String retLine = null;
        File file = new File(filePath);
        if (file.exists()) {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            retLine = bufferedReader.readLine();
        } else {
            throw new Exception("File not exist, filePath = (" + filePath + ") !");
        }
        return retLine;
    }

    /**
     * Returns the content of a file in string. (Every lines!)
     *
     * @param filePath
     * @return
     * @throws Exception
     */
    public String getFileAllLineValue(String filePath) throws Exception {
        String retString = new String("");
        File file = new File(filePath);
        if (file.exists()) {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            String row = new String("");
            while ((row = bufferedReader.readLine()) != null) {
                retString += row + "\n";
            }
        } else {
            throw new Exception("File not exist, filePath = (" + filePath + ") !");
        }
        return retString;
    }

    /**
     * Returns the list of the names of the subdirectories 
     * under the received directory.
     *
     * @param baseDir
     * @return - ArrayList subdirectory names list
     * @throws Exception
     */
    public ArrayList getSubDirList(String baseDir) throws Exception {
        if (!baseDir.endsWith(sep)) {
            baseDir += sep;
        }
        ArrayList retList = new ArrayList();
        File dir = new File(baseDir);
        if (dir.exists()) {
            if (dir.isDirectory()) {
                String[] filesList = dir.list();
                if (filesList.length > 0) {
                    for (int pos = 0; pos < filesList.length; pos++) {
                        String entry = baseDir + filesList[pos];
                        if (new File(entry).isDirectory()) {
                            retList.add(filesList[pos]);
                        }
                    }
                }
            } else {
                throw new Exception("baseDir is not directory ! (" + baseDir + ")");
            }
        } else {
            throw new Exception("baseDir is not exist ! (" + baseDir + ")");
        }
        return retList;
    }

    /**
     * Converts the portalID to a directory name.
     *
     * @param portalID -
     *            incoming url string
     * @return - outgoing dir name string
     * @throws Exception
     */
    public String convertPortalIDtoDirName(String portalID) throws Exception {
        portalID = portalID.replace("/", "_");
        portalID = portalID.replace("\\", "_");
        return portalID;
    }

    /**
     * Returns the job base directories.
     *
     * (The return value is the full path)
     *
     * @param portalID - portal ID (dir path, converted value)
     * @param userID - user ID
     * @param workflowID - workflow ID
     * @param jobID - job ID
     * @return - inputs dir full path
     * @throws Exception
     */
    public String getJobBaseDirName(String portalID, String userID, String workflowID, String jobID) throws Exception {
        return portalID + sep + userID + sep + workflowID + sep + jobID + sep;
    }

    /**
     * Returns the job inputs directories.
     *
     * (The return value is the full path)
     *
     * @param portalID - portal ID (dir path, converted value)
     * @param userID - user ID
     * @param workflowID - workflow ID
     * @param jobID - job ID
     * @return - inputs dir full path
     * @throws Exception
     */
    public String getJobInputsBaseDirName(String portalID, String userID, String workflowID, String jobID) throws Exception {
        return portalID + sep + userID + sep + workflowID + sep + jobID + sep + "inputs" + sep;
    }

    /**
     * Returns the job output directories.
     *
     * (The return value is the full path)
     *
     * @param portalID - portal ID (dir path, converted value)
     * @param userID - user ID
     * @param workflowID - worklfow ID
     * @param jobID - job ID
     * @param runtimeID - runtime ID
     * @return - inputs dir full path
     * @throws Exception
     */
    public String getJobOutputsBaseDirName(String portalID, String userID, String workflowID, String jobID, String runtimeID) throws Exception {
        return portalID + sep + userID + sep + workflowID + sep + jobID + sep + "outputs" + sep + runtimeID + sep;
    }

    /**
     * Returns the size of the allocated memory of the
     * directory received in the parameter in bytes.
     *
     * @param baseDir
     * @return
     * @throws Exception
     */
    public long getDirectorySize(String baseDir) throws Exception {
        if (!baseDir.endsWith(sep)) {
            baseDir += sep;
        }
        long retSize = 0;
        File dir = new File(baseDir);
        dir.mkdirs();
        if (dir.exists()) {
            if (dir.isDirectory()) {
                String[] entryList = dir.list();
                if (entryList.length > 0) {
                    for (int pos = 0; pos < entryList.length; pos++) {
                        String entryPath = baseDir + entryList[pos];
                        File entryFile = new File(entryPath);
                        if (entryFile.isFile()) {
                            retSize += entryFile.length();
                        }
                        if (entryFile.isDirectory()) {
                            retSize += getDirectorySize(entryPath);
                        }
                    }
                }
            } else {
                throw new Exception("baseDir is not directory ! (" + baseDir + ")");
            }
        } else {
            throw new Exception("baseDir is not exist ! (" + baseDir + ")");
        }
        return retSize;
    }

    /**
     * Copies the files from the values of baseDir and hash value,
     * received in the parameter, with the names stored in the hash key value,
     * to the target directory (destDirStr), received in the parameters too.
     *
     * @param baseDirStr -
     *            counts the files in the oldPathName from this file
     * @param newNamesTable -
     *            renaming and relative path descriptor hash (key: newName,
     *            value: oldPathName), if empty or null it throws
     *            exception!
     * @param destDirStr target directory address
     * @throws Exception
     */
    public void copyHashAllFilesToDirectory(String baseDirStr, Hashtable newNamesTable, String destDirStr) throws Exception {
        if (baseDirStr.endsWith(sep)) {
            baseDirStr = baseDirStr.substring(0, baseDirStr.length() - 1);
        }
        if (destDirStr.endsWith(sep)) {
            destDirStr = destDirStr.substring(0, destDirStr.length() - 1);
        }
        FileUtils.getInstance().createDirectory(baseDirStr);
        if (null == newNamesTable) {
            newNamesTable = new Hashtable();
        }
        BufferedInputStream in = null;
        BufferedOutputStream out = null;
        byte dataBuff[] = new byte[bufferSize];
        File baseDir = new File(baseDirStr);
        baseDir.mkdirs();
        if ((baseDir.exists()) && (baseDir.isDirectory())) {
            if (!newNamesTable.isEmpty()) {
                Enumeration enumFiles = newNamesTable.keys();
                while (enumFiles.hasMoreElements()) {
                    String newName = (String) enumFiles.nextElement();
                    String oldPathName = (String) newNamesTable.get(newName);
                    if ((newName != null) && (!"".equals(newName)) && (oldPathName != null) && (!"".equals(oldPathName))) {
                        String newPathFileName = destDirStr + sep + newName;
                        String oldPathFileName = baseDirStr + sep + oldPathName;
                        if (oldPathName.startsWith(sep)) {
                            oldPathFileName = baseDirStr + oldPathName;
                        }
                        File f = new File(oldPathFileName);
                        if ((f.exists()) && (f.isFile())) {
                            in = new BufferedInputStream(new FileInputStream(oldPathFileName), bufferSize);
                            out = new BufferedOutputStream(new FileOutputStream(newPathFileName), bufferSize);
                            int readLen;
                            while ((readLen = in.read(dataBuff)) > 0) {
                                out.write(dataBuff, 0, readLen);
                            }
                            out.flush();
                            in.close();
                            out.close();
                        } else {
                        }
                    }
                }
            } else {
            }
        } else {
            throw new Exception("Base (baseDirStr) dir not exist !");
        }
    }

    /**
     * Copies all the files from baseDir to the destDir, received in the parameters
     *
     * (all dir base files, so not the subdirectories!)
     *
     * @param baseDirStr -
     *            copies from here
     * @param destDirStr -
     *            copies to here
     * @return - plussQuotaSize
     * @throws Exception
     */
    public long copyDirAllFilesToDirectory(String baseDirStr, String destDirStr) throws Exception {
        long plussQuotaSize = 0;
        if (baseDirStr.endsWith(sep)) {
            baseDirStr = baseDirStr.substring(0, baseDirStr.length() - 1);
        }
        if (destDirStr.endsWith(sep)) {
            destDirStr = destDirStr.substring(0, destDirStr.length() - 1);
        }
        FileUtils.getInstance().createDirectory(destDirStr);
        BufferedInputStream in = null;
        BufferedOutputStream out = null;
        byte dataBuff[] = new byte[bufferSize];
        File baseDir = new File(baseDirStr);
        baseDir.mkdirs();
        if (!baseDir.exists()) {
            createDirectory(baseDirStr);
        }
        if ((baseDir.exists()) && (baseDir.isDirectory())) {
            String[] entryList = baseDir.list();
            if (entryList.length > 0) {
                for (int pos = 0; pos < entryList.length; pos++) {
                    String entryName = entryList[pos];
                    String oldPathFileName = baseDirStr + sep + entryName;
                    File entryFile = new File(oldPathFileName);
                    if (entryFile.isFile()) {
                        String newPathFileName = destDirStr + sep + entryName;
                        File newFile = new File(newPathFileName);
                        if (newFile.exists()) {
                            plussQuotaSize -= newFile.length();
                            newFile.delete();
                        }
                        in = new BufferedInputStream(new FileInputStream(oldPathFileName), bufferSize);
                        out = new BufferedOutputStream(new FileOutputStream(newPathFileName), bufferSize);
                        int readLen;
                        while ((readLen = in.read(dataBuff)) > 0) {
                            out.write(dataBuff, 0, readLen);
                            plussQuotaSize += readLen;
                        }
                        out.flush();
                        in.close();
                        out.close();
                    }
                }
            }
        } else {
            throw new Exception("Base dir not exist ! baseDirStr = (" + baseDirStr + ")");
        }
        return plussQuotaSize;
    }

    /**
     * Copies all the files from baseDir to the destDir, received in the parameters
     *
     * (subdirectories will be copied too!)
     *
     * @param baseDirStr -
     *            copies from here
     * @param destDirStr -
     *            copies to here
     * @param copyOutputsRtIDsDirs -
     *            if false - if it finds in the outputs dir some rtIDs,
     *            then it won't be copied, if true everything will be copied.
     * @return - plussQuotaSize
     * @throws Exception
     */
    public long copyDirAllFilesToDirectoryRecursive(String baseDirStr, String destDirStr, boolean copyOutputsRtIDsDirs) throws Exception {
        long plussQuotaSize = 0;
        if (baseDirStr.endsWith(sep)) {
            baseDirStr = baseDirStr.substring(0, baseDirStr.length() - 1);
        }
        if (destDirStr.endsWith(sep)) {
            destDirStr = destDirStr.substring(0, destDirStr.length() - 1);
        }
        FileUtils.getInstance().createDirectory(destDirStr);
        BufferedInputStream in = null;
        BufferedOutputStream out = null;
        byte dataBuff[] = new byte[bufferSize];
        File baseDir = new File(baseDirStr);
        baseDir.mkdirs();
        if (!baseDir.exists()) {
            createDirectory(baseDirStr);
        }
        if ((baseDir.exists()) && (baseDir.isDirectory())) {
            String[] entryList = baseDir.list();
            if (entryList.length > 0) {
                for (int pos = 0; pos < entryList.length; pos++) {
                    String entryName = entryList[pos];
                    String oldPathFileName = baseDirStr + sep + entryName;
                    File entryFile = new File(oldPathFileName);
                    if (entryFile.isFile()) {
                        String newPathFileName = destDirStr + sep + entryName;
                        File newFile = new File(newPathFileName);
                        if (newFile.exists()) {
                            plussQuotaSize -= newFile.length();
                            newFile.delete();
                        }
                        in = new BufferedInputStream(new FileInputStream(oldPathFileName), bufferSize);
                        out = new BufferedOutputStream(new FileOutputStream(newPathFileName), bufferSize);
                        int readLen;
                        while ((readLen = in.read(dataBuff)) > 0) {
                            out.write(dataBuff, 0, readLen);
                            plussQuotaSize += readLen;
                        }
                        out.flush();
                        in.close();
                        out.close();
                    }
                    if (entryFile.isDirectory()) {
                        boolean enableCopyDir = false;
                        if (copyOutputsRtIDsDirs) {
                            enableCopyDir = true;
                        } else {
                            if (entryFile.getParentFile().getName().equals("outputs")) {
                                enableCopyDir = false;
                            } else {
                                enableCopyDir = true;
                            }
                        }
                        if (enableCopyDir) {
                            plussQuotaSize += this.copyDirAllFilesToDirectoryRecursive(baseDirStr + sep + entryName, destDirStr + sep + entryName, copyOutputsRtIDsDirs);
                        }
                    }
                }
            }
        } else {
            throw new Exception("Base dir not exist ! baseDirStr = (" + baseDirStr + ")");
        }
        return plussQuotaSize;
    }

    /**
     * Moves all the files from baseDir to the destDir, received in the parameters
     *
     * @param baseDirStr -
     *            moves from here
     * @param destDirStr -
     *            moves to here
     * @return - plussQuotaSize
     * @throws Exception
     */
    public long moveDirAllFilesToDirectory(String baseDirStr, String destDirStr) throws Exception {
        long plussQuotaSize = 0;
        if (baseDirStr.endsWith(sep)) {
            baseDirStr = baseDirStr.substring(0, baseDirStr.length() - 1);
        }
        if (destDirStr.endsWith(sep)) {
            destDirStr = destDirStr.substring(0, destDirStr.length() - 1);
        }
        FileUtils.getInstance().createDirectory(destDirStr);
        File baseDir = new File(baseDirStr);
        baseDir.mkdirs();
        if (!baseDir.exists()) {
            createDirectory(baseDirStr);
        }
        if ((baseDir.exists()) && (baseDir.isDirectory())) {
            String[] entryList = baseDir.list();
            if (entryList.length > 0) {
                for (int pos = 0; pos < entryList.length; pos++) {
                    String entryName = entryList[pos];
                    String oldPathFileName = baseDirStr + sep + entryName;
                    File entryFile = new File(oldPathFileName);
                    if (entryFile.isFile()) {
                        String newPathFileName = destDirStr + sep + entryName;
                        File newFile = new File(newPathFileName);
                        if (newFile.exists()) {
                            plussQuotaSize -= newFile.length();
                            newFile.delete();
                        }
                        entryFile.renameTo(newFile);
                        plussQuotaSize += newFile.length();
                    }
                }
            }
        } else {
            throw new Exception("Base dir not exist ! baseDirStr = (" + baseDirStr + ")");
        }
        return plussQuotaSize;
    }

    /**
     * Writes out the received file to the received stream. (no compression)
     *
     * @param filePathStr -
     *            full path of the file
     * @param out -
     *            writes out the file here
     * @throws Exception
     */
    public void sendFileToStream(OutputStream out, String filePathStr) throws Exception {
        File sendFile = new File(filePathStr);
        if (sendFile.exists() && (sendFile.isFile())) {
            byte dataBuff[] = new byte[bufferSize];
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(filePathStr), bufferSize);
            int readLen;
            while ((readLen = in.read(dataBuff)) > 0) {
                out.write(dataBuff, 0, readLen);
            }
            in.close();
        } else {
            throw new Exception("Not valid parameters, file not exist !!! fileFullPathStr = (" + filePathStr + ")");
        }
    }

    /**
     * Copies the given file from the baseDir received in the parameters to the file2FullPath
     *
     * The return value is the size change of the target directory.
     *
     * @param baseDirStr -
     *            files will be copied from this directory
     * @param fileName -
     *            this file will be copied
     * @param file2FullPath -
     *            file will be copied here (full path with file name)
     * @return - plussQuotaSize
     * @throws Exception
     */
    public long copyFile(String baseDirStr, String fileName, String file2FullPath) throws Exception {
        long plussQuotaSize = 0;
        if (!baseDirStr.endsWith(sep)) {
            baseDirStr += sep;
        }
        BufferedInputStream in = null;
        BufferedOutputStream out = null;
        byte dataBuff[] = new byte[bufferSize];
        String file1FullPath = new String(baseDirStr + fileName);
        if (!file1FullPath.equalsIgnoreCase(file2FullPath)) {
            File file1 = new File(file1FullPath);
            if (file1.exists() && (file1.isFile())) {
                File file2 = new File(file2FullPath);
                if (file2.exists()) {
                    plussQuotaSize -= file2.length();
                    file2.delete();
                }
                FileUtils.getInstance().createDirectory(file2.getParent());
                in = new BufferedInputStream(new FileInputStream(file1FullPath), bufferSize);
                out = new BufferedOutputStream(new FileOutputStream(file2FullPath), bufferSize);
                int readLen;
                while ((readLen = in.read(dataBuff)) > 0) {
                    out.write(dataBuff, 0, readLen);
                    plussQuotaSize += readLen;
                }
                out.flush();
                in.close();
                out.close();
            } else {
                throw new Exception("Source file not exist ! file1FullPath = (" + file1FullPath + ")");
            }
        }
        return plussQuotaSize;
    }

    /**
     * Creates a symbolic link of a given file (in fileName) 
     * from the baseDir, received in the parameters, to the file2FullPath place.
     * (E.g: it will be called many times if the output port file name 
     * stored in the graph and in the config is not the same)
     *
     * The return value is the size change of the target directory. (0 byte)
     *
     * @param baseDirStr -
     *            the source file is in this directory
     * @param fileName -
     *            the source file name
     * @param file2FullPath -
     *            the link will be placed here (full path with file name)
     * @return - plussQuotaSize
     * @throws Exception
     */
    public synchronized long createLink(String baseDirStr, String fileName, String file2FullPath) throws Exception {
        long plussQuotaSize = 0;
        if (!baseDirStr.endsWith(sep)) {
            baseDirStr += sep;
        }
        String file1FullPath = new String(baseDirStr + fileName);
        if (!file1FullPath.equalsIgnoreCase(file2FullPath)) {
            File file1 = new File(file1FullPath);
            if (file1.exists() && (file1.isFile())) {
                File file2 = new File(file2FullPath);
                FileUtils.getInstance().createDirectory(file2.getParent());
                String[] linkCmd = { "ln", "-s", file1FullPath, file2FullPath };
                int retCode = 1;
                int reCnt = 0;
                while ((retCode != 0) && (reCnt < 100)) {
                    retCode = 0;
                    reCnt++;
                    try {
                        Process result = Runtime.getRuntime().exec(linkCmd);
                        retCode = result.waitFor();
                        result.destroy();
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.out.println("storage createLink exception : " + e.getLocalizedMessage() + reCnt + " retCode : " + retCode);
                        Thread.sleep(500);
                    }
                }
            } else {
                throw new Exception("Source file not exist ! file1FullPath = (" + file1FullPath + ")");
            }
        }
        return plussQuotaSize;
    }

    /**
     * The file received in the parameter will be copied to the given place.
     *
     * The return value is the size change of the target directory.
     *
     * @param userBaseDir -
     *            the user base directory (full repository path)
     * @param sourcePath -
     *            this fill will be copied (full path with file name)
     *            (if the path ends with a  / then all the files will be copied)
     * @param destinPath -
     *            files will be copied here (full path with file name)
     * @return - plussQuotaSize
     * @throws Exception
     */
    public long copyFileWithPaths(String userBaseDir, String sourcePath, String destinPath) throws Exception {
        if (userBaseDir.endsWith(sep)) {
            userBaseDir = userBaseDir.substring(0, userBaseDir.length() - sep.length());
        }
        String file1FullPath = new String();
        if (sourcePath.startsWith(sep)) {
            file1FullPath = new String(userBaseDir + sourcePath);
        } else {
            file1FullPath = new String(userBaseDir + sep + sourcePath);
        }
        String file2FullPath = new String();
        if (destinPath.startsWith(sep)) {
            file2FullPath = new String(userBaseDir + destinPath);
        } else {
            file2FullPath = new String(userBaseDir + sep + destinPath);
        }
        long plussQuotaSize = 0;
        BufferedInputStream in = null;
        BufferedOutputStream out = null;
        byte dataBuff[] = new byte[bufferSize];
        File fileordir = new File(file1FullPath);
        if (fileordir.exists()) {
            if (fileordir.isFile()) {
                File file2 = new File(file2FullPath);
                if (file2.exists()) {
                    plussQuotaSize -= file2.length();
                    file2.delete();
                }
                FileUtils.getInstance().createDirectory(file2.getParent());
                in = new BufferedInputStream(new FileInputStream(file1FullPath), bufferSize);
                out = new BufferedOutputStream(new FileOutputStream(file2FullPath), bufferSize);
                int readLen;
                while ((readLen = in.read(dataBuff)) > 0) {
                    out.write(dataBuff, 0, readLen);
                    plussQuotaSize += readLen;
                }
                out.flush();
                in.close();
                out.close();
            }
            if (fileordir.isDirectory()) {
                String[] entryList = fileordir.list();
                if (entryList.length > 0) {
                    for (int pos = 0; pos < entryList.length; pos++) {
                        String entryName = entryList[pos];
                        String file1FullPathEntry = new String(file1FullPath.concat(entryList[pos]));
                        String file2FullPathEntry = new String(file2FullPath.concat(entryList[pos]));
                        File file2 = new File(file2FullPathEntry);
                        if (file2.exists()) {
                            plussQuotaSize -= file2.length();
                            file2.delete();
                        }
                        FileUtils.getInstance().createDirectory(file2.getParent());
                        in = new BufferedInputStream(new FileInputStream(file1FullPathEntry), bufferSize);
                        out = new BufferedOutputStream(new FileOutputStream(file2FullPathEntry), bufferSize);
                        int readLen;
                        while ((readLen = in.read(dataBuff)) > 0) {
                            out.write(dataBuff, 0, readLen);
                            plussQuotaSize += readLen;
                        }
                        out.flush();
                        in.close();
                        out.close();
                    }
                }
            }
        } else {
            throw new Exception("Source file or dir not exist ! file1FullPath = (" + file1FullPath + ")");
        }
        return plussQuotaSize;
    }

    /**
     * The file received in the parameter will be copied to the given place.
     *
     * @param sourcePath -
     *            this file will be copied (full path with file name)
     * @param destinPath -
     *            file will be copied here (full path with file name)
     * @throws Exception
     */
    public void copyFileToFileWithPaths(String sourcePath, String destinPath) throws Exception {
        BufferedInputStream in = null;
        BufferedOutputStream out = null;
        byte dataBuff[] = new byte[bufferSize];
        File file1 = new File(sourcePath);
        if (file1.exists() && (file1.isFile())) {
            File file2 = new File(destinPath);
            if (file2.exists()) {
                file2.delete();
            }
            FileUtils.getInstance().createDirectory(file2.getParent());
            in = new BufferedInputStream(new FileInputStream(sourcePath), bufferSize);
            out = new BufferedOutputStream(new FileOutputStream(destinPath), bufferSize);
            int readLen;
            while ((readLen = in.read(dataBuff)) > 0) {
                out.write(dataBuff, 0, readLen);
            }
            out.flush();
            in.close();
            out.close();
        } else {
            throw new Exception("Source file not exist ! sourcePath = (" + sourcePath + ")");
        }
    }

    /**
     * Creates a normal file from the pid output file name.
     *
     * E.g.:
     * from output.jpg_0 will be output_0.jpg
     * from output.jpg will be output.jpg
     * from output_0 will be output_0
     *
     * (the pid and the extension will be swapped)
     *
     * @param oldFileName - original pid and file name
     * @return - normal file name String
     * @throws Exception
     */
    public String getNormalFromPidName(String oldFileName) throws Exception {
        String retFileName = oldFileName;
        int pidPos = 0;
        if (oldFileName.contains("_")) {
            pidPos = oldFileName.lastIndexOf("_");
        }
        if (pidPos != 0) {
            int extPos = 0;
            if (oldFileName.contains(".")) {
                extPos = oldFileName.lastIndexOf(".");
            }
            if (extPos != 0) {
                if (extPos < pidPos) {
                    String name = oldFileName.substring(0, extPos);
                    String ext = oldFileName.substring(extPos, pidPos);
                    String pid = oldFileName.substring(pidPos, oldFileName.length());
                    retFileName = name + pid + ext;
                }
            }
        }
        return retFileName;
    }

    /**
     *Returns a unique temporary directory name.
     *
     * @return temp dir name
     */
    public String getUniqueTempDirName() {
        Long randomNum = new Long(Math.round(Math.random() * 900000) + 100000);
        return new String("x" + randomNum + System.currentTimeMillis());
    }

    /**
 * Converts the directory name to portalid, all the previously changed special characters will be formed back
 * @param portalID directory name
 * @return portal id
 */
    public String convertDirNametoPortalID(String portalID) {
        return portalID.replace("_", "/");
    }
}
