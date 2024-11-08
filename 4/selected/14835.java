package at.tuvienna.main;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * 
 * @author Andreas Gruenwald <a.gruenw@gmail.com>
 * Preprocess XML/XMI files before they are parsed with jSoup.
 * Handles encoding of files depending on charset; 
 */
public class XMLPreprocessor {

    /** Tags that can be used inside converters for html keywords that were replaced **/
    public static final String XMI_BODY = "xmiBody";

    private static final int bufferSize = 0x20000;

    /**
	 * Read a file and convert it into a string considering charset. Especially
	 * parsing of UTF-16 encoded files need additional effort because Java may cause
	 * endless loops if just reading the content using a simple BufferedReader.
	 * @param filename the filename as an absolute path, e.g. /home/andi/uml.xmi
	 * @param charset the expected charset of the file, e.g. UTF-8 or UTF-16. Mandatory!
	 * @return a string containing the read file content correctly encoded.
	 * @throws IOException is thrown if file does not exist or reading of file content failed.
	 */
    public static String readFileContent(String filename, String charset) throws IOException {
        String content = "";
        InputStream inStream = null;
        try {
            File f = new File(filename);
            inStream = new FileInputStream(f);
            ByteBuffer byteData = readToByteBuffer(inStream);
            content = byteBufferToString(byteData, charset, f.getAbsolutePath());
        } finally {
            if (inStream != null) inStream.close();
        }
        return content;
    }

    /**
	 * Reads the file content and preprocesses some file content/XML tags so that
	 * file content fits for jSoup transformation.
	 * @param filename the filename as an absolute path, e.g. /home/andi/uml.xmi
	 * @param charset the expected charset of the file, e.g. UTF-8 or UTF-16. Mandatory!
	 * @return a string containing the read file content correctly encoded.
	 * @throws IOException is thrown if file does not exist or reading of file content failed.
	 */
    public static String readPreprocessedFileContent(String filename, String charset) throws IOException {
        String content = readFileContent(filename, charset);
        content = replaceXMLTag(content, "body", "xmiBody");
        return content;
    }

    /**
	 * Replacement of an XML tag (or parts of it) by another name.
	 * @param content the whole file content.
	 * @param oldTag the old tag name, e.g. "body".
	 * @param newTag the new tag name, e.g. "xmiBody"
	 * @return the transformed content
	 */
    private static String replaceXMLTag(String content, String oldTag, String newTag) {
        content = content.replace("<" + oldTag, "<" + newTag);
        content = content.replace("</" + oldTag + ">", "</" + newTag + ">");
        content = content.replace("<" + oldTag + "/>", "<" + newTag + "/>");
        return content;
    }

    /**
	 * Reads an input stream into a byte buffer.
	 * @param inStream the input stream.
	 * @return a byte buffer containing the file content.
	 * @throws IOException is thrown if problems occur during reading of file.
	 */
    private static ByteBuffer readToByteBuffer(InputStream inStream) throws IOException {
        byte[] buffer = new byte[bufferSize];
        ByteArrayOutputStream outStream = new ByteArrayOutputStream(bufferSize);
        int read;
        while (true) {
            read = inStream.read(buffer);
            if (read == -1) break;
            outStream.write(buffer, 0, read);
        }
        ByteBuffer byteData = ByteBuffer.wrap(outStream.toByteArray());
        return byteData;
    }

    /**
	 * Convertion of byte buffer into string depending on charset.
	 * @param byteData the byte buffer (=file content).
	 * @param charsetName the charset, e.g. UTF-8 or UTF_16.
	 * @param baseUri the base URI, e.g. @{code file.getAbsolutePath()}.
	 * @return the byte buffer converted to a processable string considered charset.
	 */
    private static String byteBufferToString(ByteBuffer byteData, String charsetName, String baseUri) {
        return Charset.forName(charsetName).decode(byteData).toString();
    }
}
