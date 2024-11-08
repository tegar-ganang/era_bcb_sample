package jreceiver.client.rio.servlet;

import java.io.IOException;
import java.util.Map;
import javax.servlet.*;
import javax.servlet.http.*;
import org.apache.commons.logging.*;
import org.apache.commons.collections.LRUMap;
import jreceiver.client.common.*;
import jreceiver.client.common.servlet.*;
import jreceiver.client.rio.RioLauncher;
import jreceiver.client.rio.RioSettingCache;
import jreceiver.common.rec.source.*;
import jreceiver.common.rec.util.TuneQuery;
import jreceiver.common.rec.util.TuneQueryRec;
import jreceiver.common.rpc.*;
import jreceiver.util.ExpiringWriter;
import jreceiver.util.HelperMisc;
import jreceiver.util.HelperServlet;

/**
 * Servlet answering 'content' requests from Rio Server.
 * <p>
 * Called by the Rio Receiver to retrieve an audio file or playlist.
 * <p>
 * A URL that looks like this
 * <p>
 *    http://.../content/2b0
 * <p>
 * retrieves "audio/mpeg" data for file id 2b0 if that file
 * is an audio file (aka 'tune').
 * <p>
 * The following retrieves a playlist (fileid = 470)
 * <p>
 *    http://.../content/470?_extended=1
 * <p>
 * in the format
 * <p>
 *    1b0=TDesire (Hollywood Remix)\n
 *    150=TMysterious Ways (Solar Plexus\n
 * <p>
 * where the file id is in hex and the 'T' stands for tune.
 * <p>
 * To request ALL playlists requests on the special "100" file id
 * is used:
 * <p>
 *   http://.../content/100?_extended=1
 * <p>
 * with a reply of
 * <p>
 *   1c0=Pkiwi
 *   1d0=Pnonesuch
 * <p>
 *  Notes:
 * <p>
 * Perhaps we should redirect to the MPEG file to keep things simple.
 *
 * @author Reed Esau
 * @version $Revision: 1.24 $ $Date: 2003/05/10 12:58:27 $
 */
public class RioHostContent extends RioHostBase {

    /**
    * the server which hosts the data
    */
    protected PlaylistEncoder pl_enc_rpc;

    protected Tunes tune_rpc;

    protected Playlists pl_rpc;

    protected static ClientContentCache content_cache;

    protected static Map source_cache;

    /** max number of source recs to cache */
    protected static final int SOURCE_CACHE_COUNT = 25;

    /**
     */
    public void init() throws ServletException {
        super.init();
        try {
            pl_enc_rpc = RpcFactory.newPlaylistEncoder();
            tune_rpc = RpcFactory.newTunes();
            pl_rpc = RpcFactory.newPlaylists();
            content_cache = ClientContentCache.getInstance();
            source_cache = new LRUMap(SOURCE_CACHE_COUNT);
        } catch (RpcException e) {
            throw new ServletException("problem contacting remote server", e);
        }
    }

    /**
     * obtain a SourceRec for the given src_id, cached by host IP address
     * <p>
     * Preserve the src_id details, in case we get called again
     * immediately asking for them.
     */
    public Source getSource(String remote_host, int src_id) throws RpcException {
        Source source = (Source) source_cache.get(remote_host);
        if (source == null || src_id != source.getSrcId()) {
            if (log.isDebugEnabled()) log.debug("getSource: fetching src_id=" + src_id);
            int driver_id = RioLauncher.getDriverId();
            if (driver_id == 0) throw new RpcException("driver not yet initialized; cannot obtain content");
            Sources sources_client = RpcFactory.newSources();
            source = sources_client.getRecForDriver(src_id, driver_id);
            source_cache.put(remote_host, source);
        }
        return source;
    }

    /**
     * Determine nature of the Rio's content request and attempt to respond.
     */
    protected void handleRequest(HttpServletRequest req, HttpServletResponse resp, int src_id) throws ServletException, IOException, RpcException {
        if (log.isDebugEnabled()) log.debug("HR: src_id=" + src_id + " remote addr=" + req.getRemoteAddr() + " request_uri=" + req.getRequestURI() + " request_url=" + req.getRequestURL());
        if (src_id == 0x100) {
            log.debug("HR: sending local list of playlists");
            sendList(src_id, req, resp);
        } else if (src_id >= Source.MIN_SRC_ID) {
            Source source = getSource(req.getRemoteAddr(), src_id);
            if (source.getIsTune()) {
                sendMpeg(source, req, resp);
            } else if (source.getIsPlaylist()) {
                sendList(source.getSrcId(), req, resp);
            } else throw new ServletException("source not recognized for src_id=" + src_id);
        }
    }

    /**
     * We're responding with a playlist of tunes, or a list of playlists.
     * Either text or binary depending on what the Rio Receiver requested.
     */
    protected void sendList(int src_id, HttpServletRequest req, HttpServletResponse resp) throws IOException, RpcException {
        if (log.isDebugEnabled()) log.debug("sendList: src_id=" + src_id);
        int extended = HelperServlet.getIntParam(req, "_extended");
        boolean is_text = (extended == 1);
        RioSettingCache cache = RioSettingCache.getInstance();
        int begin = HelperServlet.getIntParam(req, "_begin", 0);
        int end = HelperServlet.getIntParam(req, "_end", -1);
        if (begin < 0) begin = 0;
        int limit = begin + cache.getResultEncodingLimit();
        if (end < 0 || limit < end) end = limit;
        int count = end - begin + 1;
        BaseResponseWriter writer = null;
        try {
            if (is_text) writer = new ResponseWriter(); else writer = new ResponseStreamWriter();
            if (src_id == 0x100) {
                final String pattern = "{0,hex}=P{1} ({2})\r\n";
                writer.write(pl_enc_rpc.encodePlaylists(pattern, begin, count));
            } else {
                int driver_id = RioLauncher.getDriverId();
                TuneQuery tq = new TuneQueryRec();
                tq.addPlaylistKey(src_id);
                Playlist pl = (Playlist) pl_rpc.getRec(src_id, null);
                tq.setOrderBy(pl.getOrderBy());
                if (is_text) {
                    if (log.isDebugEnabled()) log.debug("sendList: text encoding request");
                    final String pattern = "{0,hex}=T{1}\r\n";
                    writer.write(tune_rpc.encodeText(tq, driver_id, pattern, begin, count));
                } else {
                    if (log.isDebugEnabled()) log.debug("sendList: binary encoding request");
                    final byte[] pattern = "{0,word,le}".getBytes();
                    writer.write(tune_rpc.encodeBinary(tq, driver_id, pattern, begin, count));
                }
            }
            writer.respond(resp);
        } finally {
            if (writer != null) writer.close();
        }
    }

    /**
     * send back the requested mp3 or wma data
     * <p>
     * A range is expected to be specified in the header in
     * the following format:
     * <p>
     *         bytes=0-32767
     * <p>
     * TODO: should this method be throwing an exception if
     * mpeg data cannot be sent?
     * <p>
     * TODO: launch separate thread which keeps file open
     * and takes advantage of keep-alive properties of an
     * http connection.  Note that such a thread will have to
     * time out after a certain period if the client doesn't
     * ask for all the data in a timely fashion.
     */
    protected void sendMpeg(Source source, HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException, RpcException {
        String range = req.getHeader("Range");
        if (range != null) {
            int req_offset = 0;
            int req_size = Integer.MAX_VALUE;
            try {
                int eq = range.indexOf('=');
                int dash = range.indexOf('-', eq);
                req_offset = Integer.parseInt(range.substring(eq + 1, dash));
                int end = Integer.parseInt(range.substring(dash + 1));
                req_size = end - req_offset + 1;
            } catch (NumberFormatException e) {
                throw new ServletException("bad range " + range, e);
            }
            standardResponse(req_offset, req_size, source, req, resp);
        } else {
            nonStandardResponse(source, req, resp);
        }
    }

    /** required by the standard Rio client */
    void standardResponse(int req_offset, int req_size, Source source, HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException, RpcException {
        if (log.isDebugEnabled()) log.debug("sendMpeg: req_offset=" + req_offset + " req_size=" + req_size);
        ResponseStreamWriter writer = null;
        try {
            byte[] buf = new byte[req_size];
            ClientContent content = content_cache.getContent(RioLauncher.getDriverId(), req.getRemoteAddr(), source.getSrcId(), source.getContentURL());
            int count2 = content.getBytes(req_offset, buf, 0, req_size);
            if (count2 == 0) {
                log.warn("sendMpeg: sendError(100)");
                resp.sendError(HttpServletResponse.SC_CONTINUE);
            } else {
                String mime = content.getDstMime();
                if (mime == null) throw new ServletException("unexpected missing dst mime type");
                writer = new ResponseStreamWriter(mime);
                writer.setOffset(req_offset);
                if (count2 != req_size) writer.setComplete();
                writer.write(buf, 0, count2);
                writer.respond(resp);
            }
        } finally {
            if (writer != null) writer.close();
        }
    }

    /** required by the non-standard Rio clients -- blindly send the whole thing */
    void nonStandardResponse(Source source, HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException, RpcException {
        String mime = source.getDstMime();
        if (mime == null) throw new ServletException("unexpected missing dst mime type");
        resp.setHeader("Server", "Mercury-Content-Server/0.11");
        resp.setContentType(mime);
        ServletOutputStream out = null;
        try {
            out = resp.getOutputStream();
            ExpiringWriter writer = new ExpiringWriter(out, TIMEOUT);
            ClientContent content = content_cache.getContent(RioLauncher.getDriverId(), req.getRemoteAddr(), source.getSrcId(), source.getContentURL());
            final int MAX_BUF_SIZE = 65536;
            byte[] buf = new byte[MAX_BUF_SIZE];
            int offset = 0;
            while (true) {
                if (log.isDebugEnabled()) log.debug("sendMpeg: offset=" + offset);
                int read_count = content.getBytes(offset, buf, 0, MAX_BUF_SIZE);
                if (read_count == 0) {
                    if (log.isDebugEnabled()) log.debug("sendMpeg: read_count==0");
                    break;
                }
                offset += read_count;
                if (!writer.write(buf, 0, read_count)) break;
                if (read_count < MAX_BUF_SIZE) {
                    if (log.isDebugEnabled()) log.debug("sendMpeg: complete");
                    break;
                }
                if (log.isDebugEnabled()) log.debug("sendMpeg: waiting");
                HelperMisc.sleep(100);
            }
        } finally {
            log.debug("nonStandardResponse: complete");
            if (out != null) out.close();
        }
    }

    /**
     * implicitly fail if no src_id is provided in the * query
     */
    protected boolean requireMfileId() {
        return true;
    }

    private static final int TIMEOUT = 30000;

    /**
    * logging sink
    */
    protected static Log log = LogFactory.getLog(RioHostContent.class);
}
