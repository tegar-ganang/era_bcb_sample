package net.sourceforge.processdash.ui.web.dash;

import java.io.IOException;
import net.sourceforge.processdash.ui.DashboardIconFactory;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.FileUtils;

/**
 * Dynamically serve up the favorites icon for this dashboard instance.
 * 
 * @author Tuma
 */
public class FavIcon extends TinyCGIBase {

    @Override
    protected void writeHeader() {
        out.print("Content-type: image/x-icon\r\n\r\n");
        out.flush();
    }

    @Override
    protected void writeContents() throws IOException {
        FileUtils.copyFile(DashboardIconFactory.getApplicationIconData(), outStream);
    }
}
