package org.jd3lib;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import org.jd3lib.util.DebugOut;

/**
 * @author Andreas Grunewald
 * 
 * TODO v1.0 Write Documentation
 */
public class Id3v2Tag implements MetaData {

    Id3v2TagFactory fact;

    Id3FrameList frames;

    Id3v2TagHeader header;

    public static boolean faulty = false;

    /**
   * Create new ID3v2Tag
   */
    public Id3v2Tag() {
        frames = new Id3FrameList();
        header = new Id3v2TagHeader(true);
    }

    public ByteArrayOutputStream getTagData() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream theFrames = frames.getFramesData();
        try {
            header.getHeaderData().writeTo(out);
            theFrames.writeTo(out);
            int frameSize = theFrames.size();
            if (frameSize < header.getSize()) {
                byte[] padding = new byte[header.getSize() - frameSize];
                out.write(padding);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return out;
    }

    /**
   * @throws InstantiationException
   *           Parse existing Stream
   * 
   * @deprecated will be removed in final version
   */
    public Id3v2Tag(FileInputStream input) throws InstantiationException {
        this(input.getChannel());
    }

    /**
   * Standrad Constructor accepts filechannel of an MP3 File
   * 
   * @param theStream
   */
    public Id3v2Tag(FileChannel input) throws InstantiationException {
        ByteBuffer headerData = ByteBuffer.allocate(10);
        try {
            input.read(headerData);
            fact = Id3v2Tag_Getter.getFactory(headerData.array());
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        if (fact != null) {
            header = fact.getHeader();
            DebugOut.println(header);
            long avail = 0;
            try {
                DebugOut.println("InputPosition:" + input.position());
                DebugOut.println("Inputsize" + input.size());
                avail = input.size() - input.position();
                DebugOut.println("Available:" + avail);
                ByteBuffer tag = ByteBuffer.allocate(header.getSize());
                input.read(tag);
                tag.flip();
                frames = fact.getFrames(tag);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            throw new InstantiationException("Invalid Header can not create Tag");
        }
    }

    public Id3Frame addFrame(String fId) {
        throw new UnsupportedOperationException("This method has been deimplemented during refactoring");
    }

    /**
   * @deprecated this method is replaced by getTagData
   * @return the Tag in byte representation
   */
    public byte[] serializeTag() {
        byte[] headerStream = header.writeBytes();
        byte[] framesStream = frames.writeBytes();
        byte[] out = new byte[headerStream.length + framesStream.length];
        System.arraycopy(headerStream, 0, out, 0, headerStream.length);
        System.arraycopy(framesStream, 0, out, headerStream.length, framesStream.length);
        return out;
    }

    public boolean getFrameAvailability(String frameID) {
        return frames.containsFrame(frameID);
    }

    /**
   * @param string
   */
    public Id3Frame getFrame(String id) {
        return frames.getFrame(id);
    }

    public String toString() {
        return "HEADER:\n" + header.toString() + "FRAMES:\n" + frames.toString();
    }

    public String getTString(String fId) {
        Id3Frame current = this.getFrame(fId);
        if (current == null) return null;
        return ((Id3FrameTBase) current).getInformation();
    }

    /**
   * @param string
   * @param album
   */
    public void setTString(String fId, String inf) {
        Id3Frame current = this.getFrame(fId);
        if (current == null) current = this.addFrame(fId);
        ((Id3FrameTBase) current).setInformation(inf);
    }

    public String getTitle() {
        return getTString("TIT2");
    }

    public void setTitle(String newTitle) {
        setTString("TIT2", newTitle);
    }

    public String getArtist() {
        return getTString("TPE1");
    }

    public void setArtist(String newArtist) {
        setTString("TPE1", newArtist);
    }

    public String getAlbum() {
        String out = getTString("TALB");
        if (out == null) out = "";
        return out;
    }

    public void setAlbum(String newAlbum) {
        setTString("TALB", newAlbum);
    }

    public int getYear() {
        try {
            return Integer.parseInt(getTString("TYER"));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public void setYear(int newYear) {
        setTString("TYER", Integer.toString(newYear));
    }

    public String getComment() {
        Id3Frame current = this.getFrame("COMM");
        if (current == null) return null;
        return ((Id3FrameCOMM) current).getComment();
    }

    public void setComment(String newComment) {
        String type = "COMM";
        Id3Frame current = this.getFrame(type);
        if (current == null) current = this.addFrame(type);
        ((Id3FrameCOMM) current).setComment(newComment);
    }

    public int getTrack() {
        Id3Frame current = this.getFrame("TRCK");
        if (current == null) return -1;
        try {
            return ((Id3FrameTRCK) current).getTrackNo();
        } catch (NumberFormatException e) {
            return -2;
        }
    }

    public void setTrack(int newTrack) {
        String type = "TRCK";
        String value = new Integer(newTrack).toString();
        Id3Frame current = this.getFrame(type);
        if (current == null) current = this.addFrame(type);
        ((Id3FrameTRCK) current).setInformation(value);
    }

    public String getGenre() {
        return getTString("TCON");
    }

    public void setGenre(String newGenre) {
        setTString("TCON", newGenre);
    }
}
