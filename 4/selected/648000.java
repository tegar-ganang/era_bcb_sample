package nl.knaw.dans.common.dbflib;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Encapsulates the DBF table header.
 *
 * @author Jan van Mansum
 * @author Vesa Åkerman
 */
class DbfHeader {

    static final int OFFSET_VERSION = 0;

    static final int OFFSET_MODIFIED_DATE = 1;

    static final int OFFSET_RECORD_COUNT = 4;

    static final int OFFSET_HEADER_LENGTH = 8;

    static final int OFFSET_RECORD_LENGTH = 10;

    static final int OFFSET_RESERVED_1 = 12;

    static final int OFFSET_INCOMPLETE_TRANSATION = 14;

    static final int OFFSET_ENCRYPTION_FLAG = 15;

    static final int OFFSET_FREE_RECORD_THREAD = 16;

    static final int OFFSET_RESERVED_2 = 20;

    static final int OFFSET_MDX_FLAG = 28;

    static final int OFFSET_LANGUAGE_DRIVER = 29;

    static final int OFFSET_RESERVED_3 = 30;

    static final int OFFSET_FIELD_DESCRIPTORS = 32;

    static final int FD_OFFSET_NAME = 0;

    static final int FD_OFFSET_TYPE = 11;

    static final int FD_OFFSET_DATA_ADDRESS = 12;

    static final int FD_OFFSET_LENGTH = 16;

    static final int FD_OFFSET_DECIMAL_COUNT = 17;

    static final int FD_OFFSET_RESERVED_MULTIUSER_1 = 18;

    static final int FD_OFFSET_WORK_AREA_ID = 20;

    static final int FD_OFFSET_RESERVED_MULTIUSER_2 = 21;

    static final int FD_OFFSET_SET_FIELDS_FLAG = 23;

    static final int FD_OFFSET_RESERVED = 24;

    static final int FD_OFFSET_INDEX_FIELD_FLAG = 31;

    static final int FD_OFFSET_NEXT_FIELD = 32;

    private static final int LENGTH_FIELD_DESCRIPTOR = FD_OFFSET_NEXT_FIELD;

    private static final int LENGTH_FIELD_NAME = FD_OFFSET_TYPE - FD_OFFSET_NAME;

    private static final int LENGTH_FIELD_DATA_ADDRESS = FD_OFFSET_LENGTH - FD_OFFSET_DATA_ADDRESS;

    private static final int LENGTH_FIELD_DESCR_AFTER_DECIMAL_COUNT = FD_OFFSET_NEXT_FIELD - FD_OFFSET_RESERVED_MULTIUSER_1;

    private static final int LENGTH_TABLE_HEADER_AFTER_RECORD_COUNT = OFFSET_FIELD_DESCRIPTORS - OFFSET_RESERVED_1;

    private static final int LENGTH_TABLE_INFO_BLOCK = 32;

    private static final int LENGTH_DELETE_FLAG = 1;

    private static final int OFFSET_WORK_AREA_ID = 20;

    private static final int LENGTH_RESERVED_1 = OFFSET_INCOMPLETE_TRANSATION - OFFSET_RESERVED_1;

    private static final int LENGTH_RESERVED_2 = OFFSET_MDX_FLAG - OFFSET_RESERVED_2;

    private static final int LENGTH_RESERVED_3 = OFFSET_FIELD_DESCRIPTORS - OFFSET_RESERVED_3;

    private static final int MAX_LENGTH_FLOAT_FIELD = 20;

    private static final int MAX_LENGTH_LOGICAL_FIELD = 1;

    private static final int MAX_LENGTH_DATE_FIELD = 8;

    private static final byte FIELD_DESCRIPTOR_ARRAY_TERMINATOR = 0x0D;

    private Version version;

    private int versionByte;

    private int recordCount;

    private List<Field> fields = new ArrayList<Field>();

    private short headerLength;

    private short recordLength;

    private Date lastModifiedDate;

    private boolean hasMemo;

    void readAll(final DataInput dataInput) throws IOException, CorruptedTableException {
        readVersionByte(dataInput);
        readModifiedDate(dataInput);
        readRecordCount(dataInput);
        readHeaderLength(dataInput);
        version = Version.getVersion(versionByte, headerLength % 32);
        readRecordLength(dataInput);
        dataInput.skipBytes(LENGTH_TABLE_HEADER_AFTER_RECORD_COUNT);
        readFieldDescriptors(dataInput, getFieldCount());
    }

    Date getLastModifiedDate() {
        return lastModifiedDate;
    }

    int getLength() {
        return headerLength;
    }

    int getRecordLength() {
        return recordLength;
    }

    void setHasMemo(final boolean hasMemo) {
        this.hasMemo = hasMemo;
    }

    private void calculateRecordLength() {
        for (final Field field : fields) {
            recordLength += field.getLength();
        }
        recordLength += LENGTH_DELETE_FLAG;
    }

    private void calculateHeaderLength() {
        headerLength = (short) (LENGTH_TABLE_INFO_BLOCK + LENGTH_FIELD_DESCRIPTOR * fields.size() + version.getLengthHeaderTerminator());
    }

    private int getFieldCount() throws CorruptedTableException {
        final int nrBytesFieldDescriptorArray = headerLength - LENGTH_TABLE_INFO_BLOCK - version.getLengthHeaderTerminator();
        if ((nrBytesFieldDescriptorArray % LENGTH_FIELD_DESCRIPTOR) != 0) {
            throw new CorruptedTableException("Number of field descriptions in file could not be calculated.");
        }
        return (int) nrBytesFieldDescriptorArray / LENGTH_FIELD_DESCRIPTOR;
    }

    private void readHeaderLength(final DataInput dataInput) throws IOException {
        headerLength = Util.changeEndianness((short) dataInput.readUnsignedShort());
    }

    private void readModifiedDate(final DataInput dataInput) throws IOException {
        int year = dataInput.readByte() + 1900;
        if (year < 1980) {
            year += 100;
        }
        final int month = dataInput.readByte();
        final int day = dataInput.readByte();
        final Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONDAY, month - 1);
        cal.set(Calendar.DAY_OF_MONTH, day);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        lastModifiedDate = cal.getTime();
    }

    private void readRecordLength(final DataInput dataInput) throws IOException {
        recordLength = Util.changeEndianness((short) dataInput.readUnsignedShort());
    }

    void readFieldDescriptors(final DataInput dataInput, final int fieldCount) throws IOException {
        for (int i = 0; i < fieldCount; ++i) {
            fields.add(readField(dataInput));
        }
    }

    private Field readField(final DataInput dataInput) throws IOException {
        int length = 0;
        int decimalCount = 0;
        final String name = Util.readString(dataInput, LENGTH_FIELD_NAME);
        final char typeChar = (char) dataInput.readByte();
        dataInput.skipBytes(LENGTH_FIELD_DATA_ADDRESS);
        if (version == Version.CLIPPER_5 && typeChar == Type.CHARACTER.getCode()) {
            length = Util.changeEndiannessUnsignedShort(dataInput.readUnsignedShort());
        } else {
            length = dataInput.readUnsignedByte();
            decimalCount = dataInput.readUnsignedByte();
        }
        dataInput.skipBytes(LENGTH_FIELD_DESCR_AFTER_DECIMAL_COUNT);
        return new Field(name, Type.getTypeByCode(typeChar), length, decimalCount);
    }

    void readVersionByte(final DataInput dataInput) throws IOException {
        versionByte = dataInput.readUnsignedByte();
    }

    void readRecordCount(final DataInput dataInput) throws IOException {
        recordCount = Util.changeEndianness(dataInput.readInt());
    }

    void writeAll(final DataOutput dataOutput) throws IOException {
        writeVersion(dataOutput);
        writeModifiedDate(dataOutput);
        writeRecordCount(dataOutput);
        writeHeaderLength(dataOutput);
        writeRecordLength(dataOutput);
        writeZeros(dataOutput, LENGTH_RESERVED_1);
        writeIncompleteTransaction(dataOutput);
        writeEncryptionFlag(dataOutput);
        writeFreeRecordThread(dataOutput);
        writeZeros(dataOutput, LENGTH_RESERVED_2);
        writeMdxFlag(dataOutput);
        writeLanguageDriver(dataOutput);
        writeZeros(dataOutput, LENGTH_RESERVED_3);
        writeFieldDescriptors(dataOutput);
    }

    void setFields(final List<Field> fieldList) throws InvalidFieldTypeException, InvalidFieldLengthException {
        fields = fieldList;
        checkFieldValidity(fields);
        calculateRecordLength();
        calculateHeaderLength();
    }

    void checkFieldValidity(final List<Field> fieldList) throws InvalidFieldTypeException, InvalidFieldLengthException {
        boolean invalidLength = false;
        for (final Field field : fieldList) {
            if (!version.fieldTypes.contains(field.getType())) {
                throw new InvalidFieldTypeException("Invalid field type ('" + field.getType().toString() + "') for this version ");
            }
            switch(field.getType()) {
                case CHARACTER:
                    if (field.getLength() < 1 || field.getLength() > version.getMaxLengthCharField()) {
                        invalidLength = true;
                    }
                    break;
                case NUMBER:
                    if (field.getLength() < 1 || field.getLength() > version.getMaxLengthNumberField()) {
                        invalidLength = true;
                    }
                    break;
                case FLOAT:
                    if (field.getLength() < 1 || field.getLength() > MAX_LENGTH_FLOAT_FIELD) {
                        invalidLength = true;
                    }
                    break;
                case LOGICAL:
                    if (field.getLength() < 1 || field.getLength() > MAX_LENGTH_LOGICAL_FIELD) {
                        invalidLength = true;
                    }
                    break;
                case DATE:
                    if (field.getLength() < 1 || field.getLength() > MAX_LENGTH_DATE_FIELD) {
                        invalidLength = true;
                    }
                    break;
            }
            if (invalidLength) {
                if (field.getLength() < 1) {
                    throw new InvalidFieldLengthException("Field length less than one (field '" + field.getName() + "') ");
                } else {
                    throw new InvalidFieldLengthException("Field length exceeds the allowed field length (field '" + field.getName() + "') ");
                }
            }
        }
    }

    void setVersion(final Version version) {
        this.version = version;
    }

    void setRecordCount(final int recordCount) {
        this.recordCount = recordCount;
    }

    List<Field> getFields() {
        return Collections.unmodifiableList(new ArrayList<Field>(fields));
    }

    Version getVersion() {
        return version;
    }

    int getRecordCount() {
        return recordCount;
    }

    void writeEncryptionFlag(final DataOutput dataOutput) throws IOException {
        dataOutput.writeByte(0x00);
    }

    void writeIncompleteTransaction(final DataOutput dataOutput) throws IOException {
        dataOutput.writeByte(0x00);
    }

    void writeFieldDescriptor(final DataOutput dataOutput, final Field field) throws IOException {
        Util.writeString(dataOutput, field.getName(), LENGTH_FIELD_NAME - 1);
        dataOutput.writeByte(0x00);
        dataOutput.writeByte(field.getType().getCode());
        dataOutput.writeInt(0x00);
        dataOutput.writeByte(field.getLength());
        dataOutput.writeByte(field.getDecimalCount());
        dataOutput.writeByte(0x00);
        dataOutput.writeByte(0x00);
        dataOutput.writeByte(0x01);
        for (int i = 0; i < (LENGTH_FIELD_DESCRIPTOR - OFFSET_WORK_AREA_ID - 1); i++) {
            dataOutput.writeByte(0x00);
        }
    }

    void writeFieldDescriptors(final DataOutput dataOutput) throws IOException {
        for (final Field field : fields) {
            writeFieldDescriptor(dataOutput, field);
        }
        dataOutput.writeByte(FIELD_DESCRIPTOR_ARRAY_TERMINATOR);
    }

    void writeFreeRecordThread(final DataOutput dataOutput) throws IOException {
        writeZeros(dataOutput, 4);
    }

    void writeHeaderLength(final DataOutput dataOutput) throws IOException {
        dataOutput.writeShort(Util.changeEndianness(headerLength));
    }

    void writeLanguageDriver(final DataOutput dataOutput) throws IOException {
        writeZeros(dataOutput, 1);
    }

    void writeMdxFlag(final DataOutput dataOutput) throws IOException {
        writeZeros(dataOutput, 1);
    }

    void writeModifiedDate(final DataOutput dataOutput) throws IOException {
        final Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR) - 1900;
        if (year >= 100) {
            year -= 100;
        }
        final int month = cal.get(Calendar.MONTH) + 1;
        final int day = cal.get(Calendar.DAY_OF_MONTH);
        dataOutput.writeByte(year);
        dataOutput.writeByte(month);
        dataOutput.writeByte(day);
        lastModifiedDate = Util.createDate(year, month, day);
    }

    void writeRecordCount(final DataOutput dataOutput) throws IOException {
        dataOutput.writeInt(Util.changeEndianness(recordCount));
    }

    void writeRecordLength(final DataOutput dataOutput) throws IOException {
        dataOutput.writeShort(Util.changeEndianness(recordLength));
    }

    void writeVersion(final DataOutput dataOutput) throws IOException {
        dataOutput.writeByte(Version.getVersionByte(version, hasMemo));
    }

    void writeZeros(final DataOutput dataOutput, final int n) throws IOException {
        for (int i = 0; i < n; ++i) {
            dataOutput.writeByte(0);
        }
    }
}
