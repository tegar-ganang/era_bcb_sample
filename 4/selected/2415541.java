package org.obe.engine.repository;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.obe.client.api.repository.ObjectNotFoundException;
import org.obe.client.api.repository.RepositoryException;
import org.obe.client.api.repository.ResourceMetaData;
import org.obe.spi.service.ResourceRepository;
import org.obe.spi.service.ServiceManager;
import org.obe.util.CommonConfig;
import org.obe.util.SchemaUtils;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * A repository for XML documents, DTDs, schemas, XSL transforms, etc.
 *
 * @author Adrian Price
 */
public class BasicResourceRepository extends AbstractRepository implements ResourceRepository {

    private static final Log _logger = LogFactory.getLog(BasicResourceRepository.class);

    protected static final String RESOURCE_DIR = "resources";

    protected static final String RESOURCE_PATH = RESOURCE_DIR + System.getProperty("file.separator");

    private static final File _resourceDir = new File(CommonConfig.getConfigDir(), RESOURCE_DIR);

    private static final int BUF_SIZE = 1024;

    private static void copy(InputStream in, OutputStream out) throws IOException {
        try {
            byte[] ba = new byte[BUF_SIZE];
            int n;
            while ((n = in.read(ba)) > 0) out.write(ba, 0, n);
        } finally {
            try {
                in.close();
            } catch (IOException e) {
            }
            try {
                out.close();
            } catch (IOException e) {
            }
        }
    }

    public BasicResourceRepository(ServiceManager svcMgr) {
        super(svcMgr, ResourceMetaData.class);
    }

    public synchronized void init() throws IOException, RepositoryException {
        super.init();
        SchemaUtils.setEntityResolver(this);
        SchemaUtils.setURIResolver(this);
    }

    public synchronized void exit() {
        SchemaUtils.setEntityResolver(null);
        SchemaUtils.setURIResolver(null);
        super.exit();
    }

    public ResourceMetaData[] findXMLTypes(String locale) throws RepositoryException {
        return (ResourceMetaData[]) findObjectTypes();
    }

    public ResourceMetaData findXMLType(String type, String locale) throws RepositoryException {
        return (ResourceMetaData) findObjectType(type);
    }

    public void createEntity(ResourceMetaData entity) throws RepositoryException {
        findXMLType(entity.getResourceType(), null);
        try {
            createEntity(entity, null);
        } catch (IOException e) {
            throw new RepositoryException(e);
        }
    }

    public void deleteEntity(String id) throws RepositoryException {
        String resource = (String) findInstance(id, true);
        File file = new File(_resourceDir, resource);
        file.delete();
        super.deleteEntry(id);
    }

    public void updateEntity(ResourceMetaData entity) throws RepositoryException {
        Entry entry = updateEntry(entity.getId(), entity);
        try {
            if (entity.getContent() != null) writeFile(entry);
        } catch (IOException e) {
            throw new RepositoryException(e);
        }
    }

    public ResourceMetaData[] findXMLMetaData(boolean includeContent) throws RepositoryException {
        ResourceMetaData[] metaData;
        if (includeContent) {
            Entry[] entries = findEntries();
            metaData = new ResourceMetaData[entries.length];
            try {
                for (int i = 0; i < metaData.length; i++) {
                    if (metaData[i].getContent() == null) readFile(entries[i]);
                }
            } catch (IOException e) {
                throw new RepositoryException(e);
            }
        } else {
            metaData = (ResourceMetaData[]) findMetaData();
        }
        return metaData;
    }

    public ResourceMetaData findXMLMetaData(String id, boolean includeContent) throws RepositoryException {
        Entry entry = findEntry(id, true);
        ResourceMetaData metaData = (ResourceMetaData) entry.getMetaData();
        if (includeContent && metaData.getContent() == null) {
            try {
                readFile(entry);
            } catch (IOException e) {
                throw new RepositoryException(e);
            }
        }
        return metaData;
    }

    public InputStream findEntity(String id) throws RepositoryException {
        InputStream in = null;
        Entry entry = findEntry(id, true);
        ResourceMetaData resourceMetaData = (ResourceMetaData) entry.getMetaData();
        if (resourceMetaData != null) {
            byte[] content = resourceMetaData.getContent();
            if (content != null) in = new ByteArrayInputStream(content);
        }
        if (in == null) {
            String resource = RESOURCE_PATH + entry.getInstance(this);
            in = CommonConfig.openInputStream(resource);
            if (in == null) throw new ObjectNotFoundException(resource);
        }
        return in;
    }

    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
        if (_logger.isDebugEnabled()) _logger.debug("resolveEntity(" + publicId + ", " + systemId + ')');
        InputStream in = null;
        if (publicId != null) {
            try {
                in = findEntity(publicId);
            } catch (ObjectNotFoundException e) {
            } catch (RepositoryException e) {
                _logger.error(e);
                throw new IOException(e.getMessage());
            }
        }
        if (in == null) {
            try {
                in = findEntity(systemId);
            } catch (ObjectNotFoundException e) {
                _logger.info("Downloading " + systemId);
                in = getServiceManager().getDataConverter().toInputStream(new URL(systemId));
                try {
                    int index = systemId.lastIndexOf('.');
                    if (index == -1) index = systemId.lastIndexOf('?');
                    String xmlType = systemId.substring(index + 1).toLowerCase();
                    ResourceMetaData type = findXMLType(xmlType, null);
                    ResourceMetaData metaData = new ResourceMetaData(publicId, systemId);
                    metaData.setType(type);
                    createEntity(metaData, in);
                    in = findEntity(metaData.getId());
                } catch (RepositoryException re) {
                    throw new SAXException(re);
                }
            } catch (RepositoryException e) {
                throw new SAXException(e);
            }
        }
        InputSource inputSource = new InputSource(in);
        inputSource.setPublicId(publicId);
        inputSource.setSystemId(systemId);
        return inputSource;
    }

    public Source resolve(String href, String base) throws TransformerException {
        InputStream in;
        try {
            URI hrefUri = new URI(href);
            URI uri = base == null ? hrefUri : new URI(base).relativize(hrefUri);
            String id = uri.isAbsolute() ? uri.toURL().toString() : href;
            try {
                in = findEntity(id);
            } catch (ObjectNotFoundException e) {
                if (!uri.isAbsolute()) throw new TransformerException(e);
                _logger.info("Downloading " + uri);
                in = getServiceManager().getDataConverter().toInputStream(uri.toURL());
                ResourceMetaData metaData = new ResourceMetaData(null, id);
                createEntity(metaData, in);
                in = findEntity(metaData.getId());
            }
        } catch (IOException ioe) {
            throw new TransformerException(ioe);
        } catch (RepositoryException e) {
            throw new TransformerException(e);
        } catch (URISyntaxException e) {
            throw new TransformerException(e);
        }
        return in == null ? null : new StreamSource(in);
    }

    private void createEntity(ResourceMetaData entity, InputStream in) throws IOException, RepositoryException {
        File destFile = null;
        FileOutputStream out = null;
        try {
            String xmltype = entity.getResourceType();
            findXMLType(xmltype, null);
            if (in == null) {
                byte[] content = entity.getContent();
                if (content == null) {
                    throw new RepositoryException("No content supplied for entity: " + entity);
                }
                in = new ByteArrayInputStream(content);
            }
            destFile = File.createTempFile("obe", '.' + xmltype, _resourceDir);
            createEntry(entity.getId(), entity, destFile.getName());
            out = new FileOutputStream(destFile);
            copy(in, out);
        } catch (IOException e) {
            if (destFile != null) destFile.delete();
            throw e;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private void readFile(Entry entry) throws IOException, RepositoryException {
        String resource = RESOURCE_DIR + entry.getInstance(this);
        InputStream in = CommonConfig.openInputStream(resource);
        if (in != null) {
            ByteArrayOutputStream out = new ByteArrayOutputStream(BUF_SIZE);
            copy(in, out);
            ((ResourceMetaData) entry.getMetaData()).setContent(out.toByteArray());
        }
    }

    private void writeFile(Entry entry) throws IOException, RepositoryException {
        byte[] content = ((ResourceMetaData) entry.getMetaData()).getContent();
        if (content != null) {
            String resource = (String) entry.getInstance(this);
            OutputStream out = new FileOutputStream(new File(_resourceDir, resource));
            try {
                out.write(content);
            } finally {
                out.close();
            }
        }
    }

    protected Log getLogger() {
        return _logger;
    }

    protected String getConfigurationFileName() {
        return "BasicResourceRepository.xml";
    }

    public String getServiceName() {
        return SERVICE_NAME;
    }
}
