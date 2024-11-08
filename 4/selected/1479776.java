package servlet.util;

import java.io.*;
import java.net.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import javax.servlet.*;
import javax.servlet.http.*;

/**
 *
 * @author Administrator
 * @version
 */
public class DBFileHandler extends HttpServlet {

    /** Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/xml;charset=UTF-8");
        java.util.Properties props = new java.util.Properties();
        props.load(new java.io.FileInputStream(ejb.bprocess.util.NewGenLibRoot.getRoot() + "/SystemFiles/Env_Var.txt"));
        String jbossHomePath = props.getProperty("JBOSS_HOME");
        FileInputStream fis = new FileInputStream(new File(jbossHomePath + "/mail-service.xml"));
        java.nio.channels.FileChannel fc = fis.getChannel();
        int length = (int) fc.size();
        byte buffer[] = new byte[length];
        BufferedInputStream bis = new BufferedInputStream(fis, length);
        OutputStream out = response.getOutputStream();
        int count;
        while ((count = bis.read(buffer, 0, length)) != -1) {
            out.write(buffer, 0, length);
        }
        fis.close();
        bis.close();
        out.close();
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
