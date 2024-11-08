package org.hardtokenmgmt.hosting.web;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertStoreException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import org.bouncycastle.cms.CMSException;
import org.ejbca.core.EjbcaException;
import org.ejbca.core.model.authorization.AuthorizationDeniedException;
import org.ejbca.core.model.log.Admin;
import org.hardtokenmgmt.common.Constants;
import org.hardtokenmgmt.common.vo.ResourceDataVO;
import org.hardtokenmgmt.core.settings.BasicGlobalSettings;
import org.hardtokenmgmt.hosting.web.util.JarSigner;
import org.hardtokenmgmt.server.helpers.HTMFManageGlobalPropertiesHelper;
import org.hardtokenmgmt.server.helpers.HTMFManageResourcesHelper;

/**
 * Servlet that generates HTMF application dynamically depending 
 * on the calling organization, it generates and signs uploaded
 * custom code and resources (images, print templates, etc) and
 * global resources. It also generates the JNLP file needed
 * to start the application.
 * 
 * 
 * @author Philip Vendil 17 sep 2009
 *
 * @version $Id$
 */
public class GenHTMFPackageServlet extends HttpServlet {

    public static final String HTML_REPLACEVAR_ORGID = "@ORGID@";

    public static final String INIT_PARAM_JARSIGNERPATH = "jarsigner.keystore";

    public static final String INIT_PARAM_JARSIGNERPASSWD = "jarsigner.passphrase";

    public static final String INIT_PARAM_JARSIGNERALIAS = "jarsigner.alias";

    public static final String INIT_PARAM_APPLICATION = "application";

    private static Logger log = Logger.getLogger(GenHTMFPackageServlet.class);

    private static final long serialVersionUID = 1L;

    private JarSigner jarSigner = null;

    private String application = null;

    private int uRLPrefix;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            String keyStorePath = getInitParameter(config, INIT_PARAM_JARSIGNERPATH);
            char[] keyStorePasswd = config.getInitParameter(INIT_PARAM_JARSIGNERPASSWD).toCharArray();
            String alias = config.getInitParameter(INIT_PARAM_JARSIGNERALIAS);
            application = getInitParameter(INIT_PARAM_APPLICATION);
            uRLPrefix = ("/htmf/" + application + "/").length();
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(new FileInputStream(keyStorePath), keyStorePasswd);
            if (!ks.containsAlias(alias)) {
                log.error("Error alias '" + alias + "' doesn't exist in jar signer keystore: " + keyStorePath);
            }
            jarSigner = new JarSigner(ks, alias, keyStorePasswd);
        } catch (Exception e) {
            log.error("Error configuring GenHTMFPackageServlet, message: " + e.getMessage(), e);
        }
    }

    private String getInitParameter(ServletConfig config, String paramName) {
        String retval = config.getInitParameter(paramName);
        if (retval == null) {
            log.error("Error initializing GenHTMFPackageServlet, init parameter " + paramName + " is not set.");
        }
        return retval;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            Admin admin = getAdmin(req);
            String requestURL = URLDecoder.decode(req.getRequestURI(), "UTF-8");
            String orgId = getOrgId(requestURL);
            String fileName = getRequestedFile(orgId, requestURL);
            HashMap<String, ResourceDataVO> resources = HTMFManageResourcesHelper.getCachedResources(orgId, Constants.APPLICATION_WEBPAGES);
            if (fileName.equals("customcode.jar")) {
                genCustomCodeJar(admin, orgId, req, resp);
            } else {
                if (fileName.equals(application + ".jnlp")) {
                    genJNLP(admin, orgId, resources, fileName, req, resp);
                } else {
                    genOtherFile(admin, orgId, resources, fileName, req, resp);
                }
            }
        } catch (AuthorizationDeniedException e) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
        } catch (Exception e) {
            log.error("Error generating organisation specific content : " + e.getMessage(), e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error: " + e.getClass().getName() + ", " + e.getMessage());
        }
    }

    private void genCustomCodeJar(Admin admin, String orgId, HttpServletRequest req, HttpServletResponse resp) throws IOException, AuthorizationDeniedException, EjbcaException, InvalidKeyException, UnrecoverableKeyException, NoSuchAlgorithmException, SignatureException, CertificateException, KeyStoreException, NoSuchProviderException, CertStoreException, InvalidAlgorithmParameterException, CMSException {
        BasicGlobalSettings bgs = HTMFManageGlobalPropertiesHelper.getBasicGlobalSettingsManager().getBasicGlobalSettings(orgId, application);
        ByteArrayOutputStream gpData = new ByteArrayOutputStream();
        bgs.getProperties().store(gpData, null);
        ByteArrayOutputStream customCodeJarData = new ByteArrayOutputStream();
        JarOutputStream jarOutputStream = new JarOutputStream(customCodeJarData);
        jarOutputStream.putNextEntry(new ZipEntry("global.properties"));
        jarOutputStream.write(gpData.toByteArray());
        HashMap<String, ResourceDataVO> resources = HTMFManageResourcesHelper.getCachedResources(orgId, application);
        for (ResourceDataVO res : resources.values()) {
            if (res.getType().equals(Constants.RESOURCE_TYPE_IMAGE) || res.getType().equals(Constants.RESOURCE_TYPE_LANGUAGEFILE) || res.getType().equals(Constants.RESOURCE_TYPE_PRINTTEMPLATE)) {
                jarOutputStream.putNextEntry(new ZipEntry(res.getName()));
                jarOutputStream.write(res.getData());
                jarOutputStream.closeEntry();
            }
        }
        jarOutputStream.close();
        resp.setContentType("application/java-archive");
        resp.setDateHeader("Last-Modified", System.currentTimeMillis());
        jarSigner.signJarFile(customCodeJarData.toByteArray(), resp.getOutputStream());
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    private void genOtherFile(Admin admin, String orgId, HashMap<String, ResourceDataVO> resources, String fileName, HttpServletRequest req, HttpServletResponse resp) throws IOException, InvalidKeyException, UnrecoverableKeyException, NoSuchAlgorithmException, SignatureException, CertificateException, KeyStoreException, NoSuchProviderException, CertStoreException, InvalidAlgorithmParameterException, CMSException {
        ResourceDataVO resource = resources.get(fileName);
        if (resource == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        } else {
            if (resource.getType().equals(Constants.RESOURCE_TYPE_CUSTOMCODEJAR)) {
                resp.setContentType("application/java-archive");
                jarSigner.signJarFile(resource.getData(), resp.getOutputStream());
                resp.setDateHeader("Last-Modified", System.currentTimeMillis());
                resp.setStatus(HttpServletResponse.SC_OK);
            } else {
                if (resource.getApplication().equals(Constants.APPLICATION_ALL) || resource.getApplication().equals(Constants.APPLICATION_WEBPAGES) || resource.getApplication().equals(application)) {
                    String contentType = getContentType(fileName);
                    byte[] resourceData = resource.getData();
                    if (contentType.equals("text/html")) {
                        String htmlData = new String(resourceData);
                        htmlData = htmlData.replaceAll(HTML_REPLACEVAR_ORGID, orgId);
                        resourceData = htmlData.getBytes();
                    }
                    resp.setContentType(contentType);
                    resp.setContentLength(resourceData.length);
                    resp.getOutputStream().write(resourceData);
                    resp.setDateHeader("Last-Modified", System.currentTimeMillis());
                    resp.setStatus(HttpServletResponse.SC_OK);
                } else {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                }
            }
        }
    }

    private void genJNLP(Admin admin, String orgId, HashMap<String, ResourceDataVO> resources, String fileName, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String sourceFilename = "src_" + fileName;
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(sourceFilename);
        if (is == null) {
            log.error("Error souce JNLP file :" + sourceFilename + " not found.");
        } else {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[2048];
            int read = -1;
            while ((read = is.read(buffer)) != -1) {
                baos.write(buffer, 0, read);
            }
            String jnlpFile = new String(baos.toByteArray());
            String replaceString = "<jar href=\"" + application + "/" + orgId + "/customcode.jar\"/>\n";
            for (ResourceDataVO res : resources.values()) {
                if (res.getType().equals(Constants.RESOURCE_TYPE_CUSTOMCODEJAR)) {
                    replaceString += "<jar href=\"" + application + "/" + orgId + "/" + res.getName() + "\"/>\n";
                }
            }
            jnlpFile = jnlpFile.replace("<!--CUSTOMJARS-->", replaceString);
            jnlpFile = jnlpFile.replace("<!--ORGHREF-->", application + "/" + orgId + "/");
            resp.setContentType("application/x-java-jnlp-file");
            resp.setDateHeader("Last-Modified", System.currentTimeMillis());
            resp.getOutputStream().write(jnlpFile.getBytes());
            resp.setStatus(HttpServletResponse.SC_OK);
        }
    }

    private Admin getAdmin(HttpServletRequest req) throws AuthorizationDeniedException {
        return new Admin(Admin.TYPE_INTERNALUSER, req.getRemoteAddr());
    }

    private String getRequestedFile(String orgId, String decodedRequestURI) throws IOException {
        try {
            int len = uRLPrefix + orgId.length() + 1;
            String retval = decodedRequestURI.substring(len);
            return retval;
        } catch (ArrayIndexOutOfBoundsException e) {
            String message = "Error parsing filename from URL : " + decodedRequestURI;
            log.error(message);
            throw new IOException(message);
        }
    }

    private String getOrgId(String decodedRequestURI) throws IOException {
        try {
            String retval = decodedRequestURI.substring(uRLPrefix);
            retval = retval.substring(0, retval.indexOf("/"));
            return retval;
        } catch (ArrayIndexOutOfBoundsException e) {
            String message = "Error parsing organization from URL : " + decodedRequestURI;
            log.error(message);
            throw new IOException(message);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }

    private String getContentType(String fileName) {
        String lowerCaseFileName = fileName.toLowerCase();
        if (lowerCaseFileName.endsWith(".jnpl")) {
            return "application/x-java-jnlp-file";
        }
        if (lowerCaseFileName.endsWith(".jar")) {
            return "application/java-archive";
        }
        if (lowerCaseFileName.endsWith(".gif")) {
            return "image/gif";
        }
        if (lowerCaseFileName.endsWith(".jpg") || lowerCaseFileName.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lowerCaseFileName.endsWith(".png")) {
            return "image/png";
        }
        if (lowerCaseFileName.endsWith(".css")) {
            return "text/css";
        }
        if (lowerCaseFileName.endsWith(".pdf")) {
            return "application/pdf";
        }
        return "text/html";
    }
}
