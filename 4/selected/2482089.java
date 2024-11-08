package org.cmc.music.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public final class FileIO {

    public final byte[] getByteFile(File file) throws IOException {
        return getBytes(file);
    }

    public final byte[] getBytes(File file) throws IOException {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            return getInputStreamBytes(fis);
        } finally {
            try {
                if (fis != null) fis.close();
            } catch (Exception e) {
                Debug.debug(e);
            }
        }
    }

    public final byte[] getByteFileSafe(File file) {
        try {
            return getByteFile(file);
        } catch (Exception e) {
            Debug.debug(e);
            return null;
        }
    }

    public final String getTextFile(File file) throws IOException {
        return new String(getByteFile(file));
    }

    public final String getTextFileSafe(File file) {
        try {
            return new String(getByteFile(file));
        } catch (Exception e) {
            Debug.debug(e);
            return null;
        }
    }

    public final byte[] getLocalByteFileNIO(File file) {
        FileInputStream fis = null;
        FileChannel fFileChannel = null;
        try {
            byte result[] = new byte[(int) file.length()];
            fis = new FileInputStream(file);
            fFileChannel = fis.getChannel();
            int length = (int) file.length();
            ByteBuffer buf = getByteBuffer(nio_segment_size);
            buf.rewind();
            int numRead = 0;
            int total = 0;
            while ((numRead >= 0) && (total < length)) {
                numRead = fFileChannel.read(buf);
                if (numRead > 0) {
                    buf.flip();
                    buf.get(result, total, numRead);
                    buf.flip();
                }
                total += numRead;
                buf.rewind();
            }
            return result;
        } catch (Exception e) {
            Debug.debug(e);
        } finally {
            try {
                if (fFileChannel != null) fFileChannel.close();
                if (fis != null) fis.close();
            } catch (Exception e) {
                Debug.debug(e);
            }
        }
        return null;
    }

    public final File writeBytesToUniqueFile(byte[] src, String prefix, String suffix, File directory) throws IOException {
        File result = File.createTempFile(prefix, suffix, directory);
        writeToFile(src, result);
        return result;
    }

    public final File writeToFile(byte[] src, String prefix, String suffix, File directory) throws IOException {
        File result = new File(directory, prefix + suffix);
        result.createNewFile();
        writeToFile(src, result);
        return result;
    }

    private boolean copy_to_file_io(File src, File dst) throws IOException {
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new FileInputStream(src);
            is = new BufferedInputStream(is);
            os = new FileOutputStream(dst);
            os = new BufferedOutputStream(os);
            byte buffer[] = new byte[1024 * 64];
            int read;
            while ((read = is.read(buffer)) > 0) {
                os.write(buffer, 0, read);
            }
            return true;
        } finally {
            try {
                if (is != null) is.close();
            } catch (IOException e) {
                Debug.debug(e);
            }
            try {
                if (os != null) os.close();
            } catch (IOException e) {
                Debug.debug(e);
            }
        }
    }

    public boolean copyToFile(File src, File dst) throws IOException {
        return copy_to_file_nio(src, dst);
    }

    public boolean copyToFileSafe(File src, File dst) {
        try {
            return copyToFile(src, dst);
        } catch (IOException e) {
            Debug.debug(e);
            return false;
        }
    }

    private final boolean copy_to_file_nio(File src, File dst) throws IOException {
        FileChannel srcChannel = null, dstChannel = null;
        try {
            srcChannel = new FileInputStream(src).getChannel();
            dstChannel = new FileOutputStream(dst).getChannel();
            {
                int safe_max = (64 * 1024 * 1024) / 4;
                long size = srcChannel.size();
                long position = 0;
                while (position < size) {
                    position += srcChannel.transferTo(position, safe_max, dstChannel);
                }
            }
            return true;
        } finally {
            try {
                if (srcChannel != null) srcChannel.close();
            } catch (IOException e) {
                Debug.debug(e);
            }
            try {
                if (dstChannel != null) dstChannel.close();
            } catch (IOException e) {
                Debug.debug(e);
            }
        }
    }

    public final void writeToFile(byte[] src, File file) throws IOException {
        ByteArrayInputStream stream = null;
        try {
            stream = new ByteArrayInputStream(src);
            putInputStreamToFile(stream, file);
        } finally {
            try {
                if (stream != null) stream.close();
            } catch (Exception e) {
                Debug.debug(e);
            }
        }
    }

    public void writeToFile(String s, File file) throws IOException {
        writeToFile(s.getBytes(), file);
    }

    public void writeToFileSafe(String s, File file) {
        try {
            writeToFile(s.getBytes(), file);
        } catch (Exception e) {
            Debug.debug(e);
        }
    }

    public void writeToFileSafe(byte bytes[], File file) {
        try {
            writeToFile(bytes, file);
        } catch (Exception e) {
            Debug.debug(e);
        }
    }

    private final int nio_segment_size = 1024 * 512;

    public final void writeBytesToFileNIO(byte[] src, File file) throws IOException {
        FileOutputStream stream = null;
        FileChannel fFileChannel = null;
        try {
            stream = new FileOutputStream(file);
            fFileChannel = stream.getChannel();
            ByteBuffer buf = getByteBuffer(nio_segment_size);
            int index = 0;
            while (index < src.length) {
                buf.rewind();
                int remaining = src.length - index;
                int size = Math.min(nio_segment_size, remaining);
                if (size != buf.capacity()) {
                    buf = ByteBuffer.allocate(size);
                }
                buf.put(src, index, size);
                buf.flip();
                int written = fFileChannel.write(buf);
                buf.flip();
                index += written;
            }
        } finally {
            try {
                if (fFileChannel != null) fFileChannel.close();
                if (stream != null) stream.close();
            } catch (Exception e) {
                Debug.debug(e);
            }
        }
    }

    private ByteBuffer getByteBuffer(int size) {
        try {
            return ByteBuffer.allocateDirect(size);
        } catch (Exception e) {
            Debug.debug("Misc getByteBuffer 1", e);
        } catch (Error e) {
            Debug.debug("Misc getByteBuffer 2", e);
        }
        return null;
    }

    public final void putInputStreamToFile(InputStream src, File file) throws IOException {
        FileOutputStream stream = null;
        try {
            if (file.getParentFile() != null) file.getParentFile().mkdirs();
            stream = new FileOutputStream(file);
            copyStreamToStream(src, stream);
        } finally {
            try {
                if (stream != null) stream.close();
            } catch (Exception e) {
                Debug.debug(e);
            }
        }
    }

    public final byte[] getInputStreamBytes(InputStream src) throws IOException {
        return getBytes(src);
    }

    public final byte[] getBytes(InputStream src) throws IOException {
        ByteArrayOutputStream stream = null;
        try {
            stream = new ByteArrayOutputStream(4096);
            copyStreamToStream(src, stream);
            return stream.toByteArray();
        } finally {
            try {
                if (stream != null) stream.close();
            } catch (IOException ioe) {
            }
        }
    }

    public static final void copyStreamToStream(InputStream src, OutputStream dst) throws IOException {
        copyStreamToStream(src, dst, true);
    }

    public static final void copyStreamToStream(InputStream src, OutputStream dst, boolean close_streams) throws IOException {
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        try {
            bis = new BufferedInputStream(src);
            bos = new BufferedOutputStream(dst);
            int count;
            byte[] buffer = new byte[4096];
            while ((count = bis.read(buffer, 0, 4096)) > 0) dst.write(buffer, 0, count);
            bos.flush();
        } finally {
            if (close_streams) {
                try {
                    if (bis != null) bis.close();
                } catch (IOException e) {
                    Debug.debug(e);
                }
                try {
                    if (bos != null) bos.close();
                } catch (IOException e) {
                    Debug.debug(e);
                }
            }
        }
    }

    public final String normalizePathRelativeToPath(String a, String b) {
        try {
            a = new File(a).getCanonicalPath();
            b = new File(b).getCanonicalPath();
            return a.substring(b.length());
        } catch (Exception e) {
        }
        return null;
    }

    private long last = -1;

    public final long debugTime(String s) {
        long now = System.currentTimeMillis();
        long diff = ((now - last));
        if (last != -1) System.out.println(s + ": " + diff + " seconds");
        last = now;
        return diff;
    }

    public class DebugTimer {

        private long last = -1;

        private long total = 0;

        private final long start;

        public DebugTimer() {
            start = System.currentTimeMillis();
        }

        public final long debugTime(String s) {
            long now = System.currentTimeMillis();
            long diff = ((now - last));
            if (last != -1) System.out.println(s + ": " + diff + " seconds");
            last = now;
            return diff;
        }

        public final long debugTimeStart(String s) {
            long now = System.currentTimeMillis();
            long diff = ((now - start));
            return diff;
        }

        public final void on() {
            long now = System.currentTimeMillis();
            last = now;
        }

        public final void off() {
            long now = System.currentTimeMillis();
            long diff = ((now - last));
            total += diff;
        }

        public final void getTotal(String s) {
            System.out.println(s + ": " + total + " ms");
        }
    }

    public final String getFilenameWOExtension(String src) {
        return getFilenameWOExtension(new File(src));
    }

    public final String getFilenameWOExtension(File src) {
        String name = src.getName();
        int index = name.lastIndexOf('.');
        if (index < 0) return name;
        return name.substring(0, index);
    }

    public final File changeFileExtension(File src, String ext) {
        File parent = src.getParentFile();
        String name = src.getName();
        int index = name.lastIndexOf('.');
        if (index >= 0) name = name.substring(0, index + 1); else name += '.';
        name += ext;
        File result = new File(parent, name);
        return result;
    }

    public final String padString(String s, int length, char c) {
        StringBuffer result = new StringBuffer();
        int pad = length - s.length();
        for (int i = 0; i < pad; i++) result.append(c);
        result.append(s);
        return result.toString();
    }
}
