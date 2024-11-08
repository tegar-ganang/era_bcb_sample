package net.sf.clairv.index.resource;

import java.io.IOException;
import java.net.SocketException;
import java.util.Iterator;
import java.util.LinkedList;
import net.sf.clairv.index.document.Document;
import net.sf.clairv.index.document.DocumentFactory;
import net.sf.clairv.index.document.IndexOption;
import net.sf.clairv.index.document.StoreOption;
import net.sf.clairv.index.processor.DocumentHolder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

/**
 * Extracts documents from a remote FTP site.
 * 
 * @author qiuyin
 * 
 */
public class FtpResource extends AbstractResource {

    private static final Log log = LogFactory.getLog(FtpResource.class);

    protected String site, path = "/", user = "anonymous", password = "";

    protected int port = 0;

    private int fileCount = 0;

    public void setSite(String site) {
        this.site = site;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setPort(int port) {
        if (port > 0 && port < 65535) {
            this.port = port;
        }
    }

    public int extractDocumentsInternal(DocumentHolder holder, DocumentFactory docFactory) {
        FTPClient client = new FTPClient();
        try {
            client.connect(site, port == 0 ? 21 : port);
            client.login(user, password);
            visitDirectory(client, "", path, holder, docFactory);
            client.disconnect();
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
        }
        return fileCount;
    }

    protected void visitDirectory(FTPClient client, String prefix, String dir, DocumentHolder holder, DocumentFactory docFactory) {
        FTPFile[] children = null;
        try {
            client.changeWorkingDirectory(dir);
            children = client.listFiles();
        } catch (IOException e) {
            log.warn("Cannot change working directory to: " + dir);
            return;
        }
        if (children == null) {
            return;
        }
        LinkedList directories = new LinkedList();
        for (int i = 0; i < children.length; i++) {
            FTPFile child = children[i];
            if (child.isDirectory()) {
                directories.add(child.getName());
            } else if (child.isFile()) {
                Document doc = docFactory.createDocument();
                doc.addField("fileName", child.getName(), StoreOption.YES, IndexOption.TOKENIZED);
                doc.addField("fileSize", (child.getSize() / 1024) + "KB", StoreOption.YES, IndexOption.NO);
                doc.addField("filePath", prefix + "/" + child.getName(), StoreOption.YES, IndexOption.NO);
                fileCount++;
                holder.addDocument(doc);
            } else {
                log.warn("Discarded file: " + child.getName());
            }
        }
        for (Iterator itr = directories.iterator(); itr.hasNext(); ) {
            String f = (String) itr.next();
            visitDirectory(client, prefix + "/" + f, f, holder, docFactory);
            try {
                client.changeToParentDirectory();
            } catch (IOException e) {
                log.warn("Changing to parent directory failed.");
            }
        }
    }
}
