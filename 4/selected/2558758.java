package com.hardcode.gdbms.driver.dbf;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.sql.Types;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;
import com.hardcode.gdbms.driver.DriverUtilities;
import com.hardcode.gdbms.driver.exceptions.CloseDriverException;
import com.hardcode.gdbms.driver.exceptions.BadFieldDriverException;
import com.hardcode.gdbms.driver.exceptions.FileNotFoundDriverException;
import com.hardcode.gdbms.driver.exceptions.OpenDriverException;
import com.hardcode.gdbms.driver.exceptions.ReadDriverException;
import com.hardcode.gdbms.driver.exceptions.WriteDriverException;
import com.hardcode.gdbms.engine.data.DataSourceFactory;
import com.hardcode.gdbms.engine.data.driver.FileDriver;
import com.hardcode.gdbms.engine.data.edition.DataWare;
import com.hardcode.gdbms.engine.data.file.FileDataWare;
import com.hardcode.gdbms.engine.values.Value;
import com.hardcode.gdbms.engine.values.ValueFactory;
import com.iver.cit.gvsig.fmap.drivers.dbf.DbaseFile;
import com.iver.cit.gvsig.fmap.drivers.shp.DbaseFileHeaderNIO;
import com.iver.cit.gvsig.fmap.drivers.shp.DbaseFileWriterNIO;

/**
 * DOCUMENT ME!
 *
 * @author Fernando Gonz�lez Cort�s
 */
public class DBFDriver implements FileDriver {

    private static Locale ukLocale = new Locale("en", "UK");

    private DbaseFile dbf = new DbaseFile();

    private char[] fieldTypes;

    private DataSourceFactory dsf;

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
        dbf.open(file);
        try {
            fieldTypes = new char[getFieldCount()];
        } catch (ReadDriverException e) {
            throw new OpenDriverException(getName(), e);
        }
        for (int i = 0; i < fieldTypes.length; i++) {
            fieldTypes[i] = dbf.getFieldType(i);
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
                if (date.length() == 0) {
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
                    throw new BadFieldDriverException(getName(), e, String.valueOf(fieldType));
                }
                return ValueFactory.createValue(dat);
            } catch (UnsupportedEncodingException e1) {
                throw new BadFieldDriverException(getName(), e1, String.valueOf(fieldType));
            }
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
                if (fieldType == Types.INTEGER) {
                    return ValueFactory.createValue(0);
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
     * @throws FileCreateDriverException
     */
    private WritableByteChannel getWriteChannel(String path) throws FileNotFoundDriverException {
        WritableByteChannel channel = null;
        try {
            File f = new File(path);
            if (!f.exists()) {
                System.out.println("Creando fichero " + f.getAbsolutePath());
                if (!f.createNewFile()) {
                    throw new FileNotFoundDriverException(getName(), null, path);
                }
            }
            RandomAccessFile raf = new RandomAccessFile(f, "rw");
            channel = raf.getChannel();
        } catch (IOException e) {
            throw new FileNotFoundDriverException(getName(), e, path);
        }
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
}
