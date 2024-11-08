package com.asl.web.download;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.asl.library.domain.download.DownloadableFileSource;
import com.asl.library.domain.download.DownloadableSource;

public class DownloadUtils {

    static Log logger = LogFactory.getLog(DownloadUtils.class);

    public static final String CONTENT_MD5_KEY = "Content-MD5";

    public static final String CONTENT_TYPE_KEY = "Content-Type";

    public static final String CONNECTION_KEY = "Connection";

    public static final String CONTENT_DISPOSITION_KEY = "Content-Disposition";

    public static final String LAST_MODIFIED_KEY = "Last-Modified";

    public static final String CONTENT_LENGTH_KEY = "Content-Length";

    public static final String ACCEPT_RANGES_KEY = "Accept-Ranges";

    public static final String RANGE_KEY = "Range";

    public static final String CONTENT_RANGE_KEY = "Content-Range";

    public static final String RANGE_BYTES_KEY = "bytes";

    public static final int IO_BUFFER_SIZE = 8192;

    private static final DateFormat RFC822_DATE_FMT = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");

    /**
	 * Copy file part from current position up to 'endPos' offset to the output stream
	 * 
	 * @param channel
	 * @param buffer
	 * @param output
	 * @param endPos
	 * @throws IOException
	 */
    protected static void copy(InputStream input, byte[] buffer, OutputStream output, long begPos, long endPos) throws IOException {
        int read;
        long bytesToRead = endPos - begPos + 1;
        input.skip(begPos);
        read = input.read(buffer);
        while (bytesToRead > 0 && read > 0) {
            if (bytesToRead >= read) {
                output.write(buffer, 0, read);
                bytesToRead -= read;
                output.flush();
            } else {
                output.write(buffer, 0, (int) bytesToRead);
                break;
            }
            read = input.read(buffer);
        }
        output.flush();
    }

    /**
	 * Copy entire file or single range to the servlet output
	 * 
	 * @param response
	 * @param file
	 * @param range
	 * @throws IOException
	 */
    protected static void copyRange(HttpServletResponse response, DownloadableSource source, DownloadRange range, boolean writeContent) throws IOException {
        boolean part = range.getBegin() != 0 || range.getEnd() != range.getLength() - 1;
        String bytes = null;
        long length = range.getEnd() - range.getBegin() + 1;
        int status = HttpServletResponse.SC_OK;
        Map<String, String> headers = new HashMap<String, String>();
        if (part) {
            status = HttpServletResponse.SC_PARTIAL_CONTENT;
            bytes = "bytes " + range.getBegin() + "-" + range.getEnd() + "/" + range.getLength();
        }
        headers.put(ACCEPT_RANGES_KEY, RANGE_BYTES_KEY);
        headers.put(CONTENT_LENGTH_KEY, String.valueOf(length));
        if (bytes != null) {
            headers.put(CONTENT_RANGE_KEY, bytes);
        }
        long time = source.getLastModified();
        String mime = source.getContentType();
        String md5 = source.getContentMD5();
        if (md5 != null) {
            headers.put(CONTENT_MD5_KEY, md5);
        }
        if (mime != null) {
            headers.put(CONTENT_TYPE_KEY, mime);
        } else {
            headers.put(CONTENT_TYPE_KEY, "application/octet-stream");
        }
        if (time > 0) {
            headers.put(LAST_MODIFIED_KEY, RFC822_DATE_FMT.format(new Date(source.getLastModified())));
        }
        if (source.isAttachment()) {
            headers.put(CONTENT_DISPOSITION_KEY, "attachment; filename=" + source.getContentName() + ";");
        }
        response.setStatus(status);
        if (logger.isDebugEnabled()) {
            logger.debug("Response: " + status);
        }
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            response.setHeader(entry.getKey(), entry.getValue());
            if (logger.isDebugEnabled()) {
                logger.debug("=> " + entry.getKey() + ": " + entry.getValue());
            }
        }
        if (writeContent) {
            InputStream input = source.getStream();
            byte[] buffer = new byte[IO_BUFFER_SIZE];
            try {
                copy(input, buffer, response.getOutputStream(), range.getBegin(), range.getEnd());
            } finally {
                IOUtils.closeQuietly(input);
            }
        }
    }

    /**
	 * Parse content ranges from request. Returns list of valid ranges
	 */
    protected static List<DownloadRange> parseRanges(HttpServletRequest request, HttpServletResponse response, long length) throws IOException {
        Enumeration<?> rangesHeaders = request.getHeaders(RANGE_KEY);
        List<DownloadRange> ranges = new ArrayList<DownloadRange>();
        if (rangesHeaders == null) {
            return ranges;
        }
        String rangeHeader = request.getHeader(RANGE_KEY);
        while (rangesHeaders.hasMoreElements()) {
            String header = StringUtils.trimToNull((String) rangesHeaders.nextElement());
            if (header != null) {
                if (!header.startsWith(RANGE_BYTES_KEY)) {
                    logger.error("Bad Range header: " + rangeHeader);
                    sendRangeError(response, length);
                    return null;
                }
                header = StringUtils.substringAfter(header, "=").trim();
                StringTokenizer commaTokenizer = new StringTokenizer(header, ",");
                while (commaTokenizer.hasMoreTokens()) {
                    String rangeStr = commaTokenizer.nextToken();
                    rangeStr = StringUtils.trimToNull(rangeStr);
                    if (rangeStr == null) {
                        logger.error("Bad Range header: " + header);
                        sendRangeError(response, length);
                        return null;
                    }
                    DownloadRange range;
                    try {
                        range = new DownloadRange(rangeStr, length);
                    } catch (Exception exception) {
                        logger.error("Invalid range in " + header);
                        sendRangeError(response, length);
                        return null;
                    }
                    ranges.add(range);
                }
            }
        }
        return ranges;
    }

    /**
	 * Process GET/HEAD requests internally
	 * 
	 * @param request
	 * @param response
	 * @param source
	 * @param writeContent
	 * @throws ServletException
	 * @throws IOException
	 */
    protected static void processDownload(HttpServletRequest request, HttpServletResponse response, DownloadableSource source, boolean writeContent) throws ServletException {
        try {
            List<DownloadRange> ranges = parseRanges(request, response, source.getContentLength());
            if (ranges != null && !response.isCommitted()) {
                DownloadRange range;
                if (ranges.size() == 0) {
                    range = new DownloadRange(source.getContentLength());
                } else {
                    range = ranges.get(0);
                }
                copyRange(response, source, range, writeContent);
            }
        } catch (FileNotFoundException exception) {
            sendNotFoundError(response);
        } catch (Exception exception) {
            throw new ServletException(exception);
        }
    }

    /**
	 * Process GET request to download specified downaloadable source object
	 * 
	 * @param request
	 * @param response
	 * @param source
	 * @throws ServletException
	 */
    public static void processGet(HttpServletRequest request, HttpServletResponse response, DownloadableSource source) throws ServletException {
        processDownload(request, response, source, true);
    }

    /**
	 * Processes GET file download request
	 */
    public static void processGet(HttpServletRequest request, HttpServletResponse response, File file) throws ServletException {
        if (file != null && file.exists()) {
            processDownload(request, response, new DownloadableFileSource(file), true);
        } else {
            sendNotFoundError(response);
        }
    }

    /**
	 * Process HEAD request using downloadable source object
	 */
    public static void processHead(HttpServletRequest request, HttpServletResponse response, DownloadableSource source) throws ServletException {
        processDownload(request, response, source, false);
    }

    /**
	 * Processes HEAD request
	 * 
	 * @param request
	 * @param response
	 * @param resource
	 * @throws ServletException
	 */
    public static void processHead(HttpServletRequest request, HttpServletResponse response, File resource) throws ServletException {
        if (resource != null && resource.exists()) {
            processDownload(request, response, new DownloadableFileSource(resource), false);
        } else {
            sendNotFoundError(response);
        }
    }

    private static void sendNotFoundError(HttpServletResponse response) throws ServletException {
        try {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        } catch (IOException exception) {
            throw new ServletException(exception);
        }
    }

    protected static void sendRangeError(HttpServletResponse response, long length) throws IOException {
        String head = "bytes */" + length;
        logger.debug("Sending SC_REQUESTED_RANGE_NOT_SATISFIABLE error: " + head);
        response.addHeader(CONTENT_RANGE_KEY, head);
        response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
    }
}
