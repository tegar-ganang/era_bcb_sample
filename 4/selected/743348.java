package SDClient.utilities;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.util.Set;
import java.util.Map;
import java.util.Date;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Calendar;
import java.text.DateFormat;
import java.text.ParseException;
import javax.swing.JOptionPane;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;
import org.apache.xml.serialize.XMLSerializer;
import org.apache.xml.serialize.OutputFormat;
import org.es.uma.XMLEditor.xerces.CustomDOMParser;
import SDClient.SDMainFrame;

/**
 * This class gets some default setup information from the setup.xml file
 * if present in the current path.
 * It then allows access to this information
 */
public class Setup {

    /**
     * the file separator in a platform dependant way
     */
    private static final String FS = System.getProperty("file.separator", "");

    /**
     * the cariage return in a platform dependant way
     */
    private static final String CR = System.getProperty("line.separator", "");

    /**
     * name of the xml file containing the setup
     */
    private static final String SETUP_FILE_NAME = ".xmleditorrc.xml";

    /**
     * full name of the xml file containing the setup
     */
    private static final String FULL_SETUP_FILE_NAME = System.getProperty("user.dir", "") + FS + SETUP_FILE_NAME;

    /**
     * name of the dtd file used by the setup
     */
    private static final String SETUP_DTD_NAME = ".xmleditorrc.dtd";

    /**
     * full name of the dtd file used by the setup
     */
    private static final String FULL_SETUP_DTD_NAME = System.getProperty("user.dir", "") + FS + SETUP_DTD_NAME;

    private static final String ROOT_ELEMENT = "SETUP";

    private static final String VERSION_ELEMENT = "VERSION";

    private static final String MAIN_VERSION_ATTRIBUTE = "v";

    private static final int CURRENT_MAIN_VERSION = 1;

    private static final String OPEN_FILES_ELEMENT = "OPEN_FILES";

    private static final String OPEN_FILE_ELEMENT = "OPEN_FILE";

    private static final String REFERENCE_ATTRIBUTE = "reference";

    private static final String LAST_OPEN_ATTRIBUTE = "last_open";

    private static final String ICON_FILES_ELEMENT = "ICON_FILES";

    private static final String ICON_FILE_ELEMENT = "ICON_FILE";

    private static final String ELEMENT_ATTRIBUTE = "name";

    private static final String FILE_ATTRIBUTE = "file";

    private static final String DETECTOR_DATA_ELEMENT = "DETECTOR_DATA";

    private static final String DATA_LOCATION_ELEMENT = "DATA_LOCATION";

    private static final String PATH_ATTRIBUTE = "path";

    private static final String DATA_TYPE_ELEMENT = "DATA_TYPE";

    private static final String TYPE_ATTRIBUTE = "type";

    private static final String DTD_ATTRIBUTE = "dtd";

    private static final String ROOT_NODE_ATTRIBUTE = "root_node";

    private static final String PASSWORD_ELEMENT = "PASSWORD";

    private static final String HASH_VALUE_ATTRIBUTE = "hashValue";

    /**
     * The list of elements for which icons are defined in the default setup.
     * Must be synchronized with files
     */
    private static final String[] elements = { "SOAD", "ValidFrom", "ValidUntil", "Version", "SOA_ID", "SOAD_Description", "AC_Declarations", "attribute", "attribute_Name", "attribute_Value", "AC_Relations", "SOA_Rule", "attribute_Set", "relation", "text", "#comment", "notindtd", "SETUP", "OPEN_FILES", "ICON_FILES", "DETECTOR_DATA", "DATA_LOCATION", "DATA_TYPE", "OPEN_FILE", "VERSION" };

    /**
     * The list of files where icons are defined in the default setup.
     * Must be synchronized with elements
     */
    private static final String[] files = { "element.gif", "attribute.gif", "attribute.gif", "attribute.gif", "element.gif", "element.gif", "element.gif", "attribute.gif", "element.gif", "element.gif", "element.gif", "element.gif", "element.gif", "element.gif", "text.gif", "comment.gif", "notindtd.gif", "element.gif", "element.gif", "element.gif", "element.gif", "comment.gif", "comment.gif", "text.gif", "attribute.gif" };

    /**
     * the path where to find data in the default setup.
     */
    private static final String data_path = "";

    private static final String[] data_types = { "" };

    /**
     * the dtds for each data type defined in the default setup.
     * Must be synchronized with data_types and root_nodes
     */
    private static final String[] dtds = { "" };

    /**
     * the root node names for each data types defined in the default setup.
     * Must be synchronized with dtds and data_types
     */
    private static final String[] root_nodes = { "" };

    /**
     * the maximal number of files already opened and put into the File menu
     */
    private static int MAX__ALREADY_OPEN_FILES_NB = 4;

    /**
     * the XmlEditor using this setup
     */
    private SDMainFrame editor;

    private SetupData data = new SetupData();

    /**
     * constructors. It tries to get and parse setup.xml
     * @param editor the XmlEditor using this setup
     */
    public Setup(SDMainFrame editor) {
        data.setup = null;
        data.serializer = null;
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
        if (!dtdFile.exists()) {
            try {
                dtdFile.createNewFile();
                FileWriter fileWriter = new FileWriter(dtdFile);
                fileWriter.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + CR);
                fileWriter.write("  <!--Setup root element-->" + CR);
                fileWriter.write("  <!ELEMENT " + ROOT_ELEMENT + " (" + VERSION_ELEMENT + ", " + OPEN_FILES_ELEMENT + ", " + ICON_FILES_ELEMENT + ", " + DETECTOR_DATA_ELEMENT + "," + PASSWORD_ELEMENT + ")>" + CR + CR);
                fileWriter.write("  <!-- defines the version of the setup file -->" + CR);
                fileWriter.write("  <!ELEMENT " + VERSION_ELEMENT + " EMPTY>" + CR);
                fileWriter.write("  <!-- the only attribute is the main version number -->" + CR);
                fileWriter.write("  <!ATTLIST " + VERSION_ELEMENT + " " + MAIN_VERSION_ATTRIBUTE + " CDATA #REQUIRED>" + CR);
                fileWriter.write("  <!-- defines a list of already opened files -->" + CR);
                fileWriter.write("  <!ELEMENT " + OPEN_FILES_ELEMENT + " (" + OPEN_FILE_ELEMENT + ")*>" + CR);
                fileWriter.write("  <!-- defines a file -->" + CR);
                fileWriter.write("  <!-- the data is a name given to the file -->" + CR);
                fileWriter.write("  <!ELEMENT " + OPEN_FILE_ELEMENT + " EMPTY>" + CR);
                fileWriter.write("  <!-- the first attribute is the real place where to find the file -->" + CR);
                fileWriter.write("  <!-- the second attribute is the date of the last access to this file -->" + CR);
                fileWriter.write("  <!ATTLIST " + OPEN_FILE_ELEMENT + " " + REFERENCE_ATTRIBUTE + " CDATA #REQUIRED " + LAST_OPEN_ATTRIBUTE + " CDATA #REQUIRED>" + CR + CR);
                fileWriter.write("  <!-- defines a list of icon files -->" + CR);
                fileWriter.write("  <!ELEMENT " + ICON_FILES_ELEMENT + " (" + ICON_FILE_ELEMENT + "*)>" + CR);
                fileWriter.write("  <!-- defines an icon file and the related element -->" + CR);
                fileWriter.write("  <!-- the data is the name of the element associated to the file -->" + CR);
                fileWriter.write("  <!ELEMENT " + ICON_FILE_ELEMENT + " EMPTY>" + CR);
                fileWriter.write("  <!-- the only attribute is the real place where to find the icon file -->" + CR);
                fileWriter.write("  <!ATTLIST " + ICON_FILE_ELEMENT + " " + ELEMENT_ATTRIBUTE + " CDATA #REQUIRED " + FILE_ATTRIBUTE + " CDATA #REQUIRED>" + CR + CR);
                fileWriter.write("  <!-- knowledge concerning the data used with this editor -->" + CR);
                fileWriter.write("  <!ELEMENT " + DETECTOR_DATA_ELEMENT + " (" + DATA_LOCATION_ELEMENT + ", " + DATA_TYPE_ELEMENT + "*)>" + CR);
                fileWriter.write("  <!-- the path to the root of the data -->" + CR);
                fileWriter.write("  <!ELEMENT " + DATA_LOCATION_ELEMENT + " EMPTY>" + CR);
                fileWriter.write("  <!-- the only attribute is the place where to find the data -->" + CR);
                fileWriter.write("  <!ATTLIST " + DATA_LOCATION_ELEMENT + " " + PATH_ATTRIBUTE + " CDATA #REQUIRED>" + CR);
                fileWriter.write("  <!-- a given type of data -->" + CR);
                fileWriter.write("  <!ELEMENT " + DATA_TYPE_ELEMENT + " EMPTY>" + CR);
                fileWriter.write("  <!-- the first attribute is the dtd file for this type, relative to the DATA_LOCATION -->" + CR);
                fileWriter.write("  <!-- the second attribute is the root element to use for this data type -->" + CR);
                fileWriter.write("  <!ATTLIST " + DATA_TYPE_ELEMENT + " " + TYPE_ATTRIBUTE + " CDATA #REQUIRED " + DTD_ATTRIBUTE + " CDATA #REQUIRED " + ROOT_NODE_ATTRIBUTE + " CDATA #REQUIRED>" + CR + CR);
                fileWriter.write("  <!ELEMENT " + PASSWORD_ELEMENT + " EMPTY>" + CR);
                fileWriter.write("  <!ATTLIST " + PASSWORD_ELEMENT + " " + HASH_VALUE_ATTRIBUTE + " CDATA #REQUIRED>" + CR + CR);
                fileWriter.flush();
                fileWriter.close();
            } catch (java.io.IOException e) {
                dtdFile.delete();
            }
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
                fileWriter.write("<" + VERSION_ELEMENT + " " + MAIN_VERSION_ATTRIBUTE + "=\"" + CURRENT_MAIN_VERSION + "\" />" + CR);
                fileWriter.write("    <" + OPEN_FILES_ELEMENT + ">" + CR);
                fileWriter.write("    </" + OPEN_FILES_ELEMENT + ">" + CR);
                fileWriter.write("    <" + ICON_FILES_ELEMENT + ">" + CR);
                for (int i = 0; i < files.length; i++) {
                    fileWriter.write("        <" + ICON_FILE_ELEMENT + " " + ELEMENT_ATTRIBUTE + "=\"" + elements[i] + "\" " + FILE_ATTRIBUTE + "=\"" + files[i] + "\"/>" + CR);
                }
                fileWriter.write("    </" + ICON_FILES_ELEMENT + ">" + CR);
                fileWriter.write("    <" + DETECTOR_DATA_ELEMENT + ">" + CR);
                fileWriter.write("    </" + DETECTOR_DATA_ELEMENT + ">" + CR);
                fileWriter.write("    <" + PASSWORD_ELEMENT + " " + HASH_VALUE_ATTRIBUTE + "=\"\"" + "/>" + CR);
                fileWriter.write("</" + ROOT_ELEMENT + ">" + CR);
                fileWriter.flush();
                fileWriter.close();
                loadSetup();
            } catch (java.io.IOException e) {
                setupFile.delete();
            }
        } else {
            loadSetup();
            if (this.getVersion() < CURRENT_MAIN_VERSION) {
                JOptionPane.showMessageDialog(editor, "Your current setup files are old versions.\n" + "You should remove them and restart from scratch\n" + "to get full custumization facilities of the editor.\n" + "Remember they are located in your home directory and\n" + "are called .xmleditorrc.xml and .xmleditorrc.dtd.\n\n" + "If for some reason you don't want to remove them (some\n" + "personnal configuration maybe ?), try to move them,\n" + "relaunch the editor and merge your data into the new ones.");
            }
        }
    }

    /**
     * loads the setup from the setup file
     */
    private void loadSetup() {
        CustomDOMParser parser = new CustomDOMParser();
        try {
            try {
                parser.setFeature("http://apache.org/xml/features/dom/include-ignorable-whitespace", false);
            } catch (org.xml.sax.SAXNotSupportedException e) {
            } catch (org.xml.sax.SAXNotRecognizedException e) {
            }
            File setupFile = new File(FULL_SETUP_FILE_NAME);
            parser.parse("File:///" + setupFile.getAbsolutePath());
            Document document = parser.getDocument();
            data.setup = document.getChildNodes().item(1);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(editor, "Unable to open the setup file.\n" + "You will get the default configuration.");
        }
    }

    /**
     * get the first child of node with name name
     * @param node the node concerned
     * @param name the name of the child we are looking for
     * @return Node - The child node
     */
    private Node getChildByName(Node node, String name) {
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeName().equals(name)) {
                return child;
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
            if (data.serializer != null) {
                data.serializer.reset();
                data.serializer.setOutputByteStream(outputStream);
            } else {
                OutputFormat outputFormat = new OutputFormat(data.setup.getOwnerDocument());
                outputFormat.setPreserveSpace(false);
                outputFormat.setIndenting(true);
                outputFormat.setLineWidth(0);
                data.serializer = new XMLSerializer(outputStream, outputFormat);
            }
            data.serializer.serialize(data.setup.getOwnerDocument());
            outputStream.close();
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }
    }

    /**
     * accessor to the version number
     * @return the version number
     */
    public int getVersion() {
        if (data.setup == null) {
            return -1;
        }
        Node version = getChildByName(data.setup, VERSION_ELEMENT);
        if (version == null) {
            return -1;
        }
        NamedNodeMap attrs = version.getAttributes();
        String result = attrs.getNamedItem(MAIN_VERSION_ATTRIBUTE).getNodeValue();
        try {
            return Integer.parseInt(result);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * accessor to the last open files
     * @return an array containing the name of the last open files
     */
    public String[] getLastOpenFiles() {
        int fileNumber = 0;
        if (data.setup == null) {
            return null;
        }
        Node openFiles = getChildByName(data.setup, OPEN_FILES_ELEMENT);
        if (openFiles == null) {
            return null;
        }
        NodeList files = openFiles.getChildNodes();
        String[] auxResult = new String[files.getLength()];
        for (int i = 0; i < files.getLength(); i++) {
            Node openFile = files.item(i);
            String filePath = openFile.getAttributes().getNamedItem("reference").getNodeValue();
            File aFile = new File(filePath);
            if (aFile.exists()) {
                auxResult[fileNumber] = files.item(i).getAttributes().getNamedItem(REFERENCE_ATTRIBUTE).getNodeValue();
                fileNumber++;
            } else {
                openFiles.removeChild(openFile);
            }
        }
        String[] result = new String[fileNumber];
        for (int j = 0; j < fileNumber; j++) {
            result[j] = auxResult[j];
        }
        save();
        return result;
    }

    /**
     * adds a file to the list of last open files
     * @param fileName the name of the file
     */
    public void addLastOpenFile(String fileName) {
        if (data.setup == null) {
            return;
        }
        Node openFiles = getChildByName(data.setup, OPEN_FILES_ELEMENT);
        if (openFiles == null) {
            return;
        }
        Node file = null;
        NodeList files = openFiles.getChildNodes();
        for (int i = 0; i < files.getLength(); i++) {
            if (files.item(i).getAttributes().getNamedItem(REFERENCE_ATTRIBUTE).getNodeValue().equals(fileName)) {
                file = files.item(i);
            }
        }
        Date today = Calendar.getInstance().getTime();
        if (file != null) {
            ((Element) file).setAttribute(LAST_OPEN_ATTRIBUTE, DateFormat.getDateTimeInstance().format(today));
        } else {
            if (openFiles.getChildNodes().getLength() == MAX__ALREADY_OPEN_FILES_NB) {
                file = null;
                Date date = today;
                for (int i = 0; i < files.getLength(); i++) {
                    try {
                        Date date2 = DateFormat.getDateTimeInstance().parse(files.item(i).getAttributes().getNamedItem(LAST_OPEN_ATTRIBUTE).getNodeValue());
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
     * @return a Map containing icon file names for different element names
     */
    public Map getIcons() {
        if (data.setup == null) {
            return null;
        }
        Node icons = getChildByName(data.setup, ICON_FILES_ELEMENT);
        if (icons == null) {
            return null;
        }
        NodeList iconList = icons.getChildNodes();
        Map result = new HashMap();
        for (int i = 0; i < iconList.getLength(); i++) {
            Node icon = iconList.item(i);
            NamedNodeMap attrs = icon.getAttributes();
            result.put(attrs.getNamedItem(ELEMENT_ATTRIBUTE).getNodeValue(), attrs.getNamedItem(FILE_ATTRIBUTE).getNodeValue());
        }
        return result;
    }

    /**
     * accessor to the data location
     * @return a String containing the data location
     */
    public String getDataLocation() {
        if (data.setup == null) {
            return null;
        }
        Node types = getChildByName(data.setup, DETECTOR_DATA_ELEMENT);
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
     * @param type the type for which we'll return the dtd
     * @return a String containing the dtd location, relative to the data location
     */
    public String getDtdFromType(String type) {
        if (data.setup == null) {
            return null;
        }
        Node types = getChildByName(data.setup, DETECTOR_DATA_ELEMENT);
        if (types == null) {
            return null;
        }
        NodeList typeList = types.getChildNodes();
        for (int i = 0; i < typeList.getLength(); i++) {
            Node typeNode = typeList.item(i);
            if (typeNode.getNodeName().equals(DATA_TYPE_ELEMENT)) {
                NamedNodeMap attrs = typeNode.getAttributes();
                if (attrs.getNamedItem(TYPE_ATTRIBUTE).getNodeValue().equals(type)) {
                    return attrs.getNamedItem(DTD_ATTRIBUTE).getNodeValue();
                }
            }
        }
        return null;
    }

    /**
     * returns the root node name for a given data type
     * @param type the type for which we'll return the root node name
     * @return a String containing the root node name
     */
    public String getRootNodeFromType(String type) {
        if (data.setup == null) {
            return null;
        }
        Node types = getChildByName(data.setup, DETECTOR_DATA_ELEMENT);
        if (types == null) {
            return null;
        }
        NodeList typeList = types.getChildNodes();
        for (int i = 1; i < typeList.getLength(); i++) {
            Node typeNode = typeList.item(i);
            if (typeNode.getNodeName().equals(DATA_TYPE_ELEMENT)) {
                NamedNodeMap attrs = typeNode.getAttributes();
                if (attrs.getNamedItem(TYPE_ATTRIBUTE).getNodeValue().equals(type)) {
                    return attrs.getNamedItem(ROOT_NODE_ATTRIBUTE).getNodeValue();
                }
            }
        }
        return null;
    }

    /**
     * accessor to the data types
     * @return a Iterator containing data types
     */
    public Iterator getDataTypes() {
        if (data.setup == null) {
            return null;
        }
        Node types = getChildByName(data.setup, DETECTOR_DATA_ELEMENT);
        if (types == null) {
            return null;
        }
        NodeList typeList = types.getChildNodes();
        Set result = new HashSet();
        for (int i = 0; i < typeList.getLength(); i++) {
            Node type = typeList.item(i);
            if (type.getNodeName().equals(DATA_TYPE_ELEMENT)) {
                NamedNodeMap attrs = type.getAttributes();
                result.add(attrs.getNamedItem(TYPE_ATTRIBUTE).getNodeValue());
            }
        }
        return result.iterator();
    }

    /**
     * forces the setup to be reloaded from file
     */
    public void reload() {
        data.setup = null;
        loadSetup();
    }

    /**
     * returns the setup file name
     * @return String - The full setup file name
     */
    public String getSetupFileName() {
        return FULL_SETUP_FILE_NAME;
    }

    /**
     * Obtiene la clave secreta almacenada en xmlEditor.xml
     * @return String - cadena con el hash de la clave secreta
     */
    public String getPasswordHash() {
        String passwd = null;
        if (data.setup == null) {
            return null;
        }
        Node passwordNode = getChildByName(data.setup, PASSWORD_ELEMENT);
        if (passwordNode == null) {
            return null;
        }
        System.out.println("holaaaaaaaaaaaaaaaaaaaaa");
        NamedNodeMap attrs = passwordNode.getAttributes();
        passwd = attrs.getNamedItem(HASH_VALUE_ATTRIBUTE).getNodeValue();
        if (passwd.equals("")) {
            return null;
        }
        return passwd;
    }

    /**
     * Establece el valor del hash de la clave secreta del usuario
     * @param passwd Hash de la clave secreta del usuario
     * @return boolean - True si todo ha ido bien. False en otro caso
     */
    public boolean setPasswordHash(String passwd) {
        if (data.setup == null) {
            return false;
        }
        Node passwordNode = getChildByName(data.setup, PASSWORD_ELEMENT);
        if (passwordNode == null) {
            Element newNode = data.setup.getOwnerDocument().createElement(PASSWORD_ELEMENT);
            newNode.setAttribute(HASH_VALUE_ATTRIBUTE, passwd);
            data.setup.appendChild(newNode);
        } else {
            ((Element) passwordNode).setAttribute(HASH_VALUE_ATTRIBUTE, passwd);
        }
        save();
        return true;
    }
}
