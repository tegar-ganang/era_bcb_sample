package org.bitdrive.ui.wui;

import org.bitdrive.file.BufferdFile;
import org.bitdrive.file.FileListManager;
import org.bitdrive.file.FileListNode;
import org.bitdrive.Misc;
import org.jsonhttpd.HTTPPayload;
import org.jsonhttpd.HTTPRequest;
import org.jsonhttpd.HTTPResponseBuilder;
import org.jsonhttpd.customhandler.CustomHandler;
import java.io.IOException;
import java.io.OutputStream;

public class WatchService implements CustomHandler {

    private String path;

    private byte[] errorStr = "Video file not found".getBytes();

    public WatchService(String path) {
        this.path = path;
    }

    private String getVideoId(HTTPRequest httpRequest) {
        return httpRequest.path.substring(path.length());
    }

    private BufferdFile getFile(String id) {
        FileListNode node;
        FileListManager manager = FileListManager.getInstance();
        node = manager.getNode(Misc.hexStringToByteArray(id));
        if (node == null) {
            System.out.printf("Failed to find filelistnode");
            return null;
        }
        return manager.getFile(node);
    }

    private void stream(BufferdFile file, OutputStream outputStream, long offset, long length) throws IOException {
        byte[] data = new byte[50 * 1024];
        while (length > 0) {
            int read = file.readFile(data, (int) Math.min((long) data.length, length), 0, offset);
            if (read > 0) {
                outputStream.write(data, 0, read);
                length -= read;
                offset += read;
            } else {
                break;
            }
        }
    }

    private HTTPPayload getFileHeader(HTTPRequest httpRequest, BufferdFile file) {
        HTTPPayload payload;
        payload = new HTTPPayload();
        if (file.getNode().getFileName().endsWith(".mkv")) {
            payload.contentType = "video/x-matroska";
        } else {
            payload.contentType = "video/x-msvideo";
        }
        if (httpRequest.byte_range_from >= 0) {
            payload.status = HTTPResponseBuilder.STATUS_Partial_Content;
            payload.contentRangeFrom = httpRequest.byte_range_from;
            payload.contentRangeTotal = file.getNode().getSize();
            payload.contentLength = file.getNode().getSize() - httpRequest.byte_range_from;
            if (httpRequest.byte_range_to > 0) {
                payload.contentRangeTo = httpRequest.byte_range_to;
            } else {
                payload.contentRangeTo = payload.contentRangeTotal - 1;
            }
        } else {
            payload.status = HTTPResponseBuilder.STATUS_OK;
            payload.contentLength = file.getNode().getSize();
        }
        return payload;
    }

    private HTTPPayload getErrorHeader(HTTPRequest httpRequest) {
        HTTPPayload payload;
        payload = new HTTPPayload();
        payload.contentType = "text/plain";
        payload.status = HTTPResponseBuilder.STATUS_Not_Found;
        payload.contentLength = errorStr.length;
        return payload;
    }

    public void handle(HTTPRequest httpRequest, OutputStream outputStream) throws IOException {
        String header;
        HTTPPayload payload;
        BufferdFile file = getFile(getVideoId(httpRequest));
        if (file != null) {
            payload = getFileHeader(httpRequest, file);
            header = HTTPResponseBuilder.buildResponse(httpRequest, payload);
            System.out.println("\nResp Header: \n" + header);
            outputStream.write(header.getBytes());
            if (httpRequest.byte_range_from < 0) stream(file, outputStream, 0, payload.contentLength); else stream(file, outputStream, httpRequest.byte_range_from, payload.contentLength);
        } else {
            payload = getErrorHeader(httpRequest);
            header = HTTPResponseBuilder.buildResponse(httpRequest, payload);
            outputStream.write(header.getBytes());
            outputStream.write(errorStr);
        }
        outputStream.flush();
    }
}
