package self.io;

import java.io.*;

public final class TransferUtils {

    public static String transfer(Class clsThatCanResolveCpResource, String cpResouce) throws IOException {
        InputStream html = clsThatCanResolveCpResource.getResourceAsStream(cpResouce);
        BufferedInputStream bis = new BufferedInputStream(html, 2048);
        StringBuffer build = new StringBuffer();
        byte[] buff = new byte[4028];
        int readIn;
        try {
            while ((readIn = bis.read(buff)) > 0) {
                String dat = new String(buff, 0, readIn);
                build.append(dat);
            }
        } finally {
            bis.close();
        }
        return build.toString();
    }

    public static long transfer(Reader rdr, Writer wtr, int buffSize) throws IOException {
        long ret = 0;
        char[] buff = new char[buffSize];
        BufferedReader br = new BufferedReader(rdr, 2048);
        try {
            int readIn;
            while ((readIn = br.read(buff)) > 0) {
                ret += readIn;
                wtr.write(buff, 0, readIn);
            }
        } finally {
            br.close();
        }
        return ret;
    }

    public static long transfer(InputStream is, OutputStream os, int buffSize) throws IOException {
        long ret = 0;
        byte[] buff = new byte[buffSize];
        BufferedInputStream bis = new BufferedInputStream(is, 2048);
        try {
            int readIn;
            while ((readIn = bis.read(buff)) > 0) {
                ret += readIn;
                os.write(buff, 0, readIn);
            }
        } finally {
            bis.close();
        }
        return ret;
    }

    public static class TemporaryFileOuputStream extends OutputStream {

        File tempFile;

        private FileOutputStream fos;

        private boolean hasClosed;

        public TemporaryFileOuputStream() throws IOException {
            tempFile = File.createTempFile("tmp.fos", ".trans");
            fos = new FileOutputStream(tempFile);
        }

        public void close() throws IOException {
            fos.close();
            hasClosed = true;
        }

        public void flush() throws IOException {
            fos.flush();
        }

        public void write(byte b[], int off, int len) throws IOException {
            fos.write(b, off, len);
        }

        public void write(byte b[]) throws IOException {
            fos.write(b);
        }

        public void write(int b) throws IOException {
            fos.write(b);
        }

        public void discard() throws IOException {
            if (!hasClosed) fos.close();
            tempFile.delete();
        }
    }

    public static class TemporaryBufferedInputStream extends InputStream {

        private File tempFile;

        private FileInputStream fileStream;

        private BufferedInputStream bufferedStream;

        boolean alreadyClosed;

        public TemporaryBufferedInputStream(TemporaryFileOuputStream tfos) throws IOException {
            tempFile = tfos.tempFile;
            prepare();
        }

        public void prepare() throws IOException {
            close();
            alreadyClosed = false;
            fileStream = new FileInputStream(tempFile);
            bufferedStream = new BufferedInputStream(fileStream);
        }

        public void discard() throws IOException {
            close();
            tempFile.delete();
        }

        public int available() throws IOException {
            return bufferedStream.available();
        }

        public void close() throws IOException {
            if (alreadyClosed) return;
            alreadyClosed = true;
            if (bufferedStream != null) bufferedStream.close();
        }

        public synchronized void mark(int readlimit) {
            bufferedStream.mark(readlimit);
        }

        public boolean markSupported() {
            return bufferedStream.markSupported();
        }

        public int read() throws IOException {
            return bufferedStream.read();
        }

        public int read(byte b[]) throws IOException {
            return bufferedStream.read(b);
        }

        public int read(byte b[], int off, int len) throws IOException {
            return bufferedStream.read(b, off, len);
        }

        public synchronized void reset() throws IOException {
            bufferedStream.reset();
        }

        public long skip(long n) throws IOException {
            return bufferedStream.skip(n);
        }
    }
}
