package fr.harlie.merge_pdf.itext;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfAction;
import com.itextpdf.text.pdf.PdfImportedPage;
import com.itextpdf.text.pdf.PdfOutline;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;
import fr.harlie.merge_pdf.itext.PdfMergerPageEvent.IPdfMergerPageEvent;
import fr.harlie.merge_pdf.tree.CommonOptions;
import fr.harlie.merge_pdf.tree.INode;
import fr.harlie.merge_pdf.tree.NumberType;
import fr.harlie.merge_pdf.tree.PdfFileNode;

public class PdfMergingTool implements IPdfMergerPageEvent {

    private HashMap<Integer, PdfTemplate> templates = new HashMap<Integer, PdfTemplate>();

    private int i;

    private BaseFont helv = null;

    private List<IPdfMergerListener> listeners;

    public PdfTemplate getTemplate() {
        return templates.get(i);
    }

    public PdfMergingTool() throws DocumentException, IOException {
        helv = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
        listeners = new ArrayList<IPdfMergerListener>();
    }

    public void addIPdfMergerListener(IPdfMergerListener l) {
        listeners.remove(l);
        listeners.add(l);
    }

    public void removeIPdfMergerListener(IPdfMergerListener l) {
        listeners.remove(l);
    }

    public void generatePdf(INode rootNode, File file) throws FileNotFoundException, IOException, DocumentException {
        Document document = new Document(PageSize.A4, 50.0f, 50.0f, 50.0f, 50.0f);
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(file));
        PdfMergerPageEvent event = new PdfMergerPageEvent(this);
        writer.setPageEvent(event);
        document.open();
        int children = getChildNumber(rootNode);
        fireInitListeners(children + 2);
        buildIndex(rootNode.getChildren(), writer, document);
        appendFiles(rootNode.getChildren(), writer, document);
        fireNextSteps(children + 2, "Ecriture des fichiers");
        document.close();
        writer.close();
        fireJobDoneListeners();
    }

    protected void fireInitListeners(int children) {
        for (IPdfMergerListener li : listeners) {
            li.init(children);
        }
    }

    protected void fireJobDoneListeners() {
        for (IPdfMergerListener li : listeners) {
            li.jobDone();
        }
    }

    protected int getChildNumber(INode rootNode) {
        int children = rootNode.getChildCount();
        Stack<INode> stack = new Stack<INode>();
        List<INode> l = new ArrayList<INode>(Arrays.asList(rootNode.getChildren()));
        Collections.reverse(l);
        stack.addAll(l);
        while (!stack.empty()) {
            INode n = stack.pop();
            children += n.getChildCount();
            l = new ArrayList<INode>(Arrays.asList(n.getChildren()));
            Collections.reverse(l);
            stack.addAll(l);
        }
        return children;
    }

    private void buildIndex(INode[] files, PdfWriter writer, Document document) {
        try {
            templates.clear();
            PdfPTable table = new PdfPTable(2);
            table.setWidths(new float[] { 0.85f, 0.15f });
            fireNextSteps(1, "G�n�ration du sommaire");
            i = 0;
            Stack<INode> stack = new Stack<INode>();
            List<INode> l = new ArrayList<INode>(Arrays.asList(files));
            Collections.reverse(l);
            stack.addAll(l);
            while (!stack.empty()) {
                INode n = stack.pop();
                if (n.getLevel() == 1) {
                    PdfPCell c = new PdfPCell(new Phrase(""));
                    c.disableBorderSide(Rectangle.LEFT);
                    c.disableBorderSide(Rectangle.RIGHT);
                    c.disableBorderSide(Rectangle.TOP);
                    c.disableBorderSide(Rectangle.BOTTOM);
                    c.setColspan(2);
                    c.setPaddingLeft((n.getLevel() - 1) * 18);
                    table.addCell(c);
                }
                CommonOptions opt = CommonOptions.getCommonOptions(n);
                PdfPCell c = new PdfPCell(new Phrase(opt.getPrefix() + " " + getFullIndex(n) + " " + n.getTitle(), new Font(helv, 10, Font.NORMAL)));
                c.disableBorderSide(Rectangle.LEFT);
                c.disableBorderSide(Rectangle.RIGHT);
                c.disableBorderSide(Rectangle.TOP);
                c.disableBorderSide(Rectangle.BOTTOM);
                c.setPaddingLeft((n.getLevel() - 1) * 18);
                table.addCell(c);
                PdfTemplate template = writer.getDirectContent().createTemplate(50, 4);
                template.setBoundingBox(new Rectangle(0, 0, 50, 10));
                templates.put(i, template);
                Image image = Image.getInstance(template);
                c = new PdfPCell(image);
                c.disableBorderSide(Rectangle.LEFT);
                c.disableBorderSide(Rectangle.RIGHT);
                c.disableBorderSide(Rectangle.TOP);
                c.disableBorderSide(Rectangle.BOTTOM);
                c.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(c);
                i++;
                l = new ArrayList<INode>(Arrays.asList(n.getChildren()));
                Collections.reverse(l);
                stack.addAll(l);
            }
            Chunk chunk = new Chunk("Sommaire", new Font(helv, 25, Font.BOLD));
            chunk.setLocalDestination("Sommaire");
            Paragraph p = new Paragraph();
            p.setAlignment(Element.ALIGN_CENTER);
            p.add(chunk);
            document.add(p);
            new PdfOutline(writer.getDirectContent().getRootOutline(), PdfAction.gotoLocalPage("Sommaire", false), "Sommaire");
            document.add(table);
            document.newPage();
        } catch (DocumentException e) {
            e.printStackTrace();
        }
    }

    protected void fireNextSteps(int i, String title) {
        for (IPdfMergerListener li : listeners) {
            li.nextSteps(i, title);
        }
    }

    private void appendFiles(INode[] files, PdfWriter writer, Document document) {
        i = 0;
        Stack<INode> stack = new Stack<INode>();
        List<INode> l = new ArrayList<INode>(Arrays.asList(files));
        Collections.reverse(l);
        stack.addAll(l);
        Stack<PdfOutline> outlines = new Stack<PdfOutline>();
        for (int i = 0; i < files.length; i++) {
            outlines.add(writer.getDirectContent().getRootOutline());
        }
        while (!stack.empty()) {
            INode n = stack.pop();
            PdfOutline outline = outlines.pop();
            try {
                CommonOptions opt = CommonOptions.getCommonOptions(n);
                if (opt.isFirstPageGenerated()) {
                    outline = displayTitle(writer, document, outline, n, opt);
                }
                if (n instanceof PdfFileNode) {
                    fireNextSteps(i + 2, "Inclusion de " + ((PdfFileNode) n).getFileName());
                    includeDocument(writer, document, (PdfFileNode) n, opt);
                } else {
                    fireNextSteps(i + 2, "G�n�ration de " + n.getTitle());
                }
            } catch (DocumentException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            i++;
            l = new ArrayList<INode>(Arrays.asList(n.getChildren()));
            Collections.reverse(l);
            stack.addAll(l);
            for (int i = 0; i < n.getChildren().length; i++) {
                outlines.add(outline);
            }
            PdfTemplate t = getTemplate();
            if (t != null) {
                t.beginText();
                t.setFontAndSize(helv, 10);
                t.showTextAligned(Element.ALIGN_LEFT, "" + writer.getPageNumber(), 0, 0, 0);
                t.endText();
            }
        }
    }

    private PdfOutline displayTitle(PdfWriter writer, Document document, PdfOutline outline, INode n, CommonOptions opt) throws DocumentException, IOException {
        String index = (opt.getPrefix() + " " + getFullIndex(n)).trim();
        Chunk chunk = new Chunk(index, new Font(helv, 25, Font.BOLD));
        chunk.setLocalDestination(getUniqueId(n));
        Paragraph p = new Paragraph();
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingBefore(75f);
        p.add(chunk);
        document.add(new Paragraph(" \n \n \n", new Font(helv, 25, Font.BOLD)));
        document.add(p);
        p = new Paragraph(n.getTitle(), new Font(helv, 25, Font.BOLD));
        p.setAlignment(Element.ALIGN_CENTER);
        document.add(p);
        document.newPage();
        return new PdfOutline(outline, PdfAction.gotoLocalPage(getUniqueId(n), false), index + " " + n.getTitle());
    }

    private void includeDocument(PdfWriter writer, Document document, PdfFileNode n, CommonOptions opt) throws DocumentException, IOException {
        PdfReader reader = new PdfReader(new FileInputStream(n.getFileName()));
        for (int j = 1; j <= reader.getNumberOfPages(); j++) {
            PdfImportedPage page = writer.getImportedPage(reader, j);
            Image image = Image.getInstance(page);
            image.setRotationDegrees(n.getRotation().getAngle());
            image.scalePercent(95f);
            document.add(image);
        }
        reader.close();
        document.newPage();
    }

    public String getFullIndex(INode n) {
        CommonOptions opt = CommonOptions.getCommonOptions(n);
        if (opt.getNumberType() == NumberType.NONE) return "";
        String index = "" + opt.getNumberType().convert(n.getChildIndex() + 1);
        if (opt.isNumberTypeHierarchy()) {
            n = (INode) n.getParent();
            while (n != null && n.getParent() != null) {
                opt = CommonOptions.getCommonOptions(n);
                index = "" + opt.getNumberType().convert(n.getChildIndex() + 1) + "." + index;
                n = (INode) n.getParent();
            }
        }
        return index;
    }

    public String getUniqueId(INode n) {
        String index = "id:" + (n.getChildIndex() + 1);
        n = (INode) n.getParent();
        while (n != null && n.getParent() != null) {
            index = "" + (n.getChildIndex() + 1) + "." + index;
            n = (INode) n.getParent();
        }
        return index;
    }
}
