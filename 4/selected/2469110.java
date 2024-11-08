package com.dgtalize.netc.domain;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.Enumeration;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import com.dgtalize.netc.visual.WindowUI;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author DGtalize
 */
public class UserTreeModel extends DefaultTreeModel {

    public UserTreeModel() {
        super(new DefaultMutableTreeNode("root"));
        UserGroupTreeNode noGroupNode = new UserGroupTreeNode("(" + WindowUI.getBoundleTextUI("noGroup") + ")", true);
        getRoot().add(noGroupNode);
        addTreeModelListener(new TreeModelListener() {

            public void treeNodesChanged(TreeModelEvent e) {
            }

            public void treeNodesInserted(TreeModelEvent e) {
            }

            public void treeNodesRemoved(TreeModelEvent e) {
            }

            public void treeStructureChanged(TreeModelEvent e) {
            }
        });
    }

    public UserGroupTreeNode getNoGroupNode() {
        DefaultMutableTreeNode rootNode = getRoot();
        for (int i = 0; i < rootNode.getChildCount(); i++) {
            UserGroupTreeNode childNode = (UserGroupTreeNode) rootNode.getChildAt(i);
            if (childNode.isNoGroupNode()) {
                return childNode;
            }
        }
        return null;
    }

    public UserTreeNode findUserNode(NetCUser user) {
        return findUserNode(user.getIpaddr());
    }

    public UserTreeNode findUserNode(InetAddress userAddr) {
        UserTreeNode userNode = null;
        DefaultMutableTreeNode rootNode = getRoot();
        for (Enumeration<DefaultMutableTreeNode> enRoot = rootNode.children(); enRoot.hasMoreElements(); ) {
            DefaultMutableTreeNode enGroupNode = enRoot.nextElement();
            for (Enumeration<UserTreeNode> enGroup = enGroupNode.children(); enGroup.hasMoreElements(); ) {
                UserTreeNode enUserNode = enGroup.nextElement();
                NetCUser userIter = enUserNode.getUserObject();
                if (userIter.getIpaddr().equals(userAddr)) {
                    userNode = enUserNode;
                    break;
                }
            }
            if (userNode != null) {
                break;
            }
        }
        return userNode;
    }

    public UserGroupTreeNode findGroupNode(String name) {
        UserGroupTreeNode groupNode = null;
        DefaultMutableTreeNode rootNode = getRoot();
        for (Enumeration<UserGroupTreeNode> enRoot = rootNode.children(); enRoot.hasMoreElements(); ) {
            UserGroupTreeNode enGroupNode = enRoot.nextElement();
            if (enGroupNode.getName().equals(name)) {
                groupNode = enGroupNode;
            }
        }
        return groupNode;
    }

    public UserGroupTreeNode findGroupNodeHolds(String userAddr) {
        UserGroupTreeNode groupNode = null;
        DefaultMutableTreeNode rootNode = getRoot();
        for (Enumeration<UserGroupTreeNode> enRoot = rootNode.children(); enRoot.hasMoreElements(); ) {
            UserGroupTreeNode enGroupNode = enRoot.nextElement();
            if (enGroupNode.getUserAddrs().contains(userAddr)) {
                groupNode = enGroupNode;
            }
        }
        return groupNode;
    }

    public UserTreeNode updateUser(NetCUser userToUpdate) {
        UserTreeNode userNode = null;
        userNode = findUserNode(userToUpdate);
        if (userNode == null) {
            userNode = new UserTreeNode(userToUpdate, false);
            UserGroupTreeNode parentGroupNode = null;
            parentGroupNode = findGroupNodeHolds(userToUpdate.getIpaddr().getHostAddress());
            if (parentGroupNode == null) {
                parentGroupNode = getNoGroupNode();
            }
            insertNodeInto(userNode, parentGroupNode, parentGroupNode.getChildCount());
        } else {
            userNode.setUserObject(userToUpdate);
            nodeChanged(userNode);
        }
        return userNode;
    }

    public void removeUser(NetCUser userToRemove) {
        UserTreeNode userNode = findUserNode(userToRemove);
        if (userNode != null) {
            removeNodeFromParent(userNode);
        }
    }

    public UserGroupTreeNode addGroup(String name) {
        UserGroupTreeNode groupNode = new UserGroupTreeNode(name);
        insertNodeInto(groupNode, getRoot(), getRoot().getChildCount());
        return groupNode;
    }

    public void removeGroup(String name) throws Exception {
        removeGroup(findGroupNode(name));
    }

    public void removeGroup(UserGroupTreeNode groupNode) throws Exception {
        if (groupNode.isNoGroupNode()) {
            throw new Exception(java.util.ResourceBundle.getBundle("com/dgtalize/netc/business/MessagesUI").getString("delNoGroup"));
        } else {
            for (Enumeration<UserTreeNode> enGroup = groupNode.children(); enGroup.hasMoreElements(); ) {
                UserTreeNode enUserNode = enGroup.nextElement();
                insertNodeInto(enUserNode, getNoGroupNode(), getNoGroupNode().getChildCount());
            }
            removeNodeFromParent(groupNode);
        }
    }

    public void moveUserToGroup(UserTreeNode userNode, String groupName) {
        UserGroupTreeNode groupToMoveTo = null;
        groupToMoveTo = findGroupNode(groupName);
        if (groupToMoveTo == null || groupToMoveTo.equals(userNode.getParent())) {
            return;
        }
        String userAddr = ((NetCUser) userNode.getUserObject()).getIpaddr().getHostAddress();
        ((UserGroupTreeNode) userNode.getParent()).getUserAddrs().remove(userAddr);
        if (!groupToMoveTo.isNoGroupNode()) {
            groupToMoveTo.getUserAddrs().add(userAddr);
        }
        insertNodeInto(userNode, groupToMoveTo, groupToMoveTo.getChildCount());
    }

    public void saveStructureToXML(String xmlFilePath) throws Exception {
        Document structDoc = getStructureDocument(xmlFilePath);
        Element rootGroupsNode = structDoc.getDocumentElement();
        NodeList groupNodes = rootGroupsNode.getChildNodes();
        int nodesCout = groupNodes.getLength();
        for (int i = 0; i < nodesCout; i++) {
            rootGroupsNode.removeChild(groupNodes.item(0));
        }
        DefaultMutableTreeNode rootNode = getRoot();
        for (Enumeration<UserGroupTreeNode> enRoot = rootNode.children(); enRoot.hasMoreElements(); ) {
            UserGroupTreeNode enGroupNode = enRoot.nextElement();
            if (!enGroupNode.isNoGroupNode()) {
                Node groupNode = structDoc.createElement("group");
                Node groupNameNode = structDoc.createElement("name");
                groupNameNode.appendChild(structDoc.createTextNode(enGroupNode.getName()));
                groupNode.appendChild(groupNameNode);
                rootGroupsNode.appendChild(groupNode);
                Node usersNode = structDoc.createElement("users");
                groupNode.appendChild(usersNode);
                for (Enumeration<UserTreeNode> enGroup = enGroupNode.children(); enGroup.hasMoreElements(); ) {
                    UserTreeNode enUserNode = enGroup.nextElement();
                    NetCUser userIter = enUserNode.getUserObject();
                    Node userNode = structDoc.createElement("user");
                    userNode.appendChild(structDoc.createTextNode(userIter.getIpaddr().getHostAddress()));
                    usersNode.appendChild(userNode);
                }
            }
        }
        TransformerFactory tFactory = TransformerFactory.newInstance();
        Transformer transformer = tFactory.newTransformer();
        DOMSource source = new DOMSource(structDoc);
        StreamResult result = new StreamResult(new File(xmlFilePath));
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.transform(source, result);
    }

    public void loadStructureFromXML(String xmlFilePath) throws Exception {
        Document structDoc = getStructureDocument(xmlFilePath);
        Element rootGroupsNode = structDoc.getDocumentElement();
        NodeList groupNodes = rootGroupsNode.getChildNodes();
        for (int i = 0; i < groupNodes.getLength(); i++) {
            Node groupNode = groupNodes.item(i);
            if (groupNode.getNodeType() == Node.ELEMENT_NODE) {
                Node groupNameNode = ((Element) groupNode).getElementsByTagName("name").item(0);
                UserGroupTreeNode groupTreeNode = addGroup(groupNameNode.getTextContent());
                NodeList userNodes = ((Element) groupNode).getElementsByTagName("users").item(0).getChildNodes();
                for (int j = 0; j < userNodes.getLength(); j++) {
                    Node userNode = userNodes.item(j);
                    groupTreeNode.getUserAddrs().add(userNode.getTextContent());
                }
            }
        }
    }

    private Document getStructureDocument(String filePath) throws Exception {
        Document configDoc = null;
        File cfgFile = new File(filePath);
        if (!cfgFile.exists()) {
            cfgFile.createNewFile();
            try {
                InputStream fis = getClass().getResourceAsStream("/com/dgtalize/netc/visual/userList.xml");
                FileOutputStream fos = new FileOutputStream(cfgFile);
                byte[] buf = new byte[1024];
                int readCant = 0;
                while ((readCant = fis.read(buf)) != -1) {
                    fos.write(buf, 0, readCant);
                }
                fis.close();
                fos.close();
            } catch (Exception ex) {
                cfgFile.delete();
                throw ex;
            }
        }
        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        configDoc = db.parse(filePath);
        configDoc.getDocumentElement().normalize();
        return configDoc;
    }

    @Override
    public DefaultMutableTreeNode getRoot() {
        return (DefaultMutableTreeNode) super.getRoot();
    }
}
