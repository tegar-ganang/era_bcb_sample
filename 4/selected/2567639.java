package com.linuxense.javadbf;

import java.io.*;
import java.util.GregorianCalendar;
import java.util.Vector;

class DBFHeader {

    DBFHeader() {
        signature = 3;
        terminator1 = 13;
    }

    void read(DataInput dataInput) throws IOException {
        signature = dataInput.readByte();
        year = dataInput.readByte();
        month = dataInput.readByte();
        day = dataInput.readByte();
        numberOfRecords = Utils.readLittleEndianInt(dataInput);
        headerLength = Utils.readLittleEndianShort(dataInput);
        recordLength = Utils.readLittleEndianShort(dataInput);
        reserv1 = Utils.readLittleEndianShort(dataInput);
        incompleteTransaction = dataInput.readByte();
        encryptionFlag = dataInput.readByte();
        freeRecordThread = Utils.readLittleEndianInt(dataInput);
        reserv2 = dataInput.readInt();
        reserv3 = dataInput.readInt();
        mdxFlag = dataInput.readByte();
        languageDriver = dataInput.readByte();
        reserv4 = Utils.readLittleEndianShort(dataInput);
        Vector v_fields = new Vector();
        for (DBFField field = DBFField.createField(dataInput); field != null; field = DBFField.createField(dataInput)) v_fields.addElement(field);
        fieldArray = new DBFField[v_fields.size()];
        for (int i = 0; i < fieldArray.length; i++) fieldArray[i] = (DBFField) v_fields.elementAt(i);
    }

    void write(DataOutput dataOutput) throws IOException {
        dataOutput.writeByte(signature);
        GregorianCalendar calendar = new GregorianCalendar();
        year = (byte) (calendar.get(1) - 1900);
        month = (byte) (calendar.get(2) + 1);
        day = (byte) calendar.get(5);
        dataOutput.writeByte(year);
        dataOutput.writeByte(month);
        dataOutput.writeByte(day);
        numberOfRecords = Utils.littleEndian(numberOfRecords);
        dataOutput.writeInt(numberOfRecords);
        headerLength = findHeaderLength();
        dataOutput.writeShort(Utils.littleEndian(headerLength));
        recordLength = findRecordLength();
        dataOutput.writeShort(Utils.littleEndian(recordLength));
        dataOutput.writeShort(Utils.littleEndian(reserv1));
        dataOutput.writeByte(incompleteTransaction);
        dataOutput.writeByte(encryptionFlag);
        dataOutput.writeInt(Utils.littleEndian(freeRecordThread));
        dataOutput.writeInt(Utils.littleEndian(reserv2));
        dataOutput.writeInt(Utils.littleEndian(reserv3));
        dataOutput.writeByte(mdxFlag);
        dataOutput.writeByte(languageDriver);
        dataOutput.writeShort(Utils.littleEndian(reserv4));
        for (int i = 0; i < fieldArray.length; i++) fieldArray[i].write(dataOutput);
        dataOutput.writeByte(terminator1);
    }

    private short findHeaderLength() {
        return (short) (32 + 32 * fieldArray.length + 1);
    }

    private short findRecordLength() {
        int recordLength = 0;
        for (int i = 0; i < fieldArray.length; i++) recordLength += fieldArray[i].getFieldLength();
        return (short) (recordLength + 1);
    }

    static final byte SIG_DBASE_III = 3;

    byte signature;

    byte year;

    byte month;

    byte day;

    int numberOfRecords;

    short headerLength;

    short recordLength;

    short reserv1;

    byte incompleteTransaction;

    byte encryptionFlag;

    int freeRecordThread;

    int reserv2;

    int reserv3;

    byte mdxFlag;

    byte languageDriver;

    short reserv4;

    DBFField fieldArray[];

    byte terminator1;
}
