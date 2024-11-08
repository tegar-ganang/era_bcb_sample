package net.itsite.document.docu.view;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.a.ItSiteUtil;
import net.itsite.document.docu.DocuBean;
import net.itsite.document.docu.DocuUtils;
import net.itsite.document.documentconfig.DocumentConfigMgr;
import net.itsite.document.documentconfig.StorageBean;
import net.itsite.utils.IOUtils;
import net.simpleframework.web.page.PageRequestResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DocViewerServlet extends HttpServlet {

    private static final Log LOGGER = LogFactory.getLog(DocViewerServlet.class);

    public void getDocInfo(HttpServletRequest request, HttpServletResponse response) {
        BufferedReader info = null;
        try {
            String id = request.getParameter("docId");
            if (StringUtils.isBlank(id)) {
                response.setStatus(404);
                return;
            }
            DocuBean docuBean = DocuUtils.applicationModule.getBean(DocuBean.class, id);
            Permissions permissions = Permissions.READ_ONLY;
            if (permissions.equals(Permissions.NONE)) {
                return;
            }
            response.setHeader("Cache-Control", "private");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Connection", "Keep-Alive");
            response.setHeader("Proxy-Connection", "Keep-Alive");
            response.setContentType("application/json");
            response.setCharacterEncoding("utf-8");
            response.flushBuffer();
            final PrintWriter writer = response.getWriter();
            writer.flush();
            int pageCount = docuBean.getFileNum();
            if (pageCount == 0) {
                final StorageBean storageBean = DocumentConfigMgr.getDocuMgr().getStorageMap().get(docuBean.getPath2());
                if (storageBean != null) {
                    pageCount = storageBean.getPageCounter(DocuUtils.getDatabase(docuBean.getUserId()) + docuBean.getFileName());
                }
                if (pageCount > 0) {
                    docuBean.setFileNum(pageCount);
                    DocuUtils.applicationModule.doUpdate(new Object[] { "fileNum" }, docuBean);
                } else {
                    response.setStatus(404);
                    return;
                }
            }
            String docUri = null;
            String url = "/docviewer?";
            int readNum = DocuUtils.allowPages(docuBean);
            final StorageBean storageBean = DocumentConfigMgr.getDocuMgr().getStorageMap().get(docuBean.getPath2());
            boolean split = false;
            if (storageBean != null) {
                split = storageBean.split(DocuUtils.getDatabase(docuBean.getUserId()) + docuBean.getId() + "/");
            }
            if (readNum == 0 || split) {
                docUri = request.getContextPath() + url + "doc=" + id;
            } else docUri = request.getContextPath() + url + "doc={" + id + "-[*,0]," + (readNum >= docuBean.getFileNum() ? docuBean.getFileNum() : readNum + 1) + "}";
            String doc_status = "总共:" + docuBean.getFileNum() + "页  可阅读:" + (readNum == 0 ? docuBean.getFileNum() : readNum) + "页  价格:" + docuBean.getPoint();
            writer.write("{\"uri\":\"" + docUri + "\",\"permissions\":" + permissions.ordinal() + ",\"success\":" + docuBean.getSuccess() + ",\"numPages\":" + (readNum == 0 ? docuBean.getFileNum() : readNum) + ",\"doc_status\":" + "\"" + doc_status + "\"}");
            writer.flush();
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(404);
        } finally {
            if (info != null) {
                IOUtils.closeIO(info);
            }
        }
    }

    public void getDoc(HttpServletRequest request, HttpServletResponse response) {
        response.setHeader("Cache-Control", "private");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Accept-Ranges", "bytes");
        response.setContentType("application/x-shockwave-flash");
        try {
            response.flushBuffer();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        String doc = request.getParameter("doc");
        if (StringUtils.isBlank(doc)) {
            response.setStatus(404);
            return;
        }
        int docId = -1;
        int docPage = -1;
        String[] docInfo = doc.split("-");
        boolean split = false;
        docId = Integer.parseInt(docInfo[0]);
        DocuBean docuBean = DocuUtils.applicationModule.getBean(DocuBean.class, docId);
        try {
            if (docInfo.length == 2) {
                split = true;
                docPage = Integer.parseInt(docInfo[1]);
            }
        } catch (Exception e) {
            response.setStatus(404);
            e.printStackTrace();
            return;
        }
        OutputStream outp = null;
        InputStream in = null;
        int readNum = DocuUtils.allowPages(docuBean);
        try {
            outp = response.getOutputStream();
            final StorageBean storageBean = DocumentConfigMgr.getDocuMgr().getStorageMap().get(docuBean.getPath2());
            if (storageBean != null) {
                PageRequestResponse requestResponse = new PageRequestResponse(request, response);
                if (ItSiteUtil.isManage(requestResponse, DocuUtils.applicationModule) || ItSiteUtil.getLoginUser(requestResponse).getId().getValue().toString().equals(docuBean.getUserId().getValue().toString())) {
                    if (!split) in = storageBean.getInputStream(docuBean, "page.swf"); else if (docPage > readNum && readNum < docuBean.getFileNum()) {
                        in = request.getSession().getServletContext().getResourceAsStream(DocuUtils.deploy + "/page" + "/page.swf");
                    } else in = storageBean.getInputStream(docuBean, "page" + docPage + ".swf");
                } else if (!split) {
                    in = storageBean.getInputStream(docuBean, "page.swf");
                } else if (docPage > readNum && readNum < docuBean.getFileNum()) {
                    in = request.getSession().getServletContext().getResourceAsStream(DocuUtils.deploy + "/page" + "/page.swf");
                } else {
                    in = storageBean.getInputStream(docuBean, "page" + docPage + ".swf");
                }
            }
            response.setContentLength(in.available());
            byte[] b = new byte[1024];
            int i = 0;
            while ((i = in.read(b)) > 0) {
                outp.write(b, 0, i);
                outp.flush();
            }
            outp.flush();
        } catch (Exception ex) {
            response.setStatus(404);
            ex.printStackTrace();
        } finally {
            if (in != null) {
                IOUtils.closeIO(in);
                in = null;
            }
            if (outp != null) {
                IOUtils.closeIO(outp);
                outp = null;
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getRequestURI().equals(req.getContextPath() + "/docviewer/info")) {
            getDocInfo(req, resp);
        } else if (req.getRequestURI().equals(req.getContextPath() + "/docviewer")) {
            getDoc(req, resp);
        }
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }
}
