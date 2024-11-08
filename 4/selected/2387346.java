package hu.sztaki.lpds.submitter.admin;

import hu.sztaki.lpds.information.local.PropertyLoader;
import hu.sztaki.lpds.submitter.grids.boinc.BoincDataBaseHandler;
import hu.sztaki.lpds.submitter.grids.boinc.BridgeHandler;
import hu.sztaki.lpds.submitter.service.valery.Base;
import hu.sztaki.lpds.submitter.service.valery.Runner;
import hu.sztaki.lpds.submitter.service.valery.RunnerManagerThread;
import hu.sztaki.lpds.submitter.service.valery.util.QM;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author krisztian
 */
public class GuseServiceAdminServlet extends HttpServlet {

    /** 
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        if (request.getParameter("p") == null || "properies".equals(request.getParameter("p"))) {
            request.setAttribute("data", PropertyLoader.getInstance().getAllPropertyes());
            request.setAttribute("stat", Monitor.getI().getStats("system"));
            request.setAttribute("mem", new MemoryBean());
            request.setAttribute("menu", new String[] { "menu.actualram", "menu.sysevents", "menu.properties" });
        } else if ("jobs".equals(request.getParameter("p"))) {
            request.setAttribute("data", Base.getI().getMgs());
            String thID = request.getParameter("t");
            if (thID != null) {
                Enumeration<RunnerManagerThread> enm = Base.getI().getMgs().keys();
                RunnerManagerThread tmp;
                while (enm.hasMoreElements()) {
                    tmp = enm.nextElement();
                    if (tmp.getName().equals(thID)) {
                        request.setAttribute("rtdata", Base.getI().getMgs().get(tmp));
                        request.setAttribute("size", Base.getI().getMgs().get(tmp).size());
                        request.setAttribute("stat", Monitor.getI().getStats(tmp.getName()));
                    }
                }
            }
        } else if ("bridge".equals(request.getParameter("p"))) {
            request.setAttribute("data", BoincDataBaseHandler.getI().getClients());
            if (request.getParameter("t") != null) {
                BridgeHandler bh = BoincDataBaseHandler.getI().getClients().get(request.getParameter("t"));
                request.setAttribute("actulagrid", BoincDataBaseHandler.getI().getGridname());
                request.setAttribute("action", BoincDataBaseHandler.getI().getStat());
                request.setAttribute("size_download", bh.getDownloadQueue().getJobid().size());
                request.setAttribute("size_submit", bh.getSubmitQueue().size());
                request.setAttribute("size_status", bh.getStatusQueue().getJobid().size());
                request.setAttribute("size_delete", bh.getDeleteQueue().getJobid().size());
                request.setAttribute("stat", Monitor.getI().getStats("bridge-" + request.getParameter("t")));
                if ("download".equals(request.getParameter("q"))) request.setAttribute("bdata", bh.getDownloadQueue().getJobid()); else if ("submit".equals(request.getParameter("q"))) request.setAttribute("bdata", bh.getSubmitQueue()); else if ("status".equals(request.getParameter("q"))) request.setAttribute("bdata", bh.getStatusQueue().getJobid()); else if ("delete".equals(request.getParameter("q"))) request.setAttribute("bdata", bh.getDeleteQueue().getJobid());
            }
        } else if ("status".equals(request.getParameter("p"))) {
            Enumeration<String> enm = QM.getWFIs();
            List<String> tmp = new ArrayList();
            while (enm.hasMoreElements()) tmp.add(enm.nextElement());
            request.setAttribute("data", tmp);
            if (request.getParameter("t") != null) request.setAttribute("stat", Monitor.getI().getStats(request.getParameter("t")));
        } else if (request.getParameter("log") != null) {
            Runner tmp = Base.getI().getRunner(request.getParameter("log"));
            File f = tmp.getLogFile();
            BufferedReader br = new BufferedReader(new FileReader(f));
            String ln = "";
            PrintWriter out = response.getWriter();
            while ((ln = br.readLine()) != null) out.write(ln);
            br.close();
            out.close();
        }
        getServletContext().getRequestDispatcher("/WEB-INF/jsps/index.jsp").include(request, response);
    }

    /** 
     * Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    /** 
     * Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    /** 
     * Returns a short description of the servlet.
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }
}
