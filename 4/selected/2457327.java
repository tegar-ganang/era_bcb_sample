package org.personalsmartspace.cm.gui.impl;

import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import org.personalsmartspace.cm.api.pss3p.ContextException;
import org.personalsmartspace.cm.api.pss3p.ContextModelException;
import org.personalsmartspace.cm.api.pss3p.MalformedCtxIdentifierException;
import org.personalsmartspace.cm.broker.api.platform.ICtxBroker;
import org.personalsmartspace.cm.model.api.pss3p.CtxModelType;
import org.personalsmartspace.cm.model.api.pss3p.CtxOriginType;
import org.personalsmartspace.cm.model.api.pss3p.ICtxAssociation;
import org.personalsmartspace.cm.model.api.pss3p.ICtxAssociationIdentifier;
import org.personalsmartspace.cm.model.api.pss3p.ICtxAttribute;
import org.personalsmartspace.cm.model.api.pss3p.ICtxAttributeIdentifier;
import org.personalsmartspace.cm.model.api.pss3p.ICtxEntity;
import org.personalsmartspace.cm.model.api.pss3p.ICtxEntityIdentifier;
import org.personalsmartspace.cm.model.api.pss3p.ICtxIdentifier;
import org.personalsmartspace.cm.model.api.pss3p.ICtxModelObject;
import org.personalsmartspace.cm.model.api.pss3p.ICtxQuality;
import org.personalsmartspace.log.impl.PSSLog;
import org.personalsmartspace.pss_psm_pssmanager.api.platform.IPssManager;
import org.personalsmartspace.pss_psm_pssmanager.api.platform.PSSInfo;
import org.personalsmartspace.pss_psm_pssmanager.api.pss3p.PssManagerException;
import org.personalsmartspace.spm.access.api.platform.AccessControlException;
import org.personalsmartspace.spm.access.api.platform.CtxPermission;
import org.personalsmartspace.spm.access.api.platform.IAccessControlDecision;
import org.personalsmartspace.spm.access.api.platform.IAccessControlDecisionMgr;
import org.personalsmartspace.spm.identity.api.platform.IIdentityManagement;
import org.personalsmartspace.spm.identity.api.platform.MalformedDigitialPersonalIdentifierException;
import org.personalsmartspace.sre.api.pss3p.IDigitalPersonalIdentifier;
import javax.swing.table.DefaultTableModel;
import javax.swing.JTabbedPane;
import java.awt.GridBagConstraints;

/**
 * @author <a href="mailto:nikoskal@users.sourceforge.net">Nikos Kalatzis</a> (ICCS)
 *
 */
public class ContextAdminGUI extends javax.swing.JPanel {

    private static final long serialVersionUID = 3396028178515969L;

    PSSLog log = new PSSLog(this);

    private JPanel jPanelInput = null;

    private JTextField jTextFieldContextID = null;

    private JLabel jLabelContextID = null;

    private JButton jButtonGetModelObject = null;

    private JList jTextPaneOutput = null;

    private JLabel jLabelOutputResults = null;

    private JButton jButtonCreateModelObject = null;

    private JButton jButtonDeleteModObj = null;

    private JButton jButtonClear = null;

    private JTextField jTextFieldModelObjectType = null;

    private JLabel jLabelModelObjType = null;

    private JTextField jTextFieldStatus = null;

    private JLabel jLabelStatus = null;

    private JButton jButtonAddAttribute = null;

    private JTextField jTextFieldnewAttributeType = null;

    private JTextField jTextFieldnewAttributeValue = null;

    private JButton jButtonUpdateAttribute = null;

    private JButton jButtonRemoveAttribute = null;

    private JCheckBox jCheckBoxHistory = null;

    private JLabel jLabelHistory = null;

    private JComboBox jComboBoxDataType = null;

    private JComboBox jComboBoxTargetVid = null;

    private JLabel jLabelDataTypeMenu = null;

    private JButton exportHoCjButton = null;

    private JButton jButtonGetOperator = null;

    private JLabel jLabelTargetDPI = null;

    private JButton jButtonGetAttribute = null;

    private ICtxEntity globalEntity = null;

    private ICtxBroker ctxBroker;

    private IIdentityManagement identMgmt;

    private IPssManager pssManager;

    private JScrollPane jTextPaneOutputScrollPane = null;

    private JLabel jLabelReadAccess = null;

    private JLabel jLabelWriteAccess = null;

    private JCheckBox jCheckBoxWriteAccess = null;

    private JCheckBox jCheckBoxReadAccess = null;

    private JComboBox jComboBoxRequestorDPI = null;

    public String reqDpiString = null;

    private JScrollPane jScrollPaneTable = null;

    private JTable jTableAttributes = null;

    private String[] columnNames = { "attribute id", "attribute type", "attribute value", "QoC_params", "Ctx Source ID" };

    DefaultTableModel model = new DefaultTableModel(columnNames, 10);

    private String[] columnAssocTableNames = { "association id", "association type", "Parent entity" };

    DefaultTableModel modelAssoc = new DefaultTableModel(columnAssocTableNames, 10);

    private String[] columnAssocEntMembersTableNames = { "Entity id", "Entity type", "Parent" };

    DefaultTableModel modelAssocEntMembers = new DefaultTableModel(columnAssocEntMembersTableNames, 10);

    private JButton jButtonRefresh = null;

    ContextAdminGUI() {
        initComponents();
    }

    private IAccessControlDecisionMgr decisionMgr;

    private JLabel jLabelRequestorDPI = null;

    private JButton jButtonAddPermision = null;

    private JTabbedPane jTabbedPane = null;

    private JPanel jAttributePanel = null;

    private JPanel jAssociationPanel = null;

    private JScrollPane jAssociationsTableScrollPane = null;

    private JTable jAssociationsTable = null;

    private JTextField jTextFieldAssocID = null;

    private JTextField jTextFieldAssocValue = null;

    private JLabel jAssociationIDLabel = null;

    private JLabel jAssociationTypeLabel = null;

    private JButton jSearchAssocButton = null;

    private JButton jCreateAssocButton = null;

    private JButton jRemoveAssocButton = null;

    private JScrollPane jScrollPaneAssocEntMembers = null;

    private JTable jTableAssocEntMembers = null;

    private JLabel jLabelAssocTable = null;

    private JLabel jLabelAssocEntMembers = null;

    private JButton jRetrieveAssocButton = null;

    private JButton jAddEntToAssocButton = null;

    private JCheckBox jCheckBoxIsParent = null;

    private JLabel jLabelIsParent = null;

    private JButton jButtonRemoveEntFromAssociation = null;

    private JTextField jTextFieldEntityIDForAssociation = null;

    private JLabel jLabelEntityIDForAssociation = null;

    ContextAdminGUI(ICtxBroker ctxBroker, IIdentityManagement identMgmt, IAccessControlDecisionMgr decisionMgr, IPssManager pssManager) {
        this.ctxBroker = ctxBroker;
        this.identMgmt = identMgmt;
        this.decisionMgr = decisionMgr;
        this.pssManager = pssManager;
        initComponents();
    }

    ContextAdminGUI(ICtxBroker ctxBroker, IIdentityManagement identMgmt) {
        this.ctxBroker = ctxBroker;
        this.identMgmt = identMgmt;
        initComponents();
    }

    private void initComponents() {
        jLabelStatus = new JLabel();
        jLabelStatus.setBounds(new Rectangle(11, 439, 70, 16));
        jLabelStatus.setText("Status");
        jLabelModelObjType = new JLabel();
        jLabelModelObjType.setBounds(new Rectangle(9, 41, 84, 20));
        jLabelModelObjType.setText("Entity Type");
        jLabelOutputResults = new JLabel();
        jLabelOutputResults.setBounds(new Rectangle(478, 3, 151, 16));
        jLabelOutputResults.setText("Retrieved Entity IDs");
        jLabelContextID = new JLabel();
        jLabelContextID.setBounds(new Rectangle(8, 18, 76, 16));
        jLabelContextID.setText("Entity ID");
        jLabelTargetDPI = new JLabel();
        jLabelTargetDPI.setBounds(new Rectangle(9, 66, 83, 16));
        jLabelTargetDPI.setText("Target DPI");
        this.setVisible(true);
        this.setLayout(null);
        this.add(getJTextFieldContextID(), null);
        this.add(jLabelContextID, null);
        this.add(getJButtonGetModelObject(), null);
        this.add(jLabelOutputResults, null);
        this.add(getJButtonCreateModelObject(), null);
        this.add(getJButtonDeleteModObj(), null);
        this.add(getJButtonClear(), null);
        this.add(getJTextFieldModelObjectType(), null);
        this.add(jLabelModelObjType, null);
        this.add(getJTextFieldStatus(), null);
        this.add(jLabelStatus, null);
        this.add(getJComboBoxTagetVid(), null);
        this.add(getJButtonGetOperator(), null);
        this.add(jLabelTargetDPI, null);
        this.add(getJTextPaneOutputScrollPane(), null);
        this.add(getJButtonRefresh(), null);
        this.add(getJTabbedPane(), null);
    }

    /**
     * This method initializes jPanelInput      
     *  
     * @return javax.swing.JPanel       
     */
    private JPanel getJPanelInput() {
        if (jPanelInput == null) {
            jPanelInput = new JPanel();
            jPanelInput.setLayout(new GridBagLayout());
            jPanelInput.setBounds(new Rectangle(359, 5, 0, 0));
        }
        return jPanelInput;
    }

    /**
     * This method initializes jTextFieldContextID      
     *  
     * @return javax.swing.JTextField   
     */
    private JTextField getJTextFieldContextID() {
        if (jTextFieldContextID == null) {
            jTextFieldContextID = new JTextField();
            jTextFieldContextID.setBounds(new Rectangle(93, 17, 273, 20));
        }
        return jTextFieldContextID;
    }

    /**
     * This method initializes jButtonGetModelObject    
     *  
     * @return javax.swing.JButton      
     */
    public JButton getJButtonGetModelObject() {
        if (jButtonGetModelObject == null) {
            jButtonGetModelObject = new JButton();
            jButtonGetModelObject.setBounds(new Rectangle(116, 102, 84, 26));
            jButtonGetModelObject.setText("GetEntity");
            jButtonGetModelObject.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent e) {
                    jComboBoxTargetVid.setEnabled(true);
                    jTextFieldStatus.setText("");
                    if (jTextFieldContextID.getText().isEmpty() && !jTextFieldModelObjectType.getText().isEmpty()) {
                        List<ICtxIdentifier> list = null;
                        try {
                            String targetDpiString = jComboBoxTargetVid.getSelectedItem().toString();
                            IDigitalPersonalIdentifier targetDPI = null;
                            if (targetDpiString.equals("null")) {
                                list = ctxBroker.lookup(CtxModelType.ENTITY, jTextFieldModelObjectType.getText().trim());
                            }
                            if (!targetDpiString.equals("null")) {
                                targetDPI = identMgmt.parseDigitalPersonalIdentifier(targetDpiString);
                                list = ctxBroker.lookup(targetDPI, CtxModelType.ENTITY, jTextFieldModelObjectType.getText().trim());
                            }
                            Vector<String> list2 = new Vector<String>();
                            for (ICtxIdentifier ident : list) {
                                list2.add(ident.toString());
                            }
                            jTextFieldModelObjectType.setText("");
                            jTextPaneOutput.setListData(list2);
                            jTextFieldStatus.setText("list of Entities retrieved");
                        } catch (ContextException e1) {
                            log.error("Error on context lookup " + e1.getLocalizedMessage());
                        } catch (MalformedDigitialPersonalIdentifierException e3) {
                            log.error("Error on dpi parsing " + e3.getLocalizedMessage());
                        }
                    }
                    if (!jTextFieldContextID.getText().isEmpty() && jTextFieldModelObjectType.getText().isEmpty()) {
                        ICtxIdentifier identifier;
                        try {
                            identifier = ctxBroker.parseIdentifier(jTextFieldContextID.getText().trim());
                            ICtxEntity entity = (ICtxEntity) ctxBroker.retrieve(identifier);
                            jTextFieldStatus.setText("RETRIEVED :" + entity.getCtxIdentifier().toString());
                            globalEntity = entity;
                            entity = null;
                            populateOperatorAttributesList(globalEntity);
                            populateOperatorAssociationsList(globalEntity);
                        } catch (MalformedCtxIdentifierException e1) {
                            log.error("Error on context identifier " + e1.getLocalizedMessage());
                        } catch (ContextException e1) {
                            log.error("Error on context retrieval " + e1.getLocalizedMessage());
                            e1.printStackTrace();
                        }
                    }
                }
            });
        }
        return jButtonGetModelObject;
    }

    private JScrollPane getJTextPaneOutputScrollPane() {
        if (jTextPaneOutputScrollPane == null) {
            jTextPaneOutputScrollPane = new JScrollPane(getJTextPaneOutput());
            jTextPaneOutputScrollPane.setBounds(new Rectangle(375, 16, 333, 80));
        }
        return jTextPaneOutputScrollPane;
    }

    /**
     * This method initializes jTextPaneOutput  
     *  
     * @return javax.swing.JTextPane    
     */
    private JList getJTextPaneOutput() {
        if (jTextPaneOutput == null) {
            jTextPaneOutput = new JList();
            jTextPaneOutput.setBounds(new Rectangle(374, 18, 333, 81));
            jTextPaneOutput.addMouseListener(new java.awt.event.MouseAdapter() {

                public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (e.getClickCount() > 1) {
                        String id = (String) jTextPaneOutput.getSelectedValue();
                        globalEntity = null;
                        jTextFieldContextID.setText("");
                        jTextFieldModelObjectType.setText("");
                        jTextFieldnewAttributeType.setText("");
                        jTextFieldnewAttributeValue.setText("");
                        jTextFieldContextID.setText(id);
                        jTextFieldModelObjectType.setText("");
                        jTextFieldStatus.setText("Entity Id selected, press 'Get Entity' button to retrieve entity");
                    }
                }
            });
        }
        return jTextPaneOutput;
    }

    /**
     * This method initializes jButtonCreateModelObject 
     *  
     * @return javax.swing.JButton      
     */
    private JButton getJButtonCreateModelObject() {
        if (jButtonCreateModelObject == null) {
            jButtonCreateModelObject = new JButton();
            jButtonCreateModelObject.setBounds(new Rectangle(8, 102, 106, 26));
            jButtonCreateModelObject.setText("CreateEntity");
            jButtonCreateModelObject.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent e) {
                    try {
                        ICtxEntity entity = ctxBroker.createEntity(jTextFieldModelObjectType.getText().trim());
                        jTextFieldModelObjectType.setText("");
                        jTextFieldStatus.setText("CREATED :" + entity.getCtxIdentifier().toString());
                    } catch (ContextException e2) {
                        log.error("Context related error when creating entity  " + e2.getLocalizedMessage());
                    }
                }
            });
        }
        return jButtonCreateModelObject;
    }

    /**
     * This method initializes jButtonDeleteModObj      
     *  
     * @return javax.swing.JButton      
     */
    private JButton getJButtonDeleteModObj() {
        if (jButtonDeleteModObj == null) {
            jButtonDeleteModObj = new JButton();
            jButtonDeleteModObj.setBounds(new Rectangle(203, 102, 101, 26));
            jButtonDeleteModObj.setText("DeleteEntity");
            jButtonDeleteModObj.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent e) {
                    try {
                        ctxBroker.remove(globalEntity.getCtxIdentifier());
                        String delID = globalEntity.getCtxIdentifier().toString();
                        clearAll();
                        jTextFieldStatus.setText("DELETED :" + delID);
                    } catch (ContextException e1) {
                        log.error("Error when removing context entity " + e1.getLocalizedMessage());
                    }
                }
            });
        }
        return jButtonDeleteModObj;
    }

    /**
     * This method initializes jButtonClear     
     *  
     * @return javax.swing.JButton      
     */
    private JButton getJButtonClear() {
        if (jButtonClear == null) {
            jButtonClear = new JButton();
            jButtonClear.setBounds(new Rectangle(603, 427, 79, 33));
            jButtonClear.setText("Clear");
            jButtonClear.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent e) {
                    clearAll();
                }
            });
        }
        return jButtonClear;
    }

    void clearAll() {
        globalEntity = null;
        jTextFieldContextID.setText("");
        jTextFieldModelObjectType.setText("");
        jTextPaneOutput.setListData(new Vector());
        ((DefaultTableModel) jTableAttributes.getModel()).setRowCount(0);
        ((DefaultTableModel) jTableAttributes.getModel()).setRowCount(2);
        ((DefaultTableModel) jTableAssocEntMembers.getModel()).setRowCount(0);
        ((DefaultTableModel) jTableAssocEntMembers.getModel()).setRowCount(2);
        ((DefaultTableModel) jAssociationsTable.getModel()).setRowCount(0);
        ((DefaultTableModel) jAssociationsTable.getModel()).setRowCount(2);
        jTextFieldStatus.setText("cleared");
        jTextFieldnewAttributeType.setText("");
        jTextFieldnewAttributeValue.setText("");
        jCheckBoxHistory.setSelected(false);
        jCheckBoxHistory.setEnabled(true);
        jComboBoxDataType.setSelectedItem("String");
        jComboBoxDataType.setEnabled(true);
        jComboBoxTargetVid.setSelectedItem("null");
        jComboBoxTargetVid.setEnabled(true);
        jTextFieldAssocValue.setText("");
        jTextFieldAssocID.setText("");
        jTextFieldEntityIDForAssociation.setText("");
        jCheckBoxIsParent.setSelected(false);
    }

    /**
     * This method initializes jTextFieldModelObjectType        
     *  
     * @return javax.swing.JTextField   
     */
    private JTextField getJTextFieldModelObjectType() {
        if (jTextFieldModelObjectType == null) {
            jTextFieldModelObjectType = new JTextField();
            jTextFieldModelObjectType.setBounds(new Rectangle(97, 42, 266, 20));
            jTextFieldModelObjectType.addKeyListener(new java.awt.event.KeyAdapter() {

                public void keyTyped(java.awt.event.KeyEvent e) {
                    jComboBoxTargetVid.setEnabled(true);
                }
            });
        }
        return jTextFieldModelObjectType;
    }

    /**
     * This method initializes jScrollPaneTable 
     *  
     * @return javax.swing.JScrollPane  
     */
    private JScrollPane getJScrollPaneTable() {
        if (jScrollPaneTable == null) {
            jScrollPaneTable = new JScrollPane(getJTableAttributes(), ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
            jScrollPaneTable.setBounds(new Rectangle(5, 5, 670, 100));
            jScrollPaneTable.setViewportView(getJTableAttributes());
        }
        return jScrollPaneTable;
    }

    /**
     * This method initializes jTableAttributes 
     *  
     * @return javax.swing.JTable       
     */
    private JTable getJTableAttributes() {
        if (jTableAttributes == null) {
            jTableAttributes = new JTable(this.model) {

                private static final long serialVersionUID = 1L;

                public boolean isCellEditable(int rowIndex, int colIndex) {
                    return false;
                }
            };
            jTableAttributes.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            jTableAttributes.addMouseListener(new java.awt.event.MouseAdapter() {

                public void mouseClicked(java.awt.event.MouseEvent e) {
                    int row = jTableAttributes.rowAtPoint(e.getPoint());
                    int col = jTableAttributes.columnAtPoint(e.getPoint());
                    String selecteAttrIdString = (String) jTableAttributes.getValueAt(row, 0);
                    jTextFieldnewAttributeType.setText(selecteAttrIdString);
                    Object attrValue = jTableAttributes.getValueAt(row, 2);
                    String attrValueStr = null;
                    if (attrValue != null) {
                        attrValueStr = new String(attrValue.toString());
                        jTextFieldnewAttributeValue.setText(attrValueStr);
                    }
                    if (attrValue == null) {
                        jTextFieldnewAttributeValue.setText("");
                        log.warn("value is null");
                    }
                    ICtxIdentifier selecteAttrId;
                    try {
                        selecteAttrId = ctxBroker.parseIdentifier(selecteAttrIdString);
                        ICtxAttribute selectedAttr = (ICtxAttribute) ctxBroker.retrieve(selecteAttrId);
                        if (selectedAttr.isHistoryRecorded()) jCheckBoxHistory.setSelected(true);
                        if (!selectedAttr.isHistoryRecorded()) jCheckBoxHistory.setSelected(false);
                    } catch (ContextException e1) {
                        e1.printStackTrace();
                    }
                }
            });
        }
        return jTableAttributes;
    }

    /**
     * This method initializes jTextFieldStatus 
     *  
     * @return javax.swing.JTextField   
     */
    private JTextField getJTextFieldStatus() {
        if (jTextFieldStatus == null) {
            jTextFieldStatus = new JTextField();
            jTextFieldStatus.setBounds(new Rectangle(83, 439, 496, 20));
        }
        return jTextFieldStatus;
    }

    /**
     * This method initializes jButtonAddAttribute      
     *  
     * @return javax.swing.JButton      
     */
    private JButton getJButtonAddAttribute() {
        if (jButtonAddAttribute == null) {
            jButtonAddAttribute = new JButton();
            jButtonAddAttribute.setText("add Attribute");
            jButtonAddAttribute.setBounds(new Rectangle(150, 175, 132, 24));
            jButtonAddAttribute.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent e) {
                    ICtxAttribute attribute = null;
                    try {
                        if (jTextFieldnewAttributeValue.getText().isEmpty()) {
                            attribute = ctxBroker.createAttribute((ICtxEntityIdentifier) globalEntity.getCtxIdentifier(), jTextFieldnewAttributeType.getText().trim());
                        }
                        if (!jTextFieldnewAttributeValue.getText().isEmpty()) {
                            if (jTextFieldnewAttributeType.getText().contains(globalEntity.getCtxIdentifier().toString())) {
                                jTextFieldStatus.setText("Incorect attribute type");
                                jTextFieldnewAttributeType.setText("");
                                jTextFieldnewAttributeValue.setText("");
                                return;
                            }
                            String dataType = jComboBoxDataType.getSelectedItem().toString();
                            if (jCheckBoxHistory.isSelected()) attribute.setHistoryRecorded(true);
                            if (dataType.equalsIgnoreCase("String")) {
                                attribute = ctxBroker.createAttribute((ICtxEntityIdentifier) globalEntity.getCtxIdentifier(), jTextFieldnewAttributeType.getText().trim(), new String(jTextFieldnewAttributeValue.getText().trim()));
                            }
                            if (dataType.equalsIgnoreCase("Integer")) {
                                attribute = ctxBroker.createAttribute((ICtxEntityIdentifier) globalEntity.getCtxIdentifier(), jTextFieldnewAttributeType.getText().trim(), new Integer(jTextFieldnewAttributeValue.getText().trim()));
                            }
                            if (dataType.equalsIgnoreCase("Double")) {
                                attribute = ctxBroker.createAttribute((ICtxEntityIdentifier) globalEntity.getCtxIdentifier(), jTextFieldnewAttributeType.getText().trim(), new Double(jTextFieldnewAttributeValue.getText().trim()));
                            }
                            attribute.setSourceId("USER");
                            ctxBroker.update(attribute);
                        }
                        globalEntity = (ICtxEntity) ctxBroker.retrieve(globalEntity.getCtxIdentifier());
                        jTextFieldnewAttributeType.setText("");
                        jTextFieldnewAttributeValue.setText("");
                        populateOperatorAttributesList(globalEntity);
                        jTextFieldStatus.setText("ADDED Attribute " + attribute.getCtxIdentifier().toString());
                    } catch (ContextException e1) {
                        jTextFieldnewAttributeType.setText("");
                        jTextFieldnewAttributeValue.setText("");
                        getJTextFieldStatus().setText("incorrect types");
                        e1.printStackTrace();
                    }
                }
            });
        }
        return jButtonAddAttribute;
    }

    /**
     * This method initializes jTextFieldnewAttributeType       
     *  
     * @return javax.swing.JTextField   
     */
    private JTextField getJTextFieldnewAttributeType() {
        if (jTextFieldnewAttributeType == null) {
            jTextFieldnewAttributeType = new JTextField();
            jTextFieldnewAttributeType.setBounds(new Rectangle(6, 145, 503, 20));
            jTextFieldnewAttributeType.addKeyListener(new java.awt.event.KeyAdapter() {

                public void keyTyped(java.awt.event.KeyEvent e) {
                    jComboBoxDataType.setEnabled(true);
                    jCheckBoxHistory.setEnabled(true);
                }
            });
        }
        return jTextFieldnewAttributeType;
    }

    /**
     * This method initializes jTextFieldnewAttributeValue      
     *  
     * @return javax.swing.JTextField   
     */
    private JTextField getJTextFieldnewAttributeValue() {
        if (jTextFieldnewAttributeValue == null) {
            jTextFieldnewAttributeValue = new JTextField();
            jTextFieldnewAttributeValue.setBounds(new Rectangle(512, 145, 172, 20));
            jTextFieldnewAttributeValue.addKeyListener(new java.awt.event.KeyAdapter() {

                public void keyTyped(java.awt.event.KeyEvent e) {
                    jComboBoxDataType.setEnabled(true);
                    jCheckBoxHistory.setEnabled(true);
                }
            });
        }
        return jTextFieldnewAttributeValue;
    }

    /**
     * This method initializes jButtonUpdateAttribute   
     *  
     * @return javax.swing.JButton      
     */
    private JButton getJButtonUpdateAttribute() {
        if (jButtonUpdateAttribute == null) {
            jButtonUpdateAttribute = new JButton();
            jButtonUpdateAttribute.setText("update Attribute");
            jButtonUpdateAttribute.setBounds(new Rectangle(294, 175, 132, 24));
            jButtonUpdateAttribute.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent e) {
                    String attributeID = jTextFieldnewAttributeType.getText().trim();
                    ICtxAttribute attribute = null;
                    try {
                        attribute = (ICtxAttribute) ctxBroker.retrieve(ctxBroker.parseIdentifier(attributeID));
                        String newValueType = jComboBoxDataType.getSelectedItem().toString();
                        String updateValue = jTextFieldnewAttributeValue.getText().toString();
                        if (newValueType.equalsIgnoreCase("String")) {
                            attribute.setStringValue(updateValue);
                        }
                        if (newValueType.equalsIgnoreCase("Integer")) {
                            Integer i = new Integer(updateValue);
                            attribute.setIntegerValue(i);
                        }
                        if (newValueType.equalsIgnoreCase("Double")) {
                            attribute.setDoubleValue(new Double(updateValue));
                        }
                        if (jCheckBoxHistory.isSelected()) attribute.setHistoryRecorded(true);
                        if (!jCheckBoxHistory.isSelected()) attribute.setHistoryRecorded(false);
                        String tempType = attribute.getType();
                        attribute.setSourceId("USER");
                        ctxBroker.update(attribute);
                        if (globalEntity != null) {
                            globalEntity = (ICtxEntity) ctxBroker.retrieve(globalEntity.getCtxIdentifier());
                            jTextFieldnewAttributeType.setText("");
                            jTextFieldnewAttributeValue.setText("");
                            populateOperatorAttributesList(globalEntity);
                        }
                        if (globalEntity == null) {
                            String targetDpiString = jComboBoxTargetVid.getSelectedItem().toString();
                            List<ICtxIdentifier> attrlist = null;
                            try {
                                if (targetDpiString != "null") attrlist = ctxBroker.lookup(identMgmt.parseDigitalPersonalIdentifier(targetDpiString), CtxModelType.ATTRIBUTE, tempType);
                                if (targetDpiString.equals("null")) attrlist = ctxBroker.lookup(CtxModelType.ATTRIBUTE, tempType);
                            } catch (MalformedDigitialPersonalIdentifierException e1) {
                                log.error("Error when parsing DigitalPersonalIdentifier " + e1.getLocalizedMessage());
                            }
                            populateAttributeList(attrlist);
                        }
                    } catch (ContextException e1) {
                        e1.printStackTrace();
                    }
                }
            });
        }
        return jButtonUpdateAttribute;
    }

    /**
     * This method initializes jButtonRemoveAttribute   
     *  
     * @return javax.swing.JButton      
     */
    private JButton getJButtonRemoveAttribute() {
        if (jButtonRemoveAttribute == null) {
            jButtonRemoveAttribute = new JButton();
            jButtonRemoveAttribute.setText("remove Attribute");
            jButtonRemoveAttribute.setBounds(new Rectangle(439, 175, 132, 24));
            jButtonRemoveAttribute.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent e) {
                    try {
                        String attributeID = jTextFieldnewAttributeType.getText().trim();
                        ICtxAttributeIdentifier attrID = (ICtxAttributeIdentifier) ctxBroker.parseIdentifier(attributeID);
                        String tempType = attrID.getType();
                        ctxBroker.remove(attrID);
                        if (globalEntity != null) {
                            globalEntity = (ICtxEntity) ctxBroker.retrieve(globalEntity.getCtxIdentifier());
                            populateOperatorAttributesList(globalEntity);
                        }
                        if (globalEntity == null) {
                            String targetDpiString = jComboBoxTargetVid.getSelectedItem().toString();
                            List<ICtxIdentifier> attrlist = null;
                            try {
                                if (targetDpiString != "null") attrlist = ctxBroker.lookup(identMgmt.parseDigitalPersonalIdentifier(targetDpiString), CtxModelType.ATTRIBUTE, tempType);
                                if (targetDpiString.equals("null")) attrlist = ctxBroker.lookup(CtxModelType.ATTRIBUTE, tempType);
                            } catch (MalformedDigitialPersonalIdentifierException e1) {
                                e1.printStackTrace();
                            }
                            populateAttributeList(attrlist);
                        }
                    } catch (ContextException e1) {
                        e1.printStackTrace();
                    }
                }
            });
        }
        return jButtonRemoveAttribute;
    }

    /**
     * This method initializes jCheckBoxHistory 
     *  
     * @return javax.swing.JCheckBox    
     */
    private JCheckBox getJCheckBoxHistory() {
        if (jCheckBoxHistory == null) {
            jCheckBoxHistory = new JCheckBox();
            jCheckBoxHistory.setEnabled(true);
            jCheckBoxHistory.setBounds(new Rectangle(437, 122, 21, 21));
        }
        return jCheckBoxHistory;
    }

    /**
     * This method initializes jComboBoxDataType        
     *  
     * @return javax.swing.JComboBox    
     */
    private JComboBox getJComboBoxDataType() {
        if (jComboBoxDataType == null) {
            jComboBoxDataType = new JComboBox();
            jComboBoxDataType.addItem("String");
            jComboBoxDataType.addItem("Integer");
            jComboBoxDataType.addItem("Double");
            jComboBoxDataType.setEnabled(true);
            jComboBoxDataType.setBounds(new Rectangle(554, 115, 97, 25));
        }
        return jComboBoxDataType;
    }

    /**
     * This method initializes jComboBoxTagetVid        
     *  
     * @return javax.swing.JComboBox    
     */
    private JComboBox getJComboBoxTagetVid() {
        if (jComboBoxTargetVid == null) {
            jComboBoxTargetVid = new JComboBox();
            jComboBoxTargetVid.setEnabled(true);
            refreshTargetDpis();
            jComboBoxTargetVid.setBounds(new Rectangle(95, 65, 267, 25));
        }
        return jComboBoxTargetVid;
    }

    private void refreshForeignDpis() {
    }

    private void refreshTargetDpis() {
        jComboBoxTargetVid.removeAllItems();
        IDigitalPersonalIdentifier[] targetIdList = identMgmt.getAllDigitalPersonalIdentifiers();
        Collection<PSSInfo> pssIds = null;
        try {
            pssIds = pssManager.listPSSs();
        } catch (PssManagerException e) {
            e.printStackTrace();
        }
        jComboBoxTargetVid.addItem("-- My DPIs --");
        for (int i = 0; i < targetIdList.length; i++) {
            jComboBoxTargetVid.addItem(targetIdList[i].toUriString());
        }
        jComboBoxTargetVid.addItem("-- Foreign DPIs --");
        if (pssIds != null) {
            for (PSSInfo pssInf : pssIds) {
                jComboBoxTargetVid.addItem(pssInf.getPublicDPI());
            }
        }
        jComboBoxTargetVid.addItem("null");
        jComboBoxTargetVid.setSelectedItem("null");
    }

    /**
     * This method initializes jButtonGetOperator       
     *  
     * @return javax.swing.JButton      
     */
    private JButton getJButtonGetOperator() {
        if (jButtonGetOperator == null) {
            jButtonGetOperator = new JButton();
            jButtonGetOperator.setText("Get Operator");
            jButtonGetOperator.setBounds(new Rectangle(310, 102, 107, 26));
            jButtonGetOperator.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent e) {
                    try {
                        String targetDpiString = jComboBoxTargetVid.getSelectedItem().toString();
                        IDigitalPersonalIdentifier targetDPI = null;
                        if (targetDpiString.equals("null")) {
                            globalEntity = ctxBroker.retrieveOperator();
                        } else {
                            targetDPI = identMgmt.parseDigitalPersonalIdentifier(targetDpiString);
                            globalEntity = ctxBroker.retrieveOperator(targetDPI);
                        }
                    } catch (ContextException e1) {
                        log.error(e1.getLocalizedMessage());
                        e1.printStackTrace();
                    } catch (MalformedDigitialPersonalIdentifierException e2) {
                        log.error("Error when parsing DigitalPersonalIdentifier " + e2.getLocalizedMessage());
                    }
                    jTextFieldContextID.setText(globalEntity.getCtxIdentifier().toString());
                    jTextFieldStatus.setText("Operator Entity retrieved " + globalEntity.getCtxIdentifier().toString());
                    populateOperatorAttributesList(globalEntity);
                    List<ICtxIdentifier> assocList = new ArrayList<ICtxIdentifier>();
                    Set<ICtxAssociationIdentifier> setAssocs = globalEntity.getAssociations();
                    for (ICtxIdentifier assocID : setAssocs) {
                        assocList.add(assocID);
                    }
                    if (assocList != null) populateAssociationList(assocList);
                }
            });
        }
        return jButtonGetOperator;
    }

    /**
     * This method initializes exportHoCjButton 
     * Not used. 
     *  
     * @return javax.swing.JButton      
     */
    private JButton getExportHoCjButton() {
        if (exportHoCjButton == null) {
            exportHoCjButton = new JButton();
            exportHoCjButton.setBounds(new Rectangle(539, 190, 135, 25));
            exportHoCjButton.setText("export History");
            exportHoCjButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent e) {
                }
            });
        }
        return exportHoCjButton;
    }

    public void populateOperatorAttributesList(ICtxEntity entity) {
        Set<ICtxAttribute> attrs = entity.getAttributes();
        jTextFieldStatus.setText("");
        int numeberRows = attrs.size();
        ((DefaultTableModel) jTableAttributes.getModel()).setRowCount(numeberRows);
        int row = 0;
        int col = 0;
        for (ICtxAttribute attribute : attrs) {
            jTableAttributes.setValueAt(attribute.getCtxIdentifier().toString(), row, col);
            jTableAttributes.setValueAt(attribute.getType(), row, col + 1);
            if (attribute.getStringValue() != null) jTableAttributes.setValueAt(attribute.getStringValue(), row, col + 2);
            if (attribute.getDoubleValue() != null) jTableAttributes.setValueAt(attribute.getDoubleValue(), row, col + 2);
            if (attribute.getIntegerValue() != null) jTableAttributes.setValueAt(attribute.getIntegerValue(), row, col + 2);
            try {
                if (attribute.getBlobValue(getClass().getClassLoader()) != null) jTableAttributes.setValueAt("BLOB", row, col + 2);
            } catch (ContextModelException e) {
                jTableAttributes.setValueAt("BLOB", row, col + 2);
            }
            if (attribute.getValue() == null) jTableAttributes.setValueAt("NULL", row, col + 2);
            ICtxQuality qoc = attribute.getQuality();
            long fresh = qoc.getFreshness();
            Date date = qoc.getLastUpdate();
            CtxOriginType origin = qoc.getOrigin();
            Double precision = qoc.getPrecision();
            Double sen = qoc.getSensitivity();
            Double freq = qoc.getUpdateFrequency();
            String qocParams = "Date=" + date + ". Update frequency=" + freq + ". Origin=" + origin + ". Precision=" + precision + ". Freshness=" + fresh;
            jTableAttributes.setValueAt(qocParams, row, col + 3);
            String sourceID = attribute.getSourceId();
            jTableAttributes.setValueAt(sourceID, row, col + 4);
            row++;
        }
        jTextFieldnewAttributeType.setText("");
        jTextFieldnewAttributeValue.setText("");
    }

    public void populateAttributeList(List<ICtxIdentifier> attrlist) {
        jTextFieldStatus.setText("");
        Set<ICtxAttribute> attributeSet = new HashSet<ICtxAttribute>();
        for (ICtxIdentifier attrId : attrlist) {
            try {
                ICtxAttribute attribute = (ICtxAttribute) ctxBroker.retrieve(attrId);
                attributeSet.add(attribute);
            } catch (ContextException e) {
                log.error("Error when updating attributes table " + e.getLocalizedMessage());
            }
        }
        int numeberRows = attributeSet.size();
        ((DefaultTableModel) jTableAttributes.getModel()).setRowCount(numeberRows);
        int row = 0;
        int col = 0;
        for (ICtxAttribute attribute : attributeSet) {
            jTableAttributes.setValueAt(attribute.getCtxIdentifier().toString(), row, col);
            jTableAttributes.setValueAt(attribute.getType(), row, col + 1);
            if (attribute.getStringValue() != null) jTableAttributes.setValueAt(attribute.getStringValue(), row, col + 2);
            if (attribute.getDoubleValue() != null) jTableAttributes.setValueAt(attribute.getDoubleValue(), row, col + 2);
            if (attribute.getIntegerValue() != null) jTableAttributes.setValueAt(attribute.getIntegerValue(), row, col + 2);
            try {
                if (attribute.getBlobValue(getClass().getClassLoader()) != null) jTableAttributes.setValueAt("BLOB", row, col + 2);
            } catch (ContextModelException e) {
                jTableAttributes.setValueAt("BLOB", row, col + 2);
                log.warn("couldn't cast blob value, type BLOB");
            }
            if (attribute.getValue() == null) jTableAttributes.setValueAt("NULL", row, col + 2);
            ICtxQuality qoc = attribute.getQuality();
            long fresh = qoc.getFreshness();
            Date date = qoc.getLastUpdate();
            CtxOriginType origin = qoc.getOrigin();
            Double precision = qoc.getPrecision();
            Double sen = qoc.getSensitivity();
            Double freq = qoc.getUpdateFrequency();
            String qocParams = "Date=" + date + ". Update frequency=" + freq + ". Origin=" + origin + ". Precision=" + precision + ". Freshness=" + fresh;
            jTableAttributes.setValueAt(qocParams, row, col + 3);
            String sourceID = attribute.getSourceId();
            jTableAttributes.setValueAt(sourceID, row, col + 4);
            row++;
        }
        jTextFieldnewAttributeType.setText("");
        jTextFieldnewAttributeValue.setText("");
        if (attrlist.size() == 0) jTextFieldStatus.setText("no attributes retrieved");
    }

    private void populateAssocEntMebmers(ICtxAssociation association) {
        jCheckBoxIsParent.setSelected(false);
        jTextFieldEntityIDForAssociation.setText("");
        Set<ICtxEntityIdentifier> memberEntIDs = association.getEntities();
        int numeberRows = memberEntIDs.size();
        ((DefaultTableModel) jTableAssocEntMembers.getModel()).setRowCount(numeberRows);
        int row = 0;
        int col = 0;
        for (ICtxEntityIdentifier memberEntityID : memberEntIDs) {
            jTableAssocEntMembers.setValueAt(memberEntityID.toString(), row, col);
            jTableAssocEntMembers.setValueAt(memberEntityID.getType(), row, col + 1);
            if (association.getParentEntity() != null) {
                ICtxIdentifier parentEntID = association.getParentEntity();
                if (parentEntID.equals(memberEntityID)) {
                    jTableAssocEntMembers.setValueAt("yes", row, col + 2);
                }
                if (!parentEntID.equals(memberEntityID)) {
                    jTableAssocEntMembers.setValueAt("no", row, col + 2);
                }
            }
            if (association.getParentEntity() == null) {
                jTableAssocEntMembers.setValueAt("no", row, col + 2);
            }
            row++;
        }
    }

    /**
     * This method initializes jButtonGetAttribute      
     *  
     * @return javax.swing.JButton      
     */
    private JButton getJButtonGetAttribute() {
        if (jButtonGetAttribute == null) {
            jButtonGetAttribute = new JButton();
            jButtonGetAttribute.setText("Search Attribute");
            jButtonGetAttribute.setBounds(new Rectangle(10, 175, 132, 24));
            jButtonGetAttribute.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent e) {
                    String targetDpiString = jComboBoxTargetVid.getSelectedItem().toString();
                    List<ICtxIdentifier> attrlist = null;
                    try {
                        String type = jTextFieldnewAttributeType.getText().trim();
                        if (type.isEmpty()) {
                            jTextFieldStatus.setText("add an attribute type to search");
                            return;
                        }
                        if (targetDpiString != "null" && !type.isEmpty()) attrlist = ctxBroker.lookup(identMgmt.parseDigitalPersonalIdentifier(targetDpiString), CtxModelType.ATTRIBUTE, type);
                        if (targetDpiString.equals("null") && !type.isEmpty()) attrlist = ctxBroker.lookup(CtxModelType.ATTRIBUTE, type);
                    } catch (MalformedDigitialPersonalIdentifierException e1) {
                        log.error("Error when parsing DigitalPersonalIdentifier " + e1.getLocalizedMessage());
                    } catch (ContextException e1) {
                        log.error("Error when parsing DigitalPersonalIdentifier " + e1.getLocalizedMessage());
                        e1.printStackTrace();
                    }
                    if (attrlist != null) populateAttributeList(attrlist);
                }
            });
        }
        return jButtonGetAttribute;
    }

    /**
     * This method initializes jCheckBoxReadAccess      
     *  
     * @return javax.swing.JCheckBox    
     */
    private JCheckBox getJCheckBoxReadAccess() {
        if (jCheckBoxReadAccess == null) {
            jCheckBoxReadAccess = new JCheckBox();
            jCheckBoxReadAccess.setBounds(new Rectangle(31, 205, 21, 21));
        }
        return jCheckBoxReadAccess;
    }

    /**
     * This method initializes jCheckBoxWriteAccess     
     *  
     * @return javax.swing.JCheckBox    
     */
    private JCheckBox getJCheckBoxWriteAccess() {
        if (jCheckBoxWriteAccess == null) {
            jCheckBoxWriteAccess = new JCheckBox();
            jCheckBoxWriteAccess.setBounds(new Rectangle(30, 224, 21, 21));
        }
        return jCheckBoxWriteAccess;
    }

    /**
     * This method initializes jComboBoxRequestorDPI    
     *  
     * @return javax.swing.JComboBox    
     */
    private JComboBox getJComboBoxRequestorDPI() {
        if (jComboBoxRequestorDPI == null) {
            jComboBoxRequestorDPI = new JComboBox();
            jComboBoxRequestorDPI.setBounds(new Rectangle(342, 216, 268, 25));
            jComboBoxRequestorDPI.setEnabled(true);
            jComboBoxRequestorDPI.addItem("-- Foreign DPIs --");
            Collection<PSSInfo> pssIds = null;
            try {
                pssIds = pssManager.listPSSs();
            } catch (PssManagerException e) {
                e.printStackTrace();
            }
            if (pssIds != null) {
                for (PSSInfo pssInf : pssIds) {
                    jComboBoxRequestorDPI.addItem(pssInf.getPublicDPI());
                }
            }
            jComboBoxRequestorDPI.addItem("null");
            jComboBoxRequestorDPI.setSelectedItem("null");
        }
        return jComboBoxRequestorDPI;
    }

    void addPermission(IDigitalPersonalIdentifier requestor, ICtxIdentifier ctxIdent, boolean canRead, boolean canWrite) {
        this.log.debug("Adding permission for context identifier " + ctxIdent + ": Grant {READ=" + canRead + ", WRITE=" + canWrite + "} to requestor " + requestor);
        String actions = null;
        if (canRead && !canWrite) actions = "read"; else if (canRead && canWrite) actions = "read,write"; else if (!canRead && canWrite) actions = "write";
        if (actions == null) return;
        CtxPermission perm = new CtxPermission(ctxIdent, actions);
        try {
            IAccessControlDecision decision = this.decisionMgr.retrieve(requestor);
            if (decision == null) decision = this.decisionMgr.create(requestor);
            decision.add(perm);
            this.decisionMgr.update(decision);
        } catch (AccessControlException e) {
            this.log.error("Could not add permission: " + e.getLocalizedMessage(), e);
        }
        jTextFieldStatus.setText("Added permission " + perm);
    }

    /**
     * This method initializes jButtonAddPermision      
     *  
     * @return javax.swing.JButton      
     */
    private JButton getJButtonAddPermision() {
        if (jButtonAddPermision == null) {
            jButtonAddPermision = new JButton();
            jButtonAddPermision.setText("Add Permission to DPI");
            jButtonAddPermision.setBounds(new Rectangle(160, 216, 170, 23));
        }
        jButtonAddPermision.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent e) {
                try {
                    addPermission(getReqDPI(), ctxBroker.parseIdentifier(jTextFieldnewAttributeType.getText().trim()), jCheckBoxReadAccess.isSelected(), jCheckBoxWriteAccess.isSelected());
                } catch (ContextException e1) {
                    jTextFieldStatus.setText(e1.toString());
                    e1.printStackTrace();
                }
            }
        });
        return jButtonAddPermision;
    }

    private IDigitalPersonalIdentifier getReqDPI() {
        IDigitalPersonalIdentifier reqDPI = null;
        if (!jComboBoxRequestorDPI.getSelectedItem().toString().equals("null")) {
            reqDpiString = jComboBoxRequestorDPI.getSelectedItem().toString();
            try {
                reqDPI = identMgmt.parseDigitalPersonalIdentifier(reqDpiString);
            } catch (MalformedDigitialPersonalIdentifierException e) {
                jTextFieldStatus.setText(e.toString());
                e.printStackTrace();
            }
        }
        if (reqDPI == null) {
            jTextFieldStatus.setText("DPI can't be null");
        }
        return reqDPI;
    }

    /**
     * This method initializes jButtonRefresh   
     *  
     * @return javax.swing.JButton      
     */
    private JButton getJButtonRefresh() {
        if (jButtonRefresh == null) {
            jButtonRefresh = new JButton();
            jButtonRefresh.setBounds(new Rectangle(520, 102, 100, 27));
            jButtonRefresh.setText("Refresh ");
            jButtonRefresh.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent e) {
                    if (globalEntity != null) {
                        try {
                            globalEntity = (ICtxEntity) ctxBroker.retrieve(globalEntity.getCtxIdentifier());
                            jTextFieldContextID.setText(globalEntity.getCtxIdentifier().toString());
                            populateOperatorAttributesList(globalEntity);
                            populateOperatorAssociationsList(globalEntity);
                            jTextFieldStatus.setText(" attributes, associations, and DPIs refreshed");
                        } catch (ContextException e1) {
                            e1.printStackTrace();
                        }
                    }
                    refreshTargetDpis();
                }
            });
        }
        return jButtonRefresh;
    }

    /**
     * This method initializes jAttributePanel  
     *  
     * @return javax.swing.JPanel       
     */
    private JPanel getJAttributePanel() {
        if (jAttributePanel == null) {
            jAttributePanel = new JPanel();
            jAttributePanel.setLayout(null);
            jAttributePanel.setBounds(new Rectangle(7, 142, 685, 264));
            jLabelHistory = new JLabel();
            jLabelHistory.setText("is Recorded");
            jLabelHistory.setBounds(new Rectangle(362, 122, 68, 16));
            jAttributePanel.add(jLabelHistory);
            jLabelDataTypeMenu = new JLabel();
            jLabelDataTypeMenu.setText("Value Type");
            jLabelDataTypeMenu.setBounds(new Rectangle(483, 122, 62, 16));
            jAttributePanel.add(jLabelDataTypeMenu);
            jLabelWriteAccess = new JLabel();
            jLabelWriteAccess.setText("WriteAcces");
            jLabelWriteAccess.setBounds(new Rectangle(64, 231, 76, 16));
            jAttributePanel.add(jLabelWriteAccess, null);
            jLabelReadAccess = new JLabel();
            jLabelReadAccess.setText("Read Access");
            jLabelReadAccess.setBounds(new Rectangle(62, 208, 76, 16));
            jAttributePanel.add(jLabelReadAccess, null);
            jAttributePanel.add(getJCheckBoxHistory());
            jAttributePanel.add(getJComboBoxDataType(), getJComboBoxDataType().getName());
            jAttributePanel.add(getJTextFieldnewAttributeType(), null);
            jAttributePanel.add(getJTextFieldnewAttributeValue(), null);
            jAttributePanel.add(getJButtonAddAttribute(), null);
            jAttributePanel.add(getJButtonGetAttribute(), null);
            jAttributePanel.add(getJButtonUpdateAttribute(), null);
            jAttributePanel.add(getJButtonRemoveAttribute(), null);
            jAttributePanel.add(getJButtonAddPermision(), null);
            jAttributePanel.add(getJCheckBoxReadAccess(), null);
            jAttributePanel.add(getJCheckBoxWriteAccess(), null);
            jAttributePanel.add(getJScrollPaneTable(), null);
            jAttributePanel.add(getJComboBoxRequestorDPI(), null);
        }
        return jAttributePanel;
    }

    private JPanel getJAssociationPanel() {
        if (jAssociationPanel == null) {
            jLabelEntityIDForAssociation = new JLabel();
            jLabelEntityIDForAssociation.setBounds(new Rectangle(392, 148, 59, 16));
            jLabelEntityIDForAssociation.setText("EntityID");
            jLabelIsParent = new JLabel();
            jLabelIsParent.setBounds(new Rectangle(392, 173, 58, 16));
            jLabelIsParent.setText("isParent");
            jLabelAssocEntMembers = new JLabel();
            jLabelAssocEntMembers.setBounds(new Rectangle(415, 8, 199, 16));
            jLabelAssocEntMembers.setText("Associations Member Entity IDs");
            jLabelAssocTable = new JLabel();
            jLabelAssocTable.setBounds(new Rectangle(8, 8, 95, 16));
            jLabelAssocTable.setText("Associations");
            jAssociationTypeLabel = new JLabel();
            jAssociationTypeLabel.setBounds(new Rectangle(4, 148, 116, 16));
            jAssociationTypeLabel.setText("Association type");
            jAssociationIDLabel = new JLabel();
            jAssociationIDLabel.setBounds(new Rectangle(5, 196, 96, 20));
            jAssociationIDLabel.setText("Association ID");
            jAssociationPanel = new JPanel();
            jAssociationPanel.setLayout(null);
            jAssociationPanel.setBounds(new Rectangle(7, 142, 685, 264));
            jAssociationPanel.add(getJAssociationsTableScrollPane(), null);
            jAssociationPanel.add(getJTextFieldAssocID(), null);
            jAssociationPanel.add(getJTextFieldAssocValue(), null);
            jAssociationPanel.add(jAssociationIDLabel, null);
            jAssociationPanel.add(jAssociationTypeLabel, null);
            jAssociationPanel.add(getJSearchAssocButton(), null);
            jAssociationPanel.add(getJCreateAssocButton(), null);
            jAssociationPanel.add(getJRemoveAssocButton(), null);
            jAssociationPanel.add(getJScrollPaneAssocEntMembers(), null);
            jAssociationPanel.add(jLabelAssocTable, null);
            jAssociationPanel.add(jLabelAssocEntMembers, null);
            jAssociationPanel.add(getJRetrieveAssocButton(), null);
            jAssociationPanel.add(getJAddEntToAssocButton(), null);
            jAssociationPanel.add(getJCheckBoxIsParent(), null);
            jAssociationPanel.add(jLabelIsParent, null);
            jAssociationPanel.add(getJButtonRemoveEntFromAssociation(), null);
            jAssociationPanel.add(getJTextFieldEntityIDForAssociation(), null);
            jAssociationPanel.add(jLabelEntityIDForAssociation, null);
        }
        return jAssociationPanel;
    }

    /**
     * This method initializes jTabbedPane      
     *  
     * @return javax.swing.JTabbedPane  
     */
    private JTabbedPane getJTabbedPane() {
        if (jTabbedPane == null) {
            jTabbedPane = new JTabbedPane();
            jTabbedPane.setBounds(new Rectangle(4, 130, 696, 286));
            jTabbedPane.addTab("Attributes", getJAttributePanel());
            jTabbedPane.addTab("Associations", getJAssociationPanel());
        }
        return jTabbedPane;
    }

    /**
     * This method initializes jAssociationsTableScrollPane     
     *  
     * @return javax.swing.JScrollPane  
     */
    private JScrollPane getJAssociationsTableScrollPane() {
        if (jAssociationsTableScrollPane == null) {
            jAssociationsTableScrollPane = new JScrollPane();
            jAssociationsTableScrollPane.setBounds(new Rectangle(5, 30, 339, 100));
            jAssociationsTableScrollPane.setViewportView(getJAssociationsTable());
        }
        return jAssociationsTableScrollPane;
    }

    /**
     * This method initializes jAssociationsTable       
     *  
     * @return javax.swing.JTable       
     */
    private JTable getJAssociationsTable() {
        if (jAssociationsTable == null) {
            System.out.println("****************getJAssociationsTable() 1 ");
            jAssociationsTable = new JTable(this.modelAssoc) {

                private static final long serialVersionUID = 1L;

                public boolean isCellEditable(int rowIndex, int colIndex) {
                    return false;
                }
            };
            jAssociationsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            jAssociationsTable.addMouseListener(new java.awt.event.MouseAdapter() {

                public void mouseClicked(java.awt.event.MouseEvent e) {
                    int row = jAssociationsTable.rowAtPoint(e.getPoint());
                    int col = 0;
                    String selecteAssocIdString = (String) jAssociationsTable.getValueAt(row, col);
                    jTextFieldAssocID.setText(selecteAssocIdString);
                    ICtxIdentifier assocId;
                    try {
                        assocId = ctxBroker.parseIdentifier(selecteAssocIdString);
                        ICtxAssociation selectedAssoc = (ICtxAssociation) ctxBroker.retrieve(assocId);
                        populateAssocEntMebmers(selectedAssoc);
                        jTextFieldAssocID.setText(selectedAssoc.getCtxIdentifier().toString());
                    } catch (ContextException e1) {
                        e1.printStackTrace();
                    }
                }
            });
        }
        return jAssociationsTable;
    }

    /**
     * This method initializes jTextFieldAssocID        
     *  
     * @return javax.swing.JTextField   
     */
    private JTextField getJTextFieldAssocID() {
        if (jTextFieldAssocID == null) {
            jTextFieldAssocID = new JTextField();
            jTextFieldAssocID.setBounds(new Rectangle(105, 197, 233, 20));
        }
        return jTextFieldAssocID;
    }

    /**
     * This method initializes jTextFieldAssocValue     
     *  
     * @return javax.swing.JTextField   
     */
    private JTextField getJTextFieldAssocValue() {
        if (jTextFieldAssocValue == null) {
            jTextFieldAssocValue = new JTextField();
            jTextFieldAssocValue.setBounds(new Rectangle(125, 142, 214, 20));
        }
        return jTextFieldAssocValue;
    }

    public void populateOperatorAssociationsList(ICtxEntity entity) {
        Set<ICtxAssociationIdentifier> associationIDs = null;
        jTextFieldStatus.setText("");
        jCheckBoxIsParent.setSelected(false);
        ((DefaultTableModel) jTableAssocEntMembers.getModel()).setRowCount(0);
        ((DefaultTableModel) jTableAssocEntMembers.getModel()).setRowCount(2);
        try {
            entity = (ICtxEntity) ctxBroker.retrieve(entity.getCtxIdentifier());
            if (entity.getAssociations() != null) associationIDs = entity.getAssociations();
            if (associationIDs != null) {
                int numeberRows = associationIDs.size();
                ((DefaultTableModel) jAssociationsTable.getModel()).setRowCount(0);
                ((DefaultTableModel) jAssociationsTable.getModel()).setRowCount(numeberRows);
                int row = 0;
                int col = 0;
                for (ICtxAssociationIdentifier associationID : associationIDs) {
                    ICtxAssociation assoc = (ICtxAssociation) ctxBroker.retrieve(associationID);
                    jAssociationsTable.setValueAt(associationID.toString(), row, col);
                    jAssociationsTable.setValueAt(assoc.getType(), row, col + 1);
                    if (assoc.getParentEntity() != null) jAssociationsTable.setValueAt(assoc.getParentEntity().toString(), row, col + 2);
                    row++;
                }
                jTextFieldAssocID.setText("");
                jTextFieldAssocValue.setText("");
            }
        } catch (ContextException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method initializes jSearchAssocButton       
     *  
     * @return javax.swing.JButton      
     */
    private JButton getJSearchAssocButton() {
        if (jSearchAssocButton == null) {
            jSearchAssocButton = new JButton();
            jSearchAssocButton.setBounds(new Rectangle(193, 167, 146, 24));
            jSearchAssocButton.setText("Search Association");
            jSearchAssocButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent e) {
                    System.out.println("search assocs pressed");
                    String targetDpiString = jComboBoxTargetVid.getSelectedItem().toString();
                    List<ICtxIdentifier> assoclist = null;
                    try {
                        String type = jTextFieldAssocValue.getText().trim();
                        if (type.isEmpty()) {
                            jTextFieldStatus.setText("add an association type to search");
                            return;
                        }
                        if (targetDpiString != "null" && !type.isEmpty()) assoclist = ctxBroker.lookup(identMgmt.parseDigitalPersonalIdentifier(targetDpiString), CtxModelType.ASSOCIATION, type);
                        if (targetDpiString.equals("null") && !type.isEmpty()) assoclist = ctxBroker.lookup(CtxModelType.ASSOCIATION, type);
                        if (assoclist != null) populateAssociationList(assoclist);
                    } catch (MalformedDigitialPersonalIdentifierException e1) {
                        log.error("Error when parsing DigitalPersonalIdentifier " + e1.getLocalizedMessage());
                    } catch (ContextException e1) {
                        log.error("Error when parsing DigitalPersonalIdentifier " + e1.getLocalizedMessage());
                        e1.printStackTrace();
                    }
                }
            });
        }
        return jSearchAssocButton;
    }

    /**
     * This method initializes jCreateAssocButton       
     *  
     * @return javax.swing.JButton      
     */
    private JButton getJCreateAssocButton() {
        if (jCreateAssocButton == null) {
            jCreateAssocButton = new JButton();
            jCreateAssocButton.setBounds(new Rectangle(4, 166, 188, 24));
            jCreateAssocButton.setText("Create/Assign Association");
            jCreateAssocButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent e) {
                    String assocType = jTextFieldAssocValue.getText();
                    ICtxAssociation newAssociation = null;
                    String targetDpiString = jComboBoxTargetVid.getSelectedItem().toString();
                    IDigitalPersonalIdentifier targetDPI = null;
                    try {
                        if (targetDpiString.equals("null")) {
                            newAssociation = ctxBroker.createAssociation(assocType);
                        }
                        if (!targetDpiString.equals("null")) {
                            targetDPI = identMgmt.parseDigitalPersonalIdentifier(targetDpiString);
                            newAssociation = ctxBroker.createAssociation(targetDPI, assocType);
                        }
                        jTextFieldStatus.setText("Created assocition with id: " + newAssociation.getCtxIdentifier());
                        if (globalEntity != null && newAssociation != null) {
                            newAssociation.addEntity(globalEntity);
                            ctxBroker.update(newAssociation);
                            newAssociation = (ICtxAssociation) ctxBroker.retrieve(newAssociation.getCtxIdentifier());
                            globalEntity = (ICtxEntity) ctxBroker.retrieve(globalEntity.getCtxIdentifier());
                            List<ICtxIdentifier> assocList = null;
                            Set<ICtxAssociationIdentifier> setAssocs = null;
                            if (globalEntity.getAssociations() != null) {
                                setAssocs = globalEntity.getAssociations();
                                assocList = new ArrayList<ICtxIdentifier>();
                                for (ICtxIdentifier assocID : setAssocs) {
                                    assocList.add(assocID);
                                }
                                if (assocList != null) populateAssociationList(assocList);
                            }
                        }
                    } catch (ContextException e1) {
                        e1.printStackTrace();
                    } catch (MalformedDigitialPersonalIdentifierException e2) {
                        e2.printStackTrace();
                    }
                }
            });
        }
        return jCreateAssocButton;
    }

    /**
     * This method initializes jRemoveAssocButton       
     *  
     * @return javax.swing.JButton      
     */
    private JButton getJRemoveAssocButton() {
        if (jRemoveAssocButton == null) {
            jRemoveAssocButton = new JButton();
            jRemoveAssocButton.setBounds(new Rectangle(6, 220, 151, 25));
            jRemoveAssocButton.setText("Remove Association");
            jRemoveAssocButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent e) {
                    System.out.println("remove assoc()");
                    String assocID = jTextFieldAssocID.getText();
                    try {
                        if (!assocID.isEmpty()) {
                            ctxBroker.remove(ctxBroker.parseIdentifier(assocID));
                            if (globalEntity != null) populateOperatorAssociationsList(globalEntity);
                        }
                    } catch (ContextException e1) {
                        e1.printStackTrace();
                    }
                }
            });
        }
        return jRemoveAssocButton;
    }

    public void populateAssociationList(List<ICtxIdentifier> assoclist) {
        jTextFieldStatus.setText("");
        jCheckBoxIsParent.setSelected(false);
        ((DefaultTableModel) jTableAssocEntMembers.getModel()).setRowCount(0);
        ((DefaultTableModel) jTableAssocEntMembers.getModel()).setRowCount(2);
        Set<ICtxAssociation> assocSet = new HashSet<ICtxAssociation>();
        for (ICtxIdentifier assocId : assoclist) {
            try {
                ICtxAssociation assoc = (ICtxAssociation) ctxBroker.retrieve(assocId);
                assocSet.add(assoc);
            } catch (ContextException e) {
                log.error("Error when updating attributes table " + e.getLocalizedMessage());
            }
        }
        int numeberRows = assocSet.size();
        ((DefaultTableModel) jAssociationsTable.getModel()).setRowCount(0);
        ((DefaultTableModel) jAssociationsTable.getModel()).setRowCount(numeberRows);
        int row = 0;
        int col = 0;
        for (ICtxAssociation association : assocSet) {
            jAssociationsTable.setValueAt(association.getCtxIdentifier().toString(), row, col);
            jAssociationsTable.setValueAt(association.getType(), row, col + 1);
            if (association.getParentEntity() != null) jAssociationsTable.setValueAt(association.getParentEntity().toString(), row, col + 2);
            row++;
        }
        jTextFieldAssocID.setText("");
        jTextFieldAssocValue.setText("");
        if (assoclist.size() == 0) jTextFieldStatus.setText("no associations to list");
    }

    /**
     * This method initializes jScrollPaneAssocEntMembers       
     *  
     * @return javax.swing.JScrollPane  
     */
    private JScrollPane getJScrollPaneAssocEntMembers() {
        if (jScrollPaneAssocEntMembers == null) {
            jScrollPaneAssocEntMembers = new JScrollPane();
            jScrollPaneAssocEntMembers.setBounds(new Rectangle(415, 30, 256, 100));
            jScrollPaneAssocEntMembers.setViewportView(getJTableAssocEntMembers());
        }
        return jScrollPaneAssocEntMembers;
    }

    /**
     * This method initializes jTableAssocEntMembers    
     *  
     * @return javax.swing.JTable       
     */
    private JTable getJTableAssocEntMembers() {
        if (jTableAssocEntMembers == null) {
            jTableAssocEntMembers = new JTable(modelAssocEntMembers) {

                private static final long serialVersionUID = 1L;

                public boolean isCellEditable(int rowIndex, int colIndex) {
                    return false;
                }
            };
            jTableAssocEntMembers.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            jTableAssocEntMembers.addMouseListener(new java.awt.event.MouseAdapter() {

                public void mouseClicked(java.awt.event.MouseEvent e) {
                    int row = jTableAssocEntMembers.rowAtPoint(e.getPoint());
                    int col = 0;
                    String selectedEntityIdString = (String) jTableAssocEntMembers.getValueAt(row, col);
                    jTextFieldEntityIDForAssociation.setText(selectedEntityIdString);
                    if (jTableAssocEntMembers.getValueAt(row, 2) != null) {
                        String isParent = (String) jTableAssocEntMembers.getValueAt(row, 2);
                        if (isParent.equalsIgnoreCase("yes")) jCheckBoxIsParent.setSelected(true);
                        if (!isParent.equalsIgnoreCase("yes")) jCheckBoxIsParent.setSelected(false);
                    }
                }
            });
        }
        return jTableAssocEntMembers;
    }

    /**
     * This method initializes jRetrieveAssocButton     
     *  
     * @return javax.swing.JButton      
     */
    private JButton getJRetrieveAssocButton() {
        if (jRetrieveAssocButton == null) {
            jRetrieveAssocButton = new JButton();
            jRetrieveAssocButton.setBounds(new Rectangle(165, 221, 151, 25));
            jRetrieveAssocButton.setText("Retrieve Association");
            jRetrieveAssocButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent e) {
                    System.out.println("actionPerformed() retrieve association");
                    String assocIDString = jTextFieldAssocID.getText().trim();
                    ICtxIdentifier assocID = null;
                    List<ICtxIdentifier> assocMockList = new ArrayList();
                    try {
                        assocID = ctxBroker.parseIdentifier(assocIDString);
                        ICtxAssociation assoc = (ICtxAssociation) ctxBroker.retrieve(assocID);
                        if (assoc != null) assocMockList.add(assoc.getCtxIdentifier());
                        populateAssociationList(assocMockList);
                    } catch (ContextException e1) {
                        e1.printStackTrace();
                    }
                }
            });
        }
        return jRetrieveAssocButton;
    }

    /**
     * This method initializes jAddEntToAssocButton     
     *  
     * @return javax.swing.JButton      
     */
    private JButton getJAddEntToAssocButton() {
        if (jAddEntToAssocButton == null) {
            jAddEntToAssocButton = new JButton();
            jAddEntToAssocButton.setBounds(new Rectangle(408, 191, 227, 24));
            jAddEntToAssocButton.setText("Add Entity to Association");
            jAddEntToAssocButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent e) {
                    System.out.println("actionPerformed() add entity to Assoc");
                    String stringSelectAssocID = jTextFieldAssocID.getText().trim();
                    String stringEntityID = jTextFieldEntityIDForAssociation.getText().trim();
                    ICtxIdentifier assocId = null;
                    ICtxAssociation selectedAssoc = null;
                    ICtxIdentifier entityID = null;
                    ICtxEntity entityToAdd = null;
                    try {
                        assocId = ctxBroker.parseIdentifier(stringSelectAssocID);
                        selectedAssoc = (ICtxAssociation) ctxBroker.retrieve(assocId);
                        entityID = ctxBroker.parseIdentifier(stringEntityID);
                        entityToAdd = (ICtxEntity) ctxBroker.retrieve(entityID);
                        if (entityToAdd != null && selectedAssoc != null) {
                            selectedAssoc.addEntity(entityToAdd);
                            if (jCheckBoxIsParent.isSelected()) {
                                selectedAssoc.setParentEntity(entityToAdd);
                                jCheckBoxIsParent.setSelected(false);
                            }
                            ctxBroker.update(selectedAssoc);
                            selectedAssoc = (ICtxAssociation) ctxBroker.retrieve(selectedAssoc.getCtxIdentifier());
                            if (globalEntity != null) {
                                globalEntity = (ICtxEntity) ctxBroker.retrieve(globalEntity.getCtxIdentifier());
                                populateOperatorAssociationsList(globalEntity);
                                populateAssocEntMebmers(selectedAssoc);
                            }
                            if (globalEntity == null) {
                                List<ICtxIdentifier> assocList = new ArrayList<ICtxIdentifier>();
                                assocList.add(selectedAssoc.getCtxIdentifier());
                                populateAssociationList(assocList);
                                populateAssocEntMebmers(selectedAssoc);
                            }
                            jTextFieldEntityIDForAssociation.setText("");
                            jTextFieldStatus.setText("Entity added to association");
                        }
                    } catch (ContextException e1) {
                        e1.printStackTrace();
                    }
                }
            });
        }
        return jAddEntToAssocButton;
    }

    /**
     * This method initializes jCheckBoxIsParent        
     *  
     * @return javax.swing.JCheckBox    
     */
    private JCheckBox getJCheckBoxIsParent() {
        if (jCheckBoxIsParent == null) {
            jCheckBoxIsParent = new JCheckBox();
            jCheckBoxIsParent.setBounds(new Rectangle(459, 168, 21, 21));
        }
        return jCheckBoxIsParent;
    }

    /**
     * This method initializes jButtonRemoveEntFromAssociation  
     *  
     * @return javax.swing.JButton      
     */
    private JButton getJButtonRemoveEntFromAssociation() {
        if (jButtonRemoveEntFromAssociation == null) {
            jButtonRemoveEntFromAssociation = new JButton();
            jButtonRemoveEntFromAssociation.setBounds(new Rectangle(407, 223, 227, 25));
            jButtonRemoveEntFromAssociation.setText("Remove Entity From Association");
            jButtonRemoveEntFromAssociation.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent e) {
                    System.out.println("actionPerformed() Remove Entity from Association");
                    ICtxAssociation assoc = null;
                    ICtxEntity entity = null;
                    if (!jTextFieldEntityIDForAssociation.getText().isEmpty() && !jTextFieldAssocID.getText().isEmpty()) {
                        String assocIDString = jTextFieldAssocID.getText().trim();
                        String entityIDString = jTextFieldEntityIDForAssociation.getText().trim();
                        try {
                            ICtxIdentifier assocID = ctxBroker.parseIdentifier(assocIDString);
                            ICtxIdentifier entityID = ctxBroker.parseIdentifier(entityIDString);
                            assoc = (ICtxAssociation) ctxBroker.retrieve(assocID);
                            entity = (ICtxEntity) ctxBroker.retrieve(entityID);
                            if (entity != null && assoc != null) assoc.removeEntity(entity);
                            ctxBroker.update(assoc);
                            assoc = (ICtxAssociation) ctxBroker.retrieve(assoc.getCtxIdentifier());
                            if (globalEntity != null) {
                                globalEntity = (ICtxEntity) ctxBroker.retrieve(globalEntity.getCtxIdentifier());
                                populateOperatorAssociationsList(globalEntity);
                            }
                            if (globalEntity == null) {
                                List<ICtxIdentifier> assocList = new ArrayList<ICtxIdentifier>();
                                assocList.add(assoc.getCtxIdentifier());
                                populateAssociationList(assocList);
                                populateAssocEntMebmers(assoc);
                            }
                            jTextFieldStatus.setText("Entity " + entity.getCtxIdentifier() + " removed from association");
                        } catch (ContextException e1) {
                            e1.printStackTrace();
                        }
                    }
                }
            });
        }
        return jButtonRemoveEntFromAssociation;
    }

    /**
     * This method initializes jTextFieldEntityIDForAssociation 
     *  
     * @return javax.swing.JTextField   
     */
    private JTextField getJTextFieldEntityIDForAssociation() {
        if (jTextFieldEntityIDForAssociation == null) {
            jTextFieldEntityIDForAssociation = new JTextField();
            jTextFieldEntityIDForAssociation.setBounds(new Rectangle(461, 142, 214, 20));
        }
        return jTextFieldEntityIDForAssociation;
    }
}
