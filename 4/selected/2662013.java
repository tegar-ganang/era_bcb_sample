package de.marcelcarle.se.gruppe10.tagger.gui;

import java.awt.FlowLayout;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.KeyNotFoundException;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.datatype.Artwork;
import org.jaudiotagger.tag.id3.ID3v24Tag;

/**
 *
 * @author mict
 */
public class JInfoPanel extends JMyPanel {

    JLabel lLength;

    JTextField tfLength;

    JLabel lBitrate;

    JTextField tfBitrate;

    JLabel lFrequenz;

    JTextField tfFrequenz;

    JLabel lCounter;

    JTextField tfVersion;

    JLabel lSize;

    JTextField tfSize;

    JLabel lVBR;

    JTextField tfVBR;

    JLabel lChannels;

    JTextField tfChannels;

    public JInfoPanel() {
        super(new FlowLayout());
        initialize();
    }

    private void initialize() {
        lLength = new JLabel("Länge: ");
        tfLength = new JTextField(5);
        tfLength.setEditable(false);
        JTagDisplay tDLength = new JTagDisplay(lLength, tfLength);
        this.add(tDLength);
        lBitrate = new JLabel("BPM: ");
        tfBitrate = new JTextField(5);
        tfBitrate.setEditable(false);
        JTagDisplay tdBitrate = new JTagDisplay(lBitrate, tfBitrate);
        this.add(tdBitrate);
        lFrequenz = new JLabel("Frequenz: ");
        tfFrequenz = new JTextField(15);
        tfFrequenz.setEditable(false);
        JTagDisplay tdFrequenz = new JTagDisplay(lFrequenz, tfFrequenz);
        this.add(tdFrequenz);
        lCounter = new JLabel("Version: ");
        tfVersion = new JTextField(15);
        tfVersion.setEditable(false);
        JTagDisplay tdCounter = new JTagDisplay(lCounter, tfVersion);
        this.add(tdCounter);
        lSize = new JLabel("Dateigröße: ");
        tfSize = new JTextField(15);
        tfSize.setEditable(false);
        JTagDisplay tdSize = new JTagDisplay(lSize, tfSize);
        this.add(tdSize);
        lVBR = new JLabel("VBR: ");
        tfVBR = new JTextField(15);
        tfVBR.setEditable(false);
        JTagDisplay tDVBR = new JTagDisplay(lVBR, tfVBR);
        this.add(tDVBR);
        lChannels = new JLabel("Kanäle: ");
        tfChannels = new JTextField(15);
        tfChannels.setEditable(false);
        JTagDisplay tdChannels = new JTagDisplay(lChannels, tfChannels);
        this.add(tdChannels);
        setVisible(true);
    }

    @Override
    Tag getTag(Tag tag) {
        return tag;
    }

    @Override
    void setTags(AudioFile[] fArray, String fileExtension, Tag tmpTag) {
    }

    @Override
    void setTags(AudioFile audio, String fileExtension) {
        try {
            this.tfBitrate.setText(audio.getAudioHeader().getBitRate());
        } catch (KeyNotFoundException e) {
            this.tfBitrate.setText("");
        }
        try {
            this.tfChannels.setText(audio.getAudioHeader().getChannels());
        } catch (KeyNotFoundException e) {
            this.tfChannels.setText("");
        }
        try {
            this.tfVersion.setText(audio.getAudioHeader().getFormat());
        } catch (KeyNotFoundException e) {
            this.tfVersion.setText("");
        }
        try {
            this.tfFrequenz.setText(audio.getAudioHeader().getSampleRate());
        } catch (KeyNotFoundException e) {
            this.tfFrequenz.setText("");
        }
        try {
            this.tfLength.setText(String.valueOf(audio.getAudioHeader().getTrackLength()));
        } catch (KeyNotFoundException e) {
            this.tfLength.setText("");
        }
        try {
            this.tfSize.setText(String.valueOf(audio.getFile().length()));
        } catch (KeyNotFoundException e) {
            this.tfSize.setText("");
        }
        try {
            this.tfVBR.setText(String.valueOf(audio.getAudioHeader().isVariableBitRate()));
        } catch (KeyNotFoundException e) {
            this.tfVBR.setText("");
        }
    }
}
