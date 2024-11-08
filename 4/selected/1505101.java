package w;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import utils.C2JUtils;

/**
 * As we know, Java can be a bit awkward when handling streams e.g. you can't
 * really skip at will without doing some nasty crud. This class helps doing
 * such crud. E.g. if we are dealing with a stream that has an underlying file,
 * we can try and skip directly by using the file channel, otherwise we can try
 * (eww) closing the stream, reopening it (ASSUMING WE KNOW THE SOURCE'S URI AND
 * TYPE), and then skipping.
 * 
 * @author Maes
 */
public class InputStreamSugar {

    public static final int UNKNOWN_TYPE = 0x0;

    public static final int FILE = 0x1;

    public static final int NETWORK_FILE = 0x2;

    public static final int ZIP_FILE = 0x4;

    public static final int BAD_URI = -1;

    /**
     * Creates an inputstream from a local file, network resource, or zipped
     * file (also over a network). If an entry name is specifid AND the type is
     * specified to be zip, then a zipentry with that name will be sought.
     * 
     * @param resource
     * @param contained
     * @param type
     * @return
     */
    public static final InputStream createInputStreamFromURI(String resource, ZipEntry entry, int type) {
        InputStream is = null;
        URL u;
        if (entry == null || !C2JUtils.flags(type, ZIP_FILE)) {
            is = getDirectInputStream(resource);
        } else {
            if (entry != null && C2JUtils.flags(type, ZIP_FILE)) {
                ZipInputStream zis;
                try {
                    u = new URL(resource);
                    zis = new ZipInputStream(u.openStream());
                } catch (Exception e) {
                    try {
                        zis = new ZipInputStream(new FileInputStream(resource));
                    } catch (Exception e1) {
                        is = getDirectInputStream(resource);
                        return is;
                    }
                }
                is = getZipEntryStream(zis, entry.getName());
                if (is != null) return is;
            }
        }
        return getDirectInputStream(resource);
    }

    /** Match zip entries in a ZipInputStream based only on their name. 
     * Luckily (?) ZipEntries do not keep references to their originating
     * streams, so opening/closing ZipInputStreams all the time won't result
     * in a garbage hell...I hope.
     * 
     * @param zis
     * @param entryname
     * @return
     */
    private static InputStream getZipEntryStream(ZipInputStream zis, String entryname) {
        ZipEntry ze = null;
        try {
            while ((ze = zis.getNextEntry()) != null) {
                if (ze.isDirectory()) continue;
                if (ze.getName().equals(entryname)) {
                    return zis;
                }
            }
        } catch (IOException e) {
            return null;
        }
        return null;
    }

    private static final InputStream getDirectInputStream(String resource) {
        InputStream is = null;
        URL u;
        try {
            u = new URL(resource);
            is = u.openStream();
        } catch (Exception e) {
            try {
                is = new FileInputStream(resource);
            } catch (FileNotFoundException e1) {
            }
        }
        return is;
    }

    /**
     * Attempt to do the Holy Grail of Java Streams, aka seek to a particular
     * position. With some types of stream, this is possible if you poke deep
     * enough. With others, it's not, and you can only close & reopen them
     * (provided you know how to do that) and then skip to a particular position
     * 
     * @param is
     * @param pos
     *        The desired position
     * @param URI
     *        Information which can help reopen a stream, e.g. a filename, URL,
     *        or zip file.
     * @peram entry If we must look into a zipfile entry
     * @return the skipped stream. Might be a totally different object.
     * @throws IOException
     */
    public static final InputStream streamSeek(InputStream is, long pos, long size, String URI, ZipEntry entry, int type) throws IOException {
        if (is == null) return is;
        if (size > 0) {
            try {
                long available = is.available();
                long guesspos = size - available;
                if (guesspos > 0 && guesspos <= pos) {
                    long skipped = 0;
                    long mustskip = pos - guesspos;
                    while (skipped < mustskip) skipped += is.skip(mustskip - skipped);
                    return is;
                }
            } catch (Exception e) {
            }
        }
        if (is instanceof FileInputStream) {
            try {
                ((FileInputStream) is).getChannel().position(pos);
                return is;
            } catch (IOException e) {
                is.close();
                is = createInputStreamFromURI(URI, null, 1);
                is.skip(pos);
                return is;
            }
        }
        if (is instanceof ZipInputStream) {
            is.close();
            is = createInputStreamFromURI(URI, entry, type);
            is.skip(pos);
            return is;
        }
        try {
            URL u = new URL(URI);
            InputStream nis = u.openStream();
            nis.skip(pos);
            is.close();
            return nis;
        } catch (Exception e) {
        }
        return is;
    }

    public static List<ZipEntry> getAllEntries(ZipInputStream zis) throws IOException {
        ArrayList<ZipEntry> zes = new ArrayList<ZipEntry>();
        ZipEntry z;
        while ((z = zis.getNextEntry()) != null) {
            zes.add(z);
        }
        return zes;
    }

    /** Attempts to return a stream size estimate. Only guaranteed to work 100% 
     * for streams representing local files, and zips (if you have the entry).
     * 
     * @param is
     * @param z
     * @return
     */
    public static long getSizeEstimate(InputStream is, ZipEntry z) {
        if (is instanceof FileInputStream) {
            try {
                return ((FileInputStream) is).getChannel().size();
            } catch (IOException e) {
            }
        }
        if (is instanceof FileInputStream) {
            if (z != null) return z.getSize();
        }
        try {
            return is.available();
        } catch (IOException e) {
            try {
                return is.available();
            } catch (IOException e1) {
                return -1;
            }
        }
    }
}
