package com.googlecode.jwsm;

import java.io.*;
import java.util.*;
import java.util.jar.*;
import java.util.logging.Level;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.jcommon.j2ee.*;
import org.jdom.*;
import com.googlecode.jwsm.security.User;

/**
 * Servlet implementation class for Servlet: WebServiceArchive
 *
 * @web.servlet
 *   name="WebServiceArchive"
 *   display-name="WebServiceArchive"
 *
 * @web.servlet-mapping
 *   url-pattern="/WebServiceArchive"
 *
 */
public class WebServiceArchive extends javax.servlet.http.HttpServlet implements javax.servlet.Servlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Thread.currentThread().setName("WebServiceArchive");
        User user = JWSMSecurity.getUser(request, response, true);
        if ("application/octet-stream".equals(request.getContentType())) {
            if (user != null) {
                if (user.hasPrivilege("jwsm.wsar_manage")) {
                    String serviceName = request.getHeader("service");
                    String wsarName = serviceName + ".wsar";
                    String action = request.getHeader("action");
                    File wsar = new File(Uploader.directory, wsarName);
                    FileOutputStream fos = new FileOutputStream(wsar);
                    InputStream input = request.getInputStream();
                    byte[] buf = new byte[512];
                    int len;
                    while ((len = input.read(buf)) != -1) {
                        fos.write(buf, 0, len);
                    }
                    fos.flush();
                    fos.close();
                    if (action != null) {
                        if (action.equals("deploy")) {
                            String error = deployWSAR(wsarName);
                            if (error != null) {
                                response.setStatus(417);
                            }
                        }
                    }
                } else {
                    Log.warn("Unauthorized access atempted to deploy WSAR by: " + user.getName());
                    response.setStatus(401);
                }
            } else {
                Log.warn("Unauthorized access atempted to deploy WSAR by anonymous user");
                response.setStatus(401);
            }
        } else if (request.getMethod().equalsIgnoreCase("POST")) {
            MultipartReader reader = new MultipartReader(request.getInputStream());
            PartInputStream part;
            int count = 0;
            StringBuffer buffer = new StringBuffer();
            while ((part = reader.nextPart()) != null) {
                if ((part.isFile()) && (part.getFilename().trim().length() > 0)) {
                    String wsarName = part.getFilenameShort();
                    if (!wsarName.toLowerCase().endsWith(".wsar")) {
                        wsarName += ".wsar";
                    }
                    File wsar = new File(Uploader.directory, wsarName);
                    FileOutputStream fos = new FileOutputStream(wsar);
                    byte[] buf = new byte[512];
                    int len;
                    while ((len = part.read(buf)) != -1) {
                        fos.write(buf, 0, len);
                    }
                    fos.flush();
                    fos.close();
                    part.close();
                    if (count > 0) buffer.append(", ");
                    buffer.append(wsarName);
                    count++;
                }
            }
            if (count > 0) {
                response.sendRedirect("index.jsp?message=" + buffer.toString() + " uploaded successfully.");
            } else {
                response.sendRedirect("index.jsp?error=WSAR file was not successfully received.");
            }
        } else {
            String action = request.getParameter("action");
            if ((!action.equalsIgnoreCase("download")) && (!user.hasPrivilege("jwsm.wsar_manage"))) {
                response.sendRedirect("index.jsp?error=Insufficient privileges");
            }
            String wsar = request.getParameter("value");
            if (action.equalsIgnoreCase("download")) {
                response.setContentType("application/zip");
                response.addHeader("Content-Disposition", "Attachment; filename=\"" + wsar + "\"");
                FileInputStream fis = new FileInputStream(new File(Uploader.directory, wsar));
                OutputStream os = response.getOutputStream();
                int len;
                byte[] buf = new byte[512];
                while ((len = fis.read(buf)) != -1) {
                    os.write(buf, 0, len);
                }
                os.flush();
                os.close();
                fis.close();
            } else if (action.equalsIgnoreCase("deploy")) {
                String errorMessage = deployWSAR(wsar);
                String type = "deployed";
                if (errorMessage == null) {
                    response.sendRedirect("index.jsp?message=" + wsar + " was successfully " + type + " as service");
                } else {
                    response.sendRedirect("index.jsp?error=" + errorMessage);
                }
            } else if (action.equalsIgnoreCase("delete")) {
                File wsarFile = new File(Uploader.directory, wsar);
                if (wsarFile.delete()) {
                    response.sendRedirect("index.jsp?message=" + wsar + " was successfully deleted.");
                } else {
                    response.sendRedirect("index.jsp?error=" + wsar + " was unable to be deleted.");
                }
            }
        }
    }

    public static final String deployWSAR(String wsarName) {
        String serviceName = wsarName.substring(0, wsarName.length() - "wsar.".length());
        Service service = ServiceManager.getInstance().getService(serviceName);
        if (service != null) {
            String error = service.undeploy();
            if (error != null) {
                return error;
            }
        }
        File serviceDirectory = new File(Uploader.directory, serviceName);
        File wsar = new File(Uploader.directory, wsarName);
        try {
            JarFile jar = new JarFile(wsar);
            Enumeration enumeration = jar.entries();
            serviceDirectory.mkdirs();
            while (enumeration.hasMoreElements()) {
                JarEntry file = (JarEntry) enumeration.nextElement();
                File f = new File(serviceDirectory, file.getName());
                InputStream is = jar.getInputStream(file);
                FileOutputStream fos = new FileOutputStream(f);
                while (is.available() > 0) {
                    fos.write(is.read());
                }
                fos.flush();
                fos.close();
                is.close();
            }
        } catch (Throwable t) {
            Log.get().log(Level.WARNING, "Error trying to deploy WSAR", t);
            return "Error attempting to deploy WSAR: " + t.getMessage();
        }
        Uploader.loadService(serviceDirectory);
        return null;
    }
}
