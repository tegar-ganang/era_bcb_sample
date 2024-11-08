package com.rooster.utils.xl.exp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.Date;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import com.rooster.constants.PropertyFileConst;
import com.rooster.utils.CurrentDate;

public class ZipAndExportFolder {

    private static Logger logger = Logger.getLogger(ZipAndExportFolder.class);

    public static String checkAndZipTheFolder(HttpServletRequest req, String sInputFolderPath, String sPartFileName) {
        HttpSession session = req.getSession();
        String sResponse = new String();
        File fInputFolder = new File(sInputFolderPath);
        boolean isSourceFolderExists = checkFolderExistence(fInputFolder);
        if (isSourceFolderExists) {
            String sTempFolder = getTempFolderPath(session);
            String sNewFolder = new String("Export_" + sPartFileName + CurrentDate.getDateTimeInMillis());
            File fOutputFolder = new File(sTempFolder + sNewFolder);
            logger.debug("Temporary Folder : " + sTempFolder);
            logger.debug("New Folder : " + sNewFolder);
            logger.debug("Output Folder : " + fOutputFolder);
            boolean isFolderCreated = checkFolderExistence(fOutputFolder) ? deleteFolder(fOutputFolder) ? createFolder(fOutputFolder) : false : createFolder(fOutputFolder);
            if (isFolderCreated) {
                String sOutFileName = new String("Rooster_Exported_" + sPartFileName + CurrentDate.convertDBFormatToUS(CurrentDate.getDate(), "dd-MM-yyyy", "MM-dd-yyyy") + ".zip");
                int iPos = sInputFolderPath.indexOf("documents/");
                String sChangeDir = sInputFolderPath.substring(0, iPos);
                boolean isCompleted = zipTheFolder(sChangeDir, fInputFolder, fOutputFolder, sOutFileName);
                if (isCompleted) {
                    if (fOutputFolder.getAbsolutePath().indexOf("\\") > -1) {
                        sOutFileName = fOutputFolder.getAbsolutePath() + "\\" + sOutFileName;
                    } else {
                        sOutFileName = fOutputFolder.getAbsolutePath() + "/" + sOutFileName;
                    }
                    sResponse = "Success -" + sOutFileName;
                } else {
                    sResponse = "Error Occured - Unable To Compress The File";
                }
            }
        } else {
            sResponse = "Error Occured - The Folder To Be Compressed Does Not Exist";
        }
        return sResponse;
    }

    public static boolean zipTheFolder(String sChangeDir, File fInputFolder, File fOutputFolder, String sOutFileName) {
        String sOutFile = new String();
        if (fOutputFolder.getAbsolutePath().indexOf("\\") > -1) {
            sOutFile = fOutputFolder.getAbsolutePath() + "\\" + sOutFileName;
        } else {
            sOutFile = fOutputFolder.getAbsolutePath() + "/" + sOutFileName;
        }
        Runtime rtime = Runtime.getRuntime();
        logger.info("Input Folder Path:" + fInputFolder.getAbsolutePath());
        logger.info("Output Folder Path:" + fOutputFolder.getAbsolutePath());
        logger.info("Out File:" + sOutFile);
        logger.info("Out File Name:" + sOutFileName);
        try {
            Process process = rtime.exec("/bin/sh");
            BufferedWriter outCommand = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
            String sSlash = new String();
            if (fInputFolder.getAbsolutePath().indexOf(new String("\\")) > -1) {
                sSlash = "\\";
            } else {
                sSlash = "/";
            }
            outCommand.write("cd " + sChangeDir + "\n");
            logger.info("CD COMMAND :" + "cd " + sChangeDir + "\n");
            outCommand.flush();
            String sInputFolder = StringUtils.replace(fInputFolder.getAbsolutePath(), sChangeDir, " ");
            sInputFolder = sInputFolder.trim();
            outCommand.write("zip -9 -r " + sOutFile + " " + sInputFolder + sSlash + "\n");
            logger.info("Zip Command : " + "zip -9 -r " + sOutFile + " " + sInputFolder + sSlash + "\n");
            outCommand.flush();
            outCommand.write("exit" + "\n");
            outCommand.flush();
            BufferedReader inputStreamReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader errStreamReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            StringBuffer output = new StringBuffer();
            StringBuffer error = new StringBuffer();
            for (String line; (line = inputStreamReader.readLine()) != null; ) {
                output.append(line);
            }
            for (String line; (line = errStreamReader.readLine()) != null; ) {
                error.append(line);
            }
            logger.debug("ZIP Output Message : " + output);
            logger.debug("ZIP Error Message : " + error);
        } catch (IOException e) {
            logger.debug(e);
        } catch (Exception e) {
            logger.debug(e);
        }
        return true;
    }

    public static String exportTheFile(HttpServletRequest req, HttpServletResponse res, String sDownloadLink) {
        String sResponse = new String();
        try {
            String sFilePath = new String();
            String sFileName = new String();
            if (sDownloadLink.indexOf(new String("/")) > -1) {
                sFilePath = sDownloadLink.substring(0, sDownloadLink.lastIndexOf("/"));
                sFileName = sDownloadLink.substring(sDownloadLink.lastIndexOf("/") + 1);
            } else {
                sFilePath = sDownloadLink.substring(0, sDownloadLink.lastIndexOf("\\"));
                sFileName = sDownloadLink.substring(sDownloadLink.lastIndexOf("\\") + 1);
            }
            if (req.getServletPath().endsWith(sFileName)) {
                sResponse = "File Not Found";
            }
            sFileName = URLDecoder.decode(sFileName, "UTF-8");
            File file = new File(sFilePath, sFileName);
            if (!file.exists()) {
                sResponse = "File Not Found";
            }
            String contentType = URLConnection.guessContentTypeFromName(sFileName);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            BufferedInputStream input = null;
            BufferedOutputStream output = null;
            try {
                input = new BufferedInputStream(new FileInputStream(file));
                int contentLength = input.available();
                res.reset();
                res.setContentLength(contentLength);
                res.setContentType(contentType);
                res.setHeader("Content-disposition", "attachment; filename=\"" + sFileName + "\"");
                output = new BufferedOutputStream(res.getOutputStream());
                while (contentLength-- > 0) {
                    output.write(input.read());
                }
                output.flush();
            } catch (IOException e) {
                logger.debug(e);
            } finally {
                if (input != null) {
                    try {
                        input.close();
                    } catch (IOException e) {
                        logger.debug(e);
                    }
                }
                if (output != null) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        logger.debug(e);
                    }
                }
            }
        } catch (Exception e) {
            logger.debug(e);
        }
        return sResponse;
    }

    public static boolean checkFolderExistence(File sFolder) {
        if (sFolder.exists()) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean createFolder(File fNewFolder) {
        return fNewFolder.mkdir();
    }

    public static boolean deleteFolder(File fFolder) {
        if (fFolder.isDirectory()) {
            String[] children = fFolder.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteFolder(new File(fFolder, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        return fFolder.delete();
    }

    public static boolean deleteFolderTwoDaysOld(HttpSession session) {
        String sTempFolder = ZipAndExportFolder.getTempFolderPath(session);
        File fTempFolder = new File(sTempFolder);
        if (fTempFolder.isDirectory()) {
            String[] children = fTempFolder.list();
            for (int i = 0; i < children.length; i++) {
                String sChildFolder = new String();
                if (sTempFolder.indexOf(new String("\\")) > -1) {
                    sChildFolder = sTempFolder + "\\" + children[i];
                } else {
                    sChildFolder = sTempFolder + "/" + children[i];
                }
                File fExportFolder = new File(sChildFolder);
                if (fExportFolder.isDirectory()) {
                    if (sChildFolder.indexOf(new String("Export_")) > -1) {
                        long lTime = Long.parseLong(sChildFolder.substring(sChildFolder.lastIndexOf("_") + 1));
                        Date dFolderDate = new Date(lTime);
                        Date dToday = new Date();
                        int iDateDiff = CurrentDate.calculateDifference(dFolderDate, dToday);
                        if (iDateDiff > 2) {
                            deleteFolder(new File(sChildFolder));
                        }
                    }
                }
            }
        }
        return true;
    }

    public static String getTempFolderPath(HttpSession session) {
        return String.valueOf(session.getAttribute(PropertyFileConst.APPLICATION_ROOT_PATH)) + String.valueOf(session.getAttribute(PropertyFileConst.TEMPORARY_FOLDER));
    }

    public static String getAttachmentsFolderPath(HttpSession session) {
        return String.valueOf(session.getAttribute(PropertyFileConst.APPLICATION_ROOT_PATH)) + String.valueOf(session.getAttribute(PropertyFileConst.ATTACHMENTS_FOLDER));
    }

    public static String getRequirementsFolderPath(HttpSession session) {
        return String.valueOf(session.getAttribute(PropertyFileConst.APPLICATION_ROOT_PATH)) + String.valueOf(session.getAttribute(PropertyFileConst.REQUIREMENT_ATTACHMENTS_FOLDER));
    }

    public static String getResumesFolderPath(HttpSession session) {
        return String.valueOf(session.getAttribute(PropertyFileConst.APPLICATION_ROOT_PATH)) + String.valueOf(session.getAttribute(PropertyFileConst.DOCUMENTS_FOLDER));
    }

    public static String getOnBoardingFolderPath(HttpSession session) {
        return String.valueOf(session.getAttribute(PropertyFileConst.APPLICATION_ROOT_PATH)) + String.valueOf(session.getAttribute(PropertyFileConst.ONBOARDING_FOLDER));
    }

    public static String getOffBoardingFolderPath(HttpSession session) {
        return String.valueOf(session.getAttribute(PropertyFileConst.APPLICATION_ROOT_PATH)) + String.valueOf(session.getAttribute(PropertyFileConst.OFFBOARDING_FOLDER));
    }
}
