package jat.oppoc.ui;

import jat.oppoc.Main;
import jat.oppoc.sunjai.Contrast;
import jat.oppoc.sunjai.ImageDisplay;
import jat.oppoc.sunjai.Panner;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.media.jai.PlanarImage;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

/**
 * @author Jacek A. Teska
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class UIDialogNavigator extends JDialog {

    private static final int WIDTH = 240;

    Panner panner = null;

    Contrast contrast = null;

    JTabbedPane tabPanel = null;

    PlanarImage image = null;

    ImageDisplay imageDisplay = null;

    JComponent imageComponent = null;

    public UIDialogNavigator(Frame owner, String title, JComponent com, PlanarImage pi, ImageDisplay id) {
        super(owner, title, false);
        setTitle(title);
        if (com != null && pi != null && id != null) setParams(com, pi, id);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        addWindowListener(new UIDialogAdapter((Main) owner));
    }

    public synchronized void setParams(JComponent com, PlanarImage pi, ImageDisplay id) {
        int index = -1;
        int bandSettings = 0;
        image = pi;
        imageDisplay = id;
        imageComponent = com;
        setVisible(false);
        if (tabPanel != null) {
            index = tabPanel.getSelectedIndex();
            UIStatistic temp = (UIStatistic) tabPanel.getComponentAt(0);
            bandSettings = temp.getBandSettings();
        }
        getContentPane().removeAll();
        UIStatistic statistic = new UIStatistic(pi, bandSettings);
        panner = new Panner(com, pi, WIDTH - 90);
        panner.setBackground(Color.red);
        panner.setBorder(new EtchedBorder());
        contrast = new Contrast(pi, WIDTH - 90, WIDTH - 90);
        contrast.setDisplay(id);
        contrast.setSliderLimits(0.0, 256.0, 0.0, 2.0 * 256.0);
        contrast.setSliderLocation((WIDTH - 90) / 2, (WIDTH - 90) / 2);
        contrast.setBorder(new LineBorder(Color.white, 1));
        contrast.setBackground(Color.blue);
        contrast.setSliderColor(Color.yellow);
        contrast.setSliderOpaque(true);
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(id.getComponentColor(WIDTH), BorderLayout.NORTH);
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        JPanel p0 = new JPanel();
        p0.setLayout(new BoxLayout(p0, BoxLayout.Y_AXIS));
        p0.setBorder(new TitledBorder(new EtchedBorder(), UITools.getString("navigator.Colors")));
        JPanel p1 = new JPanel();
        p1.add(id.getRColor(UITools.getString("navigator.Red")));
        p0.add(p1);
        p1 = new JPanel();
        p1.add(id.getGColor(UITools.getString("navigator.Green")));
        p0.add(p1);
        p1 = new JPanel();
        p1.add(id.getBColor(UITools.getString("navigator.Blue")));
        p0.add(p1);
        center.add(p0);
        p0 = new JPanel();
        p0.setLayout(new BoxLayout(p0, BoxLayout.Y_AXIS));
        p0.setBorder(new TitledBorder(new EtchedBorder(), UITools.getString("navigator.Pixels")));
        p1 = new JPanel();
        p1.add(id.getXOdometer(UITools.getString("navigator.X")));
        p1.add(id.getYOdometer(UITools.getString("navigator.Y")));
        p0.add(p1);
        p1 = new JPanel();
        p1.add(id.getWidthComponent(UITools.getString("navigator.Width")));
        p1.add(id.getHeightComponent(UITools.getString("navigator.Height")));
        p0.add(p1);
        center.add(p0);
        p0 = new JPanel();
        p0.setLayout(new BoxLayout(p0, BoxLayout.Y_AXIS));
        p0.setBorder(new TitledBorder(new EtchedBorder(), UITools.getString("navigator.Statistic")));
        p0.add(statistic.getChannelPanel(UITools.getString("statistic.ChannelLabel")));
        p0.add(statistic.getMinPanel(UITools.getString("statistic.Min")));
        p0.add(statistic.getMaxPanel(UITools.getString("statistic.Max")));
        p0.add(statistic.getMeanPanel(UITools.getString("statistic.Mean")));
        center.add(p0);
        JScrollPane sp = new JScrollPane(center);
        sp.getVerticalScrollBar().setUnitIncrement(10);
        sp.getHorizontalScrollBar().setUnitIncrement(10);
        panel.add(sp);
        sp = new JScrollPane(panner);
        sp.getVerticalScrollBar().setUnitIncrement(10);
        sp.getHorizontalScrollBar().setUnitIncrement(10);
        tabPanel = new JTabbedPane(JTabbedPane.BOTTOM);
        tabPanel.addTab(UITools.getString("navigator.Statistic"), statistic);
        tabPanel.addTab(UITools.getString("navigator.Panner"), sp);
        tabPanel.addTab(UITools.getString("navigator.Contrast"), contrast);
        tabPanel.addTab(UITools.getString("navigator.Info"), panel);
        if (index != -1) tabPanel.setSelectedIndex(index);
        getContentPane().add(tabPanel);
        pack();
        setSize(WIDTH, WIDTH);
        setLocation(getOwner().getX() + getOwner().getWidth() - getWidth() - 20, getOwner().getY() + 100);
        setVisible(true);
        repaint();
    }
}

class UIDialogAdapter extends WindowAdapter {

    Main oppoc = null;

    public UIDialogAdapter(Main op) {
        oppoc = op;
    }

    public void windowClosed(WindowEvent e) {
        oppoc.setNavigator(false);
        super.windowClosed(e);
    }
}
