package vehikel.ide.views.recorder;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.Panel;
import org.eclipse.draw2d.Polyline;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.RectangleFigure;
import org.eclipse.draw2d.XYLayout;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import vehikel.recorder.RecorderChannel;
import vehikel.recorder.RecorderModel;
import vehikel.schema.IOType;
import vehikel.schema.PortScalingType;
import vehikel.schema.RGBType;

public class RecorderEditPart extends AbstractGraphicalEditPart implements Runnable {

    private static final int ELEMENTS_PER_CHANNEL = 4;

    private static final int CAPTION_VPOS = 5;

    private static final int CAPTION_HIGHT = 15;

    private static final int CAPTION_LENGTH = 110;

    private RecorderModel recorderModel;

    private final Set<VisibleChannel> visibleChannels = new HashSet<VisibleChannel>();

    private int ticks;

    private class VisibleChannel {

        IOType io;

        RecorderChannel channel;

        int visualIndex;

        VisibleChannel(int visualIndex, IOType io) {
            this.visualIndex = visualIndex;
            this.io = io;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof VisibleChannel && io.getPublicName().equals(((VisibleChannel) obj).io.getPublicName());
        }

        @Override
        public int hashCode() {
            return io.getPublicName().hashCode();
        }
    }

    public RecorderEditPart(RecorderModel recorderModel) {
        this.recorderModel = recorderModel;
        new Thread(this).start();
    }

    public synchronized void addChannel(IOType io) {
        VisibleChannel newVisibleChannel = new VisibleChannel(visibleChannels.size(), io);
        for (Iterator<VisibleChannel> iterator = visibleChannels.iterator(); iterator.hasNext(); ) {
            VisibleChannel channel = iterator.next();
            if (channel.equals(newVisibleChannel)) {
                return;
            }
        }
        visibleChannels.add(newVisibleChannel);
        newVisibleChannel.channel = recorderModel.getChannelByName(newVisibleChannel.io.getPublicName());
        Panel panel = (Panel) getFigure();
        Color fg = ColorConstants.black;
        Polyline polyline = new Polyline();
        RGBType channelColor = newVisibleChannel.io.getRecorder().getPresentationColor();
        if (channelColor != null) {
            fg = new Color(null, channelColor.getRed(), channelColor.getGreen(), channelColor.getBlue());
            polyline.setForegroundColor(fg);
        }
        polyline.setLineWidth(newVisibleChannel.io.getPort().getBitLength() > 1 ? 2 : 1);
        polyline.setOpaque(true);
        panel.add(polyline);
        org.eclipse.draw2d.Label captionValue = new Label("");
        captionValue.setLocation(new Point(7 + CAPTION_HIGHT + 2 + CAPTION_LENGTH * (visibleChannels.size() - 1), CAPTION_VPOS + CAPTION_HIGHT));
        captionValue.setSize(CAPTION_LENGTH - (CAPTION_HIGHT + 2), CAPTION_HIGHT);
        captionValue.setLabelAlignment(PositionConstants.LEFT);
        panel.add(captionValue);
        org.eclipse.draw2d.RectangleFigure captionRect = new RectangleFigure();
        captionRect.setLocation(new Point(7 + CAPTION_LENGTH * (visibleChannels.size() - 1), CAPTION_VPOS));
        captionRect.setSize(CAPTION_HIGHT, CAPTION_HIGHT);
        captionRect.setForegroundColor(ColorConstants.black);
        captionRect.setBackgroundColor(fg);
        captionRect.setFill(true);
        panel.add(captionRect);
        org.eclipse.draw2d.Label captionLabel = new Label(io.getPublicName());
        captionLabel.setLocation(new Point(7 + CAPTION_HIGHT + 2 + CAPTION_LENGTH * (visibleChannels.size() - 1), CAPTION_VPOS));
        captionLabel.setSize(CAPTION_LENGTH - (CAPTION_HIGHT + 2), CAPTION_HIGHT);
        captionLabel.setLabelAlignment(PositionConstants.LEFT);
        panel.add(captionLabel);
    }

    public synchronized void removeAllChannels() {
        Panel panel = (Panel) getFigure();
        int cx = visibleChannels.size();
        while (--cx >= 0) {
            int ex = ELEMENTS_PER_CHANNEL;
            while (--ex >= 0) {
                panel.remove((IFigure) panel.getChildren().get(ELEMENTS_PER_CHANNEL * cx + ex));
            }
        }
        visibleChannels.clear();
    }

    @Override
    public void setModel(Object model) {
        super.setModel(model);
        recorderModel = (RecorderModel) model;
    }

    @Override
    protected IFigure createFigure() {
        final Panel panel = new Panel();
        panel.setOpaque(true);
        panel.setLayoutManager(new XYLayout());
        panel.setBackgroundColor(ColorConstants.white);
        return panel;
    }

    @Override
    protected synchronized void refreshVisuals() {
        Panel panel = (Panel) getFigure();
        ++ticks;
        int visualHeight = panel.getSize().height;
        int visualWidth = panel.getSize().width;
        Iterator<VisibleChannel> iv = visibleChannels.iterator();
        while (iv.hasNext()) {
            VisibleChannel visibleChannel = iv.next();
            PortScalingType scaling = visibleChannel.io.getScaling();
            RecorderChannel.Result result;
            if (visibleChannel.io.getPort().getBitLength() > 1) {
                result = visibleChannel.channel.scaleDeepCopy(scaling.getMinCooked(), scaling.getMaxCooked(), visualHeight);
            } else {
                result = visibleChannel.channel.scaleDeepCopy(0, 1, 10);
            }
            int[] data = result.getScaled();
            Polyline polyline = (Polyline) panel.getChildren().get(ELEMENTS_PER_CHANNEL * visibleChannel.visualIndex);
            PointList points = polyline.getPoints();
            int pls = points.size();
            for (int rx = 0; rx < data.length; rx++) {
                if (rx < pls) {
                    Point point = points.getPoint(rx);
                    point.x = rx - (pls - visualWidth);
                    point.y = visualHeight - data[rx];
                    points.setPoint(point, rx);
                } else {
                    points.addPoint(new Point(rx - (pls - visualWidth), visualHeight - data[rx]));
                }
            }
            polyline.setPoints(points);
            org.eclipse.draw2d.Label captionValue = (Label) panel.getChildren().get(ELEMENTS_PER_CHANNEL * visibleChannel.visualIndex + 1);
            if (result.getMin() != Integer.MAX_VALUE) {
                captionValue.setText(result.getMin() + ", " + result.getActualValue() + ", " + result.getMax());
            } else {
                captionValue.setText("min, avr, max");
            }
        }
    }

    public void run() {
        while (true) {
            try {
                Thread.sleep(100);
                Display.getDefault().syncExec(new Runnable() {

                    public void run() {
                        refreshVisuals();
                    }
                });
            } catch (InterruptedException ex) {
                System.err.println(ex);
                ex.printStackTrace();
            }
        }
    }

    @Override
    protected void createEditPolicies() {
    }
}
