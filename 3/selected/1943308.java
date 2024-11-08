package org.pustefixframework.http;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.log4j.Logger;
import org.pustefixframework.config.project.ProjectInfo;
import org.pustefixframework.container.spring.http.UriProvidingHttpRequestHandler;
import org.pustefixframework.util.LocaleUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import de.schlund.pfixcore.workflow.Context;
import de.schlund.pfixcore.workflow.ContextImpl;
import de.schlund.pfixcore.workflow.PageRequest;
import de.schlund.pfixcore.workflow.SiteMap;
import de.schlund.pfixxml.PfixServletRequest;
import de.schlund.pfixxml.PfixServletRequestImpl;
import de.schlund.pfixxml.Tenant;
import de.schlund.pfixxml.TenantInfo;
import de.schlund.pfixxml.util.MD5Utils;

public class SiteMapRequestHandler implements UriProvidingHttpRequestHandler, ServletContextAware, ApplicationContextAware {

    private Logger LOG = Logger.getLogger(SiteMapRequestHandler.class);

    private String[] registeredURIs = new String[] { "/sitemap.xml" };

    private SiteMap siteMap;

    private TenantInfo tenantInfo;

    private ProjectInfo projectInfo;

    private ServletContext servletContext;

    private ApplicationContext applicationContext;

    private Map<String, CacheEntry> tenantToCacheEntry = new HashMap<String, CacheEntry>();

    private CacheEntry cacheEntry;

    private Context pustefixContext;

    public synchronized void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        CacheEntry entry = null;
        Tenant tenant = null;
        if (!tenantInfo.getTenants().isEmpty()) {
            tenant = tenantInfo.getMatchingTenant(request);
            if (tenant == null) {
                tenant = tenantInfo.getTenants().get(0);
            }
            entry = tenantToCacheEntry.get(tenant.getName());
        } else {
            entry = cacheEntry;
        }
        if (entry == null) {
            File tempDir = (File) servletContext.getAttribute("javax.servlet.context.tempdir");
            tempDir = new File(tempDir, "pustefix-sitemap-cache");
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }
            entry = new CacheEntry();
            entry.file = new File(tempDir, "sitemap" + (tenant == null ? "" : "-" + tenant.getName()) + ".xml");
            try {
                String host = AbstractPustefixRequestHandler.getServerName(request);
                Document doc = getSearchEngineSitemap(tenant, host);
                Transformer trf = TransformerFactory.newInstance().newTransformer();
                trf.setOutputProperty(OutputKeys.INDENT, "yes");
                FileOutputStream out = new FileOutputStream(entry.file);
                MessageDigest digest;
                try {
                    digest = MessageDigest.getInstance("MD5");
                } catch (NoSuchAlgorithmException x) {
                    throw new RuntimeException("Can't create message digest", x);
                }
                DigestOutputStream digestOutput = new DigestOutputStream(out, digest);
                trf.transform(new DOMSource(doc), new StreamResult(digestOutput));
                digestOutput.close();
                byte[] digestBytes = digest.digest();
                entry.etag = MD5Utils.byteToHex(digestBytes);
            } catch (Exception x) {
                throw new ServletException("Error creating sitemap", x);
            }
        }
        String reqETag = request.getHeader("If-None-Match");
        if (reqETag != null) {
            if (entry.etag.equals(reqETag)) {
                response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                response.flushBuffer();
                return;
            }
        }
        long reqMod = request.getDateHeader("If-Modified-Since");
        if (reqMod != -1) {
            if (entry.file.lastModified() < reqMod + 1000) {
                response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                response.flushBuffer();
                return;
            }
        }
        response.setContentType("application/xml");
        response.setContentLength((int) entry.file.length());
        response.setDateHeader("Last-Modified", entry.file.lastModified());
        response.setHeader("ETag", entry.etag);
        OutputStream out = new BufferedOutputStream(response.getOutputStream());
        InputStream in = new FileInputStream(entry.file);
        int bytes_read;
        byte[] buffer = new byte[8];
        while ((bytes_read = in.read(buffer)) != -1) {
            out.write(buffer, 0, bytes_read);
        }
        out.flush();
        in.close();
        out.close();
    }

    private Set<String> getAccessiblePages() {
        Set<String> accPages = new LinkedHashSet<String>();
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setPathInfo("/home");
        req.setMethod("GET");
        MockHttpSession session = new MockHttpSession(servletContext);
        req.setSession(session);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));
        PfixServletRequest pfxReq = new PfixServletRequestImpl(req, new Properties());
        ((ContextImpl) pustefixContext).prepareForRequest(req);
        ((ContextImpl) pustefixContext).setPfixServletRequest(pfxReq);
        if (siteMap.isProvided()) {
            List<String> pageNames = siteMap.getPageNames(true);
            for (String page : pageNames) {
                try {
                    if (pustefixContext.checkIsAccessible(new PageRequest(page))) {
                        accPages.add(page);
                    }
                } catch (Exception x) {
                    LOG.debug("Error during accessibility check for sitemap", x);
                }
            }
        } else {
            Map<String, PustefixContextXMLRequestHandler> beans = applicationContext.getBeansOfType(PustefixContextXMLRequestHandler.class);
            for (PustefixContextXMLRequestHandler reqHandler : beans.values()) {
                String[] pages = reqHandler.getRegisteredPages();
                for (String page : pages) {
                    if (!page.contains(":")) {
                        try {
                            if (pustefixContext.checkIsAccessible(new PageRequest(page))) {
                                accPages.add(page);
                            }
                        } catch (Exception x) {
                            LOG.debug("Error during accessibility check for sitemap", x);
                        }
                    }
                }
            }
        }
        return accPages;
    }

    public Document getSearchEngineSitemap(Tenant tenant, String host) throws Exception {
        String ns = "http://www.sitemaps.org/schemas/sitemap/0.9";
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Document doc = dbf.newDocumentBuilder().newDocument();
        Element root = doc.createElementNS(ns, "urlset");
        root.setAttribute("xmlns", ns);
        doc.appendChild(root);
        Set<String> accPages = getAccessiblePages();
        if (tenant == null) {
            if (!projectInfo.getSupportedLanguages().isEmpty()) {
                for (String language : projectInfo.getSupportedLanguages()) {
                    boolean defaultLanguage = language.equals(projectInfo.getDefaultLanguage());
                    for (String page : accPages) {
                        addURL(page, root, language, defaultLanguage, host);
                    }
                }
            } else {
                for (String page : accPages) {
                    addURL(page, root, null, true, host);
                }
            }
        } else {
            for (String language : tenant.getSupportedLanguages()) {
                boolean defaultLanguage = language.equals(tenant.getDefaultLanguage());
                for (String page : accPages) {
                    addURL(page, root, language, defaultLanguage, host);
                }
            }
        }
        return doc;
    }

    private void addURL(String page, Element parent, String lang, boolean defaultLang, String host) {
        Element urlElem = parent.getOwnerDocument().createElement("url");
        parent.appendChild(urlElem);
        Element locElem = parent.getOwnerDocument().createElement("loc");
        urlElem.appendChild(locElem);
        String alias;
        String langPrefix = "";
        if (lang == null) {
            alias = page;
        } else {
            alias = siteMap.getAlias(page, lang);
            if (!defaultLang) langPrefix = LocaleUtils.getLanguagePart(lang) + "/";
        }
        locElem.setTextContent("http://" + host + "/" + langPrefix + alias);
        Element cfElem = parent.getOwnerDocument().createElement("changefreq");
        urlElem.appendChild(cfElem);
        cfElem.setTextContent("weekly");
        Element prioElem = parent.getOwnerDocument().createElement("priority");
        urlElem.appendChild(prioElem);
        prioElem.setTextContent("0.5");
        Set<String> pageAlts = siteMap.getPageAlternativeKeys(page);
        if (pageAlts != null) {
            for (String pageAltKey : pageAlts) {
                Element cloned = (Element) urlElem.cloneNode(true);
                alias = siteMap.getAlias(page, lang, pageAltKey);
                ((Element) cloned.getFirstChild()).setTextContent("http://" + host + "/" + langPrefix + alias);
                parent.appendChild(cloned);
            }
        }
    }

    public String[] getRegisteredURIs() {
        return registeredURIs;
    }

    public void setSiteMap(SiteMap siteMap) {
        this.siteMap = siteMap;
    }

    public void setTenantInfo(TenantInfo tenantInfo) {
        this.tenantInfo = tenantInfo;
    }

    public void setProjectInfo(ProjectInfo projectInfo) {
        this.projectInfo = projectInfo;
    }

    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    public void setPustefixContext(Context pustefixContext) {
        this.pustefixContext = pustefixContext;
    }

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    class CacheEntry {

        File file;

        String etag;
    }
}
