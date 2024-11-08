package entagged.audioformats.asf;

import java.io.IOException;
import java.io.RandomAccessFile;
import entagged.audioformats.EncodingInfo;
import entagged.audioformats.Tag;
import entagged.audioformats.asf.data.AsfHeader;
import entagged.audioformats.asf.io.AsfHeaderReader;
import entagged.audioformats.asf.util.TagConverter;
import entagged.audioformats.exceptions.CannotReadException;
import entagged.audioformats.generic.AudioFileReader;

/**
 * This reader can read asf files containing any content (stream type). <br>
 * 
 * @author Christian Laireiter
 */
public class AsfFileReader extends AudioFileReader {

    /**
	 * (overridden)
	 * 
	 * @see entagged.audioformats.generic.AudioFileReader#getEncodingInfo(java.io.RandomAccessFile)
	 */
    protected EncodingInfo getEncodingInfo(RandomAccessFile raf) throws CannotReadException, IOException {
        raf.seek(0);
        EncodingInfo info = new EncodingInfo();
        try {
            AsfHeader header = AsfHeaderReader.readHeader(raf);
            if (header == null) {
                throw new CannotReadException("Some values must have been " + "incorrect for interpretation as asf with wma content.");
            }
            info.setBitrate(header.getAudioStreamChunk().getKbps());
            info.setChannelNumber((int) header.getAudioStreamChunk().getChannelCount());
            info.setEncodingType("ASF (audio): " + header.getAudioStreamChunk().getCodecDescription());
            info.setPreciseLength(header.getFileHeader().getPreciseDuration());
            info.setSamplingRate((int) header.getAudioStreamChunk().getSamplingRate());
        } catch (Exception e) {
            if (e instanceof IOException) throw (IOException) e; else if (e instanceof CannotReadException) throw (CannotReadException) e; else {
                throw new CannotReadException("Failed to read. Cause: " + e.getMessage());
            }
        }
        return info;
    }

    /**
	 * (overridden)
	 * 
	 * @see entagged.audioformats.generic.AudioFileReader#getTag(java.io.RandomAccessFile)
	 */
    protected Tag getTag(RandomAccessFile raf) throws CannotReadException, IOException {
        raf.seek(0);
        Tag tag = null;
        try {
            AsfHeader header = AsfHeaderReader.readHeader(raf);
            if (header == null) {
                throw new CannotReadException("Some values must have been " + "incorrect for interpretation as asf with wma content.");
            }
            tag = TagConverter.createTagOf(header);
        } catch (Exception e) {
            if (e instanceof IOException) throw (IOException) e; else if (e instanceof CannotReadException) throw (CannotReadException) e; else {
                throw new CannotReadException("Failed to read. Cause: " + e.getMessage());
            }
        }
        return tag;
    }
}
