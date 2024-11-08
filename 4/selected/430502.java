package org.tritonus.sampled.file;

import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;
import org.tritonus.share.TDebug;
import org.tritonus.share.sampled.file.TAudioOutputStream;
import org.tritonus.share.sampled.file.TDataOutputStream;

/**
 * AudioOutputStream for AIFF and AIFF-C files.
 *
 * @author Florian Bomers
 */
public class AiffAudioOutputStream extends TAudioOutputStream {

    private static final int LENGTH_NOT_KNOWN = -1;

    private AudioFileFormat.Type m_FileType;

    public AiffAudioOutputStream(AudioFormat audioFormat, AudioFileFormat.Type fileType, long lLength, TDataOutputStream dataOutputStream) {
        super(audioFormat, lLength, dataOutputStream, lLength == AudioSystem.NOT_SPECIFIED && dataOutputStream.supportsSeek());
        if (lLength != AudioSystem.NOT_SPECIFIED && lLength > 0x7FFFFFFFl) {
            throw new IllegalArgumentException("AIFF files cannot be larger than 2GB.");
        }
        m_FileType = fileType;
        if (!audioFormat.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED) && !audioFormat.getEncoding().equals(AudioFormat.Encoding.PCM_UNSIGNED)) {
            m_FileType = AudioFileFormat.Type.AIFC;
        }
    }

    protected void writeHeader() throws IOException {
        if (TDebug.TraceAudioOutputStream) {
            TDebug.out("AiffAudioOutputStream.writeHeader(): called.");
        }
        AudioFormat format = getFormat();
        boolean bIsAifc = m_FileType.equals(AudioFileFormat.Type.AIFC);
        long lLength = getLength();
        TDataOutputStream dos = getDataOutputStream();
        int nCommChunkSize = 18;
        int nFormatCode = AiffTool.getFormatCode(format);
        if (bIsAifc) {
            nCommChunkSize += 6;
        }
        int nHeaderSize = 4 + 8 + nCommChunkSize + 8;
        if (bIsAifc) {
            nHeaderSize += 12;
        }
        if (lLength != AudioSystem.NOT_SPECIFIED && lLength + nHeaderSize > 0x7FFFFFFFl) {
            lLength = 0x7FFFFFFFl - nHeaderSize;
        }
        long lSSndChunkSize = (lLength != AudioSystem.NOT_SPECIFIED) ? (lLength + (lLength % 2) + 8) : AudioSystem.NOT_SPECIFIED;
        dos.writeInt(AiffTool.AIFF_FORM_MAGIC);
        dos.writeInt((lLength != AudioSystem.NOT_SPECIFIED) ? ((int) (lSSndChunkSize + nHeaderSize)) : LENGTH_NOT_KNOWN);
        if (bIsAifc) {
            dos.writeInt(AiffTool.AIFF_AIFC_MAGIC);
            dos.writeInt(AiffTool.AIFF_FVER_MAGIC);
            dos.writeInt(4);
            dos.writeInt(AiffTool.AIFF_FVER_TIME_STAMP);
        } else {
            dos.writeInt(AiffTool.AIFF_AIFF_MAGIC);
        }
        dos.writeInt(AiffTool.AIFF_COMM_MAGIC);
        dos.writeInt(nCommChunkSize);
        dos.writeShort((short) format.getChannels());
        dos.writeInt((lLength != AudioSystem.NOT_SPECIFIED) ? ((int) (lLength / format.getFrameSize())) : LENGTH_NOT_KNOWN);
        if (nFormatCode == AiffTool.AIFF_COMM_ULAW) {
            dos.writeShort(16);
        } else {
            dos.writeShort((short) format.getSampleSizeInBits());
        }
        writeIeeeExtended(dos, format.getSampleRate());
        if (bIsAifc) {
            dos.writeInt(nFormatCode);
            dos.writeShort(0);
        }
        dos.writeInt(AiffTool.AIFF_SSND_MAGIC);
        dos.writeInt((lLength != AudioSystem.NOT_SPECIFIED) ? ((int) (lLength + 8)) : LENGTH_NOT_KNOWN);
        dos.writeInt(0);
        dos.writeInt(0);
    }

    protected void patchHeader() throws IOException {
        TDataOutputStream tdos = getDataOutputStream();
        tdos.seek(0);
        setLengthFromCalculatedLength();
        writeHeader();
    }

    public void close() throws IOException {
        long nBytesWritten = getCalculatedLength();
        if ((nBytesWritten % 2) == 1) {
            if (TDebug.TraceAudioOutputStream) {
                TDebug.out("AiffOutputStream.close(): adding padding byte");
            }
            TDataOutputStream tdos = getDataOutputStream();
            tdos.writeByte(0);
        }
        super.close();
    }

    public void writeIeeeExtended(TDataOutputStream dos, float sampleRate) throws IOException {
        int nSampleRate = (int) sampleRate;
        short ieeeExponent = 0;
        while ((nSampleRate != 0) && (nSampleRate & 0x80000000) == 0) {
            ieeeExponent++;
            nSampleRate <<= 1;
        }
        dos.writeShort(16414 - ieeeExponent);
        dos.writeInt(nSampleRate);
        dos.writeInt(0);
    }
}
