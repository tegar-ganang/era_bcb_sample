package net.assimilator.tools.webster.http;

import net.assimilator.tools.webster.WebsterImpl;
import java.net.Socket;
import java.util.Properties;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.*;

/**
 * class for processing a put file request
 *
 * @author Jim Clark
 * @author Mike T. Miller
 * @author Larry J. Mitchell
 * @version $Id$
 */
public class PutFile implements Runnable {

    /**
     * the component name for websterInstance
     */
    static final String COMPONENT = "net.assimilator.tools.websterInstance";

    /**
     * the logger we use to tell people what is happening
     */
    private static final Logger logger = Logger.getLogger(COMPONENT);

    private Socket client;

    private String fileName;

    private Properties rheader;

    private WebsterImpl websterInstance;

    public PutFile(WebsterImpl webster, Socket s, String name, Properties hdr) {
        this.websterInstance = webster;
        rheader = hdr;
        client = s;
        this.fileName = name;
    }

    public void run() {
        try {
            File putFile = websterInstance.parseFileName(fileName);
            String header;
            if (putFile.exists()) {
                header = "HTTP/1.0 200 OK\n" + "Allow: PUT\n" + "MIME-Version: 1.0\n" + "Server : Webster: a Java HTTP Server \n" + "\n\n <H1>200 File updated</H1>\n";
            } else {
                header = "HTTP/1.0 201 Created\n" + "Allow: PUT\n" + "MIME-Version: 1.0\n" + "Server : Webster: a Java HTTP Server \n" + "\n\n <H1>201 File Created</H1>\n";
            }
            FileOutputStream requestedFile = new FileOutputStream(putFile);
            InputStream in = client.getInputStream();
            int length = Integer.parseInt(ignoreCaseProperty(rheader, "Content-Length"));
            try {
                for (int i = 0; i < length; i++) {
                    requestedFile.write(in.read());
                }
            } catch (IOException e) {
                header = "HTTP/1.0 500 Internal Server Error\n" + "Allow: PUT\n" + "MIME-Version: 1.0\n" + "Server : Webster: a Java HTTP Server \n" + "\n\n <H1>500 Internal Server Error</H1>\n" + e;
            }
            DataOutputStream clientStream = new DataOutputStream(new BufferedOutputStream(client.getOutputStream()));
            clientStream.writeBytes(header);
            clientStream.flush();
            requestedFile.close();
            clientStream.close();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Closing Socket", e);
        } finally {
            try {
                client.close();
            } catch (IOException e2) {
                logger.log(Level.WARNING, "Closing incoming socket", e2);
            }
        }
    }

    public String ignoreCaseProperty(Properties props, String field) {
        Enumeration names = props.propertyNames();
        while (names.hasMoreElements()) {
            String propName = (String) names.nextElement();
            if (field.equalsIgnoreCase(propName)) {
                return (props.getProperty(propName));
            }
        }
        return (null);
    }
}
