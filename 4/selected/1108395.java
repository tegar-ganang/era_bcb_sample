package edu.whitman.halfway.jigs.gui.jigspace;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import javax.swing.table.AbstractTableModel;
import javax.swing.JOptionPane;
import org.apache.log4j.*;

public abstract class JSRSTemplateTableModel extends AbstractTableModel {

    private Category log = Category.getInstance(JSRSTemplateTableModel.class.getName());

    final String[] columnNames;

    final String newLine = System.getProperty("line.separator");

    protected ArrayList data;

    public JSRSTemplateTableModel(String[] colNames) {
        columnNames = colNames;
        data = new ArrayList();
    }

    public int getColumnCount() {
        return columnNames.length;
    }

    public int getRowCount() {
        return data.size();
    }

    public int size() {
        return data.size();
    }

    public String getColumnName(int col) {
        return columnNames[col];
    }

    public Class getColumnClass(int col) {
        if (data.size() > 0) {
            return this.getValueAt(0, col).getClass();
        }
        return null;
    }

    public void add(Component c, Object obj, int i) {
        if (i < 0 || i >= data.size()) {
            data = addObject(c, obj, -1);
        } else {
            data = addObject(c, obj, i);
        }
        fireTableDataChanged();
    }

    public void set(Component c, Object obj, int i) {
        if (i < 0 || i >= data.size()) return;
        remove(i);
        data = addObject(c, obj, i);
        fireTableDataChanged();
    }

    public Object get(int i) {
        if (i < 0 || i >= data.size()) return null;
        return data.get(i);
    }

    public void remove(int col) {
        if (col < 0 || col > data.size()) return;
        data.remove(col);
        fireTableDataChanged();
    }

    public void setData(Object[] obj) {
        data.clear();
        data.addAll(Arrays.asList(obj));
        fireTableDataChanged();
    }

    public Object[] getData(Object[] obj) {
        return data.toArray(obj);
    }

    public abstract String typeObject();

    public abstract String nameObject(Object o);

    public abstract Object editObject(Object o);

    public abstract boolean sameObject(Object o, Object n);

    private ArrayList addObject(Component c, Object newData, int addNum) {
        ArrayList newObjects = new ArrayList();
        for (int i = 0; i < data.size(); i++) {
            if (sameObject(data.get(i), newData)) {
                String name = nameObject(newData);
                Object[] options = { "Overwrite", "Ignore", "Rename" };
                int n = JOptionPane.showOptionDialog(c, "A " + typeObject() + " named \"" + name + "\" already exists." + newLine + "Would you like to Overwrite the existing " + typeObject() + newLine + "Ignore the new version, or Rename the new version?", "Overwrite, Ignore, or Rename " + typeObject() + "?", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[2]);
                if (n == JOptionPane.CANCEL_OPTION) {
                    Object update = editObject(newData);
                    if (update == null) return addObject(c, newData, addNum);
                    return addObject(c, update, addNum);
                } else if (n == JOptionPane.NO_OPTION) {
                    return data;
                }
            } else {
                newObjects.add(data.get(i));
            }
        }
        if ((addNum < 0) || (addNum > newObjects.size())) newObjects.add(newData); else newObjects.add(addNum, newData);
        return newObjects;
    }
}
