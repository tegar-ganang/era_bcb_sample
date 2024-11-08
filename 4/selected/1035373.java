package com.yerihyo.yeritools.languagetech.tfidf;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Wordcount {

    public static TreeMap<String, Integer> addWordsFromDocToMap(String document, TreeMap<String, Integer> map) {
        Integer ONE = new Integer(1);
        String words[] = document.trim().split("[^a-zA-Z0-9]");
        for (int i = 0, n = words.length; i < n; i++) {
            if (words[i].length() > 0) {
                Integer frequency = (Integer) map.get(words[i]);
                if (frequency == null) {
                    frequency = ONE;
                } else {
                    int value = frequency.intValue();
                    frequency = new Integer(value + 1);
                }
                map.put(words[i].toLowerCase(), frequency);
            }
        }
        return map;
    }

    public static Map<String, Integer> addWordsToMap(String filename, Map<String, Integer> map) throws IOException {
        FileInputStream input = new FileInputStream(filename);
        FileChannel channel = input.getChannel();
        int fileLength = (int) channel.size();
        MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, fileLength);
        Charset charset = Charset.forName("ISO-8859-1");
        CharsetDecoder decoder = charset.newDecoder();
        CharBuffer charBuffer = decoder.decode(buffer);
        Pattern linePattern = Pattern.compile(".*$", Pattern.MULTILINE);
        Pattern wordBreakPattern = Pattern.compile("[\\p{Punct}\\s}]");
        Matcher lineMatcher = linePattern.matcher(charBuffer);
        Integer ONE = new Integer(1);
        while (lineMatcher.find()) {
            CharSequence line = lineMatcher.group();
            String words[] = wordBreakPattern.split(line);
            for (int i = 0, n = words.length; i < n; i++) {
                if (words[i].length() > 0) {
                    Integer frequency = (Integer) map.get(words[i]);
                    if (frequency == null) {
                        frequency = ONE;
                    } else {
                        int value = frequency.intValue();
                        frequency = new Integer(value + 1);
                    }
                    map.put(words[i], frequency);
                }
            }
        }
        return map;
    }

    /**
	 * @param args
	 * @throws IOException 
	 * @throws IOException 
	 */
    public static void main(String[] args) throws IOException {
        Map<String, Integer> map = new TreeMap<String, Integer>();
        int tokens = 0;
        int types = 0;
        int sentences = 0;
        for (int j = 1; j <= 1; j++) {
            String filename = "C:\\Documents and Settings\\schaudhu\\Desktop\\carolyn.txt";
            FileInputStream input = new FileInputStream(filename);
            FileChannel channel = input.getChannel();
            int fileLength = (int) channel.size();
            MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, fileLength);
            Charset charset = Charset.forName("ISO-8859-1");
            CharsetDecoder decoder = charset.newDecoder();
            CharBuffer charBuffer = decoder.decode(buffer);
            Pattern linePattern = Pattern.compile(".*$", Pattern.MULTILINE);
            Pattern wordBreakPattern = Pattern.compile("[\\p{Punct}\\s}]");
            Matcher lineMatcher = linePattern.matcher(charBuffer);
            Integer ONE = new Integer(1);
            while (lineMatcher.find()) {
                CharSequence line = lineMatcher.group();
                String words[] = wordBreakPattern.split(line);
                sentences++;
                for (int i = 0, n = words.length; i < n; i++) {
                    if (words[i].length() > 0) {
                        tokens++;
                        Integer frequency = (Integer) map.get(words[i]);
                        if (frequency == null) {
                            frequency = ONE;
                            types++;
                        } else {
                            int value = frequency.intValue();
                            frequency = new Integer(value + 1);
                        }
                        map.put(words[i], frequency);
                    }
                }
            }
        }
        System.out.println("\n\n\n");
        System.out.println("\n\n\n");
        System.out.println("Sentences = " + sentences + " Tokens = " + tokens + " Types = " + types);
        System.out.println(map);
    }
}
