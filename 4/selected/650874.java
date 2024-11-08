package mobac.program.atlascreators.impl.aqm;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import mobac.utilities.Charsets;
import mobac.utilities.Utilities;

public class FlatPackCreator {

    public static final String FLAT_PACK_HEADER = "FLATPACK1";

    public static final int FILE_COPY_BUFFER_LEN = 4096 * 10;

    private String packPath = null;

    private FileOutputStream dataStream = null;

    private ByteArrayOutputStream structBuffer = null;

    private OutputStreamWriter structBufferWriter = null;

    private long currentDataWritedSize = 0;

    private long currentNbFiles = 0;

    public FlatPackCreator(final File packPath) throws FileNotFoundException {
        this(packPath.getAbsolutePath());
    }

    public FlatPackCreator(final String packPath) throws FileNotFoundException {
        this.packPath = packPath;
        if (packPath == null) throw new NullPointerException("Pack file path is null.");
        dataStream = new FileOutputStream(packPath + ".tmp");
        structBuffer = new ByteArrayOutputStream();
        structBufferWriter = new OutputStreamWriter(structBuffer, Charsets.ISO_8859_1);
        currentDataWritedSize = 0;
        currentNbFiles = 0;
    }

    public final void add(final String filePath, final String fileEntryName) throws IOException {
        add(new File(filePath), fileEntryName);
    }

    public final void add(final File filePath) throws IOException {
        add(filePath, filePath.getName());
    }

    public final void add(final File filePath, final String fileEntryName) throws IOException {
        FileInputStream in = new FileInputStream(filePath);
        byte[] buff = new byte[(int) filePath.length()];
        int read = in.read(buff);
        in.close();
        if (filePath.length() != read) throw new IOException("Error reading '" + filePath + "'.");
        add(buff, fileEntryName);
    }

    public final void add(final byte[] buff, final String fileEntryName) throws IOException {
        if (dataStream == null) throw new IOException("Write stream is null.");
        String fileSize = Integer.toString(buff.length) + "\0";
        dataStream.write(fileSize.getBytes(Charsets.ISO_8859_1));
        if (buff.length > 0) dataStream.write(buff);
        structBufferWriter.append(fileEntryName + "\0" + currentDataWritedSize + "\0");
        currentDataWritedSize += buff.length + fileSize.length();
        currentNbFiles++;
    }

    public final void close() throws IOException {
        if (dataStream == null) throw new NullPointerException("Write stream is null.");
        dataStream.flush();
        dataStream.close();
        dataStream = null;
        File tmpFile = new File(packPath + ".tmp");
        FileOutputStream packStream = new FileOutputStream(packPath);
        try {
            String nbFiles = Long.toString(currentNbFiles) + "\0";
            packStream.write(FLAT_PACK_HEADER.getBytes(Charsets.ISO_8859_1));
            structBufferWriter.flush();
            structBufferWriter.close();
            int headerSize = structBuffer.size() + nbFiles.length();
            packStream.write(Integer.toString(headerSize).getBytes(Charsets.ISO_8859_1));
            packStream.write('\0');
            packStream.write(nbFiles.getBytes(Charsets.ISO_8859_1));
            structBuffer.writeTo(packStream);
            structBufferWriter = null;
            structBuffer = null;
            FileInputStream in = new FileInputStream(tmpFile);
            try {
                byte[] buffer = new byte[FILE_COPY_BUFFER_LEN];
                int read;
                while ((read = in.read(buffer)) > 0) packStream.write(buffer, 0, read);
                packStream.flush();
                packStream.close();
            } finally {
                Utilities.closeStream(in);
            }
        } finally {
            Utilities.closeStream(packStream);
        }
        if (tmpFile.isFile()) Utilities.deleteFile(tmpFile);
        packPath = null;
        structBuffer = null;
    }
}
