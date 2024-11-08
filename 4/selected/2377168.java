package com.incendiaryblue.file;

import com.incendiaryblue.io.FileHelper;
import java.io.*;
import java.util.*;
import com.incendiaryblue.util.Debug;

/**
 * The FileInfo class can be used to get information about a File object.
 * Currently there is support for finding image sizes (width & height) for GIF
 * and JPG type images.
 */
public class FileInfo {

    private static final List validJPG = new ArrayList();

    private static Debug debug = new Debug(FileInfo.class);

    static {
        validJPG.add(new Integer(0xC0));
        validJPG.add(new Integer(0xC1));
        validJPG.add(new Integer(0xC2));
        validJPG.add(new Integer(0xC3));
        validJPG.add(new Integer(0xC5));
        validJPG.add(new Integer(0xC6));
        validJPG.add(new Integer(0xC7));
        validJPG.add(new Integer(0xC9));
        validJPG.add(new Integer(0xCA));
        validJPG.add(new Integer(0xCB));
        validJPG.add(new Integer(0xCD));
        validJPG.add(new Integer(0xCE));
        validJPG.add(new Integer(0xCF));
    }

    private String file_type;

    private String extension;

    private File f;

    private Map properties = new HashMap();

    /**
	 * Create a new FileInfo object for the given File object. The File object
	 * must represent a file that exists in the file system, or an
	 * IllegalArgumentException will be thrown.
	 */
    public FileInfo(File f) {
        if (f == null || !f.isFile()) {
            throw new IllegalArgumentException("f must be an existing file");
        }
        this.f = f;
        debug.write("File = " + f.getPath());
        this.extension = FileHelper.getExtension(f.getPath());
        this.file_type = FileType.getDescription(f);
        getInfo();
        debug.write("Constructed");
    }

    /**
	 * Returns a String describing the type of the file. This is determined from
	 * the file's extension if it has one, and looks up the description from the
	 * FileType class. See the javadoc for that class for information on
	 * configuring file type descriptions.
	 */
    public String getFileType() {
        return file_type;
    }

    /**
	 * Get the names of the dynamic properties assigned to this FileInfo.
	 * These properties will change depending on the file type, for example an
	 * image file will contain width and height properties.
	 */
    public Collection getPropertyNames() {
        return properties.keySet();
    }

    /**
	 * Get the property with the given name. Returns null if the property is not
	 * set in this FileInfo object.
	 */
    public Object getProperty(String name) {
        return properties.get(name);
    }

    /**
	 * Returns true if the type of this file is configured in the FileType class
	 * as an image type.
	 */
    public boolean isImage() {
        return FileType.isImageFile(f);
    }

    /**
	 * Read the first n bytes of the underlying file into buffer, where n is
	 * the minimum of the file's length and the size of buffer.
	 */
    private int readBytes(byte[] buffer) {
        int read = -1;
        try {
            FileInputStream in = new FileInputStream(f);
            read = in.read(buffer);
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return read;
    }

    /**
	 * Performs the work of getting the information for the file.
	 */
    private void getInfo() {
        byte[] buf;
        if (this.extension.equals("gif")) {
            buf = new byte[10];
            int read = readBytes(buf);
            properties.put("width", new Integer(-1));
            properties.put("height", new Integer(-1));
            if (read == 10) {
                if (unsigned(buf[0]) == 0x47 && unsigned(buf[1]) == 0x49 && unsigned(buf[2]) == 0x46) {
                    properties.put("width", new Integer(unsigned(buf[6]) + (unsigned(buf[7]) << 8)));
                    properties.put("height", new Integer(unsigned(buf[8]) + (unsigned(buf[9]) << 8)));
                }
            }
        } else if (this.extension.equals("jpg") || this.extension.equals("jpeg")) {
            debug.write("checking jpg");
            buf = new byte[64000];
            int read = readBytes(buf);
            debug.write("read " + read + " bytes");
            properties.put("width", new Integer(-1));
            properties.put("height", new Integer(-1));
            if (read > 3) {
                if ((unsigned(buf[0]) == 255) && (unsigned(buf[1]) == 216) && (unsigned(buf[2]) == 255)) {
                    debug.write("Testing for JPEG");
                    int index = 0;
                    boolean bFound = false;
                    for (; (index < read - 8) && !bFound; index++) {
                        if (unsigned(buf[index]) == 255 && validJPG.contains(new Integer(unsigned(buf[index + 1])))) {
                            debug.write("Found it - index = " + index);
                            bFound = true;
                        }
                    }
                    if (bFound) {
                        properties.put("width", new Integer(unsigned(buf[index + 7]) + (unsigned(buf[index + 6]) << 8)));
                        properties.put("height", new Integer(unsigned(buf[index + 5]) + (unsigned(buf[index + 4]) << 8)));
                    }
                }
            }
        }
    }

    /**
	 * Performs a bitmask operation on a byte to return the unsigned integer it
	 * represents. This is necessary as bytes in images are unsigned, but the
	 * standard Java byte data type is signed.
	 */
    private static int unsigned(byte b) {
        return (int) b & 0xFF;
    }
}
