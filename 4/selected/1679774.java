package jreceiver.server.content;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import jreceiver.common.rec.source.FtuneRec;
import jreceiver.common.rec.source.Playlist;
import jreceiver.common.rec.source.Source;
import jreceiver.common.rec.source.Tune;
import jreceiver.server.bus.BusException;
import jreceiver.server.bus.MimeBus;
import jreceiver.server.bus.PlaylistBus;
import jreceiver.server.bus.SourceBus;
import jreceiver.server.bus.TuneBus;
import jreceiver.server.stream.capture.MusicInputStreamFactory;
import jreceiver.server.util.playlist.PlaylistWriter;
import jreceiver.util.ExpiringWriter;
import jreceiver.util.HelperFile;
import jreceiver.util.HelperServlet;

/**
 * JRec Content Server
 * <p>
 *
 * @author Reed Esau
 * @version $Revision: 1.29 $ $Date: 2003/05/08 04:52:58 $
 */
public final class ContentEngine extends HttpServlet {

    /**
     */
    public void init() throws ServletException {
    }

    /**
     * The get method of the engine will be our raw MPEG server, for
     * the protocol convertors, for streaming to the browser, and finally
     * even to the devices themselves.
     */
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (log.isDebugEnabled()) log.debug("GET: " + " " + HelperServlet.dumpHeader(req) + "\n" + HelperServlet.dumpParams(req));
        String rnd_str = null;
        String pathinfo = req.getPathInfo();
        String faux_name = null;
        if (pathinfo != null) {
            StringTokenizer st = new StringTokenizer(pathinfo, "/");
            while (st.hasMoreTokens()) {
                String tok = (String) st.nextToken();
                if (tok.length() == 0) continue;
                if (rnd_str == null) {
                    rnd_str = tok;
                } else {
                    faux_name = tok;
                    break;
                }
            }
        }
        if (log.isDebugEnabled()) log.debug("GET: rnd_str=" + rnd_str + " faux_name=" + faux_name);
        try {
            Source source_rec = getSourceRec(rnd_str);
            if (source_rec.getIsTune()) {
                MimeBus mime_bus = MimeBus.getInstance();
                String file_ext = null;
                if (faux_name != null) file_ext = HelperFile.getFileExtension(faux_name);
                if (file_ext != null && mime_bus.getIsPlaylistFileExtension(file_ext)) {
                    handleTuneAsPlaylist((Tune) source_rec, file_ext, resp);
                } else handleTune((Tune) source_rec, req, resp);
            } else if (source_rec.getIsPlaylist()) {
                handlePlaylist((Playlist) source_rec, req, resp);
            } else throw new ServletException("not recognized");
        } catch (InstantiationException e) {
            throw new ServletException("object-creation-problem serving content", e);
        } catch (BusException e) {
            throw new ServletException("bus-problem serving content", e);
        }
    }

    /**
     * Attempt to obtain details on the content source cached in
     * the session.
     * <p>
     * If none or if the src_id has changed, fetch new details
     * for the src_id and store in the session.
     */
    private Source getSourceRec(String rnd_str) throws ServletException, BusException {
        if (log.isDebugEnabled()) log.debug("getSourceRec: rnd_str=" + rnd_str);
        ContentSecurityCache security_cache = ContentSecurityCache.getInstance();
        Integer i_src_id = security_cache.get(rnd_str);
        if (i_src_id == null) throw new ServletException("encoded src_id not recognized or expired");
        int src_id = i_src_id.intValue();
        if (src_id < Source.MIN_SRC_ID) throw new ServletException("invalid src_id in security cache");
        ContentSourceCache source_cache = ContentSourceCache.getInstance();
        Source source = source_cache.get(src_id);
        if (source == null) {
            if (log.isDebugEnabled()) log.debug("getSourceRec: retrieving source_rec for src_id=" + src_id);
            SourceBus src_bus = SourceBus.getInstance();
            Source simple_source = (Source) src_bus.getRec(src_id, null);
            if (log.isDebugEnabled()) log.debug("getSourceRec: source=" + simple_source);
            if (simple_source.getIsTune()) {
                TuneBus tune_bus = TuneBus.getInstance();
                source = (Source) tune_bus.getRec(src_id, GET_TUNE_ARGS);
            } else if (simple_source.getIsPlaylist()) {
                PlaylistBus pl_bus = PlaylistBus.getInstance();
                source = (Source) pl_bus.getRec(src_id, GET_PLAYLIST_ARGS);
            } else throw new ServletException("not recognized");
            if (log.isDebugEnabled()) log.debug("getSourceRec: retrieved source=" + source);
            source_cache.put(src_id, source);
        }
        return source;
    }

    /**
     * send the tune as an ad-hoc playlist with one entry
     */
    private void handleTuneAsPlaylist(Tune tune, String file_ext, HttpServletResponse resp) throws ServletException, IOException, InstantiationException, BusException {
        tune.setContentURL(getContentURL(tune.getSrcId()));
        if (log.isDebugEnabled()) log.debug("handleTuneAsPlaylist: " + tune.getContentURL());
        MimeBus mime_bus = MimeBus.getInstance();
        String pl_mime_type = mime_bus.getTypeForFileExtension(file_ext);
        if (pl_mime_type == null) throw new ServletException("playlist file extension not recognized");
        resp.setContentType(pl_mime_type);
        PlaylistWriter pl_writer = PlaylistWriter.newInstance(resp.getWriter(), pl_mime_type);
        pl_writer.write(tune);
        pl_writer.close();
    }

    /**
     * attempt to respond to a request for media data for tune
     */
    private void handleTune(Tune tune, HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (tune == null) throw new IllegalArgumentException("must provide a valid tune rec");
        if (log.isDebugEnabled()) log.debug("handleTune: " + tune);
        String dst_mime = req.getParameter("dstMime");
        String src_mime = req.getParameter("srcMime");
        if (tune.getMime() != null) src_mime = tune.getMime(); else if (src_mime == null || src_mime.trim().length() == 0) src_mime = "audio/x-mpeg";
        if (dst_mime == null || dst_mime.trim().length() == 0) dst_mime = src_mime;
        int filesize = (tune instanceof FtuneRec && src_mime.equalsIgnoreCase(dst_mime) ? (int) ((FtuneRec) tune).getFileSize() : Integer.MAX_VALUE);
        if (log.isDebugEnabled()) log.debug("handleTune: filesize=" + filesize);
        Range range = getRange(req, filesize);
        ServletOutputStream out = null;
        InputStream is = null;
        try {
            if (tune instanceof FtuneRec && src_mime.equals(dst_mime)) {
                is = new FileInputStream(((FtuneRec) tune).getFilePath());
                is.skip(range.getByteOffset());
            } else {
                try {
                    is = MusicInputStreamFactory.getStream(tune.getDirectURI().getURL(), range.getByteOffset(), dst_mime, src_mime);
                } catch (IOException e) {
                    log.warn("Unable to open connection to " + tune.getDirectURI().getURL() + " -- error: " + e);
                    throw e;
                }
            }
            resp.setContentType(dst_mime);
            out = resp.getOutputStream();
            writeMedia(is, range.getByteCount(), out);
        } finally {
            HelperFile.safeClose(is);
            HelperFile.safeClose(out);
        }
    }

    /**
     * attempt to respond to a request for a playlist
     */
    private void handlePlaylist(Playlist pl, HttpServletRequest req, HttpServletResponse resp) throws IOException, BusException, InstantiationException {
        if (log.isDebugEnabled()) log.debug("handlePlaylist: uri=" + pl.getDirectURI());
        final int ITEMS_PER_PAGE = 50;
        TuneBus tune_bus = TuneBus.getInstance();
        Vector keys = tune_bus.getKeysForPlaylist(pl.getSrcId(), 0, pl.getOrderBy(), 0, Playlist.NO_LIMIT);
        if (log.isDebugEnabled()) log.debug("handlePlaylist: keys=" + keys + " order_by=" + pl.getOrderBy());
        String mime = pl.getMime();
        String pl_mime_type = mime;
        resp.setContentType(pl_mime_type);
        PlaylistWriter pl_writer = PlaylistWriter.newInstance(resp.getWriter(), pl_mime_type);
        Hashtable args = new Hashtable();
        args.put(Source.POPULATE_MFILE, new Boolean(true));
        args.put(Source.POPULATE_MEXTERNAL, new Boolean(true));
        args.put(Source.POPULATE_CONTENT_URL, Source.DEFAULT_URL);
        int count = keys.size();
        for (int from_index = 0; from_index < count; from_index += ITEMS_PER_PAGE) {
            int to_index = from_index + ITEMS_PER_PAGE;
            if (to_index >= count) to_index = count - 1;
            List sublist = keys.subList(from_index, to_index + 1);
            Vector recs = tune_bus.getRecs(new Vector(sublist), pl.getOrderBy(), args);
            Iterator it = recs.iterator();
            while (it.hasNext()) pl_writer.write((Source) it.next());
        }
        pl_writer.close();
    }

    /**
     * transmit a block of binary mpeg data
     */
    protected void writeMedia(InputStream is, int byte_count, ServletOutputStream out) throws IOException {
        if (is == null || byte_count < 0 || out == null) throw new IllegalArgumentException();
        if (log.isDebugEnabled()) log.debug("writeMedia: count=" + byte_count);
        int block_size = (byte_count > MAX_BLOCK_SIZE ? MAX_BLOCK_SIZE : byte_count);
        byte[] buf = new byte[block_size];
        int remaining = byte_count;
        ExpiringWriter writer = new ExpiringWriter(out, WRITE_TIMEOUT);
        while (remaining > 0) {
            int bytes_to_read = (remaining < block_size ? remaining : block_size);
            if (bytes_to_read < 1) break;
            int bytes_read = is.read(buf, 0, bytes_to_read);
            if (bytes_read < 1) break;
            if (log.isDebugEnabled()) log.debug("writeMedia: writing " + bytes_read + " byte(s)");
            if (!writer.write(buf, 0, bytes_read)) break;
            remaining -= bytes_read;
        }
    }

    /**
    * obtain the byte range to return to the caller
    */
    private Range getRange(HttpServletRequest req, int file_size) throws ServletException {
        try {
            int byte_offset, byte_count;
            String range = req.getHeader("Range");
            if (range != null) {
                int eq = range.indexOf('=');
                int dash = range.indexOf('-', eq);
                byte_offset = Integer.parseInt(range.substring(eq + 1, dash));
                String s_end = range.substring(dash + 1);
                int byte_end;
                if (s_end.length() > 0) byte_end = Integer.parseInt(range.substring(dash + 1)); else byte_end = file_size;
                byte_count = byte_end - byte_offset + 1;
            } else {
                byte_offset = HelperServlet.getIntParam(req, "_offset");
                byte_count = HelperServlet.getIntParam(req, "_count", file_size - byte_offset);
            }
            int available = file_size - byte_offset;
            if (byte_count > available) byte_count = available;
            if (byte_count < 1) throw new ServletException("byte count invalid -- bad src_id?");
            return new Range(byte_offset, byte_count);
        } catch (NumberFormatException e) {
            throw new ServletException("number-format problem getting range", e);
        }
    }

    /** obtain a fresh content URL for the specified source */
    private URL getContentURL(int src_id) throws BusException {
        SourceBus source_bus = SourceBus.getInstance();
        Hashtable args = new Hashtable();
        args.put(Source.POPULATE_CONTENT_URL, Source.DEFAULT_URL);
        Source source = (Source) source_bus.getRec(src_id, args);
        return source.getContentURL();
    }

    protected static class Range {

        public Range(int byte_offset, int byte_count) {
            this.byte_offset = byte_offset;
            this.byte_count = byte_count;
        }

        public int getByteOffset() {
            return byte_offset;
        }

        public int getByteCount() {
            return byte_count;
        }

        private int byte_offset;

        private int byte_count;
    }

    protected static final int MAX_BLOCK_SIZE = 65536;

    protected static final int WRITE_TIMEOUT = 60000;

    /** pre-built arglist used in getSourceRec */
    static Hashtable GET_TUNE_ARGS = new Hashtable();

    static Hashtable GET_PLAYLIST_ARGS = new Hashtable();

    {
        GET_TUNE_ARGS.put(Source.POPULATE_MFILE, new Boolean(true));
        GET_TUNE_ARGS.put(Source.POPULATE_MEXTERNAL, new Boolean(true));
        GET_TUNE_ARGS.put(Source.POPULATE_DIRECT_URI, new Boolean(true));
        GET_PLAYLIST_ARGS.put(Source.POPULATE_MFILE, new Boolean(true));
        GET_PLAYLIST_ARGS.put(Source.POPULATE_MEXTERNAL, new Boolean(true));
        GET_PLAYLIST_ARGS.put(Source.POPULATE_DIRECT_URI, new Boolean(true));
    }

    /**
     * logging object
     */
    protected static Log log = LogFactory.getLog(ContentEngine.class);
}
