package org.dbe.toolkit.portal.ui.tools;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.log4j.Logger;
import org.dbe.toolkit.portal.client.DBEPortalClient;

/**
 * @author andy-edmonds
 */
public class UIResourcesUnzipper {

    private static final String NIX_FILEPATH_SEP = "/";

    private static Logger logger = null;

    public UIResourcesUnzipper() {
        super();
        logger = DBEPortalClient.getLogger();
    }

    /**
     * Extracts a zip file
     * 
     * @param zipName the absolute name of the zip file
     * @param extractionPath the absolute path to where the zip file is to be
     *            extracted to
     * @throws IOException
     */
    public static void unzip(String zipName, String extractionPath) throws IOException {
        Enumeration zipEntries;
        getLogger().debug("Opening: " + zipName);
        ZipFile zipFile = new ZipFile(zipName);
        zipEntries = zipFile.entries();
        while (zipEntries.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) zipEntries.nextElement();
            if (entry.isDirectory()) {
                getLogger().debug("Extracting directory: " + extractionPath + NIX_FILEPATH_SEP + entry.getName());
                File dir = new File(extractionPath + NIX_FILEPATH_SEP + entry.getName());
                dir.mkdir();
                continue;
            }
            getLogger().debug("Extracting file: " + extractionPath + NIX_FILEPATH_SEP + entry.getName());
            byte[] buffer = new byte[1024];
            int len;
            InputStream in = zipFile.getInputStream(entry);
            File x = new File(extractionPath + NIX_FILEPATH_SEP + entry.getName());
            getLogger().debug("Output location: " + extractionPath + NIX_FILEPATH_SEP + entry.getName());
            x.createNewFile();
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(x));
            while ((len = in.read(buffer)) >= 0) out.write(buffer, 0, len);
            in.close();
            out.close();
        }
        zipFile.close();
    }

    private static Logger getLogger() {
        if (logger == null) logger = DBEPortalClient.getLogger();
        return logger;
    }
}
