package javazoom.spi.mpeg.sampled.file;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Header;
import javazoom.spi.mpeg.sampled.file.tag.IcyInputStream;
import javazoom.spi.mpeg.sampled.file.tag.MP3Tag;
import org.tritonus.share.TDebug;
import org.tritonus.share.sampled.file.TAudioFileReader;

/**
 * This class implements AudioFileReader for MP3 SPI.
 */
public class MpegAudioFileReader extends TAudioFileReader {

    private final int SYNC = 0xFFE00000;

    private final AudioFormat.Encoding[][] sm_aEncodings = { { MpegEncoding.MPEG2L1, MpegEncoding.MPEG2L2, MpegEncoding.MPEG2L3 }, { MpegEncoding.MPEG1L1, MpegEncoding.MPEG1L2, MpegEncoding.MPEG1L3 }, { MpegEncoding.MPEG2DOT5L1, MpegEncoding.MPEG2DOT5L2, MpegEncoding.MPEG2DOT5L3 } };

    private static final int INITAL_READ_LENGTH = 64000;

    private static final int MARK_LIMIT = INITAL_READ_LENGTH + 1;

    private static final String[] id3v1genres = { "Blues", "Classic Rock", "Country", "Dance", "Disco", "Funk", "Grunge", "Hip-Hop", "Jazz", "Metal", "New Age", "Oldies", "Other", "Pop", "R&B", "Rap", "Reggae", "Rock", "Techno", "Industrial", "Alternative", "Ska", "Death Metal", "Pranks", "Soundtrack", "Euro-Techno", "Ambient", "Trip-Hop", "Vocal", "Jazz+Funk", "Fusion", "Trance", "Classical", "Instrumental", "Acid", "House", "Game", "Sound Clip", "Gospel", "Noise", "AlternRock", "Bass", "Soul", "Punk", "Space", "Meditative", "Instrumental Pop", "Instrumental Rock", "Ethnic", "Gothic", "Darkwave", "Techno-Industrial", "Electronic", "Pop-Folk", "Eurodance", "Dream", "Southern Rock", "Comedy", "Cult", "Gangsta", "Top 40", "Christian Rap", "Pop/Funk", "Jungle", "Native American", "Cabaret", "New Wave", "Psychadelic", "Rave", "Showtunes", "Trailer", "Lo-Fi", "Tribal", "Acid Punk", "Acid Jazz", "Polka", "Retro", "Musical", "Rock & Roll", "Hard Rock", "Folk", "Folk-Rock", "National Folk", "Swing", "Fast Fusion", "Bebob", "Latin", "Revival", "Celtic", "Bluegrass", "Avantgarde", "Gothic Rock", "Progressive Rock", "Psychedelic Rock", "Symphonic Rock", "Slow Rock", "Big Band", "Chorus", "Easy Listening", "Acoustic", "Humour", "Speech", "Chanson", "Opera", "Chamber Music", "Sonata", "Symphony", "Booty Brass", "Primus", "Porn Groove", "Satire", "Slow Jam", "Club", "Tango", "Samba", "Folklore", "Ballad", "Power Ballad", "Rhythmic Soul", "Freestyle", "Duet", "Punk Rock", "Drum Solo", "A Capela", "Euro-House", "Dance Hall", "Goa", "Drum & Bass", "Club-House", "Hardcore", "Terror", "Indie", "BritPop", "Negerpunk", "Polsk Punk", "Beat", "Christian Gangsta Rap", "Heavy Metal", "Black Metal", "Crossover", "Contemporary Christian", "Christian Rock", "Merengue", "Salsa", "Thrash Metal", "Anime", "JPop", "SynthPop" };

    public MpegAudioFileReader() {
        super(MARK_LIMIT, true);
        if (TDebug.TraceAudioFileReader) TDebug.out(">MpegAudioFileReader()");
    }

    /**
	 * Returns AudioFileFormat from File.
	 */
    public AudioFileFormat getAudioFileFormat(File file) throws UnsupportedAudioFileException, IOException {
        return super.getAudioFileFormat(file);
    }

    /**
	 * Returns AudioFileFormat from URL.
	 */
    public AudioFileFormat getAudioFileFormat(URL url) throws UnsupportedAudioFileException, IOException {
        if (TDebug.TraceAudioFileReader) {
            TDebug.out("MpegAudioFileReader.getAudioFileFormat(URL): begin");
        }
        long lFileLengthInBytes = AudioSystem.NOT_SPECIFIED;
        URLConnection conn = url.openConnection();
        conn.setRequestProperty("Icy-Metadata", "1");
        InputStream inputStream = conn.getInputStream();
        AudioFileFormat audioFileFormat = null;
        try {
            audioFileFormat = getAudioFileFormat(inputStream, lFileLengthInBytes);
        } finally {
            inputStream.close();
        }
        if (TDebug.TraceAudioFileReader) {
            TDebug.out("MpegAudioFileReader.getAudioFileFormat(URL): end");
        }
        return audioFileFormat;
    }

    /**
	 * Returns AudioFileFormat from inputstream and medialength.
	 */
    public AudioFileFormat getAudioFileFormat(InputStream inputStream, long mediaLength) throws UnsupportedAudioFileException, IOException {
        if (TDebug.TraceAudioFileReader) TDebug.out(">MpegAudioFileReader.getAudioFileFormat(InputStream inputStream, long mediaLength): begin");
        HashMap aff_properties = new HashMap();
        HashMap af_properties = new HashMap();
        int mLength = (int) mediaLength;
        int size = inputStream.available();
        PushbackInputStream pis = new PushbackInputStream(inputStream, MARK_LIMIT);
        byte head[] = new byte[12];
        pis.read(head);
        if (TDebug.TraceAudioFileReader) {
            TDebug.out("InputStream : " + inputStream + " =>" + new String(head));
        }
        if ((head[0] == 'R') && (head[1] == 'I') && (head[2] == 'F') && (head[3] == 'F') && (head[8] == 'W') && (head[9] == 'A') && (head[10] == 'V') && (head[11] == 'E')) {
            if (TDebug.TraceAudioFileReader) TDebug.out("WAV stream found");
            throw new UnsupportedAudioFileException("WAV stream found");
        } else if ((head[0] == '.') && (head[1] == 's') && (head[2] == 'n') && (head[3] == 'd')) {
            if (TDebug.TraceAudioFileReader) TDebug.out("AU stream found");
            throw new UnsupportedAudioFileException("AU stream found");
        } else if ((head[0] == 'F') && (head[1] == 'O') && (head[2] == 'R') && (head[3] == 'M') && (head[8] == 'A') && (head[9] == 'I') && (head[10] == 'F') && (head[11] == 'F')) {
            if (TDebug.TraceAudioFileReader) TDebug.out("AIFF stream found");
            throw new UnsupportedAudioFileException("AIFF stream found");
        } else if (((head[0] == 'I') | (head[0] == 'i')) && ((head[1] == 'C') | (head[1] == 'c')) && ((head[2] == 'Y') | (head[2] == 'y'))) {
            pis.unread(head);
            loadShoutcastInfo(pis, aff_properties);
        } else if (((head[0] == 'O') | (head[0] == 'o')) && ((head[1] == 'G') | (head[1] == 'g')) && ((head[2] == 'G') | (head[2] == 'g'))) {
            if (TDebug.TraceAudioFileReader) TDebug.out("Ogg stream found");
            throw new UnsupportedAudioFileException("Ogg stream found");
        } else {
            pis.unread(head);
        }
        int nVersion = AudioSystem.NOT_SPECIFIED;
        int nLayer = AudioSystem.NOT_SPECIFIED;
        int nSFIndex = AudioSystem.NOT_SPECIFIED;
        int nMode = AudioSystem.NOT_SPECIFIED;
        int FrameSize = AudioSystem.NOT_SPECIFIED;
        int nFrameSize = AudioSystem.NOT_SPECIFIED;
        int nFrequency = AudioSystem.NOT_SPECIFIED;
        int nTotalFrames = AudioSystem.NOT_SPECIFIED;
        float FrameRate = AudioSystem.NOT_SPECIFIED;
        int BitRate = AudioSystem.NOT_SPECIFIED;
        int nChannels = AudioSystem.NOT_SPECIFIED;
        int nHeader = AudioSystem.NOT_SPECIFIED;
        int nTotalMS = AudioSystem.NOT_SPECIFIED;
        boolean nVBR = false;
        AudioFormat.Encoding encoding = null;
        try {
            Bitstream m_bitstream = new Bitstream(pis);
            aff_properties.put("mp3.header.pos", new Integer(m_bitstream.header_pos()));
            Header m_header = m_bitstream.readFrame();
            nVersion = m_header.version();
            if (nVersion == 2) aff_properties.put("mp3.version.mpeg", Float.toString(2.5f)); else aff_properties.put("mp3.version.mpeg", Integer.toString(2 - nVersion));
            nLayer = m_header.layer();
            aff_properties.put("mp3.version.layer", Integer.toString(nLayer));
            nSFIndex = m_header.sample_frequency();
            nMode = m_header.mode();
            aff_properties.put("mp3.mode", new Integer(nMode));
            nChannels = nMode == 3 ? 1 : 2;
            aff_properties.put("mp3.channels", new Integer(nChannels));
            nVBR = m_header.vbr();
            af_properties.put("vbr", new Boolean(nVBR));
            aff_properties.put("mp3.vbr", new Boolean(nVBR));
            aff_properties.put("mp3.vbr.scale", new Integer(m_header.vbr_scale()));
            FrameSize = m_header.calculate_framesize();
            aff_properties.put("mp3.framesize.bytes", new Integer(FrameSize));
            if (FrameSize < 0) throw new UnsupportedAudioFileException("Invalid FrameSize : " + FrameSize);
            nFrequency = m_header.frequency();
            aff_properties.put("mp3.frequency.hz", new Integer(nFrequency));
            FrameRate = (float) ((1.0 / (m_header.ms_per_frame())) * 1000.0);
            aff_properties.put("mp3.framerate.fps", new Float(FrameRate));
            if (FrameRate < 0) throw new UnsupportedAudioFileException("Invalid FrameRate : " + FrameRate);
            if (mLength != AudioSystem.NOT_SPECIFIED) {
                aff_properties.put("mp3.length.bytes", new Integer(mLength));
                nTotalFrames = m_header.max_number_of_frames(mLength);
                aff_properties.put("mp3.length.frames", new Integer(nTotalFrames));
            }
            BitRate = m_header.bitrate();
            af_properties.put("bitrate", new Integer(BitRate));
            aff_properties.put("mp3.bitrate.nominal.bps", new Integer(BitRate));
            nHeader = m_header.getSyncHeader();
            encoding = sm_aEncodings[nVersion][nLayer - 1];
            aff_properties.put("mp3.version.encoding", encoding.toString());
            if (mLength != AudioSystem.NOT_SPECIFIED) {
                nTotalMS = Math.round(m_header.total_ms(mLength));
                aff_properties.put("duration", new Long((long) nTotalMS * 1000L));
            }
            aff_properties.put("mp3.copyright", new Boolean(m_header.copyright()));
            aff_properties.put("mp3.original", new Boolean(m_header.original()));
            aff_properties.put("mp3.crc", new Boolean(m_header.checksums()));
            aff_properties.put("mp3.padding", new Boolean(m_header.padding()));
            InputStream id3v2 = m_bitstream.getRawID3v2();
            if (id3v2 != null) {
                aff_properties.put("mp3.id3tag.v2", id3v2);
                parseID3v2Frames(id3v2, aff_properties);
            }
            if (TDebug.TraceAudioFileReader) TDebug.out(m_header.toString());
        } catch (Exception e) {
            if (TDebug.TraceAudioFileReader) TDebug.out("not a MPEG stream:" + e.getMessage());
            throw new UnsupportedAudioFileException("not a MPEG stream:" + e.getMessage());
        }
        int cVersion = (nHeader >> 19) & 0x3;
        if (cVersion == 1) {
            if (TDebug.TraceAudioFileReader) TDebug.out("not a MPEG stream: wrong version");
            throw new UnsupportedAudioFileException("not a MPEG stream: wrong version");
        }
        int cSFIndex = (nHeader >> 10) & 0x3;
        if (cSFIndex == 3) {
            if (TDebug.TraceAudioFileReader) TDebug.out("not a MPEG stream: wrong sampling rate");
            throw new UnsupportedAudioFileException("not a MPEG stream: wrong sampling rate");
        }
        if ((size == mediaLength) && (mediaLength != AudioSystem.NOT_SPECIFIED)) {
            FileInputStream fis = (FileInputStream) inputStream;
            byte[] id3v1 = new byte[128];
            long bytesSkipped = fis.skip(inputStream.available() - id3v1.length);
            int read = fis.read(id3v1, 0, id3v1.length);
            if ((id3v1[0] == 'T') && (id3v1[1] == 'A') && (id3v1[2] == 'G')) {
                parseID3v1Frames(id3v1, aff_properties);
            }
        }
        AudioFormat format = new MpegAudioFormat(encoding, (float) nFrequency, AudioSystem.NOT_SPECIFIED, nChannels, -1, FrameRate, true, af_properties);
        return new MpegAudioFileFormat(MpegFileFormatType.MP3, format, nTotalFrames, mLength, aff_properties);
    }

    /**
	 * Returns AudioInputStream from file.
	 */
    public AudioInputStream getAudioInputStream(File file) throws UnsupportedAudioFileException, IOException {
        if (TDebug.TraceAudioFileReader) TDebug.out("getAudioInputStream(File file)");
        InputStream inputStream = new FileInputStream(file);
        try {
            return getAudioInputStream(inputStream);
        } catch (UnsupportedAudioFileException e) {
            if (inputStream != null) inputStream.close();
            throw e;
        } catch (IOException e) {
            if (inputStream != null) inputStream.close();
            throw e;
        }
    }

    /**
	 * Returns AudioInputStream from url.
	 */
    public AudioInputStream getAudioInputStream(URL url) throws UnsupportedAudioFileException, IOException {
        if (TDebug.TraceAudioFileReader) {
            TDebug.out("MpegAudioFileReader.getAudioInputStream(URL): begin");
        }
        long lFileLengthInBytes = AudioSystem.NOT_SPECIFIED;
        URLConnection conn = url.openConnection();
        boolean isShout = false;
        int toRead = 4;
        byte[] head = new byte[toRead];
        conn.setRequestProperty("Icy-Metadata", "1");
        BufferedInputStream bInputStream = new BufferedInputStream(conn.getInputStream());
        bInputStream.mark(toRead);
        int read = bInputStream.read(head, 0, toRead);
        if ((read > 2) && (((head[0] == 'I') | (head[0] == 'i')) && ((head[1] == 'C') | (head[1] == 'c')) && ((head[2] == 'Y') | (head[2] == 'y')))) isShout = true;
        bInputStream.reset();
        InputStream inputStream = null;
        if (isShout == true) {
            IcyInputStream icyStream = new IcyInputStream(bInputStream);
            icyStream.addTagParseListener(IcyListener.getInstance());
            inputStream = icyStream;
        } else {
            String metaint = conn.getHeaderField("icy-metaint");
            if (metaint != null) {
                IcyInputStream icyStream = new IcyInputStream(bInputStream, metaint);
                icyStream.addTagParseListener(IcyListener.getInstance());
                inputStream = icyStream;
            } else {
                inputStream = bInputStream;
            }
        }
        AudioInputStream audioInputStream = null;
        try {
            audioInputStream = getAudioInputStream(inputStream, lFileLengthInBytes);
        } catch (UnsupportedAudioFileException e) {
            inputStream.close();
            throw e;
        } catch (IOException e) {
            inputStream.close();
            throw e;
        }
        if (TDebug.TraceAudioFileReader) {
            TDebug.out("MpegAudioFileReader.getAudioInputStream(URL): end");
        }
        return audioInputStream;
    }

    /**
	 * Return the AudioInputStream from the given InputStream.
	 */
    public AudioInputStream getAudioInputStream(InputStream inputStream) throws UnsupportedAudioFileException, IOException {
        if (TDebug.TraceAudioFileReader) TDebug.out("MpegAudioFileReader.getAudioInputStream(InputStream inputStream)");
        if (!inputStream.markSupported()) inputStream = new BufferedInputStream(inputStream);
        return super.getAudioInputStream(inputStream);
    }

    /**
	 * Parser ID3v1 frames
	 * @param frames
	 * @param props
	 */
    protected void parseID3v1Frames(byte[] frames, HashMap props) {
        String tag = new String(frames, 0, frames.length);
        int start = 3;
        String titlev1 = chopSubstring(tag, start, start += 30);
        String titlev2 = (String) props.get("title");
        if (((titlev2 == null) || (titlev2.length() == 0)) && (titlev1 != null)) props.put("title", titlev1);
        String artistv1 = chopSubstring(tag, start, start += 30);
        String artistv2 = (String) props.get("author");
        if (((artistv2 == null) || (artistv2.length() == 0)) && (artistv1 != null)) props.put("author", artistv1);
        String albumv1 = chopSubstring(tag, start, start += 30);
        String albumv2 = (String) props.get("album");
        if (((albumv2 == null) || (albumv2.length() == 0)) && (albumv1 != null)) props.put("album", albumv1);
        String yearv1 = chopSubstring(tag, start, start += 4);
        String yearv2 = (String) props.get("year");
        if (((yearv2 == null) || (yearv2.length() == 0)) && (yearv1 != null)) props.put("year", yearv1);
        String commentv1 = chopSubstring(tag, start, start += 28);
        String commentv2 = (String) props.get("comment");
        if (((commentv2 == null) || (commentv2.length() == 0)) && (commentv1 != null)) props.put("comment", commentv1);
        String trackv1 = "" + ((int) (frames[126] & 0xff));
        String trackv2 = (String) props.get("mp3.id3tag.track");
        if (((trackv2 == null) || (trackv2.length() == 0)) && (trackv1 != null)) props.put("mp3.id3tag.track", trackv1);
        int genrev1 = (int) (frames[127] & 0xff);
        if ((genrev1 >= 0) && (genrev1 < id3v1genres.length)) {
            String genrev2 = (String) props.get("mp3.id3tag.genre");
            if (((genrev2 == null) || (genrev2.length() == 0))) props.put("mp3.id3tag.genre", id3v1genres[genrev1]);
        }
    }

    private String chopSubstring(String s, int start, int end) {
        String str = s.substring(start, end);
        int loc = str.indexOf('\0');
        if (loc != -1) str = str.substring(0, loc);
        return str;
    }

    /**
	 * Parse ID3v2 frames to add album (TALB), title (TIT2), date (TYER), author (TPE1), copyright (TCOP), comment (COMM).
	 * @param frames
	 * @param props
	 */
    protected void parseID3v2Frames(InputStream frames, HashMap props) {
        byte[] bframes = null;
        int size = -1;
        try {
            size = frames.available();
            bframes = new byte[size];
            frames.mark(size);
            frames.read(bframes);
            frames.reset();
        } catch (IOException e) {
            if (TDebug.TraceAudioFileReader) TDebug.out("Cannot parse ID3v2 :" + e.getMessage());
        }
        try {
            String value = null;
            for (int i = 0; i < bframes.length - 4; i++) {
                String code = new String(bframes, i, 4);
                if ((code.equals("TALB")) || (code.equals("TIT2")) || (code.equals("TYER")) || (code.equals("TPE1")) || (code.equals("TCOP")) || (code.equals("COMM")) || (code.equals("TCON")) || (code.equals("TRCK"))) {
                    i = i + 10;
                    size = (int) (bframes[i - 6] << 24) + (bframes[i - 5] << 16) + (bframes[i - 4] << 8) + (bframes[i - 3]);
                    if (code.equals("COMM")) value = parseText(bframes, i, size, 5); else value = parseText(bframes, i, size, 1);
                    if ((value != null) && (value.length() > 0)) {
                        if (code.equals("TALB")) props.put("album", value); else if (code.equals("TIT2")) props.put("title", value); else if (code.equals("TYER")) props.put("date", value); else if (code.equals("TPE1")) props.put("author", value); else if (code.equals("TCOP")) props.put("copyright", value); else if (code.equals("COMM")) props.put("comment", value); else if (code.equals("TCON")) props.put("mp3.id3tag.genre", value); else if (code.equals("TRCK")) props.put("mp3.id3tag.track", value);
                    }
                    i = i + size - 1;
                }
            }
        } catch (RuntimeException e) {
            if (TDebug.TraceAudioFileReader) TDebug.out("Cannot parse ID3v2 :" + e.getMessage());
        }
    }

    /**
	 * Parse Text Frames.
	 * @param bframes
	 * @param offset
	 * @param size
	 * @param skip
	 * @return
	 */
    protected String parseText(byte[] bframes, int offset, int size, int skip) {
        String value = null;
        try {
            String[] ENC_TYPES = { "ISO-8859-1", "UTF16", "UTF-16BE", "UTF-8" };
            value = new String(bframes, offset + skip, size - skip, ENC_TYPES[bframes[offset]]);
        } catch (UnsupportedEncodingException e) {
            if (TDebug.TraceAudioFileReader) TDebug.out("ID3v2 Encoding error :" + e.getMessage());
        }
        return value;
    }

    /**
	 * Load shoutcast (ICY) info.
	 * @param input
	 * @param props
	 * @throws IOException
	 */
    protected void loadShoutcastInfo(InputStream input, HashMap props) throws IOException {
        IcyInputStream icy = new IcyInputStream(new BufferedInputStream(input));
        HashMap metadata = icy.getTagHash();
        MP3Tag titleMP3Tag = icy.getTag("icy-name");
        if (titleMP3Tag != null) props.put("title", ((String) titleMP3Tag.getValue()).trim());
        MP3Tag[] meta = icy.getTags();
        if (meta != null) {
            StringBuffer metaStr = new StringBuffer();
            for (int i = 0; i < meta.length; i++) {
                String key = meta[i].getName();
                String value = ((String) icy.getTag(key).getValue()).trim();
                props.put("mp3.shoutcast.metadata." + key, value);
            }
        }
    }
}
