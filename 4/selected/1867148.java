package edu.psu.citeseerx.web;

import edu.psu.citeseerx.dao2.logic.CSXDAO;
import edu.psu.citeseerx.domain.Document;
import edu.psu.citeseerx.domain.DocumentFileInfo;
import edu.psu.citeseerx.webutils.RedirectUtils;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.ModelAndView;
import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Process a request to download a file, sending the file to the user. If for 
 * some reason the file is not found and Internal error is generated.
 * @author Isaac Councill
 * @version $Rev: 1184 $ $Date: 2009-11-06 14:08:47 -0500 (Fri, 06 Nov 2009) $
 */
public class FileDownloadController implements Controller {

    private CSXDAO csxdao;

    public void setCSXDAO(CSXDAO csxdao) {
        this.csxdao = csxdao;
    }

    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String doi = request.getParameter("doi");
        String rep = request.getParameter("rep");
        String type = request.getParameter("type");
        String urlIndex = request.getParameter("i");
        Map<String, Object> model = new HashMap<String, Object>();
        if (doi == null || type == null) {
            String errorTitle = "Document Not Found";
            model.put("doi", doi);
            model.put("pagetitle", errorTitle);
            return new ModelAndView("baddoi", model);
        }
        BufferedInputStream input = null;
        BufferedOutputStream output = null;
        try {
            Document doc = null;
            try {
                doc = csxdao.getDocumentFromDB(doi);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (doc == null || doc.isPublic() == false) {
                String errorTitle = "Document Not Found";
                model.put("doi", doi);
                model.put("pagetitle", errorTitle);
                return new ModelAndView("baddoi", model);
            }
            if (type.equalsIgnoreCase("url")) {
                DocumentFileInfo finfo = doc.getFileInfo();
                int index = 0;
                try {
                    index = Integer.parseInt(urlIndex);
                } catch (NumberFormatException e) {
                    index = 0;
                }
                String url;
                if (index >= finfo.getUrls().size() || index < 0) {
                    url = finfo.getUrls().get(0);
                } else {
                    url = finfo.getUrls().get(index);
                }
                RedirectUtils.externalRedirect(response, url);
            } else {
                response.reset();
                if (type.equalsIgnoreCase("pdf")) {
                    response.setContentType("application/pdf");
                    response.setHeader("Content-Disposition", "attachment; filename=\"" + doi + ".pdf\"");
                } else if (type.equalsIgnoreCase("ps")) {
                    response.setContentType("application/ps");
                    response.setHeader("Content-Disposition", "attachment; filename=\"" + doi + ".ps\"");
                } else {
                    return null;
                }
                FileInputStream in = csxdao.getFileInputStream(doi, rep, type);
                input = new BufferedInputStream(in);
                int contentLength = input.available();
                response.setContentLength(contentLength);
                output = new BufferedOutputStream(response.getOutputStream());
                while (contentLength-- > 0) {
                    output.write(input.read());
                }
                output.flush();
            }
        } catch (IOException e) {
            throw e;
        } finally {
            try {
                input.close();
            } catch (Exception exc) {
            }
            try {
                output.close();
            } catch (Exception exc) {
            }
        }
        return null;
    }
}
