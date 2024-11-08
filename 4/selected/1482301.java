package ch.laoe.plugin;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import ch.laoe.clip.AChannel;
import ch.laoe.clip.AClip;
import ch.laoe.clip.AClipPlotter;
import ch.laoe.clip.ALayerSelection;
import ch.laoe.operation.AOSpectrum;
import ch.laoe.operation.AOToolkit;
import ch.laoe.ui.Debug;
import ch.laoe.ui.GClipEditor;
import ch.laoe.ui.GComboBoxPowerOf2;
import ch.laoe.ui.GLanguage;
import ch.laoe.ui.LProgressViewer;
import ch.laoe.ui.LWorker;
import ch.laoe.ui.Laoe;
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


Class:			GPSpectrum
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	Spectrum analysis

History:
Date:			Description:									Autor:
24.05.01		first draft										oli4
19.09.01		undo-history save of spectrum-clip		oli4

***********************************************************/
public class GPSpectrum extends GPluginFrame {

    public GPSpectrum(GPluginHandler ph) {
        super(ph);
        initGui();
    }

    public String getName() {
        return "spectrum";
    }

    private GComboBoxPowerOf2 bufferLength;

    private JComboBox window;

    private JButton createSpectrum, update;

    private JCheckBox autoscale;

    private EventDispatcher eventDispatcher;

    private void initGui() {
        JPanel p = new JPanel();
        UiCartesianLayout cl = new UiCartesianLayout(p, 10, 4);
        cl.setPreferredCellSize(new Dimension(25, 35));
        p.setLayout(cl);
        cl.add(new JLabel(GLanguage.translate("bufferLength")), 0, 0, 4, 1);
        bufferLength = new GComboBoxPowerOf2(4, 18);
        bufferLength.setSelectedExponent(14);
        cl.add(bufferLength, 4, 0, 6, 1);
        cl.add(new JLabel(GLanguage.translate("window")), 0, 1, 4, 1);
        window = new JComboBox(AOToolkit.getFFTWindowNames());
        cl.add(window, 4, 1, 6, 1);
        autoscale = new JCheckBox(GLanguage.translate("autoscale"));
        autoscale.setSelected(true);
        cl.add(autoscale, 0, 2, 10, 1);
        createSpectrum = new JButton(GLanguage.translate("new"));
        cl.add(createSpectrum, 1, 3, 4, 1);
        update = new JButton(GLanguage.translate("update"));
        cl.add(update, 5, 3, 4, 1);
        frame.getContentPane().add(p);
        pack();
        eventDispatcher = new EventDispatcher();
        createSpectrum.addActionListener(eventDispatcher);
        update.addActionListener(new LWorker(eventDispatcher));
        updateGui();
    }

    private class EventDispatcher implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == update) {
                LProgressViewer.getInstance().entrySubProgress(getName());
                LProgressViewer.getInstance().entrySubProgress(0.7);
                Debug.println(1, "plugin " + getName() + " [update] clicked");
                onUpdate();
                LProgressViewer.getInstance().exitSubProgress();
                LProgressViewer.getInstance().exitSubProgress();
            } else if (e.getSource() == createSpectrum) {
                Debug.println(1, "plugin " + getName() + " [create spectrum] clicked");
                onCreateSpectrum();
            }
            updateGui();
        }
    }

    private GClipEditor spectrumClipEditor;

    private void updateGui() {
        if (spectrumClipEditor != null) {
            update.setEnabled(true);
        } else {
            update.setEnabled(false);
        }
    }

    private void onCreateSpectrum() {
        AClip c = new AClip(1, 1, bufferLength.getSelectedValue());
        c.setName("<" + GLanguage.translate("spectrum") + ">");
        c.setSampleRate(getFocussedClip().getSampleRate());
        c.getPlotter().setPlotterXUnit(AClipPlotter.X_UNIT_FDHZ);
        c.getPlotter().setPlotterYUnit(AClipPlotter.Y_UNIT_PERCENT);
        Laoe.getInstance().addClipFrame(c);
        spectrumClipEditor = getFocussedClipEditor();
    }

    private void onUpdate() {
        AOSpectrum s = new AOSpectrum(bufferLength.getSelectedValue(), window.getSelectedIndex());
        ALayerSelection ls = getFocussedClip().getSelectedLayer().getSelection();
        ls.operateEachChannel(s);
        AClip c = spectrumClipEditor.getClip();
        AChannel ch = c.getLayer(0).getChannel(0);
        ch.setSamples(s.getSpectrum());
        ch.markChange();
        try {
            c.getHistory().store(loadIcon(), GLanguage.translate(getName()));
        } catch (NullPointerException npe) {
        }
        if (autoscale.isSelected()) {
            c.getPlotter().autoScale();
        }
        spectrumClipEditor.reload();
    }
}
