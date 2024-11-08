package vademecum.visualizer.d3.scatter.dialogs;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Vector;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import vademecum.data.GridUtils;
import vademecum.data.IColumn;
import vademecum.data.IDataGrid;
import vademecum.ui.visualizer.widgets.TableSelectorPanel;
import vademecum.visualizer.d3.scatter.ScatterPlot3D;

public class ClassColumnSelector extends JDialog {

    ScatterPlot3D plot;

    ArrayList<Integer> clusterCols;

    IDataGrid grid;

    public ClassColumnSelector(ScatterPlot3D plot) {
        super((JFrame) plot.getFigurePanel().getGraphicalViewer());
        setTitle("Class Column Selector");
        this.plot = plot;
        init();
    }

    private void init() {
        JPanel holder = new JPanel();
        holder.setLayout(new BorderLayout());
        final TableSelectorPanel vp = new TableSelectorPanel("ClassColumn");
        vp.setMinItems(1);
        vp.setMaxItems(1);
        grid = plot.getDataSource();
        if (!GridUtils.hasClusterColumns(grid)) {
            JOptionPane.showMessageDialog(null, "No class information available yet.");
        } else {
            clusterCols = GridUtils.getClusterColumnPos(grid);
            System.out.println("#Cluster Columns :");
            int numClCols = clusterCols.size();
            System.out.println(numClCols);
            for (int i = 0; i < numClCols; i++) {
                vp.addEntry("Column " + clusterCols.get(i), false);
            }
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
                System.out.println("Selection :");
                for (Integer col : truev) {
                    System.out.println(col);
                }
                if (truev.size() > 0) {
                    int clcol = clusterCols.get(truev.get(0));
                    System.out.println("Set Cluster Col to " + clcol);
                    plot.setClusterColumn(clcol);
                    plot.clearPlot();
                    System.out.println("Setting source with specified cluster column");
                    plot.setDataSource(grid, plot.usedGridVariables[0], plot.usedGridVariables[1], plot.usedGridVariables[2]);
                    plot.repaint();
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
