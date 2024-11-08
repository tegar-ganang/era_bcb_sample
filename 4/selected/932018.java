package org.eclipse.help.internal.base;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.osgi.util.NLS;

/**
 * application org.eclipse.help.indexTool
 */
public class IndexToolApplication implements IApplication {

    public synchronized Object start(IApplicationContext context) throws Exception {
        try {
            String directory = System.getProperty("indexOutput");
            if (directory == null || directory.length() == 0) {
                throw new Exception(NLS.bind(HelpBaseResources.IndexToolApplication_propertyNotSet, "indexOutput"));
            }
            String localeStr = System.getProperty("indexLocale");
            if (localeStr == null || localeStr.length() < 2) {
                throw new Exception(NLS.bind(HelpBaseResources.IndexToolApplication_propertyNotSet, "indexLocale"));
            }
            Locale locale;
            if (localeStr.length() >= 5) {
                locale = new Locale(localeStr.substring(0, 2), localeStr.substring(3, 5));
            } else {
                locale = new Locale(localeStr.substring(0, 2), "");
            }
            preindex(directory, locale);
        } catch (Exception e) {
            e.printStackTrace();
            HelpBasePlugin.logError("Preindexing failed.", e);
        }
        return EXIT_OK;
    }

    public synchronized void stop() {
    }

    private void preindex(String outputDir, Locale locale) throws Exception {
        File indexPath = new File(HelpBasePlugin.getConfigurationDirectory(), "index/" + locale);
        if (indexPath.exists()) {
            delete(indexPath);
        }
        BaseHelpSystem.getLocalSearchManager().ensureIndexUpdated(new NullProgressMonitor(), BaseHelpSystem.getLocalSearchManager().getIndex(locale.toString()));
        File d = new File(outputDir, "nl" + File.separator + locale.getLanguage());
        if (locale.getCountry().length() > 0) {
            d = new File(d, locale.getCountry());
        }
        if (!d.exists()) d.mkdirs();
        ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(new File(d, "doc_index.zip")));
        try {
            zipDirectory(indexPath, zout, null);
        } finally {
            zout.close();
        }
    }

    /**
	 * Recursively deletes directory and files.
	 * 
	 * @param file
	 * @throws IOException
	 */
    private static void delete(File file) throws IOException {
        if (file.isDirectory()) {
            File files[] = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                delete(files[i]);
            }
        }
        if (!file.delete()) {
            throw new IOException(NLS.bind(HelpBaseResources.IndexToolApplication_cannotDelete, file.getAbsolutePath()));
        }
    }

    /**
	 * Adds files in a directory to a zip stream
	 * 
	 * @param dir
	 *            directory with files to zip
	 * @param zout
	 *            ZipOutputStream
	 * @param base
	 *            directory prefix for file entries inside the zip or null
	 * @throws IOException
	 */
    private static void zipDirectory(File dir, ZipOutputStream zout, String base) throws IOException {
        byte buffer[] = new byte[8192];
        String[] files = dir.list();
        if (files == null || files.length == 0) return;
        for (int i = 0; i < files.length; i++) {
            String path;
            if (base == null) {
                path = files[i];
            } else {
                path = base + "/" + files[i];
            }
            File f = new File(dir, files[i]);
            if (f.isDirectory()) zipDirectory(f, zout, path); else {
                ZipEntry zentry = new ZipEntry(path);
                zout.putNextEntry(zentry);
                FileInputStream inputStream = new FileInputStream(f);
                int len;
                while ((len = inputStream.read(buffer)) != -1) zout.write(buffer, 0, len);
                inputStream.close();
                zout.flush();
                zout.closeEntry();
            }
        }
    }
}
