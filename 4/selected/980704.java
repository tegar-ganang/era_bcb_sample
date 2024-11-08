package com.google.code.b0rx0r.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.TreeSet;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import jvst.wrapper.gui.RotaryKnob;
import org.jdesktop.swingx.VerticalLayout;
import com.google.code.b0rx0r.PlayQueue;
import com.google.code.b0rx0r.Stoppable;
import com.google.code.b0rx0r.program.ProgramManager;
import com.google.code.b0rx0r.program.Sample;
import com.google.code.b0rx0r.program.SampleDataContainer;
import com.google.code.b0rx0r.program.Sequencable;
import com.google.code.b0rx0r.program.Slice;
import com.google.code.b0rx0r.program.SnapPoint;
import com.google.code.b0rx0r.program.Triggerable;

public class SlicingPanel extends JPanel implements Previewable {

    private Slicer slicer = new Slicer();

    private JSlider slZoom = new JSlider(JSlider.VERTICAL, 0, 100, 0);

    private JPanel pSampleEdit = new JPanel();

    private JCheckBox cbDoPitch = new JCheckBox("make loopy");

    private JSpinner spLengthBeats = new JSpinner(new SpinnerNumberModel(4, 1, 64, 1));

    private RotaryKnob rkBeatDetect = new RotaryKnob();

    private JPanel pRouting = new JPanel();

    private OutputRoutingMatrix matrix = new OutputRoutingMatrix();

    private ProgramManager programManager;

    private PlayQueue playQueue;

    private Sample sample;

    private float[] sampleLevels;

    public SlicingPanel(ProgramManager programManager, PlayQueue playQueue) {
        this.programManager = programManager;
        this.playQueue = playQueue;
        initGui();
    }

    private void initGui() {
        Dimension min = new Dimension(600, 200);
        setMinimumSize(min);
        slZoom.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent arg0) {
                float value = slZoom.getValue();
                float zoom = value / 100f;
                slicer.setHorizontalZoomRelative(zoom * zoom * zoom * zoom);
            }
        });
        setLayout(new BorderLayout());
        JScrollPane spSlicer = new JScrollPane(slicer, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        spSlicer.setMinimumSize(min);
        spSlicer.setPreferredSize(min);
        add(spSlicer, BorderLayout.CENTER);
        add(slZoom, BorderLayout.EAST);
        JPanel pSouth = new JPanel(new BorderLayout());
        add(pSouth, BorderLayout.SOUTH);
        JTabbedPane tpSouth = new JTabbedPane();
        pSouth.add(tpSouth, BorderLayout.CENTER);
        tpSouth.addTab("Edit Sample", pSampleEdit);
        tpSouth.add("Route Sample/Slice", pRouting);
        pRouting.setBorder(BorderFactory.createTitledBorder(Messages.getString("SlicingPanel.SliceDetails")));
        pRouting.add(matrix);
        pRouting.setEnabled(false);
        JPanel pLooping = new JPanel();
        pLooping.setBorder(BorderFactory.createTitledBorder("Looping"));
        pLooping.add(cbDoPitch);
        pLooping.add(spLengthBeats);
        pSampleEdit.add(pLooping);
        JToggleButton tbPlay = new JToggleButton("play");
        pLooping.add(tbPlay);
        tbPlay.addActionListener(new ActionListener() {

            private Stoppable stopMe;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (stopMe == null) {
                    stopMe = playQueue.enqueueSequencableLooping(sample, 127);
                } else {
                    stopMe.stop();
                    stopMe = null;
                }
            }
        });
        cbDoPitch.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                updatePitch();
            }
        });
        spLengthBeats.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                updatePitch();
            }
        });
        slicer.addTriggerableSelectionListener(new TriggerableSelectionListener() {

            @Override
            public void selectionChanged(Triggerable selected) {
                if (selected != null && selected instanceof SampleDataContainer) {
                    SampleDataContainer s = (SampleDataContainer) selected;
                    pRouting.setEnabled(true);
                    matrix.setCurrent(s);
                } else {
                    pRouting.setEnabled(true);
                    matrix.setCurrent(sample);
                }
            }
        });
        JPanel pAutoSlice = new JPanel();
        pAutoSlice.setBorder(BorderFactory.createTitledBorder(Messages.getString("SlicingPanel.AutoSlice")));
        pAutoSlice.setLayout(new VerticalLayout());
        pAutoSlice.add(rkBeatDetect);
        pSampleEdit.add(pAutoSlice);
        rkBeatDetect.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                if (sampleLevels == null || sampleLevels.length != calculateSampleLevelArrayLength()) computeSampleLevels();
                float value = 1.0f - rkBeatDetect.getValue();
                sample.clearAllSnapPoints();
                sliceWithThreshold(value);
                addSlicesForAllSnapPoints();
                repaint();
            }

            private void addSlicesForAllSnapPoints() {
                SnapPoint spPrev = null;
                for (SnapPoint sp : new TreeSet<SnapPoint>(sample.getSnapPoints())) {
                    if (spPrev != null) {
                        sample.addSliceBetween(spPrev, sp);
                    }
                    spPrev = sp;
                }
            }

            private void sliceWithThreshold(float treshold) {
                if (sampleLevels[0] > treshold) sample.addSnapPointAt(0);
                for (int i = 1; i < sampleLevels.length; i++) {
                    float diff = sampleLevels[i] - sampleLevels[i - 1];
                    if (diff > treshold) {
                        sample.addSnapPointAtNearestZeroCrossing(i * LEVEL_WINDOW_SIZE);
                    }
                }
                sample.addSnapPointAt(sample.getLength() - 1);
            }

            private static final int LEVEL_WINDOW_SIZE = 512;

            private void computeSampleLevels() {
                sampleLevels = new float[calculateSampleLevelArrayLength()];
                float[] sampleData = sample.getFloatSampleBuffer().getChannel(0);
                for (int i = 0; i < sampleLevels.length; i++) {
                    float max = 0;
                    for (int j = sample.getStartOffset() + i * LEVEL_WINDOW_SIZE; j < ((i + 1) * LEVEL_WINDOW_SIZE); j++) {
                        if (j >= sampleData.length) break;
                        float current = Math.abs(sampleData[j]);
                        if (current > max) max = current;
                    }
                    sampleLevels[i] = max;
                }
            }

            private int calculateSampleLevelArrayLength() {
                return (sample.getEndOffset() - sample.getStartOffset()) / LEVEL_WINDOW_SIZE;
            }
        });
    }

    protected void updatePitch() {
        if (!cbDoPitch.isSelected()) {
            sample.setPlaybackSpeed(1.0f);
        } else {
            int lengthBeats = (Integer) spLengthBeats.getValue();
            float tempoBPM = programManager.getTempoProvider().getTempo();
            float samplesPerSecond = programManager.getTempoProvider().getSampleRate();
            float samplesPerBeat = samplesPerSecond * 60 / tempoBPM;
            float lengthSamples = lengthBeats * samplesPerBeat;
            sample.setPlaybackSpeed(lengthSamples / sample.getLength());
        }
    }

    public void addTriggerableSelectionListener(TriggerableSelectionListener tsl) {
        slicer.addTriggerableSelectionListener(tsl);
    }

    public void removeTriggerableSelectionListener(TriggerableSelectionListener tsl) {
        slicer.removeTriggerableSelectionListener(tsl);
    }

    public void setSample(Sample sample) {
        if (sample != this.sample) {
            this.sample = sample;
            this.sampleLevels = null;
            slicer.setSample(sample);
            slicer.setHorizontalZoomRelative(0.0f);
            pRouting.setEnabled(true);
            matrix.setCurrent(sample);
        }
    }

    @Override
    public void triggerPreview(PlayQueue pq) {
        Sequencable s = sample;
        if (slicer.getSelectedSlice() != null) {
            s = slicer.getSelectedSlice();
        }
        if (s != null) pq.enqueueSequencable(s, 127);
    }

    public void setSelectedSlice(Slice s) {
        slicer.setSelectedSlice(s);
    }
}
