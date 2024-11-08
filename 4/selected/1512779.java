package org.openthinclient.servlet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;

public class FileServiceServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger logger = Logger.getLogger(FileServiceServlet.class);

    private final File basedir;

    public FileServiceServlet() {
        basedir = new File(System.getProperty("jboss.server.data.dir"), "nfs" + File.separator + "root");
    }

    public String[] listFiles(String dirName) throws IOException {
        File dir = makeFile(dirName);
        if (logger.isDebugEnabled()) logger.debug("Listing files in " + dir);
        return dir.list();
    }

    public ByteArrayInputStream getFile(String fileName) throws IOException {
        File file = makeFile(fileName);
        if (logger.isDebugEnabled()) logger.debug("Getting file " + file);
        FileInputStream is = new FileInputStream(file);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte buffer[] = new byte[1024];
        int read;
        while ((read = is.read(buffer)) > 0) baos.write(buffer, 0, read);
        is.close();
        return null;
    }

    /**
	 * @param dir
	 * @return
	 * @throws IOException
	 */
    private File makeFile(String name) throws IOException {
        File f = new File(basedir, name);
        String canonicalPath = f.getCanonicalPath();
        if (!canonicalPath.startsWith(basedir.getCanonicalPath())) throw new IOException("The file named " + name + " can or may not be resolved.");
        return f;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        File f = makeFile(request.getPathInfo());
        if (!f.exists() || !f.canRead()) {
            if (logger.isDebugEnabled()) logger.debug("Won't serve this file: " + f);
            response.sendError(404);
        } else {
            if (f.isDirectory()) {
                if (logger.isDebugEnabled()) logger.debug("Listing Directory: " + f);
                response.setContentType("text/plain");
                response.setCharacterEncoding("ISO-8859-1");
                PrintWriter w = response.getWriter();
                for (File file : f.listFiles()) {
                    w.write(file.isDirectory() ? "D " : "F ");
                    w.write(file.getName());
                    w.write("\n");
                }
            } else {
                if (logger.isDebugEnabled()) logger.debug("Getting file: " + f);
                response.setContentType("application/octet-stream");
                ServletOutputStream os = response.getOutputStream();
                FileInputStream is = new FileInputStream(f);
                byte buffer[] = new byte[1024];
                int read;
                while ((read = is.read(buffer)) > 0) os.write(buffer, 0, read);
                is.close();
                os.flush();
            }
        }
    }
}
