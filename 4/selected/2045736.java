package ch.laoe.plugin;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import ch.laoe.clip.AChannelMask;
import ch.laoe.clip.ALayer;
import ch.laoe.ui.Debug;
import ch.laoe.ui.GLanguage;
import ch.laoe.ui.LProgressViewer;
import ch.laoe.ui.GToolkit;
import ch.oli4.ui.UiCartesianLayout;

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


Class:			GPMask
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	plugin to edit the mask.

History:
Date:			Description:									Autor:
27.02.02		first draft										oli4

***********************************************************/
public class GPMask extends GPluginFrame {

    public GPMask(GPluginHandler ph) {
        super(ph);
        initGui();
    }

    public String getName() {
        return "mask";
    }

    public JMenuItem createMenuItem() {
        return super.createMenuItem(KeyEvent.VK_M);
    }

    protected boolean isFocussingMouseEvents() {
        return true;
    }

    private void wideMaskCopy(boolean isCtrl, AChannelMask orig) {
        if (isCtrl) {
            getFocussedClip().getSelectedLayer().wideCopyMask(orig);
        }
    }

    private AChannelMask actualMask;

    public void mousePressed(MouseEvent e) {
        actualMask = getSelectedLayer().getChannel(e.getPoint()).getMask();
        actualMask.getSegments().mousePressed(e);
        repaintFocussedClipEditor();
    }

    public void mouseReleased(MouseEvent e) {
        if (e.getClickCount() == 0) {
            repaintFocussedClipEditor();
            updateHistory(GLanguage.translate(getName()));
        }
    }

    public void mouseClicked(MouseEvent e) {
        actualMask.getSegments().mouseClicked(e);
        wideMaskCopy(GToolkit.isCtrlKey(e), actualMask);
        repaintFocussedClipEditor();
        updateHistory(GLanguage.translate(getName()));
    }

    public void mouseEntered(MouseEvent e) {
        if (actualMask.getSegments() != null) {
            actualMask.getSegments().mouseEntered(e);
        }
        repaintFocussedClipEditor();
    }

    public void mouseMoved(MouseEvent e) {
        try {
            actualMask = getSelectedLayer().getChannel(e.getPoint()).getMask();
            if (actualMask.getSegments() != null) {
                actualMask.getSegments().mouseMoved(e);
            }
            repaintFocussedClipEditor();
        } catch (Exception exc) {
        }
    }

    public void mouseDragged(MouseEvent e) {
        actualMask.getSegments().mouseDragged(e);
        wideMaskCopy(GToolkit.isCtrlKey(e), actualMask);
        repaintFocussedClipEditor();
    }

    private JButton clear, complementary, applyDefinitely;

    private EventDispatcher eventDispatcher;

    public void initGui() {
        JPanel p2 = new JPanel();
        UiCartesianLayout l2 = new UiCartesianLayout(p2, 8, 2);
        l2.setPreferredCellSize(new Dimension(30, 35));
        p2.setLayout(l2);
        clear = new JButton(GLanguage.translate("clear"));
        l2.add(clear, 0, 0, 4, 1);
        complementary = new JButton(GLanguage.translate("complement"));
        l2.add(complementary, 4, 0, 4, 1);
        applyDefinitely = new JButton(GLanguage.translate("applyDefinitely"));
        l2.add(applyDefinitely, 0, 1, 4, 1);
        frame.getContentPane().add(p2);
        pack();
        eventDispatcher = new EventDispatcher();
        clear.addActionListener(eventDispatcher);
        complementary.addActionListener(eventDispatcher);
        applyDefinitely.addActionListener(eventDispatcher);
    }

    private class EventDispatcher implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            AChannelMask m = getFocussedClip().getSelectedLayer().getSelectedChannel().getMask();
            if (e.getSource() == clear) {
                Debug.println(1, "plugin " + getName() + " [clear] clicked");
                m.clear();
                wideMaskCopy(GToolkit.isCtrlKey(e), m);
                repaintFocussedClipEditor();
            } else if (e.getSource() == complementary) {
                Debug.println(1, "plugin " + getName() + " [complementary] clicked");
                m.setComplementary();
                wideMaskCopy(GToolkit.isCtrlKey(e), m);
                repaintFocussedClipEditor();
            } else if (e.getSource() == applyDefinitely) {
                LProgressViewer.getInstance().entrySubProgress(getName());
                LProgressViewer.getInstance().entrySubProgress(0.7);
                Debug.println(1, "plugin " + getName() + " [apply definitely] clicked");
                if (GToolkit.isCtrlKey(e)) {
                    ALayer l = getFocussedClip().getSelectedLayer();
                    for (int i = 0; i < l.getNumberOfChannels(); i++) {
                        l.getChannel(i).getMask().applyDefinitely();
                    }
                } else {
                    m.applyDefinitely();
                }
                updateHistory(GLanguage.translate(getName()));
                LProgressViewer.getInstance().exitSubProgress();
                LProgressViewer.getInstance().exitSubProgress();
                reloadFocussedClipEditor();
            }
            updateHistory(GLanguage.translate(getName()));
        }
    }
}
