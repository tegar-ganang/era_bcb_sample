package in.raster.mayam.form;

import in.raster.mayam.context.ApplicationContext;
import in.raster.mayam.delegate.LocalizerDelegate;
import in.raster.mayam.form.dialog.ExportDialog;
import in.raster.mayam.util.DicomTags;
import in.raster.mayam.util.DicomTagsReader;
import in.raster.mayam.form.display.Display;
import in.raster.mayam.delegate.CineTimer;
import in.raster.mayam.delegate.DestinationFinder;
import in.raster.mayam.facade.Platform;
import in.raster.mayam.form.dcm3d.DicomMIP;
import in.raster.mayam.form.dcm3d.DicomMPR2D;
import in.raster.mayam.form.dcm3d.DicomMPR3DSlider;
import in.raster.mayam.form.dcm3d.DicomVolumeRendering;
import in.raster.mayam.form.dcm3d.SurfaceRendering;
import in.raster.mayam.model.Instance;
import in.raster.mayam.model.PresetModel;
import in.raster.mayam.model.Series;
import in.raster.mayam.model.Study;
import in.raster.mayam.util.core.TranscoderMain;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import org.dcm4che.dict.Tags;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.io.DicomInputStream;

/**
 *
 * @author  BabuHussain
 * @version 0.5
 *
 */
public class ImageToolbar extends javax.swing.JPanel {

    /** Creates new form ImageToolbar */
    CineTimer cineTimer;

    Timer timer;

    ImageView imgView;

    SurfaceRendering surface = null;

    DicomMIP dcmMip = null;

    DicomMPR2D dcmMPR2D = null;

    DicomMPR3DSlider mpr3DSlider = null;

    DicomVolumeRendering dicomVolumeRendering = null;

    public ImageToolbar() {
        initComponents();
        cineTimer = new CineTimer();
        addKeyEventDispatcher();
    }

    public ImageToolbar(ImageView imgView) {
        initComponents();
        cineTimer = new CineTimer();
        this.imgView = imgView;
        designPopup();
        layoutButton.setArrowPopupMenu(jPopupMenu1);
        textOverlayContext();
        addKeyEventDispatcher();
    }

    public void addKeyEventDispatcher() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {

            public boolean dispatchKeyEvent(KeyEvent e) {
                if (e.getID() == KeyEvent.KEY_PRESSED) {
                    if (!ApplicationContext.mainScreen.getQueryScreen().isFocused()) {
                        boolean status = false;
                        if (ApplicationContext.mainScreen.getPreference() != null) {
                            status = ApplicationContext.mainScreen.getPreference().isFocused();
                        }
                        if (!status) {
                            keyEventProcessor(e);
                        }
                    }
                }
                boolean discardEvent = false;
                return discardEvent;
            }
        });
    }

    private void keyEventProcessor(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_LEFT) {
            ApplicationContext.imgPanel.moveToPreviousInstance();
        } else if (e.getKeyCode() == KeyEvent.VK_DOWN || e.getKeyCode() == KeyEvent.VK_RIGHT) {
            ApplicationContext.imgPanel.moveToNextInstance();
        } else if (e.getKeyCode() == KeyEvent.VK_O) {
            doScout();
        } else if (e.getKeyCode() == KeyEvent.VK_I) {
            doTextOverlay();
        } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            doReset();
        } else if (e.getKeyCode() == KeyEvent.VK_C) {
            if (!loopCheckbox.isSelected()) {
                loopCheckbox.setSelected(true);
            } else {
                loopCheckbox.setSelected(false);
            }
            doCineLoop();
        } else if (e.getKeyCode() == KeyEvent.VK_S) {
            doStack();
        } else if (e.getKeyCode() == KeyEvent.VK_D) {
            doRuler(false);
        } else if (e.getKeyCode() == KeyEvent.VK_A) {
            doRuler(true);
        } else if (e.getKeyCode() == KeyEvent.VK_T) {
            doPan();
        }
    }

    private void initComponents() {
        jPopupMenu1 = new javax.swing.JPopupMenu();
        jPopupMenu2 = new javax.swing.JPopupMenu();
        toolsButtonGroup = new javax.swing.ButtonGroup();
        jPopupMenu3 = new javax.swing.JPopupMenu();
        jPopupMenu4 = new javax.swing.JPopupMenu();
        jToolBar3 = new javax.swing.JToolBar();
        layoutButton = new in.raster.mayam.form.JComboButton();
        windowing = new javax.swing.JButton();
        presetButton = new javax.swing.JButton();
        probeButton = new javax.swing.JButton();
        verticalFlip = new javax.swing.JButton();
        horizontalFlip = new javax.swing.JButton();
        leftRotate = new javax.swing.JButton();
        rightRotate = new javax.swing.JButton();
        zoomin = new javax.swing.JButton();
        zoomoutButton = new javax.swing.JButton();
        panButton = new javax.swing.JButton();
        invert = new javax.swing.JButton();
        rulerButton = new javax.swing.JButton();
        rectangleButton = new javax.swing.JButton();
        ellipseButton = new javax.swing.JButton();
        arrowButton = new javax.swing.JButton();
        clearAllMeasurement = new javax.swing.JButton();
        deleteMeasurement = new javax.swing.JButton();
        moveMeasurement = new javax.swing.JButton();
        annotationVisibility = new javax.swing.JButton();
        textOverlay = new javax.swing.JButton();
        reset = new javax.swing.JButton();
        exportButton = new javax.swing.JButton();
        metaDataButton = new javax.swing.JButton();
        stackButton = new javax.swing.JButton();
        scoutButton = new javax.swing.JButton();
        cube3DButton = new javax.swing.JButton();
        synchronizeButton = new javax.swing.JButton();
        loopCheckbox = new javax.swing.JCheckBox();
        loopSlider = new javax.swing.JSlider();
        setBackground(new java.awt.Color(102, 102, 102));
        jToolBar3.setRollover(true);
        layoutButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/in/raster/mayam/form/images/layout.png")));
        layoutButton.setText("");
        layoutButton.setToolTipText("Layout");
        layoutButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        layoutButton.setIconTextGap(2);
        layoutButton.setPreferredSize(new java.awt.Dimension(45, 45));
        layoutButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToolBar3.add(layoutButton);
        windowing.setIcon(new javax.swing.ImageIcon(getClass().getResource("/in/raster/mayam/form/images/windowing.png")));
        windowing.setToolTipText("Windowing");
        toolsButtonGroup.add(windowing);
        windowing.setFocusPainted(false);
        windowing.setFocusable(false);
        windowing.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        windowing.setPreferredSize(new java.awt.Dimension(45, 45));
        windowing.setRequestFocusEnabled(false);
        windowing.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        windowing.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                windowingActionPerformed(evt);
            }
        });
        jToolBar3.add(windowing);
        presetButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/in/raster/mayam/form/images/drop_down.png")));
        presetButton.setToolTipText("Preset");
        presetButton.setComponentPopupMenu(jPopupMenu2);
        presetButton.setFocusable(false);
        presetButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        presetButton.setMaximumSize(new java.awt.Dimension(12, 24));
        presetButton.setMinimumSize(new java.awt.Dimension(12, 24));
        presetButton.setPreferredSize(new java.awt.Dimension(45, 45));
        presetButton.setRequestFocusEnabled(false);
        presetButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        presetButton.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                presetButtonMouseClicked(evt);
            }
        });
        jToolBar3.add(presetButton);
        probeButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/in/raster/mayam/form/images/probe.png")));
        probeButton.setToolTipText("Probe");
        probeButton.setFocusable(false);
        probeButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        probeButton.setPreferredSize(new java.awt.Dimension(45, 45));
        probeButton.setRequestFocusEnabled(false);
        probeButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        probeButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                probeButtonActionPerformed(evt);
            }
        });
        jToolBar3.add(probeButton);
        verticalFlip.setIcon(new javax.swing.ImageIcon(getClass().getResource("/in/raster/mayam/form/images/flip_vertical.png")));
        verticalFlip.setToolTipText("Vertical Flip");
        verticalFlip.setFocusable(false);
        verticalFlip.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        verticalFlip.setPreferredSize(new java.awt.Dimension(45, 45));
        verticalFlip.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        verticalFlip.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                verticalFlipActionPerformed(evt);
            }
        });
        jToolBar3.add(verticalFlip);
        horizontalFlip.setIcon(new javax.swing.ImageIcon(getClass().getResource("/in/raster/mayam/form/images/flip_horizontal.png")));
        horizontalFlip.setToolTipText("Horizontal Flip");
        horizontalFlip.setFocusable(false);
        horizontalFlip.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        horizontalFlip.setPreferredSize(new java.awt.Dimension(45, 45));
        horizontalFlip.setRequestFocusEnabled(false);
        horizontalFlip.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        horizontalFlip.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                horizontalFlipActionPerformed(evt);
            }
        });
        jToolBar3.add(horizontalFlip);
        leftRotate.setIcon(new javax.swing.ImageIcon(getClass().getResource("/in/raster/mayam/form/images/rotate_left.png")));
        leftRotate.setToolTipText("Rotate Left");
        leftRotate.setFocusable(false);
        leftRotate.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        leftRotate.setPreferredSize(new java.awt.Dimension(45, 45));
        leftRotate.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        leftRotate.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                leftRotateActionPerformed(evt);
            }
        });
        jToolBar3.add(leftRotate);
        rightRotate.setIcon(new javax.swing.ImageIcon(getClass().getResource("/in/raster/mayam/form/images/rotate_right.png")));
        rightRotate.setToolTipText("Rotate Right");
        rightRotate.setFocusable(false);
        rightRotate.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        rightRotate.setPreferredSize(new java.awt.Dimension(45, 45));
        rightRotate.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        rightRotate.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rightRotateActionPerformed(evt);
            }
        });
        jToolBar3.add(rightRotate);
        zoomin.setIcon(new javax.swing.ImageIcon(getClass().getResource("/in/raster/mayam/form/images/zoomin.png")));
        zoomin.setToolTipText("Zoom In");
        zoomin.setFocusable(false);
        zoomin.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        zoomin.setPreferredSize(new java.awt.Dimension(45, 45));
        zoomin.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        zoomin.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                zoominActionPerformed(evt);
            }
        });
        jToolBar3.add(zoomin);
        zoomoutButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/in/raster/mayam/form/images/zoomout.png")));
        zoomoutButton.setToolTipText("Zoom Out");
        zoomoutButton.setFocusable(false);
        zoomoutButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        zoomoutButton.setPreferredSize(new java.awt.Dimension(45, 45));
        zoomoutButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        zoomoutButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                zoomoutButtonActionPerformed(evt);
            }
        });
        jToolBar3.add(zoomoutButton);
        panButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/in/raster/mayam/form/images/pan.png")));
        panButton.setToolTipText("Pan");
        toolsButtonGroup.add(panButton);
        panButton.setFocusable(false);
        panButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        panButton.setPreferredSize(new java.awt.Dimension(45, 45));
        panButton.setRequestFocusEnabled(false);
        panButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        panButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                panButtonActionPerformed(evt);
            }
        });
        jToolBar3.add(panButton);
        invert.setIcon(new javax.swing.ImageIcon(getClass().getResource("/in/raster/mayam/form/images/invert.png")));
        invert.setToolTipText("Invert");
        invert.setFocusable(false);
        invert.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        invert.setPreferredSize(new java.awt.Dimension(45, 45));
        invert.setRequestFocusEnabled(false);
        invert.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        invert.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                invertActionPerformed(evt);
            }
        });
        jToolBar3.add(invert);
        rulerButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/in/raster/mayam/form/images/ruler.png")));
        rulerButton.setToolTipText("Ruler");
        rulerButton.setActionCommand("ruler");
        toolsButtonGroup.add(rulerButton);
        rulerButton.setFocusable(false);
        rulerButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        rulerButton.setPreferredSize(new java.awt.Dimension(45, 45));
        rulerButton.setRequestFocusEnabled(false);
        rulerButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        rulerButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rulerButtonActionPerformed(evt);
            }
        });
        jToolBar3.add(rulerButton);
        rectangleButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/in/raster/mayam/form/images/rectangle.png")));
        rectangleButton.setToolTipText("Rectangle ROI");
        rectangleButton.setActionCommand("rectangle");
        toolsButtonGroup.add(rectangleButton);
        rectangleButton.setFocusable(false);
        rectangleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        rectangleButton.setPreferredSize(new java.awt.Dimension(45, 45));
        rectangleButton.setRequestFocusEnabled(false);
        rectangleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        rectangleButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rectangleButtonActionPerformed(evt);
            }
        });
        jToolBar3.add(rectangleButton);
        ellipseButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/in/raster/mayam/form/images/ellipse.png")));
        ellipseButton.setToolTipText("Elliptical ROI");
        ellipseButton.setActionCommand("ellipse");
        toolsButtonGroup.add(ellipseButton);
        ellipseButton.setFocusable(false);
        ellipseButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        ellipseButton.setPreferredSize(new java.awt.Dimension(45, 45));
        ellipseButton.setRequestFocusEnabled(false);
        ellipseButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        ellipseButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ellipseButtonActionPerformed(evt);
            }
        });
        jToolBar3.add(ellipseButton);
        arrowButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/in/raster/mayam/form/images/arrow.png")));
        arrowButton.setToolTipText("Arrow");
        arrowButton.setActionCommand("arrow");
        arrowButton.setFocusable(false);
        arrowButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        arrowButton.setPreferredSize(new java.awt.Dimension(45, 45));
        arrowButton.setRequestFocusEnabled(false);
        arrowButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        arrowButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                arrowButtonActionPerformed(evt);
            }
        });
        jToolBar3.add(arrowButton);
        clearAllMeasurement.setIcon(new javax.swing.ImageIcon(getClass().getResource("/in/raster/mayam/form/images/clear_all_annotation.png")));
        clearAllMeasurement.setToolTipText("Clear All Measurement");
        clearAllMeasurement.setActionCommand("clearAll");
        clearAllMeasurement.setPreferredSize(new java.awt.Dimension(45, 45));
        clearAllMeasurement.setRequestFocusEnabled(false);
        clearAllMeasurement.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearAllMeasurementActionPerformed(evt);
            }
        });
        jToolBar3.add(clearAllMeasurement);
        deleteMeasurement.setIcon(new javax.swing.ImageIcon(getClass().getResource("/in/raster/mayam/form/images/delete_annotation.png")));
        deleteMeasurement.setToolTipText("Delete Measurement");
        deleteMeasurement.setActionCommand("deleteMeasurement");
        toolsButtonGroup.add(deleteMeasurement);
        deleteMeasurement.setFocusable(false);
        deleteMeasurement.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        deleteMeasurement.setPreferredSize(new java.awt.Dimension(45, 45));
        deleteMeasurement.setRequestFocusEnabled(false);
        deleteMeasurement.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        deleteMeasurement.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteMeasurementActionPerformed(evt);
            }
        });
        jToolBar3.add(deleteMeasurement);
        moveMeasurement.setIcon(new javax.swing.ImageIcon(getClass().getResource("/in/raster/mayam/form/images/annotation_selection.png")));
        moveMeasurement.setToolTipText("Measurement Selection");
        moveMeasurement.setActionCommand("moveMeasurement");
        toolsButtonGroup.add(moveMeasurement);
        moveMeasurement.setDoubleBuffered(true);
        moveMeasurement.setFocusable(false);
        moveMeasurement.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        moveMeasurement.setPreferredSize(new java.awt.Dimension(45, 45));
        moveMeasurement.setRequestFocusEnabled(false);
        moveMeasurement.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        moveMeasurement.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                moveMeasurementActionPerformed(evt);
            }
        });
        jToolBar3.add(moveMeasurement);
        annotationVisibility.setIcon(new javax.swing.ImageIcon(getClass().getResource("/in/raster/mayam/form/images/annotation_overlay.png")));
        annotationVisibility.setToolTipText("Annotation Overlay");
        annotationVisibility.setFocusable(false);
        annotationVisibility.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        annotationVisibility.setPreferredSize(new java.awt.Dimension(45, 45));
        annotationVisibility.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        annotationVisibility.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                annotationVisibilityActionPerformed(evt);
            }
        });
        jToolBar3.add(annotationVisibility);
        textOverlay.setIcon(new javax.swing.ImageIcon(getClass().getResource("/in/raster/mayam/form/images/textoverlay.png")));
        textOverlay.setToolTipText("Text Overlay");
        textOverlay.setFocusable(false);
        textOverlay.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        textOverlay.setPreferredSize(new java.awt.Dimension(45, 45));
        textOverlay.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        textOverlay.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mousePressed(java.awt.event.MouseEvent evt) {
                textOverlayMousePressed(evt);
            }
        });
        jToolBar3.add(textOverlay);
        reset.setIcon(new javax.swing.ImageIcon(getClass().getResource("/in/raster/mayam/form/images/reset.png")));
        reset.setToolTipText("Reset");
        reset.setFocusable(false);
        reset.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        reset.setPreferredSize(new java.awt.Dimension(45, 45));
        reset.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        reset.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetActionPerformed(evt);
            }
        });
        jToolBar3.add(reset);
        exportButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/in/raster/mayam/form/images/export_series.png")));
        exportButton.setToolTipText("Export");
        exportButton.setFocusable(false);
        exportButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        exportButton.setPreferredSize(new java.awt.Dimension(45, 45));
        exportButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        exportButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportButtonActionPerformed(evt);
            }
        });
        jToolBar3.add(exportButton);
        metaDataButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/in/raster/mayam/form/images/metadata_viewerpage.png")));
        metaDataButton.setToolTipText("Meta Data");
        metaDataButton.setFocusable(false);
        metaDataButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        metaDataButton.setPreferredSize(new java.awt.Dimension(45, 45));
        metaDataButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        metaDataButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                metaDataButtonActionPerformed(evt);
            }
        });
        jToolBar3.add(metaDataButton);
        stackButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/in/raster/mayam/form/images/stack.png")));
        stackButton.setToolTipText("Stack");
        toolsButtonGroup.add(stackButton);
        stackButton.setFocusable(false);
        stackButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        stackButton.setPreferredSize(new java.awt.Dimension(45, 45));
        stackButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        stackButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stackButtonActionPerformed(evt);
            }
        });
        jToolBar3.add(stackButton);
        scoutButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/in/raster/mayam/form/images/scout.png")));
        scoutButton.setToolTipText("ScoutLine");
        toolsButtonGroup.add(scoutButton);
        scoutButton.setFocusable(false);
        scoutButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        scoutButton.setPreferredSize(new java.awt.Dimension(45, 45));
        scoutButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        scoutButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                scoutButtonActionPerformed(evt);
            }
        });
        jToolBar3.add(scoutButton);
        cube3DButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/in/raster/mayam/form/images/Cube3D.png")));
        cube3DButton.setToolTipText("3D");
        cube3DButton.setComponentPopupMenu(jPopupMenu4);
        cube3DButton.setFocusable(false);
        cube3DButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        cube3DButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        cube3DButton.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                cube3DButtonMouseClicked(evt);
            }
        });
        cube3DButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cube3DButtonActionPerformed(evt);
            }
        });
        jToolBar3.add(cube3DButton);
        synchronizeButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/in/raster/mayam/form/images/Link.png")));
        synchronizeButton.setToolTipText("Synchronize");
        toolsButtonGroup.add(synchronizeButton);
        synchronizeButton.setFocusable(false);
        synchronizeButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        synchronizeButton.setPreferredSize(new java.awt.Dimension(45, 45));
        synchronizeButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        synchronizeButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                synchronizeButtonActionPerformed(evt);
            }
        });
        jToolBar3.add(synchronizeButton);
        loopCheckbox.setFont(new java.awt.Font("Lucida Grande", 1, 12));
        loopCheckbox.setText("Loop");
        loopCheckbox.setFocusable(false);
        loopCheckbox.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        loopCheckbox.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        loopCheckbox.addChangeListener(new javax.swing.event.ChangeListener() {

            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                loopCheckboxStateChanged(evt);
            }
        });
        loopCheckbox.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loopCheckboxActionPerformed(evt);
            }
        });
        jToolBar3.add(loopCheckbox);
        loopSlider.setMaximum(9);
        loopSlider.setPaintTicks(true);
        loopSlider.setValue(6);
        loopSlider.setDoubleBuffered(true);
        loopSlider.addChangeListener(new javax.swing.event.ChangeListener() {

            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                loopSliderStateChanged(evt);
            }
        });
        jToolBar3.add(loopSlider);
        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(jToolBar3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 1151, Short.MAX_VALUE));
        layout.setVerticalGroup(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(jToolBar3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 59, Short.MAX_VALUE));
    }

    private void rightRotateActionPerformed(java.awt.event.ActionEvent evt) {
        if (ApplicationContext.annotationPanel != null && ApplicationContext.imgPanel != null) {
            ApplicationContext.imgPanel.rotateRight();
            ApplicationContext.annotationPanel.doRotateRight();
        } else {
            JOptionPane.showMessageDialog(this, "Tile selected is not valid for this process");
        }
    }

    private void leftRotateActionPerformed(java.awt.event.ActionEvent evt) {
        if (ApplicationContext.annotationPanel != null && ApplicationContext.imgPanel != null) {
            ApplicationContext.imgPanel.rotateLeft();
            ApplicationContext.annotationPanel.doRotateLeft();
        } else {
            JOptionPane.showMessageDialog(ImageToolbar.this, "Tile selected is not valid for this process");
        }
    }

    private void invertActionPerformed(java.awt.event.ActionEvent evt) {
        if (ApplicationContext.annotationPanel != null && ApplicationContext.imgPanel != null) {
            ApplicationContext.imgPanel.negative();
        } else {
            JOptionPane.showMessageDialog(ImageToolbar.this, "Tile selected is not valid for this process");
        }
    }

    private void horizontalFlipActionPerformed(java.awt.event.ActionEvent evt) {
        if (ApplicationContext.annotationPanel != null && ApplicationContext.imgPanel != null) {
            ApplicationContext.imgPanel.flipHorizontal();
            ApplicationContext.annotationPanel.doFlipHorizontal();
            ApplicationContext.imgPanel.repaint();
            ApplicationContext.layeredCanvas.textOverlay.repaint();
        } else {
            JOptionPane.showMessageDialog(ImageToolbar.this, "Tile selected is not valid for this process");
        }
    }

    private void verticalFlipActionPerformed(java.awt.event.ActionEvent evt) {
        if (ApplicationContext.annotationPanel != null && ApplicationContext.imgPanel != null) {
            ApplicationContext.imgPanel.flipVertical();
            ApplicationContext.annotationPanel.doFlipVertical();
            ApplicationContext.imgPanel.repaint();
            ApplicationContext.layeredCanvas.textOverlay.repaint();
        } else {
            JOptionPane.showMessageDialog(ImageToolbar.this, "Tile selected is not valid for this process");
        }
    }

    private void windowingActionPerformed(java.awt.event.ActionEvent evt) {
        setWindowingTool();
    }

    private void zoominActionPerformed(java.awt.event.ActionEvent evt) {
        if (ApplicationContext.annotationPanel != null && ApplicationContext.imgPanel != null) {
            resetTools();
            ApplicationContext.imgPanel.doZoomIn();
            ApplicationContext.annotationPanel.doZoomIn();
            ApplicationContext.imgPanel.repaint();
        } else {
            JOptionPane.showMessageDialog(ImageToolbar.this, "Tile selected is not valid for this process");
        }
    }

    private void resetActionPerformed(java.awt.event.ActionEvent evt) {
        doReset();
    }

    public void doReset() {
        if (ApplicationContext.annotationPanel != null && ApplicationContext.imgPanel != null) {
            ApplicationContext.imgPanel.reset();
            ApplicationContext.annotationPanel.reset();
            setWindowing();
        } else {
            JOptionPane.showMessageDialog(ImageToolbar.this, "Tile selected is not valid for this process");
        }
    }

    private void probeButtonActionPerformed(java.awt.event.ActionEvent evt) {
        if (ApplicationContext.annotationPanel != null && ApplicationContext.imgPanel != null) {
            ApplicationContext.annotationPanel.setAddLine(false);
            ApplicationContext.annotationPanel.setAddArrow(false);
            ApplicationContext.annotationPanel.setAddEllipse(false);
            ApplicationContext.annotationPanel.setAddRect(false);
            ApplicationContext.annotationPanel.stopPanning();
            ApplicationContext.imgPanel.probe();
        } else {
            JOptionPane.showMessageDialog(ImageToolbar.this, "Tile selected is not valid for this process");
        }
    }

    private void panButtonActionPerformed(java.awt.event.ActionEvent evt) {
        doPan();
    }

    public void doPan() {
        toolsButtonGroup.clearSelection();
        if (ApplicationContext.annotationPanel != null && ApplicationContext.imgPanel != null) {
            ApplicationContext.annotationPanel.setAddLine(false);
            ApplicationContext.annotationPanel.setAddArrow(false);
            ApplicationContext.annotationPanel.setAddEllipse(false);
            ApplicationContext.annotationPanel.setAddRect(false);
            ApplicationContext.imgPanel.doPan();
            ApplicationContext.annotationPanel.doPan();
            toolsButtonGroup.setSelected(panButton.getModel(), true);
        } else {
            JOptionPane.showMessageDialog(ImageToolbar.this, "Tile selected is not valid for this process");
        }
    }

    private void zoomoutButtonActionPerformed(java.awt.event.ActionEvent evt) {
        if (ApplicationContext.annotationPanel != null && ApplicationContext.imgPanel != null) {
            resetTools();
            ApplicationContext.imgPanel.doZoomOut();
            ApplicationContext.annotationPanel.doZoomOut();
            ApplicationContext.imgPanel.repaint();
        } else {
            JOptionPane.showMessageDialog(ImageToolbar.this, "Tile selected is not valid for this process");
        }
    }

    private void resetTools() {
        if (ApplicationContext.annotationPanel != null && ApplicationContext.imgPanel != null) {
            ApplicationContext.annotationPanel.setMouseLocX1(-1);
        } else {
            JOptionPane.showMessageDialog(ImageToolbar.this, "Tile selected is not valid for this process");
        }
    }

    private void rulerButtonActionPerformed(java.awt.event.ActionEvent evt) {
        doRuler(false);
    }

    public void doRuler(boolean addArrow) {
        if (ApplicationContext.annotationPanel != null && ApplicationContext.imgPanel != null) {
            ApplicationContext.annotationPanel.stopPanning();
            ApplicationContext.imgPanel.setToolsToNull();
            ApplicationContext.annotationPanel.setMouseLocX1(-1);
            toolsButtonGroup.clearSelection();
            if (addArrow) {
                if (!ApplicationContext.annotationPanel.isAddArrow()) {
                    ApplicationContext.annotationPanel.setAddArrow(true);
                    ApplicationContext.annotationPanel.setAddLine(false);
                    ApplicationContext.annotationPanel.setAddEllipse(false);
                    ApplicationContext.annotationPanel.setAddRect(false);
                } else {
                    ApplicationContext.annotationPanel.setAddArrow(false);
                }
                toolsButtonGroup.setSelected(arrowButton.getModel(), true);
            } else {
                if (!ApplicationContext.annotationPanel.isAddLine()) {
                    ApplicationContext.annotationPanel.setAddArrow(false);
                    ApplicationContext.annotationPanel.setAddLine(true);
                    ApplicationContext.annotationPanel.setAddEllipse(false);
                    ApplicationContext.annotationPanel.setAddRect(false);
                } else {
                    ApplicationContext.annotationPanel.setAddLine(false);
                }
                toolsButtonGroup.setSelected(rulerButton.getModel(), true);
            }
        } else {
            JOptionPane.showMessageDialog(ImageToolbar.this, "Tile selected is not valid for this process");
        }
    }

    public void refreshToolsDisplay() {
        if (!windowing.isEnabled()) {
            enableAllTools();
        } else {
            setAnnotationToolsStatus();
            layoutToolStatus();
        }
    }

    private void layoutToolStatus() {
        if (ApplicationContext.imgPanel.getCanvas().getLayeredCanvas().getComparedWithStudies() != null) {
            layoutButton.setEnabled(false);
        } else {
            layoutButton.setEnabled(true);
        }
    }

    private void enableAllTools() {
        layoutToolStatus();
        windowing.setEnabled(true);
        presetButton.setEnabled(true);
        probeButton.setEnabled(true);
        verticalFlip.setEnabled(true);
        horizontalFlip.setEnabled(true);
        leftRotate.setEnabled(true);
        rightRotate.setEnabled(true);
        zoomin.setEnabled(true);
        zoomoutButton.setEnabled(true);
        panButton.setEnabled(true);
        invert.setEnabled(true);
        annotationVisibility.setEnabled(true);
        textOverlay.setEnabled(true);
        reset.setEnabled(true);
        exportButton.setEnabled(true);
        metaDataButton.setEnabled(true);
        stackButton.setEnabled(true);
        scoutButton.setEnabled(true);
        cube3DButton.setEnabled(true);
        synchronizeButton.setEnabled(true);
        setAnnotationToolsStatus();
    }

    public void disableAllTools() {
        layoutButton.setEnabled(false);
        windowing.setEnabled(false);
        presetButton.setEnabled(false);
        probeButton.setEnabled(false);
        verticalFlip.setEnabled(false);
        horizontalFlip.setEnabled(false);
        leftRotate.setEnabled(false);
        rightRotate.setEnabled(false);
        zoomin.setEnabled(false);
        zoomoutButton.setEnabled(false);
        panButton.setEnabled(false);
        invert.setEnabled(false);
        rulerButton.setEnabled(false);
        arrowButton.setEnabled(false);
        rectangleButton.setEnabled(false);
        ellipseButton.setEnabled(false);
        clearAllMeasurement.setEnabled(false);
        deleteMeasurement.setEnabled(false);
        moveMeasurement.setEnabled(false);
        annotationVisibility.setEnabled(false);
        textOverlay.setEnabled(false);
        reset.setEnabled(false);
        exportButton.setEnabled(false);
        metaDataButton.setEnabled(false);
        stackButton.setEnabled(false);
        scoutButton.setEnabled(false);
        cube3DButton.setEnabled(false);
        synchronizeButton.setEnabled(false);
    }

    private void stackButtonActionPerformed(java.awt.event.ActionEvent evt) {
        doStack();
    }

    public void doStack() {
        toolsButtonGroup.clearSelection();
        if (ApplicationContext.annotationPanel != null && ApplicationContext.imgPanel != null) {
            ApplicationContext.annotationPanel.stopPanning();
            storeAnnotationHook();
            ApplicationContext.annotationPanel.setAddLine(false);
            ApplicationContext.annotationPanel.setAddArrow(false);
            ApplicationContext.annotationPanel.setAddEllipse(false);
            ApplicationContext.annotationPanel.setAddRect(false);
            ApplicationContext.imgPanel.doStack();
            ApplicationContext.imgPanel.repaint();
            if (ApplicationContext.imgPanel.isStackSelected()) {
                toolsButtonGroup.setSelected(stackButton.getModel(), true);
            }
        } else {
            JOptionPane.showMessageDialog(ImageToolbar.this, "Tile selected is not valid for this process");
        }
    }

    private void loopSliderStateChanged(javax.swing.event.ChangeEvent evt) {
        try {
            if (ApplicationContext.annotationPanel != null && ApplicationContext.imgPanel != null) {
                if (loopCheckbox.isSelected()) {
                    if (timer != null) {
                        timer.cancel();
                        timer = new Timer();
                        timer.scheduleAtFixedRate(new CineTimer(), 0, (11 - loopSlider.getValue()) * 100);
                    } else {
                        timer = new Timer();
                        timer.scheduleAtFixedRate(new CineTimer(), 0, (11 - loopSlider.getValue()) * 100);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loopCheckboxStateChanged(javax.swing.event.ChangeEvent evt) {
    }

    private void loopCheckboxActionPerformed(java.awt.event.ActionEvent evt) {
        doCineLoop();
    }

    public void doCineLoop() {
        if (ApplicationContext.annotationPanel != null && ApplicationContext.imgPanel != null) {
            if (loopCheckbox.isSelected()) {
                storeAnnotationHook();
                try {
                    timer = new Timer();
                    timer.scheduleAtFixedRate(new CineTimer(), 0, (11 - loopSlider.getValue()) * 100);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                if (timer != null) {
                    timer.cancel();
                }
            }
        } else {
            loopCheckbox.setSelected(false);
        }
    }

    public void resetCineTimer() {
        if (timer != null) {
            timer.cancel();
        }
    }

    private void rectangleButtonActionPerformed(java.awt.event.ActionEvent evt) {
        doRectangle();
    }

    public void doRectangle() {
        toolsButtonGroup.clearSelection();
        if (ApplicationContext.annotationPanel != null && ApplicationContext.imgPanel != null) {
            ApplicationContext.annotationPanel.stopPanning();
            ApplicationContext.imgPanel.setToolsToNull();
            ApplicationContext.annotationPanel.setMouseLocX1(-1);
            if (!ApplicationContext.annotationPanel.isAddRect()) {
                ApplicationContext.annotationPanel.setAddLine(false);
                ApplicationContext.annotationPanel.setAddArrow(false);
                ApplicationContext.annotationPanel.setAddEllipse(false);
                ApplicationContext.annotationPanel.setAddRect(true);
            } else {
                ApplicationContext.annotationPanel.setAddRect(false);
            }
            toolsButtonGroup.setSelected(rectangleButton.getModel(), true);
        } else {
            JOptionPane.showMessageDialog(ImageToolbar.this, "Tile selected is not valid for this process");
        }
    }

    private void ellipseButtonActionPerformed(java.awt.event.ActionEvent evt) {
        doEllipse();
    }

    public void doEllipse() {
        toolsButtonGroup.clearSelection();
        if (ApplicationContext.annotationPanel != null && ApplicationContext.imgPanel != null) {
            ApplicationContext.annotationPanel.stopPanning();
            ApplicationContext.imgPanel.setToolsToNull();
            ApplicationContext.annotationPanel.setMouseLocX1(-1);
            if (!ApplicationContext.annotationPanel.isAddEllipse()) {
                ApplicationContext.annotationPanel.setAddLine(false);
                ApplicationContext.annotationPanel.setAddArrow(false);
                ApplicationContext.annotationPanel.setAddEllipse(true);
                ApplicationContext.annotationPanel.setAddRect(false);
            } else {
                ApplicationContext.annotationPanel.setAddEllipse(false);
            }
            toolsButtonGroup.setSelected(ellipseButton.getModel(), true);
        } else {
            JOptionPane.showMessageDialog(ImageToolbar.this, "Tile selected is not valid for this process");
        }
    }

    private void clearAllMeasurementActionPerformed(java.awt.event.ActionEvent evt) {
        if (ApplicationContext.annotationPanel != null) {
            ApplicationContext.annotationPanel.clearAllMeasurement();
        } else {
            JOptionPane.showMessageDialog(ImageToolbar.this, "Tile selected is not valid for this process");
        }
    }

    private void annotationVisibilityActionPerformed(java.awt.event.ActionEvent evt) {
        if (ApplicationContext.annotationPanel != null) {
            ApplicationContext.annotationPanel.toggleAnnotation();
            setAnnotationToolsStatus();
        } else {
            JOptionPane.showMessageDialog(ImageToolbar.this, "Tile selected is not valid for this process");
        }
    }

    public void showAnnotationTools() {
        arrowButton.setEnabled(true);
        rulerButton.setEnabled(true);
        rectangleButton.setEnabled(true);
        ellipseButton.setEnabled(true);
        clearAllMeasurement.setEnabled(true);
        deleteMeasurement.setEnabled(true);
        moveMeasurement.setEnabled(true);
    }

    public void hideAnnotationTools() {
        arrowButton.setEnabled(false);
        rulerButton.setEnabled(false);
        rectangleButton.setEnabled(false);
        ellipseButton.setEnabled(false);
        clearAllMeasurement.setEnabled(false);
        deleteMeasurement.setEnabled(false);
        moveMeasurement.setEnabled(false);
        String actionCommand = null;
        if (toolsButtonGroup != null && toolsButtonGroup.getSelection() != null) {
            actionCommand = toolsButtonGroup.getSelection().getActionCommand();
        }
        if (ApplicationContext.annotationPanel != null && ApplicationContext.imgPanel != null) {
            if (actionCommand != null) {
                if (actionCommand.equalsIgnoreCase("ruler") || actionCommand.equalsIgnoreCase("arrow") || actionCommand.equalsIgnoreCase("rectangle") || actionCommand.equalsIgnoreCase("ellipse") || actionCommand.equalsIgnoreCase("deleteMeasurement") || actionCommand.equalsIgnoreCase("moveMeasurement")) {
                    ApplicationContext.annotationPanel.setAddLine(false);
                    ApplicationContext.annotationPanel.setAddEllipse(false);
                    ApplicationContext.annotationPanel.setAddRect(false);
                    ApplicationContext.annotationPanel.setAddArrow(false);
                    ApplicationContext.annotationPanel.stopPanning();
                    ApplicationContext.imgPanel.doWindowing();
                    toolsButtonGroup.clearSelection();
                    toolsButtonGroup.setSelected(windowing.getModel(), true);
                }
            }
        }
    }

    public void setWindowingTool() {
        toolsButtonGroup.clearSelection();
        if (ApplicationContext.annotationPanel != null && ApplicationContext.imgPanel != null) {
            ApplicationContext.annotationPanel.setAddLine(false);
            ApplicationContext.annotationPanel.setAddArrow(false);
            ApplicationContext.annotationPanel.setAddEllipse(false);
            ApplicationContext.annotationPanel.setAddRect(false);
            ApplicationContext.annotationPanel.stopPanning();
            ApplicationContext.imgPanel.doWindowing();
            if (ApplicationContext.imgPanel.isWindowingSelected()) {
                toolsButtonGroup.setSelected(windowing.getModel(), true);
            }
        }
    }

    public void setWindowing() {
        if (ApplicationContext.annotationPanel != null && ApplicationContext.imgPanel != null) {
            ApplicationContext.imgPanel.setWindowingToolsAsDefault();
            if (ApplicationContext.imgPanel.isWindowingSelected()) {
                toolsButtonGroup.setSelected(windowing.getModel(), true);
            }
        }
    }

    public void setAnnotationToolsStatus() {
        if (ApplicationContext.annotationPanel.isShowAnnotation()) {
            ApplicationContext.imgView.getImageToolbar().showAnnotationTools();
        } else {
            ApplicationContext.imgView.getImageToolbar().hideAnnotationTools();
        }
    }

    private void deleteMeasurementActionPerformed(java.awt.event.ActionEvent evt) {
        toolsButtonGroup.clearSelection();
        if (ApplicationContext.annotationPanel != null) {
            ApplicationContext.annotationPanel.doDeleteMeasurement();
            if (ApplicationContext.annotationPanel.isDeleteMeasurement()) {
                toolsButtonGroup.setSelected(deleteMeasurement.getModel(), true);
            }
        } else {
            JOptionPane.showMessageDialog(ImageToolbar.this, "Tile selected is not valid for this process");
        }
    }

    private void moveMeasurementActionPerformed(java.awt.event.ActionEvent evt) {
        toolsButtonGroup.clearSelection();
        if (ApplicationContext.annotationPanel != null && ApplicationContext.imgPanel != null) {
            ImagePanel.tool = "";
            AnnotationPanel.tool = "";
            ApplicationContext.annotationPanel.doMoveMeasurement();
            if (AnnotationPanel.isMoveMeasurement()) {
                toolsButtonGroup.setSelected(moveMeasurement.getModel(), true);
            }
        } else {
            JOptionPane.showMessageDialog(ImageToolbar.this, "Tile selected is not valid for this process");
        }
    }

    private void exportButtonActionPerformed(java.awt.event.ActionEvent evt) {
        if (ApplicationContext.annotationPanel != null && ApplicationContext.imgPanel != null) {
            ExportDialog jpegConvertor = new ExportDialog(ApplicationContext.imgView, true);
            Display.alignScreen(jpegConvertor);
            jpegConvertor.setVisible(true);
        } else {
            JOptionPane.showMessageDialog(this, "Tile selected is not valid for this process");
        }
    }

    private void presetButtonMouseClicked(java.awt.event.MouseEvent evt) {
        if (presetButton.isEnabled()) {
            int x = evt.getX();
            int y = evt.getY();
            long z = evt.getWhen();
            int mo = evt.getModifiers();
            int cc = evt.getClickCount();
            designPresetContext();
            presetButton.dispatchEvent(new java.awt.event.MouseEvent(this.presetButton, MouseEvent.MOUSE_CLICKED, z, mo, x, y, cc, true));
        }
    }

    private void metaDataButtonActionPerformed(java.awt.event.ActionEvent evt) {
        if (ApplicationContext.annotationPanel != null && ApplicationContext.imgPanel != null) {
            ArrayList<DicomTags> dcmTags = DicomTagsReader.getTags(new File(ApplicationContext.imgPanel.getDicomFileUrl()));
            DicomTagsViewer dicomTagsViewer = new DicomTagsViewer(dcmTags);
            Display.alignScreen(dicomTagsViewer);
            dicomTagsViewer.setVisible(true);
        } else {
            JOptionPane.showMessageDialog(this, "Tile selected is not valid for this process");
        }
    }

    private void scoutButtonActionPerformed(java.awt.event.ActionEvent evt) {
        doScout();
    }

    public void doScout() {
        if (!ImagePanel.isDisplayScout()) {
            ImagePanel.setDisplayScout(true);
            LocalizerDelegate localizer = new LocalizerDelegate();
            localizer.drawScoutLineWithBorder();
        } else {
            ImagePanel.setDisplayScout(false);
            LocalizerDelegate.hideScoutLine();
        }
    }

    private void textOverlayMousePressed(java.awt.event.MouseEvent evt) {
        if (textOverlay.isEnabled()) {
            int x = evt.getX();
            int y = evt.getY();
            long z = evt.getWhen();
            int mo = evt.getModifiers();
            int cc = evt.getClickCount();
            textOverlay.dispatchEvent(new java.awt.event.MouseEvent(this.textOverlay, MouseEvent.MOUSE_CLICKED, z, mo, x, y, cc, true));
        }
    }

    private void cube3DButtonMouseClicked(java.awt.event.MouseEvent evt) {
        int x = evt.getX();
        int y = evt.getY();
        long z = evt.getWhen();
        int mo = evt.getModifiers();
        int cc = evt.getClickCount();
        design3DPopup();
        cube3DButton.dispatchEvent(new java.awt.event.MouseEvent(this.cube3DButton, MouseEvent.MOUSE_CLICKED, z, mo, x, y, cc, true));
    }

    private void cube3DButtonActionPerformed(java.awt.event.ActionEvent evt) {
    }

    private void synchronizeButtonActionPerformed(java.awt.event.ActionEvent evt) {
        ApplicationContext.imgPanel.doSynchronize();
    }

    private void arrowButtonActionPerformed(java.awt.event.ActionEvent evt) {
        doRuler(true);
    }

    private void designPresetContext() {
        if (ApplicationContext.annotationPanel != null && ApplicationContext.imgPanel != null) {
            ArrayList presetList = ApplicationContext.databaseRef.getPresetValueForModality(ApplicationContext.imgPanel.getModality());
            jPopupMenu2.removeAll();
            JMenuItem menu = new JMenuItem("PRESETS") {

                @Override
                protected void paintComponent(Graphics grphcs) {
                    grphcs.setFont(new Font("Arial", Font.BOLD, 12));
                    grphcs.setColor(Color.blue);
                    grphcs.drawString(this.getText(), 32, 14);
                }
            };
            menu.setEnabled(false);
            jPopupMenu2.add(menu);
            jPopupMenu2.addSeparator();
            for (int i = 0; i < presetList.size(); i++) {
                final PresetModel presetModel = (PresetModel) presetList.get(i);
                if (!presetModel.getPresetName().equalsIgnoreCase("PRESETNAME")) {
                    JMenuItem menu1 = new JMenuItem(presetModel.getPresetName());
                    jPopupMenu2.add(menu1);
                    menu1.addActionListener(new ActionListener() {

                        public void actionPerformed(ActionEvent e) {
                            ApplicationContext.imgPanel.windowChanged(Integer.parseInt(presetModel.getWindowLevel()), Integer.parseInt(presetModel.getWindowWidth()));
                        }
                    });
                }
            }
            this.setComponentPopupMenu(jPopupMenu1);
        } else {
            JOptionPane.showMessageDialog(this, "Tile selected is not valid for this process");
        }
    }

    private javax.swing.JButton annotationVisibility;

    private javax.swing.JButton arrowButton;

    private javax.swing.JButton clearAllMeasurement;

    private javax.swing.JButton cube3DButton;

    private javax.swing.JButton deleteMeasurement;

    private javax.swing.JButton ellipseButton;

    private javax.swing.JButton exportButton;

    private javax.swing.JButton horizontalFlip;

    private javax.swing.JButton invert;

    public javax.swing.JPopupMenu jPopupMenu1;

    private javax.swing.JPopupMenu jPopupMenu2;

    private javax.swing.JPopupMenu jPopupMenu3;

    private javax.swing.JPopupMenu jPopupMenu4;

    private javax.swing.JToolBar jToolBar3;

    private in.raster.mayam.form.JComboButton layoutButton;

    private javax.swing.JButton leftRotate;

    private javax.swing.JCheckBox loopCheckbox;

    private javax.swing.JSlider loopSlider;

    private javax.swing.JButton metaDataButton;

    private javax.swing.JButton moveMeasurement;

    private javax.swing.JButton panButton;

    private javax.swing.JButton presetButton;

    private javax.swing.JButton probeButton;

    private javax.swing.JButton rectangleButton;

    private javax.swing.JButton reset;

    private javax.swing.JButton rightRotate;

    private javax.swing.JButton rulerButton;

    private javax.swing.JButton scoutButton;

    private javax.swing.JButton stackButton;

    private javax.swing.JButton synchronizeButton;

    private javax.swing.JButton textOverlay;

    private javax.swing.ButtonGroup toolsButtonGroup;

    private javax.swing.JButton verticalFlip;

    private javax.swing.JButton windowing;

    private javax.swing.JButton zoomin;

    private javax.swing.JButton zoomoutButton;

    public void designPopup() {
        JPanel jp = new JPanel(new GridLayout(3, 3));
        JButton jb1 = new JButton("1x1");
        JButton jb2 = new JButton("1x2");
        JButton jb3 = new JButton("1x3");
        JButton jb4 = new JButton("2x1");
        JButton jb5 = new JButton("2x2");
        JButton jb6 = new JButton("2x3");
        JButton jb7 = new JButton("3x1");
        JButton jb8 = new JButton("3x2");
        JButton jb9 = new JButton("3x3");
        jp.add(jb1);
        jp.add(jb2);
        jp.add(jb3);
        jp.add(jb4);
        jp.add(jb5);
        jp.add(jb6);
        jp.add(jb7);
        jp.add(jb8);
        jp.add(jb9);
        jp.setBounds(0, 0, 200, 400);
        jPopupMenu1.add(jp);
        jb1.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jb1ActionPerformed(evt);
            }
        });
        jb2.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jb2ActionPerformed(evt);
            }
        });
        jb3.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jb3ActionPerformed(evt);
            }
        });
        jb4.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jb4ActionPerformed(evt);
            }
        });
        jb5.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jb5ActionPerformed(evt);
            }
        });
        jb6.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jb6ActionPerformed(evt);
            }
        });
        jb7.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jb7ActionPerformed(evt);
            }
        });
        jb8.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jb8ActionPerformed(evt);
            }
        });
        jb9.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jb9ActionPerformed(evt);
            }
        });
    }

    public void jb1ActionPerformed(ActionEvent e) {
        changeLayout(1, 1);
    }

    public void storeAnnotationHook() {
        for (int i = 0; i < ApplicationContext.imgView.jTabbedPane1.getComponentCount(); i++) {
            for (int j = 0; j < ((JPanel) ApplicationContext.imgView.jTabbedPane1.getComponent(i)).getComponentCount(); j++) {
                try {
                    if (((JPanel) ApplicationContext.imgView.jTabbedPane1.getComponent(i)).getComponent(j) instanceof LayeredCanvas) {
                        LayeredCanvas tempCanvas = ((LayeredCanvas) ((JPanel) ApplicationContext.imgView.jTabbedPane1.getComponent(i)).getComponent(j));
                        if (tempCanvas.imgpanel != null) {
                            tempCanvas.imgpanel.storeAnnotation();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void removeAllPanelsFromSelectedTab() {
        for (int i = ((JPanel) ApplicationContext.imgView.jTabbedPane1.getSelectedComponent()).getComponentCount() - 1; i >= 0; i--) {
            ((JPanel) ApplicationContext.imgView.jTabbedPane1.getSelectedComponent()).remove(i);
        }
    }

    public void changeLayout(int row, int col) {
        String siuid;
        LayeredCanvas tempCanvas = null;
        if (((JPanel) ApplicationContext.imgView.jTabbedPane1.getSelectedComponent()).getComponent(0) instanceof LayeredCanvas) {
            tempCanvas = ((LayeredCanvas) ((JPanel) ApplicationContext.imgView.jTabbedPane1.getSelectedComponent()).getComponent(0));
            siuid = tempCanvas.imgpanel.getStudyUID();
        } else {
            tempCanvas = ((LayeredCanvas) ((JPanel) ApplicationContext.imgView.jTabbedPane1.getSelectedComponent()).getComponent(1));
            siuid = tempCanvas.imgpanel.getStudyUID();
        }
        GridLayout g = new GridLayout(row, col);
        ArrayList<Instance> instanceArray = getInstanceArray();
        ArrayList tempRef = ApplicationContext.databaseRef.getUrlBasedOnStudyIUID(siuid);
        for (int i = ((JPanel) ApplicationContext.imgView.jTabbedPane1.getSelectedComponent()).getComponentCount() - 1; i >= 0; i--) {
            ((JPanel) ApplicationContext.imgView.jTabbedPane1.getSelectedComponent()).remove(i);
        }
        ((JPanel) ApplicationContext.imgView.jTabbedPane1.getSelectedComponent()).setLayout(g);
        for (int i = 0; i < (row * col); i++) {
            if (i < tempRef.size()) {
                File file = (File) tempRef.get(i);
                LayeredCanvas canvas = new LayeredCanvas(file.getAbsolutePath());
                ((JPanel) ApplicationContext.imgView.jTabbedPane1.getSelectedComponent()).add(canvas, i);
                for (int x = 0; x < instanceArray.size(); x++) {
                    if (file.getAbsolutePath().equalsIgnoreCase(new DestinationFinder().getFileDestination(instanceArray.get(x).getFilepath()))) {
                        LayeredCanvas tempCanvas1 = ((LayeredCanvas) ((JPanel) ApplicationContext.imgView.jTabbedPane1.getSelectedComponent()).getComponent(i));
                        if (ApplicationContext.databaseRef.getMultiframeStatus() && tempCanvas1.imgpanel.isMulitiFrame()) {
                            if (instanceArray != null && instanceArray.get(x) != null && instanceArray.get(x).getAnnotations() != null && instanceArray.get(x).getAnnotations().get(0) != null) {
                                tempCanvas1.annotationPanel.setAnnotation(instanceArray.get(x).getAnnotations().get(0));
                            }
                        } else {
                            if (instanceArray != null && instanceArray.get(x) != null && instanceArray.get(x).getAnnotation() != null) {
                                tempCanvas1.annotationPanel.setAnnotation(instanceArray.get(x).getAnnotation());
                            }
                        }
                        break;
                    }
                }
            } else {
                LayeredCanvas j = new LayeredCanvas();
                j.setStudyUID(siuid);
                ((JPanel) ApplicationContext.imgView.jTabbedPane1.getSelectedComponent()).add(j, i);
            }
        }
        ((JPanel) ApplicationContext.imgView.jTabbedPane1.getSelectedComponent()).revalidate();
        ((JPanel) ApplicationContext.imgView.jTabbedPane1.getSelectedComponent()).repaint();
        ApplicationContext.imgPanel = ((LayeredCanvas) ((JPanel) ApplicationContext.imgView.jTabbedPane1.getSelectedComponent()).getComponent(0)).imgpanel;
        ApplicationContext.annotationPanel = ((LayeredCanvas) ((JPanel) ApplicationContext.imgView.jTabbedPane1.getSelectedComponent()).getComponent(0)).annotationPanel;
        ApplicationContext.layeredCanvas = ((LayeredCanvas) ((JPanel) ApplicationContext.imgView.jTabbedPane1.getSelectedComponent()).getComponent(0));
        hideLayoutSelectionPopup();
        ((Canvas) ApplicationContext.imgPanel.getCanvas()).setSelection();
    }

    private void hideLayoutSelectionPopup() {
        jPopupMenu1.setVisible(false);
    }

    public ArrayList getInstanceArray() {
        ArrayList<Instance> instanceArray = new ArrayList<Instance>();
        String studyUID = "";
        if (ApplicationContext.imgPanel != null) {
            studyUID = ApplicationContext.imgPanel.getStudyUID();
        } else {
            studyUID = ApplicationContext.layeredCanvas.getStudyUID();
        }
        for (Study study : MainScreen.studyList) {
            if (study.getStudyInstanceUID().equalsIgnoreCase(studyUID)) {
                ArrayList<Series> seriesList = (ArrayList<Series>) study.getSeriesList();
                for (int i = 0; i < seriesList.size(); i++) {
                    Series series = seriesList.get(i);
                    Instance instance = series.getImageList().get(0);
                    instanceArray.add(instance);
                }
            }
        }
        return instanceArray;
    }

    public void jb2ActionPerformed(ActionEvent e) {
        changeLayout(1, 2);
    }

    public void jb3ActionPerformed(ActionEvent e) {
        changeLayout(1, 3);
    }

    public void jb4ActionPerformed(ActionEvent e) {
        changeLayout(2, 1);
    }

    public void jb5ActionPerformed(ActionEvent e) {
        changeLayout(2, 2);
    }

    public void jb6ActionPerformed(ActionEvent e) {
        changeLayout(2, 3);
    }

    public void jb7ActionPerformed(ActionEvent e) {
        changeLayout(3, 1);
    }

    public void jb8ActionPerformed(ActionEvent e) {
        changeLayout(3, 2);
    }

    public void jb9ActionPerformed(ActionEvent e) {
        changeLayout(4, 3);
    }

    public String seriesDir = "";

    public String studyDir = "";

    private void design3DPopup() {
        JMenuItem mpr = new JMenuItem("2D Orthogonal MPR");
        JMenuItem mpr3D = new JMenuItem("3D MPR");
        JMenuItem mip = new JMenuItem("3D MIP");
        JMenuItem surfaceRendering = new JMenuItem("3D Surface Rendering");
        JMenuItem volumeRendering = new JMenuItem("Volume Rendering");
        jPopupMenu4.removeAll();
        jPopupMenu4.add(mpr);
        jPopupMenu4.addSeparator();
        jPopupMenu4.add(mpr3D);
        jPopupMenu4.add(mip);
        jPopupMenu4.add(surfaceRendering);
        jPopupMenu4.add(volumeRendering);
        LayeredCanvas tempCanvas = null;
        surfaceRendering.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                doFileCopy();
                surface = new SurfaceRendering();
                surface.readDicomDir(seriesDir);
                surface.setLocationRelativeTo(ImageToolbar.this);
                surface.setVisible(true);
            }
        });
        mip.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                doFileCopy();
                dcmMip = new DicomMIP();
                dcmMip.readDicomDir(seriesDir);
                dcmMip.setLocationRelativeTo(ImageToolbar.this);
                dcmMip.setVisible(true);
            }
        });
        mpr.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                doFileCopy();
                dcmMPR2D = new DicomMPR2D();
                dcmMPR2D.readDicomDir(seriesDir);
                dcmMPR2D.setLocationRelativeTo(ImageToolbar.this);
                dcmMPR2D.setVisible(true);
            }
        });
        mpr3D.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                doFileCopy();
                mpr3DSlider = new DicomMPR3DSlider();
                mpr3DSlider.readDicom(seriesDir);
                mpr3DSlider.setLocationRelativeTo(ImageToolbar.this);
                mpr3DSlider.setVisible(true);
            }
        });
        volumeRendering.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                doFileCopy();
                dicomVolumeRendering = new DicomVolumeRendering();
                dicomVolumeRendering.readDicomDir(seriesDir);
                dicomVolumeRendering.setLocationRelativeTo(ImageToolbar.this);
                dicomVolumeRendering.setVisible(true);
            }
        });
    }

    public void doFileCopy() {
        String filePath = ApplicationContext.imgPanel.getDicomFileUrl();
        studyDir = new File(filePath).getParent();
        String seriesUID = ApplicationContext.imgPanel.getSeriesUID();
        String instancePath[] = ApplicationContext.imgPanel.getInstancesFilePath();
        String transferSyntaxUID = getTransferSyntaxUID(filePath).trim();
        boolean isCompressed = false;
        if (transferSyntaxUID != null) {
            if (!transferSyntaxUID.equalsIgnoreCase("1.2.840.10008.1.2") && !transferSyntaxUID.equalsIgnoreCase("1.2.840.10008.1.2.2") && !transferSyntaxUID.equalsIgnoreCase("1.2.840.10008.1.2.1")) {
                isCompressed = true;
            }
        }
        if (isCompressed && Platform.getCurrentPlatform().equals(Platform.MAC)) {
            JOptionPane.showMessageDialog(this, "Compressed studies are not supported for this operation.");
            return;
        }
        DestinationFinder finder = new DestinationFinder();
        for (int i = 0; i < instancePath.length; i++) {
            try {
                File sourceFile = new File(finder.getFileDestination(instancePath[i]));
                FileInputStream fis = new FileInputStream(sourceFile);
                seriesDir = studyDir + File.separator + seriesUID;
                File seriesFolder = new File(seriesDir);
                if (!seriesFolder.exists()) {
                    seriesFolder.mkdir();
                }
                if (!isCompressed) {
                    FileOutputStream fout = new FileOutputStream(new File(seriesFolder, sourceFile.getName()));
                    copy(fis, fout);
                } else {
                    String destinationPath = seriesDir + File.separator + sourceFile.getName();
                    String transParam[] = { "--ivle", sourceFile.getAbsolutePath(), destinationPath };
                    if (sourceFile.isFile()) {
                        synchronized (this) {
                            TranscoderMain.main(transParam);
                        }
                    }
                }
            } catch (IOException ex) {
                Logger.getLogger(ImageToolbar.class.getName()).log(Level.SEVERE, null, ex);
            } catch (Exception ex) {
                Logger.getLogger(ImageToolbar.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private String getTransferSyntaxUID(String fileName) {
        String tfsUid = null;
        try {
            File tmpFile = new File(fileName);
            DicomInputStream dis = new DicomInputStream(tmpFile);
            DicomObject dicomObject = dis.readDicomObject();
            tfsUid = dicomObject.getString(Tags.TransferSyntaxUID);
            dis.close();
            return tfsUid;
        } catch (Exception e) {
            return tfsUid;
        }
    }

    private void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[8 * 1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            if (out != null) {
                out.write(buffer, 0, read);
            }
        }
        in.close();
        out.close();
    }

    public void doTextOverlay() {
        if (ApplicationContext.annotationPanel != null && ApplicationContext.imgPanel != null) {
            ApplicationContext.layeredCanvas.textOverlay.toggleTextOverlay();
        } else {
            JOptionPane.showMessageDialog(ImageToolbar.this, "Tile selected is not valid for this process");
        }
    }

    public void textOverlayContext() {
        JMenuItem currentFrame = new JMenuItem("Selected");
        JMenuItem allFrame = new JMenuItem("All");
        jPopupMenu3.add(currentFrame);
        jPopupMenu3.add(allFrame);
        textOverlay.setComponentPopupMenu(jPopupMenu3);
        allFrame.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                LayeredCanvas tempCanvas = null;
                int childCount = ((JPanel) ApplicationContext.imgView.jTabbedPane1.getSelectedComponent()).getComponentCount();
                for (int i = 0; i < childCount; i++) {
                    if (((JPanel) ApplicationContext.imgView.jTabbedPane1.getSelectedComponent()).getComponent(i) instanceof LayeredCanvas) {
                        tempCanvas = ((LayeredCanvas) ((JPanel) ApplicationContext.imgView.jTabbedPane1.getSelectedComponent()).getComponent(i));
                        if (tempCanvas.textOverlay != null) {
                            tempCanvas.textOverlay.toggleTextOverlay();
                        }
                    }
                }
            }
        });
        currentFrame.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (ApplicationContext.annotationPanel != null && ApplicationContext.imgPanel != null) {
                    ApplicationContext.layeredCanvas.textOverlay.toggleTextOverlay();
                } else {
                    JOptionPane.showMessageDialog(ImageToolbar.this, "Tile selected is not valid for this process");
                }
            }
        });
    }
}
