package com.lowagie.tools.plugins;

import java.io.File;
import java.io.FileOutputStream;
import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import com.lowagie.text.Document;
import com.lowagie.text.PageSize;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfImportedPage;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.tools.arguments.FileArgument;
import com.lowagie.tools.arguments.OptionArgument;
import com.lowagie.tools.arguments.PdfFilter;
import com.lowagie.tools.arguments.ToolArgument;

/**
 * Converts a Tiff file to a PDF file.
 */
public class Handouts extends AbstractTool {

    /**
	 * Constructs a Tiff2Pdf object.
	 */
    public Handouts() {
        arguments.add(new FileArgument(this, "srcfile", "The file you want to convert", false, new PdfFilter()));
        arguments.add(new FileArgument(this, "destfile", "The file to which the converted TIFF has to be written", true, new PdfFilter()));
        OptionArgument oa = new OptionArgument(this, "pages", "The number of pages you want on one handout page");
        oa.addOption("2 pages on 1", "2");
        oa.addOption("3 pages on 1", "3");
        oa.addOption("4 pages on 1", "4");
        oa.addOption("5 pages on 1", "5");
        oa.addOption("6 pages on 1", "6");
        oa.addOption("7 pages on 1", "7");
        oa.addOption("8 pages on 1", "8");
        arguments.add(oa);
    }

    /**
	 * @see com.lowagie.tools.plugins.AbstractTool#createFrame()
	 */
    protected void createFrame() {
        internalFrame = new JInternalFrame("Handouts", true, true, true);
        internalFrame.setSize(300, 250);
        internalFrame.setJMenuBar(getMenubar());
        internalFrame.getContentPane().add(getConsole(20, 10));
    }

    /**
	 * @see com.lowagie.tools.plugins.AbstractTool#execute()
	 */
    public void execute() {
        try {
            if (getValue("srcfile") == null) throw new InstantiationException("You need to choose a sourcefile");
            File src = (File) getValue("srcfile");
            if (getValue("destfile") == null) throw new InstantiationException("You need to choose a destination file");
            File dest = (File) getValue("destfile");
            int pages;
            try {
                pages = Integer.parseInt((String) getValue("pages"));
            } catch (Exception e) {
                pages = 4;
            }
            float x1 = 30f;
            float x2 = 280f;
            float x3 = 320f;
            float x4 = 565f;
            float[] y1 = new float[pages];
            float[] y2 = new float[pages];
            float height = (778f - (20f * (pages - 1))) / pages;
            y1[0] = 812f;
            y2[0] = 812f - height;
            for (int i = 1; i < pages; i++) {
                y1[i] = y2[i - 1] - 20f;
                y2[i] = y1[i] - height;
            }
            PdfReader reader = new PdfReader(src.getAbsolutePath());
            int n = reader.getNumberOfPages();
            System.out.println("There are " + n + " pages in the original file.");
            Document document = new Document(PageSize.A4);
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(dest));
            document.open();
            PdfContentByte cb = writer.getDirectContent();
            PdfImportedPage page;
            int rotation;
            int i = 0;
            int p = 0;
            while (i < n) {
                i++;
                Rectangle rect = reader.getPageSizeWithRotation(i);
                float factorx = (x2 - x1) / rect.width();
                float factory = (y1[p] - y2[p]) / rect.height();
                float factor = (factorx < factory ? factorx : factory);
                float dx = (factorx == factor ? 0f : ((x2 - x1) - rect.width() * factor) / 2f);
                float dy = (factory == factor ? 0f : ((y1[p] - y2[p]) - rect.height() * factor) / 2f);
                page = writer.getImportedPage(reader, i);
                rotation = reader.getPageRotation(i);
                if (rotation == 90 || rotation == 270) {
                    cb.addTemplate(page, 0, -factor, factor, 0, x1 + dx, y2[p] + dy + rect.height() * factor);
                } else {
                    cb.addTemplate(page, factor, 0, 0, factor, x1 + dx, y2[p] + dy);
                }
                cb.setRGBColorStroke(0xC0, 0xC0, 0xC0);
                cb.rectangle(x3 - 5f, y2[p] - 5f, x4 - x3 + 10f, y1[p] - y2[p] + 10f);
                for (float l = y1[p] - 19; l > y2[p]; l -= 16) {
                    cb.moveTo(x3, l);
                    cb.lineTo(x4, l);
                }
                cb.rectangle(x1 + dx, y2[p] + dy, rect.width() * factor, rect.height() * factor);
                cb.stroke();
                System.out.println("Processed page " + i);
                p++;
                if (p == pages) {
                    p = 0;
                    document.newPage();
                }
            }
            document.close();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(internalFrame, e.getMessage(), e.getClass().getName(), JOptionPane.ERROR_MESSAGE);
            System.err.println(e.getMessage());
        }
    }

    /**
	 * @see com.lowagie.tools.plugins.AbstractTool#valueHasChanged(com.lowagie.tools.arguments.ToolArgument)
	 */
    public void valueHasChanged(ToolArgument arg) {
        if (internalFrame == null) {
            return;
        }
    }

    /**
     * Converts a tiff file to PDF.
     * @param args
     */
    public static void main(String[] args) {
        Handouts tool = new Handouts();
        if (args.length < 2) {
            System.err.println(tool.getUsage());
        }
        tool.setArguments(args);
        tool.execute();
    }

    /**
	 * @see com.lowagie.tools.plugins.AbstractTool#getDestPathPDF()
	 */
    protected File getDestPathPDF() throws InstantiationException {
        return (File) getValue("destfile");
    }
}
