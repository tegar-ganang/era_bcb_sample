package de.ddb.conversion.ssh;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Map;
import org.apache.commons.io.input.ReaderInputStream;
import org.apache.commons.io.output.WriterOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import de.ddb.conversion.CharacterNormalizer;
import de.ddb.conversion.ConverterException;
import de.ddb.conversion.util.CopyInputStream;

/**
 * @author kett
 * 
 */
public class RemoteExec {

    /**
	 * Default connection time out to be set on channel.
	 */
    public static final int CHANNEL_CONNECTION_TIMOUT = 10000;

    /**
	 * Logger for this class
	 */
    private static final Log LOGGER = LogFactory.getLog(RemoteExec.class);

    private String user;

    private String host;

    private String password;

    private String command;

    private Map<String, Object> environmentProperties;

    private Session session;

    private int port = 22;

    /**
	 * @return port
	 */
    public int getPort() {
        return this.port;
    }

    /**
	 * @param port
	 */
    public void setPort(final int port) {
        this.port = port;
    }

    /**
	 * @return environmentProperties
	 */
    public Map<String, Object> getEnvironmentProperties() {
        return this.environmentProperties;
    }

    /**
	 * @param environmentProperties
	 */
    public void setEnvironmentProperties(final Map<String, Object> environmentProperties) {
        this.environmentProperties = environmentProperties;
    }

    /**
	 * @throws JSchException
	 */
    final void connect() throws JSchException {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Preparing command invokation [" + getCommand() + "]. Connecting to host[" + this.host + "]...");
        }
        final JSch jsch = new JSch();
        JSch.setConfig("StrictHostKeyChecking", "no");
        this.session = jsch.getSession(this.user, this.host, this.port);
        this.session.setPassword(this.password);
        this.session.connect();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Connected.");
        }
    }

    /**
	 * 
	 */
    public void disconnect() {
        if (this.session != null) {
            this.session.disconnect();
        }
    }

    /**
	 * 
	 */
    public void destroy() {
        disconnect();
        this.session = null;
        this.command = null;
        this.environmentProperties = null;
        this.host = null;
        this.password = null;
        this.user = null;
    }

    /**
	 * @param in
	 * @param out
	 * @param processInCharset
	 * @param processOutCharset
	 * @param procInNormalizer
	 * @param procOutNormalizer
	 * @throws JSchException
	 * @throws ConverterException
	 * @throws IOException
	 */
    public void remoteExec(final Reader in, final Writer out, final Charset processInCharset, final Charset processOutCharset, final CharacterNormalizer procInNormalizer, final CharacterNormalizer procOutNormalizer) throws JSchException, IOException, ConverterException {
        final ReaderInputStream ris = new ReaderInputStream(in, processInCharset);
        final WriterOutputStream wos = new WriterOutputStream(out, processOutCharset);
        processRequest(ris, wos);
    }

    /**
	 * @param in
	 * @param out
	 * @throws JSchException
	 * @throws ConverterException
	 * @throws IOException
	 */
    public void remoteExec(final InputStream in, final OutputStream out) throws JSchException, IOException {
        processRequest(in, out);
    }

    /**
	 * @param input
	 * @return byteArray
	 * @throws IOException
	 */
    public byte[] remoteExec(final byte[] input) throws IOException {
        final InputStream in = new ByteArrayInputStream(input);
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        processRequest(in, out);
        return out.toByteArray();
    }

    /**
	 * @param in
	 * @param out
	 * @throws IOException
	 */
    protected void processRequest(final InputStream in, final OutputStream out) throws IOException {
        ChannelExec channel = null;
        OutputStream channelOut = null;
        InputStream channelIn = null;
        InputStream channelErrorIn = null;
        int numBytes = 0;
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Executing command: [" + getCommand() + "].");
        }
        try {
            channel = getChannel();
            channelOut = channel.getOutputStream();
            channelIn = channel.getInputStream();
            channelErrorIn = channel.getErrStream();
            final ByteArrayOutputStream errorOut = new ByteArrayOutputStream();
            final CopyInputStream writeToChannel = new CopyInputStream("Write to Process", in, channelOut);
            final Thread writeToProcessThread = new Thread(writeToChannel);
            final Thread readErrorsThread = new Thread(new CopyInputStream("Read Errors", channelErrorIn, errorOut));
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Start seperate io threads.");
            }
            readErrorsThread.start();
            writeToProcessThread.start();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Start looping around read process.");
            }
            int i;
            final byte[] tmp = new byte[1024];
            while (true) {
                i = 0;
                while (channelIn.available() > 0) {
                    i = channelIn.read(tmp);
                    if (i < 0) {
                        break;
                    }
                    out.write(tmp, 0, i);
                    numBytes += i;
                }
                if (channel.isClosed()) {
                    break;
                }
            }
            readErrorsThread.join();
            out.flush();
            LOGGER.info("Finished reading incoming. Checking error conditions.");
            final String error = new String(errorOut.toByteArray(), "UTF-8");
            if (error != null && error.length() > 0) {
                throw new IOException(error);
            }
            if (writeToChannel.getException() != null) {
                throw new IOException(writeToChannel.getException());
            }
            if (numBytes < 1 && channel.getExitStatus() == 0) {
                throw new IOException("Disappointing response from command [" + getCommand() + "]. More then [" + numBytes + "] bytes or RC != 0 expected.");
            }
        } catch (final InterruptedException e) {
            LOGGER.error(e, e);
        } catch (final JSchException e) {
            throw new IOException(e);
        } finally {
            LOGGER.info("Number of bytes received from channel: [" + numBytes + "].");
            if (channel != null) {
                LOGGER.info("Exit code: [" + channel.getExitStatus() + "].");
                channel.disconnect();
            }
            if (out != null) {
                out.close();
            }
            if (channelIn != null) {
                channelIn.close();
            }
        }
    }

    /**
	 * @author kett
	 * 
	 */
    public static class MyLogger implements com.jcraft.jsch.Logger {

        static java.util.Map<Integer, String> name = new java.util.Hashtable<Integer, String>();

        static {
            name.put(new Integer(DEBUG), "DEBUG: ");
            name.put(new Integer(INFO), "INFO: ");
            name.put(new Integer(WARN), "WARN: ");
            name.put(new Integer(ERROR), "ERROR: ");
            name.put(new Integer(FATAL), "FATAL: ");
        }

        public boolean isEnabled(final int level) {
            return true;
        }

        public void log(final int level, final String message) {
            switch(level) {
                case DEBUG:
                    LOGGER.debug(message);
                    break;
                case INFO:
                    LOGGER.info(message);
                    break;
                case WARN:
                    LOGGER.warn(message);
                    break;
                case ERROR:
                    LOGGER.error(message);
                    break;
                case FATAL:
                    LOGGER.fatal(message);
                    break;
            }
        }
    }

    /**
	 * @return user
	 */
    public String getUser() {
        return this.user;
    }

    /**
	 * @param user
	 */
    public void setUser(final String user) {
        this.user = user;
    }

    /**
	 * @return host
	 */
    public String getHost() {
        return this.host;
    }

    /**
	 * @param host
	 */
    public void setHost(final String host) {
        this.host = host;
    }

    /**
	 * @return password
	 */
    public String getPassword() {
        return this.password;
    }

    /**
	 * @param password
	 */
    public void setPassword(final String password) {
        this.password = password;
    }

    /**
	 * @return command
	 */
    public String getCommand() {
        return this.command;
    }

    /**
	 * @param command
	 */
    public void setCommand(final String command) {
        this.command = command;
    }

    private ChannelExec getChannel() throws JSchException {
        ChannelExec channel = null;
        try {
            if (this.session == null) {
                connect();
            }
            channel = (ChannelExec) this.session.openChannel("exec");
            channel.setCommand(this.command);
            if (this.environmentProperties != null) {
                for (final String key : this.environmentProperties.keySet()) {
                    LOGGER.debug("setting environment property: " + key + "=" + this.environmentProperties.get(key));
                    channel.setEnv(key, (String) this.environmentProperties.get(key));
                }
            }
            channel.connect();
        } catch (final JSchException e) {
            LOGGER.warn("Failed to receive channel from session. Current command [" + getCommand() + "]. Disconnecting...");
            disconnect();
            this.session = null;
            throw e;
        }
        return channel;
    }
}
