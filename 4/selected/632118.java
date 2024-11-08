package org.formaria.editor.project.registry;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Vector;
import java.util.jar.JarFile;
import java.util.prefs.Preferences;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.zip.ZipEntry;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import org.formaria.debug.DebugLogger;
import org.formaria.editor.EditorDefaults;
import org.formaria.editor.project.EditorProject;
import org.formaria.editor.project.helper.AFileFilter;
import org.formaria.xml.XmlElement;
import org.formaria.xml.nanoxml.NanoXmlElement;
import org.formaria.xml.nanoxml.NanoXmlWriter;
import org.formaria.swing.Page;
import org.formaria.aria.ProjectManager;
import org.formaria.aria.build.BuildProperties;
import org.formaria.editor.project.pages.components.EditorComponentAdapter;
import org.formaria.editor.project.pages.components.EditorRegisteredComponentFactory;
import org.formaria.registry.RegisteredComponentFactory;
import org.formaria.aria.ComponentFactory;

/**
 * A class for editing and creating component registrations for Java UI beans.
 * <p>Copyright Formaria Ltd., (c) 2001-2005 </p>
 * <p>$Revision: 1.15 $ </p>
 */
public class ComponentRegistryEditor extends JDialog implements ActionListener, TreeSelectionListener, ChangeListener {

    private static final int PROPERTY = 0;

    private static final int INCLUDED = 1;

    private static final int MODE = 2;

    private static final int GET = 3;

    private static final int SET = 4;

    private static final int TYPE = 5;

    private static final int COMMENT = 6;

    private JTabbedPane tabPanel;

    private JPanel commandPanel;

    private JButton okBtn, cancelBtn;

    private JButton editBtn, removeBtn, addBtn, iconBtn, includeBtn;

    private JList libraryList;

    private JTree componentTree;

    private DefaultMutableTreeNode root;

    private DefaultTreeModel model;

    private JScrollPane jScrollPane1;

    private JTable componentPropertyTable;

    private JTextField nameEdit, classEdit, widgetEdit, iconEdit, widthEdit, heightEdit;

    JTextArea previewPanel;

    private Vector<JarRef> libraries;

    private boolean selectionChanging;

    private ClassTreeNodeItem selectedTreeNode;

    private String fileName;

    private Hashtable nodesTable;

    private boolean expertMode;

    private EditorProject currentProject;

    public static void main(String[] args) {
        new JFrame("Testing the Component Registration Editor");
        ComponentRegistryEditor csd = new ComponentRegistryEditor("Component Registration Editor", null, true);
        csd.setSize(new Dimension(800, 600));
        csd.setModal(true);
        csd.setVisible(true);
        System.exit(0);
    }

    /**
   * Create a new instance of the editor
   * @param title the dialog title
   */
    public ComponentRegistryEditor(String title, String xmlFileName, boolean mode) {
        try {
            setTitle(title);
            tabPanel = new JTabbedPane();
            tabPanel.addChangeListener(this);
            expertMode = mode;
            currentProject = (EditorProject) ProjectManager.getCurrentProject();
            nodesTable = new Hashtable();
            root = new DefaultMutableTreeNode("Components", true);
            libraries = new Vector<JarRef>();
            setupLibrariesPanel();
            setupComponentsPanel();
            setupPreviewPanel();
            setupCommandPanel();
            JPanel contentPanel = (JPanel) getContentPane();
            contentPanel.add(tabPanel, BorderLayout.CENTER);
            contentPanel.add(commandPanel, BorderLayout.SOUTH);
            tabPanel.setSelectedIndex(0);
            pack();
            addWindowListener(new WindowAdapter() {

                public void windowClosing(WindowEvent we) {
                    closeDlg();
                }
            });
            addSystemComponents();
            addRegisteredJars();
            libraryList.updateUI();
            fillComponentsTree();
            if (xmlFileName == null) {
                Preferences prefs = Preferences.userNodeForPackage(EditorDefaults.class);
                String defDir = prefs.get("ComponentRegistry", "c:\\");
                JFileChooser chooser = new JFileChooser();
                AFileFilter myFilter = new AFileFilter("xml", "XML files");
                myFilter.setDirMustExist(true);
                myFilter.setFileMustExist(true);
                chooser.setFileFilter(myFilter);
                chooser.setFileSelectionMode(chooser.FILES_ONLY);
                chooser.setDialogTitle("Choose the registry file");
                chooser.setCurrentDirectory(new File(defDir));
                if ((currentProject != null) && (currentProject instanceof EditorProject)) {
                    String fn = ((EditorProject) currentProject).getPath() + File.separator + "resources";
                    chooser.setCurrentDirectory(new File(fn));
                }
                if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                    fileName = chooser.getSelectedFile().toString();
                    try {
                        prefs.put("ComponentRegistry", chooser.getSelectedFile().getCanonicalPath());
                    } catch (IOException ex) {
                    }
                }
            } else fileName = xmlFileName;
            setup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void closeDlg() {
        int ret = JOptionPane.showConfirmDialog(this, "Do you want do save?", "Save", JOptionPane.OK_CANCEL_OPTION);
        if (ret == JOptionPane.OK_OPTION) saveXml();
    }

    /**
   * Setup the libraries panel
   * @throws Exception
   */
    private void setupLibrariesPanel() throws Exception {
        JPanel librariesPanel = new JPanel();
        librariesPanel.setLayout(new BorderLayout());
        JScrollPane scrollPane = new JScrollPane();
        libraryList = new JList(libraries);
        JPanel cmdPanel = new JPanel();
        BoxLayout boxLayout = new BoxLayout(cmdPanel, BoxLayout.Y_AXIS);
        editBtn = new JButton();
        removeBtn = new JButton();
        addBtn = new JButton();
        librariesPanel.setLayout(new BorderLayout());
        editBtn.setText("Edit");
        editBtn.addActionListener(this);
        removeBtn.setText("Remove");
        removeBtn.addActionListener(this);
        addBtn.setText("Add...");
        addBtn.addActionListener(this);
        librariesPanel.add(scrollPane, BorderLayout.CENTER);
        librariesPanel.add(cmdPanel, BorderLayout.EAST);
        cmdPanel.setLayout(boxLayout);
        cmdPanel.add(editBtn);
        cmdPanel.add(removeBtn);
        cmdPanel.add(addBtn);
        scrollPane.getViewport().add(libraryList, null);
        tabPanel.add(librariesPanel, "Libraries");
    }

    /**
   * Setup the components panel
   * @throws Exception
   */
    private void setupComponentsPanel() throws Exception {
        JPanel componentsPanel = new JPanel();
        componentsPanel.setBorder(BorderFactory.createEmptyBorder());
        componentsPanel.setLayout(new BorderLayout(10, 10));
        JSplitPane splitter = new JSplitPane();
        componentTree = new JTree();
        componentTree.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        componentTree.addTreeSelectionListener(this);
        JPanel rightPanel = new JPanel();
        rightPanel.setBorder(BorderFactory.createEmptyBorder());
        rightPanel.setLayout(new BorderLayout());
        JPanel topPanel = new JPanel();
        topPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        GridLayout grid = new GridLayout(6, 2);
        grid.setVgap(4);
        grid.setHgap(4);
        topPanel.setLayout(grid);
        topPanel.add(new JLabel("Name"));
        topPanel.add(nameEdit = new JTextField());
        topPanel.add(new JLabel("Class"));
        topPanel.add(classEdit = new JTextField());
        topPanel.add(new JLabel("Widget set"));
        topPanel.add(widgetEdit = new JTextField());
        topPanel.add(new JLabel("Icon"));
        JPanel iconPanel = new JPanel();
        iconPanel.setLayout(new BorderLayout());
        topPanel.add(iconPanel);
        iconPanel.add(iconEdit = new JTextField(), BorderLayout.CENTER);
        iconPanel.add(iconBtn = new JButton("..."), BorderLayout.EAST);
        iconBtn.addActionListener(this);
        topPanel.add(new JLabel("Preferred Size"));
        JPanel sizePanel = new JPanel();
        sizePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(sizePanel);
        sizePanel.add(widthEdit = new JTextField("   "));
        sizePanel.add(new JLabel(" x "));
        sizePanel.add(heightEdit = new JTextField("   "));
        widthEdit.setMinimumSize(new Dimension(50, 20));
        heightEdit.setMinimumSize(new Dimension(50, 20));
        widthEdit.setPreferredSize(new Dimension(50, 20));
        heightEdit.setPreferredSize(new Dimension(50, 20));
        topPanel.add(new JLabel("Include/Exclude"));
        topPanel.add(includeBtn = new JButton("Toggle"));
        includeBtn.addActionListener(this);
        rightPanel.add(topPanel, BorderLayout.NORTH);
        classEdit.setEnabled(false);
        widgetEdit.setEnabled(false);
        jScrollPane1 = new JScrollPane();
        jScrollPane1.setBorder(BorderFactory.createEmptyBorder());
        rightPanel.add(jScrollPane1, BorderLayout.CENTER);
        componentPropertyTable = new JTable();
        componentPropertyTable.setBorder(BorderFactory.createEmptyBorder());
        BorderLayout borderLayout1 = new BorderLayout();
        componentsPanel.setLayout(borderLayout1);
        splitter.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
        componentsPanel.add(splitter, BorderLayout.CENTER);
        JScrollPane scroller = new JScrollPane(componentTree);
        scroller.setBorder(BorderFactory.createEmptyBorder());
        splitter.add(scroller, JSplitPane.LEFT);
        scroller.setBorder(BorderFactory.createEmptyBorder());
        splitter.add(rightPanel, JSplitPane.RIGHT);
        splitter.setDividerLocation(200);
        fillComponentsTree();
        tabPanel.add(componentsPanel, "Components");
    }

    /**
   * Setup the preview panel
   * @throws Exception
   */
    private void setupPreviewPanel() throws Exception {
        JScrollPane previewScrollPane = new JScrollPane();
        previewPanel = new JTextArea();
        previewPanel.setBorder(BorderFactory.createEmptyBorder());
        previewScrollPane.getViewport().add(previewPanel, null);
        tabPanel.add(previewScrollPane, "Preview");
    }

    /**
   * Create the command panel
   * @throws Exception
   */
    private void setupCommandPanel() throws Exception {
        commandPanel = new JPanel();
        commandPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        cancelBtn = new JButton();
        cancelBtn.setText("Cancel");
        cancelBtn.addActionListener(this);
        okBtn = new JButton();
        okBtn.setText("OK");
        okBtn.addActionListener(this);
        commandPanel.add(cancelBtn);
        commandPanel.add(okBtn);
    }

    /**
   * A handler for button clicks
   * @param e the event
   */
    public void actionPerformed(ActionEvent e) {
        Object srcObj = e.getSource();
        if (srcObj == addBtn) addLibrary(); else if (srcObj == removeBtn) removeLibrary(); else if (srcObj == okBtn) {
            saveSelection();
            if (saveXml()) setVisible(false);
        } else if (srcObj == cancelBtn) setVisible(false); else if (srcObj == iconBtn) selectIconFile(); else if (srcObj == includeBtn) includeAllProperties();
    }

    /**
   * The tab selection has changed
   * @param e
   */
    public void stateChanged(ChangeEvent e) {
        if (tabPanel.getSelectedIndex() != 1) {
            saveComponentSettings();
            fillPreviewArea();
        }
    }

    /**
   * Include/Exclude all the component properties
   */
    private void includeAllProperties() {
        DefaultTableModel dm = (DefaultTableModel) componentPropertyTable.getModel();
        int rowCount = dm.getRowCount();
        if (rowCount > 0) {
            boolean include = ((Boolean) dm.getValueAt(0, 1)).booleanValue();
            Boolean newValue = new Boolean(!include);
            for (int i = 0; i < rowCount; i++) dm.setValueAt(newValue, i, 1);
        }
    }

    /**
   * Add the Aria components. The System files are already
   * loaded so the loading proceedure here is different to the normal jar scan
   */
    private void addSystemComponents() {
        DefaultMutableTreeNode systemNode = new DefaultMutableTreeNode("System");
        root.add(systemNode);
        Hashtable componentFactories = ComponentFactory.getFactories();
        Enumeration enumeration = componentFactories.keys();
        while (enumeration.hasMoreElements()) {
            Object obj = componentFactories.get(enumeration.nextElement());
            if (obj instanceof EditorRegisteredComponentFactory) {
                EditorRegisteredComponentFactory ercf = ((EditorRegisteredComponentFactory) obj);
                Hashtable<String, String> jarFiles = ercf.getJarPaths();
                Hashtable systemComponents = ercf.getComponents();
                Hashtable configFiles = ercf.getConfigFiles();
                Enumeration configFilesEnumeration = configFiles.keys();
                while (configFilesEnumeration.hasMoreElements()) {
                    String key = (String) configFilesEnumeration.nextElement();
                    if (jarFiles.get(key) != null) continue;
                    if (key.equals("Project")) continue;
                    JarRef jarRef = new JarRef();
                    jarRef.setJarPath(key);
                    libraries.addElement(jarRef);
                    DefaultMutableTreeNode libNode = new DefaultMutableTreeNode(key);
                    systemNode.add(libNode);
                    Enumeration componentEnum = systemComponents.keys();
                    while (componentEnum.hasMoreElements()) {
                        EditorComponentAdapter ca = (EditorComponentAdapter) systemComponents.get(componentEnum.nextElement());
                        if (ca.getOwner().equals(key)) {
                            Class clazz = ca.getComponentClass();
                            ClassTreeNodeItem item = new ClassTreeNodeItem(clazz, expertMode, jarRef);
                            item.setPreferredSize(ca.getPreferredSize());
                            item.setIconFile(ca.getIcon(false));
                            item.setIsSystemClass(true);
                            if (item != null) {
                                DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(item);
                                libNode.add(newNode);
                                nodesTable.put(clazz.toString().substring(6), item);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
   * Add the Jar components from the registered files
   */
    private void addRegisteredJars() {
        Hashtable componentFactories = ComponentFactory.getFactories();
        Enumeration enumeration = componentFactories.keys();
        while (enumeration.hasMoreElements()) {
            Object obj = componentFactories.get(enumeration.nextElement());
            if (obj instanceof EditorRegisteredComponentFactory) {
                EditorRegisteredComponentFactory ercf = ((EditorRegisteredComponentFactory) obj);
                Hashtable<String, String> jarFiles = ercf.getJarPaths();
                Enumeration<String> keys = jarFiles.keys();
                while (keys.hasMoreElements()) {
                    String key = keys.nextElement();
                    String jarPath = jarFiles.get(key);
                    addJarFile(currentProject.getProjectTitle(), jarPath);
                }
            }
        }
    }

    /**
   * Setup the list configuration
   */
    private void setup() {
        Hashtable componentFactories = ComponentFactory.getFactories();
        Enumeration enumeration = componentFactories.keys();
        while (enumeration.hasMoreElements()) {
            Object obj = componentFactories.get(enumeration.nextElement());
            if (obj instanceof EditorRegisteredComponentFactory) {
                EditorRegisteredComponentFactory ercf = ((EditorRegisteredComponentFactory) obj);
                Hashtable registeredComponents = ercf.getComponents();
                Enumeration componentNameEnum = registeredComponents.keys();
                while (componentNameEnum.hasMoreElements()) {
                    EditorComponentAdapter ca = (EditorComponentAdapter) registeredComponents.get((String) componentNameEnum.nextElement());
                    ClassTreeNodeItem item = (ClassTreeNodeItem) nodesTable.get(ca.getClassName());
                    if (item == null) continue;
                    item.setUserName(ca.getType());
                    item.setPreferredSize(ca.getPreferredSize());
                    int numProperties = ca.getNumProperties();
                    for (int j = 0; j < numProperties; j++) {
                        String propertyName = ca.getPropertyName(j);
                        ArrayList beanProperties = item.getUserProperties();
                        for (int k = 0; k < beanProperties.size(); k++) {
                            Object[] props = ((ClassTreeNodeItem.Property) beanProperties.get(k)).values;
                            if (((String) props[PROPERTY]).equalsIgnoreCase(propertyName)) {
                                props[INCLUDED] = new Boolean(true);
                                String mode = ca.getMode(j);
                                if ((mode != null) && (mode.length() > 0)) props[MODE] = mode; else props[MODE] = "Normal";
                                int accessType = ca.getPropertyAccessType(j);
                                if (accessType == ca.BOTH) props[GET] = new Boolean(true); else if (accessType == ca.SETTER) props[SET] = new Boolean(true); else {
                                    props[GET] = new Boolean(true);
                                    props[SET] = new Boolean(true);
                                }
                                props[COMMENT] = ca.getPropertyComment(j);
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
   * Add a new library / jar
   */
    private void addLibrary() {
        Preferences prefs = Preferences.userNodeForPackage(EditorDefaults.class);
        String defDir = prefs.get("ComponentRegistryJars", "c:\\");
        JFileChooser chooser = new JFileChooser();
        AFileFilter myFilter = new AFileFilter("jar", "Jar files");
        myFilter.setDirMustExist(true);
        myFilter.setFileMustExist(true);
        chooser.setDialogTitle("Choose the library Jar file");
        chooser.setFileFilter(myFilter);
        chooser.setFileSelectionMode(chooser.FILES_ONLY);
        chooser.setCurrentDirectory(new File(defDir));
        while (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            if (chooser.getSelectedFile().exists()) {
                String jarName = chooser.getSelectedFile().toString();
                for (int i = 0; i < libraries.size(); i++) {
                    JarRef jr = libraries.elementAt(i);
                    String path = jr.getJarPath();
                    if ((path != null) && path.equals(jarName)) {
                        JOptionPane.showMessageDialog(this, "A Jar with this name has been added already!");
                        return;
                    }
                }
                try {
                    prefs.put("ComponentRegistryJars", chooser.getSelectedFile().getCanonicalPath());
                } catch (IOException ex) {
                }
                addJarFile(currentProject.getProjectTitle(), jarName);
                libraryList.updateUI();
                fillComponentsTree();
                break;
            } else {
                JOptionPane.showMessageDialog(this, "A Jar with this name doesn't exist!");
            }
        }
    }

    /**
   * Remove a library / jar and delete the component references
   */
    private void removeLibrary() {
        int idx;
        if ((idx = libraryList.getSelectedIndex()) > -1) {
            JarRef removedJarRef = libraries.remove(idx);
            String removedJar = removedJarRef.getJarPath();
            Enumeration treeNodesEnum = root.breadthFirstEnumeration();
            while (treeNodesEnum.hasMoreElements()) {
                DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) treeNodesEnum.nextElement();
                if (treeNode.toString().equals(removedJar)) {
                    ((DefaultMutableTreeNode) treeNode.getParent()).remove(treeNode);
                    selectedTreeNode = null;
                    clearPropertyTable();
                    break;
                }
            }
            libraryList.updateUI();
            fillComponentsTree();
        }
    }

    /**
   * Read the library configuration file
   * @param fn the file containing the library entries
       * @param componentStore the location to store the individual component entries
   */
    private void readLibraryComponents(String fn, Vector componentStore) {
    }

    private void fillComponentsTree() {
        model = new DefaultTreeModel(root);
        componentTree.setModel(model);
        fillPropertyTable();
    }

    private void fillPropertyTable() {
        if (selectedTreeNode != null) {
            ArrayList userProperties = selectedTreeNode.getUserProperties();
            nameEdit.setText(selectedTreeNode.getUserName());
            classEdit.setText(selectedTreeNode.getUserClass().toString().substring(6));
            widgetEdit.setText("Swing");
            iconEdit.setText(selectedTreeNode.getIconFile());
            Dimension d = selectedTreeNode.getPreferredSize();
            widthEdit.setText(Integer.toString(d.width));
            heightEdit.setText(Integer.toString(d.height));
            DefaultTableModel dm = new DefaultTableModel();
            dm.addColumn("Property");
            dm.addColumn("Included");
            dm.addColumn("Mode");
            dm.addColumn("Get");
            dm.addColumn("Set");
            dm.addColumn("Type");
            dm.addColumn("Comment");
            if (userProperties != null) {
                for (int i = 0; i < userProperties.size(); i++) {
                    Object[] values = ((ClassTreeNodeItem.Property) userProperties.get(i)).values;
                    dm.addRow(values);
                }
            }
            componentPropertyTable = new JTable(dm) {

                public Class getColumnClass(int c) {
                    return getValueAt(0, c).getClass();
                }

                public boolean isCellEditable(int row, int col) {
                    if (col < 1) return false; else return true;
                }
            };
            setUpModeColumn(componentPropertyTable, componentPropertyTable.getColumnModel().getColumn(2));
            jScrollPane1.getViewport().removeAll();
            jScrollPane1.getViewport().add(componentPropertyTable, null);
        }
    }

    private void clearPropertyTable() {
        nameEdit.setText("");
        classEdit.setText("");
        widgetEdit.setText("");
        iconEdit.setText("");
        jScrollPane1.getViewport().removeAll();
    }

    private void fillPreviewArea() {
        if (previewPanel != null) previewPanel.setText(generateXml());
    }

    /**
   * Save the component and method options.
   */
    private void saveComponentSettings() {
        if (selectedTreeNode != null) {
            selectedTreeNode.setUserName(nameEdit.getText());
            DefaultTableModel dm = (DefaultTableModel) componentPropertyTable.getModel();
            int rowCount = dm.getRowCount();
            ArrayList properties = new ArrayList();
            for (int i = 0; i < rowCount; i++) {
                Object[] values = new Object[7];
                values[0] = dm.getValueAt(i, PROPERTY);
                values[1] = dm.getValueAt(i, INCLUDED);
                values[2] = dm.getValueAt(i, MODE);
                values[3] = dm.getValueAt(i, GET);
                values[4] = dm.getValueAt(i, SET);
                values[5] = dm.getValueAt(i, TYPE);
                values[6] = dm.getValueAt(i, COMMENT);
                Object prop = selectedTreeNode.createProperty(values);
                properties.add(prop);
            }
            selectedTreeNode.setUserProperties(properties);
        }
    }

    private void setUpModeColumn(JTable table, TableColumn modeColumn) {
        JComboBox comboBox = new JComboBox();
        comboBox.addItem("Novice");
        comboBox.addItem("Normal");
        comboBox.addItem("Expert");
        modeColumn.setCellEditor(new DefaultCellEditor(comboBox));
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setToolTipText("Click for combo box");
        modeColumn.setCellRenderer(renderer);
    }

    /**
   * Adds the Java Beans in the jar file to the user panel.
   * First it loads all the Beans and resources into the JarClassLoader.
   * Then it determines which resources are beans (Java-Bean: True)
   * from the Manifest.
   *
   * @param fn Full path to the jar file.
   * @return true if a bean is added
   */
    private boolean addJarFile(String projectName, String fn) {
        JarRef jarRef = null;
        boolean libFound = false;
        for (int i = 0; i < libraries.size(); i++) {
            JarRef jr = libraries.elementAt(i);
            if ((jr.getJarPath() != null) && jr.getJarPath().equals(fn) && jr.getProjectName().equals(projectName)) {
                libFound = true;
                jarRef = jr;
                return false;
            }
        }
        if (!libFound) {
            jarRef = new JarRef();
            jarRef.setProjectName(projectName);
            jarRef.setJarPath(fn);
            libraries.addElement(jarRef);
        }
        JarClassLoader loader = JarClassLoader.getJarClassLoader();
        if (loader == null) {
            try {
                loader = new JarClassLoader(new URL("file:" + fn), getClass().getClassLoader());
                loader.setJarClassLoader(loader);
            } catch (Exception ex) {
                DebugLogger.logError("Bad Class Loader");
                ex.printStackTrace();
                return false;
            }
        } else loader.addJarFile(fn);
        JarFile jarfile;
        try {
            jarfile = new JarFile(fn);
        } catch (IOException ex) {
            DebugLogger.logError("Error opening jar file: " + fn);
            return false;
        }
        boolean beanAdded = false;
        DefaultMutableTreeNode jarNode = addJarNode(projectName + ":" + fn);
        Enumeration entries = jarfile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            String name = entry.getName();
            if (name.indexOf(".class") > 0) {
                Class clazz = getComponentFromJar(loader, name);
                if (clazz != null) addUserClass(jarNode, clazz, jarRef);
            }
        }
        return beanAdded;
    }

    /**
   * Add a node for the Jar file to the tree
   * @param fn the name of the jar file.
   * @return the new node
   */
    private DefaultMutableTreeNode addJarNode(String fn) {
        int pos = fn.lastIndexOf(File.separatorChar);
        String name = fn.substring(pos + 1);
        DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(name);
        root.add(newNode);
        return newNode;
    }

    /**
   * Adds the bean to the user tab on the panel. This method determines if the
   * bean is from a class or from a serializable file.
   *
   * @param parentNode the node to which beans are attached
   * @param loader ClassLoader to use for instantiation and class resolution
   * @param name Name of the bean and resource to use. The format should be the
       *             full path to the resouce i.e. sunw/demo/buttons/OurButton.class.
   */
    private void addUserClass(DefaultMutableTreeNode parentNode, Class clazz, JarRef jarRef) {
        ClassTreeNodeItem item = new ClassTreeNodeItem(clazz, expertMode, jarRef);
        if (item != null) {
            DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(item);
            parentNode.add(newNode);
            nodesTable.put(clazz.toString().substring(6), item);
        }
    }

    /**
   * The tree selection has changed so highlight the selected component
   * @param e
   */
    public void valueChanged(TreeSelectionEvent e) {
        if (!selectionChanging) {
            saveComponentSettings();
            saveSelection();
            selectionChanging = true;
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) componentTree.getLastSelectedPathComponent();
            if (node == null) {
                selectionChanging = false;
                return;
            }
            Object nodeInfo = node.getUserObject();
            if (nodeInfo instanceof ClassTreeNodeItem) {
                selectedTreeNode = (ClassTreeNodeItem) nodeInfo;
                fillPropertyTable();
            }
            selectionChanging = false;
        }
    }

    private String generateXml() {
        try {
            StringWriter sw = new StringWriter();
            Writer writer = new BufferedWriter(sw);
            NanoXmlWriter xmlWriter = new NanoXmlWriter(writer);
            XmlElement componentsElement = new NanoXmlElement("Components");
            XmlElement jarsElement = new NanoXmlElement("Jars");
            int numLibs = libraries.size();
            for (int i = 0; i < numLibs; i++) {
                JarRef jarRef = libraries.elementAt(i);
                String path = jarRef.getJarPath();
                if ((path != null) && (path.indexOf(".jar") > 0)) {
                    XmlElement jarElement = generateJarEntry(jarRef);
                    jarsElement.addChild(jarElement);
                }
            }
            componentsElement.addChild(jarsElement);
            Enumeration nodes = root.breadthFirstEnumeration();
            while (nodes.hasMoreElements()) {
                DefaultMutableTreeNode tn = (DefaultMutableTreeNode) nodes.nextElement();
                Object uo = tn.getUserObject();
                if (uo instanceof ClassTreeNodeItem) {
                    ClassTreeNodeItem btni = (ClassTreeNodeItem) uo;
                    XmlElement componentElement = generateClassEntry(btni);
                    componentsElement.addChild(componentElement);
                }
            }
            xmlWriter.write(componentsElement, true, 4);
            writer.flush();
            writer.close();
            sw.flush();
            sw.close();
            return sw.toString();
        } catch (IOException ex) {
            previewPanel.setText("Error generating file");
        }
        return null;
    }

    /**
   * Write the XML entries for an individual node
   * @param selectedTreeNode
   * @return the new XML element
   */
    private XmlElement generateClassEntry(ClassTreeNodeItem selectedTreeNode) {
        if (selectedTreeNode != null) {
            ArrayList userProperties = selectedTreeNode.getUserProperties();
            if (userProperties != null) {
                XmlElement componentElement = new NanoXmlElement("Component");
                componentElement.setAttribute("name", selectedTreeNode.getUserName());
                componentElement.setAttribute("class", selectedTreeNode.getUserClass().toString().substring(6));
                componentElement.setAttribute("icon", selectedTreeNode.getIconFile());
                componentElement.setAttribute("ui", selectedTreeNode.getUI());
                componentElement.setAttribute("jars", selectedTreeNode.getJarRef().getJarRef());
                Dimension d = selectedTreeNode.getPreferredSize();
                componentElement.setAttribute("size", "" + d.width + "x" + d.height);
                boolean include = false;
                for (int i = 0; i < userProperties.size(); i++) {
                    Object values[] = ((ClassTreeNodeItem.Property) userProperties.get(i)).values;
                    if (((Boolean) values[INCLUDED]).booleanValue()) {
                        String type = (String) values[TYPE];
                        XmlElement propertyElement = componentElement.createElement("Property");
                        propertyElement.setAttribute("name", (String) values[PROPERTY]);
                        propertyElement.setAttribute("mode", (String) values[MODE]);
                        if ((values[COMMENT] != null) && (((String) values[COMMENT]).length() > 0)) propertyElement.setAttribute("comment", (String) values[COMMENT]);
                        Boolean rm = (Boolean) values[GET];
                        Boolean wm = (Boolean) values[SET];
                        propertyElement.setAttribute("type", (rm.booleanValue() ? (wm.booleanValue() ? "both" : "set") : "get"));
                        componentElement.addChild(propertyElement);
                        XmlElement paramElement = propertyElement.createElement("Param");
                        paramElement.setAttribute("type", type);
                        propertyElement.addChild(paramElement);
                        include = true;
                    }
                }
                if (include) return componentElement;
            }
        }
        return null;
    }

    /**
   * Write the XML entries for an individual jar
   * @param path the path to the jar file
   * @return the new XML element
   */
    private XmlElement generateJarEntry(JarRef jarRef) {
        XmlElement componentElement = new NanoXmlElement("Jar");
        componentElement.setAttribute("path", jarRef.getJarPath());
        componentElement.setAttribute("project", jarRef.getProjectName());
        componentElement.setAttribute("name", jarRef.getJarRef());
        return componentElement;
    }

    /**
   * Save the Xml to file
   */
    private boolean saveXml() {
        saveComponentSettings();
        String saveFileName = fileName;
        try {
            File file = new File(fileName);
            if (file.exists()) {
                int ret = JOptionPane.showConfirmDialog(this, "The file exists already, do you want to overwrite the file?", "Save and overwrite", JOptionPane.OK_CANCEL_OPTION);
                if (ret == JOptionPane.CANCEL_OPTION) return false;
            }
            FileWriter fw = new FileWriter(fileName);
            Writer writer = new BufferedWriter(fw);
            writer.write(generateXml());
            writer.flush();
            fw.flush();
            writer.close();
            fw.close();
            EditorRegisteredComponentFactory.addConfigFile("User", fileName, true);
            EditorRegisteredComponentFactory.updateConfig();
            return true;
        } catch (IOException ex) {
            if (BuildProperties.DEBUG) DebugLogger.logError("Unable to write the project file");
        }
        return false;
    }

    private boolean getFileName() {
        JFileChooser chooser = new JFileChooser();
        AFileFilter myFilter = new AFileFilter("xml", "XML files");
        chooser.setFileFilter(myFilter);
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            fileName = chooser.getSelectedFile().toString();
            return true;
        }
        return false;
    }

    private Class getComponentFromJar(JarClassLoader loader, String fileName) {
        String className = "";
        try {
            className = fileName.substring(0, fileName.indexOf(".class")).replace('/', '.');
            Class clazz = loader.loadClass(className);
            if (Component.class.isAssignableFrom(clazz)) {
                if (Page.class.isAssignableFrom(clazz)) return null;
                Class[] args = new Class[0];
                if (clazz.getConstructor(args) != null) return clazz;
            }
        } catch (NoSuchMethodException nsme) {
            DebugLogger.logError("Component registration problem, constructor not found for: " + className);
        } catch (NoClassDefFoundError ncdfe) {
            DebugLogger.logError("Component registration problem, no classdef found for: " + className);
        } catch (Exception ex) {
            DebugLogger.logError("Component registration problem seting up: " + className + ", " + ex.getMessage() + ". " + ex.getCause());
        }
        return null;
    }

    private void saveSelection() {
        if (selectedTreeNode != null) {
            selectedTreeNode.setUserName(nameEdit.getText());
            selectedTreeNode.setIconFile(iconEdit.getText());
            int w = 100;
            int h = 20;
            try {
                w = Integer.parseInt(widthEdit.getText());
            } catch (NumberFormatException numberFormatException) {
            }
            try {
                h = Integer.parseInt(heightEdit.getText());
            } catch (NumberFormatException numberFormatException) {
            }
            if ((w != 100) || (h != 20)) selectedTreeNode.setPreferredSize(new Dimension(w, h));
            selectedTreeNode.setIconFile(iconEdit.getText());
        }
    }

    private void selectIconFile() {
        JFileChooser chooser = new JFileChooser();
        AFileFilter myFilter = new AFileFilter("gif", "GIF files");
        chooser.setFileFilter(myFilter);
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            iconEdit.setText(chooser.getSelectedFile().toString());
        }
    }
}
