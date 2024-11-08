package unibg.overencrypt.domain;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import unibg.overencrypt.server.ServerConfiguration;
import com.bradmcevoy.common.ContentTypeUtils;
import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.CollectionResource;
import com.bradmcevoy.http.CopyableResource;
import com.bradmcevoy.http.DeletableResource;
import com.bradmcevoy.http.GetableResource;
import com.bradmcevoy.http.MoveableResource;
import com.bradmcevoy.http.PropFindableResource;
import com.bradmcevoy.http.PropPatchableResource;
import com.bradmcevoy.http.Range;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.Request.Method;
import com.bradmcevoy.http.http11.auth.DigestResponse;
import com.bradmcevoy.http.webdav.PropPatchHandler.Fields;

/**
 * The Class PlainSimpleResource.
 *
 * @author Flavio Giovarruscio & Riccardo Tribbia
 * @version 1.0
 */
public class PlainSimpleResource implements Resource, CopyableResource, DeletableResource, GetableResource, MoveableResource, PropFindableResource, PropPatchableResource {

    /** Logger for this class. */
    private static Logger LOGGER = org.apache.log4j.Logger.getLogger(OverEncryptedResource.class);

    /** The file. */
    private File file;

    /**
	 * Instantiates a new plain simple resource.
	 *
	 * @param file the file
	 */
    public PlainSimpleResource(File file) {
        this.file = file;
    }

    /**
	 * Gets the real file.
	 *
	 * @return the real file
	 */
    public File getRealFile() {
        return this.file;
    }

    public void moveTo(CollectionResource newParent, String newName) {
        File dest = new File(newParent.getName(), newName);
        boolean ok = this.file.renameTo(dest);
        if (!ok) throw new RuntimeException("Failed to move to: " + dest.getAbsolutePath());
        this.file = dest;
    }

    public Date getCreateDate() {
        return null;
    }

    public void copyTo(CollectionResource newParent, String newName) {
        File dest = new File(newParent.getName(), newName);
        try {
            FileUtils.copyFile(file, dest);
        } catch (IOException ex) {
            throw new RuntimeException("Failed doing copy to: " + dest.getAbsolutePath(), ex);
        }
    }

    public void delete() {
        boolean ok = file.delete();
        if (!ok) throw new RuntimeException("Failed to delete");
    }

    public Object authenticate(String user, String password) {
        return user;
    }

    /**
	 * Authenticate.
	 *
	 * @param digestRequest the digest request
	 * @return the object
	 */
    public Object authenticate(DigestResponse digestRequest) {
        return digestRequest;
    }

    /**
	 * Checks if is digest allowed.
	 *
	 * @return true, if is digest allowed
	 */
    public boolean isDigestAllowed() {
        return true;
    }

    public boolean authorise(Request request, Method method, Auth auth) {
        return true;
    }

    @Override
    public String checkRedirect(Request arg0) {
        LOGGER.debug("CHECK REDIRECT CALLED");
        return null;
    }

    @Override
    public Date getModifiedDate() {
        return new Date(file.lastModified());
    }

    @Override
    public String getName() {
        return file.getName();
    }

    @Override
    public String getRealm() {
        return ServerConfiguration.getREALM();
    }

    @Override
    public String getUniqueId() {
        return String.valueOf(file.hashCode());
    }

    /**
	 * Compare two resources.
	 *
	 * @param o the resource to be compared
	 * @return the value 0 if the argument resource is equal to this resource; a value less than 0 if this resource is lexicographically less than the resource argument; and a value greater than 0 if this resource is lexicographically greater than the resource argument.
	 */
    public int compareTo(Resource o) {
        return this.getName().compareTo(o.getName());
    }

    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException {
        LOGGER.debug("GET REQUEST OR RESPONSE - Send content: " + file.getAbsolutePath());
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            int bytes = IOUtils.copy(in, out);
            LOGGER.debug("wrote bytes:  " + bytes);
            out.flush();
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    public Long getMaxAgeSeconds(Auth auth) {
        return new GregorianCalendar().getTimeInMillis();
    }

    public Long getContentLength() {
        return file.length();
    }

    public String getContentType(String preferredList) {
        String mime = ContentTypeUtils.findContentTypes(this.file);
        return ContentTypeUtils.findAcceptableContentType(mime, preferredList);
    }

    @Override
    public void setProperties(Fields arg0) {
    }
}
