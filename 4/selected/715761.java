package ostf.gui.frame;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Properties;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ostf.gui.model.TreeElement;
import ostf.gui.model.XmlFile;
import ostf.gui.model.XmlFileManager;
import ostf.gui.model.XmlTree;
import ostf.gui.model.XmlTreeNode;
import ostf.gui.transfer.CutAndPaste;
import ostf.gui.xerces.xml.DocumentImpl;
import ostf.gui.xerces.xml.XmlParser;
import ostf.gui.xerces.xsd.XmlSchema;
import ostf.gui.xerces.xsd.XmlSchemaManager;

public class TestPlanPanel implements TreeSelectionListener {

    public static Log logger = LogFactory.getLog(TestPlanPanel.class.getName());

    public static final String TESTPLAN_SCHEMA_LOCATION = "TestPlanEditor.schemaLocation";

    public static final String COMMENT_DISPLAYED = "TestPlanEditor.commentDisplayed";

    public static final String VALUE_DISPLAYED_ASATTR = "TestPlanEditor.valueDisplayedAsAttribute";

    public static final String ALSE_DISPLAYED_ASATTR = "TestPlanEditor.simpleElementDisplayedAsAttribute";

    public static final String DEFAULT_SCHEMA_LOCATION = "resource/testplan/ostf.xsd";

    public static final boolean DEFAULT_COMMENT_DISPLAYED = true;

    public static final boolean DEFAULT_VALUE_DISPLAYED_ASATTR = true;

    public static final boolean DEFAULT_ALSE_DISPLAYED_ASATTR = true;

    private static String schemaLocation = DEFAULT_SCHEMA_LOCATION;

    private DocumentImpl document;

    private XmlFile xmlFile;

    private XmlEditorPane editorPane;

    private XmlTree xmlTree;

    private CutAndPaste cutAndPaste;

    private boolean commentDisplayed = DEFAULT_COMMENT_DISPLAYED;

    private boolean valueDisplayedAsAttribute = DEFAULT_VALUE_DISPLAYED_ASATTR;

    private boolean alseDisplayedAsAttribute = DEFAULT_ALSE_DISPLAYED_ASATTR;

    private static PreferenceTreeNode preference;

    public TestPlanPanel(String fileName) {
        super();
        Properties props = AdminConsole.getInstance().getPreferenceDialog().getPreferences();
        commentDisplayed = Boolean.parseBoolean(props.getProperty(COMMENT_DISPLAYED, String.valueOf(DEFAULT_COMMENT_DISPLAYED)));
        valueDisplayedAsAttribute = Boolean.parseBoolean(props.getProperty(VALUE_DISPLAYED_ASATTR, String.valueOf(DEFAULT_VALUE_DISPLAYED_ASATTR)));
        alseDisplayedAsAttribute = Boolean.parseBoolean(props.getProperty(ALSE_DISPLAYED_ASATTR, String.valueOf(DEFAULT_ALSE_DISPLAYED_ASATTR)));
        xmlTree = new XmlTree(this);
        editorPane = new XmlEditorPane(this);
        xmlTree.addTreeSelectionListener(this);
        cutAndPaste = new CutAndPaste(xmlTree);
        xmlFile = XmlFileManager.getInstance().getXmlFile(fileName);
        if (xmlFile == null) {
            logger.warn("xmlFile is null and no xml document is loaded into the console");
            return;
        }
        document = xmlFile.getDocument();
        xmlTree.getXmlModel().setRootNode(document);
        logger.info(fileName + " was loaded into TestPlanPanel");
    }

    public static PreferenceTreeNode getPreference() {
        return preference;
    }

    public static String getSchemaLocation() {
        return schemaLocation;
    }

    public static void setSchemaLocation(String schemaLocation) {
        TestPlanPanel.schemaLocation = schemaLocation;
    }

    public static XmlSchema getXmlSchema() {
        return XmlSchemaManager.getInstance().getXmlSchema(new File(schemaLocation).getAbsolutePath());
    }

    public static XmlParser getXmlParser(boolean validate) {
        return XmlSchemaManager.getInstance().getXmlParser(validate ? new File(schemaLocation).getAbsolutePath() : null, DocumentImpl.class.getName());
    }

    public XmlFile getXmlFile() {
        return xmlFile;
    }

    public CutAndPaste getCutAndPaste() {
        return cutAndPaste;
    }

    public XmlTree getXmlTree() {
        return xmlTree;
    }

    public XmlEditorPane getEditorPane() {
        return editorPane;
    }

    public boolean isValueDisplayedAsAttribute() {
        return valueDisplayedAsAttribute;
    }

    public void setValueDisplayedAsAttribute(boolean vAsA) {
        valueDisplayedAsAttribute = vAsA;
    }

    public boolean isCommentDisplayed() {
        return commentDisplayed;
    }

    public void setCommentDisplayed(boolean display) {
        commentDisplayed = display;
    }

    public boolean isAlseDisplayedAsAttribute() {
        return alseDisplayedAsAttribute;
    }

    public void setAlseDisplayedAsAttribute(boolean aAsa) {
        alseDisplayedAsAttribute = aAsa;
    }

    public void redisplayEditorPane() {
        editorPane.redisplay();
    }

    public void redisplayTree() {
        xmlTree.getXmlModel().redisplayTree();
    }

    public void redisplayAll() {
        redisplayEditorPane();
        redisplayTree();
    }

    public void eraseEditorPane() {
        editorPane.displayNode(null);
    }

    public void fileChanged(XmlFile file) {
        document = file.getDocument();
        ((XmlTreeNode) xmlTree.getXmlModel().getRoot()).setNode(document);
        redisplayTree();
    }

    public void valueChanged(TreeSelectionEvent e) {
        TreePath path = (TreePath) e.getPath();
        XmlTreeNode treeNode = (XmlTreeNode) path.getLastPathComponent();
        TreeElement node = (TreeElement) treeNode.getNode();
        editorPane.displayNode(node);
    }

    public boolean close(boolean save) {
        if (save) {
            int result = this.checkSave(true);
            if (result == -2) {
                return false;
            }
        }
        XmlFileManager.getInstance().closeFile(xmlFile);
        AdminConsole.getInstance().removeTestPlanPanel(this);
        logger.info(xmlFile.getAbsolutePath() + " is closed");
        return true;
    }

    public int checkSave(boolean ask) {
        boolean modifiedOnDisk = xmlFile.lastModified < xmlFile.lastModified();
        if (!xmlFile.upToDate || modifiedOnDisk) {
            if (ask) {
                Object[] options = { "Yes", "No", "Yes to all", "No to all", "Cancel" };
                String message;
                if (!xmlFile.upToDate) {
                    message = "Modifications will be lost in file " + xmlFile.getAbsolutePath() + ".\nDo you want to save it ?";
                } else {
                    message = "File " + xmlFile.getAbsolutePath() + " was modified on disk but not in this editor.\nDo you want to save it ?";
                }
                int answer = JOptionPane.showOptionDialog(AdminConsole.getInstance(), message, "Unsaved file", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
                switch(answer) {
                    case 0:
                        if (!saveFile(xmlFile)) {
                            return -2;
                        }
                        break;
                    case 1:
                        break;
                    case 2:
                        if (!saveFile(xmlFile)) {
                            return -2;
                        }
                        ask = false;
                        break;
                    case 3:
                        return -1;
                    case 4:
                        return -2;
                }
            } else {
                if (!saveFile(xmlFile)) {
                    return -2;
                }
            }
        }
        if (ask) {
            return 0;
        } else {
            return 1;
        }
    }

    public void save() {
        checkSave(false);
    }

    private boolean saveFile(XmlFile file) {
        if ((file.lastModified < file.lastModified()) && !file.upToDate) {
            Object[] options = { "Yes", "No" };
            int answer = JOptionPane.showOptionDialog(AdminConsole.getInstance(), "This file was modified on disk.\nDo you want to overwrite it ?", "Modified file", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[1]);
            if (answer == 1) {
                return false;
            }
        }
        boolean result = file.save();
        logger.info(file.getAbsolutePath() + " is saved");
        if (result && xmlFile != null) {
            XmlFileManager.getInstance().closeFile(xmlFile);
        }
        return result;
    }

    public boolean nameAndSaveFile(XmlFile file) {
        XmlFile newFile = null;
        if (newFile != null) {
            if (newFile.exists()) {
                Object[] options = { "Yes", "No" };
                int answer = JOptionPane.showOptionDialog(AdminConsole.getInstance(), "This file already exists.\nDo you want to overwrite it ?", "Existing file", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[1]);
                if (answer == 1) {
                    return false;
                }
            }
            fileChanged(newFile);
            XmlFileManager.getInstance().closeFile(xmlFile);
            newFile.save();
            return true;
        }
        return false;
    }

    static {
        preference = new PreferenceTreeNode() {

            private static final long serialVersionUID = 5573676742447732167L;

            private JPanel preferencePanel;

            private JCheckBox commentDisplayed, valueDisplayed, alseDisplayed;

            private JRadioButton changeDisplay, redisplayPlan;

            private JTextField xsdLocation;

            private void buildUI() {
                preferencePanel = new JPanel();
                preferencePanel.setLayout(new BoxLayout(preferencePanel, BoxLayout.PAGE_AXIS));
                JPanel displayPref = new JPanel();
                displayPref.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Display Configuration"), BorderFactory.createEmptyBorder(4, 4, 4, 4)));
                displayPref.setLayout(new BoxLayout(displayPref, BoxLayout.PAGE_AXIS));
                JPanel panel1 = new JPanel(new FlowLayout(FlowLayout.LEADING));
                commentDisplayed = new JCheckBox();
                panel1.add(commentDisplayed);
                panel1.add(new JLabel("comment node in test plan displayed"));
                JPanel panel2 = new JPanel(new FlowLayout(FlowLayout.LEADING));
                valueDisplayed = new JCheckBox();
                panel2.add(valueDisplayed);
                panel2.add(new JLabel("#value node in test plan displayed as an attribute in parent element"));
                JPanel panel3 = new JPanel(new FlowLayout(FlowLayout.LEADING));
                alseDisplayed = new JCheckBox();
                panel3.add(alseDisplayed);
                panel3.add(new JLabel("attribute like element(containes only one #value or attribute) in test plan displayed as an attribute in parent element"));
                JPanel panel4 = new JPanel(new FlowLayout(FlowLayout.LEADING));
                changeDisplay = new JRadioButton("change for all test plans", true);
                redisplayPlan = new JRadioButton("change for current test plan");
                ButtonGroup group = new ButtonGroup();
                group.add(changeDisplay);
                group.add(redisplayPlan);
                panel4.add(changeDisplay);
                panel4.add(redisplayPlan);
                displayPref.add(Box.createRigidArea(new Dimension(0, 5)));
                displayPref.add(panel1);
                displayPref.add(panel2);
                displayPref.add(panel3);
                displayPref.add(panel4);
                preferencePanel.add(Box.createRigidArea(new Dimension(0, 10)));
                preferencePanel.add(displayPref);
                JPanel panel5 = new JPanel(new FlowLayout(FlowLayout.LEADING));
                xsdLocation = new JTextField();
                JButton button = new JButton("change");
                button.setPreferredSize(new Dimension(85, 20));
                button.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        String location = ".";
                        if (xsdLocation.getText() != null && xsdLocation.getText().trim().length() > 0) location = xsdLocation.getText().trim();
                        JFileChooser fileChooser = new JFileChooser(location);
                        int returnValue = fileChooser.showOpenDialog(AdminConsole.getInstance().getPreferenceDialog());
                        if (returnValue == JFileChooser.APPROVE_OPTION) {
                            xsdLocation.setText(fileChooser.getSelectedFile().getAbsolutePath());
                        }
                    }
                });
                panel5.add(new JLabel("test plan xml schema location : "));
                panel5.add(xsdLocation);
                panel5.add(button);
                preferencePanel.add(Box.createRigidArea(new Dimension(0, 10)));
                preferencePanel.add(panel5);
                preferencePanel.add(Box.createRigidArea(new Dimension(0, 150)));
                this.setUserObject("TestPlanEditor");
            }

            public JPanel showPreferencePane() {
                if (preferencePanel == null) buildUI();
                return preferencePanel;
            }

            public void save(Properties preferences) {
                if (changeDisplay.isSelected()) {
                    preferences.setProperty(COMMENT_DISPLAYED, String.valueOf(commentDisplayed.isSelected()));
                    preferences.setProperty(VALUE_DISPLAYED_ASATTR, String.valueOf(valueDisplayed.isSelected()));
                    preferences.setProperty(ALSE_DISPLAYED_ASATTR, String.valueOf(alseDisplayed.isSelected()));
                    redisplayTestPlanPanels(preferences);
                } else {
                    TestPlanPanel panel = AdminConsole.getInstance().getCurrentTestPlanPanel();
                    if (panel != null) {
                        panel.setCommentDisplayed(commentDisplayed.isSelected());
                        panel.setValueDisplayedAsAttribute(valueDisplayed.isSelected());
                        panel.setAlseDisplayedAsAttribute(alseDisplayed.isSelected());
                        panel.redisplayAll();
                        commentDisplayed.setSelected(Boolean.parseBoolean(preferences.getProperty(COMMENT_DISPLAYED, String.valueOf(DEFAULT_COMMENT_DISPLAYED))));
                        valueDisplayed.setSelected(Boolean.parseBoolean(preferences.getProperty(VALUE_DISPLAYED_ASATTR, String.valueOf(DEFAULT_VALUE_DISPLAYED_ASATTR))));
                        alseDisplayed.setSelected(Boolean.parseBoolean(preferences.getProperty(ALSE_DISPLAYED_ASATTR, String.valueOf(DEFAULT_ALSE_DISPLAYED_ASATTR))));
                        changeDisplay.setSelected(true);
                    }
                }
                schemaLocation = xsdLocation.getText();
                preferences.setProperty(TESTPLAN_SCHEMA_LOCATION, schemaLocation);
            }

            public void load(Properties preferences) {
                if (preferencePanel == null) buildUI();
                commentDisplayed.setSelected(Boolean.parseBoolean(preferences.getProperty(COMMENT_DISPLAYED, String.valueOf(DEFAULT_COMMENT_DISPLAYED))));
                valueDisplayed.setSelected(Boolean.parseBoolean(preferences.getProperty(VALUE_DISPLAYED_ASATTR, String.valueOf(DEFAULT_VALUE_DISPLAYED_ASATTR))));
                alseDisplayed.setSelected(Boolean.parseBoolean(preferences.getProperty(ALSE_DISPLAYED_ASATTR, String.valueOf(DEFAULT_ALSE_DISPLAYED_ASATTR))));
                schemaLocation = preferences.getProperty(TESTPLAN_SCHEMA_LOCATION, DEFAULT_SCHEMA_LOCATION);
                xsdLocation.setText(schemaLocation);
                redisplayTestPlanPanels(preferences);
            }

            public void restoreDefaults() {
                commentDisplayed.setSelected(DEFAULT_COMMENT_DISPLAYED);
                valueDisplayed.setSelected(DEFAULT_VALUE_DISPLAYED_ASATTR);
                alseDisplayed.setSelected(DEFAULT_ALSE_DISPLAYED_ASATTR);
                changeDisplay.setSelected(true);
                xsdLocation.setText(DEFAULT_SCHEMA_LOCATION);
            }

            private void redisplayTestPlanPanels(Properties preferences) {
                TestPlanPanel[] panels = AdminConsole.getInstance().getAllTestPlanPanels();
                for (int i = 0; panels != null && i < panels.length; i++) {
                    panels[i].setCommentDisplayed(Boolean.parseBoolean(preferences.getProperty(COMMENT_DISPLAYED, String.valueOf(DEFAULT_COMMENT_DISPLAYED))));
                    panels[i].setValueDisplayedAsAttribute(Boolean.parseBoolean(preferences.getProperty(VALUE_DISPLAYED_ASATTR, String.valueOf(DEFAULT_VALUE_DISPLAYED_ASATTR))));
                    panels[i].setAlseDisplayedAsAttribute(Boolean.parseBoolean(preferences.getProperty(ALSE_DISPLAYED_ASATTR, String.valueOf(DEFAULT_ALSE_DISPLAYED_ASATTR))));
                    panels[i].redisplayAll();
                }
            }
        };
        AdminConsole.getInstance().registerPreferences(preference);
        schemaLocation = AdminConsole.getInstance().getPreference(TESTPLAN_SCHEMA_LOCATION, DEFAULT_SCHEMA_LOCATION);
    }

    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                JFrame.setDefaultLookAndFeelDecorated(true);
                JFrame frame = new JFrame("TestPlanPreference");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setContentPane(preference.showPreferencePane());
                frame.pack();
                frame.setVisible(true);
            }
        });
    }
}
