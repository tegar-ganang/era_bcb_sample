package vademecum.visualizer.pmatrix2d.dialogs;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.ArrayList;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.NumberFormatter;
import vademecum.math.density.pareto.ParetoDensity;
import vademecum.ui.visualizer.VisualizerFrame;
import vademecum.ui.visualizer.vgraphics.VGraphics;
import vademecum.visualizer.heightMatrix.widgets.ColorLegend;
import vademecum.visualizer.pmatrix2d.PMatrix2D;

public class SphereRadiusDialog extends JDialog implements ComponentListener {

    int initialPercentileValue = 3;

    int initialNumberOfClusters = 6;

    double initialRadius = 1.2345;

    ParetoDensity pdens;

    PMatrix2D plot;

    public SphereRadiusDialog(PMatrix2D plot) {
        super((JFrame) plot.getFigurePanel().getGraphicalViewer());
        add(new ParetoRadiusPanel());
        setSize(365, 255);
        this.plot = plot;
        if (pdens != null) {
            pdens = plot.getParetoDensity();
            initialPercentileValue = pdens.getPercentile();
            initialNumberOfClusters = pdens.getClusters();
            initialRadius = pdens.getRadius();
        }
        addComponentListener(this);
    }

    public static void main(String[] args) {
        SphereRadiusDialog testapp = new SphereRadiusDialog(null);
        testapp.setVisible(true);
        testapp.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    }

    private javax.swing.JButton jButton1;

    private javax.swing.JButton jButton2;

    private javax.swing.JButton jButton3;

    private javax.swing.JLabel jLabel1;

    private javax.swing.JLabel jLabel2;

    private javax.swing.JLabel jLabel3;

    private javax.swing.JSeparator jSeparator1;

    private javax.swing.JSlider jSlider1;

    private javax.swing.JSpinner jSpinner1;

    private javax.swing.JTextField jTextField1;

    private JFormattedTextField jTextField2;

    /**
 * Panel as Inner class 
 */
    class ParetoRadiusPanel extends javax.swing.JPanel {

        /** Creates new form ParetoRadiusPanel */
        public ParetoRadiusPanel() {
            initComponents();
        }

        private void initComponents() {
            jButton1 = new javax.swing.JButton();
            jButton2 = new javax.swing.JButton();
            jButton3 = new javax.swing.JButton();
            jLabel1 = new javax.swing.JLabel();
            jSpinner1 = new javax.swing.JSpinner(new SpinnerNumberModel(initialNumberOfClusters, 1, 40, 1));
            jSlider1 = new javax.swing.JSlider(javax.swing.JSlider.HORIZONTAL, 0, 99, initialPercentileValue);
            jLabel2 = new javax.swing.JLabel();
            jTextField1 = new javax.swing.JTextField();
            jLabel3 = new javax.swing.JLabel();
            java.text.NumberFormat numberFormat = java.text.NumberFormat.getNumberInstance();
            NumberFormatter formatter = new NumberFormatter(numberFormat);
            jTextField2 = new JFormattedTextField(formatter);
            jSeparator1 = new javax.swing.JSeparator();
            setLayout(null);
            jButton1.setText("Close");
            add(jButton1);
            jButton1.setBounds(30, 190, 70, 25);
            jButton1.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent arg0) {
                    dispose();
                }
            });
            jButton2.setText("Determine ParetoRadius");
            add(jButton2);
            jButton2.setBounds(112, 70, 218, 20);
            jButton2.addActionListener(new DetermineParetoRadiusAction());
            jButton3.setText("Apply");
            add(jButton3);
            jButton3.setBounds(250, 190, 70, 25);
            jButton3.addActionListener(new ApplyAction());
            jLabel1.setText("Number of expected Clusters :");
            add(jLabel1);
            jLabel1.setBounds(20, 20, 200, 15);
            add(jSpinner1);
            jSpinner1.setBounds(240, 20, 40, 20);
            add(jSlider1);
            jSlider1.setBounds(20, 140, 200, 16);
            jSlider1.addChangeListener(new SliderChangeListener());
            jLabel2.setText("Distance Percentiles :");
            add(jLabel2);
            jLabel2.setBounds(20, 120, 140, 15);
            jTextField1.setEditable(false);
            jTextField1.setText(Integer.toString(initialPercentileValue));
            add(jTextField1);
            jTextField1.setBounds(240, 140, 40, 19);
            jLabel3.setText("Sphere Radius :");
            add(jLabel3);
            jLabel3.setBounds(20, 50, 130, 15);
            jTextField2.setValue(new Double(initialRadius));
            add(jTextField2);
            jTextField2.setBounds(30, 70, 69, 19);
            add(jSeparator1);
            jSeparator1.setBounds(20, 100, 310, 10);
        }
    }

    class ApplyAction implements ActionListener {

        public void actionPerformed(ActionEvent arg0) {
            double radius = (Double) jTextField2.getValue();
            System.out.println("Applying Radius : " + radius);
            if (pdens != null) {
                pdens.setRadius(radius);
                pdens.calculateDensities();
            }
            plot.refreshPlot();
            if (plot.getLegend() != null) {
                plot.getLegend().setMin(0d);
                plot.getLegend().setMax(plot.getMaxHeight());
                plot.getLegend().repaint();
            }
        }
    }

    /** Auto Determination of Pareto Radius with ALU's Algorithm */
    class DetermineParetoRadiusAction implements ActionListener {

        public void actionPerformed(ActionEvent arg0) {
            System.out.println("Searching the  Radius with ");
            int numclusters = (Integer) jSpinner1.getValue();
            System.out.println("#Clusters : " + numclusters + "  and ");
            if (pdens != null) {
                pdens.setClusters(numclusters);
                double paretoRadius = pdens.getParetoRadius();
                int paretoPercentile = pdens.getPercentile();
                jTextField2.setValue(new Double(paretoRadius));
                jSlider1.setValue(paretoPercentile);
            }
        }
    }

    class SliderChangeListener implements ChangeListener {

        public void stateChanged(ChangeEvent arg0) {
            jTextField1.setText(Integer.toString(jSlider1.getValue()));
            if (pdens != null) {
                System.out.println("Calculating Radius for");
                int numclusters = (Integer) jSpinner1.getValue();
                System.out.println("#Clusters : " + numclusters + "  and ");
                int ppercentile = jSlider1.getValue();
                System.out.println("Pareto Percentile : " + ppercentile);
                pdens.setClusters(numclusters);
                pdens.setPercentile(ppercentile);
                double pradius = pdens.getParetoPercentileRadius();
                jTextField2.setValue(new Double(pradius));
            }
        }
    }

    public void componentHidden(ComponentEvent arg0) {
    }

    public void componentMoved(ComponentEvent arg0) {
    }

    public void componentResized(ComponentEvent arg0) {
    }

    /**
	 * Update Elements when GUI is visible
	 */
    public void componentShown(ComponentEvent arg0) {
        if (plot != null) {
            pdens = plot.getParetoDensity();
            double radius = pdens.getRadius();
            ;
            jTextField2.setValue(new Double(radius));
            int clusters = pdens.getClusters();
            jSpinner1.setValue(clusters);
            int percentile = pdens.getPercentile();
            jSlider1.setValue(percentile);
        }
    }
}
