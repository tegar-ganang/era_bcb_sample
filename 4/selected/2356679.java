package jrdesktop.server.http;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import jrdesktop.main;
import jrdesktop.rmi.server.RMIServer;
import jrdesktop.utilities.FileUtility;

public class HttpConnection extends Thread {

    private Socket client;

    public HttpConnection(Socket socket) {
        client = socket;
    }

    /**
     * http connection engine
     */
    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            DataOutputStream out = new DataOutputStream(client.getOutputStream());
            String line;
            while ((line = in.readLine()) != null) {
                if (line.length() == 0) break;
                if (line.startsWith("GET / ")) {
                    out.writeBytes("HTTP/1.0 200 \n");
                    out.writeBytes("Content-Type: text/html\n");
                    out.writeBytes("\n");
                    out.flush();
                    InputStream is = main.class.getResourceAsStream("server/http/index.html");
                    BufferedInputStream bis = new BufferedInputStream(is);
                    int ch;
                    while ((ch = bis.read()) != -1) out.write(ch);
                    out.writeBytes("\n");
                    out.flush();
                } else if (line.startsWith("GET /jrdesktop.html")) {
                    out.writeBytes("HTTP/1.0 200 \n");
                    out.writeBytes("Content-Type: text/html\n");
                    out.writeBytes("\n");
                    out.flush();
                    out.writeBytes("<html><head><title>jrdesktop - Java Remote Desktop</title></head>" + "<body style='margin:0; padding: 0;'><link href='http://" + RMIServer.serverConfig.server_address + ":" + RMIServer.serverConfig.http_port + "/idle.png' " + "rel='icon' type='image/png'/>" + "<applet code='jrdesktop.mainApplet.class' archive='http://" + RMIServer.serverConfig.server_address + ":" + RMIServer.serverConfig.http_port + "/jrdesktop.jar' " + "width='490' height='320'>" + "<param name='noexit' value='true' />" + "<param name='noicon' value='true' />" + "</applet></body></html>\n");
                    out.writeBytes("\n");
                    out.flush();
                } else if (line.startsWith("GET /idle.png")) {
                    out.writeBytes("HTTP/1.0 200 \n");
                    out.writeBytes("Content-type: image/png\n");
                    out.writeBytes("\n");
                    out.flush();
                    InputStream is = main.class.getResourceAsStream("images/idle.png");
                    BufferedInputStream bis = new BufferedInputStream(is);
                    int ch;
                    while ((ch = bis.read()) != -1) out.write(ch);
                    out.writeBytes("\n");
                    out.flush();
                } else if (line.startsWith("GET /logo.png")) {
                    out.writeBytes("HTTP/1.0 200 \n");
                    out.writeBytes("Content-type: image/png\n");
                    out.writeBytes("\n");
                    out.flush();
                    InputStream is = main.class.getResourceAsStream("images/logo.png");
                    BufferedInputStream bis = new BufferedInputStream(is);
                    int ch;
                    while ((ch = bis.read()) != -1) out.write(ch);
                    out.writeBytes("\n");
                    out.flush();
                } else if (line.startsWith("GET /application.png")) {
                    out.writeBytes("HTTP/1.0 200 \n");
                    out.writeBytes("Content-type: image/png\n");
                    out.writeBytes("\n");
                    out.flush();
                    InputStream is = main.class.getResourceAsStream("images/application.png");
                    BufferedInputStream bis = new BufferedInputStream(is);
                    int ch;
                    while ((ch = bis.read()) != -1) out.write(ch);
                    out.writeBytes("\n");
                    out.flush();
                } else if (line.startsWith("GET /applet.png")) {
                    out.writeBytes("HTTP/1.0 200 \n");
                    out.writeBytes("Content-type: image/png\n");
                    out.writeBytes("\n");
                    out.flush();
                    InputStream is = main.class.getResourceAsStream("images/applet.png");
                    BufferedInputStream bis = new BufferedInputStream(is);
                    int ch;
                    while ((ch = bis.read()) != -1) out.write(ch);
                    out.writeBytes("\n");
                    out.flush();
                } else if (line.startsWith("GET /jws.png")) {
                    out.writeBytes("HTTP/1.0 200 \n");
                    out.writeBytes("Content-type: image/png\n");
                    out.writeBytes("\n");
                    out.flush();
                    InputStream is = main.class.getResourceAsStream("images/jws.png");
                    BufferedInputStream bis = new BufferedInputStream(is);
                    int ch;
                    while ((ch = bis.read()) != -1) out.write(ch);
                    out.writeBytes("\n");
                    out.flush();
                } else if (line.startsWith("GET /extension.png")) {
                    out.writeBytes("HTTP/1.0 200 \n");
                    out.writeBytes("Content-type: image/png\n");
                    out.writeBytes("\n");
                    out.flush();
                    InputStream is = main.class.getResourceAsStream("images/extension.png");
                    BufferedInputStream bis = new BufferedInputStream(is);
                    int ch;
                    while ((ch = bis.read()) != -1) out.write(ch);
                    out.writeBytes("\n");
                    out.flush();
                } else if (line.startsWith("GET /portable.png")) {
                    out.writeBytes("HTTP/1.0 200 \n");
                    out.writeBytes("Content-type: image/png\n");
                    out.writeBytes("\n");
                    out.flush();
                    InputStream is = main.class.getResourceAsStream("images/portable.png");
                    BufferedInputStream bis = new BufferedInputStream(is);
                    int ch;
                    while ((ch = bis.read()) != -1) out.write(ch);
                    out.writeBytes("\n");
                    out.flush();
                } else if (line.startsWith("GET /jrdesktop_applet.jnlp")) {
                    String jnlp = getJNLP("http://" + RMIServer.serverConfig.server_address + ":" + String.valueOf(RMIServer.serverConfig.http_port) + "/", true);
                    out.writeBytes("HTTP/1.0 200 \n");
                    out.writeBytes("Content-type: application/x-java-jnlp-file\n");
                    out.writeBytes("Content-Disposition: attachment; filename=jrdesktop_applet.jnlp\n");
                    out.writeBytes("Content-Transfer-Encoding: binary\n");
                    out.writeBytes("Content-Length: " + jnlp.getBytes().length + "\n");
                    out.writeBytes("\n");
                    out.flush();
                    out.writeBytes(jnlp);
                    out.writeBytes("\n");
                    out.flush();
                } else if (line.startsWith("GET /jrdesktop.jnlp")) {
                    String jnlp = getJNLP("http://" + RMIServer.serverConfig.server_address + ":" + String.valueOf(RMIServer.serverConfig.http_port) + "/", false);
                    out.writeBytes("HTTP/1.0 200 \n");
                    out.writeBytes("Content-type: application/x-java-jnlp-file\n");
                    out.writeBytes("Content-Disposition: attachment; filename=jrdesktop.jnlp\n");
                    out.writeBytes("Content-Transfer-Encoding: binary\n");
                    out.writeBytes("Content-Length: " + jnlp.getBytes().length + "\n");
                    out.writeBytes("\n");
                    out.flush();
                    out.writeBytes(jnlp);
                    out.writeBytes("\n");
                    out.flush();
                } else if (line.startsWith("GET /jrdesktop.jar")) {
                    String filename = FileUtility.getJarnameURL();
                    byte[] fileBytes = new byte[] {};
                    if (new File(filename).isFile()) fileBytes = FileUtility.fileToByteArray(filename);
                    out.writeBytes("HTTP/1.0 200 \n");
                    out.writeBytes("Content-type: application/x-java-archive\n");
                    out.writeBytes("Content-Disposition: attachment; filename=jrdesktop.jar\n");
                    out.writeBytes("Content-Transfer-Encoding: binary\n");
                    out.writeBytes("Content-Length: " + fileBytes.length + "\n");
                    out.writeBytes("\n");
                    out.flush();
                    out.write(fileBytes);
                    out.flush();
                }
            }
            out.close();
            in.close();
            client.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Generate a JWS file
     * @param url
     * @param isApplet  if <code>true</code>, generate a JWS applet, otherwise a JWS application
     * @return the generated jnlp file
     */
    public String getJNLP(String url, boolean isApplet) {
        return "<?xml version='1.0' encoding='UTF-8' standalone='yes'?>" + "<jnlp spec='1.0+'>" + "<information>" + "    <title>jrdesktop - Java Remote Desktop</title>" + "    <vendor>BENYAMMI Bachir</vendor>" + "    <homepage href='http://jrdesktop.net/'/>" + "    <description>jrdesktop is a cross-platform open source software that provides view and control of a user's desktop.</description>" + "    <description kind='short'>jrdesktop - Java Remote Desktop</description>" + "    <offline-allowed/>" + "</information>" + "<security>" + "   <all-permissions/>" + "</security>" + "<resources>" + "   <j2se version='1.5+'/>" + "  <jar eager='true' href='" + url + "jrdesktop.jar' main='true'/>" + "</resources>" + (isApplet ? "<applet-desc " + "	width='460'" + "	height='320'" + "	main-class='jrdesktop.mainApplet'" + "	name='jrdesktop - Java Remote Desktop'>" + "</applet-desc>" : "<application-desc main-class='jrdesktop.main'>" + "</application-desc>") + "</jnlp>";
    }
}
