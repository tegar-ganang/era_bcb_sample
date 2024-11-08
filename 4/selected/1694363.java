package uk.ac.liv.jt.debug;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.liv.jt.format.ByteReader;
import uk.ac.liv.jt.format.JTFile;
import uk.ac.liv.jt.segments.JTSegment;
import de.jreality.reader.AbstractReader;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.util.Input;

public class DebugJTReader extends AbstractReader {

    private JTFile jtFile;

    private SceneGraphComponent mRootGroupNode;

    private ByteReader reader;

    public JTFile getJtFile() {
        return jtFile;
    }

    public SceneGraphComponent getMRootGroupNode() {
        return mRootGroupNode;
    }

    public SceneGraphComponent getRootGroupNode() {
        return mRootGroupNode;
    }

    /**
     * Copies content of an inputStream to temp a file
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

    private boolean load(InputStream inputStream) {
        try {
            File jtf = copyToTemp(inputStream);
            reader = new ByteReader(jtf);
            setJtFile(new JTFile(reader));
            if (DebugInfo.debugMode) {
                System.out.println("\n=================");
                System.out.println("== File Header ==");
                System.out.println("=================\n");
            }
            jtFile.readHeader();
            if (DebugInfo.debugMode) {
                System.out.println("\n=================");
                System.out.println("== TOC Segment ==");
                System.out.println("=================\n");
            }
            reader.position(jtFile.getTocOffset());
            jtFile.readTOC();
            Collection<JTSegment> segments = jtFile.getSegments();
            for (JTSegment segment : segments) if (segment.getOffset() == 48810) {
                if (DebugInfo.debugMode) {
                    System.out.println("\n===============");
                    System.out.println("== Segment " + segment.getType() + " ==");
                    System.out.println("===============\n");
                }
                segment.read();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Logger.getLogger("liv.JTViewer").log(Level.WARNING, "JTReader.load");
            Logger.getLogger("liv.JTViewer").log(Level.WARNING, "\tIOException");
            return false;
        }
        return true;
    }

    @Override
    public void setInput(Input input) throws IOException {
        super.setInput(input);
        root = new SceneGraphComponent("jt");
        setRootGroupNode(root);
        load(input.getInputStream());
    }

    public void setJtFile(JTFile jtFile) {
        this.jtFile = jtFile;
    }

    public void setMRootGroupNode(SceneGraphComponent rootGroupNode) {
        mRootGroupNode = rootGroupNode;
    }

    private void setRootGroupNode(SceneGraphComponent node) {
        mRootGroupNode = node;
    }

    public ByteReader getReader() {
        return reader;
    }

    public void setReader(ByteReader reader) {
        this.reader = reader;
    }
}
