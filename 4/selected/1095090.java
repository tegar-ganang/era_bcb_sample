package org.sourcejammer.client.gui.conf;

import org.sourcejammer.util.AppConfig;
import java.io.*;
import org.w3c.dom.*;
import org.sourcejammer.xml.*;
import org.apache.xerces.dom.*;
import org.sourcejammer.project.view.Project;
import org.sourcejammer.project.view.NodeName;
import java.util.Stack;
import org.xml.sax.SAXException;
import org.sourcejammer.util.ConfigurationException;

/**
 * Title:        SourceJammer v 0.1.0
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:
 * @author Robert MacGrogan
 * @version $Revision: 1.3 $
 */
public class GuiConf {

    public static final String GUI_CONF_ROOT_NODE = "UserArchiveConfig";

    public static final String DEFAULT_DIRECTORIES_NODE = "DefaultDirectories";

    File mflGuiConf = null;

    Document mdocGuiConfXML = null;

    Element melmDefaultDirectories = null;

    public GuiConf(String archiveName, String userName) throws IOException {
        AppConfig oConf = AppConfig.getInstance();
        String sConfFileName = archiveName + "." + userName;
        mflGuiConf = new File(oConf.getConfigFilePath(), sConfFileName);
        if (mflGuiConf.exists()) {
            try {
                mdocGuiConfXML = XMLUtil.getXMLDoc(mflGuiConf);
                Element elmRoot = mdocGuiConfXML.getDocumentElement();
                try {
                    melmDefaultDirectories = XMLUtil.getChildElement(DEFAULT_DIRECTORIES_NODE, elmRoot);
                } catch (XMLNodeDoesNotExistException ex) {
                    melmDefaultDirectories = XMLUtil.addNewChildElement(DEFAULT_DIRECTORIES_NODE, elmRoot);
                    saveConfFile();
                }
            } catch (SAXException ex) {
                throw new ConfigurationException("Local configuration file for this archive and user is corrupted.");
            }
        } else {
            mdocGuiConfXML = new DocumentImpl();
            Element elmRoot = XMLUtil.addRootElement(GUI_CONF_ROOT_NODE, mdocGuiConfXML);
            melmDefaultDirectories = XMLUtil.addNewChildElement(DEFAULT_DIRECTORIES_NODE, elmRoot);
            saveConfFile();
        }
    }

    private synchronized void saveConfFile() throws IOException {
        XMLUtil.writeXMLDocToFileSys(mdocGuiConfXML, mflGuiConf);
    }

    public void setDefaultWorkingDirectory(NodeName nodeName, File directory) throws IOException, XMLNodeDoesNotExistException {
        if (directory == null) {
            throw new IOException("The directory File is null.");
        }
        if (!directory.isDirectory()) {
            throw new IOException(directory.getAbsolutePath() + " is not a directory.");
        }
        if (!directory.canWrite()) {
            throw new IOException("Cannot write to " + directory.getAbsolutePath() + ". Directory is read-only.");
        }
        if (!directory.exists()) {
            directory.mkdir();
        }
        Stack oNameStack = getInvertedNodeNameStack(nodeName);
        Element elmProjectElement = melmDefaultDirectories;
        oNameStack.pop();
        while (!oNameStack.isEmpty()) {
            String sProj = (String) oNameStack.pop();
            elmProjectElement = getConfElement(elmProjectElement, sProj);
        }
        XMLUtil.addOrReplaceElementText(elmProjectElement, directory.getAbsolutePath());
        saveConfFile();
    }

    public File getDefaultWorkingDirectory(NodeName oNodeName) throws IOException {
        File flDefaultDirectory = null;
        Stack oNameStack = getInvertedNodeNameStack(oNodeName);
        String sDirectory = null;
        Element elmProject = melmDefaultDirectories;
        oNameStack.pop();
        while (!oNameStack.isEmpty()) {
            String sProj = (String) oNameStack.pop();
            elmProject = getConfElement(elmProject, sProj);
            String sTemp = XMLUtil.getStringFromNode(elmProject, "");
            if (sTemp != null && !sTemp.equals("")) {
                sDirectory = sTemp;
            } else if (sDirectory != null && !sDirectory.equals("")) {
                sDirectory = sDirectory + File.separator + sProj;
            }
        }
        if (sDirectory != null && !sDirectory.equals("")) {
            flDefaultDirectory = new File(sDirectory.trim());
        }
        return flDefaultDirectory;
    }

    private Element getConfElement(Element parent, String name) throws IOException {
        Element elmReturn = null;
        try {
            elmReturn = XMLUtil.getChildElement(name, parent);
        } catch (XMLNodeDoesNotExistException ex) {
            elmReturn = XMLUtil.addNewChildElement(name, parent);
            saveConfFile();
        }
        return elmReturn;
    }

    private Stack getInvertedNodeNameStack(NodeName nodeName) {
        Stack oNameStack = new Stack();
        NodeName oParentName = nodeName;
        while (oParentName != null) {
            oNameStack.push(oParentName.getName());
            oParentName = oParentName.getParent();
        }
        return oNameStack;
    }
}
