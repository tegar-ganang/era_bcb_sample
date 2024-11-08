package org.pfyshnet.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;
import java.lang.ref.WeakReference;
import java.nio.channels.FileChannel;

public class FileKeeper {

    private int WipeRounds;

    private File Root;

    private Hashtable<String, WeakReference<PfyshFile>> Files;

    public FileKeeper(File root, boolean deleteonexit, int wiperounds) {
        if (!root.exists()) {
            root.mkdirs();
        }
        Init(root, deleteonexit);
        WipeRounds = wiperounds;
    }

    public FileKeeper(String rootstr, boolean deleteonexit, int wiperounds) {
        WipeRounds = wiperounds;
        File f = new File(rootstr);
        if (!f.exists()) {
            f.mkdirs();
        }
        Init(f, deleteonexit);
    }

    private void Init(File root, boolean deleteonexit) {
        Root = root;
        Files = new Hashtable<String, WeakReference<PfyshFile>>();
        if (deleteonexit) {
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

                public void run() {
                    Iterator<String> i = Files.keySet().iterator();
                    while (i.hasNext()) {
                        String fn = i.next();
                        File f = new File(fn);
                        f.delete();
                    }
                }
            }));
        }
    }

    public File copyFile(File f, String pre) throws IOException {
        return copyFile(f, getKeeperFile(pre));
    }

    public static File copyFile(File from, File to) throws IOException {
        FileOutputStream fos = new FileOutputStream(to);
        FileInputStream fis = new FileInputStream(from);
        FileChannel foc = fos.getChannel();
        FileChannel fic = fis.getChannel();
        foc.transferFrom(fic, 0, fic.size());
        foc.close();
        fic.close();
        return to;
    }

    public synchronized File getKeeperFile(String pre) throws IOException {
        DeleteLost();
        File f = File.createTempFile(pre, ".dat", Root);
        PfyshFile pf = new PfyshFile(this, f.getPath(), WipeRounds);
        Files.put(new String(f.getPath()), new WeakReference<PfyshFile>(pf));
        return pf;
    }

    public synchronized void delete(PfyshFile file) {
        Files.remove(file.getPath());
    }

    protected synchronized void DeleteLost() {
        Iterator<Entry<String, WeakReference<PfyshFile>>> i = Files.entrySet().iterator();
        while (i.hasNext()) {
            Entry<String, WeakReference<PfyshFile>> e = i.next();
            if (e.getValue().get() == null) {
                File f = new File(e.getKey());
                f.delete();
                i.remove();
            }
        }
    }
}
