package org.marcont2.soa.rest.threads;

import org.marcont2.soa.rest.*;
import java.io.*;
import java.net.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.*;
import javax.servlet.http.*;
import org.marcont2.commons.ThreadModel;
import org.marcont2.exceptions.SemVersionInitializationException;
import org.marcont2.logic.MarcOntLogicBean;
import org.ontoware.rdf2go.model.Model;
import org.ontoware.rdf2go.model.Syntax;

/**
 *
 * @author macdab
 */
public class ThreadQueryHandler {

    private MarcOntLogicBean molb = null;

    private String id;

    private String method;

    private String action;

    private String requestURI;

    private String content;

    private boolean authenticated;

    private int status;

    private String errorMessage;

    private static final Logger logger = Logger.getLogger(ThreadQueryHandler.class.getName());

    public ThreadQueryHandler() {
        molb = MarcOntLogicBean.instance();
    }

    /** 
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/xml");
        response.setHeader("Cache-Control", "no-cache");
        System.out.println("ThreadServlet_MD: " + request.getRequestURI());
        response.setStatus(400);
        response.getWriter().write("<threads>");
        response.getWriter().write("</threads>");
    }

    public String processQuery() {
        if (this.method.equalsIgnoreCase("get")) {
            return performGetQuery();
        } else if (this.method.equalsIgnoreCase("post")) {
            return performPostQuery();
        } else if (this.method.equalsIgnoreCase("put")) {
            return performPutQuery();
        } else if (this.method.equalsIgnoreCase("delete")) {
            return performDeleteQuery();
        }
        return null;
    }

    public String performGetQuery() {
        String result = null;
        if (this.id != null) {
            result = getThread();
            if (result != null) return result;
        }
        this.status = 400;
        this.errorMessage = "No content found!";
        return null;
    }

    public String performPostQuery() {
        this.status = 400;
        this.errorMessage = "No content found!" + this.method;
        return null;
    }

    public String performPutQuery() {
        this.status = 400;
        this.errorMessage = "No content found!" + this.method;
        return null;
    }

    public String performDeleteQuery() {
        this.status = 400;
        this.errorMessage = "No content found!" + this.method;
        return null;
    }

    private String getAllThreads() {
        ThreadModel[] tm = molb.getAllThreads();
        String result = null;
        result = "<threads>";
        for (int i = 0; i < tm.length; i++) {
            result += "<thread>\n";
            result += "<label>" + this.getRequestURI() + tm[i].getLabel() + "</label>\n";
            result += "<uri>" + tm[i].getLabel() + "</uri>\n";
            result += "<description>" + tm[i].getDescription() + "</description>\n";
            result += "</thread>\n";
        }
        result += "</threads>";
        return result;
    }

    private String getThread() {
        String result = null;
        ThreadModel tm = molb.getThread(this.id);
        if (tm != null) {
            Model m = tm.getRDFmetadata();
            return m.serialize(Syntax.RdfXml);
        }
        return result;
    }

    /**
     * @return Returns the method.
     */
    public String getMethod() {
        return method;
    }

    /**
     * @param method
     *            The method to set.
     */
    public void setMethod(String _method) {
        this.method = _method;
    }

    /**
     * @return Returns the Id of the thread.
     */
    public String getId() {
        return id;
    }

    /**
     * @param id
     *  The id to set.
     */
    public void setId(String _id) {
        this.id = _id;
    }

    /**
     * @return Returns the Action for the thread.
     */
    public String getAction() {
        return action;
    }

    /**
     * @param id
     *  The id to set.
     */
    public void setAction(String _action) {
        this.action = _action;
    }

    /**
     * @return Returns the Request URI.
     */
    public String getRequestURI() {
        return requestURI;
    }

    /**
     * @param rURI
     *            The RequestURI to set
     */
    public void setRequestURI(String _rURI) {
        this.requestURI = _rURI;
    }

    /**
     * @return Returns the Request URI.
     */
    public int getStatus() {
        return status;
    }

    public String getAuthor() {
        return this.molb.getThreadAuthor(this.id);
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public MarcOntLogicBean getMolb() {
        return molb;
    }

    public void setMolb(MarcOntLogicBean molb) {
        this.molb = molb;
    }
}
