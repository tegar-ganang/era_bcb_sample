package net.sourceforge.vplace;

import net.sourceforge.vplace.authentication.UserAuthentication;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.io.*;
import java.util.ResourceBundle;
import javax.servlet.http.*;

public class StudentData extends HttpServlet {

    static ResourceBundle settings = ResourceBundle.getBundle("vpAuth");

    static Log logging = LogFactory.getLog(StudentData.class);

    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        if (!VPHelpers.validate(req, res, this)) {
            return;
        }
        VPUser user = UserAuthentication.getUser(req);
        req.setAttribute("VPUser", user);
        StudentSession sess = (StudentSession) req.getSession().getAttribute("StudentSession");
        String file = (String) req.getParameter("file");
        if ((file == null) || file.equals("")) {
            VPHelpers.error(req, res, "No file requested to display");
        }
        String origReq = file;
        file = sess.getDataDir() + "/" + file;
        logging.debug("Looking up file " + file);
        File theFile = new File(file);
        if (!theFile.exists() || !theFile.canRead()) {
            VPHelpers.error(req, res, "Cannot read file \"" + origReq + "\".");
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
}
