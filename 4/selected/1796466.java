package ch.laoe.ui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import ch.laoe.clip.AChannel;
import ch.laoe.clip.AChannelPlotter;
import ch.laoe.clip.AClip;
import ch.laoe.clip.AClipPlotter;
import ch.laoe.clip.ALayer;
import ch.laoe.plugin.GPlugin;
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


Autor:			olivier g�umann, switzerland
Description:	statusbar printed on bottom of frame, showing
               main states. 

History:
Date:			Description:									Autor:
21.08.10		first draft     								oli4

***********************************************************/
public class GStatusBar extends JPanel implements MouseMotionListener, ActionListener {

    public GStatusBar() {
        UiCartesianLayout clt = new UiCartesianLayout(this, 48, 1);
        clt.setPreferredCellSize(new Dimension(30, 35));
        clt.setBorderGap(0.01f);
        clt.setCellGap(0.01f);
        setLayout(clt);
        Font f = new Font("Courrier", Font.PLAIN, 10);
        setPreferredSize(new Dimension(getPreferredSize().width, 30));
        currentPlugin = new JLabel();
        currentPlugin.setFont(f);
        currentPlugin.setBorder(BorderFactory.createEtchedBorder());
        clt.add(currentPlugin, 0, 0, 8, 1);
        keyMouseHelp = new JLabel();
        keyMouseHelp.setFont(f);
        keyMouseHelp.setBorder(BorderFactory.createEtchedBorder());
        clt.add(keyMouseHelp, 8, 0, 12, 1);
        selectedLayer = new JLabel();
        selectedLayer.setFont(f);
        selectedLayer.setBorder(BorderFactory.createEtchedBorder());
        clt.add(selectedLayer, 20, 0, 4, 1);
        selectedChannel = new JLabel();
        selectedChannel.setFont(f);
        selectedChannel.setBorder(BorderFactory.createEtchedBorder());
        clt.add(selectedChannel, 24, 0, 4, 1);
        cursorPosition = new JLabel();
        cursorPosition.setFont(f);
        cursorPosition.setBorder(BorderFactory.createEtchedBorder());
        clt.add(cursorPosition, 28, 0, 10, 1);
        playPointer = new JLabel();
        playPointer.setFont(f);
        playPointer.setBorder(BorderFactory.createEtchedBorder());
        clt.add(playPointer, 38, 0, 4, 1);
        xUnit = new JComboBox(AClipPlotter.getPlotterXUnitNames());
        xUnit.setFont(f);
        xUnit.setBorder(BorderFactory.createEtchedBorder());
        xUnit.addActionListener(this);
        clt.add(xUnit, 42, 0, 3, 1);
        yUnit = new JComboBox(AClipPlotter.getPlotterYUnitNames());
        yUnit.setFont(f);
        yUnit.setBorder(BorderFactory.createEtchedBorder());
        yUnit.addActionListener(this);
        clt.add(yUnit, 45, 0, 3, 1);
        update();
    }

    private JLabel currentPlugin, keyMouseHelp, selectedLayer, selectedChannel, cursorPosition, playPointer;

    private JComboBox xUnit, yUnit;

    public void mouseMoved(MouseEvent e) {
        try {
            calculateCursorPositions(e);
        } catch (Exception exc) {
        }
    }

    public void mouseDragged(MouseEvent e) {
        try {
            calculateCursorPositions(e);
        } catch (Exception exc) {
        }
    }

    private void calculateCursorPositions(MouseEvent e) {
        AClip c = Laoe.getInstance().getFocussedClipEditor().getClip();
        AClipPlotter clp = (AClipPlotter) c.getPlotter();
        ALayer l = c.getSelectedLayer();
        int i = l.getPlotter().getInsideChannelIndex(e.getPoint());
        if (i >= 0) {
            AChannel ch = l.getChannel(i);
            AChannelPlotter chp = ch.getPlotter();
            double x = chp.graphToSampleX(e.getPoint().x);
            float y = chp.graphToSampleY(e.getPoint().y);
            cursorPosition.setText("<html><b>" + GLanguage.translate("position") + "</b><br>" + ((float) clp.toPlotterXUnit(x)) + clp.getPlotterXUnitName() + " / " + ((float) clp.toPlotterYUnit(y)) + clp.getPlotterYUnitName() + "</html>");
        }
        GPlugin p = Laoe.getInstance().getPluginHandler().getFocussedPlugin();
        String s = GLanguage.translate("mouse");
        if (GToolkit.isShiftKey(e)) {
            s = s + "+" + GLanguage.translate("shift");
        }
        if (GToolkit.isCtrlKey(e)) {
            s = s + "+" + GLanguage.translate("ctrl");
        }
        keyMouseHelp.setText("<html><b>" + s + "</b><br>" + GLanguage.translate(p.getKeyMouseHelp(GToolkit.isShiftKey(e), GToolkit.isCtrlKey(e))) + "</html>");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == xUnit) {
            AClip c = Laoe.getInstance().getFocussedClipEditor().getClip();
            c.getPlotter().setPlotterXUnit(xUnit.getSelectedIndex());
        } else if (e.getSource() == yUnit) {
            AClip c = Laoe.getInstance().getFocussedClipEditor().getClip();
            c.getPlotter().setPlotterYUnit(yUnit.getSelectedIndex());
        }
        update();
        Laoe.getInstance().getFocussedClipEditor().reload();
    }

    public void update() {
        GPlugin p = Laoe.getInstance().getPluginHandler().getFocussedPlugin();
        if (p != null) {
            currentPlugin.setText("<html><b>" + GLanguage.translate("plugin") + "</b><br>" + GLanguage.translate(p.getName()) + "</html>");
        } else {
            currentPlugin.setText(GLanguage.translate("<html><b>" + GLanguage.translate("none") + "</b></html>"));
        }
        try {
            AClip c = Laoe.getInstance().getFocussedClipEditor().getClip();
            AClipPlotter clp = c.getPlotter();
            selectedLayer.setText("<html><b>" + GLanguage.translate("layer") + "</b><br>" + c.getSelectedLayer().getName() + "</html>");
            selectedChannel.setText("<html><b>" + GLanguage.translate("channel") + "</b><br>" + c.getSelectedLayer().getSelectedChannel().getName() + "</html>");
            playPointer.setText("<html><b>" + GLanguage.translate("play") + "</b><br>" + (float) clp.toPlotterXUnit(c.getAudio().getPlayPointer()) + clp.getPlotterXUnitName() + "</html>");
            if (xUnit.getSelectedIndex() != clp.getPlotterXUnit()) {
                xUnit.setSelectedIndex(clp.getPlotterXUnit());
            }
            if (yUnit.getSelectedIndex() != clp.getPlotterYUnit()) {
                yUnit.setSelectedIndex(clp.getPlotterYUnit());
            }
        } catch (Exception e) {
        }
    }
}
