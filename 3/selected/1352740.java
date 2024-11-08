package net.sourceforge.guacamole.net.basic;

import java.io.BufferedReader;
import net.sourceforge.guacamole.net.auth.AuthenticationProvider;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.net.auth.UsernamePassword;
import net.sourceforge.guacamole.properties.FileGuacamoleProperty;
import net.sourceforge.guacamole.properties.GuacamoleProperties;
import net.sourceforge.guacamole.protocol.GuacamoleConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * Authenticates users against a static list of username/password pairs.
 * Each username/password may be associated with exactly one configuration.
 * This list is stored in an XML file which is reread if modified.
 * 
 * @author Michael Jumper
 */
public class BasicFileAuthenticationProvider implements AuthenticationProvider<UsernamePassword> {

    private Logger logger = LoggerFactory.getLogger(BasicFileAuthenticationProvider.class);

    private long mappingTime;

    private Map<String, AuthInfo> mapping;

    /**
     * The filename of the XML file to read the user mapping from.
     */
    public static final FileGuacamoleProperty BASIC_USER_MAPPING = new FileGuacamoleProperty() {

        @Override
        public String getName() {
            return "basic-user-mapping";
        }
    };

    private File getUserMappingFile() throws GuacamoleException {
        return GuacamoleProperties.getProperty(BASIC_USER_MAPPING);
    }

    public synchronized void init() throws GuacamoleException {
        File mapFile = getUserMappingFile();
        if (mapFile == null) throw new GuacamoleException("Missing \"basic-user-mapping\" parameter required for basic login.");
        logger.info("Reading user mapping file: {}", mapFile);
        try {
            BasicUserMappingContentHandler contentHandler = new BasicUserMappingContentHandler();
            XMLReader parser = XMLReaderFactory.createXMLReader();
            parser.setContentHandler(contentHandler);
            Reader reader = new BufferedReader(new FileReader(mapFile));
            parser.parse(new InputSource(reader));
            reader.close();
            mappingTime = mapFile.lastModified();
            mapping = contentHandler.getUserMapping();
        } catch (IOException e) {
            throw new GuacamoleException("Error reading basic user mapping file.", e);
        } catch (SAXException e) {
            throw new GuacamoleException("Error parsing basic user mapping XML.", e);
        }
    }

    @Override
    public Map<String, GuacamoleConfiguration> getAuthorizedConfigurations(UsernamePassword credentials) throws GuacamoleException {
        File userMappingFile = getUserMappingFile();
        if (userMappingFile.exists() && mappingTime < userMappingFile.lastModified()) {
            synchronized (this) {
                if (userMappingFile.exists() && mappingTime < userMappingFile.lastModified()) {
                    logger.info("User mapping file {} has been modified.", userMappingFile);
                    init();
                }
            }
        }
        if (mapping == null) throw new GuacamoleException("User mapping could not be read.");
        AuthInfo info = mapping.get(credentials.getUsername());
        if (info != null && info.validate(credentials.getUsername(), credentials.getPassword())) {
            Map<String, GuacamoleConfiguration> configs = new HashMap<String, GuacamoleConfiguration>();
            configs.put("DEFAULT", info.getConfiguration());
            return configs;
        }
        return null;
    }

    public static class AuthInfo {

        public static enum Encoding {

            PLAIN_TEXT, MD5
        }

        private String auth_username;

        private String auth_password;

        private Encoding auth_encoding;

        private GuacamoleConfiguration config;

        public AuthInfo(String auth_username, String auth_password, Encoding auth_encoding) {
            this.auth_username = auth_username;
            this.auth_password = auth_password;
            this.auth_encoding = auth_encoding;
            config = new GuacamoleConfiguration();
        }

        private static final char HEX_CHARS[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

        public static String getHexString(byte[] bytes) {
            if (bytes == null) return null;
            StringBuilder hex = new StringBuilder(2 * bytes.length);
            for (byte b : bytes) {
                hex.append(HEX_CHARS[(b & 0xF0) >> 4]).append(HEX_CHARS[(b & 0x0F)]);
            }
            return hex.toString();
        }

        public boolean validate(String username, String password) {
            if (username != null && password != null && username.equals(auth_username)) {
                switch(auth_encoding) {
                    case PLAIN_TEXT:
                        return password.equals(auth_password);
                    case MD5:
                        try {
                            MessageDigest digest = MessageDigest.getInstance("MD5");
                            String hashedPassword = getHexString(digest.digest(password.getBytes()));
                            return hashedPassword.equals(auth_password.toUpperCase());
                        } catch (NoSuchAlgorithmException e) {
                            throw new UnsupportedOperationException("Unexpected lack of MD5 support.", e);
                        }
                }
            }
            return false;
        }

        public GuacamoleConfiguration getConfiguration() {
            return config;
        }
    }

    private static class BasicUserMappingContentHandler extends DefaultHandler {

        private Map<String, AuthInfo> authMapping = new HashMap<String, AuthInfo>();

        public Map<String, AuthInfo> getUserMapping() {
            return Collections.unmodifiableMap(authMapping);
        }

        private enum State {

            ROOT, USER_MAPPING, AUTH_INFO, PROTOCOL, PARAMETER, END
        }

        private State state = State.ROOT;

        private AuthInfo current = null;

        private String currentParameter = null;

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            switch(state) {
                case USER_MAPPING:
                    if (localName.equals("user-mapping")) {
                        state = State.END;
                        return;
                    }
                    break;
                case AUTH_INFO:
                    if (localName.equals("authorize")) {
                        authMapping.put(current.auth_username, current);
                        state = State.USER_MAPPING;
                        return;
                    }
                    break;
                case PROTOCOL:
                    if (localName.equals("protocol")) {
                        state = State.AUTH_INFO;
                        return;
                    }
                    break;
                case PARAMETER:
                    if (localName.equals("param")) {
                        state = State.AUTH_INFO;
                        return;
                    }
                    break;
            }
            throw new SAXException("Tag not yet complete: " + localName);
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            switch(state) {
                case ROOT:
                    if (localName.equals("user-mapping")) {
                        state = State.USER_MAPPING;
                        return;
                    }
                    break;
                case USER_MAPPING:
                    if (localName.equals("authorize")) {
                        AuthInfo.Encoding encoding;
                        String encodingString = attributes.getValue("encoding");
                        if (encodingString == null) encoding = AuthInfo.Encoding.PLAIN_TEXT; else if (encodingString.equals("plain")) encoding = AuthInfo.Encoding.PLAIN_TEXT; else if (encodingString.equals("md5")) encoding = AuthInfo.Encoding.MD5; else throw new SAXException("Invalid encoding type");
                        current = new AuthInfo(attributes.getValue("username"), attributes.getValue("password"), encoding);
                        state = State.AUTH_INFO;
                        return;
                    }
                    break;
                case AUTH_INFO:
                    if (localName.equals("protocol")) {
                        state = State.PROTOCOL;
                        return;
                    }
                    if (localName.equals("param")) {
                        currentParameter = attributes.getValue("name");
                        if (currentParameter == null) throw new SAXException("Attribute \"name\" required for param tag.");
                        state = State.PARAMETER;
                        return;
                    }
                    break;
            }
            throw new SAXException("Unexpected tag: " + localName);
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            String str = new String(ch, start, length);
            switch(state) {
                case PROTOCOL:
                    current.getConfiguration().setProtocol(str);
                    return;
                case PARAMETER:
                    current.getConfiguration().setParameter(currentParameter, str);
                    return;
            }
            if (str.trim().length() != 0) throw new SAXException("Unexpected character data.");
        }
    }
}
