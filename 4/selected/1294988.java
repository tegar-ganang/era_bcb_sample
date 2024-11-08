package picasatagstopictures.scan;

import picasatagstopictures.gui.IGuiFeedback;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import picasatagstopictures.util.FileUtil;
import picasatagstopictures.util.ScannerProperties;

/**
 *
 * @author Tom Wiedenhoeft, GPL v3
 */
public class Scanner extends Thread {

    public static final String KEY_USING_BACK_UP = "backup_pictures_when_writing";

    public static final String KEY_FACES_READ_ONLY = "is_faces_readonly";

    public static final String KEY_FACES_REMOVE = "is_faces_remove";

    public static final String KEY_GEOTAGS_READ_ONLY = "is_geotags_readonly";

    public static final String KEY_GEOTAGS_REMOVE = "is_geotags_remove";

    public static final String KEY_RECURSIV = "scan_directories_recursively";

    public static final String KEY_WRITE_FULL_NAMES = "write_picasa_full_names";

    public static final String KEY_CONTACTS = "contacts_file";

    public static final String KEY_FOLDER = "picture_folder";

    public static final String KEY_WAIT_FOR_EXIFTOOLS_MILLISECONDS = "wait_for_exiftools_max_milliseconds";

    public static long DEFAULT_MAX_WAIT_TIME_FOR_EXIFTOOLS = 200000;

    private Logger logger;

    private RunFlag runFlag;

    private String directory;

    private String contactsFile;

    private boolean recursively;

    private boolean backUpFiles;

    private boolean readFacesOnly;

    private boolean removeFaces;

    private boolean readGeotagsOnly;

    private boolean removeGeotags;

    private boolean writePicasaFullNames;

    private long maxWaitTimeForExifToolsMilliseconds;

    private Faces faces;

    private IGuiFeedback feedbackGUI;

    private PictureReaderWriter readerWriter;

    public Scanner() {
        this.logger = Logger.getLogger(this.getClass().getName());
        this.faces = new Faces();
        this.runFlag = new RunFlag();
    }

    public void setFeedback(IGuiFeedback feedback) {
        this.feedbackGUI = feedback;
    }

    @Override
    public void run() {
        this.logger.info("Start running...");
        this.loadScannerProperties();
        if (this.contactsFile == null) {
            String s = "Do not know the contacts.xml where picasa stores persons. Something went wrong with the configuration file. Scann can not be started.";
            this.sendFeedback(s);
            this.logger.severe(s);
            this.cleanUp();
            return;
        }
        if (this.directory == null) {
            String s = "Do not know wich directory to scan for picturs. Something went wrong with the configuration file. Scann can not be started.";
            this.sendFeedback(s);
            this.logger.severe(s);
            this.cleanUp();
            return;
        }
        this.sendFeedback("Start reading contacts (faces) from '" + this.contactsFile + "'.");
        ContactsXMLReader contactsReader = new ContactsXMLReader();
        this.faces = contactsReader.scanContactsXML(this.contactsFile);
        if (this.faces == null) {
            this.sendFeedback("Could not read contacts.xml (Picasa): " + this.contactsFile);
            this.cleanUp();
            return;
        } else {
            this.sendContactsFeedback();
        }
        try {
            this.sendFeedback("Start reading '.picasa.ini' from here: '" + this.directory + "'.");
            this.scanDir(this.directory);
        } catch (ExiftoolTimeoutException ex) {
            this.logger.log(Level.SEVERE, null, ex);
            this.sendFeedback("Exiftool-Error: Timout of Exiftool after '" + this.maxWaitTimeForExifToolsMilliseconds + "' milliseconds. More precise: Exiftool did not respond - via std.out or std.err - to the last arguments written to the args file of exiftool.");
        } catch (Exception ex) {
            this.logger.log(Level.SEVERE, null, ex);
            this.sendFeedback("Something went wrong while the scanner thread was running. The java error message is: " + ex.getMessage());
        } finally {
            this.logger.finest("Exiting run()...");
            this.sendFeedback("Good by. Scanning complete.");
            this.cleanUp();
        }
    }

    public void cleanUp() {
        this.logger.fine("Cleaning up...");
        if (this.readerWriter != null) {
            try {
                this.readerWriter.cleanUp();
            } catch (ExiftoolTimeoutException ex) {
                this.sendFeedback("Exiftool-Error: Failed to stop exiftool. Please check if the process is still running.");
                Logger.getLogger(Scanner.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (this.feedbackGUI != null) {
            this.feedbackGUI.receiveFeedback("cleanup");
        }
    }

    private void scanDir(String baseDir) throws ExiftoolTimeoutException {
        if (!this.runFlag.isRunning()) {
            this.logger.log(Level.FINER, "Was about to scan directory {0} but scanning (thread) was stopped.", baseDir);
            return;
        }
        this.logger.log(Level.FINER, "About to scan directory {0}", baseDir);
        this.sendFeedback("Scanning dir... " + baseDir);
        File base = new File(baseDir);
        File[] files = base.listFiles();
        if (files == null) {
            this.logger.log(Level.FINER, "No files or directories found in directory {0}", baseDir);
            return;
        }
        int length = files.length;
        this.logger.log(Level.FINER, "Directory {0} contains ''{1}'' files/directories.", new Object[] { baseDir, length });
        File picasaIni = null;
        for (int i = 0; i < length; i++) {
            File file = files[i];
            if (!this.runFlag.isRunning()) {
                this.logger.log(Level.FINER, "Was about to read file {0} but scanning (thread) was stopped.", file.getAbsolutePath());
                return;
            }
            if (file.isFile()) {
                String fileName = file.getName();
                if (fileName.matches("(?i)\\.picasa\\.ini")) {
                    this.logger.log(Level.FINER, "Found file {0}.", fileName);
                    picasaIni = file;
                } else {
                    this.logger.log(Level.FINER, "Found file {0} but it is not ''.picasa.ini''.", fileName);
                }
            }
            if (picasaIni != null) {
                break;
            }
        }
        if (picasaIni == null) {
            this.logger.log(Level.FINER, "Found no ''.picasa.ini'' in directory ''{0}''.", baseDir);
        } else {
            this.scanPicasaIni(picasaIni);
            if (!this.runFlag.isRunning()) {
                return;
            }
        }
        for (int i = 0; i < length; i++) {
            File file = files[i];
            if (file.isDirectory()) {
                String path = file.getAbsolutePath();
                this.logger.log(Level.FINER, "Jumping from base directory ''{0}'' to sub directory ''{1}''.", new Object[] { baseDir, path });
                if (!this.recursively) {
                    return;
                }
                this.scanDir(path);
            }
        }
        this.logger.log(Level.FINER, "No more sub directories found in ''{0}''.", baseDir);
    }

    private void loadScannerProperties() {
        ScannerProperties scannerProperties = ScannerProperties.getInstance();
        this.backUpFiles = scannerProperties.getBoolean(Scanner.KEY_USING_BACK_UP, false);
        this.maxWaitTimeForExifToolsMilliseconds = (long) scannerProperties.getInt(Scanner.KEY_WAIT_FOR_EXIFTOOLS_MILLISECONDS, (int) Scanner.DEFAULT_MAX_WAIT_TIME_FOR_EXIFTOOLS);
        this.readFacesOnly = scannerProperties.getBoolean(Scanner.KEY_FACES_READ_ONLY, false);
        this.removeFaces = scannerProperties.getBoolean(Scanner.KEY_FACES_REMOVE, false);
        this.readGeotagsOnly = scannerProperties.getBoolean(Scanner.KEY_GEOTAGS_READ_ONLY, false);
        this.removeGeotags = scannerProperties.getBoolean(Scanner.KEY_GEOTAGS_REMOVE, false);
        this.writePicasaFullNames = scannerProperties.getBoolean(Scanner.KEY_WRITE_FULL_NAMES, true);
        this.recursively = scannerProperties.getBoolean(Scanner.KEY_RECURSIV, true);
        this.contactsFile = scannerProperties.get(Scanner.KEY_CONTACTS, null);
        if (this.contactsFile == null) {
            this.logger.log(Level.SEVERE, "Found no contacts file in scanner properties ''{0}''.", scannerProperties.getPropertiesFile());
            ResourceFinder resFinder = new ResourceFinder();
            this.contactsFile = resFinder.findContactsXML();
            this.logger.log(Level.INFO, "Found and use default contacts.xml: {0}", this.contactsFile);
        }
        this.directory = scannerProperties.get(Scanner.KEY_FOLDER, null);
        if (this.directory == null) {
            this.logger.log(Level.SEVERE, "Found no pictures directory in scanner properties ''{0}''.", scannerProperties.getPropertiesFile());
        }
        this.logger.log(Level.CONFIG, "Loaded Scanner Properties from file ''{0}'': Picasa contacts file = ''{1}'', picture directory to scan = ''{2}'', scan direcetories recursively = ''{3}'', faces read only = ''{4}'', faces remove = ''{5}'', geotags read only = ''{6}'', geotags remove = ''{7}'', write backup files = ''{8}'', use picasa full names = ''{9}''.", new Object[] { ScannerProperties.getInstance().getPropertiesFile(), this.contactsFile, this.directory, this.recursively, this.readFacesOnly, this.removeFaces, this.readGeotagsOnly, this.removeGeotags, this.backUpFiles, this.writePicasaFullNames });
    }

    private void scanPicasaIni(File picasaIni) throws ExiftoolTimeoutException {
        FileUtil util = new FileUtil();
        String fileContent = null;
        try {
            fileContent = util.getFileAsString(picasaIni.getAbsoluteFile());
        } catch (Exception ex) {
            Logger.getLogger(Scanner.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        String baseDir = picasaIni.getParent();
        BufferedReader in = new BufferedReader(new StringReader(fileContent));
        Picture picture = null;
        try {
            String line = null;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("[") && line.endsWith("]")) {
                    if (picture != null) {
                        this.logger.log(Level.FINER, "Start to release (write) the values (faces, geo) of old file name: {0}", picture.getFile());
                        this.readWritePicture(picture);
                        if (!this.runFlag.isRunning()) {
                            return;
                        }
                    }
                    picture = new Picture();
                    String fileName = line;
                    fileName = fileName.replaceAll("\\[|\\]", "").trim();
                    fileName = baseDir + File.separator + fileName;
                    picture.setFile(fileName);
                    this.logger.log(Level.FINER, "Found file name ''{0}'' in file ''{1}''.", new Object[] { fileName, picasaIni.getAbsolutePath() });
                }
                if (line.toLowerCase().startsWith("faces=")) {
                    String expression = ",\\p{Alnum}+";
                    Pattern pattern = Pattern.compile(expression);
                    this.logger.log(Level.FINER, "Trying to find expression {0} in line ''{1}''", new Object[] { expression, line });
                    Matcher matcher = pattern.matcher(line);
                    while (matcher.find()) {
                        String faceId = matcher.group();
                        faceId = faceId.replaceAll(",", "");
                        Face face = this.faces.getFaceById(faceId);
                        if (face != null) {
                            Face copy = face.getCopy();
                            copy.setFile(picture.getFile());
                            picture.addFace(copy);
                            this.logger.log(Level.FINER, "Found face id ''{0}'' in line ''{1}'' in file ''{2}''.", new Object[] { faceId, line, picasaIni.getAbsolutePath() });
                        } else {
                            String msg = "Found face id '" + faceId + "' in line '" + line + "' without person from contact.xml. File: '" + picasaIni.getAbsolutePath() + "'.";
                            picture.appendPicasaScannerErrorResponse(msg);
                            this.logger.log(Level.WARNING, msg);
                        }
                    }
                    if (!picture.hasFaces()) {
                        String msg = "Found 'faces=' without id in .picasa.ini file '" + picasaIni.getAbsolutePath() + "'.";
                        this.logger.log(Level.FINE, msg);
                    }
                }
                if (line.toLowerCase().startsWith("geotag=")) {
                    String expression = "=.+";
                    Pattern pattern = Pattern.compile(expression);
                    this.logger.log(Level.FINER, "Trying to find expression {0} in line ''{1}''", new Object[] { expression, line });
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        String coordinates = matcher.group();
                        coordinates = coordinates.replaceAll("=", "");
                        this.logger.log(Level.FINER, "Found face id ''{0}'' in line ''{1}'' in file ''{2}''.", new Object[] { coordinates, line, picasaIni.getAbsolutePath() });
                        String[] splittees = coordinates.split(",");
                        if (splittees.length == 2) {
                            String latitude = splittees[0].trim();
                            String longitude = splittees[1].trim();
                            Geotag gtag = new Geotag();
                            gtag.setLatitudePicasa(latitude);
                            gtag.setLongitudePicasa(longitude);
                            picture.setGeotag(gtag);
                        }
                    } else {
                        String msg = "Found 'geotag=' without coordinates in .picasa.ini file '" + picasaIni.getAbsolutePath() + "'.";
                        picture.appendPicasaScannerErrorResponse(msg);
                        this.logger.log(Level.FINE, msg);
                    }
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(Scanner.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (picture != null) {
            this.readWritePicture(picture);
            if (!this.runFlag.isRunning()) {
                return;
            }
        }
    }

    private void readWritePicture(Picture picture) throws ExiftoolTimeoutException {
        if (this.readerWriter == null) {
            this.readerWriter = new PictureReaderWriter(this.feedbackGUI, this.runFlag);
            this.readerWriter.init();
            if (!this.readerWriter.isExiftoolRunning()) {
                this.logger.warning("Exiftool cound not be started.");
                this.runFlag.setRunning(false);
                return;
            }
        }
        this.readerWriter.readPicture(picture);
    }

    /**
     * Send a feedbackString the GUI
     * @param feedbackString
     */
    private void sendFeedback(String feedbackString) {
        if (this.feedbackGUI == null) {
            return;
        }
        if (!this.runFlag.isRunning()) {
            return;
        }
        this.feedbackGUI.receiveFeedback(feedbackString);
    }

    private void sendContactsFeedback() {
        Iterator it = this.faces.getFaces().iterator();
        while (it.hasNext()) {
            Face face = (Face) it.next();
            String faceId = face.getId();
            String name = face.getName();
            String displayName = face.getDisplay();
            String modifiedTime = face.getModified();
            String feedbackString = "new face\nid=" + faceId + "\nname=" + name + "\ndisplay=" + displayName + "\nmodified_time=" + modifiedTime;
            this.logger.log(Level.FINE, "Sending feedback to GUI: {0}", feedbackString);
            this.sendFeedback(feedbackString);
        }
    }

    public void stopRuning() {
        this.logger.fine("Stop running...");
        this.runFlag.setRunning(false);
        this.logger.finer("Scanner was told to stop.)");
    }
}
