package de.ddb.conversion.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Map;
import org.apache.log4j.Logger;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import de.ddb.conversion.CharacterNormalizer;
import de.ddb.conversion.ConverterException;

/**
 * @author kett
 *
 */
public class RemoteExec {

    /**
	 * Logger for this class
	 */
    static final Logger logger = Logger.getLogger(RemoteExec.class);

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
        return port;
    }

    /**
	 * @param port
	 */
    public void setPort(int port) {
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
    public void setEnvironmentProperties(Map<String, Object> environmentProperties) {
        this.environmentProperties = environmentProperties;
    }

    /**
	 * @throws JSchException
	 */
    public void connect() throws JSchException {
        if (session == null) {
            JSch jsch = new JSch();
            JSch.setConfig("StrictHostKeyChecking", "no");
            this.session = jsch.getSession(this.user, this.host, this.port);
            this.session.setPassword(this.password);
        }
        this.session.connect();
    }

    private ChannelExec getChannel() throws JSchException {
        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand(this.command);
        if (environmentProperties != null) {
            for (String key : environmentProperties.keySet()) {
                logger.debug("setting environment property: " + key + "=" + environmentProperties.get(key));
                channel.setEnv(key, (String) environmentProperties.get(key));
            }
        }
        channel.connect();
        return channel;
    }

    /**
	 * 
	 */
    public void disconnect() {
        if (session == null) {
            session.disconnect();
        }
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
    public void remoteExec(Reader in, Writer out, Charset processInCharset, Charset processOutCharset, CharacterNormalizer procInNormalizer, CharacterNormalizer procOutNormalizer) throws JSchException, ConverterException, IOException {
        boolean isDisconnect = false;
        ChannelExec channel = null;
        Writer writeToProcess = null;
        Reader readFromProcess = null;
        Reader readErrorsFromProcess = null;
        try {
            if (session == null) {
                isDisconnect = true;
                connect();
            }
            channel = getChannel();
            long time = System.currentTimeMillis();
            if (logger.isDebugEnabled()) {
                logger.debug("remoteExec() - creating process and streams");
            }
            writeToProcess = new BufferedWriter(new OutputStreamWriter((channel.getOutputStream()), processInCharset));
            readFromProcess = new BufferedReader(new InputStreamReader((channel.getInputStream()), processOutCharset));
            readErrorsFromProcess = new BufferedReader(new InputStreamReader((channel.getErrStream())));
            StringWriter writeErrors = new StringWriter();
            if (logger.isDebugEnabled()) {
                logger.debug("remoteExec() - finished creating process and streams (time=" + (System.currentTimeMillis() - time) + "ms)");
            }
            if (logger.isDebugEnabled()) {
                logger.debug("remoteExec() - start writing/reading to/from process");
            }
            time = System.currentTimeMillis();
            Runnable writeToProcessThread = new CopyCharStream("Write to Process", in, writeToProcess, procInNormalizer);
            Thread readFromProcessThread = new Thread(new CopyCharStream("Read from Process", readFromProcess, out, procOutNormalizer));
            Thread readErrorsThread = new Thread(new CopyCharStream("Read Errors", readErrorsFromProcess, writeErrors, null));
            readErrorsThread.start();
            readFromProcessThread.start();
            writeToProcessThread.run();
            if (logger.isDebugEnabled()) {
                logger.debug("remoteExec() - finished writing input (time=" + (System.currentTimeMillis() - time) + "ms)");
            }
            readFromProcessThread.join();
            if (logger.isDebugEnabled()) {
                logger.debug("remoteExec() - finished reading output (time=" + (System.currentTimeMillis() - time) + "ms)");
            }
            readErrorsThread.join();
            if (logger.isDebugEnabled()) {
                logger.debug("remoteExec() - finished reading errors (time=" + (System.currentTimeMillis() - time) + "ms)");
            }
            if (logger.isDebugEnabled()) {
                logger.debug("remoteExec() - process has terminated (time=" + (System.currentTimeMillis() - time) + "ms)");
            }
            String error = writeErrors.getBuffer().toString();
            if (error != null && error.length() > 0) {
                throw new IOException(error);
            }
            logger.debug("remoteExec() -  exit code: " + channel.getExitStatus());
        } catch (InterruptedException e) {
            logger.error(e);
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
            if (isDisconnect) {
                if (session != null) {
                    session.disconnect();
                }
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("remoteExec() - end");
        }
    }

    /**
	 * @param in
	 * @param out
	 * @throws JSchException
	 * @throws ConverterException
	 * @throws IOException
	 */
    public void remoteExec(InputStream in, OutputStream out) throws JSchException, ConverterException, IOException {
        boolean isDisconnect = false;
        ChannelExec channel = null;
        OutputStream writeToProcess = null;
        InputStream readFromProcess = null;
        InputStream readErrorsFromProcess = null;
        try {
            if (session == null) {
                isDisconnect = true;
                connect();
            }
            channel = getChannel();
            long time = System.currentTimeMillis();
            if (logger.isDebugEnabled()) {
                logger.debug("remoteExec() - creating process and streams");
            }
            writeToProcess = channel.getOutputStream();
            readFromProcess = channel.getInputStream();
            readErrorsFromProcess = channel.getErrStream();
            ByteArrayOutputStream writeErrors = new ByteArrayOutputStream();
            if (logger.isDebugEnabled()) {
                logger.debug("remoteExec() - finished creating process and streams (time=" + (System.currentTimeMillis() - time) + "ms)");
            }
            if (logger.isDebugEnabled()) {
                logger.debug("remoteExec() - start writing/reading to/from process");
            }
            time = System.currentTimeMillis();
            Runnable writeToProcessThread = new CopyInputStream("Write to Process", in, writeToProcess);
            Thread readFromProcessThread = new Thread(new CopyInputStream("Read from Process", readFromProcess, out));
            Thread readErrorsThread = new Thread(new CopyInputStream("Read Errors", readErrorsFromProcess, writeErrors));
            readErrorsThread.start();
            readFromProcessThread.start();
            writeToProcessThread.run();
            if (logger.isDebugEnabled()) {
                logger.debug("remoteExec() - finished writing input (time=" + (System.currentTimeMillis() - time) + "ms)");
            }
            readFromProcessThread.join();
            if (logger.isDebugEnabled()) {
                logger.debug("remoteExec() - finished reading output (time=" + (System.currentTimeMillis() - time) + "ms)");
            }
            readErrorsThread.join();
            if (logger.isDebugEnabled()) {
                logger.debug("remoteExec() - finished reading errors (time=" + (System.currentTimeMillis() - time) + "ms)");
            }
            if (logger.isDebugEnabled()) {
                logger.debug("remoteExec() - process has terminated (time=" + (System.currentTimeMillis() - time) + "ms)");
            }
            String error = new String(writeErrors.toByteArray(), "UTF-8");
            if (error != null && error.length() > 0) {
                throw new IOException(error);
            }
            logger.debug("remoteExec() -  exit code: " + channel.getExitStatus());
        } catch (InterruptedException e) {
            logger.error(e);
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
            if (isDisconnect) {
                if (session != null) {
                    session.disconnect();
                }
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("remoteExec() - end");
        }
    }

    /**
	 * @param input
	 * @return byteArray
	 * @throws IOException
	 */
    public byte[] remoteExec(byte[] input) throws IOException {
        boolean isDisconnect = false;
        byte[] ret = null;
        ChannelExec channel = null;
        try {
            if (session == null) {
                isDisconnect = true;
                connect();
            }
            channel = getChannel();
            if (input != null) {
                OutputStream processOut = channel.getOutputStream();
                processOut.write(input);
                processOut.close();
            }
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            InputStream processOut = channel.getInputStream();
            int i;
            while ((i = processOut.read()) != -1) {
                buffer.write(i);
            }
            logger.debug("remoteExec() -  exit code: " + channel.getExitStatus());
            ret = buffer.toByteArray();
        } catch (JSchException e) {
            throw new IOException(e.getMessage());
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
            if (isDisconnect) {
                if (session != null) {
                    session.disconnect();
                }
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("remoteExec() - end");
        }
        return ret;
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

        public boolean isEnabled(int level) {
            return true;
        }

        public void log(int level, String message) {
            switch(level) {
                case (DEBUG):
                    logger.debug(message);
                    break;
                case (INFO):
                    logger.info(message);
                    break;
                case (WARN):
                    logger.warn(message);
                    break;
                case (ERROR):
                    logger.error(message);
                    break;
                case (FATAL):
                    logger.fatal(message);
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
    public void setUser(String user) {
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
    public void setHost(String host) {
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
    public void setPassword(String password) {
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
    public void setCommand(String command) {
        this.command = command;
    }
}
