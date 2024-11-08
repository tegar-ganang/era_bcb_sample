package com.todo.core;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Iterator;
import java.util.List;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.ExceptionConverter;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfGState;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import com.todo.objects.ToDoDate;
import com.todo.objects.ToDoItem;
import com.todo.utils.Constants;
import com.todo.utils.Utils;

public class ReportGenerator extends PdfPageEventHelper {

    public static final Font font_body = new Font(Font.HELVETICA, 8, Font.NORMAL);

    public static final Font font_header = new Font(Font.HELVETICA, 8, Font.BOLD);

    public PdfPTable table;

    public PdfGState gstate;

    public PdfTemplate tpl;

    public BaseFont helv;

    /**
	 * Private constructor -- so this class is never created.
	 * ============================================================================
	 */
    private ReportGenerator() {
    }

    /**
	 * First prompts the user with a Save File dialog, then creates 
	 * a .pdf report from the list of ToDoItems.
	 * ============================================================================ 
	 */
    public static void createReport(Shell shell, List items, int[] columnPositions) {
        Document doc = new Document(PageSize.A4);
        try {
            File file = getFile(shell);
            if (file == null) {
                return;
            }
            PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(file));
            PdfPTable table = new PdfPTable(7);
            writer.setPageEvent(new ReportGenerator());
            table.getDefaultCell().setVerticalAlignment(Element.ALIGN_MIDDLE);
            doc.open();
            doc.add(new Phrase("ToDo - " + (new ToDoDate()).getDateTimeStr(), font_header));
            setupTable(table, columnPositions);
            Iterator it = items.iterator();
            ToDoItem item = null;
            for (int i = 0; it.hasNext(); i++) {
                item = (ToDoItem) it.next();
                writeRow(i, item, table, columnPositions);
            }
            doc.add(table);
        } catch (Exception e) {
            System.out.println("Error generating report:");
            e.printStackTrace();
            Utils.showMessageBox(shell, "Error", "Error generating report:\n\n" + Utils.getStackTraceStr(e), SWT.ICON_ERROR | SWT.OK);
        } finally {
            doc.close();
        }
    }

    /**
	 * Sets the PdfPTable's columns, widths, and headers based on the 
	 * user's column positions.
	 * ============================================================================ 
	 */
    private static void setupTable(PdfPTable table, int[] columnPositions) throws Exception {
        int[] widths = new int[7];
        for (int i = 0; i < columnPositions.length; i++) {
            switch(columnPositions[i]) {
                case 0:
                case 1:
                    widths[i] = 3;
                    break;
                case 2:
                case 3:
                case 4:
                    widths[i] = 10;
                    break;
                case 5:
                    widths[i] = 14;
                    break;
                case 6:
                    widths[i] = 40;
                    break;
                default:
                    break;
            }
        }
        table.setWidths(widths);
        table.setWidthPercentage(100);
        table.getDefaultCell().setPadding(3);
        table.getDefaultCell().setBorderWidth(1);
        table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);
        table.getDefaultCell().setGrayFill(.6f);
        for (int i = 0; i < columnPositions.length; i++) {
            switch(columnPositions[i]) {
                case 0:
                case 1:
                    table.addCell(new Phrase(" ", font_header));
                    break;
                case 2:
                    table.addCell(new Phrase("Posted", font_header));
                    break;
                case 3:
                    table.addCell(new Phrase("Due", font_header));
                    break;
                case 4:
                    table.addCell(new Phrase("Completed", font_header));
                    break;
                case 5:
                    table.addCell(new Phrase("Category", font_header));
                    break;
                case 6:
                    table.addCell(new Phrase("Description", font_header));
                    break;
                default:
                    break;
            }
        }
        table.setHeaderRows(1);
    }

    /**
	 * Writes a single row to the specified table containing the data
	 * from the supplied ToDoItem.
	 * ============================================================================ 
	 */
    private static void writeRow(int rowNo, ToDoItem item, PdfPTable table, int[] columnPositions) {
        if (rowNo % 2 == 1) {
            table.getDefaultCell().setGrayFill(0.85f);
        } else {
            table.getDefaultCell().setGrayFill(0.0f);
        }
        for (int i = 0; i < columnPositions.length; i++) {
            writeCell(rowNo, columnPositions[i], item, table);
        }
    }

    /**
	 * Writes a single cell of the ToDoItem depending on the cellNo.
	 * ============================================================================ 
	 */
    private static void writeCell(int rowNo, int cellNo, ToDoItem item, PdfPTable table) {
        if (cellNo == 0) {
            table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);
            if (item.isCompleted()) {
                table.addCell(new Phrase("X", font_body));
            } else {
                table.addCell(new Phrase("", font_body));
            }
        } else if (cellNo == 1) {
            if (item.getPriority() == Constants.PRIORITY_NORMAL) {
                table.getDefaultCell().setBackgroundColor(Color.blue);
            } else {
                table.getDefaultCell().setBackgroundColor(Color.red);
            }
            table.addCell("");
            table.getDefaultCell().setBackgroundColor(null);
        } else if (cellNo == 2) {
            table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(new Phrase(item.getPostedDateStr(), font_body));
        } else if (cellNo == 3) {
            table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(new Phrase(item.getDueDateStr(), font_body));
        } else if (cellNo == 4) {
            table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(new Phrase(item.getCompletedDateStr(), font_body));
        } else if (cellNo == 5) {
            table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_LEFT);
            table.addCell(new Phrase(item.getCategory(), font_body));
        } else if (cellNo == 6) {
            table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_LEFT);
            table.addCell(new Phrase(item.getDescription(), font_body));
        }
    }

    /**
	 * Prompts the user for a .pdf file to save to, and returns it.
	 * Returns null if the user selects 'Cancel'.
	 * ============================================================================ 
	 */
    private static File getFile(Shell shell) {
        FileDialog saveDialog = new FileDialog(shell, SWT.SAVE);
        saveDialog.setFilterExtensions(new String[] { "*.pdf;", "*.*" });
        saveDialog.setFilterNames(new String[] { "Adobe PDF Files (*.pdf)", "All Files " });
        if (saveDialog.open() == null) {
            return null;
        }
        String fileName = saveDialog.getFileName();
        if (!Utils.endsWith(fileName, ".pdf")) {
            fileName += ".pdf";
        }
        File file = new File(saveDialog.getFilterPath(), fileName);
        if (file.exists()) {
            if (Utils.showMessageBox(shell, "ToDo", "The file " + fileName + " already exists.\nWould you like to overwrite it?", SWT.ICON_WARNING | SWT.YES | SWT.NO) == SWT.NO) {
                return null;
            }
        }
        return file;
    }

    public void onOpenDocument(PdfWriter writer, Document document) {
        try {
            tpl = writer.getDirectContent().createTemplate(100, 100);
            tpl.setBoundingBox(new Rectangle(-20, -20, 100, 100));
            helv = BaseFont.createFont("Helvetica", BaseFont.WINANSI, false);
        } catch (Exception e) {
            throw new ExceptionConverter(e);
        }
    }

    public void onEndPage(PdfWriter writer, Document document) {
        PdfContentByte cb = writer.getDirectContent();
        cb.saveState();
        String text = "Page " + writer.getPageNumber() + " of ";
        float textSize = helv.getWidthPoint(text, 7);
        float textBase = document.bottom() - 20;
        float adjust = helv.getWidthPoint("0", 7);
        cb.beginText();
        cb.setFontAndSize(helv, 7);
        cb.setTextMatrix(document.right() - textSize - adjust, textBase);
        cb.showText(text);
        cb.endText();
        cb.addTemplate(tpl, document.right() - adjust, textBase);
        cb.saveState();
    }

    public void onCloseDocument(PdfWriter writer, Document document) {
        tpl.beginText();
        tpl.setFontAndSize(helv, 7);
        tpl.setTextMatrix(0, 0);
        tpl.showText(String.valueOf((writer.getPageNumber() - 1)));
        tpl.endText();
    }
}
