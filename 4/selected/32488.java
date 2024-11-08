package com.lowagie.toolbox.plugins;

import java.io.File;
import java.io.FileOutputStream;
import javax.swing.JInternalFrame;
import com.lowagie.text.Document;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfImportedPage;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.toolbox.AbstractTool;
import com.lowagie.toolbox.arguments.AbstractArgument;
import com.lowagie.toolbox.arguments.FileArgument;
import com.lowagie.toolbox.arguments.filters.PdfFilter;

/**
 * This tool lets you generate a PDF that shows N pages on 1.
 * @since 2.1.1 (imported from itexttoolbox project)
 */
public class Divide extends AbstractTool {

    static {
        addVersion("$Id: Divide.java 3271 2008-04-18 20:39:42Z xlv $");
    }

    /**
	 * Constructs an Divide object.
	 */
    public Divide() {
        menuoptions = MENU_EXECUTE | MENU_EXECUTE_SHOW;
        arguments.add(new FileArgument(this, "srcfile", "The file you want to divide", false, new PdfFilter()));
        arguments.add(new FileArgument(this, "destfile", "The resulting PDF", true, new PdfFilter()));
    }

    /**
	 * @see com.lowagie.toolbox.AbstractTool#createFrame()
	 */
    protected void createFrame() {
        internalFrame = new JInternalFrame("Divide", true, false, true);
        internalFrame.setSize(300, 80);
        internalFrame.setJMenuBar(getMenubar());
        System.out.println("=== Divide OPENED ===");
    }

    /**
	 * @see com.lowagie.toolbox.AbstractTool#execute()
	 */
    public void execute() {
        try {
            if (getValue("srcfile") == null) {
                throw new InstantiationException("You need to choose a sourcefile");
            }
            File src = (File) getValue("srcfile");
            if (getValue("destfile") == null) {
                throw new InstantiationException("You need to choose a destination file");
            }
            File dest = (File) getValue("destfile");
            PdfReader reader = new PdfReader(src.getAbsolutePath());
            int total = reader.getNumberOfPages();
            System.out.println("There are " + total + " pages in the original file.");
            Rectangle pageSize = reader.getPageSize(1);
            Rectangle newSize = new Rectangle(pageSize.getWidth() / 2, pageSize.getHeight());
            Document document = new Document(newSize, 0, 0, 0, 0);
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(dest));
            document.open();
            PdfContentByte cb = writer.getDirectContent();
            PdfImportedPage page;
            float offsetX, offsetY;
            int p;
            for (int i = 0; i < total; i++) {
                p = i + 1;
                pageSize = reader.getPageSize(p);
                newSize = new Rectangle(pageSize.getWidth() / 2, pageSize.getHeight());
                document.newPage();
                offsetX = 0;
                offsetY = 0;
                page = writer.getImportedPage(reader, p);
                cb.addTemplate(page, 1, 0, 0, 1, offsetX, offsetY);
                document.newPage();
                offsetX = -newSize.getWidth();
                offsetY = 0;
                page = writer.getImportedPage(reader, p);
                cb.addTemplate(page, 1, 0, 0, 1, offsetX, offsetY);
            }
            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @see com.lowagie.toolbox.AbstractTool#valueHasChanged(com.lowagie.toolbox.arguments.AbstractArgument)
     * @param arg StringArgument
     */
    public void valueHasChanged(AbstractArgument arg) {
        if (internalFrame == null) {
            return;
        }
    }

    /**
     * Generates a divided version of an NUp version of an existing PDF file.
     *
     * @param args String[]
     */
    public static void main(String[] args) {
        Divide tool = new Divide();
        if (args.length < 2) {
            System.err.println(tool.getUsage());
        }
        tool.setMainArguments(args);
        tool.execute();
    }

    /**
     *
     * @see com.lowagie.toolbox.AbstractTool#getDestPathPDF()
     * @throws InstantiationException
     * @return File
     */
    protected File getDestPathPDF() throws InstantiationException {
        return (File) getValue("destfile");
    }
}
