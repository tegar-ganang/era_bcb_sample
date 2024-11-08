package com.metanology.mde.ui.pimEditor.diagrams;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.PrinterGraphics;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.print.PrintGraphicalViewerOperation;
import org.eclipse.swt.printing.Printer;
import org.eclipse.swt.widgets.Display;

/**
 * Print the diagram on the multiple pages
 * @author wwang
 * @since 3.0
 */
public class DiagramPrintOperation extends PrintGraphicalViewerOperation {

    private double scale;

    /**
	 * Constructor for DiagramPrintOperation
	 *  
	 * @param p
	 * @param g
	 */
    public DiagramPrintOperation(Printer p, GraphicalViewer g) {
        super(p, g);
        scale = getPrinter().getDPI().x / Display.getDefault().getDPI().x;
    }

    /**
	 * @see org.eclipse.draw2d.PrintOperation#printPages()
	 */
    protected void printPages() {
        IFigure figure = getPrintSource();
        boolean nextPage = true;
        Point offset = figure.getBounds().getLocation();
        int x = (int) (getPrintSource().getBounds().getSize().width / (getPrintRegion().width / scale));
        int y = (int) (getPrintSource().getBounds().getSize().height / (getPrintRegion().height / scale));
        PrinterGraphics g = getFreshPrinterGraphics();
        g.setForegroundColor(figure.getForegroundColor());
        g.setBackgroundColor(figure.getBackgroundColor());
        g.setFont(figure.getFont());
        for (int i = 0; i < getNumofPages(); i++) {
            g.pushState();
            getPrinter().startPage();
            g.scale(scale);
            g.translate(-offset.x, -offset.y);
            g.clipRect(figure.getBounds());
            figure.paint(g);
            getPrinter().endPage();
            g.restoreState();
            if (x > 0) {
                x--;
                offset.x += (int) (getPrintRegion().width / scale);
            } else if (y > 0) {
                y--;
                offset.y += (int) (getPrintRegion().height / scale);
                offset.x = figure.getBounds().getLocation().x;
                x = (int) (getPrintSource().getBounds().getSize().width / (getPrintRegion().width / scale));
            }
        }
    }

    /**
	 * @see org.eclipse.draw2d.PrintOperation#run(String)
	 */
    public void run(String jobName) {
        super.run(jobName);
    }

    /**
	 * Return the number of pages to be printed
	 * @return int
	 */
    private int getNumofPages() {
        double x = getPrintSource().getBounds().getSize().width / (getPrintRegion().width / scale);
        double y = getPrintSource().getBounds().getSize().height / (getPrintRegion().height / scale);
        return ((x > (int) x) ? (int) x + 1 : (int) x) * ((y > (int) y) ? (int) y + 1 : (int) y);
    }
}
