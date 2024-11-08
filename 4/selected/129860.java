package test;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.print.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import javax.print.attribute.*;
import javax.print.attribute.standard.*;
import javax.swing.*;
import com.sun.pdfview.*;

public class PDFPrinter extends JFrame implements Printable {

    PDFFile pdfFile;

    public PDFPrinter(String title) {
        super(title);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        JMenu menu = new JMenu("File");
        JMenuItem mi = new JMenuItem("Print...");
        mi.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                try {
                    doPrint();
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(PDFPrinter.this, e.getMessage());
                }
            }
        });
        menu.add(mi);
        menu.addSeparator();
        mi = new JMenuItem("Exit");
        mi.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                dispose();
            }
        });
        menu.add(mi);
        JMenuBar mb = new JMenuBar();
        mb.add(menu);
        setJMenuBar(mb);
        setSize(200, 200);
        setVisible(true);
    }

    void doPrint() throws Exception {
        JFileChooser fcOpen = new JFileChooser();
        fcOpen.setCurrentDirectory(new File(System.getProperty("user.dir")));
        fcOpen.setAcceptAllFileFilterUsed(false);
        fcOpen.setFileFilter(new javax.swing.filechooser.FileFilter() {

            public boolean accept(File f) {
                if (f.isDirectory()) {
                    return true;
                }
                String s = f.getName();
                int i = s.lastIndexOf('.');
                if (i > 0 && i < s.length() - 1) {
                    String ext;
                    ext = s.substring(i + 1).toLowerCase();
                    if (ext.equals("pdf")) {
                        return true;
                    }
                }
                return false;
            }

            public String getDescription() {
                return "Accepts .pdf files";
            }
        });
        if (fcOpen.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        RandomAccessFile raf = new RandomAccessFile(fcOpen.getSelectedFile(), "r");
        FileChannel fc = raf.getChannel();
        ByteBuffer buf = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
        pdfFile = new PDFFile(buf);
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintable(this);
        try {
            HashPrintRequestAttributeSet attset;
            attset = new HashPrintRequestAttributeSet();
            attset.add(new PageRanges(1, pdfFile.getNumPages()));
            if (job.printDialog(attset)) {
                job.print(attset);
            }
        } catch (PrinterException pe) {
            JOptionPane.showMessageDialog(this, pe.getMessage());
        }
    }

    public int print(Graphics g, PageFormat format, int index) throws PrinterException {
        int pagenum = index + 1;
        if (pagenum < 1 || pagenum > pdfFile.getNumPages()) {
            return NO_SUCH_PAGE;
        }
        Graphics2D g2d = (Graphics2D) g;
        AffineTransform at = g2d.getTransform();
        PDFPage pdfPage = pdfFile.getPage(pagenum);
        Dimension dim;
        dim = pdfPage.getUnstretchedSize((int) format.getImageableWidth(), (int) format.getImageableHeight(), pdfPage.getBBox());
        Rectangle bounds = new Rectangle((int) format.getImageableX(), (int) format.getImageableY(), dim.width, dim.height);
        PDFRenderer rend = new PDFRenderer(pdfPage, (Graphics2D) g, bounds, null, null);
        try {
            pdfPage.waitForFinish();
            rend.run();
        } catch (InterruptedException ie) {
            JOptionPane.showMessageDialog(this, ie.getMessage());
        }
        g2d.setTransform(at);
        g2d.draw(new Rectangle2D.Double(format.getImageableX(), format.getImageableY(), format.getImageableWidth(), format.getImageableHeight()));
        return PAGE_EXISTS;
    }

    public static void main(String[] args) {
        Runnable r = new Runnable() {

            public void run() {
                new PDFPrinter("PDF Printer");
            }
        };
        EventQueue.invokeLater(r);
    }
}
