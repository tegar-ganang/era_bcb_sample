package org.nees.tivo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.VolatileImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Vector;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import org.nees.rbnb.JpgSaverSink;
import org.nees.rbnb.TimeProgressListener;
import org.nees.time.TimeChangeListener;
import org.nees.time.TimeLine;
import org.nees.time.TimeLinePlayerPanel;
import org.nees.time.TimeScrollBar;

public class Viewer extends JFrame {

    private final ImageRepositoryViewer itsViewerController;

    private final Viewer thisViewer;

    private ViewerSegmentHolder currentSegment = null;

    private ViewerSegmentHolder previousSegment = null;

    private ArchiveImageInterface currentImage = null;

    static ViewerUtilities v = new ViewerUtilities();

    private boolean captureRunning = false;

    private File captureRoot = null;

    private boolean extendedAction = false;

    private ViewerArchiveCover archiveCover;

    public Viewer(ImageRepositoryViewer viewer, ViewerSegmentHolder startHere, ViewerArchiveCover archiveCover) {
        thisViewer = this;
        currentSegment = startHere;
        itsViewerController = viewer;
        this.archiveCover = archiveCover;
        if (currentSegment == null) currentSegment = getNewSegment("Initial Segment");
        createAndShowGUI();
    }

    private static final String OPEN_SEGMENT = "Open New...";

    private static final String SWITCH_SEGMENT = "Open...";

    private static final String CLOSE_SEGMENT = "Exit";

    private static final String RESET_ENDPOINTS = "Reset to Endpoints";

    private static final String MERGE_SEGMENTS = "Merge Two Segments";

    private static final String SAVE_SEGMENT = "Save Full Segment";

    private static final String SAVE_CLIPPED = "Save Clipped Segemnt";

    private static final String DELETE = "Delete Segment";

    private static final String CONVERT_TO_MPG = "Generate MPG from Segment";

    private static final String CAPTURE_SEGMENT = "Capture New Segment";

    private static final String PUSH_SEGMENT = "Push Segment to RBNB";

    private static final String ABOUT = "About";

    private JMenuItem openMenuItem, switchMenuItem, closeMenuItem, resetMenuItem, deleteMenuItem, mergeMenuItem, saveMenuItem, saveClippedMenuItem, convertMenuItem, captureMenuItem, pushMenuItem, aboutMenuItem;

    private Action openAction, switchAction, closeAction, resetAction, deleteAction, mergeAction, saveAction, saveClippedAction, convertAction, captureAction, pushAction, aboutAction;

    private JLabel segmentLabel = new JLabel();

    private JLabel markTimeLabel;

    private JLabel startTimeLabel;

    private JLabel endTimeLabel;

    private JLabel currentStartTimeLabel;

    private JLabel currentEndTimeLabel;

    private JLabel timeLabel;

    private TimeScrollBar timeScrollBar;

    private JPEGPanel imagePanel;

    private TimeLinePlayerPanel playerPanel;

    private JMenuBar menuBar;

    private void createAndShowGUI() {
        JFrame.setDefaultLookAndFeelDecorated(true);
        addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                exit();
            }
        });
        makeActions();
        initMenu();
        if (currentSegment != null) {
            initGraphics();
            updateViewToCurrentSegment();
        }
        adjustMenuItems();
        pack();
        setVisible(true);
    }

    private void makeActions() {
        String desc;
        int key;
        desc = "Open a new window with the selected segment";
        key = KeyEvent.VK_O;
        openAction = new ViewerAction(OPEN_SEGMENT, desc, key) {

            public void actionPerformed(ActionEvent ev) {
                System.out.println(ev.getActionCommand());
                ViewerSegmentHolder s = getNewSegment("Show...");
                if (s == null) return;
                itsViewerController.startNewViewer(s);
            }
        };
        desc = "Switch to new segment in this window";
        key = KeyEvent.VK_S;
        switchAction = new ViewerAction(SWITCH_SEGMENT, desc, key) {

            public void actionPerformed(ActionEvent ev) {
                System.out.println(ev.getActionCommand());
                ViewerSegmentHolder s = getNewSegment("Switch to...");
                if (s == null) return;
                if (currentSegment == null) {
                    itsViewerController.startNewViewer(s);
                    exit();
                } else setAndUpdateCurrentSegment(s);
            }
        };
        desc = "Close this window (exit when last window closes)";
        key = KeyEvent.VK_C;
        closeAction = new ViewerAction(CLOSE_SEGMENT, desc, key) {

            public void actionPerformed(ActionEvent ev) {
                System.out.println(ev.getActionCommand());
                exit();
            }
        };
        desc = "Reset the endpoint of the clipping region to their original valeus";
        resetAction = new ViewerAction(RESET_ENDPOINTS, desc) {

            public void actionPerformed(ActionEvent ev) {
                System.out.println(ev.getActionCommand());
                currentSegment.resetStartEndTimes();
                setSegmentLabel();
                updateImageFromTimeLine();
            }
        };
        desc = "Select a segment and delete it";
        key = KeyEvent.VK_D;
        deleteAction = new ViewerAction(DELETE, desc, key) {

            public void actionPerformed(ActionEvent ev) {
                System.out.println(ev.getActionCommand());
                try {
                    ViewerSegmentHolder segmentToDelete = getNewSegment("Segment to Delete");
                    if (segmentToDelete == null) return;
                    int option = JOptionPane.showConfirmDialog(thisViewer, "Delete " + segmentToDelete.getName() + "?", "Confirm Delete Action", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                    if (option != 0) return;
                    deleteSegment(segmentToDelete);
                } catch (Throwable t) {
                    t.printStackTrace();
                    showError(thisViewer, "Some problem with Delete");
                }
            }
        };
        desc = "Select two segments and merge them";
        mergeAction = new ViewerAction(MERGE_SEGMENTS, desc) {

            public void actionPerformed(ActionEvent ev) {
                System.out.println(ev.getActionCommand());
                runMerge();
            }
        };
        desc = "Save a new segment from a copy of the current segment";
        saveAction = new ViewerAction(SAVE_SEGMENT, desc) {

            public void actionPerformed(ActionEvent ev) {
                if (currentSegment == null) return;
                String newName = archiveCover.nextDefaultSegmentName();
                newName = getTextValue(thisViewer, "Save Segment " + currentSegment.getName() + " as: ", newName);
                if (newName == null) return;
                startExtendedAction();
                try {
                    ViewerSegmentHolder seg = (ViewerSegmentHolder) archiveCover.makeNewCopyOfSegment(currentSegment.getName(), newName);
                    setAndUpdateCurrentSegment(seg);
                } catch (Exception e) {
                    e.printStackTrace();
                    showError(thisViewer, "Save of " + newName + " failed.");
                }
                endExtendedAction();
            }
        };
        desc = "Save a new segment from a copy of the " + "clipped region of the current segment";
        saveClippedAction = new ViewerAction(SAVE_CLIPPED, desc) {

            public void actionPerformed(ActionEvent ev) {
                System.out.println(ev.getActionCommand());
                if (currentSegment == null) return;
                String newName = archiveCover.nextDefaultSegmentName();
                newName = getTextValue(thisViewer, "Save Segment " + currentSegment.getName() + " as: ", newName);
                if (newName == null) return;
                startExtendedAction();
                double startTime = currentSegment.getTimeLine().getCurrentStartTime();
                double endTime = currentSegment.getTimeLine().getCurrentEndTime();
                try {
                    ViewerSegmentHolder seg = (ViewerSegmentHolder) archiveCover.makeNewCopyOfSegment(currentSegment.getName(), newName, startTime, endTime);
                    setAndUpdateCurrentSegment(seg);
                } catch (Exception e) {
                    e.printStackTrace();
                    showError(thisViewer, "Save of " + newName + " failed.");
                }
                endExtendedAction();
            }
        };
        desc = "Select a segment and convert it to MPEG3";
        convertAction = new ViewerAction(CONVERT_TO_MPG, desc) {

            public void actionPerformed(ActionEvent ev) {
                System.out.println(ev.getActionCommand());
                final SwingWorker worker = new SwingWorker() {

                    public Object construct() {
                        makeMovieOfCurrentSegment();
                        return null;
                    }
                };
                worker.start();
            }
        };
        desc = "Capture a segment from RBNB";
        captureAction = new ViewerAction(CAPTURE_SEGMENT, desc) {

            public void actionPerformed(ActionEvent ev) {
                new LongTimeDialog(thisViewer, "Initialize Dialog to Capture a Segment to RBNB");
                System.out.println(ev.getActionCommand());
                final SwingWorker worker = new SwingWorker() {

                    public Object construct() {
                        runCapture();
                        return null;
                    }
                };
                worker.start();
            }
        };
        desc = "Select a segment and push it to RBNB";
        pushAction = new ViewerAction(PUSH_SEGMENT, desc) {

            public void actionPerformed(ActionEvent ev) {
                System.out.println(ev.getActionCommand());
                final SwingWorker worker = new SwingWorker() {

                    public Object construct() {
                        pushSegmentToRbnb();
                        return null;
                    }
                };
                worker.start();
            }
        };
        desc = "Display the About dialog";
        aboutAction = new ViewerAction(ABOUT, desc) {

            public void actionPerformed(ActionEvent ev) {
                System.out.println(ev.getActionCommand());
                new AboutDialog(thisViewer);
            }
        };
    }

    private void exit() {
        setAndUpdateCurrentSegment(null);
        dispose();
        this.itsViewerController.topLevelExit(this);
    }

    private void initMenu() {
        openMenuItem = makeMenuItem("Open Segment", KeyEvent.VK_O, openAction);
        switchMenuItem = makeMenuItem("Show New Segment in Window", KeyEvent.VK_S, switchAction);
        closeMenuItem = makeMenuItem("Exit - Close Segment", KeyEvent.VK_X, closeAction);
        resetMenuItem = makeMenuItem("Reset clippting points to ends", 0, resetAction);
        deleteMenuItem = makeMenuItem("Delete Segment", KeyEvent.VK_D, deleteAction);
        mergeMenuItem = makeMenuItem("Merge Two Segments", KeyEvent.VK_M, mergeAction);
        saveMenuItem = makeMenuItem("Save New Segment From Full Segment", KeyEvent.VK_F, saveAction);
        saveClippedMenuItem = makeMenuItem("Save New Segment From Clipped Segment", KeyEvent.VK_C, saveClippedAction);
        convertMenuItem = makeMenuItem("Create MPEG from Segment", KeyEvent.VK_C, convertAction);
        captureMenuItem = makeMenuItem("Capture Segment From RBNB", KeyEvent.VK_C, captureAction);
        pushMenuItem = makeMenuItem("Push Segment to RBNB", KeyEvent.VK_P, pushAction);
        aboutMenuItem = makeMenuItem("About", KeyEvent.VK_A, aboutAction);
        JMenu menu;
        menuBar = new JMenuBar();
        menu = new JMenu("Segment");
        menu.setMnemonic(KeyEvent.VK_T);
        menu.getAccessibleContext().setAccessibleDescription("Actions on Archive Segments");
        menuBar.add(menu);
        menu.add(openMenuItem);
        menu.addSeparator();
        menu.add(switchMenuItem);
        menu.addSeparator();
        menu.add(closeMenuItem);
        menu = new JMenu("Timeline");
        menu.getAccessibleContext().setAccessibleDescription("Actions on this segments timeline");
        menuBar.add(menu);
        menu.add(resetMenuItem);
        menu = new JMenu("Archive");
        menu.setMnemonic(KeyEvent.VK_R);
        menu.getAccessibleContext().setAccessibleDescription("Operations on the Archive");
        menuBar.add(menu);
        menu.add(saveMenuItem);
        menu.add(saveClippedMenuItem);
        menu.add(mergeMenuItem);
        menu.add(deleteMenuItem);
        menu = new JMenu("RBNB");
        menu.setMnemonic(KeyEvent.VK_R);
        menu.getAccessibleContext().setAccessibleDescription("Get or Put Segments from RBNB");
        menuBar.add(menu);
        menu.add(captureMenuItem);
        menu.add(pushMenuItem);
        menu = new JMenu("MPEG");
        menu.setMnemonic(KeyEvent.VK_G);
        menu.getAccessibleContext().setAccessibleDescription("Create MPEG from Segment");
        menuBar.add(menu);
        menu.add(convertMenuItem);
        menu = new JMenu("Help");
        menu.setMnemonic(KeyEvent.VK_H);
        menu.getAccessibleContext().setAccessibleDescription("Help");
        menuBar.add(menu);
        menu.add(aboutMenuItem);
        setJMenuBar(menuBar);
    }

    private JMenuItem makeMenuItem(String text, int key, Action a) {
        JMenuItem menuItem = new JMenuItem(text, key);
        menuItem.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        if (a != null) menuItem.addActionListener(a);
        menuItem.getAccessibleContext().setAccessibleDescription(text);
        return menuItem;
    }

    private ViewerSegmentHolder getNewSegment(String message) {
        GetSegmentDialog d = new GetSegmentDialog(message, archiveCover, this, currentSegment);
        if ((!d.isCancled()) && (d.getSelectedSegment() != null)) return (ViewerSegmentHolder) d.getSelectedSegment();
        return null;
    }

    private void initGraphics() {
        setTitle("Browse Repository");
        JPanel top = new JPanel();
        top.setLayout(new BorderLayout());
        if (currentSegment != null) {
            String titleText = "Image Time: ";
            String timeText = ImageRepositoryViewer.DATE_FORMAT.format(new Date());
            timeLabel = new JLabel(timeText);
            markTimeLabel = new JLabel(timeText);
            startTimeLabel = new JLabel(timeText);
            endTimeLabel = new JLabel(timeText);
            currentStartTimeLabel = new JLabel(timeText);
            currentEndTimeLabel = new JLabel(timeText);
            JPanel main = new JPanel();
            main.setLayout(new BorderLayout());
            JPanel titlePanel = new JPanel();
            titlePanel.setLayout(new BorderLayout());
            JPanel labelPanel = new JPanel();
            labelPanel.add(new JLabel("Current Segment: "));
            setSegmentLabel();
            labelPanel.add(segmentLabel);
            titlePanel.add(labelPanel, BorderLayout.NORTH);
            JPanel timeBarPanel = new JPanel();
            timeBarPanel.setLayout(new BoxLayout(timeBarPanel, BoxLayout.PAGE_AXIS));
            JPanel timeLabels = new JPanel();
            timeLabels.setLayout(new BoxLayout(timeLabels, BoxLayout.Y_AXIS));
            JPanel markLine = new JPanel();
            markLine.setLayout(new BoxLayout(markLine, BoxLayout.X_AXIS));
            JPanel startEndTextAndLabels = new JPanel();
            startEndTextAndLabels.setLayout(new BoxLayout(startEndTextAndLabels, BoxLayout.X_AXIS));
            JPanel currentStartEndTextAndLabels = new JPanel();
            currentStartEndTextAndLabels.setLayout(new BoxLayout(currentStartEndTextAndLabels, BoxLayout.X_AXIS));
            JPanel holderMarkTime = new JPanel();
            holderMarkTime.setLayout(new BoxLayout(holderMarkTime, BoxLayout.X_AXIS));
            JPanel holderStartTime = new JPanel();
            holderStartTime.setLayout(new BoxLayout(holderStartTime, BoxLayout.X_AXIS));
            JPanel holderCurrentStartTime = new JPanel();
            holderCurrentStartTime.setLayout(new BoxLayout(holderCurrentStartTime, BoxLayout.X_AXIS));
            JPanel holderCurrentEndTime = new JPanel();
            holderCurrentEndTime.setLayout(new BoxLayout(holderCurrentEndTime, BoxLayout.X_AXIS));
            JPanel holderEndTime = new JPanel();
            holderEndTime.setLayout(new BoxLayout(holderEndTime, BoxLayout.X_AXIS));
            holderMarkTime.add(new JLabel("Time at mark position: "));
            holderMarkTime.add(markTimeLabel);
            holderStartTime.add(new JLabel("Start Time -- "));
            holderStartTime.add(startTimeLabel);
            holderEndTime.add(endTimeLabel);
            holderEndTime.add(new JLabel("-- EndTime"));
            holderCurrentStartTime.add(new JLabel("Clipped at -- "));
            holderCurrentStartTime.add(currentStartTimeLabel);
            holderCurrentEndTime.add(currentEndTimeLabel);
            holderCurrentEndTime.add(new JLabel("-- as Clipped"));
            markLine.add(holderMarkTime);
            startEndTextAndLabels.add(holderStartTime);
            startEndTextAndLabels.add(Box.createHorizontalGlue());
            startEndTextAndLabels.add(holderEndTime);
            currentStartEndTextAndLabels.add(holderCurrentStartTime);
            currentStartEndTextAndLabels.add(Box.createHorizontalGlue());
            currentStartEndTextAndLabels.add(holderCurrentEndTime);
            timeLabels.add(markLine);
            timeLabels.add(startEndTextAndLabels);
            timeLabels.add(currentStartEndTextAndLabels);
            timeLabels.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            titlePanel.add(timeLabels, BorderLayout.CENTER);
            TimeLine timeLine = new TimeLine(0.0, 1.0);
            timeScrollBar = new TimeScrollBar(timeLine);
            timeScrollBar.setBorder(BorderFactory.createLineBorder(Color.black));
            timeBarPanel.add(timeScrollBar);
            playerPanel = new TimeLinePlayerPanel(timeLine);
            timeBarPanel.add(playerPanel);
            titlePanel.add(timeBarPanel, BorderLayout.SOUTH);
            main.add(titlePanel, BorderLayout.NORTH);
            JPanel holder = new JPanel();
            holder.setLayout(new BorderLayout());
            JPanel titleHolder = new JPanel();
            titleHolder.add(new JLabel(titleText));
            titleHolder.add(timeLabel);
            holder.add(titleHolder, BorderLayout.NORTH);
            imagePanel = new JPEGPanel();
            holder.add(imagePanel, BorderLayout.CENTER);
            main.add(holder, BorderLayout.CENTER);
            top.add(main, BorderLayout.CENTER);
        }
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(top, BorderLayout.CENTER);
    }

    private void updateImageFromTimeLine() {
        if (currentSegment == null) return;
        Object o = currentSegment.getTimeLine().getCurrentEventObject();
        if (o == null) return;
        if (!(o instanceof ArchiveImageInterface)) {
            System.out.println("Unexpected class of time line event object class = " + o.getClass().getName());
            return;
        }
        ArchiveImageInterface image = (ArchiveImageInterface) o;
        updateCurrentImage(image);
    }

    private void runMerge() {
        MergeSegmentDialog merge = new MergeSegmentDialog(archiveCover, this, currentSegment);
        ArchiveSegmentInterface seg1 = merge.getSeg1();
        if (seg1 == null) return;
        ArchiveSegmentInterface seg2 = merge.getSeg2();
        if (seg2 == null) return;
        String name = merge.getMergeSegmentName();
        if (name == null) return;
        startExtendedAction();
        try {
            ArchiveSegmentInterface seg = merge(name, seg1, seg2);
            archiveCover.addCoveredSegment((ViewerSegmentHolder) seg);
            setAndUpdateCurrentSegment((ViewerSegmentHolder) seg);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (ArchiveException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        }
        endExtendedAction();
    }

    public ArchiveSegmentInterface merge(String name, ArchiveSegmentInterface seg1, ArchiveSegmentInterface seg2) throws IllegalArgumentException, ArchiveException, FileNotFoundException, IOException {
        if (seg1.getStartTime() > seg2.getStartTime()) {
            ArchiveSegmentInterface temp = seg1;
            seg1 = seg2;
            seg2 = temp;
        }
        if (archiveCover.getSegmentByName(name) != null) throw new IllegalArgumentException("In merge of segments " + seg1.getName() + " and " + seg2.getName() + " the name of the" + " new (megred) segment, " + name + " is already in use.");
        if (seg1.getEndTime() >= seg2.getStartTime()) System.out.println("Warning, segments overlap. In the case that images " + "are at the same time in both sequences, images from " + "the earler segment (" + seg1.getName() + ") " + "will have priority.");
        ArchiveSegmentInterface seg = archiveCover.mergeOrderedSegments(name, seg1, seg2);
        return seg;
    }

    private void deleteSegment(ViewerSegmentHolder seg) {
        if (seg == null) {
            System.out.println("Delete: no segment selected.");
            return;
        }
        try {
            startExtendedAction();
            archiveCover.removeSegment(seg.getName(), true);
            if (seg.equals(currentSegment)) {
                exit();
            }
            endExtendedAction();
        } catch (ArchiveException e) {
            endExtendedAction();
            e.printStackTrace();
            showError(this, "WARNING: segment not completely deleted (" + seg.getName() + ")");
        }
    }

    private JpgSaverSink captureControl = null;

    private JDialog captureDialog = null;

    private String captureSegmentName;

    JLabel captureProgressLabel = null;

    ImageCaptureArgsDialog caputreArgsDialog = null;

    JButton captureProgressCancelButton, captureProgressSaveButton;

    String captureServer, captureChannel;

    private void runCapture() {
        if (caputreArgsDialog == null) {
            caputreArgsDialog = new ImageCaptureArgsDialog(this, archiveCover);
            caputreArgsDialog.start();
        } else {
            caputreArgsDialog.restart(caputreArgsDialog.getHost(), caputreArgsDialog.getPort(), caputreArgsDialog.getSourcePath());
        }
        if (caputreArgsDialog.cancled()) {
            return;
        }
        System.out.println("Start Time: " + caputreArgsDialog.getStartTime());
        System.out.println("End Time: " + caputreArgsDialog.getEndTime());
        captureSegmentName = caputreArgsDialog.getSegmentName();
        String captureSinkName = "_CaptureSink";
        String captureHost = caputreArgsDialog.getHost();
        String capturePort = caputreArgsDialog.getPort();
        captureServer = captureHost + ":" + capturePort;
        String captureSourcePath = caputreArgsDialog.getSourcePath();
        captureChannel = captureSourcePath;
        double captureStartTime = caputreArgsDialog.getStartTime();
        double captureEndTime = caputreArgsDialog.getEndTime();
        String startTimeString = "";
        if (captureStartTime == 0.0) startTimeString = "now"; else startTimeString = ImageRepositoryViewer.DATE_FORMAT.format(new Date((long) (captureStartTime * 1000.0)));
        String endTimeString = "";
        if (captureEndTime == 0.0) endTimeString = "forever"; else endTimeString = ImageRepositoryViewer.DATE_FORMAT.format(new Date((long) (captureEndTime * 1000.0)));
        String[] message = { "<html><font size=+1><b>Capture</b></font></html>", "Start the capture of segment = " + captureSegmentName, "on RBNB server = " + captureHost + ":" + capturePort, "capturing from " + captureSourcePath, "running from " + startTimeString, "to " + endTimeString, "or until you stop it" };
        int n = JOptionPane.showConfirmDialog(this, message, "Start Capture", JOptionPane.OK_CANCEL_OPTION);
        if (n != JOptionPane.OK_OPTION) {
            return;
        }
        System.out.println("");
        System.out.println("Parameters");
        System.out.println("Segment Name: " + captureSegmentName);
        System.out.println("Server: " + captureHost + ":" + capturePort);
        System.out.println("Path of Source: " + captureSourcePath);
        System.out.println("Start Time: " + startTimeString);
        System.out.println("End Time: " + endTimeString);
        System.out.println("Duration: " + (captureEndTime - captureStartTime));
        System.out.println("");
        System.out.println("Let the capture being");
        captureRunning = true;
        adjustMenuItems();
        captureRoot = new File(((Archive) archiveCover.itsArchive).getBaseDir(), captureSegmentName);
        String base = captureRoot.getAbsolutePath();
        try {
            captureControl = new JpgSaverSink();
            captureControl.setup(captureHost, capturePort, captureSinkName, captureSourcePath, base, captureStartTime, captureEndTime);
            captureControl.addTimeProgressListener(new TimeProgressListener() {

                public void progressUpdate(double estimatedDuration, double consumedTime) {
                    setCaptureProgress(consumedTime, estimatedDuration);
                }
            });
            captureControl.startThread();
        } catch (Throwable e) {
            e.printStackTrace();
            String[] error = { "<html><font size=+1><b>Capture</b></font></html>", "RBNB failed on capture of = " + captureSegmentName, "on RBNB server = " + captureHost + "/" + capturePort, "capturing from " + captureSourcePath, "reason = " + e.toString() };
            showError(this, error);
            return;
        }
        captureDialog = new JDialog(this, "Monitor Capture");
        captureDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        JPanel labelPanel = new JPanel();
        labelPanel.setLayout(new BorderLayout());
        JLabel label = new JLabel("<html><p align=center><font size=+1>" + "<b>Capture</b></font></p><br>" + "Running the capture of segment = " + captureSegmentName + "<br> on RBNB server = " + captureHost + ":" + capturePort + "<br> capturing from " + captureSourcePath + "<br>Start Time: " + startTimeString + "<br>End Time: " + endTimeString + "<br>Duration: " + (captureEndTime - captureStartTime) + "<br> " + "<br> <b>CLICK</b> below to stop the Capture.</html>");
        label.setHorizontalAlignment(JLabel.CENTER);
        labelPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        labelPanel.add(label, BorderLayout.CENTER);
        label = new JLabel(captureProgressDisplay());
        labelPanel.add(label, BorderLayout.SOUTH);
        captureProgressLabel = label;
        JPanel closePanel = new JPanel();
        JButton button;
        button = new JButton("Stop and Cancel");
        button.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                captureDone(false);
            }
        });
        captureProgressCancelButton = button;
        button = new JButton("Stop and Save");
        button.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                captureDone(true);
            }
        });
        captureProgressSaveButton = button;
        closePanel.setLayout(new BoxLayout(closePanel, BoxLayout.X_AXIS));
        closePanel.add(captureProgressCancelButton);
        closePanel.add(captureProgressSaveButton);
        closePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(labelPanel, BorderLayout.CENTER);
        contentPanel.add(closePanel, BorderLayout.SOUTH);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        contentPanel.setOpaque(true);
        captureDialog.setContentPane(contentPanel);
        captureDialog.setLocationRelativeTo(this);
        captureDialog.pack();
        captureDialog.setVisible(true);
    }

    double captureProgressCurrent = 0.0;

    double captureProgressDuration = 0.0;

    private void setCaptureProgress(double current, double duration) {
        captureProgressCurrent = current;
        captureProgressDuration = duration;
        captureProgressLabel.setText(captureProgressDisplay());
        if (current >= duration) {
            System.out.println("Capture done");
            String display = "<html><font size=-2>";
            if (captureControl.isStatusOk()) display += "Segment Capture Done:"; else display += "Some problem in Capture:";
            String[] s = captureControl.getStatusMessages();
            if (s.length > 0) {
                for (int i = 0; i < s.length; i++) {
                    display += "<br>" + s[i];
                }
            }
            display += "</font></html>";
            captureProgressLabel.setText(display);
            captureProgressCancelButton.setText("Cancel");
            captureProgressSaveButton.setText("Save");
            captureDialog.pack();
        }
    }

    private String captureProgressDisplay() {
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(4);
        nf.setMinimumFractionDigits(1);
        return "<html>Progress: " + nf.format(captureProgressCurrent) + "<br>out of " + nf.format(captureProgressDuration) + " seconds.</html>";
    }

    private void captureDone(boolean isOK) {
        captureRunning = false;
        adjustMenuItems();
        File root = captureRoot;
        captureRoot = null;
        String name = captureSegmentName;
        captureSegmentName = null;
        if (captureControl != null) {
            captureControl.stopThread();
            int retries = 0;
            while (captureControl.isStillRunning() && (retries++ < 20)) {
                try {
                    Thread.sleep(100);
                } catch (Exception ignore) {
                }
            }
            if (captureControl.isStillRunning()) {
                String[] error = { "<html><font size=+1><b>Capture Demo</b></font></html>", "Serious problems with the capture thread?", "It is likely that the thread is hung; ", "on capture of = " + name };
                showError(this, error);
            }
            captureControl = null;
        }
        if (!isOK) {
            System.out.println("Deleting temporary dir tree at " + root.getAbsolutePath());
            boolean worked = recursivelyDelete(root);
            if (worked) System.out.println("Delted: " + root.getAbsolutePath()); else System.out.println("Faild to delete: " + root.getAbsolutePath());
            return;
        }
        archiveCover.updateMapFromTrueArchive();
        ViewerSegmentHolder seg = (ViewerSegmentHolder) archiveCover.getSegmentByName(name);
        if (seg != null) {
            seg.setProperty(ArchiveSegmentInterface.PROPERTY_KEY_Server, captureServer);
            seg.setProperty(ArchiveSegmentInterface.PROPERTY_KEY_Channel, captureChannel);
            seg.setProperty(ArchiveSegmentInterface.PROPERTY_KEY_Date_Created, ImageRepositoryViewer.DATE_FORMAT.format(new Date(seg.getStartTime())));
            if (currentSegment == null) {
                itsViewerController.startNewViewer(seg);
                exit();
            } else setAndUpdateCurrentSegment(seg);
        } else System.out.println("Segment unexpectedly null: " + name);
        if (captureDialog != null) {
            captureDialog.setVisible(false);
            captureDialog.dispose();
            captureDialog = null;
        }
    }

    private boolean recursivelyDelete(File root) {
        boolean didIt = true;
        File[] fl = root.listFiles();
        if (fl == null) return root.delete(); else for (int i = 0; i < fl.length; i++) {
            didIt = didIt && recursivelyDelete(fl[i]);
        }
        return didIt && root.delete();
    }

    private void makeMovieOfCurrentSegment() {
        ArchiveInterface theArchive = (ArchiveInterface) archiveCover;
        String segmentName = currentSegment.getName();
        String movieFile = "archive.mov";
        double startTime = 0.0, endTime = 0.0, duration = 0.0;
        boolean useDuration = false;
        float speedupFactor = 1.0f;
        ArchiveToMovie m = new ArchiveToMovie();
        if (m.setup(theArchive, segmentName, movieFile, startTime, endTime, duration, useDuration, speedupFactor)) m.exec();
    }

    SegmentPushArgsDialog pushArgsDialog;

    private void pushSegmentToRbnb() {
        if (pushArgsDialog == null) {
            pushArgsDialog = new SegmentPushArgsDialog(this, archiveCover, currentSegment);
            pushArgsDialog.start();
        } else {
            pushArgsDialog.restart(pushArgsDialog.getHost(), pushArgsDialog.getPort(), currentSegment);
        }
        if (pushArgsDialog.cancled()) {
            return;
        }
        String host = pushArgsDialog.getHost();
        String port = pushArgsDialog.getPort();
        ArchiveSegmentInterface segment = pushArgsDialog.getSegment();
        String sourceName = pushArgsDialog.getSource();
        String channelName = pushArgsDialog.getChannel();
        ArchiveToRbnb doit = new ArchiveToRbnb();
        if (doit.setup(host, port, sourceName, channelName, segment) && doit.connect()) doit.startThread();
    }

    private void startExtendedAction() {
        extendedAction = true;
        adjustMenuItems();
    }

    private void endExtendedAction() {
        extendedAction = false;
        adjustMenuItems();
    }

    private void adjustMenuItems() {
        openMenuItem.setEnabled(false);
        switchMenuItem.setEnabled(false);
        resetMenuItem.setEnabled(false);
        deleteMenuItem.setEnabled(false);
        saveMenuItem.setEnabled(false);
        saveClippedMenuItem.setEnabled(false);
        mergeMenuItem.setEnabled(false);
        convertMenuItem.setEnabled(false);
        captureMenuItem.setEnabled(false);
        pushMenuItem.setEnabled(false);
        aboutMenuItem.setEnabled(true);
        if (extendedAction) return;
        if (captureRunning) captureMenuItem.setEnabled(false); else captureMenuItem.setEnabled(true);
        if (archiveCover.getSegmentsArray().length == 0) return;
        openMenuItem.setEnabled(true);
        switchMenuItem.setEnabled(true);
        deleteMenuItem.setEnabled(true);
        if (archiveCover.getSegmentsArray().length > 1) mergeMenuItem.setEnabled(true);
        if (currentSegment == null) return;
        convertMenuItem.setEnabled(true);
        pushMenuItem.setEnabled(true);
        resetMenuItem.setEnabled(true);
        saveMenuItem.setEnabled(true);
        saveClippedMenuItem.setEnabled(true);
    }

    private void showError(Component item, Object message) {
        JOptionPane.showMessageDialog(item, message);
    }

    private String getTextValue(Component item, Object message, String initialValue) {
        return JOptionPane.showInputDialog(item, message, initialValue);
    }

    private void setAndUpdateCurrentSegment(ViewerSegmentHolder seg) {
        setCurrentSegment(seg);
        updateViewToCurrentSegment();
        pack();
    }

    private void setCurrentSegment(ViewerSegmentHolder seg) {
        previousSegment = currentSegment;
        currentSegment = seg;
    }

    private void updateViewToCurrentSegment() {
        if ((previousSegment == null) && (currentSegment == null)) return;
        if ((currentSegment != null) && currentSegment.equals(previousSegment)) return;
        if (previousSegment != null) {
            previousSegment.getTimeLine().removeAllTimeListeners();
        }
        if (currentSegment != null) {
            setSegmentLabel();
            if (timeScrollBar != null) {
                TimeLine timeLine = currentSegment.getTimeLine();
                timeScrollBar.resetTimeLine(timeLine);
                timeLine.addTimeListener(new TimeChangeListener() {

                    public void timeChanged(TimeLine t, boolean currentMarkChanged, boolean marksChanged) {
                        if (currentSegment != null) {
                            updateTimelineTimes(t);
                            if (currentMarkChanged) updateImageFromTimeLine();
                        }
                    }
                });
                playerPanel.setNewTimeLine(timeLine);
                timeLine.resetTimes();
                timeLine.forceFullNotify();
            }
        }
        adjustMenuItems();
    }

    private void updateCurrentImage(ArchiveImageInterface image) {
        if (image == null) return;
        currentImage = image;
        long time = currentImage.getTime();
        String timeText = ImageRepositoryViewer.DATE_FORMAT.format(new Date(time));
        timeLabel.setText(timeText);
        imagePanel.update(image.getImageInputStream());
    }

    private void updateTimelineTimes(TimeLine t) {
        double d;
        long time;
        String timeText;
        d = t.getCurrentTime();
        time = (long) (d * 1000.0);
        timeText = ImageRepositoryViewer.DATE_FORMAT.format(new Date(time));
        markTimeLabel.setText(timeText);
        d = t.getStartTime();
        time = (long) (d * 1000.0);
        timeText = ImageRepositoryViewer.DATE_FORMAT.format(new Date(time));
        startTimeLabel.setText(timeText);
        d = t.getCurrentStartTime();
        time = (long) (d * 1000.0);
        timeText = ImageRepositoryViewer.DATE_FORMAT.format(new Date(time));
        currentStartTimeLabel.setText(timeText);
        d = t.getCurrentEndTime();
        time = (long) (d * 1000.0);
        timeText = ImageRepositoryViewer.DATE_FORMAT.format(new Date(time));
        currentEndTimeLabel.setText(timeText);
        d = t.getEndTime();
        time = (long) (d * 1000.0);
        timeText = ImageRepositoryViewer.DATE_FORMAT.format(new Date(time));
        endTimeLabel.setText(timeText);
    }

    private void setSegmentLabel() {
        if (currentSegment == null) segmentLabel.setText("<html><font color=red><b>No Segment Selected." + "</b></font> (Use " + SWITCH_SEGMENT + " to select a segmemt.)</html>"); else {
            String name = currentSegment.getName();
            segmentLabel.setText("<html><b>" + name + "</b></html>");
        }
    }

    private abstract class ViewerAction extends AbstractAction {

        ViewerAction(String text, String desc) {
            super(text);
            putValue(SHORT_DESCRIPTION, desc);
        }

        ViewerAction(String text, String desc, int mnemonic) {
            this(text, desc);
            putValue(MNEMONIC_KEY, new Integer(mnemonic));
        }
    }

    private class JPEGPanel extends JComponent {

        private Image image;

        private VolatileImage volatileImage;

        private boolean newFrame;

        private boolean keepAspectRatio = true;

        public JPEGPanel() {
            super();
            image = null;
            volatileImage = null;
            newFrame = false;
        }

        private void createBackBuffer() {
            if (volatileImage != null) {
                volatileImage.flush();
                volatileImage = null;
            }
            volatileImage = createVolatileImage(image.getWidth(null), image.getHeight(null));
            copyFrame();
        }

        private void copyFrame() {
            Graphics2D gVolatile = (Graphics2D) volatileImage.getGraphics();
            synchronized (this) {
                gVolatile.drawImage(image, 0, 0, null);
                newFrame = false;
            }
            gVolatile.dispose();
        }

        public final void paintComponent(Graphics g1) {
            Graphics2D g = (Graphics2D) g1;
            if (image == null) {
                g.setBackground(Color.BLACK);
                g.clearRect(0, 0, getWidth(), getHeight());
                return;
            }
            if (volatileImage == null || newFrame) {
                createBackBuffer();
            }
            do {
                int valCode = volatileImage.validate(getGraphicsConfiguration());
                if (valCode == VolatileImage.IMAGE_RESTORED) {
                    copyFrame();
                } else if (valCode == VolatileImage.IMAGE_INCOMPATIBLE) {
                    createBackBuffer();
                }
                int componentWidth = getWidth();
                int componentHeight = getHeight();
                g.setBackground(Color.BLACK);
                g.clearRect(0, 0, componentWidth, componentHeight);
                int imageWidth = volatileImage.getWidth();
                int imageHeight = volatileImage.getHeight();
                float widthScale = componentWidth / (float) imageWidth;
                float heightScale = componentHeight / (float) imageHeight;
                if (keepAspectRatio && widthScale != heightScale) {
                    widthScale = heightScale = Math.min(widthScale, heightScale);
                }
                int scaledWidth = (int) (imageWidth * widthScale);
                int scaledHeight = (int) (imageHeight * heightScale);
                int widthOffset = (componentWidth - scaledWidth) / 2;
                int heightOffset = (componentHeight - scaledHeight) / 2;
                AffineTransform af = new AffineTransform(widthScale, 0f, 0f, heightScale, widthOffset, heightOffset);
                g.drawImage(volatileImage, af, this);
            } while (volatileImage.contentsLost());
        }

        public void update(byte[] imageData) {
            Image newImage = new ImageIcon(imageData).getImage();
            showImage(newImage);
        }

        public void update(String imageFileName) {
            Image newImage = new ImageIcon(imageFileName).getImage();
            showImage(newImage);
        }

        private static final int IMAGE_BUFFER_SIZE = 10000;

        private class Holder {

            byte[] b;

            int c;

            Holder(byte[] bb, int cc) {
                b = bb;
                c = cc;
            }
        }

        public void update(InputStream imageStream) {
            try {
                Vector all = new Vector();
                byte[] data = new byte[IMAGE_BUFFER_SIZE];
                int read;
                read = imageStream.read(data);
                all.addElement(new Holder(data, read));
                while (read >= 0) {
                    data = new byte[IMAGE_BUFFER_SIZE];
                    read = imageStream.read(data);
                    if (read > 0) all.addElement(new Holder(data, read));
                }
                int total = 0;
                for (int i = 0; i < all.size(); i++) total += ((Holder) all.elementAt(i)).c;
                byte[] buf = new byte[total];
                int mark = 0;
                for (int i = 0; i < all.size(); i++) {
                    byte[] one = ((Holder) all.elementAt(i)).b;
                    int count = ((Holder) all.elementAt(i)).c;
                    System.arraycopy(one, 0, buf, mark, count);
                    mark += count;
                }
                update(buf);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void clear() {
            image = null;
            repaint();
        }

        private void showImage(Image newImage) {
            synchronized (this) {
                if (image != null) {
                    image.flush();
                    image = null;
                }
                image = newImage;
                newFrame = true;
            }
            repaint();
        }

        public Dimension getPreferredSize() {
            Dimension dimension;
            if (image != null) {
                dimension = new Dimension(image.getWidth(this), image.getHeight(this));
            } else {
                dimension = new Dimension(0, 0);
            }
            return dimension;
        }

        public Dimension getMinimumSize() {
            return new Dimension(0, 0);
        }

        public Dimension getDisplayedImageSize() {
            float widthScale = getWidth() / (float) image.getWidth(null);
            float heightScale = getHeight() / (float) image.getHeight(null);
            if (keepAspectRatio && widthScale != heightScale) {
                widthScale = heightScale = Math.min(widthScale, heightScale);
            }
            int scaledWidth = (int) (volatileImage.getWidth() * widthScale);
            int scaledHeight = (int) (volatileImage.getHeight() * heightScale);
            return new Dimension(scaledWidth, scaledHeight);
        }
    }
}
