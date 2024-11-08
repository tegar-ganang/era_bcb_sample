package com.strongauth.skcews.util;

import com.strongauth.skcews.common.CommonWS;
import com.strongauth.skcews.exception.SkceWSException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.logging.Logger;
import javax.activation.DataHandler;

/**
 * A collection of util methods.
 *
 *  
 */
public class Util {

    private static final Logger logger = Logger.getAnonymousLogger();

    /**
     * Creates an unique directory under the rootPath. The folder name is a timestamp
     * in milliseconds since January 1, 1970, 00:00:00 GMT to ensure uniqueness.
     * Based on the parameter 'hidden' the directory name is prefixed by a dot to
     * make it hidden. In an unlikely event that the target directory already exists,
     * it retries with a different timestamp.
     * @return the name of the newly created directory.
     * @param baseDir The directory under which the new directory will be created.
     * @param hidden If true, creates a hidden directory.
     */
    public static String createUniqueDir(String baseDir, boolean hidden) throws SkceWSException {
        logger.fine("Entering Util.createUniqueDir method");
        String path = null;
        boolean success = false;
        if ((baseDir != null) && (!(baseDir.equals("")))) {
            if (hidden) {
                path = baseDir + CommonWS.fs + "." + getTime();
            } else {
                path = baseDir + CommonWS.fs + getTime();
            }
            File targetDir = new File(path);
            if (targetDir.exists()) {
                logger.warning("Target Directory already exists. Retrying with a different name");
                createUniqueDir(baseDir, hidden);
            } else {
                success = targetDir.mkdirs();
            }
            if (success) {
                logger.info("Created Directory: " + targetDir.getPath());
            } else {
                logger.severe("CANNOT create Directory: " + targetDir.getPath());
                throw new SkceWSException("CANNOT create Directory: " + targetDir.getPath());
            }
        }
        return path;
    }

    /**
     * Returns the number of milliseconds since January 1, 1970, 00:00:00 GMT as a string
     * @return String The number of milliseconds since January 1, 1970, 00:00:00 GMT as a string.     *
     */
    public static String getTime() {
        return String.valueOf(new Date().getTime());
    }

    /**
     * Writes the data from the given datahandler to a file of the specified name
     * in the specified target location.
     * @return String The full path of the result file.
     * @param dataHandler The DataHandler that has the data to be written to.
     * @param fileName The name of the file to be written to.
     * @param targetPath The full path of the target location.
     */
    public static String writeToFile(DataHandler dataHandler, String fileName, String targetPath) throws SkceWSException {
        String fileFullPath = "";
        InputStream inputStream = null;
        File outputFile = null;
        FileOutputStream fos = null;
        logger.info("Util.writeToFile: fileName = " + fileName);
        logger.info("Util.writeToFile: targetPath = " + targetPath);
        try {
            if (dataHandler != null) {
                inputStream = dataHandler.getInputStream();
                if (inputStream != null) {
                    if (targetPath != null) {
                        outputFile = new File(targetPath + CommonWS.fs + fileName);
                        fos = new FileOutputStream(outputFile);
                        logger.info("Util.writeToFile: writing to target..");
                        byte[] b = new byte[1024];
                        int numread;
                        while ((numread = inputStream.read(b)) != -1) {
                            fos.write(b, 0, numread);
                            fos.flush();
                        }
                        logger.info("Util.writeToFile: wrote to target.");
                        fos.close();
                        inputStream.close();
                        fileFullPath = outputFile.getAbsolutePath();
                    } else {
                        logger.severe("targetPath is null. Please specify the target location to write the file");
                        throw new SkceWSException("Please specify the target location to write the file");
                    }
                } else {
                    logger.severe("No data in datahandler");
                    throw new SkceWSException("Datahandler has no data");
                }
            } else {
                logger.severe("Datahandler is null");
                throw new SkceWSException("No DataHandler specified");
            }
        } catch (FileNotFoundException e) {
            logger.severe(e.getLocalizedMessage());
            throw new SkceWSException(e);
        } catch (IOException e) {
            logger.severe(e.getLocalizedMessage());
            throw new SkceWSException(e);
        } finally {
            logger.info("Util.writeToFile: closing the streams..");
            try {
                if (inputStream != null) inputStream.close();
                if (fos != null) fos.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        logger.fine("Util.writeToFile: returning the file path..");
        return fileFullPath;
    }

    /**
     * Deletes the specified file.
     * @param filePath The full path of the file to be deleted.
     * @return boolean true if the file is successfully deleted and false if not.
     */
    public static boolean delete(String filePath) {
        boolean status = false;
        if (filePath != null) {
            File fileToDel = new File(filePath);
            status = fileToDel.delete();
        }
        if (status) logger.fine("Util.delete: Deleted file: " + filePath); else logger.fine("Util.delete: CANNOT delete file: " + filePath);
        return status;
    }
}
