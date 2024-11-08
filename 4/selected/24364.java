package org.expasy.jpl.commons.base.io;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * A File that supports localized paths
 * 
 * Also adds basename/dirname functionality.
 * 
 * Note: This kind of crosses the line between the core interpreter and the
 * "util" package. It's here so that we're consistent in the source() feature of
 * the interpreter.
 */
public final class JPLFile extends File {

    private static final long serialVersionUID = 3173731867857806938L;

    private String dirName = "";

    private String baseName;

    private String baseNameNoExt;

    private String extension;

    public JPLFile(String fileName) {
        super(fileName);
        int lastSepIndex = fileName.lastIndexOf(File.separator);
        if (lastSepIndex != -1) {
            dirName = fileName.substring(0, lastSepIndex);
            baseName = fileName.substring(lastSepIndex + 1);
        } else {
            baseName = fileName;
        }
        int extIndex = fileName.lastIndexOf('.');
        if (extIndex < lastSepIndex) {
            extIndex = -1;
        }
        if (extIndex >= 0) {
            baseNameNoExt = fileName.substring(lastSepIndex + 1, extIndex);
            extension = fileName.substring(extIndex + 1);
        } else {
            baseNameNoExt = baseName;
        }
    }

    public String getDirName() {
        return dirName;
    }

    public String getBaseName() {
        return baseName;
    }

    public String getBaseNameNoExt() {
        return baseNameNoExt;
    }

    /**
	 * Get the file name extension.
	 * 
	 * @return the last characters after the period in the name of a file.
	 */
    public String getExtension() {
        return extension;
    }

    public boolean hasExtension() {
        return extension != null && extension.length() > 0;
    }

    public static String getDirName(String filename) {
        JPLFile file = new JPLFile(filename);
        return file.getDirName();
    }

    public static String getBaseName(String filename) {
        JPLFile file = new JPLFile(filename);
        return file.getBaseName();
    }

    public static String getBaseNameNoExt(String filename) {
        JPLFile file = new JPLFile(filename);
        return file.getBaseNameNoExt();
    }

    /**
	 * Get the file name extension.
	 * 
	 * @return the last characters after the period in the name of a file.
	 */
    public static String getExtension(String filename) {
        JPLFile file = new JPLFile(filename);
        return file.getExtension();
    }

    public static boolean hasExtension(String filename) {
        JPLFile file = new JPLFile(filename);
        return file.hasExtension();
    }

    public static void copy(File file1, File file2) throws IOException {
        FileReader in = new FileReader(file1);
        FileWriter out = new FileWriter(file2);
        int c;
        while ((c = in.read()) != -1) out.write(c);
        in.close();
        out.close();
    }

    public String toString() {
        return super.toString() + ", dirName = " + dirName + ", baseName = " + baseName;
    }
}
