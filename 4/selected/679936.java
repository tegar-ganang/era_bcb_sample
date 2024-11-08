package de.carne.fs.core.transfer;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.Future;

/**
 * <code>FileOutputStream</code> based <code>WritableByteChannel</code> implementation reporting write progress to a
 * <code>IncrementalExportListener</code>.
 */
public class ExportWritableByteChannel implements WritableByteChannel {

    private FileOutputStream out;

    private IncrementalExportListener listener;

    private Future<Void> task;

    /**
	 * Construct <code>ExportWritableByteChannel</code>.
	 * 
	 * @param out The <code>FileOutputStream</code> to write the data into.
	 * @param listener The <code>IncrementalExportListener</code> receiving the write progress.
	 * @param task The running export.
	 */
    public ExportWritableByteChannel(FileOutputStream out, IncrementalExportListener listener, Future<Void> task) {
        assert out != null;
        assert listener != null;
        assert task != null;
        this.out = out;
        this.listener = listener;
        this.task = task;
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        int write;
        if (!this.task.isCancelled() && !this.task.isDone()) {
            write = this.out.getChannel().write(src);
        } else {
            write = 0;
        }
        if (write > 0) {
            this.listener.exportProgress(write);
        }
        return write;
    }

    @Override
    public void close() throws IOException {
        this.out.close();
        this.out = null;
    }

    @Override
    public boolean isOpen() {
        return this.out != null;
    }
}
