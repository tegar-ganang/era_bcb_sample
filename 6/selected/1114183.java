package com.mepping.snmpjaag.admin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.net.ProtocolCommandEvent;
import org.apache.commons.net.ProtocolCommandListener;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.opennms.protocols.snmp.SnmpOctetString;
import com.mepping.snmpjaag.MgmtInfoBase;
import com.mepping.snmpjaag.SnmpJavaAgent;
import com.mepping.snmpjaag.bootstrap.Bootstrap;

public class AdminFileDownload extends SnmpOctetString implements AdminObject {

    private static final long serialVersionUID = 1L;

    private SnmpJavaAgent snmpJavaAgent;

    public AdminFileDownload() {
        super("AdminFileDownload".getBytes());
    }

    public void setSnmpJavaAgent(SnmpJavaAgent snmpJavaAgent) {
        this.snmpJavaAgent = snmpJavaAgent;
    }

    public void setString(byte[] data) {
        setString(new String(data));
    }

    public void setString(String data) {
        int i = data.indexOf('@');
        if (-1 == i) {
            throw new IllegalArgumentException("Malformed value \"" + data + "\": correct format is <path>@<url>");
        }
        String path = data.substring(0, i);
        String url = data.substring(i + 1);
        System.out.println("[" + path + "]@[" + url + "]");
        final File file = new File(Bootstrap.SNMPJAAG_HOME, path);
        try {
            if (!file.getCanonicalPath().startsWith(Bootstrap.SNMPJAAG_HOME)) {
                throw new IllegalArgumentException("Invalid path \"" + path + "\"");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid path \"" + path + "\"");
        }
        if (file.exists()) {
        }
        final URL jurl;
        try {
            jurl = new URL(url);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid URL \"" + url + "\"");
        }
        new Thread() {

            public void run() {
                try {
                    if (jurl.getProtocol().toLowerCase().equals("http")) {
                        downloadHttp(file, jurl);
                    } else if (jurl.getProtocol().toLowerCase().equals("ftp")) {
                        downloadFtp(file, jurl);
                    }
                    snmpJavaAgent.sendTrap(MgmtInfoBase.fileDownloadCompleted);
                } catch (Exception e) {
                    e.printStackTrace();
                    snmpJavaAgent.sendTrap(MgmtInfoBase.fileDownloadError);
                }
            }
        }.start();
    }

    private void downloadHttp(File file, URL jurl) throws HttpException, IOException {
        System.out.println("downloadHttp(" + file + ", " + jurl + ")");
        HttpClient client = new HttpClient();
        client.getHttpConnectionManager().getParams().setConnectionTimeout(10000);
        HttpMethod method = new GetMethod(jurl.toString());
        method.setFollowRedirects(true);
        try {
            client.executeMethod(method);
            InputStream in = method.getResponseBodyAsStream();
            FileOutputStream out = new FileOutputStream(file);
            byte[] buf = new byte[256];
            int count;
            int length = 0;
            while (-1 != (count = in.read(buf))) {
                out.write(buf, 0, count);
                length += count;
            }
            System.out.println("downloadHttp: downloaded file size: " + length);
            out.close();
            in.close();
        } finally {
            method.releaseConnection();
        }
    }

    private void downloadFtp(File file, URL jurl) throws SocketException, IOException {
        System.out.println("downloadFtp(" + file + ", " + jurl + ")");
        FTPClient client = new FTPClient();
        client.addProtocolCommandListener(new ProtocolCommandListener() {

            public void protocolCommandSent(ProtocolCommandEvent event) {
                System.out.println("downloadFtp: " + event.getMessage());
            }

            public void protocolReplyReceived(ProtocolCommandEvent event) {
                System.out.println("downloadFtp: " + event.getMessage());
            }
        });
        try {
            client.connect(jurl.getHost(), -1 == jurl.getPort() ? FTP.DEFAULT_PORT : jurl.getPort());
            int reply = client.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                client.disconnect();
                throw new IOException("FTP server refused connection.");
            }
            if (!client.login("anonymous", "anonymous")) {
                client.logout();
                throw new IOException("Authentication failure.");
            }
            client.setFileType(FTP.BINARY_FILE_TYPE);
            client.enterLocalPassiveMode();
            FileOutputStream out = new FileOutputStream(file);
            boolean ok = client.retrieveFile(jurl.getPath(), out);
            out.close();
            client.logout();
            if (!ok) {
                throw new IOException("File transfer failure.");
            }
        } catch (IOException e) {
            throw e;
        } finally {
            if (client.isConnected()) {
                try {
                    client.disconnect();
                } catch (IOException e) {
                }
            }
        }
    }
}
