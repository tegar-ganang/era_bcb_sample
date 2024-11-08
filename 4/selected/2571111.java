package gawky.jasper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.lowagie.text.Document;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfCopy;
import com.lowagie.text.pdf.PdfImportedPage;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;
import com.lowagie.text.pdf.PdfWriter;

class PDFFilter implements FilenameFilter {

    public boolean accept(File dir, String name) {
        return (name.endsWith(".pdf"));
    }
}

public class Concat {

    private static final Log log = LogFactory.getLog(Concat.class);

    public static void concatFiles(InputStream[] streamin, OutputStream target) {
        try {
            Document document = null;
            PdfCopy writer = null;
            for (int i = 0; i < streamin.length; i++) {
                PdfReader reader = new PdfReader(streamin[i]);
                reader.consolidateNamedDestinations();
                int numberofpages = reader.getNumberOfPages();
                if (i == 0) {
                    document = new Document(reader.getPageSizeWithRotation(1));
                    writer = new PdfCopy(document, target);
                    document.open();
                }
                PdfImportedPage page;
                for (int x = 1; x <= numberofpages; x++) {
                    page = writer.getImportedPage(reader, x);
                    writer.addPage(page);
                }
                writer.freeReader(reader);
            }
            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void concatFiles(String path, String target) {
        String[] files = new File(path).list(new PDFFilter());
        try {
            Document document = null;
            PdfCopy writer = null;
            for (int i = 0; i < files.length; i++) {
                PdfReader reader = new PdfReader(path + files[i]);
                reader.consolidateNamedDestinations();
                int numberofpages = reader.getNumberOfPages();
                if (log.isDebugEnabled()) log.debug("There are " + numberofpages + " pages in " + files[i]);
                if (i == 0) {
                    document = new Document(reader.getPageSizeWithRotation(1));
                    writer = new PdfCopy(document, new FileOutputStream(target));
                    document.open();
                }
                PdfImportedPage page;
                for (int x = 1; x <= numberofpages; x++) {
                    page = writer.getImportedPage(reader, x);
                    writer.addPage(page);
                    log.debug("Processed page " + x);
                }
                writer.freeReader(reader);
            }
            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Image getPDF(PdfWriter pdfWriter, String filename) throws Exception {
        PdfReader pdfCoverReader = new PdfReader(filename);
        PdfImportedPage pip = pdfWriter.getImportedPage(pdfCoverReader, 1);
        return Image.getInstance(pip);
    }

    public static void addBackground(String infile, String bgfile, String outfile) {
        try {
            PdfReader reader = new PdfReader(infile);
            int n = reader.getNumberOfPages();
            PdfStamper stamp = new PdfStamper(reader, new FileOutputStream(outfile));
            int i = 0;
            PdfContentByte under;
            Image img = getPDF(stamp.getWriter(), bgfile);
            img.setAbsolutePosition(0, 0);
            while (i < n) {
                i++;
                under = stamp.getUnderContent(i);
                under.addImage(img);
            }
            stamp.close();
        } catch (Exception de) {
            de.printStackTrace();
        }
    }

    public static void addBackground(InputStream instream, String bgfile, OutputStream outstream) {
        try {
            PdfReader reader = new PdfReader(instream);
            int n = reader.getNumberOfPages();
            PdfStamper stamp = new PdfStamper(reader, outstream);
            int i = 0;
            PdfContentByte under;
            Image img = getPDF(stamp.getWriter(), bgfile);
            img.setAbsolutePosition(0, 0);
            while (i < n) {
                i++;
                under = stamp.getUnderContent(i);
                under.addImage(img);
            }
            stamp.close();
        } catch (Exception de) {
            de.printStackTrace();
        }
    }
}
