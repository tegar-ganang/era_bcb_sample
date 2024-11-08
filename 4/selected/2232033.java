package allensoft.javacvs.client;

import java.io.*;
import java.util.*;
import java.util.prefs.*;
import java.util.zip.*;
import java.text.*;
import allensoft.util.*;
import allensoft.io.*;
import allensoft.javacvs.client.event.*;

/** This is the main class that implements the CVS client/server protocol.
 It is used to send requests to the server and process the server's responses.
 Typical usage is to create a batch of requests to perform and then perform them using
 the <code>perfromRequestBatch</code> method. For example, to perform a request that
 commits changes to a file with the log message "Added some new features" you could do this:<pre><code>

	// Create a request batch to add requests to

 	CVSRequestBatch batch = new CVSRequestBatch();

	// Add a request to the batch

	batch.addRequest (new CommitRequest (file, new CommitOptions ("Added some new features")));
	
 	// Now try to run the batch of requests. In this example, there is only one request added to the
	// batch but you can add as many as you like. The requests can even be for completely different repositories.
 
 	try
 	{
 		client.performRequestBatch (batch);	// Perform the batch
 	}

	// Catch any problems and tell user

	catch (CVSException e) {...}
	catch (IOException e) {...}

</code></pre>

 As sending a single request is quite common there is also a more convenient method, <code>performRequest</code>, to do a single request
 without the requirement of creating a batch first. The above example could have also been written:

<pre><code>

	try
 	{
		client.performRequest (new CommitRequest (file, new CommitOptions ("Added some new features")));
 	}

	// Catch any problems and tell user

	catch (CVSException e) {...}
	catch (IOException e) {...}

</code></pre>
 
 The CVSClient also has some other very unique and powerful features.
 One of those is a batch mode which causes requests to automatically be added to a batch and performed at
 a later time. This is transparent to any calling code that is trying to perform requests. Batch mode is turned
 on by calling the <code>enterBatchMode</code> method. The requests that have built up can then be sent by calling
 <code>sendBatch</code>. Batch mode can also be turned off by calling <code>exitBatchMode</code> but this
 will mean that none of the requests that have been built up since <code>enterBatchMode</code> will be performed. */
public class CVSClient {

    private static Preferences prefs = Preferences.userNodeForPackage(CVSClient.class);

    private static ResourceLoader res = ResourceLoader.getLoader(CVSClient.class);

    /** Defines the version of this client. */
    public static final String VERSION = "0.2";

    /** Creates a new CVS client that uses the supllied connection manager for creating connections
	 and the supplied login manger for getting login details from the user. */
    public CVSClient(CVSConnectionManager connectionManager, LoginManager loginManager) {
        m_ConnectionManager = connectionManager;
        m_LoginManager = loginManager;
    }

    /** Creates a new CVS client that uses the default connection manager for creating connections
	 and the supplied login manger for getting login details from the user. */
    public CVSClient(LoginManager loginManager) {
        this(new DefaultConnectionManager(), loginManager);
    }

    public CVSRequest getCurrentRequest() {
        synchronized (m_Lock) {
            return m_CurrentRequest;
        }
    }

    public void addCVSClientListener(CVSClientListener listener) {
        if (listener == null) throw new NullPointerException("listener was null");
        synchronized (m_Lock) {
            if (m_Listeners == null) m_Listeners = new ArrayList(4);
            m_Listeners.add(listener);
        }
    }

    public void removeCVSClientListener(CVSClientListener listener) {
        synchronized (m_Lock) {
            if (m_Listeners != null) m_Listeners.remove(listener);
        }
    }

    public static int getDefaultRemoteZipCompressionLevel() {
        return prefs.getInt("defaultRemoteZipLevel", 9);
    }

    public static void setDefaultRemoteZipCompressionLevel(int n) {
        prefs.putInt("defaultRemoteZipLevel", n);
    }

    public boolean isPerformingRequests() {
        return m_bPerformingRequests;
    }

    /** Gets the connection manager this client uses to create connections to a server. */
    public CVSConnectionManager getConnectionManager() {
        return m_ConnectionManager;
    }

    /** Sets the connection manager this client uses to create connections to a server. */
    public void setConnectionManager(CVSConnectionManager manager) {
        m_ConnectionManager = manager;
    }

    /** Gets the login manager this client uses to get login details from the user when necessary. */
    public LoginManager getLoginManager() {
        return m_LoginManager;
    }

    /** Sets the login manager this client uses to get login details from the user when necessary. */
    public void setLoginManager(LoginManager manager) {
        m_LoginManager = manager;
    }

    public String getValidResponses() {
        return m_sValidResponses;
    }

    public void setValidResponses(String sResponses) {
        m_sValidResponses = sResponses;
    }

    public boolean getUseUnchanged() {
        return m_bUseUnchanged;
    }

    public void setUseUnchanged(boolean b) {
        m_bUseUnchanged = b;
    }

    /** Gets the file filter used to determine if a file should be ignored. The filter should accept
	    files that should not be ignored. This is used only by the client to send questionable commands to the server.
	    It will not actually prevent files from being added to a repository, say, if this filter thinks they
	    should be ignored. If a filter is not set then a CVSIgnoreFileFilter will be returned. */
    public FileFilter getIgnoreFileFilter() {
        return m_IgnoreFileFilter;
    }

    /** Sets the file filter used to determine if a file should be ignored. The filter should accept
	    files that should not be ignored. This is used only by the client to send questionable commands to the server.
	    It will not actually prevent files from being added to a repository, say, if this filter thinks they
	    should be ignored. */
    public void setIgnoreFileFilter(FileFilter filter) {
        m_IgnoreFileFilter = filter;
    }

    public GlobalOptions getGlobalOptions() {
        return m_GlobalOptions;
    }

    public void setGlobalOptions(GlobalOptions options) {
        m_GlobalOptions = options;
    }

    public boolean isRequestValid(String sRequest) {
        return (m_sValidRequests == null) ? true : m_sValidRequests.indexOf(sRequest) != -1;
    }

    /** Performs a request. This involves sending the request to the server and
	 receiving the response from the server. This is the main method for performing a request
	 and by passing specific request types to this method different requests can be performed.
	 For example, to send a request to update a file:<code><pre>
	 client.performRequest (new UpdateRequest (file));
	 </pre></code>
	 There are more convenient methods for performing common requests that build the request object and supply
	 it to this method. If bSendImmediately is true then the request will be sent immediately regardless of whether this client is
	    in batch mode or not.
	 @param request the request to perform.
	 @param bSendImmediately true if request should be sent immediately regardless of whether this client is in batch mode.
	        If false the request will be added to the current batch if in batch mode or performed immedaitely otherwise.
	 @return a <code>CVSResponse</code> detailing the response from the server or null if no response was received.
	 */
    public CVSResponse performRequest(CVSRequest request, boolean bSendImmediately) throws IOException, CVSException {
        performRequestBatch(new CVSRequestBatch(request), bSendImmediately);
        return request.getResponse();
    }

    public CVSResponse performRequest(CVSRequest request) throws IOException, CVSException {
        return performRequest(request, false);
    }

    /** Performs a sequential list of requests using only one connection to a server. If the requests have different
	    repository locations then they will be performed in the order they are defined with one connection to each server
	    required. If bSendImmediately is true then the batch will be sent immediately regardless of whether this client is
	    in batch mode or not. After performing the batch you can examine the batch and see which requests completed successfully
	    and which had problems. */
    public void performRequestBatch(CVSRequestBatch batch, boolean bSendImmediately) throws IOException, CVSException {
        List requests = new LinkedList();
        synchronized (batch) {
            for (int i = 0; i < batch.getRequestCount(); i++) {
                CVSRequest request = batch.getRequest(i);
                request.m_Client = this;
                if (request.validateRequest()) requests.add(request); else request.m_Client = null;
            }
        }
        if (!bSendImmediately) {
            CVSRequestBatch requestBatch = null;
            synchronized (m_BatchLock) {
                if (m_RequestBatch != null && batch != m_RequestBatch) {
                    requestBatch = m_RequestBatch;
                    Iterator i = requests.iterator();
                    while (i.hasNext()) m_RequestBatch.addRequest((CVSRequest) i.next());
                }
            }
            if (requestBatch != null) {
                Iterator i = requests.iterator();
                while (i.hasNext()) ((CVSRequest) i.next()).waitForSuccessfulCompletion();
                return;
            }
        }
        synchronized (this) {
            Map requestsMap = new HashMap(10);
            Iterator i = requests.iterator();
            while (i.hasNext()) {
                CVSRequest request = (CVSRequest) i.next();
                if (!request.hasCompletedSuccessfully()) {
                    RepositoryLocation location = request.getRepositoryLocation();
                    List requestsList = (List) requestsMap.get(location);
                    if (requestsList == null) {
                        requestsList = new LinkedList();
                        requestsMap.put(location, requestsList);
                    }
                    requestsList.add(request);
                }
            }
            WorkingDirectory.synchWithFileSystem();
            m_AbortException = null;
            Set entries = requestsMap.entrySet();
            i = entries.iterator();
            fireStartingRequests();
            try {
                while (i.hasNext()) {
                    Map.Entry entry = (Map.Entry) i.next();
                    List requestsList = (List) entry.getValue();
                    RepositoryLocation location = (RepositoryLocation) entry.getKey();
                    try {
                        openConnection(location);
                    } catch (IOException e) {
                        notifyRequestsOfUnsuccessfulConnection(requestsList);
                        throw e;
                    } catch (CVSException e) {
                        notifyRequestsOfUnsuccessfulConnection(requestsList);
                        throw e;
                    }
                    try {
                        Iterator j = requestsList.iterator();
                        while (j.hasNext()) {
                            CVSRequest request = (CVSRequest) j.next();
                            m_CurrentRequest = request;
                            if (m_AbortException != null) throw m_AbortException;
                            request.clientPerformRequest();
                            WorkingDirectory.writeEntriesFiles();
                        }
                    } finally {
                        closeConnection();
                    }
                }
            } finally {
                fireFinishedRequests();
            }
        }
    }

    private void notifyRequestsOfUnsuccessfulConnection(List requests) {
        Iterator i = requests.iterator();
        while (i.hasNext()) {
            CVSRequest request = (CVSRequest) i.next();
            request.couldNotConnect();
        }
    }

    public void performRequestBatch(CVSRequestBatch batch) throws IOException, CVSException {
        performRequestBatch(batch, false);
    }

    /** Puts this client in batch mode. When in batch mode the client won't actually perform any requests
	    until the <code>sendBatch</code> method is called. This is transparent to any threads that have asked this client to
	 perform requests and they will simply wait until the batch is sent before continuing. */
    public void enterBatchMode() {
        synchronized (m_BatchLock) {
            if (m_RequestBatch == null) {
                m_RequestBatch = new CVSRequestBatch();
                fireEnteredBatchMode();
            }
        }
    }

    /** Exits batch mode. If this is called before <code>sendBatch</code> has been called the the current
	 batch of requests will not be performed. */
    public void exitBatchMode() {
        synchronized (m_BatchLock) {
            if (m_RequestBatch != null) {
                m_RequestBatch = null;
                fireExitedBatchMode();
            }
        }
    }

    /** Checks if this client is in batch mode. */
    public boolean isInBatchMode() {
        synchronized (m_BatchLock) {
            return m_RequestBatch != null;
        }
    }

    /** Gets the request batch that will be sent when <code>sendBatch</code> is called.
	 @return null if this client is not in batch mode or the batch that will be executed. */
    public CVSRequestBatch getBatch() {
        synchronized (m_BatchLock) {
            return m_RequestBatch;
        }
    }

    /** Sends the current batch of requests that have built up since <code>enterBatchMode</code> was called and
	    exits batch mode. If the client is not in batch mode then this will
	    do nothing. */
    public void sendBatch() throws CVSException, IOException {
        synchronized (m_BatchLock) {
            if (m_RequestBatch != null) {
                performRequestBatch(m_RequestBatch, true);
                m_RequestBatch = null;
                fireExitedBatchMode();
            }
        }
    }

    /** Aborts the request(s) currently being performed. */
    public void abortRequest(RequestAbortedException e) {
        m_AbortException = e;
    }

    public void abortRequest() {
        abortRequest(new RequestAbortedException());
    }

    CVSException getAbortException() {
        return m_AbortException;
    }

    private String getResourceString(String key) {
        return ResourceLoader.getString(this, CVSClient.class, key);
    }

    File getCurrentDirectory() {
        return m_CurrentRequest.getCurrentDirectory();
    }

    RepositoryLocation getRepositoryLocation() {
        return m_RepositoryLocation;
    }

    /** Gets the relative path from this request's working directory to the supplied file. */
    String getRelativePath(File file) {
        return CVSUtilities.getRelativePath(getCurrentDirectory(), file);
    }

    /** Gets a relative path to the supplied file entry from this request's working directory. */
    String getRelativePath(FileEntry file) {
        return getRelativePath(file.getFile());
    }

    /** Gets a relative path to the supplied working directory from this request's working directory. */
    String getRelativePath(WorkingDirectory workingDirectory) {
        return getRelativePath(workingDirectory.getDirectory());
    }

    /** Gets a file from a relative path received from the server. The file is relative to the current directory
	 specified in the request object that this is the response to. */
    File getFileFromRelativePath(String sRelativePath) {
        return CVSUtilities.getFileFromRelativePath(getCurrentDirectory(), sRelativePath);
    }

    /** Sends the supplied text to the server. The text should be in ASCII text
	    as this is all that is supported in the CVS protocol. This is generally used
	    to send a command to the server. */
    void sendText(String sText) throws IOException {
        byte[] bytes = null;
        try {
            bytes = sText.getBytes("ASCII");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("ASCII is not supported");
        }
        m_Out.write(bytes);
        fireSentText(sText);
        Thread.yield();
    }

    /** Sends the supplied text followed by a new line to the server. */
    void sendLine(String sText) throws IOException, CVSException {
        if (m_AbortException != null) throw m_AbortException;
        StringBuffer buffer = new StringBuffer(sText);
        buffer.append('\n');
        sendText(buffer.toString());
    }

    /** Sends a "Directory" command to the server. */
    void sendDirectory(File directory, String sRepositoryPath) throws IOException, CVSException {
        if (!directory.equals(getCurrentDirectory())) {
            DirectoryEntry entry = null;
            try {
                entry = DirectoryEntry.getDirectoryEntry(directory);
            } catch (IOException e) {
            } catch (CVSException e) {
            }
            if (entry != null && !m_DirectoriesSent.contains(entry.getWorkingDirectory().getDirectory())) sendDirectory(entry.getWorkingDirectory());
        }
        if (!directory.equals(m_LastDirectory)) {
            sendLine("Directory " + getRelativePath(directory));
            sendLine(sRepositoryPath);
            m_LastDirectory = directory;
            m_DirectoriesSent.add(directory);
        }
    }

    /** Sends a "Directory" command to the server. */
    void sendDirectory(WorkingDirectory workingDirectory) throws IOException, CVSException {
        sendDirectory(workingDirectory.getDirectory(), workingDirectory.getAbsoluteRepositoryPath());
        if (!m_WorkingDirectoriesStateHasBeenSentFor.contains(workingDirectory)) {
            if (m_CurrentRequest.getSendQuestionableCommands()) {
                File[] files = workingDirectory.getDirectory().listFiles(m_IgnoreFileFilter);
                Arrays.sort(files);
                for (int i = 0; i < files.length; i++) {
                    File file = files[i];
                    if (workingDirectory.getEntry(file) == null) sendQuestionable(file.getName());
                }
            }
            if (workingDirectory.isStaticDirectory()) sendStaticDirectory();
            String sStickyTag = workingDirectory.getStickyTagSpec();
            if (sStickyTag != null) sendSticky(sStickyTag);
            String sCheckinProg = workingDirectory.getCheckinProgram();
            if (sCheckinProg != null) sendCheckinProg(sCheckinProg);
            String sUpdateProg = workingDirectory.getUpdateProgram();
            if (sUpdateProg != null) sendUpdateProg(sUpdateProg);
            m_WorkingDirectoriesStateHasBeenSentFor.add(workingDirectory);
        }
    }

    void sendCurrentDirectory() throws CVSException, IOException {
        sendDirectory(WorkingDirectory.getWorkingDirectory(getCurrentDirectory()));
    }

    void sendStaticDirectory() throws IOException, CVSException {
        sendLine("Static-directory");
    }

    void sendSticky(String sTagSpec) throws IOException, CVSException {
        sendLine("Sticky " + sTagSpec);
    }

    void sendCheckinProg(String sProg) throws IOException, CVSException {
        sendLine("Checkin-prog " + sProg);
    }

    void sendUpdateProg(String sProg) throws IOException, CVSException {
        sendLine("Update-prog " + sProg);
    }

    /** Sends an "Entry" command to the server. */
    void sendEntry(FileEntry entry) throws IOException, CVSException {
        StringBuffer buffer = new StringBuffer(50);
        buffer.append("Entry /");
        buffer.append(entry.getName());
        buffer.append('/');
        if (entry.isLocallyRemoved()) {
            buffer.append('-');
            buffer.append(entry.getRevision());
        } else if (entry.isLocallyAdded()) buffer.append('0'); else buffer.append(entry.getRevision());
        buffer.append('/');
        if (entry.getHasConflicts()) {
            buffer.append('+');
            if (!entry.isModified()) buffer.append('=');
        } else if (entry.isLocallyRemoved()) {
            if (entry.getFile().exists()) buffer.append('=');
        } else if (entry.isLocallyAdded()) ;
        buffer.append('/');
        buffer.append(entry.getKeywordSubstitutionMode().toString());
        buffer.append('/');
        sendLine(buffer.toString());
    }

    /** Sends a "Modified" command to the server. This tells the server the contents
	    of a file we have on the client. */
    void sendModified(File file, boolean isBinary) throws IOException, CVSException {
        if (!file.exists()) return;
        CVSClient client = this;
        sendLine("Modified " + file.getName());
        if (UnixFileSupport.isEnabled()) {
            UnixFilePermissions permissions = UnixFileSupport.getPermissions(file);
            StringBuffer buffer = new StringBuffer(20);
            buffer.append("u=");
            if (permissions.isOwnerReadable()) buffer.append('r');
            if (permissions.isOwnerWritable()) buffer.append('w');
            if (permissions.isOwnerExecutable()) buffer.append('x');
            buffer.append(",g=");
            if (permissions.isGroupReadable()) buffer.append('r');
            if (permissions.isGroupWritable()) buffer.append('w');
            if (permissions.isGroupExecutable()) buffer.append('x');
            buffer.append(",o=");
            if (permissions.isOtherReadable()) buffer.append('r');
            if (permissions.isOtherWritable()) buffer.append('w');
            if (permissions.isOtherExecutable()) buffer.append('x');
            sendLine(buffer.toString());
        } else sendLine("u=rw,g=r,o=r");
        if (isBinary) sendBinaryFile(file); else {
            TextFileFormatter formatter = TextFileFormatter.getFormatterForSendingText(file);
            if (formatter == null) sendTextFile(file); else {
                File tempFile = File.createTempFile("javacvs", "tmp");
                try {
                    formatter.formatTextFile(file, tempFile);
                    sendTextFile(tempFile);
                } finally {
                    tempFile.delete();
                }
            }
        }
    }

    /** Sends the contents of a binary file to the server. The contents are
	 not modified in any way. */
    private void sendBinaryFile(File file) throws IOException, CVSException {
        BufferedInputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(file));
            if (m_bCompressFiles) {
                GZIPOutputStream gzipOut = null;
                InputStream gzipIn = null;
                File gzipFile = null;
                try {
                    gzipFile = File.createTempFile("javacvs", "tmp");
                    gzipOut = new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(gzipFile)));
                    int b;
                    while ((b = in.read()) != -1) gzipOut.write((byte) b);
                    gzipOut.close();
                    long gzipLength = gzipFile.length();
                    sendLine("z" + Long.toString(gzipLength));
                    gzipIn = new BufferedInputStream(new FileInputStream(gzipFile));
                    for (long i = 0; i < gzipLength; i++) {
                        b = gzipIn.read();
                        if (b == -1) throw new EOFException();
                        m_Out.write((byte) b);
                    }
                } finally {
                    if (gzipOut != null) gzipOut.close();
                    if (gzipIn != null) gzipIn.close();
                    if (gzipFile != null) gzipFile.delete();
                }
            } else {
                long nLength = file.length();
                sendLine(Long.toString(nLength));
                for (long i = 0; i < nLength; i++) {
                    int b = in.read();
                    if (b == -1) throw new EOFException();
                    m_Out.write((byte) b);
                }
            }
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /** Sends the contents of a text file to the server. The difference between
	 this method and sendBinaryFile is that this will convert any
	 \r\n or just \r to \n (the unix line terminator). */
    private void sendTextFile(File file) throws IOException, CVSException {
        BufferedReader in = null;
        boolean warnedAboutBinary = false;
        RepositoryDetails.EndOfLineType eolType = null;
        RepositoryDetails repositoryDetails = RepositoryDetails.get(m_RepositoryLocation);
        if (repositoryDetails != null && repositoryDetails.shouldConvertEndOfLinesForFile(file)) eolType = repositoryDetails.getRepositoryEndOfLineType();
        byte[] endOfLine = null;
        if (eolType != null) endOfLine = eolType.getEndOfLine().getBytes("ASCII");
        boolean sentConversionUpdate = false;
        try {
            in = new BufferedReader(new FileReader(file));
            OutputStream tempOut = null;
            InputStream tempIn = null;
            File tempFile = null;
            try {
                tempFile = File.createTempFile("javacvs", "tmp");
                tempOut = new BufferedOutputStream(new FileOutputStream(tempFile));
                if (m_bCompressFiles) tempOut = new GZIPOutputStream(tempOut);
                int n;
                while ((n = in.read()) != -1) {
                    if (eolType != null) {
                        if (n == '\r') {
                            tempOut.write(endOfLine);
                            if (!sentConversionUpdate) {
                                fireStatusUpdate(MessageFormat.format(res.getString("convertingRepositoryEndOfLine"), new Object[] { file.getAbsolutePath(), eolType.toString() }));
                                sentConversionUpdate = true;
                            }
                            n = in.read();
                            if (n == -1) break;
                            if (n == '\n') continue;
                        } else if (n == '\n') {
                            tempOut.write(endOfLine);
                            if (!sentConversionUpdate) {
                                fireStatusUpdate(MessageFormat.format(res.getString("convertingRepositoryEndOfLine"), new Object[] { file.getAbsolutePath(), eolType.toString() }));
                                sentConversionUpdate = true;
                            }
                            continue;
                        }
                    }
                    if (!warnedAboutBinary && CVSUtilities.isProbablyBinary(n)) {
                        fireStatusUpdate(MessageFormat.format(res.getString("textFileContainsBinary"), new Object[] { file.getAbsolutePath() }));
                        warnedAboutBinary = true;
                    }
                    tempOut.write(n);
                }
                tempOut.close();
                long tempLength = tempFile.length();
                tempIn = new BufferedInputStream(new FileInputStream(tempFile));
                sendLine((m_bCompressFiles ? "z" : "") + Long.toString(tempLength));
                for (long i = 0; i < tempLength; i++) {
                    n = tempIn.read();
                    if (n == -1) throw new EOFException();
                    m_Out.write((byte) n);
                }
            } finally {
                if (tempOut != null) tempOut.close();
                if (tempIn != null) tempIn.close();
                if (tempFile != null) tempFile.delete();
            }
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private boolean isBinaryCharacter(int n) {
        return n == 0;
    }

    /** Sends an "Is-modified" command to the server. */
    void sendIsModified(String sName) throws IOException, CVSException {
        sendLine("Is-modified " + sName);
    }

    void sendArgument(String sArgument) throws IOException, CVSException {
        int nIndex = 0;
        boolean bFinished = false;
        while (!bFinished) {
            int nEndOfLine = sArgument.indexOf('\n', nIndex);
            String sLine = null;
            if (nEndOfLine == -1) {
                sLine = sArgument.substring(nIndex);
                bFinished = true;
            } else sLine = sArgument.substring(nIndex, nEndOfLine);
            sendLine((nIndex == 0 ? "Argument " : "Argumentx ") + sLine);
            if (!bFinished) nIndex = nEndOfLine + 1;
        }
    }

    void sendUnchanged(String sName) throws IOException, CVSException {
        sendLine("Unchanged " + sName);
    }

    void sendQuestionable(String sName) throws IOException, CVSException {
        sendLine("Questionable " + sName);
    }

    void sendArgument(File file) throws IOException, CVSException {
        sendArgument(getRelativePath(file));
    }

    void sendArgument(FileEntry file) throws IOException, CVSException {
        sendArgument(getRelativePath(file));
    }

    void sendArgument(WorkingDirectory workingDirectory) throws IOException, CVSException {
        sendArgument(getRelativePath(workingDirectory));
    }

    void sendKopt(KeywordSubstitutionMode mode) throws IOException, CVSException {
        if (mode != KeywordSubstitutionMode.NOT_DEFINED) sendLine("Kopt " + mode.toString());
    }

    void sendWatchActions(WatchActions actions) throws IOException, CVSException {
        if (actions.watchEdit()) {
            sendArgument("-a");
            sendArgument("edit");
        }
        if (actions.watchUnedit()) {
            sendArgument("-a");
            sendArgument("unedit");
        }
        if (actions.watchCommit()) {
            sendArgument("-a");
            sendArgument("commit");
        }
    }

    void sendDateOption(Date date) throws IOException, CVSException {
        sendArgument("-D " + g_DateFormatter.format(date));
    }

    void sendGlobalOption(String sOption) throws IOException, CVSException {
        sendLine("Global_option " + sOption);
    }

    private void sendNotify(File file, boolean edit) throws IOException, CVSException {
        sendLine("Notify " + file.getName());
        StringBuffer buffer = new StringBuffer(50);
        if (edit) buffer.append('E'); else buffer.append('U');
        buffer.append('\t');
        buffer.append(DateFormat.getDateTimeInstance().format(new Date()));
        buffer.append('\t');
        buffer.append(java.net.InetAddress.getLocalHost().getHostName());
        buffer.append('\t');
        buffer.append(file.getParentFile().getAbsolutePath());
        buffer.append("\tEUC");
        sendLine(buffer.toString());
    }

    void sendEditNotify(File file) throws IOException, CVSException {
        sendNotify(file, true);
    }

    void sendUneditNotify(File file) throws IOException, CVSException {
        sendNotify(file, false);
    }

    /** Sends the details of a file to the server. This is an accumulation of a Directory,
	 Entry and Modified (or Unchanged) command. */
    void sendFileDetails(FileEntry entry) throws IOException, CVSException {
        if (!m_FileDetailsSent.contains(entry.getFile())) {
            sendDirectory(entry.getWorkingDirectory());
            sendEntry(entry);
            File file = entry.getFile();
            if (file.exists()) {
                if (!m_bUseUnchanged || entry.isModified() || entry.isLocallyAdded()) {
                    if (m_CurrentRequest.canSendIsModified()) sendIsModified(file.getName()); else sendModified(file, entry.isBinary());
                } else sendUnchanged(entry.getName());
            }
            m_FileDetailsSent.add(file);
        }
    }

    void sendFileDetails(File file, KeywordSubstitutionMode mode) throws IOException, CVSException {
        if (file.isDirectory()) throw new IllegalArgumentException("Use sendDirectoryDetails to send directories");
        if (!m_FileDetailsSent.contains(file)) {
            FileEntry entry = null;
            try {
                entry = FileEntry.getFileEntry(file);
            } catch (IOException e) {
            } catch (CVSException e) {
            }
            if (entry != null) sendFileDetails(entry); else {
                sendDirectory(WorkingDirectory.getWorkingDirectory(file.getParentFile()));
                sendKopt(mode);
                sendModified(file, mode == KeywordSubstitutionMode.KB);
                m_FileDetailsSent.add(file);
            }
        }
    }

    void sendFileDetails(File file) throws IOException, CVSException {
        boolean isBinary = false;
        if (!CVSUtilities.isUnderCVSControl(file)) isBinary = CVSUtilities.isProbablyBinary(file);
        sendFileDetails(file, isBinary ? KeywordSubstitutionMode.KB : KeywordSubstitutionMode.NOT_DEFINED);
    }

    /** Sends the details of an entire working directory to the server. */
    void sendDirectoryDetails(WorkingDirectory workingDir, boolean bRecursive, FileEntryFilter fileEntryFilter) throws IOException, CVSException {
        sendDirectory(workingDir);
        Entry[] entries = workingDir.getEntries();
        for (int i = 0; i < entries.length; i++) {
            Entry entry = entries[i];
            if (entry instanceof FileEntry) {
                FileEntry fileEntry = (FileEntry) entry;
                if (fileEntryFilter.accept(fileEntry)) sendFileDetails(fileEntry);
            } else if (bRecursive) sendDirectoryDetails(WorkingDirectory.getWorkingDirectory(entry.getFile()), true, fileEntryFilter);
        }
    }

    void sendDirectoryDetails(WorkingDirectory workingDir, boolean bRecursive) throws IOException, CVSException {
        sendDirectoryDetails(workingDir, bRecursive, FileEntryFilter.ALL_FILES);
    }

    void sendDirectoryDetails(WorkingDirectory workingDir) throws IOException, CVSException {
        sendDirectoryDetails(workingDir, true, FileEntryFilter.ALL_FILES);
    }

    void sendDirectoryDetails(File directory, boolean bRecursive, FileEntryFilter fileEntryFilter) throws IOException, CVSException {
        WorkingDirectory w = null;
        try {
            w = WorkingDirectory.getWorkingDirectory(directory);
        } catch (IOException e) {
            return;
        } catch (CVSException e) {
            return;
        }
        sendDirectoryDetails(w, bRecursive, fileEntryFilter);
    }

    void sendDirectoryDetails(File directory, boolean bRecursive) throws IOException, CVSException {
        sendDirectoryDetails(directory, bRecursive, FileEntryFilter.ALL_FILES);
    }

    void sendDirectoryDetails(File directory) throws IOException, CVSException {
        sendDirectoryDetails(directory, true, FileEntryFilter.ALL_FILES);
    }

    /** Sends some options to the server. */
    void sendOptions(Options options) throws IOException, CVSException {
        if (options == null) return;
        options.m_Client = this;
        options.sendOptions();
    }

    /** Performs the supplied request without trying to open a connection to the server. It
	 assumes the client has already opened a connection to the server. */
    CVSResponse performSubRequest(CVSRequest request) throws IOException, CVSException {
        CVSRequest oldRequest = m_CurrentRequest;
        m_CurrentRequest = request;
        try {
            request.m_Client = this;
            if (!request.getRepositoryLocation().equals(m_RepositoryLocation)) throw new CVSException("Cannot " + request.getDescription() + " because the request is for a different repository location");
            if (!request.validateRequest()) return null;
            request.clientPerformRequest();
            return request.getResponse();
        } finally {
            request.m_Client = null;
            m_CurrentRequest = oldRequest;
        }
    }

    ExpandModulesResponse expandModules(String[] modules) throws IOException, CVSException {
        return (ExpandModulesResponse) performSubRequest(new ExpandModulesRequest(getRepositoryLocation(), getCurrentDirectory(), modules));
    }

    ValidRequestsResponse validRequests() throws IOException, CVSException {
        return (ValidRequestsResponse) performSubRequest(new ValidRequestsRequest(getRepositoryLocation()));
    }

    GetWrapperRCSOptionsResponse getWrapperRCSOptions() throws IOException, CVSException {
        return (GetWrapperRCSOptionsResponse) performSubRequest(new GetWrapperRCSOptionsRequest(getRepositoryLocation()));
    }

    /** Flushes the output stream to the server. */
    void flush() throws IOException {
        m_Out.flush();
    }

    /** Receive a line of text from the server. */
    String receiveLine() throws IOException {
        flush();
        m_ReceiveBuffer.setLength(0);
        int n;
        while (true) {
            n = m_In.read();
            if (n == -1) throw new EOFException("Connection closed");
            if (n == '\n') break;
            m_ReceiveBuffer.append((char) n);
        }
        String s = m_ReceiveBuffer.toString();
        fireReceivedText(s);
        Thread.yield();
        return s;
    }

    /** Receives a file transmission and outputs it to <code>out</code>.
	    @param file the file to write received data to or <code>null</code> to discard the file. */
    private void receiveBinaryFile(OutputStream out) throws IOException, CVSException {
        flush();
        long nLength = 0;
        String sLength = receiveLine();
        if (sLength.charAt(0) == 'z') {
            try {
                nLength = Long.parseLong(sLength.substring(1));
            } catch (Exception e) {
                throw new CVSProtocolException("Invalid response from server: \"" + sLength + "\"\n" + "Could not parse the length of the file to be received.", e);
            }
            File gzipFile = null;
            OutputStream gzipOut = null;
            InputStream gzipIn = null;
            try {
                gzipFile = File.createTempFile("javacvs", "tmp");
                gzipOut = new BufferedOutputStream(new FileOutputStream(gzipFile));
                for (int i = 0; i < nLength; i++) {
                    int n = m_In.read();
                    if (n == -1) throw new EOFException();
                    gzipOut.write((byte) n);
                    Thread.yield();
                }
                gzipOut.close();
                gzipIn = new GZIPInputStream(new BufferedInputStream(new FileInputStream(gzipFile)));
                while (true) {
                    int n = gzipIn.read();
                    if (n == -1) break;
                    if (out != null) out.write((byte) n);
                }
            } finally {
                if (gzipIn != null) gzipIn.close();
                if (gzipOut != null) gzipOut.close();
                if (gzipFile != null) gzipFile.delete();
            }
        } else {
            try {
                nLength = Long.parseLong(sLength);
            } catch (NumberFormatException e) {
                throw new CVSProtocolException("Invalid response from server: \"" + sLength + "\"\n" + "Could not parse the length of the file to be received.", e);
            }
            for (int i = 0; i < nLength; i++) {
                int n = m_In.read();
                if (n == -1) throw new EOFException();
                if (out != null) out.write((byte) n);
            }
        }
    }

    void receiveBinaryFile(File fileToSaveAs) throws IOException, CVSException {
        File backup = null;
        if (fileToSaveAs.exists()) {
            backup = File.createTempFile("javacvs", "tmp");
            if (!fileToSaveAs.renameTo(backup)) throw new IOException("Could not backup existing file " + fileToSaveAs.getAbsolutePath());
        }
        OutputStream out = null;
        try {
            out = new BufferedOutputStream(new FileOutputStream(fileToSaveAs));
            receiveBinaryFile(out);
        } catch (Exception e) {
            if (fileToSaveAs.exists()) fileToSaveAs.delete();
            if (backup != null && !backup.renameTo(fileToSaveAs)) throw new IOException("Could not restore file " + fileToSaveAs.getAbsolutePath() + " from backup file " + backup.getAbsolutePath());
            if (e instanceof CVSException) throw (CVSException) e; else if (e instanceof IOException) throw (IOException) e; else throw (RuntimeException) e;
        } finally {
            if (out != null) out.close();
        }
    }

    /** Receives the contents of a text file and writes them to out. This is
	    a helper function for the receiveTextFile(File) method and should not
	 be called anywhere else - hence private. null can be provided to skip over
	 the file contents. */
    private void receiveTextFile(File file, Writer out) throws IOException, CVSException {
        flush();
        RepositoryDetails.EndOfLineType eolType = null;
        RepositoryDetails repositoryDetails = RepositoryDetails.get(m_RepositoryLocation);
        if (repositoryDetails != null && repositoryDetails.shouldConvertEndOfLinesForFile(file)) eolType = repositoryDetails.getLocalEndOfLineType();
        String sEndOfLine = null;
        if (eolType != null) sEndOfLine = eolType.getEndOfLine();
        long nLength = 0;
        String sLength = receiveLine();
        boolean sentStatusUpdateAboutConvert = false;
        boolean sentBinaryWarning = false;
        if (sLength.charAt(0) == 'z') {
            try {
                nLength = Long.parseLong(sLength.substring(1));
            } catch (Exception e) {
                throw new CVSProtocolException("Invalid response from server: \"" + sLength + "\"\n" + "Could not parse the length of the file to be received.", e);
            }
            File gzipFile = null;
            OutputStream gzipOut = null;
            InputStream gzipIn = null;
            try {
                gzipFile = File.createTempFile("javacvs", "tmp");
                gzipOut = new BufferedOutputStream(new FileOutputStream(gzipFile));
                for (int i = 0; i < nLength; i++) {
                    int n = m_In.read();
                    if (n == -1) throw new EOFException();
                    gzipOut.write((byte) n);
                    Thread.yield();
                }
                gzipOut.close();
                gzipIn = new GZIPInputStream(new BufferedInputStream(new FileInputStream(gzipFile)));
                while (true) {
                    int n = gzipIn.read();
                    if (n == -1) break;
                    if (eolType != null) {
                        if (n == '\r') {
                            if (out != null) {
                                out.write(sEndOfLine);
                                if (!sentStatusUpdateAboutConvert) {
                                    fireStatusUpdate(MessageFormat.format(res.getString("convertingLocalEndOfLine"), new Object[] { file.getAbsolutePath(), eolType.toString() }));
                                    sentStatusUpdateAboutConvert = true;
                                }
                            }
                            n = gzipIn.read();
                            if (n == -1) break;
                            if (n == '\n') continue;
                        } else if (n == '\n') {
                            if (out != null) {
                                out.write(sEndOfLine);
                                if (!sentStatusUpdateAboutConvert) {
                                    fireStatusUpdate(MessageFormat.format(res.getString("convertingLocalEndOfLine"), new Object[] { file.getAbsolutePath(), eolType.toString() }));
                                    sentStatusUpdateAboutConvert = true;
                                }
                            }
                            continue;
                        }
                    }
                    if (!sentBinaryWarning && CVSUtilities.isProbablyBinary(n)) {
                        fireStatusUpdate(MessageFormat.format(res.getString("receivedTextFileIsProbablyBinary"), new Object[] { file.getAbsolutePath() }));
                        sentBinaryWarning = true;
                    }
                    if (out != null) out.write(n);
                }
            } finally {
                if (gzipIn != null) gzipIn.close();
                if (gzipOut != null) gzipOut.close();
                if (gzipFile != null) gzipFile.delete();
            }
        } else {
            try {
                nLength = Long.parseLong(sLength);
            } catch (NumberFormatException e) {
                throw new CVSProtocolException("Invalid response from server: \"" + sLength + "\"\n" + "Could not parse the length of the file to be received.", e);
            }
            for (int i = 0; i < nLength; i++) {
                int n = m_In.read();
                if (n == -1) break;
                if (eolType != null) {
                    if (n == '\r') {
                        if (out != null) {
                            out.write(sEndOfLine);
                            if (!sentStatusUpdateAboutConvert) {
                                fireStatusUpdate(MessageFormat.format(res.getString("convertingLocalEndOfLine"), new Object[] { file.getAbsolutePath(), eolType.toString() }));
                                sentStatusUpdateAboutConvert = true;
                            }
                        }
                        n = m_In.read();
                        if (n == -1) break;
                        if (n == '\n') continue;
                    } else if (n == '\n') {
                        if (out != null) {
                            out.write(sEndOfLine);
                            if (!sentStatusUpdateAboutConvert) {
                                fireStatusUpdate(MessageFormat.format(res.getString("convertingLocalEndOfLine"), new Object[] { file.getAbsolutePath(), eolType.toString() }));
                                sentStatusUpdateAboutConvert = true;
                            }
                        }
                        continue;
                    }
                }
                if (!sentBinaryWarning && CVSUtilities.isProbablyBinary(n)) {
                    fireStatusUpdate(MessageFormat.format(res.getString("receivedTextFileIsProbablyBinary"), new Object[] { file.getAbsolutePath() }));
                    sentBinaryWarning = true;
                }
                if (out != null) out.write(n);
            }
        }
    }

    /** Receives a text file from the server. The lines will be terminated with the platform's
	    line separator. If the user has configured a text file formatters for this file then the formatter will be
	    run to process the file (for example, for indentation style). If there are any problems
	    then an exception will be thrown. If the file existed then it is unlikely it was changed as this
	    method will receive the file contents to a temporary file, the origional file is moved out of the
	    way and this file is moved in to replace it. Should something go wrong during this process
	 the origional file is moved back. */
    void receiveTextFile(File fileToSaveAs) throws IOException, CVSException {
        File backup = null;
        if (fileToSaveAs.exists()) {
            backup = File.createTempFile("javacvs", "tmp");
            if (!fileToSaveAs.renameTo(backup)) throw new IOException("Could not backup existing file " + fileToSaveAs.getAbsolutePath());
        }
        BufferedWriter out = null;
        try {
            TextFileFormatter formatter = TextFileFormatter.getFormatterForReceivingText(fileToSaveAs);
            if (formatter == null) {
                out = new BufferedWriter(new FileWriter(fileToSaveAs));
                receiveTextFile(fileToSaveAs, out);
            } else {
                File tempFile = File.createTempFile("javacvs", "tmp");
                Writer tempWriter = null;
                try {
                    tempWriter = new BufferedWriter(new FileWriter(tempFile));
                    receiveTextFile(fileToSaveAs, tempWriter);
                    tempWriter.close();
                    formatter.formatTextFile(tempFile, fileToSaveAs);
                } finally {
                    if (tempWriter != null) tempWriter.close();
                    tempFile.delete();
                }
            }
        } catch (Exception e) {
            if (fileToSaveAs.exists()) fileToSaveAs.delete();
            if (backup != null && !backup.renameTo(fileToSaveAs)) throw new IOException("Could not restore file " + fileToSaveAs.getAbsolutePath() + " from backup file " + backup.getAbsolutePath());
            if (e instanceof CVSException) throw (CVSException) e; else if (e instanceof IOException) throw (IOException) e; else throw (RuntimeException) e;
        } finally {
            if (out != null) out.close();
        }
    }

    /** Establish a connection with the server for the supplied repository location.
	    If a connection already exists and it can be used for this repository then
	    this method will not do anything. Otherwise it will ask the connection manager
	    to make a new connection to the repository. */
    private void openConnection(RepositoryLocation location) throws CVSException, IOException {
        if (location.getConnectionMethod().equals(CVSConnectionMethod.LOCAL)) fireStatusUpdate(getResourceString("connectingToLocalServer")); else fireStatusUpdate(MessageFormat.format(getResourceString("connectingToServer"), new Object[] { location.getHostName() }));
        closeConnection();
        m_Connection = m_ConnectionManager.createConnection(this, location, m_LoginManager);
        fireOpenedConnection();
        m_RepositoryLocation = location;
        m_In = new BufferedInputStream(m_Connection.getInputStream());
        m_Out = new BufferedOutputStream(m_Connection.getOutputStream());
        final InputStream errorStream = m_Connection.getErrorStream();
        if (errorStream != null) {
            Thread thread = new Thread() {

                public void run() {
                    BufferedReader in = new BufferedReader(new InputStreamReader(errorStream));
                    String sError;
                    try {
                        while ((sError = in.readLine()) != null) {
                            fireStatusUpdate(sError);
                        }
                    } catch (IOException e) {
                    } finally {
                        try {
                            in.close();
                        } catch (IOException e) {
                        }
                    }
                }
            };
            thread.start();
        }
        clearOptimisationState();
        sendLine("Root " + location.getRepositoryPath());
        ValidRequestsResponse validRequestsResponse = validRequests();
        m_sValidRequests = null;
        if (validRequestsResponse != null && !validRequestsResponse.hadError()) m_sValidRequests = validRequestsResponse.getValidRequests();
        sendLine("Valid-responses " + m_sValidResponses);
        if (m_bUseUnchanged) sendLine("UseUnchanged");
        sendOptions(m_GlobalOptions);
        int nZipLevel = m_GlobalOptions.getZipCompressionLevel();
        if (nZipLevel == -1) {
            RepositoryDetails details = RepositoryDetails.get(location);
            if (details != null) nZipLevel = details.getZipCompressionLevel();
            if (nZipLevel == -1) {
                if (location.getConnectionMethod() == CVSConnectionMethod.LOCAL) nZipLevel = 0; else nZipLevel = getDefaultRemoteZipCompressionLevel();
            }
        }
        m_bCompressFiles = false;
        if (nZipLevel > 0) {
            if (isRequestValid("gzip-file-contents")) {
                sendLine("gzip-file-contents " + nZipLevel);
                m_bCompressFiles = true;
            } else nZipLevel = 0;
        }
        m_nZipLevel = nZipLevel;
    }

    private void closeConnection() {
        if (m_Connection != null) {
            m_Connection.close();
            m_Connection = null;
        }
        m_RepositoryLocation = null;
    }

    protected void fireSentText(String sText) {
        List listeners = null;
        synchronized (m_Lock) {
            if (m_Listeners == null || m_Listeners.size() == 0) return;
            listeners = (List) m_Listeners.clone();
        }
        CVSClientTextEvent event = new CVSClientTextEvent(this, sText);
        Iterator i = listeners.iterator();
        while (i.hasNext()) {
            CVSClientListener listener = (CVSClientListener) i.next();
            listener.sentText(event);
        }
    }

    protected void fireReceivedText(String sText) {
        List listeners = null;
        synchronized (m_Lock) {
            if (m_Listeners == null || m_Listeners.size() == 0) return;
            listeners = (List) m_Listeners.clone();
        }
        CVSClientTextEvent event = new CVSClientTextEvent(this, sText);
        Iterator i = listeners.iterator();
        while (i.hasNext()) {
            CVSClientListener listener = (CVSClientListener) i.next();
            listener.receivedText(event);
        }
    }

    protected synchronized void fireStatusUpdate(String sText) {
        List listeners = null;
        synchronized (m_Lock) {
            if (m_Listeners == null || m_Listeners.size() == 0) return;
            listeners = (List) m_Listeners.clone();
        }
        CVSClientTextEvent event = new CVSClientTextEvent(this, sText);
        Iterator i = listeners.iterator();
        while (i.hasNext()) {
            CVSClientListener listener = (CVSClientListener) i.next();
            listener.statusUpdate(event);
        }
    }

    protected void fireReceivedResponse(CVSResponse response) {
        List listeners = null;
        synchronized (m_Lock) {
            if (m_Listeners == null || m_Listeners.size() == 0) return;
            listeners = (List) m_Listeners.clone();
        }
        CVSClientResponseEvent event = new CVSClientResponseEvent(this, response);
        Iterator i = listeners.iterator();
        while (i.hasNext()) {
            CVSClientListener listener = (CVSClientListener) i.next();
            listener.receivedResponse(event);
        }
    }

    protected void fireStartingRequests() {
        m_bPerformingRequests = true;
        List listeners = null;
        synchronized (m_Lock) {
            if (m_Listeners == null || m_Listeners.size() == 0) return;
            listeners = (List) m_Listeners.clone();
        }
        CVSClientEvent event = new CVSClientEvent(this);
        Iterator i = listeners.iterator();
        while (i.hasNext()) {
            CVSClientListener listener = (CVSClientListener) i.next();
            listener.startingRequests(event);
        }
    }

    protected void fireOpenedConnection() {
        m_bPerformingRequests = true;
        List listeners = null;
        synchronized (m_Lock) {
            if (m_Listeners == null || m_Listeners.size() == 0) return;
            listeners = (List) m_Listeners.clone();
        }
        CVSClientEvent event = new CVSClientEvent(this);
        Iterator i = listeners.iterator();
        while (i.hasNext()) {
            CVSClientListener listener = (CVSClientListener) i.next();
            listener.openedConnection(event);
        }
    }

    protected void fireFinishedRequests() {
        m_bPerformingRequests = false;
        List listeners = null;
        synchronized (m_Lock) {
            if (m_Listeners == null || m_Listeners.size() == 0) return;
            listeners = (List) m_Listeners.clone();
        }
        CVSClientEvent event = new CVSClientEvent(this);
        Iterator i = listeners.iterator();
        while (i.hasNext()) {
            CVSClientListener listener = (CVSClientListener) i.next();
            listener.finishedRequests(event);
        }
    }

    protected void fireEnteredBatchMode() {
        m_bPerformingRequests = false;
        List listeners = null;
        synchronized (m_Lock) {
            if (m_Listeners == null || m_Listeners.size() == 0) return;
            listeners = (List) m_Listeners.clone();
        }
        CVSClientEvent event = new CVSClientEvent(this);
        Iterator i = listeners.iterator();
        while (i.hasNext()) {
            CVSClientListener listener = (CVSClientListener) i.next();
            listener.enteredBatchMode(event);
        }
    }

    protected void fireExitedBatchMode() {
        m_bPerformingRequests = false;
        List listeners = null;
        synchronized (m_Lock) {
            if (m_Listeners == null || m_Listeners.size() == 0) return;
            listeners = (List) m_Listeners.clone();
        }
        CVSClientEvent event = new CVSClientEvent(this);
        Iterator i = listeners.iterator();
        while (i.hasNext()) {
            CVSClientListener listener = (CVSClientListener) i.next();
            listener.exitedBatchMode(event);
        }
    }

    private void clearOptimisationState() {
        m_LastDirectory = null;
        m_DirectoriesSent.clear();
        m_WorkingDirectoriesStateHasBeenSentFor.clear();
        m_FileDetailsSent.clear();
    }

    private CVSConnectionManager m_ConnectionManager;

    private RepositoryLocation m_RepositoryLocation;

    private LoginManager m_LoginManager;

    private CVSConnection m_Connection;

    private InputStream m_In;

    private OutputStream m_Out;

    private int m_nZipLevel;

    private boolean m_bCompressFiles;

    private StringBuffer m_ReceiveBuffer = new StringBuffer(100);

    private ArrayList m_Listeners;

    private String m_sValidResponses = "E M ok error Valid-requests Created Merged Updated Update-existing Removed Remove-entry New-entry Checked-in Copy-file Notified Clear-sticky Set-sticky Clear-static-directory Set-static-directory Wrapper-rcsOption Mod-time";

    private boolean m_bUseUnchanged = true;

    private boolean m_bPerformingRequests = false;

    private CVSRequest m_CurrentRequest;

    private Object m_Lock = new Object();

    private RequestAbortedException m_AbortException;

    private boolean m_bAborted;

    private String m_sValidRequests;

    private File m_LastDirectory;

    private static final DateFormat g_DateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private Set m_DirectoriesSent = new HashSet(50);

    private Set m_WorkingDirectoriesStateHasBeenSentFor = new HashSet(50);

    private Set m_FileDetailsSent = new HashSet(200);

    private FileFilter m_IgnoreFileFilter = CVSIgnoreFileFilter.getInstance();

    private GlobalOptions m_GlobalOptions = new GlobalOptions();

    private CVSRequestBatch m_RequestBatch;

    private Object m_BatchLock = new Object();
}
