package org.ethontos.owlwatcher.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;
import org.ethontos.owlwatcher.exceptions.OWFileCopyException;
import org.ethontos.owlwatcher.view.TabView;

/**
 * 
 * @author peter
 * Last update: May 18, 2009
 *
 */
public class FileUtils {

    public static final Pattern dotPattern = Pattern.compile("\\.");

    public static final Pattern slashPattern = Pattern.compile("/");

    public static File handleFileExtension(File userChoice, String preferenceID, String extension) {
        final String rootName = userChoice.getName();
        final Preferences userPrefs = Preferences.userNodeForPackage(TabView.class);
        boolean addExtension = userPrefs.getBoolean(preferenceID, true);
        int dotPos = rootName.lastIndexOf(".");
        String pName;
        if (addExtension) if (dotPos > -1) if (dotPos == rootName.lastIndexOf(extension)) pName = rootName; else pName = rootName.substring(0, dotPos) + extension; else pName = rootName + extension; else pName = rootName;
        return new File(userChoice.getParentFile(), pName);
    }

    /**
     * Local file copy
     * @param fromFile source for copy
     * @param toFile destination for copy
     * @throws OWFileCopyException
     */
    public static void copyFile(File fromFile, File toFile) throws OWFileCopyException {
        try {
            FileChannel src = new FileInputStream(fromFile).getChannel();
            FileChannel dest = new FileOutputStream(toFile).getChannel();
            dest.transferFrom(src, 0, src.size());
            src.close();
            dest.close();
        } catch (IOException e) {
            throw (new OWFileCopyException("An error occurred while copying a file", e));
        }
    }

    /**
     * Copies the contents of a URI to a local file
     * @param from source to copy from
     * @param toFile local destination
     * @return true unless an IOException occurs 
     */
    public static boolean copyURItoFile(URI from, File toFile) {
        try {
            final URL fromURL = from.toURL();
            final InputStream fromStream = fromURL.openStream();
            final FileOutputStream toStream = new FileOutputStream(toFile);
            final byte[] buf = new byte[2048];
            int byteCount = fromStream.read(buf);
            while (byteCount > 0) {
                toStream.write(buf, 0, byteCount);
                byteCount = fromStream.read(buf);
            }
            fromStream.close();
            toStream.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
