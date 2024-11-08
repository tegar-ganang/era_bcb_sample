package org.kablink.teaming.webdav;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Map;
import javax.activation.MimetypesFileTypeMap;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kablink.teaming.domain.DefinableEntity;
import org.kablink.teaming.domain.FileAttachment;
import org.kablink.teaming.domain.NoFileByTheIdException;
import org.kablink.teaming.domain.VersionAttachment;
import org.kablink.teaming.module.file.FileIndexData;
import com.bradmcevoy.common.ContentTypeUtils;
import com.bradmcevoy.http.DeletableResource;
import com.bradmcevoy.http.GetableResource;
import com.bradmcevoy.http.PropFindableResource;
import com.bradmcevoy.http.Range;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import com.bradmcevoy.http.exceptions.NotFoundException;
import com.bradmcevoy.http.http11.PartialGetHelper;
import com.bradmcevoy.io.ReadingException;
import com.bradmcevoy.io.WritingException;

/**
 * @author jong
 *
 */
public class FileResource extends WebdavResource implements PropFindableResource, GetableResource, DeletableResource {

    private static final Log logger = LogFactory.getLog(FileResource.class);

    private String name;

    private String id;

    private Date createdDate;

    private Date modifiedDate;

    private String webdavPath;

    private FileAttachment fa;

    private FileResource(WebdavResourceFactory factory, String webdavPath, String name, String id, Date createdDate, Date modifiedDate) {
        super(factory);
        this.webdavPath = webdavPath;
        this.name = name;
        this.id = id;
        this.createdDate = createdDate;
        this.modifiedDate = modifiedDate;
    }

    public FileResource(WebdavResourceFactory factory, String webdavPath, FileAttachment fa) {
        this(factory, webdavPath, fa.getFileItem().getName(), fa.getId(), fa.getCreation().getDate(), fa.getModification().getDate());
        this.fa = fa;
    }

    public FileResource(WebdavResourceFactory factory, String webdavPath, FileIndexData fid) {
        this(factory, webdavPath, fid.getName(), fid.getId(), fid.getCreatedDate(), fid.getModifiedDate());
    }

    @Override
    public String getUniqueId() {
        return "fa:" + id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Date getModifiedDate() {
        return modifiedDate;
    }

    @Override
    public Date getCreateDate() {
        return createdDate;
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        try {
            resolveFileAttachment();
        } catch (NoFileByTheIdException e) {
            throw new NotFoundException(e.getLocalizedMessage());
        }
        DefinableEntity owningEntity = fa.getOwner().getEntity();
        InputStream in = getFileModule().readFile(owningEntity.getParentBinder(), owningEntity, fa);
        try {
            if (range != null) {
                if (logger.isDebugEnabled()) logger.debug("sendContent: ranged content: " + toString(fa));
                PartialGetHelper.writeRange(in, range, out);
            } else {
                if (logger.isDebugEnabled()) logger.debug("sendContent: send whole file " + toString(fa));
                IOUtils.copy(in, out);
            }
            out.flush();
        } catch (ReadingException e) {
            throw new IOException(e);
        } catch (WritingException e) {
            throw new IOException(e);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    @Override
    public String getContentType(String accepts) {
        String mime = ContentTypeUtils.findContentTypes(name);
        String s = ContentTypeUtils.findAcceptableContentType(mime, accepts);
        if (logger.isTraceEnabled()) logger.trace("getContentType: preferred: " + accepts + " mime: " + mime + " selected: " + s);
        return s;
    }

    @Override
    public Long getContentLength() {
        return null;
    }

    public String getWebdavPath() {
        return webdavPath;
    }

    private FileAttachment resolveFileAttachment() throws NoFileByTheIdException {
        if (fa == null) {
            fa = (FileAttachment) getCoreDao().load(FileAttachment.class, id);
            if (fa == null) throw new NoFileByTheIdException(id); else if (fa instanceof VersionAttachment) throw new NoFileByTheIdException(id, "The specified file id represents a file version rather than a file");
        }
        return fa;
    }

    private String toString(FileAttachment fa) {
        return new StringBuilder().append("[").append(fa.getFileItem().getName()).append(":").append(fa.getId()).append("]").toString();
    }

    @Override
    public void delete() throws NotAuthorizedException, ConflictException, BadRequestException {
    }
}
