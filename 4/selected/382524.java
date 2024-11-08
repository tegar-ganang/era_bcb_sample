package org.bitdrive.access.http.impl;

import org.bitdrive.external.Misc;
import org.bitdrive.file.core.api.FileFactory;
import org.bitdrive.file.core.api.ReadableFile;
import org.jsonhttpd.HTTPPayload;
import org.jsonhttpd.HTTPRequest;
import org.jsonhttpd.HTTPResponseBuilder;
import org.jsonhttpd.customhandler.CustomHandler;
import java.io.IOException;
import java.io.OutputStream;

public class HTTPStreamer implements CustomHandler {

    private String path;

    private FileFactory fileFactory;

    private byte[] errorString = "Video file not found".getBytes();

    public HTTPStreamer(String path, FileFactory fileFactory) {
        this.path = path;
        this.fileFactory = fileFactory;
    }

    public String getPath() {
        return path;
    }

    private String getVideoId(HTTPRequest httpRequest) {
        if (httpRequest.path.indexOf("/download/") != -1) {
            return httpRequest.path.substring(path.length() + "/download/".length() - 1);
        } else {
            return httpRequest.path.substring(path.length());
        }
    }

    private boolean isDownload(HTTPRequest httpRequest) {
        return httpRequest.path.indexOf("/download/") != -1;
    }

    private void stream(ReadableFile file, OutputStream outputStream, long offset, long length) throws IOException {
        byte[] data = new byte[512 * 1024];
        file.seek(offset);
        while (length > 0) {
            int read = file.read(data, 0, (int) Math.min(data.length, length));
            if (read > 0) {
                outputStream.write(data, 0, read);
                length -= read;
            } else {
                break;
            }
        }
    }

    private HTTPPayload getFileHeader(HTTPRequest httpRequest, ReadableFile file) {
        HTTPPayload payload;
        payload = new HTTPPayload();
        if (isDownload(httpRequest)) {
            payload.contentType = "application/octet-stream";
        } else {
            payload.contentType = "video/x-matroska";
        }
        if (httpRequest.byte_range_from >= 0) {
            payload.status = HTTPResponseBuilder.STATUS_Partial_Content;
            payload.contentRangeFrom = httpRequest.byte_range_from;
            payload.contentRangeTotal = file.size();
            payload.contentLength = file.size() - httpRequest.byte_range_from;
            if (httpRequest.byte_range_to > 0) {
                payload.contentRangeTo = httpRequest.byte_range_to;
            } else {
                payload.contentRangeTo = payload.contentRangeTotal - 1;
            }
        } else {
            payload.status = HTTPResponseBuilder.STATUS_OK;
            payload.contentLength = file.size();
        }
        return payload;
    }

    private HTTPPayload getErrorHeader(HTTPRequest httpRequest) {
        HTTPPayload payload;
        payload = new HTTPPayload();
        payload.contentType = "text/plain";
        payload.status = HTTPResponseBuilder.STATUS_Not_Found;
        payload.contentLength = errorString.length;
        return payload;
    }

    public void handle(HTTPRequest httpRequest, OutputStream outputStream) throws IOException {
        String header;
        ReadableFile file;
        HTTPPayload payload;
        file = fileFactory.getFile(Misc.hexStringToByteArray(getVideoId(httpRequest)));
        if (file != null) {
            try {
                payload = getFileHeader(httpRequest, file);
                header = HTTPResponseBuilder.buildResponse(httpRequest, payload);
                outputStream.write(header.getBytes());
                if (httpRequest.byte_range_from < 0) stream(file, outputStream, 0, payload.contentLength); else stream(file, outputStream, httpRequest.byte_range_from, payload.contentLength);
            } finally {
                file.close();
            }
        } else {
            payload = getErrorHeader(httpRequest);
            header = HTTPResponseBuilder.buildResponse(httpRequest, payload);
            outputStream.write(header.getBytes());
            outputStream.write(errorString);
        }
        outputStream.flush();
    }
}
