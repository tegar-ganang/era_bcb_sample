package es.eucm.eadventure.engine.resourcehandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import es.eucm.eadventure.engine.resourcehandler.zipurl.ZipURL;

/**
 * This resource handler loads files for being used on a standard java
 * application
 */
class ResourceHandlerUnrestricted extends ResourceHandler {

    /**
     * Singleton
     */
    private static ResourceHandlerUnrestricted instance;

    public static ResourceHandler getInstance() {
        return instance;
    }

    public static void create() {
        instance = new ResourceHandlerUnrestricted();
    }

    public static void delete() {
        if (instance != null && instance.tempFiles != null) {
            for (TempFile file : instance.tempFiles) {
                file.delete();
            }
        }
        instance = null;
    }

    /**
     * Empty constructor
     */
    private ResourceHandlerUnrestricted() {
    }

    @Override
    public void setZipFile(String zipFilename) {
        try {
            ResourceHandler.zipPath = zipFilename;
            zipFile = new ZipFile(zipFilename);
        } catch (ZipException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public OutputStream getOutputStream(String path) {
        OutputStream os = null;
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        try {
            os = new FileOutputStream(path);
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            os = null;
        }
        return os;
    }

    @Override
    public InputStream getResourceAsStream(String path) {
        InputStream is = null;
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        try {
            is = new FileInputStream(path);
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            is = null;
        }
        return is;
    }

    @Override
    public URL getResourceAsURLFromZip(String path) {
        try {
            return ZipURL.createAssetURL(zipPath, path);
        } catch (MalformedURLException e) {
            return null;
        }
    }

    public InputStream buildInputStream(String filePath) {
        return getResourceAsStreamFromZip(filePath);
    }

    public String[] listNames(String filePath) {
        File dir = new File(zipPath, filePath);
        return dir.list();
    }

    /**
     * Extracts the resource and get it copied to a file in the local system.
     * Required when an asset cannot be loaded directly from zip
     * 
     * @param assetPath
     * @return The absolute path of the destiny file where the asset was copied
     */
    @Override
    public URL getResourceAsURL(String assetPath) {
        URL toReturn = null;
        try {
            InputStream is = this.getResourceAsStreamFromZip(assetPath);
            String filePath = generateTempFileAbsolutePath(getExtension(assetPath));
            File destinyFile = new File(filePath);
            if (writeFile(is, destinyFile)) {
                toReturn = destinyFile.toURI().toURL();
                TempFile tempFile = new TempFile(destinyFile.getAbsolutePath());
                tempFile.setOriginalAssetPath(assetPath);
                tempFiles.add(tempFile);
            } else toReturn = null;
        } catch (Exception e) {
            toReturn = null;
        }
        return toReturn;
    }

    public boolean writeFile(InputStream is, File dest) {
        try {
            FileOutputStream os = new FileOutputStream(dest);
            int c;
            byte[] buffer = new byte[512];
            while ((c = is.read(buffer)) != -1) os.write(buffer, 0, c);
            os.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean copyFile(File source, File dest) {
        try {
            FileReader in = new FileReader(source);
            FileWriter out = new FileWriter(dest);
            int c;
            while ((c = in.read()) != -1) out.write(c);
            in.close();
            out.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
