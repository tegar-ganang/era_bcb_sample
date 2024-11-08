package net.sourceforge.fraglets.aotools.codec;

import com.jclark.xml.output.UTF8XMLWriter;
import com.jclark.xsl.sax.XSLProcessorImpl;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import net.sourceforge.fraglets.aotools.model.BaseNanoCluster;
import net.sourceforge.fraglets.aotools.model.BaseNanoList;
import net.sourceforge.fraglets.aotools.model.Body;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/** XML encoder for nano lists.
 *
 * @author kre
 * @version $Revision: 1.2 $
 */
public class NanoListPresenter {

    /** The UTF8 writer. */
    UTF8XMLWriter out;

    /** Creates new NanoListPresenter
     * @param out the output stream for XML output
     */
    public NanoListPresenter(OutputStream out) {
        this.out = new UTF8XMLWriter(out, UTF8XMLWriter.MINIMIZE_EMPTY_ELEMENTS);
    }

    /** Encode a base nano list.
     * @param list the list to encode
     * @throws IOException on IO failures
     */
    public void encodeBaseNanoList(BaseNanoList list) throws IOException {
        out.processingInstruction("xml-stylesheet", "href=\"PresentNanoList.xsl\" type=\"text/xsl\"");
        out.write('\n');
        out.startElement(NanoListTags.PRESENT_NANO_LIST);
        out.write('\n');
        Iterator i = list.getAllNanos();
        String currentName = null;
        String currentSkill = null;
        int currentSlot[] = new int[BaseNanoCluster.NANO_TYPES.length];
        Arrays.fill(currentSlot, -1);
        while (i.hasNext()) {
            BaseNanoCluster nextCluster = (BaseNanoCluster) i.next();
            String name = nextCluster.getName();
            if (currentName != null && !currentName.equals(name)) {
                encodeBaseNanoCluster(currentName, currentSkill, currentSlot);
                currentName = null;
                currentSkill = null;
                Arrays.fill(currentSlot, -1);
            }
            currentName = name;
            String skill = nextCluster.getSkill();
            if (currentSkill == null) {
                currentSkill = skill;
            } else if (!currentSkill.equals(skill)) {
                throw new IllegalArgumentException("mismatched skill: " + nextCluster);
            }
            int type = nextCluster.getType();
            int slot = nextCluster.getBodyLoc();
            if (currentSlot[type] == -1) {
                currentSlot[type] = slot;
            } else if (currentSlot[type] == slot) {
                System.err.println("warning: duplicate nano type: " + nextCluster);
            } else {
                throw new IllegalArgumentException("mismatched nano type: " + nextCluster);
            }
        }
        if (currentName != null) {
            encodeBaseNanoCluster(currentName, currentSkill, currentSlot);
        }
        out.endElement(NanoListTags.PRESENT_NANO_LIST);
        out.write('\n');
    }

    /** Encode a base nano.
     * @param nano the nano to encode
     * @throws IOException on IO failures
     */
    public void encodeBaseNanoCluster(String name, String skill, int slot[]) throws IOException {
        out.write("  ");
        out.startElement(NanoListTags.PRESENT_NANO_CLUSTER);
        out.attribute(NanoListTags.NAME, name);
        out.attribute(NanoListTags.SKILL, skill);
        out.write('\n');
        for (int i = 0; i < slot.length; i++) {
            String typeName = BaseNanoCluster.NANO_TYPES[i];
            if (!typeName.equalsIgnoreCase("unknown") && slot[i] != -1) {
                String slotName = Body.SLOT_NAMES[slot[i]];
                encodeBaseNanoType(typeName, slotName);
            }
        }
        out.write("  ");
        out.endElement(NanoListTags.PRESENT_NANO_CLUSTER);
        out.write('\n');
    }

    /** Encode one nano type.
     * @param name name of the property
     * @param value value of the property
     * @throws IOException on IO failures
     */
    public void encodeBaseNanoType(String name, String slot) throws IOException {
        out.write("    ");
        out.startElement(NanoListTags.PRESENT_NANO_TYPE);
        out.attribute(NanoListTags.NAME, name);
        out.write(slot);
        out.endElement(NanoListTags.PRESENT_NANO_TYPE);
        out.write('\n');
    }

    public static void encode(BaseNanoList list, URL style, OutputStream out) throws IOException, SAXException {
        XSLProcessorImpl processor = ProcessorFactory.createProcessor(style, out);
        EncoderThread writer = new EncoderThread(list);
        try {
            writer.start();
            processor.parse(writer.getInputSource());
            writer.join();
        } catch (InterruptedException ex) {
        }
    }

    public static class EncoderThread extends Thread {

        private BaseNanoList list;

        private PipedOutputStream out;

        private PipedInputStream in;

        public EncoderThread(BaseNanoList list) throws IOException {
            this.list = list;
            this.out = new PipedOutputStream();
            this.in = new PipedInputStream(this.out);
        }

        public InputSource getInputSource() {
            try {
                java.io.InputStreamReader reader = new java.io.InputStreamReader(in, "UTF-8");
                InputSource source = new InputSource(reader);
                return source;
            } catch (UnsupportedEncodingException ex) {
                throw new RuntimeException("unsupported mandatory encoding: " + ex);
            }
        }

        public void run() {
            try {
                NanoListPresenter presenter = new NanoListPresenter(out);
                presenter.encodeBaseNanoList(list);
                presenter.flush();
                out.close();
            } catch (java.io.IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    /** Flush the output stream.
     * @throws IOException on IO failures
     */
    public void flush() throws IOException {
        out.flush();
    }

    /** Testing method.
     * @param args provide exactly onr file name for output
     */
    public static void main(String args[]) {
        try {
            int arg = 0;
            boolean doHtml;
            if (arg < args.length && args[arg].equals("-html")) {
                arg += 1;
                doHtml = true;
            } else {
                doHtml = false;
            }
            OutputStream out;
            if (args.length > arg) {
                out = new java.io.FileOutputStream(args[arg]);
            } else {
                out = System.out;
            }
            if (doHtml) {
                encode(BaseNanoList.getBaseNanoList(), NanoListPresenter.class.getResource("PresentNanoList.xsl"), out);
            } else {
                NanoListPresenter encoder = new NanoListPresenter(out);
                encoder.encodeBaseNanoList(BaseNanoList.getBaseNanoList());
                encoder.flush();
            }
            if (out != System.out) {
                out.close();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
