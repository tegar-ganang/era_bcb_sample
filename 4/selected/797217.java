package wand.graphicsChooser;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Dimension;
import javax.swing.JFrame;
import wand.patterns.filter.*;
import wand.patterns.*;
import wand.ChannelFrame;

public class FilterChooserFrame extends JFrame {

    private TestPattern test0 = new TestPattern();

    private TestPattern test1 = new TestPattern();

    private TestPattern test2 = new TestPattern();

    private Shutter shutter0 = new Shutter();

    private Shutter shutter1 = new Shutter();

    private Shutter shutter2 = new Shutter();

    private FlowLayout layout = new FlowLayout();

    private PatternInterface p0, p1, p2;

    public void choiceMade(String choice) {
        if (choice.equals("shutter")) {
            p0 = shutter0;
            p1 = shutter1;
            p2 = shutter2;
        } else {
            p0 = test0;
            p1 = test1;
            p2 = test2;
        }
        ChannelFrame.filterPanel.getChannelDisplay().setPatternType(p0);
        ChannelFrame.filterPanel.getOutputPreviewPanel().setPatternType(p1);
        ChannelFrame.filterPanel.getOutputDisplayPanel().setPatternType(p2);
    }

    public FilterChooserFrame() {
        this.setTitle("Filter Chooser");
        this.setPreferredSize(new Dimension(850, 750));
        this.setAlwaysOnTop(true);
        this.setVisible(false);
        setLocationByPlatform(true);
        this.setBackground(Color.yellow);
        setLayout(layout);
        getContentPane().add(new FilterBeatButtons());
        getContentPane().add(new FilterAmbientButtons());
        pack();
        choiceMade("shutter");
    }
}
