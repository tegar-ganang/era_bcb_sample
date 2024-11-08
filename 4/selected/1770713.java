package com.tikal.delivery.patchtool.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.tools.ant.DirectoryScanner;

/**
 * @author itaio
 * 
 * TODO To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Style - Code Templates
 */
public class JarUtil {

    /**
	 * 
	 */
    public JarUtil() {
        super();
    }

    public static File unjar(File in, File workingDir) throws IOException {
        if (in == null || in.length() == 0) return null;
        return unjar(in, workingDir, in.getName());
    }

    public static File unjar(File in, File workingDir, String prefix) throws IOException {
        ZipFile zf;
        Enumeration enumer;
        if (in == null || in.length() == 0) return null;
        File dest = new File(workingDir, prefix + System.currentTimeMillis() + PatchRandom.getRandom());
        dest.mkdir();
        if (!dest.exists()) {
            dest.mkdir();
        }
        if (!dest.isDirectory()) {
            throw new IOException("Destination must be a directory.");
        }
        zf = new ZipFile(in);
        enumer = zf.entries();
        while (enumer.hasMoreElements()) {
            ZipEntry target = (ZipEntry) enumer.nextElement();
            String fileName = target.getName();
            if (fileName.charAt(fileName.length() - 1) == '/') {
                fileName = fileName.substring(0, fileName.length() - 1);
            }
            if (fileName.charAt(0) == '/') {
                fileName = fileName.substring(1);
            }
            if (File.separatorChar != '/') {
                fileName = fileName.replace('/', File.separatorChar);
            }
            File file = new File(dest, fileName);
            if (target.isDirectory()) {
                file.mkdirs();
            } else {
                File parent = file.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
                InputStream is = zf.getInputStream(target);
                BufferedInputStream bis = new BufferedInputStream(is);
                FileOutputStream fos = new FileOutputStream(file);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                int c;
                while ((c = bis.read()) != -1) {
                    bos.write((byte) c);
                }
                bos.close();
                fos.close();
            }
        }
        return dest;
    }

    /**
	 * create jar
	 */
    public static File jar(File in, String outArc, File tempDir, PatchConfigXML conf) {
        FileOutputStream arcFile = null;
        JarOutputStream jout = null;
        DirectoryScanner ds = null;
        ds = new DirectoryScanner();
        ds.setCaseSensitive(true);
        ds.setBasedir(in);
        ds.scan();
        ds.setCaseSensitive(true);
        String[] names = ds.getIncludedFiles();
        ArrayList exName = new ArrayList();
        if (names == null || names.length < 1) return null;
        File tempArc = new File(tempDir, outArc.substring(0, outArc.length()));
        try {
            Manifest mf = null;
            List v = new ArrayList();
            for (int i = 0; i < names.length; i++) {
                if (names[i].toUpperCase().indexOf("MANIFEST.MF") > -1) {
                    FileInputStream fis = new FileInputStream(in.getAbsolutePath() + "/" + names[i].replace('\\', '/'));
                    mf = new Manifest(fis);
                } else v.add(names[i]);
            }
            String[] toJar = new String[v.size()];
            v.toArray(toJar);
            tempArc.createNewFile();
            arcFile = new FileOutputStream(tempArc);
            if (mf == null) jout = new JarOutputStream(arcFile); else jout = new JarOutputStream(arcFile, mf);
            byte[] buffer = new byte[1024];
            for (int i = 0; i < toJar.length; i++) {
                if (conf != null) {
                    if (!conf.allowFileAction(toJar[i], PatchConfigXML.OP_CREATE)) {
                        exName.add(toJar[i]);
                        continue;
                    }
                }
                String currentPath = in.getAbsolutePath() + "/" + toJar[i];
                String entryName = toJar[i].replace('\\', '/');
                JarEntry currentEntry = new JarEntry(entryName);
                jout.putNextEntry(currentEntry);
                FileInputStream fis = new FileInputStream(currentPath);
                int len;
                while ((len = fis.read(buffer)) >= 0) jout.write(buffer, 0, len);
                fis.close();
                jout.closeEntry();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                jout.close();
                arcFile.close();
            } catch (IOException e1) {
                throw new RuntimeException(e1);
            }
        }
        return tempArc;
    }
}
