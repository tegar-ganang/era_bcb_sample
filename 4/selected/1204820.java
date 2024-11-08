package signit.application;

import com.sun.star.beans.PropertyValue;
import com.sun.star.comp.helper.BootstrapException;
import com.sun.star.connection.NoConnectException;
import com.sun.star.document.XDocumentInfo;
import com.sun.star.document.XDocumentInfoSupplier;
import com.sun.star.frame.XComponentLoader;
import com.sun.star.frame.XDesktop;
import com.sun.star.frame.XModel;
import com.sun.star.lang.ArrayIndexOutOfBoundsException;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.text.XTextDocument;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import signit.application.constants.SignItConstants;

/**
 *
 * @author Sachin Sudheendra
 */
public class MetaDataWriter {

    public static void writeIntoMetaData(XDocumentInfo documentInfo, short arg0, String key, String value) throws Exception, BootstrapException {
        documentInfo.setUserFieldName(arg0, key);
        documentInfo.setUserFieldValue(arg0, value);
    }

    public static XDocumentInfo getDocument(XComponentContext xLocalContext) throws NoConnectException, Exception, java.lang.Exception {
        XMultiComponentFactory xmcf = xLocalContext.getServiceManager();
        Object desktop = xmcf.createInstanceWithContext("com.sun.star.frame.Desktop", xLocalContext);
        XDesktop xDesktop = (XDesktop) UnoRuntime.queryInterface(com.sun.star.frame.XDesktop.class, desktop);
        XComponent document = xDesktop.getCurrentComponent();
        XModel xmodel = (XModel) UnoRuntime.queryInterface(XModel.class, document);
        if (xmodel == null || xmodel.getURL().isEmpty()) {
            MyLogger.log("Please save the document before proceeding.");
        } else {
            MyLogger.log(xmodel.getURL());
        }
        XComponentLoader xCompLoader = (XComponentLoader) UnoRuntime.queryInterface(XComponentLoader.class, xDesktop);
        XComponent xComponent = xCompLoader.loadComponentFromURL(xmodel.getURL(), "_default", 0, new PropertyValue[0]);
        XTextDocument xDoc = (XTextDocument) UnoRuntime.queryInterface(XTextDocument.class, xComponent);
        XDocumentInfoSupplier xDocumentInfoSupplier = (XDocumentInfoSupplier) UnoRuntime.queryInterface(XDocumentInfoSupplier.class, xDoc);
        return (XDocumentInfo) xDocumentInfoSupplier.getDocumentInfo();
    }

    public static boolean shouldOverwriteIfPresent(XDocumentInfo xDocumentInfo) {
        try {
            String userFieldValue = xDocumentInfo.getUserFieldValue(SignItConstants.META_DATA_ARG0);
            if (!userFieldValue.isEmpty()) {
                int showConfirmDialog = JOptionPane.showConfirmDialog(new JFrame(), "The document has already been signed. Do you want to overwrite?", "Overwrite?", JOptionPane.OK_CANCEL_OPTION);
                if (showConfirmDialog == JOptionPane.CANCEL_OPTION) {
                    return false;
                }
            }
            return true;
        } catch (ArrayIndexOutOfBoundsException ex) {
            return true;
        }
    }
}
