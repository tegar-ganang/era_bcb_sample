package net.sf.borg.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import net.sf.borg.common.Errmsg;
import net.sf.borg.common.PrefName;
import net.sf.borg.common.Prefs;
import net.sf.borg.common.Resource;
import net.sf.borg.common.XTree;
import net.sf.borg.model.beans.Address;
import net.sf.borg.model.beans.Appointment;
import net.sf.borg.model.beans.KeyedBean;
import net.sf.borg.model.beans.Link;
import net.sf.borg.model.beans.LinkXMLAdapter;
import net.sf.borg.model.beans.Memo;
import net.sf.borg.model.beans.Project;
import net.sf.borg.model.beans.Task;
import net.sf.borg.model.db.BeanDB;
import net.sf.borg.model.db.LinkDB;
import net.sf.borg.model.db.jdbc.LinkJdbcDB;

public class LinkModel extends Model {

    public static class LinkType {

        private String name_;

        public LinkType(String n) {
            name_ = n;
        }

        ;

        public String toString() {
            return name_;
        }
    }

    public static final LinkType FILELINK = new LinkType("file");

    public static final LinkType ATTACHMENT = new LinkType("attachment");

    public static final LinkType URL = new LinkType("url");

    public static final LinkType APPOINTMENT = new LinkType("appointment");

    public static final LinkType MEMO = new LinkType("memo");

    public static final LinkType PROJECT = new LinkType("project");

    public static final LinkType TASK = new LinkType("task");

    public static final LinkType ADDRESS = new LinkType("address");

    private static LinkModel self_ = null;

    private static HashMap<Class<? extends KeyedBean<?>>, String> typemap = new HashMap<Class<? extends KeyedBean<?>>, String>();

    static {
        typemap.put(Appointment.class, "appointment");
        typemap.put(Memo.class, "memo");
        typemap.put(Task.class, "task");
        typemap.put(Address.class, "address");
        typemap.put(Project.class, "project");
    }

    public static LinkModel create() {
        self_ = new LinkModel();
        return (self_);
    }

    public static String attachmentFolder() {
        String dbtype = Prefs.getPref(PrefName.DBTYPE);
        if (dbtype.equals("hsqldb")) {
            String path = Prefs.getPref(PrefName.HSQLDBDIR) + "/attachments";
            File f = new File(path);
            if (!f.exists()) {
                if (!f.mkdir()) {
                    Errmsg.notice(Resource.getPlainResourceString("att_folder_err") + path);
                    return null;
                }
            }
            return path;
        }
        return null;
    }

    public static LinkModel getReference() {
        return (self_);
    }

    private BeanDB<Link> db_;

    public void addLink(KeyedBean<?> owner, String path, LinkType linkType) throws Exception {
        if (owner == null) {
            Errmsg.notice(Resource.getPlainResourceString("att_owner_null"));
            return;
        }
        if (linkType == ATTACHMENT) {
            String atfolder = attachmentFolder();
            if (atfolder == null) throw new Exception("attachments not supported");
            File orig = new File(path);
            String fname = orig.getName();
            String newpath = atfolder + "/" + fname;
            int i = 1;
            while (true) {
                File newfile = new File(newpath);
                if (!newfile.exists()) break;
                fname = Integer.toString(i) + orig.getName();
                newpath = atfolder + "/" + fname;
                i++;
            }
            copyFile(path, newpath);
            path = fname;
        }
        Link at = newLink();
        at.setKey(-1);
        at.setOwnerKey(new Integer(owner.getKey()));
        Object o = typemap.get(owner.getClass());
        if (o == null) throw new Exception("illegal link owner type");
        at.setOwnerType((String) o);
        at.setPath(path);
        at.setLinkType(linkType.toString());
        saveLink(at);
    }

    private static void copyFile(String fromFile, String toFile) throws Exception {
        FileInputStream from = null;
        FileOutputStream to = null;
        try {
            from = new FileInputStream(fromFile);
            to = new FileOutputStream(toFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = from.read(buffer)) != -1) to.write(buffer, 0, bytesRead);
        } finally {
            if (from != null) try {
                from.close();
            } catch (IOException e) {
                ;
            }
            if (to != null) try {
                to.close();
            } catch (IOException e) {
                ;
            }
        }
    }

    public void delete(int key) throws Exception {
        Link l = getLink(key);
        delete(l);
    }

    public void delete(Link l) throws Exception {
        if (l.getLinkType().equals(ATTACHMENT.toString())) {
            File f = new File(attachmentFolder() + "/" + l.getPath());
            f.delete();
        }
        db_.delete(l.getKey());
        refresh();
    }

    public void deleteLinks(int id, Class<? extends KeyedBean<?>> type) throws Exception {
        if (!hasLinks()) return;
        Collection<Link> atts = getLinks(id, type);
        Iterator<Link> it = atts.iterator();
        while (it.hasNext()) {
            Link at = it.next();
            delete(at);
        }
    }

    public void deleteLinks(KeyedBean<?> owner) throws Exception {
        if (!hasLinks()) return;
        Collection<Link> atts = getLinks(owner);
        Iterator<Link> it = atts.iterator();
        while (it.hasNext()) {
            Link at = it.next();
            delete(at);
        }
    }

    public void export(Writer fw) throws Exception {
        if (!hasLinks()) return;
        fw.write("<LINKS>\n");
        LinkXMLAdapter ta = new LinkXMLAdapter();
        for (Link addr : getLinks()) {
            XTree xt = ta.toXml(addr);
            fw.write(xt.toString());
        }
        fw.write("</LINKS>");
    }

    public BeanDB<Link> getDB() {
        return (db_);
    }

    public Link getLink(int key) throws Exception {
        return db_.readObj(key);
    }

    public Collection<Link> getLinks() throws Exception {
        return db_.readAll();
    }

    public Collection<Link> getLinks(int id, Class<? extends KeyedBean<?>> type) throws Exception {
        LinkDB adb = (LinkDB) db_;
        String o = typemap.get(type);
        if (o == null) return new ArrayList<Link>();
        return adb.getLinks(id, o);
    }

    public Collection<Link> getLinks(KeyedBean<?> ownerbean) throws Exception {
        LinkDB adb = (LinkDB) db_;
        if (ownerbean == null) return new ArrayList<Link>();
        Object o = typemap.get(ownerbean.getClass());
        if (o == null) return new ArrayList<Link>();
        return adb.getLinks(ownerbean.getKey(), (String) o);
    }

    public boolean hasLinks() {
        if (db_ != null) return true;
        return false;
    }

    public void importXml(XTree xt) throws Exception {
        if (!hasLinks()) return;
        LinkXMLAdapter aa = new LinkXMLAdapter();
        for (int i = 1; ; i++) {
            XTree ch = xt.child(i);
            if (ch == null) break;
            if (!ch.name().equals("Link")) continue;
            Link addr = aa.fromXml(ch);
            addr.setKey(-1);
            saveLink(addr);
        }
        refresh();
    }

    public void moveLinks(KeyedBean<?> oldOwner, KeyedBean<?> newOwner) throws Exception {
        Collection<Link> atts = getLinks(oldOwner);
        Iterator<Link> it = atts.iterator();
        while (it.hasNext()) {
            Link at = it.next();
            at.setOwnerKey(new Integer(newOwner.getKey()));
            Object o = typemap.get(newOwner.getClass());
            if (o == null) throw new Exception("illegal link owner type");
            at.setOwnerType((String) o);
            db_.updateObj(at);
        }
    }

    public Link newLink() {
        return (db_.newObj());
    }

    @SuppressWarnings("unchecked")
    public void open_db(String url) throws Exception {
        db_ = new LinkJdbcDB(url);
    }

    public void refresh() {
        refreshListeners();
    }

    public void remove() {
        removeListeners();
        try {
            if (db_ != null) db_.close();
        } catch (Exception e) {
            Errmsg.errmsg(e);
            return;
        }
        db_ = null;
    }

    public void saveLink(Link addr) throws Exception {
        int num = addr.getKey();
        if (num == -1) {
            int newkey = db_.nextkey();
            addr.setKey(newkey);
            db_.addObj(addr);
        } else {
            db_.updateObj(addr);
        }
        refresh();
    }
}
