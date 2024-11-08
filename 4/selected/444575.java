package org.ludo.vcalx;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Security;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import javax.mail.BodyPart;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;

/**
 * Class used to extract the calendar entries from an Exchange server
 * and write them to an .ics file.<br>
 * The idea of getting the calendar entries comes originally from a
 * perl script I found, that is located at:<br>
 * <a href="http://rasterweb.net/raster/200409.html#09102004110000">
 * http://rasterweb.net/raster/200409.html#09102004110000</a>
 * <br><br>
 * Thanks to Pete for this great idea!
 * 
 * @author <a href="mailto:masterludo@gmx.net">Ludovic Kim-Xuan Galibert</a>
 * @version $Id: VCalendarExtractor.java,v 1.3 2005/10/03 18:12:21 masterludo Exp $
 * @created 29.10.2004, 13:33:46
 */
public class VCalendarExtractor {

    /** Holds the Logger */
    private static Logger LOG = Logger.getLogger("VCalendarExtractor");

    /** Holds the name of the properties file */
    private static final String PROPERTIES_FILE = "vcalx.properties";

    /** Holds the key for the "host" property */
    private static final String KEY_HOST = "host";

    /** Holds the key for the "port" property */
    private static final String KEY_PORT = "port";

    /** Holds the key for the "ssl" property */
    private static final String KEY_SSL = "ssl";

    /** Holds the key for the "keystore" property */
    private static final String KEY_KEYSTORE = "keystore";

    /** Holds the key for the "login" property */
    private static final String KEY_LOGIN = "login";

    /** Holds the key for the "password" property */
    private static final String KEY_PASSWORD = "password";

    /** Holds the key for the "calendar box" property */
    private static final String KEY_CALENDAR_BOX = "calendar.box";

    /** Holds the key for the "output" property */
    private static final String KEY_OUTPUT_FILE = "output";

    /** Holds the key for the "debug" property */
    private static final String KEY_DEBUG = "debug";

    /** Holds the default name of the output file */
    private static final String DEFAULT_OUTPUT_FILE = "ical.ics";

    /** Holds the name for the IMAP protocol */
    private static final String IMAP_PROTOCOL = "imap";

    /** Holds the default port for IMAP connections without SSL */
    private static final String DEFAULT_NON_SSL_PORT = "143";

    /** Holds the default port for IMAP connections with SSL */
    private static final String DEFAULT_SSL_PORT = "993";

    /** Holds the mime type "text/calendar" */
    private static final String MIME_TEXT_CALENDAR = "text/calendar";

    /** Holds the mime type "multiplart/alternative" */
    private static final String MIME_MULTIPLART_ALTERNATIVE = "multipart/alternative";

    /** Holds the mime type "multiplart/mixed" */
    private static final String MIME_MULTIPLART_MIXED = "multipart/mixed";

    /** Holds the mime type "text/plain" */
    private static final String MIME_TEXT_PLAIN = "text/plain";

    /**
	 * Extracts the exchange calendar entries to the file.
	 * @param someProperties  the properties read from the properties file,
	 * must not be <code>null</code>.
	 * @return  true if the extract was successful.
	 */
    public boolean extract(Properties someProperties) {
        boolean useSsl = Boolean.valueOf(someProperties.getProperty(KEY_SSL)).booleanValue();
        String host = someProperties.getProperty(KEY_HOST);
        String sPort = someProperties.getProperty(KEY_PORT);
        if (null == sPort) {
            if (useSsl) {
                sPort = DEFAULT_SSL_PORT;
            } else {
                sPort = DEFAULT_NON_SSL_PORT;
            }
        }
        int iPort = Integer.parseInt(sPort);
        String login = someProperties.getProperty(KEY_LOGIN);
        String mailbox = someProperties.getProperty(KEY_CALENDAR_BOX);
        String outputFile = someProperties.getProperty(KEY_OUTPUT_FILE);
        if (null == outputFile) {
            outputFile = DEFAULT_OUTPUT_FILE;
        }
        Properties props = System.getProperties();
        if (useSsl) {
            String keystore = someProperties.getProperty(KEY_KEYSTORE);
            if (null != keystore) {
                File ksf = new File(keystore);
                if (ksf.exists()) {
                    props.setProperty("javax.net.ssl.trustStore", keystore);
                }
            }
            Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
            props.setProperty("java.protocol.handler.pkgs", "com.sun.net.ssl.internal.www.protocol");
            props.setProperty("mail.imap.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.setProperty("mail.imap.socketFactory.fallback", "false");
            props.setProperty("mail.imap.socketFactory.port", sPort);
        }
        props.put("mail.imap.class", "com.sun.mail.imap.IMAPStore");
        Session session = Session.getDefaultInstance(props, null);
        session.setDebug(false);
        Store store = null;
        boolean connected = false;
        try {
            store = session.getStore(IMAP_PROTOCOL);
            System.out.println("Connecting...");
            LOG.info("Connecting...");
            store.connect(host, iPort, login, someProperties.getProperty(KEY_PASSWORD));
            someProperties.setProperty(KEY_PASSWORD, "");
            connected = true;
        } catch (NoSuchProviderException nspe) {
            nspe.printStackTrace();
        } catch (MessagingException me) {
            me.printStackTrace();
        }
        boolean retVal = false;
        if (connected) {
            System.out.println("Connected, opening mailbox...");
            LOG.info("Connected, opening mailbox...");
            Folder folder = openMailbox(store, mailbox);
            boolean calendarRead = false;
            if (null != folder) {
                try {
                    int count = folder.getMessageCount();
                    System.out.println("Extracting " + count + " messages...");
                    LOG.info("Extracting " + count + " messages...");
                    FileOutputStream fos = new FileOutputStream(new File(outputFile));
                    Message[] messages = folder.getMessages();
                    for (int i = 0; i < count; i++) {
                        System.out.print("*");
                        LOG.info("*");
                        String calPart = getCalendarPart(messages[i]);
                        if (null != calPart) {
                            fos.write(calPart.getBytes());
                        }
                    }
                    fos.flush();
                    fos.close();
                    System.out.println("\nMessages read!");
                    LOG.info("\nMessages read!");
                    calendarRead = true;
                } catch (MessagingException me) {
                    me.printStackTrace();
                } catch (FileNotFoundException fnfe) {
                    fnfe.printStackTrace();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            } else {
                System.out.println("The mailbox could not be opened");
                LOG.warning("The mailbox could not be opened");
            }
            try {
                store.close();
                retVal = calendarRead;
            } catch (MessagingException me) {
                me.printStackTrace();
            }
        } else {
            System.out.println("Connection failed!");
            LOG.warning("Connection failed!");
        }
        return retVal;
    }

    /**
	 * Opens the mailbox with the given name.
	 * @param aStore  a store.
	 * @param aMailbox  the mailbox to open.
	 * @return  the successfuly opened folder, can be <code>null</code>.
	 */
    public Folder openMailbox(Store aStore, String aMailbox) {
        Folder retVal = null;
        try {
            Folder defaultFolder = aStore.getDefaultFolder();
            if (defaultFolder != null) {
                retVal = defaultFolder.getFolder(aMailbox);
                if (retVal != null) {
                    retVal.open(Folder.READ_ONLY);
                } else {
                    System.out.println("No mailbox '" + aMailbox + "' could be found!");
                    LOG.warning("No mailbox '" + aMailbox + "' could be found!");
                }
            } else {
                System.out.println("No default mailbox found!");
                LOG.warning("No default mailbox found!");
            }
        } catch (MessagingException me) {
            System.out.println("Mailbox '" + aMailbox + "' could not be opened!");
            LOG.warning("Mailbox '" + aMailbox + "' could not be opened!");
            me.printStackTrace();
        }
        return retVal;
    }

    /**
	 * Gets the calendar part of the given message as string.
	 * @param aMessage  a message.
	 * @return  the calendar part or <code>null</code> if no calendar part was found.
	 */
    public String getCalendarPart(Message aMessage) {
        Object content = null;
        String retVal = null;
        try {
            LOG.info("\nTrying to get calendar part from Message with content type: " + aMessage.getContentType());
            if (aMessage.getContentType().toLowerCase().startsWith(MIME_MULTIPLART_ALTERNATIVE)) {
                if (aMessage.getContent() instanceof Multipart) {
                    content = extractCalendarPartFromMultipart((Multipart) aMessage.getContent());
                } else {
                    LOG.info("Content class of Message is no MultiPart: " + (null != aMessage.getContent() ? aMessage.getContent().getClass().getName() : null));
                }
            } else if (aMessage.getContentType().toLowerCase().startsWith(MIME_MULTIPLART_MIXED)) {
                if (aMessage.getContent() instanceof Multipart) {
                    LOG.info("Content of the Message is multipart");
                    Multipart multipart = (Multipart) aMessage.getContent();
                    int count = multipart.getCount();
                    LOG.info("Multipart count: " + count);
                    for (int i = 0; i < count; i++) {
                        BodyPart body = multipart.getBodyPart(i);
                        LOG.info("Content-type of the BodyPart: " + body.getContentType());
                        if (body.getContentType().toLowerCase().startsWith(MIME_TEXT_CALENDAR)) {
                            LOG.info("Content class of the BodyPart: " + (null != body.getContent() ? body.getContent().getClass().getName() : null));
                            if (body.getContent() instanceof InputStream) {
                                InputStream is = (InputStream) body.getContent();
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                byte[] buffer = new byte[1024 * 4];
                                int readBytes;
                                while ((readBytes = is.read(buffer)) >= 0) {
                                    baos.write(buffer, 0, readBytes);
                                }
                                baos.flush();
                                content = baos.toString();
                                baos.close();
                            } else {
                                content = body.getContent();
                            }
                            break;
                        } else if (body.getContentType().toLowerCase().startsWith(MIME_MULTIPLART_ALTERNATIVE)) {
                            if (body.getContent() instanceof Multipart) {
                                content = extractCalendarPartFromMultipart((Multipart) body.getContent());
                            } else {
                                LOG.info("Content class of Message is no MultiPart: " + (null != aMessage.getContent() ? aMessage.getContent().getClass().getName() : null));
                            }
                        }
                    }
                } else {
                    LOG.info("Content class of Message is no MultiPart: " + (null != aMessage.getContent() ? aMessage.getContent().getClass().getName() : null));
                }
            } else if (aMessage.getContentType().toLowerCase().startsWith(MIME_TEXT_PLAIN)) {
                if (aMessage.getContent() instanceof String) {
                    content = (String) aMessage.getContent();
                } else {
                    LOG.warning("Content of Message is no string: " + (null != aMessage.getContent() ? aMessage.getContent().getClass().getName() : null));
                }
            } else {
                LOG.warning("Unsupported content type: " + aMessage.getContentType());
            }
            if (content instanceof String) {
                LOG.info("Final content class is a string");
                retVal = (String) content;
            } else {
                LOG.info("Final content class is: " + (null != content ? content.getClass().getName() : null));
            }
        } catch (MessagingException me) {
            me.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return retVal;
    }

    /**
	 * Extracts the calendar part from a multipart.
	 * @param aMultipart
	 * @return  the calendar part or <code>null</code> if no calendar part was found.
	 */
    private String extractCalendarPartFromMultipart(Multipart aMultipart) {
        assert null != aMultipart;
        String retVal = null;
        try {
            Object content = null;
            int count = aMultipart.getCount();
            LOG.info("Multipart count: " + count);
            for (int i = 0; i < count; i++) {
                BodyPart body = aMultipart.getBodyPart(i);
                LOG.info("Content-type of the BodyPart: " + body.getContentType());
                if (body.getContentType().toLowerCase().startsWith(MIME_TEXT_CALENDAR)) {
                    LOG.info("Content class of the BodyPart: " + (null != body.getContent() ? body.getContent().getClass().getName() : null));
                    if (body.getContent() instanceof InputStream) {
                        InputStream is = (InputStream) body.getContent();
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        byte[] buffer = new byte[1024 * 4];
                        int readBytes;
                        while ((readBytes = is.read(buffer)) >= 0) {
                            baos.write(buffer, 0, readBytes);
                        }
                        baos.flush();
                        content = baos.toString();
                        baos.close();
                    } else {
                        content = body.getContent();
                    }
                    break;
                }
            }
            if (content instanceof String) {
                LOG.info("Final content class is a string");
                retVal = (String) content;
            } else {
                LOG.info("Final content class is: " + (null != content ? content.getClass().getName() : null));
            }
        } catch (MessagingException me) {
            me.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return retVal;
    }

    /**
	 * Runs the extractor.
	 * @param args  if no argument is passed, the password will be read from the properties
	 * file, otherwise it should be passed as first argument. Further arguments will not be read.
	 */
    public static void main(String[] args) {
        try {
            Properties props = new Properties();
            props.load(new FileInputStream(PROPERTIES_FILE));
            boolean debugEnabled = false;
            if (props.containsKey(KEY_DEBUG)) {
                debugEnabled = Boolean.valueOf(props.getProperty(KEY_DEBUG)).booleanValue();
            }
            Level level;
            if (debugEnabled) {
                FileHandler fh = new FileHandler("vcalx.log");
                fh.setFormatter(new SimpleFormatter());
                LOG.addHandler(fh);
                level = Level.ALL;
            } else {
                level = Level.OFF;
            }
            LOG.setUseParentHandlers(false);
            LOG.setLevel(level);
            VCalendarExtractor extractor = new VCalendarExtractor();
            boolean extracted = false;
            boolean passFound = true;
            if (!props.containsKey(KEY_PASSWORD)) {
                if (args.length > 0) {
                    props.setProperty(KEY_PASSWORD, args[0]);
                } else {
                    passFound = false;
                    System.out.println("You must supply a password!");
                }
            }
            if (passFound) {
                extracted = extractor.extract(props);
            }
            if (extracted) {
                System.out.println("The Exchange calendar was successfuly extracted!");
            } else {
                System.out.println("The Exchange calendar coult not be extracted!");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
