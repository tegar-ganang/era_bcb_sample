package vademecum.visualizer.D2.qqplot.dialogs;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Vector;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import vademecum.data.IColumn;
import vademecum.data.IDataGrid;
import vademecum.ui.visualizer.widgets.RadioBoxSelectorPanel;
import vademecum.visualizer.D2.qqplot.QQPlot2D;

public class QQPlotVariableSelector extends JDialog {

    QQPlot2D plot;

    public QQPlotVariableSelector(QQPlot2D plot) {
        super((JFrame) plot.getFigurePanel().getGraphicalViewer());
        setTitle("Variable Selector");
        this.plot = plot;
        init();
    }

    private void init() {
        JPanel holder = new JPanel();
        holder.setLayout(new BorderLayout());
        final RadioBoxSelectorPanel vp = new RadioBoxSelectorPanel();
        IDataGrid grid = plot.getDataSource();
        Vector<IColumn> v = grid.getColumns();
        ArrayList<Integer> usedVars = plot.getPlotDataVariables();
        int cnt = 0;
        for (IColumn col : v) {
            if (usedVars.contains(cnt)) {
                vp.addBox(col.getLabel(), true);
            } else {
                vp.addBox(col.getLabel(), false);
            }
            cnt++;
        }
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new BorderLayout());
        JButton cancelBtn = new JButton("Close");
        cancelBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                dispose();
            }
        });
        JButton applyBtn = new JButton("Apply");
        applyBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                Vector<Integer> truev = new Vector<Integer>();
                int plotno = 0;
                for (Boolean in : vp.getSelectionVector()) {
                    if (in.booleanValue() == true) {
                        truev.add(plotno);
                    }
                    plotno++;
                }
                plot.clearPlot();
                for (Integer plno : truev) {
                    plot.setDataSource(plot.getDataSource(), plno);
                }
                plot.repaint();
            }
        });
        buttonsPanel.add(cancelBtn, BorderLayout.WEST);
        buttonsPanel.add(applyBtn, BorderLayout.EAST);
        holder.add(vp, BorderLayout.CENTER);
        holder.add(buttonsPanel, BorderLayout.SOUTH);
        setContentPane(holder);
        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        pack();
    }
}
