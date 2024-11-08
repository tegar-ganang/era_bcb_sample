package core.gui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import core.Stack;
import core.io.SliceReader;
import core.io.VolumeReader;

/**
 * This class represents a scrollable slice-view to a given VolumeReader (Stack)
 * such as the main-view on the tabbedpane.
 * 
 * @author bachewii
 * 
 */
public class ImagePane extends JPanel {

    private VolumeReader reader;

    private int[] tmp;

    Theba control;

    boolean drawFibers = false;

    BufferedImage img;

    JTabbedPane tabPane = new JTabbedPane();

    private int[] colors;

    public ImagePane(Theba f) {
        this.control = f;
        generateColorTable();
        addMouseMotionListener(new MouseMotionListener() {

            public void mouseDragged(MouseEvent e) {
            }

            public void mouseMoved(MouseEvent e) {
                control.setPointerLabel(e.getPoint());
            }
        });
    }

    public ImagePane(Theba forsk, SliceReader reader2) {
        reader = reader2;
        this.control = forsk;
        generateColorTable();
        addMouseMotionListener(new MouseMotionListener() {

            public void mouseDragged(MouseEvent e) {
            }

            public void mouseMoved(MouseEvent e) {
                control.setPointerLabel(e.getPoint());
            }
        });
    }

    /**
	 * Creates and returns a colormodel suitable for identifying fibres
	 */
    public IndexColorModel getIndexedColorModel() {
        byte ff = (byte) 0xff;
        byte[] r = new byte[256];
        byte[] g = new byte[256];
        byte[] b = new byte[256];
        ;
        r[255] = ff;
        g[255] = ff;
        b[255] = ff;
        for (int i = 1; i < 255; i++) {
            b[i] = (byte) (100 + ((i * 4) % 15) * 10);
            g[i] = (byte) (50 + ((i * 4) % 14) * 10);
            r[i] = (byte) (0 + (((i * 5)) % 10) * 20);
        }
        r[7] = ff;
        g[7] = 0;
        b[7] = 0;
        IndexColorModel cm = new IndexColorModel(8, 256, r, g, b);
        return cm;
    }

    public VolumeReader getSliceReader() {
        return reader;
    }

    public Stack getVolume() {
        return new Stack(reader);
    }

    public boolean isDrawFibers() {
        return drawFibers;
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (img != null) {
            g.drawImage(img, 0, 0, this);
        }
    }

    /**
	 * Generate a lookup-table with predefined colors for each region
	 */
    public void generateColorTable() {
        colors = new int[Theba.MAX_FIBERS];
        for (int i = 0; i < colors.length; i++) {
            colors[i] = (((i * 4) % 15) * 10) << 16 | (100 + ((i * 4) % 14) * 10) << 8 | (100 + (((i * 5)) % 10) * 20);
        }
        colors[Theba.INVALID] = 0xff0000;
    }

    /**
	 * Get the lookup-color for a given reg-value
	 * 
	 * @param val
	 */
    public int getColor(short val) {
        if (val < 0) return 0;
        int index = val % colors.length;
        return colors[index];
    }

    public void setData(short[] data, int w, int h) {
        if (control.isMinimized()) {
            return;
        }
        if (tmp == null || tmp.length != w * h) tmp = new int[w * h];
        for (int i = 0; i < tmp.length; i++) {
            if (data[i] <= 255) {
                tmp[i] = data[i] | data[i] << 8 | data[i] << 16;
            } else {
                tmp[i] = getColor(data[i]);
            }
        }
        if (data == null) return;
        short id = control.getTracker().getCurrentId();
        if (control.hideWhite().isSelected()) {
            for (int i = 0; i < tmp.length; i++) {
                if (data[i] == id && id != 0) {
                    tmp[i] = 0xffff00;
                } else if (data[i] <= 255) {
                    tmp[i] = 0xff000000;
                }
            }
        }
        if (img == null || w != img.getWidth() || h != img.getHeight()) {
            img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            setPreferredSize(new Dimension(w, h));
        }
        img.getRaster().setDataElements(0, 0, w, h, tmp);
    }

    public void setDrawFibers(boolean drawFibers) {
        this.drawFibers = drawFibers;
    }

    public void setReader(VolumeReader reader) {
        this.reader = reader;
    }

    public void showSlice(int index) {
        short[] pixels = reader.getSlice(index);
        if (reader != null) setData(pixels, control.width, control.height); else JOptionPane.showMessageDialog(null, "No reader attached to this view");
        repaint();
    }

    public void updateData(int slice) {
        if (reader == null) System.err.println("No reader attached to this pane!"); else {
            setData(reader.getSlice(slice), reader.getWidth(), reader.getHeight());
            repaint();
        }
    }

    public void setInput(SliceReader writer) {
        reader.destroy();
        reader = writer;
    }
}
