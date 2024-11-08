package fi.hip.gb.server.servlet;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import fi.hip.gb.core.Config;
import fi.hip.gb.core.SessionHandler;
import fi.hip.gb.core.Storage;
import fi.hip.gb.core.WorkResult;
import fi.hip.gb.utils.FileUtils;

/**
 * Download agent JAR (/dl/jarfile.jar) and result files 
 * (/dl/12345/resultfile.txt) through HTTP.
 * 
 * @author Juho Karppinen
 * @version $Id: DlServlet.java 1025 2006-04-21 15:07:21Z jkarppin $
 */
public class DlServlet extends HttpServlet {

    /**
	 * @param request request object
	 * @param response our response object
	 * @throws ServletException if error occurred
	 * @throws IOException if error occurred
	 */
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final String filePath = request.getPathInfo().substring(1);
        if (filePath != null) {
            try {
                int index = filePath.indexOf("/");
                if (index != -1) {
                    Long jobID = Long.parseLong(filePath.substring(0, index));
                    Storage storage = SessionHandler.getInstance().getSession(jobID);
                    WorkResult result = storage.getResult().findResults(new String[] { filePath.substring(index + 1) });
                    sendFile(result.firstResult().fileURL().getPath(), response);
                    return;
                }
            } catch (NumberFormatException nfe) {
            }
            FileFilter ff = new FileFilter() {

                public boolean accept(File pathname) {
                    return pathname.getName().endsWith(filePath);
                }
            };
            String[] deployDirs = Config.getInstance().getPluginDirectories();
            for (int i = 0; i < deployDirs.length; i++) {
                File[] deployFiles = null;
                if (deployDirs[i].startsWith("/")) deployFiles = new File(deployDirs[i]).listFiles(ff); else deployFiles = new File(Config.getInstance().getDataDirectory() + "/" + deployDirs[i]).listFiles(ff);
                if (deployFiles != null && deployFiles.length > 0) {
                    sendFile(deployFiles[0].getAbsolutePath(), response);
                }
            }
        }
    }

    /**
     * Sends a file to the client.
     * @param filePath path of the file to send
     * @param response servlet response
     * @throws IOException
     */
    private void sendFile(String filePath, HttpServletResponse response) throws IOException {
        String filename = FileUtils.getFilename(filePath);
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(filePath);
            if (in != null) {
                out = new BufferedOutputStream(response.getOutputStream());
                in = new BufferedInputStream(in);
                response.setContentType("application/unknown");
                response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
                int c;
                while ((c = in.read()) != -1) out.write(c);
                return;
            }
        } finally {
            if (in != null) try {
                in.close();
            } catch (Exception e) {
            }
            if (out != null) try {
                out.close();
            } catch (Exception e) {
            }
        }
    }
}
