package org.paradox.data;

import org.paradox.ParadoxConnection;
import org.paradox.utils.filefilters.TableFilter;
import org.paradox.metadata.ParadoxField;
import org.paradox.metadata.ParadoxTable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import org.paradox.data.table.value.AbstractFieldValue;
import org.paradox.data.table.value.BooleanValue;
import org.paradox.data.table.value.DateValue;
import org.paradox.data.table.value.DoubleValue;
import org.paradox.data.table.value.IntegerValue;
import org.paradox.data.table.value.StringValue;
import org.paradox.data.table.value.TimeValue;
import org.paradox.utils.DateUtils;

/**
 * Paradox table manipulation
 *
 * @author Leonardo Alves da Costa
 * @since 03/12/2009
 * @version 1.0
 */
public final class TableData {

    public static ArrayList<ParadoxTable> listTables(final ParadoxConnection conn, final String tableName) throws SQLException {
        final ArrayList<ParadoxTable> tables = new ArrayList<ParadoxTable>();
        final File[] fileList = conn.getDir().listFiles(new TableFilter(tableName));
        for (final File file : fileList) {
            final ParadoxTable table;
            try {
                table = loadTableHeader(conn, file);
            } catch (final IOException ex) {
                throw new SQLException("Error loading Paradox tables.", ex);
            }
            if (table.isValid()) {
                tables.add(table);
            }
        }
        return tables;
    }

    public static ArrayList<ParadoxTable> listTables(final ParadoxConnection conn) throws SQLException {
        final ArrayList<ParadoxTable> tables = new ArrayList<ParadoxTable>();
        final File[] fileList = conn.getDir().listFiles(new TableFilter());
        for (final File file : fileList) {
            final ParadoxTable table;
            try {
                table = loadTableHeader(conn, file);
            } catch (final IOException ex) {
                throw new SQLException("Error loading Paradox tables.", ex);
            }
            if (table.isValid()) {
                tables.add(table);
            }
        }
        return tables;
    }

    public static ArrayList<ArrayList<AbstractFieldValue>> loadData(final ParadoxConnection conn, final ParadoxTable table, final Collection<ParadoxField> fields) throws IOException, SQLException {
        final ArrayList<ArrayList<AbstractFieldValue>> ret = new ArrayList<ArrayList<AbstractFieldValue>>();
        final FileInputStream fs = new FileInputStream(table.getFile());
        final int blockSize = table.getBlockSizeBytes();
        final int recordSize = table.getRecordSize();
        final int headerSize = table.getHeaderSize();
        final ByteBuffer buffer = ByteBuffer.allocate(blockSize);
        FileChannel channel = null;
        try {
            channel = fs.getChannel();
            if (table.getUsedBlocks() > 0) {
                int nextBlock = 1;
                do {
                    buffer.order(ByteOrder.LITTLE_ENDIAN);
                    channel.position(headerSize + ((nextBlock - 1) * blockSize));
                    buffer.clear();
                    channel.read(buffer);
                    buffer.flip();
                    nextBlock = buffer.getShort();
                    buffer.getShort();
                    final int addDataSize = buffer.getShort();
                    final int rowsInBlock = (addDataSize / recordSize) + 1;
                    buffer.order(ByteOrder.BIG_ENDIAN);
                    for (int loop = 0; loop < rowsInBlock; loop++) {
                        final ArrayList<AbstractFieldValue> row = new ArrayList<AbstractFieldValue>();
                        for (final ParadoxField field : table.getFields()) {
                            AbstractFieldValue fieldValue = null;
                            switch(field.getType()) {
                                case 1:
                                    {
                                        final ByteBuffer value = ByteBuffer.allocate(field.getSize());
                                        for (int chars = 0; chars < field.getSize(); chars++) {
                                            value.put(buffer.get());
                                        }
                                        value.flip();
                                        final String v = table.getCharset().decode(value).toString();
                                        fieldValue = new StringValue(v);
                                        break;
                                    }
                                case 2:
                                    {
                                        int a1 = (0x000000FF & ((int) buffer.get()));
                                        int a2 = (0x000000FF & ((int) buffer.get()));
                                        int a3 = (0x000000FF & ((int) buffer.get()));
                                        int a4 = (0x000000FF & ((int) buffer.get()));
                                        long days = ((long) (a1 << 24 | a2 << 16 | a3 << 8 | a4)) & 0x0FFFFFFFL;
                                        if ((a1 & 0xB0) != 0) {
                                            final Date date = DateUtils.SdnToGregorian(days + 1721425);
                                            fieldValue = new DateValue(date);
                                        } else {
                                            fieldValue = new DateValue(null);
                                        }
                                        break;
                                    }
                                case 3:
                                    {
                                        final int v = buffer.getShort();
                                        fieldValue = new IntegerValue(v);
                                        break;
                                    }
                                case 5:
                                case 6:
                                    {
                                        final double v = buffer.getDouble() * -1;
                                        if (Double.compare(Double.NEGATIVE_INFINITY, 1 / v) == 0) {
                                            fieldValue = new DoubleValue(null);
                                        } else {
                                            fieldValue = new DoubleValue(v);
                                        }
                                        break;
                                    }
                                case 9:
                                    {
                                        final byte v = buffer.get();
                                        if (v == 0) {
                                            fieldValue = new BooleanValue(null);
                                        } else if (v == -127) {
                                            fieldValue = new BooleanValue(Boolean.TRUE);
                                        } else if (v == -128) {
                                            fieldValue = new BooleanValue(Boolean.TRUE);
                                        } else {
                                            throw new SQLException("Invalid value " + v + ".");
                                        }
                                        break;
                                    }
                                case 0x14:
                                    {
                                        int a1 = (0x000000FF & ((int) buffer.get()));
                                        int a2 = (0x000000FF & ((int) buffer.get()));
                                        int a3 = (0x000000FF & ((int) buffer.get()));
                                        int a4 = (0x000000FF & ((int) buffer.get()));
                                        long timeInMillis = ((long) (a1 << 24 | a2 << 16 | a3 << 8 | a4)) & 0x0FFFFFFFL;
                                        if ((a1 & 0xB0) != 0) {
                                            final Calendar calendar = new GregorianCalendar(1, 0, 0);
                                            calendar.add(Calendar.MILLISECOND, (int) timeInMillis);
                                            final Time time = new Time(calendar.getTimeInMillis());
                                            fieldValue = new TimeValue(time);
                                        } else {
                                            fieldValue = new TimeValue(null);
                                        }
                                        break;
                                    }
                                case 0x16:
                                    {
                                        int v = buffer.getInt() & 0x0FFFFFFF;
                                        fieldValue = new IntegerValue(v);
                                        break;
                                    }
                                default:
                                    throw new SQLException("Type " + field.getType() + " not found.");
                            }
                            if (fields.contains(field)) {
                                row.add(fieldValue);
                            }
                        }
                        ret.add(row);
                    }
                } while (nextBlock != 0);
            }
        } finally {
            if (channel != null) {
                channel.close();
            }
            fs.close();
        }
        return ret;
    }

    private static ParadoxTable loadTableHeader(final ParadoxConnection conn, final File file) throws IOException {
        final FileInputStream fs = new FileInputStream(file);
        final ParadoxTable table = new ParadoxTable(file, file.getName());
        final ByteBuffer buffer = ByteBuffer.allocate(2048);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        FileChannel channel = null;
        try {
            channel = fs.getChannel();
            channel.read(buffer);
            buffer.flip();
            table.setRecordSize(buffer.getShort());
            table.setHeaderSize(buffer.getShort());
            table.setType(buffer.get());
            table.setBlockSize(buffer.get());
            table.setRowCount(buffer.getInt());
            table.setUsedBlocks(buffer.getShort());
            table.setTotalBlocks(buffer.getShort());
            table.setFirstBlock(buffer.getShort());
            table.setLastBlock(buffer.getShort());
            buffer.position(0x21);
            table.setFieldCount(buffer.getShort());
            table.setPrimaryFieldCount(buffer.getShort());
            buffer.position(0x38);
            table.setWriteProtected(buffer.get());
            table.setVersionId(buffer.get());
            buffer.position(0x49);
            table.setAutoIncrementValue(buffer.getInt());
            table.setFirstFreeBlock(buffer.getShort());
            buffer.position(0x55);
            table.setReferencialIntegrity(buffer.get());
            if (table.getVersionId() > 4) {
                buffer.position(0x6A);
                table.setCharset(Charset.forName("cp" + buffer.getShort()));
                buffer.position(0x78);
            } else {
                buffer.position(0x58);
            }
            final ArrayList<ParadoxField> fields = new ArrayList<ParadoxField>();
            for (int loop = 0; loop < table.getFieldCount(); loop++) {
                final ParadoxField field = new ParadoxField();
                field.setType(buffer.get());
                field.setSize(buffer.get());
                field.setTableName(table.getName());
                field.setTable(table);
                fields.add(field);
            }
            if (table.getVersionId() > 4) {
                if (table.getVersionId() == 0xC) {
                    buffer.position(0x78 + 261 + 4 + 6 * fields.size());
                } else {
                    buffer.position(0x78 + 83 + 6 * fields.size());
                }
            } else {
                buffer.position(0x58 + 83 + 6 * fields.size());
            }
            for (int loop = 0; loop < table.getFieldCount(); loop++) {
                final ByteBuffer name = ByteBuffer.allocate(261);
                while (true) {
                    final byte c = buffer.get();
                    if (c == 0) {
                        break;
                    }
                    name.put(c);
                }
                name.flip();
                fields.get(loop).setName(table.getCharset().decode(name).toString());
            }
            table.setFields(fields);
            final ArrayList<Short> fieldsOrder = new ArrayList<Short>();
            for (int loop = 0; loop < table.getFieldCount(); loop++) {
                fieldsOrder.add(buffer.getShort());
            }
            table.setFieldsOrder(fieldsOrder);
        } finally {
            if (channel != null) {
                channel.close();
            }
            fs.close();
        }
        return table;
    }
}
