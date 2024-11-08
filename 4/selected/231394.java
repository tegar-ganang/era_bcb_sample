package com.iver.cit.gvsig.fmap.drivers.dbf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.sql.Types;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.prefs.Preferences;
import com.hardcode.gdbms.driver.DriverUtilities;
import com.hardcode.gdbms.driver.exceptions.BadFieldDriverException;
import com.hardcode.gdbms.driver.exceptions.CloseDriverException;
import com.hardcode.gdbms.driver.exceptions.InitializeWriterException;
import com.hardcode.gdbms.driver.exceptions.OpenDriverException;
import com.hardcode.gdbms.driver.exceptions.ReadDriverException;
import com.hardcode.gdbms.driver.exceptions.WriteDriverException;
import com.hardcode.gdbms.engine.data.DataSourceFactory;
import com.hardcode.gdbms.engine.data.driver.FileDriver;
import com.hardcode.gdbms.engine.data.edition.DataWare;
import com.hardcode.gdbms.engine.data.file.FileDataWare;
import com.hardcode.gdbms.engine.values.Value;
import com.hardcode.gdbms.engine.values.ValueFactory;
import com.iver.cit.gvsig.exceptions.visitors.ProcessWriterVisitorException;
import com.iver.cit.gvsig.exceptions.visitors.StartWriterVisitorException;
import com.iver.cit.gvsig.exceptions.visitors.StopWriterVisitorException;
import com.iver.cit.gvsig.fmap.drivers.FieldDescription;
import com.iver.cit.gvsig.fmap.drivers.ITableDefinition;
import com.iver.cit.gvsig.fmap.drivers.TableDefinition;
import com.iver.cit.gvsig.fmap.drivers.shp.DbaseFileHeaderNIO;
import com.iver.cit.gvsig.fmap.drivers.shp.DbaseFileWriterNIO;
import com.iver.cit.gvsig.fmap.edition.IRowEdited;
import com.iver.cit.gvsig.fmap.edition.IWriteable;
import com.iver.cit.gvsig.fmap.edition.IWriter;
import com.iver.cit.gvsig.fmap.edition.fieldmanagers.AbstractFieldManager;
import com.iver.cit.gvsig.fmap.edition.writers.dbf.DbfWriter;
import com.iver.utiles.NumberUtilities;

/**
 * DOCUMENT ME!
 *
 * @author Fernando Gonz�lez Cort�s
 */
public class DBFDriver extends AbstractFieldManager implements FileDriver, IWriteable, IWriter {

    private static Preferences prefs = Preferences.userRoot().node("gvSIG.encoding.dbf");

    private static Locale ukLocale = new Locale("en", "UK");

    private DbaseFile dbf = new DbaseFile();

    private char[] fieldTypes;

    private DataSourceFactory dsf;

    private DbfWriter dbfWriter = new DbfWriter();

    private File file = null;

    private static String tempDirectoryPath = System.getProperty("java.io.tmpdir");

    private File fTemp;

    private ITableDefinition tableDef;

    private Charset charSet = null;

    public Charset getCharSet() {
        return charSet;
    }

    public void setCharSet(Charset charSet) {
        this.charSet = charSet;
    }

    /**
     * @see com.hardcode.driverManager.Driver#getName()
     */
    public String getName() {
        return "gdbms dbf driver";
    }

    /**
     * @see com.hardcode.gdbms.engine.data.GDBMSDriver#open(java.io.File)
     */
    public void open(File file) throws OpenDriverException {
        this.file = file;
        try {
            dbf.open(file);
            setCharSet(dbf.getCharSet());
            fieldTypes = new char[getFieldCount()];
            for (int i = 0; i < fieldTypes.length; i++) {
                fieldTypes[i] = dbf.getFieldType(i);
            }
            int aux = (int) (Math.random() * 1000);
            if (fTemp == null) {
                fTemp = new File(file.getAbsolutePath() + ".tmp.dbf");
                dbfWriter.setFile(fTemp);
            }
            if (charSet != null) dbfWriter.setCharset(charSet);
        } catch (ReadDriverException e) {
            throw new OpenDriverException(getName(), e);
        }
    }

    /**
     * @see com.hardcode.gdbms.engine.data.GDBMSDriver#close()
     */
    public void close() throws CloseDriverException {
        try {
            dbf.close();
        } catch (IOException e) {
            throw new CloseDriverException(getName(), e);
        }
    }

    /**
     * @see com.hardcode.gdbms.engine.data.driver.ReadAccess#getFieldValue(long,
     *      int)
     */
    public Value getFieldValue(long rowIndex, int fieldId) throws ReadDriverException {
        char cfieldType = fieldTypes[fieldId];
        int fieldType = getFieldType(fieldId);
        String strValue;
        if (cfieldType == 'D') {
            String date;
            try {
                date = dbf.getStringFieldValue((int) rowIndex, fieldId).trim();
            } catch (UnsupportedEncodingException e1) {
                throw new ReadDriverException(getName(), e1);
            }
            if (date.length() < 8) {
                return null;
            }
            String year = date.substring(0, 4);
            String month = date.substring(4, 6);
            String day = date.substring(6, 8);
            DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT, ukLocale);
            String strAux = month + "/" + day + "/" + year;
            Date dat;
            try {
                dat = df.parse(strAux);
            } catch (ParseException e) {
                throw new ReadDriverException(getName(), e);
            }
            return ValueFactory.createValue(dat);
        } else {
            try {
                strValue = dbf.getStringFieldValue((int) rowIndex, fieldId);
            } catch (UnsupportedEncodingException e1) {
                throw new BadFieldDriverException(getName(), e1);
            }
            strValue = strValue.trim();
            if (fieldType == Types.BOOLEAN) {
                strValue = strValue.toLowerCase();
                strValue = Boolean.toString(strValue.equals("t") || strValue.equals("y"));
            }
            try {
                return ValueFactory.createValueByType(strValue, fieldType);
            } catch (Exception e) {
                if (fieldType == Types.INTEGER || fieldType == Types.BIGINT || fieldType == Types.FLOAT || fieldType == Types.DECIMAL || fieldType == Types.DOUBLE) {
                    try {
                        return ValueFactory.createValueByType("0", fieldType);
                    } catch (ParseException e1) {
                        throw new BadFieldDriverException(getName(), null, String.valueOf(fieldType));
                    }
                } else {
                    throw new BadFieldDriverException(getName(), null, String.valueOf(fieldType));
                }
            }
        }
    }

    /**
     * @see com.hardcode.gdbms.engine.data.driver.ReadAccess#getFieldCount()
     */
    public int getFieldCount() throws ReadDriverException {
        return dbf.getFieldCount();
    }

    /**
     * @see com.hardcode.gdbms.engine.data.driver.ReadAccess#getFieldName(int)
     */
    public String getFieldName(int fieldId) throws ReadDriverException {
        return dbf.getFieldName(fieldId);
    }

    /**
     * @see com.hardcode.gdbms.engine.data.driver.ReadAccess#getRowCount()
     */
    public long getRowCount() throws ReadDriverException {
        return dbf.getRecordCount();
    }

    /**
     * @see com.hardcode.gdbms.engine.data.driver.FileDriver#fileAccepted(java.io.File)
     */
    public boolean fileAccepted(File f) {
        return f.getAbsolutePath().toUpperCase().endsWith("DBF");
    }

    /**
     * @see com.hardcode.gdbms.engine.data.driver.ObjectDriver#getFieldType(int)
     */
    public int getFieldType(int i) throws ReadDriverException {
        char fieldType = fieldTypes[i];
        if (fieldType == 'L') {
            return Types.BOOLEAN;
        } else if (fieldType == 'F') {
            return Types.DOUBLE;
        } else if (fieldType == 'N') {
            if (dbf.getFieldDecimalLength(i) > 0) {
                return Types.DOUBLE;
            } else {
                return Types.INTEGER;
            }
        } else if (fieldType == 'C') {
            return Types.VARCHAR;
        } else if (fieldType == 'D') {
            return Types.DATE;
        } else {
            throw new BadFieldDriverException(getName(), null, String.valueOf(fieldType));
        }
    }

    /**
     * @see com.hardcode.gdbms.engine.data.driver.DriverCommons#setDataSourceFactory(com.hardcode.gdbms.engine.data.DataSourceFactory)
     */
    public void setDataSourceFactory(DataSourceFactory dsf) {
        this.dsf = dsf;
    }

    private void writeToTemp(DataWare dataWare, File file) throws WriteDriverException, ReadDriverException {
        DbaseFileWriterNIO dbfWrite = null;
        DbaseFileHeaderNIO myHeader;
        Value[] record;
        try {
            myHeader = DbaseFileHeaderNIO.createDbaseHeader(dataWare);
            myHeader.setNumRecords((int) dataWare.getRowCount());
            dbfWrite = new DbaseFileWriterNIO(myHeader, (FileChannel) getWriteChannel(file.getPath()));
            record = new Value[dataWare.getFieldCount()];
            for (int j = 0; j < dataWare.getRowCount(); j++) {
                for (int r = 0; r < dataWare.getFieldCount(); r++) {
                    record[r] = dataWare.getFieldValue(j, r);
                }
                dbfWrite.write(record);
            }
            dbfWrite.close();
        } catch (IOException e) {
            throw new WriteDriverException(getName(), e);
        }
    }

    /**
     * @throws ReadDriverException
     * @see com.hardcode.gdbms.engine.data.driver.FileDriver#writeFile(com.hardcode.gdbms.engine.data.file.FileDataWare,
     *      java.io.File)
     */
    public void writeFile(FileDataWare dataWare) throws WriteDriverException, ReadDriverException {
        String temp = dsf.getTempFile();
        writeToTemp(dataWare, new File(temp));
        try {
            FileChannel fcout = dbf.getWriteChannel();
            FileChannel fcin = new FileInputStream(temp).getChannel();
            DriverUtilities.copy(fcin, fcout);
        } catch (IOException e) {
            throw new WriteDriverException(getName(), e);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param path DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     *
     * @throws IOException DOCUMENT ME!
     */
    private WritableByteChannel getWriteChannel(String path) throws IOException {
        WritableByteChannel channel;
        File f = new File(path);
        if (!f.exists()) {
            System.out.println("Creando fichero " + f.getAbsolutePath());
            if (!f.createNewFile()) {
                throw new IOException("Cannot create file " + f);
            }
        }
        RandomAccessFile raf = new RandomAccessFile(f, "rw");
        channel = raf.getChannel();
        return channel;
    }

    public void createSource(String arg0, String[] arg1, int[] arg2) throws ReadDriverException {
        DbaseFileHeaderNIO myHeader;
        int[] lengths = new int[arg2.length];
        for (int i = 0; i < arg2.length; i++) {
            lengths[i] = 100;
        }
        try {
            myHeader = DbaseFileHeaderNIO.createDbaseHeader(arg1, arg2, lengths);
            myHeader.setNumRecords(0);
            DbaseFileWriterNIO dbfWrite = new DbaseFileWriterNIO(myHeader, (FileChannel) getWriteChannel(arg0));
            dbfWrite = new DbaseFileWriterNIO(myHeader, (FileChannel) getWriteChannel(arg0));
        } catch (IOException e) {
            throw new ReadDriverException(getName(), e);
        }
    }

    public int getFieldWidth(int i) throws ReadDriverException {
        return dbf.getFieldLength(i);
    }

    public IWriter getWriter() {
        return this;
    }

    public void preProcess() throws StartWriterVisitorException {
        dbfWriter.setCharset(getCharSet());
        dbfWriter.preProcess();
    }

    public void process(IRowEdited row) throws ProcessWriterVisitorException {
        dbfWriter.process(row);
    }

    public void postProcess() throws StopWriterVisitorException {
        dbfWriter.postProcess();
        try {
            short originalEncoding = dbf.getDbaseHeader().getLanguageID();
            File dbfFile = fTemp;
            FileChannel fcinDbf = new FileInputStream(dbfFile).getChannel();
            FileChannel fcoutDbf = new FileOutputStream(file).getChannel();
            DriverUtilities.copy(fcinDbf, fcoutDbf);
            fTemp.delete();
            close();
            RandomAccessFile fo = new RandomAccessFile(file, "rw");
            fo.seek(29);
            fo.writeByte(originalEncoding);
            fo.close();
            open(file);
        } catch (FileNotFoundException e) {
            throw new StopWriterVisitorException(getName(), e);
        } catch (IOException e) {
            throw new StopWriterVisitorException(getName(), e);
        } catch (CloseDriverException e) {
            throw new StopWriterVisitorException(getName(), e);
        } catch (OpenDriverException e) {
            throw new StopWriterVisitorException(getName(), e);
        }
    }

    public String getCapability(String capability) {
        return dbfWriter.getCapability(capability);
    }

    public void setCapabilities(Properties capabilities) {
        dbfWriter.setCapabilities(capabilities);
    }

    public boolean canWriteAttribute(int sqlType) {
        return dbfWriter.canWriteAttribute(sqlType);
    }

    public void initialize(ITableDefinition tableDefinition) throws InitializeWriterException {
        dbfWriter.initialize(tableDefinition);
    }

    public ITableDefinition getTableDefinition() throws ReadDriverException {
        tableDef = new TableDefinition();
        int numFields;
        numFields = getFieldCount();
        FieldDescription[] fieldsDescrip = new FieldDescription[numFields];
        for (int i = 0; i < numFields; i++) {
            fieldsDescrip[i] = new FieldDescription();
            int type = getFieldType(i);
            fieldsDescrip[i].setFieldType(type);
            fieldsDescrip[i].setFieldName(getFieldName(i));
            fieldsDescrip[i].setFieldLength(getFieldWidth(i));
            if (NumberUtilities.isNumeric(type)) {
                if (!NumberUtilities.isNumericInteger(type)) fieldsDescrip[i].setFieldDecimalCount(6);
            } else fieldsDescrip[i].setFieldDecimalCount(0);
        }
        tableDef.setFieldsDesc(fieldsDescrip);
        return tableDef;
    }

    public boolean canAlterTable() {
        return true;
    }

    public boolean alterTable() {
        return true;
    }

    public boolean canSaveEdits() {
        if (file.canWrite()) return true;
        return false;
    }

    public boolean isWriteAll() {
        return true;
    }

    public void setFieldValue(int rowIndex, int fieldId, Object obj) throws IOException {
        dbf.setFieldValue(rowIndex, fieldId, obj);
    }
}
