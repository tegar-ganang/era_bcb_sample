package info.nekonya.xml.beanReader.test.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

public class Utils {

    public static enum CopyMode {

        OverwriteFile(1), OverwriteFolder(3);

        int val;

        private CopyMode(int value) {
            val = value;
        }
    }

    @SuppressWarnings("unused")
    private static Logger log = Logger.getLogger(Utils.class.getCanonicalName());

    public static interface Functor<A, B> {

        B apply(A obj);
    }

    public static class StringWrapper {

        public StringWrapper(String str) {
            if (str == null) {
                NullPointerException e = new NullPointerException("Argument is null.");
                log.throwing("Utils.StringWrapper", "<init>", e);
                throw e;
            }
            _str = str;
        }

        public StringWrapper(Object obj) {
            this(obj.toString());
        }

        @Override
        public String toString() {
            return _str;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) return false;
            if (!(obj instanceof StringWrapper)) return false;
            StringWrapper wrapper = (StringWrapper) obj;
            return wrapper.toString().equals(this.toString());
        }

        private String _str;
    }

    public static boolean safeEquals(Object o1, Object o2) {
        if (o1 == o2) return true; else if (o1 == null || o2 == null) return false; else {
            boolean o1toO2 = o1.equals(o2);
            boolean o2toO1 = o2.equals(o1);
            if (o1toO2 != o2toO1) {
                RuntimeException e = new RuntimeException("Inconsistent 'equals' operation for objects " + o1.toString() + " and " + o2.toString() + ".");
                log.throwing("Utils", "safeEquals", e);
                throw e;
            } else return o1toO2;
        }
    }

    public static String stringImplode(Collection<String> strings, String glue) {
        List<String> stringList = new ArrayList<String>(strings);
        StringBuilder builder = new StringBuilder();
        if (stringList.size() > 0) {
            for (int i = 0; i < stringList.size() - 1; i++) builder.append(stringList.get(i) + glue);
            builder.append(stringList.get(stringList.size() - 1));
        }
        return builder.toString();
    }

    public static <A, B> List<B> transform(Collection<A> coll, Functor<A, B> functor) {
        List<B> newColl = new ArrayList<B>();
        for (A obj : coll) newColl.add(functor.apply(obj));
        return newColl;
    }

    @SuppressWarnings("unchecked")
    public static <A, B> B[] transform(A[] coll, Functor<A, B> functor, Class<B> outputClass) {
        B[] newArray = (B[]) Array.newInstance(outputClass, coll.length);
        for (int i = 0; i < coll.length; i++) newArray[i] = functor.apply(coll[i]);
        return newArray;
    }

    public static int combineHashCodes(int[] codes) {
        return combineHashCodes_rec(codes, 0);
    }

    private static int combineHashCodes_rec(int[] codes, int accumulatedCode) {
        if (codes.length == 0) return accumulatedCode; else {
            int code = codes[0];
            int[] leftoverCodes = new int[codes.length - 1];
            for (int i = 1; i < codes.length; i++) leftoverCodes[i] = codes[i];
            int temp = 31 * accumulatedCode + code;
            return combineHashCodes_rec(leftoverCodes, temp);
        }
    }

    public static String readFile(File file) throws IOException {
        BufferedReader bf = new BufferedReader(new FileReader(file));
        StringBuffer strBuffer = new StringBuffer();
        int currChar = bf.read();
        while (currChar != -1) {
            strBuffer.append(currChar);
            currChar = bf.read();
        }
        bf.close();
        return strBuffer.toString();
    }

    public static String readFile(File file, String encodingName) throws IOException {
        BufferedReader bf = new BufferedReader(new InputStreamReader(new FileInputStream(file), encodingName));
        StringWriter strWriter = new StringWriter();
        int currChar = bf.read();
        while (currChar != -1) {
            strWriter.write(currChar);
            currChar = bf.read();
        }
        bf.close();
        return strWriter.toString();
    }

    public static void copy(File from, File to, CopyMode mode) throws IOException {
        if (!from.exists()) {
            IllegalArgumentException e = new IllegalArgumentException("Source doesn't exist: " + from.getCanonicalFile());
            log.throwing("Utils", "copy", e);
            throw e;
        }
        if (from.isFile()) {
            if (!to.canWrite()) {
                IllegalArgumentException e = new IllegalArgumentException("Cannot write to target location: " + to.getCanonicalFile());
                log.throwing("Utils", "copy", e);
                throw e;
            }
        }
        if (to.exists()) {
            if ((mode.val & CopyMode.OverwriteFile.val) != CopyMode.OverwriteFile.val) {
                IllegalArgumentException e = new IllegalArgumentException("Target already exists: " + to.getCanonicalFile());
                log.throwing("Utils", "copy", e);
                throw e;
            }
            if (to.isDirectory()) {
                if ((mode.val & CopyMode.OverwriteFolder.val) != CopyMode.OverwriteFolder.val) {
                    IllegalArgumentException e = new IllegalArgumentException("Target is a folder: " + to.getCanonicalFile());
                    log.throwing("Utils", "copy", e);
                    throw e;
                } else to.delete();
            }
        }
        if (from.isFile()) {
            FileChannel in = new FileInputStream(from).getChannel();
            FileLock inLock = in.lock();
            FileChannel out = new FileOutputStream(to).getChannel();
            FileLock outLock = out.lock();
            try {
                in.transferTo(0, (int) in.size(), out);
            } finally {
                inLock.release();
                outLock.release();
                in.close();
                out.close();
            }
        } else {
            to.mkdirs();
            File[] contents = to.listFiles();
            for (File file : contents) {
                File newTo = new File(to.getCanonicalPath() + "/" + file.getName());
                copy(file, newTo, mode);
            }
        }
    }

    public static void write(InputStream in, OutputStream out) throws IOException {
        ReadableByteChannel inChannel = Channels.newChannel(in);
        WritableByteChannel outChannel = Channels.newChannel(out);
        ByteBuffer buffer = ByteBuffer.wrap(new byte[10]);
        while (inChannel.read(buffer) > 0) {
            buffer.flip();
            outChannel.write(buffer);
            buffer.clear();
        }
    }
}
