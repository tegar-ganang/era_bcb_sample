package com.hillert.upload.service.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.hillert.upload.service.UploadService;

/**
 * @author admin
 *
 */
public class UploadServiceImpl implements UploadService {

    /**
     * Logger declaration.
     */
    private static final Log LOGGER = LogFactory.getLog(UploadServiceImpl.class);

    /**
     * Constructor.
     */
    public UploadServiceImpl() {
        super();
    }

    /**
     * @see com.hillert.upload.service.UploadService#saveFile(java.io.InputStream)
     */
    public void saveFile(final InputStream inputStream, final String fileName) {
        final File file = new File(System.getProperty("java.io.tmpdir") + File.separator + fileName);
        FileOutputStream fileOut = null;
        try {
            fileOut = new FileOutputStream(file);
            IOUtils.copy(inputStream, fileOut);
        } catch (FileNotFoundException e) {
            LOGGER.error("saveFile() - File Not Found." + e);
        } catch (IOException e) {
            LOGGER.error("saveFile() - Error while saving file." + e);
        } finally {
            try {
                inputStream.close();
                if (fileOut != null) {
                    fileOut.close();
                }
            } catch (IOException e) {
                LOGGER.error(e);
            }
        }
    }
}
