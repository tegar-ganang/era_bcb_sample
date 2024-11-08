package shu.cms.applet.gradient.measure;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import shu.cms.colorspace.depend.*;
import shu.cms.colorspace.independ.*;
import shu.cms.devicemodel.lcd.*;
import shu.cms.hvs.gradient.*;
import shu.cms.lcd.*;
import shu.cms.measure.*;
import shu.cms.measure.meter.*;
import shu.cms.plot.*;
import shu.cms.util.*;
import shu.ui.*;
import shu.util.log.*;

/**
 * <p>Title: Colour Management System</p>
 *
 * <p>Description: a Colour Management System by Java</p>
 *
 * <p>Copyright: Copyright (c) 2008</p>
 *
 * <p>Company: skygroup</p>
 *
 * @author skyforce
 * @version 1.0
 */
public class SmoothMeasureFrame extends JFrame {

    protected JPanel contentPane;

    protected BorderLayout borderLayout1 = new BorderLayout();

    protected JToolBar jToolBar = new JToolBar();

    private String targetLogoFilename;

    public SmoothMeasureFrame(String targetLogoFilename) {
        this(targetLogoFilename, null, null);
    }

    public SmoothMeasureFrame(String targetLogoFilename, Meter meter, RGBBase.Channel targetChannel) {
        this.targetLogoFilename = targetLogoFilename;
        this.meter = meter;
        this.targetChannel = targetChannel;
        try {
            setDefaultCloseOperation(EXIT_ON_CLOSE);
            jbInit();
            myInit();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private void myInit() throws Exception {
        desktopPane.putClientProperty("JDesktopPane.dragMode", "outline");
        GUIUtils.defaultScreen(this);
        initInstr();
    }

    protected void initInstr() {
        if (mm == null) {
            if (meter == null) {
                meter = RemoteMeter.getDefaultInstance();
            }
            mm = new MeterMeasurement(meter, false);
            if (targetLogoFilename != null) {
                LCDTarget lcdTarget = LCDTarget.Instance.getFromLogo(targetLogoFilename);
                targetxyYArray = lcdTarget.filter.xyYArray();
            }
        }
        this.jButton_MeasureW.setEnabled(true);
        this.jButton_MeasureR.setEnabled(true);
        this.jButton_MeasureG.setEnabled(true);
        this.jButton_MeasureB.setEnabled(true);
    }

    private RGBBase.Channel targetChannel;

    private static final RGBBase.Channel getTargetChannel(LCDTarget lcdTarget) {
        LCDTarget.Number number = lcdTarget.getNumber();
        switch(number) {
            case Ramp256W:
                return RGBBase.Channel.W;
            case Ramp256R:
            case Ramp256R_W:
                return RGBBase.Channel.R;
            case Ramp256G:
            case Ramp256G_W:
                return RGBBase.Channel.G;
            case Ramp256B:
            case Ramp256B_W:
                return RGBBase.Channel.B;
            default:
                return null;
        }
    }

    private CIExyY[] targetxyYArray;

    private Meter meter;

    private MeterMeasurement mm = null;

    protected JDesktopPane desktopPane = new JDesktopPane();

    protected JButton jButton_MeasureW = new JButton();

    protected JButton jButton_Calibrate = new JButton();

    protected JButton jButton_MeasureR = new JButton();

    protected JButton jButton_MeasureG = new JButton();

    protected JButton jButton_MeasureB = new JButton();

    protected JButton jButton_CPCode = new JButton();

    protected JFileChooser jFileChooser1 = new JFileChooser();

    /**
     * Component initialization.
     *
     * @throws java.lang.Exception
     */
    private void jbInit() throws Exception {
        contentPane = (JPanel) getContentPane();
        contentPane.setLayout(borderLayout1);
        setTitle("Smooth Measure");
        jButton_MeasureW.setEnabled(false);
        jButton_MeasureW.setText("white");
        jButton_MeasureW.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                jButton_MeasureW_actionPerformed(e);
            }
        });
        desktopPane.setBackground(Color.black);
        jButton_Calibrate.setText("calibrate");
        jButton_Calibrate.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                jButton_Calibrate_actionPerformed(e);
            }
        });
        jButton_MeasureR.setEnabled(false);
        jButton_MeasureR.setText("red");
        jButton_MeasureR.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                jButton_MeasureR_actionPerformed(e);
            }
        });
        jButton_MeasureG.setEnabled(false);
        jButton_MeasureG.setText("green");
        jButton_MeasureG.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                jButton_MeasureG_actionPerformed(e);
            }
        });
        jButton_MeasureB.setEnabled(false);
        jButton_MeasureB.setText("blue");
        jButton_MeasureB.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                jButton_MeasureB_actionPerformed(e);
            }
        });
        jButton_CPCode.setText("CPCode");
        jButton_CPCode.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                jButton_CPCode_actionPerformed(e);
            }
        });
        jToolBar.setFloatable(false);
        jToolBar.add(jButton_Calibrate);
        jToolBar.add(jButton_MeasureW);
        jToolBar.add(jButton_MeasureR);
        jToolBar.add(jButton_MeasureG);
        jToolBar.add(jButton_MeasureB);
        jToolBar.add(jButton_CPCode);
        desktopPane.putClientProperty("JDesktopPane.dragMode", "outline");
        contentPane.add(desktopPane, java.awt.BorderLayout.CENTER);
        contentPane.add(jToolBar, java.awt.BorderLayout.NORTH);
    }

    public static void main(String[] args) {
        Meter m = null;
        SmoothMeasureFrame frame = new SmoothMeasureFrame(null, m, RGBBase.Channel.G);
        frame.setVisible(true);
    }

    private void measure(final RGBBase.Channel ch, final boolean plotDeltaTarget) {
        new Thread() {

            public void run() {
                LCDTargetBase.Number number = LCDTargetBase.Number.getRamp256Number(ch, true);
                int end = 255;
                LCDTarget lcdTarget = LCDTarget.Measured.measure(mm, number);
                GSDFGradientModel.setAnalyzeRange(0, end);
                GSDFGradientModel gm = new GSDFGradientModel(lcdTarget);
                gm.setImageChannel(ch);
                gm.setPatternSign(GSDFGradientModel.PatternSign.Threshold);
                gm.setTargetxyYArray(targetxyYArray);
                gm.setRecommendThresholdPercent(ch);
                Plot2D plot = gm.plotPatternAccelInfo(null, true, plotDeltaTarget);
                String title = ch.toString() + ((cpcodeFilename != null) ? " " + cpcodeFilename : "");
                plot.setTitle(title);
                plot.setVisible(false);
                double[] actualAccel = gm.getActualAccelArray();
                if (outputMessage) {
                    System.out.println(title);
                    System.out.println("ActualAccel: " + Arrays.toString(actualAccel));
                    System.out.println("MaxAndMinPrimeCode: " + Arrays.toString(getMaxAndMinPrimeCode(actualAccel)));
                    GSDFGradientModel.PatternAndScore pas = gm.getPatternAndScore();
                    System.out.println("smooth score: " + pas.score);
                    double deltaaSum = gm.getTotalDeltaa();
                    if (deltaaSum != -1) {
                        System.out.println("Total delta a : " + deltaaSum);
                    }
                    System.out.println("");
                }
                SmoothMeasureInternalFrame iframe = new SmoothMeasureInternalFrame(plot);
                desktopPane.add(iframe);
                iframe.setVisible(true);
            }
        }.start();
    }

    private boolean outputMessage = true;

    /**
     * ��o��s�p�H�Τs����
     * @param accelArray double[]
     * @return int[]
     */
    private static int[] getMaxAndMinPrimeCode(double[] accelArray) {
        int[] indexs = PeakFinder.getMaxAndMinIndex(accelArray, true);
        return indexs;
    }

    public void jButton_MeasureW_actionPerformed(ActionEvent e) {
        measure(RGBBase.Channel.W, (targetChannel != null) ? targetChannel.equals(RGBBase.Channel.W) : true);
    }

    public void jButton_Calibrate_actionPerformed(ActionEvent e) {
        mm.calibrate();
    }

    public void setMeter(Meter meter) {
        this.meter = meter;
    }

    public void jButton_MeasureR_actionPerformed(ActionEvent e) {
        measure(RGBBase.Channel.R, (targetChannel != null) ? targetChannel.equals(RGBBase.Channel.R) : false);
    }

    public void jButton_MeasureG_actionPerformed(ActionEvent e) {
        measure(RGBBase.Channel.G, (targetChannel != null) ? targetChannel.equals(RGBBase.Channel.G) : false);
    }

    public void jButton_MeasureB_actionPerformed(ActionEvent e) {
        measure(RGBBase.Channel.B, (targetChannel != null) ? targetChannel.equals(RGBBase.Channel.B) : false);
    }

    private String cpcodeFilename = null;

    public void jButton_CPCode_actionPerformed(ActionEvent e) {
        if (meter instanceof DummyMeter) {
            jFileChooser1.setCurrentDirectory(new File(System.getProperty("user.dir")));
            jFileChooser1.showOpenDialog(this);
            File file = jFileChooser1.getSelectedFile();
            if (file != null) {
                String filename = file.getAbsolutePath();
                cpcodeFilename = file.getName();
                RGB[] rgbArray = null;
                try {
                    rgbArray = RGBArray.loadVVExcel(filename);
                } catch (jxl.read.biff.BiffException ex) {
                    Logger.log.error("", ex);
                } catch (IOException ex) {
                    Logger.log.error("", ex);
                }
                DummyMeter dm = (DummyMeter) meter;
                LCDModel model = dm.getLCDModel();
                DisplayLUT displayLUT = new DisplayLUT(rgbArray);
                model.setDisplayLUT(displayLUT);
            }
        }
    }
}
