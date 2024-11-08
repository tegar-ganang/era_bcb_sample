package vademecum.visualizer.kdeplot.dialogs;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.NumberFormatter;
import vademecum.visualizer.kdeplot.KDEPlot;

public class BandWidthDialog extends JDialog {

    final KDEPlot plot;

    private javax.swing.JButton jButton1;

    private javax.swing.JButton jButton2;

    private javax.swing.JLabel jLabel1;

    private javax.swing.JSlider jSlider1;

    private JFormattedTextField jTextField1;

    public BandWidthDialog(KDEPlot plot) {
        super((JFrame) plot.getFigurePanel().getGraphicalViewer());
        setTitle("Bandwidth Selection");
        this.plot = plot;
        add(new BandwidthSelector());
        setSize(220, 140);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    }

    public static void main(String[] args) {
        new BandWidthDialog(null).setVisible(true);
    }

    public class BandwidthSelector extends javax.swing.JPanel {

        /** Creates new form BandwidthSelector */
        public BandwidthSelector() {
            initComponents();
        }

        private void initComponents() {
            jButton1 = new javax.swing.JButton();
            jButton2 = new javax.swing.JButton();
            jSlider1 = new javax.swing.JSlider();
            java.text.NumberFormat numberFormat = java.text.NumberFormat.getNumberInstance();
            NumberFormatter formatter = new NumberFormatter(numberFormat);
            jTextField1 = new JFormattedTextField(formatter);
            jLabel1 = new javax.swing.JLabel();
            setLayout(null);
            jButton1.setText("Close");
            add(jButton1);
            jButton1.setBounds(10, 80, 70, 20);
            jButton1.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent arg0) {
                    dispose();
                }
            });
            jButton2.setText("Apply");
            add(jButton2);
            jButton2.setBounds(130, 80, 70, 20);
            jButton2.addActionListener(new ApplyAction());
            jTextField1.setText("jTextField1");
            add(jTextField1);
            jTextField1.setBounds(110, 20, 80, 19);
            add(jSlider1);
            jSlider1.setBounds(10, 50, 190, 16);
            jSlider1.setMinimum(0);
            jSlider1.setMaximum(100);
            jSlider1.addChangeListener(new ChangeListener() {

                public void stateChanged(ChangeEvent arg0) {
                    int val = jSlider1.getValue();
                    double bval = (double) val / 10d;
                    if (bval == 0d) {
                        bval = 0.1;
                    }
                    jTextField1.setValue(bval);
                }
            });
            if (plot != null) {
                jSlider1.setValue((int) plot.getBandWidth() * 10);
            } else {
                jSlider1.setValue(15);
            }
            jLabel1.setText("Bandwidth :");
            add(jLabel1);
            jLabel1.setBounds(20, 20, 80, 15);
        }
    }

    class ApplyAction implements ActionListener {

        public void actionPerformed(ActionEvent arg0) {
            double bw = (Double) jTextField1.getValue();
            System.out.println("Applying Bandwidth : " + bw);
            if (plot != null) {
                plot.setBandWidth(bw);
                plot.refreshPlot();
            } else {
                System.err.println("err: no plot defined");
            }
        }
    }
}
