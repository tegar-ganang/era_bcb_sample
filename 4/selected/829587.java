package org.ofbiz.webapp.view;

import java.io.*;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.MimeConstants;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.FOPException;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import javax.xml.transform.*;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.Result;

/**
 * FopRenderer
 */
public class FopRenderer {

    public static final String module = FopRenderer.class.getName();

    /**
     * Renders a PDF document from a FO script that is passed in and returns the content as a ByteArrayOutputStream
     * @param writer    a Writer stream that supplies the FO text to be rendered
     * @return  ByteArrayOutputStream containing the binary representation of a PDF document
     * @throws GeneralException
     */
    public static ByteArrayOutputStream render(Writer writer) throws GeneralException {
        FopFactory fopFactory = ApacheFopFactory.instance();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TransformerFactory transFactory = TransformerFactory.newInstance();
        try {
            Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, out);
            Transformer transformer = transFactory.newTransformer();
            Reader reader = new StringReader(writer.toString());
            Source src = new StreamSource(reader);
            Result res = new SAXResult(fop.getDefaultHandler());
            try {
                transformer.transform(src, res);
                fopFactory.getImageFactory().clearCaches();
                return out;
            } catch (TransformerException e) {
                Debug.logError("FOP transform failed:" + e, module);
                throw new GeneralException("Unable to transform FO to PDF", e);
            }
        } catch (TransformerConfigurationException e) {
            Debug.logError("FOP TransformerConfiguration Exception " + e, module);
            throw new GeneralException("Transformer Configuration Error", e);
        } catch (FOPException e) {
            Debug.logError("FOP Exception " + e, module);
            throw new GeneralException("FOP Error", e);
        }
    }
}
