package rabbit.handler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import rabbit.cache.*;
import rabbit.http.HTTPDateParser;
import rabbit.http.HTTPHeader;
import rabbit.io.HTTPInputStream;
import rabbit.io.MaxSizeOutputStream;
import rabbit.io.MaximumSizeExcededException;
import rabbit.io.MultiOutputStream;
import rabbit.proxy.Connection;
import rabbit.proxy.PartialCacher;
import rabbit.proxy.Proxy;
import rabbit.util.Logger;
import rabbit.util.SProperties;

/** This class is an implementation of the Handler interface.
 *  This handler does no filtering, it only sends the data as
 *  effective as it can.
 */
public class BaseHandler implements Handler, HandlerFactory, Logger {

    /** The Connection handling the request.*/
    protected Connection con;

    /** The actual request made. */
    protected HTTPHeader request;

    /** The actual response. */
    protected HTTPHeader response;

    /** The stream to read data from. */
    protected HTTPInputStream contentstream;

    /** The stream to send data to. */
    protected MultiOutputStream clientstream;

    /** May we cache this request. */
    protected boolean maycache;

    /** May we filter this request */
    protected boolean mayfilter;

    /** The cache entry if available. */
    protected NCacheEntry entry = null;

    /** The cache stream if available. */
    protected OutputStream cacheStream = null;

    /** The length of the data beeing handled or -1 if unknown.*/
    protected long size = -1;

    /** For creating the factory.
     */
    public BaseHandler() {
    }

    /** Create a new BaseHansler for the given request.
     * @param con the Connection handling the request.
     * @param request the actual request made.
     * @param response the actual response.
     * @param contentstream the stream to read data from.
     * @param clientstream the stream to write data to.
     * @param maycache May we cache this request? 
     * @param mayfilter May we filter this request?
     * @param size the size of the data beeing handled.
     */
    public BaseHandler(Connection con, HTTPHeader request, HTTPHeader response, HTTPInputStream contentstream, MultiOutputStream clientstream, boolean maycache, boolean mayfilter, long size) {
        this.con = con;
        this.request = request;
        this.response = response;
        this.contentstream = contentstream;
        this.clientstream = clientstream;
        this.maycache = maycache;
        this.mayfilter = mayfilter;
        this.size = size;
    }

    public Handler getNewInstance(Connection connection, HTTPHeader header, HTTPHeader webheader, HTTPInputStream contentStream, MultiOutputStream out, boolean maycache, boolean mayfilter, long size) {
        return new BaseHandler(connection, header, webheader, contentStream, out, maycache, mayfilter, size);
    }

    /** Get a HandlerFactory that creates BaseHandlers.
     */
    public static HandlerFactory getFactory() {
        return new BaseHandler();
    }

    /** Write the response header
     * @throws IOException if writing the response fails.
     */
    protected void writeHeader() throws IOException {
        if (response != null) {
            clientstream.writeHTTPHeader(response);
        }
    }

    /** Try to use the resource size to decide if we may cache or not. 
     *  If the size is known and the size is bigger than the maximum cache 
     *  size, then we dont want to cache the resource. 
     */
    protected boolean mayCacheFromSize() {
        NCache cache = con.getProxy().getCache();
        if ((size > 0 && size > cache.getMaxSize()) || (cache.getMaxSize() == 0)) return false;
        return true;
    }

    /** Check if this handler may force the cached resource to be less than the cache max size.
     * @return true
     */
    protected boolean mayRestrictCacheSize() {
        return true;
    }

    /** Set up the cache stream if available.
     * @throws IOException if a cachestream couldnt be set up.
     */
    protected void addCacheStream() throws IOException {
        if (maycache && mayCacheFromSize()) {
            NCache cache = con.getProxy().getCache();
            entry = cache.newEntry(request);
            String expires = response.getHeader("Expires");
            Date exp = null;
            if (expires != null) {
                exp = HTTPDateParser.getDate(expires);
                if (exp == null && expires.equals("0")) exp = new Date(0);
                if (exp != null) {
                    Date now = new Date();
                    if (now.after(exp)) {
                        logError(Logger.WARN, "expire date in the past: '" + expires + "'");
                        entry = null;
                        return;
                    }
                    entry.setExpires(exp.getTime());
                } else {
                    logError(Logger.MSG, "unable to parse expire date: '" + expires + "' for URI: '" + request.getRequestURI() + "'");
                    entry = null;
                    return;
                }
            }
            String entryName = cache.getEntryName(entry.getId(), false);
            if (response.getStatusCode().equals("206")) {
                NCacheEntry oldentry = cache.getEntry(request);
                if (oldentry != null) {
                    String oldName = cache.getEntryName(oldentry);
                    PartialCacher pc = new PartialCacher(this, oldName, response);
                    cacheStream = pc;
                    clientstream.addOutputStream(cacheStream);
                    updateRange(oldentry, response, pc);
                    cache.entryChanged(oldentry);
                    return;
                } else {
                    entry.setDataHook(response);
                    cacheStream = new PartialCacher(this, entryName, response);
                    clientstream.addOutputStream(cacheStream);
                }
            } else {
                entry.setDataHook(response);
                cacheStream = new FileOutputStream(entryName);
                if (mayRestrictCacheSize()) cacheStream = new MaxSizeOutputStream(cacheStream, cache.getMaxSize());
                clientstream.addOutputStream(cacheStream);
            }
        }
    }

    private void updateRange(NCacheEntry old, HTTPHeader response, PartialCacher pc) {
        Proxy proxy = con.getProxy();
        HTTPHeader oldrequest = (HTTPHeader) old.getKey();
        HTTPHeader oldresponse = (HTTPHeader) old.getDataHook(proxy.getCache());
        String cr = oldresponse.getHeader("Content-Range");
        if (cr == null) {
            String cl = oldresponse.getHeader("Content-Length");
            if (cl != null) {
                long l = Long.parseLong(cl);
                cr = "bytes 0-" + (l - 1) + "/" + l;
            }
        }
        if (cr != null && cr.startsWith("bytes ")) cr = cr.substring(6);
        StringTokenizer st = new StringTokenizer(cr, "-/");
        if (st.countTokens() == 3) {
            try {
                int start = Integer.parseInt(st.nextToken());
                int end = Integer.parseInt(st.nextToken());
                int total = Integer.parseInt(st.nextToken());
                if (end == pc.getStart() - 1) {
                    oldrequest.setHeader("Range", "bytes=" + start + "-" + end);
                    oldresponse.setHeader("Content-Range", "bytes " + start + "-" + pc.getEnd() + "/" + total);
                } else {
                    oldrequest.addHeader("Range", "bytes=" + start + "-" + end);
                    oldresponse.addHeader("Content-Range", "bytes " + start + "-" + pc.getEnd() + "/" + total);
                }
            } catch (NumberFormatException e) {
                logError(Logger.WARN, "Bad content range in cache?s: " + e);
            }
        }
    }

    /** This method is used to prepare the stream for the data being sent.
     *  This method does nothing here.
     */
    protected void prepareStream() throws IOException {
    }

    /** Write the data to the client. Handle that the cached resource gets too big. 
     */
    protected void writeData(byte[] v, int off, int len) throws IOException {
        clientstream.write(v, off, len);
    }

    private void transfer(FileChannel fc, WritableByteChannel wc) throws IOException {
        long length = fc.size();
        long transferred = 0;
        long pos = 0;
        do {
            pos += fc.transferTo(pos, length - pos, wc);
        } while (pos < length - 1);
    }

    /** Send the actual data.
     * @throws IOException if reading or writing of the data fails.
     */
    protected void send() throws IOException {
        FileChannel fc = contentstream.getFileChannel();
        if (fc != null) {
            WritableByteChannel wc = clientstream.getChannel();
            if (wc != null) {
                transfer(fc, wc);
                return;
            }
        }
        byte v[] = new byte[2048];
        int read;
        if (size < 0) {
            while ((read = contentstream.read(v)) > 0) {
                writeData(v, 0, read);
            }
        } else {
            long total = 0;
            while (total < size && (read = contentstream.read(v)) > 0) {
                total += read;
                writeData(v, 0, read);
            }
            if (total != size) setPartialContent(total, size);
        }
        clientstream.flush();
    }

    /** Mark the current response as a partial response. 
     */
    protected void setPartialContent(long got, long shouldbe) {
        response.setHeader("RabbIT-Partial", "" + shouldbe);
    }

    /** This method is used to finish the stream for the data being sent.
     *  This method does nothing here.
     */
    protected void finishStream() throws IOException {
    }

    private void removePrivateParts(HTTPHeader header, String type) {
        List<String> cons = header.getHeaders("Cache-Control");
        for (int i = 0; i < cons.size(); i++) {
            String val = cons.get(i);
            int j = val.indexOf(type);
            if (j >= 0) {
                String p = val.substring(j + type.length());
                StringTokenizer st = new StringTokenizer(p, ",\"");
                while (st.hasMoreTokens()) {
                    String t = st.nextToken();
                    header.removeHeader(t);
                }
            }
        }
    }

    private void removePrivateParts(HTTPHeader header) {
        removePrivateParts(header, "private=");
        removePrivateParts(header, "no-cache=");
    }

    /** Check if the client stream also have a cache stream.
     */
    protected boolean clientStreamHasCache() {
        return clientstream.containsStream(cacheStream);
    }

    /** Close nesseccary files and adjust the cached files.
     *  If you override this one, remember to call super.finish ()!
     * @throws IOException if closing the files does.
     */
    protected void finish() throws IOException {
        try {
            Proxy proxy = con.getProxy();
            if (cacheStream != null) {
                if (clientstream != null) clientstream.removeOutputStream(cacheStream);
                cacheStream.close();
            }
            clientstream.flush();
            if (cacheStream != null && !clientStreamHasCache()) {
                removeCache(new MaximumSizeExcededException("resource size got too big " + "or (image)handler removed it."));
            }
            if (entry != null && maycache) {
                NCache cache = proxy.getCache();
                String entryName = cache.getEntryName(entry.getId(), false);
                File f = new File(entryName);
                long filesize = f.length();
                entry.setSize(filesize);
                String cl = response.getHeader("Content-Length");
                if (cl == null) {
                    response.removeHeader("Transfer-Encoding");
                    response.setHeader("Content-Length", "" + filesize);
                }
                removePrivateParts(response);
                cache.addEntry(entry);
            }
            if (response != null && response.getHeader("Content-Length") != null) con.setContentLength(response.getHeader("Content-length"));
        } finally {
            con = null;
            request = null;
            response = null;
            contentstream = null;
            clientstream = null;
            entry = null;
            cacheStream = null;
            size = -1;
        }
    }

    /** Handle the request.
     * A request is made in these steps: 
     * <xmp>
     * writeHeader (); 
     * addCacheStream (); 
     * prepareStream ();
     * send (); 
     * finishStream ();
     * finish ();
     * </xmp>
     * @throws IOException if any of the underlying methods does.
     */
    public void handle() throws IOException {
        try {
            writeHeader();
            addCacheStream();
            prepareStream();
            send();
            finishStream();
        } catch (IOException e) {
            removeCache(e);
            throw e;
        } finally {
            finish();
        }
    }

    /** Remove the cachestream and the cache entry.
     *  Use this to clean up resources held, but not released due to IOExceptions.
     * @param e the Exception that happened
     */
    protected void removeCache(Exception e) {
        if (cacheStream != null) {
            Proxy proxy = con.getProxy();
            try {
                cacheStream.close();
            } catch (IOException ioe) {
                logError(Logger.ERROR, "unable to close cache file: " + ioe);
            }
            String entryName = proxy.getCache().getEntryName(entry.getId(), false);
            File f = new File(entryName);
            if (f.exists()) {
                logError(Logger.DEBUG, "removing cache file due to exception: " + e);
                f.delete();
            }
            clientstream.removeOutputStream(cacheStream);
            cacheStream = null;
            entry = null;
        }
    }

    /** Setup the factory, this method does nothing in this class.
     */
    public void setup(Logger logger, SProperties properties) {
    }

    public void logError(String error) {
        logError(ERROR, error);
    }

    public void logError(int type, String error) {
        con.getProxy().logError(type, error);
    }
}
