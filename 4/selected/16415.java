package lrf.merge;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Hashtable;
import lrf.objects.Book;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfDestination;
import com.lowagie.text.pdf.PdfImportedPage;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfWriter;

public class MergePDFAndTOC {

    public static void wBuild(File dir, String pdfName) throws Exception {
        wBuild(dir.getCanonicalPath(), pdfName);
    }

    public static void wBuild(final String dir, String pdfName) throws Exception {
        Entrada padre = new Entrada(null, dir, new String[] { ".pdf", ".lrf" });
        Document document = new Document();
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(new File(pdfName)));
        writer.setCompressionLevel(5);
        document.addTitle(getLastName(dir));
        document.addAuthor(getLastName(dir));
        document.open();
        document.newPage();
        createToC(document, padre, 0);
        concatenaPDFs(document, writer, padre);
        document.close();
        System.out.println("Fin");
    }

    private static void createToC(Document doc, Entrada ent, int level) throws DocumentException {
        System.out.println("Creando ToC para " + ent.getCanonicalName());
        String Titulo = ent.getCanonicalName();
        Entrada padre = ent.getPadre();
        addPara(doc, Titulo, Paragraph.ALIGN_LEFT, (padre != null ? padre.getCanonicalName() : null), ent.getCanonicalName(), true, level);
        ArrayList<Entrada> al = ent.getHijos();
        for (int i = 0; i < al.size(); i++) {
            Entrada hij = (Entrada) al.get(i);
            String subtitle = hij.getNombrePresentable();
            if (hij.isDir()) createToC(doc, hij, level + 1); else addPara(doc, subtitle, Paragraph.ALIGN_LEFT, hij.getCanonicalName(), null, false, level + 1);
        }
    }

    private static void addPara(Document doc, String txt, int ali, String localGoto, String localDest, boolean subr, int level) throws DocumentException {
        Chunk chunk;
        Paragraph para;
        for (int i = 0; i < level; i++) txt = "\t" + txt;
        chunk = new Chunk(txt);
        chunk.setFont(new Font(1, Math.max(12, 21 - 3 * level)));
        if (subr) chunk.setUnderline(Color.BLACK, 0.00f, 0.075f, 0, -0.2f, PdfContentByte.LINE_CAP_ROUND);
        if (localGoto != null) chunk.setLocalGoto(localGoto);
        if (localDest != null) chunk.setLocalDestination(localDest);
        para = new Paragraph(chunk);
        para.setAlignment(ali);
        para.setSpacingAfter(20);
        doc.add(para);
    }

    private static void concatenaPDFs(Document doc, PdfWriter writer, Entrada ent) throws Exception {
        ArrayList<Entrada> al = ent.getHijos();
        PdfContentByte cb = writer.getDirectContent();
        for (int i = 0; i < al.size(); i++) {
            Entrada hij = (Entrada) al.get(i);
            if (hij.isSelectedResource() == -1) continue;
            InputStream gen = null;
            if (hij.getNombre().toLowerCase().endsWith(".lrf")) {
                Book book = new Book(hij.getFile());
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                book.getPDF(bos, false, new Hashtable<String, String>());
                gen = new ByteArrayInputStream(bos.toByteArray());
            } else {
                gen = new FileInputStream(hij.getFile());
            }
            PdfReader reader = new PdfReader(gen);
            int numPagesToImport = reader.getNumberOfPages();
            for (int j = 1; j <= numPagesToImport; j++) {
                doc.newPage();
                PdfImportedPage page = writer.getImportedPage(reader, j);
                if (j == 1) {
                    cb.localDestination(hij.getCanonicalName(), new PdfDestination(PdfDestination.FIT));
                    System.out.println("\tA�adiendo " + hij.getCanonicalName());
                }
                cb.addTemplate(page, 0, 0);
            }
            reader.close();
        }
        for (int i = 0; i < al.size(); i++) {
            Entrada hij = (Entrada) al.get(i);
            if (hij.isDir()) {
                concatenaPDFs(doc, writer, hij);
                writer.flush();
            }
        }
    }

    private static String getLastName(String dir) {
        int x = dir.lastIndexOf("\\");
        if (x > 0 && x + 1 < dir.length()) dir = dir.substring(x + 1);
        x = dir.lastIndexOf("/");
        if (x > 0 && x + 1 < dir.length()) dir = dir.substring(x + 1);
        return dir;
    }
}
