package ch.laoe.plugin;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import ch.laoe.clip.AChannel;
import ch.laoe.clip.AChannelPlotter;
import ch.laoe.clip.AClip;
import ch.laoe.clip.AClipPlotter;
import ch.laoe.clip.ALayer;
import ch.laoe.clip.ALayerPlotter;
import ch.laoe.ui.Debug;
import ch.laoe.ui.GControlTextSF;
import ch.laoe.ui.GControlTextY;
import ch.laoe.ui.GLanguage;
import ch.laoe.ui.GToolkit;
import ch.laoe.ui.Laoe;
import ch.oli4.ui.UiCartesianLayout;
import ch.oli4.ui.control.UiControlEvent;
import ch.oli4.ui.control.UiControlListener;
import ch.oli4.ui.control.UiControlText;

/***********************************************************

This file is part of LAoE.

LAoE is free software; you can redistribute it and/or modify it
under the terms of the GNU General Public License as published
by the Free Software Foundation; either version 2 of the License,
or (at your option) any later version.

LAoE is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with LAoE; if not, write to the Free Software Foundation,
Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA


Class:			GPZoom
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	plugin to zoom the clip-view. 
					mouse action table: 
	------------------------------------------------------------
					press/drag/release	click				double-click
	------------------------------------------------------------
	no key		translate xy			-					-
	shift			zoom in x 				zoom out x		autoscale x
	shift&ctrl	zoom in xy  			zoom out xy		autoscale xy
	ctrl			zoom in y				zoom out y		autoscale y
	------------------------------------------------------------

History:
Date:			Description:									Autor:
30.08.00		erster Entwurf									oli4
10.11.00		better zoom support of plotters is
				used here										oli4
30.01.01		extended zoom-functions controlled by
				mouse 											oli4
24.03.01		individual layers / common clip zoom	oli4
14.04.01		cursors added									oli4
09.12.01		autoscale-modes and selectable x/y added	oli4
10.02.02		add different y autoscale-modes			oli4

***********************************************************/
public class GPZoom extends GPluginFrame {

    public GPZoom(GPluginHandler ph) {
        super(ph);
        initGui();
    }

    public String getName() {
        return "zoom";
    }

    public JMenuItem createMenuItem() {
        return super.createMenuItem(KeyEvent.VK_1);
    }

    protected boolean isFocussingMouseEvents() {
        return true;
    }

    public void reload() {
        super.reload();
        if (pluginHandler.getFocussedClipEditor() != null) {
            reloadZoomFrame();
        }
    }

    private BasicStroke dashedStroke, normalStroke;

    private AChannelPlotter mouseChannelPlotter;

    private int mouseZoomXPress, mouseZoomYPress, mouseZoomXReleas, mouseZoomYReleas, mouseZoomXOld, mouseZoomYOld;

    private boolean shiftActive, ctrlActive;

    private boolean mouseDown;

    public void paintOntoClip(Graphics2D g2d, Rectangle rect) {
        if (mouseDown) {
            int xMin = Math.min(mouseZoomXPress, mouseZoomXReleas);
            int xDelta = Math.abs(mouseZoomXReleas - mouseZoomXPress);
            int yMin = Math.min(mouseZoomYPress, mouseZoomYReleas);
            int yDelta = Math.abs(mouseZoomYReleas - mouseZoomYPress);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
            if (shiftActive && ctrlActive) {
                g2d.setColor(Color.white);
                g2d.setStroke(normalStroke);
                g2d.drawRect(xMin, yMin, xDelta, yDelta);
                g2d.setColor(Color.black);
                g2d.setStroke(dashedStroke);
                g2d.drawRect(xMin, yMin, xDelta, yDelta);
            } else if (shiftActive) {
                g2d.setColor(Color.white);
                g2d.setStroke(normalStroke);
                g2d.drawRect(xMin, 0, xDelta, rect.height - 1);
                g2d.setColor(Color.black);
                g2d.setStroke(dashedStroke);
                g2d.drawRect(xMin, 0, xDelta, rect.height - 1);
            } else if (ctrlActive) {
                g2d.setColor(Color.white);
                g2d.setStroke(normalStroke);
                g2d.drawRect(0, yMin, rect.width - 1, yDelta);
                g2d.setColor(Color.black);
                g2d.setStroke(dashedStroke);
                g2d.drawRect(0, yMin, rect.width - 1, yDelta);
            } else {
                g2d.setColor(Color.white);
                g2d.setStroke(normalStroke);
                g2d.drawLine(mouseZoomXPress, mouseZoomYPress, mouseZoomXReleas, mouseZoomYReleas);
                g2d.setColor(Color.black);
                g2d.setStroke(dashedStroke);
                g2d.drawLine(mouseZoomXPress, mouseZoomYPress, mouseZoomXReleas, mouseZoomYReleas);
            }
        }
    }

    public void mousePressed(MouseEvent e) {
        Debug.println(5, "mouse pressed: start zoom functionality");
        mouseDown = true;
        mouseZoomXPress = e.getPoint().x;
        mouseZoomYPress = e.getPoint().y;
        mouseZoomXReleas = mouseZoomXPress;
        mouseZoomYReleas = mouseZoomYPress;
        mouseZoomXOld = mouseZoomXPress;
        mouseZoomYOld = mouseZoomYPress;
        shiftActive = GToolkit.isShiftKey(e);
        ctrlActive = GToolkit.isCtrlKey(e);
        ALayer l = getFocussedClip().getSelectedLayer();
        int i = l.getPlotter().getInsideChannelIndex(e.getPoint());
        mouseChannelPlotter = l.getChannel(i).getPlotter();
    }

    public void mouseReleased(MouseEvent e) {
        mouseDown = false;
        double sx1 = mouseChannelPlotter.graphToSampleX(mouseZoomXPress);
        double sx2 = mouseChannelPlotter.graphToSampleX(mouseZoomXReleas);
        double sxMin = Math.min(sx1, sx2);
        double sxMax = Math.max(sx1, sx2);
        int deltaX = mouseZoomXReleas - mouseZoomXPress;
        float sy1 = mouseChannelPlotter.graphToSampleY(mouseZoomYPress);
        float sy2 = mouseChannelPlotter.graphToSampleY(mouseZoomYReleas);
        float syMin = Math.min(sy1, sy2);
        float syMax = Math.max(sy1, sy2);
        int deltaY = mouseZoomYReleas - mouseZoomYPress;
        AClipPlotter cp = getFocussedClip().getPlotter();
        ALayerPlotter lp = getFocussedClip().getSelectedLayer().getPlotter();
        if (!shiftActive && !ctrlActive) {
        } else {
            if (shiftActive) {
                if (Math.abs(deltaX) > 0) {
                    Debug.println(5, "mouse released: zoom x into rectangle");
                    cp.setXRange(sxMin, sxMax - sxMin);
                }
            }
            if (ctrlActive) {
                if (Math.abs(deltaY) > 0) {
                    Debug.println(5, "mouse released: zoom y into rectangle");
                    if (individualY.isSelected()) {
                        lp.setYRange(syMin, syMax - syMin);
                    } else {
                        cp.setYRange(syMin, syMax - syMin);
                    }
                }
            }
        }
        if (e.getClickCount() == 0) {
            reloadZoomFrame();
            reloadFocussedClipEditor();
        }
    }

    public void mouseClicked(MouseEvent e) {
        Debug.println(5, "zoom out on click");
        AClipPlotter cp = getFocussedClip().getPlotter();
        ALayerPlotter lp = getFocussedClip().getSelectedLayer().getPlotter();
        if (e.getClickCount() == 1) {
            if (GToolkit.isShiftKey(e)) {
                Debug.println(5, "mouse clicked: zoom out x");
                cp.zoomX(.5f);
            }
            if (GToolkit.isCtrlKey(e)) {
                Debug.println(5, "mouse clicked: zoom out y");
                if (individualY.isSelected()) {
                    lp.zoomY(.5f);
                } else {
                    cp.zoomY(.5f);
                }
            }
        } else if (e.getClickCount() == 2) {
            if (GToolkit.isShiftKey(e)) {
                Debug.println(5, "mouse double-clicked: autoscale visible x");
                cp.autoScaleX();
            }
            if (GToolkit.isCtrlKey(e)) {
                Debug.println(5, "mouse double-clicked: autoscale visible y");
                if (individualY.isSelected()) {
                    lp.autoScaleY();
                } else {
                    cp.autoScaleY();
                }
            }
        }
        reloadZoomFrame();
        reloadFocussedClipEditor();
    }

    public void mouseEntered(MouseEvent e) {
        ((Component) e.getSource()).setCursor(actualCursor);
    }

    public void mouseDragged(MouseEvent e) {
        mouseZoomXOld = mouseZoomXReleas;
        mouseZoomYOld = mouseZoomYReleas;
        mouseZoomXReleas = e.getPoint().x;
        mouseZoomYReleas = e.getPoint().y;
        if (!shiftActive && !ctrlActive) {
            double sx1 = mouseChannelPlotter.graphToSampleX(mouseZoomXReleas);
            double sx2 = mouseChannelPlotter.graphToSampleX(mouseZoomXOld);
            float sy1 = mouseChannelPlotter.graphToSampleY(mouseZoomYReleas);
            float sy2 = mouseChannelPlotter.graphToSampleY(mouseZoomYOld);
            AClipPlotter cp = getFocussedClip().getPlotter();
            ALayerPlotter lp = getFocussedClip().getSelectedLayer().getPlotter();
            Debug.println(5, "mouse released: move xy offset");
            if (individualY.isSelected()) {
                lp.translateYOffset(sy2 - sy1);
            } else {
                cp.translateYOffset(sy2 - sy1);
            }
            cp.translateXOffset(sx2 - sx1);
            reloadZoomFrame();
            pluginHandler.getFocussedClipEditor().reload();
        } else {
            reloadZoomFrame();
            repaintFocussedClipEditor();
        }
    }

    public void mouseMoved(MouseEvent e) {
        Cursor c;
        if (GToolkit.isShiftKey(e)) {
            if (GToolkit.isCtrlKey(e)) {
                c = xyCursor;
            } else {
                c = xCursor;
            }
        } else {
            if (GToolkit.isCtrlKey(e)) {
                c = yCursor;
            } else {
                c = moveCursor;
            }
        }
        if (c != actualCursor) {
            actualCursor = c;
            ((Component) e.getSource()).setCursor(actualCursor);
        }
    }

    public void mouseWheelMoved(MouseWheelEvent e) {
        ALayer l = getFocussedClip().getSelectedLayer();
        int i = l.getPlotter().getInsideChannelIndex(e.getPoint());
        AChannelPlotter chp = l.getChannel(i).getPlotter();
        double xc = chp.graphToSampleX(e.getX());
        double yc = chp.graphToSampleY(e.getY());
        int m = e.getWheelRotation();
        AClipPlotter cp = getFocussedClip().getPlotter();
        ALayerPlotter lp = getFocussedClip().getSelectedLayer().getPlotter();
        final double factor = 1.1f;
        if (GToolkit.isShiftKey(e)) {
            if (GToolkit.isCtrlKey(e)) {
                cp.zoomX(m > 0 ? factor : 1f / factor, xc);
                if (individualY.isSelected()) {
                    lp.zoomY(m > 0 ? factor : 1f / factor, yc);
                } else {
                    cp.zoomY(m > 0 ? factor : 1f / factor, yc);
                }
            } else {
                cp.zoomX(m > 0 ? factor : 1f / factor, xc);
            }
        } else {
            if (GToolkit.isCtrlKey(e)) {
                if (individualY.isSelected()) {
                    lp.zoomY(m > 0 ? factor : 1f / factor, yc);
                } else {
                    cp.zoomY(m > 0 ? factor : 1f / factor, yc);
                }
            } else {
                final double shiftFactor = 0.035f;
                if (individualY.isSelected()) {
                    cp.translateXOffset(((AChannelPlotter) lp.getLayerModel().getChannel(0).getPlotter()).getXLength() * (m > 0 ? shiftFactor : -shiftFactor));
                } else {
                    cp.translateXOffset(((AChannelPlotter) cp.getClipModel().getLayer(0).getChannel(0).getPlotter()).getXLength() * (m > 0 ? shiftFactor : -shiftFactor));
                }
            }
        }
        reloadZoomFrame();
        pluginHandler.getFocussedClipEditor().reload();
    }

    private UiControlText scaleX, scaleY, offsetX, offsetY;

    private JButton autoScale;

    private JCheckBox individualY, autoX, autoY;

    private JComboBox xAutoMode, yAutoMode;

    private EventDispatcher eventDispatcher;

    private Cursor moveCursor, xCursor, yCursor, xyCursor, actualCursor;

    public void initGui() {
        moveCursor = createCustomCursor("zoomMoveCursor");
        xCursor = createCustomCursor("zoomXCursor");
        yCursor = createCustomCursor("zoomYCursor");
        xyCursor = createCustomCursor("zoomXYCursor");
        actualCursor = null;
        float dash[] = { 4.f, 4.f };
        dashedStroke = new BasicStroke(1.f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 1.f, dash, 0.f);
        normalStroke = new BasicStroke();
        JPanel p = new JPanel();
        frame.getContentPane().add(p);
        UiCartesianLayout cl = new UiCartesianLayout(p, 10, 5);
        cl.setPreferredCellSize(new Dimension(35, 35));
        p.setLayout(cl);
        autoX = new JCheckBox(GLanguage.translate("x"));
        autoX.setSelected(true);
        cl.add(autoX, 2, 0, 4, 1);
        autoY = new JCheckBox(GLanguage.translate("y"));
        autoY.setSelected(true);
        cl.add(autoY, 6, 0, 4, 1);
        cl.add(new JLabel(GLanguage.translate("offset")), 0, 1, 2, 1);
        offsetX = new GControlTextSF(Laoe.getInstance(), 9, true, true);
        offsetX.setDataRange(-1e9, 1e9);
        offsetX.setData(1);
        offsetX.setUnit("s");
        cl.add(offsetX, 2, 1, 4, 1);
        offsetY = new GControlTextY(Laoe.getInstance(), 9, true, true);
        offsetY.setDataRange(-1e9, 1e9);
        offsetY.setData(1);
        offsetY.setUnit("%");
        cl.add(offsetY, 6, 1, 4, 1);
        cl.add(new JLabel(GLanguage.translate("range")), 0, 2, 2, 1);
        scaleX = new GControlTextSF(Laoe.getInstance(), 9, true, true);
        scaleX.setDataRange(0, 1e9);
        scaleX.setData(1);
        scaleX.setUnit("s");
        cl.add(scaleX, 2, 2, 4, 1);
        scaleY = new GControlTextY(Laoe.getInstance(), 9, true, true);
        scaleY.setDataRange(0, 1e9);
        scaleY.setData(1);
        scaleY.setUnit("%");
        cl.add(scaleY, 6, 2, 4, 1);
        cl.add(new JLabel(GLanguage.translate("mode")), 0, 3, 2, 1);
        String xAutoModeItems[] = { GLanguage.translate("wholeClip"), GLanguage.translate("loopPoints"), GLanguage.translate("measurePoints"), GLanguage.translate("selections") };
        xAutoMode = new JComboBox(xAutoModeItems);
        xAutoMode.setSelectedIndex(0);
        cl.add(xAutoMode, 2, 3, 4, 1);
        String yAutoModeItems[] = { GLanguage.translate("wholeClip"), GLanguage.translate("zoomedRange"), GLanguage.translate("sampleWidth"), GLanguage.translate("selections") };
        yAutoMode = new JComboBox(yAutoModeItems);
        yAutoMode.setSelectedIndex(1);
        cl.add(yAutoMode, 6, 3, 4, 1);
        individualY = new JCheckBox(GLanguage.translate("yIndividual"));
        cl.add(individualY, 0, 4, 4, 1);
        autoScale = new JButton(GLanguage.translate("autoscale"));
        cl.add(autoScale, 4, 4, 4, 1);
        pack();
        eventDispatcher = new EventDispatcher();
        scaleX.addControlListener(eventDispatcher);
        scaleY.addControlListener(eventDispatcher);
        offsetX.addControlListener(eventDispatcher);
        offsetY.addControlListener(eventDispatcher);
        individualY.addActionListener(eventDispatcher);
        autoScale.addActionListener(eventDispatcher);
    }

    private void reloadZoomFrame() {
        AChannelPlotter chp = getFocussedClip().getSelectedLayer().getChannel(0).getPlotter();
        scaleX.setData(chp.getXLength());
        scaleY.setData(chp.getYLength());
        offsetX.setData(chp.getXOffset());
        offsetY.setData(chp.getYOffset());
    }

    private class EventDispatcher implements UiControlListener, ActionListener {

        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == individualY) {
                Debug.println(1, "plugin " + getName() + " [individual y] clicked");
                AClipPlotter.setAutoScaleIndividualYEnabled(individualY.isSelected());
            } else if (e.getSource() == autoScale) {
                Debug.println(1, "plugin " + getName() + " [autoscale] clicked");
                AClip c = getFocussedClip();
                AClipPlotter cp = c.getPlotter();
                ALayer l = c.getSelectedLayer();
                AChannel ch = l.getChannel(0);
                AChannelPlotter chp = ch.getPlotter();
                if (autoX.isSelected()) {
                    double offset, length;
                    switch(xAutoMode.getSelectedIndex()) {
                        case 0:
                            cp.autoScaleX();
                            break;
                        case 1:
                            offset = c.getAudio().getLoopStartPointer();
                            length = c.getAudio().getLoopEndPointer() - c.getAudio().getLoopStartPointer();
                            cp.setXRange(offset - .03f * length, length * 1.06f);
                            break;
                        case 2:
                            offset = GPMeasure.getLowerCursor();
                            length = GPMeasure.getHigherCursor() - GPMeasure.getLowerCursor();
                            cp.setXRange(offset - .03f * length, length * 1.06f);
                            break;
                        case 3:
                            offset = l.getSelection().getLowestSelectedIndex();
                            length = l.getSelection().getHighestSelectedIndex() - l.getSelection().getLowestSelectedIndex();
                            cp.setXRange(offset - .03f * length, length * 1.06f);
                            break;
                    }
                }
                if (autoY.isSelected()) {
                    int xOffset, xLength;
                    float maxValue;
                    switch(yAutoMode.getSelectedIndex()) {
                        case 0:
                            cp.autoScaleY();
                            break;
                        case 1:
                            xOffset = (int) chp.getXOffset();
                            xLength = (int) chp.getXLength();
                            cp.autoScaleY(xOffset, xLength);
                            break;
                        case 2:
                            maxValue = 1 << (c.getSampleWidth() - 1);
                            cp.setYRange(-maxValue * 1.03f, 2 * maxValue * 1.03f);
                            break;
                        case 3:
                            xOffset = l.getSelection().getLowestSelectedIndex();
                            xLength = l.getSelection().getHighestSelectedIndex() - l.getSelection().getLowestSelectedIndex();
                            cp.autoScaleY(xOffset, xLength);
                            break;
                    }
                }
                reloadZoomFrame();
                reloadFocussedClipEditor();
            }
        }

        public void onDataChanging(UiControlEvent e) {
        }

        public void onDataChanged(UiControlEvent e) {
            AClip c = getFocussedClip();
            AClipPlotter cp = c.getPlotter();
            ALayerPlotter lp = c.getSelectedLayer().getPlotter();
            if ((e.getSource() == scaleX) || (e.getSource() == offsetX)) {
                Debug.println(1, "plugin " + getName() + " [scale x] changed");
                if (individualY.isSelected()) {
                    lp.setXRange((float) offsetX.getData(), (float) scaleX.getData());
                } else {
                    cp.setXRange((float) offsetX.getData(), (float) scaleX.getData());
                }
            } else if ((e.getSource() == scaleY) || (e.getSource() == offsetY)) {
                Debug.println(1, "plugin " + getName() + " [scale y] changed");
                if (individualY.isSelected()) {
                    lp.setYRange((float) offsetY.getData(), (float) scaleY.getData());
                } else {
                    cp.setYRange((float) offsetY.getData(), (float) scaleY.getData());
                }
            }
            reloadZoomFrame();
            reloadFocussedClipEditor();
        }

        public void onValidate(UiControlEvent e) {
        }
    }
}
