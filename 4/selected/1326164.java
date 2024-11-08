package net.sourceforge.vplace;

import net.sourceforge.vplace.authentication.UserAuthentication;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.io.*;
import java.text.DateFormat;
import java.util.ResourceBundle;
import javax.servlet.*;
import javax.servlet.http.*;

public class ShowPlacement extends HttpServlet {

    static ResourceBundle settings = ResourceBundle.getBundle("vpAuth");

    static Log logging = LogFactory.getLog(ShowPlacement.class);

    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        if (!VPHelpers.validate(req, res, this)) {
            return;
        }
        VPUser user = UserAuthentication.getUser(req);
        req.setAttribute("VPUser", user);
        StudentSession sess = (StudentSession) req.getSession().getAttribute("StudentSession");
        VPPlacement placement = sess.getPlacement();
        String placementStr = placement.getName();
        String tutor = sess.getTutor();
        String display = req.getParameter("display");
        if (display == null) {
            display = "";
        }
        String phase = sess.getPhase();
        if (display.equals("root")) {
            phase = "";
        }
        String file = (String) req.getParameter("file");
        String forward = "../show-placement.jsp";
        String phaseVal = "";
        logging.debug("Showing placement " + placementStr + " phase " + phase + " file " + file + " display " + display);
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
        if (!file.toLowerCase().endsWith("html") && !file.toLowerCase().endsWith("htm")) {
            streamFile(req, res, new File(settings.getString("vp.datadir") + "/placements/" + placementStr + "/" + phaseVal + file));
            return;
        } else if (display.equals("contents")) {
            data += VPHelpers.contentsPage((VPStudent) user, placement);
        } else {
            File indexFile = new File(settings.getString("vp.datadir") + "/placements/" + placementStr + "/" + phaseVal + file);
            if (!indexFile.exists()) {
                VPHelpers.error(req, res, "Placement file '" + placementStr + "/" + phaseVal + file + "' does not exist");
                return;
            }
            if (!indexFile.isFile() || !indexFile.canRead()) {
                VPHelpers.error(req, res, "Placement file '" + placementStr + "/" + phaseVal + file + "' cannot be read");
                return;
            }
            FileReader dataSource = new FileReader(indexFile);
            StringWriter dataBuff = new StringWriter();
            try {
                while (dataSource.ready()) dataBuff.write(dataSource.read());
            } catch (IOException ioe) {
                VPHelpers.error(req, res, "Error reading file '" + placementStr + phaseVal + file + "'");
                return;
            }
            data = dataBuff.toString();
            data = resolveLinks(data, placementStr, tutor, phase);
            data = resolveImages(data, placementStr, tutor, phase);
            data = replaceStr(data, "<VP:username>", user.getUserName());
            data = replaceStr(data, "<VP:fullname>", ((VPStudent) user).getFullName());
            data = replaceStr(data, "<VP:boxfile>", "<a href=\"ShowPlacement?display=root&file=links.html&phase=\">Virtual Boxfile</a>");
            data = replaceStr(data, "<VP:portfolio>", "<a href=\"ShowPortfolio?placement=" + placementStr + "&phase=" + phase + "\">Virtual Portfolio</a>");
            data = replaceStr(data, "<VP:tasklist>", "<a href=\"ShowPlacement?display=contents\">&laquo; Back to Task List</a>");
            if (!phase.equals("")) {
                String phaseNo = phase.substring(5);
                if (phaseNo.length() == 1) {
                    data = replaceStr(data, "<VP:taskpage>", "<a href=\"ShowPlacement\">&laquo; Back to Task " + phaseNo + "</a>");
                }
            }
            data = replaceStr(data, "<VP:logout>", "<a href=\"ShowStatic?file=logout.html\">log out</a>");
            data = replaceStr(data, "<VP:company>", "<a href=\"" + placement.getCompanySite() + "\" target=\"_blank\">company website</a>");
            data = replaceStr(data, "<VP:website>", "<a href=\"ShowStatic?file=site.html\">Virtual Placement web site</a>");
            data = replaceStr(data, "<VP:contentlink>", "<a href=\"ShowPlacement?display=contents\">Placement Contents</a>");
            data = replaceStr(data, "<VP:divide>", "<img src=\"../graphics/dotted_line.gif\" alt=\"\" width=\"550\" height=\"1\">");
            data = replaceStr(data, "<VP:submit>", "<a href=\"Submit\">submit</a>");
            data = replaceStr(data, "<VP:form>", "<form method=\"POST\" action=\"Submit\">\n<input type=\"hidden\" name=\"form\" value=\"yes\">");
            data = replaceStr(data, "</VP:form>", "</form>");
            if (!phase.equals("")) {
                VPPhase current = placement.getPhase(phase);
                String end;
                try {
                    end = (DateFormat.getInstance()).format(current.getEnd());
                } catch (Exception e) {
                    end = "<font color=\"red\">date format exception</font>";
                }
                data = replaceStr(data, "<VP:duration>", current.getDuration());
                data = replaceStr(data, "<VP:deadline>", end);
            }
        }
        req.setAttribute("data", data);
        ((VPStudent) user).setCurrentVP(placementStr);
        ((VPStudent) user).setVPTutor(tutor);
        VPHelpers.save(user);
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

    public String replaceStr(String in, String find, String replace) {
        int pos = in.indexOf(find);
        int endpos = 0;
        String temp = "";
        while (pos != -1) {
            temp += in.substring(endpos, pos);
            temp += replace;
            endpos = pos + find.length();
            pos = in.indexOf(find, endpos);
        }
        temp += in.substring(endpos);
        return temp;
    }

    public String resolveLinks(String in, String placement, String tutor, String phase) {
        String temp = in;
        temp = resolveToServer(temp, placement, tutor, phase, "href", "ShowPlacement");
        return resolveToServer(temp, placement, tutor, phase, "HREF", "ShowPlacement");
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
                temp += (server + "?file=");
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
