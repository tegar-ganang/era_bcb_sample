package ch.laoe.plugin;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JPanel;
import ch.laoe.clip.AChannel;
import ch.laoe.clip.AClip;
import ch.laoe.clip.AClipPlotter;
import ch.laoe.clip.ALayerSelection;
import ch.laoe.operation.AOHistogram;
import ch.laoe.ui.Debug;
import ch.laoe.ui.GClipEditor;
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


Class:			GPHistogram
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	histogram analysis

History:
Date:			Description:									Autor:
08.06.02		first draft										oli4

***********************************************************/
public class GPHistogram extends GPluginFrame {

    public GPHistogram(GPluginHandler ph) {
        super(ph);
        initGui();
    }

    public String getName() {
        return "histogram";
    }

    private JButton createHistogram, update;

    private EventDispatcher eventDispatcher;

    private void initGui() {
        JPanel p = new JPanel();
        UiCartesianLayout cl = new UiCartesianLayout(p, 10, 2);
        cl.setPreferredCellSize(new Dimension(25, 35));
        p.setLayout(cl);
        createHistogram = new JButton(GLanguage.translate("new"));
        cl.add(createHistogram, 1, 1, 4, 1);
        update = new JButton(GLanguage.translate("update"));
        cl.add(update, 5, 1, 4, 1);
        frame.getContentPane().add(p);
        pack();
        eventDispatcher = new EventDispatcher();
        createHistogram.addActionListener(eventDispatcher);
        update.addActionListener(new LWorker(eventDispatcher));
        updateGui();
    }

    private class EventDispatcher implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == update) {
                LProgressViewer.getInstance().entrySubProgress(getName());
                LProgressViewer.getInstance().entrySubProgress(1.0);
                Debug.println(1, "plugin " + getName() + " [update] clicked");
                onUpdate();
                LProgressViewer.getInstance().exitSubProgress();
                LProgressViewer.getInstance().exitSubProgress();
            } else if (e.getSource() == createHistogram) {
                Debug.println(1, "plugin " + getName() + " [create histogram] clicked");
                onCreateHistogram();
            }
            updateGui();
        }
    }

    private GClipEditor histogramClipEditor;

    private void updateGui() {
        if (histogramClipEditor != null) {
            update.setEnabled(true);
        } else {
            update.setEnabled(false);
        }
    }

    private void onCreateHistogram() {
        AClip c = new AClip(1, 1, AOHistogram.getHistogramLength());
        c.setName("<" + GLanguage.translate("histogram") + ">");
        c.setSampleRate(getFocussedClip().getSampleRate());
        c.getPlotter().setPlotterXUnit(AClipPlotter.X_UNIT_1);
        c.getPlotter().setPlotterYUnit(AClipPlotter.Y_UNIT_1);
        Laoe.getInstance().addClipFrame(c);
        histogramClipEditor = getFocussedClipEditor();
    }

    private void onUpdate() {
        AOHistogram h = new AOHistogram();
        ALayerSelection ls = getFocussedClip().getSelectedLayer().getSelection();
        ls.operateEachChannel(h);
        AClip c = histogramClipEditor.getClip();
        AChannel ch = c.getLayer(0).getChannel(0);
        ch.setSamples(h.getHistogram());
        ch.markChange();
        try {
            c.getHistory().store(loadIcon(), GLanguage.translate(getName()));
        } catch (NullPointerException npe) {
        }
        c.getPlotter().autoScale();
        histogramClipEditor.reload();
    }
}
