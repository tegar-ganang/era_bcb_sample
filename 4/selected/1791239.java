package com.ufnasoft.dms.gui;

import java.awt.*;
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
import java.util.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class RawTableData extends InitDMS {

    int selectedProjectId = 0;

    String[][] rawTableData = null;

    String[] imageNames = null;

    public RawTableData(int selectedId) {
        selectedProjectId = selectedId;
        String urlString = dms_url + "/servlet/com.ufnasoft.dms.server.ServerGetProjectDocuments";
        String rvalue = "";
        String filename = dms_home + FS + "temp" + FS + username + "documents.xml";
        try {
            String urldata = urlString + "?username=" + URLEncoder.encode(username, "UTF-8") + "&key=" + URLEncoder.encode(key, "UTF-8") + "&projectid=" + selectedProjectId + "&filename=" + URLEncoder.encode(username, "UTF-8") + "documents.xml";
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
            NodeList nodelist = document.getElementsByTagName("doc");
            int num = nodelist.getLength();
            rawTableData = new String[num][11];
            imageNames = new String[num];
            for (int i = 0; i < num; i++) {
                rawTableData[i][0] = new String(DOMUtil.getSimpleElementText((Element) nodelist.item(i), "did"));
                rawTableData[i][1] = new String(DOMUtil.getSimpleElementText((Element) nodelist.item(i), "t"));
                rawTableData[i][2] = new String(DOMUtil.getSimpleElementText((Element) nodelist.item(i), "f"));
                rawTableData[i][3] = new String(DOMUtil.getSimpleElementText((Element) nodelist.item(i), "d"));
                rawTableData[i][4] = new String(DOMUtil.getSimpleElementText((Element) nodelist.item(i), "l"));
                String firstname = new String(DOMUtil.getSimpleElementText((Element) nodelist.item(i), "fn"));
                String lastname = new String(DOMUtil.getSimpleElementText((Element) nodelist.item(i), "ln"));
                rawTableData[i][5] = firstname + " " + lastname;
                rawTableData[i][6] = new String(DOMUtil.getSimpleElementText((Element) nodelist.item(i), "dln"));
                rawTableData[i][7] = new String(DOMUtil.getSimpleElementText((Element) nodelist.item(i), "rsid"));
                rawTableData[i][8] = new String(DOMUtil.getSimpleElementText((Element) nodelist.item(i), "img"));
                imageNames[i] = new String(DOMUtil.getSimpleElementText((Element) nodelist.item(i), "img"));
                rawTableData[i][9] = new String(DOMUtil.getSimpleElementText((Element) nodelist.item(i), "ucin"));
                rawTableData[i][10] = new String(DOMUtil.getSimpleElementText((Element) nodelist.item(i), "dtid"));
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

    public String[][] getTableData() {
        String[][] tableData = new String[rawTableData.length][5];
        for (int w = 0; w < rawTableData.length; w++) {
            tableData[w][0] = new String(rawTableData[w][2]);
            tableData[w][1] = new String(rawTableData[w][7]);
            tableData[w][2] = new String(rawTableData[w][4]);
            if (rawTableData[w][5].equals("null null")) {
                tableData[w][3] = new String("null");
            } else {
                tableData[w][3] = new String(rawTableData[w][5]);
            }
            try {
                if (tableData[w][2].equals("true") && (!rawTableData[w][9].equals("yes"))) {
                    tableData[w][2] = new String("stealMate");
                }
                if (tableData[w][2].equals("3")) {
                    tableData[w][3] = new String("final");
                }
            } catch (NullPointerException e) {
                ;
            }
            tableData[w][4] = new String(rawTableData[w][6]);
        }
        return tableData;
    }

    public String[][] getRawTableData() {
        return rawTableData;
    }

    public String[] getImageNames() {
        return imageNames;
    }
}
