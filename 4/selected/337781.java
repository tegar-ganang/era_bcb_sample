package com.cosmos.swingb;

/**
 * Created	:	13.03.2009
 * @author	Petar Milev
 *
 */
public abstract class JBColumn<T> {

    private int index;

    private String columnName;

    private Class columnClass;

    private boolean visible = true;

    private boolean editable = true;

    private boolean readable = true;

    private boolean writeable = false;

    public JBColumn() {
    }

    public JBColumn(String columnName, Class columnClass, int index) {
        super();
        this.columnName = columnName;
        this.columnClass = columnClass;
        this.index = index;
    }

    public JBColumn(String columnName, Class columnClass, boolean visible, boolean editable, boolean readable, boolean writeable, int index) {
        super();
        this.columnName = columnName;
        this.columnClass = columnClass;
        this.visible = visible;
        this.editable = editable;
        this.readable = readable;
        this.writeable = writeable;
        this.index = index;
    }

    public void setValue(T arg0, Object arg1) {
    }

    public boolean isWriteable(T arg0) {
        return writeable;
    }

    public boolean isReadable(T arg0) {
        return readable;
    }

    public abstract Object getValue(T item);

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public Class getColumnClass() {
        return columnClass;
    }

    public void setColumnClass(Class columnClass) {
        this.columnClass = columnClass;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }
}
