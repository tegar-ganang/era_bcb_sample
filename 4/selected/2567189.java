package org.dctmvfs.vfs.provider.dctm.client.impl.invmeta;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dctmvfs.vfs.provider.dctm.client.DctmFile;
import org.dctmvfs.vfs.provider.dctm.client.DctmFileException;
import org.dctmvfs.vfs.provider.dctm.client.operations.Operation;
import org.dctmvfs.vfs.provider.dctm.client.serializing.SerializerException;

public class InverseMetaFileImpl implements DctmFile {

    private static final Log log = LogFactory.getLog(InverseMetaFileImpl.class);

    private InverseMetaClientImpl metaClient = null;

    protected DctmFile wrappedFile = null;

    private DctmFile wrappedMetaFile = null;

    private Map attributes = null;

    public InverseMetaFileImpl(InverseMetaClientImpl metaClient, DctmFile wrappedFile, DctmFile wrappedMetaFile) throws DctmFileException {
        this.metaClient = metaClient;
        this.wrappedFile = wrappedFile;
        this.wrappedMetaFile = wrappedMetaFile;
    }

    public InverseMetaFileImpl(InverseMetaClientImpl metaClient) {
        this.metaClient = metaClient;
    }

    public void setWrappedFile(DctmFile file) {
        this.wrappedFile = file;
    }

    public void setWrappedMetaFile(DctmFile metaFile) {
        this.wrappedMetaFile = metaFile;
    }

    public Map getAttributes() throws DctmFileException {
        if (this.attributes == null) {
            this.loadAttributes();
        }
        return this.attributes;
    }

    /**
	 * Stores the given attribute if it existed in the loaded set; it is not possible to add
	 * attributes using this method. After setting the attribute the meta file is saved.
	 * @param name
	 * @param value
	 * @throws DctmFileException
	 */
    public void setAttribute(String name, Object value) throws DctmFileException {
        HashMap tmpMap = new HashMap();
        tmpMap.put(name, value);
        this.setAttributes(tmpMap);
    }

    /**
	 * Stores the given attributes (if they existed in the loaded set); it is not possible to add
	 * attributes using this method. After setting the attributes the meta file is saved. It is more
	 * efficient than setting attribute per attribute because it only saves at the end.
	 **/
    public void setAttributes(Map attrs) throws DctmFileException {
        if (this.attributes == null) {
            this.loadAttributes();
        }
        Iterator keyIterator = attrs.keySet().iterator();
        while (keyIterator.hasNext()) {
            String name = keyIterator.next().toString();
            Object value = attrs.get(name);
            if (this.attributes.containsKey(name)) {
                this.attributes.put(name, value);
            }
        }
        File tmpFile = null;
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            inputStream = this.metaClient.getAttributeSerializer().serialize(this);
            tmpFile = File.createTempFile("dctmvfs", ".tmp");
            outputStream = new FileOutputStream(tmpFile);
            int bufferSize = 1024 * 4;
            byte[] buffer = new byte[bufferSize];
            int read = -1;
            while ((read = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, read);
            }
            inputStream.close();
            inputStream = null;
            outputStream.close();
            outputStream = null;
            this.wrappedMetaFile.setFileContent(tmpFile);
        } catch (Exception e) {
            throw new DctmFileException("Error serializing attributes", e);
        } finally {
            if (tmpFile != null && tmpFile.exists()) {
                tmpFile.delete();
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    log.warn("Error closing inputstream", e);
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    log.warn("Error closing outputstream", e);
                }
            }
        }
    }

    public long getContentSize() throws DctmFileException {
        if (this.wrappedFile != null) {
            return this.wrappedFile.getContentSize();
        } else if (this.wrappedMetaFile != null) {
            return this.wrappedMetaFile.getContentSize();
        } else {
            throw new DctmFileException("Could not get content size; no content file was set");
        }
    }

    public String getId() throws DctmFileException {
        if (this.wrappedFile != null) {
            return this.wrappedFile.getId();
        } else if (this.wrappedMetaFile != null) {
            return this.wrappedMetaFile.getId();
        } else {
            throw new DctmFileException("Could not get id; no content file was set");
        }
    }

    public String getType() throws DctmFileException {
        if (this.wrappedFile != null) {
            return this.wrappedFile.getType();
        } else {
            return "dm_sysobject";
        }
    }

    public InputStream getInputStream() throws DctmFileException {
        if (this.wrappedMetaFile != null) {
            return this.wrappedFile.getInputStream();
        }
        if (this.wrappedMetaFile != null) {
            return this.wrappedMetaFile.getInputStream();
        } else {
            throw new DctmFileException("Could not get inputstream; no content file was set");
        }
    }

    public long getLastModifiedTime() throws DctmFileException {
        if (this.wrappedFile != null) {
            return this.wrappedFile.getLastModifiedTime();
        } else if (this.wrappedMetaFile != null) {
            return this.wrappedMetaFile.getLastModifiedTime();
        } else {
            throw new DctmFileException("Could not get last modified time; no content file was set");
        }
    }

    public String getName() throws DctmFileException {
        if (this.wrappedFile != null) {
            return this.wrappedFile.getName();
        } else if (this.wrappedMetaFile != null) {
            return this.wrappedMetaFile.getName();
        } else {
            throw new DctmFileException("Could not get name; no content file was set");
        }
    }

    public boolean isFolder() throws DctmFileException {
        if (this.wrappedFile != null) {
            return this.wrappedFile.isFolder();
        } else if (this.wrappedMetaFile != null) {
            return false;
        } else {
            throw new DctmFileException("Could not get type; no content file was set");
        }
    }

    /**
	 * Delete the available files
	 */
    public void delete() throws DctmFileException {
        if (this.wrappedFile != null) {
            this.wrappedFile.delete();
        }
        if (this.wrappedMetaFile != null) {
            this.wrappedMetaFile.delete();
        }
    }

    /**
	 * Sets the filecontent
	 */
    public void setFileContent(File tmpFile) throws DctmFileException {
        if (this.wrappedFile != null) {
            this.wrappedFile.setFileContent(tmpFile);
        }
    }

    public boolean isWriteable() throws DctmFileException {
        if (this.wrappedFile != null) {
            return this.wrappedFile.isWriteable();
        } else {
            return false;
        }
    }

    /**
	 * If there's no content file, then the meta file is not treated as a meta file
	 * If there's no meta file, then the attributes are empty
	 * If there is an error parsing the meta file, then the attributes are empty
	 * @throws DctmFileException
	 */
    protected void loadAttributes() throws DctmFileException {
        if (this.wrappedFile == null || this.wrappedMetaFile == null) {
            this.attributes = new HashMap();
        } else {
            try {
                this.attributes = this.metaClient.getAttributeDeserializer().deserialize(this.wrappedMetaFile.getInputStream());
            } catch (SerializerException e) {
                log.error("Error while parsing meta file", e);
                this.attributes = new HashMap();
            }
        }
    }

    public Object perform(Operation operation) throws DctmFileException {
        return this.wrappedFile.perform(operation);
    }
}
