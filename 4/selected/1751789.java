package com.iver.cit.gvsig.fmap.edition.writers.dbf;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.sql.Types;
import java.util.ArrayList;
import com.hardcode.gdbms.driver.exceptions.InitializeWriterException;
import com.iver.cit.gvsig.exceptions.visitors.ProcessWriterVisitorException;
import com.iver.cit.gvsig.exceptions.visitors.StartWriterVisitorException;
import com.iver.cit.gvsig.exceptions.visitors.StopWriterVisitorException;
import com.iver.cit.gvsig.fmap.core.IRow;
import com.iver.cit.gvsig.fmap.drivers.FieldDescription;
import com.iver.cit.gvsig.fmap.drivers.ITableDefinition;
import com.iver.cit.gvsig.fmap.drivers.shp.DbaseFileHeaderNIO;
import com.iver.cit.gvsig.fmap.drivers.shp.DbaseFileWriterNIO;
import com.iver.cit.gvsig.fmap.drivers.shp.SHP;
import com.iver.cit.gvsig.fmap.edition.IRowEdited;
import com.iver.cit.gvsig.fmap.edition.fieldmanagers.AddFieldCommand;
import com.iver.cit.gvsig.fmap.edition.fieldmanagers.FieldCommand;
import com.iver.cit.gvsig.fmap.edition.fieldmanagers.RemoveFieldCommand;
import com.iver.cit.gvsig.fmap.edition.fieldmanagers.RenameFieldCommand;
import com.iver.cit.gvsig.fmap.edition.writers.AbstractWriter;
import com.iver.cit.gvsig.fmap.layers.FBitSet;

public class DbfWriter extends AbstractWriter {

    private String dbfPath = null;

    private DbaseFileWriterNIO dbfWrite;

    private DbaseFileHeaderNIO myHeader;

    private int numRows;

    private Object[] record;

    private ArrayList fieldCommands = new ArrayList();

    private FBitSet selection = null;

    private FieldDescription[] originalFields;

    private Charset charset;

    public DbfWriter() {
        super();
        this.capabilities.setProperty("FieldNameMaxLength", "10");
    }

    public void setFile(File f) {
        String absolutePath = f.getAbsolutePath();
        if (absolutePath.toUpperCase().endsWith("DBF")) {
            dbfPath = absolutePath;
        } else {
            dbfPath = SHP.getDbfFile(f).getAbsolutePath();
        }
    }

    private WritableByteChannel getWriteChannel(String path) throws IOException {
        WritableByteChannel channel;
        File f = new File(path);
        if (!f.exists()) {
            System.out.println("Creando fichero " + f.getAbsolutePath());
            if (!f.createNewFile()) {
                System.err.print("Error al crear el fichero " + f.getAbsolutePath());
                throw new IOException("Cannot create file " + f);
            }
        }
        RandomAccessFile raf = new RandomAccessFile(f, "rw");
        channel = raf.getChannel();
        return channel;
    }

    public void preProcess() throws StartWriterVisitorException {
        alterTable();
        dbfWrite.setCharset(charset);
        if (selection == null) {
            try {
                myHeader.setNumRecords(0);
                dbfWrite = new DbaseFileWriterNIO(myHeader, (FileChannel) getWriteChannel(dbfPath));
                record = new Object[myHeader.getNumFields()];
                numRows = 0;
                dbfWrite.setCharset(charset);
            } catch (IOException e) {
                throw new StartWriterVisitorException(getName(), e);
            }
        }
    }

    public void process(IRowEdited row) throws ProcessWriterVisitorException {
        IRow rowEdit = row.getLinkedRow();
        switch(row.getStatus()) {
            case IRowEdited.STATUS_ADDED:
            case IRowEdited.STATUS_ORIGINAL:
            case IRowEdited.STATUS_MODIFIED:
                try {
                    for (int i = 0; i < record.length; i++) record[i] = rowEdit.getAttribute(i);
                    dbfWrite.write(record);
                    numRows++;
                } catch (IOException e) {
                    throw new ProcessWriterVisitorException(getName(), e);
                }
        }
    }

    public void postProcess() throws StopWriterVisitorException {
        try {
            myHeader.setNumRecords(numRows);
            dbfWrite = new DbaseFileWriterNIO(myHeader, (FileChannel) getWriteChannel(dbfPath));
            dbfWrite.setCharset(charset);
        } catch (IOException e) {
            throw new StopWriterVisitorException(getName(), e);
        }
    }

    public String getName() {
        return "DBF Writer";
    }

    public boolean canWriteAttribute(int sqlType) {
        switch(sqlType) {
            case Types.DOUBLE:
            case Types.FLOAT:
            case Types.INTEGER:
            case Types.BIGINT:
                return true;
            case Types.DATE:
                return true;
            case Types.BIT:
            case Types.BOOLEAN:
                return true;
            case Types.VARCHAR:
            case Types.CHAR:
            case Types.LONGVARCHAR:
                return true;
        }
        return false;
    }

    public void initialize(ITableDefinition tableDefinition) throws InitializeWriterException {
        super.initialize(tableDefinition);
        originalFields = tableDefinition.getFieldsDesc();
        myHeader = DbaseFileHeaderNIO.createDbaseHeader(tableDefinition.getFieldsDesc());
        if (dbfPath == null) {
            throw new InitializeWriterException(getName(), null);
        }
    }

    public FieldDescription[] getOriginalFields() {
        return originalFields;
    }

    public boolean alterTable() throws StartWriterVisitorException {
        FieldDescription[] fieldsDesc = getFields();
        myHeader = DbaseFileHeaderNIO.createDbaseHeader(fieldsDesc);
        try {
            dbfWrite = new DbaseFileWriterNIO(myHeader, (FileChannel) getWriteChannel(dbfPath));
            dbfWrite.setCharset(charset);
        } catch (IOException e) {
            throw new StartWriterVisitorException(getName(), e);
        }
        return true;
    }

    public void addField(FieldDescription fieldDesc) {
        AddFieldCommand c = new AddFieldCommand(fieldDesc);
        fieldCommands.add(c);
    }

    public FieldDescription removeField(String fieldName) {
        RemoveFieldCommand c = new RemoveFieldCommand(fieldName);
        FieldDescription[] act = getFields();
        FieldDescription found = null;
        for (int i = 0; i < act.length; i++) {
            if (act[i].getFieldAlias().compareToIgnoreCase(fieldName) == 0) {
                found = act[i];
                break;
            }
        }
        fieldCommands.add(c);
        return found;
    }

    public void renameField(String antName, String newName) {
        RenameFieldCommand c = new RenameFieldCommand(antName, newName);
        fieldCommands.add(c);
    }

    public FieldDescription[] getFields() {
        ArrayList aux = new ArrayList();
        for (int i = 0; i < getOriginalFields().length; i++) {
            aux.add(getOriginalFields()[i]);
        }
        for (int j = 0; j < fieldCommands.size(); j++) {
            FieldCommand fc = (FieldCommand) fieldCommands.get(j);
            if (fc instanceof AddFieldCommand) {
                AddFieldCommand ac = (AddFieldCommand) fc;
                aux.add(ac.getFieldDesc());
            }
            if (fc instanceof RemoveFieldCommand) {
                RemoveFieldCommand rc = (RemoveFieldCommand) fc;
                for (int k = 0; k < aux.size(); k++) {
                    FieldDescription fAux = (FieldDescription) aux.get(k);
                    if (fAux.getFieldAlias().compareTo(rc.getFieldName()) == 0) {
                        aux.remove(k);
                        break;
                    }
                }
            }
            if (fc instanceof RenameFieldCommand) {
                RenameFieldCommand renc = (RenameFieldCommand) fc;
                for (int k = 0; k < aux.size(); k++) {
                    FieldDescription fAux = (FieldDescription) aux.get(k);
                    if (fAux.getFieldAlias().compareTo(renc.getAntName()) == 0) {
                        fAux.setFieldAlias(renc.getNewName());
                        break;
                    }
                }
            }
        }
        return (FieldDescription[]) aux.toArray(new FieldDescription[0]);
    }

    public boolean canAlterTable() {
        return true;
    }

    public boolean canSaveEdits() {
        File aux = new File(dbfPath);
        if (aux.canWrite()) return true;
        return false;
    }

    public Charset getCharset() {
        return charset;
    }

    public void setCharset(Charset charset) {
        this.charset = charset;
    }
}
