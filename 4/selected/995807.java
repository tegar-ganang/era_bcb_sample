package chat.client.tool;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;

@Deprecated
class FileSendtest {

    /**
	 * @param args
	 * @throws IOException
	 */
    public static void main(final String[] args) throws IOException {
        final long start = System.currentTimeMillis();
        final File f = new File("T:/Games/Stronghold Crusader.7z");
        final FileInputStream fis = new FileInputStream(f);
        final FileChannel fc = fis.getChannel();
        final ByteBuffer bb = ByteBuffer.allocate(4096);
        final SocketChannel sc = SocketChannel.open(new InetSocketAddress(InetAddress.getByName(null), 1440));
        final long middle = System.currentTimeMillis();
        while (fc.read(bb) != -1) {
            bb.compact();
            sc.write(bb);
            bb.clear();
        }
        fc.close();
        sc.close();
        final long end = System.currentTimeMillis();
        System.out.println("Vorbereitung: " + (middle - start));
        System.out.println("Lesen/Senden: " + (end - middle));
        System.out.println("Gesamt: " + (end - start));
    }
}
