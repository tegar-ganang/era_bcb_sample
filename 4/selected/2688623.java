package org.magiclight.common;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 *
 * @author Mtec030
 */
public class ZipUtil {

    /**
	 * Buffer size for file operations (like copy file for example).
	 */
    private static final int TEMP_FILE_BUFFER_SIZE = 2048;

    private ZipUtil() {
    }

    /**
	 * Zip specified byte array into a byte array and return it
	 *
	 * @param buffer
	 * @return
	 */
    public static final byte[] zipBuffer(byte[] buffer) {
        if (buffer == null) {
            throw new RuntimeException("buffer is null, zipBuffer");
        }
        MLUtil.d("zipBuffer: " + buffer.length);
        ByteArrayInputStream is = new ByteArrayInputStream(buffer);
        ByteArrayOutputStream os = new ByteArrayOutputStream(1000);
        ZipOutputStream zip = new ZipOutputStream(os);
        zip.setMethod(ZipOutputStream.DEFLATED);
        try {
            ZipEntry zipentry = new ZipEntry("A");
            zipentry.setSize(buffer.length);
            zip.putNextEntry(zipentry);
            int n;
            byte[] temp = new byte[TEMP_FILE_BUFFER_SIZE];
            while ((n = is.read(temp)) > -1) {
                zip.write(temp, 0, n);
            }
            zip.closeEntry();
            zip.close();
            is.close();
            os.close();
            byte[] bb = os.toByteArray();
            MLUtil.d("zipBuffer-compressed=" + bb.length);
            return (bb);
        } catch (IOException e) {
            MLUtil.runtimeError(e, "zipBuffer");
        }
        return (null);
    }

    /**
	 * Unzip a buffer that has been zipped into buffer and return it
	 *
	 * @param buffer
	 * @return
	 */
    public static final byte[] unZipBuffer(byte[] buffer) {
        if (buffer == null) {
            throw new RuntimeException("buffer is null, unZipBuffer");
        }
        ByteArrayInputStream is = new ByteArrayInputStream(buffer);
        ByteArrayOutputStream os = new ByteArrayOutputStream(1000);
        try {
            MLUtil.d("unZipBuffer: " + buffer.length);
            ZipInputStream zip = new ZipInputStream(is);
            ZipEntry zipentry = zip.getNextEntry();
            if (zipentry == null) {
                MLUtil.runtimeError(null, "unZipBuffer failure");
                return (null);
            }
            int n;
            byte[] temp = new byte[TEMP_FILE_BUFFER_SIZE];
            while ((n = zip.read(temp)) > -1) os.write(temp, 0, n);
            zip.closeEntry();
            zip.close();
            is.close();
            os.close();
            byte[] bb = os.toByteArray();
            MLUtil.d("uncompressed=" + bb.length);
            return (bb);
        } catch (IOException e) {
            MLUtil.runtimeError(e, "unzipBuffer");
        }
        return (null);
    }

    /**
	 * Extract file from zip file
	 * zipfile is name of zip file, name is name of file in zip and
	 * dest is the name of the file to save it as.
	 *
	 * @param zipfile
	 * @param dest
	 * @param name
	 * @return
	 */
    public static final boolean zipExtract(String zipfile, String name, String dest) {
        boolean f = false;
        try {
            InputStream in = new BufferedInputStream(new FileInputStream(zipfile));
            ZipInputStream zin = new ZipInputStream(in);
            ZipEntry e;
            while ((e = zin.getNextEntry()) != null) {
                if (e.getName().equals(name)) {
                    FileOutputStream out = new FileOutputStream(dest);
                    byte b[] = new byte[TEMP_FILE_BUFFER_SIZE];
                    int len = 0;
                    while ((len = zin.read(b)) != -1) out.write(b, 0, len);
                    out.close();
                    f = true;
                    break;
                }
            }
            zin.close();
        } catch (FileNotFoundException e) {
            MLUtil.runtimeError(e, "extractZip " + zipfile + " " + name);
        } catch (IOException e) {
            MLUtil.runtimeError(e, "extractZip " + zipfile + " " + name);
        }
        return (f);
    }

    /**
	 * Extract file from zip file into byte buffer
	 * zipfile is name of zip file, name is name of file in zip
	 * null is returned if the entry is not found (no error message unless the zip is not found)
	 *
	 * @param zipfile
	 * @param name
	 * @return
	 */
    public static final byte[] zipExtract(String zipfile, String name) {
        byte[] res = null;
        try {
            InputStream in = new BufferedInputStream(new FileInputStream(zipfile));
            ZipInputStream zin = new ZipInputStream(in);
            ZipEntry e;
            while ((e = zin.getNextEntry()) != null) {
                if (e.getName().equalsIgnoreCase(name)) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    byte b[] = new byte[TEMP_FILE_BUFFER_SIZE];
                    int len = 0;
                    while ((len = zin.read(b)) != -1) out.write(b, 0, len);
                    res = out.toByteArray();
                    break;
                }
            }
            zin.close();
        } catch (FileNotFoundException e) {
            MLUtil.runtimeError(e, "extractZip " + zipfile + " " + name);
        } catch (IOException e) {
            MLUtil.runtimeError(e, "extractZip " + zipfile + " " + name);
        }
        return (res);
    }

    /**
	 * Extract file from zip byte buffer into byte buffer
	 * zipfile is name of zip file, name is name of file in zip
	 * null is returned if the entry is not found (no error message unless the zip is not found)
	 *
	 * @param zipfile
	 * @param name
	 * @return
	 */
    public static final byte[] zipExtract(byte[] zipfile, String name) {
        byte[] res = null;
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(zipfile);
            InputStream in = new BufferedInputStream(bis);
            ZipInputStream zin = new ZipInputStream(in);
            ZipEntry e;
            while ((e = zin.getNextEntry()) != null) {
                if (e.getName().equalsIgnoreCase(name)) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    byte b[] = new byte[TEMP_FILE_BUFFER_SIZE];
                    int len = 0;
                    while ((len = zin.read(b)) != -1) out.write(b, 0, len);
                    res = out.toByteArray();
                    break;
                }
            }
            zin.close();
        } catch (FileNotFoundException e) {
            MLUtil.runtimeError(e, "extractZip " + zipfile + " " + name);
        } catch (IOException e) {
            MLUtil.runtimeError(e, "extractZip " + zipfile + " " + name);
        }
        return (res);
    }

    /**
	 * Check is specified file exists in zip (case independant)
	 *
	 * @param zipfile 
	 * @param name
	 * @return
	 */
    public static final boolean zipCheck(String zipfile, String name) {
        try {
            InputStream in = new BufferedInputStream(new FileInputStream(zipfile));
            ZipInputStream zin = new ZipInputStream(in);
            ZipEntry e;
            while ((e = zin.getNextEntry()) != null) {
                if (e.getName().equalsIgnoreCase(name)) {
                    zin.close();
                    return (true);
                }
            }
            zin.close();
        } catch (FileNotFoundException e) {
            MLUtil.runtimeError(e, "checkZip " + zipfile + " " + name);
        } catch (IOException e) {
            MLUtil.runtimeError(e, "checkZip " + zipfile + " " + name);
        }
        return (false);
    }

    /**
	 * Get a list of all files that has the specified "name" in their file name,
	 * case independent name checking is used. If endTest is true endsWith() is used
	 * otherwise indexOf() is used for the name test, if name is null all files
	 * in the zip file will be returned.
	 *
	 * @param zipfile
	 * @param name
	 * @param endTest
	 * @return
	 */
    public static final ArrayList<String> zipFind(String zipfile, String name, boolean endTest) {
        ArrayList<String> result = new ArrayList<String>();
        String filter = (name != null ? name.toUpperCase() : null);
        try {
            InputStream in = new BufferedInputStream(new FileInputStream(zipfile));
            ZipInputStream zin = new ZipInputStream(in);
            ZipEntry e;
            while ((e = zin.getNextEntry()) != null) {
                if (filter == null) {
                    result.add(e.getName());
                } else {
                    if (endTest) {
                        if (e.getName().toUpperCase().endsWith(filter)) result.add(e.getName());
                    } else {
                        if (e.getName().toUpperCase().indexOf(filter) >= 0) result.add(e.getName());
                    }
                }
            }
            zin.close();
        } catch (FileNotFoundException e) {
            MLUtil.runtimeError(e, "findZip " + zipfile + " " + name);
        } catch (IOException e) {
            MLUtil.runtimeError(e, "findZip " + zipfile + " " + name);
        }
        return (result);
    }

    /**
	 * Get a list of all files that has the specified "name" in their file name,
	 * case independent name checking is used. If endTest is true endsWith() is used
	 * otherwise indexOf() is used for the name test, if name is null all files
	 * in the zip file will be returned.
	 *
	 * @param zipfile 
	 * @param name
	 * @param endTest
	 * @return
	 */
    public static final ArrayList<String> zipFind(byte[] zipfile, String name, boolean endTest) {
        ArrayList<String> result = new ArrayList<String>();
        String filter = (name != null ? name.toUpperCase() : null);
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(zipfile);
            InputStream in = new BufferedInputStream(bis);
            ZipInputStream zin = new ZipInputStream(in);
            ZipEntry e;
            while ((e = zin.getNextEntry()) != null) {
                if (filter == null) {
                    result.add(e.getName());
                } else {
                    if (endTest) {
                        if (e.getName().toUpperCase().endsWith(filter)) result.add(e.getName());
                    } else {
                        if (e.getName().toUpperCase().indexOf(filter) >= 0) result.add(e.getName());
                    }
                }
            }
            zin.close();
        } catch (FileNotFoundException e) {
            MLUtil.runtimeError(e, "findZip " + zipfile + " " + name);
        } catch (IOException e) {
            MLUtil.runtimeError(e, "findZip " + zipfile + " " + name);
        }
        return (result);
    }

    /**
	 * Add/update or delete file from zip archive
	 * zip is the contents of the zip archive
	 * name is the name of the file inside the archive to work on
	 * oldname is the old name for rename or null for delete or update/insert
	 * contents is the file to update/insert or null otherwise
	 * delete is true if file should be deleted, otherwise false.
	 * @param zip 
	 * @param oldname
	 * @param name
	 * @param contents
	 * @param delete
	 * @return the new zip archive as a byte array or null on error
	 */
    public static byte[] zipUpdate(byte[] zip, String name, String oldname, byte[] contents, boolean delete) {
        try {
            File temp = File.createTempFile("atf", ".zip");
            InputStream in = new BufferedInputStream(new ByteArrayInputStream(zip));
            OutputStream os = new BufferedOutputStream(new FileOutputStream(temp));
            ZipInputStream zin = new ZipInputStream(in);
            ZipOutputStream zout = new ZipOutputStream(os);
            ZipEntry e;
            ZipEntry e2;
            byte buffer[] = new byte[TEMP_FILE_BUFFER_SIZE];
            int bytesRead;
            boolean found = false;
            boolean rename = false;
            String oname = name;
            if (oldname != null) {
                name = oldname;
                rename = true;
            }
            while ((e = zin.getNextEntry()) != null) {
                if (!e.isDirectory()) {
                    String ename = e.getName();
                    if (delete && ename.equals(name)) continue;
                    e2 = new ZipEntry(rename ? oname : ename);
                    zout.putNextEntry(e2);
                    if (ename.equals(name)) {
                        found = true;
                        zout.write(contents);
                    } else {
                        while ((bytesRead = zin.read(buffer)) != -1) zout.write(buffer, 0, bytesRead);
                    }
                    zout.closeEntry();
                }
            }
            if (!found && !delete) {
                e = new ZipEntry(name);
                zout.putNextEntry(e);
                zout.write(contents);
                zout.closeEntry();
            }
            zin.close();
            zout.close();
            byte[] res = MLUtil.loadFileToBuffer(temp.getPath());
            temp.delete();
            return (res);
        } catch (FileNotFoundException e) {
            MLUtil.runtimeError(e, name);
        } catch (IOException e) {
            MLUtil.runtimeError(e, name);
        }
        return (null);
    }

    /**
	 * Add/update or delete file from zip archive
	 * zipfile is the name of the zip archive
	 * name is the name of the file inside the archive to work on
	 * oldname is the old namee for rename or null for delete or update/insert
	 * contents is the file to update/insert or null otherwise
	 * delete is true if file should be deleted, otherwise false.
	 *
	 * @param zipfile
	 * @param name
	 * @param oldname
	 * @param contents
	 * @param delete
	 * @return
	 */
    public static final boolean zipUpdate(String zipfile, String name, String oldname, byte[] contents, boolean delete) {
        try {
            File temp = File.createTempFile("atf", ".zip");
            InputStream in = new BufferedInputStream(new FileInputStream(zipfile));
            OutputStream os = new BufferedOutputStream(new FileOutputStream(temp));
            ZipInputStream zin = new ZipInputStream(in);
            ZipOutputStream zout = new ZipOutputStream(os);
            ZipEntry e;
            ZipEntry e2;
            byte buffer[] = new byte[TEMP_FILE_BUFFER_SIZE];
            int bytesRead;
            boolean found = false;
            boolean rename = false;
            String oname = name;
            if (oldname != null) {
                name = oldname;
                rename = true;
            }
            while ((e = zin.getNextEntry()) != null) {
                if (!e.isDirectory()) {
                    String ename = e.getName();
                    if (delete && ename.equals(name)) continue;
                    e2 = new ZipEntry(rename ? oname : ename);
                    zout.putNextEntry(e2);
                    if (ename.equals(name)) {
                        found = true;
                        zout.write(contents);
                    } else {
                        while ((bytesRead = zin.read(buffer)) != -1) zout.write(buffer, 0, bytesRead);
                    }
                    zout.closeEntry();
                }
            }
            if (!found && !delete) {
                e = new ZipEntry(name);
                zout.putNextEntry(e);
                zout.write(contents);
                zout.closeEntry();
            }
            zin.close();
            zout.close();
            File fp = new File(zipfile);
            fp.delete();
            MLUtil.copyFile(temp, fp);
            temp.delete();
            return (true);
        } catch (FileNotFoundException e) {
            MLUtil.runtimeError(e, "updateZip " + zipfile + " " + name);
        } catch (IOException e) {
            MLUtil.runtimeError(e, "updateZip " + zipfile + " " + name);
        }
        return (false);
    }
}
