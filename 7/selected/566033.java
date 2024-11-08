package tr.edu.metu.srdc.utils;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Vector;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.Spring;
import javax.swing.SpringLayout;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import org.apache.xerces.impl.xs.XSAttributeDecl;
import org.apache.xerces.impl.xs.XSAttributeUseImpl;
import org.apache.xerces.impl.xs.XSElementDecl;
import org.apache.xerces.xs.XSComplexTypeDefinition;
import org.apache.xerces.xs.XSConstants;
import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSImplementation;
import org.apache.xerces.xs.XSLoader;
import org.apache.xerces.xs.XSModel;
import org.apache.xerces.xs.XSModelGroup;
import org.apache.xerces.xs.XSNamedMap;
import org.apache.xerces.xs.XSObject;
import org.apache.xerces.xs.XSObjectList;
import org.apache.xerces.xs.XSParticle;
import org.apache.xerces.xs.XSTerm;
import org.apache.xerces.xs.XSTypeDefinition;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import com.sun.org.apache.xerces.internal.impl.dv.XSSimpleType;
import tr.edu.metu.srdc.model.XStanceNode;
import tr.edu.metu.srdc.view.XStanceCellRenderer;
import tr.edu.metu.srdc.view.XStance;

public class XsRoutines {

    private static XStanceNode rootNode;

    public static TreePath selectionPath = null;

    public static XSModel xsModel = null;

    private static File xsFile = null;

    public static int getGlobalElementNumberInXSD(Document doc) {
        int number = 0;
        NodeList nodeList = doc.getDocumentElement().getChildNodes();
        int index = 0;
        while (index < nodeList.getLength()) {
            if (nodeList.item(index).getNodeType() == Element.ELEMENT_NODE) break;
            index++;
        }
        Node elementNode = nodeList.item(index);
        Element element = (Element) nodeList.item(index);
        if (element.getLocalName().equals("element")) number++;
        Node nextSibling = null;
        while ((nextSibling = elementNode.getNextSibling()) != null) {
            if (nextSibling.getNodeType() == Element.ELEMENT_NODE) {
                Element nextElement = (Element) nextSibling;
                if (nextElement.getLocalName().equals("element")) number++;
            }
            elementNode = nextSibling;
        }
        return number;
    }

    public static Vector getGlobalElementsInXSD(Document doc) {
        Vector elementVector = new Vector();
        NodeList nodeList = doc.getDocumentElement().getChildNodes();
        int index = 0;
        while (index < nodeList.getLength()) {
            if (nodeList.item(index).getNodeType() == Element.ELEMENT_NODE) break;
            index++;
        }
        Node elementNode = nodeList.item(index);
        Element element = (Element) nodeList.item(index);
        if (element.getLocalName().equals("element")) {
            elementVector.add(element);
        }
        Node nextSibling = null;
        while ((nextSibling = elementNode.getNextSibling()) != null) {
            System.out.println(element.getLocalName() + " - " + nextSibling.getLocalName());
            if (nextSibling.getNodeType() == Element.ELEMENT_NODE) {
                Element nextElement = (Element) nextSibling;
                if (nextElement.getLocalName().equals("element")) {
                    elementVector.add(nextElement);
                }
            }
            elementNode = nextSibling;
        }
        return elementVector;
    }

    public static XStanceNode createXSRoot(File xsFile, XStance viewer) {
        XsRoutines.xsFile = xsFile;
        XStanceNode root = null;
        boolean elementFormDefault = true;
        boolean attributeFormDefault = true;
        if (xsFile == null || xsFile.exists() == false) return null;
        xsModel = getXSModel(xsFile.getAbsolutePath());
        if (xsModel == null) {
            JOptionPane.showMessageDialog(new JFrame(), "No XSD file is loaded", "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        Document document = XmlRoutines.parse(xsFile);
        Element xsElement = document.getDocumentElement();
        String elementFormDefaultAttr = xsElement.getAttribute("elementFormDefault");
        if (elementFormDefaultAttr != null && elementFormDefaultAttr.equals("qualified")) {
            elementFormDefault = true;
        } else {
            elementFormDefault = false;
        }
        String attributeFormDefaultAttr = xsElement.getAttribute("attributeFormDefault");
        if (attributeFormDefaultAttr != null && attributeFormDefaultAttr.equals("qualified")) {
            attributeFormDefault = true;
        } else {
            attributeFormDefault = false;
        }
        String rootName = null;
        int length = getGlobalElementNumberInXSD(document);
        if (length > 1) {
            createRootSelectDialog(document, viewer);
            return rootNode;
        }
        if (length == 1) {
            rootName = getGlobalElementName(xsModel, 0);
        }
        if (rootName != null) {
            root = getTreeNode(xsModel, rootName);
        }
        return root;
    }

    public static String getGlobalElementName(XSModel model, int index) {
        String elemName = null;
        XSNamedMap elems = model.getComponents(XSConstants.ELEMENT_DECLARATION);
        elemName = elems.item(index).getName();
        return elemName;
    }

    public static Vector getXSAttrNodeNamesFromType(XSModel model, String typeName) {
        Vector idRefs = new Vector();
        XSNamedMap elements = model.getComponents(XSConstants.ELEMENT_DECLARATION);
        int index = 0;
        XSElementDecl element = null;
        for (; index < elements.getLength(); index++) {
            element = (XSElementDecl) elements.item(index);
            if (element.getTypeDefinition().getTypeCategory() == XSTypeDefinition.COMPLEX_TYPE) {
                XSComplexTypeDefinition typeDef = (XSComplexTypeDefinition) element.getTypeDefinition();
                if (typeDef.getContentType() == XSComplexTypeDefinition.CONTENTTYPE_ELEMENT) {
                    XSObjectList list = typeDef.getAttributeUses();
                    for (index = 0; index < list.getLength(); index++) {
                        XSAttributeUseImpl attr = (XSAttributeUseImpl) list.item(index);
                        if (attr.getAttrDeclaration().getTypeDefinition().getName().equals(typeName)) {
                            if (!idRefs.contains(attr.getAttrDeclaration().getName())) idRefs.addElement(attr.getAttrDeclaration().getName());
                        }
                    }
                }
            }
        }
        XSNamedMap attrGroups = model.getComponents(XSConstants.ATTRIBUTE_GROUP);
        index = 0;
        org.apache.xerces.impl.xs.XSAttributeGroupDecl attrGroup = null;
        for (; index < attrGroups.getLength(); index++) {
            attrGroup = (org.apache.xerces.impl.xs.XSAttributeGroupDecl) attrGroups.item(index);
            XSObjectList list = attrGroup.getAttributeUses();
            for (int index2 = 0; index2 < list.getLength(); index2++) {
                XSAttributeUseImpl attr = (XSAttributeUseImpl) list.item(index2);
                String declaredType = attr.getAttrDeclaration().getTypeDefinition().getName();
                if (declaredType != null && declaredType.equals(typeName)) {
                    if (!idRefs.contains(attr.getAttrDeclaration().getName())) idRefs.addElement(attr.getAttrDeclaration().getName());
                }
            }
        }
        return idRefs;
    }

    public static XStanceNode getTreeNode(XSModel model, String elemName) {
        XStanceNode node = null;
        XSNamedMap elems = model.getComponents(XSConstants.ELEMENT_DECLARATION);
        int length = elems.getLength();
        String[] tokens = elemName.split(" ");
        XSElementDeclaration elem;
        for (int i = 0; i < length; i++) {
            elem = (XSElementDeclaration) elems.item(i);
            if (tokens[0].equals(elem.getName())) {
                node = getTreeNodeFromElement(elem);
                break;
            }
        }
        tokens = removeFirst(tokens);
        if (node != null) {
            while (tokens != null && tokens.length > 0) {
                int length2 = node.getChildCount();
                for (int j = 0; j < length2; j++) {
                    if (((String) ((XStanceNode) node.getChildAt(j)).getUserObject()).equals(tokens[0])) {
                        node = (XStanceNode) node.getChildAt(j);
                        break;
                    }
                }
                tokens = removeFirst(tokens);
            }
        }
        return node;
    }

    public static String[] removeFirst(String[] arr) {
        String result[] = null;
        int length = arr.length - 1;
        if (length > 0) {
            result = new String[length];
            for (int i = 0; i < length; i++) {
                result[i] = arr[i + 1];
            }
        }
        return result;
    }

    private static DefaultMutableTreeNode getTreeNodeFromRoot(XSModel model) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Schema Root");
        XSNamedMap elems = model.getComponents(XSConstants.ELEMENT_DECLARATION);
        int length = elems.getLength();
        XSElementDeclaration elem;
        for (int i = 0; i < length; i++) {
            elem = (XSElementDeclaration) elems.item(i);
            root.add(getTreeNodeFromElement(elem));
        }
        return root;
    }

    private static XStanceNode getTreeNodeFromElement(XSElementDeclaration elem) {
        String name = elem.getName();
        String namespace = elem.getNamespace();
        Document doc = XmlRoutines.newDocument(false, xsFile);
        Node xmlNode = doc.createElementNS(namespace, name);
        XStanceNode node = new XStanceNode(xmlNode);
        XSTypeDefinition typeDef = elem.getTypeDefinition();
        if (typeDef.getTypeCategory() == XSTypeDefinition.COMPLEX_TYPE) {
            node = getTreeNodeFromComplex(node, (XSComplexTypeDefinition) typeDef);
        }
        return node;
    }

    private static XStanceNode getTreeNodeFromComplex(XStanceNode node, XSComplexTypeDefinition complex) {
        switch(complex.getContentType()) {
            case XSComplexTypeDefinition.CONTENTTYPE_ELEMENT:
                XSObjectList list = complex.getAttributeUses();
                Document doc = XmlRoutines.newDocument(false, xsFile);
                int index = 0;
                while (index < list.getLength()) {
                    XSObject obj = list.item(index);
                    XSAttributeUseImpl attr = (XSAttributeUseImpl) obj;
                    Attr newAttr = doc.createAttributeNS(attr.getAttrDeclaration().getNamespace(), attr.getAttrDeclaration().getName());
                    newAttr = (Attr) ((Element) node.getUserObject()).getOwnerDocument().adoptNode(newAttr);
                    ((Element) node.getUserObject()).setAttributeNode(newAttr);
                    index++;
                }
                XSModelGroup modelGroup = (XSModelGroup) complex.getParticle().getTerm();
                XSObjectList particles = modelGroup.getParticles();
                int length = particles.getLength();
                for (int i = 0; i < length; i++) {
                    node = getTreeNodeFromTerm(node, (XSTerm) ((XSParticle) particles.item(i)).getTerm());
                }
                break;
            case XSComplexTypeDefinition.CONTENTTYPE_EMPTY:
                break;
            case XSComplexTypeDefinition.CONTENTTYPE_MIXED:
                break;
            case XSComplexTypeDefinition.CONTENTTYPE_SIMPLE:
                break;
        }
        return node;
    }

    private static XStanceNode getTreeNodeFromTerm(XStanceNode node, XSTerm term) {
        XStanceNode childNode = null;
        switch(term.getType()) {
            case XSConstants.ELEMENT_DECLARATION:
                XSElementDeclaration elem = (XSElementDeclaration) term;
                if (isRecursive(node, elem.getName())) {
                    String name = elem.getName();
                    String namespace = elem.getNamespace();
                    Document doc = XmlRoutines.newDocument(false, xsFile);
                    Node newNode = doc.createElementNS(namespace, name);
                    childNode = new XStanceNode(newNode);
                    node.add(childNode);
                } else {
                    childNode = getTreeNodeFromElement(elem);
                    node.add(childNode);
                }
                break;
            case XSConstants.MODEL_GROUP:
                XSModelGroup modelGroup = (XSModelGroup) term;
                if (term == null) break; else if (term.getName() != null) {
                    if (term.getName().equals("Package")) break;
                }
                short compositor = modelGroup.getCompositor();
                if (compositor == XSModelGroup.COMPOSITOR_SEQUENCE) {
                    XSObjectList particles = modelGroup.getParticles();
                    int length = particles.getLength();
                    for (int i = 0; i < length; i++) {
                        node = getTreeNodeFromTerm(node, (XSTerm) ((XSParticle) particles.item(i)).getTerm());
                    }
                } else if (compositor == XSModelGroup.COMPOSITOR_CHOICE) {
                    XSObjectList particles = modelGroup.getParticles();
                    int length = particles.getLength();
                    for (int i = 0; i < length; i++) {
                        XSTerm xsTerm = (XSTerm) ((XSParticle) particles.item(i)).getTerm();
                        if (xsTerm == null) continue;
                        node = getTreeNodeFromTerm(node, xsTerm);
                    }
                }
                break;
            case XSConstants.WILDCARD:
                break;
        }
        return node;
    }

    public static boolean isRecursive(XStanceNode node, String elemName) {
        boolean isRec = false;
        if (node == null) {
            isRec = false;
        } else if (elemName.equalsIgnoreCase((String) ((Node) node.getUserObject()).getLocalName())) {
            isRec = true;
        }
        return isRec;
    }

    public static XSModel getXSModel(String uri) {
        XSModel model = null;
        try {
            System.setProperty(DOMImplementationRegistry.PROPERTY, "org.apache.xerces.dom.DOMXSImplementationSourceImpl");
            DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
            XSImplementation impl = (XSImplementation) registry.getDOMImplementation("XS-Loader");
            XSLoader xsLoader = impl.createXSLoader(null);
            model = xsLoader.loadURI(uri);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return model;
    }

    private static void createRootSelectDialog(Document document, XStance viewer) {
        final JDialog rootSelect = new JDialog(new JFrame(), "XStance", true);
        JDialog.setDefaultLookAndFeelDecorated(true);
        rootSelect.setResizable(false);
        JPanel rootSelectPane = new JPanel(new SpringLayout());
        rootSelect.setContentPane(rootSelectPane);
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Schema Root");
        final JTree tree = new JTree(getTreeNodeFromRoot(xsModel));
        tree.setCellRenderer(new XStanceCellRenderer(viewer));
        tree.setRootVisible(false);
        JScrollPane treePane = new JScrollPane(tree);
        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.X_AXIS));
        buttonPane.setBorder(new EmptyBorder(3, 0, 3, 3));
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");
        okButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                rootNode = (XStanceNode) tree.getSelectionPath().getPathComponent(1);
                Object[] oldPath = tree.getSelectionPath().getPath();
                Object[] newPath = new Object[oldPath.length - 1];
                for (int i = 0; i < newPath.length; i++) {
                    newPath[i] = oldPath[i + 1];
                }
                selectionPath = new TreePath(newPath);
                rootSelect.dispose();
            }
        });
        cancelButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                rootSelect.dispose();
            }
        });
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(okButton);
        buttonPane.add(Box.createRigidArea(new Dimension(5, 0)));
        buttonPane.add(cancelButton);
        rootSelectPane.add(new JLabel("Specify the root element"));
        rootSelectPane.add(treePane);
        rootSelectPane.add(buttonPane);
        makeCompactGrid(rootSelectPane, 3, 1, 1, 10, 1, 2);
        rootSelectPane.getRootPane().setDefaultButton(okButton);
        rootSelectPane.setPreferredSize(new Dimension(260, 240));
        rootSelect.pack();
        rootSelect.setVisible(true);
    }

    private static void makeCompactGrid(Container parent, int rows, int cols, int initialX, int initialY, int xPad, int yPad) {
        SpringLayout layout = (SpringLayout) parent.getLayout();
        SpringLayout.Constraints constraints;
        Spring width, height, x = Spring.constant(initialX), y = Spring.constant(initialY);
        for (int c = 0; c < cols; c++) {
            width = Spring.constant(0);
            for (int r = 0; r < rows; r++) {
                width = Spring.max(width, getConstraintsForCell(r, c, parent, cols).getWidth());
            }
            for (int r = 0; r < rows; r++) {
                constraints = getConstraintsForCell(r, c, parent, cols);
                constraints.setX(x);
                constraints.setWidth(width);
            }
            x = Spring.sum(x, Spring.sum(width, Spring.constant(xPad)));
        }
        for (int r = 0; r < rows; r++) {
            height = Spring.constant(0);
            for (int c = 0; c < cols; c++) {
                height = Spring.max(height, getConstraintsForCell(r, c, parent, cols).getHeight());
            }
            for (int c = 0; c < cols; c++) {
                constraints = getConstraintsForCell(r, c, parent, cols);
                constraints.setY(y);
                constraints.setHeight(height);
            }
            y = Spring.sum(y, Spring.sum(height, Spring.constant(yPad)));
        }
        constraints = layout.getConstraints(parent);
        constraints.setConstraint(SpringLayout.SOUTH, y);
        constraints.setConstraint(SpringLayout.EAST, x);
    }

    private static SpringLayout.Constraints getConstraintsForCell(int row, int col, Container parent, int cols) {
        SpringLayout layout = (SpringLayout) parent.getLayout();
        Component c = parent.getComponent(row * cols + col);
        return layout.getConstraints(c);
    }

    public static void main(String args[]) {
        File aFile = new File("deneme.xml");
        Document document = XmlRoutines.parse(aFile);
        System.out.println(getGlobalElementNumberInXSD(document));
        final JFrame rootSelect = new JFrame("XmlStylist");
        rootSelect.setSize(300, 300);
        JDialog.setDefaultLookAndFeelDecorated(true);
        rootSelect.setResizable(true);
        JPanel rootSelectPane = new JPanel(new SpringLayout());
        rootSelect.setContentPane(rootSelectPane);
        DefaultTreeModel model = new DefaultTreeModel(createXSRoot(aFile, new XStance()));
        model.reload();
        final JTree tree = new JTree(model);
        tree.setSelectionPath(selectionPath);
        tree.setCellRenderer(new XStanceCellRenderer(new XStance()));
        tree.setRootVisible(true);
        JScrollPane treePane = new JScrollPane(tree);
        rootSelect.getContentPane().add(treePane);
        rootSelect.setVisible(true);
    }

    public static File getXsFile() {
        return xsFile;
    }

    public static XSAttributeUseImpl findAttributeUseImpl(XSModel model, String nodeName, String attrName) {
        if (attrName == null) return null;
        if (model != null) {
            XSNamedMap elements = model.getComponents(XSConstants.ELEMENT_DECLARATION);
            int index = 0;
            XSElementDecl element = null;
            for (; index < elements.getLength(); index++) {
                element = (XSElementDecl) elements.item(index);
                if (element.getName().equals(nodeName)) {
                    break;
                }
            }
            if (index != elements.getLength()) {
                if (element.getTypeDefinition().getTypeCategory() == XSTypeDefinition.COMPLEX_TYPE) {
                    XSComplexTypeDefinition typeDef = (XSComplexTypeDefinition) element.getTypeDefinition();
                    if (typeDef.getContentType() == XSComplexTypeDefinition.CONTENTTYPE_ELEMENT) {
                        XSObjectList list = typeDef.getAttributeUses();
                        for (index = 0; index < list.getLength(); index++) {
                            XSAttributeUseImpl attr = (XSAttributeUseImpl) list.item(index);
                            if (attr.getAttrDeclaration().getName().equals(attrName)) {
                                return attr;
                            }
                        }
                    }
                }
            } else {
                XSNamedMap attrGroups = model.getComponents(XSConstants.ATTRIBUTE_GROUP);
                index = 0;
                org.apache.xerces.impl.xs.XSAttributeGroupDecl attrGroup = null;
                for (; index < attrGroups.getLength(); index++) {
                    attrGroup = (org.apache.xerces.impl.xs.XSAttributeGroupDecl) attrGroups.item(index);
                    XSObjectList list = attrGroup.getAttributeUses();
                    for (int index2 = 0; index2 < list.getLength(); index2++) {
                        XSAttributeUseImpl attr = (XSAttributeUseImpl) list.item(index2);
                        if (attr.getAttrDeclaration().getName().equals(attrName)) {
                            return attr;
                        }
                    }
                }
            }
        }
        return null;
    }
}
