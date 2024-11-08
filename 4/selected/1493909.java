package eu.planets_project.tb.impl.data.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import eu.planets_project.ifr.core.common.conf.PlanetsServerConfig;
import eu.planets_project.ifr.core.storage.api.DataRegistry;
import eu.planets_project.ifr.core.storage.api.DataRegistryFactory;
import eu.planets_project.ifr.core.storage.api.DigitalObjectManager.DigitalObjectNotFoundException;
import eu.planets_project.ifr.core.storage.api.DigitalObjectManager.DigitalObjectNotStoredException;
import eu.planets_project.services.datatypes.Content;
import eu.planets_project.services.datatypes.DigitalObject;
import eu.planets_project.tb.api.data.util.DataHandler;
import eu.planets_project.tb.api.data.util.DigitalObjectRefBean;
import eu.planets_project.tb.api.model.Experiment;
import eu.planets_project.tb.gui.UserBean;
import eu.planets_project.tb.gui.util.JSFUtil;
import eu.planets_project.tb.impl.system.BackendProperties;

/**
 * @author <a href="mailto:andrew.lindley@ait.ac.at">Andrew Lindley</a>
 * @since 27.10.2009
 *
 */
public class DataHandlerImpl implements DataHandler {

    DataRegistry dataReg = DataRegistryFactory.getDataRegistry();

    private Log log = LogFactory.getLog(DataHandlerImpl.class);

    private String localFileDirBase;

    private static final String IN_DIR_PATH = "/planets-testbed/inputdata";

    private String FileInDir;

    private static final String OUT_DIR_PATH = "/planets-testbed/outputdata";

    private String FileOutDir;

    private static final String FILE_DIR_PATH = "/planets-testbed/filestore";

    private String FileStoreDir;

    private String externallyReachableFiledir;

    Base64 base64 = new Base64();

    /**
	 * 
	 */
    public DataHandlerImpl() {
        readProperties();
    }

    /**
	 * 
	 */
    private void readProperties() {
        BackendProperties bp = new BackendProperties();
        localFileDirBase = BackendProperties.getTBFileDir();
        FileInDir = localFileDirBase + IN_DIR_PATH;
        FileOutDir = localFileDirBase + OUT_DIR_PATH;
        FileStoreDir = localFileDirBase + FILE_DIR_PATH;
        externallyReachableFiledir = bp.getExternallyReachableFiledir();
    }

    @Deprecated
    public URI getHttpFileRef(File localFileRef, boolean input) throws URISyntaxException, FileNotFoundException {
        if (!localFileRef.canRead()) {
            throw new FileNotFoundException(localFileRef + " not found");
        }
        HttpServletRequest req = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        String authority = req.getLocalName() + ":" + Integer.toString(req.getLocalPort());
        if (input) {
            return new URI("http", authority, "/planets-testbed/inputdata/" + localFileRef.getName(), null, null);
        } else {
            return new URI("http", authority, "/planets-testbed/outputdata/" + localFileRef.getName(), null, null);
        }
    }

    public URI storeBytestream(InputStream in, String name) throws IOException {
        DigitalObject.Builder dob = new DigitalObject.Builder(Content.byReference(in));
        dob.title(name);
        URI refname = this.storeDigitalObject(dob.build());
        log.debug("Stored file '" + name + "' under reference: " + refname);
        return refname;
    }

    public URI storeBytearray(byte[] b, String name) throws IOException {
        ByteArrayInputStream bin = new ByteArrayInputStream(b);
        return this.storeBytestream(bin, name);
    }

    public URI storeUriContent(URI u) throws MalformedURLException, IOException {
        URI ref = null;
        InputStream in = null;
        try {
            URLConnection c = u.toURL().openConnection();
            in = c.getInputStream();
            ref = this.storeBytestream(in, u.toString());
        } finally {
            in.close();
        }
        return ref;
    }

    public URI storeFile(File f) throws FileNotFoundException, IOException {
        return this.storeBytestream(new FileInputStream(f), f.getName());
    }

    public URI storeDigitalObject(DigitalObject dob, Experiment exp) {
        URI defaultDomUri = this.dataReg.getDefaultDigitalObjectManagerId();
        this.log.info("Attempting to store in data registry: " + defaultDomUri);
        UserBean currentUser = (UserBean) JSFUtil.getManagedObject("UserBean");
        String userid = ".";
        if (currentUser != null && currentUser.getUserid() != null) {
            userid = currentUser.getUserid();
        }
        URI baseUri = null;
        try {
            if (exp == null) {
                baseUri = new URI(defaultDomUri.toString() + "/testbed/users/" + userid + "/digitalobjects/");
            } else {
                baseUri = new URI(defaultDomUri.toString() + "/testbed/experiments/experiment-" + exp.getEntityID() + "/");
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return null;
        }
        log.info("Attempting to store in: " + baseUri);
        String name = dob.getTitle();
        if (name == null || "".equals(name)) {
            UUID randomSequence = UUID.randomUUID();
            name = exp.getExperimentSetup().getBasicProperties().getExperimentName() + "-" + randomSequence + ".digitalObject";
        }
        URI dobUri;
        try {
            dobUri = new URI(baseUri.getScheme(), baseUri.getUserInfo(), baseUri.getHost(), baseUri.getPort(), baseUri.getPath() + name, null, null);
        } catch (URISyntaxException e1) {
            e1.printStackTrace();
            return null;
        }
        log.info("Calling Data Registry List for " + baseUri);
        List<URI> storedDobs = dataReg.list(baseUri);
        if (storedDobs != null) {
            int unum = 1;
            while (storedDobs.contains(dobUri)) {
                try {
                    dobUri = new URI(baseUri.getScheme(), baseUri.getUserInfo(), baseUri.getHost(), baseUri.getPort(), baseUri.getPath() + unum + "-" + name, null, null);
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                    return null;
                }
                unum++;
            }
        }
        log.info("Attempting to store at: " + dobUri);
        try {
            dobUri = this.storeDigitalObject(dobUri, dob);
        } catch (DigitalObjectNotStoredException e) {
            log.error("Store failed! " + e);
            e.printStackTrace();
            return null;
        }
        log.info("Was store at: " + dobUri);
        return dobUri;
    }

    private URI storeDigitalObject(DigitalObject dob) {
        return this.storeDigitalObject(dob, null);
    }

    private URI storeDigitalObject(URI domUri, DigitalObject dob) throws DigitalObjectNotStoredException {
        return dataReg.storeAsNew(domUri, dob);
    }

    public DigitalObjectRefBean get(String id) {
        DigitalObjectRefBean dobr = null;
        try {
            dobr = this.findDOinDataRegistry(id);
        } catch (FileNotFoundException e1) {
            log.debug("File " + id + " not found in Data Registry. " + e1);
        }
        if (dobr == null) {
            try {
                dobr = this.findDOinTestbedCache(id);
            } catch (FileNotFoundException e) {
                log.debug("File " + id + " not found in Testbed File Cache. " + e);
            }
        }
        if (dobr == null) {
            log.error("Could not find any content for " + id);
        }
        return dobr;
    }

    private DigitalObjectRefBean findDOinTestbedCache(String id) throws FileNotFoundException {
        log.debug("Looking for " + id + " in the TB file cache.");
        File file = null;
        String name = null;
        if (id.contains("ROOT.war/planets-testbed")) {
            id = id.substring(id.lastIndexOf("ROOT.war") + 8);
            file = new File(localFileDirBase, id);
            name = getInputFileIndexEntryName(file);
        } else if (id.contains("ROOT.war\\planets-testbed")) {
            id = id.substring(id.lastIndexOf("ROOT.war") + 8);
            file = new File(localFileDirBase, id);
            name = getOutputFileIndexEntryName(file);
        } else {
            file = new File(FileStoreDir, id);
            name = getIndexFileEntryName(file);
        }
        if (id.startsWith("\\")) {
            id.replaceAll("\\\\", "/");
        }
        if (file.exists()) {
            this.log.debug("Found file: " + file.getAbsolutePath());
            return new DigitalObjectRefBean(name, id, file);
        }
        throw new FileNotFoundException("Could not find file " + id);
    }

    private DigitalObjectRefBean findDOinDataRegistry(String id) throws FileNotFoundException {
        URI domUri = null;
        try {
            domUri = new URI(id);
        } catch (URISyntaxException e) {
            throw new FileNotFoundException("Could not find file " + id);
        }
        if (this.dataReg.hasDigitalObjectManager(domUri)) {
            try {
                this.log.info("Retrieving Digital Object at " + domUri);
                DigitalObject digitalObject = this.dataReg.retrieve(domUri);
                String leafname = domUri.getPath();
                if (leafname.endsWith("/")) {
                    leafname = leafname.substring(0, leafname.length() - 1);
                }
                if (leafname.contains("/")) {
                    leafname = leafname.substring(leafname.lastIndexOf("/") + 1);
                }
                return new DigitalObjectRefBean(leafname, domUri.toString(), domUri, digitalObject);
            } catch (DigitalObjectNotFoundException e) {
                throw new FileNotFoundException("Could not find file " + id);
            }
        }
        throw new FileNotFoundException("Could not find file " + id);
    }

    public List<DigitalObject> convertFileRefsToURLAccessibleDigos(Collection<String> localFileRefs) {
        List<DigitalObject> ret = new ArrayList<DigitalObject>();
        for (String fileRef : localFileRefs) {
            try {
                URI uriRef = new URI(fileRef);
                this.log.info("Retrieving Digital Object at " + uriRef);
                DigitalObject refByValueDigo = DataRegistryFactory.retrieveAsTbReference(dataReg, new URI(fileRef));
                ret.add(refByValueDigo);
            } catch (DigitalObjectNotFoundException e) {
                e.printStackTrace();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    /**
     * @param name
     * @return
     */
    public static String createShortDOName(String name) {
        int lastSlash = name.lastIndexOf("/");
        if (lastSlash != -1) {
            return name.substring(lastSlash + 1, name.length());
        }
        return name;
    }

    private String getInputFileIndexEntryName(File localFileRef) {
        if ((localFileRef != null) && (localFileRef.canRead())) {
            try {
                Properties props = this.getInputDirIndex();
                if (props.containsKey(localFileRef.getName())) {
                    return props.getProperty(localFileRef.getName());
                }
            } catch (IOException e) {
                log.debug("index file name for " + localFileRef.getName() + " was not found");
            }
        }
        return localFileRef.getName();
    }

    private String getOutputFileIndexEntryName(File localFileRef) {
        if ((localFileRef != null) && (localFileRef.canRead())) {
            try {
                Properties props = this.getOutputDirIndex();
                if (props.containsKey(localFileRef.getName())) {
                    return props.getProperty(localFileRef.getName());
                }
            } catch (IOException e) {
                log.debug("index file name for " + localFileRef.getName() + " was not found");
            }
        }
        return localFileRef.getName();
    }

    /**
     * checks if for the input files an index file exists and if not creates it. As for the input
     * files a random number is created, the original fileName is stored within the index property file
     * @throws IOException 
     */
    private Properties getInputDirIndex() throws IOException {
        return getIndex(true);
    }

    /**
     * checks if for the input files an index file exists and if not creates it. As for the input
     * files a random number is created, the original fileName is stored within the index property file
     * @throws IOException 
     */
    private Properties getOutputDirIndex() throws IOException {
        return getIndex(false);
    }

    /**
     * Returns the index properties either of the input or for the output files of 
     * and experiment
     * @param bInputIndex
     * @return
     */
    private Properties getIndex(boolean bInputIndex) throws IOException {
        File dir;
        if (bInputIndex) {
            dir = new File(FileInDir);
        } else {
            dir = new File(FileOutDir);
        }
        dir.mkdirs();
        File f = new File(dir, "index_names.properties");
        if (!((f.exists()) && (f.canRead()))) {
            f.createNewFile();
        }
        Properties properties = new Properties();
        FileInputStream ResourceFile = new FileInputStream(f);
        properties.load(ResourceFile);
        return properties;
    }

    /**
     * checks if index file exists and if not creates it. As for the input/output
     * files a random number is created, the original fileName is stored within the index property file
     * @throws IOException 
     */
    private Properties getIndex() throws IOException {
        File dir = new File(FileStoreDir);
        dir.mkdirs();
        File f = new File(dir, "index_names.properties");
        if (!((f.exists()) && (f.canRead()))) {
            f.createNewFile();
        }
        Properties properties = new Properties();
        FileInputStream ResourceFile = new FileInputStream(f);
        properties.load(ResourceFile);
        return properties;
    }

    /**
     * Fetches for a given file (the resource's physical file name on the disk) its
     * original logical name which is stored within an index.
     * e.g. ce37d69b-64c0-4476-9040-72512f07bb49.TIF to Test1.TIF
     * @param sFileRandomNumber the corresponding file name or its logical random number if none is available
     */
    private String getIndexFileEntryName(File sFileRandomNumber) {
        log.debug("Looking for name of: " + sFileRandomNumber.getAbsolutePath());
        if (sFileRandomNumber != null) {
            try {
                Properties props = this.getIndex();
                if (props.containsKey(sFileRandomNumber.getName())) {
                    return props.getProperty(sFileRandomNumber.getName());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        log.warn("Could not find name for File: " + sFileRandomNumber.getName());
        return sFileRandomNumber.getName();
    }

    /**
     * Creates a temporary that's deleted on exit.
     * @return
     * @throws IOException
     */
    public static File createTemporaryFile() throws IOException {
        int lowerBound = 1;
        int upperBound = 9999;
        int random = (int) (lowerBound + Math.random() * (upperBound - lowerBound));
        File f = File.createTempFile("dataHandlerTemp" + random, null);
        f.deleteOnExit();
        return f;
    }

    public static byte[] decodeToByteArray(String sBase64ByteArrayString) {
        return Base64.decodeBase64(sBase64ByteArrayString.getBytes());
    }

    public static String encodeToBase64ByteArrayString(File src) throws IOException {
        byte[] b = new byte[(int) src.length()];
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(src);
            fis.read(b);
        } catch (IOException e) {
            throw e;
        } finally {
            fis.close();
        }
        return Base64.encodeBase64(b).toString();
    }

    /**
     * @param inputStream
     * @param tmpFile
     * @throws IOException 
     */
    public static void storeStreamInFile(InputStream in, File f) throws IOException {
        f.createNewFile();
        FileOutputStream fos = new FileOutputStream(f);
        byte[] b = new byte[1024];
        int read_size;
        while ((read_size = in.read(b)) != -1) {
            fos.write(b, 0, read_size);
            fos.flush();
        }
        fos.close();
    }

    /**
     * TODO A factory that looks up the DataHandler in the context:
     * @return The DataHandler
     */
    public static DataHandler findDataHandler() {
        return new DataHandlerImpl();
    }

    public File createTempFileInExternallyAccessableDir() throws IOException {
        log.debug("createTempFileInExternallyAccessableDir");
        File f = createTemporaryFile();
        log.debug("temp file location: " + f.getAbsolutePath());
        File dir = new File(externallyReachableFiledir);
        log.debug("directory " + externallyReachableFiledir + " exists? " + dir.exists());
        File target = new File(dir.getAbsoluteFile() + "/" + f.getName());
        log.debug("target: " + dir.getAbsoluteFile() + "/" + f.getName() + " exists: " + target.exists());
        target.deleteOnExit();
        FileInputStream fis = new FileInputStream(f);
        FileOutputStream fos = new FileOutputStream(target);
        try {
            byte[] buf = new byte[1024];
            int i = 0;
            while ((i = fis.read(buf)) != -1) {
                fos.write(buf, 0, i);
            }
        } catch (Exception e) {
            throw new IOException("Problems moving data from temp to externally reachable jboss-web deployer dir" + e);
        } finally {
            if (fis != null) fis.close();
            if (fos != null) fos.close();
            f.delete();
        }
        log.debug("rename success? target " + target.getAbsolutePath() + " exists?: " + target.exists() + " f deleted?: " + f.exists());
        return target;
    }

    public File copyLocalFileAsTempFileInExternallyAccessableDir(String localFileRef) throws IOException {
        log.debug("copyLocalFileAsTempFileInExternallyAccessableDir");
        File f = this.createTempFileInExternallyAccessableDir();
        FileChannel srcChannel = new FileInputStream(localFileRef).getChannel();
        FileChannel dstChannel = new FileOutputStream(f).getChannel();
        log.debug("before transferring via FileChannel from src-inputStream: " + localFileRef + " to dest-outputStream: " + f.getAbsolutePath());
        dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
        srcChannel.close();
        dstChannel.close();
        log.debug("copyLocalFileAsTempFileInExternallyAccessableDir returning: " + f.getAbsolutePath());
        return f;
    }

    public URI getHttpFileRef(File tempFileInExternalDir) throws URISyntaxException, FileNotFoundException {
        if (!tempFileInExternalDir.canRead()) {
            throw new FileNotFoundException("getHttpFileRef for " + tempFileInExternalDir + " not found");
        }
        String authority = PlanetsServerConfig.getHostname() + ":" + PlanetsServerConfig.getPort();
        URI ret = new URI("http", authority, "/planets-testbed/" + tempFileInExternalDir.getName(), null, null);
        log.debug("returning httpFileRef: " + ret + " for: " + tempFileInExternalDir.getAbsolutePath());
        return ret;
    }
}
