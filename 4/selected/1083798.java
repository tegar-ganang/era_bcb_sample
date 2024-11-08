package edu.whitman.halfway.jigs.gui.shared;

import edu.whitman.halfway.util.StringUtil;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import javax.swing.table.AbstractTableModel;
import javax.swing.JOptionPane;
import org.apache.log4j.*;

public abstract class ArrayListTableModel extends AbstractTableModel {

    private Category log = Category.getInstance(ArrayListTableModel.class.getName());

    protected Component component;

    protected String[] columnNames;

    protected ArrayList data;

    protected ArrayListTableModel(Component c) {
        this(c, new String[0], new ArrayList());
    }

    public ArrayListTableModel(Component c, String[] colNames) {
        this(c, colNames, new ArrayList());
    }

    public ArrayListTableModel(Component c, String[] colNames, ArrayList data) {
        component = c;
        columnNames = colNames;
        this.data = data;
    }

    public int getColumnCount() {
        return columnNames.length;
    }

    public int getRowCount() {
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

    public void add(Object obj) {
        data = addObject(obj, -1);
    }

    public void add(Object obj, int i) {
        if (i < 0 || i >= data.size()) {
            data = addObject(obj, -1);
        } else {
            data = addObject(obj, i);
        }
        fireTableDataChanged();
    }

    public void set(Object obj, int i) {
        if (i < 0 || i >= data.size()) return;
        data.remove(i);
        data = addObject(obj, i);
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

    public void setData(ArrayList data) {
        this.data = data;
        fireTableDataChanged();
    }

    public void setDataAsObject(Object[] obj) {
        data.clear();
        data.addAll(Arrays.asList(obj));
        fireTableDataChanged();
    }

    public ArrayList getData() {
        return data;
    }

    public Object[] getDataAsObject() {
        return (Object[]) data.toArray(new Object[data.size()]);
    }

    public abstract String typeObject();

    public abstract String nameObject(Object o);

    public abstract Object editObject(Object o);

    public abstract boolean sameObject(Object o, Object n);

    private ArrayList addObject(Object newData, int addNum) {
        ArrayList newObjects = new ArrayList();
        for (int i = 0; i < data.size(); i++) {
            if (sameObject(data.get(i), newData)) {
                String name = nameObject(newData);
                Object[] options = { "Overwrite", "Ignore", "Rename" };
                int n = JOptionPane.showOptionDialog(JOptionPane.getFrameForComponent(component), "A " + typeObject() + " named \"" + name + "\" already exists." + StringUtil.newline + "Would you like to Overwrite the existing " + typeObject() + StringUtil.newline + "Ignore the new version, or Rename the new version?", "Overwrite, Ignore, or Rename " + typeObject() + "?", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[2]);
                if (n == JOptionPane.CANCEL_OPTION) {
                    Object update = editObject(newData);
                    if (update == null) return addObject(newData, addNum);
                    return addObject(update, addNum);
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
