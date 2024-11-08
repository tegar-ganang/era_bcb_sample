package annone.http;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import annone.util.Checks;
import annone.util.EmptyInputStream;
import annone.util.Nullable;

public class HttpFileContent implements HttpContent {

    protected final File target;

    private RandomAccessFile open;

    private FileLock lock;

    private final String type;

    private final String encoding;

    public HttpFileContent(File target, @Nullable String type, @Nullable String encoding) {
        Checks.notNull("target", target);
        this.target = target;
        this.type = type;
        this.encoding = encoding;
        try {
            this.open = new RandomAccessFile(target, "r");
            this.lock = open.getChannel().tryLock();
        } catch (IOException xp) {
        }
    }

    @Override
    protected void finalize() throws Throwable {
        if (lock != null) lock.release();
        if (open != null) open.close();
    }

    @Override
    public long getLength() {
        return target.length();
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getEncoding() {
        return encoding;
    }

    @Override
    public InputStream getInputStream() {
        try {
            return new FileInputStream(target);
        } catch (FileNotFoundException xp) {
            return new EmptyInputStream();
        }
    }
}
