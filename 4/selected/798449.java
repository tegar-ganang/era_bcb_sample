package moxie.fs;

import fuse.*;
import seda.sandStorm.api.ConfigDataIF;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import static bamboo.util.Curry.*;

public class MirrorFs extends MoxieHandler {

    private static final int block_size = 512;

    private static long fh_cntr = 1000;

    private FuseStatfs statfs;

    private String src_path;

    private String mnt_path;

    public void init(ConfigDataIF config) throws Exception {
        super.init(config);
        if (config.contains("SourcePath")) src_path = config.getString("SourcePath");
        logger.warn("SourcePath=" + src_path);
        statfs = new FuseStatfs();
        statfs.blocks = 1000;
        statfs.blockSize = block_size;
        statfs.blocksFree = 0;
        statfs.files = 1000;
        statfs.filesFree = 0;
        statfs.namelen = 2048;
        return;
    }

    public void mount(String mount_path, String[] args, Thunk1<FuseException> cb) {
        if (logger.isInfoEnabled()) {
            String arg_st = new String();
            for (String s : args) arg_st += s + ",";
            logger.info("mount: mnt_path=" + mount_path + ", args=" + arg_st);
        }
        this.mnt_path = mount_path;
        FuseException fe = null;
        cb.run(fe);
        return;
    }

    public void unmount(String mount_path, Thunk1<FuseException> cb) {
        logger.info("unmount: mnt_path=" + mount_path);
        FuseException fe = null;
        cb.run(fe);
        return;
    }

    public void statfs(Thunk2<FuseException, FuseStatfs> cb) {
        logger.info("statfs:");
        FuseException fe = null;
        cb.run(fe, statfs);
        return;
    }

    public void getattr(String path, Thunk2<FuseException, FuseStat> cb) {
        String fullpath = getPath(path);
        logger.info("getattr: path=" + fullpath);
        FuseStat stat = null;
        File f = new File(fullpath);
        if (!f.exists()) {
            FuseException fe = new FuseException("No such file.");
            fe.initErrno(FuseException.ENOENT);
            cb.run(fe, stat);
            return;
        }
        stat = new FuseStat();
        stat.mode = f.isDirectory() ? FuseFtype.TYPE_DIR | 0755 : FuseFtype.TYPE_FILE | 0644;
        stat.nlink = 1;
        stat.uid = 0;
        stat.gid = 0;
        stat.size = f.length();
        stat.atime = stat.mtime = stat.ctime = (int) f.lastModified();
        stat.blocks = (int) ((stat.size + 511L) / 512L);
        FuseException fe = null;
        cb.run(fe, stat);
        return;
    }

    public void create(String path, int mode, Thunk1<FuseException> cb) {
        String fullpath = getPath(path);
        logger.info("create: path=" + fullpath + " mode=" + mode);
        File f = new File(fullpath);
        if (f.exists()) {
            FuseException fe = new FuseException("File exists.");
            fe.initErrno(FuseException.EEXIST);
            cb.run(fe);
            return;
        }
        try {
            boolean success = f.createNewFile();
            if (!success) {
                throw new IOException("create failed.");
            }
        } catch (Exception e) {
            FuseException fe = new FuseException("Failed.");
            fe.initErrno(FuseException.EACCES);
            cb.run(fe);
            return;
        }
        FuseException fe = null;
        cb.run(fe);
        return;
    }

    public void open(String path, int flags, Thunk2<FuseException, Long> cb) {
        String fullpath = getPath(path);
        logger.info("open: path=" + fullpath + " flags=" + flags);
        long fhandle = fh_cntr++;
        FuseException fe = null;
        cb.run(fe, fhandle);
        return;
    }

    public void close(String path, long fh, Thunk1<FuseException> cb) {
        String fullpath = getPath(path);
        logger.info("close: path=" + fullpath + " fh=" + fh);
        FuseException fe = null;
        cb.run(fe);
        return;
    }

    public void release(String path, long fh, int flags, Thunk1<FuseException> cb) {
        String fullpath = getPath(path);
        logger.info("release: path=" + fullpath + " fh=" + fh + " flags=" + flags);
        FuseException fe = null;
        cb.run(fe);
        return;
    }

    public void read(String path, long fh, ByteBuffer buffer, long offset, Thunk1<FuseException> cb) {
        String fullpath = getPath(path);
        int length = buffer.capacity();
        logger.info("read: path=" + fullpath + " fh=" + fh + " offset=" + offset + " length=" + length);
        File f = new File(fullpath);
        if (!f.exists()) {
            FuseException fe = new FuseException("Does not exist.");
            fe.initErrno(FuseException.ENOENT);
            cb.run(fe);
            return;
        }
        if (!f.canRead()) {
            FuseException fe = new FuseException("Not readable.");
            fe.initErrno(FuseException.EINVAL);
            cb.run(fe);
            return;
        }
        try {
            FileChannel fchannel = (new FileInputStream(f)).getChannel();
            fchannel.position(offset);
            fchannel.read(buffer);
        } catch (Exception e) {
            FuseException fe = new FuseException("Read failed.");
            fe.initErrno(FuseException.EACCES);
            cb.run(fe);
            return;
        }
        FuseException fe = null;
        cb.run(fe);
        return;
    }

    public void write(String path, long fh, ByteBuffer buffer, long offset, Thunk1<FuseException> cb) {
        String fullpath = getPath(path);
        int length = buffer.capacity();
        logger.info("write: path=" + fullpath + " fh=" + fh + " offset=" + offset + " length=" + length);
        File f = new File(fullpath);
        if (!f.exists()) {
            FuseException fe = new FuseException("Does not exist.");
            fe.initErrno(FuseException.ENOENT);
            cb.run(fe);
            return;
        }
        if (!f.canWrite()) {
            FuseException fe = new FuseException("Not writeable.");
            fe.initErrno(FuseException.EINVAL);
            cb.run(fe);
            return;
        }
        try {
            FileChannel fchannel = (new FileOutputStream(f)).getChannel();
            fchannel.position(offset);
            fchannel.write(buffer);
        } catch (Exception e) {
            FuseException fe = new FuseException("Write failed.");
            fe.initErrno(FuseException.EACCES);
            cb.run(fe);
            return;
        }
        FuseException fe = null;
        cb.run(fe);
        return;
    }

    public void fsync(String path, long fhandle, boolean data_sync, Thunk1<FuseException> cb) {
        String fullpath = getPath(path);
        logger.info("fsync: path=" + fullpath + " fhandle=" + fhandle + " data_sync=" + data_sync);
        FuseException fe = new FuseException("Not supported");
        fe.initErrno(FuseException.EROFS);
        cb.run(fe);
        return;
    }

    public void truncate(String path, long size, Thunk1<FuseException> cb) {
        String fullpath = getPath(path);
        logger.info("truncate: path=" + fullpath + " size=" + size);
        FuseException fe = new FuseException("Read-only filesystem.");
        fe.initErrno(FuseException.EROFS);
        cb.run(fe);
        return;
    }

    public void getdir(String path, Thunk2<FuseException, FuseDirEnt[]> cb) {
        String fullpath = getPath(path);
        logger.info("getdir: path=" + fullpath);
        FuseDirEnt[] dir_entries = null;
        File f = new File(fullpath);
        if (!f.isDirectory()) {
            FuseException fe = new FuseException("Not A Directory");
            fe.initErrno(FuseException.ENOTDIR);
            cb.run(fe, dir_entries);
            return;
        }
        File children[] = f.listFiles();
        dir_entries = new FuseDirEnt[children.length];
        for (int i = 0; i < children.length; ++i) {
            dir_entries[i] = new FuseDirEnt();
            dir_entries[i].name = children[i].getName();
            dir_entries[i].mode = children[i].isDirectory() ? FuseFtype.TYPE_DIR : FuseFtype.TYPE_FILE;
        }
        FuseException fe = null;
        cb.run(fe, dir_entries);
        return;
    }

    public void mkdir(String path, int mode, Thunk1<FuseException> cb) {
        String fullpath = getPath(path);
        logger.info("mkdir: path=" + fullpath + " mode=" + mode);
        File f = new File(fullpath);
        if (f.exists()) {
            FuseException fe = new FuseException("Directory already exists.");
            fe.initErrno(FuseException.EEXIST);
            cb.run(fe);
            return;
        }
        try {
            f.mkdirs();
        } catch (Exception e) {
            FuseException fe = new FuseException("Read Only");
            fe.initErrno(FuseException.EACCES);
            cb.run(fe);
            return;
        }
        FuseException fe = null;
        cb.run(fe);
        return;
    }

    public void rmdir(String path, Thunk1<FuseException> cb) {
        String fullpath = getPath(path);
        logger.info("rmdir: path=" + fullpath);
        FuseException fe = new FuseException("Not implemented.");
        fe.initErrno(FuseException.EIO);
        cb.run(fe);
        return;
    }

    public void link(String from, String to, Thunk1<FuseException> cb) {
        String fullpath_from = getPath(from);
        String fullpath_to = getPath(to);
        logger.info("link: from=" + fullpath_from + " to=" + fullpath_to);
        FuseException fe = new FuseException("Not implemented.");
        fe.initErrno(FuseException.EIO);
        cb.run(fe);
        return;
    }

    public void symlink(String from, String to, Thunk1<FuseException> cb) {
        String fullpath_from = getPath(from);
        String fullpath_to = getPath(to);
        logger.info("symlink: from=" + fullpath_from + " to=" + fullpath_to);
        FuseException fe = new FuseException("Not implemented.");
        fe.initErrno(FuseException.EIO);
        cb.run(fe);
        return;
    }

    public void readlink(String path, Thunk2<FuseException, String> cb) {
        logger.info("readlink: path=" + path);
        FuseException fe = new FuseException("Not implemented.");
        fe.initErrno(FuseException.EIO);
        String link_resolve = null;
        cb.run(fe, link_resolve);
        return;
    }

    public void unlink(String path, Thunk1<FuseException> cb) {
        String fullpath = getPath(path);
        logger.info("unlink: path=" + fullpath);
        FuseException fe = new FuseException("Not implemented.");
        fe.initErrno(FuseException.EIO);
        cb.run(fe);
        return;
    }

    public void rename(String from, String to, Thunk1<FuseException> cb) {
        String fullpath_from = getPath(from);
        String fullpath_to = getPath(to);
        logger.info("rename: from=" + fullpath_from + " to=" + fullpath_to);
        FuseException fe = new FuseException("Not implemented.");
        fe.initErrno(FuseException.EIO);
        cb.run(fe);
        return;
    }

    public void chmod(String path, int mode, Thunk1<FuseException> cb) {
        String fullpath = getPath(path);
        logger.info("chmod: path=" + fullpath + " mode=" + mode);
        FuseException fe = new FuseException("Not implemented.");
        fe.initErrno(FuseException.EIO);
        cb.run(fe);
        return;
    }

    public void chown(String path, int uid, int gid, Thunk1<FuseException> cb) {
        String fullpath = getPath(path);
        logger.info("chown: path=" + fullpath + " uid=" + uid + " gid=" + gid);
        FuseException fe = new FuseException("Not implemented.");
        fe.initErrno(FuseException.EIO);
        cb.run(fe);
        return;
    }

    public void utime(String path, int atime, int mtime, Thunk1<FuseException> cb) {
        String fullpath = getPath(path);
        logger.info("utime: path=" + fullpath + " atime=" + atime + " mtime=" + mtime);
        FuseException fe = new FuseException("Not implemented.");
        fe.initErrno(FuseException.EIO);
        cb.run(fe);
        return;
    }

    private String getPath(String path) {
        return (src_path + path);
    }
}
