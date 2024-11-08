package eu.planets_project.ifr.core.storage.impl.oai;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import eu.planets_project.ifr.core.storage.api.query.Query;
import eu.planets_project.ifr.core.storage.api.query.QueryValidationException;
import eu.planets_project.services.datatypes.Content;
import eu.planets_project.services.datatypes.DigitalObject;
import eu.planets_project.services.datatypes.Metadata;
import eu.planets_project.services.datatypes.DigitalObject.Builder;
import java.io.InputStream;
import java.net.*;
import java.io.*;
import java.util.*;

/**
 * KB implementation of the OAI digital object manager.
 */
public class OAIDigitalObjectManagerKBImpl extends AbstractOAIDigitalObjectManagerImpl {

    /**
	 * This is a buffer size and a max byte array size for reading data 
	 * from InputStream to byte array
	 */
    public static final int BUFFER_SIZE = 1048576;

    private static final String RESOLVER_START = "<dc:identifier xsi:type=\"dcterms:URI\">";

    private static final String RESOLVER_END = "</dc:identifier>";

    private static final String POST_FORM_START = "method=\"post\" action=\"";

    private static final String POST_FORM_END = "\">";

    private static final String POST_FORM_NAME = "name=\"";

    private static final String POST_FORM_VALUE = "\" value=\"";

    private static final String LINK_START = "Open de <a href=\"";

    private static final String LINK_END = "\" target=\"_self\">publicatie";

    private static final String DOMAIN_NAME = "domain_name_place_holder";

    private static final String COMMENT_START = "<!--";

    private static final String COMMENT_END = "-->";

    private static final String AND_CHAR = "&";

    private static final String GLEICH_CHAR = "=";

    private static final String BRACES = "%22";

    private static final String METADATA_END = "</dc";

    private static final String METADATA_TYPE = "dcx";

    private URI baseRegistryURI = null;

    /**
	 * This is an enumeration of the OAI article metadata
	 */
    enum OaiMetadata {

        title, bibliographicCitation, creator, subject, abstrac, publisher, extent, uri, isPartOf, accessRights
    }

    /**
	 * This is an array of the OAI article numbers
	 */
    String[] Articles = { "1237818724132", "1237818828220", "1237818827273", "1237818707653", "1237818781294", "1237818757698", "1237818353465", "1237818740969", "1237818819576", "1237818655074", "1262698022536" };

    /**
	 * The cache map binds URI with the digital object.
	 */
    private static Map<URI, DigitalObject> leafMap = new HashMap<URI, DigitalObject>();

    /**
	 * This is a cache for list method.
	 */
    private static ArrayList<URI> uriList = new ArrayList<URI>();

    /**
	 * @param baseURL The base URL
	 */
    public OAIDigitalObjectManagerKBImpl(String baseURL) {
        super(baseURL, "");
    }

    /**
	 * This method evaluates original HTTP URI from registry URI
	 * @param uri The registry URI
	 * @return The original HTTP URI
	 */
    public URI getOriginalUri(URI keyUri) {
        URI res = keyUri;
        try {
            if (keyUri != null) {
                log.info("OAIDigitalObjectManagerKBImpl getOriginalUri() find out the original key for uri: " + keyUri);
                for (URI uri : leafMap.keySet()) {
                    if (uri.toString().contains(keyUri.toString())) {
                        res = uri;
                        log.info("OAIDigitalObjectManagerKBImpl getOriginalUri() found: " + res);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.info("OAIDigitalObjectManagerKBImpl getOriginalUri() error: " + e.getMessage());
        }
        return res;
    }

    /**
	 * {@inheritDoc}
	 * @see eu.planets_project.ifr.core.storage.api.DigitalObjectManager#retrieve(java.net.URI)
	 */
    public DigitalObject retrieve(URI pdURI) throws DigitalObjectNotFoundException {
        long starttime = System.currentTimeMillis();
        log.info("OAIDigitalObjectManagerKBImpl retrieve() starttime: " + starttime);
        try {
            log.log(Level.INFO, "OAIDigitalObjectManagerKBImpl retrieve() uri: " + pdURI);
            URI originalURI = getOriginalUri(pdURI);
            if (originalURI != null && leafMap.containsKey(originalURI)) {
                log.info("OAIDigitalObjectManagerKBImpl retrieve() already exist in map uri: " + originalURI);
                long endtime = System.currentTimeMillis();
                log.info("OAIDigitalObjectManagerKBImpl retrieve() timediff: " + (endtime - starttime));
                return leafMap.get(originalURI);
            }
            long endtime = System.currentTimeMillis();
            log.info("OAIDigitalObjectManagerKBImpl retrieve() error1: NoHTTP URL available." + " timediff: " + (endtime - starttime));
            throw new DigitalObjectNotFoundException("No HTTP URL available for this record");
        } catch (Exception e) {
            throw new DigitalObjectNotFoundException(e.getMessage());
        }
    }

    public List<URI> list(URI pdURI, Query q) throws QueryValidationException {
        return list(pdURI, null);
    }

    /**
     * This method retrieves article metadata from the metadata repository response
     * @param emd
     * @param resolver
     * @return data for particular metadata field
     */
    private String retrieveOaiMetadata(OaiMetadata emd, String resolver) {
        String res = "";
        if (resolver != null && resolver.indexOf(emd.name()) > 0) {
            res = resolver.substring(resolver.indexOf(emd.name()) + emd.name().length() + 1, resolver.indexOf(METADATA_END, resolver.indexOf(emd.name())));
            if (emd.equals(OaiMetadata.abstrac) && res != null && res.length() > 0) {
                res = res.substring(1);
            }
        }
        log.log(Level.INFO, "retrieveOaiMetadata() res: " + res + ", enum: " + emd);
        return res;
    }

    public List<URI> list(URI pdURI) {
        if (pdURI != null) {
            if (uriList != null && uriList.size() > 0) {
                return uriList;
            } else {
                long starttime = System.currentTimeMillis();
                log.info("OAIDigitalObjectManagerKBImpl list() starttime: " + starttime);
                ArrayList<URI> resultList = new ArrayList<URI>();
                for (int i = 0; i < Articles.length; i++) {
                    String resolver = transferData(OAIDigitalObjectManagerKBBase.DEFAULT_BASE_URL + BRACES + Articles[i] + BRACES);
                    String resolverLink = resolver.substring(resolver.indexOf(RESOLVER_START) + RESOLVER_START.length(), resolver.indexOf(RESOLVER_END));
                    log.log(Level.INFO, "test() resolverLink[" + i + "]: " + resolverLink);
                    try {
                        if (resolverLink != null) {
                            String publicationLink = retrieveIntermediateHtmlPage(resolverLink);
                            resultList.add(URI.create(publicationLink));
                            String title = "";
                            List<Metadata> metadataList = new ArrayList<Metadata>(0);
                            for (OaiMetadata emd : OaiMetadata.values()) {
                                String md = retrieveOaiMetadata(emd, resolver);
                                if (md != null && md.length() > 0) {
                                    Metadata metadata = new Metadata(URI.create(METADATA_TYPE), emd.name(), md);
                                    metadataList.add(metadata);
                                    if (metadata.getName().equalsIgnoreCase(OaiMetadata.title.name())) {
                                        title = metadata.getContent();
                                    }
                                }
                            }
                            if (publicationLink != null && publicationLink.toString().length() > 0) {
                                Builder builder = new DigitalObject.Builder(Content.byReference(URI.create(publicationLink).toURL()));
                                builder.title(title);
                                String filename = "";
                                if (publicationLink != null) {
                                    filename = URI.create(publicationLink).getPath();
                                    log.info("OAIDigitalObjectManagerKBImpl list() filename: " + filename);
                                    if (filename != null) {
                                        String[] parts = filename.split("/");
                                        if (parts != null && parts.length > 0) filename = parts[parts.length - 1];
                                    }
                                }
                                log.info("OAIDigitalObjectManagerKBImpl list() filename: " + filename + ", pdURI.toString(): " + pdURI.toString() + ", publicationLink: " + publicationLink);
                                URI permanentUri = URI.create(getBaseRegistryURI() + "/" + filename).normalize();
                                builder.permanentUri(permanentUri);
                                builder.metadata(metadataList.toArray(new Metadata[] {}));
                                long endtime = System.currentTimeMillis();
                                log.info("OAIDigitalObjectManagerKBImpl list() timediff: " + (endtime - starttime));
                                DigitalObject o = builder.build();
                                if (publicationLink != null && !leafMap.containsKey(publicationLink)) {
                                    log.info("OAIDigitalObjectManagerKBImpl list() add to map uri: " + publicationLink);
                                    leafMap.put(URI.create(publicationLink), o);
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.info("OAIDigitalObjectManagerKBImpl list() error: " + e.getMessage());
                    }
                }
                uriList = resultList;
                return resultList;
            }
        } else {
            return new ArrayList<URI>();
        }
    }

    /**
     * This method removes comments from the HTML page.
     * @param str The HTML page source code
     * @return The HTML source code without comments
     */
    private static String removeComments(String str) {
        String res = str;
        if (str.indexOf(COMMENT_START) > 0) {
            String tmp = str.substring(0, str.indexOf(COMMENT_START)) + str.substring(str.indexOf(COMMENT_END) + COMMENT_END.length());
            res = removeComments(tmp);
        }
        log.log(Level.INFO, "removeComments return res: " + res);
        return res;
    }

    /**
     * This method transfers bytes from InputStream to string
     * @param path The path to the server
     * @return data as a string
     */
    private static String transferData(String path) {
        String res = "";
        try {
            URL url = URI.create(path).toURL();
            InputStream is = url.openStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[BUFFER_SIZE];
            int len;
            log.log(Level.INFO, "##### inputstream available: " + is.available());
            while ((len = is.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            log.log(Level.INFO, "##### buf length: " + len + ", out.len: " + out.size());
            byte[] byteContent = out.toByteArray();
            log.log(Level.INFO, "evaluateContent() byteContent.length: " + byteContent.length);
            out.close();
            is.close();
            res = new String(byteContent);
        } catch (Exception e) {
            log.info("OAIDigitalObjectManagerKBImpl error: " + e.getMessage());
        }
        return res;
    }

    /**
     * This method creates a post form request
     * @param path The path to the server
     * @param parameterList The form parameter
     * @return The server response
     */
    private static String sendPostRequest(String path, ArrayList<String> parameterList) {
        String res = "";
        try {
            URL url = URI.create(path).toURL();
            String data = "";
            Iterator<String> i = parameterList.iterator();
            while (i.hasNext()) {
                if (data.length() == 0) {
                    data = i.next();
                } else {
                    data = data + AND_CHAR + i.next();
                }
                log.log(Level.INFO, "sendPostRequest() data: " + data);
            }
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(data);
            wr.flush();
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = rd.readLine()) != null) {
                res = res + line;
                log.log(Level.INFO, "sendPostRequest() line: " + line);
            }
            wr.close();
            rd.close();
        } catch (Exception e) {
            log.log(Level.INFO, "sendPostRequest() error: " + e.getMessage());
        }
        return res;
    }

    /**
     * This method retrieves the intermediate HTML page from the resolver link.
     * @param resolver link
     * @return intermediate HTML page source code
     */
    private static String retrieveIntermediateHtmlPage(String resolver) {
        String res = "";
        String postForm = transferData(resolver);
        log.log(Level.INFO, "test() postForm: " + postForm);
        postForm = removeComments(postForm);
        log.log(Level.INFO, "test() after removeComments postForm: " + postForm);
        String postFormLink = postForm.substring(postForm.indexOf(POST_FORM_START) + POST_FORM_START.length(), postForm.indexOf(POST_FORM_END, postForm.indexOf(POST_FORM_START) + POST_FORM_START.length()));
        ArrayList<String> parameterList = new ArrayList<String>(0);
        ArrayList<String> tmpParameterList = new ArrayList<String>(Arrays.asList(postForm.split(POST_FORM_NAME)));
        Iterator<String> i = tmpParameterList.iterator();
        while (i.hasNext()) {
            String line = i.next();
            if (line.contains(POST_FORM_VALUE)) {
                try {
                    String name = URLEncoder.encode(line.substring(0, line.indexOf(POST_FORM_VALUE)), "UTF-8");
                    String value = URLEncoder.encode(line.substring(line.indexOf(POST_FORM_VALUE) + POST_FORM_VALUE.length(), line.indexOf(POST_FORM_END)), "UTF-8");
                    parameterList.add(name + GLEICH_CHAR + value);
                    log.log(Level.INFO, "test() param: " + name + GLEICH_CHAR + value);
                } catch (Exception e) {
                    log.log(Level.INFO, "retrieveIntermediateHtmlPage(): " + e.getMessage());
                }
            }
        }
        String intermediateHtml = sendPostRequest(postFormLink, parameterList);
        log.log(Level.INFO, "test() intermediateHtml: " + intermediateHtml);
        if (intermediateHtml != null && intermediateHtml.length() > 0) {
            res = DOMAIN_NAME + intermediateHtml.substring(intermediateHtml.indexOf(LINK_START) + LINK_START.length(), intermediateHtml.indexOf(LINK_END));
        }
        log.log(Level.INFO, "test() retrieveIntermediateHtmlPage() res: " + res);
        return res;
    }

    public URI storeAsNew(URI pdURI, DigitalObject digitalObject) throws eu.planets_project.ifr.core.storage.api.DigitalObjectManager.DigitalObjectNotStoredException {
        throw new DigitalObjectNotStoredException("Storing not supported by this implementation.");
    }

    public URI storeAsNew(DigitalObject digitalObject) throws eu.planets_project.ifr.core.storage.api.DigitalObjectManager.DigitalObjectNotStoredException {
        throw new DigitalObjectNotStoredException("Storing not supported by this implementation.");
    }

    public URI updateExisting(URI pdURI, DigitalObject digitalObject) throws eu.planets_project.ifr.core.storage.api.DigitalObjectManager.DigitalObjectNotStoredException, eu.planets_project.ifr.core.storage.api.DigitalObjectManager.DigitalObjectNotFoundException {
        throw new DigitalObjectNotStoredException("Storing not supported by this implementation.");
    }

    /**
     * This class manages the cache of retrieved digital objects.
     * @author GrafR
     *
     */
    @SuppressWarnings("unused")
    private class ManagerControl extends Thread {

        /**
         * Repository implementation
         */
        OAIDigitalObjectManagerKBImpl impl;

        /**
         * The time between consistency check
         */
        long sleeptime = 1200000;

        long starttime = 0;

        int counter = 0;

        /**
         * @param impl The repository implementation
         */
        public ManagerControl(OAIDigitalObjectManagerKBImpl _impl) {
            this.impl = _impl;
            starttime = System.currentTimeMillis();
            log.info("ManagerControl() starttime: " + starttime);
        }

        public void run() {
            while (true) {
                try {
                    log.info("ManagerControl run().");
                    Thread.sleep(sleeptime);
                    Iterator<URI> leafIterator = leafMap.keySet().iterator();
                    while (leafIterator.hasNext()) {
                        URI currentUri = leafIterator.next();
                        log.info("ManagerControl check uri: " + currentUri);
                        try {
                            DigitalObject presentObj = impl.retrieve(currentUri);
                            if (!presentObj.equals(leafMap.get(currentUri))) {
                                leafMap.put(currentUri, presentObj);
                            }
                        } catch (DigitalObjectNotFoundException e) {
                            log.info("ManagerControl digital object not found for uri: " + currentUri);
                            long removetime = System.currentTimeMillis();
                            log.info("ManagerControl() difftime: " + (removetime - starttime) + ", counter: " + counter);
                            log.info("ManagerControl remove from cache for uri: " + currentUri);
                            if (leafMap.containsKey(currentUri)) {
                                leafMap.remove(currentUri);
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    log.info("ManagerControl error: " + e.getMessage());
                    e.printStackTrace();
                }
                counter++;
            }
        }
    }

    public URI getBaseRegistryURI() {
        return baseRegistryURI;
    }

    public void setBaseRegistryURI(URI _baseRegistryURI) {
        baseRegistryURI = _baseRegistryURI;
    }
}
