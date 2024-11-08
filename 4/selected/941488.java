package com.jungleford.msn.hc;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.*;
import javax.swing.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

/**
 * This tool is designed for combining MSN chat logs of one person.
 * (c)2005 jungleford
 * 
 * @author Samuel Lee
 * @version 1.0
 */
public class Combiner extends Thread {

    private int current = 0;

    private int target = 0;

    private File dir1 = null;

    private File dir2 = null;

    private File[] logs1;

    private File[] logs2;

    private File output = null;

    private boolean isDir = false;

    private int sessionNum = 0;

    private static final String CONFIG_FILE = "MessageLog.xsl";

    public Combiner(File dir1, File dir2, File output, boolean isDir) {
        this.dir1 = dir1;
        this.dir2 = dir2;
        this.output = output;
        this.isDir = isDir;
        if (isDir) {
            if (dir1.isFile() || dir2.isFile() || output.isFile()) {
                JOptionPane.showMessageDialog(null, "You should choose a directory, NOT a file!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            List files1 = Arrays.asList(dir1.list());
            List files2 = Arrays.asList(dir2.list());
            if (!files1.contains(CONFIG_FILE) || !files2.contains(CONFIG_FILE)) {
                JOptionPane.showMessageDialog(null, "You should choose a directory with MSN history log files!\nI cannot find the MessageLog.xsl in this directory.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!dir1.getName().equals(dir2.getName())) {
                int choice = JOptionPane.showConfirmDialog(null, "Maybe the two directories donnot belong to the same MSN account.\nPlease check the directories. If confirm, click \"Yes\", and \"No\" otherwise.", "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (choice == JOptionPane.NO_OPTION) return;
            }
            FileFilter filter = new FileFilter() {

                public boolean accept(File f) {
                    return f.isFile() && f.getName().toLowerCase().endsWith(".xml");
                }
            };
            File[] f1 = dir1.listFiles(filter);
            File[] f2 = dir2.listFiles(filter);
            logs1 = f1.length >= f2.length ? f1 : f2;
            logs2 = f1.length < f2.length ? f1 : f2;
            current = 0;
            target = logs1.length;
        } else {
            if (dir1.isDirectory() || dir2.isDirectory() || output.isDirectory()) {
                JOptionPane.showMessageDialog(null, "You should choose a file, NOT a directory!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            current = 0;
            target = 1;
        }
    }

    /**
   * Thread to monitor current.
   */
    public void run() {
        if (!interrupted()) {
            if (isDir) {
                Map map2 = new HashMap();
                for (int i = 0; i < logs2.length; i++) {
                    map2.put(logs2[i].getName(), logs2[i]);
                }
                if (!output.exists()) output.mkdirs();
                File config = new File(dir1, CONFIG_FILE);
                if (config.exists()) copy(config, new File(output, CONFIG_FILE));
                for (int i = 0; i < logs1.length; i++) {
                    current++;
                    File input1 = logs1[i];
                    File input2 = (File) map2.get(input1.getName());
                    if (input2 == null) {
                        copy(input1, new File(output, input1.getName()));
                    } else {
                        combine(input1, input2, new File(output, input1.getName()));
                    }
                    try {
                        sleep(1);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            } else {
                combine(dir1, dir2, output);
                current = 1;
                try {
                    sleep(1);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }

    /**
   * Combine two logs.
   * 
   * @param input1
   *          a xml message log file.
   * @param input2
   *          another log file
   * @param output
   *          the combined log file.
   */
    private void combine(File input1, File input2, File output) {
        Document doc1 = parseXmlFile(input1, false);
        Document doc2 = parseXmlFile(input2, false);
        Element root1 = doc1.getDocumentElement();
        Element root2 = doc2.getDocumentElement();
        NodeList list2 = root2.getChildNodes();
        for (int i = 0; i < list2.getLength(); i++) {
            Node node = doc1.importNode(list2.item(i), true);
            root1.appendChild(node);
        }
        List msgs = combine(root1.getChildNodes());
        root1.setAttribute("LastSessionID", Integer.toString(sessionNum));
        NodeList n = root1.getChildNodes();
        int length = n.getLength();
        int i = 0;
        while (i < length) {
            root1.removeChild(n.item(0));
            i++;
        }
        for (int j = 0; j < msgs.size(); j++) {
            root1.appendChild((Element) msgs.get(j));
        }
        writeXmlFile(doc1, output);
    }

    private List combine(List list) {
        Collections.sort(list);
        sessionNum = arrangeSessionID(list);
        return list;
    }

    private List combine(NodeList list) {
        List newList = repack(list);
        Set s = new HashSet(newList);
        newList = new ArrayList(s);
        return combine(newList);
    }

    /**
   * Combine two message lists and sort the message elements by time.
   * 
   * @param list1
   *          a message list.
   * @param list2
   *          another message list.
   * @return the combined message list.
   */
    private List combine(NodeList list1, NodeList list2) {
        List newList1 = repack(list1);
        List newList2 = repack(list2);
        newList1.addAll(newList2);
        Set s = new HashSet(newList1);
        newList1 = new ArrayList(s);
        return combine(newList1);
    }

    /**
   * Package message elemnts into ComparableMessage objects.
   * 
   * @param list
   *          the list of original message elements.
   * @return the list of ComparableMessage.
   */
    private List repack(NodeList list) {
        List newList = new ArrayList();
        try {
            for (int i = 0; i < list.getLength(); i++) {
                ComparableMessage msg = new ComparableMessage((Element) list.item(i));
                newList.add(msg);
            }
        } catch (HCException e) {
            e.printStackTrace();
        }
        return newList;
    }

    /**
   * An algorithm for arranging session IDs.
   * 
   * @param list
   *          the list of messages (XML elements) which already sorted.
   * @return the total number of sessions.
   */
    private int arrangeSessionID(List list) {
        int oldId = 0, max = 0, delta = 0, oldTotal = 0, total = 0;
        List newList = new ArrayList();
        for (int i = 0; i < list.size(); i++) {
            Element msg = ((ComparableMessage) list.get(i)).getElement();
            int id = Integer.parseInt(msg.getAttribute("SessionID"));
            if (id == oldId || id == oldId + 1) {
                max = id;
            } else {
                delta = max;
            }
            total = id + delta >= oldTotal ? id + delta : oldTotal + 1;
            msg.setAttribute("SessionID", Integer.toString(total));
            newList.add(msg);
            oldId = id;
            oldTotal = total;
        }
        list.removeAll(list);
        for (int i = 0; i < newList.size(); i++) {
            list.add(newList.get(i));
        }
        return total;
    }

    /**
   * Copy from JAVA ELMANAC. Parses an XML file and returns a DOM document.
   * If validating is true,
   * the contents is validated against the DTD specified in the file.
   * 
   * @param file
   *          the MSN log file.
   * @param validating
   * @return the Document node handle.
   */
    private static Document parseXmlFile(File file, boolean validating) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(validating);
            Document doc = factory.newDocumentBuilder().parse(file);
            return doc;
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
   * Copy from JAVA ELMANAC. This method writes a DOM document to a file.
   * 
   * @param doc
   * @param file
   *          output XML file.
   */
    private static void writeXmlFile(Document doc, File file) {
        try {
            Source source = new DOMSource(doc);
            Result result = new StreamResult(file);
            Transformer xformer = TransformerFactory.newInstance().newTransformer();
            xformer.transform(source, result);
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }
    }

    /**
   * Copy from JAVA ELMANAC.
   */
    private static void copy(File src, File dst) {
        try {
            FileChannel srcChannel = new FileInputStream(src).getChannel();
            FileChannel dstChannel = new FileOutputStream(dst).getChannel();
            dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
            srcChannel.close();
            dstChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
   * @return current log being processed.
   */
    public int getCurrent() {
        return current;
    }

    /**
   * @return total number of log files.
   */
    public int getTarget() {
        return target;
    }
}
