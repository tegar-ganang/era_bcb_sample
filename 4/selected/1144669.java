package randres.kindle.previewer;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import randres.kindle.model.FileItem;
import randres.kindle.previewer.huffmandec.HuffmanDecoder;
import randres.kindle.previewer.jfif.JFIFViewer;
import randres.kindle.previewer.util.MyRandomAccessFile;

public class PRCPreViewer {

    private static final int PRC_HEADER_LENGTH = 78;

    private static final int PRC_RECORD_LENGTH = 8;

    private static final Object AUTHOR_KEY = 100;

    private static final Object ENCODING_KEY = 600;

    private static final Object LOCALE_KEY = 610;

    private static final Object VERSION_KEY = 620;

    private String type;

    private String id;

    private String title;

    private long creationTimeLong;

    private ReadableByteChannel channel;

    private Hashtable extensions;

    private Hashtable recordMap;

    private Vector<HuffmanSection> huffmanTable;

    private boolean debug = false;

    private String m_encoding = "UTF-8";

    private int coverOffset;

    private int imageSection = -1;

    private int fullNameOffset = -1;

    private int fullNameLength = -1;

    public PRCPreViewer(String filename) throws Exception {
        File file = new File(filename);
        FileInputStream fin = new FileInputStream(file);
        channel = fin.getChannel();
        extensions = new Hashtable();
        recordMap = new Hashtable();
        huffmanTable = new Vector<HuffmanSection>();
        int numRecords = readPRCHeader();
        readRecords(numRecords);
        readMOBIHeader();
        readEXTHeader();
        channel.close();
        fin.close();
        if (fullNameOffset != -1) {
            title = getFullname(file, (Integer) recordMap.get(0), fullNameOffset, fullNameLength, m_encoding);
            System.out.println(title);
        }
    }

    public static String getFullname(File file, int firstRecordOffset, int fullNameOffset, int fullNameLength, String charsetName) throws Exception {
        MyRandomAccessFile fileAccessor = new MyRandomAccessFile(file);
        byte[] fullNameSection = new byte[fullNameLength];
        fileAccessor.read(fullNameSection, firstRecordOffset + fullNameOffset, fullNameLength);
        String fullName = new String(fullNameSection, Charset.forName(charsetName));
        fileAccessor.close();
        return fullName;
    }

    public static void storeImage(File file, int imageSectionOffset, int endImageSectionOffset, String folder) throws Exception {
        int length = endImageSectionOffset - imageSectionOffset;
        MyRandomAccessFile fileAccessor = new MyRandomAccessFile(file);
        byte[] headerSection = new byte[length];
        fileAccessor.read(headerSection, imageSectionOffset, length);
        BufferedImage buf = ImageIO.read(new ByteArrayInputStream(headerSection));
        String hash = FileItem.calculateSHA1(file.getName());
        ImageIO.write(buf, "jpeg", new File(folder + File.separator + hash + ".jpg"));
        fileAccessor.close();
    }

    private void readRecords(int numRecords) throws Exception {
        debug("nrecords " + numRecords);
        ByteBuffer records = ByteBuffer.allocateDirect((numRecords * PRC_RECORD_LENGTH));
        channel.read(records);
        records.rewind();
        for (int i = 0; i < numRecords && records.hasRemaining(); i++) {
            int recordOffset = records.getInt();
            int recordId = records.getInt();
            recordMap.put(recordId, recordOffset);
        }
        channel.read(ByteBuffer.allocate(2));
        ByteBuffer palmDocHeader = ByteBuffer.allocateDirect(16);
        int readed = channel.read(palmDocHeader);
        palmDocHeader.rewind();
        int compression = palmDocHeader.getShort();
        int unused = palmDocHeader.getShort();
        int textLength = palmDocHeader.getInt();
        int recordCount = palmDocHeader.getShort();
        int recordSize = palmDocHeader.getShort();
        int currentPosition = palmDocHeader.getInt();
        debug(String.format(" Compression: %d  (1 == no compression, 2 = PalmDOC compression, 17480 = HUFF/CDIC compression) \n" + " Text total length: %d bytes  \n" + " Text records: %d (Record size %d)", compression, textLength, recordCount, recordSize));
    }

    private void readMOBIHeader() throws Exception {
        ByteBuffer prcHeaderBB = ByteBuffer.allocateDirect(8);
        channel.read(prcHeaderBB);
        prcHeaderBB.rewind();
        String name = getString(prcHeaderBB, 4);
        int length = prcHeaderBB.getInt();
        ByteBuffer header = ByteBuffer.allocateDirect(length - 8);
        channel.read(header);
        header.rewind();
        int mobyType = header.getInt();
        int textEncoding = header.getInt();
        int unique_id = header.getInt();
        int version = header.getInt();
        String reserved = getString(header, 40);
        m_encoding = TextEncodings.getTextEncoding(textEncoding).toString();
        extensions.put(ENCODING_KEY, m_encoding);
        extensions.put(VERSION_KEY, version);
        int firstNonBookIndex = header.getInt();
        debug("First not text record " + firstNonBookIndex);
        fullNameOffset = header.getInt();
        fullNameLength = header.getInt();
        debug("FNO " + String.format(" %d (x%04x)", fullNameOffset, fullNameOffset));
        int locale = header.getInt();
        extensions.put(LOCALE_KEY, locale);
        int inputLanguage = header.getInt();
        int outputLanguage = header.getInt();
        int minVersion = header.getInt();
        int firstImageIndex = header.getInt();
        int huffmanRecordOffset = header.getInt();
        int huffmanRecordCount = header.getInt();
        imageSection = firstImageIndex;
        debug(String.format(" HuffRecOff %d (Count %d)  ==> abs byte %x ", huffmanRecordOffset, huffmanRecordCount, recordMap.get(huffmanRecordOffset + 2)));
        for (int i = 0; i < huffmanRecordCount; i++) {
            int offset = (Integer) recordMap.get(huffmanRecordOffset + i);
            int offsetNext = (Integer) recordMap.get(huffmanRecordOffset + i + 1);
            HuffmanSection hs = new HuffmanSection(offset, (offsetNext - offset));
            huffmanTable.add(hs);
        }
    }

    private void readEXTHeader() throws Exception {
        ByteBuffer prcHeaderBB = ByteBuffer.allocateDirect(8);
        channel.read(prcHeaderBB);
        prcHeaderBB.rewind();
        String name = getString(prcHeaderBB, 4);
        int length = prcHeaderBB.getInt();
        ByteBuffer header = ByteBuffer.allocateDirect(length - 8);
        channel.read(header);
        header.rewind();
        int recordCount = header.getInt();
        int remaining = length - 12;
        for (int i = 0; i < recordCount; i++) {
            int recordType = header.getInt();
            int recordLength = header.getInt();
            if (recordType == 201) {
                coverOffset = header.getInt();
            } else {
                String data = getString(header, recordLength - 8);
                extensions.put(recordType, data);
            }
            remaining -= recordLength;
        }
        debug("Remaining " + remaining);
    }

    private int readPRCHeader() throws IOException {
        ByteBuffer prcHeaderBB = ByteBuffer.allocateDirect(PRC_HEADER_LENGTH);
        channel.read(prcHeaderBB);
        prcHeaderBB.rewind();
        title = getString(prcHeaderBB, 32);
        int flags = prcHeaderBB.getShort();
        int version = prcHeaderBB.getShort();
        creationTimeLong = (prcHeaderBB.getInt() - 2082852) * 1000L;
        long modificationTimeLong = (prcHeaderBB.getInt() - 2082852) * 1000L;
        long backupTimeLong = (prcHeaderBB.getInt() - 2082852) * 1000L;
        int mod_num = prcHeaderBB.getInt();
        int app_info = prcHeaderBB.getInt();
        int sort_info = prcHeaderBB.getInt();
        type = getString(prcHeaderBB, 4);
        id = getString(prcHeaderBB, 4);
        int id_seed = prcHeaderBB.getInt();
        int next_record_list = prcHeaderBB.getInt();
        int num_records = prcHeaderBB.getShort();
        return num_records;
    }

    public String getString(ByteBuffer bb, int length) {
        byte[] array = new byte[length];
        for (int i = 0; i < length; i++) {
            array[i] = bb.get();
        }
        String s = "";
        try {
            s = new String(array, m_encoding);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return correctName(s);
    }

    public static String getStringStatic(ByteBuffer bb, int length) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < length; i++) {
            byte byteReaded = bb.get();
            try {
                if (byteReaded != 0 && byteReaded != (byte) 0xff) {
                    sb.append(String.format("%c", byteReaded));
                }
            } catch (Exception e) {
                sb.append("?");
            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(String.format("Type: %s \n", type));
        sb.append(String.format("Id: %s \n", id));
        sb.append(String.format("Version: %s \n", extensions.get(VERSION_KEY)));
        sb.append(String.format("Title: %s \n", title));
        if (extensions.containsKey(AUTHOR_KEY)) {
            sb.append(String.format("Author: %s \n", extensions.get(AUTHOR_KEY)));
        }
        sb.append(String.format("Locale: %s \n", extensions.get(LOCALE_KEY)));
        sb.append(String.format("Encoding: %s \n", extensions.get(ENCODING_KEY)));
        sb.append(String.format("Creation Date: %s \n", new Date(creationTimeLong)));
        return sb.toString();
    }

    public PreviewInfo getPreviewInfo() {
        PreviewInfo info = new PreviewInfo();
        info.setTitle(title);
        info.setAuthor((String) extensions.get(AUTHOR_KEY));
        info.setResume(toHTMLString());
        info.setData(PreviewInfo.COVER_IMAGE_OFFSET, ((Integer) recordMap.get(imageSection + coverOffset)).toString());
        info.setData(PreviewInfo.COVER_IMAGE_END, ((Integer) recordMap.get(imageSection + coverOffset + 1)).toString());
        return info;
    }

    public String toHTMLString() {
        StringBuffer sb = new StringBuffer();
        sb.append(String.format("<b>Type:</b> %s <br>", type));
        sb.append(String.format("<b>Id:</b> %s <br>", id));
        sb.append(String.format("<b>Version:</b> %s <br>", extensions.get(VERSION_KEY)));
        sb.append(String.format("<b>Title:</b> %s <br>", title));
        if (extensions.containsKey(AUTHOR_KEY)) {
            sb.append(String.format("<b>Author:</b> %s <br>", extensions.get(AUTHOR_KEY)));
        }
        sb.append(String.format("<b>Locale:</b> %s <br>", extensions.get(LOCALE_KEY)));
        sb.append(String.format("<b>Encoding:</b> %s <br>", extensions.get(ENCODING_KEY)));
        sb.append(String.format("<b>Creation Date:</b> %s <br>", new Date(creationTimeLong)));
        return sb.toString();
    }

    public static void debugByteSection(byte[] stream) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < stream.length; i++) {
            if (i % 8 == 0 && i > 0) {
                System.out.println(sb.toString());
                sb = new StringBuffer();
            }
            sb.append(String.format(" %02x ", stream[i]));
        }
    }

    public void debug(String mssg) {
        if (debug) {
            System.out.println(mssg);
        }
    }

    class HuffmanSection {

        int offset;

        int length;

        public HuffmanSection(int offset, int length) {
            this.offset = offset;
            this.length = length;
        }

        public int getOffset() {
            return offset;
        }

        public int getLength() {
            return length;
        }
    }

    public static String correctName(String name) {
        name = name.replace("_233_", "e");
        name = name.replace("_237_", "i");
        name = name.replace("_250_", "u");
        name = name.replace("_225_", "a");
        name = name.replace("_243_", "o");
        name = name.replace("_241_", "ñ");
        name = name.replace("_193_", "A");
        return name.toLowerCase();
    }

    public static void processDir(Writer writer, File path) {
        try {
            for (File file : path.listFiles()) {
                String filename = file.getAbsolutePath();
                if (file.isDirectory()) {
                    System.out.println("Processing " + filename);
                    processDir(writer, file);
                } else if (filename.endsWith(".prc") || filename.endsWith(".mobi")) {
                    try {
                        PRCPreViewer preview = new PRCPreViewer(filename);
                        String record = String.format("%s\t%s\t%s\n", filename, correctName((String) preview.extensions.get(AUTHOR_KEY)), correctName(preview.title));
                        writer.append(record);
                        writer.flush();
                    } catch (Exception e) {
                        System.out.println("Fail: " + filename + " " + e.getMessage());
                    }
                } else {
                    System.out.println("Skipped: " + filename);
                }
            }
        } catch (Exception e) {
            System.out.println("Fail directory: " + path.getAbsolutePath() + " " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        String filename = "/home/randres/Escritorio/kindle/books/15.mobi";
        try {
            PRCPreViewer preview = new PRCPreViewer(filename);
            System.out.println(preview);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
