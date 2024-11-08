package org.asky78.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import javax.activation.MimetypesFileTypeMap;
import lombok.Cleanup;

/**
 * 파일 매니저
 * 
 * @author 김재승
 */
public class XFileManager {

    public static boolean NEW_RECORDE = false;

    public static boolean APPEND_RECORDE = true;

    public static int BUFFER = 8096;

    public static final String ISO88591 = "8859_1";

    public static final String CP949 = "CP949";

    public static final String MS949 = "MS949";

    public static final String EUC = "EUC-KR";

    public static final String UTF8 = "UTF-8";

    public static final String KSC5601 = "KSC5601";

    private Charset charset = Charset.forName(UTF8);

    private CharsetDecoder decoder = charset.newDecoder();

    public XFileManager() {
    }

    public XFileManager changeDecoder(String charsetName) {
        this.charset = Charset.forName(charsetName);
        this.decoder = charset.newDecoder();
        return this;
    }

    public static String showMeTheFileEncoding(String filePath) throws IOException {
        @Cleanup FileInputStream fis = new FileInputStream(filePath);
        byte[] BOM = new byte[4];
        fis.read(BOM, 0, 4);
        if ((BOM[0] & 0xFF) == 0xEF && (BOM[1] & 0xFF) == 0xBB && (BOM[2] & 0xFF) == 0xBF) return ("UTF-8"); else if ((BOM[0] & 0xFF) == 0xFE && (BOM[1] & 0xFF) == 0xFF) return ("UTF-16BE"); else if ((BOM[0] & 0xFF) == 0xFF && (BOM[1] & 0xFF) == 0xFE) return ("UTF-16LE"); else if ((BOM[0] & 0xFF) == 0x00 && (BOM[1] & 0xFF) == 0x00 && (BOM[0] & 0xFF) == 0xFE && (BOM[1] & 0xFF) == 0xFF) return ("UTF-32BE"); else if ((BOM[0] & 0xFF) == 0xFF && (BOM[1] & 0xFF) == 0xFE && (BOM[0] & 0xFF) == 0x00 && (BOM[1] & 0xFF) == 0x00) return ("UTF-32LE"); else return ("EUC-KR");
    }

    public static String showMeTheFileMimeType(String filePath) throws IOException {
        return new MimetypesFileTypeMap().getContentType(filePath);
    }

    public String read(String filepath) throws IOException {
        StringBuilder result = new StringBuilder();
        @Cleanup FileChannel file_channel = new FileInputStream(new File(filepath)).getChannel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        while ((file_channel.read(buffer)) != -1) {
            buffer.flip();
            CharBuffer charBuffer = decoder.decode(buffer);
            result.append(charBuffer.toString());
            buffer.clear();
        }
        return result.toString();
    }

    public String read2(String filepath) throws IOException {
        StringBuilder result = new StringBuilder();
        @Cleanup FileChannel file_channel = new FileInputStream(new File(filepath)).getChannel();
        ByteBuffer buffer = ByteBuffer.allocate(256);
        while ((file_channel.read(buffer)) != -1) {
            buffer.flip();
            while (buffer.hasRemaining()) {
                result.append((char) buffer.get());
            }
            buffer.clear();
        }
        return result.toString();
    }

    public String readOnce(String filepath) throws IOException {
        CharBuffer charBuffer = null;
        @Cleanup FileChannel file_channel = new FileInputStream(new File(filepath)).getChannel();
        int ch_size = (int) file_channel.size();
        MappedByteBuffer mapbyte_buffer = file_channel.map(FileChannel.MapMode.READ_ONLY, 0, ch_size);
        charBuffer = decoder.decode(mapbyte_buffer);
        return charBuffer.toString();
    }

    public void copy(String source, String target) throws IOException {
        @Cleanup FileChannel sourceChannel = new FileInputStream(new File(source)).getChannel();
        @Cleanup FileChannel targetChannel = new FileOutputStream(new File(target)).getChannel();
        targetChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
    }

    public void write(String filePath, String content) throws Exception {
        write(filePath, content, false);
    }

    public void append(String filePath, String content) throws Exception {
        write(filePath, content, true);
    }

    private void write(String filePath, String content, boolean append) throws Exception {
        @Cleanup FileChannel file_channel = new FileOutputStream(new File(filePath), append).getChannel();
        ByteBuffer buffer = ByteBuffer.allocate(content.length());
        buffer.clear();
        buffer.put(content.getBytes());
        buffer.flip();
        while (buffer.hasRemaining()) {
            file_channel.write(buffer);
        }
    }

    private void writeShit(String filePath, String content, boolean append) throws Exception {
        @Cleanup FileChannel file_channel = new FileOutputStream(new File(filePath), append).getChannel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        for (int i = 0; i < content.length(); i++) {
            buffer.putChar(content.charAt(i));
        }
        buffer.flip();
        file_channel.write(buffer);
        buffer.clear();
    }

    public boolean delete(String filePath) {
        File file = new File(filePath);
        return file.delete();
    }

    private void closeAllResources(FileChannel file_channel) throws Exception {
        try {
            if (file_channel != null) file_channel.close();
        } catch (Exception e) {
            throw e;
        }
    }

    public static String getBodyByChannel(String filepath) throws FileNotFoundException, IOException {
        ByteBuffer byteBuf = ByteBuffer.allocate(500);
        StringBuilder builder = new StringBuilder();
        @Cleanup FileChannel file_channel = new FileInputStream(new File(filepath)).getChannel();
        while (file_channel.read(byteBuf) != -1) {
            byteBuf.flip();
            builder.append(byteBuf.array());
            byteBuf.clear();
        }
        return builder.toString();
    }

    public static String getBodyByBufferedReader(String filepath) throws FileNotFoundException, IOException {
        return getBodyByBufferedReader(filepath, UTF8);
    }

    public static String getBodyByBufferedReader(String filepath, String encoding) throws IOException, FileNotFoundException {
        String line = "";
        StringBuilder builder = new StringBuilder();
        @Cleanup BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filepath), encoding));
        while ((line = reader.readLine()) != null) {
            builder.append(line).append("\n");
        }
        return builder.toString();
    }

    public static String getBodyByFileInputStream(String filepath) throws IOException {
        @Cleanup FileInputStream reader = new FileInputStream(filepath);
        byte[] b = new byte[BUFFER];
        int read = 0;
        StringBuilder builder = new StringBuilder();
        while ((read = reader.read(b, 0, BUFFER)) != -1) builder.append(new String(b, 0, read));
        return builder.toString();
    }
}
