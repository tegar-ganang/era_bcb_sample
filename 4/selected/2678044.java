package tufts.vue;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;
import javax.activation.MimetypesFileTypeMap;
import javax.swing.JOptionPane;
import javax.xml.namespace.QName;
import javax.xml.rpc.ServiceException;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.encoding.*;
import org.apache.log4j.Logger;
import org.jdom.*;
import edu.tufts.vue.dsm.DataSource;

public class SakaiPublisher {

    /**
     * All references to _local resources have URLs with a "file" prefix
     */
    public static final String FILE_PREFIX = "file://";

    public static final String DEFAULT_MIME_TYPE = "application/vue";

    /**
	 * Maps published to Sakai are stored in folders that reflect their 
	 * filename. The filename is transformed by replacing the ".vue" suffix
	 * with " vue map". 
	 */
    public static final String VUE_MAP_FOLDER_SUFFIX = " vue map";

    private static final Logger Log = Logger.getLogger(SakaiPublisher.class);

    private static final String RESOURCE_DESC = "VUE resource";

    private static final String MAP_DESC = "VUE map";

    /** uploadMap
     *  
     * @param dataSource DataSource object for Sakai LMS
     * @param collectionId Workspace in Sakai to store map
     * @param map VUE map to store in Sakai
     * @param overwrite TODO
     * @param publisher 
     */
    public static void uploadMap(DataSource dataSource, Object collectionId, LWMap map, int overwrite) throws Exception {
        Properties dsConfig = dataSource.getConfiguration();
        String sessionId = getSessionId(dsConfig);
        String hostUrl = getHostUrl(dsConfig);
        File savedMapFile = saveMapToFile(map);
        String resourceName = savedMapFile.getName();
        if (overwrite == JOptionPane.YES_OPTION) {
        }
        String folderName = createFolder(sessionId, hostUrl, collectionId.toString(), makeSakaiFolderFromVueMap(resourceName));
        if (savedMapFile.exists()) {
            hostUrl = getHostUrl(dsConfig);
            uploadObjectToRepository(hostUrl, sessionId, resourceName, folderName, savedMapFile, map.hasNotes() ? map.getNotes() : MAP_DESC, false);
        }
    }

    /** uploadMapAll
     * Iterate over map, storing local resources in repository, rewriting the 
     * references in the map so they point to the remote location.  Then store
     * revised map in repository, leaving map in memory unchanged. (!?)
     * 
     * Based on method of the same name in tufts.vue.FedoraPublisher.java.
     *  
     * @param dataSource DataSource object for Sakai LMS
     * @param collectionId Workspace in Sakai to store map
     * @param map VUE map to store in Sakai
     * @param overwrite TODO
     */
    public static void uploadMapAll(DataSource dataSource, Object collectionId, LWMap map, int overwrite) throws CloneNotSupportedException, IOException, ServiceException {
        Properties dsConfig = dataSource.getConfiguration();
        String hostUrl = getHostUrl(dsConfig);
        String sessionId = getSessionId(dsConfig);
        File savedMapFile = saveMapToFile(map);
        String resourceName = savedMapFile.getName();
        if (!savedMapFile.exists()) {
            throw new IOException();
        }
        String folderName = createFolder(sessionId, hostUrl, collectionId.toString(), makeSakaiFolderFromVueMap(resourceName));
        String mapLabel = map.getLabel();
        File origFile = map.getFile();
        File tempFile = new File(VueUtil.getDefaultUserFolder() + File.separator + origFile.getName());
        tempFile.deleteOnExit();
        tufts.vue.action.ActionUtil.marshallMap(tempFile, map);
        LWMap cloneMap = tufts.vue.action.OpenAction.loadMap(tempFile.getAbsolutePath());
        Iterator<LWComponent> i = cloneMap.getAllDescendents(LWComponent.ChildKind.PROPER).iterator();
        while (i.hasNext()) {
            LWComponent component = (LWComponent) i.next();
            Log.debug("Component:" + component + " has resource:" + component.hasResource());
            if (component.hasResource() && (component instanceof LWNode || component instanceof LWLink) && (component.getResource() instanceof URLResource)) {
                URLResource resource = (URLResource) component.getResource();
                System.out.println("Component:" + component + "file:" + resource.getSpec() + " has file:" + resource.getSpec().startsWith(FILE_PREFIX));
                if (resource.isLocalFile()) {
                    File file = new File(resource.getSpec().replace(FILE_PREFIX, ""));
                    System.out.println("LWComponent:" + component.getLabel() + " Resource: " + resource.getSpec() + " File:" + file + " exists:" + file.exists() + " MimeType" + new MimetypesFileTypeMap().getContentType(file));
                    if (!file.exists()) {
                        continue;
                    }
                    uploadObjectToRepository(getHostUrl(dsConfig), sessionId, file.getName(), folderName, file, RESOURCE_DESC, true);
                    resource.removeProperty("File");
                    String ingestUrl = hostUrl + "/access/content" + folderName + (new File(resource.getSpec().replace(FILE_PREFIX, ""))).getName();
                    component.setResource(URLResource.create(ingestUrl));
                }
            }
        }
        tufts.vue.action.ActionUtil.marshallMap(tempFile, cloneMap);
        uploadObjectToRepository(hostUrl, sessionId, resourceName, folderName, cloneMap.getFile(), cloneMap.hasNotes() ? cloneMap.getNotes() : MAP_DESC, false);
        tufts.vue.action.ActionUtil.marshallMap(origFile, map);
    }

    /**
     * Add a resource to a given collection.  The resource is passed encoded 
     * using Base64.
	 *
     * @param hostUrl
     * @param sessionId a valid sessionid
     * @param resourceName a name of the resource to be added
     * @param collectionId  collectionId of the collection it is to be added to
     * @param file local resource
     * @param description of the resource to be added
     * @param isBinary if true, content is encoded using Base64, if false content is assumed to be text.
     * @throws MalformedURLException
	 * @throws RemoteException
	 * @throws ServiceException
	 */
    private static void uploadObjectToRepository(String hostUrl, String sessionId, String resourceName, String collectionId, File file, String description, boolean isBinary) throws IOException, MalformedURLException, RemoteException, ServiceException {
        String contentMime = null;
        String type = getMimeType(file);
        String retVal;
        String endpoint = hostUrl + "/sakai-axis/ContentHosting.jws";
        Service service = new Service();
        Call call = (Call) service.createCall();
        call.setTargetEndpointAddress(new java.net.URL(endpoint));
        call.setOperationName(new QName(hostUrl + "/", "createContentItem"));
        if (isBinary) {
            contentMime = Base64.encode(getByteArrayFromFile(file));
        } else {
            contentMime = getStringFromFile(file);
        }
        retVal = (String) call.invoke(new Object[] { sessionId, resourceName, collectionId, contentMime, description, type, isBinary });
    }

    /**
	 * @param file
	 * @return file contents as a String
	 */
    private static String getStringFromFile(File file) throws IOException {
        String s;
        StringBuffer sb = new StringBuffer();
        BufferedReader in = new BufferedReader(new FileReader(file));
        while ((s = in.readLine()) != null) {
            sb.append(s);
            sb.append("\n");
        }
        in.close();
        return sb.toString();
    }

    /**
	 * @param file
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
    private static byte[] getByteArrayFromFile(File file) {
        int bufferSize = 1024 * 8;
        ByteBuffer inBuf = ByteBuffer.allocate(bufferSize);
        ByteArrayOutputStream outBuf = new ByteArrayOutputStream(bufferSize);
        int numRead = 0;
        try {
            FileChannel fc = new FileInputStream(file).getChannel();
            while ((numRead = fc.read(inBuf)) >= 0) {
                inBuf.flip();
                outBuf.write(inBuf.array());
                inBuf.clear();
            }
            outBuf.flush();
            outBuf.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outBuf.toByteArray();
    }

    /**
	 * @param configuration
	 * @return authenticated session id
	 */
    public static String getSessionId(Properties configuration) {
        String username = configuration.getProperty("sakaiUsername");
        String password = configuration.getProperty("sakaiPassword");
        String hostUrl = getHostUrl(configuration);
        String sessionId = null;
        try {
            String endpoint = hostUrl + "/sakai-axis/SakaiLogin.jws";
            Service service = new Service();
            Call call = (Call) service.createCall();
            call.setTargetEndpointAddress(new java.net.URL(endpoint));
            call.setOperationName(new QName(hostUrl + "/", "login"));
            sessionId = (String) call.invoke(new Object[] { username, password });
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (ServiceException e) {
            e.printStackTrace();
        }
        return sessionId;
    }

    /**
	 * @param configuration
	 * @param sessionId
	 * @return the server's id string. This is concatenated with the 
	 * session id to create a JSESSIONID cookie.  VUE uses the cookie to 
	 * access content using HTTP requests.
	 */
    public static String getServerId(Properties configuration, String sessionId) {
        String hostUrl = getHostUrl(configuration);
        String serverId = null;
        try {
            String endpoint = hostUrl + "/sakai-axis/SakaiServerUtil.jws";
            Service service = new Service();
            Call call = (Call) service.createCall();
            call.setTargetEndpointAddress(new java.net.URL(endpoint));
            call.setOperationName(new QName(hostUrl + "/", "getSakaiServerId"));
            serverId = (String) call.invoke(new Object[] { sessionId });
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (ServiceException e) {
            e.printStackTrace();
        }
        return serverId;
    }

    /**
	 * @param ds DataSource representing the Sakai repository
	 * @return JSESSIONID cookie string
	 */
    public static String getCookieString(DataSource ds) {
        Properties dsConfig = ds.getConfiguration();
        String sessionId = getSessionId(dsConfig);
        String serverId = getServerId(dsConfig, sessionId);
        return "JSESSION=" + sessionId + "." + serverId;
    }

    /**
	 * @param map
	 * @param publisher 
	 * @return File object that contains marshalled content of map parameter
	 */
    private static File saveMapToFile(LWMap map) {
        File tmpFile = map.getFile();
        if ((!map.isModified()) && (null != tmpFile)) {
            tmpFile = map.getFile();
        }
        if (map.isModified()) {
            tmpFile = new File(VueUtil.getDefaultUserFolder() + File.separator + map.getFile().getName());
            tmpFile.deleteOnExit();
            tufts.vue.action.ActionUtil.marshallMap(tmpFile);
        }
        return tmpFile;
    }

    /**
	 * @param sessionId
	 * @param hostUrl
	 * @param collectionId
	 * @param folderName
	 * @return collectionId of  newly created folder
	 */
    private static String createFolder(String sessionId, String hostUrl, String collectionId, String folderName) {
        String resString = null;
        String resId = collectionId + folderName;
        try {
            String endpoint = hostUrl + "/sakai-axis/ContentHosting.jws";
            Service service = new Service();
            String content = "Test folder: " + folderName;
            Log.debug("Folder name: " + folderName + ", content data: " + content);
            Call call = (Call) service.createCall();
            call.setTargetEndpointAddress(new java.net.URL(endpoint));
            call.setOperationName(new QName(hostUrl + "/", "createFolder"));
            resString = (String) call.invoke(new Object[] { sessionId, collectionId, folderName });
            System.out.println("Sent ContentHosting.createFolder(sessionId, collId, name), got '" + resString + "'");
        } catch (Exception e) {
            System.err.println(e.toString());
        }
        return resId + "/";
    }

    /**  
	 * @param configuration
	 * @return 
	 */
    private static String getHostUrl(Properties configuration) {
        String hostUrl = configuration.getProperty("sakaiHost");
        String port = configuration.getProperty("sakaiPort");
        if (!hostUrl.startsWith("http://")) {
            hostUrl = "http://" + hostUrl;
        }
        if (port != null && port.length() > 0) {
            hostUrl = hostUrl + ":" + port;
        }
        return hostUrl;
    }

    /** Given a Sakai collection id, return its children, if any.  For example,
	 * for this collection hierarchy:
	 * Bedrock
	 *    Fred
	 *    	BamBam
	 *    Wilma
	 *    	Pebbles
	 * a call to getChildCollections("/Bedrock/") would return Fred and Wilma, 
	 * but not BamBam or Pebbles.
	 * @param collectionId
	 * @return
	 */
    public static Vector<String> getChildCollectionIds(String collectionId) {
        return new Vector();
    }

    /** Create a a Sakai folder name by replacing the ".vue" suffix with the 
	 * defined folder suffix defined in VUE_MAP_FOLDER_SUFFIX.
	 * 
	 * @param fileName is the name of the VUE map file
	 * @return name of Sakai folder to publish map into
	 */
    public static String makeSakaiFolderFromVueMap(String fileName) {
        String sakaiFolder = fileName.replaceAll("\\.vue$", VUE_MAP_FOLDER_SUFFIX);
        return sakaiFolder;
    }

    private static String getMimeType(File file) {
        String mimeType = DEFAULT_MIME_TYPE;
        if (file.getName().endsWith("vue")) {
            mimeType = DEFAULT_MIME_TYPE;
        } else if (file != null) {
            mimeType = new MimetypesFileTypeMap().getContentType(file);
        }
        return mimeType;
    }
}
