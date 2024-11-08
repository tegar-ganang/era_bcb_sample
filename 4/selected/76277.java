package org.tango.pogo.pogo_gui;

import fr.esrf.TangoDs.Except;
import fr.esrf.tango.pogo.pogoDsl.InheritanceStatus;
import fr.esrf.tango.pogo.pogoDsl.PogoDslFactory;
import fr.esrf.tango.pogo.pogoDsl.PropType;
import fr.esrf.tango.pogo.pogoDsl.Property;
import fr.esrf.tangoatk.widget.util.ATKGraphicsUtils;
import fr.esrf.tangoatk.widget.util.ErrorPane;
import org.eclipse.emf.common.util.EList;
import org.tango.pogo.pogo_gui.tools.OAWutils;
import org.tango.pogo.pogo_gui.tools.PopupTable;
import org.tango.pogo.pogo_gui.tools.Utils;
import javax.swing.*;
import java.util.ArrayList;
import java.util.StringTokenizer;

public class PropertyDialog extends JDialog {

    private static final String[] propertyTypeNames = { "DevBoolean", "DevShort", "DevLong", "DevFloat", "DevDouble", "String", "DevUShort", "DevULong", "Array of DevShort", "Array of DevLong", "Array of DevFloat", "Array of DevDouble", "Array of String" };

    private static final int booleanProp = 0;

    private static final int shortProp = 1;

    private static final int intProp = 2;

    private static final int floatProp = 3;

    private static final int doubleProp = 4;

    private static final int stringProp = 5;

    private static final int ushortProp = 6;

    private static final int uintProp = 7;

    private static final int shortVector = 8;

    private static final int intVector = 9;

    private static final int floatVector = 10;

    private static final int doubleVector = 11;

    private static final int stringVector = 12;

    private static final String defaultDataType = propertyTypeNames[stringProp];

    private InheritanceStatus orig_status;

    private int retVal = JOptionPane.OK_OPTION;

    public PropertyDialog(JFrame parent, Property property, boolean is_dev) {
        super(parent, true);
        initComponents();
        setProperty(property);
        manageInheritanceStatus(property);
        titleLbl.setText(((is_dev) ? "Device" : "Class") + "  Property");
        if (is_dev) {
            mandatoryBtn.setToolTipText(Utils.buildToolTip("Mandatoty Device Property", "The property value must be specified in Tango database.\n" + "Otherwise all commands and read/write attribute will throw an exception."));
        } else mandatoryBtn.setVisible(false);
        pack();
        ATKGraphicsUtils.centerDialog(this);
    }

    private void manageInheritanceStatus(Property prop) {
        if (prop != null) {
            orig_status = prop.getStatus();
            if (Utils.isTrue(orig_status.getInherited())) {
                setEditable(false);
            } else setEditable(true);
        } else {
            orig_status = OAWutils.factory.createInheritanceStatus();
            orig_status.setAbstract("false");
            orig_status.setInherited("false");
            orig_status.setConcrete("true");
            orig_status.setConcreteHere("true");
        }
    }

    private void setNotEditable(JComboBox jcb) {
        String name = (String) jcb.getSelectedItem();
        if (name != null) {
            jcb.removeAllItems();
            jcb.addItem(name);
        }
    }

    private void setEditable(boolean b) {
        nameTxt.setEditable(b);
        if (!b) setNotEditable(typeComboBox);
    }

    private void setProperty(Property property) {
        for (String type : propertyTypeNames) typeComboBox.addItem(type);
        if (property != null) {
            nameTxt.setText(property.getName());
            String desc = Utils.strReplaceSpecialCharToDisplay(property.getDescription());
            descTxt.setText(desc);
            EList<String> values = property.getDefaultPropValue();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < values.size(); i++) {
                sb.append(values.get(i));
                if (i < values.size() - 1) sb.append("\n");
            }
            valueTxt.setText(sb.toString().trim());
            mandatoryBtn.setSelected(Utils.isTrue(property.getMandatory()));
            String strType = pogo2tangoType(property.getType());
            for (String type : propertyTypeNames) if (type.equals(strType)) typeComboBox.setSelectedItem(type);
        } else typeComboBox.setSelectedItem(defaultDataType);
    }

    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;
        javax.swing.JPanel titlePanel = new javax.swing.JPanel();
        titleLbl = new javax.swing.JLabel();
        javax.swing.JPanel bottomPanel = new javax.swing.JPanel();
        javax.swing.JButton okBtn = new javax.swing.JButton();
        javax.swing.JButton cancelBtn = new javax.swing.JButton();
        javax.swing.JPanel centerPanel = new javax.swing.JPanel();
        javax.swing.JLabel nameLbl = new javax.swing.JLabel();
        nameTxt = new javax.swing.JTextField();
        javax.swing.JLabel descLbl = new javax.swing.JLabel();
        javax.swing.JScrollPane scrollPane = new javax.swing.JScrollPane();
        descTxt = new javax.swing.JTextArea();
        javax.swing.JLabel arginLbl = new javax.swing.JLabel();
        javax.swing.JLabel valueLbl = new javax.swing.JLabel();
        typeComboBox = new javax.swing.JComboBox();
        descBtn = new javax.swing.JButton();
        valueBtn = new javax.swing.JButton();
        mandatoryBtn = new javax.swing.JRadioButton();
        javax.swing.JScrollPane scrollPane2 = new javax.swing.JScrollPane();
        valueTxt = new javax.swing.JTextArea();
        addWindowListener(new java.awt.event.WindowAdapter() {

            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });
        titleLbl.setFont(new java.awt.Font("Dialog", 1, 18));
        titleLbl.setText("Dialog Title");
        titlePanel.add(titleLbl);
        getContentPane().add(titlePanel, java.awt.BorderLayout.PAGE_START);
        okBtn.setText("OK");
        okBtn.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okBtnActionPerformed(evt);
            }
        });
        bottomPanel.add(okBtn);
        cancelBtn.setText("Cancel");
        cancelBtn.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelBtnActionPerformed(evt);
            }
        });
        bottomPanel.add(cancelBtn);
        getContentPane().add(bottomPanel, java.awt.BorderLayout.SOUTH);
        centerPanel.setLayout(new java.awt.GridBagLayout());
        nameLbl.setText("Property Name:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 10);
        centerPanel.add(nameLbl, gridBagConstraints);
        nameTxt.setColumns(20);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        centerPanel.add(nameTxt, gridBagConstraints);
        descLbl.setText("Property description:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 10);
        centerPanel.add(descLbl, gridBagConstraints);
        scrollPane.setPreferredSize(new java.awt.Dimension(200, 100));
        descTxt.setColumns(20);
        descTxt.setRows(5);
        scrollPane.setViewportView(descTxt);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        centerPanel.add(scrollPane, gridBagConstraints);
        arginLbl.setText("Propery Type :");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(25, 20, 0, 10);
        centerPanel.add(arginLbl, gridBagConstraints);
        valueLbl.setText("Default Value :");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(10, 20, 0, 0);
        centerPanel.add(valueLbl, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(25, 0, 0, 0);
        centerPanel.add(typeComboBox, gridBagConstraints);
        descBtn.setText("...");
        descBtn.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        descBtn.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                descBtnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 20);
        centerPanel.add(descBtn, gridBagConstraints);
        valueBtn.setText("...");
        valueBtn.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        valueBtn.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                descBtnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 20);
        centerPanel.add(valueBtn, gridBagConstraints);
        mandatoryBtn.setText("Mandatory in Database");
        mandatoryBtn.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        mandatoryBtn.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mandatoryBtnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.insets = new java.awt.Insets(30, 20, 0, 0);
        centerPanel.add(mandatoryBtn, gridBagConstraints);
        scrollPane2.setPreferredSize(new java.awt.Dimension(166, 60));
        valueTxt.setColumns(20);
        valueTxt.setRows(5);
        scrollPane2.setViewportView(valueTxt);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        centerPanel.add(scrollPane2, gridBagConstraints);
        getContentPane().add(centerPanel, java.awt.BorderLayout.CENTER);
        pack();
    }

    @SuppressWarnings({ "UnusedDeclaration" })
    private void okBtnActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            String name = nameTxt.getText();
            if (name.length() == 0) Except.throw_exception("Property name ?", "Property name ?", "PropertyDialog.okBtnActionPerformed()");
            name = Utils.checkNameSyntax(name, true);
            nameTxt.setText(name);
        } catch (Exception e) {
            ErrorPane.showErrorMessage(this, null, e);
            return;
        }
        retVal = JOptionPane.OK_OPTION;
        doClose();
    }

    @SuppressWarnings({ "UnusedDeclaration" })
    private void cancelBtnActionPerformed(java.awt.event.ActionEvent evt) {
        retVal = JOptionPane.CANCEL_OPTION;
        doClose();
    }

    @SuppressWarnings({ "UnusedDeclaration" })
    private void closeDialog(java.awt.event.WindowEvent evt) {
        retVal = JOptionPane.CANCEL_OPTION;
        doClose();
    }

    private void descBtnActionPerformed(java.awt.event.ActionEvent evt) {
        JButton btn = (JButton) evt.getSource();
        String text = "";
        if (btn == valueBtn) text = valueTxt.getText(); else if (btn == descBtn) text = descTxt.getText();
        EditDialog dlg = new EditDialog(this, text.trim());
        if (dlg.showDialog() == JOptionPane.OK_OPTION) {
            if (btn == valueBtn) valueTxt.setText(dlg.getText().trim()); else if (btn == descBtn) descTxt.setText(dlg.getText().trim());
        }
    }

    @SuppressWarnings({ "UnusedDeclaration" })
    private void mandatoryBtnActionPerformed(java.awt.event.ActionEvent evt) {
        boolean mandatory = !(mandatoryBtn.getSelectedObjects() == null);
        valueTxt.setEnabled(!mandatory);
        valueBtn.setEnabled(!mandatory);
    }

    private void doClose() {
        setVisible(false);
        dispose();
    }

    Property getProperty() {
        String name = nameTxt.getText();
        Property property = OAWutils.factory.createProperty();
        property.setName(name);
        String desc = descTxt.getText();
        desc = Utils.strReplaceSpecialCharToCode(desc);
        property.setDescription(desc);
        String strType = (String) typeComboBox.getSelectedItem();
        PropType type = tango2pogoType(strType);
        property.setType(type);
        boolean mandatory = !(mandatoryBtn.getSelectedObjects() == null);
        if (mandatory) property.setMandatory("true"); else {
            String strVal = Utils.strReplace(valueTxt.getText(), "\\n", "\n").trim();
            EList<String> list = property.getDefaultPropValue();
            StringTokenizer st = new StringTokenizer(strVal, "\n");
            while (st.hasMoreTokens()) {
                String line = st.nextToken().trim();
                if (line.length() > 0) list.add(line);
            }
        }
        property.setStatus(orig_status);
        return property;
    }

    public static Property cloneProperty(Property srcProperty) {
        Property newProperty = OAWutils.cloneProperty(srcProperty);
        InheritanceStatus inher_status = newProperty.getStatus();
        if (!Utils.isTrue(inher_status.getAbstract())) {
            inher_status.setAbstract("false");
            inher_status.setInherited("false");
            inher_status.setConcrete("true");
            inher_status.setConcreteHere("true");
        }
        if (Utils.isTrue(inher_status.getInherited())) {
            inher_status.setAbstract("false");
            inher_status.setInherited("false");
            inher_status.setConcrete("true");
            inher_status.setConcreteHere("true");
        }
        newProperty.setStatus(inher_status);
        return newProperty;
    }

    public int showDialog() {
        setVisible(true);
        return retVal;
    }

    private javax.swing.JButton descBtn;

    private javax.swing.JTextArea descTxt;

    private javax.swing.JRadioButton mandatoryBtn;

    private javax.swing.JTextField nameTxt;

    private javax.swing.JLabel titleLbl;

    private javax.swing.JComboBox typeComboBox;

    private javax.swing.JButton valueBtn;

    private javax.swing.JTextArea valueTxt;

    public static PropType createType(PropType type) {
        return tango2pogoType(pogo2tangoType(type));
    }

    public static PropType tango2pogoType(String tangoType) {
        if (tangoType.startsWith("Tango::")) tangoType = tangoType.substring("Tango::".length());
        if (tangoType.equals("void")) tangoType = "DevVoid";
        PogoDslFactory factory = OAWutils.factory;
        if (tangoType.equals(propertyTypeNames[booleanProp])) return factory.createBooleanType();
        if (tangoType.equals(propertyTypeNames[shortProp])) return factory.createShortType();
        if (tangoType.equals(propertyTypeNames[intProp]) || tangoType.equals("DevInt")) return factory.createIntType();
        if (tangoType.equals(propertyTypeNames[floatProp])) return factory.createFloatType();
        if (tangoType.equals(propertyTypeNames[doubleProp])) return factory.createDoubleType();
        if (tangoType.equals(propertyTypeNames[ushortProp])) return factory.createUShortType();
        if (tangoType.equals(propertyTypeNames[uintProp])) return factory.createUIntType();
        if (tangoType.equals(propertyTypeNames[stringProp]) || tangoType.equals("DevString") || tangoType.equals("string")) return factory.createStringType();
        if (tangoType.equals(propertyTypeNames[shortVector]) || tangoType.equals("vector<short>") || tangoType.equals("DevVarShortArray")) return factory.createShortVectorType();
        if (tangoType.equals(propertyTypeNames[intVector]) || tangoType.equals("vector<long>") || tangoType.equals("DevVarLongArray")) return factory.createIntVectorType();
        if (tangoType.equals(propertyTypeNames[floatVector]) || tangoType.equals("vector<float>") || tangoType.equals("DevVarFloatArray")) return factory.createFloatVectorType();
        if (tangoType.equals(propertyTypeNames[doubleVector]) || tangoType.equals("vector<double>") || tangoType.equals("DevVarDoubleArray")) return factory.createDoubleVectorType();
        if (tangoType.equals(propertyTypeNames[stringVector]) || tangoType.toLowerCase().equals("vector<string>") || tangoType.equals("DevStringVector") || tangoType.equals("DevVarStringArray")) return factory.createStringVectorType();
        System.err.println("============================================");
        System.err.println(tangoType + " NOT FOUND for property !!!");
        System.err.println("============================================");
        return null;
    }

    public static String pogo2tangoType(PropType type) {
        String header = "fr.esrf.tango.pogo.pogoDsl.impl.";
        String prop_tag = "Vector";
        String footer = "TypeImpl";
        String strPogoType = type.toString();
        if (strPogoType.startsWith(header)) {
            String tangoType = strPogoType.substring(header.length());
            int pos = tangoType.indexOf(footer);
            if (pos > 0) tangoType = tangoType.substring(0, pos);
            if (tangoType.indexOf(prop_tag) > 0) {
                if (tangoType.contains("ShortVector")) return propertyTypeNames[shortVector];
                if (tangoType.contains("IntVector")) return propertyTypeNames[intVector];
                if (tangoType.contains("FloatVector")) return propertyTypeNames[floatVector];
                if (tangoType.contains("DoubleVector")) return propertyTypeNames[doubleVector];
                if (tangoType.contains("StringVector")) return propertyTypeNames[stringVector];
            }
            if (tangoType.startsWith("Boolean")) return propertyTypeNames[booleanProp];
            if (tangoType.startsWith("Short")) return propertyTypeNames[shortProp];
            if (tangoType.startsWith("Int")) return propertyTypeNames[intProp];
            if (tangoType.startsWith("Float")) return propertyTypeNames[floatProp];
            if (tangoType.startsWith("Double")) return propertyTypeNames[doubleProp];
            if (tangoType.startsWith("String")) return propertyTypeNames[stringProp];
            if (tangoType.startsWith("UShort")) return propertyTypeNames[ushortProp];
            if (tangoType.startsWith("UInt")) return propertyTypeNames[uintProp];
        } else System.err.println(strPogoType + " not found !  (" + header + ")");
        return "Not a Pogo Type";
    }

    private static int[] columnSize = { 140, 130, 40, 400 };

    private static String[] columnTitle = { "name", "type", "Inherited", "Description" };

    public static void popupSummary(JFrame parent, ArrayList<Property> vp, boolean is_dev) {
        ArrayList<ArrayList<String>> summary = buildSummary(vp);
        String title = Integer.toString(vp.size()) + ((is_dev) ? "  Device" : "  Class") + " Properties";
        PopupTable ppt = new PopupTable(parent, title, columnTitle, summary);
        int nb = vp.size();
        if (nb > 35) nb = 35;
        ppt.setPreferredSize(columnSize, nb);
        ppt.setVisible(true);
    }

    public static ArrayList<ArrayList<String>> buildSummary(ArrayList<Property> vp) {
        ArrayList<ArrayList<String>> result = new ArrayList<ArrayList<String>>();
        for (Property prop : vp) {
            ArrayList<String> line = new ArrayList<String>();
            line.add(prop.getName());
            line.add(pogo2tangoType(prop.getType()));
            InheritanceStatus status = prop.getStatus();
            line.add(Utils.strBoolean(status.getInherited()));
            line.add(Utils.strReplace(prop.getDescription(), "\\n", "\n"));
            result.add(line);
        }
        return result;
    }
}
