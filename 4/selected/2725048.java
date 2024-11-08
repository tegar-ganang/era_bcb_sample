package org.apache.commons.vfs.provider.sftp;

import java.io.IOException;
import java.util.Collection;
import org.apache.commons.vfs.FileName;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystem;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemOptions;
import org.apache.commons.vfs.UserAuthenticationData;
import org.apache.commons.vfs.provider.AbstractFileSystem;
import org.apache.commons.vfs.provider.GenericFileName;
import org.apache.commons.vfs.util.UserAuthenticatorUtils;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

/**
 * Represents the files on an SFTP server.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision: 480428 $ $Date: 2006-11-28 22:15:24 -0800 (Tue, 28 Nov 2006) $
 */
public class SftpFileSystem extends AbstractFileSystem implements FileSystem {

    private Session session;

    private ChannelSftp idleChannel;

    protected SftpFileSystem(final GenericFileName rootName, final Session session, final FileSystemOptions fileSystemOptions) {
        super(rootName, null, fileSystemOptions);
        this.session = session;
    }

    protected void doCloseCommunicationLink() {
        if (idleChannel != null) {
            idleChannel.disconnect();
            idleChannel = null;
        }
        if (session != null) {
            session.disconnect();
            session = null;
        }
    }

    /**
     * Returns an SFTP channel to the server.
     */
    protected ChannelSftp getChannel() throws IOException {
        if (this.session == null) {
            Session session;
            UserAuthenticationData authData = null;
            try {
                final GenericFileName rootName = (GenericFileName) getRootName();
                authData = UserAuthenticatorUtils.authenticate(getFileSystemOptions(), SftpFileProvider.AUTHENTICATOR_TYPES);
                session = SftpClientFactory.createConnection(rootName.getHostName(), rootName.getPort(), UserAuthenticatorUtils.getData(authData, UserAuthenticationData.USERNAME, UserAuthenticatorUtils.toChar(rootName.getUserName())), UserAuthenticatorUtils.getData(authData, UserAuthenticationData.PASSWORD, UserAuthenticatorUtils.toChar(rootName.getPassword())), getFileSystemOptions());
            } catch (final Exception e) {
                throw new FileSystemException("vfs.provider.sftp/connect.error", getRootName(), e);
            } finally {
                UserAuthenticatorUtils.cleanup(authData);
            }
            this.session = session;
        }
        try {
            final ChannelSftp channel;
            if (idleChannel != null) {
                channel = idleChannel;
                idleChannel = null;
            } else {
                channel = (ChannelSftp) session.openChannel("sftp");
                channel.connect();
                Boolean userDirIsRoot = SftpFileSystemConfigBuilder.getInstance().getUserDirIsRoot(getFileSystemOptions());
                String workingDirectory = getRootName().getPath();
                if (workingDirectory != null && (userDirIsRoot == null || !userDirIsRoot.booleanValue())) {
                    try {
                        channel.cd(workingDirectory);
                    } catch (SftpException e) {
                        throw new FileSystemException("vfs.provider.sftp/change-work-directory.error", workingDirectory);
                    }
                }
            }
            return channel;
        } catch (final JSchException e) {
            throw new FileSystemException("vfs.provider.sftp/connect.error", getRootName(), e);
        }
    }

    /**
     * Returns a channel to the pool.
     */
    protected void putChannel(final ChannelSftp channel) {
        if (idleChannel == null) {
            if (channel.isConnected() && !channel.isClosed()) {
                idleChannel = channel;
            }
        } else {
            channel.disconnect();
        }
    }

    /**
     * Adds the capabilities of this file system.
     */
    protected void addCapabilities(final Collection caps) {
        caps.addAll(SftpFileProvider.capabilities);
    }

    /**
     * Creates a file object.  This method is called only if the requested
     * file is not cached.
     */
    protected FileObject createFile(final FileName name) throws FileSystemException {
        return new SftpFileObject(name, this);
    }

    /**
     * last mod time is only a int and in seconds, thus can be off by 999
     *
     * @return 1000
     */
    public double getLastModTimeAccuracy() {
        return 1000L;
    }
}
