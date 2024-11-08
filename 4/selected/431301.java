package com.memoire.vfs;

import java.io.*;
import java.util.Enumeration;
import java.util.Hashtable;
import com.memoire.fu.FuSort;

/**
 * Ram Disk.
 */
public class VfsRamDisk implements Serializable {

    private static final int UNKNOWN = 0;

    private static final int DIRECTORY = 1;

    private static final int FILE = 2;

    static class Entry implements Serializable {

        private static final long serialVersionUID = 474601829196632525L;

        public transient long time_;

        public transient int type_;

        public transient ByteArrayOutputStream data_;

        public String toString() {
            return "E " + type_ + " " + time_ + " " + data_;
        }

        private void writeObject(ObjectOutputStream _oos) throws IOException {
            _oos.writeLong(time_);
            _oos.writeInt(type_);
            boolean b = (data_ != null);
            _oos.writeBoolean(b);
            if (b) {
                _oos.writeInt(data_.size());
                data_.writeTo(_oos);
            }
        }

        private void readObject(ObjectInputStream _ois) throws IOException, ClassNotFoundException {
            time_ = _ois.readLong();
            type_ = _ois.readInt();
            boolean b = _ois.readBoolean();
            if (b) {
                int size = _ois.readInt();
                data_ = new ByteArrayOutputStream(size);
                for (int i = 0; i < size; i++) data_.write(_ois.read());
            }
        }
    }

    private static VfsRamDisk singleton_;

    public static final VfsRamDisk getInstance() {
        if (singleton_ == null) createInstance();
        return singleton_;
    }

    private static final synchronized void createInstance() {
        if (singleton_ == null) singleton_ = new VfsRamDisk();
    }

    public static final synchronized void setInstance(VfsRamDisk _disk) {
        if (singleton_ != null) throw new Error("Ram Disk already set");
        singleton_ = _disk;
    }

    private Hashtable table_;

    private String[] paths_;

    private VfsRamDisk() {
        table_ = new Hashtable();
        paths_ = null;
    }

    private final Entry getEntry(String _path) {
        return (Entry) table_.get(_path);
    }

    private final synchronized void putEntry(String _path, Entry _entry) {
        paths_ = null;
        table_.put(_path, _entry);
    }

    private final void removeEntry(String _path) {
        paths_ = null;
        table_.remove(_path);
    }

    private final void updateParentTime(String _path) {
        int p = _path.lastIndexOf(File.separatorChar);
        String s;
        if (p > 0) s = _path.substring(0, p); else if (p == 0) s = "/"; else return;
        Entry e = getEntry(s);
        if (e != null) e.time_ = System.currentTimeMillis();
    }

    boolean canRead(String _path) {
        return exists(_path);
    }

    boolean canWrite(String _path) {
        return true;
    }

    boolean delete(String _path) {
        Entry e = getEntry(_path);
        if (e == null) return false;
        removeEntry(_path);
        updateParentTime(_path);
        return true;
    }

    boolean exists(String _path) {
        Entry e = getEntry(_path);
        if (e == null) return false;
        return e.type_ != UNKNOWN;
    }

    ByteArrayOutputStream getOutputStream(String _path) {
        Entry e = getEntry(_path);
        if (e != null) {
            e.time_ = System.currentTimeMillis();
            updateParentTime(_path);
        }
        return (e == null) ? null : e.data_;
    }

    boolean isDirectory(String _path) {
        Entry e = getEntry(_path);
        return (e == null) ? false : (e.type_ == DIRECTORY);
    }

    boolean isFile(String _path) {
        Entry e = getEntry(_path);
        return (e == null) ? false : (e.type_ == FILE);
    }

    long lastModified(String _path) {
        Entry e = getEntry(_path);
        return (e == null) ? 0L : e.time_;
    }

    long length(String _path) {
        Entry e = getEntry(_path);
        return ((e == null) || (e.data_ == null)) ? 0L : e.data_.toByteArray().length;
    }

    void createDirectory(String _path) {
        Entry e = getEntry(_path);
        if (e == null) putEntry(_path, (e = new Entry()));
        e.data_ = null;
        e.time_ = System.currentTimeMillis();
        e.type_ = DIRECTORY;
        updateParentTime(_path);
    }

    void createFile(String _path) {
        Entry e = getEntry(_path);
        if (e == null) putEntry(_path, (e = new Entry()));
        e.data_ = new ByteArrayOutputStream(0);
        e.time_ = System.currentTimeMillis();
        e.type_ = FILE;
        updateParentTime(_path);
    }

    boolean rename(String _path, String _name) {
        Entry e = getEntry(_path);
        if (e == null) return false;
        if (!_path.equals(_name)) {
            putEntry(_name, e);
            removeEntry(_path);
            updateParentTime(_path);
            updateParentTime(_name);
        }
        return true;
    }

    public synchronized String[] getPaths() {
        if (paths_ == null) {
            int l = table_.size();
            Enumeration e = table_.keys();
            int i = 0;
            paths_ = new String[l];
            while (e.hasMoreElements()) {
                paths_[i] = (String) e.nextElement();
                i++;
            }
        }
        return paths_;
    }

    public static void tree(VfsFile _f) {
        String s = _f.getName();
        if ("".equals(s)) s = "/";
        System.out.println(s);
        tree(_f, 0, "");
    }

    private static void tree(VfsFile _f, int _n, String _s) {
        String[] c = _f.list();
        FuSort.sort(c);
        for (int i = 0; i < c.length; i++) {
            if (!".".equals(c[i]) && !"..".equals(c[i])) {
                System.out.println(_s + "|_" + c[i]);
                tree(_f.createChild(c[i]), _n + 1, _s + ((i == c.length - 1) ? "  " : "| "));
            }
        }
    }
}
