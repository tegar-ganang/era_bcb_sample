package n2hell.fs;

import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;
import n2hell.config.SshConfig;
import n2hell.xmlrpc.SshFactory;
import com.trilead.ssh2.Connection;
import com.trilead.ssh2.SFTPv3Client;
import com.trilead.ssh2.SFTPv3DirectoryEntry;
import com.trilead.ssh2.SFTPv3FileAttributes;

public class FileSystemSftp implements IFileSystem {

    private SFTPv3Client channel = null;

    private Connection connection;

    private static MergeSort fileMS = new MergeSort();

    private final String[] roots;

    public FileSystemSftp(SshConfig ssh, String[] roots) throws IOException {
        if (roots == null || roots.length == 0) this.roots = new String[] { "/" }; else this.roots = roots;
        connection = SshFactory.getConnection(ssh);
    }

    public FileSystemItem[] list(String path, Boolean includeFiles) throws IOException {
        SFTPv3Client channel = getChannel();
        SFTPv3FileAttributes stat = channel.lstat(path);
        if (stat.isDirectory()) return getChildren(path, includeFiles);
        return null;
    }

    @SuppressWarnings("unchecked")
    public FileSystemItem[] getRoots() throws IOException {
        FileSystemItem[] res = new FileSystemItem[roots.length];
        for (int i = 0; i < roots.length; i++) {
            String id = roots[i];
            res[i] = new FileSystemItem(id, id, true);
        }
        return res;
    }

    @SuppressWarnings("unchecked")
    private FileSystemItem[] getChildren(String path, Boolean includeFiles) throws IOException {
        SFTPv3Client channel = getChannel();
        Vector ls = channel.ls(path);
        Vector<FileSystemItem> res = new Vector<FileSystemItem>();
        for (Iterator iterator = ls.iterator(); iterator.hasNext(); ) {
            SFTPv3DirectoryEntry entry = (SFTPv3DirectoryEntry) iterator.next();
            String name = (String) entry.filename;
            String id = (path.equals("/") ? "" : path) + "/" + name;
            if (!name.equals("..") && !name.equals(".") && (includeFiles || entry.attributes.isDirectory())) {
                FileSystemItem item = new FileSystemItem(id, name, entry.attributes.isDirectory());
                res.add(item);
            }
        }
        FileSystemItem[] arr = res.toArray(new FileSystemItem[res.size()]);
        fileMS.sort(arr);
        return arr;
    }

    private SFTPv3Client getChannel() throws IOException {
        if (channel == null) channel = new SFTPv3Client(connection);
        return channel;
    }
}
