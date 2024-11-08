package org.granite.webcompiler;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import flex2.tools.oem.VirtualLocalFile;
import flex2.tools.oem.VirtualLocalFileSystem;

/**
 * Webcompiler sample servlet.
 * @author Bouiaw
 */
public class WebCompilerServlet extends HttpServlet {

    private Logger logger = Logger.getLogger(WebCompiler.class);

    private static final long serialVersionUID = 1L;

    private ServletConfig servletConfig = null;

    private WebCompiler webCompiler;

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        this.servletConfig = servletConfig;
        webCompiler = WebCompiler.getInstance();
        webCompiler.setTargetPlayer("10.0.0");
        try {
            webCompiler.init(servletConfig.getServletContext().getRealPath("/WEB-INF"));
        } catch (Exception e) {
            logger.error(e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        boolean force = false;
        String sForce = request.getParameter("force");
        if (sForce != null && sForce.equals("true")) force = true;
        boolean isLibrary = false;
        String sIsLibrary = request.getParameter("islibrary");
        if (sIsLibrary != null && sIsLibrary.equals("true")) isLibrary = true;
        boolean virtual = false;
        String name = "";
        String content = "";
        String sVirtual = request.getParameter("virtual");
        if (sVirtual != null && sVirtual.equals("true")) {
            virtual = true;
            if (request.getParameter("name") != null) name = request.getParameter("name");
            if (request.getParameter("content") != null) content = request.getParameter("content");
        }
        String extension = "";
        WebCompilerType type;
        if (isLibrary) {
            extension = WebCompiler.LIBRARY_EXTENSION;
            type = WebCompilerType.library;
        } else {
            extension = WebCompiler.APPLICATION_EXTENSION;
            type = WebCompilerType.application;
        }
        File swfFile = null;
        if (!virtual) {
            File mxmlFile = new File(servletConfig.getServletContext().getRealPath(request.getRequestURI().substring(request.getContextPath().length() + 2)));
            String mxmlPath = mxmlFile.getCanonicalPath();
            swfFile = new File(mxmlPath.substring(0, (mxmlPath.length() - 4)) + extension);
            try {
                swfFile = webCompiler.compileMxmlFile(mxmlFile, swfFile, force, type);
            } catch (WebCompilerException e) {
                response.getWriter().append(e.getMessage());
                return;
            }
        } else {
            VirtualLocalFileSystem vFileSystem = new VirtualLocalFileSystem();
            VirtualLocalFile vFile = vFileSystem.create(servletConfig.getServletContext().getRealPath("/") + name, content, new File(servletConfig.getServletContext().getRealPath("/")), (new Date()).getTime());
            String mxmlPath = vFile.getName();
            swfFile = new File(mxmlPath.substring(0, (mxmlPath.length() - 4)) + extension);
            try {
                swfFile = webCompiler.compileMxmlVirtualFile(vFile, swfFile, force, type);
            } catch (WebCompilerException e) {
                response.getWriter().append(e.getMessage());
                return;
            }
        }
        response.setContentType("application/x-shockwave-flash");
        response.setContentLength((int) swfFile.length());
        response.setBufferSize((int) swfFile.length());
        response.setDateHeader("Expires", 0);
        OutputStream os = null;
        InputStream is = null;
        try {
            is = new BufferedInputStream(new FileInputStream(swfFile));
            os = response.getOutputStream();
            for (int b = is.read(); b != -1; b = is.read()) os.write(b);
        } finally {
            if (is != null) try {
                is.close();
            } finally {
                if (os != null) os.close();
            }
        }
    }

    @Override
    public void destroy() {
        servletConfig = null;
    }
}
