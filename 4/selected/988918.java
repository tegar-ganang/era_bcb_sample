package org.jaxen.function;

import java.util.List;
import org.jaxen.Context;
import org.jaxen.Function;
import org.jaxen.FunctionCallException;
import org.jaxen.Navigator;

/**
 * <p>
 * <b>4.2</b> <code><i>string</i> normalize-space(<i>string</i>)</code>
 * </p>
 * 
 * <blockquote src="http://www.w3.org/TR/xpath#function-normalize-space"> 
 * The <b>normalize-space</b> function
 * returns the argument string with whitespace normalized by stripping
 * leading and trailing whitespace and replacing sequences of whitespace
 * characters by a single space. Whitespace characters are the same as
 * those allowed by the <a href="http://www.w3.org/TR/REC-xml#NT-S" target="_top">S</a>
 * production in XML. If the argument is omitted, it defaults to the
 * context node converted to a string, in other words the <a
 * href="http://www.w3.org/TR/xpath#dt-string-value"  
 * target="_top">string-value</a> of the context node.
 * </blockquote>
 * 
 * @author James Strachan (james@metastuff.com)
 * @see <a href="http://www.w3.org/TR/xpath#function-normalize-space"
 *      target="_top">Section 4.2 of the XPath Specification</a>
 */
public class NormalizeSpaceFunction implements Function {

    /**
     * Create a new <code>NormalizeSpaceFunction</code> object.
     */
    public NormalizeSpaceFunction() {
    }

    /** 
     * Returns the string-value of the first item in <code>args</code>
     * after removing all leading and trailing white space, and 
     * replacing each other sequence of whitespace by a single space.
     * Whitespace consists of the characters space (0x32), carriage return (0x0D),
     * linefeed (0x0A), and tab (0x09).
     *
     * @param context the context at the point in the
     *         expression when the function is called
     * @param args a list that contains exactly one item
     * 
     * @return a normalized <code>String</code>
     * 
     * @throws FunctionCallException if <code>args</code> does not have length one
     */
    public Object call(Context context, List args) throws FunctionCallException {
        if (args.size() == 0) {
            return evaluate(context.getNodeSet(), context.getNavigator());
        } else if (args.size() == 1) {
            return evaluate(args.get(0), context.getNavigator());
        }
        throw new FunctionCallException("normalize-space() cannot have more than one argument");
    }

    /** 
     * Returns the string-value of <code>strArg</code> after removing
     * all leading and trailing white space, and 
     * replacing each other sequence of whitespace by a single space.
     * Whitespace consists of the characters space (0x32), carriage return (0x0D),
     * linefeed (0x0A), and tab (0x09).
     *
     * @param strArg the object whose string-value is normalized
     * @param nav the context at the point in the
     *         expression when the function is called
     * 
     * @return the normalized string-value
     */
    public static String evaluate(Object strArg, Navigator nav) {
        String str = StringFunction.evaluate(strArg, nav);
        char[] buffer = str.toCharArray();
        int write = 0;
        int lastWrite = 0;
        boolean wroteOne = false;
        int read = 0;
        while (read < buffer.length) {
            if (isXMLSpace(buffer[read])) {
                if (wroteOne) {
                    buffer[write++] = ' ';
                }
                do {
                    read++;
                } while (read < buffer.length && isXMLSpace(buffer[read]));
            } else {
                buffer[write++] = buffer[read++];
                wroteOne = true;
                lastWrite = write;
            }
        }
        return new String(buffer, 0, lastWrite);
    }

    private static boolean isXMLSpace(char c) {
        return c == ' ' || c == '\n' || c == '\r' || c == '\t';
    }
}
