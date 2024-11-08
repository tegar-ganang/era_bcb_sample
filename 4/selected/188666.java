package unibg.overencrypt.domain;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import unibg.overencrypt.server.ServerConfiguration;
import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.CollectionResource;
import com.bradmcevoy.http.CopyableResource;
import com.bradmcevoy.http.DeletableResource;
import com.bradmcevoy.http.GetableResource;
import com.bradmcevoy.http.LockInfo;
import com.bradmcevoy.http.LockResult;
import com.bradmcevoy.http.LockTimeout;
import com.bradmcevoy.http.LockToken;
import com.bradmcevoy.http.LockingCollectionResource;
import com.bradmcevoy.http.MakeCollectionableResource;
import com.bradmcevoy.http.MoveableResource;
import com.bradmcevoy.http.PropFindableResource;
import com.bradmcevoy.http.PutableResource;
import com.bradmcevoy.http.Range;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.XmlWriter;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;

/**
 * Manages the OverEncrypted folder.
 *
 * @author Flavio Giovarruscio & Riccardo Tribbia
 * @version 1.0
 */
public class OverEncryptedFolderResource extends OverEncryptedResource implements MakeCollectionableResource, PutableResource, CopyableResource, DeletableResource, MoveableResource, PropFindableResource, LockingCollectionResource, GetableResource {

    /** Logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(OverEncryptedFolderResource.class);

    /** The owner id. */
    protected int ownerId = 0;

    /**
	 * Instantiates a new over encrypted folder resource.
	 *
	 * @param host the hostname of the client
	 * @param factory the OverEncrypt resource factory
	 * @param folder the folder to generate
	 * @param ownerid the owner id
	 * @param isOwner true if user is the owner of the file
	 */
    public OverEncryptedFolderResource(String host, OverEncryptResourceFactory factory, File folder, int ownerid, boolean isOwner) {
        super(host, factory, folder, ownerid, isOwner);
        this.ownerId = ownerid;
        if (!realFile.exists()) throw new IllegalArgumentException("Directory does not exist: " + realFile.getAbsolutePath());
        if (!realFile.isDirectory()) throw new IllegalArgumentException("Is not a directory: " + realFile.getAbsolutePath());
    }

    public CollectionResource createCollection(String name) {
        File fnew = null;
        if (isOwner) {
            fnew = new File(realFile, name);
            boolean ok = fnew.mkdir();
            if (!ok) throw new RuntimeException("Failed to create: " + fnew.getAbsolutePath());
        } else {
            throw new RuntimeException("Create new folder in this is not permitted because user isn't its owner.");
        }
        return (CollectionResource) new OverEncryptedFolderResource(host, factory, new File(realFile, name), ownerId, isOwner);
    }

    public Resource child(String name) {
        File fchild = new File(realFile, name);
        return factory.resolveFile(this.host, fchild);
    }

    public List<? extends Resource> getChildren() {
        ArrayList<Resource> list = new ArrayList<Resource>();
        File[] files = this.realFile.listFiles();
        if (files != null) {
            for (File fchild : files) {
                Resource res = factory.resolveFile(this.host, fchild);
                if (res != null) {
                    list.add(res);
                } else {
                    LOGGER.error("Couldn't resolve file - " + fchild.getAbsolutePath());
                }
            }
        }
        return list;
    }

    /**
	 * Will redirect if a default page has been specified on the factory.
	 *
	 * @param request the request
	 * @return the string
	 */
    public String checkRedirect(Request request) {
        if (factory.getDefaultPage() != null) {
            return request.getAbsoluteUrl() + "/" + factory.getDefaultPage();
        } else {
            return null;
        }
    }

    public Resource createNew(String name, InputStream in, Long length, String contentType) throws IOException {
        File dest = new File(this.getRealFile(), name);
        LOGGER.debug("PUT?? - real file: " + this.getRealFile() + ",name: " + name);
        if (isOwner) {
            if (!".request".equals(name) && !".tokens".equals(name)) {
                FileOutputStream out = null;
                try {
                    out = new FileOutputStream(dest);
                    IOUtils.copy(in, out);
                } finally {
                    IOUtils.closeQuietly(out);
                }
            } else {
                if (ServerConfiguration.isDynamicSEL()) {
                } else {
                }
                FileOutputStream out = null;
                try {
                    out = new FileOutputStream(dest);
                    IOUtils.copy(in, out);
                } finally {
                    IOUtils.closeQuietly(out);
                }
            }
            return factory.resolveFile(this.host, dest);
        } else {
            LOGGER.error("User isn't owner of this folder");
            return null;
        }
    }

    @Override
    protected void doCopy(File dest) {
        try {
            FileUtils.copyDirectory(this.getRealFile(), dest);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to copy to:" + dest.getAbsolutePath(), ex);
        }
    }

    public LockToken createAndLock(String name, LockTimeout timeout, LockInfo lockInfo) throws NotAuthorizedException {
        File dest = new File(this.getRealFile(), name);
        createEmptyFile(dest);
        OverEncryptedFileResource newRes = new OverEncryptedFileResource(host, factory, dest, ownerId, true, false);
        LockResult res = newRes.lock(timeout, lockInfo);
        return res.getLockToken();
    }

    /**
	 * Creates an empty file.
	 *
	 * @param file the file created
	 */
    private void createEmptyFile(File file) {
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(fout);
        }
    }

    /**
	 * Will generate a listing of the contents of this directory,
	 * unless the factory's allowDirectoryBrowsing has been set to false.
	 * 
	 * If so it will just output a message saying that access has been disabled.
	 *
	 * @param out the output stream of the files
	 * @param range the range
	 * @param params the parameters
	 * @param contentType the content type
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws NotAuthorizedException the not authorized exception
	 */
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException {
        String subpath = getRealFile().getCanonicalPath().substring(factory.getRoot().getCanonicalPath().length()).replace('\\', '/');
        String uri = "/" + factory.getContextPath() + subpath;
        XmlWriter w = new XmlWriter(out);
        w.open("html");
        w.open("body");
        w.begin("h1").open().writeText(this.getName()).close();
        w.open("table");
        for (Resource r : getChildren()) {
            w.open("tr");
            w.open("td");
            w.begin("a").writeAtt("href", uri + "/" + r.getName()).open().writeText(r.getName()).close();
            w.close("td");
            w.begin("td").open().writeText(r.getModifiedDate() + "").close();
            w.close("tr");
        }
        w.close("table");
        w.close("body");
        w.close("html");
        w.flush();
    }

    public Long getMaxAgeSeconds(Auth auth) {
        return null;
    }

    public String getContentType(String accepts) {
        return "text/html";
    }

    public Long getContentLength() {
        return null;
    }
}
