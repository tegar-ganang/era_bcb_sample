package up2p.servlet;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.xmldb.api.base.XMLDBException;
import up2p.core.CommunityNotFoundException;
import up2p.core.WebAdapter;
import up2p.util.FileUtil;

/**
 * Executes an XQuery on the local database and returns the XML result directly.
 * GET requests should give a filename parameter and POST requests read XQuery
 * text directly from the input stream.
 * 
 * <p>
 * Expected parameters:
 * <ul>
 * <li>up2p:communityId - id of the community to search
 * <li>up2p:filename - path and file name on the local file system for
 * containing the XQuery (e.g. test/myQuery.xq). Can be relative to the U-P2P
 * file root or an absolute path. <br>
 * <b>OR </b>
 * <li>up2p:xquery - an XQuery statement to be executed directly
 * </ul>
 * 
 * 
 * @author Neal Arthorne
 * @version 1.0
 */
public class XQuerySearchServlet extends AbstractWebAdapterServlet {

    private static final String MODE = "search";

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doSearch(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doSearch(request, response);
    }

    /**
     * Performs the XQuery search given in a file in the up2p:filename parameter
     * or directly as an XQuery in the up2p:xquery parameter.
     * 
     * @param request request for the search
     * @param response response to the user
     * @throws ServletException if an error occurs in processing
     * @throws IOException if an error occurs in writing to and from the request
     * and response objects
     */
    protected void doSearch(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String communityId = getCurrentCommunityId(request.getSession());
        if ((communityId == null) || communityId.length() == 0) {
            LOG.warn("XQuerySearchServlet " + request.getMethod() + " Request is missing the id of the" + " community.");
            writeError(request, response, "<p><b>XQuerySearchServlet:</b> Request is missing" + " the ID of the community.</p>", MODE);
            return;
        }
        String filename = request.getParameter(HttpParams.UP2P_FILENAME);
        String xQueryStr = request.getParameter(HttpParams.UP2P_XQUERY_SEARCH);
        if (filename == null && xQueryStr == null) {
            LOG.warn("XQuerySearchServlet " + request.getMethod() + " request is missing both a file name or XQuery.");
            writeError(request, response, "<p><b>XQuerySearchServlet:</b> Request is missing" + " either a file name of an XQuery file or an" + " XQuery.</p>", MODE);
            return;
        }
        if (filename != null) {
            File xQueryFile = new File(filename);
            if (!xQueryFile.isAbsolute()) {
                LOG.debug("XQuerySearchServlet Converting relative filename " + xQueryFile.getPath());
                xQueryFile = new File(System.getProperty(WebAdapter.UP2P_HOME) + File.separator + xQueryFile.getPath());
                LOG.debug("XQuerySearchServlet Converted filename to" + " absolute file " + xQueryFile.getAbsolutePath());
            }
            try {
                xQueryStr = FileUtil.readFile(xQueryFile);
            } catch (IOException e) {
                LOG.error("XQuerySearchServlet Error reading XQuery from file " + xQueryFile.getAbsolutePath());
                writeError(request, response, "<p><b>XQuerySearchServlet:</b> Error reading XQuery" + " from file " + xQueryFile.getAbsolutePath() + "</p>", MODE);
                return;
            }
        }
        Map params = new HashMap();
        Enumeration requestParams = request.getParameterNames();
        while (requestParams.hasMoreElements()) {
            String paramName = (String) requestParams.nextElement();
            if (!paramName.startsWith("up2p:")) params.put(paramName, request.getParameter(paramName));
        }
        String result = null;
        try {
            result = adapter.localSearchXQuery(communityId, xQueryStr, params);
        } catch (CommunityNotFoundException e) {
            LOG.warn("XQuerySearchServlet " + request.getMethod() + " Community id of the search (" + communityId + ") is invalid.");
            writeError(request, response, "<p><b>XQuerySearchServlet:</b> Invalid community id: " + communityId + ".</p>", MODE);
            return;
        } catch (XMLDBException e) {
            LOG.warn("XQuerySearchServlet Error in the XQuery search.", e);
            String errorMsg = "<p><b>XQuerySearchServlet:</b> Error in the XQuery.</p><p>";
            if (e.getCause() != null) errorMsg += e.getCause().getMessage(); else errorMsg += e.getMessage();
            errorMsg += "</p><p>XQuery String:<br><pre>" + xQueryStr + "</pre></p>";
            writeError(request, response, errorMsg, MODE);
            return;
        }
        response.setContentType("text/xml");
        response.getWriter().print(result);
    }
}
