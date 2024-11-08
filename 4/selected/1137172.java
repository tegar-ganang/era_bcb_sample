package com.groovytagger.mp3.tags;

import com.groovytagger.interfaces.SourceFile;
import com.groovytagger.mp3.tags.abstracts.AbstractMp3File;
import com.groovytagger.mp3.tags.abstracts.ID3Tag;
import com.groovytagger.mp3.tags.id3v1.ID3V1;
import com.groovytagger.mp3.tags.id3v2.abstracts.ID3V2;
import com.groovytagger.mp3.tags.io.ID3DataInputStream;
import com.groovytagger.utils.GlobalApplicationStatus;
import com.groovytagger.utils.LogManager;
import com.groovytagger.utils.StaticObj;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A class representing an MP3 file.
 */
public class MP3File extends AbstractMp3File {

    /** Construct an object representing the MP3 file specified.
   *
   * @param oSourceFile a File pointing to the source MP3 file
   */
    public MP3File(File oSourceFile) {
        super(oSourceFile);
    }

    public MP3File(SourceFile oFileSource) {
        super(oFileSource);
    }

    public void syncFile() throws Exception {
        if ((id3V2Tag != null) && (!id3V2Tag.containsAtLeastOneFrame())) {
            throw new Exception("This file has an ID3 V2 tag which cannot be written because it does not contain at least one frame.");
        }
        if (id3V1Tag != null) {
            v1Sync();
        }
        if (id3V2Tag != null) {
            v2Sync();
        }
    }

    private void v1Sync() throws Exception {
        SourceFile oTmpFileSource = null;
        InputStream oSourceIS = null;
        OutputStream oTmpOS = null;
        try {
            try {
                oSourceIS = new BufferedInputStream(sourceFile.getInputStream());
            } catch (Exception e) {
                throw new Exception("Error opening [" + sourceFile.getName() + "]", e);
            }
            try {
                try {
                    oTmpFileSource = sourceFile.createTempFile("id3.", ".tmp");
                } catch (Exception e) {
                    throw new Exception("Unable to create temporary file.", e);
                }
                try {
                    oTmpOS = oTmpFileSource.getOutputStream();
                } catch (Exception e) {
                    throw new Exception("Error opening temporary file for writing.", e);
                }
                try {
                    long lFileLength = sourceFile.length();
                    byte[] abyBuffer = new byte[65536];
                    long lCopied = 0;
                    long lTotalToCopy = lFileLength - 128;
                    while (lCopied < lTotalToCopy) {
                        long lLeftToCopy = lTotalToCopy - lCopied;
                        long lToCopyNow = (lLeftToCopy >= 65536) ? 65536 : lLeftToCopy;
                        oSourceIS.read(abyBuffer, 0, (int) lToCopyNow);
                        oTmpOS.write(abyBuffer, 0, (int) lToCopyNow);
                        lCopied += lToCopyNow;
                    }
                    byte[] abyCheckTag = new byte[3];
                    oSourceIS.read(abyCheckTag);
                    if (!((abyCheckTag[0] == 'T') && (abyCheckTag[1] == 'A') && (abyCheckTag[2] == 'G'))) {
                        oTmpOS.write(abyCheckTag);
                        for (int i = 0; i < 125; i++) {
                            oTmpOS.write(oSourceIS.read());
                        }
                    }
                    id3V1Tag.write(oTmpOS);
                    oTmpOS.flush();
                } finally {
                    oTmpOS.close();
                }
            } finally {
                oSourceIS.close();
            }
            if (!sourceFile.delete()) {
                int iFails = 1;
                int iDelay = 1;
                while (!sourceFile.delete()) {
                    System.gc();
                    Thread.sleep(iDelay);
                    iFails++;
                    iDelay *= 2;
                    if (iFails > 10) {
                        throw new Exception("Unable to delete original file.");
                    }
                }
            }
            if (!oTmpFileSource.renameTo(sourceFile)) {
                throw new Exception("Unable to rename temporary file " + oTmpFileSource.toString() + " to " + sourceFile.toString() + ".");
            }
        } catch (Exception e) {
            throw new Exception("Error processing [" + sourceFile.getName() + "].", e);
        }
    }

    private void v2Sync() throws Exception {
        SourceFile oTmpFileSource = null;
        InputStream oSourceIS = null;
        OutputStream oTmpOS = null;
        id3V2Tag.sanityCheck();
        try {
            try {
                oSourceIS = new BufferedInputStream(sourceFile.getInputStream());
            } catch (Exception e) {
                throw new Exception("Error opening [" + sourceFile.getName() + "]", e);
            }
            try {
                try {
                    oTmpFileSource = sourceFile.createTempFile("id3.", ".tmp");
                } catch (Exception e) {
                    throw new Exception("Unable to create temporary file.", e);
                }
                try {
                    oTmpOS = new BufferedOutputStream(oTmpFileSource.getOutputStream());
                } catch (Exception e) {
                    throw new Exception("Error opening temporary file for writing.", e);
                }
                try {
                    id3V2Tag.write(oTmpOS);
                    byte[] abyCheckTag = new byte[3];
                    oSourceIS.read(abyCheckTag);
                    if ((abyCheckTag[0] == 'I') && (abyCheckTag[1] == 'D') && (abyCheckTag[2] == '3')) {
                        int iVersion = oSourceIS.read();
                        int iPatch = oSourceIS.read();
                        if (iVersion > 4) {
                            oTmpOS.close();
                            oTmpFileSource.delete();
                            throw new Exception("Will not overwrite tag of version greater than 2.4.0.");
                        }
                        oSourceIS.skip(1);
                        byte[] abyTagLength = new byte[4];
                        if (oSourceIS.read(abyTagLength) != 4) {
                            throw new Exception("Error reading existing ID3 tag.");
                        }
                        ID3DataInputStream oID3DIS = new ID3DataInputStream(new ByteArrayInputStream(abyTagLength));
                        long iTagLength = oID3DIS.readID3Four();
                        oID3DIS.close();
                        while (iTagLength > 0) {
                            long iNumSkipped = oSourceIS.skip(iTagLength);
                            if (iNumSkipped == 0) {
                                throw new Exception("Error reading existing ID3 tag.");
                            }
                            iTagLength -= iNumSkipped;
                        }
                    } else {
                        oTmpOS.write(abyCheckTag);
                    }
                    byte[] abyBuffer = new byte[65536];
                    int iNumRead;
                    while ((iNumRead = oSourceIS.read(abyBuffer)) != -1) {
                        oTmpOS.write(abyBuffer, 0, iNumRead);
                    }
                    oTmpOS.flush();
                } finally {
                    oTmpOS.close();
                }
            } finally {
                oSourceIS.close();
            }
            if (!sourceFile.delete()) {
                int iFails = 1;
                int iDelay = 1;
                while (!sourceFile.delete()) {
                    System.gc();
                    Thread.sleep(iDelay);
                    iFails++;
                    iDelay *= 2;
                    if (iFails > 10) {
                        throw new Exception("Unable to delete original file.");
                    }
                }
            }
            if (!oTmpFileSource.renameTo(sourceFile)) {
                throw new Exception("Unable to rename temporary file " + oTmpFileSource.toString() + " to " + sourceFile.toString() + ".");
            }
        } catch (Exception e) {
            throw new Exception("Error processing [" + sourceFile.getName() + "].", e);
        }
    }

    public ID3Tag[] getAllTags() throws Exception {
        List oID3TagList = new ArrayList();
        ID3V1 oID3V1Tag = getId3V1Tag();
        if (oID3V1Tag != null) {
            oID3TagList.add(oID3V1Tag);
        }
        ID3V2 oID3V2Tag = getId3V2Tag();
        if (oID3V2Tag != null) {
            oID3TagList.add(oID3V2Tag);
        }
        return (ID3Tag[]) oID3TagList.toArray(new ID3Tag[0]);
    }

    public ID3V1 getId3V1Tag() throws Exception {
        try {
            InputStream oSourceIS = new BufferedInputStream(sourceFile.getInputStream());
            try {
                long lFileLength = sourceFile.length();
                oSourceIS.skip(lFileLength - 128);
                byte[] abyCheckTag = new byte[3];
                oSourceIS.read(abyCheckTag);
                if ((abyCheckTag[0] == 'T') && (abyCheckTag[1] == 'A') && (abyCheckTag[2] == 'G')) {
                    ID3V1 oID3V1Tag = ID3V1.read(oSourceIS);
                    return oID3V1Tag;
                } else {
                    return null;
                }
            } finally {
                oSourceIS.close();
            }
        } catch (Exception e) {
            LogManager.getInstance().getLogger().error(e);
            if (StaticObj.DEBUG) e.printStackTrace();
            throw new Exception(e);
        }
    }

    public ID3V2 getId3V2Tag() throws Exception {
        try {
            InputStream oSourceIS = new BufferedInputStream(sourceFile.getInputStream());
            ID3DataInputStream oSourceID3DIS = new ID3DataInputStream(oSourceIS);
            try {
                byte[] abyCheckTag = new byte[3];
                oSourceID3DIS.readFully(abyCheckTag);
                if ((abyCheckTag[0] == 'I') && (abyCheckTag[1] == 'D') && (abyCheckTag[2] == '3')) {
                    return ID3V2.read(oSourceID3DIS);
                } else {
                    return null;
                }
            } finally {
                oSourceID3DIS.close();
            }
        } catch (Exception e) {
            throw new Exception("Error reading tags from file.", e);
        }
    }

    public void removeTags() throws Exception {
        removeID3V1Tag();
        removeID3V2Tag();
    }

    public void removeID3V1Tag() throws Exception {
        SourceFile oTmpFileSource = null;
        InputStream oSourceIS = null;
        OutputStream oTmpOS = null;
        try {
            try {
                oSourceIS = new BufferedInputStream(sourceFile.getInputStream());
            } catch (Exception e) {
                throw new Exception("Error opening [" + sourceFile.getName() + "]", e);
            }
            try {
                try {
                    oTmpFileSource = sourceFile.createTempFile("id3.", ".tmp");
                } catch (Exception e) {
                    throw new Exception("Unable to create temporary file.", e);
                }
                try {
                    oTmpOS = new BufferedOutputStream(oTmpFileSource.getOutputStream());
                } catch (Exception e) {
                    throw new Exception("Error opening temporary file for writing.", e);
                }
                try {
                    long lFileLength = sourceFile.length();
                    byte[] abyBuffer = new byte[65536];
                    long lCopied = 0;
                    long lTotalToCopy = lFileLength - 128;
                    while (lCopied < lTotalToCopy) {
                        long lLeftToCopy = lTotalToCopy - lCopied;
                        long lToCopyNow = (lLeftToCopy >= 65536) ? 65536 : lLeftToCopy;
                        oSourceIS.read(abyBuffer, 0, (int) lToCopyNow);
                        oTmpOS.write(abyBuffer, 0, (int) lToCopyNow);
                        lCopied += lToCopyNow;
                    }
                    byte[] abyCheckTag = new byte[3];
                    oSourceIS.read(abyCheckTag);
                    if (!((abyCheckTag[0] == 'T') && (abyCheckTag[1] == 'A') && (abyCheckTag[2] == 'G'))) {
                        oTmpOS.write(abyCheckTag);
                        for (int i = 0; i < 125; i++) {
                            oTmpOS.write(oSourceIS.read());
                        }
                    }
                    oTmpOS.flush();
                } finally {
                    oTmpOS.close();
                }
            } finally {
                oSourceIS.close();
            }
            if (!sourceFile.delete()) {
                int iFails = 1;
                int iDelay = 1;
                while (!sourceFile.delete()) {
                    System.gc();
                    Thread.sleep(iDelay);
                    iFails++;
                    iDelay *= 2;
                    if (iFails > 10) {
                        throw new Exception("Unable to delete original file.");
                    }
                }
            }
            if (!oTmpFileSource.renameTo(sourceFile)) {
                throw new Exception("Unable to rename temporary file " + oTmpFileSource.toString() + " to " + sourceFile.toString() + ".");
            }
        } catch (Exception e) {
            throw new Exception("Error processing [" + sourceFile.getName() + "].", e);
        }
    }

    public void removeID3V2Tag() throws Exception {
        SourceFile oTmpFileSource = null;
        InputStream oSourceIS = null;
        OutputStream oTmpOS = null;
        try {
            oTmpFileSource = sourceFile.createTempFile("id3.", ".tmp");
        } catch (Exception e) {
            throw new Exception("Unable to create temporary file.", e);
        }
        try {
            try {
                oSourceIS = new BufferedInputStream(sourceFile.getInputStream());
            } catch (Exception e) {
                throw new Exception("Error opening [" + sourceFile.getName() + "]", e);
            }
            try {
                try {
                    oTmpOS = new BufferedOutputStream(oTmpFileSource.getOutputStream());
                } catch (Exception e) {
                    throw new Exception("Error opening temporary file for writing.", e);
                }
                try {
                    byte[] abyCheckTag = new byte[3];
                    oSourceIS.read(abyCheckTag);
                    if ((abyCheckTag[0] == 'I') && (abyCheckTag[1] == 'D') && (abyCheckTag[2] == '3')) {
                        oSourceIS.skip(1);
                        byte[] abyTagLength = new byte[4];
                        if (oSourceIS.read(abyTagLength) != 4) {
                            throw new Exception("Error reading existing ID3 tags.");
                        }
                        ID3DataInputStream oID3DIS = new ID3DataInputStream(new ByteArrayInputStream(abyTagLength));
                        long iTagLength = oID3DIS.readID3Four();
                        oID3DIS.close();
                        while (iTagLength > 0) {
                            long iNumSkipped = oSourceIS.skip(iTagLength);
                            if (iNumSkipped == 0) {
                                throw new Exception("Error reading existing ID3 tag.");
                            }
                            iTagLength -= iNumSkipped;
                        }
                    } else {
                        oTmpOS.write(abyCheckTag);
                    }
                    byte[] abyBuffer = new byte[65536];
                    int iNumRead;
                    while ((iNumRead = oSourceIS.read(abyBuffer)) != -1) {
                        oTmpOS.write(abyBuffer, 0, iNumRead);
                    }
                    oTmpOS.flush();
                } finally {
                    oTmpOS.close();
                }
            } finally {
                oSourceIS.close();
            }
            if (!sourceFile.delete()) {
                int iFails = 1;
                int iDelay = 1;
                while (!sourceFile.delete()) {
                    System.gc();
                    Thread.sleep(iDelay);
                    iFails++;
                    iDelay *= 2;
                    if (iFails > 10) {
                        throw new Exception("Unable to delete original file.");
                    }
                }
            }
            if (!oTmpFileSource.renameTo(sourceFile)) {
                throw new Exception("Unable to rename temporary file " + oTmpFileSource.toString() + " to " + sourceFile.toString() + ".");
            }
        } catch (Exception e) {
            throw new Exception("Error processing [" + sourceFile.getName() + "].", e);
        }
    }
}
