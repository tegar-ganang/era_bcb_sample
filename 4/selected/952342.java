package com.vionto.vithesaurus.tools;

import java.io.*;

/**
 * Some useful tools for dealing with Strings.
 */
public class StringTools {

    private static final int BUFFER_SIZE = 4096;

    private StringTools() {
    }

    public static String slashEscape(String str) {
        return str.replace("/", "___");
    }

    public static String slashUnescape(String str) {
        return str.replace("___", "/");
    }

    public static String normalize(String word) {
        return word.replaceAll("\\(.*?\\)", "").replaceAll("\\s+", " ").trim();
    }

    public static String normalizeForSort(String s) {
        return normalize(s.toLowerCase().replace('ä', 'a').replace('ü', 'u').replace('ö', 'o').replace("ß", "ss"));
    }

    /**
   * Replaces all occurrences of<br>
   * <code>&lt;, &gt;, &amp;</code> <br>
   * with <br>
   * <code>&amp;lt;, &amp;gt;, &amp;amp;</code><br>
   * 
   * @param string
   *          The input string
   * @return The modified String, with replacements.
   */
    public static String replaceHtmlMarkupChars(final String string) {
        return string.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
    }

    /**
   * Write the contents if {@code file} to {@code out}.
   * @param file input file
   * @param out stream to be written to
   * @throws IOException
   */
    public static void writeToStream(final File file, final OutputStream out) throws IOException {
        final FileInputStream fis = new FileInputStream(file);
        try {
            final BufferedInputStream bis = new BufferedInputStream(fis);
            try {
                final byte[] chars = new byte[BUFFER_SIZE];
                int readBytes = 0;
                while (readBytes >= 0) {
                    readBytes = bis.read(chars, 0, BUFFER_SIZE);
                    if (readBytes <= 0) {
                        break;
                    }
                    out.write(chars, 0, readBytes);
                }
            } finally {
                bis.close();
            }
        } finally {
            fis.close();
        }
    }
}
