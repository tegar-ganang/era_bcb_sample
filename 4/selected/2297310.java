package hu.sztaki.lpds.pgportal.wfeditor.server;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import javax.servlet.*;
import javax.servlet.http.*;
import com.oreilly.servlet.*;
import hu.sztaki.lpds.pgportal.services.pgrade.*;
import hu.sztaki.lpds.pgportal.services.utils.*;
import hu.sztaki.lpds.pgportal.services.quota.*;
import hu.sztaki.lpds.pgportal.wfeditor.common.jdl.JDLDocument;
import hu.sztaki.lpds.pgportal.wfeditor.common.jdl.JDLList;
import hu.sztaki.lpds.pgportal.wfeditor.common.serialize.WorkflowObject;
import com.oreilly.servlet.multipart.DefaultFileRenamePolicy;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class WorkflowEditorServlet extends HttpServlet {

    private String prefixDir, binDir, usersDir, workflowRepositoryDir;

    private boolean isDebugOn;

    private static final String DEMO_PREFIX_DIR = new String("demoPrefixDir/");

    private boolean isSupportBroker;

    public static final int UPDATE_ACTION_NO_ACTION = 0;

    public static final int UPDATE_ACTION_COPY_DIR = 1;

    public static final int UPDATE_ACTION_DELETE_DIR = 2;

    public static final int UPDATE_ACTION_RENAME_DIR = 3;

    public WorkflowEditorServlet() {
        this.printlnLog(this.getClass().getName() + ".WorkflowEditorServlet()", "Constructor is calling.");
        this.prefixDir = PropertyLoader.getPrefixDir();
        this.printlnLog(this.getClass().getName() + ".WorkflowEditorServlet()", "Prefixdir: " + this.prefixDir);
        this.binDir = new String(prefixDir + "bin/");
        this.usersDir = new String(prefixDir + "users/");
        this.workflowRepositoryDir = new String(prefixDir + "workflowRepository/");
        try {
            this.isSupportBroker = new Boolean(PropertyLoader.getInstance().getProperty("is.support.lcg2.broker")).booleanValue();
        } catch (Exception e) {
            this.isSupportBroker = false;
        }
    }

    private String getUserName(String sessionId) {
        SessionIDManager sm = SessionIDManager.getInstance();
        String userName = sm.getUsername(sessionId);
        return userName;
    }

    private SZGJobManagerConfiguration[] getResourceConfigs(String username) throws Exception {
        SZGJobManagerConfigs cfs = SZGJobManagerConfigs.getInstance(username);
        SZGJobManagerConfiguration[] c = cfs.getConfigurations(true);
        return c;
    }

    private SZGJobManagerConfiguration[] getResourceConfigsForGrid(String aUserName, String aGridName, boolean isMonitored) throws Exception {
        System.out.println("WorkflowEditorServlet.getResourceConfigsForGrid() called.");
        GridJobManagerConfigs cfs = GridJobManagerConfigs.getInstance(aUserName, aGridName);
        System.out.println("WorkflowEditorServlet.getResourceConfigsForGrid():GridJobManagerConfigs.getInstance(" + aUserName + "," + aGridName + ") called.");
        SZGJobManagerConfiguration[] c = cfs.getConfigurations(isMonitored);
        System.out.println("WorkflowEditorServlet.getResourceConfigsForGrid():GridJobManagerConfigs.getConfigurations(" + isMonitored + ") called.");
        return c;
    }

    private String getDebugErrorMessage() {
        try {
            PropertyLoader pl = PropertyLoader.getInstance();
            return pl.getProperty("workflow.editor.show.debug");
        } catch (Exception e) {
            this.printlnLog(this.getClass().getName() + ".getDebugErrorMessage()", "Exception:" + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private String getEditorRole(String sessionId) {
        SessionIDManager sm = SessionIDManager.getInstance();
        String eR = sm.getWorkflowEditorRole(sessionId);
        if (eR.equals("")) return null; else return eR;
    }

    private String getActualUserDir(String userName) {
        return this.usersDir + "" + userName;
    }

    public void printlnLog(String path, String text) {
        java.util.Calendar c = Calendar.getInstance();
        System.out.println("[" + c.get(Calendar.YEAR) + "." + (c.get(Calendar.MONTH) + 1) + "." + c.get(Calendar.DAY_OF_MONTH) + "-" + c.get(Calendar.HOUR_OF_DAY) + ":" + c.get(Calendar.MINUTE) + ":" + c.get(Calendar.SECOND) + "] " + path + "-" + text);
    }

    public void printLog(String path, String text) {
        java.util.Calendar c = Calendar.getInstance();
        System.out.print("[" + c.get(Calendar.YEAR) + "." + (c.get(Calendar.MONTH) + 1) + "." + c.get(Calendar.DAY_OF_MONTH) + "-" + c.get(Calendar.HOUR_OF_DAY) + ":" + c.get(Calendar.MINUTE) + ":" + c.get(Calendar.SECOND) + "] " + path + "-" + text);
    }

    public static BufferedInputStream makeBIS(String fileName) throws IOException {
        FileInputStream fis = new FileInputStream(fileName);
        return new BufferedInputStream(fis, 4096);
    }

    /**
     * This is a shorthand method for easily creating a BufferedOutputStream
     * which is the most efficient way to write to a file - much faster
     * than directy using a FileOutputStream.
     * <p>
     * Don`t forget to call close() on it when you`re done.
     *
     *
     * @param file the file to write to
     * @return a buffered output stream on success
     * @exception IOException if there is a problem opening the file
     *
     * @see BufferedOutputStream
     * @see FileOutputStream
     * @see makeBIS
     */
    public static BufferedOutputStream makeBOS(File file) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        return new BufferedOutputStream(fos, 4096);
    }

    public static void copy(String from, String to) throws IOException {
        BufferedInputStream bis = makeBIS(from);
        File toF = new File(to);
        BufferedOutputStream bos = makeBOS(toF);
        byte[] buf = new byte[4096];
        int nr;
        while ((nr = bis.read(buf, 0, buf.length)) > 0) bos.write(buf, 0, nr);
        try {
            bis.close();
            bos.close();
        } catch (IOException e) {
        }
    }

    private boolean isFileExist(String path) {
        File f = new File(path);
        if (!f.exists()) {
            return false;
        } else {
            return true;
        }
    }

    private boolean isInstrumented(String fileName) {
        boolean isInstrumented = false;
        try {
            File file = new File(fileName);
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            String line;
            Pattern pattern = Pattern.compile("Libgrmon");
            Matcher matcher;
            while ((line = br.readLine()) != null) {
                matcher = pattern.matcher(line);
                if (matcher.find()) {
                    isInstrumented = true;
                    break;
                }
            }
            br.close();
        } catch (IOException ioe) {
            isInstrumented = false;
            this.printlnLog(this.getClass().getName() + ".isInstrumented()", "Failed: IO Exception was occured.");
        }
        return isInstrumented;
    }

    public boolean deleteFileRecursively(File f) {
        boolean b = false;
        try {
            if (!f.exists()) return true;
            this.printlnLog(this.getClass().getName() + ".deleteFileRecursively()", "dir:" + f.getName());
            if (f.isDirectory()) {
                File[] children = f.listFiles();
                for (int i = 0; i < children.length; i++) {
                    this.deleteFileRecursively(children[i]);
                }
            }
            b = f.delete();
            if (b) this.printlnLog(this.getClass().getName() + ".deleteFileRecursively()", "deleting " + f.getName() + " success."); else this.printlnLog(this.getClass().getName() + ".deleteFileRecursively()", "deleting " + f.getName() + " not success.");
        } catch (Exception e) {
            e.printStackTrace();
            e.getMessage();
        }
        return b;
    }

    public String gzipCompress(String fileName, String compressedFileName) {
        String ret = null;
        final int BUFFER = 2048;
        try {
            FileInputStream fis = new FileInputStream(fileName);
            FileOutputStream fos = new FileOutputStream(compressedFileName);
            java.util.zip.GZIPOutputStream zos = new java.util.zip.GZIPOutputStream(new BufferedOutputStream(fos));
            int count;
            byte data[] = new byte[BUFFER];
            while ((count = fis.read(data, 0, BUFFER)) != -1) {
                zos.write(data, 0, count);
            }
            fis.close();
            zos.flush();
            zos.close();
        } catch (Exception e) {
            ret = e.getMessage();
        }
        return ret;
    }

    public String gzipDecompress(String compressedFileName, String FileName) {
        String ret = null;
        final int BUFFER = 2048;
        try {
            FileInputStream fis = new FileInputStream(compressedFileName);
            java.util.zip.GZIPInputStream zis = new java.util.zip.GZIPInputStream(new BufferedInputStream(fis));
            FileOutputStream fos = new FileOutputStream(FileName);
            int count;
            byte data[] = new byte[BUFFER];
            while ((count = zis.read(data, 0, BUFFER)) != -1) {
                fos.write(data, 0, count);
            }
            fos.flush();
            fos.close();
            zis.close();
        } catch (Exception e) {
            ret = e.getMessage();
        }
        return ret;
    }

    public static final String reg_replaceFirst(final String regex, final String replacement, final String subject) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(subject);
        return matcher.replaceFirst(replacement);
    }

    public static final String reg_replaceAll(final String regex, final String replacement, final String subject) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(subject);
        return matcher.replaceAll(replacement);
    }

    public String getExtension(String par) {
        Pattern pattern = Pattern.compile("[.]([^.]*)$");
        Matcher matcher = pattern.matcher(par);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    static final String chopExtension(final String par) {
        Pattern pattern = Pattern.compile("[.][^.]*$");
        Matcher matcher = pattern.matcher(par);
        return matcher.replaceAll("");
    }

    private boolean isWorkflowRunning(String actualUserDir, String workflowName) {
        String path = actualUserDir + "/" + workflowName + "_files/lock";
        if (isFileExist(path)) return true; else return false;
    }

    private boolean isWorkflowRescuable(String userName, String workflowName) {
        SZGWorkflow wf = SZGWorkflowList.getInstance(userName).getWorkflow(workflowName);
        if (wf != null) {
            return wf.canRescue();
        } else {
            return false;
        }
    }

    private boolean isWorkflowDirExist(String actualUserDir, String workflowName) {
        String path = actualUserDir + "/" + workflowName + "_files";
        if (this.isFileExist(path)) return true; else {
            this.printlnLog(this.getClass().getName() + ".isWorkflowDirExist()", "Directory not exist: " + path);
            return false;
        }
    }

    private boolean isWorkflowFileExist(String actualUserDir, String workflowName) {
        String path = actualUserDir + "/" + workflowName + "_files/" + workflowName + "_remote.wrk";
        if (this.isFileExist(path)) return true; else {
            this.printlnLog(this.getClass().getName() + ".isWorkflowFileExist()", "File not exist: " + path);
            return false;
        }
    }

    private String neededWorkflowComponentsExist(String actualUserDir, String workflowName) {
        String returnStr = new String("");
        if (!this.isWorkflowFileExist(actualUserDir, workflowName)) returnStr = "WORKFLOW_FILE_NOT_EXIST"; else returnStr = "EXIST";
        return returnStr;
    }

    private String isWorkflowExist(String actualUserDir, String workflowName) {
        String returnStr = new String("EXIST");
        if (!this.isWorkflowDirExist(actualUserDir, workflowName)) returnStr = "WORKFLOW_DIR_NOT_EXIST"; else returnStr = this.neededWorkflowComponentsExist(actualUserDir, workflowName);
        if (!returnStr.equals("EXIST")) this.printlnLog(this.getClass().getName() + ".isWorkflowExist()", "Workflow: '" + workflowName + "' Status: " + returnStr);
        return returnStr;
    }

    private boolean isTargetWorkflowExist(String actualUserDir, String workflowName) {
        return isWorkflowDirExist(actualUserDir, workflowName);
    }

    private boolean createWorkflowDir(String actualUserDir, String workflowName) {
        File workflowDir = new File(actualUserDir + "/" + workflowName + "_files");
        return workflowDir.mkdirs();
    }

    private boolean deleteDir(String path) {
        File dir = new File(path);
        return dir.delete();
    }

    private String copyWorkflowFiles(String actualUserDir, String sourceWorkflowName, String sourceWorkflowPath, String targetWorkflowPath) {
        this.printlnLog(this.getClass().getName() + ".copyWorkflowFiles()", "sourceWorkflowPath: " + sourceWorkflowPath);
        this.printlnLog(this.getClass().getName() + ".copyWorkflowFiles()", "targetWorkflowPath: " + targetWorkflowPath);
        String returnStr = new String("SUCCESS");
        if (!isWorkflowDirExist(actualUserDir, sourceWorkflowName)) {
            returnStr = "SOURCE_WORKFLOW_DIR_NOT_EXIST";
            this.printlnLog(this.getClass().getName() + ".copyWorkflowFiles()", "Cannot copy workflow files: source workflow dir not exist.");
        } else {
            ArrayList jobList = getJobDirectoriesPath(sourceWorkflowPath);
            String jobName;
            for (int i = 0; i < jobList.size(); i++) {
                jobName = (String) (jobList.get(i));
                if (jobName.compareTo("info") == 0) continue;
                if (!MiscUtils.copyFileRecursively(new File(sourceWorkflowPath + "/" + jobName), new File(targetWorkflowPath + "/" + jobName), false)) {
                    returnStr = "ERROR_IN_COPY";
                    this.printlnLog(this.getClass().getName() + ".copyWorkflowFiles()", "Copying job: " + jobName + " - Failed");
                } else {
                    this.printlnLog(this.getClass().getName() + ".copyWorkflowFiles()", "Copying job: " + jobName + " - Success");
                }
            }
        }
        return returnStr;
    }

    private void writeStrToFile(String filePath, String writeStr) {
        try {
            FileWriter out = new FileWriter(filePath);
            out.write(writeStr);
            out.close();
        } catch (IOException e) {
            this.printlnLog(this.getClass().getName() + ".writeStrToFile()", "Error: cannot write file:" + filePath + ". IOException was occured: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String readFileToStr(String filePath) {
        String returnStr = new String("");
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(filePath));
            String line;
            while ((line = br.readLine()) != null) {
                returnStr += line + "\n";
            }
            br.close();
        } catch (FileNotFoundException fnfe) {
            this.printlnLog(this.getClass().getName() + ".readFileToStr()", "FILE_NOT_FOUND_EXCEPTION: " + filePath);
            returnStr = "FILE_NOT_FOUND_EXCEPTION";
        } catch (IOException ioe) {
            returnStr = "IO_EXCEPTION";
            this.printlnLog(this.getClass().getName() + ".readFileToStr()", "IOExeption:" + filePath);
        }
        return returnStr;
    }

    private ArrayList getJobDirectoriesPath(String dir) {
        ArrayList dirList = new ArrayList();
        File[] fileArray;
        File f = new File(dir);
        fileArray = f.listFiles();
        if (fileArray != null) {
            String remove = new String(dir + "/");
            for (int i = 0; i < fileArray.length; i++) {
                if (fileArray[i].isDirectory() && !fileArray[i].getName().endsWith("_files")) {
                    dirList.add(fileArray[i].getAbsolutePath().substring(remove.length()));
                }
            }
        }
        Collections.sort(dirList);
        return dirList;
    }

    private ArrayList getDirectoriesFromDir(String dir) {
        ArrayList dirList = new ArrayList();
        File[] fileArray;
        String workflowDirName;
        File f = new File(dir);
        fileArray = f.listFiles();
        if (fileArray != null) {
            String remove = new String(dir + "/");
            for (int i = 0; i < fileArray.length; i++) {
                if (fileArray[i].isDirectory()) {
                    System.out.println("!!!!!!!!!!!!!!!!!!!" + fileArray[i].getAbsolutePath());
                    workflowDirName = fileArray[i].getAbsolutePath().substring(remove.length());
                    System.out.println("!!!!!!!!!!!!!!!!!!!" + workflowDirName);
                    dirList.add(workflowDirName);
                }
            }
        }
        Collections.sort(dirList);
        return dirList;
    }

    private void loadWorkflowToPortal(String userName, String workflowName, String jobParams) {
        try {
            java.lang.String s = jobParams;
            java.lang.String[] ss;
            ss = s.split(";");
            String[] oldJobs = SZGWorkflowListGetFacade.getJobNames(userName, workflowName);
            String[] newJobs = new String[ss.length];
            for (int i = 0; i < ss.length; i++) {
                java.lang.String[] ssin = ss[i].split(",");
                String ssin0 = "", ssin1 = "", ssin2 = "", ssin3 = "", ssin4 = null;
                String ssin5;
                if (ssin != null) {
                    if ((ssin.length > 0) && (ssin[0] != null)) ssin0 = ssin[0];
                    if ((ssin.length > 1) && (ssin[1] != null)) ssin1 = ssin[1];
                    if ((ssin.length > 2) && (ssin[2] != null)) ssin2 = ssin[2];
                    if ((ssin.length > 3) && (ssin[3] != null)) ssin3 = ssin[3];
                    if (ssin.length > 4) {
                        ssin4 = ssin[4];
                    }
                    if (ssin.length > 5) {
                        ssin5 = ssin[5];
                        if (ssin0.compareTo("PS=ENV1") == 0) {
                            if (ssin5.trim().length() != 0) ssin1 = ssin5; else ssin1 = "undef";
                        }
                    } else if (ssin.length > 1) {
                        if ((ssin0.compareTo("PS=ENV1") == 0) && (ssin1.compareTo("lfc") == 0)) ssin1 = "undef";
                    }
                }
                newJobs[i] = ssin0;
                this.printlnLog(this.getClass().getName() + ".loadWorkflowToPortal()", ssin0 + "," + ssin1 + "," + ssin2 + "," + ssin3);
                SZGWorkflowListServletFacade.addJobAtSave(userName, ssin0, ssin1, workflowName, ssin2.equals("on") ? true : false, ssin3, true, ssin4 == null ? SZGJob.NORMAL_TYPE : Integer.parseInt(ssin4));
            }
            SZGWorkflowListServletFacade.eliminateOldJobs(userName, workflowName, newJobs, oldJobs);
            String paramList = this.getActualUserDir(userName) + "/" + workflowName + "_files/" + workflowName + "_remote.wrk";
            SZGWorkflowListServletFacade.setWorkflowSubmissionParamlist(userName, workflowName, paramList);
        } catch (Exception ex) {
            this.printlnLog(this.getClass().getName() + ".loadWorkflowToPortal()", "Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private String getWorkflowStatus(String userName, String workflowName) {
        String actualUserDir = this.getActualUserDir(userName);
        String returnStr = new String("");
        String isWE = isWorkflowExist(actualUserDir, workflowName);
        if (!isWE.equals("EXIST")) {
            returnStr = isWE;
        } else if (isWorkflowRunning(actualUserDir, workflowName)) {
            returnStr = "RUNNING";
            this.printlnLog(this.getClass().getName() + ".getWorkflowStatus()", "Status of '" + workflowName + "': " + returnStr);
        } else if (isWorkflowRescuable(userName, workflowName)) {
            returnStr = "RESCUE";
            this.printlnLog(this.getClass().getName() + ".getWorkflowStatus()", "Status of '" + workflowName + "': " + returnStr);
        } else {
            returnStr = "NOT_RUNNING";
        }
        return returnStr;
    }

    private void cleanWorkflowSublibrary(String userName, String workflowName) {
        String workflowPath = PropertyLoader.getPrefixDir() + "users/" + userName + '/' + workflowName + "_files/" + workflowName + "_remote.wrk";
        String scriptPath = PropertyLoader.getPrefixDir() + "bin/clean.sh";
        if (!(new File(scriptPath)).exists()) {
            System.out.println("ERROR: install scipt. It is not found at " + scriptPath);
        } else if ((new File(workflowPath)).exists()) {
            try {
                Process p = Runtime.getRuntime().exec("/bin/bash " + scriptPath + " " + workflowPath);
                System.out.println("clean sh will be executed for user= " + userName + " workflow= " + workflowName);
                p.waitFor();
            } catch (InterruptedException e) {
                System.out.println(e.toString());
            } catch (IOException ee) {
                System.out.println(ee.toString());
            }
            ;
        }
    }

    private void saveWorkflow(String userName, String workflowName, String workflowStr, String outputFilesRemotePathStr, String jobParams, Hashtable aWfParams, String isIncomplete) {
        writeStrToFile(this.getActualUserDir(userName) + "/" + workflowName + "_files/" + "incomplete.dat", isIncomplete);
        writeStrToFile(this.getActualUserDir(userName) + "/" + workflowName + "_files/" + workflowName + "_remote.wrk", workflowStr);
        writeStrToFile(this.getActualUserDir(userName) + "/" + workflowName + "_files/" + "outputFilesRemotePath.dat", outputFilesRemotePathStr);
        String workflowGridName = (String) aWfParams.get("gridName");
        if (workflowGridName != null) {
            writeStrToFile(this.getActualUserDir(userName) + "/" + workflowName + "_files/" + "gridName.dat", workflowGridName);
        }
        loadWorkflowToPortal(userName, workflowName, jobParams);
        cleanWorkflowSublibrary(userName, workflowName);
    }

    private void setResources(String userName, String workflowName, String workflowStr, String outputFilesRemotePathStr, String jobParams, Hashtable aWfParams, String isIncomplete) {
        writeStrToFile(this.getActualUserDir(userName) + "/" + workflowName + "_files/" + workflowName + "_remote.wrk", workflowStr);
        String workflowGridName = (String) aWfParams.get("gridName");
        this.printlnLog(this.getClass().getName() + ".setResources()", "workflowGridName:" + workflowGridName);
        this.printlnLog(this.getClass().getName() + ".setResources()", "jobParams:" + jobParams);
        String[] jobParam = jobParams.split(";");
        for (int i = 0; i < jobParam.length; i++) {
            String[] params = jobParam[i].split(",");
            if (params[3] != null) {
                SZGWorkflowListServletFacade.setJobResource(userName, params[0], params[1], workflowName, (params[2].equals("on") ? true : false), params[3]);
            }
        }
    }

    /**
     * This methods save the upload objects from the given source to the given target.
     * If the target file exists, and the given isDeActivateIfExists true, it will try to deactivate first.
     */
    private void saveObject(File source, File target, boolean isDeActivateIfExists, Hashtable parameters) throws Exception {
        if (!target.getParentFile().exists()) {
            if (!target.mkdirs()) {
                throw new IOException("Cannot create directory");
            }
        }
        if (target.exists() && isDeActivateIfExists) {
            try {
                deActivateObject(target, parameters);
            } catch (Exception e) {
                target.delete();
            }
        }
        FileInputStream fileInput = new FileInputStream(source);
        GZIPInputStream gzipInput = new GZIPInputStream(fileInput);
        BufferedInputStream bufferedInput = new BufferedInputStream(gzipInput);
        FileOutputStream fileOut = new FileOutputStream(target);
        int data;
        while ((data = bufferedInput.read()) != -1) {
            fileOut.write(data);
        }
        bufferedInput.close();
        fileOut.flush();
        fileOut.close();
        printlnLog(this.getClass().getName() + ".saveObject()", "Object saved from: " + source.getPath() + " to target: " + target.getPath());
    }

    /**
     * This method activates the target object.
     */
    private void activateObject(File source, Hashtable parameters) throws Exception {
        FileInputStream fileInput = new FileInputStream(source);
        ObjectInputStream objectInput = new ObjectInputStream(fileInput);
        Object object = objectInput.readObject();
        objectInput.close();
        if (object instanceof WorkflowObject) {
            ((WorkflowObject) object).activate(parameters);
        }
        printlnLog(this.getClass().getName() + ".activateObject()", "Source file: " + source.getPath() + " Class: " + object.getClass().getName());
    }

    /**
     * This method deactivate the target object.
     */
    private void deActivateObject(File source, Hashtable parameters) throws Exception {
        FileInputStream fileInput = new FileInputStream(source);
        ObjectInputStream objectInput = new ObjectInputStream(fileInput);
        Object object = objectInput.readObject();
        objectInput.close();
        if (object instanceof WorkflowObject) {
            ((WorkflowObject) object).deActivate(parameters);
        }
        printlnLog(this.getClass().getName() + ".deActivateObject()", "Source file: " + source.getPath() + " Class: " + object.getClass().getName());
    }

    private ArrayList getWorkflowList(String actualUserDir) {
        ArrayList dirList;
        String item;
        String workflowName;
        dirList = getDirectoriesFromDir(actualUserDir);
        ArrayList wfList = new ArrayList();
        for (int i = 0; i < dirList.size(); i++) {
            item = (String) (dirList.get(i));
            workflowName = reg_replaceFirst("_files", "", item);
            if (neededWorkflowComponentsExist(actualUserDir, workflowName).equals("EXIST")) {
                wfList.add(workflowName);
            }
        }
        return wfList;
    }

    private String getJobStatus(String actualUserDir, String workflowName, String s) {
        String statusBuffer = "";
        java.lang.String[] ss;
        ss = s.split(";");
        BufferedReader br;
        String line;
        File file;
        for (int i = 0; i < ss.length; i++) {
            file = new File(actualUserDir + "/" + workflowName + "_files/" + ss[i] + ".status");
            if (file.exists()) {
                try {
                    br = new BufferedReader(new FileReader(file));
                    if ((line = br.readLine()) != null) {
                        statusBuffer = statusBuffer.concat(ss[i] + ";" + line.trim() + "\n");
                    }
                    br.close();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            } else {
                statusBuffer = statusBuffer.concat(ss[i] + ";" + "Error" + ";" + "Cannot find " + ss[i] + ".status file." + "\n");
                this.printlnLog(this.getClass().getName() + ".getJobStatus()", ss[i] + ": Cannot find '" + ss[i] + ".status' file.");
            }
        }
        return statusBuffer;
    }

    private String getAttachedWrkFileContent(String actualUserDir, String attachedWorkflowName) {
        String attachedWorkflowFile = actualUserDir + "/" + attachedWorkflowName + "_files/" + attachedWorkflowName + "_remote.wrk";
        String returnStr = new String("");
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(attachedWorkflowFile));
        } catch (FileNotFoundException fnfe) {
            this.printlnLog(this.getClass().getName() + ".getAttachedWrkFileContent()", "Cannot find workflow file: " + attachedWorkflowFile);
            returnStr = "Error;" + "Cannot attach to the " + attachedWorkflowName + " workflow.\n";
        }
        try {
            String line;
            while ((line = br.readLine()) != null) {
                returnStr += line + "<br>";
            }
            br.close();
            this.printlnLog(this.getClass().getName() + ".getAttachedWrkFileContent()", "Status: Success.");
        } catch (IOException ioe) {
            this.printlnLog(this.getClass().getName() + ".getAttachedWrkFileContent()", "IOException was occured while try to read workflow.");
            returnStr = "Error;" + "Cannot attach to the " + attachedWorkflowName + " workflow.\n";
        }
        return returnStr;
    }

    private String processUpdateAction(int action, String workflowDir, String[] item) {
        String returnStr = new String("");
        if (action == WorkflowEditorServlet.UPDATE_ACTION_COPY_DIR) {
            if (MiscUtils.copyFileRecursively(new File(workflowDir + "" + item[1]), new File(workflowDir + "" + item[2]), false)) {
                this.clearOldJobDescs(new File(workflowDir + "" + item[2]).getAbsolutePath());
                returnStr += item[0] + ";" + item[1] + ";" + "1" + ";" + "" + "\n";
                this.printlnLog(this.getClass().getName() + ".processUpdateAction()", "Action:COPY_DIR Source:" + item[1] + " Target:" + item[2] + " Status:Success");
            } else {
                returnStr += item[0] + ";" + item[1] + ";" + "0" + ";" + "ERROR_IN_COPY" + "\n";
                this.printlnLog(this.getClass().getName() + ".processUpdateAction()", "Action:COPY_DIR Source:" + item[1] + " Target:" + item[2] + " Status:Error");
            }
        } else if (action == WorkflowEditorServlet.UPDATE_ACTION_DELETE_DIR) {
            boolean b = MiscUtils.deleteFileRecursively(new File(workflowDir + "" + item[1]));
            if (b) {
                returnStr += item[0] + ";" + item[1] + ";" + "1" + ";" + "" + "\n";
                this.printlnLog(this.getClass().getName() + ".processUpdateAction()", "Action:DELETE_DIR Source:" + item[1] + " Status:Success");
            } else {
                returnStr += item[0] + ";" + item[1] + ";" + "0" + ";" + "ERROR_IN_DELETE" + "\n";
                this.printlnLog(this.getClass().getName() + ".processUpdateAction()", "Action:DELETE_DIR Source:" + item[1] + " Status:Error");
            }
        } else if (action == WorkflowEditorServlet.UPDATE_ACTION_RENAME_DIR) {
            boolean isCp = MiscUtils.copyFileRecursively(new File(workflowDir + "" + item[1]), new File(workflowDir + "" + item[2]), false);
            boolean isD = MiscUtils.deleteFileRecursively(new File(workflowDir + "" + item[1]));
            if (isCp && isD) {
                this.clearOldJobDescs(new File(workflowDir + "" + item[2]).getAbsolutePath());
                returnStr += item[0] + ";" + item[1] + ";" + "1" + ";" + "" + "\n";
                this.printlnLog(this.getClass().getName() + ".processUpdateAction()", "Action:RENAME_DIR Source:" + item[1] + " Target:" + item[2] + " Status:Success");
            } else {
                returnStr += item[0] + ";" + item[1] + ";" + "1" + ";" + "ERROR_IN_RENAME" + "\n";
                this.printlnLog(this.getClass().getName() + ".processUpdateAction()", "Action:RENAME_DIR Source:" + item[1] + " Target:" + item[2] + " Status:Error");
            }
        }
        return returnStr;
    }

    private String updateFiles(String workflowDir, String dirLists) {
        String returnStr = new String("");
        String[] dirList = dirLists.split(";");
        String[] item;
        int action;
        for (int i = 0; i < dirList.length; i++) {
            item = dirList[i].split(",");
            try {
                action = Integer.parseInt(item[0]);
                returnStr += this.processUpdateAction(action, workflowDir, item);
            } catch (NumberFormatException nfe) {
                returnStr += item[0] + ";" + item[1] + ";" + "0" + ";" + "ERROR_IN_UPDATE" + "\n";
                this.printlnLog(this.getClass().getName() + ".updateFiles()", item[1] + ": Cannot identify the action [" + item[0] + "]: NumberFormatException.");
            }
        }
        return returnStr;
    }

    private String getGrids(String userName) {
        String returnStr = "";
        GridConfigs gridConfigs = GridConfigs.getInstance();
        GridConfiguration[] gc = gridConfigs.getGridConfigs();
        for (int i = 0; i < gc.length; i++) {
            returnStr += gc[i].getName() + ";\n";
        }
        return returnStr;
    }

    private String getGridNameForWorkflow(String aUserName, String aWorkflowName) {
        String gridName = SZGWorkflowListGetFacade.getGridName(aUserName, aWorkflowName);
        this.printlnLog(this.getClass().getName() + ".getGridNameForWorkflow(" + aUserName + "," + aWorkflowName + ")", "gridName:" + gridName);
        if (gridName == null) return "Error;NO_GIRD_NAME_FOR_WORKFLOW"; else if (gridName.equals("")) return "Error;NO_GIRD_NAME_FOR_WORKFLOW"; else return gridName;
    }

    private String getWorkflowParameters(String aUserName, String aWorkflowName) {
        String returnStr = "";
        returnStr += "gridName;" + this.getGridNameForWorkflow(aUserName, aWorkflowName) + "\n";
        return returnStr;
    }

    private String getResourcesForGrid(String aUserName, String aGridName, boolean isMonitored) {
        String returnStr = "";
        try {
            SZGJobManagerConfiguration[] resources;
            resources = this.getResourceConfigsForGrid(aUserName, aGridName, isMonitored);
            String line;
            if (resources != null && resources.length > 0) {
                for (int i = 0; i < resources.length; i++) {
                    line = resources[i].getContactString() + ";" + resources[i].getJobManager() + ";" + resources[i].isMonitorable() + "\n";
                    returnStr += line;
                }
                this.printlnLog(this.getClass().getName() + ".getResourcesForGrid()", "Status: Success.");
            } else {
                returnStr = "Error;No resource found for '" + aGridName + "'.";
                this.printlnLog(this.getClass().getName() + ".getResourcesForGrid()", "Error: No resource.");
            }
        } catch (Exception ex) {
            returnStr = "Error;No resource for grid.";
            this.printlnLog(this.getClass().getName() + ".getResourcesForGrid()", "Exception was catched: " + ex.getMessage());
            ex.printStackTrace();
        }
        return returnStr;
    }

    private String getGridForJobs(String aUserName, String aWorkflowName, String jobParams) {
        String returnStr = "";
        String[] lines;
        lines = jobParams.split(";");
        String gridName = "";
        for (int i = 0; i < lines.length; i++) {
            this.printLog(this.getClass().getName() + ".getGridForJobs()", "job[" + i + "]:" + lines[i] + " - gridName: ");
            try {
                gridName = SZGWorkflowListGetFacade.getGridNameForJob(aUserName, aWorkflowName, lines[i]);
                System.out.println(gridName + ".");
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (gridName != null) {
                returnStr += lines[i] + ";" + gridName + "\n";
            } else {
                returnStr += lines[i] + ";" + "Error" + ";" + "No grid name for this job." + "\n";
                this.printlnLog(this.getClass().getName() + ".getGridForJobs()", lines[i] + ": No grid name for this job.");
            }
        }
        return returnStr;
    }

    private void cmdUpload(MultipartRequest multi, HttpServletResponse res, String userName) throws ServletException, IOException {
        String remoteDirName = null;
        String command;
        String targetDirectory = new String("");
        boolean isSessionValid = false;
        String workflowName;
        res.setContentType("text/html");
        PrintWriter out = res.getWriter();
        try {
            command = multi.getParameter("command");
            workflowName = multi.getParameter("workflow");
            SZGWorkflowListGetFacade.getWorkflow(userName, workflowName).setIncomplete(true);
            Enumeration params = multi.getParameterNames();
            while (params.hasMoreElements()) {
                String name = (String) params.nextElement();
                if (name.equals("targetDirectory")) {
                    targetDirectory = multi.getParameter(name);
                }
                if (name.equals("sessionId")) {
                    userName = this.getUserName(multi.getParameter(name));
                    if (userName != null) {
                        isSessionValid = true;
                    } else {
                        isSessionValid = false;
                    }
                }
            }
            if (isSessionValid) {
                System.out.println(" - userName: " + userName);
                remoteDirName = this.getActualUserDir(userName) + "/" + targetDirectory;
                if (remoteDirName != null) {
                    File remoteDir = new File(remoteDirName);
                    remoteDir.mkdirs();
                }
                Enumeration files = multi.getFileNames();
                while (files.hasMoreElements()) {
                    String name = (String) files.nextElement();
                    String filename = multi.getFilesystemName(name);
                    String original = multi.getOriginalFileName(name);
                    String type = multi.getContentType(name);
                    File f = multi.getFile(name);
                    this.printlnLog(this.getClass().getName() + ".cmdUpload()", "Uploading file: " + remoteDirName + "/" + name);
                    if (remoteDirName != null) {
                        clearDir(remoteDirName);
                        out.println("Status;DECOMPRESS_START");
                        out.flush();
                        this.printlnLog(this.getClass().getName() + ".cmdUpload()", "Status: Decompress is starting.");
                        gzipDecompress(f.getAbsolutePath(), remoteDirName + "/" + name);
                        out.println("Status;DECOMPRESS_STOP");
                        out.flush();
                        this.printlnLog(this.getClass().getName() + ".cmdUpload()", "Status: Upload finished.");
                        Process p = Runtime.getRuntime().exec("/bin/chmod a+x " + remoteDirName + "/" + name);
                        f.delete();
                        out.println("Status;SUCCESS");
                        SZGWorkflowListGetFacade.getWorkflow(userName, workflowName).setIncomplete(false);
                    } else {
                        out.println("Error;Error");
                        this.printlnLog(this.getClass().getName() + ".cmdUpload()", "Error: no remoteDirName");
                    }
                }
            } else {
                out.println("Error;NO_USERNAME_TO_SESSION");
                this.printlnLog(this.getClass().getName() + ".cmdUpload()", "Error: No user name to session.");
            }
        } catch (Exception e) {
            out.println("Error;Error " + e.getMessage());
            e.printStackTrace();
            this.printlnLog(this.getClass().getName() + ".cmdUpload()", "Exception: " + e.getMessage());
        } finally {
            out.close();
        }
        ((userBean) quotaStaticService.getInstance().getUserBeans().get(userName)).realocateWorkflowSize(targetDirectory.split("_file")[0]);
    }

    /**
     * This method gets the uploaded Object(s) from the client
     * If request parameters contains a true isSave, the save the object to the given target in the user's dir.
     * If request parameters contains a true isActivate, the portal server activates it.
     */
    private void cmdUploadObject(MultipartRequest multiRequest, HttpServletResponse response, String userName) throws ServletException, IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        Hashtable parameters = new Hashtable();
        String workflowName = multiRequest.getParameter("workflowName");
        String targetDirectoryName = multiRequest.getParameter("targetDirectory");
        boolean isSave = new Boolean(multiRequest.getParameter("isSave")).booleanValue();
        boolean isActivate = new Boolean(multiRequest.getParameter("isActivate")).booleanValue();
        boolean isDeActivateIfExists = new Boolean(multiRequest.getParameter("isDeActivateIfExists")).booleanValue();
        String actualUserDirectoryName = this.getActualUserDir(userName);
        File targetDirectory;
        if (this.isTargetWorkflowExist(actualUserDirectoryName, workflowName)) {
            targetDirectory = new File(actualUserDirectoryName + "/" + workflowName + "_files", targetDirectoryName);
        } else {
            targetDirectory = new File(actualUserDirectoryName, targetDirectoryName);
        }
        Enumeration params = multiRequest.getParameterNames();
        while (params.hasMoreElements()) {
            String name = (String) params.nextElement();
            parameters.put(name, multiRequest.getParameter(name));
        }
        parameters.put("targetDirectory", targetDirectory.getPath());
        Enumeration files = multiRequest.getFileNames();
        while (files.hasMoreElements()) {
            String name = (String) files.nextElement();
            File sourceFile = multiRequest.getFile(name);
            String originalName = multiRequest.getOriginalFileName(name);
            String type = multiRequest.getContentType(name);
            printlnLog(this.getClass().getName() + ".cmdUploadObject()", "Path: " + targetDirectory.getPath() + "/" + originalName + " Name: " + name + " File: " + sourceFile.getAbsolutePath() + " Type: " + type);
            try {
                if (isSave) {
                    File targetFile = new File(targetDirectory, originalName);
                    this.saveObject(sourceFile, targetFile, isDeActivateIfExists, parameters);
                    if (isActivate) {
                        this.activateObject(targetFile, parameters);
                    }
                }
                sourceFile.delete();
            } catch (Exception e) {
                response.sendError(response.SC_INTERNAL_SERVER_ERROR, "Server error: " + e.toString());
                printlnLog(this.getClass().getName() + ".cmdUploadObject()", "Exception: " + e.toString());
            }
        }
        out.write("OK!");
    }

    /**
     * This method download an object from the request file in user's dir to client
     * If request parameters contains a true isDeActivate, the portal server deactivates the object.
     */
    private void cmdDownloadObject(HttpServletRequest request, HttpServletResponse response, String userName) throws ServletException, IOException {
        response.setContentType("application/x-java-object");
        Hashtable parameters = new Hashtable();
        String workflowName = request.getParameter("workflowName");
        String sourceDirectoryName = request.getParameter("sourceDirectory");
        String sourceFileName = request.getParameter("sourceFile");
        String actualUserDirectoryName = this.getActualUserDir(userName);
        boolean isDeActivate = new Boolean(request.getParameter("isDeActivate")).booleanValue();
        boolean isDelete = new Boolean(request.getParameter("isDelete")).booleanValue();
        File sourceDirectory;
        if (this.isTargetWorkflowExist(actualUserDirectoryName, workflowName)) {
            sourceDirectory = new File(actualUserDirectoryName + "/" + workflowName + "_files", sourceDirectoryName);
        } else {
            sourceDirectory = new File(actualUserDirectoryName, sourceDirectoryName);
        }
        File sourceFile = new File(sourceDirectory, sourceFileName);
        Enumeration params = request.getParameterNames();
        while (params.hasMoreElements()) {
            String name = (String) params.nextElement();
            parameters.put(name, request.getParameter(name));
        }
        parameters.put("sourceDirectory", sourceDirectory.getPath());
        try {
            if (isDeActivate) {
                this.deActivateObject(sourceFile, parameters);
            }
            ServletOutputStream out = response.getOutputStream();
            GZIPOutputStream gzipOut = new GZIPOutputStream(out);
            ServletUtils.returnFile(sourceFile.getAbsolutePath(), gzipOut);
            gzipOut.finish();
            gzipOut.close();
            if (isDelete) {
                sourceFile.delete();
            }
        } catch (FileNotFoundException fnfEx) {
            response.sendError(response.SC_NOT_FOUND, "Source file not found !" + sourceFile.getName());
        } catch (Exception e) {
            response.sendError(response.SC_INTERNAL_SERVER_ERROR, "Server error: " + e.toString());
            printlnLog(this.getClass().getName() + ".cmdUploadObject()", "Exception: " + e.toString());
        }
    }

    private void cmdGetPSProps(HttpServletRequest req, HttpServletResponse res, String userName) throws ServletException, IOException {
        res.setContentType("text/html");
        PrintWriter out = res.getWriter();
        String workflowName = req.getParameter("workflowName");
        try {
            String actualUserDir = this.getActualUserDir(userName);
            String fileName = actualUserDir + "/" + workflowName + "_files/" + ".PS=PROPS1.desc";
            File file = new File(fileName);
            BufferedReader reader = new BufferedReader(new FileReader(file));
            out.println(reader.readLine());
            reader.close();
            this.printlnLog(this.getClass().getName() + ".cmdGetPSProps()", workflowName + " .PS=PROPS1.desc open success!");
            fileName = actualUserDir + "/" + workflowName + "_files/" + ".PS=ENV1.desc";
            file = new File(fileName);
            if (file.exists()) {
                reader = new BufferedReader(new FileReader(file));
                out.println(reader.readLine());
                reader.close();
                this.printlnLog(this.getClass().getName() + ".cmdGetPSProps()", workflowName + " .PS=ENV1.desc open success!");
            }
        } catch (Exception e) {
            this.printlnLog(this.getClass().getName() + ".cmdGetPSProps()", "Error while opening " + workflowName + " .PS=PROPS1.desc! Reason:" + e.getMessage());
            out.println("Error;" + "Cannot open worklow's PS properties!");
        }
    }

    private void cmdSaveWorkflow(HttpServletRequest req, HttpServletResponse res, String userName) throws ServletException, IOException {
        res.setContentType("text/html");
        PrintWriter out = res.getWriter();
        Hashtable wfParams = new Hashtable();
        String workflowName = req.getParameter("workflowName");
        String wfNotSubmitable = req.getParameter("workflowIncomplete");
        String workflowStr = req.getParameter("workflowStr");
        String outputFilesRemotePathStr = req.getParameter("outputFilesRemotePathStr");
        String jobParams = req.getParameter("jobParams");
        String gridName = req.getParameter("gridName");
        if (gridName != null) {
            wfParams.put("gridName", gridName);
        }
        this.printlnLog(this.getClass().getName() + ".cmdSaveWorkflow()", "Saving '" + workflowName + "'.");
        String returnStr = this.getWorkflowStatus(userName, workflowName);
        if (returnStr.equals("RUNNING")) {
            out.println("Error;WORKFLOW_IS_RUNNING");
            this.printlnLog(this.getClass().getName() + ".cmdSaveWorkflow()", "Cannot save '" + workflowName + "'. Worfklow is running.");
        } else if (returnStr.equals("NOT_RUNNING")) {
            saveWorkflow(userName, workflowName, workflowStr, outputFilesRemotePathStr, jobParams, wfParams, wfNotSubmitable);
            out.println("Status;SUCCESS");
            this.printlnLog(this.getClass().getName() + ".cmdSaveWorkflow()", "Save '" + workflowName + "' success.");
        } else {
            out.println("Error;" + returnStr);
            this.printlnLog(this.getClass().getName() + ".cmdSaveWorkflow()", "Cannot save '" + workflowName + "'. Reason: " + returnStr);
        }
        userBean ubean = quotaStaticService.getInstance().getUserBean(userName);
        if (ubean.getWorkflow(workflowName) == null) {
            ubean.addWorkflow(workflowName);
        }
        ubean.realocateWorkflowSize(workflowName);
        out.close();
    }

    private void cmdSetResources(HttpServletRequest req, HttpServletResponse res, String userName) throws ServletException, IOException {
        res.setContentType("text/html");
        PrintWriter out = res.getWriter();
        Hashtable wfParams = new Hashtable();
        String workflowName = req.getParameter("workflowName");
        String wfNotSubmitable = req.getParameter("workflowIncomplete");
        String workflowStr = req.getParameter("workflowStr");
        String outputFilesRemotePathStr = req.getParameter("outputFilesRemotePathStr");
        String jobParams = req.getParameter("jobParams");
        String gridName = req.getParameter("gridName");
        this.printlnLog(this.getClass().getName() + ".cmdSetResources()", "gridName: " + gridName);
        if (gridName != null) {
            wfParams.put("gridName", gridName);
        }
        this.printlnLog(this.getClass().getName() + ".cmdSetResources()", "Setting resource for '" + workflowName + "'.");
        String returnStr = this.getWorkflowStatus(userName, workflowName);
        if (returnStr.equals("RUNNING")) {
            out.println("Error;WORKFLOW_IS_RUNNING");
            this.printlnLog(this.getClass().getName() + ".cmdSetResources()", "Cannot set resources for '" + workflowName + "'. Worfklow is running.");
        } else if (returnStr.equals("RESCUE")) {
            setResources(userName, workflowName, workflowStr, outputFilesRemotePathStr, jobParams, wfParams, wfNotSubmitable);
            out.println("Status;SUCCESS");
            this.printlnLog(this.getClass().getName() + ".cmdSetResources()", "Setting resources '" + workflowName + "' success.");
        } else {
            out.println("Error;" + returnStr);
            this.printlnLog(this.getClass().getName() + ".cmdSetResources()", "Cannot set resources for '" + workflowName + "'. Reason: " + returnStr);
        }
        out.close();
    }

    private void cmdSaveAsWorkflow(HttpServletRequest req, HttpServletResponse res, String userName) throws ServletException, IOException {
        res.setContentType("text/html");
        PrintWriter out = res.getWriter();
        Hashtable wfParams = new Hashtable();
        String sourceWorkflowName = req.getParameter("sourceWorkflowName");
        String targetWorkflowName = req.getParameter("targetWorkflowName");
        String wfNotSubmitable = req.getParameter("workflowIncomplete");
        String workflowStr = req.getParameter("workflowStr");
        String outputFilesRemotePathStr = req.getParameter("outputFilesRemotePathStr");
        String jobParams = req.getParameter("jobParams");
        String gridName = req.getParameter("gridName");
        if (gridName != null) {
            wfParams.put("gridName", gridName);
        }
        String returnStr = new String("SUCCESS");
        String actualUserDir = this.getActualUserDir(userName);
        this.printlnLog(this.getClass().getName() + ".cmdSaveAsWorkflow()", "Saving '" + sourceWorkflowName + "' as '" + targetWorkflowName + "'.");
        if (isTargetWorkflowExist(actualUserDir, targetWorkflowName)) {
            returnStr = "TARGET_WORKFLOW_EXIST";
            this.printlnLog(this.getClass().getName() + ".cmdSaveAsWorkflow()", "Cannot save as worfklow: Target workflow has already existed.");
        } else {
            if (createWorkflowDir(actualUserDir, targetWorkflowName)) {
                if (!sourceWorkflowName.equals("")) {
                    returnStr = copyWorkflowFiles(actualUserDir, sourceWorkflowName, actualUserDir + "/" + sourceWorkflowName + "_files", actualUserDir + "/" + targetWorkflowName + "_files");
                }
                saveWorkflow(userName, targetWorkflowName, workflowStr, outputFilesRemotePathStr, jobParams, wfParams, wfNotSubmitable);
            } else {
                returnStr = "CANT_CREATE_TARGET_DIR";
                this.printlnLog(this.getClass().getName() + ".cmdSaveAsWorkflow()", "Cannot save as worfklow: Cannot create target dir.");
            }
        }
        if (!sourceWorkflowName.equals(targetWorkflowName)) {
            setInitWorkflow diSet = new setInitWorkflow(userName, targetWorkflowName);
            diSet.deleteFiles();
        }
        quotaStaticService.getInstance().reallocateUserWorkflowSpace(userName, targetWorkflowName);
        if (returnStr.equals("SUCCESS")) {
            returnStr = "Status;SUCCESS";
            this.printlnLog(this.getClass().getName() + ".cmdSaveAsWorkflow()", "Status: Success.");
        } else {
            returnStr = "Error;" + returnStr;
        }
        out.println(returnStr);
        out.close();
    }

    private void cmdRefresh(HttpServletRequest req, HttpServletResponse res, String userName) throws ServletException, IOException {
        res.setContentType("text/html");
        PrintWriter out = res.getWriter();
        String s = req.getParameter("jobParams");
        String workflowName = req.getParameter("workflow");
        String actualUserDir = this.getActualUserDir(userName);
        String returnStr = getWorkflowStatus(userName, workflowName);
        if (returnStr.equals("RUNNING")) {
            returnStr = "Status;RUNNING\n";
            returnStr += getJobStatus(actualUserDir, workflowName, s);
        } else if (returnStr.equals("RESCUE")) {
            returnStr = "Status;RESCUE\n";
            returnStr += getJobStatus(actualUserDir, workflowName, s);
        } else if (returnStr.equals("NOT_RUNNING")) {
            returnStr = "Status;NOT_RUNNING";
        } else {
            returnStr = "Error;" + returnStr;
            this.printlnLog(this.getClass().getName() + ".cmdRefresh()", "Cannot get status information about '" + workflowName + "'. Reason: " + returnStr);
        }
        this.printlnLog(this.getClass().getName() + ".cmdRefresh()", "'" + workflowName + "' : " + returnStr);
        out.println(returnStr);
        out.close();
    }

    private void cmdGetParameters(HttpServletRequest req, HttpServletResponse res, String userName) throws ServletException, IOException {
        res.setContentType("text/html");
        PrintWriter out = res.getWriter();
        if (req.getParameter("usersDir").equals("1")) {
            if (usersDir.equals(null) || usersDir.equals("")) {
                out.println("Error;NO_USERS_DIR_PARAMETER");
                this.printlnLog(this.getClass().getName() + ".cmdGetParameters()", "usersDir: " + this.usersDir);
            } else {
                out.println("usersDir;" + usersDir + userName + "/");
                this.printlnLog(this.getClass().getName() + ".cmdGetParameters()", "usersDir: " + this.usersDir + userName + "/");
            }
        }
        if (req.getParameter("debugErrorMessage").equals("1")) {
            String debugErrorMessage = getDebugErrorMessage();
            this.printlnLog(this.getClass().getName() + ".cmdGetParameters()", "debugErrorMessage: " + debugErrorMessage);
            if (debugErrorMessage.equals(null) || debugErrorMessage.equals("")) out.println("Error;NO_DEBUG_ERROR_MESSAGE_PARAMETER"); else out.println("debugErrorMessage;" + debugErrorMessage);
        }
        if (req.getParameter("editorRole").equals("1")) {
            String eR = this.getEditorRole(req.getParameter("sessionId"));
            this.printlnLog(this.getClass().getName() + ".cmdGetParameters()", "editorRole: " + eR);
            if (eR != null) out.println("editorRole;" + eR); else out.println("Error;NO_EDITOR_ROLE_PARAMETER");
        }
        if (req.getParameter("isSupportBroker").equals("1")) {
            this.printlnLog(this.getClass().getName() + ".cmdGetParameters()", "isSupportBroker: " + this.isSupportBroker);
            out.println("isSupportBroker;" + this.isSupportBroker);
        }
        out.close();
    }

    private void cmdGetResources(HttpServletRequest req, HttpServletResponse res, String userName) throws ServletException, IOException {
        res.setContentType("text/html");
        PrintWriter out = res.getWriter();
        String returnStr = new String("");
        try {
            SZGJobManagerConfiguration[] resources;
            resources = getResourceConfigs(userName);
            if (resources != null && resources.length > 0) {
                for (int i = 0; i < resources.length; i++) {
                    returnStr += resources[i].getContactString() + ";" + resources[i].getJobManager() + ";" + resources[i].isMonitorable() + "\n";
                }
                this.printlnLog(this.getClass().getName() + ".cmdGetResources()", "Status: Success.");
            } else {
                returnStr = "Error;No resource.";
                this.printlnLog(this.getClass().getName() + ".cmdGetResources()", "Error: No resource.");
            }
        } catch (Exception ex) {
            returnStr = "Error;No resource.";
            this.printlnLog(this.getClass().getName() + ".cmdGetResources()", "Exception was catched: " + ex.getMessage());
            ex.printStackTrace();
        }
        System.out.println(this.getClass().getName() + "cmdGetResources()-returnStr:" + returnStr);
        out.println(returnStr);
        out.close();
    }

    private void cmdGetAttachedWrkFileContent(HttpServletRequest req, HttpServletResponse res, String userName) throws ServletException, IOException {
        res.setContentType("text/html");
        PrintWriter out = res.getWriter();
        String actualUserDir = this.getActualUserDir(userName);
        String attachedWorkflowName = req.getParameter("attachedWorkflowName");
        this.printlnLog(this.getClass().getName() + ".cmdGetAttachedWrkFileContent()", "Get workflow file content for: " + attachedWorkflowName);
        String returnStr = this.getAttachedWrkFileContent(actualUserDir, attachedWorkflowName);
        out.println(returnStr);
        out.close();
    }

    private void cmdGetSizeAcceptance(HttpServletRequest req, HttpServletResponse res, String userName) throws ServletException, IOException {
        res.setContentType("text/html");
        PrintWriter out = res.getWriter();
        String sizeOfFile = req.getParameter("sizeRequired");
        String returnStr = "SIZE_ERROR";
        try {
            if (quotaStaticService.getInstance().getUserBean(userName).isStorageHasFreeSpaceFor(Long.parseLong(sizeOfFile))) {
                returnStr = "OK";
            } else {
                userBean ub = quotaStaticService.getInstance().getUserBean(userName);
                returnStr = "SIZE_ERROR" + ";" + MiscUtils.getSpaceSizeFromByte(ub.getQuotaeSize()) + ";" + MiscUtils.getSpaceSizeFromByte(ub.getStorageSize());
            }
        } catch (NumberFormatException e) {
        }
        out.println(returnStr);
        out.close();
    }

    private void cmdGetSizeOfWorkflow(HttpServletRequest req, HttpServletResponse res, String userName) throws ServletException, IOException {
        res.setContentType("text/html");
        PrintWriter out = res.getWriter();
        String workflowName = req.getParameter("workflowName");
        System.out.println("cmdGetSizeOfWorkflow name=" + workflowName);
        long discNeed = quotaStaticService.getInstance().getUserBean(userName).getWorkflow(workflowName).getAllWorkflowSize();
        System.out.println("cmdGetSizeOfWorkflow name=" + workflowName + " result=" + discNeed);
        String returnStr = "OK" + discNeed;
        out.println(returnStr);
        out.close();
    }

    private void cmdGetWorkflowStatus(HttpServletRequest req, HttpServletResponse res, String userName) throws ServletException, IOException {
        res.setContentType("text/html");
        PrintWriter out = res.getWriter();
        String workflowName = req.getParameter("workflowName");
        String returnStr = getWorkflowStatus(userName, workflowName);
        if (returnStr.equals("RUNNING")) out.println("Status;RUNNING"); else if (returnStr.equals("RESCUE")) out.println("Status;RESCUE"); else if (returnStr.equals("NOT_RUNNING")) out.println("Status;NOT_RUNNING"); else out.println("Error;" + returnStr);
        out.close();
    }

    private void cmdGetWorkflowList(HttpServletRequest req, HttpServletResponse res, String userName) throws ServletException, IOException {
        res.setContentType("text/html");
        PrintWriter out = res.getWriter();
        String actualUserDir = this.getActualUserDir(userName);
        String returnStr = new String("");
        ArrayList wfList = this.getWorkflowList(actualUserDir);
        for (int i = 0; i < wfList.size(); i++) {
            returnStr += (String) wfList.get(i) + "\n";
        }
        out.println(returnStr);
        out.close();
    }

    private void cmdCopyDemoWorkflowToUsersDir(HttpServletRequest req, HttpServletResponse res, String userName) throws ServletException, IOException {
        res.setContentType("text/html");
        PrintWriter out = res.getWriter();
        String workflowName = req.getParameter("workflowName");
        String returnStr = new String("SUCCESS");
        this.printlnLog(this.getClass().getName() + ".copyWorkflowFiles()", "Copying '" + workflowName + "' workflow to '" + userName + "' user's dir:");
        String actualUserDir = this.getActualUserDir(userName);
        if (isTargetWorkflowExist(actualUserDir, workflowName)) {
            returnStr = "TARGET_WORKFLOW_EXIST";
            this.printlnLog(this.getClass().getName() + ".cmdCopyDemoWorkflowToUsersDir()", "Target workflow '" + workflowName + "' has already existed; hasn't copyed anything.");
        } else {
            this.printlnLog(this.getClass().getName() + ".cmdCopyDemoWorkflowToUsersDir()", "Copying files: ");
            if (!MiscUtils.copyFileRecursively(new java.io.File(workflowRepositoryDir + workflowName + "_files"), new java.io.File(actualUserDir + "/" + workflowName + "_files"), false)) {
                returnStr = "ERROR_IN_COPY";
                this.printlnLog(this.getClass().getName() + ".cmdCopyDemoWorkflowToUsersDir()", "Copy failed.");
            } else {
                this.printlnLog(this.getClass().getName() + ".cmdCopyDemoWorkflowToUsersDir()", "Copy success.");
            }
            String wrkStr = readFileToStr(usersDir + userName + "/" + workflowName + "_files/" + workflowName + "_remote.wrk");
            String outputFilesRemotePathStr = readFileToStr(usersDir + userName + "/" + workflowName + "_files/outputFilesRemotePath.dat");
            String replacement = new String(actualUserDir + "/");
            wrkStr = reg_replaceAll(DEMO_PREFIX_DIR, replacement, wrkStr);
            outputFilesRemotePathStr = reg_replaceAll(DEMO_PREFIX_DIR, replacement, outputFilesRemotePathStr);
            writeStrToFile(actualUserDir + "/" + workflowName + "_files/" + workflowName + "_remote.wrk", wrkStr);
            writeStrToFile(actualUserDir + "/" + workflowName + "_files/" + "outputFilesRemotePath.dat", outputFilesRemotePathStr);
            this.printlnLog(this.getClass().getName() + ".cmdCopyDemoWorkflowToUsersDir()", "Copying workflow: Success");
        }
        if (returnStr.equals("SUCCESS")) out.println("Status;SUCCESS"); else out.println("Error;" + returnStr);
        out.close();
    }

    private void cmdCheckGraphFilesExist(HttpServletRequest req, HttpServletResponse res, String userName) throws ServletException, IOException {
        res.setContentType("text/html");
        PrintWriter out = res.getWriter();
        String workflowName = req.getParameter("workflowName");
        String uploadFilesListReqStr = req.getParameter("uploadFilesListReqStr");
        String returnStr = new String("");
        String actualUserDir = this.getActualUserDir(userName);
        String[] lines = uploadFilesListReqStr.split(";");
        String[] line;
        String path = new String("");
        String flag = new String("");
        for (int i = 0; i < lines.length; i++) {
            line = lines[i].split(",");
            if (line.length < 3) {
                returnStr += line[0] + ";" + line[1] + ";" + ";" + "0" + "\n";
                continue;
            }
            if (line[1].equals("-")) {
                path = actualUserDir + "/" + workflowName + "_files/" + line[0] + "/" + line[2];
                if (isFileExist(path)) {
                    flag = "1";
                } else {
                    flag = "0";
                    this.printlnLog(this.getClass().getName() + ".cmdCheckGraphFilesExist()", "Error: file not exist: " + workflowName + "_files/" + line[0] + "/" + line[2]);
                }
            } else {
                path = actualUserDir + "/" + workflowName + "_files/" + line[0] + "/" + line[1] + "/" + line[2];
                if (isFileExist(path)) {
                    flag = "1";
                } else {
                    flag = "0";
                    this.printlnLog(this.getClass().getName() + ".cmdCheckGraphFilesExist()", "Error: file not exist: " + workflowName + "_files/" + line[0] + "/" + line[1] + "/" + line[2]);
                }
            }
            returnStr += line[0] + ";" + line[1] + ";" + line[2] + ";" + flag + "\n";
        }
        out.print(returnStr);
        out.close();
    }

    private void cmdCheckJobExecutableIsInstrumented(HttpServletRequest req, HttpServletResponse res, String userName) throws ServletException, IOException {
        res.setContentType("text/html");
        PrintWriter out = res.getWriter();
        String workflowName = req.getParameter("workflowName");
        String jobListReqStr = req.getParameter("jobListReqStr");
        String returnStr = new String("");
        String actualUserDir = this.getActualUserDir(userName);
        String[] lines = jobListReqStr.split(";");
        String[] line;
        String path = new String("");
        String flag = new String("");
        for (int i = 0; i < lines.length; i++) {
            line = lines[i].split(",");
            path = actualUserDir + "/" + workflowName + "_files/" + line[0] + "/" + line[1];
            this.printLog(this.getClass().getName() + ".cmdCheckJobExecutableIsInstrumented()", "Job: " + line[0] + "/" + line[1] + ": ");
            if (isInstrumented(path)) flag = "1"; else flag = "0";
            returnStr += line[0] + ";" + flag + "\n";
            System.out.println(flag);
        }
        out.print(returnStr);
        out.close();
    }

    private void cmdUpdateFiles(HttpServletRequest req, HttpServletResponse res, String userName) throws ServletException, IOException {
        res.setContentType("text/html");
        PrintWriter out = res.getWriter();
        String workflowName = req.getParameter("workflowName");
        String updateDirListParams = req.getParameter("updateDirListParams");
        String workflowDir = this.getActualUserDir(userName) + "/" + workflowName + "_files/";
        String returnStr = updateFiles(workflowDir, updateDirListParams);
        ((userBean) quotaStaticService.getInstance().getUserBeans().get(userName)).realocateWorkflowSize(workflowName);
        out.println(returnStr);
        out.close();
    }

    private void cmdGetGrids(HttpServletRequest req, HttpServletResponse res, String userName) throws ServletException, IOException {
        res.setContentType("text/html");
        PrintWriter out = res.getWriter();
        String returnStr = this.getGrids(userName);
        this.printlnLog(this.getClass().getName() + ".cmdGetGrids()", "returnStr: " + returnStr);
        out.println(returnStr);
        out.close();
    }

    private void cmdGetWorkflowParameters(HttpServletRequest req, HttpServletResponse res, String userName) throws ServletException, IOException {
        res.setContentType("text/html");
        PrintWriter out = res.getWriter();
        String workflowName = req.getParameter("workflowName");
        String returnStr = this.getWorkflowParameters(userName, workflowName);
        this.printlnLog(this.getClass().getName() + ".cmdGetWorkflowParameters()", "returnStr: " + returnStr);
        out.println(returnStr);
        out.close();
    }

    private void cmdGetResourcesForGrid(HttpServletRequest req, HttpServletResponse res, String userName) throws ServletException, IOException {
        res.setContentType("text/html");
        PrintWriter out = res.getWriter();
        String gridName = req.getParameter("gridName");
        boolean isMonitored = new Boolean(req.getParameter("monitor")).booleanValue();
        String returnStr = this.getResourcesForGrid(userName, gridName, isMonitored);
        this.printlnLog(this.getClass().getName() + ".cmdGetResourcesForGrid()", "returnStr:" + returnStr + ":END");
        out.println(returnStr);
        out.close();
    }

    private void cmdGetGridForJobs(HttpServletRequest req, HttpServletResponse res, String userName) throws ServletException, IOException {
        res.setContentType("text/html");
        PrintWriter out = res.getWriter();
        String jobParams = req.getParameter("jobParams");
        String workflowName = req.getParameter("workflowName");
        String returnStr = this.getGridForJobs(userName, workflowName, jobParams);
        this.printlnLog(this.getClass().getName() + ".cmdGetGridForJobs()", "returnStr:" + returnStr + ":END");
        out.println(returnStr);
        out.close();
    }

    private void cmdMonitorTester(HttpServletRequest req, HttpServletResponse res, String userName) throws ServletException, IOException {
        res.setContentType("text/html");
        PrintWriter out = res.getWriter();
        String host = req.getParameter("host");
        String returnStr;
        if (MiscUtils.isMonitorAvailable(host)) returnStr = "host:" + host + "monitor: TRUE"; else returnStr = "host:" + host + "monitor: FALSE";
        out.println(returnStr);
        this.printlnLog(this.getClass().getName() + ".cmdGetResourcesForGrid()", "out.println() END-returnStr:" + returnStr + ":END");
        out.close();
    }

    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String command = req.getParameter("command");
        MultipartRequest multiRequest = null;
        String sessionId = req.getParameter("sessionId");
        this.printLog(this.getClass().getName() + ".doPost()", "COMMAND: " + command);
        if (command == null) {
            try {
                multiRequest = new MultipartRequest(req, "/tmp", 500 * 1024 * 1024, new DefaultFileRenamePolicy());
                command = multiRequest.getParameter("command");
                sessionId = multiRequest.getParameter("sessionId");
            } catch (IOException io) {
                this.printLog(this.getClass().getName() + ".doPost()", "Error: " + io.toString());
            }
            this.printLog(this.getClass().getName() + ".doPost()", "COMMAND: " + command);
        }
        String userName = this.getUserName(sessionId);
        System.out.println(" - userName: " + userName);
        if (userName != null) {
            if (command.equals("saveWorkflow")) {
                cmdSaveWorkflow(req, res, userName);
            } else if (command.equals("setResources")) {
                cmdSetResources(req, res, userName);
            } else if (command.equals("saveAsWorkflow")) {
                cmdSaveAsWorkflow(req, res, userName);
            } else if (command.equals("refresh")) {
                cmdRefresh(req, res, userName);
            } else if (command.equals("getParameters")) {
                cmdGetParameters(req, res, userName);
            } else if (command.equals("getResources")) {
                cmdGetResources(req, res, userName);
            } else if (command.equals("getAttachedWrkFileContent")) {
                cmdGetAttachedWrkFileContent(req, res, userName);
            } else if (command.equals("getWorkflowStatus")) {
                cmdGetWorkflowStatus(req, res, userName);
            } else if (command.equals("saveAsWorkflow")) {
                cmdSaveAsWorkflow(req, res, userName);
            } else if (command.equals("getSizeAcceptance")) {
                cmdGetSizeAcceptance(req, res, userName);
            } else if (command.equals("getSizeOfWorkflow")) {
                cmdGetSizeOfWorkflow(req, res, userName);
            } else if (command.equals("getWorkflowList")) {
                cmdGetWorkflowList(req, res, userName);
            } else if (command.equals("copyDemoWorkflowToUsersDir")) {
                cmdCopyDemoWorkflowToUsersDir(req, res, userName);
            } else if (command.equals("checkGraphFilesExist")) {
                cmdCheckGraphFilesExist(req, res, userName);
            } else if (command.equals("checkJobExecutableIsInstrumented")) {
                cmdCheckJobExecutableIsInstrumented(req, res, userName);
            } else if (command.equals("updateFiles")) {
                cmdUpdateFiles(req, res, userName);
            } else if (command.equals("getGrids")) {
                cmdGetGrids(req, res, userName);
            } else if (command.equals("getWorkflowParameters")) {
                this.cmdGetWorkflowParameters(req, res, userName);
            } else if (command.equals("getResourcesForGrid")) {
                this.cmdGetResourcesForGrid(req, res, userName);
            } else if (command.equals("getGridForJobs")) {
                this.cmdGetGridForJobs(req, res, userName);
            } else if (command.equals("getWorkflowList")) {
                cmdGetWorkflowList(req, res, userName);
            } else if (command.equals("copyDemoWorkflowToUsersDir")) {
                cmdCopyDemoWorkflowToUsersDir(req, res, userName);
            } else if (command.equals("checkGraphFilesExist")) {
                cmdCheckGraphFilesExist(req, res, userName);
            } else if (command.equals("checkJobExecutableIsInstrumented")) {
                cmdCheckJobExecutableIsInstrumented(req, res, userName);
            } else if (command.equals("updateFiles")) {
                cmdUpdateFiles(req, res, userName);
            } else if (command.equals("getGrids")) {
                cmdGetGrids(req, res, userName);
            } else if (command.equals("getWorkflowParameters")) {
                this.cmdGetWorkflowParameters(req, res, userName);
            } else if (command.equals("getResourcesForGrid")) {
                this.cmdGetResourcesForGrid(req, res, userName);
            } else if (command.equals("getGridForJobs")) {
                this.cmdGetGridForJobs(req, res, userName);
            } else if (command.equals("downloadObject")) {
                this.cmdDownloadObject(req, res, userName);
            } else if (command.equals("upload")) {
                this.cmdUpload(multiRequest, res, userName);
            } else if (command.equals("uploadObject")) {
                this.cmdUploadObject(multiRequest, res, userName);
            } else if (command.equals("getPSProps")) {
                this.cmdGetPSProps(req, res, userName);
            }
        } else {
            res.setContentType("text/html");
            PrintWriter out = res.getWriter();
            out.println("Error;NO_USERNAME_TO_SESSION");
            this.printlnLog(this.getClass().getName() + ".doPost()", "Error: no user name to session.");
        }
    }

    private boolean clearDir(String dirName) {
        boolean retValue = true;
        String JDL_EXTENSION = ".jdl";
        int JDL_EXTENSION_LENGTH = JDL_EXTENSION.length();
        File dir = new File(dirName);
        if (dir.exists()) {
            String[] oldies = dir.list();
            if (oldies != null) {
                int dirLength = oldies.length;
                if (dirLength > 0) {
                    for (int i = 0; i < dirLength; i++) {
                        String actName = oldies[i];
                        int actLength = actName.length();
                        if (actName.equals(dirName + "." + JDL_EXTENSION)) {
                            File obsolate = new File(dirName + "/" + actName);
                            try {
                                if (!obsolate.isDirectory()) retValue = retValue && obsolate.delete(); else {
                                    try {
                                        int j = Integer.parseInt(actName);
                                    } catch (NumberFormatException ee) {
                                        obsolate.delete();
                                    }
                                }
                            } catch (SecurityException e) {
                                this.printlnLog(this.getClass().getName() + ".clearDir()", "Deleting obsolate file: " + dirName + "/" + actName + " failed because of " + e);
                                retValue = false;
                            }
                        }
                    }
                }
            }
        } else {
            retValue = false;
        }
        return retValue;
    }

    private boolean clearOldJobDescs(String jobDirName) {
        File jobDir = new File(jobDirName);
        boolean retValue = true;
        try {
            final String JDL_EXTENSION = ".jdl";
            File[] descFiles = jobDir.listFiles(new FileFilter() {

                public boolean accept(File pathname) {
                    return pathname.getName().endsWith(JDL_EXTENSION);
                }
            });
            for (int i = 0; i < descFiles.length; i++) {
                if (!this.chopExtension(descFiles[i].getName()).equals(jobDir.getName())) {
                    retValue = retValue && descFiles[i].delete();
                }
            }
        } catch (Exception e) {
            retValue = false;
            this.printlnLog(this.getClass().getName() + ".clearOldJobDescs()", "Error! Reason:" + e.getMessage());
        }
        return retValue;
    }
}
