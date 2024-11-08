package net.sourceforge.vplace;

import net.sourceforge.vplace.authentication.UserAuthentication;
import java.io.*;
import java.util.ResourceBundle;
import javax.servlet.*;
import javax.servlet.http.*;

public class ShowPopup extends HttpServlet {

    static ResourceBundle settings = ResourceBundle.getBundle("vpAuth");

    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        if (!VPHelpers.validate(req, res, this)) {
            return;
        }
        VPUser user = UserAuthentication.getUser(req);
        req.setAttribute("VPUser", user);
        StudentSession sess = (StudentSession) req.getSession().getAttribute("StudentSession");
        String placement = sess.getPlacement().getName();
        String tutor = sess.getTutor();
        String phase = sess.getPhase();
        String file = (String) req.getParameter("file");
        String forward = "../show-popup.jsp";
        String phaseVal = "";
        if (phase == null) {
            phase = "";
        }
        if (!phase.equals("")) {
            phaseVal = phase + "/";
        }
        if (file == null) {
            file = "";
        }
        if (file.equals("")) {
            file = "index.html";
        }
        String data = "";
        File indexFile = new File(settings.getString("vp.datadir") + "/placements/" + placement + "/" + phaseVal + file);
        if (!indexFile.exists()) {
            VPHelpers.error(req, res, "Placement file '" + placement + "/" + phaseVal + file + "' does not exist");
            return;
        }
        if (!indexFile.isFile() || !indexFile.canRead()) {
            VPHelpers.error(req, res, "Placement file '" + placement + "/" + phaseVal + file + "' cannot be read");
            return;
        }
        if (!file.toLowerCase().endsWith("html") && !file.toLowerCase().endsWith("htm")) {
            streamFile(req, res, indexFile);
            return;
        }
        FileReader dataSource = new FileReader(indexFile);
        StringWriter dataBuff = new StringWriter();
        try {
            while (dataSource.ready()) dataBuff.write(dataSource.read());
        } catch (IOException ioe) {
            VPHelpers.error(req, res, "Error reading file '" + placement + phaseVal + file + "'");
            return;
        }
        data = dataBuff.toString();
        data = resolveLinks(data, placement, tutor, phase);
        data = resolveImages(data, placement, tutor, phase);
        req.setAttribute("data", data);
        RequestDispatcher rd = req.getRequestDispatcher(forward);
        try {
            rd.forward(req, res);
        } catch (ServletException e) {
            VPHelpers.error(req, res, "Error forwarding to: " + forward + ".");
            return;
        }
    }

    public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        doGet(req, res);
    }

    public String resolveLinks(String in, String placement, String tutor, String phase) {
        String temp = in;
        temp = resolveToServer(temp, placement, tutor, phase, "href", "ShowPopup");
        return resolveToServer(temp, placement, tutor, phase, "HREF", "ShowPopup");
    }

    public String resolveImages(String in, String placement, String tutor, String phase) {
        String temp = in;
        temp = resolveToServer(temp, placement, tutor, phase, "src", "ImageLoader");
        return resolveToServer(temp, placement, tutor, phase, "SRC", "ImageLoader");
    }

    private String resolveToServer(String in, String placement, String tutor, String phase, String match, String server) {
        int pos = in.indexOf(match);
        int endpos = 0;
        int endstr = 0;
        String temp = "";
        while (pos != -1) {
            pos += (match.length() + 1);
            endstr = in.indexOf(" ", pos);
            if (in.charAt(pos) == '\"') {
                pos++;
                endstr = in.indexOf("\"", pos);
            }
            temp += in.substring(endpos, pos);
            if ((in.substring(pos, endstr).indexOf(":") == -1) && (in.charAt(pos) != '#')) {
                if (in.substring(pos + 3, pos + 12).equals("site.html")) {
                    temp += "ShowStatic?file=";
                    pos += 3;
                } else if (!in.substring(pos + 3, pos + 22).equals("virtualconsult.html") && !in.substring(pos + 3, pos + 12).equals("site.html")) {
                    temp += (server + "?phase=" + phase + "&file=");
                }
            }
            endpos = pos;
            pos = in.indexOf(match, endpos);
        }
        temp += in.substring(endpos);
        return temp;
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
