package org.elf.businesslayer.dictionary.manager;

import org.elf.businesslayer.dictionary.*;
import java.sql.ResultSet;
import java.util.*;
import org.elf.datalayer.*;
import org.elf.businesslayer.*;
import org.elf.datalayer.dictionary.DefKernelConnectionFactories;

/**
 * Ayuda a mantener el diccionario sincronizado con la base de datos
 * @author <a href="mailto:logongas@users.sourceforge.net">Lorenzo Gonz�lez</a>
 */
public class BusinessDictionaryManager {

    /**
     * Rellena el diccionario a partir de las tablas de base de datos.
     * @param listTables Lista de tablas de base de datos
     */
    public static void generateDictionaryTables(List<String> listTables) {
        for (int i = 0; i < listTables.size(); i++) {
            generateDictionaryTable(listTables.get(i));
        }
    }

    /**
     * Rellena el diccionario de una �nica tabla de base de datos en la conexi�n por defecto
     * @param tableName Nombre de la tabla a rellenar.
     */
    public static void generateDictionaryTable(String tableName) {
        generateDictionaryTable(DLSession.getConnection().getDefaultConnectionName(), tableName);
    }

    /**
     * Rellena el diccionario de una �nica tabla de base de datos en la conexi�n por defecto
     * @param tableName Nombre de la tabla a rellenar.
     */
    public static void generateDictionaryTable(String connectionName, String tableName) {
        DLSession.getTransaction().begin();
        try {
            RecordSet rst = DLSession.getConnection().executeQuery("(" + connectionName + ")SELECT * FROM " + tableName + " WHERE 1=0");
            Parameters defTableParameters = new Parameters();
            defTableParameters.addParameter(tableName, DataType.DT_STRING);
            defTableParameters.addParameter(connectionName, DataType.DT_STRING);
            defTableParameters.addParameter(tableName, DataType.DT_STRING);
            DLSession.getConnection().executeUpdate("(DICTIONARY)INSERT INTO  BL_DefTable(TableName,ConnectionName,Caption) VALUES (?,?,?)", defTableParameters);
            for (int i = 0; i < rst.getColumnCount(); i++) {
                String columnName = rst.getColumnName(i);
                Parameters parameters = new Parameters();
                parameters.addParameter(tableName, DataType.DT_STRING);
                parameters.addParameter(columnName, DataType.DT_STRING);
                parameters.addParameter(i, DataType.DT_INT);
                parameters.addParameter(rst.getDataType(i).ordinal(), DataType.DT_INT);
                parameters.addParameter(columnName, DataType.DT_STRING);
                DLSession.getConnection().executeUpdate("(DICTIONARY)INSERT INTO BL_DefColumn(TableName,ColumnName,ColumnIndex,DataType,Caption) VALUES (?,?,?,?,?)", parameters);
            }
        } catch (RuntimeException rex) {
            throw rex;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        DLSession.getTransaction().commit();
    }

    /**
     * Rellena el diccionario de una �nica columna de una tabla de la base de datos
     * @param tableName
     * @param columnName
     */
    public static void generateDictionaryColumn(String tableName, String columnName) {
        DefTable defTable = BLSession.getDictionary().getDefTables().getDefTable(tableName);
        String connectionName = defTable.getConnectionName();
        if ((connectionName == null) || (connectionName.trim().equals("") == true)) {
            connectionName = DLSession.getConnection().getDefaultConnectionName();
        }
        DLSession.getTransaction().begin();
        try {
            RecordSet rst = DLSession.getConnection().executeQuery("(" + connectionName + ")SELECT " + columnName + " FROM " + tableName + " WHERE 1=0");
            rst.next();
            RecordSet rstMax = DLSession.getConnection().executeQuery("(DICTIONARY)SELECT MAX(ColumnIndex) FROM BL_DefColumn WHERE TableName=?", tableName, DataType.DT_STRING);
            rstMax.next();
            int columnIndex = rstMax.getInt(0) + 1;
            if (rstMax.wasNull() == true) {
                columnIndex = 0;
            }
            Parameters parameters = new Parameters();
            parameters.addParameter(tableName, DataType.DT_STRING);
            parameters.addParameter(columnName, DataType.DT_STRING);
            parameters.addParameter(columnIndex, DataType.DT_INT);
            parameters.addParameter(rst.getDataType(columnName).ordinal(), DataType.DT_INT);
            DLSession.getConnection().executeUpdate("(DICTIONARY)INSERT INTO BL_DefColumn(TableName,ColumnName,ColumnIndex,DataType) VALUES (?,?,?,?)", parameters);
            rst.close();
            rstMax.close();
        } catch (RuntimeException rex) {
            DLSession.getTransaction().rollback();
            throw rex;
        } catch (Exception ex) {
            DLSession.getTransaction().rollback();
            throw new RuntimeException(ex);
        }
        DLSession.getTransaction().commit();
    }

    /**
     * Actualiza en el diccionario de una �nica columna de una tabla de la base de datos el tipo de datos
     * @param tableName
     * @param columnName
     */
    public static void updateDictionaryColumn(String tableName, String columnName) {
        DefTable defTable = BLSession.getDictionary().getDefTables().getDefTable(tableName);
        String connectionName = defTable.getConnectionName();
        if ((connectionName == null) || (connectionName.trim().equals("") == true)) {
            connectionName = DLSession.getConnection().getDefaultConnectionName();
        }
        DLSession.getTransaction().begin();
        try {
            RecordSet rst = DLSession.getConnection().executeQuery("(" + connectionName + ")SELECT " + columnName + " FROM " + tableName + " WHERE 1=0");
            Parameters parameters = new Parameters();
            parameters.addParameter(rst.getDataType(columnName).ordinal(), DataType.DT_INT);
            parameters.addParameter(tableName, DataType.DT_STRING);
            parameters.addParameter(columnName, DataType.DT_STRING);
            DLSession.getConnection().executeUpdate("(DICTIONARY)UPDATE BL_DefColumn SET DataType=? WHERE TableName=? AND ColumnName=?", parameters);
            rst.close();
        } catch (RuntimeException rex) {
            DLSession.getTransaction().rollback();
            throw rex;
        } catch (Exception ex) {
            DLSession.getTransaction().rollback();
            throw new RuntimeException(ex);
        }
        DLSession.getTransaction().commit();
    }

    /**
     * Comprueba todas las tablas del diccionario.
     * Chequea si todas las tablas del diccionario existen en la base de datos.
     * Se usa la conexi�n por defecto 
     * y si hay diferencias entre las columnas.
     * @return Lista de los conflictos encontrados
     */
    public static Conflicts checkTables() {
        Conflicts conflicts = new Conflicts();
        DefTables defTables = BLSession.getDictionary().getDefTables();
        {
            Iterator it = defTables.keySet().iterator();
            while (it.hasNext()) {
                DefTable defTable = defTables.getDefTable((String) it.next());
                if ((defTable.isVirtual() == false) && (defTable.getConnectionName().equals("DICTIONARY") == false)) {
                    checkTable(defTable, conflicts);
                }
            }
        }
        {
            DefKernelConnectionFactories dkcf = DLSession.getDictionary().getDefConnection().getDefKernelConnectionFactories();
            Iterator<String> it = dkcf.keySet().iterator();
            while (it.hasNext()) {
                String connectionName = it.next();
                if (connectionName.equals("DICTIONARY") == false) {
                    Object legacyConnection = DLSession.getConnection().getLegacyConnection(connectionName);
                    if (legacyConnection instanceof java.sql.Connection) {
                        java.sql.Connection connection = (java.sql.Connection) legacyConnection;
                        String[] types = { "TABLE" };
                        try {
                            ResultSet rst = connection.getMetaData().getTables((String) null, (String) null, (String) null, types);
                            while (rst.next()) {
                                String tableName = rst.getString("TABLE_NAME");
                                DefTable defTable = getDefTable(tableName);
                                if (defTable == null) {
                                    conflicts.add(Conflict.createConclictNotExistsTableInDictionary(connectionName, tableName));
                                } else {
                                    String dictionaryTableConnectionName = defTable.getConnectionName();
                                    if ((dictionaryTableConnectionName == null) || (dictionaryTableConnectionName.trim().equals(""))) {
                                        dictionaryTableConnectionName = DLSession.getConnection().getDefaultConnectionName();
                                    }
                                    if (dictionaryTableConnectionName.equalsIgnoreCase(connectionName) == false) {
                                        conflicts.add(Conflict.createConclictNotExistsTableInDictionary(connectionName, tableName));
                                    }
                                }
                            }
                        } catch (Exception ex) {
                            conflicts.add(Conflict.createConclictNotExistsSchemaInformation(connectionName));
                        }
                    }
                }
            }
        }
        return conflicts;
    }

    /**
     * Comprueba las diferencias entre una tabla del diccionario y la base de datos.
     * @param defTable Tabla del diccionario a comprobar si existe en la base de datos
     * @param conflicts Lista de conflictos encontrados
     */
    public static void checkTable(DefTable defTable, Conflicts conflicts) {
        DefColumns defColumns = defTable.getDefColumns();
        RecordSet rst;
        try {
            StringBuilder sqlSelect = new StringBuilder();
            if (defTable.getConnectionName().trim().equals("") == false) {
                sqlSelect.append("(" + defTable.getConnectionName() + ")");
            }
            sqlSelect.append("SELECT * FROM " + defTable.getTableName() + " WHERE 1=0");
            rst = DLSession.getConnection().executeQuery(sqlSelect.toString());
        } catch (Exception ex) {
            String connectionName;
            if (defTable.getConnectionName().trim().equals("") == false) {
                connectionName = defTable.getConnectionName();
            } else {
                connectionName = DLSession.getConnection().getDefaultConnectionName();
            }
            conflicts.add(Conflict.createConclictNotExistsTableInDb(connectionName, defTable.getTableName()));
            return;
        }
        for (int i = 0; i < rst.getColumnCount(); i++) {
            String columnName = rst.getColumnName(i);
            if (defColumns.getDefColumn(columnName) == null) {
                conflicts.add(Conflict.createConclictNotExistsColumnInDictionary(defTable.getTableName(), columnName));
            } else {
                if (rst.getDataType(i) != defColumns.getDefColumn(columnName).getDataType()) {
                    conflicts.add(Conflict.createConclictDifferentTypesInColumn(defTable.getTableName(), columnName, defColumns.getDefColumn(columnName).getDataType(), rst.getDataType(i)));
                }
            }
        }
        Iterator it = defColumns.keySet().iterator();
        while (it.hasNext()) {
            String columnName = (String) it.next();
            if ((defColumns.getDefColumn(columnName).isVirtual() == false) && (defColumns.getDefColumn(columnName).isMultivalue() == false)) {
                if (rst.existsColumnName(columnName) == false) {
                    conflicts.add(Conflict.createConclictNotExistsColumnInDb(defTable.getTableName(), columnName));
                }
            }
        }
        rst.close();
    }

    /**
     * Crea una tabla a base de datos MySQL a partir de los datos del diccionario
     * @param defTable Definici�n de la tabla a crear
     */
    public static void createMySQLTableFromDictionary(DefTable defTable) {
        DefColumns defColumns = defTable.getDefColumns();
        StringBuilder sqlColumns = new StringBuilder();
        StringBuilder sqlPrimaryKeyColumns = new StringBuilder();
        for (int i = 0; i < defColumns.size(); i++) {
            DefColumn defColumn = defColumns.getDefColumn(i);
            if ((defColumn.isVirtual() == false) && (defColumn.isMultivalue() == false)) {
                if (sqlColumns.length() != 0) {
                    sqlColumns.append(",");
                }
                sqlColumns.append("\n`" + defColumn.getColumnName() + "` " + getMySQLDataType(defColumn) + " ");
                if (defColumn.isPrimaryKey() == true) {
                    sqlColumns.append(" NOT NULL ");
                } else {
                    sqlColumns.append(" default NULL ");
                }
            }
            if ((defColumn.isPrimaryKey() == true) && (defColumn.isMultivalue() == false)) {
                if (sqlPrimaryKeyColumns.length() != 0) {
                    sqlPrimaryKeyColumns.append(",");
                }
                sqlPrimaryKeyColumns.append("`" + defColumn.getColumnName() + "`");
            }
        }
        StringBuilder sqlCreate = new StringBuilder();
        if (defTable.getConnectionName().trim().equals("") == false) {
            sqlCreate.append("(" + defTable.getConnectionName() + ")");
        }
        sqlCreate.append("CREATE TABLE `" + defTable.getTableName() + "` (\n");
        sqlCreate.append(sqlColumns);
        sqlCreate.append(",\nPRIMARY KEY  (" + sqlPrimaryKeyColumns.toString() + ")");
        sqlCreate.append("\n)");
        DLSession.getConnection().executeUpdate(sqlCreate.toString());
    }

    /**
     * A�ade una columna a una base de datos MySQL a partir de una columna del diccionario.
     * @param defColumn Definici�n de la columna a a�adir
     */
    public static void addMySQLColumnFromDictionary(DefColumn defColumn) {
        DefTable defTable = BLSession.getDictionary().getDefTables().getDefTable(defColumn.getTableName());
        if (defColumn.isVirtual() == true) {
            throw new RuntimeException("No se puede crear una columna virtual " + defColumn.getTableName() + "." + defColumn.getColumnName());
        }
        if (defColumn.isMultivalue() == true) {
            throw new RuntimeException("No se puede crear una columna multivalue " + defColumn.getTableName() + "." + defColumn.getColumnName());
        }
        StringBuffer sql = new StringBuffer();
        if (defTable.getConnectionName().trim().equals("") == false) {
            sql.append("(" + defTable.getConnectionName() + ")");
        }
        sql.append("ALTER TABLE `" + defTable.getTableName() + "` ADD ");
        sql.append(" `" + defColumn.getColumnName() + "` ");
        sql.append(getMySQLDataType(defColumn));
        DLSession.getConnection().executeUpdate(sql.toString());
    }

    /**
     * Obtiene un String con el tipo de datos de la base de datos.<br>
     * Esta funci�n se ha hecho espec�fica de MySQL.
     * DT_STRING --> VARCHAR(40)
     * @param defColumn Definicion de la columan de la que se obtienen su tipo en SQL
     * @return String con el tipo
     */
    private static String getMySQLDataType(DefColumn defColumn) {
        String mySQLDataType;
        switch(defColumn.getDataType()) {
            case DT_STRING:
                mySQLDataType = "VARCHAR(" + defColumn.getLength() + ")";
                break;
            case DT_BYTE:
                mySQLDataType = "TINYINT";
                break;
            case DT_SHORT:
                mySQLDataType = "SMALLINT";
                break;
            case DT_INT:
                mySQLDataType = "INT";
                break;
            case DT_LONG:
                mySQLDataType = "BIGINT";
                break;
            case DT_FLOAT:
                mySQLDataType = "FLOAT";
                break;
            case DT_DOUBLE:
                mySQLDataType = "DOUBLE";
                break;
            case DT_BOOLEAN:
                mySQLDataType = "BOOLEAN";
                break;
            case DT_DATE:
                mySQLDataType = "DATE";
                break;
            case DT_DATETIME:
                mySQLDataType = "DATETIME";
                break;
            case DT_BIGDECIMAL:
                mySQLDataType = "DECIMAL(" + defColumn.getLength() + defColumn.getDecimals() + "," + defColumn.getDecimals() + ")";
                break;
            case DT_CLOB:
                mySQLDataType = "TEXT";
                break;
            case DT_BLOB:
                mySQLDataType = "BLOB";
                break;
            default:
                throw new RuntimeException("El tipo de datos no es v�lido" + defColumn.getDataType());
        }
        return mySQLDataType;
    }

    /**
     * Obtiene la definici�n de una tabla en el diccionario a partir del nombre de dicha tabla.
     * No tiene en cuanta las mayusculas o minusculas en el nombre de la tabla.
     * @param tableName Nombre de la tabla
     * @return La definic�n de la tabla
     */
    private static DefTable getDefTable(String tableName) {
        DefTables defTables = BLSession.getDictionary().getDefTables();
        Iterator it = defTables.keySet().iterator();
        while (it.hasNext()) {
            DefTable defTable = defTables.getDefTable((String) it.next());
            if (defTable.getTableName().equalsIgnoreCase(tableName)) {
                return defTable;
            }
        }
        return null;
    }
}
