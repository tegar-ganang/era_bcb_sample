package org.speech.asr.gui.dao.jcr.resource;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.speech.asr.gui.constant.JcrConstants;
import org.speech.asr.common.jcr.JcrResource;
import org.springmodules.jcr.JcrCallback;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.io.InputStream;

/**
 * //@todo class description
 * <p/>
 * Creation date: May 17, 2009 <br/>
 * <a href="http://www.grapesoftware.com">www.grapesoftware.com</a>
 *
 * @author Lukasz Olczak
 */
public class LoadJcrResourceCallback implements JcrCallback {

    /**
   * slf4j Logger.
   */
    private static final Logger log = LoggerFactory.getLogger(LoadJcrResourceCallback.class.getName());

    private static final int BUF_SIZE = 1000000;

    private static final int HDD_SECTOR_SIZE = 512;

    private JcrResource jcrResource;

    public LoadJcrResourceCallback(JcrResource jcrResource) {
        this.jcrResource = jcrResource;
    }

    public Object doInJcr(Session session) throws IOException, RepositoryException {
        Node resourceNode = session.getNodeByUUID(jcrResource.getUuid());
        InputStream in = resourceNode.getProperty(JcrConstants.JCR_DATA_PROPERTY).getStream();
        ByteArrayOutputStream out = new ByteArrayOutputStream(BUF_SIZE);
        try {
            byte[] buf = new byte[HDD_SECTOR_SIZE];
            int read = 0;
            while ((read = in.read(buf)) != -1) {
                out.write(buf, 0, read);
            }
        } finally {
            in.close();
            out.close();
        }
        return out.toByteArray();
    }
}
