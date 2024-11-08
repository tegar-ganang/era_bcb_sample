package net.sourceforge.processdash.tool.bridge.report;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import net.sourceforge.processdash.tool.bridge.ResourceCollection;
import net.sourceforge.processdash.util.FileUtils;

/**
 * A report which generates a ZIP archive of the resources in a collection.
 */
public class ResourceContentStream implements CollectionReport {

    public static final ResourceContentStream INSTANCE = new ResourceContentStream();

    public static final String MANIFEST_FILENAME = "manifest.xml";

    public String getContentType() {
        return "application/zip";
    }

    public void runReport(ResourceCollection c, List<String> resources, OutputStream out) throws IOException {
        ZipOutputStream zipOut = new ZipOutputStream(out);
        zipOut.setLevel(9);
        ZipEntry e = new ZipEntry(MANIFEST_FILENAME);
        zipOut.putNextEntry(e);
        XmlCollectionListing.INSTANCE.runReport(c, resources, zipOut);
        zipOut.closeEntry();
        for (String resourceName : resources) {
            long lastMod = c.getLastModified(resourceName);
            if (lastMod < 1) continue;
            Long checksum = c.getChecksum(resourceName);
            if (checksum == null) continue;
            e = new ZipEntry(resourceName);
            e.setTime(lastMod);
            zipOut.putNextEntry(e);
            InputStream resourceContent = c.getInputStream(resourceName);
            try {
                FileUtils.copyFile(resourceContent, zipOut);
            } finally {
                resourceContent.close();
            }
            zipOut.closeEntry();
        }
        zipOut.finish();
    }
}
