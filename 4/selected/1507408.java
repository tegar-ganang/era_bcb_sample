package com.c4j.filetools;

import static java.lang.String.format;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.channels.FileChannel;
import com.c4j.sre.C4JException;
import com.c4j.sre.C4JRuntimeException;
import com.c4j.sre.IMain;

final class FileToolsImpl extends FileToolsBase implements IFileTools {

    private static final String SEARCH_CLASS = "com.c4j.sre.C4JException";

    private static File sreRootpath = null;

    private static File sreClasspath = null;

    private static File sreLibrary = null;

    private static final int BLOCK_SIZE = 8192;

    private static void findSRE() {
        final URL url = IMain.class.getClassLoader().getResource(SEARCH_CLASS.replace('.', '/') + ".class");
        if (url == null) throw new C4JRuntimeException(format("Could not find class ‘%s’. If you renamed or moved it, please adjust " + "the constant 'SEARCH_CLASS in C4JRuntime.", SEARCH_CLASS));
        String urlstring = url.getPath().replaceAll("%20", " ");
        if (urlstring.startsWith("file:")) urlstring = urlstring.substring("file:".length(), urlstring.length());
        final int bang = urlstring.lastIndexOf('!');
        if (bang == -1) {
            sreRootpath = new File("/home/koethnig/c4jworkspace/c4j/Runtime");
            sreClasspath = new File(sreRootpath, "binary");
            sreLibrary = new File(sreRootpath, RUNTIME_JAR);
        } else {
            final int jarslash = urlstring.lastIndexOf('/', bang);
            sreRootpath = new File(urlstring.substring(0, jarslash));
            sreClasspath = new File(new File(urlstring.substring(0, jarslash)), RUNTIME_JAR);
            sreLibrary = sreClasspath;
        }
    }

    /**
     * Constructs a new instance of the appropriate component with the given name.
     *
     * @param instanceName
     *         the name of the instance.
     */
    public FileToolsImpl(final String instanceName) {
        super(instanceName);
    }

    @Override
    protected IFileTools provide_filetools() {
        return this;
    }

    @Override
    public void copyFile2File(final File src, final File dest, final boolean force) throws C4JException {
        if (dest.exists()) if (force && !dest.delete()) throw new C4JException(format("Copying ‘%s’ to ‘%s’ failed; cannot overwrite existing file.", src.getPath(), dest.getPath()));
        FileChannel inChannel = null;
        FileChannel outChannel = null;
        try {
            inChannel = new FileInputStream(src).getChannel();
            outChannel = new FileOutputStream(dest).getChannel();
            inChannel.transferTo(0, inChannel.size(), outChannel);
            if (src.canExecute()) dest.setExecutable(true, false);
        } catch (final IOException e) {
            throw new C4JException(format("Could not copy ‘%s’ to ‘%s’.", src.getPath(), dest.getPath()), e);
        } finally {
            if (inChannel != null) try {
                try {
                    inChannel.close();
                } catch (final IOException e) {
                    throw new C4JException(format("Could not close input stream for ‘%s’.", src.getPath()), e);
                }
            } finally {
                if (outChannel != null) try {
                    outChannel.close();
                } catch (final IOException e) {
                    throw new C4JException(format("Could not close output stream for ‘%s’.", dest.getPath()), e);
                }
            }
        }
    }

    @Override
    public void copyStream2Stream(final InputStream in, final OutputStream out) throws IOException {
        final byte[] buffer = new byte[BLOCK_SIZE];
        int read = 0;
        while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
    }

    @Override
    public File getSREClasspath() {
        if (sreClasspath == null) findSRE();
        return sreClasspath;
    }

    @Override
    public File getSRELibrary() {
        if (sreLibrary == null) findSRE();
        return sreLibrary;
    }

    @Override
    public String readFile(final File file) {
        if (!file.exists()) return null;
        try {
            final BufferedReader reader = new BufferedReader(new FileReader(file));
            final StringBuffer result = new StringBuffer();
            try {
                String x = reader.readLine();
                while (x != null) {
                    result.append(x + "\n");
                    x = reader.readLine();
                }
                result.deleteCharAt(result.length() - 1);
            } catch (final IOException e) {
                return null;
            } finally {
                reader.close();
            }
            return result.toString();
        } catch (final IOException e) {
            return null;
        }
    }

    @Override
    public void saveFile(final File file, final String content) throws C4JException {
        try {
            final PrintWriter pr = new PrintWriter(file);
            pr.write(content);
            pr.close();
        } catch (final FileNotFoundException e) {
            throw new C4JException(format("Could not write file ‘%s’.", file.getPath()));
        }
    }

    @Override
    public boolean deleteFolder(final File folder) {
        if (folder.isDirectory()) for (final File file : folder.listFiles()) deleteFolder(file);
        return folder.delete();
    }
}
