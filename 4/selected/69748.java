package org.sqlsplatter.tinyhorror.objects;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sqlsplatter.tinyhorror.other.C;

/**
 * Keeps schema physical data for underlying physical data management, including
 * file locking.<br>
 * WARNING: tableNames misses are not checked this side, which means every miss
 * will result in a NullPointerException; this work must be done on the Database
 * side.
 */
public class TxtPhysEngine implements IDBManager {

    private static final String DEF_FILENAME = "TABLES.DEF";

    private final File dataDir;

    private TxtFactory factory;

    private Map<String, PhysTableEntity> tableEntities;

    private RandomAccessFile defFile;

    private FileLock defFileLock;

    /**
	 * Check tables and fills dictionary.<br>
	 * Must not be called directly, as it's called by the Factory.
	 * @param factory 
	 * 
	 * @param dataDir
	 *            data path.
	 * @throws SQLException
	 *             tables error.
	 */
    protected TxtPhysEngine(TxtFactory factory, File dataDir) {
        this.factory = factory;
        this.dataDir = dataDir;
        tableEntities = new HashMap<String, PhysTableEntity>();
    }

    public PhysTableEntity getTableEntity(String tableName) {
        return tableEntities.get(tableName);
    }

    /**
	 * Must be called after instantiation.<br>
	 * Links to a ThsSchema and load tables from definition file.
	 * <p>
	 * Def file is created if it doesn't exist.
	 * 
	 * @param dataDir
	 *            data dir.
	 * @return map of tables.
	 * @throws SQLException
	 *             error loading tables.
	 */
    public Map<String, ThsTable> loadSchema() throws SQLException {
        Map<String, ThsTable> tables = new HashMap<String, ThsTable>();
        try {
            File defFileHnd = new File(dataDir, DEF_FILENAME);
            defFile = new RandomAccessFile(defFileHnd, "rw");
            FileChannel fCh = defFile.getChannel();
            defFileLock = fCh.lock(0, fCh.size(), true);
            String line;
            while ((line = defFile.readLine()) != null) {
                ThsTable table = parseTableDef(line);
                String tableName = table.getName();
                File tableFile = new File(dataDir, tableName);
                if (!tableFile.exists()) throw new SQLException("Table file not found");
                tables.put(tableName, table);
                tableEntities.put(tableName, new PhysTableEntity(tableFile));
            }
            return tables;
        } catch (IOException e) {
            throw (SQLException) new SQLException().initCause(e);
        }
    }

    public void reset(String tableName) throws IOException {
        PhysTableEntity ptab = tableEntities.get(tableName);
        ptab.seek(0);
    }

    /**
	 * Get table definition, for DEF file.<br>
	 * WARNING: includes LF at the end.
	 * 
	 * @param table
	 *            table.
	 * @return table definition.
	 */
    private String createTableDef(ThsTable table) {
        String tableName = table.getName();
        StringBuilder sb = new StringBuilder(tableName);
        sb.append('|');
        ThsColumn[] columns = table.getColumns();
        for (int i = 0; i < columns.length; i++) {
            ThsColumn column = columns[i];
            sb.append(column.getName()).append(',').append(column.getType()).append(',').append(column.getLength());
            if (i < columns.length - 1) sb.append(';');
        }
        sb.append(C.LINE_SEP);
        return sb.toString();
    }

    /**
	 * Parses a table from a DEF record.
	 * 
	 * @param line
	 *            DEF record.
	 * @return parsed table.
	 * @throws SQLException
	 *             error in parsing.
	 */
    private ThsTable parseTableDef(String line) throws SQLException {
        String[] tableDefStr = line.split("\\|");
        List<ThsColumn> columns = new ArrayList<ThsColumn>();
        String[] columnDefsStr = tableDefStr[1].split("\\;");
        for (String columnData : columnDefsStr) {
            String[] data = columnData.split("\\,");
            ThsColumn column = new ThsColumn(data[0], data[1].charAt(0), Integer.parseInt(data[2]));
            columns.add(column);
        }
        String tableName = tableDefStr[0];
        ThsTable table = factory.getTable(tableName, columns);
        return table;
    }

    public void add(ThsTable table, Map<String, ThsTable> tables) throws IOException {
        String tableName = table.getName();
        File newTableHnd = new File(dataDir, tableName);
        tableEntities.put(tableName, new PhysTableEntity(newTableHnd));
        updateDictionaryFile(tables);
    }

    public void remove(String tableName, Map<String, ThsTable> tables) throws IOException {
        tableEntities.get(tableName).delete();
        tableEntities.remove(tableName);
        updateDictionaryFile(tables);
    }

    public void renameColumn(Map<String, ThsTable> tables) throws IOException {
        updateDictionaryFile(tables);
    }

    /**
	 * Overwrite def file with new definitions.
	 * 
	 * @throws IOException
	 *             i/o error.
	 */
    private void updateDictionaryFile(Map<String, ThsTable> tables) throws IOException {
        defFile.seek(0);
        for (ThsTable table : tables.values()) {
            String tableDef = createTableDef(table);
            defFile.writeBytes(tableDef);
        }
        defFile.setLength(defFile.getFilePointer());
    }

    public void close() throws IOException {
        IOException first_exc = null;
        try {
            defFileLock.release();
            defFile.close();
        } catch (IOException e) {
            first_exc = e;
        }
        ;
        for (PhysTableEntity tableEntity : tableEntities.values()) {
            try {
                tableEntity.unlockAndClose();
            } catch (IOException e) {
                if (first_exc == null) first_exc = e;
            }
            ;
        }
        if (first_exc != null) throw first_exc;
    }
}
