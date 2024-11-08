package com.googlecode.jue.file;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.zip.Checksum;
import com.googlecode.jue.util.ConcurrentLRUCache;

/**
 * 以块的方式读写文件，可以设置块的大小，以及是否缓存
 * 
 * @author noah
 */
public class BlockFileChannel {

    /**
	 * 最大缓存数
	 */
    public static final int MAX_CAPACITY = 100000;

    /**
	 * 文件块的校验码的长度
	 */
    public static final int CHECKSUM_SIZE = 8;

    /**
	 * 校验码存储文件的后缀
	 */
    public static final String BLOCK_SUFFIX = ".bck";

    /**
	 * 默认的文件块大小
	 */
    public static final int DEFAULT_BLOCK_SIZE = 4096;

    /**
	 * 文件Channel对象
	 */
    private FileChannel fileChannel;

    /**
	 * 存储block块的校验码的文件
	 */
    private FileChannel blockChecksumChannel;

    /**
	 * 块大小
	 */
    private final int blockSize;

    /**
	 * 是否缓存文件块
	 */
    private final boolean blockCache;

    /**
	 * 缓存
	 */
    private ConcurrentLRUCache<Long, ByteBuffer> cache;

    /**
	 * 校验码生成器
	 */
    private ChecksumGenerator checksumGenerator;

    /**
	 * 构造一个BlockFileChannel
	 * 
	 * @param file
	 *            文件对象
	 * @param checksumGenerator
	 *            校验码生成器
	 * @throws FileNotFoundException
	 */
    public BlockFileChannel(File file, ChecksumGenerator checksumGenerator) {
        this(file, DEFAULT_BLOCK_SIZE, false, checksumGenerator);
    }

    /**
	 * 构造一个BlockFileChannel
	 * 
	 * @param file
	 *            文件对象
	 * @param blockSize
	 *            快大小
	 * @param checksumGenerator
	 *            校验码生成器
	 * @throws FileNotFoundException
	 */
    public BlockFileChannel(File file, int blockSize, ChecksumGenerator checksumGenerator) {
        this(file, blockSize, true, checksumGenerator);
    }

    /**
	 * 构造一个BlockFileChannel
	 * 
	 * @param filePath
	 *            文件路径
	 * @param blockSize
	 *            快大小
	 * @param checksumGenerator
	 *            校验码生成器
	 * @throws FileNotFoundException
	 */
    public BlockFileChannel(String filePath, int blockSize, ChecksumGenerator checksumGenerator) {
        this(filePath, blockSize, true, checksumGenerator);
    }

    /**
	 * 构造一个BlockFileChannel
	 * 
	 * @param filePath
	 *            文件路径
	 * @param blockSize
	 *            块大小
	 * @param blockCache
	 *            是否缓存块
	 * @param checksumGenerator
	 *            校验码生成器
	 * @throws FileNotFoundException
	 *             文件不存在
	 */
    public BlockFileChannel(String filePath, int blockSize, boolean blockCache, ChecksumGenerator checksumGenerator) {
        this(new File(filePath), blockSize, blockCache, checksumGenerator);
    }

    /**
	 * 构造一个BlockFileChannel
	 * 
	 * @param file
	 *            文件对象
	 * @param blockSize
	 *            块大小
	 * @param blockCache
	 *            是否缓存块
	 * @param checksumGenerator
	 *            校验码生成器
	 * @throws FileNotFoundException
	 *             文件不存在
	 */
    public BlockFileChannel(File file, int blockSize, boolean blockCache, ChecksumGenerator checksumGenerator) {
        try {
            RandomAccessFile raf = new RandomAccessFile(file, "rw");
            this.fileChannel = raf.getChannel();
            File blockchkFile = new File(getChecksumFilename(file.getName()));
            RandomAccessFile blockchkFileRaf = new RandomAccessFile(blockchkFile, "rw");
            this.blockChecksumChannel = blockchkFileRaf.getChannel();
        } catch (FileNotFoundException e) {
        }
        this.blockSize = blockSize;
        this.blockCache = blockCache;
        this.checksumGenerator = checksumGenerator;
        if (blockCache) {
            cache = new ConcurrentLRUCache<Long, ByteBuffer>(MAX_CAPACITY);
        }
    }

    public boolean isBlockCache() {
        return blockCache;
    }

    public int getBlockSize() {
        return blockSize;
    }

    /**
	 * 关闭文件
	 * 
	 * @throws IOException
	 */
    public void close() throws IOException {
        fileChannel.close();
    }

    /**
	 * 返回文件大小
	 * 
	 * @return
	 * @throws IOException
	 */
    public long size() throws IOException {
        return fileChannel.size();
    }

    /**
	 * 读取数据到字节数组中
	 * 
	 * @param buffer
	 *            需要存储的数据缓存
	 * @param position
	 *            读取位置
	 * @param checksum
	 *            是否要校验数据
	 * @return 返回读取的数据长度
	 * @throws IOException
	 *             文件读取异常
	 * @throws ChecksumException
	 *             校验错误抛出的异常
	 */
    public int read(ByteBuffer buffer, long position, boolean checksum) throws IOException, ChecksumException {
        if (size() == 0) {
            return -1;
        }
        int maxSize = buffer.remaining();
        int count = 0;
        long[] blockIndexes = getReadBlockIndexes(position, maxSize);
        for (int i = 0; i < blockIndexes.length; ++i) {
            ByteBuffer blockDataBuffer = getBlockData(blockIndexes[i], checksum);
            int oldPos = blockDataBuffer.position();
            int oldLimit = blockDataBuffer.limit();
            if (i == 0) {
                blockDataBuffer.position((int) (position % blockSize));
                if (maxSize < blockDataBuffer.remaining()) {
                    blockDataBuffer.limit(blockDataBuffer.position() + maxSize);
                }
            } else if (i == blockIndexes.length - 1) {
                int s = maxSize - count;
                if (s < blockDataBuffer.remaining()) {
                    blockDataBuffer.limit(s);
                }
            }
            count += blockDataBuffer.remaining();
            buffer.put(blockDataBuffer);
            blockDataBuffer.position(oldPos);
            blockDataBuffer.limit(oldLimit);
        }
        return count;
    }

    /**
	 * 读取相应的文件块
	 * 
	 * @param blockIndex
	 *            文件块位置
	 * @param checksum
	 *            是否需要校验数据
	 * @return
	 * @throws IOException
	 * @throws ChecksumException
	 */
    private ByteBuffer getBlockData(long blockIndex, boolean checksum) throws IOException, ChecksumException {
        ByteBuffer dataBuffer = null;
        if (blockCache) {
            dataBuffer = cache.get(blockIndex);
        }
        if (dataBuffer == null) {
            dataBuffer = ByteBuffer.allocate(blockSize);
            int ct = 0;
            do {
                int n = fileChannel.read(dataBuffer, blockIndex * blockSize + ct);
                if (n == -1) {
                    break;
                }
                ct += n;
            } while (ct < blockSize);
            if (ct == 0) {
                return null;
            }
            dataBuffer.flip();
            if (checksum) {
                ByteBuffer chksumBuffer = ByteBuffer.allocate(CHECKSUM_SIZE);
                int c = 0;
                do {
                    int n = blockChecksumChannel.read(chksumBuffer, blockIndex * CHECKSUM_SIZE + c);
                    if (n == -1) {
                        break;
                    }
                    c += n;
                } while (c < CHECKSUM_SIZE);
                if (c == 0) {
                    throw new ChecksumException("can not read checksum");
                }
                chksumBuffer.flip();
                long chksum = chksumBuffer.getLong();
                byte[] b = dataBuffer.array();
                Checksum cksum = checksumGenerator.createChecksum();
                cksum.update(b, 0, b.length);
                long chksum2 = cksum.getValue();
                if (chksum != chksum2) {
                    throw new ChecksumException();
                }
            }
            if (blockCache) {
                cache.put(blockIndex, dataBuffer);
            }
        }
        dataBuffer.rewind();
        return dataBuffer;
    }

    /**
	 * 获取需要读取的数据对应的文件块的位置
	 * 
	 * @param pos
	 *            文件位置
	 * @param size
	 *            要获取的长度
	 * @return
	 * @throws IOException
	 */
    private long[] getReadBlockIndexes(long pos, int size) throws IOException {
        long fileSize = size();
        if (pos >= fileSize) {
            throw new IOException("out of file");
        }
        long startBlockIndex = pos / blockSize;
        long endBlockIndex = (pos + size) / blockSize;
        long lastBlockIndex = (long) Math.ceil((double) fileSize / blockSize) - 1;
        if (endBlockIndex > lastBlockIndex) {
            endBlockIndex = lastBlockIndex;
        }
        int count = (int) (endBlockIndex - startBlockIndex + 1);
        long[] indexes = new long[count];
        for (int i = 0; i < count; ++i, ++startBlockIndex) {
            indexes[i] = startBlockIndex;
        }
        return indexes;
    }

    /**
	 * 在指定位置写入数据
	 * 
	 * @param data
	 *            数据缓冲区
	 * @param position
	 *            位置
	 * @return
	 * @throws IOException
	 */
    public int write(ByteBuffer dataBuffer, long position) throws IOException {
        long fileSize = size();
        if (position > fileSize) {
            throw new IOException("out of file");
        }
        int startPos = dataBuffer.position();
        int oldLimit = dataBuffer.limit();
        int dataSize = dataBuffer.remaining();
        long[] blockIndexes = getWriteBlockIndexes(position, dataSize);
        long[] checksums = generateChecksum(dataBuffer, position, blockIndexes);
        if (blockCache) {
            for (int i = 0; i < blockIndexes.length; ++i) {
                cache.remove(blockIndexes[i]);
            }
        }
        dataBuffer.position(startPos);
        dataBuffer.limit(oldLimit);
        int written = fileChannel.write(dataBuffer, position);
        ByteBuffer chksumBuffer = ByteBuffer.allocate(CHECKSUM_SIZE * checksums.length);
        for (int i = 0; i < checksums.length; ++i) {
            chksumBuffer.putLong(checksums[i]);
        }
        chksumBuffer.flip();
        blockChecksumChannel.write(chksumBuffer, blockIndexes[0] * CHECKSUM_SIZE);
        return written;
    }

    /**
	 * 生成校验码
	 * @param dataBuffer
	 * @param position
	 * @param blockIndexes 
	 * @return
	 * @throws IOException  
	 */
    private long[] generateChecksum(ByteBuffer dataBuffer, long position, long[] blockIndexes) throws IOException {
        int oldLimit = dataBuffer.limit();
        long[] checksums = new long[blockIndexes.length];
        for (int i = 0; i < blockIndexes.length; ++i) {
            long blockIndex = blockIndexes[i];
            ByteBuffer buffer = null;
            try {
                buffer = getBlockDataOrEmptyBuffer(blockIndex, false);
            } catch (ChecksumException e) {
            }
            if (i == 0) {
                int pos = (int) (position % blockSize);
                buffer.position(pos);
                if (dataBuffer.remaining() > buffer.remaining()) {
                    dataBuffer.limit(buffer.remaining());
                }
            } else if (i == blockIndexes.length - 1) {
                dataBuffer.limit(oldLimit);
            } else {
                dataBuffer.limit(dataBuffer.position() + blockSize);
            }
            buffer.put(dataBuffer);
            buffer.flip();
            Checksum chksum = checksumGenerator.createChecksum();
            byte[] b = buffer.array();
            chksum.update(b, 0, b.length);
            checksums[i] = chksum.getValue();
        }
        return checksums;
    }

    /**
	 * 获取对应的块数据，如果该块超出文件，那么返回空缓冲区
	 * @param blockIndex
	 * @param checksum
	 * @return
	 * @throws IOException
	 * @throws ChecksumException 
	 */
    private ByteBuffer getBlockDataOrEmptyBuffer(long blockIndex, boolean checksum) throws IOException, ChecksumException {
        if (blockIndex * blockSize < size()) {
            return getBlockData(blockIndex, checksum);
        }
        return ByteBuffer.allocate(blockSize);
    }

    /**
	 * 获取即将写入的数据对应的文件块的位置
	 * 
	 * @param pos
	 *            文件位置
	 * @param size
	 *            要获取的长度
	 * @return
	 * @throws IOException
	 */
    private long[] getWriteBlockIndexes(long pos, int size) throws IOException {
        long startBlockIndex = pos / blockSize;
        long endBlockIndex = (pos + size) / blockSize;
        int count = (int) (endBlockIndex - startBlockIndex + 1);
        long[] indexes = new long[count];
        for (int i = 0; i < count; ++i, ++startBlockIndex) {
            indexes[i] = startBlockIndex;
        }
        return indexes;
    }

    /**
	 * 获取校验和文件名
	 * @param fileName
	 * @return
	 */
    public static String getChecksumFilename(String fileName) {
        return fileName + BLOCK_SUFFIX;
    }
}
