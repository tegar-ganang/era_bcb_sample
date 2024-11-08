package uk.ac.liv.jt.viewer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import uk.ac.liv.jt.format.JTFile;
import uk.ac.liv.jt.segments.LSGSegment;
import de.jreality.reader.AbstractReader;
import de.jreality.util.Input;
import uk.ac.liv.jt.format.elements.TriStripSetShapeLODElement;

/**
 * Implementation of a JT file Reader for the JReality system. 
 * This needs to be properly registered in the system.
 * 
 * @see JTViewer
 * @author fabio
 *
 */
public class JTReader extends AbstractReader {

    private JTFile jtFile;

    @Override
    public void setInput(Input input) throws IOException {
        super.setInput(input);
        TriStripSetShapeLODElement.testDisplay = false;
        File jtf = null;
        try {
            jtf = input.toFile();
        } catch (Exception e) {
        }
        if (jtf == null) try {
            URI uri = input.toURL().toURI();
            jtf = new File(uri);
        } catch (Exception e) {
            System.out.println("copy inputStream to file");
            InputStream inputStream = input.getInputStream();
            jtf = copyToTemp(inputStream);
        }
        jtFile = new JTFile(jtf, jtf.toURI());
        LSGSegment lsg = jtFile.read();
        root = lsg.generateSceneGraph();
    }

    /**
     * Utility method, copies content of an inputStream to temp a file
     * 
     * @param is
     * @return the file just created
     * @throws IOException
     */
    public static File copyToTemp(InputStream is) throws IOException {
        File f = File.createTempFile("jtfile", null);
        FileOutputStream os = new FileOutputStream(f);
        byte[] buf = new byte[16 * 1024];
        int i;
        while ((i = is.read(buf)) != -1) os.write(buf, 0, i);
        is.close();
        os.close();
        return f;
    }
}
