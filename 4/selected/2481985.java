/*
 * DenseFileCache.java
 *
 * Created on November 26, 2005, 1:53 PM
 *
 * Copyright (c) Joerg Wassmer
 * This library is free software. You can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 2 or above
 * as published by the Free Software Foundation.
 * For more information please visit <http://jaxlib.sourceforge.net>.
 */

package jaxlib.cache;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import jaxlib.arc.zip.DeflatedOutputStream;
import jaxlib.arc.zip.DeflaterProperties;
import jaxlib.arc.zip.InflatedInputStream;
import jaxlib.col.concurrent.ConcurrentHashXMap;
import jaxlib.io.file.Files;
import jaxlib.io.stream.BufferedXOutputStream;
import jaxlib.io.stream.ByteBufferInputStream;
import jaxlib.io.stream.NullInputStream;
import jaxlib.io.stream.XOutputStream;
import jaxlib.io.stream.objects.ClassLoaderObjectInputStream;
import jaxlib.ref.UnmodifiableWeakReference;
import jaxlib.util.CheckArg;
import jaxlib.util.sorting.TunedQuickSort;


/**
 * A cache storing all objects in a single file.
 * <p>
 * The cache handles a single file. If an object is not present in memory, but the key is, then the object
 * is read from the file cache and moved to the temporar memory cache represented by a {@link SoftReference}.
 * </p><p>
 * This class has been designed for good concurrency. Multiple threads can concurrently retrieve objects
 * from the cache. The {@link #clear()}, {@link #put(Object,Object)} and {@link #remove(Object()} operations
 * are single threaded, blocking all other calls to the cache instance.
 * </p><p>
 * Performance and the level of concurrency is highly system dependent. It depends on the speed of the
 * file system, the amount of memory, and on the {@link MappedByteBuffer} implementation of the Java VM.
 * </p><p>
 * As a consequence of the concurrency support, {@link CacheEntryValidator validation} of a particular cached
 * object may also happen several times simultaneously. However, the cache guarentees that the most up to
 * date validation result wins. The validator may synchronize onto a particular cache entry to avoid multiple
 * validations of the same object at the same time. Threads accessing other entries will not be blocked.
 * </p><p>
 * This cache implementation is limited to a file size of {@link Integer#MAX_VALUE Integer.MAX_VALUE}
 * bytes.
 * </p>
 *
 * @see DenseFileCacheProperties
 *
 * @author  <a href="mailto:joerg.wassmer@web.de">Joerg Wassmer</a>
 * @version $Id: DenseFileCache.java 3016 2011-11-28 06:17:26Z joerg_wassmer $
 * @since JaXLib 1.0
 */
@SuppressWarnings("element-type-mismatch")
public class DenseFileCache<K,V> extends Cache<K,V>
{

  /**
   * Used as file lock indicator inside same VM.
   * Filelocks are working only over processes, not over threads of same VM.
   */
  private static final long VM_LOCK_ID = Double.doubleToLongBits(Math.random() * Long.MAX_VALUE);

  private static final int ENTRY_HEADER_LENGTH  = 4;
  private static final int FILE_HEADER_LENGTH   = 4 + 4 + 1 + 8;
  private static final int MAGIC                = 0xCACEBABE;
  private static final int VERSION              = 0;

  private static final int FLAG_CLEAN_SHUTDOWN  = 1;
  private static final int FLAG_COMPRESSED      = 2;
  private static final int FLAG_LOCKED          = 4;

  private final     DefaultCacheStatistics                        cacheStatistics;
  private           ClassLoader                                   classloader;
  private volatile  int                                           deletedLength;
  private final     boolean                                       deleteOnClose;
  private           RandomAccessFile                              file;
  private volatile  MappedByteBuffer                              fileBuffer;
  private           FileChannel                                   fileChannel;
  private volatile  int                                           fileLength;
  private           DenseFileCache.GrowingFileBufferOutputStream  fileOut;
  private final     Entry<K,V>                                    head = new Entry<>();
  private final     ReentrantReadWriteLock                        lock;
  private           ConcurrentHashXMap<K,Entry<K,V>>              map;
  private           XOutputStream                                 out;
  private final     File                                          path;
  private final     DenseFileCacheProperties                      properties;
  private final     ReentrantReadWriteLock.ReadLock               readLock;
  private final     ReentrantReadWriteLock.WriteLock              writeLock;




  @SuppressWarnings("finally")
  public DenseFileCache(
    CacheSystem cacheSystem, String name, DenseFileCacheProperties properties, ClassLoader classloader
  )
  throws IOException
  {
    super(cacheSystem, name, properties);

    properties = (DenseFileCacheProperties) super.properties;

    this.classloader = classloader;
    this.properties  = properties;
    File path        = properties.getPath();

    if (path == null)
    {
      path = Files.createTempFile(name, ".cache").toFile();
      path.deleteOnExit();
      this.deleteOnClose = true;
    }
    else if (path.isDirectory())
    {
      path = Files.createTempFile(path.toPath(), name, ".cache").toFile();
      path.deleteOnExit();
      this.deleteOnClose = true;
    }
    else if (path.isFile())
    {
      this.deleteOnClose = properties.deleteFileOnClose;
    }
    else if (properties.deleteFileOnClose)
    {
      Files.mkdirs(path);
      path = Files.createTempFile(path.toPath(), name, ".cache").toFile();
      path.deleteOnExit();
      this.deleteOnClose = true;
    }
    else
    {
      final File parent = path.getParentFile();
      if (parent != null)
        Files.mkdirs(parent);
      path.createNewFile();
      this.deleteOnClose = false;
    }

    this.path = path;

    this.map = new ConcurrentHashXMap<>(1);
    final RandomAccessFile file = Files.openLockedRandomAccessFile(path, "rw", 0, TimeUnit.MILLISECONDS);
    this.file = file;

    final long existingFileLength = this.file.length();
    if (existingFileLength != 0)
    {
      file.seek(0);

      if
      (
           (existingFileLength < FILE_HEADER_LENGTH)
        || (file.readInt() != MAGIC)
      )
      {
        try
        {
          file.close();
        }
        finally
        {
          throw new IOException(
            "File was existing and either is corrupted or has not bean created by this class:\n" + path
          );
        }
      }
      else if (properties.persistent && (file.readInt() == VERSION))
      {
        final int flags     = file.read();
        final long vmLockId = file.readLong();

        if (vmLockId == VM_LOCK_ID)
        {
          try
          {
            file.close();
          }
          finally
          {
            throw new IOException("File already is in use by another thread:\n" + path);
          }
        }
        else if ((flags & FLAG_CLEAN_SHUTDOWN) != 0) // clean shutdown?
        {
          if (((flags & FLAG_COMPRESSED) != 0) == (properties.compressionLevel > 0))
          {
            // TODO: read persistent entries into map
          }
        }
        else
        {
          // log a warning
        }
      }
    }

    file.seek(0);
    file.writeInt(MAGIC);
    file.writeInt(VERSION);

    // flags - clear clean shutdown flag
    int flags = 0;
    if (properties.compressionLevel > 0)
      flags |= FLAG_COMPRESSED;
    file.writeInt(flags);

    // set lock id (clear on clean shutdown)
    file.writeLong(VM_LOCK_ID);

    this.fileLength = (int) file.getFilePointer();
    assert (this.fileLength == FILE_HEADER_LENGTH);
    this.fileChannel = file.getChannel();

    this.fileOut = new GrowingFileBufferOutputStream();
    this.fileOut.position(this.fileLength);

    if (properties.compressionLevel <= 0)
    {
      this.out = fileOut;
    }
    else
    {
      this.out = new DenseFileCache.NonClosingBufferedOutputStream(
        new DenseFileCache.NonClosingDeflatedOutputStream(this.fileOut, properties.compressionLevel)
      );
    }

    this.cacheStatistics  = new DefaultCacheStatistics();
    this.lock             = new ReentrantReadWriteLock(properties.fair);
    this.readLock         = this.lock.readLock();
    this.writeLock        = this.lock.writeLock();
  }







  private void beginRead()
  {
    this.readLock.lock();
  }


  private void beginWrite()
  {
    this.writeLock.lock();
  }


  private ObjectInputStream createObjectInputStream(final ByteBuffer buffer) throws IOException
  {
    ClassLoader classloader = this.classloader;
    if (classloader == null)
      classloader = Thread.currentThread().getContextClassLoader();

    final InputStream bin = new ByteBufferInputStream(buffer);
    final InputStream in = (this.properties.compressionLevel == 0)
      ? bin
      : DenseFileCache.InflatedInputStreamImpl.getSharedInstance(bin);

    return new ClassLoaderObjectInputStream(in, classloader);
  }


  private void endRead()
  {
    this.readLock.unlock();
  }


  private void endWrite()
  {
    this.writeLock.unlock();
  }


  private RandomAccessFile ensureFileOpen() throws IOException
  {
    final RandomAccessFile file = this.file;
    if (file != null)
      return file;
    else
      throw new ClosedChannelException();
  }


  private FileChannel ensureOpen() throws IOException
  {
    final FileChannel fileChannel = this.fileChannel;
    if (fileChannel != null)
      return fileChannel;
    else
      throw new ClosedChannelException();
  }


  private ByteBuffer entryBuffer(final Entry<K,V> entry) throws IOException
  {
    final int offset = entry.offset;
    if (offset < 0)
    {
      return null;
    }
    else
    {
      final ByteBuffer buffer = fileBuffer().duplicate();
      buffer.position(offset).limit(offset + entry.length + ENTRY_HEADER_LENGTH);
      return buffer;
    }
  }


  @SuppressWarnings("finally")
  private MappedByteBuffer fileBuffer() throws IOException
  {
    final FileChannel fileChannel = ensureOpen();
    final long fileLength = fileChannel.size();
    MappedByteBuffer fileBuffer = this.fileBuffer;

    if ((fileBuffer == null) || (fileBuffer.capacity() != fileLength))
    {
      fileBuffer = null;
      this.fileBuffer = null;

      if (fileLength <= Integer.MAX_VALUE)
      {
        this.fileBuffer = fileBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, fileChannel.size());
      }
      else
      {
        try
        {
          close();
        }
        finally
        {
          throw new IOException("Error: size of file exceeds Integer.MAX_VALUE bytes");
        }
      }
    }
    else
    {
      fileBuffer.limit(fileBuffer.capacity()).position(0);
    }

    return fileBuffer;
  }


  private MappedByteBuffer fileBuffer(final int minCapacity) throws IOException
  {
    MappedByteBuffer fileBuffer = this.fileBuffer;

    if ((fileBuffer == null) || (fileBuffer.capacity() < minCapacity))
    {
      fileBuffer = null;
      this.fileBuffer = null;
      final FileChannel fileChannel = ensureOpen();
      if (fileChannel.size() < minCapacity)
      {
        final int newCapacity = Math.max(minCapacity, (int) Math.min(
           Integer.MAX_VALUE,
            (float) minCapacity + ((float) minCapacity * DenseFileCache.this.properties.fileGrowFactor)
        ));
        ensureFileOpen().setLength(newCapacity);
      }
      fileBuffer = fileBuffer();
    }

    return fileBuffer;
  }


  private GrowingFileBufferOutputStream fileOutputStream() throws IOException
  {
    @SuppressWarnings("unchecked")
    GrowingFileBufferOutputStream out = this.fileOut;

    if (out != null)
      return out;
    else
      throw new ClosedChannelException();
  }


  private V getValue(Entry<K,V> entry) throws IOException
  {
    boolean read = false;
    V value = entry.getValue();

    if (value == null)
    {
      beginRead();
      try
      {
        value = entry.getValue();
        if (value == null)
        {
          value = readValue(entry);
          read = true;
        }
      }
      finally
      {
        endRead();
      }
    }


    //
    // Note ReentrantReadWriteLock does not allow upgrading a readlock to a writelock.
    //
    // However, by moving validation out of the locked block we don't risk the whole cache
    // of being blocked by long running validations.
    //


    if (value != null) // a bogus class may have returned null via readResolve()
    {
      final CacheEntryValidator entryValidator = this.properties.cacheEntryValidator;
      if (entryValidator == null)
      {
        if (read)
          this.cacheStatistics.hitsInPersistentStore.incrementAndGet();
        else
          this.cacheStatistics.hitsInMemoryStore.incrementAndGet();
      }
      else
      {
        Object oldAttachment = entry.get();
        Object newAttachment = entryValidator.validateCacheEntry(
          this, entry.key, value, oldAttachment
        );

        if (newAttachment == null)
        {
          oldAttachment = null;
          entry         = null;
          value         = null;
          remove(entry.key);
          this.cacheStatistics.missesBecauseInvalid.incrementAndGet();
        }
        else if (oldAttachment == newAttachment)
        {
          oldAttachment = null;
          newAttachment = null;
          entry         = null;
          if (read)
            this.cacheStatistics.hitsInPersistentStore.incrementAndGet();
          else
            this.cacheStatistics.hitsInMemoryStore.incrementAndGet();
        }
        else
        {
          entry.compareAndSet(oldAttachment, newAttachment);
          oldAttachment = null;
          newAttachment = null;
          entry         = null;
          if (read)
            this.cacheStatistics.hitsInPersistentStore.incrementAndGet();
          else
            this.cacheStatistics.hitsInMemoryStore.incrementAndGet();
        }
      }
    }

    return value;
  }




  private XOutputStream outputStream() throws IOException
  {
    XOutputStream out = this.out;

    if (out != null)
      return out;
    else
      throw new ClosedChannelException();
  }



  private ConcurrentHashXMap<K,Entry<K,V>> map() throws IOException
  {
    final ConcurrentHashXMap<K,Entry<K,V>> map = this.map;
    if (map != null)
      return map;
    else
      throw new ClosedChannelException();
  }



  private void purgeFile(final boolean force) throws IOException
  {
    if (!force)
    {
      final int deletedLength = this.deletedLength;
      if (deletedLength == 0)
      {
        return;
      }
      else
      {
        final int maxFileOverhead = this.properties.maxFileOverhead;
        if ((maxFileOverhead == -1) || (ensureFileOpen().length() / this.fileLength <= maxFileOverhead))
          return;
      }
    }

    boolean ok = false;
    beginWrite();

    try
    {
      if (this.deletedLength == 0)
        return;

      final Entry[] entries = new Entry[map.size()];

      // copy entries to array, oldest first
      {
        int i = 0;
        for (Entry e = this.head.prev; e != this.head; e = e.prev)
          entries[i++] = e;
      }

      // Sort ascending by offset.
      // The array should already be mostly sorted, thus this goes quick
      TunedQuickSort.getInstance().apply(entries);

      final MappedByteBuffer writeBuffer = fileBuffer();
      final ByteBuffer       readBuffer  = writeBuffer.duplicate();

      writeBuffer.position(FILE_HEADER_LENGTH);

      for (int i = 0; i < entries.length; i++)
      {
        Entry<?,?> entry = entries[i];
        entries[i] = null;

        final int srcPos = entry.offset;
        final int dstPos = writeBuffer.position();

        if (srcPos != dstPos)
        {
          entry.offset = dstPos;
          int srcLimit = srcPos + ENTRY_HEADER_LENGTH + entry.length;

          while (++i < entries.length)
          {
            entry = entries[i];
            if (entry.offset == srcLimit)
            {
              entry.offset = dstPos + (srcLimit - srcPos);
              srcLimit += ENTRY_HEADER_LENGTH + entry.length;
              entries[i] = null;
            }
            else
            {
              i--;
              break;
            }
          }

          readBuffer.limit(srcLimit);
          readBuffer.position(srcPos);
          writeBuffer.put(readBuffer);
        }
      }

      this.deletedLength  = 0;

      final int spaceUsed = writeBuffer.position();
      this.fileLength     = spaceUsed;

      final int maxFileSize = Math.max(
        (int) (spaceUsed + (spaceUsed * this.properties.fileGrowFactor)),
        Math.max(this.properties.minimumFileSize, spaceUsed * this.properties.maxFileOverhead)
      );

      if (writeBuffer.capacity() > maxFileSize)
      {
        this.fileBuffer = null;
        ensureFileOpen().setLength(maxFileSize);
      }
      else
      {
        writeBuffer.limit(writeBuffer.capacity());
        writeBuffer.position(0);
      }

      ok = true;
    }
    finally
    {
      if (!ok)
      {
        this.fileBuffer = null;
        try
        {
          clear();
        }
        catch (final Throwable ex)
        {
          // ignore, throw previous exception
        }
      }
      endWrite();
    }
  }


  @SuppressWarnings("unchecked")
  private V readValue(final Entry<K,V> entry) throws IOException
  {
    synchronized (entry)
    {
      SoftReference<V> valueRef = entry.valueRef;
      if (valueRef != null)
      {
        V value = valueRef.get();
        if (value == null)
        {
          valueRef = null;
          entry.valueRef = null;
        }
        else
        {
          return value;
        }
      }

      ByteBuffer buffer = entryBuffer(entry);
      final int  length = buffer.getInt();
      final V value;
      final Object attachment;

      ObjectInputStream in = createObjectInputStream(buffer);
      buffer = null;

      try
      {
        value = (V) in.readObject();
        attachment = in.readObject();
      }
      catch (final ClassNotFoundException ex)
      {
        throw new IOException(ex);
      }
      finally
      {
        in.close();
      }

      entry.valueRef = new SoftReference<>(value);
      entry.set(attachment);

      return value;
    }
  }





  private void writeValue(final Entry<K,V> entry, final V value)
  throws IOException
  {
    assert (this.lock.isWriteLockedByCurrentThread());

    final int offset = this.fileLength;
    final GrowingFileBufferOutputStream fileOut = fileOutputStream();

    // Don't use file channel for writing, because thread interruption causes the file to get closed

    fileOut.position(offset + 4);

    XOutputStream out = outputStream();

    try
    {
      final ObjectOutputStream objOut = new ObjectOutputStream(out);

      objOut.writeObject(value);

      Object attachment = entry.get();
      if (!(attachment instanceof Serializable))
        attachment = null;
      objOut.writeObject(attachment);
      attachment = null;

      objOut.close();
      out = null;
    }
    finally
    {
      if ((out != null) && (out != fileOut) && isOpen())
        out.close();
    }

    MappedByteBuffer fileBuffer = this.fileBuffer;
    if (fileBuffer != null)
    {
      final int position  = fileBuffer.position();
      final int length    = position - (offset + ENTRY_HEADER_LENGTH);
      fileBuffer.putInt(offset, length);

      this.fileLength = position;
      entry.offset    = offset;
      entry.length    = length;

      fileBuffer.force();
    }
    else
    {
      throw new AsynchronousCloseException();
    }
  }




  @Override
  protected void closeImpl() throws IOException
  {
    beginWrite();
    try
    {
      RandomAccessFile file = this.file;

      if (file != null)
      {
        this.classloader  = null;
        this.file         = null;
        this.fileChannel  = null;
        this.fileOut      = null;
        this.map          = null;
        this.out          = null;
        this.head.next    = this.head;
        this.head.prev    = this.head;

        // Try deleting before close.
        // Not sure whether that works always. If not sucessfull then other processes can open the file
        // which we delete a short time later below.
        if (this.deleteOnClose)
          this.path.delete();

        file.close();
        file = null;

        if (this.deleteOnClose)
          this.path.delete();
      }
    }
    finally
    {
      endWrite();
    }
  }




  @Override
  protected void purge()
  {
    super.purge();

    try
    {
      purgeFile(false);
    }
    catch (final IOException ex)
    {
      CacheSystem.log.severe("I/O error", ex);
    }
  }



  private boolean putImpl(final K key, final V value, final boolean onlyIfAbsent) throws IOException
  {
    CheckArg.instanceOf(key,   this.properties.keyClass,   "key");
    CheckArg.instanceOf(value, this.properties.valueClass, "value");

    int actualSize;

    beginWrite();
    try
    {
      ConcurrentHashXMap<K,Entry<K,V>> map = map();
      Entry<K,V> entry = map.get(key);
      actualSize = map.size();

      if (entry == null)
      {
        final int maxEntries = this.properties.maxEntries;
        if ((maxEntries >= 0) && (actualSize > maxEntries))
        {
          Entry<K,V> oldest = this.head.prev;
          map.remove(oldest.key);
          Entry<K,V> prev = oldest.prev;
          this.head.prev = prev;
          prev.next = this.head;

          // Do not do this, clearMemoryCache() would fail
          // oldest.next = null;
          // oldest.prev = null;

          oldest.valueRef = null; // may help gc
          final int offset = oldest.offset;
          oldest.offset = -1;
          if (this.properties.persistent)
            fileBuffer().putInt(offset, -oldest.length);
          this.deletedLength += oldest.length + ENTRY_HEADER_LENGTH;
          actualSize--;
          assert (actualSize >= 0);
        }

        entry = new Entry<>(this, key);
        writeValue(entry, value);
        entry.valueRef = new SoftReference<>(value);
        map.put(key, entry);
        actualSize++;

        entry.next = this.head.next;
        entry.prev = this.head;
        this.head.next = entry;
      }
      else if (onlyIfAbsent)
      {
        return false;
      }
      else
      {
        synchronized (entry)
        {
          writeValue(entry, value);
          entry.set(null);
          entry.valueRef = new SoftReference<>(value);
        }
      }
    }
    finally
    {
      endWrite();
    }

    final int largestSize = this.cacheStatistics.largestSize.get();
    if (actualSize > largestSize)
      this.cacheStatistics.largestSize.compareAndSet(largestSize, actualSize);

    return true;
  }






  @Override
  public void clear() throws IOException
  {
    beginWrite();
    try
    {
      this.head.next = this.head;
      this.head.prev = this.head;
      map().clear();
      this.deletedLength = 0;
      this.fileLength    = FILE_HEADER_LENGTH;

      final int maxFileOverhead = this.properties.maxFileOverhead;
      if (maxFileOverhead != -1)
      {
        final RandomAccessFile file = ensureFileOpen();
        final long fileSize = file.length();
        final long overhead = fileSize / FILE_HEADER_LENGTH;

        if (overhead > maxFileOverhead)
        {
          final int minimumFileSize = Math.max(FILE_HEADER_LENGTH, this.properties.minimumFileSize);
          if (minimumFileSize < fileSize)
          {
            this.fileBuffer = null;
            file.setLength(minimumFileSize);
          }
        }
      }
    }
    finally
    {
      endWrite();
    }
  }



  /**
   * Cleares all references to objects currently cached in memory.
   *
   * @since JaXLib 1.0
   */
  public void clearMemoryCache() throws IOException
  {
    for (Entry<K,V> e = this.head.next; e != this.head; e = e.next)
      e.valueRef = null;
  }



  @Override
  public boolean containsKey(Object key) throws IOException
  {
    return map().containsKey(key);
  }



  @Override
  public V get(Object key) throws IOException
  {
    Entry<K,V> entry = map().get(key);
    if (entry == null)
    {
      this.cacheStatistics.missesBecauseAbsent.incrementAndGet();
      return null;
    }
    else
    {
      return getValue(entry);
    }
  }



  @Override
  public CacheStatistics getCacheStatistics()
  {
    return this.cacheStatistics;
  }


  @Override
  public final int getCapacity() throws IOException
  {
    return map().capacity();
  }



  @Override
  public Iterator<K> keys() throws IOException
  {
    return map().keySet().iterator();
  }



  @Override
  public boolean put(K key, V value) throws IOException
  {
    putImpl(key, value, false);
    return true;
  }


  @Override
  public boolean putIfAbsent(K key, V value) throws IOException
  {
    return !map().containsKey(key) && putImpl(key, value, true);
  }


  @Override
  public boolean remove(Object key) throws IOException
  {
    beginWrite();
    try
    {
      final Entry<K,V> entry = map().remove(key);
      if (entry == null)
      {
        return false;
      }
      else
      {
        final int offset = entry.offset;
        if (offset > 0)
        {
          entry.offset = -1;
          if (this.properties.persistent)
            fileBuffer().putInt(offset, -entry.length);
          this.deletedLength += entry.length + ENTRY_HEADER_LENGTH;

          Entry<K,V> prev = entry.prev;
          Entry<K,V> next = entry.next;
          prev.next = next;
          next.prev = prev;
        }
        return true;
      }
    }
    finally
    {
      endWrite();
    }
  }


  @Override
  public final int size() throws IOException
  {
    return map().size();
  }









  @SuppressWarnings("serial")
  private static final class Entry<K,V> extends AtomicReference<Object> implements Comparable<Entry>
  {

    final K key;
    final UnmodifiableWeakReference<DenseFileCache<K,V>> storeRef;
    volatile SoftReference<V> valueRef;
    volatile int length;
    volatile int offset;

    volatile Entry<K,V> next;
    volatile Entry<K,V> prev;

    /**
     * To be used for DenseFileCache.head only.
     */
    Entry()
    {
      super();

      this.key      = null;
      this.next     = this;
      this.prev     = this;
      this.storeRef = null;
    }


    @SuppressWarnings("unchecked")
    Entry(final DenseFileCache<K, V> owner, final K key)
    {
      super();

      this.key      = key;
      this.offset   = -1;
      this.storeRef = (UnmodifiableWeakReference) owner.self;
    }


    private DenseFileCache<K,V> getStore() throws IOException
    {
      final DenseFileCache<K,V> store = this.storeRef.get();
      if (store != null)
      {
        return store;
      }
      else
      {
        this.valueRef = null;
        throw new ClosedChannelException();
      }
    }


    final V getValue()
    {
      final SoftReference<V> r = this.valueRef;
      if (r == null)
      {
        return null;
      }
      else
      {
        V value = r.get();
        if (value == null)
          this.valueRef = null;
        return value;
      }
    }


    @Override
    public final int compareTo(Entry b)
    {
      return this.offset - b.offset;
    }

  }






  private final class GrowingFileBufferOutputStream extends XOutputStream
  {


    GrowingFileBufferOutputStream()
    {
      super();
    }


    private MappedByteBuffer buffer(int minRemaining) throws IOException
    {
      MappedByteBuffer fileBuffer = DenseFileCache.this.fileBuffer;

      if ((fileBuffer == null) || (fileBuffer.remaining() < minRemaining))
      {
        final int position    = (fileBuffer != null) ? fileBuffer.position() : 0;
        final int minCapacity = position + minRemaining;
        if (minCapacity < 0)
          throw new IOException("File size exceeds Integer.MAX_VALUE bytes");

        if (fileBuffer != null)
        {
          fileBuffer.force();
          fileBuffer = null;
        }
        fileBuffer = DenseFileCache.this.fileBuffer(minCapacity);
        fileBuffer.position(position);
      }

      return fileBuffer;
    }


    void position(int position) throws IOException
    {
      buffer(4).position(position);
    }


    @Override
    public void closeInstance()
    {
      // nop
    }


    @Override
    public boolean isOpen()
    {
      return true;
    }


    @Override
    public void write(final int b) throws IOException
    {
      buffer(1).put((byte) b);
    }


    @Override
    public void write(final byte[] src, final int offs, final int len) throws IOException
    {
      buffer(len).put(src, offs, len);
    }


    @Override
    public void writeInt(int v) throws IOException
    {
      buffer(4).putInt(v);
    }


    @Override
    public void writeLong(long v) throws IOException
    {
      buffer(8).putLong(v);
    }

  }



  private static final class InflatedInputStreamImpl extends InflatedInputStream
  {

    private static final ThreadLocal<SoftReference<InflatedInputStreamImpl>> cachedInflater = new ThreadLocal<>();


    static InflatedInputStreamImpl getSharedInstance(InputStream in) throws IOException
    {
      SoftReference<InflatedInputStreamImpl> r = cachedInflater.get();
      InflatedInputStreamImpl a = (r == null) ? null : r.get();
      if (a == null)
      {
        return new InflatedInputStreamImpl(in);
      }
      else
      {
        a.reinit(in);
        return a;
      }
    }


    private final SoftReference<InflatedInputStreamImpl> self = new SoftReference<>(this);


    InflatedInputStreamImpl(InputStream in)
    {
      super(in, 512, true, true);
    }


    @Override
    protected void reinit(InputStream in) throws IOException
    {
      super.reinit(in);
    }


    @Override
    public void close() throws IOException
    {
      super.reinit(NullInputStream.SHARED_INSTANCE);
      cachedInflater.set(this.self);
    }

  }



  private static final class NonClosingBufferedOutputStream extends BufferedXOutputStream
  {

    private final NonClosingDeflatedOutputStream out;


    NonClosingBufferedOutputStream(NonClosingDeflatedOutputStream out)
    {
      super(out, 512);
      this.out = out;
    }


    @Override
    public void close() throws IOException
    {
      flush();
      this.out.reinit();
    }


    @Override
    public void closeInstance() throws IOException
    {
      throw new SecurityException();
    }
  }



  private static final class NonClosingDeflatedOutputStream extends DeflatedOutputStream
  {

    private static DeflaterProperties createDeflaterProperties(int compressionLevel)
    {
      DeflaterProperties p = new DeflaterProperties();
      p.setCompressionLevel(compressionLevel);
      p.setNoWrap(true);
      return p;
    }


    NonClosingDeflatedOutputStream(OutputStream out, int compressionLevel)
    {
      super(out, createDeflaterProperties(compressionLevel));
    }


    @Override
    public void close() throws IOException
    {
      reinit();
    }

    @Override
    public void closeInstance() throws IOException
    {
      throw new SecurityException();
    }
  }



}
