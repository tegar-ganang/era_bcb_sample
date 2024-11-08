package vademecum.visualizer.pmatrix2d.dialogs;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Vector;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import vademecum.data.GridUtils;
import vademecum.data.IDataGrid;
import vademecum.ui.visualizer.vgraphics.image.VImage;
import vademecum.ui.visualizer.widgets.TableSelectorPanel;
import vademecum.visualizer.pmatrix2d.PMatrix2D;

public class TiledDisplay extends JDialog {

    PMatrix2D plot;

    ArrayList<Integer> clusterCols;

    IDataGrid grid;

    Canvas drawPanel;

    BufferedImage img;

    public TiledDisplay(PMatrix2D plot) {
        super((JFrame) plot.getFigurePanel().getGraphicalViewer());
        setTitle("Tiled Display");
        this.plot = plot;
        init();
    }

    private void init() {
        img = plot.getImage();
        drawPanel = new Canvas();
        setContentPane(drawPanel);
        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setSize(640, 400);
    }

    public class Canvas extends JPanel {

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponents(g);
            Graphics2D g2 = (Graphics2D) g;
            int x3rd = this.getWidth() / 3;
            int y3rd = this.getHeight() / 3;
            g2.drawImage(img, 0, 0, x3rd, y3rd, this);
            g2.drawImage(img, x3rd, 0, x3rd, y3rd, this);
            g2.drawImage(img, 2 * x3rd, 0, x3rd, y3rd, this);
            g2.drawImage(img, 0, y3rd, x3rd, y3rd, this);
            g2.drawImage(img, x3rd, y3rd, x3rd, y3rd, this);
            g2.drawImage(img, 2 * x3rd, y3rd, x3rd, y3rd, this);
            g2.drawImage(img, 0, 2 * y3rd, x3rd, y3rd, this);
            g2.drawImage(img, x3rd, 2 * y3rd, x3rd, y3rd, this);
            g2.drawImage(img, 2 * x3rd, 2 * y3rd, x3rd, y3rd, this);
            BasicStroke stroke = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1, new float[] { 1 }, 0);
            g2.setStroke(stroke);
            g2.setColor(Color.GREEN);
            g2.drawRect(x3rd, y3rd, x3rd, y3rd);
        }
    }
}
