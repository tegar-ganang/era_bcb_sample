package org.vd.store.impl.reader.google;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.slide.authenticate.AuthenticateException;
import org.jaxen.JaxenException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.NativeJavaMethod;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.vd.extensions.javamail.SharedBufferedInputStream;
import org.vd.extensions.slide.SlideStorageExtension;
import org.vd.extensions.xml.XPathUtils;
import org.vd.extensions.xml.xerces.HTMLParser;
import org.vd.store.FileChangeListener;
import org.vd.store.NotLoggedInException;
import org.vd.store.VirtualFile;
import org.vd.store.impl.reader.WebBasedReader;
import org.vd.store.utils.HttpClientUtils;

public class GoogleStorageReader extends WebBasedReader implements FileChangeListener {

    private static final Log LOGGER = LogFactory.getLog(GoogleStorageReader.class);

    public static final boolean DEBUG = false;

    private SortedSet<VirtualFile> m_files;

    public GoogleStorageReader() {
        m_files = new TreeSet<VirtualFile>();
    }

    @Override
    public void login(String login, String password) throws AuthenticateException {
        try {
            PostMethod method = new PostMethod("https://www.google.com/accounts/ServiceLoginAuth");
            HttpClientUtils.setCommonMethodParameter(method);
            method.setParameter("ltmpl", "cm_blanco");
            method.setParameter("ltmplcache", "2");
            method.setParameter("continue", "http://mail.google.com/mail/");
            method.setParameter("service", "mail");
            method.setParameter("rm", "false");
            method.setParameter("hl", "en");
            method.setParameter("Email", login);
            method.setParameter("Passwd", password);
            method.setParameter("PersistentCookie", "yes");
            method.setParameter("rmShown", "1");
            String location = null;
            try {
                int code = m_client.executeMethod(method);
                if (!(code == 301 || code == 302)) throw new AuthenticateException("Bad login/password");
                location = method.getResponseHeader("Location").getValue();
            } finally {
                method.releaseConnection();
            }
            GetMethod getMethod = new GetMethod(location);
            HttpClientUtils.setCommonMethodParameter(getMethod);
            int code = m_client.executeMethod(getMethod);
            if (200 != code) throw new AuthenticateException("Bad login/password");
            Document doc = HTMLParser.parse(new ByteArrayInputStream(getMethod.getResponseBody()));
            location = XPathUtils.getXPathValue("//meta/@content", doc);
            int idx = location.indexOf("url=");
            if (idx >= 0) location = location.substring(idx + "url=".length());
            while (location.startsWith("'")) location = location.substring(1);
            while (location.endsWith("'")) location = location.substring(0, location.length() - 1);
            getMethod.releaseConnection();
            getMethod = new GetMethod(location);
            HttpClientUtils.setCommonMethodParameter(getMethod);
            code = m_client.executeMethod(getMethod);
            if (200 != code) throw new AuthenticateException("Bad login/password");
        } catch (HttpException e) {
            AuthenticateException ex = new AuthenticateException(e.getMessage());
            ex.initCause(e);
            throw ex;
        } catch (JaxenException e) {
            AuthenticateException ex = new AuthenticateException(e.getMessage());
            ex.initCause(e);
            throw ex;
        } catch (IOException e) {
            AuthenticateException ex = new AuthenticateException(e.getMessage());
            ex.initCause(e);
            throw ex;
        } catch (JDOMException e) {
            AuthenticateException ex = new AuthenticateException(e.getMessage());
            ex.initCause(e);
            throw ex;
        }
    }

    @Override
    protected boolean hasAllNecessaryCookies() {
        return true;
    }

    @Override
    public void buildList(boolean incremental) throws HttpException, IOException, JDOMException, JaxenException, SecurityException, NoSuchMethodException {
        PostMethod method = new PostMethod("http://mail.google.com/mail/");
        method.addParameter("search", "adv");
        method.addParameter("as_from", "");
        method.addParameter("as_to", "");
        method.addParameter("as_subj", "Briefcase:");
        method.addParameter("as_subset", "all");
        method.addParameter("as_has", "");
        method.addParameter("as_hasnot", "");
        method.addParameter("as_attach", "true");
        method.addParameter("as_within", "1d");
        method.addParameter("as_date", "");
        method.addParameter("view", "tl");
        method.addParameter("start", "0");
        HttpClientUtils.setCommonMethodParameter(method);
        int code = m_client.executeMethod(method);
        if (200 != code) throw new IOException("Unable to perform search.");
        Document doc = HTMLParser.parse(method.getResponseBodyAsStream());
        if (DEBUG) LOGGER.trace("Downloaded document\n" + new XMLOutputter(Format.getPrettyFormat()).outputString(doc.getRootElement()));
        List list = XPathUtils.getXPath("/html/script", doc);
        if (null == list || list.isEmpty()) return;
        EcmaError ex = null;
        for (int i = 1; i < list.size(); i++) {
            Element e = (Element) list.get(i);
            if (DEBUG) LOGGER.trace("Processing\n" + new XMLOutputter(Format.getPrettyFormat()).outputString(e));
            try {
                List l = parse(e.getTextTrim());
                if (null == l || l.isEmpty()) continue;
                buildList(l);
            } catch (EcmaError e1) {
                ex = e1;
            }
        }
        if (null != ex) throw ex;
    }

    private List parse(String script) throws SecurityException, NoSuchMethodException {
        if (script.startsWith("<!--")) script = script.substring("<!--".length());
        Context cx = Context.enter();
        try {
            Scriptable scope = cx.initStandardObjects();
            GMailDFunction func = new GMailDFunction();
            NativeJavaObject o = new NativeJavaObject(scope, func, func.getClass());
            scope.setPrototype(o);
            Method method = func.getClass().getDeclaredMethod("run", new Class[] { Object[].class });
            ScriptableObject.putProperty(scope, "D", new NativeJavaMethod(method, "D"));
            cx.evaluateString(scope, script, null, 0, null);
            return func.getThreadId();
        } finally {
            Context.exit();
        }
    }

    private void buildList(List list) {
        if (null == list || list.isEmpty()) return;
        for (Object o : list) {
            VirtualFile f = (VirtualFile) o;
            if (m_files.add(f)) LOGGER.debug("Caching file: " + f);
        }
    }

    public boolean exists(VirtualFile file) throws NotLoggedInException, IOException {
        ensureActionAllowed();
        synchronized (m_files) {
            for (VirtualFile f : m_files) {
                if (f.getPath().contains(file.getPath())) return true;
            }
        }
        return m_files.contains(file);
    }

    public VirtualFile toFile(String path) throws NotLoggedInException, IOException {
        ensureActionAllowed();
        if (path.startsWith(SlideStorageExtension.FILES_PREFIX)) path = path.substring(SlideStorageExtension.FILES_PREFIX.length());
        if (!path.startsWith("/")) path = "/" + path;
        synchronized (m_files) {
            for (VirtualFile f : m_files) {
                if (f.getPath().equals(path)) {
                    return f;
                }
            }
        }
        throw new FileNotFoundException("No such file.");
    }

    public Set<VirtualFile> list(VirtualFile dir) throws NotLoggedInException, IOException {
        ensureActionAllowed();
        Set<VirtualFile> set = new HashSet<VirtualFile>();
        int length = dir.getPath().length();
        synchronized (m_files) {
            for (VirtualFile f : m_files) {
                if (f.getPath().contains(dir.getPath())) {
                    String substring = f.getPath().substring(length);
                    int idx = substring.indexOf('/');
                    if (idx > 0) {
                        String path = substring.substring(0, idx);
                        VirtualFile file = new VirtualFile(path, false, true);
                        set.add(file);
                    } else set.add(f);
                }
            }
        }
        return set;
    }

    public InputStream load(VirtualFile file) throws NotLoggedInException, IOException {
        ensureActionAllowed();
        VirtualFile found = null;
        synchronized (m_files) {
            for (VirtualFile f : m_files) {
                if (f.getPath().equals(file.getPath())) {
                    found = f;
                    break;
                }
            }
        }
        if (null == found) throw new FileNotFoundException();
        Object id = found.getIdentifier();
        PostMethod method = new PostMethod("http://mail.google.com/mail/");
        method.addParameter("view", "om");
        method.addParameter("th", (String) id);
        HttpClientUtils.setCommonMethodParameter(method);
        int code = m_client.executeMethod(method);
        if (200 != code) throw new IOException("Unable to perform search.");
        InputStream in = method.getResponseBodyAsStream();
        try {
            MimeMessage message = new MimeMessage(null, new SharedBufferedInputStream(in));
            Enumeration e = message.getAllHeaders();
            while (e.hasMoreElements()) {
                Header header = (Header) e.nextElement();
                System.out.println(header.getName() + " : " + header.getValue());
            }
            file.setContentType(message.getContentType());
            file.setSize(-1);
            return message.getInputStream();
        } catch (MessagingException e) {
            IOException ioe = new IOException();
            ioe.initCause(e);
            throw ioe;
        }
    }

    private InputStream debug(InputStream inputStream) throws IOException {
        long time = System.currentTimeMillis();
        ByteArrayOutputStream out = new ByteArrayOutputStream(8 * 1024);
        byte[] temp = new byte[2048];
        try {
            int read = inputStream.read(temp);
            while (read > 0) {
                out.write(temp, 0, read);
                read = inputStream.read(temp);
            }
        } finally {
            System.out.println("Took " + (System.currentTimeMillis() - time) + " millis");
        }
        return new ByteArrayInputStream(out.toByteArray());
    }

    public void fileAdded(VirtualFile file) {
        m_fullListBuilt = false;
    }

    public void fileDeleted(VirtualFile file) {
        m_fullListBuilt = false;
    }
}
