package cing.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.ProgressListener;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.json.JSONException;
import org.json.JSONObject;
import Wattos.Utils.General;
import Wattos.Utils.InOut;
import Wattos.Utils.OSExec;
import Wattos.Utils.StringArrayList;
import Wattos.Utils.Strings;
import cing.Constants;
import cing.client.Settings;
import cing.client.Utils;
import com.braju.format.Format;
import com.braju.format.Parameters;

public class iCingServlet extends HttpServlet {

    private static final long serialVersionUID = 6098745782027999297L;

    static final String PROJECT_NAME_regexp = ".+.pdb|.+.ent|.+.tgz|.+.tar.gz";

    static {
        if (Settings.DO_DEBUG) {
            General.setVerbosityToDebug();
        }
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, java.io.IOException {
        writeJsonError(response, "Denying iCingServlet.doGet. Try a POST.");
    }

    /**
     * Return a json string to file formPanel handler with only one element: {"error","reason"} or {"message","999 kb"}
     * */
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, java.io.IOException {
        General.showDebug("processing doPost by iCingServlet revision: [" + Settings.REVISION + "]");
        JSONObject result = new JSONObject();
        File pathUser = null;
        File pathProject = null;
        FileItem actualFileItem = null;
        String currentAccessKey = null;
        String currentUserId = Settings.FORM_PARM_USER_ID_DEFAULT;
        String currentAction = null;
        FileItemFactory factory = new DiskFileItemFactory();
        ServletFileUpload upload = new ServletFileUpload(factory);
        List<FileItem> items = null;
        HashMap<String, String> parameterMap = new HashMap<String, String>();
        ProgressListener progressListener = new ProgressListener() {

            private long batchesRead = -1;

            private long bytesPerBatch = 100 * 1024;

            public void update(long pBytesRead, long pContentLength, int pItems) {
                long batchCurrentRead = pBytesRead / bytesPerBatch;
                if (batchesRead == batchCurrentRead) {
                    return;
                }
                batchesRead = batchCurrentRead;
                String pBytesReadStr = Ut.bytesToFormattedString(pBytesRead);
                String pContentLengthStr = Ut.bytesToFormattedString(pContentLength);
                if (pContentLength == -1) {
                    General.showDebug("So far, " + pBytesReadStr + " have been read from item[" + pItems + "].");
                } else {
                    General.showDebug("So far, " + pBytesReadStr + " of " + pContentLengthStr + " have been read from item[" + pItems + "].");
                }
            }
        };
        upload.setProgressListener(progressListener);
        boolean isMultipart = ServletFileUpload.isMultipartContent(request);
        if (!isMultipart) {
            writeJsonError(response, Settings.ERROR_NOT_MULTI_PART);
            return;
        }
        try {
            items = upload.parseRequest(request);
        } catch (FileUploadException e) {
            e.printStackTrace();
            writeJsonError(response, Settings.ERROR_PARSE_REQUEST);
            return;
        }
        if (items == null) {
            writeJsonError(response, "Got a serious error while: upload.parseRequest(request).");
            return;
        }
        if (items.size() < Settings.FORM_PARM_MINIMUM.length) {
            String msg = "Got " + items.size() + " items which is less than the expected:" + Settings.FORM_PARM_MINIMUM.length + " items.";
            writeJsonError(response, msg);
            return;
        }
        for (int i = 0; i < items.size(); i++) {
            FileItem item = items.get(i);
            String name = item.getFieldName();
            String value = item.getString();
            if (item.isFormField()) {
                if (name.equals(Settings.FORM_PARM_ACCESS_KEY)) {
                    currentAccessKey = value;
                    General.showDebug("retrieved currentAccessKey: " + currentAccessKey);
                    continue;
                }
                if (name.equals(Settings.FORM_PARM_USER_ID)) {
                    currentUserId = value;
                    General.showDebug("retrieved currentUserId: " + currentUserId);
                    continue;
                }
                if (name.equals(Settings.FORM_PARM_ACTION)) {
                    currentAction = value;
                    jsonResultPut(result, Settings.FORM_PARM_ACTION, currentAction);
                    General.showDebug("retrieved action: " + currentAction);
                    continue;
                }
                parameterMap.put(name, value);
                if (value == null) {
                    value = Settings.NONE;
                }
                int endIndex = Math.min(100, value.length());
                String valueTruncated = value.substring(0, endIndex);
                Parameters p = new Parameters();
                p.add(name);
                p.add(valueTruncated);
                String msg = Format.sprintf("Retrieved extra parameter [%-30s] with value (first 100 bytes): [%-100s]", p);
                General.showDebug(msg);
            } else {
                actualFileItem = item;
                General.showDebug("retrieved actualFileItem: " + actualFileItem);
            }
        }
        if (currentAction == null) {
            writeJsonError(response, result, "Failed to retrieve Action.");
            return;
        }
        if (currentAccessKey == null) {
            writeJsonError(response, result, "Failed to retrieve " + Settings.FORM_PARM_ACCESS_KEY);
            return;
        }
        if (currentUserId == null) {
            writeJsonError(response, result, "Failed to retrieve  " + Settings.FORM_PARM_USER_ID);
            return;
        }
        if (!Settings.FORM_ACTION_ALIST.contains(currentAction)) {
            writeJsonError(response, result, "Requested action unknown:  " + currentAction);
            return;
        }
        String serverTmpDir = Constants.dRoot + "/" + Settings.RESULT_URL;
        pathUser = new File(serverTmpDir, currentUserId);
        if (!pathUser.exists()) {
            if (!pathUser.mkdir()) {
                writeJsonError(response, result, "Failed to mkdir for user at: [" + pathUser + "]");
                return;
            }
            InOut.chmod(pathUser, "a+rw");
        }
        pathProject = new File(pathUser, currentAccessKey);
        if (!pathProject.exists()) {
            if (!pathProject.mkdir()) {
                writeJsonError(response, result, "Failed to mkdir for project at: [" + pathProject + "]");
                return;
            }
            InOut.chmod(pathProject, "a+rw");
        }
        if (currentAction.equals(Settings.FORM_ACTION_SAVE)) {
            processFile(response, result, pathProject, actualFileItem);
        } else if (currentAction.equals(Settings.FORM_ACTION_PROJECT_NAME)) {
            processPname(response, result, pathProject);
        } else if (currentAction.equals(Settings.FORM_ACTION_PURGE)) {
            processPurgeProject(response, result, pathProject);
        } else if (currentAction.equals(Settings.FORM_ACTION_LOG)) {
            processLog(response, result, pathProject);
        } else if (currentAction.equals(Settings.FORM_ACTION_STATUS)) {
            processStatus(response, result, pathProject);
        } else if (currentAction.equals(Settings.FORM_ACTION_RUN)) {
            processRun(response, result, pathProject, parameterMap);
        } else if (currentAction.equals(Settings.FORM_ACTION_CRITERIA)) {
            processOptions(response, result, pathProject, parameterMap);
        } else {
            writeJsonError(response, result, "Requested action unknown:  " + currentAction + " [CODE ERROR]");
            return;
        }
    }

    private void processPname(HttpServletResponse response, JSONObject result, File pathProject) {
        String projectName = getProjectName(pathProject);
        if (projectName == null) {
            writeJsonError(response, result, "Failed to get project name.");
            return;
        }
        jsonResultPut(result, Settings.RESPONSE_RESULT, projectName);
        writeJson(response, result);
    }

    private void processPurgeProject(HttpServletResponse response, JSONObject result, File pathProject) {
        boolean status = false;
        if (pathProject.exists()) {
            General.showOutput("Removing project: " + pathProject);
            InOut.deleteDirectoryRecursively(pathProject);
            if (!pathProject.exists()) {
                status = true;
            }
        }
        if (status) {
            jsonResultPut(result, Settings.RESPONSE_RESULT, "Removed project: " + pathProject);
            writeJson(response, result);
            return;
        }
        writeJsonError(response, result, "Failed to remove project directory on server");
        return;
    }

    private void jsonResultPut(JSONObject result, String key, String value) {
        try {
            result.put(key, value);
        } catch (JSONException e) {
            General.showError("Failed to JSONObject.put [" + key + "] [" + key + "]");
            e.printStackTrace();
        }
    }

    /**
     * If the exit code was not set yet then set it to success
     *
     * @param response
     * @param result
     */
    private void writeJson(HttpServletResponse response, JSONObject result) {
        if (!result.has(Settings.RESPONSE_EXIT_CODE)) {
            jsonResultPut(result, Settings.RESPONSE_EXIT_CODE, Settings.RESPONSE_EXIT_CODE_SUCCESS);
        }
        String responseStr = result.toString();
        int endIndex = Math.min(responseStr.length(), Settings.MAX_RESPONSE_REPORTED_FOR_DEBUGGING);
        String responseStrTruncate = responseStr.substring(0, endIndex).replace(General.eol, "");
        General.showDebug("responseTruncate is: [" + responseStrTruncate + "]");
        response.setContentType("text/plain");
        try {
            response.getWriter().write(result.toString());
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }

    private void writeJsonError(HttpServletResponse response, JSONObject result, String msg) {
        jsonResultPut(result, Settings.RESPONSE_EXIT_CODE, Settings.RESPONSE_EXIT_CODE_ERROR);
        jsonResultPut(result, Settings.RESPONSE_RESULT, msg);
        writeJson(response, result);
    }

    private void writeJsonError(HttpServletResponse response, String msg) {
        JSONObject result = new JSONObject();
        writeJsonError(response, result, msg);
    }

    /**
     * @param pathProject
     * @return null on error.
     */
    private String getProjectFilePath(File pathProject) {
        if (pathProject == null) {
            General.showCodeBug("Got null for pathProject in iCingServlet#getProjectFilePath");
            return null;
        }
        InOut.RegExpFilenameFilter ff = new InOut.RegExpFilenameFilter(PROJECT_NAME_regexp);
        String[] list = pathProject.list(ff);
        General.showOutput("Found files: " + Strings.toString(list));
        if (list.length < 1) {
            return null;
        }
        return list[0];
    }

    /**
     * @param pathProject
     * @return null on error.
     */
    private String getProjectName(File pathProject) {
        String projectFilePath = getProjectFilePath(pathProject);
        if (projectFilePath == null) {
            General.showCodeBug("Got null for projectFilePath in iCingServlet#getProjectName");
            return null;
        }
        String projectName = null;
        if (projectFilePath.endsWith("tgz") || projectFilePath.endsWith("tar.gz")) {
            if (projectFilePath.contains(".cing.")) {
                ;
            } else {
            }
        }
        if (projectName == null) {
            projectName = InOut.getFilenameBase(projectFilePath);
        }
        return projectName;
    }

    /**
     * Actually saves the file
     *
     * @param parameterMap
     *
     * @param item
     * @return a JSON string.
     */
    private void processRun(HttpServletResponse response, JSONObject result, File pathProject, HashMap<String, String> parameterMap) {
        String verbosity = null;
        String residues = null;
        String ensemble = null;
        for (Iterator<String> it = parameterMap.keySet().iterator(); it.hasNext(); ) {
            String key = it.next();
            String value = parameterMap.get(key);
            if (key.equals(Settings.FORM_PARM_VERBOSITY)) {
                verbosity = value;
            } else if (key.equals(Settings.FORM_PARM_RESIDUES)) {
                residues = value;
            } else if (key.equals(Settings.FORM_PARM_ENSEMBLE)) {
                ensemble = value;
            } else {
                General.showError("Ignoring unexpected key/value: " + key + "/" + value);
            }
        }
        File doneFile = new File(pathProject, Settings.DONE_FILE);
        File cingRunLogFile = new File(pathProject, Settings.CING_RUN_LOG_FILE);
        File lastLogSendFile = new File(pathProject, Settings.LAST_LOG_SEND_FILE);
        File[] fileListToDelete = new File[] { doneFile, cingRunLogFile, lastLogSendFile };
        for (int i = 0; i < fileListToDelete.length; i++) {
            File f = fileListToDelete[i];
            if (f.exists()) {
                f.delete();
            }
        }
        String cmdCdProjectDir = "cd " + pathProject;
        String cmdRunStarting = "(" + cmdCdProjectDir + ";date;echo 'DEBUG: Starting cing run') >>& " + cingRunLogFile;
        String cmdRunDone = "touch " + doneFile;
        String projectFileName = getProjectFilePath(pathProject);
        if (projectFileName == null) {
            writeJsonError(response, result, "Failed to getProjectFilePath");
            return;
        }
        String possibleInit = getInitString(projectFileName);
        String projectName = getProjectName(pathProject);
        if (projectFileName == null) {
            writeJsonError(response, result, "Failed to getProjectName");
            return;
        }
        String cmdRunZipper = "zip -rq " + projectName + Settings.ZIP_REPORT_FILENAME_POST_FIX + ".zip cingRun.log " + projectName + ".cing";
        String cing_options = "--name " + projectName + " --script doValidateiCing.py " + possibleInit;
        General.showDebug("verbosity at iCingServlet: " + verbosity);
        if (verbosity != null && verbosity.length() > 0 && (!verbosity.equals("null"))) {
            cing_options += " --verbosity " + verbosity;
        }
        General.showDebug("residues at iCingServlet: " + residues);
        if (residues != null && residues.length() > 0 && (!residues.equals("null"))) {
            residues = residues.replaceAll(" ", "");
            cing_options += " --ranges " + residues;
        }
        if (ensemble != null && ensemble.length() > 0 && (!ensemble.equals("null"))) {
            ensemble = ensemble.replaceAll(" ", "");
            cing_options += " --ensemble " + ensemble;
        }
        String cingWrapperScript = Constants.cingRoot + "/" + Settings.CING_WRAPPER_SCRIPT;
        Parameters p = new Parameters();
        p.add(cmdCdProjectDir);
        p.add(cingWrapperScript);
        p.add(cing_options);
        p.add(cmdRunZipper);
        p.add(cmdRunDone);
        p.add(cingRunLogFile);
        String cmdRun = Format.sprintf("(%s; %s %s; %s; %s) >>& %s &", p);
        General.showDebug("cmdRunStarting: [" + cmdRunStarting + "]");
        General.showDebug("cmdRunZipper:   [" + cmdRunZipper + "]");
        General.showDebug("cmdRun:         [" + cmdRun + "]");
        String[] cmdList = new String[] { cmdRunStarting, cmdRun };
        if (General.verbosity != General.verbosityDebug) {
            cmdList = new String[] { cmdRun };
        }
        int delayBetweenSubmittingJobs = 500;
        int status = OSExec.exec(cmdList, delayBetweenSubmittingJobs);
        if (status != 0) {
            writeJsonError(response, result, "Failed to submit all jobs.");
            return;
        }
        jsonResultPut(result, Settings.RESPONSE_RESULT, Settings.RESPONSE_STARTED);
        writeJson(response, result);
    }

    private String getInitString(String projectFileName) {
        if (projectFileName.endsWith(".cing.tgz") || projectFileName.endsWith(".cing.tar.gz")) {
            return "--old";
        }
        if (projectFileName.endsWith(".cyana.tgz") || projectFileName.endsWith(".cyana.tar.gz")) {
            return "--initCyana " + projectFileName;
        }
        if (projectFileName.endsWith(".tgz") || projectFileName.endsWith(".tar.gz")) {
            return "--initCcpn " + projectFileName;
        }
        if (projectFileName.endsWith(".ent") || projectFileName.endsWith(".pdb")) {
            return "--initPDB " + projectFileName;
        }
        return null;
    }

    /**
     * Actually saves the file
     *
     * @param item
     * @return a JSON string.
     */
    private void processFile(HttpServletResponse response, JSONObject result, File pathProject, FileItem item) {
        if (item == null) {
            writeJsonError(response, "No actual file item retrieved");
            return;
        }
        General.showDebug("Checking size...");
        if (item.getSize() > Settings.FILE_UPLOAD_MAX_SIZE) {
            General.showError("Size not acceptable.");
            writeJsonError(response, result, "Failed to retrieve " + Settings.ERROR_SIZE_UNACCEPTABLE);
            return;
        }
        String fileName = item.getName();
        General.showDebug("fn   : " + fileName);
        fileName = Utils.getFileNameWithoutPath(fileName);
        General.showDebug("fn(b): " + fileName);
        if (fileName.indexOf(":") >= 0 || fileName.indexOf("..") >= 0 || fileName.charAt(0) == '/') {
            writeJsonError(response, result, "Filename not considered safe: [" + fileName + "]");
            return;
        }
        File uploadedFile = new File(pathProject, fileName);
        if (uploadedFile.exists()) {
            if (!uploadedFile.delete()) {
                writeJsonError(response, result, "Failed to remove file with the same name: [" + uploadedFile + "]");
                return;
            }
        }
        try {
            item.write(uploadedFile);
        } catch (Exception e) {
            e.printStackTrace();
            writeJsonError(response, result, Settings.ERROR_WRITE_FAILED);
            return;
        }
        InOut.chmod(uploadedFile, "a+rw");
        long length = uploadedFile.length();
        long lengthFormElement = item.getSize();
        General.showDebug("Fileform     length: " + lengthFormElement);
        General.showDebug("File written length: " + length);
        String sizeStr = Ut.bytesToFormattedString(length);
        jsonResultPut(result, Settings.RESPONSE_RESULT, sizeStr);
        writeJson(response, result);
    }

    /**
     * Saves the validation settings file: valSets.cfg
     *
     * @param item
     * @return a JSON string.
     */
    private void processOptions(HttpServletResponse response, JSONObject result, File pathProject, HashMap<String, String> parameterMap) {
        General.showDebug("Now in processOptions.");
        if (parameterMap.size() == 0) {
            jsonResultPut(result, Settings.RESPONSE_RESULT, "No parameters to save");
            writeJson(response, result);
            return;
        }
        String fileName = Settings.VAL_SETS_CFG_DEFAULT_FILENAME;
        File uploadedFile = new File(pathProject, fileName);
        if (uploadedFile.exists()) {
            General.showWarning("Removing existing criteria file");
            if (!uploadedFile.delete()) {
                writeJsonError(response, result, "Failed to delete existing criteria file");
                return;
            }
        }
        String parameterListString = Ut.mapToPythonRFC822ConfigurationSettings(parameterMap);
        if (parameterListString == null) {
            writeJsonError(response, result, "Failed to convert options to text.");
            return;
        }
        boolean force = true;
        boolean interactive = true;
        if (!InOut.writeTextToFile(uploadedFile, parameterListString, force, interactive)) {
            writeJsonError(response, result, "Failed to write options to file as InOut.writeTextToFile failed.");
            return;
        }
        InOut.chmod(uploadedFile, "a+rw");
        long length = uploadedFile.length();
        General.showDebug("File written length: " + length);
        String sizeStr = Ut.bytesToFormattedString(length);
        jsonResultPut(result, Settings.RESPONSE_RESULT, "Saved options: " + sizeStr + " on server");
        writeJson(response, result);
    }

    private void processStatus(HttpServletResponse response, JSONObject result, File pathProject) {
        File doneFile = new File(pathProject, Settings.DONE_FILE);
        String status = Settings.RESPONSE_STATUS_NOT_DONE;
        if (doneFile.exists()) {
            status = Settings.RESPONSE_STATUS_DONE;
        }
        jsonResultPut(result, Settings.RESPONSE_RESULT, status);
        writeJson(response, result);
        return;
    }

    /**
     *
     * @param response
     * @param result
     *            Message will be byte by byte and end up in a PRE block
     * @param pathProject
     * @param item
     * @param currentUserId
     * @param currentAccessKey
     */
    private void processLog(HttpServletResponse response, JSONObject result, File pathProject) {
        General.showDebug("Retrieving cing log tail.");
        File lastLogSendFile = new File(pathProject, Settings.LAST_LOG_SEND_FILE);
        String lastLog = Settings.RESPONSE_LOG_VALUE_NONE;
        File cingRunLogFile = new File(pathProject, Settings.CING_RUN_LOG_FILE);
        if (!cingRunLogFile.exists()) {
            jsonResultPut(result, Settings.RESPONSE_RESULT, lastLog);
            writeJson(response, result);
        }
        long cingrunLogFileSize = cingRunLogFile.length();
        General.showDebug("cingrunLogFileSize: " + cingrunLogFileSize);
        long cingrunLogFileSizeLast = 0;
        if (lastLogSendFile.exists()) {
            General.showDebug("Checking lastLogSendFile: " + lastLogSendFile);
            StringArrayList sal = new StringArrayList();
            boolean statusRead = sal.read(lastLogSendFile.toString());
            lastLogSendFile.delete();
            if (!statusRead) {
                writeJsonError(response, result, "Failed to read the lastLogSendFile: " + lastLogSendFile);
                return;
            }
            if (sal.size() < 1) {
                writeJsonError(response, result, "Failed to read at least one line from the present lastLogSendFile: " + lastLogSendFile);
                return;
            }
            String cingrunLogFileSizeLastStr = sal.getString(0);
            General.showDebug("cingrunLogFileSizeLast (string): " + cingrunLogFileSizeLastStr);
            cingrunLogFileSizeLast = Long.parseLong(cingrunLogFileSizeLastStr);
            General.showDebug("cingrunLogFileSizeLast (long): " + cingrunLogFileSizeLast);
        } else {
            General.showDebug("no LAST_LOG_SEND_FILE: " + Settings.LAST_LOG_SEND_FILE);
        }
        StringArrayList sal = new StringArrayList();
        sal.add(Long.toString(cingrunLogFileSize));
        General.showDebug("writing to LAST_LOG_SEND_FILE: " + Settings.LAST_LOG_SEND_FILE);
        if (!sal.write(lastLogSendFile.toString())) {
            writeJsonError(response, result, "Failed to write to new lastLogSendFile: " + lastLogSendFile);
            return;
        }
        if (cingrunLogFileSize > cingrunLogFileSizeLast) {
            long newLogSize = cingrunLogFileSize - cingrunLogFileSizeLast;
            General.showDebug("New log size: " + newLogSize);
            try {
                RandomAccessFile raf = new RandomAccessFile(cingRunLogFile, "r");
                raf.seek(cingrunLogFileSizeLast);
                byte[] b = new byte[(int) newLogSize];
                raf.readFully(b);
                raf.close();
                lastLog = new String(b);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                writeJsonError(response, result, "Failed to find cingRunLogFile: " + cingRunLogFile);
                return;
            } catch (IOException e) {
                e.printStackTrace();
                writeJsonError(response, result, "Detected IOException see tomcat log");
                return;
            }
        } else {
            General.showDebug("No new log");
        }
        jsonResultPut(result, Settings.RESPONSE_EXIT_CODE, Settings.RESPONSE_EXIT_CODE_SUCCESS);
        jsonResultPut(result, Settings.RESPONSE_RESULT, lastLog);
        writeJson(response, result);
    }
}
