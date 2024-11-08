import java.io.FileInputStream;
import java.nio.ByteOrder;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class DcmFileReader {

    VRParser m_vrParser;

    public DcmFileReader(String filename) {
        ByteBuffer buffer = getByteBuffer(filename);
        m_vrParser = VRParser.create(buffer);
        m_vrParser.parsePicture(buffer, 132);
    }

    private ByteBuffer getByteBuffer(String filename) {
        ByteBuffer buffer = null;
        try {
            long filesize = (new File(filename)).length();
            System.out.println("FileSize=" + filesize);
            final FileInputStream fis = new FileInputStream(filename);
            FileChannel fc = fis.getChannel();
            buffer = ByteBuffer.allocate((int) filesize);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            int bytesRead = fc.read(buffer);
            System.out.println("bytesRead=" + bytesRead);
        } catch (Exception e) {
            String msg = e.getMessage();
            if (null != msg) System.out.println(msg);
            System.out.println(e.getStackTrace());
        }
        return buffer;
    }
}
