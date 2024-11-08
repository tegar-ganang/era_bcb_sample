package servlet.acquisitions;

import java.io.*;
import java.net.*;
import javax.servlet.*;
import javax.servlet.http.*;

/**
 *
 * @author  Administrator
 * @version
 */
public class AccessioningReportServlet extends HttpServlet {

    servlet.util.NewGenXMLGenerator newGenXMLGenerator = null;

    servlet.util.HomeFactory homeFactory = null;

    /** Initializes the servlet.
     */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            home = (ejb.bprocess.acquisitions.AccessioningReportSessionHome) ejb.bprocess.util.HomeFactory.getInstance().getRemoteHome("AccessioningReportSession");
        } catch (Exception exp) {
            exp.printStackTrace();
        }
        newGenXMLGenerator = servlet.util.NewGenXMLGenerator.getInstance();
    }

    /** Destroys the servlet.
     */
    public void destroy() {
    }

    /** Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            String xml = "";
            java.io.InputStream ios = request.getInputStream();
            String xmlStr = newGenXMLGenerator.getXMLDocument(ios);
            org.jdom.Document doc = newGenXMLGenerator.getDocFromXMLDocument(xmlStr);
            String operationid = doc.getRootElement().getAttributeValue("no");
            if (operationid.equals("1")) {
                javax.servlet.http.HttpSession session = request.getSession(true);
                xml = (new AccessioningReportServletHandler(home)).getAccessionDetails(doc, session);
                try {
                } catch (Exception exp) {
                    exp.printStackTrace();
                }
                java.io.OutputStream os = response.getOutputStream();
                newGenXMLGenerator.compressXMLDocument(os, xml);
            } else if (operationid.equals("2")) {
                xml = (new AccessioningReportServletHandler(home)).getAccessionSeriesDetails(doc);
                try {
                } catch (Exception exp) {
                    exp.printStackTrace();
                }
                java.io.OutputStream os = response.getOutputStream();
                newGenXMLGenerator.compressXMLDocument(os, xml);
            } else if (operationid.equals("3")) {
                xml = (new AccessioningReportServletHandler(home)).getCompleteRecord(doc);
                try {
                } catch (Exception exp) {
                    exp.printStackTrace();
                }
                java.io.OutputStream os = response.getOutputStream();
                newGenXMLGenerator.compressXMLDocument(os, xml);
            } else if (operationid.equals("4")) {
                String file = doc.getRootElement().getChildText("FileName");
                java.nio.channels.FileChannel fc = (new java.io.FileInputStream(file)).getChannel();
                int fileLength = (int) fc.size();
                java.nio.MappedByteBuffer bb = fc.map(java.nio.channels.FileChannel.MapMode.READ_ONLY, 0, fileLength);
                java.util.StringTokenizer stk = new java.util.StringTokenizer(file, ".");
                stk.nextToken();
                String ext = stk.nextToken();
                String contenttype = "";
                try {
                    contenttype = java.util.ResourceBundle.getBundle("server").getString(ext.trim().toUpperCase());
                } catch (Exception e) {
                    java.util.Properties p1 = new java.util.Properties();
                    java.io.File propfile = new java.io.File(ejb.bprocess.util.NewGenLibRoot.getRoot() + "/SystemFiles/ContentTypes.properties");
                    java.io.FileInputStream f1 = new java.io.FileInputStream(ejb.bprocess.util.NewGenLibRoot.getRoot() + java.util.ResourceBundle.getBundle("server").getString("SystemFilesPath") + "/ContentTypes.properties");
                    p1.load(f1);
                    contenttype = p1.getProperty(ext.trim().toUpperCase(), "");
                }
                response.setContentType(contenttype);
                java.nio.channels.WritableByteChannel wbc = java.nio.channels.Channels.newChannel(response.getOutputStream());
                wbc.write(bb);
            }
        } catch (Exception exp) {
            exp.printStackTrace();
        }
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

    private ejb.bprocess.acquisitions.AccessioningReportSessionHome home;
}
