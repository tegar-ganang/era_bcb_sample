package net.sf.cclearly.conn.reflector;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.cclearly.entities.MachineID;
import net.sf.cclearly.entities.User;
import net.sf.cclearly.util.MD5;
import za.dats.net.TransportableObject;

public class Reflector {

    private static final String MSGTYPE_REGISTER = "REGISTER";

    private static final String MSGTYPE_DEREGISTER = "DEREGISTER";

    private static final String MSGTYPE_CONTACTLIST = "CONTACTLIST";

    private static final String MSGTYPE_GETMESSAGE = "GETMESSAGE";

    private static final String MSGTYPE_MESSAGERECEIVED = "MESSAGERECEIVED";

    private static final String MSGTYPE_SENDMESSAGE = "SENDMESSAGE";

    private static final String MSGTYPE_TEST = "TESTURL";

    private static Logger logger = Logger.getLogger(Reflector.class.getName());

    private static final String ERRCODE_RECIPIENT_INVALID = "RECIPIENT_INVALID";

    private static final Pattern ERROR_CODE_PATTERN = Pattern.compile("[(]([^)]*)[)]");

    public enum SendStatus {

        OK, ERROR, RECIPIENT_INVALID
    }

    private String host;

    private String path;

    private int port = 80;

    private String password;

    private String proxyHost;

    private int proxyPort;

    private String fullURL;

    public Reflector() {
        initialize();
    }

    public String getHost() {
        return host;
    }

    public String getPassword() {
        return password;
    }

    public int getPort() {
        return port;
    }

    public void setHost(String host) {
        path = getHostPath(host);
        this.host = validateHost(host);
        fullURL = host;
    }

    private String validateHost(String host) {
        host = host.toLowerCase();
        if (host.startsWith("http://")) {
            host = host.substring(7);
        }
        if (host.contains("/")) {
            host = host.substring(0, host.indexOf("/"));
        }
        return host;
    }

    private String getHostPath(String host) {
        if (host.toLowerCase().startsWith("http://")) {
            host = host.substring(8);
        }
        if (host.contains("/")) {
            host = host.substring(host.indexOf("/"));
            if (host.length() == 0) {
                return "/index.php";
            }
        } else {
            return "/index.php";
        }
        return host;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setProxy(String proxyHost, int proxyPort) {
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        Properties systemSettings = System.getProperties();
        systemSettings.put("http.proxyHost", proxyHost);
        systemSettings.put("http.proxyPort", proxyPort);
        System.setProperties(systemSettings);
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public String getFullURL() {
        return fullURL;
    }

    /**
     * 
     * @param machine
     * @param name
     * @param email
     * @return true upon successful connection and registration, false
     *         otherwise.
     */
    public boolean connect(MachineID machine, String name, String email) throws ReflectorException {
        if ((name == null) || (name.length() == 0)) {
            return false;
        }
        PostMethod post = getPostMethod(MSGTYPE_REGISTER, machine);
        post.setParameter("name", name);
        post.setParameter("email", email);
        return executePostAndProcess(post);
    }

    /**
     * 
     * @param post
     * @return true if success, false if not.
     */
    private boolean executePostAndProcess(PostMethod post) throws ReflectorException {
        if (post == null) {
            return false;
        }
        try {
            if (!post.execute()) {
                post.flush();
                logger.log(Level.WARNING, "Invalid response: HTTP Status of " + post.getStatusCode());
                return false;
            }
            String result = post.getResponseBodyAsString();
            String[] strings = result.split(":");
            if (strings.length == 0) {
                logger.log(Level.INFO, "Invalid response from reflector");
                throw new ReflectorException("Invalid response from reflector");
            }
            if ("OK".equals(strings[0])) {
                return true;
            } else {
                logger.info("Error from reflector: " + strings[1]);
                throw new ReflectorException(strings[1]);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            post.releaseConnection();
        }
    }

    private PostMethod getPostMethod(String messageType, MachineID machine) {
        if (host == null) {
            return null;
        }
        try {
            URL uri = new URL("http", host, port, path);
            PostMethod post = new PostMethod(uri);
            post.setParameter("pwd", password);
            post.setParameter("messagetype", messageType);
            post.setParameter("machineid", machine.toString());
            return post;
        } catch (MalformedURLException e) {
            return null;
        }
    }

    public boolean deregister(MachineID machine) throws ReflectorException {
        PostMethod post = getPostMethod(MSGTYPE_DEREGISTER, machine);
        return executePostAndProcess(post);
    }

    public List<User> getContactList(MachineID machine) throws ReflectorException {
        PostMethod post = getPostMethod(MSGTYPE_CONTACTLIST, machine);
        try {
            if (!post.execute()) {
                post.flush();
                logger.log(Level.WARNING, "Invalid response: HTTP Status of " + post.getStatusCode());
                throw new ReflectorException("Could not get contacts - HTTP status: " + post.getStatusCode());
            }
            List<User> result = new LinkedList<User>();
            Scanner scanner = new Scanner(post.getResponseBodyAsString());
            READ_SCANNER: while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                StringTokenizer tokenizer = new StringTokenizer(line, ",");
                int elmCount = 0;
                User user = new User();
                while (tokenizer.hasMoreTokens()) {
                    String element = tokenizer.nextToken();
                    if (element.charAt(0) == '"') {
                        element = element.substring(1, element.length() - 1).replace("\\\"", "\"");
                    }
                    switch(elmCount) {
                        case 0:
                            {
                                try {
                                    user.setMachineID(new MachineID(element));
                                } catch (Exception e) {
                                    continue READ_SCANNER;
                                }
                                break;
                            }
                        case 1:
                            user.setName(element);
                            break;
                        case 2:
                            user.setEmail(element);
                            break;
                    }
                    elmCount++;
                }
                if (!machine.equals(user.getMachineID())) {
                    result.add(user);
                }
            }
            return result;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            post.releaseConnection();
        }
    }

    public SendStatus sendMessage(MachineID senderMachine, MachineID recipientMachine, TransportableObject message) {
        ReflectorMessage msg = new ReflectorMessage(senderMachine, message);
        byte[] msgData;
        try {
            msgData = msg.getByteMessage();
        } catch (IOException e) {
            return SendStatus.ERROR;
        }
        String md5 = MD5.md5(msgData);
        if (md5 == null) {
            return SendStatus.ERROR;
        }
        PostMethod post = getPostMethod(MSGTYPE_SENDMESSAGE, senderMachine);
        post.addPart("pwd", password);
        post.addPart("messagetype", MSGTYPE_SENDMESSAGE);
        post.addPart("machineid", senderMachine.toString());
        post.addPart("to", recipientMachine.toString());
        post.addPart("message-md5", md5);
        post.addFilePart("message", msgData);
        try {
            if (!post.execute()) {
                post.flush();
                logger.log(Level.WARNING, "Invalid response: HTTP Status of " + post.getStatusCode());
                return SendStatus.ERROR;
            }
            String result = post.getResponseBodyAsString();
            String[] strings = result.split(":");
            if (strings.length == 0) {
                logger.log(Level.INFO, "Invalid response from reflector");
                return SendStatus.ERROR;
            }
            if ("OK".equals(strings[0])) {
                return SendStatus.OK;
            } else {
                Matcher matcher = ERROR_CODE_PATTERN.matcher(strings[1]);
                if (matcher.find()) {
                    String errorCode = matcher.group(1);
                    if (ERRCODE_RECIPIENT_INVALID.equals(errorCode)) {
                        return SendStatus.RECIPIENT_INVALID;
                    } else {
                        logger.info("Error from reflector: " + strings[1]);
                        return SendStatus.ERROR;
                    }
                } else {
                    logger.info("Error from reflector: " + strings[1]);
                    return SendStatus.ERROR;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return SendStatus.ERROR;
        } finally {
            post.releaseConnection();
        }
    }

    /**
     * 
     * @param recipientMachine
     * @return null if no message is on the reflector queue.
     * @throws ReflectorException
     */
    public ReflectorMessage getMessage(MachineID recipientMachine, ReflectorMessageListener listener) throws ReflectorException {
        if (listener == null) {
            listener = new ReflectorMessageAdapter();
        }
        PostMethod post = getPostMethod(MSGTYPE_GETMESSAGE, recipientMachine);
        try {
            listener.executing();
            if (!post.execute()) {
                post.flush();
                listener.failed("Invalid response from server");
                logger.log(Level.WARNING, "Invalid response: HTTP Status of " + post.execute());
                throw new ReflectorException("Could not get message - " + post.getStatusCode());
            }
            String filename = post.getResponseHeader("cclearly-file");
            if (filename == null) {
                listener.noMessage();
                return null;
            }
            String sizeHeader = post.getResponseHeader("cclearly-size");
            int size = 0;
            if (sizeHeader != null) {
                size = Integer.valueOf(sizeHeader);
            }
            String responseMd5 = post.getResponseHeader("message-md5");
            if (responseMd5 == null) {
                throw new ReflectorException("No md5 checksum header found from reflector.");
            }
            if ((filename == null) || (filename.length() == 0)) {
                return null;
            }
            listener.startReceiving(filename, size);
            ReflectorMessage result = new ReflectorMessage();
            InputStream responseBodyAsStream = post.getResponseBodyAsStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int readSize = 0;
            int position = 0;
            byte[] data = new byte[4096];
            while ((readSize = responseBodyAsStream.read(data)) != -1) {
                out.write(data, 0, readSize);
                position += readSize;
                listener.progress(filename, position, size);
            }
            listener.finishedReceiving(filename);
            String md5 = MD5.md5(out.toByteArray());
            if (md5 == null) {
                throw new ReflectorException("Could not md5 message");
            }
            if (!md5.equals(responseMd5)) {
                throw new ReflectorException("Checksums from reflector does not match: Download - " + md5 + ", Header - " + responseMd5);
            }
            result.setByteMessage(new DataInputStream(new ByteArrayInputStream(out.toByteArray())));
            PostMethod reply = getPostMethod(MSGTYPE_MESSAGERECEIVED, recipientMachine);
            reply.setParameter("filename", filename);
            listener.startRemoving(filename);
            executePostAndProcess(reply);
            listener.finishedRemoving(filename);
            return result;
        } catch (IOException e) {
            listener.failed("IO Problem: " + e.getMessage());
            throw new ReflectorException(e);
        } catch (IllegalStateException e) {
            listener.failed("Illegal State: " + e.getMessage());
            throw new ReflectorException(e);
        } finally {
            post.releaseConnection();
        }
    }

    private synchronized void initialize() {
    }

    public void testSettings(MachineID machine) throws ReflectorException {
        if (host.length() == 0) {
            return;
        }
        PostMethod post = getPostMethod(MSGTYPE_TEST, machine);
        if (post == null) {
            return;
        }
        if (!post.execute()) {
            throw new ReflectorException("Could not connect to reflector");
        }
        String result;
        try {
            result = post.getResponseBodyAsString();
        } catch (IOException e) {
            throw new ReflectorException(e.getMessage());
        }
        String[] strings = result.split(":");
        if (strings.length == 0) {
            throw new ReflectorException("Unknown response from reflector.");
        }
        if ("OK".equals(strings[0])) {
            return;
        } else {
            throw new ReflectorException(strings[1]);
        }
    }
}
