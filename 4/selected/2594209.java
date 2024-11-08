package net.abhijat.se.process.integration.cvsi.db.test;

import java.io.*;
import javax.xml.parsers.*;
import net.abhijat.se.process.integration.cvsi.db.XMLDBLight;
import org.w3c.dom.*;

/**
 * @author abhijat
 */
public class TestXMLDB {

    private XMLDBLight db;

    private BufferedReader sysin;

    private Document document;

    public TestXMLDB() throws Exception {
        db = XMLDBLight.createReadWriteDB(".", "testdb");
        this.sysin = new BufferedReader(new InputStreamReader(System.in));
        DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = fac.newDocumentBuilder();
        this.document = builder.newDocument();
        Thread t = new Thread(new writer());
        t.setPriority(Thread.NORM_PRIORITY);
        t.start();
    }

    public void cleanup() {
    }

    private void write() throws Exception {
        System.out.print("Enter :");
        String line = sysin.readLine();
        if (line.startsWith("q:")) {
            this.read(line.substring(2));
            return;
        }
        Element commentElement = document.createElement("cvs-comment");
        Node textComment = document.createTextNode(line);
        commentElement.setAttribute("date", System.currentTimeMillis() + "");
        commentElement.appendChild(textComment);
        String id;
        if ((id = db.appendNode(commentElement)) != null) {
            System.out.println("Appended node with id:" + id);
        } else {
            System.out.println("Could not append");
        }
    }

    private void read(String id) throws Exception {
        InputStream is = db.openReadStream(id);
        byte data[] = new byte[128];
        int amt = 0;
        while ((amt = is.read(data)) > -1) {
            String str = new String(data, 0, amt);
            System.out.println("DATA >>>");
            System.out.println(str);
            System.out.println("<<< DATA ");
        }
        is.close();
    }

    class writer implements Runnable {

        public void run() {
            while (true) {
                try {
                    write();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main2(String args[]) throws Exception {
        TestXMLDB db = new TestXMLDB();
        db.cleanup();
    }

    public static void main(String args[]) {
        try {
            main2(args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
