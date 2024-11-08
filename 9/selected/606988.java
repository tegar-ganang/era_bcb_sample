package vademecum.visualizer.d3.scatter.dialogs;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Vector;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import vademecum.data.IColumn;
import vademecum.data.IDataGrid;
import vademecum.ui.visualizer.widgets.ComboBoxSelectorPanel;
import vademecum.visualizer.d3.scatter.ScatterPlot3D;

public class Scatter3DVariableSelector extends JDialog {

    /** Logger */
    private static Log log = LogFactory.getLog(Scatter3DVariableSelector.class);

    ScatterPlot3D plot;

    public Scatter3DVariableSelector(ScatterPlot3D plot3D) {
        super((JFrame) plot3D.getFigurePanel().getGraphicalViewer());
        setTitle("Variable Selector");
        plot = plot3D;
        init();
    }

    private void init() {
        JPanel holder = new JPanel();
        holder.setLayout(new BorderLayout());
        final ComboBoxSelectorPanel vp = new ComboBoxSelectorPanel();
        IDataGrid grid = plot.getDataSource();
        int numcols = grid.getNumCols();
        ArrayList<Integer> validcols = new ArrayList<Integer>();
        for (int i = 0; i < numcols; i++) {
            if (!grid.getColumn(i).getType().equals(vademecum.data.IClusterNumber.class)) {
                validcols.add(i);
            }
        }
        Vector<IColumn> v = grid.getColumns();
        for (int i = 0; i < validcols.size(); i++) {
            System.out.println(v.get(validcols.get(i)).getLabel());
            vp.addVariable(v.get(validcols.get(i)).getLabel());
        }
        int[] vars = plot.usedGridVariables;
        if (vars.length == 3) {
            vp.addBox("X-Axis", vars[0]);
            vp.addBox("Y-Axis", vars[1]);
            vp.addBox("Z-Axis", vars[2]);
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
                Vector<Integer> v = new Vector<Integer>();
                int dim = 0;
                System.out.println("Box/Index");
                for (Integer in : vp.getSelectedIndices()) {
                    v.add(in);
                }
                System.out.println("v :");
                for (Integer vint : v) {
                    log.debug(vint + " ");
                }
                plot.clearPlot();
                plot.setDataSource(plot.getDataSource(), v.get(0), v.get(1), v.get(2));
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
