package utils;

import java.io.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Bastian Hinterleitner
 */
public class EasyXml {

    /**
     *The parts the xml file has
     */
    public Part[] part = new Part[9999];

    /**
     *number of parts the xml file has
     */
    protected int parts = 0;

    /**
     *url to the file the xml is saved in
     */
    public String url;

    /**
     *name of the xml (not the file)
     */
    public String name;

    /**
     *Creates an xml to which parts can be added
     * @param name name of the xml (not the file)
     */
    public EasyXml(String name) {
        this.name = name;
    }

    /**
     *finds all values of a given path in your EasyXml
     * @param node array of nodes(Strings) that act like a path to the item you want to find (eg: new String[]{"person","age"} as node finds all items under [...]person[...]age)
     * @return returns an array of all found items(as Strings) with the given node path
     */
    public String[] find(String[] node) {
        save(System.getProperty("java.io.tmpdir") + "\\tempEasyXml.xml");
        return find(new File(System.getProperty("java.io.tmpdir") + "\\tempEasyXml.xml"), node);
    }

    /**
     *finds all values of a given path of an xml file
     * @param file url of the xml file
     * @param node array of nodes(Strings) that act like a path to the item you want to find (eg: new String[]{"person","age"} as node finds all items under [...]person[...]age)
     * @return returns an array of all found items(as Strings) with the given node path
     */
    public static String[] find(File file, String[] node) {
        if (file.exists()) {
            try {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document doc = db.parse(file);
                doc.getDocumentElement().normalize();
                Node firstNode = doc.getChildNodes().item(0);
                NodeList nodeLst = doc.getElementsByTagName(node[node.length - 1]);
                Element element;
                NodeList scnLst;
                if (node.length > 1) {
                    String[] str;
                    String[] firstStr = new String[9999];
                    int strLength = 0;
                    for (int i = 0; i < nodeLst.getLength(); i++) {
                        Node newNode = nodeLst.item(i);
                        boolean entspricht = false;
                        for (int j = node.length - 2; j >= 0; j--) {
                            while ((!newNode.getParentNode().equals(firstNode)) && (!newNode.getParentNode().getNodeName().equals(node[j]))) {
                                newNode = newNode.getParentNode();
                            }
                        }
                        if (!newNode.getParentNode().getNodeName().equals(firstNode.getNodeName())) entspricht = true;
                        if (entspricht) {
                            int j = 0;
                            while (j < nodeLst.item(i).getChildNodes().getLength()) {
                                if (!nodeLst.item(i).getChildNodes().item(j).getNodeName().equals("#text")) {
                                    firstStr[strLength] = nodeLst.item(i).getChildNodes().item(j).getNodeName();
                                    strLength++;
                                } else {
                                    if (nodeLst.item(i).getChildNodes().getLength() == 1) {
                                        firstStr[strLength] = nodeLst.item(i).getChildNodes().item(j).getTextContent();
                                        strLength++;
                                        j++;
                                    }
                                }
                                j++;
                            }
                        }
                    }
                    str = new String[strLength];
                    for (int i = 0; i < str.length; i++) {
                        str[i] = firstStr[i];
                    }
                    return str;
                } else {
                    String[] firstStr = new String[9999];
                    int lng = nodeLst.getLength();
                    int strLength = 0;
                    for (int i = 0; i < lng; i++) {
                        int j = 0;
                        while (j < nodeLst.item(i).getChildNodes().getLength()) {
                            if (!nodeLst.item(i).getChildNodes().item(j).getNodeName().equals("#text")) {
                                firstStr[strLength] = nodeLst.item(i).getChildNodes().item(j).getNodeName();
                                strLength++;
                            } else {
                                if (nodeLst.item(i).getChildNodes().getLength() == 1) {
                                    firstStr[strLength] = nodeLst.item(i).getChildNodes().item(j).getTextContent();
                                    strLength++;
                                    j++;
                                }
                            }
                            j++;
                        }
                    }
                    String[] str = new String[strLength];
                    for (int i = 0; i < str.length; i++) {
                        str[i] = firstStr[i];
                    }
                    return str;
                }
            } catch (Exception e) {
                return null;
            }
        } else {
            return new String[] {};
        }
    }

    /**
     *Saves your xml
     * @param url path where to save the xml (eg "/directory/xmlFile.xml")
     * @return returns whether successful
     */
    public boolean save(String url) {
        try {
            String str = "<?xml version=\"1.0\" encoding=\"iso-8859-15\" ?>\n<" + name + ">\n";
            for (int i = 0; i < parts; i++) {
                str += part[i].save();
            }
            str += "</" + name + ">";
            utils.SimpleFile.save(str, url);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     *Adds a part to your xml with a given name
     * @param name the name of the part
     * @return the added Part is returned and may be used for further adding parts
     */
    public Part addPart(String name) {
        part[parts] = new Part(name, 1);
        Part p = part[parts];
        parts++;
        return p;
    }

    /**
     *Adds a final part to your xml with a given name and value
     * @param name the name of the part
     * @param info the value of the part
     * @return the added Part is returned but shouldnt be used for further adding parts
     */
    public Part addPart(String name, String info) {
        part[parts] = new Part(name, info, 1);
        Part p = part[parts];
        parts++;
        return p;
    }

    /**
     *removes a part from the xml
     * @param i number of the part to remove (call getParts() to know which one to remove)
     * @return return whether successful
     */
    public boolean remPart(int i) {
        try {
            for (int j = i; j < parts; j++) {
                part[j] = part[j + 1];
            }
            parts--;
            return true;
        } catch (Exception e) {
            System.out.println(e);
            return false;
        }
    }

    /**
     *lists all the Parts added in a String array (without values)
     * @return String array of names of all parts added
     */
    public String[] getParts() {
        String[] str = new String[parts];
        for (int i = 0; i < str.length; i++) {
            str[i] = part[i].name;
        }
        return str;
    }
}
