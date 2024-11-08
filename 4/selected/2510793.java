package be.bzbit.framework.pdf.render;

import java.awt.print.Book;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import be.bzbit.framework.pdf.exception.PdfRenderingException;
import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFPage;

public class PdfFilePrinter {

    private final File pdfFile;

    public PdfFilePrinter(final File pdfFile) {
        this.pdfFile = pdfFile;
    }

    public void print() {
        try {
            RandomAccessFile raf = new RandomAccessFile(pdfFile, "r");
            FileChannel channel = raf.getChannel();
            ByteBuffer buf = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
            PDFFile pdffile = new PDFFile(buf);
            NonScalingPdfPrintPage pages = new NonScalingPdfPrintPage(pdffile);
            PrinterJob printerJob = PrinterJob.getPrinterJob();
            PageFormat pageFormat = PrinterJob.getPrinterJob().defaultPage();
            Paper paper = new Paper();
            PDFPage page = pdffile.getPage(0);
            paper.setSize(page.getWidth(), page.getHeight());
            paper.setImageableArea(0, 0, page.getWidth(), page.getHeight());
            pageFormat.setPaper(paper);
            printerJob.setJobName(pdfFile.getName());
            Book book = new Book();
            book.append(pages, pageFormat, 1);
            printerJob.setPageable(book);
            printerJob.print();
        } catch (Exception e) {
            throw new PdfRenderingException(e);
        }
    }
}
