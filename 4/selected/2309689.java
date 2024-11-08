package icreate.mans;

import java.io.*;
import java.net.*;

public class FileManager {

    /** Holds value of property source
     */
    private URL source;

    /** Holds value of property dest
     */
    private URL dest;

    /** Creates a new instance of Object
     */
    public FileManager() {
    }

    /** Getter for property dest
     * @return Value of property dest
     */
    public java.net.URL getDest() {
        return dest;
    }

    /** Setter for property dest.
     * @param dest New value of property dest.
     */
    public void setDest(java.net.URL dest) {
        this.dest = dest;
    }

    /** Getter for property source.
     * @return Value of property source.
     */
    public java.net.URL getSource() {
        return source;
    }

    /** Setter for property source.
     * @param source New value of property source.
     */
    public void setSource(java.net.URL source) {
        this.source = source;
    }

    /** Delete file
     * @param filename path of file
     * @throws FileManagerException If unable to delete file
     */
    public void deleteFile(String filename) throws FileManagerException {
        File delFile = new File(filename);
        if (delFile.exists()) {
            if (!delFile.delete()) throw new FileManagerException("Deleting of file " + delFile.getAbsolutePath() + " failed");
        }
    }

    /** Deletes file specified in property source
     * @throws FileManagerException if unable to delete file
     */
    public void deleteFile() throws FileManagerException {
        File delFile = new File(this.source.getPath());
        if (delFile.exists()) {
            if (!delFile.delete()) throw new FileManagerException("Unable to delete file");
        }
    }

    public void deleteFolder(String path, boolean withFiles) throws FileManagerException {
        File delFolder = new File(path);
        if (delFolder.exists()) {
            File[] files = delFolder.listFiles();
            if (withFiles) {
                for (int i = 0; i < files.length; i++) {
                    if (files[i].isDirectory()) {
                        deleteFolder(files[i].getAbsolutePath(), true);
                    } else {
                        files[i].delete();
                    }
                }
                if (!delFolder.delete()) throw new FileManagerException("Delete folder failed: " + delFolder.getAbsolutePath());
            } else {
                if (files.length == 0) {
                    if (!delFolder.delete()) throw new FileManagerException("Delete folder failed: " + delFolder.getAbsolutePath());
                }
            }
        } else {
            return;
        }
    }

    /** General copy file method.  source property and dest properties are used.
     * @throws FileManagerException if the copy fails
     */
    public void copyFile() throws FileManagerException {
        try {
            copyFile(this.source, this.dest);
        } catch (Exception e) {
            e.printStackTrace();
            throw new FileManagerException("Copy file failed");
        }
    }

    public void copyFile(URL s, URL d) throws FileManagerException {
        try {
            copyFile(s.getPath(), d.getPath());
        } catch (Exception e) {
            throw new FileManagerException("Copy File failed");
        }
    }

    public void copyFile(String s, String d) throws FileManagerException {
        try {
            copyFile(new File(s), new File(d));
        } catch (Exception e) {
            throw new FileManagerException("Copy File failed");
        }
    }

    public void copyFile(File s, File d) throws FileManagerException {
        try {
            InputStream in = new FileInputStream(s);
            OutputStream out = new FileOutputStream(d);
            copyFile(in, out);
        } catch (Exception e) {
            e.printStackTrace();
            throw new FileManagerException("Copy File failed");
        }
    }

    private void copyFile(InputStream in, OutputStream out) throws FileManagerException {
        try {
            byte[] buffer = new byte[4096];
            int bytes_read;
            while ((bytes_read = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytes_read);
            }
            in.close();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw new FileManagerException("Copy file failed");
        }
    }

    public void copyFolder() throws FileManagerException {
        copyFolder(new File(this.source.getPath()), new File(this.dest.getPath()));
    }

    public void copyFolder(File o_folder, File n_folder) throws FileManagerException {
        if (o_folder.isDirectory()) {
            if (!n_folder.exists()) makeDir(n_folder.getAbsolutePath());
            File[] o_files = o_folder.listFiles();
            try {
                for (int i = 0; i < o_files.length; i++) {
                    if (o_files[i].isDirectory()) {
                        makeDir(n_folder.getAbsolutePath() + n_folder.separator + o_files[i].getName());
                        copyFolder(o_files[i], new File(n_folder.getAbsolutePath() + n_folder.separator + o_files[i].getName()));
                    } else {
                        copyFile(new FileInputStream(o_files[i].getAbsolutePath()), new FileOutputStream(n_folder.getPath() + n_folder.separator + o_files[i].getName()));
                    }
                }
            } catch (FileNotFoundException fnfe) {
                throw new FileManagerException("File not found");
            }
        }
    }

    public void copyFolder(String in_path, String out_path) throws FileManagerException {
        copyFolder(new File(in_path), new File(out_path));
    }

    /** Make new directory
     * @param dirs path of new directory
     * @throws FileManagerException If unable to create new directory
     */
    public void makeDir(String dirs) throws FileManagerException {
        File mDir = new File(dirs);
        if (!mDir.exists()) {
            if (!mDir.mkdirs()) throw new FileManagerException("Unable to create folder");
        }
    }

    public boolean exists(String path) {
        File f_path = new File(path);
        return f_path.exists();
    }

    /** Create temporary file
     * @param dir path
     * @throws IOException io problem
     * @return Temporary file with .tmp as the extension
     */
    public File tempFile(String dir) throws IOException {
        File temp_dir = new File(dir);
        return File.createTempFile("temp_comp", ".tmp", temp_dir);
    }

    public File tempFile() throws FileManagerException {
        try {
            return tempFile(getSource().getPath());
        } catch (IOException ioe) {
            throw new FileManagerException("Unable to temp file on server");
        }
    }

    public File tempFile(java.net.URL url) throws FileManagerException {
        try {
            return tempFile(url.getPath());
        } catch (IOException ioe) {
            throw new FileManagerException("Unable to temp file on server");
        }
    }
}
