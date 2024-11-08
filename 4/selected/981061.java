package com.memoire.fu;

import java.io.*;
import java.util.*;

/**
 * Ram Disk.
 */
public class FuRamDisk implements Serializable {

    private static final int UNKNOWN = 0;

    private static final int DIRECTORY = 1;

    private static final int FILE = 2;

    private static class Entry implements Serializable {

        static final long serialVersionUID = 474601829196632525L;

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

    private static FuRamDisk singleton_ = null;

    public static final FuRamDisk getInstance() {
        if (singleton_ == null) createInstance();
        return singleton_;
    }

    private static final synchronized void createInstance() {
        if (singleton_ == null) singleton_ = new FuRamDisk();
    }

    private static final synchronized void setInstance(FuRamDisk _disk) {
        if (singleton_ != null) throw new Error("Ram Disk already set");
        singleton_ = _disk;
    }

    private static final synchronized FuRamDisk loadDisk(String _path) throws IOException {
        try {
            ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(_path)));
            Object r = ois.readObject();
            ois.close();
            return (FuRamDisk) r;
        } catch (ClassNotFoundException ex) {
            throw new IOException("bad format");
        }
    }

    private static final synchronized void saveDisk(String _path, FuRamDisk _instance) throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(_path)));
        oos.writeObject(_instance);
        oos.flush();
        oos.close();
    }

    private Hashtable table_;

    private String[] paths_;

    private FuRamDisk() {
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
        return true;
    }

    boolean exists(String _path) {
        Entry e = getEntry(_path);
        if (e == null) return false;
        return e.type_ != UNKNOWN;
    }

    ByteArrayOutputStream getOutputStream(String _path) {
        Entry e = getEntry(_path);
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
        return (e == null) ? 0l : e.time_;
    }

    long length(String _path) {
        Entry e = getEntry(_path);
        return ((e == null) || (e.data_ == null)) ? 0l : e.data_.toByteArray().length;
    }

    void createDirectory(String _path) {
        Entry e = getEntry(_path);
        if (e == null) putEntry(_path, (e = new Entry()));
        e.data_ = null;
        e.time_ = System.currentTimeMillis();
        e.type_ = DIRECTORY;
    }

    void createFile(String _path) {
        Entry e = getEntry(_path);
        if (e == null) putEntry(_path, (e = new Entry()));
        e.data_ = new ByteArrayOutputStream(0);
        e.time_ = System.currentTimeMillis();
        e.type_ = FILE;
        System.err.println(getEntry(_path));
    }

    boolean rename(String _path, String _name) {
        Entry e = getEntry(_path);
        if (e == null) return false;
        if (!_path.equals(_name)) {
            putEntry(_name, e);
            removeEntry(_path);
        }
        return true;
    }

    synchronized String[] getPaths() {
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

    public static void list(FuFile _f) {
        String s = _f.getName();
        if ("".equals(s)) s = "/";
        String t = "            ";
        s += "                    ";
        s = s.substring(0, 20) + " ";
        if (_f.isDirectory()) {
            s += 'd';
            t += _f.list().length;
        } else if (_f.isFile()) {
            s += 'f';
            t += _f.length();
        } else s += 'u';
        t = t.substring(t.length() - 12);
        s += " " + t;
        t = FuLib.date(_f.lastModified(), java.text.DateFormat.SHORT) + " " + FuLib.time(_f.lastModified(), java.text.DateFormat.SHORT) + "                    ";
        t = t.substring(0, 20);
        s += " " + t;
        System.out.println(s);
    }

    public static void tree(FuFile _f) {
        String s = _f.getName();
        if ("".equals(s)) s = "/";
        System.out.println(s);
        tree(_f, 0, "");
    }

    private static void tree(FuFile _f, int _n, String _s) {
        String[] c = _f.list();
        FuSort.sort(c);
        for (int i = 0; i < c.length; i++) {
            if (!".".equals(c[i]) && !"..".equals(c[i])) {
                System.err.println(_s + "|_" + c[i]);
                tree(_f.createChild(c[i]), _n + 1, _s + ((i == c.length - 1) ? "  " : "| "));
            }
        }
    }

    public static void syntax() {
        String s = "Syntax: furamdisk (i|e|l|t) path-to-disk [file [...]]\n" + "  c : create a disk\n" + "  i : inject files in the disk\n" + "  e : erase  files in the disk\n" + "  l : list the disk content\n" + "  t : disk tree\n";
        System.err.println(s);
        System.exit(-1);
    }

    public static void main(String[] _args) {
        if ((_args.length < 2) || (_args[0].length() != 1)) syntax();
        char command = _args[0].charAt(0);
        String disk = _args[1];
        boolean dirty = false;
        if (command == 'c') {
            if (new File(disk).exists()) {
                System.err.println("Error: the disk already exists");
                System.exit(-2);
            }
            dirty = true;
        } else {
            if (!new File(disk).exists()) {
                System.err.println("Error: can not find the disk");
                System.exit(-3);
            }
            try {
                setInstance(loadDisk(disk));
            } catch (IOException ex) {
                System.err.println("Error: " + ex.getMessage());
                System.exit(-4);
            }
        }
        if (_args.length == 2) {
            String[] p = getInstance().getPaths();
            FuSort.sort(p);
            if (p.length > 0) {
                if (command == 't') tree(new FuFileRam(p[0])); else if (command == 'l') {
                    for (int i = 0; i < p.length; i++) list(new FuFileRam(p[i]));
                }
            } else if (command != 'c') System.out.println("empty disk");
        } else {
            for (int i = 2; i < _args.length; i++) {
                if ((command == 'c') || (command == 'i')) {
                    try {
                        FuFile g = new FuFileFile(_args[i]);
                        FuFile f = new FuFileRam(g.getAbsolutePath());
                        if (g.isDirectory()) {
                            System.err.println("create " + g);
                            f.mkdirs();
                        } else {
                            f.getParentFuFile().mkdirs();
                        }
                        if (g.isFile()) {
                            InputStream in = g.getInputStream();
                            OutputStream out = f.getOutputStream();
                            int c;
                            int n = 0;
                            while ((c = in.read()) != -1) {
                                n++;
                                out.write(c);
                            }
                            in.close();
                            out.close();
                            System.err.println("copy " + g + " [" + n + " octets]");
                        }
                    } catch (IOException ex) {
                        System.err.println("Error: can not create/copy " + _args[i]);
                        System.exit(-6);
                    }
                    dirty = true;
                } else {
                    FuFileRam f = new FuFileRam(_args[i]);
                    if (f.exists()) {
                        if (command == 'e') {
                            f.delete();
                            dirty = true;
                        } else if (command == 't') tree(f); else if (command == 'l') list(f);
                    } else {
                        System.err.println("Error: doesn't exist " + _args[i]);
                        System.exit(-7);
                    }
                }
            }
        }
        if (dirty) {
            try {
                saveDisk(disk, getInstance());
            } catch (IOException ex) {
                System.err.println("Error: " + ex.getMessage());
                System.exit(-5);
            }
        }
        System.exit(0);
    }
}
