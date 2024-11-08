package gdromImage;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;

public class cdi {

    public static final int CDI_V2_ID = 0x80000004;

    public static final int CDI_V3_ID = 0x80000005;

    public static final int CDI_V35_ID = 0x80000006;

    private final ByteBuffer cdi_trailer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);

    private final IntBuffer cdi_trailer_fields = cdi_trailer.asIntBuffer();

    private FileChannel fileChannel;

    private static final byte TRACK_START_MARKER[] = { 0, 0, 1, 0, 0, 0, (byte) 255, (byte) 255, (byte) 255, (byte) 255, 0, 0, 1, 0, 0, 0, (byte) 255, (byte) 255, (byte) 255, (byte) 255 };

    private static final byte EXT_MARKER[] = { 0, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255 };

    private RandomAccessFile read;

    private long length = 0;

    private int version;

    private long header_offset;

    public cdi(String file) throws FileNotFoundException {
        read = new RandomAccessFile(file, "r");
        try {
            length = read.length();
            fileChannel = read.getChannel();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean cdi_is_valid() throws IOException {
        read.seek(length - 8);
        fileChannel.read(cdi_trailer);
        version = cdi_trailer_fields.get(0);
        header_offset = cdi_trailer_fields.get(0);
        if (header_offset >= length || header_offset == 0) return false;
        return version == CDI_V2_ID || version == CDI_V3_ID || version == CDI_V35_ID;
    }

    class cdi_track {

        private static final int pregap_length = 0;

        private static final int length = 4;

        private static final int mode = 13;

        private static final int start_lba = 24;

        private static final int total_length = 28;

        private static final int sector_size = 44;

        private ByteBuffer trackInfo;

        public cdi_track() {
            trackInfo = ByteBuffer.allocate(87);
        }
    }
}
