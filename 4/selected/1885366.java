package org.magicdroid.app.common;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import org.magicdroid.commons.Refactor;
import org.magicdroid.commons.Structures;
import org.magicdroid.commons.Structures.ValueHolder;

public class IOUtils {

    public static byte[] load(String text) throws FileNotFoundException {
        final ValueHolder<ByteArrayOutputStream> result = new Structures.ValueHolder<ByteArrayOutputStream>();
        new Refactor.FileTx<FileInputStream>(new FileInputStream(text)) {

            @Override
            protected void internalExecute(FileInputStream input) throws IOException {
                ByteArrayOutputStream r = new ByteArrayOutputStream();
                byte[] b = new byte[4096];
                int len;
                while ((len = input.read(b)) != -1) r.write(b, 0, len);
                result.set(r);
            }
        }.execute();
        return result.get().toByteArray();
    }

    public static void save(String filename, final byte[] data) throws FileNotFoundException {
        new Refactor.FileTx<FileOutputStream>(new FileOutputStream(filename)) {

            @Override
            protected void internalExecute(FileOutputStream output) throws IOException {
                output.write(data);
            }
        }.execute();
    }
}
