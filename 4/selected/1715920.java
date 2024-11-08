package org.cishell.testing.webapp;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A servlet for downloading files from the server. The path for accessing
 * should be something like [host]/cishell/download_file?filename=FILENAME
 * 
 * @author dmcoe
 * 
 */
public class ReturnFile extends HttpServlet {

    private static final long serialVersionUID = 1L;

    public static final String RETURN_FILE_URL = "/download_file";

    private static final String RETURN_FILE_URL_GET_PARAMETER = "filename";

    private static final String DIRECTORY_PATH = Activator.FILE_DIRECTORY;

    private File directory;

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        directory = new File(DIRECTORY_PATH);
        if (!directory.isDirectory()) {
            throw new ServletException(DIRECTORY_PATH + " is not a directory");
        }
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String filename = request.getParameter(RETURN_FILE_URL_GET_PARAMETER);
        File file = new File(directory.getPath() + File.separatorChar + filename);
        if (file.exists() && !file.isDirectory()) {
            PrintWriter out = null;
            BufferedInputStream buf = null;
            response.setContentLength((int) file.length());
            response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            try {
                out = response.getWriter();
                buf = new BufferedInputStream(new FileInputStream(file));
                int readBytes = 0;
                while ((readBytes = buf.read()) != -1) {
                    out.write(readBytes);
                }
            } catch (IOException e) {
                throw new ServletException(e.getMessage());
            } finally {
                if (out != null) {
                    out.close();
                }
                if (buf != null) {
                    buf.close();
                }
            }
        } else {
            response.setContentType("text/html");
            Writer outHtml = response.getWriter();
            outHtml.write("<html><body>");
            outHtml.write("<p>There is no such file as " + filename);
            outHtml.write("</body></html>");
        }
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    /**
	 * An html link that would download the given filename.
	 * 
	 * @param filename
	 *            The filename of the file to be downloaded
	 * 
	 * @return Return an html link in string form
	 */
    public static String returnFileLink(String filename) {
        String html = "<a href=\"" + Activator.WEBAPP_ROOT_URL + RETURN_FILE_URL + "?" + RETURN_FILE_URL_GET_PARAMETER + "=" + filename + "\">" + filename + "</a>";
        return html;
    }
}
