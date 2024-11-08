package org.jd3lib;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * @author Andreas Grunewald
 * 
 * LATER 1.0 Write Documentation
 */
public class MP3File implements AudioFile {

    private final File theOriginal;

    private long MP3beginning;

    private MPEGInfo mpegI;

    private Id3v1Tag v1tag;

    private Id3v2Tag v2tag;

    private boolean iD3v2parsed;

    private boolean iD3v1parsed;

    /**
   * Associates this Object with a file
   * 
   * @param string
   *          the filePath
   */
    public MP3File(String string) {
        this(new File(string));
    }

    /**
   * @param currentFile
   */
    public MP3File(File currentFile) {
        theOriginal = currentFile;
        try {
            FileChannel theStream = new FileInputStream(theOriginal).getChannel();
            MP3beginning = theStream.size();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
   * @return
   */
    private byte[] getMP3() {
        try {
            FileInputStream temp = new FileInputStream(getFilePath());
            FileChannel tChan = temp.getChannel();
            ByteBuffer byBu = ByteBuffer.allocate((int) ((int) tChan.size() - MP3beginning));
            tChan.read(byBu, MP3beginning);
            return byBu.array();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new byte[] {};
    }

    /**
   * @return
   */
    public byte[] getData() {
        ByteArrayOutputStream tag = new ByteArrayOutputStream();
        if (this.hasID3v2()) try {
            this.getID3v2().getTagData().writeTo(tag);
            tag.write(this.getMP3());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return tag.toByteArray();
    }

    /**
   * Returns the canonical path to this file or /tmpNonExistingFile if the file
   * doesn't exist. TODO refactor is method see if it makes sense to throw an
   * exception and which type of exception.
   */
    public String getFilePath() {
        try {
            return theOriginal.getCanonicalPath();
        } catch (IOException e) {
            return "/tmpNonExistingFile";
        }
    }

    /**
   * @return
   */
    public Id3v1Tag getID3v1() {
        if (v1tag == null && iD3v1parsed == false) {
            FileChannel chan;
            try {
                chan = new FileInputStream(theOriginal).getChannel();
                if (chan.size() < 128) return null;
                chan.position(chan.size() - 128);
                ByteBuffer mybyte = ByteBuffer.allocate(128);
                chan.read(mybyte);
                mybyte.flip();
                try {
                    v1tag = new Id3v1Tag(mybyte);
                } catch (InstantiationException e) {
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return v1tag;
    }

    /**
   * @return
   */
    public Id3v2Tag getID3v2() {
        if (v2tag == null && iD3v2parsed == false) {
            iD3v2parsed = true;
            FileChannel theStream;
            try {
                theStream = new FileInputStream(theOriginal).getChannel();
                MP3beginning = theStream.size();
                try {
                    v2tag = new Id3v2Tag(theStream);
                } catch (InstantiationException e) {
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        return v2tag;
    }

    /**
   * @return
   */
    public MPEGInfo getMEPGInfo() {
        return null;
    }

    /**
   * Tests if the file has an ID3v1 tag if the tag hasn't been parsed it will
   * first try to parse the tag before returning a value
   * 
   * @return
   */
    public boolean hasID3v1() {
        if (iD3v1parsed == false) this.getID3v1();
        return v1tag != null;
    }

    /**
   * Tests if the file has an ID3v2 tag if the tag hasn't been parsed it will
   * first try to parse the tag before returning a value
   * 
   * @return
   */
    public boolean hasID3v2() {
        if (iD3v2parsed == false) this.getID3v2();
        return v2tag != null;
    }

    /**
   * @return
   */
    public boolean hasMPEGInfo() {
        return mpegI != null;
    }

    /**
   *  
   */
    public void stripTags() {
        v2tag = null;
        v1tag = null;
    }

    /**
   * @param string
   *  
   */
    public void writeToDisk(String string) {
        String newFileName = this.getFilePath();
        if (string != null) newFileName += string + System.currentTimeMillis() + ".mp3";
        try {
            FileOutputStream tag = new FileOutputStream(newFileName);
            tag.write(getData());
            tag.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public File getTheFile() {
        return theOriginal;
    }

    public MetaData getMetaData() {
        if (this.hasID3v2()) return getID3v2();
        return getID3v1();
    }

    public String getFileName() {
        return theOriginal.getName().substring(0, theOriginal.getName().lastIndexOf('.'));
    }

    public String getFileType() {
        return theOriginal.getName().substring(theOriginal.getName().lastIndexOf('.'), theOriginal.getName().length());
    }
}
