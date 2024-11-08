import java.awt.*;
import java.io.*;
import java.beans.*;
import javax.activation.*;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;

/**
 * A very simple TextViewer Bean for the MIMEType "text/plain"
 *
 * @author	Christopher Cotton
 */
public class TextViewer extends JPanel implements CommandObject {

    private JTextArea text_area = null;

    private DataHandler dh = null;

    private String verb = null;

    /**
     * Constructor
     */
    public TextViewer() {
        super(new GridLayout(1, 1));
        text_area = new JTextArea();
        text_area.setEditable(false);
        text_area.setLineWrap(true);
        JScrollPane sp = new JScrollPane();
        sp.setPreferredSize(new Dimension(300, 300));
        sp.getViewport().add(text_area);
        add(sp);
    }

    public void setCommandContext(String verb, DataHandler dh) throws IOException {
        this.verb = verb;
        this.dh = dh;
        this.setInputStream(dh.getInputStream());
    }

    /**
   * set the data stream, component to assume it is ready to
   * be read.
   */
    public void setInputStream(InputStream ins) {
        int bytes_read = 0;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte data[] = new byte[1024];
        try {
            while ((bytes_read = ins.read(data)) > 0) baos.write(data, 0, bytes_read);
            ins.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        text_area.setText(baos.toString());
    }
}
