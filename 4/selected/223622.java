package easyaccept.util.file.jbfs.storages;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import easyaccept.util.file.FileHelper;
import easyaccept.util.file.jbfs.FileStorage;

public class LocalSystemFileStorage implements FileStorage {

    /**
	 * Byte buffer size
	 */
    public static final int BYTE_BUFFER_SIZE = 256;

    private String storeName = "";

    private String rootDir;

    public void createNewFile(String filePath, InputStream in) throws IOException {
        FileOutputStream out = null;
        try {
            File file = newFileRef(filePath);
            FileHelper.createNewFile(file, true);
            out = new FileOutputStream(file);
            IOUtils.copy(in, out);
        } finally {
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(out);
        }
    }

    public void createNewFile(String filePath, byte[] data) throws IOException {
        FileUtils.writeByteArrayToFile(newFileRef(filePath), data);
    }

    public void delete(String path) throws IOException {
        FileUtils.forceDelete(newFileRef(path));
    }

    public void deleteQuietly(String path) {
        FileUtils.deleteQuietly(newFileRef(path));
    }

    public boolean exists(String path) {
        return FileHelper.exists(getRootDir() + path);
    }

    public OutputStream getAsStream(String filePath) throws IOException {
        return FileUtils.openOutputStream(newFileRef(filePath));
    }

    public String getAsString(String filePath) throws IOException {
        return FileUtils.readFileToString(newFileRef(filePath));
    }

    public byte[] getBytes(String filePath) throws IOException {
        return FileUtils.readFileToByteArray(newFileRef(filePath));
    }

    @SuppressWarnings("unchecked")
    public Collection listFiles(String path) {
        return FileUtils.listFiles(newFileRef(path), FileFilterUtils.fileFileFilter(), null);
    }

    @SuppressWarnings("unchecked")
    public Collection listDirs(String path) {
        return FileUtils.listFiles(newFileRef(path), FileFilterUtils.notFileFilter(FileFilterUtils.fileFileFilter()), FileFilterUtils.directoryFileFilter());
    }

    public void mkdir(String path) throws IOException {
        FileUtils.forceMkdir(newFileRef(path));
    }

    public void renameFile(String newName, String oldName) throws IOException {
        FileUtils.moveFile(newFileRef(oldName), newFileRef(newName));
    }

    public void renameDir(String newName, String oldName) throws IOException {
        FileUtils.moveDirectoryToDirectory(newFileRef(oldName), newFileRef(newName), true);
    }

    /**
	 * Creates a file with root relative path
	 * 
	 * @param name
	 * @return
	 */
    public File newFileRef(String path) {
        return new File(getRootDir() + path);
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public String getRootDir() {
        return rootDir;
    }

    public void setRootDir(String rootDir) {
        this.rootDir = rootDir;
    }
}
