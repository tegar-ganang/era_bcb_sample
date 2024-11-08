package de.ddb.conversion;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.jcraft.jsch.JSchException;
import de.ddb.charset.CharsetUtil;
import de.ddb.charset.EightBitCharset;
import de.ddb.conversion.ssh.RemoteExec;

/**
 * <p>
 * Invokes a process using a specified UNIX command. It than writes the input to
 * the process-in and reads the result from process-out. By default Charcters
 * will be NFD-normalized (decomposition of characters) before writing to the
 * process and NFC-normalized (composition of combining characters) after
 * reading from the process.
 * <p>
 */
public class BinaryConverter extends GenericConverter {

    /**
	 * ConverterContext property key to identify host value.
	 */
    public static final String PROPKEY_HOST = "Converter.host";

    /**
	 * ConverterContext property key to identify user value.
	 */
    public static final String PROPKEY_USER = "Converter.user";

    /**
	 * ConverterContext property key to identify password value.
	 */
    public static final String PROPKEY_PASSWORD = "Converter.password";

    /**
	 * Logger for this class
	 */
    private static final Log LOGGER = LogFactory.getLog(BinaryConverter.class);

    private static CharacterNormalizer NFC_NORMALIZER;

    private static CharacterNormalizer NFD_NORMALIZER;

    private static Charset IDENTITY_CHARSET = new EightBitCharset();

    private String processInEncoding;

    private String processOutEncoding;

    private CharacterNormalizer procInputCharNormalizer = NFD_NORMALIZER;

    private CharacterNormalizer procOutputCharNormalizer = NFC_NORMALIZER;

    private String command;

    private int port = 22;

    private RemoteExec remoteExec;

    private Map<String, Object> environmentProperties;

    /**
	 * @param command
	 * @param environmentProperties
	 * @param processInEncoding
	 * @param processOutEncoding
	 * @param numberOfProcesses
	 * @throws ConverterInitializationException
	 */
    public BinaryConverter(String command, List<String> environmentProperties, String processInEncoding, String processOutEncoding, int numberOfProcesses) throws ConverterInitializationException {
        this(command, processInEncoding, processOutEncoding);
        for (String prop : environmentProperties) {
            int seperatorPos = prop.indexOf('=');
            String key = prop.substring(0, seperatorPos).trim();
            String value = prop.substring(seperatorPos + 1).trim();
            getEnvironmentProperties().put(key, value);
        }
        for (String prop : environmentProperties) {
            int seperatorPos = prop.indexOf('=');
            String key = prop.substring(0, seperatorPos).trim();
            String value = prop.substring(seperatorPos + 1).trim();
            this.getEnvironmentProperties().put(key, value);
            if (this.getEnvironmentProperties().containsKey(BinaryConverter.PROPKEY_HOST) && this.getEnvironmentProperties().containsKey(BinaryConverter.PROPKEY_USER) && this.getEnvironmentProperties().containsKey(BinaryConverter.PROPKEY_PASSWORD)) {
                getConverterContext().getProperties().setProperty(BinaryConverter.PROPKEY_HOST, getEnvironmentProperties().get(BinaryConverter.PROPKEY_HOST).toString());
                getConverterContext().getProperties().setProperty(BinaryConverter.PROPKEY_USER, getEnvironmentProperties().get(BinaryConverter.PROPKEY_USER).toString());
                getConverterContext().getProperties().setProperty(BinaryConverter.PROPKEY_PASSWORD, getEnvironmentProperties().get(BinaryConverter.PROPKEY_PASSWORD).toString());
                LOGGER.debug("Found property configuration. Replaced config from resource file.");
            }
        }
    }

    /**
	 * <p>
	 * Konstruktor für Remote-Aufrufe
	 * </p>
	 * 
	 * @param command
	 * @param sourceEncoding
	 * @param targetEncoding
	 * @throws ConverterInitializationException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @since 19.12.2008
	 */
    public BinaryConverter(String command, String sourceEncoding, String targetEncoding) throws ConverterInitializationException {
        this.processInEncoding = sourceEncoding;
        this.processOutEncoding = targetEncoding;
        this.command = command;
        try {
            NFC_NORMALIZER = new CharacterNormalizer(CharacterNormalizer.NFC_MODE);
            NFD_NORMALIZER = new CharacterNormalizer(CharacterNormalizer.NFD_MODE);
        } catch (ClassNotFoundException e) {
            throw new ConverterInitializationException(e);
        }
        try {
            ResourceBundle config = ResourceBundle.getBundle("de.ddb.conversion.config.converter");
            this.getConverterContext().getProperties().setProperty(BinaryConverter.PROPKEY_HOST, config.getString(BinaryConverter.PROPKEY_HOST));
            this.getConverterContext().getProperties().setProperty(BinaryConverter.PROPKEY_USER, config.getString(BinaryConverter.PROPKEY_USER));
            this.getConverterContext().getProperties().setProperty(BinaryConverter.PROPKEY_PASSWORD, config.getString(BinaryConverter.PROPKEY_PASSWORD));
            LOGGER.debug("Initialized from config.");
        } catch (MissingResourceException e) {
            LOGGER.info("No config found.");
        }
    }

    /**
	 * @return
	 */
    public Map<String, Object> getEnvironmentProperties() {
        if (environmentProperties == null) {
            environmentProperties = new HashMap<String, Object>();
        }
        return environmentProperties;
    }

    /**
	 * @param environmentProperties
	 */
    public void setEnvironmentProperties(Map<String, Object> environmentProperties) {
        this.environmentProperties = environmentProperties;
    }

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
	 * @return Returns the procInputCharNormalizer.
	 * @since 11.07.2008
	 */
    public CharacterNormalizer getProcInputCharNormalizer() {
        return this.procInputCharNormalizer;
    }

    /**
	 * @param procInputCharNormalizer
	 *            The procInputCharNormalizer to set.
	 * @since 11.07.2008
	 */
    public void setProcInputCharNormalizer(CharacterNormalizer procInputCharNormalizer) {
        this.procInputCharNormalizer = procInputCharNormalizer;
    }

    /**
	 * @return Returns the procOutputCharNormalizer.
	 * @since 11.07.2008
	 */
    public CharacterNormalizer getProcOutputCharNormalizer() {
        return this.procOutputCharNormalizer;
    }

    /**
	 * @param procOutputCharNormalizer
	 *            The procOutputCharNormalizer to set.
	 * @since 11.07.2008
	 */
    public void setProcOutputCharNormalizer(CharacterNormalizer procOutputCharNormalizer) {
        this.procOutputCharNormalizer = procOutputCharNormalizer;
    }

    /**
	 * @return Returns the processInEncoding.
	 * @since 30.08.2005
	 */
    public String getProcessInEncoding() {
        return this.processInEncoding;
    }

    /**
	 * @return Returns the processOutEncoding.
	 * @since 30.08.2005
	 */
    public String getProcessOutEncoding() {
        return this.processOutEncoding;
    }

    /**
	 * @throws IOException
	 * @throws ConverterException
	 * @see de.ddb.conversion.GenericConverter#convert(java.io.InputStream,
	 *      java.io.OutputStream, ConversionParameters)
	 * @since 16.08.2005
	 */
    public void convert(InputStream in, OutputStream out, ConversionParameters params) throws ConverterException {
        Reader reader;
        Writer writer;
        Charset processInCharset;
        Charset processOutCharset;
        if ((getProcessInEncoding() == null && getProcessOutEncoding() == null) || (params.getSourceCharset().equalsIgnoreCase(getProcessInEncoding()) && params.getTargetCharset().equalsIgnoreCase(getProcessOutEncoding()))) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Converting raw input.");
            }
            remoteExec(in, out);
        } else {
            CharacterNormalizer _inputNormalizer = this.procInputCharNormalizer;
            CharacterNormalizer _outputNormalizer = this.procOutputCharNormalizer;
            if (getProcessInEncoding() != null && !getProcessInEncoding().equals(params.getSourceCharset())) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Setting input reader to charset [" + params.getSourceCharset() + "].");
                    LOGGER.debug("Setting process in to charset [" + getProcessInEncoding() + "].");
                }
                processInCharset = CharsetUtil.forName(getProcessInEncoding());
                reader = new InputStreamReader(in, CharsetUtil.forName(params.getSourceCharset()));
                _inputNormalizer = null;
            } else {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Setting input reader to charset [" + IDENTITY_CHARSET.displayName() + "].");
                    LOGGER.debug("Setting process in to charset [" + IDENTITY_CHARSET.displayName() + "].");
                }
                processInCharset = IDENTITY_CHARSET;
                reader = new InputStreamReader(in, IDENTITY_CHARSET);
                _inputNormalizer = null;
            }
            if (getProcessOutEncoding() != null && !getProcessOutEncoding().equals(params.getTargetCharset())) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Setting output writer to charset [" + params.getTargetCharset() + "].");
                    LOGGER.debug("Setting process out to charset [" + getProcessOutEncoding() + "].");
                }
                processOutCharset = CharsetUtil.forName(getProcessOutEncoding());
                writer = new OutputStreamWriter(out, CharsetUtil.forName(params.getTargetCharset()));
                _outputNormalizer = null;
            } else {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Setting output writer to charset [" + IDENTITY_CHARSET.displayName() + "].");
                    LOGGER.debug("Setting process out to charset [" + IDENTITY_CHARSET.displayName() + "].");
                }
                processOutCharset = IDENTITY_CHARSET;
                writer = new OutputStreamWriter(out, IDENTITY_CHARSET);
            }
            try {
                remoteExec(reader, writer, processInCharset, processOutCharset, _inputNormalizer, _outputNormalizer);
            } catch (IOException e) {
                throw new ConverterException(e);
            } catch (JSchException e) {
                throw new ConverterException(e);
            }
        }
    }

    private void remoteExec(Reader in, Writer out, Charset processInCharset, Charset processOutCharset, CharacterNormalizer inputNormalizer, CharacterNormalizer outputNormalizer) throws IOException, JSchException, ConverterException {
        getRemoteExec().remoteExec(in, out, processInCharset, processOutCharset, inputNormalizer, outputNormalizer);
    }

    private void remoteExec(InputStream in, OutputStream out) throws ConverterException {
        try {
            getRemoteExec().remoteExec(in, out);
        } catch (JSchException e) {
            throw new ConverterException(e);
        } catch (IOException e) {
            throw new ConverterException(e);
        }
    }

    /**
	 * @see de.ddb.conversion.GenericConverter#destroy()
	 * @since 22.08.2005
	 */
    @Override
    public void destroy() throws ConverterException, IOException {
        getRemoteExec().destroy();
        this.remoteExec = null;
        this.command = null;
        this.environmentProperties = null;
    }

    /**
	 * @return
	 */
    public String getCommand() {
        return command;
    }

    /**
	 * @param command
	 */
    public void setCommand(String command) {
        this.command = command;
    }

    /**
	 * @param processInEncoding
	 */
    public void setProcessInEncoding(String processInEncoding) {
        this.processInEncoding = processInEncoding;
    }

    /**
	 * @param processOutEncoding
	 */
    public void setProcessOutEncoding(String processOutEncoding) {
        this.processOutEncoding = processOutEncoding;
    }

    private RemoteExec getRemoteExec() {
        if (this.remoteExec == null) {
            this.remoteExec = new RemoteExec();
            this.remoteExec.setCommand(this.command);
            this.remoteExec.setEnvironmentProperties(getEnvironmentProperties());
            this.remoteExec.setHost(getConverterContext().getProperties().getProperty(BinaryConverter.PROPKEY_HOST));
            this.remoteExec.setUser(getConverterContext().getProperties().getProperty(BinaryConverter.PROPKEY_USER));
            this.remoteExec.setPassword(getConverterContext().getProperties().getProperty(BinaryConverter.PROPKEY_PASSWORD));
            this.remoteExec.setPort(this.port);
        }
        return this.remoteExec;
    }
}
