package org.easyrec.servlet;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPOutputStream;

/**
 * based on <a href="http://balusc.blogspot.com/2009/02/fileservlet-supporting-resume-and.html"/>
 */
public class StaticServlet extends HttpServlet {

    private static final int DEFAULT_BUFFER_SIZE = 10240;

    private static final long DEFAULT_EXPIRE_TIME = 604800000L;

    private static final String MULTIPART_BOUNDARY = "MULTIPART_BYTERANGES";

    private String basePath;

    /**
     * Initialize the servlet.
     *
     * @see HttpServlet#init().
     */
    public void init() throws ServletException {
        this.basePath = getServletContext().getRealPath(getInitParameter("basePath"));
        if (this.basePath == null) throw new ServletException("FileServlet init param 'basePath' is required."); else {
            File path = new File(this.basePath);
            if (!path.exists()) throw new ServletException("FileServlet init param 'basePath' value '" + this.basePath + "' does actually not exist in file system."); else if (!path.isDirectory()) throw new ServletException("FileServlet init param 'basePath' value '" + this.basePath + "' is actually not a directory in file system."); else if (!path.canRead()) throw new ServletException("FileServlet init param 'basePath' value '" + this.basePath + "' is actually not readable in file system.");
        }
    }

    private void rewriteApiJs(HttpServletRequest request) throws IOException {
        File file = new File(basePath, "api-js/easyrec.js");
        if (!file.exists()) return;
        BufferedReader reader = null;
        FileWriter writer = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String firstLine = reader.readLine();
            if (firstLine.startsWith("var easyrecApiUrl")) return;
            File tempFile = File.createTempFile("easyrec-api", "js");
            writer = new FileWriter(tempFile, false);
            String localName = request.getLocalName();
            localName = localName.equals("0.0.0.0") ? "localhost" : localName;
            String webappPath = getServletContext().getContextPath();
            String extendedWebAppPath = request.getScheme() + "://" + localName + ":" + request.getLocalPort() + webappPath;
            writer.write("var easyrecApiUrl=\"");
            writer.write(extendedWebAppPath);
            writer.write("/api/1.0/json/\";\n");
            String line = null;
            writer.write(firstLine);
            writer.write('\n');
            while ((line = reader.readLine()) != null) {
                writer.write(line);
                writer.write('\n');
            }
            writer.close();
            reader.close();
            if (!file.delete()) log("Failed to delete old api js file.");
            if (!tempFile.renameTo(file)) log("Failed to move temp file to api js file.");
        } catch (Exception ex) {
            log("Error while rewriting api js.", ex);
        } finally {
            close(reader);
            close(writer);
        }
    }

    /**
     * Process HEAD request. This returns the same headers as GET request, but without content.
     *
     * @see HttpServlet#doHead(HttpServletRequest, HttpServletResponse).
     */
    protected void doHead(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response, false);
    }

    /**
     * Process GET request.
     *
     * @see HttpServlet#doGet(HttpServletRequest, HttpServletResponse).
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response, true);
    }

    /**
     * Process the actual request.
     *
     * @param request  The request to be processed.
     * @param response The response to be created.
     * @param content  Whether the request body should be written (GET) or not (HEAD).
     * @throws IOException If something fails at I/O level.
     */
    private void processRequest(HttpServletRequest request, HttpServletResponse response, boolean content) throws IOException {
        String requestedFile = request.getPathInfo();
        if (requestedFile == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (requestedFile.equals("/easyrec.js") && request.getServletPath().equals("/api-js")) rewriteApiJs(request);
        File file = new File(new File(basePath, request.getServletPath()), URLDecoder.decode(requestedFile, "UTF-8"));
        if (!file.exists()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (file.isDirectory()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        String fileName = file.getName();
        long length = file.length();
        long lastModified = file.lastModified();
        String eTag = fileName + "_" + length + "_" + lastModified;
        String ifNoneMatch = request.getHeader("If-None-Match");
        if (ifNoneMatch != null && matches(ifNoneMatch, eTag)) {
            response.setHeader("ETag", eTag);
            response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }
        long ifModifiedSince = request.getDateHeader("If-Modified-Since");
        if (ifNoneMatch == null && ifModifiedSince != -1 && ifModifiedSince + 1000 > lastModified) {
            response.setHeader("ETag", eTag);
            response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }
        String ifMatch = request.getHeader("If-Match");
        if (ifMatch != null && !matches(ifMatch, eTag)) {
            response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
            return;
        }
        long ifUnmodifiedSince = request.getDateHeader("If-Unmodified-Since");
        if (ifUnmodifiedSince != -1 && ifUnmodifiedSince + 1000 <= lastModified) {
            response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
            return;
        }
        Range full = new Range(0, length - 1, length);
        List<Range> ranges = new ArrayList<Range>();
        String range = request.getHeader("Range");
        if (range != null) {
            if (!range.matches("^bytes=\\d*-\\d*(,\\d*-\\d*)*$")) {
                response.setHeader("Content-Range", "bytes */" + length);
                response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                return;
            }
            String ifRange = request.getHeader("If-Range");
            if (ifRange != null && !ifRange.equals(eTag)) {
                try {
                    long ifRangeTime = request.getDateHeader("If-Range");
                    if (ifRangeTime != -1 && ifRangeTime + 1000 < lastModified) ranges.add(full);
                } catch (IllegalArgumentException ignore) {
                    ranges.add(full);
                }
            }
            if (ranges.isEmpty()) {
                for (String part : range.substring(6).split(",")) {
                    long start = sublong(part, 0, part.indexOf("-"));
                    long end = sublong(part, part.indexOf("-") + 1, part.length());
                    if (start == -1) {
                        start = length - end;
                        end = length - 1;
                    } else if (end == -1 || end > length - 1) end = length - 1;
                    if (start > end) {
                        response.setHeader("Content-Range", "bytes */" + length);
                        response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                        return;
                    }
                    ranges.add(new Range(start, end, length));
                }
            }
        }
        String contentType = getServletContext().getMimeType(fileName);
        boolean acceptsGzip = false;
        String disposition = "inline";
        if (contentType == null) contentType = "application/octet-stream";
        if (contentType.startsWith("text")) {
            String acceptEncoding = request.getHeader("Accept-Encoding");
            acceptsGzip = acceptEncoding != null && accepts(acceptEncoding, "gzip");
            contentType += ";charset=UTF-8";
        } else if (!contentType.startsWith("image")) {
            String accept = request.getHeader("Accept");
            disposition = accept != null && accepts(accept, contentType) ? "inline" : "attachment";
        }
        if (fileName.endsWith(".ico")) {
            contentType = "image/x-icon";
        }
        response.reset();
        response.setBufferSize(DEFAULT_BUFFER_SIZE);
        response.setHeader("Content-Disposition", disposition + ";filename=\"" + fileName + "\"");
        response.setHeader("Accept-Ranges", "bytes");
        response.setHeader("ETag", eTag);
        response.setDateHeader("Last-Modified", lastModified);
        response.setDateHeader("Expires", System.currentTimeMillis() + DEFAULT_EXPIRE_TIME);
        RandomAccessFile input = null;
        OutputStream output = null;
        try {
            input = new RandomAccessFile(file, "r");
            output = response.getOutputStream();
            if (ranges.isEmpty() || ranges.get(0) == full) {
                Range r = full;
                response.setContentType(contentType);
                response.setHeader("Content-Range", "bytes " + r.start + "-" + r.end + "/" + r.total);
                if (content) {
                    if (acceptsGzip) {
                        response.setHeader("Content-Encoding", "gzip");
                        output = new GZIPOutputStream(output, DEFAULT_BUFFER_SIZE);
                    } else response.setHeader("Content-Length", String.valueOf(r.length));
                    copy(input, output, r.start, r.length);
                }
            } else if (ranges.size() == 1) {
                Range r = ranges.get(0);
                response.setContentType(contentType);
                response.setHeader("Content-Range", "bytes " + r.start + "-" + r.end + "/" + r.total);
                response.setHeader("Content-Length", String.valueOf(r.length));
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                if (content) copy(input, output, r.start, r.length);
            } else {
                response.setContentType("multipart/byteranges; boundary=" + MULTIPART_BOUNDARY);
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                if (content) {
                    ServletOutputStream sos = (ServletOutputStream) output;
                    for (Range r : ranges) {
                        sos.println();
                        sos.println("--" + MULTIPART_BOUNDARY);
                        sos.println("Content-Type: " + contentType);
                        sos.println("Content-Range: bytes " + r.start + "-" + r.end + "/" + r.total);
                        copy(input, output, r.start, r.length);
                    }
                    sos.println();
                    sos.println("--" + MULTIPART_BOUNDARY + "--");
                }
            }
        } finally {
            close(output);
            close(input);
        }
    }

    /**
     * Returns true if the given accept header accepts the given value.
     *
     * @param acceptHeader The accept header.
     * @param toAccept     The value to be accepted.
     * @return True if the given accept header accepts the given value.
     */
    private static boolean accepts(String acceptHeader, String toAccept) {
        String[] acceptValues = acceptHeader.split("\\s*(,|;)\\s*");
        Arrays.sort(acceptValues);
        return Arrays.binarySearch(acceptValues, toAccept) > -1 || Arrays.binarySearch(acceptValues, toAccept.replaceAll("/.*$", "/*")) > -1 || Arrays.binarySearch(acceptValues, "*/*") > -1;
    }

    /**
     * Returns true if the given match header matches the given value.
     *
     * @param matchHeader The match header.
     * @param toMatch     The value to be matched.
     * @return True if the given match header matches the given value.
     */
    private static boolean matches(String matchHeader, String toMatch) {
        String[] matchValues = matchHeader.split("\\s*,\\s*");
        Arrays.sort(matchValues);
        return Arrays.binarySearch(matchValues, toMatch) > -1 || Arrays.binarySearch(matchValues, "*") > -1;
    }

    /**
     * Returns a substring of the given string value from the given begin index to the given end
     * index as a long. If the substring is empty, then -1 will be returned
     *
     * @param value      The string value to return a substring as long for.
     * @param beginIndex The begin index of the substring to be returned as long.
     * @param endIndex   The end index of the substring to be returned as long.
     * @return A substring of the given string value as long or -1 if substring is empty.
     */
    private static long sublong(String value, int beginIndex, int endIndex) {
        String substring = value.substring(beginIndex, endIndex);
        return (substring.length() > 0) ? Long.parseLong(substring) : -1;
    }

    /**
     * Copy the given byte range of the given input to the given output.
     *
     * @param input  The input to copy the given range to the given output for.
     * @param output The output to copy the given range from the given input for.
     * @param start  Start of the byte range.
     * @param length Length of the byte range.
     * @throws IOException If something fails at I/O level.
     */
    private static void copy(RandomAccessFile input, OutputStream output, long start, long length) throws IOException {
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int read;
        if (input.length() == length) {
            while ((read = input.read(buffer)) > 0) output.write(buffer, 0, read);
        } else {
            input.seek(start);
            long toRead = length;
            while ((read = input.read(buffer)) > 0) if ((toRead -= read) > 0) output.write(buffer, 0, read); else {
                output.write(buffer, 0, (int) toRead + read);
                break;
            }
        }
    }

    /**
     * Close the given resource.
     *
     * @param resource The resource to be closed.
     */
    private static void close(Closeable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (IOException ignore) {
            }
        }
    }

    /**
     * This class represents a byte range.
     */
    protected class Range {

        long start;

        long end;

        long length;

        long total;

        /**
         * Construct a byte range.
         *
         * @param start Start of the byte range.
         * @param end   End of the byte range.
         * @param total Total length of the byte source.
         */
        public Range(long start, long end, long total) {
            this.start = start;
            this.end = end;
            this.length = end - start + 1;
            this.total = total;
        }
    }
}
