package net.sourceforge.vplace;

import net.sourceforge.vplace.authentication.UserAuthentication;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.io.*;
import java.util.ResourceBundle;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.*;

public class TutorData extends HttpServlet {

    static ResourceBundle settings = ResourceBundle.getBundle("vpAuth");

    static Log logging = LogFactory.getLog(TutorData.class);

    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        VPUser tutor = UserAuthentication.getUser(req);
        req.setAttribute("VPUser", tutor);
        String placement = req.getParameter("placement");
        if ((placement == null) || placement.equals("")) {
            VPHelpers.error(req, res, "missing placement parameter");
        }
        String delete = req.getParameter("delete-student");
        if ((delete != null) && (delete != "")) {
            deleteStudent(delete, placement, tutor, req, res);
            return;
        }
        String origReq = req.getParameter("file");
        String student = req.getParameter("student");
        if ((origReq == null) || origReq.equals("") || (student == null) || student.equals("")) {
            VPHelpers.error(req, res, "missing parameter");
        }
        String file = StudentSession.getDataDirFor(student, placement, tutor.getUserName()) + "/" + origReq;
        logging.debug("Looking up file " + file);
        File theFile = new File(file);
        if (!theFile.exists() || !theFile.canRead()) {
            VPHelpers.error(req, res, "Cannot read file \"" + file + "\".");
        }
        streamFile(req, res, theFile);
    }

    public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        doGet(req, res);
    }

    private boolean streamFile(HttpServletRequest req, HttpServletResponse res, File source) throws IOException {
        if (!source.exists() || !source.canRead()) {
            VPHelpers.error(req, res, "Cannot read file \"" + source.getPath() + "\".");
        }
        try {
            OutputStream out = res.getOutputStream();
            InputStream in = new FileInputStream(source);
            while (in.available() > 0) out.write(in.read());
            in.close();
            out.close();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public void deleteStudent(String student, String placement, VPUser tutor, HttpServletRequest req, HttpServletResponse res) throws IOException {
        String sure = req.getParameter("sure");
        if ((sure == null) || sure.equals("")) {
            req.setAttribute("delete-status", "confirm");
        }
        if ((sure != null) && sure.equalsIgnoreCase("yes")) {
            String dir = StudentSession.getDataDirFor(student, placement, tutor.getUserName());
            try {
                recurseDelete(new File(dir));
                req.setAttribute("delete-status", "done");
                logging.info("Deleted " + student + "'s data for placement " + placement);
            } catch (IOException e) {
                req.setAttribute("delete-status", "failed");
                logging.error("Failed to delete " + student + "'s data for placement " + placement, e);
            }
        }
        String forward = "../list-placement-files.jsp";
        RequestDispatcher rd = req.getRequestDispatcher(forward);
        logging.debug("forwarding to " + forward);
        try {
            rd.forward(req, res);
        } catch (ServletException e) {
            logging.error("Could not forward to " + forward, e);
            VPHelpers.error(req, res, "Error forwarding to: " + forward + ".");
            return;
        }
    }

    private boolean recurseDelete(File dir) throws IOException {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (!recurseDelete(files[i])) {
                    throw new IOException("could not delete " + files[i]);
                }
            }
        }
        if (!dir.delete()) {
            throw new IOException("could not delete " + dir);
        }
        return true;
    }
}
