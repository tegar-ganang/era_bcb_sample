package de.carne.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import de.carne.io.Closeables;
import de.carne.io.Files;

/**
 *
 */
public abstract class Library {

    protected Library(ClassLoader loader, String[] names) throws IOException {
        assert loader != null;
        assert names != null;
        assert names.length > 0;
        assert names.length % 3 == 0;
        final File tmpDir = new File(System.getProperty("java.io.tmpdir"));
        tmpDir.mkdirs();
        for (int nameIndex = 0; nameIndex < names.length; nameIndex += 2) {
            final String libraryFileName = Files.joinFileName(tmpDir.getAbsolutePath(), names[nameIndex + 1]).getAbsolutePath();
            copyResourceToFile(loader, names[nameIndex], libraryFileName);
            if (names[nameIndex + 2] != null) {
                System.load(libraryFileName);
            }
        }
    }

    private static void copyResourceToFile(ClassLoader loader, String resource, String file) throws IOException {
        final File out = new File(file);
        out.delete();
        if (!out.exists()) {
            OutputStream outOS = null;
            InputStream inIS = null;
            try {
                inIS = loader.getResourceAsStream(resource);
                if (inIS == null) {
                    throw new FileNotFoundException(resource);
                }
                outOS = new FileOutputStream(out);
                out.deleteOnExit();
                final byte[] buffer = new byte[4096];
                int read;
                while ((read = inIS.read(buffer)) >= 0) {
                    outOS.write(buffer, 0, read);
                }
            } finally {
                Closeables.saveClose(inIS);
                Closeables.saveClose(outOS);
            }
        }
    }
}
