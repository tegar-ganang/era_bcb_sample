package org.lhuillier.pwsafe;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.lhuillier.pwsafe.io.DbReader;
import org.lhuillier.pwsafe.io.DbWriter;
import org.lhuillier.pwsafe.io.StretchedKey;
import org.lhuillier.pwsafe.model.Database;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class Run {

    public static void main(String[] args) throws Exception {
        Injector injector = Guice.createInjector(new PwsafeModule());
        DbReader dbLoader = injector.getInstance(DbReader.class);
        FileInputStream inStream = new FileInputStream("test.psafe3");
        byte[] data;
        try {
            data = readFully(inStream);
        } finally {
            inStream.close();
        }
        StretchedKey key = dbLoader.prepareKey(data, "test");
        Database db = dbLoader.load(data, key);
        DbWriter dbWriter = injector.getInstance(DbWriter.class);
        key = key.resalt("test");
        byte[] written = dbWriter.write(db, key);
        FileOutputStream out = new FileOutputStream("temp.psafe3");
        out.write(written);
        out.close();
    }

    static byte[] readFully(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8092];
        while (in.available() > 0) {
            int read = in.read(buffer);
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }
}
