package com.stfa.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * StFA(Simple Transactional File Access)基础功能类
 *
 * @author  J.Z.
 * @version 1.0, 2010-5-7
 */
public class StFAIO {

    private static StFAIO instance;

    private StFAIO() {
    }

    public static StFAIO getInstance() {
        if (instance == null) {
            synchronized (StFAIO.class) {
                if (instance == null) instance = new StFAIO();
            }
        }
        return instance;
    }

    /**
	 * 判断文件或目录是否存在
	 * 
	 * @param target 文件或目录名
	 * @return true 文件或目录存在，反之 false
	 */
    public boolean isExist(String target) {
        return isExist(new File(target));
    }

    /**
	 * 判断文件或目录是否存在
	 * 
	 * @param target 文件或目录
	 * @return true 文件或目录存在，反之 false
	 */
    public boolean isExist(File target) {
        return (target != null && target.exists());
    }

    /**
	 * 判断目标是否是文件
	 * 
	 * @param target 目标名
	 * @return true 目标存在且是文件, 否则  false
	 */
    public boolean isFile(String target) {
        return isFile(new File(target));
    }

    /**
	 * 判断目标是否是文件
	 * 
	 * @param target 目标
	 * @return true 目标存在且是文件, 否则  false
	 */
    public boolean isFile(File target) {
        return (target != null && target.exists() && target.isFile());
    }

    /**
	 * 判断目标是否是目录
	 * 
	 * @param target 目标名
	 * @return true 目标存在且是目录, 否则 false
	 */
    public boolean isDir(String target) {
        return isDir(new File(target));
    }

    /**
	 * 判断目标是否是目录
	 * 
	 * @param target 目标
	 * @return true 目标存在且是目录, 否则 false
	 */
    public boolean isDir(File target) {
        return (target != null && target.exists() && target.isDirectory());
    }

    /**
	 * 将数据写入到目标文件
	 * 
	 * @param target 目标文件
	 * @param data 数据流
	 * @param bufferSize 缓存大小
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
    public void save(String target, InputStream data, int bufferSize) throws IOException {
        OutputStream writer = new FileOutputStream(target);
        byte[] bytes = new byte[bufferSize];
        int readn = -1;
        while ((readn = data.read(bytes)) != -1) {
            writer.write(bytes, 0, readn);
        }
        writer.flush();
        writer.close();
    }

    /**
	 * 将字节数据写入到目标文件。支持大文件(>2GB)
	 * 
	 * @param target 文件名
	 * @param b 字节流
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
    public void save(String target, byte[][] b) throws IOException {
        OutputStream writer = new FileOutputStream(target);
        for (int i = 0; i < b.length; i++) {
            writer.write(b[i]);
        }
        writer.flush();
        writer.close();
    }

    /**
	 * 将字节数据写入到目标文件。文件大小小于2GB。
	 * 
	 * @param target 目标文件名
	 * @param b 字节流
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
    public void save(String target, byte[] b) throws IOException {
        OutputStream writer = new FileOutputStream(target);
        writer.write(b);
        writer.flush();
        writer.close();
    }

    /**
	 * 创建目录
	 * 
	 * @param target 需要创建的目录名
	 * @throws IOException
	 */
    public void mkDir(String target) throws IOException {
        mkDir(new File(target));
    }

    /**
	 * 创建目录
	 * 
	 * @param target 需要创建的目录
	 * @throws IOException
	 */
    public void mkDir(File target) throws IOException {
        if (!target.mkdir()) throw new IOException("create directory failed");
    }

    /**
	 * 创建目录，所有必要的目录都将会被创建
	 * 
	 * @param target 需要创建的目录名
	 * @throws IOException
	 */
    public void mkDirs(String target) throws IOException {
        mkDirs(new File(target));
    }

    /**
	 * 创建目录，所有必要的目录都将会被创建
	 * 
	 * @param target 需要创建的目录
	 * @throws IOException
	 */
    public void mkDirs(File target) throws IOException {
        if (!target.mkdirs()) throw new IOException("create directory failed");
    }

    /**
	 * 删除文件。若目标为文件夹，则无论文件夹空与否，都将删除失败。
	 * 
	 * @param target 目标文件名
	 * @throws IOException
	 */
    public void rmFile(String target) throws IOException {
        rmFile(new File(target));
    }

    /**
	 * 删除文件。若目标为文件夹，则无论文件夹空与否，都将删除失败。
	 * 
	 * @param target 目标文件
	 * @throws IOException
	 */
    public void rmFile(File target) throws IOException {
        if (target.isDirectory()) throw new IOException("file not exist(cannot delete a directory)");
        if (!target.delete()) throw new IOException("remove file failed");
    }

    /**
	 * 删除文件夹。若目标为文件，则删除失败。
	 * 
	 * @param targetDir 目标文件夹
	 * @throws IOException
	 */
    public void rmDir(String targetDir) throws IOException {
        rmDir(new File(targetDir));
    }

    /**
	 * 删除文件夹。若目标为文件，则删除失败。
	 * 
	 * @param targetDir 目标文件夹
	 * @throws IOException
	 */
    public void rmDir(File targetDir) throws IOException {
        if (!targetDir.isDirectory()) throw new IOException("directory not exist(cannot delete a file)");
        File[] list = targetDir.listFiles();
        for (int i = 0; i < list.length; i++) {
            if (!list[i].isDirectory()) rmFile(list[i]); else rmDir(list[i]);
        }
        if (!targetDir.delete()) throw new IOException("remove directory failed");
    }

    /**
	 * 将文件移动到目标文件，若需移动的文件为文件夹则移动失败。
	 * 
	 * @param source 源文件名
	 * @param target 目标文件名。若目标文件为文件夹，则使用源文件的文件名；
	 * 				 若目标文件与源文件处于同一目录，则目标文件名自动加
	 *               "N copy of"前缀，N为数字。若目标为指定文件，且目标文
	 *               件与源文件处于同一目录，则指定文件名无效。
	 * @param bufferSize 缓冲区大小
	 * @throws IOException
	 */
    public void mvFile(String source, String target, boolean replace, int bufferSize) throws IOException {
        mvFile(new File(source), new File(target), replace, bufferSize);
    }

    /**
	 * 将文件移动到目标文件，若需移动的文件为文件夹则移动失败。
	 * 
	 * @param source 源文件
	 * @param target 目标文件。若目标文件为文件夹，则使用源文件的文件名；
	 * 				 若目标文件与源文件处于同一目录，且replace为false，则目标文件名自动加
	 *               "N copy of"前缀，N为数字。若目标为指定文件，且目标文
	 *               件与源文件处于同一目录，则指定文件名无效。
	 * @param replace 是否覆盖              
	 * @param bufferSize 缓冲区大小
	 * @throws IOException
	 */
    public void mvFile(File source, File target, boolean replace, int bufferSize) throws IOException {
        cpFile(source, target, replace, bufferSize);
        if (!(source.getPath().equals(target.getPath()) && replace)) rmFile(source);
    }

    /**
	 * 将文件复制到指定目标，支持改文件名。
	 * 
	 * @param source 源文件名
	 * @param target 目标文件名。若目标文件为文件夹，则使用源文件的文件名；
	 * 				 若目标文件与源文件处于同一目录，且replace为false，则目标文件名自动加
	 *               "N copy of"前缀，N为数字。若目标为指定文件，且目标文
	 *               件与源文件处于同一目录，则指定文件名无效。
	 * @param replace 是否覆盖   
	 * @param bufferSize 缓存大小
	 * @throws IOException
	 */
    public void cpFile(String source, String target, boolean replace, int bufferSize) throws IOException {
        cpFile(new File(source), new File(target), replace, bufferSize);
    }

    /**
	 * 将文件复制到指定目标，支持改文件名。
	 * 
	 * @param source 源文件
	 * @param target 目标文件。若目标文件为文件夹，则使用源文件的文件名；
	 * 				 若目标文件与源文件处于同一目录，则目标文件名自动加
	 *               "N copy of"前缀，N为数字。若目标为指定文件，且目标文
	 *               件与源文件处于同一目录，则指定文件名无效。
	 * @param bufferSize 缓冲区大小
	 * @throws IOException
	 */
    public void cpFile(File source, File target, boolean replace, int bufferSize) throws IOException {
        if (!source.exists()) throw new IOException("source file not exists");
        if (!source.isFile()) throw new IOException("source file not exists(is a directory)");
        InputStream src = new FileInputStream(source);
        File tarn = target;
        if (target.isDirectory() || !(!(target.exists()) || replace)) {
            String tardir = target.isDirectory() ? target.getPath() : target.getParent();
            tarn = new File(tardir + File.separator + source.getName());
            int n = 1;
            while (!(!tarn.exists() || replace)) {
                tarn = new File(tardir + File.separator + String.valueOf(n) + " copy of " + source.getName());
                n++;
            }
        }
        if (source.getPath().equals(tarn.getPath()) && replace) return;
        OutputStream tar = new FileOutputStream(tarn);
        byte[] bytes = new byte[bufferSize];
        int readn = -1;
        while ((readn = src.read(bytes)) > 0) {
            tar.write(bytes, 0, readn);
        }
        tar.flush();
        tar.close();
        src.close();
    }

    /**
	 * 移动文件夹
	 * 
	 * @param sourceDir 源文件夹
	 * @param targetDir 目标地址
	 * @param merge 是否合并
	 * @param replace 是否覆盖文件
	 * @param bufferSize 缓冲区大小
	 * @throws IOException
	 */
    public void mvDir(String sourceDir, String targetDir, boolean merge, boolean replace, int bufferSize) throws IOException {
        mvDir(new File(sourceDir), new File(targetDir), merge, replace, bufferSize);
    }

    /**
	 * 移动文件夹
	 * 
	 * @param sourceDir 源文件夹
	 * @param targetDir 目标地址
	 * @param merge 是否合并
	 * @param replace 是否覆盖文件
	 * @param bufferSize 缓冲区大小
	 * @throws IOException
	 */
    public void mvDir(File sourceDir, File targetDir, boolean merge, boolean replace, int bufferSize) throws IOException {
        cpDir(sourceDir, targetDir, merge, replace, bufferSize);
        if (!(sourceDir.getPath().equals(targetDir.getPath() + File.separator + sourceDir.getName()) && merge)) rmDir(sourceDir);
    }

    /**
	 * 复制整个文件夹，不支持重命名
	 * 
	 * @param sourceDir 源文件夹。若为文件，则移动失败
	 * @param targetDir 目标地址。 若为文件，则移动失败
	 * @param merge 当merge=true, 若目标文件夹已存在，则合并
	 * @param replace 该参数仅在merge取true时作用，若为true,则覆盖文件夹中的文件
	 * @param bufferSize 缓冲区大小
	 * @throws IOException
	 */
    public void cpDir(String sourceDir, String targetDir, boolean merge, boolean replace, int bufferSize) throws IOException {
        cpDir(new File(sourceDir), new File(targetDir), merge, replace, bufferSize);
    }

    /**
	 * 复制整个文件夹，不支持重命名
	 * 
	 * @param sourceDir 源文件夹。若为文件，则移动失败
	 * @param targetDir 目标地址。 若为文件，则移动失败
	 * @param merge 当merge=true, 若目标文件夹已存在，则合并；否则重命名目标文件夹(命名规则加"N copy of"前缀, N为数字)
	 * @param replace 该参数仅在merge取true时作用，若为true,则覆盖文件夹中的文件
	 * @param bufferSize 缓冲区大小
	 * @throws IOException
	 */
    public void cpDir(File sourceDir, File targetDir, boolean merge, boolean replace, int bufferSize) throws IOException {
        if (!sourceDir.exists()) throw new FileNotFoundException("source directory not exsit");
        if (!sourceDir.isDirectory()) throw new FileNotFoundException("source directory not found (is a file)");
        if (!targetDir.exists()) throw new FileNotFoundException("target directory not exsit");
        if (!targetDir.isDirectory()) throw new FileNotFoundException("target directory not found (is a file)");
        File tardir = new File(targetDir.getPath() + File.separator + sourceDir.getName());
        int n = 1;
        while (!(!tardir.exists() || merge)) {
            tardir = new File(targetDir.getPath() + File.separator + String.valueOf(n) + " copy of " + sourceDir.getName());
            n++;
        }
        if (!tardir.exists()) mkDirs(tardir);
        File[] list = sourceDir.listFiles();
        for (int i = 0; i < list.length; i++) {
            if (!list[i].isDirectory()) cpFile(list[i], tardir, replace, bufferSize); else cpDir(list[i], tardir, merge, replace, bufferSize);
        }
    }
}
