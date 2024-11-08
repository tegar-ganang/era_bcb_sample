package org.ibex.io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.util.WeakHashMap;
import com.thoughtworks.xstream.XStream;

public class Persistent {

    private static WeakHashMap cache = new WeakHashMap();

    private static final XStream xstream = new XStream();

    public transient File file;

    public transient String path;

    public transient FileLock lock;

    public Persistent(File file) {
        this.file = file;
        this.path = file.getAbsolutePath();
    }

    public Persistent(String path) {
        this(new File(path));
    }

    public static Persistent read(File file) throws Exception {
        Persistent ret = (Persistent) cache.get(file.getAbsolutePath());
        if (ret != null) return ret;
        ret = (Persistent) xstream.fromXML(new BufferedReader(new InputStreamReader(new FileInputStream(file))));
        ret.file = file;
        ret.path = file.getAbsolutePath();
        ret.lock = new RandomAccessFile(file, "rw").getChannel().tryLock();
        if (ret.lock == null) throw new RuntimeException("could not acquire lock on " + ret.path);
        cache.put(ret.path, ret);
        return ret;
    }

    public synchronized void write() throws Exception {
        FileOutputStream fos = new FileOutputStream(path + "-");
        FileDescriptor fd = fos.getFD();
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
        xstream.toXML(this, bw);
        bw.flush();
        fd.sync();
        bw.close();
        new File(path + "-").renameTo(file);
    }
}
