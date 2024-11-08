package vademecum.visualizer.D2.scatter.dialogs;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Vector;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import vademecum.data.IColumn;
import vademecum.data.IDataGrid;
import vademecum.ui.visualizer.widgets.ComboBoxSelectorPanel;
import vademecum.visualizer.D2.scatter.ScatterPlot2D;

public class Scatter2DVariableSelector extends JDialog {

    ScatterPlot2D scatter2D;

    public Scatter2DVariableSelector(vademecum.visualizer.D2.scatter.ScatterPlot2D plot2D) {
        super((JFrame) plot2D.getFigurePanel().getGraphicalViewer());
        setTitle("Variable Selector");
        scatter2D = plot2D;
        init();
    }

    private void init() {
        JPanel holder = new JPanel();
        holder.setLayout(new BorderLayout());
        final ComboBoxSelectorPanel vp = new ComboBoxSelectorPanel();
        IDataGrid grid = scatter2D.getDataSource();
        int numcols = grid.getNumCols();
        ArrayList<Integer> validcols = new ArrayList<Integer>();
        validcols.addAll(grid.getColumnIndicesMatching(Double.class));
        validcols.addAll(grid.getColumnIndicesMatching(Integer.class));
        Collections.sort(validcols);
        vp.addVariable("Number");
        Vector<IColumn> v = grid.getColumns();
        for (int i = 0; i < validcols.size(); i++) {
            vp.addVariable(v.get(validcols.get(i)).getLabel());
        }
        int[] vars = scatter2D.usedGridVariables;
        if (vars.length == 2) {
            vp.addBox("X-Axis", vars[0] + 1);
            vp.addBox("Y-Axis", vars[1] + 1);
        } else {
            vp.addBox("X-Axis");
            vp.addBox("Y-Axis", vars[0] + 1);
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
                Vector<Integer> v = new Vector<Integer>();
                int dim = 0;
                Vector<Integer> validCols = new Vector<Integer>();
                validCols.addAll(scatter2D.getDataSource().getColumnIndicesMatching(Double.class));
                validCols.addAll(scatter2D.getDataSource().getColumnIndicesMatching(Integer.class));
                Collections.sort(validCols);
                for (Integer in : vp.getSelectedIndices()) {
                    if (in > 0) dim++;
                    in--;
                    v.add(validCols.get(in));
                }
                System.out.println("v :");
                for (Integer vint : v) {
                    System.out.print(vint + " ");
                }
                if (dim == 0) {
                    JOptionPane.showMessageDialog(null, "Select at least one Variable please.");
                } else if (dim == 1) {
                    scatter2D.clearPlot();
                    scatter2D.resetLegendEntries();
                    System.out.println("1Dim : v.get(1) = " + v.get(1));
                    scatter2D.setDataSource(scatter2D.getDataSource(), v.get(1));
                    scatter2D.repaint();
                } else if (dim == 2) {
                    scatter2D.clearPlot();
                    scatter2D.resetLegendEntries();
                    System.out.println("2Dim : v.get(0) = " + v.get(0) + "v.get(1) = " + v.get(1));
                    scatter2D.setDataSource(scatter2D.getDataSource(), v.get(0), v.get(1));
                    scatter2D.repaint();
                }
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
