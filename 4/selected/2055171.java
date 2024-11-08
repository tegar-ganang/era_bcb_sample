package ch.laoe.plugin;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import javax.swing.JMenuItem;
import ch.laoe.clip.AChannel;
import ch.laoe.clip.ALayerSelection;
import ch.laoe.operation.AOMove;
import ch.laoe.ui.GLanguage;
import ch.laoe.ui.LProgressViewer;
import ch.laoe.ui.GToolkit;

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


Class:			GPMove
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	plugin to move the selection.  

History:
Date:			Description:									Autor:
13.04.01		first draft										oli4

***********************************************************/
public class GPMove extends GPlugin {

    public GPMove(GPluginHandler ph) {
        super(ph);
        initGraphics();
        initCursors();
    }

    public String getName() {
        return "move";
    }

    protected boolean isFocussingMouseEvents() {
        return true;
    }

    public boolean isVisible() {
        return true;
    }

    private Cursor moveCursor, moveSelectionCursor;

    private Cursor actualCursor;

    private void initCursors() {
        moveCursor = createCustomCursor("moveCursor");
        moveSelectionCursor = createCustomCursor("moveSelectionCursor");
        actualCursor = null;
    }

    private void setCursor(MouseEvent e, Cursor c) {
        if (c != actualCursor) {
            actualCursor = c;
            ((Component) e.getSource()).setCursor(actualCursor);
        }
    }

    private boolean ctrlActive;

    private boolean mouseDown;

    private AChannel channel;

    private int channelIndex;

    private int pressedX, draggedX;

    private int draggedGraphX;

    private int pressedSelectionOffset;

    public void mousePressed(MouseEvent e) {
        mouseDown = true;
        channelIndex = getFocussedClip().getSelectedLayer().getPlotter().getInsideChannelIndex(e.getPoint());
        channel = getFocussedClip().getSelectedLayer().getChannel(channelIndex);
        pressedX = (int) channel.getPlotter().graphToSampleX(e.getPoint().x);
        pressedSelectionOffset = channel.getSelection().getOffset();
        ctrlActive = GToolkit.isCtrlKey(e);
    }

    public void mouseReleased(MouseEvent e) {
        if (ctrlActive) {
        } else {
            if (channelIndex >= 0) {
                int newIndex = (int) channel.getPlotter().graphToSampleX(e.getPoint().x);
                LProgressViewer.getInstance().entrySubProgress(getName());
                ALayerSelection ls = getFocussedClip().getSelectedLayer().getSelection();
                ls.operateEachChannel(new AOMove(newIndex));
                getFocussedClip().getSelectedLayer().setEmptySelection();
                updateHistory(GLanguage.translate(getName()));
                LProgressViewer.getInstance().exitSubProgress();
                reloadFocussedClipEditor();
            }
        }
        mouseDown = false;
        ctrlActive = false;
    }

    public void mouseMoved(MouseEvent e) {
        if (GToolkit.isCtrlKey(e)) {
            setCursor(e, moveSelectionCursor);
        } else {
            setCursor(e, moveCursor);
        }
    }

    public void mouseDragged(MouseEvent e) {
        draggedGraphX = e.getPoint().x;
        if (ctrlActive) {
            if (channelIndex >= 0) {
                draggedX = (int) channel.getPlotter().graphToSampleX(e.getPoint().x);
                channel.getSelection().setOffset(pressedSelectionOffset + draggedX - pressedX);
            }
        }
        repaintFocussedClipEditor();
    }

    public void mouseEntered(MouseEvent e) {
        ((Component) e.getSource()).setCursor(actualCursor);
    }

    private Stroke stroke;

    private void initGraphics() {
        stroke = new BasicStroke();
    }

    public void paintOntoClip(Graphics2D g2d, Rectangle rect) {
        if (mouseDown) {
            if (ctrlActive) {
            } else {
                int x1 = draggedGraphX;
                int y1 = (int) rect.getY() + 1;
                int x2 = x1;
                int y2 = (int) rect.getY() + (int) rect.getHeight() - 1;
                int w = 3;
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
                g2d.setColor(Color.black);
                g2d.setStroke(stroke);
                g2d.drawLine(x1, y1, x2, y2);
                g2d.drawLine(x1 - w, y1, x1 + w, y1);
                g2d.drawLine(x2 - w, y2, x2 + w, y2);
            }
        }
    }
}
