package org.nightlabs.editor2d.iofilter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.nightlabs.editor2d.DrawComponent;
import org.nightlabs.editor2d.util.ImageGenerator;
import org.nightlabs.io.AbstractSingleFileExtensionIOFilter;

/**
 * @author Daniel.Mazurek [at] NightLabs [dot] de
 *
 */
public abstract class AbstractImageFilter extends AbstractSingleFileExtensionIOFilter {

    @Override
    protected boolean supportsRead() {
        return false;
    }

    @Override
    protected boolean supportsWrite() {
        return true;
    }

    public Object read(InputStream in) throws IOException {
        throw new UnsupportedOperationException("ImageFilter can only write; NOT read!");
    }

    public void write(Object o, OutputStream out) throws IOException {
        DrawComponent dc = (DrawComponent) o;
        dc.clearBounds();
        int width = dc.getWidth();
        int height = dc.getHeight();
        ImageGenerator.writeModelAsImage(dc, width, height, initFileExtension(), out, dc.getRenderMode(), dc.getRenderModeManager());
    }
}
