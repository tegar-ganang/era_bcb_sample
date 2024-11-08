package com.lowagie.tools.plugins;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JInternalFrame;
import com.lowagie.text.Document;
import com.lowagie.text.pdf.PRAcroForm;
import com.lowagie.text.pdf.PdfCopy;
import com.lowagie.text.pdf.PdfImportedPage;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.SimpleBookmark;
import com.lowagie.tools.arguments.FileArgument;
import com.lowagie.tools.arguments.PdfFilter;
import com.lowagie.tools.arguments.ToolArgument;

/**
 * Concatenates two PDF files
 */
public class Concat extends AbstractTool {

    /**
	 * Constructs a Tiff2Pdf object.
	 */
    public Concat() {
        menuoptions = MENU_EXECUTE | MENU_EXECUTE_SHOW;
        arguments.add(new FileArgument(this, "srcfile1", "The first PDF file", false, new PdfFilter()));
        arguments.add(new FileArgument(this, "srcfile2", "The second PDF file", false, new PdfFilter()));
        arguments.add(new FileArgument(this, "destfile", "The file to which the concatenated PDF has to be written", true, new PdfFilter()));
    }

    /**
	 * @see com.lowagie.tools.plugins.AbstractTool#createFrame()
	 */
    protected void createFrame() {
        internalFrame = new JInternalFrame("Concatenate 2 PDF files", true, true, true);
        internalFrame.setSize(550, 250);
        internalFrame.setJMenuBar(getMenubar());
        internalFrame.getContentPane().add(getConsole(40, 30));
    }

    /**
	 * @see com.lowagie.tools.plugins.AbstractTool#execute()
	 */
    public void execute() {
        try {
            String[] files = new String[2];
            if (getValue("srcfile1") == null) throw new InstantiationException("You need to choose a first sourcefile");
            files[0] = ((File) getValue("srcfile1")).getAbsolutePath();
            if (getValue("srcfile2") == null) throw new InstantiationException("You need to choose a second sourcefile");
            files[1] = ((File) getValue("srcfile2")).getAbsolutePath();
            if (getValue("destfile") == null) throw new InstantiationException("You need to choose a destination file");
            File pdf_file = (File) getValue("destfile");
            int pageOffset = 0;
            ArrayList master = new ArrayList();
            Document document = null;
            PdfCopy writer = null;
            for (int i = 0; i < 2; i++) {
                PdfReader reader = new PdfReader(files[i]);
                reader.consolidateNamedDestinations();
                int n = reader.getNumberOfPages();
                List bookmarks = SimpleBookmark.getBookmark(reader);
                if (bookmarks != null) {
                    if (pageOffset != 0) SimpleBookmark.shiftPageNumbers(bookmarks, pageOffset, null);
                    master.addAll(bookmarks);
                }
                pageOffset += n;
                System.out.println("There are " + n + " pages in " + files[i]);
                if (i == 0) {
                    document = new Document(reader.getPageSizeWithRotation(1));
                    writer = new PdfCopy(document, new FileOutputStream(pdf_file));
                    document.open();
                }
                PdfImportedPage page;
                for (int p = 0; p < n; ) {
                    ++p;
                    page = writer.getImportedPage(reader, p);
                    writer.addPage(page);
                    System.out.println("Processed page " + p);
                }
                PRAcroForm form = reader.getAcroForm();
                if (form != null) writer.copyAcroForm(reader);
            }
            if (master.size() > 0) writer.setOutlines(master);
            document.close();
        } catch (Exception e) {
            e.printStackTrace();
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
     * Concatenates two PDF files.
     * @param args
     */
    public static void main(String[] args) {
        Concat tool = new Concat();
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
