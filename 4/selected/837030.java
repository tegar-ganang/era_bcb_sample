package org.gridbus.broker.gui.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Image;
import java.util.HashMap;
import java.util.Map;
import javax.swing.DefaultCellEditor;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import org.gridbus.broker.constants.JobStatus;
import org.gridbus.broker.gui.action.XMLBeanAdlEditor;
import org.gridbus.broker.gui.action.XMLBeanCredentialEditor;
import org.gridbus.broker.gui.action.XMLBeanJsdlEditor;
import org.gridbus.broker.gui.action.XMLBeanXgrlEditor;
import org.gridbus.broker.gui.action.XMLBeanXpmlEditor;
import org.gridbus.broker.gui.model.AttributeTableModel;
import org.gridbus.broker.gui.model.DimensionEditorModel;
import org.gridbus.broker.gui.model.NodeAdapter;
import org.gridbus.broker.gui.view.CellEditableTable;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Xingchen Chu
 * @version 1.0
 * <code> BaseUtil </node>
 */
public final class BaseUtil extends Constants {

    public static final String IMAGE_BASE_DIR = Constants.getImageBaseDir();

    public static String[] getSupportedValue(Node node) {
        String nodeName = node != null ? node.getNodeName() : "";
        if (nodeName.equalsIgnoreCase("OperatingSystemName")) {
            return new String[] { "Unknown", "MACOS", "ATTUNIX", "DGUX", "DECNT", "Tru64_UNIX", "OpenVMS", "HPUX", "AIX", "MVS", "OS400", "OS_2", "JavaVM", "MSDOS", "WIN3x", "WIN95", "WIN98", "WINNT", "WINCE", "NCR3000", "NetWare", "OSF", "DC_OS", "Reliant_UNIX", "SCO_UnixWare", "SCO_OpenServer", "Sequent", "IRIX", "Solaris", "SunOS", "U6000", "ASERIES", "TandemNSK", "TandemNT", "BS2000", "LINUX", "Lynx", "XENIX", "VM", "Interactive_UNIX", "BSDUNIX", "FreeBSD", "NetBSD", "GNU_Hurd", "OS9", "MACH_Kernel", "Inferno", "QNX", "EPOC", "IxWorks", "VxWorks", "MiNT", "BeOS", "HP_MPE", "NextStep", "PalmPilot", "Rhapsody", "Windows_2000", "Dedicated", "OS_390", "VSE", "TPF", "Windows_R_Me", "Caldera_Open_UNIX", "OpenBSD", "Not_Applicable", "Windows_XP", "z_OS", "other" };
        } else if (nodeName.equalsIgnoreCase("CPUArchitectureName")) {
            return new String[] { "sparc", "powerpc", "x86", "x86_32", "x86_64", "parisc", "mips", "ia64", "arm", "other" };
        } else if (nodeName.equalsIgnoreCase("FileSystemType")) {
            return new String[] { "swap", "temporary", "spool", "normal" };
        } else if (nodeName.equalsIgnoreCase("CreationFlag")) {
            return new String[] { "overwrite", "append", "dontOverwrite" };
        }
        return new String[] {};
    }

    public static String[] getSupportedNodeNames(Node node) {
        String type = "";
        if (node != null && node.getOwnerDocument() != null) {
            type = node.getOwnerDocument().getFirstChild().getNodeName();
        }
        if (Constants.XPML.equalsIgnoreCase(type)) {
            return getSupportedNodeNamesForXpml(node);
        } else if (Constants.XGRL.equalsIgnoreCase(type)) {
            return getSupportedNodeNamesForXgrl(node);
        } else if (Constants.JSDL.equalsIgnoreCase(type)) {
            return getSupportedNodeNamesForJsdl(node);
        } else if (Constants.XCL.equalsIgnoreCase(type)) {
            return getSupportedNodeNamesForCredentials(node);
        } else if (Constants.ADL.equalsIgnoreCase(type)) {
            return getSupportedNodeNamesForAdl(node);
        } else {
            return new String[] {};
        }
    }

    private static String[] getSupportedNodeNamesForAdl(Node node) {
        String name = node.getNodeName();
        if (name != null) {
            if (name.equalsIgnoreCase(Constants.ADL)) {
                return new String[] { "JobDefinition" };
            } else {
                return getSupportedNodeNamesForJsdl(node);
            }
        }
        return new String[] {};
    }

    private static String[] getSupportedNodeNamesForCredentials(Node node) {
        String name = node.getNodeName();
        if (name != null) {
            if (name.equalsIgnoreCase(Constants.XCL)) {
                return new String[] { "mappings", "credentials" };
            } else if (name.equalsIgnoreCase("mappings")) {
                return new String[] { "mapping" };
            } else if (name.equalsIgnoreCase("credentials")) {
                return new String[] { "proxyCertificate", "auth", "keystore" };
            } else if (name.equalsIgnoreCase("proxyCertificate")) {
                return new String[] { "local", "myProxy", "srbGSI" };
            }
        }
        return new String[] {};
    }

    private static String[] getSupportedNodeNamesForJsdl(Node node) {
        String name = node.getNodeName();
        if (name != null) {
            if (name.equalsIgnoreCase(Constants.JSDL)) {
                return new String[] { "JobDescription" };
            } else if (name.equalsIgnoreCase("JobDescription")) {
                return new String[] { "JobIdentification", "Application", "Resources", "DataStaging" };
            } else if (name.equalsIgnoreCase("JobIdentification")) {
                return new String[] { "JobName", "Description", "JobAnnotation", "JobProject" };
            } else if (name.equalsIgnoreCase("Application")) {
                return new String[] { "ApplicationName", "ApplicationVersion", "Description" };
            } else if (name.equalsIgnoreCase("Resources")) {
                return new String[] { "CandidateHosts", "FileSystem", "ExclusiveExecution", "OperatingSystem", "CPUArchitecture", "IndividualCPUSpeed", "IndividualCPUCount", "IndividualCPUTime", "IndividualNetworkBandwith", "IndividualPhysicalMemory", "IndividualVirtualMemory", "IndividualDiskSpace", "TotalCPUTime", "TotalCPUCount", "TotalPhysicalMemory", "TotalVirtualMemory", "TotalDiskSpace", "TotalResourceCount" };
            } else if (name.equalsIgnoreCase("DataStaging")) {
                return new String[] { "FileName", "FilesystemName", "CreationFlag", "DeleteOnTermination", "Source", "Target" };
            } else if (name.equalsIgnoreCase("CandidateHosts")) {
                return new String[] { "HostName" };
            } else if (name.equalsIgnoreCase("FileSystem")) {
                return new String[] { "FileSystemType", "Description", "MountPoint", "DiskSpace" };
            } else if (name.equalsIgnoreCase("OperatingSystem")) {
                return new String[] { "OperatingSystemType", "OperatingSystemVersion", "Description" };
            } else if (name.equalsIgnoreCase("CPUArchitecture")) {
                return new String[] { "CPUArchitectureName" };
            } else if (name.equalsIgnoreCase("Source") || name.equalsIgnoreCase("Target")) {
                return new String[] { "URI" };
            } else if (name.equalsIgnoreCase("DiskSpace") || name.equalsIgnoreCase("IndividualCPUTime") || name.equalsIgnoreCase("IndividualCPUSpeed") || name.equalsIgnoreCase("IndividualCPUCount") || name.equalsIgnoreCase("IndividualPhysicalMemory") || name.equalsIgnoreCase("IndividualVirtualMemory") || name.equalsIgnoreCase("IndividualNetworkBandwidth") || name.equalsIgnoreCase("IndividualDiskSpace") || name.equalsIgnoreCase("TotalCPUTime") || name.equalsIgnoreCase("TotalCPUCount") || name.equalsIgnoreCase("TotalPhysicalMemory") || name.equalsIgnoreCase("TotalVirtualMemory") || name.equalsIgnoreCase("TotalDiskSpace") || name.equalsIgnoreCase("TotalResourceCount")) {
                return new String[] { "UpperBoundedRange", "LowerBoundedRange", "Exact", "Range" };
            } else if (name.equalsIgnoreCase("Range")) {
                return new String[] { "LowerBound", "UpperBound" };
            } else {
                return new String[] { Constants.TEXT_NODE };
            }
        } else {
            return new String[] {};
        }
    }

    private static String[] getSupportedNodeNamesForXpml(Node node) {
        String name = node.getNodeName();
        if (name != null) {
            if (name.equalsIgnoreCase(Constants.XPML)) {
                return new String[] { "description", "qos", "localInputDirectory", "parameter", "job-requirements", "pre-process", "task", "post-process" };
            } else if (name.equalsIgnoreCase("qos")) {
                return new String[] { "deadline", "budget", "optimisation" };
            } else if (name.equalsIgnoreCase("parameter")) {
                return new String[] { "single", "range", "enumeration", "file" };
            } else if (name.equalsIgnoreCase("pre-process") || name.equalsIgnoreCase("task") || name.equalsIgnoreCase("post-process")) {
                return new String[] { "copy", "execute", "substitute" };
            } else if (name.equalsIgnoreCase("enumeration")) {
                return new String[] { "list", "default" };
            } else if (name.equalsIgnoreCase("copy") || name.equalsIgnoreCase("substitute")) {
                return new String[] { "source", "destination" };
            } else if (name.equalsIgnoreCase("execute")) {
                return new String[] { "command", "arg" };
            } else if (name.equalsIgnoreCase("description")) {
                return new String[] { Constants.TEXT_NODE };
            } else if (name.equalsIgnoreCase("job-requirements")) {
                return new String[] { "property" };
            } else {
                return new String[] {};
            }
        } else {
            return new String[] {};
        }
    }

    private static String[] getSupportedNodeNamesForXgrl(Node node) {
        String name = "";
        String parentName = "";
        if (node != null && node.getParentNode() != null) {
            name = node.getNodeName();
            parentName = node.getParentNode().getNodeName();
        }
        if (name != null) {
            if (name.equalsIgnoreCase(Constants.XGRL)) {
                return new String[] { "service" };
            } else if (name.equalsIgnoreCase("service")) {
                return new String[] { "compute", "data", "information", "application" };
            } else if (name.equalsIgnoreCase("compute")) {
                return new String[] { "globus", "unicore", "alchemi", "xgrid", "pbs", "sge", "fork", "condor" };
            } else if ((name.equalsIgnoreCase("globus") || name.equalsIgnoreCase("pbs") || name.equalsIgnoreCase("sge"))) {
                return new String[] { "queue" };
            } else if (name.equalsIgnoreCase("information")) {
                return new String[] { "replicaCatalog", "srbMCAT", "networkWeatherService" };
            } else {
                return new String[] {};
            }
        } else {
            return new String[] {};
        }
    }

    private static Map getSupportedAttributesForXgrl(String name, Node parent) {
        Map result = new HashMap();
        if (name != null && parent != null) {
            String parentName = parent.getNodeName();
            if ("service".equalsIgnoreCase(name)) {
                result.put("type", new String[] { "compute", "data", "information", "application" });
                result.put("cost", new String[] { "1.0" });
                result.put("mappingID", new String[] { "" });
                result.put("mappingID", new String[] { "" });
            } else if ("compute".equalsIgnoreCase(name)) {
                result.put("middleware", new String[] { "globus", "unicore", "alchemi", "xgrid", "sge", "pbs", "fork", "condor" });
                result.put("firewall", new String[] { "false", "true" });
            } else if ("xgrid".equalsIgnoreCase(name)) {
                result.put("controller", new String[] { "default" });
                result.put("version", new String[] { "tp2" });
            } else if ("queue".equalsIgnoreCase(name)) {
                result.put("name", new String[] { "queue1" });
                result.put("cost", new String[] { "", "0" });
                result.put("priority", new String[] { "", "5" });
                result.put("limit", new String[] { "", "50" });
            } else if ("globus".equalsIgnoreCase(name)) {
                result.put("hostname", new String[] { "127.0.0.1" });
                result.put("jobmanager", new String[] { "", "jobmanager-fork", "jobmanager-sge", "jobmanager-pbs", "jobmanager-condor" });
                result.put("version", new String[] { "", "2.4", "3.2", "4.0" });
                result.put("service", new String[] { "" });
                result.put("fileStagingURL", new String[] { "http://localhost:10000//tmp" });
                result.put("configuration", new String[] { "" });
            } else if ("unicore".equalsIgnoreCase(name)) {
                result.put("gatewayURL", new String[] { "http://localhost" });
                result.put("version", new String[] { "4.1" });
            } else if ("alchemi".equalsIgnoreCase(name)) {
                result.put("managerURL", new String[] { "http://localhost" });
                result.put("version", new String[] { "1.0" });
            } else if ("pbs".equalsIgnoreCase(name)) {
                result.put("hostname", new String[] { "localhost" });
                result.put("version", new String[] { "2.3" });
            } else if ("condor".equalsIgnoreCase(name)) {
                result.put("hostname", new String[] { "localhost" });
                result.put("version", new String[] { "6.7" });
            } else if ("sge".equalsIgnoreCase(name)) {
                result.put("hostname", new String[] { "localhost" });
                result.put("version", new String[] {});
            } else if ("fork".equalsIgnoreCase(name)) {
                result.put("hostname", new String[] { "localhost" });
            } else if ("information".equalsIgnoreCase(name)) {
                result.put("type", new String[] { "replicaCatalog", "srbMCAT", "networkStatus" });
            } else if ("replicaCatalog".equalsIgnoreCase(name)) {
                result.put("replicaHost", new String[] { "http://localhost" });
                result.put("replicaTop", new String[] { "default" });
            } else if ("srbMCAT".equalsIgnoreCase(name)) {
                result.put("host", new String[] { "http://localhost" });
                result.put("port", new String[] { "6833" });
                result.put("domain", new String[] { "localhost" });
                result.put("home", new String[] { "home" });
                result.put("defaultResource", new String[] { "default" });
                result.put("authSchema", new String[] { "GSI_AUTH", "ENCRYPT1" });
                result.put("serverDN", new String[] {});
            } else if ("networkWeatherService".equalsIgnoreCase(name)) {
                result.put("nameServer", new String[] { "http://localhost" });
                result.put("port", new String[] { "10000" });
            } else if ("data".equalsIgnoreCase(name)) {
                result.put("type", new String[] { "gridftp", "srb" });
                result.put("hostname", new String[] { "http://localhost" });
                result.put("accessMode", new String[] { "read-only", "write-only", "read-wrtie" });
            } else if ("application".equalsIgnoreCase(name)) {
                result.put("url", new String[] { "http://localhost" });
            }
        }
        return result;
    }

    public static Map getSupportedAttributes(String name, Node parent) {
        String type = "";
        if (parent.getOwnerDocument() != null) {
            type = parent.getOwnerDocument().getFirstChild().getNodeName();
        }
        if (Constants.XPML.equalsIgnoreCase(type)) {
            return getSupportedAttributesForXpml(name, parent);
        } else if (Constants.XGRL.equalsIgnoreCase(type)) {
            return getSupportedAttributesForXgrl(name, parent);
        } else if (Constants.JSDL.equalsIgnoreCase(type)) {
            return getSupportedAttributesForJsdl(name, parent);
        } else if (Constants.XCL.equalsIgnoreCase(type)) {
            return getSupportedAttributesForCredential(name, parent);
        } else if (Constants.ADL.equalsIgnoreCase(type)) {
            return getSupportedAttributesForAdl(name, parent);
        } else {
            return new HashMap();
        }
    }

    private static Map getSupportedAttributesForAdl(String name, Node parent) {
        return getSupportedAttributesForJsdl(name, parent);
    }

    private static Map getSupportedAttributesForJsdl(String name, Node parent) {
        Map result = new HashMap();
        if (name != null && parent != null) {
            String parentName = parent.getNodeName();
            if ("JobDefinition".equalsIgnoreCase(name)) {
                result.put("id", new String[] { "" });
            } else if ("FileSystem".equalsIgnoreCase(name)) {
                result.put("name", new String[] { "default" });
            } else if ("DataStaging".equalsIgnoreCase(name)) {
                result.put("name", new String[] { "" });
            } else if ("UpperBound".equalsIgnoreCase(name) || "LowerBound".equalsIgnoreCase(name) || "UpperBoundedRange".equalsIgnoreCase(name) || "LowerBoundedRange".equalsIgnoreCase(name)) {
                result.put("exclusiveBound", new String[] { "", "true", "false" });
            } else if ("Exact".equalsIgnoreCase(name)) {
                result.put("epsilon", new String[] { "", "0,0" });
            }
        }
        return result;
    }

    private static Map getSupportedAttributesForXpml(String name, Node parent) {
        Map result = new HashMap();
        if (name != null && parent != null) {
            String parentName = parent.getNodeName();
            if ("parameter".equalsIgnoreCase(name)) {
                result.put("name", new String[] { "" });
                result.put("type", new String[] { "integer", "float", "string", "gridfile" });
                result.put("domain", new String[] { "range", "single", "file", "enumeration" });
            } else if ("range".equalsIgnoreCase(name)) {
                result.put("from", new String[] { "0" });
                result.put("to", new String[] { "1" });
                result.put("type", new String[] { "step", "points" });
                result.put("interval", new String[] { "1" });
            } else if ("file".equalsIgnoreCase(name)) {
                result.put("url", new String[] { "default" });
                result.put("protocol", new String[] { "lfn", "srb" });
            } else if ("execute".equalsIgnoreCase(name)) {
                result.put("remoteAccess", new String[] { "false", "true" });
            } else if ("substitute".equalsIgnoreCase(parentName) && ("source".equalsIgnoreCase(name) || "destination".equalsIgnoreCase(name))) {
                result.put("file", new String[] { "default.dat" });
            } else if (parentName.indexOf("copy") >= 0 && ("source".equalsIgnoreCase(name) || "destination".equalsIgnoreCase(name))) {
                result.put("location", new String[] { "local", "remote", "node" });
                result.put("file", new String[] { "default.dat" });
            } else if ("command".equalsIgnoreCase(name) || "arg".equalsIgnoreCase(name) || "optimisation".equalsIgnoreCase(name) || "localInputDirectory".equalsIgnoreCase(name) || "single".equalsIgnoreCase(name) || "list".equalsIgnoreCase(name) || "default".equalsIgnoreCase(name)) {
                result.put("value", new String[] { "default" });
            } else if ("optimisation".equalsIgnoreCase(name)) {
                result.put("value", new String[] { "COST", "TIME", "COST_TIME", "DATA", "COST_DATA", "TIME_DATA", "NONE" });
            } else if ("deadline".equalsIgnoreCase(name) || "budget".equalsIgnoreCase(name)) {
                result.put("value", new String[] { " " });
            }
        }
        return result;
    }

    private static Map getSupportedAttributesForCredential(String name, Node parent) {
        Map result = new HashMap();
        if (name != null && parent != null) {
            String parentName = parent.getNodeName();
            if ("credentials".equalsIgnoreCase(name)) {
                result.put("type", new String[] { "proxyCertificate", "auth", "keystore" });
                result.put("id", new String[] { "id1" });
            } else if ("proxyCertificate".equalsIgnoreCase(name)) {
                result.put("source", new String[] { "local", "myProxy", "srbGSI" });
            } else if ("auth".equalsIgnoreCase(name)) {
                result.put("username", new String[] { "guest" });
                result.put("password", new String[] { "" });
            } else if ("keystore".equalsIgnoreCase(name)) {
                result.put("file", new String[] { "default.txt" });
                result.put("password", new String[] { " " });
            } else if ("myProxy".equalsIgnoreCase(name)) {
                result.put("host", new String[] { "localhost" });
                result.put("port", new String[] { "7512" });
                result.put("username", new String[] { "guest" });
                result.put("password", new String[] { " " });
            } else if ("local".equalsIgnoreCase(name) && "proxyCertificate".equalsIgnoreCase(parentName)) {
                result.put("usercertfile", new String[] { "" });
                result.put("userkeyfile", new String[] { "" });
                result.put("password", new String[] { " " });
            } else if ("srbGSI".equalsIgnoreCase(name)) {
                result.put("username", new String[] { "guest" });
                result.put("proxyPassword", new String[] { "" });
                result.put("usercertfile", new String[] { "" });
                result.put("usercertkey", new String[] { "" });
                result.put("proxyfile", new String[] { "" });
                result.put("ca", new String[] { "" });
            } else if ("mapping".equalsIgnoreCase(name)) {
                result.put("credentialID", new String[] { " " });
                result.put("serviceID", new String[] { " " });
            }
        }
        return result;
    }

    public static void customizeCellEditor(CellEditableTable table, String nodeName, Node parent, int colIndex) {
        Map attrsMap = BaseUtil.getSupportedAttributes(nodeName, parent);
        Object[] names = attrsMap.keySet().toArray();
        for (int i = 0; i < names.length; i++) {
            int rowIndex = ((AttributeTableModel) table.getModel()).getRowIndexForName(names[i]);
            String[] values = (String[]) attrsMap.get(names[i]);
            setTableEditor(table, values, rowIndex, colIndex);
        }
    }

    public static ImageIcon createIcon(String dir, String filename, String type) {
        try {
            return new ImageIcon(new java.net.URL(dir + "/" + filename + "." + type));
        } catch (Exception e) {
        }
        return null;
    }

    private static void setTableEditor(CellEditableTable table, Object[] data, int row, int col) {
        DimensionEditorModel model = table.getDimensionEditorModel();
        if (model == null) {
            model = new DimensionEditorModel();
            table.setDimensionEditorModel(model);
        }
        JComboBox cb = new JComboBox(data);
        if (data.length <= 1) {
            cb.setEditable(true);
        }
        model.addEditor(row, col, new DefaultCellEditor(cb));
    }

    public static void setTableEditor(CellEditableTable table, JComboBox box, int row, int col) {
        DimensionEditorModel model = table.getDimensionEditorModel();
        if (model == null) {
            model = new DimensionEditorModel();
            table.setDimensionEditorModel(model);
        }
        model.addEditor(row, col, new DefaultCellEditor(box));
    }

    public static int expand(javax.swing.JTree xmlTree, Node expect) {
        if (expect != null && !"xpml".equalsIgnoreCase(expect.getNodeName())) {
            int previous = expand(xmlTree, expect.getParentNode());
            int index = xmlTree.getModel().getIndexOfChild(new NodeAdapter(expect.getParentNode()), new NodeAdapter(expect)) + 1;
            int rowToExpand = previous + index;
            xmlTree.expandRow(rowToExpand);
            return index + previous;
        } else if (expect != null && "xpml".equalsIgnoreCase(expect.getNodeName())) {
            xmlTree.expandRow(0);
        }
        return 0;
    }

    public static Node selectSchema(String name) {
        if (name.equalsIgnoreCase("xpml")) {
            return new XMLBeanXpmlEditor().newDocument();
        } else if (name.equalsIgnoreCase("xgrl")) {
            return new XMLBeanXgrlEditor().newDocument();
        } else if (name.equalsIgnoreCase("credential")) {
            return new XMLBeanCredentialEditor().newDocument();
        } else if (name.equalsIgnoreCase("jsdl")) {
            return new XMLBeanJsdlEditor().newDocument();
        } else if (name.equalsIgnoreCase("adl")) {
            return new XMLBeanAdlEditor().newDocument();
        } else {
            throw new RuntimeException("internal error : unsupported schema '" + name + "'");
        }
    }

    public static String dumpXml(Node root, int indent) {
        if (root == null) {
            return "";
        }
        String content = "";
        String name = root.getNodeName();
        for (int i = 0; i < indent; i++) {
            content += " ";
        }
        content += "<" + name;
        NamedNodeMap attrs = root.getAttributes();
        if (attrs.getLength() == 0 && root.getParentNode() != null && root.getParentNode().getNodeType() == Node.DOCUMENT_NODE) {
            String namespace = root.getNamespaceURI();
            if (namespace != null && !"".equalsIgnoreCase(namespace)) {
                content += " xmlns=\"" + root.getNamespaceURI() + "\"";
            }
        }
        for (int i = 0; i < attrs.getLength(); i++) {
            Node attr = attrs.item(i);
            content += " " + attr.getNodeName();
            content += "=\"" + attr.getNodeValue() + "\"";
        }
        content += ">";
        NodeList children = root.getChildNodes();
        boolean isText = false;
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            int type = child.getNodeType();
            if (type == Node.ELEMENT_NODE) {
                content += "\r\n";
                content += dumpXml(child, indent + 3);
            } else if (type == Node.TEXT_NODE) {
                content += child.getNodeValue().trim();
                isText = true;
            }
        }
        if (children.getLength() != 0 && !isText) {
            content += "\r\n";
            for (int i = 0; i < indent; i++) {
                content += " ";
            }
        }
        content += "</" + root.getNodeName() + ">";
        return content;
    }

    public static String showChoiceDialog(Component root, String title, String message, Icon image, Object[] alternatives) {
        String name = (String) JOptionPane.showInputDialog(root, message, title, JOptionPane.PLAIN_MESSAGE, image, alternatives, "");
        return name;
    }

    public static Color statusColor(int status) {
        switch(status) {
            case JobStatus.SCHEDULED:
                return Color.ORANGE;
            case JobStatus.STAGE_IN:
                return Color.GRAY;
            case JobStatus.STAGE_OUT:
                return Color.MAGENTA;
            case JobStatus.READY:
                return Color.YELLOW;
            case JobStatus.SUBMITTED:
                return Color.BLUE;
            case JobStatus.ACTIVE:
                return Color.BLUE;
            case JobStatus.DONE:
                return Color.GREEN;
            case JobStatus.FAILED:
                return Color.RED;
            default:
                return Color.CYAN;
        }
    }

    public static String statusChar(int status) {
        switch(status) {
            case JobStatus.SCHEDULED:
                return "S";
            case JobStatus.STAGE_IN:
                return "I";
            case JobStatus.STAGE_OUT:
                return "O";
            case JobStatus.READY:
                return "W";
            case JobStatus.SUBMITTED:
                return "R";
            case JobStatus.ACTIVE:
                return "R";
            case JobStatus.DONE:
                return "D";
            case JobStatus.FAILED:
                return "F";
            default:
                return "U";
        }
    }

    public static Image getLogo() {
        return createIcon(Constants.getImageBaseDir(), "logo", "gif").getImage();
    }
}
