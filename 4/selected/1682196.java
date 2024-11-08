package com.memoire.foo;

import com.memoire.foo.*;
import java.io.*;

public class FooInputStream {

    private static FooCategory pkg_ = null;

    public static final FooCategory init() {
        if (pkg_ == null) {
            pkg_ = FooCategory.create(InputStream.class);
            pkg_.setMessage(">>", FooInputStream.class, "read");
            pkg_.setMessage("->", FooInputStream.class, "readAll");
        }
        return pkg_;
    }

    public static byte[] readAll(InputStream _in) {
        byte[] r = null;
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            while (_in.available() > 0) out.write(_in.read());
            r = out.toByteArray();
        } catch (Exception ex) {
            throw new RuntimeException(FooLib.getClassName(ex.getClass()) + ":" + ex.getMessage());
        }
        return r;
    }

    public static Object read(InputStream _in) {
        Object r = null;
        FooParser parser = FooLib.getParser();
        synchronized (parser) {
            try {
                r = parser.parseExpr(false, false);
            } catch (IOException ex) {
            }
        }
        return r;
    }
}
