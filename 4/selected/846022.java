package me.buick.util.jmeter.snmpprocessvisualizers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import me.buick.util.snmp.core.pojo.ProcessInfoPojo;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.jorphan.reflect.Functor;
import org.apache.log.Logger;

public class SNMPTableModel extends DefaultTableModel {

    private static final Logger log = LoggingManager.getLoggerForClass();

    private static final long serialVersionUID = 233L;

    transient ArrayList<ProcessInfoPojo> objects = new ArrayList<ProcessInfoPojo>();

    transient List<String> headers = new ArrayList<String>();

    transient ArrayList<Class<Object>> classes = new ArrayList<Class<Object>>();

    transient ArrayList<Functor> readFunctors = new ArrayList<Functor>();

    transient ArrayList<Functor> writeFunctors = new ArrayList<Functor>();

    transient Class<ProcessInfoPojo> objectClass = null;

    /**
	 * The ObjectTableModel is a TableModel whose rows are objects; columns are
	 * defined as Functors on the object.
	 * 
	 * @param headers
	 *            - Column names
	 * @param _objClass
	 *            - Object class that will be used
	 * @param readFunctors
	 *            - used to get the values
	 * @param writeFunctors
	 *            - used to set the values
	 * @param editorClasses
	 *            - class for each column
	 */
    public SNMPTableModel(String[] headers, Class<ProcessInfoPojo> _objClass, Functor[] readFunctors, Functor[] writeFunctors, Class<Object>[] editorClasses) {
        this(headers, readFunctors, writeFunctors, editorClasses);
        this.objectClass = _objClass;
    }

    /**
	 * The ObjectTableModel is a TableModel whose rows are objects; columns are
	 * defined as Functors on the object.
	 * 
	 * @param headers
	 *            - Column names
	 * @param readFunctors
	 *            - used to get the values
	 * @param writeFunctors
	 *            - used to set the values
	 * @param editorClasses
	 *            - class for each column
	 */
    public SNMPTableModel(String[] headers, Functor[] readFunctors, Functor[] writeFunctors, Class<Object>[] editorClasses) {
        this.headers.addAll(Arrays.asList(headers));
        this.classes.addAll(Arrays.asList(editorClasses));
        this.readFunctors = new ArrayList<Functor>(Arrays.asList(readFunctors));
        this.writeFunctors = new ArrayList<Functor>(Arrays.asList(writeFunctors));
        int numHeaders = headers.length;
        int numClasses = classes.size();
        if (numClasses != numHeaders) {
            log.warn("Header count=" + numHeaders + " but classes count=" + numClasses);
        }
        int numWrite = writeFunctors.length;
        if (numWrite > 0 && numWrite != numHeaders) {
            log.warn("Header count=" + numHeaders + " but writeFunctor count=" + numWrite);
        }
        int numRead = readFunctors.length;
        if (numRead > 0 && numRead != numHeaders) {
            log.warn("Header count=" + numHeaders + " but readFunctor count=" + numRead);
        }
    }

    private Object readResolve() {
        objects = new ArrayList<ProcessInfoPojo>();
        headers = new ArrayList<String>();
        classes = new ArrayList<Class<Object>>();
        readFunctors = new ArrayList<Functor>();
        writeFunctors = new ArrayList<Functor>();
        return this;
    }

    public Iterator<ProcessInfoPojo> iterator() {
        return objects.iterator();
    }

    public void clearData() {
        int size = getRowCount();
        objects.clear();
        super.fireTableRowsDeleted(0, size);
    }

    public void addRow(ProcessInfoPojo value) {
        log.debug("Adding row value: " + value);
        if (objectClass != null) {
            final Class<? extends ProcessInfoPojo> valueClass = value.getClass();
            if (!objectClass.isAssignableFrom(valueClass)) {
                throw new IllegalArgumentException("Trying to add class: " + valueClass.getName() + "; expecting class: " + objectClass.getName());
            }
        }
        objects.add(value);
        super.fireTableRowsInserted(objects.size() - 1, objects.size());
    }

    public void insertRow(ProcessInfoPojo value, int index) {
        objects.add(index, value);
        super.fireTableRowsInserted(index, index + 1);
    }

    /**
	 * @see javax.swing.table.TableModel#getColumnCount()
	 */
    public int getColumnCount() {
        return headers.size();
    }

    /**
	 * @see javax.swing.table.TableModel#getColumnName(int)
	 */
    public String getColumnName(int col) {
        return (String) headers.get(col);
    }

    /**
	 * @see javax.swing.table.TableModel#getRowCount()
	 */
    public int getRowCount() {
        if (objects == null) {
            return 0;
        }
        return objects.size();
    }

    /**
	 * @see javax.swing.table.TableModel#getValueAt(int, int)
	 */
    public Object getValueAt(int row, int col) {
        log.debug("Getting row value");
        Object value = objects.get(row);
        if (headers.size() == 1 && col >= readFunctors.size()) {
            return value;
        }
        Functor getMethod = (Functor) readFunctors.get(col);
        if (getMethod != null && value != null) {
            return getMethod.invoke(value);
        }
        return null;
    }

    /**
	 * @see javax.swing.table.TableModel#isCellEditable(int, int)
	 */
    public boolean isCellEditable(int arg0, int arg1) {
        return true;
    }

    /**
	 * @see javax.swing.table.DefaultTableModel#moveRow(int, int, int)
	 */
    public void moveRow(int start, int end, int to) {
        List<ProcessInfoPojo> subList = objects.subList(start, end);
        for (int x = end - 1; x >= start; x--) {
            objects.remove(x);
        }
        objects.addAll(to, subList);
        super.fireTableChanged(new TableModelEvent(this));
    }

    /**
	 * @see javax.swing.table.DefaultTableModel#removeRow(int)
	 */
    public void removeRow(int row) {
        objects.remove(row);
        super.fireTableRowsDeleted(row, row);
    }

    /**
	 * @see javax.swing.table.TableModel#setValueAt(java.lang.Object, int, int)
	 */
    public void setValueAt(ProcessInfoPojo cellValue, int row, int col) {
        if (row < objects.size()) {
            Object value = objects.get(row);
            if (col < writeFunctors.size()) {
                Functor setMethod = (Functor) writeFunctors.get(col);
                if (setMethod != null) {
                    setMethod.invoke(value, new Object[] { cellValue });
                    super.fireTableDataChanged();
                }
            } else if (headers.size() == 1) {
                objects.set(row, cellValue);
            }
        }
    }

    /**
	 * @see javax.swing.table.TableModel#getColumnClass(int)
	 */
    public Class<Object> getColumnClass(int arg0) {
        return classes.get(arg0);
    }
}
