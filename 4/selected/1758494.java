package jaxlib.io.channel;

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import javax.annotation.Nullable;

/**
 * @author  jw
 * @since   JaXLib 1.0
 * @version $Id: DecoFileChannel.java 2858 2010-12-30 06:33:16Z joerg_wassmer $
 */
public class DecoFileChannel extends FileChannel implements Serializable {

    /**
   * @since JaXLib 1.0
   */
    private static final long serialVersionUID = 1L;

    protected FileChannel delegate;

    public DecoFileChannel(@Nullable final FileChannel delegate) {
        super();
        this.delegate = delegate;
    }

    private FileChannel delegate() throws IOException {
        final FileChannel delegate = this.delegate;
        if (delegate == null) throw new ClosedChannelException();
        return delegate;
    }

    @Override
    protected void implCloseChannel() throws IOException {
        FileChannel delegate = this.delegate;
        if (delegate != null) {
            try {
                delegate.close();
                delegate = null;
                this.delegate = null;
            } finally {
                if ((delegate != null) && !delegate.isOpen()) this.delegate = null;
            }
        }
    }

    @Override
    public void force(final boolean metaData) throws IOException {
        delegate().force(metaData);
    }

    @Override
    public FileLock lock(final long position, final long size, final boolean shared) throws IOException {
        final FileLock lock = delegate().lock(position, size, shared);
        return (lock == null) ? null : new FileLockImpl(this, lock);
    }

    @Override
    public MappedByteBuffer map(final FileChannel.MapMode mode, final long position, final long size) throws IOException {
        return delegate().map(mode, position, size);
    }

    @Override
    public long position() throws IOException {
        return delegate().position();
    }

    @Override
    public FileChannel position(final long newPosition) throws IOException {
        delegate().position(newPosition);
        return this;
    }

    @Override
    public int read(final ByteBuffer dst) throws IOException {
        return delegate().read(dst);
    }

    @Override
    public int read(final ByteBuffer dst, final long position) throws IOException {
        return delegate().read(dst, position);
    }

    @Override
    public long read(final ByteBuffer[] dsts, final int offset, final int length) throws IOException {
        return delegate().read(dsts, offset, length);
    }

    @Override
    public long size() throws IOException {
        return delegate().size();
    }

    @Override
    public long transferFrom(final ReadableByteChannel src, final long position, final long count) throws IOException {
        return delegate().transferFrom(src, position, count);
    }

    @Override
    public long transferTo(final long position, final long count, final WritableByteChannel target) throws IOException {
        return delegate().transferTo(position, count, target);
    }

    @Override
    public FileChannel truncate(final long size) throws IOException {
        delegate().truncate(size);
        return this;
    }

    @Override
    public FileLock tryLock(final long position, final long size, final boolean shared) throws IOException {
        final FileLock lock = delegate().tryLock(position, size, shared);
        return (lock == null) ? null : new FileLockImpl(this, lock);
    }

    @Override
    public int write(final ByteBuffer src) throws IOException {
        return delegate().write(src);
    }

    @Override
    public int write(final ByteBuffer src, final long position) throws IOException {
        return delegate().write(src, position);
    }

    @Override
    public long write(final ByteBuffer[] src, final int offset, final int length) throws IOException {
        return delegate().write(src, offset, length);
    }

    private static final class FileLockImpl extends FileLock {

        private FileLock delegate;

        FileLockImpl(final FileChannel owner, final FileLock delegate) {
            super(owner, delegate.position(), delegate.size(), delegate.isShared());
            this.delegate = delegate;
        }

        @Override
        public final boolean isValid() {
            final FileLock delegate = this.delegate;
            if (delegate == null) return false;
            final boolean valid = delegate.isValid();
            if (!valid) this.delegate = null;
            return valid;
        }

        @Override
        public final void release() throws IOException {
            FileLock delegate = this.delegate;
            if (delegate == null) return;
            try {
                delegate.release();
                delegate = null;
                this.delegate = null;
            } finally {
                if ((delegate != null) && !delegate.isValid()) this.delegate = null;
            }
        }
    }
}
