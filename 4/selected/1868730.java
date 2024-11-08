package jcfs.webdav;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.CollectionResource;
import com.bradmcevoy.http.Range;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Request.Method;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.Map;
import jcfs.core.fs.RFile;
import jcfs.core.fs.RFileInputStream;
import org.apache.commons.io.IOUtils;

/**
 * access to a RFile
 * @author enrico
 */
public class RFileResource implements com.bradmcevoy.http.FileResource {

    private RFile file;

    RFileResource(RFile rFile) {
        this.file = rFile;
    }

    public void copyTo(CollectionResource cr, String string) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getUniqueId() {
        return file.getAbsolutePath();
    }

    public String getName() {
        return file.getName();
    }

    public Object authenticate(String string, String string1) {
        return null;
    }

    public boolean authorise(Request rqst, Method method, Auth auth) {
        return true;
    }

    public String getRealm() {
        return "JCFS";
    }

    public Date getModifiedDate() {
        return new java.util.Date();
    }

    public String checkRedirect(Request rqst) {
        return null;
    }

    public void delete() {
        file.delete();
    }

    public void sendContent(OutputStream out, Range range, Map map, String string) throws IOException, NotAuthorizedException, BadRequestException {
        System.out.println("sendContent " + file);
        RFileInputStream in = new RFileInputStream(file);
        try {
            IOUtils.copyLarge(in, out);
        } finally {
            in.close();
        }
    }

    public Long getMaxAgeSeconds(Auth auth) {
        return null;
    }

    public String getContentType(String string) {
        return null;
    }

    public Long getContentLength() {
        return null;
    }

    public void moveTo(CollectionResource cr, String string) throws ConflictException {
    }

    public String processForm(Map map, Map map1) throws BadRequestException, NotAuthorizedException {
        return null;
    }

    public Date getCreateDate() {
        return new java.util.Date();
    }
}
