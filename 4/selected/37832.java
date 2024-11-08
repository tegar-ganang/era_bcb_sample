package org.or5e.web.action;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.UUID;
import org.or5e.core.AppConstants;
import org.or5e.core.dsource.VideoManager;
import org.or5e.core.dsource.dao.VideoDAO;
import org.or5e.core.transcode.NativeOperation;
import org.or5e.web.core.AAAManagement;
import com.opensymphony.xwork2.Action;

public class FileUploadAction extends BaseAction implements AppConstants {

    private static final long serialVersionUID = 5509657704351548345L;

    File uploadFile = null;

    private String uploadFileContentType = null;

    private String uploadFileFileName = null;

    private String messageOut = "Content Type Not Allowed.";

    private AAAManagement aaaManager = null;

    private VideoManager videoManager = null;

    private NativeOperation nativeOperation = null;

    public final NativeOperation getNativeOperation() {
        return nativeOperation;
    }

    public final void setNativeOperation(NativeOperation nativeOperation) {
        this.nativeOperation = nativeOperation;
    }

    public final VideoManager getVideoManager() {
        return videoManager;
    }

    public final void setVideoManager(VideoManager videoManager) {
        this.videoManager = videoManager;
    }

    public final AAAManagement getAaaManager() {
        return aaaManager;
    }

    public final void setAaaManager(AAAManagement aaaManager) {
        this.aaaManager = aaaManager;
    }

    public final String getMessageOut() {
        return messageOut;
    }

    public final void setMessageOut(String messageOut) {
        this.messageOut = messageOut;
    }

    public final File getUploadFile() {
        return uploadFile;
    }

    public final void setUploadFile(File uploadFile) {
        this.uploadFile = uploadFile;
    }

    public final String getUploadFileContentType() {
        return uploadFileContentType;
    }

    public final void setUploadFileContentType(String uploadFileContentType) {
        this.uploadFileContentType = uploadFileContentType;
    }

    public final String getUploadFileFileName() {
        return uploadFileFileName;
    }

    public final void setUploadFileFileName(String uploadFileFileName) {
        this.uploadFileFileName = uploadFileFileName;
    }

    /**
	 * Action that will be performed during the file upload, It has the Series of steps that needs to be performed...
	 * Step 1: Validate the User Session
	 * Step 2: Upload and Store the file into the draft location, If it fails to upload and store, then send the error message to the client and abort the operation.
	 * Step 3: Check if we need to do transcoding
	 * Step 3a: if transcoding is needed, then transcode the file into H264 format
	 * Step 4: Update the Database accordingly, check for success database upload,
	 * Step 4: If the database upload fails, then remove the uploaded file and send error message to the client.
	 */
    @Override
    public String execute() {
        debug("FileUpload Action is called...");
        if (this.userAuthKey != null) {
            debug("Validating the User Session....");
            Boolean isValidUserSession = aaaManager.isUserSessionAlive(this.userAuthKey);
            debug("Session is Valid: " + isValidUserSession);
            if (isValidUserSession) {
                messageOut = "GOOD";
                String fileName = null;
                String splashScreenImg = "";
                debug("Check do we need to encode the file and proceed with File upload operation");
                boolean doesNotNeedEncode = uploadFileContentType.endsWith("mp4");
                String filePath = (doesNotNeedEncode) ? getproperty("videoDraftPath") : getproperty("videoDraftPathForNonH264");
                fileName = processFileUploadOperation(doesNotNeedEncode);
                File sourceFile = new File(filePath + fileName);
                if (uploadFileContentType.endsWith("mp4")) {
                    splashScreenImg = nativeOperation.getDefaultSplashScreen(sourceFile, 5, (byte) 0);
                }
                debug("File Name during Operation: " + fileName);
                if (fileName != null) {
                    debug("File is Uploaded... and Start the DB Operation...");
                    processDatabaseOperation(fileName, splashScreenImg);
                }
            }
        }
        return Action.SUCCESS;
    }

    @Override
    public void validate() {
        super.validate();
    }

    @Override
    public String toString() {
        return "UserKey: " + this.userAuthKey + "\n" + "File: " + this.uploadFileFileName + "\n" + "ContentType: " + this.uploadFileContentType;
    }

    /**
	 * Once the file is uploaded and transcoded successfully, then this method will be updating the Database accordingly.
	 * @param fileName Uploaded file name.
	 * @return
	 */
    private Boolean processDatabaseOperation(String fileName, String splashScreenImg) {
        VideoDAO videoDAO = getDraftVideoDAO(fileName, splashScreenImg);
        return videoManager.addVideo(videoDAO);
    }

    /**
	 * File Upload process will be taken care by this method.
	 * @return
	 */
    private String processFileUploadOperation(boolean isH264File) {
        String fileType = this.uploadFileFileName.substring(this.uploadFileFileName.lastIndexOf('.'));
        int uniqueHashCode = UUID.randomUUID().toString().hashCode();
        if (uniqueHashCode < 0) {
            uniqueHashCode *= -1;
        }
        String randomFileName = uniqueHashCode + fileType;
        String fileName = (isH264File) ? getproperty("videoDraftPath") : getproperty("videoDraftPathForNonH264") + randomFileName;
        File targetVideoPath = new File(fileName + randomFileName);
        System.out.println("Path: " + targetVideoPath.getAbsolutePath());
        try {
            targetVideoPath.createNewFile();
            FileChannel outStreamChannel = new FileOutputStream(targetVideoPath).getChannel();
            FileChannel inStreamChannel = new FileInputStream(this.uploadFile).getChannel();
            inStreamChannel.transferTo(0, inStreamChannel.size(), outStreamChannel);
            outStreamChannel.close();
            inStreamChannel.close();
            return randomFileName;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private VideoDAO getDraftVideoDAO(String fileName, String splashScreenImg) {
        VideoDAO dao = new VideoDAO();
        dao.setVideoTitle("DRAFT: " + this.uploadFileFileName);
        dao.setVideoFileName(fileName);
        dao.setVideoAccess("PRIVATE");
        dao.setVideoDescription("DESCRIPTION");
        dao.setVideoState("DRAFT");
        dao.setCategoryID(1);
        dao.setVideoTranscoding(true);
        dao.setVideoSplashscreenImage(splashScreenImg);
        dao.setVideoType(this.uploadFileContentType);
        dao.setUserID(userID);
        return dao;
    }
}
