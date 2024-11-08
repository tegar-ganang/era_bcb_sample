package net.sf.xpontus.utils;

import net.sf.xpontus.constants.XPontusConstantsIF;
import org.apache.commons.collections.map.ListOrderedMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import javax.activation.MimetypesFileTypeMap;

/**
 *
 * @author Yves Zoundi <yveszoundi at users dot sf dot net>
 */
public class MimeTypesProvider {

    private static MimeTypesProvider INSTANCE;

    private XPontusMimetypesFileTypeMap provider;

    private MimeTypesProvider() {
        File mimeTypesFile = new File(XPontusConstantsIF.XPONTUS_HOME_DIR, "mimes.properties");
        try {
            if (!mimeTypesFile.exists()) {
                OutputStream os = null;
                InputStream is = getClass().getResourceAsStream("/net/sf/xpontus/configuration/mimetypes.properties");
                os = FileUtils.openOutputStream(mimeTypesFile);
                IOUtils.copy(is, os);
                IOUtils.closeQuietly(is);
                IOUtils.closeQuietly(os);
            }
            provider = new XPontusMimetypesFileTypeMap(mimeTypesFile.getAbsolutePath());
            MimetypesFileTypeMap.setDefaultFileTypeMap(provider);
        } catch (Exception err) {
            err.printStackTrace();
        }
    }

    /**
     *
     * @param m
     */
    public void addMimeTypes(String m) {
        ((XPontusMimetypesFileTypeMap) provider).addMimeTypes(m);
    }

    /**
     * Returns the mime type for the given file or text/plain
     * @param input The file object
     * @return
     */
    public String getMimeTypeForFile(File input) {
        return provider.getContentType(input);
    }

    /**
     *
     * @param input The file path or filename
     * @return The mime type for the given file or text/plain
     */
    public String getMimeType(String input) {
        return provider.getContentType(input);
    }

    /**
     *
     * @return
     */
    public static MimeTypesProvider getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new MimeTypesProvider();
        }
        return INSTANCE;
    }

    public Map<String, List<String>> getContentTypesMap() {
        return provider.getContentTypesMap();
    }

    public ListOrderedMap getTypes() {
        return provider.getTypes();
    }
}
