package architecture.common.license.io;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import architecture.common.exception.LicenseException;
import architecture.common.license.License;
import architecture.common.license.LicenseSigner;

/**
 * @author  donghyuck
 */
public class LicenseWriter {

    /**
	 * @uml.property  name="license"
	 * @uml.associationEnd  
	 */
    private final License license;

    /**
	 * @uml.property  name="document"
	 */
    private Document document;

    private static final Log log = LogFactory.getLog(LicenseWriter.class);

    public LicenseWriter(License license, LicenseSigner signer) throws LicenseException {
        this.license = license;
        try {
            document = DocumentHelper.parseText(license.toXML());
            Element root = document.getRootElement();
            signer.sign(license);
            Element sig = root.addElement("signature");
            sig.setText(license.getSignature());
        } catch (Exception e) {
            log.fatal(e.getMessage(), e);
            throw new LicenseException((new StringBuilder()).append("Unable to sign license ").append(e.getMessage()).toString(), e);
        }
    }

    /**
	 * @return
	 * @uml.property  name="license"
	 */
    public License getLicense() {
        return license;
    }

    /**
	 * @return
	 * @uml.property  name="document"
	 */
    public Document getDocument() {
        return document;
    }

    public static String encode(License license, int columns, LicenseSigner signer) throws IOException {
        LicenseWriter lw = new LicenseWriter(license, signer);
        StringWriter writer = new StringWriter();
        lw.write(writer, columns);
        return writer.toString();
    }

    public void write(Writer writer) throws IOException {
        write(writer, 80);
    }

    public void write(Writer writer, int columns) throws IOException {
        String xml = document.asXML();
        String base64 = new String(Base64.encodeBase64(xml.getBytes("utf-8")));
        if (columns > 0) base64 = addLineBreaks(base64, columns);
        StringReader reader = new StringReader(base64);
        char buffer[] = new char[32768];
        int len;
        while ((len = reader.read(buffer)) != -1) writer.write(buffer, 0, len);
    }

    private String addLineBreaks(String s, int cols) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i += cols) {
            b.append(s.substring(i, i + cols < s.length() ? i + cols : s.length()));
            b.append("\n");
        }
        return b.toString();
    }
}
