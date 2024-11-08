package sk.sigp.tetras.itext;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import org.junit.Test;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.AcroFields;
import com.lowagie.text.pdf.PdfDictionary;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfReader;

public class PdfFormFillServiceTest {

    private PdfFormFillService _service = new PdfFormFillService();

    @Test
    public void fillPdfFields() throws FileNotFoundException, DocumentException, IOException {
        byte[] exampleIn = readExamplePdf();
        EnumMap<PdfParamEnum, String> params = new EnumMap<PdfParamEnum, String>(PdfParamEnum.class);
        params.put(PdfParamEnum.COMPANY_NAME, "Abraka dabra a.s.");
        params.put(PdfParamEnum.COMPANY_ADDRESS, "Čarodejnícka 12, Faxovo 99999");
        params.put(PdfParamEnum.COMPANY_COUNTRY, "Transilvánia");
        SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy");
        params.put(PdfParamEnum.FAX_DATE, fmt.format(new Date()));
        byte[] result = _service.fillPdfForm(params, exampleIn);
        FileOutputStream exampleOut = new FileOutputStream(new File("target/example_filled.pdf"));
        exampleOut.write(result);
        exampleOut.close();
    }

    private byte[] readExamplePdf() throws IOException {
        InputStream stream = this.getClass().getResourceAsStream("/faxDE.pdf");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int count = -1;
        byte[] data = new byte[1024];
        while ((count = stream.read(data)) != -1) out.write(data, 0, count);
        return out.toByteArray();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void showPdfFields() throws IOException, DocumentException {
        PdfReader reader = new PdfReader(readExamplePdf());
        AcroFields form = reader.getAcroFields();
        HashMap<String, Object> fields = form.getFields();
        for (String key : fields.keySet()) {
            System.out.println(key);
            AcroFields.Item item = form.getFieldItem(key);
            PdfDictionary dict;
            PdfName name;
            System.out.println("pages: " + item.page);
            for (Iterator i = item.merged.iterator(); i.hasNext(); ) {
                dict = (PdfDictionary) i.next();
                for (Iterator it = dict.getKeys().iterator(); it.hasNext(); ) {
                    name = (PdfName) it.next();
                    System.out.println(name.toString() + ": " + dict.get(name));
                }
                System.out.println("------------------------------------");
            }
        }
        reader.close();
    }
}
