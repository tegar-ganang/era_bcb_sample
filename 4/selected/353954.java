package corner.orm.tapestry.pdf;

import java.io.IOException;
import com.lowagie.text.Document;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfImportedPage;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfWriter;

/**
 * 对pdf模板文件进行解析
 * 
 * @author jcai
 * @author <a href=mailto:Ghostbb@bjmaxinfo.com>Ghostbb</a>
 * @version $Revision: 3678 $
 * @since 2.3.7
 */
public class PdfOutputPageEvent extends PdfPageEventHelper {

    /** 模板文件的读取器 * */
    private PdfReader reader = null;

    /** 用来处理pdf的字节内容 * */
    private PdfContentByte canvas;

    public PdfOutputPageEvent(PdfReader reader) {
        this.reader = reader;
    }

    public PdfOutputPageEvent(IPdfPage pdfComponent) {
        try {
            reader = new PdfReader(pdfComponent.getSpecification().getSpecificationLocation().getResourceURL());
        } catch (IOException e) {
            throw new PdfSystemException(e);
        }
    }

    /**
	 * 当打开文档的时候，同时也开始处理
	 * 
	 * @see com.lowagie.text.pdf.PdfPageEventHelper#onOpenDocument(com.lowagie.text.pdf.PdfWriter,
	 *      com.lowagie.text.Document)
	 */
    @Override
    public void onOpenDocument(PdfWriter writer, Document doc) {
        canvas = writer.getDirectContentUnder();
    }

    private void createBurdenText(PdfWriter writer) {
        PdfContentByte content = writer.getDirectContent();
        content.beginText();
        content.endText();
    }

    public void addTemplateData(PdfWriter writer, Document doc, int templatePageNum) {
        PdfImportedPage templateData = writer.getImportedPage(reader, templatePageNum);
        createBurdenText(writer);
        canvas.addTemplate(templateData, 0, 0);
    }
}
