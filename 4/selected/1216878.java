package gnu.activation.viewers;

import java.awt.Dimension;
import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.activation.CommandObject;
import javax.activation.DataHandler;

/**
 * Simple text editor component.
 *
 * @author <a href='mailto:dog@gnu.org'>Chris Burdess</a>
 * @version 1.0.2
 */
public class TextEditor extends TextArea implements CommandObject, ActionListener {

    private transient DataHandler dh;

    public TextEditor() {
        super("", 24, 80, 1);
    }

    public Dimension getPreferredSize() {
        return getMinimumSize(24, 80);
    }

    public void setCommandContext(String verb, DataHandler dh) throws IOException {
        this.dh = dh;
        InputStream in = dh.getInputStream();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        for (int len = in.read(buf); len != -1; len = in.read(buf)) bytes.write(buf, 0, len);
        in.close();
        setText(bytes.toString());
    }

    public void actionPerformed(ActionEvent event) {
        if ("save".equals(event.getActionCommand()) && dh != null) {
            OutputStream out = null;
            try {
                out = dh.getOutputStream();
                if (out != null) out.write(getText().getBytes());
            } catch (IOException e) {
                e.printStackTrace(System.err);
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace(System.err);
                    }
                }
            }
        }
    }
}
