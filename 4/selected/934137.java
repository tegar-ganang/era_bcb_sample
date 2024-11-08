package net.sf.ij_plugins.imageio;

import ij.IJ;
import ij.plugin.PlugIn;
import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.IIOServiceProvider;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.ImageWriterSpi;
import java.util.Iterator;

/**
 * Displays information about available javax.imageio image reader and image writer service
 * providers.
 *
 * @author Jarek Sacha
 * @version $Revision: 1.3 $
 */
public class IJImageIOInfoPlugin implements PlugIn {

    public void run(final String arg) {
        String message = "--------------------------------------------\n" + serviceProviderInfo(ImageReaderSpi.class, false) + "--------------------------------------------\n" + serviceProviderInfo(ImageWriterSpi.class, false) + "--------------------------------------------\n";
        message += "Reader format names: ";
        final String[] readers = ImageIO.getReaderFormatNames();
        for (final String reader : readers) {
            message += reader + ", ";
        }
        message += "\n";
        message += "Reader format names: ";
        final String[] writers = ImageIO.getWriterFormatNames();
        for (final String writer : writers) {
            message += writer + ", ";
        }
        IJ.showMessage("ImageIO readers & writers", message);
    }

    private static String serviceProviderInfo(final Class category, final boolean useOrdering) {
        final Iterator categories = IIORegistry.getDefaultInstance().getServiceProviders(category, useOrdering);
        final StringBuffer buf = new StringBuffer();
        while (categories.hasNext()) {
            final Object o = categories.next();
            final IIOServiceProvider iioServiceProvider = (IIOServiceProvider) o;
            buf.append(iioServiceProvider.getDescription(null));
            buf.append(" : ");
            buf.append(o.getClass().getName());
            buf.append("\n");
        }
        return buf.toString();
    }
}
