package aportal.action.cataloguing;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForward;
import java.io.*;
import java.util.ArrayList;

/**
 *
 * @author  Administrator
 */
public class AttachmentsAndURLsAction extends Action {

    /** Creates a new instance of AttachmentsAndURLsAction */
    public AttachmentsAndURLsAction() {
    }

    public org.apache.struts.action.ActionForward execute(org.apache.struts.action.ActionMapping actionMapping, org.apache.struts.action.ActionForm actionForm, javax.servlet.http.HttpServletRequest httpServletRequest, javax.servlet.http.HttpServletResponse httpServletResponse) throws java.lang.Exception {
        String showAttachment = httpServletRequest.getParameter("SHOW");
        String send = "";
        String resource = "";
        boolean streamCalled = false;
        java.util.Properties prop = System.getProperties();
        if (showAttachment == null || showAttachment.equals("")) {
            String catid = httpServletRequest.getParameter("CatId");
            String libid = httpServletRequest.getParameter("LibId");
            if (httpServletRequest.getParameter("resource") != null) {
                resource = httpServletRequest.getParameter("resource");
            }
            aportal.form.cataloguing.AttachmentsAndURLsForm auf = (aportal.form.cataloguing.AttachmentsAndURLsForm) actionForm;
            ejb.bprocess.opac.xcql.OPACUtilities opacutil = null;
            try {
                opacutil = ((ejb.bprocess.opac.xcql.OPACUtilitiesHome) ejb.bprocess.util.HomeFactory.getInstance().getRemoteHome("OPACUtilities")).create();
            } catch (Exception e) {
                e.printStackTrace();
            }
            String openArchiveStatus = null;
            if (httpServletRequest.getSession().getAttribute("Patron_Id") == null) {
                try {
                    openArchiveStatus = opacutil.getOpenArchiveStatus(catid, libid);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (httpServletRequest.getSession().getAttribute("Patron_Id") != null || (openArchiveStatus != null && openArchiveStatus.equals("A"))) {
                try {
                    if (catid == null) {
                        catid = httpServletRequest.getParameter("attachCatRecId");
                    }
                    if (libid == null) {
                        libid = httpServletRequest.getParameter("attachLibId");
                    }
                    auf.setCatId(catid);
                    auf.setLibId(libid);
                    java.util.Hashtable htvals = opacutil.getAttachmentsAndURLs(catid, libid);
                    java.util.Vector vecAtta = (java.util.Vector) htvals.get("ATTACHMENTS");
                    java.util.Vector vecURL = (java.util.Vector) htvals.get("URLS");
                    auf.setAttachments(vecAtta);
                    auf.setUrls(vecURL);
                    prop.load(new FileInputStream(ejb.bprocess.util.NewGenLibRoot.getRoot() + java.io.File.separator + "SystemFiles" + java.io.File.separator + "ENV_VAR.txt"));
                    String xpdf = prop.getProperty("ACCESS_XPDF");
                    String xpdfConfig = "";
                    if (xpdf != null && xpdf.equals("true") && prop.getProperty("XPDF_CONFIG_PATH") != null) {
                        xpdfConfig = prop.getProperty("XPDF_CONFIG_PATH");
                        String ipAddr = prop.getProperty("IPADDRESS");
                        String port = prop.getProperty("PORT");
                        xpdfConfig += "?FName=http://" + ipAddr + ":" + port;
                    }
                    httpServletRequest.setAttribute("XPDF_PATH", xpdfConfig);
                    if (vecAtta.size() == 1 && vecURL.size() == 0) {
                        String basedirec = ejb.bprocess.util.NewGenLibRoot.getAttachmentsPath() + "/CatalogueRecords";
                        String catdirec = basedirec + "/CAT_" + catid + "_" + libid;
                        System.out.println("AttachmentsAndURLsAction  " + catdirec);
                        java.io.File direc = new java.io.File(catdirec);
                        java.io.File filesinit[] = direc.listFiles(new FileFilter() {

                            @Override
                            public boolean accept(File pathname) {
                                if (pathname.isDirectory()) {
                                    return false;
                                } else {
                                    return true;
                                }
                            }
                        });
                        String resource1 = filesinit[0].getName();
                        if (xpdfConfig.equals("")) {
                            ActionForward forward = new ActionForward();
                            forward.setRedirect(true);
                            forward.setPath("AttachmentDownloadServlet?Type=4&Id=" + catid + "&LibId=" + libid + "&FileName=" + resource1);
                            return forward;
                        } else {
                            ActionForward forward = new ActionForward();
                            forward.setRedirect(true);
                            forward.setPath(xpdfConfig + "/newgenlibctxt/AccessContent?CatId=" + catid + "&LibId=" + libid + "&resource=" + resource1);
                            return forward;
                        }
                    } else if (vecAtta.size() == 0 && vecURL.size() == 1) {
                        ActionForward forward = new ActionForward();
                        forward.setRedirect(true);
                        forward.setPath(((ArrayList) vecURL.elementAt(0)).get(0).toString());
                        return forward;
                    } else {
                        send = "selfPage";
                    }
                } catch (Exception exp) {
                    exp.printStackTrace();
                }
            } else {
                httpServletRequest.setAttribute("Target", "AttachmentsAndURLs");
                send = "loginUser";
                httpServletRequest.setAttribute("AttachCatId", catid);
                httpServletRequest.setAttribute("AttachLibId", libid);
            }
        } else {
            send = "selfPage";
            String catid = httpServletRequest.getParameter("CatId");
            String libid = httpServletRequest.getParameter("LibId");
            if (httpServletRequest.getParameter("resource") != null) {
                resource = httpServletRequest.getParameter("resource");
            }
            if (httpServletRequest.getParameter("SHOW") != null && httpServletRequest.getParameter("SHOW").equals("true")) {
                ActionForward forward = new ActionForward();
                forward.setRedirect(true);
                forward.setPath("AttachmentDownloadServlet?Type=4&Id=" + catid + "&LibId=" + libid + "&FileName=" + resource);
                return forward;
            }
        }
        if (streamCalled) {
            return null;
        } else {
            return actionMapping.findForward(send);
        }
    }

    public boolean outputDigitalResource(String catid, String libid, javax.servlet.http.HttpServletResponse httpServletResponse, String resource) throws Exception {
        FileInputStream fin = new FileInputStream(ejb.bprocess.util.NewGenLibRoot.getAttachmentsPath() + java.io.File.separator + "CatalogueRecords" + java.io.File.separator + "CAT_" + catid + "_" + libid + java.io.File.separator + resource);
        java.nio.channels.FileChannel fC = fin.getChannel();
        int sz = (int) fC.size();
        System.out.println("file name " + resource);
        java.util.Properties prop = System.getProperties();
        prop.load(new FileInputStream(ejb.bprocess.util.NewGenLibRoot.getRoot() + java.io.File.separator + "SystemFiles" + java.io.File.separator + "ContentTypes.properties"));
        String typeOfFile = resource.substring(resource.lastIndexOf('.') + 1);
        typeOfFile = typeOfFile.toLowerCase();
        System.out.println("type of file   " + typeOfFile.trim());
        String s = prop.getProperty(typeOfFile.trim());
        System.out.println("content type" + s);
        httpServletResponse.setContentType(s);
        httpServletResponse.setHeader("Content-Disposition", "inline; filename=" + resource);
        byte digCon[] = new byte[sz];
        fin.read(digCon);
        System.out.println("ouput bytes" + digCon.length);
        try {
            OutputStream out = httpServletResponse.getOutputStream();
            out.write(digCon);
            fin.close();
            out.close();
        } catch (Exception e) {
            System.out.println("exception is " + e.getMessage());
        }
        return true;
    }
}
