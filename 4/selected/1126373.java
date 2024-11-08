package net.jadoth.util.file;

import static net.jadoth.util.chars.VarChar.LargeVarChar;
import static net.jadoth.util.chars.VarChar.MediumVarChar;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.channels.FileChannel;
import net.jadoth.Jadoth;

/**
 * @author Thomas M�nz
 *
 */
public abstract class JaFiles {

    public static final File ensureWritableFile(final File parent, final String filename) throws FileException {
        return JaFiles.ensureWriteableFile(new File(parent, filename));
    }

    public static final File ensureDirectory(final File dir) throws DirectoryException {
        try {
            if (dir.exists()) {
                return dir;
            }
            if (!dir.mkdirs()) {
                throw new DirectoryException(dir, "Directory could not have been created.");
            }
        } catch (final SecurityException e) {
            throw new DirectoryException(dir, e);
        }
        return dir;
    }

    public static final File ensureDirectoryAndFile(final File file) throws FileException {
        final File parent;
        if ((parent = file.getParentFile()) != null) {
            ensureDirectory(parent);
        }
        return ensureFile(file);
    }

    public static final File ensureFile(final File file) throws FileException {
        try {
            file.createNewFile();
        } catch (final IOException e) {
            throw new FileException(file, e);
        }
        return file;
    }

    public static final File ensureWriteableFile(final File file) throws FileException {
        try {
            file.createNewFile();
        } catch (final IOException e) {
            throw new FileException(file, e);
        }
        if (!file.canWrite()) {
            throw new FileException(file, "Unwritable file");
        }
        return file;
    }

    public static String packageStringToFolderPathString(final String packageString) {
        return Jadoth.ensureCharAtEnd(packageString.replaceAll("\\.", "/"), '/');
    }

    public static final Appendable appendTextFromFile(final Appendable app, final File file) throws IOException {
        BufferedReader reader = null;
        String line;
        try {
            reader = new BufferedReader(new FileReader(file));
            if ((line = reader.readLine()) != null) {
                app.append(line);
                while ((line = reader.readLine()) != null) {
                    app.append('\n').append(line);
                }
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        return app;
    }

    public static final String readTextFromFile(final File file) throws IOException {
        return appendTextFromFile(LargeVarChar(), file).toString();
    }

    public static final String buildFilePath(final String... items) {
        return MediumVarChar().setListSeperator('/').list(items).toString();
    }

    public static final File buildFile(final String... items) {
        return new File(buildFilePath(items));
    }

    public static final File buildFile(final File parent, final String... items) {
        return new File(parent, buildFilePath(items));
    }

    public static void copyFile(final File in, final File out) throws IOException {
        final FileChannel inChannel = new FileInputStream(in).getChannel();
        final FileChannel outChannel = new FileOutputStream(out).getChannel();
        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } catch (final IOException e) {
            throw e;
        } finally {
            if (inChannel != null) {
                inChannel.close();
            }
            if (outChannel != null) {
                outChannel.close();
            }
        }
    }

    public static void closeSilent(final Closeable os) {
        if (os == null) {
            return;
        }
        try {
            os.close();
        } catch (final Throwable t) {
        }
    }
}
