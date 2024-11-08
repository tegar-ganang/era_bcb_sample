package rafaelortis.dbsmartcopy.metadataviewer.dbanalizer;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.plaf.basic.BasicTabbedPaneUI.TabSelectionHandler;
import rafaelortis.dbsmartcopy.metadataviewer.IOHelper;

/**
 *
 * @author ortis
 */
public class DBAnalizer {

    private Connection conn;

    private DatabaseMetaData databaseMetaData;

    public Connection getConn() {
        return conn;
    }

    public void setConn(Connection conn) {
        this.conn = conn;
    }

    public DBAnalizer(Connection conn) throws SQLException {
        this.conn = conn;
        databaseMetaData = conn.getMetaData();
    }

    public TableMetaData analizeTable(String schema, String tableName) throws Exception {
        ResultSet pks = null;
        ResultSet columns = null;
        ResultSet resultSet1 = null;
        ResultSet resultSet2 = null;
        try {
            TableMetaData table = new TableMetaData();
            table.setConn(conn);
            table.setTableName(tableName);
            if (schema != null) {
                table.setSchema(schema);
            }
            pks = databaseMetaData.getPrimaryKeys(null, null, tableName);
            while (pks.next()) {
                String columnName = pks.getString("COLUMN_NAME");
                table.getPkColumns().add(columnName);
            }
            columns = databaseMetaData.getColumns(null, null, tableName, "%");
            while (columns.next()) {
                if (table.getSchema() == null) table.setSchema(columns.getString("TABLE_SCHEM"));
                String columnName = columns.getString("COLUMN_NAME");
                String columnType = columns.getString("TYPE_NAME");
                table.getColumnsNames().add(columnName);
                table.getColumnsTypes().put(columnName, columnType);
            }
            resultSet1 = databaseMetaData.getImportedKeys(null, null, tableName);
            while (resultSet1.next()) {
                String iTable = resultSet1.getString("PKTABLE_NAME");
                String iColumn = resultSet1.getString("PKCOLUMN_NAME");
                String column = resultSet1.getString("FKCOLUMN_NAME");
                String pkName = resultSet1.getString("FK_NAME");
                String iSchema = resultSet1.getString("PKTABLE_SCHEM");
                if (fkInList(pkName, table.getImportedKeys())) {
                    findInList(pkName, table.getImportedKeys()).getReference().put(column, iColumn);
                } else {
                    table.getImportedKeys().add(new ForeignKey(iTable, iColumn, column, pkName, ForeignKey.IMPORTED_KEY, tableName, iSchema));
                }
            }
            resultSet2 = databaseMetaData.getExportedKeys(null, null, tableName);
            while (resultSet2.next()) {
                String iTable = resultSet2.getString("FKTABLE_NAME");
                String iColumn = resultSet2.getString("FKCOLUMN_NAME");
                String column = resultSet2.getString("PKCOLUMN_NAME");
                String fkName = resultSet2.getString("FK_NAME");
                String iSchema = resultSet2.getString("FKTABLE_SCHEM");
                if (fkInList(fkName, table.getExportedKeys())) {
                    findInList(fkName, table.getExportedKeys()).getReference().put(column, iColumn);
                } else {
                    table.getExportedKeys().add(new ForeignKey(iTable, iColumn, column, fkName, ForeignKey.EXPORTED_KEY, tableName, iSchema));
                }
            }
            return table;
        } finally {
            if (pks != null) pks.close();
            if (columns != null) columns.close();
            if (resultSet1 != null) resultSet1.close();
            if (resultSet2 != null) resultSet2.close();
        }
    }

    public void findReferences2(TableMetaData tableMetaData, List<String> tables, List<String> tablesAnalized) throws Exception {
        if (tablesAnalized == null) tablesAnalized = new ArrayList<String>();
        List<TableMetaData> references = new ArrayList<TableMetaData>(0);
        List<TableMetaData> importedReferences = new ArrayList<TableMetaData>(0);
        List<TableMetaData> exportedReferences = new ArrayList<TableMetaData>(0);
        for (String table : tables) {
            TableMetaData tableMd = analizeTable(null, table);
            tablesAnalized.add(table);
        }
        tableMetaData.getReferences().addAll(references);
    }

    public void findReferences(TableMetaData tableMetaData, List<ForeignKey> importedFKsAnalized, List<ForeignKey> exportedFKsAnalized, String referencedBy, String referenceHist, boolean imported, boolean exported) throws Exception {
        IOHelper.writeInfo("Finding references of " + tableMetaData.getSchema() + "." + tableMetaData.getTableName());
        if (importedFKsAnalized == null) importedFKsAnalized = new ArrayList<ForeignKey>(0);
        if (exportedFKsAnalized == null) exportedFKsAnalized = new ArrayList<ForeignKey>(0);
        List<TableMetaData> references = new ArrayList<TableMetaData>(0);
        List<TableMetaData> importedReferences = new ArrayList<TableMetaData>(0);
        List<TableMetaData> exportedReferences = new ArrayList<TableMetaData>(0);
        if (referenceHist == null) referenceHist = tableMetaData.getTableName().toUpperCase(); else referenceHist += "->" + tableMetaData.getTableName().toUpperCase();
        for (ForeignKey fk : tableMetaData.getImportedKeys()) {
            if (!importedFKsAnalized.contains(fk) && !fk.getReferenceTable().equalsIgnoreCase(referencedBy) && !tableMetaData.getTableName().equalsIgnoreCase(fk.getReferenceTable())) {
                IOHelper.writeInfo("Analizing imported fk " + fk.getConstraintName() + " from " + referenceHist);
                importedFKsAnalized.add(fk);
                TableMetaData fkMetaData = analizeTable(tableMetaData.getSchema(), fk.getReferenceTable());
                fkMetaData.setType(TableMetaData.MD_FROM_IMPORTED_KEY);
                references.add(fkMetaData);
                importedReferences.add(fkMetaData);
            } else {
                IOHelper.writeInfo(fk.getConstraintName() + " already analized");
            }
        }
        for (ForeignKey fk : tableMetaData.getExportedKeys()) {
            if (!importedFKsAnalized.contains(fk) && !fk.getReferenceTable().equalsIgnoreCase(referencedBy) && !tableMetaData.getTableName().equalsIgnoreCase(fk.getReferenceTable()) && exported) {
                IOHelper.writeInfo("Analizing exported fk " + fk.getConstraintName() + " from " + referenceHist);
                exportedFKsAnalized.add(fk);
                TableMetaData fkMetaData = analizeTable(tableMetaData.getSchema(), fk.getReferenceTable());
                fkMetaData.setType(TableMetaData.MD_FROM_EXPORTED_KEY);
                references.add(fkMetaData);
                exportedReferences.add(fkMetaData);
            } else {
                IOHelper.writeInfo(fk.getConstraintName() + " already analized");
            }
        }
        for (TableMetaData tmd : references) {
            List<TableMetaData> ref2Pass = new ArrayList<TableMetaData>();
            for (ForeignKey fk : tmd.getImportedKeys()) {
                if (!importedFKsAnalized.contains(fk) && !fk.getReferenceTable().equalsIgnoreCase(referencedBy) && !tmd.getTableName().equalsIgnoreCase(fk.getReferenceTable())) {
                    IOHelper.writeInfo("Analizing 2 pass imported fk " + fk.getConstraintName() + " from " + referenceHist + "->" + tmd.getTableName());
                    importedFKsAnalized.add(fk);
                    TableMetaData fkMetaData = analizeTable(tmd.getSchema(), fk.getReferenceTable());
                    fkMetaData.setType(TableMetaData.MD_FROM_IMPORTED_KEY);
                    findReferences(fkMetaData, importedFKsAnalized, exportedFKsAnalized, tmd.getTableName(), referenceHist + "->" + tmd.getTableName(), true, false);
                    ref2Pass.add(fkMetaData);
                } else {
                    IOHelper.writeInfo(fk.getConstraintName() + " already analized");
                }
            }
            tmd.getReferences().addAll(ref2Pass);
            tmd.getReferencesImported().addAll(ref2Pass);
        }
        for (TableMetaData tmd : references) {
            List<TableMetaData> ref2Pass = new ArrayList<TableMetaData>();
            List<TableMetaData> exportRef2Pass = new ArrayList<TableMetaData>();
            for (ForeignKey fk : tmd.getExportedKeys()) {
                if (!exportedFKsAnalized.contains(fk) && !fk.getReferenceTable().equalsIgnoreCase(referencedBy) && !tmd.getTableName().equalsIgnoreCase(fk.getReferenceTable()) && exported) {
                    IOHelper.writeInfo("Analizing 2 pass exported fk " + fk.getConstraintName() + " from " + referenceHist + "->" + tmd.getTableName());
                    exportedFKsAnalized.add(fk);
                    TableMetaData fkMetaData = analizeTable(tmd.getSchema(), fk.getReferenceTable());
                    fkMetaData.setType(TableMetaData.MD_FROM_EXPORTED_KEY);
                    findReferences(fkMetaData, importedFKsAnalized, exportedFKsAnalized, tmd.getTableName(), referenceHist + "->" + tmd.getTableName(), true, true);
                    ref2Pass.add(fkMetaData);
                } else {
                    IOHelper.writeInfo(fk.getConstraintName() + " already analized");
                }
            }
            tmd.getReferences().addAll(ref2Pass);
            tmd.getReferencesExported().addAll(ref2Pass);
        }
        tableMetaData.getReferences().addAll(references);
        tableMetaData.getReferencesImported().addAll(importedReferences);
        tableMetaData.getReferencesExported().addAll(exportedReferences);
    }

    public void readAllRelatedData(TableMetaData tableMetaData, List<String> tablesRead, String referencedBy) throws Exception {
        if (tablesRead == null) tablesRead = new ArrayList<String>(0);
        if (!tablesRead.contains(tableMetaData.getTableName())) {
            DBUtil.readTableData(tableMetaData, null);
            tablesRead.add(tableMetaData.getTableName());
        }
        for (TableMetaData refTableMetaData : tableMetaData.getReferences()) {
            if (!tablesRead.contains(refTableMetaData.getTableName()) && !refTableMetaData.getTableName().equalsIgnoreCase(referencedBy)) {
                readAllRelatedData(refTableMetaData, tablesRead, tableMetaData.getTableName());
            }
        }
    }

    public void readFilteredRelatedData(TableMetaData tableMetaData, List<String> tablesRead, String referencedBy, String filter, List<Row> relatedData, boolean imported) throws Exception {
        boolean initialTable = false;
        boolean readImportsAgain = false;
        if (tablesRead == null) {
            tablesRead = new ArrayList<String>(0);
            DBUtil.readTableData(tableMetaData, filter);
            tablesRead.add(tableMetaData.getTableName());
        } else {
            if (!tablesRead.contains(tableMetaData.getTableName() + referencedBy)) {
                DBUtil.readTableDataByReference(tableMetaData, referencedBy, relatedData, imported);
                if (tableMetaData.getData().size() > 0) {
                    tablesRead.add(tableMetaData.getTableName() + referencedBy);
                    readImportsAgain = true;
                }
            }
        }
        for (TableMetaData refTableMetaData : tableMetaData.getReferencesImported()) {
            if ((!tablesRead.contains(refTableMetaData.getTableName() + tableMetaData.getTableName()) || readImportsAgain) && !refTableMetaData.getTableName().equalsIgnoreCase(referencedBy)) {
                IOHelper.writeInfo("Reading reference data on " + refTableMetaData.getTableName() + " from " + tableMetaData.getTableName());
                readFilteredRelatedData(refTableMetaData, tablesRead, tableMetaData.getTableName(), filter, tableMetaData.getData(), true);
            } else if ("advogado_processo_origem".equals(tableMetaData.getTableName())) {
                IOHelper.writeInfo("Reading reference data on " + refTableMetaData.getTableName() + " from " + tableMetaData.getTableName() + "already done");
            }
        }
        for (TableMetaData refTableMetaData : tableMetaData.getReferencesExported()) {
            if (!tablesRead.contains(refTableMetaData.getTableName() + tableMetaData.getTableName()) && !refTableMetaData.getTableName().equalsIgnoreCase(referencedBy)) {
                IOHelper.writeInfo("Reading reference data on " + refTableMetaData.getTableName() + " from " + tableMetaData.getTableName());
                readFilteredRelatedData(refTableMetaData, tablesRead, tableMetaData.getTableName(), filter, tableMetaData.getData(), false);
            } else if ("advogado_processo_origem".equals(tableMetaData.getTableName())) {
                IOHelper.writeInfo("Reading reference data on " + refTableMetaData.getTableName() + " from " + tableMetaData.getTableName() + "already done");
            }
        }
    }

    public void writeAllRelatedData(Connection dest, TableMetaData tableMetaData, List<String> tablesWritten, String referencedBy) throws Exception {
        if (tablesWritten == null) tablesWritten = new ArrayList<String>(0);
        IOHelper.writeInfo("Writting " + tableMetaData.getTableName());
        for (TableMetaData refTableMetaData : tableMetaData.getReferencesImported()) {
            IOHelper.writeInfo("searching imported ref to write: " + refTableMetaData.getTableName());
            if (!tablesWritten.contains(refTableMetaData.getTableName()) && !refTableMetaData.getTableName().equalsIgnoreCase(referencedBy) && tableInList(tableMetaData.getImportedKeys(), refTableMetaData.getTableName()) && (tableMetaData.getType() == TableMetaData.MD_FROM_IMPORTED_KEY || tableMetaData.getReferencesImported().contains(refTableMetaData))) {
                IOHelper.writeInfo("will write imported: " + refTableMetaData.getTableName());
                writeAllRelatedData(dest, refTableMetaData, tablesWritten, tableMetaData.getTableName());
            } else {
                IOHelper.writeInfo("not imported:" + refTableMetaData.getTableName());
            }
        }
        if (!tablesWritten.contains(tableMetaData.getTableName())) {
            DBUtil.insertTableData(dest, tableMetaData);
            tablesWritten.add(tableMetaData.getTableName());
        }
        for (TableMetaData refTableMetaData : tableMetaData.getReferencesExported()) {
            if (!tablesWritten.contains(refTableMetaData.getTableName()) && !refTableMetaData.getTableName().equalsIgnoreCase(referencedBy) && tableInList(tableMetaData.getExportedKeys(), refTableMetaData.getTableName())) {
                writeAllRelatedData(dest, refTableMetaData, tablesWritten, tableMetaData.getTableName());
            }
        }
    }

    private boolean tableInList(List<ForeignKey> keysList, String tableName) {
        for (ForeignKey fk : keysList) {
            if (tableName.equalsIgnoreCase(fk.getReferenceTable())) return true;
        }
        return false;
    }

    private boolean fkInList(String pkName, List<ForeignKey> fkList) {
        for (ForeignKey fk : fkList) {
            if (fk.getConstraintName().equalsIgnoreCase(pkName)) return true;
        }
        return false;
    }

    private ForeignKey findInList(String pkName, List<ForeignKey> fkList) {
        for (ForeignKey fk : fkList) {
            if (fk.getConstraintName().equalsIgnoreCase(pkName)) return fk;
        }
        return null;
    }

    public TableMetaData analizeByLayers(String schema, String tableName) throws Exception {
        TableMetaData metaData = analizeTable(schema, tableName);
        List<String> tablesAnalized = new ArrayList<String>();
        List<ForeignKey> nextLayer = buildNextLayerList(metaData, tablesAnalized, true);
        if (nextLayer.size() > 0) metaData = analizeNextLayer(nextLayer, metaData, tablesAnalized, true);
        nextLayer = buildNextLayerList(metaData, tablesAnalized, false);
        if (nextLayer.size() > 0) metaData = analizeNextLayer(nextLayer, metaData, tablesAnalized, false);
        for (TableMetaData mt : metaData.getReferences()) {
            for (ForeignKey fk : mt.getImportedKeys()) {
                TableMetaData impMt = findMetadata(metaData, fk.getReferenceTable());
                if (!mt.getReferencesImported().contains(impMt) && !mt.getTableName().equalsIgnoreCase(impMt.getTableName())) mt.getReferencesImported().add(impMt);
            }
        }
        return metaData;
    }

    private List<ForeignKey> buildNextLayerList(TableMetaData metaData, List<String> tablesAnalized, boolean imported) throws Exception {
        List<ForeignKey> nextLayer = new ArrayList<ForeignKey>();
        if (imported) {
            for (ForeignKey fk : metaData.getImportedKeys()) {
                IOHelper.writeInfo("Processing " + fk.getConstraintName() + " from " + metaData.getTableName());
                if (!tablesAnalized.contains(fk.getConstraintName()) && metaData.getSchema().equals(fk.getReferenceSchema())) nextLayer.add(fk); else IOHelper.writeInfo("Import pass - imported fk " + fk.getConstraintName().toUpperCase() + "-" + fk.getConstraintTableName() + " from " + fk.getReferenceTable().toUpperCase() + " but was already analized");
            }
        } else {
            for (ForeignKey fk : metaData.getImportedKeys()) {
                IOHelper.writeInfo("Processing " + fk.getConstraintName() + " from " + metaData.getTableName());
                if (!tablesAnalized.contains(fk.getConstraintName()) && metaData.getSchema().equals(fk.getReferenceSchema())) nextLayer.add(fk); else IOHelper.writeInfo("Export pass - imported fk " + fk.getConstraintName().toUpperCase() + "-" + fk.getConstraintTableName() + " from " + fk.getReferenceTable().toUpperCase() + " but was already analized");
            }
            for (ForeignKey fk : metaData.getExportedKeys()) {
                IOHelper.writeInfo("Processing " + fk.getConstraintName() + " from " + metaData.getTableName());
                if (!tablesAnalized.contains(fk.getConstraintName()) && metaData.getSchema().equals(fk.getReferenceSchema())) nextLayer.add(fk); else IOHelper.writeInfo("Export pass - exported pk " + fk.getConstraintName().toUpperCase() + "-" + fk.getConstraintTableName() + " from " + fk.getReferenceTable().toUpperCase() + " but was already analized");
            }
        }
        return nextLayer;
    }

    private TableMetaData analizeNextLayer(List<ForeignKey> nextLayer, TableMetaData metaData, List<String> tablesAnalized, boolean imported) throws Exception {
        List<ForeignKey> currLayer = new ArrayList<ForeignKey>(nextLayer);
        nextLayer.clear();
        for (ForeignKey nextFk : currLayer) {
            TableMetaData mt = null;
            if (!tablesAnalized.contains(nextFk.getConstraintName())) {
                mt = analizeTable(metaData.getSchema(), nextFk.getReferenceTable());
                if (!metaData.getReferences().contains(mt) && !metaData.getTableName().equalsIgnoreCase(mt.getTableName())) {
                    metaData.getReferences().add(mt);
                } else {
                    mt = findMetadata(metaData, nextFk.getReferenceTable());
                }
                tablesAnalized.add(nextFk.getConstraintName());
            } else {
                IOHelper.writeInfo("Table already analized " + nextFk.getReferenceTable());
                mt = findMetadata(metaData, nextFk.getReferenceTable());
            }
            if (nextFk.getType() == ForeignKey.EXPORTED_KEY) {
                IOHelper.writeInfo(mt.getTableName() + " added to exported ref on " + findMetadata(metaData, nextFk.getConstraintTableName()).getTableName());
                if (!mt.getTableName().equalsIgnoreCase(nextFk.getConstraintTableName())) findMetadata(metaData, nextFk.getConstraintTableName()).getReferencesExported().add(mt);
                mt.setType(TableMetaData.MD_FROM_EXPORTED_KEY);
            } else {
                IOHelper.writeInfo(mt.getTableName() + " added to imported ref on " + findMetadata(metaData, nextFk.getConstraintTableName()).getTableName());
                if (!mt.getTableName().equalsIgnoreCase(nextFk.getConstraintTableName())) findMetadata(metaData, nextFk.getConstraintTableName()).getReferencesImported().add(mt);
                mt.setType(TableMetaData.MD_FROM_IMPORTED_KEY);
            }
            nextLayer.addAll(buildNextLayerList(mt, tablesAnalized, imported));
        }
        IOHelper.writeInfo("End layer");
        if (nextLayer.size() > 0) {
            IOHelper.writeInfo("Begining next layer. Size :" + nextLayer.size());
            IOHelper.writeInfo("Next layer tables:");
            for (ForeignKey table : nextLayer) {
                IOHelper.writeInfo(table.getReferenceTable());
            }
            return analizeNextLayer(nextLayer, metaData, tablesAnalized, imported);
        } else {
            return metaData;
        }
    }

    private TableMetaData findMetadata(TableMetaData baseMeta, String table) throws Exception {
        for (TableMetaData meta : baseMeta.getReferences()) {
            if (meta.getTableName().equals(table)) return meta;
        }
        return baseMeta;
    }
}
