package com.germinus.xpression.cms.contents.binary;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import com.germinus.xpression.cms.jcr.JCRUtil;

/**
 * @author agonzalez
 *
 */
public class JCRBinaryDataHolder implements BinaryDataHolder {

    private String _id;

    private String workspace;

    /**
     * @param string 
     * 
     */
    public JCRBinaryDataHolder(String id, String workspace) {
        super();
        _id = id;
        this.workspace = workspace;
    }

    public String getId() {
        return _id;
    }

    public void setId(String id) {
        this._id = id;
    }

    public InputStream getInputStream() throws IOException {
        Node contentNode = getResourceNode();
        try {
            return contentNode.getProperty(JCRUtil.JCR_DATA_PREFIX).getBinary().getStream();
        } catch (ValueFormatException e) {
            throw new IOException("Property has not expected nodeType (Stream): " + e.getMessage());
        } catch (PathNotFoundException e) {
            throw new IOException("Content node does not contains expected property: " + e.getMessage());
        } catch (RepositoryException e) {
            throw new IOException("Error obtaining binary data: " + e.getMessage());
        }
    }

    public byte[] getData() throws IOException {
        InputStream stream = getInputStream();
        byte[] buffer = new byte[4096];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int read;
        while ((read = stream.read(buffer)) > 0) {
            baos.write(buffer, 0, read);
        }
        return baos.toByteArray();
    }

    public String getEncoding() {
        try {
            Node resourceNode = getResourceNode();
            return resourceNode.getProperty(JCRUtil.JCR_ENCODING_PREFIX).getString();
        } catch (IOException e) {
            throw new RuntimeException("Error obtaining jcr node: " + e);
        } catch (RepositoryException e) {
            throw new RuntimeException("Error obtaining encoding property from jcr node: " + e);
        }
    }

    private Node getResourceNode() throws IOException {
        try {
            Node fileNode = JCRUtil.getNodeById(getId(), workspace);
            return fileNode.getNode(JCRUtil.JCR_CONTENT_PREFIX);
        } catch (ItemNotFoundException e) {
            throw new IOException("Node not found exception: " + getId());
        } catch (PathNotFoundException e) {
            throw new IOException("File node does not contains expected subnodes: " + e.getMessage());
        } catch (RepositoryException e) {
            throw new IOException("Error obtaining binary data: " + e.getMessage());
        }
    }
}
