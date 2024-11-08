package org.iosgi.util.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author Sven Schulz
 */
public class Streams {

    private static final ExecutorService EXEC_SVC = Executors.newCachedThreadPool();

    public static Future<Void> drain(final InputStream source, final OutputStream target) {
        return EXEC_SVC.submit(new Callable<Void>() {

            @Override
            public Void call() throws Exception {
                try {
                    try {
                        byte[] buf = new byte[4096];
                        int read;
                        while ((read = source.read(buf)) != -1) {
                            target.write(buf, 0, read);
                        }
                    } finally {
                        target.flush();
                        target.close();
                    }
                } finally {
                    source.close();
                }
                return null;
            }
        });
    }

    public static Future<Void> drain(final InputStream source, final File target) throws FileNotFoundException {
        return drain(source, new FileOutputStream(target));
    }
}
