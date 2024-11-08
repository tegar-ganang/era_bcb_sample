package unibg.overencrypt.domain;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.GregorianCalendar;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import unibg.overencrypt.server.ServerConfiguration;
import com.bradmcevoy.common.ContentTypeUtils;
import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.CopyableResource;
import com.bradmcevoy.http.DeletableResource;
import com.bradmcevoy.http.GetableResource;
import com.bradmcevoy.http.MoveableResource;
import com.bradmcevoy.http.PropFindableResource;
import com.bradmcevoy.http.Range;
import com.bradmcevoy.http.webdav.PropPatchHandler.Fields;

/**
 * Manages the OverEncrypted file.
 *
 * @author Flavio Giovarruscio & Riccardo Tribbia
 * @version 1.0
 */
public class OverEncryptedFileResource extends OverEncryptedResource implements CopyableResource, DeletableResource, GetableResource, MoveableResource, PropFindableResource {

    /** Logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(OverEncryptedFileResource.class);

    /** Verify if the client is allowed. */
    private boolean isAllowedClient = false;

    /**
	 * Instantiates a new over encrypted file resource.
	 *
	 * @param host the hostname of the client
	 * @param factory the OverEncrypt resource factory
	 * @param file the file to generate
	 * @param ownerid the owner id
	 * @param isOwner true if user is the owner of the file
	 * @param isAllowedClient true if client is allowed
	 */
    public OverEncryptedFileResource(String host, OverEncryptResourceFactory factory, File file, int ownerid, boolean isOwner, boolean isAllowedClient) {
        super(host, factory, file, ownerid, isOwner);
        this.isAllowedClient = isAllowedClient;
    }

    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException {
        LOGGER.debug("DOWNLOAD - Send content: " + realFile.getAbsolutePath());
        LOGGER.debug("Output stream: " + out.toString());
        if (ServerConfiguration.isDynamicSEL()) {
            LOGGER.error("IS DINAMIC SEL????");
        } else {
        }
        if (".tokens".equals(realFile.getName()) || ".response".equals(realFile.getName()) || ".request".equals(realFile.getName()) || isAllowedClient) {
            FileInputStream in = null;
            try {
                in = new FileInputStream(realFile);
                int bytes = IOUtils.copy(in, out);
                LOGGER.debug("System resource or Allowed Client wrote bytes:  " + bytes);
                out.flush();
            } catch (Exception e) {
                LOGGER.error("Error while downloading over encryption system " + realFile.getName() + " file", e);
            } finally {
                IOUtils.closeQuietly(in);
            }
        } else {
        }
    }

    public Long getMaxAgeSeconds(Auth auth) {
        return new GregorianCalendar().getTimeInMillis();
    }

    /**
	 * Executes the copy of the file.
	 *
	 * @param dest the destination of copy
	 * @{@inheritDoc}
	 */
    @Override
    protected void doCopy(File dest) {
        try {
            FileUtils.copyFile(realFile, dest);
        } catch (IOException ex) {
            throw new RuntimeException("Failed doing copy to: " + dest.getAbsolutePath(), ex);
        }
    }

    public Long getContentLength() {
        return realFile.length();
    }

    /**
	 * Sets the properties.
	 *
	 * @param fields the new properties
	 */
    @Deprecated
    public void setProperties(Fields fields) {
    }

    public String getContentType(String preferredList) {
        String mime = ContentTypeUtils.findContentTypes(this.realFile);
        return ContentTypeUtils.findAcceptableContentType(mime, preferredList);
    }
}
