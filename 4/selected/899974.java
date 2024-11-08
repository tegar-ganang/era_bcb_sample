package org.opennms.protocols.sftp;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;
import org.opennms.core.utils.ThreadCategory;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

/**
 * The class for managing SFTP URL Connection.
 * <p>The default connection timeout is 30 seconds.</p>
 * 
 * @author <a href="mailto:agalue@opennms.org">Alejandro Galue</a>
 */
public class SftpUrlConnection extends URLConnection {

    /** The Constant default timeout in milliseconds. */
    public static final int DEFAULT_TIMEOUT = 30000;

    /** The SSH session. */
    private Session m_session;

    /** The SFTP channel. */
    private ChannelSftp m_channel;

    /** The connection flag, true when the connection has been started. */
    private boolean m_connected = false;

    /**
     * Instantiates a new SFTP URL connection.
     *
     * @param url the URL
     */
    protected SftpUrlConnection(URL url) {
        super(url);
    }

    @Override
    public void connect() throws IOException {
        if (m_connected) {
            return;
        }
        m_connected = true;
        if (url.getUserInfo() == null) {
            throw new IOException("User credentials required.");
        }
        JSch jsch = new JSch();
        try {
            String prvkey = System.getProperty("sftp.private-key.location");
            if (prvkey != null) {
                jsch.addIdentity(prvkey);
            }
            int port = url.getPort() > 0 ? url.getPort() : url.getDefaultPort();
            String[] userInfo = url.getUserInfo().split(":");
            m_session = jsch.getSession(userInfo[0], url.getHost(), port);
            if (userInfo.length > 1) {
                m_session.setPassword(userInfo[1]);
            }
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            m_session.setConfig(config);
            m_session.setTimeout(DEFAULT_TIMEOUT);
            m_session.connect();
            m_channel = (ChannelSftp) m_session.openChannel("sftp");
            m_channel.connect();
        } catch (JSchException e) {
            disconnect();
            throw new IOException("Can't connect using " + url + " because " + e.getMessage());
        }
    }

    /**
     * Disconnect.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public void disconnect() throws IOException {
        if (m_channel != null) m_channel.disconnect();
        if (m_session != null) m_session.disconnect();
    }

    /**
     * Gets the channel.
     *
     * @return the channel
     */
    public ChannelSftp getChannel() throws IOException {
        if (m_channel == null) {
            connect();
        }
        return m_channel;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        String filePath = getPath();
        try {
            return getChannel().get(filePath);
        } catch (SftpException e) {
            throw new IOException("Can't retrieve " + filePath + " from " + url.getHost() + " because " + e.getMessage());
        }
    }

    /**
     * Gets the path.
     *
     * @return the path
     * @throws SftpUrlException the SFTP URL exception
     */
    protected String getPath() throws SftpUrlException {
        return url.getPath();
    }

    /**
     * Log.
     *
     * @return the thread category
     */
    protected ThreadCategory log() {
        return ThreadCategory.getInstance(getClass());
    }
}
