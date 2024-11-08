package com.ericdaugherty.mail;

import java.io.*;
import java.net.*;
import java.security.KeyStore;
import java.util.*;
import javax.net.ssl.*;
import com.sun.mail.pop3.POP3Message;
import com.sun.mail.smtp.SMTPMessage;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.util.SharedFileInputStream;
import junit.framework.TestCase;
import com.ericdaugherty.mail.server.utils.FileUtils;

/**
 *
 * @author Andreas Kyrmegalos
 */
public class JESBaseCase3 extends TestCase {

    final Object lock = new Object();

    public JESBaseCase3(String testName) {
        super(testName);
    }

    public void execute(final List<Request> tasks, String testCase, final String server, final Properties rcptInfo, final Properties configurationProperties, final PasswordAuthenticator senderCredentials) throws Exception {
        domainAndPort = server + ":41002";
        String baseDir = System.getProperty("basedir");
        int count = 0;
        File tempJESDirTemp = null;
        do {
            tempJESDirTemp = new File(System.getProperty("java.io.tmpdir"), "jes" + count);
            if (!tempJESDirTemp.exists()) {
                tempJESDirTemp.mkdir();
                break;
            }
            count++;
            if (count == 1000) {
                System.exit(1000);
            }
        } while (true);
        final File tempJESDir = tempJESDirTemp;
        File testJESDir = new File(baseDir, "target" + File.separator + "test-classes" + File.separator + testCase);
        Utils.copyFiles(testJESDir, tempJESDir);
        File lib = new File(tempJESDir, "lib");
        lib.mkdir();
        File forTest = new File(baseDir, "forTest");
        forTest.mkdir();
        String[] surefirePathElements = System.getProperty("surefire.test.class.path").split(File.pathSeparator);
        File aFile;
        for (int i = 0; i < surefirePathElements.length; i++) {
            aFile = new File(surefirePathElements[i]);
            if (surefirePathElements[i].contains("commons-codec") || surefirePathElements[i].contains("commons-logging") || surefirePathElements[i].contains("dnsjava") || surefirePathElements[i].contains("log4j") || surefirePathElements[i].contains("javax.mail") || surefirePathElements[i].contains("pop3") || surefirePathElements[i].contains("smtp") || surefirePathElements[i].contains("activation")) {
                FileUtils.copyFile(aFile, new File(forTest, aFile.getName()));
            }
        }
        final List<File> userDirectories = new ArrayList((int) (tasks.size() / .75));
        for (Request request : tasks) {
            File userTempDir = new File(tempJESDir, request.getUsername());
            userDirectories.add(userTempDir);
            if (!userTempDir.exists()) {
                userTempDir.mkdir();
            }
        }
        Utils.copyFiles(forTest, lib);
        File testJESFile = new File(baseDir, "pom.xml");
        BufferedReader br = null;
        String line = null, name = null, version = null, pckg = null;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(testJESFile), System.getProperty("file.encoding")));
            count = 0;
            do {
                line = br.readLine().trim();
                if (line.startsWith("<version>")) {
                    version = line.substring(line.indexOf(">") + 1, line.lastIndexOf("<"));
                } else if (line.startsWith("<name>")) {
                    name = line.substring(line.indexOf(">") + 1, line.lastIndexOf("<"));
                } else if (line.startsWith("<packaging>")) {
                    pckg = line.substring(line.indexOf(">") + 1, line.lastIndexOf("<"));
                }
                if (version != null && name != null && pckg != null) {
                    break;
                } else if (count == 30) {
                    throw new IOException();
                }
                count++;
            } while (true);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ioe) {
                }
            }
        }
        testJESFile = new File(baseDir, "target" + File.separator + name + "-" + version + "." + pckg);
        FileUtils.copyFile(testJESFile, new File(tempJESDir, testJESFile.getName()));
        final String smtpHost = server;
        final String smtpPort = "17025";
        final String pop3Host = server;
        final String pop3Port = "17110";
        final Properties properties = System.getProperties();
        properties.setProperty("mail.smtp.host", smtpHost);
        properties.setProperty("mail.smtp.port", smtpPort);
        properties.setProperty("mail.smtp.localaddress", server);
        properties.setProperty("mail.pop3.host", pop3Host);
        properties.setProperty("mail.pop3.port", pop3Port);
        properties.setProperty("mail.pop3.localaddress", server);
        if (configurationProperties.getProperty("SASL") != null) {
            properties.setProperty("mail.smtp.auth", "true");
            properties.setProperty("mail.smtp.auth.mechanisms", configurationProperties.getProperty("SASL"));
            if (configurationProperties.getProperty("REALM") != null) {
                properties.setProperty("mail.smtp.sasl.realm", configurationProperties.getProperty("REALM"));
            }
        }
        if (configurationProperties.getProperty("STARTTLS") != null) {
            properties.setProperty("mail.smtp.starttls.enable", "true");
            properties.setProperty("mail.smtp.ssl.protocols", configurationProperties.getProperty("PROTOCOL"));
            properties.setProperty("mail.smtp.ssl.ciphersuites", configurationProperties.getProperty("CIPHERS"));
            properties.setProperty("mail.pop3.starttls.enable", "true");
            properties.setProperty("mail.pop3.ssl.protocols", configurationProperties.getProperty("PROTOCOL"));
            properties.setProperty("mail.pop3.ssl.ciphersuites", configurationProperties.getProperty("CIPHERS"));
            SSLContext sslContext = SSLContext.getInstance("TLS");
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(null, null);
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX");
            File truststore = new File(tempJESDir, "truststore.jks");
            if (truststore.exists()) {
                KeyStore ks = KeyStore.getInstance("JKS", "SUN");
                FileInputStream fis = new FileInputStream(truststore);
                ks.load(fis, null);
                tmf.init(ks);
                fis.close();
                fis = null;
            } else {
                tmf.init((KeyStore) null);
            }
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            SSLSocketFactory sslsf = sslContext.getSocketFactory();
            properties.put("mail.smtp.ssl.socketFactory", sslsf);
            properties.put("mail.pop3.ssl.socketFactory", sslsf);
        }
        System.setProperty("java.security.policy", tempJESDir.getPath() + File.separator + "jes.policy");
        Properties log4jProperties = new Properties();
        log4jProperties.setProperty("defaultthreshold", "info");
        log4jProperties.setProperty("log4j.appender.stdout", "org.apache.log4j.ConsoleAppender");
        log4jProperties.setProperty("log4j.appender.stdout.threshold", "info");
        log4jProperties.setProperty("log4j.appender.stdout.layout", "org.apache.log4j.PatternLayout");
        log4jProperties.setProperty("log4j.appender.stdout.layout.ConversionPattern", "%d{ISO8601} - [%t] %C{1} - %m%n");
        log4jProperties.setProperty("log4j.appender.file", "org.apache.log4j.RollingFileAppender");
        log4jProperties.setProperty("log4j.appender.file.File", tempJESDir.getPath() + File.separator + "logs" + File.separator + "jes.log");
        log4jProperties.setProperty("log4j.appender.file.MaxFileSize", "10000KB");
        log4jProperties.setProperty("log4j.appender.file.MaxBackupIndex", "20");
        log4jProperties.setProperty("log4j.appender.file.threshold", "debug");
        log4jProperties.setProperty("log4j.appender.file.layout", "org.apache.log4j.PatternLayout");
        log4jProperties.setProperty("log4j.appender.file.layout.ConversionPattern", "%d{ISO8601} - [%t] %C{1} - %m%n");
        log4jProperties.setProperty("log4j.rootLogger", "debug,stdout,file");
        org.apache.log4j.PropertyConfigurator.configure(log4jProperties);
        com.ericdaugherty.mail.server.Mail.main(new String[] { tempJESDir.getPath(), "" });
        addDomain(server);
        addRealm(1, "users");
        List<String> realms = new ArrayList(1);
        realms.add("users");
        String username = senderCredentials.getPasswordAuthentication().getUserName();
        if (username.indexOf('@') != -1) {
            username = username.substring(0, username.indexOf('@'));
        }
        addUser(username, senderCredentials.getPasswordAuthentication().getPassword().toCharArray(), 1, realms);
        Iterator iterator = rcptInfo.keySet().iterator();
        String user;
        while (iterator.hasNext()) {
            user = (String) iterator.next();
            addUser(user, rcptInfo.getProperty(user).toCharArray(), 1, realms);
        }
        try {
            Thread.sleep(2 * 1000);
        } catch (InterruptedException ex) {
        }
        final Map<String, List<String>> userMessages = new HashMap(6, 0.75f);
        userMessages.put("ernest", new ArrayList(tasks.size() / 4));
        userMessages.put("kara", new ArrayList(tasks.size() / 4));
        userMessages.put("perl", new ArrayList(tasks.size() / 4));
        userMessages.put("wizard", new ArrayList(tasks.size() / 4));
        final List<byte[]> hashes = new ArrayList(tasks.size());
        final List<byte[]> pop3Hashes = new ArrayList(tasks.size());
        Thread thread = new Thread(new Runnable() {

            @SuppressWarnings("CallToThreadDumpStack")
            public void run() {
                InputStream is = null;
                OutputStream os = null;
                Store store = null;
                try {
                    while (tasks.size() > 0) {
                        final Request request = tasks.remove(0);
                        System.out.println("Checking out " + request.getUsername() + "'s message");
                        String messageFilename = request.getMessage().getName();
                        File userDir = new File(tempJESDir, request.getUsername());
                        Session session = Session.getInstance(properties, senderCredentials);
                        is = new SharedFileInputStream(request.getMessage());
                        SMTPMessage messageSMTP = new SMTPMessage(session, is);
                        messageSMTP.setFrom(new InternetAddress(senderCredentials.getEmailAddress()));
                        messageSMTP.setRecipient(Message.RecipientType.TO, new InternetAddress(request.getUsername() + '@' + server));
                        Transport.send(messageSMTP);
                        File output = new File(userDir, "generatedSMTP" + messageFilename);
                        try {
                            os = new PrintStream(output, System.getProperty("file.encoding"));
                            messageSMTP.writeTo(os);
                            os.write(new byte[] { 0x2E, 0x0D, 0x0A });
                        } finally {
                            os.close();
                        }
                        is.close();
                        new PseudoSMTPProcessor().process(output.getPath(), server);
                        Tools.createMD5File(new File(userDir, "generatedSMTP" + messageFilename + ".converted").getPath());
                        File userDirJES = new File(tempJESDir, "users" + File.separator + request.getUsername() + '@' + server);
                        FilenameFilter ff = new FilenameFilter() {

                            public boolean accept(File directory, String filename) {
                                if (filename.toLowerCase().endsWith(".loc")) {
                                    return userMessages.get(request.getUsername()).add(filename);
                                }
                                return false;
                            }
                        };
                        File[] results;
                        int count = 0;
                        while ((results = userDirJES.listFiles(ff)).length == 0) {
                            count++;
                            if (count == 7) {
                                throw new Exception();
                            }
                            System.out.println("mail not yet received, sleeping...");
                            try {
                                Thread.sleep(5 * 1000);
                            } catch (InterruptedException ex) {
                            }
                        }
                        byte[] derivedMD5 = Utils.getDerivedMD5(results[0]);
                        byte[] originalMD5 = Utils.getOriginalMD5(new File(userDir, "generatedSMTP" + messageFilename + ".converted" + ".md5"));
                        boolean assertionCandidate = Arrays.equals(originalMD5, derivedMD5);
                        assertTrue(assertionCandidate);
                        System.out.println(toHex(derivedMD5, 16));
                        System.out.println(toHex(originalMD5, 16));
                        synchronized (hashes) {
                            hashes.add(derivedMD5);
                        }
                        session = Session.getInstance(properties, new PasswordAuthenticator(request.getUsername() + '@' + server, rcptInfo.getProperty(request.getUsername())));
                        store = session.getStore("pop3");
                        store.connect();
                        Folder folder = store.getFolder("inbox");
                        folder.open(Folder.READ_WRITE);
                        Message[] messages;
                        count = 0;
                        do {
                            messages = folder.getMessages();
                            if (messages.length == 0) {
                                count++;
                                if (count == 7) {
                                    throw new Exception();
                                }
                                System.out.println("Going to sleep again");
                                try {
                                    Thread.sleep(5 * 1000);
                                } catch (InterruptedException ex) {
                                }
                            } else {
                                break;
                            }
                        } while (true);
                        POP3Message messagePOP3 = (POP3Message) messages[0];
                        os = new PrintStream(new File(userDir, "generatedPOP3" + messageFilename), System.getProperty("file.encoding"));
                        messagePOP3.writeTo(os);
                        os.close();
                        messagePOP3.setFlag(Flags.Flag.DELETED, true);
                        folder.close(true);
                        store.close();
                        Tools.createMD5File(new File(userDir, "generatedPOP3" + messageFilename).getPath());
                        originalMD5 = Utils.getOriginalMD5(new File(userDir, "generatedPOP3" + messageFilename + ".md5"));
                        System.out.println(Arrays.equals(originalMD5, derivedMD5));
                        System.out.println(toHex(derivedMD5, 16));
                        System.out.println(toHex(originalMD5, 16));
                        synchronized (pop3Hashes) {
                            pop3Hashes.add(originalMD5);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    fail(e.getLocalizedMessage());
                } finally {
                    if (is != null) {
                        try {
                            is.close();
                        } catch (IOException ex) {
                        }
                    }
                    if (os != null) {
                        try {
                            os.close();
                        } catch (IOException ex) {
                        }
                    }
                    if (store != null) {
                        try {
                            store.close();
                        } catch (MessagingException ex) {
                        }
                    }
                }
                synchronized (lock) {
                    lock.notify();
                }
            }
        });
        thread.start();
        synchronized (lock) {
            try {
                lock.wait();
            } catch (InterruptedException ex) {
                System.out.println(ex.getMessage());
            }
        }
        try {
            Thread.sleep(2 * 1000);
        } catch (InterruptedException ex) {
        }
        System.setSecurityManager(null);
        com.ericdaugherty.mail.server.Mail.getInstance().shutdown();
        try {
            Thread.sleep(5 * 1000);
        } catch (InterruptedException ex) {
        }
        Iterator<byte[]> iter = pop3Hashes.iterator();
        byte[] result;
        Iterator<byte[]> iter2;
        while (iter.hasNext()) {
            result = iter.next();
            iter2 = hashes.iterator();
            while (iter2.hasNext()) {
                if (Arrays.equals(result, iter2.next())) ;
                iter2.remove();
            }
        }
        assertEquals(hashes.size(), 0);
        org.apache.log4j.Logger.getRootLogger().getAppender("file").close();
        org.apache.log4j.LogManager.shutdown();
        Utils.deleteFiles(tempJESDir);
        tempJESDir.delete();
        Utils.deleteFiles(forTest);
        forTest.delete();
    }

    public static final String DOMAIN = "domain:";

    public static final String DOMAIN_ID = "domainId:";

    public static final String USER_ID = "userId:";

    public static final String USERNAME = "username:";

    public static final String PASSWORD = "password:";

    public static final String REALM = "realm:";

    public static final String COMMAND_INSERT_DOMAIN = "insertDomain";

    public void addDomain(String domain) {
        List<String> lineCommands = new ArrayList(4);
        lineCommands.add(COMMAND_INSERT_DOMAIN);
        lineCommands.add(DOMAIN + domain);
        lineCommands.add(".");
        transmitData(lineCommands);
    }

    public static final String COMMAND_INSERT_USER = "insertUser";

    public void addUser(String username, char[] password, int domainId, List<String> realmsToAdd) {
        List<String> lineCommands = new ArrayList(8);
        lineCommands.add(COMMAND_INSERT_USER);
        lineCommands.add(USERNAME + username);
        lineCommands.add(PASSWORD + new String(password));
        lineCommands.add(DOMAIN_ID + domainId);
        if (!realmsToAdd.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String realm : realmsToAdd) {
                sb.append(realm).append(':');
            }
            sb.deleteCharAt(sb.length() - 1);
            lineCommands.add(REALM + sb.toString());
        }
        lineCommands.add(".");
        transmitData(lineCommands);
    }

    public static final String COMMAND_INSERT_REALM = "insertRealm";

    public void addRealm(int domainId, String realmName) {
        List<String> lineCommands = new ArrayList(6);
        lineCommands.add(COMMAND_INSERT_REALM);
        lineCommands.add(DOMAIN_ID + domainId);
        lineCommands.add(REALM + realmName);
        lineCommands.add(".");
        transmitData(lineCommands);
    }

    public static final String COMMAND_ADD_USER_TO_REALM = "addUserToRealm";

    public void addUserToRealms(String username, int userId, int domainId, char[] password, String[] realms) {
        List<String> lineCommands = new ArrayList(8);
        lineCommands.add(COMMAND_ADD_USER_TO_REALM);
        lineCommands.add(USERNAME + username);
        lineCommands.add(USER_ID + userId);
        lineCommands.add(DOMAIN_ID + domainId);
        lineCommands.add(PASSWORD + new String(password));
        StringBuilder sb = new StringBuilder();
        for (String realm : realms) {
            sb.append(realm).append(',');
        }
        sb.deleteCharAt(sb.length() - 1);
        lineCommands.add(REALM + sb.toString());
        lineCommands.add(".");
        transmitData(lineCommands);
    }

    private static final String CRLF_STRING = "\r\n";

    private static final String US_ASCII = "US-ASCII";

    private String domainAndPort;

    /** Writer to sent data to the client */
    private PrintWriter out;

    /** Socket connection to the client */
    private Socket socket;

    private void Waitforconnection() throws SocketException {
        int count = 0;
        while (!socket.isConnected()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {
                throw new SocketException("Unable to complete the transaction due to internal error.");
            }
            if (count++ > 120) {
                throw new SocketException("No connection established. Please try again later.");
            }
        }
        socket.setSoTimeout(5 * 60 * 1000);
    }

    /**
    * Writes the specified output message to the client.
    */
    private void write(String message) {
        if (message != null) {
            out.print(message + CRLF_STRING);
            out.flush();
        }
    }

    private void transmitData(List<String> lineCommands) {
        try {
            String domain = domainAndPort.substring(0, domainAndPort.indexOf(':'));
            if (domain.equalsIgnoreCase("localhost") || domain.equals("127.0.0.1")) {
                domain = null;
            }
            final String finalDomain = domain;
            socket = new Socket(InetAddress.getByName(finalDomain), Integer.valueOf(domainAndPort.substring(domainAndPort.indexOf(':') + 1)));
            out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), US_ASCII)));
            Waitforconnection();
            for (String lineCommand : lineCommands) {
                write(lineCommand);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                out.close();
                out = null;
            }
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ex) {
                }
            }
        }
    }

    private String toHex(byte[] b, int len) {
        if (b == null) {
            return "";
        }
        StringBuilder s = new StringBuilder("");
        int i;
        for (i = 0; i < len; i++) {
            s.append(toHex(b[i]));
        }
        return s.toString();
    }

    private String toHex(byte b) {
        Integer I = new Integer((((int) b) << 24) >>> 24);
        int i = I.intValue();
        if (i < (byte) 16) {
            return "0" + Integer.toString(i, 16);
        } else {
            return Integer.toString(i, 16);
        }
    }
}
