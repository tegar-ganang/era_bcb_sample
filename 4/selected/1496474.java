package com.javampire.util.gui;

import com.javampire.util.dao.ReadableDataNode;
import com.javampire.util.dao.DAOUtil;
import javax.swing.table.AbstractTableModel;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * TODO: document this.
 *
 * @author <a href="mailto:cnagy@ecircle.de">Csaba Nagy</a>
 * @version $Revision: 1.2 $ $Date: 2007/06/28 18:28:11 $
 */
public class NodeTableModel extends AbstractTableModel {

    private static final int BUFFER_SIZE = 100;

    private final ArrayList recordCache;

    private final Method[] methods;

    private final String[] propertyNames;

    private final ReadableDataNode data;

    private int crtOffset;

    private int rowCount;

    public NodeTableModel(ReadableDataNode data, int offset) throws IOException {
        this.data = data;
        recordCache = new ArrayList();
        fetchData(offset);
        Object record = data.newRecord();
        PropertyDescriptor[] propertyDescriptors;
        try {
            propertyDescriptors = Introspector.getBeanInfo(record.getClass()).getPropertyDescriptors();
        } catch (IntrospectionException e) {
            throw new IOException(e.getMessage());
        }
        ArrayList<Method> readMethods = new ArrayList<Method>(propertyDescriptors.length);
        ArrayList<String> readablePropertyNames = new ArrayList<String>(propertyDescriptors.length);
        for (PropertyDescriptor descriptor : propertyDescriptors) {
            final Method readMethod = descriptor.getReadMethod();
            final Method writeMethod = descriptor.getWriteMethod();
            if (readMethod != null && writeMethod != null) {
                readMethods.add(readMethod);
                readablePropertyNames.add(descriptor.getDisplayName());
            }
        }
        methods = readMethods.toArray(new Method[readMethods.size()]);
        propertyNames = readablePropertyNames.toArray(new String[readablePropertyNames.size()]);
    }

    public void nextPage() throws IOException {
        fetchNextPage();
        fireTableDataChanged();
    }

    public void previousPage() throws IOException {
        fetchData(Math.max(0, crtOffset - BUFFER_SIZE));
        fireTableDataChanged();
    }

    public void firstPage() throws IOException {
        fetchData(0);
        fireTableDataChanged();
    }

    public void lastPage() throws IOException {
        fetchData(Math.max(0, data.getRecordCount() - BUFFER_SIZE));
        fireTableDataChanged();
    }

    public void goTo(int index) throws IOException {
        fetchData(index);
        fireTableDataChanged();
    }

    private void fetchData(int offset) throws IOException {
        crtOffset = offset;
        if (offset != this.data.getRecordId()) {
            this.data.seek(offset);
        }
        fetchNextPage();
    }

    @SuppressWarnings({ "unchecked" })
    private void fetchNextPage() throws IOException {
        recordCache.clear();
        rowCount = DAOUtil.transferData(this.data, recordCache, BUFFER_SIZE);
    }

    public int getRowCount() {
        return rowCount;
    }

    public String getColumnName(int column) {
        return propertyNames[column];
    }

    public int getColumnCount() {
        return methods.length;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        Object record = recordCache.get(rowIndex);
        return extractColumn(record, columnIndex);
    }

    public Object extractColumn(Object record, int columnIndex) {
        try {
            return methods[columnIndex].invoke(record);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }
    }
}
