package com.ecyrd.jspwiki.project;

import java.io.IOException;
import java.util.List;
import java.util.StringTokenizer;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import mase.system.MaseSystem;
import mase.util.WikipageService;
import mase.wikipage.Project;
import mase.wikipage.WikiPage;
import org.apache.log4j.Logger;
import com.ecyrd.jspwiki.filters.RedirectException;

/**
 * @author herbiga
 *
 */
public class EditProjectServlet extends HttpServlet {

    private static final long serialVersionUID = 7532592226662774107L;

    Logger log = Logger.getLogger(this.getClass().getName());

    private static MaseSystem ms;

    static {
        try {
            Context ctx = new InitialContext();
            ms = (MaseSystem) ctx.lookup(WikipageService.jndiLookUp);
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        try {
            HttpSession session = req.getSession();
            session.removeAttribute("msg");
            String author = (String) session.getAttribute("logon.isDone");
            if (author == null) {
                throw new RedirectException("Sorry. Please login again.", "Failure.jsp?page=writeError");
            }
            List<WikiPage> notPos = null;
            Project prj = ms.findProject(Long.parseLong(req.getParameter("projid")));
            prj.setManager(ms.findTeamMember(Long.parseLong(req.getParameter("managerID"))));
            String error = "";
            if (!(error = WikipageService.checkInput(req.getParameter("projectname"))).equalsIgnoreCase("ok")) {
                throw new RedirectException(error + " in your project name", req.getParameter("page"));
            }
            prj.setName(req.getParameter("projectname").trim());
            if (ms.updateProject(prj) == false) {
                throw new RedirectException("Database is not reachable!", req.getParameter("page"));
            } else {
                if (req.getParameter("allwikis") != null) {
                    notPos = ms.changeWikiPagePermission(Integer.parseInt(req.getParameter("readPermission")), Integer.parseInt(req.getParameter("writePermission")), prj.getFullQualifiedName(), (String) req.getSession().getAttribute("logon.isDone"), true);
                } else {
                    notPos = ms.changeWikiPagePermission(Integer.parseInt(req.getParameter("readPermission")), Integer.parseInt(req.getParameter("writePermission")), prj.getFullQualifiedName(), (String) req.getSession().getAttribute("logon.isDone"), false);
                }
                if (notPos == null) {
                    throw new RedirectException("Database error.", req.getParameter("page"));
                } else if (!notPos.isEmpty()) {
                    String outputPage = "";
                    String resPerson = "";
                    WikiPage wiki = null;
                    StringTokenizer st = null;
                    List<Project> prjs = null;
                    for (int i = 0; i < notPos.size(); i++) {
                        wiki = notPos.get(i);
                        outputPage += "|<a href=\"Wiki.jsp?page=" + wiki.getFullQualifiedName() + "\">" + wiki.getFullQualifiedName() + "</a>";
                        if (wiki.getWritePermission() == WikipageService.PERM_OWNER) {
                            resPerson += "|<a href=\"EditTeamMember.jsp?page=viewUser&id=" + wiki.getAuthor().getId() + "\">" + wiki.getAuthor().getFirstName() + " " + wiki.getAuthor().getLastName() + "</a>";
                        } else if (wiki.getWritePermission() == WikipageService.PERM_MANAGER || wiki.getWritePermission() == WikipageService.PERM_PROJECT) {
                            st = new StringTokenizer(wiki.getFullQualifiedName(), ".");
                            if (st.hasMoreTokens()) {
                                String prjName = st.nextToken();
                                prjs = ms.findAllVersionsOfProjectByName(prjName);
                            }
                            for (int j = 0; j < prjs.size(); j++) {
                                if (prjs.get(j).getVersion() == 1) {
                                    if (wiki.getWritePermission() == WikipageService.PERM_MANAGER) {
                                        resPerson += "|<a href=\"EditTeamMember.jsp?page=viewUser&id=" + prjs.get(j).getManager().getId() + "\">" + prjs.get(j).getManager().getFirstName() + " " + prjs.get(j).getManager().getLastName() + "</a>";
                                    } else {
                                        resPerson += "|<a href=\"Project.jsp?page=viewTeamMember&lastpage=allProjectsTable&projid=" + prjs.get(j).getId() + "\">TeamMember</a>";
                                    }
                                }
                            }
                        } else if (wiki.getWritePermission() == WikipageService.PERM_LOGGEDIN) {
                            resPerson += "|You have to log in.";
                        } else {
                            resPerson += "|Error";
                        }
                    }
                    req.getSession().setAttribute("outputPage", outputPage);
                    req.getSession().setAttribute("resPerson", resPerson);
                    res.sendRedirect("Failure.jsp?page=accessList");
                } else {
                    res.sendRedirect(req.getParameter("nextpage"));
                }
            }
        } catch (RedirectException e) {
            req.getSession().setAttribute("msg", e.getMessage());
            res.sendRedirect(e.getRedirect());
        }
    }

    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        doPost(req, res);
    }
}
