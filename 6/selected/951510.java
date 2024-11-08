package tufts.oki.repository.fedora;

import osid.repository.*;
import tufts.oki.shared.TypeIterator;
import java.util.prefs.Preferences;
import java.util.*;
import java.net.*;
import java.io.*;
import javax.swing.JOptionPane;
import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.Unmarshaller;
import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.mapping.MappingException;
import java.io.OutputStreamWriter;
import org.xml.sax.InputSource;
import javax.xml.namespace.QName;
import fedora.server.types.gen.*;
import fedora.server.utilities.DateUtility;
import org.apache.axis.encoding.ser.*;
import org.apache.axis.types.NonNegativeInteger;
import org.apache.axis.client.Service;
import javax.xml.rpc.ServiceException;
import java.rmi.RemoteException;
import org.apache.commons.net.ftp.*;
import fedora.server.management.FedoraAPIM;
import fedora.server.utilities.StreamUtility;
import fedora.client.ingest.AutoIngestor;

public class Repository implements osid.repository.Repository {

    public final boolean DEBUG = false;

    public static final String DC_NAMESPACE = "dc:";

    public static final String[] DC_FIELDS = { "title", "creator", "subject", "date", "type", "format", "identifier", "collection", "coverage" };

    private Preferences prefs = null;

    private String displayName = "";

    private String description = "";

    private URL address = null;

    private String userName = null;

    private String password = null;

    private String conf = null;

    private java.util.Vector recordStructures = new java.util.Vector();

    private java.util.Vector assetTypes = new java.util.Vector();

    private java.util.Vector searchTypes = new java.util.Vector();

    private java.util.Vector assets = new java.util.Vector();

    private osid.shared.Id id = null;

    private URL configuration = null;

    private Properties fedoraProperties;

    /** Creates a new instance of Repository */
    public Repository(String conf, String id, String displayName, String description, URL address, String userName, String password) throws osid.repository.RepositoryException {
        System.out.println("Repository CONSTRUCTING[" + conf + ", " + id + ", " + displayName + ", " + description + ", " + address + ", " + userName + ", " + password + "] " + this);
        try {
            this.id = new PID(id);
        } catch (osid.shared.SharedException sex) {
        }
        this.displayName = displayName;
        this.description = description;
        this.address = address;
        this.userName = userName;
        this.password = password;
        this.conf = conf;
        this.configuration = getResource(conf);
        setFedoraProperties(configuration);
        loadFedoraObjectAssetTypes();
        searchTypes.add(new SearchType("Search"));
        searchTypes.add(new SearchType("Advanced Search"));
    }

    /** sets a soap call to perform all digital repository operations
     * @throws RepositoryException if Soap call can't be made
     */
    public void setFedoraProperties(Properties fedoraProperties) {
        this.fedoraProperties = fedoraProperties;
    }

    public void setFedoraProperties(java.net.URL conf) {
        String url = address.getProtocol() + "://" + address.getHost() + ":" + address.getPort() + "/" + address.getFile();
        System.out.println("FEDORA Address = " + url);
        fedoraProperties = new Properties();
        try {
            System.out.println("Fedora Properties " + conf);
            prefs = FedoraUtils.getPreferences(this);
            fedoraProperties.setProperty("url.fedora.api", prefs.get("url.fedora.api", ""));
            fedoraProperties.setProperty("url.fedora.type", prefs.get("url.fedora.type", ""));
            fedoraProperties.setProperty("url.fedora.soap.access", url + prefs.get("url.fedora.soap.access", ""));
            fedoraProperties.setProperty("url.fedora.get", url + prefs.get("url.fedora.get", ""));
            fedoraProperties.setProperty("fedora.types", prefs.get("fedora.types", ""));
        } catch (Exception ex) {
            System.out.println("Unable to load fedora Properties" + ex);
        }
    }

    private void loadFedoraObjectAssetTypes() {
        try {
            Vector fedoraTypesVector = FedoraUtils.stringToVector(fedoraProperties.getProperty("fedora.types"));
            Iterator i = fedoraTypesVector.iterator();
            while (i.hasNext()) {
                createFedoraObjectAssetType((String) i.next());
            }
        } catch (Throwable t) {
            System.out.println("Unable to load fedora types" + t.getMessage());
        }
    }

    public Properties getFedoraProperties() {
        return fedoraProperties;
    }

    public URL getConfiguration() {
        return configuration;
    }

    /**To create AssetTypes that don't exist when repository is loaded. OKI NEEDS to add such a feature
     *@ param String type
     *@ return FedoraObjectAssetType
     *@throws osid.repository.RepositoryException
     */
    public FedoraObjectAssetType createFedoraObjectAssetType(String type) throws osid.repository.RepositoryException {
        java.util.Iterator i = assetTypes.iterator();
        while (i.hasNext()) {
            FedoraObjectAssetType fedoraObjectAssetType = (FedoraObjectAssetType) i.next();
            if (fedoraObjectAssetType.getType().equals(type)) return fedoraObjectAssetType;
        }
        FedoraObjectAssetType fedoraObjectAssetType = new FedoraObjectAssetType(this, type);
        osid.repository.RecordStructureIterator iter = fedoraObjectAssetType.getRecordStructures();
        while (iter.hasNextRecordStructure()) {
            osid.repository.RecordStructure recordStructure = (osid.repository.RecordStructure) iter.nextRecordStructure();
            if (recordStructures.indexOf(recordStructure) < 0) recordStructures.add(recordStructure);
        }
        assetTypes.add(fedoraObjectAssetType);
        return fedoraObjectAssetType;
    }

    /** AssetTypes are loaded from the configuration file. In future versions these will be loaded directly from FEDORA.
     *  OKI Team recommends having  an object in digital repository that maintains this information.
     * @ throws RepositoryException
     */
    private void loadAssetTypes() throws osid.repository.RepositoryException {
        FedoraObjectAssetType fedoraObjectAssetType = new FedoraObjectAssetType(this, "TUFTS_STD_IMAGE");
    }

    public FedoraObjectAssetType getAssetType(String type) throws osid.repository.RepositoryException {
        java.util.Iterator i = assetTypes.iterator();
        while (i.hasNext()) {
            FedoraObjectAssetType fedoraObjectAssetType = (FedoraObjectAssetType) i.next();
            if (fedoraObjectAssetType.getType().equals(type)) return fedoraObjectAssetType;
        }
        return createFedoraObjectAssetType(type);
    }

    public boolean isFedoraObjectAssetTypeSupported(String type) {
        java.util.Iterator i = assetTypes.iterator();
        while (i.hasNext()) {
            FedoraObjectAssetType fedoraObjectAssetType = (FedoraObjectAssetType) i.next();
            if (fedoraObjectAssetType.getType().equals(type)) return true;
        }
        return false;
    }

    /**     Create a new Asset of this AssetType to this Repository.  The implementation of this method sets the Id for the new object.
     *     @return Asset
     *     @throws RepositoryException if there is a general failure or if the Type is unknown
     */
    public osid.repository.Asset createAsset(String displayName, String description, osid.shared.Type assetType) throws osid.repository.RepositoryException {
        if (!assetTypes.contains(assetType)) assetTypes.add(assetType);
        try {
            osid.repository.Asset obj = new Asset(this, displayName, description, assetType);
            assets.add(obj);
            return obj;
        } catch (osid.shared.SharedException ex) {
            throw new osid.repository.RepositoryException("DR.createAsset" + ex.getMessage());
        }
    }

    /**     Delete an Asset from this Repository.
     *     @param osid.shared.Id
     *     @throws RepositoryException if there is a general failure  or if the object has not been created
     */
    public void deleteAsset(osid.shared.Id assetId) throws osid.repository.RepositoryException {
        throw new osid.repository.RepositoryException(osid.OsidException.UNIMPLEMENTED);
    }

    public osid.repository.RecordStructureIterator getRecordStructuresByType(osid.shared.Type recordStructureType) throws osid.repository.RepositoryException {
        throw new osid.repository.RepositoryException(osid.OsidException.UNIMPLEMENTED);
    }

    /**     Get all the AssetTypes in this Repository.  AssetTypes are used to categorize Assets.  Iterators return a set, one at a time.  The Iterator's hasNext method returns true if there are additional objects available; false otherwise.  The Iterator's next method returns the next object.
     *     @return osid.shared.TypeIterator  The order of the objects returned by the Iterator is not guaranteed.
     *     @throws RepositoryException if there is a general failure
     */
    public osid.shared.TypeIterator getAssetTypes() throws osid.repository.RepositoryException {
        return new TypeIterator(assetTypes);
    }

    /**     Get all the Assets in this Repository.  Iterators return a set, one at a time.  The Iterator's hasNext method returns true if there are additional objects available; false otherwise.  The Iterator's next method returns the next object.
     *     @return AssetIterator  The order of the objects returned by the Iterator is not guaranteed.
     *     @throws RepositoryException if there is a general failure
     */
    public osid.repository.AssetIterator getAssets() throws osid.repository.RepositoryException {
        Vector assetVector = new Vector();
        String assetId = "tufts:";
        String location = null;
        try {
            for (int i = 1; i <= 10; i++) {
                osid.repository.Asset asset = new Asset(new PID(assetId + i), this);
                assetVector.add(asset);
            }
        } catch (Throwable t) {
            throw new RepositoryException(t.getMessage());
        }
        return (osid.repository.AssetIterator) new AssetIterator(assetVector);
    }

    /**     Get the description for this Repository.
     *     @return String the name
     *     @throws RepositoryException if there is a general failure
     */
    public String getDescription() throws osid.repository.RepositoryException {
        return this.description;
    }

    /**     Get the name for this Repository.
     *     @return String the name
     *     @throws RepositoryException if there is a general failure
     */
    public String getDisplayName() throws osid.repository.RepositoryException {
        return displayName;
    }

    /**     Get the Unique Id for this Repository.
     *     @return osid.shared.Id Unique Id this is usually set by a create method's implementation
     *     @throws RepositoryException if there is a general failure
     */
    public osid.shared.Id getId() throws osid.repository.RepositoryException {
        return id;
    }

    /**     Get all the InfoStructures in this Repository.  InfoStructures are used to categorize information about Assets.  Iterators return a set, one at a time.  The Iterator's hasNext method returns true if there are additional objects available; false otherwise.  The Iterator's next method returns the next object.
     *     @return InfoStructureIterator  The order of the objects returned by the Iterator is not guaranteed.
     *     @throws RepositoryException if there is a general failure
     */
    public osid.repository.RecordStructureIterator getRecordStructures() throws osid.repository.RepositoryException {
        return (osid.repository.RecordStructureIterator) new RecordStructureIterator(recordStructures);
    }

    /**     Get the InfoStructures that this AssetType must support.  InfoStructures are used to categorize information about Assets.  Iterators return a set, one at a time.  The Iterator's hasNext method returns true if there are additional objects available; false otherwise.  The Iterator's next method returns the next object.
     *     @return InfoStructureIterator  The order of the objects returned by the Iterator is not guaranteed.
     *     @throws RepositoryException if there is a general failure
     */
    public osid.repository.RecordStructureIterator getMandatoryRecordStructures(osid.shared.Type assetType) throws osid.repository.RepositoryException {
        return new RecordStructureIterator(new java.util.Vector());
    }

    /**     Get all the SearchTypes supported by this Repository.  Iterators return a set, one at a time.  The Iterator's hasNext method returns true if there are additional objects available; false otherwise.  The Iterator's next method returns the next object.
     *     @return osid.shared.TypeIterator  The order of the objects returned by the Iterator is not guaranteed.
     *     @throws RepositoryException if there is a general failure
     */
    public osid.shared.TypeIterator getSearchTypes() throws osid.repository.RepositoryException {
        return new TypeIterator(searchTypes);
    }

    /**     Get the the StatusTypes of this Asset.
     *     @return osid.shared.Type
     *     @throws RepositoryException if there is a general failure
     */
    public osid.shared.Type getStatus(osid.shared.Id assetId) throws osid.repository.RepositoryException {
        throw new osid.repository.RepositoryException(osid.OsidException.UNIMPLEMENTED);
    }

    /**     Get all the StatusTypes supported by this Repository.  Iterators return a set, one at a time.  The Iterator's hasNext method returns true if there are additional objects available; false otherwise.  The Iterator's next method returns the next object.
     *     @return osid.shared.TypeIterator  The order of the objects returned by the Iterator is not guaranteed.
     *     @throws RepositoryException if there is a general failure
     */
    public osid.shared.TypeIterator getStatusTypes() throws osid.repository.RepositoryException {
        throw new osid.repository.RepositoryException(osid.OsidException.UNIMPLEMENTED);
    }

    /**     Update the description for this Repository.
     *     @param String description
     *     @throws RepositoryException if there is a general failure
     */
    public void updateDescription(String description) throws osid.repository.RepositoryException {
        this.description = description;
    }

    /**     Update the "tufts/dr/fedora/temp/"name for this Repository.
     *     @param String name
     *     @throws RepositoryException if there is a general failure
     */
    public void updateDisplayName(String displayName) throws osid.repository.RepositoryException {
        this.displayName = displayName;
    }

    /**     Set the Asset's status Type accordingly and relax validation checking when creating InfoRecords and InfoFields or updating InfoField's values.
     *     @param osid.shared.Id
     *     @return boolean
     *     @throws RepositoryException if there is a general failure
     */
    public void invalidateAsset(osid.shared.Id assetId) throws osid.repository.RepositoryException {
        throw new osid.repository.RepositoryException(osid.OsidException.UNIMPLEMENTED);
    }

    /**     Validate all the InfoRecords for an Asset and set its status Type accordingly.  If the Asset is valid, return true; otherwise return false.  The implementation may throw an Exception for any validation failures and use the Exception's message to identify specific causes.
     *     @param osid.shared.Id
     *     @return boolean
     *     @throws RepositoryException if there is a general failure
     */
    public boolean validateAsset(osid.shared.Id assetId) throws osid.repository.RepositoryException {
        throw new osid.repository.RepositoryException(osid.OsidException.UNIMPLEMENTED);
    }

    public osid.shared.Id copyAsset(osid.repository.Asset asset) throws osid.repository.RepositoryException {
        throw new osid.repository.RepositoryException(osid.OsidException.UNIMPLEMENTED);
    }

    public osid.repository.Asset getAsset(osid.shared.Id assetId) throws osid.repository.RepositoryException {
        Condition[] condition = new Condition[1];
        condition[0] = new Condition();
        condition[0].setProperty("pid");
        condition[0].setOperator(ComparisonOperator.eq);
        try {
            System.out.println("Searching for object =" + assetId.getIdString());
            condition[0].setValue(assetId.getIdString());
        } catch (osid.shared.SharedException ex) {
            throw new osid.repository.RepositoryException(ex.getMessage());
        }
        SearchCriteria searchCriteria = new SearchCriteria();
        searchCriteria.setConditions(condition);
        searchCriteria.setMaxReturns("1");
        osid.repository.AssetIterator mAssetIterator = FedoraSoapFactory.advancedSearch(this, searchCriteria);
        if (mAssetIterator.hasNextAsset()) return mAssetIterator.nextAsset(); else throw new osid.repository.RepositoryException(osid.repository.RepositoryException.UNKNOWN_ID);
    }

    public osid.repository.Asset getAsset(osid.shared.Id assetId, long date) throws osid.repository.RepositoryException {
        throw new osid.repository.RepositoryException(osid.OsidException.UNIMPLEMENTED);
    }

    public osid.shared.PropertiesIterator getProperties() throws osid.repository.RepositoryException {
        throw new osid.repository.RepositoryException(osid.OsidException.UNIMPLEMENTED);
    }

    public osid.shared.Properties getPropertiesByType(osid.shared.Type propertiesType) throws osid.repository.RepositoryException {
        throw new osid.repository.RepositoryException(osid.OsidException.UNIMPLEMENTED);
    }

    public osid.shared.TypeIterator getPropertyTypes() throws osid.repository.RepositoryException {
        throw new osid.repository.RepositoryException(osid.OsidException.UNIMPLEMENTED);
    }

    public osid.shared.LongValueIterator getAssetDates(osid.shared.Id assetId) throws osid.repository.RepositoryException {
        throw new osid.repository.RepositoryException(osid.OsidException.UNIMPLEMENTED);
    }

    public osid.repository.AssetIterator getAssets(java.io.Serializable searchCriteria, osid.shared.Type searchType) throws osid.repository.RepositoryException {
        System.out.println("SEARCHING FEDORA = " + this.fedoraProperties.getProperty("url.fedora.soap.access"));
        SearchCriteria lSearchCriteria = (SearchCriteria) searchCriteria;
        if (searchType.getKeyword().equals("Search")) {
            return FedoraSoapFactory.search(this, lSearchCriteria);
        } else if (searchType.getKeyword().equals("Advanced Search")) {
            return FedoraSoapFactory.advancedSearch(this, lSearchCriteria);
        } else {
            throw new osid.repository.RepositoryException(osid.repository.RepositoryException.UNKNOWN_TYPE);
        }
    }

    public osid.shared.Type getType() throws osid.repository.RepositoryException {
        return new Type("repository", "tufts.edu", "fedora_image", "");
    }

    public osid.repository.Asset getAssetByDate(osid.shared.Id id, long date) throws osid.repository.RepositoryException {
        return getAsset(id, date);
    }

    public osid.repository.AssetIterator getAssetsBySearch(java.io.Serializable serializable, osid.shared.Type type, osid.shared.Properties properties) throws osid.repository.RepositoryException {
        return getAssets(serializable, type);
    }

    public osid.repository.AssetIterator getAssetsByType(osid.shared.Type type) throws osid.repository.RepositoryException {
        return getAssets();
    }

    public boolean supportsUpdate() throws osid.repository.RepositoryException {
        return false;
    }

    public boolean supportsVersioning() throws osid.repository.RepositoryException {
        return false;
    }

    public osid.shared.Id ingest(String fileName, String templateFileName, String fileType, File file, Properties properties) throws osid.repository.RepositoryException, java.net.SocketException, java.io.IOException, osid.shared.SharedException, javax.xml.rpc.ServiceException {
        long sTime = System.currentTimeMillis();
        if (DEBUG) System.out.println("INGESTING FILE TO FEDORA:fileName =" + fileName + "fileType =" + fileType + "t = 0");
        String host = FedoraUtils.getFedoraProperty(this, "admin.ftp.address");
        String url = FedoraUtils.getFedoraProperty(this, "admin.ftp.url");
        int port = Integer.parseInt(FedoraUtils.getFedoraProperty(this, "admin.ftp.port"));
        String userName = FedoraUtils.getFedoraProperty(this, "admin.ftp.username");
        String password = FedoraUtils.getFedoraProperty(this, "admin.ftp.password");
        String directory = FedoraUtils.getFedoraProperty(this, "admin.ftp.directory");
        FTPClient client = new FTPClient();
        client.connect(host, port);
        client.login(userName, password);
        client.changeWorkingDirectory(directory);
        client.setFileType(FTP.BINARY_FILE_TYPE);
        client.storeFile(fileName, new FileInputStream(file));
        client.logout();
        client.disconnect();
        if (DEBUG) System.out.println("INGESTING FILE TO FEDORA: Writting to FTP Server:" + (System.currentTimeMillis() - sTime));
        fileName = url + fileName;
        int BUFFER_SIZE = 10240;
        StringBuffer sb = new StringBuffer();
        String s = new String();
        BufferedInputStream fis = new BufferedInputStream(new FileInputStream(new File(getResource(templateFileName).getFile().replaceAll("%20", " "))));
        byte[] buf = new byte[BUFFER_SIZE];
        int ch;
        int len;
        while ((len = fis.read(buf)) > 0) {
            s = s + new String(buf);
        }
        fis.close();
        if (DEBUG) System.out.println("INGESTING FILE TO FEDORA: Read Mets File:" + (System.currentTimeMillis() - sTime));
        String r = updateMetadata(s, fileName, file.getName(), fileType, properties);
        if (DEBUG) System.out.println("INGESTING FILE TO FEDORA: Resplaced Metadata:" + (System.currentTimeMillis() - sTime));
        File METSfile = File.createTempFile("vueMETSMap", ".xml");
        FileOutputStream fos = new FileOutputStream(METSfile);
        fos.write(r.getBytes());
        fos.close();
        AutoIngestor a = new AutoIngestor(address.getHost(), address.getPort(), FedoraUtils.getFedoraProperty(this, "admin.fedora.username"), FedoraUtils.getFedoraProperty(this, "admin.fedora.username"));
        String pid = a.ingestAndCommit(new FileInputStream(METSfile), "Test Ingest");
        if (DEBUG) System.out.println("INGESTING FILE TO FEDORA: Ingest complete:" + (System.currentTimeMillis() - sTime));
        System.out.println(" METSfile= " + METSfile.getPath() + " PID = " + pid);
        return new PID(pid);
    }

    private String updateMetadata(String s, String fileLocation, String fileTitle, String fileType, Properties dcFields) {
        Calendar calendar = new GregorianCalendar();
        java.text.SimpleDateFormat date = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        String created = date.format(calendar.getTime());
        String dcMetadata;
        s = s.replaceAll("%file.location%", fileLocation).trim();
        s = s.replaceAll("%file.title%", fileTitle);
        s = s.replaceAll("%file.type%", fileType);
        s = s.replaceAll("%file.created%", created);
        s = s.replaceAll("%dc.Metadata%", getMetadataString(dcFields));
        return s;
    }

    private java.net.URL getResource(String name) {
        java.net.URL url = null;
        java.io.File f = new java.io.File(name);
        if (f.exists()) {
            try {
                url = f.toURL();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (url == null) url = getClass().getResource(name);
        System.out.println("fedora.conf = " + url.getFile());
        return url;
    }

    public static boolean isSupportedMetadataField(String field) {
        for (int i = 0; i < DC_FIELDS.length; i++) {
            if (DC_FIELDS[i].equalsIgnoreCase(field)) return true;
        }
        return false;
    }

    public static String getMetadataString(Properties dcFields) {
        String metadata = "";
        Enumeration e = dcFields.keys();
        while (e.hasMoreElements()) {
            String field = (String) e.nextElement();
            if (isSupportedMetadataField(field)) metadata += "<" + DC_NAMESPACE + field + ">" + dcFields.getProperty(field) + "</" + DC_NAMESPACE + field + ">";
        }
        return metadata;
    }

    public String getAddress() {
        return this.address.getHost();
    }

    public void setAddress(String address) throws java.net.MalformedURLException {
        this.address = new URL("http", address, 8080, "fedora/");
    }

    public String getUserName() {
        return this.userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword() {
        this.password = password;
    }

    public String getConf() {
        return this.conf;
    }

    public void setConf(String conf) {
        this.conf = conf;
    }

    public Preferences getPrefernces() {
        return this.prefs;
    }

    public void setConf(Preferences prefs) {
        this.prefs = prefs;
    }
}
