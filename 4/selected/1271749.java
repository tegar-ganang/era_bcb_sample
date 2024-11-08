package de.sonivis.tool.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sun.xml.internal.ws.util.StringUtils;

/**
 * @author Benedikt, Sebastian
 * @version $Revision: 1591 $, $Date: 2010-03-25 08:59:12 -0400 (Thu, 25 Mar 2010) $
 */
public final class CoreTooling {

    public static final double NANO_TIME_FACTOR = 0.000001D;

    public static final String NANO_TIME_IDENTIFIER = " msec";

    /**
	 * Logger.
	 */
    private static final Logger LOGGER = LoggerFactory.getLogger(CoreTooling.class);

    /**
	 * This class provides only static methods.
	 */
    private CoreTooling() {
    }

    /**
	 * Returns the {@link Shell} of SONIVIS:Tool
	 * 
	 * @return {@link Shell} of SONIVIS:Tool
	 */
    public static Shell getShell() {
        Display display = Display.getCurrent();
        final Shell shell;
        if (display == null) {
            display = Display.getDefault();
        }
        if (display == null) {
            shell = null;
        } else {
            shell = display.getActiveShell();
        }
        return shell;
    }

    /**
	 * Returns a resized copy of a given font.
	 * 
	 * @param originalFont
	 *            The font to copy & resize.
	 * @param display
	 *            The display the font should be created for.
	 * @param resizeFactor
	 *            Factor for resizing the original font, e.g. 2.0 for double size or 0.5 for half
	 *            size.
	 * @return Resized copy of the given font.
	 */
    public static Font getResizedFont(final Font originalFont, final Display display, final double resizeFactor) {
        final FontData[] fontData = originalFont.getFontData();
        for (final FontData element : fontData) {
            element.setHeight((int) (element.getHeight() * resizeFactor));
        }
        return new Font(display, fontData);
    }

    /**
	 * Returns a copy of a given font in the given font style.
	 * 
	 * @param originalFont
	 *            The font to copy & resize.
	 * @param display
	 *            The display the font should be created for.
	 * @param style
	 *            Bitwise ORed SWT constants, e.g. SWT.BOLD | SWT.ITALIC.
	 * @return Restyled copy of the given font.
	 */
    public static Font getStyledFont(final Font originalFont, final Display display, final int style) {
        final FontData[] fontData = originalFont.getFontData();
        for (final FontData element : fontData) {
            element.setStyle(style);
        }
        return new Font(display, fontData);
    }

    /**
	 * Test if input can be parsed to integer.
	 * 
	 * @param input
	 *            Object to test.
	 * @return true if input can be parsed to integer.
	 */
    public static boolean isInteger(final Object input) {
        return isInteger(input.toString());
    }

    /**
	 * Test if input can be parsed to integer.
	 * 
	 * @param input
	 *            String to test.
	 * @return true if input can be parsed to integer.
	 */
    public static boolean isInteger(final String input) {
        try {
            Integer.parseInt(input);
            return true;
        } catch (final NumberFormatException nfe) {
            return false;
        }
    }

    /**
	 * Returns the integer value of input object.
	 * 
	 * @param input
	 *            Object to parse to integer.
	 * @return The integer value of input.
	 */
    public static Integer getInteger(final Object input) throws NumberFormatException {
        return getInteger(input.toString());
    }

    /**
	 * Returns the integer value of input string.
	 * 
	 * @param input
	 *            String to parse to integer.
	 * @return The integer value of input.
	 */
    public static Integer getInteger(final String input) throws NumberFormatException {
        return Integer.parseInt(input);
    }

    /**
	 * Test if input can be parsed to double.
	 * 
	 * @param input
	 *            Object to test.
	 * @return true if input can be parsed to double.
	 */
    public static boolean isDouble(final Object input) {
        return isDouble(input.toString());
    }

    /**
	 * Test if input can be parsed to double.
	 * 
	 * @param input
	 *            String to test.
	 * @return true if input can be parsed to double.
	 */
    public static boolean isDouble(final String input) {
        try {
            Double.parseDouble(input);
            return true;
        } catch (final NumberFormatException nfe) {
            return false;
        }
    }

    /**
	 * Returns the double value of input object.
	 * 
	 * @param input
	 *            Object to parse to double.
	 * @return The double value of input.
	 */
    public static Double getDouble(final Object input) throws NumberFormatException {
        return getDouble(input.toString());
    }

    /**
	 * Returns the double value of input string.
	 * 
	 * @param input
	 *            String to parse to double.
	 * @return The double value of input.
	 */
    public static Double getDouble(final String input) throws NumberFormatException {
        return Double.parseDouble(input);
    }

    /**
	 * Tries to parse input as double and round up to decimalPlaces. If input can be parsed to
	 * double it returns rounded double or - if rounding returns integer - an integer as String.
	 * 
	 * @param input
	 * @param decimalPlaces
	 * @return Rounded input.
	 */
    public static String tryToRoundAsDouble(final String input, final Integer decimalPlaces) {
        Double val = new Double(0);
        String result = "";
        if (isInteger(input) || !isDouble(input)) {
            return input;
        }
        val = getDouble(input);
        val = Math.round(val * Math.pow(10, decimalPlaces)) / Math.pow(10, decimalPlaces);
        result = val.toString();
        if (result.endsWith(".0")) {
            result = result.substring(0, result.length() - 2);
        }
        return result;
    }

    /**
	 * Makes an array human readable using the given objects <code>toString()</code> method. Uses a
	 * string like 'or' or 'and' to connect the words.
	 * 
	 * @return The human readable version of the array.
	 */
    public static String humanizeArray(final Object[] array, final String connectingWord) {
        final StringBuffer result = new StringBuffer();
        switch(array.length) {
            case 0:
                result.append("None.");
                break;
            case 1:
                result.append(array[0].toString());
                result.append('.');
                break;
            case 2:
                result.append(array[0].toString());
                result.append(" " + connectingWord + " ");
                result.append(array[1].toString());
                result.append('.');
                break;
            default:
                for (int i = 0; i < array.length - 1; ++i) {
                    result.append(array[i].toString());
                    result.append(", ");
                }
                result.append(connectingWord + " ");
                result.append(array[array.length - 1]);
                break;
        }
        return result.toString();
    }

    /**
	 * Makes an array human readable using the given objects <code>toString()</code> method.
	 * 
	 * @return The human readable version of the array.
	 */
    public static String humanizeArray(final Object[] array) {
        return humanizeArray(array, "and");
    }

    /**
	 * Returns a human readable version of a camel case string, e.g. "selected
	 * nodes" instead of "selectedNodes".
	 * 
	 * @param string
	 *            A camel-case string.
	 * @return Human readable version of the given string.
	 */
    public static String humanizeCamelCaseString(final String string) {
        return string.replaceAll("([A-Z])", " $1").toLowerCase();
    }

    public static String capitalizeFirstChar(final String string) {
        return StringUtils.capitalize(string);
    }

    public static void drawArrow(final GC gc, final boolean startHead, final boolean endHead, final int x1, final int y1, final int x2, final int y2) {
        gc.drawLine(x1, y1, x2, y2);
        final float[] headPoints = { -5, -2, -5, 2 };
        final Transform transform = new Transform(gc.getDevice());
        transform.rotate((float) Math.toDegrees(Math.atan2(y2 - y1, x2 - x1)));
        transform.transform(headPoints);
        if (startHead) {
            gc.drawLine(x1 - Math.round(headPoints[0]), y1 - Math.round(headPoints[1]), x1, y1);
            gc.drawLine(x1 - Math.round(headPoints[2]), y1 - Math.round(headPoints[3]), x1, y1);
        }
        if (endHead) {
            gc.drawLine(x2 + Math.round(headPoints[0]), y2 + Math.round(headPoints[1]), x2, y2);
            gc.drawLine(x2 + Math.round(headPoints[2]), y2 + Math.round(headPoints[3]), x2, y2);
        }
    }

    public static void drawNode(final GC gc, final int x, final int y, final double diameter) {
        gc.setAlpha(255);
        gc.setLineStyle(SWT.LINE_SOLID);
        gc.setLineWidth(1);
        gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_WHITE));
        gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_BLACK));
        final int diameterInt = (int) diameter;
        gc.fillOval(x - diameterInt / 2, y - diameterInt / 2, diameterInt, diameterInt);
        gc.drawOval(x - diameterInt / 2, y - diameterInt / 2, diameterInt, diameterInt);
    }

    /**
	 * Auxiliary method to get primitive of an object.
	 * 
	 * @param testObject
	 *            Object to get primitive from.
	 * @return The primitive of given object.
	 */
    @SuppressWarnings("unchecked")
    public static Class getClassPrimitive(final Object testObject) {
        Class returnClass;
        if (testObject.getClass().isPrimitive()) {
            returnClass = testObject.getClass();
        } else if (testObject instanceof Integer) {
            returnClass = int.class;
        } else if (testObject instanceof Boolean) {
            returnClass = boolean.class;
        } else if (testObject instanceof Double) {
            returnClass = double.class;
        } else if (testObject instanceof Long) {
            returnClass = long.class;
        } else if (testObject instanceof Float) {
            returnClass = float.class;
        } else if (testObject instanceof String) {
            returnClass = String.class;
        } else {
            returnClass = Object.class;
        }
        return returnClass;
    }

    public static Class<?> getClassPrimitiveForClass(final Class<?> clazz) {
        Class<?> returnClass;
        if (clazz.isPrimitive()) {
            returnClass = clazz;
        } else if (clazz == Integer.class) {
            returnClass = int.class;
        } else if (clazz == Boolean.class) {
            returnClass = boolean.class;
        } else if (clazz == Double.class) {
            returnClass = double.class;
        } else if (clazz == Long.class) {
            returnClass = long.class;
        } else if (clazz == Float.class) {
            returnClass = float.class;
        } else if (clazz == String.class) {
            returnClass = String.class;
        } else {
            returnClass = Object.class;
        }
        return returnClass;
    }

    /**
	 * Returns a copy of the object, or null if the object cannot be serialized.
	 */
    public static Object copy(final Object orig) {
        Object obj = null;
        try {
            final CoreTooling.FastByteArrayOutputStream fbos = new CoreTooling.FastByteArrayOutputStream();
            final ObjectOutputStream out = new ObjectOutputStream(fbos);
            out.writeObject(orig);
            out.flush();
            out.close();
            final ObjectInputStream in = new ObjectInputStream(fbos.getInputStream());
            obj = in.readObject();
        } catch (final IOException e) {
            e.printStackTrace();
        } catch (final ClassNotFoundException cnfe) {
            cnfe.printStackTrace();
        }
        return obj;
    }

    /**
	 * ByteArrayOutputStream implementation that doesn't synchronize methods and doesn't copy the
	 * data on toByteArray().
	 */
    private static class FastByteArrayOutputStream extends OutputStream {

        /**
		 * Buffer and size
		 */
        private byte[] buf = null;

        private int size = 0;

        /**
		 * Constructs a stream with buffer capacity size 5K
		 */
        public FastByteArrayOutputStream() {
            this(5 * 1024);
        }

        /**
		 * Constructs a stream with the given initial size
		 */
        public FastByteArrayOutputStream(final int initSize) {
            this.size = 0;
            this.buf = new byte[initSize];
        }

        /**
		 * Ensures that we have a large enough buffer for the given size.
		 */
        private void verifyBufferSize(final int sz) {
            if (sz > buf.length) {
                byte[] old = buf;
                buf = new byte[Math.max(sz, 2 * buf.length)];
                System.arraycopy(old, 0, buf, 0, old.length);
                old = null;
            }
        }

        @Override
        public final void write(final byte[] b) {
            verifyBufferSize(size + b.length);
            System.arraycopy(b, 0, buf, size, b.length);
            size += b.length;
        }

        @Override
        public final void write(final byte[] b, final int off, final int len) {
            verifyBufferSize(size + len);
            System.arraycopy(b, off, buf, size, len);
            size += len;
        }

        @Override
        public final void write(final int b) {
            verifyBufferSize(size + 1);
            buf[size++] = (byte) b;
        }

        /**
		 * Returns a ByteArrayInputStream for reading back the written data
		 */
        public InputStream getInputStream() {
            return new FastByteArrayInputStream(buf, size);
        }
    }

    /**
	 * ByteArrayInputStream implementation that does not synchronize methods.
	 */
    private static class FastByteArrayInputStream extends InputStream {

        /**
		 * Our byte buffer
		 */
        private byte[] buf = null;

        /**
		 * Number of bytes that we can read from the buffer
		 */
        private int count = 0;

        /**
		 * Number of bytes that have been read from the buffer
		 */
        private int pos = 0;

        public FastByteArrayInputStream(final byte[] buf, final int count) {
            this.buf = buf;
            this.count = count;
        }

        @Override
        public final int available() {
            return count - pos;
        }

        @Override
        public final int read() {
            return (pos < count) ? (buf[pos++] & 0xff) : -1;
        }

        @Override
        public final int read(final byte[] b, final int off, int len) {
            if (pos >= count) {
                return -1;
            }
            if ((pos + len) > count) {
                len = (count - pos);
            }
            System.arraycopy(buf, pos, b, off, len);
            pos += len;
            return len;
        }

        @Override
        public final long skip(long n) {
            if ((pos + n) > count) {
                n = count - pos;
            }
            if (n < 0) {
                return 0;
            }
            pos += n;
            return n;
        }
    }

    /**
	 * Copies a given directory including recursively.
	 * 
	 * @param sourceDir
	 *            Source directory to copy.
	 * @param targetDir
	 *            Target to copy the directory to.
	 * @throws IOException
	 */
    public static void copyDirectory(final File sourceDir, final File targetDir) throws IOException {
        if (sourceDir.isDirectory()) {
            if (!targetDir.exists()) {
                if (!targetDir.mkdir()) {
                    throw new IOException("Directory " + sourceDir.getAbsolutePath() + " could not be created.");
                }
            }
            final String[] children = sourceDir.list();
            for (final String element : children) {
                copyDirectory(new File(sourceDir, element), new File(targetDir, element));
            }
        } else {
            copyFile(sourceDir, targetDir);
        }
    }

    /**
	 * Copies fromFile to toFile.
	 * 
	 * @param fromFile
	 * @param toFile
	 * @throws IOException
	 */
    public static void copyFile(final File fromFile, File toFile) throws IOException {
        try {
            if (!fromFile.exists()) {
                throw new IOException("FileCopy: " + "no such source file: " + fromFile.getAbsoluteFile());
            }
            if (!fromFile.isFile()) {
                throw new IOException("FileCopy: " + "can't copy directory: " + fromFile.getAbsoluteFile());
            }
            if (!fromFile.canRead()) {
                throw new IOException("FileCopy: " + "source file is unreadable: " + fromFile.getAbsoluteFile());
            }
            if (toFile.isDirectory()) {
                toFile = new File(toFile, fromFile.getName());
            }
            if (toFile.exists() && !toFile.canWrite()) {
                throw new IOException("FileCopy: " + "destination file is unwriteable: " + toFile.getAbsoluteFile());
            }
            final FileChannel inChannel = new FileInputStream(fromFile).getChannel();
            final FileChannel outChannel = new FileOutputStream(toFile).getChannel();
            try {
                inChannel.transferTo(0, inChannel.size(), outChannel);
            } catch (final IOException e) {
                throw e;
            } finally {
                if (inChannel != null) {
                    inChannel.close();
                }
                if (outChannel != null) {
                    outChannel.close();
                }
            }
        } catch (final IOException e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("CopyFile went wrong!", e);
            }
        }
    }
}
