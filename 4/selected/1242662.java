package unibg.overencrypt.domain;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;
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
import com.bradmcevoy.http.PropPatchableResource;
import com.bradmcevoy.http.Range;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import com.bradmcevoy.http.webdav.PropPatchHandler.Fields;

/**
 *
 * @author Flavio Giovarruscio & Riccardo Tribbia
 * @version 1.0
 */
public class OverEncryptedFriendsFile extends OverEncryptedFriendsResource implements CopyableResource, DeletableResource, GetableResource, MoveableResource, PropFindableResource, PropPatchableResource {

    private static final Logger LOGGER = Logger.getLogger(OverEncryptedFriendsFile.class);

    private boolean isAllowedClient;

    public OverEncryptedFriendsFile(OverEncryptResourceFactory factory, String path, boolean isAllowedClient) {
        super(factory, isAllowedClient);
        this.realFile = new File(ServerConfiguration.getWebDAVrootPath() + "/" + path);
    }

    @Override
    public Date getCreateDate() {
        return null;
    }

    @Override
    public Long getContentLength() {
        return realFile.length();
    }

    @Override
    public String getContentType(String preferredList) {
        String mime = ContentTypeUtils.findContentTypes(this.realFile);
        return ContentTypeUtils.findAcceptableContentType(mime, preferredList);
    }

    @Override
    public Long getMaxAgeSeconds(Auth auth) {
        return new GregorianCalendar().getTimeInMillis();
    }

    @Override
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
                LOGGER.error("Error while uploading over encryption system " + realFile.getName() + " file", e);
            } finally {
                IOUtils.closeQuietly(in);
            }
        } else {
            FileInputStream in = null;
            try {
                in = new FileInputStream(realFile);
                int bytes = IOUtils.copy(in, out);
                LOGGER.debug("System resource or Allowed Client wrote bytes:  " + bytes);
                out.flush();
            } catch (Exception e) {
                LOGGER.error("Error while uploading over encryption system " + realFile.getName() + " file", e);
            } finally {
                IOUtils.closeQuietly(in);
            }
        }
    }

    /**
	 * Sets the properties.
	 *
	 * @param fields the new properties
	 */
    @Deprecated
    public void setProperties(Fields fields) {
    }

    @Override
    public void delete() throws NotAuthorizedException, ConflictException, BadRequestException {
        boolean ok = false;
        if (allowedClient) ok = this.realFile.delete();
        if (!ok) throw new RuntimeException("Failed to delete");
    }
}
