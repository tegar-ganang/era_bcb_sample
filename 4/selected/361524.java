package org.openthinclient.tftp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.SocketAddress;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.directory.shared.ldap.util.Base64;
import org.apache.log4j.Logger;
import org.openthinclient.common.directory.LDAPDirectory;
import org.openthinclient.common.model.Client;
import org.openthinclient.common.model.Realm;
import org.openthinclient.common.model.schema.provider.SchemaLoadingException;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.ldap.Filter;
import org.openthinclient.ldap.LDAPConnectionDescriptor;
import org.openthinclient.ldap.TypeMapping;
import org.openthinclient.ldap.auth.UsernamePasswordHandler;
import org.openthinclient.tftp.tftpd.TFTPProvider;

/**
 * 
 * @author grafvr
 * 
 */
public class PXEConfigTFTProvider implements TFTPProvider {

    private static final Logger logger = Logger.getLogger(PXEConfigTFTProvider.class);

    private Set<Realm> realms;

    private URL templateURL;

    private final String DEFAULT_CLIENT_MAC = "00:00:00:00:00:00";

    public PXEConfigTFTProvider() throws DirectoryException {
        init();
    }

    /**
	 * @throws DirectoryException
	 */
    private void init() throws DirectoryException {
        final LDAPConnectionDescriptor lcd = new LDAPConnectionDescriptor();
        lcd.setProviderType(LDAPConnectionDescriptor.ProviderType.SUN);
        lcd.setAuthenticationMethod(LDAPConnectionDescriptor.AuthenticationMethod.SIMPLE);
        lcd.setCallbackHandler(new UsernamePasswordHandler("uid=admin,ou=system", System.getProperty("ContextSecurityCredentials", "secret").toCharArray()));
        try {
            realms = LDAPDirectory.findAllRealms(lcd);
            logger.info("----------------realms----------------");
            for (final Realm realm : realms) try {
                realm.getSchema(realm);
                logger.info("Serving realm " + realm);
            } catch (final SchemaLoadingException e) {
                logger.fatal("Can't serve realm " + realm, e);
            }
        } catch (final DirectoryException e) {
            logger.fatal("Can't init directory", e);
            throw e;
        }
    }

    public void setOptions(Map<String, String> options) {
        if (!options.containsKey("template")) throw new IllegalArgumentException("Need the 'template' option");
        try {
            this.templateURL = new URL(options.get("template"));
        } catch (final MalformedURLException e) {
            try {
                this.templateURL = new File(options.get("template")).toURL();
            } catch (final MalformedURLException f) {
                throw new IllegalArgumentException("template' option must contain a valid URL", f);
            }
        }
    }

    public long getLength(SocketAddress peer, SocketAddress local, String arg0, String arg1) throws IOException {
        return -1;
    }

    public InputStream getStream(SocketAddress peer, SocketAddress local, String prefix, String fileName) throws IOException {
        logger.info("Got request for " + fileName);
        if (fileName.contains("/") || fileName.length() != 20) throw new FileNotFoundException("Don't know what to make of this file name: " + fileName);
        final String hwAddress = fileName.substring(3).replaceAll("-", ":");
        logger.info("MAC is " + fileName);
        try {
            final Client client = findClient(hwAddress);
            if (client != null) {
                logger.info("Serving Client " + client);
                final String file = streamAsString(templateURL.openStream());
                if (logger.isDebugEnabled()) logger.debug("Template: " + file);
                final Map<String, String> globalVariables = new HashMap<String, String>();
                globalVariables.put("myip", ((InetSocketAddress) local).getAddress().getHostAddress());
                globalVariables.put("basedn", client.getRealm().getConnectionDescriptor().getBaseDN());
                String processed = resolveVariables(file, client, globalVariables);
                if (logger.isDebugEnabled()) logger.debug("Processed template: >>>>\n" + processed + "<<<<\n");
                processed = processed.replaceAll("\\r", "");
                processed = processed.replaceAll("\\\\[\\t ]*\\n", "");
                processed = processed.replaceAll("[\\t ]+", " ");
                if (logger.isDebugEnabled()) logger.debug("Template after cleanup: >>>>\n" + processed + "<<<<\n");
                return new ByteArrayInputStream(processed.getBytes("ASCII"));
            }
        } catch (final Exception e) {
            logger.error("Can't query for client for PXE service", e);
            new FileNotFoundException("Can't query for client for PXE service: " + e);
        }
        throw new FileNotFoundException("Client " + fileName + " not Found");
    }

    private static final Pattern TEMPLATE_REPLACEMENT_PATTERN = Pattern.compile("\\$\\{([^\\}]+)\\}");

    /**
	 * @param template
	 * @param client
	 * @param globalVariables
	 * @return
	 */
    private String resolveVariables(String template, Client client, Map<String, String> globalVariables) {
        final StringBuffer result = new StringBuffer();
        final Matcher m = TEMPLATE_REPLACEMENT_PATTERN.matcher(template);
        while (m.find()) {
            String variable = m.group(1);
            String encoding = "";
            if (variable.contains(":")) {
                encoding = variable.substring(0, variable.indexOf(":"));
                variable = variable.substring(variable.indexOf(":") + 1);
            }
            String value = client.getValue(variable);
            if (null == value) {
                value = globalVariables.get(variable);
                if (null == value) logger.warn("Pattern refers to undefined variable " + variable);
            }
            if (null != value) value = resolveVariables(value, client, globalVariables); else value = "";
            try {
                if (encoding.equalsIgnoreCase("base64")) value = new String(Base64.encode(value.getBytes("UTF-8"))); else if (encoding.equalsIgnoreCase("urlencoded")) value = URLEncoder.encode(value, "UTF-8").replaceAll("\\+", "%20"); else if (encoding.length() > 0) logger.warn("Ignoring unsupported encoding: " + encoding);
            } catch (final UnsupportedEncodingException e) {
                logger.error("That's silly: UTF8-encoding is unsupported!");
            }
            m.appendReplacement(result, value);
        }
        m.appendTail(result);
        final String processed = result.toString();
        return processed;
    }

    /**
	 * @param hwAddress
	 * @return
	 * @throws DirectoryException
	 * @throws SchemaLoadingException
	 */
    private Client findClient(String hwAddress) throws DirectoryException, SchemaLoadingException {
        Client client = null;
        for (final Realm realm : realms) {
            Set<Client> found = realm.getDirectory().list(Client.class, new Filter("(&(macAddress={0})(l=*))", hwAddress), TypeMapping.SearchScope.SUBTREE);
            if (found.size() > 0) {
                if (found.size() > 1) logger.warn("Found more than one client for hardware address " + hwAddress);
                client = found.iterator().next();
                client.initSchemas(realm);
                return client;
            } else if (found.size() == 0) {
                final String pxeServicePolicy = realm.getValue("BootOptions.PXEServicePolicy");
                if ("AnyClient".equals(pxeServicePolicy)) {
                    found = realm.getDirectory().list(Client.class, new Filter("(&(macAddress={0})(l=*))", DEFAULT_CLIENT_MAC), TypeMapping.SearchScope.SUBTREE);
                    if (found.size() > 0) {
                        if (found.size() > 1) logger.warn("Found more than one client for default hardware address " + DEFAULT_CLIENT_MAC);
                        client = found.iterator().next();
                        client.initSchemas(realm);
                        return client;
                    }
                }
            }
        }
        return null;
    }

    private String streamAsString(InputStream is) throws IOException {
        final ByteArrayOutputStream s = new ByteArrayOutputStream();
        final byte b[] = new byte[1024];
        int read;
        while ((read = is.read(b)) >= 0) s.write(b, 0, read);
        is.close();
        return s.toString("ASCII");
    }
}
