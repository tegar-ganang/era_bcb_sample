package com.dcivision.framework.notification;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.StringTokenizer;
import lotus.domino.EmbeddedObject;
import lotus.domino.NotesException;
import lotus.domino.RichTextItem;
import lotus.domino.RichTextParagraphStyle;
import lotus.domino.RichTextStyle;

/**
 * A RichTextOutputStream is a custom OutputStream that can be used to
 * write data to a Lotus Notes RichTextItem. The primary purpose would be
 * to have the ability to substitute a RichTextOutputStream in a situation where
 * you would normally have to use another type of java.io.OutputStream for writing.
 * 
 * If you need to have a type of java.io.Writer instead, you can easily wrap the
 * RichTextOutputStream in a java.io.OutputStreamWriter. For example:
 *  * Database db = agentContext.getCurrentDatabase();
 * View view = db.getView("All Docs");
 * Document doc = view.getFirstDocument();
 * if (doc.hasItem("DocXML"))
 *     doc.removeItem("DocXML");
 * RichTextItem rtitem = doc.createRichTextItem("DocXML");
 * OutputStreamWriter osw = new OutputStreamWriter( new RichTextOutputStream(rtitem, true, true) );
 * doc.generateXML(osw);
 * osw.close();
 * 
 * Keep in mind that closing the OutputStreamWriter will also close the RichTextOutputStream
 * in the example above, and if you don't close the OutputStreamWriter right away
 * and decide to write to the RichTextOutputStream before everything gets closed, you
 * should flush() the OutputStreamWriter before using the RichTextOutputStream again.
 * 
 * Some of the convenience here lies in the fact that you can automatically
 * convert linefeeds to addNewLine calls on your RichTextField as you're
 * writing to the stream, by calling the setConvert(true) method (or by specifying
 * convertLinefeeds as true when you initially create the stream -- by default,
 * the convertLinefeeds option is already set to true). Also, all of the
 * Exceptions that are thrown here are IOExceptions, to keep in line with the
 * standard operation of other OutputStreams (NotesExceptions are converted
 * to IOExceptions when they are thrown).
 * 
 * In addition, there are some convenience methods that have been added
 * that are outside of the normal OutputStream contract. For example, you can
 * easily write an entire InputStream to a RichTextField by making a call like this:
 *  * RichTextItem body = doc.createRichTextItem("Body");
 * RichTextOutputStream rtos = new RichTextOutputStream(body);
 * rtos.write( new FileInputStream("c:\\somefile.txt") );
 * 
 * Other nice features are the ability to save the Document that the RichTextItem
 * is associated with when the stream is closed using the close() method (this will 
 * happen if you call setSaveOption(true) or set shouldSave to true when you
 * create the stream) and the ability to attach files or change RichText styles as you're
 * writing to the stream.
 * 
 * version 1.0
 * Julian Robichaux ( http://www.nsftools.com )
 * 
 * @author Julian Robichaux ( http://www.nsftools.com )
 * @version 1.0
 */
public class RichTextOutputStream extends OutputStream {

    private RichTextItem rtitem;

    private boolean convertLF = true;

    private boolean saveOnClose = false;

    /**
	 * Create a RichTextOutputStream using the specified Notes RichTextItem
	 * as a target.
	 *
	 * @param  item		the RichTextItem that we're writing to
	 */
    public RichTextOutputStream(RichTextItem item) {
        this(item, true, false);
    }

    /**
	 * Create a RichTextOutputStream using the specified Notes RichTextItem
	 * as a target, specifying whether or not linefeeds should automatically be
	 * converted to new RichText lines as they are encountered.
	 *
	 * @param  item		the RichTextItem that we're writing to
	 * @param  convertLinefeeds		a boolean value indicating whether linefeeds
	 * 											should be converted to new RichText lines
	 * 											(true, the default behavior) or sent to the
	 * 											RichTextItem as raw characters (false)
	 */
    public RichTextOutputStream(RichTextItem item, boolean convertLinefeeds) {
        this(item, convertLinefeeds, false);
    }

    /**
	 * Create a RichTextOutputStream using the specified Notes RichTextItem
	 * as a target, specifying whether or not linefeeds should automatically be
	 * converted to new RichText lines as they are encountered, and whether
	 * or not the document that the RichTextItem is in should be automatically
	 * saved when the stream is closed using the close() method.
	 *
	 * @param  item		the RichTextItem that we're writing to
	 * @param  convertLinefeeds		a boolean value indicating whether linefeeds
	 * 											should be converted to new RichText lines
	 * 											(true, the default behavior) or sent to the
	 * 											RichTextItem as raw characters (false)
	 * @param  shouldSave				a boolean value indicating whether the RichTextItem's
	 * 											parent Document should be saved when the close()
	 * 											method is called (true) or not (false, the default behavior)
	 */
    public RichTextOutputStream(RichTextItem item, boolean convertLinefeeds, boolean shouldSave) {
        rtitem = item;
        setConvert(convertLinefeeds);
        setSaveOption(shouldSave);
    }

    /**
	 * Changes the behaivor of the stream, so that it either automatically converts
	 * \r and \n linefeeds to new RichText lines when they are encountered (the
	 * default behavior) or not. Note that this behavior can be changed at any time
	 * as the stream is being written to, although it affects only new data that is written
	 * to the RichTextItem, not data that has already been written.
	 *
	 * @param  convertLinefeeds		a boolean value indicating whether linefeeds
	 * 											should be converted to new RichText lines
	 * 											(true, the default behavior) or sent to the
	 * 											RichTextItem as raw characters (false)
	 */
    public void setConvert(boolean convertLinefeeds) {
        convertLF = convertLinefeeds;
    }

    /**
	 * Changes the behavior of the stream when the close() method is called,
	 * so that it either causes the Document that contains the RichTextItem to be
	 * saved or it doesn't (default is that it doesn't).
	 *
	 * @param  shouldSave				a boolean value indicating whether the RichTextItem's
	 * 											parent Document should be saved when the close()
	 * 											method is called (true) or not (false, the default behavior)
	 */
    public void setSaveOption(boolean shouldSave) {
        saveOnClose = shouldSave;
    }

    /**
	 * Returns the RichTextItem that the stream is currently writing to; convenient if
	 * you've passed the RichTextOutputStream to a method and you need to access
	 * the underlying RichTextItem directly.
	 *
	 * @return    the Notes RichTextItem that this stream writes to
	 */
    public RichTextItem getRichTextItem() {
        return rtitem;
    }

    /**
	 * Closes the RichTextOutputStream, so that no more data can be written to it.
	 */
    public void close() throws IOException {
        if (saveOnClose) save();
        rtitem = null;
        super.close();
    }

    /**
	 * Overrides the default flush() method in the OutputStream class, although this
	 * method really does nothing, because we're not buffering data and we're always
	 * writing directly to the underlying RichTextItem.
	 */
    public void flush() {
    }

    /**
	 * Saves the RichTextItem we're working on by saving the Document that
	 * contains it.
	 */
    public boolean save() throws IOException {
        try {
            return rtitem.getParent().save();
        } catch (NotesException e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
	 * Writes an array of bytes to the RichTextItem (converting it first to a String).
	 *
	 * @param  b		an array of bytes
	 */
    public void write(byte[] b) throws IOException {
        write(new String(b));
    }

    /**
	 * Writes len bytes from a byte array to the RichTextItem, starting at offset off 
	 * (converting it first to a String).
	 *
	 * @param  b		an array of bytes
	 * @param  off		the start offset in the data
	 * @param  len		the number of bytes to write
	 */
    public void write(byte[] b, int off, int len) throws IOException {
        write(new String(b, off, len));
    }

    /**
	 * Writes a single byte to the RichTextItem (converting it first to a String).
	 *
	 * @param  b		a single byte
	 */
    public void write(int b) throws IOException {
        write(String.valueOf((char) b));
    }

    /**
	 * Writes a String to the RichTextItem.
	 *
	 * @param  str		a String to write
	 */
    public void write(String str) throws IOException {
        try {
            if (rtitem == null) throw new IOException("The RichTextItem that you are trying to write to is null (maybe close() was called)");
            if (str == null) return;
            if (!convertLF) {
                rtitem.appendText(str);
            } else {
                boolean wroteCR = false;
                String token = "";
                StringTokenizer st = new StringTokenizer(str, "\r\n", true);
                while (st.hasMoreTokens()) {
                    token = st.nextToken();
                    if (token.equals("\r")) {
                        rtitem.addNewLine(1);
                        wroteCR = true;
                    } else if (token.equals("\n")) {
                        if (!wroteCR) rtitem.addNewLine(1);
                        wroteCR = false;
                    } else {
                        rtitem.appendText(token);
                        wroteCR = false;
                    }
                }
            }
        } catch (NotesException e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
	 * Writes an entire InputStream to the RichTextItem (converting its contents to a String).
	 *
	 * @param  in		an InputStream
	 */
    public void write(InputStream in) throws IOException {
        int howManyBytes;
        byte[] bytesIn = new byte[2048];
        while ((howManyBytes = in.read(bytesIn)) >= 0) write(bytesIn, 0, howManyBytes);
    }

    /**
	 * Writes a String to the RichTextItem, followed by a RichText newline.
	 *
	 * @param  str		a String to write
	 */
    public void writeLine(String str) throws IOException {
        write(str);
        try {
            rtitem.addNewLine(1);
        } catch (NotesException e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
	 * Writes an attachment or object to the RichTextItem, using the parameters that are
	 * called for in the NotesRichTextItem.embedObject() method (see this method in the 
	 * Notes Designer Help for more information about the parameters and usage).
	 *
	 * @param  type		either EmbeddedObject.EMBED_ATTACHMENT, 
	 * 							EmbeddedObject.EMBED_OBJECT, or 
	 * 							EmbeddedObject.EMBED_OBJECTLINK
	 * @param  objClass		the name of an application (for EmbeddedObject.EMBED_OBJECT)
	 * 								or null
	 * @param  source		the name of the file to attach or link to
	 * @param  name		either a name by which you want to refer to the EmbeddedObject,
	 * 							or null.
	 * @return  EmbeddedObject		a Notes EmbeddedObject reference to the file that
	 * 											was attached
	 */
    public EmbeddedObject writeAttachment(int type, String objClass, String source, String name) throws IOException {
        try {
            return rtitem.embedObject(type, objClass, source, name);
        } catch (NotesException e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
	 * Changes the RichTextStyle of the text that will be written to the underlying
	 * RichTextItem. Note that this only affects data that is written after this method
	 * is called, not data that has already been written.
	 *
	 * @param  style		a Notes RichTextStyle that should be used to format
	 * 							data that is written to this stream
	 */
    public void setStyle(RichTextStyle style) throws IOException {
        try {
            rtitem.appendStyle(style);
        } catch (NotesException e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
	 * Changes the RichTextParagraphStyle of the text that will be written to the underlying
	 * RichTextItem. Note that this only affects data that is written after this method
	 * is called, not data that has already been written.
	 *
	 * @param  style		a Notes RichTextParagraphStyle that should be used to format
	 * 							data that is written to this stream
	 */
    public void setParagraphStyle(RichTextParagraphStyle pstyle) throws IOException {
        try {
            rtitem.appendParagraphStyle(pstyle);
        } catch (NotesException e) {
            throw new IOException(e.getMessage());
        }
    }
}
