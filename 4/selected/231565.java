package gov.sns.apps.sclsetcm;

import javax.swing.*;
import gov.sns.ca.Channel;
import java.awt.*;
import java.util.*;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.net.*;
import javax.swing.*;
import java.awt.event.*;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import gov.sns.tools.plot.*;
import gov.sns.tools.apputils.files.RecentFileTracker;
import gov.sns.tools.swing.DecimalField;
import gov.sns.tools.apputils.SimpleChartPopupMenu;
import gov.sns.tools.apputils.files.RecentFileTracker;
import gov.sns.xal.model.probe.Probe;

/**
 *
 * @author y32
 */
public class AnalysPanel implements ItemListener, ActionListener {

    JPanel anaPanel;

    JPanel compPanel;

    JButton spass;

    JButton solve;

    JButton saverlt;

    JButton removep;

    BasicGraphData fitting;

    BasicGraphData measure;

    SCLAnalysis analysis;

    SCLCmDocument doc;

    static String[] mcav = new String[1];

    JComboBox mlist;

    DecimalField tflfd;

    DecimalField tftime;

    double dlfd;

    double dtime;

    double[] accm = new double[1];

    double[] phasem = new double[1];

    double[] accf = new double[1];

    double[] phasef = new double[1];

    NumberFormat nf = NumberFormat.getNumberInstance();

    protected FunctionGraphsJPanel plotfit;

    protected JFileChooser exportsFileChooser;

    protected RecentFileTracker exportsTracker;

    public AnalysPanel(SCLCmDocument mydoc) {
        doc = mydoc;
        analysis = new SCLAnalysis(doc);
        nf.setMaximumFractionDigits(2);
        dtime = 2.0;
    }

    public JPanel makeComp() {
        Iterator itr = doc.resultMap.keySet().iterator();
        mcav = new String[doc.resultMap.keySet().size()];
        int i = 0;
        while (itr.hasNext()) {
            mcav[i] = (String) itr.next();
            i = i + 1;
        }
        compPanel = new JPanel();
        GridBagLayout gbl = new GridBagLayout();
        GridBagConstraints gbc = new GridBagConstraints();
        compPanel.setLayout(gbl);
        spass = new JButton("Single Pass");
        spass.setEnabled(true);
        spass.addActionListener(this);
        solve = new JButton("Solve");
        solve.setEnabled(true);
        solve.addActionListener(this);
        saverlt = new JButton("Save");
        saverlt.setEnabled(true);
        saverlt.addActionListener(this);
        removep = new JButton("Remove Point");
        removep.setEnabled(true);
        removep.addActionListener(this);
        mlist = new JComboBox(mcav);
        mlist.addActionListener(this);
        tflfd = new DecimalField(dlfd, 8);
        tftime = new DecimalField(dtime, 8);
        JLabel jllfd = new JLabel("LFD Coefficient");
        JLabel jltime = new JLabel("Solver Time (s)");
        int cy = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.;
        gbc.weighty = 0.;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(1, 10, 3, 12);
        gbc.gridx = 0;
        gbc.gridy = cy;
        gbl.setConstraints(mlist, gbc);
        gbc.gridx = 4;
        gbc.gridy = cy++;
        gbl.setConstraints(removep, gbc);
        gbc.gridx = 2;
        gbc.gridy = cy;
        gbl.setConstraints(jltime, gbc);
        gbc.gridy = cy++;
        gbc.gridx = 4;
        gbl.setConstraints(jllfd, gbc);
        gbc.gridy = cy;
        gbc.gridx = 2;
        gbl.setConstraints(tftime, gbc);
        gbc.gridy = cy++;
        gbc.gridx = 4;
        gbl.setConstraints(tflfd, gbc);
        gbc.gridy = cy;
        gbc.gridx = 0;
        gbl.setConstraints(spass, gbc);
        gbc.gridy = cy;
        gbc.gridx = 2;
        gbl.setConstraints(solve, gbc);
        gbc.gridy = cy;
        gbc.gridx = 4;
        gbl.setConstraints(saverlt, gbc);
        compPanel.add(mlist);
        compPanel.add(spass);
        compPanel.add(solve);
        compPanel.add(saverlt);
        compPanel.add(removep);
        compPanel.add(tflfd);
        compPanel.add(tftime);
        compPanel.add(jllfd);
        compPanel.add(jltime);
        return compPanel;
    }

    public JPanel getPanel() {
        return anaPanel;
    }

    public JPanel getComp() {
        return compPanel;
    }

    public JPanel makeAnalysPanel() {
        anaPanel = new JPanel();
        fitting = new BasicGraphData();
        measure = new BasicGraphData();
        plotfit = new FunctionGraphsJPanel();
        anaPanel.setLayout(new GridLayout(2, 1, 1, 1));
        anaPanel.addMouseListener(new SimpleChartPopupMenu(plotfit));
        measure.addPoint(accm, phasem);
        measure.setDrawLinesOn(false);
        measure.setDrawPointsOn(true);
        measure.setGraphProperty("Legend", "Measure");
        measure.setGraphColor(Color.BLUE);
        fitting.addPoint(accf, phasef);
        fitting.setDrawLinesOn(true);
        fitting.setDrawPointsOn(false);
        fitting.setGraphProperty("Legend", "Fit");
        fitting.setGraphColor(Color.RED);
        plotfit.setEnabled(true);
        plotfit.setGraphBackGroundColor(Color.white);
        plotfit.setPreferredSize(new Dimension(400, 300));
        plotfit.setAxisNames("Eacc (MV/m)", "Phase (deg)");
        plotfit.setLegendButtonVisible(true);
        plotfit.setChooseModeButtonVisible(true);
        plotfit.addGraphData(measure);
        plotfit.addGraphData(fitting);
        anaPanel.add(plotfit);
        anaPanel.add(makeComp());
        return anaPanel;
    }

    public void itemStateChanged(ItemEvent ie) {
        Checkbox cb = (Checkbox) ie.getItemSelectable();
    }

    public void actionPerformed(ActionEvent ae) {
        if (ae.getActionCommand().equals(mlist.getActionCommand())) {
            analysis.setCav(mcav[mlist.getSelectedIndex()]);
            accm = analysis.xdata;
            phasem = analysis.ydata;
            measure.updateValues(accm, phasem);
            plotfit.refreshGraphJPanel();
        } else if (ae.getActionCommand().equals("Single Pass")) {
            analysis.initial();
            tflfd.setText(nf.format(analysis.computlfd()));
            getfitcurve();
            fitting.updateValues(accf, phasef);
            plotfit.refreshGraphJPanel();
        } else if (ae.getActionCommand().equals("Solve")) {
            analysis.settmax(tftime.getValue());
            analysis.fitting();
            getfitcurve();
            tflfd.setText(nf.format(analysis.computlfd()));
            fitting.updateValues(accf, phasef);
            plotfit.refreshGraphJPanel();
        } else if (ae.getActionCommand().equals("Remove Point")) {
            removePoint();
            measure.updateValues(accm, phasem);
            plotfit.refreshGraphJPanel();
        } else if (ae.getActionCommand().equals("Save")) {
            final File file = getfile();
            String cv;
            if (file == null) return;
            try {
                final Writer writer = new FileWriter(file);
                Iterator itr = analysis.lfdMap.keySet().iterator();
                writer.write("SCL LFD data generated: " + new SimpleDateFormat("MMM dd, yyyy HH:mm:ss").format(new Date()) + "\n");
                while (itr.hasNext()) {
                    cv = itr.next().toString();
                    writer.write(cv + "  " + nf.format(analysis.lfdMap.get(cv)) + "\n");
                }
                writer.flush();
            } catch (java.io.IOException ie) {
                System.out.println("Error writing results" + ie);
            }
        }
    }

    private void getfitcurve() {
        double xst;
        int n = 50;
        accf = new double[n];
        phasef = new double[n];
        xst = analysis.xmax / (float) (n - 1);
        for (int i = 0; i < n; i++) {
            accf[i] = xst * (float) i;
            phasef[i] = analysis.k0 + analysis.k2 * accf[i] * accf[i];
        }
    }

    protected File getfile() {
        if (exportsTracker == null) {
            exportsTracker = new RecentFileTracker(1, doc.myWindow().getClass(), "EXPORT_URL");
        }
        if (exportsFileChooser == null) {
            exportsFileChooser = new JFileChooser();
            exportsTracker.applyRecentFolder(exportsFileChooser);
        }
        final int status = exportsFileChooser.showSaveDialog(doc.myWindow());
        switch(status) {
            case JFileChooser.APPROVE_OPTION:
                break;
            default:
                return null;
        }
        final File file = exportsFileChooser.getSelectedFile();
        if (file.exists()) {
            final int continueStatus = JOptionPane.showConfirmDialog(doc.myWindow(), "The file: \"" + file.getPath() + "\" already exits!\n Overwrite this file?");
            switch(continueStatus) {
                case JOptionPane.YES_OPTION:
                    break;
                case JOptionPane.NO_OPTION:
                    return getfile();
                default:
                    return null;
            }
        }
        exportsTracker.cacheURL(file);
        return file;
    }

    public void removePoint() {
        Integer ind = plotfit.getPointChosenIndex();
        if (ind != null) {
            int nsize = accm.length - 1;
            int iind = ind.intValue();
            double[] oldacc = accm;
            double[] oldphase = phasem;
            double[] newacc = new double[nsize];
            double[] newphase = new double[nsize];
            for (int i = 0; i < nsize; i++) {
                if (i < iind) {
                    newacc[i] = oldacc[i];
                    newphase[i] = oldphase[i];
                } else {
                    newacc[i] = oldacc[i + 1];
                    newphase[i] = oldphase[i + 1];
                }
            }
            analysis.xdata = newacc;
            analysis.ydata = newphase;
            accm = newacc;
            phasem = newphase;
        } else {
            System.out.println("No point found.");
        }
    }
}
