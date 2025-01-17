package fr.itris.glips.compiler.rtdb.file;

import org.w3c.dom.*;
import fr.itris.glips.compiler.rtdb.*;
import fr.itris.glips.rtda.toolkit.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;

/**
 * @author ITRIS, Jordi SUC
 */
public class CompiledXMLFile extends CompiledFile {

    protected static final String viewNodeName = "view", tagNodeName = "tag", alarmAckAttName = "alarm.ack", projectAttName = "project", locationAttName = "location";

    /**
	 * the set of all the used tag names in this xml file
	 */
    protected static Set<String> tagNames = new HashSet<String>();

    static {
        tagNames.add("tag.enumerated");
        tagNames.add("tag.float");
        tagNames.add("tag.integer");
        tagNames.add("tag.string");
    }

    /**
	 * the svg file specified in the xml root node
	 */
    protected String rootView = null;

    /**
     * the map associating a view element to a svg file
     */
    protected HashMap<Element, SVGFile> viewElementToSvgFiles = new HashMap<Element, SVGFile>();

    /**
     * the map associating the absolute xml path of a svg file to this svg file
     */
    protected HashMap<String, SVGFile> xmlPathToSvgFiles = new HashMap<String, SVGFile>();

    /**
     * the list of the tags that can be found in the xml file and that will be printed into the tags text file
     */
    protected LinkedList<String> tags = new LinkedList<String>();

    /**
     * the ip address of the server
     */
    protected String ipAddress = "";

    /**
     * the map associating the name of a destination data base file to a source data base file
     */
    protected Map<String, File> dataBaseFiles;

    /**
     * the constructor of the class
     * @param fileManager the file manager
     * @param path the path of the file
     * @param doc the document of this compiled file
     * @param rootView view the root view
     * @param dataBaseFiles 
     */
    public CompiledXMLFile(FileManager fileManager, String path, Document doc, String rootView, Map<String, File> dataBaseFiles) {
        super(fileManager, path, doc, XML_FILE);
        this.rootView = rootView;
        this.ipAddress = doc.getDocumentElement().getAttribute(serverAddressName);
        this.dataBaseFiles = dataBaseFiles;
        handleViewsAndRecordTags();
    }

    /**
     * @return the ip address of the server
     */
    public String getIpAddress() {
        return ipAddress;
    }

    @Override
    protected void writeFile() {
        super.writeFile();
        try {
            String tagListFilePath = file.toURI().toASCIIString();
            tagListFilePath = tagListFilePath.substring(0, tagListFilePath.lastIndexOf(FileManager.GLIPS_VIEW_EXTENSION)) + FileManager.TAG_LIST_FILE_EXTENSION;
            File tagListFile = new File(new URI(tagListFilePath));
            StringBuffer buffer = new StringBuffer("");
            for (String tagName : tags) {
                buffer.append(tagName + "\n");
            }
            ByteBuffer byteBuffer = ByteBuffer.wrap(buffer.toString().getBytes("UTF-8"));
            FileOutputStream out = new FileOutputStream(tagListFile);
            FileChannel channel = out.getChannel();
            channel.write(byteBuffer);
            channel.close();
        } catch (Exception ex) {
        }
        try {
            String parentPath = file.getParentFile().toURI().toASCIIString();
            if (!parentPath.endsWith("/")) {
                parentPath += "/";
            }
            File srcFile = null, destFile = null;
            byte[] tab = new byte[1000];
            int nb = 0;
            InputStream in = null;
            OutputStream out = null;
            for (String destinationName : dataBaseFiles.keySet()) {
                srcFile = dataBaseFiles.get(destinationName);
                if (srcFile != null) {
                    destFile = new File(new URI(parentPath + destinationName));
                    in = new BufferedInputStream(new FileInputStream(srcFile));
                    out = new BufferedOutputStream(new FileOutputStream(destFile));
                    while (in.available() > 0) {
                        nb = in.read(tab);
                        if (nb > 0) {
                            out.write(tab, 0, nb);
                        }
                    }
                    in.close();
                    out.flush();
                    out.close();
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * handles the view elements that can be found in this compiled xml file
     * and records all the tags that can be found in the xml file
     */
    protected void handleViewsAndRecordTags() {
        Node cur = null;
        String projectName = "", location = "", xmlPath = "";
        SVGFile svgFile = null;
        String tagPath = "";
        Element element = null;
        if (rootView == null || (rootView != null && rootView.equals(""))) {
            rootView = doc.getDocumentElement().getAttribute("defaultView");
        }
        for (NodeIterator it = new NodeIterator(doc.getDocumentElement()); it.hasNext(); ) {
            cur = it.next();
            if (cur != null && cur instanceof Element) {
                element = (Element) cur;
                if (cur.getNodeName().equals(viewNodeName)) {
                    projectName = element.getAttribute(projectAttName);
                    location = element.getAttribute(locationAttName);
                    xmlPath = TagToolkit.getPath((Element) cur);
                    element.removeAttribute(projectAttName);
                    if (projectName != null && !projectName.equals("") && location != null && !location.equals("") && xmlPath != null && !xmlPath.equals("")) {
                        svgFile = new SVGFile(fileManager, fileManager.getWorkspacePath(), projectName, location, xmlPath);
                        viewElementToSvgFiles.put(element, svgFile);
                        xmlPathToSvgFiles.put(xmlPath, svgFile);
                    }
                } else if (tagNames.contains(cur.getNodeName())) {
                    tagPath = TagToolkit.getPath(element);
                    if (tagPath != null && !tagPath.equals("")) {
                        tags.add(tagPath);
                    }
                }
            }
        }
    }

    /**
     * @return the map associating the absolute xml path of a svg file to this svg file
     */
    public HashMap<String, SVGFile> getSVGfiles() {
        return xmlPathToSvgFiles;
    }

    /**
     * adds the link items in this compiled xml file
     * and sets the path for the view nodes location attribute
     */
    public void handleLinkItems() {
        SVGFile svgFile = null;
        Set<String> usedTags = null;
        Element linkItem = null;
        int count = 0;
        for (Element view : viewElementToSvgFiles.keySet()) {
            if (view != null) {
                svgFile = viewElementToSvgFiles.get(view);
                if (svgFile != null) {
                    usedTags = svgFile.getUsedTagNames();
                    for (String tagName : usedTags) {
                        if (tagName != null && !tagName.equals("")) {
                            linkItem = view.getOwnerDocument().createElement("linkItem");
                            linkItem.setAttribute("tag", tagName);
                            linkItem.setAttribute("id", "id" + (count++));
                            view.appendChild(linkItem);
                        }
                    }
                }
            }
        }
    }

    /**
     * normalizes the xml document
     */
    public void normalizeXMLDocument() {
        NodeList elements = doc.getDocumentElement().getElementsByTagName("equipment.group");
        LinkedList<Element> elementsList = new LinkedList<Element>();
        Node cur = null;
        for (int i = 0; i < elements.getLength(); i++) {
            cur = elements.item(i);
            if (cur != null && cur instanceof Element) {
                elementsList.add((Element) cur);
            }
        }
        Element element = null;
        Node firstChild = null;
        for (int i = elementsList.size() - 1; i >= 0; i--) {
            element = elementsList.get(i);
            doc.getDocumentElement().removeChild(element);
            firstChild = doc.getDocumentElement().getFirstChild();
            if (firstChild != null) {
                doc.getDocumentElement().insertBefore(element, firstChild);
            } else {
                doc.getDocumentElement().appendChild(element);
            }
        }
    }

    /**
	 * @return Returns the rootViewFile.
	 */
    public String getRootView() {
        return rootView;
    }
}
