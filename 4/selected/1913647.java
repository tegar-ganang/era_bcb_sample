package blue.soundObject.ceciliaModule;

import java.io.File;
import java.io.IOException;
import javax.sound.sampled.UnsupportedAudioFileException;
import blue.utility.SoundFileUtilities;
import electric.xml.Element;

public class CFileIn extends CeciliaObject {

    private String fileName;

    private int offset;

    /**
     * @return Returns the offset.
     */
    public int getOffset() {
        return offset;
    }

    /**
     * @param offset
     *            The offset to set.
     */
    public void setOffset(int offset) {
        this.offset = offset;
    }

    public CFileIn() {
        fileName = "";
        offset = 0;
    }

    /**
     * @return Returns the fileName.
     */
    public String getFileName() {
        return fileName.replace('\\', '/');
    }

    /**
     * @param fileName
     *            The fileName to set.
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String processText(String ceciliaText) {
        return null;
    }

    public void initialize(String[] tokens) {
        this.setObjectName(tokens[1]);
        for (int i = 2; i < tokens.length; i += 2) {
            if (tokens[i].equals("-label")) {
            } else {
            }
        }
    }

    public static CeciliaObject loadFromXML(Element data) {
        CFileIn cObj = new CFileIn();
        CeciliaObject.initBasicFromXML(data, cObj);
        cObj.setFileName(data.getTextString("fileName"));
        try {
            cObj.setOffset(Integer.parseInt(data.getTextString("offset")));
        } catch (Exception e) {
        }
        return cObj;
    }

    public Element saveAsXML() {
        Element retVal = CeciliaObject.getBasicXML(this);
        retVal.addElement("fileName").setText(this.getFileName());
        retVal.addElement("offset").setText(Integer.toString(this.offset));
        return retVal;
    }

    /**
     * @return
     */
    public boolean isAudioFile() {
        if (this.fileName == null || this.fileName.equals("")) {
            return false;
        }
        File f = new File(this.fileName);
        if (!f.exists()) {
            return false;
        }
        try {
            SoundFileUtilities.getDurationInSeconds(this.fileName);
            return true;
        } catch (IOException e) {
            return false;
        } catch (UnsupportedAudioFileException e) {
            return false;
        }
    }

    /**
     * Used by Offset Slider to figure out how many ticks to set for range
     * 
     * @return
     */
    public int getMaxTicks() {
        try {
            float dur = SoundFileUtilities.getDurationInSeconds(this.fileName);
            return (int) (dur * 10);
        } catch (Exception e) {
            return -1;
        }
    }

    public float getDuration() {
        try {
            return SoundFileUtilities.getDurationInSeconds(this.getFileName());
        } catch (IOException e) {
            return -1.0f;
        } catch (UnsupportedAudioFileException e) {
            return -1.0f;
        }
    }

    public int getChannels() {
        try {
            return SoundFileUtilities.getNumberOfChannels(this.getFileName());
        } catch (IOException e) {
            return -1;
        } catch (UnsupportedAudioFileException e) {
            return -1;
        }
    }

    public int getFrames() {
        try {
            return SoundFileUtilities.getNumberOfFrames(this.getFileName());
        } catch (IOException e) {
            return -1;
        } catch (UnsupportedAudioFileException e) {
            return -1;
        }
    }

    public float getSampleRate() {
        try {
            return SoundFileUtilities.getSampleRate(this.getFileName());
        } catch (IOException e) {
            return -1.0f;
        } catch (UnsupportedAudioFileException e) {
            return -1.0f;
        }
    }
}
