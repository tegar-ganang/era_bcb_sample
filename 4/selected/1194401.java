package org.jforensics.ie;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jforensics.general.CacheRecord;
import org.jforensics.general.Util;

/**
 * Given an index.dat file, parse it and return a hashtable of metadata
 * allow a metadata lookup based on cache name and file name.  The parser
 * class handles all of the file access work of obtaining the byte buffers
 * from the file and obtaining information that is global to the file.
 * @author Administrator
 *
 */
public class IEIndexParser {

    public static final int FILE_SIZE_OFFSET = 0x1C;

    public static final int HASH_CODE_OFFSET = 0x20;

    public static final int CACHE_NAMES_OFFSET = 0x50;

    public static final int FILE_SIZE_SIZE = 4;

    public static final int HASH_CODE_SIZE = 4;

    public static final int CACHE_DIR_NAME_SIZE = 8;

    public static final int BLOCK_SIZE = 0x80;

    private Long fileSize;

    private FileChannel fileChannel;

    private String[] cacheNames = null;

    private File file;

    public IEIndexParser(File file) {
        this.file = file;
        try {
            FileInputStream fis = new FileInputStream(file);
            fileChannel = fis.getChannel();
            getBrowserVersion();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getBrowserVersion() {
        int version = -1;
        try {
            fileChannel.position(0);
            ByteBuffer buffer = ByteBuffer.allocate(100);
            fileChannel.read(buffer);
            String header = Util.readString(buffer, 0);
            Pattern pattern = Pattern.compile("\\d+(?=\\.)");
            Matcher matcher = pattern.matcher(header);
            if (matcher.find()) {
                String match = matcher.group(0);
                version = NumberFormat.getInstance().parse(match).intValue();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return version;
    }

    public String convertCacheIndexToName(int index) {
        if (this.cacheNames == null) {
            cacheNames = getCacheNames();
        }
        String cacheName = null;
        if (index >= 0 && index < cacheNames.length) {
            cacheName = cacheNames[index];
        }
        return cacheName;
    }

    private String[] getCacheNames() {
        List<String> cacheNames = new ArrayList<String>();
        try {
            ByteBuffer fileSizeBuffer = ByteBuffer.allocate(CACHE_DIR_NAME_SIZE);
            fileChannel.position(CACHE_NAMES_OFFSET);
            for (int x = 0; x < 10; x++) {
                fileChannel.read(fileSizeBuffer);
                String cacheName = new String(fileSizeBuffer.array());
                if (cacheName.charAt(0) != 0x00) {
                    cacheNames.add(cacheName.trim());
                    fileChannel.position(fileChannel.position() + 4);
                    fileSizeBuffer.clear();
                } else {
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cacheNames.toArray(new String[cacheNames.size()]);
    }

    public String getFilePath() {
        String path = "";
        try {
            path = file.getCanonicalPath();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return path;
    }

    public long getFileSize() {
        try {
            ByteBuffer fileSizeBuffer = ByteBuffer.allocate(FILE_SIZE_SIZE);
            fileChannel.position(FILE_SIZE_OFFSET);
            fileChannel.read(fileSizeBuffer);
            fileSizeBuffer.flip();
            fileSize = Util.arrayToLong(fileSizeBuffer.array());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fileSize;
    }

    public int getFirstHashOffset() {
        int hashOffset = -1;
        try {
            ByteBuffer hashOffsetBuffer = ByteBuffer.allocate(HASH_CODE_SIZE);
            fileChannel.position(HASH_CODE_OFFSET);
            fileChannel.read(hashOffsetBuffer);
            hashOffsetBuffer.flip();
            hashOffset = Util.arrayToInt(hashOffsetBuffer.array());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hashOffset;
    }

    /**
	 * TODO: Convert this to a generic cache record
	 * Check the flags to see if it's valid
	 * @param activityLoc
	 * @return
	 */
    public CacheRecord getActivity(ActivityLocator activityLoc) {
        CacheRecord cacheRecord = null;
        try {
            fileChannel.position(activityLoc.getOffset() + 0x04);
            ByteBuffer fourBuffer = ByteBuffer.allocate(4);
            fileChannel.read(fourBuffer);
            long recordBlockSize = Util.arrayToLong(fourBuffer.array());
            int recordSize = (int) (recordBlockSize * IEIndexParser.BLOCK_SIZE);
            if (recordSize > 0) {
                fileChannel.position(activityLoc.getOffset());
                ByteBuffer activityBlock = ByteBuffer.allocate(recordSize);
                fileChannel.read(activityBlock);
                IECacheRecord activity = new IECacheRecord(this, activityLoc.getFlag(), activityBlock, getBrowserVersion());
                cacheRecord = activity.getCacheRecord();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cacheRecord;
    }

    public IEIndexHashtable getHash(int offset) {
        IEIndexHashtable hash = null;
        try {
            ByteBuffer hashSize = ByteBuffer.allocate(4);
            fileChannel.position(offset + 0x04);
            fileChannel.read(hashSize);
            hashSize.flip();
            int size = Util.arrayToInt(hashSize.array());
            int hashByteSize = size * BLOCK_SIZE;
            ByteBuffer hashBlock = ByteBuffer.allocate(hashByteSize);
            fileChannel.position(offset);
            fileChannel.read(hashBlock);
            hash = new IEIndexHashtable(hashBlock);
            hash.setOffset(offset);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hash;
    }
}
