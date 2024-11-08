package co.edu.unal.ungrid.image.dicom.display.wave;

import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import co.edu.unal.ungrid.image.dicom.core.Attribute;
import co.edu.unal.ungrid.image.dicom.core.AttributeList;
import co.edu.unal.ungrid.image.dicom.core.AttributeTreeBrowser;
import co.edu.unal.ungrid.image.dicom.core.BinaryInputStream;
import co.edu.unal.ungrid.image.dicom.core.DicomInputStream;
import co.edu.unal.ungrid.image.dicom.core.ServiceObjectPairClass;
import co.edu.unal.ungrid.image.dicom.core.TagFromName;
import co.edu.unal.ungrid.image.dicom.display.ApplicationFrame;
import co.edu.unal.ungrid.image.dicom.scpecg.SCPECG;
import co.edu.unal.ungrid.image.dicom.scpecg.SCPTreeBrowser;

/**
 * <p>
 * This class is an entire application for displaying and viewing DICOM and SCP
 * ECG waveforms.
 * </p>
 * 
 * 
 */
public class EcgViewer extends ApplicationFrame {

    public static final long serialVersionUID = 200609220000001L;

    /**
	 * @uml.property name="milliMetresPerPixel"
	 */
    private final float milliMetresPerPixel = (float) (25.4 / 72);

    /**
	 * @uml.property name="defaultHeightOfTileInMicroVolts"
	 */
    private final int defaultHeightOfTileInMicroVolts = 2000;

    /**
	 * @uml.property name="minimumHeightOfTileInMicroVolts"
	 */
    private final int minimumHeightOfTileInMicroVolts = 1000;

    /**
	 * @uml.property name="maximumHeightOfTileInMicroVolts"
	 */
    private final int maximumHeightOfTileInMicroVolts = 5000;

    /**
	 * @uml.property name="minorIntervalHeightOfTileInMicroVolts"
	 */
    private final int minorIntervalHeightOfTileInMicroVolts = 500;

    /**
	 * @uml.property name="majorIntervalHeightOfTileInMicroVolts"
	 */
    private final int majorIntervalHeightOfTileInMicroVolts = 1000;

    /**
	 * @uml.property name="heightOfTileSliderLabel"
	 */
    private final String heightOfTileSliderLabel = "Height of tile in uV";

    /**
	 * @uml.property name="defaultHorizontalScalingInMilliMetresPerSecond"
	 */
    private final int defaultHorizontalScalingInMilliMetresPerSecond = 25;

    /**
	 * @uml.property name="minimumHorizontalScalingInMilliMetresPerSecond"
	 */
    private final int minimumHorizontalScalingInMilliMetresPerSecond = 10;

    /**
	 * @uml.property name="maximumHorizontalScalingInMilliMetresPerSecond"
	 */
    private final int maximumHorizontalScalingInMilliMetresPerSecond = 50;

    /**
	 * @uml.property name="minorIntervalHorizontalScalingInMilliMetresPerSecond"
	 */
    private final int minorIntervalHorizontalScalingInMilliMetresPerSecond = 5;

    /**
	 * @uml.property name="majorIntervalHorizontalScalingInMilliMetresPerSecond"
	 */
    private final int majorIntervalHorizontalScalingInMilliMetresPerSecond = 10;

    /**
	 * @uml.property name="horizontalScalingSliderLabel"
	 */
    private final String horizontalScalingSliderLabel = "mm/S";

    /**
	 * @uml.property name="defaultVerticalScalingInMilliMetresPerMilliVolt"
	 */
    private final int defaultVerticalScalingInMilliMetresPerMilliVolt = 10;

    /**
	 * @uml.property name="minimumVerticalScalingInMilliMetresPerMilliVolt"
	 */
    private final int minimumVerticalScalingInMilliMetresPerMilliVolt = 5;

    /**
	 * @uml.property name="maximumVerticalScalingInMilliMetresPerMilliVolt"
	 */
    private final int maximumVerticalScalingInMilliMetresPerMilliVolt = 25;

    /**
	 * @uml.property name="minorIntervalVerticalScalingInMilliMetresPerMilliVolt"
	 */
    private final int minorIntervalVerticalScalingInMilliMetresPerMilliVolt = 5;

    /**
	 * @uml.property name="majorIntervalVerticalScalingInMilliMetresPerMilliVolt"
	 */
    private final int majorIntervalVerticalScalingInMilliMetresPerMilliVolt = 10;

    /**
	 * @uml.property name="verticalScalingSliderLabel"
	 */
    private final String verticalScalingSliderLabel = "mm/mV";

    /**
	 * @uml.property name="maximumSliderWidth"
	 */
    private final int maximumSliderWidth = 320;

    /**
	 * @uml.property name="maximumSliderHeight"
	 */
    private final int maximumSliderHeight = 100;

    /**
	 * @uml.property name="minimumAttributeTreePaneWidth"
	 */
    private final int minimumAttributeTreePaneWidth = 200;

    /**
	 * @param application
	 * @param sourceECG
	 * @param scrollPaneOfDisplayedECG
	 * @param scrollPaneOfAttributeTree
	 * @param requestedHeightOfTileInMicroVolts
	 * @param requestedHorizontalScalingInMilliMetresPerSecond
	 * @param requestedVerticalScalingInMilliMetresPerMilliVolt
	 */
    private void loadSourceECGIntoScrollPane(JFrame application, SourceECG sourceECG, JScrollPane scrollPaneOfDisplayedECG, JScrollPane scrollPaneOfAttributeTree, int requestedHeightOfTileInMicroVolts, int requestedHorizontalScalingInMilliMetresPerSecond, int requestedVerticalScalingInMilliMetresPerMilliVolt) {
        Cursor was = application.getCursor();
        application.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        int numberOfChannels = sourceECG.getNumberOfChannels();
        int numberOfSamplesPerChannel = sourceECG.getNumberOfSamplesPerChannel();
        float samplingIntervalInMilliSeconds = sourceECG.getSamplingIntervalInMilliSeconds();
        int nTilesPerColumn = numberOfChannels;
        int nTilesPerRow = 1;
        int timeOffsetInMilliSeconds = 0;
        float requestedHeightOfTileInMilliVolts = (float) (requestedHeightOfTileInMicroVolts) / 1000f;
        float horizontalPixelsPerMilliSecond = (float) (requestedHorizontalScalingInMilliMetresPerSecond) / (1000 * milliMetresPerPixel);
        float verticalPixelsPerMilliVolt = (float) (requestedVerticalScalingInMilliMetresPerMilliVolt) / milliMetresPerPixel;
        float widthOfPixelInMilliSeconds = 1 / horizontalPixelsPerMilliSecond;
        float widthOfSampleInPixels = samplingIntervalInMilliSeconds / widthOfPixelInMilliSeconds;
        int maximumWidthOfRowInSamples = numberOfSamplesPerChannel * nTilesPerRow;
        int maximumWidthOfRowInPixels = (int) (maximumWidthOfRowInSamples * widthOfSampleInPixels);
        float heightOfPixelInMilliVolts = 1 / verticalPixelsPerMilliVolt;
        int wantECGPanelheight = (int) (nTilesPerColumn * requestedHeightOfTileInMilliVolts / heightOfPixelInMilliVolts);
        ECGPanel pg = new ECGPanel(sourceECG.getSamples(), numberOfChannels, numberOfSamplesPerChannel, sourceECG.getChannelNames(), nTilesPerColumn, nTilesPerRow, samplingIntervalInMilliSeconds, sourceECG.getAmplitudeScalingFactorInMilliVolts(), horizontalPixelsPerMilliSecond, verticalPixelsPerMilliVolt, timeOffsetInMilliSeconds, sourceECG.getDisplaySequence(), maximumWidthOfRowInPixels, wantECGPanelheight);
        pg.setPreferredSize(new Dimension(maximumWidthOfRowInPixels, wantECGPanelheight));
        scrollPaneOfDisplayedECG.setViewportView(pg);
        application.setCursor(was);
    }

    /**
	 * @param inputFileName
	 * @param application
	 * @param scrollPaneOfDisplayedECG
	 * @param scrollPaneOfAttributeTree
	 * @return a SourceECG, or null if load failed
	 */
    private SourceECG loadDicomFile(String inputFileName, JFrame application, JScrollPane scrollPaneOfDisplayedECG, JScrollPane scrollPaneOfAttributeTree) {
        SourceECG sourceECG = null;
        if (inputFileName != null) {
            try {
                DicomInputStream i = new DicomInputStream(new BufferedInputStream(new FileInputStream(inputFileName)));
                AttributeList list = new AttributeList();
                list.read(i);
                i.close();
                new AttributeTreeBrowser(list, scrollPaneOfAttributeTree);
                Attribute a = list.get(TagFromName.MediaStorageSopClassUid);
                String useSOPClassUID = (a != null && a.getValueMultiplicity() == 1) ? a.getStringValues()[0] : null;
                if (useSOPClassUID == null) {
                    a = list.get(TagFromName.SopClassUid);
                    useSOPClassUID = (a != null && a.getValueMultiplicity() == 1) ? a.getStringValues()[0] : null;
                }
                if (ServiceObjectPairClass.isWaveform(useSOPClassUID)) {
                    sourceECG = new DicomSourceECG(list);
                } else {
                    throw new Exception("unsupported SOP Class " + useSOPClassUID);
                }
            } catch (Exception e) {
            }
        }
        return sourceECG;
    }

    /**
	 * @param inputFileName
	 * @param application
	 * @param scrollPaneOfDisplayedECG
	 * @param scrollPaneOfAttributeTree
	 * @return a SourceECG, or null if load failed
	 */
    private SourceECG loadSCPECGFile(String inputFileName, JFrame application, JScrollPane scrollPaneOfDisplayedECG, JScrollPane scrollPaneOfAttributeTree) {
        SourceECG sourceECG = null;
        if (inputFileName != null) {
            try {
                BinaryInputStream i = new BinaryInputStream(new BufferedInputStream(new FileInputStream(inputFileName)), false);
                SCPECG scpecg = new SCPECG(i, false);
                new SCPTreeBrowser(scpecg, scrollPaneOfAttributeTree);
                sourceECG = new SCPSourceECG(scpecg, true);
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        }
        return sourceECG;
    }

    /**
	 * @param inputFileName
	 * @param application
	 * @param scrollPaneOfDisplayedECG
	 * @param scrollPaneOfAttributeTree
	 * @return a SourceECG, or null if load failed
	 */
    private SourceECG loadECGFile(String inputFileName, JFrame application, JScrollPane scrollPaneOfDisplayedECG, JScrollPane scrollPaneOfAttributeTree) {
        Cursor was = application.getCursor();
        application.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        SourceECG sourceECG = null;
        if (inputFileName == null) {
            JFileChooser chooser = new JFileChooser(lastDirectoryPath);
            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                inputFileName = chooser.getSelectedFile().getAbsolutePath();
                lastDirectoryPath = chooser.getCurrentDirectory().getAbsolutePath();
            }
        }
        scrollPaneOfDisplayedECG.setViewportView(null);
        scrollPaneOfDisplayedECG.repaint();
        scrollPaneOfAttributeTree.setViewportView(null);
        scrollPaneOfAttributeTree.repaint();
        sourceECG = loadDicomFile(inputFileName, application, scrollPaneOfDisplayedECG, scrollPaneOfAttributeTree);
        if (sourceECG == null) {
            sourceECG = loadSCPECGFile(inputFileName, application, scrollPaneOfDisplayedECG, scrollPaneOfAttributeTree);
        }
        application.setCursor(was);
        return sourceECG;
    }

    /**
	 * @uml.property name="lastDirectoryPath"
	 */
    private String lastDirectoryPath;

    /**
	 * @param inputFileName
	 * @param application
	 * @param scrollPaneOfDisplayedECG
	 * @param scrollPaneOfAttributeTree
	 * @return a SourceECG, or null if load failed
	 */
    private class ResetScalingToDefaultsActionListener implements ActionListener {

        /***/
        CommonScalingSliderChangeListener scalingChangeListener;

        /***/
        int defaultHeightOfTileInMicroVolts;

        /***/
        int defaultHorizontalScalingInMilliMetresPerSecond;

        /***/
        int defaultVerticalScalingInMilliMetresPerMilliVolt;

        /**
		 * @param scalingChangeListener
		 * @param defaultHeightOfTileInMicroVolts
		 * @param defaultHorizontalScalingInMilliMetresPerSecond
		 * @param defaultVerticalScalingInMilliMetresPerMilliVolt
		 */
        public ResetScalingToDefaultsActionListener(CommonScalingSliderChangeListener scalingChangeListener, int defaultHeightOfTileInMicroVolts, int defaultHorizontalScalingInMilliMetresPerSecond, int defaultVerticalScalingInMilliMetresPerMilliVolt) {
            this.scalingChangeListener = scalingChangeListener;
            this.defaultHeightOfTileInMicroVolts = defaultHeightOfTileInMicroVolts;
            this.defaultHorizontalScalingInMilliMetresPerSecond = defaultHorizontalScalingInMilliMetresPerSecond;
            this.defaultVerticalScalingInMilliMetresPerMilliVolt = defaultVerticalScalingInMilliMetresPerMilliVolt;
        }

        /**
		 * @param event
		 */
        public void actionPerformed(ActionEvent event) {
            scalingChangeListener.setValuesAndRedraw(defaultHeightOfTileInMicroVolts, defaultHorizontalScalingInMilliMetresPerSecond, defaultVerticalScalingInMilliMetresPerMilliVolt);
        }
    }

    /**
	 * @author Administrator
	 */
    private class CommonScalingSliderChangeListener implements ChangeListener {

        /***/
        JSlider heightOfTileSlider;

        /***/
        JSlider horizontalScalingSlider;

        /***/
        JSlider verticalScalingSlider;

        /***/
        JFrame application;

        /***/
        SourceECG sourceECG;

        /***/
        JScrollPane scrollPaneOfDisplayedECG;

        /***/
        JScrollPane scrollPaneOfAttributeTree;

        /***/
        int requestedHeightOfTileInMicroVolts;

        /***/
        int requestedHorizontalScalingInMilliMetresPerSecond;

        /***/
        int requestedVerticalScalingInMilliMetresPerMilliVolt;

        /**
		 * @param sourceECG
		 * @param application
		 * @param scrollPaneOfDisplayedECG
		 * @param scrollPaneOfAttributeTree
		 * @param heightOfTileSlider
		 * @param horizontalScalingSlider
		 * @param verticalScalingSlider
		 * @param requestedHeightOfTileInMicroVolts
		 * @param requestedHorizontalScalingInMilliMetresPerSecond
		 * @param requestedVerticalScalingInMilliMetresPerMilliVolt
		 */
        public CommonScalingSliderChangeListener(JFrame application, SourceECG sourceECG, JScrollPane scrollPaneOfDisplayedECG, JScrollPane scrollPaneOfAttributeTree, JSlider heightOfTileSlider, JSlider horizontalScalingSlider, JSlider verticalScalingSlider, int requestedHeightOfTileInMicroVolts, int requestedHorizontalScalingInMilliMetresPerSecond, int requestedVerticalScalingInMilliMetresPerMilliVolt) {
            this.heightOfTileSlider = heightOfTileSlider;
            this.horizontalScalingSlider = horizontalScalingSlider;
            this.verticalScalingSlider = verticalScalingSlider;
            this.requestedHeightOfTileInMicroVolts = requestedHeightOfTileInMicroVolts;
            this.requestedHorizontalScalingInMilliMetresPerSecond = requestedHorizontalScalingInMilliMetresPerSecond;
            this.requestedVerticalScalingInMilliMetresPerMilliVolt = requestedVerticalScalingInMilliMetresPerMilliVolt;
            this.application = application;
            this.sourceECG = sourceECG;
            this.scrollPaneOfDisplayedECG = scrollPaneOfDisplayedECG;
            this.scrollPaneOfAttributeTree = scrollPaneOfAttributeTree;
        }

        /**
		 * @param e
		 */
        public void stateChanged(ChangeEvent e) {
            JSlider slider = (JSlider) (e.getSource());
            if (!slider.getValueIsAdjusting()) {
                boolean changed = false;
                int value = slider.getValue();
                if (slider == heightOfTileSlider) {
                    if (value != requestedHeightOfTileInMicroVolts) {
                        requestedHeightOfTileInMicroVolts = value;
                        changed = true;
                    }
                } else if (slider == horizontalScalingSlider) {
                    if (value != requestedHorizontalScalingInMilliMetresPerSecond) {
                        requestedHorizontalScalingInMilliMetresPerSecond = value;
                        changed = true;
                    }
                } else if (slider == verticalScalingSlider) {
                    if (value != requestedVerticalScalingInMilliMetresPerMilliVolt) {
                        requestedVerticalScalingInMilliMetresPerMilliVolt = value;
                        changed = true;
                    }
                }
                if (changed) {
                    loadSourceECGIntoScrollPane(application, sourceECG, scrollPaneOfDisplayedECG, scrollPaneOfAttributeTree, requestedHeightOfTileInMicroVolts, requestedHorizontalScalingInMilliMetresPerSecond, requestedVerticalScalingInMilliMetresPerMilliVolt);
                }
            }
        }

        /**
		 * @param requestedHeightOfTileInMicroVolts
		 * @param requestedHorizontalScalingInMilliMetresPerSecond
		 * @param requestedVerticalScalingInMilliMetresPerMilliVolt
		 */
        public void setValuesAndRedraw(int requestedHeightOfTileInMicroVolts, int requestedHorizontalScalingInMilliMetresPerSecond, int requestedVerticalScalingInMilliMetresPerMilliVolt) {
            heightOfTileSlider.setValue(requestedHeightOfTileInMicroVolts);
            horizontalScalingSlider.setValue(requestedHorizontalScalingInMilliMetresPerSecond);
            verticalScalingSlider.setValue(requestedVerticalScalingInMilliMetresPerMilliVolt);
            boolean changed = false;
            if (this.requestedHeightOfTileInMicroVolts != requestedHeightOfTileInMicroVolts) {
                this.requestedHeightOfTileInMicroVolts = requestedHeightOfTileInMicroVolts;
                changed = true;
            }
            if (this.requestedHorizontalScalingInMilliMetresPerSecond != requestedHorizontalScalingInMilliMetresPerSecond) {
                this.requestedHorizontalScalingInMilliMetresPerSecond = requestedHorizontalScalingInMilliMetresPerSecond;
                changed = true;
            }
            if (this.requestedVerticalScalingInMilliMetresPerMilliVolt != requestedVerticalScalingInMilliMetresPerMilliVolt) {
                this.requestedVerticalScalingInMilliMetresPerMilliVolt = requestedVerticalScalingInMilliMetresPerMilliVolt;
                changed = true;
            }
            if (changed) {
                loadSourceECGIntoScrollPane(application, sourceECG, scrollPaneOfDisplayedECG, scrollPaneOfAttributeTree, requestedHeightOfTileInMicroVolts, requestedHorizontalScalingInMilliMetresPerSecond, requestedVerticalScalingInMilliMetresPerMilliVolt);
            }
        }
    }

    /**
	 * @param parent
	 * @param initial
	 * @param minimum
	 * @param maximum
	 * @param major
	 * @param minor
	 * @param label
	 */
    private final JSlider addCommonSlider(JPanel parent, GridBagLayout layout, GridBagConstraints constraints, int initial, int minimum, int maximum, int major, int minor, String labelText) {
        JSlider slider = new JSlider(minimum, maximum, initial);
        slider.setPaintLabels(true);
        slider.setPaintTicks(true);
        slider.setPaintTrack(true);
        slider.setMajorTickSpacing(major);
        slider.setMinorTickSpacing(minor);
        slider.setSnapToTicks(true);
        JLabel label = new JLabel(labelText);
        parent.add(label);
        layout.setConstraints(label, constraints);
        parent.add(slider);
        layout.setConstraints(slider, constraints);
        slider.setMaximumSize(new Dimension(maximumSliderWidth, maximumSliderHeight));
        return slider;
    }

    /**
	 * @param title
	 * @param inputFileName
	 */
    private void doCommonConstructorStuff(String title, String inputFileName) {
        JScrollPane scrollPaneOfDisplayedECG = new JScrollPane();
        JScrollPane scrollPaneOfAttributeTree = new JScrollPane();
        SourceECG sourceECG = loadECGFile(inputFileName, this, scrollPaneOfDisplayedECG, scrollPaneOfAttributeTree);
        if (sourceECG != null) {
            loadSourceECGIntoScrollPane(this, sourceECG, scrollPaneOfDisplayedECG, scrollPaneOfAttributeTree, defaultHeightOfTileInMicroVolts, defaultHorizontalScalingInMilliMetresPerSecond, defaultVerticalScalingInMilliMetresPerMilliVolt);
        }
        JPanel controlsPanel = new JPanel();
        controlsPanel.setPreferredSize(new Dimension(minimumAttributeTreePaneWidth, 50));
        GridBagLayout controlsPanelLayout = new GridBagLayout();
        controlsPanel.setLayout(controlsPanelLayout);
        GridBagConstraints controlsPanelConstraints = new GridBagConstraints();
        controlsPanelConstraints.gridwidth = GridBagConstraints.REMAINDER;
        JSlider heightOfTileSlider = addCommonSlider(controlsPanel, controlsPanelLayout, controlsPanelConstraints, defaultHeightOfTileInMicroVolts, minimumHeightOfTileInMicroVolts, maximumHeightOfTileInMicroVolts, majorIntervalHeightOfTileInMicroVolts, minorIntervalHeightOfTileInMicroVolts, heightOfTileSliderLabel);
        JSlider horizontalScalingSlider = addCommonSlider(controlsPanel, controlsPanelLayout, controlsPanelConstraints, defaultHorizontalScalingInMilliMetresPerSecond, minimumHorizontalScalingInMilliMetresPerSecond, maximumHorizontalScalingInMilliMetresPerSecond, majorIntervalHorizontalScalingInMilliMetresPerSecond, minorIntervalHorizontalScalingInMilliMetresPerSecond, horizontalScalingSliderLabel);
        JSlider verticalScalingSlider = addCommonSlider(controlsPanel, controlsPanelLayout, controlsPanelConstraints, defaultVerticalScalingInMilliMetresPerMilliVolt, minimumVerticalScalingInMilliMetresPerMilliVolt, maximumVerticalScalingInMilliMetresPerMilliVolt, majorIntervalVerticalScalingInMilliMetresPerMilliVolt, minorIntervalVerticalScalingInMilliMetresPerMilliVolt, verticalScalingSliderLabel);
        CommonScalingSliderChangeListener commonScalingSliderChangeListener = new CommonScalingSliderChangeListener(this, sourceECG, scrollPaneOfDisplayedECG, scrollPaneOfAttributeTree, heightOfTileSlider, horizontalScalingSlider, verticalScalingSlider, defaultHeightOfTileInMicroVolts, defaultHorizontalScalingInMilliMetresPerSecond, defaultVerticalScalingInMilliMetresPerMilliVolt);
        heightOfTileSlider.addChangeListener(commonScalingSliderChangeListener);
        horizontalScalingSlider.addChangeListener(commonScalingSliderChangeListener);
        verticalScalingSlider.addChangeListener(commonScalingSliderChangeListener);
        JButton defaultButton = new JButton("Default");
        defaultButton.setToolTipText("Reset scaling to defaults");
        controlsPanel.add(defaultButton);
        defaultButton.addActionListener(new ResetScalingToDefaultsActionListener(commonScalingSliderChangeListener, defaultHeightOfTileInMicroVolts, defaultHorizontalScalingInMilliMetresPerSecond, defaultVerticalScalingInMilliMetresPerMilliVolt));
        controlsPanelLayout.setConstraints(defaultButton, controlsPanelConstraints);
        Container content = getContentPane();
        JSplitPane attributeTreeAndControls = new JSplitPane(JSplitPane.VERTICAL_SPLIT, controlsPanel, scrollPaneOfAttributeTree);
        JSplitPane attributeTreeAndDisplayedECG = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, attributeTreeAndControls, scrollPaneOfDisplayedECG);
        content.add(attributeTreeAndDisplayedECG);
        pack();
        setVisible(true);
    }

    /**
	 * @param title
	 * @param inputFileName
	 */
    private EcgViewer(String title, String inputFileName) {
        super(title, null);
        doCommonConstructorStuff(title, inputFileName);
    }

    /**
	 * <p>
	 * The method to invoke the application.
	 * </p>
	 * 
	 * @param arg
	 *            optionally, a single file which may be a DICOM or an SCP-ECG
	 *            waveform; if absent a file dialog is presented
	 */
    public static void main(String arg[]) {
        String inputFileName = null;
        if (arg.length == 1) {
            inputFileName = arg[0];
        }
        new EcgViewer("ECG Viewer", inputFileName);
    }
}
