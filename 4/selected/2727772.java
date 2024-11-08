package org.lindenb.tinytools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import org.lindenb.io.PreferredDirectory;
import org.lindenb.lang.ThrowablePane;
import org.lindenb.sw.vocabulary.SVG;
import org.lindenb.sw.vocabulary.XLINK;
import org.lindenb.swing.DrawingArea;
import org.lindenb.swing.SwingUtils;
import org.lindenb.util.Cast;
import org.lindenb.util.Compilation;
import org.lindenb.xml.XMLUtilities;

public class MaskMaker extends JFrame implements MouseListener, MouseMotionListener {

    private static final long serialVersionUID = 1L;

    private static final double DMAX = 100.0;

    private static final double DMIN = 20.0;

    private BufferedImage image;

    private JPanel drawingArea;

    private float zoom = 1.0f;

    private Figure selFigure = null;

    private int selIndex1 = -1;

    private int selIndex2 = -1;

    private Point mousePrev = null;

    private List<Figure> figures = new ArrayList<Figure>();

    private URL sourceURL;

    /**
	 * 
	 * Figure
	 *
	 */
    private static class Figure {

        private List<Point2D.Double> points = new ArrayList<Point2D.Double>();

        Figure(Point2D.Double... array) {
            for (Point2D.Double p : array) {
                this.points.add(p);
            }
        }

        public Shape getShape() {
            GeneralPath shape = new GeneralPath();
            shape.moveTo(getX(0), getY(0));
            for (int i = 1; i < size(); ++i) {
                shape.lineTo(getX(i), getY(i));
            }
            shape.closePath();
            return shape;
        }

        public Point2D at(int index) {
            return this.points.get(index);
        }

        public int size() {
            return this.points.size();
        }

        public double getX(int index) {
            return at(index).getX();
        }

        public double getY(int index) {
            return at(index).getY();
        }

        public int findIndexAt(double x, double y, double distMax) {
            int best = -1;
            double bestL = Double.MAX_VALUE;
            for (int i = 0; i < size(); ++i) {
                double d = at(i).distance(x, y);
                if (d > distMax) continue;
                if (best == -1 || d < bestL) {
                    best = i;
                    bestL = d;
                }
            }
            return best;
        }

        public boolean findSegment(int x, int y, double distMax, int array[]) {
            array[0] = -1;
            array[1] = -1;
            int i = findIndexAt(x, y, distMax);
            if (i == -1) return false;
            int prev = (i == 0 ? size() - 1 : i - 1);
            Line2D.Double prevLine = new Line2D.Double(at(i), at(prev));
            int next = (i + 1 < size() ? i + 1 : 0);
            Line2D.Double nextLine = new Line2D.Double(at(i), at(next));
            array[0] = i;
            array[1] = (nextLine.ptLineDistSq(x, y) < prevLine.ptLineDistSq(x, y) ? next : prev);
            return true;
        }

        public void insertAt(int index, double x, double y) {
            this.points.add(index, new Point2D.Double(x, y));
        }

        public void setLocationAt(int index, double x, double y) {
            at(index).setLocation(new Point2D.Double(x, y));
        }

        public void removePointAt(int index) {
            this.points.remove(index);
        }
    }

    private void setZoom(float zoom) {
        this.zoom = zoom;
        Dimension d = new Dimension((int) ((float) this.image.getWidth() * this.zoom), (int) ((float) this.image.getHeight() * this.zoom));
        this.drawingArea.setSize(d);
        this.drawingArea.setPreferredSize(d);
        this.drawingArea.repaint();
    }

    private void paintDrawingArea(Graphics2D g) {
        Dimension d = new Dimension((int) ((float) this.image.getWidth() * this.zoom), (int) ((float) this.image.getHeight() * this.zoom));
        g.drawImage(this.image, 0, 0, d.width, d.height, 0, 0, this.image.getWidth(), this.image.getHeight(), this.drawingArea);
        for (int i = this.figures.size() - 1; i >= 0; --i) {
            Figure f = this.figures.get(i);
            g.setColor(f == this.selFigure ? Color.RED : Color.GREEN);
            g.draw(f.getShape());
            for (int j = 0; j < f.size(); ++j) g.drawString("" + j, (int) f.getX(j), (int) f.getY(j));
        }
        if (this.selFigure != null && this.mousePrev != null && selIndex1 != -1 && selIndex2 != -1) {
            g.setColor(Color.RED);
            g.draw(new Line2D.Double(this.selFigure.at(selIndex1), this.mousePrev));
            g.draw(new Line2D.Double(this.selFigure.at(selIndex2), this.mousePrev));
        } else if (this.selFigure != null && selIndex1 != -1) {
            g.setColor(Color.RED);
            g.drawOval(mousePrev.x - 5, mousePrev.y - 5, 10, 10);
        }
    }

    @Override
    public void mouseClicked(MouseEvent event) {
    }

    @Override
    public void mouseExited(MouseEvent event) {
        this.mousePrev = null;
        this.selIndex1 = -1;
        this.selIndex2 = -1;
        this.drawingArea.repaint();
    }

    @Override
    public void mouseEntered(MouseEvent event) {
        if (this.selFigure != null) {
            this.mousePrev = null;
            int indexes[] = new int[2];
            if (this.selFigure.findSegment(event.getX(), event.getY(), DMAX, indexes)) {
                this.selIndex1 = indexes[0];
                this.selIndex2 = indexes[1];
                this.drawingArea.repaint();
            }
        }
        this.mousePrev = new Point(event.getX(), event.getY());
    }

    @Override
    public void mousePressed(MouseEvent event) {
        if (this.selFigure != null) {
            int indexes[] = new int[2];
            if (this.selFigure.findSegment(event.getX(), event.getY(), DMAX, indexes)) {
                double d1 = this.selFigure.at(indexes[0]).distance(event.getX(), event.getY());
                double d2 = this.selFigure.at(indexes[1]).distance(event.getX(), event.getY());
                if (d1 < d2 && d1 < DMIN) {
                    this.selIndex1 = indexes[0];
                    this.selIndex2 = -1;
                } else if (d2 < DMIN) {
                    this.selIndex1 = indexes[1];
                    this.selIndex2 = -1;
                } else {
                    this.selIndex1 = indexes[0];
                    this.selIndex2 = indexes[1];
                    this.selFigure.insertAt(Math.max(this.selIndex1, this.selIndex2), event.getX(), event.getY());
                    this.selIndex1 = Math.max(this.selIndex1, this.selIndex2);
                    this.selIndex2 = -1;
                }
            }
            drawingArea.repaint();
        }
    }

    @Override
    public void mouseDragged(MouseEvent event) {
        if (this.selFigure != null && this.selIndex1 != -1 && this.selIndex2 == -1) {
            this.selFigure.setLocationAt(this.selIndex1, event.getX(), event.getY());
            drawingArea.repaint();
        }
        this.mousePrev = new Point(event.getX(), event.getY());
    }

    @Override
    public void mouseReleased(MouseEvent event) {
    }

    @Override
    public void mouseMoved(MouseEvent event) {
        if (this.selFigure != null) {
            int indexes[] = new int[2];
            if (this.selFigure.findSegment(event.getX(), event.getY(), DMAX, indexes)) {
                double d1 = this.selFigure.at(indexes[0]).distance(event.getX(), event.getY());
                double d2 = this.selFigure.at(indexes[1]).distance(event.getX(), event.getY());
                if (d1 < d2 && d1 < DMIN) {
                    this.selIndex1 = indexes[0];
                    this.selIndex2 = -1;
                } else if (d2 < DMIN) {
                    this.selIndex1 = indexes[1];
                    this.selIndex2 = -1;
                } else {
                    this.selIndex1 = indexes[0];
                    this.selIndex2 = indexes[1];
                }
            }
        }
        this.drawingArea.repaint();
        this.mousePrev = new Point(event.getX(), event.getY());
    }

    /**
	 * MaskCreatorDialog
	 */
    private MaskMaker(BufferedImage image, URL url) {
        super("MaskCreator");
        this.image = image;
        this.sourceURL = url;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        JPanel contentPane = new JPanel(new BorderLayout());
        setContentPane(contentPane);
        JToolBar toolbar = new JToolBar();
        toolbar.add(new JButton(new AbstractAction("[+]") {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent ae) {
                setZoom(zoom * 1.2f);
            }
        }));
        toolbar.add(new JButton(new AbstractAction("[-]") {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent ae) {
                setZoom(zoom * 0.9f);
            }
        }));
        toolbar.add(new JButton(new AbstractAction("[!]") {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent ae) {
                Rectangle r = drawingArea.getVisibleRect();
                double cx = r.getCenterX();
                double cy = r.getCenterY();
                Figure f = new Figure(new Point2D.Double(cx - 20, cy - 20), new Point2D.Double(cx + 20, cy - 20), new Point2D.Double(cx + 20, cy + 20), new Point2D.Double(cx - 20, cy + 20));
                selFigure = f;
                figures.add(f);
                drawingArea.repaint();
            }
        }));
        contentPane.add(toolbar, BorderLayout.NORTH);
        setContentPane(contentPane);
        this.drawingArea = new DrawingArea() {

            private static final long serialVersionUID = 1L;

            @Override
            public void paintDrawingArea(Graphics2D g) {
                MaskMaker.this.paintDrawingArea(Graphics2D.class.cast(g));
            }
        };
        this.drawingArea.setOpaque(true);
        this.drawingArea.addMouseListener(this);
        this.drawingArea.addMouseMotionListener(this);
        this.drawingArea.setBackground(Color.BLACK);
        Dimension d = new Dimension(this.image.getWidth(), this.image.getHeight());
        this.drawingArea.setPreferredSize(d);
        this.drawingArea.setSize(d);
        contentPane.add(new JScrollPane(this.drawingArea));
        JMenuBar bar = new JMenuBar();
        JMenu menu = new JMenu("File");
        bar.add(menu);
        menu.add(new AbstractAction("Save as SVG") {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    File f = showSaveDialog();
                    if (f == null) return;
                    saveToSVG(f);
                } catch (Exception err) {
                    ThrowablePane.show(MaskMaker.this, err);
                }
            }
        });
        menu.add(new AbstractAction("Save as PNG") {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    File f = showSaveDialog();
                    if (f == null) return;
                    saveToPNG(f);
                } catch (Exception err) {
                    ThrowablePane.show(MaskMaker.this, err);
                }
            }
        });
        setJMenuBar(bar);
    }

    private File showSaveDialog() {
        JFileChooser fileChooser = new JFileChooser(PreferredDirectory.getPreferredDirectory());
        if (fileChooser.showSaveDialog(MaskMaker.this) != JFileChooser.APPROVE_OPTION) return null;
        File f = fileChooser.getSelectedFile();
        if (f == null || (f.exists() && JOptionPane.showConfirmDialog(MaskMaker.this, f.toString() + " already exists. Overwrite ?", "Warning", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION)) {
            return null;
        }
        PreferredDirectory.setPreferredDirectory(f);
        return f;
    }

    private void saveToPNG(File f) throws IOException {
        for (Figure fig : this.figures) {
            Shape shape = fig.getShape();
            Rectangle rect = fig.getShape().getBounds();
            AffineTransform tr = AffineTransform.getTranslateInstance(-rect.x, -rect.y);
            shape = tr.createTransformedShape(shape);
            BufferedImage dest = new BufferedImage(rect.width, rect.height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = dest.createGraphics();
            g.setClip(shape);
            g.drawImage(this.image, -rect.x, -rect.y, null);
            g.dispose();
            ImageIO.write(dest, "png", f);
        }
    }

    private void saveToSVG(File f) throws IOException {
        double width = 1;
        double height = 1;
        for (Figure fig : this.figures) {
            for (int i = 0; i < fig.size(); ++i) {
                width = Math.max(width, fig.getX(i) + 1);
                height = Math.max(height, fig.getY(i) + 1);
            }
        }
        PrintWriter out = new PrintWriter(f);
        out.println("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
        out.println("<svg xmlns=\"" + SVG.NS + "\" " + " xmlns:xlink=\"" + XLINK.NS + "\" " + " version='1.0' width='" + width + "' height='" + height + "'>");
        out.println("<defs>");
        out.print("<image id='img' " + " width='" + this.image.getWidth() + "' " + " height='" + this.image.getHeight() + "' " + " xlink:href='" + XMLUtilities.escape(sourceURL.toString()) + "' " + " />");
        out.println("</defs>");
        int clipIndex = 0;
        for (Figure fig : this.figures) {
            out.println("<g clip-rule='nonzero'>");
            out.println("<clipPath id='clip" + (++clipIndex) + "'>");
            out.print("<path d='");
            for (int i = 0; i < fig.size(); ++i) {
                out.print((i == 0 ? "M " : "L ") + fig.getX(i) + " " + fig.getY(i) + " ");
            }
            out.print("z'/>");
            out.println("</clipPath>");
            out.print("<use x='0' y='0'  xlink:href='#img' clip-path='url(#clip" + clipIndex + ")'  clip-rule='evenodd' />");
            out.println("</g>");
            for (int i = 0; i < fig.size(); ++i) {
                width = Math.max(width, fig.getX(i) + 1);
                height = Math.max(height, fig.getY(i) + 1);
            }
        }
        out.println("</svg>");
        out.flush();
        out.close();
    }

    /**
	 * main
	 */
    public static void main(String[] args) {
        try {
            JDialog.setDefaultLookAndFeelDecorated(true);
            JFrame.setDefaultLookAndFeelDecorated(true);
            int optind = 0;
            while (optind < args.length) {
                if (args[optind].equals("-h")) {
                    System.err.println(Compilation.getLabel());
                } else if (args[optind].equals("--")) {
                    optind++;
                    break;
                } else if (args[optind].startsWith("-")) {
                    System.err.println("Unknown option " + args[optind]);
                } else {
                    break;
                }
                ++optind;
            }
            if (optind == args.length) {
                System.err.println("URL missing");
                return;
            }
            while (optind < args.length) {
                String url = args[optind++];
                BufferedImage img;
                MaskMaker app = null;
                if (Cast.URL.isA(url)) {
                    img = ImageIO.read(Cast.URL.cast(url));
                    app = new MaskMaker(img, Cast.URL.cast(url));
                } else {
                    File f = new File(url);
                    img = ImageIO.read(f);
                    app = new MaskMaker(img, f.toURI().toURL());
                }
                SwingUtils.center(app, 100);
                SwingUtils.show(app);
            }
        } catch (Throwable err) {
            err.printStackTrace();
        }
    }
}
