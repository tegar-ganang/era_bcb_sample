package vademecum.visualizer.heightsurface.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import vademecum.visualizer.heightsurface.VSurface3D;
import vademecum.visualizer.heightsurface.jRenderer3D.JRenderer3D;

public class SurfaceDialog extends JDialog {

    JPanel settingsPanel2;

    private JSlider sliderMin;

    private JSlider sliderMax;

    private JSlider sliderScale;

    private JSlider sliderZRatio;

    JRenderer3D jRenderer3D;

    VSurface3D plot;

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        new SurfaceDialog(null).setVisible(true);
    }

    public SurfaceDialog(VSurface3D surf) {
        super((JFrame) surf.getFigurePanel().getGraphicalViewer());
        setTitle("Surface Settings");
        this.plot = surf;
        if (plot != null) this.jRenderer3D = plot.jRenderer3D;
        add(createSettingsPanelRight());
        setSize(120, 450);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    }

    private JPanel createSettingsPanelRight() {
        Dimension sliderDim2 = new Dimension(70, 400);
        JPanel sliderPanel2 = createSliderPanel2();
        sliderPanel2.setPreferredSize(sliderDim2);
        settingsPanel2 = new JPanel();
        settingsPanel2.setLayout(new BorderLayout());
        settingsPanel2.add(sliderPanel2, BorderLayout.CENTER);
        return settingsPanel2;
    }

    private JPanel createSliderPanel2() {
        sliderScale = createSliderVertical("Scale: 1.0", 25, 300, 100);
        sliderZRatio = createSliderVertical("ZRatio: 1.0", 25, 200, 100);
        sliderMin = createSliderVertical("Min:0", 0, 255, 0);
        sliderMax = createSliderVertical("Max:255", 0, 255, 255);
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(4, 1));
        panel.add(sliderScale);
        panel.add(sliderZRatio);
        panel.add(sliderMax);
        panel.add(sliderMin);
        return panel;
    }

    private JSlider createSliderVertical(String borderTitle, int min, int max, int value) {
        Border empty = BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder());
        Font sliderFont = new Font("Sans", Font.PLAIN, 11);
        JSlider slider = new JSlider(JSlider.VERTICAL, min, max, value);
        slider.setBorder(new TitledBorder(empty, borderTitle, TitledBorder.CENTER, TitledBorder.BELOW_TOP, sliderFont));
        slider.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent event) {
                sliderChange((JSlider) event.getSource());
            }
        });
        return slider;
    }

    /**
 * Updates illumination, smoothing and scaling. Renders and updates the image.
 *
 */
    private void sliderChange(JSlider slider) {
        if (slider == sliderScale) {
            double scaleSlider = sliderScale.getValue() / 100.;
            String str = "Scale: " + (int) (scaleSlider * 100) / 100.;
            setSliderTitle(sliderScale, Color.black, str);
            double scale = plot.scaleInit * plot.scaleWindow * scaleSlider;
            jRenderer3D.setTransformScale(scale);
        } else if (slider == sliderMin) {
            int max = sliderMax.getValue();
            int min = sliderMin.getValue();
            if (min >= max) {
                max = Math.min(256, min + 1);
                sliderMax.setValue(max);
                sliderMax.repaint();
            }
            String str = "Min:" + min;
            setSliderTitle(sliderMin, Color.black, str);
            str = "Max:" + max;
            setSliderTitle(sliderMax, Color.black, str);
            jRenderer3D.setSurfacePlotMinMax(min, max);
        } else if (slider == sliderMax) {
            int max = sliderMax.getValue();
            int min = sliderMin.getValue();
            if (max <= min) {
                min = Math.max(-1, max - 1);
                sliderMin.setValue(min);
                sliderMin.repaint();
            }
            String str = "Min:" + min;
            setSliderTitle(sliderMin, Color.black, str);
            str = "Max:" + max;
            setSliderTitle(sliderMax, Color.black, str);
            jRenderer3D.setSurfacePlotMinMax(min, max);
        } else if (slider == sliderZRatio) {
            double zAspectRatio = sliderZRatio.getValue() / 100.;
            String str = "ZRatio:" + zAspectRatio;
            setSliderTitle(sliderZRatio, Color.black, str);
            zAspectRatio *= plot.zRatioInit;
            jRenderer3D.setTransformZAspectRatio(zAspectRatio);
        }
        plot.renderAndUpdateDisplay();
    }

    private void setSliderTitle(JSlider slider, Color color, String str) {
        Border empty = BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder());
        Font sliderFont = new Font("Sans", Font.PLAIN, 11);
        slider.setBorder(new TitledBorder(empty, str, TitledBorder.CENTER, TitledBorder.BELOW_TOP, sliderFont));
    }
}
