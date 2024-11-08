package jdrummer;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.Vector;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.swing.JPanel;
import jdrummer.xml.XMLCategory;

/**
 * This class represents the whole song, made up of sheets (pages), patterns (rows) and notes
 * @author Marlodavampire
 *
 */
public class Song {

    public Sheet[] s;

    public byte[] Drum[];

    AudioFormat outputFormat[];

    int currentSheet;

    public int numberOfSheets;

    JPanel panel;

    URL baseURL;

    public XMLCategory cat;

    boolean xmlDragging;

    boolean xmlThreeClicks;

    int maxSounds;

    Vector<Thread> nt;

    public Song(XMLCategory _cat, JPanel patternsPanel, URL _baseURL, boolean xmlDragging, boolean xmlThreeClicks, int maxSounds) {
        nt = new Vector<Thread>();
        cat = _cat;
        s = new Sheet[1];
        currentSheet = 0;
        numberOfSheets = 1;
        panel = patternsPanel;
        baseURL = _baseURL;
        Drum = new byte[16][];
        outputFormat = new AudioFormat[16];
        this.xmlDragging = xmlDragging;
        this.xmlThreeClicks = xmlThreeClicks;
        this.maxSounds = maxSounds;
        for (int x = 0; x < cat.getNumberOfDrums(); x++) {
            ByteArrayOutputStream buffer;
            buffer = new ByteArrayOutputStream();
            try {
                AudioInputStream din = null;
                AudioInputStream in = AudioSystem.getAudioInputStream(new BufferedInputStream(getClass().getClassLoader().getResourceAsStream(cat.drum[x].getFile())));
                AudioFormat baseFormat = in.getFormat();
                AudioFormat decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, baseFormat.getSampleRate(), 16, baseFormat.getChannels(), baseFormat.getChannels() * 2, baseFormat.getSampleRate(), false);
                din = AudioSystem.getAudioInputStream(decodedFormat, in);
                if (buffer != null) {
                    byte[] data = new byte[4096];
                    int nBytesRead;
                    while ((nBytesRead = din.read(data, 0, data.length)) != -1) {
                        try {
                            buffer.write(data, 0, nBytesRead);
                        } catch (Exception e) {
                            System.out.println(e);
                        }
                    }
                    din.close();
                }
                outputFormat[x] = decodedFormat;
            } catch (Exception e) {
                System.out.println(e);
            }
            Drum[x] = buffer.toByteArray();
            buffer = null;
        }
        s[0] = new Sheet(cat, patternsPanel, _baseURL, outputFormat, xmlDragging, xmlThreeClicks);
        s[0].draw(patternsPanel);
    }

    public void tick() {
        boolean next = false;
        for (int x = 0; x < cat.getNumberOfDrums(); x++) {
            s[currentSheet].p[x].playNote(Drum[x], nt, maxSounds);
            if (s[currentSheet].p[x].getNextNote() >= 16) {
                s[currentSheet].p[x].reset();
                next = true;
            }
        }
        if (next) moveToNext();
    }

    public void undraw(JPanel patternsPanel) {
        s[currentSheet].undraw(patternsPanel);
    }

    public void draw(JPanel patternsPanel) {
        s[currentSheet].draw(patternsPanel);
    }

    public int getCurrentSheet() {
        return currentSheet;
    }

    public int getNumberOfSheets() {
        return numberOfSheets;
    }

    public void addSheet(boolean move) {
        Sheet[] old;
        old = s;
        s = new Sheet[numberOfSheets + 1];
        for (int x = 0; x < numberOfSheets; x++) {
            s[x] = old[x];
        }
        if (move) s[currentSheet].undraw(panel);
        s[numberOfSheets] = new Sheet(cat, panel, baseURL, outputFormat, xmlDragging, xmlThreeClicks);
        if (move) s[numberOfSheets].draw(panel);
        if (move) currentSheet = numberOfSheets;
        numberOfSheets++;
    }

    public void deleteSheet() {
        if (numberOfSheets > 1) {
            Sheet[] old;
            old = s;
            s = new Sheet[numberOfSheets - 1];
            if (currentSheet == 0) {
                old[0].undraw(panel);
                for (int x = 0; x < numberOfSheets - 1; x++) {
                    s[x] = old[x + 1];
                }
                currentSheet = 0;
                s[currentSheet].draw(panel);
            } else if (currentSheet == numberOfSheets - 1) {
                old[numberOfSheets - 1].undraw(panel);
                for (int x = 0; x < numberOfSheets - 1; x++) {
                    s[x] = old[x];
                }
                currentSheet = currentSheet - 1;
                s[currentSheet].draw(panel);
            } else {
                old[currentSheet].undraw(panel);
                for (int x = 0; x < currentSheet; x++) {
                    s[x] = old[x];
                }
                for (int x = currentSheet + 1; x <= numberOfSheets - 1; x++) {
                    s[x - 1] = old[x];
                }
                currentSheet = currentSheet - 1;
                s[currentSheet].draw(panel);
            }
            numberOfSheets--;
        } else {
            s[0].undraw(panel);
            s = new Sheet[1];
            currentSheet = 0;
            s[0] = new Sheet(cat, panel, baseURL, outputFormat, xmlDragging, xmlThreeClicks);
            s[0].draw(panel);
        }
    }

    public void moveTo(int sheetNum) {
        s[currentSheet].undraw(panel);
        currentSheet = sheetNum;
        s[currentSheet].draw(panel);
    }

    public void moveToNext() {
        if (!(numberOfSheets == 1)) {
            if (currentSheet == numberOfSheets - 1) {
                moveTo(0);
            } else {
                moveTo(currentSheet + 1);
            }
        }
    }
}
