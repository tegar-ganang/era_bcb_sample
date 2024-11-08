package vademecum.visualizer.densityscatter.dialogs;

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
import vademecum.data.IColumn;
import vademecum.data.IDataGrid;
import vademecum.ui.visualizer.widgets.TableSelectorPanel;
import vademecum.visualizer.densityscatter.VPDEScatter;

public class ContourVariableSelector extends JDialog {

    VPDEScatter plot;

    public ContourVariableSelector(VPDEScatter plot) {
        super((JFrame) plot.getFigurePanel().getGraphicalViewer());
        setTitle("Variable Selector");
        this.plot = plot;
        init();
    }

    private void init() {
        JPanel holder = new JPanel();
        holder.setLayout(new BorderLayout());
        final TableSelectorPanel vp = new TableSelectorPanel();
        vp.setMinItems(2);
        vp.setMaxItems(2);
        IDataGrid grid = plot.getDataSource();
        Vector<IColumn> v = grid.getColumns();
        ArrayList<Integer> used = plot.getPlotDataVariables();
        int cnt = 0;
        for (IColumn col : v) {
            if (used.contains(cnt)) {
                vp.addEntry(col.getLabel(), true);
            } else {
                vp.addEntry(col.getLabel(), false);
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
                Vector<Integer> v = new Vector<Integer>();
                int truecnt = 0;
                int index = 0;
                for (Boolean in : vp.getSelectionVector()) {
                    if (in == true) {
                        truecnt++;
                        v.add(index);
                    }
                    index++;
                }
                if (truecnt < vp.getMinItems()) {
                    JOptionPane.showMessageDialog(null, "Please select at least " + vp.getMinItems() + " Item(s).");
                } else {
                    plot.clearPlot();
                    if (v.size() > 0) {
                        plot.setDataSource(plot.getDataSource(), v);
                    }
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
