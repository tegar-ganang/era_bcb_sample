package TdbArchiver.Collector.Tools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import TdbArchiver.Collector.DbProxy;
import fr.esrf.Tango.AttrDataFormat;
import fr.esrf.Tango.AttrWriteType;
import fr.esrf.TangoDs.TangoConst;
import fr.esrf.TangoDs.Util;
import fr.soleil.commonarchivingapi.ArchivingTools.Diary.ILogger;
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

public class FileToolsWithoutNio {

    private int _dataFormat;

    private int _writable;

    private String _fileName;

    private String _tableName;

    private BufferedWriter _bufferedWriter;

    private long _windowsDuration;

    private WindowThread _myWindowThread;

    protected static ExecutorService executorService = Executors.newCachedThreadPool();

    private static String _localFilePath;

    private static String _remoteFilePath;

    private ILogger _logger;

    private DbProxy _dbProxy;

    public FileToolsWithoutNio(String tableName, int dataFormat, int writable, long windowsDuration, ILogger logger, boolean doStart, DbProxy dbProxy, String workingDsPath, String workingDbPath) {
        _dataFormat = dataFormat;
        _writable = writable;
        _fileName = buid_fileName(tableName);
        _tableName = tableName;
        _windowsDuration = windowsDuration;
        _logger = logger;
        _dbProxy = dbProxy;
        _localFilePath = workingDsPath;
        _remoteFilePath = workingDbPath;
        if (doStart) {
            _myWindowThread = new WindowThread();
            executorService.submit(_myWindowThread);
        }
    }

    private synchronized String get_fileName() {
        return _fileName;
    }

    private synchronized void set_fileName(String fileName) {
        _fileName = fileName;
    }

    private String buid_fileName(String tableName) {
        StringBuffer fileName = new StringBuffer();
        DateHeure dh = new DateHeure();
        String date = dh.toString("yyyyMMdd");
        String time = dh.toString("HHmmss");
        fileName.append(tableName);
        fileName.append("-");
        fileName.append(date);
        fileName.append("-");
        fileName.append(time);
        fileName.append(".dat");
        return fileName.toString();
    }

    public synchronized void initialize() {
        checkDirs(_localFilePath);
        initFile();
    }

    private static void checkDirs(String path) {
        File pathDir = new File(path);
        if (!pathDir.exists()) pathDir.mkdirs();
    }

    private String getLocalFilePath() {
        return getLocalFilePath(get_fileName());
    }

    private String getLocalFilePath(String fileName) {
        return _localFilePath + File.separator + fileName;
    }

    private synchronized void initFile() {
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(getLocalFilePath());
        } catch (IOException e) {
            Util.out2.println("ERROR !! " + "\r\n" + "\t Origin : \t " + "FileTools.initFile" + "\r\n" + "\t Reason : \t " + e.getClass().getName() + "\r\n" + "\t Description : \t " + e.getMessage() + "\r\n" + "\t Additional information : \t " + "" + "\r\n");
            e.printStackTrace();
        }
        _bufferedWriter = new BufferedWriter(fileWriter);
    }

    private synchronized void flushBuffer() throws IOException {
        _bufferedWriter.close();
    }

    public synchronized void processEventScalar(ScalarEvent scalarEvent) throws ArchivingException {
        try {
            String readValue = scalarEvent.valueToString(0);
            String writeValue = scalarEvent.valueToString(1);
            if (_dbProxy.getDataBase().getDbConn().getDbType() == ConfigConst.TDB_ORACLE) {
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
                StringBuffer stringBuffer = new StringBuffer();
                stringBuffer.append("\"");
                stringBuffer.append(DateUtil.milliToString(scalarEvent.getTimeStamp(), DateUtil.FR_DATE_PATTERN));
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
            } else if (_dbProxy.getDataBase().getDbConn().getDbType() == ConfigConst.TDB_MYSQL) {
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
                        write((new StringBuffer().append(toDbTimeStringMySQL(scalarEvent.getTimeStamp())).append(ConfigConst.FIELDS_LIMIT).append(readValue).append(ConfigConst.FIELDS_LIMIT).append(writeValue).append(ConfigConst.LINES_LIMIT)).toString());
                        break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            Util.out2.println("ERROR !! " + "\r\n" + "\t Origin : \t " + "FileTools.processEventScalar" + "\r\n" + "\t Reason : \t " + e.getClass().getName() + "\r\n" + "\t Description : \t " + e.getMessage() + "\r\n" + "\t Additional information : \t " + "File :\t " + _fileName + "\r\n");
            if (e.getMessage().indexOf("Stream closed") != -1) {
                openFileSimply();
            }
        }
    }

    private synchronized void write(String line) throws IOException {
        try {
            _bufferedWriter.write(line);
        } catch (IOException e) {
            String msg = "FileTools Without Nio/write/problem writing for attribute/" + this._tableName;
            this._logger.trace(ILogger.LEVEL_ERROR, msg);
            this._logger.trace(ILogger.LEVEL_ERROR, e);
            throw e;
        }
    }

    public synchronized void processEventSpectrum(SpectrumEvent_RO spectrumEvent_ro) throws ArchivingException {
        try {
            if (spectrumEvent_ro.getData_type() == TangoConst.Tango_DEV_STRING) {
                String[] value = (String[]) spectrumEvent_ro.getValue();
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
            if (_dbProxy.getDataBase().getDbConn().getDbType() == ConfigConst.TDB_ORACLE) {
                if (GlobalConst.ARCHIVER_NULL_VALUE.equals(value)) {
                    value = GlobalConst.ORACLE_NULL_VALUE;
                }
                write(new StringBuffer().append("\"").append(DateUtil.milliToString(spectrumEvent_ro.getTimeStamp(), DateUtil.FR_DATE_PATTERN)).append("\"").append(",").append("\"").append(spectrumEvent_ro.getDim_x()).append("\"").append(",").append("\"").append(value).append("\"").toString());
                write(ConfigConst.NEW_LINE);
            } else if (_dbProxy.getDataBase().getDbConn().getDbType() == ConfigConst.TDB_MYSQL) {
                if (GlobalConst.ARCHIVER_NULL_VALUE.equals(value)) {
                    value = GlobalConst.MYSQL_NULL_VALUE;
                }
                StringBuffer buff = new StringBuffer();
                buff.append(toDbTimeStringMySQL(spectrumEvent_ro.getTimeStamp()));
                buff.append(ConfigConst.FIELDS_LIMIT);
                buff.append(spectrumEvent_ro.getDim_x());
                buff.append(ConfigConst.FIELDS_LIMIT);
                buff.append(value);
                buff.append(ConfigConst.LINES_LIMIT);
                write(buff.toString());
            }
        } catch (IOException e) {
            Util.out2.println("ERROR !! " + "\r\n" + "\t Origin : \t " + "FileTools.processEventSpectrum" + "\r\n" + "\t Reason : \t " + e.getClass().getName() + "\r\n" + "\t Description : \t " + e.getMessage() + "\r\n" + "\t Additional information : \t " + "File :\t " + _fileName + "\r\n");
            if (e.getMessage().indexOf("Stream closed") != -1) {
                openFileSimply();
            }
        }
    }

    /**
	 * @param spectrumEvent_rw
	 * @throws ArchivingException
	 */
    public synchronized void processEventSpectrum(SpectrumEvent_RW spectrumEvent_rw) throws ArchivingException {
        try {
            if (spectrumEvent_rw.getData_type() == TangoConst.Tango_DEV_STRING) {
                String[] value = (String[]) spectrumEvent_rw.getValue();
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
            if (_dbProxy.getDataBase().getDbConn().getDbType() == ConfigConst.TDB_ORACLE) {
                if (GlobalConst.ARCHIVER_NULL_VALUE.equals(readValue)) {
                    readValue = GlobalConst.ORACLE_NULL_VALUE;
                }
                if (GlobalConst.ARCHIVER_NULL_VALUE.equals(writeValue)) {
                    writeValue = GlobalConst.ORACLE_NULL_VALUE;
                }
                StringBuffer buff = new StringBuffer();
                buff.append("\"");
                buff.append(DateUtil.milliToString(spectrumEvent_rw.getTimeStamp(), DateUtil.FR_DATE_PATTERN));
                buff.append("\"");
                buff.append(",");
                buff.append("\"");
                buff.append(spectrumEvent_rw.getDim_x());
                buff.append("\"");
                buff.append(",");
                buff.append("\"");
                buff.append(readValue);
                buff.append("\"");
                buff.append(",");
                buff.append("\"");
                buff.append(writeValue);
                buff.append("\"");
                String content = buff.toString();
                write(content);
                write(ConfigConst.NEW_LINE);
            } else if (_dbProxy.getDataBase().getDbConn().getDbType() == ConfigConst.TDB_ORACLE) {
                if (GlobalConst.ARCHIVER_NULL_VALUE.equals(readValue)) {
                    readValue = GlobalConst.MYSQL_NULL_VALUE;
                }
                if (GlobalConst.ARCHIVER_NULL_VALUE.equals(writeValue)) {
                    writeValue = GlobalConst.MYSQL_NULL_VALUE;
                }
                StringBuffer buff = new StringBuffer();
                buff.append(toDbTimeStringMySQL(spectrumEvent_rw.getTimeStamp()));
                buff.append(ConfigConst.FIELDS_LIMIT);
                buff.append(spectrumEvent_rw.getDim_x());
                buff.append(ConfigConst.FIELDS_LIMIT);
                buff.append(readValue);
                buff.append(ConfigConst.FIELDS_LIMIT);
                buff.append(writeValue);
                buff.append(ConfigConst.LINES_LIMIT).toString();
                String content = buff.toString();
                write(content);
            }
        } catch (IOException e) {
            Util.out2.println("ERROR !! " + "\r\n" + "\t Origin : \t " + "FileTools.processEventSpectrum" + "\r\n" + "\t Reason : \t " + e.getClass().getName() + "\r\n" + "\t Description : \t " + e.getMessage() + "\r\n" + "\t Additional information : \t " + "File :\t " + _fileName + "\r\n");
            if (e.getMessage().indexOf("Stream closed") != -1) {
                openFileSimply();
            }
        }
    }

    public synchronized void processEventImage(ImageEvent_RO imageEvent_ro) throws ArchivingException {
        try {
            if (imageEvent_ro.getData_type() == TangoConst.Tango_DEV_STRING) {
                String[] value = (String[]) imageEvent_ro.getValue();
                String[] transformedValue = null;
                if (value != null) {
                    transformedValue = new String[value.length];
                    for (int i = 0; i < value.length; i++) {
                        transformedValue[i] = StringFormater.formatStringToWrite(value[i]);
                    }
                    imageEvent_ro.setValue(transformedValue);
                }
            }
            if (_dbProxy.getDataBase().getDbConn().getDbType() == ConfigConst.TDB_ORACLE) {
                write(new StringBuffer().append("\"").append(DateUtil.milliToString(imageEvent_ro.getTimeStamp(), DateUtil.FR_DATE_PATTERN)).append("\"").append(",").append("\"").append(imageEvent_ro.getDim_x()).append("\"").append(",").append("\"").append(imageEvent_ro.getDim_y()).append("\"").append(",").append("\"").append(imageEvent_ro.getValue_AsString()).append("\"").toString());
                write(ConfigConst.NEW_LINE);
            } else if (_dbProxy.getDataBase().getDbConn().getDbType() == ConfigConst.TDB_MYSQL) {
                write(new StringBuffer().append(toDbTimeStringMySQL(imageEvent_ro.getTimeStamp())).append(ConfigConst.FIELDS_LIMIT).append(imageEvent_ro.getDim_x()).append(ConfigConst.FIELDS_LIMIT).append(imageEvent_ro.getDim_y()).append(ConfigConst.FIELDS_LIMIT).append(imageEvent_ro.getValue_AsString()).append(ConfigConst.LINES_LIMIT).toString());
            }
        } catch (IOException e) {
            Util.out2.println("ERROR !! " + "\r\n" + "\t Origin : \t " + "FileTools.processEventImage" + "\r\n" + "\t Reason : \t " + e.getClass().getName() + "\r\n" + "\t Description : \t " + e.getMessage() + "\r\n" + "\t Additional information : \t " + "File :\t " + _fileName + "\r\n");
            if (e.getMessage().indexOf("Stream closed") != -1) {
                openFileSimply();
            }
        }
    }

    private String toDbTimeStringMySQL(long time) {
        return (new Timestamp(time)).toString();
    }

    public synchronized String swapFile(boolean isAsynchronous) {
        System.out.println("FileTools/swapFile/START");
        System.out.println("FileTools.swapFile");
        try {
            String oldFile;
            flushBuffer();
            oldFile = get_fileName();
            set_fileName(buid_fileName(_tableName));
            initFile();
            (new ExportTask(oldFile, isAsynchronous)).execute();
        } catch (Throwable t) {
            Util.out2.println("ERROR !! " + "\r\n" + "\t Origin : \t " + "FileTools.swapFile" + "\r\n" + "\t Reason : \t " + t.getClass().getName() + "\r\n" + "\t Description : \t " + t.getMessage() + "\r\n" + "\t Additional information : \t " + "" + "\r\n");
            t.printStackTrace();
            String message = "Problem (IOException) in swapFile file: get_fileName()|" + get_fileName() + "|isAsynchronous|" + isAsynchronous;
            _logger.trace(ILogger.LEVEL_CRITIC, message);
            _logger.trace(ILogger.LEVEL_CRITIC, t);
        }
        return _tableName;
    }

    /**
	 * Close the file and launch an export of this file to the database.
	 */
    public synchronized void closeFile(boolean isAsynchronous) {
        System.out.println("FileTools.closeFile");
        try {
            this.flushBuffer();
            (new ExportTask(get_fileName(), isAsynchronous)).execute();
            _myWindowThread.destroy();
        } catch (Throwable t) {
            Util.out2.println("ERROR !! " + "\r\n" + "\t Origin : \t " + "FileTools.closeFile" + "\r\n" + "\t Reason : \t " + t.getClass().getName() + "\r\n" + "\t Description : \t " + t.getMessage() + "\r\n" + "\t Additional information : \t " + "" + "\r\n");
            t.printStackTrace();
            String message = "Problem (IOException) in closeFile file: get_fileName()|" + get_fileName() + "|isAsynchronous|" + isAsynchronous;
            _logger.trace(ILogger.LEVEL_CRITIC, message);
            _logger.trace(ILogger.LEVEL_CRITIC, t);
        }
    }

    public synchronized void closeFileSimply() {
        try {
            this.flushBuffer();
        } catch (Throwable t) {
            Util.out2.println("ERROR !! " + "\r\n" + "\t Origin : \t " + "FileTools.closeFileSimply" + "\r\n" + "\t Reason : \t " + t.getClass().getName() + "\r\n" + "\t Description : \t " + t.getMessage() + "\r\n" + "\t Additional information : \t " + "" + "\r\n");
            t.printStackTrace();
            String message = "Problem (IOException) in closeFileSimply file: get_fileName()|" + get_fileName();
            _logger.trace(ILogger.LEVEL_CRITIC, message);
            _logger.trace(ILogger.LEVEL_CRITIC, t);
        }
    }

    public synchronized void openFileSimply() {
        set_fileName(buid_fileName(_tableName));
        initFile();
    }

    public class ExportTask {

        String _exportFileName;

        boolean _isAsynchronous;

        public ExportTask(String fileName, boolean isAsynchronous) {
            _exportFileName = fileName;
            _isAsynchronous = isAsynchronous;
        }

        public synchronized void execute() {
            ExportThreadRunnable runnable = new ExportThreadRunnable(_exportFileName);
            Future future = executorService.submit(runnable);
            if (!_isAsynchronous) {
                try {
                    future.get();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class ExportThreadRunnable implements Runnable {

        String _exportFileName;

        public ExportThreadRunnable(String fileName) {
            _exportFileName = fileName;
        }

        public synchronized void run() {
            try {
                System.out.println("ExportThreadRunnable.run");
                switch(_dataFormat) {
                    case AttrDataFormat._SCALAR:
                        System.out.println("ExportThreadRunnable/_remoteFilePath|" + _remoteFilePath + "|_exportFileName|" + _exportFileName + "|_tableName|" + _tableName + "|_writable|" + _writable);
                        _dbProxy.exportToDB_Scalar(_remoteFilePath, _exportFileName, _tableName, _writable);
                        break;
                    case AttrDataFormat._SPECTRUM:
                        _dbProxy.exportToDB_Spectrum(_remoteFilePath, _exportFileName, _tableName, _writable);
                        break;
                    case AttrDataFormat._IMAGE:
                        _dbProxy.exportToDB_Image(_remoteFilePath, _exportFileName, _tableName, _writable);
                        break;
                    default:
                        Util.out2.println("exportThread.run : " + "DataFormat (" + _dataFormat + ") not supported !! ");
                        break;
                }
                String message = "Exported file successfully: _remoteFilePath|" + _remoteFilePath + "|_exportFileName|" + _exportFileName + "|_tableName|" + _tableName;
                _logger.trace(ILogger.LEVEL_DEBUG, message);
            } catch (ArchivingException e) {
                e.printStackTrace();
                Util.out2.println(e.toString());
                String message = "Problem (ArchivingException) exporting file: _remoteFilePath|" + _remoteFilePath + "|_exportFileName|" + _exportFileName + "|_tableName|" + _tableName;
                _logger.trace(ILogger.LEVEL_ERROR, message);
                _logger.trace(ILogger.LEVEL_ERROR, e);
            } catch (Throwable t) {
                t.printStackTrace();
                String message = "Problem (Throwable) exporting file: _remoteFilePath|" + _remoteFilePath + "|_exportFileName|" + _exportFileName + "|_tableName|" + _tableName;
                _logger.trace(ILogger.LEVEL_CRITIC, message);
                _logger.trace(ILogger.LEVEL_CRITIC, t);
            }
        }
    }

    /**
	 * This class represent the object that is called each time a file must be
	 * sent to the database.
	 */
    private class WindowThread implements Runnable {

        String state = "NOT RUNNING";

        public void activate(boolean b) {
            if (b) state = "RUNNING"; else state = "NOT RUNNING";
        }

        /**
		 * This method is called by the system to give a Thread a chance to
		 * clean up before it actually exits.
		 */
        public void destroy() {
            System.out.println("WindowThread.destroy");
            activate(false);
        }

        public synchronized void run() {
            activate(true);
            while (state.equals("RUNNING")) {
                try {
                    Thread.sleep(_windowsDuration);
                    String oldFileName;
                    flushBuffer();
                    oldFileName = get_fileName();
                    openFileSimply();
                    new ExportTask(oldFileName, true).execute();
                } catch (Throwable t) {
                    t.printStackTrace();
                    String message = "Problem (Throwable) in WindowThread.run file: _remoteFilePath|" + _remoteFilePath + "|_tableName|" + _tableName;
                    message += "|_bufferedWriter==null?|" + (_bufferedWriter == null);
                    _logger.trace(ILogger.LEVEL_CRITIC, message);
                    _logger.trace(ILogger.LEVEL_CRITIC, t);
                }
            }
            System.out.println("WindowThread exiting !!");
        }
    }

    public static void traceDouble(double[] in) {
        if (in == null) {
            return;
        }
        int len = in.length;
        for (int i = 0; i < len; i++) {
            System.out.println("i/" + i + "/in [ i ]/" + in[i]);
        }
    }
}
