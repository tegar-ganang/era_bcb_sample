package net.sourceforge.processdash.ui.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.net.http.TinyCGIException;
import net.sourceforge.processdash.tool.export.DataExtractionScaffold;
import net.sourceforge.processdash.util.DataStreamCache;
import net.sourceforge.processdash.util.FileUtils;

public class PngCache extends TinyCGIBase {

    private static final boolean USE_DISK_CACHE = !Settings.getBool(DataExtractionScaffold.SCAFFOLD_MODE_SETTING, false);

    private static final DataStreamCache PNG_CACHE = new DataStreamCache(USE_DISK_CACHE);

    static OutputStream getOutputStream() {
        return PNG_CACHE.getOutputStream();
    }

    @Override
    protected void doGet() throws IOException {
        int streamID = Integer.parseInt(getParameter("id"));
        InputStream pngData = PngCache.PNG_CACHE.getInputStream(streamID);
        if (pngData == null) throw new TinyCGIException(HttpURLConnection.HTTP_NOT_FOUND, "Not Found", "Not Found");
        out.print("Content-type: image/png\r\n\r\n");
        out.flush();
        FileUtils.copyFile(pngData, outStream);
        outStream.flush();
        outStream.close();
    }
}
