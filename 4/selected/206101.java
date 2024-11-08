package org.appleframework.vfs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channel;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.appleframework.Apple;
import org.appleframework.exceptions.UnexpectedException;
import org.appleframework.utils.IO;

/**
 * 
 * @author zhujg
 */
public class FileSystem extends VirtualFile {

    public File file;

    public FileSystem(File file) {
        this.file = file;
    }

    public FileSystem(FileSystem parent, String path) {
        if (parent.file != null) {
            file = new File(parent.file, path);
        }
    }

    /**
   * 返回文件名
   * @return
   */
    public String getName() {
        return file.getName();
    }

    public String relativePath() {
        List<String> path = new ArrayList<String>();
        File f = file;
        while (f != null && !f.equals(Apple.appPath)) {
            path.add(f.getName());
            f = f.getParentFile();
        }
        Collections.reverse(path);
        StringBuilder builder = new StringBuilder();
        for (String p : path) {
            builder.append("/" + p);
        }
        return builder.toString();
    }

    /**
   * 是否是文件夹
   * @return
   */
    public boolean isDirectory() {
        return file.isDirectory();
    }

    /**
   * 返回最后修改时间
   * @return
   */
    public Long lastModified() {
        if (file != null) {
            return file.lastModified();
        }
        return 0l;
    }

    public long length() {
        if (file != null) {
            return file.length();
        }
        return 0;
    }

    @Override
    public List<VirtualFile> list() {
        List<VirtualFile> res = new ArrayList<VirtualFile>();
        if (exists()) {
            File[] children = file.listFiles();
            for (int i = 0; i < children.length; i++) {
                res.add(new FileSystem(children[i]));
            }
        }
        return res;
    }

    @Override
    public boolean exists() {
        return file.exists();
    }

    @Override
    public InputStream inputstream() {
        try {
            return new FileInputStream(file);
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    @Override
    public OutputStream outputstream() {
        try {
            return new FileOutputStream(file);
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    public FileSystem child(String name) {
        return new FileSystem(this, name);
    }

    public Channel channel() {
        try {
            FileInputStream fis = new FileInputStream(file);
            FileChannel ch = fis.getChannel();
            return ch;
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    @Override
    public String contentToString() {
        try {
            return IO.readContentToString(inputstream());
        } catch (IOException e) {
            throw new UnexpectedException(e);
        }
    }
}
