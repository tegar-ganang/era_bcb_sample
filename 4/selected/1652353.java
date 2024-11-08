package net.i2geo.constructions.xwikiapi.img;

import net.i2geo.api.server.Construction;
import net.i2geo.api.server.HtmlFragment;
import java.net.URL;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.IOException;

/** Simple construction to return png of a fixed size.
 *
 * @author Paul Libbrecht, DFKI GmbH, under the intergeo project, http://inter2geo.eu/
 */
public class ImgConstruction extends Construction {

    public ImgConstruction(URL constructionURL, ImgConstructionFactory factory) {
        super(constructionURL);
        this.factory = factory;
    }

    private final ImgConstructionFactory factory;

    public int getAmountOfParts() {
        return 1;
    }

    public HtmlFragment getHtmlFragment(String pathToArchive, String pathToConstruction, int partNum) throws UnsupportedOperationException {
        return new ImgHtmlFragment(pathToConstruction, factory);
    }

    public void writePNG(OutputStream out, int partNum) throws UnsupportedOperationException {
        byte[] buff = new byte[128];
        int r = 0;
        try {
            InputStream in = constructionURL.openStream();
            while ((r = in.read(buff, 0, 128)) > 0) out.write(buff, 0, r);
            out.flush();
        } catch (IOException e) {
            throw new IllegalStateException("Failed delivering image.", e);
        }
    }
}
