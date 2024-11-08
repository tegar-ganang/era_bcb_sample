package gnu.activation.viewers;

import java.awt.Dimension;
import java.awt.TextArea;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import javax.activation.CommandObject;
import javax.activation.DataHandler;

/**
 * Simple text display component.
 *
 * @author <a href='mailto:dog@gnu.org'>Chris Burdess</a>
 * @version 1.0.2
 */
public class TextViewer extends TextArea implements CommandObject {

    public TextViewer() {
        super("", 24, 80, 1);
        setEditable(false);
    }

    public Dimension getPreferredSize() {
        return getMinimumSize(24, 80);
    }

    public void setCommandContext(String verb, DataHandler dh) throws IOException {
        InputStream in = dh.getInputStream();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        for (int len = in.read(buf); len != -1; len = in.read(buf)) bytes.write(buf, 0, len);
        in.close();
        setText(bytes.toString());
    }
}
