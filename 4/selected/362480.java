package name.vaites.ticketwatcher;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.net.URLEncoder;

/**
 *
 * @author john.vaites@gmail.com
 */
public class TicketWatcher {

    private static final TicketWatcher ticketWatcher = new TicketWatcher();

    private static String propertiesFileName;

    private static Properties props;

    public static String writeFolder;

    private static String readFolder;

    private static String archiveFolder;

    private static String statusFolder;

    private static String host;

    private static int port;

    private static int sockMode;

    private static int sockTimeout;

    private static String streamEndsWith;

    private static boolean xmlFile;

    private static boolean XmlValidation;

    private static String writeTransform;

    private static String readTransform;

    /**
     * getProperties loads up the properties file
     */
    private static void getProperties() {
        if (props == null) {
            props = new Properties();
        }
        try {
            props.load(new FileInputStream(propertiesFileName));
            props.list(System.out);
            host = props.getProperty("Host") == null ? "localhost" : props.getProperty("Host");
            port = Integer.valueOf((props.getProperty("Port") == null ? "80" : props.getProperty("Port")));
            writeFolder = props.getProperty("WriteFolder") == null ? "../tickets/dropcopy" : props.getProperty("WriteFolder");
            readFolder = props.getProperty("ReadFolder") == null ? "../tickets/read" : props.getProperty("ReadFolder");
            archiveFolder = props.getProperty("ArchiveFolder") == null ? "../tickets/archive" : props.getProperty("ArchiveFolder");
            statusFolder = props.getProperty("StatusFolder") == null ? "../tickets/status" : props.getProperty("StatusFolder");
            sockTimeout = (Integer.valueOf((props.getProperty("SockTimeout") == null) ? "1000" : props.getProperty("SockTimeOut")));
            streamEndsWith = ((props.getProperty("StreamEndsWith") == null) ? "</RWP_1>" : props.getProperty("StreamEndsWith"));
            sockMode = (Integer.valueOf((props.getProperty("SockMode") == null) ? "1" : props.getProperty("SockMode")));
            readTransform = ((props.getProperty("ReadTransform") == null) ? "../conf/readTransform.xslt" : props.getProperty("ReadTransform"));
            writeTransform = ((props.getProperty("WriteTransform") == null) ? "../conf/writeTransform.xslt" : props.getProperty("WriteTransform"));
            xmlFile = (Boolean.valueOf((props.getProperty("XmlFile") == null) ? "false" : props.getProperty("XmlFile")));
        } catch (IOException e) {
            e.getMessage();
        }
    }

    /**
     * New file needs processing
     * @param fileName of the file to be processed
     */
    public static void onNewFile(String fileName) {
        createStatusFile("NEW_FILE", "Processing new file " + fileName);
        StringBuilder result = new StringBuilder();
        String data = null;
        if (Utils.fileOrFolderExists(writeFolder + fileName)) {
            System.out.println("INFO: file exists - reading file next");
            data = Utils.readFile(writeFolder + fileName);
            System.out.println("INFO: file read " + data + "  now moving file to archive");
            Utils.moveFile(fileName, writeFolder, archiveFolder);
            if (Utils.fileOrFolderExists(writeFolder + fileName)) {
                System.out.println("tried to move file but it is still in the write folder");
            } else {
                System.out.println("INFO: file is gone from writeFolder");
            }
            try {
                if ((data != null) && (data.length() > 0)) {
                    System.out.println("INFO: despatching data");
                    switch(sockMode) {
                        case 1:
                            String params = URLEncoder.encode("data", "UTF-8") + "=" + URLEncoder.encode(data, "UTF-8");
                            result.append(Utils.httpUrlConnection_post(host, params));
                            break;
                        case 2:
                            result.append(Utils.inputStreamReader_readline(host, port, data));
                            break;
                        case 4:
                            if (!streamEndsWith.isEmpty()) {
                                result.append(Utils.inputStreamReader_readUntilEndsWith(host, port, data, streamEndsWith));
                            } else {
                                System.out.println("FATAL: configured to use inputStreamReader_readUntilEndsWith but streamEndsWith is empty");
                                System.exit(2);
                            }
                            break;
                        case 5:
                            result.append(Utils.inputStreamReader_timeout(host, port, data, sockTimeout));
                    }
                    if (!result.toString().isEmpty()) {
                        System.out.println("INFO: result = " + result.toString() + ".  writing file to read folder");
                        Utils.createFile(readFolder, fileName, result.toString());
                    } else {
                        System.out.println("WARNING: result is empty - nothing to write to read folder");
                    }
                } else {
                    createStatusFile("EMPTY_FILE", fileName + " is an empty file");
                }
            } catch (Exception e) {
                System.out.print(e);
            }
        } else {
            createStatusFile("FILE_READ_ERROR", writeFolder + fileName + " not found");
        }
    }

    /**
     * foldersExist validates folders exist on startup
     * @return true if all the necessary folders exist
     */
    private static boolean foldersExist() {
        if (!Utils.fileOrFolderExists(writeFolder)) {
            System.out.println("FATAL: Application Stopped: Write folder is missing (" + writeFolder + ")");
            return false;
        } else if (!Utils.fileOrFolderExists(readFolder)) {
            System.out.println("FATAL: Application Stopped: Read folder is missing (" + readFolder + ")");
            return false;
        } else if (!Utils.fileOrFolderExists(archiveFolder)) {
            System.out.println("FATAL: Application Stopped: Archive folder is missing (" + archiveFolder + ")");
            return false;
        } else if (!Utils.fileOrFolderExists(statusFolder)) {
            System.out.println("FATAL: Application Stopped: Status folder is missing (" + statusFolder + ")");
            return false;
        }
        return true;
    }

    /**
     * checkForTickets processes tickets in writeFolder that exist on startup
     */
    private static void checkForTickets() {
        File folder = new File(writeFolder);
        File[] files = folder.listFiles();
        for (File file : files) {
            if (file.isFile()) onNewFile(file.getName().toString());
        }
    }

    /**
     * createStatusFile writes a status file to statusFolder
     * @param eventType Things like FILE_NOT_FOUND.  Used in the filename.
     * @param message The contents of the status file
     */
    public static void createStatusFile(String eventType, String message) {
        String now = new java.text.SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_S_").format(new java.util.Date());
        String fileName = now + eventType + ".txt";
        Utils.createFile(statusFolder, fileName, message);
        System.out.println(fileName + " " + message);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        System.out.println("INFO: Starting jticketwriter");
        if (args[0] != null) {
            propertiesFileName = args[0];
        } else {
            propertiesFileName = "../conf/ticketwatcher.properties";
        }
        System.out.println("INFO: properties file used is: " + propertiesFileName);
        getProperties();
        if (!foldersExist()) {
            System.exit(1);
        }
        checkForTickets();
        WatchFolder watchFolder = new WatchFolder(ticketWatcher);
        Thread watchFolderThread = new Thread(watchFolder);
        watchFolderThread.start();
    }
}
