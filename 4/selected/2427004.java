package picasatagstopictures.scan;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import picasatagstopictures.gui.IGuiFeedback;
import picasatagstopictures.util.FileUtil;
import picasatagstopictures.util.ScannerProperties;

/**
 *
 * @author oj
 */
public class PictureReaderWriter {

    private Logger logger;

    private RunFlag runFlag;

    private long maxWaitTimeForExifToolsMilliseconds;

    private boolean useFullNames;

    private boolean isReadOnlyFaces;

    private boolean isRemoveFaces;

    private boolean isReadOnlyGeo;

    private boolean isRemoveGeo;

    private boolean useBackUp;

    private IGuiFeedback guiFeedback;

    private ExiftoolCaller caller;

    private ExifToolsFeedback exifToolFeedback;

    private String exifToolArgFile;

    private boolean exiftoolRunning;

    /**
     * It starts Exiftool and keeps it open to listen to its args file.
     * Do not forget to call cleanUp() after using this method.
     * 
     * @param runFlag 
     */
    public PictureReaderWriter(IGuiFeedback guiFeedback, RunFlag runFlag) {
        this.logger = Logger.getLogger(this.getClass().getName());
        this.guiFeedback = guiFeedback;
        this.runFlag = runFlag;
    }

    public void init() throws ExiftoolTimeoutException {
        ScannerProperties scannerProperties = ScannerProperties.getInstance();
        this.useFullNames = scannerProperties.getBoolean(Scanner.KEY_WRITE_FULL_NAMES, true);
        this.useBackUp = scannerProperties.getBoolean(Scanner.KEY_USING_BACK_UP, false);
        this.isReadOnlyFaces = scannerProperties.getBoolean(Scanner.KEY_FACES_READ_ONLY, false);
        this.isRemoveFaces = scannerProperties.getBoolean(Scanner.KEY_FACES_REMOVE, false);
        this.isReadOnlyGeo = scannerProperties.getBoolean(Scanner.KEY_GEOTAGS_READ_ONLY, true);
        this.isRemoveGeo = scannerProperties.getBoolean(Scanner.KEY_GEOTAGS_REMOVE, false);
        this.maxWaitTimeForExifToolsMilliseconds = (long) scannerProperties.getInt(Scanner.KEY_WAIT_FOR_EXIFTOOLS_MILLISECONDS, (int) Scanner.DEFAULT_MAX_WAIT_TIME_FOR_EXIFTOOLS);
        this.prepareExifTool();
    }

    /**
     * Call allways befor using readPicture().
     * It starts Exiftool and keeps it open to listen to its args file.
     * Do not forget to call cleanUp() after using this method.
     * 
     * @return true if exiftool can be started and is responding to arguments
     * written to its args file,.
     */
    private void prepareExifTool() throws ExiftoolTimeoutException {
        if (!this.startExifTool()) {
            this.exiftoolRunning = false;
            return;
        } else {
            this.exiftoolRunning = true;
        }
    }

    public void readPicture(Picture picture) throws ExiftoolTimeoutException {
        if (picture != null) {
            this.logger.log(Level.FINE, "Start Reading/Writing picture {0}", picture.toString());
        }
        try {
            if (!this.exiftoolRunning) {
                String s = "Nothing was read or written by ExifTool. ExifTool seems to have a problem.";
                this.logger.fine(s);
                picture.appendMessage(s);
                return;
            }
            if (picture.getFile() == null) {
                String s = "Nothing was read or written by ExifTool. The picture (its data holder) contains no file.";
                this.logger.fine(s);
                picture.appendMessage(s);
                return;
            }
            String args = this.assembleReadMessage(picture);
            if (args == null) {
                picture.appendMessage("No file stored.");
                return;
            }
            List keywordsPicture = new ArrayList();
            String response = this.writeArgsToFileAndWait(args, picture, false);
            if (response == null) {
                return;
            } else {
                keywordsPicture = this.readResponse(response, picture);
            }
            this.updatePicture(keywordsPicture, picture);
        } finally {
            this.sendFeedback(picture);
        }
    }

    /**
     * Read the response of exiftool.
     * 1. Mark faces as "to be written" if not in
     * the keywords of the picture.
     * 2. Mark Geotags to be written if not contained in picture or different
     * than to picasa Geotags.
     * @param response of exiftool
     * @return The list of existing keywords of the picture
     */
    private List readResponse(String response, Picture picture) {
        List keywordsPicture = new ArrayList();
        if (response.equals("")) {
            this.markMissingKeywordsAsToBeWrittenOrRemoved(keywordsPicture, picture);
            return keywordsPicture;
        }
        Geotag geotag = picture.getGeotag();
        BufferedReader buffer = new BufferedReader(new StringReader(response));
        String line = null;
        try {
            while ((line = buffer.readLine()) != null) {
                this.logger.log(Level.FINER, "Starting to analyse the output of exiftools. Output was ''{0}''.", line);
                String[] splittees = line.split(":");
                int splitteesCount = splittees.length;
                if (splitteesCount == 2) {
                    String splittee = splittees[1].trim();
                    if (line.toLowerCase().contains("keywords")) {
                        keywordsPicture = this.makeListFromKeywords(splittee);
                        this.markMissingKeywordsAsToBeWrittenOrRemoved(keywordsPicture, picture);
                    } else if (line.toLowerCase().contains("gps latitude")) {
                        if (geotag == null) {
                            geotag = new Geotag();
                            picture.setGeotag(geotag);
                        }
                        geotag.setLatitudePicture(splittee);
                    } else if (line.toLowerCase().contains("gps longitude")) {
                        if (geotag == null) {
                            geotag = new Geotag();
                            picture.setGeotag(geotag);
                        }
                        geotag.setLongitudePicture(splittee);
                    } else if (line.toLowerCase().contains("gps position")) {
                        if (geotag == null) {
                            geotag = new Geotag();
                            picture.setGeotag(geotag);
                        }
                        String gpsPosition = splittee;
                        String[] pos = gpsPosition.split(",");
                        if (pos.length == 2) {
                            String lat = pos[0].trim();
                            String lon = pos[1].trim();
                            geotag.setLatitudePicture(lat);
                            geotag.setLongitudePicture(lon);
                        } else {
                            String warningMessage = "WARNING: Response of exiftool contains 'gps position' but contains no character ','. Response: " + line;
                            this.logger.warning(warningMessage);
                            picture.appendExiftoolErrorResponse(warningMessage);
                        }
                    } else {
                        String warningMessage = "ERROR: Response of exiftool does neither contain 'Keywords' nor 'GPS Latitude' nor 'GPS Longitude' nor 'GPS Position'. Response: " + line;
                        this.logger.warning(warningMessage);
                        picture.appendExiftoolErrorResponse(warningMessage);
                    }
                } else {
                    String warningMessage = "ERROR: Response of exiftool does not contain the character ':'. Response: " + line;
                    this.logger.warning(warningMessage);
                    picture.appendExiftoolErrorResponse(warningMessage);
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(PictureReaderWriter.class.getName()).log(Level.SEVERE, null, ex);
        }
        return keywordsPicture;
    }

    private String assembleReadMessage(Picture picture) {
        String file = picture.getFile();
        if (file == null) {
            this.logger.warning("The data holder of a picture has no file stored. This should never happen.");
            return null;
        }
        this.logger.log(Level.FINER, "Assemble read message for picture: ''{0}''", file);
        String args = "-Keywords\n-GPSLatitude\n-GPSLongitude\n-GPSPosition\n" + file + "\n-execute\n";
        return args;
    }

    /**
     * Write keywords and geotag to picture.
     * @param keywordsPicture
     * @param picture 
     * @return List of all keywords (as Strings) that are written to the picture
     * (includes existing an new). Returns null if exiftool could not write the
     * keywords to file.
     */
    private void updatePicture(List keywordsPicture, Picture picture) throws ExiftoolTimeoutException {
        boolean pictureNeedsWriting = false;
        if (!this.isReadOnlyFaces) {
            if (this.isRemoveFaces) {
                boolean b = this.removeKeywordsFromPicture(keywordsPicture, picture);
                if (b) {
                    pictureNeedsWriting = true;
                } else {
                    this.logger.fine("The picture does not contain any faces as keywords. No need to remove the keywords (update the picture for this reason).");
                }
            } else {
                boolean b = this.addKeywordsToWriteToPicture(keywordsPicture, picture);
                if (b) {
                    pictureNeedsWriting = true;
                } else {
                    this.logger.fine("The picture contains all faces as keywords. No need to update the keywords.");
                }
            }
        }
        if (!this.isReadOnlyGeo) {
            if (this.isRemoveGeo) {
                boolean b = this.markGeotagsAsToBeRemoved(picture);
                if (b) {
                    pictureNeedsWriting = true;
                } else {
                    this.logger.fine("The picture does not contain geo coordinates. No need to remove them (update the picture for this reason).");
                }
            } else {
                boolean b = this.markGeotagsAsToBeWritten(picture);
                if (b) {
                    pictureNeedsWriting = true;
                } else {
                    this.logger.fine("No need to update the geo coordinates.");
                }
            }
        }
        if (!pictureNeedsWriting) {
            this.logger.fine("No need to update the faces in the picture. The faces in this picture are neither marked as to be written nor to be removed.");
            return;
        }
        String file = picture.getFile();
        String args = this.assembleWriteMessage(keywordsPicture, file, picture);
        String response = this.writeArgsToFileAndWait(args, picture, false);
        if (response == null) {
            picture.appendMessage("Exiftool failed to write keywords into the picture.");
            this.markFacesAsWritten(picture, false);
            this.markFacesAsRemoved(picture, false);
            this.markGeopositionAsWritten(picture, false);
            this.markGeopositionAsRemoved(picture, false);
            return;
        } else {
            picture.appendMessage("Update of picture with keywords successfull. Exiftool responded with: " + response);
            this.markFacesAsWritten(picture, true);
            this.markFacesAsRemoved(picture, true);
            this.markGeopositionAsWritten(picture, true);
            this.markGeopositionAsRemoved(picture, true);
        }
    }

    private List makeListFromKeywords(String keywordsString) {
        String[] keywordsArray = keywordsString.split(",");
        int lenght = keywordsArray.length;
        this.logger.log(Level.FINER, "Making List of keywords String. Count of existsing keywords in picure is: {0}", lenght);
        ArrayList keywords = new ArrayList();
        for (int i = 0; i < lenght; i++) {
            String keyword = keywordsArray[i].trim();
            keywords.add(keyword);
            this.logger.log(Level.FINER, "Added keyword to List: {0}", keyword);
        }
        return keywords;
    }

    /**
     * Removes the face from the keywords if it finds the picasa full name
     * or the picasa display name.
     * @param keywordsPicture
     * @param picture
     * @return 
     */
    private boolean removeKeywordsFromPicture(List keywordsPicture, Picture picture) {
        Iterator itFaces = picture.getFaces().getFaces().iterator();
        boolean isPictureNeedsWriting = false;
        while (itFaces.hasNext()) {
            Face face = (Face) itFaces.next();
            boolean b = face.isToBeRemoved();
            if (b) {
                boolean hasRemoved = false;
                String keywordToRemove = face.getName();
                boolean contains = keywordsPicture.contains(keywordToRemove);
                if (contains) {
                    this.logger.log(Level.FINER, "Will remove the Picasa (full) name ''{0}''.", keywordToRemove);
                    keywordsPicture.remove(keywordToRemove);
                    isPictureNeedsWriting = true;
                    hasRemoved = true;
                }
                this.logger.log(Level.FINER, "Will remove the Picasa (full) name ''{0}''.", keywordToRemove);
                keywordToRemove = face.getDisplay();
                contains = keywordsPicture.contains(keywordToRemove);
                if (contains) {
                    this.logger.log(Level.FINER, "Will remove the Picasa display name ''{0}''.", keywordToRemove);
                    keywordsPicture.remove(keywordToRemove);
                    isPictureNeedsWriting = true;
                    hasRemoved = true;
                }
                if (!hasRemoved) {
                    this.logger.warning("The programm flow should never come here. Faild to remove a face from the keywords list. This list is later used be exiftool to write the keywords to file. What has done befor? The picasa faces of this picture where read. The keywords in the picture where read. At least one face in the keywords are marked as to be removed from the picture. Now it should be removed from an internal keywords list but this fails.");
                }
            }
        }
        return isPictureNeedsWriting;
    }

    private boolean markGeotagsAsToBeRemoved(Picture picture) {
        boolean isPictureNeedsWriting = false;
        Geotag geotag = picture.getGeotag();
        if (geotag == null) {
            this.logger.finer("Geo position should be written to picture but there is no geo postion (data object) for this picture.");
            return isPictureNeedsWriting;
        }
        String lat = geotag.getLatitudePicture();
        if (lat != null) {
            isPictureNeedsWriting = true;
            geotag.setToBeRemoved(true);
        }
        String lon = geotag.getLongitudePicture();
        if (lon != null) {
            isPictureNeedsWriting = true;
            geotag.setToBeRemoved(true);
        }
        return isPictureNeedsWriting;
    }

    private boolean markGeotagsAsToBeWritten(Picture picture) {
        boolean isPictureNeedsWriting = false;
        Geotag geotag = picture.getGeotag();
        if (geotag == null) {
            this.logger.finer("Geo position should be written to picture but there is no geo postion (data object) for this picture.");
            return isPictureNeedsWriting;
        }
        String lat = geotag.getLatitudePicture();
        String lon = geotag.getLongitudePicture();
        if (lat != null && lon != null) {
            this.logger.finer("Picture contains already geo coordinates. Do not allow to overwrite with geo coordinates of Picasa if any.");
        } else {
            lat = geotag.getLatitudePicasa();
            lon = geotag.getLongitudePicasa();
            if (lat != null && lon != null) {
                this.logger.finer("Picture contains no geo coordinates but Picasa has geo coordinates for this Picture.");
                geotag.setNeedsToBeWritten(true);
                isPictureNeedsWriting = true;
            } else {
                this.logger.finer("Picture contains no geo coordinates. The geo coordinates should be written to this picture but Picasa has no geo coordinates for this Picture.");
                geotag.setNeedsToBeWritten(false);
            }
        }
        return isPictureNeedsWriting;
    }

    private boolean addKeywordsToWriteToPicture(List keywordsPicture, Picture picture) {
        Iterator itFaces = picture.getFaces().getFaces().iterator();
        boolean isPictureNeedsWriting = false;
        while (itFaces.hasNext()) {
            Face face = (Face) itFaces.next();
            boolean b = face.isToBeWritten();
            if (b) {
                isPictureNeedsWriting = true;
                String keywordToAdd = null;
                if (this.useFullNames) {
                    keywordToAdd = face.getName();
                    this.logger.log(Level.FINER, "Will add the Picasa (full) name ''{0}''.", keywordToAdd);
                } else {
                    keywordToAdd = face.getDisplay();
                    this.logger.log(Level.FINER, "Will add the Picasa display name ''{0}''.", keywordToAdd);
                }
                keywordsPicture.add(keywordToAdd);
            }
        }
        return isPictureNeedsWriting;
    }

    private String assembleWriteMessage(List keywordsToWrite, String file, Picture picture) {
        this.logger.log(Level.FINER, "Assemble write message for picture keywords: ''{0}''", file);
        String args = "";
        Iterator itKeywords = keywordsToWrite.iterator();
        while (itKeywords.hasNext()) {
            String keywordToAdd = (String) itKeywords.next();
            if (args.isEmpty()) {
                args = "-Keywords=" + keywordToAdd + "\n";
            } else {
                args = args + "-Keywords=" + keywordToAdd + "\n";
            }
        }
        if (args.isEmpty()) {
            if (this.isRemoveFaces) {
                args = "-Keywords=\n";
            }
        }
        if (picture != null) {
            Geotag geotag = picture.getGeotag();
            if (geotag != null) {
                if (geotag.isNeedsToBeWritten()) {
                    String lat = geotag.getLatitudePicasa();
                    String lon = geotag.getLongitudePicasa();
                    String geoposition = lat + "," + lon;
                    if (args.isEmpty()) {
                        args = "-GPSLatitude=" + lat + "\n";
                        args = "-GPSLongitude=" + lon + "\n";
                    } else {
                        args = args + "-GPSLatitude=" + lat + "\n";
                        args = args + "-GPSLongitude=" + lon + "\n";
                    }
                } else if (geotag.isToBeRemoved()) {
                    if (args.isEmpty()) {
                        args = "-GPSLatitude=\n";
                        args = "-GPSLongitude=\n";
                    } else {
                        args = args + "-GPSLatitude=\n";
                        args = args + "-GPSLongitude=\n";
                    }
                }
            }
        }
        String feedbackString = "ExifTool-Write: ";
        if (args.isEmpty()) {
            feedbackString = feedbackString + "No keywords or geo coordinates to write";
        } else {
            args = args + file + "\n";
            if (!this.useBackUp) {
                this.logger.finer("The user wants to overwrite the original file.");
                args = args + "-overwrite_original\n";
            }
            args = args + "-execute\n";
            feedbackString = feedbackString + args.replaceAll("\n", " ");
        }
        this.logger.finer(feedbackString);
        return args;
    }

    private void markMissingKeywordsAsToBeWrittenOrRemoved(List keywordsPicture, Picture picture) {
        Iterator it = picture.getFaces().getFaces().iterator();
        while (it.hasNext()) {
            Face face = (Face) it.next();
            boolean isFaceInKeywords = this.isFaceInKeywords(keywordsPicture, face);
            face.setInKeywords(isFaceInKeywords);
            if (this.isRemoveFaces) {
                face.setToBeRemoved(isFaceInKeywords);
            } else {
                face.setToBeWritten(!isFaceInKeywords);
            }
        }
    }

    private void markFacesAsWritten(Picture picture, boolean successfullyWritten) {
        this.logger.finer("Mark all faces of the picture as (not) written successfully.");
        Iterator it = picture.getFaces().getFaces().iterator();
        while (it.hasNext()) {
            Face face = (Face) it.next();
            boolean b = face.isToBeWritten();
            if (b) {
                face.setWritten(successfullyWritten);
            }
        }
    }

    private void markGeopositionAsWritten(Picture picture, boolean successfullyWritten) {
        this.logger.finer("Mark geo postion of the picture as (not) written successfully.");
        Geotag geo = picture.getGeotag();
        if (geo != null) {
            boolean b = geo.isNeedsToBeWritten();
            if (b) {
                geo.setWritten(successfullyWritten);
            }
        }
    }

    private void markGeopositionAsRemoved(Picture picture, boolean successfullyWritten) {
        this.logger.finer("Mark geo position of the picture as (not) written successfully.");
        Geotag geo = picture.getGeotag();
        if (geo != null) {
            boolean b = geo.isToBeRemoved();
            if (b) {
                geo.setRemoved(successfullyWritten);
            }
        }
    }

    private void markFacesAsRemoved(Picture picture, boolean successfullyWritten) {
        this.logger.finer("Mark all faces of the picture as (not) removed successfully.");
        Iterator it = picture.getFaces().getFaces().iterator();
        while (it.hasNext()) {
            Face face = (Face) it.next();
            boolean b = face.isToBeRemoved();
            if (b) {
                face.setRemoved(successfullyWritten);
            }
        }
    }

    /**
     * Send a feedbackString the GUI
     * @param feedbackString
     */
    private void sendFeedback(String feedbackString) {
        if (this.guiFeedback == null) {
            return;
        }
        if (!this.runFlag.isRunning()) {
            return;
        }
        this.guiFeedback.receiveFeedback(feedbackString);
    }

    private void sendFeedback(Picture picture) {
        if (this.guiFeedback == null) {
            return;
        }
        if (!this.runFlag.isRunning()) {
            return;
        }
        this.guiFeedback.receiveFeedback(picture);
    }

    /**
     * 
     * Example: Picasa knows the face under 'Max Meier' as (full) name and as
     * 'Max' as display name. Exiftool reads 'Max' as keyword.
     * 
     * The method compares the display name AND the (full) name.
     * This prevents the user from accidently write the (full) name (Example: 'Max Meier')
     * into the picture if the picture already contains (the "face" Max Meier) as keyword 'Max' 
     * (what is the display name in Picasa.)
     * 
     * @param keywords the keywords found in the picture by exiftool
     * @return true if Picasa knows the face either as display name or as (full) name.
     */
    private boolean isFaceInKeywords(List keywordsPicture, Face face) {
        int lenght = keywordsPicture.size();
        this.logger.log(Level.FINER, "Check wether Faces is already in keywords list of picture. Count of existsing keywords in picure is: {0}", lenght);
        Iterator it = keywordsPicture.iterator();
        while (it.hasNext()) {
            String keyword = (String) it.next();
            this.logger.log(Level.FINER, "Start to compare existing keyword: {0}", keyword);
            String display = face.getDisplay();
            if (display != null && !"".equals(display)) {
                if (keyword.equalsIgnoreCase(display)) {
                    this.logger.log(Level.FINER, "Yes, picture already contains the display name ''{0}'' as keyword.", keyword);
                    return true;
                }
            }
            String name = face.getName();
            if (name != null && !"".equals(name)) {
                if (keyword.equalsIgnoreCase(name)) {
                    this.logger.log(Level.FINER, "Yes, picture already contains the (full) name ''{0}'' as keyword.", keyword);
                    return true;
                }
            }
            this.logger.log(Level.FINER, "No, picture does not contain the face (as display or full name) as keyword ''{0}''.", keyword);
        }
        this.logger.finer("No, picture does not contain the face (as display or full name) in the keywords of the picture.");
        return false;
    }

    /**
     * Create a new empty args file for exiftool.
     * Check if exiftool can be called at all.
     * Start exiftool and keep it open to listen to its args file.
     * Check if exiftool responds to messages written into its args file.
     * @return true if exiftool is ready.
     */
    private boolean startExifTool() throws ExiftoolTimeoutException {
        boolean isArgsFileOk = this.prepareExifToolArgsFile();
        if (!isArgsFileOk) {
            this.logger.warning("The args file for ExifTool can not be deleted or not created.");
            return false;
        }
        boolean exiftoolCanBeCalled = this.canExiftoolBeExecuted();
        if (!exiftoolCanBeCalled) {
            return false;
        }
        boolean exiftoolRespondsToArgsFile = this.isExiftoolReactingToArgsFile();
        if (!exiftoolRespondsToArgsFile) {
            this.cleanUp();
            return false;
        }
        return true;
    }

    /**
     * This object uses ExifTool via the args file of ExifTool.
     * ExifTool is told what to do via this args file.
     * 
     * This method tries to delete the args file. If successfull then it
     * writes ''
     * 
     * @return false if the file can not be deleted or written.
     */
    private boolean prepareExifToolArgsFile() {
        this.setExifToolArgsFile();
        if (this.exifToolArgFile != null && !"".equals(this.exifToolArgFile)) {
        } else {
            this.logger.severe("Do not know what args file to use. Please report to the developer.");
            return false;
        }
        File file = new File(this.exifToolArgFile);
        if (file.exists()) {
            this.logger.log(Level.FINER, "Args file ''{0}'' already exists. Trying to delete it...", this.exifToolArgFile);
            boolean deleted = file.delete();
            if (deleted) {
                this.logger.log(Level.FINER, "Yes, deleted args file of exiftool ''{0}''.", file);
            } else {
                this.logger.log(Level.WARNING, "No, could not delete args file of exiftool ''{0}''.", file);
                return false;
            }
        }
        try {
            boolean created = file.createNewFile();
            if (created) {
                this.logger.log(Level.FINE, "Args file ''{0}'' was created.", this.exifToolArgFile);
            } else {
                this.logger.log(Level.WARNING, "Args file ''{0}'' was NOT created.", this.exifToolArgFile);
            }
            return created;
        } catch (IOException ex) {
            Logger.getLogger(PictureReaderWriter.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }

    private void setExifToolArgsFile() {
        if (this.exifToolArgFile == null) {
            String dir = System.getProperty("user.dir");
            this.exifToolArgFile = dir + File.separator + "exiftool_args.txt";
        }
    }

    private boolean canExiftoolBeExecuted() {
        ExifToolsFeedback exifToolFeedbackTest = new ExifToolsFeedback();
        String command = "exiftool -ver";
        ExiftoolCaller testCaller = new ExiftoolCaller(exifToolFeedbackTest);
        testCaller.setCommand(command);
        boolean canExecute = testCaller.canExecuteCommand(command);
        if (canExecute) {
            this.guiFeedback.receiveFeedback("Exiftool can be used.");
            this.logger.fine("Yes, can execute exiftool.");
        } else {
            this.guiFeedback.receiveFeedback("Exiftool-Error: Exiftool can not read / write pictures. \nReason: Exiftool can not be found and/or used.\nExample: Under Windows the file 'exiftool.exe' might be missing.");
            this.logger.fine("No, can not execute exiftool.");
        }
        return canExecute;
    }

    /**
     * Start exiftool and keep it open to listen to its args file.
     * Check wether exiftool responds to messages written to the args file by
     * appending '-ver\n-execute\n' to the args file.
     * 
     * @return true if exiftool did send its version to std.out 
     */
    private boolean isExiftoolReactingToArgsFile() throws ExiftoolTimeoutException {
        this.exifToolFeedback = new ExifToolsFeedback();
        String command = "exiftool -stay_open True -@ " + exifToolArgFile;
        this.logger.log(Level.INFO, "Starting exiftool and keep it open to read the args file using command ''{0}''...", command);
        if (this.caller == null) {
            this.caller = new ExiftoolCaller(this.exifToolFeedback);
        }
        caller.setCommand(command);
        caller.start();
        String args = "-ver\n-execute\n";
        this.logger.fine("Testing if exiftool responds to messages written into the args file...");
        String versionString = this.writeArgsToFileAndWait(args, null, false);
        if (versionString == null) {
            this.guiFeedback.receiveFeedback("Can not read/write pictures. Reason: Exiftool does not respond to messages written to file '" + this.exifToolArgFile + "'");
            return false;
        }
        boolean isVersionString = versionString.matches("\\d+\\.\\d+");
        if (isVersionString) {
            this.guiFeedback.receiveFeedback("Exiftool version is: " + versionString);
            this.logger.log(Level.FINE, "Yes, exiftool seems to respond to arguments written to the args file ''{0}''. Exiftool replied its version ''{1}''.", new Object[] { this.exifToolArgFile, versionString });
        } else {
            this.guiFeedback.receiveFeedback("Can not read/write pictures. Reason: Exiftool responds to messages written to file '" + this.exifToolArgFile + "' but the respons '" + versionString + " seems to be not a version.");
            this.logger.log(Level.WARNING, "No, exiftool does not seem to respond to arguments written to the args file ''{0}''. Exiftool was asked for its version and replied with ''{1}''.", new Object[] { this.exifToolArgFile, versionString });
            return false;
        }
        return isVersionString;
    }

    public void cleanUp() throws ExiftoolTimeoutException {
        this.logger.fine("Cleaning up...");
        if (this.caller != null) {
            String args = "-stay_open\nfalse\n";
            String response = this.writeArgsToFileAndWait(args, null, true);
            if (response == null) {
                this.logger.log(Level.INFO, "Exiftool did not respond on trying to close it by sending the args ''{0}''.", args);
            } else if (response.equals("file not found")) {
                this.logger.log(Level.FINE, "Successfully closed exiftool by sending the args ''{0}''.", args);
            }
        }
    }

    private String writeArgsToFileAndWait(String args, Picture picture, boolean isStayFalseMessage) throws ExiftoolTimeoutException {
        if (picture != null) {
            picture.appendExiftoolArgs(args);
        }
        boolean wasWritten = this.writeCommandToExifToolArgsFile(args);
        if (!wasWritten) {
            return null;
        }
        long startTime = System.currentTimeMillis();
        long breakTime = startTime + this.maxWaitTimeForExifToolsMilliseconds;
        String messages = null;
        boolean hasSystemERRmessage = false;
        boolean keepWaiting = true;
        while (keepWaiting) {
            if (System.currentTimeMillis() > breakTime) {
                String msg = "Time out after '" + this.maxWaitTimeForExifToolsMilliseconds + "' ms. Exiftool was to slow to respond to the arguments '" + args + "' The args file of exiftool is '";
                this.logger.log(Level.WARNING, msg);
                if (picture != null) {
                    picture.appendExiftoolErrorResponse(msg);
                }
                throw new ExiftoolTimeoutException();
            }
            String stdOutErrMessage = exifToolFeedback.receiveFeedback(null);
            if (stdOutErrMessage == null) {
                continue;
            }
            BufferedReader bufReader = new BufferedReader(new StringReader(stdOutErrMessage));
            this.logger.log(Level.FINEST, "Exiftool responded to agruments. Arguments where: ''{0}''. Response was: ''{1}''.", new Object[] { args, stdOutErrMessage });
            try {
                String line = null;
                while ((line = bufReader.readLine()) != null) {
                    this.logger.log(Level.FINER, "Reading line: ''{0}''.", line);
                    line = line.trim();
                    if (line.matches("\\{ready\\d*\\}")) {
                        this.logger.log(Level.FINER, "Response ended successfully with ready message ''{0}''.", line);
                        keepWaiting = false;
                        if (!hasSystemERRmessage) {
                            if (messages == null) {
                                messages = "";
                            }
                        }
                        continue;
                    }
                    if (line.contains(ExiftoolCaller.ERR_STREAM)) {
                        if (line.matches("(?i).+file not found:")) {
                            if (line.toLowerCase().contains("file not found")) {
                                return "file not found";
                            }
                        } else {
                            hasSystemERRmessage = true;
                            String msg = "Exiftool responded to arguments via STD.ERR. Arguments where '" + args + "'. The response was '" + stdOutErrMessage + "'.";
                            this.logger.log(Level.WARNING, msg);
                            if (picture != null) {
                                picture.appendExiftoolErrorResponse(line);
                                picture.appendMessage(msg);
                            }
                        }
                    } else {
                        if (messages == null) {
                            messages = line;
                        } else {
                            if (picture != null) {
                                picture.appendExiftoolResponses(line);
                            }
                            messages = messages + "\n" + line;
                        }
                    }
                }
            } catch (IOException ex) {
                Logger.getLogger(PictureReaderWriter.class.getName()).log(Level.SEVERE, null, ex);
                return null;
            }
        }
        this.logger.log(Level.FINE, "Yes, exiftool responded to argument(s). Argument(s): ''{0}''. Response: ''{1}''.", new Object[] { args, messages });
        return messages;
    }

    private boolean writeCommandToExifToolArgsFile(String args) {
        this.logger.log(Level.FINER, "Trying to write arguemts to exiftools args file. Arguments are: ''{0}''. Exiftools args file is: ''{1}''.", new Object[] { args, this.exifToolArgFile });
        FileUtil util = new FileUtil();
        try {
            util.appendLineToFile(this.exifToolArgFile, args);
        } catch (Exception ex) {
            Logger.getLogger(PictureReaderWriter.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        return true;
    }

    /**
     * @return the exiftoolRunning
     */
    public boolean isExiftoolRunning() {
        return exiftoolRunning;
    }

    /**
     * @return the runFlag
     */
    public RunFlag getRunFlag() {
        return runFlag;
    }
}
