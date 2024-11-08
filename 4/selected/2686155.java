package com.knowgate.jcifs.smb;

import java.net.URL;
import java.net.UnknownHostException;
import java.net.MalformedURLException;
import java.io.InputStream;
import java.io.IOException;
import com.knowgate.debug.*;

/**
 * This InputStream can read bytes from a file on an SMB file server. Offsets are 64 bits.
 */
public class SmbFileInputStream extends InputStream {

    private SmbFile file;

    private long fp;

    private int readSize, openFlags;

    private byte[] tmp = new byte[1];

    /**
 * Creates an {@link java.io.InputStream} for reading bytes from a file on
 * an SMB server addressed by the <code>url</code> parameter. See {@link
 * jcifs.smb.SmbFile} for a detailed description and examples of the smb
 * URL syntax.
 *
 * @param url An smb URL string representing the file to read from
 * @return A new <code>InputStream</code> for the specified <code>SmbFile</code>
 */
    public SmbFileInputStream(String url) throws SmbException, MalformedURLException, UnknownHostException {
        this(new SmbFile(url));
    }

    /**
 * Creates an {@link java.io.InputStream} for reading bytes from a file on
 * an SMB server represented by the {@link jcifs.smb.SmbFile} parameter. See
 * {@link jcifs.smb.SmbFile} for a detailed description and examples of
 * the smb URL syntax.
 *
 * @param url An smb URL string representing the file to write to
 * @return A new <code>InputStream</code> for the specified <code>SmbFile</code>
 */
    public SmbFileInputStream(SmbFile file) throws SmbException, MalformedURLException, UnknownHostException {
        this(file, SmbFile.O_RDONLY);
    }

    SmbFileInputStream(SmbFile file, int openFlags) throws SmbException, MalformedURLException, UnknownHostException {
        this.file = file;
        this.openFlags = openFlags;
        file.open(openFlags, SmbFile.ATTR_NORMAL, 0);
        readSize = Math.min(file.tree.session.transport.rcv_buf_size - 70, file.tree.session.transport.server.maxBufferSize - 70);
    }

    /**
 * Closes this input stream and releases any system resources associated with the stream.
 *
 * @throws IOException if a network error occurs
 */
    public void close() throws IOException {
        file.close();
    }

    /**
 * Reads a byte of data from this input stream.
 *
 * @throws IOException if a network error occurs
 */
    public int read() throws IOException {
        if (read(tmp, 0, 1) == -1) {
            return -1;
        }
        return tmp[0] & 0xFF;
    }

    /**
 * Reads up to b.length bytes of data from this input stream into an array of bytes.
 *
 * @throws IOException if a network error occurs
 */
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    /**
 * Reads up to len bytes of data from this input stream into an array of bytes.
 *
 * @throws IOException if a network error occurs
 */
    public int read(byte[] b, int off, int len) throws IOException {
        if (len <= 0) {
            return 0;
        }
        long start = fp;
        file.open(openFlags, SmbFile.ATTR_NORMAL, 0);
        if (DebugFile.trace) DebugFile.writeln("read: fid=" + file.fid + ",off=" + off + ",len=" + len);
        SmbComReadAndXResponse response = new SmbComReadAndXResponse(b, off);
        if (file.type == SmbFile.TYPE_NAMED_PIPE) {
            response.responseTimeout = 0;
        }
        int r, n;
        do {
            r = len > readSize ? readSize : len;
            if (DebugFile.trace) DebugFile.writeln("read: len=" + len + ",r=" + r + ",fp=" + fp);
            try {
                file.send(new SmbComReadAndX(file.fid, fp, r, null), response);
            } catch (SmbException se) {
                if (file.type == SmbFile.TYPE_NAMED_PIPE && se.getNtStatus() == NtStatus.NT_STATUS_PIPE_BROKEN) {
                    return -1;
                }
                throw se;
            }
            if ((n = response.dataLength) <= 0) {
                return (int) ((fp - start) > 0L ? fp - start : -1);
            }
            fp += n;
            len -= n;
            response.off += n;
        } while (len > 0 && n == r);
        return (int) (fp - start);
    }

    public int available() throws IOException {
        SmbNamedPipe pipe;
        TransPeekNamedPipe req;
        TransPeekNamedPipeResponse resp;
        if (file.type != SmbFile.TYPE_NAMED_PIPE) {
            return 0;
        }
        pipe = (SmbNamedPipe) file;
        file.open((pipe.pipeType & 0xFF0000) | SmbFile.O_EXCL, SmbFile.ATTR_NORMAL, 0);
        req = new TransPeekNamedPipe(file.unc, file.fid);
        resp = new TransPeekNamedPipeResponse(pipe);
        pipe.sendTransaction(req, resp);
        if (resp.status == TransPeekNamedPipeResponse.STATUS_DISCONNECTED || resp.status == TransPeekNamedPipeResponse.STATUS_SERVER_END_CLOSED) {
            file.opened = false;
            return 0;
        }
        return resp.available;
    }

    /**
 * Skip n bytes of data on this stream. This operation will not result
 * in any IO with the server. Unlink <tt>InputStream</tt> value less than
 * the one provided will not be returned if it exceeds the end of the file
 * (if this is a problem let us know).
 */
    public long skip(long n) throws IOException {
        if (n > 0) {
            fp += n;
            return n;
        }
        return 0;
    }
}
