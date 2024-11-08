package com.ufnasoft.dms.gui;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class GetMessages extends InitDMS {

    String[][] messages = null;

    String[][] messageTableData = null;

    public GetMessages(String messageType) {
        String urlString = dms_url + "/servlet/com.ufnasoft.dms.server.ServerGetMessages";
        String rvalue = "";
        String filename = dms_home + FS + "temp" + FS + username + "messages.xml";
        try {
            String urldata = urlString + "?username=" + URLEncoder.encode(username, "UTF-8") + "&key=" + URLEncoder.encode(key, "UTF-8") + "&messagetype=" + messageType + "&filename=" + URLEncoder.encode(username, "UTF-8") + "messages.xml";
            ;
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder parser = factory.newDocumentBuilder();
            URL u = new URL(urldata);
            DataInputStream is = new DataInputStream(u.openStream());
            FileOutputStream os = new FileOutputStream(filename);
            int iBufSize = is.available();
            byte inBuf[] = new byte[20000 * 1024];
            int iNumRead;
            while ((iNumRead = is.read(inBuf, 0, iBufSize)) > 0) os.write(inBuf, 0, iNumRead);
            os.close();
            is.close();
            File f = new File(filename);
            InputStream inputstream = new FileInputStream(f);
            Document document = parser.parse(inputstream);
            NodeList nodelist = document.getElementsByTagName("message");
            int num = nodelist.getLength();
            messages = new String[num][7];
            for (int i = 0; i < num; i++) {
                messages[i][0] = new String(DOMUtil.getSimpleElementText((Element) nodelist.item(i), "messageid"));
                messages[i][1] = new String(DOMUtil.getSimpleElementText((Element) nodelist.item(i), "subject"));
                messages[i][2] = (new String(DOMUtil.getSimpleElementText((Element) nodelist.item(i), "firstname"))) + " " + (new String(DOMUtil.getSimpleElementText((Element) nodelist.item(i), "lastname")));
                messages[i][3] = new String(DOMUtil.getSimpleElementText((Element) nodelist.item(i), "messagedatetime"));
                messages[i][4] = new String(DOMUtil.getSimpleElementText((Element) nodelist.item(i), "messagefrom"));
                messages[i][5] = new String(DOMUtil.getSimpleElementText((Element) nodelist.item(i), "messageto"));
                messages[i][6] = new String(DOMUtil.getSimpleElementText((Element) nodelist.item(i), "documentid"));
            }
        } catch (MalformedURLException ex) {
            System.out.println(ex);
        } catch (ParserConfigurationException ex) {
            System.out.println(ex);
        } catch (NullPointerException e) {
        } catch (Exception ex) {
            System.out.println(ex);
        }
    }

    public String[][] getMessagesData() {
        return messages;
    }

    public String[][] getMessageTableData() {
        messageTableData = new String[messages.length][3];
        for (int w = 0; w < messages.length; w++) {
            messageTableData[w][0] = new String(messages[w][1]);
            messageTableData[w][1] = new String(messages[w][2]);
            messageTableData[w][2] = new String(messages[w][3]);
        }
        return messageTableData;
    }
}
