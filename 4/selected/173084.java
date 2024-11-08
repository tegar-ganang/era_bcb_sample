/*
 * SwapBuffer.java
 *
 * Created on May 5, 2006, 11:11 PM
 *
 * Copyright (c) Joerg Wassmer
 * This library is free software. You can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 2 or above
 * as published by the Free Software Foundation.
 * For more information please visit <http://jaxlib.sourceforge.net>.
 */

package jaxlib.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import jaxlib.io.file.Files;
import jaxlib.io.stream.BufferedXOutputStream;
import jaxlib.io.stream.ByteBufferInputStream;
import jaxlib.io.stream.IOStreams;
import jaxlib.lang.Exceptions;
import jaxlib.text.DateFormatISO8601;
import jaxlib.util.CheckArg;
import jaxlib.util.CheckBounds;
import jaxlib.util.Strings;


/**
 * @deprecated Will be removed, no replacement.
 *
 * Byte buffer swapping out to disk when a given memory limit has been reached.
 *
 * @author  <a href="mailto:joerg.wassmer@web.de">Joerg Wassmer</a>
 * @since   JaXLib 1.0
 * @version $Id: SwapBuffer.java 3029 2011-12-29 00:36:48Z joerg_wassmer $
 */
@Deprecated
public class SwapBuffer extends Object
{

  private final int maxMemory;
  private final AtomicReference<Content> content;
  private final AtomicLong sequenceNumber;


  public SwapBuffer()
  {
    this(4096);
  }


  public SwapBuffer(final int maxMemory)
  {
    super();

    CheckArg.notNegative(maxMemory, "maxMemory");
    this.maxMemory = maxMemory;
    this.content = new AtomicReference<>();
    this.sequenceNumber = new AtomicLong();
  }



  protected File newFile() throws IOException
  {
    return Files.createTempFile();
  }



  final void update(Content content) throws IOException
  {
    content = this.content.getAndSet(content);
    if (content != null)
      content.detach();
  }



  public void clear() throws IOException
  {
    update(null);
  }



  public boolean discard(final Content content) throws IOException
  {
    CheckArg.notNull(content, "content");

    if (this.content.compareAndSet(content, null))
    {
      content.detach();
      return true;
    }
    else
    {
      return false;
    }
  }



  public Content getContent()
  {
    return this.content.get();
  }



  public SwapBuffer.Output newContent() throws IOException
  {
    return new Output(this, this.maxMemory);
  }





  public static abstract class Content extends Object
  {

    final long sequenceNumber;
    final long size;
    final long timestamp;


    Content(SwapBuffer owner, long size, long timestamp)
    {
      super();

      this.sequenceNumber = owner.sequenceNumber.incrementAndGet();
      this.size           = size;
      this.timestamp      = timestamp;
    }


    abstract void close() throws IOException;

    abstract void detach() throws IOException;

    public abstract ContentChannel openChannel() throws IOException;


    public final long getSequenceNumber()
    {
      return this.sequenceNumber;
    }


    public final long getSize()
    {
      return this.size;
    }


    public final long getTimestamp()
    {
      return this.timestamp;
    }
  }




  public static abstract class ContentChannel extends Object implements Channel
  {

    ContentChannel()
    {
      super();
    }


    @Override
    public abstract void close() throws IOException;

    public abstract ByteBuffer getBytes() throws IOException;

    public abstract ByteBuffer getBytes(long offs, int len) throws IOException;

    public abstract InputStream getInputStream() throws IOException;

    public abstract InputStream getInputStream(long offs) throws IOException;

    public abstract boolean isBuffered() throws IOException;

    @Override
    public abstract boolean isOpen();

    public abstract byte[] toByteArray() throws IOException;

    public abstract byte[] toByteArray(long offs, int len) throws IOException;

  }




  private static final class ContentInFile extends Content
  {

    private volatile boolean closed;
    final File file;
    private FileChannel fileChannel;
    private volatile MappedByteBuffer buffer;
    private final AtomicInteger openChannels;


    ContentInFile(SwapBuffer owner, File file, long size, long timestamp)
    {
      super(owner, size, timestamp);

      this.file = file;
      this.openChannels = new AtomicInteger(1); // one for SwapBuffer instance
    }


    private void lockFile(final FileChannel fc) throws IOException
    {
      if (fc.tryLock(0, Long.MAX_VALUE, true) == null)
        throw new IOException("file is exclusively locked by another process: " + this.file);

      final long lastModified = this.file.lastModified();
      if (this.timestamp != lastModified)
      {
        final DateFormatISO8601 df = DateFormatISO8601.getInstance(DateFormatISO8601.COMPLETE_WITH_SPACES);
        throw new IOException(Strings.concat(
          "File has been modified: ", this.file.getPath(),
          "\n  expected timestamp = ", df.format(this.timestamp),
          "\n  actual timestamp   = ", df.format(lastModified)
        ));
      }

      final long size = fc.size();
      if (this.size != size)
      {
        throw new IOException(Strings.concat(
          "File has been modified: ", this.file.getPath(),
          "\n  expected size     = ", String.valueOf(this.size),
          "\n  actual size       = ", String.valueOf(size)
        ));
      }
    }



    final void channelClosed() throws IOException
    {
      if (this.openChannels.decrementAndGet() == 0)
      {
        FileChannel fc = null;

        synchronized (this)
        {
          if (this.openChannels.get() == 0)
          {
            fc = this.fileChannel;
            this.fileChannel = null;
            this.buffer = null;
          }
        }

        if (fc != null)
          fc.close();
      }
    }


    @Override
    @SuppressWarnings("finally")
    final void close() throws IOException
    {
      FileChannel fc;

      synchronized (this)
      {
        if (this.closed)
          return;

        this.closed = true;
        this.buffer = null;
        fc = this.fileChannel;
        this.fileChannel = null;
      }

      if (fc != null)
      {
        try
        {
          fc.close();
        }
        catch (final IOException ex)
        {
          try
          {
            this.file.delete();
          }
          finally
          {
            throw ex;
          }
        }
      }

      this.file.delete();
    }


    @Override
    final void detach() throws IOException
    {
      channelClosed();
    }



    final synchronized FileChannel getFileChannel() throws IOException
    {
      return getFileChannel0();
    }


    final FileChannel getFileChannel0() throws IOException
    {
      if (this.closed)
        throw new ClosedChannelException();

      if (this.fileChannel != null)
        return this.fileChannel;

      final RandomAccessFile f = new RandomAccessFile(this.file, "r");
      try
      {
        final FileChannel fc = f.getChannel();
        lockFile(fc);
        this.fileChannel = fc;
        return fc;
      }
      catch (final Throwable ex)
      {
        IO.tryClose(f);
        throw Exceptions.rethrow(ex);
      }
    }



    /**
     * Get stream for files > Integer.MAX_VALUE
     */
    final FileInputStream getFileInputStream() throws IOException
    {
      if (this.closed)
        throw new ClosedChannelException();

      final FileInputStream f = new FileInputStream(this.file);
      try
      {
        lockFile(f.getChannel());
        return f;
      }
      catch (final Throwable ex)
      {
        IO.tryClose(f);
        throw Exceptions.rethrow(ex);
      }
    }




    /**
     * Get buffer for files <= Integer.MAX_VALUE
     */
    final MappedByteBuffer getInternalBuffer() throws IOException
    {
      MappedByteBuffer buffer = this.buffer;
      if (buffer != null)
      {
        if (this.closed)
          throw new ClosedChannelException();
        return buffer;
      }

      synchronized (this)
      {
        buffer = this.buffer;
        if (buffer != null)
          return buffer;

        FileChannel fc = getFileChannel0();
        try
        {
          this.buffer = buffer = fc.map(FileChannel.MapMode.READ_ONLY, 0, this.size);
          return buffer;
        }
        catch (final Exception ex)
        {
          this.fileChannel = null;
          IO.tryClose(fc);
          throw Exceptions.rethrow(ex);
        }
      }
    }



    @Override
    protected synchronized void finalize() throws Throwable
    {
      close();
    }


    @Override
    public ContentChannel openChannel() throws IOException
    {
      this.openChannels.incrementAndGet();
      return new ContentInFile.ContentChannelImpl(this);
    }





    private static final class ContentChannelImpl extends ContentChannel
    {

      private volatile ContentInFile owner;


      ContentChannelImpl(ContentInFile owner)
      {
        super();

        this.owner = owner;
      }


      private ContentInFile ensureOpen() throws IOException
      {
        ContentInFile owner = this.owner;
        if (owner != null)
          return owner;
        else
          throw new ClosedChannelException();
      }


      @Override
      protected void finalize() throws Throwable
      {
        close();
      }


      @Override
      public final void close() throws IOException
      {
        ContentInFile owner;

        synchronized (this)
        {
          owner = this.owner;
          if (owner == null)
            return;
          this.owner = null;
        }

        owner.channelClosed();
      }


      @Override
      public final ByteBuffer getBytes() throws IOException
      {
        final ContentInFile owner = ensureOpen();
        if (owner.size <= Integer.MAX_VALUE)
          return owner.getInternalBuffer().duplicate();
        else
          throw new IOException("file size exceeds Integer.MAX_VALUE bytes");
      }


      @Override
      public final ByteBuffer getBytes(final long offs, final int len) throws IOException
      {
        final ContentInFile owner = ensureOpen();
        CheckBounds.offset(owner.size, offs, len);
        if (len == 0)
          return ByteBuffer.allocate(0);

        if (owner.size <= Integer.MAX_VALUE)
        {
          ByteBuffer buffer = owner.getInternalBuffer();
          buffer.position((int) offs).limit((int) (offs + len));
          return buffer.slice();
        }
        else
        {
          return owner.getFileChannel().map(FileChannel.MapMode.READ_ONLY, offs, len);
        }
      }


      @Override
      public final InputStream getInputStream() throws IOException
      {
        final ContentInFile owner = ensureOpen();
        if (owner.size <= Integer.MAX_VALUE)
          return new ByteBufferInputStream(owner.getInternalBuffer().duplicate());
        else
          return owner.getFileInputStream();
      }


      @Override
      public final InputStream getInputStream(final long offs) throws IOException
      {
        final ContentInFile owner = ensureOpen();
        CheckBounds.offset(owner.size, offs, owner.size);

        if (owner.size <= Integer.MAX_VALUE)
        {
          final ByteBuffer buffer = owner.getInternalBuffer().duplicate();
          buffer.position((int) offs);
          return new ByteBufferInputStream(buffer);
        }
        else
        {
          final FileInputStream in = owner.getFileInputStream();
          IOStreams.skipFully(in, offs);
          return in;
        }
      }


      @Override
      public final boolean isBuffered() throws IOException
      {
        return ensureOpen().size <= Integer.MAX_VALUE;
      }


      @Override
      public final boolean isOpen()
      {
        final ContentInFile owner = this.owner;
        return (owner != null) && !owner.closed;
      }


      @Override
      public final byte[] toByteArray() throws IOException
      {
        final ContentInFile owner = ensureOpen();
        if (owner.size > Integer.MAX_VALUE)
          throw new IOException("file size exceeds Integer.MAX_VALUE bytes");

        final ByteBuffer buffer = owner.getInternalBuffer().duplicate();
        final byte[] a = new byte[buffer.remaining()];
        buffer.get(a);
        return a;
      }


      @Override
      public final byte[] toByteArray(final long offs, final int len) throws IOException
      {
        final ContentInFile owner = ensureOpen();
        CheckBounds.offset(owner.size, offs, len);
        if (len == 0)
          return new byte[0];

        if (owner.size <= Integer.MAX_VALUE)
        {
          final ByteBuffer buffer = owner.getInternalBuffer().duplicate();
          buffer.position((int) offs);
          final byte[] a = new byte[len];
          buffer.get(a);
          return a;
        }
        else
        {
          final FileChannel fc = owner.getFileChannel();
          final ByteBuffer a = ByteBuffer.allocate(len);
          while (a.hasRemaining())
          {
            if (fc.read(a, offs + a.position()) < 0)
              throw new IOException("file has been modified concurrently");
          }
          return a.array();
        }
      }

    }

  }








  private static final class ContentInMemory extends Content
  {

    private volatile ByteBuffer buffer;


    ContentInMemory(SwapBuffer owner, long timestamp, ByteBuffer buffer)
    {
      super(owner, buffer.remaining(), timestamp);
      this.buffer = buffer;
    }


    @Override
    final void close()
    {
      this.buffer = null;
    }


    @Override
    final void detach()
    {
      // nop
    }


    @Override
    public ContentChannel openChannel() throws IOException
    {
      final ByteBuffer buffer = this.buffer;
      if (buffer != null)
        return new ContentChannelImpl(buffer);
      else
        throw new ClosedChannelException();
    }




    private static final class ContentChannelImpl extends ContentChannel
    {

      volatile ByteBuffer buffer;


      ContentChannelImpl(ByteBuffer buffer)
      {
        super();
        this.buffer = buffer;
      }


      private ByteBuffer ensureOpen() throws IOException
      {
        final ByteBuffer buffer = this.buffer;
        if (buffer != null)
          return buffer;
        else
          throw new ClosedChannelException();
      }


      @Override
      public final void close()
      {
        this.buffer = null;
      }


      @Override
      public final ByteBuffer getBytes() throws IOException
      {
        return ensureOpen().asReadOnlyBuffer();
      }


      @Override
      public final ByteBuffer getBytes(final long offs, final int len) throws IOException
      {
        final ByteBuffer buffer = ensureOpen().asReadOnlyBuffer();
        CheckBounds.offset(buffer.remaining(), offs, len);
        buffer.position((int) offs).limit((int) (offs + len));
        return buffer.slice();
      }


      @Override
      public final InputStream getInputStream() throws IOException
      {
        return new ByteBufferInputStream(ensureOpen().duplicate());
      }


      @Override
      public final InputStream getInputStream(final long offs) throws IOException
      {
        final ByteBuffer buffer = ensureOpen().duplicate();
        CheckBounds.offset(buffer.remaining(), offs, buffer.remaining());
        buffer.position((int) offs);
        return new ByteBufferInputStream(buffer);
      }


      @Override
      public final boolean isBuffered()
      {
        return true;
      }


      @Override
      public final boolean isOpen()
      {
        return this.buffer != null;
      }


      @Override
      public final byte[] toByteArray() throws IOException
      {
        return ensureOpen().array().clone();
      }


      @Override
      public final byte[] toByteArray(final long offs, final int len) throws IOException
      {
        final ByteBuffer buffer = ensureOpen().duplicate();
        CheckBounds.offset(buffer.remaining(), offs, len);
        buffer.position((int) offs);
        final byte[] a = new byte[len];
        buffer.get(a);
        return a;
      }
    }
  }



/*


  private static final class FileChannelImpl extends DecoFileChannel
  {

    private Closeable owner;


    FileChannelImpl(final Closeable owner, final FileChannel delegate)
    {
      super(delegate);
      this.owner = owner;
    }


    final void closed()
    {
      this.owner     = null;
      super.delegate = null;
    }


    @Override
    protected void implCloseChannel() throws IOException
    {
      try
      {
        super.implCloseChannel();
      }
      finally
      {
        final Closeable owner = this.owner;
        if (owner != null)
        {
          this.owner = null;
          owner.close();
        }
      }
    }
  }





  private static final class FileInputStreamImpl extends FileInputStream
  {

    private volatile FileChannelImpl channel;
    private BufferFile owner;


    FileInputStreamImpl(final BufferFile owner) throws IOException
    {
      super(owner.file);
      this.owner = owner;
    }



    @Override
    public final synchronized void close() throws IOException
    {
      BufferFile owner = this.owner;
      if (owner == null)
        return;

      try
      {
        this.owner = null;
        super.close();
        close0(owner);
        owner = null;
      }
      finally
      {
        if (owner != null)
        {
          if (super.getChannel().isOpen())
            this.owner = owner;
          else
            close0(owner);
        }
      }
    }


    private void close0(final BufferFile owner)
    {
      this.owner = null;
      FileChannelImpl channel = this.channel;
      if (channel != null)
        channel.closed();
      owner.closedInput();
    }


    @Override
    public final FileChannel getChannel()
    {
      FileChannelImpl channel = this.channel;
      if (channel == null)
      {
        synchronized (this)
        {
          channel = this.channel;
          if (channel == null)
          {
            channel = new FileChannelImpl(this, super.getChannel());
            this.channel = channel;
            if (this.owner == null)
              channel.closed();
          }
        }
      }
      return channel;
    }

  }

  */




  public static final class Output extends BufferedXOutputStream implements WritableByteChannel
  {

    private SwapBuffer owner;
    private File file;


    Output(final SwapBuffer owner, int maxMemory)
    {
      super(null, maxMemory);
      this.owner = owner;
    }


    private void deleteFile()
    {
      File file = this.file;
      if (file != null)
      {
        this.file = null;
        file.delete();
      }
    }


    @Override
    @SuppressWarnings("finally")
    protected synchronized OutputStream createOut() throws IOException
    {
      SwapBuffer owner = this.owner;
      if (owner == null)
        throw new ClosedChannelException();

      try
      {
        final File file = owner.newFile();
        FileOutputStream out = new FileOutputStream(file);
        this.file = file;
        return out;
      }
      catch (final IOException ex)
      {
        try
        {
          close();
        }
        finally
        {
          throw ex;
        }
      }
    }


    @Override
    @SuppressWarnings("finally")
    public synchronized void closeInstance() throws IOException
    {
      try
      {
        super.close();
        deleteFile();
      }
      catch (final IOException ex)
      {
        try
        {
          deleteFile();
        }
        finally
        {
          throw ex;
        }
      }
      finally
      {
        this.owner = null;
      }
    }



    @SuppressWarnings("finally")
    public synchronized void commit() throws IOException
    {
      final SwapBuffer owner = this.owner;
      if (owner == null)
        throw new ClosedChannelException();

      FileOutputStream out = (FileOutputStream) getOut();

      if (out != null)
      {
        final File file = this.file;
        try
        {
          flush();
          FileChannel fc = out.getChannel();
          final long size = fc.size();
          fc.force(true);
          fc = null;
          out = null;

          this.file = null;
          close();

          owner.update(new ContentInFile(owner, file, size, file.lastModified()));
        }
        catch (final IOException ex)
        {
          this.file = file;
          try
          {
            close();
          }
          finally
          {
            throw ex;
          }
        }
        finally
        {
          this.owner = null;
        }
      }
      else
      {
        this.owner = null;
        ByteBuffer memoryBuffer = getBuffer();
        close();
        memoryBuffer.flip();

        if (memoryBuffer.remaining() != memoryBuffer.capacity())
        {
          ByteBuffer newBuffer = ByteBuffer.allocate(memoryBuffer.remaining());
          newBuffer.put(memoryBuffer);
          memoryBuffer = newBuffer;
          newBuffer.flip();
        }

        owner.update(new ContentInMemory(owner, System.currentTimeMillis(), memoryBuffer.duplicate()));
      }
    }


    @Override
    public final void flushBuffer() throws IOException
    {
      if (this.file != null)
        super.flushBuffer();
      else if (!isOpen())
        throw new IOException("closed");
    }

  }







}
