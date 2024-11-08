package edu.psu.its.lionshare.share.gnutella;

import edu.psu.its.lionshare.security.AuthorizationManager;
import edu.psu.its.lionshare.security.DefaultSecurityManager;
import edu.psu.its.lionshare.metadata.MetadataManager;
import java.io.*;
import java.net.*;
import java.util.Collections;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.bootstrap.BootstrapServerManager;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.filters.*;
import com.limegroup.gnutella.downloader.*;
import com.limegroup.gnutella.uploader.*;
import com.limegroup.gnutella.chat.*;
import com.limegroup.gnutella.xml.*;
import com.limegroup.gnutella.security.ServerAuthenticator;
import com.limegroup.gnutella.security.Authenticator;
import com.limegroup.gnutella.security.Cookies;
import com.limegroup.gnutella.statistics.OutOfBandThroughputStat;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.updates.*;
import com.limegroup.gnutella.settings.*;
import com.limegroup.gnutella.browser.*;
import com.limegroup.gnutella.search.*;
import com.limegroup.gnutella.upelection.*;
import com.limegroup.gnutella.tigertree.TigerTreeCache;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 *
 * Extends the Gnutella Router Service to enable the querying of peerserver,
 * as well as facilitate the use of a SecureDownloader.
 *
 * @author Lorin Metzger.
 * LionShareP2P.
 *
 */
public class LionShareRouterService extends RouterService {

    private static final Log LOG = LogFactory.getLog(LionShareRouterService.class);

    private static final LionShareSecureAcceptor secure_acceptor = new LionShareSecureAcceptor();

    private static final MetadataManager metadata_manager = new edu.psu.its.lionshare.metadata.manager.DatabaseMetadataManager();

    private static final GnutellaAuthorizationManager authorization_manager = new GnutellaAuthorizationManager();

    private static ActivityCallback callback;

    /**
   *
   * Contructs a new <code>com.limegroup.gnutella.RouterService</code> using
   * the given ActivityCallback, and a <code>LionShareMessageRouter</code> and
   * a <code>ShareFileManager</code>
   *
   */
    public LionShareRouterService(ActivityCallback callback) {
        super(callback, new LionShareMessageRouter(), new ShareFileManager());
        this.callback = callback;
        DefaultSecurityManager.getInstance().addListener((ShareFileManager) this.getFileManager());
    }

    public void start() {
        super.start();
        synchronized (LionShareRouterService.class) {
            secure_acceptor.start();
            LOG.debug("SETUP SECURE SSL SERVER SOCKET");
        }
    }

    public static LionShareSecureAcceptor getSecureAcceptor() {
        return secure_acceptor;
    }

    public static AuthorizationManager getAuthorizationManager() {
        return authorization_manager;
    }

    public static int getSecurePort() {
        return secure_acceptor.getPort(true);
    }

    public static MetadataManager getMetadataManager() {
        return metadata_manager;
    }

    /**
   * This method was overridden so we could check to see if the file
   * being downloaded needs to be downloaded over a secure connection.
   * If so it should call the SecureFileDownloader that hasn't been written yet.
   *
   * TODO: Need to create a SecureFileDownloader.
   *
   */
    public static Downloader download(RemoteFileDesc[] files, java.util.List alts, boolean overwrite, GUID queryGUID) throws FileExistsException, AlreadyDownloadingException, java.io.FileNotFoundException {
        return RouterService.download(files, alts, overwrite, queryGUID);
    }

    /**
   * This method was overridden so we could check to see if the file
   * being downloaded needs to be downloaded over a secure connection.
   * If so it should call the SecureFileDownloader that hasn't been written yet.
   *
   * TODO: Need to create a SecureFileDownloader.
   *
   */
    public static Downloader download(RemoteFileDesc[] files, boolean overwrite, GUID queryGUID) throws FileExistsException, AlreadyDownloadingException, java.io.FileNotFoundException {
        return download(files, Collections.EMPTY_LIST, overwrite, queryGUID);
    }

    /**
   * This method was overridden so we could check to see if the file
   * being downloaded needs to be downloaded over a secure connection.
   * If so it should call the SecureFileDownloader that hasn't been written yet.
   *
   * TODO: Need to create a SecureFileDownloader.
   *
   */
    public static synchronized Downloader download(URN urn, String textQuery, String filename, String[] defaultURL, boolean overwrite) throws IllegalArgumentException, AlreadyDownloadingException, FileExistsException {
        return RouterService.download(urn, textQuery, filename, defaultURL, overwrite);
    }

    /**
   * This method was overridden so we could check to see if the file
   * being downloaded needs to be downloaded over a secure connection.
   * If so it should call the SecureFileDownloader that hasn't been written yet.
   *
   * TODO: Need to create a SecureFileDownloader.
   *
   */
    public static Downloader downloadFile(File incompleteFile) throws AlreadyDownloadingException, CantResumeException {
        return RouterService.download(incompleteFile);
    }

    /**
   *
   * This method was overridden so we could query the peerservers as well,
   * as the LionShare/Gnutella network.
   *
   *
   * TODO: Still need to put in the code to query peerservers.
   *
   */
    public static void query(final byte[] guid, final String query, final String richQuery, final MediaType type) {
        RouterService.query(guid, query, richQuery, type);
    }
}
