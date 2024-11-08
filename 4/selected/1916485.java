package net.walkingtools.j2se.editor;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.jar.*;
import java.util.prefs.*;
import java.util.zip.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.xml.parsers.*;
import net.walkingtools.*;
import net.walkingtools.gpsTypes.*;
import net.walkingtools.gpsTypes.hiperGps.*;
import net.walkingtools.international.*;
import net.walkingtools.server.*;
import net.walkingtools.server.gpx.*;
import org.xml.sax.*;

/**
 * A graphical user interface for editing Walking Tools projects
 * @author Brett Stalbaum
 * @version 0.1.1
 * @since 0.0.4
  */
public class HiperGpsGui extends JFrame implements ActionListener, TableModelListener, HiperGpsCommunicatorListener, HiperGpsLoginListener, Runnable {

    /**
     * serialVersionUID for Serializable
     */
    public static final long serialVersionUID = 0xf36eb80;

    private static final String fileSeparator = System.getProperty("file.separator");

    private static final String DEPLOYDIR = fileSeparator + "deploy";

    private static final String SAMPLEDIR = fileSeparator + "sample-files";

    private String currentProjectDir = null;

    private File hiperGpsHome = null;

    private String currentProjectName = null;

    private File lastOpenDirectory = null;

    private Jad jad = null;

    private Vector<GpxWaypoint> wptVector = null;

    private GpxFormatter formatter = null;

    private int type = -1;

    private TypeFilter pngFilter = null;

    private TypeFilter wavFilter = null;

    private HiperWaypointTable wptTable = null;

    private AbstractTableModel tableModel = null;

    private int jarSize = 0;

    private Vector<File> imageFiles = null;

    private Vector<File> audioFiles = null;

    private Translation[] translation = null;

    private int currentTranslation = 0;

    private Preferences prefs = null;

    private boolean closing = false;

    private String hiperGeoId = null;

    private User user = null;

    private String jsessionId = null;

    private Communicator communicator = null;

    private String statusLabelString = null;

    private boolean projectStatusOk = false;

    private String serverBaseUrl = null;

    /** Creates new form HiperGpsGui */
    public HiperGpsGui() {
        serverBaseUrl = "http://132.239.234.247/wtserver/";
        setMinimumSize(new java.awt.Dimension(1000, 625));
        setPreferredSize(new java.awt.Dimension(1000, 625));
        Properties props = new Properties(System.getProperties());
        props.setProperty("line.separator", "\r\n");
        props.put("file.encoding", "UTF-8");
        System.setProperties(props);
        Translations translations = new Translations();
        Enumeration<Translation> enumer = new Translations().getTranslations();
        translation = new Translation[translations.size()];
        int i = 0;
        while (enumer.hasMoreElements()) {
            translation[i++] = enumer.nextElement();
        }
        Arrays.sort(translation);
        initComponents();
        for (i = 0; i < translation.length; i++) {
            JMenuItem jmi = new JMenuItem(translation[i].getName());
            jmi.addActionListener(this);
            languageMenu.add(jmi);
            if (translation[i].getName().equals("Espanol")) {
                jmi.setEnabled(false);
            }
        }
        prefs = Preferences.userNodeForPackage(this.getClass());
        currentTranslation = prefs.getInt("language", 0);
        imageFiles = new Vector<File>(20);
        audioFiles = new Vector<File>(20);
        hiperGpsHome = new File(System.getProperty("user.dir"));
        lastOpenDirectory = hiperGpsHome;
        formatter = new GpxFormatter(false);
        type = GpsTypeContainer.EMPTY;
        pngFilter = new TypeFilter(translation[currentTranslation].translate("PNG image file"), ".png");
        wavFilter = new TypeFilter(translation[currentTranslation].translate("WAV audio"), ".wav");
        wptVector = new Vector<GpxWaypoint>(0);
        wptTable = new HiperWaypointTable(this, wptVector, translation[currentTranslation].getLocale());
        wptTable.setShowGrid(true);
        wptTable.setGridColor(new Color(238, 238, 238));
        wptTable.setEnabled(false);
        tableModel = (AbstractTableModel) wptTable.getModel();
        wapointsScrollPane.setViewportView(wptTable);
        statusLabelString = translation[currentTranslation].translate("Status: ");
        communicator = new Communicator(this, serverBaseUrl);
        setToCurrentLanguage();
        aboutMenuItem.setEnabled(true);
        exitMenuItem.setEnabled(true);
        fileMenu.setEnabled(true);
        newProjectMenuItem.setEnabled(true);
        openProjectMenuItem.setEnabled(true);
        ingestMenu.setEnabled(true);
        typeMenu.setEnabled(true);
        exitMenuItem.addActionListener(this);
        newProjectMenuItem.addActionListener(this);
        openProjectMenuItem.addActionListener(this);
        closeProjectMenuItem.addActionListener(this);
        ingestImageMenuItem.addActionListener(this);
        ingestAudioMenuItem.addActionListener(this);
        deleteImageMenuItem.addActionListener(this);
        deleteAudioMenuItem.addActionListener(this);
        addWaypointButton.addActionListener(this);
        deleteWaypointButton.addActionListener(this);
        aboutMenuItem.addActionListener(this);
        zoomInButton.addActionListener(this);
        zoomOutButton.addActionListener(this);
        zoomResetButton.addActionListener(this);
        newProjectFromGpxMenuItem.addActionListener(this);
        importGpxWaypointsMenuItem.addActionListener(this);
        moveUpButton.addActionListener(this);
        moveDownButton.addActionListener(this);
        setFreeFormMenuItem.addActionListener(this);
        setRouteMenuItem.addActionListener(this);
        saveAsMenuItem.addActionListener(this);
        uploadUpdateMenuItem.addActionListener(this);
        aboutHiperGeoOnlineMenuItem.addActionListener(this);
        setTitle(translation[currentTranslation].translate("HiperGps Project Manager"));
        enterClosedState();
    }

    /**
    * Main method
    * @param args the command line arguments
    */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                try {
                    UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
                } catch (ClassNotFoundException ex) {
                    errorOutOnStart();
                } catch (InstantiationException ex) {
                    errorOutOnStart();
                } catch (IllegalAccessException ex) {
                    errorOutOnStart();
                } catch (UnsupportedLookAndFeelException ex) {
                    errorOutOnStart();
                }
                new HiperGpsGui().setVisible(true);
            }
        });
    }

    private static void errorOutOnStart() {
        JOptionPane.showMessageDialog(null, "Can't use Metal look and feel. This is the java platform look and feel, " + "thus something is terribly wrong, if not nearly impossible.", "Can't use Metal look and feel", JOptionPane.INFORMATION_MESSAGE);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals(translation[currentTranslation].translate("Exit"))) {
            System.exit(0);
        } else if (e.getActionCommand().equals(translation[currentTranslation].translate("About HiperGps"))) {
            showDialog(translation[currentTranslation].translate("Walkingtools HiperGps Editor\n") + translation[currentTranslation].translate("Cicero Silva and Brett Stalbaum\n") + WalkingtoolsInformation.license + "\n" + translation[currentTranslation].translate("See http://www.walkingtools.net."), translation[currentTranslation].translate("About HiperGps and WalkingTools"), JOptionPane.INFORMATION_MESSAGE);
        } else if (e.getActionCommand().equals(translation[currentTranslation].translate("About HiperGeoOnline"))) {
            showDialog("For more information on HiperGeo, please see:\nhttp://www.walkingtools.net/?page_id=361", "More Information", JOptionPane.INFORMATION_MESSAGE);
        } else if (e.getActionCommand().equals(translation[currentTranslation].translate("New Project"))) {
            newEmptyProject();
        } else if (e.getActionCommand().equals(translation[currentTranslation].translate("New Project From GPX File"))) {
            newProjectFromGpx();
        } else if (e.getActionCommand().equals(translation[currentTranslation].translate("Open Project Folder"))) {
            openProject();
        } else if (e.getActionCommand().equals(translation[currentTranslation].translate("Import GPX Waypoints"))) {
            importGpxWaypoints();
        } else if (e.getActionCommand().equals(translation[currentTranslation].translate("Close Project"))) {
            writeGpx();
            writeJar();
            writeJad();
            enterClosedState();
        } else if (e.getActionCommand().equals(translation[currentTranslation].translate("Ingest Image"))) {
            ingestFile(pngFilter, WalkingtoolsInformation.IMAGEDIR);
        } else if (e.getActionCommand().equals(translation[currentTranslation].translate("Ingest Audio"))) {
            ingestFile(wavFilter, WalkingtoolsInformation.AUDIODIR);
        } else if (e.getActionCommand().equals(translation[currentTranslation].translate("Delete Image Asset"))) {
            deleteFile(pngFilter, WalkingtoolsInformation.IMAGEDIR);
        } else if (e.getActionCommand().equals(translation[currentTranslation].translate("Delete Audio Asset"))) {
            deleteFile(wavFilter, WalkingtoolsInformation.AUDIODIR);
        } else if (e.getActionCommand().equals(translation[currentTranslation].translate("+ row"))) {
            addRow();
        } else if (e.getActionCommand().equals(translation[currentTranslation].translate("- row"))) {
            removeRow(wptTable.getSelectedRow());
        } else if (e.getActionCommand().equals(translation[currentTranslation].translate("+"))) {
            mapPanel.zoomIn();
        } else if (e.getActionCommand().equals(translation[currentTranslation].translate("-"))) {
            mapPanel.zoomOut();
        } else if (e.getActionCommand().equals(translation[currentTranslation].translate("recenter"))) {
            mapPanel.resetView();
        } else if (e.getActionCommand().equals(translation[currentTranslation].translate("move up"))) {
            moveRowUp(wptTable.getSelectedRow());
        } else if (e.getActionCommand().equals(translation[currentTranslation].translate("move down"))) {
            moveRowDown(wptTable.getSelectedRow());
        } else if (e.getActionCommand().equals(translation[currentTranslation].translate("Set Freeform Tour"))) {
            setType(GpsTypeContainer.WAYPOINTS);
        } else if (e.getActionCommand().equals(translation[currentTranslation].translate("Set Route Tour"))) {
            setType(GpsTypeContainer.ROUTE);
        } else if (e.getActionCommand().equals(translation[currentTranslation].translate("Save As..."))) {
            copyProject();
        } else if (e.getActionCommand().equals(translation[currentTranslation].translate("Upload/Update Project"))) {
            getUserInfo();
        } else {
            for (int i = 0; i < translation.length; i++) {
                if (e.getActionCommand().equals(translation[i].getName())) {
                    currentTranslation = i;
                    prefs.putInt("language", i);
                    setToCurrentLanguage();
                    break;
                }
            }
        }
    }

    private void setToCurrentLanguage() {
        Locale.setDefault(translation[currentTranslation].getLocale());
        mapPanel.setTranslation(translation[currentTranslation]);
        wptTable.setTranslation(translation[currentTranslation]);
        wptTable.setEnabled(false);
        wptTable.setEnabled(true);
        setTitle(translation[currentTranslation].translate("HiperGps Project Manager") + ": " + currentProjectName);
        setName("hiperWaypointFrame");
        addWaypointButton.setText(translation[currentTranslation].translate("+ row"));
        deleteWaypointButton.setText(translation[currentTranslation].translate("- row"));
        zoomInButton.setText(translation[currentTranslation].translate("+"));
        zoomOutButton.setText(translation[currentTranslation].translate("-"));
        zoomResetButton.setText(translation[currentTranslation].translate("recenter"));
        moveUpButton.setText(translation[currentTranslation].translate("move up"));
        moveDownButton.setText(translation[currentTranslation].translate("move down"));
        hiperGpsMenu.setText(translation[currentTranslation].translate("HiperGps"));
        aboutMenuItem.setText(translation[currentTranslation].translate("About HiperGps"));
        exitMenuItem.setText(translation[currentTranslation].translate("Exit"));
        fileMenu.setText(translation[currentTranslation].translate("File"));
        newProjectMenuItem.setText(translation[currentTranslation].translate("New Project"));
        openProjectMenuItem.setText(translation[currentTranslation].translate("Open Project Folder"));
        newProjectFromGpxMenuItem.setText(translation[currentTranslation].translate("New Project From GPX File"));
        closeProjectMenuItem.setText(translation[currentTranslation].translate("Close Project"));
        ingestMenu.setText(translation[currentTranslation].translate("Data"));
        ingestImageMenuItem.setText(translation[currentTranslation].translate("Ingest Image"));
        ingestAudioMenuItem.setText(translation[currentTranslation].translate("Ingest Audio"));
        deleteImageMenuItem.setText(translation[currentTranslation].translate("Delete Image Asset"));
        deleteAudioMenuItem.setText(translation[currentTranslation].translate("Delete Audio Asset"));
        importGpxWaypointsMenuItem.setText(translation[currentTranslation].translate("Import GPX Waypoints"));
        typeMenu.setText(translation[currentTranslation].translate("Type"));
        setFreeFormMenuItem.setText(translation[currentTranslation].translate("Set Freeform Tour"));
        setRouteMenuItem.setText(translation[currentTranslation].translate("Set Route Tour"));
        hiperGeoOnlineMenu.setText(translation[currentTranslation].translate("HiperGeoOnline"));
        uploadUpdateMenuItem.setText(translation[currentTranslation].translate("Upload/Update Project"));
        saveAsMenuItem.setText(translation[currentTranslation].translate("Save As..."));
        aboutHiperGeoOnlineMenuItem.setText(translation[currentTranslation].translate("About HiperGeoOnline"));
        statusLabelString = translation[currentTranslation].translate("Status: ");
    }

    /**
     * Imports waypoints from a Gpx file
     */
    public void importGpxWaypoints() {
        JFileChooser chooser = new JFileChooser(translation[currentTranslation].translate("Import GPX waypoints from file"));
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setDialogTitle(translation[currentTranslation].translate("Import GPX Waypoints"));
        chooser.setFileFilter(new TypeFilter(translation[currentTranslation].translate("GPX file"), ".gpx"));
        chooser.setCurrentDirectory(lastOpenDirectory);
        int val = chooser.showOpenDialog((java.awt.Component) this);
        if (val == JFileChooser.APPROVE_OPTION) {
            File inFile = chooser.getSelectedFile();
            if (inFile.isFile() && inFile.canRead()) {
                lastOpenDirectory = inFile.getParentFile();
                int typeTemp = type;
                Vector<GpxWaypoint> temp = readGpx(inFile);
                if (temp == null) {
                    return;
                }
                type = typeTemp;
                adding: for (int i = 0; i < temp.size(); i++) {
                    GpxWaypoint gpxPoint = temp.get(i);
                    for (int j = 0; j < wptVector.size(); j++) {
                        if (gpxPoint.getLatitude() == wptVector.get(j).getLatitude() && gpxPoint.getLongitude() == wptVector.get(j).getLongitude()) {
                            continue adding;
                        }
                    }
                    HiperWaypoint newHiperPoint = new HiperWaypoint(gpxPoint.getLatitude(), gpxPoint.getLongitude(), (float) gpxPoint.getElevation(), gpxPoint.getName(), 30);
                    wptVector.add(newHiperPoint);
                }
                wptTable.updateTable(wptVector);
                mapPanel.resetView();
            } else {
                showDialog(translation[currentTranslation].translate("Can not import from ") + inFile.getName(), translation[currentTranslation].translate("Not a Gpx"), JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void tableChanged(TableModelEvent tme) {
        mapPanel.setWayPointContainer(getContainer());
        mapPanel.resetViewNoScale();
        if (!closing) {
            writeGpx();
            writeJar();
            writeJad();
        }
        checkProject(currentProjectDir);
    }

    private void moveRowUp(int row) {
        if (row >= 1) {
            GpxWaypoint temp = wptVector.get(row);
            wptVector.removeElementAt(row);
            wptVector.insertElementAt(temp, row - 1);
            wptTable.updateTable(wptVector);
        } else if (row == 0) {
            showDialog(translation[currentTranslation].translate("Can't move first row up"), translation[currentTranslation].translate("No place to go"), JOptionPane.ERROR_MESSAGE);
        } else {
            showDialog(translation[currentTranslation].translate("No row was selected"), translation[currentTranslation].translate("No row selection"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void moveRowDown(int row) {
        if (row < 0) {
            showDialog(translation[currentTranslation].translate("No row was selected"), translation[currentTranslation].translate("No row selection"), JOptionPane.ERROR_MESSAGE);
        } else if (row <= wptVector.size() - 2) {
            GpxWaypoint temp = wptVector.get(row);
            wptVector.removeElementAt(row);
            wptVector.insertElementAt(temp, row + 1);
            wptTable.updateTable(wptVector);
        } else if (row == wptVector.size() - 1) {
            showDialog(translation[currentTranslation].translate("Can't move last row down"), translation[currentTranslation].translate("No place to go"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addRow() {
        HiperWaypoint wpt = new HiperWaypoint(0, 0, 0, "", 30);
        wpt.setName("wpt " + (wptVector.size() + 1));
        wptVector.add(wpt);
        wptTable.updateTable(wptVector);
        mapPanel.resetView();
    }

    private void removeRow(int row) {
        if (row >= 0) {
            wptVector.removeElementAt(row);
            wptTable.updateTable(wptVector);
        } else {
            showDialog(translation[currentTranslation].translate("No row was selected"), translation[currentTranslation].translate("No row selection"), JOptionPane.ERROR_MESSAGE);
        }
        mapPanel.resetView();
    }

    private void deleteFile(TypeFilter filter, String directory) {
        JFileChooser chooser = new JFileChooser(translation[currentTranslation].translate("Delete File"));
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setDialogTitle(translation[currentTranslation].translate("Delete File"));
        chooser.setCurrentDirectory(new File(currentProjectDir + directory));
        chooser.setFileFilter(filter);
        int val = chooser.showOpenDialog((java.awt.Component) this);
        if (val == JFileChooser.APPROVE_OPTION) {
            File inFile = chooser.getSelectedFile();
            if (inFile.getName().equals("default.wav") || inFile.getName().equals("default.png")) {
                showDialog(translation[currentTranslation].translate("Can not delete default asset: ") + inFile.getName() + translation[currentTranslation].translate(", but it can be replaced with ") + translation[currentTranslation].translate("Data -> Ingest ..."), translation[currentTranslation].translate("Can not delete"), JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            if (inFile.delete()) {
                if (directory.equals(WalkingtoolsInformation.AUDIODIR)) {
                    audioFiles.remove(inFile);
                } else if (directory.equals(WalkingtoolsInformation.IMAGEDIR)) {
                    imageFiles.remove(inFile);
                }
                showDialog(translation[currentTranslation].translate("Deleted asset ") + inFile.getName(), translation[currentTranslation].translate("OK"), JOptionPane.INFORMATION_MESSAGE);
            } else {
                showDialog(translation[currentTranslation].translate("Could not delete asset ") + inFile.getName(), translation[currentTranslation].translate("Error"), JOptionPane.ERROR_MESSAGE);
            }
        }
        wptTable.updateTable(wptVector);
    }

    private void ingestFile(TypeFilter filter, String directory) {
        JFileChooser chooser = new JFileChooser(translation[currentTranslation].translate("Ingest File"));
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setMultiSelectionEnabled(true);
        chooser.setDialogTitle(translation[currentTranslation].translate("Ingest File"));
        chooser.setCurrentDirectory(lastOpenDirectory);
        chooser.setFileFilter(filter);
        int val = chooser.showOpenDialog((java.awt.Component) this);
        if (val == JFileChooser.APPROVE_OPTION) {
            File[] inFiles = chooser.getSelectedFiles();
            lastOpenDirectory = new File(inFiles[0].getParent());
            for (int i = 0; i < inFiles.length; i++) {
                if (inFiles[i].canRead()) {
                    try {
                        File outFile = new File(currentProjectDir + directory + fileSeparator + inFiles[i].getName());
                        File result = Utilities.copyFile(inFiles[i], outFile, this);
                        if (directory.equals(WalkingtoolsInformation.AUDIODIR)) {
                            audioFiles.add(result);
                        } else if (directory.equals(WalkingtoolsInformation.IMAGEDIR)) {
                            imageFiles.add(result);
                        }
                    } catch (IOException ex) {
                        showDialog(translation[currentTranslation].translate("Could not ingest ") + inFiles[i].getName(), translation[currentTranslation].translate("Copy Error"), JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        }
        wptTable.updateTable(wptVector);
    }

    private class TypeFilter extends javax.swing.filechooser.FileFilter {

        private String extension = null;

        private String description = null;

        TypeFilter(String description, String extension) {
            this.extension = extension;
            this.description = description;
        }

        public boolean accept(File f) {
            if (f.isDirectory()) {
                return true;
            } else {
                try {
                    String name = f.getName();
                    String ext = name.substring(name.lastIndexOf("."));
                    if (ext.equals(extension) && !name.contains(WalkingtoolsInformation.MEDIAUUID)) {
                        return true;
                    } else {
                        return false;
                    }
                } catch (Exception e) {
                    return false;
                }
            }
        }

        @Override
        public String getDescription() {
            return description + ": " + extension;
        }
    }

    private void openProject() {
        enterClosedState();
        JFileChooser chooser = new JFileChooser(translation[currentTranslation].translate("Open Project"));
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle(translation[currentTranslation].translate("Open Project Folder"));
        chooser.setCurrentDirectory(lastOpenDirectory);
        int returnVal = chooser.showOpenDialog((java.awt.Component) this);
        if (returnVal != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File inFile = chooser.getSelectedFile();
        lastOpenDirectory = inFile.getParentFile();
        try {
            currentProjectDir = inFile.getCanonicalPath();
        } catch (IOException ex) {
            currentProjectDir = inFile.getAbsolutePath();
        }
        lastOpenDirectory = inFile;
        currentProjectName = currentProjectDir.substring(currentProjectDir.lastIndexOf(File.separatorChar) + 1);
        if (checkProject(currentProjectDir)) {
            try {
                jad = new Jad(new File(currentProjectDir + DEPLOYDIR + fileSeparator + currentProjectName + ".jad"));
                String id = jad.getAdditionalAttribute("hiperGeoId");
                if (id != null) {
                    hiperGeoId = id;
                }
            } catch (IOException ex) {
                enterClosedState();
                showDialog(translation[currentTranslation].translate("Severe: Could not open .jad File"), translation[currentTranslation].translate("Could not open .jad"), JOptionPane.ERROR_MESSAGE);
                return;
            }
            File temp = new File(currentProjectDir + WalkingtoolsInformation.IMAGEDIR);
            File[] files = temp.listFiles(new FilenameFilter() {

                public boolean accept(File file, String name) {
                    if (name.endsWith(".png")) {
                        return true;
                    } else {
                        return false;
                    }
                }
            });
            for (int i = 0; i < files.length; i++) {
                imageFiles.add(files[i]);
            }
            temp = new File(currentProjectDir + WalkingtoolsInformation.AUDIODIR);
            files = temp.listFiles(new FilenameFilter() {

                public boolean accept(File file, String name) {
                    if (name.endsWith(".wav")) {
                        return true;
                    } else {
                        return false;
                    }
                }
            });
            for (int i = 0; i < files.length; i++) {
                audioFiles.add(files[i]);
            }
            wptVector = readHiperGpx(new File(currentProjectDir + WalkingtoolsInformation.GPXDIR + "/hipergps.gpx"));
            wptTable.updateTable(wptVector);
            GpsTypeContainer container = getContainer();
            setType(container.getGpsType());
            mapPanel.setWayPointContainer(container);
            mapPanel.resetView();
            enterEditingState();
        } else {
            showDialog(translation[currentTranslation].translate("Severe: Could not open project folder"), translation[currentTranslation].translate("Could not open project"), JOptionPane.ERROR_MESSAGE);
            enterClosedState();
        }
    }

    private Vector<GpxWaypoint> readHiperGpx(File inFile) {
        Vector<GpxWaypoint> vecy = null;
        HiperGpxParser gpx = null;
        Vector<GpxExtensionParser> extParsers = new Vector<GpxExtensionParser>(1);
        extParsers.add(new HiperExtensionParser());
        try {
            gpx = new HiperGpxParser(false, extParsers);
            gpx.setGpxFile(inFile);
            GpsTypeContainer container = gpx.getGpsTypeContainer();
            type = container.getGpsType();
            Coordinates[] coords = container.getCoordinates();
            vecy = new Vector<GpxWaypoint>();
            for (int i = 0; i < coords.length; i++) {
                vecy.add((HiperWaypoint) coords[i]);
            }
        } catch (SAXException ex) {
            showDialog(translation[currentTranslation].translate("Severe: Could not Parse GPX File"), "SAXException", JOptionPane.ERROR_MESSAGE);
        } catch (ParserConfigurationException ex) {
            showDialog(translation[currentTranslation].translate("Severe: Could not Parse GPX File"), "ParserConfigurationException", JOptionPane.ERROR_MESSAGE);
        } catch (GpxFileTypeException e) {
            if (gpx.getGpxType() == GpsTypeContainer.TRACKLOG) {
                showDialog(translation[currentTranslation].translate("Presently, HiperGps does not work with Track Log data"), translation[currentTranslation].translate("Can not open Track Log data"), JOptionPane.ERROR_MESSAGE);
            } else {
                showDialog(translation[currentTranslation].translate("Not a Gpx"), translation[currentTranslation].translate("Not a Gpx"), JOptionPane.ERROR_MESSAGE);
            }
            enterClosedState();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return vecy;
    }

    private Vector<GpxWaypoint> readGpx(File inFile) {
        Vector<GpxWaypoint> vecy = null;
        GpxParser gpx = null;
        try {
            gpx = new GpxParser(false);
            gpx.setGpxFile(inFile);
            GpsTypeContainer container = gpx.getGpsTypeContainer();
            int gpxType = container.getGpsType();
            if (gpxType != GpsTypeContainer.WAYPOINTS && gpxType != GpsTypeContainer.ROUTE) {
                throw new GpxFileTypeException("Not a WAYPOINTS or ROUTE GPX");
            }
            Coordinates[] coords = container.getCoordinates();
            vecy = new Vector<GpxWaypoint>();
            for (int i = 0; i < coords.length; i++) {
                vecy.add((GpxWaypoint) coords[i]);
            }
        } catch (SAXException ex) {
            showDialog(translation[currentTranslation].translate("Severe: Could not Parse GPX File"), "SAXException", JOptionPane.ERROR_MESSAGE);
        } catch (ParserConfigurationException ex) {
            showDialog(translation[currentTranslation].translate("Severe: Could not Parse GPX File"), "ParserConfigurationException", JOptionPane.ERROR_MESSAGE);
        } catch (GpxFileTypeException e) {
            if (gpx.getGpxType() == GpsTypeContainer.TRACKLOG) {
                showDialog(translation[currentTranslation].translate("Presently, HiperGps does not work with Track Log data"), translation[currentTranslation].translate("Can not open Track Log data"), JOptionPane.ERROR_MESSAGE);
            } else {
                showDialog(translation[currentTranslation].translate("Not a Gpx"), translation[currentTranslation].translate("Not a Gpx"), JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return vecy;
    }

    private void newEmptyProject() {
        enterClosedState();
        wptVector = new Vector<GpxWaypoint>();
        newProject();
    }

    private void newProjectFromGpx() {
        enterClosedState();
        JFileChooser chooser = new JFileChooser(translation[currentTranslation].translate("Open New Project From Gpx"));
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setDialogTitle(translation[currentTranslation].translate("Select a GPX file to import as new"));
        chooser.setCurrentDirectory(hiperGpsHome);
        chooser.setFileFilter(new TypeFilter(translation[currentTranslation].translate("GPX file"), ".gpx"));
        int returnVal = chooser.showOpenDialog((java.awt.Component) this);
        if (returnVal != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File inFile = chooser.getSelectedFile();
        wptVector = readGpx(inFile);
        if (wptVector == null) {
            return;
        }
        Vector<GpxWaypoint> temp = new Vector<GpxWaypoint>(wptVector.size());
        for (int i = 0; i < wptVector.size(); i++) {
            GpxWaypoint gpxPoint = wptVector.get(i);
            HiperWaypoint newHiperPoint = new HiperWaypoint(gpxPoint.getLatitude(), gpxPoint.getLongitude(), (float) gpxPoint.getElevation(), gpxPoint.getName(), 30);
            temp.add(newHiperPoint);
        }
        wptVector = temp;
        if (!newProject()) {
            return;
        }
        try {
            jad = new Jad(new File(currentProjectDir + DEPLOYDIR + fileSeparator + currentProjectName + ".jad"));
        } catch (IOException ex) {
            enterClosedState();
            showDialog(translation[currentTranslation].translate("Severe: Could not open .jad File"), translation[currentTranslation].translate("Could not open .jad"), JOptionPane.ERROR_MESSAGE);
            return;
        }
        wptTable.updateTable(wptVector);
        GpsTypeContainer container = getContainer();
        setType(container.getGpsType());
        mapPanel.setWayPointContainer(container);
        mapPanel.resetView();
        enterEditingState();
    }

    private boolean newProject() {
        JFileChooser chooser = new JFileChooser(translation[currentTranslation].translate("New Project"));
        chooser.setFileSelectionMode(JFileChooser.SAVE_DIALOG);
        chooser.setDialogTitle(translation[currentTranslation].translate("Make New Project Folder"));
        chooser.setCurrentDirectory(hiperGpsHome);
        int returnVal = chooser.showSaveDialog((java.awt.Component) this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (file.getName().indexOf(" ") != -1) {
                showDialog(translation[currentTranslation].translate("Spaces not allowed in Project Name"), translation[currentTranslation].translate("Spaces not allowed"), JOptionPane.ERROR_MESSAGE);
                enterClosedState();
                return false;
            }
            buildProjectDirectory(file);
        } else {
            enterClosedState();
            return false;
        }
        return true;
    }

    private void buildProjectDirectory(File inFile) {
        if (inFile.exists()) {
            showDialog(translation[currentTranslation].translate("Directory Exists, can not create new project here."), translation[currentTranslation].translate("Directory Exists"), JOptionPane.ERROR_MESSAGE);
            enterClosedState();
            return;
        }
        try {
            try {
                currentProjectDir = inFile.getCanonicalPath();
            } catch (IOException ex) {
                currentProjectDir = inFile.getAbsolutePath();
            }
            currentProjectName = currentProjectDir.substring(currentProjectDir.lastIndexOf(File.separatorChar) + 1);
            File file = new File(currentProjectDir + DEPLOYDIR);
            file.mkdirs();
            file = new File(currentProjectDir + WalkingtoolsInformation.GPXDIR);
            file.mkdirs();
            file = new File(currentProjectDir + WalkingtoolsInformation.IMAGEDIR);
            file.mkdirs();
            file = new File(currentProjectDir + WalkingtoolsInformation.AUDIODIR);
            file.mkdirs();
            file = new File(currentProjectDir + SAMPLEDIR);
            file.mkdirs();
            jad = new Jad();
            jad.setMIDletJarSize(jarSize);
            jad.setMIDletJarURL(currentProjectName + ".jar");
            jad.setMIDletName(currentProjectName);
            jad.setMIDletVendor("Walking Tools HiperGps Project: www.walkingtools.net");
            jad.setMIDletVersion(WalkingtoolsInformation.version);
            jad.setMicroEditionProfile("MIDP-2.0");
            jad.setMicroEditionConfiguration("CLDC-1.1");
            jad.addMIDlet("HiperGps", "/img/icon_" + WalkingtoolsInformation.MEDIAUUID + ".png", WalkingtoolsInformation.HIPERWAYPOINTMIDLET);
            jad.addAdditionalAttribute("gpxRoute", "hipergps.gpx");
            jad.addAdditionalAttribute("gpxWaypoints", "hipergps.gpx");
            jad.addAdditionalAttribute("language", translation[currentTranslation].getCode());
            Utilities.copyFile("res/wtj2me.jar", currentProjectDir + "/res/wtj2me.jar", this);
            Utilities.copyFile("res/img/default" + "_" + WalkingtoolsInformation.MEDIAUUID + ".png", currentProjectDir + WalkingtoolsInformation.IMAGEDIR + "/default.png", this);
            Utilities.copyFile("res/audio/default" + "_" + WalkingtoolsInformation.MEDIAUUID + ".wav", currentProjectDir + WalkingtoolsInformation.AUDIODIR + "/default.wav", this);
            Utilities.copyFile("res/img/icon_" + WalkingtoolsInformation.MEDIAUUID + ".png", currentProjectDir + WalkingtoolsInformation.IMAGEDIR + "/icon_" + WalkingtoolsInformation.MEDIAUUID + ".png", this);
            Utilities.copyFile("res/img/loaderIcon_" + WalkingtoolsInformation.MEDIAUUID + ".png", currentProjectDir + WalkingtoolsInformation.IMAGEDIR + "/loaderIcon_" + WalkingtoolsInformation.MEDIAUUID + ".png", this);
            Utilities.copyFile("res/img/mygps_" + WalkingtoolsInformation.MEDIAUUID + ".png", currentProjectDir + WalkingtoolsInformation.IMAGEDIR + "/mygps_" + WalkingtoolsInformation.MEDIAUUID + ".png", this);
            String[] sampleNames = { "deadb", "eastcrk", "elcap", "goose", "copacobana", "joaoquim", "praia", "soldado" };
            for (int i = 0; i < sampleNames.length; i++) {
                Utilities.copyFile("res/img/" + sampleNames[i] + "_" + WalkingtoolsInformation.MEDIAUUID + ".png", currentProjectDir + SAMPLEDIR + fileSeparator + sampleNames[i] + ".png", this);
                Utilities.copyFile("res/audio/" + sampleNames[i] + "_" + WalkingtoolsInformation.MEDIAUUID + ".wav", currentProjectDir + SAMPLEDIR + fileSeparator + sampleNames[i] + ".wav", this);
            }
            Utilities.copyFile("res/gpx/rioWaypoints.gpx", currentProjectDir + SAMPLEDIR + "/rioWaypoints.gpx", this);
            writeGpx();
            writeJar();
            writeJad();
            checkProject(currentProjectDir);
            wptTable.updateTable(wptVector);
            GpsTypeContainer container = getContainer();
            setType(container.getGpsType());
            mapPanel.setWayPointContainer(container);
            mapPanel.resetView();
            enterEditingState();
        } catch (IOException ex) {
            ex.printStackTrace();
            enterClosedState();
            showDialog(translation[currentTranslation].translate("Can not create a project under path"), translation[currentTranslation].translate("Could not create new project"), JOptionPane.ERROR_MESSAGE);
            enterClosedState();
        }
    }

    public void communicationRecieved(String message) {
        if (message.startsWith("version=")) {
            String serverVersion = message.substring(message.indexOf("=") + 1);
            int[] serverVersionArray = Utilities.parseVersionString(serverVersion);
            int[] thisVersion = Utilities.parseVersionString(WalkingtoolsInformation.version);
            for (int i = 0; i < serverVersionArray.length; i++) {
                if (serverVersionArray[i] != thisVersion[i]) {
                    String errorMessage = "Versions must match: server:" + serverVersion + " client:" + WalkingtoolsInformation.version;
                    showDialog(errorMessage, "Versions do not match", JOptionPane.ERROR_MESSAGE);
                    statusLabel.setText(statusLabelString + errorMessage);
                    return;
                }
            }
            statusLabel.setText(statusLabelString + "Version check OK");
            if (projectStatusOk) {
                statusLabel.setText(statusLabelString + "Version check OK");
            } else {
                statusLabel.setText(statusLabelString + "Project must be in green or finsihed state to upload");
                return;
            }
            verifyLogin();
        } else if (message.equals("LOGINFAIL")) {
            statusLabel.setText(statusLabelString + "Login Failed");
        } else if (message.startsWith("jsessionid=")) {
            jsessionId = message.substring(message.indexOf("=") + 1);
            statusLabel.setText(statusLabelString + "Login OK");
            verifyProject();
        } else if (message.startsWith("hipergeoid=")) {
            hiperGeoId = message.substring(message.indexOf("=") + 1);
            jad.addAdditionalAttribute("hiperGeoId", hiperGeoId);
            writeJar();
            writeJad();
            statusLabel.setText(statusLabelString + "Uploading new Project");
            uploadJadFile();
        } else if (message.equals("PROJECTOK")) {
            statusLabel.setText(statusLabelString + "Updating Existing Project");
            uploadJadFile();
        } else if (message.equals("PROJECTFAIL")) {
            statusLabel.setText(statusLabelString + "User does not own project, can not continue.");
        } else if (message.equals("JADOK")) {
            statusLabel.setText(statusLabelString + ".jad file uploaded");
            uploadJarFile();
        } else if (message.equals("JADFAIL")) {
            statusLabel.setText(statusLabelString + ".jad failed to upload");
        } else if (message.equals("JAROK")) {
            statusLabel.setText(statusLabelString + ".jar file uploaded");
            confirmUpload();
        } else if (message.equals("UPLOADOK")) {
            statusLabel.setText(statusLabelString + "Upload was successful. The project is online.");
        } else if (message.equals("UPLOADFAIL")) {
            statusLabel.setText(statusLabelString + "Upload failed, try again or login to website.");
        } else if (message.equals("SERVERFAIL") || message.equals("")) {
            statusLabel.setText(statusLabelString + "The server is experiencing a problem, please try again later.");
        } else {
            statusLabel.setText(statusLabelString + "Error: " + message);
        }
    }

    private void getUserInfo() {
        if (user != null) {
            new LoginDialog(this, user.getEmail());
        } else {
            new LoginDialog(this);
        }
    }

    public void userLogin(User user) {
        this.user = user;
        Thread t = new Thread(this);
        t.start();
    }

    public void run() {
        enterUploadState();
        checkVersion();
        try {
            Thread.sleep(10000);
        } catch (InterruptedException ex) {
            if (currentProjectDir != null) {
                enterEditingState();
                statusLabel.setText(statusLabelString);
            }
        }
        if (currentProjectDir != null) {
            enterEditingState();
            statusLabel.setText(statusLabelString);
        }
    }

    private void checkVersion() {
        communicator.checkVersion();
    }

    private void verifyLogin() {
        communicator.verifyLogin(user.getEmail(), user.getPassword());
    }

    private void verifyProject() {
        hiperGeoId = jad.getAdditionalAttribute("hiperGeoId");
        communicator.verifyProject(jsessionId, hiperGeoId, currentProjectName);
    }

    private void uploadJadFile() {
        File jadFile = new File(currentProjectDir + DEPLOYDIR + fileSeparator + currentProjectName + ".jad");
        if (jadFile.exists()) {
            communicator.uploadJad(jsessionId, jadFile);
        }
    }

    private void uploadJarFile() {
        File gpx = new File(currentProjectDir + DEPLOYDIR + fileSeparator + currentProjectName + ".jar");
        if (gpx.exists()) {
            communicator.uploadJar(jsessionId, gpx);
        }
    }

    public void confirmUpload() {
        communicator.confirmUpload(jsessionId, hiperGeoId, currentProjectName);
    }

    private void showDialog(String message, String title, int type) {
        JOptionPane.showMessageDialog(this, message, title, type);
    }

    private void writeJad() {
        if (currentProjectDir == null) {
            return;
        }
        jad.clearMidlets();
        String name = currentProjectDir.substring(currentProjectDir.lastIndexOf(File.separatorChar) + 1);
        jad.setMIDletName(name);
        if (type == GpsTypeContainer.ROUTE) {
            jad.addMIDlet(name, "/img/icon_" + WalkingtoolsInformation.MEDIAUUID + ".png", WalkingtoolsInformation.HIPERROUTEMIDLET);
        } else {
            jad.addMIDlet(name, "/img/icon_" + WalkingtoolsInformation.MEDIAUUID + ".png", WalkingtoolsInformation.HIPERWAYPOINTMIDLET);
        }
        jad.addMIDlet("Gps", "/img/mygps_" + WalkingtoolsInformation.MEDIAUUID + ".png", WalkingtoolsInformation.PRODUCTIONSTUDIOMIDLET);
        jad.addMIDlet("Search", "/img/loaderIcon_" + WalkingtoolsInformation.MEDIAUUID + ".png", "net.walkingtools.server.javame.MIDlet.HiperGeoSearchMIDlet");
        File theJar = new File(currentProjectDir + DEPLOYDIR + fileSeparator + currentProjectName + ".jar");
        jarSize = (int) theJar.length();
        jad.setMIDletJarSize(jarSize);
        jad.setAdditionalAttribute("language", translation[currentTranslation].getCode());
        try {
            File temp = new File(currentProjectDir + DEPLOYDIR + fileSeparator + name + ".jad");
            if (!temp.exists()) {
                temp.createNewFile();
            } else if (!temp.canWrite()) {
                throw new IOException("Can't write to : " + temp.getCanonicalPath());
            }
            FileOutputStream jadFile = new FileOutputStream(temp);
            String fileData = jad.formatJad();
            jadFile.write(fileData.getBytes());
            jadFile.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            showDialog(translation[currentTranslation].translate("Can not write to jad"), translation[currentTranslation].translate("Jad Error"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void writeGpx() {
        if (currentProjectDir == null) {
            return;
        }
        try {
            File temp = new File(currentProjectDir + WalkingtoolsInformation.GPXDIR + "/hipergps.gpx");
            if (!temp.exists()) {
                temp.createNewFile();
            } else if (!temp.canWrite()) {
                throw new IOException("Can't write to : " + temp.getAbsolutePath());
            }
            FileOutputStream gpxFile = new FileOutputStream(temp);
            GpsTypeContainer container = getContainer();
            container.setGpsDataFormatter(formatter);
            String fileData = container.toString();
            gpxFile.write(fileData.getBytes());
            gpxFile.close();
        } catch (IOException ex) {
            showDialog(translation[currentTranslation].translate("Can not write to gpx"), translation[currentTranslation].translate("Gpx Error"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void writeJar() {
        try {
            File outJar = new File(currentProjectDir + DEPLOYDIR + fileSeparator + currentProjectName + ".jar");
            jarSize = (int) outJar.length();
            File tempJar = File.createTempFile("hipergps" + currentProjectName, ".jar");
            tempJar.deleteOnExit();
            File preJar = new File(currentProjectDir + "/res/wtj2me.jar");
            JarInputStream preJarInStream = new JarInputStream(new FileInputStream(preJar));
            Manifest mFest = preJarInStream.getManifest();
            java.util.jar.Attributes atts = mFest.getMainAttributes();
            if (hiperGeoId != null) {
                atts.putValue("hiperGeoId", hiperGeoId);
            }
            jad.updateAttributes(atts);
            JarOutputStream jarOutStream = new JarOutputStream(new FileOutputStream(tempJar), mFest);
            byte[] buffer = new byte[WalkingtoolsInformation.BUFFERSIZE];
            JarEntry jarEntry = null;
            while ((jarEntry = preJarInStream.getNextJarEntry()) != null) {
                if (jarEntry.getName().contains("net/") || jarEntry.getName().contains("org/")) {
                    try {
                        jarOutStream.putNextEntry(jarEntry);
                    } catch (ZipException ze) {
                        continue;
                    }
                    int read;
                    while ((read = preJarInStream.read(buffer)) != -1) {
                        jarOutStream.write(buffer, 0, read);
                    }
                    jarOutStream.closeEntry();
                }
            }
            File[] icons = { new File(currentProjectDir + WalkingtoolsInformation.IMAGEDIR + fileSeparator + "icon_" + WalkingtoolsInformation.MEDIAUUID + ".png"), new File(currentProjectDir + WalkingtoolsInformation.IMAGEDIR + fileSeparator + "loaderIcon_" + WalkingtoolsInformation.MEDIAUUID + ".png"), new File(currentProjectDir + WalkingtoolsInformation.IMAGEDIR + fileSeparator + "mygps_" + WalkingtoolsInformation.MEDIAUUID + ".png") };
            for (int i = 0; i < icons.length; i++) {
                jarEntry = new JarEntry("img/" + icons[i].getName());
                try {
                    jarOutStream.putNextEntry(jarEntry);
                } catch (ZipException ze) {
                    continue;
                }
                FileInputStream in = new FileInputStream(icons[i]);
                while (true) {
                    int read = in.read(buffer, 0, buffer.length);
                    if (read <= 0) {
                        break;
                    }
                    jarOutStream.write(buffer, 0, read);
                }
                in.close();
            }
            for (int i = 0; i < imageFiles.size(); i++) {
                jarEntry = new JarEntry("img/" + imageFiles.get(i).getName());
                try {
                    jarOutStream.putNextEntry(jarEntry);
                } catch (ZipException ze) {
                    continue;
                }
                FileInputStream in = new FileInputStream(imageFiles.get(i));
                while (true) {
                    int read = in.read(buffer, 0, buffer.length);
                    if (read <= 0) {
                        break;
                    }
                    jarOutStream.write(buffer, 0, read);
                }
                in.close();
            }
            for (int i = 0; i < audioFiles.size(); i++) {
                jarEntry = new JarEntry("audio/" + audioFiles.get(i).getName());
                try {
                    jarOutStream.putNextEntry(jarEntry);
                } catch (ZipException ze) {
                    continue;
                }
                FileInputStream in = new FileInputStream(audioFiles.get(i));
                while (true) {
                    int read = in.read(buffer, 0, buffer.length);
                    if (read <= 0) {
                        break;
                    }
                    jarOutStream.write(buffer, 0, read);
                }
                in.close();
            }
            File gpx = new File(currentProjectDir + WalkingtoolsInformation.GPXDIR + "/hipergps.gpx");
            jarEntry = new JarEntry("gpx/" + gpx.getName());
            jarOutStream.putNextEntry(jarEntry);
            FileInputStream in = new FileInputStream(gpx);
            while (true) {
                int read = in.read(buffer, 0, buffer.length);
                if (read <= 0) {
                    break;
                }
                jarOutStream.write(buffer, 0, read);
            }
            in.close();
            jarOutStream.flush();
            jarOutStream.close();
            jarSize = (int) tempJar.length();
            preJarInStream = new JarInputStream(new FileInputStream(tempJar));
            mFest = preJarInStream.getManifest();
            atts = mFest.getMainAttributes();
            atts.putValue("MIDlet-Jar-Size", "" + jarSize + 1);
            jarOutStream = new JarOutputStream(new FileOutputStream(outJar), mFest);
            while ((jarEntry = preJarInStream.getNextJarEntry()) != null) {
                try {
                    jarOutStream.putNextEntry(jarEntry);
                } catch (ZipException ze) {
                    continue;
                }
                int read;
                while ((read = preJarInStream.read(buffer)) != -1) {
                    jarOutStream.write(buffer, 0, read);
                }
                jarOutStream.closeEntry();
            }
            jarOutStream.flush();
            preJarInStream.close();
            jarOutStream.close();
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private GpsTypeContainer getContainer() {
        GpsTypeContainer container = null;
        if (type == GpsTypeContainer.WAYPOINTS) {
            container = WaypointContainer.makeWaypointContainer(wptVector);
        } else if (type == GpsTypeContainer.ROUTE) {
            if (wptVector.size() <= 0) {
                container = GpxRoute.makeRoute(wptVector, "EMPTY");
            } else if (wptVector.size() == 1) {
                container = GpxRoute.makeRoute(wptVector, "ONE POINT ROUTE");
            } else {
                container = GpxRoute.makeRoute(wptVector, wptVector.firstElement().getName() + "-" + wptVector.lastElement().getName());
            }
        } else if (type == GpsTypeContainer.EMPTY) {
            container = new WaypointContainer();
            setType(GpsTypeContainer.EMPTY);
        }
        container.setAuthor("The Walking Tools HiperGps Project");
        container.setLink("Home of WalkingTools", "text/html", WalkingtoolsInformation.projectUrl);
        return container;
    }

    private void setType(int type) {
        this.type = type;
        if (this.type == GpsTypeContainer.WAYPOINTS) {
            moveUpButton.setEnabled(false);
            moveDownButton.setEnabled(false);
            typeMenu.remove(setFreeFormMenuItem);
            typeMenu.add(setRouteMenuItem);
        } else if (this.type == GpsTypeContainer.ROUTE) {
            moveUpButton.setEnabled(true);
            moveDownButton.setEnabled(true);
            typeMenu.add(setFreeFormMenuItem);
            typeMenu.remove(setRouteMenuItem);
        } else {
            return;
        }
        wptTable.updateTable(wptVector);
    }

    private void copyProject() {
        if (currentProjectDir != null) {
            JFileChooser chooser = new JFileChooser(translation[currentTranslation].translate("Save as"));
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            chooser.setDialogTitle(translation[currentTranslation].translate("Save as"));
            chooser.setCurrentDirectory(hiperGpsHome);
            int val = chooser.showOpenDialog((java.awt.Component) this);
            String oldProjectName = null;
            if (val == JFileChooser.APPROVE_OPTION) {
                File outDir = chooser.getSelectedFile();
                try {
                    Utilities.copyDirectory(new File(currentProjectDir), outDir, this);
                } catch (IOException e) {
                    showDialog(translation[currentTranslation].translate("Could not copy ") + outDir.getName(), translation[currentTranslation].translate("Copy Error"), JOptionPane.ERROR_MESSAGE);
                    return;
                }
                oldProjectName = currentProjectName;
                currentProjectDir = outDir.getAbsolutePath();
                currentProjectName = outDir.getName();
                lastOpenDirectory = outDir;
            }
            hiperGeoId = null;
            jad.removeAdditionalAttribute("hiperGeoId");
            writeJar();
            writeJad();
            File file = new File(currentProjectDir + DEPLOYDIR + fileSeparator + oldProjectName + ".jad");
            file.delete();
            file = new File(currentProjectDir + DEPLOYDIR + fileSeparator + oldProjectName + ".jar");
            file.delete();
            setTitle(translation[currentTranslation].translate("HiperGps Project Manager") + ": " + currentProjectName);
        }
    }

    private boolean checkProject(String projectFolder) {
        File file = new File(projectFolder + DEPLOYDIR);
        if (!file.canRead()) {
            buildStatusPanel.setStopSign();
            return false;
        }
        file = new File(projectFolder + WalkingtoolsInformation.GPXDIR);
        if (!file.canRead()) {
            buildStatusPanel.setStopSign();
            return false;
        }
        file = new File(projectFolder + WalkingtoolsInformation.IMAGEDIR);
        if (!file.canRead()) {
            buildStatusPanel.setStopSign();
            return false;
        }
        file = new File(projectFolder + WalkingtoolsInformation.AUDIODIR);
        if (!file.canRead()) {
            buildStatusPanel.setStopSign();
            return false;
        }
        file = new File(projectFolder + WalkingtoolsInformation.GPXDIR + "/hipergps.gpx");
        if (!file.canRead()) {
            buildStatusPanel.setStopSign();
            return false;
        }
        file = new File(projectFolder + DEPLOYDIR + fileSeparator + currentProjectName + ".jad");
        if (!file.canRead()) {
            buildStatusPanel.setStopSign();
            return false;
        }
        file = new File(projectFolder + WalkingtoolsInformation.IMAGEDIR + fileSeparator + "icon_" + WalkingtoolsInformation.MEDIAUUID + ".png");
        if (!file.canRead()) {
            buildStatusPanel.setStopSign();
            System.out.println(projectFolder + WalkingtoolsInformation.IMAGEDIR + fileSeparator + "icon_" + WalkingtoolsInformation.MEDIAUUID + ".png");
            return false;
        }
        file = new File(projectFolder + WalkingtoolsInformation.IMAGEDIR + fileSeparator + "loaderIcon_" + WalkingtoolsInformation.MEDIAUUID + ".png");
        if (!file.canRead()) {
            buildStatusPanel.setStopSign();
            System.out.println(projectFolder + WalkingtoolsInformation.IMAGEDIR + fileSeparator + "loaderIcon_" + WalkingtoolsInformation.MEDIAUUID + ".png");
            return false;
        }
        file = new File(projectFolder + WalkingtoolsInformation.IMAGEDIR + fileSeparator + "mygps_" + WalkingtoolsInformation.MEDIAUUID + ".png");
        if (!file.canRead()) {
            buildStatusPanel.setStopSign();
            return false;
        }
        file = new File(currentProjectDir + DEPLOYDIR + fileSeparator + currentProjectName + ".jar");
        if (!file.canRead()) {
            buildStatusPanel.setStopSign();
            return false;
        } else {
            jarSize = (int) file.length();
        }
        if (jad != null) {
            if (!jad.validate()) {
                buildStatusPanel.setStopSign();
                return false;
            }
        }
        boolean allOk = true;
        for (int i = 0; i < wptVector.size(); i++) {
            HiperWaypoint ghw = ((HiperWaypoint) wptVector.get(i));
            if (ghw.getImageType().getFileName() == null || ghw.getAudioType().getFileName() == null || ghw.getRadius() <= 0) {
                allOk = false;
            }
        }
        if (wptVector.size() == 0) {
            allOk = false;
        }
        if (allOk) {
            buildStatusPanel.setGoSign();
            projectStatusOk = allOk;
        } else {
            buildStatusPanel.setWarnSign();
        }
        return true;
    }

    private void enterEditingState() {
        mapPanel.setEnabled(true);
        addWaypointButton.setEnabled(true);
        deleteWaypointButton.setEnabled(true);
        setType(type);
        zoomInButton.setEnabled(true);
        zoomOutButton.setEnabled(true);
        zoomResetButton.setEnabled(true);
        closeProjectMenuItem.setEnabled(true);
        ingestImageMenuItem.setEnabled(true);
        ingestAudioMenuItem.setEnabled(true);
        deleteImageMenuItem.setEnabled(true);
        deleteAudioMenuItem.setEnabled(true);
        tableModel.addTableModelListener(this);
        setFreeFormMenuItem.setEnabled(true);
        setRouteMenuItem.setEnabled(true);
        importGpxWaypointsMenuItem.setEnabled(true);
        saveAsMenuItem.setEnabled(true);
        uploadUpdateMenuItem.setEnabled(true);
        newProjectMenuItem.setEnabled(true);
        openProjectMenuItem.setEnabled(true);
        newProjectFromGpxMenuItem.setEnabled(true);
        addMouseMotionListener(mapPanel);
        addMouseListener(mapPanel);
        wptTable.setEnabled(true);
        checkProject(currentProjectDir);
        setTitle(translation[currentTranslation].translate("HiperGps Project Manager") + ": " + currentProjectName);
    }

    private void enterUploadState() {
        wptTable.setEnabled(false);
        removeMouseMotionListener(mapPanel);
        removeMouseListener(mapPanel);
        mapPanel.setEnabled(false);
        addWaypointButton.setEnabled(false);
        deleteWaypointButton.setEnabled(false);
        zoomInButton.setEnabled(false);
        zoomOutButton.setEnabled(false);
        zoomResetButton.setEnabled(false);
        moveUpButton.setEnabled(false);
        moveDownButton.setEnabled(false);
        newProjectMenuItem.setEnabled(false);
        openProjectMenuItem.setEnabled(false);
        newProjectFromGpxMenuItem.setEnabled(false);
        closeProjectMenuItem.setEnabled(false);
        ingestImageMenuItem.setEnabled(false);
        ingestAudioMenuItem.setEnabled(false);
        deleteImageMenuItem.setEnabled(false);
        deleteAudioMenuItem.setEnabled(false);
        setFreeFormMenuItem.setEnabled(false);
        setRouteMenuItem.setEnabled(false);
        saveAsMenuItem.setEnabled(false);
        uploadUpdateMenuItem.setEnabled(false);
        importGpxWaypointsMenuItem.setEnabled(false);
        typeMenu.add(setFreeFormMenuItem);
        typeMenu.add(setRouteMenuItem);
    }

    private void enterClosedState() {
        statusLabel.setText(statusLabelString);
        closing = true;
        tableModel.removeTableModelListener(this);
        wptTable.setEnabled(false);
        if (wptVector != null) {
            wptVector.clear();
            wptTable.updateTable(wptVector);
        }
        mapPanel.setEnabled(false);
        mapPanel.clear();
        currentProjectDir = null;
        currentProjectName = null;
        lastOpenDirectory = hiperGpsHome;
        jad = null;
        buildStatusPanel.setStopSign();
        addWaypointButton.setEnabled(false);
        deleteWaypointButton.setEnabled(false);
        zoomInButton.setEnabled(false);
        zoomOutButton.setEnabled(false);
        zoomResetButton.setEnabled(false);
        moveUpButton.setEnabled(false);
        moveDownButton.setEnabled(false);
        closeProjectMenuItem.setEnabled(false);
        ingestImageMenuItem.setEnabled(false);
        ingestAudioMenuItem.setEnabled(false);
        deleteImageMenuItem.setEnabled(false);
        deleteAudioMenuItem.setEnabled(false);
        setFreeFormMenuItem.setEnabled(false);
        setRouteMenuItem.setEnabled(false);
        saveAsMenuItem.setEnabled(false);
        uploadUpdateMenuItem.setEnabled(false);
        importGpxWaypointsMenuItem.setEnabled(false);
        typeMenu.add(setFreeFormMenuItem);
        typeMenu.add(setRouteMenuItem);
        removeMouseMotionListener(mapPanel);
        removeMouseListener(mapPanel);
        closing = false;
        setTitle(translation[currentTranslation].translate("HiperGps Project Manager"));
    }

    /**
     * Returns the current project directory
     * @return the current project directory
     */
    protected String getCurrentProjectDir() {
        return currentProjectDir;
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    private void initComponents() {
        addWaypointButton = new javax.swing.JButton();
        deleteWaypointButton = new javax.swing.JButton();
        wapointsScrollPane = new javax.swing.JScrollPane();
        zoomInButton = new javax.swing.JButton();
        zoomOutButton = new javax.swing.JButton();
        zoomResetButton = new javax.swing.JButton();
        titlePanel = new net.walkingtools.j2se.editor.TitlePanel();
        buildStatusPanel = new net.walkingtools.j2se.editor.BuildStatusPanel();
        moveUpButton = new javax.swing.JButton();
        moveDownButton = new javax.swing.JButton();
        mapPanel = new net.walkingtools.j2se.editor.HiperMapPanel();
        statusLabel = new javax.swing.JLabel();
        menuBar = new javax.swing.JMenuBar();
        hiperGpsMenu = new javax.swing.JMenu();
        aboutMenuItem = new javax.swing.JMenuItem();
        exitMenuItem = new javax.swing.JMenuItem();
        fileMenu = new javax.swing.JMenu();
        newProjectMenuItem = new javax.swing.JMenuItem();
        openProjectMenuItem = new javax.swing.JMenuItem();
        newProjectFromGpxMenuItem = new javax.swing.JMenuItem();
        saveAsMenuItem = new javax.swing.JMenuItem();
        closeProjectMenuItem = new javax.swing.JMenuItem();
        ingestMenu = new javax.swing.JMenu();
        ingestImageMenuItem = new javax.swing.JMenuItem();
        ingestAudioMenuItem = new javax.swing.JMenuItem();
        deleteImageMenuItem = new javax.swing.JMenuItem();
        deleteAudioMenuItem = new javax.swing.JMenuItem();
        importGpxWaypointsMenuItem = new javax.swing.JMenuItem();
        typeMenu = new javax.swing.JMenu();
        setFreeFormMenuItem = new javax.swing.JMenuItem();
        setRouteMenuItem = new javax.swing.JMenuItem();
        languageMenu = new javax.swing.JMenu();
        hiperGeoOnlineMenu = new javax.swing.JMenu();
        aboutHiperGeoOnlineMenuItem = new javax.swing.JMenuItem();
        uploadUpdateMenuItem = new javax.swing.JMenuItem();
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("HiperGps Project Manager");
        setName("hiperWaypointFrame");
        addWaypointButton.setText("+ row");
        deleteWaypointButton.setText("- row");
        wapointsScrollPane.setViewportBorder(javax.swing.BorderFactory.createEtchedBorder(java.awt.Color.gray, java.awt.Color.lightGray));
        wapointsScrollPane.setMinimumSize(new java.awt.Dimension(360, 435));
        wapointsScrollPane.setPreferredSize(new java.awt.Dimension(360, 435));
        zoomInButton.setText("+");
        zoomOutButton.setText("-");
        zoomResetButton.setText("recenter");
        org.jdesktop.layout.GroupLayout titlePanelLayout = new org.jdesktop.layout.GroupLayout(titlePanel);
        titlePanel.setLayout(titlePanelLayout);
        titlePanelLayout.setHorizontalGroup(titlePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(0, 393, Short.MAX_VALUE));
        titlePanelLayout.setVerticalGroup(titlePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(0, 50, Short.MAX_VALUE));
        org.jdesktop.layout.GroupLayout buildStatusPanelLayout = new org.jdesktop.layout.GroupLayout(buildStatusPanel);
        buildStatusPanel.setLayout(buildStatusPanelLayout);
        buildStatusPanelLayout.setHorizontalGroup(buildStatusPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(0, 50, Short.MAX_VALUE));
        buildStatusPanelLayout.setVerticalGroup(buildStatusPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(0, 50, Short.MAX_VALUE));
        moveUpButton.setText("move up");
        moveDownButton.setText("move down");
        org.jdesktop.layout.GroupLayout mapPanelLayout = new org.jdesktop.layout.GroupLayout(mapPanel);
        mapPanel.setLayout(mapPanelLayout);
        mapPanelLayout.setHorizontalGroup(mapPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(0, 361, Short.MAX_VALUE));
        mapPanelLayout.setVerticalGroup(mapPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(0, 435, Short.MAX_VALUE));
        statusLabel.setText("status:");
        hiperGpsMenu.setText("HiperGps");
        aboutMenuItem.setText("About HiperGps");
        hiperGpsMenu.add(aboutMenuItem);
        exitMenuItem.setText("Exit");
        hiperGpsMenu.add(exitMenuItem);
        menuBar.add(hiperGpsMenu);
        fileMenu.setText("File");
        newProjectMenuItem.setText("New Project");
        fileMenu.add(newProjectMenuItem);
        openProjectMenuItem.setText("Open Project Folder");
        fileMenu.add(openProjectMenuItem);
        newProjectFromGpxMenuItem.setText("New Project From GPX File");
        fileMenu.add(newProjectFromGpxMenuItem);
        saveAsMenuItem.setText("Save As...");
        fileMenu.add(saveAsMenuItem);
        closeProjectMenuItem.setText("Close Project");
        fileMenu.add(closeProjectMenuItem);
        menuBar.add(fileMenu);
        ingestMenu.setText("Data");
        ingestImageMenuItem.setText("Ingest Image");
        ingestMenu.add(ingestImageMenuItem);
        ingestAudioMenuItem.setText("Ingest Audio");
        ingestMenu.add(ingestAudioMenuItem);
        deleteImageMenuItem.setText("Delete Image Asset");
        ingestMenu.add(deleteImageMenuItem);
        deleteAudioMenuItem.setText("Delete Audio Asset");
        ingestMenu.add(deleteAudioMenuItem);
        importGpxWaypointsMenuItem.setText("Import GPX Waypoints");
        ingestMenu.add(importGpxWaypointsMenuItem);
        menuBar.add(ingestMenu);
        typeMenu.setText("Type");
        setFreeFormMenuItem.setText("Set Freeform Tour");
        typeMenu.add(setFreeFormMenuItem);
        setRouteMenuItem.setText("Set Route Tour");
        typeMenu.add(setRouteMenuItem);
        menuBar.add(typeMenu);
        languageMenu.setText("Language");
        menuBar.add(languageMenu);
        hiperGeoOnlineMenu.setText("HiperGeoOnline");
        aboutHiperGeoOnlineMenuItem.setText("About HiperGeoOnline");
        hiperGeoOnlineMenu.add(aboutHiperGeoOnlineMenuItem);
        uploadUpdateMenuItem.setText("Upload/Update Project");
        hiperGeoOnlineMenu.add(uploadUpdateMenuItem);
        menuBar.add(hiperGeoOnlineMenu);
        setJMenuBar(menuBar);
        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(layout.createSequentialGroup().addContainerGap().add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(statusLabel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 960, Short.MAX_VALUE).add(layout.createSequentialGroup().add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(layout.createSequentialGroup().add(mapPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)).add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(layout.createSequentialGroup().add(zoomInButton).addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED).add(zoomOutButton).addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED).add(zoomResetButton).add(153, 153, 153)).add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup().add(titlePanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 393, Short.MAX_VALUE).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)))).add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING).add(org.jdesktop.layout.GroupLayout.LEADING, layout.createSequentialGroup().add(addWaypointButton).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(deleteWaypointButton).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(moveUpButton).addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED).add(moveDownButton)).add(layout.createSequentialGroup().addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 508, Short.MAX_VALUE).add(buildStatusPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)).add(layout.createSequentialGroup().addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(wapointsScrollPane, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 555, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))))).addContainerGap()));
        layout.setVerticalGroup(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(layout.createSequentialGroup().add(24, 24, 24).add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING).add(buildStatusPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).add(titlePanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING).add(wapointsScrollPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).add(mapPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE).add(zoomResetButton).add(zoomOutButton).add(zoomInButton).add(addWaypointButton).add(moveUpButton).add(deleteWaypointButton).add(moveDownButton)).add(14, 14, 14).add(statusLabel).addContainerGap()));
        pack();
    }

    private javax.swing.JMenuItem aboutHiperGeoOnlineMenuItem;

    private javax.swing.JMenuItem aboutMenuItem;

    private javax.swing.JButton addWaypointButton;

    private net.walkingtools.j2se.editor.BuildStatusPanel buildStatusPanel;

    private javax.swing.JMenuItem closeProjectMenuItem;

    private javax.swing.JMenuItem deleteAudioMenuItem;

    private javax.swing.JMenuItem deleteImageMenuItem;

    private javax.swing.JButton deleteWaypointButton;

    private javax.swing.JMenuItem exitMenuItem;

    private javax.swing.JMenu fileMenu;

    private javax.swing.JMenu hiperGeoOnlineMenu;

    private javax.swing.JMenu hiperGpsMenu;

    private javax.swing.JMenuItem importGpxWaypointsMenuItem;

    private javax.swing.JMenuItem ingestAudioMenuItem;

    private javax.swing.JMenuItem ingestImageMenuItem;

    private javax.swing.JMenu ingestMenu;

    private javax.swing.JMenu languageMenu;

    private net.walkingtools.j2se.editor.HiperMapPanel mapPanel;

    private javax.swing.JMenuBar menuBar;

    private javax.swing.JButton moveDownButton;

    private javax.swing.JButton moveUpButton;

    private javax.swing.JMenuItem newProjectFromGpxMenuItem;

    private javax.swing.JMenuItem newProjectMenuItem;

    private javax.swing.JMenuItem openProjectMenuItem;

    private javax.swing.JMenuItem saveAsMenuItem;

    private javax.swing.JMenuItem setFreeFormMenuItem;

    private javax.swing.JMenuItem setRouteMenuItem;

    private javax.swing.JLabel statusLabel;

    private net.walkingtools.j2se.editor.TitlePanel titlePanel;

    private javax.swing.JMenu typeMenu;

    private javax.swing.JMenuItem uploadUpdateMenuItem;

    private javax.swing.JScrollPane wapointsScrollPane;

    private javax.swing.JButton zoomInButton;

    private javax.swing.JButton zoomOutButton;

    private javax.swing.JButton zoomResetButton;
}
