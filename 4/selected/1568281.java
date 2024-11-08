package TdbArchiver.Collector.Tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.sql.Timestamp;
import java.util.Date;
import org.slf4j.Logger;
import TdbArchiver.Collector.DbProxy;
import fr.esrf.Tango.AttrDataFormat;
import fr.esrf.Tango.AttrWriteType;
import fr.esrf.TangoDs.TangoConst;
import fr.esrf.TangoDs.Util;
import fr.soleil.commonarchivingapi.ArchivingTools.Tools.DateHeure;
import fr.soleil.commonarchivingapi.ArchivingTools.Tools.GlobalConst;
import fr.soleil.commonarchivingapi.ArchivingTools.Tools.StringFormater;
import fr.soleil.hdbtdbArchivingApi.ArchivingApi.ConfigConst;
import fr.soleil.hdbtdbArchivingApi.ArchivingTools.Tools.ArchivingException;
import fr.soleil.hdbtdbArchivingApi.ArchivingTools.Tools.DateUtil;
import fr.soleil.hdbtdbArchivingApi.ArchivingTools.Tools.ImageEvent_RO;
import fr.soleil.hdbtdbArchivingApi.ArchivingTools.Tools.ScalarEvent;
import fr.soleil.hdbtdbArchivingApi.ArchivingTools.Tools.SpectrumEvent_RO;
import fr.soleil.hdbtdbArchivingApi.ArchivingTools.Tools.SpectrumEvent_RW;

public class FileTools {

    private final int dataFormat;

    private final int writable;

    private String fileName;

    private final String tableName;

    private FileChannel channel;

    private final String localFilePath;

    private final String remoteFilePath;

    private final Logger logger;

    private final DbProxy dbProxy;

    private Timestamp lastTimestamp;

    private final String attributeName;

    private int writtenAttributes;

    private int attributePerFile;

    private Date creationFileDate;

    private final long exportPeriod;

    public FileTools(final String attributeName, final String tableName, final int dataFormat, final int writable, final long windowsDuration, final Logger logger, final DbProxy dbProxy, final String workingDsPath, final String workingDbPath) throws IOException, ArchivingException {
        this.dataFormat = dataFormat;
        this.writable = writable;
        this.tableName = tableName;
        this.logger = logger;
        this.dbProxy = dbProxy;
        this.attributeName = attributeName;
        exportPeriod = windowsDuration;
        attributePerFile = 0;
        localFilePath = workingDsPath;
        remoteFilePath = workingDbPath;
        writtenAttributes = 0;
        logger.debug("new FileTools for " + attributeName + " at " + localFilePath);
        checkDirs(localFilePath);
        openFile();
    }

    private String buidFileName(final String tableName) {
        final StringBuffer fileName = new StringBuffer();
        creationFileDate = new Date();
        final DateHeure dh = new DateHeure(creationFileDate);
        fileName.append(tableName);
        fileName.append("-");
        fileName.append(dh.toString("yyyyMMdd"));
        fileName.append("-");
        fileName.append(dh.toString("HHmmss"));
        fileName.append(".dat");
        return fileName.toString();
    }

    private static void checkDirs(final String path) {
        final File pathDir = new File(path);
        if (!pathDir.exists()) {
            pathDir.mkdirs();
        }
    }

    private String getLocalFilePath() {
        return localFilePath + File.separator + fileName;
    }

    private synchronized void openFile() throws IOException, ArchivingException {
        try {
            fileName = buidFileName(tableName);
            logger.info("open file " + getLocalFilePath());
            final FileOutputStream stream = new FileOutputStream(new File(getLocalFilePath()));
            channel = stream.getChannel();
            if (dbProxy.getDataBase().getDbConn().getDbType() == ConfigConst.TDB_ORACLE) {
                exportFileToDB(fileName);
            }
        } catch (final IOException e) {
            logger.error("ERROR !! " + "\r\n" + "\t Origin : \t " + "FileTools.initFile" + "\r\n" + "\t Reason : \t " + e.getClass().getName() + "\r\n" + "\t Description : \t " + e.getMessage() + "\r\n" + "\t Additional information : \t " + "" + "\r\n");
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Close the file and launch an export of this file to the database.
     */
    public synchronized void closeFile() throws IOException, ArchivingException {
        final String oldFileName = fileName;
        logger.info("closing file " + getLocalFilePath());
        channel.close();
        logger.info("file closed " + getLocalFilePath());
        if (dbProxy.getDataBase().getDbConn().getDbType() == ConfigConst.TDB_MYSQL) {
            exportFileToDB(oldFileName);
        }
    }

    public synchronized void processEventScalar(final ScalarEvent scalarEvent) throws ArchivingException {
        try {
            String readValue = scalarEvent.valueToString(0);
            String writeValue = scalarEvent.valueToString(1);
            final long timeStampValue = scalarEvent.getTimeStamp();
            if (isValidLine(timeStampValue)) {
                doExport();
                if (dbProxy.getDataBase().getDbConn().getDbType() == ConfigConst.TDB_ORACLE) {
                    if (readValue == null || GlobalConst.ARCHIVER_NULL_VALUE.equalsIgnoreCase(readValue.trim())) {
                        readValue = GlobalConst.ORACLE_NULL_VALUE;
                    }
                    if (writeValue == null || GlobalConst.ARCHIVER_NULL_VALUE.equalsIgnoreCase(writeValue.trim())) {
                        writeValue = GlobalConst.ORACLE_NULL_VALUE;
                    }
                    if (scalarEvent.getData_type() == TangoConst.Tango_DEV_STRING) {
                        readValue = StringFormater.formatStringToWrite(readValue);
                        writeValue = StringFormater.formatStringToWrite(writeValue);
                    }
                    final StringBuffer stringBuffer = new StringBuffer();
                    stringBuffer.append("\"");
                    stringBuffer.append(DateUtil.milliToString(timeStampValue, DateUtil.FR_DATE_PATTERN));
                    stringBuffer.append("\"");
                    stringBuffer.append(",");
                    switch(scalarEvent.getWritable()) {
                        case AttrWriteType._READ:
                            stringBuffer.append("\"").append(readValue).append("\"");
                            break;
                        case AttrWriteType._READ_WRITE:
                        case AttrWriteType._READ_WITH_WRITE:
                            stringBuffer.append("\"").append(readValue).append("\"");
                            stringBuffer.append(",");
                            stringBuffer.append("\"").append(writeValue).append("\"");
                            break;
                        case AttrWriteType._WRITE:
                            stringBuffer.append("\"").append(writeValue).append("\"");
                            break;
                    }
                    write(stringBuffer.toString());
                    write(ConfigConst.NEW_LINE);
                } else if (dbProxy.getDataBase().getDbConn().getDbType() == ConfigConst.TDB_MYSQL) {
                    if (readValue == null || GlobalConst.ARCHIVER_NULL_VALUE.equalsIgnoreCase(readValue.trim())) {
                        readValue = GlobalConst.MYSQL_NULL_VALUE;
                    }
                    if (writeValue == null || GlobalConst.ARCHIVER_NULL_VALUE.equalsIgnoreCase(writeValue.trim())) {
                        writeValue = GlobalConst.MYSQL_NULL_VALUE;
                    }
                    if (scalarEvent.getData_type() == TangoConst.Tango_DEV_STRING) {
                        readValue = StringFormater.formatStringToWrite(readValue);
                        writeValue = StringFormater.formatStringToWrite(writeValue);
                    }
                    switch(scalarEvent.getWritable()) {
                        case AttrWriteType._READ:
                            write(new StringBuffer().append(toDbTimeStringMySQL(scalarEvent.getTimeStamp())).append(ConfigConst.FIELDS_LIMIT).append(readValue).append(ConfigConst.LINES_LIMIT).toString());
                            break;
                        case AttrWriteType._READ_WITH_WRITE:
                            write(new StringBuffer().append(toDbTimeStringMySQL(scalarEvent.getTimeStamp())).append(ConfigConst.FIELDS_LIMIT).append(readValue).append(ConfigConst.FIELDS_LIMIT).append(writeValue).append(ConfigConst.LINES_LIMIT).toString());
                            break;
                        case AttrWriteType._WRITE:
                            write(new StringBuffer().append(toDbTimeStringMySQL(scalarEvent.getTimeStamp())).append(ConfigConst.FIELDS_LIMIT).append(writeValue).append(ConfigConst.LINES_LIMIT).toString());
                            break;
                        case AttrWriteType._READ_WRITE:
                            write(new StringBuffer().append(toDbTimeStringMySQL(scalarEvent.getTimeStamp())).append(ConfigConst.FIELDS_LIMIT).append(readValue).append(ConfigConst.FIELDS_LIMIT).append(writeValue).append(ConfigConst.LINES_LIMIT).toString());
                            break;
                    }
                }
            } else {
                logger.debug("This timestamps has already been inserted : " + new Timestamp(timeStampValue) + " in the file " + fileName + "for " + scalarEvent.getAttribute_complete_name());
            }
        } catch (final IOException e) {
            e.printStackTrace();
            logger.error("IOException for " + scalarEvent.getAttribute_complete_name());
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Unknow Exception for " + scalarEvent.getAttribute_complete_name());
        }
    }

    private void doExport() throws IOException, ArchivingException {
        final long currentDate = System.currentTimeMillis();
        final long elapseTime = currentDate - creationFileDate.getTime();
        if (attributePerFile <= 0 && elapseTime >= exportPeriod) {
            logger.debug("export because of elapseTime " + elapseTime);
            switchFile();
        } else if (attributePerFile > 0) {
            writtenAttributes++;
            if (writtenAttributes >= attributePerFile || elapseTime >= exportPeriod) {
                logger.debug("export due to writtenAttributes " + writtenAttributes + " - elapseTime " + elapseTime);
                switchFile();
                writtenAttributes = 0;
            }
        }
    }

    private synchronized void write(final String line) throws IOException {
        try {
            final ByteBuffer buff = ByteBuffer.wrap(line.getBytes());
            channel.write(buff);
        } catch (final IOException e) {
            e.printStackTrace();
            final String msg = "FileToolsWithNio/write/problem writing for attribute/" + tableName;
            logger.error(msg, e);
            throw e;
        }
    }

    public synchronized void processEventSpectrum(final SpectrumEvent_RO spectrumEvent_ro) throws ArchivingException {
        try {
            final long timeStampValue = spectrumEvent_ro.getTimeStamp();
            if (isValidLine(timeStampValue)) {
                doExport();
                if (spectrumEvent_ro.getData_type() == TangoConst.Tango_DEV_STRING) {
                    final String[] value = (String[]) spectrumEvent_ro.getValue();
                    String[] transformedValue = null;
                    if (value != null) {
                        transformedValue = new String[value.length];
                        for (int i = 0; i < value.length; i++) {
                            transformedValue[i] = StringFormater.formatStringToWrite(value[i]);
                        }
                        spectrumEvent_ro.setValue(transformedValue);
                    }
                }
                String value = spectrumEvent_ro.getValue_AsString();
                if (dbProxy.getDataBase().getDbConn().getDbType() == ConfigConst.TDB_ORACLE) {
                    if (GlobalConst.ARCHIVER_NULL_VALUE.equals(value)) {
                        value = GlobalConst.ORACLE_NULL_VALUE;
                    }
                    write(new StringBuffer().append("\"").append(DateUtil.milliToString(spectrumEvent_ro.getTimeStamp(), DateUtil.FR_DATE_PATTERN)).append("\"").append(",").append("\"").append(Double.toString(spectrumEvent_ro.getDim_x())).append("\"").append(",").append("\"").append(value).append("\"").toString());
                    write(ConfigConst.NEW_LINE);
                } else if (dbProxy.getDataBase().getDbConn().getDbType() == ConfigConst.TDB_MYSQL) {
                    if (GlobalConst.ARCHIVER_NULL_VALUE.equals(value)) {
                        value = GlobalConst.MYSQL_NULL_VALUE;
                    }
                    final StringBuffer buff = new StringBuffer();
                    buff.append(toDbTimeStringMySQL(spectrumEvent_ro.getTimeStamp()));
                    buff.append(ConfigConst.FIELDS_LIMIT);
                    buff.append((double) spectrumEvent_ro.getDim_x());
                    buff.append(ConfigConst.FIELDS_LIMIT);
                    buff.append(value);
                    buff.append(ConfigConst.LINES_LIMIT);
                    write(buff.toString());
                }
            } else {
                logger.info("This timestamps has already been inserted : " + new Timestamp(timeStampValue) + " in the file " + fileName);
            }
        } catch (final IOException e) {
            Util.out2.println("ERROR !! " + "\r\n" + "\t Origin : \t " + "FileTools.processEventSpectrum" + "\r\n" + "\t Reason : \t " + e.getClass().getName() + "\r\n" + "\t Description : \t " + e.getMessage() + "\r\n" + "\t Additional information : \t " + "File :\t " + fileName + "\r\n");
        }
    }

    /**
     * @param spectrumEvent_rw
     * @throws ArchivingException
     */
    public synchronized void processEventSpectrum(final SpectrumEvent_RW spectrumEvent_rw) throws ArchivingException {
        try {
            final long timeStampValue = spectrumEvent_rw.getTimeStamp();
            if (isValidLine(timeStampValue)) {
                if (spectrumEvent_rw.getData_type() == TangoConst.Tango_DEV_STRING) {
                    final String[] value = (String[]) spectrumEvent_rw.getValue();
                    String[] transformedValue = null;
                    if (value != null) {
                        transformedValue = new String[value.length];
                        for (int i = 0; i < value.length; i++) {
                            transformedValue[i] = StringFormater.formatStringToWrite(value[i]);
                        }
                        spectrumEvent_rw.setValue(transformedValue);
                    }
                }
                String readValue = spectrumEvent_rw.getSpectrumValueRW_AsString_Read();
                String writeValue = spectrumEvent_rw.getSpectrumValueRW_AsString_Write();
                if (dbProxy.getDataBase().getDbConn().getDbType() == ConfigConst.TDB_ORACLE) {
                    if (GlobalConst.ARCHIVER_NULL_VALUE.equals(readValue)) {
                        readValue = GlobalConst.ORACLE_NULL_VALUE;
                    }
                    if (GlobalConst.ARCHIVER_NULL_VALUE.equals(writeValue)) {
                        writeValue = GlobalConst.ORACLE_NULL_VALUE;
                    }
                    final StringBuffer buff = new StringBuffer();
                    buff.append("\"");
                    buff.append(DateUtil.milliToString(spectrumEvent_rw.getTimeStamp(), DateUtil.FR_DATE_PATTERN));
                    buff.append("\"");
                    buff.append(",");
                    buff.append("\"");
                    buff.append(Double.toString(spectrumEvent_rw.getDim_x()));
                    buff.append("\"");
                    buff.append(",");
                    buff.append("\"");
                    buff.append(readValue);
                    buff.append("\"");
                    buff.append(",");
                    buff.append("\"");
                    buff.append(writeValue);
                    buff.append("\"");
                    final String content = buff.toString();
                    write(content);
                    write(ConfigConst.NEW_LINE);
                } else if (dbProxy.getDataBase().getDbConn().getDbType() == ConfigConst.TDB_MYSQL) {
                    if (GlobalConst.ARCHIVER_NULL_VALUE.equals(readValue)) {
                        readValue = GlobalConst.MYSQL_NULL_VALUE;
                    }
                    if (GlobalConst.ARCHIVER_NULL_VALUE.equals(writeValue)) {
                        writeValue = GlobalConst.MYSQL_NULL_VALUE;
                    }
                    final StringBuffer buff = new StringBuffer();
                    buff.append(toDbTimeStringMySQL(spectrumEvent_rw.getTimeStamp()));
                    buff.append(ConfigConst.FIELDS_LIMIT);
                    buff.append((double) spectrumEvent_rw.getDim_x());
                    buff.append(ConfigConst.FIELDS_LIMIT);
                    buff.append(readValue);
                    buff.append(ConfigConst.FIELDS_LIMIT);
                    buff.append(writeValue);
                    buff.append(ConfigConst.LINES_LIMIT).toString();
                    final String content = buff.toString();
                    write(content);
                }
                doExport();
            } else {
                logger.info("This timestamps has already been inserted : " + new Timestamp(timeStampValue) + " in the file " + fileName);
            }
        } catch (final IOException e) {
            logger.error("ERROR !! " + "\r\n" + "\t Origin : \t " + "FileTools.processEventSpectrum" + "\r\n" + "\t Reason : \t " + e.getClass().getName() + "\r\n" + "\t Description : \t " + e.getMessage() + "\r\n" + "\t Additional information : \t " + "File :\t " + fileName + "\r\n");
        }
    }

    public synchronized void processEventImage(final ImageEvent_RO imageEvent_ro) throws ArchivingException {
        try {
            doExport();
            if (imageEvent_ro.getData_type() == TangoConst.Tango_DEV_STRING) {
                final String[] value = (String[]) imageEvent_ro.getValue();
                String[] transformedValue = null;
                if (value != null) {
                    transformedValue = new String[value.length];
                    for (int i = 0; i < value.length; i++) {
                        transformedValue[i] = StringFormater.formatStringToWrite(value[i]);
                    }
                    imageEvent_ro.setValue(transformedValue);
                }
            }
            if (dbProxy.getDataBase().getDbConn().getDbType() == ConfigConst.TDB_ORACLE) {
                write(new StringBuffer().append("\"").append(DateUtil.milliToString(imageEvent_ro.getTimeStamp(), DateUtil.FR_DATE_PATTERN)).append("\"").append(",").append("\"").append(Double.toString(imageEvent_ro.getDim_x())).append("\"").append(",").append("\"").append(Double.toString(imageEvent_ro.getDim_y())).append("\"").append(",").append("\"").append(imageEvent_ro.getValue_AsString()).append("\"").toString());
                write(ConfigConst.NEW_LINE);
            } else if (dbProxy.getDataBase().getDbConn().getDbType() == ConfigConst.TDB_MYSQL) {
                final long timeStampValue = imageEvent_ro.getTimeStamp();
                if (isValidLine(timeStampValue)) {
                    write(new StringBuffer().append("\"").append(DateUtil.milliToString(imageEvent_ro.getTimeStamp(), DateUtil.FR_DATE_PATTERN)).append("\"").append(",").append("\"").append(Double.toString(imageEvent_ro.getDim_x())).append("\"").append(",").append("\"").append(Double.toString(imageEvent_ro.getDim_y())).append("\"").append(",").append("\"").append(imageEvent_ro.getValue_AsString()).append("\"").toString());
                    write(ConfigConst.NEW_LINE);
                } else {
                    logger.info("This timestamps has already been inserted : " + new Timestamp(timeStampValue) + " in the file " + fileName);
                }
            }
        } catch (final IOException e) {
            logger.error("ERROR !! " + "\r\n" + "\t Origin : \t " + "FileTools.processEventImage" + "\r\n" + "\t Reason : \t " + e.getClass().getName() + "\r\n" + "\t Description : \t " + e.getMessage() + "\r\n" + "\t Additional information : \t " + "File :\t " + fileName + "\r\n");
        }
    }

    private String toDbTimeStringMySQL(final long time) {
        return new Timestamp(time).toString();
    }

    /**
     * Switch file in an atomic action
     * 
     * @throws IOException
     * @throws ArchivingException
     */
    public synchronized String switchFile() throws IOException, ArchivingException {
        logger.info("#######Exporting file " + fileName + " - attribute " + attributeName + "-  period " + exportPeriod + " - attrPerFile " + attributePerFile);
        closeFile();
        openFile();
        return tableName;
    }

    /**
     * Close the file and launch an export of this file to the database.
     */
    private void exportFileToDB(final String fileName) {
        try {
            logger.debug("start exporting " + remoteFilePath + "/" + fileName + " - _tableName:" + tableName + " - attribute " + attributeName);
            switch(dataFormat) {
                case AttrDataFormat._SCALAR:
                    dbProxy.exportToDB_Scalar(remoteFilePath, fileName, tableName, writable);
                    break;
                case AttrDataFormat._SPECTRUM:
                    dbProxy.exportToDB_Spectrum(remoteFilePath, fileName, tableName, writable);
                    break;
                case AttrDataFormat._IMAGE:
                    dbProxy.exportToDB_Image(remoteFilePath, fileName, tableName, writable);
                    break;
                default:
                    Util.out2.println("Export : " + "DataFormat (" + dataFormat + ") not supported !! ");
                    break;
            }
            final String message = "Export out OK -  of " + remoteFilePath + "/" + fileName + " - _tableName:" + tableName + " - attribute " + attributeName;
            logger.debug(message);
        } catch (final ArchivingException e) {
            e.printStackTrace();
            final String message = "Problem (ArchivingException) exporting file: _remoteFilePath|" + remoteFilePath + "|_exportFileName|" + fileName + "|_tableName|" + tableName;
            logger.error(message, e);
        }
    }

    private boolean isValidLine(final long currentTimestamp) {
        boolean res = false;
        final Timestamp ts = new Timestamp(currentTimestamp);
        if (ts != null && (lastTimestamp == null || ts.after(lastTimestamp))) {
            res = true;
            lastTimestamp = ts;
        }
        return res;
    }

    public int getAttributePerFile() {
        return attributePerFile;
    }

    public void setAttributePerFile(final int attributePerFile) {
        this.attributePerFile = attributePerFile;
    }
}
