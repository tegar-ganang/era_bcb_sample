package blue.soundObject;

import java.io.Serializable;
import javax.swing.JOptionPane;
import blue.Arrangement;
import blue.BlueSystem;
import blue.GlobalOrcSco;
import blue.SoundObjectLibrary;
import blue.Tables;
import blue.noteProcessor.NoteProcessorChain;
import blue.orchestra.GenericInstrument;
import blue.utility.ObjectUtilities;
import electric.xml.Element;

/**
 * @author steven
 *
 */
public class FrozenSoundObject extends AbstractSoundObject implements Serializable, Cloneable {

    private static final String FSO_INSTR_NAME = "Frozen SoundObject Player Instrument";

    private static final String FSO_HAS_BEEN_COMPILED = "frozenSoundObject.hasBeenCompiled";

    private SoundObject frozenSoundObject;

    private String frozenWaveFileName;

    private int numChannels = 0;

    private static transient int instrumentNumber;

    public FrozenSoundObject() {
    }

    public float getObjectiveDuration() {
        return this.subjectiveDuration;
    }

    public NoteProcessorChain getNoteProcessorChain() {
        return null;
    }

    public void setNoteProcessorChain(NoteProcessorChain chain) {
    }

    public int getTimeBehavior() {
        return SoundObject.TIME_BEHAVIOR_NOT_SUPPORTED;
    }

    public void setTimeBehavior(int timeBehavior) {
        return;
    }

    public float getRepeatPoint() {
        return -1.0f;
    }

    public void setRepeatPoint(float repeatPoint) {
    }

    public SoundObject getFrozenSoundObject() {
        return frozenSoundObject;
    }

    public void setFrozenSoundObject(SoundObject frozenSoundObject) {
        this.frozenSoundObject = frozenSoundObject;
    }

    public String getFrozenWaveFileName() {
        return frozenWaveFileName;
    }

    public void setFrozenWaveFileName(String frozenWaveFileName) {
        this.frozenWaveFileName = frozenWaveFileName;
    }

    public void generateGlobals(GlobalOrcSco globalOrcSco) {
    }

    public void generateFTables(Tables tables) {
    }

    public NoteList generateNotes(float renderStart, float renderEnd) throws SoundObjectException {
        NoteList n = new NoteList();
        if (instrumentNumber == 0) {
            return n;
        }
        float newDur = subjectiveDuration;
        if (renderEnd > 0 && renderEnd < subjectiveDuration) {
            newDur = renderEnd;
        }
        newDur = newDur - renderStart;
        StringBuffer buffer = new StringBuffer();
        buffer.append("i").append(instrumentNumber);
        buffer.append("\t").append(startTime + renderStart);
        buffer.append("\t").append(newDur);
        buffer.append("\t\"").append(this.getFrozenWaveFileName()).append("\"");
        buffer.append("\t").append(renderStart);
        Note tempNote = null;
        try {
            tempNote = Note.createNote(buffer.toString());
        } catch (NoteParseException e) {
            throw new SoundObjectException(this, e);
        }
        if (tempNote != null) {
            n.addNote(tempNote);
        }
        return n;
    }

    public void generateInstruments(Arrangement arrangement) {
        Object obj = arrangement.getCompilationVariable(FSO_HAS_BEEN_COMPILED);
        if (obj == null || obj != Boolean.TRUE) {
            arrangement.setCompilationVariable(FSO_HAS_BEEN_COMPILED, Boolean.TRUE);
            String instrumentText = generateInstrumentText();
            if (instrumentText == null) {
                JOptionPane.showMessageDialog(null, BlueSystem.getString("audioFile.couldNotGenerate") + " " + getName());
                return;
            }
            GenericInstrument temp = new GenericInstrument();
            temp.setName(FSO_INSTR_NAME);
            temp.setText(instrumentText);
            temp.setEnabled(true);
            int iNum = arrangement.addInstrument(temp);
            instrumentNumber = iNum;
        }
    }

    private String generateInstrumentText() {
        StringBuffer iText = new StringBuffer();
        String channelVariables = getChannelVariables();
        if (channelVariables == null) {
            return null;
        }
        iText.append(channelVariables).append("\tdiskin2\tp4, 1, p5\n");
        if (this.numChannels == 1) {
            iText.append("\tout\t").append(channelVariables);
        } else {
            iText.append("\toutc\t").append(channelVariables);
        }
        return iText.toString();
    }

    private String getChannelVariables() {
        if (numChannels <= 0) {
            return null;
        }
        String info = "aChannel1";
        int i = 1;
        while (i < numChannels) {
            i++;
            info += ", aChannel" + i;
        }
        return info;
    }

    public static void main(String[] args) {
    }

    /**
     * @return
     */
    public int getNumChannels() {
        return numChannels;
    }

    /**
     * @param i
     */
    public void setNumChannels(int i) {
        numChannels = i;
    }

    public static SoundObject loadFromXML(Element data, SoundObjectLibrary sObjLibrary) throws Exception {
        FrozenSoundObject fso = new FrozenSoundObject();
        SoundObjectUtilities.initBasicFromXML(data, fso);
        fso.setNumChannels(Integer.parseInt(data.getElement("numChannels").getTextString()));
        fso.setFrozenWaveFileName(data.getElement("frozenWaveFileName").getTextString());
        fso.setFrozenSoundObject((SoundObject) ObjectUtilities.loadFromXML(data.getElement("soundObject"), sObjLibrary));
        return fso;
    }

    public Element saveAsXML(SoundObjectLibrary sObjLibrary) {
        Element retVal = SoundObjectUtilities.getBasicXML(this);
        retVal.addElement("numChannels").setText(Integer.toString(this.getNumChannels()));
        retVal.addElement("frozenWaveFileName").setText(this.getFrozenWaveFileName());
        retVal.addElement(this.getFrozenSoundObject().saveAsXML(sObjLibrary));
        return retVal;
    }
}
