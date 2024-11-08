package org.etf.dbx.formatter.delta;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.etf.dbx.formatter.AbstractFormatter;
import org.etf.dbx.formatter.DbxFormatterException;
import org.etf.dbx.jaxb.ColumnType;
import org.etf.dbx.jaxb.FieldType;
import org.etf.dbx.jaxb.IndexType;
import org.etf.dbx.jaxb.Schema;
import org.etf.dbx.jaxb.SequenceType;
import org.etf.dbx.jaxb.TableType;
import org.etf.dbx.jaxb.IndexType.IndexSpec;
import org.etf.dbx.utils.Helper;

public class DeltaFormatter extends AbstractFormatter {

    private static final Logger logger = Logger.getLogger(DeltaFormatter.class);

    private static final String[][] sequenceFields = new String[5][];

    private static final String[][] columnFields = new String[2][];

    private static final String[][] indexFields = new String[2][];

    private static final String[][] indexSpecFields = new String[2][];

    static {
        sequenceFields[0] = new String[] { "cache", "cache" };
        sequenceFields[1] = new String[] { "incrementBy", "incrBy" };
        sequenceFields[2] = new String[] { "startWith", "startWith" };
        sequenceFields[3] = new String[] { "cycle", "cycle" };
        sequenceFields[4] = new String[] { "order", "order" };
        columnFields[0] = new String[] { "notNull", "notNull" };
        columnFields[1] = new String[] { "_default", "defaultValue" };
        indexFields[0] = new String[] { "columns", "columns" };
        indexFields[1] = new String[] { "defaultType", "defaultType" };
        indexSpecFields[0] = new String[] { "type", "type" };
        indexSpecFields[1] = new String[] { "options", "options" };
    }

    @Override
    protected void processImpl(Schema schema, String dirName) throws DbxFormatterException {
        throw new UnsupportedOperationException();
    }

    public void process(Schema prevSchema, Schema currSchema, String outDir) throws Exception {
        logger.info("Generating delta report...");
        String path = new File("templates/delta").getAbsolutePath();
        StringTemplateGroup templates = new StringTemplateGroup("tg", path);
        StringTemplate template = templates.getInstanceOf("delta");
        populateSequences(template, prevSchema, currSchema);
        populateTables(template, prevSchema, currSchema);
        String delta = template.toString();
        try {
            FileUtils.writeStringToFile(new File(outDir, "delta_report.html"), delta);
            FileUtils.copyFileToDirectory(new File("templates/delta/main.css"), new File(outDir));
        } catch (IOException ex) {
            throw new DbxFormatterException(ex);
        }
        logger.info("Delta report has been generated.");
    }

    private void populateTables(StringTemplate template, Schema prev, Schema curr) throws Exception {
        Map<String, TableType> currMap = Helper.toMap(curr.getTables().getTable(), TableType.class, "name");
        Map<String, TableType> prevMap = Helper.toMap(prev.getTables().getTable(), TableType.class, "name");
        int total = 0;
        List<Table> list = new ArrayList<Table>();
        for (TableType currTable : curr.getTables().getTable()) {
            if (!prevMap.containsKey(currTable.getName())) {
                list.add(createTable(currTable));
            }
        }
        template.setAttribute("added_tables", list);
        total += list.size();
        list = new ArrayList<Table>();
        for (TableType table : prev.getTables().getTable()) {
            if (!currMap.containsKey(table.getName())) {
                list.add(createTable(table));
            }
        }
        template.setAttribute("removed_tables", list);
        total += list.size();
        list = new ArrayList<Table>();
        for (TableType currTable : curr.getTables().getTable()) {
            TableType prevTable = prevMap.get(currTable.getName());
            if (prevTable != null) {
                boolean isModified = false;
                Table table = createTable(currTable);
                if (currTable.getColumn().size() != prevTable.getColumn().size()) {
                    table.getColumns().setModified(true);
                    isModified = true;
                }
                if (currTable.getIndex().size() != prevTable.getIndex().size()) {
                    table.getIndexes().setModified(true);
                    isModified = true;
                }
                DetailTable detailTable = createDetailTable(prevTable, currTable);
                table.setDetails(detailTable);
                if (isModified || detailTable.isModified()) {
                    list.add(table);
                }
            }
        }
        template.setAttribute("modified_tables", list);
        total += list.size();
        template.setAttribute("has_tables", total > 0);
    }

    private DetailTable createDetailTable(TableType prevTable, TableType currTable) throws Exception {
        DetailTable detailTable = new DetailTable();
        populateColumns(prevTable.getColumn(), currTable.getColumn(), detailTable);
        populateIndexes(prevTable.getIndex(), currTable.getIndex(), detailTable);
        return detailTable;
    }

    private void populateIndexes(List<IndexType> prevIdxes, List<IndexType> currIdxes, DetailTable detailTable) throws Exception {
        Map<String, IndexType> prevMap = Helper.toMap(prevIdxes, IndexType.class, "name");
        Map<String, IndexType> currMap = Helper.toMap(currIdxes, IndexType.class, "name");
        List<Index> list = new ArrayList<Index>();
        for (IndexType currIdx : currIdxes) {
            if (!prevMap.containsKey(currIdx.getName())) {
                list.add(createIndex(currIdx));
            }
        }
        detailTable.setAddedIndexes(list);
        list = new ArrayList<Index>();
        for (IndexType prevIdx : prevIdxes) {
            if (!currMap.containsKey(prevIdx.getName())) {
                list.add(createIndex(prevIdx));
            }
        }
        detailTable.setRemovedIndexes(list);
        list = new ArrayList<Index>();
        for (IndexType currIdx : currIdxes) {
            IndexType prevIdx = prevMap.get(currIdx.getName());
            if (prevIdx != null) {
                Index index = createIndex(currIdx);
                boolean isModified = merge(currIdx, prevIdx, index, indexFields);
                if (mergeIndexSpecs(currIdx.getIndexSpec(), prevIdx.getIndexSpec(), index)) {
                    isModified = true;
                }
                if (isModified) {
                    list.add(index);
                }
            }
        }
        detailTable.setModifiedIndexes(list);
        if (list.size() > 0) {
            detailTable.setModified(true);
        }
    }

    private boolean mergeIndexSpecs(List<IndexSpec> curr, List<IndexSpec> prev, Index index) throws Exception {
        boolean result = false;
        if (curr != null && prev != null) {
            Map<String, IndexSpec> prevMap = Helper.toMap(prev, IndexSpec.class, "dbName");
            Map<String, IndexDbSpec> map = Helper.toMap(index.getSpecs(), Element.class, "name");
            for (IndexSpec currSpec : curr) {
                String key = currSpec.getDbName().toString();
                IndexSpec prevSpec = prevMap.get(key);
                if (prevSpec != null) {
                    if (merge(currSpec, prevSpec, map.get(key), indexSpecFields)) {
                        result = true;
                    }
                }
            }
        }
        return result;
    }

    private Index createIndex(IndexType currIdx) {
        Index index = new Index(currIdx.getName());
        index.setColumns(new Element(currIdx.getColumns()));
        index.setDefaultType(new Element(Helper.toString(currIdx.getDefaultType(), "NORMAL")));
        if (currIdx.getIndexSpec() != null) {
            List<IndexDbSpec> list = new ArrayList<IndexDbSpec>();
            for (IndexSpec spec : currIdx.getIndexSpec()) {
                IndexDbSpec dbSpec = new IndexDbSpec(spec.getDbName().name());
                if (!StringUtils.isBlank(spec.getOptions())) {
                    dbSpec.setOptions(new Element(spec.getOptions()));
                }
                if (!StringUtils.isBlank(spec.getType())) {
                    dbSpec.setType(new Element(spec.getType()));
                }
                list.add(dbSpec);
            }
            index.setSpecs(list);
        }
        return index;
    }

    private void populateColumns(List<ColumnType> prevCols, List<ColumnType> currCols, DetailTable detailTable) throws Exception {
        Map<String, ColumnType> prevMap = Helper.toMap(prevCols, ColumnType.class, "name");
        Map<String, ColumnType> currMap = Helper.toMap(currCols, ColumnType.class, "name");
        List<Column> list = new ArrayList<Column>();
        for (ColumnType currCol : currCols) {
            if (!prevMap.containsKey(currCol.getName())) {
                list.add(createColumn(currCol));
            }
        }
        detailTable.setAddedColumns(list);
        list = new ArrayList<Column>();
        for (ColumnType prevCol : prevCols) {
            if (!currMap.containsKey(prevCol.getName())) {
                list.add(createColumn(prevCol));
            }
        }
        detailTable.setRemovedColumns(list);
        list = new ArrayList<Column>();
        for (ColumnType currCol : currCols) {
            ColumnType prevCol = prevMap.get(currCol.getName());
            if (prevCol != null) {
                Column column = createColumn(currCol);
                boolean isModified = merge(currCol, prevCol, column, columnFields);
                if (!Helper.isEquals(getType(currCol), getType(prevCol))) {
                    column.getType().setModified(true);
                    isModified = true;
                }
                if (isModified) {
                    list.add(column);
                }
            }
        }
        detailTable.setModifiedColumns(list);
        if (list.size() > 0) {
            detailTable.setModified(true);
        }
    }

    private Column createColumn(ColumnType currCol) {
        Column column = new Column(currCol.getName());
        column.setDefaultValue(new Element(Helper.toString(currCol.getDefault(), "")));
        column.setNotNull(new Element(String.valueOf(currCol.isNotNull())));
        column.setType(new Element(getType(currCol)));
        return column;
    }

    private String getType(ColumnType col) {
        if (col.getType() != null) {
            return ((FieldType) col.getType()).getName();
        }
        return "";
    }

    private Table createTable(TableType tableType) {
        Table table = new Table(tableType.getName());
        table.setColumns(new Element(String.valueOf(Helper.listSize(tableType.getColumn()))));
        table.setIndexes(new Element(String.valueOf(Helper.listSize(tableType.getIndex()))));
        if (tableType.getPrimaryKey() != null) {
            table.setPk(new Element(tableType.getPrimaryKey().getColumns()));
        } else {
            table.setPk(new Element("NONE"));
        }
        return table;
    }

    private void populateSequences(StringTemplate template, Schema prev, Schema curr) throws Exception {
        Map<String, SequenceType> currMap = Helper.toMap(curr.getSequences().getSequence(), SequenceType.class, "name");
        Map<String, SequenceType> prevMap = Helper.toMap(prev.getSequences().getSequence(), SequenceType.class, "name");
        int total = 0;
        List<Sequence> list = new ArrayList<Sequence>();
        for (SequenceType currSeq : curr.getSequences().getSequence()) {
            if (!prevMap.containsKey(currSeq.getName())) {
                list.add(createSequence(currSeq));
            }
        }
        template.setAttribute("added_seq", list);
        total += list.size();
        list = new ArrayList<Sequence>();
        for (SequenceType seq : prev.getSequences().getSequence()) {
            if (!currMap.containsKey(seq.getName())) {
                list.add(createSequence(seq));
            }
        }
        template.setAttribute("removed_seq", list);
        total += list.size();
        list = new ArrayList<Sequence>();
        for (SequenceType currSeq : curr.getSequences().getSequence()) {
            SequenceType prevSeq = prevMap.get(currSeq.getName());
            if (prevSeq != null) {
                Sequence sequence = createSequence(currSeq);
                boolean isModified = merge(currSeq, prevSeq, sequence, sequenceFields);
                if (isModified) {
                    list.add(sequence);
                }
            }
        }
        template.setAttribute("modified_seq", list);
        total += list.size();
        template.setAttribute("has_seq", total > 0);
    }

    private Sequence createSequence(SequenceType seqType) {
        Sequence sequence = new Sequence(seqType.getName());
        sequence.setCache(new Element(Helper.toString(seqType.getCache(), "NONE")));
        sequence.setCycle(new Element(String.valueOf(seqType.isCycle())));
        sequence.setIncrBy(new Element(Helper.toString(seqType.getIncrementBy(), "DEFAULT")));
        sequence.setOrder(new Element(String.valueOf(seqType.isOrder())));
        sequence.setStartWith(new Element(Helper.toString(seqType.getStartWith(), "DEFAULT")));
        return sequence;
    }

    private <T> boolean merge(T obj1, T obj2, Object element, String[][] fields) throws Exception {
        boolean result = false;
        for (int i = 0; i < fields.length; i++) {
            Field field = obj1.getClass().getDeclaredField(fields[i][0]);
            boolean isAcc = field.isAccessible();
            field.setAccessible(true);
            boolean isModified = false;
            Class<?> klass = field.getDeclaringClass();
            if (klass.isPrimitive()) {
                if (klass == int.class) {
                    isModified = field.getInt(obj1) != field.getInt(obj2);
                } else if (klass == boolean.class) {
                    isModified = field.getBoolean(obj1) != field.getBoolean(obj2);
                } else if (klass == byte.class) {
                    isModified = field.getByte(obj1) != field.getByte(obj2);
                } else if (klass == char.class) {
                    isModified = field.getChar(obj1) != field.getChar(obj2);
                } else if (klass == double.class) {
                    isModified = field.getDouble(obj1) != field.getDouble(obj2);
                } else if (klass == float.class) {
                    isModified = field.getFloat(obj1) != field.getFloat(obj2);
                } else if (klass == long.class) {
                    isModified = field.getLong(obj1) != field.getLong(obj2);
                } else if (klass == short.class) {
                    isModified = field.getShort(obj1) != field.getShort(obj2);
                }
            } else {
                isModified = !Helper.isEquals(field.get(obj1), field.get(obj2));
            }
            field.setAccessible(isAcc);
            if (isModified) {
                Field elField = element.getClass().getDeclaredField(fields[i][1]);
                isAcc = elField.isAccessible();
                elField.setAccessible(true);
                ((Element) elField.get(element)).setModified(true);
                elField.setAccessible(isAcc);
                result = true;
            }
        }
        return result;
    }
}
