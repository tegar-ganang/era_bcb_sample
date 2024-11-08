package org.owasp.oss.httpserver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.Iterator;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.log4j.Logger;
import org.owasp.oss.ca.CertificationAuthority;
import org.owasp.oss.ca.CertificationAuthorityException;
import org.owasp.oss.ca.UserManager;
import org.owasp.oss.ca.model.User;
import org.owasp.oss.util.CertificateUtil;

public class OpenSignResourceServlet extends OSSBaseServlet {

    private static Logger log = Logger.getLogger(OpenSignResourceServlet.class);

    private void buildCertificateBlock() throws CertificationAuthorityException, CertificateEncodingException {
        CertificationAuthority ca = CertificationAuthority.getInstance();
        Certificate cert = ca.getCertificate(_resourceName);
        if (cert == null) _content += "<br /><br /><br />Resource has no certificate"; else _content += "<br /><br /><br />Certificate: <br /><br />" + "<span id=\"certificate\">" + new String(CertificateUtil.certificateToPEM(cert)) + "</span>";
    }

    private void buildSubEntitiesBlock() {
        List<User> subEntitiesList = UserManager.getInstance().getAllSubEntities(_resourceName);
        _leftMenu = "sub-entities:<br />";
        Iterator<User> iter = subEntitiesList.iterator();
        while (iter.hasNext()) {
            User user = iter.next();
            _leftMenu += "<span id=\"menu_link\"><a href=\"/" + user.getResourceName() + "\">" + user.getResourceName() + "</span><br />\r\n";
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        load(req, resp);
        _title = "user profile";
        String property = req.getParameter("property");
        CertificationAuthority ca = CertificationAuthority.getInstance();
        try {
            if (property == null) {
                _content = "Requested resource: " + _resourceName;
                buildCertificateBlock();
                buildSubEntitiesBlock();
                send();
                return;
            } else if (property.equals("cert")) {
                Certificate cert = ca.getCertificate(_resourceName);
                if (cert == null) {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
                resp.setStatus(HttpServletResponse.SC_OK);
                ServletOutputStream respBody = resp.getOutputStream();
                String format = req.getParameter("responseFormat");
                if (format != null) {
                    if (format.equals("PEM")) {
                        resp.setContentType("text/plain");
                        respBody.write(CertificateUtil.certificateToPEM(cert));
                        respBody.flush();
                        return;
                    }
                }
                respBody.write(cert.getEncoded());
                respBody.flush();
            } else resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
        } catch (CertificateEncodingException e) {
            log.error("Could not process GET request", e);
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
        } catch (CertificationAuthorityException e) {
            log.error("Could not process GET request", e);
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    private boolean login(HttpServletRequest req) throws IOException {
        String userName = req.getParameter("user_name");
        String password = req.getParameter("password");
        if (userName != null || password != null) {
            User user = UserManager.getInstance().getUser(userName);
            if (user != null) {
                if (user.getPassword().equals(password)) {
                    _user = user;
                    _userName = userName;
                    log.info("User " + _userName + " has logged in");
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        load(req, resp);
        try {
            _title = "Issue CSR";
            login(req);
            if (!isUserSet()) return;
            String resourceName = _user.getResourceName();
            String csr = req.getParameter("csr");
            if (csr == null) {
                InputStream bodyStream = req.getInputStream();
                if (bodyStream == null) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }
                ByteArrayOutputStream bodyByteStream = new ByteArrayOutputStream();
                ;
                int current = -1;
                while ((current = bodyStream.read()) != -1) bodyByteStream.write(current);
                csr = new String(bodyByteStream.toByteArray());
            }
            CertificationAuthority ca = CertificationAuthority.getInstance();
            Certificate cert = ca.processCsr(csr, _user);
            resp.setStatus(HttpServletResponse.SC_OK);
            ServletOutputStream respBody = resp.getOutputStream();
            String format = req.getParameter("responseFormat");
            if (format != null) {
                if (format.equals("PEM")) {
                    resp.setContentType("text/plain");
                    respBody.write(CertificateUtil.certificateToPEM(cert));
                    respBody.flush();
                    return;
                }
            }
            respBody.write(cert.getEncoded());
            respBody.flush();
        } catch (CertificationAuthorityException e) {
            log.error("Could not process POST request", e);
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
        } catch (CertificateEncodingException e) {
            log.error("Could not process POST request", e);
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }
}
