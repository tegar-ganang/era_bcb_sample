package com.quikj.server.log;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import com.quikj.server.framework.AceCommandService;
import com.quikj.server.framework.AceException;
import com.quikj.server.framework.AceInputSocketStream;
import com.quikj.server.framework.AceInputSocketStreamMessage;
import com.quikj.server.framework.AceLogMessage;
import com.quikj.server.framework.AceLogMessageInterface;
import com.quikj.server.framework.AceLogMessageParser;
import com.quikj.server.framework.AceLogger;
import com.quikj.server.framework.AceLoggerInterface;
import com.quikj.server.framework.AceMailMessage;
import com.quikj.server.framework.AceMailService;
import com.quikj.server.framework.AceMessageInterface;
import com.quikj.server.framework.AceSQL;
import com.quikj.server.framework.AceSQLMessage;
import com.quikj.server.framework.AceSignalMessage;
import com.quikj.server.framework.AceThread;
import com.quikj.server.framework.AceTimer;
import com.quikj.server.framework.AceTimerMessage;
import com.quikj.server.framework.AceTraceInfoMessage;
import com.quikj.server.framework.AceTraceReqMessage;

public class LogProcessor extends AceThread implements AceLoggerInterface {

    class LogMessageEvent implements AceMessageInterface {

        private AceLogMessage message;

        public LogMessageEvent(AceLogMessage message) {
            this.message = message;
        }

        public AceLogMessage getMessage() {
            return message;
        }

        public String messageType() {
            return "AceLogMessage";
        }
    }

    class SocketInfo {

        private BufferedWriter writer;

        private AceInputSocketStream stream;

        public SocketInfo(BufferedWriter writer, AceInputSocketStream stream) {
            this.writer = writer;
            this.stream = stream;
        }

        public AceInputSocketStream getStream() {
            return stream;
        }

        public BufferedWriter getWriter() {
            return writer;
        }
    }

    private static final String ARG_DIR = "dir=";

    private static final String ARG_FILE = "file=";

    private static final int MSG_ID_LEN = 5 + 1;

    private static final int HOST_NAME_LEN = 25 + 1;

    private static final int PROCESS_NAME_LEN = 15 + 1;

    private static final int PROCESS_INSTANCE_LEN = 3 + 1;

    private String hostName;

    private String processName;

    private int processInstance;

    private int txPort;

    private int rxPort;

    private HashMap txList = new HashMap();

    private HashMap rxList = new HashMap();

    private ConnectionListener txListener;

    private ConnectionListener rxListener;

    private int logGroup;

    private LogFile syslogFile = null;

    private LogFile oplogFile = null;

    private LogFile sysrepFile = null;

    private AceTimer timerQ = null;

    private int archiveTimerId = -1;

    private DocumentBuilder dBuilder = null;

    private int processGroupMask;

    private Hashtable traceInfo = new Hashtable();

    private String mailDir;

    private String mailFile;

    private String logEmailMessage = null;

    private Connection connection = null;

    private AceSQL database = null;

    private static LogProcessor instance = null;

    public LogProcessor() throws IOException, AceException, UnknownHostException, ParserConfigurationException {
        super("LogProcessor");
        timerQ = new AceTimer();
        timerQ.start();
        hostName = InetAddress.getLocalHost().getHostName();
        processName = LogConfiguration.Instance().getProcessName();
        processInstance = LogConfiguration.Instance().getProcessInstance();
        processGroupMask = LogConfiguration.Instance().getGroupMask();
        rxPort = LogConfiguration.Instance().getRxPort();
        txPort = LogConfiguration.Instance().getTxPort();
        logGroup = LogConfiguration.Instance().getLogGroup();
        txListener = new ConnectionListener("TxConnectionListener", new ServerSocket(txPort), new Integer(0));
        txListener.start();
        rxListener = new ConnectionListener("RxConnectionListener", new ServerSocket(rxPort), new Integer(1));
        rxListener.start();
        archiveTimerId = timerQ.startTimer(LogConfiguration.Instance().getNextArchivesInterval(), this, 0L);
        if (archiveTimerId == -1) {
            throw new AceException("Could not start archive timer");
        }
        if (LogConfiguration.Instance().saveToFile() == true) {
            syslogFile = new LogFile(AceLogger.SYSTEM_LOG, hostName);
            oplogFile = new LogFile(AceLogger.USER_LOG, hostName);
            sysrepFile = new LogFile(AceLogger.SYSTEM_REPORT, hostName);
        }
        try {
            String mailDir = LogConfiguration.Instance().getMailDir();
            String mailFile = LogConfiguration.Instance().getMailFile();
            if ((mailDir != null) && (mailFile != null)) {
                new AceMailService(mailDir, mailFile, this);
                AceMailService.getInstance().start();
            }
        } catch (Exception ex) {
            System.err.println("Error starting AceMailService " + ex.getClass().getName() + ": " + ex.getMessage());
        }
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setValidating(false);
        dbf.setIgnoringComments(true);
        dbf.setIgnoringElementContentWhitespace(true);
        dbf.setCoalescing(true);
        dBuilder = dbf.newDocumentBuilder();
        int command_port = LogConfiguration.Instance().getCommandPort();
        if (command_port >= 0) {
            AceCommandService service = new AceCommandService("Ace Log Server Management Console", "ALS> ", command_port, 10);
            service.registerCommandHandler("shutdown", new LogShutdownCommandHandler());
        }
        log(AceLogger.INFORMATIONAL, AceLogger.SYSTEM_LOG, "LogProcessor.LogProcessor() -- Log processor started");
        instance = this;
    }

    public static LogProcessor Instance() {
        return instance;
    }

    public static void main(String[] args) {
        String dir = null;
        String file = null;
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith(ARG_DIR) == true) {
                dir = args[i].substring(ARG_DIR.length());
            } else if (args[i].startsWith(ARG_FILE) == true) {
                file = args[i].substring(ARG_FILE.length());
            } else {
                System.err.println("Command line parameter : " + args[i] + " unrecognized");
                System.exit(1);
            }
        }
        if ((dir == null) || (file == null)) {
            System.err.println(ARG_DIR + " and/or " + ARG_FILE + " parameter missing in LogProcessor");
            System.exit(1);
        }
        try {
            new LogConfiguration(dir, file);
        } catch (Exception ex) {
            System.err.println(ex.getClass().getName() + " while loading configuration file : " + ex.getMessage());
            System.exit(1);
        }
        LogProcessor lp = null;
        try {
            lp = new LogProcessor();
            lp.start();
            Runtime.getRuntime().addShutdownHook(new ShutdownHandlerThread());
        } catch (Exception ex1) {
            System.err.println(ex1.getClass().getName() + " while loading log processor thread : " + ex1.getMessage());
            System.exit(1);
        }
        try {
            lp.join();
            System.exit(0);
        } catch (InterruptedException ex2) {
            System.err.println("InterruptedException : " + ex2.getMessage());
            System.exit(1);
        }
    }

    private void broadcastMessage(AceLogMessage message) {
        String formatted_message = formatMessage(message.getFormattedMessage());
        Iterator iter = rxList.entrySet().iterator();
        while (iter.hasNext() == true) {
            Map.Entry entry = (Map.Entry) iter.next();
            LogProcessor.SocketInfo info = (LogProcessor.SocketInfo) entry.getValue();
            BufferedWriter writer = info.getWriter();
            sendMessage(formatted_message, writer);
        }
    }

    private void broadcastTraceMessage(AceTraceInfoMessage message) {
        String formatted_message = formatMessage(message.getFormattedMessage());
        Iterator iter = txList.entrySet().iterator();
        while (iter.hasNext() == true) {
            Map.Entry entry = (Map.Entry) iter.next();
            LogProcessor.SocketInfo info = (LogProcessor.SocketInfo) entry.getValue();
            BufferedWriter writer = info.getWriter();
            sendMessage(formatted_message, writer);
        }
    }

    public void dispose() {
        if (interruptWait(AceSignalMessage.SIGNAL_TERM, "Request to kill the thread received") == false) {
            log(AceLogger.ERROR, AceLogger.SYSTEM_LOG, "LogProcessor.dispose() -- Error occured while sending signal : " + getErrorMessage());
        }
        if (AceMailService.getInstance() != null) {
            AceMailService.getInstance().dispose();
        }
        if (txListener != null) {
            txListener.dispose();
        }
        if (rxListener != null) {
            rxListener.dispose();
        }
        Iterator iter = txList.entrySet().iterator();
        while (iter.hasNext() == true) {
            Map.Entry entry = (Map.Entry) iter.next();
            LogProcessor.SocketInfo info = (LogProcessor.SocketInfo) entry.getValue();
            info.getStream().dispose();
        }
        txList.clear();
        iter = rxList.entrySet().iterator();
        while (iter.hasNext() == true) {
            Map.Entry entry = (Map.Entry) iter.next();
            LogProcessor.SocketInfo info = (LogProcessor.SocketInfo) entry.getValue();
            info.getStream().dispose();
        }
        rxList.clear();
        if (archiveTimerId != -1) {
            if (timerQ != null) {
                timerQ.cancelTimer(archiveTimerId, this);
            }
        }
        if (timerQ != null) {
            timerQ.dispose();
            timerQ = null;
        }
        if (syslogFile != null) {
            syslogFile.dispose();
        }
        if (oplogFile != null) {
            oplogFile.dispose();
        }
        if (sysrepFile != null) {
            sysrepFile.dispose();
        }
        if (database != null) {
            database.dispose();
            database = null;
        }
        if (AceCommandService.getInstance() != null) {
            AceCommandService.getInstance().dispose();
        }
        super.dispose();
    }

    private String format(long time_stamp, String host, String process_name, int process_instance, int severity, String message, int msg_id) {
        return (new AceLogMessage(new java.util.Date(time_stamp), 1, host, process_name, process_instance, severity, 0, message, msg_id, true)).getFormattedMessage();
    }

    private String formatForOutputDevice(long time_stamp, String host, String process_name, int process_instance, int severity, String message, int msg_id, boolean in_color) {
        StringBuffer buffer = new StringBuffer();
        String escape_char_prefix = null;
        if (in_color == true) {
            escape_char_prefix = AceLogger.COLOR_S[severity];
        } else {
            escape_char_prefix = "";
        }
        java.util.Date date = new java.util.Date(time_stamp);
        if (msg_id != -1) {
            buffer.append(pad0(process_instance, MSG_ID_LEN - 1));
        }
        pad(buffer, MSG_ID_LEN);
        buffer.append(AceLogger.SEVERITY_S[severity]);
        pad(buffer, MSG_ID_LEN + AceLogger.MAX_SEVERITY_LENGTH + 1);
        buffer.append(host);
        pad(buffer, MSG_ID_LEN + AceLogger.MAX_SEVERITY_LENGTH + 1 + HOST_NAME_LEN);
        buffer.append(process_name);
        pad(buffer, MSG_ID_LEN + AceLogger.MAX_SEVERITY_LENGTH + 1 + HOST_NAME_LEN + PROCESS_NAME_LEN);
        buffer.append(pad0(process_instance, PROCESS_INSTANCE_LEN - 1));
        pad(buffer, MSG_ID_LEN + AceLogger.MAX_SEVERITY_LENGTH + 1 + HOST_NAME_LEN + PROCESS_NAME_LEN + PROCESS_INSTANCE_LEN);
        buffer.append(message);
        return new String(escape_char_prefix + date.toString() + ' ' + buffer.toString() + '\n');
    }

    private String formatMessage(String message) {
        StringBuffer buffer = new StringBuffer();
        StringTokenizer tokens = new StringTokenizer(message, "\n");
        int count = tokens.countTokens();
        for (int i = 0; i < count; i++) {
            String line = tokens.nextToken();
            if (line.startsWith(".") == true) {
                buffer.append(" ." + line + '\n');
            } else {
                buffer.append(line + '\n');
            }
        }
        buffer.append(".\n");
        return buffer.toString();
    }

    private String getDateString(java.util.Date timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(timestamp);
        String date_string = cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH) + 1) + "-" + cal.get(Calendar.DAY_OF_MONTH) + " " + cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE) + ":" + cal.get(Calendar.SECOND);
        return date_string;
    }

    private Connection getDbConnection(LogDbInfo dbInfo) {
        try {
            boolean new_connection = false;
            if (connection == null) {
                new_connection = true;
            } else {
                if (connection.isClosed() == true) {
                    new_connection = true;
                    database = null;
                } else {
                    Statement st = connection.createStatement();
                    try {
                        st.executeQuery("SELECT 1 FROM DUAL");
                    } catch (SQLException e) {
                        new_connection = true;
                        database = null;
                    }
                }
            }
            if (new_connection == true) {
                Class.forName(dbInfo.getDbClass()).newInstance();
                String urlString = dbInfo.getDbmsUrl() + "://" + dbInfo.getDbHost() + "/" + dbInfo.getDbName() + "?useUnicode=true&characterEncoding=utf8";
                connection = DriverManager.getConnection(urlString, dbInfo.getDbUser(), dbInfo.getDbPassword());
                if (connection != null) {
                    database = new AceSQL(connection);
                }
                return connection;
            } else {
                return connection;
            }
        } catch (Exception ex) {
            System.err.println(" getDbConnection : An error occurred while trying to connect to the database server" + '\n' + "                   Exception processing result : " + ex.getMessage() + '\n');
            connection = null;
            database = null;
            return null;
        }
    }

    public java.lang.String getLogEmailMessage() {
        return logEmailMessage;
    }

    public boolean log(int severity, int msg_type, String message) {
        return log(severity, msg_type, message, -1);
    }

    public boolean log(int severity, int msg_type, String message, int msg_id) {
        AceLogMessage log_msg = new AceLogMessage(logGroup, hostName, processName, processInstance, severity, msg_type, message, msg_id);
        LogProcessor.LogMessageEvent event = new LogProcessor.LogMessageEvent(log_msg);
        return sendMessage(event);
    }

    private void pad(StringBuffer buffer, int length) {
        int num_pad = length - buffer.length();
        if (num_pad > 0) {
            char[] char_pad = new char[num_pad];
            for (int i = 0; i < num_pad; i++) {
                char_pad[i] = ' ';
            }
            buffer.append(char_pad);
        }
    }

    private String pad0(int num, int size) {
        String pad_str = "";
        String num_str = new String((new Integer(num)).toString());
        int num_pad = size - num_str.length();
        if (num_pad > 0) {
            char[] char_pad = new char[num_pad];
            for (int i = 0; i < num_pad; i++) {
                char_pad[i] = '0';
            }
            pad_str = new String(char_pad);
        }
        return new String(pad_str + num_str);
    }

    private void proccessSaveToFile(AceLogMessage message) {
        int msg_type = message.getMessageType();
        if ((msg_type == AceLogger.SYSTEM_LOG) || (msg_type == AceLogger.USER_LOG) || (msg_type == AceLogger.SYSTEM_REPORT)) {
            LogFile file = null;
            switch(msg_type) {
                case AceLogger.SYSTEM_LOG:
                    file = syslogFile;
                    break;
                case AceLogger.USER_LOG:
                    file = oplogFile;
                    break;
                case AceLogger.SYSTEM_REPORT:
                    file = sysrepFile;
                    break;
            }
            if (file.write(format(message.getTimeStamp(), message.getHostName(), message.getProcessName(), message.getProcessInstance(), message.getSeverity(), message.getMessage(), message.getMessageId())) == false) {
                log(AceLogger.ERROR, AceLogger.SYSTEM_LOG, "LogProcessor.processLogMessage() -- Error writing log message to the file");
            }
        }
    }

    private void processConnection(ConnectionEvent event, HashMap list) {
        try {
            Socket tx_socket = event.getSocket();
            tx_socket.setKeepAlive(true);
            tx_socket.setTcpNoDelay(true);
            int code = tx_socket.hashCode();
            BufferedWriter tx_writer = new BufferedWriter(new OutputStreamWriter(tx_socket.getOutputStream()));
            AceInputSocketStream reader_thread = new AceInputSocketStream(code, "AceLogger", tx_socket, true);
            reader_thread.start();
            list.put(new Integer(code), new LogProcessor.SocketInfo(tx_writer, reader_thread));
        } catch (Exception ex) {
            log(AceLogger.WARNING, AceLogger.SYSTEM_LOG, "LogProcessor.processConnection() -- Error creating socket stream : " + ex.getClass().getName() + ": " + ex.getMessage());
        }
    }

    private void processEmailNotification(AceLogMessage message) {
        LogEmailInfo emailInfoElement;
        emailInfoElement = new LogEmailInfo();
        if (LogConfiguration.Instance().checkEmailType(message.getSeverity(), emailInfoElement)) sendLogEmailNotification(message, emailInfoElement);
    }

    private void processLogDbSqlStmts(LogDbInfo dbInfo, AceLogMessage message) throws SQLException {
        String sqlStmt = "insert into " + LogDbInfo.getLogTblName() + " values (?, ?, ?, ?, ?, ?);";
        PreparedStatement pStmt = connection.prepareStatement(sqlStmt);
        pStmt.setString(1, getDateString(new java.util.Date(message.getTimeStamp())));
        pStmt.setInt(2, message.getSeverity());
        pStmt.setString(3, message.getHostName());
        pStmt.setString(4, message.getProcessName());
        pStmt.setString(5, String.valueOf(message.getProcessInstance()));
        pStmt.setString(6, message.getMessage());
        if (database.executeSQL(pStmt, (String[]) null, null) == -1) {
            System.err.println("processLogDbSqlStmts() : Couldn't store log record in the database");
        }
    }

    private void processLogMessage(AceInputSocketStreamMessage message) {
        Integer code = new Integer((int) message.getUserParm());
        LogProcessor.SocketInfo stream = (LogProcessor.SocketInfo) txList.get(code);
        HashMap list;
        if (stream == null) {
            stream = (LogProcessor.SocketInfo) rxList.get(code);
            if (stream == null) {
                log(AceLogger.WARNING, AceLogger.SYSTEM_LOG, "LogProcessor.processLogMessage() -- A log message is received from an unregistered input stream");
                return;
            } else {
                list = rxList;
            }
        } else {
            list = txList;
        }
        switch(message.getStatus()) {
            case AceInputSocketStreamMessage.EOF_REACHED:
                stream.getStream().dispose();
                list.remove(code);
                break;
            case AceInputSocketStreamMessage.READ_COMPLETED:
                try {
                    InputSource is = new InputSource(new StringReader(message.getLines()));
                    Document doc = dBuilder.parse(is);
                    AceLogMessageParser parser = new AceLogMessageParser(processGroupMask, doc);
                    AceLogMessageInterface msg_element = parser.getMessageElement();
                    if ((msg_element instanceof AceTraceReqMessage) == true) {
                        processTraceReqMessage((AceTraceReqMessage) msg_element, list, stream);
                    } else if ((msg_element instanceof AceLogMessage) == true) {
                        processLogMessage((AceLogMessage) msg_element, list, stream);
                    } else if ((msg_element instanceof AceTraceInfoMessage) == true) {
                        processTraceInfoMessage((AceTraceInfoMessage) msg_element, list, stream);
                    } else {
                        log(AceLogger.WARNING, AceLogger.SYSTEM_LOG, "LogProcessor.processLogMessage() -- An unexpected log message is received : " + msg_element.messageType());
                    }
                } catch (SAXException ex1) {
                    log(AceLogger.WARNING, AceLogger.SYSTEM_LOG, "LogProcessor.processLogMessage() -- A log message is received with invalid XML syntax : " + ex1.getMessage());
                    return;
                } catch (AceException ex2) {
                    log(AceLogger.WARNING, AceLogger.SYSTEM_LOG, "LogProcessor.processLogMessage() -- A log message is received with invalid syntax : " + ex2.getMessage());
                    return;
                } catch (IOException ex3) {
                    log(AceLogger.WARNING, AceLogger.SYSTEM_LOG, "LogProcessor.processLogMessage() -- A log message is received with IO error : " + ex3.getMessage());
                    return;
                }
                break;
            default:
                log(AceLogger.ERROR, AceLogger.SYSTEM_LOG, "LogProcessor.processLogMessage() -- A log message is received with invalid status : " + message.getStatus());
        }
    }

    private void processLogMessage(AceLogMessage message, HashMap list, LogProcessor.SocketInfo info) {
        broadcastMessage(message);
        if (LogConfiguration.Instance().printToConsole() == true) {
            processPrintToConsole(message);
        }
        if (LogConfiguration.Instance().saveToFile() == true) {
            proccessSaveToFile(message);
        }
        if (AceMailService.getInstance() != null) {
            processEmailNotification(message);
        }
        LogDbInfo dbInfoElement = LogConfiguration.Instance().checkLogDbInfo();
        if (dbInfoElement.isDbExists()) processSaveLogToDb(dbInfoElement, message);
    }

    private void processPrintToConsole(AceLogMessage message) {
        System.out.println(formatForOutputDevice(message.getTimeStamp(), message.getHostName(), message.getProcessName(), message.getProcessInstance(), message.getSeverity(), message.getMessage(), message.getMessageId(), LogConfiguration.Instance().printColor()));
    }

    private void processSaveLogToDb(LogDbInfo dbInfo, AceLogMessage message) {
        if (getDbConnection(dbInfo) == null) {
            System.err.println("processSaveLogToDB : Failure connecting to database " + dbInfo.getDbmsUrl() + '\n');
            return;
        }
        try {
            processLogDbSqlStmts(dbInfo, message);
        } catch (SQLException ex) {
            System.err.println("processSaveLogToDB : An SQL error occurred while trying to update the DB" + '\n' + "                     Exception processing result : " + ex.getMessage() + '\n');
            return;
        }
    }

    private void processSqlResult(AceSQLMessage message) {
        if (message.getStatus() == AceSQLMessage.SQL_ERROR) {
            System.err.println("processSqlResult() : Error encountered while storing log message in database");
            return;
        }
        if (message.getAffectedRows() != 1) {
            System.err.println("processSqlResult() : Error storing log message in database");
        }
    }

    private void processTimerMessage(AceTimerMessage message) {
        if (LogConfiguration.Instance().saveToFile() == false) {
            return;
        }
        if (syslogFile.archive() == false) {
            log(AceLogger.WARNING, AceLogger.SYSTEM_LOG, "LogProcessor.processTimerMessage() -- Archiving of the SYSLOG file failed : " + syslogFile.getErrorMessage());
        }
        if (oplogFile.archive() == false) {
            log(AceLogger.WARNING, AceLogger.SYSTEM_LOG, "LogProcessor.processTimerMessage() -- Archiving of the OPLOG file failed : " + oplogFile.getErrorMessage());
        }
        if (sysrepFile.archive() == false) {
            log(AceLogger.WARNING, AceLogger.SYSTEM_LOG, "LogProcessor.processTimerMessage() -- Archiving of the SYSREP file failed : " + sysrepFile.getErrorMessage());
        }
    }

    private void processTraceInfoMessage(AceTraceInfoMessage message, HashMap list, LogProcessor.SocketInfo info) {
        Hashtable trace_info = message.getTraceInformation();
        if (trace_info != null) {
            traceInfo = trace_info;
            AceTraceInfoMessage msg = new AceTraceInfoMessage(processGroupMask, traceInfo);
            broadcastTraceMessage(msg);
        }
    }

    private void processTraceReqMessage(AceTraceReqMessage message, HashMap list, LogProcessor.SocketInfo info) {
        AceTraceInfoMessage msg = new AceTraceInfoMessage(processGroupMask, traceInfo);
        sendMessage(formatMessage(msg.getFormattedMessage()), info.getWriter());
    }

    public void run() {
        while (true) {
            AceMessageInterface message = waitMessage();
            if (message == null) {
                log(AceLogger.FATAL, AceLogger.SYSTEM_LOG, "LogProcessor.run() -- An event is received with no event message : " + getErrorMessage());
                dispose();
                break;
            }
            if ((message instanceof AceTimerMessage) == true) {
                AceTimerMessage timer_message = (AceTimerMessage) message;
                if (timer_message.getTimerId() == archiveTimerId) {
                    processTimerMessage(timer_message);
                } else {
                    log(AceLogger.WARNING, AceLogger.SYSTEM_LOG, "LogProcessor.run() -- An unexpected timer event is received");
                }
            } else if ((message instanceof ConnectionEvent) == true) {
                ConnectionEvent cevent = (ConnectionEvent) message;
                if (((Integer) cevent.getUserParm()).intValue() == 0) {
                    processConnection(cevent, txList);
                } else if (((Integer) cevent.getUserParm()).intValue() == 1) {
                    processConnection(cevent, rxList);
                }
            } else if ((message instanceof AceInputSocketStreamMessage) == true) {
                processLogMessage((AceInputSocketStreamMessage) message);
            } else if ((message instanceof LogProcessor.LogMessageEvent) == true) {
                LogProcessor.LogMessageEvent event = (LogProcessor.LogMessageEvent) message;
                processLogMessage(event.getMessage(), null, null);
            } else if ((message instanceof AceSQLMessage) == true) {
                AceSQLMessage result_message = (AceSQLMessage) message;
                processSqlResult(result_message);
            } else if ((message instanceof AceSignalMessage) == true) {
                break;
            } else {
                log(AceLogger.WARNING, AceLogger.SYSTEM_LOG, "LogProcessor.run() -- An unexpected event is received : " + message.messageType());
            }
        }
    }

    private void sendLogEmailNotification(AceLogMessage emailMessage, LogEmailInfo emailInfoElement) {
        AceMailMessage msg = new AceMailMessage();
        try {
            msg.setSubject("Ace LOG notification");
            msg.setBody(formatForOutputDevice(emailMessage.getTimeStamp(), emailMessage.getHostName(), emailMessage.getProcessName(), emailMessage.getProcessInstance(), emailMessage.getSeverity(), emailMessage.getMessage(), emailMessage.getMessageId(), false));
            if (!emailInfoElement.getEmailToAddress().isEmpty()) {
                for (int index = 0; index < emailInfoElement.getEmailToAddress().size(); index++) {
                    msg.addTo(emailInfoElement.getEmailToAddress(index));
                }
                if (!emailInfoElement.getEmailCcAddress().isEmpty()) {
                    for (int index = 0; index < emailInfoElement.getEmailCcAddress().size(); index++) {
                        msg.addCc(emailInfoElement.getEmailCcAddress(index));
                    }
                }
            }
            if (AceMailService.getInstance().addToMailQueue(msg) == false) {
                System.err.println("Error adding message to mail service queue");
            }
        } catch (Exception ex1) {
            log(AceLogger.ERROR, AceLogger.SYSTEM_LOG, "LogProcessor.sendLogEmailNotification() -- IO Error while sending datagram message : " + ex1.getMessage());
        }
    }

    private boolean sendMessage(String message, BufferedWriter writer) {
        try {
            writer.write(message);
            writer.flush();
        } catch (IOException ex) {
            log(AceLogger.ERROR, AceLogger.SYSTEM_LOG, "LogProcessor.sendMessage() -- IO Error while sending message : " + ex.getMessage());
            return false;
        }
        return true;
    }

    public void setLogEmailMessage(java.lang.String logEmailMessage) {
        this.logEmailMessage = logEmailMessage;
    }
}
