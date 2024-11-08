package com.vo.universalworker.storage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileLock;
import org.restlet.data.Status;
import org.restlet.ext.wadl.WadlServerResource;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;

/**
 * Resource that handles the system cache.
 *
 * @author Jean-Christophe Malapert
 */
public class StoreObject extends WadlServerResource {

    private StorageManagement app;

    private static final String cachedFileName = "save.xml";

    @Override
    protected void doInit() throws ResourceException {
        super.doInit();
        this.app = (StorageManagement) getApplication();
    }

    /**
     * Jobs caching. This methods handles conccurent access by the use of FileLock object
     * @param object XML representation of the cache
     */
    @Post
    public void acceptObject(String object) {
        FileOutputStream fout = null;
        FileLock lock = null;
        try {
            fout = new FileOutputStream(this.app.getStorageDirectory() + File.separator + cachedFileName);
            lock = fout.getChannel().lock();
            fout.write(object.getBytes());
        } catch (FileNotFoundException ex) {
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, ex);
        } catch (IOException ex) {
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, ex);
        } finally {
            try {
                fout.close();
                lock.release();
            } catch (IOException ex) {
            }
        }
    }
}
