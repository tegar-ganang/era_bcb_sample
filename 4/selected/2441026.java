package com.ivis.xprocess.framework.vcs.impl.svn;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.tmatesoft.svn.core.ISVNLogEntryHandler;
import org.tmatesoft.svn.core.SVNAuthenticationException;
import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.internal.wc.DefaultSVNOptions;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.ISVNPropertyHandler;
import org.tmatesoft.svn.core.wc.ISVNStatusHandler;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNLogClient;
import org.tmatesoft.svn.core.wc.SVNPropertyData;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNStatus;
import org.tmatesoft.svn.core.wc.SVNStatusClient;
import org.tmatesoft.svn.core.wc.SVNStatusType;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;
import org.tmatesoft.svn.core.wc.SVNWCClient;
import org.tmatesoft.svn.core.wc.SVNWCUtil;
import com.ivis.xprocess.core.Xproject;
import com.ivis.xprocess.framework.DataSource;
import com.ivis.xprocess.framework.XchangeElement;
import com.ivis.xprocess.framework.XchangeElementContainer;
import com.ivis.xprocess.framework.Xelement;
import com.ivis.xprocess.framework.exceptions.XmlLoadException;
import com.ivis.xprocess.framework.impl.DatasourceDescriptor;
import com.ivis.xprocess.framework.impl.XchangeElementImpl;
import com.ivis.xprocess.framework.impl.XelementImpl;
import com.ivis.xprocess.framework.vcs.Proxy;
import com.ivis.xprocess.framework.vcs.VCSPathChange;
import com.ivis.xprocess.framework.vcs.VCSPathChangeType;
import com.ivis.xprocess.framework.vcs.VCSTransactionListener;
import com.ivis.xprocess.framework.vcs.VcsInfoListener;
import com.ivis.xprocess.framework.vcs.VcsProvider;
import com.ivis.xprocess.framework.vcs.VcsXPXInfo;
import com.ivis.xprocess.framework.vcs.VCSTransaction.TransactionType;
import com.ivis.xprocess.framework.vcs.auth.VCSAuth;
import com.ivis.xprocess.framework.vcs.auth.VCSPasswordAuth;
import com.ivis.xprocess.framework.vcs.auth.VCSSSHAuth;
import com.ivis.xprocess.framework.vcs.exceptions.VCSException;
import com.ivis.xprocess.framework.vcs.exceptions.VCSObjectAlreadyExistsException;
import com.ivis.xprocess.framework.vcs.exceptions.VCSPathAlreadyExistsInRepositoryException;
import com.ivis.xprocess.framework.vcs.exceptions.VCSResourceNotUnderVersionControlException;
import com.ivis.xprocess.framework.xml.DirectoryFilter;
import com.ivis.xprocess.framework.xml.IFileIndex;
import com.ivis.xprocess.framework.xml.LimboIndex;
import com.ivis.xprocess.framework.xml.XMLFilenameFilter;
import com.ivis.xprocess.framework.xml.XMLifier;
import com.ivis.xprocess.util.FileUtils;
import com.ivis.xprocess.util.UuidUtils;

/**
 * The VCSProvider implementation for SVN.
 *
 */
public class SubversionProvider implements VcsProvider {

    public static String PROP_DATE = "svn:date";

    public static String PROP_AUTHOR = "svn:author";

    public static String PROP_LOG = "svn:log";

    public static final int UNKNOW_REVISION = -1;

    protected static final Logger logger = Logger.getLogger(SubversionProvider.class.getName());

    private static SVNClientManager clientManager = null;

    private static SVNStatusClient statusClient = null;

    private ISVNAuthenticationManager authManager;

    private DefaultSVNOptions options;

    private VCSState state;

    protected DataSource source = null;

    private Set<VcsInfoListener> infoListeners = new HashSet<VcsInfoListener>();

    protected Set<VCSTransactionListener> preTransactionListeners = new HashSet<VCSTransactionListener>();

    protected Set<VCSTransactionListener> postTransactionListeners = new HashSet<VCSTransactionListener>();

    private SVNRepository svnRepository;

    public SubversionProvider() {
        DAVRepositoryFactory.setup();
        state = VcsProvider.VCSState.NOT_AUTHENTICATED;
    }

    public boolean isLoggedIn() {
        if (state == VcsProvider.VCSState.NOT_AUTHENTICATED) {
            return false;
        }
        return true;
    }

    protected File getRootFile() throws VCSException {
        File root = new File(source.getLocalRootDirectory());
        if (root == null) {
            VCSException e = new VCSException("Can not locate root directory for commit.");
            logger.throwing(this.getClass().getName(), "getRootFile", e);
            throw e;
        }
        return root;
    }

    public void getFileInfo(File path) throws VCSException {
        if (!isLoggedIn()) {
            VCSException e = new VCSException("You are not logged on to the VCS provider. No VCS functionality is available.");
            throw (e);
        }
        try {
            SVNStatus status = getSVNStatusClient().doStatus(path, false);
            SVNNodeKind kind = status.getKind();
            if (kind == SVNNodeKind.DIR) {
                System.out.println("DIR");
            } else if (kind == SVNNodeKind.FILE) {
                System.out.println("FILE");
            } else if (kind == SVNNodeKind.NONE) {
                System.out.println("NONE");
            } else if (kind == SVNNodeKind.UNKNOWN) {
                System.out.println("UNKNOWN");
            }
        } catch (SVNException e) {
            VCSException vcse = new VCSException(e);
            logger.throwing(this.getClass().getName(), "getFileInfo", vcse);
            throw vcse;
        }
    }

    public void login(VCSAuth auth, Proxy proxy) {
        if (auth instanceof VCSPasswordAuth) {
            VCSPasswordAuth passwordAuth = (VCSPasswordAuth) auth;
            authManager = new BasicAuthenticationManager(passwordAuth.getUserName(), passwordAuth.getPassword());
        } else if (auth instanceof VCSSSHAuth) {
            VCSSSHAuth sshAuth = (VCSSSHAuth) auth;
            authManager = new BasicAuthenticationManager(sshAuth.getUserName(), sshAuth.getKeyFile(), sshAuth.getPassPhrase(), sshAuth.getPort());
        }
        if (proxy != null) {
            ((BasicAuthenticationManager) authManager).setProxy(proxy.getHost(), proxy.getPort(), proxy.getUserName(), proxy.getPassWord());
        }
        internalLogin();
    }

    public void initialize(VCSAuth auth, Proxy proxy) {
        if (auth instanceof VCSPasswordAuth) {
            VCSPasswordAuth passwordAuth = (VCSPasswordAuth) auth;
            authManager = new BasicAuthenticationManager(passwordAuth.getUserName(), passwordAuth.getPassword());
        } else if (auth instanceof VCSSSHAuth) {
            VCSSSHAuth sshAuth = (VCSSSHAuth) auth;
            authManager = new BasicAuthenticationManager(sshAuth.getUserName(), sshAuth.getKeyFile(), sshAuth.getPassPhrase(), sshAuth.getPort());
        }
        if (proxy != null) {
            ((BasicAuthenticationManager) authManager).setProxy(proxy.getHost(), proxy.getPort(), proxy.getUserName(), proxy.getPassWord());
        }
    }

    private void internalLogin() {
        options = SVNWCUtil.createDefaultOptions(true);
        options.setMergerFactory(new XprocessMergerFactory(source));
        clientManager = SVNClientManager.newInstance(options, authManager);
        SVNEventHandler.getInstance().setPersistenceHelper(source.getPersistenceHelper());
        SVNEventHandler.getInstance().addObserver((SVNEventObserver) source.getPersistenceHelper().getFileIndex());
        clientManager.getUpdateClient().setEventHandler(SVNEventHandler.getInstance());
        clientManager.getCommitClient().setEventHandler(SVNEventHandler.getInstance());
        state = VcsProvider.VCSState.AUTHENTICATED;
        SVNRepositoryFactoryImpl.setup();
        try {
            svnRepository = clientManager.createRepository(SVNURL.parseURIEncoded(source.getDatasourceURL()), true);
            svnRepository.setAuthenticationManager(authManager);
        } catch (SVNException e) {
            e.printStackTrace();
        }
    }

    /**
     * For use with the web server Obtains a ClientManger for a specific
     * password auth
     *
     * @param auth
     * @return
     */
    private SVNClientManager getClientManager(VCSPasswordAuth auth) {
        ISVNAuthenticationManager myAuth = new BasicAuthenticationManager(auth.getUserName(), auth.getPassword());
        SVNClientManager myClientManager = SVNClientManager.newInstance(options, myAuth);
        myClientManager.getUpdateClient().setEventHandler(SVNEventHandler.getInstance());
        myClientManager.getCommitClient().setEventHandler(SVNEventHandler.getInstance());
        return myClientManager;
    }

    /**
     * For use with the web server performs a commit using the supplied identity
     *
     * @param paths
     * @param auth
     * @throws VCSException
     */
    public long commit(File[] paths, VCSPasswordAuth auth) throws VCSException {
        return commit(paths, getClientManager(auth));
    }

    private long commit(File[] paths, SVNClientManager clientManager) throws VCSException {
        long newRevision = -1;
        if (!isLoggedIn()) {
            VCSException e = new VCSException("You are not logged on to the VCS provider. No VCS functionality is available.");
            throw (e);
        }
        sendInfo("Committing " + paths);
        SVNTransaction trans = getTransaction();
        try {
            SVNCommitInfo info = getClientManager().getCommitClient().doCommit(paths, true, getCommitComment(), false, true);
            newRevision = info.getNewRevision();
        } catch (SVNException e) {
            throw SVNExceptionConverter.convert(e);
        } finally {
            processChanges(trans);
        }
        return newRevision;
    }

    private String getCommitComment() {
        String commitComment = "";
        try {
            commitComment = java.net.InetAddress.getLocalHost().toString();
        } catch (UnknownHostException e) {
            logger.warning(e.getMessage());
        }
        return commitComment;
    }

    /**
     * If the client manager does not exist it creates one.
     *
     * @return the SVN client manager
     */
    public SVNClientManager getClientManager() {
        if (clientManager == null) {
            options = SVNWCUtil.createDefaultOptions(true);
            options.setMergerFactory(new XprocessMergerFactory(source));
            clientManager = SVNClientManager.newInstance(options, authManager);
            clientManager.getUpdateClient().setEventHandler(SVNEventHandler.getInstance());
            clientManager.getCommitClient().setEventHandler(SVNEventHandler.getInstance());
        }
        return clientManager;
    }

    public DataSource getDataSource() {
        return source;
    }

    public void setDataSource(DataSource source) {
        this.source = source;
    }

    public long commit(XchangeElement... elements) throws VCSException {
        Collection<File> filesToCommit = new ArrayList<File>();
        String path;
        String containerPath;
        File ContainerFile;
        File xpx;
        IFileIndex idx = source.getPersistenceHelper().getFileIndex();
        for (int i = 0; i < elements.length; i++) {
            path = idx.getFullPath(elements[i]);
            if (path == null) {
                ((XchangeElementImpl) elements[i]).save();
                path = idx.getFullPath(elements[i]);
                if (path == null) {
                    throw new VCSException("Failed to obtain a path for XchangeElement " + elements[i].getId());
                }
            }
            xpx = new File(path);
            if (!xpx.exists()) {
                throw new VCSException("File for path " + path + " does not exist");
            }
            try {
                if (isAddRequired(xpx)) {
                    add(xpx);
                    if (elements[i] instanceof XchangeElementContainer) {
                        containerPath = source.getDescriptor().getOpenDir().getAbsolutePath() + File.separator + elements[i].getId();
                        ContainerFile = new File(containerPath);
                        if (!ContainerFile.exists()) {
                            throw new VCSException("Directory for XchangeElementContainer does not exist " + elements[i].getId());
                        }
                        if (isAddRequired(ContainerFile)) {
                            add(ContainerFile);
                            filesToCommit.add(ContainerFile);
                        }
                    }
                }
            } catch (SVNException e) {
                throw SVNExceptionConverter.convert(e);
            }
            filesToCommit.add(xpx);
        }
        File[] files = new File[filesToCommit.size()];
        return commit(filesToCommit.toArray(files));
    }

    public long commit(File[] paths) throws VCSException {
        return commit(paths, getClientManager());
    }

    public long commit() throws VCSException {
        commitLimbo();
        return commit(new File[] { getRootFile() });
    }

    private void commitLimbo() throws VCSException {
        LimboIndex limboIndex = new LimboIndex(source.getPersistenceHelper());
        for (String limboFile : limboIndex.getLimboIndexes()) {
            String fullToLocation = source.getLocalRootDirectory() + limboFile;
            String fullFromLocation = source.getPersistenceHelper().getDataLayout().getLimboDir() + limboFile;
            boolean successfullAdd = false;
            while (!successfullAdd) {
                String limboDirs = limboFile.substring(0, limboFile.lastIndexOf(File.separator));
                File parentPath = new File(fullToLocation.substring(0, fullToLocation.lastIndexOf(File.separator)));
                String topDirThatDoesExistInParentPath = findNewParentDir(parentPath.getAbsolutePath());
                try {
                    File fromPath = new File(fullFromLocation);
                    File toPath = new File(fullToLocation);
                    if (!parentPath.exists()) {
                        parentPath.mkdirs();
                    }
                    FileUtils.copyFile(fromPath, toPath);
                    try {
                        StringTokenizer stringTokenizer = new StringTokenizer(limboDirs, File.separator);
                        String path = source.getLocalRootDirectory();
                        while (stringTokenizer.hasMoreTokens()) {
                            path += (File.separator + stringTokenizer.nextToken());
                            if (isAddRequired(new File(path))) {
                                add(new File(path));
                            }
                        }
                        if (isAddRequired(toPath)) {
                            add(toPath);
                        }
                    } catch (SVNException svnException) {
                        logger.throwing(this.getClass().getName(), "commiting limbo", svnException);
                        throw SVNExceptionConverter.convert(svnException);
                    }
                    if (topDirThatDoesExistInParentPath == null) {
                        commit(new File[] { toPath });
                    } else {
                        commit(new File[] { new File(topDirThatDoesExistInParentPath) });
                    }
                    commit(new File[] { getRootFile() });
                    successfullAdd = true;
                } catch (VCSPathAlreadyExistsInRepositoryException vcsPathAlreadyExistsInRepositoryException) {
                    File artifactDir = new File(source.getPersistenceHelper().getDataLayout().getArtifactsDir());
                    File topDir = new File(topDirThatDoesExistInParentPath);
                    delete(topDir);
                    String artifactsDir = source.getPersistenceHelper().getDataLayout().getArtifactsDir();
                    boolean successfullUpdate = false;
                    while (!successfullUpdate) {
                        if (topDir.getAbsolutePath().equals(artifactsDir)) {
                            update(getRootFile());
                        } else {
                            update(artifactDir);
                        }
                        successfullUpdate = true;
                    }
                } catch (IOException ioException) {
                    logger.severe(ioException.getMessage());
                }
            }
        }
        File limboDir = new File(source.getPersistenceHelper().getDataLayout().getLimboDir());
        if (limboDir.exists()) {
            for (File file : limboDir.listFiles()) {
                if (file.isDirectory()) {
                    FileUtils.deleteDir(file);
                } else {
                    file.delete();
                }
            }
        }
    }

    private String findNewParentDir(String parentPath) {
        String path = "";
        StringTokenizer stringTokenizer = new StringTokenizer(parentPath, File.separator);
        while (stringTokenizer.hasMoreTokens()) {
            if (path.length() == 0) {
                if (System.getProperty("os.name").toLowerCase().startsWith("mac") || System.getProperty("os.name").toLowerCase().startsWith("linux")) {
                    path = File.separator + stringTokenizer.nextToken();
                } else {
                    path = stringTokenizer.nextToken();
                }
            } else {
                path += (File.separator + stringTokenizer.nextToken());
            }
            File file = new File(path);
            if (!file.exists()) {
                return path;
            }
        }
        return null;
    }

    private long checkout(String url, File dir, SVNRevision revision) throws VCSException {
        if (!isLoggedIn()) {
            VCSException e = new VCSException("You are not logged on to the VCS provider. No VCS functionality is available.");
            throw (e);
        }
        sendInfo("Checking out " + revision.getNumber() + " to " + source.getLocalRootDirectory());
        File descriptor = new File(source.getLocalRootDirectory() + File.separator + DatasourceDescriptor.DATASOURCE_DESCRIPTOR_NAME);
        if (descriptor.exists()) {
            descriptor.delete();
        }
        SVNTransaction trans = getTransaction();
        try {
            SVNURL svnurl = SVNURL.parseURIEncoded(url);
            SVNUpdateClient updateClient = getClientManager().getUpdateClient();
            updateClient.setIgnoreExternals(false);
            long version = updateClient.doCheckout(svnurl, dir, revision, revision, true);
            return version;
        } catch (SVNException e) {
            logger.throwing(this.getClass().getName(), "checkout", e);
            throw SVNExceptionConverter.convert(e);
        } finally {
            processChanges(trans);
        }
    }

    public long checkoutHead(String url) throws VCSException {
        return checkout(url, getRootFile(), SVNRevision.HEAD);
    }

    public long checkout(long revision) throws VCSException {
        return checkout(source.getDatasourceURL(), getRootFile(), SVNRevision.create(revision));
    }

    public long checkoutHead(String url, File dir) throws VCSException {
        return checkout(url, dir, SVNRevision.HEAD);
    }

    public long checkoutHead() throws VCSException {
        return checkoutHead(source.getDatasourceURL(), getRootFile());
    }

    public void createRepository() {
    }

    public void add(File path) throws VCSException {
        add(path, getClientManager());
    }

    /**
     * For use with the web server Adds a file with the supplied identity
     *
     * @param path
     * @param auth
     * @throws VCSException
     */
    public void add(File path, VCSPasswordAuth auth) throws VCSException {
        add(path, getClientManager(auth));
    }

    private void add(File path, SVNClientManager clientManager) throws VCSException {
        try {
            if (isAddRequired(path)) {
                clientManager.getWCClient().doAdd(path, false, false, false, true);
            }
        } catch (SVNException e) {
            throw SVNExceptionConverter.convert(e);
        }
    }

    public void move(File src, File dest) throws VCSException {
        try {
            getClientManager().getMoveClient().doMove(src, dest);
        } catch (SVNException e) {
            throw SVNExceptionConverter.convert(e);
        }
    }

    public void move(XchangeElement src, XchangeElementContainer dest) throws VCSException {
        IFileIndex idx = source.getPersistenceHelper().getFileIndex();
        File srcXpx = new File(idx.getFullPath(src.getId()));
        File destDir = new File(idx.getFullPath(dest.getId())).getParentFile();
        File destFile = new File(destDir.getAbsolutePath() + File.separator + src.getUuid() + XMLifier.XML_EXTENSION);
        move(srcXpx, destFile);
        idx.remove(src.getId());
        idx.addElement(src.getId(), destFile.getAbsolutePath());
    }

    public long updateArtifacts() throws VCSException {
        return update(new File(source.getPersistenceHelper().getDataLayout().getArtifactsDir()));
    }

    public long update() throws VCSException {
        return update(getRootFile());
    }

    public long update(File path) throws VCSException {
        return update(path, SVNRevision.HEAD);
    }

    public long update(long revision) throws VCSException {
        return update(getRootFile(), SVNRevision.create(revision));
    }

    private long update(final File path, final SVNRevision revision) throws VCSException {
        if (!isLoggedIn()) {
            VCSException e = new VCSException("You are not logged on to the VCS provider. No VCS functionality is available.");
            throw (e);
        }
        sendInfo("Updating " + source.getLocalRootDirectory());
        SVNTransaction trans = getTransaction();
        long version = -1;
        boolean successfullUpdate = false;
        while (!successfullUpdate) {
            try {
                try {
                    SVNUpdateClient updateClient = getClientManager().getUpdateClient();
                    updateClient.setIgnoreExternals(false);
                    version = updateClient.doUpdate(path, revision, true);
                } catch (SVNException e) {
                    VCSException vcs = SVNExceptionConverter.convert(e);
                    logger.throwing(this.getClass().getName(), "update", vcs);
                    throw vcs;
                }
            } catch (VCSObjectAlreadyExistsException vcsObjectAlreadyExistsException) {
                logger.log(Level.SEVERE, "File/Directory already exists on local file system so tidying up by moving it to the orphan dir - " + vcsObjectAlreadyExistsException.getMessage(), new RuntimeException());
                String pathConflict = vcsObjectAlreadyExistsException.getMessage();
                String existingPath = source.getLocalRootDirectory() + File.separator + pathConflict;
                existingPath = FileUtils.fixPath(existingPath);
                moveToOrphansFolder(new File(existingPath));
            } catch (VCSPathAlreadyExistsInRepositoryException vcsObjectAlreadyExistsException) {
                logger.log(Level.SEVERE, "File/Directory already exists on local file system so tidying up by moving it to the orphan dir - " + vcsObjectAlreadyExistsException.getMessage(), new RuntimeException());
                String pathConflict = vcsObjectAlreadyExistsException.getMessage();
                String existingPath = source.getLocalRootDirectory() + File.separator + pathConflict;
                existingPath = FileUtils.fixPath(existingPath);
                moveToOrphansFolder(new File(existingPath));
            } finally {
                processChanges(trans);
            }
            successfullUpdate = true;
        }
        return version;
    }

    public void revert(File path, boolean recursive) throws VCSException {
        if (!isLoggedIn()) {
            VCSException e = new VCSException("You are not logged on to the VCS provider. No VCS functionality is available.");
            throw (e);
        }
        sendInfo("Reverting " + path.getName());
        SVNTransaction trans = getTransaction();
        SVNWCClient client = getClientManager().getWCClient();
        try {
            client.doRevert(path, recursive);
        } catch (SVNException e) {
            logger.throwing(this.getClass().getName(), "revert", e);
            throw SVNExceptionConverter.convert(e);
        } finally {
            processChanges(trans);
        }
    }

    public void delete(File path) throws VCSException {
        sendInfo("Deleting " + path.getName());
        SVNTransaction trans = getTransaction();
        SVNWCClient client = getClientManager().getWCClient();
        try {
            client.doDelete(path, true, false);
            if (path.exists()) {
                logger.log(Level.SEVERE, "Error: XPX still exists after deletion - " + path);
            }
        } catch (SVNException e) {
            logger.throwing(this.getClass().getName(), "delete", e);
            throw SVNExceptionConverter.convert(e);
        } finally {
            processChanges(trans);
        }
    }

    public void resolve(File path, boolean recursive) throws VCSException {
        if (!isLoggedIn()) {
            VCSException e = new VCSException("You are not logged on to the VCS provider. No VCS functionality is available.");
            throw (e);
        }
        sendInfo("Resolving " + path.getName());
        SVNTransaction trans = getTransaction();
        SVNWCClient client = getClientManager().getWCClient();
        try {
            client.doResolve(path, recursive);
        } catch (SVNException e) {
            logger.throwing(this.getClass().getName(), "resolve", e);
            throw SVNExceptionConverter.convert(e);
        } finally {
            processChanges(trans);
        }
    }

    public void autoAddNewFiles() throws VCSException {
        SVNTransaction trans = getTransaction();
        File root = new File(source.getLocalRootDirectory());
        File descriptor = new File(root.getAbsoluteFile() + File.separator + DatasourceDescriptor.DATASOURCE_DESCRIPTOR_NAME);
        if (descriptor.exists()) {
            try {
                if (isAddRequired(descriptor)) {
                    sendInfo("Adding descriptor " + descriptor.getName());
                    add(descriptor);
                }
            } catch (SVNException e) {
                logger.throwing(this.getClass().getName(), "autoAddNewFiles", e);
                throw SVNExceptionConverter.convert(e);
            }
        } else {
            logger.log(Level.INFO, "Datasource.xml not present.");
        }
        execAutoAddNewFiles(root);
        processChanges(trans);
    }

    private void execAutoAddNewFiles(File root) throws VCSException {
        try {
            for (File subDirectory : root.listFiles(SVNDirectoryFilter.getInstance())) {
                if (isAddRequired(subDirectory)) {
                    sendInfo("Adding directory " + subDirectory.getName());
                    add(subDirectory);
                }
                execAutoAddNewFiles(subDirectory);
            }
            for (File xmlFile : root.listFiles(SVNFileNameFilter.getInstance())) {
                if (isAddRequired(xmlFile)) {
                    sendInfo("Adding file " + xmlFile.getName());
                    add(xmlFile);
                }
            }
        } catch (SVNException e) {
            logger.throwing(this.getClass().getName(), "execAutoAddNewFiles", e);
            throw SVNExceptionConverter.convert(e);
        }
    }

    public VCSState getState() {
        return state;
    }

    private boolean isAddRequired(File file) throws SVNException {
        try {
            SVNStatus status = getSVNStatusClient().doStatus(file, false);
            if (status.getContentsStatus() == SVNStatusType.STATUS_UNVERSIONED) {
                return true;
            }
        } catch (SVNException svnException) {
            VCSException vcsException = SVNExceptionConverter.convert(svnException);
            if (vcsException instanceof VCSResourceNotUnderVersionControlException) {
                return true;
            } else {
                throw svnException;
            }
        }
        return false;
    }

    protected SVNTransaction getTransaction() {
        SVNTransaction trans = new SVNTransaction();
        SVNEventHandler.getInstance().addFirstObserver(trans);
        return trans;
    }

    protected void processChanges(SVNTransaction trans) {
        SVNEventHandler.getInstance().removeObserver(trans);
        trans.setPost(false);
        trans.addAll(checkContainerDeletes(trans));
        sendInfo("Notifying pre-listeners");
        for (VCSTransactionListener listener : preTransactionListeners) {
            listener.processTransaction(trans);
        }
        updateModel(trans);
        trans.setPost(true);
        sendInfo("Notifying post-listeners");
        for (VCSTransactionListener listener : postTransactionListeners) {
            listener.processTransaction(trans);
        }
        trans = null;
    }

    /**
     * A VCS delete may leave a directory and files behind. The operation
     * checks to make sure the file / directory has actually gone and clears
     * up afterwards.
     *
     * @param trans
     */
    private SVNTransaction checkContainerDeletes(SVNTransaction trans) {
        SVNTransaction consequentialDeletes = new SVNTransaction();
        for (VCSPathChange change : trans.getDeletes()) {
            if (change.getType() == VCSPathChangeType.UPDATE_DELETE) {
                File file = new File(source.getLocalRootDirectory() + "/" + change.getPath());
                if (file.exists()) {
                    if (file.isDirectory()) {
                        deleteXpxFilesIn(file, consequentialDeletes);
                    }
                    FileUtils.deleteDir(file);
                }
            }
        }
        return consequentialDeletes;
    }

    private void deleteXpxFilesIn(File directory, SVNTransaction trans) {
        try {
            if (!directory.exists()) {
                return;
            }
            for (File subDirectory : directory.listFiles(DirectoryFilter.getInstance())) {
                deleteXpxFilesIn(subDirectory, trans);
            }
            String idToDelete;
            XchangeElement elementToDelete;
            for (File xmlFile : directory.listFiles(XMLFilenameFilter.getInstance())) {
                idToDelete = UuidUtils.getIdfromPath(xmlFile.getPath());
                elementToDelete = (XchangeElement) source.getPersistenceHelper().uuidToElement(idToDelete);
                if (elementToDelete != null) {
                    logger.log(Level.FINE, "Element remains after container deletion " + xmlFile.getPath());
                    sendInfo("XPX found after container delete " + xmlFile.getPath() + " id " + idToDelete);
                    moveToOrphansFolder(xmlFile);
                    if (trans != null) {
                        String relativePath = "open" + source.getPersistenceHelper().getFileIndex().getRelativePath(xmlFile);
                        trans.pathChangeOccurred(PathChangeType.UPDATE_DELETE, relativePath);
                    }
                }
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error finding files to delete", e);
        }
    }

    private void moveToOrphansFolder(File file) {
        File dir = source.getDescriptor().getOrphanDir();
        File dest = new File(dir.getAbsolutePath() + File.separator + file.getName());
        try {
            if (!file.isDirectory()) {
                FileUtils.copyFile(file, dest);
            } else {
                move(file, dest);
                deleteSVNFolders(dest);
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to copy file to orphan dir", e);
        } catch (VCSException e) {
            logger.log(Level.WARNING, "Failed move dir to orphan dir", e);
        }
    }

    public void updateModel(SVNTransaction trans) {
        if ((trans.getTransactionType() != null) && (trans.getTransactionType() == TransactionType.UPDATE)) {
            for (VCSPathChange change : trans.getNonMoveDeletes()) {
                sendInfo("Deleting " + change.getId());
                source.getPersistenceHelper().vcsDelete(change.getId());
            }
            if (trans.getAll().size() > 0) {
                sendInfo("Reindexing " + source.getLocalRootDirectory());
                source.getPersistenceHelper().getFileIndex().index();
            }
            for (VCSPathChange change : trans.getEverythingExceptDeletes()) {
                if ((change.getId() != null) && !change.getPath().startsWith(DatasourceDescriptor.ARTIFACT_DIRECTORY_DEFAULT)) {
                    if (change.isForwarder()) {
                        XchangeElement xe = (XchangeElement) source.getPersistenceHelper().getElement(change.getId());
                        if (xe.isGhost()) {
                            load(change.getId());
                        }
                    } else {
                        load(change.getId());
                    }
                } else {
                    if (change.getPath().endsWith(DatasourceDescriptor.DATASOURCE_DESCRIPTOR_NAME)) {
                        sendInfo("Loading datasource descriptor");
                        source.getDescriptor().restoreAll();
                    }
                }
            }
        }
    }

    private void load(String id) {
        sendInfo("DeXMLifier loading " + id);
        try {
            Class<? extends XelementImpl> clazz = UuidUtils.getTypeFromUUID(id);
            if (XchangeElementContainer.class.isAssignableFrom(clazz)) {
                XchangeElementContainer container = (XchangeElementContainer) source.getPersistenceHelper().uuidToElement(id);
                boolean hollowContents = true;
                if ((container != null) && !container.hasHollowContents()) {
                    hollowContents = false;
                }
                source.getPersistenceHelper().getDexmlifier().load(id);
                container = (XchangeElementContainer) source.getPersistenceHelper().uuidToElement(id);
                container.setHollowContents(hollowContents);
            } else {
                XchangeElement xe = (XchangeElement) source.getPersistenceHelper().uuidToElement(id);
                if ((xe != null) && !xe.isHollow()) {
                    source.getPersistenceHelper().getDexmlifier().load(id);
                } else {
                    String containerId = source.getPersistenceHelper().getFileIndex().getContainerUUID(id);
                    XchangeElementContainer container = (XchangeElementContainer) source.getPersistenceHelper().uuidToElement(containerId);
                    if ((container != null) && !container.hasHollowContents()) {
                        source.getPersistenceHelper().getDexmlifier().load(id);
                    }
                }
            }
        } catch (XmlLoadException noop) {
            logger.log(Level.INFO, "Should never happen!", noop);
        }
    }

    protected void sendInfo(String info) {
        for (VcsInfoListener listener : infoListeners) {
            listener.processInfo(info);
        }
    }

    public void addInfoListener(VcsInfoListener listener) {
        infoListeners.add(listener);
    }

    public void removeInfoListener(VcsInfoListener listener) {
        infoListeners.remove(listener);
    }

    private SVNStatusClient getSVNStatusClient() {
        if (statusClient == null) {
            statusClient = new SVNStatusClient(authManager, null);
        }
        return statusClient;
    }

    /**
     * @param xelement
     * @param remote true if the status of the xelement should also be checked in the Repository, or false
     * @return the SVNStatus for the xelement
     * @throws SVNException
     */
    public SVNStatus getStatus(Xelement xelement, boolean remote) throws SVNException {
        XchangeElement ee = xelement.getXchangeElement();
        String path = source.getPersistenceHelper().getFileIndex().getFullPath(ee.getId());
        File xpx = new File(path);
        if (!xpx.exists()) {
            throw new SVNException(SVNErrorMessage.create(SVNErrorCode.UNKNOWN, "XPX does not exist " + path));
        }
        return getSVNStatusClient().doStatus(xpx, remote);
    }

    public VcsXPXInfo getVcsInfo(XchangeElement xelement) throws VCSException {
        return getVcsInfo(xelement, true);
    }

    public boolean xpxExists(XchangeElement xchangeElement) {
        File originalXPX = new File(source.getPersistenceHelper().getFileIndex().getFullPath(xchangeElement.getId()));
        return fileExists(originalXPX);
    }

    public boolean fileExists(File file) {
        SVNStatus status = null;
        try {
            status = getSVNStatusClient().doStatus(file, true);
        } catch (SVNException e) {
            return false;
        }
        if (status.getRemoteContentsStatus() == SVNStatusType.STATUS_DELETED) {
            return false;
        }
        return true;
    }

    public long getLocalRevision(XchangeElement element) throws VCSException {
        String path = source.getPersistenceHelper().getFileIndex().getFullPath(element.getId());
        if ((path == null) || (path.length() == 0)) {
            return 0;
        }
        SVNStatus status = null;
        try {
            status = getSVNStatusClient().doStatus(new File(path), false);
        } catch (SVNException e) {
            throw SVNExceptionConverter.convert(e);
        }
        return status.getRevision().getNumber();
    }

    public VcsXPXInfo getVcsInfo(XchangeElement xelement, boolean remote) throws VCSException {
        try {
            String xpxPath = source.getPersistenceHelper().getFileIndex().getFullPath(xelement.getId());
            File xpx = new File(xpxPath);
            return copyVCSInfo(getSVNStatusClient().doStatus(xpx, remote));
        } catch (SVNException e) {
            throw SVNExceptionConverter.convert(e);
        }
    }

    public List<VcsXPXInfo> getVcsHistory(XchangeElement xelement) throws VCSException {
        SVNLogClient logClient = getClientManager().getLogClient();
        List<VcsXPXInfo> frag_history = new ArrayList<VcsXPXInfo>();
        try {
            String xpxPath = source.getPersistenceHelper().getFileIndex().getFullPath(xelement.getId());
            File xpx = new File(xpxPath);
            final Collection<SVNLogEntry> log_entries = new ArrayList<SVNLogEntry>();
            ISVNLogEntryHandler handler = new ISVNLogEntryHandler() {

                public void handleLogEntry(SVNLogEntry logEntry) {
                    log_entries.add(logEntry);
                }
            };
            logClient.doLog(new File[] { xpx }, SVNRevision.create(0), SVNRevision.HEAD, false, false, 20, handler);
            for (SVNLogEntry entry : log_entries) {
                VcsXPXInfo info = copyVCSInfo(entry);
                frag_history.add(info);
            }
        } catch (SVNException e) {
            throw SVNExceptionConverter.convert(e);
        } catch (Exception e) {
        }
        return frag_history;
    }

    private VcsXPXInfo copyVCSInfo(SVNStatus status) {
        VcsXPXInfo vcsXPXInfo = new VcsXPXInfo();
        SVNRevision revision = status.getRevision();
        if (revision != null) {
            vcsXPXInfo.setRevision(revision.getNumber());
        } else {
            vcsXPXInfo.setRevision(UNKNOW_REVISION);
        }
        SVNRevision remote_revision = status.getRemoteRevision();
        if (remote_revision != null) {
            vcsXPXInfo.setRemoteRevision(remote_revision.getNumber());
        } else {
            vcsXPXInfo.setRemoteRevision(UNKNOW_REVISION);
        }
        vcsXPXInfo.setAuthor(status.getAuthor());
        vcsXPXInfo.setCommittedDate(status.getCommittedDate());
        return vcsXPXInfo;
    }

    private VcsXPXInfo copyVCSInfo(SVNLogEntry logEntry) {
        VcsXPXInfo vcsXPXInfo = new VcsXPXInfo();
        long revision = logEntry.getRevision();
        if (revision != -1) {
            vcsXPXInfo.setRevision(revision);
        } else {
            vcsXPXInfo.setRevision(UNKNOW_REVISION);
        }
        vcsXPXInfo.setAuthor(logEntry.getAuthor());
        vcsXPXInfo.setCommittedDate(logEntry.getDate());
        vcsXPXInfo.setMessage(logEntry.getMessage());
        return vcsXPXInfo;
    }

    public void add(Xelement xelement) throws VCSException {
        XchangeElement xe = (xelement instanceof XchangeElement) ? (XchangeElement) xelement : xelement.getContainedIn();
        String fullPath = source.getPersistenceHelper().getFileIndex().getFullPath(xe);
        String root = source.getLocalRootDirectory();
        File xpx = new File(fullPath);
        String openPath = "open" + source.getPersistenceHelper().getFileIndex().getRelativePath(xpx);
        File dir;
        for (String dirPath : FileUtils.getDirsFromFragment(openPath)) {
            dir = new File(root + File.separator + dirPath);
            if (dir.exists()) {
                add(dir);
            }
        }
        if (xelement instanceof XchangeElementContainer) {
            File containerDir = new File(root + File.separator + "open" + File.separator + xelement.getId());
            if (containerDir.exists()) {
                add(containerDir);
            }
        }
        try {
            if (isAddRequired(xpx)) {
                clientManager.getWCClient().doAdd(xpx, false, false, true, true);
            }
        } catch (SVNException e) {
            throw SVNExceptionConverter.convert(e);
        }
    }

    public boolean isAddRequired(Xelement xelement) throws VCSException {
        String xpxPath = source.getPersistenceHelper().getFileIndex().getFullPath(xelement.getId());
        if (xpxPath == null) {
            logger.log(Level.WARNING, "Unable to locate the path for " + xelement.getUuid());
            return false;
        }
        File xpx = new File(xpxPath);
        try {
            if (isAddRequired(xpx)) {
                return true;
            }
        } catch (SVNException e) {
            throw SVNExceptionConverter.convert(e);
        }
        return false;
    }

    public void addArtifact(File artifact) throws VCSException {
        String path = artifact.getPath();
        int artifactPos = path.lastIndexOf(DatasourceDescriptor.ARTIFACT_DIRECTORY_DEFAULT);
        if (artifactPos == -1) {
            logger.log(Level.WARNING, "Artifact path is not relative to the artifacts managed dir");
        }
        String frag = path.substring(artifactPos);
        String root = path.substring(0, artifactPos);
        File dir;
        try {
            for (String dirPath : FileUtils.getDirsFromFragment(frag)) {
                dir = new File(root + dirPath);
                if (dir.exists() && isAddRequired(dir)) {
                    add(dir);
                }
            }
            if (artifact.exists() && isAddRequired(artifact)) {
                add(artifact);
            }
        } catch (SVNException e) {
            throw SVNExceptionConverter.convert(e);
        }
    }

    public boolean updateNeeded(Xelement xelement) throws VCSException {
        SVNStatus status = null;
        try {
            status = getStatus(xelement, true);
        } catch (SVNException e) {
            throw new VCSException(e);
        }
        if (status.getRevision() != status.getRemoteRevision()) {
            return true;
        } else {
            return false;
        }
    }

    public void cleanup(File path) throws VCSException {
        sendInfo("Running cleanup " + path.getName());
        SVNWCClient client = getClientManager().getWCClient();
        try {
            client.doCleanup(path);
        } catch (SVNException e) {
            throw SVNExceptionConverter.convert(e);
        }
    }

    public void cleanup() throws VCSException {
        cleanup(getRootFile());
    }

    public void addPreTransactionListener(VCSTransactionListener listener) {
        preTransactionListeners.add(listener);
    }

    public void removePreTransactionListener(VCSTransactionListener listener) {
        preTransactionListeners.remove(listener);
    }

    public void addPostTransactionListener(VCSTransactionListener listener) {
        postTransactionListeners.add(listener);
    }

    public void removePostTransactionListener(VCSTransactionListener listener) {
        postTransactionListeners.remove(listener);
    }

    public void makeStandalone() {
        deleteSVNFolders(new File(source.getLocalRootDirectory()));
        String artifactLimboLocation = source.getPersistenceHelper().getDataLayout().getArtifactsLimboDir();
        String artifactDirLocation = source.getPersistenceHelper().getDataLayout().getArtifactsDir();
        File artifactLimboDir = new File(artifactLimboLocation);
        if (artifactLimboDir.exists() && (artifactLimboDir.listFiles().length > 0)) {
            File artifactDir = new File(artifactDirLocation);
            FileUtils.copyDir(artifactLimboDir, artifactDir);
            if (!FileUtils.deleteDirContents(artifactLimboDir)) {
                logger.log(Level.SEVERE, "Make Stand Alone - unable to remove the artifact limbo fragment, " + artifactLimboDir);
            }
        }
    }

    private void deleteSVNFolders(File directory) {
        if (!directory.exists()) {
            return;
        }
        for (File subDir : directory.listFiles(DirectoryFilter.getInstance())) {
            deleteSVNFolders(subDir);
        }
        if (directory.getName().equals(".svn")) {
            FileUtils.deleteDir(directory);
        }
    }

    public void addUnversioned(String openPathString) throws SVNException {
        ISVNStatusHandler handler = new ISVNStatusHandler() {

            public void handleStatus(SVNStatus status) throws SVNException {
                if (status.getContentsStatus() == SVNStatusType.STATUS_UNVERSIONED) {
                    if (status.getFile().toString().endsWith(".xpx")) {
                        getClientManager().getWCClient().doAdd(status.getFile(), false, false, false, true);
                    }
                }
            }
        };
        File file = new File(openPathString);
        getSVNStatusClient().doStatus(file, true, false, false, false, handler);
    }

    public void setRevisionProperty(String name, String value, long version) throws VCSException {
        SVNWCClient client = getClientManager().getWCClient();
        SVNURL url;
        PropertyHandler propHandler = new PropertyHandler();
        try {
            url = SVNURL.parseURIEncoded(source.getRepositoryURL());
            SVNRevision rev = SVNRevision.create(version);
            SVNPropertyValue svnPropertyValue = SVNPropertyValue.create(value);
            client.doSetRevisionProperty(url, rev, name, svnPropertyValue, false, propHandler);
        } catch (SVNException e) {
            throw SVNExceptionConverter.convert(e);
        }
    }

    public String getRevisionProperty(String name, long version) throws VCSException {
        SVNWCClient client = getClientManager().getWCClient();
        PropertyHandler propHandler = new PropertyHandler();
        SVNURL url;
        try {
            url = SVNURL.parseURIEncoded(source.getRepositoryURL());
            SVNRevision rev = SVNRevision.create(version);
            client.doGetRevisionProperty(url, name, rev, propHandler);
        } catch (SVNException e) {
            throw SVNExceptionConverter.convert(e);
        }
        return propHandler.getValue();
    }

    /**
     * Copy src from one location in the repository to the
     * destination.
     *
     * @param src
     * @param destination
     */
    public void copy(String src, String destination) {
        SVNURL svnUrl = null;
        SVNRepository repos = null;
        try {
            svnUrl = SVNURL.parseURIEncoded(source.getDatasourceURL());
            repos = SVNRepositoryFactory.create(svnUrl);
            repos.setAuthenticationManager(authManager);
            long version = repos.getLatestRevision();
            ISVNEditor editor = repos.getCommitEditor("", null, true, null);
            editor.openRoot(-1);
            String destDir = destination.substring(0, destination.lastIndexOf("/"));
            editor.openDir(destDir, version);
            editor.addFile(destination, src, version);
            editor.closeDir();
            editor.closeEdit();
        } catch (SVNException e) {
            e.printStackTrace();
        }
    }

    public void testConnection() throws VCSException {
        SVNURL svnUrl = null;
        SVNRepository repos = null;
        try {
            svnUrl = SVNURL.parseURIEncoded(source.getDatasourceURL());
            repos = SVNRepositoryFactory.create(svnUrl);
            repos.setAuthenticationManager(authManager);
            repos.testConnection();
        } catch (SVNException e) {
            throw SVNExceptionConverter.convert(e);
        }
    }

    public void testConnection(VCSPasswordAuth auth) throws VCSException {
        SVNURL svnUrl = null;
        SVNRepository repos = null;
        ISVNAuthenticationManager authMgr = new BasicAuthenticationManager(auth.getUserName(), auth.getPassword());
        try {
            svnUrl = SVNURL.parseURIEncoded(source.getDatasourceURL());
            repos = SVNRepositoryFactory.create(svnUrl);
            repos.setAuthenticationManager(authMgr);
            repos.testConnection();
        } catch (SVNException e) {
            throw SVNExceptionConverter.convert(e);
        }
    }

    public void getPath(String path, OutputStream out) throws VCSException {
        try {
            svnRepository.getFile(path, -1, null, out);
        } catch (SVNException e) {
            throw SVNExceptionConverter.convert(e);
        }
    }

    public String getRepositoryURL() throws VCSException {
        SVNURL url = null;
        try {
            url = svnRepository.getRepositoryRoot(true);
            return url.toString();
        } catch (SVNException e) {
            throw SVNExceptionConverter.convert(e);
        }
    }

    public void unload() {
        SVNEventHandler.getInstance().setPersistenceHelper(null);
        clientManager = null;
    }

    public boolean isWriteable(Xelement element) throws VCSException {
        XchangeElement xchangeElement = null;
        if (element instanceof XchangeElement) {
            xchangeElement = (XchangeElement) element;
        } else {
            xchangeElement = element.getContainedIn();
        }
        String openFileFragment = source.getPersistenceHelper().getFileIndex().getOpenPathFragment(xchangeElement.getId());
        openFileFragment = FileUtils.fixPath(openFileFragment);
        boolean fileWriteable = false;
        ISVNEditor editor = null;
        try {
            String openFragment = source.getDescriptor().getOpenDirectoryFragment();
            String urlToElement = source.getDatasourceURL() + "/" + openFragment;
            SVNURL url = SVNURL.parseURIDecoded(urlToElement);
            SVNRepository repos = SVNRepositoryFactory.create(url);
            repos.setAuthenticationManager(new BasicAuthenticationManager(source.getUserName(), source.getPassword()));
            editor = repos.getCommitEditor("", null, true, null);
            editor.openRoot(-1);
            editor.openFile(openFileFragment, -1);
            fileWriteable = true;
            if (xchangeElement instanceof XchangeElementContainer) {
                String relativeDirPath = openFileFragment.substring(1, openFileFragment.lastIndexOf(".xpx"));
                editor.openDir(relativeDirPath, -1);
                if (xchangeElement instanceof Xproject) {
                    Xproject project = (Xproject) xchangeElement;
                    String delegateTaskFragment = source.getPersistenceHelper().getFileIndex().getOpenPathFragment(project.getDelegateTask().getId());
                    delegateTaskFragment = FileUtils.fixPath(delegateTaskFragment);
                    editor.openFile(delegateTaskFragment, -1);
                }
            }
        } catch (SVNAuthenticationException e) {
            if (fileWriteable) {
                System.err.println("Container file writable, but not the directory...");
            }
            return false;
        } catch (SVNException e) {
            throw SVNExceptionConverter.convert(e);
        } finally {
            if (editor != null) {
                try {
                    editor.abortEdit();
                } catch (SVNException e) {
                    throw SVNExceptionConverter.convert(e);
                }
            }
        }
        return true;
    }

    private class PropertyHandler implements ISVNPropertyHandler {

        String name = null;

        String value = null;

        public void handleProperty(File arg0, SVNPropertyData prop) {
            setVars(prop);
        }

        public void handleProperty(SVNURL arg0, SVNPropertyData prop) {
            setVars(prop);
        }

        public void handleProperty(long arg0, SVNPropertyData prop) {
            setVars(prop);
        }

        private void setVars(SVNPropertyData prop) {
            name = prop.getName();
            value = prop.getValue().getString();
        }

        public String getValue() {
            return value;
        }
    }

    public enum PathChangeType {

        UPDATE_COMPLETED, COMMIT_DELTA_SENT, COMMIT_MODIFIED, COMMIT_DELETED, COMMIT_REPLACED, COMMIT_ADDED, COMMIT_COMPLETED, UPDATE_ADD, UPDATE_DELETE, UPDATE_UPDATE, UPDATE_CONFLICTED, UPDATE_MERGED
    }
}
