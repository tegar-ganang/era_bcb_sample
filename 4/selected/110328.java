package ch.cern.lhcb.xmleditor.specific;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.swing.JOptionPane;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import ch.cern.lhcb.xmleditor.Editor;
import ch.cern.lhcb.xmleditor.ResourceManager;
import ch.cern.lhcb.xmleditor.xerces.CustomDOMParser;
import ch.cern.lhcb.xmleditor.xerces.DocumentImpl;
import ch.cern.lhcb.xmleditor.xerces.ElementImpl;

/**
 * This class gets some default setup information from the setup.xml file if
 * present in the current path. It then allows access to this information
 */
public class Setup {

    /**
     * the file separator in a platform dependant way
     */
    private static final String FS = System.getProperty("file.separator");

    /**
     * the cariage return in a platform dependant way
     */
    private static final String CR = System.getProperty("line.separator");

    /**
     * name of the xml file containing the setup
     */
    private static final String SETUP_FILE_NAME = ".setup.xml";

    /**
     * full name of the xml file containing the setup
     */
    private static final String FULL_SETUP_FILE_NAME = System.getProperty("user.home") + FS + SETUP_FILE_NAME;

    /**
     * name of the dtd file used by the setup
     */
    private static final String SETUP_DTD_NAME = ".setup.dtd";

    /**
     * full name of the dtd file used by the setup
     */
    private static final String FULL_SETUP_DTD_NAME = System.getProperty("user.home") + FS + SETUP_DTD_NAME;

    private static final String ROOT_ELEMENT = "SETUP";

    private static final String VERSION_ELEMENT = "VERSION";

    private static final String MAJOR_VERSION_ATTRIBUTE = "v";

    private static final int CURRENT_MAJOR_VERSION = 1;

    private static final String MINOR_VERSION_ATTRIBUTE = "r";

    private static final int CURRENT_MINOR_VERSION = 3;

    private static final String OPEN_FILES_ELEMENT = "OPEN_FILES";

    private static final String MAX_NB_ATTRIBUTE = "max_nb";

    private static final String OPEN_FILE_ELEMENT = "OPEN_FILE";

    private static final String REFERENCE_ATTRIBUTE = "reference";

    private static final String LAST_OPEN_ATTRIBUTE = "last_open";

    private static final String ICON_FILES_ELEMENT = "ICON_FILES";

    private static final String ICON_FILE_ELEMENT = "ICON_FILE";

    private static final String ELEMENT_ATTRIBUTE = "name";

    private static final String FILE_ATTRIBUTE = "file";

    private static final String RECORD_DATA_ELEMENT = "RECORD_DATA";

    private static final String DATA_LOCATION_ELEMENT = "DATA_LOCATION";

    private static final String PATH_ATTRIBUTE = "path";

    private static final String DATA_TYPE_ELEMENT = "DATA_TYPE";

    private static final String TYPE_ATTRIBUTE = "type";

    private static final String DTD_ATTRIBUTE = "dtd";

    private static final String ROOT_NODE_ATTRIBUTE = "root_node";

    private static final String MISC_ELEMENT = "MISC";

    private static final String ALLOW_REFERENCES_ELEMENT = "ALLOW_REFERENCES";

    /**
     * The list of elements for which icons are defined in the default setup.
     * Must be synchronized with files
     */
    private static final String[] elements = { ResourceManager.getString("record"), ResourceManager.getString("group"), ResourceManager.getString("command"), ResourceManager.getString("signal"), ResourceManager.getString("expr"), ResourceManager.getString("level"), ResourceManager.getString("ramp"), ResourceManager.getString("sine") };

    /**
     * The list of files where icons are defined in the default setup. Must be
     * synchronized with elements
     */
    private static final String[] files = { "record.png", "group.png", "command.png", "signal.png", "expr.png", "level.png", "ramp.png", "sine.png" };

    /**
     * the path where to find data in the default setup.
     */
    private static final String[] data_paths = { "/records" };

    /**
     * the data types defined in the default setup. Must be synchronized with
     * dtds and root_nodes
     */
    private static final String[][] data_types = { { "Record", "Signal" } };

    /**
     * the dtds for each data type defined in the default setup. Must be
     * synchronized with data_types and root_nodes
     */
    private static final String[][] dtds = { { "signal.dtd", "signal.dtd" } };

    /**
     * whether references are allowed or not for each dtd defined in the default
     * setup. Must be synchronized with data_types, root_nodes and paths
     */
    private static final boolean[][] allow_references = { { true, true, true, true } };

    /**
     * the root node names for each path and data types defined in the default
     * setup. Must be synchronized with dtds and data_types
     */
    private static final String[][] root_nodes = { { "record", "signal" } };

    /**
     * the maximal number of files already opened and put into the File menu
     */
    private static int DEFAULT_MAX_NB_ATTRIBUTE = 6;

    /**
     * the maximal number of files already opened and put into the File menu
     */
    private int maxNbOfOpenFiles = DEFAULT_MAX_NB_ATTRIBUTE;

    /**
     * the editor using this setup
     */
    private Editor editor;

    /**
     * this document is the image of the setup xml file
     */
    private Node setup;

    /**
     * the serializer used to serialize
     */
    private XMLSerializer serializer;

    /**
     * constructors. It tries to get and parse setup.xml
     * 
     * @param editor the editor using this setup
     */
    public Setup(Editor editor) {
        setup = null;
        serializer = null;
        this.editor = editor;
        checkAndCreateSetup();
    }

    /**
     * checks if setup files exist and create default ones if not
     */
    private void checkAndCreateSetup() {
        checkAndCreateDTD();
        checkAndCreateXml();
    }

    /**
     * checks if a dtd setup file exists and creates a default one if not
     */
    private void checkAndCreateDTD() {
        File dtdFile = new File(FULL_SETUP_DTD_NAME);
        try {
            dtdFile.createNewFile();
            FileWriter fileWriter = new FileWriter(dtdFile);
            fileWriter.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + CR);
            fileWriter.write("  <!--Setup root element-->" + CR);
            fileWriter.write("  <!ELEMENT " + ROOT_ELEMENT + " (" + VERSION_ELEMENT + ", " + OPEN_FILES_ELEMENT + ", " + ICON_FILES_ELEMENT + ", " + RECORD_DATA_ELEMENT + ", " + MISC_ELEMENT + ")>" + CR + CR);
            fileWriter.write("  <!-- defines the version of the setup file -->" + CR);
            fileWriter.write("  <!ELEMENT " + VERSION_ELEMENT + " EMPTY>" + CR);
            fileWriter.write("  <!-- the only attribute is the main version number -->" + CR);
            fileWriter.write("  <!ATTLIST " + VERSION_ELEMENT + " " + MAJOR_VERSION_ATTRIBUTE + " CDATA #REQUIRED  " + MINOR_VERSION_ATTRIBUTE + " CDATA #IMPLIED>" + CR);
            fileWriter.write("  <!-- defines a list of already opened files -->" + CR);
            fileWriter.write("  <!ELEMENT " + OPEN_FILES_ELEMENT + " (" + OPEN_FILE_ELEMENT + ")*>" + CR);
            fileWriter.write("  <!-- the attribute is the maximum number of " + "open files -->" + CR);
            fileWriter.write("  <!ATTLIST " + OPEN_FILES_ELEMENT + " " + MAX_NB_ATTRIBUTE + " CDATA #REQUIRED>" + CR);
            fileWriter.write("  <!-- defines a file -->" + CR);
            fileWriter.write("  <!-- the data is a name given to the file -->" + CR);
            fileWriter.write("  <!ELEMENT " + OPEN_FILE_ELEMENT + " EMPTY>" + CR);
            fileWriter.write("  <!-- the first attribute is the real place where to " + "find the file -->" + CR);
            fileWriter.write("  <!-- the second attribute is the date of the last " + "access to this file -->" + CR);
            fileWriter.write("  <!ATTLIST " + OPEN_FILE_ELEMENT + " " + REFERENCE_ATTRIBUTE + " CDATA #REQUIRED " + LAST_OPEN_ATTRIBUTE + " CDATA #REQUIRED>" + CR + CR);
            fileWriter.write("  <!-- defines a list of icon files -->" + CR);
            fileWriter.write("  <!ELEMENT " + ICON_FILES_ELEMENT + " (" + ICON_FILE_ELEMENT + "*)>" + CR);
            fileWriter.write("  <!-- defines an icon file and the related element -->" + CR);
            fileWriter.write("  <!-- the data is the name of the element associated to " + "the file -->" + CR);
            fileWriter.write("  <!ELEMENT " + ICON_FILE_ELEMENT + " EMPTY>" + CR);
            fileWriter.write("  <!-- the only attribute is the real place where to find " + "the icon file -->" + CR);
            fileWriter.write("  <!ATTLIST " + ICON_FILE_ELEMENT + " " + ELEMENT_ATTRIBUTE + " CDATA #REQUIRED " + FILE_ATTRIBUTE + " CDATA #REQUIRED>" + CR + CR);
            fileWriter.write("  <!-- knowledge concerning the data used with this " + "editor -->" + CR);
            fileWriter.write("  <!ELEMENT " + RECORD_DATA_ELEMENT + " (" + DATA_LOCATION_ELEMENT + "*)>" + CR);
            fileWriter.write("  <!-- path to the root of some data -->" + CR);
            fileWriter.write("  <!ELEMENT " + DATA_LOCATION_ELEMENT + " (" + DATA_TYPE_ELEMENT + "*)>" + CR);
            fileWriter.write("  <!-- the only attribute is the place where to find the " + "data -->" + CR);
            fileWriter.write("  <!ATTLIST " + DATA_LOCATION_ELEMENT + " " + PATH_ATTRIBUTE + " CDATA #REQUIRED>" + CR);
            fileWriter.write("  <!-- a given type of data -->" + CR);
            fileWriter.write("  <!ELEMENT " + DATA_TYPE_ELEMENT + " EMPTY>" + CR);
            fileWriter.write("  <!-- the first attribute is the dtd file for this type, " + "relative to the DATA_LOCATION -->" + CR);
            fileWriter.write("  <!-- the second attribute is the root element to use for " + "this data type -->" + CR);
            fileWriter.write("  <!ATTLIST " + DATA_TYPE_ELEMENT + " " + TYPE_ATTRIBUTE + " CDATA #REQUIRED " + DTD_ATTRIBUTE + " CDATA #REQUIRED " + ROOT_NODE_ATTRIBUTE + " CDATA #REQUIRED>" + CR + CR);
            fileWriter.write("  <!-- miscellaneous knowledge -->" + CR);
            fileWriter.write("  <!ELEMENT " + MISC_ELEMENT + " (" + ALLOW_REFERENCES_ELEMENT + "*)>" + CR);
            fileWriter.write("  <!-- specifies that references are allowed in a given " + "dtd -->" + CR);
            fileWriter.write("  <!ELEMENT " + ALLOW_REFERENCES_ELEMENT + " EMPTY>" + CR);
            fileWriter.write("  <!-- the only attribute is the dtd where references are " + "allowed -->" + CR);
            fileWriter.write("  <!ATTLIST " + ALLOW_REFERENCES_ELEMENT + " " + DTD_ATTRIBUTE + " CDATA #REQUIRED>" + CR + CR);
            fileWriter.flush();
            fileWriter.close();
        } catch (java.io.IOException e) {
            dtdFile.delete();
        }
    }

    /**
     * checks if an xml setup file exists and creates a default one if not
     */
    private void checkAndCreateXml() {
        File setupFile = new File(FULL_SETUP_FILE_NAME);
        if (!setupFile.exists()) {
            try {
                setupFile.createNewFile();
                FileWriter fileWriter = new FileWriter(setupFile);
                fileWriter.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + CR);
                fileWriter.write("<!DOCTYPE " + ROOT_ELEMENT + " SYSTEM \"" + SETUP_DTD_NAME + "\">" + CR);
                fileWriter.write("<" + ROOT_ELEMENT + ">" + CR);
                fileWriter.write("<" + VERSION_ELEMENT + " " + MAJOR_VERSION_ATTRIBUTE + "=\"" + CURRENT_MAJOR_VERSION + "\"");
                if (CURRENT_MINOR_VERSION > 0) {
                    fileWriter.write(" " + MINOR_VERSION_ATTRIBUTE + "=\"" + CURRENT_MINOR_VERSION + "\"");
                }
                fileWriter.write(" />" + CR);
                fileWriter.write("    <" + OPEN_FILES_ELEMENT + " " + MAX_NB_ATTRIBUTE + "=\"" + DEFAULT_MAX_NB_ATTRIBUTE + "\">" + CR);
                fileWriter.write("    </" + OPEN_FILES_ELEMENT + ">" + CR);
                fileWriter.write("    <" + ICON_FILES_ELEMENT + ">" + CR);
                for (int i = 0; i < files.length; i++) {
                    fileWriter.write("        <" + ICON_FILE_ELEMENT + " " + ELEMENT_ATTRIBUTE + "=\"" + elements[i] + "\" " + FILE_ATTRIBUTE + "=\"" + files[i] + "\"/>" + CR);
                }
                fileWriter.write("    </" + ICON_FILES_ELEMENT + ">" + CR);
                fileWriter.write("    <" + RECORD_DATA_ELEMENT + ">" + CR);
                for (int i = 0; i < data_paths.length; i++) {
                    fileWriter.write("        <" + DATA_LOCATION_ELEMENT + " " + PATH_ATTRIBUTE + "=\"" + data_paths[i] + "\">" + CR);
                    for (int j = 0; j < data_types[i].length; j++) {
                        fileWriter.write("        <" + DATA_TYPE_ELEMENT + " " + TYPE_ATTRIBUTE + "=\"" + data_types[i][j] + "\" " + DTD_ATTRIBUTE + "=\"" + dtds[i][j] + "\" " + ROOT_NODE_ATTRIBUTE + "=\"" + root_nodes[i][j] + "\"/>" + CR);
                    }
                    fileWriter.write("        </" + DATA_LOCATION_ELEMENT + ">" + CR);
                }
                fileWriter.write("    </" + RECORD_DATA_ELEMENT + ">" + CR);
                fileWriter.write("    <" + MISC_ELEMENT + ">" + CR);
                for (int i = 0; i < data_paths.length; i++) {
                    for (int j = 0; j < data_types[i].length; j++) {
                        if (allow_references[i][j]) {
                            fileWriter.write("        <" + ALLOW_REFERENCES_ELEMENT + " " + DTD_ATTRIBUTE + "=\"" + dtds[i][j] + "\"/>" + CR);
                        }
                    }
                }
                fileWriter.write("    </" + MISC_ELEMENT + ">" + CR);
                fileWriter.write("</" + ROOT_ELEMENT + ">" + CR);
                fileWriter.flush();
                fileWriter.close();
                loadSetup();
            } catch (java.io.IOException e) {
                setupFile.delete();
            }
        } else {
            loadSetup();
            if (this.getVersion() < CURRENT_MAJOR_VERSION) {
                JOptionPane.showMessageDialog(editor, "Your current setup files are old versions.\n" + "You should remove them and restart from scratch\n" + "to get full custumization facilities of the editor.\n" + "Remember they are located in your home directory and\n" + "are called .setup.xml and .setup.dtd.\n\n" + "If for some reason you don't want to remove them\n" + "(some personnal configuration maybe ?), try to move\n" + "them, relaunch the editor and merge your data into\n" + "the new ones.");
            } else if (this.getMinorVersion() < CURRENT_MINOR_VERSION) {
                JOptionPane.showMessageDialog(editor, "Your current setup files are old version, namely " + MAJOR_VERSION_ATTRIBUTE + this.getVersion() + MINOR_VERSION_ATTRIBUTE + this.getMinorVersion() + ".\n" + "I will update it to version " + MAJOR_VERSION_ATTRIBUTE + CURRENT_MAJOR_VERSION + MINOR_VERSION_ATTRIBUTE + CURRENT_MINOR_VERSION + ".\n");
                this.updateSetup(CURRENT_MAJOR_VERSION, this.getMinorVersion(), CURRENT_MINOR_VERSION);
            }
        }
    }

    /**
     * loads the setup from the setup file
     */
    private void loadSetup() {
        CustomDOMParser parser = CustomDOMParser.getParser();
        try {
            try {
                parser.setFeature("http://apache.org/xml/features/dom/" + "include-ignorable-whitespace", false);
            } catch (org.xml.sax.SAXNotSupportedException e) {
            } catch (org.xml.sax.SAXNotRecognizedException e) {
            }
            File setupFile = new File(FULL_SETUP_FILE_NAME);
            parser.parse("File:///" + setupFile.getAbsolutePath());
            Document document = parser.getDocument();
            setup = document.getElementsByTagName(ROOT_ELEMENT).item(0);
            maxNbOfOpenFiles = loadMaxNbOfOpenFiles();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(editor, "Unable to open the setup file.\n" + "You will get the default configuration.");
        }
    }

    /**
     * gets the maximum number open files stored in the setup from the setup
     * 
     * @return the maximum number open files stored in the setup
     */
    private int loadMaxNbOfOpenFiles() {
        Element openFiles = (Element) getChildByName(setup, OPEN_FILES_ELEMENT);
        if (openFiles != null) {
            try {
                return Integer.parseInt(openFiles.getAttribute(MAX_NB_ATTRIBUTE));
            } catch (NumberFormatException e) {
            }
        }
        return DEFAULT_MAX_NB_ATTRIBUTE;
    }

    /**
     * get the first child of node with name name
     * 
     * @param node the node concerned
     * @param name the name of the child we are looking for
     * @return the first child of node with name name
     */
    private Node getChildByName(Node node, String name) {
        NodeList children = ((Element) node).getElementsByTagName(name);
        if (children.getLength() > 0) {
            return children.item(0);
        } else {
            return null;
        }
    }

    /**
     * get the first child of node with name as "name" attribute
     * 
     * @param node the node concerned
     * @param name the value of the "name" attribute
     * @return the first child of node with name as "name" attribute
     */
    private Element getElementByNameAttribute(Node node, String name) {
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element) {
                Element result = (Element) child;
                if (result.getAttribute("name").equals(name)) {
                    return result;
                }
            }
        }
        return null;
    }

    /**
     * saves the current setup by writing it back to the file it came from
     */
    private void save() {
        try {
            OutputStream outputStream = new FileOutputStream(FULL_SETUP_FILE_NAME);
            if (serializer != null) {
                serializer.reset();
                serializer.setOutputByteStream(outputStream);
            } else {
                OutputFormat outputFormat = new OutputFormat(setup.getOwnerDocument());
                outputFormat.setPreserveSpace(false);
                outputFormat.setIndenting(true);
                outputFormat.setLineWidth(0);
                serializer = new XMLSerializer(outputStream, outputFormat);
            }
            serializer.serialize(setup.getOwnerDocument());
            outputStream.close();
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }
    }

    /**
     * accessor to the version number
     * 
     * @return the version number
     */
    public int getVersion() {
        if (setup == null) {
            return -1;
        }
        Node version = getChildByName(setup, VERSION_ELEMENT);
        if (version == null) {
            return -1;
        }
        String result = ((Element) version).getAttribute(MAJOR_VERSION_ATTRIBUTE);
        try {
            return Integer.parseInt(result);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * accessor to the minor version number
     * 
     * @return the minor version number
     */
    public int getMinorVersion() {
        if (setup == null) {
            return -1;
        }
        Node version = getChildByName(setup, VERSION_ELEMENT);
        if (version == null) {
            return -1;
        }
        String result = ((Element) version).getAttribute(MINOR_VERSION_ATTRIBUTE);
        try {
            return Integer.parseInt(result);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * accessor to the last open files
     * 
     * @return an array containing the name of the last open files
     */
    public String[] getLastOpenFiles() {
        if (setup == null) {
            return null;
        }
        Node openFiles = getChildByName(setup, OPEN_FILES_ELEMENT);
        if (openFiles == null) {
            return null;
        }
        NodeList files = openFiles.getChildNodes();
        String[] result = new String[files.getLength()];
        for (int i = 0; i < files.getLength(); i++) {
            result[i] = ((Element) files.item(i)).getAttribute(REFERENCE_ATTRIBUTE);
        }
        return result;
    }

    /**
     * adds a file to the list of last open files
     * 
     * @param fileName the name of the file
     */
    public void addLastOpenFile(String fileName) {
        if (setup == null) {
            return;
        }
        Element openFiles = (Element) getChildByName(setup, OPEN_FILES_ELEMENT);
        if (openFiles == null) {
            return;
        }
        Node file = null;
        NodeList files = openFiles.getElementsByTagName(OPEN_FILE_ELEMENT);
        for (int i = 0; i < files.getLength(); i++) {
            if (((Element) files.item(i)).getAttribute(REFERENCE_ATTRIBUTE).equals(fileName)) {
                file = files.item(i);
                break;
            }
        }
        Date today = Calendar.getInstance().getTime();
        if (file != null) {
            ((Element) file).setAttribute(LAST_OPEN_ATTRIBUTE, DateFormat.getDateTimeInstance().format(today));
        } else {
            while (files.getLength() >= maxNbOfOpenFiles) {
                file = null;
                Date date = today;
                for (int i = 0; i < files.getLength(); i++) {
                    try {
                        Date date2 = DateFormat.getDateTimeInstance().parse(((Element) files.item(i)).getAttribute(LAST_OPEN_ATTRIBUTE));
                        if (date2.before(date)) {
                            date = date2;
                            file = files.item(i);
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
                openFiles.removeChild(file);
            }
            Element newNode = openFiles.getOwnerDocument().createElement(OPEN_FILE_ELEMENT);
            newNode.setAttribute(REFERENCE_ATTRIBUTE, fileName);
            newNode.setAttribute(LAST_OPEN_ATTRIBUTE, DateFormat.getDateTimeInstance().format(today));
            openFiles.appendChild(newNode);
        }
        editor.rebuildOpenFilesFileMenu();
        save();
    }

    /**
     * accessor to the icons
     * 
     * @return a Map containing icon file names for different element names
     */
    public Map getIcons() {
        if (setup == null) {
            return null;
        }
        Node icons = getChildByName(setup, ICON_FILES_ELEMENT);
        if (icons == null) {
            return null;
        }
        NodeList iconList = icons.getChildNodes();
        Map result = new HashMap();
        for (int i = 0; i < iconList.getLength(); i++) {
            Element icon = (Element) iconList.item(i);
            result.put(icon.getAttribute(ELEMENT_ATTRIBUTE), icon.getAttribute(FILE_ATTRIBUTE));
        }
        return result;
    }

    /**
     * accessor to the data location
     * 
     * @return a String containing the data location
     */
    public String getDataLocation() {
        if (setup == null) {
            return null;
        }
        Node types = getChildByName(setup, RECORD_DATA_ELEMENT);
        if (types == null) {
            return null;
        }
        Node location = getChildByName(types, DATA_LOCATION_ELEMENT);
        if (location == null) {
            return null;
        }
        NamedNodeMap attrs = location.getAttributes();
        return attrs.getNamedItem(PATH_ATTRIBUTE).getNodeValue();
    }

    /**
     * returns the dtd for a given data type
     * 
     * @param type the type for which we'll return the dtd
     * @return a String containing the dtd location, relative to the data
     *         location
     */
    public String getDtdFromType(String type) {
        if (setup == null) {
            return null;
        }
        Element types = (Element) getChildByName(setup, RECORD_DATA_ELEMENT);
        if (types == null) {
            return null;
        }
        NodeList dataLocList = types.getElementsByTagName(DATA_LOCATION_ELEMENT);
        for (int i = 0; i < dataLocList.getLength(); i++) {
            Element dataLoc = (Element) dataLocList.item(i);
            NodeList typeNodes = dataLoc.getElementsByTagName(DATA_TYPE_ELEMENT);
            for (int j = 0; j < typeNodes.getLength(); j++) {
                Element typeNode = (Element) typeNodes.item(j);
                if (typeNode.getAttribute(TYPE_ATTRIBUTE).equals(type)) {
                    String dataLocation = dataLoc.getAttribute(PATH_ATTRIBUTE);
                    String fileName = typeNode.getAttribute(DTD_ATTRIBUTE);
                    if (dataLocation != null) {
                        if (!dataLocation.equals("")) {
                            if (dataLocation.substring(dataLocation.length() - 1) != System.getProperty("file.separator")) {
                                dataLocation = dataLocation + System.getProperty("file.separator");
                            }
                        }
                        return dataLocation + fileName;
                    } else {
                        return fileName;
                    }
                }
            }
        }
        return null;
    }

    /**
     * returns the root node name for a given data type
     * 
     * @param type the type for which we'll return the root node name
     * @return a String containing the root node name
     */
    public String getRootNodeFromType(String type) {
        if (setup == null) {
            return null;
        }
        Element types = (Element) getChildByName(setup, RECORD_DATA_ELEMENT);
        if (types == null) {
            return null;
        }
        NodeList dataLocList = types.getElementsByTagName(DATA_LOCATION_ELEMENT);
        for (int i = 0; i < dataLocList.getLength(); i++) {
            Element dataLoc = (Element) dataLocList.item(i);
            NodeList typeNodes = dataLoc.getElementsByTagName(DATA_TYPE_ELEMENT);
            for (int j = 0; j < typeNodes.getLength(); j++) {
                Element typeNode = (Element) typeNodes.item(j);
                if (typeNode.getAttribute(TYPE_ATTRIBUTE).equals(type)) {
                    return typeNode.getAttribute(ROOT_NODE_ATTRIBUTE);
                }
            }
        }
        return null;
    }

    /**
     * accessor to the data types
     * 
     * @return a Iterator containing data types
     */
    public Iterator getDataTypes() {
        Set result = this.internalGetDataTypes();
        if (result == null) {
            return null;
        }
        return result.iterator();
    }

    /**
     * accessor to the data types
     * 
     * @return a Set containing data types
     */
    private Set internalGetDataTypes() {
        if (setup == null) {
            return null;
        }
        Element types = (Element) getChildByName(setup, RECORD_DATA_ELEMENT);
        if (types == null) {
            return null;
        }
        Set result = new HashSet();
        boolean replicationProblem = false;
        Set replicatedTypes = new HashSet();
        NodeList dataLocList = types.getElementsByTagName(DATA_LOCATION_ELEMENT);
        for (int i = 0; i < dataLocList.getLength(); i++) {
            Element dataLoc = (Element) dataLocList.item(i);
            NodeList typeNodes = dataLoc.getElementsByTagName(DATA_TYPE_ELEMENT);
            for (int j = 0; j < typeNodes.getLength(); j++) {
                String newType = (((Element) typeNodes.item(j)).getAttribute(TYPE_ATTRIBUTE));
                if (result.contains(newType)) {
                    replicationProblem = true;
                    if (!replicatedTypes.contains(newType)) {
                        replicatedTypes.add(newType);
                    }
                } else {
                    result.add(newType);
                }
            }
        }
        if (replicationProblem) {
            String replicatedTypesString = "";
            boolean firstType = true;
            Iterator replicatedTypesIterator = replicatedTypes.iterator();
            while (replicatedTypesIterator.hasNext()) {
                if (!firstType) {
                    replicatedTypesString += ", ";
                } else {
                    firstType = false;
                }
                replicatedTypesString += (String) replicatedTypesIterator.next();
            }
            JOptionPane.showMessageDialog(editor, "The setup file contains twice or more some data" + " types, namely :\n" + replicatedTypesString + "\n" + "Only the first one of each will be considered.");
        }
        return result;
    }

    /**
     * forces the setup to be reloaded from file
     */
    public void reload() {
        setup = null;
        loadSetup();
    }

    /**
     * returns the setup file name
     */
    public String getSetupFileName() {
        return FULL_SETUP_FILE_NAME;
    }

    /**
     * says whether a given dtd allows references or not
     */
    public boolean allowsReferences(String dtd) {
        int index = dtd.lastIndexOf(FS);
        if (index < 0) index = 0;
        String dtdName = dtd.substring(index);
        if (setup == null) {
            return false;
        }
        Element misc = (Element) getChildByName(setup, MISC_ELEMENT);
        if (misc == null) {
            return false;
        }
        NodeList dtds = misc.getElementsByTagName(ALLOW_REFERENCES_ELEMENT);
        for (int i = 0; i < dtds.getLength(); i++) {
            if (((Element) dtds.item(i)).getAttribute(DTD_ATTRIBUTE).equals(dtdName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * updates a given setup for a new minor version number
     * 
     * @param mainVersion the main version number
     * @param minorVersion the minor version number of the current setup
     * @param newMinorVersion the new minor version number
     */
    private void updateSetup(int mainVersion, int minorVersion, int newMinorVersion) {
        if (setup == null) {
            throw new RuntimeException("No Setup present while updating it !");
        }
        outOfSwitch: switch(mainVersion) {
            case 1:
                switch(minorVersion) {
                    case 0:
                        if (newMinorVersion < 1) {
                            return;
                        }
                        ;
                    case 1:
                        if (newMinorVersion < 2) {
                            return;
                        }
                        ;
                    case 2:
                        if (newMinorVersion < 3) {
                            return;
                        }
                        ;
                        addMaxNbAttribute();
                        convertDataTypes();
                        updateDataTypes();
                        addAllowReferences();
                    default:
                        updateMinorVersionValue();
                }
                updateIcons();
                save();
                return;
        }
        JOptionPane.showMessageDialog(editor, "Failed to update your configuration from version " + MAJOR_VERSION_ATTRIBUTE + mainVersion + MINOR_VERSION_ATTRIBUTE + minorVersion + " to version " + MAJOR_VERSION_ATTRIBUTE + mainVersion + MINOR_VERSION_ATTRIBUTE + newMinorVersion + ".\nPlease report this problem to the LHCb team : " + "lhcb-support@lhcb-lb.cern.ch\n." + "Currently, you will use your old configuration file.");
    }

    /**
     * updates setup for major version value
     */
    private void updateMajorVersionValue() {
        Element versionNode = (Element) getChildByName(setup, VERSION_ELEMENT);
        if (versionNode == null) {
            throw new RuntimeException("No version while updating Setup !");
        }
        versionNode.setAttribute(MAJOR_VERSION_ATTRIBUTE, String.valueOf(CURRENT_MAJOR_VERSION));
    }

    /**
     * updates setup for minor version value
     */
    private void updateMinorVersionValue() {
        Element versionNode = (Element) getChildByName(setup, VERSION_ELEMENT);
        if (versionNode == null) {
            throw new RuntimeException("No version while updating Setup !");
        }
        versionNode.setAttribute(MINOR_VERSION_ATTRIBUTE, String.valueOf(CURRENT_MINOR_VERSION));
    }

    /**
     * updates the list of existing icons
     */
    private void updateIcons() {
        Node iconsNode = getChildByName(setup, ICON_FILES_ELEMENT);
        if (iconsNode == null) {
            throw new RuntimeException("No iconsNode while updating Setup !");
        }
        Map icons = this.getIcons();
        for (int i = 0; i < elements.length; i++) {
            if (icons.containsKey(elements[i])) {
                String file = (String) icons.get(elements[i]);
                if (!file.equals(files[i])) {
                    Element oldIcon = getElementByNameAttribute(iconsNode, elements[i]);
                    oldIcon.setAttribute(FILE_ATTRIBUTE, files[i]);
                }
            } else {
                Element newIcon = new ElementImpl((DocumentImpl) setup.getOwnerDocument(), ICON_FILE_ELEMENT);
                newIcon.setAttribute(ELEMENT_ATTRIBUTE, elements[i]);
                newIcon.setAttribute(FILE_ATTRIBUTE, files[i]);
                iconsNode.appendChild(newIcon);
            }
        }
    }

    /**
     * This adds an attribute MAX_NB to the OPEN_FILES node. this is used in the
     * update from version 1.2 to version 1.3
     */
    private void addMaxNbAttribute() {
        Element openFiles = (Element) getChildByName(setup, OPEN_FILES_ELEMENT);
        if (openFiles == null) {
            return;
        }
        openFiles.setAttribute(MAX_NB_ATTRIBUTE, String.valueOf(DEFAULT_MAX_NB_ATTRIBUTE));
    }

    /**
     * This converts the data types from defined in version 1.2 into version 1.3
     */
    private void convertDataTypes() {
        Element types = (Element) getChildByName(setup, RECORD_DATA_ELEMENT);
        if (types == null) {
            return;
        }
        Element dataLocation = (Element) getChildByName(types, DATA_LOCATION_ELEMENT);
        NodeList typeList = types.getElementsByTagName(DATA_TYPE_ELEMENT);
        for (int i = 0; i < typeList.getLength(); i++) {
            Node typeNode = typeList.item(i);
            types.removeChild(typeNode);
            dataLocation.appendChild(typeNode);
        }
    }

    /**
     * This updates the list of data types defined
     */
    private void updateDataTypes() {
        Element types = (Element) getChildByName(setup, RECORD_DATA_ELEMENT);
        if (types == null) {
            types = new ElementImpl((DocumentImpl) setup.getOwnerDocument(), RECORD_DATA_ELEMENT);
            setup.appendChild(types);
        }
        Map existingDataLocations = new HashMap();
        NodeList dataLocList = types.getElementsByTagName(DATA_LOCATION_ELEMENT);
        for (int i = 0; i < dataLocList.getLength(); i++) {
            existingDataLocations.put(((Element) dataLocList.item(i)).getAttribute(PATH_ATTRIBUTE), dataLocList.item(i));
        }
        Set existingTypes = this.internalGetDataTypes();
        for (int i = 0; i < data_paths.length; i++) {
            Element dataLocation = (Element) existingDataLocations.get(data_paths[i]);
            if (dataLocation == null) {
                dataLocation = new ElementImpl((DocumentImpl) setup.getOwnerDocument(), DATA_LOCATION_ELEMENT);
                dataLocation.setAttribute(PATH_ATTRIBUTE, data_paths[i]);
                types.appendChild(dataLocation);
            }
            for (int j = 0; j < data_types[i].length; j++) {
                if (!existingTypes.contains(data_types[i][j])) {
                    Element newType = new ElementImpl((DocumentImpl) setup.getOwnerDocument(), DATA_TYPE_ELEMENT);
                    newType.setAttribute(TYPE_ATTRIBUTE, data_types[i][j]);
                    newType.setAttribute(DTD_ATTRIBUTE, dtds[i][j]);
                    newType.setAttribute(ROOT_NODE_ATTRIBUTE, root_nodes[i][j]);
                    dataLocation.appendChild(newType);
                }
            }
        }
    }

    /**
     * This adds ALLOW_REFERENCES_ELEMENT nodes to the setup
     */
    private void addAllowReferences() {
        Element misc = new ElementImpl((DocumentImpl) setup.getOwnerDocument(), MISC_ELEMENT);
        setup.appendChild(misc);
        for (int i = 0; i < data_paths.length; i++) {
            for (int j = 0; j < data_types[i].length; j++) {
                if (allow_references[i][j]) {
                    Element allowRef = new ElementImpl((DocumentImpl) setup.getOwnerDocument(), ALLOW_REFERENCES_ELEMENT);
                    allowRef.setAttribute(DTD_ATTRIBUTE, dtds[i][j]);
                    misc.appendChild(allowRef);
                }
            }
        }
    }
}
