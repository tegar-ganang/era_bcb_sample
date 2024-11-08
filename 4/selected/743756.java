package p.s;

import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * @author jdp
 */
public abstract class File extends Abstract {

    public static class Get extends File {

        public Get() {
            super();
        }

        public void init(Request request) throws IOException {
            super.init(request);
            if (this.isFile()) {
                this.setStatusOk();
                this.setConnection(request);
                Welcome file = this.file;
                Headers headers = this.headers;
                headers.setContentTypeFor(file);
                headers.setContentLengthFor(file);
                headers.setLastModifiedFor(file);
                headers.setETagFor(file);
                if (this.isModifiedSince(request)) this.setStatusOk(); else this.setStatusNotModified();
            } else if (this.isDirectory()) {
                this.welcome(request);
            } else this.setStatusNotFound();
        }

        /**
         * Called from 'tail'.  
         */
        protected void read(Request request) throws IOException {
            FileChannel file = new FileInputStream(this.file).getChannel();
            try {
                WritableByteChannel out = request.getChannel();
                file.transferTo(0L, this.file.length(), out);
            } finally {
                file.close();
            }
        }

        public void tail(Request request, Output out) throws IOException {
            if (this.isFile() && this.isOk()) this.read(request);
        }
    }

    public static class Put extends File {

        protected long length;

        public Put() {
            super();
        }

        /**
         * Called from 'init' after 'super.init' to define the
         * response.  A subclass implements access control.
         */
        protected void writeInit(Request request) throws IOException {
            java.io.File file = this.file;
            java.io.File dir = file.getParentFile();
            if (!dir.exists()) {
                if (!dir.mkdirs()) this.setStatusForbidden();
            }
            if (!file.exists()) this.setStatusCreated(); else this.setStatusOk();
        }

        /**
         * Called from 'tail' to write the file.  A subclass
         * implements access control.
         */
        protected void writeTail(Request request) throws IOException {
            if (this.isOk()) {
                long length = this.length;
                if (0 < length) {
                    java.io.File file = this.file;
                    try {
                        FileChannel fc = new FileOutputStream(file).getChannel();
                        try {
                            ReadableByteChannel in = request.getChannel();
                            fc.transferFrom(in, 0L, length);
                        } finally {
                            fc.close();
                        }
                    } catch (IOException exc) {
                        this.setStatusNotFound();
                    }
                } else {
                    java.io.File file = this.file;
                    try {
                        FileChannel fc = new FileOutputStream(file).getChannel();
                        try {
                            ReadableByteChannel in = request.getChannel();
                            long pos = 0L, read;
                            while (0L < (read = fc.transferFrom(in, pos, 1024L))) {
                                pos += read;
                            }
                        } finally {
                            fc.close();
                        }
                    } catch (IOException exc) {
                        this.setStatusNotFound();
                    }
                }
            }
        }
    }

    protected Welcome file;

    public File() {
        super();
    }

    public boolean isFile() {
        return this.file.isFile();
    }

    public boolean isDirectory() {
        return this.file.isDirectory();
    }

    public boolean isModifiedSince(Request request) {
        Welcome file = this.file;
        if (file.isFile()) {
            long since = request.getIfModifiedSince();
            if (0L < since) {
                long last = (this.file.lastModified() / Seconds) * Seconds;
                return (since < last);
            }
        }
        return true;
    }

    public void init(Request request) throws IOException {
        super.init(request);
        this.file = request.getLocationFile();
        this.setLocation(this.file);
    }
}
