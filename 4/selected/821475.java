package rafa.midi.saiph.gen;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import javax.sound.midi.InvalidMidiDataException;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import rafa.midi.saiph.SaiphXML;
import rafa.midi.saiph.values.Values;
import rafa.midi.saiph.values.gen.ConstantGenerator;
import rafa.midi.saiph.values.gen.ValuesGenerator;

/**
 * Generator of a track for MIDI channel messages.
 * @author rafa
  */
public abstract class CMSegmentGenerator extends SegmentGenerator {

    /** Generator for the MIDI channel of MIDI event in this Track. */
    protected ValuesGenerator channelGenerator;

    /** Generator for the first data byte of MIDI events. */
    protected ValuesGenerator byte1Generator;

    /** Generator for the second data byte of MIDI events. */
    protected ValuesGenerator byte2Generator;

    /**
	 * @param channel MIDI channel for the track's events.
	 * @param midiTrack MIDI track which will be filled by this Generator.
	 */
    public CMSegmentGenerator() {
        channelGenerator = new ConstantGenerator(Values.CHANNEL);
    }

    protected void reset() {
        super.reset();
        channelGenerator.reset();
        byte1Generator.reset();
        byte2Generator.reset();
    }

    /**
	 * @return
	 */
    public ValuesGenerator getByte1Generator() {
        return byte1Generator;
    }

    /**
	 * @return
	 */
    public ValuesGenerator getByte2Generator() {
        return byte2Generator;
    }

    /**
	 * @return
	 */
    public ValuesGenerator getChannelGenerator() {
        return channelGenerator;
    }

    /**
	 * @param generator
	 */
    public void setByte1Generator(ValuesGenerator generator) {
        byte1Generator = generator;
    }

    /**
	 * @param generator
	 */
    public void setByte2Generator(ValuesGenerator generator) {
        byte2Generator = generator;
    }

    /**
	 * @param i
	 * @throws InvalidMidiDataException
	 */
    public void setChannelGenerator(ValuesGenerator cg) {
        this.channelGenerator = cg;
    }

    public Node toSaiphXML(Document doc) {
        Node node = super.toSaiphXML(doc);
        Node channelGeneratorNode = doc.createElement("channelGenerator");
        channelGeneratorNode.appendChild(channelGenerator.toSaiphXML(doc));
        node.appendChild(channelGeneratorNode);
        return node;
    }

    public boolean fromSaiphXML(Node node) throws NumberFormatException, DOMException, SecurityException, IllegalArgumentException, InvalidMidiDataException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        boolean ok = super.fromSaiphXML(node);
        if (!ok) return false;
        Node cgNode = ((Element) node).getElementsByTagName("channelGenerator").item(0);
        Node vgNode = ((Element) cgNode).getFirstChild();
        String typeAttr = ((Element) vgNode).getAttribute("type");
        String cgClassName = ((Element) vgNode).getAttribute("class");
        Class cgClass = Class.forName(cgClassName);
        Constructor cgClassConstructor = cgClass.getConstructor(new Class[] { int.class });
        Object cg = cgClassConstructor.newInstance(new Object[] { Integer.valueOf(typeAttr) });
        ok = ((SaiphXML) cg).fromSaiphXML(vgNode);
        if (!ok) return false;
        setChannelGenerator((ValuesGenerator) cg);
        return ok;
    }
}
