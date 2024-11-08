package gov.sns.apps.wireanalysis;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import gov.sns.tools.swing.*;
import gov.sns.tools.apputils.EdgeLayout;
import gov.sns.tools.plot.*;
import gov.sns.tools.data.*;
import gov.sns.tools.fit.lsm.*;
import java.text.NumberFormat;

public class AnalysisPanel extends JPanel {

    public JPanel mainPanel;

    public JTable datatable;

    private StoredResultsPanel storedresultspanel;

    private DataTable masterdatatable;

    private DataTable resultsdatatable;

    private JButton removebutton;

    private JButton fitbutton;

    private JButton storebutton;

    private JButton hnormbutton;

    private JButton vnormbutton;

    private JButton vcutbutton;

    private JButton centerbutton;

    private JButton clearbutton;

    private JButton gnormbutton;

    private JButton fitthreshbutton;

    private JLabel sigmalabel;

    private JLabel amplabel;

    private JLabel centerlabel;

    private JLabel pedestallabel;

    private JLabel[] rlabels = new JLabel[3];

    private JPanel fitresultspanel;

    private JPanel vcutpanel;

    private JPanel hnormpanel;

    private JPanel vnormpanel;

    private JPanel centerpanel;

    private JPanel fitthreshpanel;

    private boolean dataHasBeenFit = false;

    private boolean linearplot = true;

    private boolean freezefloor = true;

    private boolean thresholdexists = false;

    private boolean gaussfit = true;

    private String filename;

    private String wirename;

    private String direction;

    private String label;

    private String[] plottypes = { "Plot Linear Values", "Plot Log Values" };

    private String[] flooroptions = { "Freeze Offset at 0.0", "Fit Offset" };

    private String[] calcmodes = { "Gaussian Fit", "Statistical RMS" };

    private JComboBox scalechooser = new JComboBox(plottypes);

    private JComboBox floorchooser = new JComboBox(flooroptions);

    private JComboBox calcmodechooser = new JComboBox(calcmodes);

    NumberFormat numFor = NumberFormat.getNumberInstance();

    DecimalField[] result = new DecimalField[4];

    DecimalField[] err = new DecimalField[4];

    DecimalField vcut;

    DecimalField hnorm;

    DecimalField vnorm;

    DecimalField center;

    DecimalField fitthreshold;

    String currentdataname;

    ArrayList currentdata;

    double sdata[];

    double data[];

    double fitparams[] = new double[4];

    double fitparams_err[] = new double[4];

    GenDocument doc;

    EdgeLayout layout = new EdgeLayout();

    FunctionGraphsJPanel datapanel;

    ArrayList attributes;

    public AnalysisPanel() {
    }

    public AnalysisPanel(GenDocument aDocument) {
        doc = aDocument;
        storedresultspanel = new StoredResultsPanel(doc);
        makeComponents();
        setStyling();
        addComponents();
        setAction();
    }

    public void addComponents() {
        EdgeLayout layout = new EdgeLayout();
        mainPanel.setLayout(layout);
        layout.add(datapanel, mainPanel, 10, 15, EdgeLayout.LEFT);
        layout.add(scalechooser, mainPanel, 15, 235, EdgeLayout.LEFT);
        layout.add(removebutton, mainPanel, 15, 270, EdgeLayout.LEFT);
        layout.add(hnormpanel, mainPanel, 10, 305, EdgeLayout.LEFT);
        layout.add(centerpanel, mainPanel, 10, 345, EdgeLayout.LEFT);
        layout.add(vnormpanel, mainPanel, 10, 380, EdgeLayout.LEFT);
        layout.add(vcutpanel, mainPanel, 10, 415, EdgeLayout.LEFT);
        layout.add(gnormbutton, mainPanel, 15, 455, EdgeLayout.LEFT);
        layout.add(floorchooser, mainPanel, 230, 230, EdgeLayout.LEFT);
        layout.add(fitthreshpanel, mainPanel, 225, 255, EdgeLayout.LEFT);
        layout.add(calcmodechooser, mainPanel, 230, 300, EdgeLayout.LEFT);
        layout.add(fitbutton, mainPanel, 230, 335, EdgeLayout.LEFT);
        layout.add(fitresultspanel, mainPanel, 215, 375, EdgeLayout.LEFT);
        layout.add(storebutton, mainPanel, 65, 525, EdgeLayout.LEFT);
        layout.add(clearbutton, mainPanel, 195, 525, EdgeLayout.LEFT);
        this.add(mainPanel);
    }

    public void makeComponents() {
        mainPanel = new JPanel();
        mainPanel.setPreferredSize(new Dimension(420, 565));
        mainPanel.setBorder(BorderFactory.createTitledBorder("Profile Analysis"));
        datapanel = new FunctionGraphsJPanel();
        datapanel.setPreferredSize(new Dimension(400, 210));
        datapanel.setGraphBackGroundColor(Color.WHITE);
        currentdata = new ArrayList();
        attributes = new ArrayList();
        attributes.add(new DataAttribute("file", new String("").getClass(), true));
        attributes.add(new DataAttribute("wire", new String("").getClass(), true));
        attributes.add(new DataAttribute("direction", new String("").getClass(), true));
        attributes.add(new DataAttribute("data", new ArrayList().getClass(), false));
        resultsdatatable = new DataTable("DataTable", attributes);
        removebutton = new JButton("Remove Point");
        fitbutton = new JButton("Fit All Data");
        storebutton = new JButton("Store Results");
        hnormbutton = new JButton("H Normalize By:");
        vnormbutton = new JButton("V Normalize To:");
        gnormbutton = new JButton("Normalize By Fit");
        vcutbutton = new JButton("V Cut Below:     ");
        centerbutton = new JButton("H Offset By:      ");
        clearbutton = new JButton("Clear Stored Results");
        fitthreshbutton = new JButton("Fit Data Above:");
        filename = new String("");
        wirename = new String("");
        direction = new String("");
        label = new String("");
        numFor.setMinimumFractionDigits(3);
        for (int i = 0; i <= 3; i++) {
            result[i] = new DecimalField(0, 6, numFor);
            err[i] = new DecimalField(0, 6, numFor);
        }
        hnorm = new DecimalField(1.0, 4, numFor);
        vnorm = new DecimalField(1.0, 4, numFor);
        vcut = new DecimalField(0.01, 4, numFor);
        center = new DecimalField(0.0, 4, numFor);
        fitthreshold = new DecimalField(0.0, 4, numFor);
        rlabels[0] = new JLabel("Parameter");
        rlabels[1] = new JLabel("   Value");
        rlabels[2] = new JLabel("   Error");
        sigmalabel = new JLabel(" Sigma = ");
        amplabel = new JLabel(" Amp. = ");
        centerlabel = new JLabel(" Center = ");
        pedestallabel = new JLabel(" Offset = ");
        fitresultspanel = new JPanel();
        fitresultspanel.setPreferredSize(new Dimension(200, 130));
        fitresultspanel.setBorder(BorderFactory.createTitledBorder("Fit Results"));
        fitresultspanel.setLayout(new GridLayout(5, 3));
        fitresultspanel.add(rlabels[0]);
        fitresultspanel.add(rlabels[1]);
        fitresultspanel.add(rlabels[2]);
        fitresultspanel.add(sigmalabel);
        fitresultspanel.add(result[0]);
        fitresultspanel.add(err[0]);
        fitresultspanel.add(amplabel);
        fitresultspanel.add(result[1]);
        fitresultspanel.add(err[1]);
        fitresultspanel.add(centerlabel);
        fitresultspanel.add(result[2]);
        fitresultspanel.add(err[2]);
        fitresultspanel.add(pedestallabel);
        fitresultspanel.add(result[3]);
        fitresultspanel.add(err[3]);
        vcutpanel = new JPanel();
        vcutpanel.add(vcutbutton);
        vcutpanel.add(vcut);
        vnormpanel = new JPanel();
        vnormpanel.add(vnormbutton);
        vnormpanel.add(vnorm);
        hnormpanel = new JPanel();
        hnormpanel.add(hnormbutton);
        hnormpanel.add(hnorm);
        centerpanel = new JPanel();
        centerpanel.add(centerbutton);
        centerpanel.add(center);
        fitthreshpanel = new JPanel();
        fitthreshpanel.add(fitthreshbutton);
        fitthreshpanel.add(fitthreshold);
    }

    public void setAction() {
        scalechooser.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (scalechooser.getSelectedIndex() == 0) {
                    linearplot = true;
                } else {
                    linearplot = false;
                }
                plotData();
            }
        });
        floorchooser.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (floorchooser.getSelectedIndex() == 0) {
                    freezefloor = true;
                } else {
                    freezefloor = false;
                }
            }
        });
        calcmodechooser.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (calcmodechooser.getSelectedIndex() == 0) {
                    gaussfit = true;
                } else {
                    gaussfit = false;
                }
                plotData();
            }
        });
        removebutton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                removePoint();
            }
        });
        fitbutton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                thresholdexists = false;
                if (gaussfit) {
                    gaussFit(thresholdexists, 0.0);
                } else {
                    statFit(thresholdexists, 0.0);
                }
            }
        });
        hnormbutton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                normalizeHorizontal();
            }
        });
        vnormbutton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                normalizeVertical();
            }
        });
        gnormbutton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                normalizeByGaussian();
            }
        });
        vcutbutton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                cutVertical();
            }
        });
        centerbutton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                centerOffset();
            }
        });
        storebutton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                storeResult();
            }
        });
        clearbutton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                clearResults();
            }
        });
        fitthreshbutton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                thresholdexists = true;
                if (gaussfit) {
                    gaussFit(thresholdexists, fitthreshold.getValue());
                } else {
                    statFit(thresholdexists, fitthreshold.getValue());
                }
            }
        });
    }

    public void removePoint() {
        Integer index = datapanel.getPointChosenIndex();
        if (index != null) {
            int newsize = sdata.length - 1;
            int iindex = index.intValue();
            double[] oldsdata = sdata;
            double[] olddata = data;
            double[] tempsdata = new double[newsize];
            double[] tempdata = new double[newsize];
            for (int i = 0; i < newsize; i++) {
                if (i < iindex) {
                    tempsdata[i] = oldsdata[i];
                    tempdata[i] = olddata[i];
                } else {
                    tempsdata[i] = oldsdata[i + 1];
                    tempdata[i] = olddata[i + 1];
                }
            }
            sdata = tempsdata;
            data = tempdata;
            plotData();
        }
    }

    public void normalizeHorizontal() {
        double norm = hnorm.getValue();
        int size = sdata.length;
        double max = 0.0;
        for (int i = 0; i < size; i++) sdata[i] /= norm;
        dataHasBeenFit = false;
        plotData();
    }

    public void normalizeVertical() {
        double norm = vnorm.getValue();
        int size = sdata.length;
        double max = 0.0;
        for (int i = 0; i < size; i++) if (data[i] > max) max = data[i];
        if (max != 0.0) {
            for (int i = 0; i < size; i++) data[i] *= norm / max;
        }
        dataHasBeenFit = false;
        plotData();
    }

    public void normalizeByGaussian() {
        if (dataHasBeenFit) {
            double norm = vnorm.getValue();
            int size = sdata.length;
            double max = 0.0;
            for (int i = 0; i < size; i++) if (data[i] > max) max = data[i];
            for (int i = 0; i < size; i++) {
                sdata[i] -= fitparams[2];
                sdata[i] /= fitparams[0];
                data[i] -= fitparams[3];
                data[i] /= fitparams[1];
            }
            dataHasBeenFit = false;
            plotData();
        }
    }

    public void cutVertical() {
        double threshold = vcut.getValue();
        int gooddatapoints = 0;
        int currentsize = sdata.length;
        for (int i = 0; i < currentsize; i++) if (data[i] >= threshold) gooddatapoints++;
        double[] tempsdata = new double[gooddatapoints];
        double[] tempdata = new double[gooddatapoints];
        int j = 0;
        for (int i = 0; i < currentsize; i++) {
            if (data[i] >= threshold) {
                tempsdata[j] = sdata[i];
                tempdata[j] = data[i];
                j++;
            }
        }
        sdata = tempsdata;
        data = tempdata;
        dataHasBeenFit = false;
        plotData();
    }

    public void centerOffset() {
        double cent = center.getValue();
        int size = sdata.length;
        for (int i = 0; i < size; i++) sdata[i] -= cent;
        dataHasBeenFit = false;
        plotData();
    }

    public void gaussFit(boolean datathresh, double vthresh) {
        double[] snewdata;
        double[] newdata;
        if (datathresh) {
            double threshold = vthresh;
            int gooddatapoints = 0;
            int currentsize = sdata.length;
            for (int i = 0; i < currentsize; i++) if (data[i] >= threshold) gooddatapoints++;
            double[] tempsdata = new double[gooddatapoints];
            double[] tempdata = new double[gooddatapoints];
            int j = 0;
            for (int i = 0; i < currentsize; i++) {
                if (data[i] >= threshold) {
                    tempsdata[j] = sdata[i];
                    tempdata[j] = data[i];
                    j++;
                }
            }
            snewdata = tempsdata;
            newdata = tempdata;
        } else {
            snewdata = sdata;
            newdata = data;
        }
        Gaussian gs = new Gaussian();
        gs.setData(snewdata, newdata);
        gs.fitParameter(Gaussian.SIGMA, true);
        gs.fitParameter(Gaussian.AMP, true);
        gs.fitParameter(Gaussian.CENTER, true);
        gs.fitParameter(Gaussian.PEDESTAL, true);
        int iterations = 1;
        boolean result = gs.guessAndFit(iterations);
        gs.setParameter(Gaussian.SIGMA, gs.getParameter(Gaussian.SIGMA));
        gs.setParameter(Gaussian.AMP, gs.getParameter(Gaussian.AMP));
        gs.setParameter(Gaussian.CENTER, gs.getParameter(Gaussian.CENTER));
        if (freezefloor) {
            gs.setParameter(Gaussian.PEDESTAL, 0.0);
            gs.fitParameter(Gaussian.PEDESTAL, false);
        } else {
            gs.setParameter(Gaussian.PEDESTAL, gs.getParameter(Gaussian.PEDESTAL));
            gs.fitParameter(Gaussian.PEDESTAL, true);
        }
        iterations = 5;
        result = gs.fit();
        dataHasBeenFit = result;
        if (dataHasBeenFit) {
            fitparams[0] = gs.getParameter(Gaussian.SIGMA);
            fitparams[1] = gs.getParameter(Gaussian.AMP);
            fitparams[2] = gs.getParameter(Gaussian.CENTER);
            fitparams[3] = gs.getParameter(Gaussian.PEDESTAL);
            fitparams_err[0] = gs.getParameterError(Gaussian.SIGMA);
            fitparams_err[1] = gs.getParameterError(Gaussian.AMP);
            fitparams_err[2] = gs.getParameterError(Gaussian.CENTER);
            fitparams_err[3] = gs.getParameterError(Gaussian.PEDESTAL);
        }
        plotData();
        updateResultsPanel();
    }

    public void statFit(boolean datathresh, double vthresh) {
        double[] snewdata;
        double[] newdata;
        int currentsize = sdata.length;
        if (datathresh) {
            double threshold = vthresh;
            int gooddatapoints = 0;
            for (int i = 0; i < currentsize; i++) if (data[i] >= threshold) gooddatapoints++;
            double[] tempsdata = new double[gooddatapoints];
            double[] tempdata = new double[gooddatapoints];
            int j = 0;
            for (int i = 0; i < currentsize; i++) {
                if (data[i] >= threshold) {
                    tempsdata[j] = sdata[i];
                    tempdata[j] = data[i];
                    j++;
                }
            }
            snewdata = tempsdata;
            newdata = tempdata;
            currentsize = gooddatapoints;
        } else {
            snewdata = sdata;
            newdata = data;
        }
        double mean = 0.0;
        double sqrrms = 0.0;
        double rms = 0.0;
        double totals = 0.0;
        for (int i = 0; i < currentsize; i++) {
            mean += snewdata[i] * newdata[i];
            totals += newdata[i];
        }
        mean /= totals;
        for (int i = 0; i < currentsize; i++) {
            sqrrms += Math.pow((snewdata[i] - mean), 2) * newdata[i];
        }
        sqrrms /= totals;
        rms = Math.sqrt(sqrrms);
        fitparams[0] = rms;
        fitparams[1] = 0.0;
        fitparams[2] = mean;
        fitparams[3] = 0.0;
        fitparams_err[0] = 0.0;
        fitparams_err[1] = 0.0;
        fitparams_err[2] = 0.0;
        fitparams_err[3] = 0.0;
        updateResultsPanel();
    }

    public void storeResult() {
        GenericRecord record = new GenericRecord(resultsdatatable);
        ArrayList results = new ArrayList();
        double[] fit = new double[fitparams.length];
        System.arraycopy(fitparams, 0, fit, 0, fitparams.length);
        double[] errors = new double[fitparams_err.length];
        System.arraycopy(fitparams_err, 0, errors, 0, fitparams_err.length);
        results.add(fit);
        results.add(errors);
        results.add(sdata);
        results.add(data);
        Map bindings = new HashMap();
        bindings.put("file", filename);
        bindings.put("wire", wirename);
        bindings.put("direction", direction);
        if (resultsdatatable.record(bindings) != null) {
            resultsdatatable.remove(resultsdatatable.record(bindings));
        }
        record.setValueForKey(new String(filename), "file");
        record.setValueForKey(new String(wirename), "wire");
        record.setValueForKey(new String(direction), "direction");
        record.setValueForKey(new ArrayList(results), "data");
        resultsdatatable.add(record);
        doc.resultsdatatable = resultsdatatable;
    }

    public void clearResults() {
        if (resultsdatatable.records().size() == 0) {
            System.out.println("No records to remove!");
        } else {
            Collection records = resultsdatatable.records();
            Iterator itr = records.iterator();
            while (itr.hasNext()) {
                GenericRecord record = (GenericRecord) itr.next();
                resultsdatatable.remove(record);
            }
            doc.resultsdatatable = resultsdatatable;
        }
    }

    public void plotData() {
        datapanel.removeAllGraphData();
        BasicGraphData rawgraphdata = new BasicGraphData();
        BasicGraphData fitgraphdata = new BasicGraphData();
        if (!linearplot) {
            double temp;
            double[] logdata = new double[data.length];
            for (int i = 0; i < logdata.length; i++) {
                temp = data[i];
                if (temp <= 0.0) temp = 0.00001;
                logdata[i] = Math.log(temp) / Math.log(10);
            }
            rawgraphdata.addPoint(sdata, logdata);
        } else {
            rawgraphdata.addPoint(sdata, data);
        }
        rawgraphdata.setDrawPointsOn(true);
        rawgraphdata.setDrawLinesOn(false);
        rawgraphdata.setGraphProperty("Legend", new String("raw data"));
        rawgraphdata.setGraphColor(Color.RED);
        datapanel.addGraphData(rawgraphdata);
        if (dataHasBeenFit) {
            int i = 0;
            double a[] = fitparams;
            double xmin = a[2] - 5 * a[0];
            double xmax = a[2] + 5 * a[0];
            double points = 100.0;
            double inc = (xmax - xmin) / points;
            int npoints = (new Double(points)).intValue();
            double sfit[] = new double[npoints];
            double yfit[] = new double[npoints];
            double x = xmin;
            while (x <= xmax && i < npoints) {
                sfit[i] = x;
                yfit[i] = a[3] + a[1] * Math.exp(-(x - a[2]) * (x - a[2]) / (2.0 * a[0] * a[0]));
                x += inc;
                i++;
            }
            if (!linearplot) {
                double temp;
                double[] ylogfit = new double[yfit.length];
                for (int j = 0; j < ylogfit.length; j++) {
                    temp = yfit[j];
                    if (temp <= 0.0) temp = 0.00001;
                    ylogfit[j] = Math.log(temp) / Math.log(10);
                }
                fitgraphdata.addPoint(sfit, ylogfit);
            } else {
                fitgraphdata.addPoint(sfit, yfit);
            }
            fitgraphdata.setDrawPointsOn(false);
            fitgraphdata.setDrawLinesOn(true);
            fitgraphdata.setGraphProperty("Legend", new String("fit data"));
            fitgraphdata.setGraphColor(Color.BLACK);
            rawgraphdata.setDrawLinesOn(false);
            datapanel.addGraphData(fitgraphdata);
        }
        datapanel.setLegendButtonVisible(true);
        datapanel.setChooseModeButtonVisible(true);
        datapanel.setName("   " + label);
    }

    public void updateResultsPanel() {
        for (int i = 0; i <= 3; i++) {
            result[i].setValue(fitparams[i]);
            err[i].setValue(fitparams_err[i]);
        }
    }

    public void resetCurrentData(String file, String wire, String direct) {
        filename = file;
        wirename = wire;
        direction = direct;
        label = (new String(filename + ":" + wirename + ":" + direction));
        ArrayList currentdat = new ArrayList();
        ArrayList wiredata = new ArrayList();
        DataTable masterdatatable = doc.masterdatatable;
        Map bindings = new HashMap();
        bindings.put("file", filename);
        bindings.put("wire", wire);
        GenericRecord record = masterdatatable.record(bindings);
        wiredata = (ArrayList) record.valueForKey("data");
        ArrayList slist = (ArrayList) wiredata.get(0);
        ArrayList xlist = (ArrayList) wiredata.get(1);
        ArrayList ylist = (ArrayList) wiredata.get(2);
        ArrayList zlist = (ArrayList) wiredata.get(3);
        double[] sdat = new double[slist.size()];
        double[] dat = new double[slist.size()];
        double xmax = 0;
        double zmax = 0;
        for (int i = 0; i < slist.size(); i++) {
            if (Math.abs(((Double) xlist.get(i)).doubleValue()) > Math.abs(xmax)) xmax = ((Double) xlist.get(i)).doubleValue();
        }
        if (direction.equals("H")) {
            currentdat.add(slist);
            currentdat.add(xlist);
            for (int i = 0; i < slist.size(); i++) {
                sdat[i] = ((Double) slist.get(i)).doubleValue();
                if (xmax < 0) dat[i] = -((Double) xlist.get(i)).doubleValue(); else dat[i] = ((Double) xlist.get(i)).doubleValue();
            }
        }
        if (direction.equals("V")) {
            currentdat.add(slist);
            currentdat.add(ylist);
            for (int i = 0; i < slist.size(); i++) {
                sdat[i] = ((Double) slist.get(i)).doubleValue();
                if (xmax < 0) dat[i] = -((Double) ylist.get(i)).doubleValue(); else dat[i] = ((Double) ylist.get(i)).doubleValue();
            }
        }
        if (direction.equals("D")) {
            currentdat.add(slist);
            currentdat.add(zlist);
            for (int i = 0; i < slist.size(); i++) {
                sdat[i] = Math.sqrt(2.0) * (((Double) slist.get(i)).doubleValue());
                if (xmax < 0) dat[i] = -((Double) zlist.get(i)).doubleValue(); else dat[i] = ((Double) zlist.get(i)).doubleValue();
            }
        }
        currentdata.clear();
        currentdata = currentdat;
        sdata = sdat;
        data = dat;
    }

    public void setStyling() {
    }

    public void setNewDataFlag(boolean flag) {
        dataHasBeenFit = !flag;
    }
}
