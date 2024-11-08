package vademecum.visualizer.D2.qqplot.dialogs;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Vector;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import vademecum.data.IColumn;
import vademecum.data.IDataGrid;
import vademecum.ui.visualizer.widgets.ComboBoxSelectorPanel;
import vademecum.ui.visualizer.widgets.RadioBoxSelectorPanel;
import vademecum.visualizer.D2.qqplot.QQPlot2D;

public class QQPlot_Attrib_vs_Distr extends JDialog {

    QQPlot2D plot;

    JComboBox distrbox;

    public QQPlot_Attrib_vs_Distr(QQPlot2D plot) {
        super((JFrame) plot.getFigurePanel().getGraphicalViewer());
        setTitle("Variable / Distribution Selector");
        this.plot = plot;
        init();
    }

    private void init() {
        JPanel holder = new JPanel();
        holder.setLayout(new BorderLayout());
        final ComboBoxSelectorPanel vp = new ComboBoxSelectorPanel();
        distrbox = new JComboBox();
        distrbox.addItem("Standard Normal Distribution");
        distrbox.addItem("Uniform Distribution");
        distrbox.setSelectedIndex(0);
        IDataGrid grid = plot.getDataSource();
        int numcols = grid.getNumCols();
        ArrayList<Integer> validcols = new ArrayList<Integer>();
        for (int i = 0; i < numcols; i++) {
            if (!grid.getColumn(i).getType().equals(vademecum.data.IClusterNumber.class)) {
                validcols.add(i);
            }
        }
        Vector<IColumn> v = grid.getColumns();
        ArrayList<Integer> usedVars = plot.getPlotDataVariables();
        for (int i = 0; i < validcols.size(); i++) {
            vp.addVariable(v.get(validcols.get(i)).getLabel());
        }
        int varindex = 0;
        if (usedVars.size() > 0) {
            varindex = usedVars.get(0);
        }
        vp.addBox("Variable ", varindex);
        JPanel disPanel = new JPanel();
        disPanel.setLayout(new BorderLayout());
        disPanel.add(new JLabel("Distributions"), BorderLayout.NORTH);
        disPanel.add(distrbox, BorderLayout.SOUTH);
        vp.add(disPanel);
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
                int var = 0;
                for (Integer in : vp.getSelectedIndices()) {
                    var = in;
                }
                plot.clearPlot();
                int dflag = distrbox.getSelectedIndex();
                plot.setReferenceDistribution(dflag);
                plot.setDataSource(plot.getDataSource(), var);
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
