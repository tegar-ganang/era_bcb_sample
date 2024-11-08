package com.patientis.framework.itext;

import java.awt.BorderLayout;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import org.jpedal.PdfDecoder;
import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFPage;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.html.simpleparser.HTMLWorker;
import com.lowagie.text.html.simpleparser.StyleSheet;
import com.lowagie.text.pdf.Barcode;
import com.lowagie.text.pdf.BarcodeDatamatrix;
import com.lowagie.text.pdf.Barcode128;
import com.lowagie.text.pdf.Barcode39;
import com.lowagie.text.pdf.BarcodeCodabar;
import com.lowagie.text.pdf.BarcodeEAN;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfImportedPage;
import com.lowagie.text.pdf.PdfObject;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import com.patientis.client.service.common.BaseService;
import com.patientis.framework.api.services.ClinicalServer;
import com.patientis.framework.controls.ISPanel;
import com.patientis.framework.controls.forms.ISFrame;
import com.patientis.framework.locale.ImageEditUtil;
import com.patientis.framework.locale.ImageUtil;
import com.patientis.framework.locale.SystemUtil;
import com.patientis.framework.utility.FileSystemUtil;
import com.patientis.framework.utility.IHandleFile;
import com.patientis.framework.utility.ProcessUtil;
import com.patientis.model.clinical.FormModel;
import com.patientis.model.common.ByteWrapper;
import com.patientis.model.common.Converter;

/**
 * One line class description
 *
 */
public class PDFUtility {

    /**
	 * Create a pdf file for the specified image
	 * @param file
	 * @param image
	 */
    private static void createPDFFromImage(File pdfFile, File imageFile, int pdfWidth, int pdfHeight) throws Exception {
        BufferedImage img = ImageUtil.loadImage(imageFile);
        Document document = new Document();
        PdfWriter.getInstance(document, new FileOutputStream(pdfFile));
        File tmpFile = null;
        if (pdfWidth == 0 || pdfHeight == 0) {
            tmpFile = imageFile;
            document.setPageSize(new Rectangle((img.getWidth() * 1.05f), (img.getHeight() * 1.05f)));
        } else {
            tmpFile = SystemUtil.getTemporaryFile();
            BufferedImage letterSizeImage = ImageEditUtil.createThumbnail(img, pdfWidth, pdfHeight);
            ImageUtil.savePNG(letterSizeImage, tmpFile);
        }
        document.open();
        Image imgInstance = Image.getInstance(tmpFile.getAbsolutePath());
        document.add(imgInstance);
        document.close();
    }

    /**
	 * 
	 * @param pdfFile
	 * @param imageFile
	 * @throws Exception
	 */
    public static void createLetterSizePDFfromImage(File pdfFile, File imageFile) throws Exception {
        int pdfWidth = 0;
        Converter.convertInteger(PageSize.LETTER.getWidth() * .90);
        int pdfHeight = 0;
        Converter.convertInteger(PageSize.LETTER.getHeight() * .90);
        createPDFFromImage(pdfFile, imageFile, pdfWidth, pdfHeight);
    }

    /**
	* General Images example
	* @param args no arguments needed
	*/
    public static void main(String[] args) throws Exception {
        BarcodeCodabar barcode = new BarcodeCodabar();
        barcode.setCodeType(BarcodeEAN.UPCA);
        String code = "A" + "1233" + "A";
        barcode.setCode(code);
        barcode.setGuardBars(false);
        File file = new File("C:\\temp\\a1.pdf");
        List<PDFText> content = new ArrayList<PDFText>();
        int height = 35;
        content.add(new PDFText("ADIPEX P", 8, 5, 8));
        createPdfFile(file, content, 80, height);
        addBarcode(barcode, code, file, new File("C:\\temp\\b.pdf"), 10, 0, 0, false);
        PDFSimpleViewer viewer = new PDFSimpleViewer(new ISFrame(), new File("C:\\temp\\b.pdf"), false);
        viewer.show();
    }

    /**
	 * 
	 * @param document
	 * @throws Exception
	 */
    public static void generateDataMatrix(Document document, PdfWriter writer, String ndc, String display, int offset) throws Exception {
        BarcodeDatamatrix datamatrixBarCode = new BarcodeDatamatrix();
        datamatrixBarCode.setOptions(BarcodeDatamatrix.DM_AUTO);
        datamatrixBarCode.setHeight(0);
        datamatrixBarCode.setWidth(0);
        datamatrixBarCode.generate(ndc);
        Image image = datamatrixBarCode.createImage();
        PdfContentByte cb = writer.getDirectContent();
        image.setAbsolutePosition(50, document.getPageSize().getHeight() - (50 * offset));
        cb.addImage(image);
    }

    /**
	 * 
	 * @param pdfFile
	 * @param pngFile
	 * @throws Exception
	 */
    public static void createImageFromPDFFailed(File pdfFile, File pngFile) throws Exception {
        RandomAccessFile raf = new RandomAccessFile(pdfFile, "r");
        FileChannel channel = raf.getChannel();
        ByteBuffer buf = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
        PDFFile pdffile = new PDFFile(buf);
        int num = pdffile.getNumPages();
        for (int i = 0; i < num; i++) {
            PDFPage page = pdffile.getPage(i);
            int width = (int) page.getBBox().getWidth();
            int height = (int) page.getBBox().getHeight();
            java.awt.Rectangle rect = new java.awt.Rectangle(0, 0, width, height);
            BufferedImage img = (BufferedImage) page.getImage((int) rect.getWidth(), (int) rect.getHeight(), rect, null, true, true);
            ImageIO.write(img, "png", pngFile);
        }
    }

    /**
	 * 
	 * @param pdfFile
	 * @return
	 * @throws Exception
	 */
    public static ISPanel createPDFPanel(File pdfFile) throws Exception {
        ISPanel panel = new ISPanel();
        PDFSimpleViewer viewer = new PDFSimpleViewer(new ISFrame(), pdfFile, false);
        panel.add(viewer.getContentPanel(), BorderLayout.CENTER);
        return panel;
    }

    /**
	 * 
	 * @param pdfFile
	 * @param imageFile
	 * @throws Exception
	 */
    public static BufferedImage createImageFromPDF(File pdfFile, int page) throws Exception {
        PdfDecoder pdfDecoder = new PdfDecoder();
        pdfDecoder.openPdfFile(pdfFile.getAbsolutePath());
        pdfDecoder.decodePage(page);
        BufferedImage img = pdfDecoder.getPageAsImage(page);
        return img;
    }

    /**
	 * 
	 * @param pdfFile
	 * @param htmlFile
	 * @throws Exception
	 */
    public static void createPdfFromHtml(File pdfFile, File htmlFile) throws Exception {
        Document document = new Document();
        StyleSheet st = new StyleSheet();
        st.loadTagStyle("body", "leading", "16.0");
        PdfWriter.getInstance(document, new FileOutputStream(pdfFile));
        document.open();
        ArrayList p = HTMLWorker.parseToList(new FileReader(htmlFile), st);
        for (int i = 0; i < p.size(); i++) {
            document.add((Element) p.get(i));
        }
        document.close();
    }

    /**
	 * 
	 * @param targetFile
	 * @throws Exception
	 */
    public static void createPdfFile(File targetFile, List<PDFText> content, float width, float height) throws Exception {
        Document document = new Document(new Rectangle(width, height));
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(targetFile));
        document.open();
        PdfContentByte cb = writer.getDirectContent();
        PdfTemplate tp1 = cb.createTemplate(width, height);
        for (PDFText text : content) {
            PDFText.addText(tp1, text);
        }
        cb.addTemplate(tp1, 0, 0);
        document.close();
    }

    /**
	 * 
	 * @param targetFile
	 * @throws Exception
	 */
    public static void appendPdfFile(File templateFile, File targetFile, List<PDFText> content, float width, float height) throws Exception {
        PdfReader reader = new PdfReader(templateFile.getAbsolutePath());
        PdfStamper stamper = new PdfStamper(reader, new FileOutputStream(targetFile));
        PdfContentByte cb = stamper.getOverContent(1);
        PdfTemplate tp1 = cb.createTemplate(width, height);
        for (PDFText text : content) {
            PDFText.addText(tp1, text);
        }
        cb.addTemplate(tp1, 0, 0);
        stamper.close();
    }

    /**
	 * 
	 * @throws Exception
	 */
    public static void createBarcode(File targetFile, Barcode barcode, boolean landscape, int xpos, int ypos) throws Exception {
        Document document = new Document();
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(targetFile));
        document.open();
        PdfContentByte cb = writer.getDirectContent();
        Image image = barcode.createImageWithBarcode(cb, null, null);
        if (landscape) {
            image.setRotationDegrees(90);
            ypos += (int) barcode.getBarcodeSize().getWidth();
        } else {
            ypos += (int) barcode.getBarHeight();
        }
        image.setAbsolutePosition(xpos, document.getPageSize().getHeight() - 10 - ypos);
        cb.addImage(image);
        document.close();
    }

    /**
	 * 
	 * @param barcode
	 * @param sourcePdfFile
	 * @param targetPdfFile
	 * @throws Exception
	 */
    public static void addBarcode(Barcode barcode, String code, File sourcePdfFile, File targetPdfFile, int xpos, int ypos, int margin, boolean landscape) throws Exception {
        PdfReader reader = new PdfReader(sourcePdfFile.getAbsolutePath());
        PdfStamper stamper = new PdfStamper(reader, new FileOutputStream(targetPdfFile));
        PdfContentByte cb = stamper.getOverContent(1);
        barcode.setCode(code);
        Image image = barcode.createImageWithBarcode(cb, null, null);
        if (landscape) {
            image.setRotationDegrees(90);
            ypos += (int) barcode.getBarcodeSize().getWidth();
        } else {
            ypos += (int) barcode.getBarHeight();
        }
        image.setAbsolutePosition(xpos, reader.getPageSize(1).getHeight() - margin - ypos);
        cb.addImage(image);
        stamper.close();
    }

    /**
	 * 
	 * @param targetFile
	 * @param pageWidth
	 * @param pageHeight
	 * @param pdfFiles
	 * @return
	 * @throws Exception
	 */
    public static ByteWrapper combinePDFs(File targetFile, int pageWidth, int pageHeight, List<File> pdfFiles) throws Exception {
        Document document = new Document(new Rectangle(pageWidth, pageHeight));
        PdfWriter pdfWriter = PdfWriter.getInstance(document, new FileOutputStream(targetFile));
        document.open();
        PdfContentByte cb = pdfWriter.getDirectContent();
        for (File file : pdfFiles) {
            PdfReader reader = new PdfReader(file.getAbsolutePath());
            for (int p = 1; p < reader.getNumberOfPages() + 1; p++) {
                document.newPage();
                PdfImportedPage page1 = pdfWriter.getImportedPage(reader, p);
                cb.addTemplate(page1, 0, 0);
            }
            reader.close();
        }
        document.close();
        return new ByteWrapper(FileSystemUtil.getBinaryContents(targetFile));
    }
}
