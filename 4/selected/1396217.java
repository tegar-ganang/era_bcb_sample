package org.icenigrid.gridsam.core.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import java.util.concurrent.*;
import java.util.ResourceBundle;
import java.util.MissingResourceException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.icenigrid.gridsam.core.JobInstance;
import org.icenigrid.gridsam.core.hostenv.ConfigUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * <strong>Purpose:</strong><br>
 * Provide some utilities for the ease of manipulating the batch system
 * 
 * @author qiaojian<br>
 *         Email: qiaojian@software.ict.ac.cn<br>
 *         Update Date : 2007-7-21<br>
 * @version 1.0.1
 */
public class BatchUtils {

    /**
	 * Logger.
	 */
    private static final Log log = LogFactory.getLog("driver.BatchUtils");

    /**
	 * The path of Driver in the batch.conf.
	 */
    private static String driverScript = null;

    /**
	 * The path to the file_driver.
	 */
    private static String fileDriverPath = null;

    /**
	 * The path to the temp location.
	 */
    private static String xTempDir = null;

    private static String[] fileDriverEnv = null;

    private static boolean useJNI = false;

    private static Semaphore execSemaphore = null;

    private static int workdirHierarchyLevel = 0;

    static {
        try {
            ConfigUtil xConfigUtil = new ConfigUtil(BatchConstants.OMII_GRIDSAM_SERVICE_PROPERTIES_NAME);
            String driverName = xConfigUtil.getProperty("driver.script", "pbs_driver_sudo.sh");
            ConfigUtil xSecurityConfigUtil = new ConfigUtil(BatchConstants.OMII_GRIDSAM_SECURITY_CONFIG_NAME);
            String driverHome = xSecurityConfigUtil.getProperty("driver.home", "[Error! Cannot get driver.home from " + BatchConstants.OMII_GRIDSAM_SECURITY_CONFIG_NAME + "]");
            if ("DEFAULT".equals(driverHome)) {
                driverHome = System.getProperty("GRIDSAM_HOME") + "/drivers";
            }
            File drivers = new File(driverHome);
            for (File d : drivers.listFiles()) {
                if (d.isFile() && d.getName().endsWith(".sh")) {
                    d.setExecutable(true);
                }
            }
            driverScript = driverHome + File.separator + driverName;
            log.info("Initializing the PBS/LSF driver path: " + driverScript);
            String fileDriverName = xSecurityConfigUtil.getProperty("file.driver.script", "file_driver_sudo.sh");
            fileDriverPath = driverHome + File.separator + fileDriverName;
            log.info("Initializing the file driver path: " + fileDriverPath);
            xTempDir = xSecurityConfigUtil.getProperty("TempStagingDir", "/tmp");
            log.info("Initializing the temp staging dir: " + xTempDir);
            File tempDir = new File(xTempDir);
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }
            try {
                String levelStr = xSecurityConfigUtil.getProperty(BatchConstants.WORKING_DIR_HIERARCHY_LEVEL, "0");
                workdirHierarchyLevel = Integer.parseInt(levelStr);
                if (workdirHierarchyLevel > 16 || workdirHierarchyLevel < 0) {
                    workdirHierarchyLevel = 0;
                }
            } catch (Exception e) {
            }
            log.info("Initializing working directory hierarchy level to: " + workdirHierarchyLevel);
            String fdEnv = null;
            String useJNIString = null;
            String maxConcurrentExecString = null;
            try {
                ResourceBundle rb = ResourceBundle.getBundle("ICTDRM");
                try {
                    fdEnv = rb.getString("file_driver_environment");
                } catch (Exception e) {
                }
                try {
                    useJNIString = rb.getString("use_jni");
                } catch (Exception e) {
                }
                try {
                    maxConcurrentExecString = rb.getString("max_concurrent_execs");
                } catch (Exception e) {
                }
            } catch (MissingResourceException e) {
            }
            if (fdEnv != null) {
                fileDriverEnv = fdEnv.split(",");
            }
            if (useJNIString != null && "true".equals(useJNIString)) {
                useJNI = true;
                log.warn("Using JNI routines for spawning subprocesses, this disables the ability to pass environment variables to the subprocess.");
            }
            int maxConcurrentExec = 30;
            if (maxConcurrentExecString != null) {
                try {
                    maxConcurrentExec = Integer.parseInt(maxConcurrentExecString);
                } catch (NumberFormatException nfe) {
                }
                if (maxConcurrentExec <= 0) {
                    maxConcurrentExec = 30;
                }
            }
            execSemaphore = new Semaphore(maxConcurrentExec, true);
            log.info("Built execution semaphore with " + execSemaphore.availablePermits() + " permits");
        } catch (Exception e) {
            log.error("![Error! read properties file error]" + e.getMessage());
        }
    }

    /**
	 * To create the working dir for the indicated job, which is owned by the
	 * correponding linux user.
	 * 
	 * @param owner
	 *            The owner of the working dir.
	 * @param dirPath
	 *            The path to working dir.<br>
	 *            eg:/home/qiaojian/GOS-ff11111111111111111111111
	 * @return FileDriverResponseParser instance
	 * @throws RemoteException
	 *             fail to make directory.
	 */
    public static FileDriverResponseParser makeDir(String owner, String dirPath) throws RemoteException {
        return makeDir(owner, dirPath, null);
    }

    public static FileDriverResponseParser makeDir(String owner, String dirPath, HashMap env) throws RemoteException {
        String command = fileDriverPath + " -u " + owner + " -o mkdir -p1 " + dirPath;
        String response = execProcess(command, xTempDir, env);
        FileDriverResponseParser xRet = new FileDriverResponseParser("mkdir", response);
        return xRet;
    }

    /**
	 * 
	 * To create the dir for the indicated job, and chmod this directory.
	 * 
	 * @param owner
	 *            The owner of the working dir.
	 * @param dirPath
	 *            The path to working dir.<br>
	 *            eg:/home/qiaojian/GOS-ff11111111111111111111111.
	 * @param umask
	 *            the umask of the dirPath.
	 * @return FileDriverResponseParser instance
	 * @throws RemoteException
	 *             fail to make directory and chmod.
	 */
    public static FileDriverResponseParser makeDirAndChmod(String owner, String dirPath, String umask) throws RemoteException {
        return makeDirAndChmod(owner, dirPath, umask, null);
    }

    public static FileDriverResponseParser makeDirAndChmod(String owner, String dirPath, String umask, HashMap env) throws RemoteException {
        String command = fileDriverPath + " -u " + owner + " -o mkdirAndChmod " + " -p1 " + dirPath + " -p2 " + umask;
        String response = execProcess(command, xTempDir, env);
        FileDriverResponseParser xRet = new FileDriverResponseParser("mkdirAndChmod", response);
        return xRet;
    }

    /**
	 * 
	 * To chmod this directory.
	 * 
	 * @param owner
	 *            The owner of the working dir.
	 * @param dirPath
	 *            The path to working dir.<br>
	 *            eg:/home/qiaojian/GOS-ff11111111111111111111111.
	 * @param umask
	 *            the umask of the dirPath.
	 * @return FileDriverResponseParser instance
	 * @throws RemoteException
	 *             fail to chmod.
	 */
    public static FileDriverResponseParser chmod(String owner, String dirPath, String umask) throws RemoteException {
        return chmod(owner, dirPath, umask, null);
    }

    public static FileDriverResponseParser chmod(String owner, String dirPath, String umask, HashMap env) throws RemoteException {
        String command = fileDriverPath + " -u " + owner + " -o chmod " + " -p1 " + dirPath + " -p2 " + umask;
        String response = execProcess(command, xTempDir, env);
        FileDriverResponseParser xRet = new FileDriverResponseParser("chmod", response);
        return xRet;
    }

    /**
	 * 
	 * To cat the content of the indicated file, which is owned by targer user.
	 * 
	 * @param owner
	 *            The owner of the file
	 * @param filePath
	 *            The path to indicated file.
	 * @return string content of file
	 * @throws RemoteException
	 *             fail to cat the file.
	 */
    public static String catFile(String owner, String filePath) throws RemoteException {
        return catFile(owner, filePath, null);
    }

    public static String catFile(String owner, String filePath, HashMap env) throws RemoteException {
        String command = fileDriverPath + " -u " + owner + " -o cat " + " -p1 " + filePath;
        String response = execProcess(command, xTempDir, env);
        return response;
    }

    /**
	 * To delete the indicated dir or file.
	 * 
	 * @param owner
	 *            The driver user name.
	 * @param dirPath
	 *            The path to target dir or file. /tmp/GOS-111111/result.txt
	 * @return FileDriverResponseParser instance
	 * @throws RemoteException
	 *             fail to delete file.
	 */
    public static FileDriverResponseParser deleteFile(String owner, String dirPath) throws RemoteException {
        return deleteFile(owner, dirPath, null);
    }

    public static FileDriverResponseParser deleteFile(String owner, String dirPath, HashMap env) throws RemoteException {
        String command = fileDriverPath + " -u " + owner + " -o del -p1 " + dirPath;
        String response = execProcess(command, xTempDir, env);
        FileDriverResponseParser xRet = new FileDriverResponseParser("del", response);
        return xRet;
    }

    /**
	 * To read the content of source file and write to the target file. During
	 * this process, owner-changing operation will be done.
	 * 
	 * @param owner
	 *            The linux user who own the real working dir.
	 * @param sourceFilePath
	 *            The absulote path to the original file, which will be read and
	 *            in the temp dir.
	 * @param targetFilePath
	 *            The absulote path to the target file, which is in the real
	 *            working dir
	 * @return FileDriverResponseParser instance containing the invoking result.
	 * @throws RemoteException
	 *             fail to copy the source to target.
	 */
    public static FileDriverResponseParser copyContent(String owner, String sourceFilePath, String targetFilePath) throws RemoteException {
        return copyContent(owner, sourceFilePath, targetFilePath, null);
    }

    public static FileDriverResponseParser copyContent(String owner, String sourceFilePath, String targetFilePath, HashMap env) throws RemoteException {
        String command = fileDriverPath + " -u " + owner + " -o copyContent -p1 " + sourceFilePath + " -p2 " + targetFilePath;
        String response = execProcess(command, xTempDir, env);
        FileDriverResponseParser xRet = new FileDriverResponseParser("copyContent", response);
        return xRet;
    }

    /**
	 * To move source file and write to the target file. During this process,
	 * owner-changing operation will NOT be done.
	 * 
	 * @param owner
	 *            The linux user who own the real working dir.
	 * @param sourceFilePath
	 *            The absulote path to the original file, which will be read and
	 *            in the temp dir.
	 * @param targetFilePath
	 *            The absulote path to the target file, which is in the real
	 *            working dir
	 * @return FileDriverResponseParser instance containing the invoking result.
	 * @throws RemoteException
	 *             fail to mv source to target.
	 */
    public static FileDriverResponseParser moveFile(String owner, String sourceFilePath, String targetFilePath) throws RemoteException {
        return moveFile(owner, sourceFilePath, targetFilePath, null);
    }

    public static FileDriverResponseParser moveFile(String owner, String sourceFilePath, String targetFilePath, HashMap env) throws RemoteException {
        String command = fileDriverPath + " -u " + owner + " -o mv -p1 " + sourceFilePath + " -p2 " + targetFilePath;
        String response = execProcess(command, xTempDir, env);
        FileDriverResponseParser xRet = new FileDriverResponseParser("mv", response);
        return xRet;
    }

    /**
	 * to change stagein-file's owner to localUser.
	 * 
	 * @param owner
	 *            file's owner.
	 * @param group
	 * 			  owner is belong to this group. 
	 * @param filePath
	 *            file's path.
	 * 
	 * @return FileDriverResponseParser instance containing the invoking result.
	 * @throws RemoteException
	 *             fail to change owner of the file.
	 */
    public static FileDriverResponseParser chown(String owner, String group, String filePath) throws RemoteException {
        return chown(owner, group, filePath, null);
    }

    public static FileDriverResponseParser chown(String owner, String group, String filePath, HashMap env) throws RemoteException {
        String command = fileDriverPath + " -u root -o chown -p1 " + owner + ":" + group + " -p2 " + filePath;
        String response = execProcess(command, xTempDir, env);
        FileDriverResponseParser xRet = new FileDriverResponseParser("chown", response);
        return xRet;
    }

    /**
	 * check owner can read filePath or not.
	 * 
	 * @param owner
	 *            localUser's name
	 * @param filePath
	 *            file's path.
	 * @return FileDriverResponseParser instance containing the invoking result.
	 * @throws RemoteException
	 *             fail to get whether the file is readable or not.
	 */
    public static FileDriverResponseParser isReadAble(String owner, String filePath) throws RemoteException {
        return isReadAble(owner, filePath, null);
    }

    public static FileDriverResponseParser isReadAble(String owner, String filePath, HashMap env) throws RemoteException {
        String command = fileDriverPath + " -u " + owner + " -o isReadAble -p1 " + filePath + "";
        String response = execProcess(command, xTempDir, env);
        FileDriverResponseParser xRet = new FileDriverResponseParser("isReadAble", response);
        return xRet;
    }

    /**
	 * get tail string of the file.
	 * 
	 * @param owner
	 *            localUser's name.
	 * 
	 * @param lineNumber
	 *            tail line number of the file.
	 * @param filePath
	 *            file's path.
	 * @return FileDriverResponseParser instance containing the invoking result.
	 * @throws RemoteException
	 *             fail to get whether the file is readable or not.
	 */
    public static String tail(String owner, int lineNumber, String filePath) throws RemoteException {
        return tail(owner, lineNumber, filePath, null);
    }

    public static String tail(String owner, int lineNumber, String filePath, HashMap env) throws RemoteException {
        String command = fileDriverPath + " -u " + owner + " -o tail " + " -p1 " + lineNumber + " -p2 " + filePath;
        String response = execProcess(command, xTempDir, env);
        return response;
    }

    /**
	 * check owner can write in filePath or not.
	 * 
	 * @param owner
	 *            localUser's name
	 * @param filePath
	 *            file's path.
	 * @return FileDriverResponseParser instance containing the invoking result.
	 * @throws RemoteException
	 *             fail to get whether the file is writeable or not.
	 */
    public static FileDriverResponseParser isWriteAble(String owner, String filePath) throws RemoteException {
        return isWriteAble(owner, filePath, null);
    }

    public static FileDriverResponseParser isWriteAble(String owner, String filePath, HashMap env) throws RemoteException {
        String command = fileDriverPath + " -u " + owner + " -o isWriteAble -p1 " + filePath;
        String response = execProcess(command, xTempDir, env);
        FileDriverResponseParser xRet = new FileDriverResponseParser("isWriteAble", response);
        return xRet;
    }

    /**
	 * To get the first exist path of the given dirPath.
	 * 
	 * @param owner
	 *            The owner of the working dir.
	 * @param dirPath
	 *            The path to check.<br>
	 *            eg:/home/qiaojian/GOS-ff11111111111111111111111
	 * @return FileDriverResponseParser instance
	 * @throws RemoteException
	 *             fail to make directory.
	 */
    public static FileDriverResponseParser getFirstExistPath(String owner, String dirPath) throws RemoteException {
        return getFirstExistPath(owner, dirPath, null);
    }

    public static FileDriverResponseParser getFirstExistPath(String owner, String dirPath, HashMap env) throws RemoteException {
        String command = fileDriverPath + " -u " + owner + " -o getFirstExistPath -p1 " + dirPath;
        String response = execProcess(command, xTempDir, env);
        FileDriverResponseParser xRet = new FileDriverResponseParser("getFirstExistPath", response);
        return xRet;
    }

    /**
	 * do post process in job working directory.
	 * 
	 * @param owner
	 *            the local user
	 * @param prefix
	 *            the prefix of the file that generate by system
	 * @param localJobID
	 *            job id
	 * @param workingDir
	 *            woring directory
	 * @param processFile
	 *            the file that contain post process command.
	 * @throws RemoteException
	 *             fail to do process in job working directory.
	 */
    public static void doProcess(String owner, String prefix, String localJobID, String workingDir, String processFile) throws RemoteException {
        doProcess(owner, prefix, localJobID, workingDir, processFile, null);
    }

    public static void doProcess(String owner, String prefix, String localJobID, String workingDir, String processFile, HashMap env) throws RemoteException {
        String command = driverScript + " -u " + owner + " -p " + prefix + " -i " + localJobID + " -post -w " + workingDir + " -f " + processFile;
        if (useJNI) {
            execProcessbyJNI(command, xTempDir);
        } else {
            execProcess(command, xTempDir, env);
        }
    }

    /**
	 * To submit local batch job.
	 * 
	 * @param owner
	 *            The linux user who own the real job
	 * @param prefix
	 *            The file prefix of the indicated job
	 * @param workingDir
	 *            The path to the real working dir
	 * @return DriverResponseParser instance containing the invoking result.
	 * @throws RemoteException
	 *             fail to sumbit the job.
	 */
    public static DriverResponseParser submit(String owner, String prefix, String workingDir) throws RemoteException {
        return submit(owner, prefix, workingDir, null);
    }

    public static DriverResponseParser submit(String owner, String prefix, String workingDir, HashMap env) throws RemoteException {
        String command = driverScript + " -u " + owner + " -p " + prefix + " -b -w " + workingDir;
        String response = execProcess(command, xTempDir, env);
        DriverResponseParser xRet = new DriverResponseParser(prefix, "-b", response);
        return xRet;
    }

    /**
	 * To query local batch job status.
	 * 
	 * @param owner
	 *            The linux user who own the real job
	 * @param prefix
	 *            The file prefix of the indicated job
	 * @param localJobID
	 *            The jobId of the indicated pbs/lsf job.
	 * @param workingDir
	 *            The path to the real working dir
	 * @return DriverResponseParser instance containing the invoking result.
	 * @throws RemoteException
	 *             fail to get status of the job.
	 */
    public static DriverResponseParser status(String owner, String prefix, String localJobID, String workingDir) throws RemoteException {
        return status(owner, prefix, localJobID, workingDir, null);
    }

    public static DriverResponseParser status(String owner, String prefix, String localJobID, String workingDir, HashMap env) throws RemoteException {
        String command = driverScript + " -u " + owner + " -p " + prefix + " -i " + localJobID + " -s -w " + workingDir;
        String response = execProcess(command, xTempDir, env);
        DriverResponseParser xRet = new DriverResponseParser(prefix, "-s", response);
        return xRet;
    }

    /**
	 * To cancel local batch job.
	 * 
	 * @param owner
	 *            The linux user who own the real job
	 * @param prefix
	 *            The file prefix of the indicated job
	 * @param localJobID
	 *            The jobId of the indicated pbs/lsf job.
	 * @param workingDir
	 *            The path to the real working dir
	 * @return DriverResponseParser instance containing the invoking result.
	 * @throws RemoteException
	 *             fail to cancel the job.
	 */
    public static DriverResponseParser cancel(String owner, String prefix, String localJobID, String workingDir) throws RemoteException {
        return cancel(owner, prefix, localJobID, workingDir, null);
    }

    public static DriverResponseParser cancel(String owner, String prefix, String localJobID, String workingDir, HashMap env) throws RemoteException {
        String command = driverScript + " -u " + owner + " -p " + prefix + " -i " + localJobID + " -c -w " + workingDir;
        String response = execProcess(command, xTempDir, env);
        DriverResponseParser xRet = new DriverResponseParser(prefix, "-c", response);
        return xRet;
    }

    /**
	 * To get the screen output of the indicated local batch job.
	 * 
	 * @param owner
	 *            The linux user who own the real job
	 * @param prefix
	 *            The file prefix of the indicated job
	 * @param localJobID
	 *            The jobId of the indicated pbs/lsf job.
	 * @param workingDir
	 *            The path to the real working dir
	 * @return DriverResponseParser instance containing the invoking result.
	 * @throws RemoteException
	 *             fail to get screen-output of the job.
	 */
    public static DriverResponseParser screenOutput(String owner, String prefix, String localJobID, String workingDir) throws RemoteException {
        return screenOutput(owner, prefix, localJobID, workingDir, null);
    }

    public static DriverResponseParser screenOutput(String owner, String prefix, String localJobID, String workingDir, HashMap env) throws RemoteException {
        String command = driverScript + " -u " + owner + " -p " + prefix + " -i " + localJobID + " -r -w " + workingDir;
        String response = execProcess(command, xTempDir, env);
        DriverResponseParser xRet = new DriverResponseParser(prefix, "-r", response);
        return xRet;
    }

    /**
	 * To get the details of the indicated local batch job.
	 * 
	 * @param owner
	 *            The linux user who own the real job
	 * @param prefix
	 *            The file prefix of the indicated job
	 * @param localJobID
	 *            The jobId of the indicated pbs/lsf job.
	 * @param workingDir
	 *            The path to the real working dir
	 * @return DriverResponseParser instance containing the invoking result.
	 * @throws RemoteException
	 *             fail to get detail info of the job.
	 */
    public static DriverResponseParser detail(String owner, String prefix, String localJobID, String workingDir) throws RemoteException {
        return detail(owner, prefix, localJobID, workingDir, null);
    }

    public static DriverResponseParser detail(String owner, String prefix, String localJobID, String workingDir, HashMap env) throws RemoteException {
        String command = driverScript + " -u " + owner + " -p " + prefix + " -i " + localJobID + " -d -w " + workingDir;
        String response = execProcess(command, xTempDir, env);
        DriverResponseParser xRet = new DriverResponseParser(prefix, "-d", response);
        return xRet;
    }

    /**
	 * To get the stdout and stderr file name of the indicated local batch job.
	 * 
	 * @param owner
	 *            The linux user who own the real job
	 * @param prefix
	 *            The file prefix of the indicated job
	 * @param localJobID
	 *            The jobId of the indicated pbs/lsf job.
	 * @param workingDir
	 *            The path to the real working dir
	 * @return DriverResponseParser instance containing the invoking result.
	 * @throws RemoteException
	 *             fail to get standand ouput or standard error output.
	 */
    public static DriverResponseParser stdOutErr(String owner, String prefix, String localJobID, String workingDir) throws RemoteException {
        return stdOutErr(owner, prefix, localJobID, workingDir, null);
    }

    public static DriverResponseParser stdOutErr(String owner, String prefix, String localJobID, String workingDir, HashMap env) throws RemoteException {
        String command = driverScript + " -u " + owner + " -p " + prefix + " -i " + localJobID + " -o -w " + workingDir;
        String response = execProcess(command, xTempDir, env);
        DriverResponseParser xRet = new DriverResponseParser(prefix, "-o", response);
        return xRet;
    }

    /**
	 * To delete the temp dir after setting the job as failed! Just try once :)
	 * 
	 * @param pJob
	 *            The job instance which own the temp Dir.
	 */
    public static void deleteTempAfterFailed(JobInstance pJob) {
        if (pJob == null || pJob.getID() == null) {
            return;
        }
        try {
            String tmpDir = (String) pJob.getProperties().get(HPCGJobPropertyConstants.TEMP_DIR);
            String localUN = (String) pJob.getProperties().get(HPCGJobPropertyConstants.LOCAL_UN);
            if (tmpDir == null || tmpDir.trim().length() == 0) {
                return;
            }
            if (localUN == null || localUN.trim().length() == 0) {
                return;
            }
            deleteFile("root", tmpDir);
        } catch (Exception e) {
            log.warn("Warn from deleteTempAfterFailed() for job urn:gridsam:" + pJob.getID() + ". Cause: " + e.getLocalizedMessage());
        }
    }

    /**
	 * To read the proxy content.
	 * 
	 * @param proxyPath
	 *            the path of the proxy file.
	 * @return byte[] proxy the content of the proxy file.
	 * @throws Exception
	 *             fail to read proxy file.
	 */
    public static byte[] readProxy(String proxyPath) throws Exception {
        FileChannel fileCh = new FileInputStream(new File(proxyPath)).getChannel();
        ByteBuffer byteBuffer = ByteBuffer.allocate(new Long(fileCh.size()).intValue());
        fileCh.read(byteBuffer);
        return byteBuffer.array();
    }

    /**
	 * create a file in the intended location , and then inject the content into
	 * this file.
	 * 
	 * @param src
	 *            content which should be written into the created file
	 * @param fn
	 *            file location which will be created
	 * @return true if success
	 */
    public static boolean string2File(String src, String fn) {
        BufferedWriter out = null;
        if ((src == null) || (fn == null)) return false;
        try {
            out = new BufferedWriter(new FileWriter(fn));
            out.write(src);
            out.flush();
            out.close();
        } catch (IOException e) {
            log.error("error in string2File  " + fn + ":" + e.toString());
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ie) {
                }
                out = null;
                return false;
            }
        }
        out = null;
        return true;
    }

    /**
	 * To write the string into the indicated file.
	 * 
	 * @param src
	 *            the content you want to set to a file.
	 * @param fn
	 *            file name.
	 * @return true if success
	 */
    public static boolean string2FileAppend(String src, String fn) {
        BufferedWriter out = null;
        if ((src == null) || (fn == null)) return false;
        try {
            out = new BufferedWriter(new FileWriter(fn, true));
            out.write(src);
            out.flush();
            out.close();
        } catch (IOException e) {
            log.error("error in string2FileAppend  " + fn + ":" + e.toString());
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ie) {
                }
                out = null;
                return false;
            }
        }
        out = null;
        return true;
    }

    /**
	 * convert a XML String into the XML Document object.
	 * 
	 * @param str
	 *            xml String.
	 * @return Document instance from string content
	 */
    public static Document string2Document(String str) {
        return source2Document(new InputSource(new StringReader(str)));
    }

    /**
	 * read the inputsource into the XML document.
	 * 
	 * @param is
	 *            input source of the xml file.
	 * @return ocument instance from string content
	 */
    private static Document source2Document(InputSource is) {
        Document d = null;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setValidating(false);
            DocumentBuilder db = dbf.newDocumentBuilder();
            d = db.parse(is);
        } catch (Exception e) {
            log.error("source2Document" + "Parse error:" + e.toString());
        }
        return d;
    }

    /**
	 * get the pure name , pure name means the name without path information.
	 * e.g: pure name of "d:/1234/newfolder/1.txt" is "1.txt"
	 * 
	 * @param path
	 *            path name.
	 * @return pure name string
	 */
    public static String getPureName(String path) {
        int[] spchars = { '/', '\\', ':' };
        int t = -1;
        if (path == null) return path;
        for (int i = 0; i < spchars.length; i++) {
            int idx = path.lastIndexOf(spchars[i]);
            if (idx > t) {
                t = idx;
            }
        }
        return path.substring(t + 1);
    }

    /**
	 * get element body using name from document d.
	 * 
	 * @param d
	 *            document instance.
	 * @param name
	 *            element tag name.
	 * @return elementbody string
	 */
    public static String getElementBody(Document d, String name) {
        NodeList nl = d.getDocumentElement().getElementsByTagName(name);
        if (nl.getLength() < 1) return null;
        Element el = (Element) nl.item(0);
        NodeList nl2 = el.getChildNodes();
        if (nl2.getLength() < 1) return null;
        return nl2.item(0).getNodeValue();
    }

    private static Vector pbcCache = new Vector(10);

    private static HashMap pbcBusy = new HashMap(10);

    private static synchronized ProcessBuilderContainer borrowProcessBuilderContainer() {
        Iterator i = pbcCache.iterator();
        while (i.hasNext()) {
            ProcessBuilderContainer pbc = (ProcessBuilderContainer) i.next();
            if (pbcBusy.get(pbc) == null) {
                pbcBusy.put(pbc, "busy");
                log.debug(".borrowProcessBuilderContainer, reusing a ProcessBuilderContainer, there are now " + pbcBusy.size() + " busy ProcessBuilderContainers");
                return pbc;
            }
        }
        ProcessBuilderContainer pbc = new ProcessBuilderContainer();
        pbcBusy.put(pbc, "busy");
        pbcCache.add(pbc);
        log.debug(".borrowProcessBuilder, creating a new ProcessBuilder, there are now " + pbcBusy.size() + " busy ProcessBuilders");
        return pbc;
    }

    private static synchronized void returnProcessBuilderContainer(ProcessBuilderContainer pbc) {
        pbcBusy.remove(pbc);
        log.debug(".returnProcessBuilderContainer, there are now " + pbcBusy.size() + " busy ProcessBuilderContainers");
    }

    /**
	 * Utility function for extracting the additional job specific environement varables from the job instance
	 */
    public static HashMap getAdditionalEnv(org.icenigrid.gridsam.core.plugin.JobContext pContext) {
        return (HashMap) pContext.getJobInstance().getProperties().get("KeyValuePairs");
    }

    /**
	 * Utility fucntion for setting a 'script' to be executable.
	 * This is not very elegant, but gets round the issue where files stored in a war format loose any executions attributes.
	 */
    public static void setExecutable(String filename) {
        File f = new File(filename);
        if (!f.canExecute()) {
            f.setExecutable(true);
        }
    }

    /**
	 * execute the command-line command in the home directory _homedir.
	 * 
	 * @param _cmdline
	 *            command.
	 * @param _homedir
	 *            home directory.
	 * @return stdout content string of this process
	 */
    protected static String execProcess(String _cmdline, String _homedir) {
        return execProcess(_cmdline, _homedir, null);
    }

    protected static String execProcess(String _cmdline, String _homedir, HashMap _env) {
        long start = System.currentTimeMillis();
        Process p = null;
        String stdout = null;
        ProcessBuilderContainer pbc = null;
        try {
            execSemaphore.acquire();
            log.debug("there are " + execSemaphore.getQueueLength() + " threads waiting for execProcess");
            pbc = borrowProcessBuilderContainer();
            String[] args = _cmdline.split(" +");
            setExecutable(args[0]);
            pbc.pb.command(args);
            pbc.pb.directory(new File(_homedir));
            Map<String, String> env = pbc.pb.environment();
            env.clear();
            if (_env != null) {
                Iterator keys = _env.keySet().iterator();
                while (keys.hasNext()) {
                    String k = (String) keys.next();
                    String v = (String) _env.get(k);
                    env.put("GRIDSAM_" + k, v);
                }
            }
            if (fileDriverEnv != null) {
                for (int i = 0; i < fileDriverEnv.length; i++) {
                    if (fileDriverEnv[i] != null && System.getenv(fileDriverEnv[i]) != null) {
                        env.put(fileDriverEnv[i], System.getenv(fileDriverEnv[i]));
                    }
                }
            }
            env.put("GRIDSAM_HOME", System.getProperty("GRIDSAM_HOME"));
            env.put("GRIDSAM_NAME", System.getProperty("GRIDSAM_NAME"));
            env.put("GRIDSAM_USER", System.getProperty("GRIDSAM_USER"));
            p = pbc.pb.start();
            InputStream is = p.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            StringBuffer buffer = new StringBuffer();
            String line;
            while ((line = br.readLine()) != null) {
                buffer.append(line);
                buffer.append('\n');
            }
            stdout = buffer.toString();
            log.debug("Command: " + _cmdline + "\nStdOut : " + stdout);
        } catch (Exception e1) {
            log.error("Error when invoke the script:" + _cmdline + "\n Cause:" + e1.getMessage());
        } finally {
            if (p != null) {
                try {
                    p.getInputStream().close();
                    p.getOutputStream().close();
                    p.getErrorStream().close();
                    p = null;
                } catch (Exception e2) {
                }
            }
            returnProcessBuilderContainer(pbc);
            execSemaphore.release();
            log.debug(".execProcess('" + _cmdline + "', '" + _homedir + "') took " + (System.currentTimeMillis() - start) + " milliseconds");
        }
        return stdout;
    }

    public static String execProcessPublic(String command, String directory, HashMap env) {
        return execProcess(command, directory, env);
    }

    /**
	 * execute the command-line command in the home directory _homedir.
	 * 
	 * @param _cmdline
	 *            command.
	 * @param _homedir
	 *            home directory.
	 * @return stdout content string of this process
	 */
    public static String execProcessbyJNI(String _cmdline, String _homedir) {
        String stdout = null;
        try {
            try {
                String returnStr = jniexec(_cmdline);
                stdout = returnStr;
                String stderr = returnStr;
                log.debug("JNI Command: " + _cmdline + "\nStdOut : " + stdout + "StdErr : " + stderr);
            } catch (Exception e1) {
                log.error("Error when invoke the script:" + _cmdline + "\n Cause:" + e1.getMessage());
            }
        } catch (RuntimeException e) {
            log.error("Error when invoke the script:" + _cmdline + "\n Cause:" + e.getMessage());
        }
        return stdout;
    }

    public static native String jniexec(String msg);

    public static native int jniexeccode(String msg);

    /**
	 * read the content of a file whose path is given in fn value , and change
	 * the format to String.
	 * 
	 * @param fn
	 *            file name.
	 * @return file content string
	 */
    public static String file2String(String fn) {
        File f = new File(fn);
        if (f.exists() && f.length() == 0) {
            return " ";
        }
        Reader theReader = null;
        if (fn == null) return null;
        try {
            theReader = new FileReader(fn);
            StringWriter sw = new StringWriter();
            char[] text = new char[128];
            int n;
            while ((n = theReader.read(text, 0, 128)) > 0) {
                sw.write(text, 0, n);
            }
            String tmp = sw.toString();
            String tmp1 = tmp.replace('\r', '\n');
            String tmp2 = tmp1.replaceAll("\n\n", "\n");
            return tmp2;
        } catch (IOException e) {
            log.error("file2String   " + fn + ":" + e.toString());
            return null;
        } finally {
            if (theReader != null) {
                try {
                    theReader.close();
                } catch (IOException ie) {
                }
                theReader = null;
            }
        }
    }

    /**
	 * handle zip file.
	 * 
	 * @param zipLocation
	 *            zip file path.
	 * @return String[] zip entries
	 * @throws Exception
	 *             fail to list zip entry of the file.
	 */
    public static String[] listZipEntry(String zipLocation) throws Exception {
        ArrayList list = new ArrayList();
        File zipFile = new File(zipLocation);
        ZipInputStream zis = null;
        try {
            zis = new ZipInputStream(new FileInputStream(zipFile));
            for (ZipEntry ze = null; (ze = zis.getNextEntry()) != null; ) {
                String ename = ze.getName();
                ename = ename.substring(ename.indexOf(File.separator) + 1);
                list.add(ename);
            }
        } catch (IOException ioe) {
            throw ioe;
        } finally {
            if (zis != null) try {
                zis.close();
            } catch (IOException e) {
                throw e;
            }
        }
        String[] p = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            p[i] = (String) list.get(i);
        }
        return p;
    }

    /**
	 * unzip . it is for the use of blaster
	 * 
	 * @param zipLocation
	 *            zip file path.
	 * @param toDir
	 *            unzip file to this directory.
	 * @throws Exception
	 *             fail to unzip the file.
	 */
    public static void UnZip(String zipLocation, String toDir) throws Exception {
        File zipFile = new File(zipLocation);
        File dir = new File(toDir);
        ZipInputStream zis = null;
        try {
            zis = new ZipInputStream(new FileInputStream(zipFile));
            for (ZipEntry ze = null; (ze = zis.getNextEntry()) != null; ) UnZipEntry(dir, zis, ze.getName(), ze.isDirectory());
        } catch (IOException ioe) {
            throw ioe;
        } finally {
            if (zis != null) try {
                zis.close();
            } catch (IOException e) {
                throw e;
            }
        }
    }

    /**
	 * unzip.
	 * 
	 * @param dir
	 * @param zinputStream
	 * @param entryName
	 * @param isDirectory
	 * @throws IOException
	 */
    public static void UnZipEntry(File dir, InputStream zinputStream, String entryName, boolean isDirectory) throws IOException {
        File f = new File(dir, entryName);
        try {
            String p = f.getParent();
            File dirParent = null;
            if (p != null) dirParent = new File(p);
            if (dirParent != null) dirParent.mkdirs();
            if (isDirectory) {
                f.mkdirs();
            } else {
                byte buffer[] = new byte[1024];
                int length = 0;
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(f);
                    while ((length = zinputStream.read(buffer)) >= 0) fos.write(buffer, 0, length);
                    fos.close();
                    fos = null;
                } finally {
                    if (fos != null) try {
                        fos.close();
                    } catch (IOException e) {
                    }
                }
            }
        } catch (FileNotFoundException ex) {
        }
    }

    /**
	 * @param list
	 * @param zipname
	 * @param type
	 * @return true if success
	 * @throws Exception
	 */
    public static boolean ZipFiles(List list, String zipname, boolean type) throws Exception {
        String[] files = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            files[i] = (String) list.get(i);
        }
        return ZipFiles(files, zipname, type);
    }

    /**
	 * used when generate original job script
	 * 
	 * @param p
	 * @param h
	 * @return formalizePath string
	 */
    public static String formalizePath(String p, String h) {
        if (p.startsWith("/")) {
            return p;
        }
        if (p.indexOf(":") > -1) {
            return p;
        }
        return h + File.separator + p;
    }

    /**
	 * @param files
	 * @param zipname
	 * @param type
	 * @return true if ZipFiles success
	 * @throws Exception
	 */
    public static boolean ZipFiles(String[] files, String zipname, boolean type) throws Exception {
        FileOutputStream os = new FileOutputStream(zipname);
        ZipOutputStream zip = new ZipOutputStream(os);
        for (int i = 0; i < files.length; i++) {
            File file = new File(files[i]);
            if (file.exists()) {
                byte[] buf = new byte[1024];
                int len;
                ZipEntry zipEntry = new ZipEntry(type ? file.getName() : file.getPath());
                try {
                    FileInputStream fin = new FileInputStream(file);
                    BufferedInputStream in = new BufferedInputStream(fin);
                    zip.putNextEntry(zipEntry);
                    while ((len = in.read(buf)) >= 0) {
                        zip.write(buf, 0, len);
                    }
                    in.close();
                    zip.closeEntry();
                } catch (FileNotFoundException e) {
                } catch (IOException e) {
                }
            }
        }
        zip.close();
        os.close();
        return true;
    }

    public static String splitWorkingDirectory(String in) {
        if (workdirHierarchyLevel == 0) {
            return in;
        }
        StringBuffer out = new StringBuffer();
        for (int i = in.length() - 1; i >= (in.length() - 2 * workdirHierarchyLevel); i -= 2) {
            out.append(in.charAt(i));
            out.append(in.charAt(i - 1));
            out.append(File.separator);
        }
        out.append(in);
        return out.toString();
    }
}

class ProcessBuilderContainer {

    public ProcessBuilder pb;

    public ProcessBuilderContainer() {
        pb = new ProcessBuilder();
        pb.redirectErrorStream(true);
    }
}
