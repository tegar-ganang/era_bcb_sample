package fr.itris.glips.svgeditor.io.managers.export.handler;

import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;
import org.apache.batik.bridge.*;
import org.apache.batik.gvt.*;
import org.w3c.dom.Document;
import com.lowagie.text.pdf.*;
import fr.itris.glips.svgeditor.*;
import fr.itris.glips.svgeditor.io.managers.export.*;
import fr.itris.glips.svgeditor.io.managers.export.handler.dialog.*;
import fr.itris.glips.svgeditor.io.managers.export.image.*;
import fr.itris.glips.svgeditor.io.managers.export.monitor.*;

/**
 * the class used to export images in a jpg format
 * @author ITRIS, Jordi SUC
 */
public class PDFExport extends Export {

    /**
     * the size of the pages
     */
    protected com.lowagie.text.Rectangle pageSize = null;

    /**
     * whether the orientation is "portrait"
     */
    protected boolean isPortrait = true;

    /**
     * the margins
     */
    protected Insets margins = new Insets(0, 0, 0, 0);

    /**
     * information on the pdf file
     */
    protected String title = "", author = "", subject = "", keywords = "", creator = "";

    /**
	 * the constructor of the class
	 * @param fileExport the object manager the export
	 */
    protected PDFExport(FileExport fileExport) {
        super(fileExport);
        if (Editor.getParent() instanceof Frame) {
            exportDialog = new PDFExportDialog((Frame) Editor.getParent());
        } else {
            exportDialog = new PDFExportDialog((JDialog) Editor.getParent());
        }
    }

    @Override
    public void export(JComponent relativeComponent, Document document, File destFile) {
        monitor = new ExportMonitor(Editor.getParent(), 0, 100, FileExport.prefixLabels[3]);
        monitor.setRelativeComponent(relativeComponent);
        PDFExportDialog pdfExportDialog = (PDFExportDialog) exportDialog;
        int res = exportDialog.showExportDialog(document);
        if (res == ExportDialog.OK_ACTION) {
            pageSize = pdfExportDialog.getPageSize();
            isPortrait = pdfExportDialog.isPortrait();
            margins = pdfExportDialog.getMargins();
            title = pdfExportDialog.getTitle();
            author = pdfExportDialog.getAuthor();
            subject = pdfExportDialog.getSubject();
            keywords = pdfExportDialog.getKeywords();
            creator = pdfExportDialog.getCreator();
            writeImage(document, destFile);
        }
    }

    @Override
    protected void writeImage(final BufferedImage image, final File destFile) {
    }

    /**
	 * writes the image to export
	 * @param document a document
	 * @param destFile the destination file
	 */
    protected void writeImage(final org.w3c.dom.Document document, final File destFile) {
        Thread thread = new Thread() {

            private boolean pdfDocOpen = true;

            @Override
            public void run() {
                monitor.start();
                monitor.setProgress(0);
                UserAgentAdapter userAgent = null;
                GVTBuilder builder = null;
                BridgeContext ctx = null;
                RootGraphicsNode gvtRoot = null;
                try {
                    userAgent = new UserAgentAdapter();
                    ctx = new BridgeContext(userAgent, null, new DocumentLoader(userAgent));
                    builder = new GVTBuilder();
                    ctx.setDynamicState(BridgeContext.STATIC);
                    monitor.setProgress(5);
                    if (monitor.isCancelled()) {
                        monitor.stop();
                        ctx.dispose();
                        return;
                    }
                    GraphicsNode gvt = builder.build(ctx, document);
                    monitor.setProgress(40);
                    if (monitor.isCancelled()) {
                        monitor.stop();
                        ctx.dispose();
                        return;
                    }
                    if (gvt != null) {
                        gvtRoot = gvt.getRoot();
                        final BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(destFile));
                        com.lowagie.text.Rectangle rect = pageSize;
                        rect = isPortrait ? rect : rect.rotate();
                        final com.lowagie.text.Document pdfDoc = new com.lowagie.text.Document(rect, margins.left, margins.right, margins.top, margins.bottom) {

                            @Override
                            public void open() {
                                super.open();
                                synchronized (PDFExport.this) {
                                    pdfDocOpen = true;
                                }
                            }

                            @Override
                            public void close() {
                                try {
                                    super.close();
                                } catch (Exception ex) {
                                }
                                synchronized (PDFExport.this) {
                                    pdfDocOpen = false;
                                }
                            }
                        };
                        pdfDoc.addTitle(title);
                        pdfDoc.addAuthor(author);
                        pdfDoc.addSubject(subject);
                        pdfDoc.addKeywords(keywords);
                        pdfDoc.addCreator(creator);
                        PdfWriter writer = PdfWriter.getInstance(pdfDoc, out);
                        pdfDoc.open();
                        PdfContentByte cb = writer.getDirectContent();
                        final PdfTemplate tp = cb.createTemplate(rect.width(), rect.height());
                        tp.setWidth(rect.width());
                        tp.setHeight(rect.height());
                        Graphics2D g2 = tp.createGraphics(rect.width(), rect.height(), new DefaultFontMapper());
                        SVGDocumentImageCreator.handleGraphicsConfiguration(g2);
                        Thread writerStateThread = new Thread() {

                            @Override
                            public void run() {
                                while (pdfDocOpen) {
                                    if (monitor.isCancelled()) {
                                        synchronized (PDFExport.this) {
                                            pdfDocOpen = false;
                                        }
                                        try {
                                            tp.reset();
                                            pdfDoc.close();
                                        } catch (Exception ex) {
                                            ex.printStackTrace();
                                        }
                                        monitor.stop();
                                        break;
                                    }
                                    try {
                                        sleep(500);
                                    } catch (Exception ex) {
                                    }
                                }
                            }
                        };
                        writerStateThread.start();
                        monitor.setIndeterminate(true);
                        gvtRoot.paint(g2);
                        g2.dispose();
                        cb.addTemplate(tp, 0, 0);
                        pdfDoc.close();
                        out.flush();
                        out.close();
                        monitor.stop();
                    }
                    ctx.dispose();
                } catch (Exception ex) {
                    handleExportFailure();
                    if (ctx != null) {
                        ctx.dispose();
                    }
                }
            }
        };
        thread.start();
    }
}
