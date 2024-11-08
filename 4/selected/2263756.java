package com.dokumentarchiv.smtp;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Properties;
import java.util.StringTokenizer;
import javax.mail.MessagingException;
import javax.mail.Session;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.dokumentarchiv.EEMProxy;
import com.dokumentarchiv.ProxyWorker;
import com.dokumentarchiv.filter.FilterGroup;
import de.inovox.AdvancedMimeMessage;

/**
 * Worker implementation that handles SMTP communication
 * 
 * @author Carsten Burghardt
 * @version $Id: SMTPProxyWorker.java 613 2008-03-12 21:40:35Z carsten $
 */
public class SMTPProxyWorker extends ProxyWorker {

    private static Log log = LogFactory.getLog(SMTPProxyWorker.class);

    /**
     * @param sock
     * @param server
     * @param port
     * @param filter
     * @param encoding
     * @param config
     */
    public SMTPProxyWorker(Socket sock, InetAddress server, int port, FilterGroup filter, String encoding, Configuration config) {
        super(sock, server, port, filter, encoding, config);
    }

    /**
     * @param importDir
     * @param filter
     * @param config
     */
    public SMTPProxyWorker(File importDir, FilterGroup filter, Configuration config) {
        super(importDir, filter, config);
    }

    /**
     * Handle the requests
     */
    public void run() {
        if (client != null) {
            processSocket();
        } else {
            log.error("No valid params given");
        }
    }

    /**
     * Processes data of the client socket
     */
    protected void processSocket() {
        BufferedReader cis = null;
        Writer cos = null;
        BufferedReader sis = null;
        Writer sos = null;
        try {
            cis = new BufferedReader(new InputStreamReader(client.getInputStream(), encoding));
            cos = new PrintWriter(client.getOutputStream(), true);
        } catch (IOException io) {
            log.error("Failed to create client streams", io);
            close();
            return;
        }
        Socket out = null;
        try {
            out = new Socket(targetServer, targetPort);
            sis = new BufferedReader(new InputStreamReader(out.getInputStream(), encoding));
            sos = new PrintWriter(out.getOutputStream(), true);
            readReply(sis, cos);
        } catch (IOException io) {
            log.error("Failed to create server streams", io);
            try {
                cos.write("421 " + EEMProxy.getHelo() + ". SMTP Proxy temporary unavailable." + CRLF);
                cos.close();
            } catch (IOException ignore) {
            } finally {
                close();
            }
            return;
        }
        String req;
        while (true) {
            try {
                req = cis.readLine();
                if (req == null) break;
                StringTokenizer st = new StringTokenizer(req);
                if (!st.hasMoreTokens()) {
                    send("500 Syntax error - No command entered.", cos);
                    continue;
                }
                String com = st.nextToken().toUpperCase();
                if (com.equals("QUIT")) {
                    send("QUIT", sos);
                    sos.close();
                    send("221 " + EEMProxy.getHelo() + ". SMTP Proxy connection closed.", cos);
                    close();
                    return;
                }
                if (com.equals("HELO") || com.equals("EHLO")) {
                    send(com + " " + EEMProxy.getHelo(), sos);
                    readReply(sis, cos);
                    continue;
                }
                if (com.equals("DATA")) {
                    send(req, sos);
                    String tmp = sis.readLine();
                    if (tmp == null) throw new EOFException();
                    send(tmp, cos);
                    if (tmp.length() > 3) {
                        if (tmp.charAt(0) == '4' || tmp.charAt(0) == '5') {
                            continue;
                        }
                    }
                    StringBuffer buffer = new StringBuffer();
                    String line;
                    while ((line = cis.readLine()) != null) {
                        sos.write(line);
                        sos.write(CRLF);
                        if (line.equals(".")) {
                            break;
                        }
                        buffer.append(line + CRLF);
                    }
                    sos.flush();
                    readReply(sis, cos);
                    byte[] bytes = buffer.toString().getBytes();
                    ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
                    Session session = Session.getDefaultInstance(new Properties());
                    AdvancedMimeMessage message = null;
                    try {
                        message = new AdvancedMimeMessage(session, stream);
                    } catch (MessagingException e) {
                        log.error("Failed to create Message", e);
                        continue;
                    }
                    processMessage(message);
                    continue;
                }
                if (com.equals("STARTTLS")) {
                    send("500 STARTTLS is not implemented.", cos);
                    continue;
                }
                send(req, sos);
                readReply(sis, cos);
            } catch (IOException io) {
                log.error("IO Error", io);
                try {
                    out.close();
                } catch (IOException ignore) {
                }
                close();
                return;
            }
        }
    }

    /**
     * Read the answer from the target server
     * @param di
     * @param d
     * @throws IOException
     */
    protected void readReply(BufferedReader di, Writer writer) throws IOException {
        String line;
        while (true) {
            line = di.readLine();
            if (line == null) throw new EOFException();
            writer.write(line);
            writer.write(CRLF);
            if (line.length() <= 3 || line.charAt(3) != '-') {
                break;
            }
        }
        writer.flush();
    }
}
