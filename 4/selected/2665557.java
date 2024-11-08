package net.sourceforge.processdash.tool.bridge.report;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import net.sourceforge.processdash.tool.bridge.ResourceCollection;
import net.sourceforge.processdash.util.FileUtils;

public class ResourceBackupStream implements CollectionReport {

    public String getContentType() {
        return "application/zip";
    }

    public void runReport(ResourceCollection collection, List<String> resourceNames, OutputStream out) throws IOException {
        InputStream in = collection.getBackupInputStream();
        FileUtils.copyFile(in, out);
        in.close();
    }

    public static final ResourceBackupStream INSTANCE = new ResourceBackupStream();
}
