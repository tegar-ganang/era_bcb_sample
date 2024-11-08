package fi.hip.gb.disk.transport.http;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.DiskFileUpload;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import fi.hip.gb.disk.Coordinator;
import fi.hip.gb.disk.conf.Config;
import fi.hip.gb.disk.info.FileInfo;
import fi.hip.gb.disk.transport.jgroups.JGroupsServer;

/**
 * A servlet to receive a single file upload. 
 * <p>
 * Depending of the <code>coordinator</code> init parameter
 * in web.xml this servlet acts either as a plain storage element
 * or coordinator.
 * 
 * @author Mikko Pitkanen
 * @version $Id$
 */
public class StorageServlet extends HttpServlet {

    /**
     * 
     */
    private static final long serialVersionUID = -2922018771770666717L;

    /** path to permanent storage directory */
    private String storagePath;

    /** should we act as coordinator */
    private boolean isCoordinator = false;

    /** our configuration file */
    private String confFile;

    private Log log = LogFactory.getLog(StorageServlet.class);

    private Log pLog = LogFactory.getLog("fi.hip.gb.perf.run");

    public void init(ServletConfig config) throws ServletException {
        confFile = config.getInitParameter("config");
        if (confFile == null || confFile.length() == 0) confFile = "http-disk";
        String coordinator = config.getInitParameter("coordinator");
        if (coordinator != null && coordinator.equalsIgnoreCase("true")) {
            this.isCoordinator = true;
        }
        new Config(confFile, "conf");
        if (Config.getJGroupsProperties() != null) {
            JGroupsServer.getInstance();
        }
        this.storagePath = Config.getSiloDir();
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        log.info("in method doGet");
        if (request.getParameter("operation") == null) {
            log.info("no matching operation lets get file");
            final String filePath = request.getPathInfo().substring(1);
            if (filePath.length() > 0) {
                File fileToClient = new File(storagePath + "/" + filePath);
                Coordinator coord = null;
                if (this.isCoordinator) {
                    try {
                        coord = new Coordinator(confFile);
                        coord.get(fileToClient);
                    } catch (InterruptedException e) {
                        throw new IOException(e.getMessage());
                    }
                }
                long sInterval = System.currentTimeMillis();
                sendFile(fileToClient, response);
                if (this.isCoordinator) {
                    pLog.debug(coord.logBuffer.toString() + "\t" + (System.currentTimeMillis() - sInterval));
                    fileToClient.delete();
                }
            } else {
                PrintWriter display = response.getWriter();
                response.setContentType("text/html");
                display.println("<form method=\"post\" " + "action=\"\" " + "enctype=\"multipart/form-data\">" + "<input type=\"file\" name=\"file1\">" + "<input type=\"submit\"> </form>");
                String[] files = null;
                if (this.isCoordinator) {
                    FileInfo[] infos = new Coordinator(confFile).getInfoSystem().listFiles();
                    files = new String[infos.length];
                    for (int i = 0; i < infos.length; i++) {
                        files[i] = infos[i].getFileName();
                    }
                } else {
                    files = new File(storagePath).list();
                }
                for (int i = 0; i < files.length; i++) {
                    display.println("<a href=\"" + files[i] + "\">");
                    display.println(files[i]);
                    display.println("</a><br/>");
                }
                display.close();
            }
        } else if (request.getParameter("operation").equals("delete")) {
            String toDelete = (String) request.getParameter("filename");
            log.info("Requsting for delete: " + this.storagePath + "/" + toDelete);
            if (new File(this.storagePath + "/" + toDelete).delete()) {
            } else if (this.isCoordinator) {
                try {
                    new Coordinator(this.confFile).delete(toDelete);
                } catch (InterruptedException e) {
                    log.warn("could NOT delete: " + toDelete + " " + e.getMessage());
                }
            } else {
                log.info("INFO: could NOT delete: " + this.storagePath + "/" + toDelete);
            }
        } else if (request.getParameter("operation").equals("exists")) {
            String toCheck = (String) request.getParameter("filename");
            log.info("Checking for file: " + this.storagePath + "/" + toCheck);
            if (new File(this.storagePath + "/" + toCheck).exists()) {
                log.info("found: " + this.storagePath + "/" + toCheck);
                response.getOutputStream().write(1);
            } else if (this.isCoordinator) {
                if (new Coordinator(this.confFile).getInfoSystem().findFile(toCheck) != null) response.getOutputStream().write(1); else response.getOutputStream().write(0);
            } else {
                log.info("could NOT find: " + this.storagePath + "/" + toCheck);
                response.getOutputStream().write(0);
            }
            return;
        }
    }

    /**
     * A servlet to receive a single file upload. 
     * Note that this servlet assumes a specific part name and creates a 
     * specific output file, both determined at compile time.
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     * @throws ServletException
     * @throws IOException
     */
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        long sInterval = System.currentTimeMillis();
        log.info("in server doPost");
        DiskFileUpload upload = new DiskFileUpload();
        File outFile = null;
        try {
            List items = upload.parseRequest(request);
            Iterator iter = items.iterator();
            while (iter.hasNext()) {
                FileItem item = (FileItem) iter.next();
                String fileName = item.getName();
                long sizeInBytes = item.getSize();
                log.info("putting file: " + fileName);
                outFile = new File(storagePath + "/" + fileName);
                if (outFile.exists() && outFile.length() == sizeInBytes) {
                    log.info("not putting, already exists: " + storagePath + "/" + fileName);
                    continue;
                }
                InputStream input = item.getInputStream();
                FileOutputStream fileStream = new FileOutputStream(outFile);
                byte[] bt = new byte[10000];
                int cnt = input.read(bt);
                while (cnt != -1) {
                    fileStream.write(bt, 0, cnt);
                    cnt = input.read(bt);
                }
                input.close();
                fileStream.close();
                if (this.isCoordinator) {
                    Coordinator coord = new Coordinator(confFile);
                    coord.logBuffer.append("put \t" + outFile.length() + "\t" + (System.currentTimeMillis() - sInterval));
                    coord.put(outFile);
                    pLog.debug(coord.logBuffer.toString());
                    outFile.delete();
                }
            }
        } catch (IOException e) {
            log.info("server doPost" + e.getMessage());
            if (outFile != null) {
                outFile.delete();
            }
            throw e;
        } catch (FileUploadException fe) {
            log.info("server doPost" + fe.getMessage());
            throw new IOException("server doPost" + fe.getMessage());
        }
    }

    /**
     * Sends a file to the client.
     * @param filePath path of the file to send
     * @param response servlet response
     * @throws IOException
     */
    private void sendFile(File fileToSend, HttpServletResponse response) throws IOException {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(fileToSend);
            if (in != null) {
                out = new BufferedOutputStream(response.getOutputStream());
                in = new BufferedInputStream(in);
                response.setContentType("application/unknown");
                response.setHeader("Content-Disposition", "attachment; filename=\"" + fileToSend.getName() + "\"");
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
