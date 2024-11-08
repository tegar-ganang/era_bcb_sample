package edu.psu.citeseerx.web;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import edu.psu.citeseerx.dao2.RepositoryMap;
import edu.psu.citeseerx.utility.FileNamingUtils;
import edu.psu.citeseerx.webutils.RedirectUtils;

/**
 * Servlet providing the chart for a document.
 * @author Isaac Councill
 * @version $Rev$ $Date$
 */
public class CiteChartServlet extends HttpServlet {

    private static final String repID = "chartRepository";

    private RepositoryMap repMap = new RepositoryMap();

    public void init() throws ServletException {
        String chartRepository = getServletConfig().getInitParameter(repID);
        Map<String, String> map = new HashMap<String, String>();
        map.put(repID, chartRepository);
        repMap.setRepositoryMap(map);
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        boolean foundChart = false;
        String doi = request.getParameter("doi");
        if (doi != null && repID != null) {
            String relPath = FileNamingUtils.getDirectoryFromDOI(doi);
            relPath += "citechart.png";
            String path = repMap.buildFilePath(repID, relPath);
            File chartFile = new File(path);
            if (!chartFile.exists()) {
                RedirectUtils.sendRedirect(request, response, "/images/nochart.png");
                return;
            }
            response.reset();
            response.setContentType("image/png");
            FileInputStream in = new FileInputStream(chartFile);
            BufferedInputStream input = new BufferedInputStream(in);
            int contentLength = input.available();
            response.setContentLength(contentLength);
            BufferedOutputStream output = new BufferedOutputStream(response.getOutputStream());
            while (contentLength-- > 0) {
                output.write(input.read());
            }
            output.flush();
        } else {
            RedirectUtils.sendRedirect(request, response, "/images/nochart.png");
        }
    }
}
