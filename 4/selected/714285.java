package com.jot.system;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Map;
import java.util.TreeMap;
import mojasi.MojasiException;
import mojasi.MojasiToken;
import mojasi.MojasiWriter;
import mojasi.configuration.ConfigManager;
import mojasi.utils.Base64Stuff;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import com.jot.system.client.DummyClientObjectCache;
import com.jot.system.entities.ImMessage;
import com.jot.system.nio.readers.DefaultSocketReader;
import com.jot.system.pjson.Guid;
import com.jot.system.utils.EndTimer;
import com.jot.system.utils.JotTime;

public class BackupFileManager {

    public static Logger logger = Logger.getLogger(BackupFileManager.class);

    private JotContext g;

    private Map<Long, FileStruct> files = new TreeMap<Long, FileStruct>();

    private FileStruct current;

    private int currentPos = 0;

    int fileMaxSize = 8 * 1024 * 1024;

    int fileMaxCount = 20;

    private String dir;

    private String keysdir;

    private boolean recoverMode = false;

    private int recoverCount = 0;

    public BackupFileManager(JotContext ggg) {
        g = ggg;
        fileMaxSize = (int) ConfigManager.mgr.getLong("BackupFileManager.fileMaxSize", fileMaxSize);
        fileMaxCount = (int) ConfigManager.mgr.getLong("BackupFileManager.fileMaxCount", fileMaxCount);
        init();
    }

    class FileStruct {

        long name;

        RandomAccessFile file;

        MappedByteBuffer buf;

        File keysFile;

        FileOutputStream keys;

        FileStruct(long name, boolean isCurrent) throws IOException {
            this.name = name;
            if (isCurrent) {
                file = new RandomAccessFile(dir + name, "rw");
                file.setLength(fileMaxSize);
            } else file = new RandomAccessFile(dir + name, "r");
            FileChannel fc = file.getChannel();
            long fileSize = fc.size();
            MappedByteBuffer buffer = null;
            if (isCurrent) {
                buffer = fc.map(FileChannel.MapMode.READ_WRITE, 0, fileSize);
                buffer.put((byte) 0);
            } else buffer = fc.map(FileChannel.MapMode.READ_ONLY, 0, fileSize);
            buf = buffer;
            keysFile = new File(keysdir + name, "");
            if (isCurrent) {
                if (!keysFile.exists()) keysFile.createNewFile();
                keys = new FileOutputStream(keysFile);
            }
            logger.trace("mapped file " + dir + name + " size=" + fileSize);
            assert buf != null;
        }

        void delete() throws IOException {
            logger.warn("deleting BackFileMgr.FileStruct " + dir + name);
            file.close();
            File tmp = new File(dir + name);
            tmp.delete();
            tmp = new File(keysdir + name);
            tmp.delete();
            file = null;
            buf = null;
        }

        void close() throws IOException {
            file.close();
            if (keys != null) keys.close();
            buf = null;
            file = null;
        }
    }

    static class myFilter implements FilenameFilter {

        public boolean accept(File dir, String name) {
            try {
                Long.parseLong(name);
                return true;
            } catch (NumberFormatException e) {
            }
            return false;
        }
    }

    synchronized void init() {
        files = new TreeMap<Long, FileStruct>();
        dir = "backfiles/" + g.peer.getName().toB64String().replace('/', '_') + "/";
        keysdir = "backfiles/keys_" + g.peer.getName().toB64String().replace('/', '_') + "/";
        File path = new File(dir);
        path.mkdirs();
        path = new File(keysdir);
        path.mkdirs();
        String[] names = path.list(new myFilter());
        for (String name : names) {
            long tmpl = Long.parseLong(name);
            FileStruct fs = null;
            try {
                fs = new FileStruct(tmpl, false);
            } catch (FileNotFoundException e) {
                assert false : "the file has to be here because we just got it from the directory!";
            } catch (IOException e) {
                logger.error("err ", e);
                assert false : "the file has to be here because we just got it from the directory!";
            }
            files.put(tmpl, fs);
        }
    }

    private void makeNewCurrent() {
        if (current != null) {
            try {
                if (current.keys != null) current.keys.close();
                current.keys = null;
            } catch (IOException e) {
                logger.error("can't close current ", e);
            }
        }
        current = null;
        long latest = JotTime.get();
        File fff = null;
        try {
            fff = new File(dir + latest);
            if (!fff.exists()) {
                fff.createNewFile();
            } else assert false : "always make new file";
            current = new FileStruct(latest, true);
            currentPos = 0;
            files.put(latest, current);
        } catch (FileNotFoundException e) {
            assert false : "didn't we just create the file?";
        } catch (IOException e) {
            logger.fatal("very bad problem here" + latest, e);
        }
        logger.trace("made new current");
    }

    @SuppressWarnings("unused")
    private void close() {
        for (Long file : files.keySet()) {
            FileStruct in = files.get(file);
            try {
                in.close();
            } catch (IOException e) {
                logger.error("close error " + file, e);
            }
        }
        if (current.keys != null) try {
            current.keys.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        current.keys = null;
        current = null;
        currentPos = 0;
        files = new TreeMap<Long, FileStruct>();
    }

    private void deleteOldest() {
        assert files.size() > 0;
        long oldest = files.keySet().iterator().next();
        FileStruct oldFile = files.get(oldest);
        files.remove(oldest);
        logger.trace("promoting all objects out of oldest file " + oldest);
        int readCount = 0;
        int readByteCount = 0;
        for (int i = 0; i < g.objectServers.objCacheBuckets.bucketCount; i++) {
            ObjectCacheBucket ocb = g.objectServers.objCacheBuckets.get(i);
            if (ocb == null) continue;
            for (ObjectServerCachedObject oc : ocb.values()) {
                if (oc.file == oldest && oc.entity == null) {
                    read(oc, oldFile.buf);
                    readCount++;
                    readByteCount += oc.length();
                    g.objectServers.cachedByteCount += oc.length();
                    write(oc);
                    g.objectServers.cachedByteCount -= oc.length();
                    oc.entity = null;
                }
            }
        }
        logger.trace("promoted " + readByteCount + " bytes in " + readCount + " objects");
        try {
            oldFile.delete();
        } catch (IOException e) {
            logger.error(e);
        }
    }

    private boolean read(ObjectServerCachedObject osco, MappedByteBuffer buf) {
        int oldPos = buf.position();
        buf.position(osco.offset);
        int length = DefaultSocketReader.snoopMojasiLength(buf);
        int c = buf.get();
        if (c == '#') {
            {
                c = buf.get();
                int tmp;
                int valLength = 0;
                while ((tmp = Base64Stuff.char2nibble[c]) >= 0) {
                    valLength = (valLength << 6) + tmp;
                    c = buf.get();
                }
                if (length != valLength) {
                    logger.error("snoopMojasiLength is busted");
                    return false;
                }
            }
            byte[] bytes = new byte[length];
            buf.position(osco.offset);
            buf.get(bytes);
            JotMojasiParser parser = new JotMojasiParser(g, null, new DummyClientObjectCache());
            Object obj = null;
            try {
                obj = parser.parseObject(new MojasiToken(bytes, 0));
            } catch (MojasiException e) {
                String str = new String(bytes);
                if (str.length() > 256) str = str.substring(0, 256);
                logger.error("object failed to load ", e);
                return false;
            }
            if (obj instanceof CachedObject) {
                CachedObject co = (CachedObject) obj;
                osco.version = co.version;
                osco.entity = co.entity;
                osco.lastSerialization = null;
                if (osco.entity == null) {
                    logger.error("null entity for " + osco.guid);
                    return false;
                } else {
                    if (!osco.entity.guid.equals(osco.guid)) {
                        logger.error("serious guid mismatch for " + osco.guid + " vs " + osco.entity.guid);
                        return false;
                    }
                }
            } else {
                logger.error("expected CachedObject got " + obj);
                return false;
            }
        } else {
            logger.error("Failed to read object - not mojasi");
            return false;
        }
        buf.position(oldPos);
        return true;
    }

    public boolean read(ObjectServerCachedObject osco, boolean failAllowed) {
        FileStruct file = files.get(osco.file);
        boolean ok = false;
        if (file != null) {
            ok = read(osco, file.buf);
        } else if (!failAllowed) {
            logger.error("null file for " + osco.file + " guid " + osco.guid);
        }
        return ok;
    }

    public synchronized void write(ObjectServerCachedObject osco) {
        if (recoverMode) {
            return;
        }
        if (current == null) makeNewCurrent();
        try {
            CachedObject writeMe = new CachedObject(osco.guid, osco.version, osco.entity);
            byte[] bytes = MojasiWriter.object2bytes(writeMe);
            if (currentPos + bytes.length + 1 > fileMaxSize) {
                makeNewCurrent();
            }
            if (files.size() > fileMaxCount) {
                deleteOldest();
            }
            if (currentPos + bytes.length + 1 > fileMaxSize) {
                makeNewCurrent();
            }
            try {
                if (currentPos + bytes.length > current.file.length()) current.file.setLength(currentPos + bytes.length + 1);
            } catch (IOException e) {
                logger.error("write err", e);
            }
            osco.file = current.name;
            osco.offset = currentPos;
            int oldPos = current.buf.position();
            current.buf.position(currentPos);
            current.buf.put(bytes);
            current.buf.put((byte) 0);
            currentPos += bytes.length;
            current.buf.position(oldPos);
            MojasiWriter w = new MojasiWriter();
            w.mask = 0x8000;
            w.writeGenericObject(osco);
            byte[] got = w.getBytes();
            current.keys.write(got);
        } catch (IOException e) {
            logger.error("write failed ", e);
        } catch (MojasiException e) {
            logger.error("write failed ", e);
        }
    }

    public synchronized void recover() {
        EndTimer timer = new EndTimer();
        if (current == null) ;
        recoverMode = true;
        byte[] buff = new byte[1000];
        for (long lll : files.keySet()) {
            FileStruct file = files.get(lll);
            try {
                FileInputStream in = new FileInputStream(file.keysFile);
                long flen = file.keysFile.length();
                for (int pos = 0; pos < flen; ) {
                    int i = 0;
                    int c = in.read();
                    buff[i++] = (byte) c;
                    if (c != '#') {
                        logger.error("non mojasi object in key backup files");
                        break;
                    }
                    int tmp;
                    int valLength = 0;
                    c = in.read();
                    buff[i++] = (byte) c;
                    while ((tmp = Base64Stuff.char2nibble[c]) >= 0) {
                        valLength = (valLength << 6) + tmp;
                        c = in.read();
                        buff[i++] = (byte) c;
                    }
                    in.read(buff, i, valLength - i);
                    Object obj = JotMojasiParser.bytes2Object(buff, g);
                    if (obj instanceof ObjectServerCachedObject) {
                        ObjectServerCachedObject osco = (ObjectServerCachedObject) obj;
                        g.objectServers.setForBackupManager(osco);
                        recoverCount++;
                        pos += valLength;
                    } else logger.error("expected ObjectServerCachedObject got " + obj);
                }
            } catch (IOException e) {
                logger.error("backfile failed to recover ", e);
            }
        }
        logger.info("Recovered " + recoverCount + " in " + timer.elapsed());
        recoverMode = false;
    }

    static Guid makeGuid(int i) {
        return new Guid(i + "+im+" + i);
    }

    static ImMessage makeObject(int i) {
        ImMessage im = new ImMessage();
        im.guid = makeGuid(i);
        int mod = 512;
        int len = avgObjectSize + (i % mod) - mod / 2;
        String message = Integer.toString(i) + "___message___";
        while (message.length() < len) message = message + message;
        message = message.substring(0, len);
        im.message = message;
        im.entityGlobalContext = null;
        im.version = 0;
        return im;
    }

    @Deprecated
    public void wipeout() {
        if (1 == 1) return;
        for (Long lo : files.keySet()) {
            FileStruct ff = files.get(lo);
            try {
                ff.delete();
            } catch (IOException e) {
                logger.error("wipeout failed on " + lo);
            }
        }
        init();
    }

    static int targetSize = 64 * 1024 * 1024;

    static int memSize = 16 * 1024 * 1024;

    static int objectCount = 128 * 1024;

    static int avgObjectSize = targetSize / objectCount;

    static int targetDiskSpace = targetSize * 4 / 3;

    public static void main(String[] args) {
        BackupFileManager.logger.setLevel(Level.TRACE);
        JotContext g = new JotContext(Role.ObjectStore, "Dum00");
        BackupFileManager mgr = g.objectServers.backfilemgr;
        if (mgr == null) {
            logger.error("your config may be using a DataSourceProvider, so there is no BackFileMgr");
            return;
        }
        g.objectServers.maxCachedByteCount = memSize;
        System.out.println("total file size = " + mgr.fileMaxSize * mgr.fileMaxCount);
        System.out.println("avg object size " + avgObjectSize);
        int[] versions = new int[objectCount];
        EndTimer timer = new EndTimer();
        mgr.recoverCount = 0;
        mgr.recover();
        System.out.println("Recovered " + mgr.recoverCount + " in " + timer.elapsed());
        mgr.recoverCount = 0;
        g.objectServers.checkCachedByteCount();
        timer = new EndTimer();
        long totalObjectSize = 0;
        if (g.objectServers.cachedByteCount < memSize) {
            for (int i = 0; i < objectCount; i++) {
                Guid guid = makeGuid(i);
                CachedObject co = g.objectServers.findCachedObjForBackFileMgr(guid);
                if (co != null) {
                    versions[i] = co.version;
                    continue;
                }
                ImMessage im = makeObject(i);
                byte[] bytes = MojasiWriter.object2bytes(im);
                totalObjectSize += bytes.length;
                versions[i] = im.version;
                g.objectServers.put(im.guid, null, im, im.version, bytes);
                if (i % (objectCount / 10) == 0) System.out.println(".");
            }
            System.out.println();
        }
        System.out.println("make " + objectCount + " objects in  " + timer.elapsed() + " totalObjectSize = " + totalObjectSize);
        g.objectServers.checkCachedByteCount();
        timer = new EndTimer();
        totalObjectSize = 0;
        if (g.objectServers.cachedByteCount < memSize) {
            for (int i = 0; i < objectCount; i++) {
                ImMessage im = makeObject(i);
                byte[] bytes = MojasiWriter.object2bytes(im);
                totalObjectSize += bytes.length;
                CachedObject co = g.objectServers.findCachedObjForBackFileMgr(im.guid);
                if (co != null) im.version = co.version + 1;
                versions[i] = im.version;
                g.objectServers.put(im.guid, null, im, im.version, bytes);
                if (i % (objectCount / 10) == 0) System.out.println(".");
            }
            System.out.println();
        }
        System.out.println("bump " + objectCount + " objects in  " + timer.elapsed() + " totalObjectSize = " + totalObjectSize);
        g.objectServers.checkCachedByteCount();
        timer = new EndTimer();
        if (g.objectServers.cachedByteCount < memSize) {
            for (int i = 0; i < objectCount; i++) {
                ImMessage im = makeObject(i);
                byte[] bytes = MojasiWriter.object2bytes(im);
                totalObjectSize += bytes.length;
                CachedObject co = g.objectServers.findCachedObjForBackFileMgr(makeGuid(i));
                if (co != null) {
                    assert versions[i] == co.version;
                }
                if (i % (objectCount / 10) == 0) System.out.println(".");
            }
            System.out.println();
        }
        System.out.println("version check " + objectCount + " objects in  " + timer.elapsed());
        g.objectServers.checkCachedByteCount();
        mgr.close();
        g.stop(true);
    }

    public boolean isRecoverMode() {
        return recoverMode;
    }
}
