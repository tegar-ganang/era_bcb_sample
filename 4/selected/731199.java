package org.tritonus.sampled.file;

import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import org.tritonus.share.TDebug;
import org.tritonus.share.sampled.file.TAudioOutputStream;
import org.tritonus.share.sampled.file.TDataOutputStream;

/**
 * AudioOutputStream for AU files.
 *
 * @author Florian Bomers
 * @author Matthias Pfisterer
 */
public class AuAudioOutputStream extends TAudioOutputStream {

    private static String description = "Created by Tritonus";

    /**
	* Writes a null-terminated ascii string s to f.
	* The total number of bytes written is aligned on a 2byte boundary.
	* @exception IOException Write error.
	*/
    protected static void writeText(TDataOutputStream dos, String s) throws IOException {
        if (s.length() > 0) {
            dos.writeBytes(s);
            dos.writeByte(0);
            if ((s.length() % 2) == 0) {
                dos.writeByte(0);
            }
        }
    }

    /**
	* Returns number of bytes that have to written for string s (with alignment)
	*/
    protected static int getTextLength(String s) {
        if (s.length() == 0) {
            return 0;
        } else {
            return (s.length() + 2) & 0xFFFFFFFE;
        }
    }

    public AuAudioOutputStream(AudioFormat audioFormat, long lLength, TDataOutputStream dataOutputStream) {
        super(audioFormat, lLength > 0x7FFFFFFFl ? AudioSystem.NOT_SPECIFIED : lLength, dataOutputStream, lLength == AudioSystem.NOT_SPECIFIED && dataOutputStream.supportsSeek());
    }

    protected void writeHeader() throws IOException {
        if (TDebug.TraceAudioOutputStream) {
            TDebug.out("AuAudioOutputStream.writeHeader(): called.");
        }
        AudioFormat format = getFormat();
        long lLength = getLength();
        TDataOutputStream dos = getDataOutputStream();
        if (TDebug.TraceAudioOutputStream) {
            TDebug.out("AuAudioOutputStream.writeHeader(): AudioFormat: " + format);
            TDebug.out("AuAudioOutputStream.writeHeader(): length: " + lLength);
        }
        dos.writeInt(AuTool.AU_HEADER_MAGIC);
        dos.writeInt(AuTool.DATA_OFFSET + getTextLength(description));
        dos.writeInt((lLength != AudioSystem.NOT_SPECIFIED) ? ((int) lLength) : AuTool.AUDIO_UNKNOWN_SIZE);
        dos.writeInt(AuTool.getFormatCode(format));
        dos.writeInt((int) format.getSampleRate());
        dos.writeInt(format.getChannels());
        writeText(dos, description);
    }

    protected void patchHeader() throws IOException {
        TDataOutputStream tdos = getDataOutputStream();
        tdos.seek(0);
        setLengthFromCalculatedLength();
        writeHeader();
    }
}
