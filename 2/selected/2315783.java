package org.exist.validation.resolver;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import org.apache.log4j.Logger;
import org.apache.xerces.util.XMLCatalogResolver;
import org.apache.xerces.xni.XMLResourceIdentifier;
import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.w3c.dom.ls.LSInput;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *  Wrapper around xerces2's
 *  <a href="http://xerces.apache.org/xerces2-j/javadocs/xerces2/org/apache/xerces/util/XMLCatalogResolver.html"
 *                                                      >XMLCatalogresolver</a>
 * @author Dannes Wessels (dizzzz@exist-db.org)
 */
public class eXistXMLCatalogResolver extends XMLCatalogResolver {

    public eXistXMLCatalogResolver() {
        super();
        LOG.debug("Initializing");
    }

    public eXistXMLCatalogResolver(java.lang.String[] catalogs) {
        super(catalogs);
        LOG.debug("Initializing using catalogs");
    }

    eXistXMLCatalogResolver(java.lang.String[] catalogs, boolean preferPublic) {
        super(catalogs, preferPublic);
        LOG.debug("Initializing using catalogs, preferPublic=" + preferPublic);
    }

    private static final Logger LOG = Logger.getLogger(eXistXMLCatalogResolver.class);

    /**
     *  Constructs a catalog resolver with the given list of entry files.
     *
     * @param catalogs List of Strings
     *
     *  TODO: check for non-String and NULL values.
     */
    public void setCatalogs(List<String> catalogs) {
        if (catalogs != null && catalogs.size() > 0) {
            String[] allCatalogs = new String[catalogs.size()];
            int counter = 0;
            for (String element : catalogs) {
                allCatalogs[counter] = element;
                counter++;
            }
            super.setCatalogList(allCatalogs);
        }
    }

    /**
     * @see org.apache.xerces.util.XMLCatalogResolver#resolveEntity(String, String)
     */
    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
        LOG.debug("Resolving publicId='" + publicId + "', systemId='" + systemId + "'");
        InputSource retValue = super.resolveEntity(publicId, systemId);
        if (retValue == null) {
            retValue = resolveEntityFallback(publicId, systemId);
        }
        LOG.debug("Resolved " + (retValue != null));
        if (retValue != null) {
            LOG.debug("PublicId='" + retValue.getPublicId() + "' SystemId=" + retValue.getSystemId());
        }
        return retValue;
    }

    /** moved from Collection.resolveEntity() revision 6144 */
    private InputSource resolveEntityFallback(String publicId, String systemId) throws SAXException, IOException {
        LOG.debug("Resolve failed, fallback scenario");
        if (publicId != null) {
            return null;
        }
        URL url = new URL(systemId);
        if (url.getProtocol().equals("file")) {
            String path = url.getPath();
            File f = new File(path);
            if (!f.canRead()) {
                return resolveEntity(null, f.getName());
            } else {
                return new InputSource(f.getAbsolutePath());
            }
        } else {
            return new InputSource(url.openStream());
        }
    }

    /**
     * @see org.apache.xerces.util.XMLCatalogResolver#resolveResource(String, String, String, String, String)
     */
    public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
        LOG.debug("Resolving type='" + type + "', namespaceURI='" + namespaceURI + "', publicId='" + publicId + "', systemId='" + systemId + "', baseURI='" + baseURI + "'");
        LSInput retValue = super.resolveResource(type, namespaceURI, publicId, systemId, baseURI);
        LOG.debug("Resolved " + (retValue != null));
        if (retValue != null) {
            LOG.debug("PublicId='" + retValue.getPublicId() + "' SystemId='" + retValue.getSystemId() + "' BaseURI='" + retValue.getBaseURI() + "'");
        }
        return retValue;
    }

    /**
     * @see org.apache.xerces.util.XMLCatalogResolver#resolveEntity(String, String, String, String)
     */
    public InputSource resolveEntity(String name, String publicId, String baseURI, String systemId) throws SAXException, IOException {
        LOG.debug("Resolving name='" + name + "', publicId='" + publicId + "', baseURI='" + baseURI + "', systemId='" + systemId + "'");
        InputSource retValue = super.resolveEntity(name, publicId, baseURI, systemId);
        LOG.debug("Resolved " + (retValue != null));
        if (retValue != null) {
            LOG.debug("PublicId='" + retValue.getPublicId() + "' SystemId='" + retValue.getSystemId() + "'");
        }
        return retValue;
    }

    /**
     * @see org.apache.xerces.util.XMLCatalogResolver#resolveIdentifier(XMLResourceIdentifier)
     */
    public String resolveIdentifier(XMLResourceIdentifier xri) throws IOException, XNIException {
        if (xri.getExpandedSystemId() == null && xri.getLiteralSystemId() == null && xri.getNamespace() == null && xri.getPublicId() == null) {
            return null;
        }
        LOG.debug("Resolving XMLResourceIdentifier: " + getXriDetails(xri));
        String retValue = super.resolveIdentifier(xri);
        LOG.debug("Resolved " + (retValue != null));
        if (retValue != null) {
            LOG.debug("Identifier='" + retValue + "'");
        }
        return retValue;
    }

    /**
     * @see org.apache.xerces.util.XMLCatalogResolver#resolveEntity(XMLResourceIdentifier)
     */
    public XMLInputSource resolveEntity(XMLResourceIdentifier xri) throws XNIException, IOException {
        if (xri.getExpandedSystemId() == null && xri.getLiteralSystemId() == null && xri.getNamespace() == null && xri.getPublicId() == null) {
            return null;
        }
        LOG.debug("Resolving XMLResourceIdentifier: " + getXriDetails(xri));
        XMLInputSource retValue = super.resolveEntity(xri);
        LOG.debug("Resolved " + (retValue != null));
        if (retValue != null) {
            LOG.debug("PublicId='" + retValue.getPublicId() + "' SystemId='" + retValue.getSystemId() + "' BaseSystemId=" + retValue.getBaseSystemId());
        }
        return retValue;
    }

    /**
     * @see org.apache.xerces.util.XMLCatalogResolver#getExternalSubset(String, String)
     */
    public InputSource getExternalSubset(String name, String baseURI) throws SAXException, IOException {
        LOG.debug("name='" + name + "' baseURI='" + baseURI + "'");
        return super.getExternalSubset(name, baseURI);
    }

    private String getXriDetails(XMLResourceIdentifier xrid) {
        StringBuilder sb = new StringBuilder();
        sb.append("PublicId='").append(xrid.getPublicId()).append("' ");
        sb.append("BaseSystemId='").append(xrid.getBaseSystemId()).append("' ");
        sb.append("ExpandedSystemId='").append(xrid.getExpandedSystemId()).append("' ");
        sb.append("LiteralSystemId='").append(xrid.getLiteralSystemId()).append("' ");
        sb.append("Namespace='").append(xrid.getNamespace()).append("' ");
        return sb.toString();
    }
}
