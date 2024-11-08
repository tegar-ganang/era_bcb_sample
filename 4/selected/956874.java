package gg.de.sbmp3.filesystem;

import de.ueberdosis.mp3info.ExtendedID3Tag;
import de.ueberdosis.mp3info.ID3Reader;
import de.ueberdosis.mp3info.id3v2.FrameCOMM;
import de.ueberdosis.mp3info.id3v2.FrameT;
import de.ueberdosis.mp3info.id3v2.ID3V2Frame;
import de.ueberdosis.mp3info.id3v2.ID3V2Tag;
import de.ueberdosis.util.OutputCtr;
import gg.de.sbmp3.backend.data.FileBean;
import gg.de.sbmp3.common.MemoryRandomAccessFile;
import gg.de.sbmp3.common.Util;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.xml.DOMConfigurator;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;

/**
 * @author Benjamin
 *
 * Creates a new filebean from a mp3 file.
 * Reads all value form the mp3 file and saves it to the bean.
 */
public class Tagreader {

    private static Log log = LogFactory.getLog(Tagreader.class);

    private File file;

    private FileBean filebean;

    private Map tags;

    private ID3Reader reader;

    private ExtendedID3Tag exttags;

    /**
	 * constructor
     *  
	 * @param filename the name and path of the mp3 file to create a filebean for
	 */
    public Tagreader(String filename, boolean ramBufferMode) {
        file = new File(filename);
        filebean = new FileBean();
        tags = new HashMap();
        filebean.setId3tags(tags);
        OutputCtr.setLevel(0);
        try {
            RandomAccessFile raf;
            if (ramBufferMode) raf = new MemoryRandomAccessFile(filename, "r"); else raf = new RandomAccessFile(filename, "r");
            reader = new ID3Reader(raf);
            exttags = reader.getExtendedID3Tag();
        } catch (IOException e) {
            if (log.isWarnEnabled()) log.warn("exception while reading id3 tags from file", e);
        }
    }

    /**
     * Reads all properties from the filesystem and mpeg header of
     * the file and all tags from the id3tags version 1 and 2 and
     * saves it to the filebean
     *
	 * @throws IOException
	 */
    public void read() throws IOException {
        readFileProperties();
        readMP3Properties();
        readID3v1();
        readID3v2();
    }

    private void readFileProperties() {
        filebean.setPath(file.getAbsolutePath());
        filebean.setFilesize((int) file.length());
        filebean.setModifiedDate(new Date(file.lastModified()));
    }

    private void readMP3Properties() {
        filebean.setBitrate(exttags.getBitrateI());
        filebean.setLength(exttags.getRuntime());
        filebean.setMode((byte) exttags.getChannelMode());
        filebean.setSamplerate(exttags.getFrequencyI());
        String codec = "MPEG " + exttags.getMpegIDS() + " Layer " + exttags.getLayerS();
        filebean.setCodec(codec);
        if (filebean.getLength() != 0) {
            int bav = filebean.getFilesize() * 8 / filebean.getLength() / 1024;
            filebean.setBitrateAverage(bav);
        }
    }

    private void readID3v1() {
        addID3Tag("v1AL", exttags.getAlbum());
        addID3Tag("v1AR", exttags.getArtist());
        addID3Tag("v1CM", exttags.getComment());
        addID3Tag("v1TI", exttags.getTitle());
        addID3Tag("v1YE", exttags.getYear());
        if (exttags.getGenre() != 126) addID3Tag("v1GE", Byte.toString(exttags.getGenre()));
        if (exttags.getTrack() != 0) addID3Tag("v1TR", Byte.toString(exttags.getTrack()));
    }

    private void readID3v2() throws IOException {
        ID3V2Tag v2tag = ID3Reader.getV2Tag();
        if (v2tag != null) {
            Vector v = v2tag.getFrames();
            Iterator i = v.iterator();
            while (i.hasNext()) {
                ID3V2Frame frame = (ID3V2Frame) i.next();
                if (frame instanceof FrameT) {
                    FrameT tframe = (FrameT) frame;
                    addID3Tag(tframe.getFrameID(), tframe.getText());
                }
                if (frame instanceof FrameCOMM) {
                    FrameCOMM commframe = (FrameCOMM) frame;
                    addID3Tag(commframe.getFrameID(), commframe.getText());
                }
            }
        }
    }

    private void addID3Tag(String tag, String value) {
        tag = Util.cleanString(tag);
        value = Util.cleanString(value);
        if (!value.equals("")) {
            tags.put(tag, value);
        }
    }

    /**
     * Gets the filebean.
     * It is only filled after a call of the read method.
     *
	 * @return the filled filebean
	 */
    public FileBean getFilebean() {
        return filebean;
    }

    /**
     * just for testing
     *
	 * @param args
	 */
    public static void main(String args[]) {
        DOMConfigurator.configure("log4j.xml");
        Tagreader t = new Tagreader("d:/temp/Barry White - Let The Music Play.mp3", false);
        try {
            t.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Path:\t" + t.getFilebean().getPath());
        System.out.println("Filesize:\t" + t.getFilebean().getFilesize() + " byte");
        System.out.println("Moddate:\t" + t.getFilebean().getModifiedDate());
        System.out.println();
        System.out.println("Codec:\t" + t.getFilebean().getCodec());
        System.out.println("Bitrate:\t" + t.getFilebean().getBitrate() + " kbps");
        System.out.println("Bitrate average:\t" + t.getFilebean().getBitrateAverage() + " kbps");
        System.out.println("Samplerate:\t" + t.getFilebean().getSamplerate() + " khz");
        System.out.println("Mode:\t" + t.getFilebean().getMode());
        System.out.println("Length:\t" + t.getFilebean().getLength() + " sec");
        System.out.println();
        Map tags = t.getFilebean().getId3tags();
        Iterator i = tags.keySet().iterator();
        while (i.hasNext()) {
            String key = (String) i.next();
            System.out.print(key);
            System.out.print(":\t");
            System.out.println(t.getFilebean().getId3tags().get(key));
        }
    }
}
