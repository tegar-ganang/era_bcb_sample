package com.db4o.internal.fileheader;

import com.db4o.*;
import com.db4o.ext.*;
import com.db4o.internal.*;

/**
 * @exclude
 */
public class FileHeader0 extends FileHeader {

    static final int HEADER_LENGTH = 2 + (Const4.INT_LENGTH * 4);

    private ConfigBlock _configBlock;

    private PBootRecord _bootRecord;

    public void close() throws Db4oIOException {
        _configBlock.close();
    }

    protected FileHeader newOnSignatureMatch(LocalObjectContainer file, ByteArrayBuffer reader) {
        byte firstFileByte = reader.readByte();
        if (firstFileByte != Const4.YAPBEGIN) {
            if (firstFileByte != Const4.YAPFILEVERSION) {
                return null;
            }
            file.blockSizeReadFromFile(reader.readByte());
        } else {
            if (reader.readByte() != Const4.YAPFILE) {
                return null;
            }
        }
        return new FileHeader0();
    }

    protected void readFixedPart(LocalObjectContainer file, ByteArrayBuffer reader) throws OldFormatException {
        _configBlock = ConfigBlock.forExistingFile(file, reader.readInt());
        skipConfigurationLockTime(reader);
        readClassCollectionAndFreeSpace(file, reader);
    }

    private void skipConfigurationLockTime(ByteArrayBuffer reader) {
        reader.incrementOffset(Const4.ID_LENGTH);
    }

    public void readVariablePart(LocalObjectContainer file) {
        if (_configBlock._bootRecordID <= 0) {
            return;
        }
        Object bootRecord = Debug.readBootRecord ? getBootRecord(file) : null;
        if (!(bootRecord instanceof PBootRecord)) {
            initBootRecord(file);
            file.generateNewIdentity();
            return;
        }
        _bootRecord = (PBootRecord) bootRecord;
        file.activate(bootRecord, Integer.MAX_VALUE);
        file.setNextTimeStampId(_bootRecord.i_versionGenerator);
        file.systemData().identity(_bootRecord.i_db);
    }

    private Object getBootRecord(LocalObjectContainer file) {
        file.showInternalClasses(true);
        try {
            return file.getByID(file.systemTransaction(), _configBlock._bootRecordID);
        } finally {
            file.showInternalClasses(false);
        }
    }

    public void initNew(LocalObjectContainer file) throws Db4oIOException {
        _configBlock = ConfigBlock.forNewFile(file);
        initBootRecord(file);
    }

    private void initBootRecord(LocalObjectContainer file) {
        file.showInternalClasses(true);
        try {
            _bootRecord = new PBootRecord();
            file.storeInternal(file.systemTransaction(), _bootRecord, false);
            _configBlock._bootRecordID = file.getID(file.systemTransaction(), _bootRecord);
            writeVariablePart(file, 1);
        } finally {
            file.showInternalClasses(false);
        }
    }

    public Transaction interruptedTransaction() {
        return _configBlock.getTransactionToCommit();
    }

    public void writeTransactionPointer(Transaction systemTransaction, int transactionAddress) {
        writeTransactionPointer(systemTransaction, transactionAddress, _configBlock.address(), ConfigBlock.TRANSACTION_OFFSET);
    }

    public MetaIndex getUUIDMetaIndex() {
        return _bootRecord.getUUIDMetaIndex();
    }

    public int length() {
        return HEADER_LENGTH;
    }

    public void writeFixedPart(LocalObjectContainer file, boolean startFileLockingThread, boolean shuttingDown, StatefulBuffer writer, int blockSize_, int freespaceID) {
        writer.writeByte(Const4.YAPFILEVERSION);
        writer.writeByte((byte) blockSize_);
        writer.writeInt(_configBlock.address());
        writer.writeInt((int) timeToWrite(_configBlock.openTime(), shuttingDown));
        writer.writeInt(file.systemData().classCollectionID());
        writer.writeInt(freespaceID);
        if (Debug.xbytes && Deploy.overwrite) {
            writer.setID(Const4.IGNORE_ID);
        }
        writer.write();
        file.syncFiles();
    }

    public void writeVariablePart(LocalObjectContainer file, int part) {
        if (part == 1) {
            _configBlock.write();
        } else if (part == 2) {
            _bootRecord.write(file);
        }
    }
}
