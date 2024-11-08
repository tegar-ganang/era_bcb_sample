package vademecum.visualizer.pmatrix2d.dialogs;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import vademecum.visualizer.pmatrix2d.PMatrix2D;

public class ContourDialog extends JDialog {

    final PMatrix2D plot;

    JSlider css;

    public ContourDialog(PMatrix2D plot) {
        super((JFrame) plot.getFigurePanel().getGraphicalViewer());
        this.plot = plot;
        css = new JSlider(0, 50, this.plot.getContourSteps());
        css.setMajorTickSpacing(10);
        css.setMinorTickSpacing(1);
        css.setPaintTicks(true);
        css.setPaintLabels(true);
        css.addChangeListener(new SliderListener());
        add(css);
        pack();
        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    }

    class SliderListener implements ChangeListener {

        public void stateChanged(ChangeEvent arg0) {
            plot.setContourSteps(css.getValue());
            plot.repaint();
        }
    }
}