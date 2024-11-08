package jat.oppoc.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.renderable.ParameterBlock;
import jat.oppoc.sunjai.Colorbar;
import jat.oppoc.sunjai.XYPlot;
import javax.media.jai.Histogram;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;

/**
 * @author Jacek A. Teska
 *
 */
public class UIStatistic extends JPanel {

    private JRadioButton[] jrb;

    private XYPlot graphHistogram;

    private Colorbar colorbar;

    private JPanel channelPanel = null;

    private JLabel channelLabel = null;

    private JPanel meanPanel = null;

    private JTextField mean = null;

    private JPanel minPanel = null;

    private JTextField min = null;

    private JPanel maxPanel = null;

    private JTextField max = null;

    private Histogram histogram;

    private double[][] extrema;

    private double[] means;

    public UIStatistic(PlanarImage pi, int bandInit) {
        setLayout(new BorderLayout());
        jrb = new JRadioButton[4];
        JPanel p1 = new JPanel() {

            Insets insets = new Insets(2, 2, 2, 2);

            public Insets getInsets() {
                return insets;
            }
        };
        p1.setLayout(new BoxLayout(p1, BoxLayout.X_AXIS));
        p1.setBorder(new EtchedBorder());
        ButtonGroup bg = new ButtonGroup();
        jrb[0] = new JRadioButton(UITools.getString("statistic.Red"));
        jrb[0].addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                setHistogramParam(0);
            }
        });
        jrb[0].setSelected(bandInit == 0);
        bg.add(jrb[0]);
        p1.add(jrb[0]);
        jrb[1] = new JRadioButton(UITools.getString("statistic.Green"));
        jrb[1].addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                setHistogramParam(1);
            }
        });
        jrb[1].setSelected(bandInit == 1);
        bg.add(jrb[1]);
        p1.add(jrb[1]);
        jrb[2] = new JRadioButton(UITools.getString("statistic.Blue"));
        jrb[2].addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                setHistogramParam(2);
            }
        });
        jrb[2].setSelected(bandInit == 2);
        bg.add(jrb[2]);
        p1.add(jrb[2]);
        jrb[3] = new JRadioButton(UITools.getString("statistic.Gray"));
        jrb[3].addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                setHistogramParam(3);
            }
        });
        jrb[3].setSelected(bandInit == 3);
        bg.add(jrb[3]);
        p1.add(jrb[3]);
        add(p1, BorderLayout.NORTH);
        p1 = new JPanel(new BorderLayout());
        p1.setBorder(new EtchedBorder());
        graphHistogram = new XYPlot();
        graphHistogram.setBorder(new LineBorder(new Color(0, 0, 255), 1));
        p1.add(graphHistogram, BorderLayout.CENTER);
        colorbar = new Colorbar();
        colorbar.setBorder(new LineBorder(new Color(255, 0, 255), 2));
        colorbar.setPreferredSize(new Dimension(250, 25));
        p1.add(colorbar, BorderLayout.SOUTH);
        add(p1, BorderLayout.CENTER);
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(pi);
        RenderedOp op = JAI.create("histogram", pb, null);
        histogram = (Histogram) op.getProperty("histogram");
        op = JAI.create("extrema", pb, null);
        extrema = (double[][]) op.getProperty("extrema");
        op = JAI.create("mean", pb, null);
        means = (double[]) op.getProperty("mean");
        setHistogramParam(bandInit);
    }

    public void setHistogramParam(int band) {
        Color color;
        int colorComp;
        if (band == 0) {
            color = Color.red;
            colorComp = Colorbar.RedColorComp;
        } else if (band == 1) {
            color = Color.green;
            colorComp = Colorbar.GreenColorComp;
        } else if (band == 2) {
            color = Color.blue;
            colorComp = Colorbar.BlueColorComp;
        } else {
            color = Color.gray;
            colorComp = Colorbar.GrayColorComp;
        }
        setChannelLabel();
        if (jrb[band].isSelected()) if (band == 3) {
            int[] array = new int[histogram.getNumBins(0)];
            int av;
            for (int i = 0; i < histogram.getNumBins(0); i++) {
                av = histogram.getBinSize(0, i) + histogram.getBinSize(1, i) + histogram.getBinSize(2, i);
                av = av / 3;
                array[i] = av;
            }
            graphHistogram.setColor(color);
            graphHistogram.plot(array);
            colorbar.setBackground(color);
            colorbar.setColorComp(colorComp);
            colorbar.setSize(colorbar.getWidth(), 25);
            if (mean != null) {
                mean.setText(Double.toString((means[0] + means[1] + means[2]) / 3));
            }
            if (min != null) {
                min.setText(Double.toString((extrema[0][0] + extrema[0][1] + extrema[0][2]) / 3));
            }
            if (max != null) {
                max.setText(Double.toString((extrema[1][0] + extrema[1][1] + extrema[1][2]) / 3));
            }
        } else {
            int[] array = new int[histogram.getNumBins(band)];
            for (int i = 0; i < histogram.getNumBins(band); i++) array[i] = histogram.getBinSize(band, i);
            graphHistogram.setColor(color);
            graphHistogram.plot(array);
            colorbar.setBackground(color);
            colorbar.setColorComp(colorComp);
            colorbar.setSize(colorbar.getWidth(), 25);
            if (mean != null) {
                mean.setText(Double.toString(means[band]));
            }
            if (min != null) {
                min.setText(Double.toString(extrema[0][band]));
            }
            if (max != null) {
                max.setText(Double.toString(extrema[1][band]));
            }
        }
    }

    /**
	 * @return BandSettings on basis of radio buttons
	 */
    public int getBandSettings() {
        int ret;
        if (jrb[0].isSelected()) ret = 0; else if (jrb[1].isSelected()) ret = 1; else if (jrb[2].isSelected()) ret = 2; else ret = 3;
        return ret;
    }

    public JPanel getMeanPanel(String name) {
        if (meanPanel == null) {
            meanPanel = new JPanel();
            meanPanel.add(new JLabel(name));
            mean = new JTextField();
            if (getBandSettings() == 3) mean.setText(Double.toString((means[0] + means[1] + means[2]) / 3)); else mean.setText(Double.toString(means[getBandSettings()]));
            mean.setEditable(false);
            meanPanel.add(mean);
        }
        return meanPanel;
    }

    public JPanel getMinPanel(String name) {
        if (minPanel == null) {
            minPanel = new JPanel();
            minPanel.add(new JLabel(name));
            min = new JTextField();
            if (getBandSettings() == 3) min.setText(Double.toString((extrema[0][0] + extrema[0][1] + extrema[0][2]) / 3)); else min.setText(Double.toString(extrema[0][getBandSettings()]));
            min.setEditable(false);
            minPanel.add(min);
        }
        return minPanel;
    }

    public JPanel getMaxPanel(String name) {
        if (maxPanel == null) {
            maxPanel = new JPanel();
            maxPanel.add(new JLabel(name));
            max = new JTextField();
            if (getBandSettings() == 3) max.setText(Double.toString((extrema[1][0] + extrema[1][1] + extrema[1][2]) / 3)); else max.setText(Double.toString(extrema[1][getBandSettings()]));
            max.setEditable(false);
            maxPanel.add(max);
        }
        return maxPanel;
    }

    public JPanel getChannelPanel(String name) {
        if (channelPanel == null) {
            channelPanel = new JPanel();
            channelPanel.add(new JLabel(name));
            channelLabel = new JLabel();
            channelLabel.setFont(new Font(UITools.getString("dw.fontlabel"), Font.PLAIN, 14));
            setChannelLabel();
            channelPanel.add(channelLabel);
        }
        return channelPanel;
    }

    private void setChannelLabel() {
        if (channelLabel != null) {
            if (jrb[0].isSelected()) channelLabel.setText(UITools.getString("statistic.Red")); else if (jrb[1].isSelected()) channelLabel.setText(UITools.getString("statistic.Green")); else if (jrb[2].isSelected()) channelLabel.setText(UITools.getString("statistic.Blue")); else channelLabel.setText(UITools.getString("statistic.Gray"));
        }
    }
}
