package org.roomwareproject.communicator.httpd;

import org.roomwareproject.server.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;

public class WebService implements Runnable {

    private Socket client;

    private Logger logger;

    private PrintStream out;

    private Scanner in;

    private RoomWareServer server;

    WebService(RoomWareServer roomwareServer, Socket client, Logger logger) {
        this.client = client;
        this.logger = logger;
        this.server = roomwareServer;
    }

    public void run() {
        try {
            out = new PrintStream(client.getOutputStream(), false, "UTF-8");
            in = new Scanner(client.getInputStream());
            String line = in.nextLine();
            String path = "/";
            String[] splitted = null;
            splitted = line.split(" ");
            if (splitted[0].trim().contains("GET") && splitted.length > 1) path = splitted[1];
            if (line.contains("crossdomain")) handleCrossDomain(); else handleRequest(path.trim());
            out.close();
            client.close();
        } catch (URISyntaxException ex) {
            Logger.getLogger(WebService.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException cause) {
            logger.warning(cause.getMessage());
        }
    }

    private String xmlEscape(String raw) {
        String escaped;
        if (raw == null) return "";
        escaped = raw.replace("&", "&amp;");
        escaped = escaped.replace("<", "&lt;");
        return escaped;
    }

    private void handleRequest(String httpPath) throws IOException {
        String protocol = "none";
        String prefix = "";
        String suffix = "";
        if (httpPath != "/") {
            String[] splittedPath = httpPath.split("/");
            if (splittedPath.length > 1) {
                protocol = splittedPath[1];
            }
        }
        if (protocol.equals("json-plain")) {
            out.println("HTTP/0.9 200 OK");
            out.println("Server: RoomWare HTTP Communicator");
            out.println("Content-Type: text/plain");
            out.println("");
            prefix = "{\n";
            suffix = "}\n";
        } else if (protocol.equals("json")) {
            out.println("HTTP/0.9 200 OK");
            out.println("Server: RoomWare HTTP Communicator");
            out.println("Content-Type: application/json");
            out.println("");
            prefix = "{\n";
            suffix = "}\n";
        } else if (protocol.equals("xml")) {
            out.println("HTTP/0.9 200 OK");
            out.println("Server: RoomWare HTTP Communicator");
            out.println("Content-Type: text/xml");
            out.println("");
            prefix = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<devicelist>\n";
            suffix = "</devicelist>";
        } else if (protocol.equals("csv")) {
            out.println("HTTP/0.9 200 OK");
            out.println("Server: RoomWare HTTP Communicator");
            out.println("Content-Type: text/csv");
            out.println("");
            prefix = "id,name,type,zone,time\n";
            suffix = "";
        } else if (protocol.equals("none") && httpPath.equals("/")) {
            out.println("HTTP/0.9 200 OK");
            out.println("Server: RoomWare HTTP Communicator");
            out.println("Content-Type: text/html");
            out.println("Connection: close");
            out.println("");
            Scanner ins = new Scanner(getClass().getResourceAsStream("/docs/index.html"));
            while (ins.hasNextLine()) {
                out.println(ins.nextLine());
            }
            return;
        } else {
            out.println("HTTP/0.9 404 Not found");
            out.println("Server: RoomWare HTTP Communicator");
            out.println("");
            return;
        }
        Set<Presence> presences = server.getPresences();
        String response = prefix;
        Iterator it = presences.iterator();
        while (it.hasNext()) {
            Presence p = (Presence) it.next();
            Device d = p.getDevice();
            String deviceAddressRaw = d.getDeviceAddress().toString();
            String friendlyNameRaw = d.getFriendlyName();
            String typeRaw = d.getDeviceAddress().getType();
            String zoneRaw = p.getZone();
            long time = p.getDetectTime().getTime();
            if (protocol.equals("json-plain") || protocol.equals("json")) {
                if (friendlyNameRaw != "null") friendlyNameRaw = "\"" + friendlyNameRaw + "\"";
                response += "\t\"" + deviceAddressRaw + "\": {\n" + "\t\t\"name\": " + friendlyNameRaw + ",\n" + "\t\t\"type\": \"" + typeRaw + "\",\n" + "\t\t\"zone\": \"" + zoneRaw + "\",\n" + "\t\t\"time\": " + time + "\n" + "\t}";
                if (it.hasNext()) {
                    response += ",";
                }
                response += "\n";
            } else if (protocol.equals("xml")) {
                if (friendlyNameRaw == null) friendlyNameRaw = "";
                response += "<device><id>" + xmlEscape(deviceAddressRaw) + "</id><name>" + xmlEscape(friendlyNameRaw) + "</name><type>" + xmlEscape(typeRaw) + "</type><zone>" + xmlEscape(zoneRaw) + "</zone><time>" + time + "</time></device>\n";
            } else if (protocol.equals("csv")) {
                if (friendlyNameRaw == null) friendlyNameRaw = "";
                response += deviceAddressRaw + "," + friendlyNameRaw + "," + typeRaw + "," + zoneRaw + "," + time;
                if (it.hasNext()) {
                    response += "\n";
                }
            } else {
            }
        }
        response += suffix;
        out.println(response);
    }

    private void handleCrossDomain() throws IOException, URISyntaxException {
        out.println("HTTP/0.9 200 OK");
        out.println("Server: RoomWare HTTP Communicator");
        out.println("Content-Type: application/xml");
        out.println("Connection: close");
        out.println("");
        Scanner ins = new Scanner(getClass().getResourceAsStream("/docs/crossdomain.xml"));
        while (ins.hasNextLine()) {
            out.println(ins.nextLine());
        }
    }
}
