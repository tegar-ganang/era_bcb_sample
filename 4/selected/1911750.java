package net.sf.cotta;

import java.io.*;

public class FileIo {

    private FileSystem fileSystem;

    private TPath path;

    private static final int MAGIC_NUMBER = 16;

    public static final Mode APPEND = new Mode();

    public static final Mode OVERWRITE = new Mode();

    private static final Mode DEFAULT_MODE = OVERWRITE;

    public FileIo(FileSystem fileSystem, TPath path) {
        this.fileSystem = fileSystem;
        this.path = path;
    }

    public InputStream inputStream() throws TIoException {
        return fileSystem.createInputStream(path);
    }

    public OutputStream outputStream() throws TIoException {
        return outputStream(DEFAULT_MODE);
    }

    private OutputStream outputStream(Mode mode) throws TIoException {
        if (!fileSystem.dirExists(path.parent())) {
            fileSystem.createDir(path.parent());
        }
        return fileSystem.createOutputStream(path, mode);
    }

    public void save(String content) throws TIoException {
        OutputStreamWriter writer = outputStreamWriter();
        try {
            writeBare(writer, content);
        } catch (IOException e) {
            throw new TIoException(path, "saving content", e);
        }
    }

    private void writeBare(OutputStreamWriter writer, String content) throws IOException {
        try {
            writer.write(content);
            writer.flush();
        } finally {
            writer.close();
        }
    }

    private OutputStreamWriter outputStreamWriter() throws TIoException {
        return new OutputStreamWriter(outputStream());
    }

    public String load() throws TIoException {
        InputStreamReader reader = inputStreamReader();
        return loadContent(reader, path);
    }

    public static String loadContent(InputStreamReader reader, TPath path) throws TIoException {
        StringBuffer content = new StringBuffer();
        try {
            loadBare(content, reader);
        } catch (IOException e) {
            throw new TIoException(path, "Loading content");
        }
        return content.toString();
    }

    private static void loadBare(StringBuffer content, InputStreamReader reader) throws IOException {
        try {
            char[] buffer = new char[MAGIC_NUMBER];
            int read = 0;
            while (read != -1) {
                content.append(buffer, 0, read);
                read = reader.read(buffer, 0, buffer.length);
            }
        } finally {
            reader.close();
        }
    }

    private InputStreamReader inputStreamReader() throws TIoException {
        return new InputStreamReader(inputStream());
    }

    public Writer writer() throws TIoException {
        return writer(OVERWRITE);
    }

    public Writer writer(Mode mode) throws TIoException {
        return new OutputStreamWriter(outputStream(mode));
    }

    public Reader reader() throws TIoException {
        return new InputStreamReader(inputStream());
    }

    public BufferedReader bufferedReader() throws TIoException {
        return new BufferedReader(reader());
    }

    public static void copy(InputStream is, OutputStream os) throws IOException {
        byte[] buffer = new byte[256];
        int read = is.read(buffer, 0, buffer.length);
        while (read > -1) {
            os.write(buffer, 0, read);
            read = is.read(buffer, 0, buffer.length);
        }
    }

    public static class Mode {

        private Mode() {
        }
    }
}
