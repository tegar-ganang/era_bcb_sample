package servlet.util;

import java.io.*;
import java.net.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.zip.*;

/**
 *
 * @author Administrator
 * @version
 */
public class LogFileHandler extends HttpServlet {

    /** Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     */
    servlet.util.NewGenXMLGenerator newGenXMLGenerator = null;

    static final int BUFFER = 2048;

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        newGenXMLGenerator = servlet.util.NewGenXMLGenerator.getInstance();
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "inline; filename=c:/server1.zip");
        try {
            BufferedInputStream origin = null;
            FileOutputStream dest = new FileOutputStream("server.zip");
            ZipOutputStream zipOut = new ZipOutputStream(new BufferedOutputStream(dest));
            byte data[] = new byte[BUFFER];
            java.util.Properties props = new java.util.Properties();
            props.load(new java.io.FileInputStream(ejb.bprocess.util.NewGenLibRoot.getRoot() + "/SystemFiles/ENV_VAR.txt"));
            String jbossHomePath = props.getProperty("JBOSS_HOME");
            jbossHomePath = jbossHomePath.replaceAll("deploy", "log");
            FileInputStream fis = new FileInputStream(new File(jbossHomePath + "/server.log"));
            origin = new BufferedInputStream(fis, BUFFER);
            ZipEntry entry = new ZipEntry(jbossHomePath + "/server.log");
            zipOut.putNextEntry(entry);
            int count;
            while ((count = origin.read(data, 0, BUFFER)) != -1) {
                zipOut.write(data, 0, count);
            }
            origin.close();
            zipOut.closeEntry();
            java.io.FileInputStream fis1 = new java.io.FileInputStream(new java.io.File("server.zip"));
            java.nio.channels.FileChannel fc1 = fis1.getChannel();
            int length1 = (int) fc1.size();
            byte buffer[] = new byte[length1];
            System.out.println("size of zip file = " + length1);
            fis1.read(buffer);
            OutputStream out1 = response.getOutputStream();
            out1.write(buffer);
            fis1.close();
            out1.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String parseISToString(InputStream is) {
        DataInputStream din = new DataInputStream(is);
        StringBuffer sb = new StringBuffer();
        try {
            String line = null;
            while ((line = din.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (Exception ex) {
            ex.getMessage();
        } finally {
            try {
                is.close();
            } catch (Exception ex) {
            }
        }
        return sb.toString();
    }

    /** Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    /** Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    /** Returns a short description of the servlet.
     */
    public String getServletInfo() {
        return "Short description";
    }
}
