package vademecum.data;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import javax.swing.JDialog;
import vademecum.Core;
import vademecum.core.experiment.ExperimentNode;
import vademecum.extensionPoint.DefaultDataNode;
import vademecum.extensionPoint.ExtensionFactory;
import vademecum.math.density.pareto.ParetoDensity;
import vademecum.math.statistics.Univariate;
import vademecum.ui.project.DataNavigation;
import vademecum.ui.project.Expertice;

public class POutlierPlugin extends DefaultDataNode implements ActionListener {

    private static final String ACTION_OK = "ok";

    JDialog preferencesDialog;

    Retina retina_in = new Retina(1, 1, 1, true);

    Retina retina_out = retina_in;

    IDataGrid data_in, data_out;

    PMatrix pMatrix;

    ParetoDensity pdens = new ParetoDensity();

    private Vector<IBestMatch> bmList;

    int bm_index = 0;

    /** Threshold for Outlier Search */
    double heightPercentage = 0d;

    double heightCut = 0d;

    int numberOfOutliers;

    double min;

    double max;

    boolean autoSeek = false;

    /** Option for Setting the k-minimal value search*/
    boolean seekMinKValues = false;

    /** Parameter for returning k minimal values */
    int minK = 0;

    ArrayList<Integer> outlierIndices = new ArrayList<Integer>();

    ExperimentNode clustering = null;

    /** Simple Clusterer Reference */
    ExperimentNode newNode;

    int tagValue = 15;

    Hashtable<IBestMatch, Double> ht = new Hashtable<IBestMatch, Double>();

    SortedArray sortedArray = new SortedArray();

    public POutlierPlugin() {
        super();
    }

    public String getName() {
        return "PMatrix Densities";
    }

    public String getResultText() {
        NumberFormat fmt = NumberFormat.getInstance();
        fmt.setMaximumFractionDigits(3);
        StringBuffer stb = new StringBuffer();
        stb.append("Number of Values found " + String.valueOf(numberOfOutliers));
        stb.append("\nMin :" + min);
        stb.append("\nCut :" + fmt.format(heightCut) + " (" + fmt.format(heightPercentage) + " %)");
        stb.append("\nMax :" + max);
        stb.append("\n10 Values with lowest PMatrix Height:");
        for (int i = 0; i < Math.min(sortedArray.size(), 10); i++) {
            Sortable s = (Sortable) sortedArray.elementAt(i);
            IBestMatch object_key = (IBestMatch) s.getObject();
            Double integer_object = (Double) s.getKey();
            stb.append("\nIndex : " + object_key.getIndex() + " with " + integer_object);
        }
        return stb.toString();
    }

    public ParetoDensity getParetoDensity() {
        return this.pdens;
    }

    public void setInput(Class inputType, Object data) {
        if (inputType == Retina.class) {
            retina_in = (Retina) ((Retina) data).copy();
            data_in = (IDataGrid) ((Retina) data).getInputVectors().copy();
            pdens.setDataGrid(data_in);
            pdens.setCenters(GridUtils.retinaToGrid(retina_in));
            pdens.setParetoRadius();
        }
    }

    public Object getOutput(Class outputType) {
        if (outputType == IDataGrid.class) {
            return data_out;
        }
        return null;
    }

    public JDialog getPreferencesDialog(Frame owner) {
        return new ContingentSelDialog(this, owner);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand() == ACTION_OK) {
            this.firePropertiesChangedEvent();
            this.preferencesDialog.dispose();
        }
    }

    public void setDensityPercentage(double perc) {
        this.heightPercentage = perc;
        firePropertiesChangedEvent();
    }

    public void setAutoSeek(boolean b) {
        this.autoSeek = b;
    }

    public void setMinKValuesSearch(boolean b) {
        this.seekMinKValues = b;
        firePropertiesChangedEvent();
    }

    public void setMinK(int k) {
        this.minK = k;
    }

    /**
	 * Setting the class Label for the found values.
	 * The Plugin "SimpleClusterer" will be initialized with that.
	 * 
	 * @param tagnum
	 */
    public void setTagValue(int tagnum) {
        this.tagValue = tagnum;
    }

    public void init() {
        sortedArray.clear();
        outlierIndices.clear();
        ht.clear();
        bm_index = 0;
        data_out = data_in;
        this.fireProgressChangedEvent(10, "calculate P-Matrix...");
        pMatrix = new PMatrix(this.retina_in, this.data_in);
        pMatrix.setParetoDensity(this.pdens);
        log.debug("Start calculating heights of the P-Matrix...");
        pMatrix.calculateHeights();
        log.debug("finished calculating heights of the P-Matrix.");
        this.fireProgressChangedEvent(20, "searching for Outliers...");
        this.bmList = (Vector<IBestMatch>) retina_in.getBMList().clone();
        max = HeightMatrixUtils.getMax(this.pMatrix);
        heightCut = max * heightPercentage / 100d;
        log.info("HeightCut (" + heightPercentage + "%) = " + heightCut);
        min = Double.MAX_VALUE;
    }

    public void iterate() {
        IBestMatch bm = bmList.get(bm_index);
        int bmrow = bm.getRow();
        int bmcol = bm.getColumn();
        double ph = pMatrix.getHeight(bmrow, bmcol);
        ht.put(bm, ph);
        if (autoSeek == false && this.seekMinKValues == false) {
            if (ph <= heightCut) {
                log.info((bm_index + 1) + ": " + ph);
                outlierIndices.add(bm_index);
            }
        }
        bm_index++;
        int progress = Math.round(100f * (float) bm_index / (float) bmList.size());
        this.fireProgressChangedEvent(progress, "examining Bestmatch " + bm_index + " ...");
    }

    public boolean hasFinished() {
        if (bm_index >= bmList.size()) {
            log.debug("P Outlier finished.");
            for (Enumeration e = ht.keys(); e.hasMoreElements(); ) {
                IBestMatch object_key = (IBestMatch) e.nextElement();
                Double double_object = (Double) ht.get(object_key);
                Sortable s = new Sortable(double_object, object_key);
                sortedArray.add(s);
            }
            Collections.sort(sortedArray);
            if (autoSeek == true) {
                double[] vals = new double[sortedArray.size()];
                double[] diffvals = new double[sortedArray.size()];
                for (int i = 0; i < sortedArray.size(); i++) {
                    Sortable s = (Sortable) sortedArray.elementAt(i);
                    Double integer_object = (Double) s.getKey();
                    vals[i] = integer_object;
                }
                double cn = 0d;
                double[] conc = new double[vals.length];
                for (int i = 0; i < vals.length; i++) {
                    if (i < vals.length - 1) {
                        diffvals[i] = vals[i + 1] - vals[i];
                    } else {
                        diffvals[i] = diffvals[i - 1];
                    }
                    cn += diffvals[i];
                    conc[i] = diffvals[i] * vals[i];
                }
                double vmean = Univariate.getMean(vals);
                for (int i = 0; i < conc.length; i++) {
                    conc[i] = conc[i] / (cn * vmean);
                }
                data_out = GridUtils.addArray(this.data_in, vals, "Densities");
                data_out = GridUtils.addArray(this.data_out, diffvals, "DiffDensities");
                data_out = GridUtils.addArray(this.data_out, conc, "ConcDensities");
                int markIndex = 0;
                double ascendingConcentration = 0d;
                for (int i = 0; i < conc.length; i++) {
                    if (conc[i] == 0d) {
                    } else {
                        if (conc[i] >= ascendingConcentration) {
                            ascendingConcentration = conc[i];
                        } else {
                            markIndex = i;
                            break;
                        }
                    }
                }
                System.out.println("Ascended to Index : " + markIndex);
                System.out.println("Please Check Indices:");
                for (int i = 0; i < markIndex; i++) {
                    Sortable s = (Sortable) sortedArray.elementAt(i);
                    IBestMatch object_key = (IBestMatch) s.getObject();
                    int outl = object_key.getIndex();
                    outlierIndices.add(outl);
                }
            }
            if (this.seekMinKValues) {
                outlierIndices.clear();
                for (int i = 0; i < this.minK; i++) {
                    Sortable s = (Sortable) sortedArray.elementAt(i);
                    IBestMatch object_key = (IBestMatch) s.getObject();
                    int outl = object_key.getIndex();
                    System.out.println("min k=" + i + " : " + outl);
                    outlierIndices.add(outl);
                }
            }
            log.info("Number of possible Outliers : " + outlierIndices.size());
            this.numberOfOutliers = outlierIndices.size();
            for (int indy : outlierIndices) {
                log.debug(indy);
            }
            if (outlierIndices.size() > 0) {
                if (newNode == null) {
                    DataNavigation nav = ((Expertice) Core.projectPanel.getSelectedComponent()).getDataNavigation();
                    newNode = ExtensionFactory.createDataNode("vademecum.clusterer.simpleClusterer@simpleClusterer");
                    ExperimentNode actNode = (ExperimentNode) nav.getLastSelectedPathComponent();
                    ExperimentNode parentNode = (ExperimentNode) actNode.getParent();
                    nav.addNode(parentNode, newNode);
                }
                int[] clusterinfo = new int[data_out.getNumRows()];
                for (int i = 0; i < this.outlierIndices.size(); i++) {
                    clusterinfo[outlierIndices.get(i)] = this.tagValue;
                }
                String clusterstr = "";
                for (Integer i : clusterinfo) clusterstr += "," + String.valueOf(i);
                newNode.getMethod().setProperty("clustering", clusterstr);
                newNode.setState(ExperimentNode.READY);
                newNode.getMethod().init();
            }
            return true;
        }
        return false;
    }

    public void reset() {
        this.bm_index = 0;
    }

    public void load(File folder) {
    }

    public void save(File folder) {
    }
}
