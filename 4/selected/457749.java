package n2hell.xmlrpc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import n2hell.config.ScgiConfig;
import n2hell.config.SshConfig;
import com.trilead.ssh2.Connection;
import com.trilead.ssh2.LocalStreamForwarder;

/**
 * ssh transport
 * 
 * @author vsolomenchuk
 * 
 */
public class SSHTransport implements Transport {

    private final ScgiConfig scgiConfig;

    private final SshConfig sshConfig;

    private Connection connection;

    private LocalStreamForwarder channel;

    /**
	 * constructor
	 * 
	 * @throws JSchException
	 * @throws IOException
	 */
    public SSHTransport(SshConfig sshConfig, ScgiConfig scgiConfig) {
        this.sshConfig = sshConfig;
        this.scgiConfig = scgiConfig;
    }

    public void close() throws IOException {
        if (channel != null) channel.close();
        channel = null;
    }

    public InputStream getInputStream() throws IOException {
        return getChannel().getInputStream();
    }

    public OutputStream getOutputStream() throws IOException {
        return getChannel().getOutputStream();
    }

    public void setTimeout(int timeout) {
    }

    public void shutdownOutput() throws IOException {
        channel.getOutputStream().close();
    }

    public void connect() throws IOException {
        connection = SshFactory.getConnection(sshConfig);
    }

    private LocalStreamForwarder getChannel() throws IOException {
        if (channel == null) channel = connection.createLocalStreamForwarder(scgiConfig.getHost(), scgiConfig.getPort());
        return channel;
    }

    public boolean isConnected() {
        return connection != null && connection.isAuthenticationComplete();
    }
}
