package org.argouml.model.mdr;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.jmi.reflect.RefObject;
import javax.jmi.reflect.RefPackage;
import org.apache.log4j.Logger;
import org.netbeans.api.xmi.XMIInputConfig;
import org.netbeans.lib.jmi.util.DebugException;
import org.netbeans.lib.jmi.xmi.XmiContext;
import org.omg.uml.foundation.core.ModelElement;

/**
 * Custom resolver to use with XMI reader.
 * <p>
 * 
 * This provides two functions:
 * <nl>
 * <li>Records the mapping of <code>xmi.id</code>'s to MDR objects as they
 * are resolved so that the map can be used to lookup objects by xmi.id later
 * (used by diagram subsystem to associate GEF/PGML objects with model
 * elements).
 * <li>Resolves a System ID to a fully specified URL which can be used by MDR
 * to open and read the referenced content. The standard MDR resolver is
 * extended to support that "jar:" protocol for URLs, allowing it to handle
 * multi-file Zip/jar archives contained a set of models. The method
 * <code>toUrl</code> and supporting methods and fields was copied from the
 * AndroMDA 3.1 implementation
 * (org.andromda.repositories.mdr.MDRXmiReferenceResolverContext) by Ludo
 * (rastaman).
 * </nl>
 * <p>
 * NOTE: This is not a standalone implementation of the reference resolver since
 * it depends on extending the specific MDR implementation.
 * 
 * @author Tom Morris
 * 
 */
class XmiReferenceResolverImpl extends XmiContext {

    private static final Logger LOG = Logger.getLogger(XmiReferenceResolverImpl.class);

    private Map<String, Object> idToObjects = Collections.synchronizedMap(new HashMap<String, Object>());

    /**
     * Map indexed by MOF ID.
     */
    private Map<String, XmiReference> objectsToId;

    /**
     * System ID of top level document
     */
    private String topSystemId;

    /**
     * URI form of topSystemID for use in relativization.
     */
    private URI baseUri;

    /**
     * The array of paths in which the models references in other models will be
     * searched.
     * Copied from AndroMDA 3.1 by Ludo (rastaman).
     * see org.andromda.repositories.mdr.MDRXmiReferenceResolverContext
     */
    private List<String> modulesPath = new ArrayList<String>();

    /**
     * Module to URL map to cache things we've already found.
     * Copied from AndroMDA 3.1 by Ludo (rastaman).
     * 
     * see org.andromda.repositories.mdr.MDRXmiReferenceResolverContext
     */
    private Map<String, URL> urlMap = new HashMap<String, URL>();

    /**
     * Mapping from URL or absolute reference back to the original SystemID
     * that was read from the input file.  We'll preserve this mapping when
     * we write things back out again.
     */
    private Map<String, String> reverseUrlMap = new HashMap<String, String>();

    private boolean profile;

    private Map<String, String> public2SystemIds;

    private String modelPublicId;

    XmiReferenceResolverImpl(RefPackage[] extents, XMIInputConfig config, Map<String, XmiReference> objectToIdMap, Map<String, String> publicIds, List<String> searchDirs, boolean isProfile, String publicId, String systemId) {
        super(extents, config);
        objectsToId = objectToIdMap;
        modulesPath = searchDirs;
        profile = isProfile;
        public2SystemIds = publicIds;
        modelPublicId = publicId;
        if (isProfile) {
            if (public2SystemIds.containsKey(modelPublicId)) {
                LOG.warn("Either an already loaded profile is being re-read " + "or a profile with the same publicId is being loaded! " + "publicId = \"" + publicId + "\"; existing systemId = \"" + public2SystemIds.get(publicId) + "\"; new systemId = \"" + systemId + "\".");
            }
            public2SystemIds.put(publicId, systemId);
        }
    }

    /**
     * Save registered ID in our object map.
     * 
     * @param systemId
     *            URL of XMI field
     * @param xmiId
     *            xmi.id string for current object
     * @param object
     *            referenced object
     */
    @Override
    public void register(final String systemId, final String xmiId, final RefObject object) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Registering XMI ID '" + xmiId + "' in system ID '" + systemId + "' to object with MOF ID '" + object.refMofId() + "'");
        }
        if (topSystemId == null) {
            topSystemId = systemId;
            try {
                baseUri = new URI(systemId.substring(0, systemId.lastIndexOf('/') + 1));
            } catch (URISyntaxException e) {
                LOG.warn("Bad URI syntax for base URI from XMI document " + systemId, e);
                baseUri = null;
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Top system ID set to " + topSystemId);
            }
        }
        String resolvedSystemId = systemId;
        if (profile && systemId.equals(topSystemId)) {
            resolvedSystemId = modelPublicId;
        } else if (systemId.equals(topSystemId)) {
            resolvedSystemId = null;
        } else if (reverseUrlMap.get(systemId) != null) {
            resolvedSystemId = reverseUrlMap.get(systemId);
        } else {
            LOG.debug("Unable to map systemId - " + systemId);
        }
        String key;
        if (resolvedSystemId == null || "".equals(resolvedSystemId)) {
            key = xmiId;
        } else {
            key = resolvedSystemId + "#" + xmiId;
        }
        if (!idToObjects.containsKey(key) && !objectsToId.containsKey(object.refMofId())) {
            super.register(resolvedSystemId, xmiId, object);
            idToObjects.put(key, object);
            objectsToId.put(object.refMofId(), new XmiReference(resolvedSystemId, xmiId));
        } else {
            if (idToObjects.containsKey(key) && idToObjects.get(key) != object) {
                ((ModelElement) idToObjects.get(key)).getName();
                LOG.error("Collision - multiple elements with same xmi.id : " + xmiId);
                throw new IllegalStateException("Multiple elements with same xmi.id");
            }
            if (objectsToId.containsKey(object.refMofId())) {
                LOG.debug("register called twice for the same object " + "- ignoring second");
                XmiReference ref = objectsToId.get(object.refMofId());
                LOG.debug(" - first reference = " + ref.getSystemId() + "#" + ref.getXmiId());
                LOG.debug(" - 2nd reference   = " + systemId + "#" + xmiId);
            }
        }
    }

    /**
     * Return complete map of all registered objects.
     * 
     * @return map of xmi.id to RefObject correspondences
     */
    public Map<String, Object> getIdToObjectMap() {
        return idToObjects;
    }

    /**
     * Reinitialize the object id maps to the empty state.
     */
    public void clearIdMaps() {
        idToObjects.clear();
        objectsToId.clear();
    }

    /**
     * Convert a System ID from an HREF (typically filespec-like) to a URL.
     * Copied from AndroMDA 3.1 by Ludo (rastaman)
     * see @link org.andromda.repositories.mdr.MDRXmiReferenceResolverContext
     * @see org.netbeans.lib.jmi.xmi.XmiContext#toURL(java.lang.String)
     */
    @Override
    public URL toURL(String systemId) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("attempting to resolve Xmi Href --> '" + systemId + "'");
        }
        final String suffix = getSuffix(systemId);
        String exts = "\\.jar|\\.zip";
        String suffixWithExt = suffix.replaceAll(exts, "");
        URL modelUrl = urlMap.get(suffixWithExt);
        if (modelUrl == null) {
            if (public2SystemIds.containsKey(systemId)) {
                modelUrl = getValidURL(public2SystemIds.get(systemId));
            }
            if (modelUrl == null) {
                modelUrl = getValidURL(fixupURL(systemId));
            }
            if (modelUrl == null) {
                String modelUrlAsString = findModuleURL(suffix);
                if (!(modelUrlAsString == null || "".equals(modelUrlAsString))) {
                    modelUrl = getValidURL(modelUrlAsString);
                }
                if (modelUrl == null) {
                    modelUrl = findModelUrlOnClasspath(systemId);
                }
                if (modelUrl == null) {
                    modelUrl = super.toURL(systemId);
                }
            }
            if (modelUrl != null) {
                LOG.info("Referenced model --> '" + modelUrl + "'");
                urlMap.put(suffixWithExt, modelUrl);
                String relativeUri = systemId;
                try {
                    if (baseUri != null) {
                        relativeUri = baseUri.relativize(new URI(systemId)).toString();
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("       system ID " + systemId + "\n  relativized as " + relativeUri);
                        }
                    } else {
                        relativeUri = systemId;
                    }
                } catch (URISyntaxException e) {
                    LOG.error("Error relativizing system ID " + systemId, e);
                    relativeUri = systemId;
                }
                reverseUrlMap.put(modelUrl.toString(), relativeUri);
                reverseUrlMap.put(systemId, relativeUri);
            } else {
            }
        }
        return modelUrl;
    }

    /**
     * Finds a module in the module search path. 
     * <p>
     * Copied from AndroMDA 3.1 by Ludo (rastaman).
     * 
     * see org.andromda.repositories.mdr.MDRXmiReferenceResolverContext
     * @param moduleName
     *            the name of the module without any path
     * @return the complete URL string of the module if found (null if not
     *         found)
     */
    private String findModuleURL(String moduleName) {
        if (modulesPath == null) {
            return null;
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("findModuleURL: modulesPath.size() = " + modulesPath.size());
        }
        for (String moduleDirectory : modulesPath) {
            File candidate = new File(moduleDirectory, moduleName);
            if (LOG.isDebugEnabled()) {
                LOG.debug("candidate '" + candidate.toString() + "' exists=" + candidate.exists());
            }
            if (candidate.exists()) {
                String urlString;
                try {
                    urlString = candidate.toURI().toURL().toExternalForm();
                } catch (MalformedURLException e) {
                    return null;
                }
                return fixupURL(urlString);
            }
        }
        if (public2SystemIds.containsKey(moduleName)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Couldn't find user model (\"" + moduleName + "\") in modulesPath, attempt " + "to use a model stored within the zargo file.");
            }
            return moduleName;
        }
        return null;
    }

    /**
     * Gets the suffix of the <code>systemId</code>.
     * <p>
     * Copied from AndroMDA 3.1 by Ludo (rastaman). see
     * org.andromda.repositories.mdr.MDRXmiReferenceResolverContext
     * 
     * @param systemId the system identifier.
     * @return the suffix as a String.
     */
    private String getSuffix(String systemId) {
        int lastSlash = systemId.lastIndexOf("/");
        if (lastSlash > 0) {
            String suffix = systemId.substring(lastSlash + 1);
            return suffix;
        }
        return systemId;
    }

    /**
     * The suffixes to use when searching for referenced models on the
     * classpath.
     * <p>
     * Copied from AndroMDA 3.1 by Ludo (rastaman).
     * see org.andromda.repositories.mdr.MDRXmiReferenceResolverContext
     */
    protected static final String[] CLASSPATH_MODEL_SUFFIXES = new String[] { "xml", "xmi" };

    /**
     * Searches for the model URL on the classpath.
     * <p>
     * Copied from AndroMDA 3.1 by Ludo (rastaman).
     * 
     * see org.andromda.repositories.mdr.MDRXmiReferenceResolverContext
     * 
     * @param systemId
     *            the system identifier.
     * @return the suffix as a String.
     */
    private URL findModelUrlOnClasspath(String systemId) {
        final String dot = ".";
        String modelName = systemId;
        if (public2SystemIds.containsKey(systemId)) {
            modelName = public2SystemIds.get(systemId);
        } else {
            int filenameIndex = systemId.lastIndexOf("/");
            if (filenameIndex > 0) {
                modelName = systemId.substring(filenameIndex + 1, systemId.length());
            } else {
                LOG.warn("Received systemId with no '/'" + systemId);
            }
            if (modelName.lastIndexOf(dot) > 0) {
                modelName = modelName.substring(0, modelName.lastIndexOf(dot));
            }
        }
        URL modelUrl = Thread.currentThread().getContextClassLoader().getResource(modelName);
        if (modelUrl == null) {
            modelUrl = this.getClass().getResource(modelName);
        }
        if (modelUrl == null) {
            if (CLASSPATH_MODEL_SUFFIXES != null && CLASSPATH_MODEL_SUFFIXES.length > 0) {
                for (String suffix : CLASSPATH_MODEL_SUFFIXES) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("searching for model reference --> '" + modelUrl + "'");
                    }
                    modelUrl = Thread.currentThread().getContextClassLoader().getResource(modelName + dot + suffix);
                    if (modelUrl != null) {
                        break;
                    }
                    modelUrl = this.getClass().getResource(modelName);
                    if (modelUrl != null) {
                        break;
                    }
                }
            }
        }
        return modelUrl;
    }

    /**
     * Returns a URL if the systemId is valid. Returns null otherwise. Catches
     * exceptions as necessary.
     * <p>
     * Copied from AndroMDA 3.1 by Ludo (rastaman). See
     * org.andromda.repositories.mdr.MDRXmiReferenceResolverContext
     * 
     * @param systemId
     *            the system id
     * @return the URL (if valid) or null
     */
    private URL getValidURL(String systemId) {
        InputStream stream = null;
        URL url = null;
        try {
            url = new URL(systemId);
            stream = url.openStream();
            stream.close();
        } catch (MalformedURLException e) {
            url = null;
        } catch (IOException e) {
            url = null;
        } finally {
            stream = null;
        }
        return url;
    }

    /**
     * Fix up a file URL for a Zip file or Jar.  Assume it is a single
     * file archive with the entry name the same as the base name.
     */
    private String fixupURL(String url) {
        final String suffix = getSuffix(url);
        if (suffix.endsWith(".zargo")) {
            url = "jar:" + url + "!/" + suffix.substring(0, suffix.length() - 6) + ".xmi";
        } else if (suffix.endsWith(".zip") || suffix.endsWith(".jar")) {
            url = "jar:" + url + "!/" + suffix.substring(0, suffix.length() - 4);
        }
        return url;
    }

    @Override
    public void readExternalDocument(String arg0) {
        try {
            super.readExternalDocument(arg0);
        } catch (DebugException e) {
            LOG.error("Error reading external document " + arg0);
            throw new XmiReferenceException(arg0, e);
        }
    }
}
