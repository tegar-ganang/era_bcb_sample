package net.sourceforge.processdash.tool.bridge.client;

import static net.sourceforge.processdash.tool.bridge.ResourceFilterFactory.INCLUDE_PARAM;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import net.sourceforge.processdash.tool.bridge.OfflineLockStatus;
import net.sourceforge.processdash.tool.bridge.OfflineLockStatusListener;
import net.sourceforge.processdash.tool.bridge.ResourceBridgeConstants;
import net.sourceforge.processdash.tool.bridge.ResourceCollection;
import net.sourceforge.processdash.tool.bridge.ResourceCollectionInfo;
import net.sourceforge.processdash.tool.bridge.ResourceCollectionType;
import net.sourceforge.processdash.tool.bridge.ResourceFilterFactory;
import net.sourceforge.processdash.tool.bridge.ResourceListing;
import net.sourceforge.processdash.tool.bridge.report.ListingHashcodeCalculator;
import net.sourceforge.processdash.tool.bridge.report.ResourceCollectionDiff;
import net.sourceforge.processdash.tool.bridge.report.ResourceContentStream;
import net.sourceforge.processdash.tool.bridge.report.XmlCollectionListing;
import net.sourceforge.processdash.util.ClientHttpRequest;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.HTTPUtils;
import net.sourceforge.processdash.util.ProfTimer;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.lock.AlreadyLockedException;
import net.sourceforge.processdash.util.lock.LockFailureException;
import net.sourceforge.processdash.util.lock.LockUncertainException;
import net.sourceforge.processdash.util.lock.NotLockedException;

public class ResourceBridgeClient implements ResourceBridgeConstants {

    static final String CLIENT_VERSION = "1.0";

    ResourceCollection localCollection;

    FilenameFilter syncDownOnlyFiles;

    String remoteUrl;

    String userName;

    String userId;

    String sourceIdentifier;

    String extraLockData;

    OfflineLockStatus offlineLockStatus;

    OfflineLockStatusListener offlineLockStatusListener;

    private static final Logger logger = Logger.getLogger(ResourceBridgeClient.class.getName());

    public ResourceBridgeClient(ResourceCollection localCollection, String remoteUrl, FilenameFilter syncDownOnlyFiles) {
        this.localCollection = localCollection;
        this.remoteUrl = remoteUrl;
        this.syncDownOnlyFiles = syncDownOnlyFiles;
        this.userId = getUserId();
        this.offlineLockStatus = OfflineLockStatus.NotLocked;
    }

    public synchronized void setSourceIdentifier(String sourceIdentifier) {
        this.sourceIdentifier = sourceIdentifier;
    }

    public synchronized void setExtraLockData(String extraLockData) {
        this.extraLockData = extraLockData;
    }

    public void setOfflineLockStatusListener(OfflineLockStatusListener l) {
        this.offlineLockStatusListener = l;
    }

    public OfflineLockStatus getOfflineLockStatus() {
        return offlineLockStatus;
    }

    private void setOfflineLockStatus(OfflineLockStatus s) {
        this.offlineLockStatus = s;
        if (offlineLockStatusListener != null) offlineLockStatusListener.setOfflineLockStatus(s);
    }

    public synchronized boolean syncDown() throws IOException {
        return syncDown(null);
    }

    public synchronized boolean syncDown(SyncFilter filter) throws IOException {
        ProfTimer pt = new ProfTimer(logger, "ResourceBridgeClient.syncDown[" + remoteUrl + "]");
        if (hashesMatch()) {
            pt.click("checked hashes - match");
            return false;
        }
        pt.click("checked hashes - mismatch");
        ResourceCollectionDiff diff = getDiff();
        applySyncFilter(diff, filter);
        pt.click("Computed local-vs-remote diff");
        if (diff == null || diff.noDifferencesFound()) return false;
        for (String resourceName : diff.getOnlyInA()) {
            logger.fine("deleting local resource " + resourceName);
            localCollection.deleteResource(resourceName);
        }
        List includes = new ArrayList();
        addMultiple(includes, INCLUDE_PARAM, diff.getOnlyInB());
        addMultiple(includes, INCLUDE_PARAM, diff.getDiffering());
        if (!includes.isEmpty()) downloadFiles(makeGetRequest(DOWNLOAD_ACTION, includes));
        pt.click("Copied down changes");
        return true;
    }

    /**
     * Apply changes to the collection on the server to make it match the
     * contents of the local collection.
     * 
     * @return true if any changes were made, false if the collections were
     *         already in sync.
     * @throws LockFailureException
     *                 if this client does not own a write lock on the server
     *                 collection
     * @throws IOException
     *                 if any other problem prevents the sync from succeeding
     */
    public synchronized boolean syncUp() throws IOException, LockFailureException {
        return syncUp(null);
    }

    public synchronized boolean syncUp(SyncFilter filter) throws IOException, LockFailureException {
        if (userName == null) throw new NotLockedException();
        ProfTimer pt = new ProfTimer(logger, "ResourceBridgeClient.syncUp[" + remoteUrl + "]");
        ResourceCollectionDiff diff = getDiff();
        applySyncFilter(diff, filter);
        pt.click("Computed local-vs-remote diff");
        if (diff == null || diff.noDifferencesFound()) {
            logger.finer("no changes to sync up");
            return false;
        }
        boolean madeChange = false;
        List<String> filesToDownload = new ArrayList<String>();
        if (!diff.getOnlyInB().isEmpty()) {
            List<String> params = new ArrayList<String>();
            for (String resourceName : diff.getOnlyInB()) {
                if (isSyncDownOnly(resourceName)) {
                    filesToDownload.add(resourceName);
                } else {
                    logger.fine("deleting remote resource " + resourceName);
                    params.add(DELETE_FILE_PARAM);
                    params.add(resourceName);
                }
            }
            if (!params.isEmpty()) {
                doPostRequest(DELETE_ACTION, (Object[]) params.toArray());
                pt.click("Deleted remote resources");
                madeChange = true;
            }
        }
        if (!diff.getOnlyInA().isEmpty() || !diff.getDiffering().isEmpty()) {
            List params = new ArrayList();
            for (String resourceName : diff.getOnlyInA()) {
                if (isSyncDownOnly(resourceName)) {
                    logger.fine("deleting local resource " + resourceName);
                    localCollection.deleteResource(resourceName);
                    madeChange = true;
                } else {
                    logger.fine("uploading new resource " + resourceName);
                    addFileUploadParamsWithBatching(params, resourceName);
                    madeChange = true;
                }
            }
            for (String resourceName : diff.getDiffering()) {
                if (isSyncDownOnly(resourceName)) {
                    filesToDownload.add(resourceName);
                } else {
                    logger.fine("uploading modified resource " + resourceName);
                    addFileUploadParamsWithBatching(params, resourceName);
                    madeChange = true;
                }
            }
            if (!params.isEmpty()) {
                doPostRequest(UPLOAD_ACTION, (Object[]) params.toArray());
                pt.click("Uploaded new/modified resources");
                madeChange = true;
            }
        }
        if (!filesToDownload.isEmpty()) {
            List params = addMultiple(null, INCLUDE_PARAM, filesToDownload);
            downloadFiles(makeGetRequest(DOWNLOAD_ACTION, params));
            madeChange = true;
        }
        if (!madeChange) {
            logger.finer("no changes to sync up");
        }
        return madeChange;
    }

    public synchronized void saveDefaultExcludedFiles() throws IOException, LockFailureException {
        if (userName == null) throw new NotLockedException();
        List params = new ArrayList();
        for (String name : ResourceFilterFactory.DEFAULT_EXCLUDE_FILENAMES) {
            addFileUploadParams(params, name);
        }
        if (!params.isEmpty()) {
            doPostRequest(UPLOAD_ACTION, (Object[]) params.toArray());
            logger.fine("Uploaded default excluded files");
        }
    }

    public synchronized URL doBackup(String qualifier) throws IOException {
        ProfTimer pt = new ProfTimer(logger, "ResourceBridgeClient.doBackup[" + remoteUrl + "]");
        try {
            doPostRequest(BACKUP_ACTION, BACKUP_QUALIFIER_PARAM, qualifier);
            pt.click("backup finished, qualifer = " + qualifier);
        } catch (LockFailureException e) {
            logger.log(Level.SEVERE, "Received unexpected exception", e);
            pt.click("backup failed");
        }
        StringBuffer result = new StringBuffer(remoteUrl);
        HTMLUtils.appendQuery(result, VERSION_PARAM, CLIENT_VERSION);
        HTMLUtils.appendQuery(result, ACTION_PARAM, GET_BACKUP_ACTION);
        return new URL(result.toString());
    }

    public synchronized void acquireLock(String userName) throws LockFailureException {
        ProfTimer pt = new ProfTimer(logger, "ResourceBridgeClient.acquireLock[" + remoteUrl + "]");
        try {
            this.userName = userName;
            doLockPostRequest(ACQUIRE_LOCK_ACTION);
            pt.click("Acquired bridged lock");
        } catch (LockFailureException lfe) {
            this.userName = null;
            throw lfe;
        } catch (Exception e) {
            this.userName = null;
            setOfflineLockStatus(OfflineLockStatus.NotLocked);
            throw new LockFailureException(e);
        }
    }

    /**
     * Reassert a lock that was enabled for offline use during a previous
     * session.
     * 
     * If the lock could be reobtained, this will return successfully - even if
     * the lock is no longer enabled for offline use. After calling this method,
     * clients should call {@link #getOfflineLockStatus()} to ensure that the
     * lock is in the mode they expect.
     */
    public synchronized void resumeOfflineLock(String userName) throws LockFailureException {
        if (!StringUtils.hasValue(extraLockData)) throw new IllegalStateException("No extra lock data has been set");
        ProfTimer pt = new ProfTimer(logger, "ResourceBridgeClient.resumeOfflineLock[" + remoteUrl + "]");
        try {
            this.userName = userName;
            doLockPostRequest(ASSERT_LOCK_ACTION);
            pt.click("Resumed offline bridged lock");
        } catch (LockFailureException lfe) {
            this.userName = null;
            throw lfe;
        } catch (Exception e) {
            setOfflineLockStatus(OfflineLockStatus.Enabled);
        }
    }

    public synchronized void pingLock() throws LockFailureException {
        if (userName == null) throw new NotLockedException();
        ProfTimer pt = new ProfTimer(logger, "ResourceBridgeClient.pingLock[" + remoteUrl + "]");
        try {
            doLockPostRequest(PING_LOCK_ACTION);
            pt.click("Pinged bridged lock");
        } catch (LockFailureException lfe) {
            throw lfe;
        } catch (Exception e) {
            throw new LockUncertainException(e);
        }
    }

    public synchronized void assertLock() throws LockFailureException {
        doAssertLock(ASSERT_LOCK_ACTION);
    }

    public synchronized void setOfflineLockEnabled(boolean offlineEnabled) throws LockFailureException {
        if (!StringUtils.hasValue(extraLockData)) throw new IllegalStateException("No extra lock data has been set");
        if (offlineEnabled) doAssertLock(ENABLE_OFFLINE_LOCK_ACTION); else doAssertLock(DISABLE_OFFLINE_LOCK_ACTION);
    }

    private synchronized void doAssertLock(String action) throws LockFailureException {
        if (userName == null) throw new NotLockedException();
        ProfTimer pt = new ProfTimer(logger, "ResourceBridgeClient." + action + "[" + remoteUrl + "]");
        try {
            doLockPostRequest(action);
            pt.click("Asserted bridged lock");
        } catch (LockFailureException lfe) {
            throw lfe;
        } catch (Exception e) {
            throw new LockUncertainException(e);
        }
    }

    public synchronized void releaseLock() {
        if (userName == null) return;
        ProfTimer pt = new ProfTimer(logger, "ResourceBridgeClient.releaseLock[" + remoteUrl + "]");
        try {
            doLockPostRequest(RELEASE_LOCK_ACTION);
            setOfflineLockStatus(OfflineLockStatus.NotLocked);
            pt.click("Released bridged lock");
        } catch (Exception e) {
            logger.log(Level.FINE, "Unable to release bridged lock", e);
        }
    }

    /**
     * Save a single file to the server.
     * 
     * @param remoteUrl the url of the team server
     * @param resourceName the name of the resource to save the data as
     * @param data the data to save to the server
     * @return the checksum of the file, as written to the server 
     * @throws IOException if an IO error occurs
     * @throws LockFailureException if the team server rejects the request
     *      because a lock is required. 
     */
    public static Long uploadSingleFile(URL remoteUrl, String resourceName, InputStream data) throws IOException, LockFailureException {
        byte[] response = doAnonymousPostRequest(remoteUrl, UPLOAD_ACTION, resourceName, data);
        ResourceCollectionInfo remoteList = XmlCollectionListing.parseListing(new ByteArrayInputStream(response));
        return remoteList.getChecksum(resourceName);
    }

    /**
     * Delete a single file from the server.
     * 
     * @param remoteUrl the url of the team server
     * @param resourceName the name of the resource to delete
     * @throws IOException if an IO error occurs
     * @throws LockFailureException if the team server rejects the request
     *      because a lock is required. 
     */
    public static boolean deleteSingleFile(URL remoteUrl, String resourceName) throws IOException, LockFailureException {
        doAnonymousPostRequest(remoteUrl, DELETE_ACTION, DELETE_FILE_PARAM, resourceName);
        return true;
    }

    /**
     * Create a new data collection on the server.
     * 
     * @param remoteUrl the url of the team server
     * @param type the type of resource collection to create
     * @return the ID of the newly created connection
     * @throws IOException if an IO error occurs
     * @throws LockFailureException if the team server rejects the request
     *      because a lock is required. 
     */
    public static String createNewCollection(URL remoteUrl, ResourceCollectionType type) throws IOException, LockFailureException {
        return createNewCollection(remoteUrl, type, null);
    }

    /**
     * Create a new data collection on the server.
     * 
     * @param remoteUrl the url of the team server
     * @param type the type of resource collection to create
     * @param desiredID the ID that should be used for the new collection
     * @return the ID of the newly created collection
     * @throws IOException if an IO error occurs
     * @throws LockFailureException if the team server rejects the request
     *      because a lock is required. 
     */
    public static String createNewCollection(URL remoteUrl, ResourceCollectionType type, String desiredID) throws IOException, LockFailureException {
        Object[] params = new Object[desiredID == null ? 2 : 4];
        params[0] = NEW_COLLECTION_TYPE_PARAM;
        params[1] = type.toString();
        if (desiredID != null) {
            params[2] = NEW_COLLECTION_ID_PARAM;
            params[3] = desiredID;
        }
        byte[] results = doAnonymousPostRequest(remoteUrl, NEW_COLLECTION_ACTION, params);
        return new String(results, "UTF-8");
    }

    private boolean isSyncDownOnly(String resourceName) {
        return (syncDownOnlyFiles != null && syncDownOnlyFiles.accept(null, resourceName));
    }

    private boolean hashesMatch() throws IOException {
        URLConnection conn = makeGetRequest(HASHCODE_ACTION);
        conn.connect();
        long localHash = ListingHashcodeCalculator.getListingHashcode(localCollection, ResourceFilterFactory.DEFAULT_FILTER);
        String hashResult = HTTPUtils.getResponseAsString(conn);
        long remoteHash = Long.valueOf(hashResult);
        return (localHash == remoteHash);
    }

    private ResourceCollectionDiff getDiff() throws IOException {
        URLConnection conn = makeGetRequest(LIST_ACTION);
        conn.connect();
        ResourceCollectionInfo localList = new ResourceListing(localCollection, ResourceFilterFactory.DEFAULT_FILTER);
        ResourceCollectionInfo remoteList = XmlCollectionListing.parseListing(new BufferedInputStream(conn.getInputStream()));
        return new ResourceCollectionDiff(localList, remoteList);
    }

    private void applySyncFilter(ResourceCollectionDiff diff, SyncFilter filter) {
        if (diff != null && filter != null) {
            applySyncFilter(diff, filter, diff.getOnlyInA());
            applySyncFilter(diff, filter, diff.getOnlyInB());
            applySyncFilter(diff, filter, diff.getDiffering());
        }
    }

    private void applySyncFilter(ResourceCollectionDiff diff, SyncFilter filter, List<String> resourceNames) {
        for (Iterator<String> i = resourceNames.iterator(); i.hasNext(); ) {
            String resourceName = i.next();
            long localTime = diff.getA().getLastModified(resourceName);
            long remoteTime = diff.getB().getLastModified(resourceName);
            if (!filter.shouldSync(resourceName, localTime, remoteTime)) i.remove();
        }
    }

    private ResourceCollectionInfo downloadFiles(URLConnection conn) throws IOException {
        ResourceCollectionInfo info = null;
        InputStream response = new BufferedInputStream(conn.getInputStream());
        ZipInputStream zipIn = new ZipInputStream(response);
        ZipEntry e;
        while ((e = zipIn.getNextEntry()) != null) {
            String name = e.getName();
            long modTime = e.getTime();
            if (ResourceContentStream.MANIFEST_FILENAME.equals(name)) {
                InputStream infoIn = new ByteArrayInputStream(FileUtils.slurpContents(zipIn, false));
                info = XmlCollectionListing.parseListing(infoIn);
                continue;
            }
            OutputStream out = localCollection.getOutputStream(name, modTime);
            if (out == null) continue;
            logger.fine("downloading resource " + name);
            FileUtils.copyFile(zipIn, out);
            out.close();
            zipIn.closeEntry();
        }
        zipIn.close();
        return info;
    }

    private void addFileUploadParamsWithBatching(List params, String resourceName) throws IOException, LockFailureException {
        if (params.size() >= 100) {
            doPostRequest(UPLOAD_ACTION, (Object[]) params.toArray());
            params.clear();
        }
        addFileUploadParams(params, resourceName);
    }

    private void addFileUploadParams(List params, String resourceName) throws IOException {
        InputStream in = localCollection.getInputStream(resourceName);
        if (in == null) return;
        params.add(resourceName);
        params.add(in);
        long modTime = localCollection.getLastModified(resourceName);
        if (modTime > 0) {
            params.add(UPLOAD_TIMESTAMP_PARAM_PREFIX + resourceName);
            params.add(Long.toString(modTime));
        }
    }

    private HttpURLConnection makeGetRequest(String action, List parameters) throws IOException {
        return makeGetRequest(action, parameters.toArray());
    }

    private HttpURLConnection makeGetRequest(String action, Object... parameters) throws IOException {
        StringBuffer request = new StringBuffer(remoteUrl);
        HTMLUtils.appendQuery(request, VERSION_PARAM, CLIENT_VERSION);
        HTMLUtils.appendQuery(request, ACTION_PARAM, action);
        for (int i = 0; i < parameters.length; i += 2) {
            HTMLUtils.appendQuery(request, String.valueOf(parameters[i]), String.valueOf(parameters[i + 1]));
        }
        String requestStr = request.toString();
        URLConnection conn;
        if (requestStr.length() < MAX_URL_LENGTH) {
            URL url = new URL(requestStr);
            conn = url.openConnection();
        } else {
            int queryPos = requestStr.indexOf('?');
            byte[] query = requestStr.substring(queryPos + 1).getBytes(HTTPUtils.DEFAULT_CHARSET);
            URL url = new URL(requestStr.substring(0, queryPos));
            conn = url.openConnection();
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Content-Length", Integer.toString(query.length));
            OutputStream outputStream = new BufferedOutputStream(conn.getOutputStream());
            outputStream.write(query);
            outputStream.close();
        }
        return (HttpURLConnection) conn;
    }

    private void doPostRequest(String action, Object... params) throws IOException, LockFailureException {
        doPostRequest(action, null, params);
    }

    private void doLockPostRequest(String action, Object... params) throws IOException, LockFailureException {
        doPostRequest(action, offlineLockStatusResponseAnalyzer, params);
    }

    private void doPostRequest(String action, HttpResponseAnalyzer responseAnalyzer, Object... params) throws IOException, LockFailureException {
        try {
            doPostRequest(new URL(remoteUrl), userName, userId, sourceIdentifier, extraLockData, responseAnalyzer, action, params);
        } catch (LockFailureException lfe) {
            if (lfe.isFatal()) setOfflineLockStatus(OfflineLockStatus.NotLocked);
            throw lfe;
        }
    }

    private static byte[] doAnonymousPostRequest(URL remoteUrl, String action, Object... params) throws IOException, LockFailureException {
        return doPostRequest(remoteUrl, null, null, null, null, null, action, params);
    }

    private interface HttpResponseAnalyzer {

        public void analyze(URLConnection conn);
    }

    private static byte[] doPostRequest(URL remoteUrl, String userName, String userId, String sourceIdentifier, String extraLockData, HttpResponseAnalyzer responseAnalyzer, String action, Object... params) throws IOException, LockFailureException {
        if (userId == null) userId = getUserId();
        ClientHttpRequest request = new ClientHttpRequest(remoteUrl);
        request.setParameter(VERSION_PARAM, CLIENT_VERSION);
        request.setParameter(ACTION_PARAM, action);
        maybeSetParameter(request, EXTRA_INFO_PARAM, userName);
        maybeSetParameter(request, USER_ID_PARAM, userId);
        maybeSetParameter(request, SOURCE_IDENTIFIER, sourceIdentifier);
        maybeSetParameter(request, EXTRA_LOCK_DATA, extraLockData);
        try {
            InputStream in = request.post(params);
            if (responseAnalyzer != null) responseAnalyzer.analyze(request.getConnection());
            return FileUtils.slurpContents(in, true);
        } catch (IOException ioe) {
            checkForLockException(request.getConnection());
            throw ioe;
        }
    }

    private static void maybeSetParameter(ClientHttpRequest request, String paramName, String paramValue) throws IOException {
        if (paramValue != null) request.setParameter(paramName, paramValue);
    }

    /**
     * Look at the headers from an HTTP response, and see if they specify an
     * offline lock status.  Based on the presence or absence of that header,
     * return the offline lock status that is in effect.
     * 
     * This method treats the absence of the offline lock status header as the
     * status "unsupported". Therefore, this should only be used on POST actions
     * that are expected to return the header (e.g., lock actions).
     */
    private static OfflineLockStatus getOfflineLockStatus(URLConnection conn) {
        OfflineLockStatus status = OfflineLockStatus.Unsupported;
        try {
            String statusHeader = conn.getHeaderField(OFFLINE_LOCK_HEADER);
            if (StringUtils.hasValue(statusHeader)) status = OfflineLockStatus.valueOf(statusHeader);
        } catch (Exception e) {
            status = OfflineLockStatus.Unsupported;
        }
        return status;
    }

    private HttpResponseAnalyzer offlineLockStatusResponseAnalyzer = new HttpResponseAnalyzer() {

        public void analyze(URLConnection conn) {
            setOfflineLockStatus(getOfflineLockStatus(conn));
        }
    };

    /**
     * Look at the headers from an HTTP response, and see if they specify a lock
     * failure exception. If so, create an appropriate exception and throw it.
     * 
     * If the response headers do <b>not</b> specify a lock failure, this
     * method will do nothing and return.
     * 
     * @param conn
     *                a connection to a URL
     * @throws LockFailureException
     *                 if the connection was an HTTP connection and if the
     *                 response headers indicate a lock failure
     * @throws IOException
     *                 if an error occurred when attempting to examine the
     *                 response headers
     */
    private static void checkForLockException(URLConnection conn) throws IOException, LockFailureException {
        if (conn instanceof HttpURLConnection) {
            HttpURLConnection http = (HttpURLConnection) conn;
            int code = http.getResponseCode();
            if (code != HttpURLConnection.HTTP_CONFLICT) return;
            String exceptionClass = http.getHeaderField(LOCK_EXCEPTION_HEADER);
            if (!StringUtils.hasValue(exceptionClass)) return;
            if (exceptionClass.equals(AlreadyLockedException.class.getName())) {
                String extraInfo = http.getHeaderField(ALREADY_LOCKED_HEADER);
                throw new AlreadyLockedException(extraInfo);
            }
            LockFailureException lfe;
            try {
                Class clazz = Class.forName(exceptionClass);
                lfe = (LockFailureException) clazz.newInstance();
            } catch (Throwable t) {
                lfe = new LockFailureException(exceptionClass + ", " + http.getResponseMessage());
            }
            throw lfe;
        }
    }

    private static List addMultiple(List l, String paramName, List values) {
        if (l == null) l = new ArrayList();
        for (Object value : values) {
            l.add(paramName);
            l.add(value);
        }
        return l;
    }

    private static String getUserId() {
        return System.getProperty("user.name");
    }

    private static final int MAX_URL_LENGTH = 512;
}
