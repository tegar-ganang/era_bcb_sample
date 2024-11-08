package test.com.ivis.xprocess.license;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.ivis.xprocess.util.FileUtils;

public class LicenseCopier {

    private static final Logger logger = Logger.getLogger(LicenseCopier.class.getName());

    private static LicenseCopier instance = null;

    private static final String LICENSE_FILE_PACKAGE = "test/com/ivis/xprocess/util/";

    private static final String LICENSE_FILE_NAME = "licenseEE.properties";

    private static final String TO_LICENSE_FILE_NAME = "xprocess.lic";

    public static LicenseCopier getInstance() {
        if (instance == null) {
            instance = new LicenseCopier();
        }
        return instance;
    }

    public void copy(String tempPath) {
        File localPathFile = new File(tempPath + File.separator + "local/");
        localPathFile.mkdirs();
        File licenseSrcFile = null;
        String licensePath = System.getProperty("license.path");
        if ((licensePath == null) || licensePath.equals("")) {
            try {
                URL url = Thread.currentThread().getContextClassLoader().getResource(LICENSE_FILE_PACKAGE + LICENSE_FILE_NAME);
                if (url != null) {
                    licenseSrcFile = new File(url.toURI());
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Problem geting rescource from classloader", e);
            }
        } else {
            licenseSrcFile = new File(licensePath + File.separator + LICENSE_FILE_NAME);
        }
        if (licenseSrcFile == null) {
            return;
        }
        if (!licenseSrcFile.exists()) {
            logger.log(Level.WARNING, "License file does not exist");
            return;
        }
        File toFile = new File(localPathFile.getPath() + File.separator + TO_LICENSE_FILE_NAME);
        try {
            FileUtils.copyFile(licenseSrcFile, toFile);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Problem copying license file", e);
        }
    }
}
