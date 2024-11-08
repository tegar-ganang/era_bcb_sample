package com.spiralgeneration.shortcutjava.util;

import java.io.*;

/**
 * <p>Utility class to handle the most common processing needs of Shortcuts and Shortcut Java in general.</p>
 */
public class Utils {

    /**
	 * Stores the line break for the system that Shortcut Java is being run on, so it only has
	 * to be called/parsed once.
	 */
    public static final String LINE_BREAK_STRING = System.getProperty("line.separator");

    /**
	 * A line break as a byte array, for easy writing back into a stream.
	 */
    public static final byte[] LINE_BREAK = LINE_BREAK_STRING.getBytes();

    /**
	 * <p>Determines if a character is white space as defined by Shortcut Java. This is used to find
	 * key signifying words to mark class/interface/enum declarations as well as for applying 
	 * shortcuts to the proper code. This is defined as:</p>
	 *
	 * <pre>	<font color="#993333">' ' </font>|| <font color="#993333">'\t' </font>|| <font color="#993333">'\r' </font>|| <font color="#993333">'\n' </font>|| <font color="#993333">0x0b</font> || <font color="#993333">'\f'</font></pre>
	 *
	 * @see com.spiralgeneration.shortcutjava.shortcuts.AliasShortcut
	 * @see com.spiralgeneration.shortcutjava.shortcuts.MultiExceptionShortcut
	 */
    public static boolean isWhiteSpace(int charValue) {
        if (charValue == ' ' || charValue == '\t' || charValue == '\r' || charValue == '\n' || charValue == 0x0b || charValue == '\f') return true;
        return false;
    }

    private static final ByteArrayOutputStream tmpWord = new ByteArrayOutputStream(512);

    /**
	 * <p>Reads ahead in the stream one whole word at a time. Equivalent to using the regular expression
	 * for (\b)&lt;word_to_find&gt;(\b), but for an input stream.</p>
	 *
	 * <p><b>NOTE: This method is NOT thread-safe.</b></p>
	 */
    public static String readWord(InputStream source) throws IOException {
        tmpWord.reset();
        int readByte = -1;
        while (Utils.isWhiteSpace((readByte = source.read()))) ;
        tmpWord.write(readByte);
        while (!Utils.isWhiteSpace((readByte = source.read()))) tmpWord.write(readByte);
        return new String(tmpWord.toByteArray());
    }

    /**
	 * <p>Reads until a specified character is reached, unless that character is within a comment or a string literal. Thus
	 * a "limit" character is considered to be reached if it is not within a current comment and not within a string literal.</p>
	 *
	 * <p>For example, if we have the below code and usage of this method, it will stop at the '/' in <b>line 2</b>, <i>not</i>
	 * the '/' in <b>line 1</b>!</p>
	 *
	 * <pre>
	 *   <font color="#339900" style="background-color: #e8e8e8; padding: 2px;"><b>Call to this method</b></font>
	 *
	 *	Utils.readTill( false, '/', source, out, true );
	 *
	 *   <font color="#339900" style="background-color: #e8e8e8; padding: 2px;"><b>The code to parse</b></font>
	 *
	 *	<font color="#999999" style="background-color: #e8e8e8; padding: 2px; font-size: 12px;">1.</font> <font color="#0066cc"><b>public</b></font> String someString = <font color="#993333"><b>"This has a '/' that won't stop readTill.";</b></font>
	 *	<font color="#999999" style="background-color: #e8e8e8; padding: 2px; font-size: 12px;">2.</font> <font color="#339900">//The first character in this line stops readTill</font>
	 * </pre>
	 *
	 * @param isCommentStart If the last character read was '/', which is the first character of a comment-start. Helps determine if we're in a comment.
	 * @param limitChar The character to read until (as long as it's encountered outside a comment and outside a string).
	 * @param source The stream to read from.
	 * @param out The stream to write out everything read into. <b>However</b>, if dontSaveComments is "true", then comments will not be written
	 * to this stream.
	 * @param dontSaveComments If "true", then everything inside a comment is not written to the "out" stream.
	 *
	 * @return returns the last character read from the stream.
	 */
    public static int readTill(boolean isCommentStart, char limitChar, InputStream source, OutputStream out, boolean dontSaveComments) throws IOException {
        int nextChar = -1;
        int lastChar = -1;
        while ((nextChar = source.read()) != -1) {
            if (!isCommentStart && nextChar == '/') {
                if (!dontSaveComments) out.write(nextChar);
                isCommentStart = true;
                lastChar = nextChar;
                continue;
            }
            if (isCommentStart && nextChar == '/') {
                if (!dontSaveComments) out.write(nextChar);
                while ((nextChar = source.read()) != -1 && nextChar != '\n') if (!dontSaveComments) out.write(nextChar);
                if (!dontSaveComments) out.write(nextChar); else out.write(LINE_BREAK);
                isCommentStart = false;
                lastChar = nextChar;
            } else if (isCommentStart && nextChar == '*') {
                if (!dontSaveComments) out.write(nextChar);
                while ((nextChar = source.read()) != -1) {
                    if (!dontSaveComments) out.write(nextChar);
                    if (nextChar == '*' && (nextChar = source.read()) == '/') break;
                }
                if (!dontSaveComments) out.write(nextChar); else out.write(' ');
                isCommentStart = false;
                lastChar = nextChar;
            } else if (nextChar == '"' && lastChar != '\\' && limitChar != '"') {
                out.write(nextChar);
                while ((nextChar = source.read()) != -1 && (nextChar != '"' || (nextChar == '"' && lastChar == '\\'))) {
                    out.write(nextChar);
                    lastChar = nextChar;
                }
                if (nextChar != -1) out.write(nextChar);
                lastChar = nextChar;
            } else if (nextChar != limitChar) {
                out.write(nextChar);
                lastChar = nextChar;
            } else {
                lastChar = nextChar;
                break;
            }
        }
        return nextChar;
    }
}
