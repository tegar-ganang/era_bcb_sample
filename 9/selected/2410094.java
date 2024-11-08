package vademecum.visualizer.pdeplot.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
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
import vademecum.ui.visualizer.widgets.CheckBoxSelectorPanel;
import vademecum.ui.visualizer.widgets.TableSelectorPanel;
import vademecum.visualizer.pdeplot.PDEPlot;

public class PPDEPlotVariableSelector extends JDialog {

    PDEPlot plot;

    public PPDEPlotVariableSelector(PDEPlot plot) {
        super((JFrame) plot.getFigurePanel().getGraphicalViewer());
        setTitle("Variable Selector");
        this.plot = plot;
        init();
    }

    private void init() {
        JPanel holder = new JPanel();
        holder.setLayout(new BorderLayout());
        final TableSelectorPanel vp = new TableSelectorPanel();
        vp.setMinItems(1);
        IDataGrid grid = plot.getDataGrid();
        ArrayList<Integer> usedVars = plot.getPlotDataVariables();
        int numcols = grid.getNumCols();
        ArrayList<Integer> validcols = new ArrayList<Integer>();
        for (int i = 0; i < numcols; i++) {
            if (!grid.getColumn(i).getType().equals(vademecum.data.IClusterNumber.class)) {
                validcols.add(i);
            }
        }
        vp.setMaxItems(validcols.size());
        Vector<IColumn> v = grid.getColumns();
        for (int i = 0; i < validcols.size(); i++) {
            if (usedVars.contains(i)) {
                vp.addEntry(v.get(validcols.get(i)).getLabel(), true);
            } else {
                vp.addEntry(v.get(validcols.get(i)).getLabel(), false);
            }
        }
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new BorderLayout());
        JButton cancelBtn = new JButton("Cancel");
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
                    plot.setDataGrid(plot.getDataGrid(), plno);
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
