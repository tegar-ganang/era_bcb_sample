package com.google.code.b0rx0r.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JPanel;
import javax.swing.JViewport;
import com.google.code.b0rx0r.ColorScheme;
import com.google.code.b0rx0r.program.Sample;
import com.google.code.b0rx0r.program.SampleListener;
import com.google.code.b0rx0r.program.Slice;
import com.google.code.b0rx0r.program.SnapPoint;
import com.google.code.b0rx0r.program.Triggerable;
import com.google.code.b0rx0r.program.TriggerableListener;

public class Slicer extends JPanel implements SampleListener, TriggerableListener {

    private static final long DONT_CARE = -1;

    private float verticalZoom = 1.0f;

    private float horizontalZoom = 0.01f;

    private int width;

    private SnappointHandlePane snapHP = new SnappointHandlePane();

    private WaveformDisplay waveDisplay = new WaveformDisplay();

    private SliceHandlePane sliceHP = new SliceHandlePane();

    private SnapPoint spSelected;

    private Slice slSelected;

    private Sample sample;

    private List<TriggerableSelectionListener> listeners = new ArrayList<TriggerableSelectionListener>();

    private float horizontalZoomRelative;

    public Slicer() {
        initGui();
    }

    private void initGui() {
        setLayout(new BorderLayout());
        add(snapHP, BorderLayout.NORTH);
        add(waveDisplay, BorderLayout.CENTER);
        add(sliceHP, BorderLayout.SOUTH);
    }

    public Slice getSelectedSlice() {
        return slSelected;
    }

    @Override
    public void addNotify() {
        super.addNotify();
        final JViewport parent = (JViewport) getParent();
        parent.addComponentListener(new ComponentAdapter() {

            @Override
            public void componentResized(ComponentEvent e) {
                setHorizontalZoomRelative(horizontalZoomRelative);
            }
        });
    }

    public void setSample(Sample sample) {
        if (this.sample != sample) {
            if (this.sample != null) this.sample.removeSampleObserver(this);
            this.sample = sample;
            if (this.sample != null) this.sample.addSampleObserver(this);
            updateView(DONT_CARE);
        }
    }

    public void addTriggerableSelectionListener(TriggerableSelectionListener tsl) {
        listeners.add(tsl);
    }

    public void removeTriggerableSelectionListener(TriggerableSelectionListener tsl) {
        listeners.remove(tsl);
    }

    void setSelectedSlice(Slice sl) {
        if (sl != slSelected) {
            slSelected = sl;
            fireSelectionChanged();
            repaint();
        }
    }

    private void fireSelectionChanged() {
        for (TriggerableSelectionListener l : listeners) {
            l.selectionChanged(slSelected);
        }
    }

    public float getVerticalZoom() {
        return verticalZoom;
    }

    public void setVerticalZoom(float verticalZoom) {
        if (this.verticalZoom != verticalZoom) {
            this.verticalZoom = verticalZoom;
            updateView(DONT_CARE);
        }
    }

    public float getHorizontalZoom() {
        return horizontalZoom;
    }

    public void setHorizontalZoom(float horizontalZoom) {
        if (this.horizontalZoom != horizontalZoom) {
            long midpointSample = getMidpointSample();
            this.horizontalZoom = horizontalZoom;
            updateView(midpointSample);
        }
    }

    private Rectangle getView() {
        return ((JViewport) getParent()).getViewRect();
    }

    private long getMidpointSample() {
        long x = (long) ((getView().x + getView().width / 2) / horizontalZoom);
        return x;
    }

    private void setMidpointSample(long midpointSample) {
        int x = (int) (midpointSample * horizontalZoom);
        System.out.println("midpoint " + midpointSample + " => " + x);
        Rectangle r = getView();
        x -= r.width / 2;
        r.x = x;
        scrollRectToVisible(r);
    }

    /**
	 * set the RELATIVE horizontal zoom. 0.0 = fit into available viewport 1.0 =
	 * 1sample/pixel
	 */
    public void setHorizontalZoomRelative(float horizontalZoom) {
        this.horizontalZoomRelative = horizontalZoom;
        float samples = sample == null ? 1000 : sample.getFloatSampleBuffer().getSampleCount();
        float viewport = ((JViewport) getParent()).getWidth();
        float maxZoom = 1.0f;
        float minZoom = viewport / samples;
        float zoom = minZoom + horizontalZoom * (maxZoom - minZoom);
        setHorizontalZoom(zoom);
    }

    private void updateView(long midpointSample) {
        if (sample == null) return;
        width = (int) (sample.getFloatSampleBuffer().getSampleCount() * horizontalZoom);
        waveDisplay.updateView();
        snapHP.updateView();
        sliceHP.updateView();
        if (midpointSample != DONT_CARE) setMidpointSample(midpointSample);
        repaint();
    }

    private class WaveformDisplay extends JPanel {

        float[][] waveData = new float[][] { { 0f } };

        public void updateView() {
            waveData = SimpleWaveformDisplay.calculate(sample.getFloatSampleBuffer(), width, getHeight());
            repaint();
        }

        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (sample == null) return;
            int nHeight = getHeight();
            int startX = (int) (sample.getStartOffset() * horizontalZoom);
            int endX = (int) (sample.getEndOffset() * horizontalZoom);
            Rectangle clip = g.getClipBounds();
            int nWidth = clip.width + clip.x;
            int nChannels = sample.getFloatSampleBuffer().getChannelCount();
            g.setColor(ColorScheme.SLICER_WAVEFORM_CLIPPED_BACKGROUND);
            g.fillRect(0, 0, startX, nHeight);
            g.fillRect(endX, 0, nWidth - endX, nHeight);
            System.out.println(horizontalZoom);
            g.setColor(getForeground());
            for (int c = 0; c < nChannels; c++) {
                for (int x = clip.x; x < clip.x + clip.width; x++) {
                    int i = (int) (x / horizontalZoom);
                    int i2 = (int) ((x + 1) / horizontalZoom);
                    float sum = 0;
                    for (int j = i; j < i2; j++) {
                        sum += Math.abs(sample.getFloatSampleBuffer().getChannel(c)[j]);
                    }
                    sum /= i2 - i;
                    int value = (int) (sum * nHeight / nChannels);
                    int y1 = (nHeight * c / nChannels) + (nHeight - 2 * value) / 2;
                    int y2 = y1 + 2 * value;
                    g.drawLine(x, y1, x, y2);
                }
            }
            for (SnapPoint point : sample.getSnapPoints()) {
                int offset = point.getOffset();
                offset = (int) (offset * horizontalZoom);
                Color c = point == spSelected ? getForeground().brighter() : getForeground();
                g.setColor(c);
                g.drawLine(offset, 0, offset, nHeight);
            }
        }
    }

    private class SnappointHandlePane extends JPanel {

        private Dimension dim = new Dimension(100, 8);

        public SnappointHandlePane() {
            setBackground(ColorScheme.SLICER_SNAPPOINT_HANDLE_PANE_BACKGROUND_COLOR);
            MouseAdapter ml = new MouseAdapter() {

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
                        Point p = e.getPoint();
                        int offset = (int) (p.getX() / horizontalZoom);
                        SnapPoint existing = findSnapPoint(offset);
                        if (existing != null) {
                            SnapPoint before = sample.findSnapPointBefore(existing.getOffset());
                            SnapPoint after = sample.findSnapPointAfter(existing.getOffset());
                            Set<Slice> toDelete = new HashSet<Slice>();
                            for (Slice s : sample.getSlices()) {
                                if (s.getStart() == existing) {
                                    if (before != null) s.setStart(before); else toDelete.add(s);
                                }
                                if (s.getEnd() == existing) {
                                    if (after != null) s.setEnd(after); else toDelete.add(s);
                                }
                            }
                            for (Slice s : toDelete) sample.deleteSlice(s);
                            sample.deleteSnapPoint(existing);
                        } else {
                            sample.addSnapPointAtNearestZeroCrossing(offset);
                        }
                    }
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    spSelected = findSnapPoint((int) (e.getPoint().x / horizontalZoom));
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    spSelected = null;
                    Slicer.this.repaint();
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (spSelected != null) {
                        int offset = (int) (e.getPoint().x / horizontalZoom);
                        if (offset < 0) offset = 0;
                        if (offset > sample.getFloatSampleBuffer().getSampleCount()) offset = sample.getFloatSampleBuffer().getSampleCount();
                        spSelected.setOffset(sample.getNearestZeroCrossing(offset));
                        Slicer.this.repaint();
                    }
                }
            };
            addMouseListener(ml);
            addMouseMotionListener(ml);
        }

        protected SnapPoint findSnapPoint(int offset) {
            int min = offset - (int) (5 / horizontalZoom);
            int max = offset + (int) (5 / horizontalZoom);
            for (SnapPoint sp : sample.getSnapPoints()) {
                if (sp.getOffset() >= min && sp.getOffset() <= max) {
                    return sp;
                }
            }
            return null;
        }

        public void updateView() {
            dim.width = width;
            setPreferredSize(dim);
            setMinimumSize(dim);
            setMaximumSize(dim);
            invalidate();
            revalidate();
            repaint();
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            if (sample == null) return;
            for (SnapPoint point : sample.getSnapPoints()) {
                int offset = point.getOffset();
                offset = (int) (offset * horizontalZoom);
                Color c = point == spSelected ? getForeground().brighter() : getForeground();
                g.setColor(c);
                g.drawLine(offset - 5, 2, offset, getHeight() - 2);
                g.drawLine(offset + 5, 2, offset, getHeight() - 2);
                g.drawLine(offset - 5, 2, offset + 5, 2);
            }
        }
    }

    private class SliceHandlePane extends JPanel {

        private Dimension dim = new Dimension(100, 16);

        public SliceHandlePane() {
            setBackground(ColorScheme.SLICER_SLICE_HANDLE_PANE_BACKGROUND_COLOR);
            MouseAdapter ml = new MouseAdapter() {

                @Override
                public void mouseClicked(MouseEvent e) {
                    Point p = e.getPoint();
                    int offset = (int) (p.getX() / horizontalZoom);
                    Slice sl = sample.findSlice(offset);
                    if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
                        if (sl == null) {
                            SnapPoint start = sample.findSnapPointBefore(offset);
                            SnapPoint end = sample.findSnapPointAfter(offset);
                            if (start != null && end != null) {
                                sl = sample.addSliceBetween(start, end);
                                setSelectedSlice(sl);
                                repaint();
                            }
                        } else {
                            sample.deleteSlice(sl);
                        }
                    } else if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 1) {
                        if (sl != null) {
                            if (slSelected == sl) {
                                setSelectedSlice(null);
                            } else {
                                setSelectedSlice(sl);
                            }
                            repaint();
                        }
                    }
                }
            };
            addMouseListener(ml);
            addMouseMotionListener(ml);
        }

        public void updateView() {
            dim.width = width;
            setPreferredSize(dim);
            setMinimumSize(dim);
            setMaximumSize(dim);
            invalidate();
            revalidate();
            repaint();
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            if (sample == null) return;
            for (Slice s : sample.getSlices()) {
                paintSlice(g, s);
            }
            if (slSelected != null) paintSlice(g, slSelected);
        }

        private void paintSlice(Graphics g, Slice s) {
            boolean selected = s == slSelected;
            Color sliceColor = s.getColor();
            if (sliceColor == null) sliceColor = ColorScheme.DEFAULT_SLICE_COLOR;
            if (selected) sliceColor = sliceColor.brighter();
            g.setColor(sliceColor);
            SnapPoint start = s.getStart();
            SnapPoint end = s.getEnd();
            int x1 = (int) (start.getOffset() * horizontalZoom);
            int x2 = (int) ((end.getOffset() - start.getOffset()) * horizontalZoom);
            g.fillRect(x1, 0, x2, getHeight() - 1);
            if (s.getName() != null) {
                g.setColor(ColorScheme.SLICER_SLICE_HANDLE_PANE_TEXT_COLOR);
                Shape clip = g.getClip();
                g.setClip(x1, 0, x2, getHeight() - 1);
                g.drawString(s.getName(), x1 + 2, getHeight() - 3);
                g.setClip(clip);
            }
            if (selected) {
                g.setColor(ColorScheme.SLICER_SLICE_HANDLE_PANE_SLICE_SELECTED_LINE_COLOR);
                g.drawRect(x1, 0, x2, getHeight() - 1);
            }
        }
    }

    @Override
    public void snapPointAdded(SnapPoint sp) {
        repaint();
    }

    @Override
    public void snapPointMoved(SnapPoint sp) {
        repaint();
    }

    @Override
    public void snapPointRemoved(SnapPoint sp) {
        repaint();
    }

    @Override
    public void sliceAdded(Slice sp) {
        sp.addTriggerableListener(this);
        repaint();
    }

    @Override
    public void sliceMoved(Slice sp) {
        repaint();
    }

    @Override
    public void sliceRemoved(Slice sp) {
        sp.removeTriggerableListener(this);
        if (slSelected == sp) spSelected = null;
        repaint();
    }

    @Override
    public void triggerableChanged(Triggerable triggerable) {
        repaint();
    }
}
