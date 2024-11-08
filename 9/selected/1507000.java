package vademecum.visualizer.D2.scatter.dialogs;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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

public class Scatter2DColorSelector extends JDialog {

    ScatterPlot2D scatter2D;

    public Scatter2DColorSelector(vademecum.visualizer.D2.scatter.ScatterPlot2D plot2D) {
        super((JFrame) plot2D.getFigurePanel().getGraphicalViewer());
        setTitle("Color Selector");
        scatter2D = plot2D;
        init();
    }

    private void init() {
        JPanel holder = new JPanel();
        holder.setLayout(new BorderLayout());
        final ComboBoxSelectorPanel vp = new ComboBoxSelectorPanel();
        IDataGrid grid = scatter2D.getDataSource();
        vp.addVariable("Number");
        Vector<IColumn> v = grid.getColumns();
        for (IColumn col : v) {
            vp.addVariable(col.getLabel());
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
                System.out.println("Box/Index");
                for (Integer in : vp.getSelectedIndices()) {
                    if (in > 0) dim++;
                    in--;
                    v.add(in);
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
