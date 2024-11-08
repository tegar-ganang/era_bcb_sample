package fiji.updater.logic;

import fiji.updater.Updater;
import fiji.updater.util.Progress;
import fiji.updater.util.Progressable;
import ij.IJ;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

public class FileUploader extends Progressable {

    protected final String uploadDir;

    int total;

    public FileUploader() {
        this("/var/www/update/");
    }

    public FileUploader(String uploadDir) {
        this.uploadDir = uploadDir;
    }

    public void calculateTotalSize(List<SourceFile> sources) {
        total = 0;
        for (SourceFile source : sources) total += (int) source.getFilesize();
    }

    public synchronized void upload(List<SourceFile> sources) throws IOException {
        setTitle("Uploading");
        calculateTotalSize(sources);
        int count = 0;
        File lock = null;
        File db = new File(uploadDir + Updater.XML_COMPRESSED);
        byte[] buffer = new byte[65536];
        for (SourceFile source : sources) {
            File file = new File(uploadDir + source.getFilename());
            if (lock == null) lock = file;
            File dir = file.getParentFile();
            if (!dir.exists()) dir.mkdirs();
            OutputStream out = new FileOutputStream(file);
            InputStream in = source.getInputStream();
            addItem(source);
            int currentCount = 0;
            int currentTotal = (int) source.getFilesize();
            for (; ; ) {
                int read = in.read(buffer);
                if (read < 0) break;
                out.write(buffer, 0, read);
                currentCount += read;
                setItemCount(currentCount, currentTotal);
                setCount(count + currentCount, total);
            }
            in.close();
            out.close();
            count += currentCount;
            itemDone(source);
        }
        File backup = new File(db.getAbsolutePath() + ".old");
        if (backup.exists()) backup.delete();
        db.renameTo(backup);
        lock.renameTo(db);
        done();
    }

    public interface SourceFile {

        public String getFilename();

        public String getPermissions();

        public long getFilesize();

        public InputStream getInputStream() throws IOException;
    }
}
