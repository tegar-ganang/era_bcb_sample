package org.hardtokenmgmt.buildtools;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This program will insert an environment entry entry in the ejb-jar.xml file of an ear file.
 * 
 * 
 * The program takes four  parameters:
 * from-ear-file
 * to-ear-file
 * ejb-jar (name of jar containing ejb ejb-jar.xml file to edit)
 * env-entry-name to set 
 * env-entry-value to set
 * 
 * @author Philip Vendil
 *
 */
public class ConfigureEJBJARXML {

    private static final int FROMEAR = 0;

    private static final int TOEAR = 1;

    private static final int EJBJAR = 2;

    private static final int ENVNAME = 3;

    private static final int ENVVALUE = 4;

    /**
	 * @param args
	 */
    public static void main(String[] args) throws Exception {
        if (args.length != 5) {
            displayUsageAndExit();
        }
        File fromEAR = new File(args[FROMEAR]);
        if (!fromEAR.exists() || !fromEAR.canRead() || !fromEAR.isFile()) {
            System.out.println("Error reading EAR, make sure the file " + args[FROMEAR] + " is a file and readable for the user.");
            System.exit(-1);
        }
        File toEAR = new File(args[TOEAR]);
        String ejbJarName = args[EJBJAR];
        String envEntryName = args[ENVNAME];
        String envEntryValue = args[ENVVALUE];
        setEnvEntry(fromEAR, toEAR, ejbJarName, envEntryName, envEntryValue);
    }

    private static void setEnvEntry(File fromEAR, File toEAR, String ejbJarName, String envEntryName, String envEntryValue) throws Exception {
        ZipInputStream earFile = new ZipInputStream(new FileInputStream(fromEAR));
        FileOutputStream fos = new FileOutputStream(toEAR);
        ZipOutputStream tempZip = new ZipOutputStream(fos);
        ZipEntry next = earFile.getNextEntry();
        while (next != null) {
            ByteArrayOutputStream content = new ByteArrayOutputStream();
            byte[] data = new byte[30000];
            int numberread;
            while ((numberread = earFile.read(data)) != -1) {
                content.write(data, 0, numberread);
            }
            if (next.getName().equals(ejbJarName)) {
                content = editEJBJAR(next, content, envEntryName, envEntryValue);
                next = new ZipEntry(ejbJarName);
            }
            tempZip.putNextEntry(next);
            tempZip.write(content.toByteArray());
            next = earFile.getNextEntry();
        }
        earFile.close();
        tempZip.close();
        fos.close();
    }

    private static ByteArrayOutputStream editEJBJAR(ZipEntry zipEntry, ByteArrayOutputStream content, String envEntryName, String envEntryValue) throws Exception {
        ZipInputStream jarFile = new ZipInputStream(new ByteArrayInputStream(content.toByteArray()));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream tempZip = new ZipOutputStream(baos);
        ZipEntry next = jarFile.getNextEntry();
        HashSet<String> addedEntries = new HashSet<String>();
        while (next != null) {
            if (addedEntries.contains(next.getName())) {
                next = jarFile.getNextEntry();
                continue;
            }
            ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
            byte[] data = new byte[30000];
            int numberread;
            while ((numberread = jarFile.read(data)) != -1) {
                dataStream.write(data, 0, numberread);
            }
            if (next.getName().equals("META-INF/ejb-jar.xml")) {
                dataStream = editEnvEntry(next, dataStream, envEntryName, envEntryValue);
                next = new ZipEntry("META-INF/ejb-jar.xml");
            }
            addedEntries.add(next.getName());
            tempZip.putNextEntry(next);
            tempZip.write(dataStream.toByteArray());
            next = jarFile.getNextEntry();
        }
        jarFile.close();
        tempZip.close();
        return baos;
    }

    private static ByteArrayOutputStream editEnvEntry(ZipEntry next, ByteArrayOutputStream content, String envEntryName, String envEntryValue) throws Exception {
        String ejbjarxml = new String(content.toByteArray(), "UTF-8");
        ByteArrayOutputStream retval = new ByteArrayOutputStream();
        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        LocalEntityResolver localEntityResolver = new LocalEntityResolver("http://java.sun.com/dtd/ejb-jar_2_0.dtd", new File("src/deploy/appserver/ejb-jar_2_0.dtd"));
        db.setEntityResolver(localEntityResolver);
        Document d = db.parse(new ByteArrayInputStream(ejbjarxml.getBytes("UTF-8")));
        NodeList nl = d.getElementsByTagName("env-entry");
        for (int i = 0; i < nl.getLength(); i++) {
            Node n = nl.item(i);
            String entryName = getEntryName(n);
            if (entryName != null && entryName.trim().equals(envEntryName)) {
                Node entryValue = getEntryValueNode(n);
                if (entryValue != null) {
                    entryValue.setNodeValue(envEntryValue);
                }
                break;
            }
        }
        Source source = new DOMSource(d);
        Result result = new StreamResult(retval);
        Transformer xformer = TransformerFactory.newInstance().newTransformer();
        xformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "http://java.sun.com/dtd/ejb-jar_2_0.dtd");
        xformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "-//Sun Microsystems, Inc.//DTD Enterprise JavaBeans 2.0//EN");
        xformer.transform(source, result);
        return retval;
    }

    private static Node getEntryValueNode(Node n) {
        NodeList nl = n.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node cn = nl.item(i);
            if (cn.getNodeName() != null && cn.getNodeName().equals("env-entry-value")) {
                return cn.getFirstChild();
            }
        }
        return null;
    }

    private static String getEntryName(Node n) {
        NodeList nl = n.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node cn = nl.item(i);
            if (cn.getNodeName() != null && cn.getNodeName().equals("env-entry-name")) {
                if (cn.getFirstChild() != null) {
                    return cn.getFirstChild().getNodeValue();
                }
            }
        }
        return null;
    }

    private static void displayUsageAndExit() {
        System.out.println("Usage :  <from-ear> <to-ear> <ejb-jar> <env-entry-name> <env-entry-value>\n\n" + "\n" + "This program will insert an environment entry entry in the ejb-jar.xml file of an ear file.");
        System.exit(-1);
    }
}
