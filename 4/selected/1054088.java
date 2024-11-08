package com.ufnasoft.dms.gui;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class GetMyDocuments extends InitDMS {

    String[][] myDocsData = null;

    String[] myDocsToolTip = null;

    String[] myDocumentImageName = null;

    int[] myDocIds = null;

    public GetMyDocuments() {
        String urlString = dms_url + "/servlet/com.ufnasoft.dms.server.ServerGetMyDocuments";
        String rvalue = "";
        String filename = dms_home + FS + "temp" + FS + username + "mydocuments.xml";
        try {
            String urldata = urlString + "?username=" + URLEncoder.encode(username, "UTF-8") + "&key=" + URLEncoder.encode(key, "UTF-8") + "&filename=" + URLEncoder.encode(username, "UTF-8") + "mydocuments.xml";
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
            NodeList nodelist = document.getElementsByTagName("document");
            int num = nodelist.getLength();
            myDocsData = new String[num][4];
            myDocsToolTip = new String[num];
            myDocumentImageName = new String[num];
            myDocIds = new int[num];
            for (int i = 0; i < num; i++) {
                myDocsData[i][0] = new String(DOMUtil.getSimpleElementText((Element) nodelist.item(i), "filename"));
                myDocsData[i][1] = new String(DOMUtil.getSimpleElementText((Element) nodelist.item(i), "project"));
                myDocsData[i][2] = new String(DOMUtil.getSimpleElementText((Element) nodelist.item(i), "deadline"));
                myDocsData[i][3] = new String(DOMUtil.getSimpleElementText((Element) nodelist.item(i), "workingfolder"));
                myDocsToolTip[i] = new String(DOMUtil.getSimpleElementText((Element) nodelist.item(i), "title"));
                myDocumentImageName[i] = new String(DOMUtil.getSimpleElementText((Element) nodelist.item(i), "imagename"));
                myDocIds[i] = (new Integer(new String(DOMUtil.getSimpleElementText((Element) nodelist.item(i), "documentid")))).intValue();
            }
        } catch (MalformedURLException ex) {
            System.out.println(ex);
        } catch (ParserConfigurationException ex) {
            System.out.println(ex);
        } catch (NullPointerException ex) {
        } catch (Exception ex) {
            System.out.println(ex);
        }
    }

    public String[][] getMyDocsData() {
        return myDocsData;
    }

    public String[] getMyDocsToolTip() {
        return myDocsToolTip;
    }

    public String[] getMyDocumentImageName() {
        return myDocumentImageName;
    }

    public int[] getMyDocIds() {
        return myDocIds;
    }
}
