package org.genos.gmf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.SQLException;
import javax.servlet.http.HttpServletResponse;
import org.genos.db.UniqueID;
import org.genos.utils.ContentTypes;
import org.genos.utils.FileUtils;

/**
 * Methods dealing with disk storage
 */
public class DiskStorage {

    /**
	 * Returns the directory for static files of the current user.
	 * If it not exists, this method also creates the directory.
	 * @param uid	User id
	 * @return		Path to the directory where we store generated files
	 */
    public static String getStaticDirectory(Integer uid) {
        String dir = Configuration.homeTemp + "static/" + uid;
        File d = new File(dir);
        if (!d.exists()) d.mkdirs();
        return dir;
    }

    /**
	 * Gets the directory where the file fileid will be stored.<br>
	 * With current directory policy, up to 100 files can be stored in every directory.
	 * @param fileid	File id.
	 * @return			Directory where the file will be located.
	 */
    static String getDirectory(Integer fileid) {
        String s = fileid.toString();
        return s.substring(s.length() - 2);
    }

    /**
	 * Saves a file to disk, returning an id for it.
	 * @param filecontents	Contents of the file
	 * @return				Id identifying the file in the storage system.
	 */
    public static int save(ByteBuffer filecontents) throws Exception {
        int id = UniqueID.getUniqueId(1);
        if (id == -1) {
            String message = "DiskStorage.save(): couldn't get an unique id for the file.";
            Configuration.logger.error(message);
            throw new Exception(message);
        }
        String prefix = getDirectory(new Integer(id));
        File dir = new File(Configuration.diskStorage + prefix);
        if (!dir.exists()) if (!dir.mkdirs()) {
            String message = "DiskStorage.save(): couldn't create storage directory " + prefix;
            Configuration.logger.error(message);
            throw new Exception(message);
        }
        String filename = Configuration.diskStorage + prefix + "/" + id;
        FileOutputStream fso = new FileOutputStream(filename);
        fso.write(filecontents.array());
        fso.close();
        return id;
    }

    /**
	 * Makes a copy of a file and returns the id of the copied file
	 * @param fileid	File to copy
	 * @return			File id of the copied file
	 */
    public static int copy(Integer fileid) throws Exception {
        String prefix = getDirectory(fileid);
        String filename = Configuration.diskStorage + prefix + "/" + fileid;
        int newid = UniqueID.getUniqueId(1);
        if (newid == -1) {
            String message = "DiskStorage.copy(): couldn't get an unique id for the file.";
            Configuration.logger.error(message);
            throw new Exception(message);
        }
        String newprefix = getDirectory(newid);
        File dir = new File(Configuration.diskStorage + newprefix);
        if (!dir.exists()) dir.mkdirs();
        String newfilename = Configuration.diskStorage + newprefix + "/" + newid;
        FileUtils.copyFile(filename, newfilename);
        return newid;
    }

    /**
	 * Deletes a file from disk.
	 * @param uid		User id.
	 * @param fileid	File id.
	 */
    public static void delete(Integer fileid) {
        String prefix = getDirectory(fileid);
        String filename = Configuration.diskStorage + prefix + "/" + fileid;
        File f = new File(filename);
        if (!f.delete()) Configuration.logger.warn("DiskStorage.delete(): couldn't delete file " + filename);
    }

    /**
	 * File download. Send a file as response.
	 * @param conn	Database connection.
	 * @param rid	File Resource id.
	 */
    public static void download(Connection conn, Integer fileid, String filename, HttpServletResponse response) throws SQLException, IOException, Exception {
        String prefix = getDirectory(fileid);
        String file = Configuration.diskStorage + prefix + "/" + fileid;
        FileInputStream fsi = new FileInputStream(file);
        response.setHeader("Content-disposition", "attachment; filename=" + filename.replaceAll(" ", "_"));
        response.setHeader("Content-Type", ContentTypes.getContentType(filename));
        try {
            OutputStream os = response.getOutputStream();
            byte[] content = new byte[8192];
            int n = 0;
            while ((n = fsi.read(content)) > 0) os.write(content, 0, n);
            os.flush();
        } catch (Exception e) {
        }
        fsi.close();
    }

    /**
	 * File download. Send a file as response.
	 * @param conn	Database connection.
	 * @param rid	File Resource id.
	 */
    public static void viewFile(Connection conn, String filename, HttpServletResponse response) throws SQLException, IOException, Exception {
        response.setHeader("Content-Type", ContentTypes.getContentType(filename));
        File f = new File(filename);
        if (!f.exists() || !f.isFile()) throw new IOException("DiskStorage.viewFile(): File not found: " + filename);
        response.setHeader("Content-disposition", "inline; filename=" + f.getName().replaceAll(" ", "_"));
        FileInputStream fis = new FileInputStream(f);
        OutputStream os = response.getOutputStream();
        byte[] content = new byte[8192];
        int n = 0;
        while ((n = fis.read(content)) > 0) os.write(content, 0, n);
        os.flush();
        fis.close();
        return;
    }

    /**
	 * Returns the size of a file in bytes.
	 * @param fileid	File id.
	 * @return			Size in bytes.
	 */
    public static long getFileSize(Integer fileid) {
        if (fileid == null) return 0;
        String prefix = getDirectory(fileid);
        String filename = Configuration.diskStorage + prefix + "/" + fileid;
        File f = new File(filename);
        return f.length();
    }

    /**
	 * Returns a string representation of a file size, adding it proper suffix (K-kbytes, M-Mbytes).
	 * @param s		File size in bytes.
	 * @return		File size in String.
	 */
    public static String getStringSize(long s) {
        if (s > 1024 * 1024) return String.valueOf(s / (1024 * 1024)) + " Mb";
        if (s > 1024) return String.valueOf(s / 1024) + " Kb";
        return String.valueOf(s) + " bytes";
    }
}
