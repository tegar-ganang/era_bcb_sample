package org.hyperion.fileserver;

import org.hyperion.Server;
import org.hyperion.cache.Cache;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.util.zip.CRC32;

/**
 * Handles update requests and creates a response.
 *
 * @author Graham Edgecombe
 */
public class RequestHandler {

    /**
     * The absolute path of the files directory.
     */
    public static final String FILES_DIRECTORY = new File("data/htdocs/").getAbsolutePath();

    /**
     * The cached CRC table.
     */
    private static ByteBuffer crcTable = null;

    /**
     * The cache instance.
     */
    private static Cache cache;

    /**
     * Handles a single request.
     *
     * @param request The request.
     * @return The response.
     */
    public static synchronized Response handle(Request request) {
        if (cache == null) {
            try {
                cache = new Cache(new File("./data/cache/"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        String path = request.getPath();
        if (path.equals("/")) {
            path = "/index.html";
        }
        String mime = getMimeType(path);
        try {
            if (crcTable == null) {
                crcTable = calculateCrcTable();
            }
            if (path.startsWith("/crc")) {
                return new Response(crcTable.asReadOnlyBuffer(), mime);
            } else if (path.startsWith("/title")) {
                return new Response(cache.getFile(0, 1).getBytes(), mime);
            } else if (path.startsWith("/config")) {
                return new Response(cache.getFile(0, 2).getBytes(), mime);
            } else if (path.startsWith("/interface")) {
                return new Response(cache.getFile(0, 3).getBytes(), mime);
            } else if (path.startsWith("/media")) {
                return new Response(cache.getFile(0, 4).getBytes(), mime);
            } else if (path.startsWith("/versionlist")) {
                return new Response(cache.getFile(0, 5).getBytes(), mime);
            } else if (path.startsWith("/textures")) {
                return new Response(cache.getFile(0, 6).getBytes(), mime);
            } else if (path.startsWith("/wordenc")) {
                return new Response(cache.getFile(0, 7).getBytes(), mime);
            } else if (path.startsWith("/sounds")) {
                return new Response(cache.getFile(0, 8).getBytes(), mime);
            }
            path = new File(FILES_DIRECTORY + path).getAbsolutePath();
            if (!path.startsWith(FILES_DIRECTORY)) {
                return null;
            }
            RandomAccessFile f = new RandomAccessFile(path, "r");
            try {
                MappedByteBuffer data = f.getChannel().map(MapMode.READ_ONLY, 0, f.length());
                return new Response(data, mime);
            } finally {
                f.close();
            }
        } catch (IOException ex) {
            return null;
        }
    }

    /**
     * <p>Calculates the crc table.</p>
     * <p/>
     * <p>The following code is based on research into the client (above) and
     * some forum post I found on the web archive.</p>
     *
     * @return The crc table.
     * @throws IOException if an I/O error occurs.
     */
    private static ByteBuffer calculateCrcTable() throws IOException {
        final CRC32 crc = new CRC32();
        int[] checksums = new int[9];
        checksums[0] = Server.VERSION;
        for (int i = 1; i < checksums.length; i++) {
            byte[] file = cache.getFile(0, i).getBytes();
            crc.reset();
            crc.update(file, 0, file.length);
            checksums[i] = (int) crc.getValue();
        }
        int hash = 1234;
        for (int i = 0; i < checksums.length; i++) {
            hash = (hash << 1) + checksums[i];
        }
        ByteBuffer bb = ByteBuffer.allocate(4 * (checksums.length + 1));
        for (int i = 0; i < checksums.length; i++) {
            bb.putInt(checksums[i]);
        }
        bb.putInt(hash);
        bb.flip();
        return bb;
    }

    /**
     * Gets the mime type of a file.
     *
     * @param path The path to the file.
     * @return The mime type.
     */
    private static String getMimeType(String path) {
        String mime = "application/octect-stream";
        if (path.endsWith(".htm") || path.endsWith(".html")) {
            mime = "text/html";
        } else if (path.endsWith(".jar")) {
            mime = "application/java-archive";
        }
        return mime;
    }
}
