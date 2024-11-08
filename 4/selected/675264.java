package org.suse.ui.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.wst.sse.core.internal.provisional.INodeAdapter;
import org.eclipse.wst.sse.core.internal.provisional.INodeNotifier;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

public class Task extends GraphElement {

    public String getName() {
        IDOMNode type = (IDOMNode) getNode().getAttributes().getNamedItem("name");
        if (type != null) return type.getNodeValue();
        String parentName = getParentName();
        return parentName == null ? null : parentName;
    }

    private String getParentName() {
        INodeAdapter adapter = ((IDOMNode) getNode().getParentNode()).getAdapterFor(GraphElement.class);
        if (!(adapter instanceof Node)) return null;
        return ((Node) adapter).getName();
    }

    public void setName(String name) {
        if (name == null) {
            getNode().getAttributes().removeNamedItem("name");
        } else if (canSetNameTo(name)) {
            uncheckedSetName(name);
        }
        notifyChange(ELEMENT_NAME_SET);
    }

    public String getDueDate() {
        IDOMNode duedate = (IDOMNode) getNode().getAttributes().getNamedItem("duedate");
        return duedate == null ? null : duedate.getNodeValue();
    }

    public void setDueDate(String duedate) {
        if (duedate == null) {
            getNode().getAttributes().removeNamedItem("duedate");
        } else {
            org.w3c.dom.Node node = getNode().getAttributes().getNamedItem("duedate");
            if (node == null) {
                node = getNode().getOwnerDocument().createAttribute("duedate");
                getNode().getAttributes().setNamedItem(node);
            }
            node.setNodeValue(duedate);
        }
    }

    public void setSwimlane(String swimlane) {
        if (swimlane == null) {
            getNode().getAttributes().removeNamedItem("swimlane");
        } else {
            org.w3c.dom.Node node = getNode().getAttributes().getNamedItem("swimlane");
            if (node == null) {
                node = getNode().getOwnerDocument().createAttribute("swimlane");
                getNode().getAttributes().setNamedItem(node);
            }
            node.setNodeValue(swimlane);
        }
    }

    public org.w3c.dom.Node getAssignment() {
        return getNode("assignment");
    }

    public org.w3c.dom.Node getController() {
        return getNode("controller");
    }

    public org.w3c.dom.Node getNode(String name) {
        if (name == null) return null;
        NodeList list = getNode().getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            org.w3c.dom.Node candidate = list.item(i);
            if (name.equals(candidate.getNodeName())) {
                return candidate;
            }
        }
        return null;
    }

    public boolean hasAssignment() {
        return (getAssignment() != null);
    }

    public boolean hasController() {
        return (getController() != null);
    }

    public void addAssignment() {
        if (hasAssignment()) return;
        addNode("assignment");
    }

    public void addController() {
        if (hasController()) return;
        addNode("controller");
    }

    public void addNode(String name) {
        IDOMNode last = (IDOMNode) getNode().getLastChild();
        if (last == null) {
            last = (IDOMNode) getNode().getOwnerDocument().createTextNode("\n" + getPaddingString(getLevel() - 1));
            getNode().appendChild(last);
        }
        addNodeBefore(last, name);
    }

    public void removeAssignment() {
        removeNode("assignment");
    }

    public void removeController() {
        removeNode("controller");
    }

    private void removeNode(String name) {
        IDOMNode node = (IDOMNode) getNode(name);
        if (node == null) return;
        IDOMNode previous = (IDOMNode) node.getPreviousSibling();
        if (previous != null) {
            getNode().removeChild(previous);
        }
        getNode().removeChild(node);
    }

    private void removeChildren(org.w3c.dom.Node node) {
        while (node.getChildNodes().getLength() > 0) {
            node.removeChild(node.getFirstChild());
        }
    }

    public void addAssignmentBefore(IDOMNode before) {
        addNodeBefore(before, "assignment");
    }

    public void addControllerBefore(IDOMNode before) {
        addNodeBefore(before, "controller");
    }

    private void addNodeBefore(IDOMNode before, String name) {
        IDOMNode node = (IDOMNode) getNode().getOwnerDocument().createElement(name);
        getNode().insertBefore(node, before);
        IDOMNode text = (IDOMNode) getNode().getOwnerDocument().createTextNode("\n" + getPaddingString(getLevel()));
        getNode().insertBefore(text, node);
    }

    public String getAssignmentExpression() {
        org.w3c.dom.Node assignment = getAssignment();
        if (assignment == null) return null;
        org.w3c.dom.Node expression = assignment.getAttributes().getNamedItem("expression");
        return expression == null ? null : expression.getNodeValue();
    }

    public String getAssignmentDelegateClassName() {
        return getNodeDelegateClassName("assignment");
    }

    public String getControllerDelegateClassName() {
        return getNodeDelegateClassName("controller");
    }

    private String getNodeDelegateClassName(String nodeName) {
        org.w3c.dom.Node node = getNode(nodeName);
        if (node == null) return null;
        org.w3c.dom.Node attr = node.getAttributes().getNamedItem("class");
        return attr == null ? null : attr.getNodeValue();
    }

    public void setAssignmentExpression(String expression) {
        if (!hasAssignment()) addAssignment();
        org.w3c.dom.Node attr = getNode("expression");
        if (attr == null) {
            attr = getAssignment().getOwnerDocument().createAttribute("expression");
            getAssignment().getAttributes().setNamedItem(attr);
        }
        if (expression == null) {
            getAssignment().getAttributes().removeNamedItem("expression");
        } else {
            attr.setNodeValue(expression);
        }
    }

    public void setAssignmentDelegateClassName(String delegateClassName) {
        setNodeDelegateClassName("assignment", delegateClassName);
    }

    public void setControllerDelegateClassName(String delegateClassName) {
        setNodeDelegateClassName("controller", delegateClassName);
    }

    private void setNodeDelegateClassName(String nodeName, String delegateClassName) {
        org.w3c.dom.Node node = getNode(nodeName);
        if (node == null) return;
        String oldName = getNodeDelegateClassName(nodeName);
        org.w3c.dom.Node attr = node.getAttributes().getNamedItem("class");
        if (attr == null) {
            attr = node.getOwnerDocument().createAttribute("class");
            node.getAttributes().setNamedItem(attr);
        }
        if (delegateClassName == null) {
            node.getAttributes().removeNamedItem("class");
        } else {
            attr.setNodeValue(delegateClassName);
        }
        getProcessDefinition().classNameChanged(oldName, delegateClassName);
    }

    public String getAssignmentConfigurationType() {
        return getNodeConfigurationType("assignment");
    }

    public String getControllerConfigurationType() {
        return getNodeConfigurationType("controller");
    }

    private String getNodeConfigurationType(String nodeName) {
        org.w3c.dom.Node node = getNode(nodeName);
        if (node == null) return null;
        org.w3c.dom.Node attr = node.getAttributes().getNamedItem("config-type");
        return attr == null ? "field" : attr.getNodeValue();
    }

    public void setAssignmentConfigurationType(String configurationType) {
        setNodeConfigurationType("assignment", configurationType);
    }

    public void setControllerConfigurationType(String configurationType) {
        setNodeConfigurationType("controller", configurationType);
    }

    private void setNodeConfigurationType(String nodeName, String configurationType) {
        org.w3c.dom.Node node = getNode(nodeName);
        if (node == null) return;
        org.w3c.dom.Node attr = node.getAttributes().getNamedItem("config-type");
        if (attr == null) {
            attr = node.getOwnerDocument().createAttribute("config-type");
            node.getAttributes().setNamedItem(attr);
        }
        if (configurationType == null || "field".equals(configurationType)) {
            node.getAttributes().removeNamedItem("config-type");
        }
        attr.setNodeValue(configurationType);
    }

    public void addAssignmentConfigurationInfo(TreeMap info) {
        addNodeConfigurationInfo("assignment", info);
    }

    public void addControllerConfigurationInfo(TreeMap info) {
        addNodeConfigurationInfo("controller", info);
    }

    private void addNodeConfigurationInfo(String nodeName, TreeMap info) {
        org.w3c.dom.Node node = getNode(nodeName);
        if (node == null) return;
        removeChildren(node);
        int level = getLevel() + 1;
        if (!info.isEmpty()) {
            IDOMNode last = (IDOMNode) getNode().getOwnerDocument().createTextNode("\n" + getPaddingString(level - 1));
            node.appendChild(last);
        }
        Iterator iterator = info.keySet().iterator();
        while (iterator.hasNext()) {
            IDOMNode before = (IDOMNode) node.getLastChild();
            String elementName = (String) iterator.next();
            String elementValue = (String) info.get(elementName);
            addConfigurationElement(nodeName, elementName, elementValue, before);
        }
    }

    public void addControllerVariables(List variables) {
        if (!hasController()) return;
        removeChildren(getController());
        int level = getLevel() + 1;
        if (variables.size() == 0) return;
        IDOMNode last = (IDOMNode) getNode().getOwnerDocument().createTextNode("\n" + getPaddingString(level - 1));
        getController().appendChild(last);
        for (int i = 0; i < variables.size(); i++) {
            addControllerVariable((Variable) variables.get(i));
        }
    }

    private void addControllerVariable(Variable variable) {
        if (variable.name == null || "".equals(variable.name)) return;
        Document document = getNode().getOwnerDocument();
        IDOMNode before = (IDOMNode) getController().getLastChild();
        IDOMNode element = (IDOMNode) document.createElement("variable");
        org.w3c.dom.Node name = document.createAttribute("name");
        name.setNodeValue(variable.name);
        element.getAttributes().setNamedItem(name);
        org.w3c.dom.Node access = document.createAttribute("access");
        String accessString = ",";
        if (Boolean.TRUE.equals(variable.read)) accessString += "read";
        if (Boolean.TRUE.equals(variable.write)) accessString += ",write";
        if (Boolean.TRUE.equals(variable.required)) accessString += ",required";
        if (!",read,write".equals(accessString)) {
            access.setNodeValue(accessString.substring(1));
            element.getAttributes().setNamedItem(access);
        }
        if (variable.mappedName != null && !"".equals(variable.mappedName)) {
            org.w3c.dom.Node mappedName = document.createAttribute("mapped-name");
            mappedName.setNodeValue(variable.mappedName);
            element.getAttributes().setNamedItem(mappedName);
        }
        getController().insertBefore(element, before);
        IDOMNode text = (IDOMNode) document.createTextNode("\n" + getPaddingString(getLevel() + 1));
        getController().insertBefore(text, element);
    }

    public List getControllerVariables() {
        ArrayList result = new ArrayList();
        if (hasController() && getController().getAttributes().getNamedItem("class") == null) {
            NodeList children = getController().getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                if ("variable".equals(children.item(i).getNodeName())) {
                    NamedNodeMap attributes = children.item(i).getAttributes();
                    String name = attributes.getNamedItem("name").getNodeValue();
                    org.w3c.dom.Node accessAttr = attributes.getNamedItem("access");
                    String access = accessAttr == null ? "read,write" : accessAttr.getNodeValue();
                    org.w3c.dom.Node mappedNameAttr = attributes.getNamedItem("mapped-name");
                    String mappedName = mappedNameAttr == null ? null : mappedNameAttr.getNodeValue();
                    result.add(new Variable(name, access.indexOf("read") != -1, access.indexOf("write") != -1, access.indexOf("required") != -1, mappedName));
                }
            }
        }
        return result;
    }

    public void addAssignmentConfigurationInfo(String info) {
        addNodeConfigurationInfo("assignment", info);
    }

    public void addControllerConfigurationInfo(String info) {
        addNodeConfigurationInfo("controller", info);
    }

    private void addNodeConfigurationInfo(String nodeName, String info) {
        org.w3c.dom.Node node = getNode(nodeName);
        if (node == null) return;
        removeChildren(node);
        if (info != null) {
            StringBuffer nodeText = new StringBuffer("\n");
            if (info.trim().indexOf('\n') == -1) {
                nodeText.append(getPaddingString(getLevel() + 1));
            }
            nodeText.append(info.trim()).append("\n").append(getPaddingString(getLevel()));
            IDOMNode config = (IDOMNode) getNode().getOwnerDocument().createTextNode(nodeText.toString());
            node.appendChild(config);
        }
    }

    private void addConfigurationElement(String nodeName, String name, String value, IDOMNode before) {
        Document document = getNode(nodeName).getOwnerDocument();
        IDOMNode element = (IDOMNode) document.createElement(name);
        getNode(nodeName).insertBefore(element, before);
        element.appendChild((IDOMNode) document.createTextNode(value));
        IDOMNode text = (IDOMNode) document.createTextNode("\n" + getPaddingString(getLevel() + 1));
        getNode(nodeName).insertBefore(text, element);
    }

    public Object getAssignmentConfigurationInfo() {
        return getNodeConfigurationInfo("assignment");
    }

    public Object getControllerConfigurationInfo() {
        return getNodeConfigurationInfo("controller");
    }

    private Object getNodeConfigurationInfo(String nodeName) {
        String configurationType = getNodeConfigurationType(nodeName);
        if ("constructor".equals(configurationType) || "configuration-property".equals(configurationType)) {
            return getNodeConfigurationString(nodeName);
        } else {
            return getNodeConfigurationMap(nodeName);
        }
    }

    private String getNodeConfigurationString(String nodeName) {
        if (getNode(nodeName).hasChildNodes()) {
            return ((IDOMNode) getNode(nodeName).getFirstChild()).getSource().trim();
        }
        return null;
    }

    private TreeMap getNodeConfigurationMap(String nodeName) {
        TreeMap result = new TreeMap();
        if (getNode(nodeName) != null) {
            NodeList children = getNode(nodeName).getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                IDOMNode node = (IDOMNode) children.item(i);
                if (node.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                    IDOMNode child = (IDOMNode) node.getFirstChild();
                    result.put(node.getNodeName(), child == null ? null : child.getNodeValue());
                }
            }
        }
        return result;
    }

    public boolean isBlocking() {
        IDOMNode blocking = (IDOMNode) getNode().getAttributes().getNamedItem("blocking");
        if (blocking == null) return false;
        String value = blocking.getNodeValue();
        if ("true".equals(value) || "yes".equals(value)) {
            return true;
        }
        return false;
    }

    public void setBlocking(boolean blocking) {
        if (blocking == false) {
            getNode().getAttributes().removeNamedItem("blocking");
        } else {
            org.w3c.dom.Node node = getNode().getAttributes().getNamedItem("blocking");
            if (node == null) {
                node = getNode().getOwnerDocument().createAttribute("blocking");
                getNode().getAttributes().setNamedItem(node);
            }
            node.setNodeValue("true");
        }
    }

    private boolean canSetNameTo(String name) {
        Node parent = getParent();
        if (parent == null || parent instanceof StartState) return true;
        Task task = ((TaskNode) parent).getTaskByName(name);
        return task == null || task == this;
    }

    private Node getParent() {
        IDOMNode result = (IDOMNode) getNode().getParentNode();
        return result == null ? null : (Node) result.getAdapterFor(GraphElement.class);
    }

    private void uncheckedSetName(String name) {
        org.w3c.dom.Node node = getNode().getAttributes().getNamedItem("name");
        if (node == null) {
            node = getNode().getOwnerDocument().createAttribute("name");
            getNode().getAttributes().setNamedItem(node);
        }
        node.setNodeValue(name);
    }

    public void notifyChanged(INodeNotifier notifier, int eventType, Object changedFeature, Object oldValue, Object newValue, int pos) {
        if (changedFeature != null && ((IDOMNode) changedFeature).getNodeName().equals("name")) {
            notifyChange(ELEMENT_NAME_SET);
        }
        super.notifyChanged(notifier, eventType, changedFeature, oldValue, newValue, pos);
    }

    public IPropertyDescriptor[] getPropertyDescriptors() {
        return new IPropertyDescriptor[0];
    }

    public Object getPropertyValue(Object id) {
        return null;
    }

    public void setPropertyValue(Object id, Object value) {
    }
}
