package ch.laoe.plugin;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import ch.laoe.clip.AChannel;
import ch.laoe.clip.ALayer;
import ch.laoe.ui.Debug;
import ch.laoe.ui.GCookie;
import ch.laoe.ui.GEditableArea;
import ch.laoe.ui.GLanguage;
import ch.laoe.ui.GToolkit;
import ch.oli4.ui.UiCartesianLayout;
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


Class:			GPSpectrogramFilter
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	
plugin to select in 2D in spectrograms

History:
Date:			Description:									Autor:
10.03.02		first draft										oli4
13.03.02		windowed overlap introduced				oli4
21.03.02		more selection-features like copy, paste, all,
				none												oli4
16.06.02		use channelstack as channel-chooser		oli4
15.10.2010  separate filter and selection          oli4

***********************************************************/
public class GPSpectrogramSelect extends GPluginFrame {

    public GPSpectrogramSelect(GPluginHandler ph) {
        super(ph);
        initGui();
        updateCookie();
    }

    public String getName() {
        return "spectrogramSelect";
    }

    public void start() {
        super.start();
        updateCookie();
    }

    public void reload() {
        super.reload();
        updateCookie();
        brushSize.refresh();
    }

    protected boolean isFocussingMouseEvents() {
        return true;
    }

    private GEditableArea area;

    private AChannel actualChannel;

    private void updateCookie() {
        try {
            ALayer l = getSelectedLayer();
            for (int i = 0; i < l.getNumberOfChannels(); i++) {
                if (l.getChannel(i).getCookies().getCookie(getName()) == null) {
                    AChannel ch = l.getChannel(i);
                    ch.getCookies().setCookie(new Cookie(ch), getName());
                }
            }
        } catch (Exception e) {
        }
    }

    private void updateActualArea(MouseEvent e) {
        try {
            AChannel ch = getSelectedLayer().getChannel(e.getPoint());
            if (ch != null) {
                actualChannel = getSelectedLayer().getChannel(e.getPoint());
                area = ((Cookie) actualChannel.getCookies().getCookie(getName())).area;
            }
        } catch (Exception exc) {
            Debug.printStackTrace(5, exc);
        }
    }

    private void updateActualArea() {
        try {
            actualChannel = getSelectedLayer().getSelectedChannel();
            area = ((Cookie) actualChannel.getCookies().getCookie(getName())).area;
        } catch (Exception exc) {
            Debug.printStackTrace(5, exc);
        }
    }

    public static class Cookie extends GCookie {

        public Cookie(AChannel ch) {
            area = new GEditableArea();
            area.setChannel(ch);
        }

        public GEditableArea area;
    }

    private void wideCopySpectrogramSelection(MouseEvent e, GEditableArea orig) {
        if (GToolkit.isCtrlKey(e)) {
            getFocussedClip().getSelectedLayer().wideCopySpectrogramSelection(orig);
        }
    }

    /**
	 *	mouse events
	 */
    public void mousePressed(MouseEvent e) {
        try {
            updateActualArea(e);
            area.mousePressed(e);
            repaintFocussedClipEditor();
        } catch (Exception exc) {
            Debug.printStackTrace(5, exc);
        }
    }

    public void mouseReleased(MouseEvent e) {
        area.mouseReleased(e);
        wideCopySpectrogramSelection(e, area);
        repaintFocussedClipEditor();
    }

    public void mouseMoved(MouseEvent e) {
        try {
            updateActualArea(e);
            switch(drawMode.getSelectedIndex()) {
                case 0:
                    area.setDrawMode(GEditableArea.DRAW_MODE_RECTANGLE);
                    break;
                case 1:
                    area.setDrawMode(GEditableArea.DRAW_MODE_LINE);
                    break;
                case 2:
                    area.setDrawMode(GEditableArea.DRAW_MODE_POLYGON);
                    break;
            }
            area.setBrushSize((int) brushSize.getData());
            area.mouseMoved(e);
            repaintFocussedClipEditor();
        } catch (Exception exc) {
        }
    }

    public void mouseDragged(MouseEvent e) {
        area.mouseDragged(e);
        wideCopySpectrogramSelection(e, area);
        repaintFocussedClipEditor();
    }

    public void mouseClicked(MouseEvent e) {
        area.mouseClicked(e);
        if (e.getClickCount() == 1) {
            Debug.println(6, "mouse clicked: unselect");
            area.clear();
        } else if (e.getClickCount() == 2) {
            Debug.println(6, "mouse double-clicked: create marked selection");
            area.selectAll();
        } else if (e.getClickCount() == 3) {
            Debug.println(6, "mouse tripple-clicked: create channel selection");
            area.selectAll();
        }
        updateHistory(GLanguage.translate(getName()) + " " + GLanguage.translate("range"));
        repaintFocussedClipEditor();
        wideCopySpectrogramSelection(e, area);
        repaintFocussedClipEditor();
    }

    public void mouseEntered(MouseEvent e) {
        updateActualArea(e);
        area.mouseEntered(e);
        repaintFocussedClipEditor();
    }

    /**
	 *	graphics
	 */
    public void paintOntoClip(Graphics2D g2d, Rectangle rect) {
        try {
            ALayer l = getSelectedLayer();
            for (int i = 0; i < l.getNumberOfChannels(); i++) {
                ((Cookie) l.getChannel(i).getCookies().getCookie(getName())).area.paintOntoClip(g2d, rect);
            }
        } catch (Exception exc) {
        }
    }

    /**
	 *	GUI
	 */
    private UiControlText brushSize;

    private JCheckBox inversed;

    private JComboBox drawMode;

    private EventDispatcher eventDispatcher;

    private void initGui() {
        JPanel p = new JPanel();
        UiCartesianLayout l = new UiCartesianLayout(p, 10, 3);
        l.setPreferredCellSize(new Dimension(25, 35));
        p.setLayout(l);
        l.add(new JLabel(GLanguage.translate("drawMode")), 0, 0, 5, 1);
        String drawModeItems[] = { GLanguage.translate("rectangle"), GLanguage.translate("line"), GLanguage.translate("shape") };
        drawMode = new JComboBox(drawModeItems);
        drawMode.setSelectedIndex(0);
        l.add(drawMode, 5, 0, 5, 1);
        l.add(new JLabel(GLanguage.translate("brushSize")), 0, 1, 5, 1);
        brushSize = new UiControlText(9, true, true);
        brushSize.setDataRange(2, 50);
        brushSize.setData(8);
        l.add(brushSize, 5, 1, 5, 1);
        inversed = new JCheckBox(GLanguage.translate("inversed"));
        l.add(inversed, 0, 2, 7, 1);
        frame.getContentPane().add(p);
        pack();
        eventDispatcher = new EventDispatcher();
        inversed.addActionListener(eventDispatcher);
        drawMode.addActionListener(eventDispatcher);
        updateComponents();
    }

    private class EventDispatcher implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == inversed) {
                Debug.println(1, "plugin " + getName() + " [inversed] clicked");
                onInverse(e);
            } else if (e.getSource() == drawMode) {
                Debug.println(1, "plugin " + getName() + " [draw mode] clicked");
                updateComponents();
            }
            repaintFocussedClipEditor();
        }
    }

    private void updateComponents() {
        brushSize.setEnabled(drawMode.getSelectedIndex() == 1);
    }

    private GEditableArea clipBoardArea;

    private void onInverse(ActionEvent e) {
        updateActualArea();
        if (GToolkit.isCtrlKey(e)) {
            ALayer l = getSelectedLayer();
            for (int i = 0; i < l.getNumberOfChannels(); i++) {
                ((Cookie) l.getChannel(i).getCookies().getCookie(getName())).area.setInversed(inversed.isSelected());
            }
        } else {
            area.setInversed(inversed.isSelected());
        }
        repaintFocussedClipEditor();
    }
}
