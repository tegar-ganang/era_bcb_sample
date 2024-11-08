package uk.ac.cam.caret.minibix.qtibank.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import uk.ac.cam.caret.minibix.archive.api.Item;
import uk.ac.cam.caret.minibix.archive.api.ItemFactory;
import org.apache.commons.io.IOUtils;

/**
 * A simple string
 * 
 */
public class SlurpedByteArrayItem implements Item {

    byte[] data;

    int size;

    boolean alreadySerialized = false;

    public SlurpedByteArrayItem(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOUtils.copy(is, baos);
        data = baos.toByteArray();
    }

    public ItemFactory getFactory() {
        return new SlurpedByteArrayItemFactory();
    }

    public void serialize(OutputStream os) throws IOException {
        os.write(data);
    }
}
