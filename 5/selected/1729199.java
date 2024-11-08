package jframe.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.utils.IOUtils;

/**
 * @描述:<p>ZIP压缩和解压工具</p>
 *
 * @作者: 叶平平(yepp)
 *
 * @时间: 2011-12-30 下午10:21:24
 */
public class ZipUtil {

    /**
	 * 将文件集合 压缩到文件 没有目录结构 
	 * @param zipFileName 压缩文件名 
	 * @param filepaths   文件集合
	 * @throws IOException 
	 */
    public static void zip(String zipFileName, List<String> filepaths) throws IOException {
        File file = new File(zipFileName);
        ZipArchiveOutputStream zos = null;
        try {
            zos = new ZipArchiveOutputStream(file);
            if (filepaths != null && filepaths.size() > 0) {
                for (String filepath : filepaths) {
                    File efile = new File(filepath);
                    zip(zos, efile, "");
                }
            }
        } finally {
            zos.flush();
            zos.finish();
            zos.close();
        }
    }

    /**
	 * 按文件夹结构压缩文件  
	 * @param zipFileName   压缩文件名 
	 * @param filepath   可以单文件也可以使文件夹
	 * @throws IOException 
	 */
    public static void zip(String zipFileName, String filepath) throws IOException {
        File file = new File(zipFileName);
        ZipArchiveOutputStream zos = null;
        try {
            zos = new ZipArchiveOutputStream(file);
            File efile = new File(filepath);
            zip(zos, efile, "");
        } finally {
            zos.flush();
            zos.finish();
            zos.close();
        }
    }

    /**
	 *  递归压缩 文件夹 文件
	 * @param zout
	 * @param file
	 * @param base
	 * @param buff
	 * @throws IOException
	 */
    private static void zip(ZipArchiveOutputStream zos, File efile, String base) throws IOException {
        if (efile.isDirectory()) {
            File[] lf = efile.listFiles();
            base = base + File.separator + efile.getName();
            for (File file : lf) {
                zip(zos, file, base);
            }
        } else {
            ZipArchiveEntry entry = new ZipArchiveEntry(efile, base + File.separator + efile.getName());
            zos.setEncoding("utf-8");
            zos.putArchiveEntry(entry);
            InputStream is = new FileInputStream(efile);
            IOUtils.copy(is, zos);
            is.close();
            zos.closeArchiveEntry();
        }
    }

    /**
	 * 解压文件
	 * @param zipFileName   zip 文件
	 * @param folder        目标文件夹，为空就是当前文件夹
	 * @param isCreate      是否要
	 * @throws IOException 
	 */
    @SuppressWarnings("unchecked")
    public static void unzip(String zipFileName, String folder, boolean isCreate) throws IOException {
        File file = new File(zipFileName);
        File folderfile = null;
        if (file.exists() && file.isFile()) {
            String mfolder = folder == null ? file.getParent() : folder;
            String fn = file.getName();
            fn = fn.substring(0, fn.lastIndexOf("."));
            mfolder = isCreate ? (mfolder + File.separator + fn) : mfolder;
            folderfile = new File(mfolder);
            if (!folderfile.exists()) {
                folderfile.mkdirs();
            }
        } else {
            throw new FileNotFoundException("不存在 zip 文件");
        }
        ZipFile zipFile = new ZipFile(file);
        try {
            Enumeration<ZipArchiveEntry> en = zipFile.getEntries();
            ZipArchiveEntry ze = null;
            while (en.hasMoreElements()) {
                ze = en.nextElement();
                if (ze.isDirectory()) {
                    String dirName = ze.getName();
                    dirName = dirName.substring(0, dirName.length() - 1);
                    File f = new File(folderfile.getPath() + File.separator + dirName);
                    f.mkdirs();
                } else {
                    File f = new File(folderfile.getPath() + File.separator + ze.getName());
                    if (!f.getParentFile().exists()) {
                        f.getParentFile().mkdirs();
                    }
                    f.createNewFile();
                    InputStream in = zipFile.getInputStream(ze);
                    OutputStream out = new FileOutputStream(f);
                    IOUtils.copy(in, out);
                    out.close();
                    in.close();
                }
            }
        } finally {
            zipFile.close();
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        List<String> paths = new ArrayList<String>();
        paths.add("D:/1.txt");
        paths.add("D:/2.txt");
        String file = "D:\\WEB-INF";
        paths.add(file);
    }
}
