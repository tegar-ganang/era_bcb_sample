package org.karticks.mapreduce;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Map;

/**
 * Implements the Mapper interface to do word counts. Does absolutely no processing
 * on the stream i.e. assumes that there are no punctuation marks or line endings.
 * 
 * @author Kartick Suriamoorthy
 */
public class WordCountMapper implements Mapper {

    /**
	 * Reads the InputStream and parses words out of the stream. Assumes that the
	 * words are just separated by spaces (" ") and that there are no punctuation
	 * marks or line endings. Converts all the words into lower case and stores
	 * them (and their counts) in the resulting Map.
	 */
    public Map<String, Integer> doMap(InputStream is) {
        Map<String, Integer> map = new Hashtable<String, Integer>();
        String s = readInputStream(is);
        String[] words = s.split("\\s+");
        for (String word : words) {
            if (word.length() > 0) {
                word = word.toLowerCase();
                Integer value = map.get(word);
                if (value == null) {
                    map.put(word, new Integer(1));
                } else {
                    value++;
                    map.put(word, value);
                }
            }
        }
        return map;
    }

    private String readInputStream(InputStream is) {
        try {
            BufferedInputStream buis = new BufferedInputStream(is);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[512];
            int read = 0;
            while (true) {
                read = buis.read(buffer, 0, buffer.length);
                if (read == -1) {
                    break;
                } else {
                    baos.write(buffer, 0, read);
                }
            }
            baos.flush();
            String s = baos.toString();
            baos.close();
            return s;
        } catch (IOException ioe) {
            throw new RuntimeException("Error while reading from input stream. Error message : " + ioe.getMessage(), ioe);
        }
    }
}
