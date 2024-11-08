package ch.olsen.routes.cell.library;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import ch.olsen.products.util.logging.Logger;
import ch.olsen.products.util.serialize.XmlSerializer;
import ch.olsen.routes.atom.Atom;
import ch.olsen.routes.atom.AtomFactory;
import ch.olsen.routes.cell.service.gwt.client.LibraryTreeItem;
import ch.olsen.routes.framework.RoutesFramework;
import ch.olsen.servicecontainer.commongwt.client.SessionException;
import ch.olsen.servicecontainer.commongwt.client.UserRoleAndService;
import ch.olsen.servicecontainer.domain.SCEntryPoint;
import ch.olsen.servicecontainer.internalservice.auth.AccessControlElement;
import ch.olsen.servicecontainer.internalservice.auth.AuthInterface;
import ch.olsen.servicecontainer.internalservice.persistence.PersistenceSession;
import ch.olsen.servicecontainer.node.SCNode;
import ch.olsen.servicecontainer.service.EntryPoint;
import ch.olsen.servicecontainer.service.HttpServlet;
import ch.olsen.servicecontainer.service.Logging;
import ch.olsen.servicecontainer.service.PersistenceService;
import ch.olsen.servicecontainer.service.Service;
import ch.olsen.servicecontainer.service.ServicePostActivate;
import ch.olsen.servicecontainer.service.ServicePrePassivate;
import com.db4o.ObjectContainer;
import com.db4o.ObjectSet;
import com.db4o.query.Query;

@Service
public class LibraryServiceImpl implements LibraryService, AccessControlElement {

    LibraryInternalItem root;

    @Logging
    public Logger log;

    @PersistenceService
    public PersistenceSession db;

    @EntryPoint
    public SCEntryPoint sc;

    @ServicePostActivate
    public void init() {
        root = new LibraryInternalItem("", "Root Element", null, null, true);
        try {
            registerItemInternal("Main", "Main", "", null, true, true);
        } catch (LibraryException e1) {
        }
        ObjectContainer db = this.db.openSession();
        try {
            Query q = db.query();
            q.constrain(LibraryPersistence.class);
            ObjectSet<LibraryPersistence> set = q.execute();
            if (set.hasNext()) {
                LibraryPersistence p = set.next();
                db.activate(p, 1000);
                root = p.root;
            }
        } finally {
            this.db.closeSession(db);
        }
        ClassLoader cl = this.getClass().getClassLoader();
        if (cl instanceof URLClassLoader) {
            URLClassLoader urlcl = (URLClassLoader) cl;
            URL urls[] = urlcl.getURLs();
            for (URL url : urls) {
                String fileName = url.getFile();
                try {
                    Iterator<String> iterator;
                    if (new File(fileName).isDirectory()) {
                        iterator = new DirIterator(fileName);
                    } else {
                        iterator = new JarIterator(fileName);
                    }
                    while (iterator.hasNext()) {
                        String subEntry = iterator.next();
                        if (new File(subEntry).isDirectory()) continue;
                        if (subEntry.endsWith("class") && subEntry.indexOf("$") < 0) {
                            String className = "";
                            try {
                                className = subEntry.replace("/", ".");
                                className = className.substring(0, className.length() - 6);
                                final Class clazz = cl.loadClass(className);
                                if (clazz.isAnnotationPresent(LibraryAutoDeploy.class) && Atom.class.isAssignableFrom(clazz)) {
                                    LibraryAutoDeploy annotation = (LibraryAutoDeploy) clazz.getAnnotation(LibraryAutoDeploy.class);
                                    LibraryInternalItem parent = lookup(annotation.path());
                                    if (parent == null) {
                                        String elems[] = annotation.path().split("/");
                                        String path = "";
                                        for (int n = 0; n < elems.length - 1; n++) path += (n > 0 ? "/" : "") + elems[n];
                                        parent = registerItemInternal(elems[elems.length - 1], elems[elems.length - 1], path, null, true, true);
                                    }
                                    registerItemInternal(annotation.name(), annotation.desc(), annotation.path(), new LibraryAtomStdFactory(className), true, true);
                                }
                            } catch (Throwable e) {
                                if (log.isDebug()) log.debug("Could not load class while inspecting jar file: " + className + " : " + e.getMessage());
                            }
                        }
                    }
                } catch (Exception e) {
                    log.info("Error while analizying jar file " + fileName + ": " + e.getMessage());
                }
            }
        }
    }

    public static class LibraryAtomStdFactory implements AtomFactory {

        private static final long serialVersionUID = 1L;

        final String className;

        public LibraryAtomStdFactory(String className) {
            this.className = className;
        }

        public Atom instantiate(RoutesFramework framework) throws Exception {
            final Class clazz = getClass().getClassLoader().loadClass(className);
            return (Atom) clazz.getConstructor(RoutesFramework.class).newInstance(framework);
        }
    }

    public class JarIterator implements Iterator<String> {

        Enumeration<JarEntry> entries;

        public JarIterator(String fileName) throws IOException {
            JarFile jar = new JarFile(fileName);
            entries = jar.entries();
        }

        public boolean hasNext() {
            return entries.hasMoreElements();
        }

        public String next() {
            return entries.nextElement().getName();
        }

        public void remove() {
        }
    }

    public class DirIterator implements Iterator<String> {

        String files[];

        int n = 0;

        public DirIterator(String dirName) {
            files = buildFiles("", dirName).toArray(new String[] {});
        }

        private Collection<String> buildFiles(String prefix, String dirName) {
            String here[] = new File(dirName).list();
            List<String> ret = new LinkedList<String>();
            for (String f : here) {
                ret.add(prefix + f);
                if (new File(dirName + f).isDirectory()) ret.addAll(buildFiles(prefix + f + "/", dirName + f + "/"));
            }
            return ret;
        }

        public boolean hasNext() {
            return n < files.length;
        }

        public String next() {
            return files[n++];
        }

        public void remove() {
        }
    }

    public LibraryTreeItem[] enumerateItems(String session, String path) {
        LibraryInternalItem ret = lookup(path);
        if (ret == null) return null;
        List<LibraryTreeItem> ret2;
        ret2 = compileUserAccessibleItems(session, null, ret.children, "");
        return ret2.toArray(new LibraryTreeItem[0]);
    }

    private ArrayList compileUserAccessibleItems(String session, LibraryTreeItem parent, Map<String, LibraryInternalItem> items, String path) {
        ArrayList ret = new ArrayList();
        for (LibraryInternalItem ti : items.values()) {
            UserRoleAndService rs = null;
            if (ti.internal || (rs = sc.checkAccess(session, ReadRole, createObjId(path, ti.item.name))) != null) {
                LibraryTreeItem copy = new LibraryTreeItem(ti.item.name, ti.item.desc, parent, ti.item.isFolder());
                copy.role = rs;
                ret.add(copy);
                copy.treeChildren = compileUserAccessibleItems(session, copy, ti.children, path + ti.item.name + "/");
            }
        }
        return ret;
    }

    public AtomFactory get(String session, String name) throws SessionException {
        LibraryInternalItem item = lookup(name);
        if (item == null) return null;
        if (item.internal || sc.checkAccess(session, ReadRole, name) != null) {
            AtomFactory factory = item.factory;
            if (factory == null) return null;
            return factory;
        } else throw new SessionException("Not enough rights to access element");
    }

    public LibraryTreeItem registerFolder(String userSession, String name, String desc, String path) throws LibraryException, SessionException {
        LibraryInternalItem ret = registerItemInternal(name, desc, path, null, false, false);
        if (ret == null) return null;
        sc.grantAccess(userSession, AuthInterface.OWNER, createObjId(path, name));
        return ret.item;
    }

    public LibraryTreeItem registerItem(String userSession, String name, String desc, String path, AtomFactory factory, boolean overwrite) throws LibraryException, SessionException {
        LibraryInternalItem ret = registerItemInternal(name, desc, path, factory, overwrite, false);
        if (ret == null) return null;
        sc.grantAccess(userSession, AuthInterface.OWNER, createObjId(path, name));
        return ret.item;
    }

    private LibraryInternalItem registerItemInternal(String name, String desc, String path, AtomFactory factory, boolean overwrite, boolean internal) throws LibraryException {
        LibraryInternalItem parent = lookup(path);
        if (parent == null) throw new LibraryException("Path not found");
        if (!overwrite && parent.children.containsKey(name)) throw new LibraryException("Library Element already registered");
        LibraryInternalItem newe = new LibraryInternalItem(name, desc, parent, factory, internal);
        parent.put(name, newe);
        return newe;
    }

    private void registerItemInternal(LibraryInternalItem item, String path, boolean overwrite) throws LibraryException {
        LibraryInternalItem parent = lookup(path);
        if (parent == null) throw new LibraryException("Path not found");
        if (!overwrite && parent.children.containsKey(item.item.getName())) throw new LibraryException("Library Element already registered");
        parent.put(item.item.getName(), item);
        item.parent = parent;
        item.item.parent = parent.item;
    }

    private LibraryInternalItem lookup(String path) {
        String elems[] = path.split("/");
        Map<String, LibraryInternalItem> cur = root.children;
        LibraryInternalItem ret = root;
        for (int n = 0; n < elems.length; n++) {
            if (elems[n].length() == 0) continue;
            ret = cur.get(elems[n]);
            if (ret != null) {
                cur = ret.children;
            } else return null;
        }
        return ret;
    }

    public void unregisterItem(String userSession, String name) throws SessionException {
        LibraryInternalItem toRemove = lookup(name);
        if (toRemove != null) {
            String location = name;
            if (location.endsWith("/")) location = location.substring(0, location.length() - 1);
            location = location.substring(0, location.indexOf("/"));
            if (checkPermissions(userSession, location, toRemove)) {
                String elems[] = name.split("/");
                name = elems[elems.length - 1];
                if (name.length() == 0) name = elems[elems.length - 2];
                toRemove.parent.remove(name);
            } else throw new SessionException("Not enough rights to delete item");
        }
    }

    /**
	 * 
	 * @param userSession
	 * @param toRemove
	 * @param alreadyChecked   store in here all ownerids already checked so we don't have to
	 * check them again in the auth service
	 * @return
	 */
    private boolean checkPermissions(String userSession, String location, LibraryInternalItem toRemove) {
        try {
            if (sc.checkAccess(userSession, AdminRole, createObjId(location, toRemove.item.name)) == null) return false;
        } catch (Exception e) {
            return false;
        }
        for (LibraryInternalItem i : toRemove.children.values()) if (!checkPermissions(location + i.item.name + "/", userSession, i)) return false;
        return true;
    }

    @ServicePrePassivate
    public void passivate() {
        ObjectContainer db = this.db.openSession();
        try {
            for (Object o : db.get(null)) db.delete(o);
            LibraryPersistence p = new LibraryPersistence();
            p.root = root;
            db.set(p);
        } finally {
            this.db.closeSession(db);
        }
    }

    private static class LibraryInternalItem implements Serializable {

        private static final long serialVersionUID = 1L;

        LibraryInternalItem parent;

        boolean internal;

        LibraryTreeItem item;

        AtomFactory factory;

        Map<String, LibraryInternalItem> children = new LinkedHashMap<String, LibraryInternalItem>();

        public LibraryInternalItem(final String name, final String desc, final LibraryInternalItem parent, final AtomFactory factory, boolean internal) {
            this.item = new LibraryTreeItem(name, desc, parent != null ? parent.item : null, factory == null);
            this.factory = factory;
            this.parent = parent;
            this.internal = internal;
        }

        public void put(String name, LibraryInternalItem child) {
            children.put(name, child);
            item.addChild(child.item);
        }

        public boolean remove(String name) {
            item.removeChild(name);
            return children.remove(name) != null;
        }
    }

    public static class LibraryPersistence implements Serializable {

        private static final long serialVersionUID = 1L;

        LibraryInternalItem root;
    }

    @HttpServlet(url = "download")
    public void handleServletDownloadElement(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            Object id_ = request.getParameter("id");
            if (id_ == null) {
                httpMsg(response, "This servlet needs a id as parameter");
                return;
            }
            String id = id_.toString();
            LibraryInternalItem item = lookup(id);
            if (item == null) {
                httpMsg(response, "Specified library item not found");
                return;
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            if (item.item.isFolder()) {
                LibraryInternalItem tmpInternalParent = item.parent;
                item.parent = null;
                LibraryTreeItem tmpParent = item.item.parent;
                item.item.parent = null;
                XmlSerializer.writeToXML(baos, item);
                item.parent = tmpInternalParent;
                item.item.parent = tmpParent;
            } else {
                LibraryInternalItem copy = new LibraryInternalItem(item.item.getName(), item.item.description(), null, item.factory, false);
                XmlSerializer.writeToXML(baos, copy);
            }
            response.setContentType("application/x-download");
            response.setHeader("Content-Disposition", "attachment; filename=" + id.replace("/", "-") + ".atom.xml");
            OutputStream out = response.getOutputStream();
            out.write(baos.toByteArray());
            out.flush();
        } catch (Exception e) {
            response.getWriter().println("Error in writing to xml: " + e.getMessage());
            log.warn("Could not write item to xml: " + e.getMessage(), e);
        } finally {
            response.getWriter().flush();
        }
    }

    @HttpServlet(url = "upload")
    public void handleServletUploadElement(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            String name = null;
            String location = null;
            LibraryInternalItem toRead = null;
            boolean isMultipart = ServletFileUpload.isMultipartContent(request);
            String session;
            try {
                session = getSession(request);
            } catch (Exception e) {
                httpMsg(response, "User not logged in");
                return;
            }
            if (isMultipart) {
                FileItemFactory factory = new DiskFileItemFactory();
                ServletFileUpload upload = new ServletFileUpload(factory);
                try {
                    List items = upload.parseRequest(request);
                    Iterator iter = items.iterator();
                    while (iter.hasNext()) {
                        FileItem item = (FileItem) iter.next();
                        if (item.isFormField()) {
                            if (item.getFieldName().equals("name")) name = item.getString(); else if (item.getFieldName().equals("location")) location = item.getString();
                        } else if (item.getFieldName().equals("file")) {
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            Streams.copy(item.getInputStream(), baos, true, new byte[4096]);
                            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
                            ObjectInputStream ois = new ObjectInputStream(bais);
                            toRead = (LibraryInternalItem) ois.readObject();
                        }
                    }
                    if (name != null && location != null && toRead != null) {
                        toRead.item.name = name;
                        registerItemInternal(toRead, location, true);
                        String objId = createObjId(location, name);
                        sc.grantAccess(session, AuthInterface.OWNER, objId);
                        httpMsg(response, "Element " + location + "Created");
                    } else {
                        httpMsg(response, "Parameters missing");
                    }
                    return;
                } catch (Exception e) {
                    httpMsg(response, "Exception while reading uploaded file: " + e.getMessage());
                    return;
                }
            } else {
                httpMsg(response, "Parameters missing");
            }
        } finally {
            response.getWriter().flush();
        }
    }

    private String createObjId(String location, String name) {
        if (location.endsWith("/")) return location + name;
        return location + "/" + name;
    }

    private String getSession(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        String session = null;
        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++) {
                if (cookies[i].getName().equals(SCNode.USERSESSIONCOOKIENAME)) {
                    session = cookies[i].getValue();
                    break;
                }
            }
        }
        return session;
    }

    private static void httpMsg(HttpServletResponse response, String message) throws IOException {
        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().println("<p>" + message + "</p>");
    }

    private static final String AdminRole = "Admin";

    private static final String ContribRole = "Contributor";

    private static final String ReadRole = "Read Only";

    static Role rolesRoot;

    static {
        Role read = new Role();
        read.name = ReadRole;
        Role contrib = new Role();
        contrib.name = ContribRole;
        Role admin = new Role();
        admin.name = AdminRole;
        rolesRoot = read;
        read.parents.add(contrib);
        contrib.parents.add(admin);
    }

    public Role getRoleHierarchy() {
        return rolesRoot;
    }

    public boolean isAnonymousAccess() {
        return false;
    }
}
