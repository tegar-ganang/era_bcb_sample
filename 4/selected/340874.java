package com.netx.eap.R1.core;

import java.io.PrintWriter;
import java.io.IOException;
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.servlet.http.HttpServletResponse;
import com.netx.generics.R1.util.Tools;
import com.netx.generics.R1.time.Timestamp;
import com.netx.basic.R1.shared.Globals;
import com.netx.basic.R1.shared.RUN_MODE;
import com.netx.basic.R1.eh.Checker;
import com.netx.basic.R1.io.File;
import com.netx.basic.R1.io.ExtendedInputStream;
import com.netx.basic.R1.io.ExtendedOutputStream;
import com.netx.basic.R1.io.ExtendedReader;
import com.netx.basic.R1.io.ExtendedWriter;
import com.netx.basic.R1.io.Streams;
import com.netx.basic.R1.io.Translator;
import com.netx.basic.R1.io.BasicIOException;
import com.netx.basic.R1.io.FileSystemException;
import com.netx.basic.R1.io.ReadWriteException;

class EapResponseImpl extends HttpServletResponseWrapper implements EapResponse {

    private final ServletContext _srvCtx;

    EapResponseImpl(HttpServletResponse response, ServletContext srvCtx) {
        super(response);
        _srvCtx = srvCtx;
    }

    public ServletContext getServletContext() {
        return _srvCtx;
    }

    public EapContext getEapContext() {
        return (EapContext) getServletContext().getAttribute(Constants.SRVCTX_EAP_CTX);
    }

    public void setContentType(String contentType) {
        Checker.checkNull(contentType, "contentType");
        Checker.checkEmpty(contentType, "contentType");
        if (contentType.startsWith("text")) {
            if (contentType.indexOf("charset") == -1) {
                contentType = contentType + "; charset=" + Config.CHARSET;
            }
        }
        super.setContentType(contentType);
    }

    public void setLastModified(Timestamp timestamp) {
        Checker.checkNull(timestamp, "timestamp");
        setLastModified(timestamp.getTimeInMilliseconds());
    }

    public void setLastModified(long timestamp) {
        setDateHeader("Last-Modified", timestamp);
    }

    public void setDisableCache() {
        Tools.sendDisableCacheHeaders(this);
    }

    public void setEnableCache() {
        if (Globals.getRunMode() == RUN_MODE.DEV) {
            setDisableCache();
        } else {
            Tools.sendEnableCacheHeaders(this);
        }
    }

    public void sendFile(File file) throws BasicIOException {
        Checker.checkNull(file, "file");
        String contentType = setHeadersFor(file);
        if (contentType == null || !contentType.startsWith("text")) {
            _stream(file);
        } else {
            _write(file);
        }
    }

    public void sendBinaryFile(File file) throws BasicIOException {
        Checker.checkNull(file, "file");
        setHeadersFor(file);
        _stream(file);
    }

    public void sendTextFile(File file) throws BasicIOException {
        Checker.checkNull(file, "file");
        setHeadersFor(file);
        _write(file);
    }

    public void sendRedirect(String location) throws BasicIOException {
        try {
            super.sendRedirect(location);
        } catch (IOException io) {
            throw Translator.translateIOE(io, STREAM_NAME);
        }
    }

    public void sendError(int code, String path) throws BasicIOException {
        try {
            super.sendError(code, path);
        } catch (IOException io) {
            throw Translator.translateIOE(io, STREAM_NAME);
        }
    }

    public void sendRedirectPage(String location) throws BasicIOException {
        this.sendRedirect(location);
    }

    public ServletOutputStream getOutputStream() throws ReadWriteException {
        try {
            return super.getOutputStream();
        } catch (IOException io) {
            throw Translator.translateIOE(io, STREAM_NAME);
        }
    }

    public PrintWriter getWriter() throws ReadWriteException {
        try {
            return super.getWriter();
        } catch (IOException io) {
            throw Translator.translateIOE(io, STREAM_NAME);
        }
    }

    String setHeadersFor(File file) throws FileSystemException {
        String contentType = MimeTypes.getAssociatedContentType(file);
        if (contentType != null) {
            setContentType(contentType);
        }
        long contentLength = file.getSize();
        if (contentLength < Integer.MAX_VALUE) {
            setContentLength((int) contentLength);
        } else {
            this.setHeader("Content-Length", new Long(contentLength).toString());
        }
        setLastModified(file.getLastModified());
        setHeader("ETag", _getEtagHeader(file));
        return contentType;
    }

    private void _stream(File file) throws BasicIOException {
        if (file.getSize() > 0) {
            ExtendedInputStream in = file.getInputStream();
            ExtendedOutputStream out = new ExtendedOutputStream(getOutputStream(), STREAM_NAME);
            Streams.copy(in, out);
            in.close();
            out.close();
        }
    }

    private void _write(File file) throws BasicIOException {
        if (file.getSize() > 0) {
            ExtendedReader reader = new ExtendedReader(file);
            ExtendedWriter writer = new ExtendedWriter(getWriter(), STREAM_NAME);
            Streams.copy(reader, writer);
            reader.close();
            writer.close();
        }
    }

    private String _getEtagHeader(File file) throws FileSystemException {
        StringBuilder sb = new StringBuilder();
        sb.append("W/\"");
        sb.append(Long.toHexString(file.getLastModified().getTimeInMilliseconds()));
        sb.append(file.getRelativePath().hashCode());
        sb.append("\"");
        return sb.toString();
    }
}
