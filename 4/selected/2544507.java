package org.zzdict.utils;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * StarDict index file parser
 * 
 * @author zzh
 * 
 */
public class StarDictIndexFileParser {

    /**
	 * default wordEncoding is UTF8
	 */
    private String wordEncoding = "UTF8";

    private String indexFileName;

    private FileInputStream fis;

    private int readlimit = 8192;

    /**
	 * costructor of StarDictIndexFileParser
	 * 
	 * @param indexFileName
	 *            index file name
	 * @exception FileNotFoundException
	 *                if file named indexFileName is not found
	 */
    public StarDictIndexFileParser(String indexFileName) throws FileNotFoundException {
        this.indexFileName = indexFileName;
        fis = new FileInputStream(indexFileName);
    }

    /**
	 * costructor of StarDictIndexFileParser
	 * 
	 * @param indexFileName
	 *            index file name
	 * @param wordEncoding
	 *            encoding of word in index file
	 * @exception FileNotFoundException
	 *                if file named indexFileName is not found
	 */
    public StarDictIndexFileParser(String indexFileName, String wordEncoding) throws FileNotFoundException {
        this.indexFileName = indexFileName;
        this.wordEncoding = wordEncoding;
        fis = new FileInputStream(indexFileName);
    }

    /**
	 * Parse index file The ".idx" file's format. The .idx file is just a word
	 * list.
	 * 
	 * The word list is a sorted list of word entries.
	 * 
	 * Each entry in the word list contains three fields, one after the other:
	 * word_str; // a utf-8 string terminated by '\0'. word_data_offset; // word
	 * data's offset in .dict file word_data_size; // word data's total size in
	 * .dict file
	 * 
	 * @exception IOException
	 *                if I/O error occurs
	 * @throws FileFormatErrorException
	 *             if file format is wrong
	 */
    public synchronized Map<String, DictDataInfo> parseIndexFile() throws IOException, FileNotFoundException, FileFormatErrorException {
        Map<String, DictDataInfo> map = new ConcurrentHashMap<String, DictDataInfo>();
        BufferedInputStream bis = new BufferedInputStream(fis);
        String word;
        while ((word = readString(bis)) != null) {
            map.put(word, new DictDataInfo(readInt(bis), readInt(bis)));
        }
        return map;
    }

    /**
	 * read string from a buffered input stream
	 * 
	 * @param bis
	 *            the buffered input stream
	 * @return read string, null if input stream reach the end
	 * @throws IOException
	 * @throws FileFormatErrorException
	 *             if file format is wrong
	 */
    private String readString(BufferedInputStream bis) throws IOException, FileFormatErrorException {
        bis.mark(readlimit);
        int pos = getNullCharPosition(bis);
        bis.reset();
        if (pos == -1) return null;
        byte[] buf = new byte[pos];
        bis.read(buf);
        String result = new String(buf, wordEncoding);
        bis.read();
        return result;
    }

    /**
	 * get null char position
	 * 
	 * @param bis
	 *            buffered input stream
	 * @return null char position
	 * @throws IOException
	 * @throws FileFormatErrorException
	 *             if file format is wrong
	 */
    private int getNullCharPosition(BufferedInputStream bis) throws IOException, FileFormatErrorException {
        int length;
        byte[] buf = new byte[256];
        int pos = -1;
        boolean nullCharFound = false;
        while ((length = bis.read(buf)) != -1) {
            for (int i = 0; i < length; i++) {
                pos++;
                if (buf[i] == 0) {
                    nullCharFound = true;
                    break;
                }
            }
            if (nullCharFound) break;
        }
        return pos;
    }

    /**
	 * read long from a buffered input stream, bytes are big endian
	 * 
	 * @param bis
	 *            the buffered input stream
	 * @return read long, -1 if input stream reach the end
	 * @throws FileFormatErrorException
	 *             if file format is wrong
	 * @throws IOException
	 */
    @SuppressWarnings("unused")
    private long readLong(BufferedInputStream bis) throws IOException, FileFormatErrorException {
        return ((long) (readInt(bis)) << 32) | (readInt(bis) & 0xFFFFFFFFL);
    }

    /**
	 * read integer from a buffered input stream, bytes are big endian
	 * 
	 * @param bis
	 *            the buffered input stream
	 * @return read integer, -1 if input stream reach the end
	 * @exception IOException
	 * @exception FileFormatErrorException
	 *                if file format is wrong
	 */
    private int readInt(BufferedInputStream bis) throws IOException, FileFormatErrorException {
        byte[] buf = new byte[4];
        int length = bis.read(buf);
        if (length < 4) throw new FileFormatErrorException(this.indexFileName, fis.getChannel().position());
        int value = 0;
        for (int i = 0; i < 4; i++) {
            value <<= 8;
            value |= (int) buf[i] & 0xFF;
        }
        return value;
    }
}

class DictDataInfo {

    long wordDataOffset;

    int wordDataSize;

    public DictDataInfo(long wordDataOffset, int wordDataSize) {
        this.wordDataOffset = wordDataOffset;
        this.wordDataSize = wordDataSize;
    }
}
