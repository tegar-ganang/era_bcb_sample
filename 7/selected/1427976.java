package kr.ac.ssu.imc.durubi.report.designer.dialogs.dbquery;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

public class DRQBTableBox extends JPanel {

    protected JList fieldList;

    protected JLabel titleBar;

    protected JScrollPane scrollPane;

    protected boolean typeFlag;

    protected String[] fields;

    public DRQBTableBox(String tableName, String[] fieldNames, String[] keyFieldNames, boolean tFlag) {
        super(new BorderLayout());
        typeFlag = tFlag;
        fields = new String[fieldNames.length + 1];
        fields[0] = new String(tableName + ".*");
        for (int i = 1; i < fields.length; i++) fields[i] = fieldNames[i - 1];
        titleBar = new JLabel(tableName);
        titleBar.setFont(new Font("", Font.PLAIN, 11));
        titleBar.setBorder(new EtchedBorder());
        titleBar.setBackground(Color.lightGray);
        titleBar.setOpaque(true);
        titleBar.setHorizontalAlignment(SwingConstants.CENTER);
        add(titleBar, BorderLayout.NORTH);
        fieldList = new JList(fields);
        fieldList.setFont(new Font("monospaced", Font.PLAIN, 11));
        fieldList.setCellRenderer(new DRQBTableBoxListRenderer(keyFieldNames));
        scrollPane = new JScrollPane(fieldList);
        add(scrollPane, BorderLayout.CENTER);
        setBorder(new SoftBevelBorder(BevelBorder.RAISED));
    }

    public String getName() {
        return titleBar.getText();
    }

    public JLabel getTitleBar() {
        return titleBar;
    }

    public String[] getFieldList() {
        String[] fieldList = new String[fields.length - 1];
        for (int i = 0; i < fieldList.length; i++) {
            fieldList[i] = fields[i + 1];
        }
        return fieldList;
    }
}
