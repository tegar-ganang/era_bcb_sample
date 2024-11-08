package com.c2b2.ipoint.presentation.portlets.services;

import com.c2b2.ipoint.model.BlogEntry;
import com.c2b2.ipoint.model.BlogTrackBack;
import com.c2b2.ipoint.model.PersistentModelException;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
  * Id:
  *
  * Copyright 2006 C2B2 Consulting Limited. All rights reserved.
  * Use of this code is subject to license.
  * Please check your license agreement for usage restrictions
  *
  * This class Handles a track back URL. It requires a POST
  * corresponding to the trackback <a href="http://www.sixapart.com/pronet/docs/trackback_spec">specification</a> 
  * and creates a track back entry
  * for the Blog Entry<br>
  * 
  *
  * @author $Author: steve $
  * @version $Revision: 1.1 $
  * $Date: 2006/03/14 20:42:22 $
  *
  */
public class HandleTrackBack implements SupplementaryService {

    public HandleTrackBack() {
    }

    public void execute(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        response.setContentType("text/xml");
        try {
            PrintWriter pw = response.getWriter();
            response.setCharacterEncoding("UTF-8");
            if (request.getMethod() != "POST") {
                writeErrorResponse(pw, "Request Method must be POST");
                return;
            }
            String blogEntryID = request.getParameter("BlogEntryID");
            if (blogEntryID != null) {
                try {
                    BlogEntry be = BlogEntry.findEntry(blogEntryID);
                    if (be.hasTrackBack(request.getRemoteHost())) {
                        writeErrorResponse(pw, "You have already posted a trackback on this entry from IPAddress " + request.getRemoteHost());
                        return;
                    }
                    String url = request.getParameter("url");
                    if (url == null) {
                        writeErrorResponse(pw, "Your post should include a url");
                    } else {
                        String title = request.getParameter("title");
                        String excerpt = request.getParameter("excerpt");
                        String blogName = request.getParameter("blog_name");
                        be.createTrackBack(url, title != null ? title : "No Title", excerpt != null ? excerpt : "", blogName != null ? blogName : "", request.getRemoteHost());
                        writeSuccess(pw);
                    }
                } catch (PersistentModelException e) {
                    writeErrorResponse(pw, "Unable to find the Blog Entry with ID " + blogEntryID);
                }
            } else {
                writeErrorResponse(pw, "The track back URL should include the parameter BlogEntryID");
            }
        } catch (IOException e) {
            throw new ServletException("Unable to get the Writer from the Response");
        }
    }

    public void writeErrorResponse(PrintWriter pw, String error) {
        pw.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        pw.write("<response>");
        pw.write("<error>1</error>");
        pw.write("<message>" + error + "</message>");
        pw.write("</response>");
    }

    public void writeSuccess(PrintWriter pw) {
        pw.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        pw.write("<response>");
        pw.write("<error>0</error>");
        pw.write("</response>");
    }
}
