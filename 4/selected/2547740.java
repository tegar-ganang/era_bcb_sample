package edu.ucsd.osdt.db;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;

public class TableConfig {

    private ArrayList columns = new ArrayList();

    private String name = null;

    public TableConfigColumn getTableConfigColumn(String columnName) {
        int idx = getIndexForName(columnName);
        if (idx >= 0) return (TableConfigColumn) columns.get(idx); else return null;
    }

    public void putTableConfigColumn(TableConfigColumn aCol) {
        this.columns.add(aCol);
    }

    public void removeTableConfigColumn(String columnName) {
        int idx = getIndexForName(columnName);
        if (idx >= 0) columns.remove(idx);
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    /**
	  * Returns index of item with given name
	  */
    private int getIndexForName(String aName) {
        for (int i = 0; i < columns.size(); i++) {
            TableConfigColumn aCol = (TableConfigColumn) columns.get(i);
            if ((aCol.getName() != null) && aCol.getName().equals(aName)) {
                return i;
            }
        }
        return -1;
    }

    public LinkedList<String> getChannelNames() {
        LinkedList<String> chNames = new LinkedList<String>();
        for (int i = 0; i < columns.size(); i++) {
            TableConfigColumn aCol = (TableConfigColumn) columns.get(i);
            chNames.add(aCol.getChannelMapping());
        }
        return chNames;
    }

    public List getTableConfigColumnsAsList() {
        return columns;
    }
}
